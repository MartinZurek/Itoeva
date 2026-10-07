package com.notime.glyphsim.matrix

/**
 * **Die Wesen in feiner Pixel-Art** - Neuentwurf nach den Charakterstudien vom 04.10.
 * (`docs/concept-art/character-art-studies/`, gezeichnet mit `tools/character-art`).
 *
 * ## Wie die neuen Bilder zu den alten Ablaeufen finden
 *
 * Alles, was eine Kreatur tut, steuern weiter die bestehenden Ablaeufe in [AvatarAnimations]:
 * Sie liefern Bild fuer Bild eine grobe 16x19-Pose. Diese Posen bleiben die eine Quelle fuer das,
 * WAS geschieht - Entscheidungen, Reaktionen, Wege, Vergleichsbilder der Reaktionspruefung
 * bleiben unberuehrt. Gezeichnet wird aber nicht mehr die grobe Pose, sondern das passende Bild
 * aus dem Bogen der Kreatur. Welches, wird an der groben Pose abgelesen:
 *
 * - **Schattenseite gesetzt** (siehe [AvatarShading]): Sie geht - Laufbilder im eigenen Takt.
 * - **Augen zu und deutlich flacher als im Stand**: Sie schlaeft.
 * - **Hebt sie zwei Zellen oder mehr vom Boden ab**, springt sie: Freude.
 * - In jedem Fall wird das neue Bild so hoch angehoben, wie die grobe Pose abhebt - Huepfer und
 *   Spruenge bleiben sichtbar.
 * - **Augen zu, sonst nichts**: Blinzeln oder ein geschlossener Moment.
 * - Sonst Ruhe, mit Atmen und gelegentlichem Blinzeln.
 *
 * Reine Rechnung ohne Android; geladen und gezeichnet wird in [AvatarSpriteView].
 */
object CreatureSprites {

    /** Kantenlaenge eines Bildes im Bogen. */
    const val FRAME = 64

    /** Erste Zeile unter den Fuessen - dort steht die Figur auf. */
    const val FEET = 62

    /** Breite des Bildes relativ zur Breite des alten Sprite-Kastens. */
    const val SCALE = 0.9f

    const val IDLE = 0
    const val IDLE_BREATH = 1
    const val BLINK = 2
    const val WALK_FIRST = 3
    const val JOY_FIRST = 7
    const val SLEEP_FIRST = 9
    const val FRONT = 11
    const val BACK = 12
    const val FRONT_WALK_FIRST = 13
    const val BACK_WALK_FIRST = 15
    const val FRAME_COUNT = 17

    private const val BREATH_MS = 900L
    private const val WALK_MS = 140L
    private const val JOY_MS = 260L
    private const val SLEEP_MS = 1400L
    private const val BLINK_EVERY_MS = 4200L
    private const val BLINK_MS = 160L

    /** Ab so vielen Zellen ueber dem Boden gilt eine Pose als Sprung (Freude). */
    private const val JUMP = 2

    /** Ab diesem Anteil der Standhoehe gilt eine Pose mit geschlossenen Augen als Liegen. */
    private const val LYING = 0.8f

    /** Pfad des Bogens unter `assets/`. */
    fun assetFor(species: AvatarSpecies): String = "creatures/${species.name.lowercase()}.png"

    /** Welches Bild, wie hoch angehoben (in Zellen des alten Rasters) und ob gespiegelt. */
    data class Look(val frame: Int, val liftCells: Int, val mirrored: Boolean,
        val blendFrame: Int? = null, val blend: Float = 1f)

    private val standHeight = HashMap<AvatarSpecies, Int>()

