package com.notime.glyphsim.matrix

/**
 * Beschreibt die Grundform einer [AvatarSpecies] auf dem 16x16-[AvatarGeometry]-Raster:
 * [silhouette] ist die gefuellte Koerperform, [accessory] statischer Kopfschmuck
 * (Ohren/Horn/Fluegel/Federn - wird nie mitverschoben oder ausgestanzt). Augen/Mund
 * sind wie bei [AvatarAnimations] "ausgestanzte" Luecken in der Silhouette, deshalb pro
 * Spezies eigene Positionen ([eyesOpen]/[eyesClosed]/[mouthNeutral]/[mouthOpen]).
 * [feet]/[tail] sind Funktionen statt fester Listen, weil beide je nach Animationsphase
 * variieren (Fuss-Spreizung beim Huepfen, Schwanz-Wedel-Ausschlag).
 */
data class AvatarBody(
    val silhouette: List<Pair<Int, Int>>,
    val accessory: List<Pair<Int, Int>>,
    val eyesOpen: Set<Pair<Int, Int>>,
    val eyesClosed: Set<Pair<Int, Int>>,
    /**
     * Halb geschlossenes Lid als Zwischenstufe des Blinzelns.
     *
     * Ein Blinzler nur aus offen -> zu -> offen liest sich wie ein hartes Aufblitzen; mit einer
     * Zwischenstufe (nur noch die untere Augenzeile offen) senkt sich das Lid sichtbar von oben.
     *
     * Alle Augen sind dafuer zwei Zeilen hoch. Anfangs hatten vier der sechs Spezies nur ein
     * einzelnes Pixel je Auge - dort war eine Halbstufe geometrisch unmoeglich, das Auge konnte
     * nur schlagartig verschwinden. Die zweite Zeile gibt ihnen nicht nur den weichen Blinzler,
     * sondern auch mehr Ausdruck: bei einem Pixel liegt die gesamte Mimik in An/Aus.
     *
     * Der Standardwert [eyesOpen] (= keine sichtbare Halbstufe) bleibt als Rueckfallebene fuer
     * kuenftige Formen mit einzeiligen Augen bestehen, wird derzeit aber von jeder Spezies
     * ueberschrieben.
     */
    val eyesHalf: Set<Pair<Int, Int>> = eyesOpen,
    val mouthNeutral: Set<Pair<Int, Int>>,
    val mouthOpen: Set<Pair<Int, Int>>,
    val feet: (spread: Int) -> List<Pair<Int, Int>>,
    val tail: (wag: Int) -> List<Pair<Int, Int>>,
    /**
     * Spezies-eigener Bewegungs-Akzent, [phase] ist -1/0/1 (0 = Ruhelage).
     *
     * Grund: [feet] und [tail] gibt es nicht bei jeder Spezies - STARLET/GLOOP/HOOTLET haben
     * keinen Schwanz, GLOOP zusaetzlich keine Fuesse. Bewegungen, die sich allein darauf
     * stuetzen, blieben dort wirkungslos (gemessen: deutlich weniger Pixelaenderung pro
     * Uebergang als bei PUFFLING, die Reaktionen wirkten dadurch systematisch matter). Der
     * Akzent gibt JEDER Spezies etwas Eigenes, das sich bewegt - Ohren zucken, Fluegel
     * schlagen, Schleim quetscht sich - und macht die Auswahl der Grundform damit ueberhaupt
     * erst spuerbar statt nur eine andere Silhouette zu sein.
     *
     * Wird wie [feet]/[tail] mit dem Koerper mitverschoben.
     */
    val accent: (phase: Int) -> List<Pair<Int, Int>> = { emptyList() },
    /**
     * **Eine Silhouette, die sich mit der Bewegung AENDERT** statt nur ergaenzt zu werden.
     *
     * [accent] kann ausschliesslich etwas hinzufuegen - ein Ohr, einen Fluegel, eine Ausbeulung.
     * Fuer fuenf der sechs Kreaturen ist das genau richtig: Ein Koerper aus Fell und Knochen
     * behaelt seine Form, es bewegen sich nur Teile davon.
     *
     * Ein Schleim nicht. Bei ihm nimmt Quetschen oben etwas WEG und setzt es seitlich wieder an,
     * und das laesst sich mit Zusatzzellen grundsaetzlich nicht ausdruecken - genau deshalb hatte
     * GLOOP bisher vier Zellen Ausbeulung und bewegte sich im Uebrigen wie ein Stein mit Auge.
     * Deshalb darf eine Grundform hier ihre gesamte Silhouette aus der Bewegungsphase rechnen
     * (siehe [GloopShape]).
     *
     * Voreinstellung ist die feste Silhouette - fuer alle anderen aendert sich nichts.
     */
    val silhouetteFor: (phase: Int) -> List<Pair<Int, Int>> = { silhouette },
    /**
     * Um wieviele Zeilen Augen und Mund bei dieser Bewegungsphase mitwandern.
     *
     * Nur fuer verformbare Koerper von null verschieden (siehe [silhouetteFor]): Aendert sich die
     * Form, muss das Gesicht mit - sonst sitzt es am falschen Ende. Als VERSCHIEBUNG und nicht als
     * Position, damit Blinzeln und Mundoeffnen unveraendert mit denselben Punktmengen arbeiten.
     */
    val holeShiftFor: (phase: Int) -> Int = { 0 }
)

