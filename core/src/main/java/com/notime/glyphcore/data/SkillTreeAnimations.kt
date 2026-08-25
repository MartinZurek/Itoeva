package com.notime.glyphcore.data

/**
 * **Die Motive, die es fuer den Animations-Baum noch nicht gab.**
 *
 * Der Baum ([AnimationTree]) hat 79 Knoten, und 67 davon liessen sich mit dem vorhandenen Bestand
 * besetzen. Die uebrigen zwoelf beschreiben Dinge, die vorher schlicht nicht vorkamen: ein
 * Dribbling, ein Besuch, ein Teller. Sie liegen bewusst hier und nicht in
 * [DefaultLibraryAnimations] oder [AvatarSignatureAnimations] - jene beiden Saetze haben ihre
 * eigene Geschichte (26 allgemeine, 30 charakterspezifische), und sie nachtraeglich anwachsen zu
 * lassen wuerde beide Zahlen zu einer Behauptung machen, die nicht mehr stimmt.
 *
 * ## Warum ASCII statt Koordinatenlisten
 *
 * Die vorhandenen Motive sind als Punktlisten geschrieben (`listOf(5 to 4, 6 to 4, ...)`). Das ist
 * kompakt, aber man sieht der Zeile nicht an, was sie zeichnet - Fehler faellt erst auf dem Geraet
 * auf, und auch dort nur, wenn man weiss, wonach man sucht. [sprite] nimmt stattdessen kleine
 * Bildchen entgegen:
 *
 *     sprite(4, 6, "#####")
 *
 * Das laesst sich im Quelltext lesen wie das, was es ist. Geprueft wird jedes Motiv trotzdem am
 * Bild: `AnimationPreviewTest` schreibt Kontaktboegen nach `core/build/preview/`.
 *
 * ## Der Anspruch
 *
 * Derselbe wie bei den vorhandenen Motiven (siehe [DefaultLibraryAnimations]): **jede Animation
 * braucht eine zweite erkennbare Bewegung.** Ein einzelnes pulsierendes Element ist zu wenig - beim
 * Teller isst die Gabel UND der Berg wird kleiner, beim Dribbling federt der Ball UND die Hand geht
 * mit.
 */
object SkillTreeAnimations {

    /** Anschluss an 26 allgemeine + 30 charakterspezifische. */
    private const val SORT_OFFSET = 56

    fun seed(): List<LibraryAnimation> {
        val entries = listOf(
            Triple("Plate", "🍽️", plateFrames()),
            Triple("Visit", "🚪", visitFrames()),
            Triple("Call", "📞", callFrames()),
            Triple("Dribble", "🤾", dribbleFrames()),
            Triple("Shot", "🥅", shotFrames()),
            Triple("Lift", "💪", liftFrames()),
            Triple("Breather", "⏸️", breatherFrames()),
            Triple("Notes", "📝", notesFrames()),
            Triple("Sing", "🎤", singFrames()),
            Triple("Map", "🗺️", mapFrames()),
            Triple("Confetti", "🎊", confettiFrames()),
            Triple("Candles", "🕯️", candlesFrames())
        )
        return entries.mapIndexed { index, (label, emoji, frames) ->
            LibraryAnimation(
                label = label,
                emoji = emoji,
                framesData = FrameCodec.encode(frames),
                sortOrder = SORT_OFFSET + index
            )
        }
    }

    // ================= Zeichenhilfe =================

    /**
     * Setzt ein kleines Bild mit seiner linken oberen Ecke auf ([x], [y]).
     *
     * `#` ist eine leuchtende Zelle, alles andere bleibt dunkel. Punkte ausserhalb des Rasters
     * fallen still weg - das ist gewollt, damit ein Motiv am Rand ein- und ausfahren kann, ohne
     * dass jede Position von Hand beschnitten werden muss.
     */
    private fun sprite(x: Int, y: Int, vararg rows: String): List<Pair<Int, Int>> =
        rows.flatMapIndexed { dy, row ->
            row.mapIndexedNotNull { dx, c ->
                if (c == '#') (x + dx) to (y + dy) else null
            }
        }.filter { (px, py) -> px in 0 until SIZE && py in 0 until SIZE }

