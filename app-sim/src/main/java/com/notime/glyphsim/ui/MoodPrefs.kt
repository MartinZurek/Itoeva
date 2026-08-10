package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphsim.settings.SettingsCatalog
import com.notime.glyphsim.settings.SettingsStore

/**
 * Ob die Stimmung des Avatars sein Verhalten beeinflusst (siehe
 * [com.notime.glyphsim.matrix.AvatarMood]).
 *
 * **Standardmaessig AN** - seit Phase 7, und das war vorher zweimal anders begruendet als es
 * stimmte.
 *
 * Hier stand: "braucht mindestens drei ausgeloeste Erinnerungen, bevor sie von NEUTRAL
 * abweicht". Das galt fuer eine Rechnung, die es seit der Umstellung auf Tagesziele nicht mehr
 * gibt - tatsaechlich schlug sie sofort aus. Der echte Grund fuers Abschalten war ein anderer:
 * Sie aendert das Bewegungstempo des Wesens, und wer die App gerade einrichtet, haelt einen
 * traegen Avatar fuer einen Fehler statt fuer eine Aussage.
 *
 * **Dieser Grund traegt nicht mehr.** Zwei Dinge haben sich geaendert:
 *
 * 1. Die Stimmung rechnet anteilig gegen das, was bis jetzt anstand (siehe
 *    [com.notime.glyphsim.matrix.expectedByNow]). Vorher war sie den halben Tag lang truebe,
 *    auch bei einem puenktlich erledigten Tag - genau das sah aus wie ein Fehler.
 * 2. Die Standard-Erinnerungen tragen gar kein Tagesziel (siehe
 *    [com.notime.glyphcore.data.DefaultReminders]). Ohne Ziel bleibt die Stimmung NEUTRAL,
 *    egal wie dieser Schalter steht. Sie kann also fruehestens in dem Moment sichtbar werden,
 *    in dem jemand selbst "das will ich 4x am Tag" eingetragen hat - und dann ist sie genau
 *    die Rueckmeldung, um die er gebeten hat.
 *
 * Damit war "aus" nur noch die Voreinstellung, die die zentrale Rueckmeldung der App auch dann
 * unterdrueckt, wenn der Nutzer sie sich ausdruecklich gewuenscht hat.
 *
 * **Fuer bestehende Installationen** heisst das: Wer den Schalter nie angefasst hat, bekommt die
 * Stimmung jetzt - aber nur, wenn er ein Tagesziel gesetzt hat. Wer sie ausdruecklich
 * ausgeschaltet hat, behaelt das: der gespeicherte Wert hat Vorrang vor dem Standard.
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
