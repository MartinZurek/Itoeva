package com.notime.glyphsim.skilltree

import com.notime.glyphcore.data.AnimationNode
import com.notime.glyphcore.data.AnimationTree
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.matrix.PlayEffects
import com.notime.glyphsim.matrix.PlayRoutine
import com.notime.glyphsim.matrix.PlayScene
import com.notime.glyphsim.matrix.RoutineStep

/**
 * Was der Begleiter gerade tut - und seit wann.
 *
 * **Der Zustand, ohne den es Stufe 3 nicht geben kann.** Ein Blatt wie "Dribbling" ist keine
 * Beschaeftigung, der man nachgehen kann; es ist etwas, das man TUT, WAEHREND man Ball spielt.
 * Ohne ein "gerade laeuft Ballsport" haette eine Einlage nichts, worin sie stattfinden koennte,
 * und der Unterschied zwischen den Ebenen waere nur eine Einrueckung in der Anzeige.
 */
data class AvatarActivity(val nodeId: String, val sinceMillis: Long) {

    /**
     * Ob die Beschaeftigung abgelaufen ist.
     *
     * **Warum sie ueberhaupt ablaeuft:** Ohne Verfall wuerde der Begleiter noch Stunden spaeter
     * als "spielt Ball" gelten, und eine Einlage waere dann eine Antwort auf etwas, das laengst
     * vorbei ist. Wer die App am Morgen kurz benutzt und am Abend wiederkommt, erwartet keinen
     * Anschluss an den Vormittag.
     */
    fun isStale(nowMillis: Long): Boolean =
        nowMillis - sinceMillis >= LIFETIME_MS

    companion object {
        /**
         * Wie lange eine Beschaeftigung nachwirkt.
         *
         * Fuenf Minuten: lang genug, dass mehrere Zuege in einer Sitzung zusammenhaengen, kurz
         * genug, dass nichts ueber eine Pause hinweg stehen bleibt.
         */
        const val LIFETIME_MS = 5L * 60 * 1000
    }
}

/** Ein Schritt in dem, was nach einem Zug aus der Leiste abgespielt wird. */
sealed interface ActivityStep {
    val nodeId: String

    /** Der Begleiter faengt diese Beschaeftigung an - Stufe 1 oder 2. */
    data class Begin(override val nodeId: String) : ActivityStep

    /** Die Einlage in die laufende Beschaeftigung - Stufe 3. */
    data class Flourish(override val nodeId: String) : ActivityStep
}

/**
 * Was abgespielt wird, und was danach laeuft.
 *
 * [steps] ist bewusst eine Liste und kein einzelner Schritt: Wer eine Einlage auf eine unpassende
 * Beschaeftigung zieht, soll den Wechsel UND die Einlage in einem Zug sehen. Als zwei getrennte
 * Aufrufe waere dazwischen die Ruhelage zu sehen, und aus einer Bewegung wuerden zwei.
 */
data class ActivityPlan(
    val steps: List<ActivityStep>,
    /** Die Beschaeftigung, die danach laeuft. */
    val resultingActivity: String
) {
    /** Ob dafuer erst die Beschaeftigung gewechselt werden musste. */
    val isSwitch: Boolean get() = steps.size > 1
}

/**
 * Der kleine Ausschnitt aus der Welt, den eine Absicht fuer ihre konkrete Ausfuehrung braucht.
 *
 * Kein zweiter Weltzustand: [place] kommt direkt aus `DockScreen.currentPlace`, die Freischaltungen
 * aus [AvatarUnlockRepository], die Stufe aus dem vorhandenen Play-Mode-Level. Die Ausfuehrung
 * bleibt vollstaendig bei [PlayRoutine] und `DockScreen.runRoutine`.
 */
data class ActivityContext(
    val place: PlayScene.Place,
    val unlockedNodeIds: Set<String>,
    val avatarLevel: Int
)

/**
 * Ergebnis der bestehenden Aktivitaetsentscheidung plus die Routine, die sie in der Welt sichtbar
 * macht. [plan] bleibt die semantische Wahrheit (Begin/Flourish); [routine] ist nur ihre vorhandene
 * Ausfuehrungsebene.
 */
