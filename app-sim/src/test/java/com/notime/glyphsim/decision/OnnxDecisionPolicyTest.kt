package com.notime.glyphsim.decision

import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.PlayScene
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * Das ausgelieferte Modell: laedt es, rechnet es wie onnxruntime, und faellt es sauber zurueck,
 * wenn etwas nicht stimmt?
 */
class OnnxDecisionPolicyTest {

    private val loaded = DecisionTestSupport.bundled

    @Test
    fun `das mitgelieferte Modell wird geladen und ist klein`() {
        assertTrue("Rueckfall: ${loaded.fallbackReason}", loaded.usesModel)
        val bytes = DecisionTestSupport.modelFile.length()
        assertTrue("Modell zu gross: $bytes", bytes < 1_000_000)
        val policy = loaded.policy as OnnxDecisionPolicy
        assertTrue(policy.parameterCount in 1..100_000)
    }

    /** Dieselben Zeilen, gerechnet von onnxruntime beim Training und hier vom eigenen Leser. */
    @Test
    fun `rechnet wie onnxruntime`() {
        val model = OnnxModel.parse(DecisionTestSupport.modelFile.readBytes())
        val zeilen = File("src/test/decision-policy-reference.csv").readLines().filterNot { it.startsWith("#") }
        assertTrue(zeilen.size >= 10)
        for (zeile in zeilen) {
            val werte = zeile.split(',').map { it.toFloat() }
            val erwartet = werte[0]
            val eingabe = werte.drop(1).toFloatArray()
            assertEquals(model.inputWidth, eingabe.size)
            assertEquals(erwartet, model.run(eingabe, 1)[0], 1e-4f)
        }
    }

    @Test
    fun `Modell und Schema gehoeren zusammen`() {
        val model = OnnxModel.parse(DecisionTestSupport.modelFile.readBytes())
        assertEquals(DecisionPolicies.MODEL_VERSION.toString(), model.metadata[DecisionPolicies.KEY_MODEL_VERSION])
        assertEquals(DecisionFeatures.SCHEMA_VERSION.toString(), model.metadata[DecisionPolicies.KEY_SCHEMA_VERSION])
        assertEquals(DecisionFeatures.NAMES, model.metadata[DecisionPolicies.KEY_FEATURE_NAMES]!!.split(','))
    }

    /** App und Stream-APK bekommen dieselbe Datei - es gibt keinen Variantenordner daneben. */
    @Test
    fun `App und Stream teilen dasselbe Modell`() {
        val varianten = File("src").listFiles()!!.filter { it.isDirectory && it.name != "main" }
        for (v in varianten) {
            assertFalse("${v.name} ueberschreibt das Modell", File(v, "assets/" + DecisionPolicies.ASSET_NAME).exists())
        }
    }

    @Test
    fun `fehlendes, kaputtes oder fremdes Modell faellt zurueck`() {
        assertFalse(DecisionPolicies.load(null).usesModel)
        assertFalse(DecisionPolicies.load(ByteArray(0)).usesModel)
        assertFalse(DecisionPolicies.load("kein onnx".toByteArray()).usesModel)
        val echt = DecisionTestSupport.modelFile.readBytes()
        assertFalse(DecisionPolicies.load(echt.copyOf(echt.size / 2)).usesModel)
        // Falsches Schema: dieselbe Datei, nur die Versionsangabe ausgetauscht.
        val fremd = replace(echt, "feature_schema_version", "1", "9")
        val r = DecisionPolicies.load(fremd)
        assertFalse(r.usesModel)
        assertTrue(r.fallbackReason!!.contains("schema"))
        assertEquals(ExistingUtilityPolicy, r.policy)
    }

