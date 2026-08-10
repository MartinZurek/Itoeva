package com.notime.glyphsim.reminder

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.notime.glyphcore.reminder.ReminderPause
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueft die Pause an ihrer echten Ablage - dass sie sich setzen, lesen, ablaufen und aufheben
 * laesst.
 *
 * Die Zeitrechnung selbst liegt als JVM-Test in `ReminderPauseTest`; hier geht es um das, was ein
 * Nachbau nicht zeigen wuerde: dass der Zustand ueber SharedPreferences wirklich haelt und dass
 * ein abgelaufener Zeitpunkt von selbst als "nicht pausiert" gilt.
 *
 * **Warum das wichtig ist:** die Pause verhindert nur, dass NEUE Alarme gestellt werden. Bliebe
 * sie nach Ablauf haengen, kaeme nie wieder eine Erinnerung - und niemand wuerde nach einer
 * abgelaufenen Pause suchen.
 */
@RunWith(AndroidJUnit4::class)
class ReminderPauseEffectTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() = ReminderPause.resume(context)

    @After
    fun tearDown() = ReminderPause.resume(context)

    @Test
    fun ohneEingriffIstNichtsPausiert() {
        assertFalse(ReminderPause.isPaused(context))
        assertNull(ReminderPause.pausedUntil(context))
    }

    @Test
    fun einePauseHaeltUndNenntIhrEnde() {
        val ende = System.currentTimeMillis() + 60 * 60 * 1000L
        ReminderPause.pauseUntil(context, ende)

        assertTrue(ReminderPause.isPaused(context))
        assertEquals(ende, ReminderPause.pausedUntil(context))
    }

    /**
     * **Der Fall, auf den es ankommt.** Ein abgelaufener Zeitpunkt zaehlt als "nicht pausiert",
     * ohne dass jemand aufraeumen muss. Waere das anders, bliebe die App nach der ersten Pause
     * dauerhaft still.
     */
    @Test
    fun einAbgelaufenerZeitpunktGiltNichtMehrAlsPause() {
        ReminderPause.pauseUntil(context, System.currentTimeMillis() - 1)

        assertFalse("Eine abgelaufene Pause muss von selbst enden", ReminderPause.isPaused(context))
        assertNull(ReminderPause.pausedUntil(context))
    }

    /**
     * Der Ablauf laesst sich mit hereingereichter Zeit pruefen, ohne zu warten - dieselbe
     * Pause ist "vorher" aktiv und "nachher" vorbei.
     */
    @Test
    fun dieselbePauseIstVorherAktivUndNachherVorbei() {
        val jetzt = System.currentTimeMillis()
        val ende = jetzt + 60 * 60 * 1000L
        ReminderPause.pauseUntil(context, ende)

        assertTrue(ReminderPause.isPaused(context, nowMillis = ende - 1))
        assertFalse(ReminderPause.isPaused(context, nowMillis = ende))
        assertFalse(ReminderPause.isPaused(context, nowMillis = ende + 1))
    }

    @Test
    fun fortsetzenHebtDiePauseSofortAuf() {
        ReminderPause.pauseUntil(context, System.currentTimeMillis() + 60 * 60 * 1000L)
        assertTrue(ReminderPause.isPaused(context))

        ReminderPause.resume(context)

        assertFalse(ReminderPause.isPaused(context))
    }

    /**
     * Die angebotenen Dauern ergeben an der echten Ablage dasselbe wie in der reinen Rechnung -
     * die Bruecke zwischen `endOf` und `pauseUntil` haelt.
     */
    @Test
    fun dieAngebotenenDauernLassenSichSetzen() {
        val jetzt = System.currentTimeMillis()

        ReminderPause.Duration.entries.forEach { dauer ->
            val ende = ReminderPause.endOf(dauer, jetzt)
            ReminderPause.pauseUntil(context, ende)

            assertEquals("$dauer", ende, ReminderPause.pausedUntil(context, nowMillis = jetzt))
            assertTrue("$dauer muss in der Zukunft enden", ende > jetzt)
        }
    }
}