    private val SIZE = ReminderFrameGrid.SIZE

    // ================= koerper/essen: Teller =================

    /** Ein Teller, der leer gegessen wird - die Gabel geht hinunter, der Berg wird kleiner. */
    private fun plateFrames(): List<List<Pair<Int, Int>>> {
        fun plate() =
            sprite(1, 9, "###########") +
                sprite(2, 10, "#########") +
                sprite(4, 11, "#####")

        fun food(level: Int) = when (level) {
            3 -> sprite(4, 6, "#####") + sprite(3, 7, "#######") + sprite(2, 8, "#########")
            2 -> sprite(5, 7, "###") + sprite(4, 8, "#####")
            1 -> sprite(5, 8, "###")
            else -> emptyList()
        }

        // Zinken zeigen nach UNTEN, der Stiel nach oben - sonst sieht die Gabel aus wie eine Harke.
        fun fork(top: Int) = sprite(4, top, "..#..", "..#..", "#####", "#.#.#")

        return listOf(
            fork(0) + food(3) + plate(),
            fork(2) + food(3) + plate(),
            fork(3) + food(3) + plate(),
            fork(0) + food(2) + plate(),
            fork(3) + food(2) + plate(),
            fork(0) + food(1) + plate(),
            fork(3) + food(1) + plate(),
            fork(0) + food(0) + plate()
        )
    }

    // ================= naehe/freunde: Besuch =================

    /**
     * Jemand kommt zur Tuer herein und winkt.
     *
     * **Der erste Entwurf hatte ein Tuerblatt**, das von rechts nach links schrumpfte. Am
     * Kontaktbogen war zu sehen, dass sich das als ineinandergeschachtelte Rechtecke liest und
     * nicht als Tuer - zwei Umrisse ineinander ergeben auf 13x13 Ringe, keine Tiefe. Ohne Blatt,
     * dafuer mit einer Figur, die hereingeht, ist sofort klar, worum es geht.
     */
    private fun visitFrames(): List<List<Pair<Int, Int>>> {
        fun doorway() =
            sprite(2, 1, "#########") +
                sprite(2, 2, "#", "#", "#", "#", "#", "#", "#", "#", "#") +
                sprite(10, 2, "#", "#", "#", "#", "#", "#", "#", "#", "#") +
                sprite(2, 11, "#########")

        fun visitor(x: Int, armUp: Boolean) =
            sprite(x, 3, "##", "##") +
                sprite(x, 5, "##", "##", "##") +
                sprite(x, 8, "#.#", "#.#", "#.#") +
                if (armUp) sprite(x + 2, 3, "#", "#") else sprite(x + 2, 6, "#")

        return listOf(
            doorway(),
            doorway() + visitor(8, armUp = false),
            doorway() + visitor(7, armUp = false),
            doorway() + visitor(5, armUp = false),
            doorway() + visitor(5, armUp = true),
            doorway() + visitor(5, armUp = false),
            doorway() + visitor(5, armUp = true),
            doorway() + visitor(5, armUp = false)
        )
    }

    // ================= naehe/freunde: Anrufen =================

    /**
     * Der Hoerer klingelt: er wackelt, und die Wellen laufen nach aussen.
     *
     * Schmaler und tiefer als im ersten Entwurf - dort sass er am oberen Rand und die Wellen
     * stiessen an ihn, sodass beides zu einem Klumpen verschwamm.
     */
    private fun callFrames(): List<List<Pair<Int, Int>>> {
        fun handset(dx: Int) =
            sprite(3 + dx, 5, "##...##") +
                sprite(3 + dx, 6, "##...##") +
                sprite(3 + dx, 7, "#######")

        fun rings(stage: Int) = when (stage) {
            1 -> sprite(1, 6, "#") + sprite(11, 6, "#")
            2 -> sprite(0, 5, "#", "#", "#") + sprite(12, 5, "#", "#", "#")
            3 -> sprite(0, 4, "#", "#", "#", "#", "#") + sprite(12, 4, "#", "#", "#", "#", "#")
            else -> emptyList()
        }

        return listOf(
            handset(0) + rings(0),
            handset(-1) + rings(1),
            handset(1) + rings(2),
            handset(-1) + rings(3),
            handset(1) + rings(2),
            handset(-1) + rings(1),
            handset(0) + rings(0)
        )
    }

