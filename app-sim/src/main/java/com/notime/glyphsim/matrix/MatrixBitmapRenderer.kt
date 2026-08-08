package com.notime.glyphsim.matrix

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

/**
 * Rendert einen Matrix-Frame als android.graphics.Bitmap mit transparentem
 * Hintergrund - fuers Home-Screen-Widget (RemoteViews), das keine Compose-
 * Canvas hosten kann wie [SimulatedMatrixView]. Gleiche Puck-/LED-Optik,
 * gleiche Farben ([MatrixColors]), aber ohne die schwarze quadratische
 * Grundflaeche der In-App-Ansicht: ausserhalb des Kreises bleibt alles
 * transparent, damit das Widget auf dem Homescreen als runder "Puck"
 * erscheint statt als schwarzes Quadrat.
 */
object MatrixBitmapRenderer {

    fun render(frame: IntArray, sizePx: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = sizePx / 2f
        val center = radius

        val puckPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MatrixColors.PUCK }
        canvas.drawCircle(center, center, radius, puckPaint)

        val rimStroke = sizePx / 150f
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MatrixColors.PUCK_RIM
            style = Paint.Style.STROKE
            strokeWidth = rimStroke
        }
        canvas.drawCircle(center, center, radius - rimStroke / 2, rimPaint)

        // Punkteraster minimal kleiner als der Puck (siehe MatrixCellSizing) - sonst
        // ragt die aeusserste LED-Reihe ueber den runden Rand hinaus.
        val cell = sizePx / MatrixCellSizing.EFFECTIVE_GRID_UNITS
        val dotRadius = cell * MatrixCellSizing.DOT_RADIUS_FACTOR
        val gridOrigin = center - MatrixGeometry.SIZE / 2f * cell
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        for ((x, y) in MatrixGeometry.activeCells) {
            val brightness = frame.getOrElse(y * MatrixGeometry.SIZE + x) { 0 }
            val fraction = brightness.coerceIn(0, MatrixGeometry.MAX_BRIGHTNESS).toFloat() / MatrixGeometry.MAX_BRIGHTNESS
            dotPaint.color = lerpColor(MatrixColors.LED_OFF, MatrixColors.LED_ON, fraction)
            canvas.drawCircle(gridOrigin + (x + 0.5f) * cell, gridOrigin + (y + 0.5f) * cell, dotRadius, dotPaint)
        }
        return bitmap
    }

    private fun lerpColor(from: Int, to: Int, fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f)
        val r = Color.red(from) + ((Color.red(to) - Color.red(from)) * f).toInt()
        val g = Color.green(from) + ((Color.green(to) - Color.green(from)) * f).toInt()
        val b = Color.blue(from) + ((Color.blue(to) - Color.blue(from)) * f).toInt()
        return Color.rgb(r, g, b)
    }
}
