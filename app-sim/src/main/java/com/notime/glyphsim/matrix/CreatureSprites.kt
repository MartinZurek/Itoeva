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
    data class Look(val frame: Int, val liftCells: Int, val mirrored: Boolean)

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
        val mirrored = direction != PlayControl.Dir.UP && direction != PlayControl.Dir.DOWN && AvatarFacing.mirrors(side)
        val box = bounds(raw, AvatarGeometry.SIZE, AvatarGeometry.HEIGHT)
            ?: return Look(IDLE, 0, mirrored)
        val ground = AvatarBodies.forSpecies(species).groundRow()
        val lift = (ground - box[1]).coerceAtLeast(0)
        val eyesOpen = AvatarAccent.eyesIn(raw).any { it }
        val lying = box[1] - box[0] + 1 < standHeightOf(species) * LYING
        val walking = moving ?: (side != AvatarShading.Side.NONE)
        val frame = when {
            !eyesOpen && lying -> SLEEP_FIRST + ((timeMs / SLEEP_MS) % 2).toInt()
            direction == PlayControl.Dir.UP -> if (walking) BACK_WALK_FIRST + ((timeMs / WALK_MS) % 2).toInt() else BACK
            direction == PlayControl.Dir.DOWN -> if (walking) FRONT_WALK_FIRST + ((timeMs / WALK_MS) % 2).toInt() else FRONT
            // Gehen zuerst: Der alte Gang huepft selbst eine Zelle - das ist kein Jubel.
            walking -> WALK_FIRST + ((timeMs / WALK_MS) % 4).toInt()
            // Erst ein richtiger Sprung ist Freude; ein kleiner Wipper im Stand bleibt Ruhe.
            lift >= JUMP -> JOY_FIRST + ((timeMs / JOY_MS) % 2).toInt()
            !eyesOpen -> BLINK
            timeMs % BLINK_EVERY_MS < BLINK_MS -> BLINK
            else -> if ((timeMs / BREATH_MS) % 2 == 0L) IDLE else IDLE_BREATH
        }
        return Look(frame, lift, mirrored)
    }
}
