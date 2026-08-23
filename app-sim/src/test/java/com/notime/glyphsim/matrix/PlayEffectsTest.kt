package com.notime.glyphsim.matrix

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayEffectsTest {

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
