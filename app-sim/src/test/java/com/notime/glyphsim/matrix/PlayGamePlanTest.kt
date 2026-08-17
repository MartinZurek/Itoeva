package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.ui.PlayModeXp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regressionstests fuer die Spielplaene ([PlayGamePlan]/[PlayGamePlans]) - die "Handschrift" jedes
 * Avatars, wie sie in der Klassendoku von [PlayGamePlan] beschrieben ist.
 *
 * Wichtig: das Spieltempo wird hier gegen die TATSAECHLICH dokumentierte Spanne (2 bis 16 Minuten,
 * abgeleitet aus den Kommentaren je Avatar - Wyrmling am schnellsten mit 2 Minuten, Gloops letzte
 * Stufe am langsamsten mit 16) geprueft, NICHT gegen [com.notime.glyphcore.data.ReminderValidation.INTERVAL_RANGE].
 * Diese generische DB-Validierungsgrenze (1..1440 Minuten) sagt nichts ueber das gewuenschte
 * Spieltempo aus - sie wuerde jeden Wert von 1 bis 24 Stunden klaglos durchlassen.
 */
class PlayGamePlanTest {

    private fun allPlans(): Map<AvatarSpecies, PlayGamePlan> =
        AvatarSpecies.entries.associateWith { PlayGamePlans.forSpecies(it) }

    @Test
    fun medicineNeverAppearsInAnyPlan() {
        for ((species, plan) in allPlans()) {
            for (stage in plan.stages) {
                assertFalse(
                    "$species/Stufe ab Level ${stage.fromLevel}: MEDICINE darf in keinem Spielplan " +
                        "vorkommen - eine zufaellige Spiel-Ausloesung darf nie wie eine echte " +
                        "Medikamenten-Erinnerung wirken.",
                    AnimationType.MEDICINE in stage.topicWeights
                )
            }
        }
    }

    @Test
    fun everyStageHasOnlyPositiveWeights() {
        for ((species, plan) in allPlans()) {
            for (stage in plan.stages) {
                for ((topic, weight) in stage.topicWeights) {
                    assertTrue(
                        "$species/Stufe ab Level ${stage.fromLevel}: Gewicht fuer $topic ist $weight, " +
                            "muss positiv sein - ein Gewicht <= 0 wuerde das Thema entweder nie oder " +
                            "unsinnig ziehen.",
                        weight > 0
                    )
                }
            }
        }
    }

    @Test
    fun fromLevelsAreStrictlyIncreasingAndUnique() {
        for ((species, plan) in allPlans()) {
            val levels = plan.stages.map { it.fromLevel }
            assertEquals(
                "$species: fromLevel-Werte sind nicht eindeutig ($levels) - zwei Stufen wuerden " +
                    "sich sonst um dieselben Level streiten.",
                levels.size,
                levels.toSet().size
            )
            assertEquals(
                "$species: Stufen sind nicht streng aufsteigend nach fromLevel sortiert ($levels)",
                levels,
                levels.sorted()
            )
        }
    }

    /**
     * Jedes ueber [PlayModeXp.levelFor] erreichbare Level muss sich zu einer eindeutigen, naechst-
     * liegenden Stufe aufloesen lassen - ausgedrueckt als Eigenschaft (keine spaetere Stufe greift
     * ebenfalls noch), nicht als Nachbau der `stageFor`-Formel selbst.
     */
    @Test
    fun stageForResolvesEveryReachableLevelToTheClosestPrecedingStage() {
        val reachableLevels = (0..2000 step 10).map { xp -> PlayModeXp.levelFor(xp) }.toSet()
        for ((species, plan) in allPlans()) {
            for (level in reachableLevels) {
                val stage = plan.stageFor(level)
                assertTrue(
                    "$species: stageFor($level) liefert eine Stufe ab Level ${stage.fromLevel}, " +
                        "die erst spaeter gilt.",
                    stage.fromLevel <= level
                )
                assertTrue(
                    "$species: stageFor($level) hat nicht die naechstliegende Stufe gewaehlt - es " +
                        "gibt eine spaetere Stufe, die bei Level $level ebenfalls schon gelten wuerde.",
                    plan.stages.none { it.fromLevel > stage.fromLevel && it.fromLevel <= level }
                )
            }
        }
    }

