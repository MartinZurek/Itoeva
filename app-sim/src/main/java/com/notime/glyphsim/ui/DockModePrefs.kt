package com.notime.glyphsim.ui

import android.content.Context

/**
 * Merkt sich, ob der manuelle "Dock-Modus" (Bildschirm bleibt an, zeigt die Uhr
 * dauerhaft wie eine Nachttisch-Uhr) beim letzten Verlassen der App aktiv war, damit
 * er nach einem Neustart der App wiederhergestellt wird.
 */
object DockModePrefs {
    private const val PREFS_NAME = "dock_mode_prefs"
    private const val KEY_ENABLED = "dock_mode_enabled"

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
