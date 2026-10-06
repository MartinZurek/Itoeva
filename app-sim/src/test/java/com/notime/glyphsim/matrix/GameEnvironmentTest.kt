package com.notime.glyphsim.matrix

import org.junit.Assert.*
import org.junit.Test

class GameEnvironmentTest {
    private val scene = GameScenes.of(PlayScene.Place.BEACH)!!
    private fun grid(m: GameEnvironment.Material) = GameEnvironment.Grid(ByteArray(480 * 270) { m.ordinal.toByte() }, m)
    private fun walk(dt: Long): GameEnvironment.State {
        var env = GameEnvironment.State(place = scene.place)
        var elapsed = 0L
        while (elapsed < 1000L) {
            val before = GameMovement.State(pos = PlayControl.Pos(.2f + elapsed * .0004f, .5f))
            elapsed += dt
            val after = before.copy(pos = PlayControl.Pos(.2f + elapsed * .0004f, .5f))
            env = GameEnvironment.tick(env, scene, before, after, dt, grid(GameEnvironment.Material.SAND))
        }
        return env
    }
    @Test fun `Schrittspuren haben bei verschiedenen Bildraten denselben Abstand`() {
        val slow = walk(40L).contacts
        val fast = walk(10L).contacts
        assertEquals(slow.size, fast.size)
        slow.zip(fast).forEach { (a,b) ->
            assertEquals(a.x, b.x, .001f)
            assertEquals(a.y, b.y, .001f)
            assertEquals(a.foot, b.foot)
        }
        assertTrue(fast.size > 10)
    }
    @Test fun `Stillstand und Wanddruck erzeugen keine Spuren`() {
        val s = GameMovement.State(velocity = PlayControl.Stick(1f,0f))
        var env = GameEnvironment.State(place = scene.place)
        repeat(100) { env = GameEnvironment.tick(env, scene, s, s, 16L, grid(GameEnvironment.Material.MUD)) }
        assertTrue(env.contacts.isEmpty())
    }
    @Test fun `Flug hinterlaesst nichts und eine gepufferte Landung genau einen Kontakt`() {
        val from = PlayControl.Pos(.3f,.4f)
        val to = PlayControl.Pos(.4f,.4f)
        val arc = GameMovement.Arc(from,to,0f,0f,null)
        val before = GameMovement.State(pos = from, action = GameMovement.Action.JUMP, elapsed = 400L, arc = arc)
        val airborne = before.copy(pos = to, elapsed = 500L)
        val env = GameEnvironment.State(place = scene.place)
        assertTrue(GameEnvironment.tick(env,scene,before,airborne,16L,grid(GameEnvironment.Material.WATER)).contacts.isEmpty())
        val landed = airborne.copy(elapsed = 0L, arc = arc.copy(from = to))
        val result = GameEnvironment.tick(env,scene,airborne,landed,16L,grid(GameEnvironment.Material.WATER))
        assertEquals(1,result.contacts.size)
        assertTrue(result.contacts.single().impact > 0f)
        assertEquals(GameScenes.feet(scene,to).first,result.contacts.single().x,.001f)
    }
    @Test fun `Eine Holzplattform spritzt nicht durch das Wasser darunter`() {
        val before = GameMovement.State(pos = PlayControl.Pos(.2f,.4f), support = "dock", height = 20f)
        val after = before.copy(pos = PlayControl.Pos(.23f,.4f))
        val result = GameEnvironment.tick(GameEnvironment.State(place = scene.place),scene,before,after,16L,grid(GameEnvironment.Material.WATER))
        assertTrue(result.contacts.isNotEmpty())
        assertTrue(result.contacts.all { it.material == GameEnvironment.Material.WOOD })
        assertEquals(GameScenes.feet(scene,after.pos).second - 20f,result.contacts.last().y,GameScenes.avatarHeight(scene,after.pos) * .035f + .001f)
    }
    @Test fun `Ortswechsel und Versetzen zeichnen keine Verbindung ueber das Bild`() {
        val before = GameMovement.State(pos = PlayControl.Pos(0f,.4f))
        val after = before.copy(pos = PlayControl.Pos(1f,.4f))
        val env = GameEnvironment.State(place = scene.place)
        assertTrue(GameEnvironment.tick(env,scene,before,after,16L,grid(GameEnvironment.Material.MUD)).contacts.isEmpty())
        assertTrue(GameEnvironment.tick(env.copy(place = PlayScene.Place.PARK),scene,before,after,16L,null).contacts.isEmpty())
    }
    @Test fun `Spuren bleiben bei Rueckkehr kurz erhalten und laufen begrenzt aus`() {
        val walked = walk(10L)
        val reset = GameEnvironment.resetSampling(walked)
        assertEquals(walked.contacts,reset.contacts)
        var old = reset
        repeat(1201) { old = GameEnvironment.advance(old,50L) }
        assertTrue(old.contacts.isEmpty())
        var many = GameEnvironment.State(place = scene.place)
        repeat(500) { i ->
            val a = GameMovement.State(pos = PlayControl.Pos(if (i % 2 == 0) .2f else .24f,.4f))
            val b = a.copy(pos = PlayControl.Pos(if (i % 2 == 0) .24f else .2f,.4f))
            many = GameEnvironment.tick(many,scene,a,b,16L,grid(GameEnvironment.Material.MUD))
        }
        assertEquals(GameEnvironment.MAX_CONTACTS,many.contacts.size)
    }
    @Test fun `Neue Moebel sind mit der bestehenden Sprunghilfe erreichbar`() {
        GameScenes.painted.forEach { place ->
            val scene = GameScenes.of(place)!!
            val surfaces = GameSurfaces.painted(scene)
            surfaces.filter { it.id !in setOf("park-crate", "living-tea-table") }.forEach { target ->
                var state = GameMovement.State(pos = PlayControl.Pos((target.x0 + target.x1) / 2f,
                    (target.d1 + .08f).coerceAtMost(1f)), facing = PlayControl.Dir.UP)
                state = GameMovement.command(state,GameMovement.Command.JUMP,PlayControl.Stick(0f,-1f),surfaces)
                assertEquals(target.id,state.arc!!.surface)
                repeat(14) { state = GameMovement.tick(state,PlayControl.Stick(),50L,surfaces).state }
                assertEquals(target.id,state.support)
                assertTrue(target.contains(state.pos))
                assertEquals(target.heightAt(state.pos),state.height,.001f)
            }
        }
    }
    @Test fun `Materialraster und Lebensdauer unterscheiden Sand Matsch und Wasser`() {
        assertEquals(GameEnvironment.Material.WATER,grid(GameEnvironment.Material.WATER).at(-20f,300f))
        assertTrue(GameEnvironment.lifetime(GameEnvironment.Material.SAND) > GameEnvironment.lifetime(GameEnvironment.Material.WATER))
        assertTrue(GameEnvironment.lifetime(GameEnvironment.Material.MUD) > 30_000L)
    }
    @Test fun `Alle gemalten Orte haben bewegliche Ebenen und kleine gepackte Ausschnitte`() {
        assertEquals(GameScenes.painted,GameRoomCatalog.rooms.keys)
        GameRoomCatalog.rooms.values.forEach { room ->
            assertTrue(room.parts.isNotEmpty())
            assertTrue(room.parts.all { it.w in 1..480 && it.h in 1..270 && it.sx + it.w <= 1024 && it.sy + it.h <= 2048 })
            assertTrue(room.parts.any { GameEnvironment.pose(it,0L) != GameEnvironment.pose(it,1500L) })
        }
    }
    @Test fun `Automatisches Hinlaufen bleibt ausserhalb der Moebelkoerper`() {
        GameScenes.painted.forEach { place ->
            val room = GameScenes.of(place)!!
            val surfaces = GameSurfaces.painted(room)
            room.spots.forEach { spot ->
                val target = GameScenes.posAt(room,spot.standX,spot.standY)
                assertFalse("Standort ${place}/${spot.station}",surfaces.any { it.contains(target) })
                var pos = PlayControl.Pos(.02f,.95f)
                repeat(1600) {
                    val next = GameSurfaces.approach(room,pos,target,16L)
                    assertFalse("Weg ${place}/${spot.station}",surfaces.any { it.contains(next) })
                    pos = next
                }
                assertEquals("Ziel ${place}/${spot.station}",target.x,pos.x,.0001f)
                assertEquals("Ziel ${place}/${spot.station}",target.depth,pos.depth,.0001f)
            }
        }
    }
    @Test fun `Moebel halten die Fuesse bei jeder Tiefe auf derselben gemalten Oberkante`() {
        val tops = mapOf("living-sofa" to 185f,"bedroom-bed" to 189f,"park-bench" to 166f,
            "forest-log" to 195f,"camp-log" to 202f,"beach-chair" to 183f,"swamp-dock" to 200f)
        GameScenes.painted.forEach { place ->
            val room = GameScenes.of(place)!!
            GameSurfaces.painted(room).filter { it.id in tops }.forEach { surface ->
                listOf(surface.d0,surface.d1).forEach { d ->
                    val pos = PlayControl.Pos((surface.x0 + surface.x1) / 2,d)
                    assertEquals(tops[surface.id]!!,GameScenes.feet(room,pos).second - surface.heightAt(pos),.001f)
                    assertTrue(surface.heightAt(pos) in 0f..52f)
                }
            }
        }
        assertFalse(GameSurfaces.painted(GameScenes.of(PlayScene.Place.LIVING)!!).any { it.id.contains("door") || it.id.contains("piano") })
    }
}
