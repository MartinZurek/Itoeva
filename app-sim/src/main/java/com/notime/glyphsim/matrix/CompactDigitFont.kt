package com.notime.glyphsim.matrix

/**
 * Schmalerer 3 (breit) x 5 (hoch) Pixel-Font fuer die Ziffern 0-9 - dieselben
 * Formen wie [DigitFont], nur um eine Spalte schmaler. Ein Ziffernpaar braucht
 * damit nur 3+1+3=7 statt 9 Spalten, sodass in den kritischen Randzeilen der
 * runden Matrix (y=1/y=11, dort sind nur x=2..10 ueberhaupt aktiv - siehe
 * [MatrixGeometry]) auf jeder Seite mindestens 1 Spalte sichtbarer Abstand zum
 * runden Rand bleibt, statt ihn randlos auszufuellen wie [DigitFont] es tut.
 */
object CompactDigitFont : PixelFont {
    private val DIGITS: Map<Char, List<String>> = mapOf(
        '0' to listOf("111", "101", "101", "101", "111"),
        '1' to listOf("010", "110", "010", "010", "111"),
        '2' to listOf("111", "001", "111", "100", "111"),
        '3' to listOf("111", "001", "111", "001", "111"),
        '4' to listOf("101", "101", "111", "001", "001"),
        '5' to listOf("111", "100", "111", "001", "111"),
        '6' to listOf("111", "100", "111", "101", "111"),
        '7' to listOf("111", "001", "001", "001", "001"),
        '8' to listOf("111", "101", "111", "101", "111"),
        '9' to listOf("111", "101", "111", "001", "111")
    )

    override val digitWidth = 3
    override val digitHeight = 5

    override fun pointsFor(char: Char): List<Pair<Int, Int>> {
        val rows = DIGITS[char] ?: return emptyList()
        return rows.flatMapIndexed { y, row ->
            row.mapIndexedNotNull { x, c -> if (c == '1') x to y else null }
        }
    }
}
