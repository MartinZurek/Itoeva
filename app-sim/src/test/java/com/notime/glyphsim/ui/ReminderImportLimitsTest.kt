package com.notime.glyphsim.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft die Obergrenzen fuer das, was von aussen in den Import hereinkommt.
 *
 * ## Warum das eine eigene Sorge ist
 *
 * Die Activity ist `exported` und nimmt Teilen-Absichten von **jeder** App auf dem Geraet
 * entgegen. Was hier ankommt, ist damit nicht "eine KI-Antwort", sondern beliebiger Text aus
 * beliebiger Quelle - versehentlich (jemand teilt ein ganzes E-Book) wie absichtlich. Ohne
 * Grenzen bestimmt der Absender, wie viel Arbeit die App macht.
 *
 * Der Rest des Imports war schon misstrauisch gegenueber dem *Inhalt* (siehe
 * [ReminderImport.sanitize]) - gegen seine *Menge* war er es nicht.
 */
class ReminderImportLimitsTest {

    // --- Laenge der Eingabe ------------------------------------------------------------------

    @Test
    fun `normale Antwort bleibt unangetastet`() {
        val text = "Hier sind deine Erinnerungen: " + "{\"reminders\":[]}"
        val ergebnis = ReminderImport.begrenzeEingabe(text)

        assertEquals(text, ergebnis.text)
        assertFalse("Nichts zu kuerzen bei normaler Laenge", ergebnis.gekuerzt)
    }

    @Test
    fun `genau die Obergrenze wird noch nicht gekuerzt`() {
        val text = "x".repeat(ReminderImport.MAX_INPUT_CHARS)
        val ergebnis = ReminderImport.begrenzeEingabe(text)

        assertEquals(ReminderImport.MAX_INPUT_CHARS, ergebnis.text.length)
        assertFalse(ergebnis.gekuerzt)
    }

    @Test
    fun `zu langer Text wird gekuerzt und meldet das`() {
        val text = "x".repeat(ReminderImport.MAX_INPUT_CHARS + 1)
        val ergebnis = ReminderImport.begrenzeEingabe(text)

        assertEquals(ReminderImport.MAX_INPUT_CHARS, ergebnis.text.length)
        assertTrue(
            "Kuerzen muss gemeldet werden - sonst sucht der Nutzer nach Erinnerungen, die " +
                "er geteilt hat und die nirgends auftauchen",
            ergebnis.gekuerzt
        )
    }

    // --- Verschachtelungstiefe ---------------------------------------------------------------

    /**
     * Der JSON-Leser arbeitet rekursiv. Genuegend Ebenen erzeugen einen StackOverflowError -
     * und der ist kein `Exception`, wird also von dem `catch` im Dialog NICHT aufgefangen.
     * Deshalb muss die Tiefe vorher begrenzt werden und nicht erst hinterher behandelt.
     */
    @Test
    fun `uebermaessig verschachteltes JSON wird abgelehnt`() {
        val tief = "{\"a\":".repeat(ReminderImport.MAX_JSON_DEPTH + 5) +
            "1" +
            "}".repeat(ReminderImport.MAX_JSON_DEPTH + 5)

        assertNull("Zu tiefe Verschachtelung darf nicht durchgereicht werden", ReminderImport.extractJson(tief))
    }

    @Test
    fun `uebliche Verschachtelung geht weiterhin durch`() {
        val normal = """
            Hier bitte:
            {"reminders":[{"label":"Trinken","days":["MO","DI"]}]}
            Viel Erfolg!
        """.trimIndent()

        val json = ReminderImport.extractJson(normal)
        assertNotNull("Eine gewoehnliche Antwort muss weiterhin gefunden werden", json)
        assertTrue(json!!.startsWith("{"))
        assertTrue(json.endsWith("}"))
    }

    /**
     * Wichtig fuer den Alltag: eine abgeschnittene Antwort (Klammern gehen nie auf null zurueck)
     * darf nicht in eine Endlosschleife oder einen Absturz laufen, sondern schlicht nichts finden.
     */
    @Test
    fun `unvollstaendiges JSON liefert nichts statt zu scheitern`() {
        assertNull(ReminderImport.extractJson("{\"reminders\":[{\"label\":\"Trin"))
    }

    // --- Anzahl der Erinnerungen -------------------------------------------------------------

    /**
     * Die Zahl selbst wird im Dialog angewandt (dort liegt der JSON-Leser). Hier wird nur
     * festgehalten, dass es sie gibt und sie in einer plausiblen Groessenordnung liegt - eine
     * Obergrenze von 2 waere so unbrauchbar wie gar keine.
     */
    @Test
    fun `Obergrenze fuer die Anzahl ist gesetzt und brauchbar`() {
        assertTrue("Zu knapp fuer einen echten Import", ReminderImport.MAX_REMINDERS >= 10)
        assertTrue("So hoch, dass sie nichts mehr abfaengt", ReminderImport.MAX_REMINDERS <= 100)
    }
}
