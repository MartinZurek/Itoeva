package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import org.junit.Test
import java.io.File

/**
 * Manuelles Kontaktbogen-Werkzeug fuer die Weltmotive.
 *
 * **Es zeigt mehrere Takte, nicht einen.** Ein Standbild sagt ueber eine Animation nur, ob sie
 * gut AUSSIEHT - nicht, ob sie sich gut BEWEGT. Genau daran ist hier schon Arbeit vorbeigelaufen:
 * Ein Motiv, das im geprueften Einzelbild sauber stand, blitzte im Ablauf nur auf oder zuckte
 * gleichfoermig hin und her. Deshalb laeuft jedes Motiv ueber eine volle Schleife.
 */
class ActivityPreviewTool {

    /** Takte, ueber die jedes Motiv gezeigt wird - eine volle Schleife plus Anschluss. */
    private val beats = listOf(0, 3, 6, 9, 12)

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
                for (beat in beats) {
                    append("\n=== ").append(topic).append("  Takt ").append(beat).append(" ===\n")
                    append(
                        ScenePreview.render(
                            place = place,
                            species = species,
                            phase = beat,
                            overlay = PlayEffects.activityCells(topic, x, y, beat, width)
                        )
                    )
                }
            }
        }
        val target = File(System.getProperty("java.io.tmpdir"), "activity-preview.txt")
        target.writeText(out)
        println("Weltmotiv-Kontaktbogen: ${target.absolutePath}")
    }
}
