package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationTree
import com.notime.glyphsim.matrix.AvatarAnimations.BEAT_MS
import com.notime.glyphsim.matrix.AvatarAnimations.Beat
import com.notime.glyphsim.matrix.AvatarAnimations.FAST_MS
import com.notime.glyphsim.matrix.AvatarAnimations.SETTLE_MS
import com.notime.glyphsim.matrix.AvatarAnimations.SLOW_MS
import com.notime.glyphsim.matrix.AvatarAnimations.beat

/**
 * **Sucht die Choreografie zu einem Knoten des Animations-Baums - und zwar nach oben.**
 *
 * Bisher entschied der Name: `forLabel("Bubble")` traf oder traf nicht, und wer nicht traf, bekam
 * die allgemeine Freuden-Reaktion seiner Spezies. Das genuegt, solange alle Motive gleichrangig
 * nebeneinander liegen - mit einem Baum ist es zu grob. "Dribbling" hat keine eigene Antwort, aber
 * es ist Ballsport, und darauf kann der Avatar antworten.
 *
 * **Das ist der Grund, warum der Baum bezahlbar ist.** Ohne den Rueckfall muesste jeder der 79
 * Knoten mit einer eigenen Choreografie kommen. Mit ihm genuegen 18 Gruppen-Antworten, damit rund
 * vierzig Blaetter etwas Passendes bekommen.
 *
 * ## Zwei Sorten Antwort, und nur eine wird vererbt
 *
 * Der erste Entwurf lief einfach die ganze Kette gegen [AvatarSignatureReactions] - und hat dabei
 * genau einen Fall veraendert, der zeigt, warum das falsch ist: **Idea** haengt unter
 * `lernen/lesen`, und dieser Knoten traegt das Motiv *Scroll*. Idea erbte also Scrolls Antwort -
 * "der Blick wandert zeilenweise, dann: verstanden". Vom Takt her passt das zur Gluehbirne sogar
 * gut. Nur haelt der Avatar dabei eine **Schriftrolle** in der Hand, denn die Requisite gehoert zu
 * Scroll und nicht zu der Stelle im Baum, an der Scroll zufaellig sitzt.
 *
 * Daraus die Regel:
 *
 * - **Motiveigene Antworten** ([AvatarSignatureReactions], die 30 charakterspezifischen) gelten
 *   fuer **genau ihren Knoten**. Sie tragen die Requisite ihres Motivs.
 * - **Gruppen-Antworten** ([groupAnswer]) gelten fuer **alles darunter** und kommen deshalb ohne
 *   motivgebundene Requisite aus.
 */
internal object AvatarReactions {

    /**
     * `null` heisst: fuer diesen Knoten und alles ueber ihm gibt es nichts Eigenes - der Aufrufer
     * nimmt seinen bisherigen Weg (Handlung zum Thema bzw. arteigene Freuden-Reaktion).
     *
     * Ein unbekannter Pfad ergibt ebenfalls `null`, weil [AnimationTree.fallbackChain] fuer ihn
     * eine leere Kette liefert - ein Tippfehler spielt also nichts Beliebiges ab.
     */
    fun forNode(nodeId: String?, body: AvatarBody): List<Beat>? {
        if (nodeId == null) return null

        // 1. Die motiveigene Antwort - nur fuer genau diesen Knoten, siehe Klassendoku.
        AvatarSignatureReactions.forNode(nodeId, body)?.let { return it }

        // 2. Nach oben: Antworten, die einer Stelle im Baum gehoeren statt einem Motiv.
        //
        // MIT dem Knoten selbst. Der erste Entwurf liess ihn aus (`drop(1)`) in der Annahme, er sei
        // in Schritt 1 schon drangewesen - Schritt 1 fragt aber nur die MOTIV-Antworten ab. Eine
        // Untergruppe fand dadurch ihre eigene Gruppen-Antwort nicht: Wer "Ballsport" zog, bekam die
        // generische Freuden-Reaktion, waehrend jedes Blatt darunter die richtige bekam.
        for (path in AnimationTree.fallbackChain(nodeId)) {
            groupAnswer(path, body)?.let { return it }
        }
        return null
    }

