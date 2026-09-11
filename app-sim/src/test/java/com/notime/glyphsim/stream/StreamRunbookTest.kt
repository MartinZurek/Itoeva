package com.notime.glyphsim.stream

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Bindet das Runbook `docs/streaming-poc.md` an die Build-Konfiguration, gegen die es
 * geschrieben ist.
 *
 * ## Der Befund, der dazu gefuehrt hat
 *
 * Das Runbook stand mehrere Tage mit `installDebug` und
 * `am start -n com.notime.glyphminderwatch/...` im Baum - also mit der NORMALEN App, obwohl
 * NT-058 den Stream-Client misst. Wer ihm gefolgt waere, haette zwei Stunden aufgezeichnet,
 * ohne Direktstart, ohne automatische Slot-Belegung und ohne Viewer-Simulator, und haette es
 * vermutlich erst beim Auswerten gemerkt.
 *
 * Solche Abweichungen entstehen nicht durch Nachlaessigkeit, sondern dadurch, dass ein
 * Dokument und eine Gradle-Datei nichts voneinander wissen. Dieser Test macht sie zu einer
 * Einheit: Wer den Build-Typ umbenennt oder das Suffix aendert, bekommt einen roten Test statt
 * eines Messtags ins Leere.
 *
 * ## Was er ausdruecklich NICHT tut
 *
 * Er baut nichts, startet nichts und prueft keine Messwerte. Er prueft nur, dass die drei
 * Bezeichner, an denen der Leser haengenbleibt - Aufgabe, APK-Pfad, `applicationId` - im
 * Dokument genauso stehen wie in `app-sim/build.gradle.kts`.
 *
 * Die Dateien werden relativ zum Modulverzeichnis gelesen. Sowohl Gradle als auch
 * `tools/reaction-preview/tests.sh` fuehren die Tests von dort aus - siehe die Begruendung im
 * Skript, wo dasselbe fuer die Golden-Datei von `ReactionFingerprintTest` gilt.
 */
class StreamRunbookTest {

    private val runbook = File("../docs/streaming-poc.md").readText()
    private val buildScript = File("build.gradle.kts").readText()

    @Test
    fun `der Build-Typ stream existiert und ist separat installierbar`() {
        assertTrue(
            "Build-Typ 'stream' fehlt in app-sim/build.gradle.kts",
            buildScript.contains("create(\"stream\")")
        )
        assertTrue(
            "Ohne eigenes applicationId-Suffix liesse sich der Stream-Client nicht neben der " +
                "normalen App installieren - genau das verlangt das Runbook",
            buildScript.contains("applicationIdSuffix = \".stream\"")
        )
    }

    @Test
    fun `das Runbook nennt die Aufgabe, den APK-Pfad und die applicationId der Stream-Variante`() {
        for (bezeichner in listOf(
            ":app-sim:assembleStream",
            "app-sim/build/outputs/apk/stream/app-sim-stream.apk",
            "com.notime.glyphminderwatch.stream"
        )) {
            assertTrue("docs/streaming-poc.md nennt '$bezeichner' nicht", bezeichner in runbook)
        }
    }

    @Test
    fun `das Runbook schickt niemanden mehr in die normale App`() {
        // Der eigentliche Fehler von damals: ein Startbefehl ohne '.stream'. Geprueft wird
        // deshalb der Startbefehl selbst und nicht bloss, ob das Paket irgendwo vorkommt - die
        // normale App darf im Text durchaus erwaehnt werden, nur nicht gestartet.
        val startbefehle = Regex("""am start -n (\S+)""").findAll(runbook).map { it.groupValues[1] }.toList()
        assertTrue("Kein Startbefehl im Runbook gefunden", startbefehle.isNotEmpty())
        for (befehl in startbefehle) {
            assertTrue(
                "Startbefehl '$befehl' startet nicht den Stream-Client",
                befehl.startsWith("com.notime.glyphminderwatch.stream/")
            )
        }
    }

    @Test
    fun `die Grenze zwischen Einfluss und Steuerung steht im Runbook mit ihren Zahlen`() {
        // Die wichtigste Beobachtung des Laufs ist eine ueber Ablehnung. Sie laesst sich nur
        // pruefen, wenn im Messbogen steht, WARUM ein Impuls verlieren darf.
        assertTrue(
            "Das Runbook nennt GoalInfluence.MAX_WEIGHT nicht",
            "MAX_WEIGHT" in runbook
        )
        assertTrue(
            "Das Runbook nennt UtilitySelector.MIN_PRESSURE nicht",
            "MIN_PRESSURE" in runbook
        )
    }
}
