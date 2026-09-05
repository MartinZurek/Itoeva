package com.notime.glyphsim.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft die Bedingungen, unter denen im Spielmodus ueberhaupt Musik laufen darf.
 *
 * Genau diese Faelle sind am Geraet kaum herzustellen - man muesste mit laufendem Podcast und
 * stumm gestelltem Telefon in den Spielmodus gehen, und zwar in einem Build, in dem noch kein
 * Track gemergt ist. Deshalb liegt die Entscheidung in [PlayMusic.shouldPlay] ohne Android.
 */
class PlayMusicTest {

    private fun play(
        enabled: Boolean = true,
        trackPresent: Boolean = true,
        otherAudioActive: Boolean = false,
        deviceSilent: Boolean = false
    ) = PlayMusic.shouldPlay(enabled, trackPresent, otherAudioActive, deviceSilent)

    /**
     * Der Normalfall - und der einzige, in dem etwas zu hoeren ist. Alles andere unten schaltet
     * ihn wieder ab.
     */
    @Test
    fun `eingeschaltet, Track da, nichts im Weg - dann laeuft sie`() {
        assertTrue(play())
    }

    /**
     * **Die wichtigste Zeile dieser Datei.** Der Standard ist AUS; eine App, die nach einem
     * Update ungefragt Musik spielt, hat ihr Vertrauen verspielt, bevor der erste Takt vorbei ist.
     */
    @Test
    fun `ohne Einschalten bleibt es still`() {
        assertFalse(play(enabled = false))
    }

    /**
     * Der Track kommt aus einem eigenen erzeugten Pull Request und fehlt in jedem Build, in dem
     * noch keiner gemergt wurde. Dann ist Stille der richtige Zustand - kein Absturz, keine
     * Fehlermeldung.
     */
    @Test
    fun `ohne ausgelieferten Track bleibt es still`() {
        assertFalse(play(trackPresent = false))
    }

    /**
     * Laeuft ein Podcast, bleibt es still - und zwar ohne Audio-Focus anzufordern. Wer Focus
     * greift, pausiert die Wiedergabe des Nutzers; ein Spielmodus, der den Podcast anhaelt, ist
     * kaputt, unabhaengig davon, wie huebsch die Musik war.
     */
    @Test
    fun `ueber fremdem Ton bleibt es still`() {
        assertFalse(play(otherAudioActive = true))
    }

    @Test
    fun `bei stumm gestelltem Geraet bleibt es still`() {
        assertFalse(play(deviceSilent = true))
    }

    /** Jede Sperre allein genuegt; sie muessen sich nicht gegenseitig aufheben koennen. */
    @Test
    fun `jede Sperre allein genuegt`() {
        assertFalse(play(enabled = false, trackPresent = false))
        assertFalse(play(otherAudioActive = true, deviceSilent = true))
        assertFalse(play(enabled = false, otherAudioActive = true, deviceSilent = true))
    }
}
