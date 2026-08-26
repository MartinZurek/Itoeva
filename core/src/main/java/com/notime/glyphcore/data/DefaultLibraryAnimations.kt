package com.notime.glyphcore.data

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Startbefuellung der Animations-Bibliothek: 26 Beispiel-Animationen als reine
 * Punktdaten (kein Kotlin-Rendering-Code noetig) - beweist das Datenmodell und gibt
 * sofort etwas zum Ausprobieren, bevor weitere 30-40 dazukommen. Wird einmalig von
 * [LibraryAnimationRepository.seedIfEmpty] eingespielt. 1:1 uebernommen aus dem
 * :app-Modul.
 *
 * Auf denselben Gestaltungs-Anspruch wie die 12 fest eingebauten [AnimationType]s
 * gebracht (siehe matrix/ReminderAnimations.kt): mehr Frames, und jede Animation mit
 * einer zweiten erkennbaren Bewegung statt nur einem einzelnen pulsierenden Element
 * (Kritikpunkt an fruehen Entwuerfen der eingebauten Typen laut README) - der Stern
 * hat einen umlaufenden Twinkle-Punkt statt nur Groessen-Pulsieren, die Batterie
 * einen Lade-/Entlade-Zyklus mit Blitz statt einmaligem Vollladen usw. Alle Frames
 * mit demselben Mini-Skript (render.py, nicht Teil des Repos) als vergroesserte
 * Bitmaps geprueft, das fuer die eingebauten Typen erwaehnt wird.
 */
object DefaultLibraryAnimations {

    /**
     * Die 26 allgemeinen Animationen plus die 30 charakterspezifischen der sechs Avatare
     * ([AvatarSignatureAnimations]). Beide Saetze liegen in derselben Bibliothek und sind gleich
     * verwendbar - unterschiedlich ist nur, wofuer sie entworfen wurden.
     *
     * Die Zuordnung zum Animations-Baum ([LibraryAnimation.nodeId]) passiert hier an EINER Stelle
     * fuer beide Saetze, statt sie an 56 einzelne Eintraege zu schreiben: Der Baum kennt die
     * Zuordnung ohnehin ([AnimationTree.nodeIdFor]), und eine zweite, von Hand gepflegte Kopie
     * daneben koennte nur auseinanderlaufen. Ein Eintrag ohne passenden Knoten bekaeme `null` -
     * dass es den nicht gibt, bewacht `AnimationTreeTest`.
     */
    fun seed(): List<LibraryAnimation> =
        (general() + AvatarSignatureAnimations.seed() + SkillTreeAnimations.seed())
            .map { it.copy(nodeId = AnimationTree.nodeIdFor(it.label)) }

    private fun general(): List<LibraryAnimation> = listOf(
        LibraryAnimation(label = "Star", emoji = "⭐", framesData = FrameCodec.encode(starFrames()), sortOrder = 0),
        LibraryAnimation(label = "Wave", emoji = "🌊", framesData = FrameCodec.encode(waveFrames()), sortOrder = 1),
        LibraryAnimation(label = "Check", emoji = "✅", framesData = FrameCodec.encode(checkFrames()), sortOrder = 2),
        LibraryAnimation(label = "Rain", emoji = "☂️", framesData = FrameCodec.encode(umbrellaFrames()), sortOrder = 3),
        LibraryAnimation(label = "Music", emoji = "🎵", framesData = FrameCodec.encode(musicFrames()), sortOrder = 4),
        LibraryAnimation(label = "Battery", emoji = "🔋", framesData = FrameCodec.encode(batteryFrames()), sortOrder = 5),
        LibraryAnimation(label = "Rocket", emoji = "🚀", framesData = FrameCodec.encode(rocketFrames()), sortOrder = 6),
        LibraryAnimation(label = "Dog", emoji = "🐕", framesData = FrameCodec.encode(dogFrames()), sortOrder = 7),
        LibraryAnimation(label = "Cat", emoji = "🐈", framesData = FrameCodec.encode(catFrames()), sortOrder = 8),
        LibraryAnimation(label = "Gift", emoji = "🎁", framesData = FrameCodec.encode(giftFrames()), sortOrder = 9),
        LibraryAnimation(label = "Stocks", emoji = "📈", framesData = FrameCodec.encode(stockFrames()), sortOrder = 10),
        LibraryAnimation(label = "TAMA", emoji = "🔤", framesData = FrameCodec.encode(tamaFrames()), sortOrder = 11),
        LibraryAnimation(label = "Pet", emoji = "👾", framesData = FrameCodec.encode(tamagotchiFrames()), sortOrder = 12),
        LibraryAnimation(label = "Breathe", emoji = "🌀", framesData = FrameCodec.encode(breatheFrames()), sortOrder = 13),
        LibraryAnimation(label = "Football", emoji = "⚽", framesData = FrameCodec.encode(footballFrames()), sortOrder = 14),
        LibraryAnimation(label = "Basketball", emoji = "🏀", framesData = FrameCodec.encode(basketballFrames()), sortOrder = 15),
        LibraryAnimation(label = "Fitness", emoji = "🏋️", framesData = FrameCodec.encode(fitnessFrames()), sortOrder = 16),
        LibraryAnimation(label = "Robot", emoji = "🤖", framesData = FrameCodec.encode(robotFrames()), sortOrder = 17),
        LibraryAnimation(label = "Fire", emoji = "🔥", framesData = FrameCodec.encode(fireFrames()), sortOrder = 18),
        LibraryAnimation(label = "Trophy", emoji = "🏆", framesData = FrameCodec.encode(trophyFrames()), sortOrder = 19),
        LibraryAnimation(label = "Plant", emoji = "🌱", framesData = FrameCodec.encode(plantFrames()), sortOrder = 20),
        LibraryAnimation(label = "Target", emoji = "🎯", framesData = FrameCodec.encode(targetFrames()), sortOrder = 21),
        LibraryAnimation(label = "Airplane", emoji = "✈️", framesData = FrameCodec.encode(airplaneFrames()), sortOrder = 22),
        LibraryAnimation(label = "Cake", emoji = "🎂", framesData = FrameCodec.encode(cakeFrames()), sortOrder = 23),
        LibraryAnimation(label = "Idea", emoji = "💡", framesData = FrameCodec.encode(lightbulbFrames()), sortOrder = 24),
        LibraryAnimation(label = "Mail", emoji = "✉️", framesData = FrameCodec.encode(envelopeFrames()), sortOrder = 25)
    )

