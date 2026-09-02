package com.notime.glyphsim.skilltree

import com.notime.glyphcore.data.AnimationNode
import com.notime.glyphcore.data.AnimationTree

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
}
