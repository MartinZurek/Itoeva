package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.living.SymbolicIntent
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft den Nachklang einer beantworteten Erinnerung - was von ihr uebrig bleibt, nachdem die
 * eine angeforderte Routine gelaufen ist.
 *
 * Am Geraet waere davon nichts zu belegen: Man muesste eine Erinnerung beantworten, danach eine
 * Viertelstunde zusehen und koennte hinterher nicht sagen, ob das Wesen wegen der Antwort beim
 * Thema blieb oder weil der Wuerfel es ohnehin ergab. Genau deshalb steht die Rechnung in einer
 * reinen Funktion.
 */
class PlayAfterglowTest {

    private val TAGESBEGINN = 1_000_000_000_000L
    private val MITTAGS = TAGESBEGINN + 12L * 60 * 60 * 1000

    private fun antwort(topic: AnimationType, vorMillis: Long, now: Long = MITTAGS) =
        PlayAfterglow.Answer(topic, now - vorMillis)

    // ================= Die beiden Zeitskalen =================

    @Test
    fun `frisch beantwortet klingt stark nach`() {
        val bonus = PlayAfterglow.bonuses(
            listOf(antwort(AnimationType.BOOK, vorMillis = 30_000)),
            nowMillis = MITTAGS,
            dayStartMillis = TAGESBEGINN
        )
        assertEquals(PlayAfterglow.ECHO_BONUS, bonus[AnimationType.BOOK])
    }

    @Test
    fun `nach dem Nachklang bleibt die Tagesfarbe`() {
        val bonus = PlayAfterglow.bonuses(
            listOf(antwort(AnimationType.BOOK, vorMillis = PlayAfterglow.ECHO_MS)),
            nowMillis = MITTAGS,
            dayStartMillis = TAGESBEGINN
        )
        assertEquals(PlayAfterglow.DAY_BONUS, bonus[AnimationType.BOOK])
    }

    @Test
    fun `die Tagesfarbe verschwindet nicht im Lauf des Tages`() {
        // Der Punkt der ganzen Sache: Am Abend soll noch zu sehen sein, worum morgens gebeten
        // wurde. Ein Nachklang, der nach einer Stunde weg ist, aendert keinen Tag.
        val bonus = PlayAfterglow.bonuses(
            listOf(antwort(AnimationType.BOOK, vorMillis = 10L * 60 * 60 * 1000)),
            nowMillis = MITTAGS,
            dayStartMillis = TAGESBEGINN
        )
        assertEquals(PlayAfterglow.DAY_BONUS, bonus[AnimationType.BOOK])
    }

    @Test
    fun `der frische Nachklang ist staerker als die Tagesfarbe`() {
        // Sonst waeren die beiden Skalen nur zwei Namen fuer dasselbe.
        assertTrue(PlayAfterglow.ECHO_BONUS > PlayAfterglow.DAY_BONUS)
    }

    // ================= Was nicht nachklingt =================

    @Test
    fun `von gestern klingt nichts nach`() {
        val bonus = PlayAfterglow.bonuses(
            listOf(PlayAfterglow.Answer(AnimationType.BOOK, TAGESBEGINN - 1)),
            nowMillis = MITTAGS,
            dayStartMillis = TAGESBEGINN
        )
        assertTrue(bonus.isEmpty())
    }

    @Test
    fun `eine Antwort aus der Zukunft zaehlt nicht`() {
        // Kann nur eine verstellte Uhr sein. Sie zu zaehlen hiesse, etwas nachklingen zu lassen,
        // das noch nicht passiert ist.
        val bonus = PlayAfterglow.bonuses(
            listOf(PlayAfterglow.Answer(AnimationType.BOOK, MITTAGS + 60_000)),
            nowMillis = MITTAGS,
            dayStartMillis = TAGESBEGINN
        )
        assertTrue(bonus.isEmpty())
    }

