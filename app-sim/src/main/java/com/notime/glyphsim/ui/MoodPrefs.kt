package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphsim.settings.SettingsCatalog
import com.notime.glyphsim.settings.SettingsStore

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
    // Ablageort und Schluessel stehen jetzt gebuendelt in SettingsCatalog - die oeffentliche
    // Schnittstelle hier bleibt unveraendert, damit keine der Aufrufstellen angefasst werden muss.
    fun isEnabled(context: Context): Boolean =
        SettingsStore.read(context, SettingsCatalog.MoodEnabled)

    fun setEnabled(context: Context, enabled: Boolean) {
        SettingsStore.write(context, SettingsCatalog.MoodEnabled, enabled)
    }
}
