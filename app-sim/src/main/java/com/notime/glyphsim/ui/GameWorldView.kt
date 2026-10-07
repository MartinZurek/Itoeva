package com.notime.glyphsim.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.notime.glyphsim.matrix.*
import kotlin.math.roundToInt
import kotlin.math.sin

/** Ein einziges Panorama: an Ortsgrenzen wird weder das Bild geladen noch ausgetauscht. */
@Composable
internal fun GameWorldView(scene: GameScenes.Scene, image: ImageBitmap, camera: GameCamera.State,
    minute: Int, lampOn: Boolean, phase: Int, environment: GameEnvironment.State,
    pos: PlayControl.Pos, fade: Float, modifier: Modifier) {
    Canvas(modifier) {
        val local = GameCamera.fit(camera, scene, size.width, size.height)
        val fit = local.copy(left = local.left - GameWorld.origin(scene.place) * local.scale)
        drawImage(image, IntOffset.Zero, IntSize(image.width, image.height),
            IntOffset(fit.left.roundToInt(), fit.top.roundToInt()),
            IntSize((GameWorld.WIDTH * fit.scale).roundToInt(), (GameWorld.HEIGHT * fit.scale).roundToInt()),
            alpha = fade.coerceIn(0f, 1f), filterQuality = FilterQuality.Low)
        val dark = GameSceneLighting.darkness(scene, minute)
        val dusk = GameSceneLighting.dusk(scene, minute)
        if (dusk > 0f) drawRect(Color(0xFFFF9C57).copy(alpha = dusk * .12f * fade))
        drawRect(Color(0xFF0A1030).copy(alpha = dark * fade))
        GameWorld.places.forEach { place ->
            val section = GameWorld.scene(place)!!
            for (light in GameSceneLighting.sources(section, minute, lampOn, false, phase)
                .filter { it.tone != GameSceneLighting.Tone.SUN }) {
                val (x, y) = fit.toScreen(GameWorld.origin(place) + light.x, light.y)
                val center = Offset(x, y)
                val radius = light.radius * fit.scale
                drawCircle(Brush.radialGradient(listOf(Color(0xFFEDBC76).copy(alpha = light.power * fade), Color.Transparent),
                    center, radius), radius, center)
            }
        }
        val (x, y) = GameScenes.feet(scene, pos)
        val (sx, sy) = local.toScreen(x, y)
        drawOval(Color(0x660B1821).copy(alpha = .4f * fade), Offset(sx - 15f * local.scale, sy - 2f * local.scale),
            Size(30f * local.scale, 6f * local.scale))
        roomContacts(scene, environment, local, (1f - dark) * fade, false)
    }
}

/** Ortsfeste Regungen bleiben beim Schwenken an der Landschaft verankert. */
@Composable
internal fun GameWorldForeground(scene: GameScenes.Scene, camera: GameCamera.State,
    environments: Map<PlayScene.Place, GameEnvironment.State>, clock: Long, minute: Int,
    fade: Float, modifier: Modifier) {
    Canvas(modifier) {
        val local = GameCamera.fit(camera, scene, size.width, size.height)
        val fit = local.copy(left = local.left - GameWorld.origin(scene.place) * local.scale)
        val dark = GameSceneLighting.darkness(scene, minute)
        withTransform({ translate(fit.left, fit.top); scale(fit.scale, fit.scale, Offset.Zero) }) {
            repeat(46) { i ->
                val x = 475f + i * 31f
                val y = 571f + (i % 4) * 4f
                val sway = sin(clock / 750f + i * .7f) * 2.4f
                drawLine(Color(0xFF798C47).copy(alpha = (1f - dark) * .8f * fade), Offset(x, y),
                    Offset(x + sway, y - 9f - i % 3), 1.2f)
            }
            repeat(14) { i ->
                val time = clock / 1000f
                val x = 500f + ((i * 103f + time * 12f) % 1400f)
                val y = ((i * 67f + time * (10f + i % 3)) % 515f)
                drawOval(Color(0xFFBEA758).copy(alpha = .7f * (1f - dark) * fade),
                    Offset(x + sin(time + i) * 8f, y), Size(3.5f, 2f))
            }
        }
        environments.filterKeys(GameWorld::contains).forEach { (place, state) ->
            roomContacts(GameWorld.scene(place)!!, state, GameCamera.fit(camera, GameWorld.scene(place)!!,
                size.width, size.height), (1f - dark) * fade, true)
        }
    }
}
