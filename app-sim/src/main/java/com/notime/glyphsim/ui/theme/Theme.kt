package com.notime.glyphsim.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.notime.glyphsim.ui.TamaPalette

/**
 * Das Material-Theme der App - **einer der Abnehmer von [TamaPalette], nicht eine zweite Quelle.**
 *
 * Hier standen dieselben fuenf Werte ein zweites Mal, mit dem Kommentar "dieselben Werte, die
 * Startbildschirm, Pflegebuch und Assistent bereits von Hand verwenden". Die Doppelung war also
 * bekannt und blieb trotzdem stehen - vermutlich, weil die Sammlung `DialogPalette` hiess und
 * damit nach etwas klang, das ein Theme nichts angeht. Sie heisst jetzt nach der App, und hier
 * wird sie gelesen statt abgeschrieben.
 *
 * **Warum kein Dynamic Color mehr:** die Farben kamen bis eben aus dem Hintergrundbild des
 * Nutzers (Material You). Das ergab je nach Bild einen goldenen oder rosa Untergrund, auf dem
 * Uhr und Avatar - beides Lichtpunkte auf Schwarz - nur mit einer eigenen dunklen Flaeche
 * lesbar waren. Genau diese Flaechen wirkten dann als Kasten bzw. Kreis um die Figuren. Eine
 * feste Palette ist hier keine Einschraenkung, sondern Voraussetzung: die App zeigt eine
 * simulierte LED-Matrix, und die lebt vom schwarzen Grund.
 *
 * **Warum immer dunkel, unabhaengig von der Systemeinstellung:** ein heller Modus haette
 * dasselbe Problem. Der Erinnerungs-Bildschirm folgte bislang dem System und stand dadurch
 * neben dem schwarzen Startbildschirm wie eine fremde App.
 */
private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD8C4),
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF1F4A40),
    onPrimaryContainer = Color(0xFFB6EEDD),
    secondary = Color(0xFFC2CAD9),
    onSecondary = Color(0xFF2A303B),
    tertiary = Color(0xFFEAB8CD),
    onTertiary = Color(0xFF3D2430),
    background = TamaPalette.Background,
    onBackground = TamaPalette.TextPrimary,
    surface = TamaPalette.SheetBackground,
    onSurface = TamaPalette.TextPrimary,
    surfaceVariant = TamaPalette.RowBackground,
    onSurfaceVariant = TamaPalette.TextMuted,
    outline = Color(0xFF3A3A40),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3A0906)
)

@Composable
fun GlyphSimTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
