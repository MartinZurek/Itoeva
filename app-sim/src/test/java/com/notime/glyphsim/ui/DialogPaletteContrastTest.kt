package com.notime.glyphsim.ui

import androidx.compose.ui.graphics.Color
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
class DialogPaletteContrastTest {

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
            DialogPalette.TextPrimary,
            DialogPalette.SheetBackground,
            AA_NORMAL
        )
    }

    @Test
    fun `Lauftext auf Zeilen und Sprechblasen erfuellt AA`() {
        assertMindestens(
            "TextPrimary auf RowBackground",
            DialogPalette.TextPrimary,
            DialogPalette.RowBackground,
            AA_NORMAL
        )
        assertMindestens(
            "TextPrimary auf BubbleBackground",
            DialogPalette.TextPrimary,
            DialogPalette.BubbleBackground,
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
            DialogPalette.TextMuted,
            DialogPalette.SheetBackground,
            AA_NORMAL
        )
        assertMindestens(
            "TextMuted auf RowBackground",
            DialogPalette.TextMuted,
            DialogPalette.RowBackground,
            AA_NORMAL
        )
        assertMindestens(
            "TextMuted auf BubbleBackground",
            DialogPalette.TextMuted,
            DialogPalette.BubbleBackground,
            AA_NORMAL
        )
    }

    /**
     * Die Flaechen selbst muessen sich voneinander abheben - sonst verschwindet die Gliederung des
     * Dialogs. Fuer Flaechen gilt die niedrigere Schwelle fuer grafische Elemente.
     */
    @Test
    fun `Zeilen heben sich vom Blatt ab`() {
        val wert = ratio(DialogPalette.RowBackground, DialogPalette.SheetBackground)
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
            "TextPrimary  / Sheet " to ratio(DialogPalette.TextPrimary, DialogPalette.SheetBackground),
            "TextPrimary  / Row   " to ratio(DialogPalette.TextPrimary, DialogPalette.RowBackground),
            "TextPrimary  / Bubble" to ratio(DialogPalette.TextPrimary, DialogPalette.BubbleBackground),
            "TextMuted    / Sheet " to ratio(DialogPalette.TextMuted, DialogPalette.SheetBackground),
            "TextMuted    / Row   " to ratio(DialogPalette.TextMuted, DialogPalette.RowBackground),
            "TextMuted    / Bubble" to ratio(DialogPalette.TextMuted, DialogPalette.BubbleBackground)
        )
        paare.forEach { (name, wert) -> println("Kontrast %s : %.2f:1".format(name, wert)) }
        assertTrue(paare.all { it.second >= AA_GROSS })
    }
}
