package com.notime.glyphsim.matrix

/** Sitzablauf bewegt den Hueftpunkt auf die Sitzflaeche, nicht die Fuesse auf den Boden davor. */
object GameSeating {
    const val ENTER_MS = 420L
    const val RISE_MS = 360L
    data class State(val piece: GameFurniture.Piece, val from: PlayControl.Pos, val start: Long,
        val rise: Long? = null)
    data class Frame(val pos: PlayControl.Pos, val lift: Float, val receiver: Float,
        val motion: CreatureSprites.MotionCue?, val complete: Boolean)

    private fun choice(scene: GameScenes.Scene, station: PlayScene.Station, unavailable: Set<String>): GameFurniture.Piece? {
        val spot = scene.spots.firstOrNull { it.station == station } ?: return null
        return GameFurniture.pieces(scene).filter { it.seat?.stations?.contains(station) == true && it.id !in unavailable }
            .minByOrNull { kotlin.math.abs(it.seat!!.x - spot.standX) }
    }
    fun approach(scene: GameScenes.Scene, station: PlayScene.Station, unavailable: Set<String> = emptySet()): PlayControl.Pos? {
        val piece = choice(scene, station, unavailable) ?: return null
        return GameAdventure.safePosition(scene.place, GameScenes.posAt(scene, piece.seat!!.x, piece.front + 12f),
            GameSurfaces.painted(scene))
    }
    fun begin(scene: GameScenes.Scene, station: PlayScene.Station, from: PlayControl.Pos,
        clock: Long, unavailable: Set<String> = emptySet()): State? =
        choice(scene, station, unavailable)?.let { State(it, from, clock) }

    fun frame(scene: GameScenes.Scene, species: AvatarSpecies, state: State, clock: Long): Frame {
        val seat = requireNotNull(state.piece.seat)
        val p = if (state.rise != null) ((clock - state.rise).toFloat() / RISE_MS).coerceIn(0f, 1f)
            else ((clock - state.start).toFloat() / ENTER_MS).coerceIn(0f, 1f)
        val ease = p * p * (3f - 2f * p)
        val blend = if (state.rise != null) 1f - ease else ease
        val target = GameScenes.posAt(scene, seat.x, state.piece.ground)
        val pos = PlayControl.Pos(state.from.x + (target.x - state.from.x) * blend,
            state.from.depth + (target.depth - state.from.depth) * blend)
        val body = GameCharacterScale.visibleHeight(scene, target, species)
        val leg = body * when (species) {
            AvatarSpecies.GLOOP -> .025f
            AvatarSpecies.STARLET -> .055f
            AvatarSpecies.PUFFLING -> .10f
            AvatarSpecies.HOOTLET -> .12f
            else -> .18f
        }
        val receiver = state.piece.ground - seat.y
        val lift = (receiver - leg).coerceAtLeast(0f) * blend
        val motion = if (state.rise != null || p < 1f) CreatureSprites.MotionCue(
            if (state.rise != null) CreatureSprites.Motion.RISE else CreatureSprites.Motion.SIT, p)
            else null
        return Frame(pos, lift, receiver * blend, motion, state.rise != null && p >= 1f)
    }
}
