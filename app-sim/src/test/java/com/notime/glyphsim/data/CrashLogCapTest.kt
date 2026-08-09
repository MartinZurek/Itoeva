package com.notime.glyphsim.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft das Kappen des Absturzberichts.
 *
 * ## Warum das noetig ist
 *
 * Ein Stapelabbild ist ueblicherweise wenige Kilobyte gross - aber nicht immer. Eine
 * Endlosrekursion erzeugt einen `StackOverflowError` mit Tausenden gleichfoermiger Zeilen, und
 * eine Kette verschachtelter Ursachen legt weitere darauf. Ungekappt landet das vollstaendig im
 * App-Speicher und danach vollstaendig in einem `Text` der Einstellungen - ausgerechnet in einer
 * Lage, in der die App ohnehin gerade abgestuerzt ist.
 *
 * Reine Zeichenkettenarbeit, laeuft deshalb als JVM-Test bei jedem `gradlew verify` mit.
 */
class CrashLogCapTest {

    @Test
    fun `ein gewoehnlicher Bericht bleibt unveraendert`() {
        val bericht = buildString {
            appendLine("Tama 1.0")
            appendLine("2026-08-09 10:00:00")
            appendLine("Google Pixel 6, Android 15 (API 35)")
            appendLine("Thread: main")
            appendLine()
            append("java.lang.IllegalStateException: irgendetwas\n\tat com.notime.glyphsim.Foo.bar(Foo.kt:42)")
        }

        assertEquals(bericht, CrashLog.cap(bericht))
    }

    @Test
    fun `genau die Obergrenze wird noch nicht gekappt`() {
        val bericht = "x".repeat(CrashLog.MAX_REPORT_CHARS)
        assertEquals(bericht, CrashLog.cap(bericht))
    }

    /**
     * Der Fall, um den es geht: ein Abbild mit Tausenden Zeilen, wie es eine Endlosrekursion
     * erzeugt.
     */
    @Test
    fun `ein ueberlanger Bericht wird gekappt und sagt das`() {
        val zeile = "\tat com.notime.glyphsim.Foo.bar(Foo.kt:42)\n"
        val bericht = "java.lang.StackOverflowError\n" + zeile.repeat(20_000)
        assertTrue("Der Testfall muss die Grenze wirklich ueberschreiten", bericht.length > CrashLog.MAX_REPORT_CHARS)

        val gekappt = CrashLog.cap(bericht)

        assertEquals(CrashLog.MAX_REPORT_CHARS + CrashLog.TRUNCATION_MARKER.length, gekappt.length)
        assertTrue(
            "Ohne Hinweis haelt der Leser das abgeschnittene Ende fuer das echte",
            gekappt.endsWith(CrashLog.TRUNCATION_MARKER)
        )
    }

    /**
     * Der Kopf des Berichts - Version, Zeitpunkt, Geraet - steht am Anfang und muss das Kappen
     * ueberstehen. Er ist der Teil, ohne den ein Abbild kaum einzuordnen ist.
     */
    @Test
    fun `der Kopf bleibt auch beim Kappen erhalten`() {
        val kopf = "Tama 1.0\n2026-08-09 10:00:00\nGoogle Pixel 6, Android 15 (API 35)\nThread: main\n\n"
        val bericht = kopf + "x".repeat(CrashLog.MAX_REPORT_CHARS * 2)

        assertTrue(CrashLog.cap(bericht).startsWith(kopf))
    }
}
