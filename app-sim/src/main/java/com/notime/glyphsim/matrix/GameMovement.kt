package com.notime.glyphsim.matrix

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** Ein einziger Bildtakt fuer Daumen, Bodenkontakt und freiwillige Bewegungen. Ohne Android. */
object GameMovement {
    enum class Command { JUMP, ROLL, REST }
    enum class Action { JUMP, ROLL, SIT, REST, RISE }

    /** Hoehe in Szenenpixeln; die Flaeche benutzt dieselben Bodenkoordinaten wie [PlayControl]. */
    data class Surface(val id: String, val x0: Float, val x1: Float,
        val d0: Float, val d1: Float, val height: Float, val depthSlope: Float = 0f, val anchorDepth: Float = 0f) {
        fun heightAt(pos: PlayControl.Pos) = height + depthSlope * (pos.depth - anchorDepth)
        fun contains(pos: PlayControl.Pos) = pos.x in x0..x1 && pos.depth in d0..d1
        fun closest(pos: PlayControl.Pos) = PlayControl.Pos(
            pos.x.coerceIn(x0 + min(0.02f, (x1 - x0) / 3f), x1 - min(0.02f, (x1 - x0) / 3f)),
            pos.depth.coerceIn(d0 + min(0.02f, (d1 - d0) / 3f), d1 - min(0.02f, (d1 - d0) / 3f)))
    }

    data class Arc(val from: PlayControl.Pos, val to: PlayControl.Pos,
        val fromHeight: Float, val toHeight: Float, val surface: String?, val duration: Long = 650L,
        val apex: Float = 30f)

    data class State(
        val pos: PlayControl.Pos = PlayControl.Pos(),
        val velocity: PlayControl.Stick = PlayControl.Stick(),
        val facing: PlayControl.Dir = PlayControl.Dir.DOWN,
        val gaitMs: Double = 0.0,
        val action: Action? = null,
        val elapsed: Long = 0L,
        val arc: Arc? = null,
        val support: String? = null,
        val height: Float = 0f,
        val queuedJumpMs: Long = 0L,
        val running: Boolean = false,
        val runBlend: Float = 0f
    ) {
        val moving: Boolean get() = action == null && velocity.strength > 0.025f
        val tempo: Float get() = if (moving) PlayControl.tempo(velocity.strength) else 0f
        val progress: Float get() = when (action) {
            Action.JUMP -> elapsed.toFloat() / (arc?.duration ?: 650L)
            Action.ROLL -> elapsed / 460f
            Action.SIT -> elapsed / 380f
            Action.REST -> 1f
            Action.RISE -> elapsed / 260f
            null -> 0f
        }.coerceIn(0f, 1f)
    }
    data class Result(val state: State, val exit: PlayControl.Dir? = null)

    private fun vector(dir: PlayControl.Dir) = PlayControl.Stick(dir.dx.toFloat(), dir.dy.toFloat())
    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    private fun distance(a: PlayControl.Pos, b: PlayControl.Pos): Float {
        val x = a.x - b.x
        val y = (a.depth - b.depth) * 0.25f
        return sqrt(x * x + y * y)
    }

    /** Kleine Landekorrektur nur zu einer erreichbaren Flaeche in der gewuenschten Richtung. */
    fun jumpTarget(state: State, input: PlayControl.Stick, surfaces: List<Surface>, maxStepHeight: Float = 52f): Surface? {
        val wish = if (input.strength > 0.1f) input else vector(state.facing)
        return surfaces.filter { it.id != state.support && it.heightAt(it.closest(state.pos)) - state.height <= maxStepHeight }
            .map { it to it.closest(state.pos) }
            .filter { (_, p) -> distance(state.pos, p) <= 0.20f &&
                (p.x - state.pos.x) * wish.x + (p.depth - state.pos.depth) * wish.y * 0.25f >= -0.015f }
            .minByOrNull { (_, p) -> distance(state.pos, p) }?.first
    }

