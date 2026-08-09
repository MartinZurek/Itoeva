package com.notime.glyphcore.reminder

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.DaysOfWeekMask
import com.notime.glyphcore.data.GlyphReminder
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Zeitzonen und Zeitumstellung in [ReminderScheduler.nextTrigger].
 *
 * ## Warum getrennt von [ReminderSchedulerNextTriggerTest]
 *
 * Dort geht es um die Fachlogik - Fenster, Wochentage, Intervalle - und zwar durchgehend in
 * Europe/Berlin. Hier geht es ausschliesslich um die Zeitrechnung darunter, und die hat eigene
 * Fallen: eine Stunde, die es an einem Tag im Jahr nicht gibt, eine andere, die es zweimal gibt,
 * Zonen mit halben Stunden Versatz, und Zonen, in denen die Umstellung um Mitternacht liegt.
 *
 * **Was diese Tests leisten sollen.** Ein Fehler hier fuehrt zu keinem Absturz, sondern dazu, dass
 * an zwei Tagen im Jahr Erinnerungen ausfallen, doppelt kommen oder die Alarmkette stehen bleibt -
 * und zwar so selten, dass es niemand reproduziert, wenn es auffaellt. Die Tests halten deshalb
 * bewusst das tatsaechliche Verhalten fest, auch wo es ueberrascht; wo es fragwuerdig ist, steht
 * das im Kommentar statt in einer stillen Erwartung.
 *
 * ## Die entscheidende Eigenschaft
 *
 * `nextTrigger` muss **immer echt spaeter** liefern als den uebergebenen Zeitpunkt. Die Alarmkette
 * traegt sich selbst weiter: jeder feuernde Alarm plant den naechsten. Liefert die Funktion einmal
 * denselben oder einen frueheren Zeitpunkt, feuert die Kette entweder sofort wieder in einer
 * Schleife oder bleibt endgueltig stehen. Diese Eigenschaft wird deshalb in jedem Test hier
 * mitgeprueft, nicht nur der konkrete Wert.
 */
class ReminderSchedulerTimeZoneTest {

    private fun reminder(
        daysOfWeek: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
        startMinuteOfDay: Int = 0,
        endMinuteOfDay: Int = 23 * 60 + 59,
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

    private fun at(text: String, zone: ZoneId): Long =
        LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    private fun local(epochMillis: Long, zone: ZoneId): LocalDateTime =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zone)

    /**
     * Ruft [ReminderScheduler.nextTrigger] auf und prueft dabei die Grundeigenschaft mit: das
     * Ergebnis muss echt nach dem Ausgangszeitpunkt liegen.
     */
    private fun naechster(reminder: GlyphReminder, nach: Long, zone: ZoneId): Long {
        val next = ReminderScheduler.nextTrigger(reminder, nach, zone)
        assertNotNull("nextTrigger lieferte gar keinen Zeitpunkt", next)
        assertTrue(
            "nextTrigger muss echt spaeter liegen: ${local(next!!, zone)} nach ${local(nach, zone)}",
            next > nach
        )
        return next
    }

    /** Spielt die Kette [anzahl] Schritte weiter, wie es der feuernde Alarm tut. */
    private fun kette(reminder: GlyphReminder, start: Long, zone: ZoneId, anzahl: Int): List<Long> {
        val ergebnis = mutableListOf<Long>()
        var jetzt = start
        repeat(anzahl) {
            jetzt = naechster(reminder, jetzt, zone)
            ergebnis += jetzt
        }
        return ergebnis
    }

    // --- Sommerzeit-Beginn: eine Stunde faellt aus ------------------------------------------

