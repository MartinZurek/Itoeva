package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayScene.Place
import com.notime.glyphsim.matrix.PlayEffects.Carried

/** Ein Spielstand fuer die aktiv gespielte Welt. Darstellung und Effekte sind daraus abgeleitet. */
object GameAdventure {
    const val VERSION = 2
    const val MILLIS_PER_MINUTE = 2000L // Ein Tag dauert 48 aktive Minuten; Menues pausieren ihn.
    enum class Event { FOREST_SEEDS, FOREST_WOOD, OLD_MARKER, PLANTED, BENCH_REPAIRED, CAMP_REPAIRED, WAYMARKED }
    enum class ObjectId(val place: Place, val pos: PlayControl.Pos) {
        SEEDS(Place.FOREST, PlayControl.Pos(.25f, .72f)),
        WOOD(Place.FOREST, PlayControl.Pos(.78f, .70f)),
        MARKER(Place.FOREST, PlayControl.Pos(.55f, .30f)),
        GARDEN(Place.PARK, PlayControl.Pos(.27f, .77f)),
        BENCH(Place.PARK, PlayControl.Pos(.48f, .82f)),
        CAMP(Place.CAMP, PlayControl.Pos(.65f, .75f))
    }
    data class State(
        val place: Place = Place.STREET,
        val pos: PlayControl.Pos = PlayControl.Pos(.5f, .65f),
        val facing: PlayControl.Dir = PlayControl.Dir.DOWN,
        val backpack: PlayBackpack.Backpack = PlayBackpack.Backpack(),
        val elapsed: Long = 0L,
        val collected: Set<String> = emptySet(),
        val events: List<Event> = emptyList(),
        val met: Set<String> = emptySet(),
        val lampOn: Boolean = true,
        val tvOn: Boolean = false,
        val stowed: List<Carried> = emptyList(),
        val social: Map<String, String> = emptyMap(),
        val residentItems: Map<String, List<Carried>> = emptyMap()
    ) {
        val absoluteMinute: Int get() = (480L + elapsed / MILLIS_PER_MINUTE).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        val minuteOfDay: Int get() = absoluteMinute % 1440
        val weather: PlayWeather get() = if (absoluteMinute / 360 % 4 == 2) PlayWeather.RAIN else PlayWeather.CLEAR
        val dayPhase: PlayAmbientActivity.DayPhase get() = when (minuteOfDay) {
            in 360..659 -> PlayAmbientActivity.DayPhase.MORNING
            in 660..1079 -> PlayAmbientActivity.DayPhase.MIDDAY
            in 1080..1319 -> PlayAmbientActivity.DayPhase.EVENING
            else -> PlayAmbientActivity.DayPhase.NIGHT
        }
    }
    enum class Outcome { CHANGED, ALREADY, NEED_SEEDS, NEED_WOOD, FULL }
    data class Result(val state: State, val outcome: Outcome)

    fun tick(state: State, millis: Long, active: Boolean): State =
        if (active) state.copy(elapsed = (state.elapsed + millis.coerceIn(0, 100)).coerceAtMost(2_000_000_000_000L)) else state

    /** Es wird auf Boden gespeichert: ein laufender Sprung ist kein wiederherstellbarer Ruhepunkt. */
    fun safePosition(place: Place, requested: PlayControl.Pos, surfaces: List<GameMovement.Surface>? = null): PlayControl.Pos {
        val x = requested.x.takeIf { it.isFinite() }?.coerceIn(.025f, .975f) ?: .5f
        val d = requested.depth.takeIf { it.isFinite() }?.coerceIn(.025f, .975f) ?: .65f
        val scene = GameWorld.scene(place)
        val pos = scene?.let { GameTerrain.clamp(it,PlayControl.Pos(x,d)) } ?: PlayControl.Pos(x,d)
        val obstacles = surfaces ?: GameWorld.scene(place)?.let { GameSurfaces.painted(it) }.orEmpty()
        if (obstacles.none { it.contains(pos) && it.heightAt(pos) > 1f }) return pos
        return (1..19).flatMap { ix -> (1..19).map { iy -> PlayControl.Pos(ix / 20f, iy / 20f) } }
            .filter { p -> (scene == null || GameTerrain.valid(scene,p)) && obstacles.none { it.contains(p) && it.heightAt(p) > 1f } }
            .minByOrNull { p -> (p.x - x) * (p.x - x) + (p.depth - d) * (p.depth - d) * .0625f }
            ?: PlayControl.Pos(.5f, .95f)
    }

