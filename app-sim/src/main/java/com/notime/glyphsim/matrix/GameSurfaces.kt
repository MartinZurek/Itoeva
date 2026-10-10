package com.notime.glyphsim.matrix

/** Nur wirkliche, gezeichnete Standflaechen; Trefferrechtecke von Tueren sind keine Plattformen. */
object GameSurfaces {
    data class Crate(val surface: GameMovement.Surface, val left: Float, val right: Float,
        val top: Float, val bottom: Float)

    /** Eine niedrige Entdeckerkiste im freien vorderen Parkbereich. Bild und Kollision teilen die Masse. */
    fun crate(scene: GameScenes.Scene): Crate? {
        if (scene.place != PlayScene.Place.PARK) return null
        val center = PlayControl.Pos(0.72f, 0.58f)
        val (x, ground) = GameScenes.feet(scene, center)
        val left = x - 19f
        val right = x + 19f
        val p0 = GameScenes.posAt(scene, left, ground)
        val p1 = GameScenes.posAt(scene, right, ground)
        return Crate(GameMovement.Surface("park-crate", p0.x, p1.x, 0.51f, 0.64f, 20f, scene.nearY - scene.farY, 0.58f),
            left, right, ground - 20f, ground)
    }

    fun painted(scene: GameScenes.Scene): List<GameMovement.Surface> {
        if (GameWorld.isWorld(scene)) {
            val measured = GameFurniture.pieces(scene)
            return (if (measured.isNotEmpty()) measured.map { it.surface(scene) } else GameWorld.surfaces(scene)) +
                listOfNotNull(crate(scene)?.surface)
        }
        if (scene.asset.startsWith("interiors/")) {
            return GameFurniture.pieces(scene).map { it.surface(scene) }
        }
        val table = if (scene.place == PlayScene.Place.LIVING) {
            // Der niedrige Teetisch links vor dem Sofa im bestehenden living.png.
            val ground = 212f
            val left = GameScenes.posAt(scene, 148f, ground)
            val right = GameScenes.posAt(scene, 204f, ground)
            GameMovement.Surface("living-tea-table", left.x, right.x, 0.04f, 0.36f,
                ground - 184f, scene.nearY - scene.farY, left.depth)
        } else null
        fun platform(id: String, left: Float, right: Float, top: Float, ground: Float,
            back: Float, front: Float): GameMovement.Surface {
            val anchor = GameScenes.posAt(scene, (left + right) / 2f, ground)
            return GameMovement.Surface(id, GameScenes.posAt(scene, left, ground).x,
                GameScenes.posAt(scene, right, ground).x, GameScenes.posAt(scene, left, back).depth,
                GameScenes.posAt(scene, right, front).depth, ground - top,
                scene.nearY - scene.farY, anchor.depth)
        }
        // Oberkanten aus den ausgelieferten Bildern. Die Hoehe haelt die Fusslinie bei Tiefe fest.
        val seats = when (scene.place) {
            PlayScene.Place.LIVING -> listOf(platform("living-sofa", 211f, 292f, 185f, 212f, 208f, 215f))
            PlayScene.Place.BEDROOM -> listOf(platform("bedroom-bed", 234f, 342f, 189f, 224f, 217f, 230f))
            PlayScene.Place.PARK -> listOf(platform("park-bench", 87f, 191f, 166f, 196f, 186f, 201f))
            PlayScene.Place.FOREST -> listOf(platform("forest-log", 285f, 439f, 195f, 231f, 227f, 235f))
            PlayScene.Place.CAMP -> listOf(platform("camp-log", 191f, 303f, 202f, 230f, 221f, 236f))
            PlayScene.Place.BEACH -> listOf(platform("beach-chair", 335f, 375f, 183f, 213f, 204f, 218f))
            PlayScene.Place.SWAMP -> listOf(platform("swamp-dock", 218f, 362f, 200f, 224f, 217f, 229f))
            else -> emptyList()
        }
        return listOfNotNull(crate(scene)?.surface, table) + seats
    }

