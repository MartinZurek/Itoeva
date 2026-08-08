package com.notime.glyphsim.ui

import android.content.Context

/**
 * Ob die Aufnahme-Schaltflaeche im Play-Modus ueberhaupt erscheint.
 *
 * **Warum ein Schalter und nicht einfach immer sichtbar.** Der Dock-Modus ist bewusst leer - eine
 * schwarze Flaeche, eine Uhr, sonst nichts. Jedes dauerhaft sichtbare Bedienelement widerspricht
 * dem. Wer Filme aufnehmen will, schaltet es frei und hat es dann griffbereit; alle anderen
 * behalten ihren ungestoerten Bildschirm.
 */
object ClipPrefs {
    private const val PREFS = "clip_prefs"
    private const val KEY_ENABLED = "recording_enabled"

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