    /** Sparkle-Strahlen pulsieren klein->gross, dazu ein Twinkle-Punkt der reihum an den vier Ecken aufblitzt. */
    private fun starFrames(): List<List<Pair<Int, Int>>> {
        val cx = 6
        val cy = 6
        fun rays(n: Int, diag: Boolean): List<Pair<Int, Int>> {
            val pts = mutableListOf<Pair<Int, Int>>()
            for (k in 1..n) {
                pts += listOf(cx + k to cy, cx - k to cy, cx to cy + k, cx to cy - k)
            }
            if (diag) {
                for (k in 1 until n) {
                    pts += listOf(cx + k to cy + k, cx - k to cy - k, cx + k to cy - k, cx - k to cy + k)
                }
            }
            pts += (cx to cy)
            return pts
        }
        val twinkleSpots = listOf(10 to 2, 2 to 10, 10 to 10, 2 to 2)
        val pattern: List<Pair<Int, Int?>> = listOf(2 to null, 3 to 0, 4 to 1, 3 to null, 2 to 2, 1 to 3)
        return pattern.map { (n, spot) ->
            val pts = rays(n, diag = n >= 4).toMutableList()
            if (spot != null) pts += twinkleSpots[spot]
            pts
        }
    }

    /** Sinus-Schwell ueber 8 Phasenschritte, mit einem Schaum-Punkt an jedem Wellenkamm. */
    private fun waveFrames(): List<List<Pair<Int, Int>>> = (0 until 8).map { step ->
        val offset = step * 0.75
        val colY = (1..11).associateWith { x ->
            (6 + 2 * sin((x + offset) * 0.6)).roundToInt().coerceIn(1, 11)
        }
        val pts = colY.map { (x, y) -> x to y }.toMutableList()
        for (x in 2..10) {
            val y = colY.getValue(x)
            if (y < colY.getValue(x - 1) && y < colY.getValue(x + 1)) {
                pts += x to (y - 1)
            }
        }
        pts
    }

    private fun shift(pts: List<Pair<Int, Int>>, dx: Int = 0, dy: Int = 0): List<Pair<Int, Int>> =
        pts.map { (x, y) -> (x + dx) to (y + dy) }

    private fun circlePoints(cx: Int, cy: Int, radius: Int, steps: Int): List<Pair<Int, Int>> {
        val seen = LinkedHashSet<Pair<Int, Int>>()
        for (i in 0 until steps) {
            val angle = 2 * PI * i / steps
            val x = (cx + radius * cos(angle)).roundToInt()
            val y = (cy + radius * sin(angle)).roundToInt()
            seen += x to y
        }
        return seen.toList()
    }

    /** Haken zeichnet sich strichweise, danach schliesst sich ein Kreis-Badge drumherum (wie das Emoji). */
    private fun checkFrames(): List<List<Pair<Int, Int>>> {
        val tick = listOf(3 to 6, 4 to 7, 5 to 8, 6 to 7, 7 to 6, 8 to 5, 9 to 4)
        val circle = circlePoints(cx = 6, cy = 6, radius = 5, steps = 24)
        return listOf(
            tick.take(2),
            tick.take(4),
            tick,
            tick,
            tick + circle,
            tick + circle,
            tick + circle
        )
    }

    /** Regenschirm bleibt an Ort und Stelle, Tropfen fallen durchgehend auf versetzten Spalten daran vorbei. */
    private fun umbrellaFrames(): List<List<Pair<Int, Int>>> {
        val canopy = listOf(4 to 4, 5 to 3, 6 to 3, 7 to 3, 8 to 4, 3 to 5, 9 to 5, 6 to 4)
        val handle = listOf(6 to 5, 6 to 6, 6 to 7, 5 to 8, 5 to 9)
        val base = canopy + handle
        val dropCols = listOf(0, 2, 10, 12, 1, 11)
        val phases = listOf(0, 5, 2, 8, 10, 4)
        return (0 until 12).map { t ->
            val pts = base.toMutableList()
            dropCols.forEachIndexed { i, x ->
                val y = (t + phases[i]) % 13
                if (y <= 11) pts += x to y
            }
            pts
        }
    }

    /** Zwei Achtelnoten wippen gegenlaeufig auf und ab, der Verbindungsbalken blitzt im Takt auf. */
    private fun musicFrames(): List<List<Pair<Int, Int>>> {
        val stemL = listOf(4 to 3, 4 to 4, 4 to 5, 4 to 6, 4 to 7)
        val stemR = listOf(9 to 2, 9 to 3, 9 to 4, 9 to 5, 9 to 6)
        val headL = listOf(2 to 8, 3 to 8, 3 to 9, 2 to 9)
        val headR = listOf(7 to 7, 8 to 7, 8 to 8, 7 to 8)
        val beam = listOf(4 to 3, 5 to 3, 6 to 3, 7 to 2, 8 to 2, 9 to 2)

        fun shift(pts: List<Pair<Int, Int>>, dy: Int) = pts.map { (x, y) -> x to (y + dy) }

        val bob = listOf(0, -1, 0, 1, 0, -1, 0, 1)
        return bob.mapIndexed { i, dy ->
            val pts = (shift(stemL, dy) + shift(headL, dy) + shift(stemR, -dy) + shift(headR, -dy)).toMutableList()
            if (i % 2 == 0) pts += beam
            pts
        }
    }

