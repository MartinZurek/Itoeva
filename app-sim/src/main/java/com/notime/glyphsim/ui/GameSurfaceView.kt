package com.notime.glyphsim.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import com.notime.glyphsim.matrix.GameScenes
import com.notime.glyphsim.matrix.GameSurfaces
import com.notime.glyphsim.matrix.PlayControl

/** Native Pixelzeichnung: ihre Oberkante ist zugleich die getestete Landeflaeche. */
@Composable
internal fun GameSurfaceView(scene: GameScenes.Scene, pos: PlayControl.Pos, fade: Float, modifier: Modifier = Modifier) {
    val crate = GameSurfaces.crate(scene) ?: return
    Canvas(modifier) {
        val fit = GameScenes.fit(scene, size.width, size.height, pos.x, pos.depth)
        fun rect(x: Float, y: Float, w: Float, h: Float, color: Long) {
            val (sx, sy) = fit.toScreen(x, y)
            drawRect(Color(color).copy(alpha = fade), Offset(sx, sy), Size(w * fit.scale, h * fit.scale))
        }
        val x = crate.left
        val y = crate.top
        val w = crate.right - x
        rect(x, y, w, 20f, 0xFF382C27)
        rect(x + 2f, y + 2f, w - 4f, 16f, 0xFF80523B)
        rect(x + 2f, y + 2f, w - 4f, 4f, 0xFFB38859)
        for (line in listOf(8f, 13f)) rect(x + 3f, y + line, w - 6f, 1f, 0xFF4E382E)
        for (bar in listOf(5f, w - 9f)) {
            rect(x + bar, y + 3f, 4f, 14f, 0xFFC29561)
            rect(x + bar + 1f, y + 5f, 1f, 1f, 0xFF45342B)
            rect(x + bar + 1f, y + 14f, 1f, 1f, 0xFF45342B)
        }
    }
}
