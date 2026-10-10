package com.notime.glyphsim.matrix

import kotlin.math.*

/** Konzeptstudien als echte Pixelgruppen: statische Materiallagen auf gemeinsamen Raumkoerpern. */
object GameRoomArt {
    data class Mark(val points: List<Pair<Float,Float>>, val color: Long,
        val oval: Boolean = false, val clip: List<Pair<Float,Float>>? = null)
    private val cache=java.util.concurrent.ConcurrentHashMap<GameScenes.Scene,List<Mark>>()
    fun marks(scene: GameScenes.Scene): List<Mark> = cache.getOrPut(scene) { Painter(scene).paint() }

    /** Ortsfeste Pixelzeichnung des Tuerblatts; Aufschwingen veraendert nur seine projizierte Breite. */
    fun door(width: Int,height: Int): List<Mark> {
        val marks=mutableListOf<Mark>()
        fun rect(x: Int,y: Int,w: Int,h: Int,color: Long) {
            if(w>0 && h>0) marks+=Mark(listOf(x.toFloat() to y.toFloat(),(x+w).toFloat() to y.toFloat(),
                (x+w).toFloat() to (y+h).toFloat(),x.toFloat() to (y+h).toFloat()),color)
        }
        rect(0,0,width,height,0xFF624936)
        rect(2,2,width-4,height-4,0xFFA47B50)
        rect(3,3,1,height-6,0xFFCCA671)
        for(x in 6 until width-4 step 6) {
            rect(x,3,1,height-6,0xFF8C6644)
            for(y in 8 until height-6 step 17) {
                rect(x-2,y,1,5+(x+y)%5,0xFFAE8657)
            }
        }
        for(y in listOf(9,height/2+4)) {
            val h=if(y==9) height/2-15 else height/2-13
            rect(5,y,width-10,h,0xFF775639)
            rect(6,y+1,width-12,h-2,0xFFB18858)
            rect(7,y+2,width-14,1,0xFFD0AA71)
            rect(7,y+3,1,h-5,0xFFC29A63)
        }
        rect(width-8,height*58/100-2,3,5,0xFF76583B)
        rect(width-7,height*58/100-1,2,2,0xFFE2C37B)
        for(y in listOf(15,height-19)) rect(2,y,3,3,0xFF675742)
        return marks
    }