data class ResolvedActivity(
    val plan: ActivityPlan,
    val topic: AnimationType,
    val routine: PlayRoutine
)

/**
 * **Die Regeln fuer "was passiert, wenn ich das auf den Begleiter ziehe".**
 *
 * Reine Rechnung ohne Compose, Datenbank und Coroutinen - dadurch laesst sich das Verhalten
 * pruefen, das sich sonst nur durch Zuschauen beurteilen liesse. Und genau hier steckt die
 * Mechanik, um die es bei Stufe 3 geht.
 */
object AvatarActivityPlans {

    /**
     * [current] ist die laufende Beschaeftigung, oder `null`, wenn keine laeuft. Abgelaufene
     * uebergibt der Aufrufer gar nicht erst (siehe [AvatarActivityBus.currentIfFresh]).
     *
     * ## Die drei Faelle
     *
     * - **Eine Beschaeftigung gezogen** (Stufe 1 oder 2): Er faengt sie an. Auch dann, wenn er sie
     *   schon tut - ein zweiter Zug auf dieselbe Sache ist ein Anstupsen, kein Nichts.
     * - **Eine Einlage auf die passende Beschaeftigung**: nur die Einlage. Die Beschaeftigung
     *   laeuft weiter, sie wird nicht neu begonnen - sonst faenge er nach jedem Trick wieder von
     *   vorn an.
     * - **Eine Einlage auf etwas anderes**: erst der Wechsel zur zugehoerigen Beschaeftigung, dann
     *   die Einlage. Wer schlaeft und ein Dribbling bekommt, steht auf, geht spielen und dribbelt
     *   dann - er dribbelt nicht im Bett.
     *
     * **Passend heisst: genau der Elternknoten.** Wer allgemein "Sport" macht und ein Dribbling
     * bekommt, wechselt zu Ballsport - fuer einen Trick braucht es einen Ball, und "Sport" ist
     * noch keiner.
     */
    fun planFor(current: AvatarActivity?, dropped: AnimationNode): ActivityPlan {
        if (dropped.kind == AnimationNode.Kind.ACTIVITY) {
            return ActivityPlan(listOf(ActivityStep.Begin(dropped.id)), dropped.id)
        }

        // Eine Einlage ohne Elternknoten kann es im Baum nicht geben (Stufe 3 haengt immer unter
        // Stufe 2) - falls doch, spielt sie fuer sich, statt dass hier etwas abstuerzt.
        val parent = dropped.parentId
            ?: return ActivityPlan(listOf(ActivityStep.Flourish(dropped.id)), dropped.id)

        return if (current?.nodeId == parent) {
            ActivityPlan(listOf(ActivityStep.Flourish(dropped.id)), parent)
        } else {
            ActivityPlan(
                listOf(ActivityStep.Begin(parent), ActivityStep.Flourish(dropped.id)),
                parent
            )
        }
    }

