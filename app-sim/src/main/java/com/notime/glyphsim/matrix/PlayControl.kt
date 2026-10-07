package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.matrix.PlayScene.Place
import com.notime.glyphsim.matrix.PlayScene.Station
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * **Die Steuerung von Itoeva 2** - der Spieler bewegt den Avatar selbst.
 *
 * Entschieden am 03.10.: Weg vom Erinnerungsspiel, hin zu einem aktiven Spiel, in dem man die
 * Figur steuert - links, rechts, und "hoch/runter" als **Tiefe im Bild** wie in HD-2D-Spielen:
 * Der Boden bekommt Tiefe, hoch heisst nach hinten (zur Bodenlinie), runter nach vorn (zum
 * Betrachter). Der Avatar bewegt sich dabei nicht mehr von selbst.
 *
 * Laeuft man an einen Rand weiter, kommt man zum Nachbarort in dieser Richtung - ueber dieselbe
 * Weltkarte, auf der auch die autonomen Wege liegen ([PlayMap]). Daheim liegen die Zimmer an
 * einem Flur ([HOUSE]): links und rechts geht es von Zimmer zu Zimmer, ganz rechts hinaus auf
 * die Strasse.
 *
 * Reine Rechnung ohne Android - die Darstellung (DockScreen) setzt nur um, was hier steht.
 */
object PlayControl {

    enum class Dir(val dx: Int, val dy: Int) { LEFT(-1, 0), RIGHT(1, 0), UP(0, -1), DOWN(0, 1) }

    /**
     * Wo die Figur steht: [x] waagerecht als Bruchteil der Bildbreite (0 = links, 1 = rechts),
     * [depth] in der Tiefe (0 = hinten an der Bodenlinie, 1 = ganz vorn). [pushMs] zaehlt, wie
     * lange schon gegen einen Rand gedrueckt wird - erst dann geht es hinaus.
     */
    data class Pos(val x: Float = 0.3f, val depth: Float = 0.5f, val pushMs: Long = 0L)

    /** Ergebnis eines Schritts: die neue Stelle, und ob die Figur dabei hinausgeht. */
    data class Step(val pos: Pos, val exit: Dir? = null)

    /** Bildbreiten pro Sekunde - quer durchs Bild in gut drei Sekunden. */
    const val SPEED_X = 0.30f

    /** Tiefe pro Sekunde - von hinten nach vorn in einer knappen Sekunde. */
    const val SPEED_DEPTH = 1.2f

    /** So lange muss man gegen einen Rand druecken, bevor es hinausgeht. */
    const val EXIT_PUSH_MS = 260L

    /** Radial statt achsenweise: diagonales Ziehen darf keine Extra-Geschwindigkeit geben. */
    data class Stick(val x: Float = 0f, val y: Float = 0f) {
        val strength: Float get() = sqrt(x * x + y * y).coerceAtMost(1f)
        fun direction(previous: Dir = Dir.DOWN): Dir? {
            if (strength < 0.01f) return null
            // Nahe der Diagonalen nicht bei jedem Fingerzittern Vorder-/Seitenansicht wechseln.
            if (previous.dx != 0 && abs(x) >= abs(y) * 0.85f) return if (x > 0) Dir.RIGHT else Dir.LEFT
            if (previous.dy != 0 && abs(y) >= abs(x) * 0.85f) return if (y > 0) Dir.DOWN else Dir.UP
            return swipeDir(x, y, 0f)
        }
    }

    fun stick(dx: Float, dy: Float, radius: Float, deadZone: Float = 0.14f): Stick {
        if (!dx.isFinite() || !dy.isFinite() || radius <= 0f || !radius.isFinite()) return Stick()
        val length = sqrt(dx * dx + dy * dy)
        val zone = deadZone.coerceIn(0f, 0.9f)
        val amount = ((length / radius - zone) / (1f - zone)).coerceIn(0f, 1f)
        return if (length <= 0f || amount == 0f) Stick() else Stick(dx / length * amount, dy / length * amount)
    }

    /** Bei halbem Ausschlag normales Gehen, am Rand 2,5-faches Tempo auf allen Vieren. */
    fun tempo(strength: Float): Float {
        val s = strength.coerceIn(0f, 1f)
        return if (s <= 0.55f) s / 0.55f else 1f + (s - 0.55f) / 0.45f * 1.5f
    }

