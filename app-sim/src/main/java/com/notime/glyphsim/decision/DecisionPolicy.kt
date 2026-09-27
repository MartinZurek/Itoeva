package com.notime.glyphsim.decision

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.living.AgentState
import com.notime.glyphsim.living.GoalInfluence
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.WorldState
import com.notime.glyphsim.matrix.PlayAmbientActivity
import com.notime.glyphsim.matrix.PlayGroupGame
import com.notime.glyphsim.matrix.PlayRoutine
import com.notime.glyphsim.matrix.PlayRoutines
import com.notime.glyphsim.matrix.PlayScene
import kotlin.math.exp
import kotlin.random.Random

/**
 * **Die Decision Policy: welcher der erlaubten sichtbaren Ablaeufe jetzt am besten passt.**
 *
 * ## Wo sie sitzt, und wo nicht
 *
 * Der Living-Agent-Kern bleibt, was er ist: Er waehlt aus Beduerfnissen, Persoenlichkeit, Kosten
 * und Erinnerung das ZIEL, plant den Weg und prueft jede Voraussetzung unmittelbar vor der
 * Ausfuehrung. Daran aendert die Policy nichts. Sie sitzt eine Ebene tiefer - dort, wo bisher zwei
 * Wuerfel entschieden, WIE eine Absicht aussieht: [PlayAmbientActivity.nextTopic] zog das Thema,
 * [PlayRoutines.forTopic] den Ablauf, und [PlayGroupGame] machte daraus gegebenenfalls ein Spiel.
 *
 * ```
 * Beduerfnisse, Persoenlichkeit, Beziehungen, Erinnerung, Welt, Ort, Zeit, Verlauf
 *         -> zulaessige Kandidaten (DecisionCandidates, aus dem Kern abgeleitet)
 *         -> DecisionPolicy.score je Kandidat
 *         -> DecisionSelector (Softmax mit Temperatur, gesetzter Zufall)
 *         -> LivingRuntimeAdapter.prepare(preferredRoutine = ...)  (bestehender Weg)
 *         -> sichtbarer Ablauf -> ActionOutcome -> Zustand, Erinnerung, Beziehung
 * ```
 *
 * **Ein Modell kann nur zwischen Kandidaten waehlen, die die Welt ohnehin erlaubt.** Oeffnungs-
 * zeiten, Vorrat, Geld, Nachtruhe, der Ausschluss von Medizin, die Mindestdauer draussen und der
 * Vorrang des Gruppenspiels wirken bei der Erzeugung der Kandidaten - nicht bei der Bewertung.
 * Selbst ein voellig falsches Modell kann deshalb nur eine unguenstige, nie eine unzulaessige
 * Wahl treffen.
 *
 * ## Warum eine Bewertung je Kandidat und keine feste Ausgabeliste
 *
 * Jeder Kandidat wird ueber strukturierte Merkmale beschrieben ([DecisionFeatures]), die aus
 * seinem Ablauf, seiner Kernwirkung und seinem Ort ABGELEITET werden. Ein neuer Ablauf in
 * [PlayRoutines.allFor], ein neues Gruppenspiel oder ein neuer Ort wird dadurch automatisch
 * Kandidat und bekommt eine Bewertung, ohne dass sich an der Form des Modells etwas aendert.
 */
interface DecisionPolicy {

    /** Kurzname fuer Protokoll und Vergleich. */
    val name: String

    /** Bewertung eines Kandidaten - hoeher heisst passender. Nur relativ zu den anderen sinnvoll. */
    fun score(state: DecisionState, candidate: ActionCandidate): Float

    /** Alle Kandidaten eines Moments; eine Policy darf das gebuendelt schneller rechnen. */
    fun scoreAll(state: DecisionState, candidates: List<ActionCandidate>): FloatArray =
        FloatArray(candidates.size) { score(state, candidates[it]) }
}

/**
 * Die Eingaben von [PlayAmbientActivity.nextTopic] als ein Wert - dieselben neun Signale, damit
 * die bisherige Themenwahl unveraendert als Rueckfall und als Merkmal zur Verfuegung steht.
 */
