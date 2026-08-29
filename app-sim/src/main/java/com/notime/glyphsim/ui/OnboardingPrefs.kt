package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphsim.settings.SettingsCatalog
import com.notime.glyphsim.settings.SettingsStore

/**
 * Merkt sich, ob der Nutzer den Avatar schon einmal angetippt hat.
 *
 * **Wozu:** der Avatar ist inzwischen der Einstieg in alles - Einfuehrung, gefuehrtes Einrichten,
 * KI-Import, Einstellungen (siehe [AvatarAssistantDialog]). Nur sieht man ihm das nicht an. Wer
 * die App zum ersten Mal oeffnet, sieht eine Uhr und eine Kreatur und muss erst auf die Idee
 * kommen, die Kreatur anzufassen. Ein einmaliger Hinweis loest das; danach waere er nur noch im
 * Weg, deshalb verschwindet er nach der ersten Beruehrung dauerhaft.
 */
object OnboardingPrefs {

    /**
     * Ob das Wesen sich schon einmal von selbst vorgestellt hat.
     *
     * **Warum es das ueberhaupt tut.** Der erste Start zeigte bisher eine Uhr, eine Kreatur und
     * eine Sprechblase. Wer nicht auf die Idee kam, die Kreatur anzufassen, hat nie erfahren,
     * worum es hier geht - und schon gar nicht das Wichtigste: dass hier nichts gezaehlt wird und
     * es nichts aufrechtzuerhalten gibt. Eine App, deren Kernversprechen man nur durch Ausprobieren
     * findet, hat es nicht gegeben.
     *
     * **Warum trotzdem kein Willkommens-Ablauf.** Die Einfuehrung gibt es schon, das Wesen haelt
     * sie, und sie ist jederzeit wieder aufrufbar. Ein zweiter Erklaer-Ort davor waere eine zweite
     * Quelle derselben Wahrheit - und die beiden liefen mit dem naechsten Umbau auseinander (siehe
     * die acht Texte, die Phase 5a einsammeln musste). Also fuehrt dasselbe Wesen wie immer, nur
     * ungefragt beim ersten Mal.
     *
     * Getrennt von [hasTappedAvatar]: die Begruessung laeuft von allein und darf den Hinweis
     * darauf, dass man den Avatar auch selbst antippen kann, nicht ersetzen.
     */
    fun hasBeenGreeted(context: Context): Boolean =
        SettingsStore.read(context, SettingsCatalog.Greeted)

    fun markGreeted(context: Context) {
        SettingsStore.write(context, SettingsCatalog.Greeted, true)
    }

    fun hasTappedAvatar(context: Context): Boolean =
        SettingsStore.read(context, SettingsCatalog.TappedAvatar)

    fun markAvatarTapped(context: Context) {
        SettingsStore.write(context, SettingsCatalog.TappedAvatar, true)
    }

    /**
     * Wie [hasTappedAvatar], nur fuer die Uhr: ein Antippen wechselt sofort und ohne Rueckfrage
     * in den Dock-Modus (staendig eingeschaltetes Display, spuerbar mehr Akkuverbrauch als im
     * Normalbetrieb) - ohne einmaligen Hinweis vorher waere das eine unerklaerte Ueberraschung
     * beim ersten zufaelligen Antippen der Uhr.
     */
    fun hasTappedClock(context: Context): Boolean =
        SettingsStore.read(context, SettingsCatalog.TappedClock)

    fun markClockTapped(context: Context) {
        SettingsStore.write(context, SettingsCatalog.TappedClock, true)
    }

    /**
     * Wie [hasTappedClock], nur fuer die vier Speicherplaetze rechts (siehe ActionSlots.kt): ohne
     * einen einmaligen Hinweis sieht man den leeren Plaetzen nicht an, dass sich eine Aktion von
     * der Uhr dorthin ziehen laesst statt nur auf den Avatar.
     */
    fun hasUsedActionSlot(context: Context): Boolean =
        SettingsStore.read(context, SettingsCatalog.UsedActionSlot)

    fun markActionSlotUsed(context: Context) {
        SettingsStore.write(context, SettingsCatalog.UsedActionSlot, true)
    }

    /**
     * Ob der Hinweis auf dem Baumbildschirm schon einmal zu sehen war (siehe SkillTreeDialog.kt).
     *
     * Noetig geworden, weil "auf einen hervorgehobenen Knoten tippen, um ihn freizuschalten"
     * sich der Oberflaeche selbst nicht ansieht - anders als beim Avatar oder der Uhr gibt es
     * hier keine Geste, auf die man von allein kommt, wenn man einen Skillpunkt uebrig hat und
     * nicht weiss, wo er hingehoert.
     */
    fun hasSeenSkillTreeHint(context: Context): Boolean =
        SettingsStore.read(context, SettingsCatalog.SkillTreeHintSeen)

    fun markSkillTreeHintSeen(context: Context) {
        SettingsStore.write(context, SettingsCatalog.SkillTreeHintSeen, true)
    }

}
