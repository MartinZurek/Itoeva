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

}
