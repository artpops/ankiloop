package com.florentrevest.microanki

import android.content.Context

/**
 * Tiny wrapper around SharedPreferences holding all user configuration:
 * which deck to draw from and how often a card should pop up.
 */
class Prefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var deckId: Long
        get() = sp.getLong(KEY_DECK_ID, NO_DECK)
        set(value) = sp.edit().putLong(KEY_DECK_ID, value).apply()

    var deckName: String
        get() = sp.getString(KEY_DECK_NAME, "") ?: ""
        set(value) = sp.edit().putString(KEY_DECK_NAME, value).apply()

    /**
     * Seconds between two flashcards. Set by the user in settings.
     * Defaults to [DEFAULT_INTERVAL_SECONDS].
     */
    var intervalSeconds: Int
        get() = sp.getInt(KEY_INTERVAL_SECONDS, DEFAULT_INTERVAL_SECONDS).coerceAtLeast(MIN_INTERVAL_SECONDS)
        set(value) = sp.edit().putInt(KEY_INTERVAL_SECONDS, value.coerceAtLeast(MIN_INTERVAL_SECONDS)).apply()

    /** Whether the back button is blocked until the card is answered. */
    var forceAnswer: Boolean
        get() = sp.getBoolean(KEY_FORCE, true)
        set(value) = sp.edit().putBoolean(KEY_FORCE, value).apply()

    /** Persisted so the timer can be restarted after a reboot. */
    var timerEnabled: Boolean
        get() = sp.getBoolean(KEY_TIMER_ENABLED, false)
        set(value) = sp.edit().putBoolean(KEY_TIMER_ENABLED, value).apply()

    val hasDeck: Boolean get() = deckId != NO_DECK

    companion object {
        const val NO_DECK = -1L

        const val DEFAULT_INTERVAL_SECONDS = 300 // 5 minutes
        const val MIN_INTERVAL_SECONDS = 10

        private const val NAME = "microanki_prefs"
        private const val KEY_DECK_ID = "deck_id"
        private const val KEY_DECK_NAME = "deck_name"
        private const val KEY_INTERVAL_SECONDS = "interval_seconds"
        private const val KEY_FORCE = "force_answer"
        private const val KEY_TIMER_ENABLED = "timer_enabled"
    }
}
