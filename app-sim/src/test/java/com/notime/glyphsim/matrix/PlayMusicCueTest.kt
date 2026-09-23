package com.notime.glyphsim.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft, wann ein Gast beim Hereinkommen sein Thema anspielen darf - [PlayMusicCue].
 *
 * Am Geraet liesse sich das nur beobachten, indem man eine Stunde im Park wartet und mitzaehlt.
 * Der Kern der Regel ist aber nicht das Anspielen, sondern das Weglassen: Ohne die Pausen
 * wuerde die Musik bei vier Gaesten zum Senderwechsel.
 */
class PlayMusicCueTest {

    private val alleThemen = (1..AvatarSpecies.entries.size).toList()
    private val jetzt = 1_000_000_000L

    private fun judge(
        guest: AvatarSpecies = AvatarSpecies.STARLET,
        host: AvatarSpecies? = AvatarSpecies.GLOOP,
        playingRole: MusicRole? = MusicRole.MAIN_DAY,
        cueRunning: Boolean = false,
        themeVariants: Collection<Int> = alleThemen,
        memory: PlayMusicCue.Memory = PlayMusicCue.Memory(),
        nowMs: Long = jetzt
    ) = PlayMusicCue.judge(guest, host, playingRole, cueRunning, themeVariants, memory, nowMs)

    @Test
    fun `ein fremder Gast ueber laufender Szenenmusik bekommt seinen Auftritt`() {
        assertEquals(PlayMusicCue.Verdict.PLAY, judge())
    }

    /** Die Regel aus PlayMusic gilt auch hier: Nichts schaltet Musik eigenmaechtig ein. */
    @Test
    fun `ohne laufende Musik kein Einspieler`() {
        assertEquals(PlayMusicCue.Verdict.NO_MUSIC, judge(playingRole = null))
    }

    @Test
    fun `die eigene Begruessung hat Vorrang vor jedem Gast`() {
        assertEquals(
            PlayMusicCue.Verdict.GREETING_PLAYING,
            judge(playingRole = MusicRole.CHARACTER_THEME)
        )
    }

    @Test
    fun `ein Gast vom selben Wesen bringt kein eigenes Motiv mit`() {
        assertEquals(
            PlayMusicCue.Verdict.SAME_AS_HOST,
            judge(guest = AvatarSpecies.GLOOP, host = AvatarSpecies.GLOOP)
        )
    }

    @Test
    fun `hoechstens ein Einspieler zugleich`() {
        assertEquals(PlayMusicCue.Verdict.CUE_RUNNING, judge(cueRunning = true))
    }

    /** Wie bei der Begruessung: ein fremdes Stueck ist schlechter als gar keines. */
    @Test
    fun `fehlt genau dieses Thema, bleibt es beim Szenenscore`() {
        val ohneStarlet = alleThemen - MusicRole.characterThemeVariant(AvatarSpecies.STARLET)
        assertEquals(PlayMusicCue.Verdict.NO_TRACK, judge(themeVariants = ohneStarlet))
        assertEquals(
            PlayMusicCue.Verdict.PLAY,
            judge(guest = AvatarSpecies.HOOTLET, themeVariants = ohneStarlet)
        )
    }

    // ================= Die Pausen =================

    @Test
    fun `kurz nach einem Einspieler schweigt auch ein anderes Wesen`() {
        val eben = PlayMusicCue.remember(
            PlayMusicCue.Memory(), AvatarSpecies.HOOTLET, jetzt - 30_000L
        )
        assertEquals(PlayMusicCue.Verdict.COOLDOWN, judge(memory = eben))
    }

    @Test
    fun `nach der globalen Pause darf das naechste Wesen`() {
        val damals = PlayMusicCue.remember(
            PlayMusicCue.Memory(), AvatarSpecies.HOOTLET,
            jetzt - PlayMusicCue.GLOBAL_COOLDOWN_MS
        )
        assertEquals(PlayMusicCue.Verdict.PLAY, judge(memory = damals))
    }

