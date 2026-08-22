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

    /**
     * Die Orte, an denen sich der Avatar aufhalten kann.
     *
     * **Drei davon liegen draussen, und das ist eine bewusste Korrektur.** Lange gab es genau
     * einen Ort unter freiem Himmel, den Park - und weil der Tagesablauf ueberwiegend aus Wohnen,
     * Arbeiten und Essen besteht, war dieses Wesen praktisch nie draussen. Das faellt beim
     * Zuschauen sofort auf: Eine Welt, die nur aus Zimmern besteht, wirkt weniger wie ein Leben
     * als wie eine Wohnungsbesichtigung.
     *
     * [STREET] und [FOREST] beheben das nicht dadurch, dass sie noch zwei Kulissen hinzufuegen,
     * sondern dadurch, dass sie zwei fehlende ANLAESSE liefern: Die Strasse ist der Weg, der
     * bisher fehlte - wer zur Arbeit ging, verschwand in einem Zimmer und erschien im naechsten.
     * Der Wald ist das Gegenteil des Zuhauses und damit der einzige Ort, an dem "spazieren gehen"
     * mehr ist als "im Park hin und her laufen".
     */
    /**
     * [CRAFT] ist der einzige Ort, den es NUR EINMAL gibt und der trotzdem bei jeder Kreatur ein
     * anderer Ort ist: die eigene Ecke, in der sie ihrer Art nach etwas tut. Der Drache schmiedet,
     * der Schleim toepfert, der Wuestenfuchs graebt, die Eule spielt. Es ist der Gegenentwurf zum
     * Arbeitsplatz - dort geht sie einem BERUF nach, hier tut sie etwas, weil sie es ist.
     */
    enum class Place { BEDROOM, BATH, DESK, WORK, KITCHEN, NOOK, LIVING, CRAFT, PARK, SHOP, STREET, FOREST, MEADOW, CITY }

    /** Draussen gibt es keine Wand und keinen Zimmerboden - siehe [build]. */
    private val Place.isIndoors: Boolean
        get() = !isOutdoors(this)

    /**
     * Ob dieser Ort unter freiem Himmel liegt.
     *
     * Oeffentlich, weil auch ausserhalb der Kulisse danach gefragt wird - der Mond etwa steigt
     * ueber jedem Ort auf, an dem man ihn sehen koennte, und nicht nur ueber dem Park (siehe
     * DockScreen). Wer einen neuen Ort ergaenzt, traegt ihn hier ein und bekommt Himmel, Wolken,
     * Sterne und Mond, ohne an vier Stellen nachzuziehen.
     */
    fun isOutdoors(place: Place): Boolean =
        place == Place.PARK || place == Place.STREET || place == Place.FOREST ||
            place == Place.MEADOW || place == Place.CITY

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
        //
        // Auf der STRASSE erst recht: Sie ist der Ort, an dem man einander begegnet, ohne dass es
        // etwas zu bedeuten haette. Im WALD dagegen NICHT - dort jemandem zu begegnen ist das
        // Gegenteil dessen, wofuer man in den Wald geht, und ein Fremder zwischen den Baeumen
        // wirkt eher beunruhigend als belebt.
        // Die STADT gehoert dazu wie die Strasse - ein Platz, an dem Leute ohnehin unterwegs sind.
        Place.PARK, Place.SHOP, Place.LIVING, Place.WORK, Place.STREET, Place.CITY -> true
        // Die eigene Ecke ([Place.CRAFT]) ausdruecklich NICHT: Wer dort sitzt, hat sich
        // zurueckgezogen. Ein Fremder, der einem beim Toepfern zusieht, ist das Gegenteil davon.
        //
        // Die WIESE genauso wenig wie der Wald: eine stille Freiflaeche zum Ausspannen, kein
        // Treffpunkt - dafuer gibt es bereits Park und Stadt.
        Place.BEDROOM, Place.BATH, Place.DESK, Place.KITCHEN, Place.NOOK, Place.CRAFT,
        Place.FOREST, Place.MEADOW -> false
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
    enum class Station { BED, SEAT, DESK, TABLE, FRIDGE, BOOKSHELF, LAMP, TV, BENCH, DOOR, RACK, CHECKOUT, TUB, BASIN, WORKPLACE, CRAFT }

    /** Aufloesung eines [Station] in Szenen-Zellen: waagerechte Mitte und Aufsetzzeile. */
    data class SceneSpot(val centerX: Int, val groundY: Int)

    /**
     * Die Form zu einer Erwerbung.
     *
     * Als Funktion und nicht als Feld am Enum: [Prop] ist eine interne Angelegenheit dieser Datei
     * und soll es bleiben - ein oeffentliches Enum, das sie mit sich traegt, wuerde sie nach
     * aussen tragen.
     */
    private fun propOf(acquisition: Acquisition): Prop = when (acquisition) {
        Acquisition.BACKPACK -> PROP_BACKPACK
        Acquisition.WALLMAP -> PROP_WALLMAP
        Acquisition.SCOOTER -> PROP_SCOOTER
        Acquisition.FEEDBOWL -> PROP_FEEDBOWL
        Acquisition.PETBASKET -> PROP_PETBASKET
        Acquisition.SLEEPING_PET -> PROP_SLEEPING_PET
        Acquisition.BLANKET -> PROP_BLANKET
        Acquisition.MOBILE -> PROP_MOBILE
        Acquisition.STARJAR -> PROP_STARJAR
        Acquisition.TOOLBOX -> PROP_TOOLBOX
        Acquisition.CONTRAPTION -> PROP_CONTRAPTION
        Acquisition.SIGNALRIG -> PROP_SIGNALRIG
    }

    /**
     * **Was sich im Lauf der Zeit ANSAMMELT** - Dinge, die zur Wohnung dazukommen, weil das Wesen
     * sich entwickelt hat (siehe [com.notime.glyphsim.ui.PlayPath]).
     *
     * **Der Unterschied zur uebrigen Einrichtung ist der Zeitpunkt.** Alles andere steht von
     * Anfang an da: Es beschreibt, WER hier wohnt. Diese Dinge beschreiben, was seither passiert
     * ist - ein Rucksack, weil er losgeht; eine Werkzeugkiste, weil er baut; ein Napf, weil er
     * sich um etwas kuemmert. Ein Zimmer, das am ersten Tag genauso aussieht wie nach zwei
     * Monaten, kann von diesen zwei Monaten nichts erzaehlen.
     *
     * **Verteilt auf drei Raeume, nicht gestapelt in einem.** Das erste Stueck steht in seiner
     * eigenen Ecke (dort faengt ein Hobby an), das zweite im Wohnzimmer (dort zeigt man, was man
     * macht), das dritte im Schlafzimmer (dort steht, was einem gehoert). Nebenbei loest das ein
     * handfestes Problem: Drei zusaetzliche Gegenstaende in EINEM Zimmer waeren auf einem schmalen
     * Bild nicht mehr unterzubringen.
     */
    enum class Acquisition(
        val place: Place,
        internal val anchorX: Float,
        internal val liftCells: Int = 0
    ) {
        // ---- Der Aufbrecher: unterwegs sein ----
        BACKPACK(Place.CRAFT, 0.44f),
        // 0,72 und nicht 0,5: Dort haengt bereits der Wandschmuck der Wohnung (siehe [Home.wall]),
        // und zwei Bilder an derselben Stelle heben einander auf - das zweite faellt weg.
        // Eine Zelle hoeher als der uebrige Wandschmuck: Fennecs Ofen ist zwoelf Zellen hoch und
        // reicht damit als einziges Geraet bis in die Reihe, in der sonst Bilder haengen.
        WALLMAP(Place.LIVING, 0.72f, liftCells = 12),
        SCOOTER(Place.BEDROOM, 0.58f),

        // ---- Der Fuersorgliche: sich um etwas kuemmern ----
        FEEDBOWL(Place.CRAFT, 0.44f),
        PETBASKET(Place.LIVING, 0.46f),
        SLEEPING_PET(Place.BEDROOM, 0.58f),

        // ---- Der Stille: sammeln, was Ruhe gibt ----
        BLANKET(Place.CRAFT, 0.44f),
        MOBILE(Place.LIVING, 0.72f, liftCells = 12),
        STARJAR(Place.BEDROOM, 0.58f),

        // ---- Der Macher: Technik ----
        TOOLBOX(Place.CRAFT, 0.44f),
        CONTRAPTION(Place.LIVING, 0.46f),
        SIGNALRIG(Place.BEDROOM, 0.58f)
    }

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
        // Ueber dieselbe Anordnung wie die Kulisse: Ist ein Moebel auf schmalem Bild ausgewichen
        // (siehe [fitting]), muss die Aufsetzstelle mitwandern - sonst setzt sich die Figur neben
        // den Sessel statt hinein.
        val placement = fitting(placementsFor(place, species), widthCells, floorY)
            .firstOrNull { it.station == station } ?: return null
        val spot = placement.prop.useSpot ?: return null
        return SceneSpot(
            centerX = originX(placement, widthCells) + spot.first,
            groundY = originY(placement, floorY) + spot.second
        )
    }

    /**
     * **Schmaler als so viele Spalten wird die Welt nie gezeichnet.**
     *
     * Wie viele Spalten zur Verfuegung stehen, ergibt sich aus Bildschirmbreite geteilt durch
     * Zellgroesse - und die Zellgroesse haengt daran, wie gross der Nutzer seine Uhr gezogen hat.
     * Auf einem kleinen Geraet mit weit aufgezogener Uhr blieben sonst rund dreissig Spalten, und
     * in dreissig Spalten passen Sofa, Fernseher und Tuer schlicht nicht nebeneinander: 13 + 11 +
     * 7 sind schon 31.
     *
     * Statt jedes Zimmer fuer diesen Extremfall zu verbiegen, verkleinert sich in dieser Lage die
     * Zelle (siehe DockScreen) - die Figur wird ein wenig kleiner, das Zimmer bleibt vollstaendig.
     * Das ist die richtige Reihenfolge: Lieber ein etwas kleinerer Bewohner in einer heilen
     * Wohnung als ein grosser in einer, in der die Moebel ineinanderstecken.
     */
    const val MIN_SCENE_CELLS = 40

    /**
     * **So breit wird ein ZIMMER hoechstens - egal, wie breit das Bild ist.**
     *
     * Gemeldet als "im Querformat steht alles sehr weit auseinander", und genau so war es: Die
     * Verankerung ist ein Bruchteil der verfuegbaren Breite. Im Hochformat sind das rund vierzig
     * Spalten, quer gedreht ueber achtzig - dieselbe Einrichtung zieht sich dann auf die doppelte
     * Strecke, und zwischen Bett und Nachttisch klafft ein halber Bildschirm. Kein einzelner Wert
     * ist dabei falsch; es ist die Vorschrift selbst, die bei ungewohnten Massen auseinanderlaeuft.
     *
     * Die Antwort ist nicht, jede Zahl fuer das Querformat noch einmal zu bestimmen, sondern
     * anzuerkennen, dass ein Zimmer eine GROESSE hat: Ein Schlafzimmer wird nicht doppelt so
     * gross, weil man das Telefon dreht. Ueber diese Breite hinaus waechst der Raum nicht mehr
     * mit, sondern steht mittig - Boden und Decke laufen weiter durchs ganze Bild, sodass es kein
     * Kaesteln gibt, aber die Einrichtung bleibt beieinander.
     *
     * Der Wert liegt bewusst nah am Hochformat: Diese Zimmer sind FUER diese Breite eingerichtet
     * worden, jede Anordnung ist bei etwa vierzig bis sechzig Spalten geprueft.
     */
    const val MAX_ROOM_CELLS = 60

    /** Wie breit das Zimmer bei dieser Bildbreite ist - siehe [MAX_ROOM_CELLS]. */
    fun roomWidth(widthCells: Int): Int = widthCells.coerceAtMost(MAX_ROOM_CELLS)

    /**
     * Rechnet eine Verankerung IM ZIMMER auf einen Bruchteil des ganzen BILDES um.
     *
     * Gebraucht ueberall dort, wo sich nicht die Kulisse, sondern die Figur an einem Bruchteil
     * ausrichtet - ihr Ruheplatz und das Umherlaufen (siehe [RoutineStep.Stroll]). Ohne diese
     * Umrechnung liefe sie im Querformat quer ueber den Bildschirm, waehrend ihre Moebel in der
     * Mitte stuenden: Sie waere staendig woanders als ihre Welt.
     */
    fun screenFraction(roomAnchor: Float, widthCells: Int): Float {
        if (widthCells <= 0) return roomAnchor
        val room = roomWidth(widthCells)
        if (room >= widthCells) return roomAnchor
        val shift = (widthCells - room) / 2f
        return ((shift + room * roomAnchor.coerceIn(0f, 1f)) / widthCells).coerceIn(0f, 1f)
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
        // Der Wald hat den HOECHSTEN Horizont von allen Orten draussen - und das ist genau
        // umgekehrt zur Weite des Parks. Im Wald sieht man keinen Himmel; die Baeume stehen dicht
        // und nah. Ein tief gelegter Horizont wuerde daraus eine Lichtung machen.
        place == Place.FOREST -> 0.78f
        place.isIndoors -> 0.80f
        dayPhase == PlayAmbientActivity.DayPhase.NIGHT -> 0.93f
        dayPhase == PlayAmbientActivity.DayPhase.EVENING -> 0.88f
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
        val placement = fitting(placementsFor(place, species), widthCells, floorY)
            .firstOrNull { it.station == station } ?: return emptyList()
        val ox = originX(placement, widthCells)
        val oy = originY(placement, floorY)
        return placement.prop.art.map { (x, y) -> (ox + x) to (oy + y) }
    }

    /**
     * ALLE Requisiten eines Ortes mit den Zellen, die sie belegen - auch die ohne Station.
     *
     * Oeffentlich fuer die Kompositions-Pruefung, und zwar aus einem konkreten Anlass: Die
     * Ueberschneidungs-Pruefung sah bisher nur Requisiten MIT Station und liess die Tuer
     * ausdruecklich aus. Genau dort stand dann in jedem Arbeitsraum eine Lampe mitten im
     * Tuerrahmen, und ein Fenster ragte hinein - unbemerkt, weil beide Beteiligten aus der
     * Pruefung herausfielen. Was nicht angesehen wird, wird eben auch nicht gefunden.
     *
     * Die Beschriftung ist der Stationsname, sonst die laufende Nummer - sie dient nur dazu, in
     * einer Fehlermeldung sagen zu koennen, WELCHE zwei Dinge ineinanderstehen.
     */
    fun propFootprints(
        place: Place,
        widthCells: Int,
        floorY: Int,
        species: AvatarSpecies = AvatarSpecies.PUFFLING,
        acquisitions: Set<Acquisition> = emptySet()
    ): List<Pair<String, List<Pair<Int, Int>>>> =
        fitting(placementsFor(place, species, acquisitions), widthCells, floorY)
            // OHNE den Hintergrund (siehe [Placement.behind]): Eine Laterne vor einer Hausfassade
            // ist keine Ueberschneidung, sondern der Zweck der Sache. Wuerde die Pruefung sie
            // melden, muesste man ihr die Tiefe wieder austreiben.
            .filterNot { it.behind }
            .mapIndexed { index, placement ->
                val ox = originX(placement, widthCells)
                val oy = originY(placement, floorY)
                val label = placement.station?.name ?: "Requisite $index"
                label to placement.prop.art.map { (x, y) -> (ox + x) to (oy + y) }
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
        val placement = fitting(placementsFor(place, species), widthCells, floorY)
            .firstOrNull { it.station == station } ?: return emptyList()
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
        // **Arbeiten faengt auf der STRASSE an, nicht am Arbeitsplatz.**
        //
        // Vorher verschwand die Figur in einem Zimmer und erschien im naechsten - der Arbeitsweg,
        // also der einzige Teil des Arbeitstages, der ueberhaupt draussen stattfindet, kam
        // schlicht nicht vor. Der Ablauf setzt jetzt vor der Tuer an und geht von dort hinein
        // (siehe PlayRoutines.allFor(WORK)); den Weg zurueck geht er am Ende ebenso.
        AnimationType.WORK -> Place.STREET
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
        AnimationType.BOOK, AnimationType.MINDFULNESS -> Place.NOOK
        // **Etwas machen hat jetzt einen eigenen Ort.** Vorher fand es in der Leseecke statt,
        // zwischen Buch und Stille - drei Themen an einem Platz, und ausgerechnet das taetigste
        // davon hatte dort nichts, womit es haette umgehen koennen.
        AnimationType.CREATIVITY -> Place.CRAFT
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
        // Rechts von der Werkbank: Die Figur arbeitet an ihr (useSpot links davon), steht in Ruhe
        // aber daneben - sonst haette sie nie eine andere Haltung zu ihrem eigenen Werkstueck.
        Place.CRAFT -> 0.42f
        Place.PARK -> 0.20f
        Place.SHOP -> 0.34f
        Place.BATH -> 0.42f
        Place.WORK -> 0.20f
        // Auf der Strasse weit LINKS: Sie ist ein Weg, kein Aufenthaltsort - wer sie betritt, ist
        // unterwegs, und ein Anfang am Rand macht daraus eine Strecke statt einer Buehne.
        Place.STREET -> 0.08f
        Place.FOREST -> 0.06f
        // Wie der Park: eine offene Flaeche, kein Durchgang - die Figur darf mittig stehen.
        Place.MEADOW -> 0.20f
        // Wie die Strasse, aus demselben Grund: auch die Stadt ist ein Weg, kein Aufenthaltsort.
        Place.CITY -> 0.08f
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
        species: AvatarSpecies = AvatarSpecies.PUFFLING,
        /**
         * Was sich im Lauf der Zeit angesammelt hat (siehe [Acquisition]) - leer heisst: eine
         * Wohnung wie am ersten Tag.
         */
        acquisitions: Set<Acquisition> = emptySet()
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
        cells += groundDetail(place, widthCells, floorY, species)

        for (placement in fitting(placementsFor(place, species, acquisitions), widthCells, floorY)) {
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

        cells += ambient(place, phase, widthCells, floorY, dayPhase, lampOn, tvOn, species, acquisitions)
        cells += housePet(place, phase, widthCells, floorY)
        cells += weather(
            place, fitting(placementsFor(place, species, acquisitions), widthCells, floorY),
            phase, widthCells, floorY
        )

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
        val usedGlow: List<Pair<Int, Int>> = emptyList(),
        /**
         * Wo bei einer LEUCHTE der Kern sitzt - in den lokalen Koordinaten der Requisite.
         *
         * **Warum das jetzt an der Requisite haengt und nicht mehr in [ambient].** Die
         * Umgebungsanimation suchte ihre Lichtquelle bis hierher am Gegenstand selbst
         * (`prop === LAMP`) und rechnete mit dessen Massen. Das ging genau so lange gut, wie ALLE
         * gleich wohnten: Sobald der Drache eine Feuerschale und der Schleim einen Leuchtpilz
         * bekommt, findet diese Abfrage nichts mehr - und die Leseecke bliebe fuer vier von sechs
         * Kreaturen unbeleuchtet, ohne dass irgendwo ein Fehler auftauchte. Die Leuchte weiss
         * jetzt selbst, wo ihr Licht sitzt; das Zimmer muss davon nichts wissen.
         */
        val lightAt: Pair<Int, Int>? = null,
        /**
         * Die leuchtende FLAECHE eines Geraets (Mattscheibe, Monitor, Feuerstelle) - als Umriss
         * ausgespart gezeichnet und erst von [ambient] gefuellt, damit das Licht eine eigene
         * Ebene bleibt und heller sein darf als das Geraet drumherum.
         *
         * Aus demselben Grund an der Requisite wie [lightAt]: Ein Kamin hat seine Flammen an
         * anderer Stelle als ein Fernseher seine Mattscheibe.
         */
        val screenArea: List<Pair<Int, Int>> = emptyList(),
        /**
         * Wo auf einer Requisite das GEFAESS steht, ueber dem Dampf aufsteigt (siehe [ambient]).
         *
         * Aus demselben Anlass wie [lightAt] hier gelandet: Solange alle am selben Tisch assen,
         * konnte die Umgebungsanimation mit festen Massen rechnen. Sechs verschiedene Tische
         * koennen ihren Topf nicht alle an derselben Stelle tragen - und muessten es, wenn der
         * Dampf weiter von aussen bestimmt wuerde. Dann waeren die Tische nur noch verschiedene
         * Untergestelle unter derselben Platte, und genau das war der Grund, warum zwei Kuechen
         * einander nicht loslassen wollten.
         */
        val vaporAt: Pair<Int, Int>? = null
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
        val station: Station? = null,
        /**
         * Wieviele Spalten am rechten Rand fuer diese Requisite gesperrt sind - siehe [besideDoor].
         * Die Verankerung rechnet dann nur noch mit der Flaeche links davon.
         */
        val keepClearRight: Int = 0,
        /**
         * Dasselbe am linken Rand - fuer Requisiten, die neben einem grossen Moebel stehen, das
         * bei [anchorX] 0 verankert ist (Wanne, Kuehlschrank, Bett).
         *
         * Ohne das rutscht der Nachbar auf schmalen Bildern hinein: [anchorX] ist ein Bruchteil
         * der GESAMTEN Breite, und wenn davon ein Drittel bereits von der Wanne belegt ist, liegt
         * "bei zwei Dritteln" eben noch in der Wanne. Mit der Sperre bezieht sich der Bruchteil
         * auf den Platz, der tatsaechlich frei ist - das Becken steht dann bei jeder Breite
         * zwischen Wanne und Tuer statt mal daneben und mal darin.
         */
        val keepClearLeft: Int = 0,
        /**
         * Nachtraegliche Verschiebung, die [fitting] auf schmalen Bildern setzt, damit sich zwei
         * Stationen nicht beruehren. In der Einrichtung selbst wird das nie angegeben.
         */
        val offsetX: Int = 0,
        /**
         * **Steht WEIT hinten** - Hausfassade, Baum am Waldrand.
         *
         * Fuer alles Uebrige gilt: Zwei Dinge duerfen nicht ineinanderstehen, denn nebeneinander
         * gezeichnete Moebel liegen auf demselben Boden. Hintergrund ist aber gerade das, wovor
         * etwas steht. Eine Laterne vor einem Haus ist keine Ueberschneidung, sondern Tiefe - und
         * ohne diesen Unterschied hat die Anordnung genau das getan, was sie tun musste: Sie hat
         * zwei von drei Haeusern der Strasse als Kollision aussortiert, bis von der Strasse nur
         * noch eine Bank und eine Laterne im Nichts uebrig waren.
         */
        val behind: Boolean = false
    )

    /**
     * **Die Tuer bekommt ihren Platz zugeteilt, statt ihn sich zu teilen.**
     *
     * Die Tuer steht immer ganz rechts ([Placement.anchorX] = 1). Weil [anchorX] ein Bruchteil des
     * FREIEN Platzes ist, landet aber alles ab etwa 0,85 ebenfalls dort - eine Lampe bei 0,92 stand
     * dadurch mitten im Tuerrahmen, und zwar in jeder Backstube, jeder Sternwarte und jeder
     * Turnhalle gleichzeitig. Das war kein Vertippen an einer Stelle, sondern eine Luecke in der
     * Anordnung: Nichts hinderte eine Requisite daran, sich dorthin zu stellen.
     *
     * Deshalb wird die Sperrzone hier einmal zentral vergeben, statt an sechs Arbeitsplaetzen
     * einzeln Zahlen nachzujustieren - beim naechsten neuen Raum gilt sie von selbst mit.
     */
    private fun besideDoor(placements: List<Placement>): List<Placement> {
        val reserved = DOOR.width + DOOR_MARGIN
        return placements.map { if (it.prop === DOOR) it else it.copy(keepClearRight = reserved) } +
            Placement(DOOR, anchorX = 1f, brightness = BACKDROP, station = Station.DOOR)
    }

    /** Luft zwischen Tuerrahmen und dem naechsten Moebel - ohne sie stossen beide aneinander. */
    private const val DOOR_MARGIN = 2

    private fun originX(placement: Placement, widthCells: Int): Int {
        // Die Einrichtung steht im ZIMMER, nicht auf dem ganzen Bild - siehe [roomWidth].
        val room = roomWidth(widthCells)
        val shift = (widthCells - room) / 2
        val left = placement.keepClearLeft
        val right = room - placement.keepClearRight
        val span = (right - left).coerceAtLeast(placement.prop.width)
        val x = shift + left +
            ((span - placement.prop.width).coerceAtLeast(0) * placement.anchorX).roundToInt() +
            placement.offsetX
        // Bei extremer Enge darf die Sperre die Requisite nicht aus dem Bild schieben.
        return x.coerceIn(0, (widthCells - placement.prop.width).coerceAtLeast(0))
    }

    private fun originY(placement: Placement, floorY: Int): Int =
        floorY - placement.prop.height - placement.liftCells

    /**
     * **Auf einem schmalen Bild faellt Beiwerk weg, statt sich zu ueberlagern.**
     *
     * Die Zahl der Spalten ist nicht frei waehlbar: Sie ergibt sich aus Bildschirmbreite geteilt
     * durch Zellgroesse, und die Zellgroesse haengt daran, wie gross der Nutzer seine Uhr gezogen
     * hat. Zwischen kleinem Geraet mit grosser Uhr und grossem Geraet mit kleiner Uhr liegt rund
     * das Dreifache - ein Schlafzimmer, das bei 60 Spalten luftig steht, hat bei 30 schlicht nicht
     * genug Wand fuer Bett UND Nachttisch.
     *
     * Zwei Antworten, je nachdem, worum es geht:
     *
     * **Beiwerk faellt weg.** Zwei ineinandergeschobene Moebel liest niemand mehr als Moebel, ein
     * fehlender Nachttisch faellt dagegen gar nicht auf. Was keine Station traegt, weicht deshalb
     * als erstes.
     *
     * **Stationen ruecken zusammen.** Weglassen kommt hier nicht in Frage - liefe die Figur zu
     * einem Bett, das es auf diesem Geraet nicht gibt, waere das ein echter Fehler. Sie werden
     * stattdessen von links nach rechts aufgereiht und notfalls ein Stueck nach rechts geschoben,
     * bis sie sich nicht mehr beruehren. Das verschiebt die Einrichtung leicht gegenueber ihren
     * gedachten Ankern, aber nur dort, wo die Anker ohnehin nicht mehr alle unterzubringen sind.
     *
     * Bleibt selbst danach eine Ueberschneidung, wird sie NICHT zugedeckt: Dann ist der Raum fuer
     * dieses Bild schlicht zu voll eingerichtet, und SceneCompositionTest soll das melden.
     */
    private fun fitting(placements: List<Placement>, widthCells: Int, floorY: Int): List<Placement> {
        if (widthCells <= 0) return placements
        // Ueber Stellen-Nummern statt ueber die Objekte selbst: Zwei gleich eingerichtete
        // Requisiten (zwei Pflanzen, zwei Regale) waeren als Werte nicht unterscheidbar.
        val result = placements.toMutableList()
        val dropped = HashSet<Int>()
        val taken = HashSet<Pair<Int, Int>>()

        fun cellsOf(placement: Placement): List<Pair<Int, Int>> {
            val ox = originX(placement, widthCells)
            val oy = originY(placement, floorY)
            return placement.prop.art.map { (x, y) -> (ox + x) to (oy + y) }
        }

        // Die TUER ZUERST, dann die uebrigen Stationen von links nach rechts. Die Reihenfolge ist
        // nicht beliebig: Die Tuer ist die einzige, die nicht ausweichen darf - kaeme sie zuletzt,
        // koennte sie die anderen auch nicht mehr abhalten, und der Fernseher wiche geradewegs in
        // den Tuerrahmen hinein.
        val stations = placements.indices
            .filter { placements[it].station != null }
            .sortedWith(compareBy(
                { if (placements[it].prop === DOOR) 0 else 1 },
                { originX(placements[it], widthCells) }
            ))

        for (index in stations) {
            val original = placements[index]
            var placement = original
            if (original.prop !== DOOR && cellsOf(original).any { it in taken }) {
                // Abwechselnd nach rechts und nach links gesucht: Nur nach rechts zu schieben
                // wuerde alles gegen die Tuer draengen, nur nach links gegen den Bildrand.
                val room = (widthCells - original.prop.width).coerceAtLeast(0)
                for (step in 1..room) {
                    val right = original.copy(offsetX = step)
                    if (cellsOf(right).none { it in taken }) { placement = right; break }
                    val left = original.copy(offsetX = -step)
                    if (cellsOf(left).none { it in taken }) { placement = left; break }
                }
            }
            result[index] = placement
            taken.addAll(cellsOf(placement))
        }

        // Danach das Beiwerk: Es bekommt, was uebrig ist, und faellt sonst weg. Was weit hinten
        // steht, ist davon ausgenommen - davor zu stehen ist ja gerade seine Aufgabe.
        for (index in placements.indices) {
            val placement = placements[index]
            if (placement.station != null || placement.behind) continue
            val cells = cellsOf(placement)
            if (cells.any { it in taken }) dropped.add(index) else taken.addAll(cells)
        }

        // In der urspruenglichen Reihenfolge zurueck: Sie bestimmt, was vor was gezeichnet wird.
        return result.filterIndexed { index, _ -> index !in dropped }
    }

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
     * Die Einrichtung eines Ortes.
     *
     * **Jeder WOHNRAUM haengt inzwischen an der Spezies** (siehe [Home]) - vorher waren es nur
     * Schlafzimmer, Arbeitsplatz und das Draussen, und die uebrigen fuenf Zimmer sahen bei allen
     * sechs Kreaturen bis auf die Zelle genau gleich aus. Die ANORDNUNG bleibt dabei ueberall
     * dieselbe: Nur so ist gesichert, dass jeder Ablauf in jeder Wohnung aufgeht und dass eine
     * Einrichtung, die bei einer Kreatur passt, auch bei den anderen nicht ineinandersteht.
     *
     * Geteilt bleiben Laden, Strasse und Wald. Das ist der Gegenpol und ebenso wichtig: Wenn
     * jeder sein eigenes Zuhause hat, braucht es Orte, an denen alle dasselbe sehen - sonst waeren
     * es sechs Welten nebeneinander statt einer mit sechs Bewohnern.
     */
    private fun placementsFor(
        place: Place,
        species: AvatarSpecies = AvatarSpecies.PUFFLING,
        acquisitions: Set<Acquisition> = emptySet()
    ): List<Placement> {
        val home = homeOf(species)
        // **Angehaengt und nicht eingeflochten** - und damit als Letzte in der Reihenfolge, in der
        // [fitting] Beiwerk verteilt. Das ist Absicht: Wird es eng, weicht das Neue und nicht die
        // Einrichtung. Ein Zimmer, in dem der Rucksack den Sessel verdraengt, waere schlechter als
        // eines ohne Rucksack.
        val furnished = furnishing(place, home, species)
        // **Dieselbe Sperrzone wie die Einrichtung** (siehe [besideDoor]) - und daran haengt mehr,
        // als es aussieht: Die Verankerung ist ein Bruchteil des FREIEN Platzes. Ohne die Sperre
        // rechnet ein neu erworbenes Stueck seinen Anker gegen die volle Breite, die Moebel
        // daneben aber gegen die Breite ohne Tuerzone - dieselbe Zahl bedeutet dann zwei
        // verschiedene Stellen. Beim ersten Versuch landete die Apparatur dadurch im Fernseher
        // und fiel als Beiwerk lautlos weg: Der Nutzer haette eine Stufe erreicht und nichts
        // gesehen.
        val reserved = if (furnished.any { it.station == Station.DOOR }) DOOR.width + DOOR_MARGIN else 0
        val gathered = acquisitions.filter { it.place == place }.map {
            Placement(
                propOf(it),
                anchorX = it.anchorX,
                liftCells = it.liftCells,
                keepClearRight = reserved
            )
        }
        // **Das Erworbene steht VOR dem Beiwerk in der Reihe** - und damit vor ihm beim Verteilen
        // (siehe [fitting], das Beiwerk in Reihenfolge bedient und den Rest weglaesst).
        //
        // Die Bedeutung ist eine andere: Eine zweite Topfpflanze ist Ausstattung, der Roller im
        // Schlafzimmer ist das Ergebnis von Wochen. Wird es eng, weicht die Pflanze. Umgekehrt
        // waere der Nutzer wochenlang auf eine Stufe zugegangen und haette nichts gesehen, weil
        // an der betreffenden Stelle zufaellig schon Grün stand.
        //
        // Die Stationen bleiben unangetastet ganz vorn: An sie geht die Figur heran, sie duerfen
        // nie ausfallen.
        return furnished.filter { it.station != null } +
            gathered +
            furnished.filter { it.station == null }
    }

    private fun furnishing(
        place: Place,
        home: Home,
        species: AvatarSpecies
    ): List<Placement> {
        return when (place) {
        // Das Schlafzimmer ist der persoenlichste Raum: Wie jemand schlaeft, sagt mehr ueber ihn
        // als das, was an seiner Wand haengt.
        Place.BEDROOM -> bedroomPlacements(species)

        Place.BATH -> besideDoor(listOf(
            Placement(home.bath, anchorX = 0f, station = Station.TUB),
            // Rechts NEBEN der Wanne verankert, nicht bei einem Bruchteil des ganzen Raumes: Bei
            // 30 Spalten (kleines Geraet, gross gezogene Uhr) stand das Becken sonst in der Wanne.
            Placement(
                BASIN, anchorX = 0.5f, station = Station.BASIN,
                keepClearLeft = home.bath.width + 2
            ),
            Placement(home.wall, anchorX = 0.5f, liftCells = 11, brightness = BACKDROP),
            // Ohne diese beiden bestuende das Bad aus Wanne und Becken - und weil das Becken bei
            // allen dasselbe ist, waeren zwei Baeder zu drei Vierteln dasselbe Bild. Gerechnet
            // wurde das: Wanne und Becken allein machten bei zwei Kreaturen bis zu 79 % derselben
            // Zellen aus.
            Placement(home.light, anchorX = 0.34f),
            Placement(home.accent, anchorX = 0.9f)
        ))
        Place.WORK -> besideDoor(workPlacements(species))
        Place.DESK -> besideDoor(listOf(
            Placement(WINDOW, anchorX = 0f, liftCells = 8, brightness = BACKDROP),
            // Die eigene Leuchte OHNE Station: Am Schreibtisch wird sie nicht geschaltet, sie
            // brennt einfach. Nachts ist sie trotzdem das, was diesen Raum von den fuenf anderen
            // Schreibtischen unterscheidet - Licht bleibt stehen, wenn der Raum zurueckweicht.
            Placement(home.light, anchorX = 0.30f),
            // Das Fenster ist in JEDEM Arbeitszimmer dasselbe - es ist der Blick nach draussen,
            // und der gehoert dem Zimmer, nicht dem Bewohner (an ihm haengen Mond, Sonne und der
            // Regen, den man von drinnen sieht). Dafuer haengt daneben, was sonst auch an seinen
            // Waenden haengt.
            Placement(home.wall, anchorX = 0.50f, liftCells = 11, brightness = BACKDROP),
            Placement(home.desk, anchorX = 0.72f, station = Station.DESK),
            Placement(home.accent, anchorX = 0.88f)
        ))
        Place.KITCHEN -> besideDoor(listOf(
            // Der Kuehlschrank ist der eine Gegenstand, den hier alle teilen - ein Geraet ohne
            // Charakter, und sechs verschiedene zu zeichnen waere Arbeit, die niemand bemerkt.
            // Alles andere gehoert der Kreatur.
            Placement(FRIDGE, anchorX = 0f, station = Station.FRIDGE),
            // **Die Anker sind neu verteilt, weil die Kuechenzeile bisher gar nicht zu sehen war.**
            // Sie stand bei 0,50 und reichte damit bis in den Tisch hinein - und weil sie keine
            // Station traegt, wich sie jedes Mal (siehe [fitting]): Das groesste Moebelstueck der
            // Wohnung fiel bei ueblichen Bildbreiten stillschweigend weg, und uebrig blieb eine
            // Kueche aus Kuehlschrank, Regal und Tisch. Jetzt hat jedes Stueck seinen Streifen:
            // Kuehlschrank ganz links, Zeile daneben, Kleinzeug in der Luecke, Tisch rechts.
            Placement(home.counter, anchorX = 0.24f),
            Placement(home.larder, anchorX = 0.52f, liftCells = SHELF_LIFT, brightness = BACKDROP),
            Placement(home.accent, anchorX = 0.62f),
            Placement(home.table, anchorX = 0.94f, station = Station.TABLE)
        ))
        Place.NOOK -> besideDoor(listOf(
            Placement(home.seat, anchorX = 0.08f, station = Station.SEAT),
            Placement(home.wall, anchorX = 0f, liftCells = 10, brightness = BACKDROP),
            Placement(home.shelf, anchorX = 0.60f, station = Station.BOOKSHELF),
            Placement(home.light, anchorX = 0.82f, station = Station.LAMP)
        ))
        // **Die eigene Ecke.** Ein Raum aus vier Stuecken, von denen drei ohnehin schon ihr
        // gehoeren (Wandschmuck, Leuchte, Kleinzeug) - das vierte ist die eigentliche Aussage.
        Place.CRAFT -> besideDoor(listOf(
            Placement(home.craft, anchorX = 0.10f, station = Station.CRAFT),
            Placement(home.wall, anchorX = 0.5f, liftCells = 11, brightness = BACKDROP),
            Placement(home.accent, anchorX = 0.60f),
            // Die Leuchte ganz aussen, das Kleinzeug in die Mitte - und nicht umgekehrt. Beim
            // ersten Entwurf stand die Figur an ihrem Ruheplatz genau IN der Leuchte und loeschte
            // sie aus. Vor einem Krug zu stehen ist Tiefe, vor einer Lichtquelle ein Verlust.
            Placement(home.light, anchorX = 0.94f)
        ))
        Place.LIVING -> besideDoor(listOf(
            Placement(home.couch, anchorX = 0.02f, station = Station.SEAT),
            Placement(home.wall, anchorX = 0.34f, liftCells = 11, brightness = BACKDROP),
            Placement(home.accent, anchorX = 0.62f),
            Placement(home.screen, anchorX = 0.78f, station = Station.TV)
        ))
        Place.SHOP -> besideDoor(listOf(
            Placement(RACK, anchorX = 0.04f, station = Station.RACK),
            Placement(RACK, anchorX = 0.34f),
            Placement(SHELF, anchorX = 0.66f, liftCells = SHELF_LIFT, brightness = BACKDROP),
            Placement(CHECKOUT, anchorX = 0.72f, station = Station.CHECKOUT)
        ))
        // **Das Draussen, das IHM gehoert** - siehe [habitatPlacements].
        Place.PARK -> habitatPlacements(species)

        // Die STRASSE: Haeuser als Hintergrund, davor Laterne und Bank.
        //
        // Die Fassaden stehen auf BACKDROP, also deutlich dunkler als alles davor. Das ist hier
        // nicht nur Staffelung, sondern die ganze Aussage: Sie sind Umgebung, nicht Gegenstand -
        // niemand geht in sie hinein, sie stehen einfach da, wie Haeuser eben. Auf
        // Moebel-Helligkeit gezeichnet uebernaehmen sie sofort das Bild.
        Place.STREET -> listOf(
            Placement(HOUSE, anchorX = 0.02f, brightness = BACKDROP, behind = true),
            Placement(HOUSE_LOW, anchorX = 0.44f, brightness = BACKDROP, behind = true),
            Placement(HOUSE, anchorX = 1f, brightness = BACKDROP, behind = true),
            Placement(BENCH, anchorX = 0.52f, station = Station.BENCH),
            // 0,74 und nicht weiter rechts: Bei 0,84 stand die Laterne vollstaendig IN der
            // rechten Fassade. Vor einem Haus zu stehen ist richtig, darin zu verschwinden nicht.
            Placement(LAMPPOST, anchorX = 0.74f, station = Station.LAMP)
        )

        // Der WALD: drei verschiedene Baeume, ein umgestuerzter Stamm zum Sitzen, Unterholz.
        //
        // Keine Laterne - der Wald ist der einzige Ort ohne kuenstliches Licht, und nachts ist er
        // deshalb wirklich dunkel. Genau das unterscheidet ihn vom Park, der nachts von seiner
        // Laterne lebt.
        Place.FOREST -> listOf(
            // Die beiden hinteren Baeume ZUERST - sie stehen hinter allem anderen und duerfen
            // sich mit dem Vordergrund ueberschneiden; genau daraus entsteht die Tiefe eines
            // Waldes. Ohne sie waere es eine Baumreihe.
            Placement(OLDTREE, anchorX = 0.34f, brightness = BACKDROP, behind = true),
            Placement(PINE, anchorX = 1f, brightness = BACKDROP, behind = true),
            Placement(PINE, anchorX = 0f),
            Placement(BUSH, anchorX = 0.28f),
            Placement(LOG, anchorX = 0.58f, station = Station.BENCH),
            Placement(TREE, anchorX = 0.86f)
        )

        // Die WIESE: eine offene Freiflaeche, sparsam bestanden - das Gegenteil des dichten
        // Waldes und ruhiger als der Park, der ihm gehoert (siehe [habitatPlacements]). Nur ein
        // Baum am Rand, etwas Gebuesch, eine Bank in der Mitte zum Verweilen.
        Place.MEADOW -> listOf(
            Placement(BUSH, anchorX = 0.10f, brightness = BACKDROP, behind = true),
            Placement(TREE, anchorX = 0.92f, brightness = BACKDROP, behind = true),
            Placement(BENCH, anchorX = 0.50f, station = Station.BENCH)
        )

        // Die STADT: dichter bebaut als die Strasse und mit ZWEI Laternen belebter - derselbe
        // Weg, nur groesser und voller. Bank und (tragende) Laterne stehen an denselben
        // Bruchteilen wie auf der Strasse: Der Ruheplatz der Figur liegt dort ebenfalls bei
        // 0,08 (siehe [avatarAnchorX]), und genau dieser Abstand ist bereits geprueft
        // (SceneCompositionTest, "an jedem Ort bleibt Platz zum Stehen") - naeher heran haette
        // die Figur im Sitzen der Bank gestanden. Nur die erste Laterne traegt die Station: eine
        // zweite Bank oder Laterne ist Kulisse, kein zweiter Platz zum Ansteuern.
        Place.CITY -> listOf(
            Placement(HOUSE, anchorX = 0.02f, brightness = BACKDROP, behind = true),
            Placement(HOUSE_LOW, anchorX = 0.30f, brightness = BACKDROP, behind = true),
            Placement(HOUSE, anchorX = 0.58f, brightness = BACKDROP, behind = true),
            Placement(HOUSE_LOW, anchorX = 0.86f, brightness = BACKDROP, behind = true),
            Placement(BENCH, anchorX = 0.52f, station = Station.BENCH),
            Placement(LAMPPOST, anchorX = 0.74f, station = Station.LAMP),
            Placement(LAMPPOST, anchorX = 0.94f)
        )
        }
    }

    /**
     * Beiwerk, das sich nicht als einzelne Requisite verankern laesst, weil es ueber die ganze
     * Breite streut - derzeit die Grasbueschel draussen.
     *
     * Aus der Position selbst gerechnet statt gewuerfelt: So steht bei jedem Neuzeichnen derselbe
     * Halm an derselben Stelle. Zufaellig gestreutes Gras wuerde bei jedem Bild neu wachsen und
     * flimmern - eine Wiese muss stillstehen duerfen.
     */
    private fun groundDetail(
        place: Place,
        widthCells: Int,
        floorY: Int,
        species: AvatarSpecies
    ): List<SceneCell> = when (place) {
        // **Der Boden gehoert zum Lebensraum** und wechselt mit ihm (siehe [habitatPlacements]).
        //
        // Ohne das saehe die Savanne aus wie eine Wiese mit Akazien und der Sumpf wie eine Wiese
        // mit Schilf. Was eine Landschaft ausmacht, ist zur Haelfte das, worauf man steht - und
        // es ist der billigste Teil davon: eine einzige Zeile ueber der Bodenlinie.
        Place.PARK -> habitatGround(species, widthCells, floorY)

        // Wald: DICHTERES Bodenkraut als im Park, und zweizeilig. Der Unterschied zwischen einer
        // gepflegten Wiese und einem Waldboden liegt genau darin, dass man den Boden nicht mehr
        // sieht - drei verschobene Teiler ergeben ein unregelmaessiges Muster, das sich beim
        // Hinsehen nicht als Rechenvorschrift zu erkennen gibt.
        Place.FOREST -> (0 until widthCells).flatMap { x ->
            buildList {
                if (x % 4 == 1 || x % 7 == 3) add(SceneCell(x, floorY - 1, STRUCTURE))
                // Nur vereinzelt eine zweite Zeile hoch - dichter gesetzt wurde daraus ein
                // flimmernder Teppich, der die Figur von unten her ausfranste.
                if (x % 13 == 6) add(SceneCell(x, floorY - 2, STRUCTURE))
            }
        }

        // Strasse: Pflasterfugen in gleichmaessigem Abstand - das Gegenstueck zum Gras. Ein
        // regelmaessiges Muster wirkt hier richtig, wo es auf einer Wiese falsch waere: Pflaster
        // IST gelegt, Gras waechst.
        Place.STREET -> (0 until widthCells)
            .filter { it % 4 == 0 }
            .map { SceneCell(it, floorY, (STRUCTURE * 1.4f).roundToInt()) }
        // Teppich: ein aufgehellter Abschnitt der Bodenlinie mit Fransen an den Enden. Auf einem
        // seitlich gesehenen Boden ist ein Teppich nichts anderes als ein Stueck Boden, das anders
        // aussieht - genau das laesst sich hier mit drei Zellen Unterschied sagen.
        Place.LIVING -> indoorFloor(species, widthCells, floorY, from = 0.22f, to = 0.62f)
        Place.BEDROOM -> indoorFloor(species, widthCells, floorY, from = 0.40f, to = 0.72f)
        Place.CRAFT -> indoorFloor(species, widthCells, floorY, from = 0.26f, to = 0.58f)

        // Wiese: LOCKERES Gras, einzeilig - lichter als der Waldboden und ohne dessen zweite
        // Reihe. Eine gepflegte Freiflaeche, keine Wildnis.
        Place.MEADOW -> (0 until widthCells)
            .filter { it % 3 == 0 || it % 5 == 2 }
            .map { SceneCell(it, floorY - 1, STRUCTURE) }

        // Stadt: dasselbe Pflaster wie die Strasse, nur dichter - das macht den Unterschied
        // zwischen einem Weg und einem Platz.
        Place.CITY -> (0 until widthCells)
            .filter { it % 3 == 0 }
            .map { SceneCell(it, floorY, (STRUCTURE * 1.4f).roundToInt()) }
        else -> emptyList()
    }

    /**
     * **Der Boden gehoert zur Wohnung wie zur Landschaft** (siehe [habitatGround]).
     *
     * Ein Teppich sagt "hier wohnt jemand, der einen Teppich hat" - und genau deshalb passt er
     * nicht zu jedem. Drei Kreaturen bekommen stattdessen den Untergrund, auf dem sie ohnehin
     * leben: Steinplatten, Dielen, feuchte Stellen. Es ist eine einzige Zeile im Bild und der
     * billigste Unterschied zwischen zwei Zimmern, den es hier gibt.
     */
    private fun indoorFloor(
        species: AvatarSpecies,
        widthCells: Int,
        floorY: Int,
        from: Float,
        to: Float
    ): List<SceneCell> = when (species) {
        AvatarSpecies.PUFFLING, AvatarSpecies.STARLET, AvatarSpecies.FENNEC ->
            rugAt(widthCells, floorY, from, to)

        // Steinplatten: breite Felder mit deutlichen Fugen. Zum Drachen gehoert kein Teppich.
        AvatarSpecies.WYRMLING -> (0 until widthCells)
            .filter { it % 6 == 0 }
            .map { SceneCell(it, floorY, (STRUCTURE * 1.8f).roundToInt()) }

        // Dielen: engere Fugen als beim Stein - schmaler heisst auf diesem Raster "aus Holz".
        AvatarSpecies.HOOTLET -> (0 until widthCells)
            .filter { it % 4 == 0 }
            .map { SceneCell(it, floorY, (STRUCTURE * 1.5f).roundToInt()) }

        // Feuchte Stellen, unregelmaessig verteilt. Wo er langgeht, bleibt etwas zurueck.
        AvatarSpecies.GLOOP -> (0 until widthCells)
            .filter { it % 5 == 2 || it % 7 == 4 }
            .map { SceneCell(it, floorY, (STRUCTURE * 1.6f).roundToInt()) }
    }

    /**
     * Der Bewuchs bzw. Untergrund je Lebensraum - eine Zeile ueber der Bodenlinie.
     *
     * Aus der Position gerechnet und nicht gewuerfelt, wie bei allem Beiwerk hier: So steht bei
     * jedem Neuzeichnen derselbe Halm an derselben Stelle. Zufaellig gestreuter Boden wuechse bei
     * jedem Bild neu und flimmerte.
     */
    private fun habitatGround(
        species: AvatarSpecies,
        widthCells: Int,
        floorY: Int
    ): List<SceneCell> = when (species) {
        // Gemaehte Wiese: einzelne Halme, weit auseinander.
        AvatarSpecies.PUFFLING -> (0 until widthCells)
            .filter { it % 7 == 3 || it % 11 == 5 }
            .map { SceneCell(it, floorY - 1, STRUCTURE) }

        // Blumenwiese: BUESCHEL mit Luecken dazwischen, dafuer hoch. Eine Wiese, die man nicht
        // maeht, waechst nicht gleichmaessig - und ein fast geschlossener Teppich sah dem
        // Waldboden zum Verwechseln aehnlich.
        AvatarSpecies.STARLET -> (0 until widthCells).flatMap { x ->
            buildList {
                if (x % 5 == 1 || x % 5 == 2) add(SceneCell(x, floorY - 1, STRUCTURE))
                if (x % 5 == 1) add(SceneCell(x, floorY - 2, STRUCTURE))
            }
        }

        // Geroell: einzelne Steine, unregelmaessig - kein Bewuchs.
        AvatarSpecies.WYRMLING -> (0 until widthCells)
            .filter { (it * it) % 13 < 3 }
            .map { SceneCell(it, floorY - 1, (STRUCTURE * 1.5f).roundToInt()) }

        // Sand: lange, flache Rippel AUF der Bodenlinie statt Halmen darueber. Ein Boden, aus
        // dem nichts herauswaechst, ist genau das, was eine Wueste ausmacht.
        AvatarSpecies.FENNEC -> (0 until widthCells)
            .filter { (it / 3) % 3 == 0 }
            .map { SceneCell(it, floorY, (STRUCTURE * 1.4f).roundToInt()) }

        // Wasser: eine durchgehende, hellere Linie mit Wellen - der einzige Boden, der glaenzt.
        AvatarSpecies.GLOOP -> (0 until widthCells).map { x ->
            val wave = if ((x / 2) % 3 == 0) 1 else 0
            SceneCell(x, floorY - wave, (STRUCTURE * (1.3f + wave * 0.5f)).roundToInt())
        }

        // Waldboden: DICHT und flach - Laub und Nadeln, aus denen nichts herausragt. Genau
        // umgekehrt zur Wiese, die hoch und in Buescheln waechst.
        AvatarSpecies.HOOTLET -> (0 until widthCells)
            .filter { it % 3 != 2 }
            .map { SceneCell(it, floorY - 1, STRUCTURE) }
    }

    private fun rugAt(widthCells: Int, floorY: Int, from: Float, to: Float): List<SceneCell> {
        // Im ZIMMER verankert wie die Moebel (siehe [roomWidth]) - ein Teppich, der sich im
        // Querformat auf die doppelte Laenge zieht, waehrend Bett und Nachttisch beieinander
        // bleiben, ist kein Teppich mehr, sondern ein Laufsteg.
        val room = roomWidth(widthCells)
        val shift = (widthCells - room) / 2
        val start = shift + (room * from).roundToInt()
        val end = (shift + (room * to).roundToInt()).coerceAtMost(widthCells - 1)
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
        useSpot = -1 to 7,
        screenArea = rect(5, 1, 7, 2)
    )

    /** Tisch mit Kanne - der Dampf darueber kommt aus [ambient]. */
    private val TABLE = Prop(
        width = 11, height = 7,
        art = rect(5, 0, 7, 2) + listOf(8 to 1) +  // Kanne mit Tuelle
            hLine(0, 10, 3) +                       // Tischplatte
            vLine(1, 4, 6) + vLine(9, 4, 6),
        useSpot = -1 to 6,
        vaporAt = 6 to 0
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
        useSpot = -4 to 8,
        lightAt = 1 to 2
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

    // ---- Draussen: Wald ----
    //
    // Ein Wald ist nicht "mehr Baeume", sondern Baeume, die sich UNTERSCHEIDEN. Drei gleiche
    // Silhouetten nebeneinander lesen sich als Muster, nicht als Bewuchs. Deshalb gibt es neben
    // dem runden Laubbaum ([TREE]) eine hohe schmale Fichte und einen knorrigen alten Baum: erst
    // die verschiedenen Umrisse machen aus einer Reihe einen Bestand.

    /** Fichte - hoch und schmal, laeuft nach oben spitz zu. Der Gegenpol zur runden Baumkrone. */
    private val PINE = Prop(
        width = 7, height = 15,
        art = listOf(3 to 0) +
            hLine(2, 4, 1) + hLine(2, 4, 2) +
            hLine(1, 5, 3) + hLine(1, 5, 4) +
            hLine(0, 6, 5) + hLine(1, 5, 6) +
            hLine(0, 6, 7) + hLine(1, 5, 8) +
            hLine(0, 6, 9) +
            vLine(3, 10, 14)
    )

    /** Alter Baum - breite Krone auf kurzem, dickem Stamm; er wirkt aelter als der schlanke. */
    private val OLDTREE = Prop(
        width = 11, height = 12,
        art = hLine(3, 7, 0) + hLine(1, 9, 1) + hLine(0, 10, 2) + hLine(0, 10, 3) +
            hLine(1, 9, 4) + hLine(2, 8, 5) +
            // Zwei Stammzellen breit statt einer: Das allein liest sich als Alter.
            vLine(5, 6, 11) + vLine(6, 6, 11)
    )

    /**
     * Umgestuerzter Stamm - im Wald das, was im Park die Bank ist.
     *
     * **Bewusst keine Bank in den Wald gestellt.** Eine Parkbank zwischen Baeumen macht aus dem
     * Wald einen weiteren Park; ein liegender Stamm sagt dasselbe ("hier kann man sitzen") und
     * sagt zugleich, WO man ist. Die Stationsbezeichnung bleibt [Station.BENCH], damit alle
     * vorhandenen Ablaeufe unveraendert funktionieren - was man benutzt, ist eine Sitzgelegenheit,
     * unabhaengig davon, wie sie aussieht.
     */
    private val LOG = Prop(
        width = 13, height = 4,
        art = hLine(0, 12, 1) + hLine(0, 12, 2) +
            // Wurzelteller links, abgebrochene Krone rechts - ohne die beiden Enden waere es ein
            // Balken statt eines Baumes, der einmal gestanden hat.
            listOf(0 to 0, 1 to 0, 0 to 3, 12 to 0, 11 to 3, 12 to 3),
        // Sitzflaeche vor dem Sitzenden, wie bei Bank und Sessel.
        frontArt = hLine(3, 12, 2),
        useSpot = 6 to 2
    )

    // ---- Draussen: die Lebensraeume ----
    //
    // **Fuenf neue Formen fuer sechs Landschaften** - der Rest kommt aus dem, was schon da ist
    // (Baum, Fichte, Strauch, Stamm, Pflanze). Das ist dasselbe Vorgehen wie bei den
    // Arbeitsplaetzen: Eine andere ZUSAMMENSTELLUNG bekannter Teile ergibt bereits einen
    // erkennbar anderen Ort, und nur was wirklich fehlt, wird neu gezeichnet.
    //
    // Was fehlte, war jeweils die eine Form, an der man eine Landschaft SOFORT erkennt: die
    // flache Krone der Akazie, der Findling, die Felsnadel, das Schilf, die hohe Blume. Ohne sie
    // waeren Savanne und Gebirge nur zwei verschieden angeordnete Baumreihen.

    /**
     * Findling - rund, gedrungen, liegt einfach da. Fuer Gebirge und Savanne.
     *
     * Zugleich die Sitzgelegenheit dieser beiden Landschaften: Eine Parkbank im Gebirge waere
     * absurd, ein Stein zum Draufsetzen ist es nicht. Wie bei Bank und Stamm legt sich seine
     * vordere Woelbung ueber die Beine des Sitzenden - dieselbe Verdeckung, die ueberall in
     * dieser Welt eine eigene Sitzhaltung erspart.
     */
    private val ROCK = Prop(
        width = 7, height = 4,
        art = hLine(2, 4, 0) + hLine(1, 5, 1) + hLine(0, 6, 2) + hLine(0, 6, 3),
        frontArt = hLine(0, 6, 2) + hLine(0, 6, 3),
        useSpot = 3 to 1
    )

    /**
     * Felsnadel - hoch, schmal, oben spitz.
     *
     * Der Gegenpol zum Findling: Aus einer runden und einer steilen Form zusammen liest sich ein
     * Gebirge. Aus zwei gleichen wuerde eine Steinsammlung.
     */
    private val CRAG = Prop(
        width = 7, height = 13,
        art = listOf(3 to 0, 3 to 1, 2 to 2, 3 to 2, 4 to 2) +
            hLine(2, 4, 3) + hLine(1, 5, 4) + hLine(1, 5, 5) +
            hLine(1, 5, 6) + hLine(0, 6, 7) + hLine(0, 6, 8) +
            hLine(0, 6, 9) + hLine(0, 6, 10) + hLine(0, 6, 11) + hLine(0, 6, 12)
    )

    /**
     * Akazie - flache, breite Krone auf hohem, duennem Stamm.
     *
     * Die eine Form, an der man eine Savanne erkennt, noch bevor man den Sand sieht. Genau
     * umgekehrt gebaut wie der Laubbaum: dort eine runde Krone auf kurzem Stamm, hier ein
     * Schirm auf einem Stiel.
     */
    private val ACACIA = Prop(
        width = 13, height = 12,
        art = hLine(3, 9, 0) + hLine(1, 11, 1) + hLine(0, 12, 2) +
            listOf(6 to 3, 6 to 4, 6 to 5, 6 to 6, 6 to 7, 6 to 8, 6 to 9, 6 to 10, 6 to 11,
                5 to 11, 7 to 11)
    )

    /** Schilf - ein paar Halme unterschiedlicher Hoehe. Fuer den Sumpf. */
    private val REEDS = Prop(
        width = 7, height = 8,
        art = listOf(0 to 3, 0 to 4, 0 to 5, 0 to 6, 0 to 7) +
            listOf(2 to 0, 2 to 1, 2 to 2, 2 to 3, 2 to 4, 2 to 5, 2 to 6, 2 to 7) +
            listOf(4 to 2, 4 to 3, 4 to 4, 4 to 5, 4 to 6, 4 to 7) +
            listOf(6 to 5, 6 to 6, 6 to 7) +
            // Kolben an den beiden hohen Halmen - erst dadurch ist es Schilf und nicht Gras.
            listOf(1 to 0, 3 to 0, 1 to 1, 3 to 1, 3 to 2, 5 to 2, 5 to 3)
    )

    /** Hohe Blume - Stiel mit Bluetenkopf. Fuer die Blumenwiese. */
    private val FLOWER = Prop(
        width = 5, height = 9,
        art = listOf(1 to 0, 2 to 0, 3 to 0, 0 to 1, 4 to 1, 1 to 2, 2 to 2, 3 to 2) +
            listOf(2 to 3, 2 to 4, 2 to 5, 2 to 6, 2 to 7, 2 to 8) +
            // Ein Blatt auf halber Hoehe, sonst ist es ein Nagel mit Kopf.
            listOf(0 to 5, 1 to 5)
    )

    // ---- Draussen: Strasse ----

    /**
     * Hausfassade - die Kulisse, vor der eine Strasse ueberhaupt erst eine Strasse ist.
     *
     * **Als Umriss mit Fensterloechern, nicht als gefuellter Block.** Eine geschlossene Flaeche in
     * dieser Groesse waere ein schwarzes Rechteck mit Rand, also genau der "Kasten", den diese
     * Welt sonst ueberall vermeidet. Die Fenster sind Aussparungen im Mauerwerk; nachts werden
     * genau sie erleuchtet (siehe [litWindows]), und dann steht dort eine Strasse, in der jemand
     * wohnt - das ist der ganze Trick, und er kostet keine einzige zusaetzliche Requisite.
     */
    private val HOUSE = Prop(
        width = 13, height = 17,
        art = hLine(0, 12, 0) + vLine(0, 1, 16) + vLine(12, 1, 16) +
            // Geschossbaender - sie geben dem Haus seine Hoehe, ohne dass Fenster noetig waeren.
            hLine(1, 11, 5) + hLine(1, 11, 10) +
            // Fensterlaibungen: nur die senkrechten Kanten, die Waende bleiben offen.
            vLine(3, 2, 4) + vLine(5, 2, 4) + vLine(8, 2, 4) + vLine(10, 2, 4) +
            vLine(3, 7, 9) + vLine(5, 7, 9) + vLine(8, 7, 9) + vLine(10, 7, 9) +
            // Tuer im Erdgeschoss.
            vLine(5, 12, 16) + vLine(8, 12, 16) + hLine(5, 8, 12)
    )

    /** Zweites, niedrigeres Haus - eine Strasse aus lauter gleichen Fassaden ist eine Tapete. */
    private val HOUSE_LOW = Prop(
        width = 11, height = 12,
        art = hLine(0, 10, 0) + vLine(0, 1, 11) + vLine(10, 1, 11) +
            hLine(1, 9, 6) +
            vLine(2, 2, 5) + vLine(4, 2, 5) + vLine(7, 2, 5) + vLine(9, 2, 5) +
            vLine(4, 8, 11) + vLine(7, 8, 11) + hLine(4, 7, 8)
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
        return besideDoor(own)
    }

    /**
     * **Jede Kreatur hat ihre eigene Landschaft.**
     *
     * Bis hierhin teilten sich alle sechs denselben Park. Das war der letzte Ort, an dem sie sich
     * nur durch ihre Silhouette unterschieden - und ausgerechnet der Ort, an dem sie am meisten
     * Zeit draussen verbringen. Ein Wesen mit Fluegeln, eines aus Schleim und ein Wuestenfuchs
     * gehoeren nicht auf dieselbe Rasenflaeche; sie sind verschieden GEBAUT, und ihre Umgebung
     * sollte das beantworten.
     *
     * **Abgeleitet aus Koerper UND Charakter**, nicht ausgewuerfelt: Wyrmling hat Fluegel und ist
     * der Antreiber, also Felsen, die man erklimmen kann. Fennec ist ein Wuestenfuchs, also
     * Savanne. Gloop ist weich und langsam, also Sumpf. Hootlet beobachtet und sieht nachts,
     * also hoher Wald. Starlet traeumt, also eine Blumenwiese. Puffling behaelt den Park - als
     * Einstiegsfigur ist das Vertraute bei ihr richtig.
     *
     * **Die Strasse bleibt fuer alle dieselbe.** Wenn jeder sein eigenes Draussen hat, braucht es
     * einen Ort, an dem man sich trotzdem begegnet - sonst waeren sechs Welten nebeneinander
     * statt einer mit sechs Bewohnern.
     *
     * Jeder Lebensraum braucht eine Sitzgelegenheit unter [Station.BENCH]: Alle Spazier-Ablaeufe
     * steuern sie an (siehe [PlayRoutines]), und ohne sie liefe die Haelfte davon ins Leere. Wie
     * sie aussieht, ist dagegen frei - Bank, Stamm oder Findling.
     */
    private fun habitatPlacements(species: AvatarSpecies): List<Placement> = when (species) {
        // PARK - der Klassiker. Als Einstiegsfigur bekommt Puffling das Vertraute: gemaehte
        // Wiese, Bank, Laterne. Wer die App zum ersten Mal oeffnet, soll nicht erst eine
        // Landschaft deuten muessen.
        AvatarSpecies.PUFFLING -> listOf(
            Placement(TREE, anchorX = 0f),
            Placement(BENCH, anchorX = 0.55f, station = Station.BENCH),
            Placement(BUSH, anchorX = 0.72f),
            Placement(LAMPPOST, anchorX = 0.86f, station = Station.LAMP),
            Placement(TREE, anchorX = 1f)
        )

        // BLUMENWIESE - "Du musst heute nicht perfekt sein." Hohe Blumen, die ueber ihr
        // zusammenschlagen, und ein umgestuerzter Stamm zum Hineinlegen. Nichts hier ist
        // gepflegt; es waechst einfach.
        AvatarSpecies.STARLET -> listOf(
            Placement(TREE, anchorX = 1f, brightness = BACKDROP, behind = true),
            Placement(FLOWER, anchorX = 0f),
            Placement(FLOWER, anchorX = 0.14f),
            Placement(LOG, anchorX = 0.52f, station = Station.BENCH),
            Placement(FLOWER, anchorX = 0.88f),
            // 0,34 und nicht 0,74: Dort stand er fast genau da, wo im Park der Strauch steht -
            // zwei Landschaften, die sich ein Detail an derselben Stelle teilen, wirken beim
            // Umschalten wie dieselbe mit anderer Figur.
            Placement(BUSH, anchorX = 0.34f)
        )

        // GEBIRGE - "Los geht's. Nur ein kleiner Schritt." Ein Drache mit Fluegeln gehoert dorthin,
        // wo es hinaufgeht. Felsnadel und Findling zusammen, weil eine runde und eine steile Form
        // erst gemeinsam ein Gebirge ergeben.
        AvatarSpecies.WYRMLING -> listOf(
            Placement(CRAG, anchorX = 0.94f, brightness = BACKDROP, behind = true),
            Placement(CRAG, anchorX = 0f),
            Placement(ROCK, anchorX = 0.50f, station = Station.BENCH),
            Placement(ROCK, anchorX = 0.74f),
            Placement(PINE, anchorX = 0.26f, brightness = BACKDROP, behind = true)
        )

        // SAVANNE - ein Wuestenfuchs in seiner eigenen Landschaft. Die Akazie traegt die Szene
        // ganz allein: An ihrer flachen Krone erkennt man sie, bevor man den Sand sieht.
        AvatarSpecies.FENNEC -> listOf(
            Placement(ACACIA, anchorX = 0.04f),
            Placement(ROCK, anchorX = 0.46f, station = Station.BENCH),
            Placement(BUSH, anchorX = 0.66f),
            Placement(ACACIA, anchorX = 1f, brightness = BACKDROP, behind = true)
        )

        // SUMPF - "Alles darf auch langsam gehen." Weich, warm, ohne Kanten; Schilf statt Baeumen.
        // Der einzige Lebensraum, in dem nichts aufragt - das passt zu einem Wesen, das selbst
        // keine feste Form hat.
        AvatarSpecies.GLOOP -> listOf(
            Placement(REEDS, anchorX = 0f),
            Placement(REEDS, anchorX = 0.18f),
            Placement(LOG, anchorX = 0.50f, station = Station.BENCH),
            Placement(REEDS, anchorX = 0.80f),
            Placement(REEDS, anchorX = 1f, brightness = BACKDROP, behind = true),
            Placement(BUSH, anchorX = 0.68f)
        )

        // HOHER WALD - "Ruhe bringt Klarheit." Eine Eule sitzt hoch und sieht nachts. Fichten
        // dicht an dicht, dazwischen ein alter Baum; kein kuenstliches Licht.
        AvatarSpecies.HOOTLET -> listOf(
            Placement(OLDTREE, anchorX = 0.38f, brightness = BACKDROP, behind = true),
            Placement(PINE, anchorX = 0f),
            Placement(PINE, anchorX = 0.20f, brightness = BACKDROP, behind = true),
            Placement(LOG, anchorX = 0.68f, station = Station.BENCH),
            Placement(PINE, anchorX = 1f),
            Placement(BUSH, anchorX = 0.80f)
        )
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

    /** WYRMLING - ausgehauener Steintrog. Eine Emailwanne mit Fuessen passt zu ihm so wenig wie
     *  ein Teppich. */
    private val STONETUB = Prop(
        width = 11, height = 5,
        art = vLine(0, 0, 3) + vLine(1, 0, 3) + vLine(9, 0, 3) + vLine(10, 0, 3) +
            hLine(0, 10, 3) + hLine(1, 9, 4),
        frontArt = rect(2, 1, 8, 3),
        useSpot = 5 to 3
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
        useSpot = -1 to 3,
        vaporAt = 6 to 0
    )

    // ---- Woran diese Kreatur isst ----
    //
    // Sechs Tische, und jeder traegt sein Gefaess woanders - moeglich geworden durch
    // [Prop.vaporAt]. Solange der Dampf von aussen an einer festen Stelle gezeichnet wurde,
    // MUSSTEN alle Tische ihren Topf dort haben, und damit blieben sie verschiedene Untergestelle
    // unter derselben Platte.

    /** STARLET - schmale Platte auf gekreuzten Beinen, die Kanne links. */
    private val CROSSTABLE = Prop(
        width = 11, height = 7,
        art = rect(1, 0, 3, 2) + listOf(4 to 1) +               // Kanne mit Tuelle
            hLine(0, 10, 3) +                                   // Platte
            listOf(1 to 4, 2 to 5, 3 to 6, 3 to 4, 1 to 6) +    // gekreuzte Beine links
            listOf(7 to 4, 8 to 5, 9 to 6, 9 to 4, 7 to 6),     // und rechts
        useSpot = -1 to 6,
        vaporAt = 2 to 0
    )

    /** FENNEC - niedriger runder Tisch. Wer auf Kissen sitzt, isst nicht an einem hohen. */
    private val LOWROUND = Prop(
        width = 11, height = 5,
        art = rect(7, 0, 9, 1) +                                // Topf rechts
            hLine(0, 10, 2) + hLine(1, 9, 3) +                  // gewoelbte Platte
            listOf(4 to 4, 6 to 4),                             // kurzer Fuss
        useSpot = -1 to 4,
        vaporAt = 8 to 0
    )

    /** HOOTLET - Tisch aus einem Baumstumpf. Im Wald nimmt man, was steht. */
    private val STUMPTABLE = Prop(
        width = 11, height = 7,
        art = rect(2, 0, 4, 2) + listOf(5 to 1) +
            hLine(0, 10, 3) + hLine(1, 9, 4) +                  // Schnittflaeche
            rect(3, 5, 7, 6),                                   // dicker Stamm
        useSpot = -1 to 6,
        vaporAt = 3 to 0
    )

    /**
     * WYRMLING - Steinplatte auf zwei Bloecken. Ein Drache isst nicht an einem Brett auf Beinen.
     *
     * Dieselben Massen wie [TABLE] und der Topf an derselben Stelle (x5..7, oberste Zeile): Der
     * Dampf darueber kommt aus [ambient] und findet ihn dadurch bei jeder Kreatur, ohne dass die
     * Umgebungsanimation die Tische auseinanderhalten muesste.
     */
    private val SLABTABLE = Prop(
        width = 11, height = 7,
        art = rect(7, 0, 9, 2) + listOf(6 to 1) +               // Kessel mit Tuelle
            hLine(0, 10, 3) + hLine(0, 10, 4) +                 // dicke Platte
            rect(1, 5, 2, 6) + rect(8, 5, 9, 6),                // zwei Steinbloecke
        useSpot = -1 to 6,
        vaporAt = 8 to 0
    )

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

    // ---- Der grosse Sitzplatz im Wohnzimmer ----
    //
    // Das Sofa war bewusst lange fuer alle dasselbe: Es ist das Moebel, auf dem man Gaeste
    // empfaengt, und gemeinsame Einrichtung sagt etwas anderes aus als der eigene Lieblingsplatz.
    // Dieses Argument traegt aber nur, solange alle ueberhaupt auf ein Sofa KOENNEN - und drei von
    // sechs koennen es nicht: GLOOP hat keine Beine (er kaeme so wenig auf eine Sitzflaeche wie in
    // eine hohe Wanne), HOOTLET ist ein Vogel und sitzt auf etwas statt in etwas, und ein Drache
    // thront. Wo der Koerper widerspricht, geht der Koerper vor.

    /** WYRMLING - Steinbank mit aufragender Lehnkante. Massiv wie alles, worauf er sich setzt. */
    private val STONESEAT = Prop(
        width = 13, height = 6,
        art = rect(0, 0, 1, 3) +                                 // Lehnkante
            rect(0, 3, 12, 4) +                                  // Sitzflaeche
            listOf(1 to 5, 11 to 5),                             // Sockel
        frontArt = rect(2, 3, 12, 4),
        useSpot = 7 to 4
    )

    /**
     * FENNEC - Polsterbank mit drei Kissenkuppen, ohne Lehne.
     *
     * Er BRAUCHT kein anderes Sitzmoebel als ein Sofa - ein Wuestenfuchs hat Beine und einen
     * Ruecken. Er bekommt trotzdem eines, und das ist der Unterschied zwischen den beiden Sorten
     * Eintrag in [Home]: Hier entscheidet nicht der Koerper, sondern dass sein Wohnzimmer sonst zu
     * drei Vierteln dasselbe waere wie das des Einstiegs-Avatars. Zu ihm passt es ohnehin - in der
     * Leseecke sitzt er auf einem Kissenberg, im Schlafzimmer liegt er in einem gepolsterten Bau.
     */
    private val DIVAN = Prop(
        width = 13, height = 4,
        art = listOf(1 to 0, 2 to 0, 5 to 0, 6 to 0, 9 to 0, 10 to 0) +   // Kissenkuppen
            hLine(0, 12, 1) + hLine(0, 12, 2) +                           // Polsterflaeche
            listOf(1 to 3, 11 to 3),                                      // Fuesse
        frontArt = hLine(0, 12, 1) + hLine(0, 12, 2),
        useSpot = 6 to 2
    )

    /** GLOOP - breite Polsterbank am Boden. Dieselbe Ueberlegung wie beim Bodenkissen, nur fuer zwei. */
    private val FLOORSOFA = Prop(
        width = 13, height = 3,
        art = hLine(1, 11, 0) + hLine(0, 12, 1) + hLine(0, 12, 2),
        frontArt = hLine(0, 12, 1) + hLine(0, 12, 2),
        useSpot = 6 to 1
    )

    /**
     * HOOTLET - Sitzbalken zwischen zwei Pfosten.
     *
     * Der Balken liegt drei Zellen ueber dem Boden und keinen Zoll hoeher. Dieselbe Grenze wie bei
     * der Sitzstange in der Leseecke ([PERCH]): Die Figur reicht STEHEND schon fast bis zur Decke,
     * und alles Erhoehte schiebt sie hindurch.
     */
    private val ROOST = Prop(
        width = 13, height = 9,
        art = vLine(0, 0, 8) + vLine(12, 0, 8) +                 // Pfosten
            hLine(0, 12, 0) +                                    // Querbalken oben
            listOf(1 to 1, 11 to 1) +                            // Verstrebung
            hLine(0, 12, 6),                                     // Sitzbalken
        frontArt = hLine(2, 12, 6),
        useSpot = 6 to 6
    )

    /**
     * STARLET - Haengeschaukel, an zwei langen Seilen von der Decke.
     *
     * **Die einzige Requisite, die OBEN beginnt.** Alles andere in dieser Welt steht auf dem
     * Boden; die Seile laufen von der Zimmerdecke herunter (die Hoehe ist genau
     * [CEILING_HEIGHT_CELLS]), und das macht die Silhouette auf den ersten Blick unverwechselbar -
     * ohne dass ein einziger Sitz anders gezeichnet werden musste als die Sitzflaeche selbst.
     *
     * Zu ihr gehoert sie, weil sie in der Haengematte schlaeft: Wer nicht auf Moebeln liegt,
     * sitzt auch nicht darauf.
     */
    private val SWINGSEAT = Prop(
        width = 11, height = CEILING_HEIGHT_CELLS,
        art = vLine(1, 0, 14) + vLine(9, 0, 14) +                // Seile bis zur Decke
            hLine(0, 10, 15) + hLine(1, 9, 16),                  // Sitz mit Polster
        frontArt = hLine(0, 10, 15) + hLine(1, 9, 16),
        // Drei Zellen ueber dem Boden - dieselbe Hoehe wie Haengematte und Sitzstange, und die
        // ist die Grenze: Hoeher stiesse die groesste Form durch die Decke.
        useSpot = 5 to 15
    )

    /**
     * **Feuerstelle - das Gegenstueck zum Fernseher.**
     *
     * Abends sitzt man um etwas herum, das leuchtet; welches Ding das ist, sagt mehr ueber einen
     * Haushalt aus als jedes andere Moebelstueck. Drei dieser Kreaturen leben wild (Drache,
     * Wuestenfuchs, Eule) - bei ihnen ist es ein Feuer, bei den uebrigen eine Mattscheibe. Beide
     * haengen am selben Schalter ([Station.TV]): "Fernseher aus" und "Feuer runtergebrannt" sind
     * dieselbe Geste, und deshalb musste dafuer kein einziger Ablauf geaendert werden.
     *
     * **Der Rauchfang ist der Grund, warum man ihn nicht mit dem Fernseher verwechselt.** Ein
     * erster Entwurf war ein Rechteck derselben Groesse mit Flammen darin - im Bild waren die
     * beiden dadurch fast deckungsgleich, und ein Wohnzimmer mit Kamin sah aus wie eines mit
     * Fernseher. Die nach oben zulaufende Haube gibt ihm eine eigene Silhouette, noch bevor das
     * erste Licht darin brennt.
     */
    private val HEARTH = Prop(
        width = 11, height = 11,
        art = hLine(4, 6, 0) + hLine(4, 6, 1) +                  // Schornstein
            hLine(3, 7, 2) + hLine(2, 8, 3) + hLine(1, 9, 4) +   // Haube
            hLine(0, 10, 5) +                                    // Sims
            vLine(0, 6, 9) + vLine(1, 6, 9) +                    // Wangen
            vLine(9, 6, 9) + vLine(10, 6, 9) +
            hLine(2, 8, 10),                                     // Bodenplatte
        useSpot = -6 to 10,
        // Die Flammen laufen nach oben spitz zu - erst diese Form unterscheidet ein Feuer von
        // einer beleuchteten Flaeche. Gefuellt wird sie von [ambient], deshalb ist sie hier
        // ausgespart und nicht Teil der Silhouette.
        screenArea = listOf(5 to 6) + hLine(4, 6, 7) + hLine(3, 7, 8) + hLine(2, 8, 9)
    )

    /**
     * WYRMLING - Feuergrube: ein Steinring mit offenem Feuer darin, kniehoch.
     *
     * Der Drache bekommt KEINEN Kamin, obwohl das Feuer zu ihm gehoert wie zu keinem sonst. Ein
     * Kamin ist ein Feuer, das jemand eingebaut hat - eine Feuergrube ist eines, um das herum man
     * sich setzt. Nebenbei loest das ein Problem, das beim Vergleich der Zimmer auffiel: Kamin,
     * Ofen und Fernseher stehen alle als hohes Ding an der Wand, und drei davon nebeneinander
     * unterscheiden sich weniger, als sie sollten. Etwas Flaches am Boden dagegen unterscheidet
     * sich von allen dreien auf den ersten Blick.
     */
    private val FIREPIT = Prop(
        width = 11, height = 6,
        art = listOf(0 to 3, 2 to 3, 8 to 3, 10 to 3) +          // aufragende Steine
            hLine(0, 10, 4) + hLine(0, 10, 5),                   // Steinring
        useSpot = -6 to 5,
        screenArea = listOf(5 to 0) + hLine(4, 6, 1) + hLine(3, 7, 2) + hLine(3, 7, 3)
    )

    /**
     * FENNEC - Kanonenofen mit Rohr.
     *
     * Der dritte Weg, abends etwas Leuchtendes im Zimmer zu haben - und noetig, weil er sonst
     * denselben Kamin haette wie Drache und Eule. Zu ihm gehoert er, weil ein Ofen genau das tut,
     * wofuer diese Figur steht: Er passt auf, dass es warm bleibt, ohne dass man ihn beachtet.
     */
    private val STOVE = Prop(
        width = 9, height = 12,
        art = vLine(4, 0, 2) +                                   // Ofenrohr
            hLine(1, 7, 3) +                                     // Deckplatte
            vLine(1, 4, 10) + vLine(7, 4, 10) +                  // Wangen
            hLine(1, 7, 5) + hLine(1, 7, 10) +                   // Kanten ueber und unter der Tuer
            listOf(0 to 11, 8 to 11),                            // Fuesse
        useSpot = -5 to 11,
        screenArea = rect(2, 6, 6, 9)
    )

    // ---- Leuchten je Spezies ----
    //
    // **Die Leuchte ist der wirksamste Unterschied zwischen zwei Zimmern**, und zwar aus einem
    // rein technischen Grund: Nachts weicht der ganze Raum zurueck, das Licht aber nicht (siehe
    // [SceneCell.isLight]). Was leuchtet, traegt die Szene dann allein - eine Feuerschale und ein
    // Lampenschirm ergeben um drei Uhr nachts zwei voellig verschiedene Bilder, obwohl der Raum
    // dahinter derselbe waere.
    //
    // Jede weiss ueber [Prop.lightAt] selbst, wo ihr Licht sitzt; [ambient] muss sie nicht kennen.

    /** WYRMLING - Feuerschale auf drei Beinen. */
    private val BRAZIER = Prop(
        width = 7, height = 8,
        art = hLine(1, 5, 1) + hLine(0, 6, 2) + hLine(1, 5, 3) +  // Schale
            vLine(3, 4, 6) + hLine(1, 5, 7),                      // Fuss
        useSpot = -4 to 7,
        lightAt = 3 to 0
    )

    /** GLOOP - Leuchtpilz. Im Sumpf waechst das Licht, statt eingeschaltet zu werden. */
    private val GLOWCAP = Prop(
        width = 7, height = 7,
        art = hLine(2, 4, 0) + hLine(1, 5, 1) + hLine(0, 6, 2) +  // Hut
            vLine(3, 3, 5) + hLine(2, 4, 6),                      // Stiel
        useSpot = -4 to 6,
        lightAt = 3 to 3
    )

    /** HOOTLET - zwei Kerzen auf einem Staender. Kein elektrisches Licht, wie in seinem Wald. */
    private val CANDLES = Prop(
        width = 5, height = 10,
        art = listOf(1 to 1, 3 to 1) +                            // Dochte
            vLine(1, 2, 4) + vLine(3, 2, 4) +                     // Kerzen
            hLine(1, 3, 5) +                                      // Teller
            vLine(2, 6, 8) + hLine(1, 3, 9),                      // Staender
        useSpot = -4 to 9,
        lightAt = 2 to 0
    )

    /** FENNEC - Sturmlaterne auf einem Pfosten. Ein Licht, das auch draussen brennen wuerde. */
    private val LANTERN = Prop(
        width = 5, height = 11,
        art = hLine(1, 3, 0) +                                    // Buegel
            vLine(1, 1, 4) + vLine(3, 1, 4) + hLine(1, 3, 5) +    // Glaskorb
            vLine(2, 6, 9) + hLine(1, 3, 10),                     // Pfosten
        useSpot = -4 to 10,
        lightAt = 2 to 2
    )

    /**
     * STARLET - Papierlampion.
     *
     * Die einzige Leuchte als UMRISS statt als gefuellte Form, und das ist hier kein Rueckfall:
     * Ein Lampion leuchtet von INNEN, die Huelle ist nur Papier davor. Waere sie gefuellt, saesse
     * das Licht auf der Aussenseite - und aus dem Lampion wuerde eine Kugel mit einem hellen
     * Punkt darauf.
     */
    private val PAPERLANTERN = Prop(
        width = 7, height = 10,
        art = hLine(2, 4, 0) + listOf(1 to 1, 5 to 1) +
            listOf(0 to 2, 6 to 2) + listOf(0 to 3, 6 to 3) +
            listOf(1 to 4, 5 to 4) + hLine(2, 4, 5) +
            vLine(3, 6, 8) + hLine(1, 5, 9),                      // Staender
        useSpot = -4 to 9,
        lightAt = 3 to 3
    )

    // ---- Was man immer wieder hervorholt: das Regal je Spezies ----

    /**
     * WYRMLING - Hort: eine Truhe mit zwei Schriftrollen obenauf.
     *
     * Ein Drache stellt seine Buecher nicht ins Regal, er hortet sie. Wie beim Buecherschrank
     * verschwindet beim Herausnehmen sichtbar etwas ([usedArt]) - sonst holte er etwas hervor,
     * ohne dass die Quelle darauf antwortet.
     */
    private val HOARD = Prop(
        width = 9, height = 8,
        art = hLine(1, 3, 0) + hLine(5, 7, 0) +                   // Rollen obenauf
            hLine(0, 8, 1) +                                      // Deckelkante
            (rect(0, 2, 8, 7) - hLine(1, 7, 4)),                  // Truhe mit Fuge
        useSpot = -5 to 7,
        usedArt = hLine(1, 3, 0) + hLine(0, 8, 1) + (rect(0, 2, 8, 7) - hLine(1, 7, 4))
    )

    /**
     * STARLET - Pflanzenregal: drei Boeden, auf denen etwas waechst statt steht.
     *
     * Sie liest auch, und ein Buecherregal waere nicht falsch - es waere nur dasselbe wie beim
     * Einstiegs-Avatar, und dann bestuende ihre halbe Leseecke aus fremdem Mobiliar. Zu ihr
     * gehoert es ohnehin: In ihrem Schlafzimmer stehen Pflanzen, an ihrer Wand rankt es, und
     * arbeiten tut sie in einer Gaertnerei.
     */
    private val PLANTSTAND = Prop(
        width = 9, height = 10,
        art = hLine(0, 8, 3) + hLine(0, 8, 6) + hLine(0, 8, 9) +  // drei Boeden
            vLine(0, 3, 9) + vLine(8, 3, 9) +                     // Wangen
            listOf(2 to 0, 3 to 0, 2 to 1, 3 to 1, 2 to 2) +      // was oben drauf steht
            listOf(5 to 1, 5 to 2, 6 to 2) +
            listOf(2 to 4, 3 to 4, 2 to 5, 6 to 4, 6 to 5) +      // im mittleren Fach
            listOf(3 to 7, 4 to 7, 3 to 8),                       // im unteren
        useSpot = -5 to 9,
        // Wie beim Buecherschrank verschwindet beim Herausnehmen sichtbar etwas.
        usedArt = hLine(0, 8, 3) + hLine(0, 8, 6) + hLine(0, 8, 9) +
            vLine(0, 3, 9) + vLine(8, 3, 9) +
            listOf(2 to 0, 3 to 0, 2 to 1, 3 to 1, 2 to 2) +
            listOf(5 to 1, 5 to 2, 6 to 2) +
            listOf(6 to 4, 6 to 5) +
            listOf(3 to 7, 4 to 7, 3 to 8)
    )

    /**
     * FENNEC - drei gestapelte Koerbe.
     *
     * Wer in einem Bau wohnt, hat keine Regale, sondern Behaelter. Der oberste steht beim
     * Herausnehmen offen ([usedArt]) - dieselbe Antwort der Quelle wie beim Buecherschrank.
     */
    private val BASKETS = Prop(
        width = 9, height = 11,
        art = hLine(1, 7, 0) + hLine(0, 8, 1) + hLine(0, 8, 3) +
            vLine(0, 1, 3) + vLine(8, 1, 3) +
            hLine(0, 8, 4) + hLine(0, 8, 7) + vLine(0, 4, 7) + vLine(8, 4, 7) +
            hLine(0, 8, 8) + hLine(0, 8, 10) + vLine(0, 8, 10) + vLine(8, 8, 10) +
            listOf(2 to 2, 6 to 2, 4 to 5, 2 to 9, 6 to 9),      // angedeutetes Geflecht
        useSpot = -5 to 10,
        usedArt = hLine(0, 8, 1) + hLine(0, 8, 3) +              // Deckel abgenommen
            vLine(0, 1, 3) + vLine(8, 1, 3) +
            hLine(0, 8, 4) + hLine(0, 8, 7) + vLine(0, 4, 7) + vLine(8, 4, 7) +
            hLine(0, 8, 8) + hLine(0, 8, 10) + vLine(0, 8, 10) + vLine(8, 8, 10) +
            listOf(2 to 2, 6 to 2, 4 to 5, 2 to 9, 6 to 9)
    )

    /**
     * HOOTLET - Buecherturm: vier Baende uebereinander, jeder gegen den naechsten versetzt.
     *
     * Ein Regal haette er auch verdient - aber ein Stapel sagt etwas anderes aus als ein Schrank:
     * Wer ordentlich einsortiert, hat die Buecher weggeraeumt; wer stapelt, ist noch dabei. Der
     * Versatz von Band zu Band ist dabei das Entscheidende - buendig uebereinander waere es ein
     * Klotz, versetzt ist es ein Stapel.
     */
    private val BOOKTOWER = Prop(
        width = 7, height = 12,
        art = (0..3).flatMap { book ->
            val top = book * 3
            val left = if (book % 2 == 0) 0 else 1
            hLine(left, left + 5, top) + hLine(left, left + 5, top + 2) +
                listOf(left to (top + 1), (left + 5) to (top + 1))
        },
        useSpot = -5 to 11,
        // Der oberste Band ist heruntergenommen - der Stapel wird sichtbar kuerzer.
        usedArt = (1..3).flatMap { book ->
            val top = book * 3
            val left = if (book % 2 == 0) 0 else 1
            hLine(left, left + 5, top) + hLine(left, left + 5, top + 2) +
                listOf(left to (top + 1), (left + 5) to (top + 1))
        }
    )

    /** GLOOP - niedriges Bodenregal; an ein hohes kaeme er nicht heran. */
    private val LOWSHELF = Prop(
        width = 9, height = 5,
        art = vLine(0, 0, 4) + vLine(8, 0, 4) +
            hLine(0, 8, 0) + hLine(0, 8, 2) + hLine(0, 8, 4) +
            listOf(2 to 1, 3 to 1, 5 to 1) +
            listOf(2 to 3, 4 to 3, 5 to 3, 6 to 3),
        useSpot = -5 to 4,
        usedArt = vLine(0, 0, 4) + vLine(8, 0, 4) +
            hLine(0, 8, 0) + hLine(0, 8, 2) + hLine(0, 8, 4) +
            listOf(2 to 1, 3 to 1) +
            listOf(2 to 3, 4 to 3, 5 to 3, 6 to 3)
    )

    // ---- Woran diese Kreatur zu Hause arbeitet ----
    //
    // Sechs Schreibtische, und jeder traegt sein eigenes Leuchtendes ([Prop.screenArea]): Monitor,
    // Runentafel, Globus, Seekarte. Das war noetig, weil das Arbeitszimmer sonst der gleichfoermigste
    // Raum der ganzen Wohnung geblieben waere - Fenster und Schreibtisch machen zusammen zwei
    // Drittel davon aus, und beide waren fuer alle dieselben.

    // ---- Die eigene Ecke: was diese Kreatur ihrer Art nach tut ----
    //
    // **Der Unterschied zum Arbeitsplatz ist der ganze Sinn dieses Ortes.** Dort geht sie einem
    // BERUF nach - Sternwarte, Turnhalle, Backstube, gewaehlt aus ihrem Leitsatz. Hier tut sie
    // etwas, weil sie ist, was sie ist: Ein Drache haemmert auf glühendes Eisen, ein Wesen ohne
    // feste Form dreht Ton, ein Wuestenfuchs graebt, eine Eule spielt in der Nacht.
    //
    // Alle tragen [Station.CRAFT], damit derselbe Ablauf (siehe PlayRoutines zu CREATIVITY) bei
    // jeder Kreatur aufgeht - was sie dort tut, unterscheidet sich, dass sie es tut, nicht.

    /** PUFFLING - Werkbank mit Lochwand. Er baut aus dem, was ihm unterkommt. */
    private val WORKBENCH = Prop(
        width = 13, height = 11,
        art = hLine(0, 12, 0) +                                   // Lochwand
            listOf(2 to 1, 2 to 2, 4 to 1, 6 to 1, 6 to 2, 6 to 3, 9 to 1, 10 to 1) +
            hLine(0, 12, 5) + hLine(0, 12, 6) +                   // Bankplatte
            vLine(1, 7, 10) + vLine(11, 7, 10) +                  // Beine
            hLine(1, 11, 8),                                      // Zwischenbrett
        useSpot = -1 to 10
    )

    /** STARLET - Anzuchtkasten unter einer Glashaube. Sie zieht heran, was spaeter draussen steht. */
    private val SEEDBED = Prop(
        width = 13, height = 9,
        art = hLine(2, 10, 0) + listOf(1 to 1, 11 to 1) +         // Haube
            listOf(0 to 2, 12 to 2) + vLine(0, 3, 5) + vLine(12, 3, 5) +
            listOf(3 to 4, 3 to 5, 6 to 3, 6 to 4, 6 to 5, 9 to 4, 9 to 5) +   // Setzlinge
            hLine(0, 12, 6) + hLine(0, 12, 7) +                   // Kastenrand
            listOf(1 to 8, 11 to 8),                              // Fuesse
        useSpot = -1 to 8
    )

    /**
     * WYRMLING - Esse mit Glutbett.
     *
     * Das Feuer ist ausgespart und wird von [ambient] gefuellt, wie jede leuchtende Flaeche in
     * dieser Welt (siehe [Prop.screenArea]) - dadurch darf die Glut heller sein als das Mauerwerk
     * um sie herum, statt eine weitere Silhouette zu sein.
     */
    private val FORGE = Prop(
        width = 13, height = 10,
        art = hLine(4, 8, 0) + listOf(3 to 1, 9 to 1) +           // Rauchfang
            listOf(2 to 2, 10 to 2) + hLine(1, 11, 3) +           // Haube
            vLine(0, 4, 9) + vLine(1, 4, 9) +                     // Wangen
            vLine(11, 4, 9) + vLine(12, 4, 9) +
            hLine(2, 10, 9),                                      // Sockel
        useSpot = -2 to 9,
        screenArea = hLine(3, 9, 8) + hLine(4, 8, 7) + listOf(6 to 6)
    )

    /** FENNEC - aufgegrabene Stelle mit einem freigelegten Krug. Buddeln ist artgerecht. */
    private val DIGSPOT = Prop(
        width = 13, height = 5,
        art = listOf(1 to 2) + hLine(0, 3, 3) + hLine(0, 4, 4) +  // Aushub links
            listOf(5 to 4, 6 to 4, 7 to 4) +                      // Grubensohle
            rect(5, 1, 7, 2) +                                    // der Krug darin
            listOf(11 to 2) + hLine(9, 12, 3) + hLine(8, 12, 4),  // Aushub rechts
        useSpot = -2 to 4
    )

    /** GLOOP - Toepferscheibe. Ein Wesen ohne feste Form gibt anderem eine; besser passt nichts. */
    private val POTTERY = Prop(
        width = 9, height = 7,
        art = hLine(2, 6, 0) + hLine(1, 7, 1) + hLine(2, 6, 2) +  // der Klumpen in Arbeit
            hLine(0, 8, 3) +                                      // Scheibe
            vLine(4, 4, 5) +                                      // Achse
            hLine(2, 6, 6),                                       // Fussplatte
        useSpot = -1 to 6
    )

    /** HOOTLET - Windharfe. Er ruft nachts; ein Instrument ist dasselbe mit anderen Mitteln. */
    private val WINDHARP = Prop(
        width = 11, height = 11,
        art = vLine(0, 0, 9) +                                    // Saeule
            listOf(1 to 0, 2 to 1, 3 to 2, 4 to 3, 5 to 4, 6 to 5, 7 to 6, 8 to 7) +   // Hals
            vLine(2, 2, 8) + vLine(4, 4, 8) + vLine(6, 6, 8) +    // drei Saiten
            hLine(0, 9, 9) + hLine(0, 9, 10),                     // Resonanzkoerper
        useSpot = -1 to 10
    )

    /** STARLET - schmales Pult, daneben waechst etwas ueber die Platte. */
    private val PLANTDESK = Prop(
        width = 12, height = 8,
        art = listOf(0 to 2, 1 to 1, 1 to 2, 2 to 2, 1 to 3) +   // Trieb auf der Platte
            hLine(4, 9, 0) + vLine(4, 1, 3) + vLine(9, 1, 3) + hLine(4, 9, 4) +   // Rahmen
            hLine(0, 11, 5) +                                    // Platte
            vLine(0, 6, 7) + vLine(11, 6, 7),                    // Wangen statt Beine
        useSpot = -1 to 7,
        screenArea = rect(5, 1, 8, 3)
    )

    /** WYRMLING - Steinblock, daran gelehnt eine Tafel mit gluehenden Zeichen. */
    private val SLABDESK = Prop(
        width = 12, height = 9,
        art = hLine(2, 8, 0) + vLine(2, 1, 4) + vLine(8, 1, 4) + hLine(2, 8, 5) +  // Tafel
            hLine(0, 11, 6) +                                    // Steinplatte
            rect(1, 7, 3, 8) + rect(8, 7, 10, 8),                // zwei Bloecke
        useSpot = -1 to 8,
        screenArea = rect(3, 1, 7, 4)
    )

    /** FENNEC - Kartentisch mit Leuchte darueber. Wer aufpasst, hat den Ueberblick vor sich liegen. */
    private val CHARTDESK = Prop(
        width = 12, height = 8,
        art = hLine(3, 8, 0) + listOf(5 to 1, 6 to 1) +          // Leuchte am Ausleger
            vLine(3, 1, 2) +
            hLine(0, 11, 3) + hLine(1, 10, 4) +                  // schraege Kartenflaeche
            vLine(1, 5, 7) + vLine(10, 5, 7),
        useSpot = -1 to 7,
        screenArea = rect(4, 1, 7, 2)
    )

    /** HOOTLET - Lesepult mit Leuchtglobus. Dieselbe Hand, die in der Sternwarte arbeitet. */
    private val LECTERN = Prop(
        width = 11, height = 10,
        art = hLine(1, 5, 0) + listOf(0 to 1, 6 to 1) +          // aufgeschlagenes Buch
            hLine(0, 6, 2) +
            listOf(7 to 1, 8 to 1, 9 to 1, 7 to 3, 8 to 3, 9 to 3) +   // Globusgestell
            hLine(0, 10, 4) +                                    // Pultplatte
            vLine(4, 5, 8) + hLine(2, 8, 9),                     // Mittelfuss
        useSpot = -1 to 9,
        screenArea = rect(7, 2, 9, 2) + listOf(8 to 1, 8 to 3)
    )

    /** GLOOP - niedriger Schreibtisch, die Scheibe flach aufgestellt. Siehe [LOWTABLE]. */
    private val LOWDESK = Prop(
        width = 12, height = 5,
        art = hLine(3, 9, 0) + listOf(3 to 1, 3 to 2, 9 to 1, 9 to 2) +
            hLine(0, 11, 3) +                                     // Platte
            vLine(1, 4, 4) + vLine(10, 4, 4),
        useSpot = -1 to 4,
        screenArea = rect(4, 1, 8, 2)
    )

    // ---- Was an der Wand haengt ----
    //
    // Das billigste Stueck Persoenlichkeit im ganzen Entwurf: fuenf mal fuenf Zellen, die kein
    // Ablauf je anfasst - und trotzdem der erste Unterschied, den man beim Hinsehen bemerkt.

    /** STARLET - Rankgeruest. Bei ihr waechst auch drinnen etwas. */
    private val TRELLIS = Prop(
        width = 7, height = 6,
        art = vLine(0, 0, 5) + vLine(3, 0, 5) + vLine(6, 0, 5) +
            hLine(0, 6, 0) + hLine(0, 6, 3) +
            listOf(1 to 1, 2 to 2, 4 to 4, 5 to 2)                // Ranken
    )

    /** WYRMLING - Ritzstein. Wer hortet, schreibt auch mit. */
    private val TABLET = Prop(
        width = 5, height = 5,
        art = hLine(0, 4, 0) + hLine(0, 4, 4) + vLine(0, 1, 3) + vLine(4, 1, 3) +
            listOf(1 to 1, 3 to 1, 2 to 2, 2 to 3)
    )

    /** HOOTLET - Sternkarte. Dieselbe Hand, die in der Sternwarte arbeitet. */
    private val STARMAP = Prop(
        width = 7, height = 5,
        art = hLine(0, 6, 0) + hLine(0, 6, 4) + vLine(0, 1, 3) + vLine(6, 1, 3) +
            listOf(2 to 1, 5 to 1, 4 to 2, 1 to 3, 3 to 3)
    )

    /**
     * FENNEC - Sanduhr.
     *
     * Zuerst hing bei ihm ein Fenster an der Wand ("am Fenster haelt er Ausschau") - das ging in
     * jedem Zimmer auf, nur nicht im Arbeitszimmer: Dort haengt ohnehin schon eines, und zwei
     * Fenster in einem Raum, von denen nur eines einen Himmel zeigt, sehen nach Versehen aus. Eine
     * Sanduhr trifft ihn ohnehin genauer: Er ist der, der auf die Gewohnheiten aufpasst.
     */
    private val SANDGLASS = Prop(
        width = 5, height = 5,
        art = hLine(0, 4, 0) + hLine(1, 3, 1) + listOf(2 to 2) + hLine(1, 3, 3) + hLine(0, 4, 4)
    )

    /** GLOOP - Moosfleck an der Wand. Er wischt ihn nicht weg; das ist der Punkt. */
    private val MOSS = Prop(
        width = 6, height = 3,
        art = listOf(1 to 0, 2 to 0, 4 to 0) + hLine(0, 5, 1) +
            listOf(0 to 2, 1 to 2, 3 to 2, 4 to 2, 5 to 2)
    )

    /**
     * **Kochstelle: Dreibein mit Kessel ueber einem Steinring.**
     *
     * Das Gegenstueck zur Kuechenzeile - und der Grund, warum sich zwei Kuechen ueberhaupt
     * unterscheiden koennen. Eine Zeile mit Spuele und Kochfeld ist mit sechzehn Zellen Breite
     * das groesste Moebelstueck der ganzen Wohnung; solange alle dieselbe hatten, war die Haelfte
     * jeder Kueche dieselbe Kueche, ganz gleich, was sonst darin stand. Drei Kreaturen leben wild
     * genug, dass eine Einbaukueche bei ihnen ohnehin fehl am Platz war.
     */
    private val COOKFIRE = Prop(
        width = 13, height = 8,
        art = listOf(6 to 0) +                                    // Spitze des Dreibeins
            listOf(5 to 1, 4 to 2, 4 to 3, 3 to 4, 3 to 5, 2 to 6, 2 to 7) +
            listOf(7 to 1, 8 to 2, 8 to 3, 9 to 4, 9 to 5, 10 to 6, 10 to 7) +
            rect(5, 3, 7, 5) + listOf(4 to 4) +                   // Kessel am Haken
            hLine(3, 9, 7)                                        // Steinring am Boden
    )

    /** STARLET - offene Anrichte: Platte, darunter ein Brett voller Toepfe statt Schranktueren. */
    private val OPENCOUNTER = Prop(
        width = 13, height = 8,
        art = hLine(0, 12, 0) +                                   // Arbeitsplatte
            vLine(0, 1, 7) + vLine(12, 1, 7) +                    // Wangen
            hLine(0, 12, 4) +                                     // Zwischenbrett
            listOf(2 to 2, 3 to 2, 2 to 3, 6 to 3, 9 to 2, 9 to 3) +
            listOf(3 to 5, 4 to 5, 8 to 5, 3 to 6, 8 to 6)
    )

    /** GLOOP - niedrige Arbeitsflaeche mit ausgespartem Becken. Siehe [LOWTABLE]. */
    private val LOWCOUNTER = Prop(
        width = 13, height = 4,
        art = (hLine(0, 12, 0) - listOf(2 to 0, 3 to 0, 4 to 0)) +   // Platte, Becken ausgespart
            listOf(2 to 1, 4 to 1) + hLine(2, 4, 2) +                // Beckenmulde
            vLine(0, 1, 3) + vLine(12, 1, 3) + hLine(0, 12, 3)
    )

    /** STARLET - Kraeuterbuendel an einer Schnur. Sie trocknet, was sie gezogen hat. */
    private val HERBS = Prop(
        width = 7, height = 5,
        art = hLine(0, 6, 0) +                                    // Schnur
            vLine(1, 1, 4) + listOf(0 to 2, 2 to 2) +             // drei Buendel
            vLine(4, 1, 3) + listOf(3 to 2, 5 to 2) +
            vLine(6, 1, 2)
    )

    /** Aufgehaengte Vorraete - das Gegenstueck zum Haengeregal in einer Kueche ohne Schraenke. */
    private val HANGING = Prop(
        width = 8, height = 4,
        art = hLine(0, 7, 0) +                                    // Stange
            vLine(1, 1, 3) + vLine(3, 1, 2) + vLine(5, 1, 3) + listOf(6 to 1, 6 to 2)
    )

    // ---- Das Kleinzeug ----

    /** WYRMLING - Kiste. */
    private val CRATE = Prop(
        width = 5, height = 4,
        art = hLine(0, 4, 0) + hLine(0, 4, 3) + vLine(0, 1, 2) + vLine(4, 1, 2) +
            listOf(1 to 1, 2 to 2, 3 to 1)
    )

    /** FENNEC - Vorratskrug. */
    private val JAR = Prop(
        width = 4, height = 5,
        art = hLine(1, 2, 0) + hLine(0, 3, 1) + hLine(0, 3, 2) + hLine(0, 3, 3) + hLine(1, 2, 4)
    )

    /** GLOOP - Schalen, die herumstehen. Er raeumt sie nicht weg. */
    private val BOWL = Prop(
        width = 5, height = 3,
        art = listOf(1 to 0, 2 to 0) + listOf(0 to 1, 4 to 1) + hLine(0, 4, 2)
    )

    /**
     * **Wie diese Kreatur wohnt** - die eine Stelle, an der die ganze Wohnung beschrieben ist.
     *
     * Bis hierher waren nur Schlafzimmer, Arbeitsplatz und das Draussen artspezifisch. Alles
     * uebrige - Wohnzimmer, Kueche, Leseecke, Bad, Schreibtisch - war fuer alle sechs Kreaturen
     * bis auf die Zeile genau dasselbe Zimmer. Beim Wechsel des Avatars aenderte sich die Figur,
     * und die Wohnung sah ihr ungeruehrt zu.
     *
     * **Nicht sechs Wohnungen gezeichnet, sondern EINE Handschrift je Kreatur.** Die Zimmer
     * behalten ihre erprobte Anordnung (dieselben Anker, dieselben Plaetze, dieselben Ablaeufe);
     * ausgetauscht wird, WAS dort steht. Ein Zimmer bleibt damit ein Zimmer, das man kennt, und
     * ist trotzdem unverwechselbar seins. Wer eine siebte Kreatur ergaenzt, fuellt genau diese
     * elf Felder aus - und hat eine vollstaendige Wohnung, ohne einen einzigen Raum anzufassen.
     *
     * Zwei Sorten Eintrag stecken hier nebeneinander, und der Unterschied ist wichtig:
     * - Was der KOERPER erzwingt ([bath], [table], [couch], [desk], [shelf]) - GLOOP hat keine
     *   Beine, HOOTLET ist ein Vogel. Das ist nicht verhandelbar.
     * - Was der CHARAKTER nahelegt ([light], [wall], [accent], [screen]) - hier waere auch eine
     *   andere Wahl vertretbar; sie muss nur zu dem passen, was die Figur sonst ausmacht.
     */
    private data class Home(
        /** Wohnzimmer: der grosse Sitzplatz ([Station.SEAT]). */
        val couch: Prop,
        /** Wohnzimmer: das Leuchtende, um das man abends herumsitzt ([Station.TV]). */
        val screen: Prop,
        /** Leseecke: der persoenliche Sitzplatz ([Station.SEAT]). */
        val seat: Prop,
        /** Leseecke: wo das steht, was sie immer wieder hervorholt ([Station.BOOKSHELF]). */
        val shelf: Prop,
        /** Die Leuchte, die sie anmacht ([Station.LAMP]). */
        val light: Prop,
        /** Ihr Schreibtisch ([Station.DESK]). */
        val desk: Prop,
        /** Wo sie sich waescht ([Station.TUB]). */
        val bath: Prop,
        /** Woran sie isst ([Station.TABLE]). */
        val table: Prop,
        /** Was in der Kueche an der Wand haengt. */
        val larder: Prop,
        /** Woran sie kocht - Kuechenzeile, Kochstelle oder niedrige Arbeitsflaeche. */
        val counter: Prop,
        /** Was an der Wand haengt. */
        val wall: Prop,
        /** Das Kleinzeug, das in jedem ihrer Zimmer herumsteht. */
        val accent: Prop,
        /** Woran sie in ihrer eigenen Ecke arbeitet ([Station.CRAFT]). */
        val craft: Prop
    )

    private fun homeOf(species: AvatarSpecies): Home = when (species) {
        // Die Einstiegsfigur behaelt die gewohnte Wohnung - dieselbe Ueberlegung wie beim Park
        // (siehe [habitatPlacements]): Wer die App zum ersten Mal oeffnet, soll nichts deuten
        // muessen. Alle uebrigen Wohnungen sind gegen DIESE hier gebaut.
        AvatarSpecies.PUFFLING -> Home(
            couch = SOFA, screen = TV, seat = CHAIR, shelf = BOOKSHELF, light = LAMP,
            desk = DESK, bath = TUB, table = TABLE, larder = SHELF, counter = COUNTER,
            wall = PICTURE, accent = PLANT, craft = WORKBENCH
        )
        // Bei ihr waechst alles: Rankgeruest statt Bild, eine hohe Blume statt der Topfpflanze,
        // und das Licht ist Papier statt Lampenschirm.
        AvatarSpecies.STARLET -> Home(
            couch = SWINGSEAT, screen = TV, seat = POUF, shelf = PLANTSTAND, light = PAPERLANTERN,
            desk = PLANTDESK, bath = TUB, table = CROSSTABLE, larder = HERBS, counter = OPENCOUNTER,
            wall = TRELLIS, accent = FLOWER, craft = SEEDBED
        )
        // Stein, Feuer und Kisten. Kein einziges Polster - er thront, er lehnt sich nicht an.
        AvatarSpecies.WYRMLING -> Home(
            couch = STONESEAT, screen = FIREPIT, seat = LEDGE, shelf = HOARD, light = BRAZIER,
            desk = SLABDESK, bath = STONETUB, table = SLABTABLE, larder = HANGING, counter = COOKFIRE,
            wall = TABLET, accent = CRATE, craft = FORGE
        )
        // Kissen, Krug, Laterne, Ofen: eine Hoehle, in der es warm ist. Dazu die Sanduhr an der
        // Wand und der Kartentisch - er ist der, der aufpasst.
        AvatarSpecies.FENNEC -> Home(
            couch = DIVAN, screen = STOVE, seat = CUSHIONS, shelf = BASKETS, light = LANTERN,
            desk = CHARTDESK, bath = SANDBATH, table = LOWROUND, larder = HERBS, counter = COOKFIRE,
            wall = SANDGLASS, accent = JAR, craft = DIGSPOT
        )
        // ALLES niedrig - Bank, Regal, Tisch, Wanne, Schreibtisch, Arbeitsflaeche. Bei ihm ist
        // das keine Geschmacksfrage: Ohne Beine kommt er an nichts Hohes heran. In der Kueche
        // haengt deshalb auch kein Regal, sondern derselbe Moosfleck wie ueberall sonst - was er
        // nicht erreichen kann, braucht er auch nicht aufzuhaengen.
        AvatarSpecies.GLOOP -> Home(
            couch = FLOORSOFA, screen = TV, seat = CUSHION, shelf = LOWSHELF, light = GLOWCAP,
            desk = LOWDESK, bath = LOWTUB, table = LOWTABLE, larder = MOSS, counter = LOWCOUNTER,
            wall = MOSS, accent = BOWL, craft = POTTERY
        )
        // Holz, Kerzen, Buecher, Sternkarte. Ein Vogel sitzt AUF Dingen, deshalb der Balken
        // statt des Sofas.
        AvatarSpecies.HOOTLET -> Home(
            couch = ROOST, screen = HEARTH, seat = PERCH, shelf = BOOKTOWER, light = CANDLES,
            desk = LECTERN, bath = BIRDBATH, table = STUMPTABLE, larder = SHELF, counter = COOKFIRE,
            wall = STARMAP, accent = BOOKS, craft = WINDHARP
        )
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
        useSpot = -6 to 8,
        screenArea = rect(1, 1, 9, 5)
    )

    // ---- Was sich ansammelt (siehe [Acquisition]) ----
    //
    // **Zwoelf kleine Formen, von denen jeder Nutzer hoechstens drei zu sehen bekommt** - eine je
    // Stufe seines Pfades. Das klingt nach Verschwendung und ist das Gegenteil: Gerade weil man
    // nur den eigenen Weg sieht, ist das, was da steht, eine Aussage ueber DIESEN Verlauf und
    // nicht Ausstattung, die ohnehin jeder bekommt.
    //
    // Alle bewusst klein (fuenf bis elf Zellen): Sie kommen zu einer fertig eingerichteten Wohnung
    // DAZU und muessen zwischen dem Platz finden, was schon da steht.

    /** Rucksack - das erste Stueck des Aufbrechers. */
    private val PROP_BACKPACK = Prop(
        width = 6, height = 7,
        art = hLine(1, 4, 0) +                                   // Deckel
            hLine(0, 5, 1) + rect(0, 2, 5, 5) +
            listOf(2 to 3, 3 to 3) +                             // Riemen angedeutet
            hLine(1, 4, 6)
    )

    /** Landkarte an der Wand - wer unterwegs ist, merkt sich, wo er war. */
    private val PROP_WALLMAP = Prop(
        width = 9, height = 6,
        art = hLine(0, 8, 0) + hLine(0, 8, 5) + vLine(0, 1, 4) + vLine(8, 1, 4) +
            // Eingezeichneter Weg quer durch - erst der macht aus dem Rechteck eine Karte.
            listOf(1 to 3, 2 to 2, 3 to 2, 4 to 3, 5 to 3, 6 to 2, 7 to 1)
    )

    /** Tretroller - das Fortbewegungsmittel, das er sich irgendwann zulegt. */
    private val PROP_SCOOTER = Prop(
        width = 11, height = 9,
        art = hLine(3, 6, 0) +                                   // Lenker
            vLine(5, 1, 5) +                                     // Lenkstange
            hLine(1, 9, 6) +                                     // Trittbrett
            listOf(1 to 7, 2 to 8, 3 to 7) +                     // Vorderrad
            listOf(7 to 7, 8 to 8, 9 to 7)                       // Hinterrad
    )

    /** Futternapf - das erste Stueck dessen, der sich kuemmert. */
    private val PROP_FEEDBOWL = Prop(
        width = 5, height = 3,
        art = listOf(1 to 0, 3 to 0) + listOf(0 to 1, 4 to 1) + hLine(0, 4, 2)
    )

    /** Koerbchen - noch leer, aber es steht bereit. */
    private val PROP_PETBASKET = Prop(
        width = 9, height = 4,
        art = listOf(0 to 0, 8 to 0) + hLine(0, 8, 1) +
            hLine(0, 8, 2) + hLine(1, 7, 3)
    )

    /**
     * Das Haustier - zusammengerollt und schlafend.
     *
     * **Nicht dasselbe wie das Tier, das durchs Bild huscht** ([housePet]): Das kommt vorbei und
     * geht wieder, es gehoert niemandem. Dieses hier liegt im Schlafzimmer und bleibt. Der
     * Unterschied ist genau der zwischen "hier laeuft etwas herum" und "ich habe jemanden".
     */
    private val PROP_SLEEPING_PET = Prop(
        width = 9, height = 4,
        art = hLine(2, 6, 0) + hLine(1, 7, 1) + hLine(0, 8, 2) +
            // Schwanz, der sich um den Koerper legt - daran erkennt man ein schlafendes Tier.
            hLine(0, 8, 3) + listOf(8 to 1)
    )

    /** Zusammengelegte Decke - das erste Stueck des Stillen. */
    private val PROP_BLANKET = Prop(
        width = 7, height = 3,
        art = hLine(0, 6, 0) + hLine(0, 6, 1) + hLine(1, 5, 2)
    )

    /** Windspiel - haengt und bewegt sich, wenn die Tuer geht. */
    private val PROP_MOBILE = Prop(
        width = 7, height = 6,
        art = hLine(1, 5, 0) +                                   // Traeger
            vLine(1, 1, 3) + vLine(3, 1, 4) + vLine(5, 1, 2) +   // Faeden
            listOf(1 to 4, 3 to 5, 5 to 3)                       // Anhaenger
    )

    /** Ein Glas mit Licht darin - das letzte Stueck des Stillen. */
    private val PROP_STARJAR = Prop(
        width = 5, height = 6,
        art = hLine(1, 3, 0) + hLine(0, 4, 1) +
            vLine(0, 2, 4) + vLine(4, 2, 4) + hLine(0, 4, 5),
        lightAt = 2 to 3
    )

    /** Werkzeugkiste - das erste Stueck des Machers. */
    private val PROP_TOOLBOX = Prop(
        width = 7, height = 5,
        art = listOf(3 to 0) + hLine(2, 4, 1) +                  // Griff
            hLine(0, 6, 2) + rect(0, 3, 6, 4) - listOf(3 to 3)
    )

    /** Eine Apparatur mit Zahnrad - man sieht ihr nicht an, wozu sie gut ist. Das ist der Punkt. */
    private val PROP_CONTRAPTION = Prop(
        width = 9, height = 9,
        art = hLine(2, 6, 0) +                                   // Zahnrad oben
            listOf(1 to 1, 7 to 1) + listOf(1 to 2, 7 to 2) + hLine(2, 6, 3) +
            vLine(4, 4, 5) +                                     // Achse
            hLine(0, 8, 6) + rect(1, 7, 7, 8)                    // Sockel
    )

    /**
     * Ein selbstgebautes Geraet mit Leuchtanzeige - das letzte Stueck des Machers.
     *
     * Das einzige der zwoelf, das LEUCHTET (siehe [Prop.lightAt] und [PROP_STARJAR] fuer das
     * zweite): Nachts weicht der Raum zurueck und das Licht bleibt stehen - wer so weit gekommen
     * ist, hat etwas gebaut, das man auch im Dunkeln sieht.
     */
    private val PROP_SIGNALRIG = Prop(
        width = 9, height = 11,
        art = vLine(4, 0, 2) + listOf(3 to 0, 5 to 0) +          // Antenne
            hLine(0, 8, 3) +                                     // Deckplatte
            vLine(0, 4, 9) + vLine(8, 4, 9) +                    // Gehaeuse
            hLine(0, 8, 10) +
            hLine(1, 7, 6),                                      // Kante ueber der Anzeige
        lightAt = 4 to 8
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

    /**
     * **Jede Leuchte im Raum, gleich welche** - Lampenschirm, Feuerschale, Leuchtpilz, Kerze,
     * Laterne, Lampion.
     *
     * Vorher suchte die Umgebungsanimation in jedem Raum GENAU EINE bestimmte Requisite
     * (`prop === LAMP`) und rechnete mit deren Massen. Das hielt genau so lange, wie alle gleich
     * wohnten. Jetzt fragt sie nicht mehr nach dem Gegenstand, sondern nach der Eigenschaft
     * ([Prop.lightAt]) - wer eine neue Leuchte zeichnet, traegt dort ein, wo ihr Kern sitzt, und
     * bekommt Schein und Bodenlicht geschenkt.
     *
     * Der Schalter wirkt nur auf die Leuchte, die eine Station traegt: Was der Ablauf anfassen
     * kann, geht aus; was bloss im Raum steht, brennt weiter.
     */
    private fun standingLights(
        placements: List<Placement>,
        widthCells: Int,
        floorY: Int,
        phase: Int,
        lampOn: Boolean
    ): List<SceneCell> = placements.flatMap { placement ->
        val at = placement.prop.lightAt ?: return@flatMap emptyList()
        if (placement.station == Station.LAMP && !lampOn) return@flatMap emptyList()
        val cx = originX(placement, widthCells) + at.first
        val cy = originY(placement, floorY) + at.second
        // Der Takt haengt an der Position, damit zwei Leuchten im selben Bild nicht im Gleichschritt
        // atmen - im Gleichtakt wirken sie wie ein Blinker, versetzt wie zwei Flammen.
        val pulse = if (beat(phase + cx, LAMP_TICKS) % 4 < 2) GLOW else GLOW - 400
        // Nach unten hin schwaecher werdend: Licht, das nur EINE Zeile breit unter dem Schirm
        // sitzt, liest sich als weitere Silhouette, nicht als Schein.
        listOf(
            SceneCell(cx - 1, cy, pulse / 3, isLight = true),
            // Der Kern der Leuchte ist das Glanzlicht des Zimmers.
            SceneCell(cx, cy, HIGHLIGHT, isLight = true),
            SceneCell(cx + 1, cy, pulse / 3, isLight = true),
            SceneCell(cx, cy + 1, pulse / 2, isLight = true)
        ) + lightPool(cx, floorY, radius = 5, peak = pulse)
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
     * **Regen und Schnee** - draussen ueber die ganze Breite, drinnen im Fensterausschnitt.
     *
     * Der schoenste Teil ist der zweite: Regen, den man vom Sessel aus durchs Fenster sieht.
     * Deshalb wird hier nicht "wenn draussen" gefragt, sondern "wo ist Himmel zu sehen" - im Park
     * ist das der ganze obere Bildteil, in der Leseecke ein Rechteck von sieben mal sechs Zellen.
     * Dass beides derselbe Regen ist, macht aus zwei Kulissen einen Ort.
     *
     * **Regen faellt schnell und schraeg, Schnee langsam und senkrecht.** Das ist der ganze
     * Unterschied in der Darstellung, und er genuegt: Bei dieser Aufloesung erkennt man Wetter
     * nicht an der Form eines Tropfens, sondern an seinem Verhalten.
     *
     * Die Tropfen stehen auf [STRUCTURE], also ganz unten in der Helligkeitsstaffelung - Wetter
     * liegt VOR allem und wuerde bei jeder hoeheren Stufe das Bild uebernehmen. Man soll es
     * bemerken, ohne hinzusehen, und nicht dabei zusehen muessen.
     */
    private fun weather(
        place: Place,
        placements: List<Placement>,
        phase: Int,
        widthCells: Int,
        floorY: Int
    ): List<SceneCell> {
        val today = PlayWeather.current()
        if (!today.isFalling || widthCells <= 0) return emptyList()

        // Wo Himmel zu sehen ist: draussen alles oberhalb des Bodens, drinnen das Fenster.
        val (left, right, top, bottom) = if (isOutdoors(place)) {
            Rect(0, widthCells - 1, 0, floorY - 1)
        } else {
            val window = placements.firstOrNull { it.prop === WINDOW } ?: return emptyList()
            val ox = originX(window, widthCells)
            val oy = originY(window, floorY)
            // Nur die OEFFNUNG, nicht der Rahmen - Regen vor dem Mauerwerk waere Regen im Zimmer.
            Rect(ox + 1, ox + window.prop.width - 2, oy + 1, oy + window.prop.height - 2)
        }
        if (right < left || bottom < top) return emptyList()

        val height = bottom - top + 1
        val snow = today == PlayWeather.SNOW
        // **Ein Fenster ist kein Himmel.** Die Dichte richtet sich nach der Flaeche: In einem
        // Ausschnitt von fuenf mal vier Zellen fuellte dieselbe Menge Tropfen wie draussen das
        // ganze Fenster und liess es zugefroren aussehen. Ein paar wenige genuegen dort - man
        // sieht ohnehin nur einen Ausschnitt des Schauers.
        val tight = height < 8
        val spacing = if (tight) 2 else 3
        val speed = if (snow) 1 else 3
        // MEHRERE je Spalte, nicht einer. Der erste Entwurf setzte einen Tropfen alle vier
        // Spalten, jeden auf einer anderen Hoehe - das las sich als verstreute Punkte, nicht als
        // Regen. Erst wenn in derselben Spalte mehrere untereinander haengen, entsteht ein
        // Schauer statt eines Sternenhimmels.
        val perColumn = when {
            tight -> 1
            snow -> 3
            else -> 3
        }

        return (left..right).filter { (it - left) % spacing == 0 }.flatMap { x ->
            // Jede Spalte auf einem eigenen Versatz, sonst faellt alles in einer Reihe - ein
            // Vorhang statt eines Schauers.
            //
            // Durchmischt statt linear: Ein einfaches Vielfaches der Spaltennummer ergab ein
            // sauberes Diagonalgitter - bei Regen faellt das nicht auf, weil er ohnehin schraeg
            // zieht, aber Schnee sah dadurch aus wie ein gedrucktes Muster statt wie Schneefall.
            val offset = ((x * 7) xor (x * x * 13)) % height
            (0 until perColumn).flatMap { n ->
                val fallen = (beat(phase, 1) * speed + offset + n * height / perColumn) % height
                // Regen zieht schraeg: Je tiefer der Tropfen, desto weiter ist er nach links
                // gewandert. Schnee faellt senkrecht und schwankt nur.
                val drift = if (snow) ((fallen / 3) % 2) else -(fallen / 4)
                val cx = x + drift
                // **Regen als kurzer Strich, Schnee als einzelnes Korn.** Bei dieser Aufloesung
                // gibt es keine Tropfenform - was Regen von Schnee unterscheidet, ist die SPUR:
                // Ein zwei Zellen langer Strich liest sich als etwas, das faellt, ein einzelner
                // Punkt als etwas, das schwebt. Damit reicht dieselbe Rechnung fuer beides.
                val length = if (snow) 1 else 2
                (0 until length).mapNotNull { d ->
                    val cy = top + fallen + d
                    if (cx < left || cx > right || cy > bottom) null
                    else SceneCell(
                        cx, cy,
                        when {
                            snow -> FURNITURE
                            // Der Kopf des Strichs heller als sein Schweif - das gibt ihm eine
                            // Richtung, und Regen ohne Richtung ist Nebel.
                            d == 0 -> STRUCTURE * 2
                            else -> STRUCTURE
                        }
                    )
                }
            }
        }
    }

    /** Der Ausschnitt, in dem Wetter zu sehen ist - draussen das Bild, drinnen das Fenster. */
    private data class Rect(val left: Int, val right: Int, val top: Int, val bottom: Int)

    /**
     * **Abends gehen in den Haeusern die Lichter an.**
     *
     * Das ist das Herzstueck der Strasse und der Grund, warum die Fassaden ueberhaupt Fenster
     * haben. Tagsueber sind es dunkle Umrisse - Kulisse eben. Sobald es Abend wird, leuchten
     * einzelne Fenster auf, und in diesem Augenblick wird aus einer Reihe Silhouetten eine
     * Strasse, in der Leute wohnen. Man sieht niemanden, und trotzdem ist plötzlich jemand da.
     *
     * **Nicht alle Fenster, und immer dieselben.** Ein voll erleuchtetes Haus sieht aus wie ein
     * Buerogebaeude; wechselnd erleuchtete Fenster sehen aus wie eine Leuchtreklame. Welche
     * brennen, wird deshalb aus der Fensterposition selbst gerechnet - dieselbe Wohnung ist bei
     * jedem Hinsehen erleuchtet, und erst dadurch wirkt es wie Gewohnheit statt wie Zufall.
     *
     * Nachts brennen weniger als abends. Auch das ist kein Effekt, sondern eine Aussage: Die
     * Nachbarn gehen schlafen.
     */
    private fun litWindows(
        place: Place,
        placements: List<Placement>,
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase,
        phase: Int
    ): List<SceneCell> {
        // Die Stadt hat dieselben Fassaden wie die Strasse (siehe [furnishing]) und geht abends
        // genauso an.
        if (place != Place.STREET && place != Place.CITY) return emptyList()
        val night = dayPhase == PlayAmbientActivity.DayPhase.NIGHT
        if (!night && dayPhase != PlayAmbientActivity.DayPhase.EVENING) return emptyList()

        return placements.filter { it.prop === HOUSE || it.prop === HOUSE_LOW }
            .flatMapIndexed { houseIndex: Int, house: Placement ->
                val ox = originX(house, widthCells)
                val oy = originY(house, floorY)
                // Die Fensteroeffnungen liegen zwischen den Laibungen der Requisite - hier als
                // Paare (Spalte, Zeilen) beschrieben, passend zu HOUSE bzw. HOUSE_LOW.
                val openings = if (house.prop === HOUSE) {
                    listOf(4 to (2..4), 9 to (2..4), 4 to (7..9), 9 to (7..9))
                } else {
                    listOf(3 to (2..5), 8 to (2..5))
                }
                openings.filterIndexed { index, _ ->
                    // Aus Haus- und Fensternummer gerechnet: fest, aber ungleichmaessig verteilt.
                    val awake = (houseIndex * 3 + index * 5) % (if (night) 4 else 3) != 0
                    awake
                }.flatMap { (col, rows) ->
                    rows.map { row ->
                        SceneCell(
                            ox + col, oy + row,
                            // Warmes, ruhiges Licht - deutlich unter der Laterne, die den
                            // Vordergrund traegt. Ein Fenster, das so hell strahlt wie eine
                            // Strassenlaterne, holt den Hintergrund nach vorn.
                            if (night) GLOW - 900 else GLOW - 500,
                            isLight = true
                        )
                    }
                }
            }
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
        species: AvatarSpecies,
        acquisitions: Set<Acquisition> = emptySet()
    ): List<SceneCell> {
        // Dieselbe Auswahl wie beim Zeichnen: Ein Fenster, das auf schmalem Bild weggefallen ist,
        // darf auch keinen Mond mehr bekommen.
        val placements = fitting(placementsFor(place, species, acquisitions), widthCells, floorY)
        return when (place) {
            // Himmelskoerper im Fenster - wechselt mit der Tageszeit, damit die Kulisse
            // dieselbe Uhrzeit "kennt" wie der Tagesablauf des Avatars.
            Place.BEDROOM -> skyInWindow(placements, widthCells, floorY, dayPhase, phase) +
                standingLights(placements, widthCells, floorY, phase, lampOn = true)

            // Fenster wie im Schlafzimmer, dazu die flackernde Mattscheibe: Der Monitor ist als
            // leerer Rahmen gezeichnet ([Prop.screenArea]) und wird erst hier gefuellt - dadurch
            // ist das Licht eine eigene Ebene und kann heller sein als das Geraet drumherum.
            Place.DESK -> {
                val sky = skyInWindow(placements, widthCells, floorY, dayPhase, phase) +
                    standingLights(placements, widthCells, floorY, phase, lampOn = true)
                // Ueber die STATION statt ueber die Requisite: Welcher Schreibtisch dort steht,
                // haengt an der Kreatur (siehe [Home]) - was ihn zum Schreibtisch macht, ist der
                // Platz, den ein Ablauf ansteuern kann.
                val desk = placements.firstOrNull { it.station == Station.DESK } ?: return sky
                val ox = originX(desk, widthCells)
                val oy = originY(desk, floorY)
                // Zwei Helligkeiten im Wechsel statt an/aus: ein hart blinkender Bildschirm
                // saehe nach Defekt aus, ein leicht atmender nach Betrieb.
                val lit = if (beat(phase, SCREEN_TICKS) % 2 == 0) GLOW else GLOW - 550
                val screen = desk.prop.screenArea.map { (col, row) ->
                    SceneCell(ox + col, oy + row, lit, isLight = true)
                }
                sky + screen
            }

            // Dampf steigt ueber der Kanne auf und loest sich oben auf. Ueber die Station gesucht,
            // damit auch die Steinplatte des Drachen und der niedrige Tisch des Schleims dampfen -
            // beide tragen ihren Topf an derselben Stelle.
            Place.KITCHEN -> {
                val table = placements.firstOrNull { it.station == Station.TABLE } ?: return emptyList()
                val vapor = table.prop.vaporAt ?: return emptyList()
                val ox = originX(table, widthCells) + vapor.first
                val oy = originY(table, floorY) + vapor.second
                val rise = beat(phase, STEAM_TICKS) % 3
                listOf(
                    SceneCell(ox, oy - 1 - rise, GLOW - rise * 500, isLight = true),
                    SceneCell(ox + 1, oy - 2 - rise, (GLOW - rise * 600).coerceAtLeast(0), isLight = true)
                )
            }

            // Lichtkegel unter dem Schirm, langsam pulsierend - nur bei eingeschalteter Lampe.
            // Ausgeschaltet bleibt allein die Silhouette stehen, und die Ecke wird spuerbar
            // dunkler: Erst dass das Ausschalten etwas WEGNIMMT, macht den Schalter zu einem
            // Schalter statt zu einer Geste ohne Folgen.
            Place.NOOK -> standingLights(placements, widthCells, floorY, phase, lampOn)

            // Die eigene Ecke: ihre Leuchte, dazu das Glutbett der Esse - die einzige Werkstatt,
            // die selbst leuchtet. Gezeichnet wird, was die Requisite an leuchtender Flaeche
            // mitbringt; wer keine hat, bekommt hier auch keine.
            Place.CRAFT -> {
                val lights = standingLights(placements, widthCells, floorY, phase, lampOn = true)
                val work = placements.firstOrNull { it.station == Station.CRAFT } ?: return lights
                if (work.prop.screenArea.isEmpty()) return lights
                val ox = originX(work, widthCells)
                val oy = originY(work, floorY)
                // Langsamer als ein Bildschirm und ohne harten Wechsel: Glut atmet, sie flackert
                // nicht.
                val lit = if (beat(phase, LAMP_TICKS) % 4 < 2) GLOW else GLOW - 700
                lights + work.prop.screenArea.map { (col, row) ->
                    SceneCell(ox + col, oy + row, lit, isLight = true)
                } + lightPool(ox + work.prop.width / 2, floorY, radius = 6, peak = lit - 900)
            }

            // Draussen: eine Wolke zieht durchs Bild, nachts stattdessen Sterne.
            //
            // Alle fuenf Orte unter freiem Himmel teilen sich diesen Himmel - er gehoert zum
            // WETTER, nicht zum Ort. Getrennt gepflegte Himmel waeren die sicherste Art, dass
            // ueber dem Park bald andere Wolken zoegen als ueber der Strasse.
            Place.PARK, Place.STREET, Place.FOREST, Place.MEADOW, Place.CITY -> {
                val skyY = (floorY - 13).coerceAtLeast(0)
                if (dayPhase == PlayAmbientActivity.DayPhase.NIGHT) {
                    // Nachts steht der Boden tief (siehe [floorFraction]) und darueber ist Platz
                    // fuer einen richtigen Himmel: ein Sternbild aus sieben Sternen, jeder auf
                    // einem eigenen Takt. Im Gleichtakt blinkend saehen sie aus wie eine
                    // Leuchtreklame; unabhaengig voneinander wie ein Nachthimmel.
                    val sky = (floorY * 0.62f).toInt()
                    // **Der Himmel nutzt die GANZE Breite** - anders als das Zimmer darunter, das
                    // eine feste Groesse hat (siehe [MAX_ROOM_CELLS]). Genau umgekehrt: Ein
                    // Schlafzimmer wird nicht groesser, weil man das Telefon dreht, ein Himmel
                    // schon. Er ist das einzige an dieser Welt, das keine Kanten hat.
                    //
                    // Die Sterne haengen deshalb an Bruchteilen der Breite statt an gezaehlten
                    // Spalten. Vorher waren es feste Abstaende ab einem Fuenftel - im Querformat
                    // draengte sich das Sternbild dadurch in der linken Bildhaelfte zusammen und
                    // liess rechts den halben Himmel leer.
                    fun at(fraction: Float): Int =
                        (widthCells * fraction).toInt().coerceIn(0, widthCells - 1)
                    listOf(
                        // Die vier hellen bilden ein wiedererkennbares Muster - erst dadurch wird
                        // aus verstreuten Punkten ein Sternbild. Ihre Abstaende zueinander bleiben
                        // anteilig gleich, damit es bei jeder Breite DASSELBE Sternbild ist.
                        SceneCell(at(0.20f), sky, twinkleOf(phase, 4, HIGHLIGHT), isLight = true),
                        SceneCell(at(0.31f), sky - 4, twinkleOf(phase, 6, GLOW), isLight = true),
                        SceneCell(at(0.44f), sky - 3, twinkleOf(phase, 5, GLOW), isLight = true),
                        SceneCell(at(0.53f), sky + 2, twinkleOf(phase, 7, GLOW), isLight = true),
                        // ... die uebrigen streuen lockerer und schwaecher ueber den Rest.
                        SceneCell(at(0.68f), sky - 7, twinkleOf(phase, 9, GLOW / 2), isLight = true),
                        SceneCell(at(0.79f), sky + 6, twinkleOf(phase, 11, GLOW / 2), isLight = true),
                        SceneCell(at(0.91f), sky + 3, twinkleOf(phase, 13, GLOW / 2), isLight = true),
                        // Zwei zusaetzliche ganz aussen: Sie fallen im Hochformat kaum auf und
                        // fuellen im Querformat genau die Ecken, die sonst leer blieben.
                        SceneCell(at(0.06f), sky - 6, twinkleOf(phase, 17, GLOW / 2), isLight = true),
                        SceneCell(at(0.97f), sky - 5, twinkleOf(phase, 19, GLOW / 2), isLight = true)
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
                    parkLantern(placements, widthCells, floorY, dayPhase) +
                    litWindows(place, placements, widthCells, floorY, dayPhase, phase)
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
                standingLights(placements, widthCells, floorY, phase, lampOn)
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

            // Wohnzimmer: der Fernseher flimmert, und sein Licht faellt in den Raum. Bei den drei
            // wild lebenden Kreaturen ist es kein Fernseher, sondern ein Feuer (siehe [HEARTH]) -
            // dieselbe Rechnung, andere Flaeche: Was leuchtet, sagt die Requisite selbst.
            Place.LIVING -> {
                val lights = standingLights(placements, widthCells, floorY, phase, lampOn = true)
                if (!tvOn) return lights
                val tv = placements.firstOrNull { it.station == Station.TV } ?: return lights
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
                lights + tv.prop.screenArea.map { (col, row) ->
                    SceneCell(ox + col, oy + row, lit, isLight = true)
                } + lightPool(ox + tv.prop.width / 2, floorY, radius = 7, peak = lit - 600)
            }
        }
    }
}
