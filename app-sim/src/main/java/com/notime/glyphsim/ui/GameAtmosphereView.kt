package com.notime.glyphsim.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.notime.glyphsim.matrix.*
import kotlin.math.roundToInt
import kotlin.math.sin

/** Begrenzte Zeilenverschiebung der vorhandenen Malerei; keine neuen Vollbild-Texturen. */
internal fun DrawScope.paintPaintedMotion(scene: GameScenes.Scene, image: ImageBitmap,
    fit: GameScenes.Fit, clock: Long, alpha: Float, images: Map<String, ImageBitmap?> = emptyMap()) {
    withTransform({ translate(fit.left, fit.top); scale(fit.scale, fit.scale, Offset.Zero) }) {
        for (patch in GameAtmosphere.patches(scene)) {
            val sample = GameAtmosphere.sample(scene, patch) ?: continue
            val source = if (sample.asset == scene.asset) image else images[sample.asset] ?: continue
            val sourceOffset = sample.offset
            val sx = source.width / sample.width
            val sy = source.height / sample.height
            clipRect(patch.x, patch.y, patch.x + patch.w, patch.y + patch.h) {
                repeat(12) { i ->
                    val y0 = patch.y + patch.h * i / 12f
                    val y1 = patch.y + patch.h * (i + 1) / 12f
                    val dx = if (patch.hanging) GameFabric.hangingOffset((i + .5f) / 12f, .5f,
                        GameAtmosphere.wind(GameWorld.origin(scene.place) + patch.x, clock) * .65f, clock, patch.seed)
                        else GameAtmosphere.bend(patch, (i + .5f) / 12f, clock, GameWorld.origin(scene.place))
                    // Umgebende Pixel werden mitgesampelt; es entsteht kein Loch am bewegten Rand.
                    val left = patch.x - 3f
                    val right = patch.x + patch.w + 3f
                    val x0 = ((left + sourceOffset) * sx).roundToInt().coerceIn(0, source.width - 1)
                    val x1 = ((right + sourceOffset) * sx).roundToInt().coerceIn(x0 + 1, source.width)
                    val py0 = (y0 * sy).roundToInt().coerceIn(0, source.height - 1)
                    val py1 = (y1 * sy).roundToInt().coerceIn(py0 + 1, source.height)
                    withTransform({ translate(dx, 0f) }) {
                        drawImage(source, IntOffset(x0, py0), IntSize(x1 - x0, py1 - py0),
                            IntOffset(left.roundToInt(), y0.roundToInt()),
                            IntSize((right - left).roundToInt(), (y1.roundToInt() - y0.roundToInt()).coerceAtLeast(1)),
                            alpha = alpha, filterQuality = FilterQuality.Low)
                    }
                }
            }
        }
    }
}

/** Regungen folgen Boden, Ufer und Licht; Kamera und aktiver Ortsname verschieben sie nicht. */
internal fun DrawScope.paintAtmosphere(scene: GameScenes.Scene, fit: GameScenes.Fit,
    clock: Long, minute: Int, alpha: Float, foreground: Boolean) {
    if (!GameWorld.isWorld(scene)) return
    val span = GameWorld.region(scene.place)!!.section
    val origin = GameWorld.origin(scene.place)
    val day = GameSceneLighting.daylight(minute)
    withTransform({ translate(fit.left, fit.top); scale(fit.scale, fit.scale, Offset.Zero) }) {
        if (foreground && scene.asset == "world/coast.png") repeat(24) { i ->
            val x = (i + .5f) * span / 24f
            val shore = GameWorld.shoreY(origin + x)
            val y = shore + 12f + (i % 4) * 22f
            if (GameWorld.material(scene, x, y) != GameEnvironment.Material.WATER) return@repeat
            val phase = clock / 1600f + (origin + x) / 45f
            val crest = sin(phase)
            val width = 10f + (i % 5) * 4f
            val shine = (.025f + day * .12f) * (.3f + .7f * kotlin.math.abs(crest)) * alpha
            drawLine(Color(0xFFE0EADB).copy(alpha = shine), Offset(x - width / 2f + crest * 2f, y),
                Offset(x + width / 2f + crest * 2f, y + sin(phase + 1f)), 1.1f)
            if (foreground && i % 6 == 0) drawOval(Color(0xFFABC9C9).copy(alpha = shine * .5f),
                Offset(x - 15f, y - 1f), Size(30f, 4f), style = Stroke(.7f))
        }
        val mist = GameAtmosphere.mist(scene.place, minute, clock) * alpha
        if (foreground && mist > 0f) {
            val drift = sin(clock / 12000f + origin / 1900f) * 20f
            drawOval(Brush.radialGradient(listOf(Color(0xFFB9D5D2).copy(alpha = mist), Color.Transparent),
                Offset(span * .5f + drift, 452f), span * .55f),
                Offset(-span * .05f + drift, 405f), Size(span * 1.1f, 100f))
        }
        if (foreground && scene.place in setOf(PlayScene.Place.PARK, PlayScene.Place.MEADOW,
                PlayScene.Place.FOREST, PlayScene.Place.VILLAGE_EDGE)) repeat(10) { i ->
            val x = span * (i + .5f) / 10f
            val y = 576f + i % 4 * 3f
            val sway = GameAtmosphere.wind(origin + x, clock) * 3f
            val light = (1f - GameSceneLighting.darknessAt(scene, x, minute)) * alpha
            drawLine(Color(0xFF71834A).copy(alpha = .50f * light), Offset(x, y),
                Offset(x + sway, y - 7f - i % 3), 1f)
        }
    }
}
