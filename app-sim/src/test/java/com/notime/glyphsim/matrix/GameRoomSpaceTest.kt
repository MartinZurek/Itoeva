package com.notime.glyphsim.matrix

import org.junit.Assert.*
import org.junit.Test

class GameRoomSpaceTest {
    @Test fun `Alle Innenraeume haben begehbare Tiefe statt eines schmalen Bildstreifens`() {
        for(scene in GameInteriorCatalog.scenes.values) {
            assertTrue(GameRoomSpace.enabled(scene))
            assertTrue(scene.nearY-scene.farY>=130f)
            for(species in AvatarSpecies.entries) {
                assertTrue(GameCharacterScale.visibleHeight(scene,PlayControl.Pos(.5f,.5f),species)<100f)
            }
            assertTrue(GameRoomSpace.bodies(scene).size>=3)
        }
    }

    @Test fun `Jedes sichtbare Moebel hat dieselbe Kollisions und Landeflaeche`() {
        for(scene in GameInteriorCatalog.scenes.values) {
            for(body in GameRoomSpace.bodies(scene)) {
                val surface=GameSurfaces.painted(scene).single { it.id==body.piece.id }
                val center=PlayControl.Pos((surface.x0+surface.x1)/2f,surface.anchorDepth)
                assertEquals(body.piece.top,GameScenes.feet(scene,center).second-surface.heightAt(center),.001f)
                assertEquals(body.faces.map { it.points },GameRoomSpace.contours(body.piece))
                assertTrue(body.faces.all { it.points.size>=4 && it.points.all { p -> p.first.isFinite() && p.second.isFinite() } })
            }
        }
    }

    @Test fun `Hinter dem Sofa ist ein echter freier Weg mit richtiger Verdeckung`() {
        val scene=GameWorld.scene(PlayScene.Place.LIVING)!!
        val sofa=GameFurniture.pieces(scene).single { it.id=="living-sofa" }
        val behind=GameScenes.posAt(scene,245f,130f)
        assertFalse(GameSurfaces.painted(scene).any { it.contains(behind) })
        assertTrue(sofa.hides(GameScenes.feet(scene,behind).second,null))
        val inFront=GameScenes.posAt(scene,245f,190f)
        assertFalse(sofa.hides(GameScenes.feet(scene,inFront).second,null))
        val left=GameScenes.posAt(scene,150f,130f)
        assertNotEquals(left,GameSurfaces.approach(scene,left,behind,50L))
        assertFalse(sofa.hides(130f,sofa.id))
    }

    @Test fun `Tischbeine verdecken nicht den freien Zwischenraum`() {
        val scene=GameWorld.scene(PlayScene.Place.CAFE)!!
        val table=GameRoomSpace.bodies(scene).single { it.piece.id=="cafe-table" }
        val gapX=(table.piece.left+table.piece.right)/2f
        val gapY=table.piece.ground-8f
        fun inside(points: List<Pair<Float,Float>>): Boolean {
            var hit=false
            var last=points.last()
            for(p in points) {
                if((p.second>gapY)!=(last.second>gapY) && gapX<
                    (last.first-p.first)*(gapY-p.second)/(last.second-p.second)+p.first) hit=!hit
                last=p
            }
            return hit
        }
        assertFalse(table.faces.any { inside(it.points) })
    }

    @Test fun `Auf der Tischplatte folgen Fuesse der Raumtiefe statt einer festen Bildzeile`() {
        val scene=GameWorld.scene(PlayScene.Place.CAFE)!!
        val body=GameRoomSpace.bodies(scene).single { it.piece.id=="cafe-table" }
        val piece=body.piece
        val surface=piece.surface(scene)
        val rear=GameScenes.posAt(scene,(piece.left+piece.right)/2f,piece.back)
        val front=GameScenes.posAt(scene,(piece.left+piece.right)/2f,piece.ground)
        val rearTop=GameScenes.feet(scene,rear).second-surface.heightAt(rear)
        val frontTop=GameScenes.feet(scene,front).second-surface.heightAt(front)
        assertEquals(piece.top,frontTop,.001f)
        assertEquals(piece.ground-piece.back,frontTop-rearTop,.001f)
        assertTrue(body.faces.any { face -> face.shade==1 && face.points.any { kotlin.math.abs(it.second-rearTop)<.001f } })
        val supported=GameMovement.State(pos=rear,height=surface.heightAt(rear),support=surface.id)
        val next=GameMovement.tick(supported,PlayControl.Stick(0f,1f),50L,listOf(surface)).state
        assertEquals(supported.height,next.height,.001f)
        assertTrue(GameScenes.feet(scene,next.pos).second-next.height>rearTop)
    }
}
