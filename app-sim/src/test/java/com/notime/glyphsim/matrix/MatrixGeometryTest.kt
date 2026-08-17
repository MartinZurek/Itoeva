package com.notime.glyphsim.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests fuer [MatrixGeometry] gegen unabhaengig ermittelte Literale, nicht gegen die
 * Implementierung selbst.
 *
 * Alle Erwartungswerte hier stammen aus einer Nachrechnung ausserhalb dieses Codes, mit zwei
 * voneinander unabhaengigen Verfahren bestimmt und gegeneinander geprueft - nicht aus
 * [MatrixGeometry.RADIUS], [MatrixGeometry.CENTER] oder [MatrixGeometry.SIZE] abgeleitet. Wuerde
 * RADIUS heimlich von 6.5 auf 6.0 oder 4.0 verkleinert, CENTER verschoben oder SIZE geaendert,
 * muessen diese Tests rot werden.
 */
class MatrixGeometryTest {

    @Test
    fun `Grundmasse stehen fest`() {
        assertEquals(13, MatrixGeometry.SIZE)
        assertEquals(6.0, MatrixGeometry.CENTER, 0.0)
        assertEquals(6.5, MatrixGeometry.RADIUS, 0.0)
        assertEquals(4095, MatrixGeometry.MAX_BRIGHTNESS)
    }

    @Test
    fun `die Mitte ist aktiv`() {
        assertTrue(MatrixGeometry.isActive(6, 6))
    }

    @Test
    fun `alle vier Ecken sind inaktiv`() {
        assertFalse(MatrixGeometry.isActive(0, 0))
        assertFalse(MatrixGeometry.isActive(0, 12))
        assertFalse(MatrixGeometry.isActive(12, 0))
        assertFalse(MatrixGeometry.isActive(12, 12))
    }

    // ---- Der eigentliche Kern: Radius 6.5, nicht 6.0 ----

    /**
     * Diese vier Zellen liegen zwischen Abstand 6.0 und 6.5 von der Rastermitte entfernt (rund
     * 6.325 bzw. 6.403 - unabhaengig von der Produktionsformel nachgerechnet). Bei Radius 6.5
     * sind sie aktiv; bei Radius 6.0 waeren sie es nicht mehr. Genau daran erkennt dieser Test,
     * ob RADIUS heimlich verkleinert wurde - der eigentliche Zweck dieser Datei.
     */
    @Test
    fun `Randzellen mit Abstand zwischen 6,0 und 6,5 sind aktiv`() {
        assertTrue("(0,4), Abstand rund 6.325 - waere bei Radius 6.0 inaktiv", MatrixGeometry.isActive(0, 4))
        assertTrue("(2,1), Abstand rund 6.403 - waere bei Radius 6.0 inaktiv", MatrixGeometry.isActive(2, 1))
        assertTrue("(4,0), Abstand rund 6.325 - waere bei Radius 6.0 inaktiv", MatrixGeometry.isActive(4, 0))
        assertTrue("(12,8), Abstand rund 6.325 - waere bei Radius 6.0 inaktiv", MatrixGeometry.isActive(12, 8))
    }

    // ---- Symmetrie an festen Gegenbeispielpaaren ----

    @Test
    fun `Symmetrie an festen Koordinatenpaaren`() {
        // Horizontal gespiegelt (x <-> 12-x)
        assertEquals(MatrixGeometry.isActive(0, 4), MatrixGeometry.isActive(12, 4))
        assertEquals(MatrixGeometry.isActive(2, 1), MatrixGeometry.isActive(10, 1))
        // Vertikal gespiegelt (y <-> 12-y)
        assertEquals(MatrixGeometry.isActive(4, 0), MatrixGeometry.isActive(4, 12))
        assertEquals(MatrixGeometry.isActive(2, 1), MatrixGeometry.isActive(2, 11))
        // Diagonal gespiegelt (x <-> y)
        assertEquals(MatrixGeometry.isActive(0, 4), MatrixGeometry.isActive(4, 0))
        assertEquals(MatrixGeometry.isActive(12, 8), MatrixGeometry.isActive(8, 12))

        // Symmetrie allein waere auch erfuellt, wenn alle vier inaktiv waeren - deshalb zusaetzlich
        // konkret pruefen, dass es sich um aktive Zellen handelt.
        assertTrue(MatrixGeometry.isActive(0, 4))
        assertTrue(MatrixGeometry.isActive(12, 4))
        assertTrue(MatrixGeometry.isActive(4, 0))
        assertTrue(MatrixGeometry.isActive(4, 12))
    }

    // ---- activeCells: Groesse, Duplikatfreiheit und Zeilenverteilung ----

    @Test
    fun `genau 137 aktive Zellen`() {
        assertEquals(137, MatrixGeometry.activeCells.size)
    }

    @Test
    fun `keine Zelle kommt doppelt vor`() {
        assertEquals(MatrixGeometry.activeCells.size, MatrixGeometry.activeCells.toSet().size)
    }

    @Test
    fun `die Zeilenverteilung stimmt genau`() {
        val erwartet = mapOf(
            0 to 5, 1 to 9, 2 to 11, 3 to 11, 4 to 13, 5 to 13, 6 to 13,
            7 to 13, 8 to 13, 9 to 11, 10 to 11, 11 to 9, 12 to 5
        )
        val tatsaechlich = MatrixGeometry.activeCells.groupingBy { it.second }.eachCount()
        for ((zeile, anzahl) in erwartet) {
            assertEquals("Zeile $zeile", anzahl, tatsaechlich[zeile] ?: 0)
        }
        assertEquals("es gibt keine Zeile ausserhalb 0..12 mit aktiven Zellen", erwartet.keys, tatsaechlich.keys)
    }
}
