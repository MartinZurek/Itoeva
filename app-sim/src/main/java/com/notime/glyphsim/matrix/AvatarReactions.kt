package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationTree
import com.notime.glyphsim.matrix.AvatarAnimations.Beat

/**
 * **Sucht die Choreografie zu einem Knoten des Animations-Baums - und zwar nach oben.**
 *
 * Bisher entschied der Name: `forLabel("Bubble")` traf oder traf nicht, und wer nicht traf, bekam
 * die allgemeine Freuden-Reaktion seiner Spezies. Das genuegt, solange alle Motive gleichrangig
 * nebeneinander liegen - mit einem Baum ist es zu grob. "Dribbling" hat noch keine eigene Antwort,
 * aber es ist Ballsport, und darauf koennte der Avatar antworten.
 *
 * **Das ist der Grund, warum der Baum bezahlbar ist.** Ohne den Rueckfall muesste jeder neue Knoten
 * mit einer eigenen Choreografie kommen - bei 79 Knoten ein Berg Arbeit, bevor ueberhaupt etwas
 * benutzbar ist. Mit ihm ist jeder Knoten ab dem Tag bespielbar, an dem er im Baum steht.
 *
 * ## Zwei Sorten Antwort, und nur eine wird vererbt
 *
 * Der erste Entwurf lief einfach die ganze Kette gegen [AvatarSignatureReactions] - und hat dabei
 * genau einen Fall veraendert, der zeigt, warum das falsch ist: **Idea** haengt unter
 * `lernen/lesen`, und dieser Knoten traegt das Motiv *Scroll*. Idea hat keine eigene Antwort, erbte
 * also Scrolls - "der Blick wandert zeilenweise, dann: verstanden". Vom Takt her passt das zur
 * Gluehbirne sogar gut. Nur haelt der Avatar dabei eine **Schriftrolle** in der Hand, denn die
 * Requisite gehoert zu Scroll und nicht zu der Stelle im Baum, an der Scroll zufaellig sitzt.
 * Eine Erinnerung "Idee" zeigt dann auf dem Glyph eine Gluehbirne, waehrend der Avatar daneben
 * liest.
 *
 * Daraus die Regel:
 *
 * - **Motiveigene Antworten** ([AvatarSignatureReactions], die 30 charakterspezifischen) gelten
 *   fuer **genau ihren Knoten**. Sie tragen die Requisite ihres Motivs und ergaeben eine Stufe
 *   tiefer ein falsches Bild.
 * - **Gruppen-Antworten** ([groupAnswer]) gelten fuer **alles darunter**. Sie werden fuer eine
 *   Stelle im Baum entworfen, nicht fuer ein Motiv, und muessen deshalb ohne motivgebundene
 *   Requisite auskommen.
 *
 * Heute gibt es nur die erste Sorte. Der Rueckfall laeuft also, findet aber (noch) nichts - und
 * genau das ist richtig: Bis eine Gruppen-Antwort wirklich fuer diese Rolle entworfen ist, bleibt
 * die arteigene Freuden-Reaktion die ehrlichere Antwort. Die 18 Untergruppen-Antworten kommen in
 * SKILLBAUM.md, Paket P9; sie werden unten eingehaengt, ohne dass ein Aufrufer sich aendert.
 */
internal object AvatarReactions {

    /**
     * `null` heisst: fuer diesen Knoten und alles ueber ihm gibt es nichts Eigenes - der Aufrufer
     * nimmt seinen bisherigen Weg (arteigene Freuden-Reaktion bzw. die Handlung zum Thema).
     *
     * Ein unbekannter Pfad ergibt ebenfalls `null`, weil [AnimationTree.fallbackChain] fuer ihn
     * eine leere Kette liefert - ein Tippfehler spielt also nichts Beliebiges ab.
     */
    fun forNode(nodeId: String?, body: AvatarBody): List<Beat>? {
        if (nodeId == null) return null

        // 1. Die motiveigene Antwort - nur fuer genau diesen Knoten, siehe Klassendoku.
        AvatarSignatureReactions.forNode(nodeId, body)?.let { return it }

        // 2. Nach oben: Antworten, die einer Stelle im Baum gehoeren statt einem Motiv.
        //    drop(1), weil der Knoten selbst in Schritt 1 schon dran war.
        for (ancestor in AnimationTree.fallbackChain(nodeId).drop(1)) {
            groupAnswer(ancestor, body)?.let { return it }
        }
        return null
    }

    /**
     * Die Antwort, die einer GRUPPE gehoert - vererbbar an alles darunter.
     *
     * Noch leer, und das ist kein Versehen: Eine Gruppen-Antwort muss ohne motivgebundene
     * Requisite auskommen (siehe Klassendoku), und keine der 30 vorhandenen Choreografien tut das.
     * Sie hier einzuhaengen, nur damit der Rueckfall "etwas tut", wuerde genau den Fehler
     * einbauen, den der Idea-Fall gezeigt hat.
     *
     * Vorgesehen sind die 9 Hauptgruppen und die 18 Untergruppen (SKILLBAUM.md, P9). Ein Eintrag
     * ist eine Zeile:
     *
     *     "sport/ballsport" -> ballsport(body)
     */
    private fun groupAnswer(nodeId: String, body: AvatarBody): List<Beat>? = when (nodeId) {
        else -> null
    }
}
