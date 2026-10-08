package com.notime.glyphsim.matrix

import kotlin.math.sin

/** Kleine Stoffteile haben feste Befestigungen und einen verzoegerten freien Saum. */
object GameFabric {
    data class Pose(val species: AvatarSpecies, val wind: Float, val clock: Long, val walking: Float = 0f)
    data class Region(val left: Float, val top: Float, val right: Float, val bottom: Float)
    // Nur tatsaechlich vorhandene Schals/Feder-/Blattspitzen; Kopf und Fusskontakt bleiben unverformt.
    private val regionCache = AvatarSpecies.entries.associateWith { species -> when (species) {
        AvatarSpecies.FENNEC -> listOf(Region(.14f,.48f,.53f,.69f))
        AvatarSpecies.STARLET -> listOf(Region(.31f,.51f,.65f,.68f))
        AvatarSpecies.HOOTLET -> listOf(Region(.17f,.53f,.36f,.70f))
        AvatarSpecies.GLOOP -> listOf(Region(.34f,.22f,.59f,.36f))
        AvatarSpecies.PUFFLING -> listOf(Region(.20f,.53f,.37f,.68f))
        AvatarSpecies.WYRMLING -> listOf(Region(.16f,.39f,.35f,.62f))
    } }
    fun regions(species: AvatarSpecies): List<Region> = regionCache.getValue(species)
    fun offset(pose: Pose, u: Float, v: Float): Float {
        val region = regions(pose.species).firstOrNull { u in it.left..it.right && v in it.top..it.bottom } ?: return 0f
        val free = ((region.right - u) / (region.right - region.left)).coerceIn(0f,1f)
        val edge = sin((v - region.top) / (region.bottom - region.top) * Math.PI).toFloat().coerceAtLeast(0f)
        val t = pose.clock.toDouble() / 1000.0
        val lag = sin(t * 1.17 - free * 1.8) * .16
        val flutter = sin(t * 3.7 - free * 2.5) * .12 * kotlin.math.abs(pose.wind)
        return ((pose.wind * .74 + lag * pose.wind + flutter +
            sin(t * 6.2 - free * 1.2) * pose.walking * .20) * free * free * edge * .025).toFloat()
    }
    fun hangingOffset(row: Float, column: Float, wind: Float, clock: Long, seed: Int): Float {
        val free = row.coerceIn(0f,1f)
        val t = clock.toDouble() / 1000.0
        return (free * free * (wind * 2.8 + sin(t * .93 - free * 1.7 + seed) * .55 * wind) *
            (.84 + .16 * sin(column * Math.PI))).toFloat()
    }
}
