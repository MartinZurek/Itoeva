package com.notime.glyphsim.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.notime.glyphsim.matrix.AvatarGeometry
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.AvatarSpriteView
import kotlin.math.roundToInt

/**
 * Wie hoch [AvatarSpriteView] innerhalb einer quadratischen Kreisflaeche stehen darf, ohne dass
 * eine ihrer vier Ecken ueber den Kreisrand hinausragt - bei einem 16:20-Seitenverhaeltnis
 * bleiben 0,781 des Durchmessers die rechnerische Grenze; 0,75 laesst spuerbar Luft.
 */
private const val SPRITE_HEIGHT_FRACTION = 0.75f

/**
 * Eine Traumblase ueber dem schlafenden Avatar.
 *
 * Der echte Avatar wird hier nie bewegt. In der Blase lebt nur eine kleine Projektion aus einer
 * vorhandenen Reaktionsanimation. Das haelt Traum und Weltzustand sauber getrennt und macht spaeter
 * Interaktionen wie einen Ballon moeglich, ohne den Schlafzustand selbst zu verletzen.
 */
@Composable
internal fun PlayDreamBubble(
    frame: IntArray,
    species: AvatarSpecies,
    dreamingAvatarOffset: Offset,
    dreamingAvatarSizeDp: Float,
    progress: Float,
    maxWidthPx: Float
) {
    val density = LocalDensity.current
    val p = progress.coerceIn(0f, 1f)
    val avatarPx = with(density) { dreamingAvatarSizeDp.dp.toPx() }
    val bubbleSizeDp = dreamingAvatarSizeDp * (0.50f + p * 0.55f)
    val bubblePx = with(density) { bubbleSizeDp.dp.toPx() }
    val centerX = (dreamingAvatarOffset.x + avatarPx * (0.85f + p * 0.30f))
        .coerceIn(bubblePx / 2f, (maxWidthPx - bubblePx / 2f).coerceAtLeast(bubblePx / 2f))
    val centerY = (dreamingAvatarOffset.y - avatarPx * (0.15f + p * 1.55f))
        .coerceAtLeast(bubblePx / 2f)
    val left = centerX - bubblePx / 2f
    val top = centerY - bubblePx / 2f

    // Zwei kleine Vorblasen verbinden Bett und Hauptblase optisch. Sie wachsen mit, ohne eine
    // weitere Zustandsmaschine zu brauchen.
    Canvas(modifier = Modifier.fillMaxSize()) {
        val alpha = 0.24f + p * 0.20f
        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.55f),
            radius = bubblePx * 0.075f,
            center = Offset(
                dreamingAvatarOffset.x + avatarPx * 0.72f,
                dreamingAvatarOffset.y - avatarPx * (0.08f + p * 0.28f)
            )
        )
        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.72f),
            radius = bubblePx * 0.12f,
            center = Offset(
                dreamingAvatarOffset.x + avatarPx * 0.82f,
                dreamingAvatarOffset.y - avatarPx * (0.18f + p * 0.55f)
            )
        )
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = bubblePx / 2f,
            center = Offset(centerX, centerY),
            style = Stroke(width = (bubblePx * 0.035f).coerceAtLeast(1f))
        )
    }

    Box(
        modifier = Modifier
            .size(bubbleSizeDp.dp)
            .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.035f)),
        contentAlignment = Alignment.Center
    ) {
        // **Die Avatar-Ansicht, nicht die Matrix-Ansicht.** [frame] ist eine ganz gewoehnliche
        // Kreatur-Reaktion aus [com.notime.glyphsim.matrix.AvatarAnimations.reactionFor] - auf
        // dem 16x20-[AvatarGeometry]-Raster, nicht auf dem 13x13-[com.notime.glyphsim.matrix
        // .MatrixGeometry]-Raster der Uhr-/Zeichen-Symbole. Hier stand bis zu diesem Fund
        // [com.notime.glyphsim.matrix.SimulatedMatrixView] - dieselbe falsche Kombination, vor
        // der [AvatarSpriteView] fuer den umgekehrten Fall ausdruecklich warnt ("wurden dabei
        // zerschert, weil hier mit der Zeilenbreite des Avatars gelesen wird"): Ein 320 Zellen
        // langes Array in einen 169 Zellen breiten Leser gegeben, gelesen mit der falschen
        // Zeilenbreite - sichtbar als Pixelsalat statt als Traumszene (gemeldet 2026-09-16).
        //
        // **Explizite Hoehe/Breite statt `fillMaxSize()`.** Die Box oben gibt bereits eine
        // straffe quadratische Groesse vor (`.size(bubbleSizeDp.dp)`); ein Aspect-Ratio-Modifier
        // kann eine bereits straffe Groesse nicht mehr veraendern (siehe [AvatarClipPlayer] fuer
        // dasselbe Muster mit expliziter Breite/Hoehe statt eines Aspect-Ratio-Modifiers).
        // `fillMaxSize()` liesse [AvatarSpriteView] deshalb quadratisch messen; die vier
        // HEADROOM-Zeilen am oberen Rand des 16x20-Rasters schieben die eigentliche Figur dann
        // unten aus dem Quadrat heraus, wo sie vom Kreisausschnitt der Blase abgeschnitten wird
        // (gefunden per Review an genau dieser Stelle).
        val spriteHeight = bubbleSizeDp.dp * SPRITE_HEIGHT_FRACTION
        val spriteWidth = spriteHeight * AvatarGeometry.SIZE.toFloat() / AvatarGeometry.HEIGHT
        AvatarSpriteView(
            frame = IntArray(frame.size) {
                (frame[it] * 0.82f).toInt().coerceIn(0, AvatarGeometry.MAX_BRIGHTNESS)
            },
            species = species,
            showBackground = false,
            contentDescription = null,
            modifier = Modifier.width(spriteWidth).height(spriteHeight)
        )
    }
}
