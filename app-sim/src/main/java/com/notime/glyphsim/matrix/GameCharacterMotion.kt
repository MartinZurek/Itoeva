package com.notime.glyphsim.matrix

import kotlin.math.exp
import kotlin.math.sin

/** Statur, Stoff und Gang-/Schwimmbewegung; Handlungen und Weltposition bleiben beim bestehenden Motor. */
object GameCharacterMotion {
    const val COLUMNS = 24
    const val ROWS = 32
    const val VERTICES = (COLUMNS + 1) * (ROWS + 1)
    enum class View { SIDE, FRONT, BACK, COMPACT }
    data class Response(val drag: Float = 0f, val sway: Float = 0f,
        val breeze: Float = 0f, val breezeSway: Float = 0f)
    data class Point(val x: Float, val y: Float)

    fun view(frame: Int): View = when (frame) {
        in 23..26, 99, in 130..137 -> View.COMPACT
        in 27..31, 37, in 39..46, 55, in 56..61, in 68..75, in 100..106, in 122..125 -> View.FRONT
        in 32..36, 38, in 47..54, in 62..67, in 76..83, in 107..113, in 126..129 -> View.BACK
        else -> View.SIDE
    }

    /** Exakter gedaempfter Nachlauf statt eines Sprungs bei Start, Stopp oder Bildratenwechsel. */
    class Follow {
        private var previous: Long? = null
        private var drag = 0f
        private var sway = 0f
        private var breeze = 0f
        private var breezeSway = 0f
        fun update(clock: Long, speed: Float, wind: Float): Response {
            val target = speed.safe(0f, 2f).let { if (it == 0f) 0f else -it * .7f }
            val windTarget = wind.safe(-1.5f, 1.5f) * .65f
            val last = previous
            previous = clock
            if (last == null || clock < last || clock.toDouble() - last.toDouble() > 250.0) {
                // Ein Hintergrundaufenthalt ist kein Landestoss und keine aufgestaute Windboee.
                drag = target
                sway = target
                breeze = windTarget
                breezeSway = windTarget
            } else {
                val dt = (clock - last) / 1000.0
                val oldDrag = drag
                val oldBreeze = breeze
                val slow = exp(-4.0 * dt).toFloat()
                val fast = exp(-9.0 * dt).toFloat()
                drag = target + (oldDrag - target) * slow
                // Analytische Kaskade zweier Daempfer, auch bei 30/60/120 Hz gleich.
                sway = target + (sway - target) * fast +
                    9f / 5f * (oldDrag - target) * (slow - fast)
                breeze = windTarget + (oldBreeze - windTarget) * slow
                breezeSway = windTarget + (breezeSway - windTarget) * fast +
                    9f / 5f * (oldBreeze - windTarget) * (slow - fast)
            }
            return Response(drag, sway, breeze, breezeSway)
        }
    }

    private fun Float.safe(low: Float, high: Float) = if (isFinite()) coerceIn(low, high) else 0f
    private fun smooth(t: Float): Float = t.coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
    private fun window(value: Float, from: Float, to: Float, fade: Float): Float =
        smooth((value - from) / fade) * smooth((to - value) / fade)

    /** Stuetzen und freie Rueckholphase teilen sich den Takt der gezeichneten Beine. */
    data class Stride(val flight: Float, val compression: Float, val leftSwing: Float, val rightSwing: Float)
    fun stride(gaitMs: Long, running: Boolean): Stride {
        val phase = Math.floorMod(gaitMs, 760L) / 760f
        fun swing(p: Float): Float = if (p <= .5f) 0f else
            sin((p - .5f) * Math.PI * 2).toFloat().coerceAtLeast(0f)
        val step = (phase * 2f) % 1f
        val flight = if (running && step > .56f)
            sin((step - .56f) / .44f * Math.PI).toFloat().coerceAtLeast(0f) else 0f
        val compression = if (running && step < .30f)
            sin(step / .30f * Math.PI).toFloat().coerceAtLeast(0f) else 0f
        return Stride(flight, compression, swing(phase), swing((phase + .5f) % 1f))
    }

