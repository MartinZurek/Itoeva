package com.notime.glyphsim.stream

import com.notime.glyphsim.matrix.PlayMap
import com.notime.glyphsim.matrix.PlayScene.Place

/** Nur belegte Kartenfakten und Darstellungen: kein Zugriff auf Routinen oder Spielstand. */
internal object FennecWorld {
    const val VERSION = 1
    data class Presentation(val target: Place?)

    fun presentation(action: String, target: String): Presentation? = when {
        action == "show_world" && target.isEmpty() -> Presentation(null)
        action == "show_place" -> Place.entries.firstOrNull { it.name == target }?.let { Presentation(it) }
        else -> null
    }

    fun route(from: Place, presentation: Presentation): List<Place> =
        presentation.target?.let { PlayMap.route(from, it) }.orEmpty()

    fun name(place: Place, german: Boolean): String {
        val names = when (place) {
            Place.BEDROOM -> "Bedroom" to "Schlafzimmer"
            Place.BATH -> "Bathroom" to "Bad"
            Place.DESK -> "Desk" to "Schreibtisch"
            Place.WORK -> "Work" to "Arbeit"
            Place.KITCHEN -> "Kitchen" to "Küche"
            Place.NOOK -> "Quiet corner" to "Ruheecke"
            Place.LIVING -> "Home" to "Zuhause"
            Place.CRAFT -> "Workshop" to "Werkstatt"
            Place.PARK -> "Park" to "Park"
            Place.SPORT -> "Sports ground" to "Sportplatz"
            Place.POND -> "Pond" to "Teich"
            Place.SHOP -> "Shop" to "Laden"
            Place.STREET -> "Street" to "Straße"
            Place.FOREST -> "Forest" to "Wald"
            Place.MEADOW -> "Meadow" to "Wiese"
            Place.CITY -> "Town" to "Stadt"
            Place.ARCADE -> "Arcade" to "Spielhalle"
            Place.BEACH -> "Beach" to "Strand"
            Place.CAFE -> "Café" to "Café"
            Place.JUNGLE -> "Jungle" to "Dschungel"
            Place.MOUNTAINS -> "Mountains" to "Berge"
            Place.SWAMP -> "Marsh" to "Sumpf"
            Place.PLAINS -> "Plains" to "Ebene"
            Place.GROTTO -> "Grotto" to "Grotte"
            Place.CAMP -> "Camp" to "Lager"
            Place.COAST_PATH -> "Coastal path" to "Küstenweg"
            Place.VILLAGE_EDGE -> "Village edge" to "Dorfrand"
            Place.MOUNTAIN_PASS -> "Mountain pass" to "Bergpass"
        }
        return if (german) names.second else names.first
    }
}
