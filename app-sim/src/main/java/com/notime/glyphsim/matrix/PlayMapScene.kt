package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayMap.Region
import com.notime.glyphsim.matrix.PlayScene.Place
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * **Die Weltkarte im Bild.**
 *
 * Gewuenscht am 29.09.: "Es muss eine Art Mappe geben, wo er sich gerade befindet" - und am
 * 01.10. freigegeben, sie auch zu zeigen. Die Wege selbst gibt es seit [PlayMap]; bisher sah man
 * sie nur daran, dass das Wesen Ort fuer Ort hindurchging.
 *
 * Bricht es zu einem weiten Weg auf (siehe [PlayMap.showsMap]), blendet die Kulisse fuer einige
 * Sekunden zur Karte ueber: alle Orte als kleine Zeichen, die Wege dazwischen gepunktet. Von
 * dort, wo es steht, zieht sich der Weg Ort fuer Ort durchgehend bis zum Ziel, das dann blinkt -
 * als saehe es vor dem Losgehen kurz nach, wohin es geht.
 *
 * **Ohne ein Wort** (Vorgabe fuer den Stream: nur Bild und Ton). Die Orte erkennt man an ihren
 * Zeichen - Haus, Stadthaus, Baum, Wasser, Berg, Zelt -, die Gegenden an ihrer Lage: daheim
 * links, die Wildnis rechts.
 */
object PlayMapScene {

    /** Ein Ort auf der Karte: Lage im Raster (Spalte 0..[GRID_W], Zeile 0..[GRID_H]). */
    private data class Spot(val gx: Int, val gy: Int)

    private const val GRID_W = 13
    private const val GRID_H = 6

    /**
     * Die Lage der Orte. Von links nach rechts nach der Entfernung von daheim (siehe
     * [PlayMap.Region]), so dass die Wege ([PlayMap.neighbors]) moeglichst ohne Kreuzung laufen.
     * Alle Zimmer sind ein Haus - die Karte zeigt die Welt, nicht den Grundriss.
     */
    private fun spotOf(place: Place): Spot = when (place) {
        Place.BEDROOM, Place.BATH, Place.DESK, Place.KITCHEN, Place.NOOK, Place.LIVING, Place.CRAFT ->
            Spot(0, 4)
        Place.STREET -> Spot(2, 4)
        Place.SHOP -> Spot(2, 6)
        Place.CITY -> Spot(3, 2)
        Place.WORK -> Spot(1, 0)
        Place.CAFE -> Spot(3, 0)
        Place.ARCADE -> Spot(5, 0)
        Place.PARK -> Spot(5, 4)
        Place.SPORT -> Spot(5, 6)
        Place.POND -> Spot(7, 6)
        Place.MEADOW -> Spot(7, 3)
        Place.FOREST -> Spot(8, 1)
        Place.PLAINS -> Spot(9, 5)
        Place.CAMP -> Spot(10, 3)
        Place.MOUNTAINS -> Spot(11, 1)
        Place.GROTTO -> Spot(13, 0)
        Place.SWAMP -> Spot(12, 3)
        Place.BEACH -> Spot(11, 6)
        Place.JUNGLE -> Spot(13, 5)
    }

    /** Das Zeichen eines Ortes, 3 x 3 Zellen. */
    private fun glyphOf(place: Place): List<String> = when (place) {
        Place.STREET -> listOf("...", ".#.", "...")
        Place.SHOP, Place.CITY, Place.WORK, Place.CAFE, Place.ARCADE -> listOf("###", "#.#", "###")
        Place.PARK, Place.MEADOW, Place.SPORT -> listOf(".#.", "###", ".#.")
        Place.FOREST, Place.JUNGLE -> listOf("#.#", "###", ".#.")
        Place.POND, Place.BEACH, Place.SWAMP -> listOf("...", "#.#", ".#.")
        Place.PLAINS -> listOf("...", "...", "###")
        Place.MOUNTAINS, Place.GROTTO -> listOf("...", ".#.", "###")
        Place.CAMP -> listOf(".#.", "#.#", "#.#")
        else -> listOf(".#.", "###", "#.#")   // daheim: ein Haus
    }

    /** Die Orte, die als eigenes Zeichen erscheinen - jedes Zimmer zaehlt als das eine Haus. */
    private val SHOWN: List<Place> = Place.entries.filter {
        PlayMap.regionOf(it) != Region.HOME || it == Place.LIVING
    }

    /** Als welches Zeichen ein Ort erscheint - jedes Zimmer als das Haus. */
    private fun shownAs(place: Place): Place =
        if (PlayMap.regionOf(place) == Region.HOME) Place.LIVING else place

    /** Zellmitte eines Ortes in einem Bild aus [widthCells] x [heightCells]. */
    private fun centerOf(place: Place, widthCells: Int, top: Int, mapHeight: Int): Pair<Int, Int> {
        val spot = spotOf(place)
        val margin = 2
        val x = margin + (spot.gx * (widthCells - 1 - 2 * margin).toFloat() / GRID_W).roundToInt()
        val y = top + margin + (spot.gy * (mapHeight - 1 - 2 * margin).toFloat() / GRID_H).roundToInt()
        return x to y
    }

