package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayControl.Dir
import com.notime.glyphsim.matrix.PlayControl.Pos
import com.notime.glyphsim.matrix.PlayScene.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayControlTest {

    @Test
    fun `rechts geht nach rechts, hoch nach hinten`() {
        val start = Pos(0.5f, 0.5f)
        assertTrue(PlayControl.step(start, Dir.RIGHT, 100).pos.x > 0.5f)
        assertTrue(PlayControl.step(start, Dir.LEFT, 100).pos.x < 0.5f)
        assertTrue(PlayControl.step(start, Dir.UP, 100).pos.depth < 0.5f)
        assertTrue(PlayControl.step(start, Dir.DOWN, 100).pos.depth > 0.5f)
        assertEquals(0.5f, PlayControl.step(start, Dir.UP, 100).pos.x, 0f)
    }

    @Test
    fun `hinaus geht es erst nach kurzem Druecken gegen den Rand`() {
        var pos = Pos(0.99f, 0.5f)
        var exit: Dir? = null
        var ms = 0L
        while (exit == null && ms < 2_000) {
            val step = PlayControl.step(pos, Dir.RIGHT, 16)
            pos = step.pos
            exit = step.exit
            ms += 16
        }
        assertEquals(Dir.RIGHT, exit)
        assertTrue("zu frueh: $ms ms", ms >= PlayControl.EXIT_PUSH_MS)
        assertTrue(pos.x <= 1f)
    }

    @Test
    fun `daheim liegen die Zimmer an einem Flur, rechts hinaus auf die Strasse`() {
        assertEquals(Place.STREET, PlayControl.neighbor(Place.LIVING, Dir.RIGHT))
        assertEquals(Place.KITCHEN, PlayControl.neighbor(Place.LIVING, Dir.LEFT))
        assertNull(PlayControl.neighbor(Place.BEDROOM, Dir.LEFT))
        assertNull(PlayControl.neighbor(Place.KITCHEN, Dir.UP))
        assertEquals(Place.LIVING, PlayControl.neighbor(Place.STREET, Dir.LEFT))
    }

    @Test
    fun `jeder Ort ist zu Fuss erreichbar, und jeder Weg fuehrt zurueck`() {
        val erreicht = mutableSetOf(Place.BEDROOM)
        val offen = ArrayDeque(listOf(Place.BEDROOM))
        while (offen.isNotEmpty()) {
            val hier = offen.removeFirst()
            for (dir in Dir.entries) {
                val dort = PlayControl.neighbor(hier, dir) ?: continue
                assertTrue("$hier -$dir-> $dort ist kein Weg der Karte",
                    dort in PlayMap.neighbors(hier) || (hier in PlayControl.HOUSE && dort in PlayControl.HOUSE))
                if (erreicht.add(dort)) offen += dort
            }
        }
        assertEquals(Place.entries.toSet(), erreicht)
        // Zurueck: Von jedem Ort aus fuehrt ein Weg wieder nach Hause.
        for (start in Place.entries) {
            val gesehen = mutableSetOf(start)
            val q = ArrayDeque(listOf(start))
            while (q.isNotEmpty()) {
                val hier = q.removeFirst()
                for (dir in Dir.entries) PlayControl.neighbor(hier, dir)?.let { if (gesehen.add(it)) q += it }
            }
            assertTrue("von $start kein Weg nach Hause", Place.LIVING in gesehen)
        }
    }

    @Test
    fun `wer rechts hinausgeht, kommt links herein`() {
        assertEquals(0f, PlayControl.entry(Dir.RIGHT, Pos(1f, 0.3f)).x, 0f)
        assertEquals(0.3f, PlayControl.entry(Dir.RIGHT, Pos(1f, 0.3f)).depth, 0f)
        assertEquals(1f, PlayControl.entry(Dir.UP, Pos(0.4f, 0f)).depth, 0f)
        assertEquals(0.4f, PlayControl.entry(Dir.UP, Pos(0.4f, 0f)).x, 0f)
    }

    @Test
    fun `die Aktionstaste nimmt den naechsten Platz in Reichweite, nie die Tuer`() {
        val plaetze = mapOf(
            PlayScene.Station.BED to 0.2f, PlayScene.Station.LAMP to 0.3f, PlayScene.Station.DOOR to 0.9f
        )
        assertEquals(PlayScene.Station.BED, PlayControl.stationInReach(0.22f, plaetze))
        assertEquals(PlayScene.Station.LAMP, PlayControl.stationInReach(0.29f, plaetze))
        assertNull(PlayControl.stationInReach(0.9f, plaetze))
        assertNull(PlayControl.stationInReach(0.6f, plaetze))
    }

    @Test
    fun `an jedem Platz ausser der Tuer gibt es etwas zu tun`() {
        for (station in PlayScene.Station.entries) {
            val ablauf = PlayControl.actionAt(station)
            if (station == PlayScene.Station.DOOR) {
                assertNull(ablauf)
                continue
            }
            requireNotNull(ablauf)
            assertEquals(RoutineStep.GoTo(station), ablauf.steps.first())
            // Wer sich hinsetzt, steht am Ende wieder auf - sonst kaeme man nicht mehr weg.
            if (ablauf.steps.any { it is RoutineStep.Occupy }) assertEquals(RoutineStep.Rise, ablauf.steps.last())
        }
        assertEquals(
            RoutineStep.Switch(PlayScene.Station.LAMP, false),
            PlayControl.actionAt(PlayScene.Station.LAMP, lampOn = true)!!.steps.last()
        )
    }

    @Test
    fun `jeder Ort mit Plaetzen bietet mindestens eine Handlung`() {
        for (place in Place.entries) {
            val plaetze = PlayScene.stationsAt(place).filter { it != PlayScene.Station.DOOR }
            assertTrue("$place", plaetze.all { PlayControl.actionAt(it) != null })
        }
    }
}
