package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphsim.settings.SettingsCatalog
import com.notime.glyphsim.settings.SettingsStore

/**
 * Optionale, manuell gesetzte Helligkeit fuer den Dock-Modus. Standardmaessig
 * deaktiviert - der Dock-Modus laeuft dann einfach mit der aktuellen System-
 * helligkeit (genau wie das Home-Screen-Widget auch keine eigene Helligkeit
 * erzwingt). Nur wenn der Nutzer das ueber den Regler in HomeScreen explizit
 * einschaltet, wird [brightness] als Fenster-Override angewendet.
 */
object DockBrightnessPrefs {
    fun isOverrideEnabled(context: Context): Boolean =
        SettingsStore.read(context, SettingsCatalog.DockBrightnessOverride)

    fun getBrightness(context: Context): Float =
        SettingsStore.read(context, SettingsCatalog.DockBrightness)

    /**
     * Schalter und Wert gehoeren zusammen und werden deshalb gemeinsam geschrieben - sonst kann
     * ein Beobachter den Zwischenstand sehen (Schalter schon an, Helligkeit noch alt) und die
     * Anzeige springt zweimal.
     */
    fun setOverride(context: Context, enabled: Boolean, brightness: Float) {
        SettingsStore.write(context, SettingsCatalog.DockBrightnessOverride, enabled)
        SettingsStore.write(context, SettingsCatalog.DockBrightness, brightness)
    }
}
