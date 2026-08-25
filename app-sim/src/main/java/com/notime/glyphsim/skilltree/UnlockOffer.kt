package com.notime.glyphsim.skilltree

import com.notime.glyphcore.data.AnimationNode
import com.notime.glyphcore.data.AnimationTree
import kotlin.random.Random

/**
 * Was beim Levelaufstieg zur Wahl steht: **zwei aus dem staerksten Zweig, dazu ein Querschlaeger.**
 *
 * Die Aufteilung steht bewusst im Typ und nicht nur in einer flachen Liste - die Oberflaeche soll
 * beides unterschiedlich zeigen koennen ("weil du viel Sport machst" vs. "und etwas ganz anderes").
 */
data class UnlockOffer(
    /** Bis zu zwei Knoten aus dem Zweig, den der Nutzer zuletzt am meisten bedient hat. */
    val focused: List<AnimationNode>,
    /** Einer aus einem ANDEREN Zweig, oder `null`, wenn es keinen anderen mehr gibt. */
    val wildcard: AnimationNode?
) {
    val all: List<AnimationNode> get() = focused + listOfNotNull(wildcard)
    val isEmpty: Boolean get() = all.isEmpty()
}

/**
 * Baut das Freischalt-Angebot.
 *
 * ## Warum nicht nur der staerkste Zweig
 *
 * Die naheliegende Regel - "biete an, was zum meistgenutzten Zweig gehoert" - verstaerkt sich
 * selbst: Wer zufaellig zweimal Sport bedient hat, bekommt Sport angeboten, schaltet Sport frei,
 * bedient dadurch noch mehr Sport. Nach zehn Leveln sitzt er in einem Ast fest und hat die
 * uebrigen acht Hauptgruppen nie zu Gesicht bekommen. Der [wildcard] ist der Ausweg: Der
 * Schwerpunkt entsteht weiterhin aus dem eigenen Verhalten, aber der Baum bleibt erreichbar.
 *
 * ## Was NICHT angeboten wird
 *
 * **Knoten ohne Zeichnung.** Zwoelf Knoten stehen im Baum, haben aber noch kein Motiv (siehe
 * [AnimationTree.pendingArtwork]). Wuerde man sie freischalten, laege in der Zieh-Leiste ein
 * Eintrag, der beim Ziehen nichts zeigt - eine leere Belohnung ist schlimmer als gar keine.
 *
 * Das hat eine Nebenwirkung, die bewusst in Kauf genommen wird: `koerper/essen` ist die einzige
 * Untergruppe ohne Motiv, und solange das so ist, sind auch ihre beiden Blaetter unerreichbar -
 * die Grenze waechst ja nur durch freigeschaltete Knoten. Sobald der Teller gezeichnet ist
 * (SKILLBAUM.md, P8), loest sich das von selbst. `UnlockOfferTest` haelt fest, welche Knoten
 * derzeit unerreichbar sind, damit die Zahl nicht unbemerkt waechst.
 *
 * **MEDICINE** kann hier gar nicht auftauchen, weil es nicht im Baum steht - die Regel aus dem
 * Spielplan des Avatars gilt damit automatisch weiter. Geprueft wird es trotzdem.
 */
object UnlockOffers {

    /** Wieviele Knoten aus dem staerksten Zweig kommen. */
    const val FOCUSED_COUNT = 2

    /**
     * [unlocked] sind die bereits offenen Pfade, [answers] die beantwortete Historie.
     *
     * [random] steuert zwei Dinge: die Auswahl unter gleichrangigen Kandidaten desselben Zweigs,
     * und - falls es ueberhaupt keine Historie gibt - welcher Zweig als "staerkster" gilt. Beides
     * ist absichtlich gewuerfelt und nicht fest: Ohne Historie GIBT es keinen staerksten Zweig,
     * und immer denselben zu nehmen waere eine Behauptung ueber den Nutzer, die niemand gedeckt
     * hat.
     */
    fun build(
        unlocked: Set<String>,
        answers: List<BranchAffinity.Answer>,
        nowMillis: Long,
        random: Random = Random
    ): UnlockOffer {
        val candidatesByRoot = frontier(unlocked).groupBy { rootOf(it.id) }
        if (candidatesByRoot.isEmpty()) return UnlockOffer(emptyList(), null)

        val scores = BranchAffinity.scores(answers, nowMillis)
        val ranking = if (scores.values.all { it == 0.0 }) {
            // Kein Signal - siehe [random] oben.
            candidatesByRoot.keys.shuffled(random)
        } else {
            BranchAffinity.ranked(answers, nowMillis).filter { it in candidatesByRoot }
        }
        if (ranking.isEmpty()) return UnlockOffer(emptyList(), null)

        val strongest = ranking.first()
        // Nur aus dem staerksten Zweig aufgefuellt, auch wenn dort weniger als zwei liegen:
        // Aus dem zweitstaerksten nachzuruecken wuerde die Aussage "das kommt aus deinem
        // Schwerpunkt" verwaessern, und der Querschlaeger sorgt ohnehin fuer die dritte Wahl.
        val focused = pick(candidatesByRoot.getValue(strongest), FOCUSED_COUNT, random)

        val otherRoots = ranking.drop(1)
        val wildcard = otherRoots
            .randomOrNull(random)
            ?.let { root -> pick(candidatesByRoot.getValue(root), 1, random).firstOrNull() }

        return UnlockOffer(focused, wildcard)
    }

    /**
     * Die **Grenze**: Kinder freigeschalteter Knoten, die selbst noch zu sind und ein Motiv haben.
     *
     * Oeffentlich, weil sich sonst nicht pruefen laesst, was ueberhaupt erreichbar ist - eine
     * Grenze, die irgendwann leer laeuft, waere ein Baum, der sich nicht mehr oeffnen laesst, ohne
     * dass irgendwo ein Fehler auftaucht.
     */
    fun frontier(unlocked: Set<String>): List<AnimationNode> =
        AnimationTree.nodes.filter { node ->
            node.motif != null &&
                node.id !in unlocked &&
                node.parentId != null &&
                node.parentId in unlocked
        }

    /** Die Hauptgruppen, mit denen jedes Profil beginnt. */
    fun startingNodes(): List<String> = AnimationTree.roots().map { it.id }

    private fun pick(from: List<AnimationNode>, count: Int, random: Random): List<AnimationNode> =
        if (from.size <= count) from.sortedBy { it.id } else from.shuffled(random).take(count)

    private fun rootOf(nodeId: String): String =
        AnimationTree.fallbackChain(nodeId).last()

    private fun <T> List<T>.randomOrNull(random: Random): T? =
        if (isEmpty()) null else this[random.nextInt(size)]
}
