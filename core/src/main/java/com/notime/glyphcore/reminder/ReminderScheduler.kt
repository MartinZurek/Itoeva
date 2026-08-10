package com.notime.glyphcore.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.notime.glyphcore.data.DaysOfWeekMask
import com.notime.glyphcore.data.GlyphReminder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Plant/entfernt die exakten Alarme fuer eine [GlyphReminder]. Logik 1:1 uebernommen
 * aus reminder/ReminderScheduler.kt im :app-Modul - einziger Unterschied: [minSdk]
 * ist hier 26 statt 34, `AlarmManager.canScheduleExactAlarms()` gibt es aber erst ab
 * API 31 (vorher war die Berechtigung implizit vorhanden, keine Nutzerfreigabe noetig).
 */
object ReminderScheduler {

    /**
     * Prueft die Freigabe fuer exakte Alarme.
     *
     * **Warum `SCHEDULE_EXACT_ALARM` und nicht `USE_EXACT_ALARM`:** die zweite Berechtigung wird
     * bei der Installation automatisch erteilt und laesst sich nicht entziehen - technisch klar
     * die bessere Wahl. Google Play laesst sie aber nur fuer Apps zu, deren *Kernfunktion* ein
     * Wecker oder Kalender ist, und blockiert andernfalls die Veroeffentlichung komplett. Diese
     * App ist ein Grenzfall (Erinnerungen ja, aber ambient-visuell statt weckend), und das Risiko
     * ist einseitig: falsch eingeschaetzt heisst nicht "Berechtigung fehlt", sondern "App laesst
     * sich gar nicht veroeffentlichen". Deshalb bleibt es bei der widerrufbaren
     * `SCHEDULE_EXACT_ALARM` - und genau deswegen muss der Entzug sauber abgefangen werden,
     * siehe [setAlarm].
     */
    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun nextTrigger(
        reminder: GlyphReminder,
        afterEpochMillis: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long? {
        if (reminder.intervalMinutes <= 0 || reminder.daysOfWeekMask == 0) return null

        val windowLengthMinutes = (reminder.endMinuteOfDay - reminder.startMinuteOfDay).let {
            if (it <= 0) it + MINUTES_PER_DAY else it
        }
        val referenceDate = Instant.ofEpochMilli(afterEpochMillis).atZone(zone).toLocalDate()

        var best: Long? = null
        for (dayOffset in -1..8) {
            val date = referenceDate.plusDays(dayOffset.toLong())
            if (!DaysOfWeekMask.contains(reminder.daysOfWeekMask, date.dayOfWeek)) continue

            val windowStart = date.atStartOfDay(zone).plusMinutes(reminder.startMinuteOfDay.toLong())
            var k = 0L
            while (true) {
                val offsetMinutes = k * reminder.intervalMinutes
                if (offsetMinutes > windowLengthMinutes) break
                val slotMillis = windowStart.plusMinutes(offsetMinutes).toInstant().toEpochMilli()
                if (slotMillis > afterEpochMillis) {
                    if (best == null || slotMillis < best!!) best = slotMillis
                    break
                }
                k++
            }
        }
        return best
    }

    suspend fun schedule(context: Context, reminder: GlyphReminder) {
        val dao = ReminderHost.dao(context)
        cancel(context, reminder)

        // Erinnerungen gehoeren zu genau einem Profil - die eines gerade nicht aktiven Profils
        // duerfen nicht feuern (siehe GlyphReminder.profileId).
        val activeProfile = ActiveProfilePrefs.get(context)
        if (reminder.profileId != activeProfile) {
            Log.d(
                TAG,
                "Kein Alarm fuer \"${reminder.label}\" (id=${reminder.id}): gehoert zu Profil " +
                    "${reminder.profileId}, aktiv ist $activeProfile"
            )
            return
        }

        if (!reminder.enabled) {
            Log.d(TAG, "Kein Alarm fuer \"${reminder.label}\" (id=${reminder.id}): deaktiviert")
            return
        }

        // Die vom Wesen selbst gewuerfelte Zeile kommt nur, wenn seine Welt laeuft. Die eigenen
        // Routinen laufen immer - seit Phase 3, siehe [PlayModeState.matchesCurrentMode].
        if (!PlayModeState.matchesCurrentMode(context, reminder)) {
            Log.d(
                TAG,
                "Kein Alarm fuer \"${reminder.label}\" (id=${reminder.id}): Spiel-Zeile, " +
                    "aber die Welt ruht"
            )
            return
        }

        // Ausdrueckliche Pause des Nutzers (siehe [ReminderPause]). Sie gilt fuer alles - auch
        // fuer die Zeile des Wesens: wer Ruhe wollte, meinte Ruhe.
        //
        // Bewusst kein eigener Wecker fuers Ende der Pause: der Watchdog sucht ohnehin nach
        // Erinnerungen ohne Alarm und plant sie beim naechsten Lauf wieder ein.
        ReminderPause.pausedUntil(context)?.let { until ->
            Log.d(
                TAG,
                "Kein Alarm fuer \"${reminder.label}\" (id=${reminder.id}): pausiert bis $until"
            )
            return
        }

        // Play-Mode-Erinnerungen haben kein festes Thema/Intervall - bei jeder Neuplanung wird
        // neu gewuerfelt, was als Naechstes kommt (siehe ReminderHost.reroll). Persistiert VOR
        // der Slot-Berechnung, damit sowohl der Alarm als auch spaetere Anzeigen (Widget,
        // ReminderAnimationBus) mit dem frisch gewuerfelten Thema arbeiten.
        val effective = if (reminder.isPlayMode) {
            ReminderHost.reroll(context, reminder).also { dao.update(it) }
        } else {
            reminder
        }

        val naturalTrigger = nextTrigger(effective, System.currentTimeMillis())
        if (naturalTrigger == null) {
            Log.d(TAG, "Kein naechster Slot fuer \"${effective.label}\" (id=${effective.id}) - keine Wochentage aktiv?")
            return
        }

        // Frueher konnte hier eine Kollisionsbehandlung ("Spread out", inzwischen samt der
        // Einstellung `CollisionPrefs` geloescht) den Zeitpunkt verschieben, wenn eine andere
        // Erinnerung nahe daran lag. Das ist ersatzlos entfallen:
        // Verschieben loeste die Kollision zwar auf, aber um den Preis, dass eine Erinnerung
        // nicht mehr dann kam, wofuer sie gestellt war - aus "alle fuenf Minuten" wurde faktisch
        // "alle acht". Treffen jetzt mehrere auf denselben Moment, feuern sie alle zu ihrer
        // echten Zeit, nur nacheinander: kurze Intervalle zuerst (siehe ReminderTrigger im
        // :app-sim-Modul). Der Slot selbst wird nie mehr angetastet.
        val triggerAtMillis = naturalTrigger

        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val scheduled = setAlarm(context, alarmManager, effective, triggerAtMillis)
        // nextTriggerEpochMillis nur setzen, wenn wirklich ein Alarm steht - sonst haelt der
        // Watchdog die Erinnerung faelschlich fuer versorgt und plant sie nie nach
        // (siehe ReminderWatchdog.findStale).
        dao.updateNextTrigger(effective.id, if (scheduled) triggerAtMillis else null)
    }

    /**
     * Setzt den Alarm - exakt wenn erlaubt, sonst ungefaehr. Liefert zurueck, ob ueberhaupt einer
     * gesetzt werden konnte.
     *
     * **Warum ein ungefaehrer Alarm statt gar keinem:** vorher brach [schedule] ab, sobald
     * [canScheduleExact] false war - es feuerte dann keine einzige Erinnerung mehr, ohne dass die
     * App das nach aussen zeigte. Seit Android 14 ist `SCHEDULE_EXACT_ALARM` bei Neuinstallationen
     * standardmaessig verweigert, ein frisch installiertes Exemplar waere damit ab Werk
     * funktionslos gewesen. Eine um einige Minuten verschobene Erinnerung ist unvergleichlich
     * besser als gar keine - der Widget-Tick handhabt das laengst genauso
     * (GlyphClockWidgetProvider.scheduleNextTick), nur die Erinnerungen selbst taten es nicht.
     *
     * Das `try/catch` ist kein Sicherheitsnetz auf Verdacht: zwischen [canScheduleExact] und dem
     * Setzen kann die Berechtigung entzogen werden, dann wirft [AlarmManager] eine
     * [SecurityException]. Ohne Behandlung riss die Alarmkette an dieser Stelle endgueltig ab.
     */
    private fun setAlarm(
        context: Context,
        alarmManager: AlarmManager,
        reminder: GlyphReminder,
        triggerAtMillis: Long
    ): Boolean {
        val pendingIntent = pendingIntent(context, reminder)
        val exact = canScheduleExact(context)
        return try {
            if (exact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                Log.d(TAG, "Alarm gesetzt fuer \"${reminder.label}\" (id=${reminder.id}) um ${formatDebug(triggerAtMillis)}")
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                Log.w(
                    TAG,
                    "Keine Exact-Alarm-Berechtigung - \"${reminder.label}\" (id=${reminder.id}) laeuft " +
                        "ungefaehr um ${formatDebug(triggerAtMillis)} statt exakt"
                )
            }
            true
        } catch (e: SecurityException) {
            // Berechtigung wurde zwischen Pruefung und Aufruf entzogen. Letzter Versuch ungenau -
            // scheitert auch der, meldet false und der Watchdog nimmt sich die Erinnerung
            // beim naechsten Lauf erneut vor.
            Log.w(TAG, "Exakter Alarm abgelehnt fuer \"${reminder.label}\" (id=${reminder.id}), versuche ungefaehr", e)
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                true
            } catch (fallback: SecurityException) {
                Log.e(TAG, "Auch ungefaehrer Alarm abgelehnt fuer \"${reminder.label}\" (id=${reminder.id})", fallback)
                false
            }
        }
    }

