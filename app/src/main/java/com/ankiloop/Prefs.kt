package com.ankiloop

import android.content.Context

/**
 * Tiny wrapper around SharedPreferences holding all user configuration:
 * which deck to draw from and how often a card should pop up.
 */
class Prefs(context: Context) {

    private val sp = run {
        val fresh = context.applicationContext
            .getSharedPreferences(NAME, Context.MODE_PRIVATE)
        migrateFromLegacy(context.applicationContext, fresh)
        fresh
    }

    /**
     * One-time carry-over for users coming from the MicroAnki fork:
     * copies deck / interval / theme / options into the new AnkiLoop prefs.
     */
    private fun migrateFromLegacy(
        app: Context,
        fresh: android.content.SharedPreferences,
    ) {
        val legacy = app.getSharedPreferences(LEGACY_NAME, Context.MODE_PRIVATE)
        if (!legacy.contains(KEY_DECK_ID) && !legacy.contains(KEY_INTERVAL_SECONDS)) return
        if (fresh.contains(KEY_DECK_ID) || fresh.contains(KEY_INTERVAL_SECONDS)) return
        fresh.edit()
            .putLong(KEY_DECK_ID, legacy.getLong(KEY_DECK_ID, NO_DECK))
            .putString(KEY_DECK_NAME, legacy.getString(KEY_DECK_NAME, "") ?: "")
            .putInt(KEY_INTERVAL_SECONDS, legacy.getInt(KEY_INTERVAL_SECONDS, DEFAULT_INTERVAL_SECONDS))
            .putBoolean(KEY_FORCE, legacy.getBoolean(KEY_FORCE, true))
            .putString(KEY_CARD_THEME, legacy.getString(KEY_CARD_THEME, SYSTEM_AUTO_THEME_ID))
            .apply()
    }

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

    /** Stored id of the pop-up flashcard theme (see ALL_CARD_THEMES). */
    var cardThemeId: String
        get() = sp.getString(KEY_CARD_THEME, SYSTEM_AUTO_THEME_ID) ?: SYSTEM_AUTO_THEME_ID
        set(value) = sp.edit().putString(KEY_CARD_THEME, value).apply()

    /** Whether grade buttons show the next-review time (e.g. "1d"). */
    var showIntervals: Boolean
        get() = sp.getBoolean(KEY_SHOW_INTERVALS, true)
        set(value) = sp.edit().putBoolean(KEY_SHOW_INTERVALS, value).apply()

    val hasDeck: Boolean get() = deckId != NO_DECK

    companion object {
        const val NO_DECK = -1L

        const val DEFAULT_INTERVAL_SECONDS = 300 // 5 minutes
        const val MIN_INTERVAL_SECONDS = 10

        private const val NAME = "ankiloop_prefs"
        private const val LEGACY_NAME = "microanki_prefs"
        private const val KEY_DECK_ID = "deck_id"
        private const val KEY_DECK_NAME = "deck_name"
        private const val KEY_INTERVAL_SECONDS = "interval_seconds"
        private const val KEY_FORCE = "force_answer"
        private const val KEY_TIMER_ENABLED = "timer_enabled"
        private const val KEY_CARD_THEME = "card_theme_id"
        private const val KEY_SHOW_INTERVALS = "show_intervals"
    }
}
