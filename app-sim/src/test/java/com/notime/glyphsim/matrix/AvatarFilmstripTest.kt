package com.notime.glyphsim.matrix

import org.junit.Test

/**
 * Entwicklungswerkzeug, kein Test mit Behauptungen: gibt die ECHTEN Avatar-Frames als ASCII-Raster
 * aus, damit sich Entwuerfe beurteilen lassen, ohne die App auf ein Geraet zu bringen.
 *
 *     ./gradlew :app-sim:testDebugUnitTest --tests "*AvatarFilmstripTest*" -i
 *
 * Warum ASCII und kein PNG: Android-Unit-Tests kompilieren gegen die Android-Stubs, in denen
 * `java.awt`/`javax.imageio` fehlen - Bilder liessen sich hier gar nicht schreiben. Bei einem
 * 16x16-Raster ist die Textform ohnehin gut lesbar und hat den Vorteil, direkt im Build-Log zu
 * stehen. Entscheidend ist, dass [AvatarAnimations] selbst aufgerufen wird und nichts nachgebaut
 * ist: ein separates Skript (wie das im README erwaehnte Python-Werkzeug) kann mit der Zeit vom
 * Produktionscode abweichen und dann Entwuerfe zeigen, die es in der App nicht gibt.
 */
class AvatarFilmstripTest {

    private fun render(frame: IntArray): List<String> {
        val size = AvatarGeometry.SIZE
        return (0 until size).map { y ->
            (0 until size).joinToString("") { x ->
                val brightness = frame.getOrElse(y * size + x) { 0 }
                when {
                    brightness <= 0 -> "."
                    brightness >= AvatarGeometry.MAX_BRIGHTNESS -> "#"
                    else -> "+"
                }
            }
        }
    }

    /** Frames nebeneinander statt untereinander - Bewegung ist so viel leichter zu beurteilen. */
    private fun printFilmstrip(title: String, frames: List<IntArray>, maxFrames: Int = 12) {
        val shown = frames.take(maxFrames)
        println("=== $title (${frames.size} Frames, zeige ${shown.size}) ===")
        val rendered = shown.map { render(it) }
        val header = shown.indices.joinToString("  ") { "F$it".padEnd(AvatarGeometry.SIZE) }
        println(header)
        for (row in 0 until AvatarGeometry.SIZE) {
            println(rendered.joinToString("  ") { it[row] })
        }
        println()
    }

    @Test
    fun printIdlePerSpecies() {
        AvatarSpecies.entries.forEach { species ->
            printFilmstrip(species.name, AvatarAnimations.idleSequence(species).frames)
        }
    }
}
