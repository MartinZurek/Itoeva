package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphsim.settings.SettingsCatalog
import com.notime.glyphsim.settings.SettingsStore

/**
 * Ob die Stimmung des Avatars sein Verhalten beeinflusst (siehe
 * [com.notime.glyphsim.matrix.AvatarMood]).
 *
 * **Standardmaessig aus.** Der Grund stand hier frueher falsch beschrieben ("braucht mindestens
 * drei ausgeloeste Erinnerungen, bevor sie von NEUTRAL abweicht") - das galt fuer eine Rechnung,
 * die es seit der Umstellung auf Tagesziele nicht mehr gibt. Tatsaechlich schlaegt die Stimmung
 * sofort aus, sobald es ueberhaupt ein Tagesziel gibt.
 *
 * Der echte Grund ist ein anderer: Sie veraendert das Bewegungstempo des Wesens. Wer die App
 * gerade einrichtet und einzelne Animationen ausprobiert, sieht dann einen traegen Avatar und
 * haelt das fuer einen Fehler statt fuer eine Aussage. Als bewusst eingeschaltete Funktion ist
 * der Zusammenhang dagegen klar.
 *
 * Seit die Stimmung anteilig gegen das rechnet, was bis jetzt anstand (siehe
 * [com.notime.glyphsim.matrix.expectedByNow]), spraeche wenig dagegen, sie voreingestellt
 * anzuschalten - vorher war sie den halben Tag lang truebe, auch bei einem puenktlich erledigten
 * Tag. Das ist eine Produktentscheidung und bleibt bis auf Weiteres offen.
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
