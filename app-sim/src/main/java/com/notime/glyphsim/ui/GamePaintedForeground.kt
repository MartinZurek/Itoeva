package com.notime.glyphsim.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import com.notime.glyphsim.matrix.*

/** Jeder Charakter wird an denselben Moebelkonturen verdeckt, auch ein NPC hinter der Bank. */
internal fun Modifier.gameCharacterOcclusion(scene: GameScenes.Scene?, pos: PlayControl.Pos,
    species: AvatarSpecies, support: String?, lift: Float, fit: GameScenes.Fit, offset: Offset): Modifier {
    if (scene == null) return this
    return drawWithContent {
        val feetY = GameScenes.feet(scene, pos).second
        val pieces = GameFurniture.pieces(scene).filter { it.hides(feetY, support) }
        val contours = Path().apply {
            for (piece in pieces) for (points in if (GameRoomSpace.enabled(scene)) GameRoomSpace.contours(piece) else piece.contours) {
                points.forEachIndexed { index, (x, y) ->
                    val (sx, sy) = fit.toScreen(x, y)
                    if (index == 0) moveTo(sx - offset.x, sy - offset.y)
                    else lineTo(sx - offset.x, sy - offset.y)
                }
                close()
            }
        }
        if (support==null && lift<=0f && GameWorld.material(scene,GameScenes.feet(scene,pos).first,feetY)==GameEnvironment.Material.GRASS) {
            val (fx,fy)=GameScenes.feet(scene,pos)
            // Bodennahe Halme verdecken die Pfoten; der Koerper bleibt vor den niedrigen Pflanzen.
            val x0=fx-12f;val x1=fx+12f
            val points=buildList {
                add(x0 to fy+3f)
                for(i in 0..12) add(x0+i*(x1-x0)/12f to fy-3f-(i%3)*1.5f)
                add(x1 to fy+3f)
            }
            points.forEachIndexed { i,(x,y) ->
                val (sx,sy)=fit.toScreen(x,y)
                if(i==0) contours.moveTo(sx-offset.x,sy-offset.y) else contours.lineTo(sx-offset.x,sy-offset.y)
            }
            contours.close()
        }
        fun paint() {
            if (contours.isEmpty) drawContent()
            else clipPath(contours, ClipOp.Difference) { this@drawWithContent.drawContent() }
        }
        paint()
    }
}
