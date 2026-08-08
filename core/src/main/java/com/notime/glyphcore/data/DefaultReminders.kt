package com.notime.glyphcore.data

import com.notime.glyphcore.BuildConfig
import java.time.DayOfWeek

/**
 * Voreingestellte Erinnerungen, die beim allerersten Start automatisch angelegt werden (siehe
 * GlyphReminderRepository.seedIfEmpty, aufgerufen aus GlyphReminderViewModel.init).
 *
 * **Diese Werte sind das, was ein neuer Nutzer als Erstes erlebt** - kein Entwicklungs-Werkzeug.
 * Hier stand frueher eine Erinnerung "Test (1 min)", die rund um die Uhr jede Minute feuerte,
 * dazu "Trink was" alle 5 und "Beweg dich" alle 10 Minuten, jeweils von 00:00 bis 23:59 an allen
 * Wochentagen. Gemeint war das als Testhilfe, ausgeliefert waere es eine App gewesen, die einen
 * ununterbrochen anspringt - auch nachts - und dabei den Eindruck erweckt, das eingestellte
 * Intervall werde nicht eingehalten (tatsaechlich feuerten schlicht drei Erinnerungen
 * gleichzeitig).
 *
 * Die Voreinstellungen sind deshalb jetzt so gewaehlt, wie man sie einem Menschen zumuten kann:
 * wache Stunden statt rund um die Uhr, und Abstaende, bei denen eine Erinnerung noch etwas
 * bedeutet. Wer kurze Intervalle zum Testen braucht, legt sie in wenigen Sekunden selbst an.
 */
object DefaultReminders {
    private val ALL_DAYS = DaysOfWeekMask.toMask(DayOfWeek.entries.toSet())
    private const val FULL_DAY_END_MINUTE = 23 * 60 + 59

    /**
     * Was ein Nutzer bekommt: wache Stunden statt rund um die Uhr, und Abstaende, bei denen eine
     * Erinnerung noch etwas bedeutet.
     */
    private fun releaseDefaults(): List<GlyphReminder> = listOf(
        GlyphReminder(
            label = "Trink was",
            animationType = AnimationType.DRINK,
            daysOfWeekMask = ALL_DAYS,
            startMinuteOfDay = 8 * 60,
            endMinuteOfDay = 20 * 60,
            intervalMinutes = 60
        ),
        GlyphReminder(
            label = "Beweg dich",
            animationType = AnimationType.MOVE,
            daysOfWeekMask = ALL_DAYS,
            startMinuteOfDay = 9 * 60,
            endMinuteOfDay = 18 * 60,
            intervalMinutes = 120
        )
    )

    /**
     * Kurze Intervalle rund um die Uhr - spart beim Entwickeln das wiederholte manuelle Anlegen
     * von Test-Erinnerungen. Nur im Debug-Build, siehe [ALL].
     */
    private fun debugDefaults(): List<GlyphReminder> = listOf(
        GlyphReminder(
            label = "Test (1 min)",
            animationType = AnimationType.GENERAL,
            daysOfWeekMask = ALL_DAYS,
            startMinuteOfDay = 0,
            endMinuteOfDay = FULL_DAY_END_MINUTE,
            intervalMinutes = 1
        ),
        GlyphReminder(
            label = "Test (5 min)",
            animationType = AnimationType.DRINK,
            daysOfWeekMask = ALL_DAYS,
            startMinuteOfDay = 0,
            endMinuteOfDay = FULL_DAY_END_MINUTE,
            intervalMinutes = 5
        ),
        GlyphReminder(
            label = "Test (10 min)",
            animationType = AnimationType.MOVE,
            daysOfWeekMask = ALL_DAYS,
            startMinuteOfDay = 0,
            endMinuteOfDay = FULL_DAY_END_MINUTE,
            intervalMinutes = 10
        )
    )

    /**
     * Am Build-Typ statt an einer Notiz "vor dem Release rausnehmen". Genau diese Sorte Aufgabe
     * geht beim Veroeffentlichen unter - und was hier steht, ist das Erste, was ein neuer Nutzer
     * erlebt: vorher waere das eine App gewesen, die ab Installation rund um die Uhr jede Minute
     * anspringt.
     *
     * Genau genommen wird ein Release-Build die Test-Erinnerungen nicht *anlegen*; ihre
     * Zeichenketten stehen weiterhin im Bytecode, solange R8 den toten Zweig nicht entfernt.
     * Das ist folgenlos - [ALL] liefert dort ausschliesslich [releaseDefaults]. Als Funktionen
     * statt als Eigenschaften, damit der ungenutzte Satz nicht einmal gebaut wird.
     */
    val ALL: List<GlyphReminder>
        get() = if (BuildConfig.DEBUG) debugDefaults() else releaseDefaults()
}
