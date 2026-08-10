package com.notime.glyphsim.state

import com.notime.glyphsim.ui.AppMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft das neue Zustandsmodell und seine Bruecke zur bisherigen Drei-Modi-Sicht.
 *
 * **Die wichtigste Aussage dieser Suite:** solange beide Sichten nebeneinander bestehen, duerfen
 * sie nicht auseinanderlaufen. Ein Rundlauf `AppMode -> TamaState -> AppMode` muss wieder dasselbe
 * ergeben, sonst zeigte die Auswahlleiste etwas anderes an, als tatsaechlich gilt.
 */
class TamaStateMappingTest {

    // --- Rundlaeufe ----------------------------------------------------------------------------

    @Test
    fun `jeder Modus ueberlebt den Rundlauf`() {
        AppMode.entries.forEach { modus ->
            val zustand = TamaStateMapping.fromAppMode(modus)
            assertEquals(modus, TamaStateMapping.toAppMode(zustand))
        }
    }

    /**
     * Der Dock-Modus steht quer zu den drei Modi: er aendert die Darstellung, nicht den Inhalt.
     * Ein Rundlauf durch `AppMode` darf ihn deshalb nicht umkippen - er geht nur verloren, weil
     * `AppMode` ihn gar nicht kennt.
     */
    @Test
    fun `der Dock-Modus veraendert den Inhalt nicht`() {
        AppMode.entries.forEach { modus ->
            val imDock = TamaStateMapping.fromAppMode(modus, dock = true)
            val ohneDock = TamaStateMapping.fromAppMode(modus, dock = false)

            assertEquals("Der Inhalt haengt nicht am Dock", ohneDock.world, imDock.world)
            assertEquals("Die Modus-Sicht bleibt dieselbe", modus, TamaStateMapping.toAppMode(imDock))
        }
    }

    // --- Die Zuordnung der Schalter -------------------------------------------------------------

    /**
     * Haelt fest, wie die drei gespeicherten Schalter heute auf den Zustand abgebildet werden -
     * die Grundlage dafuer, dass Phase 3 die Zuordnung an EINER Stelle aendern kann.
     */
    @Test
    fun `die Schalter ergeben den erwarteten Zustand`() {
        assertEquals(
            TamaState(Presentation.APP, clockOnly = false, world = WorldMode.ROUTINES),
            TamaStateMapping.fromSwitches(watchOnly = false, playActive = false, dockEnabled = false)
        )
        assertEquals(
            TamaState(Presentation.DOCK, clockOnly = false, world = WorldMode.ROUTINES),
            TamaStateMapping.fromSwitches(watchOnly = false, playActive = false, dockEnabled = true)
        )
        assertEquals(
            TamaState(Presentation.APP, clockOnly = false, world = WorldMode.PLAY),
            TamaStateMapping.fromSwitches(watchOnly = false, playActive = true, dockEnabled = false)
        )
        assertEquals(
            TamaState(Presentation.DOCK, clockOnly = false, world = WorldMode.PLAY),
            TamaStateMapping.fromSwitches(watchOnly = false, playActive = true, dockEnabled = true)
        )
    }

    /**
     * "Nur Uhr" hat Vorrang vor der Dock-Darstellung - dieselbe Regel wie in `AppMode.current`.
     *
     * Wichtig ist dabei, was NICHT passiert: der Inhalt bleibt unberuehrt. Wer im Spielmodus auf
     * "nur Uhr" schaltet, sieht nur die Uhr, aber die Welt laeuft weiter.
     */
    @Test
    fun `nur Uhr und Dock schliessen sich nicht aus`() {
        val ausgangslage = TamaStateMapping.fromSwitches(
            watchOnly = true,
            playActive = true,
            dockEnabled = true
        )

        // Beim ersten Entwurf dieses Modells waren die beiden Alternativen - dadurch waere aus
        // "Dock in Uhr-Darstellung" stillschweigend der Startbildschirm geworden. Der Fehler fiel
        // erst beim Verdrahten auf; dieser Test haelt die Korrektur fest.
        assertEquals(Presentation.DOCK, ausgangslage.presentation)
        assertTrue("Die Reduktion gilt zusaetzlich, nicht stattdessen", ausgangslage.clockOnly)
        assertEquals("Die Darstellung darf den Inhalt nicht umschalten", WorldMode.PLAY, ausgangslage.world)

        // Und die Modus-Sicht loest den Vorrang weiterhin auf: "nur Uhr" gewinnt dort.
        assertEquals(AppMode.WATCH, TamaStateMapping.toAppMode(ausgangslage))
    }

