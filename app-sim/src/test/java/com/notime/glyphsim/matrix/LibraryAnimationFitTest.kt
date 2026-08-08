package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.DefaultLibraryAnimations
import com.notime.glyphcore.data.FrameCodec
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft, dass jede mitgelieferte Bibliotheks-Animation innerhalb des RUNDEN Ausschnitts der
 * Matrix bleibt (siehe [MatrixGeometry.isActive]).
 *
 * Die Frames werden in einem quadratischen 13x13-Raster abgelegt, angezeigt wird aber nur der
 * eingeschriebene Kreis - die vier Ecken existieren schlicht nicht. Punkte, die dort liegen,
 * verschwinden spurlos: kein Fehler, kein Absturz, die Animation sieht nur unvollstaendig oder
 * unlesbar aus. Genau das war bei "TAMA" passiert, das seine vier Buchstaben in die vier Ecken
 * gesetzt hatte.
 *
 * Ein paar Punkte ausserhalb sind bei manchen Entwuerfen Absicht (etwas fliegt aus dem Bild),
 * deshalb wird nicht auf null geprueft, sondern auf einen Anteil, ab dem eine Animation
 * erkennbar Substanz verliert.
 */
class LibraryAnimationFitTest {

    /** Ab diesem Anteil unsichtbarer Punkte gilt eine Animation als nicht passend gezeichnet. */
    private companion object {
        const val MAX_CLIPPED_FRACTION = 0.15
        /** Wie weit ein Entwurf absichtlich ueber den Rand hinausreichen darf. */
        const val OUT_OF_GRID_MARGIN = 4
    }

    @Test
    fun everyDefaultAnimationStaysInsideTheRoundMatrix() {
        val offenders = mutableListOf<String>()

        DefaultLibraryAnimations.seed().forEach { animation ->
            val frames = FrameCodec.decode(animation.framesData)
            val total = frames.sumOf { it.size }
            if (total == 0) return@forEach
            val clipped = frames.sumOf { frame ->
                frame.count { (x, y) -> !MatrixGeometry.isActive(x, y) }
            }
            val fraction = clipped.toDouble() / total
            if (fraction > MAX_CLIPPED_FRACTION) {
                offenders += "${animation.label}: ${(fraction * 100).toInt()}% der Punkte liegen ausserhalb"
            }
        }

        assertTrue(
            "Diese Animationen zeichnen zu weit ausserhalb des runden Ausschnitts:\n" +
                offenders.joinToString("\n"),
            offenders.isEmpty()
        )
    }

    /**
     * Faengt wilde Koordinaten ab - also Zahlendreher und Rechenfehler, die einen Punkt weit
     * ausserhalb des Rasters landen lassen.
     *
     * Bewusst mit Spielraum ([OUT_OF_GRID_MARGIN]) statt hart auf 0 bis 12: knapp ueber den Rand
     * hinaus zu zeichnen ist hier ein Gestaltungsmittel, kein Fehler - die Rakete steigt aus dem
     * Bild und laesst ihren Flammenschweif unten herauslaufen. Solche Punkte verwirft
     * FrameCrossfade beim Zeichnen gefahrlos.
     */
    @Test
    fun noDefaultAnimationUsesWildCoordinates() {
        val lower = -OUT_OF_GRID_MARGIN
        val upper = MatrixGeometry.SIZE + OUT_OF_GRID_MARGIN
        DefaultLibraryAnimations.seed().forEach { animation ->
            FrameCodec.decode(animation.framesData).forEachIndexed { index, frame ->
                frame.forEach { (x, y) ->
                    assertTrue(
                        "${animation.label}, Frame $index: Punkt ($x,$y) liegt unplausibel weit ausserhalb",
                        x in lower..upper && y in lower..upper
                    )
                }
            }
        }
    }
}