    suspend fun cancel(context: Context, reminder: GlyphReminder) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager?.cancel(pendingIntent(context, reminder))
        ReminderHost.dao(context).updateNextTrigger(reminder.id, null)
    }

    /**
     * Bestellt ALLE bestehenden Alarme ab und plant danach nur die des gerade aktiven Profils
     * neu. Das Abbestellen muss ueber alle Erinnerungen laufen (nicht nur die des aktiven
     * Profils), sonst blieben nach einem Profil-Wechsel die Alarme des vorherigen Profils
     * aktiv und wuerden weiter feuern.
     */
    suspend fun rescheduleAll(context: Context) {
        val dao = ReminderHost.dao(context)
        dao.getAll().forEach { cancel(context, it) }
        dao.getEnabledForProfile(ActiveProfilePrefs.get(context)).forEach { schedule(context, it) }
    }

    private fun formatDebug(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("EEE dd.MM. HH:mm:ss"))

    /** Extra-Key der Erinnerungs-ID im Alarm-Intent - die Empfaenger der Apps lesen ihn aus. */
    const val EXTRA_REMINDER_ID = "extra_reminder_id"

    private const val TAG = "ReminderScheduler"
    private const val MINUTES_PER_DAY = 24 * 60

    private fun pendingIntent(context: Context, reminder: GlyphReminder): PendingIntent {
        val intent = Intent(context, ReminderHost.alarmReceiver()).apply {
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        return PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
