package com.notime.glyphsim.decision

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.LivingSimulation
import com.notime.glyphsim.living.UtilitySelector
import com.notime.glyphsim.matrix.LivingRuntimeAdapter
import com.notime.glyphsim.matrix.PlayAmbientActivity
import com.notime.glyphsim.matrix.PlayGroupGame
import com.notime.glyphsim.matrix.PlayRoutine
import com.notime.glyphsim.matrix.PlayRoutines
import com.notime.glyphsim.matrix.PlayScene
import com.notime.glyphsim.matrix.RoutineStep

/**
 * **Welche sichtbaren Ablaeufe jetzt ueberhaupt in Frage kommen** - die Gueltigkeitsschicht vor
 * der Policy.
 *
 * Keine gepflegte Liste: Die Kandidaten entstehen aus denselben Quellen, aus denen die Welt ihre
 * Ablaeufe ohnehin bezieht.
 *
 * 1. Die bisherige Themengewichtung ([PlayAmbientActivity.topicWeights]) liefert die Themen, die
 *    zu dieser Stunde zulaessig sind - nachts nur Schlaf, nie Medizin, bei laufendem Aufenthalt
 *    draussen nur Aussenthemen, bei einem Impuls genau dessen Thema.
 * 2. Fuer jedes Thema rechnet [LivingRuntimeAdapter.options] den echten naechsten Kernschritt und
 *    nennt jeden Ablauf, den dieser Schritt zeigen darf. Oeffnungszeiten, Vorrat, Geld und das Ziel
 *    des Kerns wirken dort und nirgends sonst.
 * 3. Ein Bewegungsablauf an einem Spielort, an dem wirklich noch jemand ist, wird zum
 *    Gruppenspiel ([PlayGroupGame]) - mit allen Spielarten, die dort moeglich sind. Den
 *    Einzelsport daneben gibt es dann nicht: Der Vorrang des gemeinsamen Spiels war ausdruecklich
 *    gewuenscht und bleibt eine Regel, keine Gewichtung.
 *
 * Nebenbei entsteht fuer jeden Kandidaten die Wahrscheinlichkeit, mit der die bisherige Wahl ihn
 * gezogen haette. Sie ist Rueckfall ([ExistingUtilityPolicy]) und Merkmal zugleich.
 */
object DecisionCandidates {

    /**
     * **Zwischen welchen Zielen gewaehlt werden darf.**
     *
     * Der Kern hat ein Lieblingsziel ([DecisionState.goal]). Daneben stehen die Ziele, deren
     * Bewertung knapp dahinter liegt ([GOAL_MARGIN]) und die Druck und einen Weg haben
     * ([UtilitySelector.eligible]) - dort ist die Wahl ohnehin eine Abwaegung, und dort darf die
     * Policy abwaegen. Drei harte Grenzen:
     *
     * - Ein laufender Plan wird nie verlassen - wer auf dem Weg zum Laden ist, geht zum Laden.
     * - Draengen Hunger oder Muedigkeit ([URGENT_PRESSURE]), gibt es nur dieses Ziel.
     * - Will der Kern nichts, bleibt es beim Nichtstun. Ein Wesen darf auch einmal nichts vorhaben.
     */
    fun admissibleGoals(state: DecisionState): List<GoalKind> {
        val best = state.goal ?: return emptyList()
        if (LivingSimulation.keepsGoal(state.agent)) return listOf(best)
        if (best in URGENT_GOALS && state.agent.needs.pressure(best.drivenBy) >= URGENT_PRESSURE) {
            return listOf(best)
        }
        val bestTotal = state.goalRanking.first { it.goal == best }.total
        val andere = UtilitySelector.eligible(state.agent, state.world, state.goalInfluence)
            .filter { it.goal != best && it.total >= bestTotal - GOAL_MARGIN }
            .map { it.goal }
        return listOf(best) + andere
    }

    /** Wie knapp ein Ziel hinter dem Lieblingsziel liegen muss, um waehlbar zu sein. */
    const val GOAL_MARGIN = 0.25

    /** Ab diesem Druck duldet ein Grundbeduerfnis keine Alternative. */
    const val URGENT_PRESSURE = 0.7

    val URGENT_GOALS: Set<GoalKind> = setOf(GoalKind.GET_FOOD, GoalKind.REST)

