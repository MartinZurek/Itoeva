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

/** Wassertiefe und Bugwelle folgen dem Fussanker, Springen hebt die Figur aus dem Wasser. */
@Composable
internal fun GameWaterForeground(scene: GameScenes.Scene, camera: GameCamera.State, pos: PlayControl.Pos,
    height: Float, fade: Float, modifier: Modifier, clock: Long = 0L, moving: Boolean = false, dir: PlayControl.Dir = PlayControl.Dir.DOWN) {
    Canvas(modifier) {
        val wet = GameWorld.wetness(scene, pos)
        if (wet <= 0f || height > 0f) return@Canvas
        val fit = GameCamera.fit(camera, scene, size.width, size.height)
        val (fx, fy) = GameScenes.feet(scene, pos)
        val (x,y) = fit.toScreen(fx, fy)
        val waterY = y - GameCharacterScale.waterRise(scene, pos) * fit.scale
        if (moving) {
            val dx=when(dir) { PlayControl.Dir.LEFT -> -1f; PlayControl.Dir.RIGHT -> 1f; else -> 0f }
            val dy=when(dir) { PlayControl.Dir.UP -> -.55f; PlayControl.Dir.DOWN -> .55f; else -> 0f }
            // Rueckwaerts offene Spur statt einer unabhaengigen Kreisboje.
            repeat(3) { ring ->
                val age=((clock%1200L)/1200f+ring/3f)%1f
                val back=age*48f*fit.scale
                val spread=(10f+age*24f)*fit.scale
                val center=Offset(x-dx*back,waterY-dy*back)
                val color=Color(0xFFDCEEE5).copy(alpha=(1f-age)*.32f*fade)
                drawLine(color,center+Offset(-spread,-spread*.16f),center+Offset(-spread*.45f,spread*.18f),fit.scale)
                drawLine(color,center+Offset(spread,-spread*.16f),center+Offset(spread*.45f,spread*.18f),fit.scale)
            }
        }
        val phase=(clock%900L)/900f
        val width=(17f+phase*14f)*fit.scale
        drawOval(Color(0xFFDEF4EB).copy(alpha=(1f-phase)*(if(moving) .22f else .28f)*fade),
            Offset(x-width,waterY-3f*fit.scale),Size(width*2f,(5f+phase*5f)*fit.scale),style=Stroke(fit.scale))
        val stroke=GameWater.swim(scene,pos,height,dir,moving,clock)
        if(stroke!=null) repeat(2) { hand ->
            val p=stroke.stroke
            val side=if(hand==0) -1f else 1f
            val spread=(12f+p*18f)*fit.scale
            val hx=x+side*spread
            val hy=waterY+sin(p*6.283f)*3f*fit.scale
            drawLine(Color(0xFFE7F4E8).copy(alpha=(1f-p)*.7f*fade),Offset(hx,hy),
                Offset(hx-side*9f*fit.scale,hy+2f*fit.scale),fit.scale)
            if(moving && p<.5f) repeat(3) { drop ->
                val t=p*1.2f
                val dx=side*(12f+(drop+1)*t*18f)*fit.scale
                val dy=(-30f*t+45f*t*t-drop*1.2f)*fit.scale
                drawCircle(Color(0xFFE6F3ED).copy(alpha=(1f-p*2f)*fade),fit.scale,Offset(x+dx,waterY+dy))
            }
        }
    }
}
