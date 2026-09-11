package com.notime.glyphsim.stream

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.living.AgentState
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.GoalInfluence
import com.notime.glyphsim.living.LivingSite
import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.living.Needs
import com.notime.glyphsim.living.Personality
import com.notime.glyphsim.living.UtilitySelector
import com.notime.glyphsim.living.WorldState
import com.notime.glyphsim.matrix.LivingRuntimeAdapter
import com.notime.glyphsim.matrix.PlayScene
import com.notime.glyphsim.ui.ACTION_SLOT_COUNT
import com.notime.glyphsim.ui.SavedAction
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** Belegt den lokalen Viewer-Weg, ohne Compose oder eine Plattformverbindung vorauszusetzen. */
class StreamInteractionTest {

    @Test
    fun `Stream Mode speichert eine neue Ausloesung automatisch`() {
        val saved = action(occurrenceId = 17, type = AnimationType.BOOK)

        val result = StreamInteractions.autoSave(true, StreamInteractionState(), saved)

        assertEquals(17L, result.slots.first()?.occurrenceId)
        assertEquals(1, result.slots.count { it != null })
    }

    @Test
    fun `mehr als vier Ausloesungen belegen keinen fuenften Platz`() {
        val result = (1L..5L).fold(StreamInteractionState()) { state, occurrenceId ->
            StreamInteractions.autoSave(true, state, action(occurrenceId))
        }

        assertEquals(ACTION_SLOT_COUNT, result.slots.count { it != null })
        assertEquals(listOf(1L, 2L, 3L, 4L), result.slots.mapNotNull { it?.occurrenceId })
    }

    @Test
    fun `lokaler Viewer waehlt einen vorhandenen Slot`() {
        val state = StreamInteractions.autoSave(
            true,
            StreamInteractionState(),
            action(occurrenceId = 31, type = AnimationType.BOOK)
        )

        val accepted = StreamInteractions.select(state, slotId = 1, atMinute = 720)
            as StreamSelection.Accepted

        assertEquals(1, accepted.impulse.savedSlotId)
        assertEquals(31L, accepted.impulse.occurrenceId)
        assertEquals(AnimationType.BOOK, accepted.impulse.animationType)
        assertEquals(ExternalImpulseSource.LOCAL_VIEWER_SIMULATOR, accepted.impulse.source)
        assertEquals(accepted.impulse, accepted.state.pending)
        assertEquals(31L, accepted.state.slots.first()?.occurrenceId)
    }

    @Test
    fun `lokaler Viewer kann keinen leeren Slot ausloesen`() {
        assertSame(
            StreamSelection.Missing,
            StreamInteractions.select(StreamInteractionState(), slotId = 2, atMinute = 720)
        )
    }

    @Test
    fun `Book Impuls erreicht die bestehende Living Simulation`() {
        val selected = selected(AnimationType.BOOK)
        val agent = AgentState("PUFFLING", Personality(), Needs.calm())
        val world = world()

        val prepared = LivingRuntimeAdapter.prepare(
            agent = agent,
            world = world,
            renderedPlace = PlayScene.Place.LIVING,
            interestTopic = selected.impulse.animationType,
            random = Random(4),
            goalInfluence = StreamInteractions.influenceFor(selected.impulse)
        )

        assertEquals(GoalKind.DEVELOP, prepared.result.agent.goal)
        assertEquals(AnimationType.BOOK, prepared.topic)
        assertTrue(StreamInteractions.wasHandled(selected.impulse, prepared))
        val cleared = StreamInteractions.clearHandled(selected.state, selected.impulse)
        assertNull(cleared.slots.first())
        assertNull(cleared.pending)
    }

    @Test
    fun `Work Impuls fuehrt trotz vollem Vorrat nur zur freiwilligen Arbeit`() {
        val selected = selected(AnimationType.WORK)
        val agent = AgentState("PUFFLING", Personality(), Needs.calm())

        val prepared = LivingRuntimeAdapter.prepare(
            agent = agent,
            world = world(portions = 3),
            renderedPlace = PlayScene.Place.LIVING,
            interestTopic = selected.impulse.animationType,
            random = Random(4),
            goalInfluence = StreamInteractions.influenceFor(selected.impulse)
        )

        assertEquals(GoalKind.EARN_MONEY, prepared.result.agent.goal)
        assertTrue(ActionKind.WORK in prepared.completedActions)
        assertTrue(ActionKind.EAT !in prepared.completedActions)
        assertTrue(StreamInteractions.wasHandled(selected.impulse, prepared))
        assertNull(StreamInteractions.clearHandled(selected.state, selected.impulse).pending)
    }

    @Test
    fun `normaler App Modus speichert weiterhin nicht automatisch`() {
        val start = StreamInteractionState()

        val result = StreamInteractions.autoSave(false, start, action(occurrenceId = 9))

        assertSame(start, result)
    }

    @Test
    fun `Viewer Impuls ueberschreibt keinen dringenden Agentenzustand`() {
        val selected = selected(AnimationType.BOOK)
        val agent = AgentState(
            "PUFFLING",
            Personality(),
            Needs.of(NeedKind.HUNGER to 0.95, NeedKind.GROWTH to 0.0)
        )
        val world = world(portions = 3)
        val unchanged = agent to world

        val prepared = LivingRuntimeAdapter.prepare(
            agent = agent,
            world = world,
            renderedPlace = PlayScene.Place.KITCHEN,
            interestTopic = selected.impulse.animationType,
            random = Random(4),
            goalInfluence = StreamInteractions.influenceFor(selected.impulse)
        )

        assertEquals(GoalKind.GET_FOOD, prepared.result.agent.goal)
        assertTrue(!StreamInteractions.wasHandled(selected.impulse, prepared))
        assertTrue(GoalInfluence.MAX_WEIGHT < UtilitySelector.MIN_PRESSURE)
        assertEquals(unchanged, agent to world)
        assertEquals(31L, selected.state.slots.first()?.occurrenceId)
    }

    @Test
    fun `medizinische und frei beschriftete Inhalte bleiben aus Stream Slots heraus`() {
        val medicine = StreamInteractions.autoSave(
            true,
            StreamInteractionState(),
            action(1, AnimationType.MEDICINE)
        )
        val privateLabel = StreamInteractions.autoSave(
            true,
            StreamInteractionState(),
            action(2, type = null, label = "Private note")
        )

        assertTrue(medicine.slots.all { it == null })
        assertTrue(privateLabel.slots.all { it == null })
    }

    private fun selected(type: AnimationType): StreamSelection.Accepted {
        val state = StreamInteractions.autoSave(
            true,
            StreamInteractionState(),
            action(31, type)
        )
        return StreamInteractions.select(state, slotId = 1, atMinute = 720)
            as StreamSelection.Accepted
    }

    private fun action(
        occurrenceId: Long,
        type: AnimationType? = AnimationType.MOVE,
        label: String? = null
    ) = SavedAction(
        reminderId = occurrenceId + 100,
        occurrenceId = occurrenceId,
        animationType = type,
        libraryAnimationLabel = label,
        frames = listOf(intArrayOf(0))
    )

    private fun world(portions: Int = 2) = WorldState(
        day = 0,
        minuteOfDay = 12 * 60,
        site = LivingSite.HOME,
        coins = 2,
        portions = portions,
        openSites = LivingSite.entries.toSet()
    )
}
