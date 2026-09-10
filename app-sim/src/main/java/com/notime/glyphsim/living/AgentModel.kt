package com.notime.glyphsim.living

/**
 * Ein hoher Wert bedeutet Druck: 0 ist gedeckt, 100 ist dringend.
 * Bei [ENERGY] ist damit Erschoepfung gemeint, nicht vorhandene Energie.
 */
enum class Need {
    HUNGER,
    ENERGY,
    SOCIAL,
    FUN,
    COMFORT,
    CURIOSITY,
    DEVELOPMENT
}

enum class LivingGoal {
    GET_FOOD,
    REST,
    CONNECT,
    HAVE_FUN,
    DEVELOP
}

/** Der bewusst kleine Aktionswortschatz des ersten vertikalen Schnitts. */
enum class LivingAction {
    INSPECT_FOOD,
    WORK,
    BUY_FOOD,
    RETURN_HOME,
    EAT,
    REST,
    ENJOY_MUSIC,
    STUDY,
    INVITE_TO_PLAY,
    RESPOND_TO_INVITE
}

enum class LivingLocation { HOME, WORKPLACE, SHOP, OUTDOORS }

/**
 * Startbias eines Wesens. Die spaetere Spezies-Anbindung uebersetzt [AvatarSpecies] hierher;
 * gelernte Praeferenzen liegen getrennt im [AgentState] und koennen den Startbias veraendern.
 */
data class PersonalityProfile(
    val needWeights: Map<Need, Int> = emptyMap(),
    val goalBiases: Map<LivingGoal, Int> = emptyMap(),
    val needDriftPerHour: Map<Need, Int> = emptyMap(),
    val riskAversion: Int = 5
) {
    fun weight(need: Need): Int = needWeights[need] ?: 100
    fun bias(goal: LivingGoal): Int = goalBiases[goal] ?: 0
    fun drift(need: Need): Int = needDriftPerHour[need] ?: DEFAULT_DRIFT.getValue(need)

    companion object {
        private val DEFAULT_DRIFT = mapOf(
            Need.HUNGER to 5,
            Need.ENERGY to 4,
            Need.SOCIAL to 2,
            Need.FUN to 3,
            Need.COMFORT to 1,
            Need.CURIOSITY to 2,
            Need.DEVELOPMENT to 1
        )
    }
}

data class LivingPlan(
    val goal: LivingGoal,
    val steps: List<LivingAction>,
    val createdAtMillis: Long,
    /** Revision, gegen die der Plan gebaut oder zuletzt selbst fortgeschrieben wurde. */
    val worldRevision: Long
)

enum class EpisodeKind {
    SHORTAGE,
    SUCCESS,
    FAILURE,
    SOCIAL_ACCEPTED,
    SOCIAL_REJECTED
}

/** Kompakte bedeutungsvolle Erinnerung, niemals ein Frame- oder Tick-Protokoll. */
data class Episode(
    val id: String,
    val atMillis: Long,
    val kind: EpisodeKind,
    val goal: LivingGoal?,
    val action: LivingAction?,
    val targetId: String? = null,
    val valence: Int
)

data class RelationshipState(
    val affinity: Int = 0,
    val interactions: Int = 0,
    val lastInteractionMillis: Long? = null
)

data class AgentState(
    val id: String,
    val needs: Map<Need, Int>,
    val personality: PersonalityProfile = PersonalityProfile(),
    val longTermGoals: Map<LivingGoal, Int> = emptyMap(),
    val learnedGoalPreferences: Map<LivingGoal, Int> = emptyMap(),
    val currentGoal: LivingGoal? = null,
    val plan: LivingPlan? = null,
    val currentAction: LivingAction? = null,
    val memories: List<Episode> = emptyList(),
    val relationships: Map<String, RelationshipState> = emptyMap(),
    val lastEvent: LivingEvent? = null
) {
    fun pressure(need: Need): Int = (needs[need] ?: 0).coerceIn(0, 100)
}

