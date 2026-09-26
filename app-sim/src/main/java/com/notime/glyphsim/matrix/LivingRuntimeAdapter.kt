package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.living.Action
import com.notime.glyphsim.living.ActionCatalog
import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.living.AgentState
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.GoalInfluence
import com.notime.glyphsim.living.LivingEvent
import com.notime.glyphsim.living.LivingEventKind
import com.notime.glyphsim.living.LivingSimulation
import com.notime.glyphsim.living.LivingSite
import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.living.Needs
import com.notime.glyphsim.living.Personality
import com.notime.glyphsim.living.StepResult
import com.notime.glyphsim.living.SymbolicMessage
import com.notime.glyphsim.living.WorldState
import kotlin.random.Random

/**
 * Ein vorbereiteter sichtbarer Schritt des Living Agent Systems.
 *
 * [result] ist noch nicht gespeichert. Der Aufrufer uebernimmt ihn erst, nachdem [routine]
 * vollstaendig gelaufen ist. Eine echte Erinnerung kann die Choreografie deshalb abbrechen,
 * ohne dass Lohn, Einkauf oder Erinnerung bereits unsichtbar verbucht wurden.
 */
data class PreparedLivingRoutine(
    val result: StepResult,
    val topic: AnimationType?,
    val routine: PlayRoutine?,
    val completedActions: List<ActionKind>
)

/**
 * Die schmale Grenze zwischen erklaerbarer Entscheidung und vorhandener Pixelwelt.
 *
 * Der Adapter erfindet keine zweite Ablaufmaschine. Er laesst [LivingSimulation] entscheiden
 * und fasst nur solche aufeinanderfolgenden Kernschritte zu einer sichtbaren [PlayRoutine]
 * zusammen, die deren vorhandene Choreografie bereits gemeinsam zeigt: Arbeitsweg plus Arbeit,
 * beziehungsweise Laden, Heimweg, Einraeumen und Essen.
 */
object LivingRuntimeAdapter {

    /** Die siebzehn sichtbaren Orte auf die vier entscheidungsrelevanten Orte abbilden. */
    fun siteFor(place: PlayScene.Place): LivingSite = when (place) {
        PlayScene.Place.WORK -> LivingSite.WORKPLACE
        PlayScene.Place.SHOP -> LivingSite.MARKET
        PlayScene.Place.PARK,
        PlayScene.Place.SPORT,
        PlayScene.Place.POND,
        PlayScene.Place.STREET,
        PlayScene.Place.FOREST,
        PlayScene.Place.MEADOW,
        PlayScene.Place.CITY,
        // Die Spielhalle ist fuer die Entscheidungen des Kerns "unterwegs" - weder Zuhause noch
        // Arbeit noch Markt.
        PlayScene.Place.ARCADE -> LivingSite.OUTSIDE
        PlayScene.Place.BEDROOM,
        PlayScene.Place.BATH,
        PlayScene.Place.DESK,
        PlayScene.Place.KITCHEN,
        PlayScene.Place.NOOK,
        PlayScene.Place.LIVING,
        PlayScene.Place.CRAFT -> LivingSite.HOME
    }

    /**
     * Die erste kleine echte Zeitbedingung der Welt.
     *
     * Der Kern bekommt nur die resultierende Menge. So bleiben Oeffnungszeiten hier an der
     * Runtime-Grenze und werden nicht als zweite Uhr in die Domaene geschoben.
     */
    fun openSitesAt(minuteOfDay: Int): Set<LivingSite> = buildSet {
        val minute = minuteOfDay.coerceIn(0, WorldState.MINUTES_PER_DAY - 1)
        add(LivingSite.HOME)
        add(LivingSite.OUTSIDE)
        if (minute in WORK_OPENS until WORK_CLOSES) add(LivingSite.WORKPLACE)
        if (minute in MARKET_OPENS until MARKET_CLOSES) add(LivingSite.MARKET)
    }

    /** Ein neuer Agent beginnt weder in Not noch vollkommen antriebslos. */
    fun initialAgent(profileId: String, species: AvatarSpecies): AgentState = AgentState(
        profileId = profileId,
        personality = Personality.of(species),
        needs = Needs.of(
            NeedKind.HUNGER to 0.28,
            NeedKind.ENERGY to 0.18,
            NeedKind.FUN to 0.38,
            NeedKind.SOCIAL to 0.22,
            NeedKind.COMFORT to 0.12,
            NeedKind.CURIOSITY to 0.30,
            NeedKind.GROWTH to 0.26
        )
    )

