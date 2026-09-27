package com.notime.glyphsim.decision.tool

import com.notime.glyphsim.decision.ActionCandidate
import com.notime.glyphsim.decision.DecisionFeatures
import com.notime.glyphsim.decision.DecisionPolicies
import com.notime.glyphsim.decision.DecisionPolicy
import com.notime.glyphsim.decision.DecisionSelector
import com.notime.glyphsim.decision.DecisionSimulation
import com.notime.glyphsim.decision.DecisionState
import com.notime.glyphsim.decision.DecisionTeacher
import com.notime.glyphsim.decision.ExistingUtilityPolicy
import com.notime.glyphsim.decision.DecisionCandidates
import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.living.Needs
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.LivingResidents
import com.notime.glyphsim.matrix.PlayScene
import java.io.BufferedOutputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.ln
import kotlin.random.Random

/**
 * Werkzeug zur Decision Policy - Datensatz erzeugen, Policies vergleichen, Laufzeit messen.
 *
 * Laeuft auf denselben uebersetzten Klassen wie die Offline-Tests (siehe `train.sh`). Alles ist
 * aus festen Startwerten reproduzierbar: Derselbe Aufruf erzeugt byte-gleich denselben Datensatz.
 */
fun main(args: Array<String>) {
    when (args.firstOrNull()) {
        "dataset" -> Dataset.write(File(args[1]), days = args.getOrNull(2)?.toInt() ?: 24, seeds = args.getOrNull(3)?.toInt() ?: 3)
        "compare" -> Compare.run(File(args[1]), File(args[2]), days = args.getOrNull(3)?.toInt() ?: 14)
        "bench" -> Bench.run(File(args[1]))
        else -> error("usage: dataset <dir> [days] [seeds] | compare <model> <out.md> [days] | bench <model>")
    }
}

/** Ein Lehrer mit etwas Neugier: Die Rollouts sollen auch Zustaende abseits seines Weges zeigen. */
private class ExplorerPolicy(private val epsilon: Double) : DecisionPolicy {
    override val name = "teacher+explore"
    override fun score(state: DecisionState, candidate: ActionCandidate): Float = error("batch only")
    override fun scoreAll(state: DecisionState, candidates: List<ActionCandidate>): FloatArray {
        val t = DecisionTeacher.Policy.scoreAll(state, candidates)
        val p = DecisionSelector.probabilities(t, 1.0)
        return FloatArray(candidates.size) { ln((1 - epsilon) * p[it] + epsilon / candidates.size).toFloat() }
    }
}

private object Dataset {

    fun write(dir: File, days: Int, seeds: Int) {
        dir.mkdirs()
        val names = DecisionFeatures.NAMES
        val features = DataOutputStream(BufferedOutputStream(FileOutputStream(File(dir, "features.f32")), 1 shl 20))
        val targets = DataOutputStream(BufferedOutputStream(FileOutputStream(File(dir, "teacher.f32"))))
        val groups = DataOutputStream(BufferedOutputStream(FileOutputStream(File(dir, "groups.i32"))))
        val speciesOut = DataOutputStream(BufferedOutputStream(FileOutputStream(File(dir, "species.i32"))))
        var rows = 0
        var decisions = 0
        val buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
        fun f32(out: DataOutputStream, v: Float) {
            buf.clear(); buf.putFloat(v); out.write(buf.array())
        }
        fun i32(out: DataOutputStream, v: Int) {
            buf.clear(); buf.putInt(v); out.write(buf.array())
        }
        fun emit(state: DecisionState, candidates: List<ActionCandidate>, species: AvatarSpecies) {
            if (candidates.size < 2) return
            for (c in candidates) {
                val named = DecisionFeatures.named(state, c)
                for (n in names) f32(features, named.getValue(n).toFloat())
                f32(targets, DecisionTeacher.score(named).toFloat())
                i32(groups, decisions)
                rows++
            }
            i32(speciesOut, species.ordinal)
            decisions++
        }
        for (species in AvatarSpecies.entries) {
            for (seed in 0 until seeds) {
                val perturb = Random(1_000L * species.ordinal + seed)
                DecisionSimulation.run(
                    DecisionSimulation.Config(
                        species = species,
                        days = days,
                        seed = 10_000L + 100L * species.ordinal + seed,
                        policy = ExplorerPolicy(0.25),
                        extraPresenceChance = 0.35
                    )
                ) { state, candidates, _ ->
                    emit(state, candidates, species)
                    // Dieselbe Lage mit verschobenen Beduerfnissen und anderer Gesellschaft - damit
                    // das Netz auch Hunger am Nachmittag oder Freunde im Park am Morgen gesehen hat.
                    if (perturb.nextDouble() < 0.6) {
                        val gestoert = perturbed(state, perturb)
                        emit(gestoert, DecisionCandidates.generate(gestoert), species)
                    }
                }
            }
        }
        features.close(); targets.close(); groups.close(); speciesOut.close()
        File(dir, "meta.json").writeText(
            buildString {
                append("{\n")
                append("  \"feature_schema_version\": ${DecisionFeatures.SCHEMA_VERSION},\n")
                append("  \"rows\": $rows,\n  \"decisions\": $decisions,\n  \"days\": $days,\n  \"seeds\": $seeds,\n")
                append("  \"names\": [" + names.joinToString(",") { "\"$it\"" } + "]\n}\n")
            }
        )
        println("dataset: $decisions decisions, $rows rows, ${names.size} features -> $dir")
    }

