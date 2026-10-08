package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayScene.Station as S

/** Vermessene Moebel: Kollision und Vorderkante lesen dieselbe Oberkante und Bodenlinie. */
object GameFurniture {
    data class Piece(val id: String, val left: Float, val right: Float, val top: Float,
        val ground: Float, val back: Float, val front: Float,
        val contours: List<List<Pair<Float, Float>>>,
        val seat: Seat? = null) {
        fun surface(scene: GameScenes.Scene): GameMovement.Surface {
            val center = GameScenes.posAt(scene, (left + right) / 2f, ground)
            return GameMovement.Surface(id, GameScenes.posAt(scene, left, ground).x,
                GameScenes.posAt(scene, right, ground).x,
                GameScenes.posAt(scene, left, back).depth, GameScenes.posAt(scene, right, front).depth,
                GameScenes.feet(scene, center).second - top, scene.nearY - scene.farY, center.depth)
        }
        fun hides(feetY: Float, support: String?) = support != id && feetY < front
    }

    private fun polygon(vararg points: Pair<Float, Float>) = points.toList()
    private fun leg(x: Float, y: Float, width: Float, bottom: Float) =
        polygon(x to y, x + width to y, x + width - 1f to bottom, x - 1f to bottom)

    private fun measured(scene: GameScenes.Scene): List<Piece> = when {
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
    data class Seat(val x: Float, val y: Float, val stations: Set<PlayScene.Station>)

    private fun body(id: String, left: Float, right: Float, top: Float, ground: Float,
        seatY: Float? = null, stations: Set<PlayScene.Station> = emptySet(), back: Float = ground - 8f): Piece {
        val contours = if (seatY != null) listOf(
            polygon(left - 3f to top, right + 3f to top, right + 5f to seatY + 6f,
                right - 2f to ground, left + 2f to ground, left - 5f to seatY + 6f))
            else listOf(polygon(left to top, right to top, right to ground, left to ground))
        return Piece(id, left, right, seatY ?: top, ground, back, ground + 3f, contours,
            seatY?.let { Seat((left + right) / 2f, it, stations) })
    }
    private fun table(id: String, left: Float, right: Float, top: Float, ground: Float): Piece =
        Piece(id, left, right, top, ground, ground - 10f, ground + 3f, listOf(
            polygon(left - 3f to top, right + 3f to top, right + 4f to top + 6f,
                left - 3f to top + 6f), leg(left + 6f, top + 6f, 6f, ground),
            leg(right - 10f, top + 6f, 6f, ground)))

    /** Auch hohe Schrankkoerper sind feste Hindernisse; ihre Hoehe macht sie unbespringbar. */
    private val cache = java.util.concurrent.ConcurrentHashMap<GameScenes.Scene, List<Piece>>()
    fun pieces(scene: GameScenes.Scene): List<Piece> = cache.getOrPut(scene) { buildPieces(scene) }

    private fun buildPieces(scene: GameScenes.Scene): List<Piece> {
        val original = measured(scene).map { piece ->
            when (piece.id) {
                "living-sofa" -> piece.copy(seat = Seat(245f, 138f, setOf(PlayScene.Station.SEAT)))
                "world-PARK-seat" -> piece.copy(seat = Seat(270f, 459f, setOf(PlayScene.Station.BENCH)))
                "bedroom-bed" -> piece.copy(seat = Seat(198f, 145f, setOf(PlayScene.Station.BED)))
                else -> piece
            }
        }
        if (!scene.asset.startsWith("interiors/")) return original
        val extra = when (scene.place) {
            PlayScene.Place.LIVING -> listOf(
                body("living-bookcase", 334f, 404f, 71f, 161f),
                table("living-side-table", 137f, 174f, 145f, 165f))
            PlayScene.Place.BEDROOM -> listOf(
                body("bedroom-wardrobe", 233f, 292f, 59f, 153f),
                body("bedroom-dresser", 383f, 425f, 111f, 171f),
                body("bedroom-footbench", 189f, 282f, 150f, 180f, 155f, setOf(S.SEAT)),
                table("bedroom-nightstand", 63f, 105f, 133f, 170f))
            PlayScene.Place.KITCHEN -> listOf(
                body("kitchen-counter", 84f, 270f, 121f, 182f),
                body("kitchen-fridge", 395f, 440f, 61f, 204f),
                body("kitchen-cabinet", 291f, 363f, 125f, 190f),
                body("kitchen-chair-left", 141f, 171f, 153f, 213f, 179f, setOf(S.TABLE)),
                body("kitchen-chair-right", 229f, 257f, 152f, 214f, 179f, setOf(S.TABLE)))
            PlayScene.Place.CAFE -> listOf(
                table("cafe-left-table", 110f, 181f, 138f, 196f),
                body("cafe-counter", 299f, 458f, 127f, 201f),
                body("cafe-chair-front", 209f, 251f, 140f, 224f, 177f, setOf(S.TABLE)),
                body("cafe-chair-right", 311f, 353f, 139f, 222f, 176f, setOf(S.TABLE)),
                body("cafe-chair-left", 83f, 118f, 131f, 194f, 166f, setOf(S.SEAT)))
            PlayScene.Place.CRAFT -> listOf(
                table("craft-workbench", 224f, 400f, 130f, 193f),
                body("craft-cabinet", 90f, 140f, 142f, 192f),
                body("craft-crate", 401f, 450f, 167f, 206f),
                body("craft-stool", 249f, 274f, 161f, 196f, 163f, setOf(S.CRAFT)))
            PlayScene.Place.DESK -> listOf(
                table("desk-table", 248f, 390f, 127f, 197f),
                body("desk-drawers", 250f, 292f, 133f, 196f),
                body("desk-chair", 304f, 343f, 123f, 196f, 165f, setOf(S.DESK)),
                body("desk-bookcase-left", 90f, 150f, 81f, 186f),
                body("desk-bookcase-right", 427f, 471f, 47f, 194f))
            PlayScene.Place.WORK -> listOf(
                table("work-table", 231f, 390f, 132f, 197f),
                body("work-drawers", 212f, 251f, 137f, 205f),
                body("work-chair", 273f, 313f, 128f, 202f, 167f, setOf(S.WORKPLACE)),
                body("work-cabinet", 103f, 230f, 63f, 193f),
                body("work-crate", 427f, 465f, 163f, 202f))
            PlayScene.Place.NOOK -> listOf(
                body("nook-armchair", 184f, 278f, 105f, 198f, 157f, setOf(S.SEAT, S.BOOKSHELF)),
                body("nook-bookshelf", 321f, 400f, 53f, 195f),
                table("nook-side-table", 288f, 331f, 156f, 201f),
                body("nook-chest", 424f, 469f, 166f, 204f))
            PlayScene.Place.BATH -> listOf(
                body("bath-basin", 90f, 164f, 126f, 192f),
                Piece("bath-tub", 285f, 425f, 140f, 195f, 185f, 199f,
                    listOf(polygon(281f to 140f,325f to 133f,428f to 137f,429f to 153f,
                        406f to 188f,306f to 188f,290f to 170f))),
                body("bath-stool", 231f, 256f, 161f, 194f, 163f, setOf(S.SEAT)))
            PlayScene.Place.SHOP -> listOf(
                body("shop-counter", 135f, 263f, 163f, 204f),
                body("shop-shelves-left", 95f, 278f, 80f, 185f),
                body("shop-shelves-right", 432f, 472f, 65f, 222f),
                body("shop-stool", 349f, 372f, 178f, 211f, 180f, setOf(S.SEAT)))
            PlayScene.Place.ARCADE -> listOf(
                body("arcade-machine", 284f, 374f, 69f, 203f),
                body("arcade-pinball", 411f, 455f, 128f, 212f),
                body("arcade-console", 96f, 139f, 137f, 195f),
                body("arcade-shelf", 140f, 210f, 134f, 197f),
                body("arcade-stool", 310f, 335f, 161f, 212f, 165f, setOf(S.SEAT)))
            else -> emptyList()
        }
        return original + extra
    }

}
