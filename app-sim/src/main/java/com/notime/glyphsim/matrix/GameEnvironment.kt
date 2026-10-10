package com.notime.glyphsim.matrix

import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

/** Kosmetische Raumphysik im bestehenden Spieltakt; keine gespeicherten Spielwerte. */
object GameEnvironment {
    enum class Material { STONE, WOOD, GRASS, SAND, MUD, WATER, SNOW }
    enum class Kind { LEAF, CLOUD, WATER, CURTAIN, FIRE, GRASS }
    data class Part(val kind: Kind, val sx: Int, val sy: Int, val w: Int, val h: Int,
        val x: Int, val y: Int, val seed: Int, val foreground: Boolean = false)
    data class Room(val parts: List<Part>, val material: Material)
    data class Pose(val dx: Float = 0f, val dy: Float = 0f, val angle: Float = 0f)
    class Grid(val bytes: ByteArray, val fallback: Material) {
        init { require(bytes.size == GameScenes.IMAGE_W * GameScenes.IMAGE_H) }
        fun at(x: Float, y: Float): Material = Material.entries.getOrElse(
            bytes[y.toInt().coerceIn(0, GameScenes.IMAGE_H - 1) * GameScenes.IMAGE_W +
                x.toInt().coerceIn(0, GameScenes.IMAGE_W - 1)].toInt()) { fallback }
    }
    data class Contact(val place: PlayScene.Place, val x: Float, val y: Float,
        val material: Material, val born: Long, val life: Long, val foot: Int,
        val dx: Float, val dy: Float, val impact: Float = 0f)
    data class State(val clock: Long = 0L, val place: PlayScene.Place? = null,
        val remainder: Float = 0f, val foot: Int = 0, val contacts: List<Contact> = emptyList())

    const val MAX_CONTACTS = 128
    fun lifetime(material: Material): Long = when (material) {
        Material.MUD -> 45_000L
        Material.SAND, Material.SNOW -> 60_000L
        Material.WATER -> 2_400L
        Material.GRASS -> 1_200L
        Material.WOOD, Material.STONE -> 600L
    }
    /** Der Bildtakt darf auch bei einem offenen Menue weiterlaufen, aber erzeugt keine Schritte. */
    fun advance(state: State, dt: Long): State {
        val clock = state.clock + dt.coerceIn(0L, 50L)
        return state.copy(clock = clock, contacts = state.contacts.filter { clock - it.born < it.life })
    }
    fun resetSampling(state: State) = state.copy(place = null, remainder = 0f)

