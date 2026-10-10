package com.notime.glyphsim.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.notime.glyphsim.matrix.GameEnvironment
import com.notime.glyphsim.matrix.GameRoomCatalog
import com.notime.glyphsim.matrix.GameSceneLighting
import com.notime.glyphsim.matrix.GameCamera
import com.notime.glyphsim.matrix.GameWorld
import com.notime.glyphsim.matrix.GameScenes
import com.notime.glyphsim.matrix.PlayControl
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Drei kleine Dateien pro Ort, einmal geladen; keine Bildbearbeitung im Zeichentakt. */
class GameRoomLayers(val base: ImageBitmap?, val atlas: ImageBitmap?, val grid: GameEnvironment.Grid?)

@Composable
fun rememberGameRoomLayers(scene: GameScenes.Scene?): GameRoomLayers {
    val context = LocalContext.current
    return remember(scene?.asset) {
        if (scene != null && (GameWorld.isWorld(scene) || scene.asset.startsWith("interiors/"))) return@remember GameRoomLayers(null, null, null)
        val stem = scene?.asset?.removeSuffix(".png")
        fun load(suffix: String): ImageBitmap? = stem?.let {
            runCatching { context.assets.open("${it}_$suffix.png").use { BitmapFactory.decodeStream(it) }?.asImageBitmap() }.getOrNull()
        }
        val material = scene?.let { GameRoomCatalog.rooms[it.place]?.material } ?: GameEnvironment.Material.STONE
        val grid = stem?.let {
            runCatching {
                val bytes = context.assets.open("${it}_materials.bin").use { stream -> stream.readBytes() }
                GameEnvironment.Grid(bytes, material)
            }.getOrNull()
        }
        GameRoomLayers(load("live"), load("parts"), grid)
    }
}

/** Wird unter demselben Fit wie Boden, Avatar und Trefferflaechen gezeichnet. */
internal fun DrawScope.roomParts(scene: GameScenes.Scene, layers: GameRoomLayers,
    state: GameEnvironment.State, fit: GameScenes.Fit, alpha: Float, foreground: Boolean, dark: Float = 0f) {
    val atlas = layers.atlas ?: return
    val room = GameRoomCatalog.rooms[scene.place] ?: return
    withTransform({ translate(fit.left, fit.top); scale(fit.scale, fit.scale, Offset.Zero) }) {
        clipRect(0f, 0f, 480f, 270f) {
            for (part in room.parts) {
                if (part.foreground != foreground) continue
                val pose = GameEnvironment.pose(part, state.clock)
                fun paint(dx: Float = pose.dx, dy: Float = pose.dy) {
                    drawImage(atlas, IntOffset(part.sx, part.sy), IntSize(part.w, part.h),
                        IntOffset((part.x + dx).roundToInt(), (part.y + dy).roundToInt()), IntSize(part.w, part.h),
                        alpha = alpha, filterQuality = FilterQuality.None,
                        colorFilter = if (dark > 0f) ColorFilter.tint(Color(0xFF0A1030).copy(alpha = dark), BlendMode.SrcAtop) else null)
                }
                if (part.kind == GameEnvironment.Kind.WATER) {
                    clipRect(part.x.toFloat(), part.y.toFloat(), (part.x + part.w).toFloat(), (part.y + part.h).toFloat()) {
                        paint()
                        // Umlaufende Zeilen bleiben auch an der Verschiebekante vollstaendig.
                        paint(pose.dx - part.w)
                        paint(pose.dx + part.w)
                        val t = state.clock / 1000f
                        val crest = part.x + ((t * 13f + part.seed * 37f) % part.w)
                        drawLine(Color(0x99DEECE1), Offset(crest, part.y + 1f),
                            Offset(crest + 4f + sin(t + part.seed) * 2f, part.y + 1f), 1f, alpha = alpha * .55f)
                    }
                } else {
                    val pivot = if (part.kind == GameEnvironment.Kind.CURTAIN)
                        Offset(part.x + part.w / 2f, part.y.toFloat())
                        else Offset(part.x + part.w / 2f, (part.y + part.h).toFloat())
                    rotate(pose.angle, pivot) { paint() }
                }
            }
        }
    }
}