    fun step(pos: Pos, stick: Stick, dtMs: Long, exitDelayMs: Long = EXIT_PUSH_MS,
        immediateExits: Set<Dir> = emptySet()): Step {
        val amount = stick.strength
        if (amount < 0.01f) return Step(pos.copy(pushMs = 0L))
        val dt = dtMs.coerceIn(0L, 100L) / 1000f
        val speed = tempo(amount)
        val nx = pos.x + stick.x / amount * SPEED_X * speed * dt
        val nd = pos.depth + stick.y / amount * SPEED_DEPTH * speed * dt
        val exit = when {
            nx < 0f -> Dir.LEFT
            nx > 1f -> Dir.RIGHT
            nd < 0f -> Dir.UP
            nd > 1f -> Dir.DOWN
            else -> null
        }
        val clamped = Pos(nx.coerceIn(0f, 1f), nd.coerceIn(0f, 1f))
        if (exit == null) return Step(clamped)
        val pushed = pos.pushMs + dtMs.coerceIn(0L, 100L)
        return if (exit in immediateExits || pushed >= exitDelayMs) Step(clamped, exit) else Step(clamped.copy(pushMs = pushed))
    }

    /** Ein Schritt in Richtung [dir] ueber [dtMs] Millisekunden. */
    fun step(pos: Pos, dir: Dir, dtMs: Long): Step {
        val dt = dtMs.coerceIn(0L, 100L) / 1000f
        val nx = pos.x + dir.dx * SPEED_X * dt
        val nd = pos.depth + dir.dy * SPEED_DEPTH * dt
        val atEdge = nx < 0f || nx > 1f || nd < 0f || nd > 1f
        val clamped = Pos(nx.coerceIn(0f, 1f), nd.coerceIn(0f, 1f))
        if (!atEdge) return Step(clamped)
        val pushed = pos.pushMs + dtMs
        return if (pushed >= EXIT_PUSH_MS) Step(clamped, dir) else Step(clamped.copy(pushMs = pushed))
    }

    /** Daheim: die Zimmer an einem Flur, von links nach rechts. Ganz rechts geht es hinaus. */
    val HOUSE: List<Place> = listOf(
        Place.BEDROOM, Place.BATH, Place.NOOK, Place.DESK, Place.CRAFT, Place.KITCHEN, Place.LIVING
    )

    /**
     * Wohin man kommt, wenn man in Richtung [dir] aus [place] hinausgeht - oder `null`, wenn dort
     * nichts liegt (dann bleibt die Figur am Rand stehen).
     *
     * Draussen entscheidet die Lage auf der Karte ([PlayMapScene.gridOf]): Unter den Nachbarn
     * wird der genommen, der am ehesten in diese Richtung liegt (hoechstens 60 Grad daneben).
     */
    fun neighbor(place: Place, dir: Dir): Place? {
        val room = HOUSE.indexOf(place)
        if (room >= 0) {
            return when (dir) {
                Dir.LEFT -> HOUSE.getOrNull(room - 1)
                Dir.RIGHT -> if (place == Place.LIVING) Place.STREET else HOUSE.getOrNull(room + 1)
                else -> null
            }
        }
        val (hx, hy) = PlayMapScene.gridOf(place)
        return PlayMap.neighbors(place)
            .map { if (it in HOUSE) Place.LIVING else it }
            .distinct()
            .mapNotNull { next ->
                val (nx, ny) = PlayMapScene.gridOf(next)
                val vx = (nx - hx).toFloat()
                val vy = (ny - hy).toFloat()
                val len = sqrt(vx * vx + vy * vy)
                if (len == 0f) return@mapNotNull null
                val cos = (vx * dir.dx + vy * dir.dy) / len
                if (cos < 0.5f) null else Triple(next, cos, len)
            }
            .sortedWith(compareByDescending<Triple<Place, Float, Float>> { it.second }.thenBy { it.third })
            .firstOrNull()?.first
    }

