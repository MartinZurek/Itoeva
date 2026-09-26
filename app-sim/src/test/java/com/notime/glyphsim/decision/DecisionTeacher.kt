package com.notime.glyphsim.decision

import kotlin.math.ln

/**
 * **Der Lehrer der Policy (Phase A)** - eine lesbare Bewertung, aus der das kleine Netz lernt.
 *
 * Er liegt bewusst im Testbaum und nicht in der App: Die App enthaelt nur das gelernte Modell und
 * die bisherige Logik als Rueckfall. Der Lehrer ist Werkzeug - fuer den Datensatz
 * (`tools/decision-policy/`), fuer Tests und fuer den Vergleich.
 *
 * Er setzt sich aus genau den Bausteinen zusammen, die der Auftrag nennt, und rechnet nur mit den
 * benannten Merkmalen aus [DecisionFeatures] - das Netz sieht also alles, was er sieht:
 *
 * | Baustein | Merkmale |
 * | --- | --- |
 * | vorhandene Utility | `base_logp` / `base_zero` (die bisherige Kette als Log-Wahrscheinlichkeit) |
 * | Need Fit | `need_fit`, Hunger x Essen, Muedigkeit x Ruhe/Schlaf, Neugier x Erkunden, ... |
 * | Personality Fit | `personality_fit`, `signature_match`, `leaning_match` |
 * | Novelty | `novelty`, `never_shown`, `resumption` |
 * | Repetition Penalty | `repeat_key`, `repeat_family`, `repeat_topic` |
 * | Social Opportunity | `social_opportunity`, `partner_closeness` |
 * | Continuity | `continuity`, `shared_memory` |
 * | Kosten / Aufwand | `fatigue_cost`, `place_change`, Muedigkeit x Koerperlich |
 *
 * Die Gewichte sind Handarbeit und Absicht, keine Optimierung: Die bisherige Wahl bleibt der
 * groesste Einzelterm, damit Tagesplan, Nachtruhe und Ort weiter tragen. Alles andere verschiebt
 * innerhalb dessen, was ohnehin passt.
 */
object DecisionTeacher {

    /** Log-Wert einer Option, die die bisherige Kette nie zog - selten, aber erreichbar. */
    const val ZERO_BASELINE = -2.5

    /** Wie stark die bisherige Log-Wahrscheinlichkeit zaehlt. */
    const val BASE_WEIGHT = 0.55

    /** Abzug je Punkt Zielabstand fuer ein Ziel, das der Kern nicht von sich aus gewaehlt haette. */
    const val GAP_WEIGHT = 5.0

    fun score(f: Map<String, Double>): Double {
        fun v(name: String) = f[name] ?: error("feature $name missing")
        // Die bisherige Kette: unter dem Lieblingsziel des Kerns ihre Log-Wahrscheinlichkeit (etwas
        // abgeflacht, damit Neuheit und Gelegenheit ueberhaupt etwas verschieben koennen), fuer ein
        // knapp unterlegenes Ziel ein seltener Wert, der mit dem Abstand weiter faellt.
        val base = when {
            v("goal_is_core_choice") < 0.5 -> ZERO_BASELINE - GAP_WEIGHT * v("goal_gap")
            v("base_zero") > 0.5 -> ZERO_BASELINE
            else -> BASE_WEIGHT * (v("base_logp") * 8.0).coerceAtLeast(-6.0)
        }
        val sozialOderSpiel = maxOf(v("act_social"), v("act_group"))
        var s = base
        // Need Fit
        s += 2.0 * v("need_fit")
        s += 1.5 * v("need_hunger") * v("act_eat")
        s += 1.2 * v("need_energy") * maxOf(v("act_rest"), v("act_sleep"))
        s += 1.0 * v("act_sleep") * v("phase_night")
        s += 0.8 * v("act_drink") * (1.0 - v("since_move")) * v("since_move_known")
        s += 0.3 * v("act_drink") * v("need_comfort")
        s += 0.6 * v("need_curiosity") * v("act_explore")
        s += 0.5 * v("need_fun") * v("act_game")
        s += 0.4 * v("need_growth") * v("act_learn")
        s += 0.4 * v("need_comfort") * v("act_calm")
        s += 0.8 * v("urge")
        s += 1.2 * v("outdoor_deficit")
        // Personality Fit
        s += 0.8 * v("personality_fit")
        s += 0.5 * v("signature_match")
        s += 0.3 * v("leaning_match")
        // Novelty und Wiederaufnahme
        s += 1.0 * v("novelty")
        s += 0.6 * v("never_shown")
        s += 0.5 * v("resumption")
        // Repetition Penalty
        s -= 1.2 * v("repeat_key")
        s -= 0.8 * v("repeat_family")
        s -= 0.3 * v("repeat_topic")
        // Social Opportunity und Continuity
        s += 1.5 * v("social_opportunity") * (0.5 + v("need_social"))
        s += 0.6 * v("partner_closeness") * sozialOderSpiel
        s += 1.0 * v("continuity")
        s += 0.3 * v("shared_memory") * sozialOderSpiel
        // Kosten und Aufwand
        s -= 1.2 * v("fatigue_cost")
        s -= 0.8 * v("need_energy") * v("act_physical")
        s -= 0.3 * v("place_change") * (1.0 - v("need_curiosity"))
        s -= 0.2 * v("act_effort")
        return s
    }

    /** Der Lehrer als Policy - fuer Datensatz und Vergleich ("so waere es ideal"). */
    object Policy : DecisionPolicy {
        override val name: String = "teacher"
        override fun score(state: DecisionState, candidate: ActionCandidate): Float =
            score(DecisionFeatures.named(state, candidate)).toFloat()
    }

    /** Fuer Berichte: die Summe der Log-Wahrscheinlichkeiten einer Verteilung. */
    fun entropy(p: DoubleArray): Double = -p.filter { it > 0 }.sumOf { it * ln(it) }
}
