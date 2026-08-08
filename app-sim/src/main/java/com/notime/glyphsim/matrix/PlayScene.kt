package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Eine einzelne Zelle der Kulisse: Rasterposition plus eigene Helligkeit.
 *
 * Bewusst eine duenn besetzte Liste statt eines vollen [IntArray] wie beim Avatar
 * ([AvatarGeometry]): Die Kulisse spannt sich ueber den ganzen Bildschirm (bei ueblichen
 * Geraeten gut 50x100 Zellen), waere als volles Raster also zu ueber 95 % leer. Gezeichnet
 * wird ohnehin nur, was gesetzt ist.
 */
data class SceneCell(
    val x: Int,
    val y: Int,
    val brightness: Int,
    /**
     * Ob diese Zelle eine LICHTQUELLE ist (Mond, Lampe, Mattscheibe) statt beleuchteter Materie.
     *
     * Der Unterschied zaehlt nur nachts: Dann wird der Raum abgedunkelt ([PlayScene.build]), das
     * Licht darin aber NICHT. Wuerde man beides gleich behandeln, saehe eine Nachtszene aus wie
     * eine Tagszene mit heruntergedrehter Helligkeit - eine Lampe, die im dunklen Zimmer genauso
     * viel schwaecher wird wie die Wand dahinter, hoert auf, eine Lampe zu sein.
     */
    val isLight: Boolean = false
)

/**
 * Die **Lebenswelt** des Avatars im Play-Modus - die Kulisse, in der er sich aufhaelt, statt
 * eines Sprites, das im Schwarzen schwebt (siehe ui/DockScreen.kt und ui/PlaySceneView.kt).
 *
 * **Warum Seitenansicht.** Bei einer 16x16-Figur ist eine isometrische oder perspektivische
 * Darstellung nicht lesbar - schraege Linien werden auf diesem Raster zu Treppen, und ein Bett
 * in Schraegsicht ist von einem Tisch nicht mehr zu unterscheiden. Die Seitenansicht (Boden als
 * waagerechte Linie, alles steht darauf) ist dieselbe Loesung, die Game & Watch und Tamagotchi
 * aus demselben Grund gewaehlt haben: Silhouetten bleiben auch bei sechs Zellen Hoehe eindeutig.
 *
 * **Warum prozeduraler Raum statt gezeichneter Hintergrundbilder.** Boden und Bodentextur
 * spannen sich ueber die volle Breite, die Requisiten werden als kleine, handgezeichnete
 * Silhouetten ([Prop]) an einem Bruchteil der Breite verankert. Ein fest gezeichneter
 * Hintergrund muesste dagegen fuer jedes Seitenverhaeltnis neu gebaut werden - so passt sich
 * die Szene jedem Geraet an, waehrend die eigentliche Pixel-Arbeit klein und pflegbar bleibt.
 *
 * **Warum drei Helligkeitsstufen.** [STRUCTURE] (Boden/Wand), [FURNITURE] (Moebel) und [GLOW]
 * (Lichtquellen: Lampe, Monitor, Mond) staffeln die Tiefe, der Avatar bleibt als einziges auf
 * voller Helligkeit. Dass eine Lampe HELLER leuchtet als das Moebelstueck daneben, ist der
 * eigentliche Kniff - ohne diesen Unterschied waere sie nur eine weitere Silhouette und nicht
 * als Licht zu lesen. Nebenbei ist die gedaempfte Kulisse auch die schonendere Variante fuer
 * ein OLED, das im Dock-Modus dauerhaft anbleibt (siehe Burn-in-Drift in DockScreen).
 *
 * **Der Ort ergibt sich aus der Taetigkeit** ([forTopic]) - es gibt bewusst KEINEN eigenen
 * "wo ist der Avatar gerade"-Zustand. Was er tut, entscheidet die Tageszeit-Logik in
 * [PlayAmbientActivity]; wo er das tut, folgt daraus. Schlafen heisst Schlafzimmer, Arbeiten
 * heisst Schreibtisch. Dadurch kann der Tagesablauf nie mit der Kulisse auseinanderlaufen.
 */
object PlayScene {

    // Die Staffelung wurde nachtraeglich deutlich angehoben (Boden 430→560, Moebel 900→1300,
    // Licht 1850→2500). Die urspruenglichen Werte waren rechnerisch sauber gestaffelt, ergaben auf
    // schwarzem Grund aber ein durchgehend dunkelgraues Bild: Bei 22 % Helligkeit liegt ein
    // Moebelstueck farblich naeher am Hintergrund als an der Figur, und der ganze Raum sank
    // optisch weg. Der Abstand nach oben bleibt gewahrt - der Avatar leuchtet weiterhin mit voller
    // Helligkeit und bleibt damit die klar hellste Erscheinung im Bild.

    /** Boden und Bodentextur. */
    const val STRUCTURE = 560

    /** Was an der WAND haengt (Fenster, Regal) - liegt hinter allem, was auf dem Boden steht,
     *  und ist deshalb eine Stufe schwaecher als [FURNITURE]. */
    const val BACKDROP = 850

    /** Moebel und Requisiten, die auf dem Boden stehen. */
    const val FURNITURE = 1300

    /** Lichtquellen (Lampe, Monitor, Mond) - bewusst heller als [FURNITURE], siehe Klassendoku. */
    const val GLOW = 2500

    /**
     * **Glanzlicht** - fast so hell wie der Avatar selbst.
     *
     * Bewusst nur fuer EINZELNE Zellen: den Kern einer Lampe, den Mond, den hellsten Stern. Eine
     * Flaeche auf dieser Stufe wuerde mit der Figur um die Aufmerksamkeit streiten; ein einzelner
     * Punkt dagegen setzt einen Akzent und gibt dem Bild Tiefe nach oben. Es ist derselbe
     * Gedanke, mit dem ein Maler ein Glanzlicht setzt: nicht flaechig, sondern an der einen
     * Stelle, an der das Licht wirklich sitzt.
     */
    const val HIGHLIGHT = 3700

    /** Wie stark die beleuchtete Oberkante einer Requisite hervortritt, siehe [build]. */
    private const val RIM_BOOST = 1.75f

    /** Deckel fuer die Oberkante: Eine Tischplatte darf nicht heller strahlen als eine Lampe -
     *  sonst kippt die Tiefenstaffelung, von der das ganze Bild lebt. */
    private const val RIM_MAX = GLOW - 300

    /** Die Orte, an denen sich der Avatar aufhalten kann. */
    enum class Place { BEDROOM, BATH, DESK, WORK, KITCHEN, NOOK, LIVING, PARK, SHOP }

    /** Draussen gibt es keine Wand und keinen Zimmerboden - siehe [build]. */
    private val Place.isIndoors: Boolean get() = this != Place.PARK

    /**
     * Ob an diesem Ort ueberhaupt jemand vorbeikommen kann (siehe runVisit in DockScreen).
     *
     * Im Park und im Laden trifft man Leute, im Wohnzimmer bekommt man Besuch - im SCHLAFZIMMER
     * nicht, und am Schreibtisch oder in der Leseecke ebensowenig. Ohne diese Unterscheidung lief
     * mitten in der Nacht eine fremde Kreatur durchs Schlafzimmer, was den Eindruck einer
     * bewohnten Welt eher zerstoert als verstaerkt.
     */
    fun allowsVisitors(place: Place): Boolean = when (place) {
        // Am ARBEITSPLATZ trifft man selbstverstaendlich Leute - das ist neben Park und Laden der
        // dritte Ort, an dem eine Begegnung nichts Ungewoehnliches ist.
        Place.PARK, Place.SHOP, Place.LIVING, Place.WORK -> true
        Place.BEDROOM, Place.BATH, Place.DESK, Place.KITCHEN, Place.NOOK -> false
    }

    /**
     * Ein PLATZ innerhalb eines Ortes, den die Figur aufsuchen und benutzen kann - der Begriff,
     * ueber den ein Tagesablauf ([PlayRoutine]) mit der Einrichtung spricht, ohne etwas ueber
     * Pixel, Verankerungen oder Requisiten-Groessen wissen zu muessen.
     *
     * **Warum diese Zwischenschicht ueberhaupt.** Vorher kannte der Ablauf nur "gehe zu Bruchteil
     * 0,4 der Bildschirmbreite" - eine Zahl, die nichts bedeutet und die bei jeder Aenderung an
     * der Einrichtung nachgezogen werden muesste. Mit benannten Plaetzen sagt der Ablauf "geh ans
     * Bett", und wo das Bett steht, weiss allein diese Datei. Genau daran haengt, ob sich die
     * Welt spaeter erweitern laesst, ohne dass die Ablaeufe brechen.
     */
    enum class Station { BED, SEAT, DESK, TABLE, FRIDGE, BOOKSHELF, LAMP, TV, BENCH, DOOR, RACK, CHECKOUT, TUB, BASIN, WORKPLACE }

    /** Aufloesung eines [Station] in Szenen-Zellen: waagerechte Mitte und Aufsetzzeile. */
    data class SceneSpot(val centerX: Int, val groundY: Int)

    /**
     * Wo die Figur steht, sitzt oder liegt, wenn sie [station] an diesem [place] benutzt -
     * `null`, wenn es den Platz hier nicht gibt (im Schlafzimmer gibt es keinen Schreibtisch).
     */
    fun stationSpot(
        place: Place,
        station: Station,
        widthCells: Int,
        floorY: Int,
        species: AvatarSpecies = AvatarSpecies.PUFFLING
    ): SceneSpot? {
        val placement = placementsFor(place, species).firstOrNull { it.station == station } ?: return null
        val spot = placement.prop.useSpot ?: return null
        return SceneSpot(
            centerX = originX(placement, widthCells) + spot.first,
            groundY = originY(placement, floorY) + spot.second
        )
    }

    /**
     * Wie hoch der Boden liegt - und damit, wie viel Himmel ueber der Szene steht.
     *
     * **Der Streifen ist nicht mehr ueberall gleich hoch.** Drinnen sitzt der Boden tief, das
     * Zimmer ist ein schmales Band am unteren Rand und laesst der Uhr den Platz darueber. Nachts
     * im Park rutscht er weit nach unten: Die Figur wird klein, und der Himmel nimmt fast den
     * ganzen Bildschirm ein.
     *
     * Das ist derselbe Kniff, mit dem Filme eine Weite herstellen - nicht mehr zeichnen, sondern
     * den Horizont senken. Es kostet keine einzige zusaetzliche Requisite und macht aus einer
     * naechtlichen Parkszene etwas, das man ansieht, statt etwas, an dem man vorbeischaut.
     */
    fun floorFraction(place: Place, dayPhase: PlayAmbientActivity.DayPhase): Float = when {
        place == Place.PARK && dayPhase == PlayAmbientActivity.DayPhase.NIGHT -> 0.93f
        place == Place.PARK && dayPhase == PlayAmbientActivity.DayPhase.EVENING -> 0.88f
        else -> 0.80f
    }

    /**
     * Die Zellen GENAU EINER Requisite - fuer die Kompositions-Pruefungen (siehe ScenePreview und
     * SceneCompositionTest im Testverzeichnis).
     *
     * Ohne diesen Zugang liesse sich nicht pruefen, ob die Figur das Moebelstueck verdeckt, das
     * sie gerade benutzt - und genau dieser Fehler ist beim Aufbau der Welt wieder und wieder
     * passiert (Figur im Teleskop, Figur vor dem ganzen Kuehlschrank, Figur im Lichtkegel des
     * Leuchtturms). Er faellt nirgends auf ausser im Bild.
     */
    fun propCellsAt(
        place: Place,
        station: Station,
        widthCells: Int,
        floorY: Int,
        species: AvatarSpecies = AvatarSpecies.PUFFLING
    ): List<Pair<Int, Int>> {
        val placement = placementsFor(place, species).firstOrNull { it.station == station }
            ?: return emptyList()
        val ox = originX(placement, widthCells)
        val oy = originY(placement, floorY)
        return placement.prop.art.map { (x, y) -> (ox + x) to (oy + y) }
    }

    /** Ob diese Requisite zum Hineinlegen/Hineinsetzen gedacht ist - dort ist eine Ueberdeckung
     *  gewollt und wird von [buildFront] aufgeloest. */
    fun isOccupiable(
        place: Place,
        station: Station,
        species: AvatarSpecies = AvatarSpecies.PUFFLING
    ): Boolean = placementsFor(place, species)
        .firstOrNull { it.station == station }?.prop?.frontArt?.isNotEmpty() == true

