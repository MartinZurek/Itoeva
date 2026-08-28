package com.notime.glyphsim.skilltree

import com.notime.glyphcore.data.AnimationTree

/**
 * Verbindet Level und Skillbaum: Level 1 besitzt nur die Hauptgruppen, jeder weitere Aufstieg
 * verdient genau einen neuen Knoten.
 *
 * Aus dem Bestand berechnet statt als eigener Zaehler gespeichert. Dadurch kann eine Auswahl
 * nicht verloren gehen, wenn die App zwischen Aufstieg und Wahl beendet wird. Auch bestehende
 * Spielstaende holen nach dem Update fehlende Wahlen nach.
 */
object LevelUnlocks {
    fun due(level: Int, unlocked: Set<String>): Int {
        val roots = UnlockOffers.startingNodes().toSet()
        val unlockableCount = AnimationTree.nodes.count { it.id !in roots && it.motif != null }
        val earnedByLevel = (level - 1).coerceIn(0, unlockableCount)
        val alreadyChosen = unlocked.count { it !in roots }
        return (earnedByLevel - alreadyChosen).coerceAtLeast(0)
    }
}
