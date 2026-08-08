package com.notime.glyphsim.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderTrigger.fireFromAlarm(appContext, reminderId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }
}
