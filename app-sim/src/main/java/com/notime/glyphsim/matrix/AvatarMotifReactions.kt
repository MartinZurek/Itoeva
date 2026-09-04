package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationMotif
import com.notime.glyphcore.data.AnimationTree
import com.notime.glyphsim.matrix.AvatarAnimations.BEAT_MS
import com.notime.glyphsim.matrix.AvatarAnimations.Beat
import com.notime.glyphsim.matrix.AvatarAnimations.FAST_MS
import com.notime.glyphsim.matrix.AvatarAnimations.SETTLE_MS
import com.notime.glyphsim.matrix.AvatarAnimations.SLOW_MS
import com.notime.glyphsim.matrix.AvatarAnimations.beat

/**
 * **Eigene Antworten fuer Motive, die keiner Kreatur gehoeren.**
 *
 * ## Warum es das braucht
 *
 * Der Baum hat 80 Knoten mit Motiv, aber bis hierher nur **55 verschiedene Reaktionen** - 38 Knoten
 * spielten Bild fuer Bild dasselbe wie ein Geschwister. Am dichtesten unter `sport/ballsport`: Der
 * Kopf und seine vier Blaetter (Basketball, Pokal, Dribbling, Schuss) waren **alle fuenf
 * identisch**. Wer einen Skillpunkt auf "Basketball" setzte, bekam exakt das, was "Ballsport" schon
 * tat.
 *
 * Der Grund liegt in der Vererbung und ist kein Fehler: Ein Blatt ohne eigene Antwort erbt die
 * Gruppen-Antwort seiner Untergruppe, und die ist **absichtlich requisitenfrei**
 * ([AvatarReactions.groupAnswer] erklaert, warum - eine geerbte Requisite laege sonst auch dann da,
 * wenn ein anderes Motiv gezogen wurde). Requisitenfrei heisst aber auch: austauschbar. Solange nur
 * die Uhr Reaktionen ausloeste, fiel das kaum auf; seit eine Freischaltung im Alltag sichtbar wird
 * (SKILLBAUM.md P15), ist es die Belohnung selbst, die unsichtbar bleibt.
 *
 * ## Warum ein eigenes Objekt neben [AvatarSignatureReactions]
 *
 * Dort liegen die **30 Charakter-Motive** - fuenf je Kreatur, und Klassendoku, `labels` und
 * `PER_SPECIES` sind auf genau diese 30 gebaut. Ein Basketball gehoert keiner Kreatur; ihn dort
 * einzureihen hiesse, jene Zusage aufzuweichen, um sich eine Datei zu sparen. Die **Regel** ist
 * dagegen dieselbe und gilt hier unveraendert: Eine motiveigene Antwort gilt fuer **genau ihren
 * Knoten** und traegt die Requisite ihres Motivs - vererbt wird sie nie.
 *
 * Requisiten liegen wie drueben in den oberen Zeilen (y <= 4) bzw. an den Raendern: Dort ist bei
 * jeder Spezies auch nach einer Verschiebung noch Platz, ohne dass sich Motiv und Koerper
 * ueberlagern.
 */
internal object AvatarMotifReactions {

    /** Wie [AvatarSignatureReactions.forNode], nur fuer die Motive ohne Kreatur. */
    fun forNode(nodeId: String, body: AvatarBody): List<Beat>? {
        val motif = AnimationTree.motifFor(nodeId) as? AnimationMotif.Library ?: return null
        return forLabel(motif.label, body)
    }

    /** null = kein eigener Ablauf hinterlegt, der Aufrufer geht seinen bisherigen Weg. */
    fun forLabel(label: String, body: AvatarBody): List<Beat>? = with(AvatarAnimations) {
        when (label) {
            // ---- sport/ballsport: vier Blaetter, vier verschiedene Bewegungen ----
            "Basketball" -> basketball(body)
            "Trophy" -> trophy(body)
            "Dribble" -> dribble(body)
            "Shot" -> shot(body)
            else -> null
        }
    }

    /** Alle Labels mit eigener Reaktion - der Test laeuft sie durch. */
    val labels: List<String> = listOf("Basketball", "Trophy", "Dribble", "Shot")

    // =====================================================================================
    // sport/ballsport
    //
    // Die vier unterscheiden sich absichtlich in der BAHN der Requisite, nicht bloss im Takt:
    // Basketball ein Bogen nach oben rechts, Pokal eine Senkrechte, Dribbling ein flaches
    // Auf und Ab an derselben Stelle, Schuss eine Waagerechte quer aus dem Bild. Selbst als
    // Standbild-Streifen sind sie dadurch auseinanderzuhalten - und genau daran hat es
    // gefehlt.
    // =====================================================================================

