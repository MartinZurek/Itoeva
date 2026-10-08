package com.notime.glyphsim.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.notime.glyphsim.matrix.*

/** Vorderkanten aus dem Originalbild, ohne ein zweites Bild oder ein neues Render-System. */
@Composable
internal fun GamePaintedForeground(scene: GameScenes.Scene, image: ImageBitmap?,
    movement: GameMovement.State, camera: GameCamera.State, fade: Float, minute: Int,
    lampOn: Boolean, tvOn: Boolean, phase: Int, modifier: Modifier) {
    if (image == null) return
    val contours = when (scene.place) {
        PlayScene.Place.LIVING -> listOf(
            Triple("living-sofa", 164f, listOf(170f to 128f,181f to 114f,207f to 112f,
                231f to 114f,249f to 112f,280f to 114f,293f to 123f,302f to 126f,
                307f to 137f,303f to 162f,251f to 166f,244f to 162f,216f to 160f,
                185f to 163f,174f to 154f)),
            Triple("living-tea-table", 171f, listOf(167f to 146f,175f to 145f,195f to 144f,
                216f to 147f,226f to 149f,224f to 153f,219f to 155f,217f to 169f,
                214f to 171f,211f to 169f,211f to 156f,180f to 156f,179f to 169f,
                175f to 171f,173f to 168f,172f to 154f,167f to 152f)))
        else -> emptyList()
    }
    if (contours.isEmpty()) return
    Canvas(modifier) {
        val fit = GameCamera.fit(camera, scene, size.width, size.height)
        val feetY = GameScenes.feet(scene, movement.pos).second
        withTransform({ translate(fit.left, fit.top); scale(fit.scale, fit.scale, Offset.Zero) }) {
            for ((id, baseline, points) in contours) {
                if (movement.support == id || feetY >= baseline) continue
                val path = Path().apply {
                    moveTo(points.first().first, points.first().second)
                    points.drop(1).forEach { (x,y) -> lineTo(x,y) }; close()
                }
                clipPath(path) {
                    drawImage(image, IntOffset.Zero, IntSize(image.width,image.height),
                        IntOffset.Zero, IntSize(480,270), alpha = fade, filterQuality = FilterQuality.Low)
                    drawRect(Color(0xFF0A1030).copy(alpha = GameSceneLighting.darkness(scene,minute) * fade))
                    for (light in GameSceneLighting.sources(scene,minute,lampOn,tvOn,phase)) {
                        val tint = when (light.tone) {
                            GameSceneLighting.Tone.SUN -> Color(0xFFFFEDC6)
                            GameSceneLighting.Tone.WARM -> Color(0xFFFFB96A)
                            GameSceneLighting.Tone.COOL -> Color(0xFF78C8E6)
                        }
                        val center = Offset(light.x,light.y)
                        drawCircle(Brush.radialGradient(listOf(tint.copy(alpha = light.power * .55f),
                            Color.Transparent),center,light.radius),light.radius,center,alpha = fade)
                    }
                }
            }
        }
    }
}
