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
}
