package com.notime.glyphcore.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Prueft die Zeitrechnung der Pausen-Steuerung.
 *
 * Reine Rechnung mit hereingereichter Zeit und Zeitzone - laeuft deshalb als JVM-Test mit. Die
 * Ablage selbst (SharedPreferences) ist nicht der interessante Teil; die Frage "wann endet eine
 * Pause" dagegen schon, und zwar besonders an den Umstellungstagen.
 */
class ReminderPauseTest {

    private val zone = ZoneId.of("Europe/Berlin")

    private fun at(text: String): Long =
        LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    private fun local(millis: Long): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zone)

    // --- Eine Stunde ---------------------------------------------------------------------------

    @Test
    fun `eine Stunde endet eine echte Stunde spaeter`() {
        val jetzt = at("2026-06-15T14:20:00")
        val ende = ReminderPause.endOf(ReminderPause.Duration.ONE_HOUR, jetzt, zone)

        assertEquals(60 * 60 * 1000L, ende - jetzt)
        assertEquals(LocalDateTime.parse("2026-06-15T15:20:00"), local(ende))
    }

    /**
     * Am Umstellungstag bleibt eine Stunde eine echte Stunde - die lokale Uhrzeit springt dabei
     * mit. Dasselbe Prinzip wie beim Planer (siehe ReminderSchedulerTimeZoneTest): Abstaende
     * gelten in echter Zeit, nicht auf der Wanduhr.
     */
    @Test
    fun `eine Stunde bleibt eine Stunde auch bei der Zeitumstellung`() {
        val vorDerLuecke = at("2026-03-29T01:30:00")
        val ende = ReminderPause.endOf(ReminderPause.Duration.ONE_HOUR, vorDerLuecke, zone)

        assertEquals(60 * 60 * 1000L, ende - vorDerLuecke)
        // 02:30 gibt es an diesem Tag nicht - lokal steht danach 03:30.
        assertEquals(LocalDateTime.parse("2026-03-29T03:30:00"), local(ende))
    }

    // --- Bis morgen ----------------------------------------------------------------------------

    /**
     * "Bis morgen" endet um Mitternacht und nicht in 24 Stunden. Ein Tagesziel endet ebenfalls
     * dort - der naechste Tag soll unbelastet anfangen.
     */
    @Test
    fun `bis morgen endet zur naechsten Mitternacht`() {
        val ende = ReminderPause.endOf(
            ReminderPause.Duration.UNTIL_TOMORROW,
            at("2026-06-15T14:20:00"),
            zone
        )

        assertEquals(LocalDateTime.parse("2026-06-16T00:00:00"), local(ende))
    }

    /**
     * Kurz vor Mitternacht bleibt eine kurze Pause - das ist richtig so. Wer um 23:50 "bis morgen"
     * waehlt, will nicht bis uebermorgen Ruhe, sondern den Rest dieses Tages.
     */
    @Test
    fun `kurz vor Mitternacht ergibt eine kurze Pause`() {
        val jetzt = at("2026-06-15T23:50:00")
        val ende = ReminderPause.endOf(ReminderPause.Duration.UNTIL_TOMORROW, jetzt, zone)

        assertEquals(LocalDateTime.parse("2026-06-16T00:00:00"), local(ende))
        assertTrue("Die Pause muss in der Zukunft liegen", ende > jetzt)
    }

    /**
     * Am Tag der Rueckstellung hat der Tag 25 Stunden - "bis morgen" muss trotzdem auf der
     * naechsten Mitternacht landen und nicht davor.
     */
    @Test
    fun `bis morgen trifft auch am langen Tag die Mitternacht`() {
        val jetzt = at("2026-10-25T20:00:00")
        val ende = ReminderPause.endOf(ReminderPause.Duration.UNTIL_TOMORROW, jetzt, zone)

        assertEquals(LocalDateTime.parse("2026-10-26T00:00:00"), local(ende))
        assertTrue(ende > jetzt)
    }

    // --- Jede Pause hat ein Ende -----------------------------------------------------------------

    /**
     * **Die tragende Zusage.** Keine der angebotenen Dauern fuehrt zu einer unbefristeten Pause.
     *
     * Ein Schalter "Erinnerungen aus" wird eingeschaltet und vergessen; danach kommt wochenlang
     * nichts und die App gilt als kaputt. Das ist der haeufigste Weg, wie eine Erinnerungs-App
     * still aufhoert zu erinnern - und der Grund, warum es "bis auf Weiteres" hier nicht gibt.
     */
    @Test
    fun `jede angebotene Dauer endet innerhalb eines Tages`() {
        val jetzt = at("2026-06-15T14:20:00")
        val einTag = 24 * 60 * 60 * 1000L

        ReminderPause.Duration.entries.forEach { dauer ->
            val ende = ReminderPause.endOf(dauer, jetzt, zone)
            assertTrue("$dauer liegt nicht in der Zukunft", ende > jetzt)
            assertTrue("$dauer dauert laenger als einen Tag", ende - jetzt <= einTag)
        }
    }
}
