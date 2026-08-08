package com.notime.glyphsim.ui

import android.content.Context

/**
 * Ob die Stimmung des Avatars sein Verhalten beeinflusst (siehe
 * [com.notime.glyphsim.matrix.AvatarMood]).
 *
 * **Standardmaessig aus.** Die Stimmung braucht mindestens drei ausgeloeste Erinnerungen
 * innerhalb von 24 Stunden, bevor sie ueberhaupt von NEUTRAL abweicht - beim Ausprobieren
 * einzelner Erinnerungen sieht man davon also nichts ausser einem unerklaerlich traegen Avatar,
 * sobald man ein paar Ausloesungen hat verstreichen lassen. Als bewusst eingeschaltete Funktion
 * ist der Zusammenhang dagegen klar, und beim Testen einzelner Animationen stoert sie nicht.
 */
object MoodPrefs {
    private const val PREFS_NAME = "mood_prefs"
    private const val KEY_ENABLED = "mood_enabled"

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
