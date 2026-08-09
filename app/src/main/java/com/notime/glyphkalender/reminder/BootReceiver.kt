package com.notime.glyphkalender.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notime.glyphcore.reminder.ReminderRescheduleWorker
import com.notime.glyphcore.reminder.ReminderWatchdogWorker

/**
 * Plant alle aktivierten Erinnerungen neu, wenn das System die bestehenden Alarme verworfen hat
 * (Reboot, App-Update) oder ihre berechneten Zeitpunkte nicht mehr stimmen (Zeit-/
 * Zeitzonenwechsel) - ausfuehrliche Begruendung je Ereignis im BootReceiver des :app-sim-Moduls.
 * Periodisches Sicherheitsnetz fuer die Faelle ohne Broadcast: [ReminderWatchdogWorker].
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val relevant = intent.action in setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
        if (!relevant) return

        // Nicht im Empfaenger erledigen, sondern einstellen - die Begruendung steht bei
        // ReminderRescheduleWorker und gilt hier genauso.
        ReminderRescheduleWorker.enqueue(context.applicationContext)
    }
}
