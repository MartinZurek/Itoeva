package com.notime.glyphsim.matrix

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayEffectsTest {

    @Test
    fun `Fussball bleibt sichtbar und der Trick hebt ihn deutlich an`() {
        val dribble = PlayEffects.footballCells(8, 20, PlayEffects.FootballPhase.DRIBBLE, 0, 48)
        val trick = PlayEffects.footballCells(8, 20, PlayEffects.FootballPhase.TRICK, 0, 48)

        assertTrue(dribble.size >= 9)
        assertTrue(trick.minOf { it.y } < dribble.minOf { it.y } - 5)
        assertTrue((dribble + trick).all { it.x in 0 until 48 })
    }

    @Test
    fun `Basketball prellt und landet sichtbar im Korb`() {
        val low = PlayEffects.basketballCells(
            8, 20, PlayEffects.BasketballPhase.DRIBBLE, 0, 48
        )
        val high = PlayEffects.basketballCells(
            8, 20, PlayEffects.BasketballPhase.SHOOT, 0, 48
        )
        val score = PlayEffects.basketballCells(
            8, 20, PlayEffects.BasketballPhase.SCORE, 0, 48
        )
        assertTrue(high.minOf { it.y } < low.minOf { it.y })
        assertTrue(score.size >= 20)
        assertTrue((low + high + score).all { it.x in 0 until 48 })
    }

    @Test
    fun `Hantel wandert beim Heben deutlich ueber den Kopf`() {
        val warm = PlayEffects.trainingCells(12, 20, PlayEffects.TrainingPhase.WARM_UP, 0)
        val lift = PlayEffects.trainingCells(12, 20, PlayEffects.TrainingPhase.LIFT, 0)
        assertTrue(lift.minOf { it.y } < warm.minOf { it.y } - 10)
        assertTrue(lift.size >= 20)
    }

    @Test
    fun `fliegender Drache hat Koerper Schweif und gespannte Schnur`() {
        val cells = PlayEffects.kiteCells(
            avatarCellX = 12,
            avatarCellY = 40,
            phase = PlayEffects.KitePhase.FLY,
            scenePhase = 0,
            widthCells = 60
        )

        assertTrue("Drache ist zu klein oder ohne Schnur", cells.size >= 20)
        assertTrue("Drache steigt nicht sichtbar ueber den Avatar", cells.minOf { it.y } < 30)
        assertTrue("Schnur erreicht die Hand nicht", cells.any { it.y >= 50 })
    }

    @Test
    fun `Wind bewegt den Drachen ohne ihn aus dem Bild zu schieben`() {
        val first = PlayEffects.kiteCells(40, 35, PlayEffects.KitePhase.FLY, 0, 48)
        val later = PlayEffects.kiteCells(40, 35, PlayEffects.KitePhase.FLY, 5, 48)

        assertNotEquals(first.map { it.x to it.y }.toSet(), later.map { it.x to it.y }.toSet())
        for (cell in later) {
            assertTrue("Drachenzelle ausserhalb der Breite: $cell", cell.x in 0 until 48)
        }
    }
}
