package com.notime.glyphsim.matrix

import kotlin.math.sqrt
import org.junit.Assert.*
import org.junit.Test

class GameMovementTest {
    private val right = PlayControl.Stick(1f, 0f)
    private val neutral = PlayControl.Stick()
    private val box = GameMovement.Surface("box", 0.5f, 0.65f, 0.3f, 0.6f, 20f)
    private fun run(start: GameMovement.State, input: PlayControl.Stick, ms: Int,
        surfaces: List<GameMovement.Surface> = emptyList(), dt: Long = 10L): GameMovement.State {
        var state = start
        repeat(ms / dt.toInt()) { state = GameMovement.tick(state, input, dt, surfaces).state }
        return state
    }

    @Test fun `Totzone Rand und Diagonale sind radial begrenzt`() {
        assertEquals(neutral, PlayControl.stick(3f, 4f, 56f))
        val diagonal = PlayControl.stick(100f, 100f, 56f)
        assertEquals(1f, diagonal.strength, 0.0001f)
        assertEquals(PlayControl.Stick(), PlayControl.stick(Float.NaN, 2f, 56f))
        assertEquals(0.55f, PlayControl.stick(34.328f, 0f, 56f).strength, 0.0001f)
    }
    @Test fun `Tempo steigt stetig bis auf das Zweieinhalbfache`() {
        var last = -1f
        for (i in 0..100) {
            val speed = PlayControl.tempo(i / 100f)
            assertTrue(speed >= last)
            last = speed
        }
        assertEquals(1f, PlayControl.tempo(0.55f), 0.0001f)
        assertEquals(2.5f, PlayControl.tempo(1f), 0.0001f)
        val start = PlayControl.Pos(0.4f, 0.4f)
        val straight = PlayControl.step(start, right, 100L).pos
        val diagonal = PlayControl.step(start, PlayControl.stick(1f, 1f, 1f, 0f), 100L).pos
        fun length(p: PlayControl.Pos): Float {
            val x = (p.x - start.x) / PlayControl.SPEED_X
            val y = (p.depth - start.depth) / PlayControl.SPEED_DEPTH
            return sqrt(x*x+y*y)
        }
        assertEquals(length(straight), length(diagonal), 0.0001f)
    }
    @Test fun `Loslassen bremst rasch und der Gang bleibt danach stehen`() {
        val moving = run(GameMovement.State(), right, 250)
        val stopped = run(moving, neutral, 100)
        assertFalse(stopped.moving)
        assertEquals(stopped.pos, run(stopped, neutral, 200).pos)
        assertEquals(stopped.gaitMs, run(stopped, neutral, 200).gaitMs, 0.0001)
    }
    @Test fun `verschiedene Bildraten ergeben dieselbe Gehstrecke`() {
        val slow = PlayControl.Stick(0.55f, 0f)
        val initial = GameMovement.State(pos = PlayControl.Pos(0.1f, 0.5f), velocity = slow)
        assertEquals(run(initial, slow, 1000, dt = 10).pos.x,
            run(initial, slow, 1000, dt = 20).pos.x, 0.0001f)
    }
    @Test fun `Richtung bleibt nahe der Diagonale ruhig`() {
        assertEquals(PlayControl.Dir.RIGHT, PlayControl.Stick(0.5f, 0.53f).direction(PlayControl.Dir.RIGHT))
        assertEquals(PlayControl.Dir.DOWN, PlayControl.Stick(0.4f, 0.8f).direction(PlayControl.Dir.RIGHT))
    }
    @Test fun `erreichbare Kiste bekommt einen Bogen und dauerhaften Bodenkontakt`() {
        val initial = GameMovement.State(pos = PlayControl.Pos(0.43f, 0.45f), facing = PlayControl.Dir.RIGHT)
        val jump = GameMovement.command(initial, GameMovement.Command.JUMP, right, listOf(box))
        val apex = run(jump, neutral, 320, listOf(box))
        assertTrue(apex.height > box.height)
        assertEquals(GameMovement.Action.JUMP, apex.action)
        val landed = run(jump, neutral, 660, listOf(box))
        assertNull(landed.action)
        assertEquals("box", landed.support)
        assertEquals(20f, landed.height, 0.0001f)
        assertTrue(box.contains(landed.pos))
        assertEquals(landed.pos, run(landed, neutral, 200, listOf(box)).pos)
    }
    @Test fun `zu hohe entfernte oder hinter dem Ruecken liegende Dinge ziehen nicht an`() {
        val initial = GameMovement.State(pos = PlayControl.Pos(0.43f, 0.45f))
        assertNull(GameMovement.jumpTarget(initial, right, listOf(box.copy(height = 60f))))
        assertNull(GameMovement.jumpTarget(initial.copy(pos = PlayControl.Pos(0.1f, 0.1f)), right, listOf(box)))
        assertNull(GameMovement.jumpTarget(initial, PlayControl.Stick(-1f, 0f), listOf(box)))
        val tall = box.copy(height = 60f)
        val jump = GameMovement.command(initial, GameMovement.Command.JUMP, right, listOf(tall))
        assertFalse(tall.contains(jump.arc!!.to))
        assertNull(run(jump, neutral, 660, listOf(tall)).support)
    }
    @Test fun `ein freier Sprung landet wieder auf dem Boden und wechselt keinen Ort`() {
        val initial = GameMovement.State(pos = PlayControl.Pos(0.98f, 0.5f))
        val hop = GameMovement.command(initial, GameMovement.Command.JUMP, neutral, emptyList())
        assertEquals(initial.pos, hop.arc!!.to)
        val jump = GameMovement.command(initial, GameMovement.Command.JUMP, right, emptyList())
        var state = jump
        repeat(80) {
            val result = GameMovement.tick(state, right, 10)
            if (state.action == GameMovement.Action.JUMP) assertNull(result.exit)
            state = result.state
        }
        assertEquals(0f, state.height, 0f)
        assertTrue(state.pos.x in 0f..1f)
    }
    @Test fun `Gehen und Rollen gehen am Boden nicht durch die Kiste`() {
        val start = GameMovement.State(pos = PlayControl.Pos(0.43f, 0.45f), facing = PlayControl.Dir.RIGHT)
        val walk = run(start, right, 500, listOf(box))
        assertTrue(walk.pos.x < box.x0)
        val roll = GameMovement.command(start, GameMovement.Command.ROLL, right, listOf(box))
        assertTrue(run(roll, neutral, 500, listOf(box)).pos.x < box.x0)
    }
    @Test fun `eine Kante fuehrt sanft zurueck auf den Boden`() {
        val start = GameMovement.State(pos = PlayControl.Pos(0.64f, 0.45f), velocity = right,
            height = 20f, support = "box")
        val falling = GameMovement.tick(start, right, 30, listOf(box)).state
        assertEquals(GameMovement.Action.JUMP, falling.action)
        val landed = run(falling, neutral, 350, listOf(box))
        assertEquals(0f, landed.height, 0f)
        assertNull(landed.support)
    }
    @Test fun `Huepfen auf der Kiste landet nicht in ihrem Inneren`() {
        val standing = GameMovement.State(pos = PlayControl.Pos(0.52f, 0.4f),
            height = 20f, support = "box", facing = PlayControl.Dir.RIGHT)
        val jump = GameMovement.command(standing, GameMovement.Command.JUMP, PlayControl.Stick(0.3f, 0f), listOf(box))
        val landed = run(jump, neutral, 660, listOf(box))
        assertEquals("box", landed.support)
        assertEquals(20f, landed.height, 0f)
    }
    @Test fun `Tiefe auf einer Platte veraendert nicht ihre sichtbare Oberkante`() {
        val sloped = box.copy(depthSlope = 30f, anchorDepth = 0.45f)
        val start = GameMovement.State(pos = PlayControl.Pos(0.55f, 0.45f), height = 20f, support = "box")
        val next = run(start, PlayControl.Stick(0f, 0.2f), 100, listOf(sloped))
        assertEquals(sloped.heightAt(next.pos), next.height, 0.0001f)
        val fast = start.copy(velocity = PlayControl.Stick(0f, 1f))
        val forward = GameMovement.tick(fast, PlayControl.Stick(0f, 1f), 16, listOf(sloped)).state
        assertTrue(forward.pos.depth > fast.pos.depth)
        assertEquals(sloped.heightAt(forward.pos), forward.height, 0.0001f)
    }
    @Test fun `eine Kante hebt die Figur nicht erst nach oben`() {
        val start = GameMovement.State(pos = PlayControl.Pos(0.64f, 0.45f), velocity = right,
            height = 20f, support = "box")
        val falling = GameMovement.tick(start, right, 30, listOf(box)).state
        var next = falling
        repeat(33) {
            val after = GameMovement.tick(next, neutral, 10, listOf(box)).state
            assertTrue(after.height <= next.height)
            next = after
        }
        assertEquals(0f, next.height, 0f)
    }
    @Test fun `Gangart hat Hysterese und eine kurze stetige Ueberblendung`() {
        val start = GameMovement.State(pos = PlayControl.Pos(0.1f, 0.5f))
        val fast = run(start, right, 180)
        assertTrue(fast.running)
        val middle = run(fast, PlayControl.Stick(0.76f, 0f), 80)
        assertTrue(middle.running)
        val walking = run(middle, PlayControl.Stick(0.55f, 0f), 200)
        assertFalse(walking.running)
        assertEquals(0f, run(walking, PlayControl.Stick(0.55f, 0f), 200).runBlend, 0f)
        val first = GameMovement.tick(start.copy(velocity = right), right, 16).state
        assertTrue(first.runBlend > 0f && first.runBlend < 1f)
    }
    @Test fun `Ruhe bleibt erhalten und ein neuer Zug steht zuerst auf`() {
        val sit = GameMovement.command(GameMovement.State(), GameMovement.Command.REST, neutral, emptyList())
        val rest = run(sit, neutral, 400)
        assertEquals(GameMovement.Action.REST, rest.action)
        assertEquals(GameMovement.Action.REST, run(rest, neutral, 2000).action)
        val rising = GameMovement.tick(rest, right, 16).state
        assertEquals(GameMovement.Action.RISE, rising.action)
        assertEquals(rest.pos, rising.pos)
        assertTrue(run(rising, right, 500).moving)
    }
    @Test fun `fruehe Sprungwiederholung ist gesperrt spaeter Tipp wird gepuffert`() {
        val jump = GameMovement.command(GameMovement.State(), GameMovement.Command.JUMP, neutral, emptyList())
        assertEquals(jump, GameMovement.command(jump, GameMovement.Command.JUMP, neutral, emptyList()))
        val landing = run(jump, neutral, 580)
        val queued = GameMovement.command(landing, GameMovement.Command.JUMP, neutral, emptyList())
        assertEquals(GameMovement.Action.JUMP, run(queued, neutral, 80).action)
        assertTrue(run(queued, neutral, 80).elapsed < 100L)
    }
    @Test fun `die Parkkiste und Kuechentische haben wirkliche erreichbare Flaechen`() {
        val scene = GameScenes.of(PlayScene.Place.PARK)!!
        val crate = GameSurfaces.crate(scene)!!
        val feet = GameScenes.feet(scene, PlayControl.Pos(0.72f, 0.75f))
        assertEquals(crate.top, feet.second - crate.surface.heightAt(PlayControl.Pos(0.72f, 0.75f)), 0.0001f)
        val living = GameScenes.of(PlayScene.Place.LIVING)!!
        val table = GameSurfaces.painted(living).single()
        val middle = table.closest(GameScenes.posAt(living, 176f, 212f))
        assertEquals(184f, GameScenes.feet(living, middle).second - table.heightAt(middle), 0.0001f)
        val tables = GameSurfaces.tables(PlayScene.Place.KITCHEN, AvatarSpecies.FENNEC,
            60, 40, 600f, 128f, 10f, 100f)
        assertTrue(tables.isNotEmpty())
        assertTrue(tables.all { it.height > 0f && it.height < 52f && it.x0 < it.x1 })
    }
}
