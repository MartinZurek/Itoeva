package com.notime.glyphsim.matrix

import kotlin.math.sqrt

/**
 * Die Uhr als MONDSICHEL - fuer die naechtliche Park-Szene (siehe ui/DockScreen.kt).
 *
 * **Der Gedanke dahinter.** Ueber der festen Bodenlinie schwebt nachts im Park ein runder,
 * leuchtender Puck - der als Uhr gedacht ist, in diesem Bild aber ohnehin wie ein Himmelskoerper
 * aussieht. Statt gegen diesen Eindruck anzuarbeiten, nimmt die Szene ihn auf: Dieselbe Scheibe
 * zeigt fuer die Dauer der Szene keine Ziffern, sondern eine Sichel, und wandert nach oben.
 *
 * **Warum eine Sichel und kein Vollmond.** Ein voller Kreis waere von der Uhr in ihrer
 * Grundgestalt nicht zu unterscheiden - der runde Puck ist ja schon da. Erst der abgeschnittene
 * Rand macht aus der Scheibe einen Mond. Gezeichnet wird sie als Differenz zweier Kreise: die
 * volle Scheibe minus einer versetzten zweiten - dieselbe Konstruktion, mit der man eine Sichel
 * auch auf Papier anlegt.
 *
 * **Bewusst dieselbe Bedienung.** Der Mond bleibt ziehbar und bleibt das Ziel der Fuetter-Geste:
 * Es ist weiterhin die Uhr, nur anders angezogen. Eine Szene, in der die gewohnte Handhabung
 * ploetzlich nicht mehr gilt, waere ein Fehler und keine Ueberraschung.
 */
object MoonFrame {

    private const val GRID = MatrixGeometry.SIZE

    /**
     * [phase] laesst die Sichel langsam schmaler und wieder breiter werden - der Mond steht nicht
     * still, aber er tut es so langsam, dass es beim Hinsehen nicht auffaellt und beim laengeren
     * Zuschauen doch.
     */
    fun build(phase: Int = 0): IntArray {
        val frame = IntArray(GRID * GRID)
        val center = MatrixGeometry.CENTER
        // Der ausschneidende Kreis wandert leicht hin und her; sein Abstand bestimmt die Breite
        // der Sichel.
        val shift = 3.6 + 0.9 * kotlin.math.sin(phase / 9.0)
        for ((x, y) in MatrixGeometry.activeCells) {
            val dx = x - center
            val dy = y - center
            val inDisc = sqrt(dx * dx + dy * dy) <= MatrixGeometry.RADIUS - 0.5
            if (!inDisc) continue
            val cx = x - (center + shift)
            val cy = y - (center - 0.6)
            val inCutout = sqrt(cx * cx + cy * cy) <= MatrixGeometry.RADIUS - 0.8
            if (inCutout) continue
            // Zum aeusseren Rand hin etwas schwaecher - das gibt der Sichel eine Rundung, statt
            // sie als flache Flaeche stehen zu lassen.
            val edge = sqrt(dx * dx + dy * dy) / MatrixGeometry.RADIUS
            val brightness = (MatrixGeometry.MAX_BRIGHTNESS * (1.0 - 0.30 * edge)).toInt()
            frame[y * GRID + x] = brightness.coerceIn(0, MatrixGeometry.MAX_BRIGHTNESS)
        }
        return frame
    }

    /** Ob die Sichel ueberhaupt etwas zeichnet - Absicherung gegen eine Geometrie, bei der sich
     *  die beiden Kreise vollstaendig ueberdecken wuerden. */
    fun isVisible(phase: Int = 0): Boolean = build(phase).any { it > 0 }
}
