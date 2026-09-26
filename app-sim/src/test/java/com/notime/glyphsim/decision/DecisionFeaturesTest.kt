package com.notime.glyphsim.decision

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.matrix.AvatarAnimations
import com.notime.glyphsim.matrix.PlayGroupGame
import com.notime.glyphsim.matrix.PlayRoutine
import com.notime.glyphsim.matrix.PlayRoutines
import com.notime.glyphsim.matrix.PlayScene
import com.notime.glyphsim.matrix.RoutineStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Das Merkmalsschema: feste Form, abgeleitete Bedeutung, keine Ausreisser. */
class DecisionFeaturesTest {

    private fun candidate(
        routine: PlayRoutine,
        topic: AnimationType,
        core: ActionKind,
        group: PlayGroupGame.Kind? = null,
        place: PlayScene.Place = DecisionCandidates.mainPlace(routine, PlayScene.Place.LIVING)
    ) = ActionCandidate(
        key = DecisionCandidates.keyOf(topic, routine),
        family = DecisionCandidates.familyOf(topic, routine, group),
        topic = topic,
        interestTopic = topic,
        routine = routine,
        visibleRoutine = routine,
        groupGame = group,
        place = place,
        coreAction = core,
        goal = DecisionFeatures.goalFor(core),
        goalUtility = 0.2,
        goalGap = 0.0,
        baselineProbability = 0.1
    )

    @Test
    fun `Namen sind eindeutig und die Laenge ist fest`() {
        val namen = DecisionFeatures.NAMES
        assertEquals(namen.size, namen.toSet().size)
        val state = DecisionTestSupport.state()
        val a = DecisionFeatures.encode(state, candidate(PlayRoutines.basketballRoutine(), AnimationType.MOVE, ActionKind.MOVE_BODY))
        val b = DecisionFeatures.encode(state, candidate(PlayRoutines.allFor(AnimationType.BOOK).first(), AnimationType.BOOK, ActionKind.READ))
        assertEquals(DecisionFeatures.COUNT, a.size)
        assertEquals(DecisionFeatures.COUNT, b.size)
        assertEquals(namen, DecisionFeatures.named(state, candidate(PlayRoutine(emptyList()), AnimationType.GENERAL, ActionKind.PURSUE_INTEREST)).keys.toList())
    }

    @Test
    fun `Merkmale werden aus dem Ablauf abgeleitet`() {
        val sport = ActionTraits.of(candidate(PlayRoutines.basketballRoutine(), AnimationType.MOVE, ActionKind.MOVE_BODY))
        assertTrue(sport.sport && sport.physical && sport.outdoor && sport.usesObject)
        assertFalse(sport.social)

        val spiel = PlayGroupGame.routine(PlayGroupGame.Kind.FRISBEE, PlayScene.Place.PARK)
        val gruppe = ActionTraits.of(candidate(spiel, AnimationType.MOVE, ActionKind.MOVE_BODY, PlayGroupGame.Kind.FRISBEE))
        assertTrue(gruppe.group && gruppe.social && gruppe.game)

        val kueche = PlayRoutines.allFor(AnimationType.DRINK).filter { r -> r.steps.none { it is RoutineStep.GoToPlace } }
        val essen = kueche.first { r -> r.steps.any { it is RoutineStep.Take && it.item == com.notime.glyphsim.matrix.PlayEffects.Carried.FOOD } }
        val becher = kueche.first { r -> r.steps.any { it is RoutineStep.Take && it.item == com.notime.glyphsim.matrix.PlayEffects.Carried.CUP } }
        assertTrue(ActionTraits.of(candidate(essen, AnimationType.DRINK, ActionKind.EAT)).eat)
        val trinken = ActionTraits.of(candidate(becher, AnimationType.DRINK, ActionKind.EAT))
        assertTrue(trinken.drink && !trinken.eat)

        val bett = ActionTraits.of(candidate(PlayRoutines.allFor(AnimationType.SLEEP).first(), AnimationType.SLEEP, ActionKind.REST))
        assertTrue(bett.sleep && bett.rest)

        val halle = ActionTraits.of(candidate(PlayRoutines.arcadeRoutine(), AnimationType.GENERAL, ActionKind.PURSUE_INTEREST))
        assertTrue(halle.game && halle.media && !halle.outdoor)
    }

    /**
     * **Eine neue Aktion braucht kein neues Modell.** Tischtennis in der Spielhalle - aus
     * vorhandenen Schritten gebaut, nirgends eingetragen - bekommt denselben Vektor und eine
     * endliche Bewertung aus dem ausgelieferten Modell, und die Auswahl kann es treffen.
     */
    @Test
    fun `neue Aktion wird ohne Modellaenderung bewertet`() {
        val tischtennis = PlayRoutine(
            listOf(
                RoutineStep.GoToPlace(PlayScene.Place.ARCADE),
                RoutineStep.Stroll(0.4f),
                RoutineStep.Stir(AvatarAnimations.Fidget.SHAKE),
                RoutineStep.Act(AnimationType.MOVE),
                RoutineStep.Linger(9_000L)
            )
        )
        val state = DecisionTestSupport.state(needs = mapOf(com.notime.glyphsim.living.NeedKind.FUN to 0.7))
        val neu = candidate(tischtennis, AnimationType.MOVE, ActionKind.MOVE_BODY).copy(baselineProbability = 0.0)
        assertEquals(DecisionFeatures.COUNT, DecisionFeatures.encode(state, neu).size)
        val bekannte = DecisionCandidates.generate(state)
        val alle = bekannte + neu
        val policy = DecisionTestSupport.bundled.policy
        val scores = policy.scoreAll(state, alle)
        assertTrue(scores.all { it.isFinite() })
        val p = DecisionSelector.probabilities(scores, DecisionTestSupport.bundled.temperature)
        assertTrue("neue Aktion unwaehlbar", p.last() > 0.0)
    }

    @Test
    fun `alle Merkmale bleiben in einem kleinen Bereich`() {
        val lagen = DecisionTestSupport.sweep(days = 3, seeds = 1)
        val namen = DecisionFeatures.NAMES
        for ((state, kandidaten) in lagen) {
            for (k in kandidaten) {
                val v = DecisionFeatures.encode(state, k)
                v.forEachIndexed { i, x ->
                    assertTrue("${namen[i]} = $x", x.isFinite() && x >= -1.01f && x <= 2.01f)
                }
            }
        }
    }

    @Test
    fun `Ziel und Kernhandlung passen zusammen`() {
        assertEquals(GoalKind.GET_FOOD, DecisionFeatures.goalFor(ActionKind.EAT))
        assertEquals(GoalKind.EXPLORE, DecisionFeatures.goalFor(ActionKind.EXPLORE))
        for (kind in ActionKind.entries) DecisionFeatures.coreActionOf(kind)
    }

    @Test
    fun `Verlauf uebersteht Speichern und Laden`() {
        val h = DecisionHistory()
            .recorded(DecisionHistory.Entry(10, "MOVE:1", "group:FRISBEE", "MOVE", setOf("resident:a", "resident:b"), true))
            .recorded(DecisionHistory.Entry(40, "BOOK:2", "topic:BOOK", "BOOK"))
        assertEquals(h, DecisionHistory.decode(h.encode()))
        assertEquals(DecisionHistory(), DecisionHistory.decode("kaputt\n\u0000"))
        assertEquals(10L, h.lastWith("group:FRISBEE", setOf("resident:b")))
        assertEquals(10L, h.lastOutdoor())
    }
}
