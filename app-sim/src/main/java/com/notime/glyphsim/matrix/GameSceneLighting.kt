package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayControl.Pos
import com.notime.glyphsim.matrix.PlayScene.Place
import com.notime.glyphsim.matrix.PlayScene.Station
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin

/** Licht, Figurenfarbe und Bodenschatten teilen dieselben Bild- und Weltkoordinaten. */
object GameSceneLighting {
    enum class Tone { SUN, WARM, COOL }
    enum class Kind { SKY, WINDOW, LAMP, FIRE, SCREEN, CRYSTAL }
    data class Light(val x: Float, val y: Float, val radius: Float, val power: Float, val tone: Tone,
        val groundY: Float = y + 90f, val height: Float = 90f, val kind: Kind = Kind.LAMP,
        val directionX: Float = 0f, val directionY: Float = .28f,
        val fadeStart: Float = 0f, val fadeWidth: Float = 0f) {
        val directional get() = kind == Kind.SKY
    }
    const val CAMP_FIRE_X = 130f
    const val CAMP_FIRE_Y = 198f
    data class Shadow(val footX: Float, val footY: Float, val tipX: Float, val tipY: Float,
        val width: Float, val opacity: Float = .22f, val softness: Float = 2f)
    data class Rgb(val r: Float, val g: Float, val b: Float) {
        operator fun plus(other: Rgb) = Rgb(r + other.r, g + other.g, b + other.b)
        operator fun times(value: Float) = Rgb(r * value, g * value, b * value)
        fun clamp() = Rgb(r.coerceIn(.18f, 1f), g.coerceIn(.18f, 1f), b.coerceIn(.18f, 1f))
    }
    data class CharacterLight(val left: Rgb, val right: Rgb,
        val lowerLeft: Rgb = left, val lowerRight: Rgb = right) {
        /** Physische Bildseite: die Lichtseite dreht sich nicht mit der Blickrichtung. */
        fun at(u: Float, v: Float = .5f): Rgb {
            val x = u.coerceIn(0f, 1f); val y = v.coerceIn(0f, 1f)
            return ((left * (1f - x) + right * x) * (1f - y) +
                (lowerLeft * (1f - x) + lowerRight * x) * y).clamp()
        }
    }
    const val MAX_DARK = .6f
    fun daylight(minuteOfDay: Int): Float = when (val minute = Math.floorMod(minuteOfDay, 1440)) {
        in 360..479 -> (minute - 360) / 120f
        in 480..1079 -> 1f
        in 1080..1199 -> (1200 - minute) / 120f
        else -> 0f
    }
    fun darkness(scene: GameScenes.Scene, minuteOfDay: Int): Float {
        val night = 1f - daylight(minuteOfDay)
        return if (PlayScene.isOutdoors(scene.place) && scene.place != Place.GROTTO) MAX_DARK * night
            else .1f + .32f * night
    }
    /** Dieselbe stueckweise lineare Abdunkelung wie das durchgehende Panorama. */
    fun darknessAt(scene: GameScenes.Scene, x: Float, minute: Int): Float {
        if (!GameWorld.isWorld(scene)) return darkness(scene, minute)
        val i = GameWorld.places.indexOf(scene.place)
        val here = darkness(scene, minute)
        val before = darkness(GameWorld.scene(GameWorld.places.getOrElse(i - 1) { scene.place })!!, minute)
        val after = darkness(GameWorld.scene(GameWorld.places.getOrElse(i + 1) { scene.place })!!, minute)
        val t = (x / GameWorld.region(scene.place)!!.section).coerceIn(0f, 1f)
        return if (t < .5f) (here + before) * .5f + (here - before) * t
            else here + (after - here) * (t - .5f)
    }
    fun dusk(scene: GameScenes.Scene, minuteOfDay: Int): Float {
        if (!PlayScene.isOutdoors(scene.place) || scene.place == Place.GROTTO) return 0f
        val day = daylight(minuteOfDay)
        return if (day in 0.01f..0.99f) 1f - abs(day - .5f) * 2f else 0f
    }
    private fun sky(minute: Int): Light? {
        val day = daylight(minute)
        if (day <= 0f) return null
        val progress = ((Math.floorMod(minute, 1440) - 360) / 840f).coerceIn(0f, 1f)
        return Light(0f, 0f, Float.MAX_VALUE, .44f * day, Tone.SUN,
            height = 180f + 240f * sin(progress * Math.PI).toFloat(), kind = Kind.SKY,
            directionX = 1f - 2f * progress, directionY = .25f)
    }
    /** Einmalige absolute Quellen, auch wenn mehrere Orte gleichzeitig im Kamerabild liegen. */
    fun worldSources(minute: Int, lampOn: Boolean, phase: Int): List<Light> = buildList {
        sky(minute)?.let { add(it.copy(fadeStart = GameWorld.origin(Place.GROTTO) - 100f, fadeWidth = 580f)) }
        if (lampOn) add(Light(GameWorld.origin(Place.STREET) + 442f, 286f, 330f, .68f,
            Tone.WARM, groundY = 502f, height = 216f))
        add(Light(GameWorld.origin(Place.CAMP) + 280f, 445f, 330f,
            .68f + .045f * sin(phase * .63f), Tone.WARM, groundY = 492f, height = 47f, kind = Kind.FIRE))
        add(Light(GameWorld.origin(Place.GROTTO) + 590f, 420f, 380f, .52f,
            Tone.COOL, groundY = 510f, height = 90f, kind = Kind.CRYSTAL))
    }
    fun sources(scene: GameScenes.Scene, minuteOfDay: Int, lampOn: Boolean, tvOn: Boolean,
        phase: Int): List<Light> {
        if (GameWorld.isWorld(scene)) {
            val origin = GameWorld.origin(scene.place)
            return worldSources(minuteOfDay, lampOn, phase).map { it.copy(x = it.x - origin, fadeStart = it.fadeStart - origin) }
        }
        val day = daylight(minuteOfDay)
        val room = GameLightingCatalog.rooms[scene.place].takeIf { scene.asset.startsWith("interiors/") }
        return buildList {
            if (room != null) {
                room.window?.takeIf { day > 0f }?.let { w ->
                    add(Light(w.x, w.y, 245f, .60f * day, Tone.SUN,
                        groundY = w.bottom + 36f, height = w.bottom + 36f - w.y, kind = Kind.WINDOW))
                }
                for (lamp in room.lamps) if (!lamp.switched || lampOn)
                    add(Light(lamp.x, lamp.y, lamp.radius, .46f, Tone.WARM,
                        groundY = lamp.floorY, height = lamp.floorY - lamp.y))
            } else {
                if (PlayScene.isOutdoors(scene.place) && scene.place != Place.GROTTO) sky(minuteOfDay)?.let { add(it) }
                if (lampOn) scene.spots.filter { it.station == Station.LAMP }.forEach { spot ->
                    add(Light((spot.hit.x0 + spot.hit.x1) / 2f,
                        spot.hit.y0 + (spot.hit.y1 - spot.hit.y0) * .18f, 115f, .52f, Tone.WARM))
                }
                if (scene.place == Place.CAMP) add(Light(CAMP_FIRE_X, CAMP_FIRE_Y, 95f,
                    .48f + .04f * sin(phase * .63f), Tone.WARM, kind = Kind.FIRE))
            }
            if (tvOn) scene.spots.filter { it.station == Station.TV }.forEach { spot ->
                add(Light((spot.hit.x0 + spot.hit.x1) / 2f, spot.hit.y0 + 18f, 100f,
                    .34f, Tone.COOL, kind = Kind.SCREEN))
            }
        }
    }
    fun influence(x: Float, y: Float, light: Light): Float {
        if (light.directional) return light.power * if (light.fadeWidth > 0f)
            (1f - (x - light.fadeStart) / light.fadeWidth).coerceIn(0f, 1f) else 1f
        val t = (1f - hypot(x - light.x, y - light.y) / light.radius).coerceIn(0f, 1f)
        return light.power * t * t
    }
    fun illuminationAt(x: Float, y: Float, lights: List<Light>): Float =
        lights.sumOf { influence(x, y, it).toDouble() }.toFloat().coerceIn(0f, .8f)

