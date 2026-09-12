package com.notime.glyphsim.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Die Kreatur soll ein Koerper sein, kein Scherenschnitt.**
 *
 * Jede beleuchtete Zelle stand auf voller Helligkeit; die Figur war eine reine An/Aus-Flaeche.
 * [AvatarShading] legt einen Verlauf darueber - von oben links hell nach unten rechts dunkel.
 *
 * ## Was diese Tests halten
 *
 * Nicht die Zahlen - die sind Geschmack und sollen einstellbar bleiben -, sondern die
 * Eigenschaften, ohne die der Effekt umschlaegt. Die wichtigste davon ist die **Monotonie**:
 *
 * Der erste Entwurf hat Kanten gesucht (oben hell, unten dunkel, Koerper dazwischen). Er
 * produzierte auf duennen Teilen hell-dunkel-Streifen, gab abgesetzten Fuessen das hellste
 * Licht der ganzen Figur und liess Aussenspalten zeilenweise zwischen hell und dunkel wechseln.
 * Beim Zusehen flimmerte das. Aufgefallen ist es erst beim Ausdrucken einer echten Silhouette -
 * deshalb steht die Eigenschaft, die das ausschliesst, hier als Test und nicht als Vorsatz.
 */
class AvatarShadingTest {

    private val w = AvatarGeometry.SIZE
    private val h = AvatarGeometry.HEIGHT

    private fun hell(vararg zellen: Pair<Int, Int>): IntArray {
        val frame = IntArray(w * h)
        for ((x, y) in zellen) frame[y * w + x] = AvatarGeometry.MAX_BRIGHTNESS
        return frame
    }

    private fun IntArray.at(x: Int, y: Int) = this[y * w + x]

    @Test
    fun `oben ist heller als unten`() {
        val schattiert = AvatarShading.shade(hell(6 to 4, 6 to 10, 6 to 16))
        assertTrue(schattiert.at(6, 4) > schattiert.at(6, 10))
        assertTrue(schattiert.at(6, 10) > schattiert.at(6, 16))
    }

    @Test
    fun `links ist heller als rechts`() {
        // Mindestens zwei Zeilen hoch: Eine einzeilige Figur wird bewusst unveraendert
        // durchgereicht (siehe den Test weiter unten), dort waere nichts zu messen.
        val schattiert = AvatarShading.shade(hell(3 to 8, 12 to 8, 3 to 9, 12 to 9))
        assertTrue(
            "Ohne Seitenlicht bleibt es ein Farbverlauf statt einer Woelbung",
            schattiert.at(3, 8) > schattiert.at(12, 8)
        )
        assertEquals(
            "Auf derselben Zeile darf nur die Seite den Unterschied machen",
            schattiert.at(3, 9) > schattiert.at(12, 9),
            true
        )
    }

    @Test
    fun `die Helligkeit faellt ueber die Hoehe streng und kehrt nie um`() {
        // **Die Eigenschaft, an der der erste Entwurf gescheitert ist.** Eine Umkehr bedeutet
        // hell-dunkel-hell in derselben Spalte: ein gestreiftes Ohr, ein leuchtender Fuss unter
        // einer Luecke, eine flimmernde Aussenkante bei jeder Bewegung.
        for (species in AvatarSpecies.entries) {
            val schattiert = AvatarShading.shade(AvatarAnimations.idlePose(species))
            for (x in 0 until w) {
                var vorige = Int.MAX_VALUE
                for (y in 0 until h) {
                    val wert = schattiert.at(x, y)
                    if (wert <= 0) continue
                    assertTrue(
                        "$species Spalte $x, Zeile $y wird wieder heller ($vorige -> $wert)",
                        wert <= vorige
                    )
                    vorige = wert
                }
            }
        }
    }

    @Test
    fun `jede Kreatur bekommt tatsaechlich Tiefe`() {
        for (species in AvatarSpecies.entries) {
            val roh = AvatarAnimations.idlePose(species)
            val schattiert = AvatarShading.shade(roh)
            val stufen = schattiert.filter { it > 0 }.toSet()
            assertTrue(
                "$species hat nach dem Schattieren nur ${stufen.size} Helligkeitsstufe(n)",
                stufen.size >= 4
            )
        }
    }

    @Test
    fun `die Silhouette aendert sich nicht`() {
        // Schattieren ist Darstellung, nicht Inhalt: Es darf keine Zelle hinzufuegen oder
        // loeschen. Sonst waeren Kollisionspruefung und Silhouette nicht mehr dasselbe.
        for (species in AvatarSpecies.entries) {
            val roh = AvatarAnimations.idlePose(species)
            val schattiert = AvatarShading.shade(roh)
            for (i in roh.indices) {
                assertEquals(
                    "$species Zelle $i: an/aus hat sich geaendert",
                    roh[i] > 0,
                    schattiert[i] > 0
                )
            }
        }
    }

    @Test
    fun `eine halb eingeblendete Zelle bleibt halb eingeblendet`() {
        // Die vorhandene Helligkeit wird skaliert, nicht ersetzt - sonst sprängen
        // Ueberblendungen und die Daempfung eines Besuchers auf volle Helligkeit.
        val frame = IntArray(w * h)
        frame[8 * w + 6] = AvatarGeometry.MAX_BRIGHTNESS
        frame[14 * w + 6] = AvatarGeometry.MAX_BRIGHTNESS / 2
        val schattiert = AvatarShading.shade(frame)
        val voll = AvatarShading.shade(hell(6 to 8, 6 to 14))
        assertTrue(schattiert.at(6, 14) < voll.at(6, 14))
        assertTrue(schattiert.at(6, 14) > 0)
    }

    @Test
    fun `ein unerwartetes Feld wird unveraendert durchgereicht`() {
        // Eine Zeichenroutine darf an einem fremden Feld nicht scheitern.
        val fremd = IntArray(7)
        assertSame(fremd, AvatarShading.shade(fremd))
        val leer = IntArray(w * h)
        assertSame(leer, AvatarShading.shade(leer))
    }

    @Test
    fun `eine einzeilige Figur bleibt unangetastet`() {
        // Es gibt nichts zu modellieren - und ein Verlauf ueber null Hoehe waere eine Division
        // durch null.
        val strich = hell(4 to 9, 5 to 9, 6 to 9)
        assertSame(strich, AvatarShading.shade(strich))
    }

    @Test
    fun `die Schattierung haengt an der Figur, nicht am Raster`() {
        // Sonst waere eine hockende Kreatur insgesamt dunkler als eine aufrechte, nur weil sie
        // weiter unten im Raster sitzt.
        val oben = AvatarShading.shade(hell(6 to 2, 6 to 6))
        val unten = AvatarShading.shade(hell(6 to 14, 6 to 18))
        assertEquals(oben.at(6, 2), unten.at(6, 14))
        assertEquals(oben.at(6, 6), unten.at(6, 18))
        assertNotEquals(oben.at(6, 2), oben.at(6, 6))
    }
}