    fun generate(state: DecisionState): List<ActionCandidate> {
        val ziele = admissibleGoals(state)
        if (ziele.isEmpty()) return emptyList()
        val weights = state.topicWeights
        val total = weights.values.sum()
        // Dieselbe Rueckfallregel wie PlayAmbientActivity.pickWeighted: ohne Gewicht GENERAL.
        val themen: Map<AnimationType, Double> = if (total <= 0) {
            mapOf(AnimationType.GENERAL to 1.0)
        } else {
            weights.filterValues { it > 0 }.mapValues { it.value.toDouble() / total }
        }
        val night = state.phase == PlayAmbientActivity.DayPhase.NIGHT
        val bestesZiel = ziele.first()
        val bewertung = state.goalRanking.associateBy { it.goal }
        val besteSumme = bewertung.getValue(bestesZiel).total

        val gesammelt = LinkedHashMap<Pair<PlayRoutine, GoalKind>, Sammlung>()
        for (ziel in ziele) {
            // Nur das Lieblingsziel hat eine bisherige Wahrscheinlichkeit - die alte Kette hat nie
            // ein anderes gewaehlt.
            val zielGewicht = if (ziel == bestesZiel) 1.0 else 0.0
            for ((thema, pThema) in themen) {
                val optionen = LivingRuntimeAdapter.options(
                    agent = state.agent,
                    world = state.world,
                    renderedPlace = state.currentPlace,
                    interestTopic = thema,
                    footballTrickLearned = state.footballTrickLearned,
                    recentSpecials = state.recentSpecials,
                    nearbyProfiles = state.nearbyProfiles,
                    goalInfluence = state.goalInfluence,
                    sleepAdmissible = night,
                    chosenGoal = ziel
                )
                for (option in optionen) {
                    val s = gesammelt.getOrPut(option.routine to ziel) { Sammlung(option, ziel, thema) }
                    s.add(zielGewicht * pThema * option.probability, thema)
                }
            }
        }

        val kandidaten = mutableListOf<ActionCandidate>()
        val spiele = LinkedHashMap<Pair<PlayRoutine, GoalKind>, Spiel>()
        for (s in gesammelt.values) {
            val option = s.option
            if (option.topic == AnimationType.MOVE) {
                // Derselbe Ort wie bisher in DockScreen: der erste Ortswechsel des Ablaufs.
                val spielort = firstPlace(option.routine) ?: PlayScene.forTopic(AnimationType.MOVE)
                val andere = state.othersAt(spielort)
                if (PlayGroupGame.shouldPlay(
                        topicIsMove = true,
                        place = spielort,
                        othersPresent = andere.isNotEmpty(),
                        night = night
                    )
                ) {
                    for ((art, pArt) in kindDistribution(spielort, PlayRoutines.specialOf(option.routine))) {
                        val sichtbar = PlayGroupGame.routine(art, firstPlace(option.routine))
                        val spiel = spiele.getOrPut(sichtbar to s.goal) { Spiel(art, spielort, sichtbar, andere) }
                        spiel.add(s, s.probability * pArt)
                    }
                    continue
                }
            }
            val ort = mainPlace(option.routine, state.currentPlace)
            kandidaten += ActionCandidate(
                key = keyOf(option.topic, option.routine),
                family = familyOf(option.topic, option.routine, null),
                topic = option.topic,
                interestTopic = s.interest,
                routine = option.routine,
                visibleRoutine = option.routine,
                groupGame = null,
                place = ort,
                coreAction = option.coreAction,
                goal = s.goal,
                goalUtility = bewertung.getValue(s.goal).total,
                goalGap = (besteSumme - bewertung.getValue(s.goal).total).coerceAtLeast(0.0),
                baselineProbability = s.probability,
                partners = state.othersAt(ort)
            )
        }
        for (spiel in spiele.values) {
            val basis = spiel.base ?: continue
            kandidaten += ActionCandidate(
                key = "group:${spiel.kind.name}@${spiel.place.name}",
                family = familyOf(AnimationType.MOVE, spiel.visible, spiel.kind),
                topic = AnimationType.MOVE,
                interestTopic = basis.interest,
                routine = basis.option.routine,
                visibleRoutine = spiel.visible,
                groupGame = spiel.kind,
                place = spiel.place,
                coreAction = basis.option.coreAction,
                goal = basis.goal,
                goalUtility = bewertung.getValue(basis.goal).total,
                goalGap = (besteSumme - bewertung.getValue(basis.goal).total).coerceAtLeast(0.0),
                baselineProbability = spiel.probability,
                partners = spiel.partners
            )
        }
        return kandidaten
    }