data class TopicSignals(
    val boostedTopics: Set<AnimationType> = emptySet(),
    val stayAt: PlayScene.Place? = null,
    val leaning: Set<AnimationType> = emptySet(),
    val plannedTopic: AnimationType? = null,
    val justPlayed: AnimationType? = null,
    val signatureTopic: AnimationType? = null,
    val recentTopics: List<AnimationType> = emptyList(),
    val holdOutdoors: Boolean = false,
    val afterglow: Map<AnimationType, Int> = emptyMap(),
    val movementUrge: Int = 0
) {
    fun weights(phase: PlayAmbientActivity.DayPhase): Map<AnimationType, Int> =
        PlayAmbientActivity.topicWeights(
            phase, boostedTopics, stayAt, leaning, plannedTopic, justPlayed, signatureTopic,
            recentTopics, holdOutdoors, afterglow, movementUrge
        )

    /** Der bisherige Wurf aus denselben Gewichten ([PlayAmbientActivity.nextTopic]). */
    fun draw(phase: PlayAmbientActivity.DayPhase, random: Random = Random.Default): AnimationType =
        PlayAmbientActivity.nextTopic(
            phase, boostedTopics, stayAt, leaning, plannedTopic, justPlayed, signatureTopic,
            recentTopics, holdOutdoors, afterglow, movementUrge, random
        )
}

/**
 * **Was die Policy ueber diesen Moment weiss.** Nur gelesen, nie zurueckgeschrieben.
 *
 * [world] ist bereits mit dem sichtbaren Ort synchronisiert. [nowMinute] ist die Uhr des
 * Verlaufs ([history]) - in der App die Weltzeit von `PlayTimeLapse`, in der Simulation die des
 * Kerns. [presence] sagt, wer ausser dem Wesen selbst an welchem Ort wirklich da ist.
 */
data class DecisionState(
    val agent: AgentState,
    val world: WorldState,
    val phase: PlayAmbientActivity.DayPhase,
    val currentPlace: PlayScene.Place,
    val signals: TopicSignals,
    val presence: Map<PlayScene.Place, Set<String>> = emptyMap(),
    val history: DecisionHistory = DecisionHistory(),
    val nowMinute: Long = world.absoluteMinute.toLong(),
    val minutesSinceMove: Long? = null,
    val minutesSinceOutdoors: Long? = null,
    /** Ein Zuschauer- oder Nutzerimpuls legt das Thema fest (siehe `StreamInteractions`). */
    val impulseTopic: AnimationType? = null,
    val goalInfluence: GoalInfluence? = null,
    /** Wer am aktuellen Ort als Gast steht - fuer den Kern (`nearbyProfiles`). */
    val nearbyProfiles: Set<String> = emptySet(),
    val footballTrickLearned: Boolean = false,
    val recentSpecials: List<PlayRoutines.SpecialActivity> = emptyList()
) {
    /** Die bisherige Themengewichtung dieses Moments. */
    val topicWeights: Map<AnimationType, Int> by lazy {
        impulseTopic?.let { mapOf(it to 1) } ?: signals.weights(phase)
    }

    /**
     * Das Ziel, das der Kern von sich aus verfolgen wuerde (siehe `LivingSimulation.nextGoal`) -
     * die bisherige Wahl, und die einzige, solange ein laufender Plan noch nicht fertig ist.
     */
    val goal: GoalKind? by lazy {
        com.notime.glyphsim.living.LivingSimulation.nextGoal(agent, world, goalInfluence)
    }

    /** Die Zielbewertung des Kerns, fuer Zulaessigkeit und Merkmale. */
    val goalRanking: List<com.notime.glyphsim.living.GoalScore> by lazy {
        com.notime.glyphsim.living.UtilitySelector.rank(agent, world, goalInfluence)
    }

    fun othersAt(place: PlayScene.Place): Set<String> = presence[place] ?: emptySet()
}

/**
 * **Ein sichtbarer Ablauf, den das Wesen jetzt zeigen darf.**
 *
 * [routine] geht als `preferredRoutine` in den bestehenden Laufzeitweg; [visibleRoutine] ist,
 * was tatsaechlich laeuft (bei einem Gruppenspiel dessen eigener Ablauf). [baselineProbability]
 * ist die Wahrscheinlichkeit, mit der die bisherige Wahl genau diesen Ablauf gezogen haette -
 * 0 heisst: erlaubt, aber bisher nie gewaehlt.
 */
