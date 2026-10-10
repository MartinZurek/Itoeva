package com.notime.glyphsim.matrix

/** Game-Raeume bestehen aus Boden, Waenden und einzelnen Koerpern mit gemeinsamen Kontaktkanten. */
object GameRoomSpace {
    enum class Material { WOOD, UPHOLSTERY, LINEN, TILE, METAL }
    data class Face(val points: List<Pair<Float,Float>>, val shade: Int)
    data class Body(val piece: GameFurniture.Piece, val material: Material, val faces: List<Face>)
    data class Palette(val wall: Long, val side: Long, val floor: Long, val trim: Long, val accent: Long)

    fun enabled(scene: GameScenes.Scene) = scene.asset.startsWith("interiors/")
    fun palette(place: PlayScene.Place): Palette = when (place) {
        PlayScene.Place.BEDROOM -> Palette(0xFFE1CFAF,0xFFBFA78B,0xFFB28B60,0xFF72573F,0xFF9DAB89)
        PlayScene.Place.BATH -> Palette(0xFFD8DFC9,0xFFACB99B,0xFFBBAE8D,0xFF64705B,0xFF839D85)
        PlayScene.Place.CAFE -> Palette(0xFFE2CCA7,0xFFB8A17E,0xFFAD8159,0xFF71553D,0xFF96A080)
        PlayScene.Place.ARCADE -> Palette(0xFFD1C7B8,0xFFA99DA0,0xFFAA896E,0xFF665A59,0xFF898DA0)
        else -> Palette(0xFFE3D3B4,0xFFBEAE8F,0xFFB48C60,0xFF70563D,0xFFABA88B)
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

    private fun cushion(shade: Int,x0: Float,y0: Float,x1: Float,y1: Float): Face {
        val radius=3f.coerceAtMost((y1-y0)/3f).coerceAtLeast(.5f)
        return polygon(shade,x0+radius to y0,x1-radius to y0,x1 to y0+radius,
            x1 to y1-radius,x1-radius to y1,x0+radius to y1,x0 to y1-radius,x0 to y0+radius)
    }

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
        if(id.contains("tub")) {
            // Eine Wanne hat einen offenen Rand und eine gewoelbte Schale, keine Schrankfront.
            faces+=polygon(-1,x0 to seat,x1 to seat,x1-3 to bottom-12,x1-12 to bottom-3,
                x1-20 to bottom,x0+20 to bottom,x0+9 to bottom-4,x0+2 to bottom-14)
            faces+=polygon(1,x0 to seat,x0+5 to seat-depth,x1-5 to seat-depth,x1 to seat)
            faces+=polygon(2,x0+10 to seat-3,x0+13 to seat-depth+3,x1-13 to seat-depth+3,x1-10 to seat-3)
        } else if (table || chair) {
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
            faces+=if(soft) cushion(-1,x0,top,x1-bevel,bottom) else rect(-1,x0,top,x1-bevel,bottom)
            faces+=polygon(-2,x1-bevel to top,x1 to top-depth,x1 to bottom-depth,x1-bevel to bottom)
            faces+=polygon(1,x0 to top,x0+bevel to top-depth,x1 to top-depth,x1-bevel to top)
            if(soft || bed) {
                if(soft) faces+=cushion(0,x0+bevel,minOf(high,seat-depth*.5f-17f),x1-bevel,seat-depth*.5f)
                faces+=polygon(2,x0+bevel to seat-depth*.5f,x1-bevel to seat-depth*.5f,
                    x1-bevel*1.5f to seat+2f,x0+bevel*.7f to seat+2f)
                if(soft) {
                    faces+=cushion(-1,x0,seat-depth*.8f,x0+bevel,seat+7f)
                    faces+=cushion(-1,x1-bevel,seat-depth*.8f,x1,seat+7f)
                    val middle=(x0+x1)/2f
                    faces+=rect(-1,middle-.5f,seat-depth*.5f,middle+.5f,seat+1f)
                } else {
                    // Das Bett hat eine sichtbare Liegeflaeche in die Tiefe, kein schmales Brett.
                    faces+=polygon(2,x0+bevel to seat-depth,x1-bevel to seat-depth,
                        x1-bevel to seat,x0+bevel to seat)
                    faces+=cushion(3,x0+bevel*2f,seat-depth+3f,x0+(x1-x0)*.35f,seat-depth+12f)
                    faces+=cushion(3,x0+(x1-x0)*.39f,seat-depth+3f,x0+(x1-x0)*.64f,seat-depth+12f)
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

    // Die originale Malerei ist wieder die Kulisse; Masken lesen deren vermessene Silhouetten.
    fun contours(piece: GameFurniture.Piece) = piece.contours
    fun bodies(scene: GameScenes.Scene): List<Body> = GameFurniture.pieces(scene)
        .sortedBy { it.front }.map(::body)
}
