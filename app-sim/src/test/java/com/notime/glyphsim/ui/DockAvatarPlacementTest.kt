package com.notime.glyphsim.ui

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertFalse
import org.junit.Test

/**
 * Uhr und Avatar duerfen sich im Dock-Modus nie ueberschneiden.
 *
 * Hintergrund: Die frueherere Platzierung verglich den Abstand der MITTELPUNKTE gegen
 * `(uhr + avatar) / 2 * 1,2`. Bei einer diagonalen Lage bestand diese Pruefung, obwohl sich die
 * beiden Quadrate sichtbar ueberschnitten - liegen beide Achsen bei 0,85 × halber Summe, ist der
 * euklidische Abstand rund 1,2 × halbe Summe. Dazu kam ein Rueckfall ohne jede Pruefung, der bei
 * grosser Uhr regelmaessig griff.
 *
 * Reine Geometrie, laeuft deshalb ohne Geraet. Durchgerechnet ueber viele Bildschirmgroessen und
 * Uhrpositionen, weil genau die Randlagen das Problem waren.
 */
class DockAvatarPlacementTest {

    /** Wie in DockScreen: der Avatar ist ein fester Bruchteil der Uhr. */
    private fun avatarPxFor(clockPx: Float) = clockPx * 0.62f

    private fun overlaps(
        avatar: Offset,
        avatarPx: Float,
        clock: Offset,
        clockPx: Float
    ): Boolean {
        val separatedX = avatar.x + avatarPx <= clock.x || clock.x + clockPx <= avatar.x
        val separatedY = avatar.y + avatarPx <= clock.y || clock.y + clockPx <= avatar.y
        return !separatedX && !separatedY
    }

    @Test
    fun avatarNeverOverlapsTheClock() {
        // Typische Telefongroessen in Pixeln (schmal bis gross) und Uhrgroessen von klein bis
        // sehr gross - inklusive der Faelle, in denen die Uhr fast den ganzen Schirm einnimmt.
        val screens = listOf(1080f to 2400f, 720f to 1600f, 1440f to 3200f, 900f to 1600f)
        val clockSizes = listOf(200f, 420f, 600f, 900f)

        for ((screenW, screenH) in screens) {
            for (clockPx in clockSizes) {
                if (clockPx >= screenW || clockPx >= screenH) continue
                val avatarPx = avatarPxFor(clockPx)
                val boundX = (screenW - avatarPx).coerceAtLeast(0f)
                val boundY = (screenH - avatarPx).coerceAtLeast(0f)

                // Uhr an neun Stellen: Ecken, Kanten, Mitte.
                for (fx in listOf(0f, 0.5f, 1f)) {
                    for (fy in listOf(0f, 0.5f, 1f)) {
                        val clockOffset = Offset(
                            fx * (screenW - clockPx).coerceAtLeast(0f),
                            fy * (screenH - clockPx).coerceAtLeast(0f)
                        )
                        // Mehrfach, weil die Platzierung eine Zufallskomponente hat.
                        repeat(40) {
                            val placed = randomAvatarOffset(boundX, boundY, avatarPx, clockOffset, clockPx)
                            assertFalse(
                                "Ueberschneidung bei Schirm ${screenW}x$screenH, Uhr $clockPx @ $clockOffset, " +
                                    "Avatar $avatarPx @ $placed",
                                overlaps(placed, avatarPx, clockOffset, clockPx)
                            )
                        }
                    }
                }
            }
        }
    }

    /** Der Avatar muss ausserdem vollstaendig auf dem Schirm bleiben. */
    @Test
    fun avatarStaysOnScreen() {
        val screenW = 1080f
        val screenH = 2400f
        val clockPx = 420f
        val avatarPx = avatarPxFor(clockPx)
        val boundX = screenW - avatarPx
        val boundY = screenH - avatarPx

        repeat(200) {
            val clockOffset = Offset(
                (0..(screenW - clockPx).toInt()).random().toFloat(),
                (0..(screenH - clockPx).toInt()).random().toFloat()
            )
            val placed = randomAvatarOffset(boundX, boundY, avatarPx, clockOffset, clockPx)
            assertFalse(
                "Avatar ragt aus dem Bild: $placed bei Grenzen $boundX/$boundY",
                placed.x < 0f || placed.y < 0f || placed.x > boundX || placed.y > boundY
            )
        }
    }
}
