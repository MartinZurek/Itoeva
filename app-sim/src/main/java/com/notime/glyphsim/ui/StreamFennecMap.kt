package com.notime.glyphsim.ui

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import com.notime.glyphsim.matrix.PlayMap
import com.notime.glyphsim.matrix.PlayMapScene
import com.notime.glyphsim.matrix.PlayScene.Place
import com.notime.glyphsim.stream.FennecWorld

/** Voruebergehender Blick auf dieselbe Karte; die laufende Reise wird nicht unterbrochen. */
@Composable
internal fun StreamFennecMap(from: Place, presentation: FennecWorld.Presentation, german: Boolean,
                             viewer: String, reply: String,
                             modifier: Modifier = Modifier) {
    val drawn = remember(from, presentation) { Animatable(0f) }
    LaunchedEffect(from, presentation) { drawn.animateTo(1f, tween(1_000)) }
    val route = remember(from, presentation) { FennecWorld.route(from, presentation) }
    val labels = remember { PlayMapScene.labels(100, 50) }
    val labelPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER } }
    val fontPx = with(LocalDensity.current) { 9.sp.toPx() }
    val title = if (german) "Meine Welt" else "My world"
    val here = if (german) "Hier bin ich: " else "I'm here: "
    val preview = if (german) " · Wegvorschau zu " else " · Route preview to "
    Column(modifier.widthIn(max = 680.dp).fillMaxWidth()
        .background(Color(0xF510202C), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 8.dp)) {
        Text("Fennec · @$viewer · $title", color = Color(0xFF9DDAC7), fontSize = 12.sp, lineHeight = 16.sp)
        Text(reply, color = Color(0xFFF0F5F6), fontSize = 11.sp, lineHeight = 15.sp,
            maxLines = 3, overflow = TextOverflow.Ellipsis)
        Text(here + FennecWorld.name(from, german) +
            (presentation.target?.let { preview + FennecWorld.name(it, german) } ?: ""),
            color = Color(0xFFAEC5D0), fontSize = 10.sp, lineHeight = 12.sp)
        Canvas(Modifier.fillMaxWidth().height(96.dp).padding(horizontal = 22.dp, vertical = 6.dp)) {
            val cellX = size.width / 100f
            val cellY = size.height / 50f
            val cells = PlayMapScene.build(from, route, 100, 50, drawn.value, true)
            for (cell in cells) {
                val color = when {
                    cell.isLight -> Color(0xFFFFD88A)
                    cell.brightness > 1_300 -> Color(0xFF9DDAC7)
                    cell.brightness > 600 -> Color(0xFFAEC5D0)
                    else -> Color(0xFF45616F)
                }
                drawRect(color, Offset(cell.x * cellX, cell.y * cellY), Size(cellX * .75f, cellY * .75f))
            }
            labelPaint.textSize = fontPx
            for ((place, pos) in labels) {
                labelPaint.color = if (place == presentation.target) 0xFFFFD88A.toInt() else 0xFFDAE7EE.toInt()
                drawContext.canvas.nativeCanvas.drawText(FennecWorld.name(place, german),
                    (pos.first + .5f) * cellX, (pos.second + 4f) * cellY + fontPx / 2, labelPaint)
            }
            // Zimmer erscheinen als ein Haus, genau wie im vorhandenen Reisebild.
            val marker = if (PlayMap.regionOf(from) == PlayMap.Region.HOME) Place.LIVING else from
            labels[marker]?.let { (x, y) ->
                drawCircle(Color(0xFF9DDAC7), cellX * 2.4f,
                    Offset((x + .5f) * cellX, (y + .5f) * cellY),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
            }
        }
    }
}
