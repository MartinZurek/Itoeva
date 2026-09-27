package com.notime.glyphsim.decision

/**
 * **Die gelernte Policy** - ein kleines Netz, das jeden Kandidaten aus Zustand und Merkmalen
 * bewertet (siehe `tools/decision-policy/`).
 *
 * Gerechnet wird gebuendelt: alle Kandidaten eines Moments in einem Durchlauf. Das geschieht nur
 * an Entscheidungspunkten (etwa alle fuenfzig Sekunden), nie je Bild.
 */
class OnnxDecisionPolicy internal constructor(private val model: OnnxModel) : DecisionPolicy {

    override val name: String = "onnx-v" + (model.metadata[DecisionPolicies.KEY_MODEL_VERSION] ?: "?")

    val parameterCount: Int get() = model.parameterCount

    override fun score(state: DecisionState, candidate: ActionCandidate): Float =
        scoreAll(state, listOf(candidate))[0]

    override fun scoreAll(state: DecisionState, candidates: List<ActionCandidate>): FloatArray {
        if (candidates.isEmpty()) return FloatArray(0)
        val breite = model.inputWidth
        val eingabe = FloatArray(candidates.size * breite)
        candidates.forEachIndexed { i, candidate ->
            DecisionFeatures.encode(state, candidate).copyInto(eingabe, i * breite)
        }
        return model.run(eingabe, candidates.size)
    }
}

/**
 * **Welche Policy gilt** - das Modell, wenn es passt, sonst die bisherige Logik.
 *
 * ```
 * DecisionPolicy
 * ├── ExistingUtilityPolicy   (immer da, bisheriges Verhalten)
 * └── OnnxDecisionPolicy      (nur, wenn das Modell vollstaendig passt)
 * ```
 *
 * Ein Modell wird abgelehnt, wenn es fehlt, sich nicht lesen laesst, eine unbekannte Operation
 * enthaelt, eine andere Modell- oder Schemaversion traegt, andere Merkmalsnamen nennt oder auf
 * einem Referenzmoment keine endliche Zahl liefert. Die Ablehnung ist ein Rueckfall mit Grund,
 * nie ein Absturz. Liefert ein geladenes Modell spaeter doch einmal NaN, entscheidet fuer diesen
 * einen Moment ebenfalls die bisherige Logik (siehe [DecisionEngine]).
 */
object DecisionPolicies {

    /** Die Modellversion, die diese App versteht. */
    const val MODEL_VERSION = 1

    /** Der Dateiname in `assets/` - in App und Stream-Variante dieselbe Datei. */
    const val ASSET_NAME = "decision_policy_v1.onnx"

    const val KEY_MODEL_VERSION = "policy_model_version"
    const val KEY_SCHEMA_VERSION = "feature_schema_version"
    const val KEY_FEATURE_NAMES = "feature_names"
    const val KEY_TEMPERATURE = "temperature"

    data class Loaded(
        val policy: DecisionPolicy,
        /** Temperatur der Auswahl; beim Rueckfall immer 1 (bisherige Wahrscheinlichkeiten). */
        val temperature: Double,
        /** `null`, wenn das Modell gilt - sonst warum nicht. */
        val fallbackReason: String?
    ) {
        val usesModel: Boolean get() = fallbackReason == null
    }

    fun load(bytes: ByteArray?): Loaded {
        if (bytes == null || bytes.isEmpty()) return fallback("model missing")
        val model = try {
            OnnxModel.parse(bytes)
        } catch (e: Exception) {
            return fallback("model unreadable: ${e.message}")
        }
        val version = model.metadata[KEY_MODEL_VERSION]
        if (version != MODEL_VERSION.toString()) return fallback("model version $version")
        val schema = model.metadata[KEY_SCHEMA_VERSION]
        if (schema != DecisionFeatures.SCHEMA_VERSION.toString()) return fallback("feature schema $schema")
        val namen = model.metadata[KEY_FEATURE_NAMES]?.split(',')
        if (namen != DecisionFeatures.NAMES) return fallback("feature names differ")
        if (model.inputWidth != DecisionFeatures.COUNT) return fallback("input width ${model.inputWidth}")
        val probe = try {
            model.run(FloatArray(model.inputWidth), 1)
        } catch (e: Exception) {
            return fallback("model does not run: ${e.message}")
        }
        if (!probe[0].isFinite()) return fallback("model yields ${probe[0]}")
        val temperature = model.metadata[KEY_TEMPERATURE]?.toDoubleOrNull()
            ?.takeIf { it.isFinite() && it > 0.0 }
            ?: DecisionSelector.DEFAULT_TEMPERATURE
        return Loaded(OnnxDecisionPolicy(model), temperature, null)
    }

    private fun fallback(reason: String) =
        Loaded(ExistingUtilityPolicy, DecisionSelector.DEFAULT_TEMPERATURE, reason)
}
