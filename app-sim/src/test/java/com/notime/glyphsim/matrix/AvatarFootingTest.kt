package com.notime.glyphsim.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft die Regel, an der die Figur auf dem Boden steht - siehe [AvatarFooting].
 *
 * **Der Fehler, der dazu gefuehrt hat**, war beim Lesen unsichtbar und beim Benutzen sofort da:
 * Zieht man die Uhr groesser, waechst die Zellgroesse, der Boden wandert - und die Figur blieb
 * haengen, wo sie war. Sie schwebte. Dasselbe beim Drehen des Geraets.
 *
 * Automatisch pruefbar ist das, weil sich die Frage sauber stellen laesst: Setzt die Fussreihe
 * auf der Bodenlinie auf, und zwar bei JEDER Zellgroesse? Genau das steht hier.
 */
class AvatarFootingTest {

    /** Wo die Fussreihe der Figur tatsaechlich zu liegen kommt. */
    private fun feetAt(floorPx: Float, avatarPx: Float, groundRow: Int): Float {
        val top = AvatarFooting.topFor(floorPx, avatarPx, groundRow)
        return top + (groundRow + 1) * (avatarPx / AvatarGeometry.SIZE)
    }

    @Test
    fun `die Fuesse stehen bei jeder Groesse auf dem Boden`() {
        // Ueber die ganze Spanne, die durch Ziehen der Uhr entsteht (siehe DockLayoutPrefs:
        // 64 bis 168 dp mal Bildschirmdichte) - der Fehler trat ja gerade beim Wechsel auf.
        val floorPx = 1400f
        for (avatarPx in listOf(120f, 200f, 320f, 480f, 640f)) {
            for (groundRow in 14..19) {
                assertEquals(
                    "Bei Avatargroesse $avatarPx und Fussreihe $groundRow schwebt oder steckt " +
                        "die Figur",
                    floorPx, feetAt(floorPx, avatarPx, groundRow), 0.01f
                )
            }
        }
    }

    @Test
    fun `jede Kreatur steht auf demselben Boden`() {
        // Die Fussreihe ist je Spezies verschieden - ein Schleim sitzt tiefer als eine Eule auf
        // zwei Beinen. Genau deshalb darf sie nicht fest verdrahtet werden: Sonst schwebte
        // ausgerechnet die eine Kreatur, an der niemand nachgemessen hat.
        val floorPx = 900f
        val avatarPx = 256f
        for (species in AvatarSpecies.entries) {
            val groundRow = AvatarBodies.forSpecies(species).groundRow()
            assertEquals(
                "$species steht nicht auf der Bodenlinie",
                floorPx, feetAt(floorPx, avatarPx, groundRow), 0.01f
            )
        }
    }

    @Test
    fun `eine Position ueberlebt das Drehen an derselben anteiligen Stelle`() {
        // Der zweite Teil des Fehlers: Beim Drehen tauschen Breite und Hoehe die Rollen. Rechnet
        // man den alten Pixelwert gegen die NEUEN Masse zurueck, springt die Figur quer durchs
        // Bild - der Bruchteil muss mit den ALTEN Massen ermittelt werden.
        val avatarPx = 200f
        val portrait = 1080f
        val landscape = 2160f

        val before = AvatarFooting.leftFor(0.75f, avatarPx, portrait)
        val fraction = AvatarFooting.fractionOf(before, avatarPx, portrait)
        assertEquals("der Bruchteil geht beim Zurueckrechnen verloren", 0.75f, fraction, 0.001f)

        val after = AvatarFooting.leftFor(fraction, avatarPx, landscape)
        assertEquals(
            "nach dem Drehen steht sie nicht mehr anteilig an derselben Stelle",
            0.75f, AvatarFooting.fractionOf(after, avatarPx, landscape), 0.001f
        )
    }

    @Test
    fun `die Figur wird nie aus dem Bild geschoben`() {
        // Randfaelle, die beim Drehen tatsaechlich auftreten: ein sehr schmales Bild und eine
        // Figur, die breiter ist als der Platz.
        for (widthPx in listOf(0f, 100f, 200f, 2000f)) {
            for (fraction in listOf(0f, 0.5f, 1f, -1f, 2f)) {
                val left = AvatarFooting.leftFor(fraction, avatarPx = 200f, widthPx = widthPx)
                assertTrue("negative Position bei Breite $widthPx", left >= 0f)
                assertTrue(
                    "Figur ragt bei Breite $widthPx heraus",
                    left <= (widthPx - 200f).coerceAtLeast(0f) + 0.01f
                )
            }
        }
    }

    @Test
    fun `ueber der Bildkante bleibt die Figur sichtbar`() {
        // Wenn der Boden sehr hoch liegt (Wald, siehe PlayScene.floorFraction) und die Figur
        // gross gezogen ist, waere der rechnerische Platz oberhalb des Bildes.
        val top = AvatarFooting.topFor(floorPx = 50f, avatarPx = 640f, groundRow = 18)
        assertTrue("die Figur wuerde ueber den oberen Rand hinausgeschoben", top >= 0f)
    }

    /**
     * **Wie weit die Figur danebenliegt, wenn der Boden zum falschen Zeitpunkt abgelesen wird.**
     *
     * Der Boden haengt am Ort und an der Tageszeit (siehe [PlayScene.floorFraction]) und wird beim
     * Ortswechsel weich nachgefuehrt. Die Position der Figur steht dagegen in Pixeln und wird
     * einmal berechnet, wenn ein Schritt beginnt - beim Gang nach draussen faellt dieser Zeitpunkt
     * mitten in die laufende Bodenbewegung.
     *
     * Dieser Test prueft nicht das Nachfuehren selbst; das steckt in einem Compose-Effekt im
     * Dock-Bildschirm und ist ohne Geraet nicht erreichbar. Er haelt fest, **warum es das
     * ueberhaupt braucht**: Der Unterschied zwischen drinnen und draussen-nachts ist gross genug,
     * um die Figur sichtbar in der Luft stehen zu lassen. Wuerde jemand die Bodenhoehen einander
     * angleichen, faellt dieser Test - und dann darf der Effekt auch wieder weg.
     */
    @Test
    fun `zwischen drinnen und draussen bei Nacht liegen Welten`() {
        val hoehePx = 2000f
        val drinnen = PlayScene.floorFraction(
            PlayScene.Place.NOOK, PlayAmbientActivity.DayPhase.NIGHT
        )
        val draussen = PlayScene.floorFraction(
            PlayScene.Place.PARK, PlayAmbientActivity.DayPhase.NIGHT
        )

        val avatarPx = 200f
        val obenDrinnen = AvatarFooting.topFor(hoehePx * drinnen, avatarPx, groundRow = 15)
        val obenDraussen = AvatarFooting.topFor(hoehePx * draussen, avatarPx, groundRow = 15)

        val abstand = kotlin.math.abs(obenDraussen - obenDrinnen)
        assertTrue(
            "Der Bodenunterschied betraegt nur ${abstand}px - dann braeuchte es das Nachfuehren " +
                "im Dock-Bildschirm nicht mehr",
            abstand > avatarPx / 2f
        )
    }
}
