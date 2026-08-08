package com.notime.glyphcore.reminder

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.DaysOfWeekMask
import com.notime.glyphcore.data.GlyphReminder
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests fuer [ReminderScheduler.nextTrigger] - die einzige Stelle im Projekt, an der aus
 * Wochentagsmaske, Zeitfenster und Intervall der naechste Alarmzeitpunkt entsteht. Sie ist
 * reine Logik (nur java.time, keine Android-Aufrufe) und laesst sich deshalb ohne Emulator
 * testen, obwohl sie im selben `object` wie die AlarmManager-Aufrufe steht.
 *
 * Warum ausgerechnet hier Tests: ein Fehler in dieser Funktion fuehrt nicht zu einem Absturz,
 * sondern dazu, dass eine Erinnerung still ausfaellt oder zur falschen Zeit kommt - das faellt
 * im manuellen Test kaum auf, weil man dafuer gezielt ueber Mitternacht, ueber einen
 * Wochentagswechsel oder ans Fensterende springen muesste.
 *
 * Feste Zeitzone statt [ZoneId.systemDefault] in allen Tests, damit sie unabhaengig von der
 * Zeitzone des ausfuehrenden Rechners bzw. der CI reproduzierbar sind.
 */
class ReminderSchedulerNextTriggerTest {

    private val zone = ZoneId.of("Europe/Berlin")

    private fun reminder(
        daysOfWeek: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
        startMinuteOfDay: Int = 9 * 60,
        endMinuteOfDay: Int = 18 * 60,
        intervalMinutes: Int = 60
    ) = GlyphReminder(
        id = 1L,
        label = "Test",
        animationType = AnimationType.GENERAL,
        daysOfWeekMask = DaysOfWeekMask.toMask(daysOfWeek),
        startMinuteOfDay = startMinuteOfDay,
        endMinuteOfDay = endMinuteOfDay,
        intervalMinutes = intervalMinutes
    )

