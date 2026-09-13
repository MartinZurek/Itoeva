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
 * ## Der Schatten gehoert zur BEWEGUNG, nicht zur Figur
 *
 * Ein Schatten, der immer da ist, ist ein Muster auf der Haut - man gewoehnt sich in Sekunden
 * daran und sieht ihn danach nicht mehr. Er sagt dann auch nichts: Er ist im Stand derselbe wie
 * im Lauf.
 *
 * Deshalb steht die Kreatur **ohne Schatten** da, und er erscheint nur, solange sie geht - auf
 * der Flanke, von der sie KOMMT. Wer nach rechts laeuft, ist hinten links beschattet; wer nach
 * links laeuft, hinten rechts. Beim Anhalten verschwindet er wieder.
 *
 * Damit traegt er zwei Dinge auf einmal: dass sich die Figur bewegt, und wohin. Das Sprite wird
 * nirgends gespiegelt (geprueft) - die Kante ist also das Einzige, woran man die Richtung
 * ueberhaupt ablesen kann.
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

    /** Auf welcher Flanke der Schatten liegt - oder auf keiner. */
    enum class Side {
        /** Die Figur steht. Kein Schatten, siehe Klassendoku. */
        NONE,

        /** Gang nach LINKS - beschattet ist die rechte Flanke, von der sie kommt. */
        RIGHT,

        /** Gang nach RECHTS - beschattet ist die linke Flanke, von der sie kommt. */
        LEFT
    }

    /**
     * Schattierte Kopie von [frame].
     *
     * Ein Feld, das nicht zum Raster passt, wird unveraendert zurueckgegeben, statt eine
     * Ausnahme zu werfen - eine Zeichenroutine darf an einem unerwarteten Feld nicht scheitern.
     *
     * [side] ist die beschattete Flanke - im Stand [Side.NONE], beim Gehen die Seite, von der
     * die Kreatur kommt.
     */
    fun shade(
        frame: IntArray,
        width: Int = AvatarGeometry.SIZE,
        height: Int = AvatarGeometry.HEIGHT,
        side: Side = Side.NONE
    ): IntArray {
        if (side == Side.NONE) return frame
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
            Side.NONE -> return frame
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
