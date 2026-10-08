package com.notime.glyphsim.matrix

import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.matrix.PlayScene.Station

/** Die bestehende Population bekommt Wege in der gemalten Raumgeometrie, keine zweite KI. */
object GameResidents {
    data class Actor(val snapshot: ResidentSnapshot, val pos: PlayControl.Pos,
        val moving: Boolean = false, val leaving: Boolean = false, val facing: PlayControl.Dir = PlayControl.Dir.DOWN,
        val seat: GameSeating.State? = null) {
        fun seatFrame(scene: GameScenes.Scene, clock: Long): GameSeating.Frame? =
            seat?.let { GameSeating.frame(scene, snapshot.species, it, clock) }
        fun renderPos(scene: GameScenes.Scene, clock: Long): PlayControl.Pos = seatFrame(scene, clock)?.pos ?: pos
        fun hitBox(scene: GameScenes.Scene, clock: Long): GameScenes.Box {
            val frame = seatFrame(scene, clock)
            val box = GameCharacterScale.hitBox(scene, frame?.pos ?: pos, snapshot.species)
            val lift = frame?.lift ?: 0f
            return box.copy(y0 = box.y0 - lift, y1 = box.y1 - lift)
        }
    }

    fun tick(scene: GameScenes.Scene, actors: Map<String, Actor>, snapshots: List<ResidentSnapshot>,
        millis: Long, clock: Long, talking: String?, reservedSeats: Set<String> = emptySet()): Map<String, Actor> {
        val surfaces = GameSurfaces.painted(scene)
        fun safe(pos: PlayControl.Pos) = GameAdventure.safePosition(scene.place, pos, surfaces)
        val here = snapshots.filter { it.publiclyPresent && it.place == scene.place }.associateBy { it.profileId }
        val ids = (actors.keys + here.keys).sorted()
        val occupied = mutableListOf<PlayControl.Pos>()
        val seated = mutableSetOf<String>()
        val previouslySeated = actors.values.mapNotNull { it.seat?.piece?.id }.toSet()
        return ids.mapNotNull { id ->
            val snapshot = here[id] ?: actors[id]?.snapshot ?: return@mapNotNull null
            val index = LivingResidents.all.indexOfFirst { it.profileId == id }.coerceAtLeast(0)
            val old = actors[id] ?: Actor(snapshot, safe(PlayControl.Pos(.025f, .5f)))
            val leaving = id !in here
            val unavailable = (previouslySeated + seated + reservedSeats) - setOfNotNull(old.seat?.piece?.id)
            val preferred = when (snapshot.currentAction) {
                ActionKind.READ -> listOf(Station.BOOKSHELF, Station.BENCH, Station.SEAT)
                ActionKind.REST, ActionKind.SETTLE -> listOf(Station.BENCH, Station.SEAT, Station.BED)
                ActionKind.CREATE -> listOf(Station.CRAFT, Station.DESK)
                ActionKind.CONCENTRATE, ActionKind.WORK -> listOf(Station.WORKPLACE, Station.DESK)
                ActionKind.EAT -> listOf(Station.TABLE, Station.BENCH)
                else -> emptyList()
            }
            val station = preferred.firstNotNullOfOrNull { kind -> scene.spots.firstOrNull { it.station == kind } }
            val destination = when {
                id == talking -> old.pos
                leaving -> PlayControl.Pos(.975f, .5f)
                station != null -> GameSeating.approach(scene, station.station, unavailable) ?: GameScenes.posAt(scene, station.standX, station.standY)
                else -> PlayControl.Pos(if ((clock / 25_000L + index) % 2L == 0L) .25f + index * .055f else .70f - index * .035f,
                    .35f + (index % 3) * .2f)
            }
            val origin = safe(destination)
            val target = if (leaving || id == talking) origin else {
                val candidates = sequenceOf(0f, .13f, -.13f, .26f, -.26f).flatMap { dx ->
                    sequenceOf(0f, .18f, -.18f).map { dd -> safe(PlayControl.Pos(origin.x + dx, origin.depth + dd)) }
                }
                candidates.firstOrNull { candidate -> occupied.all { other ->
                    val dx = candidate.x - other.x; val dd = (candidate.depth - other.depth) * .25f
                    dx * dx + dd * dd > .009f
                } } ?: origin
            }
            occupied += target
            val wantsSeat = !leaving && id != talking && station != null && old.pos == target
            var seat = old.seat
            if (seat != null && (!wantsSeat || station!!.station !in seat.piece.seat!!.stations) && seat.rise == null)
                seat = seat.copy(rise = clock)
            if (seat != null && GameSeating.frame(scene, snapshot.species, seat, clock).complete) seat = null
            if (seat == null && wantsSeat) seat = GameSeating.begin(scene, station!!.station, old.pos, clock, unavailable)
            seat?.let { seated += it.piece.id }
            val pos = if (seat != null) old.pos else GameSurfaces.approach(scene, old.pos, target, (millis / 2L).coerceAtLeast(0L))
            if (leaving && pos == target && seat == null) null else id to Actor(snapshot, pos, pos != old.pos, leaving,
                if (pos != old.pos) PlayControl.swipeDir(pos.x - old.pos.x, (pos.depth - old.pos.depth) * .25f, 0f) ?: old.facing else old.facing,
                seat)
        }.toMap()
    }
}
