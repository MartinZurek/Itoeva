package com.notime.glyphsim.ui

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.matrix.MatrixGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionSlotSymbolsTest {

    private fun action(
        type: AnimationType? = null,
        label: String? = null,
        frames: List<IntArray> = listOf(IntArray(MatrixGeometry.SIZE * MatrixGeometry.SIZE))
    ) = SavedAction(1L, 2L, type, label, frames)

    @Test
    fun `jeder feste Animationstyp bekommt ein sichtbares eigenes Symbol`() {
        val symbols = AnimationType.entries.map { type ->
            ActionSlotSymbols.frameFor(action(type = type))
        }

        symbols.forEach { frame ->
            assertEquals(MatrixGeometry.SIZE * MatrixGeometry.SIZE, frame.size)
            assertTrue(frame.any { it > 0 })
        }
        assertEquals(AnimationType.entries.size, symbols.map { it.toList() }.distinct().size)
    }

    @Test
    fun `Trinken zeigt das fertige Glas statt des abgelegten Zwischenframes`() {
        val arbitrarySnapshot = IntArray(MatrixGeometry.SIZE * MatrixGeometry.SIZE).also { it[0] = 1 }
        val symbol = ActionSlotSymbols.frameFor(
            action(type = AnimationType.DRINK, frames = listOf(arbitrarySnapshot))
        )

        assertFalse(symbol.contentEquals(arbitrarySnapshot))
        // Glasboden und beide Seiten des kanonischen Trink-Motivs.
        assertTrue(symbol[9 * MatrixGeometry.SIZE + 6] > 0)
        assertTrue(symbol[6 * MatrixGeometry.SIZE + 4] > 0)
        assertTrue(symbol[6 * MatrixGeometry.SIZE + 8] > 0)
    }

    @Test
    fun `Rocket zeigt eine Rakete und nicht den ersten Bibliotheksframe`() {
        val arbitrarySnapshot = IntArray(MatrixGeometry.SIZE * MatrixGeometry.SIZE).also { it[0] = 1 }
        val symbol = ActionSlotSymbols.frameFor(
            action(label = "Rocket", frames = listOf(arbitrarySnapshot))
        )

        assertFalse(symbol.contentEquals(arbitrarySnapshot))
        assertTrue(symbol[1 * MatrixGeometry.SIZE + 6] > 0)  // Spitze
        assertTrue(symbol[7 * MatrixGeometry.SIZE + 3] > 0)  // linke Finne
        assertTrue(symbol[7 * MatrixGeometry.SIZE + 9] > 0)  // rechte Finne
        assertTrue(symbol[11 * MatrixGeometry.SIZE + 6] > 0) // Flamme
    }

    @Test
    fun `freie Bibliotheksanimation nimmt ihren vollstaendigsten Frame`() {
        val sparse = IntArray(MatrixGeometry.SIZE * MatrixGeometry.SIZE).also { it[6] = 100 }
        val complete = IntArray(MatrixGeometry.SIZE * MatrixGeometry.SIZE).also {
            it[5] = 100
            it[6] = 100
            it[7] = 100
        }

        val symbol = ActionSlotSymbols.frameFor(
            action(label = "Eigenes Motiv", frames = listOf(sparse, complete))
        )

        assertTrue(symbol.contentEquals(complete))
    }
}
