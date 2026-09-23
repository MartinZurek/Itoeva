package com.notime.glyphsim.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft, wann ein Musikwechsel stattfindet und wie lange er klingt - [PlayMusicTransition].
 *
 * Der gemeldete Fall dahinter: Abends vom Wohnzimmer ueber die Strasse in den Park waren zwei
 * volle Wechsel hintereinander zu hoeren, obwohl die Strasse nur ein Durchgang war.
 */
class PlayMusicTransitionTest {

    private val t0 = 5_000_000L

    // ================= Wann =================

    @Test
    fun `aus der Stille wird sofort gestartet`() {
        val s = PlayMusicTransition.settle(null, MusicRole.MAIN_DAY, null, t0)
        assertTrue(s.switchNow)
        assertNull(s.pending)
    }

    @Test
    fun `dieselbe Rolle loest nichts aus und verwirft jede Vormerkung`() {
        val alt = PlayMusicTransition.Pending(MusicRole.HOME_EVENING, t0 - 3_000L)
        val s = PlayMusicTransition.settle(MusicRole.MAIN_DAY, MusicRole.MAIN_DAY, alt, t0)
        assertFalse(s.switchNow)
        assertNull(s.pending)
        assertNull(s.recheckInMs)
    }

    @Test
    fun `ein neuer Wunsch wird erst vorgemerkt`() {
        val s = PlayMusicTransition.settle(MusicRole.HOME_EVENING, MusicRole.MAIN_DAY, null, t0)
        assertFalse(s.switchNow)
        assertEquals(PlayMusicTransition.Pending(MusicRole.MAIN_DAY, t0), s.pending)
        assertEquals(PlayMusicTransition.ROLE_SETTLE_MS, s.recheckInMs)
    }

    @Test
    fun `wer bleibt, bekommt die neue Musik nach der Bestaetigungszeit`() {
        val erst = PlayMusicTransition.settle(MusicRole.HOME_EVENING, MusicRole.MAIN_DAY, null, t0)
        val halb = PlayMusicTransition.settle(
            MusicRole.HOME_EVENING, MusicRole.MAIN_DAY, erst.pending, t0 + 4_000L
        )
        assertFalse(halb.switchNow)
        assertEquals(PlayMusicTransition.ROLE_SETTLE_MS - 4_000L, halb.recheckInMs)
        val dann = PlayMusicTransition.settle(
            MusicRole.HOME_EVENING, MusicRole.MAIN_DAY, halb.pending,
            t0 + PlayMusicTransition.ROLE_SETTLE_MS
        )
        assertTrue(dann.switchNow)
        assertNull(dann.pending)
    }

    /** Der gemeldete Fall: Wohnzimmer -> Strasse (kurz) -> Park. Kein einziger Wechsel. */
    @Test
    fun `ein kurzer Durchgang wirft die Musik nicht um`() {
        val strasse = PlayMusicTransition.settle(
            MusicRole.HOME_EVENING, MusicRole.MAIN_DAY, null, t0
        )
        assertFalse(strasse.switchNow)
        val park = PlayMusicTransition.settle(
            MusicRole.HOME_EVENING, MusicRole.HOME_EVENING, strasse.pending, t0 + 6_000L
        )
        assertFalse(park.switchNow)
        assertNull(park.pending)
    }

    @Test
    fun `pendelt der Wunsch, beginnt die Uhr neu`() {
        val a = PlayMusicTransition.settle(MusicRole.MAIN_DAY, MusicRole.SPORT, null, t0)
        val b = PlayMusicTransition.settle(
            MusicRole.MAIN_DAY, MusicRole.HOME_EVENING, a.pending, t0 + 9_000L
        )
        assertFalse(b.switchNow)
        assertEquals(PlayMusicTransition.Pending(MusicRole.HOME_EVENING, t0 + 9_000L), b.pending)
    }

    /** Das Ende der Begruessung ist auf die Sekunde gerechnet - dort wird nicht gewartet. */
    @Test
    fun `das eigene Stueck wartet nicht, weder hinein noch hinaus`() {
        assertTrue(
            PlayMusicTransition.settle(
                MusicRole.MAIN_DAY, MusicRole.CHARACTER_THEME, null, t0
            ).switchNow
        )
        assertTrue(
            PlayMusicTransition.settle(
                MusicRole.CHARACTER_THEME, MusicRole.MAIN_DAY, null, t0
            ).switchNow
        )
    }

    @Test
    fun `eine rueckwaerts gestellte Uhr faengt die Vormerkung neu an`() {
        val zukunft = PlayMusicTransition.Pending(MusicRole.MAIN_DAY, t0 + 60_000L)
        val s = PlayMusicTransition.settle(MusicRole.HOME_EVENING, MusicRole.MAIN_DAY, zukunft, t0)
        assertFalse(s.switchNow)
        assertEquals(PlayMusicTransition.Pending(MusicRole.MAIN_DAY, t0), s.pending)
    }

    // ================= Wie lange =================

    @Test
    fun `das eigene Stueck behaelt die Ueberblendung, auf die es gerechnet ist`() {
        assertEquals(
            PlayCharacterTheme.FADE_MS,
            PlayMusicTransition.fadeMs(MusicRole.MAIN_DAY, MusicRole.CHARACTER_THEME, false)
        )
        assertEquals(
            PlayCharacterTheme.FADE_MS,
            PlayMusicTransition.fadeMs(MusicRole.CHARACTER_THEME, MusicRole.HOME_EVENING, false)
        )
        // Auch der Wechsel des Wesens innerhalb der Rolle.
        assertEquals(
            PlayCharacterTheme.FADE_MS,
            PlayMusicTransition.fadeMs(MusicRole.CHARACTER_THEME, MusicRole.CHARACTER_THEME, true)
        )
    }

    @Test
    fun `die Blendlaengen folgen der Art des Wechsels`() {
        assertEquals(
            PlayMusicTransition.START_FADE_MS,
            PlayMusicTransition.fadeMs(null, MusicRole.MAIN_DAY, false)
        )
        assertEquals(
            PlayMusicTransition.SPORT_FADE_MS,
            PlayMusicTransition.fadeMs(MusicRole.MAIN_DAY, MusicRole.SPORT, false)
        )
        assertEquals(
            PlayMusicTransition.SCENE_FADE_MS,
            PlayMusicTransition.fadeMs(MusicRole.MORNING, MusicRole.MAIN_DAY, false)
        )
        assertEquals(
            PlayMusicTransition.VARIANT_FADE_MS,
            PlayMusicTransition.fadeMs(MusicRole.SPORT, MusicRole.SPORT, true)
        )
    }

    /** Hinein in Bewegung schneller als ein Lichtwechsel, eine Rotation am langsamsten. */
    @Test
    fun `die Blendlaengen stehen im richtigen Verhaeltnis`() {
        assertTrue(PlayMusicTransition.SPORT_FADE_MS < PlayMusicTransition.SCENE_FADE_MS)
        assertTrue(PlayMusicTransition.SCENE_FADE_MS < PlayMusicTransition.VARIANT_FADE_MS)
        // Die Bestaetigung ist laenger als jede Blende - sonst waere sie kaum hoerbar.
        assertTrue(PlayMusicTransition.ROLE_SETTLE_MS > PlayMusicTransition.VARIANT_FADE_MS)
    }
}
