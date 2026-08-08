package com.notime.glyphsim.ui

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests fuer die Zeitraum-Grenzen der Fuetter-Statistik ([FeedStatsPeriod]).
 *
 * Kalenderarithmetik faellt still daneben: eine um einen Tag verschobene Wochengrenze liefert
 * einfach eine etwas andere Zahl, ohne dass irgendetwas abstuerzt - auffallen wuerde das
 * hoechstens jemandem, der von Hand nachzaehlt. Genau deshalb sind hier die Grenzfaelle
 * festgehalten: Wochenanfang, Monatsanfang und der Sonntag, an dem sich beide Lesarten
 * ("letzte 7 Tage" vs. "seit Montag") am staerksten unterscheiden.
 */
class FeedStatsPeriodTest {

    private val zone = ZoneId.of("Europe/Berlin")

    private fun millisAt(text: String): Long =
        LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    private fun startOf(period: FeedStatsPeriod, date: String): Long =
        period.startMillis(LocalDate.parse(date), zone)

    @Test
    fun `heute beginnt um Mitternacht desselben Tages`() {
        assertEquals(millisAt("2026-07-30T00:00"), startOf(FeedStatsPeriod.TODAY, "2026-07-30"))
    }

    @Test
    fun `Woche beginnt am Montag`() {
        // 30.07.2026 ist ein Donnerstag -> Wochenanfang ist Montag, der 27.
        assertEquals(millisAt("2026-07-27T00:00"), startOf(FeedStatsPeriod.WEEK, "2026-07-30"))
    }

    @Test
    fun `am Montag selbst beginnt die Woche heute`() {
        assertEquals(millisAt("2026-07-27T00:00"), startOf(FeedStatsPeriod.WEEK, "2026-07-27"))
    }

    @Test
    fun `am Sonntag zaehlt noch die laufende Woche ab Montag`() {
        // Der Fall, in dem ein gleitendes 7-Tage-Fenster am deutlichsten abweichen wuerde:
        // Sonntag, 02.08. gehoert noch zur Woche ab Montag, 27.07.
        assertEquals(millisAt("2026-07-27T00:00"), startOf(FeedStatsPeriod.WEEK, "2026-08-02"))
    }

    @Test
    fun `Monat beginnt am Ersten`() {
        assertEquals(millisAt("2026-07-01T00:00"), startOf(FeedStatsPeriod.MONTH, "2026-07-30"))
    }

    @Test
    fun `Woche darf ueber den Monatswechsel hinausreichen`() {
        // Mittwoch, 01.07.2026 - die Woche begann bereits im Juni (Montag, 29.06.).
        assertEquals(millisAt("2026-06-29T00:00"), startOf(FeedStatsPeriod.WEEK, "2026-07-01"))
    }

    @Test
    fun `Woche darf ueber den Jahreswechsel hinausreichen`() {
        // Freitag, 01.01.2027 - Wochenanfang ist Montag, der 28.12.2026.
        assertEquals(millisAt("2026-12-28T00:00"), startOf(FeedStatsPeriod.WEEK, "2027-01-01"))
    }

    @Test
    fun `seit Beginn umfasst alles`() {
        assertEquals(0L, startOf(FeedStatsPeriod.ALL_TIME, "2026-07-30"))
    }

    @Test
    fun `jeder Zeitraum liegt nicht nach dem naechstkuerzeren`() {
        // Grundregel der Staffelung: je laenger der Zeitraum, desto frueher sein Beginn -
        // sonst koennte "diese Woche" weniger zaehlen als "heute".
        val date = LocalDate.parse("2026-07-30")
        val today = FeedStatsPeriod.TODAY.startMillis(date, zone)
        val week = FeedStatsPeriod.WEEK.startMillis(date, zone)
        val month = FeedStatsPeriod.MONTH.startMillis(date, zone)
        val allTime = FeedStatsPeriod.ALL_TIME.startMillis(date, zone)
        assertEquals(true, week <= today)
        assertEquals(true, month <= week)
        assertEquals(true, allTime <= month)
    }

    @Test
    fun `Zeitzone wird beruecksichtigt`() {
        // Dieselbe Kalenderdatum-Angabe ergibt in einer anderen Zone einen anderen Zeitpunkt -
        // wichtig, weil die Ereignisse als absolute Epoch-Millis gespeichert sind.
        val berlin = FeedStatsPeriod.TODAY.startMillis(LocalDate.parse("2026-07-30"), zone)
        val tokyo = FeedStatsPeriod.TODAY.startMillis(
            LocalDate.parse("2026-07-30"),
            ZoneId.of("Asia/Tokyo")
        )
        assertEquals(true, tokyo < berlin)
        assertEquals(
            LocalDateTime.parse("2026-07-30T00:00"),
            LocalDateTime.ofInstant(Instant.ofEpochMilli(berlin), zone)
        )
    }
}
