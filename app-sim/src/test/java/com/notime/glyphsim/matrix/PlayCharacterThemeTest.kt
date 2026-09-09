package com.notime.glyphsim.matrix

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft den Anlass fuer das Charakterstueck - **wann** es laeuft.
 *
 * Am Geraet waere davon nichts zu belegen, ohne bis Mitternacht zu warten oder die Systemuhr zu
 * verstellen. Genau deshalb ist die Regel eine reine Funktion und die Ablage ein eigenes,
 * winziges Objekt ([com.notime.glyphsim.ui.PlayCharacterThemeLog]).
 */
class PlayCharacterThemeTest {

    private val heute = LocalDate.of(2026, 9, 9)

    @Test
    fun `wer heute noch nicht begruesst hat, begruesst`() {
        assertTrue(PlayCharacterTheme.isDue(lastGreetedDay = null, today = heute))
    }

    @Test
    fun `zweimal am selben Tag nicht`() {
        // Die eine Zusicherung, die aus dem Thema kein Hintergrundstueck macht.
        assertFalse(PlayCharacterTheme.isDue(heute.toEpochDay(), heute))
    }

    @Test
    fun `morgen wieder`() {
        assertTrue(PlayCharacterTheme.isDue(heute.minusDays(1).toEpochDay(), heute))
        assertTrue(PlayCharacterTheme.isDue(heute.minusDays(30).toEpochDay(), heute))
    }

    @Test
    fun `eine zurueckgestellte Uhr sperrt das Wesen nicht tagelang stumm`() {
        // Ein Gruss zuviel ist ein kleiner Fehler, ein wochenlang stummes Wesen ein grosser -
        // deshalb Gleichheit und nicht "liegt vor heute".
        assertTrue(PlayCharacterTheme.isDue(heute.plusDays(5).toEpochDay(), heute))
    }

    // ================= Die Laenge der Begruessung =================

    /**
     * **Der Punkt, an dem aus einer Begruessung Hintergrundmusik wuerde.** Der Player laesst
     * jeden Track in einer Schleife laufen; endete das Fenster erst mit dem Stueck, begaenne es
     * hoerbar ein zweites Mal, bevor der Uebergang zurueck in den Tag einsetzt.
     */
    @Test
    fun `das Fenster endet vor dem zweiten Durchlauf`() {
        assertTrue(PlayCharacterTheme.GREETING_MS < PlayCharacterTheme.PIECE_MS)
        assertEquals(
            PlayCharacterTheme.PIECE_MS - PlayCharacterTheme.FADE_MS,
            PlayCharacterTheme.GREETING_MS
        )
    }

    @Test
    fun `die Begruessung ist ein Stueck lang und keine Minute`() {
        // Gegen die naheliegende spaetere Kuerzung auf "ein paar Sekunden Erkennungsmelodie":
        // Das Stueck soll durchklingen, nicht anspielen.
        assertTrue(PlayCharacterTheme.GREETING_MS > 60_000L)
    }
}
