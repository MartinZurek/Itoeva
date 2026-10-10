package com.notime.glyphsim.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.notime.glyphsim.matrix.*
import kotlin.math.roundToInt

/** Nur die einmalig bestimmten Wasserstreifen bewegen die vorhandene Textur.
 * Starrkoerper sind ausgenommen; Nahtpixel kommen aus dem wirklich sichtbaren Anschlussbild. */
internal fun DrawScope.paintWater(scene: GameScenes.Scene, images: Map<String,ImageBitmap?>,
    fit: GameScenes.Fit, clock: Long, alpha: Float) {
    val region=GameWorld.region(scene.place) ?: return
    val localOrigin=GameWorld.origin(scene.place)-GameWorld.regionOrigin(region)
    withTransform({ translate(fit.left,fit.top); scale(fit.scale,fit.scale,Offset.Zero) }) {
        for(run in GameWater.runs(scene.asset)) {
            val start=maxOf(run.x0.toFloat()-localOrigin,0f)
            val end=minOf(run.x1.toFloat()-localOrigin,region.section)
            if(end<=start) continue
            for(tile in 0 until kotlin.math.ceil((end-start)/64f).toInt()) {
            val left=start+tile*64f
            val right=minOf(end,left+64f)
            val y=run.y.toFloat()
            if (right<=left || fit.left+right*fit.scale<0f || fit.left+left*fit.scale>size.width) continue
            val patch=GameAtmosphere.Patch(left,y,right-left,4f,0,water=true)
            val sample=GameAtmosphere.sample(scene,patch) ?: continue
            val source=images[sample.asset] ?: continue
            val dx=GameWater.displacement(GameWorld.origin(scene.place)+(left+right)/2f,y,clock)
            val sx=source.width/sample.width; val sy=source.height/sample.height
            val x0=((left+sample.offset-dx)*sx).roundToInt().coerceIn(0,source.width-1)
            val x1=((right+sample.offset-dx)*sx).roundToInt().coerceIn(x0+1,source.width)
            val y0=(y*sy).roundToInt().coerceIn(0,source.height-1)
            val y1=((y+4f)*sy).roundToInt().coerceIn(y0+1,source.height)
            clipRect(left,y,right,y+4f) {
                drawImage(source,IntOffset(x0,y0),IntSize(x1-x0,y1-y0),
                    IntOffset(left.roundToInt(),run.y),IntSize((right-left).roundToInt().coerceAtLeast(1),4),
                    alpha=alpha,filterQuality=FilterQuality.Low)
            }
            }
        }
    }
}
