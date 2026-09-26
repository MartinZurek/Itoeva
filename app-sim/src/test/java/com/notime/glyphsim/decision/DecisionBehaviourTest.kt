package com.notime.glyphsim.decision

import com.notime.glyphsim.matrix.AvatarSpecies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mehrere Ingame-Tage mit der gelernten Policy gegen dieselben Tage mit der bisherigen Logik.
 * Ausfuehrlich (14 Tage, drei Startwerte, Lehrer als Obergrenze) steht der Vergleich in
 * `tools/decision-policy/baseline-vs-policy.md`; hier der kleine, schnelle Teil davon.
 */
class DecisionBehaviourTest {

    private val loaded = DecisionTestSupport.bundled
    private val tage = 7

    private fun runs(policy: DecisionPolicy, t: Double) = AvatarSpecies.entries.map { species ->
        DecisionSimulation.run(DecisionSimulation.Config(species, tage, 900L + species.ordinal, policy, t)).records
    }

    private val baseline by lazy { runs(ExistingUtilityPolicy, 1.0) }
    private val policy by lazy { runs(loaded.policy, loaded.temperature) }

    @Test
    fun `die Policy entscheidet selbst und faellt nie zurueck`() {
        val alle = policy.flatten()
        assertTrue(alle.size > 6 * tage * 5)
        assertTrue(alle.none { it.fellBack })
    }

    @Test
    fun `mehr Abwechslung ueber mehrere Tage`() {
        val b = baseline.map(DecisionSimulation::metrics)
        val p = policy.map(DecisionSimulation::metrics)
        assertTrue(p.map { it.repeatRate }.average() < b.map { it.repeatRate }.average())
        assertTrue(p.map { it.distinctPerDay }.average() > b.map { it.distinctPerDay }.average())
        assertTrue(p.map { it.entropyBits }.average() > b.map { it.entropyBits }.average())
    }

    @Test
    fun `plausibel bleibt plausibel`() {
        val p = policy.map(DecisionSimulation::metrics)
        val b = baseline.map(DecisionSimulation::metrics)
        // Der Kern bleibt der Taktgeber: fast immer sein eigenes Ziel, und kein schwaecherer Druck.
        assertTrue(p.map { it.coreGoalShare }.average() > 0.85)
        assertTrue(p.map { it.meanGoalPressure }.average() > b.map { it.meanGoalPressure }.average() - 0.05)
        // Nachts nicht haeufiger draussen als bisher.
        assertTrue(p.map { it.nightOutdoorShare }.average() <= b.map { it.nightOutdoorShare }.average() + 0.03)
    }

    @Test
    fun `jede Spezies lebt einen anderen Tag`() {
        val profile = policy.map { recs ->
            recs.groupingBy { it.family }.eachCount().mapValues { it.value.toDouble() / recs.size }
        }
        var groesste = 0.0
        for (i in profile.indices) for (j in i + 1 until profile.size) {
            val keys = profile[i].keys + profile[j].keys
            val tv = keys.sumOf { kotlin.math.abs((profile[i][it] ?: 0.0) - (profile[j][it] ?: 0.0)) } / 2
            groesste = maxOf(groesste, tv)
        }
        assertTrue("Tage zu aehnlich: $groesste", groesste > 0.1)
    }

    @Test
    fun `derselbe Lauf wiederholt sich exakt`() {
        val cfg = DecisionSimulation.Config(AvatarSpecies.GLOOP, 3, 5L, loaded.policy, loaded.temperature)
        assertEquals(DecisionSimulation.run(cfg).records, DecisionSimulation.run(cfg).records)
    }
}