    /**
     * Zeile der Zimmerdecke - `null` im Freien, wo es keine gibt.
     *
     * Oeffentlich fuer die Kompositions-Pruefung: Eine Figur, die durch die Decke ragt, ist ein
     * Fehler, den kein anderer Test bemerkt (die Kulisse ist ja korrekt, und die Figur steht auf
     * einer gueltigen Aufsetzstelle) - man sieht ihn ausschliesslich im Bild.
     */
    fun ceilingY(place: Place, floorY: Int): Int? =
        if (place.isIndoors) (floorY - CEILING_HEIGHT_CELLS).takeIf { it > 0 } else null

    /** Welche Plaetze es an diesem Ort ueberhaupt gibt. */
    fun stationsAt(place: Place, species: AvatarSpecies = AvatarSpecies.PUFFLING): List<Station> =
        placementsFor(place, species).mapNotNull { it.station }

    /**
     * Die Teile der benutzten Requisite, die VOR der Figur liegen - Bettdecke, Sitzvorderkante.
     *
     * **Das ist der Trick, der die teuerste Arbeit erspart.** "Im Bett liegen" und "im Sessel
     * sitzen" braeuchten sonst je eine eigene, von Hand gezeichnete Koerperhaltung - mal sechs
     * Spezies, also zwoelf zusaetzliche Silhouetten, die alle zueinander passen muessen. Wird der
     * untere Teil der Figur stattdessen von der Decke verdeckt, entsteht dieselbe Aussage aus
     * Ueberdeckung statt aus neuer Pixel-Art: Was man nicht sieht, muss man nicht zeichnen. Es ist
     * derselbe Gedanke, mit dem die Augen als ausgestanzte Luecken in der Silhouette gebaut sind
     * (siehe [AvatarAnimations]).
     *
     * Wird NUR fuer den gerade benutzten Platz gezeichnet: Eine Decke, die immer vorne liegt,
     * verdeckte die Figur auch, wenn sie bloss am Bett vorbeigeht.
     */
    fun buildFront(
        place: Place,
        station: Station?,
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase,
        fade: Float = 1f,
        species: AvatarSpecies = AvatarSpecies.PUFFLING
    ): List<SceneCell> {
        if (station == null || widthCells <= 0 || floorY <= 0 || fade <= 0f) return emptyList()
        val placement = placementsFor(place, species).firstOrNull { it.station == station } ?: return emptyList()
        if (placement.prop.frontArt.isEmpty()) return emptyList()
        val originX = originX(placement, widthCells)
        val originY = originY(placement, floorY)
        val factor = atmosphere(dayPhase) * fade
        return placement.prop.frontArt.mapNotNull { (px, py) ->
            val x = originX + px
            val brightness = (placement.brightness * factor).roundToInt()
            if (brightness <= 0 || x < 0 || x >= widthCells || originY + py < 0) null
            else SceneCell(x, originY + py, brightness)
        }
    }

    /**
     * Welcher Ort zu welcher Taetigkeit gehoert.
     *
     * MEDICINE kommt aus einer autonomen Regung nie (siehe [PlayAmbientActivity]), ueber eine
     * echte Erinnerung aber schon - deshalb hier trotzdem abgedeckt und der Kueche zugeordnet,
     * wo auch das Glas Wasser dazu steht.
     */
    fun forTopic(topic: AnimationType?): Place = when (topic) {
        AnimationType.SLEEP -> Place.BEDROOM
        // Arbeiten heisst zur ARBEIT gehen; konzentrieren kann man sich auch zu Hause.
        AnimationType.WORK -> Place.WORK
        AnimationType.FOCUS -> Place.DESK
        // Koerperpflege ist kein eigenes Thema in der Bibliothek der zwoelf - MEDICINE ist dem
        // am naechsten und fuehrt deshalb ins Bad.
        AnimationType.MEDICINE -> Place.BATH
        AnimationType.DRINK -> Place.KITCHEN
        AnimationType.MOVE -> Place.PARK
        // Ausruhen und Beisammensein gehoeren ins Wohnzimmer, Lesen und Stille in die Leseecke -
        // vorher landete beides in derselben Ecke, wodurch der halbe Tagesablauf am selben Ort
        // stattfand.
        AnimationType.REST, AnimationType.LOVE, AnimationType.GENERAL, null -> Place.LIVING
        AnimationType.BOOK, AnimationType.CREATIVITY, AnimationType.MINDFULNESS -> Place.NOOK
    }

    /**
     * Wo der Avatar an diesem Ort steht (Bruchteil der Szenenbreite) - immer NEBEN der
     * Hauptrequisite, nicht darauf: die Reaktions-Animationen bringen ihre eigenen Requisiten
     * mit (Glas, Tasse, Buch - siehe [AvatarAnimations]), zwei uebereinanderliegende Betten
     * saehen nach Fehler aus, nicht nach Absicht.
     */
    fun avatarAnchorX(place: Place): Float = when (place) {
        Place.BEDROOM -> 0.44f
        Place.DESK -> 0.42f
        Place.KITCHEN -> 0.08f
        Place.NOOK -> 0.27f
        Place.LIVING -> 0.30f
        Place.PARK -> 0.20f
        Place.SHOP -> 0.34f
        Place.BATH -> 0.42f
        Place.WORK -> 0.20f
    }

    /**
     * Baut die Kulisse eines Ortes.
     *
     * [phase] laeuft langsam hoch und treibt die Umgebungsanimation ([ambient]) - Dampf steigt,
     * der Monitor flackert, die Lampe pulst. Ohne das waere die Welt ein Standbild, und ein
     * Standbild hinter einer animierten Figur wirkt wie ein Fehler in der Darstellung.
     *
     * [fade] blendet die gesamte Kulisse ab - fuer den Wechsel von einem Ort zum naechsten
     * (siehe DockScreen): erst weg, dann der neue herein. Ein harter Schnitt mitten im Bild
     * saehe nach einem Zeichenfehler aus, ein Uebergang nach einem Ortswechsel.
     */
    fun build(
        place: Place,
        phase: Int,
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase,
        fade: Float = 1f,
        /**
         * Ob die Stehlampe brennt - **der erste Gegenstand, dessen Zustand die Figur selbst
         * aendert** (siehe [RoutineStep.Switch]).
         *
         * Das ist mehr als ein Detail: Bis hierher war die Welt Kulisse, die sich nur nach der
         * Uhrzeit richtete. Ein Schalter, den die Figur betaetigt und dessen Wirkung man sieht,
         * macht aus der Umgebung etwas, auf das sie einwirkt - und genau daran erkennt man
         * jemanden, der irgendwo WOHNT, statt davor zu stehen.
         */
        lampOn: Boolean = true,
        /** Ob der Fernseher laeuft - zweites schaltbares Geraet, siehe [lampOn]. */
        tvOn: Boolean = true,
        /**
         * Die Requisite, die die Figur GERADE benutzt - sie wird dann in ihrer geoeffneten Form
         * gezeichnet (siehe [Prop.usedArt]).
         *
         * Bis hierher blieben die Moebel bei jeder Handlung unveraendert: Die Figur langte am
         * Kuehlschrank hin, und der Kuehlschrank stand da wie zuvor. Der getragene Gegenstand
         * erzaehlte danach zwar den Zweck, aber die QUELLE blieb stumm - man sah nicht, WOHER er
         * kam. Eine Tuer, die aufgeht, schliesst genau diese Luecke.
         */
        activeStation: Station? = null,
        /** Nur der Arbeitsplatz ist speziesabhaengig eingerichtet - siehe [workPlacements]. */
        species: AvatarSpecies = AvatarSpecies.PUFFLING
    ): List<SceneCell> {
        if (widthCells <= 0 || floorY <= 0 || fade <= 0f) return emptyList()

        val cells = mutableListOf<SceneCell>()

        // Boden: die eine durchgehende Linie, die aus schwebenden Sprites eine Szene macht.
        //
        // Zu den Raendern hin ausgeblendet ([edgeFalloff]) statt hart an der Bildkante abgeschnitten:
        // Eine Linie, die von Rand zu Rand durchlaeuft, macht aus dem Zimmer eine Buehne mit
        // sichtbarem Ende. Loest sie sich seitlich auf, liest man sie als Ausschnitt eines
        // groesseren Raums - dieselbe Wirkung wie ein weicher Bildrand, nur mit einem Dutzend
        // Zellen erreicht.
        for (x in 0 until widthCells) {
            cells += SceneCell(x, floorY, (STRUCTURE * edgeFalloff(x, widthCells)).roundToInt())
        }
        // Lueckenhafte Zeile darunter: gibt dem Boden Dicke, ohne ihn zu einem massiven Balken
        // zu machen - ein zweiter durchgezogener Strich wirkte wie ein Rahmen.
        for (x in 0 until widthCells step 3) {
            cells += SceneCell(x, floorY + 1, (STRUCTURE / 2 * edgeFalloff(x, widthCells)).roundToInt())
        }

        // Zimmerdecke: eine einzelne schwache Linie weit oben, nur drinnen.
        //
        // Ohne sie standen die Moebel in einer nach oben offenen Schwaerze - man sah Gegenstaende
        // auf einem Boden, aber keinen RAUM. Eine zweite waagerechte Linie in Hoehe des
        // hoechsten Moebels genuegt, damit das Auge die Flaeche dazwischen als Zimmer liest; mehr
        // (Seitenwaende, Ecken) waere auf diesem Raster nur Gitterwerk. Zu den Seiten hin
        // ausgeblendet wie der Boden, damit das Zimmer nicht wie ein Kasten endet.
        if (place.isIndoors) {
            val ceilingY = floorY - CEILING_HEIGHT_CELLS
            if (ceilingY > 0) {
                for (x in 0 until widthCells) {
                    cells += SceneCell(x, ceilingY, (STRUCTURE * 0.8f * edgeFalloff(x, widthCells)).roundToInt())
                }
            }
        }

        // VOR den Requisiten: Beiwerk am Boden (Gras) liegt in derselben Zeile, in der die
        // Requisiten aufsetzen. Zeichnete man es danach, stanzte ein Grasbueschel dem Baumstamm
        // und dem Strauch eine dunklere Kerbe in die Silhouette - spaeter gezeichnete Zellen
        // ueberschreiben frueher gezeichnete.
        cells += groundDetail(place, widthCells, floorY)

        for (placement in placementsFor(place, species)) {
            val originX = originX(placement, widthCells)
            val originY = originY(placement, floorY)
            // Oberkanten heller als der Rest der Form - **der Unterschied zwischen einem Moebel
            // und einem Klotz.**
            //
            // Vorher bekam jede Zelle einer Requisite dieselbe Helligkeit. Eine gefuellte Flaeche
            // in genau einem Grauton hat aber keine Form; sie hat nur einen Umriss, und das las
            // sich als flacher Fleck. Reales Licht faellt von oben, also ist die oberste Zelle
            // jeder Saeule die beleuchtete Kante - Tischplatte, Matratzenoberseite, Baumkrone
            // bekommen dadurch von selbst eine Oberflaeche.
            //
            // Aus der Form GERECHNET statt gezeichnet: Wer eine Requisite aendert oder eine neue
            // hinzufuegt, bekommt die Kante geschenkt und kann sie nicht vergessen.
            val inUse = placement.station != null && placement.station == activeStation
            val shape = (if (inUse) placement.prop.usedArt ?: placement.prop.art else placement.prop.art)
                .toHashSet()
            for (point in shape) {
                val (px, py) = point
                val isTopEdge = (px to (py - 1)) !in shape
                val brightness = if (isTopEdge) {
                    (placement.brightness * RIM_BOOST).roundToInt().coerceAtMost(RIM_MAX)
                } else {
                    placement.brightness
                }
                cells += SceneCell(originX + px, originY + py, brightness)
            }
            // Was beim Oeffnen zum Vorschein kommt (Kuehlschrankbeleuchtung, Regalinneres) -
            // als Licht, damit es auch nachts sichtbar bleibt und nach "offen" aussieht statt
            // nach einem Loch in der Form.
            if (inUse) {
                for ((px, py) in placement.prop.usedGlow) {
                    cells += SceneCell(originX + px, originY + py, GLOW - 400, isLight = true)
                }
            }
        }

        cells += ambient(place, phase, widthCells, floorY, dayPhase, lampOn, tvOn, species)
        cells += housePet(place, phase, widthCells, floorY)

        // Materie folgt der Tageszeit, Licht nicht (siehe [SceneCell.isLight]).
        val roomFactor = atmosphere(dayPhase) * fade
        return cells.mapNotNull { cell ->
            val scaled = (cell.brightness * if (cell.isLight) fade else roomFactor).roundToInt()
            if (scaled <= 0 || cell.x < 0 || cell.x >= widthCells || cell.y < 0) null
            else if (scaled == cell.brightness) cell
            else cell.copy(brightness = scaled)
        }
    }

