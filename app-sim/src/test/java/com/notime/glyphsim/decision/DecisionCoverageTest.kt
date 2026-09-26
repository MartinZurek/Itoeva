package com.notime.glyphsim.decision

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.matrix.LivingRuntimeAdapter
import com.notime.glyphsim.matrix.PlayGroupGame
import com.notime.glyphsim.matrix.PlayRoutine
import com.notime.glyphsim.matrix.PlayRoutines
import com.notime.glyphsim.matrix.PlayScene
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Keine sichtbare Aktion liegt still ausserhalb der Decision Policy.**
 *
 * Statt einer Liste von Aktivitaeten, die veralten koennte, traversiert dieser Test die Typen,
 * aus denen die Welt ihre Ablaeufe tatsaechlich bezieht: jedes Thema ([AnimationType]) mit allen
 * Ablaeufen aus [PlayRoutines.allFor], jede Gruppenspielart ([PlayGroupGame.Kind]), jede
 * Sonderaktivitaet ([PlayRoutines.SpecialActivity]) und jede Kernhandlung ([ActionKind]). Ueber
 * viele simulierte Tage aller sechs Wesen muss jedes davon mindestens einmal als Kandidat
 * auftauchen. Wer einen neuen Ablauf, ein neues Spiel oder eine neue Handlung einbaut, die die
 * Policy nie zu sehen bekaeme, sieht es hier - es sei denn, er nimmt sie mit Begruendung in eine
 * der Ausnahmelisten auf.
 */
class DecisionCoverageTest {

    /**
     * Die einzigen bewusst ausgeschlossenen Ablaeufe: Medizin ist nie autonom (Klassendoku von
     * [com.notime.glyphsim.matrix.PlayAmbientActivity]). Sie laeuft nur als beantwortete Erinnerung.
     */
    private val bewusstAusgeschlossen = setOf(AnimationType.MEDICINE)

    /**
     * Kernhandlungen, die kein eigener sichtbarer Ablauf sind: Zwischenschritte eines Plans
     * (nachsehen), Wirkungen einer schon laufenden Begegnung (antworten, wahrnehmen, gemeinsam
     * trainiert haben) und Fuersorge (= Medizin, siehe oben).
     */
    private val keinEigenerAblauf = setOf(
        ActionKind.INSPECT_FOOD,
        ActionKind.RESPOND_TO_INVITE,
        ActionKind.RECEIVE_RESPONSE,
        ActionKind.TRAIN_TOGETHER,
        ActionKind.TEND_SELF
    )

    private val lagen by lazy {
        DecisionTestSupport.sweep(days = 10, seeds = 2) + besondereLagen()
    }

    private val kandidaten by lazy { lagen.flatMap { it.second } }

    /** Zwei Lagen, die ein Tag ohne Gaeste nie herstellt: ein Gast im Wohnzimmer, Einsamkeit. */
    private fun besondereLagen(): List<Pair<DecisionState, List<ActionCandidate>>> {
        val gast = DecisionTestSupport.state(
            minuteOfDay = 19 * 60,
            needs = mapOf(NeedKind.SOCIAL to 0.9)
        ).let { it.copy(nearbyProfiles = setOf("resident:park:puffling"), world = it.world.copy(nearbyProfiles = setOf("resident:park:puffling"))) }
        return listOf(gast to DecisionCandidates.generate(gast))
    }

    /** Die Aktivitaetsschritte eines Ablaufs - bleiben bei Ortsvorsatz und Arbeitsweg-Zuschnitt erhalten. */
    private fun signature(routine: PlayRoutine) = routine.steps.filter { ActionTraits.isActivity(it) }

    @Test
    fun `jeder sichtbare Ablauf ist Kandidat`() {
        val gesehen = kandidaten.map { signature(it.routine) }.toSet() +
            kandidaten.map { signature(it.visibleRoutine) }.toSet()
        val fehlend = mutableListOf<String>()
        for (topic in AnimationType.entries) {
            if (topic in bewusstAusgeschlossen) continue
            val ablaeufe = PlayRoutines.allFor(topic) +
                if (topic == AnimationType.MOVE) listOf(PlayRoutines.footballRoutine(trickLearned = true)) else emptyList()
            ablaeufe.forEachIndexed { i, r ->
                // Der Trick braucht ein gelerntes Kunststueck - die Tage hier lernen keins.
                if (r == PlayRoutines.footballRoutine(true)) return@forEachIndexed
                if (signature(r) !in gesehen) fehlend += "$topic #$i"
            }
        }
        assertEquals("nie als Kandidat gesehen: $fehlend", emptyList<String>(), fehlend)
    }

