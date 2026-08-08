package com.notime.glyphsim.ui

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests fuer die Wartezeit-Berechnung des Uhr-Ticks (siehe ClockTick.kt). Klein, aber genau die
 * Sorte Arithmetik, die still danebenliegt: ein Fehler hier faellt nicht als Absturz auf, sondern
 * als Uhr, die eine Minute nachgeht oder als Schleife, die heisslaeuft und den Akku frisst -
 * beides im Dock-Modus erst nach Stunden bemerkbar.
 */
class ClockTickTest {

    @Test
    fun `wartet bis zur naechsten vollen Minute`() {
        // 10:30:20 -> noch 40s bis 10:31:00.
        val wait = millisUntilNextMinute(LocalTime.of(10, 30, 20))
        assertEquals(40_000L + WAKEUP_MARGIN_MS, wait)
    }

    @Test
    fun `beruecksichtigt Millisekunden innerhalb der Sekunde`() {
        // 10:30:20.750 -> noch 39,25s.
        val wait = millisUntilNextMinute(LocalTime.of(10, 30, 20, 750_000_000))
        assertEquals(39_250L + WAKEUP_MARGIN_MS, wait)
    }

    @Test
    fun `exakt auf der Minutengrenze wartet eine volle Minute`() {
        val wait = millisUntilNextMinute(LocalTime.of(10, 30, 0))
        assertEquals(60_000L + WAKEUP_MARGIN_MS, wait)
    }

    @Test
    fun `wartet nie null - die Schleife darf nicht heisslaufen`() {
        // Ueber eine ganze Minute in feinen Schritten: kein einziger Wert darf 0 oder negativ
        // sein, sonst wuerde delay() sofort zurueckkehren und die Schleife die CPU belegen.
        for (second in 0..59) {
            for (millis in listOf(0, 1, 500, 999)) {
                val wait = millisUntilNextMinute(LocalTime.of(10, 30, second, millis * 1_000_000))
                assertTrue("Wartezeit muss positiv sein, war $wait bei $second.$millis", wait > 0L)
            }
        }
    }

    @Test
    fun `landet immer sicher in der neuen Minute`() {
        // Der Sicherheitsaufschlag existiert, damit der Aufwachzeitpunkt nicht knapp VOR der
        // Minutengrenze liegt (dann zeigte der Frame noch die alte Minute). Geprueft ueber eine
        // ganze Minute: Startzeit + Wartezeit muss immer echt in der Folgeminute liegen.
        for (second in 0..59) {
            val now = LocalTime.of(10, 30, second)
            val wait = millisUntilNextMinute(now)
            val wakeUp = now.plusNanos(wait * 1_000_000)
            assertEquals("Aufwachen muss in Minute 31 liegen, war $wakeUp", 31, wakeUp.minute)
        }
    }

    @Test
    fun `spart gegenueber dem Sekundentakt fast alle Wakeups`() {
        // Dokumentiert den eigentlichen Zweck der Aenderung: ueber eine Stunde bleiben statt 3600
        // Wakeups nur noch 60 uebrig. Faellt auf, falls jemand versehentlich wieder auf einen
        // kuerzeren Takt umstellt.
        var elapsedMs = 0L
        var wakeups = 0
        var now = LocalTime.of(10, 0, 0)
        while (elapsedMs < 60 * 60 * 1000L) {
            val wait = millisUntilNextMinute(now)
            elapsedMs += wait
            now = now.plusNanos(wait * 1_000_000)
            wakeups++
        }
        assertEquals(60, wakeups)
    }
}
