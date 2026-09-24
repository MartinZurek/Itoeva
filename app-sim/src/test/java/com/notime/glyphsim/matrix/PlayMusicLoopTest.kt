package com.notime.glyphsim.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft die Naht einer Schleife - [PlayMusicLoop].
 *
 * Der gemeldete Fall dahinter: Viele erzeugte Stuecke blenden trotz Verbot am Ende aus, und die
 * harte Schleife des Players spielte dieses Loch jedes Mal mit.
 */
class PlayMusicLoopTest {

    private val neunzig = 89_000L
    private val erste: (List<Int>, Int) -> Int? = { v, c -> v.firstOrNull { it != c } }

    private fun plan(
        durationMs: Long = neunzig,
        current: Int = 1,
        variants: List<Int> = listOf(1, 2),
        fixedVariant: Int? = null,
        rotate: Boolean = false
    ) = PlayMusicLoop.plan(
        durationMs, current, variants, fixedVariant, rotate,
        variantFadeMs = PlayMusicTransition.VARIANT_FADE_MS, pickOther = erste
    )

    @Test
    fun `die Naht ist vorbei, bevor der Ausklang beginnt`() {
        val naht = plan()!!
        assertTrue(naht.atMs + naht.fadeMs <= neunzig - PlayMusicLoop.TAIL_TRIM_MS)
    }

    @Test
    fun `ohne Rotation beginnt dasselbe Stueck mit kurzer Blende von vorn`() {
        val naht = plan()!!
        assertEquals(1, naht.nextVariant)
        assertEquals(PlayMusicLoop.SEAM_FADE_MS, naht.fadeMs)
    }

    /** Die Rotation wechselt am Ende eines Durchlaufs, nicht mitten im Takt. */
    @Test
    fun `mit Rotation beginnt an der Naht ein anderes Stueck`() {
        val naht = plan(rotate = true)!!
        assertEquals(2, naht.nextVariant)
        assertEquals(PlayMusicTransition.VARIANT_FADE_MS, naht.fadeMs)
        // Die laengere Blende beginnt entsprechend frueher - auch sie endet vor dem Ausklang.
        assertTrue(naht.atMs + naht.fadeMs <= neunzig - PlayMusicLoop.TAIL_TRIM_MS)
    }

    @Test
    fun `mit nur einem Stueck bleibt es auch bei faelliger Rotation dabei`() {
        val naht = plan(variants = listOf(1), rotate = true)!!
        assertEquals(1, naht.nextVariant)
        assertEquals(PlayMusicLoop.SEAM_FADE_MS, naht.fadeMs)
    }

    /** Ein Charakterthema gehoert einem Wesen - an seiner Naht wechselt es nie das Stueck. */
    @Test
    fun `ein festes Stueck rotiert auch an der Naht nicht`() {
        val naht = plan(current = 3, variants = listOf(1, 2, 3), fixedVariant = 3, rotate = true)!!
        assertEquals(3, naht.nextVariant)
    }

    /** Der Player meldet -1, wenn er die Laenge nicht kennt - dann schleift er wie bisher. */
    @Test
    fun `unbekannte oder zu kurze Laengen planen keine Naht`() {
        assertNull(plan(durationMs = -1L))
        assertNull(plan(durationMs = PlayMusicLoop.MIN_PIECE_MS - 1))
    }

    /** Laut Messung vom 2026-09-24 war der laengste Ausklang sieben Sekunden (morning-03). */
    @Test
    fun `der ausgelassene Rest deckt den laengsten gemessenen Ausklang`() {
        assertTrue(PlayMusicLoop.TAIL_TRIM_MS >= 7_000L)
        // ... aber es bleibt der weitaus groesste Teil des Stuecks zu hoeren.
        assertTrue(PlayMusicLoop.playableMs(neunzig, PlayMusicTransition.VARIANT_FADE_MS) > 70_000L)
    }
}
