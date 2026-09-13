package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayEffectsTest {

    @Test
    fun `jede Haupttaetigkeit hat ein eigenes begrenztes Weltmotiv`() {
        val motifs = AnimationType.entries.map { topic ->
            PlayEffects.activityCells(topic, 8, 20, 4, 48).also { cells ->
                assertTrue("$topic hat kein Weltmotiv", cells.isNotEmpty())
                assertTrue("$topic ist als Silhouette zu klein", cells.size >= 18)
                assertTrue("$topic verlaesst das Spielfeld", cells.all { it.x in 0 until 48 })
            }.map { it.x to it.y }.toSet()
        }
        assertEquals(AnimationType.entries.size, motifs.distinct().size)
    }

    @Test
    fun `Weltmotive veraendern ihren Zustand sichtbar`() {
        for (topic in AnimationType.entries) {
            val first = PlayEffects.activityCells(topic, 8, 20, 0, 48).map { it.x to it.y }.toSet()
            val later = PlayEffects.activityCells(topic, 8, 20, 9, 48).map { it.x to it.y }.toSet()
            assertNotEquals("$topic bleibt trotz laufender Szene statisch", first, later)
        }
    }

    @Test
    fun `Fussball bleibt sichtbar und der Trick hebt ihn deutlich an`() {
        val dribble = PlayEffects.footballCells(8, 20, PlayEffects.FootballPhase.DRIBBLE, 0, 48)
        val trick = PlayEffects.footballCells(8, 20, PlayEffects.FootballPhase.TRICK, 0, 48)

        assertTrue(dribble.size >= 9)
        assertTrue(trick.minOf { it.y } < dribble.minOf { it.y } - 5)
        assertTrue((dribble + trick).all { it.x in 0 until 48 })
    }

    /**
     * **Dieser Test hat vorher das KORBBRETT gemessen.**
     *
     * `minOf { it.y }` ueber die ganze Szene ist immer die Oberkante des Bretts, und die steht
     * in jeder Phase gleich hoch - die alte Zusicherung "beim Wurf steigt der Ball" sagte also
     * nichts ueber den Ball. Sie ging nur durch, solange der Wurf den Ball ohne jeden Flug
     * oberhalb des Bretts absetzte; genau dieses Absetzen war der Fehler. Dieselbe Falle steht
     * als Warnung schon im Nachbartest (siehe `ballHoehe` in PlayMotifLegibilityTest).
     *
     * Verfolgt wird jetzt der Glanzpunkt des Balls. Der Korb traegt auch einen - aber einen
     * unbeweglichen, und daran sind die beiden zu unterscheiden.
     */
    @Test
    fun `Basketball prellt und der Wurf fliegt sichtbar zum Korb`() {
        fun glanz(phase: PlayEffects.BasketballPhase, takt: Int) =
            PlayEffects.basketballCells(8, 20, phase, takt, 48)
                .filter { it.brightness == PlayInk.SPARK }
                .map { it.x to it.y }
                .toSet()
        val fest = glanz(PlayEffects.BasketballPhase.DRIBBLE, 0)
            .intersect(glanz(PlayEffects.BasketballPhase.DRIBBLE, 6))
        fun ball(phase: PlayEffects.BasketballPhase, takt: Int) =
            (glanz(phase, takt) - fest).single()

        // Prellen: mehr als zwei Hoehen, sonst ist es ein Blinken zweier Bilder.
        val prellen = (0..12).map { ball(PlayEffects.BasketballPhase.DRIBBLE, it).second }
        assertTrue("Der Ball prellt nur ueber ${prellen.distinct()}", prellen.distinct().size >= 3)

        // Der Wurf: nach rechts UND ueber die Hand hinauf, und nicht in einem Sprung.
        val bahn = (0..8).map { ball(PlayEffects.BasketballPhase.SHOOT, it) }
        assertTrue(
            "Der Ball nimmt beim Wurf nur ${bahn.map { it.first }.distinct().size} Stellen ein",
            bahn.map { it.first }.distinct().size >= 6
        )
        for (i in 1 until bahn.size) {
            assertTrue("Der Wurf laeuft bei Takt $i zurueck", bahn[i].first >= bahn[i - 1].first)
        }
        assertTrue("Der Wurf steigt nicht", bahn.minOf { it.second } < bahn.first().second - 2)

        // Und der Treffer faellt heraus, statt im Netz zu haengen.
        val fall = (0..8).map { ball(PlayEffects.BasketballPhase.SCORE, it).second }
        assertTrue("Der Ball faellt nach dem Treffer nicht", fall.last() > fall.first())

        val alle = PlayEffects.BasketballPhase.entries.flatMap { p ->
            (0..12).flatMap { PlayEffects.basketballCells(8, 20, p, it, 48) }
        }
        assertTrue(alle.all { it.x in 0 until 48 })
    }

    @Test
    fun `Hantel wandert beim Heben deutlich ueber den Kopf`() {
        val warm = PlayEffects.trainingCells(12, 20, PlayEffects.TrainingPhase.WARM_UP, 0)
        val lift = PlayEffects.trainingCells(12, 20, PlayEffects.TrainingPhase.LIFT, 0)
        assertTrue(lift.minOf { it.y } < warm.minOf { it.y } - 10)
        assertTrue(lift.size >= 20)
    }

    @Test
    fun `Musik bekommt vom Stimmen bis zum Finale mehr sichtbare Noten`() {
        val tune = PlayEffects.musicCells(8, 20, PlayEffects.MusicPhase.TUNE, 0, 48)
        val finale = PlayEffects.musicCells(8, 20, PlayEffects.MusicPhase.FINALE, 0, 48)
        assertTrue(finale.size > tune.size + 8)
        assertTrue(finale.all { it.x in 0 until 48 })
    }

    @Test
    fun `Gemaelde waechst von der Skizze bis zur fertigen Leinwand`() {
        val sketch = PlayEffects.paintingCells(8, 20, PlayEffects.PaintingPhase.SKETCH, 0, 48)
        val reveal = PlayEffects.paintingCells(8, 20, PlayEffects.PaintingPhase.REVEAL, 0, 48)
        assertTrue(reveal.size > sketch.size)
        assertTrue(reveal.all { it.x in 0 until 48 })
    }

    @Test
    fun `fliegender Drache hat Koerper Schweif und gespannte Schnur`() {
        val cells = PlayEffects.kiteCells(
            avatarCellX = 12,
            avatarCellY = 40,
            phase = PlayEffects.KitePhase.FLY,
            scenePhase = 0,
            widthCells = 60
        )

        assertTrue("Drache ist zu klein oder ohne Schnur", cells.size >= 20)
        assertTrue("Drache steigt nicht sichtbar ueber den Avatar", cells.minOf { it.y } < 30)
        assertTrue("Schnur erreicht die Hand nicht", cells.any { it.y >= 50 })
    }

    @Test
    fun `Wind bewegt den Drachen ohne ihn aus dem Bild zu schieben`() {
        val first = PlayEffects.kiteCells(40, 35, PlayEffects.KitePhase.FLY, 0, 48)
        val later = PlayEffects.kiteCells(40, 35, PlayEffects.KitePhase.FLY, 5, 48)

        assertNotEquals(first.map { it.x to it.y }.toSet(), later.map { it.x to it.y }.toSet())
        for (cell in later) {
            assertTrue("Drachenzelle ausserhalb der Breite: $cell", cell.x in 0 until 48)
        }
    }
}
