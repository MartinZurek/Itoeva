package com.notime.glyphsim.skilltree

import com.notime.glyphcore.data.AnimationNode
import com.notime.glyphcore.data.AnimationTree
import com.notime.glyphsim.matrix.PlayEffects
import com.notime.glyphsim.matrix.PlayScene
import com.notime.glyphsim.matrix.RoutineStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft die Mechanik von Stufe 3 ([AvatarActivityPlans]).
 *
 * Das ist die Regel aus der Ausgangsfrage: "wenn er Fussball spielt und ich ihm Dribbling gebe,
 * dann macht er einen Dribbling-Trick". Sie liesse sich sonst nur durch Zuschauen beurteilen -
 * und beim Zuschauen faellt gerade der Fehler nicht auf, der hier am ehesten passiert: dass die
 * laufende Beschaeftigung nach jeder Einlage neu beginnt.
 */
class AvatarActivityPlansTest {

    private val now = 1_700_000_000_000L

    private fun node(id: String): AnimationNode = requireNotNull(AnimationTree.node(id)) { id }
    private fun running(id: String, ageMillis: Long = 0) = AvatarActivity(id, now - ageMillis)

    // ================= Eine Beschaeftigung ziehen =================

    @Test
    fun `eine Untergruppe zu ziehen faengt sie an`() {
        val plan = AvatarActivityPlans.planFor(null, node("sport/ballsport"))
        assertEquals(listOf(ActivityStep.Begin("sport/ballsport")), plan.steps)
        assertEquals("sport/ballsport", plan.resultingActivity)
        assertFalse(plan.isSwitch)
    }

    @Test
    fun `eine Hauptgruppe zu ziehen faengt sie ebenfalls an`() {
        val plan = AvatarActivityPlans.planFor(running("ruhe"), node("sport"))
        assertEquals(listOf(ActivityStep.Begin("sport")), plan.steps)
        assertEquals("sport", plan.resultingActivity)
    }

    /** Ein zweiter Zug auf dieselbe Sache ist ein Anstupsen, kein Nichts. */
    @Test
    fun `dieselbe Beschaeftigung noch einmal zu ziehen spielt sie erneut`() {
        val plan = AvatarActivityPlans.planFor(running("sport/ballsport"), node("sport/ballsport"))
        assertEquals(listOf(ActivityStep.Begin("sport/ballsport")), plan.steps)
    }

    // ================= Eine Einlage ziehen =================

    /** **Der Kernfall.** Er spielt Ball, bekommt Basketball - und spielt danach weiter Ball. */
    @Test
    fun `eine Einlage auf die passende Beschaeftigung schiebt sich nur ein`() {
        val plan = AvatarActivityPlans.planFor(
            running("sport/ballsport"),
            node("sport/ballsport/basketball")
        )
        assertEquals(listOf(ActivityStep.Flourish("sport/ballsport/basketball")), plan.steps)
        assertEquals(
            "Die laufende Beschaeftigung darf nicht beendet werden",
            "sport/ballsport",
            plan.resultingActivity
        )
        assertFalse(plan.isSwitch)
    }

    /** Wer schlaeft und einen Ball-Trick bekommt, steht auf und geht spielen - nicht im Bett. */
    @Test
    fun `eine Einlage auf etwas anderes wechselt erst die Beschaeftigung`() {
        val plan = AvatarActivityPlans.planFor(
            running("ruhe/schlafen"),
            node("sport/ballsport/basketball")
        )
        assertEquals(
            listOf(
                ActivityStep.Begin("sport/ballsport"),
                ActivityStep.Flourish("sport/ballsport/basketball")
            ),
            plan.steps
        )
        assertEquals("sport/ballsport", plan.resultingActivity)
        assertTrue(plan.isSwitch)
    }

    @Test
    fun `eine Einlage ohne laufende Beschaeftigung beginnt die zugehoerige`() {
        val plan = AvatarActivityPlans.planFor(null, node("ruhe/pause/candle"))
        assertEquals(
            listOf(ActivityStep.Begin("ruhe/pause"), ActivityStep.Flourish("ruhe/pause/candle")),
            plan.steps
        )
    }

    /**
     * Die Hauptgruppe zaehlt NICHT als passend: Fuer einen Ball-Trick braucht es einen Ball, und
     * "Sport" ist noch keiner.
     */
    @Test
    fun `die Hauptgruppe allein genuegt einer Einlage nicht`() {
        val plan = AvatarActivityPlans.planFor(running("sport"), node("sport/ballsport/basketball"))
        assertTrue("Erwartet wurde ein Wechsel zu Ballsport", plan.isSwitch)
        assertEquals("sport/ballsport", plan.resultingActivity)
    }

