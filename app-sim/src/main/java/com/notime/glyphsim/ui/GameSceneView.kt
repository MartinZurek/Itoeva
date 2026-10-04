package com.notime.glyphsim.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.notime.glyphsim.matrix.GameScenes
import com.notime.glyphsim.matrix.GameSceneLighting
import com.notime.glyphsim.matrix.PlayControl
import com.notime.glyphsim.matrix.PlayScene
import kotlin.math.sin
import kotlin.math.roundToInt

/**
 * Laedt das gemalte Bild eines Ortes (siehe [GameScenes]) - einmal je Ort. `null`, wenn es in
 * dieser Variante keines gibt: Die Bilder liegen nur in der Spiel-Variante.
 */
@Composable
fun rememberGameSceneImage(scene: GameScenes.Scene?): ImageBitmap? {
    val context = LocalContext.current
    return remember(scene?.asset) {
        scene?.let { s ->
            runCatching {
                context.assets.open(s.asset).use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
            }.getOrNull()
        }
    }
}

/**
 * Zeichnet das gemalte Bild bildschirmfuellend, **ohne Glaettung** - jeder Bildpixel bleibt ein
 * scharfes Quadrat, sonst waere es keine Pixel-Art mehr. [fade] blendet beim Ortswechsel ins
 * Schwarze.
 */
@Composable
fun GameSceneView(
    scene: GameScenes.Scene,
    image: ImageBitmap,
    fade: Float,
    minuteOfDay: Int,
    lampOn: Boolean,
    tvOn: Boolean,
    avatarPos: PlayControl.Pos,
    phase: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val fit = GameScenes.fit(scene, size.width, size.height, avatarPos.x, avatarPos.depth)
        val visible = fade.coerceIn(0f, 1f)
        val lights = GameSceneLighting.sources(scene, minuteOfDay, lampOn, tvOn, phase)
        fun point(x: Float, y: Float): Offset {
            val (sx, sy) = fit.toScreen(x, y)
            return Offset(sx, sy)
        }
        drawRect(Color.Black)
        drawImage(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(image.width, image.height),
            dstOffset = IntOffset(fit.left.roundToInt(), fit.top.roundToInt()),
            dstSize = IntSize(
                (GameScenes.IMAGE_W * fit.scale).roundToInt(),
                (GameScenes.IMAGE_H * fit.scale).roundToInt()
            ),
            alpha = visible,
            filterQuality = FilterQuality.None
        )
        // Die Illustration bleibt die Materialbasis. Licht und Schatten werden dagegen in
        // denselben Bildkoordinaten wie Laufweg und Avatar pro Bildtakt berechnet.
        drawRect(Color.Black.copy(alpha = GameSceneLighting.darkness(scene, minuteOfDay) * visible))
        lights.forEach { light ->
            val center = point(light.x, light.y)
            val radius = light.radius * fit.scale
            val tint = when (light.tone) {
                GameSceneLighting.Tone.SUN -> Color(0xFFFFEDC6)
                GameSceneLighting.Tone.WARM -> Color(0xFFFFB96A)
                GameSceneLighting.Tone.COOL -> Color(0xFF78C8E6)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tint.copy(alpha = light.power * 0.55f), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center,
                alpha = visible
            )
        }
        val shadow = GameSceneLighting.shadow(scene, avatarPos, lights)
        val foot = point(shadow.footX, shadow.footY)
        val tip = point(shadow.tipX, shadow.tipY)
        drawLine(Color(0x990E1720), foot, tip, strokeWidth = shadow.width * fit.scale, alpha = visible)
        drawOval(
            color = Color(0x660B1821),
            topLeft = Offset(foot.x - shadow.width * fit.scale, foot.y - 2f * fit.scale),
            size = androidx.compose.ui.geometry.Size(shadow.width * 2f * fit.scale, 5f * fit.scale),
            alpha = visible
        )
        // Kleine Bewegungen auf der Materialebene, ohne zufaellige Pixel pro Rekomposition.
        if (scene.place == PlayScene.Place.FOREST || scene.place == PlayScene.Place.JUNGLE) {
            repeat(7) { i ->
                val x = 35f + i * 67f + sin(phase * 0.11f + i * 1.7f) * 8f
                val y = 65f + ((phase * 0.6f + i * 29f) % 145f)
                val p = point(x, y)
                drawRect(Color(0x88C6A66C), p, androidx.compose.ui.geometry.Size(2f * fit.scale, 2f * fit.scale), alpha = visible)
            }
        }
        if (scene.place == PlayScene.Place.POND || scene.place == PlayScene.Place.BEACH) {
            repeat(5) { i ->
                val p = point(45f + i * 91f + sin(phase * 0.16f + i) * 7f, 225f + i % 2 * 8f)
                drawLine(Color(0x88B7DAE7), p, Offset(p.x + 7f * fit.scale, p.y),
                    strokeWidth = fit.scale, alpha = visible)
            }
        }
    }
}