    fun act(state: State, id: ObjectId): Result {
        if (state.place != id.place) return Result(state, Outcome.ALREADY)
        val event = when (id) {
            ObjectId.SEEDS -> Event.FOREST_SEEDS
            ObjectId.WOOD -> Event.FOREST_WOOD
            ObjectId.MARKER -> Event.OLD_MARKER
            ObjectId.GARDEN -> Event.PLANTED
            ObjectId.BENCH -> Event.BENCH_REPAIRED
            ObjectId.CAMP -> Event.CAMP_REPAIRED
        }
        if (event in state.events) return Result(state, Outcome.ALREADY)
        val required = when (id) { ObjectId.GARDEN -> Carried.SEEDS; ObjectId.BENCH, ObjectId.CAMP -> Carried.WOOD; else -> null }
        val found = when (id) { ObjectId.SEEDS -> Carried.SEEDS; ObjectId.WOOD -> Carried.WOOD; else -> null }
        if (found != null && state.backpack.isFull) return Result(state, Outcome.FULL)
        val index = state.backpack.items.indexOf(required)
        if (required != null && index < 0) return Result(state,
            if (required == Carried.SEEDS) Outcome.NEED_SEEDS else Outcome.NEED_WOOD)
        val backpack = when {
            found != null -> PlayBackpack.add(state.backpack, found)
            required != null -> PlayBackpack.removeAt(state.backpack, index)
            else -> state.backpack
        }
        return Result(state.copy(backpack = backpack, events = state.events + event), Outcome.CHANGED)
    }

    /** Ein Fund gehoert einer Quelle; wiederholtes Lesen/Sitzen erzeugt keine neuen Gegenstaende. */
    fun collect(state: State, station: PlayScene.Station): Result {
        val found = PlayBackpack.lootAt(station) ?: return Result(state, Outcome.ALREADY)
        val key = "${state.place.name}:${station.name}"
        if (key in state.collected) return Result(state, Outcome.ALREADY)
        if (state.backpack.isFull) return Result(state, Outcome.FULL)
        return Result(state.copy(backpack = PlayBackpack.add(state.backpack, found), collected = state.collected + key), Outcome.CHANGED)
    }

    /** Reversible Ablage im Rucksackfenster. Auch ein voller alter Rucksack bleibt spielbar. */
    fun stow(state: State, index: Int): State {
        val item = state.backpack.items.getOrNull(index) ?: return state
        return state.copy(backpack = PlayBackpack.removeAt(state.backpack, index), stowed = state.stowed + item)
    }

    fun retrieve(state: State, index: Int): Result {
        val item = state.stowed.getOrNull(index) ?: return Result(state, Outcome.ALREADY)
        if (state.backpack.isFull) return Result(state, Outcome.FULL)
        return Result(state.copy(backpack = PlayBackpack.add(state.backpack, item),
            stowed = state.stowed.filterIndexed { i, _ -> i != index }), Outcome.CHANGED)
    }

    fun visible(state: State, id: ObjectId): Boolean = when (id) {
        ObjectId.SEEDS -> Event.FOREST_SEEDS !in state.events
        ObjectId.WOOD -> Event.FOREST_WOOD !in state.events
        else -> true
    }

    fun position(scene: GameScenes.Scene?, id: ObjectId): PlayControl.Pos {
        if (scene == null) return safePosition(id.place, id.pos)
        val bench = scene.spots.firstOrNull { it.station == PlayScene.Station.BENCH }
        val requested = if (id == ObjectId.BENCH || id == ObjectId.CAMP) bench?.let {
            GameScenes.posAt(scene, it.standX, it.standY)
        } ?: id.pos else id.pos
        return safePosition(scene.place, requested, GameSurfaces.painted(scene))
    }

    fun meet(state: State, profile: String): State = if (LivingResidents.all.any { it.profileId == profile })
        state.copy(met = state.met + profile) else state

