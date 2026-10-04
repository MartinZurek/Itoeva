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
            place = Place.BATH,
            asset = "scenes/bath.png",
            farY = 172f, nearY = 236f,
            farLeft = 146f, farRight = 320f, nearLeft = 40f, nearRight = 420f,
            farHeight = 42f, nearHeight = 78f,
            spots = listOf(
                Spot(Station.TUB, Box(270f, 115f, 369f, 175f), 272.889f, 179.222f),
                Spot(Station.BASIN, Box(202.951f, 44.7724f, 247.959f, 158.967f), 224.682f, 172f),
                Spot(Station.DOOR, Box(152f, 67f, 185f, 153f), 153.591f, 172f)
            ),
            exits = mapOf(Dir.UP to null, Dir.DOWN to null),
            door = PlayControl.doorTarget(Place.BATH),
            cropTop = 0.7f
        ),
        Scene(
            place = Place.BEDROOM,
            asset = "scenes/bedroom.png",
            farY = 172f, nearY = 236f,
            farLeft = 146f, farRight = 326f, nearLeft = 40f, nearRight = 430f,
            farHeight = 42f, nearHeight = 78f,
            spots = listOf(
                Spot(Station.BED, Box(251.597f, 89.4167f, 350.396f, 175.271f), 317.185f, 188.025f),
                Spot(Station.DOOR, Box(152f, 67f, 185f, 153f), 153.591f, 172f)
            ),
            exits = mapOf(Dir.UP to null, Dir.DOWN to null),
            door = PlayControl.doorTarget(Place.BEDROOM),
            cropTop = 0.7f
        ),
        Scene(
            place = Place.CRAFT,
            asset = "scenes/craft.png",
            farY = 172f, nearY = 236f,
            farLeft = 146f, farRight = 326f, nearLeft = 40f, nearRight = 430f,
            farHeight = 42f, nearHeight = 78f,
            spots = listOf(
                Spot(Station.CRAFT, Box(141f, 49f, 231.271f, 163f), 183.909f, 172f),
                Spot(Station.DOOR, Box(281f, 67f, 314f, 153f), 312.5f, 172f)
            ),
            exits = mapOf(Dir.UP to null, Dir.DOWN to null),
            door = PlayControl.doorTarget(Place.CRAFT),
            cropTop = 0.7f
        ),
        Scene(
            place = Place.DESK,
            asset = "scenes/desk.png",
            farY = 172f, nearY = 236f,
            farLeft = 146f, farRight = 326f, nearLeft = 40f, nearRight = 430f,
            farHeight = 42f, nearHeight = 78f,
            spots = listOf(
                Spot(Station.DESK, Box(187.091f, 76f, 276.909f, 165.818f), 232f, 177.081f),
                Spot(Station.DOOR, Box(281f, 67f, 314f, 153f), 312.5f, 172f)
            ),
            exits = mapOf(Dir.UP to null, Dir.DOWN to null),
            door = PlayControl.doorTarget(Place.DESK),
            cropTop = 0.7f
        ),
        Scene(
            place = Place.KITCHEN,
            asset = "scenes/kitchen.png",
            farY = 172f, nearY = 236f,
            farLeft = 146f, farRight = 318f, nearLeft = 40f, nearRight = 420f,
            farHeight = 42f, nearHeight = 78f,
            spots = listOf(
                Spot(Station.FRIDGE, Box(298f, 70f, 354f, 174f), 296.649f, 177.081f),
                Spot(Station.TABLE, Box(161.723f, 119.909f, 249.627f, 186.904f), 201.333f, 195.067f),
                Spot(Station.DOOR, Box(152f, 67f, 185f, 153f), 153.591f, 172f)
            ),
            exits = mapOf(Dir.UP to null, Dir.DOWN to null),
            door = PlayControl.doorTarget(Place.KITCHEN),
            cropTop = 0.7f
        ),
        Scene(
            place = Place.LIVING,
            asset = "scenes/living.png",
            farY = 172f, nearY = 236f,
            farLeft = 150f, farRight = 322f, nearLeft = 40f, nearRight = 430f,
            farHeight = 42f, nearHeight = 78f,
            spots = listOf(
                Spot(Station.SEAT, Box(132.14f, 99f, 205.537f, 163.544f), 160.7f, 172f),
                Spot(Station.TV, Box(310f, 76f, 381f, 190f), 317.185f, 188.025f),
                Spot(Station.DOOR, Box(278f, 66f, 312f, 153f), 306.227f, 172f)
            ),
            exits = mapOf(Dir.UP to null, Dir.DOWN to null),
            door = PlayControl.doorTarget(Place.LIVING),
            cropTop = 0.7f
        ),
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
