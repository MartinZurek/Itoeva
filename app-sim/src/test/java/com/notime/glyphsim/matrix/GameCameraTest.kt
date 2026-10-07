package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayControl.Pos
import com.notime.glyphsim.matrix.PlayControl.Stick
import com.notime.glyphsim.matrix.PlayScene.Place
import org.junit.Assert.*
import org.junit.Test

class GameCameraTest {
    private val scene = GameWorld.scene(Place.PARK)!!
    @Test fun `Kamera und Touchprojektion sind in allen Zoomstufen invers`() {
        for (zoom in listOf(1.4f, 1.55f, 1.85f)) for (w in listOf(1280f, 2400f)) {
            val state = GameCamera.State(GameWorld.ASSET, 830f, 407f, zoom)
            for (place in GameWorld.places) {
                val fit = GameCamera.fit(state, GameWorld.scene(place)!!, w, 1080f)
                val point = GameScenes.feet(GameWorld.scene(place)!!, Pos(.45f, .7f))
                val screen = fit.toScreen(point.first, point.second)
                val back = fit.toImage(screen.first, screen.second)
                assertEquals(point.first, back.first, .001f); assertEquals(point.second, back.second, .001f)
            }
        }
    }
    @Test fun `am Ortswechsel bleibt die Kameraprojektion exakt dieselbe`() {
        val camera = GameCamera.tick(GameCamera.State(), scene, Pos(.95f, .5f), Stick(1f, 0f), 1920f, 1080f, 16L)
        for ((from, to) in GameWorld.places.zipWithNext()) {
            val a = GameCamera.fit(camera, GameWorld.scene(from)!!, 1920f, 1080f)
                .toScreen(480f, 533f)
            val b = GameCamera.fit(camera, GameWorld.scene(to)!!, 1920f, 1080f)
                .toScreen(0f, 533f)
            assertEquals(a.first, b.first, .001f); assertEquals(a.second, b.second, .001f)
        }
    }
    @Test fun `Kameranachlauf ist bei verschiedenen Bildraten gleich`() {
        val initial = GameCamera.tick(GameCamera.State(), scene, Pos(.1f, .5f), Stick(), 1920f, 1080f, 0L)
        fun run(dt: Long): GameCamera.State {
            var camera = initial
            repeat((1200L / dt).toInt()) { camera = GameCamera.tick(camera, scene, Pos(.8f, .5f), Stick(), 1920f, 1080f, dt) }
            return camera
        }
        val a = run(20); val b = run(50)
        assertEquals(a.x, b.x, .01f); assertEquals(a.y, b.y, .01f); assertEquals(a.zoom, b.zoom, .001f)
    }
    @Test fun `Nahe Objekte zoomen sanft und haben getrennte Ein und Austrittsgrenzen`() {
        var camera = GameCamera.tick(GameCamera.State(), scene, Pos(.5f, .5f), Stick(), 1920f, 1080f, 0, 100f)
        val before = camera.zoom
        camera = GameCamera.tick(camera, scene, Pos(.5f, .5f), Stick(), 1920f, 1080f, 16, 35f)
        assertTrue(camera.close); assertTrue(camera.zoom > before && camera.zoom < 1.85f)
        camera = GameCamera.tick(camera, scene, Pos(.5f, .5f), Stick(), 1920f, 1080f, 16, 60f)
        assertTrue(camera.close)
        camera = GameCamera.tick(camera, scene, Pos(.5f, .5f), Stick(), 1920f, 1080f, 16, 75f)
        assertFalse(camera.close)
    }
    @Test fun `Bildraender bleiben auch im Pausenmenue nach Formatwechsel bedeckt`() {
        for (place in listOf(Place.STREET, Place.FOREST, Place.LIVING)) {
            val s = GameWorld.scene(place)!!
            val world = GameWorld.isWorld(s)
            val initial = GameCamera.tick(GameCamera.State(), s, Pos(1f, 1f), Stick(), 1280f, 720f, 0L)
            for ((w, h) in listOf(2400f to 1080f, 1280f to 800f, 1080f to 2400f)) {
                val fit = GameCamera.fit(initial, s, w, h)
                val left = fit.left - if (world) GameWorld.origin(place) * fit.scale else 0f
                assertTrue(left <= .01f); assertTrue(fit.top <= .01f)
                assertTrue(left + (if (world) GameWorld.WIDTH else 480f) * fit.scale >= w - .01f)
                assertTrue(fit.top + (if (world) GameWorld.HEIGHT else 270f) * fit.scale >= h - .01f)
            }
        }
    }
    @Test fun `neuer Innenraum setzt Kamera kontrolliert auf seinen eigenen Boden`() {
        val old = GameCamera.tick(GameCamera.State(), scene, Pos(), Stick(), 1920f, 1080f, 0)
        val living = GameWorld.scene(Place.LIVING)!!
        val next = GameCamera.tick(old, living, Pos(.7f, .8f), Stick(), 1920f, 1080f, 16)
        assertEquals(living.asset, next.key)
        assertTrue(next.x in 0f..480f && next.y in 0f..270f)
    }
}
