package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.AvatarWatchdog.Observation
import com.notime.glyphsim.matrix.AvatarWatchdog.Problem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft den Waechter ueber die Figur im Spielmodus.
 *
 * Die Faelle sind die gemeldeten vom 26.09. - Figur dunkel, Figur steht still, Figur nach einer
 * gezogenen Erinnerung aus dem Bild - und die gewollten Ablaeufe, in denen dieselben Zustaende
 * kurz vorkommen und NICHT angefasst werden duerfen.
 */
class AvatarWatchdogTest {

    /** Eine gesunde Figur mittig auf einem 1080x1920-Bild. */
    private fun healthy() = Observation(
        present = true,
        left = 400f,
        top = 1200f,
        sizePx = 300f,
        screenWidthPx = 1080f,
        screenHeightPx = 1920f
    )

    @Test
    fun gesundeFigurHatKeinProblem() {
        assertEquals(emptySet<Problem>(), AvatarWatchdog.problems(healthy()))
    }

    @Test
    fun fehlendeFigurWirdErkannt() {
        assertEquals(setOf(Problem.MISSING), AvatarWatchdog.problems(Observation(present = false)))
    }

    @Test
    fun liegengebliebenesAusblendenWirdErkannt() {
        // Ein abgebrochener Tuerdurchgang: ausgeblendet und dunkel, aber niemand geht durch.
        val found = AvatarWatchdog.problems(healthy().copy(hidden = true, brightness = 0f))
        assertEquals(setOf(Problem.HIDDEN, Problem.DIMMED), found)
    }

    @Test
    fun halbDunkleFigurWirdErkannt() {
        assertTrue(Problem.DIMMED in AvatarWatchdog.problems(healthy().copy(brightness = 0.4f)))
    }

    @Test
    fun tuerdurchgangIstKeinFehler() {
        val found = AvatarWatchdog.problems(
            healthy().copy(hidden = true, brightness = 0.2f, doorTransit = true)
        )
        assertEquals(emptySet<Problem>(), found)
    }

    @Test
    fun raketeUeberDemOberenRandWirdErkannt() {
        // Der Endpunkt der Raketen-Flugbahn: 1,6 Bildhoehen ueber der Startstelle.
        val flown = healthy().copy(top = 1200f - 1.6f * 1920f)
        assertEquals(setOf(Problem.OFF_SCREEN), AvatarWatchdog.problems(flown))
    }

    @Test
    fun laufendeRaketeWirdNichtAngefasst() {
        val flying = healthy().copy(top = -2000f, reacting = true)
        assertEquals(emptySet<Problem>(), AvatarWatchdog.problems(flying))
    }

    @Test
    fun gangWirdNichtAngefasst() {
        val walking = healthy().copy(left = 2000f, moving = true)
        assertFalse(Problem.OFF_SCREEN in AvatarWatchdog.problems(walking))
    }

    @Test
    fun seitlichHinausgelaufenWirdErkannt() {
        assertTrue(Problem.OFF_SCREEN in AvatarWatchdog.problems(healthy().copy(left = 1000f)))
        assertTrue(Problem.OFF_SCREEN in AvatarWatchdog.problems(healthy().copy(left = -200f)))
    }

    @Test
    fun knappAmRandIstNochSichtbar() {
        // Weniger als die Haelfte ragt hinaus - das ist noch im Bild.
        assertFalse(Problem.OFF_SCREEN in AvatarWatchdog.problems(healthy().copy(left = 900f)))
        assertTrue(AvatarWatchdog.isOnScreen(0f, 1620f, 300f, 1080f, 1920f))
    }

    @Test
    fun ohneGemesseneFlaecheWirdNichtUmgesetzt() {
        assertTrue(AvatarWatchdog.isOnScreen(-5000f, -5000f, 300f, 0f, 0f))
        assertTrue(AvatarWatchdog.isOnScreen(-5000f, -5000f, 0f, 1080f, 1920f))
    }

    @Test
    fun ungueltigePositionGiltAlsAusserhalb() {
        assertFalse(AvatarWatchdog.isOnScreen(Float.NaN, 100f, 300f, 1080f, 1920f))
    }

    @Test
    fun eingefroreneFigurWirdErkannt() {
        val frozen = healthy().copy(idleLoopActive = false, frameUnchangedMs = 6_000L)
        assertEquals(setOf(Problem.FROZEN), AvatarWatchdog.problems(frozen))
    }

    @Test
    fun kurzePauseZwischenZweiRegungenIstKeinEinfrieren() {
        val pause = healthy().copy(idleLoopActive = false, frameUnchangedMs = 800L)
        assertEquals(emptySet<Problem>(), AvatarWatchdog.problems(pause))
    }

    @Test
    fun tagesablaufDarfLangeStillhalten() {
        // Schlafen im Bett: keine Ruhe-Schleife, lange dasselbe Bild - der Ablauf fuehrt sie.
        val sleeping = healthy().copy(
            idleLoopActive = false, animatedElsewhere = true, frameUnchangedMs = 60_000L
        )
        assertEquals(emptySet<Problem>(), AvatarWatchdog.problems(sleeping))
    }

    @Test
    fun laufendeRuheSchleifeIstNieEingefroren() {
        val idle = healthy().copy(idleLoopActive = true, frameUnchangedMs = 60_000L)
        assertFalse(Problem.FROZEN in AvatarWatchdog.problems(idle))
    }

    @Test
    fun erstZweiPruefungenHintereinanderLoesenAus() {
        val first = setOf(Problem.DIMMED)
        assertEquals(emptySet<Problem>(), AvatarWatchdog.confirmed(emptySet(), first))
        assertEquals(first, AvatarWatchdog.confirmed(first, setOf(Problem.DIMMED, Problem.FROZEN)))
    }

    @Test
    fun rueckkehrStelleBleibtImBild() {
        assertEquals(1f, AvatarWatchdog.recoveryFraction(5000f, 300f, 1080f))
        assertEquals(0f, AvatarWatchdog.recoveryFraction(-5000f, 300f, 1080f))
        assertEquals(0.5f, AvatarWatchdog.recoveryFraction(390f, 300f, 1080f), 0.001f)
        assertEquals(0.5f, AvatarWatchdog.recoveryFraction(Float.NaN, 300f, 1080f))
        assertEquals(0.5f, AvatarWatchdog.recoveryFraction(10f, 2000f, 1080f))
    }
}
