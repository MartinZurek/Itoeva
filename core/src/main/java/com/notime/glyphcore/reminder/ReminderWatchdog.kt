package com.notime.glyphcore.reminder

import com.notime.glyphcore.data.GlyphReminder

/**
 * Erkennt Erinnerungen, deren Alarmkette abgerissen ist.
 *
 * **Das Problem:** jede Erinnerung traegt sich selbst weiter - der feuernde Alarm plant in
 * [ReminderAlarmReceiver-Aequivalenten der Apps] sofort den naechsten. Faellt ein einziges Glied
 * aus, steht die Kette dauerhaft still, ohne dass irgendetwas davon Notiz nimmt. Ausloeser dafuer
 * gibt es im Alltag reichlich: ein App-Update (Android verwirft dabei alle Alarme), ein
 * Force-Stop, aggressive Akkuverwaltung des Herstellers, eine nachtraeglich entzogene
 * Exact-Alarm-Berechtigung (dann kehrt [ReminderScheduler.schedule] wirkungslos zurueck).
 *
 * Die bisherigen Sicherheitsnetze - Reboot und "App wird geoeffnet" - greifen ausgerechnet hier
 * nicht: die App ist darauf ausgelegt, NICHT geoeffnet zu werden (Widget, Dock-Modus). Eine
 * Erinnerungs-App, die still aufhoert zu erinnern, ist schlimmer als gar keine, weil man sich auf
 * sie verlaesst. [ReminderWatchdogWorker] ruft [findStale] deshalb regelmaessig auf und plant die
 * gefundenen Erinnerungen neu.
 *
 * Bewusst reine Logik ohne Android-Abhaengigkeit, damit sie als schneller JVM-Unit-Test
 * abgesichert ist (siehe ReminderWatchdogTest).
 */
object ReminderWatchdog {

    /**
     * Kulanz, bevor ein ueberfaelliger Alarm als abgerissen gilt.
     *
     * Ohne sie wuerde der Watchdog bei jedem Lauf gegen zwei voellig normale Zustaende anrennen:
     * ein Alarm, der genau jetzt feuert und dessen Nachfolger noch nicht geschrieben ist, sowie
     * die uebliche Ungenauigkeit von Doze-verzoegerten Alarmen. Fuenf Minuten liegen deutlich
     * ueber beidem und immer noch weit unter dem kuerzesten Intervall, bei dem ein echter
     * Abriss ueberhaupt auffiele.
     */
    const val OVERDUE_GRACE_MILLIS = 5 * 60 * 1000L

    /**
     * Liefert die Erinnerungen, fuer die neu geplant werden muss:
     *
     * - **kein Alarm hinterlegt** (`nextTriggerEpochMillis == null`) - typisch nach einem
     *   App-Update oder Force-Stop, bei dem die Alarme verworfen wurden
     * - **Alarm liegt deutlich in der Vergangenheit** - er haette laengst feuern und dabei den
     *   naechsten planen muessen; dass er es nicht getan hat, ist genau der Abriss
     *
     * Deaktivierte Erinnerungen bleiben aussen vor - fuer sie ist "kein Alarm" der korrekte
     * Zustand, kein Fehler.
     */
    fun findStale(
        reminders: List<GlyphReminder>,
        nowMillis: Long,
        graceMillis: Long = OVERDUE_GRACE_MILLIS
    ): List<GlyphReminder> = reminders.filter { reminder ->
        if (!reminder.enabled) return@filter false
        val trigger = reminder.nextTriggerEpochMillis ?: return@filter true
        trigger < nowMillis - graceMillis
    }
}