    /**
     * Wie hell der RAUM zur jeweiligen Tageszeit steht.
     *
     * Der eigentliche Gewinn ist nicht das Abdunkeln an sich, sondern das Verhaeltnis: Weil das
     * Licht davon ausgenommen ist, weichen Moebel und Waende nachts zurueck, und Mond, Lampe und
     * Mattscheibe treten hervor - dieselbe Kulisse erzaehlt dadurch morgens und um drei Uhr nachts
     * etwas anderes, ohne dass ein zweiter Satz Pixel-Art noetig waere. Nebenbei ist ein Dock, das
     * nachts von selbst zurueckfaehrt, genau das, was man auf einem Nachttisch will.
     */
    private fun atmosphere(dayPhase: PlayAmbientActivity.DayPhase): Float = when (dayPhase) {
        PlayAmbientActivity.DayPhase.MIDDAY -> 1f
        PlayAmbientActivity.DayPhase.MORNING -> 0.92f
        PlayAmbientActivity.DayPhase.EVENING -> 0.74f
        // Nicht tiefer: Bei 0,5 sank die Bodenlinie unter die Sichtbarkeitsschwelle, und die Figur
        // schien nachts wieder im Schwarzen zu schweben - genau der Zustand, den die Kulisse
        // beheben sollte. Der Raum darf zurueckweichen, aber der Boden muss bleiben.
        PlayAmbientActivity.DayPhase.NIGHT -> 0.58f
    }

    /** Seitliches Ausblenden des Bodens, siehe [build]. */
    private fun edgeFalloff(x: Int, widthCells: Int): Float {
        val edge = minOf(x, widthCells - 1 - x)
        if (edge >= FLOOR_FADE_CELLS) return 1f
        return (edge + 1f) / (FLOOR_FADE_CELLS + 1f)
    }

    private const val FLOOR_FADE_CELLS = 5

    /**
     * Hoehe der Zimmerdecke ueber dem Boden.
     *
     * Von 16 auf 18 angehoben, nachdem die Kompositions-Pruefung zeigte, dass STARLET - die
     * groesste der sechs Formen - stehend nur noch eine Zelle Luft hatte und in jedem erhoehten
     * Sitz- oder Liegeplatz durch die Decke ragte. Zwei Zellen mehr sind im Bild kaum zu bemerken,
     * geben aber jeder Figur den Kopfraum, den erhoehte Moebel brauchen.
     */
    private const val CEILING_HEIGHT_CELLS = 18

    // ---- Aufbau der Orte ----

    /**
     * Eine Requisite in ihrem eigenen kleinen Raster ([width] x [height], y = 0 oben) - so
     * gezeichnet, dass die UNTERSTE Zeile auf dem Boden aufsetzt.
     */
    private data class Prop(
        val width: Int,
        val height: Int,
        val art: List<Pair<Int, Int>>,
        /** Teile, die VOR der Figur liegen, waehrend sie diese Requisite benutzt - siehe [buildFront]. */
        val frontArt: List<Pair<Int, Int>> = emptyList(),
        /**
         * Wo die Figur aufsetzt, wenn sie die Requisite benutzt: (waagerechte Mitte, Aufsetzzeile)
         * in den lokalen Koordinaten der Requisite. Werte ausserhalb des eigenen Rasters sind
         * ausdruecklich erlaubt und gemeint - wer an einem Tisch STEHT, steht daneben und auf dem
         * Boden, nicht auf der Tischplatte.
         */
        val useSpot: Pair<Int, Int>? = null,
        /** Form waehrend der Benutzung - null heisst: sieht immer gleich aus. */
        val usedArt: List<Pair<Int, Int>>? = null,
        /** Leuchtende Teile, die erst beim Oeffnen sichtbar werden. */
        val usedGlow: List<Pair<Int, Int>> = emptyList()
    )

    /**
     * [anchorX] ist der Bruchteil der Szenenbreite, an dem die Requisite steht (0 = ganz links,
     * 1 = ganz rechts). [liftCells] hebt sie vom Boden ab - nur fuers Fenster, das an der Wand
     * haengt statt auf dem Boden zu stehen.
     */
    private data class Placement(
        val prop: Prop,
        val anchorX: Float,
        val liftCells: Int = 0,
        val brightness: Int = FURNITURE,
        /** Unter welchem Namen ein Tagesablauf diesen Platz ansprechen kann, siehe [Station]. */
        val station: Station? = null
    )

    private fun originX(placement: Placement, widthCells: Int): Int =
        ((widthCells - placement.prop.width).coerceAtLeast(0) * placement.anchorX).roundToInt()

    private fun originY(placement: Placement, floorY: Int): Int =
        floorY - placement.prop.height - placement.liftCells

    /**
     * **Nebenrequisiten stehen immer ganz am Rand** (anchorX 0 oder 1), die Hauptrequisite in der
     * Flaeche dazwischen.
     *
     * Nicht aus Geschmack, sondern aus Geometrie: Der Avatar belegt 16 Zellen Breite, eine
     * Requisite 3 bis 13. Auf einem schmalen Geraet (oder bei gross gezogener Uhr, was die
     * Zellgroesse hochtreibt) bleiben in der Breite nur rund 30 Zellen - dort ueberlappt jede
     * Verankerung "irgendwo bei 0,8" unweigerlich mit dem Avatar. Beim Fenster ist das der eine
     * Fall, den man nicht durchgehen lassen darf: eine Figur VOR einem Moebelstueck liest sich
     * als Tiefe, eine Figur vor dem Fenster laesst das Fenster schlicht verschwinden.
     */
    /**
     * Die Einrichtung eines Ortes. [species] wird nur am Arbeitsplatz ausgewertet - ueberall sonst
     * wohnen alle Kreaturen gleich, nur ihr Beruf unterscheidet sie.
     */
    private fun placementsFor(place: Place, species: AvatarSpecies = AvatarSpecies.PUFFLING): List<Placement> = when (place) {
        // Das Schlafzimmer ist nach dem Arbeitsplatz der zweite Raum, der von der Spezies
        // abhaengt - und der persoenlichste: Wie jemand schlaeft, sagt mehr ueber ihn als das,
        // was an seiner Wand haengt.
        Place.BEDROOM -> bedroomPlacements(species)

        Place.BATH -> listOf(
            Placement(bathFor(species), anchorX = 0f, station = Station.TUB),
            Placement(BASIN, anchorX = 0.62f, station = Station.BASIN),
            Placement(PICTURE, anchorX = 0.34f, liftCells = 11, brightness = BACKDROP),
            Placement(DOOR, anchorX = 1f, brightness = BACKDROP, station = Station.DOOR)
        )
        // Der Arbeitsplatz ist der EINZIGE Ort, dessen Einrichtung von der Spezies abhaengt -
        // siehe [workPlacements].
        Place.WORK -> workPlacements(species) + listOf(
            Placement(DOOR, anchorX = 1f, brightness = BACKDROP, station = Station.DOOR)
        )
        Place.DESK -> listOf(
            Placement(WINDOW, anchorX = 0f, liftCells = 8, brightness = BACKDROP),
            Placement(DESK, anchorX = 0.72f, station = Station.DESK),
            Placement(BOOKS, anchorX = 0.86f),
            Placement(DOOR, anchorX = 1f, brightness = BACKDROP, station = Station.DOOR)
        )
        Place.KITCHEN -> listOf(
            Placement(FRIDGE, anchorX = 0f, station = Station.FRIDGE),
            Placement(SHELF, anchorX = 0.52f, liftCells = SHELF_LIFT, brightness = BACKDROP),
            Placement(COUNTER, anchorX = 0.50f),
            Placement(diningFor(species), anchorX = 0.82f, station = Station.TABLE),
            Placement(DOOR, anchorX = 1f, brightness = BACKDROP, station = Station.DOOR)
        )
        Place.NOOK -> listOf(
            Placement(seatFor(species), anchorX = 0.08f, station = Station.SEAT),
            Placement(PICTURE, anchorX = 0f, liftCells = 10, brightness = BACKDROP),
            Placement(BOOKSHELF, anchorX = 0.60f, station = Station.BOOKSHELF),
            Placement(LAMP, anchorX = 0.80f, station = Station.LAMP),
            Placement(DOOR, anchorX = 1f, brightness = BACKDROP, station = Station.DOOR)
        )
        Place.LIVING -> listOf(
            Placement(SOFA, anchorX = 0.02f, station = Station.SEAT),
            Placement(PICTURE, anchorX = 0.34f, liftCells = 11, brightness = BACKDROP),
            Placement(PLANT, anchorX = 0.62f),
            Placement(TV, anchorX = 0.78f, station = Station.TV),
            Placement(DOOR, anchorX = 1f, brightness = BACKDROP, station = Station.DOOR)
        )
        Place.SHOP -> listOf(
            Placement(RACK, anchorX = 0.04f, station = Station.RACK),
            Placement(RACK, anchorX = 0.34f),
            Placement(SHELF, anchorX = 0.66f, liftCells = SHELF_LIFT, brightness = BACKDROP),
            Placement(CHECKOUT, anchorX = 0.72f, station = Station.CHECKOUT),
            Placement(DOOR, anchorX = 1f, brightness = BACKDROP, station = Station.DOOR)
        )
        Place.PARK -> listOf(
            Placement(TREE, anchorX = 0f),
            Placement(BENCH, anchorX = 0.55f, station = Station.BENCH),
            Placement(BUSH, anchorX = 0.70f),
            Placement(LAMPPOST, anchorX = 0.80f, station = Station.LAMP),
            Placement(TREE, anchorX = 1f)
        )
    }

    /**
     * Beiwerk, das sich nicht als einzelne Requisite verankern laesst, weil es ueber die ganze
     * Breite streut - derzeit die Grasbueschel draussen.
     *
     * Aus der Position selbst gerechnet statt gewuerfelt: So steht bei jedem Neuzeichnen derselbe
     * Halm an derselben Stelle. Zufaellig gestreutes Gras wuerde bei jedem Bild neu wachsen und
     * flimmern - eine Wiese muss stillstehen duerfen.
     */
    private fun groundDetail(place: Place, widthCells: Int, floorY: Int): List<SceneCell> = when (place) {
        Place.PARK -> (0 until widthCells)
            .filter { it % 7 == 3 || it % 11 == 5 }
            .map { SceneCell(it, floorY - 1, STRUCTURE) }
        // Teppich: ein aufgehellter Abschnitt der Bodenlinie mit Fransen an den Enden. Auf einem
        // seitlich gesehenen Boden ist ein Teppich nichts anderes als ein Stueck Boden, das anders
        // aussieht - genau das laesst sich hier mit drei Zellen Unterschied sagen.
        Place.LIVING -> rugAt(widthCells, floorY, from = 0.22f, to = 0.62f)
        Place.BEDROOM -> rugAt(widthCells, floorY, from = 0.40f, to = 0.72f)
        else -> emptyList()
    }

    private fun rugAt(widthCells: Int, floorY: Int, from: Float, to: Float): List<SceneCell> {
        val start = (widthCells * from).roundToInt()
        val end = (widthCells * to).roundToInt().coerceAtMost(widthCells - 1)
        if (end <= start) return emptyList()
        return (start..end).map { x ->
            // Fransen an den beiden Enden etwas heller als die Flaeche dazwischen.
            val fringe = x == start || x == end
            SceneCell(x, floorY, if (fringe) FURNITURE else (STRUCTURE * 1.6f).roundToInt())
        }
    }

    // ---- Die Requisiten ----
    //
    // Alle in Seitenansicht und bewusst als geschlossene Silhouetten gezeichnet, nicht als
    // Umrisse: bei sechs bis neun Zellen Hoehe zerfaellt eine Konturzeichnung optisch, eine
    // gefuellte Form bleibt auch stark gedimmt noch eindeutig lesbar. Dasselbe Prinzip wie beim
    // Avatar selbst (siehe [AvatarAnimations]).

    private fun rect(x0: Int, y0: Int, x1: Int, y1: Int): List<Pair<Int, Int>> =
        (y0..y1).flatMap { y -> (x0..x1).map { x -> x to y } }

    private fun hLine(x0: Int, x1: Int, y: Int): List<Pair<Int, Int>> = (x0..x1).map { it to y }

    private fun vLine(x: Int, y0: Int, y1: Int): List<Pair<Int, Int>> = (y0..y1).map { x to it }

