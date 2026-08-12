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

    // ---- Der Kalender ----

    private val day = java.time.LocalDate.of(2026, 3, 2)

    @Test
    fun `das erste Stueck ist sofort da`() {
        // Auf den Anfang wartet niemand. Wer die App installiert und fragt, bekommt eine Antwort -
        // "komm morgen wieder" als allererstes waere die schlechteste Begruessung, die es gibt.
        assertEquals(1, PlayLore.unlockedBy(firstDay = null, today = day))
        assertEquals(1, PlayLore.unlockedBy(firstDay = day, today = day))
    }

    @Test
    fun `je Kalendertag kommt eines dazu`() {
        assertEquals(2, PlayLore.unlockedBy(day, day.plusDays(1)))
        assertEquals(3, PlayLore.unlockedBy(day, day.plusDays(2)))
    }

    @Test
    fun `wer tagelang nicht fragt, bekommt alles Angesammelte`() {
        // **Der Unterschied zwischen einem Rhythmus und einem Belohnungsplan.** Nichts verfaellt,
        // nichts muss taeglich abgeholt werden: Wer eine Woche weg war, hat eine Woche gut.
        assertEquals(5, PlayLore.unlockedBy(day, day.plusDays(4)))
        assertEquals(
            "Nach einer Woche muss die Geschichte vollstaendig sein",
            PlayLore.PIECES,
            PlayLore.unlockedBy(day, day.plusDays(PlayLore.PIECES.toLong()))
        )
    }

    @Test
    fun `mehr als die Geschichte gibt es nicht`() {
        assertEquals(PlayLore.PIECES, PlayLore.unlockedBy(day, day.plusDays(400)))
    }

    @Test
    fun `ein zurueckgestelltes Geraet nimmt nichts weg`() {
        // Wer die Uhr zurueckstellt (oder ueber eine Zeitzone reist), soll nicht ploetzlich
        // weniger wissen als gestern. Freigeschaltet bleibt freigeschaltet.
        assertEquals(1, PlayLore.unlockedBy(day, day.minusDays(3)))
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
