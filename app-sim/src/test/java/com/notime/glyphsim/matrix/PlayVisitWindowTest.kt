package com.notime.glyphsim.matrix

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft, WANN jemand vorbeikommen darf.
 *
 * Diese Faelle sind am Geraet praktisch nicht herzustellen: Ein Besuch kommt alle anderthalb bis
 * dreieinhalb Minuten in Frage, und ob er ausbleibt, weil die Regel ihn verbietet oder weil der
 * Wuerfel anders fiel, sieht beim Zusehen genau gleich aus. Genau deshalb ist der Fehler unten
 * so lange unbemerkt geblieben.
 */
class PlayVisitWindowTest {

    private fun offen(
        place: PlayScene.Place = PlayScene.Place.STREET,
        routineRunning: Boolean = false,
        lingeringOutdoors: Boolean = false,
        occupied: Boolean = false,
        walking: Boolean = false,
        settling: Boolean = false,
        hidden: Boolean = false,
        userBusy: Boolean = false
    ) = PlayVisitWindow.isOpen(
        place, routineRunning, lingeringOutdoors, occupied, walking, settling, hidden, userBusy
    )

    @Test
    fun `auf der Strasse ohne laufenden Ablauf kommt jemand vorbei`() {
        assertTrue(offen())
    }

    /**
     * **Die Zeile, um die es geht.** Vorher war "draussen jemanden treffen" strukturell
     * unmoeglich: Unter freien Himmel kommt die Figur nur innerhalb eines Ablaufs, und waehrend
     * eines Ablaufs war jeder Besuch gesperrt. Der Test haelt fest, dass das Stehenbleiben
     * draussen die Ausnahme ist - und dass sie ohne dieses Stehenbleiben nicht gilt.
     */
    @Test
    fun `waehrend eines Ablaufs oeffnet erst das Warten unter freiem Himmel`() {
        assertFalse(offen(routineRunning = true))
        assertTrue(offen(routineRunning = true, lingeringOutdoors = true))
    }

    /** Drinnen bleibt alles wie zuvor - ein kurzes Innehalten am Kuehlschrank ist kein Anlass. */
    @Test
    fun `drinnen bringt ein Ablauf weiterhin keinen Gast`() {
        assertFalse(offen(place = PlayScene.Place.KITCHEN, routineRunning = true))
        // Und selbst wenn das Flag faelschlich gesetzt waere: Die Kueche laesst keine Besucher zu.
        assertFalse(
            offen(place = PlayScene.Place.KITCHEN, routineRunning = true, lingeringOutdoors = true)
        )
    }

    /**
     * Die Ortsregel bleibt uneingeschraenkt vorrangig. Wald und Wiese sind Rueckzugsorte - dort
     * jemandem zu begegnen ist das Gegenteil dessen, wofuer man hingeht (siehe
     * [PlayScene.allowsVisitors]).
     */
    @Test
    fun `das neue Fenster hebelt die Ortsregel nicht aus`() {
        for (place in PlayScene.Place.entries) {
            if (PlayScene.allowsVisitors(place)) continue
            assertFalse(
                "$place laesst ploetzlich Besucher zu",
                offen(place = place, routineRunning = true, lingeringOutdoors = true)
            )
        }
    }

    /** Jede einzelne Sperre genuegt, auch im neuen Fenster. */
    @Test
    fun `jede Sperre allein schliesst das Fenster`() {
        val draussenWartend = mapOf(
            "sitzt" to offen(routineRunning = true, lingeringOutdoors = true, occupied = true),
            "geht" to offen(routineRunning = true, lingeringOutdoors = true, walking = true),
            "setzt sich" to offen(routineRunning = true, lingeringOutdoors = true, settling = true),
            "nicht im Bild" to offen(routineRunning = true, lingeringOutdoors = true, hidden = true),
            "Erinnerung offen" to offen(routineRunning = true, lingeringOutdoors = true, userBusy = true)
        )
        for ((grund, ergebnis) in draussenWartend) {
            assertFalse("Trotz \"$grund\" kommt ein Gast", ergebnis)
        }
    }

