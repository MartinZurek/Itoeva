package com.notime.glyphcore.data

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Der gemeinsame Kern hat eigene Zeichenketten - und die wurden bisher von nichts bewacht.**
 *
 * `StringResourceParityTest` und `HelpTextsTest` liegen im :app-sim-Modul und pruefen nur dessen
 * Ressourcen. Die des Kerns fielen durch beide Netze, obwohl beide Apps sie benutzen.
 *
 * Aufgefallen ist das an den Voreinstellungen: Die beim allerersten Start angelegten Erinnerungen
 * hiessen fest verdrahtet "Trink was" und "Beweg dich" - ein englischsprachiger Nutzer bekam
 * also ausgerechnet als Erstes deutschen Text zu lesen. Sie stehen jetzt als Ressourcen hier, und
 * dieser Test sorgt dafuer, dass die naechste Ergaenzung nicht wieder nur auf Englisch existiert.
 */
class CoreStringResourceParityTest {

    private val en = File("src/main/res/values/strings.xml").readText()
    private val de = File("src/main/res/values-de/strings.xml").readText()

    private fun names(xml: String): Set<String> =
        Regex("""<string name="(\w+)"""").findAll(xml).map { it.groupValues[1] }.toSet()

    private fun valueOf(xml: String, name: String): String? =
        Regex("""<string name="$name">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .find(xml)?.groupValues?.get(1)

    @Test
    fun `beide Sprachen kennen dieselben Texte`() {
        assertEquals(
            "Diese Texte des Kerns gibt es nur auf Englisch",
            emptyList<String>(),
            (names(en) - names(de)).sorted()
        )
        assertEquals(
            "Diese Texte des Kerns gibt es nur auf Deutsch",
            emptyList<String>(),
            (names(de) - names(en)).sorted()
        )
    }

    /**
     * Die Beschriftungen der Vorgaben muessen existieren - sie werden beim allerersten Start
     * aufgeloest (siehe [DefaultReminders.all]). Fehlt eine, stuerzt die App genau dann ab, wenn
     * jemand sie zum ersten Mal oeffnet.
     */
    @Test
    fun `die Vorgaben-Beschriftungen gibt es in beiden Sprachen`() {
        for (name in listOf("default_reminder_drink", "default_reminder_move")) {
            assertTrue("$name fehlt auf Englisch", name in names(en))
            assertTrue("$name fehlt auf Deutsch", name in names(de))
        }
    }

    /**
     * **Und sie muessen sich unterscheiden.** Eine Uebersetzung, die versehentlich das Original
     * kopiert, faellt sonst durch jedes Netz - genau der Zustand, aus dem dieser Test entstanden
     * ist: Der deutsche Text stand in beiden Faellen da, weil es gar keinen englischen gab.
     */
    @Test
    fun `die Vorgaben sind wirklich uebersetzt`() {
        for (name in listOf("default_reminder_drink", "default_reminder_move")) {
            assertTrue(
                "$name ist auf Deutsch und Englisch identisch - vermutlich nicht uebersetzt",
                valueOf(en, name) != valueOf(de, name)
            )
        }
    }
}
