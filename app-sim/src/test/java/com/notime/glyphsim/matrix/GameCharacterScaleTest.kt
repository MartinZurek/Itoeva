package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayControl.Pos
import com.notime.glyphsim.matrix.PlayScene.Place
import org.junit.Assert.*
import org.junit.Test

class GameCharacterScaleTest {
    @Test fun `Fennec ueberragt die Bank statt mit Ohren kleiner als die Lehne zu sein`() {
        val scene = GameWorld.scene(Place.PARK)!!
        val bench = scene.spots.single { it.station == PlayScene.Station.BENCH }
        val pos = GameScenes.posAt(scene, bench.standX, bench.standY)
        val height = GameCharacterScale.visibleHeight(scene, pos, AvatarSpecies.FENNEC)
        assertTrue(height in 105f..115f)
        assertTrue(height > bench.hit.y1 - bench.hit.y0)
        val old = GameScenes.avatarHeight(scene, pos) / .8f * .82f * 109f / 128f
        assertTrue(height / old in 1.45f..1.52f)
    }

    @Test fun `alle Wesen haben feste Standmasse und bleiben in ihrer Groessenfamilie`() {
        val scene = GameWorld.scene(Place.PARK)!!; val pos = Pos(.5f,.5f)
        val fennec = GameCharacterScale.visibleHeight(scene, pos, AvatarSpecies.FENNEC)
        for (species in AvatarSpecies.entries) {
            val ref = GameCharacterScale.reference(species)
            val layout = GameCharacterScale.layoutWidth(scene, pos, species)
            val painted = layout * CreatureSprites.Rich.scaleFor(species) * (126f - ref.top) / 128f
            assertEquals(GameCharacterScale.visibleHeight(scene, pos, species), painted, .001f)
            assertTrue(painted / fennec in .85f..1.20f)
        }
    }

    @Test fun `jede Figur behaelt beim Ueberqueren aller Aussenraender ihre sichtbare Hoehe`() {
        for ((from, to) in GameWorld.places.zipWithNext()) for (species in AvatarSpecies.entries)
            for (depth in listOf(0f,.5f,1f)) {
                assertEquals(GameCharacterScale.visibleHeight(GameWorld.scene(from)!!, Pos(1f,depth), species),
                    GameCharacterScale.visibleHeight(GameWorld.scene(to)!!, Pos(0f,depth), species), .001f)
            }
    }

    @Test fun `Zoom aendert das Verhaeltnis zum Moebel nicht und der Fuss wird nie geklemmt`() {
        val scene = GameWorld.scene(Place.PARK)!!; val pos = Pos(.5f,.5f)
        for (species in AvatarSpecies.entries) for (scale in listOf(.7f,1f,2.5f)) {
            val width = GameCharacterScale.layoutWidth(scene,pos,species) * scale
            val feet = 35f
            val top = GameCharacterScale.layoutTop(feet,width,species)
            assertTrue(top < 0f)
            assertEquals(feet, top + GameCharacterScale.localFeetY(width,species), .001f)
        }
    }

    @Test fun `Antippflaeche umfasst auch Ohren und Schweif und folgt der Tiefe`() {
        val scene = GameWorld.scene(Place.PARK)!!
        for (species in AvatarSpecies.entries) {
            val back = GameCharacterScale.hitBox(scene,Pos(.5f,0f),species)
            val front = GameCharacterScale.hitBox(scene,Pos(.5f,1f),species)
            assertTrue(front.x1-front.x0 > back.x1-back.x0)
            assertTrue(front.y1-front.y0 > back.y1-back.y0)
            val feet = GameScenes.feet(scene,Pos(.5f,1f))
            assertTrue(feet in front)
            assertTrue((feet.first to feet.second-GameCharacterScale.visibleHeight(scene,Pos(.5f,1f),species)) in front)
        }
    }

