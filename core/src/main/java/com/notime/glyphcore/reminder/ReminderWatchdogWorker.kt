package com.notime.glyphcore.reminder

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Regelmaessiger Selbstheilungslauf: findet Erinnerungen, deren Alarmkette abgerissen ist (siehe
 * [ReminderWatchdog] fuer das Problem und seine Ursachen), und plant sie neu.
 *
 * **Warum WorkManager und nicht noch ein Alarm?** Der Watchdog soll genau die Faelle auffangen, in
 * denen die Alarmkette versagt hat - ein weiterer `AlarmManager`-Alarm haette exakt dieselben
 * Schwachstellen und wuerde mit ausfallen. WorkManager persistiert seine Auftraege dagegen in
 * einer eigenen Datenbank und stellt sie nach Reboot, App-Update und Prozessende selbst wieder
 * her. Er ist ausserdem nicht auf die Exact-Alarm-Berechtigung angewiesen, die dem Scheduler
 * jederzeit entzogen werden kann.
 *
 * Die Laufzeit ist bewusst ungenau ([REPEAT_INTERVAL_HOURS], WorkManager buendelt sie mit anderen
 * Systemaufgaben): der Watchdog muss nicht puenktlich sein, er muss nur ueberhaupt stattfinden.
 * Minutengenauigkeit wuerde nur Akku kosten.
 */
class ReminderWatchdogWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val dao = ReminderHost.dao(applicationContext)
            val activeProfile = ActiveProfilePrefs.get(applicationContext)
            // Nur der Satz, der zum gerade laufenden Modus gehoert (siehe [PlayModeState]). Ohne
            // diese Einschraenkung fiele der jeweils ruhende Satz dem Watchdog bei JEDEM Lauf als
            // "kein Alarm hinterlegt" auf - er wuerde ihn neu einplanen, [ReminderScheduler]
            // lehnte ihn wieder ab, und beim naechsten Lauf begaenne dasselbe von vorn.
            val stale = ReminderWatchdog.findStale(
                dao.getEnabledForProfile(activeProfile)
                    .filter { PlayModeState.matchesCurrentMode(applicationContext, it) },
                System.currentTimeMillis()
            )

            if (stale.isEmpty()) {
                Log.d(TAG, "Alarmkette intakt, nichts nachzuplanen")
                return Result.success()
            }

            Log.w(TAG, "${stale.size} abgerissene Erinnerung(en) gefunden, plane neu: ${stale.map { it.label }}")
            stale.forEach { ReminderScheduler.schedule(applicationContext, it) }
            Result.success()
        } catch (e: Exception) {
            // Kein Result.failure(): ein einmaliger Fehler (z.B. Datenbank gerade gesperrt) darf
            // den periodischen Auftrag nicht dauerhaft beenden - retry laesst ihn spaeter erneut
            // laufen, und der naechste regulaere Durchlauf kommt ohnehin.
            Log.e(TAG, "Watchdog-Lauf fehlgeschlagen", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "ReminderWatchdog"
        private const val WORK_NAME = "reminder-watchdog"

        /**
         * Alle zwei Stunden. Kurz genug, dass ein Abriss nicht ueber Nacht unbemerkt bleibt, und
         * lang genug, um im Akkuverbrauch nicht ins Gewicht zu fallen (WorkManager legt den Lauf
         * ohnehin in ein passendes Systemfenster).
         */
        private const val REPEAT_INTERVAL_HOURS = 2L

        /**
         * Aus `Application.onCreate()` aufzurufen, direkt nach [ReminderHost.install].
         *
         * [ExistingPeriodicWorkPolicy.KEEP] statt UPDATE: der Auftrag ueberlebt Reboots und
         * App-Updates von sich aus. Ihn bei jedem Start neu zu setzen wuerde den Zeitplan jedes
         * Mal zuruecksetzen - bei einer App, die oefter geoeffnet als selten gestartet wird,
         * koennte der Lauf dadurch dauerhaft nach hinten rutschen und nie stattfinden.
         */
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWatchdogWorker>(
                REPEAT_INTERVAL_HOURS,
                TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
