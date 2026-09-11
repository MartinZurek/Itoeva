package com.notime.glyphsim.living

/**
 * **Wovon ein Wesen gerade etwas will** - die langlebige Absicht ueber einer Handlung.
 *
 * Ein Ziel ueberlebt seinen Plan. Genau daran haengt der Unterschied zwischen einem lebendigen
 * Wesen und einer Abfolge: Scheitert ein Schritt, faellt der PLAN, nicht der Wunsch. Der Agent
 * will weiter essen und sucht einen anderen Weg dorthin (siehe [Planner.planFor] und
 * [LivingSimulation]).
 */
enum class GoalKind {
    /** Etwas zu essen bekommen - das Ziel, das heute mehrschrittige Plaene erzwingt. */
    GET_FOOD,

    /** Sich erholen. */
    REST,

    /** Sich vergnuegen. */
    HAVE_FUN,

    /** Etwas lernen, besser werden. */
    DEVELOP,

    /** Die Naehe eines wirklich anwesenden Wesens suchen. */
    CONNECT_WITH;

    /**
     * Das Beduerfnis, dessen Druck dieses Ziel traegt.
     *
     * Eins zu eins und nicht als Gewichtung mehrerer: Ein Ziel, das aus drei Beduerfnissen
     * gespeist wird, laesst sich nicht mehr in einem Satz begruenden - und Begruendbarkeit ist
     * die Abnahmebedingung dieses Meilensteins.
     *
     */
    val drivenBy: NeedKind
        get() = when (this) {
            GET_FOOD -> NeedKind.HUNGER
            REST -> NeedKind.ENERGY
            HAVE_FUN -> NeedKind.FUN
            DEVELOP -> NeedKind.GROWTH
            CONNECT_WITH -> NeedKind.SOCIAL
        }
}

/**
 * Die Bewertung eines Ziels, aufgeschluesselt.
 *
 * **Nicht nur die Summe, sondern ihre Teile** - sonst waere "warum will er das?" nur mit einem
 * Debugger zu beantworten, und die Anzeige muesste sich eine Begruendung ausdenken.
 */
data class GoalScore(
    val goal: GoalKind,
    /** Druck des tragenden Beduerfnisses, 0 bis 1. */
    val needPressure: Double,
    /** Verschiebung durch die Persoenlichkeit. */
    val bias: Double,
    /** Abzug fuer Muehe und Zeit des billigsten bekannten Wegs. */
    val cost: Double,
    /** Langsam aus wiederholtem Erleben gewachsener Geschmack. */
    val learnedPreference: Double = 0.0,
    /** Kleine Spur der juengsten passenden Erfolge und Fehlschlaege. */
    val memoryInfluence: Double = 0.0,
    /** Wert einer vorhandenen Beziehung fuer ein soziales Ziel. */
    val socialValue: Double = 0.0,
    /** Begrenzter Anstoss von ausserhalb der Simulation, niemals ein Befehl. */
    val externalInfluence: Double = 0.0
) {
    val total: Double
        get() = needPressure + bias + learnedPreference + memoryInfluence + socialValue +
            externalInfluence - cost
}

/**
 * Ein kleiner, fluechtiger Zielanreiz fuer Nutzer- oder spaetere Stream-Impulse.
 *
 * Er liegt absichtlich nicht im [AgentState]: Ein Zuschauer veraendert weder Bedarf noch
 * Persoenlichkeit. Die Obergrenze unter [UtilitySelector.MIN_PRESSURE] sorgt zugleich dafuer,
 * dass ein dringendes Grundbeduerfnis den Anstoss weiterhin ueberstimmen kann.
 */
data class GoalInfluence(
    val goal: GoalKind,
    val weight: Double = MAX_WEIGHT
) {
    init {
        require(weight in 0.0..MAX_WEIGHT) { "weight must be between 0 and $MAX_WEIGHT" }
    }

    companion object {
        const val MAX_WEIGHT = 0.15
    }
}

/**
 * **Welches Ziel gewinnt** - und warum.
 *
 * Ohne Zufall. Gleiche Eingaben ergeben dieselbe Reihenfolge; bei Gleichstand entscheidet die
 * Reihenfolge im Enum. Ein Wuerfel waere hier bequem und genau falsch: Eine Entscheidung, die
 * sich nicht wiederholen laesst, laesst sich auch nicht erklaeren.
 */
object UtilitySelector {

    /**
     * Ab welchem Druck ein Ziel ueberhaupt in Frage kommt.
     *
     * Ohne Schwelle waere immer irgendein Ziel aktiv, und ein Wesen ohne Beduerfnisse liefe
     * trotzdem los. Ein satter, ausgeruhter, zufriedener Agent soll nichts vorhaben duerfen.
     */
    const val MIN_PRESSURE = 0.2