    private fun bounds(frame: IntArray, width: Int, height: Int): IntArray? {
        var top = height
        var bottom = -1
        for (y in 0 until height) for (x in 0 until width) {
            if (frame.getOrElse(y * width + x) { 0 } > 0) {
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
        return if (bottom < 0) null else intArrayOf(top, bottom)
    }

    private fun standHeightOf(species: AvatarSpecies): Int = standHeight.getOrPut(species) {
        bounds(AvatarAnimations.idlePose(species), AvatarGeometry.SIZE, AvatarGeometry.HEIGHT)
            ?.let { it[1] - it[0] + 1 } ?: AvatarGeometry.HEIGHT
    }

    /**
     * Das Bild zur groben Pose [raw] (noch nicht gespiegelt), zur Laufrichtung [side] und zur
     * Uhrzeit [timeMs] (fuer Atmen, Schritte und Blinzeln im eigenen Takt).
     */
    fun look(
        raw: IntArray,
        species: AvatarSpecies,
        side: AvatarShading.Side,
        timeMs: Long,
        direction: PlayControl.Dir? = null,
        moving: Boolean? = null
    ): Look {
        val (act, lift, mirrored) = classify(raw, species, side, timeMs, direction, moving)
        val frame = when (act) {
            Activity.SLEEP -> SLEEP_FIRST + ((timeMs / SLEEP_MS) % 2).toInt()
            Activity.BACK -> BACK
            Activity.BACK_WALK -> BACK_WALK_FIRST + ((timeMs / WALK_MS) % 2).toInt()
            Activity.FRONT -> FRONT
            Activity.FRONT_WALK -> FRONT_WALK_FIRST + ((timeMs / WALK_MS) % 2).toInt()
            Activity.WALK -> WALK_FIRST + ((timeMs / WALK_MS) % 4).toInt()
            Activity.JOY -> JOY_FIRST + ((timeMs / JOY_MS) % 2).toInt()
            Activity.BLINK -> when (direction) {
                PlayControl.Dir.UP -> BACK
                PlayControl.Dir.DOWN -> FRONT
                else -> BLINK
            }
            Activity.IDLE -> if ((timeMs / BREATH_MS) % 2 == 0L) IDLE else IDLE_BREATH
        }
        return Look(frame, lift, mirrored)
    }

    /** Was die Figur gerade tut - gemeinsam fuer den einfachen und den feinen Bogen. */
    enum class Activity { SLEEP, BACK, BACK_WALK, FRONT, FRONT_WALK, WALK, JOY, BLINK, IDLE }

    internal data class Classified(val activity: Activity, val lift: Int, val mirrored: Boolean)

    internal fun classify(
        raw: IntArray,
        species: AvatarSpecies,
        side: AvatarShading.Side,
        timeMs: Long,
        direction: PlayControl.Dir?,
        moving: Boolean?
    ): Classified {
        // Aktive Laufrichtung ist eindeutig; die Schattenseite bezeichnet dagegen,
        // woher die Figur kommt, und kann im Stand NONE sein.
        val mirrored = when (direction) {
            PlayControl.Dir.LEFT -> true
            PlayControl.Dir.RIGHT, PlayControl.Dir.UP, PlayControl.Dir.DOWN -> false
            null -> AvatarFacing.mirrors(side)
        }
        val box = bounds(raw, AvatarGeometry.SIZE, AvatarGeometry.HEIGHT)
            ?: return Classified(Activity.IDLE, 0, mirrored)
        val ground = AvatarBodies.forSpecies(species).groundRow()
        val lift = (ground - box[1]).coerceAtLeast(0)
        val eyesOpen = AvatarAccent.eyesIn(raw).any { it }
        val lying = box[1] - box[0] + 1 < standHeightOf(species) * LYING
        val walking = moving ?: (side != AvatarShading.Side.NONE)
        val activity = when {
            !eyesOpen && lying -> Activity.SLEEP
            walking && direction == PlayControl.Dir.UP -> Activity.BACK_WALK
            walking && direction == PlayControl.Dir.DOWN -> Activity.FRONT_WALK
            walking -> Activity.WALK
            // Reaktionen haben Vorrang vor der gemerkten Blickrichtung. Sonst verschluckt
            // ein stehender, nach vorne blickender Fennec jeden Sprung und jedes Blinzeln.
            lift >= JUMP -> Activity.JOY
            !eyesOpen || timeMs % BLINK_EVERY_MS < BLINK_MS -> Activity.BLINK
            direction == PlayControl.Dir.UP -> Activity.BACK
            direction == PlayControl.Dir.DOWN -> Activity.FRONT
            else -> Activity.IDLE
        }
        return Classified(activity, lift, mirrored)
    }

    /**
     * **Der feine Bogen** (128 x 128, 138 Rollen je Wesen): Fennec aus `rich_sheets.py`,
     * die anderen fuenf Wesen aus `ensemble_motion.py`. Eigene gezeichnete Gangphasen,
     * Richtungsansichten, schnelle Fortbewegung und Koerperposen ersetzen Stand-Sticker.
     * Kopf, Spross, Flaum, Federn und Kleidung bekommen vorberechnete kleine Regungen
     * ueber das bestehende Puppet-Werkzeug. Kein neuer Laufzeit-Rig oder Verhaltenskern.
     * Welcher Bogen vorliegt, entscheidet seine Hoehe.
     */
    object Rich {
        const val FRAME = 128
        const val FEET = 126
        /** Die Figur fuellt ihr Bild fast ganz aus - kleiner zeichnen, damit sie so gross wirkt wie die anderen. */
        const val SCALE = 0.82f
        /**
         * Am vorherigen Standbild gemessene Weltgroesse. Wyrmlings neuer Bogen
         * hat 92 statt 61 Pixel Standhoehe; 1.13 erhaelt die bisherige Weltgroesse.
         */
        fun scaleFor(species: AvatarSpecies): Float = when (species) {
            AvatarSpecies.FENNEC -> SCALE
            AvatarSpecies.GLOOP -> 1.16f
            AvatarSpecies.PUFFLING -> 1.28f
            AvatarSpecies.WYRMLING -> 1.13f
            AvatarSpecies.STARLET -> 0.98f
            AvatarSpecies.HOOTLET -> 1.18f
        }
        const val IDLE_FIRST = 0
        const val IDLE_COUNT = 8
        const val BLINK = 8
        const val WALK_FIRST = 9
        const val WALK_COUNT = 8
        const val JOY_FIRST = 17
        const val JOY_COUNT = 6
        const val SLEEP_FIRST = 23
        const val SLEEP_COUNT = 4
        const val FRONT = 27
        const val FRONT_WALK_FIRST = 28
        const val BACK = 32
        const val BACK_WALK_FIRST = 33
        const val DIR_WALK_COUNT = 4
        const val TURN_FRONT = 37
        const val TURN_BACK = 38
        const val FRONT_IDLE_FIRST = 39
        const val BACK_IDLE_FIRST = 47
        const val FRONT_BLINK = 55
        const val FRONT_JOY_FIRST = 56
        const val BACK_JOY_FIRST = 62
        const val DRAWN_FRONT_WALK_FIRST = 68
        const val DRAWN_BACK_WALK_FIRST = 76
        const val DRAWN_DIR_WALK_COUNT = 8
        const val ACTION_FIRST = 84
        const val FRONT_ACTION_FIRST = 100
        const val BACK_ACTION_FIRST = 107
        const val RUN_FIRST = 114
        const val RUN_COUNT = 8
        const val FRONT_RUN_FIRST = 122
        const val BACK_RUN_FIRST = 126
        const val DIR_RUN_COUNT = 4
        const val ROLL_FIRST = 130
        const val ROLL_COUNT = 8
        const val FRAME_COUNT = 138

        const val IDLE_MS = 420L
        const val WALK_MS = 95L
        const val JOY_MS = 110L
        const val SLEEP_MS = 700L
        const val DIR_WALK_MS = 120L
        const val TURN_STEP_MS = 85L

        fun idleFrame(timeMs: Long): Int = IDLE_FIRST + ((timeMs / IDLE_MS) % IDLE_COUNT).toInt()
    }

    /** Darstellung einer vorhandenen Handlung, ohne Reminder oder Spielablauf zu veraendern. */
    enum class Motion { JUMP, BEND, KNEEL, SIT, RISE, STRETCH, REACH, KICK, ROLL }

    /** Fortschritt kommt aus dem laufenden Ablauf, nicht aus der zufaelligen Wanduhrphase. */
    data class MotionCue(val motion: Motion, val progress: Float)

    fun liftOf(raw: IntArray, species: AvatarSpecies): Int = bounds(raw, AvatarGeometry.SIZE, AvatarGeometry.HEIGHT)
        ?.let { (AvatarBodies.forSpecies(species).groundRow() - it[1]).coerceAtLeast(0) } ?: 0

    internal fun motionFrame(cue: MotionCue, facing: Facing): Int {
        if (cue.motion == Motion.ROLL) {
            val progress = if (cue.progress.isFinite()) cue.progress.coerceIn(0f, 1f) else 0f
            return Rich.ROLL_FIRST + (progress * Rich.ROLL_COUNT).toInt().coerceAtMost(Rich.ROLL_COUNT - 1)
        }
        val side = when (cue.motion) {
            Motion.JUMP -> intArrayOf(0, 1, 2, 2, 3, 11)
            Motion.BEND -> intArrayOf(11, 4, 5, 4, 11)
            Motion.KNEEL -> intArrayOf(11, 4, 6, 6, 7, 11)
            Motion.SIT -> intArrayOf(11, 8, 9)
            Motion.RISE -> intArrayOf(9, 10, 11)
            Motion.STRETCH -> intArrayOf(11, 12, 12, 11)
            Motion.REACH -> intArrayOf(11, 13, 13, 11)
            Motion.KICK -> intArrayOf(11, 0, 14, 14, 11)
            Motion.ROLL -> error("Rolle hat eigenen Zyklus")
        }
        val directed = when (cue.motion) {
            Motion.JUMP -> intArrayOf(0, 1, 2, 2, 0, 4)
            Motion.BEND, Motion.KNEEL -> intArrayOf(4, 5, 5, 4)
            Motion.SIT -> intArrayOf(4, 0, 3)
            Motion.RISE -> intArrayOf(3, 0, 4)
            Motion.STRETCH, Motion.REACH -> intArrayOf(4, 6, 6, 4)
            Motion.KICK, Motion.ROLL -> null  // Fuer diesen gerichteten Schuss gibt es nur Profilzeichnungen.
        }
        val useDirected = directed != null && facing in setOf(Facing.FRONT, Facing.BACK)
        val clip = if (useDirected) directed!! else side
        val progress = if (cue.progress.isFinite()) cue.progress.coerceIn(0f, 1f) else 0f
        val index = (progress * clip.size).toInt().coerceAtMost(clip.lastIndex)
        val first = if (useDirected) {
            if (facing == Facing.BACK) Rich.BACK_ACTION_FIRST else Rich.FRONT_ACTION_FIRST
        } else Rich.ACTION_FIRST
        return first + clip[index]
    }

    /** Blickrichtung fuer Drehungen. */
    enum class Facing { RIGHT, LEFT, FRONT, BACK }

    /** Ein Bild einer Drehung. */
    data class Step(val frame: Int, val mirrored: Boolean)

    /**
     * Die Zwischenbilder, wenn sich die Figur von [from] nach [to] dreht: Von rechts nach links
     * dreht sie sich ueber die halb zugewandte Ansicht und vorn herum, statt umzuklappen.
     */
    fun turnSteps(from: Facing, to: Facing): List<Step> {
        fun tf(f: Facing) = Step(Rich.TURN_FRONT, f == Facing.LEFT)
        fun tb(f: Facing) = Step(Rich.TURN_BACK, f == Facing.LEFT)
        val sides = setOf(Facing.LEFT, Facing.RIGHT)
        return when {
            from == to -> emptyList()
            from in sides && to in sides -> listOf(tf(from), Step(Rich.FRONT, false), tf(to))
            from in sides && to == Facing.FRONT -> listOf(tf(from))
            from in sides && to == Facing.BACK -> listOf(tb(from))
            from == Facing.FRONT && to in sides -> listOf(tf(to))
            from == Facing.BACK && to in sides -> listOf(tb(to))
            from == Facing.FRONT -> listOf(tf(Facing.RIGHT), Step(Rich.IDLE_FIRST, false), tb(Facing.RIGHT))
            else -> listOf(tb(Facing.RIGHT), Step(Rich.IDLE_FIRST, false), tf(Facing.RIGHT))
        }
    }

    /** Merkt sich die letzte Blickrichtung einer Figur und spielt bei einem Wechsel die Drehung. */
    class Turn {
        private var facing: Facing? = null
        private var steps: List<Step> = emptyList()
        private var since = 0L

        fun update(now: Facing, timeMs: Long): Step? {
            val last = facing
            if (last == null) {
                facing = now
                return null
            }
            if (now != last) {
                steps = turnSteps(last, now)
                facing = now
                since = timeMs
            }
            if (steps.isEmpty()) return null
            val i = ((timeMs - since) / Rich.TURN_STEP_MS).toInt()
            if (i < 0 || i >= steps.size) {
                steps = emptyList()
                return null
            }
            return steps[i]
        }
    }

    /** Beim Losgehen am Bodenkontakt beginnen, statt mitten in einer globalen Uhrphase. */
    class GaitClock {
        private var previous: Long? = null
        private var elapsed = 0L

        fun update(moving: Boolean, timeMs: Long): Long {
            if (!moving) {
                previous = null
                elapsed = 0L
                return 0L
            }
            previous?.let { elapsed += (timeMs - it).coerceIn(0L, 100L) }
            previous = timeMs
            return elapsed
        }
    }

    /** Bild im feinen Bogen; [turn] (optional) spielt Drehungen beim Richtungswechsel. */
    fun lookRich(
        raw: IntArray,
        species: AvatarSpecies,
        side: AvatarShading.Side,
        timeMs: Long,
        direction: PlayControl.Dir? = null,
        moving: Boolean? = null,
        turn: Turn? = null,
        gaitTimeMs: Long = timeMs,
        motionCue: MotionCue? = null,
        tempo: Float = 1f,
        runBlend: Float? = null
    ): Look {
        val (act, lift, mirrored) = classify(raw, species, side, timeMs, direction, moving)
        val facing = when {
            direction == PlayControl.Dir.UP -> Facing.BACK
            direction == PlayControl.Dir.DOWN -> Facing.FRONT
            mirrored -> Facing.LEFT
            else -> Facing.RIGHT
        }
        // Im Schlaf und im Sprung keine Drehung einschieben - nur merken, wohin sie schaut.
        val step = turn?.update(facing, timeMs)
        val locomotion = act == Activity.WALK || act == Activity.FRONT_WALK || act == Activity.BACK_WALK
        if (motionCue != null && !locomotion) {
            // Strecken, Greifen und Aufstehen sind Bodenbewegungen. Das alte Raster
            // hebt dort den ganzen Koerper an; die Zeichnung enthaelt die Haltung bereits.
            return Look(motionFrame(motionCue, facing),
                if (motionCue.motion == Motion.JUMP) lift else 0, mirrored)
        }
        // Ein Wendebild darf den laufenden Gang nicht durch eine starre Gleitpose ersetzen.
        if (step != null && !locomotion && act != Activity.SLEEP && act != Activity.JOY) {
            return Look(step.frame, lift, step.mirrored)
        }
        val blend = (runBlend ?: if (tempo >= 1.7f) 1f else 0f).coerceIn(0f, 1f)
        val running = blend > 0f
        val frame = when (act) {
            Activity.SLEEP -> Rich.SLEEP_FIRST + ((timeMs / Rich.SLEEP_MS) % Rich.SLEEP_COUNT).toInt()
            Activity.BACK -> Rich.BACK_IDLE_FIRST + ((timeMs / Rich.IDLE_MS) % Rich.IDLE_COUNT).toInt()
            Activity.BACK_WALK -> if (running) Rich.BACK_RUN_FIRST + ((gaitTimeMs / (Rich.WALK_MS * 2)) % Rich.DIR_RUN_COUNT).toInt()
                else Rich.DRAWN_BACK_WALK_FIRST + ((gaitTimeMs / Rich.WALK_MS) % Rich.DRAWN_DIR_WALK_COUNT).toInt()
            Activity.FRONT -> Rich.FRONT_IDLE_FIRST + ((timeMs / Rich.IDLE_MS) % Rich.IDLE_COUNT).toInt()
            Activity.FRONT_WALK -> if (running) Rich.FRONT_RUN_FIRST + ((gaitTimeMs / (Rich.WALK_MS * 2)) % Rich.DIR_RUN_COUNT).toInt()
                else Rich.DRAWN_FRONT_WALK_FIRST + ((gaitTimeMs / Rich.WALK_MS) % Rich.DRAWN_DIR_WALK_COUNT).toInt()
            Activity.WALK -> (if (running) Rich.RUN_FIRST else Rich.WALK_FIRST) + ((gaitTimeMs / Rich.WALK_MS) % Rich.WALK_COUNT).toInt()
            Activity.JOY -> (when (facing) {
                Facing.FRONT -> Rich.FRONT_JOY_FIRST
                Facing.BACK -> Rich.BACK_JOY_FIRST
                else -> Rich.JOY_FIRST
            }) + if (lift >= 3) 2 else 1
            Activity.BLINK -> when (facing) {
                Facing.FRONT -> Rich.FRONT_BLINK
                Facing.BACK -> Rich.BACK_IDLE_FIRST + ((timeMs / Rich.IDLE_MS) % Rich.IDLE_COUNT).toInt()
                else -> Rich.BLINK
            }
            Activity.IDLE -> Rich.idleFrame(timeMs)
        }
        // Der feine Gang enthaelt seine Gewichtsverlagerung bereits. Der alte Raster-Huepfer
        // wuerde zusaetzlich beide Pfoten vom Boden anheben.
        val walkFrame = if (running && locomotion && blend < 1f) when (act) {
            Activity.FRONT_WALK -> Rich.DRAWN_FRONT_WALK_FIRST + ((gaitTimeMs / Rich.WALK_MS) % 8).toInt()
            Activity.BACK_WALK -> Rich.DRAWN_BACK_WALK_FIRST + ((gaitTimeMs / Rich.WALK_MS) % 8).toInt()
            else -> Rich.WALK_FIRST + ((gaitTimeMs / Rich.WALK_MS) % 8).toInt()
        } else null
        return Look(frame, if (locomotion) 0 else lift, mirrored, walkFrame, blend)
    }
}
