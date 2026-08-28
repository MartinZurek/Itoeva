package com.notime.glyphcore.data

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Je fuenf Animationen fuer jeden der sechs Avatare - **aus ihrem Charakter abgeleitet, nicht
 * beliebig zugeordnet.**
 *
 * Die 26 Animationen in [DefaultLibraryAnimations] sind bewusst allgemein: Stern, Welle, Rakete,
 * Kuchen - Motive, die zu jeder Erinnerung passen koennen und zu keinem Avatar besonders. Das ist
 * fuer eine Bibliothek richtig, laesst aber die Figuren selbst blass. Wer Hootlet gewaehlt hat,
 * den stillen Beobachter, bekam am Ende dieselbe Rakete zu sehen wie jemand mit Wyrmling.
 *
 * Dieser Satz gehoert dagegen jeweils EINER Figur: Fennecs Leuchtturm und Anker stehen fuer den
 * verlaesslichen Beschuetzer, Gloops Faultier-Tempo und Wolken fuer den Entschleuniger, Hootlets
 * Sanduhr und Schluessel fuer Geduld und Erkenntnis. Zusammen mit den eigenen Reaktionen
 * (`AvatarSignatureReactions` im :app-sim-Modul) wird aus der Avatar-Wahl damit eine Wahl des
 * Charakters und nicht nur der Silhouette.
 *
 * **Warum trotzdem in der gemeinsamen Bibliothek** und nicht fest an den Avatar gebunden: Sie sind
 * ganz normale [LibraryAnimation]-Eintraege, lassen sich also ansehen, im Picker auswaehlen und
 * fuer jede beliebige Erinnerung verwenden. Die Zuordnung zum Avatar ist eine gestalterische, keine
 * technische Sperre - Puffling darf Hootlets Sanduhr tragen, es passt nur weniger gut.
 *
 * Alle Punkte liegen im 13x13-Raster ([ReminderFrameGrid.SIZE]), Koordinaten 0..12.
 */
object AvatarSignatureAnimations {

    /** Anschluss an die Sortierung der 26 Grund-Animationen (0..25). */
    private const val SORT_OFFSET = 26

