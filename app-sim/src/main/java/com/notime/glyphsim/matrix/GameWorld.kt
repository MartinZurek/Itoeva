package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayControl.Dir
import com.notime.glyphsim.matrix.PlayControl.Pos
import com.notime.glyphsim.matrix.PlayScene.Place
import com.notime.glyphsim.matrix.PlayScene.Station

/** Ein gemeinsamer Boden fuer den ersten Erkundungsweg; Orts-IDs bleiben speicherbar. */
object GameWorld {
    const val ASSET = "world/street-park-forest.png"
    const val WIDTH = 1920f
    const val HEIGHT = 640f
    const val SECTION = 480f
    val places = listOf(Place.STREET, Place.PARK, Place.MEADOW, Place.FOREST)

    fun contains(place: Place) = place in places
    fun isWorld(scene: GameScenes.Scene) = scene.asset == ASSET
    fun origin(place: Place) = places.indexOf(place).coerceAtLeast(0) * SECTION
    fun connected(from: Place, to: Place) = contains(from) && contains(to)
    fun immediateExits(place: Place): Set<Dir> = scene(place)?.exits.orEmpty()
        .filter { (dir, next) -> (dir == Dir.LEFT || dir == Dir.RIGHT) && next?.let { connected(place, it) } == true }.keys

    private val scenes = places.associateWith { place ->
        val exits = when (place) {
            Place.STREET -> mapOf(Dir.LEFT to Place.LIVING, Dir.RIGHT to Place.PARK, Dir.UP to Place.CITY, Dir.DOWN to Place.SHOP)
            Place.PARK -> mapOf(Dir.LEFT to Place.STREET, Dir.RIGHT to Place.MEADOW, Dir.UP to Place.SPORT, Dir.DOWN to Place.POND)
            Place.MEADOW -> mapOf(Dir.LEFT to Place.PARK, Dir.RIGHT to Place.FOREST, Dir.UP to Place.PLAINS, Dir.DOWN to Place.POND)
            else -> mapOf(Dir.LEFT to Place.MEADOW, Dir.RIGHT to Place.MOUNTAINS, Dir.UP to Place.CAMP, Dir.DOWN to Place.SWAMP)
        }
        val spots = when (place) {
            Place.STREET -> listOf(GameScenes.Spot(Station.LAMP, GameScenes.Box(428f, 238f, 457f, 500f), 442f, 530f))
            Place.PARK -> listOf(GameScenes.Spot(Station.BENCH, GameScenes.Box(180f, 414f, 357f, 501f), 267f, 526f))
            Place.FOREST -> listOf(GameScenes.Spot(Station.BENCH, GameScenes.Box(75f, 443f, 370f, 513f), 222f, 532f))
            else -> emptyList()
        }
        val left = if (place == Place.STREET) 100f else 0f
        val right = if (place == Place.FOREST) SECTION - 100f else SECTION
        GameScenes.Scene(place, ASSET, 510f, 556f, left, right, left, right,
            90f, 108f, spots, exits)
    }

    /** Nur der aktive Game-Client benutzt diese Geometrie, App 1 und Stream ihren Katalog. */
    fun scene(place: Place): GameScenes.Scene? {
        scenes[place]?.let { return it }
        val original = GameScenes.of(place) ?: return null
        val reverse = when (place) {
            Place.CITY -> mapOf(Dir.DOWN to Place.STREET)
            Place.SPORT -> mapOf(Dir.DOWN to Place.PARK)
            Place.PLAINS -> mapOf(Dir.DOWN to Place.MEADOW)
            Place.CAMP -> mapOf(Dir.DOWN to Place.FOREST)
            Place.SWAMP -> mapOf(Dir.UP to Place.FOREST)
            else -> emptyMap()
        }
        return if (reverse.isEmpty()) original else original.copy(exits = original.exits + reverse)
    }
    fun feet(place: Place, pos: Pos): Pair<Float, Float> {
        val scene = scenes.getValue(place)
        val (x, y) = GameScenes.feet(scene, pos)
        return (origin(place) + x) to y
    }