internal fun DrawScope.roomContacts(scene: GameScenes.Scene, state: GameEnvironment.State,
    fit: GameScenes.Fit, alpha: Float, foreground: Boolean = false) {
    withTransform({ translate(fit.left, fit.top); scale(fit.scale, fit.scale, Offset.Zero) }) {
        clipRect(0f, 0f, if (GameWorld.isWorld(scene)) GameWorld.region(scene.place)!!.section else 480f, GameWorld.height(scene)) {
            state.contacts.filter { it.place == scene.place }.forEach { c ->
                val age = (state.clock - c.born).coerceAtLeast(0L)
                val seconds = age / 1000f
                val fade = (1f - age.toFloat() / c.life).coerceIn(0f, 1f) * alpha
                if (c.material == GameEnvironment.Material.WATER) {
                    if (!foreground) {
                        repeat(if (c.impact > 0f) 3 else 2) { ring ->
                            val radius = 2f + seconds * (10f + c.impact * 4f) - ring * 3f
                            if (radius > 0f) drawOval(Color(0xFFA7D6D3), Offset(c.x - radius, c.y - radius * .28f),
                                Size(radius * 2, radius * .56f), alpha = fade * .75f, style = Stroke(.75f))
                        }
                    }
                    // Tropfen fliegen mit Gravitation, anstatt am Fuss als Bild zu kleben.
                    if (foreground && seconds < .75f) {
                        val count = if (c.impact > 0f) 12 else 4
                        repeat(count) { i ->
                            val angle = i * PI.toFloat() * 2f / count + c.foot
                            val power = 9f + c.impact * 11f
                            val x = c.x + cos(angle) * power * seconds
                            val y = c.y + sin(angle) * power * .22f * seconds - power * seconds + 36f * seconds * seconds
                            drawRect(Color(0xFFE4F3E5), Offset(x.roundToInt().toFloat(), y.roundToInt().toFloat()),
                                Size(if (c.impact > 0f) 2f else 1f, 2f), alpha = fade * (1f - seconds / .75f))
                        }
                    }
                } else if (!foreground) {
                    when (c.material) {
                        GameEnvironment.Material.MUD, GameEnvironment.Material.SAND, GameEnvironment.Material.SNOW -> {
                            val ink = when (c.material) {
                                GameEnvironment.Material.MUD -> Color(0xFF322C21)
                                GameEnvironment.Material.SNOW -> Color(0xFF698A9A)
                                else -> Color(0xFF96764D)
                            }
                            val heading = (kotlin.math.atan2(c.dy, c.dx) * 180f / PI.toFloat()) - 90f
                            rotate(heading, Offset(c.x, c.y)) {
                                drawOval(ink, Offset(c.x - 1.6f, c.y - 2.6f), Size(3.2f, 5.2f), alpha = fade * .55f)
                                repeat(3) { toe -> drawRect(ink, Offset(c.x - 1.5f + toe, c.y - 3.6f), Size(.8f, 1f), alpha = fade * .4f) }
                                drawLine(Color(0x99E7CFA3), Offset(c.x - 1.8f, c.y), Offset(c.x - 1.8f, c.y + 2f), .7f, alpha = fade * .5f)
                            }
                        }
                        GameEnvironment.Material.GRASS -> repeat(5) { i ->
                            val side = if (i % 2 == 0) -1f else 1f
                            drawLine(Color(0xFFA4B263), Offset(c.x, c.y),
                                Offset(c.x + side * (3f + seconds * 2f), c.y - 2f + sin(seconds * 10f + i)), .8f, alpha = fade * .6f)
                        }
                        else -> Unit
                    }
                    if (seconds < .6f && (c.impact > 0f || c.material != GameEnvironment.Material.GRASS)) {
                        repeat(if (c.impact > 0f) 8 else 3) { i ->
                            val side = (i - 3.5f) / 3.5f
                            drawRect(Color(0xFFD5C39A), Offset(c.x + side * seconds * (10f + c.impact * 6f),
                                c.y - seconds * 10f + seconds * seconds * 18f), Size(1f, 1f), alpha = fade * .6f)
                        }
                    }
                }
            }
        }
    }
}

