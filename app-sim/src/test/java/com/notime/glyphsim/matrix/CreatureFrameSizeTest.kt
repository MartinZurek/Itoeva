package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Der Traumblasen-Fund (gemeldet 2026-09-16): "nur Pixelsalat, ich sehe da nichts".**
 *
 * `PlayDreamBubble` und die vergroesserte Uhr im Schlafrueckblick zeigten eine ganz normale
 * Kreatur-Reaktion - also genau die Frames, die dieser Test misst -, aber ueber
 * [SimulatedMatrixView] statt ueber [AvatarSpriteView]. Ein Frame dieser Groesse (16x20 =
 * 320 Zellen) in einen Leser gegeben, der eine 13x13-Zeile (169 Zellen) erwartet, wird mit der
 * falschen Zeilenbreite gelesen - keine Ausnahme, kein Absturz, nur ein Bild, das keine
 * Beziehung mehr zur eigentlichen Pose hat.
 *
 * Bewusst eine eigene, kleine Datei statt ein Zusatz in `AvatarAnimationsTest`: Jene Datei
 * braucht `MatrixAnimator` (und damit `android.os.SystemClock`/Kotlin-Coroutines) und laeuft
 * deshalb nur unter Gradle, nie in der Offline-Strecke (siehe deren KDoc und der Kommentar in
 * `tools/reaction-preview/tests.sh`). Dieser Fund waere damit lokal ungeprueft geblieben.
 *
 * Der Test kann die Compose-Zeichnung selbst nicht pruefen. Er haelt aber die Tatsache fest, an
 * der dieser Fehler haengt: Eine Kreatur-Reaktion hat NIE die Groesse, die [SimulatedMatrixView]
 * erwartet. Waechst dieser Abstand jemals auf null - etwa weil jemand [AvatarGeometry] oder
 * [MatrixGeometry] aendert -, faellt das hier sofort auf, statt erst wieder am Bildschirm
 * bemerkt zu werden.
 */
class CreatureFrameSizeTest {

    @Test
    fun creatureFramesNeverFitTheThirteenByThirteenMatrixReader() {
        val creatureFrameSize = AvatarGeometry.SIZE * AvatarGeometry.HEIGHT
        val matrixFrameSize = MatrixGeometry.SIZE * MatrixGeometry.SIZE
        assertTrue(
            "Kreatur-Raster ($creatureFrameSize) und Matrix-Raster ($matrixFrameSize) sind " +
                "gleich gross geworden - eine Kreatur-Reaktion passt dann formal in " +
                "SimulatedMatrixView, waere dort aber weiterhin falsch: Sie gehoert in " +
                "AvatarSpriteView. Nur PlayWishBubble/ActionSlotSymbols/MoonFrame/clockFrame " +
                "duerfen SimulatedMatrixView speisen.",
            creatureFrameSize != matrixFrameSize
        )
        for (species in AvatarSpecies.entries) {
            for (type in AnimationType.entries) {
                for (frame in AvatarAnimations.reactionFramesFor(species, type)) {
                    assertEquals(
                        "$species/$type liefert nicht die Groesse, die AvatarSpriteView " +
                            "erwartet - waere das je 169, liesse sich der Fund oben nicht mehr " +
                            "am Zahlenverhaeltnis erkennen.",
                        creatureFrameSize,
                        frame.size
                    )
                    assertTrue(
                        "$species/$type haette zufaellig genau die Groesse des 13x13-Matrix-" +
                            "Rasters und waere damit fuer SimulatedMatrixView unterscheidbar " +
                            "falsch statt erkennbar falsch.",
                        frame.size != matrixFrameSize
                    )
                }
            }
        }
    }
}
