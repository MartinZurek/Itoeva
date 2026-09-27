package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Haelt den Weltausbau vom 26.09.2026 fest (siehe [PlayWorld]): fuenf neue Landschaften, Leben in
 * Stadt und Laden, der Supermarkt.
 *
 * Geprueft wird, was man am Geraet nur mit Glueck saehe - ob ein Ausflug seinen Ort ueberhaupt
 * erreicht, ob die Passanten je vorbeikommen, ob die neue Ebene die Figur ueberstrahlt, ob nachts
 * jemand in den Sumpf geht.
 */
class PlayWorldTest {

    private val width = 54
    private val floorY = 88

    @Before
    fun clearSky() {
        PlayWeather.forceForPreview(PlayWeather.CLEAR)
    }

    @After
    fun restoreSky() {
        PlayWeather.forceForPreview(null)
    }

    private fun build(
        place: PlayScene.Place,
        phase: Int = 0,
        dayPhase: PlayAmbientActivity.DayPhase = PlayAmbientActivity.DayPhase.MIDDAY,
        w: Int = width
    ) = PlayScene.build(place, phase, w, floorY, dayPhase)

    @Test
    fun `die neuen Landschaften liegen draussen und haben einen Sitzplatz`() {
        for (place in PlayWorld.NATURE) {
            assertTrue("$place liegt nicht draussen", PlayScene.isOutdoors(place))
            assertTrue(
                "$place hat keinen Sitzplatz",
                PlayScene.Station.BENCH in PlayScene.stationsAt(place)
            )
            for (w in intArrayOf(PlayScene.MIN_SCENE_CELLS, width, 96)) {
                assertTrue(
                    "$place: Sitzplatz bei Breite $w nicht erreichbar",
                    PlayScene.stationSpot(place, PlayScene.Station.BENCH, w, floorY) != null
                )
            }
        }
    }

    @Test
    fun `jede Landschaft wird von einem Ausflug tatsaechlich besucht`() {
        val besucht = PlayRoutines.allFor(AnimationType.MOVE).flatMap { routine ->
            routine.steps.mapNotNull { (it as? RoutineStep.GoToPlace)?.place }
        }.toSet()
        for (place in PlayWorld.NATURE) assertTrue("$place wird nie besucht", place in besucht)
    }

    @Test
    fun `nachts geht niemand in die Wildnis`() {
        val nachts = PlayRoutines.distributionFor(AnimationType.MOVE, night = true) +
            PlayRoutines.distributionFor(AnimationType.MOVE, preferPlaceChange = true, night = true)
        for ((routine, _) in nachts) {
            assertFalse(
                "Nachts erlaubt: $routine",
                routine.steps.any { it is RoutineStep.GoToPlace && it.place in PlayWorld.NATURE }
            )
        }
        repeat(200) { seed ->
            val gewaehlt = PlayRoutines.forTopic(
                AnimationType.MOVE, night = true, random = kotlin.random.Random(seed)
            )
            assertFalse(gewaehlt.steps.any { it is RoutineStep.GoToPlace && it.place in PlayWorld.NATURE })
        }
        // Tagsueber dagegen schon - sonst waere die Regel ein Verbot.
        val tags = PlayRoutines.distributionFor(AnimationType.MOVE).map { it.first }
        assertTrue(tags.any { r -> r.steps.any { it is RoutineStep.GoToPlace && it.place in PlayWorld.NATURE } })
    }

    @Test
    fun `die Verteilung passt nachts zur Wahl`() {
        val summe = PlayRoutines.distributionFor(AnimationType.MOVE, night = true).sumOf { it.second }
        assertEquals(1.0, summe, 1e-9)
    }

    @Test
    fun `jede Landschaft hat einen eigenen fernen Hintergrund`() {
        val bilder = PlayWorld.NATURE.associateWith { place ->
            PlayWorld.background(place, 0, width, floorY, PlayAmbientActivity.DayPhase.MIDDAY)
                .map { it.x to it.y }.toSet()
        }
        for ((place, zellen) in bilder) assertTrue("$place hat keinen Hintergrund", zellen.size > 60)
        val liste = bilder.entries.toList()
        for (i in liste.indices) for (j in i + 1 until liste.size) {
            val a = liste[i].value
            val b = liste[j].value
            val gemeinsam = a.count { it in b }.toDouble() / minOf(a.size, b.size)
            assertTrue("${liste[i].key} und ${liste[j].key} sehen gleich aus", gemeinsam < 0.6)
        }
    }

    @Test
    fun `die neue Kulisse ueberstrahlt nie die Figur`() {
        // Materie bleibt unter dem Glanzlicht, Leute und Fahrzeuge unter den Moebeln.
        for (place in PlayScene.Place.entries) {
            for (phase in 0 until 400 step 7) {
                for (cell in PlayWorld.midground(place, phase, width, floorY, PlayAmbientActivity.DayPhase.MIDDAY)) {
                    assertTrue("$place: Passant zu hell (${cell.brightness})", cell.brightness < PlayScene.FURNITURE)
                }
            }
            for (cell in PlayWorld.background(place, 0, width, floorY, PlayAmbientActivity.DayPhase.MIDDAY)) {
                if (!cell.isLight) {
                    assertTrue("$place: Hintergrund zu hell (${cell.brightness})", cell.brightness < PlayScene.FURNITURE)
                }
            }
        }
    }

