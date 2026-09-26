package com.notime.glyphsim.decision

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.living.AgentState
import com.notime.glyphsim.living.WorldState
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.LivingPopulation
import com.notime.glyphsim.matrix.LivingRuntimeAdapter
import com.notime.glyphsim.matrix.PlayAmbientActivity
import com.notime.glyphsim.matrix.PlayRoutines
import com.notime.glyphsim.matrix.PlayScene
import com.notime.glyphsim.matrix.RoutineStep
import java.time.LocalTime
import kotlin.random.Random

/**
 * **Mehrere Tage Leben im Zeitraffer** - derselbe Entscheidungsweg wie in DockScreen, nur ohne
 * Bildschirm.
 *
 * Kern, Bevoelkerung, Kandidaten, Policy, Auswahl und `LivingRuntimeAdapter.prepare` sind die
 * echten. Nachgebaut ist nur, was DockScreen um den Entscheidungspunkt herum mitfuehrt (Ort,
 * zuletzt gezeigte Themen, Bewegungs- und Draussen-Uhr, Verlauf). Die Zeit ist die des Kerns: Jede
 * Handlung dauert so lange, wie ihr `ActionOutcome` sagt.
 *
 * Dient drei Zwecken: Datensatz fuer das Training, Verhaltenstests ueber mehrere Tage und der
 * Vergleich bisherige Logik gegen gelernte Policy.
 */
object DecisionSimulation {

    data class Config(
        val species: AvatarSpecies,
        val days: Int,
        val seed: Long,
        val policy: DecisionPolicy,
        val temperature: Double = DecisionSelector.DEFAULT_TEMPERATURE,
        /** Zusaetzliche zufaellige Anwesenheit an Spielorten - nur fuer die Abdeckung im Datensatz. */
        val extraPresenceChance: Double = 0.0,
        val startMinute: Int = 7 * 60
    )

    data class Record(
        val day: Int,
        val minuteOfDay: Int,
        val key: String,
        val family: String,
        val topic: AnimationType,
        val place: PlayScene.Place,
        val group: Boolean,
        val social: Boolean,
        val category: String,
        /** Diese Familie war schon einmal da, aber nicht in den letzten zwoelf Stunden. */
        val resumed: Boolean,
        val placeChanged: Boolean,
        val baselineProbability: Double,
        val fellBack: Boolean,
        val night: Boolean,
        val outdoor: Boolean,
        /** Druck des Beduerfnisses hinter dem verfolgten Ziel - faellt er, wird Dringendes uebergangen. */
        val goalPressure: Double,
        /** Ob das Ziel dasselbe ist, das der Kern von sich aus gewaehlt haette. */
        val coreGoal: Boolean
    )

    data class Run(val records: List<Record>, val finalAgent: AgentState, val finalWorld: WorldState)

