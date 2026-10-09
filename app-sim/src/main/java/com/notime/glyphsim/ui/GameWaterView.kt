package com.notime.glyphsim.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import com.notime.glyphsim.matrix.*
import kotlin.math.ceil

private class WaterTexture(val bitmap: android.graphics.Bitmap, val columns: Int) {
    val vertices=FloatArray((columns+1)*2*2)
    val paint=android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
}
private val waterTextures=java.util.WeakHashMap<ImageBitmap,MutableMap<List<Int>,WaterTexture>>()

/** Die feste Wasser-/Ufermaske bewegt sich nicht; nur die Textur hat eine stetige Stroemung. */
internal fun DrawScope.paintWater(scene: GameScenes.Scene, images: Map<String,ImageBitmap?>,
    fit: GameScenes.Fit, clock: Long, alpha: Float) {
    if(alpha < .995f) return
    val region=GameWorld.region(scene.place) ?: return
    val origin=GameWorld.origin(scene.place)
    val localOrigin=origin-GameWorld.regionOrigin(region)
    withTransform({ translate(fit.left,fit.top); scale(fit.scale,fit.scale,Offset.Zero) }) {
        for(run in GameWater.runs(scene.asset)) {
            val left=maxOf(run.x0.toFloat()-localOrigin,0f)
            val right=minOf(run.x1.toFloat()-localOrigin,region.section)
            val y=run.y.toFloat()
            if(right<=left || fit.left+right*fit.scale<0f || fit.left+left*fit.scale>size.width) continue
            val sample=GameAtmosphere.sample(scene,GameAtmosphere.Patch(left,y,right-left,4f,0,water=true)) ?: continue
            val source=images[sample.asset] ?: continue
            val sx=source.width/sample.width;val sy=source.height/sample.height
            val x0=((left+sample.offset-5f)*sx).toInt().coerceIn(0,source.width-1)
            val x1=ceil((right+sample.offset+5f)*sx).toInt().coerceIn(x0+1,source.width)
            val y0=((y-1f)*sy).toInt().coerceIn(0,source.height-1)
            val y1=ceil((y+5f)*sy).toInt().coerceIn(y0+1,source.height)
            val key=listOf(x0,y0,x1,y1)
            val texture=waterTextures.getOrPut(source) { mutableMapOf() }.getOrPut(key) {
                WaterTexture(android.graphics.Bitmap.createBitmap(source.asAndroidBitmap(),x0,y0,x1-x0,y1-y0),
                    ceil((x1-x0)/sx/16f).toInt().coerceAtLeast(1))
            }
            var i=0
            for(row in 0..1) for(column in 0..texture.columns) {
                val x=(x0+(x1-x0)*column.toFloat()/texture.columns)/sx-sample.offset
                val py=(y0+(y1-y0)*row.toFloat())/sy
                texture.vertices[i++]=x+GameWater.displacement(origin+x,py,clock)
                texture.vertices[i++]=py
            }
            clipRect(left,y,right,y+4f) {
                drawIntoCanvas { canvas -> canvas.nativeCanvas.drawBitmapMesh(texture.bitmap,texture.columns,1,
                    texture.vertices,0,null,0,texture.paint) }
            }
        }
    }
}
