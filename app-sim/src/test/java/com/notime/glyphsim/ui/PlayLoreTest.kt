package com.notime.glyphsim.ui

import com.notime.glyphsim.matrix.AvatarSpecies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft den Aufbau der Geschichten (siehe [PlayLore]) - nicht die Texte selbst, sondern das, was
 * an ihnen strukturell stimmen muss.
 *
 * **Warum das ueberhaupt geprueft gehoert.** Eine vergessene Zeile in einer Liste aus zweiundvierzig
 * Textverweisen faellt niemandem auf: Das Wesen erzaehlt dann eben ein Stueck weniger, und wer
 * soll wissen, dass da eines fehlte? Genau solche Luecken schleichen sich beim Ergaenzen ein.
 */
class PlayLoreTest {

    @Test
    fun `jedes Wesen hat gleich viel zu erzaehlen`() {
        // Gleich viel, damit keines vorzeitig verstummt - wer sich fuer eine Kreatur entscheidet,
        // soll nicht die kuerzere Geschichte erwischt haben.
        for (species in AvatarSpecies.entries) {
            assertEquals(
                "$species hat nicht ${PlayLore.PIECES} Stuecke",
                PlayLore.PIECES,
                PlayLore.story(species).size
            )
        }
    }

    @Test
    fun `kein Stueck kommt zweimal vor`() {
        // Zwei gleiche Verweise waeren ein Tippfehler, der sich als Wiederholung tarnt: Das Wesen
        // erzaehlt dieselbe Sache zweimal, und die andere gar nicht.
        val all = AvatarSpecies.entries.flatMap { PlayLore.story(it) }
        assertEquals(
            "Ein Text wird von zwei Stellen verwendet",
            all.size,
            all.toSet().size
        )
        assertTrue("Ein Stueck verweist ins Leere", all.none { it == 0 })
    }

    @Test
    fun `jedes Wesen hat eine eigene Geschichte`() {
        // Der Zweck des Ganzen: Die sechs sollen sich nicht bloss anders bewegen, sondern anders
        // KLINGEN. Geteilte Texte waeren der schnellste Weg, das wieder einzuebnen.
        val perSpecies = AvatarSpecies.entries.associateWith { PlayLore.story(it).toSet() }
        for (a in AvatarSpecies.entries) {
            for (b in AvatarSpecies.entries) {
                if (a.ordinal >= b.ordinal) continue
                val shared = perSpecies.getValue(a).count { it in perSpecies.getValue(b) }
                assertEquals("$a und $b teilen sich Text", 0, shared)
            }
        }
    }
}
