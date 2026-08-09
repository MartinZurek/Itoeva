package com.notime.glyphsim.matrix

import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * **Gloops Koerper wird GERECHNET, nicht gezeichnet.**
 *
 * Die anderen fuenf Kreaturen sind feste Punktlisten mit kleinen Zusaetzen - ein zuckendes Ohr,
 * ein schlagender Fluegel. Fuer einen Schleim reicht das nicht, und zwar aus einem grundsaetzlichen
 * Grund: Ein Ohr wird HINZUGEFUEGT, aber Quetschen nimmt oben etwas WEG und setzt es seitlich
 * wieder an. Mit einer festen Silhouette plus Zusatzzellen laesst sich das nicht ausdruecken -
 * genau deshalb hatte Gloop bisher vier Zellen Ausbeulung und bewegte sich sonst wie ein Stein
 * mit Auge.
 *
 * **Ein Tropfen ist zum Glueck die Form, die sich am billigsten rechnen laesst.** Eine Ellipse
 * braucht zwei Zahlen. Verformung heisst dann: die eine groesser, die andere kleiner.
 *
 * **Die FLAECHE bleibt dabei gleich, und das ist der ganze Trick.** Wird er breiter, wird er
 * flacher; wird er schmaler, wird er hoeher. Genau daran erkennt man Gummi: Ein Koerper, der beim
 * Breiterwerden einfach groesser wird, sieht aufgeblasen aus, einer mit gleichbleibender Flaeche
 * sieht gedrueckt aus. Es ist derselbe Grundsatz, mit dem Zeichentrick seit jeher Elastizitaet
 * herstellt.
 *
 * **Der Boden bleibt liegen.** Beim Quetschen sinkt der Scheitel, nicht die Unterseite - ein
 * Klumpen, der beim Zusammendruecken in den Boden faehrt, sieht aus wie ein Fehler in der
 * Geometrie und nicht wie etwas Weiches.
 */
object GloopShape {

    /** Unterste Zeile - Gloop sitzt auf dem Boden und bleibt dort, was auch immer er tut. */
    const val BOTTOM_ROW = 14

    /** Mitte auf dem 16 Zellen breiten Raster. */
    private const val CENTER_X = 7.5f

    /** Halbe Breite und halbe Hoehe in Ruhe - daraus ergibt sich die gleichbleibende Flaeche. */
    private const val REST_RX = 4.0f
    private const val REST_RY = 4.5f

    /** Wieviel eine Stufe Verformung ausmacht. */
    private const val STEP = 0.16f

    /**
     * Der Koerper bei einer bestimmten Verformung.
     *
     * [squash] ist die Stufe: 0 ist Ruhe, positive Werte druecken ihn flach und breit, negative
     * ziehen ihn hoch und schmal. Sinnvoll sind etwa -3 bis +3; darueber hinaus wird geklemmt,
     * damit keine Bewegung ihn versehentlich zu einem Strich oder einer Scheibe macht.
     */
    fun body(squash: Int): List<Pair<Int, Int>> {
        val level = squash.coerceIn(-3, 3)
        val rx = REST_RX * (1f + STEP * level)
        // Flaeche bleibt: rx * ry ist unveraenderlich.
        val ry = (REST_RX * REST_RY) / rx

        val cells = ArrayList<Pair<Int, Int>>(64)
        val topRow = (BOTTOM_ROW - 2 * ry + 1).roundToInt()
        for (y in topRow..BOTTOM_ROW) {
            // Der Mittelpunkt der Ellipse liegt eine halbe Hoehe ueber der Unterseite.
            val dy = (y + 0.5f) - (BOTTOM_ROW + 1 - ry)
            val inside = 1f - (dy * dy) / (ry * ry)
            if (inside <= 0f) continue
            val halfWidth = rx * sqrt(inside)
            val from = (CENTER_X - halfWidth + 0.5f).roundToInt()
            val to = (CENTER_X + halfWidth - 0.5f).roundToInt()
            for (x in from..to) {
                if (x in 0 until AvatarGeometry.SIZE) cells += x to y
            }
        }
        return cells
    }

    /**
     * Ein ausgestreckter Fortsatz - Gloops Ersatz fuer einen Arm.
     *
     * **Er hat keine Gliedmassen, und genau das ist die Gelegenheit.** Wo andere Kreaturen einen
     * Arm heben, quillt bei ihm ein Stueck Koerper heraus und zieht sich wieder ein. Das
     * unterscheidet ihn nicht nur im Aussehen, sondern in der Art, wie er ueberhaupt etwas tut.
     *
     * [extent] ist die Laenge in Zellen, [toRight] die Richtung. Der Fortsatz wird nach aussen hin
     * duenner: Ein gleichbleibend dicker Balken saehe aus wie ein angeklebtes Brett.
     */
    fun reach(extent: Int, toRight: Boolean = true, row: Int = 10): List<Pair<Int, Int>> {
        if (extent <= 0) return emptyList()
        val cells = ArrayList<Pair<Int, Int>>(extent * 2)
        for (step in 1..extent) {
            val x = if (toRight) (CENTER_X + REST_RX - 1 + step).roundToInt()
            else (CENTER_X - REST_RX + 1 - step).roundToInt()
            if (x !in 0 until AvatarGeometry.SIZE) continue
            cells += x to row
            // Die erste Haelfte bleibt zwei Zellen dick, danach laeuft er spitz aus.
            if (step <= extent / 2) cells += x to (row + 1)
        }
        return cells
    }

    /**
     * Ein Tropfen, der sich vom Scheitel loest - die kleinste Regung, die es fuer ihn gibt.
     *
     * Ein Schleim, der vollkommen still steht, ist ein Stein. Ein einzelner Punkt, der ueber ihm
     * schwebt und wieder eintaucht, kostet eine Zelle und sagt "weich".
     */
    fun bead(step: Int, squash: Int = 0): List<Pair<Int, Int>> {
        if (step < 0) return emptyList()
        val top = body(squash).minOfOrNull { it.second } ?: return emptyList()
        return listOf(7 to (top - 1 - step).coerceAtLeast(0))
    }
}