    private fun locomotion(p: Point, species: AvatarSpecies, frame: Int, top: Float,
        u: Float, v: Float, gaitMs: Long, speed: Float): Point {
        val body = ((v - top) / (125f/128f - top).coerceAtLeast(.05f)).coerceIn(0f, 1f)
        val side = view(frame) == View.SIDE
        val running = frame in 114..129
        val walking = frame in 9..16 || frame in 68..83
        if ((!walking && !running) || speed <= 0f) return p
        val stride = stride(gaitMs, running)
        val active = speed.safe(0f,1f)
        val leg = smooth((body-.82f)/.18f)
        val left = window(u,.28f,.53f,.09f)
        val right = window(u,.53f,.82f,.09f)
        val swing = stride.leftSwing * left + stride.rightSwing * right
        val recoil = if (running) .038f else if (species == AvatarSpecies.STARLET) .022f else .008f
        val height = when (species) {
            AvatarSpecies.GLOOP -> .013f
            AvatarSpecies.PUFFLING, AvatarSpecies.HOOTLET -> .024f
            else -> .029f
        }
        val lean = if (running && side) (if (species == AvatarSpecies.FENNEC) .012f else .032f) else 0f
        val torsoWeight = smooth((1f-body)/.65f)
        val x = p.x + lean * torsoWeight * active +
            (stride.leftSwing * left - stride.rightSwing * right) * leg * recoil * .45f * active
        // Beim Gehen bleibt die Stuetzspitze fest. Rennen hat Stauchung, Abdruck und echte Flugphase.
        val y = p.y - swing * leg * recoil * active - stride.flight * height * active +
            (125f/128f-v) * stride.compression * .055f * active
        return Point(x,y)
    }

