package com.florentrevest.microanki

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that shows a flashcard every [Prefs.intervalSeconds],
 * but only while the screen is on and unlocked.
 *
 * Timer fork: replaces the old accessibility-service "card on app open"
 * trigger with a plain repeating interval chosen by the user in settings.
 * Ticks that fire while the screen is off or the device is locked are
 * skipped (no backlog, no lock-screen cards).
 */
class CardTimerService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var loopJob: Job? = null
    private lateinit var prefs: Prefs

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTimer()
                return START_NOT_STICKY
            }
            else -> startTimer()
        }
        return START_STICKY
    }

    private fun startTimer() {
        isRunning = true
        prefs.timerEnabled = true
        startForegroundWithType()
        restartLoop()
        Log.d(TAG, "Timer started, every ${prefs.intervalSeconds}s")
    }

    private fun stopTimer() {
        isRunning = false
        prefs.timerEnabled = false
        loopJob?.cancel()
        loopJob = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(TAG, "Timer stopped")
    }

    /** (Re)starts the delay loop so a changed interval applies immediately. */
    private fun restartLoop() {
        loopJob?.cancel()
        loopJob = scope.launch {
            while (isActive) {
                val interval = prefs.intervalSeconds.toLong()
                    .coerceAtLeast(Prefs.MIN_INTERVAL_SECONDS.toLong())
                delay(interval * 1000L)
                if (!isActive) break
                if (!isScreenOnAndUnlocked()) {
                    Log.d(TAG, "Skipping card: screen off or locked")
                    continue
                }
                showFlashcard()
            }
        }
        // Refresh the ongoing notification text with the current interval.
        val nm = getSystemService(NotificationManager::class.java)
        nm?.notify(ONGOING_ID, buildOngoingNotification())
    }

    private fun startForegroundWithType() {
        val notification = buildOngoingNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= 34) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            }
            if (type != 0) {
                startForeground(ONGOING_ID, notification, type)
            } else {
                startForeground(ONGOING_ID, notification)
            }
        } else {
            startForeground(ONGOING_ID, notification)
        }
    }

    private fun buildOngoingNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, CardTimerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_TIMER)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("MicroAnki timer running")
            .setContentText("A card every ${prefs.intervalSeconds}s — tap Stop to pause")
            .setContentIntent(openApp)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    /**
     * True only when the user can actually see and interact with a card:
     * screen on AND not on the lock screen. This keeps cards from waking
     * the phone, piling up on the lock screen, or firing while locked.
     */
    private fun isScreenOnAndUnlocked(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isInteractive) return false
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (km.isKeyguardLocked) return false
        return true
    }

    /**
     * Shows one card. Tries a direct activity launch (works when the app is
     * in the foreground) and also posts a full-screen-intent notification so
     * the card pops up from the background on Android 10+ where background
     * activity starts are restricted.
     */
    private fun showFlashcard() {
        // Re-check: screen may have turned off between the loop tick and now.
        if (!isScreenOnAndUnlocked()) {
            Log.d(TAG, "Skipping card at show time: screen off or locked")
            return
        }
        val fullScreenIntent = Intent(this, FlashcardActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullScreenPending = PendingIntent.getActivity(
            this, CARD_REQUEST_CODE, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Direct launch: fastest path when allowed.
        try {
            startActivity(fullScreenIntent)
        } catch (e: Exception) {
            Log.w(TAG, "Direct card launch failed, using full-screen notification", e)
        }

        // Full-screen fallback: heads-up notification that auto-opens the card.
        val cardNotification = NotificationCompat.Builder(this, CHANNEL_CARD)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Time for a flashcard")
            .setContentText("Tap to review your next due card")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(fullScreenPending)
            .setFullScreenIntent(fullScreenPending, true)
            .setAutoCancel(true)
            .build()
        val nm = getSystemService(NotificationManager::class.java)
        nm?.notify(CARD_NOTIFICATION_ID + (System.currentTimeMillis() % 10_000).toInt(), cardNotification)
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_TIMER, "Timer status",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Shows while the flashcard timer is running" }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CARD, "Flashcards",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "Pops up a flashcard every interval" }
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        loopJob?.cancel()
        isRunning = false
        super.onDestroy()
    }

    companion object {
        private const val TAG = "CardTimerService"

        const val ACTION_START = "com.florentrevest.microanki.START_TIMER"
        const val ACTION_STOP = "com.florentrevest.microanki.STOP_TIMER"

        const val CHANNEL_TIMER = "microanki_timer"
        const val CHANNEL_CARD = "microanki_cards"
        private const val ONGOING_ID = 1001
        private const val CARD_NOTIFICATION_ID = 2000
        private const val CARD_REQUEST_CODE = 42

        @Volatile
        var isRunning: Boolean = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, CardTimerService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, CardTimerService::class.java).setAction(ACTION_STOP)
            )
        }

        /** Restart the loop without recreating the service (interval changed). */
        fun restart(context: Context) {
            if (isRunning) start(context)
        }
    }
}

/** Restarts the timer after a reboot if the user left it enabled. */
class TimerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!Prefs(context).timerEnabled) return
        try {
            CardTimerService.start(context)
        } catch (e: Exception) {
            Log.w("TimerBootReceiver", "Could not restart timer after boot", e)
        }
    }
}
