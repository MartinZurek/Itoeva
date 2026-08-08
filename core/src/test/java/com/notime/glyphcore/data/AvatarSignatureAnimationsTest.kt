package com.notime.glyphcore.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft die 30 charakterspezifischen Animationen ([AvatarSignatureAnimations]) auf das, was sich
 * an reinen Punktdaten ueberhaupt pruefen laesst - ob eine Zeichnung *schoen* ist, entscheidet
 * weiterhin das Auge (siehe [renderSample], das die Raster ausgibt).
 *
 * Die Invarianten hier sind nicht theoretisch: Ein Frame ausserhalb des Rasters verschwindet
 * beim Abspielen stillschweigend, zwei identische Frames hintereinander sind ein sichtbarer
 * Stillstand mitten in der Animation, und ein leerer Frame ein Aussetzer.
 */
class AvatarSignatureAnimationsTest {

    private val size = ReminderFrameGrid.SIZE

    private fun framesOf(animation: LibraryAnimation): List<List<Pair<Int, Int>>> =
        FrameCodec.decode(animation.framesData)

    @Test
    fun everyAnimationHasEnoughFrames() {
        for (animation in AvatarSignatureAnimations.seed()) {
            val frames = framesOf(animation)
            assertTrue(
                "${animation.label} hat nur ${frames.size} Frames - zu wenig fuer wahrgenommene Bewegung",
                frames.size >= 5
            )
        }
    }

    /** Punkte ausserhalb 0..12 waeren beim Abspielen unsichtbar - die Figur waere angeschnitten,
     *  ohne dass es irgendwo auffiele. */
    @Test
    fun everyPointIsInsideTheGrid() {
        for (animation in AvatarSignatureAnimations.seed()) {
            for ((index, frame) in framesOf(animation).withIndex()) {
                for ((x, y) in frame) {
                    assertTrue(
                        "${animation.label}, Frame $index: Punkt ($x,$y) liegt ausserhalb des ${size}x$size-Rasters",
                        x in 0 until size && y in 0 until size
                    )
                }
            }
        }
    }

    @Test
    fun noFrameIsEmpty() {
        for (animation in AvatarSignatureAnimations.seed()) {
            for ((index, frame) in framesOf(animation).withIndex()) {
                assertTrue("${animation.label}, Frame $index ist leer", frame.isNotEmpty())
            }
        }
    }

    /** Zwei gleiche Frames hintereinander sind ein Standbild mitten in der Bewegung. */
    @Test
    fun everyFrameDiffersFromItsPredecessor() {
        for (animation in AvatarSignatureAnimations.seed()) {
            val frames = framesOf(animation)
            for (i in 1 until frames.size) {
                assertTrue(
                    "${animation.label}: Frame ${i - 1} und $i sind identisch\n" +
                        render(frames[i]),
                    frames[i - 1].toSet() != frames[i].toSet()
                )
            }
        }
    }

    /** Zu wenige Punkte sind auf 13x13 nicht als Motiv lesbar, zu viele fluten das Raster. */
    @Test
    fun everyFrameHasAReadableAmountOfPixels() {
        for (animation in AvatarSignatureAnimations.seed()) {
            for ((index, frame) in framesOf(animation).withIndex()) {
                val lit = frame.toSet().size
                assertTrue(
                    "${animation.label}, Frame $index hat nur $lit Punkte - als Motiv nicht lesbar",
                    lit >= 2
                )
                assertTrue(
                    "${animation.label}, Frame $index hat $lit Punkte - das Raster laeuft ueber",
                    lit <= size * size * 2 / 3
                )
            }
        }
    }

    @Test
    fun labelsAreUniqueAndDoNotClashWithTheGeneralSet() {
        val signature = AvatarSignatureAnimations.seed().map { it.label }
        assertEquals("Doppelte Labels im Signatur-Satz", signature.size, signature.toSet().size)

        val all = DefaultLibraryAnimations.seed().map { it.label }
        assertEquals(
            "Ein Signatur-Label kollidiert mit einer allgemeinen Animation - seedOrRefresh " +
                "gleicht ueber das Label ab und wuerde die falsche Zeichnung ueberschreiben",
            all.size,
            all.toSet().size
        )
    }

    @Test
    fun everyAvatarGotFiveAnimations() {
        assertEquals(30, AvatarSignatureAnimations.seed().size)
    }

    private fun render(frame: List<Pair<Int, Int>>): String {
        val lit = frame.toSet()
        return (0 until size).joinToString("\n") { y ->
            (0 until size).joinToString("") { x -> if ((x to y) in lit) "#" else "." }
        }
    }

    /**
     * Kein Test im eigentlichen Sinn, sondern das Auge: gibt die Raster der hier eingetragenen
     * Animationen aus, damit sich eine Zeichnung tatsaechlich beurteilen laesst - genau so sind
     * Schnecke, Schildkroete und Auge in ihre jetzige Form gekommen, nachdem sie als Klumpen
     * begonnen hatten.
     *
     * Zum Benutzen die gewuenschten Labels eintragen und
     * `./gradlew :core:testDebugUnitTest --tests '*AvatarSignatureAnimationsTest.renderSample' --rerun-tasks`
     * laufen lassen; die Ausgabe steht in
     * `core/build/test-results/testDebugUnitTest/TEST-*.xml` unter `system-out`.
     * Standardmaessig leer, damit der Testlauf nicht bei jedem Build zugemuellt wird.
     */
    @Test
    fun renderSample() {
        val wanted = emptyList<String>()
        for (animation in AvatarSignatureAnimations.seed().filter { it.label in wanted }) {
            println("=== ${animation.label} ${animation.emoji} ===")
            framesOf(animation).forEachIndexed { index, frame ->
                println("-- Frame $index")
                println(render(frame))
            }
        }
    }
}
