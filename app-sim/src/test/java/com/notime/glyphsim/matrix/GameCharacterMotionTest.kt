package com.notime.glyphsim.matrix

import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Test

class GameCharacterMotionTest {
    @Test fun `Stoff startet ohne Pixelsprung und schwingt nach dem Anhalten aus`() {
        val follow = GameCharacterMotion.Follow()
        val rest = follow.update(0, 0f, 0f)
        val first = follow.update(16, 2f, 0f)
        assertTrue(first.drag < rest.drag)
        assertTrue(first.drag > -1.4f)
        for (t in 32L..1000L step 16) follow.update(t, 2f, 0f)
        val stopped = follow.update(1016, 0f, 0f)
        assertTrue(stopped.drag < -.7f)
        var settled = stopped
        for (t in 1032L..4000L step 16) settled = follow.update(t, 0f, 0f)
        assertTrue(abs(settled.drag) < .0001f)
        assertTrue(abs(settled.sway) < .0001f)
    }

    @Test fun `Nachlauf hat dieselbe Loesung bei verschiedenen Bildraten`() {
        fun result(step: Long): GameCharacterMotion.Response {
            val follow = GameCharacterMotion.Follow()
            follow.update(0, 0f, 0f)
            var response = GameCharacterMotion.Response()
            for (t in step..1000L step step) response = follow.update(t, 1.5f, .4f)
            return response
        }
        val a = result(10); val b = result(20); val c = result(40)
        assertEquals(a.drag, b.drag, .00001f); assertEquals(a.sway, b.sway, .00001f)
        assertEquals(a.drag, c.drag, .00001f); assertEquals(a.sway, c.sway, .00001f)
    }

    @Test fun `Pause Zeitwechsel und ungueltige Eingaben stauen keine Bewegung auf`() {
        val follow = GameCharacterMotion.Follow()
        follow.update(1000, 2f, 1f)
        assertEquals(GameCharacterMotion.Response(), follow.update(10_000, 0f, 0f))
        assertEquals(GameCharacterMotion.Response(), follow.update(5, Float.NaN, Float.POSITIVE_INFINITY))
        assertTrue(follow.update(Long.MAX_VALUE, 2f, 1f).drag.isFinite())
    }

    @Test fun `Sekundaerbewegung laesst Kopf Kragen und ruhende Fussanker fest`() {
        for (species in AvatarSpecies.entries) for (frame in listOf(0, 9, 68, 76, 93, 103, 110, 114)) {
            val top = GameCharacterScale.reference(species).top / 128f
            for (u in listOf(.25f, .5f, .75f)) for (v in listOf(top, top + .05f, 120f/128f, 125f/128f, 1f)) {
                val p = GameCharacterMotion.point(species, frame, top, u, v, 1234, 123, 0f, 1.5f,
                    GameCharacterMotion.Response(-1.4f, -1.2f))
                assertEquals(u, p.x, .000001f); assertEquals(v, p.y, .000001f)
            }
        }
    }

    @Test fun `Gehen behaelt Bodenkontakt Rennen enthaelt Abdruck und Flug`() {
        for (t in 0L..760L step 10L) {
            val walk=GameCharacterMotion.stride(t,false)
            assertEquals(0f,walk.flight,0f)
            assertTrue(walk.leftSwing==0f || walk.rightSwing==0f)
        }
        assertTrue((0L..760L step 10L).any { GameCharacterMotion.stride(it,true).flight > .8f })
        assertTrue((0L..760L step 10L).any { GameCharacterMotion.stride(it,true).compression > .8f })
        for (species in AvatarSpecies.entries) {
            val top=GameCharacterScale.reference(species).top/128f
            val walk=GameCharacterMotion.point(species,68,top,.35f,125f/128f,0,120,1f,0f,GameCharacterMotion.Response())
            assertEquals(125f/128f,walk.y,.00001f)
            val run=GameCharacterMotion.point(species,122,top,.35f,125f/128f,0,290,2f,0f,GameCharacterMotion.Response())
            assertTrue(run.y < walk.y-.005f)
        }
    }

    @Test fun `Stern hebt die freie Spitze an und belastet die andere`() {
        val top=44f/128
        fun foot(u: Float,t: Long)=GameCharacterMotion.point(AvatarSpecies.STARLET,68,top,u,
            125f/128f,0,t,1f,0f,GameCharacterMotion.Response())
        assertEquals(125f/128f,foot(.35f,190).y,.00001f)
        assertTrue(foot(.67f,190).y<125f/128f-.008f)
        assertTrue(foot(.35f,570).y<125f/128f-.008f)
        assertEquals(125f/128f,foot(.67f,570).y,.00001f)
    }