    /**
     * Uebernimmt den vorhandenen Vorrat und Geldbeutel beim ersten Anschluss.
     *
     * Danach ist der profilbezogene [WorldState] die Quelle. Die alten Preferences werden nicht
     * umgedeutet oder geloescht; sie sind nur der einmalige Startwert fuer bestehende Welten.
     */
    fun initialWorld(
        absoluteMinute: Int,
        place: PlayScene.Place,
        coins: Int,
        portions: Int,
        nearbyProfiles: Set<String> = emptySet()
    ): WorldState {
        val safeMinute = absoluteMinute.coerceAtLeast(0)
        val minuteOfDay = safeMinute % WorldState.MINUTES_PER_DAY
        return WorldState(
            day = safeMinute / WorldState.MINUTES_PER_DAY,
            minuteOfDay = minuteOfDay,
            site = siteFor(place),
            coins = coins.coerceAtLeast(0),
            portions = portions.coerceIn(0, ActionCatalog.GROCERY_PORTIONS),
            openSites = openSitesAt(minuteOfDay),
            nearbyProfiles = nearbyProfiles
        )
    }

    /** Sichtbaren Ort, aktuelle Verfuegbarkeit und wirkliche Nachbarn vor jeder Wahl nachziehen. */
    fun synchroniseWorld(
        world: WorldState,
        place: PlayScene.Place,
        nearbyProfiles: Set<String> = emptySet()
    ): WorldState = world.copy(
        site = siteFor(place),
        openSites = openSitesAt(world.minuteOfDay),
        nearbyProfiles = nearbyProfiles
    )

    /**
     * Entscheidet den naechsten Kernschritt und waehlt dafuer vorhandene Choreografie.
     *
     * [interestTopic] kommt aus der bisherigen gewichteten Tagesablaufwahl. Sie entscheidet
     * nicht mehr, OB ein Grundbeduerfnis uebergangen wird, sondern nur noch, WIE eine vom Kern
     * gewaehlte Freizeit- oder Entwicklungsphase aussieht.
     *
     * [preferredRoutine] ist die Wahl der Decision Policy (siehe `decision/`). Sie wird **nur**
     * uebernommen, wenn sie zu den Ablaeufen gehoert, die dieser Kernschritt ohnehin zeigen darf
     * ([options]) - die Policy waehlt zwischen erlaubten Ausformungen, sie erweitert sie nie.
     * Passt sie nicht (die Welt hat sich zwischen Bewertung und Ausfuehrung geaendert), gilt der
     * bisherige Wurf. Ohne Vorwahl verbraucht [random] genau dieselben Zahlen wie vorher.
     */
    fun prepare(
        agent: AgentState,
        world: WorldState,
        renderedPlace: PlayScene.Place,
        interestTopic: AnimationType,
        footballTrickLearned: Boolean = false,
        recentSpecials: List<PlayRoutines.SpecialActivity> = emptyList(),
        random: Random = Random.Default,
        nearbyProfiles: Set<String> = emptySet(),
        goalInfluence: GoalInfluence? = null,
        preferredRoutine: PlayRoutine? = null,
        sleepAdmissible: Boolean = false,
        chosenGoal: GoalKind? = null
    ): PreparedLivingRoutine {
        val (result, branch) = resolve(
            agent, world, renderedPlace, interestTopic, footballTrickLearned, recentSpecials,
            nearbyProfiles, goalInfluence, sleepAdmissible, chosenGoal
        )
        if (branch == null) return PreparedLivingRoutine(result, null, null, emptyList())
        // Das Thema kommt von der gewaehlten Option: Nachts kann aus "ausruhen" der Schlaf im
        // Bett werden, und dann muss auch Musik und Erinnerung SLEEP sehen und nicht REST.
        val vorgewaehlt = preferredRoutine?.let { wunsch -> branch.options.firstOrNull { it.routine == wunsch } }
        val routine = vorgewaehlt?.routine ?: branch.sample(random)
        val topic = vorgewaehlt?.topic ?: branch.topic
        return PreparedLivingRoutine(result, topic, routine, completedActions(result))
    }

    /**
     * Ein sichtbarer Ablauf, den der naechste Kernschritt zeigen darf - mit der Wahrscheinlichkeit,
     * die ihm die bisherige Wahl gibt (0 heisst: erlaubt, aber bisher nie gezogen).
     */
    data class RoutineOption(
        val topic: AnimationType,
        val routine: PlayRoutine,
        val probability: Double,
        /** Die Kernhandlung, deren Wirkung dieser Ablauf zeigt - Quelle der Action Features. */
        val coreAction: ActionKind
    )

