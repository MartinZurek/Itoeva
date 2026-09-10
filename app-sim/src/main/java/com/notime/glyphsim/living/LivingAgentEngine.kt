package com.notime.glyphsim.living

import kotlin.math.max

/**
 * Deterministischer, darstellungsfreier Kern des ersten Living-Agent-Schnitts.
 *
 * Das Objekt entscheidet und reduziert Zustand; es spielt keine Animation ab und schreibt keine
 * Geschichte. Jede sichtbare Geschichte ist die Folge der unten erzeugten [LivingEvent]s.
 */
object LivingAgentEngine {
    private const val URGENT_PRESSURE = 80
    private const val URGENCY_BONUS = 100
    private const val MAX_MEMORIES = 24

    /** Laesst alle Beduerfnisse gemaess individuellem Profil fortschreiten. */
    fun elapse(state: AgentState, hours: Int): AgentState {
        require(hours >= 0) { "hours must not be negative" }
        val next = Need.entries.associateWith { need ->
            (state.pressure(need) + state.personality.drift(need) * hours).coerceIn(0, 100)
        }
        return state.copy(needs = next)
    }

    /** Waehlt ein Ziel und legt jeden Summanden der Utility-Rechnung offen. */
    fun decide(state: AgentState, world: LivingWorldState): GoalDecision {
        val scores = LivingGoal.entries.map { goal -> utility(goal, state, world) }
        // maxWith behaelt beim Gleichstand nicht verlaesslich die gewuenschte Fachreihenfolge.
        // Explizite Ordinal-Sortierung macht den Tiebreak reproduzierbar und sichtbar.
        val chosen = scores.sortedWith(
            compareByDescending<UtilityScore> { it.total }.thenBy { it.goal.ordinal }
        ).first()
        val memories = state.memories
            .asReversed()
            .filter { it.goal == chosen.goal }
            .take(3)
        return GoalDecision(chosen.goal, chosen, scores, memories)
    }

    /** Baut den kleinsten Plan, der das Ziel unter dem jetzigen Weltzustand erreichen kann. */
    fun plan(goal: LivingGoal, world: LivingWorldState, nowMillis: Long): LivingPlan {
        val steps = when (goal) {
            LivingGoal.GET_FOOD -> buildList {
                if (world.carriedFood > 0) {
                    // Ein Einkauf im Inventar ist bereits die Loesung; erst nach Hause bringen,
                    // statt trotz voller Tasche erneut Geld zu verdienen oder einzukaufen.
                    add(LivingAction.RETURN_HOME)
                    add(LivingAction.INSPECT_FOOD)
                    add(LivingAction.EAT)
                    return@buildList
                }
                if (world.location != LivingLocation.HOME) add(LivingAction.RETURN_HOME)
                add(LivingAction.INSPECT_FOOD)
                if (world.foodAtHome > 0) {
                    add(LivingAction.EAT)
                } else {
                    if (world.coins < world.groceryCost) add(LivingAction.WORK)
                    add(LivingAction.BUY_FOOD)
                    add(LivingAction.RETURN_HOME)
                    add(LivingAction.EAT)
                }
            }
            LivingGoal.REST -> buildList {
                if (world.location != LivingLocation.HOME) add(LivingAction.RETURN_HOME)
                add(LivingAction.REST)
            }
            LivingGoal.CONNECT -> listOf(LivingAction.INVITE_TO_PLAY)
            LivingGoal.HAVE_FUN -> listOf(LivingAction.ENJOY_MUSIC)
            LivingGoal.DEVELOP -> listOf(LivingAction.STUDY)
        }
        return LivingPlan(goal, steps, nowMillis, world.revision)
    }

