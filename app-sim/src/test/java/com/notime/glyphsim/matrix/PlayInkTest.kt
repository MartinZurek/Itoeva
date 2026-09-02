package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Haelt die Gestaltungsregeln der Requisiten-Ebene fest - die Regeln, an denen Lesbarkeit haengt
 * und die sich beim Zeichnen am leichtesten verlieren.
 *
 * **Warum das Tests sind und keine Anmerkung im Text.** Jede dieser Regeln ist hier schon einmal
 * gebrochen worden, und keine davon faellt beim Programmieren auf: Ein Motiv, das mit der Kulisse
 * verschmilzt, laeuft fehlerfrei; eines, das unten aus der Welt herauslaeuft, auch. Sichtbar wird
 * das erst auf dem Geraet oder auf dem Kontaktbogen - also zu spaet oder gar nicht. Ein Test
 * dagegen meldet sich in derselben Sekunde.
 */
class PlayInkTest {

    private val avatarX = 8
    private val avatarY = 20
    private val width = 48

    /** Die unterste Koerperzeile der Figur ist die Standlinie - siehe PlayEffects.activityCells. */
    private val groundY = avatarY + AvatarGeometry.HEIGHT - 1

    private fun everyMotif(block: (AnimationType, List<SceneCell>) -> Unit) {
        for (topic in AnimationType.entries) {
            for (phase in 0..14) {
                block(topic, PlayEffects.activityCells(topic, avatarX, avatarY, phase, width))
            }
        }
    }

    @Test
    fun `kein Motiv erfindet eigene Helligkeiten`() {
        everyMotif { topic, cells ->
            val fremd = cells.map { it.brightness }.filterNot { it in PlayInk.LEVELS }.distinct()
            assertTrue(
                "$topic benutzt Stufen ausserhalb des Zeichenkastens: $fremd",
                fremd.isEmpty()
            )
        }
    }

    @Test
    fun `Requisiten liegen ueber der Kulisse, nicht in ihr`() {
        everyMotif { topic, cells ->
            val material = cells.count { it.brightness >= PlayInk.BODY }
            assertTrue(
                "$topic hat kein Material oberhalb der Moebelstufe - es verschwindet in der Kulisse",
                material >= 6
            )
        }
    }

    @Test
    fun `nichts wird unter dem Boden gezeichnet`() {
        // Die Bodenzeile selbst ist erlaubt - dort liegt der Schatten, den ein Gegenstand auf den
        // Boden wirft. Alles darunter waere ausserhalb der Welt.
        val unterste = groundY + 1
        everyMotif { topic, cells ->
            val darunter = cells.filter { it.y > unterste }.map { it.x to it.y }
            assertTrue(
                "$topic zeichnet $darunter unterhalb des Bodens $unterste",
                darunter.isEmpty()
            )
        }
    }

