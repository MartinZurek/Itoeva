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
internal fun GameWorldView(scene: GameScenes.Scene, images: Map<String,ImageBitmap?>, camera: GameCamera.State,
    minute: Int, lampOn: Boolean, phase: Int, environment: GameEnvironment.State,
    pos: PlayControl.Pos, fade: Float, modifier: Modifier) {
    Canvas(modifier) {
        val local = GameCamera.fit(camera, scene, size.width, size.height)
        val fit = local.copy(left = local.left - GameWorld.origin(scene.place) * local.scale)
        for (region in GameWorld.regions) {
            val image = images[region.asset] ?: continue
            val x = GameWorld.regionOrigin(region)
            if (fit.left+(x+region.width)*fit.scale < 0f || fit.left+x*fit.scale > size.width) continue
            drawImage(image, IntOffset.Zero, IntSize(image.width, image.height),
                IntOffset((fit.left+x*fit.scale).roundToInt(), fit.top.roundToInt()),
                IntSize((region.width*fit.scale).roundToInt(), (GameWorld.HEIGHT*fit.scale).roundToInt()),
                alpha = fade.coerceIn(0f,1f), filterQuality = FilterQuality.Low)
        }
        val dark = GameSceneLighting.darkness(scene, minute)
        val dusk = GameSceneLighting.dusk(scene, minute)
        if (dusk > 0f) drawRect(Color(0xFFFF9C57).copy(alpha = dusk * .12f * fade))
        // Die Hoehle bleibt dunkel, auch wenn der aktive Ortsname noch Lager ist.
        val visible = GameWorld.visiblePlaces(scene)

        withTransform({ translate(fit.left, fit.top); scale(fit.scale, fit.scale, Offset.Zero) }) {
            visible.forEachIndexed { index, place ->
                val here = GameSceneLighting.darkness(GameWorld.scene(place)!!, minute)
                val before = GameSceneLighting.darkness(GameWorld.scene(visible.getOrElse(index - 1) { place })!!, minute)
                val after = GameSceneLighting.darkness(GameWorld.scene(visible.getOrElse(index + 1) { place })!!, minute)
                val sectionWidth = GameWorld.region(place)!!.section
                val x = GameWorld.origin(place)
                drawRect(Brush.horizontalGradient(listOf(
                    Color(0xFF0A1030).copy(alpha = (here + before) * .5f * fade),
                    Color(0xFF0A1030).copy(alpha = here * fade),
                    Color(0xFF0A1030).copy(alpha = (here + after) * .5f * fade)), x, x + sectionWidth),
                    Offset(x, 0f), Size(sectionWidth, GameWorld.height(scene)))
            }
        }
        GameWorld.visiblePlaces(scene).forEach { place ->
            val section = GameWorld.scene(place)!!
            for (light in GameSceneLighting.sources(section, minute, lampOn, false, phase)
                .filter { it.tone != GameSceneLighting.Tone.SUN }) {
                val (x, y) = fit.toScreen(GameWorld.origin(place) + light.x, light.y)
                val center = Offset(x, y)
                val radius = light.radius * fit.scale
                drawCircle(Brush.radialGradient(listOf((if (light.tone == GameSceneLighting.Tone.COOL) Color(0xFF83D9FA) else Color(0xFFEDBC76)).copy(alpha = light.power * fade), Color.Transparent),
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
            for (region in GameWorld.regions) {
                val baseX = GameWorld.regionOrigin(region)
                if (fit.left + (baseX + region.width) * fit.scale < 0f || fit.left + baseX * fit.scale > size.width) continue
                if (region.asset != "world/coast.png") repeat(46) { i ->
                    val x = baseX + 475f + i * 31f
                    val y = 571f + (i % 4) * 4f
                    val sway = sin(clock / 750f + i * .7f) * 2.4f
                    drawLine(Color(0xFF798C47).copy(alpha = (1f - dark) * .8f * fade), Offset(x, y),
                        Offset(x + sway, y - 9f - i % 3), 1.2f)
                }
                repeat(14) { i ->
                    val time = clock / 1000f
                    val x = baseX + 500f + ((i * 103f + time * 12f) % 1400f)
                    if (region.asset == "world/expedition.png" && x >= baseX + 960f) return@repeat
                    val y = ((i * 67f + time * (10f + i % 3)) % 515f)
                    drawOval(Color(0xFFBEA758).copy(alpha = .7f * (1f - dark) * fade),
                        Offset(x + sin(time + i) * 8f, y), Size(3.5f, 2f))
                }
            }
        }
        environments.filterKeys { it in GameWorld.visiblePlaces(scene) }.forEach { (place, state) ->
            roomContacts(GameWorld.scene(place)!!, state, GameCamera.fit(camera, GameWorld.scene(place)!!,
                size.width, size.height), (1f - dark) * fade, true)
        }
    }
}
