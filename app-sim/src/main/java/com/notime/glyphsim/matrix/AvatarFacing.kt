package com.notime.glyphsim.matrix

/**
 * **Die Figur schaut dorthin, wohin sie geht.**
 *
 * ## Warum
 *
 * Gemeldet: "Wenn er nach links geht, ist der Blick trotzdem nach rechts geneigt." Alle Posen
 * sind nach rechts gezeichnet, und bis hierher wurde das Sprite nirgends gespiegelt (siehe
 * [AvatarShading]) - eine Kreatur, die nach links lief, lief rueckwaerts: Gesicht und Schnauze
 * zeigten nach rechts, nur die Schattenkante verriet die Richtung.
 *
 * ## Was gespiegelt wird - und was nicht
 *
 * Nur der GANG nach links ([AvatarShading.Side.RIGHT], die Kreatur kommt von rechts). Im Stand
 * und bei allen Handlungen bleibt die gezeichnete Blickrichtung nach rechts. Das ist Absicht:
 * Viele Handlungen stehen neben einem Gegenstand, der passend zur rechts blickenden Pose steht
 * (Angel, Tisch, Automat). Gespiegelt griffe die Figur ins Leere.
 *
 * ## Wo das angewandt wird
 *
 * Wie die Schattierung beim ZEICHNEN, nicht in den Animationsdaten: in [AvatarSpriteView] und
 * [PlayClipRenderer], jeweils vor Gesichtssuche und Schattierung. So bleiben die abgelegten
 * Vergleichsbilder der Reaktionspruefung gueltig, und Bildschirm und Clip zeigen dasselbe.
 */
object AvatarFacing {

    /** Ob eine Figur mit dieser Schattenseite gespiegelt gezeichnet wird. */
    fun mirrors(side: AvatarShading.Side): Boolean = side == AvatarShading.Side.RIGHT

    /**
     * [frame] so, wie es fuer die Laufrichtung [side] gezeichnet wird - gespiegelt beim Gang
     * nach links, sonst unveraendert (dasselbe Feld, keine Kopie).
     */
    fun orient(
        frame: IntArray,
        side: AvatarShading.Side,
        width: Int = AvatarGeometry.SIZE,
        height: Int = AvatarGeometry.HEIGHT
    ): IntArray = if (mirrors(side)) mirror(frame, width, height) else frame

    /**
     * Waagerecht gespiegelte Kopie von [frame].
     *
     * Ein Feld, das nicht zum Raster passt, wird unveraendert zurueckgegeben - wie bei
     * [AvatarShading.shade] darf eine Zeichenroutine daran nicht scheitern.
     */
    fun mirror(
        frame: IntArray,
        width: Int = AvatarGeometry.SIZE,
        height: Int = AvatarGeometry.HEIGHT
    ): IntArray {
        if (width <= 0 || height <= 0 || frame.size != width * height) return frame
        return IntArray(frame.size) { index ->
            val y = index / width
            val x = index % width
            frame[y * width + (width - 1 - x)]
        }
    }
}