    /**
     * Alle Ziele, absteigend nach [GoalScore.total].
     *
     * [world] geht ein, weil die Kosten eines Ziels von der Lage abhaengen: Essen ist teuer,
     * wenn erst gearbeitet werden muss, und billig, wenn der Vorrat voll ist. Genau das macht
     * aus derselben Beduerfnislage an zwei Tagen zwei verschiedene Entscheidungen.
     */
    fun rank(
        agent: AgentState,
        world: WorldState,
        influence: GoalInfluence? = null
    ): List<GoalScore> =
        GoalKind.entries
            .map { goal ->
                GoalScore(
                    goal = goal,
                    needPressure = agent.needs.pressure(goal.drivenBy),
                    bias = agent.personality.bias(goal),
                    cost = Planner.estimatedCost(goal, world, agent),
                    learnedPreference = agent.learnedPreferences[goal] ?: 0.0,
                    memoryInfluence = agent.episodes
                        .asReversed()
                        .filter { it.event.goal == goal }
                        .take(RECENT_EPISODES)
                        .sumOf { it.valence * MEMORY_WEIGHT },
                    socialValue = if (goal == GoalKind.CONNECT_WITH) {
                        world.nearbyProfiles
                            .asSequence()
                            .filter { it != agent.profileId }
                            .mapNotNull { agent.relationships[it] }
                            .maxOfOrNull { (it.trust + it.closeness) * RELATIONSHIP_WEIGHT }
                            ?: 0.0
                    } else 0.0,
                    externalInfluence = influence
                        ?.takeIf { it.goal == goal }
                        ?.weight
                        ?: 0.0
                )
            }
            .sortedWith(compareByDescending<GoalScore> { it.total }.thenBy { it.goal.ordinal })

    /** Das gewinnende Ziel - oder `null`, wenn nichts draengend genug ist. */
    fun choose(
        agent: AgentState,
        world: WorldState,
        influence: GoalInfluence? = null
    ): GoalKind? = rank(agent, world, influence)
        .firstOrNull { it.needPressure >= MIN_PRESSURE || it.externalInfluence > 0.0 }
        ?.goal

    private const val RECENT_EPISODES = 6
    private const val MEMORY_WEIGHT = 0.015
    private const val RELATIONSHIP_WEIGHT = 0.05
}

/**
 * Ein Plan: geordnete Handlungen und das Ziel, aus dem sie stammen.
 *
 * Der Plan haelt sein [goal] fest, damit nach einem Fehlschlag klar ist, WAS neu geplant werden
 * muss. Ein Plan ohne Ziel waere nach dem ersten Hindernis nur noch Muell.
 */
data class Plan(val goal: GoalKind, val steps: List<Action>) {

    val next: Action? get() = steps.firstOrNull()

    val isDone: Boolean get() = steps.isEmpty()

    fun advanced(): Plan = copy(steps = steps.drop(1))

    val kinds: List<ActionKind> get() = steps.map { it.kind }
}

/**
 * **Wie ein Ziel zu einem Weg wird.**
 *
 * Der Planer ist absichtlich klein und regelbasiert und ausdruecklich kein allgemeiner
 * KI-Planer: Er kennt je Ziel die Wege, die es in dieser Welt gibt, und waehlt den, dessen
 * Voraussetzungen die aktuelle Lage traegt. Das reicht fuer die Abnahmebedingung
 * `WORK -> BUY_FOOD -> EAT` und laesst sich lesen.
 *
 * **Er plant vorwaerts aus der WIRKLICHEN Lage**, nicht aus einer Annahme. Der Plan ist deshalb
 * beim Erstellen gueltig - und kann trotzdem ungueltig werden, sobald sich die Welt zwischen
 * zwei Schritten aendert. Genau diesen Fall faengt [LivingSimulation] mit einer Neuplanung ab.
 */
object Planner {

    /**
     * Der Weg zu [goal] aus der Lage [world] - oder `null`, wenn es heute keinen gibt.
     *
     * `null` ist kein Fehler, sondern eine Aussage: Wer hungrig, pleite und vor einem
     * geschlossenen Arbeitsplatz steht, hat keinen Weg zu Essen. Der Agent behaelt dann sein
     * Ziel und meldet das Hindernis, statt eine Handlung zu erfinden.
     */
    fun planFor(goal: GoalKind, world: WorldState, agent: AgentState? = null): Plan? {
        val schritte = when (goal) {
            GoalKind.GET_FOOD -> foodSteps(world)
            GoalKind.REST -> goTo(LivingSite.HOME, world) + ActionCatalog[ActionKind.REST]
            GoalKind.HAVE_FUN, GoalKind.DEVELOP -> listOf(ActionCatalog[ActionKind.PURSUE_INTEREST])
            GoalKind.CONNECT_WITH -> world.nearbyProfiles
                .asSequence()
                .filter { it != agent?.profileId }
                .sorted()
                .firstOrNull()
                ?.let { listOf(ActionCatalog.inviteToPlay(it)) }
        }
        return schritte?.let { Plan(goal, it) }
    }