    /** Begrenztes Zeilenformat mit expliziter Version; Zukunftsversionen werden nie zurueckgeschrieben. */
    fun encode(s: State): String = listOf("ITOEVA2:$VERSION", s.place.name,
        "${s.pos.x},${s.pos.depth}", s.facing.name, PlayBackpack.encode(s.backpack), s.elapsed.toString(),
        s.collected.sorted().joinToString(","), s.events.joinToString(",") { it.name },
        s.met.sorted().joinToString(","), "${s.lampOn},${s.tvOn}", s.stowed.joinToString(",") { it.name },
        s.social.toSortedMap().entries.joinToString(";") { (id, payload) ->
            "$id@${com.notime.glyphsim.data.LivingAgentSnapshotCodec.encodeText(payload)}" },
        s.residentItems.toSortedMap().entries.joinToString(";") { (id, items) ->
            "$id@${items.joinToString(",") { it.name }}" }).joinToString("\n")

    fun decode(text: String): State {
        require(text.length <= 256_000) { "Spielstand zu gross" }
        val lines = text.split('\n')
        val version = lines.firstOrNull()?.takeIf { it.startsWith("ITOEVA2:") }?.removePrefix("ITOEVA2:")?.toIntOrNull()
        require((version == 1 && lines.size == 11) || (version == VERSION && lines.size == 13)) {
            "Spielstandversion nicht lesbar" }
        fun entries(line: String): Map<String,String> {
            if (line.isEmpty()) return emptyMap()
            val pairs=line.split(';').map { token ->
                val at=token.indexOf('@');require(at>0);token.substring(0,at) to token.substring(at+1)
            }
            require(pairs.size<=12 && pairs.map { it.first }.distinct().size==pairs.size)
            return pairs.toMap()
        }
        val social = if (version == 1) emptyMap() else entries(lines[11]).mapValues { (id,payload) ->
            require(GameEncounters.validProfile(id))
            val raw=String(java.util.Base64.getUrlDecoder().decode(payload), java.nio.charset.StandardCharsets.UTF_8)
            require(com.notime.glyphsim.data.LivingAgentSnapshotCodec.decode(raw)?.snapshot?.agent?.profileId == id)
            raw
        }
        val goods = if (version == 1) emptyMap() else entries(lines[12]).mapValues { (id,payload) ->
            require(LivingResidents.all.any { it.profileId == id })
            val goods=payload.split(',').filter { it.isNotEmpty() }.map { Carried.valueOf(it) }
            require(goods.size<=PlayBackpack.CAPACITY);goods
        }
        val xy = lines[2].split(',').map { it.toFloat() }
        require(xy.size == 2 && xy.all { it.isFinite() }) { "Ungueltige Position" }
        val place = Place.valueOf(lines[1])
        val elapsed = lines[5].toLong()
        require(elapsed in 0..2_000_000_000_000L)
        fun values(line: String) = line.split(',').filter { it.isNotEmpty() }.toSet()
        val items = values(lines[4])
        require(items.all { name -> Carried.entries.any { it.name == name } })
        require(lines[4].split(',').filter { it.isNotEmpty() }.size <= PlayBackpack.CAPACITY)
        val lights = lines[9].split(',').map { it.toBooleanStrict() }
        require(lights.size == 2)
        val met = values(lines[8]).filter { id -> LivingResidents.all.any { it.profileId == id } }.toSet()
        val migratedSocial = if (version == 1) GameEncounters.legacyKnowledge(met,
            (480L + elapsed / MILLIS_PER_MINUTE).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), place) else social
        return State(place, PlayControl.Pos(xy[0].coerceIn(.025f,.975f),xy[1].coerceIn(.025f,.975f)), PlayControl.Dir.valueOf(lines[3]),
            PlayBackpack.decode(lines[4]), elapsed, values(lines[6]), lines[7].split(',').filter { it.isNotEmpty() }.map { Event.valueOf(it) }.distinct(),
            met, lights[0], lights[1],
            lines[10].split(',').filter { it.isNotEmpty() }.map { Carried.valueOf(it) }, migratedSocial, goods)
    }
}
