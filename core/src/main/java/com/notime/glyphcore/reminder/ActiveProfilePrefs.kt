package com.notime.glyphcore.reminder

import android.content.Context

/**
 * Welcher Erinnerungs-Satz gerade gilt. Erinnerungen gehoeren zu genau einem Profil
 * (siehe [com.notime.glyphcore.data.GlyphReminder.profileId]); nur die des aktiven Profils
 * werden eingeplant und ausgeloest.
 *
 * Bewusst NEUTRAL als "Profil" modelliert und nicht als Avatar: im :app-sim-Modul entspricht
 * ein Profil einem Tamagotchi-Avatar (jeder Avatar hat seinen eigenen Satz Erinnerungen), das
 * :app-Modul steuert dagegen die echte Glyph-Matrix-Hardware und kennt gar keine Avatare - es
 * bleibt schlicht bei [DEFAULT_PROFILE_ID]. Der gemeinsame Kern darf deshalb keinen
 * Avatar-Begriff enthalten.
 *
 * Der Wert liegt in SharedPreferences statt in einer zur Laufzeit gesetzten Variable, weil
 * [ReminderScheduler] und die Alarm-Receiver der Apps auch aus einem kalt gestarteten Prozess
 * heraus laufen (Boot, AlarmManager) - dort gibt es keinen garantierten Initialisierungspunkt,
 * an dem ein Provider vorher gesetzt worden waere.
 */
object ActiveProfilePrefs {
    const val DEFAULT_PROFILE_ID = "default"

    private const val PREFS_NAME = "reminder_profile_prefs"
    private const val KEY_PROFILE_ID = "active_profile_id"

    fun get(context: Context): String =
        prefs(context).getString(KEY_PROFILE_ID, null) ?: DEFAULT_PROFILE_ID

    fun set(context: Context, profileId: String) {
        prefs(context).edit().putString(KEY_PROFILE_ID, profileId).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