    /** Fuellt sich auf, blitzt am Anschlag zweimal als Ladeblitz und entleert sich wieder - Lade-/Entlade-Loop. */
    private fun batteryFrames(): List<List<Pair<Int, Int>>> {
        val outline = listOf(
            6 to 3, 7 to 3,
            5 to 4, 8 to 4,
            5 to 5, 8 to 5,
            5 to 6, 8 to 6,
            5 to 7, 8 to 7,
            5 to 8, 8 to 8,
            5 to 9, 6 to 9, 7 to 9, 8 to 9
        )
        fun fillRow(y: Int) = listOf(6 to y, 7 to y)
        val empty = outline
        val level1 = outline + fillRow(8)
        val level2 = level1 + fillRow(7)
        val level3 = level2 + fillRow(6)
        val full = level3 + fillRow(5)
        val bolt = listOf(7 to 4, 6 to 5, 7 to 5, 6 to 6, 7 to 6, 6 to 7)
        val fullBolt = outline + bolt
        return listOf(empty, level1, level2, level3, full, fullBolt, fullBolt, full, level3, level2, level1, empty)
    }

    /**
     * Countdown 3-2-1 (Ziffern-Glyphen 1:1 aus [matrix.ReminderAnimations.workScreenDigits]
     * uebernommen, nur 4 Zeilen nach oben verschoben statt in einem Laptop-Bildschirm), dann
     * zuendet die Rakete, steigt Zeile fuer Zeile auf und verlaesst oben das Raster, waehrend
     * im Hintergrund durchgehend Sterne twinkeln. 9 Keyframes * 3 (Default-Ueberblendschritte)
     * = 27 Frames, damit ein Durchlauf (27*260ms = 7,02s) knapp unter der Ziel-Spieldauer von
     * 8s bleibt (siehe FrameCrossfade/MatrixAnimator) - kein zweiter Durchlauf noetig, kein
     * Abbruch mitten in der Animation.
     */
    private fun rocketFrames(): List<List<Pair<Int, Int>>> {
        fun digitThree() = shift(
            listOf(5 to 4, 6 to 4, 7 to 4, 7 to 5, 5 to 6, 6 to 6, 7 to 6, 7 to 7, 5 to 8, 6 to 8, 7 to 8),
            dy = -4
        )
        fun digitTwo() = shift(
            listOf(5 to 4, 6 to 4, 7 to 4, 7 to 5, 5 to 6, 6 to 6, 7 to 6, 5 to 7, 5 to 8, 6 to 8, 7 to 8),
            dy = -4
        )
        fun digitOne() = shift(listOf(6 to 4, 5 to 5, 6 to 5, 6 to 6, 6 to 7, 5 to 8, 6 to 8, 7 to 8), dy = -4)

        fun rocket(topY: Int) = shift(
            listOf(
                6 to 0,
                5 to 1, 7 to 1,
                5 to 2, 6 to 2, 7 to 2,
                5 to 3, 6 to 3, 7 to 3,
                4 to 4, 5 to 4, 6 to 4, 7 to 4, 8 to 4
            ),
            dy = topY
        )

        // Startet eine Zeile UNTER den Flossen und bleibt schmaler als deren Spannweite -
        // dadurch als eigener Feuerstrahl erkennbar statt mit dem Rumpf zu verschmelzen.
        fun flame(topY: Int, size: Int): List<Pair<Int, Int>> {
            val base = topY + 5
            return when (size) {
                0 -> emptyList()
                1 -> shift(listOf(6 to 0), dy = base)
                2 -> shift(listOf(5 to 0, 6 to 0, 7 to 0, 6 to 1), dy = base)
                else -> shift(listOf(5 to 0, 6 to 0, 7 to 0, 5 to 1, 6 to 1, 7 to 1, 6 to 2), dy = base)
            }
        }

        val starsA = listOf(1 to 1, 11 to 2, 2 to 10, 10 to 6)
        val starsB = listOf(11 to 1, 1 to 3, 10 to 9, 2 to 6)
        val starsC = listOf(1 to 1, 1 to 3, 11 to 2, 10 to 9, 2 to 10)

        return listOf(
            digitThree() + rocket(8) + starsA,
            digitTwo() + rocket(8) + starsB,
            digitOne() + rocket(8) + starsA,
            rocket(8) + flame(8, 2) + starsB,
            rocket(6) + flame(6, 3) + starsA,
            rocket(3) + flame(3, 3) + starsB,
            rocket(0) + flame(0, 2) + starsA,
            rocket(-3) + flame(-3, 1) + starsB,
            starsC
        )
    }

