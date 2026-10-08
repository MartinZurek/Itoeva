package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayControl.Pos
import com.notime.glyphsim.matrix.PlayScene.Place
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class GameLightingAtmosphereTest {
    private fun scene(place: Place) = GameWorld.scene(place)!!
    private fun lights(place: Place, minute: Int = 1380, lamp: Boolean = true) =
        GameSceneLighting.sources(scene(place), minute, lamp, false, 7)

    @Test fun `Fenster und Lampen folgen allen elf gelieferten Innenbildern`() {
        assertEquals(GameInteriorCatalog.scenes.keys, GameLightingCatalog.rooms.keys)
        for ((place, room) in GameLightingCatalog.rooms) {
            val day = lights(place, 720)
            val night = lights(place)
            assertTrue(day.any { it.kind == GameSceneLighting.Kind.WINDOW })
            assertFalse(night.any { it.kind == GameSceneLighting.Kind.WINDOW })
            val window = room.window!!
            assertTrue(window.left < window.right && window.top < window.bottom)
            assertTrue(window.bottom < window.floorY && window.floorY < 270f)
            for (lamp in room.lamps) {
                assertTrue(lamp.x in 0f..480f && lamp.y in 0f..270f)
                assertTrue(lamp.floorY > lamp.y)
                assertTrue(night.any { abs(it.x - lamp.x) < .001f && abs(it.y - lamp.y) < .001f })
            }
        }
    }
    @Test fun `Wohnzimmerlampe geht wirklich aus andere Raumleuchten bleiben erhalten`() {
        val anchor = GameLightingCatalog.rooms.getValue(Place.LIVING).lamps.first { it.switched }
        assertTrue(lights(Place.LIVING).any { it.x == anchor.x && it.y == anchor.y })
        assertFalse(lights(Place.LIVING, lamp = false).any { it.x == anchor.x && it.y == anchor.y })
    }
    @Test fun `ein Feuer bleibt ueber alle Ortskoordinaten derselbe Weltanker`() {
        for (place in GameWorld.places) {
            val source = lights(place).single { it.kind == GameSceneLighting.Kind.FIRE }
            assertEquals(GameWorld.origin(Place.CAMP) + 280f, GameWorld.origin(place) + source.x, .002f)
            assertEquals(445f, source.y, .001f)
        }
    }
    @Test fun `Lichtfarbe und Sonne springen an keiner Aussenortgrenze`() {
        for (minute in listOf(420, 720, 1140, 1380)) for ((a, b) in GameWorld.places.zipWithNext()) {
            for (species in AvatarSpecies.entries) {
                val sa = scene(a); val sb = scene(b)
                val pa = Pos(1f, .6f); val pb = Pos(0f, .6f)
                assertEquals(GameSceneLighting.duskAt(sa, GameWorld.region(a)!!.section, minute),
                    GameSceneLighting.duskAt(sb, 0f, minute), .001f)
                val ca = GameSceneLighting.character(sa, pa, species, minute, lights(a, minute))
                val cb = GameSceneLighting.character(sb, pb, species, minute, lights(b, minute))
                val aa = ca.at(.25f, .75f); val bb = cb.at(.25f, .75f)
                assertEquals("$a / $b / $minute / $species", aa.r, bb.r, .002f)
                assertEquals(aa.g, bb.g, .002f); assertEquals(aa.b, bb.b, .002f)
            }
        }
    }
    @Test fun `Hoehleneingang filtert Sonnenlicht stetig statt es am Ortsnamen abzuschalten`() {
        val cave = scene(Place.GROTTO)
        val sun = lights(Place.GROTTO, 720).single { it.directional }
        val near = GameSceneLighting.influence(0f, 500f, sun)
        val far = GameSceneLighting.influence(700f, 500f, sun)
        assertTrue(near > 0f); assertEquals(0f, far, .001f)
        assertEquals(GameSceneLighting.darknessAt(scene(Place.CAMP), 960f, 720),
            GameSceneLighting.darknessAt(cave, 0f, 720), .001f)
    }
    @Test fun `Lampe beleuchtet die physische Flanke und nicht die Blickrichtung`() {
        val room = scene(Place.LIVING)
        val pos = GameScenes.posAt(room, 190f, 220f)
        val light = GameSceneLighting.Light(115f, 150f, 300f, .8f, GameSceneLighting.Tone.WARM)
        val result = GameSceneLighting.character(room, pos, AvatarSpecies.FENNEC, 1380, listOf(light))
        assertTrue(result.at(0f).r > result.at(1f).r)
        assertTrue(result.at(0f).r > result.at(0f).b)
        val flipped = GameSceneLighting.character(room, pos, AvatarSpecies.FENNEC, 1380,
            listOf(light.copy(x = 300f)))
        assertTrue(flipped.at(1f).r > flipped.at(0f).r)
    }
    @Test fun `Kristalllicht wird kuehl und aendert niemals die Silhouette`() {
        val cave = scene(Place.GROTTO); val pos = GameScenes.posAt(cave, 610f, 505f)
        val result = GameSceneLighting.character(cave, pos, AvatarSpecies.FENNEC, 1380, lights(Place.GROTTO))
        assertTrue(result.at(.5f).b > result.at(.5f).r)
        // Die Farbwerte sind Multiplikatoren: es gibt keinen Alpha-/Leuchtaura-Kanal.
        for (u in listOf(0f, .5f, 1f)) for (v in listOf(0f, .5f, 1f)) {
            val rgb = result.at(u, v)
            assertTrue(rgb.r in .18f..1f && rgb.g in .18f..1f && rgb.b in .18f..1f)
        }
    }
    @Test fun `Sonnenwurf dreht ueber den Tag und bleibt an jedem Nahtpunkt stetig`() {
        val room = scene(Place.PARK); val pos = Pos(.5f, .6f)
        val morning = GameSceneLighting.shadows(room, pos, lights(Place.PARK, 480)).first { it.tipX != it.footX }
        val evening = GameSceneLighting.shadows(room, pos, lights(Place.PARK, 1080)).first { it.tipX != it.footX }
        assertTrue(morning.tipX > morning.footX); assertTrue(evening.tipX < evening.footX)
        for ((a, b) in GameWorld.places.zipWithNext()) {
            val aa = GameSceneLighting.shadows(scene(a), Pos(1f, .6f), lights(a, 720))
            val bb = GameSceneLighting.shadows(scene(b), Pos(0f, .6f), lights(b, 720))
            assertEquals(aa.size, bb.size)
            for (i in aa.indices) {
                assertEquals(GameWorld.origin(a) + aa[i].tipX, GameWorld.origin(b) + bb[i].tipX, .002f)
                assertEquals(aa[i].tipY, bb[i].tipY, .002f)
            }
        }
    }
    @Test fun `Sprung hebt den Bodenschatten nicht mit der Figur an`() {
        val room = scene(Place.PARK); val pos = Pos(.5f, .6f)
        val grounded = GameSceneLighting.shadows(room, pos, lights(Place.PARK)).first()
        val airborne = GameSceneLighting.shadows(room, pos, lights(Place.PARK), lift = 70f).first()
        assertEquals(grounded.footY, airborne.footY, .001f)
        assertTrue(airborne.width > grounded.width)
        assertTrue(airborne.opacity < grounded.opacity)
        assertTrue(airborne.softness > grounded.softness)
    }
    @Test fun `Plattformsupport hebt den Empfaenger bis zur vermessenen Oberkante`() {
        val room = scene(Place.KITCHEN); val piece = GameFurniture.pieces(room).single { it.id == "kitchen-table" }
        val pos = GameScenes.posAt(room, 195f, piece.ground)
        val height = piece.surface(room).heightAt(pos)
        val shadow = GameSceneLighting.shadows(room, pos, lights(Place.KITCHEN), lift = height, receiverHeight = height).first()
        assertEquals(piece.top, shadow.footY, .001f)
        assertEquals(.22f, shadow.opacity, .001f)
    }
    @Test fun `Niedrige Quelle wird durch einen Tisch verdeckt hohe Strahlen erreichen den Kopf`() {
        val room = scene(Place.KITCHEN)
        val lamp = GameSceneLighting.Light(196f, 150f, 300f, .8f, GameSceneLighting.Tone.WARM,
            groundY = 170f, height = 20f)
        assertTrue(GameSceneLighting.transmission(room, lamp, 196f, 240f, 0f) < .5f)
        assertEquals(1f, GameSceneLighting.transmission(room, lamp.copy(height = 230f), 196f, 240f, 110f), .001f)
    }
    @Test fun `Wind ist deterministisch und hat an Ortsgrenzen denselben Wert`() {
        for (clock in listOf(0L, 1333L, 10000L)) {
            for ((a, b) in GameWorld.places.zipWithNext()) {
                val x = GameWorld.origin(a) + GameWorld.region(a)!!.section
                assertEquals(GameAtmosphere.wind(x, clock), GameAtmosphere.wind(GameWorld.origin(b), clock), .0001f)
                assertTrue(abs(GameAtmosphere.wind(x, clock, PlayWeather.RAIN)) <= 1.125f)
            }
        }
    }
    @Test fun `Wind bleibt auch nach zehn Minuten zeitlich stetig`() {
        for (x in listOf(0f, 5300f, 13400f)) {
            val before = GameAtmosphere.wind(x, 599999L)
            val after = GameAtmosphere.wind(x, 600000L)
            assertTrue(abs(after - before) < .002f)
            assertTrue(GameAtmosphere.wind(x, Long.MAX_VALUE).isFinite())
        }
    }
    @Test fun `Windbewegung bleibt klein und laesst Wurzeln und Fuesse fest`() {
        val room = scene(Place.FOREST)
        val patch = GameAtmosphere.Patch(70f, 80f, 40f, 90f, 1)
        for (clock in 0L..10000L step 133L) {
            assertEquals(0f, GameAtmosphere.bend(patch, 1f, clock), .001f)
            assertTrue(abs(GameAtmosphere.bend(patch, 0f, clock)) < 1.5f)
            for (species in AvatarSpecies.entries) assertEquals(0f,
                GameAtmosphere.figureBend(room, species, 1f, clock, 1000f, PlayWeather.CLEAR), .001f)
        }
        assertEquals(0f, GameAtmosphere.figureBend(scene(Place.LIVING), AvatarSpecies.FENNEC, 0f,
            1000L, 0f, PlayWeather.CLEAR), .001f)
    }
    @Test fun `Keine Windtextur ragt aus dem zugehoerigen Bild`() {
        for (place in Place.entries) {
            val room = scene(place)
            for (patch in GameAtmosphere.patches(room)) {
                val sample = GameAtmosphere.sample(room, patch) ?: continue
                assertTrue(patch.x + sample.offset >= 3f && patch.x + sample.offset + patch.w + 3f <= sample.width)
                assertTrue(patch.y >= 0f && patch.y + patch.h <= sample.height)
            }
        }
        val forest = scene(Place.FOREST)
        val samples = GameAtmosphere.patches(forest).mapNotNull { GameAtmosphere.sample(forest, it) }
        assertTrue(samples.any { it.asset.startsWith("world/seams/") })
        val meadow = scene(Place.MEADOW)
        assertTrue(GameAtmosphere.patches(meadow).any { GameAtmosphere.sample(meadow, it) == null })
    }
    @Test fun `Staub folgt dem Licht Dunst bleibt auf passenden Orten`() {
        val fire = lights(Place.CAMP).single { it.kind == GameSceneLighting.Kind.FIRE }
        val a = GameAtmosphere.dust(fire, 3, 1200L)
        assertEquals(a, GameAtmosphere.dust(fire, 3, 1200L))
        assertTrue(a.alpha > 0f && a.alpha < .2f)
        assertEquals(0f, GameAtmosphere.dust(fire.copy(power = 0f), 3, 1200L).alpha, .001f)
        assertEquals(0f, GameAtmosphere.mist(Place.LIVING, 1380, 1200L), .001f)
        assertTrue(GameAtmosphere.mist(Place.SWAMP, 1380, 1200L) > GameAtmosphere.mist(Place.SWAMP, 720, 1200L))
    }
}
