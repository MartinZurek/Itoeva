package com.notime.glyphsim.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.notime.glyphsim.matrix.*
import kotlin.math.cos
import kotlin.math.roundToInt

/** Elf kleine Pixelkulissen; Materialgruppen werden einmal gerastert statt pro Animationsframe. */
private object RoomPixels {
    private val images=java.util.concurrent.ConcurrentHashMap<GameScenes.Scene,ImageBitmap>()
    private val doors=java.util.concurrent.ConcurrentHashMap<Pair<Int,Int>,ImageBitmap>()
    fun image(scene: GameScenes.Scene): ImageBitmap = images.getOrPut(scene) {
        raster(GameRoomArt.marks(scene),480,270)
    }
    fun door(width: Int,height: Int): ImageBitmap = doors.getOrPut(width to height) {
        raster(GameRoomArt.door(width,height),width,height)
    }
    private fun raster(marks: List<GameRoomArt.Mark>,width: Int,height: Int): ImageBitmap {
        val bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap)
        val paint=Paint().apply { isAntiAlias=false }
        fun path(points: List<Pair<Float,Float>>) = Path().apply {
            points.forEachIndexed { i,p -> if(i==0) moveTo(p.first,p.second) else lineTo(p.first,p.second) }
            close()
        }
        for(mark in marks) {
            val saved=if(mark.clip!=null) canvas.save() else null
            mark.clip?.let { canvas.clipPath(path(it)) }
            paint.color=mark.color.toInt()
            if(mark.oval) {
                val (from,to)=mark.points
                canvas.drawOval(from.first,from.second,to.first,to.second,paint)
            } else canvas.drawPath(path(mark.points),paint)
            if(saved!=null) canvas.restoreToCount(saved)
        }
        return bitmap.asImageBitmap()
    }
}

/** Raeumliche Pixelkulisse: einzelne Moebel behalten ihre gemeinsamen Kontakt-/Verdeckungskanten. */
internal fun DrawScope.paintRoomSpace(scene: GameScenes.Scene, fit: GameScenes.Fit, alpha: Float, clock: Long = 0L) {
    withTransform({translate(fit.left,fit.top);scale(fit.scale,fit.scale,Offset.Zero)}) {
        drawImage(RoomPixels.image(scene),IntOffset.Zero,IntSize(480,270),IntOffset.Zero,IntSize(480,270),alpha=alpha,filterQuality=FilterQuality.None)
        // Nur die Vorhaenge bewegen sich ueber dem statischen Materialbild. Gleiche Pixelgroesse wie Moebel.
        val cloth=Color(0xFFD8C39C)
        for(x in listOf(76f,146f)) repeat(11) { row ->
            val dx=GameFabric.hangingOffset(row/11f,.5f,.10f,clock,scene.place.ordinal)
            val fold=if(row%3==0) Color(0xFFC3AD87) else cloth
            drawRect(fold,Offset(x+dx,45f+row*4f),Size(6f,4f),alpha=alpha)
            drawRect(Color(0xFFEBDCBB),Offset(x+dx+1,45f+row*4f),Size(1f,4f),alpha=alpha)
        }
    }
}

/** Gleicher Holzstil wie die Raumkoerper; Anschlag und Durchgang lesen die vorhandene Oeffnung. */
internal fun DrawScope.paintRoomDoor(box: GameScenes.Box,fit: GameScenes.Fit,opening: Float,alpha: Float) {
    val (x,y)=fit.toScreen(box.x0,box.y0)
    val width=(box.x1-box.x0).roundToInt().coerceAtLeast(1)
    val height=(box.y1-box.y0).roundToInt().coerceAtLeast(1)
    drawRect(Color(0xFF624936),Offset(x-fit.scale,y-fit.scale),
        Size((width+2)*fit.scale,(height+2)*fit.scale),alpha=alpha)
    drawRect(Color(0xFF302B26),Offset(x,y),Size(width*fit.scale,height*fit.scale),alpha=alpha)
    drawImage(RoomPixels.door(width,height),IntOffset.Zero,IntSize(width,height),
        IntOffset(x.roundToInt(),y.roundToInt()),
        IntSize((width*fit.scale*cos(opening*1.48f)).roundToInt().coerceAtLeast(1),
            (height*fit.scale).roundToInt().coerceAtLeast(1)),alpha=alpha,filterQuality=FilterQuality.None)
}
