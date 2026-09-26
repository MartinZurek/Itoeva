package com.notime.glyphsim.decision

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.LivingSimulation
import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.matrix.LivingRuntimeAdapter
import com.notime.glyphsim.matrix.PlayAmbientActivity
import com.notime.glyphsim.matrix.PlayGroupGame
import com.notime.glyphsim.matrix.PlayRoutines
import com.notime.glyphsim.matrix.PlayScene
import com.notime.glyphsim.matrix.RoutineStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Die Gueltigkeitsschicht vor der Policy: Was hier nicht Kandidat ist, kann kein Modell waehlen.
 * Deshalb pruefen diese Tests die Regeln, die nie verhandelbar sind - und dass die bisherige Wahl
 * als Rueckfall exakt erhalten bleibt.
 */
class DecisionCandidatesTest {

    private val lagen by lazy { DecisionTestSupport.sweep(days = 6, seeds = 1) }

    /** Medizin ist nie autonom - weder als Thema noch als Kernhandlung. */
    @Test
    fun `nie Medizin`() {
        for ((_, kandidaten) in lagen) {
            for (k in kandidaten) {
                assertTrue(k.key, k.topic != AnimationType.MEDICINE)
                assertTrue(k.key, k.coreAction != com.notime.glyphsim.living.ActionKind.TEND_SELF)
            }
        }
    }

    /** Das Bett gibt es nur nachts, und nachts nie ein Gruppenspiel. */
    @Test
    fun `Schlafen nur nachts, Gruppenspiel nur tagsueber`() {
        for ((state, kandidaten) in lagen) {
            val nacht = state.phase == PlayAmbientActivity.DayPhase.NIGHT
            for (k in kandidaten) {
                if (k.visibleRoutine.steps.any { it is RoutineStep.SleepUntilMorning }) assertTrue(k.key, nacht)
                if (k.groupGame != null) assertFalse(k.key, nacht)
            }
        }
    }

    /** Die bisherige Wahl ist eine Verteilung - ueber alle Kandidaten eines Moments ergibt sie 1. */
    @Test
    fun `bisherige Wahrscheinlichkeiten summieren sich zu eins`() {
        for ((_, kandidaten) in lagen) {
            if (kandidaten.isEmpty()) continue
            assertEquals(1.0, kandidaten.sumOf { it.baselineProbability }, 1e-9)
        }
    }

    /**
     * **Der Rueckfall ist die alte Kette, nicht eine Nachbildung.** Themenwurf, Kernschritt,
     * Ablaufwurf und Gruppenspiel-Vorrang - so, wie DockScreen sie bisher nacheinander ausfuehrte
     * - werden tausendfach gewuerfelt und mit den Wahrscheinlichkeiten der Kandidaten verglichen.
     */
    @Test
    fun `ExistingUtilityPolicy trifft die bisherige Kette`() {
        val random = Random(4)
        val proben = lagen.filter { it.second.size >= 4 }.shuffled(Random(1)).take(8)
        for ((state, kandidaten) in proben) {
            val zaehler = HashMap<String, Int>()
            val n = 10_000
            repeat(n) {
                val thema = state.signals.draw(state.phase, random)
                val prepared = LivingRuntimeAdapter.prepare(
                    state.agent, state.world, state.currentPlace, thema,
                    recentSpecials = state.recentSpecials, random = random
                )
                val routine = prepared.routine ?: return@repeat
                val topic = prepared.topic!!
                val ort = routine.steps.filterIsInstance<RoutineStep.GoToPlace>().firstOrNull()?.place
                    ?: PlayScene.forTopic(topic)
                val gruppe = if (PlayGroupGame.shouldPlay(
                        topic == AnimationType.MOVE, ort, state.othersAt(ort).isNotEmpty(),
                        state.phase == PlayAmbientActivity.DayPhase.NIGHT
                    )
                ) PlayGroupGame.kindFor(ort, PlayRoutines.specialOf(routine), random.nextInt(1_000)) else null
                val key = if (gruppe != null) "group:${gruppe.name}@${ort.name}" else DecisionCandidates.keyOf(topic, routine)
                zaehler[key] = (zaehler[key] ?: 0) + 1
            }
            val erwartet = kandidaten.groupBy { it.key }.mapValues { e -> e.value.sumOf { it.baselineProbability } }
            val tv = (erwartet.keys + zaehler.keys).sumOf { k ->
                kotlin.math.abs((erwartet[k] ?: 0.0) - (zaehler[k] ?: 0).toDouble() / n)
            } / 2
            assertTrue("TV $tv at ${state.phase} ${state.currentPlace} goal ${state.goal}", tv < 0.03)
        }
    }

