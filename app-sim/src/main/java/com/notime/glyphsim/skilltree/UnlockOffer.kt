package com.notime.glyphsim.skilltree

import com.notime.glyphcore.data.AnimationNode
import com.notime.glyphcore.data.AnimationTree

/**
 * Die **Grenze** des Freischalt-Stands - welche Knoten als Naechstes erreichbar sind.
 *
 * Frueher die Grundlage eines algorithmischen 2+1-Angebots beim Levelaufstieg; das Angebot ist
 * einer manuellen Freischaltung gewichen (das Wander-Brett, SKILLBAUM.md P11) - [frontier] bleibt,
 * weil sie genau die Menge ist, aus der ein Spieler dort selbst waehlt.
 */
object UnlockOffers {

    /**
     * Die **Grenze**: Kinder freigeschalteter Knoten, die selbst noch zu sind und ein Motiv haben.
     *
     * Oeffentlich, weil sich sonst nicht pruefen laesst, was ueberhaupt erreichbar ist - eine
     * Grenze, die irgendwann leer laeuft, waere ein Baum, der sich nicht mehr oeffnen laesst, ohne
     * dass irgendwo ein Fehler auftaucht.
     */
    fun frontier(unlocked: Set<String>): List<AnimationNode> =
        AnimationTree.nodes.filter { node ->
            node.motif != null &&
                node.id !in unlocked &&
                node.parentId != null &&
                node.parentId in unlocked
        }

    /** Die Hauptgruppen, mit denen jedes Profil beginnt. */
    fun startingNodes(): List<String> = AnimationTree.roots().map { it.id }
}
