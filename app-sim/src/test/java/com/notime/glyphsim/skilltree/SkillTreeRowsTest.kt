package com.notime.glyphsim.skilltree

import com.notime.glyphcore.data.AnimationTree
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Prueft die Zustaende des Baumbildschirms ([SkillTreeRows]).
 *
 * Die Verwechslung, um die es hier geht, ist die zwischen "als Naechstes dran" und "gesperrt".
 * Sie faellt beim blossen Ansehen kaum auf - beide sind nicht anwaehlbar -, veraendert aber, was
 * der Bildschirm dem Nutzer ueber seinen naechsten Schritt sagt.
 */
class SkillTreeRowsTest {

    private val start = UnlockOffers.startingNodes().toSet()

    private fun stateOf(nodeId: String, unlocked: Set<String> = start): NodeState =
        SkillTreeRows.build(unlocked).first { it.node.id == nodeId }.state

    @Test
    fun `jeder Knoten des Baums bekommt genau eine Zeile`() {
        assertEquals(AnimationTree.nodes.size, SkillTreeRows.build(start).size)
    }

    @Test
    fun `am Anfang sind die neun Hauptgruppen offen`() {
        for (root in AnimationTree.roots()) {
            assertEquals(root.id, NodeState.UNLOCKED, stateOf(root.id))
        }
    }

    @Test
    fun `eine Untergruppe steht als Naechstes an, ihre Blaetter noch nicht`() {
        assertEquals(NodeState.AVAILABLE, stateOf("sport/ballsport"))
        assertEquals(NodeState.LOCKED, stateOf("sport/ballsport/basketball"))
    }

    @Test
    fun `mit der Untergruppe ruecken ihre Blaetter nach`() {
        val offen = start + "sport/ballsport"
        assertEquals(NodeState.UNLOCKED, stateOf("sport/ballsport", offen))
        assertEquals(NodeState.AVAILABLE, stateOf("sport/ballsport/basketball", offen))
    }

    /**
     * Ein ungezeichneter Knoten ist NIE "als Naechstes dran", auch wenn sein Elternknoten offen
     * ist - er kann gar nicht angeboten werden. Wuerde er als AVAILABLE erscheinen, warteten
     * Nutzer auf eine Freischaltung, die nie kommt.
     */
    @Test
    fun `ein ungezeichneter Knoten ist nie als Naechstes dran`() {
        val offen = start + "sport/ballsport"
        assertEquals(NodeState.AVAILABLE, stateOf("sport/ballsport/basketball", offen))
        assertEquals(NodeState.PENDING_ART, stateOf("sport/ballsport/dribbling", offen))
    }

    @Test
    fun `alle ungezeichneten Knoten tragen denselben Zustand`() {
        val rows = SkillTreeRows.build(start)
        val ungezeichnet = AnimationTree.pendingArtwork().map { it.id }.toSet()
        assertEquals(
            "Diese ungezeichneten Knoten haben einen anderen Zustand bekommen",
            emptyList<String>(),
            rows.filter { it.node.id in ungezeichnet && it.state != NodeState.PENDING_ART }
                .map { it.node.id }
        )
    }

    @Test
    fun `ein Abschnitt zeigt nur seine eigene Hauptgruppe`() {
        val rows = SkillTreeRows.forRoot("sport", start)
        assertEquals(
            emptyList<String>(),
            rows.map { it.node.id }.filterNot { it == "sport" || it.startsWith("sport/") }
        )
        assertEquals("sport", rows.first().node.id)
    }

    @Test
    fun `der Fortschritt einer Gruppe zaehlt offene gegen alle`() {
        val (offen, gesamt) = SkillTreeRows.progressFor("sport", start)
        assertEquals("Nur die Hauptgruppe selbst ist offen", 1, offen)
        assertEquals("Sport hat 1 Kopf, 2 Untergruppen und 8 Blaetter", 11, gesamt)

        val (offenSpaeter, _) = SkillTreeRows.progressFor("sport", start + "sport/ballsport")
        assertEquals(2, offenSpaeter)
    }

    /** Ist alles offen, darf keine Zeile mehr einen anderen Zustand tragen. */
    @Test
    fun `bei vollstaendig offenem Baum ist alles freigeschaltet`() {
        val alles = AnimationTree.nodes.map { it.id }.toSet()
        assertEquals(
            emptyList<NodeState>(),
            SkillTreeRows.build(alles).map { it.state }.filterNot { it == NodeState.UNLOCKED }
        )
    }
}