    /** Kontakte folgen dem tatsaechlichen Weg, nicht dem Joystick oder der Animationsphase. */
    fun tick(state: State, scene: GameScenes.Scene, before: GameMovement.State,
        after: GameMovement.State, dt: Long, grid: Grid?): State {
        val next = advance(state, dt)
        if (state.place != scene.place) return next.copy(place = scene.place, remainder = 0f)
        val (ax, ay) = GameScenes.feet(scene, before.pos)
        val (bx, by) = GameScenes.feet(scene, after.pos)
        val dx = bx - ax
        val dy = by - ay
        val distance = sqrt(dx * dx + dy * dy)
        fun material(x: Float, y: Float, support: String?) = if (support != null) Material.WOOD
            else GameWorld.material(scene, x, y) ?: grid?.at(x, y) ?: GameRoomCatalog.rooms[scene.place]?.material ?: Material.STONE
        val contacts = next.contacts.toMutableList()
        var foot = next.foot
        fun contact(x: Float, y: Float, support: String?, height: Float, impact: Float) {
            val m = material(x, y, support)
            contacts += Contact(scene.place, x, y - height, m, next.clock, lifetime(m), foot++ % 2,
                if (distance > 0f) dx / distance else 0f,
                if (distance > 0f) dy / distance else 1f, impact)
        }
        // Auch eine gepufferte Sprungkette hat genau einen Kontakt zwischen beiden Boegen.
        val landed = before.action == GameMovement.Action.JUMP && before.arc != null &&
            (after.action != GameMovement.Action.JUMP || (after.elapsed < before.elapsed &&
                after.pos == before.arc.to && after.height <= before.arc.toHeight + .1f))
        if (landed) {
            val arc = before.arc!!
            val (x, y) = GameScenes.feet(scene, arc.to)
            contact(x, y, arc.surface, arc.toHeight,
                (arc.apex + (arc.fromHeight - arc.toHeight).coerceAtLeast(0f)).coerceIn(10f, 80f) / 30f)
            return next.copy(contacts = contacts.takeLast(MAX_CONTACTS), remainder = 0f, foot = foot)
        }
        if (before.action == GameMovement.Action.JUMP || after.action == GameMovement.Action.JUMP ||
            after.action == GameMovement.Action.SIT || after.action == GameMovement.Action.REST ||
            distance > 35f || before.support != after.support) {
            return next.copy(remainder = 0f)
        }
        if (distance < 0.001f) return next
        val gaitDelta=after.gaitMs-before.gaitMs
        if(after.moving && gaitDelta>0.0 && gaitDelta<=200.0) {
            val first=kotlin.math.floor(before.gaitMs/380.0).toLong()+1L
            val last=kotlin.math.floor(after.gaitMs/380.0).toLong()
            for(step in first..last) {
                val t=((step*380.0-before.gaitMs)/gaitDelta).toFloat().coerceIn(0f,1f)
                val side=if(foot%2==0) -1f else 1f
                val width=GameScenes.avatarHeight(scene,after.pos)*.035f
                contact(ax+dx*t-dy/distance*width*side,ay+dy*t+dx/distance*width*side,
                    after.support,after.height,0f)
            }
            return next.copy(remainder=0f,contacts=contacts.takeLast(MAX_CONTACTS),foot=foot)
        }
        val stride = (GameScenes.avatarHeight(scene, after.pos) * 0.18f).coerceIn(7f, 14f)
        var at = stride - next.remainder
        while (at <= distance) {
            val t = at / distance
            val x = ax + dx * t
            val y = ay + dy * t
            // Fussbreite in Perspektive; beide Seiten treffen jeweils ihr eigenes Material.
            val side = if (foot % 2 == 0) -1f else 1f
            val width = GameScenes.avatarHeight(scene, after.pos) * 0.035f
            contact(x - dy / distance * width * side, y + dx / distance * width * side,
                after.support, after.height, 0f)
            at += stride
        }
        return next.copy(remainder = (next.remainder + distance) % stride,
            contacts = contacts.takeLast(MAX_CONTACTS), foot = foot)
    }

    /** Bewohner hinterlassen Kontakt nur nach einem echten Wegschritt, nie beim Einblenden.
     * Gemeinsame Uhr und begrenzte Liste; dieser Leser startet keinen zweiten Simulationslauf. */
    fun residentContacts(state: State,scene: GameScenes.Scene,before: Map<String,GameResidents.Actor>,
        after: Map<String,GameResidents.Actor>,clock: Long,dt: Long): State {
        if(clock/350L == (clock-dt.coerceIn(0L,50L)).coerceAtLeast(0L)/350L) return state
        val traces=after.mapNotNull { (id,actor) ->
            val previous=before[id] ?: return@mapNotNull null
            if(!actor.moving || actor.seat!=null || previous.pos == actor.pos) return@mapNotNull null
            val a=GameScenes.feet(scene,previous.pos); val b=GameScenes.feet(scene,actor.pos)
            val dx=b.first-a.first; val dy=b.second-a.second
            val distance=sqrt(dx*dx+dy*dy)
            if(distance<=.001f || distance>35f) return@mapNotNull null
            val material=GameWorld.material(scene,b.first,b.second) ?: return@mapNotNull null
            Contact(scene.place,b.first,b.second,material,state.clock,lifetime(material),
                ((clock/350L+actor.snapshot.species.ordinal)%2L).toInt(),dx/distance,dy/distance)
        }
        return state.copy(contacts=(state.contacts+traces).takeLast(MAX_CONTACTS))
    }

    /** Gemeinsame Zeitbasis, aber jede Baumkrone und jede Welle hat eine eigene Phase. */
    fun pose(part: Part, clock: Long): Pose {
        val t = clock / 1000f
        val p = part.seed * 1.618f
        val wind = sin(t * 0.83f + p) + 0.35f * sin(t * 2.1f - p)
        return when (part.kind) {
            Kind.CLOUD -> Pose(dx = sin(t * 0.065f + p) * 16f)
            Kind.WATER -> Pose(dx = sin(t * 2.4f + part.y * 0.2f) * 3f)
            Kind.FIRE -> Pose(dy = -abs(sin(t * 9f + p)) * 2f, angle = wind * 1.5f)
            Kind.CURTAIN -> Pose(dx = wind * 0.65f, angle = wind * 0.7f)
            Kind.LEAF -> Pose(dx = wind * 0.8f, angle = wind * 1.2f)
            Kind.GRASS -> Pose(angle = wind * 2.2f)
        }
    }
}
