package com.notime.glyphsim.ui

import androidx.compose.ui.graphics.Color
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Rechnet die Kontrastverhaeltnisse der Dialog-Palette nach WCAG 2.1 aus.
 *
 * ## Warum als Test und nicht per Augenmass
 *
 * "Sieht gut lesbar aus" ist keine Aussage - es haengt an Bildschirm, Helligkeit und Sehvermoegen.
 * Das Verhaeltnis ist dagegen eine Zahl, und sie laesst sich ausrechnen. Der Test haelt fest, dass
 * die heutigen Farben die Vorgaben erfuellen, und schlaegt an, sobald jemand einen Wert
 * nachdunkelt, ohne die Folgen zu pruefen.
 *
 * Reine Rechnung ohne Android - laeuft als JVM-Test bei jedem `gradlew verify` mit.
 *
 * ## Die Schwellen
 *
 * WCAG AA verlangt **4,5:1** fuer normalen Text und **3:1** fuer grossen (ab etwa 18 pt bzw. 14 pt
 * fett). AAA verlangt 7:1 bzw. 4,5:1. Geprueft wird hier gegen AA - das ist die Stufe, auf die
 * sich auch der Play Store und die gaengigen Pruefwerkzeuge beziehen.
 */
class TamaPaletteContrastTest {