    /** Draengt der Hunger, gibt es nur Essen - ein Modell kann ihn nicht wegwaehlen. */
    @Test
    fun `dringender Hunger laesst nur Essen zu`() {
        val state = DecisionTestSupport.state(needs = mapOf(NeedKind.HUNGER to 0.9, NeedKind.FUN to 0.8))
        assertEquals(listOf(GoalKind.GET_FOOD), DecisionCandidates.admissibleGoals(state))
        val kandidaten = DecisionCandidates.generate(state)
        assertTrue(kandidaten.isNotEmpty())
        assertTrue(kandidaten.all { it.goal == GoalKind.GET_FOOD })
    }

    /** Ein laufender Plan wird nicht verlassen. */
    @Test
    fun `laufender Plan bleibt`() {
        val start = DecisionTestSupport.state(needs = mapOf(NeedKind.HUNGER to 0.6), portions = 0, coins = 4)
        val schritt = LivingSimulation.step(start.agent, start.world)
        val mitten = start.copy(agent = schritt.agent, world = schritt.world)
        if (LivingSimulation.keepsGoal(mitten.agent)) {
            assertEquals(listOf(mitten.agent.goal), DecisionCandidates.admissibleGoals(mitten))
        }
    }

    /** Knapp unterlegene Ziele sind waehlbar, weit unterlegene nicht. */
    @Test
    fun `nur knapp unterlegene Ziele sind waehlbar`() {
        for ((state, kandidaten) in lagen) {
            val beste = state.goal ?: continue
            val summe = state.goalRanking.first { it.goal == beste }.total
            for (k in kandidaten) {
                if (k.goal == beste) continue
                val g = state.goalRanking.first { it.goal == k.goal }
                assertTrue("${k.goal} ${g.total} vs $beste $summe", g.total >= summe - DecisionCandidates.GOAL_MARGIN - 1e-9)
                assertTrue(g.needPressure >= com.notime.glyphsim.living.UtilitySelector.MIN_PRESSURE || g.externalInfluence > 0)
            }
        }
    }

    /** Wo Freunde spielen koennen, gibt es das Spiel statt des Einzelsports - der Vorrang bleibt. */
    @Test
    fun `Gruppenspiel hat Vorrang vor Einzelsport`() {
        val state = DecisionTestSupport.state(
            needs = mapOf(NeedKind.FUN to 0.8),
            presence = mapOf(PlayScene.Place.PARK to setOf("resident:park:puffling"))
        )
        val kandidaten = DecisionCandidates.generate(state)
        val spiele = kandidaten.filter { it.groupGame != null }
        assertTrue("keine Gruppenspiele", spiele.isNotEmpty())
        assertTrue(spiele.all { "resident:park:puffling" in it.partners })
        val solo = kandidaten.filter {
            it.topic == AnimationType.MOVE && it.groupGame == null &&
                (it.routine.steps.firstNotNullOfOrNull { s -> (s as? RoutineStep.GoToPlace)?.place } ?: PlayScene.Place.PARK) == PlayScene.Place.PARK
        }
        assertTrue("Einzelsport im Park trotz Mitspieler: ${solo.map { it.key }}", solo.isEmpty())
    }

    /** Muss die Figur draussen bleiben, sind Freizeitkandidaten nur draussen. */
    @Test
    fun `Mindestdauer draussen bleibt eine Regel`() {
        val state = DecisionTestSupport.state(
            minuteOfDay = 16 * 60,
            place = PlayScene.Place.PARK,
            needs = mapOf(NeedKind.FUN to 0.8),
            signals = TopicSignals(holdOutdoors = true)
        )
        val themen = state.topicWeights.keys
        assertTrue(themen.all { PlayScene.isOutdoors(PlayScene.forTopic(it)) })
    }

    /** Ein Impuls legt das Thema fest - die Policy waehlt nur noch die Ausformung. */
    @Test
    fun `Impuls legt das Thema fest`() {
        val state = DecisionTestSupport.state(needs = mapOf(NeedKind.FUN to 0.8)).copy(impulseTopic = AnimationType.CREATIVITY)
        val kandidaten = DecisionCandidates.generate(state)
        assertTrue(kandidaten.isNotEmpty())
        assertTrue(kandidaten.filter { it.goal == state.goal }.all { it.interestTopic == AnimationType.CREATIVITY })
    }
}
