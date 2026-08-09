package com.notime.glyphsim.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException
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
     * Obergrenze fuer die geschriebene Datei.
     *
     * Ein Stapelabbild ist ueblicherweise wenige Kilobyte gross - aber nicht immer: eine
     * Endlosrekursion erzeugt einen `StackOverflowError` mit Tausenden gleichfoermiger Zeilen,
     * und eine Kette verschachtelter Ursachen (`Caused by:`) legt weitere darauf. Ungekappt
     * landet das vollstaendig im App-Speicher und danach vollstaendig in einem Compose-`Text` in
     * den Einstellungen.
     *
     * 64 KB fassen jedes Abbild, das man tatsaechlich noch liest; alles darueber wiederholt sich
     * ohnehin nur.
     */
    const val MAX_REPORT_CHARS = 64 * 1024

    /** Am Ende eines gekappten Berichts, damit der Leser weiss, dass etwas fehlt. */
    const val TRUNCATION_MARKER = "\n[... gekuerzt, siehe CrashLog.MAX_REPORT_CHARS]"

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
            /*
             * Hier wird ausnahmsweise `Throwable` gefangen und nicht nur `Exception`.
             *
             * Sonst gilt die Regel, fatale VM-Fehler nicht zu verschlucken - an dieser Stelle
             * waere ihr Befolgen aber schaedlich: der Absturzbehandler laeuft in einem bereits
             * beschaedigten Prozess, das Schreiben kann selbst an einem `OutOfMemoryError`
             * scheitern, und wenn dieser Fehler hier hochblubbert, wird `previous` nie erreicht.
             * Dann meldet Android den urspruenglichen Absturz womoeglich gar nicht mehr - ein
             * Folgefehler im Protokollieren haette den eigentlichen Fehler verdeckt.
             *
             * Verschluckt wird er trotzdem nicht: er geht ins Logcat, und der urspruengliche
             * Absturz laeuft unveraendert weiter.
             */
            try {
                write(appContext, thread, throwable)
            } catch (secondary: Throwable) {
                Log.w(TAG, "Absturzbericht konnte nicht geschrieben werden", secondary)
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun write(context: Context, thread: Thread, throwable: Throwable) {
        val stackTrace = StringWriter().also { throwable.printStackTrace(PrintWriter(it)) }.toString()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            // Kann eigentlich nicht passieren - das eigene Paket gibt es. Ein "?" ist hier aber
            // allemal besser als ein zweiter Absturz im Absturzbehandler.
            null
        } ?: "?"

        // Geraet und Android-Version gehoeren dazu: ein Absturz, der nur auf einer Version oder
        // einem Hersteller auftritt, ist ohne diese Angaben kaum einzugrenzen.
        val bericht = buildString {
            appendLine("Tama $versionName")
            appendLine("$timestamp")
            appendLine("${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Thread: ${thread.name}")
            appendLine()
            append(stackTrace)
        }

        File(context.filesDir, FILE_NAME).writeText(cap(bericht))
    }

    /** Kappt auf [MAX_REPORT_CHARS] und macht das im Text sichtbar. */
    internal fun cap(bericht: String): String =
        if (bericht.length <= MAX_REPORT_CHARS) {
            bericht
        } else {
            bericht.take(MAX_REPORT_CHARS) + TRUNCATION_MARKER
        }

    /**
     * Der zuletzt aufgezeichnete Absturz, oder null wenn es keinen gibt.
     *
     * Gekappt wird auch beim Lesen: die Datei koennte von einem aelteren Stand stammen, der die
     * Grenze noch nicht kannte, oder von aussen veraendert worden sein. Was in ein `Text` der
     * Oberflaeche wandert, soll in jedem Fall eine bekannte Groesse haben.
     */
    fun read(context: Context): String? {
        val datei = File(context.applicationContext.filesDir, FILE_NAME)
        if (!datei.exists()) return null
        val inhalt = try {
            datei.readText()
        } catch (e: IOException) {
            // Erwartbarer Fall (Datei geloescht, waehrend gelesen wird; Speicher voll). Kein
            // Grund, die Einstellungen abstuerzen zu lassen - dann gibt es eben keinen Bericht.
            Log.w(TAG, "Absturzbericht nicht lesbar", e)
            return null
        }
        return inhalt.takeIf { it.isNotBlank() }?.let(::cap)
    }

    /**
     * Loescht den Bericht.
     *
     * Der Nutzer kann das jederzeit in den Einstellungen ausloesen. Darueber hinaus verschwindet
     * die Datei beim Deinstallieren mit dem App-Speicher, und in eine Sicherung wandert sie
     * nicht - siehe die Positivliste in res/xml/backup_rules.xml.
     */
    fun clear(context: Context) {
        val datei = File(context.applicationContext.filesDir, FILE_NAME)
        try {
            datei.delete()
        } catch (e: SecurityException) {
            Log.w(TAG, "Absturzbericht konnte nicht geloescht werden", e)
        }
    }

    private const val TAG = "CrashLog"
}
