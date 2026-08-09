package com.notime.glyphsim.matrix

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft, dass Gloop sich wie etwas Weiches verhaelt und nicht wie ein Bild, das man skaliert.
 *
 * **Warum das pruefbar ist, obwohl es um Aussehen geht.** Elastizitaet ist keine Geschmacksfrage,
 * sondern eine Rechnung: Wird er breiter, muss er flacher werden, und der Boden darf sich nicht
 * bewegen. Genau daran erkennt das Auge Gummi - ein Koerper, der beim Breiterwerden einfach
 * groesser wird, sieht aufgeblasen aus, und einer, der beim Zusammendruecken in den Boden faehrt,
 * sieht nach einem Fehler in der Geometrie aus.
 */
class GloopShapeTest {

    private val phases = -3..3

    @Test
    fun `der Boden bleibt liegen, was auch immer er tut`() {
        // Das Wichtigste von allem: Er sitzt auf dem Boden. Sinkt die Unterkante beim
        // Zusammendruecken mit, versinkt er in der Kulisse - und beim Strecken schwebt er.
        for (phase in phases) {
            val bottom = GloopShape.body(phase).maxOf { it.second }
            assertEquals("Phase $phase sitzt nicht auf dem Boden", GloopShape.BOTTOM_ROW, bottom)
        }
    }

    @Test
    fun `breiter heisst flacher - die Flaeche bleibt`() {
        val restArea = GloopShape.body(0).size
        for (phase in phases) {
            val area = GloopShape.body(phase).size
            val drift = abs(area - restArea).toFloat() / restArea
            assertTrue(
                "Phase $phase hat $area Zellen statt etwa $restArea - er wird groesser statt " +
                    "gedrueckt, und das sieht aufgeblasen aus.",
                drift <= 0.2f
            )
        }
    }

    @Test
    fun `quetschen macht breit, strecken macht hoch`() {
        fun width(phase: Int) = GloopShape.body(phase).let { it.maxOf { c -> c.first } - it.minOf { c -> c.first } }
        fun height(phase: Int) = GloopShape.body(phase).let { GloopShape.BOTTOM_ROW - it.minOf { c -> c.second } }

        for (phase in -2..2) {
            assertTrue(
                "Phase $phase ist nicht breiter als Phase ${phase - 1}",
                width(phase) >= width(phase - 1)
            )
            assertTrue(
                "Phase $phase ist nicht flacher als Phase ${phase - 1}",
                height(phase) <= height(phase - 1)
            )
        }
    }

    @Test
    fun `er bleibt in einem Stueck und im Raster`() {
        // Eine Ellipse, die aus dem Raster laeuft, waere links und rechts abgeschnitten - aus
        // einem Tropfen wuerde ein Balken.
        for (phase in phases) {
            val cells = GloopShape.body(phase)
            assertTrue("Phase $phase ist leer", cells.isNotEmpty())
            assertTrue(
                "Phase $phase reicht ueber den Rand",
                cells.all { it.first in 0 until AvatarGeometry.SIZE && it.second >= 0 }
            )
            // Jede Zeile muss durchgehend gefuellt sein - ein Loch in der Mitte waere kein
            // Koerper mehr.
            for ((row, inRow) in cells.groupBy { it.second }) {
                val xs = inRow.map { it.first }.sorted()
                assertEquals(
                    "Phase $phase hat in Zeile $row eine Luecke",
                    xs.last() - xs.first() + 1, xs.size
                )
            }
        }
    }

    @Test
    fun `das Gesicht wandert mit der Form`() {
        // Der Fehler, den das behebt: Der Koerper zog sich in die Hoehe, Auge und Mund blieben
        // stehen - und sassen dadurch am unteren Rand eines Wesens, dessen Gesicht in der Mitte
        // sein sollte.
        assertEquals("in Ruhe darf nichts verschoben werden", 0, GloopShape.holeShift(0))
        assertTrue("gestreckt wandert das Gesicht nicht nach oben", GloopShape.holeShift(-3) < 0)
        assertTrue("gequetscht wandert das Gesicht nicht nach unten", GloopShape.holeShift(3) > 0)

        // Und es muss innerhalb des Koerpers bleiben.
        for (phase in phases) {
            val eyeRow = 9 + GloopShape.holeShift(phase)
            val cells = GloopShape.body(phase)
            assertTrue(
                "Bei Phase $phase liegt das Auge in Zeile $eyeRow ausserhalb des Koerpers",
                cells.any { it.second == eyeRow }
            )
        }
    }

    @Test
    fun `der Fortsatz haengt am Koerper und nicht in der Luft`() {
        // Er ist ein Stueck Koerper, das herausquillt - kein angeklebtes Brett. Beruehrt er den
        // Koerper nicht, ist er beides nicht.
        for (phase in intArrayOf(-3, -2, 0)) {
            val body = GloopShape.body(phase).toSet()
            val arm = GloopShape.reach(extent = 3, toRight = true, squash = phase)
            assertTrue("Phase $phase liefert keinen Fortsatz", arm.isNotEmpty())
            val touches = arm.any { (x, y) -> ((x - 1) to y) in body || ((x + 1) to y) in body }
            assertTrue("Bei Phase $phase haengt der Fortsatz frei in der Luft", touches)
        }
    }
}
