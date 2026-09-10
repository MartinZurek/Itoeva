package com.notime.glyphsim.data

import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.living.AgentState
import com.notime.glyphsim.living.Episode
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.LivingEvent
import com.notime.glyphsim.living.LivingEventKind
import com.notime.glyphsim.living.LivingSite
import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.living.Needs
import com.notime.glyphsim.living.Personality
import com.notime.glyphsim.living.Planner
import com.notime.glyphsim.living.RelationshipState
import com.notime.glyphsim.living.Requirement
import com.notime.glyphsim.living.SymbolicIntent
import com.notime.glyphsim.living.WorldState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ablage mit getrennten Profil-Schluesseln, ohne Android oder Dateisystem. */
private class LivingAgentMemoryStorage(
    initial: Map<String, String> = emptyMap()
) : LivingAgentStorage {
    val values = initial.toMutableMap()

    override fun read(profileId: String): String? = values[profileId]

    override fun write(profileId: String, payload: String) {
        values[profileId] = payload
    }
}

/**
 * Prueft die Zusicherungen der Persistenz statt einzelne Codec-Helfer: vollstaendiger Roundtrip,
 * Profiltrennung, Zeitfortschritt und eine echte alte Version.
 */
class LivingAgentStoreTest {

    private val open = setOf(LivingSite.HOME, LivingSite.WORKPLACE, LivingSite.MARKET)

    private fun world(
        coins: Int = 3,
        portions: Int = 2,
        minute: Int = 8 * 60
    ) = WorldState(
        day = 1,
        minuteOfDay = minute,
        site = LivingSite.HOME,
        coins = coins,
        portions = portions,
        openSites = open,
        nearbyProfiles = setOf("STARLET")
    )

    private fun richAgent(profileId: String = "WYRMLING"): AgentState {
        val interaction = LivingEvent(
            kind = LivingEventKind.SYMBOLS_RECEIVED,
            atMinute = 1_900,
            goal = GoalKind.CONNECT_WITH,
            action = ActionKind.RECEIVE_RESPONSE,
            blockedBy = Requirement.Near("STARLET"),
            counterpartProfileId = "STARLET",
            intents = setOf(SymbolicIntent.PLAY, SymbolicIntent.YES)
        )
        val base = AgentState(
            profileId = profileId,
            personality = Personality(
                goalBias = mapOf(GoalKind.CONNECT_WITH to 0.04),
                needGrowthPerHour = mapOf(NeedKind.SOCIAL to 0.05)
            ),
            needs = Needs.of(
                NeedKind.HUNGER to 0.42,
                NeedKind.ENERGY to 0.31,
                NeedKind.SOCIAL to 0.66
            ),
            goal = GoalKind.CONNECT_WITH,
            learnedPreferences = mapOf(GoalKind.CONNECT_WITH to 0.08),
            episodes = listOf(Episode(interaction, 1)),
            relationships = mapOf(
                "STARLET" to RelationshipState(
                    trust = 0.4,
                    closeness = 0.6,
                    interactions = 3,
                    lastInteraction = interaction
                )
            ),
            lastEvent = interaction
        )
        return base.copy(plan = Planner.planFor(base.goal!!, world(), base))
    }

    @Test
    fun `vollstaendiger Zustand und Ressourcen ueberleben den Roundtrip`() {
        val storage = LivingAgentMemoryStorage()
        val store = LivingAgentStore(storage)
        val beforeWorld = world()
        val beforeAgent = richAgent()

        store.save(beforeAgent, beforeWorld)
        val restored = store.restore(
            beforeAgent.profileId,
            beforeWorld.absoluteMinute,
            beforeWorld.openSites,
            beforeWorld.nearbyProfiles
        )!!

        assertEquals(beforeAgent.copy(plan = null), restored.agent)
        assertEquals(beforeWorld, restored.world)
        assertNull(restored.migratedFromVersion)
        assertTrue(storage.values.getValue(beforeAgent.profileId).startsWith("version=2\n"))
    }