    private fun perturbed(state: DecisionState, random: Random): DecisionState {
        val needs = Needs(NeedKind.entries.associateWith { k ->
            (state.agent.needs.pressure(k) + (random.nextDouble() - 0.5) * 0.8).coerceIn(0.0, 1.0)
        })
        val presence = state.presence.toMutableMap()
        if (random.nextDouble() < 0.5) {
            val ort = listOf(PlayScene.Place.PARK, PlayScene.Place.SPORT, PlayScene.Place.MEADOW, state.currentPlace).random(random)
            presence[ort] = (presence[ort] ?: emptySet()) + LivingResidents.all.random(random).profileId
        } else if (random.nextDouble() < 0.3) {
            presence.clear()
        }
        return state.copy(
            agent = state.agent.copy(needs = needs, goal = null, plan = null),
            presence = presence,
            minutesSinceMove = if (random.nextDouble() < 0.3) random.nextLong(0, 400) else state.minutesSinceMove,
            minutesSinceOutdoors = if (random.nextDouble() < 0.3) random.nextLong(0, 400) else state.minutesSinceOutdoors
        )
    }
}

private object Compare {

    fun run(model: File, out: File, days: Int) {
        val loaded = DecisionPolicies.load(model.readBytes())
        require(loaded.usesModel) { "model rejected: ${loaded.fallbackReason}" }
        val policies = listOf(
            Triple("Baseline (bisherige Logik)", ExistingUtilityPolicy as DecisionPolicy, DecisionSelector.DEFAULT_TEMPERATURE),
            Triple("Tiny Neural Policy (ONNX v1)", loaded.policy, loaded.temperature),
            Triple("Lehrer (Obergrenze)", DecisionTeacher.Policy as DecisionPolicy, DecisionSelector.DEFAULT_TEMPERATURE)
        )
        val seeds = 3
        val sb = StringBuilder()
        sb.append("| Kennzahl | " + policies.joinToString(" | ") { it.first } + " |\n")
        sb.append("| --- |" + policies.joinToString("") { " ---: |" } + "\n")
        val alle = policies.map { (_, policy, t) ->
            AvatarSpecies.entries.flatMap { species ->
                (0 until seeds).map { seed ->
                    DecisionSimulation.run(
                        DecisionSimulation.Config(species, days, 500L + 10L * species.ordinal + seed, policy, t)
                    ).records
                }
            }
        }
        val metriken = alle.map { runs -> runs.map(DecisionSimulation::metrics) }
        fun zeile(titel: String, fmt: (DecisionSimulation.Metrics) -> Double, digits: Int = 2, prozent: Boolean = false) {
            sb.append("| $titel |")
            for (m in metriken) {
                val v = m.map(fmt).average()
                sb.append(" " + if (prozent) "%.1f %%".format(v * 100) else "%.${digits}f".format(v))
                sb.append(" |")
            }
            sb.append("\n")
        }
        zeile("Entscheidungen je Lauf", { it.decisions.toDouble() }, 0)
        zeile("Verschiedene Ablaeufe je Ingame-Tag", { it.distinctPerDay })
        zeile("Aktivitaetskategorien je Ingame-Tag", { it.categoriesPerDay })
        zeile("Ortswechsel je Ingame-Tag", { it.placeChangesPerDay })
        zeile("Direkte Wiederholung (gleicher Ablauf)", { it.repeatRate }, prozent = true)
        zeile("Soziale Aktionen", { it.socialShare }, prozent = true)
        zeile("davon Gruppenspiele", { it.groupShare }, prozent = true)
        zeile("Wiederaufnahme frueherer Aktivitaeten", { it.resumptionShare }, prozent = true)
        zeile("Verschiedene Ablaeufe insgesamt", { it.distinctTotal.toDouble() }, 1)
        zeile("Sichtbare Vielfalt (Entropie, Bit)", { it.entropyBits })
        zeile("Wahlen, die die Baseline nie trifft", { it.zeroBaselineShare }, prozent = true)
        zeile("Rueckfall auf Baseline", { it.fallbackShare }, prozent = true)
        zeile("Plausibilitaet: Ziel = Wahl des Kerns", { it.coreGoalShare }, prozent = true)
        zeile("Plausibilitaet: Druck des verfolgten Beduerfnisses", { it.meanGoalPressure })
        zeile("Plausibilitaet: nachts draussen (Anteil der Nachtwahlen)", { it.nightOutdoorShare }, prozent = true)

        // Charakterunterschiede: Anteil je Kategorie je Spezies unter der Policy.
        sb.append("\n**Charakterunterschiede (Tiny Neural Policy, Anteil der Entscheidungen)**\n\n")
        val kategorien = listOf("food", "rest", "sleep", "work", "movement", "sport", "group", "explore", "mind", "creative", "music", "game", "social", "calm")
        sb.append("| Spezies | " + kategorien.joinToString(" | ") + " |\n")
        sb.append("| --- |" + kategorien.joinToString("") { " ---: |" } + "\n")
        val policyRuns = alle[1]
        AvatarSpecies.entries.forEachIndexed { i, species ->
            val recs = policyRuns.subList(i * seeds, (i + 1) * seeds).flatten()
            sb.append("| ${species.name} |")
            for (k in kategorien) sb.append(" %.0f |".format(100.0 * recs.count { it.category == k } / recs.size.coerceAtLeast(1)))
            sb.append("\n")
        }
        out.parentFile?.mkdirs()
        out.writeText(sb.toString())
        println(sb)
    }
}

