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
 * der Konzeptbilder und Weltstudien (`docs/concept-art/world-studies/`) - feine Pixel, warme
 * Farben, echte Tiefe. Die Bilder entstehen mit `tools/world-art` (480 x 270 Pixel,
 * gemalt in Perspektive) und liegen nur in der Spiel-Variante (`src/game/assets/scenes`).
 *
 * Hier steht, was das Spiel ueber ein Bild wissen muss:
 * - **wo man gehen kann**: ein Trapez auf dem Boden, hinten schmaler als vorn;
 * - **wie gross die Figur wo ist**: hinten klein, vorn gross - das ist die Tiefe;
 * - **was man antippen kann**: Plaetze mit Trefferflaeche und Standort davor;
 * - **wohin die Raender fuehren**, wo sie von [PlayControl.neighbor] abweichen (bisher nur:
 *   gesperrte Raender - Waende, Gelaender).
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
        val cropTop: Float = 0.8f,
        /** Nur gemalte Wege mit Hoehenprofil: x normalisiert, hintere/vordere Bodenkante. */
        val walkBand: List<WalkBand> = emptyList()
    )

    data class WalkBand(val x: Float, val far: Float, val near: Float)

    fun floorBand(scene: Scene, x: Float): Pair<Float, Float> {
        val points = scene.walkBand
        if (points.isEmpty()) return scene.farY to scene.nearY
        val t = x.coerceIn(0f, 1f)
        val b = points.indexOfFirst { it.x >= t }.coerceAtLeast(0)
        if (b == 0) return points.first().far to points.first().near
        val a = points[b - 1]; val next = points[b]
        val fraction = (t - a.x) / (next.x - a.x)
        return lerp(a.far, next.far, fraction) to lerp(a.near, next.near, fraction)
    }

    /**
     * Die Orte selbst stehen im generierten [GameSceneCatalog]: Jedes Szenen-Skript in
     * `tools/world-art/places/` liefert mit dem Bild auch Gehflaeche, Plaetze und gesperrte Raender,
     * `build_all.py` schreibt daraus den Katalog. Ein Platz [Station.DOOR] fuehrt dorthin, wohin
     * die Tuer des Ortes auch sonst fuehrt ([PlayControl.doorTarget]).
     */
    private val ALL: Map<Place, Scene> by lazy { GameSceneCatalog.ALL.associateBy { it.place } }

    /** Das gemalte Bild zu [place], falls es schon eines gibt. */
    fun of(place: Place): Scene? = ALL[place]

    val painted: Set<Place> get() = ALL.keys

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    /** Wo die Fuesse der Figur im Bild stehen. */
    fun feet(scene: Scene, pos: Pos): Pair<Float, Float> {
        val d = pos.depth.coerceIn(0f, 1f)
        val left = lerp(scene.farLeft, scene.nearLeft, d)
        val right = lerp(scene.farRight, scene.nearRight, d)
        val band = floorBand(scene, pos.x)
        return lerp(left, right, pos.x.coerceIn(0f, 1f)) to lerp(band.first, band.second, d)
    }

    /** Wie hoch die Figur an dieser Stelle ist (Bildpixel) - hinten klein, vorn gross. */
    fun avatarHeight(scene: Scene, pos: Pos): Float =
        lerp(scene.farHeight, scene.nearHeight, pos.depth.coerceIn(0f, 1f))

    /** Die Stelle zu einem Fusspunkt im Bild - die Umkehrung von [feet]. */
    fun posAt(scene: Scene, x: Float, y: Float): Pos {
        val band = floorBand(scene, (x - scene.farLeft) / (scene.farRight - scene.farLeft))
        val d = ((y - band.first) / (band.second - band.first)).coerceIn(0f, 1f)
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

    fun fit(scene: Scene, screenW: Float, screenH: Float, focusX: Float = 0.5f, focusDepth: Float = 0.5f): Fit {
        val scale = max(screenW / IMAGE_W, screenH / IMAGE_H)
        val overW = IMAGE_W * scale - screenW
        val overH = IMAGE_H * scale - screenH
        // Nur den tatsaechlich ueberstehenden Teil verschieben: Bild, Trefferflaechen und
        // Fusspunkt benutzen denselben Fit. Bei 16:9 bleibt die Kamera deshalb exakt ruhig.
        val xCrop = (0.35f + 0.3f * focusX.coerceIn(0f, 1f)).coerceIn(0f, 1f)
        val yCrop = (scene.cropTop + 0.25f * (focusDepth.coerceIn(0f, 1f) - 0.5f)).coerceIn(0f, 1f)
        return Fit(scale, -overW * xCrop, -overH * yCrop)
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