    /** Seitlich laufender Hund im Trab: Koerper hebt/senkt sich mit den diagonalen Beinpaaren, Rute wedelt. */
    private fun dogFrames(): List<List<Pair<Int, Int>>> {
        fun body(dy: Int) = shift(
            listOf(
                4 to 6, 5 to 6, 6 to 6, 7 to 6,
                3 to 7, 4 to 7, 5 to 7, 6 to 7, 7 to 7, 8 to 7
            ),
            dy = dy
        )

        fun head(dy: Int) = shift(listOf(9 to 3, 9 to 4, 10 to 4, 9 to 5, 10 to 5, 11 to 5, 12 to 6), dy = dy)

        fun tail(dy: Int, wag: Boolean): List<Pair<Int, Int>> {
            val tip = if (wag) 1 to 4 else 1 to 6
            val mid = if (wag) 2 to 5 else 2 to 6
            return shift(listOf(3 to 6, mid, tip), dy = dy)
        }

        // Trab-Gangart: diagonale Beinpaare wechseln sich ab (vorne-links+hinten-rechts,
        // dann vorne-rechts+hinten-links), synchron mit dem Koerper-Huepfer (dy).
        fun legs(dy: Int, pose: Int): List<Pair<Int, Int>> {
            val down: List<Pair<Int, Int>>
            val tucked: List<Pair<Int, Int>>
            if (pose == 0) {
                down = listOf(8 to 8, 8 to 9, 4 to 8, 4 to 9)
                tucked = listOf(7 to 8, 5 to 8)
            } else {
                down = listOf(7 to 8, 7 to 9, 5 to 8, 5 to 9)
                tucked = listOf(8 to 8, 4 to 8)
            }
            return shift(down + tucked, dy = dy)
        }

        val steps = listOf(Triple(0, 0, true), Triple(1, -1, false), Triple(0, 0, false), Triple(1, -1, true))
        return steps.map { (pose, dy, wag) -> body(dy) + head(dy) + tail(dy, wag) + legs(dy, pose) }
    }

    /** Sitzende Katze: kleiner, klar vom Koerper abgesetzter Kopf, Rute wedelt seitlich, Augen blinzeln (Luecke im Gesicht). */
    private fun catFrames(): List<List<Pair<Int, Int>>> {
        fun faceRow(eyesOpen: Boolean) =
            if (eyesOpen) listOf(5 to 3, 8 to 3) else listOf(5 to 3, 6 to 3, 7 to 3, 8 to 3)

        fun head(eyesOpen: Boolean) =
            listOf(5 to 1, 8 to 1, 6 to 2, 7 to 2) + faceRow(eyesOpen) + listOf(6 to 4, 7 to 4)

        val body = listOf(
            6 to 6, 7 to 6,
            5 to 7, 6 to 7, 7 to 7, 8 to 7,
            4 to 8, 5 to 8, 6 to 8, 7 to 8, 8 to 8, 9 to 8,
            5 to 9, 6 to 9, 7 to 9, 8 to 9
        )

        fun tail(tip: Pair<Int, Int>) = listOf(10 to 7, 10 to 6, tip)

        val tails = listOf(9 to 5, 10 to 5, 11 to 5, 10 to 5)
        val eyes = listOf(true, true, false, true)
        return tails.zip(eyes).map { (tip, eyesOpen) -> head(eyesOpen) + body + tail(tip) }
    }

    /** Geschenkbox wackelt vor Vorfreude, Deckel hebt sich kurz mit einem Funkenstoss, dann wieder zu. */
    private fun giftFrames(): List<List<Pair<Int, Int>>> {
        fun box(lidDy: Int): List<Pair<Int, Int>> {
            val filled = (6..10).flatMap { y -> (4..9).map { x -> x to y } }
            val bow = shift(
                listOf(4 to 3, 5 to 4, 6 to 3, 6 to 4, 7 to 3, 7 to 4, 8 to 4, 9 to 3),
                dy = lidDy
            )
            return filled + bow
        }

        fun sparkle(size: Int): List<Pair<Int, Int>> = when (size) {
            0 -> emptyList()
            1 -> listOf(6 to 1, 7 to 1)
            else -> listOf(6 to 0, 7 to 0, 4 to 1, 9 to 1, 6 to 2, 7 to 2)
        }

        return listOf(
            box(0),
            shift(box(0), dx = -1),
            box(0),
            shift(box(0), dx = 1),
            box(-1) + sparkle(1),
            box(-2) + sparkle(2),
            box(-1) + sparkle(1),
            box(0)
        )
    }

    /** Kurszickzack zeichnet sich progressiv von links nach rechts, endet mit einem Pfeil nach oben rechts. */
    private fun stockFrames(): List<List<Pair<Int, Int>>> {
        val path = listOf(
            1 to 10, 2 to 9, 3 to 10, 4 to 8, 5 to 9,
            6 to 6, 7 to 7, 8 to 5, 9 to 6, 10 to 3, 11 to 2
        )
        val arrow = listOf(11 to 2, 10 to 1, 11 to 0, 12 to 1)

        val frames = listOf(3, 5, 7, 9, 11).map { i -> path.take(i) }.toMutableList()
        frames += path + arrow
        frames += path + arrow
        return frames
    }

