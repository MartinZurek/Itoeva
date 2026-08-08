package com.notime.glyphkalender.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notime.glyphcore.reminder.ReminderScheduler
import com.notime.glyphcore.reminder.ReminderWatchdogWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderScheduler.rescheduleAll(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
