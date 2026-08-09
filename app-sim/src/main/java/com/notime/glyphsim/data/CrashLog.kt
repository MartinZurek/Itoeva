package com.notime.glyphsim.data

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Schreibt Abstuerze in eine Datei im App-eigenen Speicher, damit sie sich nachtraeglich ansehen
 * und weitergeben lassen.
 *
 * **Warum lokal und nicht Crashlytics:** Apps, die ueber den Play Store verteilt werden, bekommen
 * Abstuerze samt Stacktrace ohnehin kostenlos ueber Android Vitals in der Play Console - dafuer
 * braucht es kein SDK. Ein Fremd-SDK haette dagegen zwei Preise: eine Abhaengigkeit zu Google
 * Play Services und eine Aenderung am Datensicherheits-Formular, weil Diagnosedaten dann erhoben
 * und an Dritte weitergegeben werden (siehe PLAY_STORE.md, dort steht derzeit "keine
 * Datenerhebung").
 *
 * Was Android Vitals NICHT abdeckt, ist die Zeit davor: Abstuerze auf dem eigenen Geraet
 * waehrend der Entwicklung, bei einer per Datei installierten Testfassung oder bei Testern. Genau
 * dort fehlte zuletzt der Stacktrace, um einen gemeldeten Absturz ueberhaupt einordnen zu
 * koennen. Diese Datei schliesst diese Luecke - ohne dass irgendetwas das Geraet verlaesst.
 *
 * **Und das gilt auch fuer Androids eigene Sicherung.** Diese Zusage war eine Zeit lang nicht
 * gedeckt: mit `allowBackup="true"` und ohne Regelwerk wanderte der Bericht samt Geraetemodell und
 * Stacktrace ins Google-Konto. `res/xml/backup_rules.xml` und `res/xml/data_extraction_rules.xml`
 * schliessen ihn seither aus (Positivliste - der Dateibereich der App ist gar nicht erst
 * aufgefuehrt). Wer den Ablageort dieser Datei aendert, muss dort nachsehen.
 */
object CrashLog {

    private const val FILE_NAME = "last-crash.txt"

    /**
     * Aus `Application.onCreate()` aufzurufen.
     *
     * Der zuvor gesetzte Handler wird danach IMMER aufgerufen: er ist es, der den Prozess
     * beendet und dem System den Absturz meldet. Wuerde man ihn ersetzen statt ihn zu ergaenzen,
     * bliebe die App nach einem Absturz in einem undefinierten Zustand haengen - und Android
     * Vitals bekaeme den Absturz womoeglich gar nicht mit.
     */
    fun install(context: Context) {
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Das Schreiben selbst darf nicht scheitern duerfen - sonst verloere man den
            // eigentlichen Absturz hinter einem Folgefehler im Absturzbehandler.
            runCatching { write(appContext, thread, throwable) }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun write(context: Context, thread: Thread, throwable: Throwable) {
        val stackTrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val versionName = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "?"

        // Geraet und Android-Version gehoeren dazu: ein Absturz, der nur auf einer Version oder
        // einem Hersteller auftritt, ist ohne diese Angaben kaum einzugrenzen.
        File(context.filesDir, FILE_NAME).writeText(
            buildString {
                appendLine("Tama $versionName")
                appendLine("$timestamp")
                appendLine("${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("Thread: ${thread.name}")
                appendLine()
                append(stackTrace)
            }
        )
    }

    /** Der zuletzt aufgezeichnete Absturz, oder null wenn es keinen gibt. */
    fun read(context: Context): String? =
        File(context.applicationContext.filesDir, FILE_NAME)
            .takeIf { it.exists() }
            ?.runCatching { readText() }
            ?.getOrNull()
            ?.takeIf { it.isNotBlank() }

    fun clear(context: Context) {
        runCatching { File(context.applicationContext.filesDir, FILE_NAME).delete() }
    }
}