    /** Ein niedriger Gegenstand blockiert nur den Strahlanteil unter seiner Oberkante. */
    fun transmission(scene: GameScenes.Scene, light: Light, x: Float, floorY: Float, z: Float): Float {
        if (light.directional) return 1f
        var result = 1f
        for (piece in GameFurniture.pieces(scene)) {
            var enter = 0f; var leave = 1f
            fun slab(start: Float, end: Float, low: Float, high: Float): Boolean {
                val delta = end - start
                if (abs(delta) < .001f) return start in low..high
                val a = (low - start) / delta; val b = (high - start) / delta
                enter = maxOf(enter, minOf(a, b)); leave = minOf(leave, maxOf(a, b))
                return enter <= leave
            }
            if (!slab(light.x, x, piece.left, piece.right) ||
                !slab(light.groundY, floorY, piece.back, piece.front)) continue
            val t = ((enter + leave) * .5f).coerceIn(0f, 1f)
            val rayHeight = light.height * (1f - t) + z * t
            if (t in 0.02f..0.98f && rayHeight < piece.ground - piece.top) result *= .22f
        }
        return result
    }

    fun character(scene: GameScenes.Scene, pos: Pos, species: AvatarSpecies, minute: Int,
        lights: List<Light>, lift: Float = 0f): CharacterLight {
        val (x, floor) = GameScenes.feet(scene, pos)
        val height = GameCharacterScale.visibleHeight(scene, pos, species)
        val dark = darknessAt(scene, x, minute)
        val base = 1f - dark * .82f
        val ambient = Rgb(base * (1f - dark * .17f), base * (1f - dark * .06f), base)
        fun side(normal: Float, fraction: Float): Rgb {
            val z = lift + height * fraction
            val y = floor - z
            var result = ambient
            for (light in lights) {
                val dx = if (light.directional) -light.directionX else light.x - x
                val distance = if (light.directional) 1f else hypot(dx, light.y - y).coerceAtLeast(1f)
                val direction = (dx / distance).coerceIn(-1f, 1f)
                val weight = influence(x, y, light) * transmission(scene, light, x, floor, z) * (.30f + .70f * ((1f + direction * normal) / 2f))
                val tint = when (light.tone) {
                    Tone.SUN -> Rgb(1f, .88f, .64f)
                    Tone.WARM -> Rgb(1f, .65f, .30f)
                    Tone.COOL -> Rgb(.25f, .72f, 1f)
                }
                result += tint * weight * .65f
            }
            return result.clamp()
        }
        return CharacterLight(side(-1f, .85f), side(1f, .85f), side(-1f, .18f), side(1f, .18f))
    }

