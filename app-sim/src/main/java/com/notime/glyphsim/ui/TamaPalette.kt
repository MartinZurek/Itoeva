package com.notime.glyphsim.ui

import androidx.compose.ui.graphics.Color

/**
 * **Die Farben der App - an einer Stelle.**
 *
 * ## Warum das hier steht
 *
 * Dieselben vier bis fuenf Werte standen wortgleich in fuenf Dateien, jeweils als private
 * Konstanten. Solange sich nichts aendert, faellt das nicht auf; aendert jemand einen Wert, hat
 * er ihn danach an vier Stellen NICHT geaendert, und die Dialoge sehen unterschiedlich aus, ohne
 * dass ein Test oder ein Blick ins Diff das zeigen wuerde.
 *
 * **Und danach ging es genau so weiter.** Die Sammlung hiess `DialogPalette` und galt als "die
 * Palette der Dialoge" - waehrend das Material-Theme (`theme/Theme.kt`) dieselben fuenf Werte ein
 * zweites Mal auffuehrte, mit dem Kommentar "dieselben Werte, die Startbildschirm, Pflegebuch und
 * Assistent bereits von Hand verwenden". Es wusste also davon und liess es so. Dazu kamen
 * woertliche Kopien im Startbildschirm und im Clip-Player.
 *
 * Der Name war das eigentliche Problem: Eine "Dialog-Palette" laedt niemanden ein, das Theme
 * daraus zu speisen. Sie heisst deshalb jetzt nach der App und nicht nach einem ihrer
 * Verwendungsorte - und `Theme.kt` liest von hier.
 *
 * ## Pruefbarkeit
 *
 * Kontrast laesst sich nur gegen etwas pruefen, das man ansprechen kann. `TamaPaletteContrastTest`
 * rechnet die Verhaeltnisse nach WCAG aus und haelt sie fest; private Konstanten in fuenf Dateien
 * liessen sich dafuer gar nicht erreichen.
 *
 * ## Wieso nicht ueber das Material-Theme
 *
 * Umgekehrt gefragt: Warum liest nicht alles aus `MaterialTheme.colorScheme`? Weil ein Teil davon
 * ausserhalb einer Komposition gebraucht wird und weil die Dialoge bewusst dunkel sind,
 * unabhaengig von der Systemeinstellung - sie zeigen die simulierte Glyph-Matrix, und die lebt
 * von leuchtenden Punkten auf schwarzem Grund. Ein helles Blatt darum herum wuerde den Eindruck
 * zerstoeren. Die Palette ist deshalb die Quelle, das Theme einer ihrer Abnehmer.
 */
internal object TamaPalette {

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

    /**
     * Der Grund, auf dem die Matrix sitzt - praktisch schwarz, aber nicht ganz, damit sich
     * Flaechen wie Karten und Dialoge minimal abheben koennen.
     */
    val Background = Color(0xFF0A0A0B)

    /** Antippbare Antwort im Gespraech - eine Spur heller als eine Sprechblase. */
    val ChoiceBackground = Color(0xFF2A2A30)
}

/**
 * Die kleinste zulaessige Hoehe einer antippbaren Flaeche.
 *
 * Kein Gestaltungswert, sondern eine Vorgabe: Die Android-Bedienungshilfen nennen 48dp als
 * Mindestmass, und darunter trifft man mit dem Finger daneben - besonders mit Schaltersteuerung
 * oder eingeschraenkter Feinmotorik. Die Antwortzeilen im Gespraech kamen mit ihrer Innenabstand-
 * Rechnung auf gut 44dp und lagen damit knapp darunter; das faellt niemandem auf, der es nicht
 * misst.
 *
 * Als Konstante neben der Palette, weil es dieselbe Sorte Festlegung ist: eine Zahl, die an vielen
 * Stellen gilt und an keiner einzelnen begruendet werden kann.
 */
internal val MIN_TOUCH_TARGET_DP = 48
