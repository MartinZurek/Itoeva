package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayControl.Dir
import com.notime.glyphsim.matrix.PlayControl.Pos
import com.notime.glyphsim.matrix.PlayScene.Place
import com.notime.glyphsim.matrix.PlayScene.Station
import kotlin.math.abs
import kotlin.math.sin

/** Die Game-Geografie ist vollstaendig: kein versteckter Rueckfall auf das alte Kartenraster. */
object GameWorld {
    const val ASSET = "world/street-park-forest.png"
    const val WIDTH = 1920f
    const val HEIGHT = 640f
    const val SECTION = 480f
    data class Region(val asset: String, val places: List<Place>, val width: Float = WIDTH) {
        val section get() = width / places.size
    }
    val regions = listOf(
        Region("world/coast.png", listOf(Place.POND, Place.BEACH, Place.SWAMP, Place.JUNGLE)),
        Region("world/coast-path.png", listOf(Place.COAST_PATH)),
        Region(ASSET, listOf(Place.STREET, Place.PARK, Place.MEADOW, Place.FOREST)),
        Region("world/village-edge.png", listOf(Place.VILLAGE_EDGE)),
        Region("world/uplands.png", listOf(Place.CITY, Place.SPORT, Place.PLAINS, Place.MOUNTAINS)),
        Region("world/mountain-pass.png", listOf(Place.MOUNTAIN_PASS)),
        Region("world/expedition.png", listOf(Place.CAMP, Place.GROTTO)))
    val transitionPlaces = setOf(Place.COAST_PATH, Place.VILLAGE_EDGE, Place.MOUNTAIN_PASS)
    val places = regions.flatMap { it.places }
    val home = listOf(Place.BEDROOM, Place.BATH, Place.DESK, Place.NOOK, Place.LIVING, Place.KITCHEN, Place.CRAFT)
    fun region(place: Place) = regions.firstOrNull { place in it.places }
    const val CAMERA_KEY = "mainland-panorama-v3"
    val totalWidth get() = regions.sumOf { it.width.toDouble() }.toFloat()
    fun regionOrigin(region: Region) = regions.take(regions.indexOf(region)).sumOf { it.width.toDouble() }.toFloat()
    /** Nachgemalte Anschluesse erhalten beide Nachbarbilder und ihre Weltanker. */
    data class Seam(val left: Region, val right: Region, val x: Float, val asset: String)
    val seams by lazy { regions.zipWithNext().mapIndexed { i, (a, b) ->
        Seam(a, b, regionOrigin(b), "world/seams/$i.png") } }
    const val SEAM_HALF = 480f
    const val SEAM_FEATHER = 128f
    fun seamAlpha(offset: Float): Float {
        val t = ((SEAM_HALF - abs(offset)) / SEAM_FEATHER).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
    fun horizontalScale(place: Place) = region(place)?.let { SECTION / it.section } ?: 1f
    fun visiblePlaces(scene: GameScenes.Scene) = if (isWorld(scene)) places else listOf(scene.place)
    fun contains(place: Place) = region(place) != null
    fun isWorld(scene: GameScenes.Scene) = region(scene.place)?.asset == scene.asset
    fun origin(place: Place) = region(place)?.let { regionOrigin(it) + it.places.indexOf(place) * it.section } ?: 0f
    fun width(scene: GameScenes.Scene) = if (isWorld(scene)) totalWidth else 480f
    fun height(scene: GameScenes.Scene) = if (isWorld(scene)) HEIGHT else 270f
    fun connected(from: Place, to: Place) = contains(from) && contains(to)
    fun opposite(dir: Dir) = when (dir) { Dir.LEFT -> Dir.RIGHT; Dir.RIGHT -> Dir.LEFT; Dir.UP -> Dir.DOWN; Dir.DOWN -> Dir.UP }

    /** Jeder Eingang besitzt einen eigenen Fussanker und genau einen gegenueberliegenden Rueckweg. */
    data class Passage(val from: Place, val to: Place, val dir: Dir, val pos: Pos, val door: Boolean = false) {
        fun hit(scene: GameScenes.Scene): GameScenes.Box {
            val (x, y) = GameScenes.feet(scene, pos)
            return if (door) GameDoors.aperture(from,to) else GameScenes.Box(x - 22f,y - 20f,x + 22f,y + 12f)
        }
    }
    private val links = mutableListOf<Passage>().apply {
        fun join(a: Place, b: Place, d: Dir, pa: Pos, pb: Pos, door: Boolean = false) {
            add(Passage(a, b, d, if (door) GameDoors.anchor(a,b) else pa, door)); add(Passage(b, a, opposite(d), if (door) GameDoors.anchor(b,a) else pb, door))
        }
        for (r in regions) for ((a, b) in r.places.zipWithNext())
            join(a, b, Dir.RIGHT, Pos(1f, .55f), Pos(0f, .55f))
        // Nur benachbarte Bilder teilen eine Naht: Tiefe ist Bewegung im Ort, kein Kartenraster.
        for ((a, b) in regions.zipWithNext())
            join(a.places.last(), b.places.first(), Dir.RIGHT, Pos(1f, .55f), Pos(0f, .55f))
        join(Place.LIVING, Place.BEDROOM, Dir.UP, Pos(.44f, .03f), Pos(.94f, .25f), true)
        join(Place.LIVING, Place.KITCHEN, Dir.RIGHT, Pos(.94f, .25f), Pos(.06f, .25f), true)
        join(Place.LIVING, Place.NOOK, Dir.UP, Pos(.62f, .03f), Pos(.06f, .25f), true)
        join(Place.BEDROOM, Place.BATH, Dir.UP, Pos(.65f, .08f), Pos(.06f, .25f), true)
        join(Place.BEDROOM, Place.DESK, Dir.LEFT, Pos(.06f, .25f), Pos(.06f, .25f), true)
        join(Place.KITCHEN, Place.CRAFT, Dir.RIGHT, Pos(.94f, .25f), Pos(.06f, .25f), true)
        join(Place.STREET, Place.LIVING, Dir.LEFT, Pos(.20f, .30f), Pos(.06f, .25f), true)
        join(Place.STREET, Place.SHOP, Dir.UP, Pos(.36f, .25f), Pos(.06f, .25f), true)
        join(Place.CITY, Place.WORK, Dir.UP, Pos(.12f, .25f), Pos(.06f, .25f), true)
        join(Place.CITY, Place.CAFE, Dir.UP, Pos(.36f, .25f), Pos(.06f, .25f), true)
        join(Place.CITY, Place.ARCADE, Dir.UP, Pos(.76f, .25f), Pos(.06f, .25f), true)
    }.toList()
    fun passages(place: Place) = links.filter { it.from == place }
    fun passageAt(scene: GameScenes.Scene, x: Float, y: Float) = passages(scene.place)
        .filter { it.door && (x to y) in it.hit(scene) }.minByOrNull { abs(GameScenes.feet(scene, it.pos).first - x) }
    fun inReach(place: Place, pos: Pos) = passages(place).filter { it.door }
        .minByOrNull { abs(it.pos.x - pos.x) + abs(it.pos.depth - pos.depth) * .4f }
        ?.takeIf { abs(it.pos.x - pos.x) < .10f && abs(it.pos.depth - pos.depth) < .22f }
    fun arrival(passage: Passage): Pos = passages(passage.to).first { it.to == passage.from }.pos.let {
        // Ein kleiner Abstand zum Rand verhindert sofortiges Zurueckspringen bei gehaltenem Daumen.
        it.copy(x = it.x.coerceIn(.06f, .94f), depth = it.depth.coerceIn(.12f, .88f), pushMs = 0L)
    }
    fun exitAt(place: Place, dir: Dir, pos: Pos): Place? = passages(place)
        .firstOrNull { p -> p.dir == dir && !p.door &&
            (connected(place, p.to) || abs(pos.x - p.pos.x) <= .13f) }?.to

    fun immediateExits(place: Place): Set<Dir> = passages(place).filter { !it.door && connected(place, it.to) }.map { it.dir }.toSet()

    private fun exits(place: Place): Map<Dir, Place?> = Dir.entries.associateWith { d ->
        passages(place).firstOrNull { it.dir == d && !it.door }?.to
    }
    private fun seat(x: Float = 240f) = GameScenes.Spot(Station.BENCH,
        GameScenes.Box(x - 65f, 440f, x + 65f, 510f), x, 538f)
    private val scenes by lazy {
        Place.entries.associateWith { place ->
            val r = region(place)
            if (r != null) {
                val spots = when (place) {
                    Place.STREET -> listOf(GameScenes.Spot(Station.LAMP, GameScenes.Box(428f,238f,457f,500f),442f,530f))
                    Place.PARK -> listOf(GameScenes.Spot(Station.BENCH,GameScenes.Box(180f,414f,357f,501f),267f,526f))
                    Place.FOREST -> listOf(GameScenes.Spot(Station.BENCH,GameScenes.Box(24f,468f,240f,518f),120f,533f))
                    Place.CITY -> emptyList()
                    Place.MOUNTAINS -> listOf(seat(275f))
                    Place.CAMP -> listOf(GameScenes.Spot(Station.BENCH,GameScenes.Box(202.5f,472f,365f,520f),282.5f,537f))
                    Place.BEACH, Place.SWAMP, Place.GROTTO -> emptyList()
                    else -> emptyList()
                }
                val first = place == places.first(); val last = place == places.last()
                val factor = r.section / SECTION
                val left = if (first) 65f * factor else 0f; val right = if (last) r.section - 65f * factor else r.section
                val scaledSpots = spots.map { it.copy(hit = it.hit.copy(x0 = it.hit.x0 * factor, x1 = it.hit.x1 * factor), standX = it.standX * factor) }
                GameScenes.Scene(place, r.asset, 450f, 625f, left, right, left, right, 65f, 108f, scaledSpots, exits(place),
                    walkBand = if (place == Place.COAST_PATH) listOf(
                        GameScenes.WalkBand(0f, 450f, 625f),
                        GameScenes.WalkBand(.12f, 450f, 510f),
                        GameScenes.WalkBand(.30f, 435f, 478f),
                        GameScenes.WalkBand(.43f, 408f, 449f),
                        GameScenes.WalkBand(.58f, 435f, 485f),
                        GameScenes.WalkBand(.78f, 450f, 535f),
                        GameScenes.WalkBand(1f, 450f, 625f)) else emptyList())
            } else {
                val original = GameInteriorCatalog.scenes.getValue(place)
                original.copy(exits = exits(place), spots = original.spots.filter { it.station != Station.DOOR }, door = null)
            }
        }
    }
    fun scene(place: Place): GameScenes.Scene? = scenes.getValue(place)
    fun feet(place: Place, pos: Pos): Pair<Float, Float> {
        val (x, y) = GameScenes.feet(scene(place)!!, pos)
        return origin(place) + x to y
    }
    fun transfer(from: Place, to: Place, dir: Dir, state: GameMovement.State): GameMovement.State? {
        if (!connected(from, to) || exitAt(from, dir, state.pos) != to ||
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
    fun residents(old: Map<Place, Map<String, GameResidents.Actor>>, snapshots: List<ResidentSnapshot>,
        dt: Long, clock: Long, talking: String?, reservedSeats: Set<String> = emptySet()): Map<Place, Map<String, GameResidents.Actor>> {
        val present = snapshots.filter { it.publiclyPresent && it.place in places }.associate { it.profileId to it.place }
        return places.associateWith { place ->
            val local = old[place].orEmpty().filterKeys { id -> present[id] == null || present[id] == place }
            GameResidents.tick(scene(place)!!, local, snapshots, dt, clock, talking, reservedSeats)
        }
    }
    fun neighbors(place: Place) = passages(place).map { it.to }.distinct()
    fun route(from: Place, to: Place): List<Place> {
        val queue = ArrayDeque(listOf(from)); val previous = mutableMapOf<Place, Place?>(from to null)
        while (queue.isNotEmpty()) {
            val here = queue.removeFirst(); if (here == to) break
            for (next in neighbors(here)) if (next !in previous) { previous[next] = here; queue.add(next) }
        }
        if (to !in previous) return emptyList()
        val path = mutableListOf<Place>(); var here = to
        while (here != from) { path += here; here = previous.getValue(here) ?: break }
        return path.reversed()
    }
    /** Ufer und trockene Stege folgen den Koordinaten des neuen Kuestenbildes. */
    fun shoreY(worldX: Float) = GameWater.shoreY(worldX)
    fun material(scene: GameScenes.Scene, x: Float, y: Float): GameEnvironment.Material? {
        if (!isWorld(scene)) return if (scene.place in GameInteriorCatalog.scenes) GameEnvironment.Material.WOOD else null
        if (scene.asset == "world/coast.png") {
            val wx = origin(scene.place) + x
            if (GameWater.contains(scene,x,y)) return GameEnvironment.Material.WATER
            return when (scene.place) {
                Place.POND, Place.BEACH -> GameEnvironment.Material.SAND
                Place.SWAMP -> GameEnvironment.Material.MUD
                else -> GameEnvironment.Material.GRASS
            }
        }
        return when (scene.place) {
            Place.CITY, Place.STREET, Place.GROTTO, Place.COAST_PATH -> GameEnvironment.Material.STONE
            Place.CAMP -> GameEnvironment.Material.GRASS
            else -> GameEnvironment.Material.GRASS
        }
    }
    fun wetness(scene: GameScenes.Scene, pos: Pos): Float {
        val (x,y) = GameScenes.feet(scene,pos)
        return if (material(scene,x,y) == GameEnvironment.Material.WATER)
            ((y-shoreY(origin(scene.place)+x))/170f).coerceIn(0f,.72f) else 0f
    }
    private fun mapX(worldX: Float) = .055f + .89f * worldX / totalWidth
    val mapPositions: Map<Place, Pair<Float, Float>> = buildMap {
        for (place in places) put(place, mapX(origin(place)+region(place)!!.section/2f) to .62f)
        val street = getValue(Place.STREET).first
        val city = getValue(Place.CITY).first
        put(Place.LIVING, street-.02f to .16f); put(Place.SHOP,street+.035f to .36f)
        put(Place.WORK,city-.04f to .15f); put(Place.CAFE,city to .32f); put(Place.ARCADE,city+.04f to .15f)
    }
    fun mapPoint(place: Place, pos: Pos): Pair<Float, Float> = if (contains(place))
        mapX(feet(place,pos).first) to .62f else mapPositions.getValue(shownAs(place))
    fun shownAs(place: Place) = if (place in home) Place.LIVING else place
}
