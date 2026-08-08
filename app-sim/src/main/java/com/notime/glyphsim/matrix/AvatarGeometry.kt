package com.notime.glyphsim.matrix

/**
 * Eigenes, volles Quadrat-Raster fuer den Tamagotchi-Avatar (siehe AvatarSpriteView) -
 * im Gegensatz zu [MatrixGeometry] keine Kreisform-Maskierung (der Avatar ist keine
 * Nachbildung der runden Glyph-Matrix-Hardware, sondern ein frei gezeichnetes
 * Pixel-Sprite) und mit 16x16 deutlich feiner als das 13x13-Raster der
 * Erinnerungs-Icons, damit die Kreatur mehr Struktur/Textur zeigen kann.
 */
object AvatarGeometry {
    /** Breite des Rasters und Kantenlaenge der gezeichneten Koerper. */
    const val SIZE = 16

    /**
     * Zusaetzliche Zeilen OBERHALB der Figur - ihre Kopffreiheit.
     *
     * **Warum das noetig wurde.** Die Koerper sind auf 16x16 gezeichnet und nutzen diese Hoehe
     * teilweise voll aus: FENNECs Ohren sitzen in Zeile 0. Jede Aufwaertsbewegung - Huepfen,
     * Strecken, Gaehnen - schob sie damit aus dem Raster, und da Punkte ausserhalb beim
     * Frame-Bau stillschweigend verworfen werden (siehe FrameCrossfade), waren die Ohren im
     * Sprung schlicht ab. Gemessen: STARLET verlor bei dy=-4 drei Zeilen, HOOTLET zwei,
     * WYRMLING eine, FENNEC bei JEDER Aufwaertsbewegung die Ohrenspitzen.
     *
     * Vier Zeilen decken den groessten vorkommenden Versatz (dy = -4) ab. Sie liegen ueber der
     * Figur, nicht unter ihr: Nach unten reicht keine Bewegung hinaus, dort stuende sie ohnehin
     * im Boden.
     */
    const val HEADROOM = 4

    /** Gesamthoehe des Rasters. */
    const val HEIGHT = SIZE + HEADROOM

    const val MAX_BRIGHTNESS = 4095
}
