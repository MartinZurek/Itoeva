package com.notime.glyphsim.ui

import com.notime.glyphsim.matrix.MusicContext
import com.notime.glyphsim.matrix.MusicRole
import com.notime.glyphsim.matrix.PlayAmbientActivity
import com.notime.glyphsim.matrix.PlayScene
import com.notime.glyphsim.settings.SettingsCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft, OB im Spielmodus ueberhaupt Musik laufen darf - die zweite Haelfte der Trennung
 * "die Welt entscheidet was, der Nutzer entscheidet ob".
 *
 * Diese Faelle sind am Geraet kaum herzustellen: Man muesste mit laufendem Podcast und stumm
 * gestelltem Telefon in den Spielmodus gehen, und zwar in einem Build, in dem der passende
 * Track noch fehlt.
 */
class PlayMusicTest {

    private val abendsZuhause = MusicContext(
        PlayAmbientActivity.DayPhase.EVENING, PlayScene.Place.LIVING
    )
    private val mittagsImPark = MusicContext(
        PlayAmbientActivity.DayPhase.MIDDAY, PlayScene.Place.PARK
    )

    /** Der heutige Auslieferungsstand: Tages- und Abendtrack. */
    private val heute = setOf(MusicRole.HOME_EVENING, MusicRole.MAIN_DAY)

    private fun decide(
        enabled: Boolean = true,
        context: MusicContext = abendsZuhause,
        available: Set<MusicRole> = heute,
        otherAudioActive: Boolean = false,
        deviceSilent: Boolean = false
    ) = PlayMusic.decide(enabled, context, available, otherAudioActive, deviceSilent)

    // ================= Das OB =================

    @Test
    fun `eingeschaltet, Track passend, nichts im Weg - dann laeuft sie`() {
        assertEquals(MusicRole.HOME_EVENING, decide())
    }

    /**
     * **Die wichtigste Zeile dieser Datei.** Beim allerersten Start ist der Schalter aus; eine
     * App, die nach einem Update ungefragt Musik spielt, hat ihr Vertrauen verspielt, bevor der
     * erste Takt vorbei ist.
     */
    @Test
    fun `der Standard beim allerersten Start ist aus`() {
        assertFalse(SettingsCatalog.MusicEnabled.default)
    }

    /**
     * **Kein Szenenwechsel darf Musik eigenmaechtig einschalten.** Ausgeschaltet heisst
     * ausgeschaltet, egal wie gut die Szene passen wuerde - deshalb steht `enabled` in
     * [PlayMusic.decide] vor dem Resolver und nicht daneben.
     */
    @Test
    fun `ausgeschaltet schlaegt jede noch so passende Szene`() {
        assertNull(decide(enabled = false))
        assertNull(decide(enabled = false, context = mittagsImPark))
        assertNull(decide(enabled = false, available = MusicRole.entries.toSet()))
    }

    /**
     * Laeuft ein Podcast, bleibt es still - und zwar ohne Audio-Focus anzufordern. Wer Focus
     * greift, pausiert die Wiedergabe des Nutzers; ein Spielmodus, der den Podcast anhaelt, ist
     * kaputt, unabhaengig davon, wie huebsch die Musik war.
     */
    @Test
    fun `ueber fremdem Ton bleibt es still`() {
        assertNull(decide(otherAudioActive = true))
    }

    @Test
    fun `bei stumm gestelltem Geraet bleibt es still`() {
        assertNull(decide(deviceSilent = true))
    }

    // ================= Das OB trifft das WAS =================

    /**
     * Eingeschaltet, aber fuer diese Lage gibt es nichts: still. Das bleibt wichtig fuer Builds,
     * in denen ein erzeugtes Asset noch nicht gemergt ist.
     */
    @Test
    fun `eingeschaltet ohne passenden Track ergibt Stille`() {
        assertNull(decide(context = mittagsImPark, available = setOf(MusicRole.HOME_EVENING)))
    }

    /** Sobald der Tages-Track existiert, fuellt sich genau diese Luecke - ohne weitere Aenderung. */
    @Test
    fun `mit Tages-Track wird aus der Mittagsstille Musik`() {
        assertEquals(
            MusicRole.MAIN_DAY,
            decide(context = mittagsImPark)
        )
    }

    /** Jede Sperre allein genuegt; sie muessen sich nicht gegenseitig aufheben koennen. */
    @Test
    fun `jede Sperre allein genuegt`() {
        assertNull(decide(enabled = false, otherAudioActive = true))
        assertNull(decide(otherAudioActive = true, deviceSilent = true))
        assertNull(decide(enabled = false, deviceSilent = true, available = emptySet()))
    }

    @Test
    fun `Ueberblendung behaelt an Anfang Mitte und Ende ihre Energie`() {
        val start = PlayMusic.transitionVolumes(0f)
        val middle = PlayMusic.transitionVolumes(0.5f)
        val end = PlayMusic.transitionVolumes(1f)

        assertEquals(0.35f, start.first, 0.0001f)
        assertEquals(0f, start.second, 0.0001f)
        assertTrue(middle.first > 0f && middle.second > 0f)
        assertEquals(middle.first, middle.second, 0.0001f)
        assertEquals(0f, end.first, 0.0001f)
        assertEquals(0.35f, end.second, 0.0001f)
    }

    @Test
    fun `unterbrochene Ueberblendung setzt ohne Lautstaerkesprung fort`() {
        val teilweiseEingeblendet = PlayMusic.transitionVolumes(0.25f).second
        val neuerWechsel = PlayMusic.transitionVolumes(0f, teilweiseEingeblendet)

        assertEquals(teilweiseEingeblendet, neuerWechsel.first, 0.0001f)
        assertEquals(0f, neuerWechsel.second, 0.0001f)
    }
}
