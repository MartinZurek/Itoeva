package com.notime.glyphsim.matrix

import com.notime.glyphsim.living.*
import com.notime.glyphsim.matrix.PlayEffects.Carried
import com.notime.glyphsim.matrix.PlayScene.Place
import org.junit.Assert.*
import org.junit.Test

class GameEncountersTest {
    private val id=LivingResidents.all.first { it.anchorPlace==Place.PARK }.profileId
    private val resident=LivingPopulation.initial(480).getValue(id).let {
        it.copy(agent=it.agent.copy(needs=Needs.calm()),world=it.world.copy(site=LivingSite.OUTSIDE)) }
    private val actor=GameResidents.Actor(LivingPopulation.snapshot(mapOf(id to resident)).single()
        .copy(place=Place.PARK,publiclyPresent=true),PlayControl.Pos(.5f,.72f))
    private fun state(vararg items: Carried)=GameAdventure.State(place=Place.PARK,pos=PlayControl.Pos(.45f,.72f),
        backpack=PlayBackpack.Backpack(items.toList()))
    private fun meet(s: GameAdventure.State)=GameEncounters.meet(s,AvatarSpecies.FENNEC,resident,actor).state
    @Test fun `Beide Seiten behalten dieselbe echte Begegnung ohne Wirtschaft oder Weltzeit zu veraendern`() {
        val before=state();val after=meet(before);val memory=GameEncounters.restore(after,resident)
        assertTrue(GameEncounters.knows(after,AvatarSpecies.FENNEC,id))
        assertFalse(GameEncounters.knows(after,AvatarSpecies.WYRMLING,id))
        val relation=memory.agent.relationships.getValue(GameEncounters.host(AvatarSpecies.FENNEC))
        assertEquals(ActionKind.GAME_MEET,relation.lastInteraction!!.action)
        assertEquals(before.absoluteMinute,relation.lastInteraction!!.atMinute)
        assertEquals(resident.world,memory.world);assertEquals(resident.agent.needs,memory.agent.needs)
        assertEquals(before.backpack,after.backpack)
    }
    @Test fun `Wiedersehen am selben Tag vervielfacht keine Naehe`() {
        val first=meet(state())
        val second=GameEncounters.meet(first,AvatarSpecies.FENNEC,resident,actor)
        assertEquals(GameEncounters.Outcome.KNOWN,second.outcome);assertSame(first,second.state)
        val next=GameEncounters.meet(first.copy(elapsed=2_880_000L),AvatarSpecies.FENNEC,resident,actor)
        assertEquals(GameEncounters.Outcome.CHANGED,next.outcome)
        assertEquals(2,GameEncounters.restore(next.state,resident).agent.relationships.getValue(GameEncounters.host(AvatarSpecies.FENNEC)).interactions)
    }
    @Test fun `Uebergabe verschiebt genau einen Gegenstand und beide Erinnerungen ueberleben Neustart`() {
        val before=meet(state(Carried.WOOD,Carried.WOOD,Carried.BOOK))
        val after=GameEncounters.give(before,AvatarSpecies.FENNEC,resident,actor,0)
        assertEquals(GameEncounters.Outcome.CHANGED,after.outcome)
        assertEquals(listOf(Carried.WOOD,Carried.BOOK),after.state.backpack.items)
        assertEquals(listOf(Carried.WOOD),GameEncounters.received(after.state,id))
        val restored=GameAdventure.decode(GameAdventure.encode(after.state))
        assertEquals(after.state,restored)
        assertEquals(ActionKind.GAME_GIVE,GameEncounters.restore(restored,resident).agent.relationships.getValue(GameEncounters.host(AvatarSpecies.FENNEC)).lastInteraction!!.action)
    }
    @Test fun `Idempotente Einblendung kann Erinnerungen nicht doppelt verbuchen`() {
        val s=GameEncounters.give(meet(state(Carried.BOOK)),AvatarSpecies.FENNEC,resident,actor,0).state
        val once=GameEncounters.restore(s,resident)
        var current=once
        repeat(100) { current=GameEncounters.restore(s,current) }
        assertEquals(once,current)
    }
    @Test fun `Abwesender ferner oder mueder Empfaenger erhaelt keine heimliche Uebergabe`() {
        val s=state(Carried.WOOD)
        val absent=GameEncounters.give(s,AvatarSpecies.FENNEC,resident,null,0)
        assertEquals(GameEncounters.Outcome.ABSENT,absent.outcome);assertSame(s,absent.state)
        val far=GameEncounters.give(s.copy(pos=PlayControl.Pos(.01f,.1f)),AvatarSpecies.FENNEC,resident,actor,0)
        assertEquals(GameEncounters.Outcome.TOO_FAR,far.outcome)
        val tired=resident.copy(agent=resident.agent.copy(needs=Needs.of(NeedKind.ENERGY to .99)))
        val no=GameEncounters.give(s,AvatarSpecies.FENNEC,tired,actor,0)
        assertEquals(GameEncounters.Outcome.BUSY,no.outcome);assertSame(s,no.state)
    }
    @Test fun `Volle Bewohnerablage und verschwundener Gegenstand lassen Spielerbesitz unberuehrt`() {
        val s=state(Carried.WOOD).copy(residentItems=mapOf(id to List(PlayBackpack.CAPACITY) { Carried.BOOK }))
        assertEquals(GameEncounters.Outcome.FULL,GameEncounters.give(s,AvatarSpecies.FENNEC,resident,actor,0).outcome)
        assertEquals(GameEncounters.Outcome.NO_ITEM,GameEncounters.give(s,AvatarSpecies.FENNEC,resident,actor,4).outcome)
        assertEquals(listOf(Carried.WOOD),s.backpack.items)
    }
    @Test fun `Dasselbe Holz hat einen direkten und einen sozialen Weg mit verschiedenen Weltfolgen`() {
        val s=state(Carried.WOOD)
        val direct=GameAdventure.act(s,GameAdventure.ObjectId.BENCH).state
        val given=GameEncounters.give(meet(s),AvatarSpecies.FENNEC,resident,actor,0).state
        val together=GameEncounters.waymark(given,AvatarSpecies.FENNEC,resident,actor)
        assertEquals(GameEncounters.Outcome.CHANGED,together.outcome)
        assertTrue(GameAdventure.Event.BENCH_REPAIRED in direct.events)
        assertFalse(GameAdventure.Event.WAYMARKED in direct.events)
        assertTrue(GameAdventure.Event.WAYMARKED in together.state.events)
        assertFalse(GameAdventure.Event.BENCH_REPAIRED in together.state.events)
        assertTrue(direct.backpack.items.isEmpty());assertTrue(together.state.backpack.items.isEmpty())
        assertTrue(GameEncounters.received(together.state,id).isEmpty())
        assertEquals(GameEncounters.Outcome.ALREADY,GameEncounters.waymark(together.state,AvatarSpecies.FENNEC,resident,actor).outcome)
    }
    @Test fun `Ohne gegebenes Holz gibt es weder Wegweiser noch Material aus dem Nichts`() {
        val s=meet(state(Carried.WOOD))
        val result=GameEncounters.waymark(s,AvatarSpecies.FENNEC,resident,actor)
        assertEquals(GameEncounters.Outcome.NEED_WOOD,result.outcome);assertSame(s,result.state)
    }
    @Test fun `Alter Spielstand wird verlustfrei gelesen neue Versionen bleiben geschuetzt`() {
        val s=state(Carried.BOOK,Carried.WOOD).copy(stowed=listOf(Carried.SEEDS),lampOn=false,tvOn=true)
        val old=GameAdventure.encode(s).split('\n').take(11).joinToString("\n").replace("ITOEVA2:2","ITOEVA2:1")
        assertEquals(s,GameAdventure.decode(old))
        assertTrue(GameAdventure.encode(GameAdventure.decode(old)).startsWith("ITOEVA2:2"))
        for (bad in listOf(GameAdventure.encode(s).replace("ITOEVA2:2","ITOEVA2:3"),GameAdventure.encode(s).replace("ITOEVA2:2","2"))) {
            assertThrows(IllegalArgumentException::class.java) { GameAdventure.decode(bad) }
        }
    }
    @Test fun `Fremde Profile und korrupte soziale Daten werden nicht still geloescht`() {
        val invalid=state().copy(social=mapOf("private-person" to "bad"))
        assertThrows(IllegalArgumentException::class.java) { GameAdventure.decode(GameAdventure.encode(invalid)) }
        val corrupted=state().copy(social=mapOf(id to "bad"))
        assertThrows(IllegalArgumentException::class.java) { GameAdventure.decode(GameAdventure.encode(corrupted)) }
    }
    @Test fun `Bekannte V1 Bewohner bleiben auch nach Neustart bekannt ohne erfundene Episode`() {
        val s = state().copy(met = setOf(id))
        val v1 = GameAdventure.encode(s).split('\n').take(11).joinToString("\n").replace("ITOEVA2:2", "ITOEVA2:1")
        val restored = GameAdventure.decode(GameAdventure.encode(GameAdventure.decode(v1)))
        assertEquals(s, restored.copy(social = emptyMap()))
        for (species in AvatarSpecies.entries) {
            assertTrue(GameEncounters.knows(restored, species, id))
            val agent = com.notime.glyphsim.data.LivingAgentSnapshotCodec.decode(restored.social.getValue(GameEncounters.host(species)))!!.snapshot.agent
            assertTrue(agent.episodes.isEmpty())
            assertEquals(com.notime.glyphsim.living.RelationshipState(), agent.relationships.getValue(id))
        }
    }
}