    /**
     * Wo die Figur im neuen Ort steht: Wer rechts hinausging, kommt links herein - und wer nach
     * hinten hinausging, kommt vorn herein. Die andere Achse bleibt, wie sie war.
     */
    fun entry(dir: Dir, from: Pos): Pos = when (dir) {
        Dir.LEFT -> Pos(1f, from.depth)
        Dir.RIGHT -> Pos(0f, from.depth)
        Dir.UP -> Pos(from.x, 1f)
        Dir.DOWN -> Pos(from.x, 0f)
    }

    // ---- Handeln vor Ort: die Aktionstaste ----

    /** So nah (Bruchteil der Bildbreite, Mitte zu Mitte) muss man an einem Platz stehen. */
    const val REACH = 0.12f

    /**
     * Der Platz, an dem man gerade steht - der naechste innerhalb von [REACH], oder `null`.
     * [stations] ordnet jedem Platz seine waagerechte Mitte als Bruchteil der Bildbreite zu;
     * [avatarCenter] ist die Mitte der Figur im selben Mass. Tueren zaehlen nicht: Durch die
     * geht man, indem man an den Rand laeuft.
     */
    fun stationInReach(avatarCenter: Float, stations: Map<Station, Float>): Station? =
        stations.filterKeys { it != Station.DOOR }
            .mapValues { abs(it.value - avatarCenter) }
            .filterValues { it <= REACH }
            .minByOrNull { it.value }?.key

    /**
     * **Was die Figur an diesem Platz tut**, wenn man die Aktionstaste drueckt - ein kurzer
     * Ablauf aus den vorhandenen Schritten, also mit denselben Bildern wie im autonomen Leben:
     * ins Bett legen und schlafen, sich auf die Bank setzen, am Schreibtisch arbeiten, ein Buch
     * aus dem Regal nehmen, Licht und Fernseher an und aus. [lampOn]/[tvOn] sind der jetzige
     * Zustand der Geraete. `null` fuer einen Platz, an dem es nichts zu tun gibt.
     */
    fun actionAt(station: Station, lampOn: Boolean = false, tvOn: Boolean = false): PlayRoutine? {
        fun sitAnd(topic: AnimationType, lingerMs: Long) = PlayRoutine(listOf(
            RoutineStep.GoTo(station), RoutineStep.Occupy(station),
            RoutineStep.Act(topic), RoutineStep.Linger(lingerMs), RoutineStep.Rise
        ))
        fun standAnd(topic: AnimationType) = PlayRoutine(listOf(
            RoutineStep.GoTo(station), RoutineStep.Act(topic), RoutineStep.Linger(1_500L)
        ))
        return when (station) {
            Station.BED -> sitAnd(AnimationType.SLEEP, 4_000L)
            Station.SEAT, Station.BENCH -> sitAnd(AnimationType.MINDFULNESS, 3_000L)
            Station.TUB -> sitAnd(AnimationType.REST, 3_000L)
            Station.DESK, Station.WORKPLACE -> standAnd(AnimationType.WORK)
            Station.TABLE, Station.FRIDGE -> standAnd(AnimationType.DRINK)
            Station.BOOKSHELF -> standAnd(AnimationType.BOOK)
            Station.CRAFT -> standAnd(AnimationType.CREATIVITY)
            Station.ARCADE -> standAnd(AnimationType.FOCUS)
            Station.BASIN, Station.RACK, Station.CHECKOUT -> standAnd(AnimationType.GENERAL)
            Station.LAMP -> PlayRoutine(listOf(RoutineStep.GoTo(station), RoutineStep.Switch(station, !lampOn)))
            Station.TV -> PlayRoutine(listOf(RoutineStep.GoTo(station), RoutineStep.Switch(station, !tvOn)))
            Station.DOOR -> null
        }
    }

    // ---- Wischen statt Steuerkreuz ----

    /**
     * Die Richtung, in die der Finger seit dem Aufsetzen gezogen wurde - oder `null`, solange
     * er noch nah am Aufsetzpunkt ist ([deadZone], gleiche Einheit wie [dx]/[dy]). Es zaehlt die
     * staerkere Achse: Ein Zug schraeg nach rechts oben ist "rechts", wenn er mehr rechts ist.
     * Bildschirm-y waechst nach unten, also ist ein Zug nach oben ([dy] < 0) [Dir.UP].
     */
    fun swipeDir(dx: Float, dy: Float, deadZone: Float): Dir? {
        if (dx * dx + dy * dy < deadZone * deadZone) return null
        return if (abs(dx) >= abs(dy)) {
            if (dx > 0) Dir.RIGHT else Dir.LEFT
        } else {
            if (dy > 0) Dir.DOWN else Dir.UP
        }
    }