    fun seed(): List<LibraryAnimation> {
        val entries = listOf(
            // ---- PUFFLING: der neugierige Optimist ----
            // Lauter kleine Dinge, denen man nachschaut: eine Blase, ein Falter, ein Drachen.
            // Nichts davon ist wichtig - genau das ist der Punkt bei einer Figur, die sich ehrlich
            // ueber Kleinigkeiten freut.
            Triple("Bubble", "🫧", bubbleFrames()),
            Triple("Butterfly", "🦋", butterflyFrames()),
            Triple("Kite", "🪁", kiteFrames()),
            Triple("Snail", "🐌", snailFrames()),
            Triple("Compass", "🧭", compassFrames()),

            // ---- STARLET: die freundliche Traeumerin ----
            // Alles leuchtet leise oder schwebt: Komet, Laterne, Feder, Sternbild, Kerze. Kein
            // Motiv mit Kanten oder Tempo - sie erinnert daran, freundlich zu sich selbst zu sein.
            Triple("Comet", "☄️", cometFrames()),
            Triple("Lantern", "🏮", lanternFrames()),
            Triple("Feather", "🪶", featherFrames()),
            Triple("Constellation", "🌌", constellationFrames()),
            Triple("Candle", "🕯️", candleFrames()),

            // ---- WYRMLING: der kleine Motivator ----
            // Aufwaerts, immer: Blitz, Gipfel, Leiter, Trommel, Fahne. Bewegung mit Richtung -
            // mutig und positiv, aber (siehe Charakter) nie hektisch.
            Triple("Bolt", "⚡", boltFrames()),
            Triple("Summit", "🏔️", summitFrames()),
            Triple("Ladder", "🪜", ladderFrames()),
            Triple("Drum", "🥁", drumFrames()),
            Triple("Flag", "🚩", flagFrames()),

            // ---- FENNEC: der ruhige Beschuetzer ----
            // Motive des Aufpassens und Haltens: Schild, Leuchtturm, Pfotenspur, Nest, Anker.
            // Ruhige, gleichmaessige Bewegungen - er erinnert, ohne je zu draengen.
            Triple("Shield", "🛡️", shieldFrames()),
            Triple("Lighthouse", "🗼", lighthouseFrames()),
            Triple("Paw", "🐾", pawFrames()),
            Triple("Nest", "🪺", nestFrames()),
            Triple("Anchor", "⚓", anchorFrames()),

            // ---- GLOOP: der Entschleuniger ----
            // Nichts hat es eilig: Schildkroete, Wolke, Tropfen, Pfuetze, Luftballon. Die
            // langsamsten Motive des ganzen Satzes, und die einzigen, die zerlaufen duerfen.
            Triple("Turtle", "🐢", turtleFrames()),
            Triple("Cloud", "☁️", cloudFrames()),
            Triple("Drip", "🫠", dripFrames()),
            Triple("Puddle", "💦", puddleFrames()),
            Triple("Balloon", "🎈", balloonFrames()),

            // ---- HOOTLET: der weise Beobachter ----
            // Sehen, verstehen, warten: Auge, Schluessel, Sanduhr, Schriftrolle, Puzzleteil.
            // Motive, die eine Erkenntnis zeigen statt einer Handlung.
            Triple("Eye", "👁️", eyeFrames()),
            Triple("Key", "🔑", keyFrames()),
            Triple("Hourglass", "⏳", hourglassFrames()),
            Triple("Scroll", "📜", scrollFrames()),
            Triple("Puzzle", "🧩", puzzleFrames())
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

    // ================= Zeichenhilfen =================

    private fun line(x0: Int, y0: Int, x1: Int, y1: Int): List<Pair<Int, Int>> {
        val points = mutableListOf<Pair<Int, Int>>()
        var x = x0
        var y = y0
        val dx = abs(x1 - x0)
        val dy = -abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        while (true) {
            points += x to y
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 >= dy) { err += dy; x += sx }
            if (e2 <= dx) { err += dx; y += sy }
        }
        return points
    }

    private fun ring(cx: Int, cy: Int, radius: Double, steps: Int = 24): List<Pair<Int, Int>> {
        val seen = LinkedHashSet<Pair<Int, Int>>()
        for (i in 0 until steps) {
            val angle = 2 * PI * i / steps
            seen += (cx + radius * cos(angle)).roundToInt() to (cy + radius * sin(angle)).roundToInt()
        }
        return seen.toList()
    }

    /** Gefuellte Raute - Grundform fuer Drachen, Schild, Puzzleteil. */
    private fun diamond(cx: Int, cy: Int, r: Int): List<Pair<Int, Int>> {
        val pts = mutableListOf<Pair<Int, Int>>()
        for (dy in -r..r) {
            val width = r - abs(dy)
            for (dx in -width..width) pts += (cx + dx) to (cy + dy)
        }
        return pts
    }

    private fun shift(pts: List<Pair<Int, Int>>, dx: Int = 0, dy: Int = 0): List<Pair<Int, Int>> =
        pts.map { (x, y) -> (x + dx) to (y + dy) }

    /** Haelt alles im Raster - eine Figur, die halb aus dem Bild laeuft, soll abgeschnitten
     *  werden statt beim Zeichnen zu stoeren. */
    private fun clip(pts: List<Pair<Int, Int>>): List<Pair<Int, Int>> =
        pts.filter { (x, y) -> x in 0..12 && y in 0..12 }

    // ================= PUFFLING =================

    /** Blase steigt auf, wird groesser, zittert - und platzt in vier auseinanderfliegende Spritzer. */
    private fun bubbleFrames(): List<List<Pair<Int, Int>>> = listOf(
        clip(ring(6, 10, 1.5)),
        clip(ring(6, 8, 2.0)),
        clip(ring(6, 6, 2.5) + listOf(5 to 5)),
        // Kurzes Zittern kurz vorm Platzen - das ist der Moment, der die Blase lebendig macht.
        clip(ring(7, 5, 2.5) + listOf(6 to 4)),
        clip(ring(5, 4, 2.5) + listOf(4 to 3)),
        // Platzen: Ring reisst in vier Spritzer auf.
        clip(listOf(3 to 2, 8 to 2, 3 to 7, 8 to 7, 5 to 1, 6 to 8)),
        clip(listOf(2 to 1, 9 to 1, 2 to 8, 9 to 8)),
        clip(listOf(1 to 0, 10 to 0))
    )

    /** Falter flattert quer durchs Bild, Fluegel offen/zu - die Flughoehe wippt dabei mit. */
    private fun butterflyFrames(): List<List<Pair<Int, Int>>> {
        fun wings(cx: Int, cy: Int, open: Boolean): List<Pair<Int, Int>> {
            val body = listOf(cx to cy, cx to (cy + 1))
            return body + if (open) {
                listOf(
                    cx - 2 to cy - 1, cx - 1 to cy - 1, cx - 2 to cy, cx - 1 to cy + 1, cx - 2 to cy + 2,
                    cx + 2 to cy - 1, cx + 1 to cy - 1, cx + 2 to cy, cx + 1 to cy + 1, cx + 2 to cy + 2
                )
            } else {
                listOf(cx - 1 to cy - 1, cx - 1 to cy, cx - 1 to cy + 1, cx + 1 to cy - 1, cx + 1 to cy, cx + 1 to cy + 1)
            }
        }
        return listOf(
            clip(wings(2, 8, true)),
            clip(wings(4, 6, false)),
            clip(wings(6, 7, true)),
            clip(wings(8, 5, false)),
            clip(wings(10, 6, true)),
            clip(wings(8, 4, false)),
            clip(wings(6, 5, true))
        )
    }

    /** Drachen steigt schraeg auf, die Schnur schlaengelt hinterher. */
    private fun kiteFrames(): List<List<Pair<Int, Int>>> {
        fun kite(cx: Int, cy: Int, tailPhase: Int): List<Pair<Int, Int>> {
            val body = diamond(cx, cy, 2)
            val tail = when (tailPhase) {
                0 -> listOf(cx to cy + 3, cx - 1 to cy + 4, cx to cy + 5)
                1 -> listOf(cx to cy + 3, cx + 1 to cy + 4, cx to cy + 5)
                else -> listOf(cx to cy + 3, cx to cy + 4, cx + 1 to cy + 5)
            }
            return body + tail
        }
        return listOf(
            clip(kite(4, 8, 0)),
            clip(kite(5, 7, 1)),
            clip(kite(6, 6, 2)),
            clip(kite(7, 5, 0)),
            clip(kite(8, 4, 1)),
            clip(kite(7, 3, 2)),
            clip(kite(6, 4, 0))
        )
    }

    /**
     * Schnecke kriecht nach rechts; die Fuehler tasten voraus.
     *
     * Das Haus als echter Kreisring statt handgesetzter Boegen - ein von Hand geplottetes
     * "Schneckenhaus" wurde auf diesem Raster zu einem unlesbaren Klumpen. Der Ring gibt die
     * runde Form, der Punkt in der Mitte reicht als Andeutung der Spirale.
     */
    private fun snailFrames(): List<List<Pair<Int, Int>>> {
        fun snail(x: Int, antennaUp: Boolean): List<Pair<Int, Int>> {
            val shell = ring(x + 3, 6, 2.5, 18) + listOf(x + 3 to 6, x + 4 to 5)
            val foot = (x..x + 8).map { it to 9 }
            val neck = listOf(x + 7 to 8, x + 8 to 8)
            val antenna = if (antennaUp) {
                listOf(x + 9 to 7, x + 9 to 6)
            } else {
                listOf(x + 9 to 7, x + 10 to 7)
            }
            return shell + foot + neck + antenna
        }
        return listOf(
            clip(snail(0, false)),
            clip(snail(0, true)),
            clip(snail(1, true)),
            clip(snail(1, false)),
            clip(snail(2, true)),
            clip(snail(2, false)),
            clip(snail(2, true))
        )
    }

    /** Kompassnadel dreht sich mehrfach, pendelt aus und bleibt nach Norden stehen. */
    private fun compassFrames(): List<List<Pair<Int, Int>>> {
        val housing = ring(6, 6, 5.0, 28)
        fun needle(angleDeg: Double): List<Pair<Int, Int>> {
            val rad = angleDeg * PI / 180.0
            val tip = (6 + 3.5 * sin(rad)).roundToInt() to (6 - 3.5 * cos(rad)).roundToInt()
            val tail = (6 - 2.5 * sin(rad)).roundToInt() to (6 + 2.5 * cos(rad)).roundToInt()
            return line(tail.first, tail.second, tip.first, tip.second)
        }
        // Schnell herum, dann immer kleinere Ausschlaege um Norden - ein Kompass ruckelt nicht
        // in seine Lage, er schwingt sich ein.
        return listOf(0.0, 120.0, 240.0, 40.0, 330.0, 15.0, 355.0, 0.0).map { angle ->
            clip(housing + needle(angle))
        }
    }

    // ================= STARLET =================

    /** Komet zieht diagonal durchs Bild, der Schweif wird laenger und loest sich am Ende in Funken auf. */
    private fun cometFrames(): List<List<Pair<Int, Int>>> {
        fun comet(hx: Int, hy: Int, tailLength: Int): List<Pair<Int, Int>> {
            // Eine kleine Raute statt eines 2x2-Blocks: klarer Kopf und genug Masse, damit der
            // Komet auf dem runden Display nicht wie eine duenne diagonale Linie wirkt.
            val head = listOf(
                hx to hy - 1, hx - 1 to hy, hx to hy, hx + 1 to hy, hx to hy + 1
            )
            val tail = (1..tailLength).flatMap { k ->
                listOf(
                    (hx - k) to (hy - k),
                    (hx - k - 1) to (hy - k),
                    (hx - k) to (hy - k + 1)
                )
            }
            return head + tail
        }
        return listOf(
            clip(comet(3, 2, 1)),
            clip(comet(4, 3, 2)),
            clip(comet(6, 5, 3)),
            clip(comet(8, 7, 4)),
            clip(comet(9, 9, 4)),
            // Aufloesen: nur noch versprengte Funken entlang der Bahn.
            clip(listOf(11 to 11, 8 to 8, 5 to 5, 2 to 2)),
            clip(listOf(9 to 9, 4 to 4))
        )
    }

    /** Laterne haengt still, ihr Licht atmet - und steigt am Ende ein Stueck auf. */
    private fun lanternFrames(): List<List<Pair<Int, Int>>> {
        fun lantern(topY: Int, glow: Int): List<Pair<Int, Int>> {
            val hanger = listOf(6 to topY, 6 to topY + 1)
            val bodyTop = listOf(4 to topY + 2, 5 to topY + 2, 6 to topY + 2, 7 to topY + 2, 8 to topY + 2)
            val sides = (topY + 3..topY + 5).flatMap { y -> listOf(3 to y, 9 to y) }
            val bodyBottom = listOf(4 to topY + 6, 5 to topY + 6, 6 to topY + 6, 7 to topY + 6, 8 to topY + 6)
            // Der Kern glimmt in drei Stufen - das ist die eigentliche Bewegung, der Rest steht.
            val core = when (glow) {
                0 -> listOf(6 to topY + 4)
                1 -> listOf(5 to topY + 4, 6 to topY + 4, 7 to topY + 4, 6 to topY + 3)
                else -> listOf(
                    5 to topY + 3, 6 to topY + 3, 7 to topY + 3,
                    5 to topY + 4, 6 to topY + 4, 7 to topY + 4,
                    5 to topY + 5, 6 to topY + 5, 7 to topY + 5
                )
            }
            return hanger + bodyTop + sides + bodyBottom + core
        }
        return listOf(
            clip(lantern(3, 0)),
            clip(lantern(3, 1)),
            clip(lantern(3, 2)),
            clip(lantern(3, 1)),
            clip(lantern(2, 2)),
            clip(lantern(1, 1)),
            clip(lantern(1, 0))
        )
    }

    /** Feder sinkt langsam und pendelt dabei seitlich - nie geradeaus, nie schnell. */
    private fun featherFrames(): List<List<Pair<Int, Int>>> {
        fun feather(cx: Int, cy: Int, lean: Int): List<Pair<Int, Int>> {
            val shaft = line(cx, cy, cx + lean, cy + 4)
            val vane = listOf(
                cx - 1 to cy + 1, cx - 2 to cy + 2, cx - 1 to cy + 3,
                cx + 1 to cy + 1, cx + 2 to cy + 2, cx + 1 to cy + 3
            )
            return shaft + vane
        }
        return listOf(
            clip(feather(6, 0, 1)),
            clip(feather(7, 2, -1)),
            clip(feather(5, 3, 1)),
            clip(feather(7, 5, -1)),
            clip(feather(5, 6, 1)),
            clip(feather(6, 8, 0))
        )
    }

    /** Sternbild: Punkte tauchen einzeln auf, dann ziehen sich Linien dazwischen - aus Einzelnem
     *  wird ein Bild. */
    private fun constellationFrames(): List<List<Pair<Int, Int>>> {
        val stars = listOf(2 to 3, 6 to 1, 9 to 4, 10 to 8, 6 to 7, 3 to 9)
        val links = listOf(
            line(2, 3, 6, 1), line(6, 1, 9, 4), line(9, 4, 10, 8),
            line(10, 8, 6, 7), line(6, 7, 3, 9), line(3, 9, 2, 3)
        )
        return listOf(
            clip(stars.take(2)),
            clip(stars.take(4)),
            clip(stars),
            clip(stars + links[0] + links[1]),
            clip(stars + links[0] + links[1] + links[2] + links[3]),
            clip(stars + links.flatten()),
            // Kurzes Aufleuchten der Knoten, dann steht das fertige Bild.
            clip(stars.flatMap { (x, y) -> listOf(x to y, x to y - 1) } + links.flatten()),
            clip(stars + links.flatten())
        )
    }

    /** Kerze brennt ruhig, die Flamme flackert unregelmaessig und duckt sich einmal kurz. */
    private fun candleFrames(): List<List<Pair<Int, Int>>> {
        val body = (7..11).flatMap { y -> listOf(5 to y, 6 to y, 7 to y) }
        val holder = listOf(4 to 12, 5 to 12, 6 to 12, 7 to 12, 8 to 12)
        val wick = listOf(6 to 6)
        fun flame(shape: Int): List<Pair<Int, Int>> = when (shape) {
            0 -> listOf(6 to 5, 6 to 4)
            1 -> listOf(6 to 5, 6 to 4, 5 to 4, 6 to 3)
            2 -> listOf(6 to 5, 6 to 4, 7 to 4, 6 to 3, 6 to 2)
            else -> listOf(6 to 5)
        }
        return listOf(
            clip(body + holder + wick + flame(0)),
            clip(body + holder + wick + flame(1)),
            clip(body + holder + wick + flame(2)),
            clip(body + holder + wick + flame(1)),
            // Der Zug, der sie fast ausgehen laesst - und sie richtet sich wieder auf.
            clip(body + holder + wick + flame(3)),
            clip(body + holder + wick + flame(1)),
            clip(body + holder + wick + flame(2))
        )
    }

    // ================= WYRMLING =================

    /** Blitz zeichnet sich von oben nach unten, schlaegt ein und blitzt einmal ganz auf. */
    private fun boltFrames(): List<List<Pair<Int, Int>>> {
        val stroke = listOf(
            7 to 0, 7 to 1, 6 to 2, 6 to 3, 5 to 4,
            8 to 4, 7 to 5, 7 to 6, 6 to 7, 6 to 8, 5 to 9, 5 to 10
        )
        val impact = listOf(3 to 11, 4 to 11, 6 to 11, 7 to 11, 2 to 12, 8 to 12)
        return listOf(
            clip(stroke.take(3)),
            clip(stroke.take(6)),
            clip(stroke.take(9)),
            clip(stroke),
            clip(stroke + impact),
            // Voller Aufblitz - kurz steht alles unter Strom.
            clip(stroke + impact + ring(6, 6, 5.5, 26)),
            clip(stroke + impact),
            clip(impact)
        )
    }

    /**
     * Ein Punkt steigt die Flanke hinauf zum Gipfel, wo sich eine Fahne aufrichtet.
     *
     * Der Berg endet bewusst bei y=10 und nicht am unteren Rand: Angezeigt wird nur der in das
     * 13x13-Quadrat einbeschriebene KREIS (siehe `MatrixGeometry.isActive` im :app-sim-Modul) -
     * ein bis in die Ecken gezogener Hang verlor dort knapp ein Fuenftel seiner Punkte und
     * franste sichtbar aus, statt als Flanke zu lesen.
     */
    private fun summitFrames(): List<List<Pair<Int, Int>>> {
        val mountain = line(3, 10, 6, 3) + line(6, 3, 9, 10) + (3..9).map { it to 10 }
        // Bewusst NEBEN der Hangkante und nicht darauf: Punkte, die genau auf der Linie liegen,
        // gehen in ihr auf - der Kletterer waere unsichtbar und die Frames identisch.
        val climberPath = listOf(2 to 10, 3 to 8, 3 to 7, 4 to 5, 4 to 4, 5 to 3)
        val flagPole = listOf(6 to 2, 6 to 1)
        return listOf(
            clip(mountain + listOf(climberPath[0])),
            clip(mountain + listOf(climberPath[1])),
            clip(mountain + listOf(climberPath[2])),
            clip(mountain + listOf(climberPath[3])),
            clip(mountain + listOf(climberPath[4])),
            clip(mountain + listOf(climberPath[5])),
            clip(mountain + listOf(climberPath[5]) + flagPole),
            clip(mountain + listOf(climberPath[5]) + flagPole + listOf(7 to 1, 8 to 1, 7 to 2))
        )
    }

    /** Sprosse fuer Sprosse nach oben - die Leiter selbst steht, nur der Kletterpunkt wandert. */
    private fun ladderFrames(): List<List<Pair<Int, Int>>> {
        val rails = (1..11).flatMap { y -> listOf(4 to y, 8 to y) }
        val rungs = listOf(2, 4, 6, 8, 10).flatMap { y -> listOf(5 to y, 6 to y, 7 to y) }
        val ladder = rails + rungs
        val steps = listOf(10, 8, 6, 4, 2)
        return steps.map { y -> clip(ladder + listOf(6 to y - 1, 6 to y)) } +
            // Oben angekommen: kurzer Sprung ueber die letzte Sprosse hinaus.
            listOf(clip(ladder + listOf(6 to 0)), clip(ladder))
    }

    /** Zwei Schlaegel treffen abwechselnd auf, bei jedem Schlag springen Klanglinien weg. */
    private fun drumFrames(): List<List<Pair<Int, Int>>> {
        val shell = (7..10).flatMap { y -> listOf(3 to y, 9 to y) } +
            listOf(3 to 6, 4 to 6, 5 to 6, 6 to 6, 7 to 6, 8 to 6, 9 to 6) +
            listOf(4 to 11, 5 to 11, 6 to 11, 7 to 11, 8 to 11)
        fun stick(left: Boolean, down: Boolean): List<Pair<Int, Int>> {
            val x = if (left) 4 else 8
            return if (down) listOf(x to 4, x to 5) else listOf(x to 2, x to 3)
        }
        val hitLeft = listOf(1 to 5, 2 to 4)
        val hitRight = listOf(11 to 5, 10 to 4)
        return listOf(
            clip(shell + stick(true, false) + stick(false, false)),
            clip(shell + stick(true, true) + stick(false, false) + hitLeft),
            clip(shell + stick(true, false) + stick(false, false)),
            clip(shell + stick(true, false) + stick(false, true) + hitRight),
            clip(shell + stick(true, false) + stick(false, false)),
            clip(shell + stick(true, true) + stick(false, true) + hitLeft + hitRight),
            clip(shell + stick(true, false) + stick(false, false))
        )
    }

    /** Fahne weht in einer durchlaufenden Welle - der Mast bleibt, das Tuch arbeitet. */
    private fun flagFrames(): List<List<Pair<Int, Int>>> {
        val pole = (1..12).map { y -> 3 to y }
        fun cloth(phase: Int): List<Pair<Int, Int>> {
            val pts = mutableListOf<Pair<Int, Int>>()
            for (dx in 1..6) {
                val wave = (1.4 * sin((dx + phase) * 0.9)).roundToInt()
                for (dy in 0..3) pts += (3 + dx) to (2 + dy + wave)
            }
            return pts
        }
        return (0 until 6).map { phase -> clip(pole + cloth(phase)) }
    }

    // ================= FENNEC =================

    /** Schild baut sich von aussen nach innen auf und haelt dann stand - zweimal pulsiert der Rand. */
    private fun shieldFrames(): List<List<Pair<Int, Int>>> {
        val outline = listOf(
            3 to 2, 4 to 2, 5 to 2, 6 to 2, 7 to 2, 8 to 2, 9 to 2,
            3 to 3, 9 to 3, 3 to 4, 9 to 4, 3 to 5, 9 to 5, 3 to 6, 9 to 6,
            4 to 7, 8 to 7, 4 to 8, 8 to 8, 5 to 9, 7 to 9, 6 to 10
        )
        val crest = listOf(6 to 4, 6 to 5, 5 to 5, 7 to 5, 6 to 6)
        return listOf(
            clip(outline.take(7)),
            clip(outline.take(15)),
            clip(outline),
            clip(outline + crest),
            // Standhalten: der Rand verstaerkt sich kurz, statt dass sich etwas bewegt.
            clip(outline + crest + listOf(2 to 3, 10 to 3, 2 to 5, 10 to 5)),
            clip(outline + crest),
            clip(outline + crest + listOf(2 to 4, 10 to 4)),
            clip(outline + crest)
        )
    }

    /** Turm steht fest, der Strahl wandert gleichmaessig einmal herum - Aufmerksamkeit ohne Hektik. */
    private fun lighthouseFrames(): List<List<Pair<Int, Int>>> {
        val tower = listOf(
            5 to 5, 6 to 5, 7 to 5,
            5 to 6, 7 to 6, 5 to 7, 7 to 7, 4 to 8, 8 to 8, 4 to 9, 8 to 9,
            4 to 10, 8 to 10, 3 to 11, 4 to 11, 5 to 11, 6 to 11, 7 to 11, 8 to 11, 9 to 11
        )
        val lamp = listOf(6 to 4, 6 to 3)
        fun beam(angleDeg: Double): List<Pair<Int, Int>> {
            val rad = angleDeg * PI / 180.0
            val ex = (6 + 5 * cos(rad)).roundToInt()
            val ey = (3 + 5 * sin(rad)).roundToInt()
            val spread = line(6, 3, ex, ey)
            val ex2 = (6 + 5 * cos(rad + 0.35)).roundToInt()
            val ey2 = (3 + 5 * sin(rad + 0.35)).roundToInt()
            return spread + line(6, 3, ex2, ey2)
        }
        return listOf(200.0, 225.0, 250.0, 290.0, 315.0, 340.0).map { angle ->
            clip(tower + lamp + beam(angle))
        }
    }

    /** Pfotenspur laeuft diagonal ins Bild - Abdruck fuer Abdruck, die aelteren verblassen nicht,
     *  sondern bleiben als Spur stehen. */
    private fun pawFrames(): List<List<Pair<Int, Int>>> {
        fun paw(cx: Int, cy: Int): List<Pair<Int, Int>> = listOf(
            cx to cy, cx + 1 to cy, cx to cy + 1, cx + 1 to cy + 1,
            cx - 1 to cy - 1, cx + 1 to cy - 2, cx + 2 to cy - 1
        )
        // Einen Schritt von den Ecken eingerueckt: Die alten ersten und letzten Zehen lagen im
        // abgeschnittenen Rand des runden Displays.
        val trail = listOf(paw(2, 9), paw(4, 7), paw(7, 5), paw(9, 3))
        return listOf(
            clip(trail[0]),
            clip(trail[0] + trail[1]),
            clip(trail[0] + trail[1] + trail[2]),
            clip(trail.flatten()),
            // Der letzte Abdruck druecktion sich noch einmal nach - dann steht die Spur.
            clip(trail.flatten() + listOf(10 to 2, 11 to 3)),
            clip(trail.flatten())
        )
    }

    /** Nest wiegt sich sanft, ein Ei kommt dazu - Fennecs Motiv fuers Behueten. */
    private fun nestFrames(): List<List<Pair<Int, Int>>> {
        fun nest(tilt: Int): List<Pair<Int, Int>> = listOf(
            2 to 7 + tilt, 3 to 8, 4 to 9, 5 to 9, 6 to 9, 7 to 9, 8 to 9, 9 to 8, 10 to 7 - tilt,
            3 to 9, 9 to 9, 4 to 10, 5 to 10, 6 to 10, 7 to 10, 8 to 10
        )
        val eggOne = listOf(5 to 7, 6 to 7, 5 to 8, 6 to 8)
        val eggTwo = listOf(7 to 7, 8 to 7, 7 to 8, 8 to 8)
        return listOf(
            clip(nest(0)),
            clip(nest(-1) + eggOne),
            clip(nest(1) + eggOne),
            clip(nest(0) + eggOne + eggTwo),
            clip(nest(-1) + eggOne + eggTwo),
            clip(nest(1) + eggOne + eggTwo),
            clip(nest(0) + eggOne + eggTwo)
        )
    }

    /** Anker sinkt, fasst und haelt - danach schwingt nur noch die Kette nach. */
    private fun anchorFrames(): List<List<Pair<Int, Int>>> {
        fun anchor(topY: Int, swing: Int): List<Pair<Int, Int>> {
            val ringPart = listOf(6 to topY, 5 to topY + 1, 7 to topY + 1, 6 to topY + 2)
            val shaft = (topY + 2..topY + 7).map { y -> 6 to y }
            val cross = listOf(4 to topY + 3, 5 to topY + 3, 7 to topY + 3, 8 to topY + 3)
            val flukes = listOf(
                3 to topY + 6, 4 to topY + 7, 5 to topY + 7,
                9 to topY + 6, 8 to topY + 7, 7 to topY + 7
            )
            return shift(ringPart + shaft + cross + flukes, dx = swing)
        }
        return listOf(
            clip(anchor(0, 0)),
            clip(anchor(1, 1)),
            clip(anchor(2, -1)),
            clip(anchor(3, 1)),
            clip(anchor(4, 0)),
            // Gefasst: der Anker steht, nur die Kette darueber pendelt aus.
            clip(anchor(5, 0) + listOf(6 to 0, 6 to 1)),
            clip(anchor(5, 0) + listOf(7 to 0, 6 to 1)),
            clip(anchor(5, 0) + listOf(6 to 0, 6 to 1))
        )
    }

    // ================= GLOOP =================

    /** Schildkroete zuckelt nach rechts, der Kopf kommt heraus und wieder herein - langsamster
     *  Ablauf des ganzen Satzes. */
    private fun turtleFrames(): List<List<Pair<Int, Int>>> {
        fun turtle(x: Int, headOut: Int, legPhase: Int): List<Pair<Int, Int>> {
            // Kuppel als Umriss: gefuellt verschmilzt der Panzer auf diesem Raster zu einem
            // Rechteck, und die Woelbung - das Einzige, was ihn als Panzer lesbar macht - geht
            // verloren.
            val dome = listOf(
                x + 2 to 4, x + 3 to 4, x + 4 to 4, x + 5 to 4,
                x + 1 to 5, x + 6 to 5,
                x + 0 to 6, x + 7 to 6
            )
            val belly = (x..x + 7).map { it to 7 }
            val pattern = listOf(x + 3 to 5, x + 4 to 6)
            val head = when (headOut) {
                0 -> listOf(x + 8 to 6)
                1 -> listOf(x + 8 to 6, x + 9 to 6)
                else -> listOf(x + 8 to 6, x + 9 to 6, x + 9 to 5)
            }
            val legs = if (legPhase == 0) {
                listOf(x + 1 to 8, x + 2 to 8, x + 5 to 8, x + 6 to 8)
            } else {
                listOf(x + 0 to 8, x + 1 to 8, x + 6 to 8, x + 7 to 8)
            }
            return dome + belly + pattern + head + legs
        }
        return listOf(
            clip(turtle(0, 0, 0)),
            clip(turtle(0, 1, 1)),
            clip(turtle(1, 2, 0)),
            clip(turtle(1, 1, 1)),
            clip(turtle(2, 2, 0)),
            clip(turtle(2, 1, 1)),
            clip(turtle(3, 0, 0))
        )
    }

    /** Wolke zieht traege vorbei und verformt sich dabei - sie kommt nie ganz zur Ruhe, hat es
     *  aber auch nie eilig. */
    private fun cloudFrames(): List<List<Pair<Int, Int>>> {
        fun cloud(x: Int, puff: Int): List<Pair<Int, Int>> {
            val base = listOf(
                x + 1 to 7, x + 2 to 7, x + 3 to 7, x + 4 to 7, x + 5 to 7, x + 6 to 7, x + 7 to 7,
                x + 2 to 6, x + 3 to 6, x + 5 to 6, x + 6 to 6
            )
            val top = when (puff) {
                0 -> listOf(x + 3 to 5, x + 4 to 5)
                1 -> listOf(x + 2 to 5, x + 3 to 5, x + 4 to 5, x + 5 to 5, x + 4 to 4)
                else -> listOf(x + 4 to 5, x + 5 to 5, x + 5 to 4)
            }
            return base + top
        }
        return listOf(
            clip(cloud(0, 0)),
            clip(cloud(0, 1)),
            clip(cloud(1, 2)),
            clip(cloud(1, 1)),
            clip(cloud(2, 0)),
            clip(cloud(2, 1)),
            clip(cloud(3, 2))
        )
    }

    /** Ein Tropfen zieht sich in die Laenge, reisst ab und faellt - und die Quelle zieht sich
     *  wieder zusammen. */
    private fun dripFrames(): List<List<Pair<Int, Int>>> {
        val source = listOf(4 to 1, 5 to 1, 6 to 1, 7 to 1, 8 to 1, 5 to 2, 6 to 2, 7 to 2)
        return listOf(
            clip(source + listOf(6 to 3)),
            clip(source + listOf(6 to 3, 6 to 4)),
            clip(source + listOf(6 to 3, 6 to 4, 6 to 5)),
            // Abriss: der Tropfen loest sich, oben bleibt eine Spitze zurueck.
            clip(source + listOf(6 to 3) + listOf(6 to 6, 6 to 7)),
            clip(source + listOf(6 to 8, 6 to 9, 5 to 9, 7 to 9)),
            clip(source + listOf(5 to 11, 6 to 11, 7 to 11)),
            // Aufschlag, der zur Seite laeuft.
            clip(source + listOf(3 to 12, 4 to 12, 5 to 12, 6 to 12, 7 to 12, 8 to 12, 9 to 12))
        )
    }

    /** Klecks faellt, zerlaeuft und schwappt aus - Ringe laufen bis zum Rand. */
    private fun puddleFrames(): List<List<Pair<Int, Int>>> = listOf(
        clip(listOf(6 to 3, 6 to 4)),
        clip(listOf(5 to 6, 6 to 6, 7 to 6, 6 to 7)),
        clip(listOf(4 to 8, 5 to 8, 6 to 8, 7 to 8, 8 to 8, 5 to 9, 6 to 9, 7 to 9)),
        // Aufsetzen und breit zerlaufen.
        clip(listOf(2 to 9, 3 to 9, 4 to 9, 8 to 9, 9 to 9, 10 to 9) + (3..9).map { it to 10 }),
        clip((1..11).map { it to 10 } + listOf(2 to 9, 10 to 9)),
        // Ringe laufen nach aussen weiter, die Pfuetze bleibt.
        clip((1..11).map { it to 10 } + listOf(0 to 9, 12 to 9, 1 to 8, 11 to 8)),
        clip((2..10).map { it to 10 })
    )

    /** Luftballon steigt gemaechlich, die Schnur schlaengelt hinterher. */
    private fun balloonFrames(): List<List<Pair<Int, Int>>> {
        fun balloon(cy: Int, stringPhase: Int): List<Pair<Int, Int>> {
            val skin = ring(6, cy, 2.5, 20)
            val knot = listOf(6 to cy + 3)
            val string = when (stringPhase) {
                0 -> listOf(6 to cy + 4, 5 to cy + 5, 6 to cy + 6)
                1 -> listOf(6 to cy + 4, 7 to cy + 5, 6 to cy + 6)
                else -> listOf(6 to cy + 4, 6 to cy + 5, 7 to cy + 6)
            }
            return skin + knot + string
        }
        return listOf(
            clip(balloon(8, 0)),
            clip(balloon(7, 1)),
            clip(balloon(6, 2)),
            clip(balloon(5, 0)),
            clip(balloon(4, 1)),
            clip(balloon(3, 2)),
            clip(balloon(3, 0))
        )
    }

    // ================= HOOTLET =================

    /**
     * Auge oeffnet sich, die Pupille wandert suchend hin und her, dann ein bedaechtiges Blinzeln.
     *
     * Als **Mandelform** gezeichnet (breiter als hoch, Spitzen seitlich) und nicht als Raute aus
     * vier Diagonalen: Die Raute lief mit der Pupille zu einem Klecks zusammen, in dem weder Lid
     * noch Iris zu erkennen waren. Die flachen Lidboegen halten die Mitte frei, sodass die
     * 3x3-Iris tatsaechlich als Blick lesbar ist - und ihre Wanderung als Suchen.
     */
    private fun eyeFrames(): List<List<Pair<Int, Int>>> {
        val corners = listOf(1 to 6, 11 to 6)
        val lowerLid = listOf(2 to 7, 3 to 7, 9 to 7, 10 to 7) + (4..8).map { it to 8 }
        val upperOpen = listOf(2 to 5, 3 to 5, 9 to 5, 10 to 5) + (4..8).map { it to 4 }
        val upperHalf = (2..10).map { it to 5 }
        val shut = (1..11).map { it to 6 }
        fun iris(cx: Int): List<Pair<Int, Int>> = (5..7).flatMap { y ->
            listOf((cx - 1) to y, cx to y, (cx + 1) to y)
        }
        val open = corners + lowerLid + upperOpen
        val half = corners + lowerLid + upperHalf
        return listOf(
            clip(shut),
            clip(half + iris(6)),
            clip(open + iris(6)),
            // Der Blick wandert - der Beobachter sucht, statt nur dazustehen.
            clip(open + iris(4)),
            clip(open + iris(6)),
            clip(open + iris(8)),
            clip(open + iris(6)),
            clip(half + iris(6)),
            clip(open + iris(6))
        )
    }

    /** Schluessel faehrt ins Schloss und dreht - der Bart wechselt dabei die Richtung. */
    private fun keyFrames(): List<List<Pair<Int, Int>>> {
        val lock = ring(9, 6, 2.5, 16) + listOf(9 to 7, 9 to 8)
        fun key(x: Int, turned: Boolean): List<Pair<Int, Int>> {
            val bow = ring(x, 6, 2.0, 12)
            val shaft = line(x + 2, 6, x + 5, 6)
            val bit = if (turned) {
                listOf(x + 4 to 5, x + 5 to 5)
            } else {
                listOf(x + 4 to 7, x + 5 to 7)
            }
            return bow + shaft + bit
        }
        return listOf(
            clip(lock + key(0, false)),
            clip(lock + key(1, false)),
            clip(lock + key(2, false)),
            clip(lock + key(3, false)),
            // Umdrehen: der Bart klappt auf die andere Seite - das ist das Aufschliessen.
            clip(lock + key(3, true)),
            clip(lock + key(3, false)),
            clip(lock + key(3, true))
        )
    }

    /** Sanduhr laeuft aus, wird gewendet und beginnt von vorn - Hootlets Geduld als Bild. */
    private fun hourglassFrames(): List<List<Pair<Int, Int>>> {
        val frame = listOf(3 to 1, 4 to 1, 5 to 1, 6 to 1, 7 to 1, 8 to 1, 9 to 1) +
            listOf(3 to 11, 4 to 11, 5 to 11, 6 to 11, 7 to 11, 8 to 11, 9 to 11) +
            line(3, 2, 6, 6) + line(9, 2, 6, 6) + line(6, 6, 3, 10) + line(6, 6, 9, 10)
        fun sandTop(level: Int): List<Pair<Int, Int>> = when (level) {
            3 -> listOf(4 to 2, 5 to 2, 6 to 2, 7 to 2, 8 to 2, 5 to 3, 6 to 3, 7 to 3, 6 to 4)
            2 -> listOf(5 to 3, 6 to 3, 7 to 3, 6 to 4)
            1 -> listOf(6 to 4)
            else -> emptyList()
        }
        fun sandBottom(level: Int): List<Pair<Int, Int>> = when (level) {
            3 -> listOf(4 to 10, 5 to 10, 6 to 10, 7 to 10, 8 to 10, 5 to 9, 6 to 9, 7 to 9, 6 to 8)
            2 -> listOf(5 to 10, 6 to 10, 7 to 10, 6 to 9)
            1 -> listOf(5 to 10, 6 to 10, 7 to 10)
            else -> emptyList()
        }
        val stream = listOf(6 to 5, 6 to 6, 6 to 7)
        return listOf(
            clip(frame + sandTop(3)),
            clip(frame + sandTop(3) + stream + sandBottom(1)),
            clip(frame + sandTop(2) + stream + sandBottom(2)),
            clip(frame + sandTop(1) + stream + sandBottom(3)),
            clip(frame + sandBottom(3)),
            // Gewendet: der Sand liegt wieder oben, das Warten beginnt von vorn.
            clip(frame + sandTop(3))
        )
    }

    /** Schriftrolle rollt auf, Zeile fuer Zeile erscheint der Text. */
    private fun scrollFrames(): List<List<Pair<Int, Int>>> {
        fun rolls(width: Int): List<Pair<Int, Int>> {
            val left = 6 - width
            val right = 6 + width
            return (3..9).flatMap { y -> listOf(left to y, right to y) } +
                listOf(left - 1 to 3, left - 1 to 9, right + 1 to 3, right + 1 to 9)
        }
        fun sheet(width: Int): List<Pair<Int, Int>> =
            ((6 - width + 1)..(6 + width - 1)).flatMap { x -> listOf(x to 3, x to 9) }
        val textLines = listOf(
            (4..8).map { it to 5 },
            (4..7).map { it to 6 },
            (4..8).map { it to 7 }
        )
        return listOf(
            clip(rolls(1)),
            clip(rolls(2) + sheet(2)),
            clip(rolls(4) + sheet(4)),
            clip(rolls(5) + sheet(5)),
            clip(rolls(5) + sheet(5) + textLines[0]),
            clip(rolls(5) + sheet(5) + textLines[0] + textLines[1]),
            clip(rolls(5) + sheet(5) + textLines.flatten())
        )
    }

    /**
     * Ein Puzzleteil schiebt sich heran und rastet ein - der Moment, in dem etwas zusammenpasst.
     *
     * Als Umrisse gezeichnet und nicht gefuellt: Zwei gefuellte Flaechen, die aneinanderstossen,
     * verschmelzen auf 13x13 zu einem Klotz, in dem die Naht - also das eigentliche Motiv - gar
     * nicht mehr zu sehen ist. Die Zapfenluecke in der rechten Kante ist der Schluessel: Erst
     * wenn der Zapfen des Teils genau dort sitzt, liest sich das Bild als "passt".
     */
    private fun puzzleFrames(): List<List<Pair<Int, Int>>> {
        // Rahmen mit einer Luecke in der rechten Kante bei y=6/7 - dort gehoert der Zapfen hin.
        val board = (2..8).flatMap { x -> listOf(x to 3, x to 10) } +
            (4..9).map { y -> 2 to y } +
            listOf(8 to 4, 8 to 5, 8 to 8, 8 to 9)
        fun piece(dx: Int): List<Pair<Int, Int>> {
            val outline = listOf(
                9 to 5, 10 to 5, 11 to 5,
                9 to 8, 10 to 8, 11 to 8,
                11 to 6, 11 to 7,
                9 to 6, 9 to 7
            )
            val tab = listOf(8 to 6, 8 to 7)
            return shift(outline + tab, dx = dx)
        }
        return listOf(
            clip(board + piece(4)),
            clip(board + piece(3)),
            clip(board + piece(2)),
            clip(board + piece(1)),
            clip(board + piece(0)),
            // Eingerastet: ein kurzer Funke rechts daneben, nicht auf der Figur selbst - sonst
            // waere der Frame mit dem vorherigen identisch und stuende still.
            clip(board + piece(0) + listOf(12 to 6, 12 to 7)),
            clip(board + piece(0)),
            clip(board + piece(0) + listOf(12 to 5, 12 to 8))
        )
    }
}
