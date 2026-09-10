package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.living.AgentState
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.LivingSite
import com.notime.glyphsim.living.LivingEventKind
import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.living.Needs
import com.notime.glyphsim.living.Personality
import com.notime.glyphsim.living.WorldState
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Belegt die eine Bruecke zwischen Kernentscheidung und vorhandener Choreografie. */
class LivingRuntimeAdapterTest {

    private val allOpen = LivingSite.entries.toSet()

    private fun world(
        coins: Int = 2,
        portions: Int = 3,
        site: LivingSite = LivingSite.HOME,
        minute: Int = 9 * 60
    ) = WorldState(0, minute, site, coins, portions, allOpen)

    private fun hungry(profile: String = "PUFFLING") = AgentState(
        profileId = profile,
        personality = Personality(),
        needs = Needs.of(NeedKind.HUNGER to 0.95)
    )

    @Test
    fun `alle sichtbaren Orte haben genau eine Domaenenbedeutung`() {
        val grouped = PlayScene.Place.entries.groupBy(LivingRuntimeAdapter::siteFor)

        assertEquals(setOf(PlayScene.Place.WORK), grouped.getValue(LivingSite.WORKPLACE).toSet())
        assertEquals(setOf(PlayScene.Place.SHOP), grouped.getValue(LivingSite.MARKET).toSet())
        assertEquals(
            setOf(
                PlayScene.Place.PARK,
                PlayScene.Place.SPORT,
                PlayScene.Place.POND,
                PlayScene.Place.STREET,
                PlayScene.Place.FOREST,
                PlayScene.Place.MEADOW,
                PlayScene.Place.CITY
            ),
            grouped.getValue(LivingSite.OUTSIDE).toSet()
        )
        assertEquals(PlayScene.Place.entries.size, grouped.values.sumOf { it.size })
    }

    @Test
    fun `Arbeit und Laden folgen der Simulationszeit`() {
        assertEquals(setOf(LivingSite.HOME, LivingSite.OUTSIDE), LivingRuntimeAdapter.openSitesAt(5 * 60))
        assertTrue(LivingSite.WORKPLACE in LivingRuntimeAdapter.openSitesAt(6 * 60))
        assertFalse(LivingSite.MARKET in LivingRuntimeAdapter.openSitesAt(6 * 60))
        assertTrue(LivingSite.MARKET in LivingRuntimeAdapter.openSitesAt(7 * 60))
        assertFalse(LivingSite.WORKPLACE in LivingRuntimeAdapter.openSitesAt(18 * 60))
        assertFalse(LivingSite.MARKET in LivingRuntimeAdapter.openSitesAt(22 * 60))
    }

    @Test
    fun `Zeitrafferminute behaelt volle simulierte Tage`() {
        val realDay = 20_000L

        val start = PlayTimeLapse.absoluteMinuteFor(
            realDay,
            realMinuteOfDay = 12 * 60,
            speed = PlayTimeLapse.Speed.TURBO,
            elapsedSeconds = 0.0
        )
        val oneDayLater = PlayTimeLapse.absoluteMinuteFor(
            realDay,
            realMinuteOfDay = 12 * 60,
            speed = PlayTimeLapse.Speed.TURBO,
            elapsedSeconds = PlayTimeLapse.Speed.TURBO.daySeconds
        )

        assertEquals(WorldState.MINUTES_PER_DAY, oneDayLater - start)
    }

    @Test
    fun `bestehende Wirtschaft wird beim ersten Anschluss uebernommen`() {
        val initial = LivingRuntimeAdapter.initialWorld(
            absoluteMinute = WorldState.MINUTES_PER_DAY + 8 * 60,
            place = PlayScene.Place.KITCHEN,
            coins = 7,
            portions = 2
        )

        assertEquals(1, initial.day)
        assertEquals(8 * 60, initial.minuteOfDay)
        assertEquals(LivingSite.HOME, initial.site)
        assertEquals(7, initial.coins)
        assertEquals(2, initial.portions)
    }

    @Test
    fun `pleite und hungrig wird sichtbar erst Arbeit und dann Einkauf`() {
        val first = LivingRuntimeAdapter.prepare(
            agent = hungry(),
            world = world(coins = 0, portions = 0),
            renderedPlace = PlayScene.Place.KITCHEN,
            interestTopic = AnimationType.CREATIVITY,
            random = Random(4)
        )

        assertEquals(listOf(ActionKind.TRAVEL, ActionKind.WORK), first.completedActions)
        assertEquals(AnimationType.WORK, first.topic)
        assertEquals(2, first.result.world.coins)
        assertTrue(first.routine!!.steps.any {
            it is RoutineStep.GoToPlace && it.place == PlayScene.Place.WORK
        })
        assertFalse(first.routine.steps.any {
            it is RoutineStep.GoToPlace && it.place == PlayScene.Place.SHOP
        })

        val second = LivingRuntimeAdapter.prepare(
            agent = first.result.agent,
            world = first.result.world,
            renderedPlace = PlayScene.Place.WORK,
            interestTopic = AnimationType.CREATIVITY,
            random = Random(4)
        )

        assertEquals(
            listOf(
                ActionKind.TRAVEL,
                ActionKind.BUY_FOOD,
                ActionKind.TRAVEL,
                ActionKind.INSPECT_FOOD,
                ActionKind.EAT
            ),
            second.completedActions
        )
        assertEquals(0, second.result.world.coins)
        assertEquals(2, second.result.world.portions)
        assertTrue(second.result.agent.needs.pressure(NeedKind.HUNGER) < 0.5)
        assertTrue(second.routine!!.steps.any {
            it is RoutineStep.GoToPlace && it.place == PlayScene.Place.SHOP
        })
        assertTrue(second.routine.steps.any {
            it is RoutineStep.GoTo && it.station == PlayScene.Station.CHECKOUT
        })
        assertTrue(second.routine.steps.any {
            it is RoutineStep.Act && it.topic == AnimationType.DRINK
        })
    }

