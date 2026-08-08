package com.notime.glyphsim.ui

import android.content.Context

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
    private const val PREFS_NAME = "onboarding_prefs"
    private const val KEY_TAPPED_AVATAR = "tapped_avatar"
    private const val KEY_TAPPED_CLOCK = "tapped_clock"

    fun hasTappedAvatar(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TAPPED_AVATAR, false)

    fun markAvatarTapped(context: Context) {
        prefs(context).edit().putBoolean(KEY_TAPPED_AVATAR, true).apply()
    }

    /**
     * Wie [hasTappedAvatar], nur fuer die Uhr: ein Antippen wechselt sofort und ohne Rueckfrage
     * in den Dock-Modus (staendig eingeschaltetes Display, spuerbar mehr Akkuverbrauch als im
     * Normalbetrieb) - ohne einmaligen Hinweis vorher waere das eine unerklaerte Ueberraschung
     * beim ersten zufaelligen Antippen der Uhr.
     */
    fun hasTappedClock(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TAPPED_CLOCK, false)

    fun markClockTapped(context: Context) {
        prefs(context).edit().putBoolean(KEY_TAPPED_CLOCK, true).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
