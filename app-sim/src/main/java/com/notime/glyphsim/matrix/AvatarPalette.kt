package com.notime.glyphsim.matrix

/**
 * **Die Akzentfarbe einer Kreatur** - der Ton, in dem ihr Gesicht leuchtet (siehe
 * [AvatarAccent]).
 *
 * ## Akzent, nicht Anstrich
 *
 * In NT-076 war die GANZE Figur in diesem Ton eingefaerbt. Das war der falsche Ort: Eine
 * einfarbige Flaeche in Kreaturform sagt zwar, welches Wesen es ist, aber nichts darueber, was
 * daran ein Gesicht ist - und sie sieht neben der weissen Kulisse aus wie ein Aufkleber. Der
 * Koerper ist deshalb wieder weiss wie die Welt, und die Farbe sitzt nur dort, wo ein Auge
 * ohnehin zuerst hinsieht.
 *
 * ## Woher die Farben kommen
 *
 * Nicht erfunden: Jede Kreatur traegt die Akzentfarbe ihres Schwerpunkts
 * ([AvatarSpecies.signatureTopic], Farben aus `ui/AnimationVisuals.kt`). Der weise Beobachter ist
 * violett, weil FOCUS violett ist; der kleine Motivator orange, weil MOVE orange ist. Damit gibt
 * es nur eine Quelle fuer "welche Farbe gehoert zu welchem Thema", und eine Erinnerung in der
 * Liste hat denselben Ton wie das Wesen, das sich darum kuemmert.
 *
 * ## Warum es GENAU die Werte von dort sind
 *
 * NT-076 hat sie noch aufgehellt, weil eine grosse Flaeche in FOCUS-Violett auf Schwarz zu
 * dunkel gewesen waere. Fuer einen Akzent auf WEISSEM Koerper dreht sich das um: Gefragt ist
 * jetzt Kontrast gegen Weiss, und den haben die kraeftigen Originale deutlich besser als die
 * aufgehellten Fassungen (gemessen 2,9 bis 5,7 gegenueber durchgehend 2,7).
 *
 * Nebenbei ist es damit buchstaeblich derselbe Ton wie in den farbigen Kreisen der
 * Erinnerungsliste - dieselbe Farbe an zwei Stellen und nicht zwei Farben, die sich aehneln.
 */
object AvatarPalette {

    /** Der ruhige Beobachter ohne Schwerpunkt - GENERAL, das gedeckte Blaugrau der Glocke. */
    private const val PUFFLING = 0xFF546E7A.toInt()

    /** Die freundliche Traeumerin - MINDFULNESS, gruen. */
    private const val STARLET = 0xFF43A047.toInt()

    /** Der kleine Motivator - MOVE, orange. */
    private const val WYRMLING = 0xFFF4511E.toInt()

    /** Der ruhige Beschuetzer - DRINK, wasserblau. */
    private const val FENNEC = 0xFF1E88E5.toInt()

    /** Der Entschleuniger - REST, tuerkis. */
    private const val GLOOP = 0xFF00897B.toInt()

    /** Der weise Beobachter - FOCUS, violett. */
    private const val HOOTLET = 0xFF6750A4.toInt()

    /**
     * Akzentfarbe der Kreatur als ARGB.
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

    /** Alle Akzentfarben - fuer die Pruefung der Abstaende untereinander. */
    val all: List<Int> = AvatarSpecies.entries.map(::tintFor)
}