    // ================= sport/ballsport: Dribbling =================

    /** Der Ball federt tief und schnell, die Hand geht mit - und wechselt die Seite. */
    private fun dribbleFrames(): List<List<Pair<Int, Int>>> {
        // Der Boden reicht NICHT ueber die volle Breite: Die Glyph-Matrix ist rund, und in den
        // untersten Zeilen liegen die Ecken ausserhalb des Ausschnitts (siehe
        // LibraryAnimationFitTest - der erste Entwurf hatte dort 38 % der Punkte im Nichts).
        fun floor() = sprite(2, 11, "#########")
        fun hand(x: Int, y: Int) = sprite(x, y, "####")
        fun ball(x: Int, y: Int) = sprite(x, y, "##", "##")
        /** Am Boden wird der Ball flach - ohne das federt er nicht, sondern schwebt nur. */
        fun squash(x: Int) = sprite(x, 10, "####")

        return listOf(
            hand(3, 2) + ball(4, 4) + floor(),
            hand(3, 3) + ball(4, 8) + floor(),
            hand(3, 3) + squash(3) + floor(),
            hand(3, 3) + ball(4, 8) + floor(),
            hand(3, 2) + ball(4, 4) + floor(),
            hand(7, 2) + ball(8, 4) + floor(),
            hand(7, 3) + ball(8, 8) + floor(),
            hand(7, 3) + squash(7) + floor()
        )
    }

    // ================= sport/ballsport: Schuss =================

    /**
     * Der Ball fliegt ins Tor, das Netz gibt nach.
     *
     * Das Tor nimmt die rechte Haelfte ein, nicht nur eine Ecke: Im ersten Entwurf war es so
     * klein, dass Ball und Pfosten gleich gross wirkten und der Ball beim Einschlag einfach
     * verschwand. Jetzt ist zu sehen, wohin er fliegt - und dass hinten etwas nachgibt.
     */
    private fun shotFrames(): List<List<Pair<Int, Int>>> {
        fun goal() =
            sprite(4, 2, "########") +
                sprite(4, 3, "#", "#", "#", "#", "#", "#", "#", "#") +
                sprite(11, 3, "#", "#", "#", "#", "#", "#", "#", "#") +
                sprite(2, 11, "#########")

        /** Das Netz gibt nach hinten nach - daran ist der Treffer zu erkennen. */
        fun net(bulge: Int) = when (bulge) {
            0 -> sprite(6, 5, "#") + sprite(9, 5, "#") + sprite(6, 8, "#") + sprite(9, 8, "#")
            1 -> sprite(7, 5, "#") + sprite(10, 5, "#") + sprite(7, 8, "#") + sprite(10, 8, "#")
            else -> sprite(12, 5, "#", "#", "#", "#") + sprite(10, 6, "#")
        }

        fun ball(x: Int, y: Int) = sprite(x, y, "##", "##")

        return listOf(
            goal() + net(0) + ball(0, 8),
            goal() + net(0) + ball(2, 6),
            goal() + net(0) + ball(5, 5),
            goal() + net(1) + ball(8, 5),
            goal() + net(2) + ball(8, 7),
            goal() + net(1) + ball(8, 9),
            goal() + net(0) + ball(8, 9)
        )
    }

    // ================= sport/kraft-ausdauer: Heben =================

