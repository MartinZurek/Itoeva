package com.notime.glyphkalender.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.notime.glyphcore.reminder.ReceiverWork
import com.notime.glyphcore.reminder.ReminderScheduler
import com.notime.glyphkalender.data.AppDatabase
import com.notime.glyphkalender.glyph.ReminderGlyphService

/**
 * Wird vom AlarmManager fuer einen Erinnerungs-Slot ausgeloest. Laedt die aktuelle
 * GlyphReminder-Definition aus der DB (statt Label/Typ direkt im Intent mitzugeben),
 * damit eine zwischenzeitliche Bearbeitung beruecksichtigt wird, startet die Animation
 * und plant ueber ReminderScheduler direkt den naechsten Slot dieser Erinnerung neu.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        // Klein genug fuer den Empfaenger und zu zeitkritisch fuer einen Worker - aber mit
        // Zeitbudget und Fehlerbehandlung, siehe ReceiverWork.
        val appContext = context.applicationContext
        ReceiverWork.runBounded(goAsync(), TAG) {
            val dao = AppDatabase.getInstance(appContext).glyphReminderDao()
            val reminder = dao.getById(reminderId)
            if (reminder == null) {
                Log.d(TAG, "Erinnerung id=$reminderId nicht mehr vorhanden, ignoriere Alarm")
                return@runBounded
            }
            Log.d(TAG, "Alarm empfangen: \"${reminder.label}\" (id=$reminderId)")
            if (reminder.enabled) {
                ReminderGlyphService.start(
                    appContext,
                    reminder.label,
                    reminder.animationType,
                    reminder.libraryAnimationId,
                    reminder.intervalMinutes
                )
            }
            ReminderScheduler.schedule(appContext, reminder)
        }
    }

    companion object {
        private const val TAG = "ReminderAlarmReceiver"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }
}
