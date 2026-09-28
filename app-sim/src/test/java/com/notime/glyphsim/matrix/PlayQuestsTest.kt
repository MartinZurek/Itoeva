package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayQuests.Progress
import com.notime.glyphsim.matrix.PlayQuests.Quest
import com.notime.glyphsim.matrix.PlayQuests.Reward
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prueft die Questketten: Stufen ueber den Tag, Tageswechsel, Belohnungen, Drachenei. */
class PlayQuestsTest {

    private fun at(h: Int, m: Int = 0) = h * 60 + m

    /** Spielt einen ganzen Questtag durch: alle faelligen Stufen bis Mitternacht. */
    private fun liveDay(start: Progress, day: Long): Progress {
        var p = PlayQuests.rollTo(start, day)
        for (minute in listOf(at(9), at(10), at(14), at(15), at(20), at(21), at(23))) {
            PlayQuests.due(p, minute)?.let { p = PlayQuests.completed(p, it) }
        }
        return p
    }

    @Test
    fun jedeQuestHatJeTagDreiStufen() {
        for (q in Quest.entries) for (d in 0 until q.days) {
            val stages = PlayQuests.stagesFor(q, d)
            assertEquals(3, stages.size)
            assertEquals(PlayQuests.STAGE_HOURS, stages.map { it.fromHour })
            assertTrue(stages.all { it.routine.steps.isNotEmpty() })
        }
    }

    @Test
    fun jedeStufeZeigtEtwas() {
        // Keine Stufe ohne ein Quest-Bild, einen getragenen Gegenstand oder einen neuen Ort.
        for (q in Quest.entries) for (d in 0 until q.days) for (s in PlayQuests.stagesFor(q, d)) {
            assertTrue("$q Tag $d Stufe ${s.index}", s.routine.steps.any {
                it is RoutineStep.Quest && it.effect != null || it is RoutineStep.Take || it is RoutineStep.GoToPlace
            })
        }
    }

    @Test
    fun stufenWerdenNachUhrzeitFaellig() {
        val p = PlayQuests.rollTo(Progress(), 100)
        assertNull(PlayQuests.due(p, at(7)))
        assertEquals(0, PlayQuests.due(p, at(9))!!.index)
        val nachErster = PlayQuests.completed(p, PlayQuests.due(p, at(9))!!)
        assertNull("die zweite Stufe erst ab 14 Uhr", PlayQuests.due(nachErster, at(12)))
        assertEquals(1, PlayQuests.due(nachErster, at(14))!!.index)
    }

    @Test
    fun spaetAbendsIstNochZeitBisZwei() {
        var p = PlayQuests.rollTo(Progress(), 100)
        repeat(2) { p = PlayQuests.completed(p, PlayQuests.due(p, at(21))!!) }
        assertNotNull(PlayQuests.due(p, at(1, 30)))
        assertNull(PlayQuests.due(p, at(3)))
    }

    @Test
    fun jedenTagEineNeueQuestInFesterReihenfolge() {
        var p = Progress()
        val gesehen = mutableListOf<Quest>()
        for (day in 0L until 6L) {
            p = liveDay(p, day)
            gesehen += p.quest
        }
        assertEquals(
            listOf(Quest.TREASURE, Quest.MAGIC, Quest.EXPEDITION, Quest.DRAGON_EGG, Quest.DRAGON_EGG, Quest.DRAGON_EGG),
            gesehen
        )
    }

    @Test
    fun eineAngefangeneGeschichteWirdNichtUebersprungen() {
        var p = PlayQuests.rollTo(Progress(), 0)
        p = PlayQuests.completed(p, PlayQuests.due(p, at(9))!!)
        p = PlayQuests.rollTo(p, 1)
        assertEquals(Quest.TREASURE, p.quest)
        assertEquals(1, p.stagesDone)
    }

    @Test
    fun belohnungenBleibenUndStehenZuHause() {
        var p = Progress()
        for (day in 0L until 6L) p = liveDay(p, day)
        assertTrue(Reward.TREASURE_CHEST in p.rewards)
        assertTrue(PlayQuests.canCastMagic(p.rewards))
        assertTrue(PlayQuests.grottoDiscovered(p.rewards))
        assertTrue(Reward.DRAGONLING in p.rewards)
        val zuHause = PlayQuests.acquisitions(p.rewards)
        assertTrue(PlayScene.Acquisition.TREASURE_CHEST in zuHause)
        assertTrue(PlayScene.Acquisition.MAGIC_WAND in zuHause)
        // Vom Drachen nur der juengste Stand.
        assertTrue(PlayScene.Acquisition.DRAGONLING in zuHause)
        assertFalse(PlayScene.Acquisition.DRAGON_EGG in zuHause)
        assertFalse(PlayScene.Acquisition.DRAGON_EGG_CRACKED in zuHause)
    }

    @Test
    fun dasEiBekommtErstRisseDannSchluepftEs() {
        var p = Progress(quest = Quest.DRAGON_EGG)
        p = liveDay(p, 0)
        assertEquals(setOf(PlayScene.Acquisition.DRAGON_EGG), PlayQuests.acquisitions(p.rewards))
        p = liveDay(p, 1)
        assertEquals(setOf(PlayScene.Acquisition.DRAGON_EGG_CRACKED), PlayQuests.acquisitions(p.rewards))
        p = liveDay(p, 2)
        assertEquals(setOf(PlayScene.Acquisition.DRAGONLING), PlayQuests.acquisitions(p.rewards))
    }

    @Test
    fun nachDerErstenRundeKeinZweitesDrachenei() {
        var p = Progress()
        val quests = mutableListOf<Quest>()
        for (day in 0L until 14L) {
            p = liveDay(p, day)
            quests += p.quest
        }
        assertEquals(1, quests.count { it == Quest.DRAGON_EGG } / Quest.DRAGON_EGG.days)
        assertTrue(p.round >= 1)
    }

    @Test
    fun derQuesttagBeginntUmSechs() {
        val tag = 24L * 60L
        assertEquals(PlayQuests.questDayOf(5 * tag + 23 * 60), PlayQuests.questDayOf(6 * tag + 60))
        assertEquals(PlayQuests.questDayOf(5 * tag + 23 * 60) + 1, PlayQuests.questDayOf(6 * tag + 7 * 60))
    }

    @Test
    fun eineFremdeStufeAendertNichts() {
        val p = PlayQuests.rollTo(Progress(), 0)
        val fremd = PlayQuests.stagesFor(Quest.MAGIC, 0)[0]
        assertEquals(p, PlayQuests.completed(p, fremd))
    }

    @Test
    fun questBilderSindNieLeerUndBleibenImBild() {
        for (effect in PlayQuestEffects.Effect.entries) {
            val alle = (0 until PlayQuestEffects.CYCLE).flatMap { PlayQuestEffects.cells(effect, 10, 12, it, 54) }
            assertTrue("$effect zeigt nie etwas", alle.isNotEmpty())
            assertTrue("$effect laeuft aus dem Bild", alle.all { it.x in 0 until 54 && it.y >= 0 })
        }
    }
}
