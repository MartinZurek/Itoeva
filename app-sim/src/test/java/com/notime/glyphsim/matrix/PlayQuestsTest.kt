package com.notime.glyphsim.matrix

import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.matrix.PlayQuests.DayKind
import com.notime.glyphsim.matrix.PlayQuests.Next
import com.notime.glyphsim.matrix.PlayQuests.Progress
import com.notime.glyphsim.matrix.PlayQuests.Quest
import com.notime.glyphsim.matrix.PlayQuests.Reward
import com.notime.glyphsim.matrix.PlayQuests.StepKind
import com.notime.glyphsim.matrix.PlayScene.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prueft die Reisen: Tagesrhythmus, Stationen ueber Stunden, Nachholen, Belohnungen, Drachenei. */
class PlayQuestsTest {

    private fun at(h: Int, m: Int = 0) = h * 60 + m

    /** Ein Reisetag im Rhythmus zwei Reisetage, ein Tag daheim. */
    private val reisetag = 0L
    private val otherThanDragon = setOf(PlayScene.Acquisition.TREASURE_CHEST, PlayScene.Acquisition.MAGIC_WAND)

    /** Spielt einen Tag durch, alle zehn Minuten von [from] bis [until] (Minute des Questtags). */
    private fun liveDay(start: Progress, day: Long, from: Int = at(6), until: Int = at(25, 50)): Progress {
        var p = PlayQuests.rollTo(start, day)
        var m = from
        while (m <= until) {
            while (true) {
                p = when (val n = PlayQuests.next(p, m % (24 * 60)) ?: break) {
                    is Next.Run -> PlayQuests.completed(p, n.step)
                    Next.Skip -> PlayQuests.skipped(p)
                }
            }
            m += 10
        }
        return p
    }

    private fun liveDays(count: Long, start: Progress = Progress()): List<Progress> {
        var p = start
        return (0 until count).map { day -> liveDay(p, day).also { p = it } }
    }

    @Test
    fun `zwei Reisetage, dann ein Tag daheim`() {
        assertEquals(
            listOf(DayKind.TRAVEL, DayKind.TRAVEL, DayKind.HOME, DayKind.TRAVEL, DayKind.TRAVEL, DayKind.HOME),
            (0L until 6L).map { PlayQuests.dayKind(it) }
        )
    }

    @Test
    fun `jede Reise dauert den ganzen Tag, mit Stationen ueber Stunden`() {
        for (quest in Quest.entries) {
            val days = PlayQuests.daysOf(quest)
            var erlebnisse = 0
            for (d in 0 until days) {
                val plan = PlayQuests.planFor(
                    Progress(quest = quest, questDayNumber = reisetag, journeyDay = d, departed = d > 0)
                )
                val kinds = plan.steps.map { it.kind }
                assertEquals("$quest Tag $d", if (d == 0) StepKind.DEPART else StepKind.BREAK_CAMP, kinds.first())
                // Der letzte Tag endet daheim, die anderen im Lager.
                assertEquals("$quest Tag $d", d == days - 1, StepKind.RETURN in kinds)
                assertEquals("$quest Tag $d", d < days - 1, StepKind.CAMP in kinds)
                erlebnisse += kinds.count { it == StepKind.EXPERIENCE }
                val unterwegs = plan.endMinute!! - plan.steps.first().atMinute
                assertTrue("$quest Tag $d ist nur ${unterwegs / 60} Stunden unterwegs", unterwegs >= 8 * 60)
                // Jede Station unterwegs (nicht das Lager) traegt mindestens anderthalb Stunden.
                val wege = plan.stations.filter { it.place != Place.CAMP }
                val grenzen = wege.map { it.fromMinute } + plan.endMinute!!
                grenzen.zipWithNext().forEach { (a, b) -> assertTrue("$quest Tag $d: Station zu kurz", b - a >= 90) }
                // Jeder Moment spielt an der Station, an der das Wesen zu dieser Zeit ist.
                for (step in plan.steps.filter { it.kind == StepKind.EXPERIENCE }) {
                    val ort = plan.stations.last { it.fromMinute <= step.atMinute }.place
                    val ziel = step.routine.steps.filterIsInstance<RoutineStep.GoToPlace>().first().place
                    assertEquals("$quest Tag $d um ${step.atMinute / 60}:${step.atMinute % 60}", ort, ziel)
                }
            }
            assertTrue("$quest: Erlebnisse unterwegs", erlebnisse >= 4)
        }
        // Zwei Reisen dauern mehrere Tage.
        assertEquals(2, PlayQuests.daysOf(Quest.EXPEDITION))
        assertEquals(3, PlayQuests.daysOf(Quest.DRAGON_EGG))
    }

