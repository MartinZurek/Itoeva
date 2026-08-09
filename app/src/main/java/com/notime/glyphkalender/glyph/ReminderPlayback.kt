package com.notime.glyphkalender.glyph

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Macht sichtbar, ob gerade eine Erinnerungsanimation laeuft - und erlaubt, sie abzubrechen.
 *
 * ## Warum es das gibt
 *
 * Das Abbrechen einer laufenden Erinnerung haengt bisher an genau einem Faden: dem
 * "Beenden"-Button in der Benachrichtigung des Vordergrunddienstes. Das war eine bewusste
 * Entscheidung, nachdem zwei direktere Wege (Naeherungssensor, Antippen der Matrix) sich als
 * unzuverlaessig erwiesen hatten - siehe [ReminderGlyphService].
 *
 * Dieser eine Faden reisst aber, sobald der Nutzer die Benachrichtigungsberechtigung ablehnt.
 * Seit Android 13 ist `POST_NOTIFICATIONS` eine Laufzeitberechtigung, und ohne sie zeigt Android
 * auch die Benachrichtigung eines Vordergrunddienstes nicht an. Der Dienst laeuft dann - nur
 * sieht man ihn nicht und kann ihn nicht beenden. Es gaebe schlicht keinen Weg mehr, eine
 * Animation vorzeitig zu stoppen.
 *
 * Deshalb hier ein zweiter, von Benachrichtigungen unabhaengiger Weg: die App selbst zeigt an,
 * dass etwas laeuft, und bietet dasselbe Beenden an.
 *
 * ## Warum ein Prozess-Singleton
 *
 * Derselbe Grund wie bei [GlyphMatrixConnection] nebenan: Dienst und Oberflaeche leben in
 * verschiedenen Bausteinen desselben Prozesses und haben keine gemeinsame Instanz, ueber die sie
 * sich verstaendigen koennten. Ein Binder waere fuer eine einzige Zeichenkette unverhaeltnismaessig.
 *
 * Der Zustand ist bewusst NICHT dauerhaft: er beschreibt, was in **diesem** Moment laeuft. Stirbt
 * der Prozess, laeuft auch nichts mehr.
 */
object ReminderPlayback {

    private val playing = MutableStateFlow<String?>(null)

    /**
     * Bezeichnung der gerade abgespielten Erinnerung, oder `null`, wenn nichts laeuft.
     * Aus der Oberflaeche zu beobachten (siehe `ReminderScreen`).
     */
    val nowPlaying: StateFlow<String?> = playing.asStateFlow()

    /** Vom Dienst aufzurufen, wenn eine Erinnerung tatsaechlich auf der Matrix erscheint. */
    fun started(title: String) {
        playing.value = title
    }

    /**
     * Vom Dienst aufzurufen, wenn nichts mehr laeuft.
     *
     * Gehoert in ein `finally`: bricht das Abspielen mit einem Fehler ab, bliebe der Hinweis in
     * der Oberflaeche sonst fuer immer stehen und boete ein Beenden fuer etwas an, das laengst
     * vorbei ist.
     */
    fun finished() {
        playing.value = null
    }

    /**
     * Bricht die laufende Erinnerung ab und verwirft die Warteschlange - dasselbe, was der
     * "Beenden"-Button in der Benachrichtigung tut, nur aus der App heraus.
     */
    fun dismiss(context: Context) {
        context.startService(
            Intent(context, ReminderGlyphService::class.java).setAction(ReminderGlyphService.ACTION_DISMISS)
        )
    }
}