    /** Die Hantel geht hoch - und die Stange biegt sich oben unter dem Gewicht. */
    private fun liftFrames(): List<List<Pair<Int, Int>>> {
        fun barbell(y: Int, bend: Boolean): List<Pair<Int, Int>> {
            val plates = sprite(1, y, "##", "##", "##") + sprite(10, y, "##", "##", "##")
            val bar = if (bend) {
                sprite(3, y + 1, "##") + sprite(5, y + 2, "###") + sprite(8, y + 1, "##")
            } else {
                sprite(3, y + 1, "#######")
            }
            return plates + bar
        }

        return listOf(
            barbell(8, bend = false),
            barbell(6, bend = false),
            barbell(4, bend = false),
            barbell(2, bend = false),
            barbell(2, bend = true),
            barbell(4, bend = false),
            barbell(6, bend = false),
            barbell(8, bend = false)
        )
    }

    // ================= arbeit/geraet: Pause machen =================

    /** Zwei Balken halten an, darueber steigt Dampf auf - die Arbeit ruht. */
    private fun breatherFrames(): List<List<Pair<Int, Int>>> {
        fun bars(height: Int): List<Pair<Int, Int>> {
            val top = 9 - height
            val column = List(height) { "##" }.toTypedArray()
            return sprite(4, top, *column) + sprite(7, top, *column)
        }

        fun steam(stage: Int) = when (stage) {
            0 -> sprite(5, 3, "#") + sprite(8, 3, "#")
            1 -> sprite(4, 2, "#") + sprite(7, 2, "#")
            2 -> sprite(5, 1, "#") + sprite(8, 1, "#")
            else -> sprite(4, 0, "#") + sprite(7, 0, "#")
        }

        return listOf(
            bars(5) + steam(0),
            bars(4) + steam(1),
            bars(5) + steam(2),
            bars(4) + steam(3),
            bars(5) + steam(0),
            bars(4) + steam(1)
        )
    }

    // ================= lernen/lesen: Notizen =================

    /** Zeile fuer Zeile entsteht Text, der Stift laeuft voraus. */
    private fun notesFrames(): List<List<Pair<Int, Int>>> {
        fun page() =
            sprite(2, 1, "#########") +
                sprite(2, 2, "#", "#", "#", "#", "#", "#", "#", "#", "#") +
                sprite(10, 2, "#", "#", "#", "#", "#", "#", "#", "#", "#") +
                sprite(2, 11, "#########")

        fun line(y: Int, length: Int) =
            if (length <= 0) emptyList() else sprite(4, y, "#".repeat(length))

        /** Die Spitze laeuft der Zeile voraus - ohne sie schreibt niemand, es erscheint nur Text. */
        fun pen(x: Int, y: Int) = sprite(x, y - 1, "#") + sprite(x, y - 2, "#")

        return listOf(
            page(),
            page() + line(4, 2) + pen(6, 4),
            page() + line(4, 5) + pen(9, 4),
            page() + line(4, 5) + line(6, 2) + pen(6, 6),
            page() + line(4, 5) + line(6, 5) + pen(9, 6),
            page() + line(4, 5) + line(6, 5) + line(8, 3) + pen(7, 8),
            page() + line(4, 5) + line(6, 5) + line(8, 5)
        )
    }

    // ================= kreativ/musik: Singen =================

    /** Das Mikrofon steht still, der Ton geht hinaus. */
    private fun singFrames(): List<List<Pair<Int, Int>>> {
        fun mic() =
            sprite(5, 2, "###", "###", "###") +
                sprite(6, 5, "#", "#", "#", "#") +
                sprite(4, 9, "#####")

        fun waves(stage: Int) = when (stage) {
            0 -> emptyList()
            1 -> sprite(3, 3, "#") + sprite(9, 3, "#")
            2 -> sprite(2, 2, "#", "#", "#") + sprite(10, 2, "#", "#", "#")
            else -> sprite(1, 1, "#", "#", "#", "#", "#") + sprite(11, 1, "#", "#", "#", "#", "#")
        }

        /** Der Fuss wippt im Takt - sonst singt das Mikrofon allein vor sich hin. */
        fun tap(dx: Int) = sprite(4 + dx, 11, "###")

        return listOf(
            mic() + waves(0) + tap(0),
            mic() + waves(1) + tap(1),
            mic() + waves(2) + tap(0),
            mic() + waves(3) + tap(1),
            mic() + waves(2) + tap(0),
            mic() + waves(1) + tap(1),
            mic() + waves(0) + tap(0)
        )
    }