    private fun at(text: String): Long =
        LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    private fun nextTriggerAfter(reminder: GlyphReminder, after: String): LocalDateTime? =
        ReminderScheduler.nextTrigger(reminder, at(after), zone)
            ?.let { LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), zone) }

    // ---- Grundfall: innerhalb des Fensters kommt der naechste Intervall-Slot ----

    @Test
    fun `waehrend des Fensters kommt der naechste Slot`() {
        // Mittwoch 10:30, Fenster 9-18 Uhr, stuendlich -> 11:00 desselben Tages.
        val next = nextTriggerAfter(reminder(), "2026-07-29T10:30:00")
        assertEquals(LocalDateTime.parse("2026-07-29T11:00"), next)
    }

    @Test
    fun `vor dem Fenster kommt der Fensterstart`() {
        val next = nextTriggerAfter(reminder(), "2026-07-29T06:00:00")
        assertEquals(LocalDateTime.parse("2026-07-29T09:00"), next)
    }

    @Test
    fun `nach dem Fenster kommt der Start des Folgetages`() {
        val next = nextTriggerAfter(reminder(), "2026-07-29T20:00:00")
        assertEquals(LocalDateTime.parse("2026-07-30T09:00"), next)
    }

    @Test
    fun `exakt auf einem Slot liefert den darauffolgenden, nicht denselben`() {
        // Wichtig fuer die Alarm-Kette: ReminderAlarmReceiver plant beim Feuern sofort neu und
        // uebergibt dabei den gerade erreichten Zeitpunkt - liefe hier derselbe Slot zurueck,
        // wuerde dieselbe Erinnerung endlos sofort wieder feuern.
        val next = nextTriggerAfter(reminder(), "2026-07-29T11:00:00")
        assertEquals(LocalDateTime.parse("2026-07-29T12:00"), next)
    }

    // ---- Fensterende ----

    @Test
    fun `letzter Slot liegt genau auf dem Fensterende`() {
        // 9-18 Uhr stuendlich: 18:00 ist der letzte Slot des Tages (Fensterlaenge exakt erreicht).
        val next = nextTriggerAfter(reminder(), "2026-07-29T17:30:00")
        assertEquals(LocalDateTime.parse("2026-07-29T18:00"), next)
    }

    @Test
    fun `Slot hinter dem Fensterende faellt weg und springt auf den Folgetag`() {
        // 9:00-9:50, Intervall 20 -> Slots 9:00, 9:20, 9:40; 10:00 laege hinter dem Fenster.
        val r = reminder(startMinuteOfDay = 9 * 60, endMinuteOfDay = 9 * 60 + 50, intervalMinutes = 20)
        val next = nextTriggerAfter(r, "2026-07-29T09:45:00")
        assertEquals(LocalDateTime.parse("2026-07-30T09:00"), next)
    }

    // ---- Fenster ueber Mitternacht (endMinuteOfDay < startMinuteOfDay) ----

    @Test
    fun `Fenster ueber Mitternacht laeuft in den Folgetag hinein`() {
        // 22:00-06:00, stuendlich. Um 23:30 am Mittwoch ist der naechste Slot 00:00 am Donnerstag.
        val r = reminder(startMinuteOfDay = 22 * 60, endMinuteOfDay = 6 * 60)
        val next = nextTriggerAfter(r, "2026-07-29T23:30:00")
        assertEquals(LocalDateTime.parse("2026-07-30T00:00"), next)
    }

    @Test
    fun `Fenster ueber Mitternacht wird am fruehen Morgen noch bedient`() {
        // Genau der Fall, fuer den nextTrigger() ab dayOffset -1 sucht: der Slot um 03:00 am
        // Donnerstag gehoert zum Fenster, das am MITTWOCH um 22:00 begonnen hat - der Mittwoch
        // muss also aktiver Wochentag sein, nicht der Donnerstag.
        val r = reminder(
            daysOfWeek = setOf(DayOfWeek.WEDNESDAY),
            startMinuteOfDay = 22 * 60,
            endMinuteOfDay = 6 * 60
        )
        val next = nextTriggerAfter(r, "2026-07-30T02:30:00") // Donnerstag frueh
        assertEquals(LocalDateTime.parse("2026-07-30T03:00"), next)
    }

    @Test
    fun `Fenster ueber Mitternacht endet am Folgetag korrekt`() {
        // Mittwoch-Fenster 22:00-06:00: nach 06:00 am Donnerstag gibt es keinen Slot mehr, der
        // naechste ist erst wieder Mittwoch 22:00 der Folgewoche.
        val r = reminder(
            daysOfWeek = setOf(DayOfWeek.WEDNESDAY),
            startMinuteOfDay = 22 * 60,
            endMinuteOfDay = 6 * 60
        )
        val next = nextTriggerAfter(r, "2026-07-30T07:00:00") // Donnerstag nach Fensterende
        assertEquals(LocalDateTime.parse("2026-08-05T22:00"), next)
    }

    // ---- Wochentagsmaske ----

    @Test
    fun `inaktiver Wochentag wird uebersprungen`() {
        // Nur Montag aktiv, Startpunkt Mittwoch -> naechster Montag.
        val r = reminder(daysOfWeek = setOf(DayOfWeek.MONDAY))
        val next = nextTriggerAfter(r, "2026-07-29T12:00:00")
        assertEquals(LocalDateTime.parse("2026-08-03T09:00"), next)
    }

    @Test
    fun `einzelner Wochentag wird auch ueber die Wochengrenze gefunden`() {
        // Der 8-Tage-Suchpuffer in nextTrigger() garantiert einen Treffer selbst dann, wenn nur
        // ein einziger Wochentag aktiv ist und man direkt nach dessen Fenster startet.
        val r = reminder(daysOfWeek = setOf(DayOfWeek.SUNDAY))
        val next = nextTriggerAfter(r, "2026-08-02T19:00:00") // Sonntag nach Fensterende
        assertEquals(LocalDateTime.parse("2026-08-09T09:00"), next)
    }

    // ---- Ungueltige Konfigurationen ----

    @Test
    fun `ohne aktive Wochentage gibt es keinen Trigger`() {
        assertNull(ReminderScheduler.nextTrigger(reminder(daysOfWeek = emptySet()), at("2026-07-29T10:00:00"), zone))
    }

    @Test
    fun `ohne Intervall gibt es keinen Trigger`() {
        // Intervall 0 wuerde in der Slot-Schleife sonst zu einer Endlosschleife fuehren
        // (offsetMinutes bliebe konstant 0 und erreichte nie das Fensterende).
        assertNull(ReminderScheduler.nextTrigger(reminder(intervalMinutes = 0), at("2026-07-29T10:00:00"), zone))
    }

    // ---- Zeitumstellung ----

    @Test
    fun `Sommerzeit-Ende dupliziert keinen Slot`() {
        // In der Nacht auf den 25.10.2026 wird in Europe/Berlin 03:00 auf 02:00 zurueckgestellt,
        // die Stunde 02:00-03:00 gibt es also zweimal. Erwartung: nextTrigger liefert trotzdem
        // einen echt spaeteren Zeitpunkt als den uebergebenen - sonst haengt die Alarmkette.
        val r = reminder(startMinuteOfDay = 0, endMinuteOfDay = 23 * 60 + 59, intervalMinutes = 60)
        val after = at("2026-10-25T02:30:00")
        val next = ReminderScheduler.nextTrigger(r, after, zone)
        requireNotNull(next)
        assert(next > after) { "nextTrigger muss echt nach dem uebergebenen Zeitpunkt liegen" }
    }
}
