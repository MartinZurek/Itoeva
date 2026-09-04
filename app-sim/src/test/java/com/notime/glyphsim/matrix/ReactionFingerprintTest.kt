package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.DefaultLibraryAnimations
import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Ein Fingerabdruck jeder Reaktion, die es gibt - der Stolperdraht fuer den Umbau auf den
 * Animations-Baum.**
 *
 * Der Umbau tauscht aus, WORUEBER eine Reaktion gefunden wird (bisher der Name der Animation,
 * kuenftig ihr Knoten im Baum samt Rueckfall nach oben, siehe [AvatarReactions]). Was dabei
 * herauskommt, soll sich nicht aendern - und genau das laesst sich an dieser Sorte Code sonst
 * kaum pruefen: Eine Bildfolge ist eine Liste von Zahlen, eine falsch gewaehlte Choreografie
 * sieht auf den ersten Blick genauso plausibel aus wie die richtige. Ein Vertauschen faellt beim
 * Zuschauen erst auf, wenn man weiss, was man erwartet hatte.
 *
 * Deshalb liegt der erwartete Stand als Datei daneben (`src/test/reaction-fingerprint.txt`), eine
 * Zeile je Motiv, und zwar **vor** dem Umbau erzeugt. Schlaegt dieser Test fehl, nennt er die
 * Zeilen, die sich unterscheiden - dann ist eine Reaktion umgezogen, und die Ursache gehoert
 * gesucht, nicht die Datei angepasst.
 *
 * ## Wann die Datei doch angefasst wird
 *
 * "Nicht anpassen" gilt fuer die Frage, auf die dieser Test antwortet: *Hat ein Umbau
 * unbeabsichtigt eine Reaktion verschoben?* Es gibt aber einen zweiten Fall, in dem sich eine Zeile
 * aendern MUSS - naemlich wenn ein Motiv absichtlich eine neue oder erstmals eine eigene
 * Choreografie bekommt. Ohne eine Aenderung an dieser Datei liesse sich eine Animation nie
 * verbessern.
 *
 * Die Grenze verlaeuft nicht zwischen "darf" und "darf nicht", sondern zwischen **benannt** und
 * **stillschweigend**. Wer eine Zeile aendert, schuldet drei Dinge: dass sich **genau** die Zeilen
 * aendern, die zu den bearbeiteten Motiven gehoeren (jede weitere ist der Fehler, den dieser Test
 * sucht); dass die Liste im Commit steht; und dass der Grund dort steht.
 *
 * So geschehen am 2026-09-04 fuer `LIB:Basketball`, `LIB:Dribble`, `LIB:Shot` und `LIB:Trophy`:
 * Alle vier trugen bis dahin **denselben** Abdruck `9f76c0f83beb` - sie spielten Bild fuer Bild
 * dieselbe geerbte Gruppen-Antwort wie `sport/ballsport` selbst (siehe [AvatarMotifReactions] fuer
 * den Befund und SKILLBAUM.md P16). Der gemeinsame alte Wert in vier Zeilen war dabei selbst der
 * Beleg: Vier verschiedene Motive duerfen nicht denselben Abdruck haben.
 *
 * Der Abdruck deckt alle sechs Spezies gleichzeitig ab: Jede Zeile ist der Hash ueber die
 * Bildfolgen UND die Standzeiten aller sechs. Damit faellt auch auf, wenn sich nur bei einer
 * Spezies etwas verschiebt - was der wahrscheinlichere Fehler ist, weil sich die Spezies genau im
 * Abschluss jeder Reaktion unterscheiden.
 */
class ReactionFingerprintTest {

    private val golden = File("src/test/reaction-fingerprint.txt")
    private val actual = File("build/reaction-fingerprint-actual.txt")

    /** Kurz genug zum Lesen, lang genug, dass zwei verschiedene Bildfolgen nicht kollidieren. */
    private fun digest(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray())
            .take(6)
            .joinToString("") { "%02x".format(it) }

