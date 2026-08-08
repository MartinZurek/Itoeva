package com.notime.glyphkalender.matrix

import kotlin.math.sqrt

/**
 * Geometrie fuer die On-Screen-Vorschau der Glyph Matrix (Testen/Anlegen eines
 * Reminders, siehe ui/ReminderScreen.kt): 13x13-Raster wie beim echten Nothing Phone
 * laut Common.getDeviceMatrixLength(), rund ins Quadrat einbeschrieben. 1:1
 * uebernommen aus matrix/MatrixGeometry.kt im :app-sim-Modul (dort fuer die
 * Simulator-Ansicht entworfen - hier fuer dieselbe Vorschau-Funktion in der echten
 * Hardware-App, siehe README-Anmerkung zur geometrischen Annaeherung dort).
 */
object MatrixGeometry {
    const val SIZE = 13
    const val MAX_BRIGHTNESS = 4095

    private const val CENTER = (SIZE - 1) / 2.0 // 6.0
    private const val RADIUS = SIZE / 2.0 // 6.5

    fun isActive(x: Int, y: Int): Boolean {
        val dx = x - CENTER
        val dy = y - CENTER
        return sqrt(dx * dx + dy * dy) <= RADIUS
    }

    val activeCells: List<Pair<Int, Int>> = buildList {
        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                if (isActive(x, y)) add(x to y)
            }
        }
    }
}