    /**
     * Die Kernregel des Looks: Jede Zelle, die an den blanken Hintergrund grenzt, muss sich von
     * ihm trennen koennen - entweder als Materie mit schwarzer Kante ringsum, oder als Licht,
     * das heller ist als alles, was in einer Kulisse vorkommen kann.
     *
     * Verboten ist genau das Dazwischen: Materiegewicht ohne Aussparung. Das ist schwer genug,
     * um mit einem Moebel zu verschmelzen, und zu dunkel, um sich davon abzusetzen - und es ist
     * der Fehler, mit dem diese Ebene angefangen hat. Sehr schwache Zeichnung bis DETAIL darf
     * dagegen anstossen: Sie ist Atmosphaere und SOLL sich einfuegen.
     */
    @Test
    fun `was an den Hintergrund grenzt, trennt sich von ihm`() {
        everyMotif { topic, cells ->
            val belegt = cells.map { it.x to it.y }.toSet()
            for (cell in cells) {
                if (cell.brightness <= PlayInk.DETAIL || cell.brightness >= PlayInk.EDGE) continue
                for ((dx, dy) in listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0)) {
                    val nx = cell.x + dx
                    val ny = cell.y + dy
                    // Was ausserhalb des Spielfelds oder unter dem Boden liegt, wird ohnehin
                    // nicht gezeichnet und braucht deshalb auch keine Aussparung.
                    if (nx !in 0 until width || ny < 0 || ny > groundY) continue
                    assertTrue(
                        "$topic: Materie bei (${cell.x},${cell.y}) grenzt bei ($nx,$ny) offen an " +
                            "den Hintergrund - entweder Aussparung ringsum oder ab EDGE hell",
                        (nx to ny) in belegt
                    )
                }
            }
        }
    }

    @Test
    fun `getragene Gegenstaende heben sich vom leuchtenden Koerper ab`() {
        for (item in PlayEffects.Carried.entries) {
            val cells = PlayEffects.carriedCells(item, avatarX, avatarY)
            assertTrue("$item wird gar nicht gezeichnet", cells.isNotEmpty())
            // Ohne schwarzen Rand liegt ein Gegenstand mittlerer Helligkeit auf einer helleren
            // Flaeche - dem Koerper - und ist damit schlicht nicht zu sehen.
            assertTrue(
                "$item ist nicht freigestellt und geht im Koerper unter",
                cells.any { it.brightness == PlayInk.VOID }
            )
            assertTrue(
                "$item benutzt Stufen ausserhalb des Zeichenkastens",
                cells.all { it.brightness in PlayInk.LEVELS }
            )
        }
    }

    /**
     * Die sieben grossen Mehrphasen-Szenen (Drachen, Fussball, Basketball, Training, Musik,
     * Malen, Angeln) rechneten ihre Helligkeiten bislang selbst aus - jede mit einer eigenen
     * Formel wie `GLOW - 180`. Seit ihrer Umstellung auf [PlayInk] gilt fuer sie dieselbe Regel
     * wie fuer die kleinen Weltmotive in [everyMotif]: Genau die Freiheit, sich eine eigene Stufe
     * auszudenken, hat den Look vorher wieder auseinanderlaufen lassen.
     */
    @Test
    fun `auch die grossen Phasen-Szenen benutzen nur den Zeichenkasten`() {
        fun assertOnlyInk(name: String, cells: List<SceneCell>) {
            val fremd = cells.map { it.brightness }.filterNot { it in PlayInk.LEVELS }.distinct()
            assertTrue(
                "$name benutzt Stufen ausserhalb des Zeichenkastens: $fremd",
                fremd.isEmpty()
            )
        }
        for (phase in PlayEffects.FootballPhase.entries) {
            for (scenePhase in 0..10) {
                assertOnlyInk(
                    "Fussball/$phase/$scenePhase",
                    PlayEffects.footballCells(avatarX, avatarY, phase, scenePhase, width)
                )
            }
        }
        for (phase in PlayEffects.BasketballPhase.entries) {
            for (scenePhase in 0..10) {
                assertOnlyInk(
                    "Basketball/$phase/$scenePhase",
                    PlayEffects.basketballCells(avatarX, avatarY, phase, scenePhase, width)
                )
            }
        }
        for (phase in PlayEffects.TrainingPhase.entries) {
            for (scenePhase in 0..10) {
                assertOnlyInk(
                    "Training/$phase/$scenePhase",
                    PlayEffects.trainingCells(avatarX, avatarY, phase, scenePhase)
                )
            }
        }
        for (phase in PlayEffects.MusicPhase.entries) {
            for (scenePhase in 0..10) {
                assertOnlyInk(
                    "Musik/$phase/$scenePhase",
                    PlayEffects.musicCells(avatarX, avatarY, phase, scenePhase, width)
                )
            }
        }
        for (phase in PlayEffects.PaintingPhase.entries) {
            for (scenePhase in 0..10) {
                assertOnlyInk(
                    "Malen/$phase/$scenePhase",
                    PlayEffects.paintingCells(avatarX, avatarY, phase, scenePhase, width)
                )
            }
        }
        for (phase in PlayEffects.FishingPhase.entries) {
            for (scenePhase in 0..10) {
                assertOnlyInk(
                    "Angeln/$phase/$scenePhase",
                    PlayEffects.fishingCells(avatarX, avatarY, phase, scenePhase, width)
                )
            }
        }
        for (phase in PlayEffects.KitePhase.entries) {
            for (scenePhase in 0..10) {
                assertOnlyInk(
                    "Drachen/$phase/$scenePhase",
                    PlayEffects.kiteCells(avatarX, avatarY, phase, scenePhase, width)
                )
            }
        }
    }

    @Test
    fun `die Bewegungskurven bleiben in ihren Grenzen`() {
        for (phase in 0..40) {
            val swing = PlayInk.swing(phase, 12)
            assertTrue("swing($phase) liegt bei $swing", swing in 0f..1f)
            val anticipate = PlayInk.anticipate(phase, 12)
            assertTrue("anticipate($phase) liegt bei $anticipate", anticipate in -1f..1f)
        }
        // Der Anlauf muss wirklich gegen die Bewegung laufen, sonst ist er keiner.
        val früh = (0..11).map { PlayInk.anticipate(it, 12) }
        assertTrue("der Anlauf holt nie aus", früh.any { it < -0.5f })
        assertTrue("der Anlauf fuehrt nie aus", früh.any { it > 0.5f })
    }
}