    // ================= Kontextuelle Fussballhandlung =================

    private fun footballPhases(resolved: ResolvedActivity): List<PlayEffects.FootballPhase> =
        resolved.routine.steps.filterIsInstance<RoutineStep.Football>().map { it.phase }

    @Test
    fun `derselbe Ballsport Impuls bleibt im Park lokal`() {
        val resolved = requireNotNull(
            AvatarActivityPlans.resolve(
                current = null,
                dropped = node("sport/ballsport"),
                context = ActivityContext(
                    place = PlayScene.Place.PARK,
                    unlockedNodeIds = setOf("sport", "sport/ballsport")
                )
            )
        )

        assertFalse(resolved.routine.steps.any { it is RoutineStep.GoToPlace })
        assertEquals(listOf(PlayEffects.FootballPhase.TOUCH), footballPhases(resolved))
    }

    @Test
    fun `derselbe Ballsport Impuls geht aus der Wohnung sichtbar zum Sportplatz`() {
        val resolved = requireNotNull(
            AvatarActivityPlans.resolve(
                current = null,
                dropped = node("sport/ballsport"),
                context = ActivityContext(
                    place = PlayScene.Place.LIVING,
                    unlockedNodeIds = setOf("sport", "sport/ballsport")
                )
            )
        )

        assertTrue(
            resolved.routine.steps.first() == RoutineStep.GoToPlace(PlayScene.Place.SPORT)
        )
    }

    @Test
    fun `Dribbling erscheint erst nach echter Freischaltung`() {
        val beginner = requireNotNull(
            AvatarActivityPlans.resolve(
                current = null,
                dropped = node("sport/ballsport"),
                context = ActivityContext(
                    place = PlayScene.Place.SPORT,
                    unlockedNodeIds = setOf("sport", "sport/ballsport")
                )
            )
        )
        val learned = requireNotNull(
            AvatarActivityPlans.resolve(
                current = null,
                dropped = node("sport/ballsport"),
                context = ActivityContext(
                    place = PlayScene.Place.SPORT,
                    unlockedNodeIds = setOf(
                        "sport",
                        "sport/ballsport",
                        "sport/ballsport/dribbling"
                    )
                )
            )
        )

        assertEquals(0, footballPhases(beginner).count { it == PlayEffects.FootballPhase.DRIBBLE })
        assertEquals(1, footballPhases(learned).count { it == PlayEffects.FootballPhase.DRIBBLE })
    }

    @Test
    fun `Dribbling Variante haengt nur an der Freischaltung nicht an einer erfundenen Levelschwelle`() {
        val resolved = requireNotNull(
            AvatarActivityPlans.resolve(
                current = null,
                dropped = node("sport/ballsport"),
                context = ActivityContext(
                    place = PlayScene.Place.SPORT,
                    unlockedNodeIds = setOf(
                        "sport",
                        "sport/ballsport",
                        "sport/ballsport/dribbling"
                    )
                )
            )
        )

        assertEquals(1, footballPhases(resolved).count { it == PlayEffects.FootballPhase.DRIBBLE })
    }

    @Test
    fun `Schuss und Zielen werden ohne Schuss Freischaltung nie benutzt`() {
        val locked = requireNotNull(
            AvatarActivityPlans.resolve(
                current = running("sport/ballsport"),
                dropped = node("sport/ballsport/schuss"),
                context = ActivityContext(
                    place = PlayScene.Place.SPORT,
                    unlockedNodeIds = setOf("sport", "sport/ballsport")
                )
            )
        )
        val unlocked = requireNotNull(
            AvatarActivityPlans.resolve(
                current = running("sport/ballsport"),
                dropped = node("sport/ballsport/schuss"),
                context = ActivityContext(
                    place = PlayScene.Place.SPORT,
                    unlockedNodeIds = setOf(
                        "sport",
                        "sport/ballsport",
                        "sport/ballsport/schuss"
                    )
                )
            )
        )

        assertFalse(footballPhases(locked).contains(PlayEffects.FootballPhase.AIM))
        assertFalse(footballPhases(locked).contains(PlayEffects.FootballPhase.KICK))
        assertTrue(footballPhases(unlocked).contains(PlayEffects.FootballPhase.AIM))
        assertTrue(footballPhases(unlocked).contains(PlayEffects.FootballPhase.KICK))
    }