    /**
     * In Europe/Berlin springt die Uhr in der Nacht auf den 29.03.2026 von 02:00 auf 03:00.
     * Die lokale Zeit 02:30 existiert an diesem Tag **nicht**.
     *
     * Geprueft wird, dass die Kette ueber die Luecke hinweg laeuft, ohne stehen zu bleiben oder
     * einen Zeitpunkt zweimal zu liefern - und dass kein gelieferter Zeitpunkt in der
     * ausgefallenen Stunde liegt (er koennte es gar nicht, aber ein Rechenfehler koennte einen
     * Zeitstempel erzeugen, der lokal dorthin faellt).
     */
    @Test
    fun `Sommerzeit-Beginn - die Kette laeuft ueber die ausgefallene Stunde`() {
        val zone = ZoneId.of("Europe/Berlin")
        val r = reminder(intervalMinutes = 60)
        val start = at("2026-03-29T00:30:00", zone)

        val slots = kette(r, start, zone, anzahl = 5)

        assertEquals("Jeder Schritt muss echt spaeter liegen", slots.sorted(), slots)
        assertEquals("Kein Zeitpunkt darf doppelt vorkommen", slots.distinct().size, slots.size)
        slots.forEach {
            val lokal = local(it, zone)
            assertTrue(
                "Zeitpunkt liegt in der ausgefallenen Stunde: $lokal",
                !(lokal.hour == 2 && lokal.toLocalDate().dayOfMonth == 29 && lokal.monthValue == 3)
            )
        }
    }

    /**
     * Haelt fest, wie die Slots an diesem Tag tatsaechlich liegen.
     *
     * `ZonedDateTime.plusMinutes` rechnet auf der **Instant**-Zeitachse (anders als `plusDays`,
     * das lokal rechnet). Eine Stunde bleibt damit immer eine echte Stunde - die lokale Uhrzeit
     * springt an der Luecke also mit. Fuer einen Nutzer heisst das: an diesem einen Tag
     * verschieben sich die Slots nach der Umstellung um eine Stunde gegenueber der gewohnten
     * Uhrzeit. Das ist die Folge davon, dass Abstaende in echter Zeit eingehalten werden - was
     * fuer eine Erinnerung ("alle 60 Minuten") die richtige Auslegung ist.
     */
    @Test
    fun `Sommerzeit-Beginn - Abstaende bleiben echte Stunden`() {
        val zone = ZoneId.of("Europe/Berlin")
        val r = reminder(intervalMinutes = 60)
        val start = at("2026-03-29T00:30:00", zone)

        val slots = kette(r, start, zone, anzahl = 3)

        slots.zipWithNext().forEach { (a, b) ->
            assertEquals(
                "Der Abstand muss in echter Zeit 60 Minuten betragen",
                60 * 60 * 1000L,
                b - a
            )
        }
    }

    // --- Sommerzeit-Ende: eine Stunde kommt doppelt ------------------------------------------

    /**
     * In der Nacht auf den 25.10.2026 wird 03:00 auf 02:00 zurueckgestellt - die Stunde
     * 02:00-03:00 gibt es zweimal.
     *
     * Der gefaehrlichste Fall der beiden: liefert die Funktion hier denselben Zeitstempel erneut,
     * feuert die Alarmkette in einer Schleife. [naechster] prueft das bei jedem Schritt mit.
     */
    @Test
    fun `Sommerzeit-Ende - kein Zeitpunkt wiederholt sich`() {
        val zone = ZoneId.of("Europe/Berlin")
        val r = reminder(intervalMinutes = 30)
        val start = at("2026-10-25T01:15:00", zone)

        val slots = kette(r, start, zone, anzahl = 8)

        assertEquals("Kein Zeitpunkt darf doppelt vorkommen", slots.distinct().size, slots.size)
        assertEquals("Die Reihenfolge muss streng steigend sein", slots.sorted(), slots)
    }

