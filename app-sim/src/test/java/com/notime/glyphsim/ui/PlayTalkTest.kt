package com.notime.glyphsim.ui

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.DaysOfWeekMask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft die Vorgaben, mit denen aus einem Gespraech heraus eine Gewohnheit entsteht.
 *
 * **Warum das nicht nur Zahlen sind.** Wer im Gespraech "richte es ein" antippt, bekommt keine
 * Maske zum Nachjustieren - er bekommt eine fertige Erinnerung. Sitzt darin ein Tagesziel, das im
 * gewaehlten Zeitfenster gar nicht erreichbar ist, dann fordert die App etwas Unmoegliches, meldet
 * das nirgends und laesst den Nutzer taeglich scheitern. Das ist der unangenehmste Fehler, den
 * diese App ueberhaupt machen kann, und er faellt beim Lesen des Codes nicht auf: Alle vier Zahlen
 * sehen fuer sich vernuenftig aus, erst ihr Zusammenspiel entscheidet.
 */
class PlayTalkTest {

    @Test
    fun `jedes vorschlagbare Thema hat brauchbare Vorgaben`() {
        for (topic in PlayTalk.SUGGESTABLE) {
            val preset = PlayTalk.presetFor(topic)
            assertTrue(
                "$topic: Das Zeitfenster endet nicht nach seinem Anfang",
                preset.endMinuteOfDay > preset.startMinuteOfDay
            )
            assertTrue("$topic: Abstand muss positiv sein", preset.intervalMinutes > 0)
            assertTrue("$topic: ohne Tagesziel steuert es seinen Tag nicht", preset.dailyGoal > 0)
            assertTrue(
                "$topic: Zeitfenster liegt ausserhalb des Tages",
                preset.startMinuteOfDay >= 0 && preset.endMinuteOfDay <= 24 * 60
            )
        }
    }

    @Test
    fun `das Tagesziel ist im Zeitfenster ueberhaupt erreichbar`() {
        for (topic in PlayTalk.SUGGESTABLE) {
            val preset = PlayTalk.presetFor(topic)
            // Anzahl der Anstupser: Beim Fensterbeginn einer, danach alle intervalMinutes einer.
            val slots = (preset.endMinuteOfDay - preset.startMinuteOfDay) / preset.intervalMinutes + 1
            assertTrue(
                "$topic verlangt ${preset.dailyGoal} am Tag, kommt im Fenster aber nur $slots mal - " +
                    "das waere ein Ziel, das sich beim besten Willen nicht erreichen laesst.",
                preset.dailyGoal <= slots
            )
        }
    }

    @Test
    fun `Vorgaben lassen Luft, statt den Tag vollzustellen`() {
        // Die Gegenprobe zum Test darueber: Ein Ziel, das GENAU der Zahl der Anstupser entspricht,
        // waere zwar erreichbar, verlangte aber, dass wirklich jeder einzelne befolgt wird. Diese
        // App soll begleiten und nicht treiben - deshalb hoechstens drei Viertel.
        for (topic in PlayTalk.SUGGESTABLE) {
            val preset = PlayTalk.presetFor(topic)
            val slots = (preset.endMinuteOfDay - preset.startMinuteOfDay) / preset.intervalMinutes + 1
            assertTrue(
                "$topic: ${preset.dailyGoal} von $slots moeglichen ist zu eng getaktet",
                preset.dailyGoal <= slots * 3 / 4
            )
        }
    }

    @Test
    fun `eine neue Gewohnheit gilt an allen sieben Tagen`() {
        assertEquals(7, DaysOfWeekMask.toSet(PlayTalk.EVERY_DAY_MASK).size)
    }

    @Test
    fun `vorgeschlagen wird nur, was noch fehlt - und hoechstens eines`() {
        val nothingMissing = PlayTalk.Knowledge(
            plan = emptyList(), fedToday = 0, steering = emptySet(), missing = emptyList()
        )
        assertNull(
            "Ohne Luecke darf er nichts vorschlagen - sonst wird aus dem Angebot eine Ermahnung",
            PlayTalk.nextSuggestion(nothingMissing)
        )

        val twoMissing = PlayTalk.Knowledge(
            plan = emptyList(), fedToday = 0, steering = emptySet(),
            missing = listOf(AnimationType.MOVE, AnimationType.DRINK)
        )
        assertEquals(AnimationType.MOVE, PlayTalk.nextSuggestion(twoMissing))
    }

    @Test
    fun `vorgeschlagen wird nur, was der Avatar auch sichtbar tut`() {
        // Ein Vorschlag, der seinen Tagesablauf nicht veraendert, waere eine leere Zusage: Der
        // Nutzer legt etwas an, weil der Begleiter danach gefragt hat, und sieht anschliessend
        // keinen Unterschied. Jedes vorschlagbare Thema muss deshalb einen eigenen Ablauf haben.
        for (topic in PlayTalk.SUGGESTABLE) {
            val routines = com.notime.glyphsim.matrix.PlayRoutines.allFor(topic)
            assertTrue("$topic hat keinen Tagesablauf", routines.isNotEmpty())
            assertTrue(
                "$topic fuehrt zu keinem Ablauf, in dem sich etwas bewegt",
                routines.any { it.steps.size > 1 }
            )
        }
    }
}
