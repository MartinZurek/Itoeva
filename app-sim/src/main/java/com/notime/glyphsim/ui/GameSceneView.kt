package com.notime.glyphsim.ui

import android.graphics.BitmapFactory
import android.graphics.Bitmap
import com.notime.glyphsim.matrix.GameGroundLight
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val images by produceState<Map<String, ImageBitmap?>>(emptyMap(), assets, enabled) {
        value = withContext(Dispatchers.Default) {
        if (!enabled) emptyMap() else
            (GameWorld.regions.map { it.asset } + GameWorld.seams.map { it.asset } + com.notime.glyphsim.matrix.GameInteriorCatalog.scenes.values.map { it.asset })
                .distinct().associateWith { asset -> runCatching {
                    assets.open(asset).use { BitmapFactory.decodeStream(it) }?.let { source ->
                        val field = GameGroundLight.estimate(asset) { u,v ->
                            source.getPixel((u * source.width).toInt().coerceAtMost(source.width-1),
                                (v * source.height).toInt().coerceAtMost(source.height-1))
                        }
                        val bitmap = if (source.isMutable) source else source.copy(Bitmap.Config.ARGB_8888, true)
                        // Nur kleine Zeilenpuffer; pro Bildtakt findet keine Pixelverarbeitung statt.
                        val row = IntArray(bitmap.width)
                        for (y in 0 until bitmap.height) {
                            val v = (y+.5f)/bitmap.height
                            if (v < .71f || v > .99f) continue
                            bitmap.getPixels(row,0,bitmap.width,0,y,bitmap.width,1)
                            for (x in row.indices) {
                                val u=(x+.5f)/bitmap.width
                                val gain=field.gain(u,v)
                                if (kotlin.math.abs(gain-1f) > .002f && GameGroundLight.floor(asset,u,v))
                                    row[x]=GameGroundLight.apply(row[x],gain)
                            }
                            bitmap.setPixels(row,0,bitmap.width,0,y,bitmap.width,1)
                        }
                        if (source !== bitmap) source.recycle()
                        bitmap.asImageBitmap()
                    }
                }.getOrNull() }
        }
    }
    return images
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
    images: Map<String, ImageBitmap?> = emptyMap(),
    clock: Long = 0L
) {
    if (GameWorld.isWorld(scene) && camera != null) {
        GameWorldView(scene, images, camera, minuteOfDay, lampOn, phase, environment, avatarPos, fade, modifier, clock)
        return
    }
    Canvas(modifier = modifier) {
        val fit = camera?.let { GameCamera.fit(it, scene, size.width, size.height) } ?: GameScenes.fit(scene, size.width, size.height, avatarPos.x, avatarPos.depth)
        val visible = fade.coerceIn(0f, 1f)
        val lights = GameSceneLighting.sources(scene, minuteOfDay, lampOn, tvOn, (clock / 200L).toInt())
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
        paintPaintedMotion(scene, image, fit, clock, visible)
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
        paintSceneLights(scene, lights, fit, clock, visible)
    }
}