data class ActionCandidate(
    val key: String,
    /** Aktivitaetsfamilie fuer Kontinuitaet: Gruppenspielart, Sonderaktivitaet oder Thema. */
    val family: String,
    val topic: AnimationType,
    val interestTopic: AnimationType,
    val routine: PlayRoutine,
    val visibleRoutine: PlayRoutine,
    val groupGame: PlayGroupGame.Kind?,
    /** Wo das Eigentliche geschieht - nicht zwingend der erste Ort des Weges. */
    val place: PlayScene.Place,
    val coreAction: ActionKind,
    /** Das Ziel, unter dem der Kern diesen Ablauf verbucht. */
    val goal: GoalKind?,
    /** [com.notime.glyphsim.living.GoalScore.total] dieses Ziels. */
    val goalUtility: Double,
    /** Wie weit dieses Ziel hinter dem liegt, das der Kern von sich aus gewaehlt haette. */
    val goalGap: Double,
    val baselineProbability: Double,
    /** Wer dort ist - Mitspieler beim Gruppenspiel, Gegenueber sonst. */
    val partners: Set<String> = emptySet()
)

/** Das Ergebnis einer Entscheidung, mit allem, was fuer Protokoll und Vergleich zaehlt. */
data class Decision(
    val candidate: ActionCandidate,
    val index: Int,
    val scores: FloatArray,
    val probabilities: DoubleArray,
    val policyName: String,
    /** Ob die primaere Policy versagt hat und die bisherige Logik entschieden hat. */
    val fellBack: Boolean
)

/**
 * **Kontrollierte Auswahl statt stumpfem Maximum.**
 *
 * Softmax mit Temperatur: Die Bewertung verschiebt Wahrscheinlichkeiten, sie legt die Wahl nicht
 * fest. Ein Wesen, das immer das Beste tut, ist vorhersehbar - und vorhersehbar ist beim Zusehen
 * dasselbe wie langweilig. Der Zufall kommt von aussen ([Random]); mit demselben Startwert faellt
 * dieselbe Wahl, und genau das macht Mehrtageslaeufe pruefbar.
 */
object DecisionSelector {

    /**
     * 1 heisst: Die Bewertung ist direkt ein Log-Gewicht. Fuer die bisherige Logik
     * ([ExistingUtilityPolicy]) ergibt das exakt die bisherigen Wahrscheinlichkeiten.
     */
    const val DEFAULT_TEMPERATURE = 1.0

    /** Nicht endliche Bewertungen fallen heraus; bleibt keine, ist das Ergebnis leer. */
    fun probabilities(scores: FloatArray, temperature: Double = DEFAULT_TEMPERATURE): DoubleArray {
        val t = temperature.coerceAtLeast(1e-3)
        val finite = scores.indices.filter { scores[it].isFinite() }
        if (finite.isEmpty()) return DoubleArray(0)
        val max = finite.maxOf { scores[it].toDouble() }
        val weights = DoubleArray(scores.size) { i ->
            if (scores[i].isFinite()) exp((scores[i] - max) / t) else 0.0
        }
        val sum = weights.sum()
        return DoubleArray(scores.size) { weights[it] / sum }
    }

    /** Index der Wahl, oder -1, wenn keine Bewertung brauchbar war. */
    fun choose(probabilities: DoubleArray, random: Random): Int {
        if (probabilities.isEmpty()) return -1
        var roll = random.nextDouble()
        for (i in probabilities.indices) {
            roll -= probabilities[i]
            if (roll < 0) return i
        }
        return probabilities.indices.last { probabilities[it] > 0.0 }
    }
}

/**
 * **Der Entscheidungspunkt** - bewerten, pruefen, waehlen.
 *
 * Liefert die primaere Policy etwas Unbrauchbares (Ausnahme, NaN, Unendlich), entscheidet fuer
 * diesen Moment die bisherige Logik. Kein Absturz, keine stille Gleichverteilung.
 */
object DecisionEngine {

    fun decide(
        state: DecisionState,
        candidates: List<ActionCandidate>,
        policy: DecisionPolicy,
        random: Random,
        temperature: Double = DecisionSelector.DEFAULT_TEMPERATURE,
        fallback: DecisionPolicy = ExistingUtilityPolicy
    ): Decision? {
        if (candidates.isEmpty()) return null
        val primary = runCatching { policy.scoreAll(state, candidates) }.getOrNull()
            ?.takeIf { scores -> scores.size == candidates.size && scores.all { it.isFinite() } }
        val usedFallback = primary == null
        val scores = primary ?: fallback.scoreAll(state, candidates)
        val t = if (usedFallback) DecisionSelector.DEFAULT_TEMPERATURE else temperature
        val probabilities = DecisionSelector.probabilities(scores, t)
        val index = DecisionSelector.choose(probabilities, random)
        if (index < 0) return null
        return Decision(
            candidate = candidates[index],
            index = index,
            scores = scores,
            probabilities = probabilities,
            policyName = if (usedFallback) fallback.name else policy.name,
            fellBack = usedFallback && policy !== fallback
        )
    }
}