    /** Koerper-/Stoffkoordinaten gehoeren zur ausgewaehlten Zeichnung, nie zur Weltposition. */
    fun point(species: AvatarSpecies, frame: Int, top: Float, u: Float, v: Float,
        clock: Long, gaitMs: Long, speed: Float, wind: Float, response: Response,
        swim: GameWater.Swim? = null): Point {
        // Schwimmglieder sind im eigenen Bogen gezeichnet. Eine Rumpfverformung
        // machte daraus bislang ein Aufblasen und Entleeren statt einen Zug.
        if (swim != null) return Point(u, v)
        val view = view(frame)
        if (view == View.COMPACT) return Point(u, v)
        if (v >= 120f / 128f) return locomotion(Point(u,v),species,frame,top,u,v,gaitMs,speed)
        val bodyV = ((v - top) / (125f / 128f - top).coerceAtLeast(.05f)).coerceIn(0f, 1f)
        val t = Math.floorMod(clock, 600_000L) / 1000.0
        val gait = Math.floorMod(gaitMs, 760L) / 760.0 * Math.PI * 2.0
        val moving = speed.safe(0f, 2f)
        val directed = view != View.SIDE
        // Nur die Taille wird schmaler. Gesicht, Kapuze, Sternspitzen und Boden bleiben frei.
        val upright = frame in 0..16 || frame in 27..83 || frame == 95 || frame == 104 || frame == 111 || frame in 114..129
        val waist = if (upright) window(bodyV, .62f, .95f, .12f) else 0f
        val taper = when (species) {
            AvatarSpecies.PUFFLING -> .13f
            AvatarSpecies.HOOTLET -> .055f
            AvatarSpecies.WYRMLING -> .035f
            AvatarSpecies.FENNEC -> .035f
            AvatarSpecies.GLOOP, AvatarSpecies.STARLET -> 0f
        }
        val axis = if (directed) .5f else when (species) {
            AvatarSpecies.FENNEC -> .61f
            AvatarSpecies.WYRMLING -> .56f
            else -> .55f
        }
        val torso = window(bodyV, .56f, .97f, .16f) * window(u, .25f, .79f, .10f)
        val breath = sin(t * when (species) {
            AvatarSpecies.GLOOP -> 1.65
            AvatarSpecies.PUFFLING -> 2.25
            AvatarSpecies.HOOTLET -> 1.35
            else -> 1.85
        } + species.ordinal * .7).toFloat()
        val step = sin(gait * 2.0 - .65).toFloat() * moving
        val soft = when (species) {
            AvatarSpecies.GLOOP -> .009f
            AvatarSpecies.PUFFLING -> .0035f
            AvatarSpecies.STARLET -> .0025f
            else -> .0015f
        }
        // Gloop erhaelt eine Volumenwelle; Knochenfiguren nur eine kleine Brust-/Gewichtsreaktion.
        val volume = (breath * .4f + step * .6f) * soft * torso
        var x = u - (u - axis) * taper * waist + (u - axis) * volume * 4f
        var y = v - volume
        if (upright) x += response.sway * .003f * torso * if (directed) .3f else 1f

        // Ganze freie Saeume, mit weichen Gewichten an Kragen, Rumpf und Pfoten.
        // Kein kleinstes Quellpixel-Runden: 0.3 Pixel Bewegung muss sichtbar bleiben.
        val clothTop = if (species == AvatarSpecies.FENNEC) .37f else .44f
        val cloth = window(bodyV, clothTop, .92f, .12f)
        val lateral = if (directed) {
            window(u, .23f, .77f, .09f) * (u - .5f) * 3f
        } else {
            val left = when (species) {
                AvatarSpecies.FENNEC -> .21f
                AvatarSpecies.WYRMLING -> .18f
                else -> .22f
            }
            window(u, left, .58f, .09f) * smooth((.60f - u) / .30f)
        }
        val material = when (species) {
            AvatarSpecies.FENNEC -> 1f
            AvatarSpecies.HOOTLET -> .7f
            AvatarSpecies.PUFFLING -> .65f
            AvatarSpecies.WYRMLING -> .5f
            AvatarSpecies.STARLET -> .45f
            AvatarSpecies.GLOOP -> .25f
        }
        val flutter = sin(t * 5.1 - bodyV * 3.8 + species.ordinal).toFloat() *
            (.12f + kotlin.math.abs(wind.safe(-1.5f, 1.5f)) * .16f + moving * .08f)
        val drag = response.drag + (response.drag - response.sway) * .7f + flutter
        val breeze = response.breeze + (response.breeze - response.breezeSway) * .7f
        // In Front-/Rueckansicht blaest Wind beide Saeume zur selben physischen Seite.
        // Gangzug darf sie dagegen symmetrisch zur Koerpermitte ziehen.
        val fabric = (drag * lateral + breeze * if (directed) kotlin.math.abs(lateral) else lateral)
            .coerceIn(-1.8f, 1.8f) * cloth * material * .018f
        x += fabric
        y += sin(t * 3.3 - bodyV * 4.2).toFloat() * cloth * lateral * material * moving * .002f
        return locomotion(Point(x,y),species,frame,top,u,v,gaitMs,speed)
    }

    fun fill(vertices: FloatArray, species: AvatarSpecies, frame: Int, top: Float,
        clock: Long, gaitMs: Long, speed: Float, wind: Float, response: Response,
        swim: GameWater.Swim? = null) {
        require(vertices.size == VERTICES * 2)
        var i = 0
        for (row in 0..ROWS) for (column in 0..COLUMNS) {
            val p = point(species, frame, top, column.toFloat() / COLUMNS, row.toFloat() / ROWS,
                clock, gaitMs, speed, wind, response,swim)
            vertices[i++] = p.x
            vertices[i++] = p.y
        }
    }
}
