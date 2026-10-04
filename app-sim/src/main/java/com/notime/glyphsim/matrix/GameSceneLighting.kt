package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayControl.Pos
import com.notime.glyphsim.matrix.PlayScene.Place
import com.notime.glyphsim.matrix.PlayScene.Station
import kotlin.math.hypot

/** Licht und Bodenschatten der gemalten Spielwelt, in den 480x270-Koordinaten der Szene. */
object GameSceneLighting {
    enum class Tone { SUN, WARM, COOL }
    data class Light(val x: Float, val y: Float, val radius: Float, val power: Float, val tone: Tone)
    data class Shadow(val footX: Float, val footY: Float, val tipX: Float, val tipY: Float, val width: Float)

    /** Zwischen sechs und acht Uhr hellt der Himmel auf, zwischen achtzehn und zwanzig ab. */
    fun daylight(minuteOfDay: Int): Float {
        val minute = Math.floorMod(minuteOfDay, 1440)
        return when (minute) {
            in 360..479 -> (minute - 360) / 120f
            in 480..1079 -> 1f
            in 1080..1199 -> (1200 - minute) / 120f
            else -> 0f
        }
    }

    fun darkness(scene: GameScenes.Scene, minuteOfDay: Int): Float {
        val outside = PlayScene.isOutdoors(scene.place)
        return if (outside) 0.32f * (1f - daylight(minuteOfDay)) else 0.12f + 0.17f * (1f - daylight(minuteOfDay))
    }

    /** Lichtpunkte stehen am gezeichneten Leuchtkoerper, nicht am Standort des Spielers. */
    fun sources(scene: GameScenes.Scene, minuteOfDay: Int, lampOn: Boolean, tvOn: Boolean, phase: Int): List<Light> {
        val result = mutableListOf<Light>()
        val day = daylight(minuteOfDay)
        if (PlayScene.isOutdoors(scene.place) && day > 0f) {
            val progress = ((Math.floorMod(minuteOfDay, 1440) - 360) / 840f).coerceIn(0f, 1f)
            result += Light(60f + progress * 360f, 22f, 325f, day * 0.11f, Tone.SUN)
        }
        if (lampOn) {
            scene.spots.filter { it.station == Station.LAMP }.forEach { spot ->
                val x = (spot.hit.x0 + spot.hit.x1) / 2f
                val y = spot.hit.y0 + (spot.hit.y1 - spot.hit.y0) * 0.18f
                val flicker = 1f + 0.035f * kotlin.math.sin(phase * 0.37).toFloat()
                result += Light(x, y, 88f, 0.42f * flicker, Tone.WARM)
            }
        }
        if (scene.place == Place.CAMP) result += Light(256f, 224f, 95f,
            0.48f + 0.04f * kotlin.math.sin(phase * 0.63).toFloat(), Tone.WARM)
        if (tvOn) scene.spots.filter { it.station == Station.TV }.forEach { spot ->
            result += Light((spot.hit.x0 + spot.hit.x1) / 2f, spot.hit.y0 + 18f, 80f, 0.28f, Tone.COOL)
        }
        return result
    }

    /** Inverses quadratisches Abfallen im begrenzten Wirkungskreis; kein Zufallstreffer. */
    fun illuminationAt(x: Float, y: Float, lights: List<Light>): Float = lights.sumOf { light ->
        val d = hypot((x - light.x).toDouble(), (y - light.y).toDouble()).toFloat()
        val t = (1f - d / light.radius).coerceIn(0f, 1f)
        (light.power * t * t).toDouble()
    }.toFloat().coerceIn(0f, 0.65f)

    /** Der naechste wirksame Punkt bestimmt die Richtung; ohne Punkt gibt es nur Kontaktschatten. */
    fun shadow(scene: GameScenes.Scene, pos: Pos, lights: List<Light>): Shadow {
        val (x, y) = GameScenes.feet(scene, pos)
        val width = GameScenes.avatarHeight(scene, pos) * 0.16f
        val source = lights.filter { it.power > 0.12f }.maxByOrNull { light ->
            val dx = x - light.x
            val dy = y - light.y
            light.power / (1f + (dx * dx + dy * dy) / (light.radius * light.radius))
        } ?: return Shadow(x, y, x + 4f, y + 2f, width)
        val dx = x - source.x
        val dy = y - source.y
        val distance = hypot(dx.toDouble(), dy.toDouble()).toFloat().coerceAtLeast(1f)
        val length = (GameScenes.avatarHeight(scene, pos) * 0.32f * source.power).coerceIn(5f, 25f)
        // Schatten liegen auf dem Boden: vertikale Projektion nur schwach, sonst schwebt er.
        return Shadow(x, y, x + dx / distance * length, y + dy / distance * length * 0.28f, width)
    }
}