    /**
     * Waehlt, plant, prueft und fuehrt genau einen Schritt aus. Weltrevisionen und gescheiterte
     * Voraussetzungen erzeugen einen neuen Plan statt einer vorgefertigten Ersatzsequenz.
     */
    fun advance(state: AgentState, world: LivingWorldState, nowMillis: Long): AgentTransition {
        val decision = decide(state, world)
        val urgent = urgentGoal(state)
        var working = state
        val setupEvents = mutableListOf<LivingEvent>()

        val shouldInterrupt = urgent != null && urgent != working.currentGoal
        if (working.plan == null || shouldInterrupt) {
            val goal = urgent ?: decision.goal
            working = working.copy(
                currentGoal = goal,
                plan = plan(goal, world, nowMillis),
                currentAction = null
            )
            setupEvents += event(LivingEventKind.PLAN_CREATED, working, nowMillis, goal = goal)
        } else if (requireNotNull(working.plan).worldRevision != world.revision) {
            val goal = working.currentGoal ?: decision.goal
            working = working.copy(plan = plan(goal, world, nowMillis), currentAction = null)
            setupEvents += event(LivingEventKind.PLAN_REBUILT, working, nowMillis, goal = goal)
        }

        val activePlan = requireNotNull(working.plan)
        val action = activePlan.steps.firstOrNull()
            ?: return AgentTransition(
                state = working.copy(currentGoal = null, plan = null, currentAction = null),
                world = world,
                events = setupEvents
            )
        val obstacle = obstacleFor(action, world)
        if (obstacle != null) {
            val failed = event(
                LivingEventKind.ACTION_FAILED,
                working,
                nowMillis,
                goal = activePlan.goal,
                action = action,
                obstacle = obstacle
            )
            val remembered = remember(working.copy(currentAction = null), failed)
            val rebuilt = plan(activePlan.goal, world, nowMillis)
            val rebuiltObstacle = rebuilt.steps.firstOrNull()?.let { obstacleFor(it, world) }
            val kind = if (rebuiltObstacle == null) LivingEventKind.PLAN_REBUILT else LivingEventKind.PLAN_BLOCKED
            val planEvent = event(kind, remembered, nowMillis, goal = activePlan.goal, obstacle = rebuiltObstacle)
            return AgentTransition(
                state = remembered.copy(plan = rebuilt, lastEvent = planEvent),
                world = world,
                events = setupEvents + failed + planEvent
            )
        }

        return execute(working, world, activePlan, action, nowMillis, setupEvents)
    }

    /** Zustand fuer eine spaetere Stream-/UI-Darstellung, ohne Text zu erfinden. */
    fun explain(state: AgentState, world: LivingWorldState): AgentExplanation {
        // Nach dem letzten Schritt ist das alte Ziel erfuellt. Fuer die lesende Schicht wird dann
        // bereits die naechste Wahl berechnet, ohne sie als Plan in den Zustand zu schreiben.
        val freshDecision = decide(state, world)
        val goal = state.currentGoal ?: freshDecision.goal
        val decision = if (state.currentGoal == null) freshDecision.score else {
            LivingGoal.entries.map { utility(it, state, world) }.first { it.goal == goal }
        }
        val next = state.plan?.steps?.firstOrNull()
        return AgentExplanation(
            doing = state.currentAction,
            wants = goal,
            why = decision,
            plan = state.plan?.steps ?: plan(goal, world, state.lastEvent?.atMillis ?: 0).steps,
            obstacle = next?.let { obstacleFor(it, world) },
            influentialMemories = goal?.let { wanted ->
                state.memories.asReversed().filter { it.goal == wanted }.take(3)
            }.orEmpty(),
            recentEvent = state.lastEvent
        )
    }

    /**
     * Antwort auf eine echte symbolische Einladung. Die Antwort haengt an aktuellem Zustand,
     * Beziehung und Ziel; sie spielt keine vorgefertigte Unterhaltung ab.
     */
    fun respond(
        receiver: AgentState,
        world: LivingWorldState,
        request: SymbolicMessage,
        nowMillis: Long
    ): CommunicationResponse {
        require(request.recipientId == receiver.id) { "message has a different recipient" }
        val sender = request.senderId
        val relationship = receiver.relationships[sender] ?: RelationshipState()
        val tired = receiver.pressure(Need.ENERGY) >= URGENT_PRESSURE
        val hungry = receiver.pressure(Need.HUNGER) >= URGENT_PRESSURE
        val currentConflict = receiver.currentGoal == LivingGoal.GET_FOOD ||
            receiver.currentGoal == LivingGoal.REST
        val acceptance = receiver.pressure(Need.SOCIAL) +
            receiver.personality.bias(LivingGoal.CONNECT) +
            relationship.affinity -
            receiver.pressure(Need.ENERGY) -
            if (currentConflict) 60 else 0
        val accepts = !tired && !hungry && acceptance >= 20 &&
            LivingAction.RESPOND_TO_INVITE in world.availableActions
        val intents = when {
            accepts -> setOf(SymbolicIntent.PLAY, SymbolicIntent.YES)
            tired -> setOf(SymbolicIntent.TIRED, SymbolicIntent.NO)
            hungry -> setOf(SymbolicIntent.FOOD, SymbolicIntent.NO)
            else -> setOf(SymbolicIntent.PLAY, SymbolicIntent.NO)
        }
        val reply = SymbolicMessage(receiver.id, sender, intents, nowMillis)
        val nextRelationship = relationship.copy(
            affinity = (relationship.affinity + if (accepts) 4 else -2).coerceIn(-100, 100),
            interactions = relationship.interactions + 1,
            lastInteractionMillis = nowMillis
        )
        val event = LivingEvent(
            kind = LivingEventKind.MESSAGE_SENT,
            actorId = receiver.id,
            atMillis = nowMillis,
            goal = LivingGoal.CONNECT,
            action = LivingAction.RESPOND_TO_INVITE,
            targetId = sender,
            intents = intents
        )
        val episodeKind = if (accepts) EpisodeKind.SOCIAL_ACCEPTED else EpisodeKind.SOCIAL_REJECTED
        val episode = Episode(
            id = episodeId(event),
            atMillis = nowMillis,
            kind = episodeKind,
            goal = LivingGoal.CONNECT,
            action = LivingAction.RESPOND_TO_INVITE,
            targetId = sender,
            valence = if (accepts) 1 else -1
        )
        val next = learn(
            receiver.copy(
                currentAction = LivingAction.RESPOND_TO_INVITE,
                relationships = receiver.relationships + (sender to nextRelationship),
                memories = appendMemory(receiver.memories, episode),
                lastEvent = event
            ),
            LivingGoal.CONNECT,
            episode.valence
        )
        return CommunicationResponse(next, event, reply)
    }