    /**
     * Der Weg zu Essen - die Stelle, an der aus einer Ressourcenlage eine Geschichte wird.
     *
     * Drei Faelle, in dieser Reihenfolge, weil jeder den naechsten nur dann braucht, wenn er
     * selbst nicht geht:
     *
     * 1. Vorrat da: nachsehen und essen.
     * 2. Vorrat leer, Geld reicht: einkaufen, dann essen.
     * 3. Beides leer: arbeiten, einkaufen, essen.
     *
     * Der dritte Fall ist nicht als Kette hinterlegt, sondern faellt aus derselben Bedingung
     * ab wie die anderen beiden - deshalb entsteht er auch dann, wenn er nie ausdruecklich
     * vorgesehen wurde, und deshalb verschwindet er, sobald Geld da ist.
     */
    private fun foodSteps(world: WorldState): List<Action>? {
        val essen = goTo(LivingSite.HOME, world) +
            ActionCatalog[ActionKind.INSPECT_FOOD] +
            ActionCatalog[ActionKind.EAT]
        if (world.portions >= 1) return essen

        val einkauf = goTo(LivingSite.MARKET, world) + ActionCatalog[ActionKind.BUY_FOOD]
        if (world.coins >= ActionCatalog.GROCERY_COST) {
            if (LivingSite.MARKET !in world.openSites) return null
            return einkauf + goTo(LivingSite.HOME, world, from = LivingSite.MARKET) +
                ActionCatalog[ActionKind.INSPECT_FOOD] + ActionCatalog[ActionKind.EAT]
        }

        if (LivingSite.WORKPLACE !in world.openSites || LivingSite.MARKET !in world.openSites) return null
        return goTo(LivingSite.WORKPLACE, world) + ActionCatalog[ActionKind.WORK] +
            goTo(LivingSite.MARKET, world, from = LivingSite.WORKPLACE) +
            ActionCatalog[ActionKind.BUY_FOOD] +
            goTo(LivingSite.HOME, world, from = LivingSite.MARKET) +
            ActionCatalog[ActionKind.INSPECT_FOOD] + ActionCatalog[ActionKind.EAT]
    }

    /** Ein Weg, sofern der Agent nicht schon dort ist. [from] beruecksichtigt Zwischenschritte. */
    private fun goTo(site: LivingSite, world: WorldState, from: LivingSite = world.site): List<Action> =
        if (from == site) emptyList() else listOf(ActionCatalog.travelTo(site))

    /**
     * Was dieses Ziel gerade ungefaehr kostet - Muehe plus Zeit, grob.
     *
     * Grob mit Absicht: Die Zahl soll teure Wege spuerbar zuruecksetzen, nicht die Zukunft
     * ausrechnen. Wer hier optimiert, baut den allgemeinen Planer, den dieser Meilenstein
     * ausdruecklich nicht will.
     */
    fun estimatedCost(goal: GoalKind, world: WorldState, agent: AgentState? = null): Double {
        val plan = planFor(goal, world, agent) ?: return UNREACHABLE
        val muehe = plan.steps.sumOf { it.effort }
        val zeit = plan.steps.sumOf { it.outcome.minutes } / MINUTES_PER_COST_POINT
        return muehe + zeit
    }

    /**
     * Der Abzug fuer ein Ziel ohne Weg. Gross genug, dass es nie gewinnt, und endlich, damit
     * die Rangfolge trotzdem lesbar bleibt - `Double.MAX_VALUE` machte aus jeder Erklaerung
     * eine Zahlenwueste.
     */
    const val UNREACHABLE = 99.0

    /**
     * Wie viele Minuten einem Kostenpunkt entsprechen.
     *
     * 600 heisst: Ein zehnstuendiger Weg kostet einen ganzen Punkt und schlaegt damit jeden
     * Beduerfnisdruck. Ein halbstuendiger kostet 0,05 und faellt kaum ins Gewicht - richtig so,
     * denn Zeit ist hier kein knappes Gut, solange der Tag reicht.
     */
    private const val MINUTES_PER_COST_POINT = 600.0
}