    /**
     * Der Nutzer hat immer Vorrang. Ohne diese Zusicherung koennte das neue Fenster einen Gast
     * ausgerechnet in den Moment schieben, in dem eine echte Erinnerung auf dem Schirm steht.
     */
    @Test
    fun `eine offene Erinnerung schlaegt jede noch so passende Lage`() {
        assertFalse(offen(userBusy = true))
        assertFalse(offen(userBusy = true, lingeringOutdoors = true))
        for (place in PlayScene.Place.entries) {
            assertFalse("$place", offen(place = place, userBusy = true))
        }
    }

    /**
     * **Ein Grueppchen statt einer Reihe.** Vorher kam der naechste Gast anderthalb bis
     * dreieinhalb Minuten nach dem letzten - der war nach einer Viertelminute laengst weg. Steht
     * jemand da und ist noch jemand uebrig, kommt er jetzt binnen Sekunden dazu.
     */
    @Test
    fun `steht schon jemand da, kommt der naechste bald dazu`() {
        val allein = PlayVisitWindow.intervalMs(PlayScene.Place.PARK, 0, moreCandidates = true)
        val dazu = PlayVisitWindow.intervalMs(PlayScene.Place.PARK, 1, moreCandidates = true)
        assertTrue(dazu.last < allein.first)
        // Ist niemand mehr uebrig, bleibt es beim gewoehnlichen Takt.
        assertTrue(PlayVisitWindow.intervalMs(PlayScene.Place.PARK, 1, moreCandidates = false) == allein)
        // Strasse und Stadt bleiben belebter als der Park.
        assertTrue(
            PlayVisitWindow.intervalMs(PlayScene.Place.STREET, 0, moreCandidates = true).last <
                allein.first
        )
    }

    /** Draussen bleibt ein Gast eine Weile, drinnen nur kurz - aber nirgends geht er sofort. */
    @Test
    fun `draussen bleibt ein Gast laenger als drinnen`() {
        val draussen = PlayVisitWindow.lingerMs(PlayScene.Place.PARK)
        val drinnen = PlayVisitWindow.lingerMs(PlayScene.Place.SHOP)
        assertTrue(drinnen.first > 0)
        assertTrue(draussen.first > drinnen.last)
    }

    /** Auch nach dem Gespraech gehoert die Szene dem Besuch, bis der letzte Gast gegangen ist. */
    @Test
    fun `autonome Regungen warten bis der letzte Gast gegangen ist`() {
        assertFalse(PlayVisitWindow.pausesHost(0))
        assertTrue(PlayVisitWindow.pausesHost(1))
        assertTrue(PlayVisitWindow.pausesHost(3))
    }

    /**
     * Wer eben gegangen ist, kommt nicht gleich wieder - sonst wechselten sich zwei Bewohner
     * endlos ab, sobald der naechste Gast binnen Sekunden dazukommen darf.
     */
    @Test
    fun `wer eben gegangen ist, kommt nicht sofort wieder`() {
        val jetzt = 1_000_000L
        val gesperrt = PlayVisitWindow.unavailableGuests(
            visiting = listOf("a"),
            lastLeftAtMs = mapOf("b" to jetzt - 10_000L, "c" to jetzt - 10 * 60_000L),
            nowMs = jetzt,
            pace = 1f
        )
        assertTrue("a" in gesperrt)
        assertTrue("b" in gesperrt)
        assertFalse("c" in gesperrt)
        // Im Zeitraffer vergeht auch diese Sperre schneller.
        assertFalse(
            "b" in PlayVisitWindow.unavailableGuests(
                emptyList(), mapOf("b" to jetzt - 10_000L), jetzt, pace = 0.1f
            )
        )
    }
}
