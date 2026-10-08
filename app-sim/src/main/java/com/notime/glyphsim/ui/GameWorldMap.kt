package com.notime.glyphsim.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notime.glyphsim.matrix.GameWorld
import com.notime.glyphsim.matrix.PlayControl
import com.notime.glyphsim.matrix.PlayMap
import com.notime.glyphsim.matrix.PlayScene.Place
import com.notime.glyphsim.stream.FennecWorld

/** Dieselben Ausgaenge wie im Spiel, mit Landschaft, lesbaren Namen und einem echten Standort. */
@Composable
internal fun GameWorldMap(current: Place, pos: PlayControl.Pos, target: Place?, route: List<Place>,
    german: Boolean, onTarget: (Place?) -> Unit, modifier: Modifier) {
    val density = LocalDensity.current
    val fontPx = with(density) { 10.sp.toPx() }
    val radius = with(density) { 28.dp.toPx() }
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER } }
    val positions = GameWorld.mapPositions
    Canvas(modifier.pointerInput(current, positions) {
        detectTapGestures { tap ->
            val margin = 20.dp.toPx()
            val picked = positions.minByOrNull { (_, p) ->
                val dx = margin + p.first * (size.width - 2f * margin) - tap.x
                val dy = margin + p.second * (size.height - 2f * margin) - tap.y
                dx * dx + dy * dy
            }?.takeIf { (_, p) ->
                val dx = margin + p.first * (size.width - 2f * margin) - tap.x
                val dy = margin + p.second * (size.height - 2f * margin) - tap.y
                dx * dx + dy * dy < radius * radius
            }?.key
            if (picked != null) onTarget(picked.takeIf { it != GameWorld.shownAs(current) })
        }
    }) {
        val margin = 20.dp.toPx()
        val innerW = size.width - margin * 2f
        val innerH = size.height - margin * 2f
        fun point(p: Pair<Float, Float>) = Offset(margin + p.first * innerW, margin + p.second * innerH)
        fun at(place: Place) = point(positions.getValue(GameWorld.shownAs(place)))
        drawRect(Color(0xFFCCCEA4))
        drawOval(Color(0xFFB6C78E), Offset(size.width * .32f, 0f), Size(size.width * .62f, size.height * .88f))
        drawOval(Color(0xFF8FAD7C), Offset(size.width * .70f, size.height * .20f), Size(size.width * .35f, size.height * .76f))
        drawOval(Color(0xFF8AB7B4), Offset(size.width * .39f, size.height * .70f), Size(size.width * .18f, size.height * .25f))
        drawRect(Color(0xFF7CAAAF), Offset(size.width * .55f, size.height * .92f), Size(size.width * .19f, size.height * .08f))
        repeat(19) { i ->
            val x = size.width * (.75f + (i % 5) * .045f)
            val y = size.height * (.34f + (i / 5) * .115f)
            drawCircle(Color(0xFF6E936B).copy(alpha = .45f), 5.dp.toPx(), Offset(x, y))
        }
        val edges = mutableSetOf<Pair<Place, Place>>()
        for (place in Place.entries) for (next in GameWorld.neighbors(place)) {
            val a = GameWorld.shownAs(place); val b = GameWorld.shownAs(next)
            if (a != b && a in positions && b in positions) edges += if (a.ordinal < b.ordinal) a to b else b to a
        }
        for ((a, b) in edges) {
            drawLine(Color(0xFF77846D), at(a), at(b), 4.dp.toPx())
            drawLine(Color(0xFFDCD1AC), at(a), at(b), 2.dp.toPx())
        }
        GameWorld.places.zipWithNext().forEach { (a, b) ->
            drawLine(Color(0xFF7D8B63), at(a), at(b), 8.dp.toPx())
            drawLine(Color(0xFFE9D8AE), at(a), at(b), 5.dp.toPx())
        }
        (listOf(current) + route).zipWithNext().forEach { (a, b) ->
            if (GameWorld.shownAs(a) != GameWorld.shownAs(b)) drawLine(Color(0xFFB27638), at(a), at(b), 3.dp.toPx())
        }
        for ((place, _) in positions) {
            val p = at(place)
            val r = 5.dp.toPx()
            when (PlayMap.regionOf(place)) {
                PlayMap.Region.HOME, PlayMap.Region.TOWN -> {
                    drawRect(Color(0xFFDFBF82), p - Offset(r, r * .8f), Size(r * 2f, r * 1.8f))
                    val roof = Path().apply { moveTo(p.x - r * 1.3f, p.y - r * .7f); lineTo(p.x, p.y - r * 1.8f); lineTo(p.x + r * 1.3f, p.y - r * .7f); close() }
                    drawPath(roof, Color(0xFF8E654E))
                    drawRect(Color(0xFF6B6653), p - Offset(r * .25f, 0f), Size(r * .5f, r))
                }
                else -> {
                    drawCircle(Color(0xFF4F7558), r, p - Offset(0f, r * .5f))
                    drawLine(Color(0xFF806A43), p, p + Offset(0f, r), r * .5f)
                }
            }
            if (place == target) drawCircle(Color(0xFF9D632F), r * 1.7f, p, style = Stroke(2.dp.toPx()))
            paint.textSize = fontPx
            paint.color = 0xFF273F34.toInt()
            // Heller Saum haelt Namen auch ueber Wald und Wegen lesbar.
            paint.style = Paint.Style.STROKE; paint.strokeWidth = 3.dp.toPx(); paint.color = 0xFFDEDABB.toInt()
            val label = FennecWorld.name(place, german)
            drawContext.canvas.nativeCanvas.drawText(label, p.x, p.y + r * 2.5f + (if (GameWorld.places.indexOf(place) % 2 == 0) 0f else 15.dp.toPx()), paint)
            paint.style = Paint.Style.FILL; paint.color = 0xFF273F34.toInt()
            drawContext.canvas.nativeCanvas.drawText(label, p.x, p.y + r * 2.5f + (if (GameWorld.places.indexOf(place) % 2 == 0) 0f else 15.dp.toPx()), paint)
        }
        val here = point(GameWorld.mapPoint(current, pos))
        drawCircle(Color(0xFFFCF5D1), 6.dp.toPx(), here)
        drawCircle(Color(0xFFB14E37), 4.dp.toPx(), here)
    }
}