    /** Die andere Seite lernt aus der Antwort und fuehrt denselben Beziehungsstand fort. */
    fun observeResponse(state: AgentState, response: SymbolicMessage): AgentState {
        require(response.recipientId == state.id) { "response has a different recipient" }
        val other = response.senderId
        val accepted = SymbolicIntent.YES in response.intents
        val relationship = state.relationships[other] ?: RelationshipState()
        val nextRelationship = relationship.copy(
            affinity = (relationship.affinity + if (accepted) 4 else -2).coerceIn(-100, 100),
            interactions = relationship.interactions + 1,
            lastInteractionMillis = response.atMillis
        )
        val kind = if (accepted) EpisodeKind.SOCIAL_ACCEPTED else EpisodeKind.SOCIAL_REJECTED
        val episode = Episode(
            id = "${response.atMillis}:reply:$other",
            atMillis = response.atMillis,
            kind = kind,
            goal = LivingGoal.CONNECT,
            action = LivingAction.RESPOND_TO_INVITE,
            targetId = other,
            valence = if (accepted) 1 else -1
        )
        return learn(
            state.copy(
                relationships = state.relationships + (other to nextRelationship),
                memories = appendMemory(state.memories, episode)
            ),
            LivingGoal.CONNECT,
            episode.valence
        )
    }

    private fun utility(goal: LivingGoal, state: AgentState, world: LivingWorldState): UtilityScore {
        val need = primaryNeed(goal, state)
        val pressure = state.pressure(need)
        val needValue = pressure * state.personality.weight(need) / 100
        val memoryValue = state.memories
            .asReversed()
            .filter { it.goal == goal }
            .take(5)
            .sumOf { it.valence * 3 }
            .coerceIn(-20, 20)
        val social = if (goal == LivingGoal.CONNECT) {
            val bestAffinity = world.nearbyAgentIds.maxOfOrNull {
                state.relationships[it]?.affinity ?: 0
            } ?: 0
            bestAffinity / 4 + if (world.nearbyAgentIds.isNotEmpty()) 5 else 0
        } else 0
        val estimated = plan(goal, world, 0).steps
        val monetaryCost = if (LivingAction.BUY_FOOD in estimated) world.groceryCost else 0
        val riskUnits = estimated.sumOf {
            when (it) {
                LivingAction.WORK -> 2
                LivingAction.BUY_FOOD -> 1
                else -> 0
            }
        }
        val unavailableRisk = estimated.count { it !in world.availableActions } * 80
        val urgent = if (
            pressure >= URGENT_PRESSURE &&
            (goal == LivingGoal.GET_FOOD || goal == LivingGoal.REST)
        ) URGENCY_BONUS else 0
        return UtilityScore(
            goal = goal,
            needSatisfaction = needValue,
            personalityPreference = state.personality.bias(goal),
            learnedPreference = state.learnedGoalPreferences[goal] ?: 0,
            longTermValue = state.longTermGoals[goal] ?: 0,
            memoryValue = memoryValue,
            socialValue = social,
            cost = monetaryCost,
            time = estimated.size * 2,
            risk = riskUnits * state.personality.riskAversion / 5 + unavailableRisk,
            urgency = urgent
        )
    }

