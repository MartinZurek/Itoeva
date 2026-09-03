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
     * **Seit P8 ist der Baum vollstaendig gezeichnet** - es gibt derzeit keinen Knoten im Zustand
     * [NodeState.PENDING_ART] mehr.
     *
     * Der Zustand bleibt trotzdem, und dieser Test auch: Er bewacht jetzt die Gegenrichtung. Kommt
     * ein neuer Knoten ohne Motiv dazu, faellt er hier auf, statt in der Zieh-Leiste zu landen und
     * beim Ziehen nichts zu zeigen.
     */
    @Test
    fun `derzeit wartet kein Knoten auf eine Zeichnung`() {
        val wartend = SkillTreeRows.build(start)
            .filter { it.state == NodeState.PENDING_ART }
            .map { it.node.id }
        assertEquals("Diese Knoten haben kein Motiv - siehe SKILLBAUM.md, P8", emptyList<String>(), wartend)
    }

    /**
     * Die Regel dahinter, unabhaengig vom aktuellen Bestand: Ein Knoten ohne Motiv ist nie "als
     * Naechstes dran", auch wenn sein Elternknoten offen ist - er kann gar nicht angeboten werden.
     * Wuerde er als AVAILABLE erscheinen, warteten Nutzer auf eine Freischaltung, die nie kommt.
     */
    @Test
    fun `ein Blatt mit Motiv rueckt nach, sobald seine Untergruppe offen ist`() {
        val offen = start + "sport/ballsport"
        assertEquals(NodeState.AVAILABLE, stateOf("sport/ballsport/basketball", offen))
        assertEquals(NodeState.AVAILABLE, stateOf("sport/ballsport/dribbling", offen))
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

    /**
     * Die Rueckmeldung nach einer Freischaltung haengt daran: Der Bildschirm klappt genau diese
     * Knoten auf, damit der neue Knoten sichtbar ist, statt in einem zugeklappten Ast zu
     * verschwinden.
     */
    @Test
    fun `die Vorfahren eines Blattes sind Untergruppe und Hauptgruppe, ohne es selbst`() {
        assertEquals(
            setOf("sport/ballsport", "sport"),
            SkillTreeRows.ancestorsOf("sport/ballsport/basketball")
        )
    }

    @Test
    fun `eine Hauptgruppe hat keine Vorfahren`() {
        assertEquals(emptySet<String>(), SkillTreeRows.ancestorsOf("sport"))
    }

    /**
     * Ein unbekannter Pfad darf keine erfundene Kette liefern - der Bildschirm wuerde sonst einen
     * Ast aufklappen, den es nicht gibt (siehe [AnimationTree.fallbackChain]).
     */
    @Test
    fun `ein unbekannter Knoten hat keine Vorfahren`() {
        assertEquals(emptySet<String>(), SkillTreeRows.ancestorsOf("gibt/es/nicht"))
    }

    /**
     * Die Testumgebung fuehrt Reaktionen vor. Ein Knoten ohne Motiv haette nichts zu zeigen, und
     * genau diese Knoten sind die, die [SkillTreeRows.build] als [NodeState.PENDING_ART] fuehrt -
     * die beiden Mengen duerfen nicht auseinanderlaufen.
     */
    @Test
    fun `vorfuehrbar sind genau die Knoten mit Motiv`() {
        val vorfuehrbar = SkillTreeRows.previewable().map { it.id }.toSet()
        val ohneMotiv = AnimationTree.pendingArtwork().map { it.id }.toSet()

        assertEquals(AnimationTree.nodes.size, vorfuehrbar.size + ohneMotiv.size)
        assertEquals(emptySet<String>(), vorfuehrbar intersect ohneMotiv)
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
