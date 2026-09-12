package com.notime.glyphsim.ui

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.living.SymbolicIntent
import com.notime.glyphsim.matrix.MatrixGeometry

/**
 * Das Bild zu einem Symbol - die letzte Stufe zwischen Entscheidung und Bildschirm.
 *
 * [com.notime.glyphsim.living.LivingSymbols] sagt, WELCHES Symbol gilt; diese Datei sagt, wie es
 * aussieht. Getrennt, weil das eine reine Weltlogik ist und das andere Pixelkunst: Ein neues
 * Ziel aendert die Bedeutung, ein neues Motiv nur das Aussehen, und beides soll sich einzeln
 * aendern lassen.
 *
 * ## Warum fast alles wiederverwendet ist
 *
 * Fuenf der sieben heute erreichbaren Symbole gibt es schon - als Piktogramme der
 * Speicherplaetze ([ActionSlotSymbols]). Das ist kein Sparen, sondern der Punkt: Wer im Slot ein
 * Buch sieht und ueber dem Kopf ein anderes, lernt zwei Zeichen fuer dieselbe Sache. Diese Datei
 * schlaegt deshalb im vorhandenen Katalog nach und zeichnet nur, was dort fehlt.
 *
 * Neu gezeichnet sind genau zwei: das Fragezeichen und das Nein. Beide haben in der Welt der
 * zwoelf Animationstypen kein Gegenstueck, weil sie keine Taetigkeit sind, sondern eine Haltung
 * dazu.
 *
 * ## Der Kompromiss, der hier benannt gehoert
 *
 * [SymbolicIntent.FOOD] zeigt den Becher aus [AnimationType.DRINK]. In dieser Welt ist DRINK das
 * Thema der Kueche und der Nahrungsaufnahme; ein eigenes Essenssymbol gibt es nicht. Der Becher
 * ist damit das naechstliegende vorhandene Zeichen und trotzdem nicht dasselbe wie "hungrig".
 * Wenn es stoert, ist das eine Zeichnung und keine Umbauarbeit.
 */
internal object LivingSymbolFrames {

    /**
     * Das Motiv zu [intent], oder `null`, wenn es dafuer noch keines gibt.
     *
     * `null` ist kein Versehen: [SymbolicIntent] fuehrt elf Werte, weil es auch die
     * Verstaendigung zwischen zwei Wesen traegt. Ueber dem Kopf erscheinen davon heute nur die
     * sieben, die [com.notime.glyphsim.living.LivingSymbols] tatsaechlich erzeugen kann. Fuer
     * `YES` und `SURPRISE` jetzt schon Piktogramme zu zeichnen hiesse, Kunst auf Verdacht zu
     * machen - ein Test haelt dafuer fest, dass jedes ERREICHBARE Symbol ein Bild hat.
     */
    fun frameFor(intent: SymbolicIntent): IntArray? = when (intent) {
        SymbolicIntent.FOOD -> ActionSlotSymbols.frameFor(AnimationType.DRINK)
        SymbolicIntent.TIRED -> ActionSlotSymbols.frameFor(AnimationType.SLEEP)
        SymbolicIntent.PLAY -> ActionSlotSymbols.frameFor(AnimationType.MOVE)
        SymbolicIntent.AFFECTION -> ActionSlotSymbols.frameFor(AnimationType.LOVE)
        SymbolicIntent.WORK -> ActionSlotSymbols.frameFor(AnimationType.WORK)
        SymbolicIntent.MUSIC -> ActionSlotSymbols.frameFor(AnimationType.CREATIVITY)
        SymbolicIntent.HOME -> ActionSlotSymbols.frameFor(AnimationType.REST)
        SymbolicIntent.QUESTION -> questionFrame()
        SymbolicIntent.NO -> noFrame()
        SymbolicIntent.YES, SymbolicIntent.SURPRISE -> null
    }

    /**
     * Fragezeichen: Haken, Stiel, Luecke, Punkt.
     *
     * Einzellig gezeichnet wie [ActionSlotSymbols] es haelt - die vorhandenen Piktogramme sind
     * Umrisse, und ein fetter Strich daneben saehe aus wie aus einer anderen App. Die Luecke vor
     * dem Punkt ist die eine Zelle, an der das Zeichen kippt: ohne sie liest es sich als Haken
     * mit Schwanz.
     */
    private fun questionFrame(): IntArray = pointsFrame(
        listOf(
            4 to 2, 5 to 2, 6 to 2, 7 to 2,
            3 to 3, 8 to 3,
            8 to 4,
            6 to 5, 7 to 5, 8 to 5,
            5 to 6, 6 to 6,
            5 to 7,
            5 to 8,
            5 to 10
        )
    )

    /**
     * Nein: ein schlankes Kreuz von Ecke zu Ecke.
     *
     * **Kein Verbotszeichen.** Der Kreis mit Schraegstrich waere international lesbar und ist auf
     * dreizehn Zellen nicht einloesbar - der Strich verschmilzt mit dem Kreisrand, und uebrig
     * bleibt eine verbeulte Null. Ein zweizelliges X wiederum verklumpt in der Mitte. Diese
     * Fassung ist einzellig und damit genauso schwer wie das Auge und der Becher daneben.
     */
    private fun noFrame(): IntArray = pointsFrame(
        listOf(
            3 to 3, 9 to 3,
            4 to 4, 8 to 4,
            5 to 5, 7 to 5,
            6 to 6,
            5 to 7, 7 to 7,
            4 to 8, 8 to 8,
            3 to 9, 9 to 9
        )
    )

    /** Wie in [ActionSlotSymbols]: Zellen ausserhalb des runden Bildschirms fallen weg. */
    private fun pointsFrame(points: List<Pair<Int, Int>>): IntArray =
        IntArray(MatrixGeometry.SIZE * MatrixGeometry.SIZE).also { frame ->
            for ((x, y) in points) {
                if (MatrixGeometry.isActive(x, y)) {
                    frame[y * MatrixGeometry.SIZE + x] = MatrixGeometry.MAX_BRIGHTNESS
                }
            }
        }
}