    /**
     * **Die Antwort, die einer Stelle im Baum gehoert - vererbbar an alles darunter.**
     *
     * Achtzehn Stueck, eine je Untergruppe. Sie beantworten nicht "was ist auf dem Glyph zu sehen",
     * sondern "worum geht es hier" - und dadurch bekommt jedes Blatt eine passende Antwort, ohne
     * dass jedes eine eigene braucht.
     *
     * ## Warum ohne Requisiten
     *
     * Die Regel aus dem Idea-Fall (siehe Klassendoku): Eine geerbte Choreografie bringt ihre
     * Requisite mit. Haette "Ballsport" einen Ball in der Hand, laege der auch dann da, wenn gerade
     * ein Pokal gezogen wurde. Gearbeitet wird deshalb nur mit dem, was JEDE Spezies hat und was zu
     * JEDEM Blatt darunter passt: Verschiebung, Haltung ([AvatarBody.accent]), Blick, Mund, Timing.
     *
     * **Kein Schwanz und keine Fuesse** - die hat nicht jede Spezies (siehe
     * [AvatarSignatureReactions]). Das Motiv auf dem Glyph traegt die Genauigkeit, der Koerper die
     * Energie.
     *
     * ## Was hier NICHT steht
     *
     * Die elf Knoten mit eingebautem Typ (die neun Hauptgruppen, `ruhe/pause`, `arbeit/erledigen`).
     * Sie spielen die ausgespielte Handlung ihres Themas - Glas leeren, Buch zuklappen -, und die
     * liegt in [AvatarAnimations.reactionFor]. Hier stuende sonst eine zweite, schlechtere Fassung.
     */
    private fun groupAnswer(nodeId: String, body: AvatarBody): List<Beat>? {
        fun pose(
            dx: Int = 0,
            dy: Int = 0,
            eyes: Set<Pair<Int, Int>> = body.eyesOpen,
            mouth: Set<Pair<Int, Int>> = body.mouthNeutral,
            accent: Int = 0,
            ms: Long = BEAT_MS
        ): Beat = AvatarAnimations.creatureFrame(
            body, eyeHoles = eyes, mouthHoles = mouth, dx = dx, dy = dy, accentPhase = accent
        ).beat(ms)

        return when (nodeId) {
            // Seitliches Federn - der Koerper spielt Ball, ohne einen zu halten.
            "sport/ballsport" -> listOf(
                pose(dx = -1, dy = -1, ms = FAST_MS),
                pose(dx = -1, ms = FAST_MS),
                pose(dx = 1, dy = -1, ms = FAST_MS),
                pose(dx = 1, ms = FAST_MS),
                pose(dy = -2, mouth = body.mouthOpen),
                pose(accent = 1, ms = SETTLE_MS)
            )

            // In die Knie, anspannen, hochdruecken.
            "sport/kraft-ausdauer" -> listOf(
                pose(dy = 1, eyes = body.eyesHalf),
                pose(dy = 1, mouth = body.mouthOpen, accent = 1, ms = SLOW_MS),
                pose(dy = -1, mouth = body.mouthOpen, accent = 1, ms = FAST_MS),
                pose(dy = -2, mouth = body.mouthOpen),
                pose(ms = SETTLE_MS)
            )

            // Kopf zurueck, schlucken, aufatmen.
            "koerper/trinken" -> listOf(
                pose(dy = -1, eyes = body.eyesHalf),
                pose(dy = -1, eyes = body.eyesClosed, mouth = body.mouthOpen, ms = SLOW_MS),
                pose(eyes = body.eyesClosed, mouth = body.mouthOpen, ms = SLOW_MS),
                pose(),
                pose(accent = 1, ms = SETTLE_MS)
            )

            // Kauen - der einzige Takt, der ganz im Gesicht stattfindet.
            "koerper/essen" -> listOf(
                pose(mouth = body.mouthOpen, ms = FAST_MS),
                pose(ms = FAST_MS),
                pose(mouth = body.mouthOpen, ms = FAST_MS),
                pose(ms = FAST_MS),
                pose(dy = -1, accent = 1),
                pose(ms = SETTLE_MS)
            )

            // Absinken, bis nichts mehr geht.
            "ruhe/schlafen" -> listOf(
                pose(eyes = body.eyesHalf, ms = SLOW_MS),
                pose(dy = 1, eyes = body.eyesHalf, ms = SLOW_MS),
                pose(dy = 1, eyes = body.eyesClosed, ms = SLOW_MS),
                pose(dy = 1, eyes = body.eyesClosed, accent = -1, ms = SETTLE_MS)
            )

            // Einmal tief durch.
            "achtsamkeit/atmen" -> listOf(
                pose(eyes = body.eyesClosed, ms = SLOW_MS),
                pose(dy = -1, eyes = body.eyesClosed, ms = SLOW_MS),
                pose(dy = -2, eyes = body.eyesClosed, ms = SLOW_MS),
                pose(dy = -1, eyes = body.eyesClosed, ms = SLOW_MS),
                pose(eyes = body.eyesHalf, ms = SETTLE_MS)
            )

            // Der Blick folgt etwas von links nach rechts - Neugier, kein Fangen.
            "achtsamkeit/beobachten" -> listOf(
                pose(dx = -1),
                pose(dx = -1, eyes = body.eyesHalf, ms = FAST_MS),
                pose(),
                pose(dx = 1),
                pose(dx = 1, mouth = body.mouthOpen, ms = FAST_MS),
                pose(accent = 1, ms = SETTLE_MS)
            )

            // Vorgebeugt, kurze Anschlaege, dann aufsehen.
            "arbeit/geraet" -> listOf(
                pose(dy = 1, eyes = body.eyesHalf),
                pose(dy = 1, eyes = body.eyesHalf, accent = 1, ms = FAST_MS),
                pose(dy = 1, eyes = body.eyesHalf, ms = FAST_MS),
                pose(dy = 1, eyes = body.eyesHalf, accent = 1, ms = FAST_MS),
                pose(mouth = body.mouthOpen),
                pose(ms = SETTLE_MS)
            )

            // Abhaken: zweimal nicken, dann aufsehen.
            //
            // Diese Untergruppe traegt zwar einen eingebauten Typ (FOCUS) und spielt fuer sich
            // selbst dessen Handlung - ihre Blaetter `check` und `target` haben aber keine eigene
            // Antwort und brauchen etwas zum Erben. Dasselbe gilt fuer `ruhe/pause` weiter unten.
            "arbeit/erledigen" -> listOf(
                pose(dy = 1, ms = FAST_MS),
                pose(ms = FAST_MS),
                pose(dy = 1, ms = FAST_MS),
                pose(mouth = body.mouthOpen),
                pose(dy = -1, accent = 1),
                pose(ms = SETTLE_MS)
            )

            // Zurueckgelehnt, einmal ausatmen, wieder da.
            "ruhe/pause" -> listOf(
                pose(dy = 1, eyes = body.eyesHalf, ms = SLOW_MS),
                pose(dx = -1, dy = 1, eyes = body.eyesHalf, ms = SLOW_MS),
                pose(dx = -1, dy = 1, eyes = body.eyesClosed, ms = SLOW_MS),
                pose(eyes = body.eyesHalf),
                pose(ms = SETTLE_MS)
            )

            // Der Blick wandert zeilenweise, dann faellt der Groschen.
            "lernen/lesen" -> listOf(
                pose(dx = -1, eyes = body.eyesHalf, ms = SLOW_MS),
                pose(eyes = body.eyesHalf, ms = SLOW_MS),
                pose(dx = 1, eyes = body.eyesHalf, ms = SLOW_MS),
                pose(dy = -1, mouth = body.mouthOpen, accent = 1),
                pose(ms = SETTLE_MS)
            )

            // Kopf schief, andere Seite, klick.
            "lernen/knobeln" -> listOf(
                pose(accent = 1, ms = SLOW_MS),
                pose(accent = 1, eyes = body.eyesHalf),
                pose(accent = -1, ms = SLOW_MS),
                pose(accent = -1, eyes = body.eyesHalf),
                pose(dy = -1, mouth = body.mouthOpen, ms = FAST_MS),
                pose(ms = SETTLE_MS)
            )

            // Im Takt wippen - schneller als alles andere hier.
            "kreativ/musik" -> listOf(
                pose(dy = -1, ms = FAST_MS),
                pose(ms = FAST_MS),
                pose(dy = -1, mouth = body.mouthOpen, ms = FAST_MS),
                pose(ms = FAST_MS),
                pose(dy = -1, mouth = body.mouthOpen, ms = FAST_MS),
                pose(accent = 1, ms = SETTLE_MS)
            )

            // Kleine genaue Bewegungen, dann einen Schritt zurueck und schauen.
            "kreativ/bauen-malen" -> listOf(
                pose(dx = -1, eyes = body.eyesHalf, ms = FAST_MS),
                pose(eyes = body.eyesHalf, ms = FAST_MS),
                pose(dx = 1, eyes = body.eyesHalf, ms = FAST_MS),
                pose(dy = 1, ms = SLOW_MS),
                pose(dy = 1, mouth = body.mouthOpen, accent = 1),
                pose(ms = SETTLE_MS)
            )

            // Hinwenden und gruessen.
            "naehe/freunde" -> listOf(
                pose(dx = 1),
                pose(dx = 1, mouth = body.mouthOpen),
                pose(dx = 1, dy = -1, mouth = body.mouthOpen, accent = 1, ms = FAST_MS),
                pose(mouth = body.mouthOpen),
                pose(accent = 1, ms = SETTLE_MS)
            )

            // Neugierig ducken und hin und her schauen.
            "naehe/tiere" -> listOf(
                pose(dy = 1),
                pose(dy = 1, dx = -1, ms = FAST_MS),
                pose(dy = 1, dx = 1, ms = FAST_MS),
                pose(dy = -1, mouth = body.mouthOpen),
                pose(accent = 1, ms = SETTLE_MS)
            )

            // Ducken und abspringen.
            "aufbruch/reisen" -> listOf(
                pose(dy = 1, eyes = body.eyesHalf),
                pose(dy = 1, accent = 1, ms = FAST_MS),
                pose(dy = -2, mouth = body.mouthOpen, ms = FAST_MS),
                pose(dy = -2, mouth = body.mouthOpen),
                pose(accent = 1, ms = SETTLE_MS)
            )

            // Huepfen.
            "aufbruch/feiern" -> listOf(
                pose(dy = -2, mouth = body.mouthOpen, ms = FAST_MS),
                pose(ms = FAST_MS),
                pose(dy = -2, mouth = body.mouthOpen, accent = 1, ms = FAST_MS),
                pose(ms = FAST_MS),
                pose(dy = -1, mouth = body.mouthOpen),
                pose(accent = 1, ms = SETTLE_MS)
            )

            else -> null
        }
    }
}
