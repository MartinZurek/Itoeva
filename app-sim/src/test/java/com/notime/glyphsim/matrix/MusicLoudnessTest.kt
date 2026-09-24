package com.notime.glyphsim.matrix

import kotlin.math.log10
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prueft den Lautheitsausgleich zwischen den Stuecken - [MusicLoudness]. */
class MusicLoudnessTest {

    private fun db(gain: Float) = 20 * log10(gain.toDouble())

    @Test
    fun `ein Stueck auf dem Ziel bleibt unveraendert`() {
        assertEquals(1f, MusicLoudness.gainForLufs(MusicLoudness.TARGET_LUFS), 0.0001f)
    }

    /** Der gemessene Fall: 99 Pixels mit -10,8 LUFS wird um 5,2 dB leiser. */
    @Test
    fun `ein lautes Stueck wird leiser, ein leises lauter`() {
        assertEquals(-5.2, db(MusicLoudness.gainForLufs(-10.8)), 0.01)
        assertEquals(3.5, db(MusicLoudness.gainForLufs(-19.5)), 0.01)
    }

    @Test
    fun `der Ausgleich hat Grenzen in beide Richtungen`() {
        assertEquals(MusicLoudness.MAX_BOOST_DB, db(MusicLoudness.gainForLufs(-40.0)), 0.01)
        assertEquals(-MusicLoudness.MAX_CUT_DB, db(MusicLoudness.gainForLufs(0.0)), 0.01)
    }

    /** Ohne Messwert spielt ein Stueck wie bisher - nie stumm, nie uebersteuert. */
    @Test
    fun `ohne Messwert bleibt alles beim Alten`() {
        assertEquals(1f, MusicLoudness.gainForLufs(null), 0f)
        assertEquals(1f, MusicLoudness.gainForLufs(Double.NaN), 0f)
        assertEquals(1f, MusicLoudness.gainFor("itoeva_gibt_es_nicht_99"), 0f)
    }

    /**
     * Die Grundlautstaerke des Players (0,35) mal der groesste Anhebungsfaktor bleibt unter 1 -
     * der Ausgleich kann nie ueber das hinaus, was der Player darstellen kann.
     */
    @Test
    fun `die groesste Anhebung passt in den Spielraum des Players`() {
        assertTrue(0.35f * MusicLoudness.gainForLufs(-99.0) <= 1f)
    }

    /** Die Tabelle ist erzeugt; jeder Eintrag muss ein plausibler Messwert sein. */
    @Test
    fun `die Tabelle enthaelt nur plausible Werte`() {
        assertTrue(MusicLoudnessTable.MEASURED_LUFS.isNotEmpty())
        for ((name, lufs) in MusicLoudnessTable.MEASURED_LUFS) {
            assertTrue(name, name.startsWith("itoeva_"))
            assertTrue("$name: $lufs", lufs in -40.0..-5.0)
        }
    }
}