    // ================= Zweiter Schnitt: Kraft & Ausdauer =================

    private fun strength(place: PlayScene.Place, unlocked: Set<String>, dropped: String) =
        AvatarActivityPlans.resolve(
            current = null,
            dropped = node(dropped),
            context = ActivityContext(place = place, unlockedNodeIds = unlocked)
        )

    private fun phases(resolved: ResolvedActivity?): List<PlayEffects.TrainingPhase> =
        resolved?.routine?.steps.orEmpty()
            .filterIsInstance<RoutineStep.Training>()
            .map { it.phase }

    /**
     * Die Basis darf jeder sehen - sonst waere ein Reminder auf "Kraft & Ausdauer" fuer einen
     * Anfaenger folgenlos. Gehoben wird deshalb aber noch lange nicht.
     */
    @Test
    fun `ohne Freischaltung waermt er nur auf und klingt aus`() {
        val resolved = strength(PlayScene.Place.SPORT, emptySet(), "sport/kraft-ausdauer")
        assertEquals(
            listOf(PlayEffects.TrainingPhase.WARM_UP, PlayEffects.TrainingPhase.REST),
            phases(resolved)
        )
    }

    /** Erst der echte Knoten bringt das Heben - nicht ein Level, nicht die Spezies. */
    @Test
    fun `Heben erscheint erst nach echter Freischaltung`() {
        val ohne = strength(
            PlayScene.Place.SPORT,
            setOf("sport", "sport/kraft-ausdauer"),
            "sport/kraft-ausdauer"
        )
        assertFalse(PlayEffects.TrainingPhase.LIFT in phases(ohne))

        val mit = strength(
            PlayScene.Place.SPORT,
            setOf("sport", "sport/kraft-ausdauer", "sport/kraft-ausdauer/heben"),
            "sport/kraft-ausdauer"
        )
        assertEquals(
            listOf(
                PlayEffects.TrainingPhase.WARM_UP,
                PlayEffects.TrainingPhase.LIFT,
                PlayEffects.TrainingPhase.REST
            ),
            phases(mit)
        )
    }

    /**
     * Der Abschluss haengt an keiner Freischaltung: Aufwaermen ohne Ausklang waere ein Stumpf -
     * man saehe jemanden anfangen und dann abbrechen.
     */
    @Test
    fun `der Ausklang kommt in jeder Ausbaustufe`() {
        for (unlocked in listOf(
            emptySet<String>(),
            setOf("sport/kraft-ausdauer"),
            setOf("sport/kraft-ausdauer", "sport/kraft-ausdauer/heben")
        )) {
            val resolved = strength(PlayScene.Place.SPORT, unlocked, "sport/kraft-ausdauer")
            assertEquals(
                "Ausklang fehlt bei $unlocked",
                PlayEffects.TrainingPhase.REST,
                phases(resolved).last()
            )
        }
    }

    /** Wiese und Park taugen genauso wie der Sportplatz - kein Ortswechsel, kein Teleport. */
    @Test
    fun `im Park wird ohne Ortswechsel trainiert`() {
        val resolved = strength(PlayScene.Place.PARK, emptySet(), "sport/kraft-ausdauer")
        assertTrue(
            resolved?.routine?.steps.orEmpty().none { it is RoutineStep.GoToPlace }
        )
    }

    /** Drinnen fuehrt der sichtbare Weg hinaus, statt Hanteln ins Wohnzimmer zu zaubern. */
    @Test
    fun `aus der Wohnung geht er sichtbar zum Sportplatz`() {
        val resolved = strength(PlayScene.Place.LIVING, emptySet(), "sport/kraft-ausdauer")
        assertEquals(
            RoutineStep.GoToPlace(PlayScene.Place.SPORT),
            resolved?.routine?.steps?.first()
        )
    }

    /** Das Blatt "heben" ist eine Einlage - es beginnt seine Beschaeftigung mit. */
    @Test
    fun `das Blatt heben loest ebenfalls kontextuell auf`() {
        val resolved = strength(
            PlayScene.Place.SPORT,
            setOf("sport/kraft-ausdauer", "sport/kraft-ausdauer/heben"),
            "sport/kraft-ausdauer/heben"
        )
        assertEquals("sport/kraft-ausdauer", resolved?.plan?.resultingActivity)
        assertTrue(PlayEffects.TrainingPhase.LIFT in phases(resolved))
    }