    @Test
    fun `auf einer mehrtaegigen Reise schlaeft das Wesen draussen im Lager`() {
        // Expedition, erster Tag: abends das Lager, und bis zum Abbruch am Morgen bleibt es dort.
        val abends = liveDay(Progress(quest = Quest.EXPEDITION), reisetag)
        assertFalse(abends.journeyDone)
        assertEquals(Place.CAMP, PlayQuests.stationAt(abends, at(23))?.place)
        assertEquals(Place.CAMP, PlayQuests.stationAt(abends, at(1, 30))?.place)
        // Der naechste Morgen: draussen aufgewacht, noch im Lager, dann weiter.
        val morgen = PlayQuests.rollTo(abends, reisetag + 1)
        assertEquals(1, morgen.journeyDay)
        assertTrue(morgen.departed)
        assertEquals(Place.CAMP, PlayQuests.stationAt(morgen, at(7))?.place)
        assertEquals(StepKind.BREAK_CAMP, (PlayQuests.next(morgen, at(7, 30)) as Next.Run).step.kind)
        // ... und abends daheim.
        val heim = liveDay(abends, reisetag + 1)
        assertTrue(heim.journeyDone)
        assertTrue(Reward.GROTTO in heim.rewards)
        assertNull(PlayQuests.stationAt(heim, at(21)))
    }

    @Test
    fun `ein Reisetag nimmt das Wesen mit hinaus und bringt es heim`() {
        var p = PlayQuests.rollTo(Progress(), reisetag)
        assertNull("vor dem Aufbruch ist es daheim", PlayQuests.stationAt(p, at(7)))
        p = liveDay(Progress(), reisetag, until = at(12))
        assertEquals(Place.FOREST, PlayQuests.stationAt(p, at(12))?.place)
        p = liveDay(Progress(), reisetag)
        assertTrue(p.journeyDone)
        assertNull("nach der Heimkehr ist es wieder daheim", PlayQuests.stationAt(p, at(22)))
        assertTrue(Reward.TREASURE_CHEST in p.rewards)
    }

    @Test
    fun `wer spaet einschaltet, sieht den Rest der Reise`() {
        // Erst um 15 Uhr eingeschaltet: Aufbruch, die verpassten Momente fallen weg, das Wesen ist
        // dort, wo die Reise um diese Zeit ist.
        var p = PlayQuests.rollTo(Progress(), reisetag)
        val aufbruch = PlayQuests.next(p, at(15)) as Next.Run
        assertEquals(StepKind.DEPART, aufbruch.step.kind)
        p = PlayQuests.completed(p, aufbruch.step)
        var uebersprungen = 0
        while (PlayQuests.next(p, at(15)) == Next.Skip) { p = PlayQuests.skipped(p); uebersprungen++ }
        assertEquals("9:30, 12:00 und 13:15", 3, uebersprungen)
        assertEquals(Place.MOUNTAINS, PlayQuests.stationAt(p, at(15))?.place)
        assertEquals(StepKind.EXPERIENCE, (PlayQuests.next(p, at(15, 30)) as Next.Run).step.kind)
    }

    @Test
    fun `ein Moment mit Belohnung wird nie uebersprungen`() {
        // Expedition, zweiter Tag, erst um 18 Uhr eingeschaltet: die Entdeckung der Grotte kommt trotzdem.
        val draussen = Progress(quest = Quest.EXPEDITION, questDayNumber = reisetag, journeyDay = 1, departed = true)
        val p = liveDay(draussen, reisetag, from = at(18), until = at(18))
        assertTrue(Reward.GROTTO in p.rewards)
    }

    @Test
    fun `wer erst nach der Heimkehrzeit einschaltet, reist am naechsten Reisetag`() {
        val p = liveDay(Progress(), reisetag, from = at(22))
        assertFalse(p.departed)
        assertFalse(p.journeyDone)
        assertEquals(Quest.TREASURE, PlayQuests.rollTo(p, reisetag + 1).quest)
    }

    @Test
    fun `nachts ist nichts faellig`() {
        val p = PlayQuests.rollTo(Progress(), reisetag)
        assertNull(PlayQuests.next(p, at(3)))
        assertNull(PlayQuests.next(p, at(7)))
    }

