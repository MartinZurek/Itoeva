package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * [PlayRoutines.distributionFor] muss genau das beschreiben, was [PlayRoutines.forTopic] wuerfelt -
 * sonst waere der Rueckfall der Decision Policy nicht mehr die bisherige Wahl.
 */
class PlayRoutineDistributionTest {

    @Test
    fun `Verteilung und Wurf stimmen ueberein`() {
        val faelle = AnimationType.entries.flatMap { t ->
            listOf(
                Triple(t, emptyList<PlayRoutines.SpecialActivity>(), false),
                Triple(t, listOf(PlayRoutines.SpecialActivity.KITE, PlayRoutines.SpecialActivity.FOOTBALL), false),
                Triple(t, emptyList(), true)
            )
        }
        val random = Random(11)
        for ((topic, recent, ortswechsel) in faelle) {
            val verteilung = PlayRoutines.distributionFor(topic, true, recent, ortswechsel)
            assertEquals(1.0, verteilung.sumOf { it.second }, 1e-9)
            val n = 20_000
            val zaehler = HashMap<PlayRoutine, Int>()
            repeat(n) {
                val r = PlayRoutines.forTopic(topic, footballTrickLearned = true, recentSpecials = recent, preferPlaceChange = ortswechsel, random = random)
                zaehler[r] = (zaehler[r] ?: 0) + 1
            }
            assertTrue("$topic: gewuerfelt, aber nicht verteilt", zaehler.keys.all { k -> verteilung.any { it.first == k } })
            for ((routine, p) in verteilung) {
                assertEquals("$topic", p, (zaehler[routine] ?: 0).toDouble() / n, 0.015)
            }
        }
    }
}
