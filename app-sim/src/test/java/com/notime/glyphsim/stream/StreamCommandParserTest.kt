package com.notime.glyphsim.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Die Befehlssyntax, gegen das gehalten, was Leute tatsaechlich in einen Chat tippen.
 *
 * Der Parser ist die einzige Stelle, an der aus Text Bedeutung wird. Alles, was hier
 * durchrutscht, wuerde spaeter eine Entscheidung im Spiel ausloesen; alles, was hier faelschlich
 * abgewiesen wird, ist fuer den Zuschauer ein Befehl, der "einfach nichts tut". Beide Fehler
 * sind im Stream gleich schlecht sichtbar, deshalb steht die Grenze hier so ausfuehrlich.
 */
class StreamCommandParserTest {

    private fun slotOf(message: String): Int? =
        (StreamCommandParser.parse(message) as? StreamInteraction.DropSafeSlot)?.slotId

    @Test
    fun `die vier Buchstaben treffen die vier Plaetze`() {
        assertEquals(1, slotOf("!drop A"))
        assertEquals(2, slotOf("!drop B"))
        assertEquals(3, slotOf("!drop C"))
        assertEquals(4, slotOf("!drop D"))
    }

    @Test
    fun `Kleinschreibung und zusaetzlicher Leerraum aendern nichts`() {
        assertEquals(1, slotOf("!drop a"))
        assertEquals(2, slotOf("  !DROP   b  "))
        assertEquals(3, slotOf("!Drop\tC"))
    }

    @Test
    fun `Ziffern sind als Nachsicht erlaubt`() {
        assertEquals(1, slotOf("!drop 1"))
        assertEquals(4, slotOf("!drop 4"))
    }

    @Test
    fun `was hinter dem Platz steht wird verworfen statt den Befehl zu zerstoeren`() {
        assertEquals(1, slotOf("!drop a bitte"))
        assertEquals(2, slotOf("!drop B!"))
        assertEquals(3, slotOf("!drop c."))
    }

    @Test
    fun `ein Platz ausserhalb der vier wird nicht erkannt`() {
        assertNull(slotOf("!drop E"))
        assertNull(slotOf("!drop 0"))
        assertNull(slotOf("!drop 5"))
        assertNull(slotOf("!drop AB"))
    }

    @Test
    fun `gewoehnlicher Chat loest nichts aus`() {
        for (zeile in listOf(
            "hallo zusammen",
            "drop A",
            "!dropA",
            "!drop",
            "!feed A",
            "",
            "   ",
            "!!drop A",
            "ich würde ja !drop A sagen"
        )) {
            assertNull("'$zeile' haette nichts ausloesen duerfen", StreamCommandParser.parse(zeile))
        }
    }

    @Test
    fun `die Zeile im Bild stammt aus derselben Quelle wie der Parser`() {
        val config = StreamCommandConfig()
        val hinweis = config.hint()
        // Sonst zeigt der Stream eine Syntax an, die der Parser gar nicht kennt - der teuerste
        // Fehler hier, weil ihn niemand meldet: Zuschauer tippen ihn ab und geben auf.
        for (teil in hinweis.split("  ")) {
            assertEquals(
                "Der angezeigte Hinweis '$teil' wird vom Parser nicht verstanden",
                true,
                StreamCommandParser.parse(teil, config) != null
            )
        }
    }

    @Test
    fun `ein anderes Praefix wirkt auf Parser und Anzeige zugleich`() {
        val config = StreamCommandConfig(prefix = "?", dropKeyword = "wirf")
        assertEquals(1, (StreamCommandParser.parse("?wirf A", config) as? StreamInteraction.DropSafeSlot)?.slotId)
        assertNull(StreamCommandParser.parse("!drop A", config))
        assertEquals(true, config.hint().startsWith("?wirf A"))
    }
}