    @Test
    fun `die Reisen folgen aufeinander, daheim wird nicht gereist`() {
        val tage = liveDays(10)
        assertEquals(
            listOf(
                Quest.TREASURE to 0, Quest.MAGIC to 0,
                Quest.EXPEDITION to 0, // daheim
                Quest.EXPEDITION to 0, Quest.EXPEDITION to 1,
                Quest.DRAGON_EGG to 0, // daheim
                Quest.DRAGON_EGG to 0, Quest.DRAGON_EGG to 1,
                Quest.DRAGON_EGG to 2 // eigentlich ein Tag daheim - aber wer draussen ist, kommt erst heim
            ),
            tage.take(9).map { it.quest to it.journeyDay }
        )
        assertFalse("daheim kein Aufbruch", tage[2].departed)
        assertTrue(tage[4].journeyDone && tage[8].journeyDone)
    }

    @Test
    fun `das Ei bekommt erst Risse, dann schluepft es - ueber Tage`() {
        // Am dritten Tag der Dracheneireise (Tag 8) kommt das Ei ins Nest; Tag 9: Risse; Tag 10: es schluepft.
        val tage = liveDays(11)
        assertEquals(setOf(PlayScene.Acquisition.DRAGON_EGG), PlayQuests.acquisitions(tage[8].rewards) - otherThanDragon)
        assertEquals(setOf(PlayScene.Acquisition.DRAGON_EGG_CRACKED), PlayQuests.acquisitions(tage[9].rewards) - otherThanDragon)
        assertEquals(setOf(PlayScene.Acquisition.DRAGONLING), PlayQuests.acquisitions(tage[10].rewards) - otherThanDragon)
    }

    @Test
    fun `belohnungen bleiben, und nach der ersten Runde kein zweites Drachenei`() {
        val tage = liveDays(21)
        val zuletzt = tage.last()
        assertTrue(Reward.TREASURE_CHEST in zuletzt.rewards)
        assertTrue(PlayQuests.canCastMagic(zuletzt.rewards))
        assertTrue(PlayQuests.grottoDiscovered(zuletzt.rewards))
        assertTrue(Reward.DRAGONLING in zuletzt.rewards)
        assertTrue(zuletzt.round >= 1)
        assertEquals(1, tage.count { it.quest == Quest.DRAGON_EGG && it.journeyDone })
    }

    @Test
    fun `unterwegs wird gegessen, gerastet und erkundet - ohne den Ort zu verlassen`() {
        for (ort in STATIONEN) for (roll in 0 until 12) for (regen in listOf(false, true)) {
            for ((hunger, muede) in listOf(0.0 to 0.0, 0.8 to 0.0, 0.0 to 0.8)) {
                val w = PlayQuests.wayside(ort, hunger, muede, regen, roll)
                assertTrue("$ort verlaesst die Station", w.routine.steps.none { it is RoutineStep.GoToPlace })
                assertTrue(w.relief.isNotEmpty())
                if (regen) assertTrue("$ort: Drachen im Regen", w.routine.steps.none { it is RoutineStep.Kite })
                if (hunger > 0.5) assertTrue(NeedKind.HUNGER in w.relief)
                if (hunger < 0.5 && muede > 0.5) assertTrue(NeedKind.ENERGY in w.relief)
            }
        }
        // Und es gibt an jeder Station mehr als eine Art, den Nachmittag zu verbringen.
        for (ort in STATIONEN) {
            val arten = (0 until 12).map { PlayQuests.wayside(ort, 0.0, 0.0, false, it).routine }.toSet()
            assertTrue("$ort: nur ${arten.size} Arten", arten.size >= 3)
        }
    }

    @Test
    fun `nachts wird im Lager geschlafen, tagsueber nicht`() {
        for (roll in 0 until 12) for (regen in listOf(false, true)) {
            val nacht = PlayQuests.wayside(Place.CAMP, 0.8, 0.0, regen, roll, night = true)
            assertTrue(RoutineStep.SleepUntilMorning in nacht.routine.steps)
            assertTrue(nacht.routine.steps.none { it is RoutineStep.GoToPlace })
            assertTrue(NeedKind.ENERGY in nacht.relief)
            val tag = PlayQuests.wayside(Place.CAMP, 0.0, 0.0, regen, roll)
            assertTrue(RoutineStep.SleepUntilMorning !in tag.routine.steps)
            // Nur das Lager ist ein Nachtlager - an einer anderen Station wird nicht geschlafen.
            val anderswo = PlayQuests.wayside(Place.MOUNTAINS, 0.0, 0.0, regen, roll, night = true)
            assertTrue(RoutineStep.SleepUntilMorning !in anderswo.routine.steps)
        }
    }