    fun command(state: State, command: Command, input: PlayControl.Stick, surfaces: List<Surface>, maxStepHeight: Float = 52f): State {
        if (state.action == Action.JUMP) {
            // Ein Tipp kurz vor der Landung wird behalten, hektisches Halten erzeugt keine Kette.
            return if (command == Command.JUMP && state.progress > 0.78f) state.copy(queuedJumpMs = 140L) else state
        }
        if (state.action == Action.ROLL || state.action == Action.RISE) return state
        if (command == Command.REST) {
            return if (state.action == Action.REST || state.action == Action.SIT)
                state.copy(action = Action.RISE, elapsed = 0L)
            else state.copy(action = Action.SIT, elapsed = 0L, velocity = PlayControl.Stick())
        }
        val wish = if (input.strength > 0.1f) input else vector(state.facing)
        if (command == Command.ROLL) return state.copy(action = Action.ROLL, elapsed = 0L,
            velocity = PlayControl.stick(wish.x, wish.y, 1f, 0f), facing = wish.direction(state.facing) ?: state.facing)
        val target = jumpTarget(state, wish, surfaces, maxStepHeight)
        val stride = PlayControl.tempo(input.strength) * 0.325f
        val freeLanding = PlayControl.Pos(
            (state.pos.x + input.x / input.strength.coerceAtLeast(0.01f) * PlayControl.SPEED_X * stride).coerceIn(0f, 1f),
            (state.pos.depth + input.y / input.strength.coerceAtLeast(0.01f) * PlayControl.SPEED_DEPTH * stride).coerceIn(0f, 1f))
        val destination = target?.closest(PlayControl.Pos(state.pos.x + wish.x * 0.12f,
            state.pos.depth + wish.y * 0.38f)) ?: collide(state.pos, freeLanding, 0f, surfaces, state.support)
        val landing = target ?: surfaces.firstOrNull { it.id == state.support && it.contains(destination) }
        return state.copy(action = Action.JUMP, elapsed = 0L, velocity = PlayControl.Stick(),
            facing = wish.direction(state.facing) ?: state.facing,
            arc = Arc(state.pos, destination, state.height, landing?.heightAt(destination) ?: 0f, landing?.id), queuedJumpMs = 0L)
    }

    /** Flaechen sind feste Gegenstaende am Boden. An ihrer Kante bleibt Bewegung tangential moeglich. */
    private fun collide(from: PlayControl.Pos, to: PlayControl.Pos, height: Float, surfaces: List<Surface>, support: String?): PlayControl.Pos {
        fun blocked(pos: PlayControl.Pos) = surfaces.any { it.id != support && it.heightAt(pos) > height + 1f && it.contains(pos) }
        if (!blocked(to)) return to
        val horizontal = to.copy(depth = from.depth, pushMs = 0L)
        if (!blocked(horizontal)) return horizontal
        val vertical = to.copy(x = from.x, pushMs = 0L)
        if (!blocked(vertical)) return vertical
        return from.copy(pushMs = 0L)
    }

