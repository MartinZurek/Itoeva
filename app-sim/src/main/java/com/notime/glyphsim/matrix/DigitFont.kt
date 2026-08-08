package com.notime.glyphsim.matrix

/**
 * Eigener, simpler 4 (breit) x 5 (hoch) Pixel-Font fuer die Ziffern 0-9. Das
 * :app-Modul zeichnet Ziffern ueber GlyphMatrixObject.setText() - das nutzt einen
 * eingebauten Font aus dem Nothing-SDK, dessen genaue Pixelform nicht Teil dieses
 * Repos ist. Fuer die Simulation hier deshalb ein eigenes, handgezeichnetes
 * 4x5-Punktraster pro Ziffer statt eines Nachbaus des SDK-Fonts.
 *
 * 5 statt 6 Zeilen hoch, damit Stunden- und Minutenblock zusammen mit 1 Zeile
 * Abstand (5+1+5=11) innerhalb der runden Matrix bleiben - siehe Kommentar in
 * ClockFrameSim.kt. Jede Ziffer hatte in der 6-zeiligen Fassung eine
 * redundante Doppelzeile (z.B. zwei identische Mittelzeilen), die hier jeweils
 * einmal entfernt wurde; die Form bleibt dadurch unveraendert erkennbar.
 */
object DigitFont : PixelFont {
    // Jede Ziffer: 5 Zeilen, je 4 Zeichen ('1' = Pixel an, '0' = aus).
    private val DIGITS: Map<Char, List<String>> = mapOf(
        '0' to listOf("1111", "1001", "1001", "1001", "1111"),
        '1' to listOf("0010", "0110", "0010", "0010", "0111"),
        '2' to listOf("1111", "0001", "1111", "1000", "1111"),
        '3' to listOf("1111", "0001", "0111", "0001", "1111"),
        '4' to listOf("1001", "1001", "1111", "0001", "0001"),
        '5' to listOf("1111", "1000", "1111", "0001", "1111"),
        '6' to listOf("1111", "1000", "1111", "1001", "1111"),
        '7' to listOf("1111", "0001", "0001", "0001", "0001"),
        '8' to listOf("1111", "1001", "1111", "1001", "1111"),
        '9' to listOf("1111", "1001", "1111", "0001", "1111")
    )

    const val DIGIT_WIDTH = 4
    const val DIGIT_HEIGHT = 5

    override val digitWidth = DIGIT_WIDTH
    override val digitHeight = DIGIT_HEIGHT

    override fun pointsFor(char: Char): List<Pair<Int, Int>> {
        val rows = DIGITS[char] ?: return emptyList()
        return rows.flatMapIndexed { y, row ->
            row.mapIndexedNotNull { x, c -> if (c == '1') x to y else null }
        }
    }
}
