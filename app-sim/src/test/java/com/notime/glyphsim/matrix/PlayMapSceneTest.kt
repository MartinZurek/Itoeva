package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayScene.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayMapSceneTest {

    private val breiten = listOf(PlayScene.MIN_SCENE_CELLS, 46, 54, 64, 72)
    private val boeden = listOf(24, 40, 82)

    @Test
    fun `die Karte erscheint nur vor weiten Wegen in eine andere Gegend`() {
        assertTrue(PlayMap.showsMap(Place.BEDROOM, Place.MOUNTAINS))
        assertTrue(PlayMap.showsMap(Place.MOUNTAINS, Place.SPORT))
        assertFalse(PlayMap.showsMap(Place.BEDROOM, Place.KITCHEN))
        assertFalse(PlayMap.showsMap(Place.PARK, Place.POND))
        assertFalse(PlayMap.showsMap(Place.LIVING, Place.STREET))
    }

    @Test
    fun `jeder Ort hat auf der Karte eine eigene Stelle`() {
        for (breite in breiten) for (boden in boeden) {
            val leer = PlayMapScene.build(Place.LIVING, emptyList(), breite, boden, 0f, false)
                .map { it.x to it.y }.toSet()
            // Jedes Ziel, einzeln angesteuert, hebt eine andere Stelle hervor.
            val ziele = Place.entries.filter { PlayMap.regionOf(it) != PlayMap.Region.HOME }
            val hervorgehoben = ziele.map { ziel ->
                PlayMapScene.build(Place.LIVING, PlayMap.route(Place.LIVING, ziel), breite, boden, 1f, true)
                    .filter { it.isLight }.map { it.x to it.y }.toSet()
            }
            assertEquals("$breite/$boden", ziele.size, hervorgehoben.distinct().size)
            assertTrue(leer.isNotEmpty())
        }
    }

    @Test
    fun `alles liegt im Bild und ueber dem Boden`() {
        for (breite in breiten) for (boden in boeden) for (von in Place.entries) for (nach in Place.entries) {
            val zellen = PlayMapScene.build(von, PlayMap.route(von, nach), breite, boden, 1f, true)
            assertTrue(zellen.all { it.x in 0 until breite && it.y in 0 until boden })
            assertTrue(zellen.all { it.brightness in 1..PlayScene.GLOW })
        }
    }

    @Test
    fun `der Weg waechst Ort fuer Ort`() {
        val weg = PlayMap.route(Place.BEDROOM, Place.GROTTO)
        val hell = { anteil: Float ->
            PlayMapScene.build(Place.BEDROOM, weg, 54, 24, anteil, false)
                .count { it.brightness >= PlayScene.GLOW - 600 }
        }
        assertTrue(hell(0f) < hell(0.5f))
        assertTrue(hell(0.5f) < hell(1f))
    }
}