    fun run(
        config: Config,
        observer: ((DecisionState, List<ActionCandidate>, Decision) -> Unit)? = null
    ): Run {
        val random = Random(config.seed)
        val presenceRandom = Random(config.seed * 31 + 7)
        val profileId = config.species.name
        var agent = LivingRuntimeAdapter.initialAgent(profileId, config.species)
        var place = PlayScene.Place.LIVING
        var world = LivingRuntimeAdapter.initialWorld(config.startMinute, place, coins = 4, portions = 2)
        var residents = LivingPopulation.initial(config.startMinute)
        var history = DecisionHistory()
        var lastMove: Long? = null
        var lastOutdoors: Long? = null
        var stayedRounds = 0
        var justPlayed: AnimationType? = null
        val recentTopics = ArrayDeque<AnimationType>()
        val recentSpecials = ArrayDeque<PlayRoutines.SpecialActivity>()
        val familySeen = HashMap<String, Long>()
        val records = mutableListOf<Record>()
        val end = config.startMinute + config.days * WorldState.MINUTES_PER_DAY

        while (world.absoluteMinute < end) {
            val now = world.absoluteMinute.toLong()
            residents = LivingPopulation.advance(residents, world.absoluteMinute)
            val presence = LivingPopulation.snapshot(residents)
                .filter { it.publiclyPresent && it.place != null }
                .groupBy({ it.place!! }, { it.profileId })
                .mapValues { it.value.toSet() }
                .toMutableMap()
            if (config.extraPresenceChance > 0 && presenceRandom.nextDouble() < config.extraPresenceChance) {
                val ort = listOf(PlayScene.Place.PARK, PlayScene.Place.SPORT, PlayScene.Place.MEADOW, place)
                    .random(presenceRandom)
                val wer = com.notime.glyphsim.matrix.LivingResidents.all.random(presenceRandom).profileId
                presence[ort] = (presence[ort] ?: emptySet()) + wer
            }
            val phase = PlayAmbientActivity.currentDayPhase(
                LocalTime.of(world.minuteOfDay / 60, world.minuteOfDay % 60)
            )
            val signals = TopicSignals(
                stayAt = place.takeIf { stayedRounds < PlayAmbientActivity.MAX_STAY_ROUNDS },
                plannedTopic = PlayAmbientActivity.plannedTopicFor(world.minuteOfDay / 60),
                justPlayed = justPlayed,
                signatureTopic = config.species.signatureTopic,
                recentTopics = recentTopics.toList(),
                movementUrge = PlayAmbientActivity.movementUrge(
                    lastMove?.let { now - it },
                    likesMovement = config.species.signatureTopic == AnimationType.MOVE
                )
            )
            val synced = LivingRuntimeAdapter.synchroniseWorld(world, place)
            val state = DecisionState(
                agent = agent,
                world = synced,
                phase = phase,
                currentPlace = place,
                signals = signals,
                presence = presence,
                history = history,
                nowMinute = now,
                minutesSinceMove = lastMove?.let { now - it },
                minutesSinceOutdoors = lastOutdoors?.let { now - it },
                recentSpecials = recentSpecials.toList()
            )
            val candidates = DecisionCandidates.generate(state)
            val decision = DecisionEngine.decide(
                state, candidates, config.policy, random, config.temperature
            )
            val prepared = LivingRuntimeAdapter.prepare(
                agent = agent,
                world = synced,
                renderedPlace = place,
                interestTopic = decision?.candidate?.interestTopic ?: AnimationType.GENERAL,
                recentSpecials = recentSpecials.toList(),
                random = random,
                preferredRoutine = decision?.candidate?.routine,
                sleepAdmissible = phase == PlayAmbientActivity.DayPhase.NIGHT,
                chosenGoal = decision?.candidate?.goal
            )
            val vorher = world.absoluteMinute
            agent = prepared.result.agent
            world = prepared.result.world
            if (world.absoluteMinute <= vorher) {
                world = world.advanced(10)
                agent = agent.copy(needs = agent.needs.advanced(10, agent.personality))
            }
            val topic = prepared.topic
            val routine = prepared.routine
            if (decision == null || topic == null || routine == null) continue
            observer?.invoke(state, candidates, decision)

            val candidate = decision.candidate
            val passt = routine == candidate.routine
            val sichtbar = if (passt) candidate.visibleRoutine else routine
            val traits = ActionTraits.of(candidate)
            val zuletzt = familySeen[candidate.family]
            records += Record(
                day = (vorher - config.startMinute) / WorldState.MINUTES_PER_DAY,
                minuteOfDay = vorher % WorldState.MINUTES_PER_DAY,
                key = candidate.key,
                family = candidate.family,
                topic = topic,
                place = candidate.place,
                group = candidate.groupGame != null,
                social = traits.social || traits.group,
                category = category(traits),
                resumed = zuletzt != null && now - zuletzt >= DecisionFeatures.RESUME_AFTER_MINUTES,
                placeChanged = candidate.place != place,
                baselineProbability = candidate.baselineProbability,
                fellBack = decision.fellBack,
                night = phase == PlayAmbientActivity.DayPhase.NIGHT,
                outdoor = sichtbar.steps.any { it is RoutineStep.GoToPlace && PlayScene.isOutdoors(it.place) },
                goalPressure = candidate.goal?.let { state.agent.needs.pressure(it.drivenBy) } ?: 0.0,
                coreGoal = candidate.goal == state.goal
            )
            familySeen[candidate.family] = now
            val draussen = sichtbar.steps.any { it is RoutineStep.GoToPlace && PlayScene.isOutdoors(it.place) }
            history = history.recorded(
                DecisionHistory.Entry(now, candidate.key, candidate.family, topic.name, candidate.partners, draussen)
            )
            if (topic == AnimationType.MOVE) lastMove = now
            if (sichtbar.steps.any { it is RoutineStep.GoToPlace && PlayScene.isOutdoors(it.place) }) {
                lastOutdoors = world.absoluteMinute.toLong()
            }
            val neuerOrt = sichtbar.steps.lastOrNull { it is RoutineStep.GoToPlace }
                ?.let { (it as RoutineStep.GoToPlace).place } ?: place
            stayedRounds = if (neuerOrt == place) stayedRounds + 1 else 0
            place = neuerOrt
            justPlayed = topic
            recentTopics.addFirst(topic)
            while (recentTopics.size > 4) recentTopics.removeLast()
            PlayRoutines.specialOf(sichtbar)?.let { s ->
                recentSpecials.addFirst(s)
                while (recentSpecials.size > 4) recentSpecials.removeLast()
            }
        }
        return Run(records, agent, world)
    }

