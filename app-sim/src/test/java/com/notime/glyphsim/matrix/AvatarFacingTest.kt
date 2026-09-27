package com.notime.glyphsim.matrix

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Haelt fest, dass die Figur beim Gehen dorthin schaut, wohin sie geht (siehe [AvatarFacing]).
 *
 * Gemeldet: "Wenn er nach links geht, ist der Blick trotzdem nach rechts geneigt."
 */
class AvatarFacingTest {

    private val w = AvatarGeometry.SIZE
    private val h = AvatarGeometry.HEIGHT

    private fun schwerpunktX(frame: IntArray, zellen: (Int) -> Boolean): Double {
        val xs = frame.indices.filter(zellen).map { it % w }
        return xs.average()
    }

    @Test
    fun `gespiegelt wird Zeile fuer Zeile, zweimal gespiegelt ist wieder das Original`() {
        val feld = IntArray(w * h)
        feld[3 * w + 0] = 7
        feld[3 * w + 5] = 4
        val gespiegelt = AvatarFacing.mirror(feld)
        assertEquals(7, gespiegelt[3 * w + (w - 1)])
        assertEquals(4, gespiegelt[3 * w + (w - 1 - 5)])
        assertEquals(0, gespiegelt[3 * w + 0])
        assertArrayEquals(feld, AvatarFacing.mirror(gespiegelt))
    }

    @Test
    fun `nur der Gang nach links wird gespiegelt`() {
        val feld = AvatarAnimations.walkSequence(AvatarSpecies.FENNEC).frames.first()
        assertSame(feld, AvatarFacing.orient(feld, AvatarShading.Side.NONE))
        assertSame(feld, AvatarFacing.orient(feld, AvatarShading.Side.LEFT))
        assertArrayEquals(AvatarFacing.mirror(feld), AvatarFacing.orient(feld, AvatarShading.Side.RIGHT))
    }

    @Test
    fun `ein Feld, das nicht zum Raster passt, bleibt unveraendert`() {
        val schief = IntArray(7) { it }
        assertSame(schief, AvatarFacing.mirror(schief))
    }

    /**
     * **Der eigentliche Befund, an allen sechs Kreaturen.** Die Gesichter sind frontal gezeichnet
     * - die Neigung steckt im Schwanz des Gang-Bildes. Beim Gang nach links muss sie genau
     * gespiegelt sein.
     */
    @Test
    fun `beim Gang nach links ist jede Kreatur genau gespiegelt`() {
        for (species in AvatarSpecies.entries) {
            val gang = AvatarAnimations.walkSequence(species).frames.first()
            val nachRechts = neigung(gang)
            val nachLinks = neigung(AvatarFacing.orient(gang, AvatarShading.Side.RIGHT))
            assertEquals("$species", -nachRechts, nachLinks, 1e-9)
        }
    }

    /**
     * **Der Schwanz haengt hinten** (gemeldet am 2026-09-26): nach rechts laufen heisst links
     * wedeln, nach links laufen rechts wedeln - dieselbe Seite wie der Schatten, der auf der
     * Flanke liegt, von der die Figur kommt (siehe [AvatarShading]).
     *
     * Bis dahin sass der Schwanz rechts, auf der Seite, in die die Posen blicken. Die fruehere
     * Fassung dieses Tests verlangte genau das ("die Neigung zeigt nach rechts") und hielt damit
     * den Fehler fest: Die Kreatur lief ihrem Schwanz hinterher.
     */
    @Test
    fun `der Schwanz haengt beim Gehen hinten`() {
        for (species in listOf(AvatarSpecies.FENNEC, AvatarSpecies.WYRMLING, AvatarSpecies.PUFFLING)) {
            val gang = AvatarAnimations.walkSequence(species).frames.first()
            assertTrue("$species nach rechts: Schwanz links", neigung(gang) < -0.1)
            assertTrue(
                "$species nach links: Schwanz rechts",
                neigung(AvatarFacing.orient(gang, AvatarShading.Side.RIGHT)) > 0.1
            )
        }
    }

    /** Wie weit der Koerper rechts (+) oder links (-) von der Mitte des Gesichts liegt. */
    private fun neigung(frame: IntArray): Double {
        val gesicht = AvatarAccent.facesIn(frame)
        val koerper = schwerpunktX(frame) { frame[it] > 0 || gesicht.getOrElse(it) { false } }
        val mitte = schwerpunktX(frame) { gesicht.getOrElse(it) { false } }
        return koerper - mitte
    }
}
