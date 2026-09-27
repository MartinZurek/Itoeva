package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayAmbientActivity.DayPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/** Prueft die gleitende Daemmerung, die Fenstergewohnheiten und den Sonnenlauf. */
class PlayDaylightTest {

    @Test
    fun phasenmittenTreffenDieBisherigenStufen() {
        assertEquals(PlayDaylight.atmosphere(DayPhase.MORNING), PlayDaylight.atmosphere(8 * 60 + 30), 0.001f)
        assertEquals(PlayDaylight.atmosphere(DayPhase.MIDDAY), PlayDaylight.atmosphere(14 * 60), 0.001f)
        assertEquals(PlayDaylight.atmosphere(DayPhase.EVENING), PlayDaylight.atmosphere(20 * 60), 0.001f)
        assertEquals(PlayDaylight.atmosphere(DayPhase.NIGHT), PlayDaylight.atmosphere(2 * 60), 0.001f)
    }

    @Test
    fun keineMinuteSpringtSichtbar() {
        // Die alte Stufe sprang um bis zu 0,26 auf einmal. Gleitend aendert sich von Minute zu
        // Minute hoechstens ein Hauch.
        for (m in 0 until 24 * 60) {
            val step = abs(PlayDaylight.atmosphere(m + 1) - PlayDaylight.atmosphere(m))
            assertTrue("Sprung $step um Minute $m", step < 0.01f)
        }
    }

    @Test
    fun abendsWirdEsWirklichDunkler() {
        assertTrue(PlayDaylight.atmosphere(18 * 60) < PlayDaylight.atmosphere(17 * 60))
        assertTrue(PlayDaylight.atmosphere(6 * 60) > PlayDaylight.atmosphere(5 * 60))
    }

    @Test
    fun helligkeitBleibtUeberDerSichtbarkeitsschwelle() {
        for (m in 0 until 24 * 60) {
            assertTrue(PlayDaylight.atmosphere(m) >= 0.58f - 0.0001f)
        }
    }

    private fun litShare(minute: Int) = (0 until 200).count { PlayDaylight.windowLit(minute, it) } / 200f

    @Test
    fun lichterGehenNachUndNachAn() {
        val shares = listOf(17 * 60 + 15, 18 * 60, 19 * 60, 20 * 60, 21 * 60).map(::litShare)
        assertEquals(0f, shares.first(), 0.001f)
        for (i in 1 until shares.size) assertTrue("nicht steigend: $shares", shares[i] >= shares[i - 1])
        assertTrue("zu wenige Fenster um 21 Uhr: ${shares.last()}", shares.last() > 0.5f)
        // ... und nicht alle: Wo niemand zu Hause ist, bleibt es dunkel.
        assertTrue(shares.last() < 0.9f)
    }

    @Test
    fun nachtsGehenSieNachUndNachAus() {
        assertTrue(litShare(1 * 60) < litShare(22 * 60))
        assertTrue(litShare(4 * 60) < litShare(1 * 60))
        assertTrue("Nachteulen fehlen", litShare(2 * 60 + 30) > 0f)
    }

    @Test
    fun fruehaufsteherMachenMorgensLicht() {
        assertTrue(litShare(6 * 60 + 45) > 0f)
        assertEquals(0f, litShare(9 * 60), 0.001f)
        assertEquals(0f, litShare(14 * 60), 0.001f)
    }

    @Test
    fun einFensterIstJedenTagGleich() {
        for (key in 0 until 50) {
            assertEquals(PlayDaylight.windowLit(20 * 60, key), PlayDaylight.windowLit(20 * 60 + 24 * 60, key))
        }
    }

    @Test
    fun sonneNurAmRandDesTages() {
        assertNull(PlayDaylight.sun(12 * 60))
        assertNull(PlayDaylight.sun(0))
        assertNotNull(PlayDaylight.sun(6 * 60 + 30))
        assertNotNull(PlayDaylight.sun(18 * 60))
    }

    @Test
    fun abendsSinktSieRechtsMorgensSteigtSieLinks() {
        val early = PlayDaylight.sun(17 * 60 + 20)!!
        val late = PlayDaylight.sun(18 * 60 + 40)!!
        assertTrue(early.xFraction > 0.5f)
        assertTrue(late.cellsAboveFloor < early.cellsAboveFloor)
        val dawn = PlayDaylight.sun(5 * 60 + 50)!!
        val morning = PlayDaylight.sun(7 * 60 + 10)!!
        assertTrue(dawn.xFraction < 0.5f)
        assertTrue(morning.cellsAboveFloor > dawn.cellsAboveFloor)
    }
}
