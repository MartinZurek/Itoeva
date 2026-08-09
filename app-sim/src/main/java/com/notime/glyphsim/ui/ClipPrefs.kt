package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphsim.settings.SettingsCatalog
import com.notime.glyphsim.settings.SettingsStore

/**
 * Ob die Aufnahme-Schaltflaeche im Play-Modus ueberhaupt erscheint.
 *
 * **Warum ein Schalter und nicht einfach immer sichtbar.** Der Dock-Modus ist bewusst leer - eine
 * schwarze Flaeche, eine Uhr, sonst nichts. Jedes dauerhaft sichtbare Bedienelement widerspricht
 * dem. Wer Filme aufnehmen will, schaltet es frei und hat es dann griffbereit; alle anderen
 * behalten ihren ungestoerten Bildschirm.
 */
object ClipPrefs {

    // Ablageort und Schluessel: siehe SettingsCatalog.
    fun isEnabled(context: Context): Boolean =
        SettingsStore.read(context, SettingsCatalog.ClipRecordingEnabled)

    fun setEnabled(context: Context, value: Boolean) {
        SettingsStore.write(context, SettingsCatalog.ClipRecordingEnabled, value)
    }

}
