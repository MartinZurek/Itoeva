package com.notime.glyphsim.matrix

import com.notime.glyphsim.R

/**
 * **Wie weit ihr beide seid** - das Kapitel, in dem eure gemeinsame Geschichte gerade steht.
 *
 * ## Was das ausdruecklich NICHT ist
 *
 * Kein Fortschritt, den man erreicht. Kein Rang, keine Stufe, kein Balken. Es gibt nichts
 * freizuschalten, nichts zu verlieren und nichts, worin man besser sein koennte als gestern.
 *
 * Das ist keine Zurueckhaltung, sondern die Bedingung dafuer, dass es ueberhaupt hier stehen
 * darf. Sobald ein Kapitel von Leistung abhinge, waere es eine Serie mit anderem Namen: Wer zwei
 * Tage aussetzt, faellt zurueck - und genau dieses Gefuehl soll diese App nicht erzeugen. Die
 * Signatur von [chapterFor] ist die Zusage: sie bekommt **nur zwei Zeitpunkte** und kann daher
 * gar nicht bewerten, wie oft, wie zuverlaessig oder wie viel jemand getan hat.
 *
 * ## Was es dann ist
 *
 * Die schlichte Feststellung, wie lange ihr euch schon kennt - gerechnet ab dem ersten Moment,
 * den ihr wirklich geteilt habt (der ersten beantworteten Erinnerung, siehe
 * `AvatarFeedEventDao.firstAnsweredMillis`), nicht ab der Installation. Ein Wesen, das
 * gestern erst dazugekommen ist, soll nicht so reden, als waere es von Anfang an dabei gewesen.
 *
 * Zeit vergeht, ob man etwas tut oder nicht. Deshalb kann ein Kapitel nur wachsen, nie
 * zurueckfallen, und niemand kann es verfehlen. Es aendert, wie das Wesen ueber euch spricht -
 * mehr nicht, und das ist der ganze Punkt.
 *
 * ## Warum je Wesen und nicht je Nutzer
 *
 * Seit Phase 4b gehoert das Pflegebuch dem Wesen (siehe `ui/PresentCompanion`). Ein frisch
 * gewaehltes Wesen faengt deshalb wieder bei [ARRIVED] an - es war bei nichts davon dabei. Das
 * ist die sichtbarste Stelle, an der jene Entscheidung etwas bedeutet: **die Routinen gehoeren
 * dir, die gemeinsame Geschichte gehoert ihm.**
 */
enum class CompanionChapter(
    /** Der Name des Kapitels, wie er dem Nutzer gezeigt wird. */
    val labelRes: Int,
    /** Was das Wesen darueber sagt - in seiner eigenen Stimme, nicht als Statusmeldung. */
    val lineRes: Int
) {
    /** Noch kein gemeinsamer Moment. Auch der Zustand nach einem Wesenswechsel. */
    ARRIVED(R.string.chapter_arrived, R.string.chapter_arrived_line),

    /** Die ersten Tage. */
    SETTLING(R.string.chapter_settling, R.string.chapter_settling_line),

    /** Man kennt sich. */
    FAMILIAR(R.string.chapter_familiar, R.string.chapter_familiar_line),

    /** Laenger dabei. */
    LONGSTANDING(R.string.chapter_longstanding, R.string.chapter_longstanding_line);

    companion object {

        /**
         * Die Grenzen in Tagen.
         *
         * Bewusst grob und bewusst frueh: Der Unterschied soll in den ersten Wochen spuerbar sein,
         * in denen man eine App entweder behaelt oder nicht. Danach passiert lange nichts mehr -
         * ein Kapitel alle paar Monate waere ein Belohnungsplan, und genau den soll es nicht
         * geben.
         */
        const val SETTLING_AFTER_DAYS = 0
        const val FAMILIAR_AFTER_DAYS = 3
        const val LONGSTANDING_AFTER_DAYS = 14

        private const val DAY_MILLIS = 24L * 60 * 60 * 1000

        /**
         * Das Kapitel zum gegebenen Zeitpunkt.
         *
         * [firstSharedMomentMillis] ist `null`, solange es keinen gemeinsamen Moment gibt - dann
         * ist es [ARRIVED], egal wie lange die App schon installiert ist. Ein Wesen, mit dem man
         * noch nichts erlebt hat, hat keine Geschichte zu erzaehlen.
         *
         * Ein [nowMillis] VOR dem ersten Moment (verstellte Uhr, Zeitzonenwechsel, Zeitumstellung)
         * ergibt null Tage statt einer negativen Zahl. Das Kapitel faellt dadurch nicht zurueck -
         * eine gestellte Uhr darf keine gemeinsame Geschichte loeschen.
         */
        fun chapterFor(firstSharedMomentMillis: Long?, nowMillis: Long): CompanionChapter {
            if (firstSharedMomentMillis == null) return ARRIVED
            val days = ((nowMillis - firstSharedMomentMillis) / DAY_MILLIS).coerceAtLeast(0L)
            return when {
                days >= LONGSTANDING_AFTER_DAYS -> LONGSTANDING
                days >= FAMILIAR_AFTER_DAYS -> FAMILIAR
                else -> SETTLING
            }
        }
    }
}