    @Test
    fun `Profile behalten getrennte Beduerfnisse und Ressourcen`() {
        val storage = LivingAgentMemoryStorage()
        val store = LivingAgentStore(storage)
        val a = richAgent("A").copy(needs = Needs.of(NeedKind.HUNGER to 0.2))
        val b = richAgent("B").copy(needs = Needs.of(NeedKind.HUNGER to 0.9))

        store.save(a, world(coins = 1), simulationMinute = 2_000)
        store.save(b, world(coins = 9), simulationMinute = 2_000)

        val restoredA = store.restore("A", 2_000, open)!!
        val restoredB = store.restore("B", 2_000, open)!!
        assertEquals(0.2, restoredA.agent.needs.pressure(NeedKind.HUNGER), 1e-9)
        assertEquals(0.9, restoredB.agent.needs.pressure(NeedKind.HUNGER), 1e-9)
        assertEquals(1, restoredA.world.coins)
        assertEquals(9, restoredB.world.coins)
    }

    @Test
    fun `verstrichene Simulationszeit laesst Beduerfnisse nachwachsen`() {
        val storage = LivingAgentMemoryStorage()
        val store = LivingAgentStore(storage)
        val startWorld = world(minute = 100)
        val startAgent = AgentState("A", Personality(), Needs.calm())

        store.save(startAgent, startWorld, simulationMinute = 100)
        val restored = store.restore("A", 220, open, startWorld.nearbyProfiles)!!

        assertEquals(
            startAgent.needs.advanced(120, startAgent.personality),
            restored.agent.needs
        )
        assertEquals(startWorld.advanced(120), restored.world)
        assertNull("Ein alter Plan darf aktuelle Hindernisse nicht umgehen", restored.agent.plan)
    }

    @Test
    fun `gleiche gespeicherte Zeit und Eingabe ergeben denselben Zustand`() {
        val storage = LivingAgentMemoryStorage()
        val store = LivingAgentStore(storage)
        store.save(richAgent("A"), world(), simulationMinute = 2_000)

        val first = store.restore("A", 2_360, open, setOf("STARLET"))
        val second = store.restore("A", 2_360, open, setOf("STARLET"))
        assertEquals(first, second)
    }

    @Test
    fun `Version eins wird mit leeren neuen Feldern nach Version zwei migriert`() {
        val profile = "ALT"
        val versionOne = listOf(
            "version=1",
            "profile=${LivingAgentSnapshotCodec.encodeText(profile)}",
            "savedAt=100",
            "worldDay=0",
            "worldMinute=100",
            "site=HOME",
            "coins=2",
            "portions=1",
            "needs=HUNGER:0.4;ENERGY:0.2",
            "bias=GET_FOOD:0.03",
            "growth=HUNGER:0.07",
            "goal=GET_FOOD",
            "lastEvent=-"
        ).joinToString("\n")
        val storage = LivingAgentMemoryStorage(mapOf(profile to versionOne))

        val restored = LivingAgentStore(storage).restore(profile, 100, open)!!

        assertEquals(1, restored.migratedFromVersion)
        assertEquals(GoalKind.GET_FOOD, restored.agent.goal)
        assertTrue(restored.agent.episodes.isEmpty())
        assertTrue(restored.agent.relationships.isEmpty())
        assertTrue(restored.agent.learnedPreferences.isEmpty())
        assertTrue(storage.values.getValue(profile).startsWith("version=2\n"))
    }

    @Test
    fun `unbekannte Zukunftsversion wird nicht geraten`() {
        val profile = "ZUKUNFT"
        val storage = LivingAgentMemoryStorage(
            mapOf(profile to "version=99\nprofile=${LivingAgentSnapshotCodec.encodeText(profile)}")
        )
        assertNull(LivingAgentStore(storage).restore(profile, 0, open))
    }
}