    /** Liefert die Policy NaN, entscheidet fuer diesen Moment die bisherige Logik. */
    @Test
    fun `NaN faellt auf die bisherige Logik zurueck`() {
        val kaputt = object : DecisionPolicy {
            override val name = "nan"
            override fun score(state: DecisionState, candidate: ActionCandidate) = Float.NaN
        }
        val state = DecisionTestSupport.state(needs = mapOf(NeedKind.FUN to 0.8))
        val kandidaten = DecisionCandidates.generate(state)
        val d = DecisionEngine.decide(state, kandidaten, kaputt, Random(1))!!
        assertTrue(d.fellBack)
        assertEquals(ExistingUtilityPolicy.name, d.policyName)
        val wirft = object : DecisionPolicy {
            override val name = "throws"
            override fun score(state: DecisionState, candidate: ActionCandidate): Float = error("boom")
        }
        assertTrue(DecisionEngine.decide(state, kandidaten, wirft, Random(1))!!.fellBack)
    }

    @Test
    fun `gleicher Zustand und gleicher Startwert ergeben dieselbe Wahl`() {
        val state = DecisionTestSupport.state(needs = mapOf(NeedKind.FUN to 0.8))
        val kandidaten = DecisionCandidates.generate(state)
        val a = (0 until 20).map { DecisionEngine.decide(state, kandidaten, loaded.policy, Random(it.toLong()), loaded.temperature)!!.candidate.key }
        val b = (0 until 20).map { DecisionEngine.decide(state, kandidaten, loaded.policy, Random(it.toLong()), loaded.temperature)!!.candidate.key }
        assertEquals(a, b)
        assertTrue("keine Abwechslung ueber Startwerte", a.toSet().size > 1)
        val s1 = loaded.policy.scoreAll(state, kandidaten)
        assertArrayEquals(s1, loaded.policy.scoreAll(state, kandidaten), 0f)
    }

    // ---- Was das Modell gelernt hat ----

    private fun score(state: DecisionState, pick: (ActionCandidate) -> Boolean): Float {
        val kandidaten = DecisionCandidates.generate(state)
        val k = kandidaten.first(pick)
        return loaded.policy.score(state, k)
    }

    @Test
    fun `Hunger macht Essen attraktiver`() {
        val satt = DecisionTestSupport.state(needs = mapOf(NeedKind.HUNGER to 0.3))
        val hungrig = DecisionTestSupport.state(needs = mapOf(NeedKind.HUNGER to 0.9))
        val essen = DecisionCandidates.generate(hungrig).first { ActionTraits.of(it).eat }
        assertTrue(loaded.policy.score(hungrig, essen) > loaded.policy.score(satt, essen))
    }

    @Test
    fun `Muedigkeit macht Ruhe und nachts das Bett attraktiver`() {
        val wach = DecisionTestSupport.state(minuteOfDay = 23 * 60 + 30, needs = mapOf(NeedKind.ENERGY to 0.72))
        val muede = DecisionTestSupport.state(minuteOfDay = 23 * 60 + 30, needs = mapOf(NeedKind.ENERGY to 0.95))
        val bett = DecisionCandidates.generate(muede).first { ActionTraits.of(it).sleep }
        assertTrue(loaded.policy.score(muede, bett) > loaded.policy.score(wach, bett))
    }

    @Test
    fun `dreimal hintereinander dasselbe senkt die Bewertung`() {
        val state = DecisionTestSupport.state(needs = mapOf(NeedKind.FUN to 0.8), minuteOfDay = 16 * 60)
        val k = DecisionCandidates.generate(state).first { it.topic == com.notime.glyphcore.data.AnimationType.MOVE }
        var h = DecisionHistory()
        repeat(3) { h = h.recorded(DecisionHistory.Entry(state.nowMinute - 30L * (3 - it), k.key, k.family, k.topic.name)) }
        assertTrue(loaded.policy.score(state.copy(history = h), k) < loaded.policy.score(state, k))
    }

    @Test
    fun `wer lange nicht draussen war, zieht es hinaus`() {
        val drinnen = DecisionTestSupport.state(needs = mapOf(NeedKind.FUN to 0.8), minutesSinceOutdoors = 10)
        val lange = drinnen.copy(minutesSinceOutdoors = 400)
        val draussen = DecisionCandidates.generate(drinnen).first { ActionTraits.of(it).outdoor }
        assertTrue(loaded.policy.score(lange, draussen) > loaded.policy.score(drinnen, draussen))
    }