    /** Die Zellen einer geraden Linie von [a] nach [b], beide Enden eingeschlossen. */
    private fun line(a: Pair<Int, Int>, b: Pair<Int, Int>): List<Pair<Int, Int>> {
        val steps = max(abs(b.first - a.first), abs(b.second - a.second))
        if (steps == 0) return listOf(a)
        return (0..steps).map { i ->
            val t = i.toFloat() / steps
            (a.first + ((b.first - a.first) * t).roundToInt()) to (a.second + ((b.second - a.second) * t).roundToInt())
        }
    }

    /**
     * **Das Bild der Karte.**
     *
     * [from] ist der Ort, an dem das Wesen steht, [route] der Weg (siehe [PlayMap.route]).
     * [drawn] (0..1) sagt, wie weit der Weg schon nachgezogen ist; [blinkOn] schaltet das
     * blinkende Ziel und die eigene Stelle. Die Karte nutzt die Zeilen bis [floorY] - hoeher wird
     * sie auch auf grossen Bildschirmen nicht als 0,6 der Breite, sonst zoege sie sich in die
     * Laenge und die Wege wuerden zu Strichen ohne Zusammenhang.
     */
    fun build(
        from: Place,
        route: List<Place>,
        widthCells: Int,
        floorY: Int,
        drawn: Float,
        blinkOn: Boolean
    ): List<SceneCell> {
        val mapHeight = minOf(floorY, (widthCells * 0.6f).roundToInt()).coerceAtLeast(GRID_H + 6)
        val top = ((floorY - mapHeight) / 2).coerceAtLeast(0)
        val center = { p: Place -> centerOf(p, widthCells, top, mapHeight) }
        val cells = HashMap<Pair<Int, Int>, Int>()
        val lights = HashSet<Pair<Int, Int>>()
        fun put(pos: Pair<Int, Int>, brightness: Int, light: Boolean = false) {
            if (pos.first !in 0 until widthCells || pos.second < 0) return
            if ((cells[pos] ?: 0) < brightness) cells[pos] = brightness
            if (light) lights += pos
        }

        // Die Wege: gepunktet, gedaempft. Jeder Weg einmal.
        val wege = mutableSetOf<Pair<Place, Place>>()
        for (a in SHOWN) for (b in PlayMap.neighbors(a)) {
            val bb = shownAs(b)
            if (bb != a) wege += if (a.ordinal < bb.ordinal) a to bb else bb to a
        }
        for ((a, b) in wege) {
            line(center(a), center(b)).forEachIndexed { i, pos ->
                if (i % 2 == 0) put(pos, PlayScene.STRUCTURE)
            }
        }
        // Die Orte.
        for (place in SHOWN) {
            val (cx, cy) = center(place)
            glyphOf(place).forEachIndexed { row, text ->
                text.forEachIndexed { col, c ->
                    if (c == '#') put((cx - 1 + col) to (cy - 1 + row), PlayScene.FURNITURE)
                }
            }
        }

        // Der Weg, der gegangen wird: Ort fuer Ort durchgezogen, so weit [drawn] reicht.
        val stops = listOf(from) + route
        val path = stops.zipWithNext().flatMap { (a, b) -> line(center(a), center(b)) }.distinct()
        val shownPath = (path.size * drawn.coerceIn(0f, 1f)).roundToInt()
        path.take(shownPath).forEach { put(it, PlayScene.GLOW - 600) }
        // Die Orte unterwegs leuchten auf, sobald der Weg sie erreicht.
        val reached = path.take(shownPath).toSet()
        for (stop in route.dropLast(1)) {
            val (cx, cy) = center(stop)
            if ((cx to cy) in reached) {
                glyphOf(stop).forEachIndexed { row, text ->
                    text.forEachIndexed { col, c ->
                        if (c == '#') put((cx - 1 + col) to (cy - 1 + row), PlayScene.GLOW - 300)
                    }
                }
            }
        }
        // Hier steht es: ein heller Punkt, der pulsiert, solange der Weg noch waechst.
        val (hx, hy) = center(from)
        if (blinkOn || drawn >= 1f) {
            for (dx in -1..1) for (dy in -1..1) put((hx + dx) to (hy + dy), PlayScene.GLOW - 300)
            put(hx to hy, PlayScene.GLOW, light = true)
        }
        // Das Ziel: blinkt, sobald der Weg angekommen ist, mit einem Rahmen darum.
        val ziel = route.lastOrNull()
        if (ziel != null && drawn >= 1f && blinkOn) {
            val (zx, zy) = center(ziel)
            for (dx in -2..2) for (dy in -2..2) {
                if (abs(dx) == 2 || abs(dy) == 2) put((zx + dx) to (zy + dy), PlayScene.GLOW, light = true)
            }
            glyphOf(ziel).forEachIndexed { row, text ->
                text.forEachIndexed { col, c ->
                    if (c == '#') put((zx - 1 + col) to (zy - 1 + row), PlayScene.GLOW, light = true)
                }
            }
        }
        return cells.map { (pos, brightness) -> SceneCell(pos.first, pos.second, brightness, pos in lights) }
    }
}
