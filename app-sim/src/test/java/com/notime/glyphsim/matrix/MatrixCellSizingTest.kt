package com.notime.glyphsim.matrix

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests fuer [MatrixCellSizing] gegen unabhaengig ermittelte Literale, nicht gegen die
 * Produktionsformel selbst - ein Test, der 13 + 2 * 0.36 nachrechnet, waere bei einer falschen
 * Formel genauso gruen wie bei einer richtigen.
 */
class MatrixCellSizingTest {

    @Test
    fun `DOT_RADIUS_FACTOR steht fest`() {
        assertEquals(0.36f, MatrixCellSizing.DOT_RADIUS_FACTOR, 0f)
    }

    @Test
    fun `EFFECTIVE_GRID_UNITS steht fest`() {
        assertEquals(13.72f, MatrixCellSizing.EFFECTIVE_GRID_UNITS, 0.0001f)
    }
}
