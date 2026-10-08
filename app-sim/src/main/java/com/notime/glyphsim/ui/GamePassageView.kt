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

/** Ortsfeste Tuerrahmen und Wegweiser teilen die Projektion ihrer Trefferflaechen. */
@Composable
internal fun GamePassageView(scene: GameScenes.Scene, camera: GameCamera.State, german: Boolean,
    fade: Float, modifier: Modifier) {
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER } }
    Canvas(modifier) {
        for (place in GameWorld.visiblePlaces(scene)) {
            val section = GameWorld.scene(place)!!
            val fit = GameCamera.fit(camera, section, size.width, size.height)
            for (passage in GameWorld.passages(place).filter { it.door || !GameWorld.connected(place, it.to) }) {
                val (fx, fy) = GameScenes.feet(section, passage.pos)
                val (x,y) = fit.toScreen(fx, fy)
                val scale = fit.scale
                if (passage.door) {
                    val corner = Offset(x - 16f * scale, y - 65f * scale)
                    val doorSize = Size(32f * scale, 65f * scale)
                    drawRect(Color(0xFF302D22).copy(alpha = .20f * fade), corner, doorSize)
                    drawRect(Color(0xFFF2D390).copy(alpha = .80f * fade), corner, doorSize, style = Stroke(2f * scale))
                    drawCircle(Color(0xFFF2D390).copy(alpha = fade), 2f * scale, Offset(x + 10f * scale, y - 30f * scale))
                } else {
                    drawCircle(Color(0xFF334A38).copy(alpha = .75f * fade), 10f * scale, Offset(x,y))
                    val dy = if (passage.dir == PlayControl.Dir.UP) -4f else 4f
                    drawLine(Color(0xFFF2D390).copy(alpha = fade), Offset(x - 4f * scale,y), Offset(x,y + dy * scale), 2f * scale)
                    drawLine(Color(0xFFF2D390).copy(alpha = fade), Offset(x + 4f * scale,y), Offset(x,y + dy * scale), 2f * scale)
                }
                paint.textSize = 12f * scale; paint.alpha = (fade * 255).toInt()
                val label = FennecWorld.name(passage.to, german)
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
    height: Float, fade: Float, modifier: Modifier) {
    Canvas(modifier) {
        val wet = GameWorld.wetness(scene, pos)
        if (wet <= 0f || height > 0f) return@Canvas
        val fit = GameCamera.fit(camera, scene, size.width, size.height)
        val (fx, fy) = GameScenes.feet(scene, pos)
        val (x,y) = fit.toScreen(fx, fy)
        val waterY = y - GameCharacterScale.waterRise(scene, pos) * fit.scale
        drawOval(Color(0xFFDEF4EB).copy(alpha = .55f * fade),
            Offset(x - 21f * fit.scale, waterY - 2f * fit.scale), Size(42f * fit.scale, 7f * fit.scale),
            style = Stroke(1.2f * fit.scale))
    }
}