    @Test
    fun `MEDICINE klingt nie nach`() {
        // Ueberall sonst in PlayAmbientActivity ausgeschlossen: Medikamente sind nichts, was ein
        // Wesen von sich aus tut - und ein Nachklang waere genau das.
        val bonus = PlayAfterglow.bonuses(
            listOf(antwort(AnimationType.MEDICINE, vorMillis = 1_000)),
            nowMillis = MITTAGS,
            dayStartMillis = TAGESBEGINN
        )
        assertTrue(bonus.isEmpty())
    }

    @Test
    fun `ohne Antworten bleibt die Gewichtung unberuehrt`() {
        assertTrue(PlayAfterglow.bonuses(emptyList(), MITTAGS, TAGESBEGINN).isEmpty())
    }

    // ================= Mehrere Antworten =================

    @Test
    fun `zwei Antworten wiegen mehr als eine`() {
        val eine = PlayAfterglow.bonuses(
            listOf(antwort(AnimationType.BOOK, 2L * PlayAfterglow.ECHO_MS)),
            MITTAGS, TAGESBEGINN
        )
        val zwei = PlayAfterglow.bonuses(
            listOf(
                antwort(AnimationType.BOOK, 2L * PlayAfterglow.ECHO_MS),
                antwort(AnimationType.BOOK, 3L * PlayAfterglow.ECHO_MS)
            ),
            MITTAGS, TAGESBEGINN
        )
        assertTrue(zwei.getValue(AnimationType.BOOK) > eine.getValue(AnimationType.BOOK))
    }

    @Test
    fun `der Deckel verhindert, dass ein Thema den Tag uebernimmt`() {
        val viele = List(10) { antwort(AnimationType.BOOK, 60_000L + it) }
        val bonus = PlayAfterglow.bonuses(viele, MITTAGS, TAGESBEGINN)
        assertEquals(PlayAfterglow.MAX_BONUS, bonus[AnimationType.BOOK])
    }

    @Test
    fun `verschiedene Themen klingen unabhaengig voneinander nach`() {
        val bonus = PlayAfterglow.bonuses(
            listOf(
                antwort(AnimationType.BOOK, 30_000),
                antwort(AnimationType.DRINK, 5L * PlayAfterglow.ECHO_MS)
            ),
            MITTAGS, TAGESBEGINN
        )
        assertEquals(PlayAfterglow.ECHO_BONUS, bonus[AnimationType.BOOK])
        assertEquals(PlayAfterglow.DAY_BONUS, bonus[AnimationType.DRINK])
    }

    // ================= isEchoing =================

    @Test
    fun `isEchoing erkennt den frischen Nachklang und sein Ende`() {
        val frisch = listOf(antwort(AnimationType.BOOK, 1_000))
        val alt = listOf(antwort(AnimationType.BOOK, PlayAfterglow.ECHO_MS))
        assertTrue(PlayAfterglow.isEchoing(frisch, MITTAGS))
        assertFalse(PlayAfterglow.isEchoing(alt, MITTAGS))
        assertFalse(PlayAfterglow.isEchoing(emptyList(), MITTAGS))
    }

    // ================= Der Nachklang einer wirklichen Begegnung (NT-085) =================

    @Test
    fun `eine angenommene Einladung klingt als LOVE nach`() {
        val antwort = PlayAfterglow.fromVisit(
            setOf(SymbolicIntent.PLAY, SymbolicIntent.YES), atMillis = MITTAGS
        )
        assertEquals(PlayAfterglow.Answer(AnimationType.LOVE, MITTAGS), antwort)
    }

    @Test
    fun `eine muede Absage klingt nicht nach`() {
        assertNull(PlayAfterglow.fromVisit(setOf(SymbolicIntent.TIRED, SymbolicIntent.NO), MITTAGS))
    }

    @Test
    fun `eine gewoehnliche Absage klingt nicht nach`() {
        assertNull(PlayAfterglow.fromVisit(setOf(SymbolicIntent.PLAY, SymbolicIntent.NO), MITTAGS))
    }

