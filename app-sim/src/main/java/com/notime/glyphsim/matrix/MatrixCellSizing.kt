package com.notime.glyphsim.matrix

/**
 * Geteilte Mass-Konstanten fuer [SimulatedMatrixView] und [MatrixBitmapRenderer]:
 * das Punkteraster wird nicht exakt auf den Puck-Durchmesser skaliert (cell =
 * diameter/SIZE), sondern minimal kleiner, damit auch die aeusserste LED-Reihe -
 * deren Mittelpunkte laut [MatrixGeometry.isActive] exakt auf dem Puck-Radius
 * liegen - komplett innerhalb des Kreises bleibt statt am Rand ueberzustehen.
 */
object MatrixCellSizing {
    const val DOT_RADIUS_FACTOR = 0.36f

    /** cell = Durchmesser / diese Zahl, statt / MatrixGeometry.SIZE. */
    val EFFECTIVE_GRID_UNITS: Float = MatrixGeometry.SIZE + 2 * DOT_RADIUS_FACTOR
}
