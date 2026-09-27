package com.notime.glyphsim.decision

import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.living.Needs
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.LivingRuntimeAdapter
import com.notime.glyphsim.matrix.PlayAmbientActivity
import com.notime.glyphsim.matrix.PlayScene
import java.io.File
import java.time.LocalTime

/** Gemeinsame Bausteine der Decision-Tests - ein Moment, das mitgelieferte Modell. */
object DecisionTestSupport {

    /** Das Modell, das die App ausliefert - relativ zu `app-sim`, wie Gradle und tests.sh laufen. */
    val modelFile = File("src/main/assets/" + DecisionPolicies.ASSET_NAME)

    val bundled: DecisionPolicies.Loaded by lazy { DecisionPolicies.load(modelFile.readBytes()) }

    /** Jeder Kandidat gleich - fuer Abdeckungslaeufe, die moeglichst viel sehen sollen. */
    object Uniform : DecisionPolicy {
        override val name = "uniform"
        override fun score(state: DecisionState, candidate: ActionCandidate) = 0f
    }

    fun state(
        species: AvatarSpecies = AvatarSpecies.WYRMLING,
        minuteOfDay: Int = 15 * 60,
        place: PlayScene.Place = PlayScene.Place.LIVING,
        needs: Map<NeedKind, Double> = emptyMap(),
        presence: Map<PlayScene.Place, Set<String>> = emptyMap(),
        history: DecisionHistory = DecisionHistory(),
        coins: Int = 4,
        portions: Int = 2,
        signals: TopicSignals? = null,
        minutesSinceOutdoors: Long? = null,
        minutesSinceMove: Long? = null
    ): DecisionState {
        val basis = LivingRuntimeAdapter.initialAgent(species.name, species)
        val agent = basis.copy(
            needs = Needs(NeedKind.entries.associateWith { needs[it] ?: basis.needs.pressure(it) })
        )
        val world = LivingRuntimeAdapter.synchroniseWorld(
            LivingRuntimeAdapter.initialWorld(2 * 1440 + minuteOfDay, place, coins, portions),
            place
        )
        return DecisionState(
            agent = agent,
            world = world,
            phase = PlayAmbientActivity.currentDayPhase(LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)),
            currentPlace = place,
            signals = signals ?: TopicSignals(
                plannedTopic = PlayAmbientActivity.plannedTopicFor(minuteOfDay / 60),
                signatureTopic = species.signatureTopic
            ),
            presence = presence,
            history = history,
            minutesSinceOutdoors = minutesSinceOutdoors,
            minutesSinceMove = minutesSinceMove
        )
    }

    /** Alle Kandidaten und Momente eines Laufs - fuer Pruefungen ueber viele Lagen. */
    fun sweep(
        policy: DecisionPolicy = Uniform,
        days: Int = 8,
        presence: Double = 0.5,
        seeds: Int = 2
    ): List<Pair<DecisionState, List<ActionCandidate>>> {
        val alle = mutableListOf<Pair<DecisionState, List<ActionCandidate>>>()
        for (species in AvatarSpecies.entries) {
            for (seed in 0 until seeds) {
                DecisionSimulation.run(
                    DecisionSimulation.Config(species, days, 77L + 13L * species.ordinal + seed, policy, extraPresenceChance = presence)
                ) { s, c, _ -> alle += s to c }
            }
        }
        return alle
    }
}