    @Test fun `Wasserlinie nutzt den Bodenmassstab und keine Sprite-Rahmenhoehe`() {
        val beach = GameWorld.scene(Place.BEACH)!!
        for (pos in listOf(Pos(.5f,.4f),Pos(.5f,1f))) {
            val rise = GameCharacterScale.waterRise(beach,pos)
            assertEquals(GameScenes.avatarHeight(beach,pos)*GameWorld.wetness(beach,pos),rise,.001f)
            for (species in AvatarSpecies.entries) assertTrue(rise < GameCharacterScale.visibleHeight(beach,pos,species))
        }
    }

    @Test fun `Plattform und Kontur teilen Oberkante und Vorderboden bei jeder Tiefe`() {
        for (place in listOf(Place.PARK,Place.LIVING,Place.BEDROOM,Place.KITCHEN,Place.CAFE)) {
            val scene = GameWorld.scene(place)!!
            for (piece in GameFurniture.pieces(scene)) {
                val surface = GameSurfaces.painted(scene).single { it.id == piece.id }
                for (d in listOf(surface.d0,surface.anchorDepth,surface.d1)) {
                    val pos = Pos((surface.x0+surface.x1)/2f,d)
                    assertEquals(piece.projectedTop(scene,pos),GameScenes.feet(scene,pos).second-surface.heightAt(pos),.001f)
                }
                assertTrue(piece.hides(piece.front-1f,null))
                assertFalse(piece.hides(piece.front,null))
                assertFalse(piece.hides(piece.front-1f,piece.id))
            }
        }
    }

    @Test fun `korrigierte Innenraumanker stehen erreichbar vor dem wirklichen Moebel`() {
        for (scene in GameInteriorCatalog.scenes.values) for (spot in scene.spots) {
            val pos = GameScenes.posAt(scene,spot.standX,spot.standY)
            assertEquals(spot.standY,GameScenes.feet(scene,pos).second,.001f)
            assertFalse(GameSurfaces.painted(scene).any { it.contains(pos) })
            assertEquals(spot.station,GameScenes.spotAt(scene,(spot.hit.x0+spot.hit.x1)/2f,(spot.hit.y0+spot.hit.y1)/2f)?.station)
        }
    }

    @Test fun `beide gemalten Tische sind mit dem neuen Game-Massstab erreichbar`() {
        for (place in listOf(Place.KITCHEN,Place.CAFE)) {
            val scene=GameWorld.scene(place)!!; val surface=GameSurfaces.painted(scene).single { it.id == "${place.name.lowercase()}-table" }
            val start=GameMovement.State(Pos((surface.x0+surface.x1)/2f,surface.d1+.015f),facing=PlayControl.Dir.UP)
            val jump=GameMovement.command(start,GameMovement.Command.JUMP,PlayControl.Stick(0f,-.5f),listOf(surface),GameCharacterScale.MAX_STEP_HEIGHT)
            assertEquals(surface.id,jump.arc!!.surface)
            var state=jump
            repeat(14) { state=GameMovement.tick(state,PlayControl.Stick(),50L,listOf(surface),maxStepHeight=GameCharacterScale.MAX_STEP_HEIGHT).state }
            assertEquals(surface.id,state.support)
            assertEquals(GameFurniture.pieces(scene).single { it.id == "${place.name.lowercase()}-table" }.projectedTop(scene,state.pos),GameScenes.feet(scene,state.pos).second-state.height,.001f)
        }
    }

    @Test fun `am Tisch liegt Fennecs Scheitel ueber der Platte und nicht unter der Tischkante`() {
        for (place in listOf(Place.KITCHEN,Place.CAFE)) {
            val scene=GameWorld.scene(place)!!; val spot=scene.spots.first { it.station==PlayScene.Station.TABLE }
            val pos=GameScenes.posAt(scene,spot.standX,spot.standY)
            val crown=spot.standY-GameScenes.avatarHeight(scene,pos)
            assertTrue(crown < GameFurniture.pieces(scene).single { it.id == "${place.name.lowercase()}-table" }.top)
        }
    }
}
