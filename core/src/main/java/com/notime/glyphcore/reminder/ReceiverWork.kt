package com.notime.glyphcore.reminder

import android.content.BroadcastReceiver
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Fuehrt die Arbeit eines [BroadcastReceiver]s begrenzt und abgesichert aus.
 *
 * ## Was daran vorher fehlte
 *
 * Alle vier Empfaenger beider Apps sahen gleich aus:
 *
 * ```
 * val pendingResult = goAsync()
 * CoroutineScope(Dispatchers.IO).launch {
 *     try { ...Arbeit... } finally { pendingResult.finish() }
 * }
 * ```
 *
 * Das `goAsync()` und das `finally` waren richtig gedacht - drei Dinge fehlten aber:
 *
 * 1. **Keine Zeitgrenze.** Android raeumt einem Empfaenger nur etwa zehn Sekunden ein, danach
 *    gilt er als haengend und der Prozess kann abgeraeumt werden. Bleibt die Arbeit stehen (eine
 *    Datenbank, die nicht aufgeht; eine Migration, die laenger braucht), wird `finish()` nie
 *    erreicht - der Empfaenger bleibt bis zum Prozessende offen.
 *
 * 2. **Keine Fehlerbehandlung.** `launch` auf einem eigens erzeugten Scope hat keinen Eltern-Job,
 *    der eine Ausnahme auffangen koennte: sie landet beim Standard-Handler und beendet den
 *    Prozess. Beim Empfaenger fuer `BOOT_COMPLETED` heisst das ein Absturz waehrend des
 *    Hochfahrens - sichtbar fuer den Nutzer, und ausgerechnet in dem Moment, in dem die Alarme
 *    neu gestellt werden sollten.
 *
 * 3. **Der Scope wurde nie aufgeraeumt.** Jeder Aufruf legte einen neuen an, den niemand mehr
 *    beendet.
 *
 * ## Was hier passiert
 *
 * Die Arbeit bekommt ein Zeitbudget, jede Ausnahme wird protokolliert statt weitergereicht, und
 * `finish()` laeuft in jedem Fall - auch bei Zeitueberschreitung. Der Scope wird danach beendet.
 *
 * **Wichtig: das ist keine Zusage, dass die Arbeit fertig wird.** Es ist die Zusage, dass ein
 * Scheitern begrenzt bleibt und sichtbar wird. Alles, was laenger dauern kann als das Budget,
 * gehoert nicht in einen Empfaenger, sondern in einen Worker - siehe [ReminderRescheduleWorker].
 */
object ReceiverWork {

    /**
     * Etwas unter dem, was Android einem Empfaenger zugesteht (ungefaehr zehn Sekunden).
     *
     * Der Abstand ist Absicht: die Zeitueberschreitung soll VOR dem System zuschlagen, damit noch
     * eine Protokollzeile und ein sauberes `finish()` moeglich sind. Kaeme das System zuerst,
     * bliebe nur ein wortloses Prozessende.
     */
    const val BUDGET_MILLIS = 8_000L

    /**
     * @param pendingResult das Ergebnis von `goAsync()`, wird immer abgeschlossen
     * @param tag Protokoll-Tag des aufrufenden Empfaengers
     */
    fun runBounded(
        pendingResult: BroadcastReceiver.PendingResult,
        tag: String,
        budgetMillis: Long = BUDGET_MILLIS,
        block: suspend () -> Unit
    ) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                withTimeout(budgetMillis) { block() }
            } catch (timeout: TimeoutCancellationException) {
                Log.w(
                    tag,
                    "Arbeit nach ${budgetMillis} ms abgebrochen - Android haette den Empfaenger " +
                        "ohnehin gleich abgeraeumt. Was laenger dauert, gehoert in einen Worker.",
                    timeout
                )
            } catch (error: Exception) {
                // Bewusst Exception und nicht Throwable: ein OutOfMemoryError oder ein
                // LinkageError laesst sich hier nicht sinnvoll behandeln, und ihn zu
                // verschlucken verstecke nur die Ursache.
                Log.e(tag, "Arbeit im Empfaenger fehlgeschlagen", error)
            } finally {
                pendingResult.finish()
                scope.cancel()
            }
        }
    }
}
