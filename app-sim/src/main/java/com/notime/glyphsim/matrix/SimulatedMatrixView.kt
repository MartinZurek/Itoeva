package com.notime.glyphsim.matrix

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.min

private val PUCK_COLOR = Color(MatrixColors.PUCK)
private val PUCK_RIM_COLOR = Color(MatrixColors.PUCK_RIM)
private val LED_OFF_COLOR = Color(MatrixColors.LED_OFF)
private val LED_ON_COLOR = Color(MatrixColors.LED_ON)

/**
 * Zeichnet die simulierte Glyph-Matrix: schwarze runde "Puck"-Flaeche mit einem
 * 13x13-Punktraster (siehe [MatrixGeometry] fuer die Kreisform/Annaeherung an die
 * echte Anordnung). [frame] ist ein IntArray der Laenge SIZE*SIZE mit
 * Helligkeitswerten 0..4095 pro Zelle, exakt dasselbe Format wie
 * GlyphMatrixManager.setMatrixFrame(int[]) im :app-Modul erwartet - Animationen und
 * die Uhr lassen sich dadurch 1:1 zwischen Hardware- und simulierter Anzeige teilen.
 */
/**
 * [showPuck] zeichnet das runde Gehaeuse mit Rand - sinnvoll dort, wo die Uhr als Nachbildung
 * der echten Glyph-Matrix-Hardware auftritt (Home-Screen-Widget, Vorschaubilder in den
 * Einstellungen). In der App selbst stoert es: dort liegt die Uhr auf schwarzem Grund, der
 * Puck-Kreis waere ein sichtbarer dunkler Kasten um die Punkte herum und schnitte ausserdem
 * alles ab, was ueber seinen Radius hinausragt. Deshalb dort abschaltbar - die Punkte
 * schweben dann frei auf dem Hintergrund.
 */
@Composable
fun SimulatedMatrixView(
    frame: IntArray,
    modifier: Modifier = Modifier,
    showPuck: Boolean = true,
    /**
     * Beschreibung fuer TalkBack (z.B. die aktuelle Uhrzeit oder der Name einer gerade
     * laufenden Erinnerungs-Animation) - der Canvas selbst hat sonst keinerlei Bedeutung fuer
     * einen Screenreader. Null bei rein dekorativen Vorschauen (Bibliothek, Einstellungen),
     * wo der Inhalt bereits durch benachbarten Text erschlossen ist.
     */
    contentDescription: String? = null
) {
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            // **Kein quadratischer Hintergrund - der Puck ist rund.**
            //
            // Hier lag eine schwarze Flaeche ueber dem GANZEN Zeichenbereich, also auch ueber den
            // vier Ecken ausserhalb des runden Gehaeuses. Solange das Dock nur aus Schwarz
            // bestand, war das unsichtbar. Seit dahinter eine Wohnung liegt, schob die Uhr beim
            // Ziehen ein schwarzes Rechteck vor Moebel und Figur her - man sah die Kanten eines
            // Vierecks, das es gar nicht geben sollte.
            //
            // Ersatzlos: Der Puck-Kreis darunter wird ohnehin gefuellt gezeichnet, die Flaeche
            // hat also nie etwas beigetragen ausser diesen Ecken. Auf hellen Karten (Bibliothek,
            // Einstellungen) sieht das runde Geraet jetzt sogar richtiger aus als ein
            // schwarzes Kaestchen.
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            )
    ) {
        drawMatrix(frame, showPuck)
    }
}

private fun DrawScope.drawMatrix(frame: IntArray, showPuck: Boolean) {
    val diameter = min(size.width, size.height)
    val radius = diameter / 2f
    val center = Offset(size.width / 2f, size.height / 2f)

    // Runder schwarzer "Puck" mit duennem Rand, wie das echte Matrix-Gehaeuse.
    if (showPuck) {
        drawCircle(color = PUCK_COLOR, radius = radius, center = center)
        drawCircle(
            color = PUCK_RIM_COLOR,
            radius = radius - 1.dp.toPx() / 2,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
    }

    // Punkteraster minimal kleiner als der Puck rendern (statt cell = diameter/SIZE):
    // sonst liegt der Mittelpunkt der aeussersten LEDs exakt auf dem Puck-Rand (gleicher
    // Radius wie MatrixGeometry.isActive) und die Haelfte ihres Durchmessers ragt ueber
    // den runden Rand hinaus. MatrixCellSizing rechnet den Puffer aus dem Punktradius-
    // Faktor (0.36) so, dass auch die aeusserste LED komplett innerhalb des Puck-Kreises
    // bleibt.
    val cell = diameter / MatrixCellSizing.EFFECTIVE_GRID_UNITS
    val dotRadius = cell * MatrixCellSizing.DOT_RADIUS_FACTOR
    val gridOrigin = Offset(
        center.x - MatrixGeometry.SIZE / 2f * cell,
        center.y - MatrixGeometry.SIZE / 2f * cell
    )

    for ((x, y) in MatrixGeometry.activeCells) {
        val brightness = frame.getOrElse(y * MatrixGeometry.SIZE + x) { 0 }
        val fraction = (brightness.coerceIn(0, MatrixGeometry.MAX_BRIGHTNESS)).toFloat() / MatrixGeometry.MAX_BRIGHTNESS
        val dotColor = if (fraction <= 0f) {
            LED_OFF_COLOR
        } else {
            lerpColor(LED_OFF_COLOR, LED_ON_COLOR, fraction)
        }
        val dotCenter = Offset(
            gridOrigin.x + (x + 0.5f) * cell,
            gridOrigin.y + (y + 0.5f) * cell
        )
        drawCircle(color = dotColor, radius = dotRadius, center = dotCenter)
    }
}

private fun lerpColor(from: Color, to: Color, fraction: Float): Color = Color(
    red = from.red + (to.red - from.red) * fraction,
    green = from.green + (to.green - from.green) * fraction,
    blue = from.blue + (to.blue - from.blue) * fraction,
    alpha = 1f
)
