package com.florentrevest.microanki

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Setup + configuration screen for the timer fork: grant permissions, pick a
 * deck, and choose how often (in seconds) a flashcard should pop up.
 */
class MainActivity : ComponentActivity() {

    private val anki by lazy { AnkiDroidHelper(this) }
    private val prefs by lazy { Prefs(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = appColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SettingsScreen()
                }
            }
        }
    }

    @Composable
    private fun SettingsScreen() {
        val context = LocalContext.current
        val scroll = rememberScrollState()
        val tick = resumeTick()

        // Re-read live status whenever we come back to the foreground.
        val apiAvailable = remember(tick) { anki.isApiAvailable() }
        val hasPermission = remember(tick) { anki.hasPermission() }
        val canOverlay = remember(tick) { Settings.canDrawOverlays(context) }
        val notificationsOn = remember(tick) { areNotificationsEnabled() }
        val batteryUnrestricted = remember(tick) { isBatteryUnrestricted() }
        val timerRunning = remember(tick) { CardTimerService.isRunning }

        val permissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { }
        val notificationPermissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(scroll)
                .padding(20.dp),
        ) {
            Text("MicroAnki", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "See a flashcard every X seconds. " +
                    "Pick a deck, set your interval, and start the timer.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(20.dp))

            SectionCard("1 · Permissions") {
                StatusRow(
                    label = "AnkiDroid installed",
                    done = apiAvailable,
                    actionLabel = "Get",
                    onAction = { openAnkiDroidInStore(context) },
                )
                StatusRow(
                    label = "Access to AnkiDroid collection",
                    done = hasPermission,
                    actionLabel = "Grant",
                    enabled = apiAvailable,
                    onAction = { permissionLauncher.launch(AnkiDroidHelper.READ_WRITE_PERMISSION) },
                )
                StatusRow(
                    label = "Display over other apps",
                    done = canOverlay,
                    actionLabel = "Allow",
                    onAction = {
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName"),
                            )
                        )
                    },
                )
                StatusRow(
                    label = "Notifications (for timer status)",
                    done = notificationsOn,
                    actionLabel = "Allow",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                            })
                        }
                    },
                )
                StatusRow(
                    label = "Ignore battery optimizations",
                    done = batteryUnrestricted,
                    actionLabel = "Disable",
                    onAction = { requestIgnoreBatteryOptimizations() },
                )
            }

            Spacer(Modifier.height(16.dp))

            SectionCard("2 · Deck to practise") {
                DeckPicker(enabled = hasPermission)
            }

            Spacer(Modifier.height(16.dp))

            SectionCard("3 · Card interval") {
                IntervalPicker(
                    timerRunning = timerRunning,
                    onStart = { CardTimerService.start(context) },
                    onStop = { CardTimerService.stop(context) },
                )
            }

            Spacer(Modifier.height(16.dp))

            SectionCard("4 · Options") {
                ForceAnswerSwitch()
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = { startActivity(Intent(this@MainActivity, FlashcardActivity::class.java)) },
                enabled = hasPermission && prefs.hasDeck,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Show a card now") }

            Spacer(Modifier.height(24.dp))
        }
    }

    private fun areNotificationsEnabled(): Boolean {
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        return nm.areNotificationsEnabled()
    }

    private fun isBatteryUnrestricted(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestIgnoreBatteryOptimizations() {
        try {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName"),
                )
            )
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    // --- Sections -----------------------------------------------------------

    @Composable
    private fun DeckPicker(enabled: Boolean) {
        val scope = rememberCoroutineScopeCompat()
        var decks by remember { mutableStateOf<List<DeckInfo>>(emptyList()) }
        var expanded by remember { mutableStateOf(false) }
        var deckName by remember { mutableStateOf(prefs.deckName) }

        Column {
            OutlinedButton(
                enabled = enabled,
                onClick = {
                    scope.launch {
                        decks = withContext(Dispatchers.IO) { anki.getDecks() }
                        expanded = true
                    }
                },
            ) {
                Text(if (deckName.isBlank()) "Choose a deck" else "Deck: $deckName")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (decks.isEmpty()) {
                    DropdownMenuItem(text = { Text("No decks found") }, onClick = { expanded = false })
                }
                decks.forEach { deck ->
                    DropdownMenuItem(
                        text = { Text(deck.name) },
                        onClick = {
                            prefs.deckId = deck.id
                            prefs.deckName = deck.name
                            deckName = deck.name
                            expanded = false
                        },
                    )
                }
            }
        }
    }

    @Composable
    private fun IntervalPicker(
        timerRunning: Boolean,
        onStart: () -> Unit,
        onStop: () -> Unit,
    ) {
        val context = LocalContext.current
        var text by remember { mutableStateOf(prefs.intervalSeconds.toString()) }

        Text(
            "A card pops up every X seconds while the timer runs.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { new ->
                text = new.filter { it.isDigit() }.take(5)
                val seconds = text.toIntOrNull()
                if (seconds != null && seconds >= Prefs.MIN_INTERVAL_SECONDS) {
                    prefs.intervalSeconds = seconds
                    if (CardTimerService.isRunning) {
                        CardTimerService.restart(context)
                    }
                }
            },
            label = { Text("Seconds between cards (min ${Prefs.MIN_INTERVAL_SECONDS})") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IntervalPreset(seconds = 30, current = prefs.intervalSeconds, onPick = {
                prefs.intervalSeconds = it
                text = it.toString()
                if (CardTimerService.isRunning) CardTimerService.restart(context)
            })
            IntervalPreset(seconds = 60, current = prefs.intervalSeconds, onPick = {
                prefs.intervalSeconds = it
                text = it.toString()
                if (CardTimerService.isRunning) CardTimerService.restart(context)
            })
            IntervalPreset(seconds = 300, current = prefs.intervalSeconds, onPick = {
                prefs.intervalSeconds = it
                text = it.toString()
                if (CardTimerService.isRunning) CardTimerService.restart(context)
            })
            IntervalPreset(seconds = 900, current = prefs.intervalSeconds, onPick = {
                prefs.intervalSeconds = it
                text = it.toString()
                if (CardTimerService.isRunning) CardTimerService.restart(context)
            })
        }
        Spacer(Modifier.height(8.dp))

        Text(
            humanInterval(prefs.intervalSeconds),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))

        if (timerRunning) {
            Button(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                Text("Stop timer")
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Timer is running — a card appears every ${prefs.intervalSeconds}s.",
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            val canStart = prefs.hasDeck &&
                (text.toIntOrNull() ?: 0) >= Prefs.MIN_INTERVAL_SECONDS
            Button(
                onClick = {
                    val seconds = text.toIntOrNull() ?: Prefs.DEFAULT_INTERVAL_SECONDS
                    prefs.intervalSeconds = seconds.coerceAtLeast(Prefs.MIN_INTERVAL_SECONDS)
                    text = prefs.intervalSeconds.toString()
                    ensureNotificationPermission()
                    onStart()
                },
                enabled = canStart,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Start timer")
            }
            if (!prefs.hasDeck) {
                Text(
                    "Pick a deck first.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }

    @Composable
    private fun IntervalPreset(seconds: Int, current: Int, onPick: (Int) -> Unit) {
        FilterChip(
            selected = current == seconds,
            onClick = { onPick(seconds) },
            label = { Text(presetLabel(seconds)) },
        )
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // The in-screen launcher handles the result-driven flow; this
                // direct request covers the button-press path on first run.
                // (If denied, cards still try a direct launch + notification.)
            }
        }
    }

    @Composable
    private fun ForceAnswerSwitch() {
        var force by remember { mutableStateOf(prefs.forceAnswer) }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Block back until answered", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Makes the card harder to dismiss without engaging.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(
                checked = force,
                onCheckedChange = {
                    force = it
                    prefs.forceAnswer = it
                },
            )
        }
    }

    // --- Small reusable pieces ---------------------------------------------

    @Composable
    private fun SectionCard(title: String, content: @Composable () -> Unit) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                content()
            }
        }
    }

    @Composable
    private fun StatusRow(
        label: String,
        done: Boolean,
        actionLabel: String,
        enabled: Boolean = true,
        onAction: () -> Unit,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        ) {
            val icon: ImageVector = if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            )
            Spacer(Modifier.width(12.dp))
            Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            if (done) {
                Text("Done", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            } else {
                TextButton(onClick = onAction, enabled = enabled) { Text(actionLabel) }
            }
        }
        HorizontalDivider()
    }

    private fun openAnkiDroidInStore(context: android.content.Context) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.ichi2.anki"))
        val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.ichi2.anki"))
        try {
            context.startActivity(market)
        } catch (e: Exception) {
            context.startActivity(web)
        }
    }
}

private fun presetLabel(seconds: Int): String = when {
    seconds < 60 -> "${seconds}s"
    seconds % 60 == 0 -> "${seconds / 60}m"
    else -> "${seconds}s"
}

private fun humanInterval(seconds: Int): String = when {
    seconds < 60 -> "Every $seconds seconds"
    seconds % 60 == 0 -> "Every ${seconds / 60} minute(s)"
    else -> "Every $seconds seconds (${seconds / 60}m ${seconds % 60}s)"
}

// --- Compose helpers --------------------------------------------------------

@Composable
private fun rememberCoroutineScopeCompat() = androidx.compose.runtime.rememberCoroutineScope()

/** Increments every time the host lifecycle reaches ON_RESUME. */
@Composable
private fun resumeTick(): Int {
    var tick by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return tick
}