    /** Kurzer Sichtbarkeitsweg um die vorhandenen Kollisionsrechtecke zum angewaehlten Platz. */
    fun approach(scene: GameScenes.Scene, from: PlayControl.Pos, target: PlayControl.Pos, dt: Long): PlayControl.Pos {
        val obstacles = painted(scene).filterNot { it.contains(from) }
        if (obstacles.isEmpty()) return GameTerrain.clamp(scene,GameScenes.approach(from, target, dt))
        fun clear(a: PlayControl.Pos, b: PlayControl.Pos): Boolean = obstacles.none { s ->
            var lo = 0f
            var hi = 1f
            fun cut(origin: Float, delta: Float, low: Float, high: Float): Boolean {
                if (kotlin.math.abs(delta) < .00001f) return origin in low..high
                val t0 = (low - origin) / delta
                val t1 = (high - origin) / delta
                lo = maxOf(lo, minOf(t0, t1))
                hi = minOf(hi, maxOf(t0, t1))
                return lo <= hi
            }
            cut(a.x, b.x - a.x, s.x0, s.x1) && cut(a.depth, b.depth - a.depth, s.d0, s.d1)
        }
        fun along(waypoint: PlayControl.Pos): PlayControl.Pos {
            val dx = waypoint.x - from.x
            val dy = waypoint.depth - from.depth
            val seconds = dt.coerceIn(0L, 50L) / 1000f * GameWater.drag(scene,from)
            val tx = if (kotlin.math.abs(dx) < .00001f) 1f else seconds * PlayControl.SPEED_X / kotlin.math.abs(dx)
            val ty = if (kotlin.math.abs(dy) < .00001f) 1f else seconds * PlayControl.SPEED_DEPTH * .5f / kotlin.math.abs(dy)
            val t = minOf(1f, tx, ty)
            val next=if (t == 1f) waypoint else PlayControl.Pos(from.x + dx * t, from.depth + dy * t)
            return GameTerrain.clamp(scene,next)
        }
        if (clear(from, target)) return along(target)
        val nodes = mutableListOf(from, target)
        obstacles.forEach { s ->
            for (x in listOf(s.x0 - .012f, s.x1 + .012f)) {
                for (d in listOf(s.d0 - .04f, s.d1 + .04f)) {
                    if (x in 0f..1f && d in 0f..1f && GameTerrain.valid(scene, PlayControl.Pos(x,d))) {
                        val p = PlayControl.Pos(x, d)
                        if (obstacles.none { it.contains(p) }) nodes += p
                    }
                }
            }
        }
        val distances = FloatArray(nodes.size) { Float.POSITIVE_INFINITY }.apply { this[0] = 0f }
        val previous = IntArray(nodes.size) { -1 }
        val visited = BooleanArray(nodes.size)
        repeat(nodes.size) {
            val u = nodes.indices.filterNot { visited[it] }.minByOrNull { distances[it] } ?: return@repeat
            if (!distances[u].isFinite()) return@repeat
            visited[u] = true
            nodes.indices.filterNot { visited[it] }.forEach { v ->
                if (clear(nodes[u], nodes[v])) {
                    val dx = nodes[u].x - nodes[v].x
                    val dy = (nodes[u].depth - nodes[v].depth) * .25f
                    val candidate = distances[u] + kotlin.math.sqrt(dx * dx + dy * dy)
                    if (candidate < distances[v]) { distances[v] = candidate; previous[v] = u }
                }
            }
        }
        if (previous[1] < 0) return from
        var first = 1
        while (previous[first] > 0) first = previous[first]
        // Parametrischer Schritt bleibt auf der geprueften Kante; getrennte Achsen koennten eine Ecke schneiden.
        return along(nodes[first])
    }

    /**
     * Die Tischplatte aus genau den Zellen der angepassten Kulisse bestimmen.
     * Kanne, Monitor und Tischfuesse sind keine Standflaeche. Keine neue Weltpipeline.
     */
    fun tables(place: PlayScene.Place, species: AvatarSpecies, widthCells: Int, floorY: Int,
        widthPx: Float, avatarPx: Float, cellPx: Float, depthBandPx: Float): List<GameMovement.Surface> {
        if (widthPx <= avatarPx || avatarPx <= 0f || cellPx <= 0f) return emptyList()
        return listOf(PlayScene.Station.TABLE, PlayScene.Station.DESK).mapNotNull { station ->
            val cells = PlayScene.propCellsAt(place, station, widthCells, floorY, species)
            if (cells.isEmpty()) return@mapNotNull null
            val fullWidth = cells.maxOf { it.first } - cells.minOf { it.first } + 1
            val row = cells.groupBy { it.second }.entries.filter { it.value.size >= fullWidth * 0.7f }
                .minByOrNull { it.key } ?: return@mapNotNull null
            fun x(cell: Int) = ((cell * cellPx - avatarPx / 2f) / (widthPx - avatarPx)).coerceIn(0f, 1f)
            val x0 = x(row.value.minOf { it.first } + 1)
            val x1 = x(row.value.maxOf { it.first })
            if (x1 <= x0) return@mapNotNull null
            val height = ((floorY + 1 - row.key) * cellPx + depthBandPx * 0.07f) * 64f / avatarPx
            GameMovement.Surface("table-${station.name}", x0, x1, 0f, 0.14f, height, depthBandPx * 64f / avatarPx, 0.07f)
        }
    }
}
