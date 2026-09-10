package com.notime.glyphsim.living

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LivingAgentEngineTest {

    @Test
    fun `ein hungriges mittelloses Wesen arbeitet kauft ein kehrt heim und isst`() {
        var state = agent(
            id = "puffling",
            needs = pressures(hunger = 95, energy = 20, funNeed = 50)
        )
        var world = world(coins = 0, food = 0)
        val actions = mutableListOf<LivingAction>()
        val events = mutableListOf<LivingEventKind>()

        repeat(5) { index ->
            val next = LivingAgentEngine.advance(state, world, nowMillis = 1_000L + index)
            state = next.state
            world = next.world
            state.currentAction?.let(actions::add)
            events += next.events.map { it.kind }
        }

        assertEquals(
            listOf(
                LivingAction.INSPECT_FOOD,
                LivingAction.WORK,
                LivingAction.BUY_FOOD,
                LivingAction.RETURN_HOME,
                LivingAction.EAT
            ),
            actions
        )
        assertEquals(0, world.coins)
        assertEquals(2, world.foodAtHome)
        assertEquals(LivingLocation.HOME, world.location)
        assertEquals(10, state.pressure(Need.HUNGER))
        assertTrue(LivingEventKind.FOOD_SHORTAGE in events)
        assertTrue(LivingEventKind.WORK_COMPLETED in events)
        assertTrue(LivingEventKind.FOOD_BOUGHT in events)
        assertTrue(LivingEventKind.ATE in events)
        assertEquals(null, state.currentGoal)
        assertEquals(null, state.plan)
    }

    @Test
    fun `eine geaenderte Welt verwirft den Plan und leitet Arbeit neu ab`() {
        var state = agent("starlet", pressures(hunger = 90))
        var world = world(coins = 2, food = 0)

        val inspected = LivingAgentEngine.advance(state, world, 1_000)
        state = inspected.state
        world = inspected.world
        assertEquals(LivingAction.BUY_FOOD, state.plan?.steps?.first())

        // Jemand anderes verbraucht das Geld zwischen zwei Schritten. Die Revision ist der
        // beobachtbare Weltwechsel, nicht ein Sonderfall fuer diese Geschichte.
        world = world.copy(coins = 0, revision = world.revision + 1)
        val replanned = LivingAgentEngine.advance(state, world, 2_000)

        assertTrue(replanned.events.any { it.kind == LivingEventKind.PLAN_REBUILT })
        assertEquals(LivingAction.WORK, replanned.state.currentAction)
        assertEquals(LivingAction.BUY_FOOD, replanned.state.plan?.steps?.first())
        assertEquals(2, replanned.world.coins)
    }

    @Test
    fun `ein blockierter Schritt bleibt fuer eine Anzeige erklaerbar`() {
        val state = agent("gloop", pressures(hunger = 100))
        val world = world(
            coins = 0,
            food = 0,
            available = LivingAction.entries.toSet() - LivingAction.WORK
        )

        val inspected = LivingAgentEngine.advance(state, world, 1_000)
        val blocked = LivingAgentEngine.advance(inspected.state, inspected.world, 2_000)
        val explanation = LivingAgentEngine.explain(blocked.state, blocked.world)

        assertEquals(LivingGoal.GET_FOOD, explanation.wants)
        assertEquals(LivingAction.WORK, explanation.plan.first())
        assertEquals(ObstacleKind.ACTION_UNAVAILABLE, explanation.obstacle?.kind)
        assertEquals(LivingEventKind.PLAN_BLOCKED, explanation.recentEvent?.kind)
        assertTrue(explanation.influentialMemories.isNotEmpty())
    }

    @Test
    fun `utility zeigt Bedarf Persoenlichkeit Langzeitziel Erinnerung Kosten Zeit und Risiko`() {
        val memory = Episode(
            id = "music-was-good",
            atMillis = 10,
            kind = EpisodeKind.SUCCESS,
            goal = LivingGoal.HAVE_FUN,
            action = LivingAction.ENJOY_MUSIC,
            valence = 1
        )
        val personality = PersonalityProfile(
            needWeights = mapOf(Need.FUN to 120),
            goalBiases = mapOf(LivingGoal.HAVE_FUN to 7),
            riskAversion = 10
        )
        val state = agent("hootlet", pressures(funNeed = 60)).copy(
            personality = personality,
            longTermGoals = mapOf(LivingGoal.HAVE_FUN to 4),
            learnedGoalPreferences = mapOf(LivingGoal.HAVE_FUN to 2),
            memories = listOf(memory)
        )

        val decision = LivingAgentEngine.decide(state, world(coins = 4, food = 2))
        val funScore = decision.allScores.first { it.goal == LivingGoal.HAVE_FUN }
        val foodScore = decision.allScores.first { it.goal == LivingGoal.GET_FOOD }

        assertEquals(LivingGoal.HAVE_FUN, decision.goal)
        assertEquals(72, funScore.needSatisfaction)
        assertEquals(7, funScore.personalityPreference)
        assertEquals(2, funScore.learnedPreference)
        assertEquals(4, funScore.longTermValue)
        assertEquals(3, funScore.memoryValue)
        assertTrue(foodScore.time > 0)
        assertTrue(foodScore.cost >= 0)
        assertTrue(foodScore.risk >= 0)
        assertEquals(listOf(memory), decision.influentialMemories)
    }

    @Test
    fun `eine Spielanfrage wird wegen echter Erschoepfung abgelehnt`() {
        val inviter = agent("starlet", pressures(social = 95, energy = 10))
        val inviteWorld = world(coins = 2, food = 2, nearby = setOf("wyrmling"))
        val invitation = LivingAgentEngine.advance(inviter, inviteWorld, 1_000)
        val request = requireNotNull(invitation.message)
        assertEquals(setOf(SymbolicIntent.PLAY, SymbolicIntent.QUESTION), request.intents)

        val tired = agent("wyrmling", pressures(social = 90, energy = 90))
        val response = LivingAgentEngine.respond(tired, world(2, 2), request, 2_000)
        val inviterAfter = LivingAgentEngine.observeResponse(invitation.state, response.message)

        assertEquals(setOf(SymbolicIntent.TIRED, SymbolicIntent.NO), response.message.intents)
        assertEquals(-2, response.state.relationships.getValue("starlet").affinity)
        assertEquals(-2, inviterAfter.relationships.getValue("wyrmling").affinity)
        assertEquals(EpisodeKind.SOCIAL_REJECTED, response.state.memories.last().kind)
    }

    @Test
    fun `dieselbe Anfrage wird bei sozialem Bedarf und Energie angenommen`() {
        val request = SymbolicMessage(
            senderId = "starlet",
            recipientId = "wyrmling",
            intents = setOf(SymbolicIntent.PLAY, SymbolicIntent.QUESTION),
            atMillis = 1_000
        )
        val ready = agent("wyrmling", pressures(social = 80, energy = 10))
        val response = LivingAgentEngine.respond(ready, world(2, 2), request, 2_000)

        assertEquals(setOf(SymbolicIntent.PLAY, SymbolicIntent.YES), response.message.intents)
        assertEquals(4, response.state.relationships.getValue("starlet").affinity)
        assertEquals(EpisodeKind.SOCIAL_ACCEPTED, response.state.memories.last().kind)
    }

    @Test
    fun `aehnliche Wesen entwickeln durch verschiedene Moeglichkeiten andere Historien`() {
        val startNeeds = pressures(funNeed = 70, development = 65, curiosity = 65)
        var musician = agent("fennec", startNeeds)
        var learner = agent("hootlet", startNeeds)
        var musicWorld = world(2, 2)
        var studyWorld = world(
            2,
            2,
            available = LivingAction.entries.toSet() - LivingAction.ENJOY_MUSIC
        )

        val musicDay = LivingAgentEngine.advance(musician, musicWorld, 1_000)
        musician = musicDay.state
        musicWorld = musicDay.world
        val studyDay = LivingAgentEngine.advance(learner, studyWorld, 1_000)
        learner = studyDay.state
        studyWorld = studyDay.world

        assertEquals(LivingAction.ENJOY_MUSIC, musician.currentAction)
        assertEquals(LivingAction.STUDY, learner.currentAction)
        assertNotEquals(musician.memories.map { it.action }, learner.memories.map { it.action })
        assertTrue((musician.learnedGoalPreferences[LivingGoal.HAVE_FUN] ?: 0) > 0)
        assertTrue((learner.learnedGoalPreferences[LivingGoal.DEVELOP] ?: 0) > 0)

        musician = LivingAgentEngine.elapse(musician, 24)
        learner = LivingAgentEngine.elapse(learner, 24)
        assertFalse(musician.needs == learner.needs)
        assertTrue(musicWorld.foodAtHome > 0)
        assertTrue(studyWorld.foodAtHome > 0)
    }

    private fun agent(id: String, needs: Map<Need, Int>): AgentState = AgentState(
        id = id,
        needs = Need.entries.associateWith { needs[it] ?: 0 }
    )

    private fun pressures(
        hunger: Int = 10,
        energy: Int = 10,
        social: Int = 10,
        funNeed: Int = 10,
        comfort: Int = 10,
        curiosity: Int = 10,
        development: Int = 10
    ): Map<Need, Int> = mapOf(
        Need.HUNGER to hunger,
        Need.ENERGY to energy,
        Need.SOCIAL to social,
        Need.FUN to funNeed,
        Need.COMFORT to comfort,
        Need.CURIOSITY to curiosity,
        Need.DEVELOPMENT to development
    )

    private fun world(
        coins: Int,
        food: Int,
        available: Set<LivingAction> = LivingAction.entries.toSet(),
        nearby: Set<String> = emptySet()
    ) = LivingWorldState(
        coins = coins,
        foodAtHome = food,
        location = LivingLocation.HOME,
        availableActions = available,
        nearbyAgentIds = nearby
    )
}
