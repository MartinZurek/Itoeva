package com.notime.glyphsim.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import com.notime.glyphsim.matrix.*

/** Jeder Charakter wird an denselben Moebelkonturen verdeckt, auch ein NPC hinter der Bank. */
internal fun Modifier.gameCharacterOcclusion(scene: GameScenes.Scene?, pos: PlayControl.Pos,
    species: AvatarSpecies, support: String?, lift: Float, fit: GameScenes.Fit, offset: Offset): Modifier {
    if (scene == null) return this
    return drawWithContent {
        val feetY = GameScenes.feet(scene, pos).second
        val pieces = GameFurniture.pieces(scene).filter { it.hides(feetY, support) }
        val rise = if (lift > 0f) 0f else GameCharacterScale.waterRise(scene, pos) * fit.scale
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
        fun paint() {
            if (pieces.isEmpty()) drawContent()
            else clipPath(contours, ClipOp.Difference) { this@drawWithContent.drawContent() }
        }
        if (rise <= 0f) paint()
        else {
            val waterline=GameCharacterScale.localFeetY(size.width,species)-rise
            // Untergetauchte Glieder bleiben schwach blau lesbar. Ein harter Schnitt
            // entfernte bislang den Beinschlag und einen grossen Teil des Armzugs.
            clipRect(left=-size.width,top=waterline,right=size.width*2f,bottom=size.height*2f) {
                val tint=Paint().apply {
                    alpha=.26f
                    colorFilter=ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
                        .55f,0f,0f,0f,0f, 0f,.82f,0f,0f,0f,
                        0f,0f,1f,0f,12f, 0f,0f,0f,1f,0f)))
                }
                drawContext.canvas.saveLayer(Rect(-size.width,waterline,size.width*2f,size.height*2f),tint)
                try { paint() } finally { drawContext.canvas.restore() }
            }
            clipRect(left=-size.width*4f,top=-size.height*4f,right=size.width*5f,bottom=waterline) { paint() }
        }
    }
}