    /**
     * Die Spielarten eines Orts, wie [PlayGroupGame.kindFor] sie aus seinem Wurf `0..999` zieht -
     * ausgezaehlt statt geschaetzt, damit der Rueckfall exakt die bisherige Wahl trifft.
     */
    fun kindDistribution(
        place: PlayScene.Place,
        special: PlayRoutines.SpecialActivity?
    ): Map<PlayGroupGame.Kind, Double> {
        val zaehler = LinkedHashMap<PlayGroupGame.Kind, Int>()
        for (roll in 0 until KIND_ROLLS) {
            val art = PlayGroupGame.kindFor(place, special, roll)
            zaehler[art] = (zaehler[art] ?: 0) + 1
        }
        return zaehler.mapValues { it.value.toDouble() / KIND_ROLLS }
    }

    /**
     * Wer wo ist - wirklich anwesende Einwohner an ihrem Ort, Gaeste am aktuellen Ort. Dieselbe
     * Auskunft, nach der DockScreen bisher entschied, ob aus Bewegung ein Gruppenspiel wird.
     */
    fun presenceByPlace(
        snapshots: List<com.notime.glyphsim.matrix.ResidentSnapshot>,
        currentPlace: PlayScene.Place,
        guests: Set<String>
    ): Map<PlayScene.Place, Set<String>> {
        val karte = LinkedHashMap<PlayScene.Place, Set<String>>()
        for (s in snapshots) {
            val ort = s.place ?: continue
            if (!s.publiclyPresent) continue
            karte[ort] = (karte[ort] ?: emptySet()) + s.profileId
        }
        if (guests.isNotEmpty()) karte[currentPlace] = (karte[currentPlace] ?: emptySet()) + guests
        return karte
    }

    /** Die Obergrenze des Wurfs, mit dem DockScreen die Spielart zieht. */
    const val KIND_ROLLS = 1_000

    /**
     * **Stabile Kennung eines Ablaufs** - aus seinem Inhalt, nicht aus einer Listenposition.
     *
     * Ein neuer Ablauf in [PlayRoutines.allFor] verschiebt deshalb keine Kennung der anderen, und
     * der Verlauf erkennt Wiederholungen auch nach einem Neustart.
     */
    fun keyOf(topic: AnimationType, routine: PlayRoutine): String =
        "${topic.name}:" + Integer.toHexString(routine.steps.joinToString("|").hashCode())

    /** Gruppenspielart, sonst Sonderaktivitaet, sonst Thema - so grob, wie Erinnerung sein soll. */
    fun familyOf(topic: AnimationType, routine: PlayRoutine, group: PlayGroupGame.Kind?): String =
        when {
            group != null -> "group:${group.name}"
            PlayRoutines.specialOf(routine) != null -> "special:${PlayRoutines.specialOf(routine)!!.name}"
            routine.steps.any { it is RoutineStep.Music } -> "music"
            routine.steps.any { it is RoutineStep.Painting } -> "painting"
            routine.steps.any { it is RoutineStep.Switch && it.device == PlayScene.Station.ARCADE } -> "arcade"
            routine.steps.any { it is RoutineStep.SleepUntilMorning } -> "sleep"
            else -> "topic:${topic.name}"
        }

    fun firstPlace(routine: PlayRoutine): PlayScene.Place? =
        routine.steps.firstNotNullOfOrNull { (it as? RoutineStep.GoToPlace)?.place }

    /**
     * Wo das Eigentliche geschieht: der letzte Ortswechsel vor dem ersten tuenden Schritt. Beim
     * Arbeitsweg ist das der Arbeitsplatz und nicht die Strasse davor.
     */
    fun mainPlace(routine: PlayRoutine, current: PlayScene.Place): PlayScene.Place {
        var ort: PlayScene.Place? = null
        for (step in routine.steps) {
            if (step is RoutineStep.GoToPlace) ort = step.place
            if (ActionTraits.isActivity(step)) return ort ?: current
        }
        return firstPlace(routine) ?: current
    }

    private class Sammlung(
        val option: LivingRuntimeAdapter.RoutineOption,
        val goal: GoalKind,
        var interest: AnimationType
    ) {
        var probability = 0.0
        private var staerkster = -1.0
        fun add(p: Double, thema: AnimationType) {
            probability += p
            if (p > staerkster) {
                staerkster = p
                interest = thema
            }
        }
    }

    private class Spiel(
        val kind: PlayGroupGame.Kind,
        val place: PlayScene.Place,
        val visible: PlayRoutine,
        val partners: Set<String>
    ) {
        var probability = 0.0
        var base: Sammlung? = null
        private var staerkste = -1.0
        fun add(s: Sammlung, p: Double) {
            probability += p
            if (p > staerkste || base == null) {
                staerkste = p
                base = s
            }
        }
    }
}
