package com.notime.glyphsim.stream

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.living.AgentState
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.LivingEvent
import com.notime.glyphsim.living.LivingEventKind
import com.notime.glyphsim.living.LivingSite
import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.living.Needs
import com.notime.glyphsim.living.Personality
import com.notime.glyphsim.living.Requirement
import com.notime.glyphsim.living.WorldState
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.LivingRuntimeAdapter
import com.notime.glyphsim.matrix.PlayScene
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Belegt den Stream-Vertrag an wirklich ausgefuehrten Runtime-Schritten. */
class LivingObservationSourceTest {

    @Test
    fun `Quelle erklaert ein Hindernis ohne die Simulation zu steuern`() {
        val journal = LivingObservationJournal(eventLimit = 8)
        val source: LivingObservationSource = journal
        val startAgent = AgentState(
            profileId = AvatarSpecies.PUFFLING.name,
            personality = Personality.of(AvatarSpecies.PUFFLING),
            needs = Needs.of(NeedKind.HUNGER to 0.95)
        )
        val startWorld = LivingRuntimeAdapter.initialWorld(
            absoluteMinute = 17 * 60 + 50,
            place = PlayScene.Place.KITCHEN,
            coins = 0,
            portions = 0
        )
        val unchanged = startAgent to startWorld

        val prepared = LivingRuntimeAdapter.prepare(
            agent = startAgent,
            world = startWorld,
            renderedPlace = PlayScene.Place.KITCHEN,
            interestTopic = AnimationType.MOVE,
            random = Random(4)
        )
        journal.record(prepared.result)
        val observed = source.current(startAgent.profileId)!!

        assertEquals(GoalKind.GET_FOOD, observed.goal)
        assertEquals(GoalKind.GET_FOOD, observed.reason?.goal)
        assertEquals(
            Requirement.SiteOpen(LivingSite.WORKPLACE),
            observed.blockedBy
        )
        assertEquals(LivingEventKind.ACTION_BLOCKED, observed.importantEvent?.kind)
        assertTrue(observed.recentEvents.none { it.kind == LivingEventKind.IDLE })
        assertEquals(unchanged, startAgent to startWorld)
    }

    @Test
    fun `Mehrtageslauf bleibt begrenzt deterministisch und erzaehlt aus Ereignissen`() {
        val first = runFourDays()
        val second = runFourDays()

        assertEquals(first, second)
        assertTrue(first.finalWorld.day >= 4)
        assertTrue(first.events.count { it.kind != LivingEventKind.IDLE } > 12)
        assertEquals(12, first.observation.recentEvents.size)
        assertNotNull(first.observation.importantEvent)
        assertTrue(first.observations.any { it.reason?.goal == it.goal })
        assertTrue(first.observations.any {
            it.currentAction != null && it.plan.firstOrNull() == it.currentAction
        })

        val blocked = first.events.indexOfFirst {
            it.kind == LivingEventKind.ACTION_BLOCKED &&
                it.blockedBy == Requirement.SiteOpen(LivingSite.WORKPLACE)
        }
        val worked = first.events.indexOfFirst {
            it.kind == LivingEventKind.ACTION_DONE && it.action == ActionKind.WORK
        }
        val bought = first.events.indexOfFirst {
            it.kind == LivingEventKind.ACTION_DONE && it.action == ActionKind.BUY_FOOD
        }
        val ate = first.events.indexOfFirst {
            it.kind == LivingEventKind.ACTION_DONE && it.action == ActionKind.EAT
        }
        assertTrue("Der geschlossene Arbeitsplatz muss zuerst zum Hindernis werden", blocked >= 0)
        assertTrue("Arbeit muss die Anpassung an das Hindernis sein", worked > blocked)
        assertTrue("Der Einkauf muss auf die Arbeit folgen", bought > worked)
        assertTrue("Erst danach kann das Wesen essen", ate > bought)
    }

    private fun runFourDays(): RunResult {
        val journal = LivingObservationJournal(eventLimit = 12)
        val random = Random(19)
        var agent = AgentState(
            profileId = AvatarSpecies.PUFFLING.name,
            personality = Personality.of(AvatarSpecies.PUFFLING),
            needs = Needs.of(NeedKind.HUNGER to 0.95)
        )
        var world = LivingRuntimeAdapter.initialWorld(
            absoluteMinute = 17 * 60 + 50,
            place = PlayScene.Place.KITCHEN,
            coins = 0,
            portions = 0
        )
        var renderedPlace = PlayScene.Place.KITCHEN
        val events = mutableListOf<LivingEvent>()
        val observations = mutableListOf<LivingObservation>()
        var steps = 0

        while (world.day < 4 && steps < 500) {
            val prepared = LivingRuntimeAdapter.prepare(
                agent = agent,
                world = world,
                renderedPlace = renderedPlace,
                interestTopic = AnimationType.MOVE,
                random = random
            )
            agent = prepared.result.agent
            world = prepared.result.world
            renderedPlace = placeFor(world.site)
            events += prepared.result.events
            journal.record(prepared.result)
            observations += journal.current(agent.profileId)!!
            steps++
        }

        assertTrue("Der Lauf darf nicht an derselben Simulationsminute haengen", steps < 500)
        return RunResult(
            finalAgent = agent,
            finalWorld = world,
            events = events,
            observations = observations,
            observation = journal.current(agent.profileId)!!
        )
    }

    private fun placeFor(site: LivingSite): PlayScene.Place = when (site) {
        LivingSite.HOME -> PlayScene.Place.KITCHEN
        LivingSite.WORKPLACE -> PlayScene.Place.WORK
        LivingSite.MARKET -> PlayScene.Place.SHOP
        LivingSite.OUTSIDE -> PlayScene.Place.STREET
    }

    private data class RunResult(
        val finalAgent: AgentState,
        val finalWorld: WorldState,
        val events: List<LivingEvent>,
        val observations: List<LivingObservation>,
        val observation: LivingObservation
    )
}
