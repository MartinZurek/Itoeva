package com.notime.glyphsim.ui

import com.notime.glyphcore.data.AnimationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Die drei Entscheidungen, die darueber entscheiden, ob die Entwicklung Begleitung ist oder
 * Bevormundung** (siehe [PlayPath]) - und alle drei lassen sich nur rechnerisch pruefen: Am Geraet
 * muesste man dafuer wochenlang unterschiedlich fuettern.
 */
class PlayPathTest {

    private fun counts(vararg pairs: Pair<AnimationType, Int>) = pairs.toMap()

    @Test
    fun `ohne Grundlage benennt er keinen Pfad`() {
        // Ein Wesen, das nach zwei Fuetterungen verkuendet, es werde jetzt "der Stille", behauptet
        // etwas ueber jemanden, den es nicht kennt - und der Nutzer merkt sofort, dass nichts
        // dahintersteckt.
        val thin = counts(AnimationType.MOVE to 5)
        assertNull(PlayPath.pathFor(thin))
        assertTrue("Zu wenig gilt faelschlich als genug", !PlayPath.enoughData(thin))
    }

    @Test
    fun `wer ueberwiegend eines tut, bekommt den passenden Pfad`() {
        val moving = counts(
            AnimationType.MOVE to 20, AnimationType.WORK to 6,
            AnimationType.DRINK to 4, AnimationType.BOOK to 2
        )
        assertEquals(PlayPath.WANDERER, PlayPath.pathFor(moving))

        val calm = counts(
            AnimationType.MINDFULNESS to 12, AnimationType.BOOK to 10,
            AnimationType.REST to 5, AnimationType.MOVE to 3
        )
        assertEquals(PlayPath.DREAMER, PlayPath.pathFor(calm))
    }

    @Test
    fun `wer von allem etwas tut, bleibt vielseitig`() {
        // **Das ist ein Ergebnis, kein Mangel.** Irgendeinen Pfad zuzuweisen, weil ein Thema
        // zufaellig zwei Antworten vorn liegt, waere geraten - und der Nutzer merkt es, sobald es
        // beim naechsten Mal ein anderer ist.
        val even = counts(
            AnimationType.MOVE to 7, AnimationType.DRINK to 7,
            AnimationType.BOOK to 7, AnimationType.FOCUS to 7
        )
        assertTrue("Zu wenig Daten statt Gleichstand", PlayPath.enoughData(even))
        assertNull("Bei Gleichstand wird geraten", PlayPath.pathFor(even))
    }

    @Test
    fun `ein knapper Vorsprung genuegt nicht`() {
        // Unterhalb einer klaren Mehrheit ist es keine Neigung, sondern Rauschen.
        val narrow = counts(
            AnimationType.MOVE to 9, AnimationType.DRINK to 8,
            AnimationType.BOOK to 8, AnimationType.FOCUS to 5
        )
        assertNull(PlayPath.pathFor(narrow))
    }

    @Test
    fun `der Anteil ist nachvollziehbar`() {
        // Ohne diese Zahl waere die Entwicklung eine Zuschreibung von aussen. Mit ihr ist sie
        // nachvollziehbar - und damit beeinflussbar, worum es hier geht.
        val counts = counts(AnimationType.MOVE to 15, AnimationType.WORK to 5, AnimationType.BOOK to 5)
        assertEquals(0.8f, PlayPath.shareOf(counts, PlayPath.WANDERER), 0.01f)
        assertEquals(0.2f, PlayPath.shareOf(counts, PlayPath.DREAMER), 0.01f)
    }

    @Test
    fun `jedes Thema zahlt auf genau einen Pfad ein`() {
        // Sonst waeren die Anteile nicht mehr vergleichbar: Ein Thema, das auf zwei Pfade
        // einzahlt, verschoebe stillschweigend beide, und die Summe ergaebe mehr als hundert
        // Prozent.
        for (topic in AnimationType.entries) {
            val hits = PlayPath.entries.count { topic in it.topics }
            assertEquals("$topic zahlt auf $hits Pfade ein", 1, hits)
        }
    }

    @Test
    fun `die Stufen steigen mit der Erfahrung und enden`() {
        assertEquals(0, PlayPath.stageFor(1))
        assertEquals(1, PlayPath.stageFor(2))
        assertEquals(2, PlayPath.stageFor(5))
        assertEquals(3, PlayPath.stageFor(9))
        assertEquals(3, PlayPath.stageFor(40))
        assertNull("Nach der letzten Stufe wird nichts mehr versprochen", PlayPath.nextStageLevel(9))
        assertEquals(2, PlayPath.nextStageLevel(1))
    }

    @Test
    fun `jede Stufe jedes Pfades hat etwas zu sagen`() {
        // Der Sinn der Sache: Nicht eine Zahl wird groesser, sondern das Wesen bekommt etwas zu
        // sagen, das es vorher nicht sagen konnte. Eine leere Stufe waere ein Aufstieg ins Nichts.
        for (path in PlayPath.entries) {
            assertNull("Stufe 0 darf nichts erzaehlen", PlayPath.loreOf(path, 0))
            for (stage in 1..3) {
                val text = PlayPath.loreOf(path, stage)
                assertTrue("$path hat auf Stufe $stage nichts zu sagen", text != null && text != 0)
            }
        }
        // Und kein Text wird von zwei Stellen benutzt.
        val all = PlayPath.entries.flatMap { path -> (1..3).mapNotNull { PlayPath.loreOf(path, it) } }
        assertEquals("Ein Text wird zweimal verwendet", all.size, all.toSet().size)
    }
}
