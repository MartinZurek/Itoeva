package com.notime.glyphsim.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests fuer [MatrixColors] gegen unabhaengig hingeschriebene Literale, nicht gegen die
 * Implementierung selbst.
 *
 * [MatrixColors] wird sowohl von der Compose-Ansicht als auch vom Widget-Bitmap-Renderer
 * verwendet, damit beide identisch aussehen - ein unbeabsichtigtes Verschieben eines dieser Werte
 * waere eine sichtbare Regression. Deshalb sind die Erwartungswerte hier als Literale verankert
 * und nicht von [MatrixColors.PUCK], [MatrixColors.PUCK_RIM], [MatrixColors.LED_OFF] oder
 * [MatrixColors.LED_ON] abgeleitet.
 */
class MatrixColorsTest {

    @Test
    fun `die vier Farbwerte stehen fest`() {
        assertEquals(0xFF0A0A0A.toInt(), MatrixColors.PUCK)
        assertEquals(0xFF2A2A2A.toInt(), MatrixColors.PUCK_RIM)
        assertEquals(0xFF232323.toInt(), MatrixColors.LED_OFF)
        assertEquals(0xFFF3F1EA.toInt(), MatrixColors.LED_ON)
    }

    @Test
    fun `alle vier Farben sind voll deckend`() {
        val alphaMaske = -0x1000000 // 0xFF000000.toInt()
        assertEquals(alphaMaske, MatrixColors.PUCK and alphaMaske)
        assertEquals(alphaMaske, MatrixColors.PUCK_RIM and alphaMaske)
        assertEquals(alphaMaske, MatrixColors.LED_OFF and alphaMaske)
        assertEquals(alphaMaske, MatrixColors.LED_ON and alphaMaske)
    }

    @Test
    fun `LED_ON ist heller als LED_OFF`() {
        assertTrue(kanalSumme(MatrixColors.LED_ON) > kanalSumme(MatrixColors.LED_OFF))
    }

    private fun kanalSumme(argb: Int): Int {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return r + g + b
    }
}
