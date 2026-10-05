package com.notime.glyphsim.matrix

/** Nur wirkliche, gezeichnete Standflaechen; Trefferrechtecke von Tueren sind keine Plattformen. */
object GameSurfaces {
    data class Crate(val surface: GameMovement.Surface, val left: Float, val right: Float,
        val top: Float, val bottom: Float)

    /** Eine niedrige Entdeckerkiste im freien vorderen Parkbereich. Bild und Kollision teilen die Masse. */
    fun crate(scene: GameScenes.Scene): Crate? {
        if (scene.place != PlayScene.Place.PARK) return null
        val center = PlayControl.Pos(0.72f, 0.75f)
        val (x, ground) = GameScenes.feet(scene, center)
        val left = x - 19f
        val right = x + 19f
        val p0 = GameScenes.posAt(scene, left, ground)
        val p1 = GameScenes.posAt(scene, right, ground)
        return Crate(GameMovement.Surface("park-crate", p0.x, p1.x, 0.62f, 0.87f, 20f, scene.nearY - scene.farY, 0.75f),
            left, right, ground - 20f, ground)
    }

    fun painted(scene: GameScenes.Scene): List<GameMovement.Surface> {
        val table = if (scene.place == PlayScene.Place.LIVING) {
            // Der niedrige Teetisch links vor dem Sofa im bestehenden living.png.
            val ground = 212f
            val left = GameScenes.posAt(scene, 148f, ground)
            val right = GameScenes.posAt(scene, 204f, ground)
            GameMovement.Surface("living-tea-table", left.x, right.x, 0.04f, 0.36f,
                ground - 184f, scene.nearY - scene.farY, left.depth)
        } else null
        return listOfNotNull(crate(scene)?.surface, table)
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
