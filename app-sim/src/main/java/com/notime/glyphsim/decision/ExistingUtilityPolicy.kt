package com.notime.glyphsim.decision

import kotlin.math.ln

/**
 * **Die bisherige Wahl, als Policy ausgedrueckt** - Rueckfall und Vergleichsgrundlage.
 *
 * Bis zur Decision Policy entschieden hintereinander die Themengewichtung
 * ([com.notime.glyphsim.matrix.PlayAmbientActivity.nextTopic]), der Kern, der Ablaufwurf
 * ([com.notime.glyphsim.matrix.PlayRoutines.forTopic]) und der Gruppenspiel-Vorrang.
 * [DecisionCandidates] rechnet fuer jeden Kandidaten aus, mit welcher Wahrscheinlichkeit diese
 * Kette ihn gezogen haette. Hier wird daraus nur noch der Logarithmus.
 *
 * Mit Temperatur 1 ergibt die Softmax daraus **exakt die bisherigen Wahrscheinlichkeiten** -
 * wer auf diese Policy zurueckfaellt, sieht dasselbe Wesen wie vorher. Kandidaten, die die alte
 * Kette nie zog (das zweite Sofa, der Becher, das Bett in der Nacht), bekommen einen so tiefen
 * Wert, dass sie praktisch nie gezogen werden - aber keinen unendlichen, damit die Rechnung
 * endlich bleibt.
 */
object ExistingUtilityPolicy : DecisionPolicy {

    override val name: String = "existing-utility"

    /** exp(-1000) ist in Double exakt 0 - die Option faellt heraus, ohne die Rechnung zu brechen. */
    const val NEVER = -1_000f

    override fun score(state: DecisionState, candidate: ActionCandidate): Float {
        val p = candidate.baselineProbability
        return if (p > 0.0) ln(p).toFloat() else NEVER
    }
}
