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
 * Der Baum erbte frueher fuer viele Bibliotheksmotive die Antwort eines Geschwisters. Wer etwa
 * einen Skillpunkt auf "Basketball" setzte, bekam dadurch exakt das, was "Ballsport" schon tat.
 * Diese Datei gibt jedem nicht charaktergebundenen Bibliotheksmotiv eine eigene sichtbare Antwort.
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
            // ---- allgemeine Bibliothek: das Motiv bestimmt die sichtbare Antwort ----
            "Star" -> star(body)
            "Wave" -> wave(body)
            "Check" -> check(body)
            "Rain" -> rain(body)
            "Music" -> music(body)
            "Battery" -> battery(body)
            "Dog" -> dog(body)
            "Stocks" -> stocks(body)
            "TAMA" -> tama(body)
            "Breathe" -> breathe(body)
            "Football" -> football(body)
            "Fitness" -> fitness(body)
            "Robot" -> robot(body)
            "Fire" -> fire(body)
            "Plant" -> plant(body)
            "Target" -> target(body)
            "Airplane" -> airplane(body)
            "Cake" -> cake(body)
            "Idea" -> idea(body)
            "Mail" -> mail(body)
            "Clock" -> clock(body)
            // ---- sport/ballsport: vier Blaetter, vier verschiedene Bewegungen ----
            "Basketball" -> basketball(body)
            "Trophy" -> trophy(body)
            "Dribble" -> dribble(body)
            "Shot" -> shot(body)
            // ---- naehe/freunde und naehe/tiere: sieben Knoten, sieben Bewegungen ----
            "Gift" -> gift(body)
            "Visit" -> visit(body)
            "Call" -> call(body)
            "Cat" -> cat(body)
            "Pet" -> pet(body)
            // ---- zusaetzliche Motive des Skillbaums ----
            "Plate" -> plate(body)
            "Lift" -> lift(body)
            "Breather" -> breather(body)
            "Notes" -> notes(body)
            "Sing" -> sing(body)
            "Map" -> map(body)
            "Confetti" -> confetti(body)
            "Candles" -> candles(body)
            else -> null
        }
    }

    /** Alle Labels mit eigener Reaktion - der Test laeuft sie durch. */
    val labels: List<String> = listOf(
        "Star", "Wave", "Check", "Rain", "Music", "Battery", "Dog", "Stocks", "TAMA",
        "Breathe", "Football", "Fitness", "Robot", "Fire", "Plant", "Target", "Airplane",
        "Cake", "Idea", "Mail", "Clock",
        "Basketball", "Trophy", "Dribble", "Shot",
        "Gift", "Visit", "Call", "Cat", "Pet",
        "Plate", "Lift", "Breather", "Notes", "Sing", "Map", "Confetti", "Candles"
    )

    /**
     * Gemeinsame Grammatik fuer kleine motiveigene Antworten.
     *
     * Geteilt wird nur WIE ein Takt in einen Avatarframe uebersetzt wird. WAS darin passiert -
     * Requisite, Bahn, Blick, Haltung und Tempo - liefert jede Funktion darunter selbst. So
     * bleibt eine neue Antwort kurz genug zum Pflegen, ohne wieder identische Geschwister zu
     * erzeugen.
     */
    private fun AvatarAnimations.motif(
        body: AvatarBody,
        props: List<List<Pair<Int, Int>>>,
        offsets: List<Pair<Int, Int>> = emptyList(),
        accents: List<Int> = emptyList(),
        halfEyes: Set<Int> = emptySet(),
        closedEyes: Set<Int> = emptySet(),
        openMouth: Set<Int> = emptySet(),
        fast: Set<Int> = emptySet(),
        slow: Set<Int> = emptySet()
    ): List<Beat> = props.mapIndexed { index, prop ->
        val (dx, dy) = offsets.getOrElse(index) { 0 to 0 }
        val eyes = when (index) {
            in closedEyes -> body.eyesClosed
            in halfEyes -> body.eyesHalf
            else -> body.eyesOpen
        }
        val hold = when (index) {
            in slow -> SLOW_MS
            in fast -> FAST_MS
            else -> BEAT_MS
        }
        creatureFrame(
            body = body,
            dx = dx,
            dy = dy,
            accentPhase = accents.getOrElse(index) { 0 },
            eyeHoles = eyes,
            mouthHoles = if (index in openMouth) body.mouthOpen else body.mouthNeutral,
            prop = prop
        ).beat(hold)
    } + creatureFrame(body, accentPhase = 1).beat(SETTLE_MS)

    // =====================================================================================
    // Allgemeine Bibliothek und Skillbaum
    //
    // Die Requisite bleibt klein und oben oder am Rand. Sie nennt das Motiv; die Koerperbahn
    // zeigt, wie das Wesen darauf reagiert. Keine dieser Antworten wird an Kinder vererbt.
    // =====================================================================================

    private fun AvatarAnimations.star(body: AvatarBody) = motif(
        body,
        listOf(listOf(2 to 1), listOf(6 to 1, 7 to 0, 7 to 1, 7 to 2, 8 to 1),
            listOf(5 to 1, 7 to 0, 7 to 1, 7 to 2, 9 to 1), listOf(13 to 1)),
        offsets = listOf(-1 to 0, 0 to -1, 1 to -1, 1 to 0),
        accents = listOf(0, 1, -1, 1), openMouth = setOf(2), fast = setOf(0, 3)
    )

    private fun AvatarAnimations.wave(body: AvatarBody) = motif(
        body,
        listOf(listOf(0 to 2, 1 to 1, 2 to 2), listOf(4 to 1, 5 to 2, 6 to 1),
            listOf(8 to 2, 9 to 1, 10 to 2), listOf(12 to 1, 13 to 2, 14 to 1)),
        offsets = listOf(-1 to 0, 0 to 1, 1 to 0, 0 to -1),
        halfEyes = setOf(1, 2), slow = setOf(1, 2)
    )

    private fun AvatarAnimations.check(body: AvatarBody) = motif(
        body,
        listOf(listOf(10 to 2), listOf(10 to 2, 11 to 3),
            listOf(10 to 2, 11 to 3, 12 to 2, 13 to 1), listOf(11 to 3, 12 to 2, 13 to 1)),
        offsets = listOf(0 to 1, 0 to 0, 0 to -1, 0 to 0),
        accents = listOf(0, 1, 1, -1), openMouth = setOf(2), slow = setOf(2)
    )

    private fun AvatarAnimations.rain(body: AvatarBody) = motif(
        body,
        listOf(listOf(2 to 0, 8 to 1, 13 to 0), listOf(2 to 2, 8 to 3, 13 to 2),
            listOf(2 to 4, 8 to 1, 13 to 4), listOf(2 to 1, 8 to 4, 13 to 1)),
        offsets = listOf(0 to 0, 0 to 1, -1 to 1, 0 to 0),
        halfEyes = setOf(1), closedEyes = setOf(2), fast = setOf(0, 3)
    )

    private fun AvatarAnimations.music(body: AvatarBody) = motif(
        body,
        listOf(listOf(3 to 2, 3 to 3, 2 to 3), listOf(7 to 0, 7 to 1, 6 to 1),
            listOf(11 to 2, 11 to 3, 10 to 3), listOf(7 to 0, 3 to 2, 11 to 2)),
        offsets = listOf(-1 to 0, 0 to -1, 1 to 0, 0 to -1),
        accents = listOf(-1, 1, -1, 1), openMouth = setOf(1, 2, 3), fast = setOf(0, 1, 2)
    )

    private fun AvatarAnimations.battery(body: AvatarBody) = motif(
        body,
        listOf(listOf(13 to 1, 14 to 1, 13 to 4, 14 to 4),
            listOf(13 to 1, 14 to 1, 13 to 3, 14 to 3, 13 to 4, 14 to 4),
            listOf(13 to 1, 14 to 1, 13 to 2, 14 to 2, 13 to 3, 14 to 3, 13 to 4, 14 to 4),
            listOf(12 to 2, 13 to 1, 14 to 2, 13 to 3)),
        offsets = listOf(0 to 1, 0 to 0, 0 to -1, 0 to -2),
        accents = listOf(0, 0, 1, -1), openMouth = setOf(2, 3), slow = setOf(2)
    )

    private fun AvatarAnimations.dog(body: AvatarBody) = motif(
        body,
        listOf(listOf(14 to 4), listOf(12 to 3, 13 to 4, 14 to 3),
            listOf(10 to 3, 11 to 4, 12 to 3), listOf(8 to 4, 9 to 3)),
        offsets = listOf(0 to 0, 1 to 0, 1 to 1, 0 to 1),
        accents = listOf(0, 1, -1, 1), halfEyes = setOf(3), openMouth = setOf(2), fast = setOf(0, 1)
    )

    private fun AvatarAnimations.stocks(body: AvatarBody) = motif(
        body,
        listOf(listOf(1 to 4, 2 to 4), listOf(1 to 4, 2 to 3, 3 to 3),
            listOf(1 to 4, 2 to 3, 3 to 3, 4 to 1, 5 to 1), listOf(4 to 1, 5 to 0, 6 to 1)),
        offsets = listOf(0 to 1, -1 to 0, 0 to -1, 1 to -1),
        accents = listOf(0, 0, 1, 1), openMouth = setOf(2, 3), slow = setOf(2)
    )

    private fun AvatarAnimations.tama(body: AvatarBody) = motif(
        body,
        listOf(listOf(2 to 0, 3 to 0, 4 to 0, 3 to 1, 3 to 2),
            listOf(6 to 0, 6 to 1, 7 to 2, 8 to 1, 8 to 0),
            listOf(10 to 0, 10 to 1, 10 to 2, 11 to 1, 12 to 0, 12 to 1, 12 to 2),
            listOf(2 to 0, 7 to 0, 11 to 0)),
        offsets = listOf(-1 to 0, 0 to 0, 1 to 0, 0 to -1),
        accents = listOf(0, 1, -1, 1), openMouth = setOf(3), fast = setOf(0, 1, 2)
    )

    private fun AvatarAnimations.breathe(body: AvatarBody) = motif(
        body,
        listOf(listOf(7 to 1), listOf(5 to 1, 7 to 0, 9 to 1),
            listOf(3 to 1, 5 to 0, 7 to 0, 9 to 0, 11 to 1), listOf(5 to 1, 7 to 0, 9 to 1)),
        offsets = listOf(0 to 1, 0 to 0, 0 to -2, 0 to 0),
        closedEyes = setOf(0, 1, 2), halfEyes = setOf(3), slow = setOf(0, 1, 2, 3)
    )

    private fun AvatarAnimations.football(body: AvatarBody) = motif(
        body,
        listOf(listOf(2 to 4), listOf(4 to 3), listOf(8 to 1), listOf(13 to 3, 14 to 2, 14 to 3, 14 to 4)),
        offsets = listOf(-1 to 1, 0 to 0, 1 to -2, 1 to 0),
        accents = listOf(0, 1, -1, 1), openMouth = setOf(2, 3), fast = setOf(1, 2)
    )

    private fun AvatarAnimations.fitness(body: AvatarBody) = motif(
        body,
        listOf(listOf(2 to 3, 13 to 3), listOf(1 to 1, 14 to 1),
            listOf(2 to 4, 13 to 4), listOf(1 to 0, 14 to 0)),
        offsets = listOf(0 to 1, 0 to -2, 0 to 1, 0 to -2),
        accents = listOf(-1, 1, -1, 1), openMouth = setOf(1, 3), fast = setOf(0, 1, 2)
    )

    private fun AvatarAnimations.robot(body: AvatarBody) = motif(
        body,
        listOf(listOf(7 to 0, 6 to 1, 7 to 1, 8 to 1),
            listOf(6 to 0, 7 to 0, 8 to 0, 6 to 2, 8 to 2),
            listOf(5 to 1, 6 to 0, 8 to 0, 9 to 1, 6 to 2, 8 to 2), listOf(7 to 0)),
        offsets = listOf(-1 to 0, 1 to 0, -1 to 0, 0 to -1),
        accents = listOf(-1, 1, -1, 1), halfEyes = setOf(1, 2), fast = setOf(0, 1, 2)
    )

    private fun AvatarAnimations.fire(body: AvatarBody) = motif(
        body,
        listOf(listOf(13 to 4), listOf(12 to 3, 13 to 2, 14 to 3, 13 to 4),
            listOf(12 to 2, 13 to 0, 14 to 2, 13 to 3), listOf(13 to 1, 14 to 3, 13 to 4)),
        offsets = listOf(-1 to 0, -1 to 0, 0 to -1, 1 to 0),
        accents = listOf(0, -1, 1, -1), openMouth = setOf(2), fast = setOf(0, 1, 3)
    )

    private fun AvatarAnimations.plant(body: AvatarBody) = motif(
        body,
        listOf(listOf(2 to 4), listOf(2 to 3, 2 to 4),
            listOf(1 to 2, 2 to 3, 3 to 2, 2 to 4), listOf(0 to 1, 1 to 2, 2 to 3, 3 to 2, 4 to 1)),
        offsets = listOf(0 to 1, 0 to 0, -1 to 0, -1 to -1),
        accents = listOf(0, 0, 1, 1), halfEyes = setOf(0, 1), openMouth = setOf(3), slow = setOf(1, 2)
    )

    private fun AvatarAnimations.target(body: AvatarBody) = motif(
        body,
        listOf(listOf(13 to 2), listOf(12 to 1, 13 to 1, 14 to 1, 12 to 2, 14 to 2, 12 to 3, 13 to 3, 14 to 3),
            listOf(13 to 0, 11 to 2, 13 to 2, 15 to 2, 13 to 4), listOf(13 to 2)),
        offsets = listOf(-1 to 1, 0 to 0, 1 to -1, 0 to 0),
        accents = listOf(0, -1, 1, 1), halfEyes = setOf(1, 2), openMouth = setOf(3), slow = setOf(2)
    )

    private fun AvatarAnimations.airplane(body: AvatarBody) = motif(
        body,
        listOf(listOf(0 to 3, 1 to 2, 1 to 3), listOf(4 to 2, 5 to 1, 5 to 2, 6 to 2),
            listOf(9 to 1, 10 to 0, 10 to 1, 11 to 1), listOf(13 to 0, 14 to 0, 15 to 0)),
        offsets = listOf(-1 to 1, 0 to 0, 1 to -1, 1 to -2),
        accents = listOf(0, 1, -1, 1), openMouth = setOf(2, 3), fast = setOf(0, 1, 3)
    )

    private fun AvatarAnimations.cake(body: AvatarBody) = motif(
        body,
        listOf(listOf(11 to 4, 12 to 4, 13 to 4),
            listOf(10 to 3, 11 to 3, 12 to 3, 13 to 3, 14 to 3, 11 to 4, 12 to 4, 13 to 4),
            listOf(12 to 1, 12 to 2, 10 to 3, 11 to 3, 12 to 3, 13 to 3, 14 to 3),
            listOf(11 to 0, 12 to 1, 13 to 0, 10 to 3, 11 to 3, 12 to 3, 13 to 3, 14 to 3)),
        offsets = listOf(0 to 1, 0 to 0, 0 to -1, 0 to -2),
        accents = listOf(0, 0, 1, -1), openMouth = setOf(2, 3), slow = setOf(2)
    )

    private fun AvatarAnimations.idea(body: AvatarBody) = motif(
        body,
        listOf(listOf(7 to 3), listOf(6 to 2, 7 to 1, 8 to 2, 7 to 3),
            listOf(5 to 2, 6 to 1, 7 to 0, 8 to 1, 9 to 2, 7 to 3),
            listOf(7 to 0, 5 to 1, 9 to 1, 6 to 3, 7 to 3, 8 to 3)),
        offsets = listOf(0 to 1, 0 to 0, 0 to -1, 0 to -2),
        accents = listOf(0, -1, 1, -1), halfEyes = setOf(0, 1), openMouth = setOf(2, 3), slow = setOf(1)
    )

    private fun AvatarAnimations.mail(body: AvatarBody) = motif(
        body,
        listOf(listOf(15 to 2), listOf(12 to 1, 13 to 1, 14 to 2, 13 to 3),
            listOf(8 to 1, 9 to 1, 10 to 2, 9 to 3), listOf(5 to 1, 6 to 2, 7 to 1, 6 to 3)),
        offsets = listOf(1 to 0, 1 to 0, 0 to 0, -1 to -1),
        accents = listOf(0, 1, -1, 1), openMouth = setOf(3), fast = setOf(0, 1)
    )

    private fun AvatarAnimations.clock(body: AvatarBody) = motif(
        body,
        listOf(listOf(12 to 2, 13 to 1, 14 to 2, 13 to 3),
            listOf(12 to 1, 13 to 0, 14 to 1, 12 to 2, 14 to 2, 13 to 3),
            listOf(12 to 1, 13 to 0, 14 to 1, 12 to 2, 13 to 2, 14 to 2, 13 to 3),
            listOf(12 to 1, 13 to 0, 14 to 1, 12 to 2, 14 to 2, 12 to 3, 13 to 3, 14 to 3)),
        offsets = listOf(0 to 1, 0 to 0, 0 to -1, 0 to 0),
        accents = listOf(0, -1, 1, -1), halfEyes = setOf(0, 1, 2), openMouth = setOf(3), fast = setOf(1, 2)
    )

    private fun AvatarAnimations.plate(body: AvatarBody) = motif(
        body,
        listOf(listOf(10 to 4, 11 to 3, 12 to 3, 13 to 3, 14 to 4),
            listOf(10 to 4, 11 to 3, 13 to 3, 14 to 4),
            listOf(10 to 4, 12 to 3, 14 to 4), listOf(10 to 4, 11 to 4, 12 to 4, 13 to 4, 14 to 4)),
        offsets = listOf(0 to 0, 1 to 1, 0 to 1, 0 to 0),
        halfEyes = setOf(0, 1, 2), openMouth = setOf(0, 1, 2), fast = setOf(0, 1, 2)
    )

    private fun AvatarAnimations.lift(body: AvatarBody) = motif(
        body,
        listOf(listOf(3 to 4, 4 to 4, 11 to 4, 12 to 4),
            listOf(2 to 3, 3 to 3, 4 to 3, 11 to 3, 12 to 3, 13 to 3),
            listOf(2 to 1, 3 to 1, 4 to 1, 11 to 1, 12 to 1, 13 to 1),
            listOf(1 to 0, 2 to 0, 3 to 0, 12 to 0, 13 to 0, 14 to 0)),
        offsets = listOf(0 to 1, 0 to 0, 0 to -2, 0 to -3),
        accents = listOf(0, -1, 1, -1), halfEyes = setOf(0, 1), openMouth = setOf(1, 2, 3), slow = setOf(2)
    )

    private fun AvatarAnimations.breather(body: AvatarBody) = motif(
        body,
        listOf(listOf(5 to 1, 5 to 2, 9 to 1, 9 to 2),
            listOf(4 to 0, 4 to 1, 4 to 2, 4 to 3, 10 to 0, 10 to 1, 10 to 2, 10 to 3),
            listOf(5 to 1, 5 to 2, 9 to 1, 9 to 2), listOf(6 to 2, 8 to 2)),
        offsets = listOf(0 to 0, 0 to 1, -1 to 1, 0 to 0),
        halfEyes = setOf(0, 3), closedEyes = setOf(1, 2), slow = setOf(0, 1, 2)
    )

    private fun AvatarAnimations.notes(body: AvatarBody) = motif(
        body,
        listOf(listOf(1 to 1, 2 to 1, 3 to 1), listOf(1 to 1, 2 to 1, 3 to 1, 5 to 2),
            listOf(1 to 1, 2 to 1, 3 to 1, 5 to 2, 6 to 2, 8 to 3),
            listOf(1 to 1, 2 to 1, 3 to 1, 5 to 2, 6 to 2, 8 to 3, 9 to 3, 11 to 1)),
        offsets = listOf(-1 to 1, -1 to 0, 0 to 0, 1 to -1),
        halfEyes = setOf(0, 1, 2), accents = listOf(0, 1, -1, 1), slow = setOf(2)
    )

    private fun AvatarAnimations.sing(body: AvatarBody) = motif(
        body,
        listOf(listOf(12 to 4), listOf(12 to 2, 12 to 3, 12 to 4),
            listOf(11 to 1, 12 to 0, 13 to 1, 12 to 2, 12 to 3, 12 to 4),
            listOf(10 to 0, 12 to 0, 14 to 0, 12 to 2, 12 to 3, 12 to 4)),
        offsets = listOf(0 to 0, 1 to 0, 1 to -1, 0 to -1),
        accents = listOf(-1, 1, -1, 1), openMouth = setOf(0, 1, 2, 3), slow = setOf(2)
    )

    private fun AvatarAnimations.map(body: AvatarBody) = motif(
        body,
        listOf(listOf(1 to 3, 2 to 2, 3 to 3),
            listOf(1 to 2, 2 to 1, 3 to 2, 4 to 1, 5 to 2),
            listOf(1 to 1, 2 to 0, 3 to 1, 4 to 0, 5 to 1, 6 to 0, 7 to 1),
            listOf(3 to 1, 4 to 0, 5 to 1, 6 to 0, 7 to 1, 8 to 0, 9 to 1)),
        offsets = listOf(-1 to 1, -1 to 0, 0 to -1, 1 to -1),
        accents = listOf(0, -1, 1, -1), halfEyes = setOf(0, 1, 2), openMouth = setOf(3), slow = setOf(1, 2)
    )

    private fun AvatarAnimations.confetti(body: AvatarBody) = motif(
        body,
        listOf(listOf(7 to 0), listOf(4 to 1, 7 to 0, 10 to 1),
            listOf(2 to 3, 5 to 2, 8 to 3, 11 to 2, 14 to 3),
            listOf(1 to 4, 4 to 3, 7 to 4, 10 to 3, 13 to 4)),
        offsets = listOf(0 to 1, 0 to 0, 0 to -2, 0 to -1),
        accents = listOf(-1, 1, -1, 1), openMouth = setOf(1, 2, 3), fast = setOf(0, 1, 3)
    )

    private fun AvatarAnimations.candles(body: AvatarBody) = motif(
        body,
        listOf(listOf(5 to 3, 5 to 4, 10 to 3, 10 to 4),
            listOf(5 to 2, 5 to 3, 5 to 4, 10 to 2, 10 to 3, 10 to 4),
            listOf(4 to 1, 5 to 0, 6 to 1, 5 to 3, 5 to 4, 9 to 1, 10 to 0, 11 to 1, 10 to 3, 10 to 4),
            listOf(5 to 3, 5 to 4, 10 to 3, 10 to 4)),
        offsets = listOf(0 to 0, 0 to 0, 0 to -1, 0 to 1),
        halfEyes = setOf(0, 1), closedEyes = setOf(3), openMouth = setOf(2), slow = setOf(2)
    )

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

    // =====================================================================================
    // naehe/freunde und naehe/tiere
    //
    // Dieselbe Regel wie beim Ballsport, hier auf eine ganze Hauptgruppe angewandt: Die
    // Bahnen muessen sich unterscheiden, und zwar auch von denen der anderen Gruppen. Fuer
    // die fuenf hier heisst das - Geschenk senkrecht in der Mitte, Besuch waagerecht nach
    // LINKS (und als einziges bewegt sich die FIGUR statt der Requisite), Anruf diagonal in
    // der rechten oberen Ecke, Katze waagerecht am Boden von rechts, Gefaehrte im Bogen um
    // ihn herum. Fuenf Richtungen, fuenf Lesarten.
    //
    // Der Unterschied zwischen "Freunde" und "Tiere" liegt zusaetzlich in der Blickrichtung:
    // Bei den Menschen kommt etwas auf Augenhoehe oder von oben, bei den Tieren von unten -
    // er geht in die Knie statt hochzusehen.
    // =====================================================================================

    /** Die Schachtel steht da, der Deckel geht hoch, etwas steigt heraus. */
    private fun AvatarAnimations.gift(body: AvatarBody): List<Beat> {
        val box = listOf(7 to 3, 8 to 3)
        return listOf(
            creatureFrame(
                body, dy = 1, eyeHoles = body.eyesHalf, prop = box + listOf(7 to 2, 8 to 2)
            ).beat(BEAT_MS),
            creatureFrame(
                body, mouthHoles = body.mouthOpen, prop = box + listOf(7 to 1, 8 to 1)
            ).beat(FAST_MS),
            creatureFrame(
                body, dy = -1, accentPhase = 1, mouthHoles = body.mouthOpen, prop = box + (8 to 0)
            ).beat(BEAT_MS),
            creatureFrame(
                body, dy = -2, mouthHoles = body.mouthOpen, prop = box + listOf(6 to 0, 9 to 0)
            ).beat(SLOW_MS),
            creatureFrame(body, dy = -1, accentPhase = -1, prop = box).beat(BEAT_MS),
            creatureFrame(body, accentPhase = 1).beat(SETTLE_MS)
        )
    }

    /**
     * Die Tuer am linken Rand geht auf - und als einzige der fuenf bewegt sich hier die FIGUR
     * auf die Requisite zu statt umgekehrt. Genau das ist ein Besuch: Man geht hin.
     */
    private fun AvatarAnimations.visit(body: AvatarBody): List<Beat> {
        val frame = listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3)
        return listOf(
            creatureFrame(body, prop = frame + (1 to 0)).beat(BEAT_MS),
            creatureFrame(body, accentPhase = 1, prop = frame + (2 to 0)).beat(FAST_MS),
            creatureFrame(
                body, mouthHoles = body.mouthOpen, prop = frame + listOf(3 to 0, 3 to 1)
            ).beat(BEAT_MS),
            creatureFrame(
                body, dx = -2, accentPhase = 1, mouthHoles = body.mouthOpen,
                prop = frame + listOf(3 to 0, 3 to 1)
            ).beat(BEAT_MS),
            creatureFrame(
                body, dx = -2, dy = -2, mouthHoles = body.mouthOpen, prop = frame
            ).beat(FAST_MS),
            creatureFrame(body, dx = -1, accentPhase = -1, prop = frame).beat(SETTLE_MS)
        )
    }

    /** Das Signal kommt schraeg aus der oberen rechten Ecke; er nimmt ab und redet. */
    private fun AvatarAnimations.call(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(15 to 2)).beat(FAST_MS),
        creatureFrame(body, accentPhase = 1, prop = listOf(14 to 1, 15 to 2)).beat(FAST_MS),
        creatureFrame(
            body, mouthHoles = body.mouthOpen, prop = listOf(13 to 0, 14 to 1, 15 to 2)
        ).beat(BEAT_MS),
        creatureFrame(
            body, accentPhase = -1, mouthHoles = body.mouthOpen, prop = listOf(13 to 0, 15 to 2)
        ).beat(SLOW_MS),
        creatureFrame(
            body, dy = -1, mouthHoles = body.mouthOpen, prop = listOf(14 to 1)
        ).beat(BEAT_MS),
        creatureFrame(body, accentPhase = 1).beat(SETTLE_MS)
    )

    /** Sie kommt am Boden von rechts heran - und er geht zu ihr hinunter, statt hochzusehen. */
    private fun AvatarAnimations.cat(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(13 to 4, 13 to 3)).beat(BEAT_MS),
        creatureFrame(body, prop = listOf(12 to 4, 12 to 3)).beat(FAST_MS),
        creatureFrame(body, accentPhase = 1, prop = listOf(10 to 4, 10 to 3)).beat(FAST_MS),
        creatureFrame(
            body, eyeHoles = body.eyesHalf, mouthHoles = body.mouthOpen,
            prop = listOf(9 to 4, 9 to 3)
        ).beat(BEAT_MS),
        // In die Knie zu ihr hinunter.
        creatureFrame(body, dy = 1, eyeHoles = body.eyesHalf, prop = listOf(9 to 4)).beat(SLOW_MS),
        creatureFrame(body, accentPhase = -1, prop = listOf(10 to 4)).beat(SETTLE_MS)
    )

    /** Der kleine Gefaehrte umrundet ihn - rechts hoch, oben herueber, links wieder herunter. */
    private fun AvatarAnimations.pet(body: AvatarBody): List<Beat> = listOf(
        creatureFrame(body, prop = listOf(12 to 3)).beat(FAST_MS),
        creatureFrame(body, accentPhase = 1, prop = listOf(12 to 1)).beat(FAST_MS),
        creatureFrame(body, mouthHoles = body.mouthOpen, prop = listOf(8 to 0)).beat(BEAT_MS),
        creatureFrame(body, accentPhase = -1, prop = listOf(4 to 1)).beat(FAST_MS),
        creatureFrame(
            body, dy = -1, mouthHoles = body.mouthOpen, prop = listOf(4 to 3)
        ).beat(BEAT_MS),
        creatureFrame(body, accentPhase = 1).beat(SETTLE_MS)
    )
}