    @Test fun `Gezeichnete Schwimmphasen erhalten den Rumpf ohne Volumenpuls`() {
        for(species in AvatarSpecies.entries) {
            val top=GameCharacterScale.reference(species).top/128f
            val v=top+(125f/128-top)*.63f
            fun point(stroke: Float)=GameCharacterMotion.point(species,104,top,.34f,v,0,0,0f,0f,
                GameCharacterMotion.Response(),GameWater.Swim(stroke,0f,9f,true))
            assertEquals(point(.25f),point(.75f))
            assertEquals(.34f,point(.25f).x,0f)
            assertEquals(v,point(.25f).y,0f)
            assertEquals(5,GameWater.swimFrame(.25f,PlayControl.Dir.DOWN))
            assertEquals(7,GameWater.swimFrame(.75f,PlayControl.Dir.DOWN))
            assertEquals(104,CreatureSprites.motionFrame(CreatureSprites.MotionCue(CreatureSprites.Motion.SWIM,.5f),
                CreatureSprites.Facing.FRONT))
        }
    }

    @Test fun `Schlaf und Rolle erhalten ihre eigene kompakte Anatomie`() {
        for (species in AvatarSpecies.entries) for (frame in listOf(23, 24, 25, 26, 99, 130, 137)) {
            val vertices = FloatArray(GameCharacterMotion.VERTICES*2)
            GameCharacterMotion.fill(vertices, species, frame, .4f, 2345, 678, 2f, 1.5f,
                GameCharacterMotion.Response(-1.4f, -1.2f))
            for (row in 0..GameCharacterMotion.ROWS) for (column in 0..GameCharacterMotion.COLUMNS) {
                val i = (row*(GameCharacterMotion.COLUMNS+1)+column)*2
                assertEquals(column.toFloat()/GameCharacterMotion.COLUMNS, vertices[i], 0f)
                assertEquals(row.toFloat()/GameCharacterMotion.ROWS, vertices[i+1], 0f)
            }
        }
    }

    @Test fun `Puffling bekommt eine Taille waehrend Gloop und Stern ihre Form behalten`() {
        for (species in listOf(AvatarSpecies.PUFFLING, AvatarSpecies.GLOOP, AvatarSpecies.STARLET)) {
            val top = GameCharacterScale.reference(species).top / 128f
            val v = top + (125f/128f-top)*.78f
            val p = GameCharacterMotion.point(species, 104, top, .75f, v, 0, 0, 0f, 0f,
                GameCharacterMotion.Response())
            if (species == AvatarSpecies.PUFFLING) assertTrue(p.x < .73f)
            else assertEquals(.75f, p.x, .002f)
        }
    }

    @Test fun `Mantelbewegung bleibt auch unter einem Quellpixel erhalten`() {
        val follow = GameCharacterMotion.Follow()
        val a = follow.update(0, 0f, 0f)
        val b = follow.update(16, .1f, .1f)
        val first = GameCharacterMotion.point(AvatarSpecies.FENNEC, 95, 17f/128, .35f, .70f,
            0, 0, 0f, 0f, a)
        val next = GameCharacterMotion.point(AvatarSpecies.FENNEC, 95, 17f/128, .35f, .70f,
            16, 0, .1f, .1f, b)
        assertTrue(abs(first.x-next.x) > .000001f)
        assertTrue(abs(first.x-next.x)*128 < 1f)
    }

    @Test fun `Wind blaest beide Frontsaeume zur selben Bildseite`() {
        val species = AvatarSpecies.FENNEC
        val top = 17f/128
        val v = top + (125f/128-top)*.72f
        for (u in listOf(.3f, .7f)) {
            val a = GameCharacterMotion.point(species,104,top,u,v,0,0,0f,0f,
                GameCharacterMotion.Response(breeze=-.65f,breezeSway=-.65f))
            val b = GameCharacterMotion.point(species,104,top,u,v,0,0,0f,0f,
                GameCharacterMotion.Response(breeze=.65f,breezeSway=.65f))
            assertTrue(b.x > a.x)
        }
    }

    @Test fun `Das Netz faltet sich bei Boeen und schnellem Gang nicht um`() {
        val vertices = FloatArray(GameCharacterMotion.VERTICES*2)
        for (species in AvatarSpecies.entries) for (frame in listOf(0, 9, 68, 76, 93, 103, 110, 114, 122, 126, 95, 104, 111, 130))
            for (clock in listOf(0L, 431L, 1423L, Long.MAX_VALUE)) for (wind in listOf(-1.5f, 1.5f))
                for(swim in listOf(null,GameWater.Swim(.25f,0f,9f,true),GameWater.Swim(.75f,0f,9f,true))) {
                GameCharacterMotion.fill(vertices, species, frame, GameCharacterScale.reference(species).top/128f,
                    clock, clock, 2f, wind, GameCharacterMotion.Response(-2.375f, -.8f, wind*.65f, -wind*.3f),swim)
                for (row in 0 until GameCharacterMotion.ROWS) for (column in 0 until GameCharacterMotion.COLUMNS) {
                    val a = (row*(GameCharacterMotion.COLUMNS+1)+column)*2
                    val b = a+2; val c = a+(GameCharacterMotion.COLUMNS+1)*2; val d = c+2
                    fun area(i: Int, j: Int, k: Int) =
                        (vertices[j]-vertices[i])*(vertices[k+1]-vertices[i+1]) -
                            (vertices[j+1]-vertices[i+1])*(vertices[k]-vertices[i])
                    assertTrue("$species/$frame/$clock/$row/$column", area(a,b,c) > 0f)
                    assertTrue("$species/$frame/$clock/$row/$column", area(b,d,c) > 0f)
                }
            }
    }
}
