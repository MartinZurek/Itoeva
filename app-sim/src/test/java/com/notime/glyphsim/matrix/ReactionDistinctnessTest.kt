package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Wieviele Knoten spielen Bild fuer Bild dasselbe wie ein Geschwister?**
 *
 * Der Fingerabdruck-Test daneben ([ReactionFingerprintTest]) beantwortet "hat sich etwas
 * unbeabsichtigt VERSCHOBEN". Er kann nicht beantworten, was am 2026-09-04 mit dem Vorschau-
 * Werkzeug (`tools/reaction-preview`) sichtbar wurde: dass viele Knoten mit Motiv eine Reaktion
 * mit einem Geschwister teilten. Fuenf davon lagen allein unter `sport/ballsport`.
 *
 * Das ist kein Fehler im Code, sondern der Preis der Vererbung: Ein Blatt ohne eigene Choreografie
 * erbt die Gruppen-Antwort seiner Untergruppe, und die ist absichtlich requisitenfrei (siehe
 * [AvatarReactions]). Mehrere Geschwister erben damit dieselbe. Solange nur die Uhr Reaktionen
 * ausloeste, fiel das kaum auf - seit eine Freischaltung im Alltag sichtbar wird (SKILLBAUM.md
 * P15), ist es die Belohnung selbst, die unsichtbar bleibt.
 *
 * Seit NT-084 ist die richtige Zahl null. Der Test ist damit keine fortzuschreibende Bestandszahl
 * mehr, sondern eine harte Vollstaendigkeitsregel: Jedes gezeichnete Motiv muss sichtbar seine
 * eigene Antwort behalten.
 */
class ReactionDistinctnessTest {

    private val nodesWithMotif = AnimationTree.nodes.filter { it.motif != null }

    /** Bilder UND Standzeiten - dieselbe Auffassung von "gleich" wie im Fingerabdruck-Test. */
    private fun shapeOf(nodeId: String): String {
        val r = AvatarAnimations.reactionFor(AvatarSpecies.PUFFLING, ReactionTrigger.Node(nodeId))
        return r.frames.joinToString(";") { f -> f.joinToString(",") } + "|" + r.holdsMs.joinToString(",")
    }

    private fun clusters(): List<List<String>> {
        val byShape = LinkedHashMap<String, MutableList<String>>()
        for (node in nodesWithMotif) byShape.getOrPut(shapeOf(node.id)) { mutableListOf() }.add(node.id)
        return byShape.values.filter { it.size > 1 }.map { it.toList() }
    }

    @Test
    fun `kein Motiv teilt seine Reaktion mit einem Geschwister`() {
        val geteilt = clusters().sumOf { it.size }
        assertEquals(
            "Diese Knoten teilen ihre Reaktion mit einem Geschwister: ${clusters()}",
            0,
            geteilt
        )
    }

    /**
     * Die Hauptgruppe `naehe` war nach dem Ballsport der dichteste Bereich: `freunde` und seine
     * drei Blaetter identisch, `tiere` und zwei seiner drei Blaetter ebenso. Seit P17 hat sie neun
     * Knoten und neun Reaktionen - die erste Gruppe im Baum, in der sich kein Knoten mehr mit einem
     * anderen deckt.
     */
    @Test
    fun `in der Gruppe naehe spielt kein Knoten dasselbe wie ein anderer`() {
        val ids = AnimationTree.nodes
            .filter { it.motif != null && AnimationTree.fallbackChain(it.id).last() == "naehe" }
            .map { it.id }
        assertEquals("naehe hat 9 gezeichnete Knoten", 9, ids.size)
        assertEquals("und muss 9 verschiedene Reaktionen haben", ids.size, ids.map { shapeOf(it) }.toSet().size)
    }

    /**
     * Die vier Blaetter unter `sport/ballsport` waren der dichteste Klumpen: Kopf und alle vier
     * Blaetter identisch. P16 hat ihnen eigene Bahnen gegeben - Bogen, Senkrechte, flaches Auf und
     * Ab, Waagerechte (siehe [AvatarMotifReactions]).
     */
    @Test
    fun `die vier Ballsport-Blaetter unterscheiden sich voneinander und vom Kopf`() {
        val ids = listOf(
            "sport/ballsport",
            "sport/ballsport/basketball",
            "sport/ballsport/trophy",
            "sport/ballsport/dribbling",
            "sport/ballsport/schuss"
        )
        val shapes = ids.map { shapeOf(it) }
        assertEquals("alle fuenf muessen verschieden sein", ids.size, shapes.toSet().size)
    }

    /**
     * Eine Hauptgruppe traegt einen eingebauten Typ und spielt dessen ausgespielte Handlung. Zwei
     * Hauptgruppen duerfen deshalb nie dasselbe spielen - das waere ein vertauschtes Thema.
     */
    @Test
    fun `keine zwei Hauptgruppen spielen dieselbe Reaktion`() {
        val roots = AnimationTree.roots().map { it.id }
        assertEquals(roots.size, roots.map { shapeOf(it) }.toSet().size)
    }

    /** Ein Klumpen aus mehr als vier Knoten waere ein ganzer Ast ohne erkennbaren Inhalt. */
    @Test
    fun `kein Klumpen ist groesser als vier Knoten`() {
        val groesster = clusters().maxOfOrNull { it.size } ?: 0
        assertTrue("groesster Klumpen: $groesster", groesster <= 4)
    }
}