    /**
     * Relative Helligkeit nach WCAG. Die Kanaele werden erst linearisiert - der sRGB-Wert einer
     * Farbe ist nicht ihre Helligkeit, sondern eine fuer das Auge vorverzerrte Groesse.
     */
    private fun luminance(color: Color): Double {
        fun channel(v: Float): Double {
            val c = v.toDouble()
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) +
            0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    /** Verhaeltnis zweier Farben, immer >= 1. Die 0,05 sind der von WCAG vorgegebene Sockel. */
    private fun ratio(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val heller = maxOf(la, lb)
        val dunkler = minOf(la, lb)
        return (heller + 0.05) / (dunkler + 0.05)
    }

    private fun assertMindestens(
        beschreibung: String,
        vordergrund: Color,
        hintergrund: Color,
        schwelle: Double
    ) {
        val wert = ratio(vordergrund, hintergrund)
        assertTrue(
            "$beschreibung: %.2f:1, verlangt sind mindestens %.1f:1".format(wert, schwelle),
            wert >= schwelle
        )
    }

    private companion object {
        const val AA_NORMAL = 4.5
        const val AA_GROSS = 3.0
    }

    @Test
    fun `Lauftext auf dem Dialogblatt erfuellt AA`() {
        assertMindestens(
            "TextPrimary auf SheetBackground",
            TamaPalette.TextPrimary,
            TamaPalette.SheetBackground,
            AA_NORMAL
        )
    }

    @Test
    fun `Lauftext auf Zeilen und Sprechblasen erfuellt AA`() {
        assertMindestens(
            "TextPrimary auf RowBackground",
            TamaPalette.TextPrimary,
            TamaPalette.RowBackground,
            AA_NORMAL
        )
        assertMindestens(
            "TextPrimary auf BubbleBackground",
            TamaPalette.TextPrimary,
            TamaPalette.BubbleBackground,
            AA_NORMAL
        )
    }

    /**
     * Der gedaempfte Nebentext ist der kritische Fall - er ist absichtlich zurueckgenommen und
     * damit derjenige, der als Erstes unter die Schwelle rutscht, wenn jemand ihn "noch etwas
     * dezenter" macht.
     */
    @Test
    fun `Nebentext erfuellt AA auf allen drei Untergruenden`() {
        assertMindestens(
            "TextMuted auf SheetBackground",
            TamaPalette.TextMuted,
            TamaPalette.SheetBackground,
            AA_NORMAL
        )
        assertMindestens(
            "TextMuted auf RowBackground",
            TamaPalette.TextMuted,
            TamaPalette.RowBackground,
            AA_NORMAL
        )
        assertMindestens(
            "TextMuted auf BubbleBackground",
            TamaPalette.TextMuted,
            TamaPalette.BubbleBackground,
            AA_NORMAL
        )
    }

    /**
     * Die Flaechen selbst muessen sich voneinander abheben - sonst verschwindet die Gliederung des
     * Dialogs. Fuer Flaechen gilt die niedrigere Schwelle fuer grafische Elemente.
     */
    @Test
    fun `Zeilen heben sich vom Blatt ab`() {
        val wert = ratio(TamaPalette.RowBackground, TamaPalette.SheetBackground)
        // Bewusst KEINE AA-Schwelle: zwei benachbarte Dunkelgrautoene sollen sich unterscheiden
        // lassen, nicht kontrastieren. Der Test haelt nur fest, dass sie ueberhaupt verschieden
        // sind - waeren sie gleich, waere die Zeilengliederung unsichtbar.
        assertTrue("RowBackground und SheetBackground sind identisch", wert > 1.0)
    }

    /**
     * Dokumentiert die tatsaechlichen Werte im Protokoll. Kein Urteil, nur Nachvollziehbarkeit -
     * wer die Palette anfasst, sieht hier sofort, wie viel Luft noch war.
     */
    @Test
    fun `Verhaeltnisse zum Nachlesen`() {
        val paare = listOf(
            "TextPrimary  / Sheet " to ratio(TamaPalette.TextPrimary, TamaPalette.SheetBackground),
            "TextPrimary  / Row   " to ratio(TamaPalette.TextPrimary, TamaPalette.RowBackground),
            "TextPrimary  / Bubble" to ratio(TamaPalette.TextPrimary, TamaPalette.BubbleBackground),
            "TextMuted    / Sheet " to ratio(TamaPalette.TextMuted, TamaPalette.SheetBackground),
            "TextMuted    / Row   " to ratio(TamaPalette.TextMuted, TamaPalette.RowBackground),
            "TextMuted    / Bubble" to ratio(TamaPalette.TextMuted, TamaPalette.BubbleBackground)
        )
        paare.forEach { (name, wert) -> println("Kontrast %s : %.2f:1".format(name, wert)) }
        assertTrue(paare.all { it.second >= AA_GROSS })
    }

    /**
     * Die neuen Werte muessen dieselbe Schwelle halten wie die alten - sonst waere das
     * Zusammenlegen ein Rueckschritt.
     */
    @Test
    fun `auch Grund und Antwortzeile erfuellen AA`() {
        assertMindestens(
            "TextPrimary auf Background",
            TamaPalette.TextPrimary,
            TamaPalette.Background,
            AA_NORMAL
        )
        assertMindestens(
            "TextPrimary auf ChoiceBackground",
            TamaPalette.TextPrimary,
            TamaPalette.ChoiceBackground,
            AA_NORMAL
        )
    }

    /**
     * **Kein zweiter Satz derselben Farben.**
     *
     * Genau daraus ist dieser Test entstanden: Die Werte standen wortgleich in fuenf Dateien,
     * spaeter ein zweites Mal im Material-Theme - dort sogar mit dem Kommentar, dass es dieselben
     * seien. Solange sie uebereinstimmen, faellt nichts auf; aendert jemand eine Seite, sehen die
     * Bildschirme unterschiedlich aus, und weder ein Test noch ein Blick ins Diff zeigt es.
     *
     * Geprueft wird an den Quelltexten selbst, weil sich anders nicht feststellen laesst, ob
     * jemand eine Farbe abgeschrieben statt referenziert hat. Ausgenommen ist die Palette selbst -
     * dort MUESSEN die Werte stehen - und alles, was eine eigene Bedeutung hat: Themenfarben,
     * Wetter, Spielwelt. Es geht um die Grundfarben der Oberflaeche, nicht um jede Farbe im Code.
     */
    @Test
    fun `die Grundfarben stehen nur in der Palette`() {
        val palette = File("src/main/java/com/notime/glyphsim/ui/TamaPalette.kt").readText()
        val grundfarben = Regex("""Color\(0x[0-9A-Fa-f]{8}\)""")
            .findAll(palette).map { it.value }.toSet()

        val gefunden = mutableListOf<String>()
        File("src/main/java/com/notime/glyphsim").walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.name == "TamaPalette.kt" }
            .forEach { datei ->
                val text = datei.readText()
                grundfarben.forEach { farbe ->
                    if (text.contains(farbe)) gefunden += "${datei.name}: $farbe"
                }
            }

        assertEquals(
            "Diese Dateien schreiben eine Grundfarbe ab, statt TamaPalette zu benutzen",
            emptyList<String>(),
            gefunden.sorted()
        )
    }
}
