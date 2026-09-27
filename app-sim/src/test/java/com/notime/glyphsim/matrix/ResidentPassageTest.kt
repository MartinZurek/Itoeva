package com.notime.glyphsim.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prueft den Vorbeigang eines Nachbarn: hinein, gruessen, weiter, dann eine Weile niemand. */
class ResidentPassageTest {

    private val id = "resident:park:puffling"
    private val w = 0.12f
    private val hostLeft = 0.40f
    private val hostWidth = 0.25f

    private fun at(ms: Long) = ResidentPassage.momentAt(id, ms, w, hostLeft, hostWidth)

    /** Alle Momente eines Zyklus im Abstand von 100 ms. */
    private fun cycle(): List<ResidentPassage.Moment?> =
        (0 until ResidentPassage.cycleMs(id) step 100).map { at(it) }

    @Test
    fun ineinemZyklusIstErEineWeileDaUndEineWeileNicht() {
        val moments = cycle()
        assertTrue(moments.any { it != null })
        assertTrue(moments.any { it == null })
        // Mehr als die Haelfte der Zeit ist niemand da - er kommt vorbei, er wohnt nicht hier.
        assertTrue(moments.count { it == null } > moments.size / 2)
    }

    @Test
    fun erGruesstNebenDemWesenUndNichtDarin() {
        val greetings = cycle().filterNotNull().filter { !it.walking }
        assertTrue("kein Gruss", greetings.isNotEmpty())
        for (g in greetings) {
            val right = g.leftFraction + w
            val bodyLeft = hostLeft + hostWidth * ResidentPassage.BODY_MARGIN
            val bodyRight = hostLeft + hostWidth * (1f - ResidentPassage.BODY_MARGIN)
            assertTrue("steht im Wesen: $g", right <= bodyLeft || g.leftFraction >= bodyRight)
            // ... aber auch nicht weit weg: hoechstens eine Figurbreite Luft.
            val gap = if (right <= bodyLeft) bodyLeft - right else g.leftFraction - bodyRight
            assertTrue("gruesst ins Leere: $gap", gap < w)
        }
    }

    @Test
    fun erGehtGleichmaessigInEineRichtung() {
        val walking = cycle().filterNotNull().filter { it.walking }
        val direction = walking.first().facingLeft
        assertTrue(walking.all { it.facingLeft == direction })
        val xs = walking.map { it.leftFraction }
        val steps = xs.zipWithNext { a, b -> b - a }.filter { kotlin.math.abs(it) > 1e-6 }
        assertTrue(steps.all { (it < 0) == direction })
    }

    @Test
    fun dieRichtungWechseltVonRundeZuRunde() {
        val c = ResidentPassage.cycleMs(id)
        val directions = (0 until 6).mapNotNull { round ->
            (0 until c step 200).firstNotNullOfOrNull { at(round * c + it) }?.facingLeft
        }
        assertTrue(directions.toSet().size == 2)
    }

    @Test
    fun amRandStehtKeinGruss() {
        val moments = (0 until ResidentPassage.cycleMs(id) step 100)
            .mapNotNull { ResidentPassage.momentAt(id, it, w, 0f, 0.25f) }
        // Kommt er von links, laege der Treffpunkt ausserhalb - dann geht er nur vorbei.
        val fromLeft = moments.filter { !it.facingLeft }
        assertTrue(fromLeft.all { it.walking })
    }

    @Test
    fun zweiNachbarnLaufenNichtImGleichschritt() {
        val other = "resident:park:starlet"
        val c = maxOf(ResidentPassage.cycleMs(id), ResidentPassage.cycleMs(other))
        val same = (0 until c step 500).count {
            (at(it) != null) == (ResidentPassage.momentAt(other, it, w, hostLeft, hostWidth) != null)
        }
        assertFalse(same == (c / 500).toInt())
    }

    @Test
    fun dieselbeZeitGibtDenselbenMoment() {
        assertEquals(at(123_456L), at(123_456L))
    }
}
