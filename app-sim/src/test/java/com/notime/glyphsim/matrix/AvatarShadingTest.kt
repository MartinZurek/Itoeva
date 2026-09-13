package com.notime.glyphsim.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Haelt fest, was die Schattierung einer Kreatur ausmacht.
 *
 * **Der Fehler, der diese Fassung erzwungen hat, war ein unsichtbarer Effekt.** Der erste
 * Entwurf war ein weicher Verlauf ueber die Hoehe. Er war rechnerisch da, messbar da, und man
 * sah ihn nicht: Dreissig Prozent Unterschied auf sechzehn Zeilen sind zwei Prozent von einer
 * Zeile zur naechsten - kein Auge trennt das. Deshalb pruefen die Tests hier nicht mehr, OB
 * abgestuft wird, sondern ob eine **Kante** entsteht, die man sehen kann.
 */
class AvatarShadingTest {

    private fun hell(vararg punkte: Pair<Int, Int>): IntArray {
        val feld = IntArray(AvatarGeometry.SIZE * AvatarGeometry.HEIGHT)
        for ((x, y) in punkte) feld[y * AvatarGeometry.SIZE + x] = AvatarGeometry.MAX_BRIGHTNESS
        return feld
    }

    private fun zeile(frame: IntArray, y: Int): List<Int> =
        (0 until AvatarGeometry.SIZE).map { frame[y * AvatarGeometry.SIZE + it] }

    @Test
    fun `die Figur zerfaellt in genau zwei Helligkeiten`() {
        // Zwei flache Toene mit einer harten Grenze - nicht sechzehn feine Stufen.
        for (species in AvatarSpecies.entries) {
            val schattiert = AvatarShading.shade(AvatarAnimations.idlePose(species))
            val stufen = schattiert.filter { it > 0 }.toSortedSet()
            assertEquals("$species hat $stufen statt zwei Stufen", 2, stufen.size)
        }
    }

    @Test
    fun `der Unterschied zwischen den beiden Stufen ist sichtbar`() {
        // Der eigentliche Zweck. Ein Verhaeltnis nahe 1 waere derselbe unsichtbare Effekt wie
        // vorher, nur anders gerechnet.
        for (species in AvatarSpecies.entries) {
            val schattiert = AvatarShading.shade(AvatarAnimations.idlePose(species))
            val stufen = schattiert.filter { it > 0 }.toSortedSet().toList()
            val verhaeltnis = stufen[0].toFloat() / stufen[1]
            assertTrue("$species: Stufen liegen bei $verhaeltnis zu eng beieinander", verhaeltnis <= 0.92f)
            assertTrue("$species: Stufen liegen bei $verhaeltnis zu weit auseinander", verhaeltnis >= 0.70f)
        }
    }

    @Test
    fun `der Schatten liegt rechts, solange die Figur steht`() {
        // Wie im Vorbild: dunkler Ton auf der rechten Flanke.
        val breit = hell(*(4..11).map { it to 8 }.toTypedArray())
        val zeile = zeile(AvatarShading.shade(breit), 8)
        val belegt = zeile.withIndex().filter { it.value > 0 }
        assertTrue("rechts ist nicht dunkler", belegt.last().value < belegt.first().value)
    }

    @Test
    fun `beim Laufen nach links springt der Schatten auf die andere Seite`() {
        // **Der Grund, warum die Flanke ueberhaupt waehlbar ist.** Das Sprite wird nirgends
        // gespiegelt; der Seitenwechsel des Schattens ist das Einzige, woran man sieht, dass
        // sich die Kreatur umgedreht hat.
        val breit = hell(*(4..11).map { it to 8 }.toTypedArray())
        val nachRechts = zeile(AvatarShading.shade(breit, side = AvatarShading.Side.RIGHT), 8)
        val nachLinks = zeile(AvatarShading.shade(breit, side = AvatarShading.Side.LEFT), 8)
        assertNotEquals(nachRechts, nachLinks)
        val linksBelegt = nachLinks.withIndex().filter { it.value > 0 }
        assertTrue("links ist nicht dunkler", linksBelegt.first().value < linksBelegt.last().value)
    }

    @Test
    fun `die Silhouette bleibt unveraendert`() {
        // Schattierung ist Darstellung, nicht Inhalt: Welche Zellen AN sind, darf sie nicht
        // anfassen - sonst waere die Figur eine andere.
        for (species in AvatarSpecies.entries) {
            val roh = AvatarAnimations.idlePose(species)
            val schattiert = AvatarShading.shade(roh)
            assertEquals(
                "$species: Silhouette veraendert",
                roh.map { it > 0 },
                schattiert.map { it > 0 }
            )
        }
    }

    @Test
    fun `eine halb eingeblendete Zelle bleibt halb`() {
        // Die Helligkeit wird SKALIERT, nicht ersetzt - sonst spraenge jede Ueberblendung beim
        // Zeichnen auf volle Staerke.
        val halb = IntArray(AvatarGeometry.SIZE * AvatarGeometry.HEIGHT)
        for (x in 4..11) halb[8 * AvatarGeometry.SIZE + x] = AvatarGeometry.MAX_BRIGHTNESS / 2
        val schattiert = AvatarShading.shade(halb)
        assertTrue(
            "eine halbe Zelle wurde heller als halb",
            schattiert.filter { it > 0 }.all { it <= AvatarGeometry.MAX_BRIGHTNESS / 2 }
        )
    }

    @Test
    fun `ein fremdes oder zu schmales Feld wird unveraendert durchgereicht`() {
        // Eine Zeichenroutine darf an einem unerwarteten Feld nicht scheitern - die
        // 13x13-Zeichen der Wunsch- und Traumblase laufen genau hier durch.
        val fremd = IntArray(13 * 13) { AvatarGeometry.MAX_BRIGHTNESS }
        assertSame(fremd, AvatarShading.shade(fremd))
        val leer = IntArray(AvatarGeometry.SIZE * AvatarGeometry.HEIGHT)
        assertSame(leer, AvatarShading.shade(leer))
        // Eine Figur von einer Spalte Breite hat keine zweite Flanke.
        val strich = hell(6 to 8, 6 to 9, 6 to 10)
        assertSame(strich, AvatarShading.shade(strich))
    }

    @Test
    fun `das Schattenband misst sich an der Figur, nicht am Raster`() {
        // Sonst saesse die Kante bei einer schmalen Kreatur neben ihr statt in ihr - und eine
        // Figur am linken Rand waere ganz unbeschattet.
        val schmalLinks = hell(*(1..4).map { it to 8 }.toTypedArray())
        val zeile = zeile(AvatarShading.shade(schmalLinks), 8)
        val belegt = zeile.withIndex().filter { it.value > 0 }
        assertTrue("auch eine schmale Figur am Rand bekommt eine Kante", belegt.last().value < belegt.first().value)
    }
}