    // ================= aufbruch/reisen: Karte =================

    /** Eine gefaltete Karte, auf der eine Route entlanglaeuft. */
    private fun mapFrames(): List<List<Pair<Int, Int>>> {
        fun sheet() =
            sprite(1, 3, "###########") +
                sprite(1, 4, "#", "#", "#", "#", "#", "#") +
                sprite(11, 4, "#", "#", "#", "#", "#", "#") +
                sprite(1, 9, "###########") +
                // Faltkanten - ohne sie ist es ein Rechteck, keine Karte.
                sprite(5, 4, "#") + sprite(5, 6, "#") + sprite(5, 8, "#") +
                sprite(8, 4, "#") + sprite(8, 6, "#") + sprite(8, 8, "#")

        val route = listOf(3 to 7, 4 to 6, 5 to 6, 6 to 5, 7 to 5, 8 to 6, 9 to 6)

        fun path(upTo: Int) = route.take(upTo)
        fun marker(index: Int) =
            route.getOrNull(index)?.let { (x, y) -> sprite(x, y - 1, "#") } ?: emptyList()

        return listOf(
            sheet() + path(1) + marker(0),
            sheet() + path(2) + marker(1),
            sheet() + path(3) + marker(2),
            sheet() + path(4) + marker(3),
            sheet() + path(5) + marker(4),
            sheet() + path(6) + marker(5),
            sheet() + path(7) + marker(6)
        )
    }

    // ================= aufbruch/feiern: Konfetti =================

    /** Schnipsel rieseln herunter und treiben dabei zur Seite. */
    private fun confettiFrames(): List<List<Pair<Int, Int>>> {
        // Jeder Schnipsel hat eine eigene Startzeile und eine eigene Drift - fielen alle gleich,
        // saehe es aus wie ein Vorhang statt wie Konfetti.
        val pieces = listOf(
            Triple(1, 0, 1), Triple(4, -3, -1), Triple(7, -1, 1),
            Triple(10, -5, -1), Triple(2, -7, 1), Triple(9, -9, -1)
        )

        return (0 until 7).map { step ->
            pieces.flatMap { (x, start, drift) ->
                val y = start + step * 2
                val shifted = x + drift * ((step / 2) % 3)
                sprite(shifted, y, "#")
            }
        }
    }

    // ================= aufbruch/feiern: Kerzen =================

    /**
     * Drei Kerzen flackern und werden ausgeblasen.
     *
     * Bewusst mehrere und ein Ende: Eine einzelne, ruhig brennende Kerze gibt es schon
     * ([AvatarSignatureAnimations], Starlets "Candle"). Hier geht es um den Moment davor und
     * danach, nicht um das Licht selbst.
     */
    private fun candlesFrames(): List<List<Pair<Int, Int>>> {
        val columns = listOf(3, 6, 9)

        fun sticks() = columns.flatMap { x -> sprite(x, 7, "#", "#", "#", "#", "#") }
        fun flames(phase: Int) = columns.mapIndexed { index, x ->
            if ((index + phase) % 2 == 0) sprite(x, 5, "#") else sprite(x, 4, "#") + sprite(x, 5, "#")
        }.flatten()

        fun smoke(stage: Int) = columns.flatMap { x ->
            when (stage) {
                1 -> sprite(x, 4, "#")
                2 -> sprite(x, 2, "#")
                else -> emptyList()
            }
        }

        return listOf(
            sticks() + flames(0),
            sticks() + flames(1),
            sticks() + flames(0),
            sticks() + flames(1),
            sticks() + smoke(1),
            sticks() + smoke(2),
            sticks()
        )
    }
}
