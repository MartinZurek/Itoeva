package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import java.io.File
import javax.imageio.ImageIO
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
            val img = ImageIO.read(file)
            assertEquals(CreatureSprites.FRAME * CreatureSprites.FRAME_COUNT, img.width)
            assertEquals(CreatureSprites.FRAME, img.height)
            // Die Fuesse stehen auf der vereinbarten Zeile: darunter nichts, in der Zeile darueber etwas.
            val ruhe = (0 until CreatureSprites.FRAME).map { x -> (img.getRGB(x, CreatureSprites.FEET - 1) ushr 24) > 0 }
            assertTrue("$species ohne Fuesse", ruhe.any { it })
            for (x in 0 until CreatureSprites.FRAME) assertEquals(0, img.getRGB(x, CreatureSprites.FEET + 1) ushr 24)
        }
    }
}
