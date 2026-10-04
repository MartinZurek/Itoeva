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
            place = Place.ARCADE,
            asset = "scenes/arcade.png",
            farY = 172f, nearY = 236f,
            farLeft = 150f, farRight = 322f, nearLeft = 40f, nearRight = 430f,
            farHeight = 42f, nearHeight = 78f,
            spots = listOf(
                Spot(Station.ARCADE, Box(135f, 69f, 280f, 161f), 190.182f, 172f),
                Spot(Station.DOOR, Box(283f, 67f, 314f, 153f), 313.545f, 172f)
            ),
            exits = mapOf(),
            door = PlayControl.doorTarget(Place.ARCADE),
            cropTop = 0.7f
        ),
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
            place = Place.BEACH,
            asset = "scenes/beach.png",
            farY = 206f, nearY = 250f,
            farLeft = 30f, farRight = 430f, nearLeft = 20f, nearRight = 440f,
            farHeight = 44f, nearHeight = 60f,
            spots = listOf(
                Spot(Station.BENCH, Box(295f, 225f, 347f, 240f), 320f, 244f)
            ),
            exits = mapOf(),
            cropTop = 0.8f
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
            place = Place.CAFE,
            asset = "scenes/cafe.png",
            farY = 172f, nearY = 236f,
            farLeft = 146f, farRight = 326f, nearLeft = 40f, nearRight = 430f,
            farHeight = 42f, nearHeight = 78f,
            spots = listOf(
                Spot(Station.SEAT, Box(251.7f, 101.3f, 308.696f, 191.253f), 249.25f, 189.125f),
                Spot(Station.DOOR, Box(281f, 67f, 314f, 153f), 312.5f, 172f)
            ),
            exits = mapOf(),
            door = PlayControl.doorTarget(Place.CAFE),
            cropTop = 0.7f
        ),
        Scene(
            place = Place.CAMP,
            asset = "scenes/camp.png",
            farY = 196f, nearY = 248f,
            farLeft = 50f, farRight = 430f, nearLeft = 20f, nearRight = 450f,
            farHeight = 42f, nearHeight = 60f,
            spots = listOf(
                Spot(Station.BENCH, Box(305f, 201f, 357f, 216f), 330f, 222f)
            ),
            exits = mapOf(),
            cropTop = 0.8f
        ),
        Scene(
            place = Place.CITY,
            asset = "scenes/city.png",
            farY = 206f, nearY = 248f,
            farLeft = 40f, farRight = 450f, nearLeft = 20f, nearRight = 460f,
            farHeight = 46f, nearHeight = 60f,
            spots = listOf(
                Spot(Station.LAMP, Box(337f, 143f, 352f, 210f), 350f, 214f),
                Spot(Station.BENCH, Box(376f, 194f, 420f, 216f), 398f, 222f)
            ),
            exits = mapOf(),
            cropTop = 0.8f
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
            place = Place.FOREST,
            asset = "scenes/forest.png",
            farY = 196f, nearY = 248f,
            farLeft = 60f, farRight = 420f, nearLeft = 30f, nearRight = 440f,
            farHeight = 42f, nearHeight = 60f,
            spots = listOf(
                Spot(Station.BENCH, Box(343f, 215f, 399f, 230f), 370f, 234f)
            ),
            exits = mapOf(),
            cropTop = 0.8f
        ),
        Scene(
            place = Place.GROTTO,
            asset = "scenes/grotto.png",
            farY = 214f, nearY = 250f,
            farLeft = 60f, farRight = 420f, nearLeft = 40f, nearRight = 440f,
            farHeight = 44f, nearHeight = 58f,
            spots = listOf(
                Spot(Station.BENCH, Box(278f, 211f, 321f, 228f), 300f, 232f)
            ),
            exits = mapOf(),
            cropTop = 0.8f
        ),
        Scene(
            place = Place.JUNGLE,
            asset = "scenes/jungle.png",
            farY = 214f, nearY = 250f,
            farLeft = 70f, farRight = 410f, nearLeft = 40f, nearRight = 430f,
            farHeight = 44f, nearHeight = 60f,
            spots = listOf(
                Spot(Station.BENCH, Box(305f, 219f, 357f, 234f), 330f, 238f)
            ),
            exits = mapOf(),
            cropTop = 0.8f
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
            place = Place.MEADOW,
            asset = "scenes/meadow.png",
            farY = 196f, nearY = 248f,
            farLeft = 50f, farRight = 430f, nearLeft = 20f, nearRight = 440f,
            farHeight = 42f, nearHeight = 60f,
            spots = listOf(
                Spot(Station.BENCH, Box(305f, 201f, 357f, 216f), 330f, 222f)
            ),
            exits = mapOf(),
            cropTop = 0.8f
        ),
        Scene(
            place = Place.MOUNTAINS,
            asset = "scenes/mountains.png",
            farY = 196f, nearY = 248f,
            farLeft = 60f, farRight = 420f, nearLeft = 30f, nearRight = 440f,
            farHeight = 40f, nearHeight = 60f,
            spots = listOf(
                Spot(Station.BENCH, Box(306f, 206f, 354f, 224f), 330f, 228f)
            ),
            exits = mapOf(),
            cropTop = 0.8f
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
            place = Place.PLAINS,
            asset = "scenes/plains.png",
            farY = 192f, nearY = 248f,
            farLeft = 50f, farRight = 430f, nearLeft = 20f, nearRight = 450f,
            farHeight = 40f, nearHeight = 60f,
            spots = listOf(
                Spot(Station.BENCH, Box(125f, 211f, 177f, 226f), 150f, 232f)
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
            place = Place.SHOP,
            asset = "scenes/shop.png",
            farY = 172f, nearY = 236f,
            farLeft = 150f, farRight = 322f, nearLeft = 40f, nearRight = 430f,
            farHeight = 42f, nearHeight = 78f,
            spots = listOf(
                Spot(Station.RACK, Box(182f, 95f, 245f, 175f), 212.833f, 179.222f),
                Spot(Station.CHECKOUT, Box(288f, 98f, 372f, 197f), 283.75f, 189.125f),
                Spot(Station.DOOR, Box(150f, 67f, 181f, 153f), 154f, 172f)
            ),
            exits = mapOf(),
            door = PlayControl.doorTarget(Place.SHOP),
            cropTop = 0.7f
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
        Scene(
            place = Place.SWAMP,
            asset = "scenes/swamp.png",
            farY = 210f, nearY = 250f,
            farLeft = 60f, farRight = 420f, nearLeft = 30f, nearRight = 440f,
            farHeight = 44f, nearHeight = 58f,
            spots = listOf(
                Spot(Station.BENCH, Box(345f, 209f, 397f, 224f), 370f, 228f)
            ),
            exits = mapOf(),
            cropTop = 0.8f
        ),
        Scene(
            place = Place.WORK,
            asset = "scenes/work.png",
            farY = 172f, nearY = 236f,
            farLeft = 150f, farRight = 322f, nearLeft = 40f, nearRight = 430f,
            farHeight = 42f, nearHeight = 78f,
            spots = listOf(
                Spot(Station.TABLE, Box(157.453f, 116.358f, 266.43f, 183.907f), 202.323f, 192f),
                Spot(Station.WORKPLACE, Box(290f, 95f, 372f, 185f), 292.375f, 189.125f),
                Spot(Station.DOOR, Box(150f, 67f, 181f, 153f), 154f, 172f)
            ),
            exits = mapOf(),
            door = PlayControl.doorTarget(Place.WORK),
            cropTop = 0.7f
        ),
    )
}
