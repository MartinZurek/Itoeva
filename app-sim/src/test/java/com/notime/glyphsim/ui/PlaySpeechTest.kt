package com.notime.glyphsim.ui

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.matrix.PlayAmbientActivity
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Prueft die drei Bedingungen, die darueber entscheiden, ob der Satz ueber dem Kopf Begleitung
 * oder Geschwaetz ist (siehe [PlaySpeech]).
 */
class PlaySpeechTest {

    @Test
    fun `zu jeder Taetigkeit gibt es einen Satz`() {
        // Sonst bliebe ausgerechnet bei dem Thema, das gerade laeuft, die Frage offen, was er
        // eigentlich vorhat - und genau dafuer gibt es die Saetze.
        val lines = AnimationType.entries.map { PlaySpeech.lineOf(it) }
        assertTrue("Ein Thema ohne Satz", lines.none { it == 0 })
        assertTrue(
            "Zwei Themen teilen sich einen Satz - dann sagt er bei beiden dasselbe",
            lines.toSet().size == AnimationType.entries.size
        )
    }

    @Test
    fun `nachts sagt er nichts`() {
        // Das Dock steht auf einem Nachttisch. Ein Text, der um drei Uhr aufleuchtet, ist genau
        // das, was dieser Modus nicht sein soll.
        for (topic in AnimationType.entries) {
            repeat(50) { seed ->
                assertNull(
                    "$topic spricht nachts",
                    PlaySpeech.lineFor(topic, PlayAmbientActivity.DayPhase.NIGHT, Random(seed))
                )
            }
        }
    }

    @Test
    fun `er kommentiert nicht jede Handlung`() {
        // Ein Wesen, das jede Regung kommentiert, ist anstrengend - man liest nach dem fuenften
        // Mal nicht mehr hin. Umgekehrt darf es auch nicht so selten sein, dass man es fuer einen
        // Zufall haelt.
        val random = Random(9)
        var spoken = 0
        val draws = 3_000
        repeat(draws) {
            if (PlaySpeech.lineFor(
                    AnimationType.DRINK,
                    PlayAmbientActivity.DayPhase.MIDDAY,
                    random
                ) != null
            ) {
                spoken++
            }
        }
        val share = spoken.toFloat() / draws
        assertTrue("Er sagt fast nie etwas ($share)", share > 0.2f)
        assertTrue("Er kommentiert praktisch alles ($share)", share < 0.5f)
    }

    @Test
    fun `der Halbsatz zur offenen Gewohnheit ist fuer alle Themen derselbe`() {
        // Bewusst EIN Text statt zwoelf: Was ihn traegt, ist nicht die Formulierung, sondern der
        // Zeitpunkt - er steht neben einer Figur, die es gerade vormacht.
        assertNotEquals(0, PlaySpeech.habitHint())
    }
}
