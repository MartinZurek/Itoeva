package com.notime.glyphsim.matrix

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

private val LED_OFF_COLOR = Color(MatrixColors.LED_OFF)
private val LED_ON_COLOR = Color(MatrixColors.LED_ON)

/**
 * Zeichnet den Tamagotchi-Avatar als eckige Pixelgrafik direkt auf dem schwarzen
 * Dock-Hintergrund - bewusst KEIN runder "Puck" wie [SimulatedMatrixView] (der Avatar
 * ist keine Uhr/Matrix-Hardware-Nachbildung, siehe [AvatarGeometry]). Jede Zelle von
 * [frame] wird als gefuelltes Quadrat gezeichnet statt als isoliertem LED-Punkt,
 * dadurch wirkt die Kreatur wie ein zusammenhaengendes Sprite. Zellen mit Helligkeit 0
 * werden komplett ausgelassen (statt gedimmt gezeichnet wie bei der Matrix) - dadurch
 * bleibt nur die Kreatur-Silhouette sichtbar, kein flaechiges Raster drumherum.
 */
/**
 * [showBackground] fuellt die Sprite-Flaeche schwarz. Gebraucht wird das nur dort, wo der Avatar
 * auf hellem Grund steht und sich sonst nicht abheben wuerde (Vorschaubilder im
 * Einstellungs-Dialog). Auf den Bildschirmen selbst liegt er ohnehin auf Schwarz - dort wirkt
 * die Fuellung als sichtbarer Kasten um die Figur und schneidet zudem alles ab, was ueber ihr
 * Quadrat hinausreicht, weshalb sie dort abgeschaltet wird.
 */
@Composable
fun AvatarSpriteView(
    frame: IntArray,
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
    /**
     * Beschreibung fuer TalkBack (Avatar-Name + Stimmung) - siehe [SimulatedMatrixView] fuer
     * dieselbe Begruendung. Null bei rein dekorativen Vorschauen.
     */
    contentDescription: String? = null,
    /**
     * Daempfung der gesamten Figur (1 = voll, kleiner = zurueckgenommen).
     *
     * Gebraucht fuer BESUCHER: Stehen zwei Kreaturen voreinander, verschmelzen zwei gleich helle
     * Silhouetten zu einem einzigen unlesbaren Fleck - man erkennt keine von beiden. Eine
     * durchgehend etwas schwaechere Zeichnung des Gastes loest das an der Wurzel: Sie liest sich
     * als groesserer Abstand zum Betrachter, und der eigene Avatar bleibt in JEDER Lage die
     * hellste Figur im Bild. Das wirkt auch dann noch, wenn sich eine Ueberdeckung nicht
     * vermeiden laesst.
     */
    brightnessScale: Float = 1f
) {
    Canvas(
        modifier = modifier
            // Hoeher als breit: Das Raster hat oberhalb der Figur Kopffreiheit, damit Spruenge
            // nicht abgeschnitten werden (siehe AvatarGeometry.HEADROOM).
            .aspectRatio(AvatarGeometry.SIZE.toFloat() / AvatarGeometry.HEIGHT)
            .then(if (showBackground) Modifier.background(Color.Black) else Modifier)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            )
    ) {
        drawSprite(frame, brightnessScale)
    }
}

private fun DrawScope.drawSprite(frame: IntArray, brightnessScale: Float) {
    val cell = size.width / AvatarGeometry.SIZE
    // Winziger Ueberlapp zwischen benachbarten Zellen, damit Antialiasing keine
    // sichtbaren Ein-Pixel-Spalten zwischen zwei eigentlich zusammenhaengenden
    // Sprite-Zellen erzeugt.
    val cellSize = Size(cell + 0.75f, cell + 0.75f)
    for (y in 0 until AvatarGeometry.HEIGHT) {
        for (x in 0 until AvatarGeometry.SIZE) {
            val brightness = frame.getOrElse(y * AvatarGeometry.SIZE + x) { 0 }
            if (brightness <= 0) continue
            val fraction = (brightness.coerceIn(0, AvatarGeometry.MAX_BRIGHTNESS).toFloat() /
                AvatarGeometry.MAX_BRIGHTNESS) * brightnessScale.coerceIn(0f, 1f)
            drawRect(
                color = lerpColor(LED_OFF_COLOR, LED_ON_COLOR, fraction),
                topLeft = Offset(x * cell, y * cell),
                size = cellSize
            )
        }
    }
}

private fun lerpColor(from: Color, to: Color, fraction: Float): Color = Color(
    red = from.red + (to.red - from.red) * fraction,
    green = from.green + (to.green - from.green) * fraction,
    blue = from.blue + (to.blue - from.blue) * fraction,
    alpha = 1f
)
