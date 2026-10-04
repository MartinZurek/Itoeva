package com.notime.glyphsim.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.notime.glyphsim.matrix.GameScenes
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
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val fit = GameScenes.fit(scene, size.width, size.height)
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
            alpha = fade.coerceIn(0f, 1f),
            filterQuality = FilterQuality.None
        )
    }
}
