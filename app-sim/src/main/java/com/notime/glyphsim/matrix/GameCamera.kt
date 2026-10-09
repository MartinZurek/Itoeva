package com.notime.glyphsim.matrix

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max

/** Eine ruhige, bildratenunabhaengige Kamera fuer Bild, Fusslinie und alle Trefferflaechen. */
object GameCamera {
    data class State(val key: String = "", val x: Float = 0f, val y: Float = 0f,
        val zoom: Float = 1f, val look: Float = 0f, val close: Boolean = false)

    fun key(scene: GameScenes.Scene) = if (GameWorld.isWorld(scene)) GameWorld.CAMERA_KEY else scene.asset
    private fun width(scene: GameScenes.Scene) = if (GameWorld.isWorld(scene)) GameWorld.width(scene) else 480f
    private fun height(scene: GameScenes.Scene) = if (GameWorld.isWorld(scene)) GameWorld.height(scene) else 270f
    private fun base(scene: GameScenes.Scene, w: Float, h: Float) = if (GameWorld.isWorld(scene))
        max(h / GameWorld.height(scene), w / GameWorld.width(scene)) else max(w / 480f, h / 270f)

    fun tick(old: State, scene: GameScenes.Scene, pos: PlayControl.Pos, input: PlayControl.Stick,
        screenW: Float, screenH: Float, dtMs: Long, interest: Float = Float.POSITIVE_INFINITY): State {
        if (screenW <= 0f || screenH <= 0f) return old
        val world = GameWorld.isWorld(scene)
        val (localX, fy) = GameScenes.feet(scene, pos)
        val fx = localX + if (world) GameWorld.origin(scene.place) else 0f
        val fresh = old.key != key(scene)
        // Unterschiedliche Ein- und Austrittsschwellen verhindern Zoom-Pumpen an einer Bank.
        val close = if (fresh || !old.close) interest < 42f else interest < 72f
        val water = GameWorld.wetness(scene, pos)
        val zoomTarget = if (world) when { water > .05f -> 1.55f + water * .65f; close -> 1.85f; input.strength > .80f -> 1.40f; else -> 1.55f }
            else 1.0f
        val dt = dtMs.coerceIn(0L, 100L) / 1000f
        fun ease(a: Float, b: Float, seconds: Float) = a + (b - a) * (1f - exp(-dt / seconds))
        val zoom = if (fresh) zoomTarget else ease(old.zoom, zoomTarget, .65f)
        val scale = base(scene, screenW, screenH) * zoom
        val halfW = screenW / scale / 2f
        val halfH = screenH / scale / 2f
        val lookTarget = input.x * if (world) 48f else 14f
        val look = if (fresh) 0f else ease(old.look, lookTarget, .30f)
        val tx = (fx + look).coerceIn(halfW, width(scene) - halfW)
        val ty = (fy - screenH / scale * (.24f + water * .12f)).coerceIn(halfH, height(scene) - halfH)
        val x = if (fresh) tx else ease(old.x, tx, .18f).coerceIn(halfW, width(scene) - halfW)
        val y = if (fresh) ty else ease(old.y, ty, .24f).coerceIn(halfH, height(scene) - halfH)
        return State(key(scene), x, y, zoom, look, close)
    }

    fun fit(state: State, scene: GameScenes.Scene, w: Float, h: Float): GameScenes.Fit {
        if (w <= 0f || h <= 0f || state.key != key(scene)) {
            if (w <= 0f || h <= 0f) return GameScenes.Fit(1f, 0f, 0f)
            return fit(tick(State(), scene, PlayControl.Pos(), PlayControl.Stick(), w, h, 0L), scene, w, h)
        }
        val scale = base(scene, w, h) * state.zoom
        val offset = if (GameWorld.isWorld(scene)) GameWorld.origin(scene.place) else 0f
        val cx = state.x.coerceIn(w / scale / 2f, width(scene) - w / scale / 2f)
        val cy = state.y.coerceIn(h / scale / 2f, height(scene) - h / scale / 2f)
        return GameScenes.Fit(scale, w / 2f + (offset - cx) * scale, h / 2f - cy * scale)
    }

    fun interest(scene: GameScenes.Scene, pos: PlayControl.Pos): Float {
        val (x, y) = GameScenes.feet(scene, pos)
        return scene.spots.minOfOrNull { abs(it.standX - x) + abs(it.standY - y) } ?: Float.POSITIVE_INFINITY
    }
}