    /** Grobe sichtbare Kategorie - fuer "wie viele verschiedene Arten von Tun am Tag". */
    fun category(t: ActionTraits): String = when {
        t.group -> "group"
        t.sleep -> "sleep"
        t.eat || t.drink -> "food"
        t.work -> "work"
        t.sport -> "sport"
        t.explore -> "explore"
        t.physical -> "movement"
        t.music -> "music"
        t.creative -> "creative"
        t.game -> "game"
        t.learn || t.mental -> "mind"
        t.social -> "social"
        t.rest -> "rest"
        t.calm -> "calm"
        else -> "other"
    }

    /** Die Kennzahlen des Vergleichs. */
    data class Metrics(
        val decisions: Int,
        val distinctPerDay: Double,
        val categoriesPerDay: Double,
        val placeChangesPerDay: Double,
        val repeatRate: Double,
        val socialShare: Double,
        val groupShare: Double,
        val resumptionShare: Double,
        val distinctTotal: Int,
        val entropyBits: Double,
        val zeroBaselineShare: Double,
        val fallbackShare: Double,
        val nightOutdoorShare: Double,
        val meanGoalPressure: Double,
        val coreGoalShare: Double
    )

    fun metrics(records: List<Record>): Metrics {
        val tage = records.groupBy { it.day }.values
        val n = records.size.coerceAtLeast(1)
        val wiederholt = records.zipWithNext().count { (a, b) -> a.key == b.key }
        val haeufig = records.groupingBy { it.key }.eachCount()
        val entropie = haeufig.values.sumOf { c ->
            val p = c.toDouble() / n
            -p * kotlin.math.ln(p) / kotlin.math.ln(2.0)
        }
        return Metrics(
            decisions = records.size,
            distinctPerDay = tage.map { t -> t.map { it.key }.toSet().size }.average(),
            categoriesPerDay = tage.map { t -> t.map { it.category }.toSet().size }.average(),
            placeChangesPerDay = tage.map { t -> t.count { it.placeChanged } }.average(),
            repeatRate = wiederholt.toDouble() / (records.size - 1).coerceAtLeast(1),
            socialShare = records.count { it.social }.toDouble() / n,
            groupShare = records.count { it.group }.toDouble() / n,
            resumptionShare = records.count { it.resumed }.toDouble() / n,
            distinctTotal = haeufig.size,
            entropyBits = entropie,
            zeroBaselineShare = records.count { it.baselineProbability <= 0.0 }.toDouble() / n,
            fallbackShare = records.count { it.fellBack }.toDouble() / n,
            nightOutdoorShare = records.count { it.night && it.outdoor }.toDouble() /
                records.count { it.night }.coerceAtLeast(1),
            meanGoalPressure = records.map { it.goalPressure }.average(),
            coreGoalShare = records.count { it.coreGoal }.toDouble() / n
        )
    }
}
