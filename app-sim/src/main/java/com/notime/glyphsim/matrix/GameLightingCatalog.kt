package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayScene.Place

/** Bildanker statt pauschaler Raumlichter: Koordinaten sind auf 480 x 270 normiert. */
internal object GameLightingCatalog {
    data class Window(val left: Float, val top: Float, val right: Float, val bottom: Float,
        val floorX: Float, val floorY: Float) {
        val x get() = (left + right) / 2f
        val y get() = (top + bottom) / 2f
    }
    data class Lamp(val x: Float, val y: Float, val floorY: Float, val radius: Float = 135f,
        val switched: Boolean = false)
    data class Room(val window: Window?, val lamps: List<Lamp>)
    val rooms = mapOf(
        Place.LIVING to Room(Window(53f, 39f, 114f, 113f, 199f, 247f),
            listOf(Lamp(160f, 109f, 167f, switched = true), Lamp(443f, 101f, 166f))),
        Place.BEDROOM to Room(Window(121f, 47f, 180f, 99f, 186f, 240f),
            listOf(Lamp(72f, 112f, 174f), Lamp(366f, 62f, 179f))),
        Place.BATH to Room(Window(326f, 43f, 386f, 113f, 213f, 246f),
            listOf(Lamp(156f, 70f, 194f), Lamp(428f, 69f, 199f))),
        Place.DESK to Room(Window(324f, 50f, 386f, 123f, 272f, 248f),
            listOf(Lamp(233f, 93f, 200f))),
        Place.KITCHEN to Room(Window(272f, 45f, 338f, 112f, 212f, 246f),
            listOf(Lamp(142f, 55f, 210f, 170f))),
        Place.NOOK to Room(Window(136f, 42f, 211f, 110f, 165f, 248f),
            listOf(Lamp(311f, 120f, 210f))),
        Place.CRAFT to Room(Window(145f, 46f, 202f, 116f, 213f, 248f),
            listOf(Lamp(360f, 55f, 208f, 170f))),
        Place.SHOP to Room(Window(314f, 45f, 373f, 117f, 252f, 248f),
            listOf(Lamp(151f, 50f, 204f), Lamp(357f, 49f, 211f))),
        Place.CAFE to Room(Window(240f, 41f, 295f, 115f, 214f, 250f),
            listOf(Lamp(207f, 44f, 214f), Lamp(427f, 45f, 220f))),
        Place.WORK to Room(Window(461f, 55f, 479f, 140f, 359f, 248f),
            listOf(Lamp(310f, 46f, 215f, 175f))),
        Place.ARCADE to Room(Window(195f, 47f, 249f, 113f, 154f, 246f),
            listOf(Lamp(166f, 44f, 203f), Lamp(281f, 40f, 203f), Lamp(390f, 41f, 210f)))
    ).mapValues { (_,room) -> room.copy(window=Window(84f,48f,145f,88f,190f,242f)) }
}
