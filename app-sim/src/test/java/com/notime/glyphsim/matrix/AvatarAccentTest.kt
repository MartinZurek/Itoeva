package com.notime.glyphsim.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Haelt fest, dass das Gesicht einer Kreatur beim Zeichnen zuverlaessig gefunden wird.
 *
 * Gefunden statt uebergeben, weil die Augen in [AvatarBody] Loecher sind und durch
 * [com.notime.glyphcore.data.FrameCrossfade] nur noch An, Aus und Ueberblendung ankommt - es gibt
 * keinen Kanal, in dem "diese Zelle ist ein Auge" bis zum Zeichnen mitreisen koennte (siehe
 * [AvatarAccent]).
 */
class AvatarAccentTest {

    private fun anzahl(gesicht: BooleanArray) = gesicht.count { it }

    @Test
    fun `jede Kreatur hat ein Gesicht`() {
        for (species in AvatarSpecies.entries) {
            val gesicht = AvatarAccent.facesIn(AvatarAnimations.idlePose(species))
            assertTrue("$species hat kein gefundenes Gesicht", anzahl(gesicht) > 0)
        }
    }

    @Test
    fun `das Gesicht liegt in der oberen Haelfte der Figur`() {
        // Augen und Mund sitzen im Kopf. Faende die Flutfuellung stattdessen etwas zwischen den
        // Fuessen, waere die Regel falsch und niemand saehe es an der Zellenzahl.
        for (species in AvatarSpecies.entries) {
            val pose = AvatarAnimations.idlePose(species)
            val gesicht = AvatarAccent.facesIn(pose)
            val figurZeilen = (0 until AvatarGeometry.HEIGHT).filter { y ->
                (0 until AvatarGeometry.SIZE).any { x -> pose[y * AvatarGeometry.SIZE + x] > 0 }
            }
            val mitte = (figurZeilen.first() + figurZeilen.last()) / 2
            val gesichtsZeilen = gesicht.indices.filter { gesicht[it] }.map { it / AvatarGeometry.SIZE }
            assertTrue(
                "$species: Gesichtszeilen $gesichtsZeilen liegen nicht im Kopf (Mitte $mitte)",
                gesichtsZeilen.all { it <= mitte + 2 }
            )
        }
    }

    @Test
    fun `die Silhouette selbst gehoert nie zum Gesicht`() {
        // Sonst bekaeme ein Teil des Koerpers die Akzentfarbe und die Figur zerfiele optisch.
        for (species in AvatarSpecies.entries) {
            val pose = AvatarAnimations.idlePose(species)
            val gesicht = AvatarAccent.facesIn(pose)
            for (i in pose.indices) {
                assertTrue(
                    "$species: beleuchtete Zelle $i wurde als Gesicht gezaehlt",
                    !(pose[i] > 0 && gesicht[i])
                )
            }
        }
    }

    @Test
    fun `einzelne eingeschlossene Zellen zaehlen nicht`() {
        // **Der Grund fuer die Mindestgroesse.** Bei WYRMLING schliesst der schlagende Fluegel je
        // nach Laufphase einzelne Zellen am Rand mit ein. Faerbte man die mit, blitzte dort ein
        // farbiger Punkt auf und wieder weg - das liest sich als Fehler.
        //
        // Geprueft wird die Eigenschaft, nicht die Spezies: Ueber einen ganzen Geh-Zyklus darf
        // die Zahl der Gesichtszellen nicht springen.
        for (species in AvatarSpecies.entries) {
            val zahlen = AvatarAnimations.walkSequence(species).frames
                .map { anzahl(AvatarAccent.facesIn(it)) }
            assertEquals("$species: Gesichtsgroesse schwankt im Gang: $zahlen", 1, zahlen.distinct().size)
        }
    }

    @Test
    fun `ein einzelnes Loch wird nicht als Gesicht gelesen`() {
        // Dieselbe Regel an einer gebauten Form, damit sie nicht nur zufaellig fuer die sechs
        // vorhandenen Kreaturen gilt.
        val feld = IntArray(AvatarGeometry.SIZE * AvatarGeometry.HEIGHT)
        for (y in 6..12) for (x in 4..11) feld[y * AvatarGeometry.SIZE + x] = AvatarGeometry.MAX_BRIGHTNESS
        feld[8 * AvatarGeometry.SIZE + 6] = 0
        assertEquals("ein einzelnes Loch wurde gefaerbt", 0, anzahl(AvatarAccent.facesIn(feld)))

        // Zwei zusammenhaengende schon.
        feld[9 * AvatarGeometry.SIZE + 6] = 0
        assertEquals("zwei zusammenhaengende Loecher ergeben kein Gesicht", 2, anzahl(AvatarAccent.facesIn(feld)))
    }

    @Test
    fun `ein fremdes Feld ergibt kein Gesicht statt eines Absturzes`() {
        assertEquals(0, AvatarAccent.facesIn(IntArray(13 * 13)).size)
    }
}