    @Test
    fun `wer gerade seinen Auftritt hatte, wiederholt ihn nicht beim Wiederkommen`() {
        val damals = PlayMusicCue.remember(
            PlayMusicCue.Memory(), AvatarSpecies.STARLET,
            jetzt - PlayMusicCue.GLOBAL_COOLDOWN_MS - 1
        )
        assertEquals(PlayMusicCue.Verdict.SPECIES_COOLDOWN, judge(memory = damals))
        // Ein anderes Wesen ist davon unberuehrt.
        assertEquals(PlayMusicCue.Verdict.PLAY, judge(guest = AvatarSpecies.FENNEC, memory = damals))
        // Und nach der Pause je Wesen darf auch Starlet wieder.
        assertEquals(
            PlayMusicCue.Verdict.PLAY,
            judge(nowMs = jetzt + PlayMusicCue.SPECIES_COOLDOWN_MS, memory = damals)
        )
    }

    @Test
    fun `eine zurueckgestellte Uhr sperrt den Einspieler nicht`() {
        val zukunft = PlayMusicCue.remember(
            PlayMusicCue.Memory(), AvatarSpecies.STARLET, jetzt + 3_600_000L
        )
        assertEquals(PlayMusicCue.Verdict.PLAY, judge(memory = zukunft))
    }

    /** Die globale Pause ist kuerzer als die je Wesen - sonst waere die zweite bedeutungslos. */
    @Test
    fun `die Pausen stehen im richtigen Verhaeltnis`() {
        assertTrue(PlayMusicCue.GLOBAL_COOLDOWN_MS < PlayMusicCue.SPECIES_COOLDOWN_MS)
    }

    // ================= Die Dauer =================

    @Test
    fun `wer bleibt, behaelt sein Thema nur eine Phrase lang`() {
        assertFalse(PlayMusicCue.releaseDue(PlayMusicCue.MAX_HOLD_MS - 1, guestLeaving = false))
        assertTrue(PlayMusicCue.releaseDue(PlayMusicCue.MAX_HOLD_MS, guestLeaving = false))
    }

    @Test
    fun `wer eilig geht, laesst sein Motiv trotzdem ausreden`() {
        assertFalse(PlayMusicCue.releaseDue(3_000L, guestLeaving = true))
        assertTrue(PlayMusicCue.releaseDue(PlayMusicCue.MIN_HOLD_MS, guestLeaving = true))
    }

    /**
     * Die Mindestdauer muss nach dem Aufklingen noch zwei Motive tragen: Bei 66 BPM (dem
     * langsamsten Thema) sind zwei Takte gut sieben Sekunden.
     */
    @Test
    fun `die Mindestdauer traegt nach dem Aufklingen zwei Takte des langsamsten Themas`() {
        val zweiTakteBei66 = 2 * 4 * 60_000L / 66
        assertTrue(PlayMusicCue.MIN_HOLD_MS - PlayMusicCue.FADE_IN_MS >= zweiTakteBei66)
        assertTrue(PlayMusicCue.MIN_HOLD_MS < PlayMusicCue.MAX_HOLD_MS)
    }

    /**
     * Ein Einspieler samt Ausklang muss vorbei sein, bevor die globale Pause den naechsten
     * erlaubt - sonst ueberlagerten sich zwei Gaeste.
     */
    @Test
    fun `ein Einspieler ist vorbei, bevor der naechste erlaubt ist`() {
        assertTrue(
            PlayMusicCue.MAX_HOLD_MS + PlayMusicCue.FADE_OUT_MS < PlayMusicCue.GLOBAL_COOLDOWN_MS
        )
    }

    /** Der Einspieler darf nicht laenger werden als das Stueck, aus dem er stammt. */
    @Test
    fun `der Einspieler bleibt innerhalb des ersten Durchlaufs`() {
        assertTrue(
            PlayMusicCue.MAX_HOLD_MS + PlayMusicCue.FADE_OUT_MS < PlayCharacterTheme.PIECE_MS
        )
    }
}