    @Test
    fun higherStagesNeverShrinkTheTopicSpace() {
        for ((species, plan) in allPlans()) {
            for (i in 0 until plan.stages.lastIndex) {
                val current = plan.stages[i].topicWeights.keys
                val next = plan.stages[i + 1].topicWeights.keys
                assertTrue(
                    "$species: Stufe ab Level ${plan.stages[i + 1].fromLevel} verliert Themen aus der " +
                        "vorherigen Stufe (fehlend: ${current - next}) - mit steigendem Level soll sich " +
                        "der Avatar oeffnen, nicht verengen.",
                    next.containsAll(current)
                )
            }
        }
    }

    @Test
    fun signatureTopicLeadsFromTheFirstStage() {
        for (species in AvatarSpecies.entries) {
            val firstStage = PlayGamePlans.forSpecies(species).stages.first()
            val signature = species.signatureTopic
            assertTrue(
                "$species: Signatur-Thema $signature kommt in der ersten Stufe gar nicht vor",
                signature in firstStage.topicWeights
            )
            val maxWeight = firstStage.topicWeights.values.max()
            assertEquals(
                "$species: Signatur-Thema $signature ist in der ersten Stufe nicht das fuehrende Thema " +
                    "(Gewichte: ${firstStage.topicWeights})",
                maxWeight,
                firstStage.topicWeights.getValue(signature)
            )
        }
    }

    @Test
    fun signatureTopicStaysPresentThroughEveryStage() {
        for (species in AvatarSpecies.entries) {
            val plan = PlayGamePlans.forSpecies(species)
            for (stage in plan.stages) {
                assertTrue(
                    "$species: Signatur-Thema ${species.signatureTopic} fehlt (oder hat Gewicht 0) ab " +
                        "Level ${stage.fromLevel} - es soll dauerhaft vorhanden bleiben.",
                    (stage.topicWeights[species.signatureTopic] ?: 0) > 0
                )
            }
        }
    }

    // ---- Spieltempo: tatsaechlich dokumentierte Spanne 2 bis 16 Minuten (siehe Klassenkommentar oben) ----

    @Test
    fun documentedPaceStaysWithinTwoToSixteenMinutes() {
        for ((species, plan) in allPlans()) {
            for (stage in plan.stages) {
                assertTrue(
                    "$species/Stufe ab Level ${stage.fromLevel}: untere Intervallgrenze " +
                        "${stage.intervalMinutes.first} liegt unter der dokumentierten Spieltempo-" +
                        "Untergrenze von $DOCUMENTED_FASTEST_MINUTES Minuten (Wyrmling ist der schnellste Plan).",
                    stage.intervalMinutes.first >= DOCUMENTED_FASTEST_MINUTES
                )
                assertTrue(
                    "$species/Stufe ab Level ${stage.fromLevel}: obere Intervallgrenze " +
                        "${stage.intervalMinutes.last} liegt ueber der dokumentierten Spieltempo-" +
                        "Obergrenze von $DOCUMENTED_SLOWEST_MINUTES Minuten.",
                    stage.intervalMinutes.last <= DOCUMENTED_SLOWEST_MINUTES
                )
            }
        }
    }

    @Test
    fun documentedPaceExtremesAreActuallyReached() {
        val bounds = allPlans().values.flatMap { it.stages }
            .flatMap { listOf(it.intervalMinutes.first, it.intervalMinutes.last) }
        assertEquals(
            "Kein Plan erreicht mehr die dokumentierte schnellste Wartezeit von " +
                "$DOCUMENTED_FASTEST_MINUTES Minuten.",
            DOCUMENTED_FASTEST_MINUTES,
            bounds.min()
        )
        assertEquals(
            "Kein Plan erreicht mehr die dokumentierte langsamste Wartezeit von " +
                "$DOCUMENTED_SLOWEST_MINUTES Minuten.",
            DOCUMENTED_SLOWEST_MINUTES,
            bounds.max()
        )
    }

    // ---- Fennec: engste Intervall-SPANNE aller Plaene - nicht die schnellste, das ist Wyrmling ----