    /**
     * Buchstaben NACHEINANDER in der Mitte, nicht vier gleichzeitig in den Ecken - und mit einer
     * dunklen Pause dazwischen.
     *
     * Der urspruengliche Entwurf setzte T, A, M, A als 2x2-Block in die vier Ecken des
     * 13x13-Rasters - also genau dorthin, wo der runde Ausschnitt der Matrix nichts mehr anzeigt
     * (siehe MatrixGeometry.isActive im app-sim-Modul). Rund ein Viertel aller Punkte war damit
     * unsichtbar, das Wort war nicht zu lesen.
     *
     * **Warum die Leer-Keyframes.** [FrameCrossfade] blendet zwischen zwei Posen ueber, indem es
     * die alten Punkte aus- und die neuen gleichzeitig einblendet. Bei Formen (Herz, Glocke) wirkt
     * das weich. Bei Buchstaben lagen dadurch waehrend jedes Uebergangs ZWEI Buchstaben
     * uebereinander - T und A gleichzeitig, halbhell, ueber die ganze Breite verteilt. Das war
     * kein Buchstabe mehr, sondern Punktesalat, und genau der Eindruck "passt nicht in den Kreis".
     * Mit einer leeren Pose zwischen je zwei Buchstaben blendet jeder Uebergang nur noch gegen
     * Schwarz: ein Buchstabe verschwindet, dann kommt der naechste. Doppelte Buchstaben-Posen
     * halten ihn davor kurz ruhig stehen (identische Posen ergeben eine Ueberblendung ohne
     * Aenderung, also einen Standbild-Moment).
     */
    private fun tamaFrames(): List<List<Pair<Int, Int>>> {
        // 9 breit, 9 hoch, danach auf (2,2) gesetzt: belegt x 2..10 und y 2..10. Auch die Ecken
        // dieses Quadrats liegen im Kreis - (2,2) hat 5,66 Abstand zur Mitte (6,6), der Radius
        // betraegt 6,5. Vorher waren die Buchstaben 5x7 und wirkten im Kreis verloren.
        fun glyph(rows: List<String>): List<Pair<Int, Int>> =
            rows.flatMapIndexed { y, row ->
                row.mapIndexedNotNull { x, c -> if (c == '#') (x + 2) to (y + 2) else null }
            }

        val letterT = glyph(
            listOf(
                "#########",
                "....#....",
                "....#....",
                "....#....",
                "....#....",
                "....#....",
                "....#....",
                "....#....",
                "....#...."
            )
        )
        val letterA = glyph(
            listOf(
                "...###...",
                "..#...#..",
                ".#.....#.",
                "#.......#",
                "#.......#",
                "#########",
                "#.......#",
                "#.......#",
                "#.......#"
            )
        )
        val letterM = glyph(
            listOf(
                "#.......#",
                "##.....##",
                "#.#...#.#",
                "#..#.#..#",
                "#...#...#",
                "#.......#",
                "#.......#",
                "#.......#",
                "#.......#"
            )
        )

        val pause = emptyList<Pair<Int, Int>>()
        return listOf(
            letterT, letterT, pause,
            letterA, letterA, pause,
            letterM, letterM, pause,
            letterA, letterA, pause
        )
    }

    /** Rundliches Pixel-Haustier huepft leicht, blinzelt (Luecke im Gesicht wie bei der Katze), Fuehler/Fuesse je nach Sprungphase. */
    private fun tamagotchiFrames(): List<List<Pair<Int, Int>>> {
        fun blob(dy: Int, eyesOpen: Boolean): List<Pair<Int, Int>> {
            var row6 = listOf(4 to 6, 5 to 6, 6 to 6, 7 to 6, 8 to 6, 9 to 6)
            if (eyesOpen) row6 = row6.filterNot { it == (5 to 6) || it == (8 to 6) }
            return shift(
                listOf(
                    6 to 3, 7 to 3,
                    5 to 4, 6 to 4, 7 to 4, 8 to 4,
                    4 to 5, 5 to 5, 6 to 5, 7 to 5, 8 to 5, 9 to 5
                ) + row6 + listOf(
                    4 to 7, 5 to 7, 6 to 7, 7 to 7, 8 to 7, 9 to 7,
                    5 to 8, 6 to 8, 7 to 8, 8 to 8,
                    6 to 9, 7 to 9
                ),
                dy = dy
            )
        }

        fun feet(dy: Int, down: Boolean) = if (down) shift(listOf(5 to 10, 8 to 10), dy = dy) else emptyList()
        fun antenna(dy: Int, up: Boolean) = if (up) shift(listOf(6 to 1, 7 to 1), dy = dy) else emptyList()

        val dys = listOf(0, -1, 0, 1, 0, 0, -1, 1)
        val eyesOpenList = listOf(true, true, true, true, false, true, true, true)
        val feetDownList = listOf(false, false, false, true, false, false, false, true)
        val antennaUpList = listOf(false, true, false, false, false, false, true, false)

        return dys.indices.map { i ->
            blob(dys[i], eyesOpenList[i]) + feet(dys[i], feetDownList[i]) + antenna(dys[i], antennaUpList[i])
        }
    }

    /** Atemkreis: Ring pulsiert klein->gross->klein um einen festen Mittelpunkt - Ein-/Ausatem-Impuls fuer einen ruhigen Moment. */
    private fun breatheFrames(): List<List<Pair<Int, Int>>> {
        val center = listOf(6 to 6)
        val radii = listOf(1, 2, 3, 4, 5, 4, 3, 2)
        return radii.map { r ->
            val steps = maxOf(8, r * 6)
            center + circlePoints(cx = 6, cy = 6, radius = r, steps = steps)
        }
    }

    private fun ball(cx: Int, cy: Int) = listOf(cx to cy, (cx - 1) to cy, (cx + 1) to cy, cx to (cy - 1), cx to (cy + 1))