    @Test
    fun `weiterziehen fuehrt an die naechste Station`() {
        for (von in listOf(Place.LIVING, Place.PLAINS, Place.STREET)) for (nach in STATIONEN) {
            val ziel = PlayQuests.travel(von, nach).steps.filterIsInstance<RoutineStep.GoToPlace>().last().place
            assertEquals(nach, ziel)
        }
    }

    private val STATIONEN: Set<Place> = Quest.entries.flatMap {
        PlayQuests.planFor(Progress(quest = it, questDayNumber = reisetag)).stations.map { s -> s.place }
    }.toSet()

    @Test
    fun `auf jeder Reise gibt es eine Begegnung - an der Station, an der das Wesen gerade ist`() {
        for (quest in Quest.entries) {
            val plan = PlayQuests.planFor(Progress(quest = quest, questDayNumber = reisetag))
            val begegnungen = plan.steps.filter { it.kind == StepKind.ENCOUNTER }
            assertEquals("$quest", 1, begegnungen.size)
            val step = begegnungen.single()
            val ort = plan.stations.last { it.fromMinute <= step.atMinute }.place
            assertEquals(quest.name, ort, step.routine.steps.filterIsInstance<RoutineStep.GoToPlace>().single().place)
            // Die Figur bleibt lange genug stehen, dass jemand dazukommen kann.
            assertTrue(step.routine.steps.any { it is RoutineStep.Linger && it.millis >= 20_000L })
        }
    }

    @Test
    fun `was unterwegs gefunden wird, bleibt bis zur Heimkehr in der Hand`() {
        // Schatzsuche: erst die Karte, nach dem Fund die Truhe, daheim nichts mehr.
        assertNull(PlayQuests.carriedOnJourney(PlayQuests.rollTo(Progress(), reisetag)))
        assertEquals(PlayEffects.Carried.MAP, PlayQuests.carriedOnJourney(liveDay(Progress(), reisetag, until = at(12))))
        assertEquals(PlayEffects.Carried.CHEST, PlayQuests.carriedOnJourney(liveDay(Progress(), reisetag, until = at(20))))
        assertNull(PlayQuests.carriedOnJourney(liveDay(Progress(), reisetag)))
        // Drachenei: am zweiten Tag gefunden, dann bis zur Heimkehr am dritten Tag in der Hand.
        val tag1 = liveDay(Progress(quest = Quest.DRAGON_EGG), reisetag)
        assertNull(PlayQuests.carriedOnJourney(liveDay(tag1, reisetag + 1, until = at(14))))
        val tag2 = liveDay(tag1, reisetag + 1)
        assertEquals(PlayEffects.Carried.EGG, PlayQuests.carriedOnJourney(tag2))
        assertEquals(PlayEffects.Carried.EGG, PlayQuests.carriedOnJourney(liveDay(tag2, reisetag + 2, until = at(12))))
    }

    @Test
    fun `Regen haelt das Weiterziehen hoechstens eine Stunde auf`() {
        val p = liveDay(Progress(), reisetag, until = at(10, 50)) // Schatzsuche, noch auf der Ebene
        assertTrue(PlayQuests.holdsForWeather(p, at(11, 10), Place.PLAINS, falling = true))
        assertFalse("ohne Regen geht es weiter", PlayQuests.holdsForWeather(p, at(11, 10), Place.PLAINS, falling = false))
        assertFalse("nach einer Stunde geht es trotzdem weiter", PlayQuests.holdsForWeather(p, at(12, 5), Place.PLAINS, falling = true))
        assertFalse("nur an einer Station der Reise", PlayQuests.holdsForWeather(p, at(11, 10), Place.LIVING, falling = true))
    }

    @Test
    fun `wer schon etwas traegt, nimmt beim Rasten nichts anderes in die Hand`() {
        for (ort in STATIONEN) for (roll in 0 until 12) for (hunger in listOf(0.0, 0.8)) {
            val w = PlayQuests.wayside(ort, hunger, 0.0, rainy = false, roll = roll, holding = true)
            assertTrue("$ort", w.routine.steps.none { it is RoutineStep.Take || it == RoutineStep.Drop })
        }
        // Und bei Regen gibt es das Unterstellen.
        val beiRegen = (0 until 24).map { PlayQuests.wayside(Place.FOREST, 0.0, 0.0, rainy = true, roll = it).routine }
        assertTrue(beiRegen.any { r -> r.steps.any { it == RoutineStep.Stir(AvatarAnimations.Fidget.SHAKE) } })
    }