    // --- Abgeleitete Eigenschaften ---------------------------------------------------------------

    /**
     * Der Bildschirm bleibt nur im Dock-Modus an - und zwar unabhaengig davon, was inhaltlich
     * laeuft. Eine Verwechslung hier waere im Betrieb als leerer Akku spuerbar.
     */
    @Test
    fun `nur der Dock-Modus haelt den Bildschirm wach`() {
        WorldMode.entries.forEach { welt ->
            listOf(true, false).forEach { nurUhr ->
                assertTrue(TamaState(Presentation.DOCK, nurUhr, welt).keepScreenOn)
                assertFalse(TamaState(Presentation.APP, nurUhr, welt).keepScreenOn)
            }
        }
    }

    /**
     * Die Fortschrittsanzeige braucht beides: einen Fortschritt und Platz. Auf dem reinen
     * Uhrenbildschirm hat sie nichts zu suchen.
     */
    @Test
    fun `der Spielfortschritt erscheint nur wo Platz dafuer ist`() {
        assertTrue(TamaState(Presentation.APP, clockOnly = false, world = WorldMode.PLAY).showsPlayProgress)
        assertTrue(TamaState(Presentation.DOCK, clockOnly = false, world = WorldMode.PLAY).showsPlayProgress)
        assertFalse(TamaState(Presentation.DOCK, clockOnly = true, world = WorldMode.PLAY).showsPlayProgress)
        assertFalse(TamaState(Presentation.DOCK, clockOnly = false, world = WorldMode.ROUTINES).showsPlayProgress)
    }

    // --- Die Aussage des Modells ------------------------------------------------------------------

    /**
     * **Der Kern von Phase 2, als Test.**
     *
     * WATCH und REMINDER unterscheiden sich AUSSCHLIESSLICH in der Darstellung - der Inhalt ist
     * identisch. Genau deshalb gehoeren sie nicht als gleichrangige Geschwister in eine
     * Auswahlleiste mit PLAY, der als einziger den Inhalt umstellt.
     */
    @Test
    fun `nur Uhr und Normalbetrieb unterscheiden sich allein in der Darstellung`() {
        val watch = TamaStateMapping.fromAppMode(AppMode.WATCH)
        val reminder = TamaStateMapping.fromAppMode(AppMode.REMINDER)

        assertEquals("Der Inhalt ist derselbe", watch.world, reminder.world)
        assertTrue("Nur die Darstellung unterscheidet sich", watch.clockOnly != reminder.clockOnly)
    }

    /**
     * Und der Gegenbeweis: PLAY ist der einzige, der den Inhalt anfasst. Das ist die Zeile, die
     * Phase 3 aufloest - danach soll auch PLAY die Routinen weiterlaufen lassen.
     */
    @Test
    fun `allein der Spielmodus veraendert den Inhalt`() {
        val inhalte = AppMode.entries.map { TamaStateMapping.fromAppMode(it).world }.toSet()

        assertEquals(
            "Genau zwei Inhalte, und der abweichende ist PLAY",
            setOf(WorldMode.ROUTINES, WorldMode.PLAY),
            inhalte
        )
        assertEquals(WorldMode.PLAY, TamaStateMapping.fromAppMode(AppMode.PLAY).world)
    }
}