    /**
     * **Alle sichtbaren Ablaeufe, die aus dem naechsten Kernschritt werden koennen.**
     *
     * Rechnet denselben reinen Kernschritt wie [prepare] und fuehrt denselben Zweig aus, wuerfelt
     * aber nicht. Das ist die Kandidatenquelle der Decision Policy: Was hier nicht steht, kann sie
     * nicht waehlen - Oeffnungszeiten, Vorrat, Geld und das Ziel des Kerns gelten also weiter.
     */
    fun options(
        agent: AgentState,
        world: WorldState,
        renderedPlace: PlayScene.Place,
        interestTopic: AnimationType,
        footballTrickLearned: Boolean = false,
        recentSpecials: List<PlayRoutines.SpecialActivity> = emptyList(),
        nearbyProfiles: Set<String> = emptySet(),
        goalInfluence: GoalInfluence? = null,
        sleepAdmissible: Boolean = false,
        chosenGoal: GoalKind? = null
    ): List<RoutineOption> = resolve(
        agent, world, renderedPlace, interestTopic, footballTrickLearned, recentSpecials,
        nearbyProfiles, goalInfluence, sleepAdmissible, chosenGoal
    ).second?.options ?: emptyList()

    /** Ein Zweig: welches Thema, welche Ablaeufe erlaubt, und wie bisher gewuerfelt wird. */
    private class Branch(
        val topic: AnimationType,
        val options: List<RoutineOption>,
        val sample: (Random) -> PlayRoutine
    )

    private fun single(topic: AnimationType, routine: PlayRoutine, core: ActionKind) =
        Branch(topic, listOf(RoutineOption(topic, routine, 1.0, core))) { routine }

