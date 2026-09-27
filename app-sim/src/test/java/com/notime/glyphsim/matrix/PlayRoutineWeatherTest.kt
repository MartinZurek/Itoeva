package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Prueft, dass Regen und Schnee den Tagesablauf veraendern - und klares Wetter nichts. */
class PlayRoutineWeatherTest {

    private fun goesTo(routine: PlayRoutine, places: Set<PlayScene.Place>) =
        routine.steps.any { it is RoutineStep.GoToPlace && it.place in places }

    @Test
    fun klaresWetterAendertNichts() {
        for (topic in AnimationType.entries) {
            assertEquals(
                PlayRoutines.distributionFor(topic),
                PlayRoutines.distributionFor(topic, weather = PlayWeather.CLEAR)
            )
        }
    }

    @Test
    fun imRegenKeinAusflugUndKeinDrachen() {
        for (weather in listOf(PlayWeather.RAIN, PlayWeather.SNOW)) {
            val moeglich = PlayRoutines.distributionFor(AnimationType.MOVE, weather = weather).map { it.first }
            assertTrue(moeglich.isNotEmpty())
            assertTrue(moeglich.none { goesTo(it, PlayRoutines.WEATHER_CLOSED) })
            assertTrue(moeglich.none { r -> r.steps.any { it is RoutineStep.Kite } })
        }
    }

    @Test
    fun beiKlaremWetterGibtEsAusfluege() {
        val moeglich = PlayRoutines.distributionFor(AnimationType.MOVE).map { it.first }
        assertTrue(moeglich.any { goesTo(it, PlayRoutines.WEATHER_CLOSED) })
    }

    @Test
    fun regenFuehrtAnsFenster() {
        val trocken = PlayRoutines.distributionFor(AnimationType.FOCUS).map { it.first }
        val nass = PlayRoutines.distributionFor(AnimationType.FOCUS, weather = PlayWeather.RAIN).map { it.first }
        val fenster = PlayRoutines.rainyDay(AnimationType.FOCUS).single()
        assertFalse(fenster in trocken)
        assertTrue(fenster in nass)
    }

    @Test
    fun wahlUndVerteilungBleibenImRegenDeckungsgleich() {
        for (topic in AnimationType.entries) {
            val verteilung = PlayRoutines.distributionFor(topic, weather = PlayWeather.RAIN)
                .map { it.first }.toSet()
            val random = Random(7)
            repeat(200) {
                val gezogen = PlayRoutines.forTopic(topic, weather = PlayWeather.RAIN, random = random)
                assertTrue("$topic: $gezogen fehlt in der Verteilung", gezogen in verteilung)
            }
        }
    }

    @Test
    fun imRegenDraussenEilig() {
        val normal = AvatarBearing.paceFactor(null, PlayAmbientActivity.DayPhase.MIDDAY)
        val eilig = AvatarBearing.paceFactor(null, PlayAmbientActivity.DayPhase.MIDDAY, hurry = true)
        assertTrue(eilig > normal + 0.1f)
        assertTrue(eilig <= AvatarBearing.HURRY_MAX_PACE)
    }
}
