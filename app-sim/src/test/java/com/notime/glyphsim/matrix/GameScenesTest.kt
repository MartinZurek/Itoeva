package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayControl.Dir
import com.notime.glyphsim.matrix.PlayControl.Pos
import com.notime.glyphsim.matrix.PlayScene.Place
import com.notime.glyphsim.matrix.PlayScene.Station
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prueft jeden gemalten Ort aus dem Katalog (siehe GameScenes, tools/world-art/build_all.py). */
class GameScenesTest {

    private val scenes = GameScenes.painted.map { GameScenes.of(it)!! }

    @Test
    fun `es gibt gemalte Orte und jeder hat sein Bild`() {
        assertTrue(scenes.isNotEmpty())
        val root = listOf(File("src/game/assets"), File("app-sim/src/game/assets")).first { it.isDirectory }
        for (scene in scenes) assertTrue("${scene.asset} fehlt", File(root, scene.asset).isFile)
    }

    @Test
    fun `hinten ist die Figur kleiner als vorn`() {
        for (scene in scenes) {
            assertTrue(scene.place.name, GameScenes.avatarHeight(scene, Pos(0.5f, 0f)) < GameScenes.avatarHeight(scene, Pos(0.5f, 1f)))
            assertTrue(scene.farY < scene.nearY)
            assertTrue(scene.farLeft < scene.farRight && scene.nearLeft < scene.nearRight)
        }
    }

    @Test
    fun `Fusspunkt und Stelle sind Umkehrungen`() {
        for (scene in scenes) {
            val pos = Pos(0.3f, 0.7f)
            val (x, y) = GameScenes.feet(scene, pos)
            val back = GameScenes.posAt(scene, x, y)
            assertEquals(pos.x, back.x, 1e-4f)
            assertEquals(pos.depth, back.depth, 1e-4f)
        }
    }

    @Test
    fun `jeder Platz steht auf begehbarem Boden, wird getroffen und gehoert zum Ort`() {
        for (scene in scenes) {
            val erlaubt = PlayScene.stationsAt(scene.place, AvatarSpecies.FENNEC).toSet()
            for (spot in scene.spots) {
                val name = "${scene.place} ${spot.station}"
                assertTrue("$name gibt es an diesem Ort nicht", spot.station in erlaubt)
                assertTrue("$name zu weit hinten", spot.standY >= scene.farY - 0.5f)
                assertTrue("$name zu weit vorn", spot.standY <= scene.nearY + 0.5f)
                val cx = (spot.hit.x0 + spot.hit.x1) / 2
                val cy = (spot.hit.y0 + spot.hit.y1) / 2
                assertNotNull("$name nicht zu treffen", GameScenes.spotAt(scene, cx, cy))
                val at = GameScenes.posAt(scene, spot.standX, spot.standY)
                assertEquals(name, spot.station, GameScenes.spotInReach(scene, at)?.station)
            }
            if (scene.spots.any { it.station == Station.DOOR }) {
                assertEquals(PlayControl.doorTarget(scene.place), scene.door)
            }
        }
    }

    @Test
    fun `die gemalten Orte schneiden keinen Ort von daheim ab`() {
        fun next(place: Place, dir: Dir): Place? =
            GameScenes.of(place)?.let { GameScenes.exit(it, dir) } ?: if (GameScenes.of(place) == null) PlayControl.neighbor(place, dir) else null
        fun reach(step: (Place, Dir) -> Place?, doors: Boolean): Set<Place> {
            val seen = mutableSetOf(Place.LIVING)
            val todo = ArrayDeque(listOf(Place.LIVING))
            while (todo.isNotEmpty()) {
                val at = todo.removeFirst()
                val targets = Dir.entries.mapNotNull { step(at, it) } +
                    listOfNotNull(if (doors) GameScenes.of(at)?.door else null)
                for (t in targets) if (seen.add(t)) todo.add(t)
            }
            return seen
        }
        val vorher = reach({ p, d -> PlayControl.neighbor(p, d) }, doors = false)
        val jetzt = reach(::next, doors = true)
        assertTrue(Place.STREET in jetzt)
        assertTrue("abgeschnitten: ${vorher - jetzt}", jetzt.containsAll(vorher))
    }

    @Test
    fun `das Bild fuellt den Bildschirm und schneidet eher oben ab`() {
        val scene = scenes.first()
        val fit = GameScenes.fit(scene, 2400f, 1080f)
        assertEquals(5f, fit.scale, 1e-4f)
        assertEquals(0f, fit.left, 1e-3f)
        assertTrue(fit.top < 0f)
        for (s in scenes) {
            val (_, y) = GameScenes.fit(s, 2400f, 1080f).toScreen(240f, s.nearY)
            assertTrue("${s.place}: vorderster Fusspunkt ausserhalb", y < 1080f)
        }
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

    @Test
    fun `kamera folgt der Tiefe nur wenn ein Bildueberstand vorhanden ist`() {
        val scene = scenes.first()
        val wide = GameScenes.fit(scene, 2400f, 1080f, 0.1f, 0.1f)
        val near = GameScenes.fit(scene, 2400f, 1080f, 0.9f, 0.9f)
        assertEquals(0f, wide.left, 1e-3f)
        assertEquals(wide.left, near.left, 1e-3f)
        assertTrue(near.top < wide.top)
        val square = GameScenes.fit(scene, 1080f, 1080f, 0.1f, 0.5f)
        val squareRight = GameScenes.fit(scene, 1080f, 1080f, 0.9f, 0.5f)
        assertTrue(squareRight.left < square.left)
    }

    @Test
    fun `laterne erhellt den Boden nur wenn sie eingeschaltet ist`() {
        val street = GameScenes.of(Place.STREET)!!
        val off = GameSceneLighting.sources(street, 23 * 60, lampOn = false, tvOn = false, phase = 0)
        val on = GameSceneLighting.sources(street, 23 * 60, lampOn = true, tvOn = false, phase = 0)
        assertTrue(off.none { it.tone == GameSceneLighting.Tone.WARM })
        assertTrue(on.any { it.tone == GameSceneLighting.Tone.WARM })
        val lamp = on.first { it.tone == GameSceneLighting.Tone.WARM }
        assertTrue(GameSceneLighting.illuminationAt(lamp.x, lamp.y, on) >
            GameSceneLighting.illuminationAt(lamp.x + 150f, lamp.y, on))
        assertEquals(0f, GameSceneLighting.daylight(23 * 60), 1e-4f)
    }

    @Test
    fun `bewegter Schatten liegt vom Licht abgewandt und folgt dem Fusspunkt`() {
        val street = GameScenes.of(Place.STREET)!!
        val light = GameSceneLighting.Light(50f, 120f, 180f, 0.5f, GameSceneLighting.Tone.WARM)
        val a = GameSceneLighting.shadow(street, Pos(0.3f, 0.5f), listOf(light))
        val b = GameSceneLighting.shadow(street, Pos(0.8f, 0.5f), listOf(light))
        assertTrue(a.tipX > a.footX)
        assertTrue(b.tipX > b.footX)
        assertTrue(b.footX > a.footX)
    }
}
