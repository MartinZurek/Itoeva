package com.notime.glyphsim.matrix

/**
 * **Gibt der flachen Silhouette Volumen** - eine beschattete Flanke neben der beleuchteten.
 *
 * ## Warum das noetig ist
 *
 * Jede beleuchtete Zelle einer Kreatur stand auf [AvatarGeometry.MAX_BRIGHTNESS]. Die Figur war
 * damit eine reine An/Aus-Flaeche: ein Scherenschnitt, kein Koerper.
 *
 * ## Warum ZWEI Toene und kein Verlauf
 *
 * Der erste Entwurf war ein weicher Verlauf von oben nach unten. Der war unsichtbar - und das
 * ist keine Geschmacksfrage, sondern nachgemessen: Verteilt man dreissig Prozent Helligkeits-
 * unterschied auf sechzehn Zeilen, betraegt der Sprung von einer Zeile zur naechsten zwei
 * Prozent. Zwei benachbarte Zellen unterscheiden sich damit um nichts, was ein Auge trennt. Die
 * Figur war messbar schattiert und sah trotzdem aus wie vorher.
 *
 * Das Bild, das den Anstoss gegeben hat, macht es anders. Ausgezaehlt hat seine Kreatur **genau
 * zwei Toene** - 8876 Pixel im Grundton, 1896 im Schattenton, keinen einzigen Zwischenwert. Und
 * der Schattenton liegt nicht unten, sondern als **Band auf einer Flanke**: ueber die volle
 * Hoehe, ueber die aeusseren rund dreissig Prozent der Breite.
 *
 * Genau daran liegt es. Was ein Auge als Koerper liest, ist nicht ein sanfter Uebergang, sondern
 * eine **Kante** zwischen zwei Flaechen. Zwei flache Toene mit einer harten Grenze zeigen mehr
 * Volumen als sechzehn feine Stufen.
 *
 * ## Die Flanke sagt, wohin die Figur geht
 *
 * Weil der Schatten auf EINER Seite liegt, kostet es nichts, ihn die Seite wechseln zu lassen -
 * und dann traegt er zusaetzlich die Laufrichtung.
 *
 * Im Stand liegt er rechts, wie im Vorbild. Laeuft die Kreatur nach LINKS, dreht sie sich, und
 * die beschattete Flanke wandert mit: Die Kante springt auf die linke Seite. Nach rechts bleibt
 * sie, wo sie im Stand war - dieselbe Richtung, dasselbe Bild.
 *
 * Das ist der einzige Hinweis darauf, dass die Figur sich umgedreht hat: Das Sprite wird
 * nirgends gespiegelt (geprueft), also traegt allein die Kante die Richtung.
 *
 * ## Wo das angewandt wird
 *
 * Beim ZEICHNEN ([AvatarSpriteView]), nicht in den Animationsdaten. Die Posen bleiben reine
 * Punktmengen, Ueberblendungen rechnen unveraendert weiter, und die abgelegten Vergleichsbilder
 * der Reaktionspruefung bleiben gueltig. Schattierung ist Darstellung, nicht Inhalt.
 *
 * Die vorhandene Helligkeit wird **skaliert**, nicht ersetzt: Eine Zelle, die gerade
 * eingeblendet wird, bleibt halb sichtbar und bekommt zusaetzlich ihre Schattierung. Dasselbe
 * gilt fuer die Daempfung eines Besuchers.
 */
object AvatarShading {

    /**
     * Helligkeit der beschatteten Flanke, als Anteil der beleuchteten.
     *
     * Aus dem Vorbild uebernommen: #BE684D zu #D97757 ist auf allen drei Kanaelen dasselbe
     * Verhaeltnis, 0,875. Der Unterschied ist klein - er wirkt, WEIL er als harte Kante zwischen
     * zwei Flaechen steht und nicht als Verlauf ueber die ganze Figur verteilt wird.
     */
    const val SHADE = 0.875f

    /**
     * Wie breit das Schattenband ist, als Anteil der Figurenbreite.
     *
     * Gemessen am Vorbild: Der dunkle Ton liegt dort von Pixel 654 bis 700 bei einer Figur von
     * 545 bis 700 - also die aeusseren 30 Prozent.
     */
    private const val SHADE_WIDTH = 0.30f

    /** Auf welcher Flanke der Schatten liegt. */
    enum class Side {
        /** Stand, oder Gang nach rechts - Schatten auf der rechten Flanke wie im Vorbild. */
        RIGHT,

        /** Gang nach links: Die Kreatur dreht sich, die beschattete Flanke wandert mit. */
        LEFT
    }

    /**
     * Schattierte Kopie von [frame].
     *
     * Ein Feld, das nicht zum Raster passt, wird unveraendert zurueckgegeben, statt eine
     * Ausnahme zu werfen - eine Zeichenroutine darf an einem unerwarteten Feld nicht scheitern.
     *
     * [side] ist die beschattete Flanke: [Side.RIGHT] im Stand und beim Gang nach rechts,
     * [Side.LEFT] beim Gang nach links.
     */
    fun shade(
        frame: IntArray,
        width: Int = AvatarGeometry.SIZE,
        height: Int = AvatarGeometry.HEIGHT,
        side: Side = Side.RIGHT
    ): IntArray {
        if (width <= 0 || height <= 0 || frame.size != width * height) return frame

        var links = width
        var rechts = -1
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (frame[y * width + x] <= 0) continue
                if (x < links) links = x
                if (x > rechts) rechts = x
            }
        }
        // Leer oder nur eine Spalte breit: Es gibt keine zweite Flanke, also nichts zu trennen.
        if (rechts <= links) return frame

        // Die Kante wird an der Figur selbst gemessen, nicht am Raster. Sonst saesse sie bei
        // einer schmalen Kreatur neben ihr statt in ihr.
        val breite = rechts - links + 1
        val band = (breite * SHADE_WIDTH).toInt().coerceAtLeast(1)
        val imSchatten: (Int) -> Boolean = when (side) {
            Side.RIGHT -> { x -> x > rechts - band }
            Side.LEFT -> { x -> x < links + band }
        }

        val shaded = IntArray(frame.size)
        for (index in frame.indices) {
            val wert = frame[index]
            if (wert <= 0) continue
            shaded[index] = if (imSchatten(index % width)) {
                (wert * SHADE).toInt().coerceIn(0, AvatarGeometry.MAX_BRIGHTNESS)
            } else {
                wert
            }
        }
        return shaded
    }
}