    /**
     * Vergleicht die Spanne (Breite) jeder Stufe von Fennec mit der jeweils passenden Stufe ALLER
     * anderen Plaene - nicht nur mit `stages.first()`, damit ein Regress, der erst in einer
     * spaeteren Stufe auftritt, nicht uebersehen wird.
     */
    @Test
    fun fennecHasTheNarrowestIntervalSpanAtEveryStage() {
        val fennecStages = PlayGamePlans.forSpecies(AvatarSpecies.FENNEC).stages
        val otherPlans = AvatarSpecies.entries.filter { it != AvatarSpecies.FENNEC }
            .associateWith { PlayGamePlans.forSpecies(it) }
        for (index in fennecStages.indices) {
            val fennecWidth = fennecStages[index].intervalMinutes.let { it.last - it.first }
            for ((species, plan) in otherPlans) {
                val stage = plan.stages.getOrNull(index) ?: continue
                val width = stage.intervalMinutes.let { it.last - it.first }
                assertTrue(
                    "Fennecs Spanne ($fennecWidth) ist an Stufenindex $index nicht schmaler oder " +
                        "gleich der von $species ($width) - dokumentiert ist Fennec als engste, " +
                        "gleichmaessigste Spanne aller Plaene.",
                    fennecWidth <= width
                )
            }
        }
    }

    /** Wyrmling ist laut Dokumentation der SCHNELLSTE Plan (kuerzeste Wartezeit), nicht Fennec. */
    @Test
    fun wyrmlingWaitsShorterThanFennecAtEveryStage() {
        val wyrmlingStages = PlayGamePlans.forSpecies(AvatarSpecies.WYRMLING).stages
        val fennecStages = PlayGamePlans.forSpecies(AvatarSpecies.FENNEC).stages
        for (index in wyrmlingStages.indices) {
            val wyrmling = wyrmlingStages[index].intervalMinutes
            val fennec = fennecStages.getOrNull(index)?.intervalMinutes ?: continue
            assertTrue(
                "Wyrmling (${wyrmling.first}..${wyrmling.last}) soll an Stufenindex $index eine " +
                    "kuerzere Mindestwartezeit haben als Fennec (${fennec.first}..${fennec.last}) - " +
                    "Wyrmling ist der schnellste Plan, Fennec nur der gleichmaessigste.",
                wyrmling.first < fennec.first
            )
        }
    }

    // ---- Dokumentierte Unterschiede zwischen Fennec und Gloop ----

    @Test
    fun fennecAndGloopDifferInPaceAndConsistency() {
        val fennecStages = PlayGamePlans.forSpecies(AvatarSpecies.FENNEC).stages
        val gloopStages = PlayGamePlans.forSpecies(AvatarSpecies.GLOOP).stages

        for (index in fennecStages.indices) {
            val fennec = fennecStages[index].intervalMinutes
            val gloop = gloopStages.getOrNull(index)?.intervalMinutes ?: continue
            assertTrue(
                "Gloop (${gloop.first}..${gloop.last}) soll an Stufenindex $index langsamer sein als " +
                    "Fennec (${fennec.first}..${fennec.last}) - dokumentiert als 'langsam' " +
                    "(Entschleuniger) gegenueber 'haeufig, aber gleichmaessig' (Beschuetzer).",
                gloop.first > fennec.first && gloop.last > fennec.last
            )
        }

        val fennecLastWeights = fennecStages.last().topicWeights.values.toSet()
        val gloopLastWeights = gloopStages.last().topicWeights.values.toSet()
        assertTrue(
            "Fennec soll auch in der letzten Stufe klar abgestufte Gewichte behalten (vorhersehbarer " +
                "Takt), hat aber nur noch ein einziges Gewicht: $fennecLastWeights",
            fennecLastWeights.size > 1
        )
        assertEquals(
            "Gloops letzte Stufe ist dokumentiert als 'alles gleich wahrscheinlich' - also ein " +
                "einziges gemeinsames Gewicht fuer alle Themen, tatsaechlich: $gloopLastWeights",
            1,
            gloopLastWeights.size
        )
    }

    private companion object {
        /** Wyrmlings schnellste Untergrenze (siehe Kommentar "Schnellstes Tempo" ueber [PlayGamePlans]). */
        const val DOCUMENTED_FASTEST_MINUTES = 2

        /** Gloops langsamste Obergrenze in der letzten Stufe (siehe Kommentar "Langsam" ueber [PlayGamePlans]). */
        const val DOCUMENTED_SLOWEST_MINUTES = 16
    }
}