    @Test
    fun `in Stadt und Laden kommt Leben vorbei`() {
        fun irgendwann(place: PlayScene.Place): Boolean = (0 until 3_000 step 5).any { phase ->
            PlayWorld.midground(place, phase, width, floorY, PlayAmbientActivity.DayPhase.MIDDAY).isNotEmpty()
        }
        for (place in listOf(PlayScene.Place.STREET, PlayScene.Place.CITY, PlayScene.Place.SHOP,
            PlayScene.Place.PARK, PlayScene.Place.BEACH, PlayScene.Place.PLAINS)) {
            assertTrue("$place: nie kommt jemand vorbei", irgendwann(place))
        }
        // ... aber nicht ununterbrochen - ein Weg, auf dem immer jemand geht, ist ein Fliessband.
        val leer = (0 until 3_000 step 5).count { phase ->
            PlayWorld.midground(PlayScene.Place.PARK, phase, width, floorY, PlayAmbientActivity.DayPhase.MIDDAY)
                .isEmpty()
        }
        assertTrue("Im Park ist immer jemand unterwegs", leer > 100)
    }

    @Test
    fun `nachts ist die Stadt ruhiger`() {
        fun belebt(dayPhase: PlayAmbientActivity.DayPhase) = (0 until 3_000 step 5).sumOf { phase ->
            PlayWorld.midground(PlayScene.Place.CITY, phase, width, floorY, dayPhase).size
        }
        assertTrue(belebt(PlayAmbientActivity.DayPhase.NIGHT) < belebt(PlayAmbientActivity.DayPhase.MIDDAY))
    }

    @Test
    fun `die Regalwand des Ladens scheint nicht durch Kasse und Regal`() {
        for (w in intArrayOf(PlayScene.MIN_SCENE_CELLS, 46, width, 72)) {
            val vorn = PlayScene.propFootprints(PlayScene.Place.SHOP, w, floorY)
                .flatMap { it.second }.toSet()
            val bild = PlayScene.build(PlayScene.Place.SHOP, 0, w, floorY, PlayAmbientActivity.DayPhase.MIDDAY)
                .associateBy { it.x to it.y }
            val wand = PlayWorld.background(
                PlayScene.Place.SHOP, 0, w, floorY, PlayAmbientActivity.DayPhase.MIDDAY,
                PlayScene.propFootprints(PlayScene.Place.SHOP, w, floorY).map { (_, cells) ->
                    (cells.minOf { it.first }..cells.maxOf { it.first }) to cells.minOf { it.second }
                }
            )
            assertTrue("Laden bei Breite $w ohne Regalwand", wand.size > 40)
            for (cell in wand) {
                assertFalse("Regalwand in einer Vordergrund-Requisite bei ${cell.x to cell.y}", (cell.x to cell.y) in vorn)
            }
            assertTrue(bild.isNotEmpty())
        }
    }

    @Test
    fun `die Bank der Stadt ist wieder zu sehen`() {
        // Vorher wurden die Fassaden NACH der Bank gezeichnet und uebermalten sie ganz.
        val bank = PlayScene.propCellsAt(PlayScene.Place.CITY, PlayScene.Station.BENCH, width, floorY)
        assertTrue(bank.isNotEmpty())
        val bild = build(PlayScene.Place.CITY).associateBy { it.x to it.y }
        val sichtbar = bank.count { (bild[it]?.brightness ?: 0) >= PlayScene.FURNITURE }
        assertTrue("Nur $sichtbar von ${bank.size} Bankzellen sichtbar", sichtbar >= bank.size * 0.8)
    }

    @Test
    fun `Sterne stehen nicht vor den Bergen`() {
        val maske = PlayWorld.skyMask(PlayScene.Place.MOUNTAINS, width, floorY)
        assertTrue(maske.isNotEmpty())
        val nacht = build(PlayScene.Place.MOUNTAINS, dayPhase = PlayAmbientActivity.DayPhase.NIGHT)
        val sterne = nacht.filter { it.isLight && it.y < floorY - 2 }
        for (stern in sterne) assertFalse("Stern vor dem Berg bei ${stern.x to stern.y}", (stern.x to stern.y) in maske)
    }

    @Test
    fun `auf flachen Bildern fliegt nichts vor den Fassaden`() {
        // Ohne Himmel ueber den Haeusern blieb ein Luftschiff vor ihnen haengen (siehe FacadeTest).
        // Was nicht ueber allem fliegen kann, fliegt gar nicht.
        for (phase in 0 until 2_000 step 3) {
            val cells = PlayWorld.ambient(
                PlayScene.Place.CITY, phase, 46, 22, PlayAmbientActivity.DayPhase.NIGHT, emptyList()
            )
            assertTrue("Phase $phase: $cells", cells.isEmpty())
        }
    }
}