private object Bench {

    fun run(modelFile: File) {
        val bytes = modelFile.readBytes()
        repeat(20) { DecisionPolicies.load(bytes) }
        val t0 = System.nanoTime()
        val loads = 200
        repeat(loads) { DecisionPolicies.load(bytes) }
        val initMs = (System.nanoTime() - t0) / 1e6 / loads
        val loaded = DecisionPolicies.load(bytes)
        require(loaded.usesModel) { "model rejected: ${loaded.fallbackReason}" }
        val momente = mutableListOf<Pair<DecisionState, List<ActionCandidate>>>()
        DecisionSimulation.run(
            DecisionSimulation.Config(AvatarSpecies.WYRMLING, 6, 42L, ExistingUtilityPolicy, extraPresenceChance = 0.3)
        ) { s, c, _ -> momente += s to c }
        fun time(label: String, block: (DecisionState, List<ActionCandidate>) -> Unit) {
            repeat(3) { momente.forEach { (s, c) -> block(s, c) } }
            val werte = momente.map { (s, c) ->
                val a = System.nanoTime(); block(s, c); (System.nanoTime() - a) / 1e3
            }.sorted()
            println("%-44s mean %7.1f us   p95 %7.1f us   (n=%d)".format(label, werte.average(), werte[(werte.size * 0.95).toInt().coerceAtMost(werte.size - 1)], werte.size))
        }
        println("model bytes: ${bytes.size}   parameters: ${(loaded.policy as com.notime.glyphsim.decision.OnnxDecisionPolicy).parameterCount}   features: ${DecisionFeatures.COUNT}")
        println("load + validate: %.3f ms".format(initMs))
        println("candidates per decision: mean %.1f  max %d".format(momente.map { it.second.size }.average(), momente.maxOf { it.second.size }))
        time("candidate generation (DecisionCandidates)") { s, _ -> DecisionCandidates.generate(s) }
        time("ONNX scoring incl. features (all candidates)") { s, c -> loaded.policy.scoreAll(s, c) }
        time("baseline scoring (all candidates)") { s, c -> ExistingUtilityPolicy.scoreAll(s, c) }
        val rt = Runtime.getRuntime()
        System.gc(); Thread.sleep(100)
        val vorher = rt.totalMemory() - rt.freeMemory()
        val viele = List(50) { DecisionPolicies.load(bytes) }
        System.gc(); Thread.sleep(100)
        val nachher = rt.totalMemory() - rt.freeMemory()
        println("heap per loaded model (rough): %.1f KB".format((nachher - vorher) / 1024.0 / viele.size))
    }
}