    /** Kontakt bleibt beim Sprung am Boden; die Projektion wird breiter und schwächer. */
    fun shadows(scene: GameScenes.Scene, pos: Pos, lights: List<Light>,
        bodyHeight: Float = GameScenes.avatarHeight(scene, pos), lift: Float = 0f,
        receiverHeight: Float = 0f): List<Shadow> {
        val (x, floor) = GameScenes.feet(scene, pos)
        val y = floor - receiverHeight
        val airborne = (lift - receiverHeight).coerceAtLeast(0f)
        val spread = 1f + airborne / bodyHeight.coerceAtLeast(1f) * .45f
        val contact = Shadow(x, y, x, y, bodyHeight * .13f * spread,
            .22f / (1f + airborne / 24f), 1.5f + airborne * .045f)
        val candidates = lights.map { it to influence(x, y - bodyHeight * .4f, it) *
            transmission(scene, it, x, floor, receiverHeight + bodyHeight * .4f) }
            .filter { it.second > .035f }.sortedByDescending { it.second }.take(3)
        return listOf(contact) + candidates.map { (light, strength) ->
            val dx = if (light.directional) light.directionX else x - light.x
            val dy = if (light.directional) light.directionY else (floor - light.groundY) * .28f
            val distance = hypot(dx, dy).coerceAtLeast(1f)
            val sourceHeight = (light.height - receiverHeight).coerceAtLeast(18f)
            val length = if (light.directional) bodyHeight * (abs(dx) * .70f + .15f)
                else (bodyHeight * distance / sourceHeight).coerceAtMost(bodyHeight * 1.4f)
            Shadow(x, y, x + dx / distance * length, y + dy / distance * length,
                bodyHeight * .12f * spread, (strength * .45f).coerceAtMost(.28f) / (1f + airborne / 80f),
                2f + length * .045f + airborne * .04f)
        }
    }
    /** Kompatibler Einzel-Schatten fuer die alte Vorschau. */
    fun shadow(scene: GameScenes.Scene, pos: Pos, lights: List<Light>): Shadow =
        shadows(scene, pos, lights).getOrElse(1) {
            val (x, y) = GameScenes.feet(scene, pos)
            Shadow(x, y, x + 4f, y + 2f, GameScenes.avatarHeight(scene, pos) * .16f)
        }
}
