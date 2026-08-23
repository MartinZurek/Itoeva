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
            // Bewegte Animationsframes muessen im Ablauf funktionieren; ein winziges ruhendes
            // Piktogramm hat eine andere Aufgabe. Die Motive, deren "vollster" Frame im Slot nur
            // als Ornament oder Pixelblock lesbar war, bekommen deshalb eine klare Silhouette.
            when (type) {
                AnimationType.FOCUS -> return focusFrame()
                AnimationType.DRINK -> return drinkFrame()
                AnimationType.WORK -> return workFrame()
                AnimationType.SLEEP -> return sleepFrame()
                AnimationType.BOOK -> return bookFrame()
                AnimationType.CREATIVITY -> return creativityFrame()
                else -> Unit
            }
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

    /** Zielscheibe mit eindeutigem Mittelpunkt statt des ornamentalen Fokus-Animationsframes. */
    private fun focusFrame(): IntArray = pointsFrame(
        listOf(
            5 to 2, 6 to 2, 7 to 2,
            3 to 3, 4 to 3, 8 to 3, 9 to 3,
            3 to 4, 9 to 4,
            2 to 5, 5 to 5, 6 to 5, 7 to 5, 10 to 5,
            2 to 6, 5 to 6, 6 to 6, 7 to 6, 10 to 6,
            2 to 7, 5 to 7, 6 to 7, 7 to 7, 10 to 7,
            3 to 8, 9 to 8,
            3 to 9, 4 to 9, 8 to 9, 9 to 9,
            5 to 10, 6 to 10, 7 to 10
        )
    )

    /** Becher als Umriss mit Henkel und sichtbarer Fluessigkeitskante. */
    private fun drinkFrame(): IntArray = pointsFrame(
        listOf(
            3 to 3, 4 to 3, 5 to 3, 6 to 3, 7 to 3, 8 to 3,
            3 to 4, 8 to 4, 9 to 4, 10 to 4,
            3 to 5, 4 to 5, 5 to 5, 6 to 5, 7 to 5, 8 to 5, 10 to 5,
            3 to 6, 8 to 6, 10 to 6,
            3 to 7, 8 to 7, 9 to 7, 10 to 7,
            4 to 8, 5 to 8, 6 to 8, 7 to 8
        )
    )

    /** Aktentasche mit Griff, Verschluss und klarer Bodenlinie. */
    private fun workFrame(): IntArray = pointsFrame(
        listOf(
            5 to 2, 6 to 2, 7 to 2,
            4 to 3, 5 to 3, 6 to 3, 7 to 3, 8 to 3,
            2 to 4, 3 to 4, 4 to 4, 5 to 4, 6 to 4, 7 to 4, 8 to 4, 9 to 4, 10 to 4,
            2 to 5, 6 to 5, 10 to 5,
            2 to 6, 3 to 6, 4 to 6, 5 to 6, 6 to 6, 7 to 6, 8 to 6, 9 to 6, 10 to 6,
            2 to 7, 6 to 7, 10 to 7,
            2 to 8, 10 to 8,
            2 to 9, 3 to 9, 4 to 9, 5 to 9, 6 to 9, 7 to 9, 8 to 9, 9 to 9, 10 to 9
        )
    )

    /** Bett mit Kopfkissen und einem frei stehenden Z als Schlafzeichen. */
    private fun sleepFrame(): IntArray = pointsFrame(
        listOf(
            8 to 2, 9 to 2, 10 to 2,
            9 to 3,
            8 to 4, 9 to 4, 10 to 4,
            2 to 6,
            2 to 7, 3 to 7, 4 to 7, 5 to 7, 6 to 7, 7 to 7, 8 to 7, 9 to 7, 10 to 7,
            2 to 8, 4 to 8, 10 to 8,
            2 to 9, 3 to 9, 4 to 9, 5 to 9, 6 to 9, 7 to 9, 8 to 9, 9 to 9, 10 to 9,
            2 to 10, 10 to 10
        )
    )

    /** Gluebirne mit Strahlen, Fassung und einem einzelnen hellen Kern. */
    private fun creativityFrame(): IntArray = pointsFrame(
        listOf(
            6 to 1,
            3 to 2, 9 to 2,
            4 to 3, 5 to 3, 6 to 3, 7 to 3, 8 to 3,
            2 to 4, 4 to 4, 8 to 4, 10 to 4,
            4 to 5, 6 to 5, 8 to 5,
            4 to 6, 5 to 6, 6 to 6, 7 to 6, 8 to 6,
            5 to 7, 6 to 7, 7 to 7,
            5 to 8, 6 to 8, 7 to 8,
            5 to 9, 7 to 9,
            5 to 10, 6 to 10, 7 to 10
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
