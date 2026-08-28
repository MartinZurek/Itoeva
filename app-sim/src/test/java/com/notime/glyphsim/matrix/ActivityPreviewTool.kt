package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import org.junit.Test
import java.io.File

/** Manuelles Kontaktbogen-Werkzeug fuer die Weltmotive; `*` markiert den laufenden Effekt. */
class ActivityPreviewTool {

    @Test
    fun dump() {
        val width = ScenePreview.WIDTH
        val floorY = ScenePreview.FLOOR_Y
        val species = AvatarSpecies.PUFFLING
        val out = buildString {
            for (topic in AnimationType.entries) {
                val place = PlayScene.forTopic(topic)
                val x = ((width - AvatarGeometry.SIZE) * PlayScene.avatarAnchorX(place)).toInt()
                val y = floorY - 1 - AvatarBodies.forSpecies(species).groundRow()
                append("\n=== ").append(topic).append(" ===\n")
                append(
                    ScenePreview.render(
                        place = place,
                        species = species,
                        phase = 8,
                        overlay = PlayEffects.activityCells(topic, x, y, 8, width)
                    )
                )
            }
        }
        val target = File(System.getProperty("java.io.tmpdir"), "activity-preview.txt")
        target.writeText(out)
        println("Weltmotiv-Kontaktbogen: ${target.absolutePath}")
    }
}
