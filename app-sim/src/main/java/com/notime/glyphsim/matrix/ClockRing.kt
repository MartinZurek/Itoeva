package com.notime.glyphsim.matrix

import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Der aeusserste Ring aktiver Zellen der runden Matrix, im Uhrzeigersinn ab "12
 * Uhr" sortiert - Grundlage fuer den [ClockStyle.RING]-Look (Minuten-Markierung,
 * die am Rand entlangwandert, plus dezente Stundenstriche).
 */
object ClockRing {
    // Zellen, deren Abstand zur Mitte mindestens RADIUS - 1 betraegt, gelten als
    // "aeusserster Ring" - grosszuegig genug, um bei einem 13x13-Kreis eine
    // durchgaengige, geschlossene Kontur zu ergeben (statt einzelner Luecken).
    private const val OUTER_BAND = 1.0

    val perimeterCells: List<Pair<Int, Int>> by lazy {
        MatrixGeometry.activeCells
            .filter { (x, y) -> distanceToCenter(x, y) >= MatrixGeometry.RADIUS - OUTER_BAND }
            .sortedBy { (x, y) -> clockAngle(x, y) }
    }

    private fun distanceToCenter(x: Int, y: Int): Double {
        val dx = x - MatrixGeometry.CENTER
        val dy = y - MatrixGeometry.CENTER
        return sqrt(dx * dx + dy * dy)
    }

    /** Winkel in Radiant, 0 = "12 Uhr" (oben), steigend im Uhrzeigersinn. */
    private fun clockAngle(x: Int, y: Int): Double {
        val dx = x - MatrixGeometry.CENTER
        val dy = y - MatrixGeometry.CENTER
        val angle = atan2(dx, -dy) // atan2(x, -y): 0 oben, waechst im Uhrzeigersinn
        return if (angle < 0) angle + 2 * Math.PI else angle
    }
}
