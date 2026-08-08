package com.notime.glyphsim.matrix

import kotlin.math.sqrt

/**
 * Geometrie der simulierten Matrix: 13x13-Raster (wie beim echten Nothing Phone
 * (4a) Pro laut Common.getDeviceMatrixLength() im :app-Modul), rund ins Quadrat
 * einbeschrieben.
 *
 * Wichtig: Nothing veroeffentlicht keine offizielle Pixel-fuer-Pixel-Koordinatenliste
 * der 169 LEDs oeffentlich in diesem Repo/Trainingsstand - [isActive] ist deshalb eine
 * geometrische Annaeherung (Kreis mit Radius 6.5 um die Rastermitte), keine
 * pixelidentische Nachbildung der echten Platine. Sieht der echten Anordnung sehr
 * aehnlich (rund, gleiche 13x13-Aufloesung), muss aber nicht 1:1 mit jeder einzelnen
 * LED-Position uebereinstimmen.
 */
object MatrixGeometry {
    const val SIZE = 13
    const val MAX_BRIGHTNESS = 4095

    const val CENTER = (SIZE - 1) / 2.0 // 6.0
    const val RADIUS = SIZE / 2.0 // 6.5

    fun isActive(x: Int, y: Int): Boolean {
        val dx = x - CENTER
        val dy = y - CENTER
        return sqrt(dx * dx + dy * dy) <= RADIUS
    }

    /** Alle aktiven (x,y)-Zellen, einmal berechnet - fuers Zeichnen und fuer Frame-Aufbau. */
    val activeCells: List<Pair<Int, Int>> = buildList {
        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                if (isActive(x, y)) add(x to y)
            }
        }
    }
}