/**
 * Die unterste belegte Zeile dieser Grundform in der Ruhelage - also die Zeile, mit der die Figur
 * aufsetzt, wenn sie auf dem Boden der Kulisse steht (siehe [PlayScene] und ui/DockScreen.kt).
 *
 * Wird gebraucht, weil die Spezies unterschiedlich tief reichen: WYRMLING endet in Zeile 13,
 * PUFFLING/FENNEC/HOOTLET in 14, STARLET in 15, und GLOOP hat ueberhaupt keine Fuesse und endet
 * mit seiner Silhouette. Ein fuer alle gleicher Versatz - der erste Entwurf rechnete pauschal mit
 * Zeile 14 - liesse deshalb je nach gewaehltem Avatar die Fuesse im Boden verschwinden oder die
 * Figur einen Pixel darueber schweben. Beides faellt auf einer durchgezogenen Bodenlinie sofort
 * auf.
 *
 * Berechnet statt je Spezies eingetragen: so bleibt es auch dann richtig, wenn jemand eine
 * Silhouette anpasst oder eine siebte Grundform hinzukommt.
 */
fun AvatarBody.groundRow(): Int = maxOf(
    silhouette.maxOfOrNull { it.second } ?: 0,
    feet(0).maxOfOrNull { it.second } ?: 0
) + AvatarGeometry.HEADROOM   // die Posen liegen um die Kopffreiheit nach unten versetzt

object AvatarBodies {
    private val noFeet: (Int) -> List<Pair<Int, Int>> = { emptyList() }
    private val noTail: (Int) -> List<Pair<Int, Int>> = { emptyList() }

    fun forSpecies(species: AvatarSpecies): AvatarBody = when (species) {
        AvatarSpecies.PUFFLING -> puffling
        AvatarSpecies.STARLET -> starlet
        AvatarSpecies.WYRMLING -> wyrmling
        AvatarSpecies.FENNEC -> fennec
        AvatarSpecies.GLOOP -> gloop
        AvatarSpecies.HOOTLET -> hootlet
    }

