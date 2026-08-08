package com.notime.glyphkalender.matrix

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.min

private val PUCK_COLOR = Color(0xFF0A0A0A)
private val PUCK_RIM_COLOR = Color(0xFF2A2A2A)
private val LED_OFF_COLOR = Color(0xFF232323)
private val LED_ON_COLOR = Color(0xFFF3F1EA)

/**
 * Zeigt eine Erinnerungs-Animation direkt im Handy-Display, statt dass man das Handy
 * umdrehen muss, um die echte Glyph Matrix auf der Rueckseite zu sehen - genutzt im
 * Anlegen/Bearbeiten-Dialog beim Antippen von "Play preview" (siehe
 * ui/ReminderScreen.kt). Zeichnet dieselbe runde "Puck"-Optik wie SimulatedMatrixView
 * im :app-sim-Modul, rein zur Vorschau - loest NICHT die echte Hardware aus.
 */
@Composable
fun MatrixPreviewView(frame: IntArray, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color.Black)
    ) {
        drawMatrix(frame)
    }
}

private fun DrawScope.drawMatrix(frame: IntArray) {
    val diameter = min(size.width, size.height)
    val radius = diameter / 2f
    val center = Offset(size.width / 2f, size.height / 2f)

    drawCircle(color = PUCK_COLOR, radius = radius, center = center)
    drawCircle(
        color = PUCK_RIM_COLOR,
        radius = radius - 1.dp.toPx() / 2,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )

    // Punkteraster minimal kleiner als der Puck rendern (siehe MatrixCellSizing) - sonst
    // liegt der Mittelpunkt der aeussersten LEDs exakt auf dem Puck-Rand und die Haelfte
    // ihres Durchmessers ragt ueber den runden Rand hinaus.
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
