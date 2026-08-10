package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType

/**
 * **Woran das Wesen sich erinnert** - die gemeinsame Geschichte als Momente statt als Zahlen.
 *
 * ## Wozu das neben dem Pflegebuch
 *
 * Das Pflegebuch beantwortet "wie viel": zwoelf Reaktionen heute, achtzig Prozent beim Trinken,
 * drei von vier Zielen. Das ist eine Auswertung, und eine Auswertung kann schlecht ausfallen.
 *
 * Hier steht das andere: **wann etwas zum ersten Mal war.** Ein erstes Mal kann man nicht
 * verfehlen, nicht verlieren und nicht unterbieten - es ist entweder passiert oder noch nicht.
 * Deshalb gibt es in dieser Liste keinen Eintrag, der jemandem etwas vorwirft, und keinen, der
 * verschwindet, wenn eine Woche schlecht laeuft.
 *
 * Das ist derselbe Gedanke wie bei [CompanionChapter], nur konkreter: Dort geht es darum, wie
 * lange ihr euch kennt, hier darum, was dabei passiert ist.
 *
 * ## Was ausdruecklich nicht hineingehoert
 *
 * Alles, was sich zaehlen oder vergleichen laesst - Summen, Quoten, Serien, "seit X Tagen". Auch
 * Luecken nicht: Ein "das letzte Mal ist lange her" waere ein Vorwurf in Form eines Datums.
 *
 * Und nichts aus dem Spielmodus. Dessen Themen werden gewuerfelt (siehe [PlayModeRoll]); "das
 * erste Mal getrunken" waere dort der Zufall und nicht der Nutzer.
 */
data class Memory(
    val kind: Kind,
    /** Wann es passiert ist. */
    val epochMillis: Long,
    /** Bei [Kind.FIRST_TOPIC]: worum es ging. Sonst `null`. */
    val topic: AnimationType? = null,
    /** Bei einer Bibliotheks-Animation deren Name statt eines Themas. */
    val libraryLabel: String? = null,
    /** Bei [Kind.CHAPTER]: welches Kapitel begonnen hat. */
    val chapter: CompanionChapter? = null
) {
    /**
     * [sortRank] entscheidet, was bei GLEICHEM Zeitpunkt oben steht - am ersten Tag fallen
     * Kennenlernen, erstes Kapitel und erstes Thema zusammen. Ausdruecklich als Zahl und nicht
     * ueber `ordinal`: Sonst haenge die Anzeige daran, in welcher Reihenfolge die Eintraege
     * zufaellig deklariert sind, und ein spaeteres Umsortieren des Enums aenderte still den
     * Bildschirm.
     *
     * Die Reihenfolge erzaehlt: erst dass ihr euch begegnet seid, dann in welchem Kapitel, dann
     * was ihr getan habt.
     */
    enum class Kind(val sortRank: Int) {
        /** Der erste Moment, den ihr geteilt habt. */
        MET(0),

        /** Das erste Mal, dass ein bestimmtes Thema beantwortet wurde. */
        FIRST_TOPIC(2),

        /** Der Beginn eines Kapitels (siehe [CompanionChapter]). */
        CHAPTER(1)
    }
}

/** Ein Thema und wann es zum ersten Mal beantwortet wurde. */
data class TopicFirst(
    val topic: AnimationType?,
    val libraryLabel: String?,
    val firstEpochMillis: Long
)

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/**
 * Baut die Liste der Erinnerungen - **rein, ohne Datenbank und ohne Uhr im Hintergrund.**
 *
 * [firstSharedMomentMillis] ist `null`, solange es keinen gemeinsamen Moment gibt; dann ist die
 * Liste leer, auch wenn [firstPerTopic] wider Erwarten etwas enthielte. Ohne Anfang keine
 * Geschichte.
 *
 * ## Warum aufsteigend sortiert
 *
 * Neueste zuerst waere die uebliche Wahl fuer eine Liste von Ereignissen - hier ist sie falsch.
 * Diese Liste erzaehlt etwas, und eine Geschichte liest man von vorn. Wer nach unten scrollt,
 * kommt in der Gegenwart an; das ist genau die Richtung, in die eine Beziehung waechst.
 *
 * ## Die Kapitel entstehen aus dem Anfang
 *
 * Sie werden nicht gespeichert, sondern gerechnet: [CompanionChapter] haengt allein daran, wie
 * lange ihr euch kennt, also steht der Beginn jedes Kapitels von vornherein fest. Aufgenommen
 * werden nur die, die zum Zeitpunkt [nowMillis] auch erreicht sind - eine Vorschau auf das
 * naechste Kapitel waere ein Ziel, und Ziele haben hier nichts zu suchen.
 */
fun buildMemories(
    firstSharedMomentMillis: Long?,
    firstPerTopic: List<TopicFirst>,
    nowMillis: Long
): List<Memory> {
    val start = firstSharedMomentMillis ?: return emptyList()

    val chapters = listOf(
        CompanionChapter.SETTLING to CompanionChapter.SETTLING_AFTER_DAYS,
        CompanionChapter.FAMILIAR to CompanionChapter.FAMILIAR_AFTER_DAYS,
        CompanionChapter.LONGSTANDING to CompanionChapter.LONGSTANDING_AFTER_DAYS
    ).mapNotNull { (chapter, afterDays) ->
        val begin = start + afterDays * DAY_MILLIS
        if (begin > nowMillis) null
        else Memory(Memory.Kind.CHAPTER, epochMillis = begin, chapter = chapter)
    }

    val topics = firstPerTopic
        // Ein Eintrag ohne beides waere nicht darstellbar - das kann nur eine Altzeile sein,
        // deren Typ es nicht mehr gibt. Still auslassen statt eine leere Zeile zu zeigen.
        .filter { it.topic != null || it.libraryLabel != null }
        .map {
            Memory(
                kind = Memory.Kind.FIRST_TOPIC,
                epochMillis = it.firstEpochMillis,
                topic = it.topic,
                libraryLabel = it.libraryLabel
            )
        }

    return (listOf(Memory(Memory.Kind.MET, start)) + chapters + topics)
        // Bei gleichem Zeitpunkt zuerst das Kennenlernen, dann das Kapitel, dann die Themen -
        // sonst stuende am ersten Tag zufaellig mal das eine, mal das andere oben.
        .sortedWith(compareBy({ it.epochMillis }, { it.kind.sortRank }))
}