    // ---- PUFFLING: rundlicher Koerper, grosse Ohren, Schwanz ----
    private val puffling = AvatarBody(
        silhouette = listOf(
            7 to 6, 8 to 6,
            6 to 7, 7 to 7, 8 to 7, 9 to 7,
            5 to 8, 6 to 8, 7 to 8, 8 to 8, 9 to 8, 10 to 8,
            4 to 9, 5 to 9, 6 to 9, 7 to 9, 8 to 9, 9 to 9, 10 to 9, 11 to 9,
            4 to 10, 5 to 10, 6 to 10, 7 to 10, 8 to 10, 9 to 10, 10 to 10, 11 to 10,
            5 to 11, 6 to 11, 7 to 11, 8 to 11, 9 to 11, 10 to 11,
            6 to 12, 7 to 12, 8 to 12, 9 to 12,
            7 to 13, 8 to 13
        ),
        accessory = listOf(6 to 4, 5 to 5, 9 to 4, 10 to 5),
        eyesOpen = setOf(6 to 9, 6 to 10, 9 to 9, 9 to 10),
        eyesClosed = emptySet(),
        eyesHalf = setOf(6 to 10, 9 to 10),
        mouthNeutral = setOf(7 to 11, 8 to 11),
        mouthOpen = setOf(6 to 11, 7 to 11, 8 to 11, 9 to 11),
        feet = { spread -> listOf((6 - spread) to 14, (9 + spread) to 14) },
        tail = { wag -> listOf(11 to (10 + wag), 12 to (9 + wag)) },
        // Ohrenspitzen zucken: die Ohren sitzen bei (5,5)/(10,5), die Spitze wandert je nach
        // Phase eine Zeile hoch bzw. nach aussen.
        accent = { phase ->
            when {
                phase > 0 -> listOf(5 to 4, 10 to 4)
                phase < 0 -> listOf(4 to 5, 11 to 5)
                else -> emptyList()
            }
        }
    )

    // ---- STARLET: ueberdimensionaler Kopf, riesige Augen, Stern-Zopf, kaum Koerper ----
    private val starlet = AvatarBody(
        silhouette = listOf(
            7 to 5, 8 to 5,
            6 to 6, 7 to 6, 8 to 6, 9 to 6,
            5 to 7, 6 to 7, 7 to 7, 8 to 7, 9 to 7, 10 to 7,
            4 to 8, 5 to 8, 6 to 8, 7 to 8, 8 to 8, 9 to 8, 10 to 8, 11 to 8,
            3 to 9, 4 to 9, 5 to 9, 6 to 9, 7 to 9, 8 to 9, 9 to 9, 10 to 9, 11 to 9, 12 to 9,
            3 to 10, 4 to 10, 5 to 10, 6 to 10, 7 to 10, 8 to 10, 9 to 10, 10 to 10, 11 to 10, 12 to 10,
            4 to 11, 5 to 11, 6 to 11, 7 to 11, 8 to 11, 9 to 11, 10 to 11, 11 to 11,
            5 to 12, 6 to 12, 7 to 12, 8 to 12, 9 to 12, 10 to 12,
            6 to 13, 7 to 13, 8 to 13, 9 to 13,
            7 to 14, 8 to 14
        ),
        accessory = listOf(7 to 2, 8 to 2, 6 to 1, 9 to 1),
        eyesOpen = setOf(5 to 9, 6 to 9, 5 to 10, 6 to 10, 10 to 9, 11 to 9, 10 to 10, 11 to 10),
        eyesClosed = emptySet(),
        // Nur noch die untere Augenzeile offen - das Lid senkt sich sichtbar, statt dass die
        // riesigen Augen auf einen Schlag verschwinden.
        eyesHalf = setOf(5 to 10, 6 to 10, 10 to 10, 11 to 10),
        mouthNeutral = setOf(8 to 12),
        mouthOpen = setOf(7 to 12, 8 to 12, 9 to 12),
        feet = { spread -> listOf((7 - spread) to 15, (8 + spread) to 15) },
        tail = noTail,
        // Kein Schwanz - stattdessen schwingt der Stern-Zopf ueber dem Kopf (Accessory bei
        // (7,2)/(8,2)/(6,1)/(9,1)) zur Seite aus.
        accent = { phase ->
            when {
                phase > 0 -> listOf(10 to 1, 10 to 2)
                phase < 0 -> listOf(5 to 1, 5 to 2)
                else -> emptyList()
            }
        }
    )

