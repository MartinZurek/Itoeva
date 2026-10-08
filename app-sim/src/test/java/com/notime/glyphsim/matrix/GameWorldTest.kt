package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayControl.Dir
import com.notime.glyphsim.matrix.PlayControl.Pos
import com.notime.glyphsim.matrix.PlayScene.Place
import org.junit.Assert.*
import org.junit.Test

class GameWorldTest {
    @Test fun `beim Bewohnerwechsel gibt es genau eine sichtbare Identitaet`() {
        val profile = LivingResidents.all.first()
        val snapshot = ResidentSnapshot(profile.profileId, profile.role, profile.species, Place.PARK,
            com.notime.glyphsim.living.LivingSite.OUTSIDE, null, null, null, 0, 1, 480, true,
            nextSpecialActivity = null, currentAction = com.notime.glyphsim.living.ActionKind.READ)
        val actor = GameResidents.Actor(snapshot, Pos(.4f, .4f))
        val old = mapOf(Place.PARK to mapOf(profile.profileId to actor))
        val moved = snapshot.copy(place = Place.MEADOW)
        val result = GameWorld.residents(old, listOf(moved), 16L, 500L, null)
        assertEquals(1, result.values.sumOf { it.size })
        assertTrue(profile.profileId in result.getValue(Place.MEADOW))
        assertFalse(profile.profileId in result.getValue(Place.PARK))
    }
    @Test fun `das Panorama ist vorhanden und hat die gemeinsame Perspektive`() {
        val root = listOf(java.io.File("src/game/assets"), java.io.File("app-sim/src/game/assets")).first { it.isDirectory }
        // Android-JVM-Tests haben keine AWT-/ImageIO-Klassen. PNG-IHDR direkt lesen.
        java.io.DataInputStream(java.io.File(root, GameWorld.ASSET).inputStream()).use { png ->
            val signature = ByteArray(8).also(png::readFully)
            assertArrayEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a), signature)
            assertEquals(13, png.readInt())
            assertEquals(0x49484452, png.readInt()) // IHDR
            val width = png.readInt(); val height = png.readInt()
            assertTrue(width > 0 && height > 0)
            assertEquals(GameWorld.WIDTH / GameWorld.HEIGHT, width.toFloat() / height, .001f)
            assertTrue(width <= 4096 && height <= 4096)
        }
    }
    @Test fun `gemeinsame Raender erhalten Fusspunkt Groesse Geschwindigkeit und Gangphase`() {
        for ((from, to) in GameWorld.places.zipWithNext()) for (depth in listOf(0f, .5f, 1f)) {
            val state = GameMovement.State(Pos(1f, depth), PlayControl.Stick(.9f, 0f), Dir.RIGHT,
                gaitMs = 543.0, running = true, runBlend = .7f)
            val next = GameWorld.transfer(from, to, Dir.RIGHT, state)!!
            val a = GameWorld.feet(from, state.pos); val b = GameWorld.feet(to, next.pos)
            assertEquals(a.first, b.first, .0001f); assertEquals(a.second, b.second, .0001f)
            assertEquals(state.velocity, next.velocity); assertEquals(state.gaitMs, next.gaitMs, 0.0)
            assertEquals(state.running, next.running); assertEquals(state.runBlend, next.runBlend, 0f)
            assertEquals(GameScenes.avatarHeight(GameWorld.scene(from)!!, state.pos),
                GameScenes.avatarHeight(GameWorld.scene(to)!!, next.pos), 0f)
            assertEquals(state.pos, GameWorld.transfer(to, from, Dir.LEFT, next)!!.pos)
        }
    }
    @Test fun `seamlos gilt nur fuer benachbarte Abschnitte und Bodenbewegung`() {
        assertNull(GameWorld.transfer(Place.PARK, Place.SPORT, Dir.UP, GameMovement.State()))
        assertNull(GameWorld.transfer(Place.STREET, Place.FOREST, Dir.RIGHT, GameMovement.State()))
        assertNull(GameWorld.transfer(Place.STREET, Place.PARK, Dir.UP, GameMovement.State()))
        assertNull(GameWorld.transfer(Place.STREET, Place.PARK, Dir.LEFT, GameMovement.State()))
        assertNull(GameWorld.transfer(Place.STREET, Place.PARK, Dir.RIGHT, GameMovement.State(height = 5f)))
    }
    @Test fun `am gemeinsamen Rand braucht man nicht stehenzubleiben`() {
        var at = GameMovement.State(Pos(.995f, .7f), PlayControl.Stick(1f, 0f))
        var exited = false
        repeat(3) {
            val result = GameMovement.tick(at, PlayControl.Stick(1f, 0f), 16L, emptyList(),
                immediateExits = GameWorld.immediateExits(Place.STREET))
            at = result.state
            if (result.exit == Dir.RIGHT) { exited = true; assertTrue(at.moving) }
        }
        assertTrue(exited)
        assertNull(GameMovement.tick(GameMovement.State(Pos(.5f, 0f), PlayControl.Stick(0f, -1f)),
            PlayControl.Stick(0f, -1f), 16L, immediateExits = GameWorld.immediateExits(Place.STREET)).exit)
    }
    @Test fun `alle bisherigen Orte bleiben von daheim aus erreichbar und haben einen Rueckweg`() {
        for (place in Place.entries) {
            if (place == Place.LIVING) continue
            assertTrue("Hinweg $place fehlt", GameWorld.route(Place.LIVING, place).isNotEmpty())
            assertTrue("Rueckweg $place fehlt", GameWorld.route(place, Place.LIVING).isNotEmpty())
            var here = Place.LIVING
            for (step in GameWorld.route(here, place)) { assertTrue(step in GameWorld.neighbors(here)); here = step }
            assertEquals(place, here)
        }
        assertEquals(listOf(Place.PARK, Place.MEADOW, Place.FOREST), GameWorld.route(Place.STREET, Place.FOREST))
    }
    @Test fun `alle Stationsanker und Funde liegen im gemeinsamen begehbaren Boden`() {
        for (place in GameWorld.places) {
            val scene = GameWorld.scene(place)!!
            for (spot in scene.spots) {
                assertTrue(spot.standY in scene.farY..scene.nearY)
                assertEquals(spot.station, GameScenes.spotAt(scene,
                    (spot.hit.x0 + spot.hit.x1) / 2f, (spot.hit.y0 + spot.hit.y1) / 2f)?.station)
            }
            for (id in GameAdventure.ObjectId.entries.filter { it.place == place }) {
                val pos = GameAdventure.position(scene, id)
                assertTrue(pos.x in 0f..1f && pos.depth in 0f..1f)
                assertFalse(GameSurfaces.painted(scene).any { it.contains(pos) && it.heightAt(pos) > 1f })
            }
        }
    }
    @Test fun `Standort an beiden Seiten einer Naht kann ohne Versionswechsel gespeichert werden`() {
        for (place in GameWorld.places) for (x in listOf(.03f, .5f, .97f)) {
            val original = GameAdventure.State(place = place, pos = Pos(x, .9f),
                events = listOf(GameAdventure.Event.PLANTED), elapsed = 567890L)
            val restored = GameAdventure.decode(GameAdventure.encode(original))
            assertEquals(original.place, restored.place); assertEquals(original.pos, restored.pos)
            assertEquals(original.events, restored.events); assertEquals(original.elapsed, restored.elapsed)
        }
    }
    @Test fun `alte Geometrie bleibt nur Metadaten die aktive Welt verwendet neue Bilder`() {
        assertEquals("scenes/street.png", GameScenes.of(Place.STREET)!!.asset)
        assertEquals(196f, GameScenes.of(Place.STREET)!!.farY, 0f)
        assertEquals("interiors/living.png", GameWorld.scene(Place.LIVING)!!.asset)
        for (place in Place.entries) assertFalse(GameWorld.scene(place)!!.asset.startsWith("scenes/"))
    }
    @Test fun `Tiefe verlaesst keinen Aussenort und jede Tuer fuehrt zum selben Ort zurueck`() {
        for (place in GameWorld.places) {
            assertNull(GameWorld.exitAt(place, Dir.UP, Pos(.5f, 0f)))
            assertNull(GameWorld.exitAt(place, Dir.DOWN, Pos(.5f, 1f)))
        }
        for (place in Place.entries) for (door in GameWorld.passages(place).filter { it.door }) {
            val reverse = GameWorld.passages(door.to).single { it.to == place }
            assertTrue(reverse.door)
            assertEquals(reverse.pos.x, GameWorld.arrival(door).x, .001f)
            assertTrue(GameWorld.arrival(door).depth in .12f.. .88f)
        }
    }
    @Test fun `breite Abschnitte behalten das physische Lauftempo`() {
        fun distance(place: Place): Float {
            val state = GameMovement.State(Pos(.3f,.5f), PlayControl.Stick(.5f,0f))
            val next = GameMovement.tick(state, state.velocity, 50L,
                horizontalScale = GameWorld.horizontalScale(place)).state
            return GameWorld.feet(place,next.pos).first - GameWorld.feet(place,state.pos).first
        }
        assertEquals(distance(Place.STREET), distance(Place.CAMP), .001f)
    }
    @Test fun `alle Aussenorte teilen die Kamera und tieferes Wasser oeffnet die Nahansicht`() {
        assertEquals(1, GameWorld.places.map { GameCamera.key(GameWorld.scene(it)!!) }.distinct().size)
        val beach = GameWorld.scene(Place.BEACH)!!
        val back = GameCamera.tick(GameCamera.State(), beach, Pos(.5f,0f), PlayControl.Stick(),960f,540f,0L)
        val front = GameCamera.tick(GameCamera.State(), beach, Pos(.5f,1f), PlayControl.Stick(),960f,540f,0L)
        assertTrue(GameWorld.wetness(beach,Pos(.5f,1f)) > GameWorld.wetness(beach,Pos(.5f,0f)))
        assertTrue(front.zoom > back.zoom)
    }
}
