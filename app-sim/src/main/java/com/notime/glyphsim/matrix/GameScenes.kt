package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayControl.Dir
import com.notime.glyphsim.matrix.PlayControl.Pos
import com.notime.glyphsim.matrix.PlayScene.Place
import com.notime.glyphsim.matrix.PlayScene.Station
import kotlin.math.abs
import kotlin.math.max

/**
 * **Die gemalte Welt von Itoeva 2.**
 *
 * Wunsch vom 04.10.: weg von der groben, flachen LED-Kulisse, hin zu einer Pixel-Welt in der Art
 * der Konzeptbilder (Kuestenpark im Abendrot, Lesezimmer zur blauen Stunde) - feine Pixel,
 * kraeftige Farben, echte Tiefe. Die Bilder entstehen mit `tools/world-art` (480 x 270 Pixel,
 * gemalt in Perspektive) und liegen nur in der Spiel-Variante (`src/game/assets/scenes`).
 *
 * Hier steht, was das Spiel ueber ein Bild wissen muss:
 * - **wo man gehen kann**: ein Trapez auf dem Boden, hinten schmaler als vorn;
 * - **wie gross die Figur wo ist**: hinten klein, vorn gross - das ist die Tiefe;
 * - **was man antippen kann**: Plaetze mit Trefferflaeche und Standort davor;
 * - **wohin die Raender fuehren**, wo sie von [PlayControl.neighbor] abweichen.
 *
 * Alle Masse in Bildpixeln. Reine Rechnung ohne Android.
 */
object GameScenes {

    const val IMAGE_W = 480
    const val IMAGE_H = 270

    /** Ein Rechteck in Bildpixeln. */
    data class Box(val x0: Float, val y0: Float, val x1: Float, val y1: Float) {
        operator fun contains(p: Pair<Float, Float>): Boolean = p.first in x0..x1 && p.second in y0..y1
    }

    /** Ein Platz im Bild: was er ist, wo man ihn trifft, wo die Figur davor steht. */
    data class Spot(val station: Station, val hit: Box, val standX: Float, val standY: Float)

    data class Scene(
        val place: Place,
        /** Pfad unter `assets/`. */
        val asset: String,
        /** Fusslinie ganz hinten und ganz vorn. */
        val farY: Float,
        val nearY: Float,
        /** Wie weit man hinten und vorn nach links und rechts kommt. */
        val farLeft: Float,
        val farRight: Float,
        val nearLeft: Float,
        val nearRight: Float,
        /** Hoehe der Figur hinten und vorn. */
        val farHeight: Float,
        val nearHeight: Float,
        val spots: List<Spot>,
        /** Ausgaenge, die anders fuehren als auf der Karte; `null` als Wert sperrt den Rand. */
        val exits: Map<Dir, Place?> = emptyMap(),
        /** Wohin der Platz [Station.DOOR] fuehrt. */
        val door: Place? = null,
        /**
         * Welcher Anteil des Ueberstands oben abgeschnitten wird, wenn das Bild hoeher ist als der
         * Bildschirm (0 = nur unten, 1 = nur oben). Der Boden ist wichtiger als der Himmel.
         */
        val cropTop: Float = 0.8f
    )

    /** Der Park am Meer im Abendrot - Plattenplatz vor dem Gelaender, Kiosk, Bank, Haus. */
    val PARK = Scene(
        place = Place.PARK,
        asset = "scenes/park.png",
        farY = 212f, nearY = 250f,
        farLeft = 104f, farRight = 398f, nearLeft = 22f, nearRight = 458f,
        farHeight = 44f, nearHeight = 62f,
        spots = listOf(
            Spot(Station.BENCH, Box(264f, 196f, 336f, 226f), 300f, 226f),
            Spot(Station.CHECKOUT, Box(402f, 150f, 480f, 218f), 410f, 222f),
            Spot(Station.DOOR, Box(0f, 40f, 126f, 206f), 104f, 213f),
            Spot(Station.RACK, Box(0f, 234f, 112f, 270f), 60f, 248f)
        ),
        exits = mapOf(Dir.LEFT to Place.LIVING, Dir.UP to null),
        door = Place.LIVING
    )

    /** Das Lesezimmer (das Wohnzimmer daheim) - Regal, Sessel, Beistelltisch, Fenster zum Park. */
    val LIVING = Scene(
        place = Place.LIVING,
        asset = "scenes/living.png",
        farY = 172f, nearY = 236f,
        farLeft = 140f, farRight = 342f, nearLeft = 26f, nearRight = 440f,
        farHeight = 42f, nearHeight = 78f,
        spots = listOf(
            Spot(Station.TABLE, Box(248f, 110f, 290f, 210f), 262f, 214f),
            Spot(Station.SEAT, Box(286f, 95f, 404f, 206f), 330f, 210f),
            Spot(Station.BOOKSHELF, Box(118f, 25f, 170f, 178f), 166f, 182f)
        ),
        exits = mapOf(Dir.RIGHT to Place.PARK, Dir.UP to null, Dir.DOWN to null),
        cropTop = 0.7f
    )