    /**
     * Bett: hohes Kopfteil, aufgeschuettetes Kissen, zwei Zeilen Matratze, Fuesse.
     *
     * Die Hoehe des Kopfteils macht die Silhouette. Ein erster Entwurf hatte es nur zwei Zeilen
     * ueber die Matratze ragen lassen und dafuer drei Zeilen Matratze - das Ergebnis war ein
     * massiger Block, der ebensogut eine Kommode haette sein koennen. Bei sechs Zellen Hoehe
     * entscheidet die Umrisslinie allein, was man erkennt.
     */
    private val BED = Prop(
        width = 13, height = 7,
        art = rect(0, 0, 1, 3) +           // Kopfteil, vier Zeilen hoch
            hLine(2, 4, 3) +               // Kissen, liegt auf der Matratzenkante auf
            rect(0, 4, 12, 5) +            // Matratze
            listOf(1 to 6, 11 to 6),       // Fuesse
        // Decke: liegt VOR der Figur und verdeckt sie BIS ZUM HALS.
        //
        // Ein erster Entwurf liess nur die untersten drei Zeilen zugedeckt - das Ergebnis war
        // eine Figur, die aufrecht im Bett STAND und der jemand eine Decke ueber die Fuesse
        // gelegt hatte. Eine nach vorn blickende Silhouette wird durch etwas Ueberdeckung nicht
        // liegend; sie muss so weit verschwinden, dass nur noch der Kopf uebrig bleibt. Genau so
        // zeigt auch das Vorbild (Tamagotchi) einen Schlafenden: ein Kopf ueber einem Deckenberg.
        frontArt = rect(2, 2, 12, 5),
        // Auf der Matratze, mit dem Kopf zum Kissen hin.
        useSpot = 7 to 5
    )

    /** Schreibtisch mit Monitor - die Mattscheibe (x5..7, y1..2) bleibt frei und wird von
     *  [ambient] als flackerndes Licht gefuellt. */
    private val DESK = Prop(
        width = 12, height = 8,
        art = hLine(4, 8, 0) + vLine(4, 1, 2) + vLine(8, 1, 2) + hLine(4, 8, 3) +
            listOf(6 to 4) +               // Monitorfuss
            hLine(0, 11, 5) +              // Tischplatte
            vLine(1, 6, 7) + vLine(10, 6, 7),
        // Steht davor auf dem Boden (Zeile 7 ist die unterste des Moebels), leicht links versetzt.
        useSpot = -1 to 7
    )

    /** Tisch mit Kanne - der Dampf darueber kommt aus [ambient]. */
    private val TABLE = Prop(
        width = 11, height = 7,
        art = rect(5, 0, 7, 2) + listOf(8 to 1) +  // Kanne mit Tuelle
            hLine(0, 10, 3) +                       // Tischplatte
            vLine(1, 4, 6) + vLine(9, 4, 6),
        useSpot = -1 to 6
    )

    /** Haengeregal - haengt an der Wand, steht nicht auf dem Boden. */
    private val SHELF = Prop(
        width = 8, height = 4,
        art = hLine(0, 7, 3) +             // Brett
            rect(1, 0, 2, 2) + rect(4, 1, 4, 2) + rect(6, 0, 6, 2)   // Glaeser/Vorraete
    )

    /** Aufhaenghoehe des Regals - an zwei Stellen gebraucht (Platzierung und Aufsetzzeile). */
    private const val SHELF_LIFT = 9

    /** Sessel mit hoher Rueckenlehne und Armlehne. */
    /**
     * Sessel - **breiter als die Kreatur, und das ist keine Geschmacksfrage.**
     *
     * Der erste Entwurf war acht Zellen breit. Die Silhouetten sind aber rund zehn Zellen breit:
     * Wer sich hineinsetzte, ragte links und rechts darueber hinaus und verdeckte die
     * Rueckenlehne - der Sessel verlor seine Form, und es sah aus, als stuende jemand davor. Ein
     * Sitzmoebel muss mindestens so breit sein wie der, der darin sitzt.
     */
    private val CHAIR = Prop(
        width = 11, height = 7,
        art = rect(0, 0, 2, 4) +           // Rueckenlehne
            rect(0, 3, 10, 4) +            // Sitzflaeche
            listOf(10 to 2) +              // Armlehne
            listOf(1 to 5, 1 to 6, 9 to 5, 9 to 6),
        // Sitzflaeche und Armlehne liegen vor dem Sitzenden - dadurch sitzt er darin, statt
        // darauf zu stehen.
        frontArt = rect(3, 3, 10, 4) + listOf(10 to 2),
        useSpot = 6 to 4
    )

    /** Stehlampe - der Lichtkegel darunter kommt aus [ambient] und nur, wenn sie eingeschaltet ist. */
    private val LAMP = Prop(
        width = 3, height = 9,
        art = hLine(0, 2, 0) + hLine(0, 2, 1) +  // Schirm
            vLine(1, 2, 7) +                      // Staender
            hLine(0, 2, 8),                       // Fuss
        // Daneben, auf der linken Seite: rechts von der Lampe ist der Bildrand.
        useSpot = -4 to 8
    )

    /** Baum. */
    private val TREE = Prop(
        width = 7, height = 9,
        art = hLine(2, 4, 0) + hLine(1, 5, 1) + hLine(0, 6, 2) + hLine(0, 6, 3) + hLine(1, 5, 4) +
            vLine(3, 5, 8)
    )

    /** Strauch - flach, damit die Silhouette nicht mit dem Baum verwechselt wird. */
    private val BUSH = Prop(
        width = 6, height = 3,
        art = hLine(1, 4, 0) + hLine(0, 5, 1) + hLine(0, 5, 2)
    )

    /** Fenster mit Sprossenkreuz - die einzige Requisite als Umriss statt als Silhouette:
     *  eine gefuellte Flaeche waere hier eine Wand, kein Fenster. */
    private val WINDOW = Prop(
        width = 7, height = 6,
        art = hLine(0, 6, 0) + hLine(0, 6, 5) + vLine(0, 1, 4) + vLine(6, 1, 4) +
            vLine(3, 1, 4) + hLine(1, 5, 2)
    )

    /**
     * Das Schlafzimmer je Spezies - Schlafplatz plus dazu passendes Beiwerk.
     *
     * Der Schlafplatz folgt der Anatomie (siehe die Requisiten weiter unten), das Beiwerk dem
     * Charakter: Der Drache hortet, die Traeumerin haelt sich Pflanzen, der Beobachter hat seine
     * Buecher dabei. Zusammen ergibt das sechs Zimmer, die man auseinanderhalten kann, ohne dass
     * dafuer sechs vollstaendige Raeume gezeichnet werden mussten.
     */
    private fun bedroomPlacements(species: AvatarSpecies): List<Placement> {
        val common = listOf(
            Placement(DOOR, anchorX = 1f, brightness = BACKDROP, station = Station.DOOR)
        )
        val own = when (species) {
            AvatarSpecies.PUFFLING -> listOf(
                Placement(BED, anchorX = 0.02f, station = Station.BED),
                Placement(NIGHTSTAND, anchorX = 0.28f),
                Placement(PICTURE, anchorX = 0f, liftCells = 10, brightness = BACKDROP),
                Placement(PLANT, anchorX = 0.80f)
            )
            // Der Drache hortet: Buecherstapel und Kisten neben dem Nest.
            AvatarSpecies.WYRMLING -> listOf(
                Placement(NEST, anchorX = 0.04f, station = Station.BED),
                Placement(BOOKS, anchorX = 0.34f),
                Placement(BOOKS, anchorX = 0.44f),
                Placement(PICTURE, anchorX = 0f, liftCells = 11, brightness = BACKDROP),
                Placement(PLANT, anchorX = 0.78f)
            )
            // Die Traeumerin: Haengematte unter dem Fenster, viel Gruen.
            AvatarSpecies.STARLET -> listOf(
                Placement(HAMMOCK, anchorX = 0.06f, station = Station.BED),
                Placement(PLANT, anchorX = 0.62f),
                Placement(PLANT, anchorX = 0.72f),
                Placement(WINDOW, anchorX = 0.84f, liftCells = 9, brightness = BACKDROP)
            )
            // Der Wuestenfuchs: Bau statt Bett, sonst wenig - er braucht keine Auslage.
            AvatarSpecies.FENNEC -> listOf(
                Placement(DEN, anchorX = 0.04f, station = Station.BED),
                Placement(NIGHTSTAND, anchorX = 0.44f),
                Placement(WINDOW, anchorX = 0.80f, liftCells = 8, brightness = BACKDROP)
            )
            // Der Entschleuniger: eine Mulde am Boden, daneben, was gerade herumliegt.
            AvatarSpecies.GLOOP -> listOf(
                Placement(HOLLOW, anchorX = 0.04f, station = Station.BED),
                Placement(BOOKS, anchorX = 0.40f),
                Placement(PLANT, anchorX = 0.62f),
                Placement(PICTURE, anchorX = 0.16f, liftCells = 10, brightness = BACKDROP)
            )
            // Der Beobachter: Baumhoehle, Buecher, Fenster zum Hinaussehen.
            AvatarSpecies.HOOTLET -> listOf(
                Placement(TREEHOLE, anchorX = 0.06f, station = Station.BED),
                Placement(BOOKS, anchorX = 0.42f),
                Placement(WINDOW, anchorX = 0.76f, liftCells = 9, brightness = BACKDROP),
                Placement(PLANT, anchorX = 0.62f)
            )
        }
        return own + common
    }

    /**
     * **Jede Kreatur hat ihren eigenen Beruf** - und damit einen eigenen Arbeitsplatz.
     *
     * Abgeleitet aus dem, wofuer die jeweilige Figur ohnehin steht (siehe
     * [AvatarSpecies.signatureTopic] und die Charakterbeschreibungen): Wyrmling treibt an, also
     * arbeitet er in der Sporthalle; Hootlet beobachtet und weiss viel, also in der Bibliothek;
     * Fennec ist aufmerksam und kuemmert sich, also im Cafe. Der Beruf ist damit keine
     * zusaetzliche Eigenschaft, die man sich merken muesste, sondern die Fortsetzung dessen, was
     * die Figur ausmacht.
     *
     * **Zusammengesetzt aus vorhandenen Requisiten statt neu gezeichnet.** Ein eigener Satz
     * Pixel-Art je Beruf waeren sechs komplette Raeume - stattdessen ergibt eine andere
     * Zusammenstellung derselben Moebel bereits einen erkennbar anderen Ort. Das ist dasselbe
     * Prinzip, mit dem die Spezies ihren Charakter aus geteilten Bausteinen beziehen.
     */
    private fun workPlacements(species: AvatarSpecies): List<Placement> = when (species) {
        // STERNWARTE - "Ruhe bringt Klarheit". Eine Eule, die mehr beobachtet als sie spricht,
        // arbeitet dort, wo Beobachten der Beruf ist. Bewusst KEINE Bibliothek: Buecherregale
        // hat er zu Hause in der Leseecke, der Arbeitsplatz saehe aus wie sein Wohnzimmer.
        AvatarSpecies.HOOTLET -> listOf(
            Placement(TELESCOPE, anchorX = 0.62f, station = Station.WORKPLACE),
            Placement(BOOKS, anchorX = 0.18f),
            Placement(WINDOW, anchorX = 0.04f, liftCells = 9, brightness = BACKDROP),
            Placement(LAMP, anchorX = 0.92f, station = Station.LAMP)
        )

        // TURNHALLE - "Los geht's. Nur ein kleiner Schritt." Sprossenwand statt Hantelbank:
        // waagerechte Sprossen sind auf diesem Raster eindeutig lesbar, und sie passen zu einem
        // Motivator, der anleitet statt anzutreiben.
        AvatarSpecies.WYRMLING -> listOf(
            Placement(WALLBARS, anchorX = 0.06f, station = Station.WORKPLACE),
            Placement(BENCH, anchorX = 0.52f, station = Station.BENCH),
            Placement(LAMP, anchorX = 0.92f, station = Station.LAMP)
        )

        // LEUCHTTURM - "Ich passe auf deine Gewohnheiten auf." Ein Leuchtfeuer wacht ueber
        // andere, ohne sie je zu stoeren; genauer laesst sich dieser Charakter kaum in einen
        // Beruf uebersetzen. Der Strahl kommt aus [ambient].
        AvatarSpecies.FENNEC -> listOf(
            // Nicht ganz am Rand: Der Platz am Tisch liegt links davon (useSpot -1) und
            // laege bei schmalem Bild sonst ausserhalb.
            Placement(TABLE, anchorX = 0.18f, station = Station.TABLE),
            Placement(BEACON, anchorX = 0.52f, station = Station.WORKPLACE),
            Placement(WINDOW, anchorX = 0.30f, liftCells = 9, brightness = BACKDROP)
        )

        // GAERTNEREI - "Du musst heute nicht perfekt sein." Nichts hier wird heute fertig; es
        // waechst in seinem Tempo. Das ist ihr Leitsatz als Arbeitsplatz.
        AvatarSpecies.STARLET -> listOf(
            Placement(POTTING, anchorX = 0.10f, station = Station.WORKPLACE),
            Placement(PLANT, anchorX = 0.58f),
            Placement(PLANT, anchorX = 0.68f),
            Placement(WINDOW, anchorX = 0.90f, liftCells = 8, brightness = BACKDROP)
        )

        // BAECKEREI - "Alles darf auch langsam gehen." Teig laesst sich nicht beschleunigen, und
        // eine Backstube darf gemuetlich-chaotisch sein. Der Ofen leuchtet beim Oeffnen.
        AvatarSpecies.GLOOP -> listOf(
            Placement(OVEN, anchorX = 0.06f, station = Station.WORKPLACE),
            Placement(COUNTER, anchorX = 0.56f),
            Placement(SHELF, anchorX = 0.60f, liftCells = SHELF_LIFT, brightness = BACKDROP),
            Placement(LAMP, anchorX = 0.94f, station = Station.LAMP)
        )

        // ZUSTELLUNG - "Kleine Schritte bringen dich weiter." Ein Beruf, der woertlich aus
        // kleinen Schritten besteht und jeden Tag etwas Neues zeigt - das passt zum neugierigen
        // Optimisten weit besser als ein Buero, das ueber ihn gar nichts aussagt.
        AvatarSpecies.PUFFLING -> listOf(
            Placement(PARCELS, anchorX = 0.08f, station = Station.WORKPLACE),
            Placement(COUNTER, anchorX = 0.52f),
            Placement(PLANT, anchorX = 0.86f),
            Placement(LAMP, anchorX = 0.96f, station = Station.LAMP)
        )
    }