    @Test
    fun `zusammengefasster Einkauf kauft nach Ladenschluss nicht weiter`() {
        val prepared = LivingRuntimeAdapter.prepare(
            agent = hungry(),
            world = world(coins = 2, portions = 0, minute = 21 * 60 + 50),
            renderedPlace = PlayScene.Place.KITCHEN,
            interestTopic = AnimationType.MOVE,
            random = Random(3)
        )

        assertEquals(listOf(ActionKind.TRAVEL), prepared.completedActions)
        assertEquals(2, prepared.result.world.coins)
        assertEquals(0, prepared.result.world.portions)
        assertTrue(prepared.result.events.any { it.kind == LivingEventKind.ACTION_BLOCKED })
        assertFalse(prepared.routine!!.steps.any {
            it is RoutineStep.GoTo && it.station == PlayScene.Station.CHECKOUT
        })
    }

    @Test
    fun `erbetene Arbeit und Einkauf verbuchen dieselbe Living-Wirtschaft`() {
        val startAgent = hungry().copy(goal = GoalKind.GET_FOOD)
        val startWorld = world(coins = 0, portions = 0)
        val workRoutine = PlayRoutines.forTopic(AnimationType.WORK, random = Random(2))

        val afterWork = LivingRuntimeAdapter.applyRequestedRoutine(
            startAgent,
            startWorld,
            PlayScene.Place.KITCHEN,
            AnimationType.WORK,
            workRoutine
        )
        assertEquals(2, afterWork.world.coins)
        assertEquals(startAgent.goal, afterWork.agent.goal)

        val shoppingRoutine = PlayRoutines.forTopic(
            AnimationType.DRINK,
            needsShopping = true,
            random = Random(2)
        )
        val afterShopping = LivingRuntimeAdapter.applyRequestedRoutine(
            afterWork.agent,
            afterWork.world,
            PlayScene.Place.WORK,
            AnimationType.DRINK,
            shoppingRoutine
        )

        assertEquals(0, afterShopping.world.coins)
        assertEquals(2, afterShopping.world.portions)
        assertTrue(afterShopping.agent.needs.pressure(NeedKind.HUNGER) < 0.5)
    }

    @Test
    fun `vorhandenes Essen nutzt den vorhandenen Kuechenablauf ohne Laden`() {
        val prepared = LivingRuntimeAdapter.prepare(
            agent = hungry(),
            world = world(coins = 0, portions = 2),
            renderedPlace = PlayScene.Place.LIVING,
            interestTopic = AnimationType.MOVE,
            random = Random(9)
        )

        assertEquals(listOf(ActionKind.INSPECT_FOOD, ActionKind.EAT), prepared.completedActions)
        assertEquals(1, prepared.result.world.portions)
        assertTrue(prepared.routine!!.steps.first() == RoutineStep.GoToPlace(PlayScene.Place.KITCHEN))
        assertFalse(prepared.routine.steps.any {
            it is RoutineStep.GoToPlace && it.place == PlayScene.Place.SHOP
        })
    }

    @Test
    fun `Freizeit bleibt vorhandene gewichtete Choreografie`() {
        val playful = AgentState(
            profileId = "STARLET",
            personality = Personality(),
            needs = Needs.of(NeedKind.FUN to 0.9)
        )
        val prepared = LivingRuntimeAdapter.prepare(
            agent = playful,
            world = world(),
            renderedPlace = PlayScene.Place.LIVING,
            interestTopic = AnimationType.CREATIVITY,
            random = Random(12)
        )

        assertEquals(listOf(ActionKind.PURSUE_INTEREST), prepared.completedActions)
        assertEquals(AnimationType.CREATIVITY, prepared.topic)
        assertNotNull(prepared.routine)
        assertTrue(prepared.routine!!.steps.any {
            it is RoutineStep.Act && it.topic == AnimationType.CREATIVITY ||
                it is RoutineStep.Music || it is RoutineStep.Painting
        })
    }

    @Test
    fun `ohne dringendes Beduerfnis wird keine Handlung erfunden`() {
        val prepared = LivingRuntimeAdapter.prepare(
            agent = AgentState("GLOOP", Personality(), Needs.calm()),
            world = world(),
            renderedPlace = PlayScene.Place.LIVING,
            interestTopic = AnimationType.MOVE,
            random = Random(1)
        )

        assertTrue(prepared.completedActions.isEmpty())
        assertNull(prepared.topic)
        assertNull(prepared.routine)
    }
}