    /**
     * Bildfolge und Standzeiten in eine Zeichenkette - beides, weil eine Reaktion mit denselben
     * Bildern, aber anderem Timing eine andere Reaktion ist.
     */
    private fun fingerprintOf(species: AvatarSpecies, trigger: ReactionTrigger): String {
        val reaction = AvatarAnimations.reactionFor(species, trigger)
        val frames = reaction.frames.joinToString(";") { frame -> frame.joinToString(",") }
        val holds = reaction.holdsMs.joinToString(",")
        val flight = AvatarAnimations.flightOffsetsFor(trigger)
            .joinToString(",") { (x, y) -> "$x/$y" }
        return "$frames|$holds|$flight"
    }

    private fun rowFor(key: String, type: AnimationType?, label: String?): String {
        val trigger = ReactionTrigger.of(type, label)
        val overAllSpecies = AvatarSpecies.entries.joinToString("#") { species ->
            fingerprintOf(species, trigger)
        }
        return "$key=${digest(overAllSpecies)}"
    }

    /**
     * Jeder Weg, auf dem eine Reaktion entstehen kann: die zwoelf eingebauten Typen, der Fall ohne
     * jedes Thema, und alle 56 Bibliotheks-Motive. Sortiert, damit die Datei stabil bleibt.
     */
    private fun currentFingerprint(): List<String> {
        val rows = mutableListOf<String>()
        for (type in AnimationType.entries.sortedBy { it.name }) {
            rows += rowFor("TYPE:${type.name}", type, null)
        }
        rows += rowFor("TYPE:NONE", null, null)
        for (label in DefaultLibraryAnimations.seed().map { it.label }.sorted()) {
            rows += rowFor("LIB:$label", null, label)
        }
        // Eine selbstgezeichnete Animation: Bibliotheks-Animation, aber an keinem Knoten. Sie
        // gehoert ausdruecklich dazu, weil sie der einzige Fall ist, in dem sich "es war eine
        // Bibliotheks-Animation" und "sie haengt im Baum" unterscheiden - und damit die Stelle,
        // an der ein Umbau der Aufrufsignatur am ehesten etwas verschiebt.
        rows += rowFor("LIB:__OHNE_KNOTEN__", null, "Selbstgemalter Kringel")
        return rows
    }

    @Test
    fun `keine Reaktion hat sich veraendert`() {
        val rows = currentFingerprint()
        actual.parentFile?.mkdirs()
        actual.writeText(rows.joinToString("\n") + "\n")

        assertTrue(
            "Der erwartete Stand fehlt. Die soeben erzeugte Fassung liegt unter " +
                "${actual.absolutePath} - pruefen und als ${golden.path} einchecken.",
            golden.exists()
        )

        val erwartet = golden.readLines().filter { it.isNotBlank() }
        val jetzt = rows

        // Zuerst die Menge der Schluessel: Ein fehlendes oder neues Motiv soll nicht als
        // "Hash unterschiedlich" auftauchen, sondern als das, was es ist.
        val keysErwartet = erwartet.map { it.substringBefore('=') }.toSet()
        val keysJetzt = jetzt.map { it.substringBefore('=') }.toSet()
        assertEquals("Diese Motive sind weggefallen", emptyList<String>(), (keysErwartet - keysJetzt).sorted())
        assertEquals("Diese Motive sind neu", emptyList<String>(), (keysJetzt - keysErwartet).sorted())

        val vorher = erwartet.associate { it.substringBefore('=') to it.substringAfter('=') }
        val nachher = jetzt.associate { it.substringBefore('=') to it.substringAfter('=') }
        val veraendert = keysErwartet
            .filter { vorher[it] != nachher[it] }
            .sorted()
            .map { "$it: ${vorher[it]} -> ${nachher[it]}" }

        assertEquals(
            "Diese Reaktionen sehen jetzt anders aus als vor dem Umbau",
            emptyList<String>(),
            veraendert
        )
    }
}