    /**
     * Beide Schnitte haengen unter `sport`, dessen eingebauter Typ MOVE ist - der Baum sagt es
     * bereits, hier wird kein eigener Typ erfunden.
     */
    @Test
    fun `beide Schnitte melden das Thema ihrer Hauptgruppe`() {
        val ball = AvatarActivityPlans.resolve(
            null, node("sport/ballsport"),
            ActivityContext(PlayScene.Place.SPORT, setOf("sport/ballsport"))
        )
        val kraft = strength(PlayScene.Place.SPORT, setOf("sport/kraft-ausdauer"), "sport/kraft-ausdauer")
        assertEquals(com.notime.glyphcore.data.AnimationType.MOVE, ball?.topic)
        assertEquals(com.notime.glyphcore.data.AnimationType.MOVE, kraft?.topic)
    }

    // ================= Einlage in eine laufende Beschaeftigung =================

    /**
     * `planFor` sagt es bereits: Laeuft der Wirt schon, kommt nur ein `Flourish` und kein `Begin`.
     * Die Ausfuehrung hat das lange ignoriert und trotzdem Hinweg, Anker und Basisphase
     * vorangestellt - wer beim Ballspielen ein Dribbling bekam, sah ihn erneut hinlaufen und den
     * Ball erstmals beruehren. Genau das soll nicht mehr passieren.
     */
    @Test
    fun `ein Dribbling in laufenden Ballsport faengt nicht von vorn an`() {
        val resolved = AvatarActivityPlans.resolve(
            current = running("sport/ballsport"),
            dropped = node("sport/ballsport/dribbling"),
            context = ActivityContext(
                PlayScene.Place.SPORT,
                setOf("sport/ballsport", "sport/ballsport/dribbling")
            )
        )
        val steps = resolved?.routine?.steps.orEmpty()
        assertTrue("kein neuer Hinweg", steps.none { it is RoutineStep.GoToPlace })
        assertFalse(
            "keine erneute Ballberuehrung",
            steps.filterIsInstance<RoutineStep.Football>()
                .any { it.phase == PlayEffects.FootballPhase.TOUCH }
        )
        assertTrue(
            "das Dribbling selbst kommt",
            steps.filterIsInstance<RoutineStep.Football>()
                .any { it.phase == PlayEffects.FootballPhase.DRIBBLE }
        )
    }

    /** Dieselbe Regel bei Kraft & Ausdauer - sie liegt gemeinsam, nicht zweimal. */
    @Test
    fun `ein Heben in laufendes Training faengt nicht von vorn an`() {
        val resolved = AvatarActivityPlans.resolve(
            current = running("sport/kraft-ausdauer"),
            dropped = node("sport/kraft-ausdauer/heben"),
            context = ActivityContext(
                PlayScene.Place.SPORT,
                setOf("sport/kraft-ausdauer", "sport/kraft-ausdauer/heben")
            )
        )
        assertEquals(listOf(PlayEffects.TrainingPhase.LIFT), phases(resolved))
    }

    /**
     * Ohne laufende Beschaeftigung bleibt der volle Ablauf - die Abkuerzung gilt nur fuer das
     * Einschieben, nicht fuer den Anfang.
     */
    @Test
    fun `ohne laufende Beschaeftigung bleibt der volle Ablauf`() {
        val resolved = AvatarActivityPlans.resolve(
            current = null,
            dropped = node("sport/kraft-ausdauer/heben"),
            context = ActivityContext(
                PlayScene.Place.SPORT,
                setOf("sport/kraft-ausdauer", "sport/kraft-ausdauer/heben")
            )
        )
        assertEquals(
            listOf(
                PlayEffects.TrainingPhase.WARM_UP,
                PlayEffects.TrainingPhase.LIFT,
                PlayEffects.TrainingPhase.REST
            ),
            phases(resolved)
        )
    }

    /**
     * Ein Impuls fuer eine NICHT freigeschaltete Faehigkeit gaebe als reine Einlage eine Routine
     * ohne einen einzigen Schritt. Dann ist der volle Ablauf richtig - er zeigt wenigstens die
     * Basis.
     */
    @Test
    fun `eine ungelernte Einlage faellt auf den vollen Ablauf zurueck`() {
        val resolved = AvatarActivityPlans.resolve(
            current = running("sport/kraft-ausdauer"),
            dropped = node("sport/kraft-ausdauer/heben"),
            context = ActivityContext(PlayScene.Place.SPORT, setOf("sport/kraft-ausdauer"))
        )
        assertEquals(
            listOf(PlayEffects.TrainingPhase.WARM_UP, PlayEffects.TrainingPhase.REST),
            phases(resolved)
        )
    }

