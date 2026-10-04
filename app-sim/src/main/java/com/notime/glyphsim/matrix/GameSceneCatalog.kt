// Generiert von tools/world-art/build_all.py aus den Szenen-Skripten - nicht von Hand aendern.
package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.GameScenes.Box
import com.notime.glyphsim.matrix.GameScenes.Scene
import com.notime.glyphsim.matrix.GameScenes.Spot
import com.notime.glyphsim.matrix.PlayControl.Dir
import com.notime.glyphsim.matrix.PlayScene.Place
import com.notime.glyphsim.matrix.PlayScene.Station

/** Die gemalten Orte (siehe [GameScenes]) - Gehflaeche, Plaetze und Raender je Bild. */
internal object GameSceneCatalog {
    val ALL: List<Scene> = listOf(
        Scene(
            place = Place.NOOK,
            asset = "scenes/nook.png",
            farY = 172f, nearY = 236f,
            farLeft = 140f, farRight = 342f, nearLeft = 26f, nearRight = 440f,
            farHeight = 42f, nearHeight = 78f,
            spots = listOf(
                Spot(Station.SEAT, Box(286f, 95f, 404f, 206f), 330f, 210f),
                Spot(Station.BOOKSHELF, Box(118f, 25f, 170f, 178f), 166f, 182f),
                Spot(Station.LAMP, Box(300f, 40f, 360f, 100f), 300f, 200f)
            ),
            exits = mapOf(Dir.UP to null, Dir.DOWN to null),
            cropTop = 0.7f
        ),
        Scene(
            place = Place.PARK,
            asset = "scenes/park.png",
            farY = 200f, nearY = 240f,
            farLeft = 96f, farRight = 470f, nearLeft = 40f, nearRight = 440f,
            farHeight = 40f, nearHeight = 58f,
            spots = listOf(
                Spot(Station.BENCH, Box(97f, 185f, 139f, 206f), 118f, 214f)
            ),
            exits = mapOf(),
            cropTop = 0.8f
        ),
        Scene(
            place = Place.POND,
            asset = "scenes/pond.png",
            farY = 222f, nearY = 250f,
            farLeft = 100f, farRight = 420f, nearLeft = 40f, nearRight = 430f,
            farHeight = 46f, nearHeight = 58f,
            spots = listOf(),
            exits = mapOf(),
            cropTop = 0.8f
        ),
        Scene(
            place = Place.SPORT,
            asset = "scenes/sport.png",
            farY = 176f, nearY = 244f,
            farLeft = 150f, farRight = 410f, nearLeft = 40f, nearRight = 440f,
            farHeight = 34f, nearHeight = 60f,
            spots = listOf(),
            exits = mapOf(),
            cropTop = 0.8f
        ),
        Scene(
            place = Place.STREET,
            asset = "scenes/street.png",
            farY = 200f, nearY = 246f,
            farLeft = 90f, farRight = 440f, nearLeft = 30f, nearRight = 440f,
            farHeight = 46f, nearHeight = 62f,
            spots = listOf(
                Spot(Station.LAMP, Box(294f, 132f, 308f, 196f), 304f, 206f),
                Spot(Station.BENCH, Box(358f, 180f, 402f, 202f), 380f, 210f)
            ),
            exits = mapOf(),
            cropTop = 0.8f
        ),
    )
}
