package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.matrix.PlayMap.Region
import com.notime.glyphsim.matrix.PlayScene.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayMapTest {

    @Test
    fun `jeder Ort ist von jedem aus erreichbar, und jeder Weg geht nur von Nachbar zu Nachbar`() {
        for (from in Place.entries) for (to in Place.entries) {
            val weg = PlayMap.route(from, to)
            if (from == to) {
                assertTrue(weg.isEmpty())
                continue
            }
            assertEquals("$from -> $to", to, weg.lastOrNull())
            (listOf(from) + weg).zipWithNext().forEach { (a, b) ->
                assertTrue("$a liegt nicht an $b", b in PlayMap.neighbors(a))
            }
        }
    }

    @Test
    fun `die Wege gehen in beide Richtungen`() {
        for (a in Place.entries) for (b in PlayMap.neighbors(a)) {
            assertTrue("$a -> $b, aber nicht zurueck", a in PlayMap.neighbors(b))
        }
    }

    @Test
    fun `vom Gebirge zum Sportplatz geht es ueber Wiese und Park`() {
        val weg = PlayMap.route(Place.MOUNTAINS, Place.SPORT)
        assertEquals(4, weg.size)
        assertEquals(listOf(Place.MEADOW, Place.PARK, Place.SPORT), weg.drop(1))
        assertFalse(Place.SPORT in PlayMap.neighbors(Place.MOUNTAINS))
    }

    @Test
    fun `aus der Wohnung geht es nur durchs Wohnzimmer hinaus`() {
        for (room in Place.entries.filter { PlayMap.regionOf(it) == Region.HOME }) {
            for (n in PlayMap.neighbors(room)) {
                if (PlayMap.regionOf(n) != Region.HOME) {
                    assertEquals(Place.LIVING, room)
                    assertEquals(Place.STREET, n)
                }
            }
        }
    }

    @Test
    fun `benachbarte Gegenden liegen auch auf der Karte nebeneinander`() {
        // Kein Weg springt ueber eine Gegend hinweg - von daheim nie direkt ins Gruen oder in
        // die Wildnis.
        for (a in Place.entries) for (b in PlayMap.neighbors(a)) {
            val sprung = kotlin.math.abs(PlayMap.regionOf(a).ordinal - PlayMap.regionOf(b).ordinal)
            assertTrue("$a -> $b springt ueber eine Gegend", sprung <= 1)
        }
    }

    @Test
    fun `draussen wird vor Ort gehandelt, was weit drinnen laege`() {
        assertTrue(PlayMap.staysLocal(Place.MOUNTAINS, Place.KITCHEN))
        assertTrue(PlayMap.staysLocal(Place.MOUNTAINS, Place.SPORT))
        assertTrue(PlayMap.staysLocal(Place.FOREST, Place.KITCHEN))
        // Kurze Wege werden gegangen, und daheim ohnehin.
        assertFalse(PlayMap.staysLocal(Place.LIVING, Place.KITCHEN))
        assertFalse(PlayMap.staysLocal(Place.POND, Place.SPORT))
        assertFalse(PlayMap.staysLocal(Place.STREET, Place.KITCHEN))
        // Weiter hinaus wird gereist.
        assertFalse(PlayMap.staysLocal(Place.MOUNTAINS, Place.BEACH))
        assertFalse(PlayMap.staysLocal(Place.LIVING, Place.MOUNTAINS))
    }

    @Test
    fun `wer im Gebirge Durst hat, geht nicht nach Hause`() {
        val daheim = PlayRoutine(
            listOf(
                RoutineStep.GoToPlace(Place.KITCHEN), RoutineStep.GoTo(PlayScene.Station.FRIDGE),
                RoutineStep.Act(AnimationType.DRINK)
            )
        )
        val draussen = PlayMap.fromHere(daheim, AnimationType.DRINK, Place.MOUNTAINS, 0)
        assertTrue(draussen.local)
        assertTrue(draussen.routine.steps.none { it is RoutineStep.GoToPlace })
        assertTrue(RoutineStep.Act(AnimationType.DRINK) in draussen.routine.steps)
        // Im Wohnzimmer bleibt es der Weg in die Kueche.
        val drinnen = PlayMap.fromHere(daheim, AnimationType.DRINK, Place.LIVING, 0)
        assertFalse(drinnen.local)
        assertEquals(daheim, drinnen.routine)
    }

    @Test
    fun `ein Ausflug von draussen geht nicht erst ueber die Strasse`() {
        val ausflug = PlayRoutines.excursion(Place.FOREST, AvatarAnimations.Fidget.LOOK_AROUND, 5_000L)
        val vomGebirge = PlayMap.trimmedFrom(ausflug, Place.MOUNTAINS)
        assertEquals(
            listOf(Place.FOREST),
            vomGebirge.steps.filterIsInstance<RoutineStep.GoToPlace>().map { it.place }
        )
        // Von daheim fuehrt derselbe Ausflug weiterhin ueber die Strasse.
        assertEquals(ausflug, PlayMap.trimmedFrom(ausflug, Place.LIVING))
    }

    @Test
    fun `vor Ort gibt es alles ausser Schlafen, Arbeiten und Schreibtisch - ohne Ortswechsel`() {
        val draussen = Place.entries.filter { PlayMap.regionOf(it) >= Region.GREEN }
        for (place in draussen) for (topic in AnimationType.entries) for (roll in 0 until 6) {
            val r = PlayMap.onTheSpot(topic, place, roll)
            if (topic in setOf(AnimationType.SLEEP, AnimationType.WORK, AnimationType.FOCUS)) {
                assertEquals(null, r)
                continue
            }
            assertNotNull("$topic/$place", r)
            r!!
            assertTrue("$topic/$place", r.steps.none { it is RoutineStep.GoToPlace })
            for (step in r.steps.filterIsInstance<RoutineStep.GoTo>()) {
                assertTrue("$topic/$place: ${step.station}", step.station in PlayScene.stationsAt(place))
            }
            assertEquals(
                "$topic/$place: Genommenes wird wieder abgelegt",
                r.steps.count { it is RoutineStep.Take }, r.steps.count { it == RoutineStep.Drop }
            )
        }
    }

    @Test
    fun `Sport im Gebirge heisst sprinten, Saltos oder einen Stein stemmen`() {
        val arten = (0 until 6).mapNotNull { PlayMap.onTheSpot(AnimationType.MOVE, Place.MOUNTAINS, it) }
        assertTrue(arten.any { r -> RoutineStep.Stir(AvatarAnimations.Fidget.FLIP) in r.steps })
        assertTrue(arten.any { r ->
            r.steps.filterIsInstance<RoutineStep.Training>().any { it.gear == PlayEffects.TrainingGear.STONE }
        })
        assertTrue(arten.any { r -> r.steps.count { it is RoutineStep.Stroll } >= 3 })
        // Nirgends die Hantel.
        assertTrue(arten.all { r ->
            r.steps.filterIsInstance<RoutineStep.Training>().none { it.gear == PlayEffects.TrainingGear.DUMBBELL }
        })
    }

    @Test
    fun `durch Zimmer und Strassen geht es zuegig, draussen wird stehengeblieben`() {
        assertFalse(PlayMap.lingersAt(Place.LIVING))
        assertFalse(PlayMap.lingersAt(Place.STREET))
        assertTrue(PlayMap.lingersAt(Place.MEADOW))
        assertTrue(PlayMap.lingersAt(Place.PLAINS))
    }
}
