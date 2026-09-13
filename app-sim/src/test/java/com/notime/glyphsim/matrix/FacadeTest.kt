package com.notime.glyphsim.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Haelt fest, was eine Hausfassade in der Ferne ausmacht (siehe `PlayScene.facadeCell`).
 *
 * Keiner der Fehler, die hier festgenagelt sind, war eine falsche Zahl:
 *
 * - Die Haeuser waren **Umrisse**. Man sah durch sie hindurch auf den schwarzen Grund, und vier
 *   davon nebeneinander ergaben ein Drahtgitter statt einer Stadt.
 * - Die **Sterne standen davor**. Bei einem Umriss faellt das nicht auf; bei einer geschlossenen
 *   Wand stanzt ein Stern ein Loch hinein, und weil er blinkt, blinkt das Loch mit.
 * - Fensterlichter und Fensteroeffnungen kamen aus **zwei getrennten Listen**, die von Hand
 *   zueinander passen mussten.
 */
class FacadeTest {

    private val breite = 46
    private val boden = 22

    private fun stadt(dayPhase: PlayAmbientActivity.DayPhase): List<SceneCell> = PlayScene.build(
        place = PlayScene.Place.CITY,
        phase = 0,
        widthCells = breite,
        floorY = boden,
        dayPhase = dayPhase,
        species = AvatarSpecies.PUFFLING
    )

    /**
     * Der Ausschnitt des ERSTEN Hauses - es steht am linken Rand und reicht von der Bodenlinie
     * nach oben. Genommen werden nur die Zeilen, in denen nichts davor steht, und nur die
     * Spalten bis zum zweiten Haus: Das niedrigere daneben beginnt bei Spalte 10 und schiebt
     * sonst seine Dachkante mit in die Messung.
     *
     * Spaeter gezeichnete Zellen ueberschreiben frueher gezeichnete, deshalb gewinnt bei doppelt
     * belegten Stellen die letzte - genau wie beim Zeichnen.
     */
    private fun fassade(dayPhase: PlayAmbientActivity.DayPhase): Map<Pair<Int, Int>, SceneCell> =
        stadt(dayPhase)
            .filter { it.x in 1..9 && it.y in 6..14 }
            .associateBy { it.x to it.y }

    @Test
    fun `die Fassade ist geschlossen, man sieht nicht hindurch`() {
        val belegt = stadt(PlayAmbientActivity.DayPhase.MIDDAY).map { it.x to it.y }.toHashSet()
        for (x in 1..9) {
            for (y in 6..14) {
                assertTrue("Loch in der Fassade bei ($x, $y)", (x to y) in belegt)
            }
        }
    }

    @Test
    fun `ein Fenster ist eine Vertiefung, also dunkler als die Wand`() {
        // Eine gefuellte Flaeche in genau einem Grauton waere ein Klotz - dasselbe Problem, das
        // bei den Moebeln zur Oberkanten-Aufhellung gefuehrt hat.
        val stufen = fassade(PlayAmbientActivity.DayPhase.MIDDAY).values
            .map { it.brightness }
            .toSortedSet()
        assertEquals("erwartet werden genau zwei Stufen: Fenster und Wand", 2, stufen.size)
    }

    @Test
    fun `jedes Licht in der Fassade sitzt in einem Fenster`() {
        // **Die Pruefung, die zwei Fehler auf einmal abdeckt.** Ein Licht, das nicht in einer
        // Fensteroeffnung sitzt, ist entweder ein Stern, der vor dem Haus steht, oder ein
        // Fensterlicht, dessen Liste nicht mehr zu den Oeffnungen passt. Beides ist hier schon
        // passiert, und beides sieht man nur, wenn man danach fragt.
        val tags = fassade(PlayAmbientActivity.DayPhase.MIDDAY)
        val dunkelste = tags.values.minOf { it.brightness }
        val fensterPlaetze = tags.filterValues { it.brightness == dunkelste }.keys

        val nachts = fassade(PlayAmbientActivity.DayPhase.NIGHT)
        val lichter = nachts.filterValues { it.isLight }.keys
        assertTrue("nachts brennt in dieser Fassade kein einziges Fenster", lichter.isNotEmpty())
        for (wo in lichter) {
            assertTrue("Licht bei $wo sitzt nicht in einem Fenster", wo in fensterPlaetze)
        }
    }

    @Test
    fun `tagsueber brennt in der Fassade kein Licht`() {
        // So war es schon vor der Fuellung (siehe litWindows: nur abends und nachts) - die
        // Fuellung darf daran nichts geaendert haben.
        val tags = fassade(PlayAmbientActivity.DayPhase.MIDDAY).values
        assertTrue("mittags leuchtet etwas in der Fassade", tags.none { it.isLight })
    }

    @Test
    fun `die Wand bleibt dunkler als das erleuchtete Fenster`() {
        // Behielte die gefuellte Flaeche die Helligkeit des frueheren Umrisses, waere die Stadt
        // das Hellste im Bild und der Blick bliebe hinten haengen statt bei der Figur.
        val nachts = fassade(PlayAmbientActivity.DayPhase.NIGHT).values
        val wand = nachts.filter { !it.isLight }.maxOf { it.brightness }
        val licht = nachts.filter { it.isLight }.minOf { it.brightness }
        assertTrue("Wand $wand gegen Fensterlicht $licht", licht > wand * 3)
    }

    @Test
    fun `dasselbe Haus sieht beim naechsten Zeichnen genauso aus`() {
        // Ein Fenster, das bei jedem Bild neu entscheidet, ob es brennt, flackert. Dieselbe
        // Regel wie beim Gras: Eine Stadt muss stillstehen duerfen.
        val einmal = stadt(PlayAmbientActivity.DayPhase.EVENING)
        val nochmal = stadt(PlayAmbientActivity.DayPhase.EVENING)
        assertEquals(
            einmal.map { Triple(it.x, it.y, it.brightness) },
            nochmal.map { Triple(it.x, it.y, it.brightness) }
        )
    }
}
