package com.notime.glyphsim.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import com.notime.glyphsim.matrix.*

/** Raeumliche Pixelkulisse aus denselben Moebelkoerpern wie Kollision, Sitzen und Verdeckung. */
internal fun DrawScope.paintRoomSpace(scene: GameScenes.Scene, fit: GameScenes.Fit, alpha: Float, clock: Long = 0L) {
    val palette=GameRoomSpace.palette(scene.place)
    fun path(points: List<Pair<Float,Float>>) = Path().apply {
        points.forEachIndexed { i,p -> if(i==0) moveTo(p.first,p.second) else lineTo(p.first,p.second) };close()
    }
    fun face(points: List<Pair<Float,Float>>, color: Color, border: Boolean = true) {
        val shape=path(points)
        drawPath(shape,color,alpha=alpha*color.alpha)
        if(border) drawPath(shape,Color(palette.trim).copy(alpha=.60f),alpha=alpha,style=Stroke(.8f))
    }
    fun rect(x: Float,y: Float,w: Float,h: Float,color: Color) =
        drawRect(color,Offset(x,y),Size(w,h),alpha=alpha)
    fun shade(color: Color, value: Int): Color {
        val gain=when(value) {-2->.63f;-1->.81f;1->1.12f;2->1.20f;3->1.38f;else->1f}
        return Color((color.red*gain).coerceAtMost(1f),(color.green*gain).coerceAtMost(1f),
            (color.blue*gain).coerceAtMost(1f),1f)
    }
    withTransform({translate(fit.left,fit.top);scale(fit.scale,fit.scale,Offset.Zero)}) {
        rect(0f,0f,480f,270f,Color(palette.side))
        val floor=listOf(scene.farLeft to scene.farY,scene.farRight to scene.farY,
            scene.nearRight to 270f,scene.nearLeft to 270f)
        face(floor,Color(palette.floor),false)
        clipPath(path(floor)) {
            // Kleine Farbstufen und Perspektivfugen statt einer weichgezeichneten Bildtextur.
            for (row in 0..17) {
                val d=row/17f
                val y=scene.farY+(270f-scene.farY)*d*d
                val next=scene.farY+(270f-scene.farY)*((row+1)/17f)*((row+1)/17f)
                rect(0f,y,480f,(next-y).coerceAtLeast(1f),shade(Color(palette.floor),if(row%3==0) -1 else 0))
                drawLine(Color(palette.trim).copy(alpha=.35f),Offset(0f,y),Offset(480f,y),.7f,alpha=alpha)
                val stagger=if(row%2==0) 0f else 35f
                for(x in -1..8) {
                    val px=x*70f+stagger
                    val back=240f+(px-240f)*(.78f+d*.22f)
                    drawLine(Color(palette.trim).copy(alpha=.30f),Offset(back,y),Offset(px,next),.7f,alpha=alpha)
                    if(scene.place != PlayScene.Place.BATH) {
                        repeat(2) { grain ->
                            rect(px+12f+grain*18f,y+2f,8f+(row%3)*3f,.6f,Color(palette.trim).copy(alpha=.16f))
                        }
                    }
                }
            }
        }
        face(listOf(0f to 12f,scene.farLeft to 28f,scene.farLeft to scene.farY,
            scene.nearLeft to 270f,0f to 270f),Color(palette.side))
        face(listOf(scene.farRight to 28f,480f to 12f,480f to 270f,
            scene.nearRight to 270f,scene.farRight to scene.farY),shade(Color(palette.side),-1))
        face(listOf(scene.farLeft to 28f,scene.farRight to 28f,scene.farRight to scene.farY,
            scene.farLeft to scene.farY),Color(palette.wall))
        rect(scene.farLeft,scene.farY-7f,scene.farRight-scene.farLeft,7f,Color(palette.trim))
        for (x in 60..420 step 18) rect(x.toFloat(),34f,1f,scene.farY-41f,Color(palette.trim).copy(alpha=.08f))
        // Fenster und Wandregal geben hintere Groessenanker; der freie Boden bleibt sichtbar.
        rect(80f,44f,69f,48f,Color(palette.trim))
        rect(84f,48f,61f,40f,Color(0xFFADC7BC))
        rect(84f,48f,61f,8f,Color(0xFFC4D7C0))
        rect(111f,48f,3f,40f,Color(palette.trim))
        rect(84f,66f,61f,3f,Color(palette.trim))
        rect(75f,91f,79f,4f,shade(Color(palette.trim),1))
        for(x in listOf(76f,146f)) repeat(12) { row ->
            val dx=GameFabric.hangingOffset(row/12f,.5f,.10f,clock,scene.place.ordinal)
            rect(x+dx,45f+row*4f,6f,4.1f,shade(Color(palette.accent),if(row%3==0) -1 else 0))
        }
        rect(184f,65f,69f,4f,Color(palette.trim))
        for (i in 0..6) rect(190f+i*8f,51f+(i%3)*2f,5f,14f-(i%3)*2f,shade(Color(palette.accent),i%3-1))
        when(scene.place) {
            PlayScene.Place.CAFE -> {
                rect(287f,41f,98f,56f,Color(palette.trim))
                rect(291f,45f,90f,48f,Color(0xFF334D46))
                for(row in 0..3) {
                    rect(301f,54f+row*8f,38f-row*5f,1f,Color(0xFFD7D1B7))
                    rect(358f,54f+row*8f,10f,1f,Color(0xFFD7D1B7))
                }
            }
            PlayScene.Place.BEDROOM -> {
                rect(322f,43f,43f,48f,Color(palette.trim))
                rect(326f,47f,35f,40f,Color(0xFFBACACB))
                face(listOf(326f to 47f,342f to 47f,361f to 77f,361f to 87f),Color(0xFFD6DEDA),false)
            }
            else -> {
                rect(290f,46f,50f,38f,Color(palette.trim))
                rect(294f,50f,42f,30f,Color(0xFF9AB7AA))
                face(listOf(294f to 80f,310f to 60f,324f to 74f,336f to 63f,336f to 80f),Color(0xFF63856D),false)
                rect(321f,53f,5f,5f,Color(0xFFE7CF94))
            }
        }
        for(lamp in GameLightingCatalog.rooms[scene.place]?.lamps.orEmpty()) {
            rect(lamp.x-1f,28f,2f,(lamp.y-28f).coerceAtLeast(2f),Color(palette.trim))
            face(listOf(lamp.x-7f to lamp.y-4f,lamp.x+7f to lamp.y-4f,
                lamp.x+11f to lamp.y+4f,lamp.x-11f to lamp.y+4f),Color(0xFFE4C68D))
            rect(lamp.x-5f,lamp.y+3f,10f,2f,Color(0xFFF2DAA6))
        }
        for (body in GameRoomSpace.bodies(scene)) {
            val piece=body.piece
            val shadow=listOf(piece.left-4f to piece.ground,piece.right+7f to piece.ground,
                piece.right+12f to piece.ground+7f,piece.left to piece.ground+7f)
            face(shadow,Color(0x332F302B),false)
            val base=when(body.material) {
                GameRoomSpace.Material.UPHOLSTERY->Color(palette.accent)
                GameRoomSpace.Material.LINEN->Color(0xFF9BA6B8)
                GameRoomSpace.Material.METAL->Color(0xFFABBFB5)
                GameRoomSpace.Material.TILE->Color(0xFFB9CEBF)
                GameRoomSpace.Material.WOOD->Color(0xFF9C7653)
            }
            body.faces.forEach { face(it.points,shade(base,it.shade)) }
            // Kleine Materialdetails bleiben auf dem jeweiligen Koerper und erzeugen keine neuen Trefferflaechen.
            if(body.material==GameRoomSpace.Material.WOOD) {
                val shape=path(body.faces.first().points)
                clipPath(shape) {
                    var y=piece.top+8f
                    while(y<piece.ground-3f) {
                        for(x in 0..4) rect(piece.left+8f+x*19f,y,7f+(x%2)*4f,.7f,shade(base,-1))
                        y+=11f
                    }
                }
            }
            if(body.material==GameRoomSpace.Material.LINEN) {
                for(x in 1..9) rect(piece.left+8f+x*14f,piece.top-24f,.7f,21f,shade(base,1))
                for(row in 0..2) rect(piece.left+9f,piece.top-22f+row*8f,piece.right-piece.left-18f,.7f,shade(base,1))
            }
            if(piece.id.contains("counter")) {
                val x=piece.left+20f
                rect(x,piece.top-13f,18f,10f,Color(0xFF496459))
                rect(x+3f,piece.top-15f,12f,3f,Color(0xFFA6B7A6))
                rect(x+5f,piece.top-8f,8f,2f,Color(0xFFE2D2B2))
                for(i in 0..2) rect(x+30f+i*8f,piece.top-7f,5f,6f,Color(0xFFE7D7B7))
            }
            if(piece.id.contains("table") || piece.id.contains("workbench")) {
                val x=(piece.left+piece.right)/2f
                val y=piece.top-14f
                rect(x-8f,y-8f,11f,6f,Color(0xFFE7D7B7))
                rect(x-7f,y-9f,9f,2f,Color(0xFF6B5544))
                rect(x+3f,y-7f,3f,3f,Color(0xFFE7D7B7))
            }
        }
    }
}
