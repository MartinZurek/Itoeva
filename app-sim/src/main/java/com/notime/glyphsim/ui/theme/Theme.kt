package com.notime.glyphsim.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Feste dunkle Palette - dieselben Werte, die Startbildschirm, Pflegebuch und Assistent bereits
 * von Hand verwenden.
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
    // Der Grund, auf dem die Matrix sitzt - praktisch schwarz, aber nicht ganz, damit sich
    // Flaechen wie Karten und Dialoge minimal abheben koennen.
    background = Color(0xFF0A0A0B),
    onBackground = Color(0xFFF1EEE6),
    surface = Color(0xFF101012),
    onSurface = Color(0xFFF1EEE6),
    surfaceVariant = Color(0xFF1A1A1D),
    onSurfaceVariant = Color(0xFF9A968E),
    outline = Color(0xFF3A3A40),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3A0906)
)

@Composable
fun GlyphSimTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