    /** Am gemeinsamen Rand kein Sicherheitsabstand, keine neue Gangphase und keine Tuerblende. */
    fun transfer(from: Place, to: Place, dir: Dir, state: GameMovement.State): GameMovement.State? {
        if (!connected(from, to) || kotlin.math.abs(places.indexOf(from) - places.indexOf(to)) != 1 ||
            (dir != Dir.LEFT && dir != Dir.RIGHT) || scenes.getValue(from).exits[dir] != to ||
            state.height > 0f || state.support != null || state.action != null) return null
        return state.copy(pos = PlayControl.entry(dir, state.pos).copy(pushMs = 0L), support = null, height = 0f)
    }

    fun surfaces(scene: GameScenes.Scene): List<GameMovement.Surface> = scene.spots
        .filter { it.station == Station.BENCH }.map { spot ->
            val a = GameScenes.posAt(scene, spot.hit.x0 + 10f, spot.standY - 25f)
            val b = GameScenes.posAt(scene, spot.hit.x1 - 10f, spot.standY - 12f)
            GameMovement.Surface("world-${scene.place.name}-seat", a.x, b.x, a.depth, b.depth,
                if (scene.place == Place.PARK) 47f else 35f, scene.nearY - scene.farY, a.depth)
        }

    /** Ein Profil darf beim Wechsel zwischen sichtbaren Abschnitten nie doppelt erscheinen. */
    fun residents(old: Map<Place, Map<String, GameResidents.Actor>>, snapshots: List<ResidentSnapshot>,
        dt: Long, clock: Long, talking: String?): Map<Place, Map<String, GameResidents.Actor>> {
        val present = snapshots.filter { it.publiclyPresent && it.place in places }.associate { it.profileId to it.place }
        return places.associateWith { place ->
            val local = old[place].orEmpty().filterKeys { id -> present[id] == null || present[id] == place }
            GameResidents.tick(scene(place)!!, local, snapshots, dt, clock, talking)
        }
    }

    /** Richtungen der ersten Gegend sind physische Ausgaenge; andere Orte behalten ihre Wege. */
    fun neighbors(place: Place): List<Place> {
        val scene = scene(place)
        return (Dir.entries.mapNotNull { dir -> scene?.let { GameScenes.exit(it, dir) }
            ?: if (scene == null) PlayControl.neighbor(place, dir) else null } + listOfNotNull(scene?.door)).distinct()
    }

    fun route(from: Place, to: Place): List<Place> {
        val queue = ArrayDeque(listOf(from))
        val previous = mutableMapOf<Place, Place?> (from to null)
        while (queue.isNotEmpty()) {
            val here = queue.removeFirst()
            if (here == to) break
            for (next in neighbors(here)) if (next !in previous) { previous[next] = here; queue.add(next) }
        }
        if (to !in previous) return emptyList()
        val path = mutableListOf<Place>()
        var here = to
        while (here != from) { path += here; here = previous.getValue(here) ?: break }
        return path.reversed()
    }

    /** Die Uebersicht zeigt die neuen gemeinsamen Wege statt der alten LED-Zeichen. */
    val mapPositions: Map<Place, Pair<Float, Float>> = mapOf(
        Place.LIVING to (.045f to .51f), Place.STREET to (.20f to .51f), Place.PARK to (.42f to .51f),
        Place.MEADOW to (.62f to .51f), Place.FOREST to (.80f to .51f),
        Place.CITY to (.20f to .27f), Place.SHOP to (.20f to .79f),
        Place.WORK to (.055f to .095f), Place.CAFE to (.20f to .095f), Place.ARCADE to (.34f to .095f),
        Place.SPORT to (.43f to .23f), Place.POND to (.49f to .79f), Place.PLAINS to (.65f to .22f),
        Place.CAMP to (.82f to .20f), Place.MOUNTAINS to (.95f to .50f), Place.GROTTO to (.96f to .10f),
        Place.SWAMP to (.78f to .78f), Place.JUNGLE to (.94f to .84f), Place.BEACH to (.65f to .93f)
    )
    fun shownAs(place: Place) = if (PlayMap.regionOf(place) == PlayMap.Region.HOME) Place.LIVING else place
}
