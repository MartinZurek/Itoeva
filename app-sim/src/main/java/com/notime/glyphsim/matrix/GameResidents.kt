package com.notime.glyphsim.matrix

import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.matrix.PlayScene.Station

/** Die bestehende Population bekommt Wege in der gemalten Raumgeometrie, keine zweite KI. */
object GameResidents {
    data class Actor(val snapshot: ResidentSnapshot, val pos: PlayControl.Pos,
        val moving: Boolean = false, val leaving: Boolean = false, val facing: PlayControl.Dir = PlayControl.Dir.DOWN)

    fun tick(scene: GameScenes.Scene, actors: Map<String, Actor>, snapshots: List<ResidentSnapshot>,
        millis: Long, clock: Long, talking: String?): Map<String, Actor> {
        val surfaces = GameSurfaces.painted(scene)
        fun safe(pos: PlayControl.Pos) = GameAdventure.safePosition(scene.place, pos, surfaces)
        val here = snapshots.filter { it.publiclyPresent && it.place == scene.place }.associateBy { it.profileId }
        val ids = (actors.keys + here.keys).sorted()
        val occupied = mutableListOf<PlayControl.Pos>()
        return ids.mapNotNull { id ->
            val snapshot = here[id] ?: actors[id]?.snapshot ?: return@mapNotNull null
            val index = LivingResidents.all.indexOfFirst { it.profileId == id }.coerceAtLeast(0)
            val old = actors[id] ?: Actor(snapshot, safe(PlayControl.Pos(.025f, .5f)))
            val leaving = id !in here
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
                station != null -> GameScenes.posAt(scene, station.standX, station.standY)
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
            val pos = GameSurfaces.approach(scene, old.pos, target, (millis / 2L).coerceAtLeast(0L))
            if (leaving && pos == target) null else id to Actor(snapshot, pos, pos != old.pos, leaving,
                if (pos != old.pos) PlayControl.swipeDir(pos.x - old.pos.x, (pos.depth - old.pos.depth) * .25f, 0f) ?: old.facing else old.facing)
        }.toMap()
    }
}
