package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayControl.Dir
import com.notime.glyphsim.matrix.PlayControl.Pos
import com.notime.glyphsim.matrix.PlayScene.Place
import com.notime.glyphsim.matrix.PlayScene.Station
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameScenesTest {

    @Test
    fun `hinten ist die Figur kleiner als vorn`() {
        for (scene in listOf(GameScenes.PARK, GameScenes.LIVING)) {
            assertTrue(GameScenes.avatarHeight(scene, Pos(0.5f, 0f)) < GameScenes.avatarHeight(scene, Pos(0.5f, 1f)))
            val (_, farY) = GameScenes.feet(scene, Pos(0.5f, 0f))
            val (_, nearY) = GameScenes.feet(scene, Pos(0.5f, 1f))
            assertTrue(farY < nearY)
        }
    }

    @Test
    fun `Fusspunkt und Stelle sind Umkehrungen`() {
        val scene = GameScenes.PARK
        val pos = Pos(0.3f, 0.7f)
        val (x, y) = GameScenes.feet(scene, pos)
        val back = GameScenes.posAt(scene, x, y)
        assertEquals(pos.x, back.x, 1e-4f)
        assertEquals(pos.depth, back.depth, 1e-4f)
    }

    @Test
    fun `jeder Platz steht auf begehbarem Boden und wird getroffen`() {
        for (scene in listOf(GameScenes.PARK, GameScenes.LIVING)) {
            for (spot in scene.spots) {
                assertTrue("${spot.station} zu weit hinten", spot.standY >= scene.farY - 0.5f)
                assertTrue("${spot.station} zu weit vorn", spot.standY <= scene.nearY + 0.5f)
                val cx = (spot.hit.x0 + spot.hit.x1) / 2
                val cy = (spot.hit.y0 + spot.hit.y1) / 2
                assertEquals(spot.station, GameScenes.spotAt(scene, cx, cy)?.station)
                // Wer am Standort steht, hat den Platz in Reichweite.
                val at = GameScenes.posAt(scene, spot.standX, spot.standY)
                assertEquals(spot.station, GameScenes.spotInReach(scene, at)?.station)
            }
        }
    }

    @Test
    fun `Zimmer und Park sind verbunden, das Gelaender nicht`() {
        assertEquals(Place.PARK, GameScenes.exit(GameScenes.LIVING, Dir.RIGHT))
        assertEquals(Place.LIVING, GameScenes.exit(GameScenes.PARK, Dir.LEFT))
        assertNull(GameScenes.exit(GameScenes.PARK, Dir.UP))
        assertNull(GameScenes.exit(GameScenes.LIVING, Dir.UP))
        assertEquals(Place.LIVING, GameScenes.PARK.door)
        assertEquals(Station.DOOR, GameScenes.spotAt(GameScenes.PARK, 80f, 170f)?.station)
    }

    @Test
    fun `das Bild fuellt den Bildschirm und schneidet eher oben ab`() {
        val fit = GameScenes.fit(GameScenes.PARK, 2400f, 1080f)
        assertEquals(5f, fit.scale, 1e-4f)
        assertEquals(0f, fit.left, 1e-3f)
        assertTrue(fit.top < 0f)
        // Der vorderste Fusspunkt bleibt sichtbar.
        val (_, y) = fit.toScreen(240f, GameScenes.PARK.nearY)
        assertTrue(y < 1080f)
        val (ix, iy) = fit.toImage(fit.toScreen(100f, 200f).first, fit.toScreen(100f, 200f).second)
        assertEquals(100f, ix, 1e-3f)
        assertEquals(200f, iy, 1e-3f)
    }

    @Test
    fun `zum Platz gehen kommt an`() {
        var pos = Pos(0.1f, 0.1f)
        val target = Pos(0.8f, 0.9f)
        repeat(200) { pos = GameScenes.approach(pos, target, 50) }
        assertEquals(target.x, pos.x, 1e-4f)
        assertEquals(target.depth, pos.depth, 1e-4f)
    }
}
