package com.notime.glyphsim.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Das Gruppenspiel (siehe [PlayGroupGame]): Alle, die da sind, spielen mit - und man sieht, was
 * gespielt wird.
 */
class PlayGroupGameTest {

    private val width = 64
    private val floorY = 26
    private val top = floorY - AvatarGeometry.HEIGHT

    private val vier = listOf(
        PlayGroupGame.Player("host", 2, top, 16),
        PlayGroupGame.Player("a", 18, top, 16),
        PlayGroupGame.Player("b", 34, top, 16),
        PlayGroupGame.Player("klein", 52, floorY - 10, 8)
    )

    private val ticks = (0 until (PlayGroupGame.DURATION_MS / 200L).toInt())

    /** Der Ball ist in jedem Takt zu sehen und bleibt im Bild und ueber dem Boden. */
    @Test
    fun `der Ball ist immer sichtbar und bleibt im Bild`() {
        for (kind in PlayGroupGame.Kind.entries) {
            for (age in ticks) {
                val cells = PlayGroupGame.momentAt(kind, vier, age, width, floorY).ballCells
                    .filter { it.brightness > PlayInk.VOID }
                assertTrue("$kind Takt $age: kein Ball", cells.isNotEmpty())
                assertTrue("$kind Takt $age: ausserhalb", cells.all { it.x in 0 until width && it.y in 0 until floorY })
            }
        }
    }

    /**
     * **Alle spielen mit.** Der gemeldete Fehler war genau das Gegenteil: vier Figuren, und nur
     * eine tat etwas. Jeder Mitspieler wirft im Laufe eines Spiels mindestens einmal.
     */
    @Test
    fun `jeder Mitspieler kommt ans Werfen`() {
        for (kind in PlayGroupGame.Kind.entries) {
            val werfer = ticks.flatMap { age ->
                PlayGroupGame.momentAt(kind, vier, age, width, floorY).poses
                    .filterValues { it == PlayGroupGame.Pose.THROW }.keys
            }.toSet()
            assertEquals("$kind", vier.map { it.id }.toSet(), werfer)
        }
    }

    /** Jeder hat in jedem Takt eine Haltung - niemand faellt aus dem Spiel. */
    @Test
    fun `jeder Mitspieler hat in jedem Takt eine Haltung`() {
        for (kind in PlayGroupGame.Kind.entries) {
            for (age in ticks) {
                val moment = PlayGroupGame.momentAt(kind, vier, age, width, floorY)
                assertEquals("$kind Takt $age", vier.map { it.id }.toSet(), moment.poses.keys)
                assertEquals("$kind Takt $age", vier.map { it.id }.toSet(), moment.facesLeft.keys)
            }
        }
    }

    /** Bei drei und mehr geht der Ball nie direkt zurueck - sonst waere es ein Hin und Her zu zweit. */
    @Test
    fun `bei drei und mehr kein direktes Zurueckspielen`() {
        for (count in 3..5) {
            for (round in 0 until 40) {
                val a = PlayGroupGame.holderAt(round, count)
                val b = PlayGroupGame.holderAt(round + 1, count)
                val c = PlayGroupGame.holderAt(round + 2, count)
                assertNotEquals("$count Spieler, Runde $round", a, b)
                assertNotEquals("$count Spieler, Runde $round: zurueck an den Werfer", a, c)
            }
        }
    }

    /** Der Ball fliegt wirklich: Er ist nicht zwei Takte hintereinander an derselben Stelle, solange er unterwegs ist. */
    @Test
    fun `der Ball bewegt sich`() {
        for (kind in PlayGroupGame.Kind.entries) {
            val positionen = ticks.map { age ->
                PlayGroupGame.momentAt(kind, vier, age, width, floorY).ballCells
                    .filter { it.brightness > PlayInk.VOID }
                    .map { it.x to it.y }.toSet()
            }
            val verschiedene = positionen.zipWithNext().count { (a, b) -> a != b }
            assertTrue("$kind: der Ball steht zu oft still ($verschiedene Wechsel)", verschiedene > ticks.count() / 2)
        }
    }

    /** Beim Korbwurf wird bejubelt - aber nicht jeder Wurf trifft. */
    @Test
    fun `beim Korbwurf gibt es Jubel und Fehlwuerfe`() {
        val jubel = ticks.count { age ->
            PlayGroupGame.momentAt(PlayGroupGame.Kind.HOOPS, vier, age, width, floorY).poses
                .values.all { it == PlayGroupGame.Pose.CHEER }
        }
        assertTrue("kein Jubel", jubel > 0)
        val runden = ticks.count() / 22
        assertTrue("jeder Wurf wird bejubelt", jubel < runden * 5)
    }

    /** Allein wird der Ball hochgeworfen und gefangen - kein leeres Bild, wenn niemand da ist. */
    @Test
    fun `allein wirft man sich den Ball selbst zu`() {
        val allein = listOf(vier.first())
        val hoehen = (0 until 30).map { age ->
            PlayGroupGame.momentAt(PlayGroupGame.Kind.CATCH, allein, age, width, floorY).ballCells
                .filter { it.brightness > PlayInk.VOID }.minOf { it.y }
        }
        assertTrue("der Ball geht nicht hoch", hoehen.max() - hoehen.min() >= 5)
    }

    /** Die Haltungen unterscheiden sich sichtbar - sonst stuende wieder jeder gleich da. */
    @Test
    fun `die Haltungen sind verschieden`() {
        for (species in AvatarSpecies.entries) {
            val ready = AvatarAnimations.gamePose(species, PlayGroupGame.Pose.READY, 0)
            val throwing = AvatarAnimations.gamePose(species, PlayGroupGame.Pose.THROW, 0)
            val catching = AvatarAnimations.gamePose(species, PlayGroupGame.Pose.CATCH, 0)
            assertFalse("$species wirft wie er steht", ready.contentEquals(throwing))
            assertFalse("$species faengt wie er steht", ready.contentEquals(catching))
            val cheer = (0..3).map { AvatarAnimations.gamePose(species, PlayGroupGame.Pose.CHEER, it) }
            assertTrue("$species jubelt ohne Bewegung", cheer.zipWithNext().any { (a, b) -> !a.contentEquals(b) })
        }
    }

    /** Park und Wiese bekommen Ball, Frisbee oder Fussball; der Sportplatz Korb oder Tor. */
    @Test
    fun `das Spiel passt zum Ort`() {
        val park = (0..11).map { PlayGroupGame.kindFor(PlayScene.Place.PARK, null, it) }.toSet()
        assertEquals(setOf(PlayGroupGame.Kind.CATCH, PlayGroupGame.Kind.FRISBEE, PlayGroupGame.Kind.KICKABOUT), park)
        val platz = (0..11).map { PlayGroupGame.kindFor(PlayScene.Place.SPORT, null, it) }.toSet()
        assertEquals(setOf(PlayGroupGame.Kind.HOOPS, PlayGroupGame.Kind.KICKABOUT), platz)
        assertEquals(
            PlayGroupGame.Kind.HOOPS,
            PlayGroupGame.kindFor(PlayScene.Place.PARK, PlayRoutines.SpecialActivity.BASKETBALL, 1)
        )
    }
}