    fun tick(state: State, input: PlayControl.Stick, dtMs: Long, surfaces: List<Surface> = emptyList(),
        exitDelayMs: Long = PlayControl.EXIT_PUSH_MS, immediateExits: Set<PlayControl.Dir> = emptySet(), horizontalScale: Float = 1f,
        maxStepHeight: Float = 52f, sweptJumpCollision: Boolean = false): Result {
        val dt = dtMs.coerceIn(0L, 50L)
        if (dt == 0L) return Result(state)
        val elapsed = state.elapsed + dt
        when (state.action) {
            Action.JUMP -> {
                val arc = state.arc ?: return Result(state.copy(action = null, height = 0f, support = null))
                val t = (elapsed.toFloat() / arc.duration).coerceIn(0f, 1f)
                val flight = if (arc.apex > 0f) ((t - 0.15f) / 0.70f).coerceIn(0f, 1f) else t
                val pos = PlayControl.Pos(lerp(arc.from.x, arc.to.x, flight), lerp(arc.from.depth, arc.to.depth, flight))
                if (sweptJumpCollision && arc.apex > 0f) {
                    val nextHeight=lerp(arc.fromHeight,arc.toHeight,flight)+4f*flight*(1f-flight)*arc.apex
                    // Auch der Flugweg hat Kontakt: ein hohes Hindernis darf nicht durchsprungen werden.
                    val blocked=(1..12).firstOrNull { sample ->
                        val f=sample/12f
                        val p=PlayControl.Pos(lerp(state.pos.x,pos.x,f),lerp(state.pos.depth,pos.depth,f))
                        val h=lerp(state.height,nextHeight,f)
                        surfaces.any { it.id != arc.surface && it.id != state.support &&
                            it.contains(p) && it.heightAt(p)>h+1f }
                    }
                    if(blocked!=null) {
                        val f=(blocked-1)/12f
                        val stop=PlayControl.Pos(lerp(state.pos.x,pos.x,f),lerp(state.pos.depth,pos.depth,f))
                        return Result(state.copy(pos=stop,elapsed=0L,support=null,queuedJumpMs=0L,
                            arc=Arc(stop,stop,state.height,0f,null,330L,0f)))
                    }
                }
                if (t == 1f) {
                    val landed = state.copy(pos = pos, height = arc.toHeight, support = arc.surface,
                        action = null, arc = null, elapsed = 0L, queuedJumpMs = 0L)
                    return Result(if (state.queuedJumpMs > 0L)
                        command(landed, Command.JUMP, input, surfaces, maxStepHeight) else landed)
                }
                return Result(state.copy(pos = pos, elapsed = elapsed,
                    height = lerp(arc.fromHeight, arc.toHeight, flight) + 4f * flight * (1f - flight) * arc.apex,
                    queuedJumpMs = (state.queuedJumpMs - dt).coerceAtLeast(0L)))
            }
            Action.ROLL -> {
                val step = PlayControl.step(state.pos, state.velocity, dt, horizontalScale = horizontalScale)
                val pos = collide(state.pos, step.pos.copy(pushMs = 0L), state.height, surfaces, state.support)
                val result = state.copy(pos = pos, height = surfaces.firstOrNull { it.id == state.support }?.heightAt(pos) ?: state.height, elapsed = elapsed,
                    action = if (elapsed >= 460L) null else Action.ROLL,
                    velocity = if (elapsed >= 460L) PlayControl.Stick() else state.velocity)
                return Result(fallIfNeeded(result, surfaces))
            }
            Action.SIT -> return Result(state.copy(elapsed = elapsed,
                action = if (elapsed >= 380L) Action.REST else Action.SIT))
            Action.REST -> return Result(if (input.strength > 0.1f)
                state.copy(action = Action.RISE, elapsed = 0L) else state)
            Action.RISE -> return Result(state.copy(elapsed = elapsed,
                action = if (elapsed >= 260L) null else Action.RISE))
            null -> Unit
        }
        // Begrenzte Beschleunigung ist bildratenunabhaengig; Loslassen bremst bewusst schneller.
        val change = dt / if (input.strength < state.velocity.strength) 75f else 135f
        fun ease(a: Float, b: Float) = a + (b - a).coerceIn(-change, change)
        val velocity = PlayControl.Stick(ease(state.velocity.x, input.x), ease(state.velocity.y, input.y))
        val step = PlayControl.step(state.pos, velocity, dt, exitDelayMs, immediateExits, horizontalScale)
        val pos = collide(state.pos, step.pos, state.height, surfaces, state.support)
        val actual = distance(state.pos, pos) > 0.00001f ||
            (step.exit != null && (exitDelayMs == 0L || step.exit in immediateExits) && pos == step.pos)
        val tempo = PlayControl.tempo(velocity.strength)
        val running = actual && if (state.running) tempo > 1.5f else tempo >= 1.85f
        val runBlend = (state.runBlend + (if (running) 1f else -1f) * dt / 160f).coerceIn(0f, 1f)
        val supportHeight = surfaces.firstOrNull { it.id == state.support }?.heightAt(pos) ?: state.height
        val next = state.copy(pos = pos, height = supportHeight, velocity = if (actual) velocity else PlayControl.Stick(),
            facing = velocity.direction(state.facing) ?: state.facing,
            gaitMs = if (actual) state.gaitMs + dt * PlayControl.tempo(velocity.strength) else state.gaitMs,
            elapsed = 0L, running = running, runBlend = runBlend)
        return Result(fallIfNeeded(next, surfaces), step.exit.takeIf { pos == step.pos })
    }

    private fun fallIfNeeded(state: State, surfaces: List<Surface>): State {
        if (state.support == null || surfaces.any { it.id == state.support && it.contains(state.pos) }) return state
        return state.copy(action = Action.JUMP, elapsed = 0L, support = null,
            velocity = PlayControl.Stick(), arc = Arc(state.pos, state.pos, state.height, 0f, null, 330L, apex = 0f))
    }
}
