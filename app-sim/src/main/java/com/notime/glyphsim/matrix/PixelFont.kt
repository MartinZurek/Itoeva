package com.notime.glyphsim.matrix

/** Gemeinsame Schnittstelle fuer Pixel-Ziffernfonts unterschiedlicher Breite (siehe [DigitFont], [CompactDigitFont]). */
interface PixelFont {
    val digitWidth: Int
    val digitHeight: Int

    /** Punktkoordinaten (relativ, 0,0 = oben links der Ziffer) eines Zeichens. */
    fun pointsFor(char: Char): List<Pair<Int, Int>>
}