    // ---- WYRMLING: kleiner Drache, Horn, Fluegel, Schwanzspitze ----
    private val wyrmling = AvatarBody(
        silhouette = listOf(
            7 to 5, 8 to 5,
            6 to 6, 7 to 6, 8 to 6, 9 to 6,
            5 to 7, 6 to 7, 7 to 7, 8 to 7, 9 to 7, 10 to 7,
            4 to 8, 5 to 8, 6 to 8, 7 to 8, 8 to 8, 9 to 8, 10 to 8, 11 to 8,
            4 to 9, 5 to 9, 6 to 9, 7 to 9, 8 to 9, 9 to 9, 10 to 9, 11 to 9,
            5 to 10, 6 to 10, 7 to 10, 8 to 10, 9 to 10, 10 to 10,
            6 to 11, 7 to 11, 8 to 11, 9 to 11,
            7 to 12, 8 to 12
        ),
        accessory = listOf(7 to 3, 8 to 3, 3 to 8, 2 to 9, 12 to 8, 13 to 9),
        eyesOpen = setOf(6 to 8, 6 to 9, 9 to 8, 9 to 9),
        eyesClosed = emptySet(),
        eyesHalf = setOf(6 to 9, 9 to 9),
        mouthNeutral = setOf(7 to 10, 8 to 10),
        mouthOpen = setOf(6 to 10, 7 to 10, 8 to 10, 9 to 10),
        feet = { spread -> listOf((6 - spread) to 13, (9 + spread) to 13) },
        tail = { wag -> listOf(11 to (10 + wag), 12 to (9 + wag), 13 to (8 + wag)) },
        // Fluegelschlag: die Fluegel sitzen bei (3,8)/(2,9) bzw. (12,8)/(13,9) - je nach Phase
        // heben oder senken sich die Spitzen.
        accent = { phase ->
            when {
                phase > 0 -> listOf(2 to 8, 13 to 8, 3 to 7, 12 to 7)
                phase < 0 -> listOf(2 to 10, 13 to 10, 3 to 10, 12 to 10)
                else -> emptyList()
            }
        }
    )

    // ---- FENNEC: schlankere, realistischere Silhouette, spitze Ohren, Schnauze ----
    private val fennec = AvatarBody(
        silhouette = listOf(
            7 to 6, 8 to 6,
            6 to 7, 7 to 7, 8 to 7, 9 to 7,
            5 to 8, 6 to 8, 7 to 8, 8 to 8, 9 to 8, 10 to 8,
            5 to 9, 6 to 9, 7 to 9, 8 to 9, 9 to 9, 10 to 9,
            5 to 10, 6 to 10, 7 to 10, 8 to 10, 9 to 10, 10 to 10,
            6 to 11, 7 to 11, 8 to 11, 9 to 11,
            6 to 12, 7 to 12, 8 to 12, 9 to 12,
            7 to 13, 8 to 13
        ),
        accessory = listOf(5 to 1, 5 to 2, 6 to 3, 10 to 1, 10 to 2, 9 to 3),
        eyesOpen = setOf(6 to 8, 6 to 9, 9 to 8, 9 to 9),
        eyesClosed = emptySet(),
        eyesHalf = setOf(6 to 9, 9 to 9),
        mouthNeutral = emptySet(),
        mouthOpen = setOf(7 to 13, 8 to 13),
        feet = { spread -> listOf((6 - spread) to 14, (9 + spread) to 14) },
        tail = { wag -> listOf(11 to (10 + wag), 12 to (10 + wag), 12 to (9 + wag), 13 to (9 + wag)) },
        // Spitze Ohren (Accessory bei (5,1)/(5,2) bzw. (10,1)/(10,2)) stellen sich auf oder
        // legen sich an.
        accent = { phase ->
            when {
                phase > 0 -> listOf(5 to 0, 10 to 0)
                phase < 0 -> listOf(4 to 2, 11 to 2)
                else -> emptyList()
            }
        }
    )

