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
}