    private class Painter(val scene: GameScenes.Scene) {
        val p=GameRoomSpace.palette(scene.place)
        val marks=mutableListOf<Mark>()
        val ink=0xFF66503C
        val wood=0xFFAD8053
        val cream=0xFFEBD8AC
        val moss=0xFF718363
        val clay=0xFFB57756
        val books=listOf(0xFFB67A58,0xFF738574,0xFFD2B482,0xFF8894A3,0xFFAB9572)
        var clipping: List<Pair<Float,Float>>? = null
        fun poly(points: List<Pair<Float,Float>>, color: Long) { marks+=Mark(points,color,clip=clipping) }
        fun poly(color: Long,vararg xy: Pair<Float,Float>) = poly(xy.toList(),color)
        fun rect(x: Float,y: Float,w: Float,h: Float,color: Long) = poly(color,x to y,x+w to y,x+w to y+h,x to y+h)
        fun oval(x: Float,y: Float,w: Float,h: Float,color: Long) {
            marks+=Mark(listOf(x to y,x+w to y+h),color,true,clipping)
        }
        fun line(x: Float,y: Float,ex: Float,ey: Float,color: Long,width: Float=1f) {
            val length=hypot(ex-x,ey-y).coerceAtLeast(.01f)
            val dx=(ey-y)/length*width/2;val dy=(ex-x)/length*width/2
            poly(color,x-dx to y+dy,ex-dx to ey+dy,ex+dx to ey-dy,x+dx to y-dy)
        }
        fun shade(color: Long,gain: Float): Long = 0xFF000000L or
            (((((color shr 16) and 255)*gain).roundToInt().coerceIn(0,255)).toLong() shl 16) or
            (((((color shr 8) and 255)*gain).roundToInt().coerceIn(0,255)).toLong() shl 8) or
            ((((color and 255)*gain).roundToInt().coerceIn(0,255)).toLong())
        fun grain(shape: List<Pair<Float,Float>>,color: Long,seed: Int,vertical: Boolean=false) {
            val previous=clipping;clipping=shape
            val x0=shape.minOf { it.first }.toInt();val x1=shape.maxOf { it.first }.toInt()
            val y0=shape.minOf { it.second }.toInt();val y1=shape.maxOf { it.second }.toInt()
            // Deterministische kurze Gruppen statt Rauschen oder weichgezeichneter Zeichnung.
            for(y in y0..y1 step 3) for(x in x0..x1 step 5) {
                val hash=(x*73+y*131+seed*31).and(255)
                if(hash<64) rect(x.toFloat(),y.toFloat(),if(vertical) 1f else (2+hash%4).toFloat(),
                    if(vertical) (2+hash%5).toFloat() else 1f,shade(color,if(hash%3==0) 1.045f else .96f))
            }
            clipping=previous
        }
        fun border(points: List<Pair<Float,Float>>,color: Long=ink) {
            var last=points.last()
            for(pt in points) { line(last.first,last.second,pt.first,pt.second,color);last=pt }
        }
        fun plant(x: Float,y: Float,size: Float=1f,flower: Boolean=false) {
            // Kleine Pflanzen stehen auf vorhandenen Moebeln/Wandregalen, nie als neue Bodenhindernisse.
            poly(clay,x-4*size to y-7*size,x+5*size to y-7*size,x+3*size to y,x-3*size to y)
            rect(x-5*size,y-8*size,11*size,2*size,shade(clay,1.15f))
            for(i in 0..6) {
                val dx=sin(i*2.1f)*7*size;val dy=-(11+i%3*4)*size
                line(x,y-8*size,x+dx,y+dy,ink)
                oval(x+dx-3*size,y+dy-3*size,6*size,4*size,if(i%2==0) moss else shade(moss,.8f))
                rect(x+dx-1*size,y+dy-2*size,2*size,1*size,shade(moss,1.25f))
                if(flower && i%2==0) {
                    oval(x+dx-2*size,y+dy-5*size,4*size,3*size,cream)
                    rect(x+dx,y+dy-4*size,1f,1f,0xFFD3A756)
                }
            }
            rect(x-2*size,y-5*size,1f,3*size,shade(clay,1.3f))
        }
        fun vine(x: Float,y: Float,length: Int,seed: Int) {
            var px=x;var py=y
            for(i in 0 until length) {
                val nx=x+sin((i+seed)*.67f)*5;val ny=y+i*3f
                line(px,py,nx,ny,shade(moss,.68f))
                val side=if(i%2==0) -1 else 1
                poly(if(i%3==0) shade(moss,1.14f) else moss,
                    nx to ny,nx+side*5 to ny-3,nx+side*7 to ny-1,nx+side*3 to ny+2)
                px=nx;py=ny
            }
        }
        fun book(x: Float,y: Float,w: Float,h: Float,index: Int) {
            rect(x,y-h,w,h,books[index.mod(books.size)])
            rect(x+1,y-h+2,1f,h-3,shade(books[index.mod(books.size)],1.18f))
            rect(x,y-3,w,1f,cream)
        }
        fun cup(x: Float,y: Float) {
            oval(x-6,y-1,14f,3f,shade(wood,.77f))
            rect(x-4,y-7,8f,6f,cream);rect(x-3,y-8,6f,2f,ink)
            rect(x+4,y-6,3f,4f,cream);rect(x+5,y-5,1f,2f,wood)
        }
        fun quilt(shape: List<Pair<Float,Float>>,base: Long,seed: Int) {
            val previous=clipping;clipping=shape
            val x0=shape.minOf { it.first }.toInt();val x1=shape.maxOf { it.first }.toInt()
            val y0=shape.minOf { it.second }.toInt();val y1=shape.maxOf { it.second }.toInt()
            for(y in y0..y1 step 8) for(x in x0..x1 step 10) {
                rect(x.toFloat(),y.toFloat(),9f,7f,shade(base,if((x/10+y/8+seed)%3==0) .91f else 1.04f))
                rect(x+2f,y+2f,2f,1f,cream)
                rect(x+5f,y+4f,2f,1f,shade(base,.8f))
            }
            clipping=previous
        }
        fun rug(x: Float,y: Float,w: Float,h: Float,color: Long) {
            oval(x,y,w,h,0x252F271E)
            oval(x+1,y-2,w-2,h,color)
            oval(x+5,y+1,w-10,h-6,cream)
            oval(x+8,y+3,w-16,h-10,color)
            oval(x+12,y+5,w-24,h-14,shade(color,.83f))
            for(i in 0..26) {
                val a=i*6.283f/27
                val cx=x+w/2+cos(a)*(w/2-13);val cy=y+h/2+sin(a)*(h/2-7)-2
                poly(cream,cx to cy-2,cx+3 to cy,cx to cy+2,cx-3 to cy)
            }
            for(i in 0..22) {
                val xx=x+10+i*(w-20)/22
                line(xx,y+h*.8f,xx-1,y+h*.8f+4,shade(cream,.84f))
            }
        }
        fun paint(): List<Mark> {
            rect(0f,0f,480f,270f,p.side)
            val floor=listOf(scene.farLeft to scene.farY,scene.farRight to scene.farY,
                scene.nearRight to 270f,scene.nearLeft to 270f)
            poly(floor,p.floor)
            clipping=floor
            for(row in 0..15) {
                val d=row/15f;val next=(row+1)/15f
                val y=scene.farY+(270-scene.farY)*d*d
                val end=scene.farY+(270-scene.farY)*next*next
                for(col in -1..8) {
                    val x=col*68f+if(row%2==0) 0 else 32
                    val back=240+(x-240)*(.78f+d*.22f)
                    val front=240+(x-240)*(.78f+next*.22f)
                    val shape=listOf(back to y,back+68*(.78f+d*.22f) to y,
                        front+68*(.78f+next*.22f) to end,front to end)
                    val base=shade(p.floor,1f+(row*3+col).mod(5)*.018f-.04f)
                    poly(shape,base);grain(shape,base,row+col)
                    line(back,y,back+68*(.78f+d*.22f),y,shade(p.floor,.76f))
                    line(back,y,front,end,shade(p.floor,.78f))
                    if(row>5 && (row+col)%4==0) {
                        oval(front+21,y+3,6f,2f,shade(p.floor,.8f))
                        rect(front+20,y+2,10f,1f,shade(p.floor,1.09f))
                    }
                }
            }
            clipping=null
            val left=listOf(0f to 12f,scene.farLeft to 28f,scene.farLeft to scene.farY,scene.nearLeft to 270f,0f to 270f)
            val right=listOf(scene.farRight to 28f,480f to 12f,480f to 270f,scene.nearRight to 270f,scene.farRight to scene.farY)
            val wall=listOf(scene.farLeft to 28f,scene.farRight to 28f,scene.farRight to scene.farY,scene.farLeft to scene.farY)
            poly(left,p.side);grain(left,p.side,4,true)
            poly(right,shade(p.side,.88f));grain(right,shade(p.side,.88f),7,true)
            poly(wall,p.wall);grain(wall,p.wall,scene.place.ordinal)
            for(y in 34..109 step 12) {
                rect(48f,y.toFloat(),384f,1f,shade(p.wall,.91f))
                rect(48f,y+1f,384f,1f,shade(p.wall,1.04f))
            }
            // Sichtbare Dachbalken und schraege Streben greifen die Huettenstudie auf.
            rect(0f,12f,480f,14f,shade(wood,.67f))
            rect(48f,26f,384f,5f,wood)
            for(x in listOf(43f,429f)) {
                val beam=listOf(x to 25f,x+9 to 25f,x+9 to 120f,x to 120f)
                poly(beam,shade(wood,.76f));grain(beam,wood,x.toInt(),true)
                rect(x+2,29f,1f,83f,shade(wood,1.15f))
            }
            line(51f,28f,77f,54f,shade(wood,.69f),6f)
            line(429f,28f,406f,51f,shade(wood,.69f),6f)
            rect(48f,108f,384f,8f,shade(wood,.74f))
            rect(48f,108f,384f,2f,shade(wood,1.22f))
            window()
            shelf()
            wallDecoration()
            vine(56f,29f,18,scene.place.ordinal)
            vine(421f,29f,12,3)
            // Teppiche liegen flach unter den Koerpern und sperren keinen Weg.
            when(scene.place) {
                PlayScene.Place.BATH -> rug(190f,188f,112f,29f,moss)
                PlayScene.Place.ARCADE -> rug(160f,177f,149f,38f,0xFF8D8091)
                PlayScene.Place.CAFE -> rug(217f,202f,132f,42f,clay)
                PlayScene.Place.BEDROOM -> rug(99f,164f,194f,51f,clay)
                else -> rug(162f,166f,164f,48f,clay)
            }
            for(lamp in GameLightingCatalog.rooms[scene.place]?.lamps.orEmpty()) {
                line(lamp.x,28f,lamp.x,lamp.y-5,ink)
                oval(lamp.x-12,lamp.y-7,24f,13f,ink)
                oval(lamp.x-11,lamp.y-7,22f,11f,0xFFE3B76C)
                rect(lamp.x-9,lamp.y+2,18f,2f,0xFFF3D99F)
                for(i in -2..2) line(lamp.x+i*3,lamp.y-5,lamp.x+i*4,lamp.y+1,0xFFBF965A)
                oval(lamp.x-3,lamp.y+2,6f,3f,cream)
            }
            for(body in GameRoomSpace.bodies(scene)) furniture(body)
            return marks.toList()
        }
        fun window() {
            // Bogen, Holzsprossen und blaue Ferne bleiben im vermessenen Lichtfenster.
            val outside=listOf(80f to 91f,80f to 58f,84f to 48f,93f to 40f,105f to 36f,
                122f to 36f,137f to 41f,146f to 50f,150f to 59f,150f to 91f)
            poly(outside,ink)
            val inside=listOf(85f to 87f,85f to 59f,88f to 50f,99f to 43f,112f to 41f,
                126f to 44f,137f to 51f,144f to 60f,144f to 87f)
            poly(inside,0xFF8DA6AC)
            clipping=inside
            rect(84f,42f,62f,18f,0xFFB3C2BE)
            oval(126f,49f,9f,9f,cream)
            for(i in 0..9) {
                val x=84+i*7f;val y=66f+(i%3)*4
                poly(if(i%2==0) 0xFF718F85 else 0xFF627F7B,x to y,x+7 to 85f,x-7 to 85f)
            }
            clipping=null
            border(inside,wood)
            rect(111f,42f,3f,46f,wood);rect(85f,66f,59f,3f,wood)
            rect(86f,69f,25f,1f,shade(wood,1.3f));rect(114f,69f,30f,1f,shade(wood,1.3f))
            rect(77f,89f,78f,5f,wood);rect(76f,89f,80f,2f,shade(wood,1.24f))
            plant(96f,90f,.55f,true);plant(133f,90f,.6f)
            // Die bewegten Stoffstreifen werden vom Renderer vor der Raumkulisse eingesetzt.
        }
        fun shelf() {
            val x=if(scene.place==PlayScene.Place.LIVING) 157f else 179f
            rect(x-2,68f,48f,3f,ink);rect(x-3,65f,50f,3f,wood)
            for(i in 0..4) book(x+i*6,65f,4f,10f+(i%3)*2,i)
            plant(x+39,65f,.6f)
            line(x+5,71f,x+9,77f,wood,2f);line(x+39,71f,x+35,77f,wood,2f)
        }
        fun wallDecoration() {
            when(scene.place) {
                PlayScene.Place.CAFE -> {
                    rect(283f,39f,103f,61f,wood);rect(287f,43f,95f,53f,0xFF42544B)
                    rect(293f,48f,83f,1f,cream)
                    for(row in 0..3) {
                        for(i in 0..4) rect(298f+i*6,56f+row*8,3f+(i%2),1f,cream)
                        rect(360f,56f+row*8,10f,1f,cream)
                    }
                    plant(402f,99f,.8f,true)
                }
                PlayScene.Place.BEDROOM -> {
                    oval(389f,41f,40f,52f,wood);oval(393f,45f,32f,44f,0xFFA9BCBA)
                    poly(0xFFCED5C5,399f to 47f,405f to 47f,423f to 77f,420f to 87f)
                }
                else -> {
                    rect(349f,44f,51f,39f,wood);rect(353f,48f,43f,31f,cream)
                    rect(356f,51f,37f,25f,0xFFA4B6A5)
                    poly(moss,356f to 76f,366f to 60f,377f to 72f,386f to 59f,393f to 76f)
                    oval(378f,53f,6f,6f,cream)
                    rect(353f,81f,44f,1f,shade(wood,.71f))
                }
            }
        }
        fun furniture(body: GameRoomSpace.Body) {
            val piece=body.piece;val id=piece.id
            val x0=piece.left;val x1=piece.right;val top=piece.top;val ground=piece.ground
            val high=piece.contours.flatten().minOf { it.second }
            val base=when(body.material) {
                GameRoomSpace.Material.UPHOLSTERY->p.accent
                GameRoomSpace.Material.LINEN->0xFF899B82
                GameRoomSpace.Material.METAL->0xFFBBC8B4
                GameRoomSpace.Material.TILE->0xFFC5CFB9
                GameRoomSpace.Material.WOOD->wood
            }
            oval(x0-4,ground-2,x1-x0+12,11f,0x282F271E)
            for((index,face) in body.faces.withIndex()) {
                val faceBase=when {
                    body.material==GameRoomSpace.Material.UPHOLSTERY && face.shade==0 -> 0xFFD6C6A1
                    id.contains("tub") && face.shade==2 -> 0xFF8FABAE
                    id.contains("machine") && face.shade==3 -> 0xFF374E52
                    else -> base
                }
                val gain=when(face.shade) {-2->.62f;-1->.82f;1->1.12f;2->1.2f;3->1.38f;else->1f}
                val color=shade(faceBase,gain)
                poly(face.points,color);border(face.points,shade(ink,.94f))
                if(body.material==GameRoomSpace.Material.WOOD && face.shade<=1) grain(face.points,color,index)
                if(body.material==GameRoomSpace.Material.UPHOLSTERY) {
                    grain(face.points,color,index)
                    if(face.shade==2) {
                        clipping=face.points
                        for(i in 1..2) line(x0+(x1-x0)*i/3,top-10,x0+(x1-x0)*i/3,top+2,shade(base,.8f))
                        clipping=null
                    }
                }
                if(body.material==GameRoomSpace.Material.LINEN && face.shade==2) quilt(face.points,base,2)
                if(id.contains("book") || id.contains("shel")) {
                    if(face.shade==2 || face.shade==3) {
                        val pts=face.points
                        val bx=pts.minOf { it.first };val by=pts.minOf { it.second }
                        book(bx,pts.maxOf { it.second },pts.maxOf { it.first }-bx,
                            pts.maxOf { it.second }-by,index)
                    }
                }
            }
            // Alle grossen Dekore bleiben innerhalb der Koerperkontur, also auch der Verdeckung.
            val soft=body.material==GameRoomSpace.Material.UPHOLSTERY
            if(soft) {
                for(i in 0..1) {
                    val x=x0+10+i*(x1-x0-31)
                    val y=top-23
                    val cushion=listOf(x+2 to y,x+14 to y+1,x+17 to y+13,x+1 to y+14,x to y+3)
                    clipping=body.faces.firstOrNull { it.shade==0 }?.points
                    if(clipping!=null) {
                        poly(cushion,if(i==0) cream else clay);border(cushion,shade(p.accent,.8f))
                        line(x+3,y+2,x+12,y+3,shade(cream,.86f))
                        rect(x+7,y+6,2f,2f,shade(clay,.9f))
                    }
                    clipping=null
                }
            }
            if(soft) {
                // Getuftete Rueckenlehne, Naehte und Saum des Sitzpolsters.
                clipping=body.faces.single { it.shade==0 }.points
                for(i in 1..3) {
                    val x=x0+(x1-x0)*i/4
                    rect(x,top-27,2f,2f,shade(cream,.71f))
                    line(x-1,top-26,x-3,top-22,shade(cream,.88f))
                }
                clipping=null
                val seat=body.faces.first { it.shade==2 }
                clipping=seat.points
                line(x0+10,top,x1-10,top,cream)
                for(i in 0..8) rect(x0+12+i*(x1-x0-24)/8,top-2,1f,1f,shade(base,.72f))
                clipping=null
            }
            if(id.endsWith("-bed")) {
                val blanket=body.faces.last { it.shade==2 }
                clipping=blanket.points
                rect(x0+12,top-28,(x1-x0)*.38f,32f,clay)
                for(i in 0..5) {
                    rect(x0+15+i*8,top-25,1f,26f,shade(clay,1.16f))
                    rect(x0+16+i*8,top-24,1f,26f,shade(clay,.85f))
                }
                clipping=body.faces.first().points
                rect(x0+7,top+1,x1-x0-18,ground-top-3,wood)
                val drape=listOf(x0+12 to top,x0+75 to top,x0+73 to ground-3,x0+53 to ground-5,x0+33 to ground-2,x0+12 to ground-4)
                poly(drape,clay);quilt(drape,clay,1)
                for(i in 0..5) line(x0+17+i*9,top+4,x0+17+i*9,ground-6,shade(clay,.81f))
                clipping=null
            }
            val shelf=id.contains("bookcase") || id.contains("shel")
            val table=id.contains("table") || id.contains("workbench") || id.contains("nightstand")
            if(shelf) {
                val plantX=x0+(x1-x0)*.65f
                // Kein Dekor oberhalb der Kollisionskontur: oberstes Regalbrett bleibt sichtbar.
                clipping=body.faces.first().points
                plant(plantX,high+25,.6f)
                clipping=null
            }
            if(table) {
                // Tassen und Vase sitzen auf der hinteren Haelfte der vorhandenen Tischplatte.
                val surface=body.faces.firstOrNull { it.shade==1 && it.points.size>=4 }
                if(surface!=null) {
                    clipping=surface.points
                    cup((x0+x1)/2,top-7)
                    if(x1-x0>70) plant(x1-20,top-11,.65f,true)
                    clipping=null
                }
            }
            if(body.material==GameRoomSpace.Material.WOOD && !shelf && !table && !id.contains("chair") && !id.contains("stool") && !id.contains("bench")) {
                clipping=body.faces.first().points
                // Rahmen, eingesetzte Paneele und Messinggriffe machen Holz lesbar.
                val width=(x1-x0-14)/2
                for(i in 0..1) {
                    val x=x0+4+i*(width+2)
                    rect(x,high+5,width,ground-high-10,shade(wood,.82f))
                    rect(x+2,high+7,width-4,ground-high-14,shade(wood,1.04f))
                    rect(x+3,high+8,1f,ground-high-17,shade(wood,1.2f))
                    rect(x+width-5,high+16,2f,4f,0xFFD4B16F)
                    rect(x+width-5,high+20,2f,1f,shade(wood,.7f))
                }
                clipping=null
            }
            if(id.contains("machine")) {
                clipping=body.faces.first { it.shade==3 }.points
                rect(x0+10,high+11,x1-x0-28,15f,0xFF354C54)
                for(i in 0..5) {
                    val x=x0+15+i*9;val y=high+15+(i%2)*4
                    rect(x,y,3f,2f,if(i%2==0) 0xFFD4BA7E else 0xFF88ADA6)
                }
                rect(x0+34,high+23,8f,2f,cream)
                clipping=null
            }
            if(id.contains("basin")) {
                clipping=body.faces.first().points
                oval(x0+7,high+3,x1-x0-23,13f,cream)
                oval(x0+13,high+5,x1-x0-34,6f,0xFF8FABAE)
                rect(x0+21,high+3,4f,5f,0xFF718D87)
                clipping=null
            }
            if(id.contains("counter")) {
                clipping=body.faces.first().points
                rect(x0+8,high+6,24f,16f,0xFF52645B)
                rect(x0+11,high+8,18f,3f,0xFF9AABA0)
                rect(x0+16,high+15,8f,3f,cream)
                for(i in 0..3) rect(x0+41+i*8,high+7,5f,4f,books[i])
                clipping=null
            }
        }
    }
}
