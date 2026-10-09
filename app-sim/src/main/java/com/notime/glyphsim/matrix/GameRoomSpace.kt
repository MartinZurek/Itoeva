package com.notime.glyphsim.matrix

/** Game-Raeume bestehen aus Boden, Waenden und einzelnen Koerpern mit gemeinsamen Kontaktkanten. */
object GameRoomSpace {
    enum class Material { WOOD, UPHOLSTERY, LINEN, TILE, METAL }
    data class Face(val points: List<Pair<Float,Float>>, val shade: Int)
    data class Body(val piece: GameFurniture.Piece, val material: Material, val faces: List<Face>)
    data class Palette(val wall: Long, val side: Long, val floor: Long, val trim: Long, val accent: Long)

    fun enabled(scene: GameScenes.Scene) = scene.asset.startsWith("interiors/")
    fun palette(place: PlayScene.Place): Palette = when (place) {
        PlayScene.Place.BEDROOM -> Palette(0xFFD4C3BB,0xFFAEA8B1,0xFFAC8165,0xFF65534B,0xFF847799)
        PlayScene.Place.BATH -> Palette(0xFFBCD3CE,0xFF89ACA6,0xFFC1C9BD,0xFF486C68,0xFF6B9D98)
        PlayScene.Place.CAFE -> Palette(0xFFD6C39E,0xFFAF987E,0xFFAA8061,0xFF654C3D,0xFF65836B)
        PlayScene.Place.ARCADE -> Palette(0xFFAAB0C5,0xFF778297,0xFF8A8291,0xFF41485C,0xFF677EA0)
        else -> Palette(0xFFD4C7AE,0xFFABA48D,0xFFB38B67,0xFF6C5945,0xFF7D9070)
    }

    /** Niedrigere Moebel und mehr freier Boden vermeiden eine Figur direkt vor der Kameralinse. */
    fun reshape(piece: GameFurniture.Piece): GameFurniture.Piece {
        fun y(value: Float) = piece.ground + (value-piece.ground)*.72f
        val depth=when {
            piece.id.endsWith("-bed") -> 46f
            piece.id.contains("sofa") || piece.id.contains("armchair") -> 22f
            piece.id.contains("table") || piece.id.contains("workbench") -> 28f
            piece.id.contains("chair") || piece.id.contains("bench") || piece.id.contains("stool") -> 18f
            else -> 15f
        }
        return piece.copy(top=y(piece.top),back=piece.ground-depth,
            contours=piece.contours.map { points -> points.map { it.first to y(it.second) } },
            seat=piece.seat?.let { it.copy(y=y(it.y)) })
    }

    private fun polygon(shade: Int, vararg xy: Pair<Float,Float>) = Face(xy.toList(),shade)
    private fun rect(shade: Int,x0: Float,y0: Float,x1: Float,y1: Float) =
        polygon(shade,x0 to y0,x1 to y0,x1 to y1,x0 to y1)

    /** Zeichenkonturen werden zugleich fuer Verdeckung verwendet; keine zweite Bild-Moebelliste. */
    private val cache=java.util.concurrent.ConcurrentHashMap<GameFurniture.Piece,Body>()
    fun body(piece: GameFurniture.Piece): Body = cache.getOrPut(piece) { buildBody(piece) }

