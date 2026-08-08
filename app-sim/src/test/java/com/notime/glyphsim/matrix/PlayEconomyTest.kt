package com.notime.glyphsim.matrix

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Der Wirtschaftskreislauf: arbeiten → Geld → einkaufen → Vorrat → essen → wieder von vorn.
 *
 * **Die Gefahr bei so einer Schleife ist nicht, dass sie abstuerzt, sondern dass sie KLEMMT.**
 * Kostet ein Einkauf mehr, als ein Arbeitstag einbringt, muesste der Avatar mehrfach arbeiten,
 * bevor er sich etwas leisten kann - der Tagesablauf bestuende dann fast nur noch aus Arbeit,
 * und alle uebrigen Orte kaemen kaum noch vor. Das faellt beim Zuschauen erst nach vielen
 * Minuten auf, und dann sucht man den Fehler ueberall, nur nicht in zwei Zahlen.
 *
 * Reine Arithmetik, kein Android noetig.
 */
class PlayEconomyTest {

    @Test
    fun `ein Arbeitstag deckt mindestens einen Einkauf`() {
        assertTrue(
            "Ein Einkauf (${PlayWallet.GROCERY_COST}) kostet mehr als ein Arbeitstag einbringt " +
                "(${PlayWallet.WAGE}) - der Avatar muesste mehrfach hintereinander arbeiten, und " +
                "der Tagesablauf verkaeme zur Maloche.",
            PlayWallet.WAGE >= PlayWallet.GROCERY_COST
        )
    }

    @Test
    fun `ein Einkauf reicht fuer mehrere Mahlzeiten`() {
        // Sonst waere er dauernd im Laden statt zu Hause - Einkaufen soll die Ausnahme sein,
        // nicht der Alltag.
        assertTrue(
            "Ein Einkauf fuellt nur ${PlayPantry.FULL} Mahlzeit(en) auf - zu wenig, um den " +
                "Laden zur Ausnahme zu machen.",
            PlayPantry.FULL >= 3
        )
    }

    @Test
    fun `die Schleife laeuft ohne Sackgasse durch`() {
        // Durchgespielt mit reinen Zahlen: Wer arbeitet, kann danach einkaufen; wer einkauft,
        // kann danach essen; wer aufgegessen hat, kann wieder arbeiten. Bliebe an einer Stelle
        // etwas uebrig, das nicht weiterfuehrt, stuende der Avatar still.
        var coins = 0
        var meals = 0
        var worked = 0
        var shopped = 0
        var ate = 0
        repeat(30) {
            when {
                meals > 0 -> { meals--; ate++ }
                coins >= PlayWallet.GROCERY_COST -> {
                    coins -= PlayWallet.GROCERY_COST
                    meals = PlayPantry.FULL
                    shopped++
                }
                else -> { coins += PlayWallet.WAGE; worked++ }
            }
            assertTrue("Negativer Kontostand", coins >= 0)
            assertTrue("Negativer Vorrat", meals >= 0)
        }
        // Entscheidend ist nicht der Kontostand am Ende - dass die Rechnung zufaellig im
        // Tiefpunkt endet, ist voellig in Ordnung, dort geht es ja weiter. Entscheidend ist,
        // dass ALLE DREI Taetigkeiten vorkommen: Bliebe eine aus, haenge die Schleife in einem
        // Zustand fest, und der Avatar taete nur noch eine einzige Sache.
        assertTrue("Es wird nie gearbeitet", worked > 0)
        assertTrue("Es wird nie eingekauft", shopped > 0)
        assertTrue("Es wird nie gegessen", ate > 0)
        // Und: gegessen wird oefter als eingekauft, sonst waere der Laden der Hauptaufenthaltsort.
        assertTrue("Er ist oefter im Laden als beim Essen ($shopped zu $ate)", ate > shopped)
    }
}
