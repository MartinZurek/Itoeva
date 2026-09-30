package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayAmbientActivity.DayPhase
import com.notime.glyphsim.matrix.PlayScene.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Licht, das die Umgebung erhellt, und Tiefe durch Ebenen (Anregung: HD-2D). */
class PlayLightTest {

    private val w = 90
    private val floor = 48

    private fun scene(place: Place, day: DayPhase, camera: Float = 0f) =
        PlayScene.build(place, 0, w, floor, day, camera = camera)

    @Test
    fun `am Lagerfeuer ist es nachts heller als weit weg davon`() {
        val cells = scene(Place.CAMP, DayPhase.NIGHT)
        val lit = PlayScene.illumination(cells, 0.58f, w, floor)!!
        val feuer = cells.filter { it.isLight && it.brightness >= 900 && it.y >= floor - 8 }
        assertTrue("kein Feuer gefunden", feuer.isNotEmpty())
        val fx = feuer.map { it.x }.average().toInt()
        val nah = lit[(floor) * w + fx]
        val fern = lit[(floor) * w + (if (fx < w / 2) w - 2 else 1)]
        assertTrue("Boden am Feuer $nah nicht heller als fern $fern", nah > fern + 0.3f)
    }

    @Test
    fun `tagsueber aendert das Licht nichts`() {
        val cells = scene(Place.CAMP, DayPhase.MIDDAY)
        assertEquals(null, PlayScene.illumination(cells, 1f, w, floor))
    }

    @Test
    fun `Angestrahltes bleibt dunkler als jede Lichtquelle`() {
        for (place in listOf(Place.CAMP, Place.STREET, Place.CITY, Place.LIVING, Place.BEACH)) {
            val cells = scene(place, DayPhase.NIGHT)
            val hellstesDing = cells.filterNot { it.isLight }.maxOfOrNull { it.brightness } ?: 0
            assertTrue("$place: $hellstesDing", hellstesDing < PlayScene.GLOW)
        }
    }

    @Test
    fun `die Berge wandern mit der Figur, der Vordergrund nicht`() {
        val links = scene(Place.MOUNTAINS, DayPhase.MIDDAY, camera = -1f)
        val mitte = scene(Place.MOUNTAINS, DayPhase.MIDDAY)
        val rechts = scene(Place.MOUNTAINS, DayPhase.MIDDAY, camera = 1f)
        assertNotEquals(links.toSet(), rechts.toSet())
        // Der Boden (und was darauf steht) bleibt, wo er ist.
        fun boden(c: List<SceneCell>) = c.filter { it.y >= floor }.toSet()
        assertEquals(boden(mitte), boden(links))
        assertEquals(boden(mitte), boden(rechts))
    }

    @Test
    fun `drinnen gibt es keine Parallaxe`() {
        assertEquals(scene(Place.LIVING, DayPhase.MIDDAY).toSet(), scene(Place.LIVING, DayPhase.MIDDAY, 1f).toSet())
    }

    @Test
    fun `verschobene Ebene behaelt ihre Breite und laesst keinen leeren Rand`() {
        val ebene = (0 until w).map { SceneCell(it, 10, 500) }
        for (dx in listOf(-3, -1, 2, 3)) {
            val verschoben = PlayScene.shiftedLayer(ebene, dx, w)
            assertEquals((0 until w).toSet(), verschoben.map { it.x }.toSet())
        }
    }
}
