package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationTree
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        assertNotNull(
            "Scroll sitzt auf lernen/lesen und hat dort eine Antwort",
            AvatarReactions.forNode("lernen/lesen", body)
        )
        assertNull(
            "idea darf Scrolls Schriftrolle nicht erben - es hat sein eigenes Motiv",
            AvatarReactions.forNode("lernen/lesen/idea", body)
        )
    }

    /**
     * Solange es keine Gruppen-Antworten gibt, darf KEIN Knoten etwas erben. Schlaegt das fehl,
     * ohne dass jemand bewusst eine Gruppen-Antwort hinterlegt hat, vererbt sich wieder eine
     * motiveigene Choreografie nach unten.
     */
    @Test
    fun `heute erbt kein Knoten eine Antwort von weiter oben`() {
        val erbend = AnimationTree.nodes
            .filter { AvatarSignatureReactions.forNode(it.id, body) == null }
            .filter { AvatarReactions.forNode(it.id, body) != null }
            .map { it.id }
        assertEquals(
            "Diese Knoten erben eine Antwort, obwohl sie keine eigene haben",
            emptyList<String>(),
            erbend
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
