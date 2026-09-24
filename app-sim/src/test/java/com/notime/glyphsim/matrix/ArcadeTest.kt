package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Die Spielhalle (Weltausbau Stufe 3) - dass sie als Ort vollstaendig ist: begehbar, mit einem
 * Automaten, an dem gespielt werden kann, mit Besuch und mit eigener Musik.
 */
class ArcadeTest {

    @Test
    fun `die Spielhalle ist ein Innenraum mit Besuch`() {
        assertFalse(PlayScene.isOutdoors(PlayScene.Place.ARCADE))
        assertTrue(PlayScene.allowsVisitors(PlayScene.Place.ARCADE))
    }

    /** Ohne diesen Platz griffe der Ablauf ins Leere und die Figur bliebe einfach stehen. */
    @Test
    fun `jede Kreatur findet dort einen Automaten`() {
        for (species in AvatarSpecies.entries) {
            assertTrue(
                "$species",
                PlayScene.Station.ARCADE in PlayScene.stationsAt(PlayScene.Place.ARCADE, species)
            )
        }
    }

    @Test
    fun `die Freizeit fuehrt manchmal in die Spielhalle`() {
        val ablaeufe = PlayRoutines.allFor(AnimationType.GENERAL)
        assertTrue(ablaeufe.any { r ->
            r.steps.any { it is RoutineStep.GoToPlace && it.place == PlayScene.Place.ARCADE }
        })
    }

    /** Wer den Automaten einschaltet, schaltet ihn auch wieder aus - sonst liefe er ewig weiter. */
    @Test
    fun `der Automat wird ein- und wieder ausgeschaltet`() {
        val schalter = PlayRoutines.arcadeRoutine().steps.filterIsInstance<RoutineStep.Switch>()
        assertEquals(listOf(true, false), schalter.map { it.on })
        assertTrue(schalter.all { it.device == PlayScene.Station.ARCADE })
    }

    @Test
    fun `in der Spielhalle klingt die Spielhalle`() {
        val alle = MusicRole.entries.toSet() - MusicRole.CHARACTER_THEME
        for (phase in listOf(PlayAmbientActivity.DayPhase.MIDDAY, PlayAmbientActivity.DayPhase.EVENING)) {
            assertEquals(
                "$phase",
                MusicRole.ARCADE,
                MusicResolver.resolve(MusicContext(phase, PlayScene.Place.ARCADE), alle)
            )
        }
        // Ohne Spielhallenmusik bleibt es beim Tag - keine stille Halle.
        assertEquals(
            MusicRole.MAIN_DAY,
            MusicResolver.resolve(
                MusicContext(PlayAmbientActivity.DayPhase.MIDDAY, PlayScene.Place.ARCADE),
                setOf(MusicRole.MAIN_DAY, MusicRole.HOME_EVENING)
            )
        )
    }

    /** Mindestens ein Bewohner kommt tagsueber vorbei - sonst waere der Besuch eine leere Zusage. */
    @Test
    fun `tagsueber kommt jemand vorbei`() {
        assertTrue(LivingResidents.nextVisitor(PlayScene.Place.ARCADE, 14 * 60) != null)
    }
}