    /** Die Gipfel-/Leiter-/Fahnen-Blaetter bleiben beim bisherigen Reaktionsweg. */
    @Test
    fun `die uebrigen Kraft Blaetter bleiben ausserhalb des Schnitts`() {
        for (id in listOf(
            "sport/kraft-ausdauer/summit",
            "sport/kraft-ausdauer/ladder",
            "sport/kraft-ausdauer/flag"
        )) {
            assertNull(
                id,
                strength(PlayScene.Place.SPORT, setOf("sport/kraft-ausdauer", id), id)
            )
        }
    }

    @Test
    fun `Basketball bleibt ausserhalb des Fussball Vertikalschnitts`() {
        assertNull(
            AvatarActivityPlans.resolve(
                current = null,
                dropped = node("sport/ballsport/basketball"),
                context = ActivityContext(
                    place = PlayScene.Place.SPORT,
                    unlockedNodeIds = setOf("sport", "sport/ballsport", "sport/ballsport/basketball")
                )
            )
        )
    }

    // ================= Kein Flackern =================

    /**
     * Ein Plan darf hoechstens EINEN Wechsel enthalten und niemals zwischen Beschaeftigungen hin
     * und her springen - sonst saehe man statt einer Bewegung ein Flackern.
     */
    @Test
    fun `kein Plan enthaelt mehr als einen Wechsel`() {
        val zustaende = listOf(null, running("ruhe/schlafen"), running("sport/ballsport"), running("lernen"))
        for (current in zustaende) {
            for (node in AnimationTree.nodes) {
                val plan = AvatarActivityPlans.planFor(current, node)
                assertTrue(
                    "${node.id}: Plan mit ${plan.steps.size} Schritten",
                    plan.steps.size <= 2
                )
                assertTrue(
                    "${node.id}: mehr als ein Beginn im Plan",
                    plan.steps.count { it is ActivityStep.Begin } <= 1
                )
                assertTrue(
                    "${node.id}: ein Plan darf nicht mit einem Beginn enden, wenn eine Einlage gezogen wurde",
                    node.kind == AnimationNode.Kind.ACTIVITY ||
                        plan.steps.last() is ActivityStep.Flourish
                )
            }
        }
    }

    /**
     * Jede Einlage muss eine Beschaeftigung haben, in der sie stattfinden kann - sonst gaebe es
     * Knoten, die man zieht und bei denen nichts Sinnvolles passieren kann.
     */
    @Test
    fun `jede Einlage haengt unter einer echten Beschaeftigung`() {
        val ohneWirt = AnimationTree.nodes
            .filter { it.kind == AnimationNode.Kind.FLOURISH }
            .filter { node ->
                val wirt = AvatarActivityPlans.hostActivityOf(node)
                wirt == null || wirt.kind != AnimationNode.Kind.ACTIVITY
            }
            .map { it.id }
        assertEquals(emptyList<String>(), ohneWirt)
    }

    @Test
    fun `eine Beschaeftigung hat keinen Wirt`() {
        assertNull(AvatarActivityPlans.hostActivityOf(node("sport")))
        assertNull(AvatarActivityPlans.hostActivityOf(node("sport/ballsport")))
        assertEquals(
            "sport/ballsport",
            AvatarActivityPlans.hostActivityOf(node("sport/ballsport/basketball"))?.id
        )
    }

    // ================= Ablauf =================

    @Test
    fun `eine frische Beschaeftigung laeuft noch`() {
        assertFalse(running("sport", ageMillis = 60_000).isStale(now))
    }

    @Test
    fun `nach fuenf Minuten ist eine Beschaeftigung abgelaufen`() {
        assertTrue(running("sport", ageMillis = AvatarActivity.LIFETIME_MS).isStale(now))
    }

    /**
     * Abgelaufen heisst: Eine Einlage findet sie nicht mehr vor und beginnt neu. Der Aufrufer
     * uebergibt sie gar nicht erst - hier steht, dass das den erwarteten Unterschied macht.
     */
    @Test
    fun `eine abgelaufene Beschaeftigung zaehlt nicht mehr als passend`() {
        val frisch = AvatarActivityPlans.planFor(
            running("sport/ballsport"),
            node("sport/ballsport/basketball")
        )
        val abgelaufen = AvatarActivityPlans.planFor(null, node("sport/ballsport/basketball"))
        assertFalse(frisch.isSwitch)
        assertTrue(abgelaufen.isSwitch)
    }
}