    /**
     * **Ein gefundener Schoenheitsfehler, hier festgehalten statt behoben.**
     *
     * Am Tag der Rueckstellung hat der Tag 25 echte Stunden. Die Fensterlaenge rechnet
     * `nextTrigger` aber aus `endMinuteOfDay - startMinuteOfDay`, also in **Wanduhr**-Minuten
     * (1439), waehrend die Slots auf der **Instant**-Zeitachse vorruecken. Nach 1439 echten
     * Minuten ist es lokal erst 22:59 - die letzte Stunde des Tages bekommt keinen Slot mehr.
     *
     * Gemessen bei einem Ganztagsfenster mit Stundenintervall:
     *
     * | Tag | Slots | letzter |
     * |---|---|---|
     * | normaler Tag | 24 | 23:00 |
     * | Umstellung vor (23 Stunden) | 23 | 23:00 |
     * | Umstellung zurueck (25 Stunden) | 24 | **22:00** |
     *
     * Die Auswirkung ist klein: an einem Tag im Jahr faellt zwischen 23:00 und Mitternacht eine
     * Erinnerung aus. Nichts stuerzt ab, die Kette bleibt intakt, und am Folgetag ist alles wie
     * gewohnt. Behoben waere es, indem das Fensterende lokal statt ueber die Dauer bestimmt wird -
     * das aendert allerdings die Bedeutung von `nextTrigger` fuer alle Faelle und ist damit eine
     * Entscheidung ueber das Verhalten, keine ueber Tests. Solange sie nicht getroffen ist, haelt
     * dieser Test den Ist-Zustand fest, damit die Eigenart nicht unbemerkt verschwindet oder sich
     * verschlimmert.
     */
    @Test
    fun `am Tag der Rueckstellung fehlt der letzte Slot des Tages`() {
        val zone = ZoneId.of("Europe/Berlin")
        val r = reminder(intervalMinutes = 60)

        fun slotsAmTag(tag: String): List<LocalDateTime> {
            val beginn = LocalDateTime.parse("${tag}T00:00:00").atZone(zone).toInstant().toEpochMilli()
            val ende = LocalDateTime.parse("${tag}T00:00:00").plusDays(1).atZone(zone).toInstant().toEpochMilli()
            val ergebnis = mutableListOf<LocalDateTime>()
            var jetzt = beginn - 1
            while (true) {
                val n = ReminderScheduler.nextTrigger(r, jetzt, zone) ?: break
                if (n >= ende) break
                ergebnis += local(n, zone)
                jetzt = n
            }
            return ergebnis
        }

        val normal = slotsAmTag("2026-06-15")
        assertEquals(24, normal.size)
        assertEquals(23, normal.last().hour)

        // Vorstellung: der Tag hat 23 Stunden, entsprechend ein Slot weniger - hier stimmt alles.
        val vor = slotsAmTag("2026-03-29")
        assertEquals(23, vor.size)
        assertEquals(23, vor.last().hour)

        // Rueckstellung: der Tag haette 25 Stunden, es bleibt aber bei 24 Slots, und der letzte
        // liegt um 22:00 statt um 23:00.
        val zurueck = slotsAmTag("2026-10-25")
        assertEquals(24, zurueck.size)
        assertEquals(
            "Wenn hier 23 steht, wurde die Fensterlaenge auf lokale Zeit umgestellt - dann " +
                "gehoert dieser Test angepasst und die Notiz darueber geloescht.",
            22,
            zurueck.last().hour
        )
    }

    // --- Zonen mit ungewoehnlichem Versatz ---------------------------------------------------

    /**
     * Asia/Kolkata liegt bei UTC+05:30 - ein halbstuendiger Versatz. Zonen mit vollen Stunden
     * verdecken Rechenfehler, die hier auffallen: ein Slot um 09:00 lokal darf nicht auf
     * 09:30 oder 08:30 rutschen.
     */
    @Test
    fun `halbstuendiger Zonenversatz verschiebt die lokale Uhrzeit nicht`() {
        val zone = ZoneId.of("Asia/Kolkata")
        val r = reminder(startMinuteOfDay = 9 * 60, endMinuteOfDay = 18 * 60, intervalMinutes = 60)

        val next = naechster(r, at("2026-06-15T08:00:00", zone), zone)

        assertEquals(LocalDateTime.parse("2026-06-15T09:00:00"), local(next, zone))
    }

    /** Australia/Eucla liegt bei UTC+08:45 - der schraegste bewohnte Versatz ueberhaupt. */
    @Test
    fun `dreiviertelstuendiger Zonenversatz verschiebt die lokale Uhrzeit nicht`() {
        val zone = ZoneId.of("Australia/Eucla")
        val r = reminder(startMinuteOfDay = 7 * 60 + 15, endMinuteOfDay = 20 * 60, intervalMinutes = 90)

        val next = naechster(r, at("2026-06-15T06:00:00", zone), zone)

        assertEquals(LocalDateTime.parse("2026-06-15T07:15:00"), local(next, zone))
    }