    // ---- Tueren ----

    /**
     * Wohin eine Tuer fuehrt: aus jedem Zimmer in den Flur (das Wohnzimmer), aus dem Wohnzimmer
     * auf die Strasse, aus Laden, Cafe, Arbeit und Spielhalle zurueck nach draussen.
     */
    fun doorTarget(place: Place): Place? = when {
        place == Place.LIVING -> Place.STREET
        place in HOUSE -> Place.LIVING
        else -> PlayMap.neighbors(place).firstOrNull()
    }

    // ---- Sichtbare Wege ----

    /**
     * **Wohin man von hier gehen kann - im Bild.** Fuer jede Richtung, in der ein Nachbarort
     * liegt ([neighbor]), ein pulsierender Pfeil am Rand mit einer Spur aus Steinen dorthin:
     * links und rechts auf halber Bodentiefe, nach hinten an der Bodenlinie in der Bildmitte,
     * nach vorn am unteren Rand. Ueber einer Tuer ([doorTop], Zelle ueber ihrer Oberkante)
     * ein Pfeil nach oben. Ohne Schrift.
     *
     * [floorY] ist die Bodenlinie, [frontRow] die vorderste Zeile des begehbaren Bodens.
     */
    fun exitMarks(
        place: Place,
        widthCells: Int,
        floorY: Int,
        frontRow: Int,
        phase: Int,
        doorTop: Pair<Int, Int>? = null
    ): List<SceneCell> {
        if (widthCells < 12 || floorY < 6) return emptyList()
        val pulse = 0.65f + 0.35f * kotlin.math.sin(phase / 6.0).toFloat()
        val bright = (MARK_TONE * pulse).toInt()
        val stone = (MARK_TONE * 0.45f).toInt()
        val cells = mutableListOf<SceneCell>()
        val midRow = (floorY + frontRow) / 2
        val centerX = widthCells / 2
        fun chevron(x: Int, y: Int, dir: Dir) {
            // Ein Winkel aus drei Zellen, die Spitze zeigt in [dir].
            val points = when (dir) {
                Dir.LEFT -> listOf(1 to -1, 0 to 0, 1 to 1)
                Dir.RIGHT -> listOf(-1 to -1, 0 to 0, -1 to 1)
                Dir.UP -> listOf(-1 to 1, 0 to 0, 1 to 1)
                Dir.DOWN -> listOf(-1 to -1, 0 to 0, 1 to -1)
            }
            for ((px, py) in points) cells += SceneCell(x + px, y + py, bright, isLight = true)
        }
        if (neighbor(place, Dir.LEFT) != null) {
            chevron(1, midRow, Dir.LEFT)
            for (x in 4..9 step 2) cells += SceneCell(x, midRow, stone)
        }
        if (neighbor(place, Dir.RIGHT) != null) {
            chevron(widthCells - 2, midRow, Dir.RIGHT)
            for (x in widthCells - 10..widthCells - 5 step 2) cells += SceneCell(x, midRow, stone)
        }
        if (neighbor(place, Dir.UP) != null) {
            chevron(centerX, floorY - 2, Dir.UP)
            for (y in floorY + 1..frontRow step 2) cells += SceneCell(centerX, y, stone)
        }
        if (neighbor(place, Dir.DOWN) != null) {
            chevron(centerX, frontRow, Dir.DOWN)
            for (y in floorY + 1 until frontRow - 1 step 2) cells += SceneCell(centerX + 3, y, stone)
        }
        if (doorTop != null && doorTarget(place) != null) {
            chevron(doorTop.first, doorTop.second - 1, Dir.UP)
        }
        return cells.filter { it.x in 0 until widthCells && it.y >= 0 }
    }

    /** Helligkeit der Wegmarken (Pfeile) - deutlich, aber unter einer Lampe. */
    private const val MARK_TONE = 2_200
}
