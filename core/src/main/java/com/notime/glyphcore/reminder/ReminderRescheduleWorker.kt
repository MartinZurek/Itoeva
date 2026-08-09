package com.notime.glyphcore.reminder

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters

/**
 * Plant nach Reboot, App-Update oder Zeitwechsel alle Alarme neu - **ausserhalb des Empfaengers**.
 *
 * ## Warum das nicht im Empfaenger bleiben konnte
 *
 * `rescheduleAll` ist die teuerste Sache, die diese App tut: Datenbank oeffnen (nach einem Update
 * womoeglich mit Migration), alle Erinnerungen lesen, fuer jede einen Alarm berechnen und setzen.
 * Ein Empfaenger hat dafuer rund zehn Sekunden. Auf einem langsamen Geraet, das gerade hochfaehrt
 * und Dutzende anderer Empfaenger gleichzeitig bedient, ist das keine sichere Annahme.
 *
 * Reisst es mittendrin ab, ist das Ergebnis das denkbar unguenstigste: ein Teil der Erinnerungen
 * hat einen Alarm, der Rest nicht - und nichts versucht es erneut. Der Nutzer merkt nur, dass
 * manche Erinnerungen nach einem Neustart nicht mehr kommen.
 *
 * ## Was WorkManager daran aendert
 *
 * - **Kein Zehn-Sekunden-Budget.** Die Arbeit darf so lange dauern, wie sie braucht.
 * - **Wiederholung bei Fehlern.** Scheitert der Lauf, versucht WorkManager es mit wachsendem
 *   Abstand erneut, statt ihn ersatzlos fallen zu lassen.
 * - **Ueberlebt das Prozessende.** Der Auftrag liegt in WorkManagers eigener Datenbank; wird die
 *   App waehrend des Laufs abgeraeumt, wird er spaeter nachgeholt.
 *
 * Dass die Ausfuehrung dadurch um Sekunden spaeter beginnt, ist der Preis - und ein guter: nach
 * einem Reboot stehen ohnehin keine Alarme, und ein paar Sekunden spaeter richtig ist besser als
 * sofort und halb.
 *
 * ## Nicht zu verwechseln mit dem Watchdog
 *
 * [ReminderWatchdogWorker] laeuft **regelmaessig** und sucht einzelne abgerissene Ketten. Dieser
 * hier laeuft **einmalig** auf ein konkretes Ereignis hin und plant alles neu. Beide zusammen
 * decken zwei verschiedene Ausfallarten ab.
 */
class ReminderRescheduleWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        ReminderScheduler.rescheduleAll(applicationContext)
        Log.d(TAG, "Alle Erinnerungen neu eingeplant")
        Result.success()
    } catch (error: Exception) {
        // Begrenzt wiederholen: haelt der Fehler an (etwa eine dauerhaft defekte Datenbank),
        // wuerde ewiges Wiederholen nur Akku kosten, ohne je zum Ziel zu kommen.
        if (runAttemptCount < MAX_ATTEMPTS) {
            Log.w(TAG, "Neuplanen fehlgeschlagen (Versuch ${runAttemptCount + 1}), versuche es erneut", error)
            Result.retry()
        } else {
            Log.e(TAG, "Neuplanen endgueltig fehlgeschlagen nach $MAX_ATTEMPTS Versuchen", error)
            // Der regelmaessige Watchdog bleibt als letztes Netz - siehe ReminderWatchdogWorker.
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "ReminderReschedule"
        private const val MAX_ATTEMPTS = 3
        private const val UNIQUE_WORK_NAME = "reminder-reschedule-all"

        /**
         * Stellt den Auftrag ein.
         *
         * `REPLACE` und nicht `KEEP`: Reboot, App-Update und Zeitzonenwechsel koennen dicht
         * beieinanderliegen (ein Update kurz vor einem Neustart, eine Zeitumstellung waehrend des
         * Hochfahrens). Was zaehlt, ist immer der neueste Stand - ein bereits wartender Auftrag mit
         * veralteter Ausgangslage darf den neuen nicht verdraengen. Weil der Lauf ohnehin ALLES neu
         * plant, geht durch das Ersetzen nichts verloren.
         */
        fun enqueue(context: Context) {
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ReminderRescheduleWorker>().build()
            )
        }
    }
}
