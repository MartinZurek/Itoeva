package com.notime.glyphsim.skilltree

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Prueft den Zustandshalter ([AvatarActivityBus]).
 *
 * Wenig Logik, aber eine Falle: Der Zustand ist prozessweit. Wer ihn zwischen Pruefungen nicht
 * zuruecksetzt, bekommt Tests, die einzeln durchlaufen und gemeinsam scheitern - und zwar je nach
 * Reihenfolge mal so und mal so.
 */
class AvatarActivityBusTest {

    private val now = 1_700_000_000_000L

    @Before fun setUp() = AvatarActivityBus.reset()
    @After fun tearDown() = AvatarActivityBus.reset()

    @Test
    fun `am Anfang laeuft nichts`() {
        assertNull(AvatarActivityBus.currentIfFresh(now))
    }

    @Test
    fun `eine gesetzte Beschaeftigung wird zurueckgegeben`() {
        AvatarActivityBus.set("sport/ballsport", now)
        assertEquals("sport/ballsport", AvatarActivityBus.currentIfFresh(now)?.nodeId)
    }

    @Test
    fun `eine abgelaufene Beschaeftigung verschwindet auch aus dem beobachtbaren Zustand`() {
        AvatarActivityBus.set("sport/ballsport", now)
        val spaeter = now + AvatarActivity.LIFETIME_MS

        assertNull(AvatarActivityBus.currentIfFresh(spaeter))
        assertNull(
            "Der beobachtbare Zustand haengt sonst der Entscheidung hinterher",
            AvatarActivityBus.current.value
        )
    }

    @Test
    fun `eine neue Beschaeftigung loest die vorige ab`() {
        AvatarActivityBus.set("ruhe/schlafen", now)
        AvatarActivityBus.set("sport/ballsport", now + 1000)
        assertEquals("sport/ballsport", AvatarActivityBus.currentIfFresh(now + 1000)?.nodeId)
    }
}
