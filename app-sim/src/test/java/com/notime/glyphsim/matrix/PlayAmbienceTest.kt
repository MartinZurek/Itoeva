package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayAmbience.Kind
import com.notime.glyphsim.matrix.PlayAmbientActivity.DayPhase
import com.notime.glyphsim.matrix.PlayScene.Place
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

/** Prueft, welche Atmo wo erklingt, und dass jede Schleife sauber und nahtlos ist. */
class PlayAmbienceTest {

    @Test
    fun jederOrtKlingtNachSich() {
        val w = PlayWeather.CLEAR
        assertEquals(Kind.WAVES, PlayAmbience.kindFor(Place.BEACH, DayPhase.MIDDAY, w))
        assertEquals(Kind.WIND, PlayAmbience.kindFor(Place.MOUNTAINS, DayPhase.MIDDAY, w))
        assertEquals(Kind.FROGS, PlayAmbience.kindFor(Place.SWAMP, DayPhase.NIGHT, w))
        assertEquals(Kind.CITY, PlayAmbience.kindFor(Place.STREET, DayPhase.EVENING, w))
        assertEquals(Kind.CAFE, PlayAmbience.kindFor(Place.CAFE, DayPhase.MORNING, w))
        assertEquals(Kind.BIRDS, PlayAmbience.kindFor(Place.PARK, DayPhase.MORNING, w))
        assertEquals(Kind.CRICKETS, PlayAmbience.kindFor(Place.PARK, DayPhase.NIGHT, w))
    }

    @Test
    fun eineWohnungKlingtNachNichts() {
        for (place in listOf(Place.LIVING, Place.BEDROOM, Place.KITCHEN, Place.BATH, Place.DESK)) {
            assertNull(PlayAmbience.kindFor(place, DayPhase.MIDDAY, PlayWeather.CLEAR))
        }
    }

    @Test
    fun regenSchlaegtDenOrt() {
        assertEquals(Kind.RAIN, PlayAmbience.kindFor(Place.BEACH, DayPhase.MIDDAY, PlayWeather.RAIN))
        assertEquals(Kind.RAIN_WINDOW, PlayAmbience.kindFor(Place.LIVING, DayPhase.MIDDAY, PlayWeather.RAIN))
        assertEquals(Kind.RAIN_WINDOW, PlayAmbience.kindFor(Place.CAFE, DayPhase.MIDDAY, PlayWeather.RAIN))
    }

    @Test
    fun schneeIstLeise() {
        assertEquals(Kind.WIND, PlayAmbience.kindFor(Place.PARK, DayPhase.MIDDAY, PlayWeather.SNOW))
        assertNull(PlayAmbience.kindFor(Place.LIVING, DayPhase.MIDDAY, PlayWeather.SNOW))
    }

    private val loops: Map<Kind, ShortArray> by lazy { Kind.entries.associateWith { PlayAmbience.render(it) } }

    @Test
    fun jedeSchleifeHatDieRichtigeLaenge() {
        for ((kind, samples) in loops) {
            assertEquals(kind.loopSeconds * PlayAmbience.SAMPLE_RATE, samples.size)
        }
    }

    @Test
    fun keineSchleifeUebersteuert() {
        for ((kind, samples) in loops) {
            val peak = samples.maxOf { abs(it.toInt()) }
            assertTrue("$kind: Spitze $peak", peak <= (kind.level * Short.MAX_VALUE).toInt() + 1)
            assertTrue("$kind ist stumm", peak > 1000)
        }
    }

    @Test
    fun dieNahtKnacktNicht() {
        // Der Sprung vom letzten auf den ersten Abtastwert darf nicht groesser sein als die
        // uebliche Bewegung von einem Abtastwert zum naechsten.
        for ((kind, s) in loops) {
            val steps = (1 until s.size).map { abs(s[it] - s[it - 1]).toDouble() }
            val typical = sqrt(steps.sumOf { it * it } / steps.size)
            val seam = abs(s[0] - s[s.size - 1]).toDouble()
            assertTrue("$kind: Naht $seam, ueblich $typical", seam <= 4 * typical + 200)
        }
    }

    @Test
    fun derRegenAnDerScheibeIstLeiserAlsDraussen() {
        fun rms(s: ShortArray) = sqrt(s.sumOf { it.toDouble() * it } / s.size)
        assertTrue(rms(loops.getValue(Kind.RAIN_WINDOW)) < rms(loops.getValue(Kind.RAIN)))
    }

    @Test
    fun dieWelleSchwilltAnUndAb() {
        val s = loops.getValue(Kind.WAVES)
        val chunk = PlayAmbience.SAMPLE_RATE / 2
        val energy: List<Double> = (0 until s.size / chunk).map { c ->
            var sum = 0.0
            for (i in c * chunk until (c + 1) * chunk) sum += s[i].toDouble() * s[i]
            sum / chunk
        }
        assertTrue(energy.max() > 3 * energy.min())
    }

    @Test
    fun dieselbeSchleifeJedesMal() {
        assertArrayEquals(PlayAmbience.render(Kind.BIRDS), PlayAmbience.render(Kind.BIRDS))
    }
}