    // ---- Schlafplaetze je Spezies ----
    //
    // **Jede Kreatur schlaeft, wie ihr Koerper es nahelegt.** Ein Bett mit Kopfteil und Fuessen
    // passt zu einer Figur mit Beinen und Ruecken - GLOOP hat weder das eine noch das andere,
    // HOOTLET ist eine Eule. Sie alle in dasselbe Bett zu legen, war der auffaelligste Rest
    // Beliebigkeit in dieser Welt: Die Wohnung sagte ueber ihren Bewohner nichts aus.
    //
    // Alle folgen derselben Bauweise wie [BED]: eine Form, ein Teil davon liegt VOR dem
    // Schlafenden ([Prop.frontArt]) und verdeckt ihn bis zum Hals, plus eine Aufsetzstelle.

    /** WYRMLING - Drachennest aus Zweigen, dazu ein kleiner Hort. */
    private val NEST = Prop(
        width = 13, height = 6,
        art = listOf(0 to 0, 3 to 0, 9 to 0, 12 to 0) +          // Zweigspitzen
            listOf(0 to 1, 1 to 1, 11 to 1, 12 to 1) +
            hLine(0, 2, 2) + hLine(10, 12, 2) +
            hLine(0, 12, 3) + hLine(1, 11, 4) + hLine(3, 9, 5),
        frontArt = hLine(0, 12, 3) + hLine(1, 11, 4) + hLine(3, 9, 5),
        useSpot = 6 to 4
    )

    /** STARLET - Haengematte zwischen zwei Pfosten, durchhaengend. */
    private val HAMMOCK = Prop(
        width = 15, height = 8,
        art = vLine(0, 1, 7) + vLine(14, 1, 7) +                 // Pfosten
            listOf(0 to 0, 14 to 0) +
            listOf(1 to 2, 2 to 3, 12 to 3, 13 to 2) +           // Aufhaengung
            hLine(3, 11, 4) + hLine(4, 10, 5) + hLine(5, 9, 6),  // durchhaengendes Tuch
        frontArt = hLine(3, 11, 4) + hLine(4, 10, 5) + hLine(5, 9, 6),
        // Tief aufgehaengt: Eine Haengematte auf halber Zimmerhoehe schoebe die groesste Form
        // durch die Decke.
        useSpot = 7 to 5
    )

    /** FENNEC - Bau mit gewoelbtem Eingang und Kissen darin. */
    private val DEN = Prop(
        width = 13, height = 8,
        art = hLine(4, 8, 0) + hLine(2, 10, 1) + hLine(1, 11, 2) +
            vLine(0, 3, 7) + vLine(1, 3, 7) + vLine(11, 3, 7) + vLine(12, 3, 7) +
            hLine(2, 10, 6) + hLine(0, 12, 7),                   // Kissen und Boden
        frontArt = hLine(2, 10, 6) + hLine(0, 12, 7),
        useSpot = 6 to 6
    )

    /** GLOOP - flache Mulde. Keine Beine, kein Ruecken: Er sinkt hinein, statt sich hinzulegen. */
    private val HOLLOW = Prop(
        width = 13, height = 5,
        art = listOf(0 to 0, 12 to 0) +
            listOf(0 to 1, 1 to 1, 11 to 1, 12 to 1) +
            hLine(0, 12, 2) + hLine(1, 11, 3) + hLine(3, 9, 4),
        frontArt = hLine(0, 12, 2) + hLine(1, 11, 3) + hLine(3, 9, 4),
        useSpot = 6 to 3
    )

    /** HOOTLET - Baumhoehle. Eine Eule schlaeft nicht IM Bett, sondern IN einem Stamm. */
    private val TREEHOLE = Prop(
        width = 11, height = 15,
        art = (rect(1, 0, 9, 14) + listOf(0 to 2, 0 to 6, 0 to 10, 10 to 3, 10 to 8, 10 to 12)) -
            rect(3, 7, 7, 12),                                    // ausgehoehlte Oeffnung
        frontArt = hLine(2, 8, 12) + hLine(1, 9, 13),             // Unterkante der Hoehle
        useSpot = 5 to 12
    )

    // ---- Waschen und Essen je Spezies ----
    //
    // Auch hier folgt der Unterschied dem Koerper, nicht dem Geschmack: GLOOP hat keine Beine und
    // kommt in keine hohe Wanne und an keinen hohen Tisch. FENNEC ist ein Wuestenfuchs - der
    // badet im Sand, nicht im Wasser. HOOTLET ist ein Vogel, und Voegel baden in einer flachen
    // Schale. Das sind keine Einfaelle, sondern das, was die jeweilige Kreatur ueberhaupt tun
    // kann.

    /** FENNEC - Sandbad. Ein Wuestenfuchs waelzt sich, er badet nicht. */
    private val SANDBATH = Prop(
        width = 11, height = 4,
        art = listOf(0 to 0, 10 to 0) + listOf(0 to 1, 10 to 1) +
            hLine(0, 10, 2) + hLine(1, 9, 3),
        frontArt = hLine(0, 10, 2) + hLine(1, 9, 3),
        useSpot = 5 to 2
    )

    /** HOOTLET - flache Vogeltraenke auf niedrigem Fuss. */
    private val BIRDBATH = Prop(
        width = 9, height = 5,
        art = listOf(0 to 0, 8 to 0) + hLine(0, 8, 1) + hLine(1, 7, 2) +
            listOf(4 to 3) + hLine(2, 6, 4),
        frontArt = hLine(0, 8, 1) + hLine(1, 7, 2),
        useSpot = 4 to 1
    )

    /** GLOOP - flache Schale statt Wanne; er kaeme ueber keinen Wannenrand. */
    private val LOWTUB = Prop(
        width = 11, height = 3,
        art = listOf(0 to 0, 10 to 0) + hLine(0, 10, 1) + hLine(1, 9, 2),
        frontArt = hLine(0, 10, 1) + hLine(1, 9, 2),
        useSpot = 5 to 1
    )

    /** GLOOP - niedriger Tisch; an einen normalen kaeme er nicht heran. */
    private val LOWTABLE = Prop(
        width = 11, height = 4,
        art = rect(5, 0, 7, 0) +                                 // Schale darauf
            hLine(0, 10, 1) +                                    // Platte
            vLine(1, 2, 3) + vLine(9, 2, 3),
        useSpot = -1 to 3
    )

    /** Wo sich diese Spezies waescht. */
    private fun bathFor(species: AvatarSpecies): Prop = when (species) {
        AvatarSpecies.FENNEC -> SANDBATH
        AvatarSpecies.HOOTLET -> BIRDBATH
        AvatarSpecies.GLOOP -> LOWTUB
        else -> TUB
    }

    /** Woran diese Spezies isst. */
    private fun diningFor(species: AvatarSpecies): Prop = when (species) {
        AvatarSpecies.GLOOP -> LOWTABLE
        else -> TABLE
    }

    // ---- Sitzplaetze je Spezies ----
    //
    // Nur der PERSOENLICHE Sitzplatz in der Leseecke unterscheidet sich; das Sofa im Wohnzimmer
    // bleibt fuer alle dasselbe. Das ist Absicht: Ein Sofa ist das Moebel, auf dem man Gaeste
    // empfaengt - gemeinsame Einrichtung sagt etwas anderes aus als der eigene Lieblingsplatz.

    /** HOOTLET - Ast an einem Stamm. Eine Eule sitzt nicht IN etwas, sie sitzt AUF etwas. */
    private val PERCH = Prop(
        width = 11, height = 7,
        art = hLine(2, 8, 0) + hLine(3, 7, 1) +                  // Blaetterdach
            vLine(0, 2, 6) + vLine(1, 2, 6) +                    // Stamm
            hLine(2, 10, 4) +                                    // Ast
            hLine(0, 3, 6),                                      // Wurzelanlauf
        // Der Ast liegt VOR ihren Faengen - deshalb sitzt sie darauf und steht nicht davor.
        frontArt = hLine(3, 10, 4),
        // Nur drei Zellen ueber dem Boden. Ein erster Entwurf setzte den Ast auf halbe
        // Zimmerhoehe - dort ragte die Eule mit dem Kopf durch die Decke, weil die Figur
        // stehend schon fast bis dorthin reicht. Eine Sitzstange kann in einem Zimmer nicht
        // hoeher sein als der Kopf dessen, der davorsteht.
        useSpot = 6 to 4
    )

    /** GLOOP - flaches Bodenkissen. Ohne Beine setzt er sich nicht, er sackt darauf zusammen. */
    private val CUSHION = Prop(
        width = 9, height = 3,
        art = hLine(1, 7, 0) + hLine(0, 8, 1) + hLine(0, 8, 2),
        frontArt = hLine(0, 8, 1) + hLine(0, 8, 2),
        useSpot = 4 to 1
    )

    /** STARLET - weicher Sitzsack, tief und rund. */
    private val POUF = Prop(
        width = 9, height = 5,
        art = hLine(2, 6, 0) + hLine(1, 7, 1) + hLine(0, 8, 2) + hLine(0, 8, 3) + hLine(1, 7, 4),
        frontArt = hLine(0, 8, 2) + hLine(0, 8, 3) + hLine(1, 7, 4),
        useSpot = 4 to 2
    )

    /** WYRMLING - Steinvorsprung. Ein Drache thront, er lehnt sich nicht an. */
    private val LEDGE = Prop(
        width = 11, height = 5,
        art = hLine(2, 8, 0) + hLine(1, 9, 1) + hLine(0, 10, 2) + rect(0, 3, 10, 4),
        frontArt = rect(0, 3, 10, 4),
        useSpot = 5 to 3
    )

    /** FENNEC - Kissenberg mit angedeuteten Nahtstellen. */
    private val CUSHIONS = Prop(
        width = 11, height = 5,
        art = (hLine(2, 7, 0) + hLine(1, 9, 1) + hLine(0, 10, 2) +
            hLine(1, 9, 3) + hLine(2, 8, 4)) - listOf(5 to 1, 4 to 3),
        frontArt = hLine(0, 10, 2) + hLine(1, 9, 3) + hLine(2, 8, 4),
        useSpot = 5 to 2
    )

    /** Der persoenliche Sitzplatz je Spezies - siehe oben. */
    private fun seatFor(species: AvatarSpecies): Prop = when (species) {
        AvatarSpecies.PUFFLING -> CHAIR
        AvatarSpecies.HOOTLET -> PERCH
        AvatarSpecies.GLOOP -> CUSHION
        AvatarSpecies.STARLET -> POUF
        AvatarSpecies.WYRMLING -> LEDGE
        AvatarSpecies.FENNEC -> CUSHIONS
    }