    /**
     * **Ein Spieler schiesst den Ball weg** - das Motiv fuer die Untergruppe `sport/ballsport`.
     *
     * Der erste Entwurf zeigte einen Ball, der in ein Tor am rechten Rand rollt. Das war aus drei
     * Gruenden nicht zu erkennen:
     *
     * 1. Das Tor sass mit seinem rechten Pfosten auf `x = 12` und damit auf der Kante des runden
     *    Ausschnitts - seine obere Ecke fiel weg, es blieb ein einseitig offener Rahmen.
     * 2. Ball und Pfosten waren beide einen Punkt breit und gleich hell. Sobald der Ball das Tor
     *    erreichte, verschmolz er damit; die letzten vier von neun Frames waren ein Rechteck, das
     *    sich fuellt.
     * 3. Es war ausserdem dasselbe Motiv wie [SkillTreeAnimations] `Shot`, das unter diesem Knoten
     *    als Blatt haengt - und die schlechtere Fassung davon.
     *
     * Deshalb jetzt eine Geste statt eines Aufbaus: eine Figur links, ein deutlich runder Ball
     * rechts, das Bein holt aus, trifft, zieht nach. Der Ball verlaesst das Bild nach rechts oben
     * und rollt am Boden wieder herein, damit die Schleife ohne Sprung zurueckfindet.
     *
     * Der Ball ist mit fuenf Zellen Durchmesser bewusst gross: kleiner bleibt von einer Kugel auf
     * diesem Raster nur ein Kreuz uebrig. Ein Muster traegt er nicht - bei dieser Groesse frisst
     * jeder dunkle Flicken die runde Silhouette auf, und dann ist es weder Ball noch Fussball.
     */
    private fun footballFrames(): List<List<Pair<Int, Int>>> {
        /** Kopf, Schultern, Rumpf. Das Bein kommt je Pose dazu. */
        fun torso() = sprite(4, 0, "#") + sprite(3, 1, "###") + sprite(4, 2, "#")

        /**
         * Der Boden reicht NICHT ueber die volle Breite - die Matrix ist rund, in der untersten
         * Zeile gibt es nur `x = 2..10` (dieselbe Falle wie beim Dribbling, siehe SKILLBAUM.md).
         */
        fun pitch() = sprite(2, 11, "#########")

        fun ball(cx: Int, cy: Int) = sprite(
            cx - 2, cy - 2,
            ".###.",
            "#####",
            "#####",
            "#####",
            ".###."
        )

        val stand = sprite(4, 3, "#", "#", "#", "#", "#", "#", "#", "###")
        val windUp = sprite(0, 3, "....#", "...##", "...#.", "..##.", ".##..", "###..")
        val strike = sprite(4, 3, "#......", "##.....", ".#.....", ".##....", "..#....", "...#...", "...####")
        val followThrough = sprite(4, 3, "#........", ".#.......", "..#......", "...###...", ".....####")
        val recover = sprite(4, 3, "#....", "#....", "##...", ".#...", ".#...", ".##..", "..#..", "..###")

        fun frame(leg: List<Pair<Int, Int>>, ballAt: Pair<Int, Int>) =
            torso() + leg + pitch() + ball(ballAt.first, ballAt.second)

        val rest = 9 to 8
        return listOf(
            frame(stand, rest),
            frame(windUp, rest),
            frame(strike, rest),
            frame(followThrough, 10 to 6),
            frame(followThrough, 12 to 4),
            frame(recover, 12 to 8),
            frame(stand, rest),
            frame(stand, rest)
        )
    }

    /** Ball fliegt im Bogen in den Korb, Netz swisht beim Durchgang. */
    private fun basketballFrames(): List<List<Pair<Int, Int>>> {
        // Korb zwei Zeilen tiefer und eine Spalte weiter innen als urspruenglich: das Brett sass
        // vorher bei x 9..12, y 0..3 - der oberen rechten Ecke, die der runde Ausschnitt der
        // Matrix abschneidet. Rund ein Viertel der Punkte war unsichtbar.
        val backboard = listOf(
            8 to 2, 9 to 2, 10 to 2, 11 to 2, 8 to 3, 11 to 3, 8 to 4, 11 to 4, 8 to 5, 9 to 5, 10 to 5, 11 to 5
        )
        val rim = listOf(5 to 6, 6 to 6, 7 to 6, 8 to 6)
        val netCalm = listOf(6 to 7, 7 to 7, 6 to 8, 7 to 8)
        val netSwish = listOf(5 to 7, 6 to 7, 7 to 7, 8 to 7, 5 to 8, 8 to 8, 6 to 9, 7 to 9)

        val positions = listOf(1 to 10, 2 to 9, 3 to 8, 4 to 7, 6 to 6)
        val frames = positions.map { (x, y) -> ball(x, y) + backboard + rim + netCalm }.toMutableList()
        frames += ball(6, 7) + backboard + rim + netSwish
        frames += ball(6, 8) + backboard + rim + netSwish
        frames += backboard + rim + netCalm
        return frames
    }

    /** Kurzhantel wird auf und ab bewegt (Curl), ein Wiederholungszaehler tickt 1 -> 2 hoch. */
    private fun fitnessFrames(): List<List<Pair<Int, Int>>> {
        fun dumbbell(y: Int): List<Pair<Int, Int>> {
            val bar = (4..8).map { it to y }
            val plateL = listOf(3 to (y - 1), 3 to y, 3 to (y + 1), 2 to y)
            val plateR = listOf(9 to (y - 1), 9 to y, 9 to (y + 1), 10 to y)
            return bar + plateL + plateR
        }

        fun digit(n: Int): List<Pair<Int, Int>> = if (n == 1) {
            listOf(6 to 0, 5 to 1, 6 to 1, 6 to 2, 6 to 3, 5 to 4, 6 to 4, 7 to 4)
        } else {
            listOf(5 to 0, 6 to 0, 7 to 0, 7 to 1, 5 to 2, 6 to 2, 7 to 2, 7 to 3, 5 to 4, 6 to 4, 7 to 4)
        }

        val ys = listOf(9, 8, 6, 5, 5, 6, 8, 9, 8, 6, 5, 5)
        val reps = listOf(1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2)
        return ys.indices.map { i -> dumbbell(ys[i]) + digit(reps[i]) }
    }

