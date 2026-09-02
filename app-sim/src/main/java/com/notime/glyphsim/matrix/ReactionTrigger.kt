package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationTree
import com.notime.glyphcore.data.AnimationType

/**
 * **Was eine Reaktion ausgeloest hat.**
 *
 * Bis hierher wurden dafuer zwei nullbare Werte nebeneinander durchgereicht - ein
 * [AnimationType] und ein Bibliotheks-Label - mit der ungeschriebenen Regel, dass genau einer
 * gesetzt ist. Beim Umbau auf den Animations-Baum ist diese Regel zum Problem geworden: Ein Knoten
 * allein kann drei verschiedene Dinge nicht auseinanderhalten, die sich unterschiedlich verhalten
 * muessen.
 *
 * - Eine **eingebaute** Erinnerung spielt die Handlung zu ihrem Thema (Glas leeren, Buch zuklappen).
 * - Eine **Bibliotheks-Animation im Baum** kann eine eigene oder eine geerbte Choreografie haben.
 * - Eine **selbstgezeichnete** Animation haengt an keinem Knoten und bekommt die arteigene
 *   Freuden-Reaktion - `nodeId = null` heisst hier also NICHT "kein Anlass".
 * - **Gar kein Anlass** (Antippen, Easter Egg) ist wieder etwas anderes.
 *
 * Als nullbare Werte waeren der dritte und der vierte Fall nicht zu unterscheiden, und genau da
 * lag der Grund, die Signatur in Paket P2 noch nicht anzufassen (siehe SKILLBAUM.md). Mit einem
 * eigenen Typ ist die Regel nicht mehr ungeschrieben, sondern steht im Code.
 */
sealed interface ReactionTrigger {

    /** Eine Erinnerung mit einem der fest eingebauten Typen. */
    data class Topic(val type: AnimationType) : ReactionTrigger

    /** Eine Animation, die im Baum haengt - erkennbar an ihrem Pfad. */
    data class Node(val nodeId: String) : ReactionTrigger

    /**
     * Eine Bibliotheks-Animation ohne Knoten: selbstgezeichnet, oder noch nicht zugeordnet.
     *
     * Sie ist ein echter Anlass und bekommt die arteigene Freuden-Reaktion - nur eben keine, die
     * zum Motiv passt, weil niemand weiss, was darauf zu sehen ist.
     */
    data object Untracked : ReactionTrigger

    /** Ohne Anlass - Antippen des Avatars, Easter Egg, Vorschau. */
    data object None : ReactionTrigger

    companion object {
        /**
         * Baut den Ausloeser so, wie ihn die Datenbank hinterlegt: genau eines von
         * [animationType] und [libraryAnimationLabel] ist gesetzt (siehe
         * [com.notime.glyphsim.data.AvatarFeedEvent]).
         *
         * Der Vorrang liegt beim Knoten: Traegt eine Erinnerung beides - was es geben kann, wenn
         * sie aus einer aelteren Fassung stammt -, gewinnt die genauere Angabe.
         */
        fun of(animationType: AnimationType?, libraryAnimationLabel: String?): ReactionTrigger {
            if (libraryAnimationLabel != null) {
                val nodeId = AnimationTree.nodeIdFor(libraryAnimationLabel)
                return if (nodeId != null) Node(nodeId) else Untracked
            }
            return animationType?.let(::Topic) ?: None
        }

        /** Aus einem bereits bekannten Knoten - der Weg der Zieh-Leiste (Paket P5). */
        fun ofNode(nodeId: String?): ReactionTrigger =
            nodeId?.let(::Node) ?: None
    }
}
