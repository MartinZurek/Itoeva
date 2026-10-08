package com.notime.glyphsim.matrix

import org.junit.Assert.*
import org.junit.Test

class GameWorldPolishTest {
    @Test fun `Alle Innenbilder besitzen gueltige Konturen und feste Oberkanten`() {
        for ((_, scene) in GameInteriorCatalog.scenes) {
            val pieces = GameFurniture.pieces(scene)
            assertTrue(scene.place.name, pieces.isNotEmpty())
            assertEquals(pieces.size, pieces.map { it.id }.distinct().size)
            for (p in pieces) {
                assertTrue(p.left < p.right && p.top < p.ground && p.back < p.front)
                assertTrue(p.contours.all { it.size >= 3 && it.all { xy -> xy.first in 0f..480f && xy.second in 0f..270f } })
                val surface = p.surface(scene)
                assertTrue(surface.x0 <= surface.x1 && surface.d0 <= surface.d1)
                for (d in listOf(surface.d0, surface.anchorDepth, surface.d1)) {
                    val at = PlayControl.Pos((surface.x0 + surface.x1) / 2f, d)
                    assertEquals(p.id, p.top, GameScenes.feet(scene, at).second - surface.heightAt(at), .01f)
                }
            }
            assertSame(pieces, GameFurniture.pieces(scene))
        }
    }
    @Test fun `Jeder Aktionsanker bleibt von beiden Seiten erreichbar`() {
        for ((_, scene) in GameInteriorCatalog.scenes) for (spot in scene.spots) {
            val surfaces = GameSurfaces.painted(scene)
            val target = GameAdventure.safePosition(scene.place, GameScenes.posAt(scene, spot.standX, spot.standY), surfaces)
            for (x in listOf(.04f, .96f)) {
                var pos = GameAdventure.safePosition(scene.place, PlayControl.Pos(x, .95f), surfaces)
                repeat(1800) { pos = GameSurfaces.approach(scene, pos, target, 50) }
                assertEquals("${scene.place}/${spot.station}/$x", target, pos)
            }
        }
    }
    @Test fun `Sitzen beginnt ohne Sprung und kehrt nach dem Aufstehen zum Anker zurueck`() {
        val scene = GameWorld.scene(PlayScene.Place.LIVING)!!
        for (species in AvatarSpecies.entries) {
            val from = GameScenes.posAt(scene, 238f, 182f)
            val seat = GameSeating.begin(scene, PlayScene.Station.SEAT, from, 1000)!!
            val first = GameSeating.frame(scene, species, seat, 1000)
            assertEquals(from, first.pos); assertEquals(0f, first.lift, .001f)
            val held = GameSeating.frame(scene, species, seat, 1420)
            assertNull("Die gehaltene Sitzposition darf die Aktivitaet nicht ueberdecken", held.motion)
            assertTrue(held.lift > 0f && held.receiver > held.lift)
            assertEquals(seat.piece.seat!!.x, GameScenes.feet(scene, held.pos).first, .01f)
            val rising = seat.copy(rise = 2000)
            assertEquals(CreatureSprites.Motion.RISE, GameSeating.frame(scene, species, rising, 2000).motion!!.motion)
            assertEquals(held.pos, GameSeating.frame(scene, species, rising, 2000).pos)
            val end = GameSeating.frame(scene, species, rising, 2360)
            assertTrue(end.complete); assertEquals(from, end.pos); assertEquals(0f, end.lift, .001f)
        }
    }
    @Test fun `Sitzender Bewohner wird an seiner sichtbaren Position getroffen und sortiert`() {
        val scene = GameWorld.scene(PlayScene.Place.LIVING)!!
        val resident = LivingPopulation.snapshot(LivingPopulation.initial(480)).first()
        val from = GameSeating.approach(scene, PlayScene.Station.SEAT)!!
        val seat = GameSeating.begin(scene, PlayScene.Station.SEAT, from, 0)!!
        for (species in AvatarSpecies.entries) {
            val actor = GameResidents.Actor(resident.copy(species = species), from, seat = seat)
            val pose = actor.seatFrame(scene, 420)!!
            val box = actor.hitBox(scene, 420)
            val base = GameCharacterScale.hitBox(scene, pose.pos, species)
            assertEquals(pose.pos, actor.renderPos(scene, 420))
            assertEquals(base.y0 - pose.lift, box.y0, .001f)
            assertEquals(base.y1 - pose.lift, box.y1, .001f)
            assertTrue(((box.x0 + box.x1) / 2f to (box.y0 + box.y1) / 2f) in box)
        }
    }
    @Test fun `Es wird nie eine besetzte Sitzflaeche doppelt vergeben`() {
        val scene = GameWorld.scene(PlayScene.Place.LIVING)!!
        val from = PlayControl.Pos(.5f,.8f)
        val seat = GameSeating.begin(scene, PlayScene.Station.SEAT, from, 0)!!
        assertNull(GameSeating.begin(scene, PlayScene.Station.SEAT, from, 0, setOf(seat.piece.id)))
        assertNull(GameSeating.begin(scene, PlayScene.Station.DOOR, from, 0))
    }
    @Test fun `Stoffbefestigung Kopf und Fuesse bleiben trotz Boeen und Gang unverschoben`() {
        for (species in AvatarSpecies.entries) for (clock in listOf(0L,1000L,999999L,Long.MAX_VALUE)) {
            val pose=GameFabric.Pose(species,1.1f,clock,2f)
            assertEquals(0f,GameFabric.offset(pose,.5f,.95f),0f)
            assertEquals(0f,GameFabric.offset(pose,.9f,.1f),0f)
            for (region in GameFabric.regions(species)) {
                assertEquals(0f,GameFabric.offset(pose,region.right,(region.top+region.bottom)/2f),.00001f)
                assertTrue(kotlin.math.abs(GameFabric.offset(pose,region.left,(region.top+region.bottom)/2f))<.05f)
            }
            assertEquals(0f,GameFabric.hangingOffset(0f,.5f,1f,clock,3),0f)
            assertTrue(GameFabric.hangingOffset(.9f,.5f,1f,clock,3).isFinite())
        }
    }
    @Test fun `Umgebungsschatten drehen mit der Sonne ohne ihren Weltfuss zu verschieben`() {
        val tree=GameWorldShadows.casters.first { it.foliage }
        val scene=GameWorld.scene(PlayScene.Place.PARK)!!
        val morning=GameSceneLighting.sources(scene,420,false,false,0).first { it.directional }
        val evening=GameSceneLighting.sources(scene,1140,false,false,0).first { it.directional }
        val a=GameWorldShadows.project(tree,morning,0)
        val b=GameWorldShadows.project(tree,evening,0)
        assertEquals(a.points.first(),b.points.first())
        assertTrue(a.points[2].first > b.points[2].first)
        assertEquals(a,GameWorldShadows.project(tree,morning,0))
        val night=GameSceneLighting.sources(scene,1380,false,false,0)
        assertFalse(night.any { it.directional })
    }
    @Test fun `Bodenlichtkorrektur laesst Himmel Wasser und Moebelfronten unveraendert`() {
        assertFalse(GameGroundLight.floor("interiors/living.png",.5f,.3f))
        assertFalse(GameGroundLight.floor("interiors/living.png",.5f,.56f))
        assertFalse(GameGroundLight.floor("world/coast.png",.2f,.98f))
        assertFalse(GameGroundLight.floor("world/expedition.png",.85f,.85f))
        val field=GameGroundLight.estimate("interiors/work.png") { u,_ -> if(u < .40f) 0xFF505050.toInt() else 0xFFB0B0B0.toInt() }
        assertTrue(field.gain(.3f,.90f)>1f)
        assertEquals(1f,field.gain(.5f,.3f),.0001f)
        val rgb=GameGroundLight.apply(0x70502814,1.2f)
        assertEquals(0x70,rgb ushr 24)
        assertEquals(2,(rgb shr 8 and 255)/(rgb and 255))
    }

}
