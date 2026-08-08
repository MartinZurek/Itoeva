package com.notime.glyphsim.ui

import android.content.Context

/**
 * Optionale, manuell gesetzte Helligkeit fuer den Dock-Modus. Standardmaessig
 * deaktiviert - der Dock-Modus laeuft dann einfach mit der aktuellen System-
 * helligkeit (genau wie das Home-Screen-Widget auch keine eigene Helligkeit
 * erzwingt). Nur wenn der Nutzer das ueber den Regler in HomeScreen explizit
 * einschaltet, wird [brightness] als Fenster-Override angewendet.
 */
object DockBrightnessPrefs {
    private const val PREFS_NAME = "dock_brightness_prefs"
    private const val KEY_OVERRIDE_ENABLED = "override_enabled"
    private const val KEY_BRIGHTNESS = "brightness"
    private const val DEFAULT_BRIGHTNESS = 0.3f

    fun isOverrideEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_OVERRIDE_ENABLED, false)

    fun getBrightness(context: Context): Float =
        prefs(context).getFloat(KEY_BRIGHTNESS, DEFAULT_BRIGHTNESS)

    fun setOverride(context: Context, enabled: Boolean, brightness: Float) {
        prefs(context).edit()
            .putBoolean(KEY_OVERRIDE_ENABLED, enabled)
            .putFloat(KEY_BRIGHTNESS, brightness)
            .apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
