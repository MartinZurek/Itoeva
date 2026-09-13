package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayDreamsTest {
    @Test
    fun `sleep and medicine never become dream memories`() {
        assertFalse(PlayDreams.isEligibleMemory(AnimationType.SLEEP))
        assertFalse(PlayDreams.isEligibleMemory(AnimationType.MEDICINE))
        assertTrue(PlayDreams.isEligibleMemory(AnimationType.MOVE))
        assertTrue(PlayDreams.isEligibleMemory(AnimationType.CREATIVITY))
    }

    @Test
    fun `dream chooses only eligible experienced topics`() {
        val memories = listOf(AnimationType.SLEEP, AnimationType.MOVE, AnimationType.MEDICINE)
        repeat(20) { seed ->
            assertEquals(AnimationType.MOVE, PlayDreams.choose(memories, Random(seed)))
        }
    }

    @Test
    fun `no eligible experience means no dream`() {
        assertNull(PlayDreams.choose(listOf(AnimationType.SLEEP, AnimationType.MEDICINE), Random(1)))
    }

    @Test
    fun `Schlaf fasst die drei juengsten wirklichen Tageserlebnisse zusammen`() {
        val memories = listOf(
            AnimationType.DRINK,
            AnimationType.BOOK,
            AnimationType.MOVE,
            AnimationType.SLEEP,
            AnimationType.CREATIVITY
        )

        assertEquals(
            listOf(AnimationType.BOOK, AnimationType.MOVE, AnimationType.CREATIVITY),
            PlayDreams.highlights(memories)
        )
    }

    @Test
    fun `Schlaf erfindet nichts und behaelt das juengste Vorkommen`() {
        assertTrue(PlayDreams.highlights(emptyList()).isEmpty())
        assertEquals(
            listOf(AnimationType.MOVE, AnimationType.BOOK),
            PlayDreams.highlights(
                listOf(AnimationType.BOOK, AnimationType.MOVE, AnimationType.BOOK)
            )
        )
    }
}