    /** Roboterkopf mit blinkender Antenne und links-rechts wanderndem Scan-Auge, Arme winken abwechselnd. */
    private fun robotFrames(): List<List<Pair<Int, Int>>> {
        val head = listOf(
            4 to 3, 5 to 3, 6 to 3, 7 to 3, 8 to 3,
            4 to 4, 8 to 4,
            4 to 5, 8 to 5,
            4 to 6, 8 to 6,
            4 to 7, 5 to 7, 6 to 7, 7 to 7, 8 to 7
        )

        fun antenna(lit: Boolean) = listOf(6 to 1, 6 to 2) + if (lit) listOf(6 to 0) else emptyList()
        fun eye(x: Int) = listOf(x to 5)
        fun body(armLUp: Boolean, armRUp: Boolean): List<Pair<Int, Int>> {
            val core = listOf(5 to 8, 6 to 8, 7 to 8, 5 to 9, 6 to 9, 7 to 9, 5 to 10, 6 to 10, 7 to 10)
            val armL = if (armLUp) listOf(3 to 8) else listOf(3 to 9)
            val armR = if (armRUp) listOf(9 to 8) else listOf(9 to 9)
            return core + armL + armR
        }

        val eyeXs = listOf(5, 6, 7, 6, 5)
        val lits = listOf(true, false, true, false, true)
        val armLs = listOf(false, true, false, true, false)
        val armRs = listOf(false, false, true, false, false)

        return eyeXs.indices.map { i -> head + antenna(lits[i]) + eye(eyeXs[i]) + body(armLs[i], armRs[i]) }
    }

    /** Lagerfeuer flackert zwischen zwei Flammenformen, Funken steigen an wechselnden Stellen auf. */
    private fun fireFrames(): List<List<Pair<Int, Int>>> {
        val logs = listOf(3 to 10, 4 to 10, 8 to 10, 9 to 10, 2 to 11, 3 to 11, 9 to 11, 10 to 11)

        val flameTall = listOf(
            6 to 3,
            5 to 4, 6 to 4, 7 to 4,
            4 to 5, 5 to 5, 6 to 5, 7 to 5, 8 to 5,
            4 to 6, 5 to 6, 6 to 6, 7 to 6, 8 to 6,
            4 to 7, 5 to 7, 6 to 7, 7 to 7, 8 to 7,
            5 to 8, 6 to 8, 7 to 8
        )
        val flameWide = listOf(
            5 to 4, 6 to 4, 7 to 4,
            4 to 5, 5 to 5, 6 to 5, 7 to 5, 8 to 5,
            3 to 6, 4 to 6, 5 to 6, 6 to 6, 7 to 6, 8 to 6, 9 to 6,
            4 to 7, 5 to 7, 6 to 7, 7 to 7, 8 to 7,
            5 to 8, 6 to 8, 7 to 8
        )
        val shapes = listOf(flameTall, flameWide, flameTall, flameWide)
        val embers = listOf(
            listOf(5 to 2, 8 to 3),
            listOf(7 to 1, 4 to 3),
            listOf(6 to 0, 9 to 2),
            listOf(4 to 2, 8 to 1)
        )
        return shapes.indices.map { i -> shapes[i] + logs + embers[i] }
    }

    /** Pokal mit Henkeln bleibt stehen, ein Funkeln twinkelt reihum an den vier Ecken. */
    private fun trophyFrames(): List<List<Pair<Int, Int>>> {
        val cup = listOf(
            5 to 3, 6 to 3, 7 to 3,
            4 to 4, 5 to 4, 6 to 4, 7 to 4, 8 to 4,
            4 to 5, 8 to 5,
            5 to 6, 6 to 6, 7 to 6,
            3 to 4, 3 to 5,
            9 to 4, 9 to 5,
            6 to 7, 6 to 8,
            5 to 9, 6 to 9, 7 to 9,
            4 to 10, 5 to 10, 6 to 10, 7 to 10, 8 to 10
        )
        val sparklePositions = listOf(2 to 2, 10 to 2, 2 to 9, 10 to 9, 10 to 2, 2 to 2)
        return sparklePositions.map { pos -> cup + listOf(pos) }
    }

    /** Samen keimt, treibt einen Stiel mit Blaettern und blueht am Ende zur Blume auf. */
    private fun plantFrames(): List<List<Pair<Int, Int>>> {
        val soil = listOf(3 to 11, 4 to 11, 5 to 11, 6 to 11, 7 to 11, 8 to 11, 9 to 11)
        val seed = soil + listOf(6 to 10)
        val sprout = soil + listOf(6 to 10, 6 to 9, 5 to 9, 7 to 9)
        val stemSmall = soil + listOf(6 to 10, 6 to 9, 6 to 8, 5 to 8, 7 to 8)
        val stemTall = soil + listOf(6 to 10, 6 to 9, 6 to 8, 6 to 7, 5 to 7, 7 to 7, 4 to 8, 8 to 8)
        val bloom = soil + listOf(
            6 to 10, 6 to 9, 6 to 8, 6 to 7,
            4 to 8, 8 to 8,
            6 to 4,
            5 to 5, 7 to 5,
            5 to 3, 6 to 3, 7 to 3,
            5 to 4, 7 to 4, 6 to 5
        )
        return listOf(seed, seed, sprout, sprout, stemSmall, stemTall, bloom, bloom, bloom)
    }

