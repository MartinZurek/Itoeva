package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayAmbientActivity.DayPhase
import com.notime.glyphsim.matrix.PlayScene.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayPastimeTest {

    @Test
    fun `ueberall gibt es etwas Ruhiges zu tun - ohne Ortswechsel und nur mit dem, was da ist`() {
        for (place in Place.entries) for (phase in DayPhase.entries) for (regen in listOf(false, true)) {
            val szenen = PlayPastime.choicesFor(place, phase, regen)
            assertTrue("$place/$phase: nichts", szenen.isNotEmpty())
            for (r in szenen) {
                assertTrue("$place: Ortswechsel", r.steps.none { it is RoutineStep.GoToPlace })
                assertTrue("$place: nimmt etwas in die Hand", r.steps.none { it is RoutineStep.Take })
                for (step in r.steps.filterIsInstance<RoutineStep.GoTo>()) {
                    assertTrue("$place: ${step.station}", step.station in PlayScene.stationsAt(place))
                }
                // Wer sich setzt, steht auch wieder auf.
                assertEquals(r.steps.count { it is RoutineStep.Occupy }, r.steps.count { it == RoutineStep.Rise })
                // Und es passiert wirklich etwas - nicht nur Warten.
                assertTrue(r.steps.any { it !is RoutineStep.Linger })
            }
        }
    }

    @Test
    fun `draussen am Tag wird Voegeln zugesehen, im Regen und nachts nicht`() {
        fun voegel(place: Place, phase: DayPhase, regen: Boolean) = PlayPastime.choicesFor(place, phase, regen)
            .any { r -> r.steps.any { it is RoutineStep.Watch && it.kind == PlayEffects.WatchKind.BIRD } }
        assertTrue(voegel(Place.PARK, DayPhase.MIDDAY, false))
        assertTrue(voegel(Place.MOUNTAINS, DayPhase.MORNING, false))
        assertFalse(voegel(Place.PARK, DayPhase.MIDDAY, true))
        assertFalse(voegel(Place.PARK, DayPhase.NIGHT, false))
        assertFalse(voegel(Place.LIVING, DayPhase.MIDDAY, false))
    }

    @Test
    fun `was kommt, geht auch wieder`() {
        for (place in Place.entries) for (phase in DayPhase.entries) {
            for (r in PlayPastime.choicesFor(place, phase, false)) {
                val watch = r.steps.filterIsInstance<RoutineStep.Watch>()
                if (watch.isEmpty()) continue
                assertEquals(PlayEffects.WatchPhase.COME, watch.first().phase)
                assertEquals(PlayEffects.WatchPhase.GO, watch.last().phase)
            }
        }
    }

    @Test
    fun `der Vogel landet am Boden und fliegt davon`() {
        val landen = PlayEffects.watchCells(PlayEffects.WatchKind.BIRD, PlayEffects.WatchPhase.STAY, 10, 20, 5, 120)
        val boden = 20 + AvatarGeometry.HEIGHT - 1
        assertTrue(landen.isNotEmpty())
        assertTrue("sitzt nicht am Boden", landen.maxOf { it.y } >= boden - 1)
        val fort = PlayEffects.watchCells(PlayEffects.WatchKind.BIRD, PlayEffects.WatchPhase.GO, 10, 20, 12, 120)
        assertTrue("fliegt nicht nach oben", fort.minOf { it.y } < landen.minOf { it.y } - 10)
        val falter = PlayEffects.watchCells(PlayEffects.WatchKind.BUTTERFLY, PlayEffects.WatchPhase.STAY, 10, 20, 3, 120)
        assertTrue(falter.isNotEmpty())
    }

    @Test
    fun `ruhige Szenen kommen fast so oft wie die grossen Handlungen, Leerlauf kaum`() {
        val random = kotlin.random.Random(7)
        val zaehler = (1..2_000).map { PlayAmbientActivity.nextAction(random) }.groupingBy { it }.eachCount()
        val tun = (zaehler[PlayAmbientActivity.Action.PERFORM] ?: 0) + (zaehler[PlayAmbientActivity.Action.PASTIME] ?: 0)
        assertTrue("nur $tun von 2000 mit Inhalt", tun > 1_400)
    }
}
