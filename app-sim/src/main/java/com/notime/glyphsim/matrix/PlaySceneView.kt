package com.notime.glyphsim.matrix

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

private val LED_ON_COLOR = Color(MatrixColors.LED_ON)

/**
 * Zeichnet die Kulisse ([PlayScene]) hinter Uhr und Avatar - im selben Zellraster und derselben
 * Zellgroesse wie das Avatar-Sprite ([AvatarSpriteView]), damit Figur und Welt sichtbar zur
 * selben Pixelwelt gehoeren. Eine abweichende Zellgroesse waere sofort als zwei uebereinander
 * gelegte Grafiken erkennbar gewesen.
 *
 * **Interpoliert von SCHWARZ, nicht von [MatrixColors.LED_OFF]** - anders als [AvatarSpriteView].
 * Dort ist LED_OFF richtig, weil das Sprite die Optik echter, nur abgedunkelter LEDs nachbildet.
 * Hier waere derselbe Startpunkt fatal: LED_OFF (#232323) ist auf schwarzem Grund bereits gut
 * sichtbar, eine Kulissenzelle mit 10 % Helligkeit kaeme also nicht dunkler heraus als eine mit
 * 60 %. Genau die Staffelung (Boden schwach, Moebel mittel, Licht hell), von der die ganze
 * Tiefenwirkung lebt, waere damit eingeebnet.
 *
 * Nicht anfassbar: Die Kulisse traegt bewusst keinerlei Gesten - sie liegt unter Uhr und Avatar
 * und darf weder das Ziehen der Uhr noch die Fuetter-Geste abfangen.
 */
@Composable
fun PlaySceneView(
    cells: List<SceneCell>,
    cellPx: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (cellPx <= 0f) return@Canvas
        // Derselbe winzige Ueberlapp wie im Avatar-Sprite: sonst zeichnet das Antialiasing
        // haarfeine Luecken zwischen zwei Zellen, was besonders am durchgehenden Bodenstrich
        // als gestrichelte Linie auffiele.
        val cellSize = Size(cellPx + 0.75f, cellPx + 0.75f)
        for (cell in cells) {
            if (cell.brightness <= 0) continue
            val fraction = cell.brightness
                .coerceIn(0, AvatarGeometry.MAX_BRIGHTNESS)
                .toFloat() / AvatarGeometry.MAX_BRIGHTNESS
            drawRect(
                color = Color(
                    red = LED_ON_COLOR.red * fraction,
                    green = LED_ON_COLOR.green * fraction,
                    blue = LED_ON_COLOR.blue * fraction,
                    alpha = 1f
                ),
                topLeft = Offset(cell.x * cellPx, cell.y * cellPx),
                size = cellSize
            )
        }
    }
}