    @Test
    fun `der Besuchsnachklang macht LOVE abends deutlich haeufiger`() {
        // LOVE hat nur abends ein Grundgewicht (siehe PlayAmbientActivity.weightsFor) - derselbe
        // Zurueckhaltungsgrundsatz wie beim Erinnerungs-Nachklang gilt auch hier: Der Bonus darf
        // nur verstaerken, was zur Tageszeit ohnehin vorkommt, nie ein Thema einfuehren.
        val besuch = PlayAfterglow.fromVisit(setOf(SymbolicIntent.PLAY, SymbolicIntent.YES), MITTAGS)
        checkNotNull(besuch)
        val bonus = PlayAfterglow.bonuses(listOf(besuch), nowMillis = MITTAGS, dayStartMillis = TAGESBEGINN)
        val random = Random(20260914)
        fun anteilLoveAbends(afterglow: Map<AnimationType, Int>): Double {
            var treffer = 0
            val laeufe = 20_000
            repeat(laeufe) {
                val gewaehlt = PlayAmbientActivity.nextTopic(
                    phase = PlayAmbientActivity.DayPhase.EVENING,
                    afterglow = afterglow,
                    random = random
                )
                if (gewaehlt == AnimationType.LOVE) treffer++
            }
            return treffer.toDouble() / laeufe
        }
        val ohne = anteilLoveAbends(emptyMap())
        val mit = anteilLoveAbends(bonus)
        assertTrue("ohne=$ohne mit=$mit", mit > ohne * 2)
    }

    // ================= Die Wirkung auf die Themenwahl =================
    //
    // Die Zuschlaege oben sind nur Zahlen, solange nicht belegt ist, dass sie das Bild aendern.

    private fun anteil(topic: AnimationType, afterglow: Map<AnimationType, Int>): Double {
        val random = Random(20260906)
        var treffer = 0
        val laeufe = 20_000
        repeat(laeufe) {
            val gewaehlt = PlayAmbientActivity.nextTopic(
                phase = PlayAmbientActivity.DayPhase.MIDDAY,
                afterglow = afterglow,
                random = random
            )
            if (gewaehlt == topic) treffer++
        }
        return treffer.toDouble() / laeufe
    }

    @Test
    fun `der frische Nachklang macht das Thema deutlich haeufiger`() {
        val ohne = anteil(AnimationType.BOOK, emptyMap())
        val mit = anteil(AnimationType.BOOK, mapOf(AnimationType.BOOK to PlayAfterglow.ECHO_BONUS))
        // BOOK hat mittags Grundgewicht 1 von 16; mit +5 sind es 6 von 21. Der Sprung ist keine
        // Feinheit - genau das ist gemeint mit "mehr als nur kurz reagieren".
        assertTrue("ohne=$ohne mit=$mit", mit > ohne * 3)
    }

    @Test
    fun `die Tagesfarbe ist spuerbar, aber uebernimmt nicht`() {
        val ohne = anteil(AnimationType.DRINK, emptyMap())
        val mit = anteil(AnimationType.DRINK, mapOf(AnimationType.DRINK to PlayAfterglow.DAY_BONUS))
        assertTrue("ohne=$ohne mit=$mit", mit > ohne)
        // Und nicht mehr: Ein Tag, an dem das Wesen nur noch trinkt, waere kein Tag.
        assertTrue("mit=$mit", mit < 0.5)
    }

    @Test
    fun `nachts holt kein Nachklang das Wesen aus dem Bett`() {
        // Die wichtigste Grenze. Die Nachtruhe kennt nur SLEEP; eine Garantie, die sich durch
        // eine Nutzerhandlung vom Nachmittag aushebeln laesst, ist keine.
        val random = Random(1)
        repeat(2_000) {
            val gewaehlt = PlayAmbientActivity.nextTopic(
                phase = PlayAmbientActivity.DayPhase.NIGHT,
                afterglow = mapOf(
                    AnimationType.BOOK to PlayAfterglow.MAX_BONUS,
                    AnimationType.MOVE to PlayAfterglow.MAX_BONUS
                ),
                random = random
            )
            assertEquals(AnimationType.SLEEP, gewaehlt)
        }
    }
}
