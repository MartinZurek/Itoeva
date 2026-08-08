package com.notime.glyphsim.matrix

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Mondsichel ist als Differenz zweier Kreise gebaut (siehe [MoonFrame]) - eine Konstruktion,
 * die bei falschen Radien lautlos entgleist: Liegen die Kreise zu weit auseinander, bleibt die
 * volle Scheibe stehen und der Mond ist von der gewoehnlichen Uhr nicht zu unterscheiden; liegen
 * sie zu dicht, verschwindet die Sichel ganz. Beides waere kein Absturz, sondern nur eine Szene,
 * die ihre Wirkung verfehlt.
 */
class MoonFrameTest {

    private val cells = MatrixGeometry.activeCells.size

    @Test
    fun `die Sichel ist sichtbar, aber keine volle Scheibe`() {
        for (phase in 0..40) {
            val lit = MoonFrame.build(phase).count { it > 0 }
            assertTrue("Phase $phase zeigt gar nichts", lit > 0)
            assertTrue(
                "Phase $phase fuellt die Scheibe fast ganz ($lit von $cells) - das waere " +
                    "von der normalen Uhr nicht zu unterscheiden",
                lit < cells * 0.62
            )
            assertTrue(
                "Phase $phase ist nur noch ein Splitter ($lit von $cells)",
                lit > cells * 0.15
            )
        }
    }

    @Test
    fun `die Sichel liegt auf einer Seite`() {
        // Eine Sichel ist per Definition unsymmetrisch - waere sie es nicht, saehe sie aus wie
        // ein Ring oder eine Scheibe.
        val g = MatrixGeometry.SIZE
        val frame = MoonFrame.build(0)
        var left = 0
        var right = 0
        for (y in 0 until g) {
            for (x in 0 until g) {
                if (frame[y * g + x] <= 0) continue
                if (x < MatrixGeometry.CENTER) left++ else right++
            }
        }
        assertTrue("Die Sichel ist symmetrisch (links $left, rechts $right)", left > right * 2)
    }

    @Test
    fun `die Sichel bleibt im Raster der Matrix`() {
        // Nur Zellen, die es auf der runden Matrix ueberhaupt gibt.
        val g = MatrixGeometry.SIZE
        val frame = MoonFrame.build(0)
        for (y in 0 until g) {
            for (x in 0 until g) {
                if (frame[y * g + x] > 0) {
                    assertTrue("Zelle ($x,$y) liegt ausserhalb der runden Matrix", MatrixGeometry.isActive(x, y))
                }
            }
        }
    }
}
