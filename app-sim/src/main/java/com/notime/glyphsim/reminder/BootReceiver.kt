package com.notime.glyphsim.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.notime.glyphcore.reminder.ReminderScheduler
import com.notime.glyphsim.widget.GlyphClockWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Plant alle aktivierten Erinnerungen neu, wenn das System die bestehenden Alarme verworfen hat
 * oder ihre berechneten Zeitpunkte nicht mehr stimmen. Vier Ereignisse loesen das aus:
 *
 * - **Reboot** - AlarmManager-Alarme ueberleben keinen Neustart.
 * - **App-Update** (`ACTION_MY_PACKAGE_REPLACED`) - Android verwirft dabei ebenfalls alle Alarme.
 *   Fehlte hier frueher: nach jedem Update feuerte keine einzige Erinnerung mehr, bis die App
 *   von Hand geoeffnet wurde. Bei einer App, die bewusst passiv im Widget/Dock-Modus laeuft,
 *   also ein stiller Totalausfall auf unbestimmte Zeit.
 * - **Zeit- und Zeitzonenwechsel** - die Alarme stehen auf absoluten Zeitpunkten, die aus
 *   Ortszeit-Fenstern berechnet wurden. Nach einem Zeitzonenwechsel zeigen sie auf die falsche
 *   Ortszeit und muessen neu gerechnet werden.
 *
 * Ein davon unabhaengiges Sicherheitsnetz laeuft periodisch ueber
 * [com.notime.glyphcore.reminder.ReminderWatchdogWorker] - es faengt die Faelle ab, zu denen es
 * gar keinen Broadcast gibt (Force-Stop, Akkuverwaltung des Herstellers).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Achtung beim Abgleich mit dem Manifest: Intent.ACTION_TIME_CHANGED heisst als
        // Action-String "android.intent.action.TIME_SET" - dort steht deshalb TIME_SET, das ist
        // kein Tippfehler. Die uebrigen drei heissen in beiden Welten gleich.
        val relevant = intent.action in setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
        if (!relevant) return

        val appContext = context.applicationContext
        GlyphClockWidgetProvider.scheduleNextTick(appContext)
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
