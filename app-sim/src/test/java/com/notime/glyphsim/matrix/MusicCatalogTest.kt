package com.notime.glyphsim.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft, dass [MusicCatalog] - die einzige Stelle, die einem Musikstueck einen Titel gibt -
 * nicht heimlich von der Wahrheit abweicht, die [MusicRole] selbst schon kennt.
 *
 * Am wichtigsten: die Zuordnung Variante -> Spezies bei [MusicRole.CHARACTER_THEME]. Sie steht
 * bereits in [MusicRole.characterThemeVariant], und [MusicCatalog.speciesFor] leitet sie
 * ABSICHTLICH aus derselben Quelle ab statt aus einer zweiten Tabelle - dieser Test haelt fest,
 * dass genau das so bleibt.
 */
class MusicCatalogTest {

    @Test
    fun `jede Spezies bekommt ueber die Bibliothek dasselbe Charakterthema wie MusicRole selbst`() {
        for (species in AvatarSpecies.entries) {
            val variant = MusicRole.characterThemeVariant(species)
            assertEquals(
                species,
                MusicCatalog.speciesFor(MusicRole.CHARACTER_THEME, variant)
            )
        }
    }

    @Test
    fun `ausserhalb von CHARACTER_THEME gibt es nie eine Spezies`() {
        for (role in MusicRole.entries) {
            if (role == MusicRole.CHARACTER_THEME) continue
            for (variant in 1..MusicRole.MAX_VARIANTS) {
                assertNull(
                    "$role Variante $variant traegt eine Spezies - das darf nur das Charakterthema",
                    MusicCatalog.speciesFor(role, variant)
                )
            }
        }
    }

    @Test
    fun `jede der sechs Spezies hat einen bekannten Titel fuer ihr Thema`() {
        for (species in AvatarSpecies.entries) {
            val variant = MusicRole.characterThemeVariant(species)
            assertTrue(
                "Keine Titel-Zuordnung fuer $species (Variante $variant)",
                MusicCatalog.titleRes(MusicRole.CHARACTER_THEME, variant) != null
            )
        }
    }

    /**
     * [MusicCatalog.DISPLAY_ORDER] ist eine ANDERE Reihenfolge als [MusicRole.entries] (Tageszeit
     * statt Deklaration) - darf sich davon aber nur in der Reihenfolge unterscheiden, nicht in
     * der Menge. Sonst fehlte einer Rolle beim Hoertest unbemerkt der Eintrag.
     */
    @Test
    fun `die Anzeige-Reihenfolge enthaelt jede Rolle genau einmal`() {
        assertEquals(MusicRole.entries.toSet(), MusicCatalog.DISPLAY_ORDER.toSet())
        assertEquals(MusicRole.entries.size, MusicCatalog.DISPLAY_ORDER.size)
    }

    @Test
    fun `jede Rolle hat eine Wann-Beschreibung`() {
        for (role in MusicRole.entries) {
            // Wirft nicht - die when-Verzweigung in roleDescriptionRes ist exhaustiv. Der Test
            // haelt trotzdem fest, DASS jede Rolle abgefragt wird, damit eine neue Rolle nicht
            // durch einen "else"-Zweig unbemerkt durchrutschen kann, sollte roleDescriptionRes
            // spaeter einmal auf eine when-Verzweigung ohne Exhaustivitaetspruefung umgebaut
            // werden.
            assertTrue(MusicCatalog.roleDescriptionRes(role) != 0)
        }
    }
}
