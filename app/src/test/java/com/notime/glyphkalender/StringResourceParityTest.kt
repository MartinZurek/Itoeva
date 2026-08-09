package com.notime.glyphkalender

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.regex.Pattern

/**
 * Prueft, dass die deutsche Fassung der Zeichenketten vollstaendig ist und zur englischen passt.
 *
 * ## Wogegen
 *
 * Eine fehlende Uebersetzung faellt nicht auf: Android faellt still auf die Standardsprache
 * zurueck. Der Nutzer sieht dann eine deutsche Oberflaeche mit einzelnen englischen Brocken darin,
 * und niemand merkt es, bis es jemandem auffaellt. Genau so war der Stand dieses Moduls vor Phase
 * 5.3 - nur umgekehrt: eine englische Oberflaeche mit einer deutschen Benachrichtigung.
 *
 * Lint meldet fehlende Uebersetzungen zwar auch (`MissingTranslation`), aber erst beim Bauen der
 * Ressourcen und ohne die Formatplatzhalter zu vergleichen. Dieser Test liest die XML-Dateien
 * direkt und laeuft als schneller JVM-Test bei jedem `gradlew verify` mit.
 *
 * ## Was geprueft wird
 *
 * 1. Jeder uebersetzbare Eintrag hat eine deutsche Entsprechung.
 * 2. Die deutsche Fassung enthaelt keine Eintraege, die es auf Englisch gar nicht gibt (Tippfehler
 *    im Namen faellt sonst niemandem auf - der Eintrag waere schlicht tot).
 * 3. Beide Fassungen verwenden dieselben Formatplatzhalter. Das ist der gefaehrlichste Fall:
 *    steht in einer Sprache `%1$s` und in der anderen `%2$d`, gibt es zur Laufzeit eine Ausnahme,
 *    und zwar nur bei den Nutzern dieser einen Sprache.
 */
class StringResourceParityTest {

    private val basis = File("src/main/res")
    private val englisch = File(basis, "values/strings.xml")
    private val deutsch = File(basis, "values-de/strings.xml")

    /** name -> Inhalt. Eintraege mit `translatable="false"` bleiben aussen vor. */
    private fun lese(datei: File, nurUebersetzbare: Boolean): Map<String, String> {
        val text = datei.readText()
        val ergebnis = linkedMapOf<String, String>()

        val string = Pattern.compile(
            """<string\s+name="([^"]+)"([^>]*)>(.*?)</string>""",
            Pattern.DOTALL
        ).matcher(text)
        while (string.find()) {
            val attribute = string.group(2).orEmpty()
            if (nurUebersetzbare && attribute.contains("translatable=\"false\"")) continue
            ergebnis[string.group(1)] = string.group(3)
        }

        // Plurals zaehlen genauso - eine fehlende Mehrzahlform ist derselbe Fehler.
        val plural = Pattern.compile(
            """<plurals\s+name="([^"]+)"([^>]*)>(.*?)</plurals>""",
            Pattern.DOTALL
        ).matcher(text)
        while (plural.find()) {
            val attribute = plural.group(2).orEmpty()
            if (nurUebersetzbare && attribute.contains("translatable=\"false\"")) continue
            ergebnis[plural.group(1)] = plural.group(3)
        }
        return ergebnis
    }

    /** Alle Formatplatzhalter einer Zeichenkette, etwa `%1${'$'}s` oder `%2${'$'}d`. */
    private fun platzhalter(inhalt: String): Set<String> =
        Regex("""%\d+\$[a-zA-Z]|%[a-zA-Z]""").findAll(inhalt).map { it.value }.toSet()

    @Test
    fun `beide Ressourcendateien existieren`() {
        assertTrue("${englisch.absolutePath} fehlt", englisch.isFile)
        assertTrue("${deutsch.absolutePath} fehlt", deutsch.isFile)
    }

    @Test
    fun `jeder uebersetzbare Eintrag hat eine deutsche Fassung`() {
        val fehlen = lese(englisch, nurUebersetzbare = true).keys - lese(deutsch, nurUebersetzbare = false).keys
        assertEquals(
            "Ohne deutsche Fassung (Android faellt dafuer still auf Englisch zurueck): $fehlen",
            emptySet<String>(),
            fehlen
        )
    }

    @Test
    fun `die deutsche Fassung enthaelt keine unbekannten Eintraege`() {
        val ueberzaehlig = lese(deutsch, nurUebersetzbare = false).keys - lese(englisch, nurUebersetzbare = false).keys
        assertEquals(
            "In values-de, aber nicht in values - vermutlich ein Tippfehler im Namen: $ueberzaehlig",
            emptySet<String>(),
            ueberzaehlig
        )
    }

    /**
     * Der gefaehrlichste Fall: abweichende Platzhalter erzeugen zur Laufzeit eine Ausnahme, und
     * zwar ausschliesslich bei den Nutzern der betroffenen Sprache. Auf dem Rechner des
     * Entwicklers faellt so etwas nie auf.
     */
    @Test
    fun `Formatplatzhalter stimmen in beiden Sprachen ueberein`() {
        val en = lese(englisch, nurUebersetzbare = true)
        val de = lese(deutsch, nurUebersetzbare = false)
        val abweichungen = en.mapNotNull { (name, inhalt) ->
            val andere = de[name] ?: return@mapNotNull null
            val a = platzhalter(inhalt)
            val b = platzhalter(andere)
            if (a == b) null else "$name: en=$a de=$b"
        }
        assertEquals("Abweichende Platzhalter: $abweichungen", emptyList<String>(), abweichungen)
    }
}
