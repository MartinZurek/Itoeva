package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.matrix.PlayAmbientActivity

/**
 * **Was der Nutzer dem Begleiter ueber sich gesagt hat.**
 *
 * Bis hierher lief die Auskunft nur in eine Richtung: Er berichtete, der Nutzer las. Ein Gegenüber
 * ist aber jemand, der auch etwas wissen WILL - und der sich merkt, was man ihm gesagt hat. Genau
 * das fehlte: Alles, was die App ueber ihren Nutzer wusste, hatte sie sich aus seinem Verhalten
 * zusammengerechnet. Gefragt hat sie nie.
 *
 * **Zwei Fragen, und beide aendern etwas.** Das ist die Bedingung, unter der eine Frage ueberhaupt
 * gestellt werden darf: Eine Antwort, die folgenlos in einer Datei liegt, ist eine Umfrage, und
 * eine Umfrage ist das Gegenteil eines Gespraechs.
 *
 * - [focusTopic] veraendert, worauf er von sich aus zugeht (siehe DockScreen) und was er
 *   vorschlaegt. Wer sagt "achte auf mein Trinken", sieht ihn danach oefter zur Kueche gehen.
 * - [busyPhase] verschiebt das Zeitfenster jeder Erinnerung, die aus dem Gespraech heraus
 *   entsteht (siehe [PlayTalk.presetFor]). Wer abends Zeit hat, bekommt keine Erinnerung, die
 *   morgens um neun anfaengt.
 *
 * **In den Einstellungen und nicht in der Datenbank.** Es sind zwei Werte, kein Verlauf - dieselbe
 * Ueberlegung wie bei Geldbeutel und Vorrat ([com.notime.glyphsim.matrix.PlayWallet]). Eine
 * Tabelle dafuer waere eine Migration fuer zwei Zeilen.
 */
object PlayUserProfile {

    private const val PREFS = "play_user_profile"
    private const val KEY_FOCUS = "focus_topic"
    private const val KEY_BUSY = "busy_phase"
    private const val KEY_ASKED = "asked"
    private const val KEY_PURPOSE = "purpose"
    private const val KEY_WEEKEND = "weekend"

    /** Worauf er besonders achten soll - `null`, solange nicht gefragt oder nicht beantwortet. */
    fun focusTopic(context: Context): AnimationType? =
        prefs(context).getString(KEY_FOCUS, null)?.let { stored ->
            runCatching { AnimationType.valueOf(stored) }.getOrNull()
        }

    /** Wann der Nutzer am ehesten Zeit hat. */
    fun busyPhase(context: Context): PlayAmbientActivity.DayPhase? =
        prefs(context).getString(KEY_BUSY, null)?.let { stored ->
            runCatching { PlayAmbientActivity.DayPhase.valueOf(stored) }.getOrNull()
        }

    fun setFocusTopic(context: Context, topic: AnimationType) {
        prefs(context).edit().putString(KEY_FOCUS, topic.name).apply()
        markAsked(context, PlayTalk.Ask.FOCUS)
    }

    fun setBusyPhase(context: Context, phase: PlayAmbientActivity.DayPhase) {
        prefs(context).edit().putString(KEY_BUSY, phase.name).apply()
        markAsked(context, PlayTalk.Ask.TIME)
    }

    /**
     * Ob eine Frage schon gestellt und beantwortet wurde.
     *
     * **Gemerkt wird das Fragen, nicht nur das Antworten.** Wer eine Frage weggewischt hat, soll
     * sie nicht beim naechsten Oeffnen wieder vorfinden - das waere kein Gespraech mehr, sondern
     * Draengeln. Sie laesst sich jederzeit ueber die Frage "was weisst du ueber mich" wieder
     * aufnehmen.
     */
    fun wasAsked(context: Context, ask: PlayTalk.Ask): Boolean =
        prefs(context).getStringSet(KEY_ASKED, emptySet()).orEmpty().contains(ask.name)

    fun markAsked(context: Context, ask: PlayTalk.Ask) {
        val current = prefs(context).getStringSet(KEY_ASKED, emptySet()).orEmpty()
        prefs(context).edit().putStringSet(KEY_ASKED, current + ask.name).apply()
    }

    /** Wozu der Nutzer ihn benutzt - bestimmt die Reihenfolge seiner Vorschlaege. */
    fun purpose(context: Context): PlayTalk.Purpose? =
        prefs(context).getString(KEY_PURPOSE, null)?.let { stored ->
            runCatching { PlayTalk.Purpose.valueOf(stored) }.getOrNull()
        }

    fun setPurpose(context: Context, purpose: PlayTalk.Purpose) {
        prefs(context).edit().putString(KEY_PURPOSE, purpose.name).apply()
        markAsked(context, PlayTalk.Ask.PURPOSE)
    }

    /**
     * Ob neue Erinnerungen auch am Wochenende gelten sollen.
     *
     * Standard ist ja - so war es immer, und wer nicht gefragt wurde, soll nichts anderes bekommen
     * als bisher.
     */
    fun includesWeekend(context: Context): Boolean =
        prefs(context).getBoolean(KEY_WEEKEND, true)

    fun setIncludesWeekend(context: Context, includes: Boolean) {
        prefs(context).edit().putBoolean(KEY_WEEKEND, includes).apply()
        markAsked(context, PlayTalk.Ask.WEEKEND)
    }

    /** Die Wochentage, mit denen eine neue Erinnerung angelegt wird. */
    fun daysMask(context: Context): Int =
        if (includesWeekend(context)) PlayTalk.EVERY_DAY_MASK else PlayTalk.WEEKDAYS_MASK

    /** Alles vergessen - fuer den Fall, dass sich jemand anders entscheidet. */
    fun forget(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