    // ---- Berufs-Requisiten ----
    //
    // Jede Kreatur hat einen Beruf, der aus IHREM Leitsatz folgt (siehe [workPlacements]). Damit
    // die Arbeitsstelle sich auch anfuehlt wie ein anderer Ort, bekommt jede ihr eigenes
    // Kernstueck - eine Bibliothek aus denselben Buecherregalen wie die Leseecke zu Hause waere
    // zwar ein anderer Raum, saehe aber aus wie derselbe.

    /** Teleskop (HOOTLET, Sternwarte) - Rohr auf einem Dreibein, schraeg zum Himmel. */
    private val TELESCOPE = Prop(
        width = 11, height = 12,
        art = listOf(8 to 0, 9 to 0, 7 to 1, 8 to 1, 6 to 2, 7 to 2, 5 to 3, 6 to 3) +
            listOf(4 to 4, 5 to 4, 3 to 5, 4 to 5, 2 to 6, 3 to 6) +   // Rohr
            hLine(1, 4, 7) +                                            // Halterung
            vLine(3, 8, 10) + listOf(1 to 11, 2 to 11, 4 to 11, 5 to 11),
        // Weit rechts daneben: Das Rohr laeuft schraeg nach oben durch das halbe Raster - stand
        // die Figur dichter, verdeckte sie genau das Stueck, das das Teleskop erkennbar macht.
        useSpot = 15 to 11
    )

    /** Leuchtfeuer (FENNEC, Leuchtturm) - Laternenhaus; der Strahl kommt aus [ambient]. */
    private val BEACON = Prop(
        width = 9, height = 11,
        art = hLine(2, 6, 0) + hLine(1, 7, 1) +                         // Dach
            vLine(1, 2, 6) + vLine(7, 2, 6) +                           // Glashaus
            hLine(1, 7, 7) +
            rect(2, 8, 6, 10),                                          // Sockel
        // LINKS daneben, denn der Strahl verlaesst den Turm nach rechts. Stand die Figur dort,
        // lag sie mitten im Lichtkegel und verdeckte ihn auf ganzer Laenge.
        useSpot = -6 to 10,
        usedGlow = rect(3, 3, 5, 5)
    )

    /** Backofen (GLOOP, Baeckerei) - grosse Klappe, innen warm. */
    private val OVEN = Prop(
        width = 10, height = 9,
        art = (hLine(0, 9, 0) + rect(0, 1, 9, 8)) -
            rect(2, 3, 7, 6) +                                          // Ofentuer
            hLine(2, 7, 2),                                             // Griffleiste
        useSpot = 13 to 8,
        usedGlow = rect(2, 3, 7, 6)
    )

    /** Pflanztisch (STARLET, Gaertnerei) - Topfreihe auf einer Bank. */
    private val POTTING = Prop(
        width = 12, height = 7,
        art = listOf(1 to 0, 2 to 0, 5 to 0, 6 to 0, 9 to 0, 10 to 0) + // Triebe
            rect(1, 1, 2, 2) + rect(5, 1, 6, 2) + rect(9, 1, 10, 2) +   // Toepfe
            hLine(0, 11, 3) +                                           // Arbeitsplatte
            vLine(1, 4, 6) + vLine(10, 4, 6),
        useSpot = 14 to 6
    )

    /** Sprossenwand (WYRMLING, Turnhalle) - waagerechte Sprossen zwischen zwei Holmen. */
    private val WALLBARS = Prop(
        width = 9, height = 14,
        art = vLine(0, 0, 13) + vLine(8, 0, 13) +
            (0..6).flatMap { hLine(1, 7, it * 2) },
        useSpot = 12 to 13
    )

    /** Paketregal (PUFFLING, Zustellung) - Faecher voller Sendungen. */
    private val PARCELS = Prop(
        width = 11, height = 10,
        art = hLine(0, 10, 0) + hLine(0, 10, 4) + hLine(0, 10, 9) +
            vLine(0, 0, 9) + vLine(10, 0, 9) +
            rect(1, 1, 3, 3) + rect(5, 2, 7, 3) +                       // Pakete oben
            rect(2, 6, 4, 8) + rect(6, 5, 9, 8),                        // Pakete unten
        useSpot = 14 to 9,
        usedGlow = rect(1, 1, 3, 3)
    )

    /** Badewanne mit Rand und Fuessen - das Wasser darin kommt aus [buildFront]. */
    private val TUB = Prop(
        width = 11, height = 6,
        art = vLine(0, 0, 4) + vLine(10, 0, 4) + hLine(0, 10, 4) +
            listOf(1 to 5, 9 to 5) +
            listOf(0 to 0, 10 to 0),
        // Wasser: liegt VOR dem Badenden und verdeckt ihn bis zur Brust - dasselbe Mittel wie die
        // Bettdecke, und aus demselben Grund (siehe [buildFront]).
        frontArt = rect(1, 2, 9, 4),
        useSpot = 5 to 4
    )

    /** Waschbecken mit Spiegel darueber. */
    private val BASIN = Prop(
        width = 7, height = 11,
        art = hLine(1, 5, 0) + vLine(1, 1, 3) + vLine(5, 1, 3) + hLine(1, 5, 4) +  // Spiegel
            listOf(3 to 5) +                                                        // Hahn
            hLine(0, 6, 6) + rect(1, 7, 5, 7) +                                     // Becken
            vLine(3, 8, 10),                                                        // Saeule
        // Seitlich davor statt mittig davor: Mittig verdeckte die Figur Becken UND Spiegel fast
        // vollstaendig - man sah nicht mehr, wovor sie steht.
        useSpot = -5 to 10
    )

    /**
     * Warenregal im Laden - vier Boeden mit Waren darauf. Bewusst hoeher und schmaler gegliedert
     * als der Buecherschrank, damit die beiden Silhouetten nicht zu verwechseln sind: Im Regal
     * liegen die Waren FLACH auf den Boeden, im Schrank stehen die Buecher senkrecht.
     */
    private val RACK = Prop(
        width = 9, height = 12,
        art = hLine(0, 8, 0) + hLine(0, 8, 4) + hLine(0, 8, 8) + hLine(0, 8, 11) +
            vLine(0, 0, 11) + vLine(8, 0, 11) +
            listOf(2 to 3, 3 to 3, 5 to 3, 6 to 3) +
            listOf(1 to 7, 2 to 7, 4 to 7, 6 to 7, 7 to 7) +
            listOf(2 to 10, 3 to 10, 5 to 10),
        useSpot = 12 to 11,
        usedGlow = listOf(2 to 3, 3 to 3, 5 to 3, 6 to 3)
    )

    /** Ladentheke mit Kasse - die Kasse leuchtet beim Bezahlen auf. */
    private val CHECKOUT = Prop(
        width = 10, height = 6,
        art = hLine(0, 9, 0) + rect(0, 1, 9, 5) + rect(6, 0, 8, 0),
        useSpot = -3 to 5,
        usedGlow = listOf(7 to 0)
    )

    /**
     * Tuer - Rahmen mit Blatt und Griff, als Umriss statt als Flaeche: Eine gefuellte Tuer waere
     * von einem Schrank nicht zu unterscheiden.
     *
     * Steht in JEDEM Innenraum an derselben Seite. Das ist kein Zufall, sondern die Bedingung
     * dafuer, dass Raumwechsel lesbar werden: Wer links hinausgeht, kommt im naechsten Raum links
     * wieder herein - erst diese Verlaesslichkeit macht aus zwei Bildern zwei Zimmer derselben
     * Wohnung.
     */
    private val DOOR = Prop(
        width = 7, height = 13,
        art = vLine(0, 0, 12) + vLine(6, 0, 12) + hLine(0, 6, 0) +
            vLine(1, 1, 12) + vLine(5, 1, 12) +          // Tuerblatt-Kanten
            listOf(4 to 7),                              // Griff
        // LINKS vor der Tuer, nicht dahinter: Die Tuer steht am rechten Bildrand, eine
        // Aufsetzstelle rechts davon laege ausserhalb des Bildes (der Test faengt genau das ab).
        useSpot = -3 to 12,
        // Geoeffnet: Das Tuerblatt schwenkt zur Angel hin zusammen (nur noch die Kante bei x1
        // bleibt stehen), die Oeffnung wird frei. Seit sich Kuehlschrank und Regal beim Benutzen
        // oeffnen, fiel ausgerechnet die Tuer auf, durch die er hindurchging, als waere sie zu.
        usedArt = vLine(0, 0, 12) + vLine(6, 0, 12) + hLine(0, 6, 0) + vLine(1, 1, 12),
        // Licht aus dem Nebenraum, das durch die offene Tuer hereinfaellt - von der Schwelle
        // aufwaerts. Der erste Entwurf hatte nur die Schwellenlinie; das unterschied eine
        // Oeffnung zwar von einem Loch, war aber zu wenig, um den Durchgang zu tragen. Die
        // beleuchtete Oeffnung ist das, IN das die Figur hineintritt, waehrend sie verblasst -
        // ohne sie verschwindet sie einfach vor einer Wand.
        usedGlow = rect(2, 6, 5, 12)
    )

    /** Parkbank. */
    private val BENCH = Prop(
        width = 11, height = 6,
        art = hLine(0, 10, 0) + hLine(0, 10, 1) +        // Rueckenlehne
            listOf(0 to 2, 10 to 2) +                     // Streben
            hLine(0, 10, 3) +                             // Sitzflaeche
            listOf(1 to 4, 1 to 5, 9 to 4, 9 to 5),       // Beine
        // Sitzflaeche vor dem Sitzenden - dasselbe Mittel wie beim Sessel.
        frontArt = hLine(2, 10, 3) + listOf(10 to 2),
        useSpot = 5 to 3
    )

    /** Laterne - die einzige Lichtquelle im Park und nachts sein Mittelpunkt. */
    private val LAMPPOST = Prop(
        width = 5, height = 14,
        art = hLine(1, 3, 0) + hLine(0, 4, 1) + hLine(1, 3, 2) +   // Leuchtenkopf
            vLine(2, 3, 12) +                                       // Mast
            hLine(1, 3, 13),                                        // Fuss
        // Daneben auf dem Boden - die Laterne steht am rechten Bildrand, also stellt er sich links.
        useSpot = -4 to 13
    )

    // ---- Beiwerk ----
    //
    // Kleine Dinge, die keine Rolle spielen und genau deshalb wichtig sind: Ein Raum mit
    // ausschliesslich benutzten Gegenstaenden wirkt wie eine Requisitenliste. Erst was ohne
    // Funktion herumsteht, macht daraus einen bewohnten Ort.

    /** Bild an der Wand. */
    private val PICTURE = Prop(
        width = 5, height = 4,
        art = hLine(0, 4, 0) + hLine(0, 4, 3) + vLine(0, 1, 2) + vLine(4, 1, 2) +
            listOf(2 to 1, 1 to 2, 3 to 2)
    )

    /** Buecherstapel - die versetzten Kanten machen aus einem Block einen Stapel. */
    private val BOOKS = Prop(
        width = 4, height = 3,
        art = hLine(1, 3, 0) + hLine(0, 3, 1) + hLine(1, 3, 2)
    )

    /** Nachttisch mit kleiner Leuchte darauf. */
    private val NIGHTSTAND = Prop(
        width = 5, height = 9,
        art = (hLine(1, 3, 0) + listOf(2 to 1) + hLine(0, 4, 2) + rect(0, 3, 4, 8)) - hLine(1, 3, 5)
    )

    /** Kuehlschrank - gefuellte Form mit ausgestanzter Tuerfuge und Griff. */
    private val FRIDGE = Prop(
        width = 5, height = 9,
        art = (rect(0, 0, 4, 8) - hLine(1, 3, 3) - listOf(3 to 5, 3 to 6)),
        useSpot = 9 to 8,
        usedArt = (rect(0, 0, 1, 8) + hLine(0, 4, 0) + hLine(0, 4, 8) +
            vLine(6, 1, 7) + vLine(7, 1, 7)) - listOf(6 to 4, 7 to 4),
        usedGlow = rect(2, 1, 4, 7)
    )

    /** Kuechenzeile - Spuele, Arbeitsplatte, Kochfeld und Backofen in EINEM Stueck. */
    private val COUNTER = Prop(
        width = 16, height = 8,
        art = (listOf(3 to 0, 4 to 0, 4 to 1) + hLine(0, 15, 2) + rect(0, 3, 15, 7)) -
            rect(2, 3, 4, 4) - listOf(10 to 2, 11 to 2, 13 to 2, 14 to 2) -
            rect(10, 5, 14, 6) - vLine(8, 4, 6),
        useSpot = 6 to 7
    )