    /** Pfeil fliegt von links ins Ziel, bleibt in der Mitte der Zielscheibe stecken. */
    private fun targetFrames(): List<List<Pair<Int, Int>>> {
        // Zielscheibe mittig statt bei cx=9: dort reichte der aeussere Ring bis x=14 und lag damit
        // teils sogar ausserhalb des 13x13-Rasters, teils in der abgeschnittenen Ecke. Zentriert
        // liegt der Ring mit Radius 5 vollstaendig innerhalb des Kreises (Radius 6,5).
        val cx = 6
        val cy = 6
        val target = circlePoints(cx, cy, 5, 28) + circlePoints(cx, cy, 3, 18) + listOf(cx to cy)

        fun dart(x: Int, y: Int) = listOf(x to y, (x - 1) to y, (x - 2) to y, (x - 1) to (y - 1), (x - 1) to (y + 1))

        val positions = listOf(3 to 6, 4 to 6, 5 to 6, 6 to 6)
        val frames = positions.map { (x, y) -> dart(x, y) + target }.toMutableList()
        frames += target + listOf(cx to cy)
        frames += target
        frames += target + listOf(cx to cy)
        return frames
    }

    /** Flugzeug bankt leicht waehrend es durchs Bild fliegt, hinterlaesst eine gestrichelte Kondensstreifen-Spur. */
    private fun airplaneFrames(): List<List<Pair<Int, Int>>> {
        fun plane(dy: Int) = shift(
            listOf(
                9 to 6, 8 to 6, 7 to 6, 6 to 6, 5 to 6, 4 to 6, 3 to 6,
                2 to 5, 2 to 7,
                6 to 4, 6 to 8,
                5 to 5, 5 to 7
            ),
            dy = dy
        )

        val steps = listOf(
            0 to listOf(1),
            -1 to listOf(0, 2),
            0 to listOf(0, 2, 4),
            1 to listOf(0, 2, 4),
            0 to listOf(0, 2, 4, 6),
            -1 to listOf(0, 2, 4, 6)
        )
        return steps.map { (dy, cols) -> plane(dy) + cols.map { c -> c to 6 } }
    }

    /** Kerze auf der Torte flackert, wird ausgepustet (Rauchfaehnchen), zuendet dann wieder. */
    private fun cakeFrames(): List<List<Pair<Int, Int>>> {
        val cake = listOf(
            4 to 8, 5 to 8, 6 to 8, 7 to 8, 8 to 8,
            3 to 9, 4 to 9, 5 to 9, 6 to 9, 7 to 9, 8 to 9, 9 to 9,
            3 to 10, 9 to 10,
            3 to 11, 4 to 11, 5 to 11, 6 to 11, 7 to 11, 8 to 11, 9 to 11,
            4 to 7, 6 to 7, 8 to 7
        )
        val candle = listOf(6 to 5, 6 to 6)
        val flameSmall = listOf(6 to 4)
        val flameBig = listOf(5 to 3, 6 to 3, 7 to 3, 6 to 4)
        val smoke = listOf(6 to 2, 7 to 1)

        return listOf(
            cake + candle + flameSmall,
            cake + candle + flameBig,
            cake + candle + flameSmall,
            cake + candle + flameBig,
            cake + candle + smoke,
            cake + candle,
            cake + candle + flameSmall,
            cake + candle + flameBig
        )
    }

    /** Gluehbirne: Gluehfaden leuchtet auf, Ideen-Strahlen pulsieren nach aussen und klingen wieder ab. */
    private fun lightbulbFrames(): List<List<Pair<Int, Int>>> {
        val bulb = listOf(
            5 to 2, 6 to 2, 7 to 2,
            4 to 3, 8 to 3,
            4 to 4, 8 to 4,
            4 to 5, 8 to 5,
            5 to 6, 7 to 6,
            5 to 7, 7 to 7
        )
        val base = listOf(5 to 8, 6 to 8, 7 to 8, 5 to 9, 6 to 9, 7 to 9)
        val filament = listOf(6 to 4, 5 to 5, 6 to 5, 7 to 5, 6 to 4)
        val raysSmall = listOf(2 to 4, 10 to 4)
        val raysBig = listOf(2 to 4, 10 to 4, 3 to 1, 9 to 1, 3 to 7, 9 to 7)

        return listOf(
            bulb + base,
            bulb + base + filament,
            bulb + base + filament + raysSmall,
            bulb + base + filament + raysBig,
            bulb + base + filament + raysSmall,
            bulb + base + filament,
            bulb + base
        )
    }

    /** Umschlag bleibt geschlossen (eine geoeffnete Klappe wirkte bei 13x13 wie ein Hausdach), stattdessen pulsiert ein Benachrichtigungs-Badge oben rechts auf. */
    private fun envelopeFrames(): List<List<Pair<Int, Int>>> {
        val body = listOf(
            3 to 6, 4 to 6, 5 to 6, 6 to 6, 7 to 6, 8 to 6, 9 to 6,
            3 to 7, 9 to 7,
            3 to 8, 9 to 8,
            3 to 9, 9 to 9,
            3 to 10, 4 to 10, 5 to 10, 6 to 10, 7 to 10, 8 to 10, 9 to 10
        )
        val flap = listOf(3 to 6, 4 to 7, 5 to 8, 6 to 9, 7 to 8, 8 to 7, 9 to 6)

        fun badge(size: Int): List<Pair<Int, Int>> = when (size) {
            0 -> emptyList()
            1 -> listOf(10 to 5)
            else -> listOf(9 to 4, 10 to 4, 11 to 4, 9 to 5, 11 to 5, 9 to 6, 10 to 6, 11 to 6)
        }

        return listOf(
            body + flap,
            body + flap + badge(1),
            body + flap + badge(2),
            body + flap + badge(2),
            body + flap + badge(1),
            body + flap,
            body + flap
        )
    }
}
