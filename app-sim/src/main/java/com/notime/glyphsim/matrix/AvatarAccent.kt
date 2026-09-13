package com.notime.glyphsim.matrix

/**
 * **Das Gesicht einer Kreatur** - die Zellen, die beim Zeichnen eine Akzentfarbe bekommen.
 *
 * ## Warum ueberhaupt ein Akzent
 *
 * Bis NT-080 war die ganze Figur in einem Ton eingefaerbt. Eine einfarbige Flaeche in
 * Kreaturform ist aber kein Charakter, sondern ein Aufkleber in Farbe - der Ton sagt zwar,
 * welches Wesen es ist, aber nichts darueber, was daran ein Gesicht ist. Der Koerper ist deshalb
 * wieder weiss wie die Welt, und die Farbe sitzt genau an der Stelle, die ein Auge zuerst sucht.
 *
 * ## Warum das Gesicht hier GEFUNDEN und nicht uebergeben wird
 *
 * Die Augen sind in [AvatarBody] als Loecher beschrieben: Sie werden aus der Silhouette
 * herausgeschnitten, nicht hinzugefuegt. Bis zum Zeichnen ist davon nichts mehr uebrig als eine
 * dunkle Stelle - ein Bild ist ein `IntArray` aus Helligkeiten, und durch [FrameCrossfade] laeuft
 * ohnehin nur noch An, Aus und Ueberblendung. Es gibt also keinen Kanal, in dem "diese Zelle ist
 * ein Auge" bis hierher mitreisen koennte, und einen einzufuehren hiesse, das gemeinsame
 * Bildformat von `:core` fuer eine Farbfrage aufzubohren.
 *
 * Gefunden wird es stattdessen an der Form selbst - genau wie [AvatarShading] seine Kante an der
 * Figur misst statt am Raster. Ein Auge ist eine dunkle Stelle, die RINGSUM von der Kreatur
 * umschlossen ist; eine Flutfuellung vom Bildrand her trennt das zuverlaessig von allem, was
 * aussen liegt. Das hat den angenehmen Nebeneffekt, dass eine siebte Kreatur ihr Gesicht
 * geschenkt bekommt, ohne dass jemand eine Liste pflegen muss.
 *
 * ## Warum erst ab zwei Zellen
 *
 * Gemessen: Bei WYRMLING schliesst der schlagende Fluegel je nach Laufphase EINZELNE Zellen am
 * Rand mit ein (gezaehlt 8, 6, 6, 7 umschlossene Zellen ueber vier Bilder). Faerbte man die mit,
 * blitzte am Fluegelrand ein farbiger Punkt auf und wieder weg - das liest sich als Fehler, nicht
 * als Gesicht.
 *
 * Augen und Mund sind dagegen bei jeder Spezies mindestens zwei Zellen gross, weil die Augen
 * ausdruecklich zwei Zeilen hoch gezeichnet sind (siehe [AvatarBody.eyesHalf]: erst dadurch gibt
 * es ueberhaupt einen weichen Blinzler). Die Grenze bei zwei zusammenhaengenden Zellen trennt
 * damit beides sauber, ohne irgendwo eine Koordinate zu nennen.
 */
object AvatarAccent {

    /** Kleinste Gruppe, die als Gesichtszug gilt - siehe Klassendoku. */
    private const val MIN_GROUP = 2

    /**
     * Die Zellen des Gesichts in [frame], als Feld von Wahrheitswerten in derselben Ordnung.
     *
     * Leer, wenn das Feld nicht zum Raster passt - eine Zeichenroutine darf an einem
     * unerwarteten Bild nicht scheitern.
     */
    fun facesIn(
        frame: IntArray,
        width: Int = AvatarGeometry.SIZE,
        height: Int = AvatarGeometry.HEIGHT
    ): BooleanArray {
        val leer = BooleanArray(0)
        if (width <= 0 || height <= 0 || frame.size != width * height) return leer

        // Alles Dunkle, das vom Bildrand aus erreichbar ist, liegt AUSSERHALB der Figur.
        val aussen = BooleanArray(frame.size)
        val warteschlange = ArrayDeque<Int>()
        fun anstossen(index: Int) {
            if (frame[index] <= 0 && !aussen[index]) {
                aussen[index] = true
                warteschlange.add(index)
            }
        }
        for (x in 0 until width) {
            anstossen(x)
            anstossen((height - 1) * width + x)
        }
        for (y in 0 until height) {
            anstossen(y * width)
            anstossen(y * width + width - 1)
        }
        while (warteschlange.isNotEmpty()) {
            val i = warteschlange.removeFirst()
            val x = i % width
            val y = i / width
            if (x > 0) anstossen(i - 1)
            if (x < width - 1) anstossen(i + 1)
            if (y > 0) anstossen(i - width)
            if (y < height - 1) anstossen(i + width)
        }

        // Was uebrig bleibt, ist umschlossen. Davon zaehlt nur, was zusammenhaengend gross genug
        // ist - siehe Klassendoku zum blitzenden Fluegelrand.
        val gesicht = BooleanArray(frame.size)
        val besucht = BooleanArray(frame.size)
        for (start in frame.indices) {
            if (frame[start] > 0 || aussen[start] || besucht[start]) continue
            val gruppe = mutableListOf<Int>()
            val offen = ArrayDeque<Int>()
            besucht[start] = true
            offen.add(start)
            while (offen.isNotEmpty()) {
                val i = offen.removeFirst()
                gruppe += i
                val x = i % width
                val y = i / width
                fun weiter(nachbar: Int) {
                    if (frame[nachbar] <= 0 && !aussen[nachbar] && !besucht[nachbar]) {
                        besucht[nachbar] = true
                        offen.add(nachbar)
                    }
                }
                if (x > 0) weiter(i - 1)
                if (x < width - 1) weiter(i + 1)
                if (y > 0) weiter(i - width)
                if (y < height - 1) weiter(i + width)
            }
            if (gruppe.size >= MIN_GROUP) for (i in gruppe) gesicht[i] = true
        }
        return gesicht
    }
}
