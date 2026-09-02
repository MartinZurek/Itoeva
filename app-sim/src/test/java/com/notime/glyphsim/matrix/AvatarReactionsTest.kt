package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationMotif
import com.notime.glyphcore.data.AnimationTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Prueft die Aufloesung einer Reaktion ueber den Animations-Baum ([AvatarReactions]).
 *
 * Waehrend [ReactionFingerprintTest] festhaelt, dass sich am ERGEBNIS nichts geaendert hat, geht es
 * hier um die Regel dahinter - und vor allem um die eine Unterscheidung, die beim Umbau erst
 * auffiel: Eine Antwort, die einem MOTIV gehoert, darf nicht an die Knoten darunter vererbt
 * werden, weil sie die Requisite ihres Motivs mitbringt.
 */
class AvatarReactionsTest {

    private val body = AvatarBodies.forSpecies(AvatarSpecies.PUFFLING)

    @Test
    fun `ein Motiv findet seine eigene Antwort ueber seinen Knoten`() {
        assertNotNull(
            "Bubble hat eine eigene Choreografie und muss sie ueber ihren Knoten finden",
            AvatarReactions.forNode("achtsamkeit/beobachten/bubble", body)
        )
    }

    @Test
    fun `alle dreissig motiveigenen Antworten sind ueber ihren Knoten erreichbar`() {
        val ohneAntwort = AvatarSignatureReactions.labels.filter { label ->
            val nodeId = AnimationTree.nodeIdFor(label)
            nodeId == null || AvatarReactions.forNode(nodeId, body) == null
        }
        assertEquals(
            "Diese Motive haben eine Choreografie, sind ueber den Baum aber nicht erreichbar",
            emptyList<String>(),
            ohneAntwort
        )
    }

    /**
     * **Der Fall, der die Regel hervorgebracht hat.**
     *
     * `lernen/lesen` traegt das Motiv *Scroll* und damit dessen Choreografie: der Blick wandert
     * zeilenweise ueber eine Schriftrolle, dann "verstanden". Das Blatt `idea` darunter hat keine
     * eigene Antwort. Wuerde es Scrolls erben, zeigte der Glyph eine Gluehbirne, waehrend der
     * Avatar daneben eine Schriftrolle liest - die Requisite gehoert zu Scroll, nicht zu der
     * Stelle im Baum, an der Scroll zufaellig sitzt.
     *
     * Vererbt werden duerfen nur Antworten, die ausdruecklich fuer eine Stelle im Baum entworfen
     * wurden (siehe `AvatarReactions.groupAnswer`).
     */
    @Test
    fun `eine motiveigene Antwort wird nicht nach unten vererbt`() {
        val scroll = AvatarSignatureReactions.forNode("lernen/lesen", body)
        assertNotNull("Scroll sitzt auf lernen/lesen und hat dort eine Antwort", scroll)

        val idea = AvatarReactions.forNode("lernen/lesen/idea", body)
        assertNotNull("Seit P9 erbt idea die Gruppen-Antwort von lernen/lesen", idea)
        assertNotEquals(
            "idea darf Scrolls Schriftrolle nicht erben - die Requisite gehoert dem Motiv",
            scroll,
            idea
        )
    }

    /**
     * **Seit P9 erbt jedes Blatt.** Vorher war die Gruppen-Weiche leer, und dieser Test hielt fest,
     * dass niemand etwas erbt; jetzt haelt er die Gegenrichtung fest: Kein Knoten steht mehr ohne
     * Antwort da.
     *
     * Ohne diese Zusicherung faellt ein Blatt still auf die generische Freuden-Reaktion zurueck -
     * es passiert also etwas, es passt nur nicht, und genau das faellt beim Zuschauen nicht auf.
     */
    @Test
    fun `jeder Knoten des Baums hat eine Antwort`() {
        val ohneAntwort = AnimationTree.nodes
            // Die elf Knoten mit eingebautem Typ spielen die ausgespielte Handlung ihres Themas;
            // die liegt in AvatarAnimations und nicht hier (siehe AvatarReactions.groupAnswer).
            .filterNot { it.motif is AnimationMotif.Builtin }
            .filter { AvatarReactions.forNode(it.id, body) == null }
            .map { it.id }
        assertEquals("Diese Knoten haben keine Antwort", emptyList<String>(), ohneAntwort)
    }

    /**
     * **Die Idea-Regel, verallgemeinert.**
     *
     * Der Einzelfall darueber pruefte `idea` unter Scroll. Dieselbe Falle gibt es ueberall dort, wo
     * eine Untergruppe ein eigenes Motiv traegt: Butterfly auf `achtsamkeit/beobachten`, Cloud auf
     * `ruhe/schlafen`, Puzzle auf `lernen/knobeln`. Wuerde deren Antwort nach unten durchschlagen,
     * traege der Avatar die Requisite des Elternmotivs zu einem ganz anderen Motiv auf dem Glyph.
     *
     * Geprueft wird jedes Blatt ohne eigene Antwort, dessen Elternknoten eine hat.
     */
    @Test
    fun `kein Blatt erbt die Motiv-Antwort seines Elternknotens`() {
        val betroffen = AnimationTree.nodes
            .filter { it.depth == 3 && AvatarSignatureReactions.forNode(it.id, body) == null }
            .mapNotNull { blatt ->
                val elternAntwort = blatt.parentId
                    ?.let { AvatarSignatureReactions.forNode(it, body) }
                    ?: return@mapNotNull null
                if (AvatarReactions.forNode(blatt.id, body) == elternAntwort) blatt.id else null
            }
        assertEquals(
            "Diese Blaetter erben die Requisite ihres Elternmotivs",
            emptyList<String>(),
            betroffen
        )
    }

    @Test
    fun `ein unbekannter Pfad spielt nichts Beliebiges ab`() {
        assertNull(AvatarReactions.forNode("sport/ballsport/gibtesnicht", body))
        assertNull(AvatarReactions.forNode("", body))
        assertNull(AvatarReactions.forNode(null, body))
    }

    /**
     * Knoten, deren Zeichnung noch fehlt, haben auch keine Choreografie - sie duerfen aber
     * abgefragt werden, ohne dass etwas bricht.
     */
    @Test
    fun `ein ungezeichneter Knoten laesst sich gefahrlos fragen`() {
        for (node in AnimationTree.pendingArtwork()) {
            assertNull("${node.id} hat noch kein Motiv und darf keine Antwort liefern",
                AvatarReactions.forNode(node.id, body))
        }
    }
}