    /**
     * Die gemeinsame Rechnung von [prepare] und [options]. Nur hier steht, welcher Kernschritt
     * zu welchem sichtbaren Zweig fuehrt - zwei Kopien davon liefen frueher oder spaeter
     * auseinander, und die Policy bewertete dann Ablaeufe, die die Welt gar nicht zeigt.
     */
    private fun resolve(
        agent: AgentState,
        world: WorldState,
        renderedPlace: PlayScene.Place,
        interestTopic: AnimationType,
        footballTrickLearned: Boolean,
        recentSpecials: List<PlayRoutines.SpecialActivity>,
        nearbyProfiles: Set<String>,
        goalInfluence: GoalInfluence?,
        sleepAdmissible: Boolean,
        chosenGoal: GoalKind?
    ): Pair<StepResult, Branch?> {
        val startWorld = synchroniseWorld(world, renderedPlace, nearbyProfiles)
        // **Die Ausformung geht VOR der Entscheidung mit hinein** (NT-072).
        //
        // Der Kern entscheidet weiterhin allein, OB Freizeit oder Entwicklung gerade traegt.
        // Faellt sie aber, soll daraus nicht wieder die eine bedeutungslose Beschaeftigung
        // werden, sondern genau das, was die gewichtete Tagesablaufwahl ohnehin schon
        // ausgesucht hat - mit deren Wirkung. Lesen stillt dann Neugier, Bewegung macht hungrig.
        var result = LivingSimulation.step(
            agent,
            startWorld,
            goalInfluence,
            interestActionFor(safeInterestTopic(agent.goal, interestTopic)),
            chosenGoal = chosenGoal
        )
        val firstAction = completedActions(result).firstOrNull() ?: return result to null

        val branch: Branch = when (firstAction) {
            ActionKind.TRAVEL -> when (result.world.site) {
                LivingSite.WORKPLACE -> {
                    result = advanceExpected(result, listOf(ActionKind.WORK))
                    if (ActionKind.WORK in completedActions(result)) {
                        workBranch(renderedPlace)
                    } else {
                        single(AnimationType.WORK, closedSiteRoutine(PlayScene.Place.WORK), ActionKind.TRAVEL)
                    }
                }
                LivingSite.MARKET -> {
                    result = advanceExpected(
                        result,
                        listOf(
                            ActionKind.BUY_FOOD,
                            ActionKind.TRAVEL,
                            ActionKind.INSPECT_FOOD,
                            ActionKind.EAT
                        )
                    )
                    if (ActionKind.BUY_FOOD in completedActions(result)) {
                        single(AnimationType.DRINK, shoppingRoutine(renderedPlace), ActionKind.BUY_FOOD)
                    } else {
                        single(AnimationType.DRINK, closedSiteRoutine(PlayScene.Place.SHOP), ActionKind.TRAVEL)
                    }
                }
                LivingSite.HOME -> {
                    if (result.agent.plan?.next?.kind == ActionKind.REST) {
                        result = advanceExpected(result, listOf(ActionKind.REST))
                        restBranch(sleepAdmissible)
                    } else {
                        result = advanceExpected(
                            result,
                            listOf(ActionKind.INSPECT_FOOD, ActionKind.EAT)
                        )
                        eatingBranch()
                    }
                }
                // **Hinaus UND dort etwas tun - in einem Ablauf.** Vorher bestand diese Runde
                // nur aus dem Weg auf die Strasse; die Bewegung oder das Erkunden, fuer das
                // die Figur hinausging, kam erst eine ganze Pause spaeter. Dazwischen stand
                // sie ohne erkennbaren Grund draussen. Dasselbe Zusammenziehen wie beim
                // Einkaufen (Weg, Kauf, Heimweg, Essen) und bei der Arbeit.
                LivingSite.OUTSIDE -> when (result.agent.plan?.next?.kind) {
                    ActionKind.MOVE_BODY -> {
                        result = advanceExpected(result, listOf(ActionKind.MOVE_BODY))
                        moveBodyBranch(footballTrickLearned, recentSpecials)
                    }
                    ActionKind.EXPLORE -> {
                        result = advanceExpected(result, listOf(ActionKind.EXPLORE))
                        exploreBranch(footballTrickLearned, recentSpecials)
                    }
                    else -> single(
                        AnimationType.MOVE,
                        PlayRoutine(listOf(RoutineStep.GoToPlace(PlayScene.Place.STREET))),
                        ActionKind.TRAVEL
                    )
                }
            }
            ActionKind.WORK -> workBranch(renderedPlace)
            ActionKind.BUY_FOOD -> {
                result = advanceExpected(
                    result,
                    listOf(ActionKind.TRAVEL, ActionKind.INSPECT_FOOD, ActionKind.EAT)
                )
                single(AnimationType.DRINK, shoppingRoutine(renderedPlace), ActionKind.BUY_FOOD)
            }
            ActionKind.INSPECT_FOOD -> {
                result = advanceExpected(result, listOf(ActionKind.EAT))
                eatingBranch()
            }
            ActionKind.EAT -> eatingBranch()
            ActionKind.REST -> restBranch(sleepAdmissible)
            ActionKind.PURSUE_INTEREST -> {
                val topic = safeInterestTopic(result.agent.goal, interestTopic)
                topicBranch(
                    topic, PlayScene.forTopic(topic), ActionKind.PURSUE_INTEREST,
                    footballTrickLearned, recentSpecials
                )
            }
            // ---- Die sieben benannten Beschaeftigungen (NT-072) ----
            //
            // Jede zeigt ihr eigenes Thema, und damit ihren eigenen Ort und ihre eigene
            // sichtbare Routine. Dass dieser `when` sie erzwingt, ist der Grund, warum eine
            // neue Kernhandlung nicht stillschweigend unsichtbar bleiben kann: Wer sie
            // hinzufuegt, muss hier sagen, wie sie aussieht, sonst faellt der Build.
            ActionKind.READ -> topicBranch(AnimationType.BOOK, ActionKind.READ)
            ActionKind.CREATE -> topicBranch(AnimationType.CREATIVITY, ActionKind.CREATE)
            ActionKind.CONCENTRATE -> topicBranch(AnimationType.FOCUS, ActionKind.CONCENTRATE)
            ActionKind.SETTLE -> topicBranch(AnimationType.MINDFULNESS, ActionKind.SETTLE)
            ActionKind.MOVE_BODY -> moveBodyBranch(footballTrickLearned, recentSpecials)
            ActionKind.EXPLORE -> exploreBranch(footballTrickLearned, recentSpecials)
            ActionKind.TEND_SELF -> topicBranch(AnimationType.MEDICINE, ActionKind.TEND_SELF)
            ActionKind.SHOW_AFFECTION -> topicBranch(AnimationType.LOVE, ActionKind.SHOW_AFFECTION)
            ActionKind.INVITE_TO_PLAY -> topicBranch(
                AnimationType.LOVE, PlayScene.Place.LIVING, ActionKind.INVITE_TO_PLAY
            )
            ActionKind.RESPOND_TO_INVITE,
            ActionKind.RECEIVE_RESPONSE -> topicBranch(
                AnimationType.GENERAL, PlayScene.Place.LIVING, firstAction
            )
            ActionKind.TRAIN_TOGETHER -> error(
                "TRAIN_TOGETHER is a completed-scene effect, not a standalone routine"
            )
        }
        return result to branch
    }

