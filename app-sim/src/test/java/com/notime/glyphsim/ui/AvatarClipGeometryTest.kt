package com.notime.glyphsim.ui

import com.notime.glyphsim.matrix.AvatarAnimations
import com.notime.glyphsim.matrix.AvatarGeometry
import com.notime.glyphsim.matrix.AvatarMood
import com.notime.glyphsim.matrix.AvatarSpecies
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

/**
 * Sichert die Annahme ab, auf der der Abstand der Uhr im [AvatarClipPlayer] beruht.
 *
 * Dass Uhr und Avatar sich im Clip ueberlappten, war zweimal derselbe Fehler: die Position der Uhr
 * war von Hand gesetzt und passte nicht mehr, sobald sich eine Groesse aenderte. Der Abstand
 * rechnet sich jetzt aus [AVATAR_MAX_LIT_RADIUS_CELLS] - dieser Test prueft, dass der Wert die
 * echten Ruhe-Animationen auch wirklich umschliesst. Faellt er, ist nicht der Test falsch, sondern
 * die Konstante zu klein: dann greift eine Spezies weiter aus als beim Aufstellen der Rechnung,
 * und die Uhr wuerde sie beruehren.
 */
class AvatarClipGeometryTest {
    @Test
    fun litIdleCellsStayWithinTheRadiusTheClockDistanceAssumes() {
        val grid = AvatarGeometry.SIZE
        // Mittelpunkt des KOERPERS, nicht des Rasters: Das Raster hat oben Kopffreiheit
        // (siehe AvatarGeometry.HEADROOM), der Koerper sitzt darunter. Die Konstante beschreibt,
        // wie weit die Figur um ihre eigene Mitte ausgreift - daran hat sich nichts geaendert.
        val centerX = grid / 2.0
        val centerY = AvatarGeometry.HEADROOM + grid / 2.0
        var worstRadius = 0.0
        var worstWhere = ""

        for (species in AvatarSpecies.values()) {
            for (mood in AvatarMood.values()) {
                for (frame in AvatarAnimations.idleSequence(species, mood).frames) {
                    for (y in 0 until AvatarGeometry.HEIGHT) {
                        for (x in 0 until grid) {
                            if (frame.getOrElse(y * grid + x) { 0 } <= 0) continue
                            // Die Zelle belegt die Flaeche [x, x+1] x [y, y+1] - zaehlen muss ihre
                            // vom Mittelpunkt aus gesehen aeusserste Ecke.
                            val radius = maxOf(
                                hypot(x - centerX, y - centerY),
                                hypot(x + 1 - centerX, y - centerY),
                                hypot(x - centerX, y + 1 - centerY),
                                hypot(x + 1 - centerX, y + 1 - centerY)
                            )
                            if (radius > worstRadius) {
                                worstRadius = radius
                                worstWhere = "$species/$mood bei ($x,$y)"
                            }
                        }
                    }
                }
            }
        }

        assertTrue(
            "Weiteste leuchtende Ruhe-Zelle: $worstWhere mit $worstRadius Zellen. " +
                "AVATAR_MAX_LIT_RADIUS_CELLS ist $AVATAR_MAX_LIT_RADIUS_CELLS und damit zu klein - " +
                "die Uhr wuerde den Avatar beruehren. Konstante auf mindestens diesen Wert anheben.",
            worstRadius <= AVATAR_MAX_LIT_RADIUS_CELLS
        )
    }
}
