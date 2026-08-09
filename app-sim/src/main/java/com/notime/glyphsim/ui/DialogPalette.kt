package com.notime.glyphsim.ui

import androidx.compose.ui.graphics.Color

/**
 * Die dunkle Palette der Bogen-Dialoge (Pflegebuch, Assistent, KI-Import, Spielmodus-Einstieg).
 *
 * ## Warum an einer Stelle
 *
 * Dieselben vier bis fuenf Farbwerte standen wortgleich in fuenf Dateien, jeweils als private
 * Konstanten. Solange sich nichts aendert, faellt das nicht auf; aendert jemand einen Wert, hat er
 * ihn danach an vier Stellen NICHT geaendert, und die Dialoge sehen unterschiedlich aus, ohne dass
 * ein Test oder ein Blick ins Diff das zeigen wuerde.
 *
 * Der zweite Grund ist die Pruefbarkeit: Kontrast laesst sich nur gegen etwas pruefen, das man
 * ansprechen kann. `DialogPaletteContrastTest` rechnet die Verhaeltnisse nach WCAG aus und haelt
 * sie fest - private Konstanten in fuenf Dateien liessen sich dafuer gar nicht erreichen.
 *
 * ## Wieso nicht ueber das Material-Theme
 *
 * Diese Dialoge sind bewusst dunkel, unabhaengig davon, ob das System hell oder dunkel steht: sie
 * zeigen die simulierte Glyph-Matrix, und die lebt von leuchtenden Punkten auf schwarzem Grund.
 * Ein helles Blatt darum herum wuerde den Eindruck zerstoeren. Sie folgen dem Theme deshalb
 * absichtlich nicht - und brauchen darum eine eigene, benannte Palette statt verstreuter Literale.
 */
internal object DialogPalette {

    /** Grund des Dialogblatts. */
    val SheetBackground = Color(0xFF101012)

    /** Etwas hellere Flaeche fuer Zeilen und Karten darauf. */
    val RowBackground = Color(0xFF1A1A1D)

    /** Sprechblasen des Avatars - eine Spur heller als [RowBackground]. */
    val BubbleBackground = Color(0xFF1E1E22)

    /** Lauftext. */
    val TextPrimary = Color(0xFFF1EEE6)

    /** Nebentext (Erlaeuterungen, Zeitangaben). Bewusst gedaempft, aber lesbar - siehe Kontrasttest. */
    val TextMuted = Color(0xFF9A968E)
}