    // ---- GLOOP: Fantasy-Schleim, ein grosses zentrales Auge, keine Beine/Ohren ----
    //
    // Als einziger Koerper GERECHNET statt gezeichnet - die Begruendung steht bei [GloopShape].
    private val gloop = AvatarBody(
        // Die Ruhelage ist dieselbe Rechnung wie jede andere Stufe. Frueher stand hier eine
        // handgeschriebene Punktliste; sobald die Form gerechnet wird, waeren das zwei Quellen
        // fuer dieselbe Gestalt, und die zweite waere die, die niemand mehr nachzieht.
        silhouette = GloopShape.body(0),
        accessory = emptyList(),
        eyesOpen = setOf(7 to 9, 8 to 9, 7 to 10, 8 to 10),
        eyesClosed = emptySet(),
        // Das einzelne grosse Auge schliesst sich von oben - untere Zeile bleibt zuletzt offen.
        eyesHalf = setOf(7 to 10, 8 to 10),
        mouthNeutral = setOf(7 to 12),
        mouthOpen = setOf(6 to 12, 7 to 12, 8 to 12, 9 to 12),
        feet = noFeet,
        tail = noTail,
        /**
         * **Der Fortsatz** - was bei ihm einem Arm am naechsten kommt.
         *
         * Er hat keine Gliedmassen, und statt ihm welche anzuzeichnen quillt bei starker
         * Streckung ein Stueck Koerper zur Seite heraus (siehe [GloopShape.reach]). Bei kleinen
         * Phasen bleibt es beim blossen Quetschen - sonst waere jede Regung ein Ausgreifen.
         *
         * Beim staerksten Zusammendruecken loest sich stattdessen ein Tropfen vom Scheitel: Was
         * seitlich herausgedrueckt wird, muss irgendwo hin.
         */
        accent = { phase ->
            when {
                phase <= -3 -> GloopShape.reach(extent = 3, toRight = true, squash = phase)
                phase >= 3 -> GloopShape.bead(step = 1, squash = phase)
                else -> emptyList()
            }
        },
        /**
         * **Gloop ist der einzige, dessen ganze Form aus der Bewegung faellt.**
         *
         * Positive Phasen druecken ihn flach und breit, negative ziehen ihn hoch und schmal, und
         * die Flaeche bleibt dabei gleich - daran erkennt man Gummi (siehe [GloopShape]). Vorher
         * war er eine feste Silhouette mit vier Zellen Ausbeulung: die einzige Kreatur ohne
         * Fuesse, ohne Schwanz und ohne Ohren, also die mit dem WENIGSTEN Bewegungsspielraum,
         * obwohl sie als Schleim den groessten haben muesste.
         */
        silhouetteFor = { phase -> GloopShape.body(phase) },
        holeShiftFor = { phase -> GloopShape.holeShift(phase) }
    )

    // ---- HOOTLET: Anime-Eule, Federohren, kleine Fluegel, Schnabel ----
    private val hootlet = AvatarBody(
        silhouette = listOf(
            7 to 6, 8 to 6,
            6 to 7, 7 to 7, 8 to 7, 9 to 7,
            5 to 8, 6 to 8, 7 to 8, 8 to 8, 9 to 8, 10 to 8,
            4 to 9, 5 to 9, 6 to 9, 7 to 9, 8 to 9, 9 to 9, 10 to 9, 11 to 9,
            4 to 10, 5 to 10, 6 to 10, 7 to 10, 8 to 10, 9 to 10, 10 to 10, 11 to 10,
            5 to 11, 6 to 11, 7 to 11, 8 to 11, 9 to 11, 10 to 11,
            6 to 12, 7 to 12, 8 to 12, 9 to 12,
            7 to 13, 8 to 13
        ),
        accessory = listOf(6 to 3, 6 to 4, 9 to 3, 9 to 4, 3 to 10, 3 to 11, 12 to 10, 12 to 11),
        eyesOpen = setOf(6 to 9, 6 to 10, 9 to 9, 9 to 10),
        eyesHalf = setOf(6 to 10, 9 to 10),
        eyesClosed = emptySet(),
        mouthNeutral = setOf(7 to 11),
        mouthOpen = setOf(7 to 11, 8 to 11, 7 to 12),
        feet = { spread -> listOf((6 - spread) to 14, (9 + spread) to 14) },
        tail = noTail,
        // Kein Schwanz - dafuer schlagen die Fluegel-Stummel (Accessory bei (3,10)/(3,11) bzw.
        // (12,10)/(12,11)) und die Federohren zucken.
        accent = { phase ->
            when {
                phase > 0 -> listOf(2 to 9, 13 to 9, 6 to 2, 9 to 2)
                phase < 0 -> listOf(3 to 12, 12 to 12)
                else -> emptyList()
            }
        }
    )
}