    /**
     * Uebersetzt eine bereits vorhandene Skill-/Reminder-Absicht in eine konkrete vorhandene
     * [PlayRoutine]. Der erste vertikale Schnitt ist bewusst nur Fussball; fuer alle anderen
     * Knoten bleibt die bisherige Reaktion unangetastet, statt hier vorschnell ein Framework zu
     * bauen.
     *
     * Entscheidend: Freischaltungen waehlen NICHT bloss eine zusaetzliche Animation nach der
     * Handlung, sondern veraendern die Handlung selbst. Ohne Dribbling-Knoten gibt es keine
     * Dribbling-Sequenz; ohne Schuss-Knoten weder Zielen noch Schuss. Die eine kurze
     * `Football(DRIBBLE)`-Phase am Anfang ist dabei der bereits vorhandene Renderer fuer eine
     * einfache Ballberuehrung. Erst Wiederholung und Ortswechsel bilden das gelernte Dribbling.
     */
    fun resolve(
        current: AvatarActivity?,
        dropped: AnimationNode,
        context: ActivityContext
    ): ResolvedActivity? {
        if (dropped.id !in FOOTBALL_INTENT_NODES) return null

        val plan = planFor(current, dropped)
        if (plan.resultingActivity != BALLSPORT_NODE) return null

        val unlocked = context.unlockedNodeIds
        val ballsportLearned = BALLSPORT_NODE in unlocked
        val dribblingLearned = DRIBBLING_NODE in unlocked
        val shotLearned = SHOT_NODE in unlocked

        val localPractice = context.place in LOCAL_FOOTBALL_PLACES
        val targetPlace = if (localPractice) context.place else PlayScene.Place.SPORT
        val anchor = when (targetPlace) {
            PlayScene.Place.SPORT -> 0.30f
            PlayScene.Place.PARK -> 0.42f
            PlayScene.Place.MEADOW -> 0.46f
            else -> 0.34f
        }

        val steps = buildList {
            // Zuhause, im Arbeitszimmer usw. keinen Ball samt Tor in die Kulisse zaubern. Der
            // vorhandene GoToPlace-Schritt zeigt den Weg durch die Tuer; es ist also kein
            // Teleport. Park und Wiese sind dagegen glaubwuerdige lokale Uebungsorte.
            if (!localPractice) {
                add(RoutineStep.GoToPlace(PlayScene.Place.SPORT))
            }
            add(RoutineStep.Stroll(anchor))

            // Basiskontakt: Der Reminder darf auch einen Anfaenger zu einer kurzen Ballberuehrung
            // anregen. Das ist noch NICHT das freigeschaltete Dribbling-Repertoire.
            add(RoutineStep.Football(PlayEffects.FootballPhase.DRIBBLE))
            add(RoutineStep.Linger(if (ballsportLearned) 4_000L else 2_500L))

            if (dribblingLearned) {
                // Gelerntes Dribbling wird als erkennbare Folge sichtbar. Hoehere Avatar-Stufen
                // verlaengern nur eine BEREITS gelernte Faehigkeit; sie schalten nichts heimlich
                // frei.
                add(RoutineStep.Stroll((anchor + 0.16f).coerceAtMost(0.72f)))
                add(RoutineStep.Football(PlayEffects.FootballPhase.DRIBBLE))
                add(RoutineStep.Linger(5_000L))
                if (context.avatarLevel >= 3) {
                    add(RoutineStep.Stroll((anchor - 0.10f).coerceAtLeast(0.18f)))
                    add(RoutineStep.Football(PlayEffects.FootballPhase.DRIBBLE))
                    add(RoutineStep.Linger(4_000L))
                }
            }

            if (shotLearned) {
                add(RoutineStep.Football(PlayEffects.FootballPhase.AIM))
                add(RoutineStep.Linger(3_000L))
                add(RoutineStep.Football(PlayEffects.FootballPhase.KICK))
                add(RoutineStep.Linger(if (context.avatarLevel >= 4) 7_000L else 5_000L))
            }
        }

        return ResolvedActivity(
            plan = plan,
            topic = AnimationType.MOVE,
            routine = PlayRoutine(steps)
        )
    }

    /**
     * Die Beschaeftigung, in der eine Einlage stattfindet - fuer Anzeige und Pruefung.
     *
     * `null` fuer alles, was selbst eine Beschaeftigung ist.
     */
    fun hostActivityOf(node: AnimationNode): AnimationNode? =
        if (node.kind == AnimationNode.Kind.FLOURISH) {
            node.parentId?.let { AnimationTree.node(it) }
        } else {
            null
        }

    private const val BALLSPORT_NODE = "sport/ballsport"
    private const val DRIBBLING_NODE = "sport/ballsport/dribbling"
    private const val SHOT_NODE = "sport/ballsport/schuss"

    private val FOOTBALL_INTENT_NODES = setOf(BALLSPORT_NODE, DRIBBLING_NODE, SHOT_NODE)
    private val LOCAL_FOOTBALL_PLACES = setOf(
        PlayScene.Place.SPORT,
        PlayScene.Place.PARK,
        PlayScene.Place.MEADOW
    )
}
