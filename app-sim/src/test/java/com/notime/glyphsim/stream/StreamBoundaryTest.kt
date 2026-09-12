package com.notime.glyphsim.stream

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Haelt die Grenze zwischen gemeinsamem Kern, normaler App und Stream-Client fest.
 *
 * ## Der Anspruch, den dieser Test bewacht
 *
 * Itoeva bleibt **ein** Spiel. Der Stream-Client ist ein weiterer Client derselben Welt, kein
 * Fork. Daraus folgen drei Dinge, die man nicht sieht und die deshalb hier stehen:
 *
 * 1. `:core` - die Schicht, die sich `:app` und `:app-sim` teilen - weiss von Twitch nichts.
 *    Wuerde dort auch nur ein Import auftauchen, traege die Glyphkalender-App ploetzlich
 *    Streaming-Code mit sich.
 * 2. Die Netzberechtigung liegt ausschliesslich im Quellsatz des Build-Typs `stream`. Die
 *    normale App kann damit gar keine Verbindung aufbauen - das ist keine Vereinbarung,
 *    sondern eine Zusicherung des Betriebssystems.
 * 3. Der gesamte Twitch-Code liegt in genau einem Paket. Wer ihn woanders anfaengt, faellt hier
 *    auf, statt ihn quer durch die Oberflaeche zu verteilen.
 *
 * Die Dateien werden relativ zum Modulverzeichnis gelesen - wie in [StreamRunbookTest] und aus
 * demselben Grund.
 */
class StreamBoundaryTest {

    private val coreSources: List<File> =
        File("../core/src/main").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private val simSources: List<File> =
        File("src/main/java").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    @Test
    fun `core kennt weder Twitch noch den Stream-Client`() {
        assertTrue("Keine :core-Quellen gefunden - der Test liefe ins Leere", coreSources.isNotEmpty())
        val verboten = listOf("twitch", "justinfan", "PRIVMSG", "glyphsim.stream")
        val treffer = coreSources.flatMap { datei ->
            val text = datei.readText()
            verboten.filter { it.lowercase() in text.lowercase() }.map { "${datei.name}: $it" }
        }
        assertEquals("In :core darf nichts davon vorkommen", emptyList<String>(), treffer)
    }

    @Test
    fun `core oeffnet keine Netzverbindung`() {
        val netz = listOf("java.net.Socket", "javax.net.ssl", "HttpURLConnection", "okhttp")
        val treffer = coreSources.flatMap { datei ->
            val text = datei.readText()
            netz.filter { it in text }.map { "${datei.name}: $it" }
        }
        assertEquals(emptyList<String>(), treffer)
        assertTrue(
            ":core darf keine Netzbibliothek als Abhaengigkeit fuehren",
            File("../core/build.gradle.kts").readText().let {
                "okhttp" !in it && "ktor" !in it && "retrofit" !in it
            }
        )
    }

    @Test
    fun `die Netzberechtigung steht nur im Quellsatz des Stream-Build-Typs`() {
        val streamManifest = File("src/stream/AndroidManifest.xml")
        assertTrue("app-sim/src/stream/AndroidManifest.xml fehlt", streamManifest.exists())
        assertTrue(
            "Der Stream-Client braucht android.permission.INTERNET",
            "android.permission.INTERNET" in streamManifest.readText()
        )
        assertTrue(
            "Die normale App darf die Netzberechtigung NICHT bekommen",
            "android.permission.INTERNET" !in File("src/main/AndroidManifest.xml").readText()
        )
    }

    @Test
    fun `der gesamte Twitch-Code liegt im Stream-Paket`() {
        val draussen = simSources
            .filter { "/glyphsim/stream/" !in it.path.replace('\\', '/') }
            .filter { datei ->
                val text = datei.readText()
                "irc.chat.twitch.tv" in text || "justinfan" in text || "PRIVMSG" in text
            }
            .map { it.name }
        assertEquals(
            "Twitch-Protokollwissen gehoert ausschliesslich nach com.notime.glyphsim.stream",
            emptyList<String>(),
            draussen
        )
    }
}
