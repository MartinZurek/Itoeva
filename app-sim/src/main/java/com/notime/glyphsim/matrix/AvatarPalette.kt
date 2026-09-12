package com.notime.glyphsim.matrix

/**
 * **Jede Kreatur bekommt eine eigene Farbe** - vorher waren alle sechs derselbe warme Weisston
 * ([MatrixColors.LED_ON]).
 *
 * ## Nur die Kreatur, nicht die Welt
 *
 * Eingefaerbt wird ausschliesslich das Avatar-Sprite. Das Zimmer, der Park, die Stationen
 * ([PlaySceneView]), die simulierte Glyph-Matrix ([SimulatedMatrixView]) und die Symbole in
 * Wunsch- und Traumblase bleiben weiss.
 *
 * Das ist keine Sparsamkeit, sondern folgt der Trennung, die dieses Projekt ohnehin zieht:
 * [SimulatedMatrixView] bildet ECHTE Hardware nach, deren LEDs weiss sind - eine farbige
 * Nachbildung waere schlicht falsch. Der Avatar dagegen ist ausdruecklich keine Nachbildung,
 * sondern ein frei gezeichnetes Sprite (siehe [AvatarGeometry]), also darf er Farbe haben. Und
 * eine farbige Figur vor weisser Kulisse ist genau das Bild, das den Anstoss gegeben hat.
 *
 * ## Woher die Farben kommen
 *
 * Nicht erfunden: Jede Kreatur traegt die Akzentfarbe ihres Schwerpunkts
 * ([AvatarSpecies.signatureTopic], Farben aus `ui/AnimationVisuals.kt`). Der weise Beobachter ist
 * violett, weil FOCUS violett ist; der kleine Motivator orange, weil MOVE orange ist. Damit gibt
 * es nur eine Quelle fuer "welche Farbe gehoert zu welchem Thema", und eine Erinnerung in der
 * Liste hat denselben Ton wie das Wesen, das sich darum kuemmert.
 *
 * ## Warum die Werte trotzdem nicht die aus der Liste sind
 *
 * Die Akzentfarben sind fuer Beschriftungen auf hellem Grund gemacht. Als leuchtende Figur auf
 * Schwarz sind sie zu dunkel: FOCUS-Violett #6750A4 hat ein Drittel der Helligkeit des heutigen
 * Weiss, und die unterste, beschattete Zeile der Figur verschwindet damit im Hintergrund.
 *
 * Deshalb ist jede Farbe **im Farbton unveraendert** (gemessen: hoechstens 0,5 Grad Abweichung)
 * und nur in der Helligkeit so weit angehoben, dass die unterste Zeile der Figur **genauso gut
 * lesbar ist wie heute bei der weissen** - Kontrast 2,54 bis 2,56 gegen den Hintergrund,
 * gegenueber 2,53 heute. Keine Kreatur ist also dunkler als das, was schon auf dem Bildschirm
 * steht, und keine ist heller als eine andere. Nur MOVE musste dabei etwas Saettigung abgeben
 * (0,88 auf 0,83), weil sein Orange sonst ueber die maximale Helligkeit hinausgelaufen waere.
 *
 * Der zweite Teil derselben Rechnung steht in [AvatarShading.TINTED_SHADOW]: Ein farbiger
 * Koerper vertraegt den tiefen Verlauf der weissen Figur nicht.
 *
 * ## Die Zahlen stehen hier und nicht als Rechnung
 *
 * Ausgeschrieben statt zur Laufzeit hergeleitet, damit man sie beim Lesen sieht und aendern
 * kann. Dass sie stimmen, haelt `AvatarPaletteTest` fest - und zwar an den EIGENSCHAFTEN
 * (gleiche Helligkeitsstufe, Lesbarkeit unten, Mindestabstand zwischen zwei Kreaturen), nicht an
 * den Zahlen selbst. Wer eine Farbe austauscht oder eine siebte Kreatur anhaengt, erfaehrt dort,
 * ob sie taugt.
 */
object AvatarPalette {

    /** Der ruhige Beobachter ohne Schwerpunkt - GENERAL, das gedeckte Blaugrau der Glocke. */
    private const val PUFFLING = 0xFF7397A7.toInt()

    /** Die freundliche Traeumerin - MINDFULNESS, gruen. */
    private const val STARLET = 0xFF45A549.toInt()

    /** Der kleine Motivator - MOVE, orange. */
    private const val WYRMLING = 0xFFFE5D2A.toInt()

    /** Der ruhige Beschuetzer - DRINK, wasserblau. */
    private const val FENNEC = 0xFF2195FA.toInt()

    /** Der Entschleuniger - REST, tuerkis. */
    private const val GLOOP = 0xFF00A594.toInt()

    /** Der weise Beobachter - FOCUS, violett. */
    private const val HOOTLET = 0xFF9E7BFB.toInt()

    /**
     * Grundton der Kreatur als ARGB.
     *
     * Bewusst `Int` und nicht `androidx.compose.ui.graphics.Color`: Dieselbe Zahl wird von der
     * Compose-Ansicht ([AvatarSpriteView]) UND vom Bitmap-Renderer des Filmexports
     * ([PlayClipRenderer]) gebraucht - wie schon bei [MatrixColors]. Nebenbei laesst sich die
     * Farbwahl damit ohne Android pruefen.
     */
    fun tintFor(species: AvatarSpecies): Int = when (species) {
        AvatarSpecies.PUFFLING -> PUFFLING
        AvatarSpecies.STARLET -> STARLET
        AvatarSpecies.WYRMLING -> WYRMLING
        AvatarSpecies.FENNEC -> FENNEC
        AvatarSpecies.GLOOP -> GLOOP
        AvatarSpecies.HOOTLET -> HOOTLET
    }

    /** Alle Grundtoene - fuer die Pruefung der Abstaende untereinander. */
    val all: List<Int> = AvatarSpecies.entries.map(::tintFor)
}