    private fun buildBody(piece: GameFurniture.Piece): Body {
        val id=piece.id
        val table=id.contains("table") || id.contains("workbench")
        val chair=!id.contains("armchair") && (id.contains("chair") || id.contains("stool") || id.contains("bench"))
        val soft=id.contains("sofa") || id.contains("armchair")
        val bed=id.endsWith("-bed")
        val shelf=id.contains("bookcase") || id.contains("shelf") || id.contains("shelves")
        val metal=id.contains("fridge") || id.contains("machine") || id.contains("tub") || id.contains("basin")
        val x0=piece.left;val x1=piece.right;val seat=piece.top;val bottom=piece.ground
        val high=piece.contours.flatten().minOfOrNull { it.second } ?: seat
        val depth=(piece.ground-piece.back).coerceIn(10f,46f)
        val bevel=((x1-x0)*.06f).coerceIn(3f,7f)
        val faces=mutableListOf<Face>()
        if (table || chair) {
            val legWidth=if (chair) 4f else 5f
            for(x in listOf(x0+bevel,x1-bevel-legWidth)) {
                faces+=rect(-2,x,seat+3f,x+legWidth,bottom)
                faces+=rect(-1,x,seat+3f,x+1f,bottom-2f)
            }
            if(chair) {
                faces+=rect(-1,x0+2f,high,x1-2f,seat-depth*.4f)
                faces+=rect(1,x0+5f,high+3f,x1-5f,seat-depth*.4f-3f)
            }
            faces+=polygon(1,x0+bevel to seat-depth,x1-bevel to seat-depth,x1 to seat,x0 to seat)
            faces+=polygon(-1,x0 to seat,x1 to seat,x1-1f to seat+4f,x0+1f to seat+4f)
        } else {
            val top=if(soft || bed) seat else high
            faces+=rect(-1,x0,top,x1-bevel,bottom)
            faces+=polygon(-2,x1-bevel to top,x1 to top-depth,x1 to bottom-depth,x1-bevel to bottom)
            faces+=polygon(1,x0 to top,x0+bevel to top-depth,x1 to top-depth,x1-bevel to top)
            if(soft || bed) {
                if(soft) faces+=rect(0,x0+bevel,high,x1-bevel,seat-depth*.5f)
                faces+=polygon(2,x0+bevel to seat-depth*.5f,x1-bevel to seat-depth*.5f,
                    x1-bevel*1.5f to seat+2f,x0+bevel*.7f to seat+2f)
                if(soft) {
                    faces+=rect(-1,x0,seat-depth*.8f,x0+bevel,seat+7f)
                    faces+=rect(-1,x1-bevel,seat-depth*.8f,x1,seat+7f)
                    val middle=(x0+x1)/2f
                    faces+=rect(-1,middle-.5f,seat-depth*.5f,middle+.5f,seat+1f)
                } else {
                    // Das Bett hat eine sichtbare Liegeflaeche in die Tiefe, kein schmales Brett.
                    faces+=polygon(2,x0+bevel to seat-depth,x1-bevel to seat-depth,
                        x1-bevel to seat,x0+bevel to seat)
                    faces+=rect(3,x0+bevel*2f,seat-depth+3f,x0+(x1-x0)*.35f,seat-depth+12f)
                    faces+=rect(3,x0+(x1-x0)*.39f,seat-depth+3f,x0+(x1-x0)*.64f,seat-depth+12f)
                    faces+=rect(-1,x0+bevel,seat-depth+16f,x1-bevel,seat-depth+18f)
                }
            }
            if(shelf) {
                val rows=((bottom-high)/17f).toInt().coerceAtLeast(2)
                repeat(rows) { row ->
                    val y=high+4f+row*(bottom-high-8f)/rows
                    faces+=rect(-2,x0+4f,y,x1-bevel-4f,y+12f)
                    var x=x0+6f
                    var book=0
                    while(x<x1-bevel-7f) {
                        faces+=rect(if ((book+row)%3==0) 3 else 2,x,y+2f+(book%2),x+3f,y+11f)
                        x+=5f;book++
                    }
                }
            } else if(!soft && !bed) {
                faces+=rect(-2,(x0+x1-bevel)/2f,high+4f,(x0+x1-bevel)/2f+1f,bottom-3f)
                faces+=rect(2,x1-bevel-7f,high+12f,x1-bevel-5f,high+19f)
                if(id.contains("machine")) faces+=rect(3,x0+8f,high+9f,x1-bevel-8f,high+28f)
            }
        }
        return Body(piece,when {metal->Material.METAL;bed->Material.LINEN;soft->Material.UPHOLSTERY;else->Material.WOOD},faces)
    }

    fun contours(piece: GameFurniture.Piece) = body(piece).faces.map { it.points }
    fun bodies(scene: GameScenes.Scene): List<Body> = GameFurniture.pieces(scene)
        .sortedBy { it.front }.map(::body)
}