    /**
     * Verbucht eine ausdruecklich erbetene sichtbare Routine in derselben Living-Welt.
     *
     * Die Bitte ersetzt kein autonomes Ziel. Sie wendet nur die Wirkungen der Handlungen an,
     * die auf dem Bildschirm wirklich gelaufen sind, und behaelt Ziel sowie Plan des Agenten.
     * Auch hier rechnet ausschliesslich [com.notime.glyphsim.living.Action.applyTo].
     */
    fun applyRequestedRoutine(
        agent: AgentState,
        world: WorldState,
        renderedPlace: PlayScene.Place,
        topic: AnimationType,
        routine: PlayRoutine
    ): StepResult {
        val originalGoal = agent.goal
        val originalPlan = agent.plan
        val syncedWorld = synchroniseWorld(world, renderedPlace, world.nearbyProfiles)
        val goal = requestedGoal(topic)
        val actions = requestedActions(topic, routine, syncedWorld)
        var currentAgent = agent.copy(plan = null)
        var currentWorld = syncedWorld
        val events = mutableListOf<LivingEvent>()
        val messages = mutableListOf<SymbolicMessage>()

        for (action in actions) {
            currentWorld = currentWorld.copy(openSites = openSitesAt(currentWorld.minuteOfDay))
            if (!action.isPossible(currentWorld)) break
            val applied = action.applyTo(currentAgent, currentWorld, goal)
            currentAgent = applied.agent
            currentWorld = applied.world
            events += applied.event
            applied.message?.let(messages::add)
        }
        return StepResult(
            agent = currentAgent.copy(goal = originalGoal, plan = originalPlan),
            world = currentWorld,
            events = events,
            messages = messages
        )
    }

    private fun advanceExpected(start: StepResult, expected: List<ActionKind>): StepResult {
        var result = start
        for (kind in expected) {
            if (result.agent.plan?.next?.kind != kind) break
            val currentWorld = result.world.copy(openSites = openSitesAt(result.world.minuteOfDay))
            val next = LivingSimulation.step(result.agent, currentWorld)
            result = next.copy(
                events = result.events + next.events,
                messages = result.messages + next.messages
            )
            if (completedActions(next).firstOrNull() != kind) break
        }
        return result
    }

    /** Sich bewegen: ein Bewegungsablauf am Bewegungsort. */
    private fun moveBodyBranch(
        footballTrickLearned: Boolean,
        recentSpecials: List<PlayRoutines.SpecialActivity>
    ): Branch {
        val ort = PlayScene.forTopic(AnimationType.MOVE)
        val options = PlayRoutines.distributionFor(
            AnimationType.MOVE, footballTrickLearned, recentSpecials
        ).map { (routine, p) ->
            RoutineOption(AnimationType.MOVE, atPlace(ort, routine), p, ActionKind.MOVE_BODY)
        }
        return Branch(AnimationType.MOVE, merged(options)) { random ->
            atPlace(
                ort,
                PlayRoutines.forTopic(
                    topic = AnimationType.MOVE,
                    footballTrickLearned = footballTrickLearned,
                    recentSpecials = recentSpecials,
                    random = random
                )
            )
        }
    }

    /**
     * **Erkunden sieht anders aus als Bewegung, obwohl beides hinausfuehrt** (NT-074).
     *
     * Bevorzugt werden die Ablaeufe, die den ORT wechseln - Strasse, Wald, Wiese. Genau das macht
     * aus einem Weg eine Strecke: Man kommt an etwas vorbei, statt vor der Haustuer im Kreis zu
     * gehen. Wer sich nur bewegt, bleibt haeufiger in der Naehe; wer erkundet, geht weiter weg.
     */
    private fun exploreBranch(
        footballTrickLearned: Boolean,
        recentSpecials: List<PlayRoutines.SpecialActivity>
    ): Branch {
        val options = PlayRoutines.distributionFor(
            AnimationType.MOVE, footballTrickLearned, recentSpecials, preferPlaceChange = true
        ).map { (routine, p) -> RoutineOption(AnimationType.MOVE, routine, p, ActionKind.EXPLORE) }
        return Branch(AnimationType.MOVE, merged(options)) { random ->
            PlayRoutines.forTopic(
                topic = AnimationType.MOVE,
                footballTrickLearned = footballTrickLearned,
                recentSpecials = recentSpecials,
                preferPlaceChange = true,
                random = random
            )
        }
    }

