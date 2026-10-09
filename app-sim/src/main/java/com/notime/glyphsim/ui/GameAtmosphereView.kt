package com.notime.glyphsim.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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

private class MotionTexture(val bitmap: android.graphics.Bitmap) {
    val vertices=FloatArray(9*17*2)
    val paint=android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
}
private val motionTextures=java.util.WeakHashMap<ImageBitmap,MutableMap<List<Int>,MotionTexture>>()

/** Kleine Ausschnitte werden einmal gecacht und als zusammenhaengende Netze verformt. */
internal fun DrawScope.paintPaintedMotion(scene: GameScenes.Scene, image: ImageBitmap,
    fit: GameScenes.Fit, clock: Long, alpha: Float, images: Map<String, ImageBitmap?> = emptyMap(),
    weather: PlayWeather = PlayWeather.CLEAR) {
    if(alpha < .995f) return
    withTransform({ translate(fit.left, fit.top); scale(fit.scale, fit.scale, Offset.Zero) }) {
        for (patch in GameAtmosphere.patches(scene)) {
            val sample=GameAtmosphere.sample(scene,patch) ?: continue
            val source=if(sample.asset==scene.asset) image else images[sample.asset] ?: continue
            val sx=source.width/sample.width; val sy=source.height/sample.height
            val x0=((patch.x-3f+sample.offset)*sx).toInt().coerceIn(0,source.width-1)
            val x1=kotlin.math.ceil((patch.x+patch.w+3f+sample.offset)*sx).toInt().coerceIn(x0+1,source.width)
            val y0=((patch.y-3f)*sy).toInt().coerceIn(0,source.height-1)
            val y1=kotlin.math.ceil((patch.y+patch.h+3f)*sy).toInt().coerceIn(y0+1,source.height)
            val key=listOf(x0,y0,x1,y1)
            val texture=motionTextures.getOrPut(source) { mutableMapOf() }.getOrPut(key) {
                MotionTexture(android.graphics.Bitmap.createBitmap(source.asAndroidBitmap(),x0,y0,x1-x0,y1-y0))
            }
            var i=0
            for(row in 0..16) for(column in 0..8) {
                val x=(x0+(x1-x0)*column/8f)/sx-sample.offset
                val y=(y0+(y1-y0)*row/16f)/sy
                texture.vertices[i++]=x+GameAtmosphere.motionOffset(patch,x,y,clock,GameWorld.origin(scene.place),weather)
                texture.vertices[i++]=y
            }
            texture.paint.alpha=(alpha.coerceIn(0f,1f)*255f).roundToInt()
            drawIntoCanvas { canvas -> canvas.nativeCanvas.drawBitmapMesh(texture.bitmap,8,16,
                texture.vertices,0,null,0,texture.paint) }
        }
    }
}

/** Regungen folgen Boden, Ufer und Licht; Kamera und aktiver Ortsname verschieben sie nicht. */
internal fun DrawScope.paintAtmosphere(scene: GameScenes.Scene, fit: GameScenes.Fit,
    clock: Long, minute: Int, alpha: Float, foreground: Boolean, weather: PlayWeather = PlayWeather.CLEAR) {
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
            val sway = GameAtmosphere.wind(origin + x, clock,weather) * 3f
            val light = (1f - GameSceneLighting.darknessAt(scene, x, minute)) * alpha
            drawLine(Color(0xFF71834A).copy(alpha = .50f * light), Offset(x, y),
                Offset(x + sway, y - 7f - i % 3), 1f)
        }
    }
}
