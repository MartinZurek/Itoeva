package com.notime.glyphsim.living

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Abends ist man wach: Ausruhen zaehlt weniger, Hinausgehen mehr - bis zwei Uhr. */
class LivingRhythmTest {

    private fun at(h: Int, m: Int = 0) = h * 60 + m

    @Test
    fun tagsueberKeinRhythmus() {
        for (goal in GoalKind.entries) assertEquals(0.0, UtilitySelector.rhythm(goal, 0.5, at(14)), 0.0)
    }

    @Test
    fun abendsZaehltAusruhenWeniger() {
        assertTrue(UtilitySelector.rhythm(GoalKind.REST, 0.5, at(22, 30)) < 0)
        assertTrue(UtilitySelector.rhythm(GoalKind.REST, 0.5, at(1, 30)) < 0)
    }

    @Test
    fun abZweiUhrGiltDieMuedigkeitWiederVoll() {
        assertEquals(0.0, UtilitySelector.rhythm(GoalKind.REST, 0.5, at(2)), 0.0)
    }

    @Test
    fun wirklichMuedeDarfImmerRuhen() {
        assertEquals(0.0, UtilitySelector.rhythm(GoalKind.REST, 0.95, at(23)), 0.0)
    }

    @Test
    fun abendsZiehtEsHinaus() {
        for (goal in listOf(GoalKind.HAVE_FUN, GoalKind.EXPLORE, GoalKind.CONNECT_WITH)) {
            assertTrue(UtilitySelector.rhythm(goal, 0.1, at(21)) > 0)
        }
        // Essen und Arbeit bleiben, wie sie sind.
        assertEquals(0.0, UtilitySelector.rhythm(GoalKind.GET_FOOD, 0.5, at(21)), 0.0)
    }
}
