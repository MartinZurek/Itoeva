package com.notime.glyphsim.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
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
        else clipRect(left = -size.width * 4f, top = -size.height * 4f,
            right = size.width * 5f,
            bottom = GameCharacterScale.localFeetY(size.width, species) - rise) { paint() }
    }
}