    /** Thema, Ort und sichtbarer Ablauf aus einer Hand - fuer die benannten Beschaeftigungen. */
    private fun topicBranch(topic: AnimationType, core: ActionKind): Branch =
        topicBranch(topic, PlayScene.forTopic(topic), core, false, emptyList())

    private fun topicBranch(topic: AnimationType, place: PlayScene.Place, core: ActionKind): Branch =
        topicBranch(topic, place, core, false, emptyList())

    private fun topicBranch(
        topic: AnimationType,
        place: PlayScene.Place,
        core: ActionKind,
        footballTrickLearned: Boolean,
        recentSpecials: List<PlayRoutines.SpecialActivity>
    ): Branch {
        val options = PlayRoutines.distributionFor(topic, footballTrickLearned, recentSpecials)
            .map { (routine, p) -> RoutineOption(topic, atPlace(place, routine), p, core) }
        return Branch(topic, merged(options)) { random ->
            atPlace(
                place,
                PlayRoutines.forTopic(
                    topic = topic,
                    footballTrickLearned = footballTrickLearned,
                    recentSpecials = recentSpecials,
                    random = random
                )
            )
        }
    }

    /**
     * Essen daheim. Bisher immer der erste Kuechenablauf; die uebrigen (etwa der Becher) sind
     * dieselbe Mahlzeit in anderer Form und stehen der Policy mit Wahrscheinlichkeit 0 offen.
     */
    private fun eatingBranch(): Branch {
        val kueche = PlayScene.Place.KITCHEN
        val options = eatingRoutines().mapIndexed { index, routine ->
            RoutineOption(
                AnimationType.DRINK, atPlace(kueche, routine), if (index == 0) 1.0 else 0.0,
                ActionKind.EAT
            )
        }
        return Branch(AnimationType.DRINK, merged(options)) { atPlace(kueche, eatingRoutine()) }
    }

    /**
     * Ausruhen daheim, wie bisher auf dem ersten Sofa-Ablauf. **Nachts ([sleepAdmissible]) darf
     * die Policy stattdessen ins Bett** - die Schlafroutine gab es bisher nur als beantwortete
     * Erinnerung, obwohl sie genau das zeigt, was ein muedes Wesen um Mitternacht tut.
     * Tagsueber bleibt sie gesperrt: [RoutineStep.SleepUntilMorning] endet ausserhalb der Nacht
     * sofort, und ein Mittagsschlaf im Bett waere dort nur ein Hinlegen und Aufstehen.
     */
    private fun restBranch(sleepAdmissible: Boolean): Branch {
        val wohnzimmer = PlayScene.Place.LIVING
        val sofa = PlayRoutines.allFor(AnimationType.REST).mapIndexed { index, routine ->
            RoutineOption(
                AnimationType.REST, atPlace(wohnzimmer, routine), if (index == 0) 1.0 else 0.0,
                ActionKind.REST
            )
        }
        val bett = if (sleepAdmissible) {
            PlayRoutines.allFor(AnimationType.SLEEP).map { routine ->
                RoutineOption(
                    AnimationType.SLEEP,
                    atPlace(PlayScene.forTopic(AnimationType.SLEEP), routine),
                    0.0,
                    ActionKind.REST
                )
            }
        } else {
            emptyList()
        }
        return Branch(AnimationType.REST, merged(sofa + bett)) { atPlace(wohnzimmer, restRoutine()) }
    }

    /** Arbeit, wie bisher auf dem ersten Arbeitsweg; die anderen Wege stehen der Policy offen. */
    private fun workBranch(renderedPlace: PlayScene.Place): Branch {
        val options = PlayRoutines.allFor(AnimationType.WORK).mapIndexed { index, routine ->
            RoutineOption(
                AnimationType.WORK, workRoutine(renderedPlace, routine), if (index == 0) 1.0 else 0.0,
                ActionKind.WORK
            )
        }
        return Branch(AnimationType.WORK, merged(options)) { workRoutine(renderedPlace) }
    }

    /** Gleiche Ablaeufe aus verschiedenen Quellen zu einer Option zusammenfassen. */
    private fun merged(options: List<RoutineOption>): List<RoutineOption> =
        options.groupBy { it.routine }.map { (_, gleiche) ->
            gleiche.first().copy(probability = gleiche.sumOf { it.probability })
        }

    private fun completedActions(result: StepResult): List<ActionKind> = result.events
        .filter {
            it.kind == LivingEventKind.ACTION_DONE ||
                it.kind == LivingEventKind.SYMBOLS_SENT ||
                it.kind == LivingEventKind.SYMBOLS_RECEIVED
        }
        .mapNotNull { it.action }

