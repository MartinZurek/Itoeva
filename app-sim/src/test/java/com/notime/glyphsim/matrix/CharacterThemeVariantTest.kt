package com.notime.glyphsim.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft die Zuordnung von [AvatarSpecies] zur [MusicRole.CHARACTER_THEME]-Variante.
 *
 * Anders als jede andere [MusicRole] rotiert diese Rolle nicht frei zwischen Stuecken derselben
 * Stimmung, sondern haengt fest am anwesenden Wesen. Diese Datei sichert genau das ab, nicht WAS
 * in den einzelnen Stuecken klingt - die kommen erst mit ITO-0017 bis ITO-0022.
 */
class CharacterThemeVariantTest {

    /** Jede Spezies bekommt genau eine Variante zwischen 1 und der Anzahl der Spezies. */
    @Test
    fun `jede Spezies bekommt genau eine Variante`() {
        for (species in AvatarSpecies.entries) {
            val variant = MusicRole.characterThemeVariant(species)
            assertTrue(
                "$species erhielt eine Variante ausserhalb 1..${AvatarSpecies.entries.size}: $variant",
                variant in 1..AvatarSpecies.entries.size
            )
        }
    }

    /**
     * Keine zwei Spezies teilen sich eine Variante - sonst haetten zwei Wesen dasselbe Thema statt
     * je ein eigenes.
     */
    @Test
    fun `keine zwei Spezies teilen sich eine Variante`() {
        val variants = AvatarSpecies.entries.map { MusicRole.characterThemeVariant(it) }
        assertEquals(variants.size, variants.distinct().size)
    }

    /**
     * Die Zuordnung folgt der Deklarationsreihenfolge (`PUFFLING` = 1 bis `HOOTLET` = 6), nicht
     * einer separat gepflegten Zahlentabelle.
     */
    @Test
    fun `Variante 1 bis 6 entsprechen der Deklarationsreihenfolge`() {
        assertEquals(1, MusicRole.characterThemeVariant(AvatarSpecies.PUFFLING))
        assertEquals(2, MusicRole.characterThemeVariant(AvatarSpecies.STARLET))
        assertEquals(3, MusicRole.characterThemeVariant(AvatarSpecies.WYRMLING))
        assertEquals(4, MusicRole.characterThemeVariant(AvatarSpecies.FENNEC))
        assertEquals(5, MusicRole.characterThemeVariant(AvatarSpecies.GLOOP))
        assertEquals(6, MusicRole.characterThemeVariant(AvatarSpecies.HOOTLET))
    }

    /**
     * **Die wichtigste Zusicherung dieser Datei.** [MusicRole.characterThemeVariant] stuetzt sich
     * auf `AvatarSpecies.entries.indexOf(species) + 1` - und der Index einer bereits deklarierten
     * Spezies in dieser Liste aendert sich nie dadurch, dass eine weitere Spezies ANS ENDE der
     * Enum tritt (in Kotlin/Java bleibt der Ordinal bestehender Eintraege beim Anhaengen stabil,
     * er verschiebt sich nur bei einer Einfuegung in der Mitte oder einer Umsortierung). Diese
     * Gleichheit mit dem Ordinal ist der Beleg dafuer: Eine siebte Spezies wuerde Variante 7
     * bekommen, ohne dass PUFFLING bis HOOTLET ihre Variante 1 bis 6 verlieren.
     */
    @Test
    fun `die Zuordnung bleibt stabil, wenn eine siebte Spezies ans Ende der Enum tritt`() {
        for (species in AvatarSpecies.entries) {
            assertEquals(
                "$species: Variante muss dem Ordinal + 1 entsprechen, sonst waere eine kuenftige " +
                    "Erweiterung am Ende nicht mehr garantiert stabil",
                species.ordinal + 1,
                MusicRole.characterThemeVariant(species)
            )
        }
    }
}