    /**
     * Suedhalbkugel: in Pacific/Auckland liegt die Umstellung im September bzw. April, also
     * gegenlaeufig zu Europa. Ein Test, der nur Europe/Berlin kennt, wuerde eine Annahme ueber
     * die Richtung der Umstellung nie bemerken.
     */
    @Test
    fun `Umstellung auf der Suedhalbkugel laeuft ebenso durch`() {
        val zone = ZoneId.of("Pacific/Auckland")
        val r = reminder(intervalMinutes = 60)

        // 27.09.2026: 02:00 springt auf 03:00.
        val slots = kette(r, at("2026-09-27T00:30:00", zone), zone, anzahl = 5)

        assertEquals(slots.distinct().size, slots.size)
        assertEquals(slots.sorted(), slots)
    }

    /**
     * In America/Santiago faellt die Umstellung auf **Mitternacht**: der 06.09.2026 beginnt dort
     * nicht um 00:00, diese lokale Zeit existiert nicht.
     *
     * Genau darauf laeuft `date.atStartOfDay(zone)` in `nextTrigger` - deshalb ist dieser Fall
     * kein Kuriosum, sondern der einzige, der die Funktion an ihrer eigenen Grundannahme trifft
     * ("ein Tag beginnt um Mitternacht"). `atStartOfDay` liefert dort den ersten gueltigen
     * Zeitpunkt; ein Fenster ab 00:00 beginnt an diesem Tag also um 01:00 statt um 00:00.
     */
    @Test
    fun `Tag ohne Mitternacht bringt die Berechnung nicht aus dem Tritt`() {
        val zone = ZoneId.of("America/Santiago")
        val r = reminder(startMinuteOfDay = 0, endMinuteOfDay = 23 * 60 + 59, intervalMinutes = 60)

        val slots = kette(r, at("2026-09-05T23:00:00", zone), zone, anzahl = 4)

        assertEquals(slots.distinct().size, slots.size)
        assertEquals(slots.sorted(), slots)
        slots.forEach {
            val lokal = local(it, zone)
            assertTrue("Ein Zeitpunkt landete in der ausgefallenen Stunde: $lokal", lokal.hour != 0 || lokal.dayOfMonth != 6)
        }
    }

    // --- UTC als Gegenprobe -------------------------------------------------------------------

    /**
     * UTC kennt keine Umstellung. Dieselbe Erinnerung muss dort das schlichteste denkbare
     * Ergebnis liefern - die Gegenprobe dazu, dass die Faelle oben wirklich an der Zeitzone
     * liegen und nicht an der Fachlogik.
     */
    @Test
    fun `ohne Umstellung liegen die Slots exakt auf dem Raster`() {
        val zone = ZoneId.of("UTC")
        val r = reminder(startMinuteOfDay = 9 * 60, endMinuteOfDay = 17 * 60, intervalMinutes = 120)

        val slots = kette(r, at("2026-03-29T08:00:00", zone), zone, anzahl = 4)
            .map { local(it, zone) }

        assertEquals(
            listOf(
                LocalDateTime.parse("2026-03-29T09:00:00"),
                LocalDateTime.parse("2026-03-29T11:00:00"),
                LocalDateTime.parse("2026-03-29T13:00:00"),
                LocalDateTime.parse("2026-03-29T15:00:00")
            ),
            slots
        )
    }

    // --- Fenster ueber Mitternacht in Verbindung mit der Umstellung ---------------------------

    /**
     * Der zusammengesetzte Fall: ein Nachtfenster (22:00-06:00) genau in der Nacht der
     * Umstellung. Beide Sonderfaelle greifen gleichzeitig ineinander.
     */
    @Test
    fun `Nachtfenster ueber die Umstellung hinweg bleibt eine steigende Kette`() {
        val zone = ZoneId.of("Europe/Berlin")
        val r = reminder(startMinuteOfDay = 22 * 60, endMinuteOfDay = 6 * 60, intervalMinutes = 60)

        val slots = kette(r, at("2026-10-24T21:00:00", zone), zone, anzahl = 8)

        assertEquals("Die Reihenfolge muss streng steigend sein", slots.sorted(), slots)
        assertEquals("Kein Zeitpunkt darf doppelt vorkommen", slots.distinct().size, slots.size)
    }
}
