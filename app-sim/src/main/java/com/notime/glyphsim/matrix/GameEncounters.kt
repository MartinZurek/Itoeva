package com.notime.glyphsim.matrix

import com.notime.glyphsim.data.LivingAgentSnapshot
import com.notime.glyphsim.data.LivingAgentSnapshotCodec
import com.notime.glyphsim.living.*
import com.notime.glyphsim.matrix.PlayEffects.Carried

/** Eine Commit-Grenze fuer Spielerbesitz, Bewohnerbesitz und BEIDE sozialen Erinnerungen. */
object GameEncounters {
    private const val HOST = "game-host:"
    enum class Outcome { CHANGED, KNOWN, ABSENT, TOO_FAR, BUSY, NO_ITEM, FULL, NEED_WOOD, ALREADY }
    data class Result(val state: GameAdventure.State, val outcome: Outcome)
    fun host(species: AvatarSpecies) = HOST + species.name
    fun validProfile(id: String) = LivingResidents.all.any { it.profileId == id } || AvatarSpecies.entries.any { host(it) == id }
    private fun stored(state: GameAdventure.State, id: String) =
        state.social[id]?.let { LivingAgentSnapshotCodec.decode(it)?.snapshot }
    fun knows(state: GameAdventure.State, species: AvatarSpecies, resident: String): Boolean =
        stored(state,host(species))?.agent?.relationships?.containsKey(resident) == true
    fun received(state: GameAdventure.State, resident: String): List<Carried> = state.residentItems[resident].orEmpty()

    /** V1 kannte Bewohner weltweit: Kenntnis erhalten, ohne Begegnungen oder Naehe zu erfinden. */
    fun legacyKnowledge(met: Set<String>, minute: Int, place: PlayScene.Place): Map<String,String> {
        if (met.isEmpty()) return emptyMap()
        return AvatarSpecies.entries.associate { species ->
            val id = host(species)
            val agent = LivingRuntimeAdapter.initialAgent(id, species).copy(
                relationships = met.associateWith { RelationshipState() })
            val world = LivingRuntimeAdapter.initialWorld(minute, place, 0, 0, emptySet())
            id to LivingAgentSnapshotCodec.encode(LivingAgentSnapshot(agent, world, minute))
        }
    }