/**
 * Der tatsaechliche kleine Weltausschnitt fuer eine Entscheidung. [revision] wird bei jeder
 * Aenderung erhoeht; dadurch kann auch eine Aenderung zwischen zwei Schritten Replanning
 * ausloesen, ohne dass der Planer Android- oder UI-Zustand beobachten muss.
 */
data class LivingWorldState(
    val coins: Int,
    val foodAtHome: Int,
    val location: LivingLocation,
    /** Essen, das bereits gekauft wurde, aber noch nach Hause getragen wird. */
    val carriedFood: Int = 0,
    val availableActions: Set<LivingAction> = LivingAction.entries.toSet(),
    val nearbyAgentIds: Set<String> = emptySet(),
    val groceryCost: Int = 2,
    val groceryQuantity: Int = 3,
    val wage: Int = 2,
    val revision: Long = 0
)

enum class ObstacleKind {
    ACTION_UNAVAILABLE,
    INSUFFICIENT_MONEY,
    NO_FOOD,
    NOT_AT_HOME,
    NO_COMPANION
}

data class AgentObstacle(val kind: ObstacleKind, val action: LivingAction)

/** Alle Summanden bleiben sichtbar, damit eine Wahl nicht nur als Gesamtzahl erklaert wird. */
data class UtilityScore(
    val goal: LivingGoal,
    val needSatisfaction: Int,
    val personalityPreference: Int,
    val learnedPreference: Int,
    val longTermValue: Int,
    val memoryValue: Int,
    val socialValue: Int,
    val cost: Int,
    val time: Int,
    val risk: Int,
    val urgency: Int
) {
    val total: Int
        get() = needSatisfaction + personalityPreference + learnedPreference + longTermValue +
            memoryValue + socialValue + urgency - cost - time - risk
}

data class GoalDecision(
    val goal: LivingGoal,
    val score: UtilityScore,
    val allScores: List<UtilityScore>,
    val influentialMemories: List<Episode>
)

enum class SymbolicIntent {
    FOOD,
    PLAY,
    MUSIC,
    HOME,
    WORK,
    AFFECTION,
    QUESTION,
    YES,
    NO,
    TIRED,
    SURPRISE
}

data class SymbolicMessage(
    val senderId: String,
    val recipientId: String,
    val intents: Set<SymbolicIntent>,
    val atMillis: Long
)

enum class LivingEventKind {
    PLAN_CREATED,
    PLAN_REBUILT,
    PLAN_BLOCKED,
    ACTION_FAILED,
    FOOD_FOUND,
    FOOD_SHORTAGE,
    WORK_COMPLETED,
    FOOD_BOUGHT,
    RETURNED_HOME,
    ATE,
    RESTED,
    ENJOYED_MUSIC,
    STUDIED,
    MESSAGE_SENT,
    MESSAGE_RECEIVED
}

/** Bedeutungsvolle Fakten, die eine spaetere Erzaehlschicht nur beobachten darf. */
data class LivingEvent(
    val kind: LivingEventKind,
    val actorId: String,
    val atMillis: Long,
    val goal: LivingGoal? = null,
    val action: LivingAction? = null,
    val obstacle: AgentObstacle? = null,
    val targetId: String? = null,
    val intents: Set<SymbolicIntent> = emptySet()
)

data class AgentExplanation(
    val doing: LivingAction?,
    val wants: LivingGoal?,
    val why: UtilityScore?,
    val plan: List<LivingAction>,
    val obstacle: AgentObstacle?,
    val influentialMemories: List<Episode>,
    val recentEvent: LivingEvent?
)

data class AgentTransition(
    val state: AgentState,
    val world: LivingWorldState,
    val events: List<LivingEvent>,
    val message: SymbolicMessage? = null
)

data class CommunicationResponse(
    val state: AgentState,
    val event: LivingEvent,
    val message: SymbolicMessage
)
