package com.notime.glyphsim.matrix

/** Vermessene Moebel: Kollision und Vorderkante lesen dieselbe Oberkante und Bodenlinie. */
object GameFurniture {
    data class Piece(val id: String, val left: Float, val right: Float, val top: Float,
        val ground: Float, val back: Float, val front: Float,
        val contours: List<List<Pair<Float, Float>>>) {
        fun surface(scene: GameScenes.Scene): GameMovement.Surface {
            val center = GameScenes.posAt(scene, (left + right) / 2f, ground)
            return GameMovement.Surface(id, GameScenes.posAt(scene, left, ground).x,
                GameScenes.posAt(scene, right, ground).x,
                GameScenes.posAt(scene, left, back).depth, GameScenes.posAt(scene, right, front).depth,
                ground - top, scene.nearY - scene.farY, center.depth)
        }
        fun hides(feetY: Float, support: String?) = support != id && feetY < front
    }

    private fun polygon(vararg points: Pair<Float, Float>) = points.toList()
    private fun leg(x: Float, y: Float, width: Float, bottom: Float) =
        polygon(x to y, x + width to y, x + width - 1f to bottom, x - 1f to bottom)

    fun pieces(scene: GameScenes.Scene): List<Piece> = when {
        GameWorld.isWorld(scene) && scene.place == PlayScene.Place.PARK -> listOf(
            Piece("world-PARK-seat", 191f, 346f, 459f, 501f, 489f, 505f, listOf(
                polygon(180f to 418f, 355f to 418f, 357f to 454f, 352f to 458f,
                    353f to 470f, 185f to 470f, 180f to 463f),
                leg(186f, 468f, 8f, 501f), leg(345f, 468f, 8f, 501f))))
        scene.asset.startsWith("interiors/") -> when (scene.place) {
            PlayScene.Place.LIVING -> listOf(
                Piece("living-sofa", 180f, 292f, 138f, 164f, 161f, 167f, listOf(
                    polygon(170f to 128f,181f to 114f,207f to 112f,231f to 114f,
                        249f to 112f,280f to 114f,293f to 123f,302f to 126f,
                        307f to 137f,303f to 162f,251f to 166f,244f to 162f,
                        216f to 160f,185f to 163f,174f to 154f))),
                Piece("living-tea-table", 170f, 220f, 149f, 171f, 164f, 173f, listOf(
                    polygon(167f to 146f,175f to 145f,195f to 144f,216f to 147f,
                        226f to 149f,224f to 153f,219f to 155f,180f to 156f,
                        172f to 154f,167f to 152f),
                    leg(173f, 154f, 6f, 171f), leg(211f, 155f, 6f, 171f))))
            PlayScene.Place.BEDROOM -> listOf(
                Piece("bedroom-bed", 111f, 275f, 145f, 179f, 173f, 182f, listOf(
                    polygon(106f to 145f,129f to 128f,196f to 130f,261f to 145f,
                        275f to 161f,271f to 177f,112f to 177f))))
            PlayScene.Place.KITCHEN -> listOf(
                Piece("kitchen-table", 124f, 267f, 153f, 212f, 202f, 218f, listOf(
                    polygon(119f to 148f,180f to 146f,267f to 150f,273f to 155f,
                        267f to 161f,126f to 161f,119f to 155f),
                    leg(128f, 160f, 7f, 211f), leg(250f, 160f, 7f, 212f))))
            PlayScene.Place.CAFE -> listOf(
                Piece("cafe-table", 235f, 328f, 151f, 219f, 209f, 222f, listOf(
                    polygon(233f to 146f,254f to 141f,303f to 143f,329f to 149f,
                        330f to 155f,309f to 159f,250f to 157f,233f to 152f),
                    leg(242f, 157f, 6f, 216f), leg(315f, 158f, 6f, 219f))))
            else -> emptyList()
        }
        else -> emptyList()
    }
}
