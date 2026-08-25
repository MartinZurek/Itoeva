package com.notime.glyphsim.skilltree

import com.notime.glyphcore.data.AnimationTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft die Neigungsrechnung ([BranchAffinity]) gegen erfundene Historien.
 *
 * Die Regeln hier sind alle eine Behauptung ueber den Nutzer, und jede einzelne kann daneben
 * liegen: Wer zaehlt, was jemand ignoriert hat, misst das Gegenteil von Interesse; wer alles
 * gleich gewichtet, laesst einen alten Schwerpunkt fuer immer stehen. Deshalb sind sie hier
 * einzeln festgehalten statt nur "die Zahlen sehen plausibel aus".
 */
class BranchAffinityTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private fun answer(nodeId: String, ageDays: Long) =
        BranchAffinity.Answer(nodeId, now - ageDays * day)

    @Test
    fun `ohne Historie stehen alle neun Hauptgruppen auf null`() {
        val scores = BranchAffinity.scores(emptyList(), now)
        assertEquals(9, scores.size)
        assertTrue("Ohne Antworten darf nichts ausschlagen", scores.values.all { it == 0.0 })
    }

    /** Ein Blatt zaehlt fuer seine Hauptgruppe - sonst mueesste die Rechnung 79 Knoten unterscheiden. */
    @Test
    fun `ein Blatt zaehlt auf seine Hauptgruppe ein`() {
        val scores = BranchAffinity.scores(listOf(answer("sport/ballsport/basketball", 0)), now)
        assertEquals(1.0, scores.getValue("sport"), 0.0001)
        assertEquals(0.0, scores.getValue("ruhe"), 0.0001)
    }

    @Test
    fun `eine Antwort von heute zaehlt voll, eine zwei Wochen alte halb`() {
        val frisch = BranchAffinity.scores(listOf(answer("sport", 0)), now).getValue("sport")
        val alt = BranchAffinity.scores(listOf(answer("sport", 14)), now).getValue("sport")
        assertEquals(1.0, frisch, 0.0001)
        assertEquals(0.5, alt, 0.0001)
    }

    /**
     * **Der Punkt, um den es bei der Abschwaechung geht.** Ohne sie wuerde ein halbes Jahr alter
     * Schwerpunkt jede aktuelle Gewohnheit ueberstimmen, und der Baum bliebe fuer immer dort
     * stehen, wo er einmal angefangen hat.
     */
    @Test
    fun `frische Antworten schlagen eine groessere Zahl alter`() {
        val answers = List(20) { answer("ruhe", 120) } + List(3) { answer("sport", 0) }
        assertEquals("sport", BranchAffinity.ranked(answers, now).first())
    }

    @Test
    fun `die Rangfolge fuehrt den staerksten Zweig zuerst`() {
        val answers = listOf(
            answer("lernen/lesen/idea", 1),
            answer("lernen", 2),
            answer("sport", 1)
        )
        val ranked = BranchAffinity.ranked(answers, now)
        assertEquals("lernen", ranked[0])
        assertEquals("sport", ranked[1])
    }

    /**
     * Ohne stabile Regel bei Gleichstand saehe ein frischer Spielstand - alle neun auf 0.0 - bei
     * jedem Aufruf anders aus, und dieselbe Historie ergaebe je nach Reihenfolge der Datenbankzeilen
     * ein anderes Angebot.
     */
    @Test
    fun `bei Gleichstand entscheidet die Reihenfolge im Baum, nicht der Zufall`() {
        val treeOrder = AnimationTree.roots().map { it.id }
        assertEquals(treeOrder, BranchAffinity.ranked(emptyList(), now))
        assertEquals(treeOrder, BranchAffinity.ranked(emptyList(), now))
    }

    /** Ein Knoten, den es nicht gibt, darf die Rechnung nicht kippen. */
    @Test
    fun `ein unbekannter Knoten wird uebergangen`() {
        val scores = BranchAffinity.scores(
            listOf(answer("gibt/es/nicht", 0), answer("sport", 0)),
            now
        )
        assertEquals(1.0, scores.getValue("sport"), 0.0001)
    }

    /** Eine Antwort aus der Zukunft (Uhr verstellt) darf nicht mehr als voll zaehlen. */
    @Test
    fun `eine Antwort mit Zeitstempel in der Zukunft zaehlt hoechstens voll`() {
        val scores = BranchAffinity.scores(listOf(answer("sport", -30)), now)
        assertEquals(1.0, scores.getValue("sport"), 0.0001)
    }
}
