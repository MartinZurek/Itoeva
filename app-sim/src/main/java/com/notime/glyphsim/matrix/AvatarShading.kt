package com.notime.glyphsim.matrix

/**
 * **Gibt der flachen Silhouette Volumen** - Licht von oben, Schatten unten.
 *
 * ## Warum das noetig ist
 *
 * Jede beleuchtete Zelle einer Kreatur stand auf [AvatarGeometry.MAX_BRIGHTNESS]. Die Figur war
 * damit eine reine An/Aus-Flaeche: ein Scherenschnitt, kein Koerper. Bei einer Pixelfigur von
 * sechzehn Zellen Kantenlaenge ist das der Unterschied zwischen einem Aufkleber und etwas, das
 * im Raum steht.
 *
 * ## Warum ein Verlauf und keine Kantenerkennung
 *
 * Der erste Entwurf hat Kanten gesucht: oben hell, unten dunkel, Koerper dazwischen. Auf dem
 * Papier richtig, im Bild falsch - und zwar aus einem Grund, den man erst sieht, wenn man es
 * ausdruckt:
 *
 * - **Duenne Teile sind gleichzeitig oben UND unten.** Ein zwei Zellen hohes Ohr wurde damit
 *   hell-dunkel gestreift und las sich als kaputt, nicht als plastisch.
 * - **Abgesetzte Teile bekamen falsches Licht.** Die Fuesse haengen unter einer Luecke; ueber
 *   ihnen ist nichts, also galten sie als Oberkante und leuchteten am hellsten - ausgerechnet
 *   das Unterste der Figur.
 * - An Stellen, wo der Koerper breiter und wieder schmaler wird, wechselten die Aussenspalten
 *   zeilenweise zwischen hell und dunkel. Das flimmert, sobald sich die Figur bewegt.
 *
 * Ein **durchgehender Verlauf von oben nach unten** hat keines dieser Probleme: Er ist ueber die
 * Hoehe streng fallend, kann also gar nicht streifen, und jedes Teil bekommt genau das Licht,
 * das zu seiner Hoehe passt. Ohren oben sind hell, Fuesse unten sind dunkel, ohne dass eine
 * Regel sie kennen muesste.
 *
 * Gemessen wird an der Figur selbst, nicht am Raster: Die oberste beleuchtete Zeile ist der
 * Lichtpunkt. Sonst waere eine hockende Kreatur insgesamt dunkler als eine aufrechte, nur weil
 * sie weiter unten im Raster sitzt.
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

    /** Helligkeit an der obersten Zeile der Figur. */
    private const val LIT = 1.0f

    /** Helligkeit an der untersten. Tiefer nicht: Die Silhouette muss lesbar bleiben. */
    const val SHADOW = 0.42f

    /**
     * Wie stark das Licht zusaetzlich von LINKS kommt.
     *
     * **Erst diese Zahl macht aus dem Verlauf eine Woelbung.** Ein rein senkrechter Verlauf
     * liest sich als Ueberblendung von hell nach dunkel - eine Flaeche, die oben heller ist.
     * Mit einem schwachen seitlichen Anteil wandert der hellste Punkt in die obere linke Ecke,
     * und die Figur bekommt eine Seite, die dem Licht zugewandt ist, und eine, die sich
     * wegdreht. Das ist der Unterschied zwischen einem Farbverlauf und einem Koerper.
     *
     * Bewusst klein. Bei 0,32 sah dieselbe Kreatur nicht runder aus, sondern schief beleuchtet,
     * und die rechte Haelfte verlor an Lesbarkeit; 0,18 ist der Wert, bei dem die Woelbung
     * eintritt, ohne dass die Silhouette darunter leidet.
     *
     * Eine feste Richtung ist hier gefahrlos: Das Sprite wird nirgends gespiegelt, eine nach
     * links laufende Kreatur nimmt ihr Licht also nicht mit.
     */
    private const val SIDE = 0.18f

    /**
     * Fussfaktor fuer eine EINGEFAERBTE Figur (siehe [AvatarPalette]) - deutlich flacher als
     * [SHADOW].
     *
     * **Weiss und Farbe vertragen nicht denselben Verlauf.** Eine weisse Zelle hat fast die
     * volle Helligkeit zur Verfuegung; von der darf der Verlauf viel wegnehmen, ohne dass unten
     * etwas verschwindet. Ein farbiger Grundton liegt von Haus aus bei rund einem Drittel davon
     * - derselbe Verlauf zoege die untersten Zeilen in den Hintergrund. Gemessen am
     * Dock-Schwarz: Farbe mit 0,42 haette unten Kontrast 1,5, also praktisch nichts; heute hat
     * die weisse Figur dort 2,53.
     *
     * 0,70 ist der Wert, bei dem eine farbige Kreatur unten genau dieselbe Lesbarkeit erreicht
     * wie die weisse heute. Er ist damit keine Geschmacksfrage, sondern die Gegenrechnung zur
     * geringeren Grundhelligkeit.
     *
     * Bestaetigt vom Bild, das den Anstoss gab: Dessen Kreatur benutzt zwei Toene desselben
     * Farbtons im Verhaeltnis 0,87 - also ebenfalls einen flachen Verlauf, nicht den tiefen.
     * Eine farbige Figur braucht ihn auch nicht so tief: Sie hebt sich schon durch den Farbton
     * von der weissen Kulisse ab, waehrend eine weisse Figur dafuer allein die Helligkeit hat.
     */
    const val TINTED_SHADOW = 0.70f

    /**
     * Schattierte Kopie von [frame].
     *
     * Ein Feld, das nicht zum Raster passt, wird unveraendert zurueckgegeben, statt eine
     * Ausnahme zu werfen - eine Zeichenroutine darf an einem unerwarteten Feld nicht scheitern.
     *
     * [floor] ist die Helligkeit an der untersten Zeile der Figur: [SHADOW] fuer die weisse
     * Zeichnung, [TINTED_SHADOW] fuer eine eingefaerbte.
     */
    fun shade(
        frame: IntArray,
        width: Int = AvatarGeometry.SIZE,
        height: Int = AvatarGeometry.HEIGHT,
        floor: Float = SHADOW
    ): IntArray {
        if (width <= 0 || height <= 0 || frame.size != width * height) return frame

        var oben = -1
        var unten = -1
        var links = width
        var rechts = -1
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (frame[y * width + x] <= 0) continue
                if (oben < 0) oben = y
                unten = y
                if (x < links) links = x
                if (x > rechts) rechts = x
            }
        }
        // Leer oder nur eine Zeile hoch: Es gibt nichts zu modellieren.
        if (oben < 0 || unten <= oben) return frame

        val hoehe = (unten - oben).toFloat()
        val breite = (rechts - links).toFloat()
        val shaded = IntArray(frame.size)
        for (y in oben..unten) {
            val vonOben = LIT - (y - oben) / hoehe * (LIT - floor)
            for (x in 0 until width) {
                val index = y * width + x
                val wert = frame[index]
                if (wert <= 0) continue
                // Bei einer nur eine Spalte breiten Figur gibt es keine Seite, die sich
                // wegdreht - dann bleibt es beim reinen Verlauf von oben.
                val vonLinks = if (breite > 0f) 1f - SIDE * (x - links) / breite else 1f
                shaded[index] = (wert * vonOben * vonLinks)
                    .toInt()
                    .coerceIn(0, AvatarGeometry.MAX_BRIGHTNESS)
            }
        }
        return shaded
    }
}
