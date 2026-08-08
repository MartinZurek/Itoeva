package com.notime.glyphsim.matrix

/**
 * ARGB-Farbwerte der Matrix-"Puck"-Optik, geteilt zwischen der Compose-Ansicht
 * ([SimulatedMatrixView]) und dem Bitmap-Renderer fuers Home-Screen-Widget
 * ([MatrixBitmapRenderer]), damit beide identisch aussehen.
 */
object MatrixColors {
    const val PUCK: Int = 0xFF0A0A0A.toInt()
    const val PUCK_RIM: Int = 0xFF2A2A2A.toInt()
    const val LED_OFF: Int = 0xFF232323.toInt()
    const val LED_ON: Int = 0xFFF3F1EA.toInt()
}
