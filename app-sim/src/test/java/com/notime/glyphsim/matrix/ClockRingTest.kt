package com.notime.glyphsim.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests fuer [ClockRing] gegen unabhaengig ermittelte Literale, nicht gegen die
 * Winkelformel selbst.
 *
 * Alle Erwartungswerte hier stammen aus einer Nachrechnung ausserhalb dieses Codes -
 * nicht aus [MatrixGeometry.RADIUS], [MatrixGeometry.CENTER] oder [ClockRing]s eigener
 * Winkelfunktion abgeleitet. Der eigentliche Kern sind die Reihenfolgen der ersten
 * sechs und der letzten drei Zellen: sie fallen auseinander, wenn jemand die
 * Sortierrichtung dreht oder den Nullpunkt des Winkels verschiebt.
 */
class ClockRingTest {

    @Test
    fun `genau 40 Zellen im Ring`() {
        assertEquals(40, ClockRing.perimeterCells.size)
    }

    @Test
    fun `erste Zelle ist 12 Uhr, senkrecht ueber der Mitte`() {
        assertEquals(6 to 0, ClockRing.perimeterCells.first())
    }

    // ---- Der eigentliche Kern: Sortierrichtung und Nullpunkt des Winkels ----

    @Test
    fun `die ersten sechs Zellen laufen im Uhrzeigersinn nach rechts`() {
        val erwartet = listOf(6 to 0, 7 to 0, 8 to 0, 9 to 1, 10 to 1, 10 to 2)
        assertEquals(erwartet, ClockRing.perimeterCells.take(6))
    }

    @Test
    fun `die letzten drei Zellen schliessen den Ring von links wieder nach oben`() {
        val erwartet = listOf(3 to 1, 4 to 0, 5 to 0)
        assertEquals(erwartet, ClockRing.perimeterCells.takeLast(3))
    }

    // ---- Duplikatfreiheit, Teilmenge und Groessenverhaeltnis ----

    @Test
    fun `keine Zelle kommt doppelt vor`() {
        assertEquals(ClockRing.perimeterCells.size, ClockRing.perimeterCells.toSet().size)
    }

    @Test
    fun `jede Ringzelle ist auch eine aktive Zelle`() {
        val aktive = MatrixGeometry.activeCells.toSet()
        for (zelle in ClockRing.perimeterCells) {
            assertTrue("$zelle sollte in activeCells liegen", zelle in aktive)
        }
    }

    @Test
    fun `der Ring ist echt kleiner als alle aktiven Zellen`() {
        assertEquals(40, ClockRing.perimeterCells.size)
        assertEquals(137, MatrixGeometry.activeCells.size)
        assertTrue(ClockRing.perimeterCells.size < MatrixGeometry.activeCells.size)
    }
}
