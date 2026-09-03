package com.notime.glyphsim.skilltree

import com.notime.glyphcore.data.AnimationNode
import com.notime.glyphcore.data.AnimationTree

/**
 * In welchem Zustand ein Knoten dem Nutzer gezeigt wird.
 *
 * Vier statt zwei ("offen"/"zu"), weil sich die drei gesperrten Faelle fuer den Betrachter
 * voellig unterschiedlich anfuehlen: Etwas, das als Naechstes drankommen kann, ist eine
 * Aussicht; etwas hinter einer verschlossenen Tuer ist ein Hinweis, wo es weitergeht; und etwas,
 * das es noch gar nicht gibt, ist keins von beidem. Eine Oberflaeche, die alle drei gleich grau
 * zeigt, macht aus einem Baum eine Liste.
 */
enum class NodeState {
    /** Freigeschaltet - liegt in der Zieh-Leiste. */
    UNLOCKED,

    /** Kann beim naechsten Aufstieg zur Wahl stehen (die Grenze, siehe [UnlockOffers.frontier]). */
    AVAILABLE,

    /** Noch zu, weil der Elternknoten zu ist. */
    LOCKED,

    /**
     * Noch nicht gezeichnet (siehe [AnimationTree.pendingArtwork]).
     *
     * Wird ausdruecklich als eigener Zustand gezeigt und nicht versteckt: Ein Loch im Baum, das
     * man sieht, ist ehrlicher als eines, das man erst beim Suchen bemerkt.
     */
    PENDING_ART
}

data class SkillTreeRow(val node: AnimationNode, val state: NodeState)

/**
 * Uebersetzt den Freischalt-Stand in das, was der Baumbildschirm zeigt.
 *
 * Reine Rechnung ohne Compose - dadurch laesst sich die Zuordnung Zustand → Knoten pruefen, ohne
 * einen Bildschirm zu starten. Was daran schiefgehen kann, ist eine Verwechslung von "gesperrt"
 * und "als Naechstes dran", und die faellt beim blossen Ansehen kaum auf.
 */
object SkillTreeRows {

    /** Alle 79 Knoten in Baumreihenfolge, jeder mit seinem Zustand. */
    fun build(unlocked: Set<String>): List<SkillTreeRow> {
        val frontier = UnlockOffers.frontier(unlocked).map { it.id }.toSet()
        return AnimationTree.nodes.map { node ->
            SkillTreeRow(node, stateOf(node, unlocked, frontier))
        }
    }

    /** Nur die Knoten einer Hauptgruppe - der Bildschirm zeigt je Gruppe einen Abschnitt. */
    fun forRoot(rootId: String, unlocked: Set<String>): List<SkillTreeRow> =
        build(unlocked).filter { AnimationTree.fallbackChain(it.node.id).last() == rootId }

    private fun stateOf(
        node: AnimationNode,
        unlocked: Set<String>,
        frontier: Set<String>
    ): NodeState = when {
        node.id in unlocked -> NodeState.UNLOCKED
        // Vor LOCKED geprueft: Ein ungezeichneter Knoten ist nie "als Naechstes dran", auch wenn
        // sein Elternknoten offen ist - er kann gar nicht angeboten werden.
        node.motif == null -> NodeState.PENDING_ART
        node.id in frontier -> NodeState.AVAILABLE
        else -> NodeState.LOCKED
    }

    /**
     * Die Vorfahren eines Knotens - ohne ihn selbst, von seinem Elternknoten aufwaerts.
     *
     * Gebraucht, um nach einer Freischaltung genau den Ast aufzuklappen, in dem der neue Knoten
     * liegt: Ohne das bliebe die Rueckmeldung unsichtbar, sobald der Zweig zugeklappt war - der
     * Stern waere weg und auf dem Bildschirm haette sich nichts geruehrt.
     *
     * Hier und nicht im Bildschirm, weil es eine Aussage ueber den Baum ist und keine ueber seine
     * Darstellung - und weil sie sich so ohne Compose pruefen laesst.
     */
    fun ancestorsOf(nodeId: String): Set<String> =
        AnimationTree.fallbackChain(nodeId).drop(1).toSet()

    /**
     * Alle Knoten, die sich vorfuehren lassen: die mit einem Motiv, in Baumreihenfolge.
     *
     * Fuer die Testumgebung unter dem Baum. Ungezeichnete Knoten ([NodeState.PENDING_ART]) fehlen
     * hier als einzige - sie haetten nichts zu zeigen.
     */
    fun previewable(): List<AnimationNode> = AnimationTree.nodes.filter { it.motif != null }

    /**
     * Wieviele Knoten einer Hauptgruppe schon offen sind, und wieviele es ueberhaupt gibt.
     *
     * Fuer die Fortschrittsanzeige je Abschnitt. Ungezeichnete zaehlen im Nenner mit: Sie gehoeren
     * zum Baum, und sie herauszurechnen wuerde eine Gruppe als "fertig" ausweisen, in der noch
     * etwas fehlt.
     */
    fun progressFor(rootId: String, unlocked: Set<String>): Pair<Int, Int> {
        val rows = forRoot(rootId, unlocked)
        return rows.count { it.state == NodeState.UNLOCKED } to rows.size
    }
}