    private val ALL = listOf(PARK, LIVING).associateBy { it.place }

    /** Das gemalte Bild zu [place], falls es schon eines gibt. */
    fun of(place: Place): Scene? = ALL[place]

    val painted: Set<Place> get() = ALL.keys

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    /** Wo die Fuesse der Figur im Bild stehen. */
    fun feet(scene: Scene, pos: Pos): Pair<Float, Float> {
        val d = pos.depth.coerceIn(0f, 1f)
        val left = lerp(scene.farLeft, scene.nearLeft, d)
        val right = lerp(scene.farRight, scene.nearRight, d)
        return lerp(left, right, pos.x.coerceIn(0f, 1f)) to lerp(scene.farY, scene.nearY, d)
    }

    /** Wie hoch die Figur an dieser Stelle ist (Bildpixel) - hinten klein, vorn gross. */
    fun avatarHeight(scene: Scene, pos: Pos): Float =
        lerp(scene.farHeight, scene.nearHeight, pos.depth.coerceIn(0f, 1f))

    /** Die Stelle zu einem Fusspunkt im Bild - die Umkehrung von [feet]. */
    fun posAt(scene: Scene, x: Float, y: Float): Pos {
        val d = ((y - scene.farY) / (scene.nearY - scene.farY)).coerceIn(0f, 1f)
        val left = lerp(scene.farLeft, scene.nearLeft, d)
        val right = lerp(scene.farRight, scene.nearRight, d)
        return Pos(((x - left) / (right - left)).coerceIn(0f, 1f), d)
    }

    /** Wohin der Rand in Richtung [dir] fuehrt; `null` heisst: hier geht es nicht weiter. */
    fun exit(scene: Scene, dir: Dir): Place? =
        if (scene.exits.containsKey(dir)) scene.exits[dir] else PlayControl.neighbor(scene.place, dir)

    /** Der Platz unter einem Tipp (Bildpixel), oder `null`. Der kleinste Treffer gewinnt. */
    fun spotAt(scene: Scene, x: Float, y: Float): Spot? =
        scene.spots.filter { (x to y) in it.hit }
            .minByOrNull { (it.hit.x1 - it.hit.x0) * (it.hit.y1 - it.hit.y0) }

    /** Der Platz, vor dem die Figur gerade steht (fuer die Aktionstaste), oder `null`. */
    fun spotInReach(scene: Scene, pos: Pos): Spot? =
        scene.spots.map { it to posAt(scene, it.standX, it.standY) }
            .filter { (_, at) -> abs(at.x - pos.x) < REACH_X && abs(at.depth - pos.depth) < REACH_DEPTH }
            .minByOrNull { (_, at) -> abs(at.x - pos.x) + abs(at.depth - pos.depth) * 0.3f }
            ?.first

    const val REACH_X = 0.12f
    const val REACH_DEPTH = 0.45f

    /**
     * Wie das Bild auf den Bildschirm kommt: so gross, dass es ihn ganz fuellt, ohne Verzerrung.
     * [scale] Bildschirmpixel je Bildpixel, [left]/[top] die Lage der Bildecke (negativ, wo
     * abgeschnitten wird).
     */
    data class Fit(val scale: Float, val left: Float, val top: Float) {
        fun toScreen(x: Float, y: Float): Pair<Float, Float> = (left + x * scale) to (top + y * scale)
        fun toImage(x: Float, y: Float): Pair<Float, Float> = ((x - left) / scale) to ((y - top) / scale)
    }

    fun fit(scene: Scene, screenW: Float, screenH: Float): Fit {
        val scale = max(screenW / IMAGE_W, screenH / IMAGE_H)
        val overW = IMAGE_W * scale - screenW
        val overH = IMAGE_H * scale - screenH
        return Fit(scale, -overW / 2f, -overH * scene.cropTop)
    }

    /**
     * Dieselbe Stelle nach einem Ortswechsel durch den Rand [dir]: Wer rechts hinausgeht, kommt
     * links herein, in derselben Tiefe.
     */
    fun entry(dir: Dir, from: Pos): Pos = PlayControl.entry(dir, from)

    /** Schrittweite, mit der die Figur zu einem Platz geht (je Bildtakt, in Stellen-Einheiten). */
    fun approach(from: Pos, to: Pos, dtMs: Long): Pos {
        val dt = dtMs.coerceIn(0L, 100L) / 1000f
        fun move(a: Float, b: Float, speed: Float): Float {
            val d = b - a
            val stepLen = speed * dt
            return if (abs(d) <= stepLen) b else a + stepLen * if (d > 0) 1f else -1f
        }
        return Pos(move(from.x, to.x, PlayControl.SPEED_X), move(from.depth, to.depth, PlayControl.SPEED_DEPTH * 0.5f))
    }
}
