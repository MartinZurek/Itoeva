package com.notime.glyphsim.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.notime.glyphsim.matrix.AvatarSpriteView
import kotlin.math.roundToInt

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
    sleepingAvatarOffset: Offset,
    sleepingAvatarSizeDp: Float,
    progress: Float,
    maxWidthPx: Float
) {
    val density = LocalDensity.current
    val p = progress.coerceIn(0f, 1f)
    val avatarPx = with(density) { sleepingAvatarSizeDp.dp.toPx() }
    val bubbleSizeDp = sleepingAvatarSizeDp * (0.50f + p * 0.55f)
    val bubblePx = with(density) { bubbleSizeDp.dp.toPx() }
    val centerX = (sleepingAvatarOffset.x + avatarPx * (0.85f + p * 0.30f))
        .coerceIn(bubblePx / 2f, (maxWidthPx - bubblePx / 2f).coerceAtLeast(bubblePx / 2f))
    val centerY = (sleepingAvatarOffset.y - avatarPx * (0.15f + p * 1.55f))
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
                sleepingAvatarOffset.x + avatarPx * 0.72f,
                sleepingAvatarOffset.y - avatarPx * (0.08f + p * 0.28f)
            )
        )
        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.72f),
            radius = bubblePx * 0.12f,
            center = Offset(
                sleepingAvatarOffset.x + avatarPx * 0.82f,
                sleepingAvatarOffset.y - avatarPx * (0.18f + p * 0.55f)
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
            .background(Color.White.copy(alpha = 0.035f))
    ) {
        AvatarSpriteView(
            frame = frame,
            showBackground = false,
            brightnessScale = 0.82f,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}
