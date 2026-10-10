package com.notime.glyphsim.matrix

import kotlin.math.sin

/** Ufer in Bildkoordinaten; Stege und Felsen bleiben starre Gegenstaende. */
object GameWater {
    /** Weltfeste Pixelwellen: der Kamm wandert, seine Lichtstaerke folgt dem Hang. */
    data class Crest(val x: Float, val y: Float, val width: Float, val light: Float)
    fun crest(column: Int, row: Int, clock: Long): Crest {
        val depth = ((row * 12f - 390f) / 220f).coerceIn(0f, 1f)
        val phase = clock / 1000.0
        val seed = Math.floorMod(column * 73 + row * 37, 101) / 101f
        val travel = phase * (1.8f + depth * 2.5f)
        val x = (column * 32f + seed * 18f + sin(row * 1.7f + phase * .7f) * 3f).toFloat()
        val y = (row * 12f + (travel + seed * 12f) % 12f).toFloat()
        val pulse = (.5f + .5f * sin(phase * 1.1f + column * 2.1f + row * .8f)).toFloat()
        return Crest(kotlin.math.floor(x / 2f) * 2f, kotlin.math.floor(y / 2f) * 2f,
            4f + kotlin.math.floor((4f + depth * 10f) * pulse / 2f) * 2f,
            (.08f + pulse * .22f) * (.4f + depth * .6f))
    }
    /** Vier gezeichnete Zugphasen je Ansicht; Koerpervolumen ist keine Animation. */
    fun swimFrame(stroke: Float, dir: PlayControl.Dir?): Int {
        val phase = if (stroke.isFinite()) Math.floorMod((stroke * 4f).toInt(), 4) else 0
        val row = when (dir) { PlayControl.Dir.UP -> 2; PlayControl.Dir.DOWN -> 1; else -> 0 }
        return row * 4 + phase
    }
    data class Run(val x0: Int, val x1: Int, val y: Int)
    private val masks = java.util.concurrent.ConcurrentHashMap<String, List<Run>>()
    private val shore = listOf(0f to 525f, 180f to 493f, 350f to 510f, 550f to 490f,
        850f to 500f, 1040f to 501f, 1190f to 476f, 1360f to 499f,
        1560f to 517f, 1760f to 498f, 1920f to 536f)
    fun shoreY(x: Float): Float {
        val i = shore.indexOfFirst { it.first >= x }.coerceAtLeast(0)
        if (i == 0) return shore.first().second
        val a = shore[i-1]; val b = shore[i]
        return a.second + (b.second-a.second) * ((x-a.first)/(b.first-a.first)).coerceIn(0f,1f)
    }
    fun rigid(x: Float, y: Float): Boolean =
        (x in 140f..385f && y in 441f..498f) ||
        (x in 1100f..1345f && y in 412f..454f) ||
        (x in 0f..95f && y in 445f..535f) ||
        (x in 415f..565f && y >= 570f) ||
        (x in 1570f..1710f && y >= 586f) ||
        (x >= 1840f && y >= 515f)
    fun contains(scene: GameScenes.Scene, x: Float, y: Float): Boolean {
        if (scene.asset != "world/coast.png") return false
        val wx = GameWorld.origin(scene.place) + x
        return y > shoreY(wx) && !rigid(wx,y)
    }
    /** Fuer die Darstellung werden nur blaue Wasserpixel in vermessenen Wasserbereichen bewegt.
     * Einmal beim Laden, nicht im Bildtakt. Einzelne helle Reflexloecher werden geschlossen. */
    fun measure(asset: String, pixel: (Float,Float) -> Int): List<Run> {
        if (asset !in setOf("world/coast.png", "world/coast-path.png")) return emptyList()
        val rows = Array(160) { BooleanArray(480) }
        for (iy in rows.indices) for (ix in rows[iy].indices) {
            val x=(ix+.5f)*4f; val y=(iy+.5f)*4f
            val area = if (asset == "world/coast.png")
                (y > shoreY(x)+3f && !rigid(x,y)) || (x in 450f..1410f && y in 310f..394f)
                else y > 475f && x in 0f..1200f
            if (!area) continue
            val rgb=pixel((ix+.5f)/480f,(iy+.5f)/160f)
            val r=rgb shr 16 and 255; val g=rgb shr 8 and 255; val b=rgb and 255
            rows[iy][ix] = g > r*1.06f && b > r*1.08f && b > 45
        }
        val result=mutableListOf<Run>()
        rows.forEachIndexed { iy,row ->
            var last = -10
            for (ix in row.indices) if (row[ix]) {
                if (ix-last in 2..3) for (hole in last+1 until ix) row[hole]=true
                last=ix
            }
            var start=-1
            for (ix in 0..row.size) {
                if (ix < row.size && row[ix]) { if(start<0) start=ix }
                else if(start>=0) { if(ix-start>=2) result+=Run(start*4,ix*4,iy*4); start=-1 }
            }
        }
        masks[asset]=result
        return result
    }
    fun runs(asset: String) = masks[asset].orEmpty()
    /** Vorderes Wasser und Flussstroemung werden binnen weniger Bilder sichtbar, fern bleibt es ruhig. */
    fun displacement(x: Float,y: Float,clock: Long): Float {
        val foreground = ((y - 390f) / 160f).coerceIn(0f, 1f)
        return sin(x*.027f+y*.091f-clock/280f)*(1.8f+foreground*1.8f) +
            sin(x*.011f-y*.046f+clock/540f)*(0.8f+foreground*.6f)
    }

    data class Swim(val stroke: Float, val bob: Float, val angle: Float, val moving: Boolean,
        val buoyancy: Float = if(moving) 28f else 22f) {
        val cue get() = CreatureSprites.MotionCue(CreatureSprites.Motion.SWIM, stroke)
    }
    /** Kein Laufzyklus unter Wasser: Ausstrecken, Zug, Zurueckholen und ruhiges Wassertreten. */
    fun swim(scene: GameScenes.Scene, pos: PlayControl.Pos, lift: Float, dir: PlayControl.Dir,
        moving: Boolean, clock: Long, species: AvatarSpecies = AvatarSpecies.FENNEC): Swim? {
        val wet = GameWorld.wetness(scene,pos)
        if (wet < .20f || lift > 0f) return null
        val phase = (clock % if(moving) 900L else 1500L).toFloat() / if(moving) 900f else 1500f
        val tilt = if(moving) 9f else 3f
        val angle = when(dir) { PlayControl.Dir.LEFT -> -tilt; PlayControl.Dir.RIGHT -> tilt; else -> sin(phase*6.283f)*4f }
        val rise = when(species) { AvatarSpecies.WYRMLING -> 14f; AvatarSpecies.STARLET -> 22f; else -> 28f }
        return Swim(phase, sin(phase*6.283f)*1.6f, angle, moving, if(moving) rise else rise*.79f)
    }
    fun motion(swim: Swim?, explicit: CreatureSprites.MotionCue?) = explicit ?: swim?.cue
    fun drag(scene: GameScenes.Scene, pos: PlayControl.Pos): Float = 1f - GameWorld.wetness(scene,pos).coerceAtMost(.6f)*.7f
}
