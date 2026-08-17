package com.notime.glyphsim.matrix

/**
 * Geteilte Mass-Konstanten fuer [SimulatedMatrixView] und [MatrixBitmapRenderer]:
 * das Punkteraster wird nicht exakt auf den Puck-Durchmesser skaliert (cell =
 * diameter/SIZE), sondern minimal kleiner. Die groesste Distanz einer laut
 * [MatrixGeometry.isActive] aktiven Zelle zur Rastermitte betraegt sqrt(41) ~=
 * 6.403 (z. B. Zelle (2,1)); zusammen mit dem Punktradius (DOT_RADIUS_FACTOR =
 * 0.36) ergibt das 6.763. Die kleinere Skalierung sorgt dafuer, dass dieser Wert
 * innerhalb des halben effektiven Rasters (EFFECTIVE_GRID_UNITS / 2 = 6.86)
 * bleibt statt am Rand des Pucks ueberzustehen.
 */
object MatrixCellSizing {
    const val DOT_RADIUS_FACTOR = 0.36f

    /** cell = Durchmesser / diese Zahl, statt / MatrixGeometry.SIZE. */
    val EFFECTIVE_GRID_UNITS: Float = MatrixGeometry.SIZE + 2 * DOT_RADIUS_FACTOR
}
