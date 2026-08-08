package com.notime.glyphcore.reminder

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.DaysOfWeekMask
import com.notime.glyphcore.data.GlyphReminder
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests fuer die Abriss-Erkennung der Alarmkette (siehe [ReminderWatchdog]).
 *
 * Der Watchdog ist die letzte Instanz, die einen stillen Ausfall noch bemerkt - ein Fehler
 * ausgerechnet hier wuerde also nicht auffallen, sondern dazu fuehren, dass Erinnerungen
 * unbemerkt ausbleiben. Deshalb sind beide Richtungen abgedeckt: nichts uebersehen (sonst bleibt
 * der Abriss bestehen) und nichts faelschlich melden (sonst wird bei jedem Lauf grundlos neu
 * geplant).
 */
class ReminderWatchdogTest {

    private val now = 1_700_000_000_000L

    private fun reminder(
        id: Long = 1L,
        enabled: Boolean = true,
        nextTriggerEpochMillis: Long? = null
    ) = GlyphReminder(
        id = id,
        label = "Test $id",
        animationType = AnimationType.GENERAL,
        daysOfWeekMask = DaysOfWeekMask.toMask(DayOfWeek.entries.toSet()),
        startMinuteOfDay = 0,
        endMinuteOfDay = 23 * 60 + 59,
        intervalMinutes = 60,
        enabled = enabled,
        nextTriggerEpochMillis = nextTriggerEpochMillis
    )

    // ---- Was als abgerissen gilt ----

    @Test
    fun `Erinnerung ohne geplanten Alarm gilt als abgerissen`() {
        // Der Zustand nach einem App-Update oder Force-Stop: Android hat die Alarme verworfen,
        // in der Datenbank steht kein Zeitpunkt mehr.
        val stale = ReminderWatchdog.findStale(listOf(reminder(nextTriggerEpochMillis = null)), now)
        assertEquals(1, stale.size)
    }

    @Test
    fun `deutlich ueberfaelliger Alarm gilt als abgerissen`() {
        // Haette vor einer Stunde feuern und dabei den naechsten planen muessen - hat er nicht.
        val overdue = now - 60 * 60 * 1000L
        val stale = ReminderWatchdog.findStale(listOf(reminder(nextTriggerEpochMillis = overdue)), now)
        assertEquals(1, stale.size)
    }

    // ---- Was NICHT als abgerissen gelten darf ----

    @Test
    fun `zukuenftiger Alarm ist gesund`() {
        val future = now + 60 * 60 * 1000L
        val stale = ReminderWatchdog.findStale(listOf(reminder(nextTriggerEpochMillis = future)), now)
        assertTrue("Ein geplanter Alarm in der Zukunft darf nicht angefasst werden", stale.isEmpty())
    }

    @Test
    fun `gerade erst faelliger Alarm bleibt in der Kulanzzeit unangetastet`() {
        // Genau der Moment, in dem der Alarm feuert und sein Nachfolger noch nicht geschrieben
        // ist - ohne Kulanz wuerde der Watchdog hier bei jedem Lauf dazwischenfunken.
        val justFired = now - 1000L
        val stale = ReminderWatchdog.findStale(listOf(reminder(nextTriggerEpochMillis = justFired)), now)
        assertTrue("Innerhalb der Kulanzzeit darf nichts nachgeplant werden", stale.isEmpty())
    }

    @Test
    fun `Alarm exakt am Rand der Kulanzzeit gilt noch als gesund`() {
        val atEdge = now - ReminderWatchdog.OVERDUE_GRACE_MILLIS
        val stale = ReminderWatchdog.findStale(listOf(reminder(nextTriggerEpochMillis = atEdge)), now)
        assertTrue("Genau auf der Grenze noch nicht eingreifen", stale.isEmpty())
    }

    @Test
    fun `deaktivierte Erinnerung ohne Alarm ist kein Fehler`() {
        // Fuer eine ausgeschaltete Erinnerung IST "kein Alarm" der richtige Zustand - wuerde der
        // Watchdog sie einplanen, wuerde er eine bewusst abgeschaltete Erinnerung wiederbeleben.
        val stale = ReminderWatchdog.findStale(
            listOf(reminder(enabled = false, nextTriggerEpochMillis = null)),
            now
        )
        assertTrue("Deaktivierte Erinnerungen duerfen nie neu geplant werden", stale.isEmpty())
    }

    @Test
    fun `deaktivierte Erinnerung mit altem Alarm bleibt ebenfalls unangetastet`() {
        val stale = ReminderWatchdog.findStale(
            listOf(reminder(enabled = false, nextTriggerEpochMillis = now - 24 * 60 * 60 * 1000L)),
            now
        )
        assertTrue(stale.isEmpty())
    }

    // ---- Gemischte Liste ----

    @Test
    fun `findet aus einer gemischten Liste genau die abgerissenen`() {
        val reminders = listOf(
            reminder(id = 1, nextTriggerEpochMillis = now + 60_000L),                  // gesund
            reminder(id = 2, nextTriggerEpochMillis = null),                           // abgerissen
            reminder(id = 3, nextTriggerEpochMillis = now - 24 * 60 * 60 * 1000L),     // abgerissen
            reminder(id = 4, enabled = false, nextTriggerEpochMillis = null),          // aus, egal
            reminder(id = 5, nextTriggerEpochMillis = now - 1000L)                     // gerade gefeuert
        )
        val staleIds = ReminderWatchdog.findStale(reminders, now).map { it.id }
        assertEquals(listOf(2L, 3L), staleIds)
    }

    @Test
    fun `leere Liste liefert leeres Ergebnis`() {
        assertTrue(ReminderWatchdog.findStale(emptyList(), now).isEmpty())
    }
}