    /** Der gespeicherte soziale Anteil wird idempotent in denselben Bewohnerkern eingeblendet. */
    fun restore(state: GameAdventure.State, resident: ResidentState): ResidentState {
        val canonical = stored(state,resident.agent.profileId)?.agent ?: return resident
        val gameRelations = canonical.relationships.filterKeys { it.startsWith(HOST) }
        val gameEpisodes = canonical.episodes.filter { it.event.counterpartProfileId?.startsWith(HOST) == true }
        val episodes = (resident.agent.episodes.filterNot { it.event.counterpartProfileId?.startsWith(HOST) == true } + gameEpisodes)
            .sortedBy { it.event.atMinute }.takeLast(AgentState.MAX_EPISODES)
        return resident.copy(agent=resident.agent.copy(relationships=resident.agent.relationships + gameRelations, episodes=episodes))
    }
    private fun check(state: GameAdventure.State, resident: ResidentState, actor: GameResidents.Actor?): Outcome? {
        if (actor == null || actor.leaving || !actor.snapshot.publiclyPresent || actor.snapshot.place != state.place ||
            actor.snapshot.profileId != resident.agent.profileId) return Outcome.ABSENT
        val dx=actor.pos.x-state.pos.x; val dy=(actor.pos.depth-state.pos.depth)*.25f
        if (dx*dx+dy*dy > .035f) return Outcome.TOO_FAR
        return null
    }
    private fun remember(state: GameAdventure.State, species: AvatarSpecies, resident: ResidentState,
        kind: ActionKind): GameAdventure.State {
        val player = stored(state,host(species))?.agent ?: LivingRuntimeAdapter.initialAgent(host(species),species)
        val other = restore(state,resident).agent
        val playerWorld = LivingRuntimeAdapter.initialWorld(state.absoluteMinute,state.place,0,0,setOf(other.profileId))
        val otherWorld = resident.world.copy(day=playerWorld.day,minuteOfDay=playerWorld.minuteOfDay,
            nearbyProfiles=setOf(player.profileId))
        fun action(target: String) = Action(kind,listOf(Requirement.Near(target)),ActionOutcome(
            rememberValence=if(kind==ActionKind.GAME_MEET) 0 else 1,
            relationshipEffect=RelationshipEffect(target,
                trustDelta=if(kind==ActionKind.GAME_MEET) 0.0 else .01,
                closenessDelta=if(kind==ActionKind.GAME_MEET) .005 else .02)))
        val first=action(other.profileId).applyTo(player,playerWorld,GoalKind.CONNECT_WITH)
        val second=action(player.profileId).applyTo(other,otherWorld,GoalKind.CONNECT_WITH)
        val payloads=mapOf(player.profileId to LivingAgentSnapshotCodec.encode(
            LivingAgentSnapshot(first.agent,playerWorld,playerWorld.absoluteMinute)),
            other.profileId to LivingAgentSnapshotCodec.encode(LivingAgentSnapshot(second.agent,resident.world,resident.world.absoluteMinute)))
        return state.copy(social=state.social+payloads,met=state.met+other.profileId)
    }
    fun meet(state: GameAdventure.State, species: AvatarSpecies, resident: ResidentState,
        actor: GameResidents.Actor?): Result {
        check(state,resident,actor)?.let { return Result(state,it) }
        val last=stored(state,host(species))?.agent?.relationships?.get(resident.agent.profileId)?.lastInteraction
        // Schliessen und Wiederholen desselben Gespraechs erzeugt keinen Naehe-Farm.
        if (last != null && last.atMinute / 1440 == state.absoluteMinute / 1440) return Result(state,Outcome.KNOWN)
        return Result(remember(state,species,resident,ActionKind.GAME_MEET),Outcome.CHANGED)
    }
    fun give(state: GameAdventure.State, species: AvatarSpecies, resident: ResidentState,
        actor: GameResidents.Actor?, index: Int): Result {
        check(state,resident,actor)?.let { return Result(state,it) }
        val item=state.backpack.items.getOrNull(index) ?: return Result(state,Outcome.NO_ITEM)
        if (resident.agent.needs.pressure(NeedKind.ENERGY) > .92 || resident.agent.needs.pressure(NeedKind.HUNGER) > .94)
            return Result(state,Outcome.BUSY)
        val goods=received(state,resident.agent.profileId)
        if (goods.size>=PlayBackpack.CAPACITY) return Result(state,Outcome.FULL)
        val after=state.copy(backpack=PlayBackpack.removeAt(state.backpack,index),
            residentItems=state.residentItems+(resident.agent.profileId to goods+item))
        return Result(remember(after,species,resident,ActionKind.GAME_GIVE),Outcome.CHANGED)
    }
    /** Alternative fuer dasselbe Holz: sichtbare Orientierung gemeinsam bauen statt eine Bank reparieren. */
    fun waymark(state: GameAdventure.State, species: AvatarSpecies, resident: ResidentState,
        actor: GameResidents.Actor?): Result {
        check(state,resident,actor)?.let { return Result(state,it) }
        if (state.place!=PlayScene.Place.PARK) return Result(state,Outcome.ABSENT)
        if (GameAdventure.Event.WAYMARKED in state.events) return Result(state,Outcome.ALREADY)
        if (!knows(state,species,resident.agent.profileId)) return Result(state,Outcome.BUSY)
        if (resident.agent.needs.pressure(NeedKind.ENERGY) > .85) return Result(state,Outcome.BUSY)
        val id=resident.agent.profileId
        val goods=received(state,id);val index=goods.indexOf(Carried.WOOD)
        if(index<0) return Result(state,Outcome.NEED_WOOD)
        val after=state.copy(residentItems=state.residentItems+(id to goods.filterIndexed { i,_ -> i!=index }),
            events=state.events+GameAdventure.Event.WAYMARKED)
        return Result(remember(after,species,resident,ActionKind.GAME_WAYMARK),Outcome.CHANGED)
    }
}