    private fun primaryNeed(goal: LivingGoal, state: AgentState): Need = when (goal) {
        LivingGoal.GET_FOOD -> Need.HUNGER
        LivingGoal.REST -> if (state.pressure(Need.ENERGY) >= state.pressure(Need.COMFORT)) {
            Need.ENERGY
        } else Need.COMFORT
        LivingGoal.CONNECT -> Need.SOCIAL
        LivingGoal.HAVE_FUN -> Need.FUN
        LivingGoal.DEVELOP -> if (
            state.pressure(Need.CURIOSITY) >= state.pressure(Need.DEVELOPMENT)
        ) Need.CURIOSITY else Need.DEVELOPMENT
    }

    private fun urgentGoal(state: AgentState): LivingGoal? {
        val food = state.pressure(Need.HUNGER)
        val rest = max(state.pressure(Need.ENERGY), state.pressure(Need.COMFORT))
        return when {
            food < URGENT_PRESSURE && rest < URGENT_PRESSURE -> null
            food >= rest -> LivingGoal.GET_FOOD
            else -> LivingGoal.REST
        }
    }

    private fun obstacleFor(action: LivingAction, world: LivingWorldState): AgentObstacle? {
        if (action !in world.availableActions) {
            return AgentObstacle(ObstacleKind.ACTION_UNAVAILABLE, action)
        }
        return when (action) {
            LivingAction.BUY_FOOD -> if (world.coins < world.groceryCost) {
                AgentObstacle(ObstacleKind.INSUFFICIENT_MONEY, action)
            } else null
            LivingAction.EAT -> when {
                world.location != LivingLocation.HOME -> AgentObstacle(ObstacleKind.NOT_AT_HOME, action)
                world.foodAtHome <= 0 -> AgentObstacle(ObstacleKind.NO_FOOD, action)
                else -> null
            }
            LivingAction.REST,
            LivingAction.INSPECT_FOOD -> if (world.location != LivingLocation.HOME) {
                AgentObstacle(ObstacleKind.NOT_AT_HOME, action)
            } else null
            LivingAction.INVITE_TO_PLAY -> if (world.nearbyAgentIds.isEmpty()) {
                AgentObstacle(ObstacleKind.NO_COMPANION, action)
            } else null
            else -> null
        }
    }