    /** Buecherschrank - zwei Faecher mit Buechergruppen. */
    private val BOOKSHELF = Prop(
        width = 7, height = 11,
        art = vLine(0, 0, 10) + vLine(6, 0, 10) + hLine(0, 6, 0) + hLine(0, 6, 10) +
            hLine(1, 5, 5) +
            vLine(1, 1, 4) + vLine(2, 1, 4) + vLine(4, 2, 4) +
            vLine(1, 6, 9) + vLine(3, 6, 9) + vLine(4, 6, 9),
        useSpot = -5 to 10,
        usedArt = vLine(0, 0, 10) + vLine(6, 0, 10) + hLine(0, 6, 0) + hLine(0, 6, 10) +
            hLine(1, 5, 5) +
            vLine(1, 1, 4) + vLine(4, 2, 4) +
            vLine(1, 6, 9) + vLine(3, 6, 9) + vLine(4, 6, 9)
    )

    /** Sofa - mit Armlehnen und angedeuteten Sitzkissen. */
    private val SOFA = Prop(
        width = 15, height = 7,
        art = (rect(1, 0, 13, 2) + rect(0, 1, 1, 4) + rect(13, 1, 14, 4) +
            rect(1, 3, 13, 4) + listOf(1 to 5, 13 to 5)) - listOf(5 to 3, 9 to 3),
        frontArt = rect(2, 3, 13, 4) + rect(13, 1, 14, 4),
        useSpot = 7 to 4
    )

    /** Fernseher auf niedrigem Fuss - die Mattscheibe fuellt [ambient]. */
    private val TV = Prop(
        width = 11, height = 9,
        art = hLine(0, 10, 0) + hLine(0, 10, 6) + vLine(0, 1, 5) + vLine(10, 1, 5) +
            listOf(5 to 7) + hLine(3, 7, 8),
        useSpot = -6 to 8
    )

    /** Topfpflanze. */
    private val PLANT = Prop(
        width = 4, height = 5,
        art = listOf(0 to 0, 3 to 0) + hLine(0, 3, 1) + hLine(1, 2, 2) +
            hLine(1, 2, 3) + hLine(0, 3, 4)
    )

    // ---- Umgebungsanimation ----

    /**
     * Was sich am jeweiligen Ort von selbst bewegt.
     *
     * Bewusst sehr sparsam: EIN Detail je Ort, langsam getaktet. Eine Kulisse, in der alles
     * gleichzeitig zappelt, zieht die Aufmerksamkeit von der Figur weg - sie soll leben, nicht
     * um Aufmerksamkeit konkurrieren. Aus demselben Grund traegt auch die Ruhe-Schleife des
     * Avatars ihre Bewegung in einem einzigen Detail (siehe [AvatarAnimations.idleSequence]).
     */
    /**
     * Mond oder Sonne im Fenster, nachts zusaetzlich ein blinkender Stern - geteilt von allen
     * Innenraeumen mit Fenster, damit derselbe Himmel nicht zweimal gepflegt werden muss.
     */
    private fun skyInWindow(
        placements: List<Placement>,
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase,
        phase: Int
    ): List<SceneCell> {
        val window = placements.firstOrNull { it.prop === WINDOW } ?: return emptyList()
        val ox = originX(window, widthCells)
        val oy = originY(window, floorY)
        val night = dayPhase == PlayAmbientActivity.DayPhase.NIGHT ||
            dayPhase == PlayAmbientActivity.DayPhase.EVENING
        // Mond schmal (zwei Zellen), Sonne breit (drei) - der Unterschied muss auf dieser
        // Groesse ueber die Form laufen, Farbe steht als Unterscheidung nicht zur Verfuegung.
        val celestial = if (night) {
            listOf(SceneCell(ox + 5, oy + 1, HIGHLIGHT, isLight = true))
        } else {
            // Tageslicht schwankt leicht, als zoege draussen etwas vorbei. Ohne das stand die
            // Kulisse tagsueber voellig still (nachts sorgt der Stern fuer die Regung) - und ein
            // regungsloser Hintergrund hinter einer animierten Figur liest sich als Fehler in der
            // Darstellung, nicht als Ruhe.
            val daylight = if (beat(phase, DAYLIGHT_TICKS) % 3 == 0) GLOW else GLOW - 380
            listOf(
                SceneCell(ox + 4, oy + 1, daylight, isLight = true),
                SceneCell(ox + 5, oy + 1, daylight, isLight = true),
                SceneCell(ox + 5, oy + 3, daylight, isLight = true)
            )
        }
        // Selten und kurz statt im Halbtakt an/aus: ein Stern, der jede Sekunde blinkt, ist eine
        // Signallampe. Auf 5 von 16 Zaehlschritten sichtbar wirkt er wie ein Funkeln.
        val star = if (night && beat(phase, STAR_TICKS) % 4 == 0) {
            listOf(SceneCell(ox + 1, oy + 3, GLOW / 2, isLight = true))
        } else {
            emptyList()
        }
        // Tagsueber faellt Licht durchs Fenster auf den Boden - nachts nicht, dann steht der Mond
        // zu tief und zu schwach dafuer. Genau dieser Unterschied laesst denselben Raum morgens
        // offen und nachts geborgen wirken.
        val pool = if (night) {
            emptyList()
        } else {
            lightPool(ox + 3, floorY, radius = 6, peak = GLOW - 700)
        }
        return celestial + star + pool
    }

    /**
     * Teilt den gemeinsamen Grundtakt herunter, damit jedes Detail seinen EIGENEN Rhythmus
     * bekommt.
     *
     * Vorher liefen Dampf, Lampe, Stern und Mattscheibe alle direkt auf demselben Zaehler und
     * damit auf demselben Raster - was zusammen wie ein Uhrwerk wirkte, nicht wie eine Umgebung.
     * Lebendigkeit entsteht gerade daraus, dass sich die Dinge NICHT gleichzeitig regen. Der
     * Grundtakt ist deshalb fein (siehe SCENE_PHASE_TICK_MS in ui/DockScreen.kt), und jedes
     * Detail nimmt sich daraus sein eigenes Vielfaches.
     */
    private fun beat(phase: Int, everyTicks: Int): Int = phase / everyTicks.coerceAtLeast(1)

    /**
     * Lichtschein auf dem Boden unter einer Lichtquelle.
     *
     * **Warum das so viel ausmacht.** Eine Lampe, die selbst leuchtet, aber nichts beleuchtet, ist
     * ein heller Fleck neben Moebeln - sie wirkt aufgeklebt. Erst der Schein auf dem Boden
     * verbindet sie mit dem Raum und macht aus zwei nebeneinanderliegenden Zeichnungen eine Szene
     * mit einer Lichtquelle darin. Es ist derselbe Gedanke wie bei der beleuchteten Oberkante der
     * Moebel (siehe [build]), nur andersherum: dort wirkt das Licht AUF etwas, hier kommt es VON
     * etwas.
     *
     * Nach aussen abfallend, damit es ein Schein bleibt und kein Balken.
     */
    private fun lightPool(centerX: Int, floorY: Int, radius: Int, peak: Int): List<SceneCell> =
        (-radius..radius).mapNotNull { d ->
            val falloff = 1f - abs(d) / (radius + 1f)
            val brightness = (peak * falloff * falloff).roundToInt()
            if (brightness <= 0) null else SceneCell(centerX + d, floorY, brightness, isLight = true)
        }

    /** Kurzes Aufblitzen auf einem eigenen Vielfachen des Grundtakts - siehe [beat]. */
    private fun twinkleOf(phase: Int, everyTicks: Int, peak: Int): Int =
        if (beat(phase, everyTicks) % 3 == 0) peak else peak / 3

    /**
     * Licht der Parklaterne - nur abends und nachts. Tagsueber brennt sie nicht, und genau das
     * macht den Unterschied zwischen einem Park am Nachmittag und einem am Abend aus, ohne dass
     * dafuer eine zweite Kulisse noetig waere.
     */
    private fun parkLantern(
        placements: List<Placement>,
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase
    ): List<SceneCell> {
        if (dayPhase != PlayAmbientActivity.DayPhase.NIGHT &&
            dayPhase != PlayAmbientActivity.DayPhase.EVENING
        ) return emptyList()
        val post = placements.firstOrNull { it.prop === LAMPPOST } ?: return emptyList()
        val ox = originX(post, widthCells)
        val oy = originY(post, floorY)
        return listOf(
            SceneCell(ox + 2, oy + 1, GLOW, isLight = true),
            SceneCell(ox + 1, oy + 1, GLOW / 2, isLight = true),
            SceneCell(ox + 3, oy + 1, GLOW / 2, isLight = true)
        ) + lightPool(ox + 2, floorY, radius = 6, peak = GLOW - 500)
    }

    /**
     * Ein Vogel zieht tagsueber durch den Park - drei Zellen, die auf und ab schlagen.
     *
     * Das billigste Stueck Leben im ganzen Entwurf und eines der wirksamsten: Etwas, das sich
     * bewegt, ohne dass es der Figur gehoert oder von ihr ausgeht, macht aus einer Buehne eine
     * Gegend. Nachts bleibt er weg - dann gehoert der Himmel den Sternen.
     */
    private fun parkBird(
        phase: Int,
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase
    ): List<SceneCell> {
        if (dayPhase == PlayAmbientActivity.DayPhase.NIGHT) return emptyList()
        val span = widthCells + BIRD_WIDTH
        val step = beat(phase, BIRD_TICKS) % (span * 3)
        if (step >= span) return emptyList()      // fliegt nur in einem Drittel der Zeit vorbei
        val x = step - BIRD_WIDTH
        // Fluegelschlag: die Spitzen wechseln zwischen oben und unten.
        val up = beat(phase, BIRD_TICKS) % 2 == 0
        val y = (floorY - 19).coerceAtLeast(0)
        val tip = if (up) y - 1 else y + 1
        return listOf(
            SceneCell(x, tip, FURNITURE),
            SceneCell(x + 1, y, FURNITURE),
            SceneCell(x + 2, tip, FURNITURE)
        )
    }

    /**
     * Eine Sternschnuppe zieht selten schraeg durch den Nachthimmel.
     *
     * **Warum gerade hier.** Nachts im Park sinkt der Horizont und der Himmel nimmt fast den
     * ganzen Bildschirm ein (siehe [floorFraction]) - eine grosse leere Flaeche, die bisher nur
     * ein paar stehende Sterne trug. Ein Ereignis, das quer hindurchzieht und wieder weg ist,
     * nutzt genau diese Weite. Und weil es SELTEN kommt (in etwa einem Sechstel der Zeit),
     * belohnt es das Zuschauen, statt zur Kulisse zu werden.
     */
    private fun shootingStar(
        phase: Int,
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase
    ): List<SceneCell> {
        if (dayPhase != PlayAmbientActivity.DayPhase.NIGHT) return emptyList()
        val cycle = beat(phase, SHOOTING_TICKS) % SHOOTING_CYCLE
        if (cycle >= SHOOTING_LENGTH) return emptyList()
        val startX = widthCells / 5
        val startY = (floorY * 0.28f).toInt().coerceAtLeast(1)
        val x = startX + cycle * 3
        val y = startY + cycle
        // Kopf hell, dahinter ein kurzer, verloeschender Schweif.
        return (0..3).mapNotNull { tail ->
            val b = if (tail == 0) HIGHLIGHT else HIGHLIGHT - tail * 900
            if (b <= 0) null else SceneCell(x - tail * 3, y - tail, b, isLight = true)
        }
    }

    private const val SHOOTING_TICKS = 1
    private const val SHOOTING_LENGTH = 8
    private const val SHOOTING_CYCLE = 48

    private const val BIRD_TICKS = 3
    private const val BIRD_WIDTH = 3

    /**
     * Ein kleines Tier huscht gelegentlich am Boden entlang - drinnen das Gegenstueck zum Vogel
     * im Park.
     *
     * Vier Zellen mit auf und ab wippendem Ruecken, nur in Wohnraeumen (nicht am Schreibtisch und
     * nicht im Laden) und nur in einem Viertel der Zeit. Wie beim Vogel gilt: Etwas, das sich
     * bewegt, ohne der Figur zu gehoeren oder von ihr auszugehen, macht aus einem eingerichteten
     * Zimmer ein bewohntes.
     */
    private fun housePet(place: Place, phase: Int, widthCells: Int, floorY: Int): List<SceneCell> {
        if (place != Place.LIVING && place != Place.BEDROOM && place != Place.KITCHEN) return emptyList()
        val span = widthCells + PET_WIDTH
        val step = beat(phase, PET_TICKS) % (span * 4)
        if (step >= span) return emptyList()
        val x = span - PET_WIDTH - step      // laeuft von rechts nach links
        val hop = if (beat(phase, PET_TICKS) % 2 == 0) 0 else -1
        return listOf(
            SceneCell(x, floorY - 1, FURNITURE),
            SceneCell(x + 1, floorY - 2 + hop, FURNITURE),
            SceneCell(x + 2, floorY - 2 + hop, FURNITURE),
            SceneCell(x + 3, floorY - 1, FURNITURE)
        )
    }

