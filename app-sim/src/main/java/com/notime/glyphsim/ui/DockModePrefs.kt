package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphsim.settings.SettingsCatalog
import com.notime.glyphsim.settings.SettingsStore

/**
 * Merkt sich, ob der manuelle "Dock-Modus" (Bildschirm bleibt an, zeigt die Uhr
 * dauerhaft wie eine Nachttisch-Uhr) beim letzten Verlassen der App aktiv war, damit
 * er nach einem Neustart der App wiederhergestellt wird.
 */
object DockModePrefs {
    // Ablageort und Schluessel stehen jetzt gebuendelt in SettingsCatalog - die oeffentliche
    // Schnittstelle hier bleibt unveraendert, damit keine der Aufrufstellen angefasst werden muss.
    fun isEnabled(context: Context): Boolean =
        SettingsStore.read(context, SettingsCatalog.DockModeEnabled)

    fun setEnabled(context: Context, enabled: Boolean) {
        SettingsStore.write(context, SettingsCatalog.DockModeEnabled, enabled)
    }
}
