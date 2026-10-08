package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayScene.Place
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.abs

/** Zeit- und ortsfeste Kosmetik, ohne Zufall, Timer, Persistenz oder zweite Weltpipeline. */
object GameAtmosphere {
    data class Patch(val x: Float, val y: Float, val w: Float, val h: Float,
        val seed: Int, val hanging: Boolean = false, val water: Boolean = false)
    data class Particle(val x: Float, val y: Float, val alpha: Float, val radius: Float)
    /** Dieselbe langsame Boee erreicht Laub, Stoffe und Figuren; Ortsteil-IDs spielen keine Rolle. */
    fun wind(worldX: Float, clock: Long, weather: PlayWeather = PlayWeather.CLEAR): Float {
        val t = clock.toDouble() / 1000.0
        val weatherScale = if (weather == PlayWeather.RAIN) 1.25f else 1f
        return (sin(t * .57f - worldX / 2100f) * .68f +
            sin(t * 1.17f - worldX / 3900f) * .22f).toFloat() * weatherScale
    }
    fun bend(patch: Patch, row: Float, clock: Long, worldOrigin: Float = 0f): Float {
        val weight = if (patch.hanging) row else 1f - row
        val wave = wind(worldOrigin + patch.x, clock) + .14f * sin(clock / 930f + patch.seed)
        return wave * weight * weight * if (patch.water) 1.25f else 1.35f
    }
    fun figureBend(scene: GameScenes.Scene, species: AvatarSpecies, v: Float, clock: Long,
        worldX: Float, weather: PlayWeather): Float {
        if (!GameWorld.isWorld(scene) || scene.place == Place.GROTTO) return 0f
        val tips = ((.55f - v) / .55f).coerceIn(0f, 1f)
        val amplitude = when (species) {
            AvatarSpecies.FENNEC, AvatarSpecies.HOOTLET -> .009f
            AvatarSpecies.GLOOP -> .003f
            else -> .006f
        }
        return wind(worldX, clock, weather) * tips * tips * amplitude
    }
    /** Lichtstaub bleibt im lokalen Wirkungskreis, statt in jedem Raum gleich zu regnen. */
    fun dust(light: GameSceneLighting.Light, index: Int, clock: Long): Particle {
        val t = clock.toDouble() / 1000.0
        val a = index * 2.39996f
        val radius = light.radius * (.12f + (index % 5) * .045f)
        val x = light.x + cos(a + t * .035f).toFloat() * radius
        val y = light.y + 20f + sin(a + t * .045f).toFloat() * radius * .45f
        val exposure = GameSceneLighting.influence(x, y, light)
        return Particle(x, y, exposure * (.15f + .12f * abs(sin(t * .31f + index)).toFloat()), .55f)
    }
    fun mist(place: Place, minute: Int, clock: Long): Float {
        if (place !in setOf(Place.POND, Place.SWAMP, Place.MOUNTAIN_PASS, Place.GROTTO)) return 0f
        val day = GameSceneLighting.daylight(minute)
        return (.035f + (1f - day) * .035f) * (.92f + .08f * sin(clock / 8000f))
    }
    data class Sample(val asset: String, val offset: Float, val width: Float, val height: Float)
    /** Bewegung darf die nachgemalten Anschluesse nicht mit dem alten Grundbild ueberdecken. */
    fun sample(scene: GameScenes.Scene, patch: Patch): Sample? {
        if (!GameWorld.isWorld(scene)) return Sample(scene.asset, 0f, 480f, 270f)
        val origin = GameWorld.origin(scene.place)
        val left = origin + patch.x - 3f; val right = origin + patch.x + patch.w + 3f
        val seam = GameWorld.seams.firstOrNull { left < it.x + GameWorld.SEAM_HALF && right > it.x - GameWorld.SEAM_HALF }
        if (seam != null) {
            val opaque = GameWorld.SEAM_HALF - GameWorld.SEAM_FEATHER
            if (left < seam.x - opaque || right > seam.x + opaque) return null
            return Sample(seam.asset, origin - (seam.x - GameWorld.SEAM_HALF),
                GameWorld.SEAM_HALF * 2f, GameWorld.HEIGHT)
        }
        val region = GameWorld.region(scene.place)!!
        return Sample(scene.asset, origin - GameWorld.regionOrigin(region), region.width, GameWorld.HEIGHT)
    }
    /** Nur kleine gemalte Pflanz-/Stoffbereiche bewegen sich, Moebel und Boden bleiben fest. */
    fun patches(scene: GameScenes.Scene): List<Patch> {
        if (scene.asset.startsWith("interiors/")) return when (scene.place) {
            Place.LIVING -> listOf(Patch(106f, 44f, 14f, 76f, 1, hanging = true), Patch(351f, 155f, 21f, 29f, 2))
            Place.BEDROOM -> listOf(Patch(176f, 50f, 12f, 66f, 3, hanging = true))
            Place.BATH -> listOf(Patch(289f, 45f, 18f, 77f, 4, hanging = true))
            Place.DESK -> listOf(Patch(300f, 58f, 16f, 63f, 5, hanging = true))
            Place.KITCHEN -> listOf(Patch(325f, 90f, 15f, 33f, 6))
            Place.NOOK -> listOf(Patch(205f, 46f, 14f, 68f, 7, hanging = true), Patch(88f, 108f, 18f, 31f, 8))
            Place.CRAFT -> listOf(Patch(201f, 55f, 15f, 57f, 9, hanging = true))
            Place.SHOP -> listOf(Patch(300f, 57f, 17f, 63f, 10, hanging = true))
            Place.CAFE -> listOf(Patch(288f, 50f, 16f, 58f, 11, hanging = true))
            Place.WORK -> listOf(Patch(265f, 144f, 17f, 23f, 12))
            Place.ARCADE -> listOf(Patch(245f, 57f, 15f, 70f, 13, hanging = true))
            else -> emptyList()
        }
        if (!GameWorld.isWorld(scene)) return emptyList()
        val origin = GameWorld.origin(scene.place)
        val region = GameWorld.region(scene.place)!!
        val local = origin - GameWorld.regionOrigin(region)
        val absolute = when (region.asset) {
            "world/street-park-forest.png" -> listOf(Patch(960f, 187f, 128f, 153f, 20),
                Patch(1425f, 169f, 178f, 163f, 21), Patch(1787f, 448f, 73f, 68f, 22))
            "world/village-edge.png" -> listOf(Patch(1165f, 172f, 159f, 157f, 23))
            "world/coast.png" -> listOf(Patch(1404f, 115f, 108f, 182f, 24), Patch(1666f, 134f, 123f, 164f, 25))
            "world/uplands.png" -> listOf(Patch(772f, 435f, 60f, 67f, 26))
            else -> emptyList()
        }
        // Jeder Bildausschnitt wird genau einmal seinem Abschnitt zugeordnet.
        return absolute.filter { it.x >= local && it.x < local + region.section }
            .map { it.copy(x = it.x - local) }
    }
}
