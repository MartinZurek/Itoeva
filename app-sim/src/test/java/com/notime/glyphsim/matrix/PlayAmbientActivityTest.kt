package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

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

    /**
     * **Ohne offene Gewohnheit ist SLEEP nachts das EINZIGE Thema - durchgehend, nicht bloss
     * ueberwiegend.** Gemeldet als "er sollte nachts durchgehend schlafen": Vorher stand REST/
     * MINDFULNESS mit im Topf (Gewicht 5 von 9, gut 55%) - SLEEP war das haeufigste Thema, aber
     * eben nicht das einzige. `weightsFor(NIGHT)` hat seither nur noch SLEEP als Grundgewicht.
     */
    @Test
    fun `nachts ist SLEEP ohne offene Gewohnheit das einzige Thema`() {
        for (seed in listOf(1, 42, 12345, 987654321)) {
            val random = Random(seed)
            val results = (1..500)
                .map { PlayAmbientActivity.nextTopic(PlayAmbientActivity.DayPhase.NIGHT, random = random) }
                .toSet()
            assertEquals("Startwert $seed: nachts kommt etwas anderes als SLEEP vor", setOf(AnimationType.SLEEP), results)
        }
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
    fun `reines Umherlaufen bleibt eine seltene Zwischenregung`() {
        val random = Random(23)
        val wander = (1..1_000).count {
            PlayAmbientActivity.nextAction(random) == PlayAmbientActivity.Action.WANDER
        }
        assertTrue("WANDER kam $wander von 1000 Mal vor", wander < 100)
    }

    @Test
    fun `Pausen bleiben im vorgesehenen Rahmen`() {
        repeat(200) {
            val pause = PlayAmbientActivity.nextPauseMillis(PlayAmbientActivity.DayPhase.MIDDAY)
            assertTrue("Pause $pause zu kurz", pause >= 18_000L)
            assertTrue("Pause $pause zu lang", pause <= 36_000L)
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

    // ---- Stundenplan als viertes Signal ----

    /**
     * Der Plan muss jede Stunde beantworten und darf MEDICINE nie nennen - er ist sonst ein
     * stiller Umweg, auf dem eine Medikamenten-Handlung doch in den autonomen Ablauf geriete.
     */
    @Test
    fun `der Stundenplan deckt jede Stunde ab und nennt nie MEDICINE`() {
        for (hour in 0..23) {
            assertTrue(
                "MEDICINE als Tagesplan um $hour Uhr",
                PlayAmbientActivity.plannedTopicFor(hour) != AnimationType.MEDICINE
            )
        }
        // Eckpunkte des dokumentierten Ablaufs - Fruehstueck, Arbeitsbeginn, Abendlektuere, Nacht.
        assertEquals(AnimationType.DRINK, PlayAmbientActivity.plannedTopicFor(7))
        assertEquals(AnimationType.WORK, PlayAmbientActivity.plannedTopicFor(10))
        assertEquals(AnimationType.BOOK, PlayAmbientActivity.plannedTopicFor(20))
        assertEquals(AnimationType.SLEEP, PlayAmbientActivity.plannedTopicFor(3))
    }

    /**
     * **Der eigentliche Zweck: Der Tag wird lesbar.** Um sieben sieht der Plan das Fruehstueck
     * vor - danach soll DRINK deutlich haeufiger vorkommen als ohne Plan, aber eben nicht immer.
     */
    @Test
    fun `ein geplantes Thema kommt deutlich haeufiger vor als ohne Plan`() {
        val ohne = zaehle(AnimationType.DRINK, plan = null, seed = 1)
        val mit = zaehle(AnimationType.DRINK, plan = AnimationType.DRINK, seed = 1)
        // 3 von 14 (21%) gegenueber 7 von 18 (39%).
        assertTrue("ohne Plan $ohne, mit Plan $mit", mit > ohne + 400)
        assertTrue("mit Plan $mit von 4000 - der Plan darf keine Gewissheit sein", mit < 2400)
    }

    /**
     * **Der Arbeitsbeginn um zehn.** Die Phase MORNING kennt gar kein WORK; ohne den Plan ginge
     * die Figur vormittags nie zur Arbeit. Der Plan darf ein Thema deshalb auch einfuehren - im
     * Unterschied zu Neigung und Verweilen, die nur gewichten, was ohnehin zur Phase passt.
     */
    @Test
    fun `der Plan darf ein Thema einfuehren das die Phase nicht kennt`() {
        assertTrue(
            "WORK kommt morgens ohne Plan gar nicht vor",
            zaehle(AnimationType.WORK, plan = null, seed = 2) == 0
        )
        val mit = zaehle(AnimationType.WORK, plan = AnimationType.WORK, seed = 2)
        // 4 von 18 = gut 22%.
        assertTrue("WORK kam $mit von 4000 mal", mit in 700..1100)
    }

    /**
     * Zwei geschuetzte Zusagen, die der Plan nicht aushebeln darf: MEDICINE bleibt aussen vor,
     * auch wenn ein Aufrufer es hineinreicht, und nachts bleibt SLEEP das einzige Thema.
     */
    @Test
    fun `der Plan hebelt weder den MEDICINE-Ausschluss noch die Nachtruhe aus`() {
        val random = Random(3)
        repeat(2000) {
            assertTrue(
                "MEDICINE trotz ausdruecklichem Plan",
                PlayAmbientActivity.nextTopic(
                    PlayAmbientActivity.DayPhase.MIDDAY,
                    plannedTopic = AnimationType.MEDICINE,
                    random = random
                ) != AnimationType.MEDICINE
            )
            assertEquals(
                "nachts etwas anderes als Schlaf",
                AnimationType.SLEEP,
                PlayAmbientActivity.nextTopic(
                    PlayAmbientActivity.DayPhase.NIGHT,
                    plannedTopic = PlayAmbientActivity.plannedTopicFor(3),
                    random = random
                )
            )
        }
    }

    /** Wie oft [topic] morgens in 4000 Ziehungen faellt - mit oder ohne Tagesplan. */
    private fun zaehle(topic: AnimationType, plan: AnimationType?, seed: Int): Int {
        val random = Random(seed)
        var treffer = 0
        repeat(4000) {
            val gezogen = PlayAmbientActivity.nextTopic(
                PlayAmbientActivity.DayPhase.MORNING,
                plannedTopic = plan,
                random = random
            )
            if (gezogen == topic) treffer++
        }
        return treffer
    }
}
