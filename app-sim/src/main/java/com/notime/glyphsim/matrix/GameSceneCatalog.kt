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
            place = Place.BEACH,
            asset = "scenes/beach.png",
            farY = 215.1f, nearY = 246f,
            farLeft = 20f, farRight = 460f, nearLeft = 10f, nearRight = 470f,
            farHeight = 48f, nearHeight = 58f,
            spots = listOf(
                Spot(Station.BENCH, Box(328.3f, 163f, 380.4f, 210.6f), 354.4f, 215.1f)
            ),
            exits = mapOf(),
            cropTop = 0.7f
        ),
        Scene(
            place = Place.BEDROOM,
            asset = "scenes/bedroom.png",
            farY = 212f, nearY = 234f,
            farLeft = 20f, farRight = 372f, nearLeft = 14f, nearRight = 378f,
            farHeight = 64f, nearHeight = 70f,
            spots = listOf(
                Spot(Station.BED, Box(215f, 120f, 362f, 232f), 285f, 222f),
                Spot(Station.DOOR, Box(0f, 40f, 26f, 232f), 26f, 222f)
            ),
            exits = mapOf(Dir.UP to null, Dir.DOWN to null),
            door = PlayControl.doorTarget(Place.BEDROOM),
            cropTop = 0.5f
        ),
        Scene(
            place = Place.CAMP,
            asset = "scenes/camp.png",
            farY = 196f, nearY = 246f,
            farLeft = 20f, farRight = 460f, nearLeft = 10f, nearRight = 470f,
            farHeight = 48f, nearHeight = 58f,
            spots = listOf(
                Spot(Station.BENCH, Box(174.3f, 180.2f, 322.7f, 221.3f), 248.5f, 225.3f)
            ),
            exits = mapOf(),
            cropTop = 0.7f
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
            place = Place.FOREST,
            asset = "scenes/forest.png",
            farY = 226.7f, nearY = 246f,
            farLeft = 20f, farRight = 460f, nearLeft = 10f, nearRight = 470f,
            farHeight = 48f, nearHeight = 58f,
            spots = listOf(
                Spot(Station.BENCH, Box(260.1f, 164.7f, 479f, 223.6f), 369.6f, 227.6f)
            ),
            exits = mapOf(),
            cropTop = 0.7f
        ),
        Scene(
            place = Place.GROTTO,
            asset = "scenes/grotto.png",
            farY = 211.9f, nearY = 236.6f,
            farLeft = 20f, farRight = 460f, nearLeft = 10f, nearRight = 470f,
            farHeight = 48f, nearHeight = 58f,
            spots = listOf(
                Spot(Station.BENCH, Box(169.9f, 178.6f, 296.5f, 205.7f), 233.2f, 211.9f)
            ),
            exits = mapOf(),
            cropTop = 0.7f
        ),
        Scene(
            place = Place.JUNGLE,
            asset = "scenes/jungle.png",
            farY = 209.6f, nearY = 246f,
            farLeft = 20f, farRight = 460f, nearLeft = 10f, nearRight = 470f,
            farHeight = 48f, nearHeight = 58f,
            spots = listOf(
                Spot(Station.BENCH, Box(196.5f, 174.5f, 281.2f, 194.3f), 238.8f, 209.6f)
            ),
            exits = mapOf(),
            cropTop = 0.7f
        ),
        Scene(
            place = Place.LIVING,
            asset = "scenes/living.png",
            farY = 208f, nearY = 228f,
            farLeft = 132f, farRight = 444f, nearLeft = 126f, nearRight = 450f,
            farHeight = 64f, nearHeight = 70f,
            spots = listOf(
                Spot(Station.SEAT, Box(150f, 145f, 305f, 205f), 230f, 212f),
                Spot(Station.TV, Box(345f, 128f, 448f, 224f), 398f, 216f),
                Spot(Station.DOOR, Box(56f, 104f, 132f, 214f), 140f, 212f)
            ),
            exits = mapOf(Dir.UP to null, Dir.DOWN to null),
            door = PlayControl.doorTarget(Place.LIVING),
            cropTop = 0.5f
        ),
        Scene(
            place = Place.MEADOW,
            asset = "scenes/meadow.png",
            farY = 204.5f, nearY = 246f,
            farLeft = 20f, farRight = 460f, nearLeft = 10f, nearRight = 470f,
            farHeight = 48f, nearHeight = 58f,
            spots = listOf(
                Spot(Station.BENCH, Box(289.4f, 146f, 479f, 195.1f), 384.2f, 204.5f)
            ),
            exits = mapOf(),
            cropTop = 0.7f
        ),
        Scene(
            place = Place.MOUNTAINS,
            asset = "scenes/mountains.png",
            farY = 209.2f, nearY = 246f,
            farLeft = 20f, farRight = 460f, nearLeft = 10f, nearRight = 470f,
            farHeight = 48f, nearHeight = 58f,
            spots = listOf(
                Spot(Station.BENCH, Box(61.4f, 186.3f, 158.9f, 215f), 110.2f, 219f)
            ),
            exits = mapOf(),
            cropTop = 0.7f
        ),
        Scene(
            place = Place.PARK,
            asset = "scenes/park.png",
            farY = 186f, nearY = 216f,
            farLeft = 150f, farRight = 470f, nearLeft = 30f, nearRight = 470f,
            farHeight = 60f, nearHeight = 72f,
            spots = listOf(
                Spot(Station.BENCH, Box(62f, 134f, 212f, 191f), 150f, 192f)
            ),
            exits = mapOf(),
            cropTop = 0.6f
        ),
        Scene(
            place = Place.PLAINS,
            asset = "scenes/plains.png",
            farY = 207.7f, nearY = 246f,
            farLeft = 20f, farRight = 460f, nearLeft = 10f, nearRight = 470f,
            farHeight = 48f, nearHeight = 58f,
            spots = listOf(
                Spot(Station.BENCH, Box(159.6f, 157.4f, 239.5f, 206.6f), 199.6f, 210.6f)
            ),
            exits = mapOf(),
            cropTop = 0.7f
        ),
        Scene(
            place = Place.POND,
            asset = "scenes/pond.png",
            farY = 165f, nearY = 188f,
            farLeft = 10f, farRight = 318f, nearLeft = 4f, nearRight = 240f,
            farHeight = 56f, nearHeight = 64f,
            spots = listOf(),
            exits = mapOf(),
            cropTop = 0.6f
        ),
        Scene(
            place = Place.SPORT,
            asset = "scenes/sport.png",
            farY = 184f, nearY = 222f,
            farLeft = 40f, farRight = 440f, nearLeft = 20f, nearRight = 460f,
            farHeight = 60f, nearHeight = 72f,
            spots = listOf(),
            exits = mapOf(),
            cropTop = 0.6f
        ),
        Scene(
            place = Place.STREET,
            asset = "scenes/street.png",
            farY = 196f, nearY = 212f,
            farLeft = 4f, farRight = 470f, nearLeft = 0f, nearRight = 476f,
            farHeight = 60f, nearHeight = 66f,
            spots = listOf(
                Spot(Station.LAMP, Box(70f, 60f, 96f, 100f), 84f, 204f),
                Spot(Station.BENCH, Box(436f, 158f, 480f, 210f), 452f, 204f)
            ),
            exits = mapOf(),
            cropTop = 0.5f
        ),
        Scene(
            place = Place.SWAMP,
            asset = "scenes/swamp.png",
            farY = 218.1f, nearY = 246f,
            farLeft = 20f, farRight = 460f, nearLeft = 10f, nearRight = 470f,
            farHeight = 48f, nearHeight = 58f,
            spots = listOf(
                Spot(Station.BENCH, Box(202.4f, 190.3f, 370f, 218.1f), 286.2f, 222.1f)
            ),
            exits = mapOf(),
            cropTop = 0.7f
        ),
    )
}
