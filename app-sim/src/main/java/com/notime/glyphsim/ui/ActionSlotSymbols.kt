package com.notime.glyphsim.ui

import com.notime.glyphsim.matrix.MatrixGeometry
import com.notime.glyphsim.matrix.ReminderAnimations

/**
 * Ein ruhiges, erkennbares Symbol fuer den Inhalt eines Aktions-Speicherplatzes.
 *
 * Die laufende Erinnerung darf nicht einfach in ihrem ersten Frame eingefroren werden: Beim
 * Trinken ist dort das Glas noch leer, beim Zeichnen steht erst ein einzelner Pinselpunkt, und
 * importierte Animationen koennen mit einem fast leeren Auftakt beginnen. Feste Animationstypen
 * werden deshalb aus ihrer kanonischen Animation neu bestimmt; Bibliotheksanimationen fallen auf
 * ihren vollstaendigsten gespeicherten Frame zurueck. So bleibt das Symbol auch nach einem
 * Neustart unabhaengig davon, in welchem Bild die Uhr beim Ablegen gerade stand.
 */
internal object ActionSlotSymbols {

    fun frameFor(action: SavedAction): IntArray {
        if (action.libraryAnimationLabel?.trim()?.equals("Rocket", ignoreCase = true) == true) {
            return rocketFrame()
        }
        action.animationType?.let { type ->
            return mostComplete(ReminderAnimations.framesFor(type))
        }
        return mostComplete(action.frames)
    }

    /**
     * Der Frame mit den meisten sichtbaren Punkten traegt bei den vorhandenen Animationen stets
     * das fertige Motiv: das gefuellte Glas, das grosse Kreuz, das offene Buch oder den
     * geschlossenen Zeichenkreis. Bei Gleichstand gewinnt der fruehere Frame, damit die Wahl
     * dauerhaft stabil bleibt.
     */
    private fun mostComplete(frames: List<IntArray>): IntArray =
        frames.maxByOrNull { frame -> frame.count { it > 0 } }
            ?: IntArray(MatrixGeometry.SIZE * MatrixGeometry.SIZE)

    /** Eigene Silhouette, weil die Rocket-Reaktion den Avatar fliegen laesst statt eine Rakete zu zeigen. */
    private fun rocketFrame(): IntArray = pointsFrame(
        listOf(
            6 to 1,
            5 to 2, 6 to 2, 7 to 2,
            5 to 3, 7 to 3,
            4 to 4, 5 to 4, 7 to 4, 8 to 4,
            4 to 5, 6 to 5, 8 to 5,
            4 to 6, 5 to 6, 6 to 6, 7 to 6, 8 to 6,
            3 to 7, 4 to 7, 5 to 7, 6 to 7, 7 to 7, 8 to 7, 9 to 7,
            3 to 8, 5 to 8, 6 to 8, 7 to 8, 9 to 8,
            5 to 9, 7 to 9,
            6 to 10,
            6 to 11
        )
    )

    private fun pointsFrame(points: List<Pair<Int, Int>>): IntArray =
        IntArray(MatrixGeometry.SIZE * MatrixGeometry.SIZE).also { frame ->
            for ((x, y) in points) {
                if (MatrixGeometry.isActive(x, y)) {
                    frame[y * MatrixGeometry.SIZE + x] = MatrixGeometry.MAX_BRIGHTNESS
                }
            }
        }
}
