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

    private val walkFrames = CreatureSprites.WALK_FIRST until CreatureSprites.WALK_FIRST + 4
    private val joyFrames = CreatureSprites.JOY_FIRST..CreatureSprites.JOY_FIRST + 1

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
            assertEquals(CreatureSprites.FRAME * CreatureSprites.FRAME_COUNT, img.width)
            assertEquals(CreatureSprites.FRAME, img.height)
            // Die Fuesse stehen auf der vereinbarten Zeile: darueber etwas, ab ihr nichts mehr.
            val fuesse = (0 until CreatureSprites.FRAME).any { x -> img.alpha(x, CreatureSprites.FEET - 1) > 0 }
            assertTrue("$species ohne Fuesse", fuesse)
            for (x in 0 until CreatureSprites.FRAME) assertEquals(0, img.alpha(x, CreatureSprites.FEET))
        }
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
