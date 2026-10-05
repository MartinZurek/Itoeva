package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.Inflater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CreatureSpritesTest {

    @Test
    fun `hohes Tempo verwendet eigene Vierbeiner in jeder Richtung und niedrigeres geht`() {
        val idle = AvatarAnimations.idlePose(AvatarSpecies.FENNEC)
        for (direction in PlayControl.Dir.entries) {
            val first = when (direction) {
                PlayControl.Dir.UP -> CreatureSprites.Rich.BACK_RUN_FIRST
                PlayControl.Dir.DOWN -> CreatureSprites.Rich.FRONT_RUN_FIRST
                else -> CreatureSprites.Rich.RUN_FIRST
            }
            val count = if (direction.dy == 0) 8 else 4
            for (phase in 0 until count) {
                val run = CreatureSprites.lookRich(idle, AvatarSpecies.FENNEC, AvatarShading.Side.NONE,
                    500L, direction, true, gaitTimeMs = phase * CreatureSprites.Rich.WALK_MS * (if (direction.dy == 0) 1 else 2), tempo = 2f)
                assertEquals(first + phase, run.frame)
                assertEquals(0, run.liftCells)
                val walk = CreatureSprites.lookRich(idle, AvatarSpecies.FENNEC, AvatarShading.Side.NONE,
                    500L, direction, true, gaitTimeMs = phase * CreatureSprites.Rich.WALK_MS, tempo = 1f)
                assertTrue(walk.frame < CreatureSprites.Rich.RUN_FIRST)
            }
        }
    }

    @Test
    fun `Rolle verwendet den ganzen gezeichneten Zyklus ohne zusaetzliches Anheben`() {
        val idle = AvatarAnimations.idlePose(AvatarSpecies.FENNEC)
        for (phase in 0 until 8) {
            val look = CreatureSprites.lookRich(idle, AvatarSpecies.FENNEC, AvatarShading.Side.NONE,
                500L, PlayControl.Dir.RIGHT, false, motionCue = CreatureSprites.MotionCue(CreatureSprites.Motion.ROLL, phase / 8f))
            assertEquals(CreatureSprites.Rich.ROLL_FIRST + phase, look.frame)
            assertEquals(0, look.liftCells)
        }
    }

    private val walkFrames = CreatureSprites.WALK_FIRST until CreatureSprites.WALK_FIRST + 4
    private val joyFrames = CreatureSprites.JOY_FIRST..CreatureSprites.JOY_FIRST + 1

    @Test
    fun `explizite Links Rechts Richtung gilt auch ohne oder gegen Schattenseite`() {
        val idle = AvatarAnimations.idlePose(AvatarSpecies.FENNEC)
        for (side in AvatarShading.Side.entries) {
            for (moving in listOf(false, true)) {
                val left = CreatureSprites.lookRich(idle, AvatarSpecies.FENNEC, side,
                    500L, PlayControl.Dir.LEFT, moving = moving)
                val right = CreatureSprites.lookRich(idle, AvatarSpecies.FENNEC, side,
                    500L, PlayControl.Dir.RIGHT, moving = moving)
                assertTrue(left.mirrored)
                assertFalse(right.mirrored)
                assertEquals(left.frame, right.frame)
            }
        }
    }

    @Test
    fun `im Stand Ruhe, ohne Anheben und ungespiegelt`() {
        for (species in AvatarSpecies.entries) {
            val look = CreatureSprites.look(AvatarAnimations.idlePose(species), species, AvatarShading.Side.NONE, 1000L)
            assertTrue(look.frame in listOf(CreatureSprites.IDLE, CreatureSprites.IDLE_BREATH))
            assertEquals(0, look.liftCells)
            assertFalse(look.mirrored)
        }
    }

    @Test
    fun `beim Gehen Laufbilder, nach links gespiegelt`() {
        for (species in AvatarSpecies.entries) {
            for (frame in AvatarAnimations.walkSequence(species).frames) {
                val rechts = CreatureSprites.look(frame, species, AvatarShading.Side.LEFT, 500L)
                assertTrue(rechts.frame in walkFrames)
                assertFalse(rechts.mirrored)
                assertTrue(CreatureSprites.look(frame, species, AvatarShading.Side.RIGHT, 500L).mirrored)
            }
        }
    }

    @Test
    fun `hoch und runter zeigen eigene Ansichten und halten beim Anhalten an`() {
        for (species in AvatarSpecies.entries) {
            val idle = AvatarAnimations.idlePose(species)
            val up = CreatureSprites.look(idle, species, AvatarShading.Side.NONE, 500L,
                PlayControl.Dir.UP, moving = false)
            val down = CreatureSprites.look(idle, species, AvatarShading.Side.NONE, 500L,
                PlayControl.Dir.DOWN, moving = false)
            assertEquals(CreatureSprites.BACK, up.frame)
            assertEquals(CreatureSprites.FRONT, down.frame)
            assertFalse(up.mirrored)
            assertTrue(CreatureSprites.look(idle, species, AvatarShading.Side.NONE, 500L,
                PlayControl.Dir.UP, moving = true).frame in CreatureSprites.BACK_WALK_FIRST..CreatureSprites.BACK_WALK_FIRST + 1)
            assertTrue(CreatureSprites.look(idle, species, AvatarShading.Side.RIGHT, 500L,
                PlayControl.Dir.LEFT, moving = false).frame in listOf(CreatureSprites.IDLE, CreatureSprites.IDLE_BREATH))
        }
    }

    @Test
    fun `ein Sprung aus einer Reaktion wird Freude und bleibt angehoben`() {
        for (species in AvatarSpecies.entries) {
            val looks = AvatarAnimations.reactionFor(species, AnimationType.MOVE).frames
                .map { CreatureSprites.look(it, species, AvatarShading.Side.NONE, 0L) }
            val joy = looks.filter { it.frame in joyFrames }
            assertTrue("$species springt nie", joy.isNotEmpty())
            assertTrue(joy.all { it.liftCells >= 2 })
        }
    }

    @Test
    fun `geschlossene Augen sind nie Laufen oder Freude`() {
        for (species in AvatarSpecies.entries) {
            for (frame in AvatarAnimations.reactionFor(species, AnimationType.SLEEP).frames) {
                if (AvatarAccent.eyesIn(frame).any { it }) continue
                val look = CreatureSprites.look(frame, species, AvatarShading.Side.NONE, 0L)
                assertTrue(look.frame == CreatureSprites.BLINK || look.frame >= CreatureSprites.SLEEP_FIRST)
            }
        }
    }

    @Test
    fun `leeres Bild faellt auf Ruhe zurueck`() {
        val look = CreatureSprites.look(IntArray(AvatarGeometry.SIZE * AvatarGeometry.HEIGHT), AvatarSpecies.GLOOP, AvatarShading.Side.NONE, 0L)
        assertEquals(CreatureSprites.IDLE, look.frame)
    }

    @Test
    fun `jeder Bogen ist da und hat alle Bilder`() {
        val root = listOf(File("src/main/assets"), File("app-sim/src/main/assets")).first { it.isDirectory }
        for (species in AvatarSpecies.entries) {
            val file = File(root, CreatureSprites.assetFor(species))
            assertTrue("$file fehlt", file.isFile)
            val img = Png.read(file.readBytes())
            // Einfacher Bogen (64er, 17 Bilder) oder feiner Bogen (96er, 39 Bilder)
            val rich = img.height == CreatureSprites.Rich.FRAME
            val size = if (rich) CreatureSprites.Rich.FRAME else CreatureSprites.FRAME
            val count = if (rich) CreatureSprites.Rich.FRAME_COUNT else CreatureSprites.FRAME_COUNT
            val feet = if (rich) CreatureSprites.Rich.FEET else CreatureSprites.FEET
            assertEquals(size * count, img.width)
            assertEquals(size, img.height)
            // Die Fuesse stehen auf der vereinbarten Zeile: darueber etwas, ab ihr nichts mehr.
            val fuesse = (0 until size).any { x -> img.alpha(x, feet - 1) > 0 }
            assertTrue("$species ohne Fuesse", fuesse)
            if (feet < size) for (x in 0 until size) assertEquals(0, img.alpha(x, feet))
        }
    }

    @Test
    fun `feiner Bogen - Ruhe, Gehen und Sprung haben eigene Schleifen`() {
        for (species in AvatarSpecies.entries) {
            val idle = CreatureSprites.lookRich(AvatarAnimations.idlePose(species), species, AvatarShading.Side.NONE, 1000L)
            assertTrue(idle.frame in CreatureSprites.Rich.IDLE_FIRST until CreatureSprites.Rich.IDLE_FIRST + CreatureSprites.Rich.IDLE_COUNT ||
                idle.frame == CreatureSprites.Rich.BLINK)
            for (frame in AvatarAnimations.walkSequence(species).frames) {
                val look = CreatureSprites.lookRich(frame, species, AvatarShading.Side.LEFT, 500L)
                assertTrue(look.frame in CreatureSprites.Rich.WALK_FIRST until CreatureSprites.Rich.WALK_FIRST + CreatureSprites.Rich.WALK_COUNT)
            }
        }
    }

    @Test
    fun `Drehung von rechts nach links laeuft vorn herum`() {
        val steps = CreatureSprites.turnSteps(CreatureSprites.Facing.RIGHT, CreatureSprites.Facing.LEFT)
        assertEquals(
            listOf(
                CreatureSprites.Step(CreatureSprites.Rich.TURN_FRONT, false),
                CreatureSprites.Step(CreatureSprites.Rich.FRONT, false),
                CreatureSprites.Step(CreatureSprites.Rich.TURN_FRONT, true)
            ),
            steps
        )
        assertEquals(listOf(CreatureSprites.Step(CreatureSprites.Rich.TURN_BACK, true)),
            CreatureSprites.turnSteps(CreatureSprites.Facing.LEFT, CreatureSprites.Facing.BACK))
        assertTrue(CreatureSprites.turnSteps(CreatureSprites.Facing.FRONT, CreatureSprites.Facing.FRONT).isEmpty())
    }

    @Test
    fun `Richtungswechsel im Stand spielt Zwischenbilder und endet in der neuen Richtung`() {
        val turn = CreatureSprites.Turn()
        val walk = AvatarAnimations.idlePose(AvatarSpecies.FENNEC)
        val rechts = CreatureSprites.lookRich(walk, AvatarSpecies.FENNEC, AvatarShading.Side.LEFT, 0L, moving = false, turn = turn)
        assertFalse(rechts.mirrored)
        val t0 = 1000L
        val seen = (0 until 6).map {
            CreatureSprites.lookRich(walk, AvatarSpecies.FENNEC, AvatarShading.Side.RIGHT,
                t0 + it * CreatureSprites.Rich.TURN_STEP_MS, moving = false, turn = turn)
        }
        assertEquals(CreatureSprites.Rich.TURN_FRONT, seen[0].frame)
        assertFalse(seen[0].mirrored)
        assertEquals(CreatureSprites.Rich.FRONT, seen[1].frame)
        assertEquals(CreatureSprites.Rich.TURN_FRONT, seen[2].frame)
        assertTrue(seen[2].mirrored)
        assertTrue(seen[3].frame in CreatureSprites.Rich.IDLE_FIRST until CreatureSprites.Rich.IDLE_FIRST + CreatureSprites.Rich.IDLE_COUNT)
        assertTrue(seen[3].mirrored)
    }

    @Test
    fun `Umkehren beim Gehen behaelt den Gang und hebt nicht beide Fuesse an`() {
        val turn = CreatureSprites.Turn()
        val raw = AvatarAnimations.walkSequence(AvatarSpecies.FENNEC).frames.first()
        CreatureSprites.lookRich(raw, AvatarSpecies.FENNEC, AvatarShading.Side.LEFT, 1000L,
            moving = true, turn = turn)
        for (i in 0..5) {
            val look = CreatureSprites.lookRich(raw, AvatarSpecies.FENNEC, AvatarShading.Side.RIGHT,
                1045L + i * 45, moving = true, turn = turn)
            assertTrue(look.frame in CreatureSprites.Rich.WALK_FIRST until CreatureSprites.Rich.WALK_FIRST + CreatureSprites.Rich.WALK_COUNT)
            assertTrue(look.mirrored)
            assertEquals(0, look.liftCells)
        }
    }

    @Test
    fun `Gang startet am Kontakt und pausierte Zeit erzeugt keinen Sprung`() {
        val gait = CreatureSprites.GaitClock()
        assertEquals(0L, gait.update(true, 5100L))
        assertEquals(45L, gait.update(true, 5145L))
        assertEquals(145L, gait.update(true, 9000L))
        assertEquals(0L, gait.update(false, 9045L))
        assertEquals(0L, gait.update(true, 12000L))
    }

    @Test
    fun `vordere Blickrichtung laesst Blinzeln und Freude durch`() {
        val idle = AvatarAnimations.idlePose(AvatarSpecies.FENNEC)
        val blink = CreatureSprites.lookRich(idle, AvatarSpecies.FENNEC, AvatarShading.Side.NONE,
            0L, PlayControl.Dir.DOWN, moving = false)
        assertEquals(CreatureSprites.Rich.FRONT_BLINK, blink.frame)
        val joy = AvatarAnimations.reactionFor(AvatarSpecies.FENNEC, AnimationType.MOVE).frames.map {
            CreatureSprites.lookRich(it, AvatarSpecies.FENNEC, AvatarShading.Side.NONE,
                500L, PlayControl.Dir.DOWN, moving = false)
        }.filter { it.frame in CreatureSprites.Rich.FRONT_JOY_FIRST until CreatureSprites.Rich.FRONT_JOY_FIRST + CreatureSprites.Rich.JOY_COUNT }
        assertTrue("Vordere Blickrichtung verschluckt die Reaktion", joy.isNotEmpty())
        assertTrue(joy.all { it.liftCells >= 2 })
    }

    @Test
    fun `vordere und hintere Ruhe bleiben animiert ohne Gehen`() {
        val idle = AvatarAnimations.idlePose(AvatarSpecies.FENNEC)
        for ((dir, first) in listOf(PlayControl.Dir.DOWN to CreatureSprites.Rich.FRONT_IDLE_FIRST,
            PlayControl.Dir.UP to CreatureSprites.Rich.BACK_IDLE_FIRST)) {
            val seen = (1..7).map { i ->
                CreatureSprites.lookRich(idle, AvatarSpecies.FENNEC, AvatarShading.Side.NONE,
                    i * CreatureSprites.Rich.IDLE_MS + 200L, dir, moving = false).frame
            }
            assertTrue(seen.all { it in first until first + CreatureSprites.Rich.IDLE_COUNT })
            assertTrue(seen.toSet().size > 1)
        }
    }

    @Test
    fun `Front und Rueckgang nutzen alle acht eigenen Bilder ohne doppelten Huepfer`() {
        val raw = AvatarAnimations.idlePose(AvatarSpecies.FENNEC)
        for ((direction, first) in listOf(
            PlayControl.Dir.DOWN to CreatureSprites.Rich.DRAWN_FRONT_WALK_FIRST,
            PlayControl.Dir.UP to CreatureSprites.Rich.DRAWN_BACK_WALK_FIRST
        )) {
            val seen = (0 until 8).map { phase ->
                val look = CreatureSprites.lookRich(raw, AvatarSpecies.FENNEC,
                    AvatarShading.Side.NONE, 500L, direction, moving = true,
                    gaitTimeMs = phase * CreatureSprites.Rich.WALK_MS)
                assertEquals(0, look.liftCells)
                assertFalse(look.mirrored)
                look.frame
            }
            assertEquals((first until first+8).toList(), seen)
        }
    }

    @Test
    fun `Strecken und Buecken bleiben am Boden auch wenn das alte Raster abhebt`() {
        val raw = AvatarAnimations.reactionFor(AvatarSpecies.FENNEC, AnimationType.MOVE).frames
            .first { CreatureSprites.liftOf(it, AvatarSpecies.FENNEC) >= 2 }
        for (motion in CreatureSprites.Motion.entries.filter { it != CreatureSprites.Motion.JUMP }) {
            for (direction in listOf(PlayControl.Dir.LEFT, PlayControl.Dir.DOWN, PlayControl.Dir.UP)) {
                val look = CreatureSprites.lookRich(raw, AvatarSpecies.FENNEC, AvatarShading.Side.NONE,
                    500L, direction, moving = false, motionCue = CreatureSprites.MotionCue(motion, .5f))
                assertEquals(0, look.liftCells)
                assertTrue(look.frame in CreatureSprites.Rich.ACTION_FIRST until CreatureSprites.Rich.FRAME_COUNT)
                assertEquals(direction == PlayControl.Dir.LEFT, look.mirrored)
            }
        }
    }

    @Test
    fun `Sitzen bleibt sitzen und Aufstehen endet im Stand`() {
        assertEquals(CreatureSprites.Rich.ACTION_FIRST+9, CreatureSprites.motionFrame(
            CreatureSprites.MotionCue(CreatureSprites.Motion.SIT, 1f), CreatureSprites.Facing.RIGHT))
        assertEquals(CreatureSprites.Rich.ACTION_FIRST+11, CreatureSprites.motionFrame(
            CreatureSprites.MotionCue(CreatureSprites.Motion.RISE, 1f), CreatureSprites.Facing.RIGHT))
        assertEquals(CreatureSprites.Rich.FRONT_ACTION_FIRST+3, CreatureSprites.motionFrame(
            CreatureSprites.MotionCue(CreatureSprites.Motion.SIT, 1f), CreatureSprites.Facing.FRONT))
        for (motion in CreatureSprites.Motion.entries) for (facing in CreatureSprites.Facing.entries) {
            for (progress in listOf(-1f, 0f, .5f, 1f, 2f, Float.NaN)) {
                assertTrue(CreatureSprites.motionFrame(CreatureSprites.MotionCue(motion, progress), facing)
                    in CreatureSprites.Rich.ACTION_FIRST until CreatureSprites.Rich.FRAME_COUNT)
            }
        }
    }

    @Test
    fun `Sprung behaelt die wirkliche Hoehe waehrend Gehen Vorrang vor Gestik hat`() {
        val raw = AvatarAnimations.reactionFor(AvatarSpecies.FENNEC, AnimationType.MOVE).frames
            .first { CreatureSprites.liftOf(it, AvatarSpecies.FENNEC) >= 2 }
        val cue = CreatureSprites.MotionCue(CreatureSprites.Motion.JUMP, .5f)
        val jump = CreatureSprites.lookRich(raw, AvatarSpecies.FENNEC, AvatarShading.Side.NONE,
            500L, moving = false, motionCue = cue)
        assertEquals(CreatureSprites.liftOf(raw, AvatarSpecies.FENNEC), jump.liftCells)
        val walking = CreatureSprites.lookRich(raw, AvatarSpecies.FENNEC, AvatarShading.Side.NONE,
            500L, PlayControl.Dir.UP, moving = true, gaitTimeMs = 0L, motionCue = cue)
        assertEquals(CreatureSprites.Rich.DRAWN_BACK_WALK_FIRST, walking.frame)
        assertEquals(0, walking.liftCells)
    }

    /**
     * Ein kleiner PNG-Leser nur fuer diesen Test: 8 Bit RGBA, ohne Zeilensprung - so schreibt
     * `tools/character-art/sheets.py`. `javax.imageio` gibt es in den Android-Unit-Tests nicht.
     */
    private class Png(val width: Int, val height: Int, private val rgba: ByteArray) {
        fun alpha(x: Int, y: Int): Int = rgba[(y * width + x) * 4 + 3].toInt() and 0xFF

        companion object {
            fun read(bytes: ByteArray): Png {
                fun int(at: Int) = ((bytes[at].toInt() and 0xFF) shl 24) or ((bytes[at + 1].toInt() and 0xFF) shl 16) or
                    ((bytes[at + 2].toInt() and 0xFF) shl 8) or (bytes[at + 3].toInt() and 0xFF)
                var pos = 8
                var width = 0
                var height = 0
                val data = ByteArrayOutputStream()
                while (pos < bytes.size) {
                    val len = int(pos)
                    val type = String(bytes, pos + 4, 4, Charsets.US_ASCII)
                    if (type == "IHDR") {
                        width = int(pos + 8)
                        height = int(pos + 12)
                        assertEquals("8 Bit", 8, bytes[pos + 16].toInt())
                        assertEquals("RGBA", 6, bytes[pos + 17].toInt())
                    }
                    if (type == "IDAT") data.write(bytes, pos + 8, len)
                    pos += 12 + len
                }
                val inflater = Inflater().apply { setInput(data.toByteArray()) }
                val stride = width * 4
                val raw = ByteArray(height * (stride + 1))
                var got = 0
                while (got < raw.size && !inflater.finished()) got += inflater.inflate(raw, got, raw.size - got)
                val out = ByteArray(height * stride)
                for (y in 0 until height) {
                    val filter = raw[y * (stride + 1)].toInt()
                    for (i in 0 until stride) {
                        val v = raw[y * (stride + 1) + 1 + i].toInt() and 0xFF
                        val a = if (i >= 4) out[y * stride + i - 4].toInt() and 0xFF else 0
                        val b = if (y > 0) out[(y - 1) * stride + i].toInt() and 0xFF else 0
                        val c = if (i >= 4 && y > 0) out[(y - 1) * stride + i - 4].toInt() and 0xFF else 0
                        val pred = when (filter) {
                            0 -> 0
                            1 -> a
                            2 -> b
                            3 -> (a + b) / 2
                            else -> {
                                val p = a + b - c
                                val pa = kotlin.math.abs(p - a)
                                val pb = kotlin.math.abs(p - b)
                                val pc = kotlin.math.abs(p - c)
                                if (pa <= pb && pa <= pc) a else if (pb <= pc) b else c
                            }
                        }
                        out[y * stride + i] = (v + pred).toByte()
                    }
                }
                return Png(width, height, out)
            }
        }
    }
}