    @Test
    fun `der Fussballtrick wird Kandidat, sobald er gelernt ist`() {
        val state = DecisionTestSupport.state(needs = mapOf(NeedKind.FUN to 0.9), minuteOfDay = 17 * 60)
            .copy(footballTrickLearned = true)
        val alle = DecisionCandidates.generate(state)
        assertTrue(alle.any { it.routine.steps.containsAll(PlayRoutines.footballRoutine(true).steps) })
    }

    @Test
    fun `jede Gruppenspielart ist Kandidat`() {
        val arten = kandidaten.mapNotNull { it.groupGame }.toSet()
        assertEquals(PlayGroupGame.Kind.entries.toSet(), arten)
    }

    @Test
    fun `jede Sonderaktivitaet ist Kandidat`() {
        val gesehen = kandidaten.mapNotNull { PlayRoutines.specialOf(it.routine) }.toSet()
        assertEquals(PlayRoutines.SpecialActivity.entries.toSet(), gesehen)
    }

    @Test
    fun `jede sichtbare Kernhandlung erreicht die Policy`() {
        val gesehen = kandidaten.map { it.coreAction }.toSet()
        val fehlend = ActionKind.entries.filter { it !in gesehen && it !in keinEigenerAblauf }
        assertEquals("Kernhandlung ohne Kandidat: $fehlend", emptyList<ActionKind>(), fehlend)
    }

    /**
     * Jedes Ziel, das der Kern ueberhaupt waehlt, kommt als Kandidat vor. `EARN_MONEY` gewinnt
     * im Kern nie (NT-087: Muenzen bezahlen heute nur Essen) - Arbeit erscheint deshalb unter
     * `GET_FOOD` (arbeiten, einkaufen, essen), und das prueft der Kernhandlungstest oben.
     */
    @Test
    fun `jedes Ziel des Kerns erreicht die Policy`() {
        val gesehen = kandidaten.mapNotNull { it.goal }.toSet()
        assertEquals(GoalKind.entries.toSet() - GoalKind.EARN_MONEY, gesehen - GoalKind.EARN_MONEY)
        assertTrue(kandidaten.any { it.coreAction == ActionKind.WORK })
    }

    /** Jeder Ort, an dem ein Ablauf spielt, kommt auch als Ort eines Kandidaten vor. */
    @Test
    fun `jeder bespielte Ort kommt vor`() {
        val orte = kandidaten.map { it.place }.toSet()
        val bespielt = AnimationType.entries.filter { it !in bewusstAusgeschlossen }
            .flatMap { PlayRoutines.allFor(it) }
            .flatMap { r -> r.steps.filterIsInstance<com.notime.glyphsim.matrix.RoutineStep.GoToPlace>().map { it.place } }
            .toSet()
        val fehlend = bespielt.filter { ort ->
            ort !in orte && kandidaten.none { k -> k.routine.steps.any { it is com.notime.glyphsim.matrix.RoutineStep.GoToPlace && it.place == ort } }
        }
        assertEquals(emptyList<PlayScene.Place>(), fehlend)
    }

    /** Die Kandidatenquelle nennt nur Ablaeufe, die der Adapter danach auch wirklich ausfuehrt. */
    @Test
    fun `jede Vorwahl wird vom Adapter uebernommen`() {
        for ((state, alle) in lagen.take(200)) {
            for (k in alle) {
                val prepared = LivingRuntimeAdapter.prepare(
                    state.agent, state.world, state.currentPlace, k.interestTopic,
                    footballTrickLearned = state.footballTrickLearned,
                    recentSpecials = state.recentSpecials,
                    nearbyProfiles = state.nearbyProfiles,
                    preferredRoutine = k.routine,
                    sleepAdmissible = state.phase == com.notime.glyphsim.matrix.PlayAmbientActivity.DayPhase.NIGHT,
                    chosenGoal = k.goal
                )
                assertEquals("${k.key} goal ${k.goal}", k.routine, prepared.routine)
                assertEquals(k.topic, prepared.topic)
            }
        }
    }
}
