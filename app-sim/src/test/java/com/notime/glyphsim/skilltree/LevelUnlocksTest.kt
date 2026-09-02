package com.notime.glyphsim.skilltree

import com.notime.glyphcore.data.AnimationTree
import org.junit.Assert.assertEquals
import org.junit.Test

class LevelUnlocksTest {
    private val roots = UnlockOffers.startingNodes().toSet()

    @Test
    fun `level eins startet nur mit den hauptgruppen`() {
        assertEquals(0, LevelUnlocks.due(level = 1, unlocked = roots))
    }

    @Test
    fun `jeder aufstieg verdient genau eine freischaltung`() {
        assertEquals(1, LevelUnlocks.due(level = 2, unlocked = roots))
        assertEquals(4, LevelUnlocks.due(level = 5, unlocked = roots))
    }

    @Test
    fun `bereits gewaehlte knoten werden angerechnet`() {
        val firstChild = AnimationTree.nodes.first { it.parentId in roots }.id
        assertEquals(0, LevelUnlocks.due(level = 2, unlocked = roots + firstChild))
        assertEquals(2, LevelUnlocks.due(level = 4, unlocked = roots + firstChild))
    }

    @Test
    fun `mehr bestand als das level erzeugt keine negative schuld`() {
        val children = AnimationTree.nodes.filter { it.parentId in roots }.take(3).map { it.id }
        assertEquals(0, LevelUnlocks.due(level = 2, unlocked = roots + children))
    }
}
