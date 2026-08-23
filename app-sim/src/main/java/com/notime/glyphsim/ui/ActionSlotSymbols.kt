package com.notime.glyphsim.ui

import com.notime.glyphsim.matrix.MatrixGeometry
import com.notime.glyphsim.matrix.ReminderAnimations
import com.notime.glyphcore.data.AnimationType

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
            // Der vollste Frame der laufenden Buchanimation ist fuer Bewegung gut, als kleines
            // ruhendes Slot-Symbol aber zu schmal: Buchruecken und Seiten sehen dort wie "II"
            // aus. Der Speicherplatz braucht stattdessen die klare Silhouette eines aufgeklappten
            // Buchs.
            if (type == AnimationType.BOOK) return bookFrame()
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

    /** Breites, aufgeklapptes Buch mit Mittelsteg und je einer sichtbaren Zeile pro Seite. */
    private fun bookFrame(): IntArray = pointsFrame(
        listOf(
            3 to 2, 4 to 2, 5 to 3, 7 to 3, 8 to 2, 9 to 2,
            2 to 3, 6 to 3, 10 to 3,
            2 to 4, 4 to 4, 6 to 4, 8 to 4, 10 to 4,
            2 to 5, 6 to 5, 10 to 5,
            2 to 6, 4 to 6, 6 to 6, 8 to 6, 10 to 6,
            2 to 7, 6 to 7, 10 to 7,
            2 to 8, 3 to 8, 4 to 8, 5 to 8,
            6 to 8,
            7 to 8, 8 to 8, 9 to 8, 10 to 8,
            4 to 9, 5 to 9, 6 to 9, 7 to 9, 8 to 9
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