    /** Der Korb steht rechts oben und bleibt stehen; der Ball beschreibt den Bogen dorthin. */
    private fun AvatarAnimations.basketball(body: AvatarBody): List<Beat> {
        val hoop = listOf(12 to 1, 13 to 1, 14 to 1)
        return listOf(
            creatureFrame(body, prop = hoop + (3 to 0)).beat(FAST_MS),
            creatureFrame(body, prop = hoop + (4 to 2)).beat(FAST_MS),
            // In die Knie, bevor er hochgeht.
            creatureFrame(body, dy = 1, eyeHoles = body.eyesHalf, prop = hoop + (5 to 4)).beat(BEAT_MS),
            creatureFrame(
                body, dy = -3, accentPhase = 1, mouthHoles = body.mouthOpen, prop = hoop + (8 to 2)
            ).beat(FAST_MS),
            creatureFrame(body, dy = -2, mouthHoles = body.mouthOpen, prop = hoop + (13 to 0)).beat(FAST_MS),
            // Durch den Ring: derselbe x-Wert, eine Zeile UNTER dem Ring.
            creatureFrame(body, accentPhase = -1, prop = hoop + (13 to 3)).beat(BEAT_MS),
            creatureFrame(body, accentPhase = 1).beat(SETTLE_MS)
        )
    }

    /** Er hebt ihn vom Boden bis ueber den Kopf - eine einzige, langsame Senkrechte. */
    private fun AvatarAnimations.trophy(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, dy = 1, eyeHoles = body.eyesHalf, prop = listOf(7 to 4, 8 to 4)).beat(BEAT_MS),
        creatureFrame(
            body, accentPhase = 1, prop = listOf(6 to 3, 7 to 3, 8 to 3, 9 to 3)
        ).beat(BEAT_MS),
        creatureFrame(
            body, dy = -1, mouthHoles = body.mouthOpen,
            prop = listOf(6 to 2, 7 to 2, 8 to 2, 9 to 2, 7 to 3, 8 to 3)
        ).beat(BEAT_MS),
        // Oben, und dort bleibt er einen Takt lang stehen - darum geht es bei einem Pokal.
        creatureFrame(
            body, dy = -3, accentPhase = 1, mouthHoles = body.mouthOpen,
            prop = listOf(6 to 1, 7 to 1, 8 to 1, 9 to 1, 7 to 2, 8 to 2)
        ).beat(SLOW_MS),
        creatureFrame(
            body, dy = -2, accentPhase = -1,
            prop = listOf(6 to 1, 7 to 1, 8 to 1, 9 to 1, 7 to 2, 8 to 2)
        ).beat(BEAT_MS),
        creatureFrame(body, accentPhase = 1).beat(SETTLE_MS)
    )

    /**
     * Flach, schnell, an derselben Stelle - und bewusst OHNE Jubel am Ende.
     *
     * Dribbling ist Kontrolle, kein Erfolg. Die anderen drei enden hoch und mit offenem Mund; wenn
     * dieses hier genauso endete, waeren vier Bewegungen wieder derselbe Satz.
     */
    private fun AvatarAnimations.dribble(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(2 to 2)).beat(FAST_MS),
        creatureFrame(body, dy = 1, eyeHoles = body.eyesHalf, prop = listOf(2 to 4)).beat(FAST_MS),
        creatureFrame(body, prop = listOf(2 to 2)).beat(FAST_MS),
        creatureFrame(body, dy = 1, eyeHoles = body.eyesHalf, prop = listOf(2 to 4)).beat(FAST_MS),
        // Einmal etwas hoeher - sonst ist es eine Schleife statt einer Geste.
        creatureFrame(body, dy = -1, accentPhase = 1, prop = listOf(2 to 1)).beat(BEAT_MS),
        creatureFrame(body, accentPhase = -1, prop = listOf(2 to 3)).beat(BEAT_MS),
        creatureFrame(body, eyeHoles = body.eyesHalf).beat(SETTLE_MS)
    )

    /** Ausholen, treffen - und der Ball geht waagerecht quer aus dem Bild ins Tor. */
    private fun AvatarAnimations.shot(body: AvatarBody): List<Beat> {
        val net = listOf(15 to 0, 15 to 1, 15 to 2, 15 to 3)
        return listOf(
            creatureFrame(
                body, dy = 1, eyeHoles = body.eyesHalf, prop = net + (4 to 4)
            ).beat(BEAT_MS),
            creatureFrame(
                body, accentPhase = 1, mouthHoles = body.mouthOpen, prop = net + (6 to 3)
            ).beat(FAST_MS),
            creatureFrame(body, dy = -1, prop = net + (9 to 2)).beat(FAST_MS),
            creatureFrame(body, dy = -1, accentPhase = -1, prop = net + (12 to 1)).beat(FAST_MS),
            // Drin: das Netz gibt an der Einschlagstelle nach.
            creatureFrame(
                body, dy = -2, mouthHoles = body.mouthOpen, prop = net + (14 to 1)
            ).beat(BEAT_MS),
            creatureFrame(body, accentPhase = 1).beat(SETTLE_MS)
        )
    }
}
