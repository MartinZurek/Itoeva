package com.notime.glyphsim.ui

import androidx.annotation.StringRes
import com.notime.glyphsim.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Zeitraeume der Fuetter-Statistik.
 *
 * Bewusst an KALENDERgrenzen ausgerichtet und nicht an "letzte 24 Stunden / 7 Tage": gefragt ist
 * "wie oft heute", und das endet um Mitternacht, nicht 24 Stunden nach der letzten Fuetterung.
 * Ein gleitendes Fenster haette zur Folge, dass eine Zahl ueber Nacht scheinbar von selbst
 * sinkt, waehrend der Tag noch laeuft.
 *
 * [ALL_TIME] beginnt bei 0 (Epoch) und umfasst damit alles, was gespeichert ist. Ein
 * Zuruecksetzen loescht die Zeilen des Avatars (siehe AvatarFeedEventDao.deleteForProfile),
 * wodurch "seit Beginn" automatisch ab dem Zuruecksetzen zaehlt - ohne dass dafuer ein
 * zusaetzlicher Zeitstempel gepflegt werden muesste.
 */
enum class FeedStatsPeriod(@StringRes val labelRes: Int) {
    TODAY(R.string.stats_period_today),
    WEEK(R.string.stats_period_week),
    MONTH(R.string.stats_period_month),
    ALL_TIME(R.string.stats_period_all);

    /**
     * Beginn des Zeitraums als Epoch-Millis, bezogen auf [today] in der Zone [zone].
     * Beides ist parametrisiert, damit sich die Grenzfaelle (Monatsanfang, Wochenanfang am
     * Montag, Jahreswechsel) ohne Warten auf echte Kalendertage testen lassen.
     */
    fun startMillis(today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): Long {
        val startDate = when (this) {
            TODAY -> today
            // Montag als Wochenanfang (ISO-8601) - in Deutschland die uebliche Lesart von
            // "diese Woche"; DayOfWeek.MONDAY ist zugleich Kotlins/Javas erster Wochentag.
            WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            MONTH -> today.withDayOfMonth(1)
            ALL_TIME -> return 0L
        }
        return startDate.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
