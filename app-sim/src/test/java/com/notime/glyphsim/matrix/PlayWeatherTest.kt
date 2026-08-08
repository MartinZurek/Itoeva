package com.notime.glyphsim.matrix

import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft das Wetter - sowohl WELCHES es gibt als auch WO es gezeichnet wird.
 *
 * Der zweite Teil ist der wichtigere: Regen im Wohnzimmer waere kein Schoenheitsfehler, sondern
 * ein Loch in der Welt. Drinnen darf er ausschliesslich im Fenster erscheinen, und in einem
 * Zimmer ohne Fenster ueberhaupt nicht.
 */
class PlayWeatherTest {

    @After
    fun reset() {
        // Der Schalter liegt im Speicher und wuerde sonst in andere Tests hinueberwirken.
        PlayWeather.forceForPreview(null)
    }

    // ---- Welches Wetter ----

    @Test
    fun `derselbe Tag hat immer dasselbe Wetter`() {
        // Wetter, das sich bei jedem Hinsehen neu entscheidet, ist kein Wetter, sondern Flimmern.
        val day = LocalDate.of(2026, 3, 14)
        val first = PlayWeather.forDate(day)
        repeat(20) { assertEquals("das Wetter wechselt beim Nachfragen", first, PlayWeather.forDate(day)) }
    }

    @Test
    fun `es schneit nicht im Sommer`() {
        // Eine Welt, in der es im Juli schneit, ist keine Welt mehr, sondern eine Zufallsanzeige.
        for (month in 3..11) {
            for (day in 1..28) {
                val date = LocalDate.of(2026, month, day)
                assertTrue(
                    "Schnee am ${date}",
                    PlayWeather.forDate(date) != PlayWeather.SNOW
                )
            }
        }
    }

    @Test
    fun `die meisten Tage sind klar`() {
        // Wetter, das staendig da ist, hoert auf aufzufallen - dann waere der ganze Aufwand
        // umsonst. Geprueft ueber ein volles Jahr.
        var clear = 0
        var total = 0
        var date = LocalDate.of(2026, 1, 1)
        while (date.year == 2026) {
            if (PlayWeather.forDate(date) == PlayWeather.CLEAR) clear++
            total++
            date = date.plusDays(1)
        }
        val share = clear.toFloat() / total
        assertTrue("nur ${(share * 100).toInt()} % klare Tage - das ist zu viel Wetter", share >= 0.7f)
        assertTrue("gar kein Wetter uebers Jahr", share <= 0.95f)
    }

    // ---- Wo es gezeichnet wird ----

    private val width = ScenePreview.WIDTH
    private val floorY = ScenePreview.FLOOR_Y

    private fun cellsWith(place: PlayScene.Place, weather: PlayWeather?): Set<Triple<Int, Int, Int>> {
        PlayWeather.forceForPreview(weather)
        return PlayScene.build(place, 3, width, floorY, PlayAmbientActivity.DayPhase.MIDDAY)
            .map { Triple(it.x, it.y, it.brightness) }
            .toSet()
    }

    @Test
    fun `in einem Zimmer ohne Fenster regnet es nicht`() {
        // Die Kueche hat keins. Waere die Pruefung "wenn drinnen, dann nichts", uebersaehe sie
        // genau den Fall, um den es geht - deshalb ueber ALLE fensterlosen Raeume.
        for (place in PlayScene.Place.entries.filterNot { PlayScene.isOutdoors(it) }) {
            val dry = cellsWith(place, PlayWeather.CLEAR)
            val wet = cellsWith(place, PlayWeather.RAIN)
            val extra = wet - dry
            if (extra.isEmpty()) continue
            // Gibt es Zusatzzellen, muss es ein Fenster geben - und sie muessen wenige sein.
            assertTrue(
                "$place bekommt $extra Regenzellen, obwohl drinnen nur das Fenster nass werden " +
                    "darf",
                extra.size <= 12
            )
        }
    }

    @Test
    fun `drinnen bleibt der Regen ueber dem Boden`() {
        // Ein Tropfen auf Bodenhoehe waere eine Pfuetze im Wohnzimmer.
        for (place in PlayScene.Place.entries.filterNot { PlayScene.isOutdoors(it) }) {
            val extra = cellsWith(place, PlayWeather.RAIN) - cellsWith(place, PlayWeather.CLEAR)
            for ((_, y, _) in extra) {
                assertTrue("$place: Regen auf Zeile $y, der Boden liegt bei $floorY", y < floorY)
            }
        }
    }

    @Test
    fun `draussen faellt spuerbar mehr als drinnen`() {
        // Die Probe aufs Ganze: Draussen ist der ganze Himmel zu sehen, drinnen ein Ausschnitt.
        // Waeren beide gleich dicht, saehe das Fenster zugefroren aus.
        val outside = (cellsWith(PlayScene.Place.PARK, PlayWeather.RAIN) -
            cellsWith(PlayScene.Place.PARK, PlayWeather.CLEAR)).size
        val inside = (cellsWith(PlayScene.Place.NOOK, PlayWeather.RAIN) -
            cellsWith(PlayScene.Place.NOOK, PlayWeather.CLEAR)).size
        assertTrue("draussen faellt kein Regen", outside > 20)
        assertTrue("drinnen ist es genauso dicht wie draussen", inside < outside / 3)
    }
}
