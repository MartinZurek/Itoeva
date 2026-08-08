package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests fuer die autonomen Zwischen-Regungen des Play-Modus ([PlayAmbientActivity]) - reine
 * Logik, laeuft deshalb ohne Geraet.
 *
 * Der wichtigste Teil ist nicht "kommt ein plausibles Thema raus" (das waere kaum pruefbar), es
 * ist die harte Garantie, dass MEDICINE nie autonom ausgeloest wird - eine autonome Regung darf
 * nie wie eine echte Medikamenten-Erinnerung wirken (siehe Klassendoku).
 */
class PlayAmbientActivityTest {

    // ---- Tagesphase ----

    @Test
    fun `Uhrzeit-Grenzen landen in der erwarteten Phase`() {
        assertEquals(PlayAmbientActivity.DayPhase.NIGHT, PlayAmbientActivity.currentDayPhase(LocalTime.of(5, 59)))
        assertEquals(PlayAmbientActivity.DayPhase.MORNING, PlayAmbientActivity.currentDayPhase(LocalTime.of(6, 0)))
        assertEquals(PlayAmbientActivity.DayPhase.MORNING, PlayAmbientActivity.currentDayPhase(LocalTime.of(10, 59)))
        assertEquals(PlayAmbientActivity.DayPhase.MIDDAY, PlayAmbientActivity.currentDayPhase(LocalTime.of(11, 0)))
        assertEquals(PlayAmbientActivity.DayPhase.MIDDAY, PlayAmbientActivity.currentDayPhase(LocalTime.of(17, 59)))
        assertEquals(PlayAmbientActivity.DayPhase.EVENING, PlayAmbientActivity.currentDayPhase(LocalTime.of(18, 0)))
        assertEquals(PlayAmbientActivity.DayPhase.EVENING, PlayAmbientActivity.currentDayPhase(LocalTime.of(22, 59)))
        assertEquals(PlayAmbientActivity.DayPhase.NIGHT, PlayAmbientActivity.currentDayPhase(LocalTime.of(23, 0)))
    }

    // ---- Themenwahl ----

    @Test
    fun `MEDICINE kommt in keiner Phase und mit keinem Boost vor`() {
        for (phase in PlayAmbientActivity.DayPhase.entries) {
            repeat(500) {
                assertTrue(
                    "MEDICINE bei $phase ohne Boost",
                    PlayAmbientActivity.nextTopic(phase) != AnimationType.MEDICINE
                )
                assertTrue(
                    "MEDICINE bei $phase trotz Boost auf alle Themen",
                    PlayAmbientActivity.nextTopic(phase, AnimationType.entries.toSet()) != AnimationType.MEDICINE
                )
            }
        }
    }

    @Test
    fun `nachts ist SLEEP das mit Abstand haeufigste Thema`() {
        val counts = (1..500)
            .map { PlayAmbientActivity.nextTopic(PlayAmbientActivity.DayPhase.NIGHT) }
            .groupingBy { it }
            .eachCount()
        val sleepCount = counts[AnimationType.SLEEP] ?: 0
        assertTrue("SLEEP kam nachts nur $sleepCount von 500 Mal vor", sleepCount > 250)
    }

    @Test
    fun `ein geboostetes Thema ausserhalb der Phase taucht trotzdem gelegentlich auf`() {
        // LOVE hat nachts kein Grundgewicht (siehe weightsFor) - nur ueber den Boost erreichbar.
        val results = (1..500)
            .map { PlayAmbientActivity.nextTopic(PlayAmbientActivity.DayPhase.NIGHT, setOf(AnimationType.LOVE)) }
            .toSet()
        assertTrue("LOVE tauchte trotz Boost nie auf", results.contains(AnimationType.LOVE))
        assertTrue("SLEEP verschwand durch den Boost auf ein anderes Thema komplett", results.contains(AnimationType.SLEEP))
    }

    // ---- Aktionswahl ----

    @Test
    fun `PERFORM ist die haeufigste Aktion`() {
        val counts = (1..1000)
            .map { PlayAmbientActivity.nextAction() }
            .groupingBy { it }
            .eachCount()
        val performCount = counts[PlayAmbientActivity.Action.PERFORM] ?: 0
        assertTrue("PERFORM kam nur $performCount von 1000 Mal vor", performCount > (counts[PlayAmbientActivity.Action.WANDER] ?: 0))
        assertTrue("PERFORM kam nur $performCount von 1000 Mal vor", performCount > (counts[PlayAmbientActivity.Action.FLOURISH] ?: 0))
    }

    @Test
    fun `Pausen bleiben im vorgesehenen Rahmen`() {
        repeat(200) {
            val pause = PlayAmbientActivity.nextPauseMillis(PlayAmbientActivity.DayPhase.MIDDAY)
            assertTrue("Pause $pause zu kurz", pause >= 6_000L)
            assertTrue("Pause $pause zu lang", pause <= 14_000L)
        }
    }

    @Test
    fun `nachts vergeht mehr Zeit zwischen den Regungen als mittags`() {
        // Ein Wesen, das um drei Uhr nachts denselben Takt haelt wie mittags, hat keinen Tag,
        // sondern eine Schleife.
        val midday = (1..200).minOf { PlayAmbientActivity.nextPauseMillis(PlayAmbientActivity.DayPhase.MIDDAY) }
        val night = (1..200).minOf { PlayAmbientActivity.nextPauseMillis(PlayAmbientActivity.DayPhase.NIGHT) }
        assertTrue("Nachts ist es nicht ruhiger als mittags ($night vs $midday)", night > midday)
    }
}
