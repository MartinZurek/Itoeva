package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.living.ActionCatalog
import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.living.AgentState
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.LivingEventKind
import com.notime.glyphsim.living.LivingSimulation
import com.notime.glyphsim.living.LivingSite
import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.living.Needs
import com.notime.glyphsim.living.Personality
import com.notime.glyphsim.living.StepResult
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

    /** Die sechzehn sichtbaren Orte auf die vier entscheidungsrelevanten Orte abbilden. */
    fun siteFor(place: PlayScene.Place): LivingSite = when (place) {
        PlayScene.Place.WORK -> LivingSite.WORKPLACE
        PlayScene.Place.SHOP -> LivingSite.MARKET
        PlayScene.Place.PARK,
        PlayScene.Place.SPORT,
        PlayScene.Place.POND,
        PlayScene.Place.STREET,
        PlayScene.Place.FOREST,
        PlayScene.Place.MEADOW,
        PlayScene.Place.CITY -> LivingSite.OUTSIDE
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
     */
    fun prepare(
        agent: AgentState,
        world: WorldState,
        renderedPlace: PlayScene.Place,
        interestTopic: AnimationType,
        footballTrickLearned: Boolean = false,
        recentSpecials: List<PlayRoutines.SpecialActivity> = emptyList(),
        random: Random = Random.Default,
        nearbyProfiles: Set<String> = emptySet()
    ): PreparedLivingRoutine {
        val startWorld = synchroniseWorld(world, renderedPlace, nearbyProfiles)
        var result = LivingSimulation.step(agent, startWorld)
        val firstAction = completedActions(result).firstOrNull()
            ?: return PreparedLivingRoutine(result, null, null, emptyList())

        val topic: AnimationType
        val routine: PlayRoutine
        when (firstAction) {
            ActionKind.TRAVEL -> when (result.world.site) {
                LivingSite.WORKPLACE -> {
                    result = advanceExpected(result, listOf(ActionKind.WORK))
                    topic = AnimationType.WORK
                    routine = workRoutine(renderedPlace)
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
                    topic = AnimationType.DRINK
                    routine = shoppingRoutine(renderedPlace)
                }
                LivingSite.HOME -> {
                    if (result.agent.plan?.next?.kind == ActionKind.REST) {
                        result = advanceExpected(result, listOf(ActionKind.REST))
                        topic = AnimationType.REST
                        routine = atPlace(PlayScene.Place.LIVING, restRoutine())
                    } else {
                        result = advanceExpected(
                            result,
                            listOf(ActionKind.INSPECT_FOOD, ActionKind.EAT)
                        )
                        topic = AnimationType.DRINK
                        routine = atPlace(PlayScene.Place.KITCHEN, eatingRoutine())
                    }
                }
                LivingSite.OUTSIDE -> {
                    topic = AnimationType.MOVE
                    routine = PlayRoutine(listOf(RoutineStep.GoToPlace(PlayScene.Place.STREET)))
                }
            }
            ActionKind.WORK -> {
                topic = AnimationType.WORK
                routine = workRoutine(renderedPlace)
            }
            ActionKind.BUY_FOOD -> {
                result = advanceExpected(
                    result,
                    listOf(ActionKind.TRAVEL, ActionKind.INSPECT_FOOD, ActionKind.EAT)
                )
                topic = AnimationType.DRINK
                routine = shoppingRoutine(renderedPlace)
            }
            ActionKind.INSPECT_FOOD -> {
                result = advanceExpected(result, listOf(ActionKind.EAT))
                topic = AnimationType.DRINK
                routine = atPlace(PlayScene.Place.KITCHEN, eatingRoutine())
            }
            ActionKind.EAT -> {
                topic = AnimationType.DRINK
                routine = atPlace(PlayScene.Place.KITCHEN, eatingRoutine())
            }
            ActionKind.REST -> {
                topic = AnimationType.REST
                routine = atPlace(PlayScene.Place.LIVING, restRoutine())
            }
            ActionKind.PURSUE_INTEREST -> {
                topic = safeInterestTopic(result.agent.goal, interestTopic)
                routine = atPlace(
                    PlayScene.forTopic(topic),
                    PlayRoutines.forTopic(
                        topic = topic,
                        footballTrickLearned = footballTrickLearned,
                        recentSpecials = recentSpecials,
                        random = random
                    )
                )
            }
            ActionKind.INVITE_TO_PLAY -> {
                topic = AnimationType.LOVE
                routine = atPlace(
                    PlayScene.Place.LIVING,
                    PlayRoutines.forTopic(AnimationType.LOVE, random = random)
                )
            }
            ActionKind.RESPOND_TO_INVITE,
            ActionKind.RECEIVE_RESPONSE -> {
                topic = AnimationType.GENERAL
                routine = atPlace(
                    PlayScene.Place.LIVING,
                    PlayRoutines.forTopic(AnimationType.GENERAL, random = random)
                )
            }
        }
        return PreparedLivingRoutine(result, topic, routine, completedActions(result))
    }

    private fun advanceExpected(start: StepResult, expected: List<ActionKind>): StepResult {
        var result = start
        for (kind in expected) {
            if (result.agent.plan?.next?.kind != kind) break
            val next = LivingSimulation.step(result.agent, result.world)
            if (completedActions(next).firstOrNull() != kind) break
            result = next.copy(
                events = result.events + next.events,
                messages = result.messages + next.messages
            )
        }
        return result
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

    private fun eatingRoutine(): PlayRoutine = PlayRoutines.allFor(AnimationType.DRINK).first {
        candidate -> candidate.steps.none {
            it is RoutineStep.GoToPlace && it.place == PlayScene.Place.SHOP
        }
    }

    private fun restRoutine(): PlayRoutine = PlayRoutines.allFor(AnimationType.REST).first()

    private fun workRoutine(renderedPlace: PlayScene.Place): PlayRoutine {
        val steps = PlayRoutines.allFor(AnimationType.WORK).first().steps
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

    private const val WORK_OPENS = 6 * 60
    private const val WORK_CLOSES = 18 * 60
    private const val MARKET_OPENS = 7 * 60
    private const val MARKET_CLOSES = 22 * 60
}
