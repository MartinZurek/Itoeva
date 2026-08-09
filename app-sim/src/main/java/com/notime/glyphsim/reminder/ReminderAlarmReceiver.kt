package com.notime.glyphsim.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notime.glyphcore.reminder.ReceiverWork

/**
 * Wird vom AlarmManager fuer einen Erinnerungs-Slot ausgeloest. Die eigentliche Arbeit steht in
 * [ReminderTrigger] - dort auch die Begruendung, warum es neben diesem Weg noch einen zweiten
 * gibt (einen Takt im laufenden Prozess, solange die App sichtbar ist).
 *
 * Dieser Weg bleibt fuer das Home-Screen-Widget zustaendig, das auch ohne offene App sichtbar
 * ist. Es gibt weiterhin keine Notification und kein Vollbild-Popup: das Widget uebernimmt allein
 * die Rolle der Glyph-Matrix-Hardware aus dem :app-Modul.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        // Anders als das Neuplanen nach einem Reboot gehoert das hier NICHT in einen Worker:
        // eine Erinnerung, die ein paar Sekunden spaeter erscheint, hat ihren Zweck verfehlt.
        // Die Arbeit ist klein (eine Zeile lesen, anzeigen, den naechsten Slot planen) und bleibt
        // deshalb im Empfaenger - aber mit Zeitbudget und ohne dass ein Fehler den Prozess
        // mitreisst. Siehe ReceiverWork.
        val appContext = context.applicationContext
        ReceiverWork.runBounded(goAsync(), TAG) {
            ReminderTrigger.fireFromAlarm(appContext, reminderId)
        }
    }

    companion object {
        private const val TAG = "ReminderAlarmReceiver"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }
}