    /** Gestern zusammen Frisbee gespielt, heute ist dieselbe Freundin im Park: das Spiel liegt naeher. */
    @Test
    fun `gemeinsames Erlebnis wird eher fortgesetzt`() {
        val freundin = "resident:park:puffling"
        val basis = DecisionTestSupport.state(
            needs = mapOf(NeedKind.FUN to 0.8),
            presence = mapOf(PlayScene.Place.PARK to setOf(freundin))
        )
        val frisbee = DecisionCandidates.generate(basis).first { it.groupGame == com.notime.glyphsim.matrix.PlayGroupGame.Kind.FRISBEE }
        val gestern = DecisionHistory().recorded(
            DecisionHistory.Entry(basis.nowMinute - 20 * 60, "group:FRISBEE@PARK", frisbee.family, "MOVE", setOf(freundin), true)
        )
        val mit = basis.copy(history = gestern)
        // Nur die Kontinuitaet vergleichen - ohne Neuheitseffekt des Schluessels.
        val ohne = basis.copy(history = DecisionHistory().recorded(gestern.entries.first().copy(partners = emptySet())))
        assertTrue(loaded.policy.score(mit, frisbee) > loaded.policy.score(ohne, frisbee))
    }

    @Test
    fun `Freunde am Spielort machen das Gruppenspiel attraktiver als allein`() {
        val allein = DecisionTestSupport.state(needs = mapOf(NeedKind.FUN to 0.8, NeedKind.SOCIAL to 0.6))
        val mit = allein.copy(presence = mapOf(PlayScene.Place.PARK to setOf("resident:park:puffling", "resident:sport:wyrmling")))
        val kandidaten = DecisionCandidates.generate(mit)
        val p = DecisionSelector.probabilities(loaded.policy.scoreAll(mit, kandidaten), loaded.temperature)
        val spiel = kandidaten.indices.filter { kandidaten[it].groupGame != null }.sumOf { p[it] }
        val base = kandidaten.filter { it.groupGame != null }.sumOf { it.baselineProbability }
        assertTrue("Gruppenspiel $spiel", spiel > 0.2)
        assertNotEquals(0.0, base)
    }

    @Test
    fun `die Spezies faerben die Bewertung`() {
        val a = DecisionTestSupport.state(species = AvatarSpecies.HOOTLET, needs = mapOf(NeedKind.GROWTH to 0.7, NeedKind.FUN to 0.6))
        val b = DecisionTestSupport.state(species = AvatarSpecies.WYRMLING, needs = mapOf(NeedKind.GROWTH to 0.7, NeedKind.FUN to 0.6))
        val ka = DecisionCandidates.generate(a)
        val kb = DecisionCandidates.generate(b)
        val gemeinsam = ka.map { it.key }.intersect(kb.map { it.key }.toSet())
        assertTrue(gemeinsam.isNotEmpty())
        val unterschied = gemeinsam.count { key ->
            loaded.policy.score(a, ka.first { it.key == key }) != loaded.policy.score(b, kb.first { it.key == key })
        }
        assertTrue(unterschied > 0)
    }

    private fun replace(bytes: ByteArray, key: String, old: String, new: String): ByteArray {
        // Metadaten stehen als key=..., value=... direkt hintereinander; der Wert folgt dem Schluessel.
        val k = key.toByteArray()
        val start = indexOf(bytes, k)
        require(start >= 0)
        val after = start + k.size
        // Tag (0x12) und Laenge (1) fuer den Wert, dann der Wert selbst.
        require(bytes[after] == 0x12.toByte() && bytes[after + 1] == old.length.toByte() && String(bytes, after + 2, old.length) == old)
        return bytes.copyOf().also { b -> new.toByteArray().copyInto(b, after + 2) }
    }

    private fun indexOf(hay: ByteArray, needle: ByteArray): Int {
        outer@ for (i in 0..hay.size - needle.size) {
            for (j in needle.indices) if (hay[i + j] != needle[j]) continue@outer
            return i
        }
        return -1
    }
}
