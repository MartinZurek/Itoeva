package com.notime.glyphsim.matrix

import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.roundToInt

/** Ein zusammenhaengendes Texturnetz statt gegeneinander verschobener Stoffkacheln. */
internal class GameCharacterPainter {
    private val follow = GameCharacterMotion.Follow()
    private val vertices = FloatArray(GameCharacterMotion.VERTICES * 2)
    private val colors = IntArray(GameCharacterMotion.VERTICES)
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private var response = GameCharacterMotion.Response()
    private var speed = 0f
    private var wind = 0f
    private var clock = 0L
    private var gait = 0L

    fun update(clock: Long, gait: Long, speed: Float, wind: Float) {
        this.clock = clock; this.gait = gait; this.speed = speed; this.wind = wind
        response = follow.update(clock, speed, wind)
    }

    fun draw(scope: DrawScope, image: ImageBitmap, species: AvatarSpecies, frame: Int, top: Int,
        left: Float, y: Float, size: Float, alpha: Float, dim: Float,
        light: GameSceneLighting.CharacterLight?, mirrored: Boolean) {
        GameCharacterMotion.fill(vertices, species, frame, top / 128f, clock, gait, speed, wind, response)
        var i = 0
        val referenceTop = GameCharacterScale.reference(species).top
        for (row in 0..GameCharacterMotion.ROWS) for (column in 0..GameCharacterMotion.COLUMNS) {
            val u = column.toFloat() / GameCharacterMotion.COLUMNS
            val v = row.toFloat() / GameCharacterMotion.ROWS
            val bodyV = ((v * 128f - referenceTop) / (126f - referenceTop)).coerceIn(0f, 1f)
            val x = if (mirrored) 1f - u else u
            // Dieselbe bilineare Lichtrechnung ohne vier Rgb-Zwischenobjekte je Netzpunkt.
            fun component(a: Float, b: Float, c: Float, d: Float) =
                ((a*(1f-x)+b*x)*(1f-bodyV)+(c*(1f-x)+d*x)*bodyV).coerceIn(.18f, 1f)
            fun channel(value: Float) = (value * dim * 255f).roundToInt().coerceIn(0, 255)
            val r = light?.let { component(it.left.r,it.right.r,it.lowerLeft.r,it.lowerRight.r) } ?: 1f
            val g = light?.let { component(it.left.g,it.right.g,it.lowerLeft.g,it.lowerRight.g) } ?: 1f
            val b = light?.let { component(it.left.b,it.right.b,it.lowerLeft.b,it.lowerRight.b) } ?: 1f
            colors[i] = (255 shl 24) or (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
            vertices[i * 2] = left + vertices[i * 2] * size
            vertices[i * 2 + 1] = y + vertices[i * 2 + 1] * size
            i++
        }
        paint.alpha = (alpha.coerceIn(0f, 1f) * 255f).roundToInt()
        // Die Farbe wird mit dem Sprite-Alpha multipliziert: keine dunkle Rechteckflaeche.
        scope.drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawBitmapMesh(image.asAndroidBitmap(), GameCharacterMotion.COLUMNS,
                GameCharacterMotion.ROWS, vertices, 0, colors, 0, paint)
        }
    }
}