/** Brandung laeuft zum vorhandenen Ufer; Materialraster schuetzt Felsen, Sand und Liegestuhl. */
internal fun DrawScope.roomShore(scene: GameScenes.Scene, layers: GameRoomLayers,
    state: GameEnvironment.State, fit: GameScenes.Fit, alpha: Float) {
    if (scene.place != com.notime.glyphsim.matrix.PlayScene.Place.BEACH) return
    val grid = layers.grid ?: return
    withTransform({ translate(fit.left, fit.top); scale(fit.scale, fit.scale, Offset.Zero) }) {
        repeat(3) { wave ->
            val travel = ((state.clock / 1000f * .22f + wave / 3f) % 1f)
            for (x in 145..479 step 4) {
                val shore = 155f + (x - 145) * .054f
                val y = shore - (1f - travel) * 22f + sin(x * .034f + wave) * 1.4f
                if (grid.at(x.toFloat(), y) == GameEnvironment.Material.WATER &&
                    grid.at(x + 3f, y) == GameEnvironment.Material.WATER) {
                    val foam = sin(travel * PI.toFloat()).coerceAtLeast(0f) * (.35f + travel * .35f)
                    drawLine(Color(0xFFE8F0DA), Offset(x.toFloat(), y.roundToInt().toFloat()),
                        Offset(x + 3f, y.roundToInt().toFloat()), 1f, alpha = alpha * foam)
                }
            }
        }
    }
}

/** Vorderes Gras verdeckt die Fuesse, Tropfen und Blaetter laufen vor der Figur vorbei. */
@Composable
fun GameRoomForegroundView(scene: GameScenes.Scene, layers: GameRoomLayers,
    state: GameEnvironment.State, avatarPos: PlayControl.Pos, fade: Float, minuteOfDay: Int,
    modifier: Modifier = Modifier, camera: GameCamera.State? = null) {
    if (GameWorld.isWorld(scene)) return
    Canvas(modifier) {
        val fit = camera?.let { GameCamera.fit(it, scene, size.width, size.height) } ?: GameScenes.fit(scene, size.width, size.height, avatarPos.x, avatarPos.depth)
        val alpha = fade.coerceIn(0f, 1f)
        roomParts(scene, layers, state, fit, alpha, true, GameSceneLighting.darkness(scene, minuteOfDay))
        roomContacts(scene, state, fit, alpha * (1f - GameSceneLighting.darkness(scene, minuteOfDay)), true)
        val parts = GameRoomCatalog.rooms[scene.place]?.parts ?: return@Canvas
        withTransform({ translate(fit.left, fit.top); scale(fit.scale, fit.scale, Offset.Zero) }) {
            clipRect(0f, 0f, 480f, 270f) {
                if (parts.any { it.kind == GameEnvironment.Kind.LEAF } && com.notime.glyphsim.matrix.PlayScene.isOutdoors(scene.place)) {
                    repeat(9) { i ->
                        val t = state.clock / 1000f
                        val y = ((t * (8f + i % 3) + i * 29f) % 285f) - 10f
                        val x = (i * 59f + t * 4f + sin(t * .9f + i) * 14f) % 480f
                        rotate(sin(t * 2f + i) * 55f, Offset(x, y)) {
                            drawRect(Color(0xFFC0A56C), Offset(x, y), Size(3f, 1f), alpha = alpha * .65f)
                            drawRect(Color(0xFF708F54), Offset(x + 1, y - 1), Size(1f, 3f), alpha = alpha * .6f)
                        }
                    }
                }
                parts.filter { it.kind == GameEnvironment.Kind.FIRE }.forEach { fire ->
                    repeat(5) { i ->
                        val t = (state.clock / 1000f + i * .63f) % 3f
                        drawRect(Color(0xFFFFC16A), Offset(fire.x + fire.w / 2f + sin(t * 3f + i) * 5f,
                            fire.y + fire.h / 2f - t * 17f), Size(1f, 2f), alpha = alpha * (1f - t / 3f))
                    }
                }
            }
        }
    }
}