    @Test
    fun `der Questtag beginnt um sechs`() {
        val tag = 24L * 60L
        assertEquals(PlayQuests.questDayOf(5 * tag + 23 * 60), PlayQuests.questDayOf(6 * tag + 60))
        assertEquals(PlayQuests.questDayOf(5 * tag + 23 * 60) + 1, PlayQuests.questDayOf(6 * tag + 7 * 60))
    }

    @Test
    fun `der Stand uebersteht Ablegen und Laden`() {
        val stand = Progress(
            quest = Quest.DRAGON_EGG, questDayNumber = 20_123L, stepsDone = 3, departed = true,
            journeyDone = false, rewards = setOf(Reward.MAGIC, Reward.DRAGON_EGG), round = 0, eggFoundDay = 20_120L
        )
        assertEquals(stand, PlayQuests.decode(PlayQuests.encode(stand)))
        assertEquals(Progress(), PlayQuests.decode(null))
        assertEquals(Progress(), PlayQuests.decode("kaputt;1"))
        // Der Stand der ersten Fassung behaelt Quest, Belohnungen und Runde.
        val alt = PlayQuests.decode("MAGIC;0;20100;2;TREASURE_CHEST;0")
        assertEquals(Quest.MAGIC, alt.quest)
        assertEquals(setOf(Reward.TREASURE_CHEST), alt.rewards)
        assertEquals(Long.MIN_VALUE, alt.questDayNumber)
    }

    @Test
    fun questBilderSindNieLeerUndBleibenImBild() {
        for (effect in PlayQuestEffects.Effect.entries) {
            val alle = (0 until PlayQuestEffects.CYCLE).flatMap { PlayQuestEffects.cells(effect, 10, 12, it, 54) }
            assertTrue("$effect zeigt nie etwas", alle.isNotEmpty())
            assertTrue("$effect laeuft aus dem Bild", alle.all { it.x in 0 until 54 && it.y >= 0 })
        }
    }

    /**
     * Was eine Quest hinterlaesst, muss man sehen - bei jedem Wesen, jeder Bildbreite und neben
     * allem, was ein Entwicklungspfad schon ins Zimmer gestellt hat. Sonst faellt es in
     * PlayScene.fitting lautlos weg, und die Belohnung waere unsichtbar.
     */
    @Test
    fun `jede Quest-Belohnung ist zu Hause zu sehen`() {
        val fehlend = mutableListOf<String>()
        // Die vollen Saetze der vier Entwicklungspfade (PlayPath.acquisitionsUpTo(pfad, 3)) - hier
        // ausgeschrieben, weil PlayPath an Android haengt und offline nicht mitkompiliert wird.
        val pfade: List<Set<PlayScene.Acquisition>> = listOf(
            emptySet(),
            setOf(PlayScene.Acquisition.BACKPACK, PlayScene.Acquisition.WALLMAP, PlayScene.Acquisition.SCOOTER),
            setOf(PlayScene.Acquisition.FEEDBOWL, PlayScene.Acquisition.PETBASKET, PlayScene.Acquisition.SLEEPING_PET),
            setOf(PlayScene.Acquisition.BLANKET, PlayScene.Acquisition.MOBILE, PlayScene.Acquisition.STARJAR),
            setOf(PlayScene.Acquisition.TOOLBOX, PlayScene.Acquisition.CONTRAPTION, PlayScene.Acquisition.SIGNALRIG)
        )
        for (belohnung in PlayQuests.acquisitions(Reward.entries.toSet()) + PlayScene.Acquisition.DRAGON_EGG +
            PlayScene.Acquisition.DRAGON_EGG_CRACKED
        ) {
            for (breite in listOf(PlayScene.MIN_SCENE_CELLS, 46, 54, 64, 72)) {
                for (species in AvatarSpecies.entries) for (boden in listOf(24, 82)) for (pfad in pfade) {
                    val ohne = PlayScene.build(
                        belohnung.place, 0, breite, boden, PlayAmbientActivity.DayPhase.MIDDAY,
                        species = species, acquisitions = pfad
                    ).map { it.x to it.y }.toSet()
                    val mit = PlayScene.build(
                        belohnung.place, 0, breite, boden, PlayAmbientActivity.DayPhase.MIDDAY,
                        species = species, acquisitions = pfad + belohnung
                    ).map { it.x to it.y }.toSet()
                    if ((mit - ohne).size < 8) fehlend += "$belohnung/$species/$breite/${pfad.size}"
                }
            }
        }
        assertEquals(emptyList<String>(), fehlend.distinct().take(10))
    }
}