    private fun shoppingRoutine(renderedPlace: PlayScene.Place): PlayRoutine {
        val shopping = PlayRoutines.allFor(AnimationType.DRINK).first { candidate ->
            candidate.steps.any {
                it is RoutineStep.GoToPlace && it.place == PlayScene.Place.SHOP
            }
        }
        if (renderedPlace != PlayScene.Place.SHOP) return shopping
        val atRack = shopping.steps.indexOfFirst {
            it is RoutineStep.GoTo && it.station == PlayScene.Station.RACK
        }
        return PlayRoutine(shopping.steps.drop(atRack.coerceAtLeast(0)))
    }

    private fun eatingRoutine(): PlayRoutine = eatingRoutines().first()

    private fun eatingRoutines(): List<PlayRoutine> = PlayRoutines.allFor(AnimationType.DRINK).filter {
        candidate -> candidate.steps.none {
            it is RoutineStep.GoToPlace && it.place == PlayScene.Place.SHOP
        }
    }

    private fun restRoutine(): PlayRoutine = PlayRoutines.allFor(AnimationType.REST).first()

    private fun closedSiteRoutine(place: PlayScene.Place): PlayRoutine = PlayRoutine(
        listOf(
            RoutineStep.GoToPlace(place),
            RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),
            RoutineStep.Linger(2_000L)
        )
    )

    private fun workRoutine(
        renderedPlace: PlayScene.Place,
        base: PlayRoutine = PlayRoutines.allFor(AnimationType.WORK).first()
    ): PlayRoutine {
        val steps = base.steps
        val lastWork = steps.indexOfLast {
            it is RoutineStep.Act && it.topic == AnimationType.WORK
        }
        val endExclusive = if (steps.getOrNull(lastWork + 1) is RoutineStep.Linger) {
            lastWork + 2
        } else {
            lastWork + 1
        }
        val start = if (renderedPlace == PlayScene.Place.WORK) {
            steps.indexOfFirst {
                it is RoutineStep.GoTo && it.station == PlayScene.Station.WORKPLACE
            }.coerceAtLeast(0)
        } else {
            0
        }
        return atPlace(
            if (renderedPlace == PlayScene.Place.WORK) PlayScene.Place.WORK else PlayScene.Place.STREET,
            PlayRoutine(steps.subList(start, endExclusive))
        )
    }

    private fun atPlace(place: PlayScene.Place, routine: PlayRoutine): PlayRoutine =
        if (routine.steps.firstOrNull() == RoutineStep.GoToPlace(place)) {
            routine
        } else {
            PlayRoutine(listOf(RoutineStep.GoToPlace(place)) + routine.steps)
        }

    private fun safeInterestTopic(goal: GoalKind?, candidate: AnimationType): AnimationType {
        val unsuitable = setOf(
            AnimationType.SLEEP,
            AnimationType.WORK,
            AnimationType.DRINK,
            AnimationType.MEDICINE
        )
        if (candidate !in unsuitable) return candidate
        return if (goal == GoalKind.DEVELOP) AnimationType.BOOK else AnimationType.MOVE
    }

    /**
     * Aus welcher Absicht eine erbetene Handlung kommt.
     *
     * Wichtig fuer die Erinnerung: Episoden und gelernter Geschmack haengen am ZIEL, nicht an
     * der Handlung. Liefe Medizin weiterhin unter "Vergnuegen", lernte das Wesen mit jeder
     * Tablette, dass Vergnuegen schoen ist - und traefe spaeter deshalb andere Entscheidungen.
     */
    private fun requestedGoal(topic: AnimationType): GoalKind = when (topic) {
        AnimationType.DRINK, AnimationType.WORK -> GoalKind.GET_FOOD
        AnimationType.REST, AnimationType.SLEEP -> GoalKind.REST
        // Fuersorge und Zur-Ruhe-Kommen sind Erholung, kein Zeitvertreib.
        AnimationType.MINDFULNESS, AnimationType.MEDICINE -> GoalKind.REST
        AnimationType.BOOK, AnimationType.FOCUS, AnimationType.CREATIVITY -> GoalKind.DEVELOP
        AnimationType.LOVE -> GoalKind.CONNECT_WITH
        AnimationType.MOVE, AnimationType.GENERAL -> GoalKind.HAVE_FUN
    }

    private fun requestedActions(
        topic: AnimationType,
        routine: PlayRoutine,
        world: WorldState
    ): List<Action> = when (topic) {
        AnimationType.WORK -> travelIfNeeded(LivingSite.WORKPLACE, world) +
            ActionCatalog[ActionKind.WORK]
        AnimationType.DRINK -> if (routine.steps.any {
                it is RoutineStep.GoToPlace && it.place == PlayScene.Place.SHOP
            }
        ) {
            travelIfNeeded(LivingSite.MARKET, world) +
                ActionCatalog[ActionKind.BUY_FOOD] +
                ActionCatalog.travelTo(LivingSite.HOME) +
                ActionCatalog[ActionKind.INSPECT_FOOD] +
                ActionCatalog[ActionKind.EAT]
        } else {
            travelIfNeeded(LivingSite.HOME, world) +
                ActionCatalog[ActionKind.INSPECT_FOOD] +
                ActionCatalog[ActionKind.EAT]
        }
        AnimationType.REST, AnimationType.SLEEP -> travelIfNeeded(LivingSite.HOME, world) +
            ActionCatalog[ActionKind.REST]

        // ---- Die acht, die frueher alle dasselbe waren (NT-072) ----
        //
        // Bis hierher endete dieser `when` mit `else -> PURSUE_INTEREST`. Acht der zwoelf
        // Reminder-Typen liefen dadurch durch **eine einzige** Handlung: Ein Buch zu lesen und
        // eine Tablette zu nehmen stillte Spass, Neugier und Wachstum in genau demselben Mass.
        // Sichtbar war das nicht - die Choreografie unterschied sich laengst -, aber der Tag
        // des Wesens bekam davon keine Struktur, und die Erinnerung lernte aus jeder Handlung
        // dasselbe.
        //
        // Kein `else` mehr, mit Absicht: Ein neuer Reminder-Typ soll hier auffallen, statt
        // still im Sammelposten zu verschwinden.
        AnimationType.BOOK -> listOf(ActionCatalog[ActionKind.READ])
        AnimationType.CREATIVITY -> listOf(ActionCatalog[ActionKind.CREATE])
        AnimationType.FOCUS -> listOf(ActionCatalog[ActionKind.CONCENTRATE])
        AnimationType.MINDFULNESS -> listOf(ActionCatalog[ActionKind.SETTLE])
        // Auch eine erbetene Bewegung faengt vor der Tuer an (NT-073).
        AnimationType.MOVE -> travelIfNeeded(LivingSite.OUTSIDE, world) +
            ActionCatalog[ActionKind.MOVE_BODY]
        AnimationType.MEDICINE -> listOf(ActionCatalog[ActionKind.TEND_SELF])

        // Ist wirklich jemand da, wird aus Zuwendung eine Begegnung - mit Beziehungswirkung.
        // Sonst bleibt es das stille Denken an jemanden.
        AnimationType.LOVE -> listOf(
            world.nearbyProfiles.sorted().firstOrNull()
                ?.let(ActionCatalog::inviteToPlay)
                ?: ActionCatalog[ActionKind.SHOW_AFFECTION]
        )

        AnimationType.GENERAL -> listOf(ActionCatalog[ActionKind.PURSUE_INTEREST])
    }

    /**
     * Welche Kernhandlung hinter einem sichtbaren Thema steckt.
     *
     * Nur die Freizeit- und Entwicklungsthemen stehen hier. Essen, Arbeit und Schlaf fehlen mit
     * Absicht: Die entscheidet der Kern aus der Lage, nicht die Themenwahl der Oberflaeche.
     */
    private fun interestActionFor(topic: AnimationType): ActionKind? = when (topic) {
        AnimationType.BOOK -> ActionKind.READ
        AnimationType.CREATIVITY -> ActionKind.CREATE
        AnimationType.FOCUS -> ActionKind.CONCENTRATE
        AnimationType.MINDFULNESS -> ActionKind.SETTLE
        AnimationType.MOVE -> ActionKind.MOVE_BODY
        AnimationType.LOVE -> ActionKind.SHOW_AFFECTION
        AnimationType.GENERAL -> ActionKind.PURSUE_INTEREST
        AnimationType.DRINK,
        AnimationType.WORK,
        AnimationType.REST,
        AnimationType.SLEEP,
        AnimationType.MEDICINE -> null
    }

    private fun travelIfNeeded(site: LivingSite, world: WorldState): List<Action> =
        if (world.site == site) emptyList() else listOf(ActionCatalog.travelTo(site))

    private const val WORK_OPENS = 6 * 60
    private const val WORK_CLOSES = 18 * 60
    private const val MARKET_OPENS = 7 * 60
    private const val MARKET_CLOSES = 22 * 60
}
