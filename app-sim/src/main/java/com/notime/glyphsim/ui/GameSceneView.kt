package com.notime.glyphsim.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.notime.glyphsim.matrix.GameEnvironment
import com.notime.glyphsim.matrix.GameCamera
import com.notime.glyphsim.matrix.GameWorld
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
internal fun rememberGameSceneImages(enabled: Boolean): Map<String, ImageBitmap?> {
    val assets = LocalContext.current.assets
    return remember(assets, enabled) {
        if (!enabled) emptyMap() else
            (GameWorld.regions.map { it.asset } + com.notime.glyphsim.matrix.GameInteriorCatalog.scenes.values.map { it.asset })
                .distinct().associateWith { asset -> runCatching {
                    assets.open(asset).use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
                }.getOrNull() }
    }
}

/**
 * Die Bewegungs- und Lichtebenen eines Ortes (siehe `tools/world-art/animate.py`):
 * [strip] - mehrere Bilder nebeneinander, in denen Laub, Wasser und Lichter sich bewegen;
 * [glow] - was nachts leuchtet (Lampen, Fenster, Feuer) samt Lichthof. Beides fehlt bei Orten
 * ohne diese Dateien; dann bleibt das ruhende Bild.
 */
class GameSceneLayers(val strip: ImageBitmap?, val glow: ImageBitmap?) {
    val frames: Int get() = strip?.let { (it.width / GameScenes.IMAGE_W).coerceAtLeast(1) } ?: 1
}

@Composable
fun rememberGameSceneLayers(scene: GameScenes.Scene?, animated: Boolean = true): GameSceneLayers {
    val context = LocalContext.current
    return remember(scene?.asset, animated) {
        if (scene != null && (GameWorld.isWorld(scene) || scene.asset.startsWith("interiors/"))) return@remember GameSceneLayers(null, null)
        fun load(path: String): ImageBitmap? = runCatching {
            context.assets.open(path).use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
        }.getOrNull()
        val base = scene?.asset?.removeSuffix(".png")
        GameSceneLayers(base?.takeIf { animated }?.let { load("${it}_anim.png") }, base?.let { load("${it}_glow.png") })
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
    modifier: Modifier = Modifier,
    layers: GameSceneLayers? = null,
    roomLayers: GameRoomLayers? = null,
    environment: GameEnvironment.State = GameEnvironment.State(),
    camera: GameCamera.State? = null,
    images: Map<String, ImageBitmap?> = emptyMap()
) {
    if (GameWorld.isWorld(scene) && camera != null) {
        GameWorldView(scene, images, camera, minuteOfDay, lampOn, phase, environment, avatarPos, fade, modifier)
        return
    }
    Canvas(modifier = modifier) {
        val fit = camera?.let { GameCamera.fit(it, scene, size.width, size.height) } ?: GameScenes.fit(scene, size.width, size.height, avatarPos.x, avatarPos.depth)
        val visible = fade.coerceIn(0f, 1f)
        val lights = GameSceneLighting.sources(scene, minuteOfDay, lampOn, tvOn, phase)
        fun point(x: Float, y: Float): Offset {
            val (sx, sy) = fit.toScreen(x, y)
            return Offset(sx, sy)
        }
        drawRect(Color.Black)
        // Bewegung: aus dem Streifen das Bild zum Takt (Laub wiegt, Wasser kraeuselt, Licht flackert).
        val live = roomLayers?.takeIf { it.base != null && it.atlas != null }
        val strip = if (live == null) layers?.strip else null
        val frame = if (strip != null) Math.floorMod(phase, layers?.frames ?: 1) else 0
        drawImage(
            image = live?.base ?: strip ?: image,
            srcOffset = IntOffset(frame * GameScenes.IMAGE_W, 0),
            srcSize = if (live == null && strip == null) IntSize(image.width, image.height) else IntSize(GameScenes.IMAGE_W, GameScenes.IMAGE_H),
            dstOffset = IntOffset(fit.left.roundToInt(), fit.top.roundToInt()),
            dstSize = IntSize(
                (GameScenes.IMAGE_W * fit.scale).roundToInt(),
                (GameScenes.IMAGE_H * fit.scale).roundToInt()
            ),
            alpha = visible,
            filterQuality = if (scene.place in com.notime.glyphsim.matrix.GameInteriorCatalog.scenes) FilterQuality.Low else FilterQuality.None
        )
        if (live != null) {
            roomParts(scene, live, environment, fit, visible, false)
            roomShore(scene, live, environment, fit, visible)
            roomContacts(scene, environment, fit, visible)
        }
        // Die Illustration bleibt die Materialbasis. Licht und Schatten werden dagegen in
        // denselben Bildkoordinaten wie Laufweg und Avatar pro Bildtakt berechnet.
        val dark = GameSceneLighting.darkness(scene, minuteOfDay)
        // Abendrot und Morgenrot: warmer Schleier um Sonnenauf- und -untergang (nur draussen).
        val dusk = GameSceneLighting.dusk(scene, minuteOfDay)
        if (dusk > 0f) drawRect(Color(0xFFFF8A4A).copy(alpha = dusk * 0.16f * visible))
        drawRect(Color(0xFF0A1030).copy(alpha = dark * visible))
        // Was leuchtet, bleibt hell: Lampen, Fenster, Feuer ueber die Abdunkelung legen.
        layers?.glow?.let { glow ->
            val glowAlpha = (dark / GameSceneLighting.MAX_DARK).coerceIn(0f, 1f)
            if (glowAlpha > 0.02f) drawImage(
                image = glow,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(glow.width, glow.height),
                dstOffset = IntOffset(fit.left.roundToInt(), fit.top.roundToInt()),
                dstSize = IntSize(
                    (GameScenes.IMAGE_W * fit.scale).roundToInt(),
                    (GameScenes.IMAGE_H * fit.scale).roundToInt()
                ),
                alpha = glowAlpha * visible * (0.9f + 0.1f * sin(phase * 0.9f)),
                filterQuality = if (scene.place in com.notime.glyphsim.matrix.GameInteriorCatalog.scenes) FilterQuality.Low else FilterQuality.None
            )
        }
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
    }
}
