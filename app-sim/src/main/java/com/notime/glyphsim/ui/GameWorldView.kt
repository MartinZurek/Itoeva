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

/** Ein einziges Panorama: an Ortsgrenzen wird weder das Bild geladen noch ausgetauscht. */
@Composable
internal fun GameWorldView(scene: GameScenes.Scene, images: Map<String,ImageBitmap?>, camera: GameCamera.State,
    minute: Int, lampOn: Boolean, phase: Int, environment: GameEnvironment.State,
    pos: PlayControl.Pos, fade: Float, modifier: Modifier, clock: Long, weather: PlayWeather = PlayWeather.CLEAR) {
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
        // Die Naht selbst ist nachgemalt; nur ihre unveraenderten Aussenraender laufen weich aus.
        for (seam in GameWorld.seams) {
            val start = seam.x - GameWorld.SEAM_HALF
            val end = seam.x + GameWorld.SEAM_HALF
            if (fit.left + end * fit.scale < 0f || fit.left + start * fit.scale > size.width) continue
            val patch = images[seam.asset] ?: continue
            fun strip(offset: Float, width: Float, alpha: Float) {
                val sx0 = ((offset + GameWorld.SEAM_HALF) / (2f * GameWorld.SEAM_HALF) * patch.width).roundToInt()
                val sx1 = ((offset + width + GameWorld.SEAM_HALF) / (2f * GameWorld.SEAM_HALF) * patch.width).roundToInt()
                val x0 = (fit.left + (seam.x + offset) * fit.scale).roundToInt()
                val x1 = (fit.left + (seam.x + offset + width) * fit.scale).roundToInt()
                if (x1 <= x0 || sx1 <= sx0) return
                drawImage(patch, IntOffset(sx0, 0), IntSize(sx1 - sx0, patch.height),
                    IntOffset(x0, fit.top.roundToInt()), IntSize(x1 - x0, (GameWorld.HEIGHT * fit.scale).roundToInt()),
                    alpha = alpha * fade.coerceIn(0f, 1f), filterQuality = FilterQuality.Low)
            }
            val edge = GameWorld.SEAM_HALF - GameWorld.SEAM_FEATHER
            strip(-edge, 2f * edge, 1f)
            repeat(16) { i ->
                val width = GameWorld.SEAM_FEATHER / 16f
                val a = -GameWorld.SEAM_HALF + i * width
                val b = edge + i * width
                strip(a, width, GameWorld.seamAlpha(a + width / 2f))
                strip(b, width, GameWorld.seamAlpha(b + width / 2f))
            }
        }
        for (place in GameWorld.visiblePlaces(scene)) {
            val section = GameWorld.scene(place)!!
            val image = images[section.asset] ?: continue
            val sectionFit = GameCamera.fit(camera, section, size.width, size.height)
            val span = GameWorld.region(place)!!.section
            if (sectionFit.left + span * sectionFit.scale < 0f || sectionFit.left > size.width) continue
            paintPaintedMotion(section, image, sectionFit, clock, fade, images,weather,environment)
            paintAtmosphere(section, sectionFit, clock, minute, fade, false,weather)
        }
        val dark = GameSceneLighting.darkness(scene, minute)
        // Die Hoehle bleibt dunkel, auch wenn der aktive Ortsname noch Lager ist.
        val visible = GameWorld.visiblePlaces(scene)

        withTransform({ translate(fit.left, fit.top); scale(fit.scale, fit.scale, Offset.Zero) }) {
            visible.forEachIndexed { index, place ->
                val here = GameSceneLighting.darkness(GameWorld.scene(place)!!, minute)
                val before = GameSceneLighting.darkness(GameWorld.scene(visible.getOrElse(index - 1) { place })!!, minute)
                val after = GameSceneLighting.darkness(GameWorld.scene(visible.getOrElse(index + 1) { place })!!, minute)
                val sectionWidth = GameWorld.region(place)!!.section
                val x = GameWorld.origin(place)
                val duskStart = GameSceneLighting.duskAt(GameWorld.scene(place)!!, 0f, minute)
                val duskMiddle = GameSceneLighting.duskAt(GameWorld.scene(place)!!, sectionWidth / 2f, minute)
                val duskEnd = GameSceneLighting.duskAt(GameWorld.scene(place)!!, sectionWidth, minute)
                drawRect(Brush.horizontalGradient(listOf(
                    Color(0xFFFF9C57).copy(alpha = duskStart * .12f * fade),
                    Color(0xFFFF9C57).copy(alpha = duskMiddle * .12f * fade),
                    Color(0xFFFF9C57).copy(alpha = duskEnd * .12f * fade)), x, x + sectionWidth),
                    Offset(x, 0f), Size(sectionWidth, GameWorld.height(scene)))
                drawRect(Brush.horizontalGradient(listOf(
                    Color(0xFF0A1030).copy(alpha = (here + before) * .5f * fade),
                    Color(0xFF0A1030).copy(alpha = here * fade),
                    Color(0xFF0A1030).copy(alpha = (here + after) * .5f * fade)), x, x + sectionWidth),
                    Offset(x, 0f), Size(sectionWidth, GameWorld.height(scene)))
            }
        }
        paintWorldShadows(scene, local, minute, clock, fade)
        paintSceneLights(scene, GameSceneLighting.sources(scene, minute, lampOn, false, (clock / 200L).toInt()), local, clock, fade)
        roomContacts(scene, environment, local, (1f - dark) * fade, false)
    }
}

/** Ortsfeste Regungen bleiben beim Schwenken an der Landschaft verankert. */
@Composable
internal fun GameWorldForeground(scene: GameScenes.Scene, camera: GameCamera.State,
    environments: Map<PlayScene.Place, GameEnvironment.State>, clock: Long, minute: Int,
    fade: Float, modifier: Modifier, weather: PlayWeather = PlayWeather.CLEAR) {
    Canvas(modifier) {
        val local = GameCamera.fit(camera, scene, size.width, size.height)
        val dark = GameSceneLighting.darkness(scene, minute)
        for (place in GameWorld.visiblePlaces(scene)) {
            val section = GameWorld.scene(place)!!
            val sectionFit = GameCamera.fit(camera, section, size.width, size.height)
            val span = GameWorld.region(place)!!.section
            if (sectionFit.left + span * sectionFit.scale < 0f || sectionFit.left > size.width) continue
            paintAtmosphere(section, sectionFit, clock, minute, fade, true,weather)
        }
        environments.filterKeys { it in GameWorld.visiblePlaces(scene) }.forEach { (place, state) ->
            roomContacts(GameWorld.scene(place)!!, state, GameCamera.fit(camera, GameWorld.scene(place)!!,
                size.width, size.height), (1f - dark) * fade, true)
        }
    }
}