    private fun execute(
        state: AgentState,
        world: LivingWorldState,
        plan: LivingPlan,
        action: LivingAction,
        nowMillis: Long,
        setupEvents: List<LivingEvent>
    ): AgentTransition {
        var nextState = state.copy(currentAction = action)
        var nextWorld = world
        var message: SymbolicMessage? = null
        val kind = when (action) {
            LivingAction.INSPECT_FOOD -> if (world.foodAtHome > 0) {
                LivingEventKind.FOOD_FOUND
            } else LivingEventKind.FOOD_SHORTAGE
            LivingAction.WORK -> {
                nextWorld = world.copy(
                    coins = world.coins + world.wage,
                    location = LivingLocation.WORKPLACE,
                    revision = world.revision + 1
                )
                nextState = nextState.adjust(Need.ENERGY, 12).adjust(Need.HUNGER, 6)
                LivingEventKind.WORK_COMPLETED
            }
            LivingAction.BUY_FOOD -> {
                nextWorld = world.copy(
                    coins = world.coins - world.groceryCost,
                    carriedFood = world.carriedFood + world.groceryQuantity,
                    location = LivingLocation.SHOP,
                    revision = world.revision + 1
                )
                LivingEventKind.FOOD_BOUGHT
            }
            LivingAction.RETURN_HOME -> {
                nextWorld = world.copy(
                    foodAtHome = world.foodAtHome + world.carriedFood,
                    carriedFood = 0,
                    location = LivingLocation.HOME,
                    revision = world.revision + 1
                )
                LivingEventKind.RETURNED_HOME
            }
            LivingAction.EAT -> {
                nextWorld = world.copy(foodAtHome = world.foodAtHome - 1, revision = world.revision + 1)
                nextState = nextState.setPressure(Need.HUNGER, 10)
                LivingEventKind.ATE
            }
            LivingAction.REST -> {
                nextState = nextState.setPressure(Need.ENERGY, 5).setPressure(Need.COMFORT, 5)
                LivingEventKind.RESTED
            }
            LivingAction.ENJOY_MUSIC -> {
                nextState = nextState.setPressure(Need.FUN, 5)
                LivingEventKind.ENJOYED_MUSIC
            }
            LivingAction.STUDY -> {
                nextState = nextState
                    .setPressure(Need.CURIOSITY, 10)
                    .setPressure(Need.DEVELOPMENT, 10)
                    .adjust(Need.ENERGY, 8)
                LivingEventKind.STUDIED
            }
            LivingAction.INVITE_TO_PLAY -> {
                val target = world.nearbyAgentIds.sorted().first()
                message = SymbolicMessage(
                    senderId = state.id,
                    recipientId = target,
                    intents = setOf(SymbolicIntent.PLAY, SymbolicIntent.QUESTION),
                    atMillis = nowMillis
                )
                val old = nextState.relationships[target] ?: RelationshipState()
                nextState = nextState.copy(
                    relationships = nextState.relationships + (
                        target to old.copy(
                            interactions = old.interactions + 1,
                            lastInteractionMillis = nowMillis
                        )
                    )
                )
                LivingEventKind.MESSAGE_SENT
            }
            LivingAction.RESPOND_TO_INVITE -> error("responses use respond()")
        }
        val event = LivingEvent(
            kind = kind,
            actorId = state.id,
            atMillis = nowMillis,
            goal = plan.goal,
            action = action,
            targetId = message?.recipientId,
            intents = message?.intents.orEmpty()
        )
        nextState = remember(nextState, event)
        val remaining = plan.steps.drop(1)
        val completed = remaining.isEmpty()
        val continuedPlan = if (completed) null else plan.copy(
            steps = remaining,
            worldRevision = nextWorld.revision
        )
        if (completed && kind in SUCCESS_EVENTS) nextState = learn(nextState, plan.goal, 1)
        nextState = nextState.copy(
            currentGoal = if (completed) null else plan.goal,
            plan = continuedPlan,
            lastEvent = event
        )
        return AgentTransition(nextState, nextWorld, setupEvents + event, message)
    }

    private fun remember(state: AgentState, event: LivingEvent): AgentState {
        val episode = when (event.kind) {
            LivingEventKind.FOOD_SHORTAGE -> Episode(
                episodeId(event), event.atMillis, EpisodeKind.SHORTAGE,
                event.goal, event.action, event.targetId, -1
            )
            LivingEventKind.ACTION_FAILED,
            LivingEventKind.PLAN_BLOCKED -> Episode(
                episodeId(event), event.atMillis, EpisodeKind.FAILURE,
                event.goal, event.action, event.targetId, -1
            )
            in SUCCESS_EVENTS -> Episode(
                episodeId(event), event.atMillis, EpisodeKind.SUCCESS,
                event.goal, event.action, event.targetId, 1
            )
            else -> null
        }
        return if (episode == null) state else state.copy(
            memories = appendMemory(state.memories, episode),
            lastEvent = event
        )
    }

    private fun learn(state: AgentState, goal: LivingGoal, valence: Int): AgentState {
        val current = state.learnedGoalPreferences[goal] ?: 0
        val next = (current + valence).coerceIn(-20, 20)
        return state.copy(learnedGoalPreferences = state.learnedGoalPreferences + (goal to next))
    }

    private fun AgentState.adjust(need: Need, delta: Int): AgentState =
        setPressure(need, pressure(need) + delta)

    private fun AgentState.setPressure(need: Need, value: Int): AgentState =
        copy(needs = needs + (need to value.coerceIn(0, 100)))

    private fun appendMemory(memories: List<Episode>, episode: Episode): List<Episode> =
        (memories + episode).takeLast(MAX_MEMORIES)

    private fun episodeId(event: LivingEvent): String =
        "${event.atMillis}:${event.kind}:${event.action ?: "none"}:${event.targetId ?: "none"}"

    private fun event(
        kind: LivingEventKind,
        state: AgentState,
        atMillis: Long,
        goal: LivingGoal? = null,
        action: LivingAction? = null,
        obstacle: AgentObstacle? = null
    ) = LivingEvent(kind, state.id, atMillis, goal, action, obstacle)

    private val SUCCESS_EVENTS = setOf(
        LivingEventKind.WORK_COMPLETED,
        LivingEventKind.FOOD_BOUGHT,
        LivingEventKind.ATE,
        LivingEventKind.RESTED,
        LivingEventKind.ENJOYED_MUSIC,
        LivingEventKind.STUDIED
    )
}
