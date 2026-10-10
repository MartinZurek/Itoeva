package com.notime.glyphsim.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.notime.glyphsim.matrix.*
import com.notime.glyphsim.stream.FennecWorld
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.cos

/** Ortsfeste Tuerrahmen und Wegweiser teilen die Projektion ihrer Trefferflaechen. */
@Composable
internal fun GamePassageView(scene: GameScenes.Scene, camera: GameCamera.State, german: Boolean,
    fade: Float, modifier: Modifier, door: GameDoors.State? = null, clock: Long = 0L,
    images: Map<String,ImageBitmap?> = emptyMap()) {
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER } }
    Canvas(modifier) {
        for (place in GameWorld.visiblePlaces(scene)) {
            val section = GameWorld.scene(place)!!
            val fit = GameCamera.fit(camera, section, size.width, size.height)
            for (passage in GameWorld.passages(place).filter { it.door || !GameWorld.connected(place, it.to) }
                .distinctBy { if(it.door) GameDoors.aperture(it.from,it.to) else it.pos }) {
                val (fx, fy) = GameScenes.feet(section, passage.pos)
                val (x,y) = fit.toScreen(fx, fy)
                val scale = fit.scale
                if (passage.door) {
                    val aperture=GameDoors.aperture(passage.from,passage.to)
                    val active=door?.takeIf { it.passage.from==passage.from &&
                        GameDoors.aperture(it.passage.from,it.passage.to)==aperture }
                    val opening=GameDoors.opening(active,active?.passage ?: passage,clock)
                    val sample=if(opening>0f) GameDoors.sample(passage) else null
                    if(opening>0f && sample!=null) {
                        val (ax,ay)=fit.toScreen(aperture.x0,aperture.y0)
                        val width=(aperture.x1-aperture.x0)*scale
                        val height=(aperture.y1-aperture.y0)*scale
                        drawRect(Color(0xFF17201C).copy(alpha=fade),Offset(ax,ay),Size(width,height))
                        val leafWidth=(width*cos(opening*1.48f)).coerceAtLeast(scale)
                        images[sample.asset]?.let { image ->
                            val sx=image.width/sample.width; val sy=image.height/sample.height
                            val sourceX=((aperture.x0+sample.offset)*sx).roundToInt().coerceIn(0,image.width-1)
                            val sourceY=(aperture.y0*sy).roundToInt().coerceIn(0,image.height-1)
                            drawImage(image,IntOffset(sourceX,sourceY),IntSize(
                                ((aperture.x1-aperture.x0)*sx).roundToInt().coerceIn(1,image.width-sourceX),
                                ((aperture.y1-aperture.y0)*sy).roundToInt().coerceIn(1,image.height-sourceY)),
                                IntOffset(ax.roundToInt(),ay.roundToInt()),
                                IntSize(leafWidth.roundToInt().coerceAtLeast(1),height.roundToInt().coerceAtLeast(1)),
                                alpha=fade,filterQuality=FilterQuality.Low)
                        }
                        drawRect(Color(0xFF8C7052).copy(alpha=fade),Offset(ax,ay),Size(leafWidth,height),style=Stroke(scale))
                    }
                } else {
                    drawCircle(Color(0xFF334A38).copy(alpha = .75f * fade), 10f * scale, Offset(x,y))
                    val dy = if (passage.dir == PlayControl.Dir.UP) -4f else 4f
                    drawLine(Color(0xFFF2D390).copy(alpha = fade), Offset(x - 4f * scale,y), Offset(x,y + dy * scale), 2f * scale)
                    drawLine(Color(0xFFF2D390).copy(alpha = fade), Offset(x + 4f * scale,y), Offset(x,y + dy * scale), 2f * scale)
                }
                paint.textSize = 12f * scale; paint.alpha = (fade * 255).toInt()
                val label = if(passage.door && GameDoors.choices(passage).size>1)
                    if(german) "Flur" else "Hallway" else FennecWorld.name(passage.to, german)
                val labelY = y - (if (passage.door) 70f else 16f) * scale
                paint.style = Paint.Style.STROKE; paint.strokeWidth = 3f * scale; paint.color = 0xFF283C32.toInt()
                drawContext.canvas.nativeCanvas.drawText(label, x, labelY, paint)
                paint.style = Paint.Style.FILL; paint.color = 0xFFF9ECCA.toInt()
                drawContext.canvas.nativeCanvas.drawText(label, x, labelY, paint)
            }
        }
    }
}

