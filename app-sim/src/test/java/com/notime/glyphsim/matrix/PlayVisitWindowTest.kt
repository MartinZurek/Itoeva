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
}