    private const val PET_TICKS = 2
    private const val PET_WIDTH = 4

    private const val BEACON_TICKS = 2
    private const val DRIP_TICKS = 3
    private const val SIGN_TICKS = 4
    private const val TV_TICKS = 2
    private const val DAYLIGHT_TICKS = 9
    private const val STAR_TICKS = 5
    private const val SCREEN_TICKS = 3
    private const val STEAM_TICKS = 3
    private const val LAMP_TICKS = 7
    private const val CLOUD_TICKS = 8
    private const val CLOUD_WIDTH = 6

    private fun ambient(
        place: Place,
        phase: Int,
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase,
        lampOn: Boolean,
        tvOn: Boolean,
        species: AvatarSpecies
    ): List<SceneCell> {
        val placements = placementsFor(place, species)
        return when (place) {
            // Himmelskoerper im Fenster - wechselt mit der Tageszeit, damit die Kulisse
            // dieselbe Uhrzeit "kennt" wie der Tagesablauf des Avatars.
            Place.BEDROOM -> skyInWindow(placements, widthCells, floorY, dayPhase, phase)

            // Fenster wie im Schlafzimmer, dazu die flackernde Mattscheibe: Der Monitor ist als
            // leerer Rahmen gezeichnet ([DESK]) und wird erst hier gefuellt - dadurch ist das
            // Licht eine eigene Ebene und kann heller sein als das Geraet drumherum.
            Place.DESK -> {
                val sky = skyInWindow(placements, widthCells, floorY, dayPhase, phase)
                val desk = placements.firstOrNull { it.prop === DESK } ?: return sky
                val ox = originX(desk, widthCells)
                val oy = originY(desk, floorY)
                // Zwei Helligkeiten im Wechsel statt an/aus: ein hart blinkender Bildschirm
                // saehe nach Defekt aus, ein leicht atmender nach Betrieb.
                val lit = if (beat(phase, SCREEN_TICKS) % 2 == 0) GLOW else GLOW - 550
                val screen = (1..2).flatMap { row ->
                    (5..7).map { col -> SceneCell(ox + col, oy + row, lit, isLight = true) }
                }
                sky + screen
            }

            // Dampf steigt ueber der Kanne auf und loest sich oben auf.
            Place.KITCHEN -> {
                val table = placements.firstOrNull { it.prop === TABLE } ?: return emptyList()
                val ox = originX(table, widthCells)
                val oy = originY(table, floorY)
                val rise = beat(phase, STEAM_TICKS) % 3
                listOf(
                    SceneCell(ox + 6, oy - 1 - rise, GLOW - rise * 500, isLight = true),
                    SceneCell(ox + 7, oy - 2 - rise, (GLOW - rise * 600).coerceAtLeast(0), isLight = true)
                )
            }

            // Lichtkegel unter dem Schirm, langsam pulsierend - nur bei eingeschalteter Lampe.
            // Ausgeschaltet bleibt allein die Silhouette stehen, und die Ecke wird spuerbar
            // dunkler: Erst dass das Ausschalten etwas WEGNIMMT, macht den Schalter zu einem
            // Schalter statt zu einer Geste ohne Folgen.
            Place.NOOK -> {
                if (!lampOn) return emptyList()
                val lamp = placements.firstOrNull { it.prop === LAMP } ?: return emptyList()
                val ox = originX(lamp, widthCells)
                val oy = originY(lamp, floorY)
                val pulse = if (beat(phase, LAMP_TICKS) % 4 < 2) GLOW else GLOW - 400
                // Nach unten hin schwaecher werdend: Licht, das nur EINE Zeile breit unter dem
                // Schirm sitzt, liest sich als weitere Silhouette, nicht als Schein.
                listOf(
                    SceneCell(ox, oy + 2, pulse / 3, isLight = true),
                    // Der Kern der Lampe ist das Glanzlicht des Zimmers.
                    SceneCell(ox + 1, oy + 2, HIGHLIGHT, isLight = true),
                    SceneCell(ox + 2, oy + 2, pulse / 3, isLight = true),
                    SceneCell(ox + 1, oy + 3, pulse / 2, isLight = true)
                ) + lightPool(ox + 1, floorY, radius = 5, peak = pulse)
            }

            // Draussen: eine Wolke zieht durchs Bild, nachts stattdessen Sterne.
            Place.PARK -> {
                val skyY = (floorY - 13).coerceAtLeast(0)
                if (dayPhase == PlayAmbientActivity.DayPhase.NIGHT) {
                    // Nachts steht der Boden tief (siehe [floorFraction]) und darueber ist Platz
                    // fuer einen richtigen Himmel: ein Sternbild aus sieben Sternen, jeder auf
                    // einem eigenen Takt. Im Gleichtakt blinkend saehen sie aus wie eine
                    // Leuchtreklame; unabhaengig voneinander wie ein Nachthimmel.
                    val sky = (floorY * 0.62f).toInt()
                    listOf(
                        // Die vier hellen bilden ein wiedererkennbares Muster - erst dadurch wird
                        // aus verstreuten Punkten ein Sternbild.
                        SceneCell(widthCells / 5, sky, twinkleOf(phase, 4, HIGHLIGHT), isLight = true),
                        SceneCell(widthCells / 5 + 5, sky - 4, twinkleOf(phase, 6, GLOW), isLight = true),
                        SceneCell(widthCells / 5 + 11, sky - 3, twinkleOf(phase, 5, GLOW), isLight = true),
                        SceneCell(widthCells / 5 + 15, sky + 2, twinkleOf(phase, 7, GLOW), isLight = true),
                        // ... die uebrigen streuen lockerer und schwaecher.
                        SceneCell(widthCells * 3 / 4, sky - 7, twinkleOf(phase, 9, GLOW / 2), isLight = true),
                        SceneCell(widthCells * 2 / 3, sky + 6, twinkleOf(phase, 11, GLOW / 2), isLight = true),
                        SceneCell(widthCells - 6, sky + 3, twinkleOf(phase, 13, GLOW / 2), isLight = true)
                    )
                } else {
                    // Bewusst breiter und heller als der erste Entwurf (drei Zellen auf
                    // Boden-Helligkeit): so klein und so schwach war das kein Woelkchen, sondern
                    // ein Staubkorn auf dem Display.
                    // Weit langsamer als der Grundtakt: eine Wolke, die je Zaehlschritt eine Zelle
                    // weiterspringt, hetzt ueber den Himmel. Der Anlauf von links ausserhalb des
                    // Bildes sorgt dafuer, dass sie hereinzieht statt an der Kante zu erscheinen.
                    val drift = beat(phase, CLOUD_TICKS) % (widthCells + CLOUD_WIDTH) - CLOUD_WIDTH
                    // Zweite Wolke: hoeher, langsamer, schwaecher. Zwei Wolken in
                    // unterschiedlichem Tempo lassen den Himmel tief wirken - eine allein sieht
                    // aus wie ein Gegenstand, der vorbeigeschoben wird.
                    val slowDrift = beat(phase, CLOUD_TICKS * 2) % (widthCells + CLOUD_WIDTH) - CLOUD_WIDTH
                    val highY = (skyY - 6).coerceAtLeast(0)
                    hLine(0, 5, skyY).map { (dx, y) -> SceneCell(drift + dx, y, FURNITURE) } +
                        hLine(1, 4, skyY - 1).map { (dx, y) -> SceneCell(drift + dx, y, FURNITURE) } +
                        hLine(0, 3, highY).map { (dx, y) -> SceneCell(slowDrift + dx, y, BACKDROP) } +
                        hLine(1, 2, highY - 1).map { (dx, y) -> SceneCell(slowDrift + dx, y, BACKDROP) }
                } + parkBird(phase, widthCells, floorY, dayPhase) +
                    shootingStar(phase, widthCells, floorY, dayPhase) +
                    parkLantern(placements, widthCells, floorY, dayPhase)
            }

            // Bad: ein Tropfen faellt in unregelmaessigen Abstaenden vom Hahn. Das eine Geraeusch,
            // das man in einem stillen Badezimmer hoert - hier als Bild.
            Place.BATH -> {
                val basin = placements.firstOrNull { it.prop === BASIN } ?: return emptyList()
                val ox = originX(basin, widthCells)
                val oy = originY(basin, floorY)
                val fall = beat(phase, DRIP_TICKS) % 6
                if (fall > 2) emptyList()
                else listOf(SceneCell(ox + 3, oy + 6 + fall, GLOW - 500, isLight = true))
            }

            // Arbeitsplatz: haengt von der Spezies ab und bringt seine Regung ueber die
            // Requisiten selbst mit (Lampe, Regal) - eine eigene reicht.
            Place.WORK -> {
                // Der Leuchtturm hat sein eigenes Licht: ein Strahl, der ueber den Bildrand
                // hinauszieht. Das ist die eine Arbeitsstelle, die ihre Regung nicht von einer
                // gewoehnlichen Lampe bezieht.
                val beacon = placements.firstOrNull { it.prop === BEACON }
                if (beacon != null) {
                    val bx = originX(beacon, widthCells)
                    val by = originY(beacon, floorY)
                    val sweep = beat(phase, BEACON_TICKS) % 4
                    val core = listOf(
                        SceneCell(bx + 3, by + 4, HIGHLIGHT, isLight = true),
                        SceneCell(bx + 4, by + 4, GLOW, isLight = true),
                        SceneCell(bx + 5, by + 4, GLOW, isLight = true)
                    )
                    // Der Strahl wandert nach rechts und wird dabei schwaecher.
                    val beam = (1..(4 + sweep * 4)).mapNotNull { d ->
                        val b = GLOW - d * 90
                        if (b <= 0) null else SceneCell(bx + 8 + d, by + 4, b, isLight = true)
                    }
                    return core + beam
                }
                val lamp = placements.firstOrNull { it.prop === LAMP } ?: return emptyList()
                if (!lampOn) return emptyList()
                val ox = originX(lamp, widthCells)
                val oy = originY(lamp, floorY)
                val pulse = if (beat(phase, LAMP_TICKS) % 4 < 2) GLOW else GLOW - 400
                listOf(
                    SceneCell(ox + 1, oy + 2, pulse, isLight = true)
                ) + lightPool(ox + 1, floorY, radius = 5, peak = pulse)
            }

            // Laden: ein Leuchtschild ueber der Tuer, das im Wechsel an- und ausgeht. Ein
            // vollstaendig unbewegter Raum liest sich als Standbild - und der Laden waere
            // ausgerechnet der einzige Ort ohne jede Regung gewesen.
            Place.SHOP -> {
                val signY = (floorY - 15).coerceAtLeast(0)
                val x = (widthCells * 0.62f).toInt()
                val lit = if (beat(phase, SIGN_TICKS) % 2 == 0) GLOW else GLOW - 900
                (0..3).map { SceneCell(x + it, signY, lit, isLight = true) }
            }

            // Wohnzimmer: der Fernseher flimmert, und sein Licht faellt in den Raum.
            Place.LIVING -> {
                if (!tvOn) return emptyList()
                val tv = placements.firstOrNull { it.prop === TV } ?: return emptyList()
                val ox = originX(tv, widthCells)
                val oy = originY(tv, floorY)
                // Drei Helligkeiten im Wechsel statt zwei: Ein Fernsehbild springt staerker als
                // ein Monitor, auf dem jemand arbeitet - daran erkennt man die beiden auseinander,
                // obwohl beide nur eine leuchtende Flaeche sind.
                // Gedaempfter als eine Lampe, obwohl es ein Bildschirm ist: Die Mattscheibe ist
                // mit rund fuenfundvierzig Zellen die groesste zusammenhaengende Lichtflaeche der
                // ganzen Welt. Auf voller Staerke uebertoente sie alles andere im Bild, den Avatar
                // eingeschlossen.
                val lit = when (beat(phase, TV_TICKS) % 3) {
                    0 -> GLOW - 500
                    1 -> GLOW - 1100
                    else -> GLOW - 800
                }
                (1..5).flatMap { row ->
                    (1..9).map { col -> SceneCell(ox + col, oy + row, lit, isLight = true) }
                } + lightPool(ox + 5, floorY, radius = 7, peak = lit - 600)
            }
        }
    }
}
