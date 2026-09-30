package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayScene.Placement
import com.notime.glyphsim.matrix.PlayScene.Prop
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * **Der Weltausbau: mehr Natur, mehr Stadt, mehr Leben** (Auftrag des Nutzers vom 26.09.2026:
 * "Die Welt sollte aussehen wie eine richtige Welt").
 *
 * Bis hierher bestand die Welt aus Zimmern und einer Handvoll Aussenorten, die fast nur aus dem
 * Boden und drei, vier Requisiten bestanden. Ueber dem Boden lag ein halber Bildschirm leerer
 * Himmel, in der Stadt ging niemand spazieren, und der Laden war ein Regal und eine Kasse.
 *
 * Diese Datei bringt drei Dinge dazu, jedes als eigene Ebene der Kulisse:
 *
 * 1. **Fuenf neue Landschaften** - Dschungel, Gebirge, Sumpf, Ebene und Strand. Jede hat einen
 *    FERNEN Hintergrund ([background]: Bergketten, Blaetterdach, Meer, Huegel), eigenes Beiwerk
 *    mit einem Sitzplatz ([furnishing]) und eigenes Leben ([ambient]: Adler, Papageien,
 *    Gluehwuermchen, Moewen, Krabben, Windmuehle).
 * 2. **Leben in Stadt und Laden** ([midground]) - Passanten, die ihre Hunde ausfuehren, Autos
 *    und ein Bus auf der Strasse, Kundschaft mit Einkaufswagen im Laden. Diese Ebene liegt
 *    zwischen Hintergrund und Vordergrund: Die Leute gehen HINTER Bank und Laterne vorbei, und
 *    die Figur steht immer vor ihnen.
 * 3. **Ein richtiger Supermarkt** - eine Regalwand mit Getraenken, Dosen, Kartons, Kuehlregal und
 *    Gemuesekisten, Deckenlicht und Gangschilder ([shopBackWall]).
 *
 * ## Warum eine eigene Datei
 *
 * `PlayScene.kt` hat ueber viertausend Zeilen und darf laut Uebergabe nie ganz gelesen werden.
 * Alles hier ist neu und haengt an genau vier Stellen dort an (Einrichtung, Boden, Hintergrund,
 * Umgebung). Wer an der neuen Welt weiterbaut, muss die alte Datei nicht oeffnen.
 *
 * ## Die Helligkeitsstaffelung
 *
 * Alles Neue liegt UNTER der bisherigen Kulisse: Fernes ([FAR]) deutlich unter dem Boden, Leute
 * und Fahrzeuge ([MID]) unter den Moebeln. Die Figur bleibt die hellste Erscheinung im Bild -
 * genau wie es die Klassendoku von [PlayScene] fuer jede Ebene verlangt. Ein Passant, der so
 * hell ist wie der Avatar, waere keine Belebung, sondern Konkurrenz.
 *
 * ## Alles aus der Position gerechnet
 *
 * Dieselbe Regel wie beim Gras: Nichts wird gewuerfelt. Wer wann vorbeigeht, ergibt sich aus
 * dem Szenentakt; wo ein Berg steht, aus der Bildbreite. Dadurch flimmert nichts, und dasselbe
 * Bild entsteht bei jedem Neuzeichnen und in jeder Aufnahme (siehe PlayClipRenderer) gleich.
 */
internal object PlayWorld {

    /** Der ferne Hintergrund: Bergketten, Blaetterdach, Skyline - klar hinter allem. */
    const val FAR = 300

    /** Etwas naeher: die vordere Bergkette, das Meer, Hausdaecher. */
    const val NEAR = 440

    /** Leute, Tiere und Fahrzeuge - sichtbar, aber unter den Moebeln ([PlayScene.FURNITURE]). */
    const val MID = 900

    // ---------------------------------------------------------------------------------------
    // Zeichenhilfen
    // ---------------------------------------------------------------------------------------

    /**
     * Pixel-Art als Textzeilen: `#` ist gesetzt, alles andere leer. Lesbarer als Koordinatenlisten
     * und damit leichter nachzuzeichnen - man sieht die Form, die man baut.
     */
    fun art(vararg rows: String): List<Pair<Int, Int>> = rows.flatMapIndexed { y, row ->
        row.mapIndexedNotNull { x, c -> if (c == '#') x to y else null }
    }

    /** Spiegelt eine Form waagerecht - fuer alles, was in beide Richtungen laufen kann. */
    private fun mirror(cells: List<Pair<Int, Int>>, width: Int): List<Pair<Int, Int>> =
        cells.map { (x, y) -> (width - 1 - x) to y }

    private fun place(
        cells: List<Pair<Int, Int>>,
        x: Int,
        y: Int,
        brightness: Int,
        light: Boolean = false
    ): List<SceneCell> = cells.map { (px, py) -> SceneCell(x + px, y + py, brightness, isLight = light) }

    /** Deterministisches Streuen - dieselbe Zahl fuer dieselbe Stelle, aber ohne sichtbares Muster. */
    private fun hash(a: Int, b: Int = 0): Int {
        var h = a * 374_761_393 + b * 668_265_263
        h = (h xor (h ushr 13)) * 1_274_126_177
        return (h xor (h ushr 16)) and 0x7fffffff
    }

    private fun isNight(dayPhase: PlayAmbientActivity.DayPhase) =
        dayPhase == PlayAmbientActivity.DayPhase.NIGHT

    private fun isDark(dayPhase: PlayAmbientActivity.DayPhase) =
        dayPhase == PlayAmbientActivity.DayPhase.NIGHT || dayPhase == PlayAmbientActivity.DayPhase.EVENING

    /** Wie hoch der ferne Hintergrund hoechstens reichen darf - auf flachen Bildern weniger. */
    private fun skyRoom(floorY: Int, wanted: Int): Int = wanted.coerceAtMost((floorY * 0.7f).toInt()).coerceAtLeast(0)

    // ---------------------------------------------------------------------------------------
    // Die neuen Orte
    // ---------------------------------------------------------------------------------------

    /**
     * Die Landschaften des Weltausbaus. Die Kristallgrotte gehoert dazu, obwohl sie ein Hoehlen-
     * raum ist: Man geht hinaus, um hinzukommen, sie schliesst nachts und bei Regen wie die uebrige
     * Wildnis. Himmel, Wetter und Sonne blendet [skyMask] bzw. PlayScene fuer sie aus.
     */
    val NATURE: Set<PlayScene.Place> = setOf(
        PlayScene.Place.JUNGLE,
        PlayScene.Place.MOUNTAINS,
        PlayScene.Place.SWAMP,
        PlayScene.Place.PLAINS,
        PlayScene.Place.BEACH,
        PlayScene.Place.GROTTO
    )

    /** Wo die Figur in Ruhe steht - immer vorn links, wo nichts ihren Platz belegt. */
    fun avatarAnchorX(place: PlayScene.Place): Float? = when (place) {
        PlayScene.Place.JUNGLE -> 0.06f
        PlayScene.Place.MOUNTAINS -> 0.08f
        PlayScene.Place.SWAMP -> 0.06f
        PlayScene.Place.PLAINS -> 0.10f
        PlayScene.Place.BEACH -> 0.14f
        PlayScene.Place.GROTTO -> 0.08f
        PlayScene.Place.CAMP -> 0.08f
        // Mitten im Raum, zwischen Tischchen und Theke.
        PlayScene.Place.CAFE -> 0.44f
        else -> null
    }

    /**
     * Die Einrichtung der neuen Landschaften. Jede hat genau EINEN Sitzplatz ([PlayScene.Station.BENCH]):
     * So laufen alle vorhandenen Ablaeufe ("hingehen, hinsetzen, verweilen") hier ohne jede
     * Aenderung - was man benutzt, ist eine Sitzgelegenheit, gleich wie sie aussieht. Dasselbe
     * Prinzip wie beim Baumstamm im Wald.
     */
    fun furnishing(place: PlayScene.Place): List<Placement>? = when (place) {
        // DSCHUNGEL: Palme und Bambus hinten, davor ein Wurzelbogen zum Sitzen und riesige
        // Blaetter. Das Blaetterdach selbst kommt aus [background].
        PlayScene.Place.JUNGLE -> listOf(
            Placement(PALM, anchorX = 0.30f, brightness = PlayScene.BACKDROP, behind = true),
            Placement(BAMBOO, anchorX = 0.97f, brightness = PlayScene.BACKDROP, behind = true),
            Placement(ROOT_ARCH, anchorX = 0.52f, station = PlayScene.Station.BENCH),
            Placement(MONSTERA, anchorX = 0.80f),
            Placement(JUNGLE_FLOWER, anchorX = 0.97f)
        )
        // GEBIRGE: Bergketten hinten (siehe [background]), vorn ein flacher Felsen zum Rasten,
        // ein Wegweiser, Latschen und Geroell.
        PlayScene.Place.MOUNTAINS -> listOf(
            Placement(ALPINE_PINE, anchorX = 0.70f, brightness = PlayScene.BACKDROP, behind = true),
            Placement(STONE_SEAT, anchorX = 0.44f, station = PlayScene.Station.BENCH),
            Placement(SIGNPOST, anchorX = 0.70f),
            Placement(SCREE, anchorX = 0.94f)
        )
        // SUMPF: ein knorriger Mangrovenbaum ueber dem Wasser, ein Holzsteg zum Sitzen,
        // Schilf und ein morscher Stumpf.
        PlayScene.Place.SWAMP -> listOf(
            Placement(MANGROVE, anchorX = 0.26f, brightness = PlayScene.BACKDROP, behind = true),
            Placement(BOARDWALK, anchorX = 0.52f, station = PlayScene.Station.BENCH),
            Placement(SWAMP_REEDS, anchorX = 0.82f),
            Placement(STUMP, anchorX = 0.97f)
        )
        // EBENE: weite Huegel hinten, eine Windmuehle, ein Heuballen zum Draufsetzen, ein
        // Weidezaun und ein paar Blumen.
        PlayScene.Place.PLAINS -> listOf(
            Placement(WINDMILL, anchorX = 0.82f, brightness = PlayScene.BACKDROP, behind = true),
            Placement(HAYBALE, anchorX = 0.44f, station = PlayScene.Station.BENCH),
            Placement(WILDFLOWERS, anchorX = 0.64f),
            Placement(PASTURE_FENCE, anchorX = 0.98f)
        )
        // CAFE: links der Sessel am Tischchen (dort sitzt man), rechts die Theke mit der
        // Espressomaschine, an der Wand die Tafel, von der Decke zwei Haengelampen. Die Tuer
        // setzt PlayScene dazu (besideDoor).
        PlayScene.Place.CAFE -> listOf(
            Placement(MENU_BOARD, anchorX = 0.34f, liftCells = 9, brightness = PlayScene.BACKDROP, behind = true),
            Placement(PENDANT, anchorX = 0.16f, liftCells = 14, brightness = PlayScene.BACKDROP, behind = true),
            Placement(PENDANT, anchorX = 0.78f, liftCells = 14, brightness = PlayScene.BACKDROP, behind = true),
            Placement(ARMCHAIR, anchorX = 0.02f, station = PlayScene.Station.SEAT),
            Placement(COUNTER_BAR, anchorX = 1f),
            Placement(CAFE_TABLE, anchorX = 0.27f)
        )
        // STRAND: Palme und Sonnenschirm hinten, davor der Liegestuhl, eine Sandburg.
        PlayScene.Place.BEACH -> listOf(
            Placement(PALM, anchorX = 0f, brightness = PlayScene.BACKDROP, behind = true),
            Placement(PARASOL, anchorX = 0.56f, brightness = PlayScene.BACKDROP, behind = true),
            Placement(DECKCHAIR, anchorX = 0.54f, station = PlayScene.Station.BENCH),
            Placement(SANDCASTLE, anchorX = 0.90f)
        )
        // KRISTALLGROTTE (Expeditionsquest, siehe PlayQuests): leuchtende Kristalle, ein
        // Felsblock zum Sitzen, Tropfsteine. Die Hoehlendecke kommt aus [background].
        PlayScene.Place.GROTTO -> listOf(
            Placement(CRYSTAL_CLUSTER, anchorX = 0.34f, brightness = PlayScene.BACKDROP, behind = true),
            Placement(STONE_SEAT, anchorX = 0.52f, station = PlayScene.Station.BENCH),
            Placement(CRYSTAL_CLUSTER, anchorX = 0.80f),
            Placement(STALAGMITE, anchorX = 0.97f)
        )
        // LAGER (mehrtaegige Reisen): in der Mitte das Feuer, rechts davon der Stamm zum Sitzen -
        // man sitzt mit Blick in die Flammen -, dahinter das Zelt, am Rand der Rucksack.
        PlayScene.Place.CAMP -> listOf(
            Placement(TENT, anchorX = 0.82f, brightness = PlayScene.BACKDROP + 200, behind = true),
            Placement(CAMPFIRE, anchorX = 0.36f),
            Placement(CAMP_LOG, anchorX = 0.56f, station = PlayScene.Station.BENCH),
            Placement(PACK, anchorX = 0.97f)
        )
        else -> null
    }

    // ---- Kristallgrotte und Quest-Belohnungen ----

    /** Ein Buendel Kristalle, die von innen leuchten. */
    private val CRYSTAL_CLUSTER = Prop(
        width = 7, height = 7,
        art = art(
            "..#....",
            "..##.#.",
            ".###.#.",
            ".#.#.##",
            "##.#.#.",
            "#..###.",
            "#######"
        ),
        lightAt = 2 to 2
    )

    /** Ein Tropfstein vom Boden. */
    private val STALAGMITE = Prop(
        width = 3, height = 6,
        art = art(".#.", ".#.", ".#.", "###", "###", "###")
    )

    /** Die Schatztruhe - heimgebracht aus der Schatzsuche, sie steht im Wohnzimmer. */
    internal val QUEST_CHEST = Prop(
        width = 7, height = 5,
        art = art(
            ".#####.",
            "#######",
            "#.###.#",
            "#######",
            "#######"
        ),
        lightAt = 3 to 2
    )

    /** Das Vogelhaus auf seiner Stange (siehe PlayGoals). */
    internal val GOAL_BIRDHOUSE = Prop(
        width = 3, height = 8,
        art = art(
            ".#.",
            "###",
            "#.#",
            "###",
            ".#.",
            ".#.",
            ".#.",
            ".#."
        )
    )

    /** Der Kraeutertopf mit Keimling. */
    internal val GOAL_HERB_SPROUT = Prop(
        width = 4, height = 6,
        art = art(
            "..#.",
            ".##.",
            "..#.",
            "####",
            "####",
            ".##."
        )
    )

    /** Der Kraeutertopf, voll ausgewachsen. */
    internal val GOAL_HERB_BUSH = Prop(
        width = 5, height = 6,
        art = art(
            "#.#.#",
            "#####",
            ".###.",
            "..#..",
            "#####",
            ".###."
        )
    )

    /** Der selbstgebaute Drachen an der Wand, mit Schwanz. */
    internal val GOAL_KITE = Prop(
        width = 5, height = 7,
        art = art(
            "..#..",
            ".###.",
            "#####",
            ".###.",
            "..#..",
            "...#.",
            "..#.."
        )
    )

    /** Der Zauberstab an der Wand - seit die Zauberlehre gelungen ist. */
    internal val QUEST_WAND = Prop(
        width = 5, height = 5,
        art = art(
            "....#",
            "...#.",
            "..#..",
            ".#...",
            "#...."
        ),
        lightAt = 4 to 0
    )

    /** Das Drachenei im Nest. */
    internal val QUEST_EGG = Prop(
        width = 7, height = 6,
        art = art(
            "..##...",
            ".####..",
            ".####..",
            "..##...",
            "#.....#",
            "#######"
        ),
        lightAt = 2 to 1
    )

    /** Dasselbe Ei mit Rissen - am zweiten Tag des Waermens. */
    internal val QUEST_EGG_CRACKED = Prop(
        width = 7, height = 6,
        art = art(
            "..#....",
            ".##.#..",
            ".#.##..",
            "..##...",
            "#.....#",
            "#######"
        ),
        lightAt = 3 to 1
    )

    /** Das Drachenjunge, geschluepft - es wohnt jetzt mit. */
    internal val QUEST_DRAGONLING = Prop(
        width = 7, height = 6,
        art = art(
            ".##....",
            "###.#.#",
            "..####.",
            "..###..",
            "..#.#.#",
            ".##.##."
        ),
        lightAt = 1 to 0
    )

    // ---- Das Lager (mehrtaegige Reisen, siehe PlayQuests) ----

    /** Das Zelt: ein Dreieck mit dunklem Eingang - daran erkennt man es auch klein. */
    private val TENT = Prop(
        width = 15, height = 8,
        art = art(
            ".......#.......",
            "......###......",
            ".....#####.....",
            "....###.###....",
            "...###...###...",
            "..###.....###..",
            ".####.....####.",
            "#####.....#####"
        )
    )

    /** Die Feuerstelle: gekreuzte Scheite im Steinkreis. Die Flammen zeichnet [ambient]. */
    private val CAMPFIRE = Prop(
        width = 7, height = 2,
        art = art(
            "..#.#..",
            "#######"
        )
    )

    /** Ein Stamm am Feuer zum Sitzen. */
    private val CAMP_LOG = Prop(
        width = 7, height = 2,
        art = art(
            ".#####.",
            "#######"
        ),
        useSpot = 3 to -1
    )

    /** Der abgestellte Rucksack. */
    private val PACK = Prop(
        width = 4, height = 4,
        art = art(
            ".##.",
            "####",
            "#..#",
            "####"
        )
    )

    // ---- Requisiten der neuen Landschaften ----

    /** Palme - gebogener Stamm, Wedel, die nach beiden Seiten herabhaengen, zwei Kokosnuesse. */
    private val PALM = Prop(
        width = 11, height = 16,
        art = art(
            "...##..##..",
            ".##..##..#.",
            "#...####..#",
            "...##.###..",
            "..#..##..#.",
            ".#...#.#..#",
            ".....#.....",
            ".....#.....",
            "....#......",
            "....#......",
            "....#......",
            "....#......",
            "...#.......",
            "...#.......",
            "...#.......",
            "..###......"
        )
    )

    /** Bambus - drei Rohre mit Knoten und ein paar Blaettern. */
    private val BAMBOO = Prop(
        width = 5, height = 15,
        art = art(
            "#...#",
            "##.##",
            "#.#.#",
            "#.#.#",
            "#####",
            "#.#.#",
            "#.#.#",
            "#.#.#",
            "#####",
            "#.#.#",
            "#.#.#",
            "#.#.#",
            "#####",
            "#.#.#",
            "#.#.#"
        )
    )

    /** Wurzelbogen - ein umgestuerzter, bemooster Stamm auf Luftwurzeln. Sitzplatz wie der Baumstamm. */
    private val ROOT_ARCH = Prop(
        width = 13, height = 5,
        art = art(
            "#.#.#...#..#.",
            "#############",
            "#############",
            "#.#.#....#.#.",
            "#..#.....#..#"
        ),
        frontArt = PlayScene.hLine(3, 12, 2),
        useSpot = 6 to 2
    )

    /** Monstera - riesige, geschlitzte Blaetter auf kurzen Stielen. */
    private val MONSTERA = Prop(
        width = 8, height = 6,
        art = art(
            ".##..##.",
            "#.##.#.#",
            "##.###.#",
            ".#..#.#.",
            "..#.##..",
            "...##..."
        )
    )

    /** Tropische Bluete - ein Kelch mit Staubgefaess. */
    private val JUNGLE_FLOWER = Prop(
        width = 5, height = 4,
        art = art(
            "#.#.#",
            ".###.",
            "..#..",
            ".###."
        )
    )

    /** Latsche - niedriger, windschiefer Nadelbaum der Hoehe. */
    private val ALPINE_PINE = Prop(
        width = 7, height = 11,
        art = art(
            "...#...",
            "..###..",
            "...#...",
            ".#####.",
            "..###..",
            ".#####.",
            "#######",
            "..###..",
            "#######",
            "...#...",
            "...#..."
        )
    )

    /** Flacher Felsen zum Rasten - man setzt sich OBEN drauf. */
    private val STONE_SEAT = Prop(
        width = 9, height = 4,
        art = art(
            "..#####..",
            ".#######.",
            "##.######",
            "#########"
        ),
        useSpot = 4 to -1
    )

    /** Wegweiser mit zwei Pfeilen - man ist unterwegs, nicht irgendwo. */
    private val SIGNPOST = Prop(
        width = 7, height = 10,
        art = art(
            "######.",
            "#######",
            "######.",
            "...#...",
            ".######",
            "#######",
            ".######",
            "...#...",
            "...#...",
            "..###.."
        )
    )

    /** Geroell - lose Steine am Hang. */
    private val SCREE = Prop(
        width = 6, height = 3,
        art = art(
            "..#...",
            ".###.#",
            "######"
        )
    )

    /** Mangrove - knorriger Baum auf Stelzwurzeln, die sich ins Wasser boegen. */
    private val MANGROVE = Prop(
        width = 13, height = 15,
        art = art(
            "..##...##....",
            ".#..#.#..##..",
            "#....###...#.",
            "..#..###..#..",
            ".....###.....",
            "....#.#.#....",
            "...#..#..#...",
            ".....###.....",
            "......#......",
            "......#......",
            "....#####....",
            "...#..#..#...",
            "..#...#...#..",
            ".#....#....#.",
            "#.....#.....#"
        )
    )

    /** Holzsteg auf Pfaehlen - man sitzt auf den Planken. */
    private val BOARDWALK = Prop(
        width = 13, height = 4,
        art = art(
            "#############",
            "#.....#.....#",
            "#.....#.....#",
            "#.....#.....#"
        ),
        useSpot = 6 to -1
    )

    /** Schilf am Sumpfrand - Rohrkolben ueber schmalen Halmen. */
    private val SWAMP_REEDS = Prop(
        width = 6, height = 7,
        art = art(
            ".#..#.",
            ".#..##",
            "##.#.#",
            "#..#.#",
            "#.#..#",
            "#.#.#.",
            "######"
        )
    )

    /** Morscher Baumstumpf. */
    private val STUMP = Prop(
        width = 4, height = 3,
        art = art(
            "#.##",
            "####",
            "####"
        )
    )

    /**
     * Windmuehle - Turm mit Tuer und Dach; die FLUEGEL dreht [ambient], sie gehoeren nicht zur
     * starren Form. Die Nabe sitzt bei [WINDMILL_HUB].
     */
    private val WINDMILL = Prop(
        width = 7, height = 14,
        art = art(
            "...#...",
            "..###..",
            ".#####.",
            ".#...#.",
            ".#...#.",
            ".#...#.",
            "#.....#",
            "#..#..#",
            "#.....#",
            "#.....#",
            "#.###.#",
            "#.#.#.#",
            "#.#.#.#",
            "#######"
        )
    )
    private val WINDMILL_HUB = 3 to 4

    /** Runder Heuballen - man setzt sich oben drauf. */
    private val HAYBALE = Prop(
        width = 8, height = 5,
        art = art(
            ".######.",
            "##.##.##",
            "#.####.#",
            "##.##.##",
            ".######."
        ),
        useSpot = 4 to -1
    )

    /** Wildblumen - drei Blueten unterschiedlicher Hoehe. */
    private val WILDFLOWERS = Prop(
        width = 6, height = 4,
        art = art(
            "#...#.",
            "#.#.#.",
            "#.#.##",
            "######"
        )
    )

    /** Weidezaun - zwei Latten auf drei Pfosten. */
    private val PASTURE_FENCE = Prop(
        width = 9, height = 4,
        art = art(
            "#...#...#",
            "#########",
            "#...#...#",
            "#########"
        )
    )

    /** Sonnenschirm - gewoelbtes Dach mit Streifen auf langem Stock. */
    private val PARASOL = Prop(
        width = 11, height = 13,
        art = art(
            ".....#.....",
            "...#####...",
            ".##.###.##.",
            "#.#.#.#.#.#",
            ".....#.....",
            ".....#.....",
            ".....#.....",
            ".....#.....",
            ".....#.....",
            ".....#.....",
            ".....#.....",
            ".....#.....",
            ".....#....."
        )
    )

    /** Liegestuhl - schraege Lehne, Sitzflaeche, Beine. */
    private val DECKCHAIR = Prop(
        width = 10, height = 6,
        art = art(
            "#.........",
            ".#........",
            "..#.......",
            "...#######",
            "...#.....#",
            "..#.....#."
        ),
        frontArt = PlayScene.hLine(4, 9, 3),
        useSpot = 6 to 3
    )

    /** Sandburg mit zwei Tuermen und Fahne. */
    private val SANDCASTLE = Prop(
        width = 7, height = 6,
        art = art(
            ".#.....",
            ".##....",
            ".#...#.",
            "###.###",
            "#######",
            "#######"
        )
    )

    // ---- Das Cafe ----

    /** Sessel - hohe Lehne links, Polster, man sitzt darin wie auf dem Sofa. */
    internal val ARMCHAIR = Prop(
        width = 8, height = 7,
        art = art(
            "##......",
            "##......",
            "##......",
            "##.....#",
            "########",
            "########",
            "#......#"
        ),
        frontArt = PlayScene.rect(3, 4, 7, 5) + (7 to 3),
        useSpot = 4 to 4
    )

    /** Rundes Tischchen mit zwei Tassen darauf. */
    private val CAFE_TABLE = Prop(
        width = 6, height = 6,
        art = art(
            ".#..#.",
            "######",
            "..##..",
            "..##..",
            "..##..",
            ".####."
        )
    )

    /**
     * Die Theke: oben die Espressomaschine mit Siebtraeger, daneben Tassen, vorn eine Vitrine mit
     * Gebaeck. Der Dampf steigt ueber [CAFE_STEAM_AT] auf (siehe [ambient]).
     */
    private val COUNTER_BAR = Prop(
        width = 14, height = 10,
        art = art(
            "..####........",
            "..#..#........",
            "..####..#.#.#.",
            "..#.#...#.#.#.",
            "##############",
            "#............#",
            "#.##.##.##.#.#",
            "#............#",
            "##############",
            "#............#"
        )
    )
    private val CAFE_STEAM_AT = 3 to 0

    /** Kreidetafel mit Karte - drei Zeilen und ein Tassen-Symbol. */
    private val MENU_BOARD = Prop(
        width = 9, height = 6,
        art = art(
            "#########",
            "#.##.#..#",
            "#.###...#",
            "#..##.#.#",
            "#.#..##.#",
            "#########"
        )
    )

    /** Haengelampe - Kabel von der Decke, Schirm, darunter das Licht. */
    private val PENDANT = Prop(
        width = 5, height = 4,
        art = art(
            "..#..",
            "..#..",
            ".###.",
            "#####"
        ),
        lightAt = 2 to 4
    )

    // ---------------------------------------------------------------------------------------
    // Boden
    // ---------------------------------------------------------------------------------------

    /** Der Untergrund der neuen Landschaften - eine bis zwei Zeilen, aus der Position gerechnet. */
    fun groundDetail(place: PlayScene.Place, widthCells: Int, floorY: Int): List<SceneCell>? = when (place) {
        // Dschungel: dichtes, hohes Bodenkraut - zwei Zeilen, noch dichter als im Wald.
        PlayScene.Place.JUNGLE -> (0 until widthCells).flatMap { x ->
            buildList {
                if (hash(x, 1) % 3 != 0) add(SceneCell(x, floorY - 1, PlayScene.STRUCTURE))
                if (hash(x, 2) % 5 == 0) add(SceneCell(x, floorY - 2, PlayScene.STRUCTURE))
            }
        }
        // Gebirge: Fels und Geroell - einzelne helle Steine, dazwischen nackter Boden.
        PlayScene.Place.MOUNTAINS -> (0 until widthCells).filter { hash(it, 3) % 4 == 0 }
            .map { SceneCell(it, floorY - 1, (PlayScene.STRUCTURE * 1.4f).roundToInt()) }
        // Sumpf: stehendes Wasser - die Bodenlinie glaenzt stellenweise, dazwischen Schlamm.
        PlayScene.Place.SWAMP -> (0 until widthCells).flatMap { x ->
            buildList {
                if (hash(x, 4) % 3 == 0) add(SceneCell(x, floorY, (PlayScene.STRUCTURE * 1.6f).roundToInt()))
                if (hash(x, 5) % 7 == 0) add(SceneCell(x, floorY - 1, PlayScene.STRUCTURE))
            }
        }
        // Ebene: hohes Gras in lockeren Buescheln.
        PlayScene.Place.PLAINS -> (0 until widthCells).flatMap { x ->
            buildList {
                if (x % 4 == 1 || x % 9 == 5) add(SceneCell(x, floorY - 1, PlayScene.STRUCTURE))
                if (x % 9 == 5) add(SceneCell(x, floorY - 2, PlayScene.STRUCTURE))
            }
        }
        // Cafe: Fliesen im Schachbrett - jede zweite Fuge hell.
        PlayScene.Place.CAFE -> (0 until widthCells).filter { it % 2 == 0 }
            .map { SceneCell(it, floorY, (PlayScene.STRUCTURE * 1.5f).roundToInt()) }
        // Strand: feine Sandrippel und ab und zu eine Muschel.
        PlayScene.Place.BEACH -> (0 until widthCells).flatMap { x ->
            buildList {
                if ((x / 2) % 3 == 0) add(SceneCell(x, floorY, (PlayScene.STRUCTURE * 1.3f).roundToInt()))
                if (hash(x, 6) % 13 == 0) add(SceneCell(x, floorY - 1, PlayScene.FURNITURE))
            }
        }
        else -> null
    }

    // ---------------------------------------------------------------------------------------
    // Der ferne Hintergrund
    // ---------------------------------------------------------------------------------------

    /**
     * Was weit hinten liegt - VOR den Requisiten gezeichnet, damit alles davor steht.
     *
     * Hier wohnt der groesste Teil des Gewinns: Ueber dem Boden lag bisher ein halber Bildschirm
     * leerer Himmel. Eine Bergkette, ein Blaetterdach oder das Meer fuellt genau diese Flaeche, und
     * erst dadurch wird aus "eine Figur neben zwei Requisiten" eine Landschaft.
     *
     * [foreground] sind die Vordergrund-Requisiten als (Spalten, oberste Zeile) - der Laden braucht
     * sie, damit seine Regalwand nicht durch Kasse, Warenregal und Tuer hindurchscheint.
     */
    fun background(
        place: PlayScene.Place,
        phase: Int,
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase,
        foreground: List<Pair<IntRange, Int>> = emptyList(),
        /** Wessen Park es ist - jedes Wesen hat dort seine eigene Landschaft (PlayScene.habitatPlacements). */
        species: AvatarSpecies = AvatarSpecies.PUFFLING
    ): List<SceneCell> = when (place) {
        PlayScene.Place.MOUNTAINS -> mountainRange(widthCells, floorY, dayPhase)
        PlayScene.Place.JUNGLE -> jungleCanopy(phase, widthCells, floorY)
        PlayScene.Place.SWAMP -> swampBackdrop(phase, widthCells, floorY)
        PlayScene.Place.PLAINS -> farmland(widthCells, floorY, dayPhase)
        PlayScene.Place.BEACH -> sea(phase, widthCells, floorY, dayPhase)
        PlayScene.Place.GROTTO -> caveVault(widthCells, floorY)
        PlayScene.Place.CITY -> skyline(phase, widthCells, floorY, dayPhase)
        PlayScene.Place.SHOP -> shopBackWall(phase, widthCells, floorY, foreground)
        PlayScene.Place.POND -> lakeShore(phase, widthCells, floorY, dayPhase)
        PlayScene.Place.FOREST -> forestLayers(phase, widthCells, floorY, dayPhase, dense = false)
        PlayScene.Place.MEADOW -> meadowHill(widthCells, floorY, flowers = false)
        PlayScene.Place.CAMP -> campBackdrop(widthCells, floorY)
        PlayScene.Place.PARK -> habitatBackdrop(species, phase, widthCells, floorY, dayPhase)
        else -> emptyList()
    }

    /**
     * **Wo das Wasser beginnt** - die Zeile der Wasserlinie, wenn der Ort eine offene
     * Wasserflaeche hat (Teich, Meer), sonst `null`. Dieselbe Rechnung wie beim Zeichnen
     * ([lakeShore], [sea]), damit Spiegelungen genau auf dem Wasser liegen (siehe
     * PlayScene.reflections).
     */
    fun waterSurface(place: PlayScene.Place, floorY: Int): Int? = when (place) {
        PlayScene.Place.POND -> {
            val maxH = skyRoom(floorY, 30)
            if (maxH < 10) null else (floorY - 1) - (maxH * 0.42f).roundToInt().coerceAtLeast(4)
        }
        PlayScene.Place.BEACH -> if (floorY < 12) null else floorY - 16
        else -> null
    }

    /** Bis zu welcher Zeile das Wasser reicht (darunter Ufer, Gischt, Sand). */
    fun waterBottom(place: PlayScene.Place, floorY: Int): Int = when (place) {
        PlayScene.Place.BEACH -> floorY - 4
        else -> floorY - 3
    }

    /** Der ferne Hintergrund des Parks - je nach Wesen eine andere Landschaft. */
    private fun habitatBackdrop(
        species: AvatarSpecies,
        phase: Int,
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase
    ): List<SceneCell> = when (species) {
        AvatarSpecies.PUFFLING -> cityPark(widthCells, floorY, dayPhase)
        AvatarSpecies.STARLET -> meadowHill(widthCells, floorY, flowers = true)
        AvatarSpecies.WYRMLING -> crags(widthCells, floorY)
        AvatarSpecies.FENNEC -> desert(phase, widthCells, floorY, dayPhase)
        AvatarSpecies.GLOOP -> swampBackdrop(phase, widthCells, floorY)
        AvatarSpecies.HOOTLET -> forestLayers(phase, widthCells, floorY, dayPhase, dense = true)
    }

    /**
     * Welche Himmelszellen der Hintergrund verdeckt - Sterne und Wolken ziehen HINTER Bergen,
     * Huegeln, Baeumen und Hochhaeusern vorbei, nicht davor (dieselbe Regel wie bei den Fassaden,
     * siehe PlayScene.facadeMask). Das Meer bleibt offen: Ueber dem Horizont ist Himmel.
     */
    fun skyMask(
        place: PlayScene.Place,
        widthCells: Int,
        floorY: Int,
        species: AvatarSpecies = AvatarSpecies.PUFFLING
    ): Set<Pair<Int, Int>> = when (place) {
        PlayScene.Place.MOUNTAINS, PlayScene.Place.JUNGLE, PlayScene.Place.CITY, PlayScene.Place.PLAINS,
        PlayScene.Place.POND, PlayScene.Place.FOREST, PlayScene.Place.MEADOW, PlayScene.Place.PARK,
        PlayScene.Place.SWAMP, PlayScene.Place.CAMP ->
            background(place, 0, widthCells, floorY, PlayAmbientActivity.DayPhase.MIDDAY, species = species)
                .mapTo(HashSet()) { it.x to it.y }
        // In der Grotte gibt es keinen Himmel: alles ueber dem Boden ist Fels.
        PlayScene.Place.GROTTO -> (0 until widthCells).flatMapTo(HashSet()) { x ->
            (0 until floorY).map { y -> x to y }
        }
        else -> emptySet()
    }

    /**
     * **Das Gebirge: ein Horn, das man wiedererkennt.**
     *
     * Vorher zwei gleichfoermige Zacken-Ketten, schachbrettartig gerastert - auf der Punktmatrix
     * las sich das als Rauschen, und kein Berg war "der Berg". Jetzt ist es ein Bild in vier
     * Ebenen, jede mit einer Aufgabe:
     *
     * 1. **Die ferne Kette** - flach, fast nur Umriss: Tiefe, sonst nichts.
     * 2. **Das Horn** - EIN hoher, schiefer Gipfel mit Schneekappe, links im Licht, rechts im
     *    Schatten. Das ist das Wahrzeichen: Wer es einmal gesehen hat, weiss, wo das Wesen ist.
     * 3. **Die Waldhuegel davor** - dunkel, mit einer Zackenkante aus Tannenspitzen. Dunkel vor
     *    hell stellt das Horn frei, statt es zu verdecken.
     * 4. **Die Almhuette** am Hang - der Ort, an dem man rastet; abends brennt Licht im Fenster.
     *
     * Flaechen sind durchgehend gefuellt statt gerastert: Auf einem Punkteraster ist eine ruhige,
     * gleichmaessig schwache Flaeche lesbarer als ein Muster.
     */
    private fun mountainRange(
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase = PlayAmbientActivity.DayPhase.MIDDAY
    ): List<SceneCell> {
        val maxH = skyRoom(floorY, 40)
        if (maxH < 6 || widthCells <= 0) return emptyList()
        val ground = floorY - 1
        val grid = HashMap<Pair<Int, Int>, SceneCell>()
        fun put(x: Int, y: Int, b: Int, light: Boolean = false) {
            if (x in 0 until widthCells && y in 0 until floorY) grid[x to y] = SceneCell(x, y, b, isLight = light)
        }

        // 1. Die ferne Kette: sanfte Wellen, nur Kammlinie und eine ganz schwache Flaeche.
        val far = IntArray(widthCells) { x ->
            val u = x / widthCells.toFloat()
            (maxH * (0.40f + 0.07f * sin(u * 9.0 + 0.6).toFloat() + 0.05f * sin(u * 23.0 + 2.0).toFloat()))
                .roundToInt()
        }
        for (x in 0 until widthCells) {
            val top = ground - far[x]
            put(x, top, Tone.EDGE - 120)
            for (y in top + 1 until ground) put(x, y, Tone.DEEP)
        }

        // 2. Das Horn: Spitze bei 62 %, links die lange Flanke im Licht, rechts steil im Schatten.
        val apexX = (widthCells * 0.62f).roundToInt()
        val leftSpan = widthCells * 0.36f
        val rightSpan = widthCells * 0.24f
        val horn = IntArray(widthCells) { x ->
            val dx = x - apexX
            val t = if (dx <= 0) -dx / leftSpan else dx / rightSpan
            // Nach innen gewoelbt (die Spitze sticht), mit kleinen Absaetzen im linken Grat.
            val shoulder = if (dx < -2 && hash(x, 41) % 4 == 0) 1 else 0
            val v = (1f - t).coerceAtLeast(0f)
            ((maxH * (0.5f * v + 0.5f * (1f - (1f - v) * (1f - v)))).roundToInt() - shoulder)
                .coerceAtLeast(0)
        }
        val snowLine = maxH * 0.58f
        for (x in 0 until widthCells) {
            val h = horn[x]
            if (h <= far[x]) continue
            val top = ground - h
            val lit = x < apexX
            // Die Schneegrenze franst aus, und in den Rinnen zieht der Schnee in Zungen hinab.
            val tongue = if (hash(x / 3, 44) % 3 == 0) 2 + hash(x, 45) % 3 else 0
            val snowDepth = (h - snowLine + 1.8f * sin(x * 0.9).toFloat()).roundToInt() + tongue
            for (y in top until ground) {
                val depth = y - top
                val b = when {
                    depth < snowDepth && lit -> if (depth == 0) Tone.GLINT else Tone.SNOW
                    depth < snowDepth -> if (depth == 0) Tone.SNOW - 200 else Tone.LIT - 80
                    // Felsrippen auf der Lichtseite: wenige lange, schraege Grate.
                    lit && Math.floorMod(x + depth, 9) == 0 -> Tone.EDGE
                    lit -> Tone.HAZE
                    else -> Tone.DEEP - 40
                }
                put(x, y, b)
            }
        }
        // Der Grat von der Spitze nach links: die beleuchtete Kante.
        for (x in 0..apexX.coerceAtMost(widthCells - 1)) {
            val h = horn[x]
            if (h > far[x] + 1) put(x, ground - h, if (h > snowLine) Tone.GLINT else Tone.LIT)
        }

        // 3. Die Waldhuegel davor: fast schwarz, die Oberkante aus Tannenspitzen.
        val hill = IntArray(widthCells) { x ->
            val u = x / widthCells.toFloat()
            (maxH * (0.22f + 0.07f * sin(u * 7.5 + 4.0).toFloat())).roundToInt()
        }
        for (x in 0 until widthCells) {
            val tip = when (Math.floorMod(x + hash(x / 3, 43) % 2, 3)) {
                1 -> 2
                else -> 1
            }
            val top = ground - hill[x] - tip
            put(x, top, Tone.EDGE)
            for (y in top + 1 until ground) put(x, y, Tone.VOID)
        }

        // 4. Die Almhuette auf dem Waldhuegel, links vom Horn.
        val hutX = (widthCells * 0.28f).roundToInt()
        if (hutX + 9 < widthCells && maxH >= 14) {
            val base = ground - hill[hutX + 4]
            val roof = art(
                "....#....",
                "...###...",
                "..#####..",
                ".#######.",
                "#########"
            )
            val wall = art(
                ".#######.",
                ".#######.",
                ".#######."
            )
            for ((px, py) in wall) put(hutX + px, base - 3 + py, Tone.HAZE)
            // Das Dach: die Traufe und die linke Schraege im Licht.
            for ((px, py) in roof) {
                put(hutX + px, base - 8 + py, if (py == 4 || px == 4 - py) Tone.LIT else Tone.EDGE)
            }
            // Zwei Fenster und die Tuer: tagsueber dunkel, abends ein warmes Licht.
            val lit = isDark(dayPhase)
            for (wx in listOf(2, 6)) put(hutX + wx, base - 2, if (lit) Tone.WINDOW else Tone.VOID, light = lit)
            put(hutX + 4, base - 2, Tone.VOID)
            put(hutX + 4, base - 1, Tone.VOID)
            // Rauch aus dem Kamin, schraeg vom Wind.
            put(hutX + 6, base - 9, Tone.HAZE)
            put(hutX + 7, base - 10, Tone.DEEP + 60)
            put(hutX + 7, base - 11, Tone.DEEP)
        }
        return grid.values.filter { it.brightness > 0 }
    }

    /**
     * **Die Toene der Landschaften**, am Geraet abgelesen: Eine Zelle leuchtet dort mit
     * Helligkeit/4095 der LED-Farbe (siehe PlaySceneView). Unter rund 200 verschwindet sie auf dem
     * schwarzen Grund; nach oben begrenzt die Moebelhelligkeit (PlayScene.FURNITURE): Der
     * Hintergrund darf nie heller sein als das, was vor ihm steht. Dazwischen liegen die
     * Stufen, mit denen Landschaft Tiefe bekommt: fern und flaechig dunkel, nah an den Kanten hell.
     */
    private object Tone {
        /** Silhouetten ganz vorn - fast schwarz, sie stellen das Helle dahinter frei. */
        const val VOID = 180

        /** Ferne Flaechen. */
        const val DEEP = 330

        /** Flaechen im Mittelgrund. */
        const val HAZE = 520

        /** Kammlinien und Umrisse. */
        const val EDGE = 740

        /** Beleuchtete Kanten. */
        const val LIT = 1000

        /** Schnee, Schaumkronen, helles Holz. */
        const val SNOW = 1150

        /** Die hellste Stelle eines Bildes - hoechstens ein paar Zellen, klar unter der Figur. */
        const val GLINT = 1260

        /** Ein warm erleuchtetes Fenster (Licht, dimmt nachts nicht). */
        const val WINDOW = 2300
    }

    /**
     * Das Blaetterdach des Dschungels: ein dichtes, gezacktes Band oben, Lianen, die daraus
     * herabhaengen und sich im Takt wiegen, entfernte Staemme und hinten ein Wasserfall.
     */
    private fun jungleCanopy(phase: Int, widthCells: Int, floorY: Int): List<SceneCell> {
        val depth = skyRoom(floorY, 30)
        if (depth < 8 || widthCells <= 0) return emptyList()
        val top = floorY - depth
        val cells = mutableListOf<SceneCell>()
        val sway = if (PlayScene.beat(phase, 7) % 4 < 2) 0 else 1
        val fall = (widthCells * 0.66f).toInt()
        for (x in 0 until widthCells) {
            // Ueber dem dichten Band loest sich das Laub nach oben hin auf - so reicht das Dach
            // bis an den Bildrand, statt als Streifen im schwarzen Himmel zu haengen.
            for (y in 0 until top) {
                val distance = top - y
                if (distance <= 28 && hash(x, y + 51) % (2 + distance / 4) == 0) {
                    cells += SceneCell(x, y, if (distance < 8) FAR else FAR - 100)
                }
            }
            val bottom = top + 6 + hash(x, 7) % 4
            for (y in top..bottom) {
                val h = hash(x, y + 11)
                val b = when {
                    y == bottom -> NEAR
                    h % 3 == 0 -> NEAR - 60
                    h % 3 == 1 -> FAR - 60
                    else -> 0
                }
                if (b > 0) cells += SceneCell(x, y, b)
            }
            // Ferne Staemme - duenne, blasse Senkrechte hinter allem.
            if (x % 11 == 5 && abs(x - fall) > 3) {
                for (y in bottom + 1 until floorY - 1) cells += SceneCell(x, y, FAR - 120)
            }
            // Lianen: ab der Mitte leicht im Wind versetzt.
            if (x % 7 == 3 && abs(x - fall) > 3) {
                val length = 8 + hash(x, 9) % 7
                for (d in 1..length) {
                    val lx = if (d > length / 2) x + sway else x
                    cells += SceneCell(lx, bottom + d, if (d % 3 == 0) NEAR else FAR)
                }
            }
        }
        // Wasserfall: zwei Spalten fliessendes Wasser, das Muster laeuft nach unten.
        val flow = PlayScene.beat(phase, 1)
        val fallTop = top + 8
        for (y in fallTop until floorY - 1) {
            for (dx in 0..2) {
                val lit = (y - flow + dx * 2) % 3 == 0
                cells += SceneCell(fall + dx, y, if (lit) PlayScene.FURNITURE - 300 else FAR)
            }
        }
        // Gischt unten.
        for (dx in -2..4) {
            if ((dx + flow) % 2 == 0) cells += SceneCell(fall + dx, floorY - 2, PlayScene.FURNITURE - 400)
        }
        return cells.filter { it.x in 0 until widthCells && it.y >= 0 }
    }

    /**
     * **Der Sumpf: kahle Baeume, Moos und Nebel.**
     *
     * Hinten eine niedrige, dunkle Baumlinie; davor drei knorrige, kahle Baeume, von deren Aesten
     * Moos in langen Faeden haengt - das ist die Silhouette, an der man einen Sumpf erkennt. Ueber
     * dem Wasser ziehen zwei Nebelbaender langsam gegeneinander, und das Wasser spiegelt die
     * Staemme gebrochen.
     */
    private fun swampBackdrop(phase: Int, widthCells: Int, floorY: Int): List<SceneCell> {
        val maxH = skyRoom(floorY, 30)
        if (widthCells <= 0 || maxH < 8) return emptyList()
        val sheet = Sheet(widthCells, floorY)
        val ground = floorY - 1
        // Die ferne Baumlinie: runde, dichte Kronen, kaum mehr als ein Saum.
        sheet.ridge(
            IntArray(widthCells) { x ->
                (maxH * 0.20f + 1.5f * sin(x * 0.7).toFloat() + (hash(x / 2, 21) % 2)).roundToInt()
            },
            edge = Tone.DEEP + 60, fill = Tone.VOID
        )
        // Die kahlen Baeume: ein krummer Stamm, zwei, drei Aeste, Moosfaeden daran.
        val trees = listOf(0.18f to 0.78f, 0.50f to 0.62f, 0.84f to 0.92f)
        for ((i, pair) in trees.withIndex()) {
            val (at, tall) = pair
            val x0 = (widthCells * at).roundToInt()
            val h = (maxH * tall).roundToInt()
            var x = x0
            for (d in 0 until h) {
                // Der Stamm neigt sich im oberen Drittel.
                if (d == h * 2 / 3) x += if (i % 2 == 0) 1 else -1
                sheet.put(x, ground - d, Tone.EDGE - 80)
                if (d < 3) sheet.put(x + 1, ground - d, Tone.DEEP + 40)
            }
            val top = ground - h
            for ((side, len, at2) in listOf(Triple(-1, 5, 3), Triple(1, 6, 6), Triple(-1, 4, 9))) {
                val by = top + at2
                if (by >= ground - 3) continue
                for (k in 1..len) {
                    val bx = x + side * k
                    val bey = by - k / 2
                    sheet.put(bx, bey, Tone.EDGE - 120)
                    // Moos: von jeder zweiten Astzelle ein Faden nach unten.
                    if (k % 2 == 0) {
                        val strand = 2 + hash(bx, i + 22) % 4
                        for (m in 1..strand) sheet.put(bx, bey + m, Tone.DEEP + 30)
                    }
                }
            }
        }
        // Nebel: zwei Baender, die langsam gegeneinander ziehen.
        val drift = PlayScene.beat(phase, 9)
        for ((row, dir) in listOf((maxH * 0.18f).roundToInt() to 1, (maxH * 0.34f).roundToInt() to -1)) {
            val y = ground - row
            for (x in 0 until widthCells) {
                if (Math.floorMod(x + dir * drift, 13) < 7 && sheet.at(x, y) == null) sheet.put(x, y, Tone.DEEP + 20)
            }
        }
        // Das Wasser: gebrochene Spiegelung der Staemme in der untersten Zeile.
        for (x in 0 until widthCells) {
            if (Math.floorMod(x + drift, 3) != 0) sheet.put(x, ground, Tone.DEEP + 60)
        }
        return sheet.cells()
    }

    // ---------------------------------------------------------------------------------------
    // Neue Landschaftsbilder (Ueberarbeitung 29.09.2026)
    // ---------------------------------------------------------------------------------------

    /**
     * Ein Zeichenblatt fuer eine Landschaft: Spaeter Gesetztes liegt vorn, gezeichnet wird nur
     * ueber dem Boden.
     */
    private class Sheet(val width: Int, val floorY: Int) {
        private val grid = HashMap<Pair<Int, Int>, SceneCell>()

        fun put(x: Int, y: Int, b: Int, light: Boolean = false) {
            if (x in 0 until width && y in 0 until floorY && b > 0) grid[x to y] = SceneCell(x, y, b, isLight = light)
        }

        fun at(x: Int, y: Int): SceneCell? = grid[x to y]

        fun shape(cells: List<Pair<Int, Int>>, x: Int, y: Int, b: Int, light: Boolean = false) {
            for ((px, py) in cells) put(x + px, y + py, b, light)
        }

        /** Eine Flaeche vom Profil [heights] bis zum Boden: Oberkante [edge], darunter [fill]. */
        fun ridge(heights: IntArray, edge: Int, fill: Int) {
            val ground = floorY - 1
            for (x in 0 until width) {
                val h = heights.getOrElse(x) { 0 }
                if (h <= 0) continue
                put(x, ground - h, edge)
                for (y in ground - h + 1 until ground) put(x, y, fill)
            }
        }

        fun cells(): List<SceneCell> = grid.values.toList()
    }

    /** Ein Profil aus Wellen: Grundhoehe [base] plus Sinusse (Frequenz, Hoehe, Versatz). */
    private fun wave(widthCells: Int, base: Float, vararg parts: Triple<Double, Float, Double>): IntArray =
        IntArray(widthCells) { x ->
            (base + parts.sumOf { (f, a, p) -> (a * sin(x * f + p)).toDouble() }.toFloat()).roundToInt()
        }

    /**
     * Eine Tanne als Silhouette: Stufen, die nach unten breiter werden. [x] ist die Mitte, [base]
     * die Zeile, auf der sie steht.
     */
    private fun Sheet.pine(x: Int, base: Int, height: Int, edge: Int, fill: Int) {
        val widest = (height / 3).coerceAtLeast(1)
        for (d in 0 until height) {
            val y = base - height + d
            // Vier Zeilen je Stufe; jede Stufe setzt schmaler an, als die vorige endete - daran
            // erkennt man eine Tanne und nicht bloss ein Dreieck.
            val half = (d / 4 + (d % 4) * 2 / 3).coerceAtMost(widest)
            for (dx in -half..half) put(x + dx, y, if (dx == -half || d == 0) edge else fill)
        }
        put(x, base, fill)
    }

    /**
     * **Die Wueste des Wuestenfuchses** (sein Park, siehe PlayScene.habitatPlacements).
     *
     * Ein Tafelberg mit Gesteinsschichten weit hinten, ein Duenenmeer davor und eine Karawane,
     * die ueber den fernen Kamm zieht. Die nahen Duenen haben die typische Form: flach im Wind,
     * steil im Lee, die Kante scharf beleuchtet. Tagsueber flimmert die Luft ueber dem Sand.
     */
    private fun desert(
        phase: Int,
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase
    ): List<SceneCell> {
        val maxH = skyRoom(floorY, 36)
        if (widthCells <= 0 || maxH < 8) return emptyList()
        val sheet = Sheet(widthCells, floorY)
        val ground = floorY - 1

        // Der Tafelberg: flache Kuppe, steile Flanken, waagerechte Schichten.
        val mesaFrom = (widthCells * 0.52f).roundToInt()
        val mesaTo = (widthCells * 0.90f).roundToInt()
        val mesaH = (maxH * 0.55f).roundToInt()
        for (x in mesaFrom..mesaTo) {
            val fromEdge = minOf(x - mesaFrom, mesaTo - x)
            val h = (mesaH - (3 - fromEdge).coerceAtLeast(0) * 3 - if (hash(x, 51) % 7 == 0) 1 else 0)
                .coerceAtLeast(0)
            val top = ground - h
            for (y in top until ground) {
                val depth = y - top
                val b = when {
                    // Fern und dunkel: eine Silhouette, vor der der helle Sand steht.
                    depth == 0 -> Tone.EDGE - 160
                    x - mesaFrom < 2 -> Tone.DEEP + 60 // die Lichtkante links
                    depth % 5 == 3 -> Tone.DEEP + 20 // Gesteinsschichten
                    else -> Tone.VOID
                }
                sheet.put(x, y, b)
            }
        }

        // Die fernen Duenen, und darueber die Karawane.
        val far = wave(widthCells, maxH * 0.20f, Triple(0.16, maxH * 0.05f, 0.4), Triple(0.41, maxH * 0.02f, 1.7))
        sheet.ridge(far, edge = Tone.EDGE - 60, fill = Tone.DEEP + 40)
        val camel = art(
            "..#.#...#",
            ".######.#",
            ".#######.",
            ".#.#.#.#."
        )
        val span = widthCells + 30
        for (i in 0 until 2) {
            val cx = Math.floorMod(PlayScene.beat(phase, 14) + i * 11 + (widthCells * 0.58f).roundToInt() + 15, span) - 15
            val crest = far.getOrElse((cx + 4).coerceIn(0, widthCells - 1)) { 0 }
            sheet.shape(camel, cx, ground - crest - 4, Tone.EDGE + 40)
        }

        // Die nahen Duenen: flach im Wind (links), steil im Lee (rechts).
        // Die Kaemme liegen dort, wo weder die Figur (0,20) noch eine Akazie davorsteht.
        val dunes = listOf(Triple(0.50f, 0.30f, 0.28f), Triple(0.99f, 0.22f, 0.24f))
        val near = IntArray(widthCells) { x ->
            dunes.maxOf { (at, height, windward) ->
                val crest = widthCells * at
                val lee = widthCells * 0.07f
                val t = if (x <= crest) (crest - x) / (widthCells * windward) else (x - crest) / lee
                if (t >= 1f) 0 else (maxH * height * (1f - t * t)).roundToInt()
            }
        }
        for (x in 0 until widthCells) {
            val h = near[x]
            if (h <= 0) continue
            val top = ground - h
            val lee = x > 0 && x < widthCells - 1 && near[x + 1] < h - 1
            for (y in top until ground) {
                val depth = y - top
                val b = when {
                    // Heller Sand in der Sonne, der Lee-Hang im Schatten.
                    depth == 0 -> if (lee) Tone.LIT - 100 else Tone.GLINT - 60
                    lee -> Tone.DEEP
                    // Windrippel auf der Luvseite: kurze schraege Striche.
                    Math.floorMod(x - depth * 2, 7) == 0 -> Tone.LIT - 120
                    else -> Tone.EDGE - 110
                }
                sheet.put(x, y, b)
            }
        }

        // Hitzeflimmern ueber dem fernen Sand - nur tagsueber.
        if (!isDark(dayPhase)) {
            val shimmer = PlayScene.beat(phase, 2)
            val y = ground - (maxH * 0.30f).roundToInt()
            for (x in 0 until widthCells) {
                if (Math.floorMod(x + shimmer, 9) < 2 && sheet.at(x, y) == null) sheet.put(x, y, Tone.DEEP - 40)
            }
        }
        return sheet.cells()
    }

    /**
     * **Der Teich als See.** Hinten das andere Ufer mit einer Baumlinie und einem Bootshaus; davor
     * die Wasserflaeche, in der sich die Baeume gebrochen spiegeln und kleine Wellen treiben. Vorher
     * stand am Teich nur Schilf auf der Bodenlinie - Wasser war nicht zu sehen.
     */
    private fun lakeShore(
        phase: Int,
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase
    ): List<SceneCell> {
        val maxH = skyRoom(floorY, 30)
        if (widthCells <= 0 || maxH < 10) return emptyList()
        val sheet = Sheet(widthCells, floorY)
        val ground = floorY - 1
        val waterRows = (maxH * 0.42f).roundToInt().coerceAtLeast(4)
        val horizon = ground - waterRows

        // Das andere Ufer: Baumkronen als runde Buckel, dazwischen einzelne Tannen.
        val shore = IntArray(widthCells) { x ->
            val bump = (1.8f * sin(x * 0.55 + 0.3)).roundToInt() + if (hash(x / 3, 61) % 3 == 0) 1 else 0
            (maxH * 0.16f).roundToInt() + bump
        }
        for (x in 0 until widthCells) {
            val top = horizon - shore[x]
            sheet.put(x, top, Tone.EDGE - 60)
            for (y in top + 1 until horizon) sheet.put(x, y, Tone.DEEP)
            // Die Uferlinie: wo Wasser und Land sich treffen, glaenzt es.
            sheet.put(x, horizon, Tone.EDGE + 40)
        }
        for (at in listOf(0.14f, 0.36f, 0.88f)) {
            sheet.pine((widthCells * at).roundToInt(), horizon - shore[(widthCells * at).roundToInt().coerceIn(0, widthCells - 1)] + 1,
                (maxH * 0.18f).roundToInt().coerceAtLeast(5), Tone.EDGE - 40, Tone.DEEP)
        }
        // Das Bootshaus am Ufer, mit Steg ins Wasser.
        val bx = (widthCells * 0.60f).roundToInt()
        sheet.shape(art("...#...", "..###..", ".#####.", "#######"), bx, horizon - 8, Tone.LIT - 200)
        sheet.shape(art(".#####.", ".#####.", ".##.##.", ".##.##."), bx, horizon - 4, Tone.HAZE + 40)
        sheet.put(bx + 2, horizon - 3, if (isDark(dayPhase)) Tone.WINDOW else Tone.VOID, light = isDark(dayPhase))
        for (dx in -5..0) sheet.put(bx + dx, horizon, Tone.LIT - 250)

        // Das Wasser: Spiegelung der Uferlinie, darunter treibende Wellen, nach vorn heller.
        val drift = PlayScene.beat(phase, 6)
        for (x in 0 until widthCells) {
            val reflect = shore[x].coerceAtMost(waterRows - 2)
            for (d in 1..reflect) {
                if (Math.floorMod(x + d + drift, 4) != 0) sheet.put(x, horizon + d, Tone.VOID)
            }
        }
        for (row in 2 until waterRows step 2) {
            val y = horizon + row
            val spacing = 9 - row / 3
            val dir = if (row % 4 == 0) 1 else -1
            for (x in 0 until widthCells) {
                if (Math.floorMod(x + dir * drift + row * 5, spacing.coerceAtLeast(5)) == 0) {
                    for (k in 0..2) sheet.put(x + k, y, Tone.EDGE - 120 + row * 15)
                }
            }
        }
        // Seerosen nah am Ufer.
        for (at in listOf(0.26f, 0.58f, 0.76f)) {
            val lx = (widthCells * at).roundToInt()
            sheet.shape(art(".##.", "####"), lx, ground - 2, Tone.LIT - 280)
        }
        return sheet.cells()
    }

    /**
     * **Der Wald in Staffeln.** Drei Reihen Tannen hintereinander, nach hinten kleiner und
     * dunkler - erst dadurch wird aus drei Baeumen auf einer Linie ein Wald, in den man
     * hineinsieht. Im hohen Wald der Eule ([dense]) stehen die Reihen enger und hoeher.
     */
    private fun forestLayers(
        phase: Int,
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase,
        dense: Boolean
    ): List<SceneCell> {
        val maxH = skyRoom(floorY, if (dense) 38 else 32)
        if (widthCells <= 0 || maxH < 10) return emptyList()
        val sheet = Sheet(widthCells, floorY)
        val ground = floorY - 1
        // Hinten ein Hang, damit die hinterste Reihe nicht auf der Bodenlinie steht.
        sheet.ridge(wave(widthCells, maxH * 0.22f, Triple(0.12, maxH * 0.05f, 1.0)), Tone.DEEP + 40, Tone.VOID)
        val rows = listOf(
            Triple(0.55f, 0.42f, Tone.DEEP + 40 to Tone.DEEP - 60),
            Triple(0.78f, 0.24f, Tone.EDGE - 160 to Tone.DEEP),
            Triple(1.0f, 0.10f, Tone.EDGE - 60 to Tone.VOID)
        )
        for ((r, row) in rows.withIndex()) {
            val (tall, lift, tones) = row
            val step = if (dense) 5 + r else 7 + r * 2
            // Erst die ganze Reihe als eine Flaeche sammeln, dann nur ihren Umriss aufhellen -
            // sonst zeichnen die Lichtkanten der einzelnen Baeume Schraffuren in die Nachbarn.
            val row = Sheet(widthCells, floorY)
            var x = (hash(r, 71) % step)
            while (x < widthCells + 4) {
                val h = (maxH * tall * (0.75f + (hash(x, r + 72) % 25) / 100f)).roundToInt()
                row.pine(x, ground - (maxH * lift).roundToInt(), h, 1, 1)
                x += step + hash(x, r + 73) % 3
            }
            for (cell in row.cells()) {
                val outline = row.at(cell.x, cell.y - 1) == null || row.at(cell.x - 1, cell.y) == null
                sheet.put(cell.x, cell.y, if (outline) tones.first else tones.second)
            }
        }
        return sheet.cells()
    }

    /**
     * **Die Wiese mit der alten Eiche.** Hinten sanfte Huegel, davor ein breiter Wiesenbuckel und
     * auf seinem Scheitel eine einzelne, grosse Eiche - das Wahrzeichen, an dem man die Wiese
     * erkennt. Auf der Blumenwiese ([flowers]) ist der Hang mit Bluetenpunkten besetzt.
     */
    private fun meadowHill(widthCells: Int, floorY: Int, flowers: Boolean): List<SceneCell> {
        val maxH = skyRoom(floorY, 30)
        if (widthCells <= 0 || maxH < 10) return emptyList()
        val sheet = Sheet(widthCells, floorY)
        val ground = floorY - 1
        sheet.ridge(
            wave(widthCells, maxH * 0.30f, Triple(0.10, maxH * 0.06f, 2.2), Triple(0.27, maxH * 0.02f, 0.5)),
            Tone.EDGE - 160, Tone.DEEP - 30
        )
        val crest = (widthCells * 0.62f).roundToInt()
        val halfWidth = widthCells * 0.46f
        val hill = IntArray(widthCells) { x ->
            val t = (x - crest) / halfWidth
            (maxH * (0.06f + 0.18f * (1f - t * t).coerceAtLeast(0f))).roundToInt()
        }
        sheet.ridge(hill, Tone.LIT - 250, Tone.VOID + 30)
        if (flowers) {
            for (x in 0 until widthCells) {
                for (d in 1 until hill[x] - 1) {
                    if (hash(x, d + 81) % 9 == 0) sheet.put(x, ground - hill[x] + d, Tone.EDGE)
                }
            }
        }
        // Die Eiche auf dem Scheitel: kraeftiger Stamm, runde Krone, oben links im Licht.
        val base = ground - hill[crest]
        val crownR = (maxH * 0.16f).roundToInt().coerceIn(3, 7)
        val trunkH = crownR + 1
        for (d in 1..trunkH) {
            sheet.put(crest, base - d, Tone.EDGE - 40)
            sheet.put(crest + 1, base - d, Tone.DEEP + 60)
        }
        val cy = base - trunkH - crownR + 1
        for (dy in -crownR..crownR) for (dx in -crownR - 2..crownR + 2) {
            val nx = dx / (crownR + 2f)
            val ny = dy / crownR.toFloat()
            val r = nx * nx + ny * ny
            if (r > 1f || (r > 0.8f && hash(crest + dx, dy + 82) % 3 == 0)) continue
            val b = when {
                nx + ny < -0.7f -> Tone.LIT - 150
                nx + ny > 0.6f -> Tone.DEEP + 20
                else -> Tone.HAZE
            }
            sheet.put(crest + dx, cy + dy, b)
        }
        return sheet.cells()
    }

    /**
     * **Das Ackerland der Ebene.** Hinten flache Huegel, davor Felder als Streifen - gepfluegt,
     * Stoppeln, Getreide -, ein Hof mit Scheune und Silo und eine Pappelreihe am Weg. Die
     * Windmuehle steht als eigene Requisite davor (siehe [furnishing]).
     */
    private fun farmland(
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase
    ): List<SceneCell> {
        val maxH = skyRoom(floorY, 30)
        if (widthCells <= 0 || maxH < 8) return emptyList()
        val sheet = Sheet(widthCells, floorY)
        val ground = floorY - 1
        val hills = wave(widthCells, maxH * 0.44f, Triple(0.09, maxH * 0.05f, 0.8), Triple(0.23, maxH * 0.02f, 2.0))
        sheet.ridge(hills, Tone.EDGE - 60, Tone.DEEP + 10)
        // Felder: drei Streifen uebereinander, jeder mit eigenem Muster.
        val fieldTop = ground - (maxH * 0.30f).roundToInt()
        for (y in fieldTop until ground) {
            val band = (y - fieldTop) * 3 / (ground - fieldTop).coerceAtLeast(1)
            for (x in 0 until widthCells) {
                val b = when (band) {
                    0 -> if (x % 2 == 0) Tone.HAZE + 80 else Tone.DEEP + 40 // Getreide
                    1 -> if (y % 2 == 0) Tone.EDGE - 220 else Tone.DEEP - 40 // gepfluegt
                    else -> if (x % 3 == 0) Tone.HAZE else Tone.VOID + 50 // Stoppeln
                }
                sheet.put(x, y, b)
            }
        }
        for (x in 0 until widthCells) sheet.put(x, fieldTop, Tone.EDGE)
        // Der Hof: Wohnhaus, Scheune, daneben das Silo - mitten im Bild, ueber dem Heuballen,
        // wo weder die Figur noch die Windmuehle davorstehen.
        val fx = (widthCells * 0.40f).roundToInt()
        sheet.shape(art("..#..", ".###.", "#####"), fx, fieldTop - 6, Tone.LIT - 100)
        sheet.shape(art("#####", "#####", "##.##"), fx, fieldTop - 3, Tone.HAZE + 80)
        if (isDark(dayPhase)) sheet.put(fx + 1, fieldTop - 2, Tone.WINDOW, light = true)
        sheet.shape(art("..###..", ".#####.", "#######"), fx + 6, fieldTop - 8, Tone.EDGE + 40)
        sheet.shape(art("#######", "#######", "#######", "#######", "###.###"), fx + 6, fieldTop - 5, Tone.EDGE - 180)
        sheet.shape(art(".##.", "####", "####", "####", "####", "####", "####", "####"), fx + 14, fieldTop - 8, Tone.HAZE + 100)
        sheet.shape(art(".##.", "#...", "#...", "#..."), fx + 14, fieldTop - 8, Tone.LIT)
        // Pappeln am Feldweg: schlanke, hohe Kronen.
        for (at in listOf(0.18f, 0.24f, 0.72f, 0.78f)) {
            val px = (widthCells * at).roundToInt()
            val h = (maxH * 0.30f).roundToInt().coerceAtLeast(7)
            for (d in 0 until h) {
                val y = fieldTop - 1 - d
                sheet.put(px, y, if (d == h - 1) Tone.EDGE else Tone.DEEP + 90)
                if (d in 2 until h - 2) sheet.put(px - 1, y, Tone.DEEP)
                if (d in 3 until h - 3) sheet.put(px + 1, y, Tone.VOID + 40)
            }
        }
        return sheet.cells()
    }

    /**
     * **Felsnadeln** - der Park des Drachen (siehe PlayScene.habitatPlacements): hohe, schmale,
     * leicht geneigte Felstuerme mit ausgefransten Flanken und Rissen, hinten ein Dunstband. Eine
     * Landschaft zum Hinaufklettern - und bewusst ohne gerade Kanten, sonst lesen sich die Nadeln
     * als Hochhaeuser.
     */
    private fun crags(widthCells: Int, floorY: Int): List<SceneCell> {
        val maxH = skyRoom(floorY, 38)
        if (widthCells <= 0 || maxH < 10) return emptyList()
        val sheet = Sheet(widthCells, floorY)
        val ground = floorY - 1
        sheet.ridge(wave(widthCells, maxH * 0.30f, Triple(0.21, maxH * 0.05f, 0.3)), Tone.DEEP + 60, Tone.VOID + 20)
        val spires = listOf(
            Triple(0.18f, 0.72f, 5), Triple(0.30f, 0.52f, 4), Triple(0.58f, 0.98f, 6),
            Triple(0.68f, 0.66f, 5), Triple(0.90f, 0.82f, 5)
        )
        for ((n, spire) in spires.withIndex()) {
            val (at, tall, width) = spire
            val cx = (widthCells * at).roundToInt()
            val h = (maxH * tall).roundToInt()
            // Leicht geneigt, nach oben spitz zulaufend, die Flanken ausgefranst - Fels, kein Haus.
            val lean = if (n % 2 == 0) 1 else -1
            for (d in 0 until h) {
                val y = ground - h + d
                val t = d / h.toFloat()
                val half = (width * sqrt(t) + (hash(d, n + 94) % 3 - 1) * 0.6f).roundToInt().coerceAtLeast(0)
                val shift = lean * ((h - d) / 7)
                val left = cx + shift - half
                val right = cx + shift + half + (hash(d / 2, n + 95) % 2)
                for (x in left..right) {
                    val b = when {
                        x == left -> Tone.LIT - 120 // Lichtkante links
                        x >= right - 1 -> Tone.DEEP // Schattenflanke
                        // Risse: kurze, senkrechte dunkle Linien.
                        hash(x, n + 96) % 5 == 0 && d % 6 < 4 -> Tone.DEEP + 30
                        else -> Tone.HAZE - 60
                    }
                    sheet.put(x, y, b)
                }
            }
        }
        // Ein Dunstband zwischen den Nadeln.
        val hazeY = ground - (maxH * 0.22f).roundToInt()
        for (x in 0 until widthCells) if (x % 3 != 0) sheet.put(x, hazeY, Tone.DEEP + 30)
        return sheet.cells()
    }

    /**
     * **Das Lager:** flache Huegel und eine niedrige Baumreihe - der Himmel bleibt weit offen,
     * damit nachts die Sterne ueber dem Feuer stehen. Mehr braucht es nicht: Das Bild traegt das
     * Feuer.
     */
    private fun campBackdrop(widthCells: Int, floorY: Int): List<SceneCell> {
        val maxH = skyRoom(floorY, 24)
        if (widthCells <= 0 || maxH < 8) return emptyList()
        val sheet = Sheet(widthCells, floorY)
        val ground = floorY - 1
        sheet.ridge(wave(widthCells, maxH * 0.34f, Triple(0.11, maxH * 0.08f, 1.9)), Tone.EDGE - 200, Tone.DEEP - 60)
        val row = Sheet(widthCells, floorY)
        var x = 2
        while (x < widthCells) {
            row.pine(x, ground, (maxH * 0.30f).roundToInt() + hash(x, 101) % 3, 1, 1)
            x += 5 + hash(x, 102) % 3
        }
        for (cell in row.cells()) {
            val outline = row.at(cell.x, cell.y - 1) == null || row.at(cell.x - 1, cell.y) == null
            sheet.put(cell.x, cell.y, if (outline) Tone.EDGE - 120 else Tone.VOID)
        }
        return sheet.cells()
    }

    /**
     * **Das Lagerfeuer:** Flammen in drei Formen, die sich abloesen, ein heller Kern, ab und zu ein
     * Funke, der aufsteigt. Abends und nachts faellt ein warmer Schein auf den Boden. Alles Licht
     * ([SceneCell.isLight]) - das Feuer dimmt nicht mit der Nacht, es ist das, was sie erhellt.
     */
    private fun flames(phase: Int, cx: Int, floorY: Int, dark: Boolean): List<SceneCell> {
        val frames = listOf(
            // Zwei, drei Zungen, die sich abwechselnd strecken - ein Kegel saehe aus wie ein Zelt.
            art("..#....", "..#..#.", ".##..#.", ".##.##.", ".#####.", "#######", ".#####."),
            art("....#..", ".#..#..", ".#.##..", ".####.#", ".######", "#######", ".#####."),
            art("...#...", "#..#...", "#.##.#.", "#.####.", ".#####.", "#######", ".#####.")
        )
        val frame = frames[PlayScene.beat(phase, 1) % frames.size]
        val top = floorY - 8
        val outer = if (dark) PlayScene.GLOW - 600 else PlayScene.FURNITURE
        val inner = if (dark) PlayScene.GLOW else PlayScene.FURNITURE + 400
        val cells = mutableListOf<SceneCell>()
        for ((px, py) in frame) {
            val core = py >= 4 && px in 2..4
            cells += SceneCell(cx - 3 + px, top + py, if (core) inner else outer, isLight = true)
        }
        // Ein Funke steigt auf und verlischt.
        val spark = phase % 8
        if (spark < 5) {
            val sx = cx + (hash(phase / 8, 103) % 3) - 1
            cells += SceneCell(sx, top - 1 - spark, (outer - spark * 250).coerceAtLeast(400), isLight = true)
        }
        // Der Schein auf dem Boden.
        if (dark) {
            for (dx in -9..9) {
                val b = PlayScene.GLOW - 900 - abs(dx) * 140
                if (b > 300) {
                    cells += SceneCell(cx + dx, floorY, b, isLight = true)
                    cells += SceneCell(cx + dx, floorY + 1, b / 2, isLight = true)
                }
            }
        }
        return cells.filter { it.x >= 0 && it.y >= 0 }
    }

    /**
     * **Der Stadtpark** - Pufflings Park: hinter den Baumkronen am Parkrand steht die Stadt, damit
     * man weiss, dass dies ein Park IN der Stadt ist. Abends gehen dort einzelne Fenster an.
     */
    private fun cityPark(
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase
    ): List<SceneCell> {
        val maxH = skyRoom(floorY, 30)
        if (widthCells <= 0 || maxH < 10) return emptyList()
        val sheet = Sheet(widthCells, floorY)
        val ground = floorY - 1
        var x = 0
        var i = 0
        while (x < widthCells) {
            val w = 4 + hash(i, 91) % 4
            val h = (maxH * (0.34f + (hash(i, 92) % 30) / 100f)).roundToInt()
            for (cx in x until (x + w).coerceAtMost(widthCells)) {
                for (y in ground - h until ground) {
                    val edge = y == ground - h || cx == x
                    val window = (cx - x) % 2 == 1 && (y - (ground - h)) % 3 == 1 && !edge
                    when {
                        edge -> sheet.put(cx, y, Tone.DEEP + 70)
                        window && isDark(dayPhase) && hash(cx * 5 + i, y) % 4 == 0 ->
                            sheet.put(cx, y, Tone.WINDOW - 700, light = true)
                        else -> sheet.put(cx, y, Tone.VOID)
                    }
                }
            }
            x += w + 1
            i++
        }
        // Die Baumkronen am Parkrand davor: runde Buckel.
        sheet.ridge(
            IntArray(widthCells) { cx ->
                (maxH * 0.20f + 2.2f * abs(sin(cx * 0.42)).toFloat()).roundToInt()
            },
            Tone.EDGE - 80, Tone.DEEP - 30
        )
        return sheet.cells()
    }

    /**
     * Das Hoehlengewoelbe: ein unregelmaessiger Bogen aus Fels, von dem Tropfsteine haengen, und
     * dahinter die Dunkelheit. Kein Himmel - dafuer leuchten die Kristalle (siehe [furnishing]).
     */
    private fun caveVault(widthCells: Int, floorY: Int): List<SceneCell> {
        if (widthCells <= 0 || floorY < 12) return emptyList()
        val cells = mutableListOf<SceneCell>()
        val top = skyRoom(floorY, 22)
        for (x in 0 until widthCells) {
            // Die Decke haengt tief und wellig; zu den Seiten laeuft sie als Wand herunter.
            val edge = minOf(x, widthCells - 1 - x)
            val ceiling = floorY - top + (hash(x, 31) % 3) + (if (edge < 4) 0 else 2)
            for (y in (ceiling - 2).coerceAtLeast(0)..ceiling) cells += SceneCell(x, y, FAR + 80)
            if (edge < 3) for (y in ceiling until floorY) cells += SceneCell(x, y, FAR + 40)
            // Tropfsteine von der Decke, jeder vierte bis siebte Spalte.
            if (hash(x, 32) % 6 == 0 && edge >= 3) {
                val len = 1 + hash(x, 33) % 4
                for (d in 1..len) cells += SceneCell(x, ceiling + d, FAR + 40)
            }
        }
        return cells.filter { it.x in 0 until widthCells && it.y >= 0 }
    }

    /**
     * Das Meer: Horizontlinie, Wellen, die langsam zum Ufer laufen, ein Saum Gischt am Strand
     * und ein Segelboot, das ueber den Horizont zieht.
     */
    private fun sea(
        phase: Int,
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase
    ): List<SceneCell> {
        if (widthCells <= 0 || floorY < 12) return emptyList()
        val horizon = floorY - 16
        val cells = mutableListOf<SceneCell>()
        val drift = PlayScene.beat(phase, 3)
        // Die Sonne: tagsueber hoch ueber dem Wasser, abends halb im Meer versunken.
        val sunX = (widthCells * 0.72f).toInt()
        val sunY = if (dayPhase == PlayAmbientActivity.DayPhase.EVENING) horizon - 2 else horizon - 22
        if (dayPhase != PlayAmbientActivity.DayPhase.NIGHT && sunY > 2) {
            for (dy in -3..3) for (dx in -3..3) {
                if (dx * dx + dy * dy > 10) continue
                val y = sunY + dy
                if (y >= horizon) continue
                val rim = dx * dx + dy * dy > 5
                cells += SceneCell(sunX + dx, y, if (rim) PlayScene.GLOW - 700 else PlayScene.GLOW, isLight = true)
            }
        }
        for (x in 0 until widthCells) cells += SceneCell(x, horizon, NEAR + 80)
        // Wellenkaemme: je Zeile kurze Striche, die sich gegeneinander verschieben - naeher am
        // Ufer dichter und heller. Jede zweite Zeile bleibt frei, sonst wird es Rauschen.
        for (row in 2..12 step 2) {
            val y = horizon + row
            val spacing = 11 - row / 2
            val shift = drift * (if (row % 4 == 0) 1 else -1)
            for (x in 0 until widthCells) {
                val m = Math.floorMod(x + shift + row * 3, spacing)
                if (m == 0 || m == 1) cells += SceneCell(x, y, FAR + row * 18)
            }
        }
        // Gischt am Ufer: kommt und geht.
        val surf = PlayScene.beat(phase, 5) % 4
        for (x in 0 until widthCells) {
            if ((x + surf) % 3 != 0) cells += SceneCell(x, floorY - 3 + (if (surf == 3) 1 else 0), NEAR)
        }
        // Mondlicht bzw. Sonnenglitzern auf dem Wasser - eine helle Bahn.
        val glitterX = (widthCells * 0.74f).toInt()
        val glitterB = if (isDark(dayPhase)) PlayScene.GLOW - 1200 else PlayScene.FURNITURE - 200
        for (row in 1..6) {
            if ((row + drift) % 2 == 0) cells += SceneCell(glitterX + row % 3 - 1, horizon + row, glitterB, isLight = isDark(dayPhase))
        }
        // Segelboot am Horizont.
        val span = widthCells + 8
        val boatX = span - 1 - PlayScene.beat(phase, 12) % span
        cells += place(art(
            "..#...",
            "..##..",
            "..###.",
            "..####",
            "######",
            ".####."
        ), boatX - 4, horizon - 5, NEAR + 120)
        // **Der Leuchtturm auf der Landzunge** - das Wahrzeichen des Strandes. Gestreift, damit man
        // ihn auch klein als Leuchtturm liest; nachts kreist sein Licht, der Strahl zeigt
        // abwechselnd aufs Meer hinaus und ueber das Land.
        val capeFrom = (widthCells * 0.80f).roundToInt()
        for (x in capeFrom until widthCells) {
            val h = ((x - capeFrom) * 0.7f).roundToInt().coerceAtMost(5)
            for (y in horizon - h..horizon) cells += SceneCell(x, y, if (y == horizon - h) Tone.EDGE - 100 else Tone.DEEP)
        }
        val towerX = (widthCells * 0.91f).roundToInt()
        val towerBase = horizon - 5
        for (d in 0 until 10) {
            for (dx in -1..1) {
                val band = if ((d / 2) % 2 == 0) Tone.LIT - 250 else Tone.HAZE - 80
                cells += SceneCell(towerX + dx, towerBase - d, if (dx == 1) band - 120 else band)
            }
        }
        val lampY = towerBase - 11
        cells += SceneCell(towerX - 1, lampY + 1, Tone.EDGE)
        cells += SceneCell(towerX + 1, lampY + 1, Tone.EDGE)
        cells += SceneCell(towerX, lampY - 1, Tone.EDGE)
        if (isDark(dayPhase)) {
            cells += SceneCell(towerX, lampY, PlayScene.GLOW, isLight = true)
            val turn = PlayScene.beat(phase, 4) % 4
            val dir = when (turn) { 0 -> -1; 2 -> 1; else -> 0 }
            if (dir != 0) {
                for (k in 1..(if (dir < 0) 9 else 4)) {
                    cells += SceneCell(towerX + dir * k, lampY, (PlayScene.GLOW - 300 - k * 140).coerceAtLeast(300), isLight = true)
                }
            }
        } else {
            cells += SceneCell(towerX, lampY, Tone.LIT)
        }
        return cells.filter { it.x in 0 until widthCells && it.y >= 0 }
    }

    /**
     * Die Skyline hinter der Stadt: Hochhaeuser in der Ferne, nachts mit einzelnen erleuchteten
     * Fenstern und einem blinkenden Warnlicht auf dem hoechsten Turm.
     */
    private fun skyline(
        phase: Int,
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase
    ): List<SceneCell> {
        val maxH = skyRoom(floorY, 34)
        if (maxH < 14 || widthCells <= 0) return emptyList()
        val cells = mutableListOf<SceneCell>()
        var x = 0
        var i = 0
        var tallest = -1 to 0
        while (x < widthCells) {
            val w = 5 + hash(i, 31) % 4
            val h = (maxH * (0.55f + (hash(i, 32) % 45) / 100f)).roundToInt()
            val top = floorY - 1 - h
            if (h > tallest.second) tallest = (x + w / 2) to h
            for (cx in x until (x + w).coerceAtMost(widthCells)) {
                for (y in top until floorY - 1) {
                    val edge = y == top || cx == x || cx == x + w - 1
                    val window = (cx - x) % 2 == 1 && (y - top) % 3 == 1 && !edge
                    val b = when {
                        edge -> FAR
                        window && isDark(dayPhase) && hash(cx * 7 + i, y) % 5 == 0 -> -1
                        window -> FAR - 140
                        else -> FAR - 200
                    }
                    if (b == -1) cells += SceneCell(cx, y, PlayScene.GLOW - 1500, isLight = true)
                    else if (b > 0) cells += SceneCell(cx, y, b)
                }
            }
            x += w + 1 + hash(i, 33) % 2
            i++
        }
        // Antenne mit Warnlicht auf dem hoechsten Turm.
        val (ax, ah) = tallest
        val antennaTop = floorY - 1 - ah - 4
        for (y in antennaTop until antennaTop + 4) cells += SceneCell(ax, y, FAR + 60)
        val blink = PlayScene.beat(phase, 5) % 2 == 0
        cells += SceneCell(ax, antennaTop - 1, if (blink) PlayScene.GLOW - 300 else PlayScene.GLOW - 1500, isLight = true)
        return cells.filter { it.x in 0 until widthCells && it.y >= 0 }
    }

    // ---------------------------------------------------------------------------------------
    // Der Supermarkt
    // ---------------------------------------------------------------------------------------

    private enum class Aisle { PRODUCE, DRINKS, COOLER, BREAD, CANS, BOXES }

    /**
     * **Die Regalwand des Supermarkts** - Getraenke, Dosen, Kuehlregal, Kartons, Gemuese und Brot,
     * dazu Gangschilder und Deckenlicht.
     *
     * Prozedural und nicht als einzelne Requisiten: Der Laden muss auch bei vierzig Spalten
     * funktionieren, und zwischen Warenregal und Kasse bleibt dort kaum Platz. Die Wand fuellt,
     * was frei ist - schmal ein, zwei Abschnitte, breit alle sechs -, und laesst die Spalten der
     * Vordergrund-Requisiten ([foreground]) aus, damit sie nicht durch Regal und Kasse
     * hindurchscheint. Die Pruefung "zwei klar getrennte Funktionsbereiche" bleibt damit gueltig:
     * vorn stehen weiterhin nur Regal, Kasse und Tuer.
     */
    private fun shopBackWall(
        phase: Int,
        widthCells: Int,
        floorY: Int,
        foreground: List<Pair<IntRange, Int>>
    ): List<SceneCell> {
        if (widthCells <= 0 || floorY < 18) return emptyList()
        val room = PlayScene.roomWidth(widthCells)
        val shift = (widthCells - room) / 2
        // Ueber ALLES hinweg, was niedrig genug ist, dass darueber noch Wand bleibt: Ueber der
        // Kasse und dem Warenregal stehen die oberen Regalboeden, nur die Tuer (fast so hoch wie
        // der Raum) bekommt eine ganz freie Spalte.
        val blocked = BooleanArray(widthCells)
        for ((range, top) in foreground) {
            if (top > floorY - 12) continue
            for (x in (range.first - 1)..(range.last + 1)) if (x in 0 until widthCells) blocked[x] = true
        }
        fun hidden(cell: SceneCell) = foreground.any { (range, top) ->
            cell.x in (range.first - 1)..(range.last + 1) && cell.y >= top - 1
        }
        val cells = mutableListOf<SceneCell>()
        val ceiling = floorY - 18
        // Deckenlicht: Leuchtroehren ueber die ganze Raumbreite.
        for (x in shift until shift + room) {
            if ((x - shift) % 10 in 2..6) cells += SceneCell(x, ceiling + 1, PlayScene.GLOW - 1300, isLight = true)
        }
        // Freie Strecken der Wand suchen und mit Abschnitten fuellen.
        var x = shift
        var segment = 0
        while (x < shift + room) {
            if (blocked[x]) { x++; continue }
            var end = x
            while (end + 1 < shift + room && !blocked[end + 1]) end++
            var sx = x
            while (end - sx + 1 >= SEGMENT_MIN) {
                val w = (end - sx + 1).coerceAtMost(SEGMENT_WIDTH)
                val aisle = Aisle.entries[segment % Aisle.entries.size]
                cells += aisleSegment(aisle, sx, w, floorY, phase)
                sx += w + 1
                segment++
            }
            x = end + 1
        }
        return cells.filter { it.x in 0 until widthCells && it.y >= 0 && !hidden(it) }
    }

    private const val SEGMENT_WIDTH = 9
    private const val SEGMENT_MIN = 5

    private fun aisleSegment(aisle: Aisle, x0: Int, w: Int, floorY: Int, phase: Int): List<SceneCell> {
        val cells = mutableListOf<SceneCell>()
        val top = floorY - 15
        val frame = FAR + 60
        val goods = NEAR + 60
        val x1 = x0 + w - 1
        // Gangschild darueber, an zwei Faeden von der Decke.
        val signY = floorY - 17
        for (sx in x0 + 1..x1 - 1) cells += SceneCell(sx, signY, PlayScene.GLOW - 1500, isLight = true)
        when (aisle) {
            Aisle.COOLER -> {
                // Kuehlregal: Glastueren mit kaltem Licht dahinter, das leise flimmert.
                for (y in top..floorY - 1) {
                    cells += SceneCell(x0, y, frame)
                    cells += SceneCell(x1, y, frame)
                }
                for (sx in x0..x1) {
                    cells += SceneCell(sx, top, frame)
                    cells += SceneCell(sx, floorY - 1, frame)
                }
                val mid = (x0 + x1) / 2
                for (y in top + 1 until floorY - 1) cells += SceneCell(mid, y, frame)
                val hum = PlayScene.beat(phase, 9) % 3
                for (y in top + 1 until floorY - 1) {
                    for (sx in x0 + 1 until x1) {
                        if (sx == mid) continue
                        val shelf = (y - top) % 4 == 0
                        val item = (y - top) % 4 in 1..2 && (sx + y) % 2 == 0
                        when {
                            shelf -> cells += SceneCell(sx, y, frame)
                            item -> cells += SceneCell(sx, y, goods)
                            (sx + y + hum) % 5 == 0 -> cells += SceneCell(sx, y, PlayScene.GLOW - 1600, isLight = true)
                        }
                    }
                }
            }
            Aisle.PRODUCE -> {
                // Gemuesetheke: schraege Kisten mit runden Fruechten, darueber ein Spiegelband.
                for (sx in x0..x1) cells += SceneCell(sx, top + 2, frame)
                for (k in 0..2) {
                    val crateTop = floorY - 4 - k * 3
                    val inset = k
                    for (sx in x0 + inset..x1 - inset) {
                        cells += SceneCell(sx, crateTop + 2, frame)
                        if ((sx + k) % 2 == 0) cells += SceneCell(sx, crateTop + 1, goods)
                        if ((sx + k) % 4 == 1) cells += SceneCell(sx, crateTop, goods)
                    }
                }
                for (y in floorY - 3..floorY - 1) {
                    cells += SceneCell(x0, y, frame)
                    cells += SceneCell(x1, y, frame)
                }
            }
            else -> {
                // Offenes Regal: drei Boeden, Seitenwangen, Ware je nach Gang.
                for (y in top..floorY - 1) {
                    cells += SceneCell(x0, y, frame)
                    cells += SceneCell(x1, y, frame)
                }
                val boards = listOf(top, top + 5, top + 10, floorY - 1)
                for (by in boards) for (sx in x0..x1) cells += SceneCell(sx, by, frame)
                for (b in 1 until boards.size) {
                    val board = boards[b]
                    for (sx in x0 + 1 until x1) {
                        val rel = sx - x0
                        when (aisle) {
                            // Flaschen: schmal und hoch, mit Hals.
                            Aisle.DRINKS -> if (rel % 2 == 1) {
                                cells += SceneCell(sx, board - 1, goods)
                                cells += SceneCell(sx, board - 2, goods)
                                cells += SceneCell(sx, board - 3, goods - 120)
                            }
                            // Dosen: gestapelt, dicht an dicht.
                            Aisle.CANS -> {
                                cells += SceneCell(sx, board - 1, goods)
                                if (rel % 3 != 0) cells += SceneCell(sx, board - 2, goods - 80)
                            }
                            // Kartons: breite Bloecke mit Fuge.
                            Aisle.BOXES -> if (rel % 4 != 0) {
                                for (dy in 1..3) cells += SceneCell(sx, board - dy, if (dy == 3) goods else goods - 100)
                            }
                            // Brot: Laibe - flache Buckel.
                            Aisle.BREAD -> {
                                if (rel % 3 != 0) cells += SceneCell(sx, board - 1, goods)
                                if (rel % 3 == 1) cells += SceneCell(sx, board - 2, goods - 60)
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
        return cells
    }

    // ---------------------------------------------------------------------------------------
    // Leute, Tiere, Fahrzeuge - die Ebene zwischen Hintergrund und Vordergrund
    // ---------------------------------------------------------------------------------------

    /** Ein Passant, drei Zellen breit und sieben hoch - zwei Gangbilder. */
    private val WALKER_A = art(".#.", "###", "###", ".#.", ".#.", "#.#", "#.#")
    private val WALKER_B = art(".#.", "###", "###", ".#.", ".#.", ".#.", ".#.")

    /** Mit Hut - damit nicht alle gleich aussehen. */
    private val HATTED_A = art("###", ".#.", "###", "###", ".#.", "#.#", "#.#")
    private val HATTED_B = art("###", ".#.", "###", "###", ".#.", ".#.", ".#.")

    /** Ein Kind - kleiner, huepft beim Gehen. */
    private val CHILD_A = art(".#.", "###", ".#.", "#.#")
    private val CHILD_B = art(".#.", "###", ".#.", ".#.")

    /** Ein Hund, der vorausgeht - Schwanz oben, Kopf vorn, zwei Beinstellungen. */
    private val DOG_A = art("#...##", "#####.", ".#.#..")
    private val DOG_B = art("#...##", "#####.", "#...#.")

    /** Einkaufswagen - Griff, Korb, Raeder. */
    private val CART = art("#....", ".####", ".#..#", ".####", "..#.#")

    /** Auto von der Seite: Dach, Fenster, Karosserie, Raeder. */
    private val CAR = art(
        "...#####....",
        "..#..#..#...",
        "############",
        "############",
        "..##....##.."
    )
    private const val CAR_WIDTH = 12

    /** Stadtbus - lang, mit Fensterreihe. */
    private val BUS = art(
        "####################",
        "#..#..#..#..#..#..##",
        "#..#..#..#..#..#..##",
        "####################",
        "####################",
        "..##...........##..."
    )
    private const val BUS_WIDTH = 20

    /**
     * Wer gerade hinter dem Vordergrund vorbeikommt - Leute mit Hund, Autos, Kundschaft.
     *
     * Wird ZWISCHEN den Hintergrund-Requisiten (Fassaden, Baeume) und dem Vordergrund
     * gezeichnet: Die Leute gehen vor den Haeusern, aber hinter Bank und Laterne vorbei, und die
     * Figur steht immer ganz vorn.
     */
    fun midground(
        place: PlayScene.Place,
        phase: Int,
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase,
        placements: List<Placement> = emptyList()
    ): List<SceneCell> {
        if (widthCells <= 0 || floorY < 8) return emptyList()
        val night = isNight(dayPhase)
        val cells = mutableListOf<SceneCell>()
        when (place) {
            PlayScene.Place.STREET, PlayScene.Place.CITY -> {
                cells += traffic(place, phase, widthCells, floorY, dayPhase)
                cells += walker(phase, widthCells, floorY, slot = 0, withDog = true, rightward = true)
                if (!night) {
                    cells += walker(phase, widthCells, floorY, slot = 1, withDog = false, rightward = false)
                    if (place == PlayScene.Place.CITY) {
                        cells += walker(phase, widthCells, floorY, slot = 2, withDog = true, rightward = false)
                    }
                }
            }
            // Draussen im Gruenen: gelegentlich jemand, der seinen Hund ausfuehrt.
            PlayScene.Place.PARK, PlayScene.Place.MEADOW, PlayScene.Place.PLAINS, PlayScene.Place.BEACH ->
                if (!night) {
                    cells += walker(phase, widthCells, floorY, slot = 3, withDog = true, rightward = place != PlayScene.Place.BEACH)
                    if (place == PlayScene.Place.BEACH) {
                        cells += walker(phase, widthCells, floorY, slot = 4, withDog = false, rightward = true, child = true)
                    }
                }
            // Im Laden: Kundschaft mit Einkaufswagen.
            PlayScene.Place.SHOP -> {
                cells += shopper(phase, widthCells, floorY, slot = 0, rightward = true)
                cells += shopper(phase, widthCells, floorY, slot = 1, rightward = false)
            }
            // Im Cafe: die Barista hinter der Theke, ein Gast am Tischchen, ab und zu jemand,
            // der hereinkommt.
            PlayScene.Place.CAFE -> {
                cells += cafeRegulars(phase, widthCells, floorY, placements)
                cells += walker(phase, widthCells, floorY, slot = 6, withDog = false, rightward = false)
            }
            // In der Spielhalle: Leute, die zwischen den Automaten umhergehen.
            PlayScene.Place.ARCADE ->
                cells += walker(phase, widthCells, floorY, slot = 5, withDog = false, rightward = false)
            // Auf der Ebene grasen Kuehe.
            else -> Unit
        }
        if (place == PlayScene.Place.PLAINS) cells += cows(phase, widthCells, floorY)
        return cells.filter { it.x in 0 until widthCells && it.y >= 0 }
    }

    /**
     * Wo ein Laeufer im Takt gerade ist - `null`, solange er nicht im Bild ist.
     *
     * Jeder [slot] hat seinen eigenen Rhythmus: Sie kommen nicht gleichzeitig und nicht im selben
     * Abstand, und etwa die Haelfte der Zeit ist niemand unterwegs - ein Weg, auf dem immer jemand
     * laeuft, ist ein Fliessband.
     */
    private fun lane(phase: Int, widthCells: Int, slot: Int, ticksPerCell: Int, extent: Int): Int? {
        val span = widthCells + extent * 2
        val cycle = span * (2 + slot % 2)
        val step = (PlayScene.beat(phase, ticksPerCell) + slot * 37) % cycle
        return if (step < span) step - extent else null
    }

    private fun walker(
        phase: Int,
        widthCells: Int,
        floorY: Int,
        slot: Int,
        withDog: Boolean,
        rightward: Boolean,
        child: Boolean = false
    ): List<SceneCell> {
        val pos = lane(phase, widthCells, slot, ticksPerCell = 3, extent = 12) ?: return emptyList()
        val x = if (rightward) pos else widthCells - 1 - pos
        val stride = PlayScene.beat(phase, 3) % 2 == 0
        val hatted = slot % 2 == 1
        val body = when {
            child -> if (stride) CHILD_A else CHILD_B
            hatted -> if (stride) HATTED_A else HATTED_B
            else -> if (stride) WALKER_A else WALKER_B
        }
        val height = body.maxOf { it.second } + 1
        val cells = mutableListOf<SceneCell>()
        val top = floorY - height
        cells += place(body, x, top, MID)
        if (withDog) {
            // Der Hund laeuft vorneweg, an der Leine.
            val dogShape = if (stride) DOG_B else DOG_A
            val dogX = if (rightward) x + 6 else x - 8
            val dog = if (rightward) dogShape else mirror(dogShape, 6)
            cells += place(dog, dogX, floorY - 3, MID)
            val handX = if (rightward) x + 2 else x
            val headX = if (rightward) dogX + 4 else dogX + 1
            val handY = top + 3
            val headY = floorY - 3
            val steps = abs(headX - handX)
            for (s in 1 until steps) {
                val lx = handX + s * (if (headX > handX) 1 else -1)
                val ly = handY + (headY - handY) * s / steps
                if (s % 2 == 1) cells += SceneCell(lx, ly, MID - 300)
            }
        }
        return cells
    }

    private fun shopper(phase: Int, widthCells: Int, floorY: Int, slot: Int, rightward: Boolean): List<SceneCell> {
        val pos = lane(phase, widthCells, slot + 7, ticksPerCell = 4, extent = 10) ?: return emptyList()
        val x = if (rightward) pos else widthCells - 1 - pos
        val stride = PlayScene.beat(phase, 4) % 2 == 0
        val body = if (slot % 2 == 0) (if (stride) WALKER_A else WALKER_B) else (if (stride) HATTED_A else HATTED_B)
        val cells = place(body, x, floorY - 7, MID - 150).toMutableList()
        val cart = if (rightward) CART else mirror(CART, 5)
        val cartX = if (rightward) x + 3 else x - 5
        cells += place(cart, cartX, floorY - 5, MID - 150)
        return cells
    }

    /** Ein Gast im Sitzen - Kopf, Oberkoerper, angewinkelte Beine. */
    private val SITTER = art(".#.", "###", "###", ".##", ".#.#")

    /**
     * Die Barista hinter der Theke (von der Theke halb verdeckt, sie arbeitet an der Maschine)
     * und ein Gast am Tischchen, der ab und zu die Tasse hebt.
     */
    private fun cafeRegulars(
        phase: Int,
        widthCells: Int,
        floorY: Int,
        placements: List<Placement>
    ): List<SceneCell> {
        val cells = mutableListOf<SceneCell>()
        placements.firstOrNull { it.prop === COUNTER_BAR }?.let { counter ->
            val ox = PlayScene.originX(counter, widthCells)
            // Geht an der Maschine hin und her.
            val step = PlayScene.beat(phase, 9) % 6
            val x = ox + 6 + (if (step < 3) step else 6 - step)
            val arm = if (PlayScene.beat(phase, 4) % 2 == 0) 0 else 1
            cells += place(WALKER_B, x, floorY - 14, MID - 150)
            cells += SceneCell(x - 1, floorY - 12 - arm, MID - 150)
        }
        placements.firstOrNull { it.prop === CAFE_TABLE }?.let { table ->
            val ox = PlayScene.originX(table, widthCells)
            val sip = PlayScene.beat(phase, 13) % 5 == 0
            cells += place(SITTER, ox + 5, floorY - 6, MID - 150)
            if (sip) cells += SceneCell(ox + 5, floorY - 6, MID - 50)
        }
        return cells
    }

    /** Autos und in der Stadt ein Bus - nachts mit Scheinwerfern. */
    private fun traffic(
        place: PlayScene.Place,
        phase: Int,
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase
    ): List<SceneCell> {
        val cells = mutableListOf<SceneCell>()
        val dark = isDark(dayPhase)
        val carPos = lane(phase, widthCells, slot = 10, ticksPerCell = 1, extent = CAR_WIDTH)
        if (carPos != null) {
            cells += place(CAR, carPos, floorY - 5, MID - 100)
            if (dark) {
                cells += SceneCell(carPos + CAR_WIDTH - 1, floorY - 3, PlayScene.GLOW, isLight = true)
                cells += SceneCell(carPos + CAR_WIDTH, floorY - 3, PlayScene.GLOW - 800, isLight = true)
                cells += SceneCell(carPos, floorY - 3, PlayScene.GLOW - 1300, isLight = true)
            }
        }
        if (place == PlayScene.Place.CITY) {
            val busPos = lane(phase, widthCells, slot = 11, ticksPerCell = 2, extent = BUS_WIDTH)
            if (busPos != null) {
                val x = widthCells - 1 - busPos - BUS_WIDTH + 1
                cells += place(BUS, x, floorY - 6, MID - 200)
                if (dark) {
                    // Innenbeleuchtung durch die Fenster.
                    for (wx in 0 until BUS_WIDTH) {
                        if (wx % 3 != 0 && wx < BUS_WIDTH - 2) {
                            cells += SceneCell(x + wx, floorY - 5, PlayScene.GLOW - 1300, isLight = true)
                        }
                    }
                    cells += SceneCell(x, floorY - 3, PlayScene.GLOW, isLight = true)
                }
            }
        }
        return cells
    }

    /** Zwei Kuehe, die gemaechlich ueber die Weide ziehen und den Kopf zum Grasen senken. */
    private fun cows(phase: Int, widthCells: Int, floorY: Int): List<SceneCell> {
        val cells = mutableListOf<SceneCell>()
        for (i in 0..1) {
            val span = widthCells + 20
            val x = (widthCells * (0.55f + i * 0.2f)).toInt() + (PlayScene.beat(phase, 25) + i * 13) % 9 - 4
            if (x > span) continue
            val grazing = (PlayScene.beat(phase, 11) + i) % 3 == 0
            val shape = if (grazing) {
                art("#.....##.", "#######..", "#######.#", "#.#..#.#.")
            } else {
                art("#......##", "#######.#", "#######..", "#.#..#.#.")
            }
            // Flecken: ausgesparte Zellen im Fell.
            val spots = setOf(2 to 1, 3 to 2, 5 to 1)
            cells += place(shape.filterNot { it in spots }, x, floorY - 12 + i * 2, FAR + 160)
        }
        return cells
    }

    // ---------------------------------------------------------------------------------------
    // Leben am Himmel und am Boden - zuletzt gezeichnet
    // ---------------------------------------------------------------------------------------

    /**
     * Was sich in der Luft und am Boden der neuen Orte regt: Adler, Papageien, Moewen, Krabben,
     * Gluehwuermchen, Blasen im Sumpf, die Fluegel der Windmuehle - und ueber der Stadt ein
     * Luftschiff, Drohnen und ein fliegendes Auto.
     */
    fun ambient(
        place: PlayScene.Place,
        phase: Int,
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase,
        placements: List<Placement>
    ): List<SceneCell> {
        if (widthCells <= 0 || floorY < 8) return emptyList()
        val night = isNight(dayPhase)
        val dark = isDark(dayPhase)
        val cells = mutableListOf<SceneCell>()
        when (place) {
            PlayScene.Place.MOUNTAINS -> if (!night) cells += eagle(phase, widthCells, floorY)
            PlayScene.Place.JUNGLE -> {
                if (!night) cells += parrot(phase, widthCells, floorY)
                if (dark) cells += fireflies(phase, widthCells, floorY, count = 6)
            }
            PlayScene.Place.SWAMP -> {
                cells += bubbles(phase, widthCells, floorY)
                cells += mist(phase, widthCells, floorY)
                if (dark) cells += fireflies(phase, widthCells, floorY, count = 8)
                else cells += frog(phase, widthCells, floorY)
            }
            PlayScene.Place.PLAINS -> {
                cells += windmillBlades(phase, widthCells, floorY, placements)
                if (!night) cells += airship(phase, widthCells, floorY, dayPhase)
            }
            PlayScene.Place.BEACH -> {
                if (!night) cells += gull(phase, widthCells, floorY)
                cells += crab(phase, widthCells, floorY)
            }
            // Die Grotte: Tropfen fallen von der Decke und glitzern, die Kristalle funkeln.
            PlayScene.Place.GROTTO -> {
                cells += drips(phase, widthCells, floorY)
                cells += fireflies(phase, widthCells, floorY, count = 4)
            }
            PlayScene.Place.CAMP -> {
                placements.firstOrNull { it.prop === CAMPFIRE }?.let { fire ->
                    cells += flames(phase, PlayScene.originX(fire, widthCells) + CAMPFIRE.width / 2, floorY, dark)
                }
                // Abends brennt im Zelt eine Laterne: Der Eingang leuchtet warm.
                if (dark) placements.firstOrNull { it.prop === TENT }?.let { tent ->
                    val ox = PlayScene.originX(tent, widthCells)
                    val oy = PlayScene.originY(tent, floorY)
                    for (row in 4..7) for (col in 7 - (row - 3)..7 + (row - 3)) {
                        if ((col to row) !in TENT.art) {
                            cells += SceneCell(ox + col, oy + row, PlayScene.GLOW - 1100 - (7 - row) * 100, isLight = true)
                        }
                    }
                }
                if (dark) cells += fireflies(phase, widthCells, floorY, count = 3)
            }
            PlayScene.Place.CITY -> {
                cells += airship(phase, widthCells, floorY, dayPhase)
                cells += drone(phase, widthCells, floorY, dayPhase, slot = 0)
                cells += drone(phase, widthCells, floorY, dayPhase, slot = 1)
                cells += flyingCar(phase, widthCells, floorY, dayPhase)
            }
            PlayScene.Place.STREET -> cells += drone(phase, widthCells, floorY, dayPhase, slot = 2)
            // Dampf ueber der Espressomaschine.
            PlayScene.Place.CAFE -> placements.firstOrNull { it.prop === COUNTER_BAR }?.let { counter ->
                val sx = PlayScene.originX(counter, widthCells) + CAFE_STEAM_AT.first
                val sy = PlayScene.originY(counter, floorY) + CAFE_STEAM_AT.second
                val rise = PlayScene.beat(phase, 3) % 4
                cells += SceneCell(sx + rise % 2, sy - 1 - rise, PlayScene.GLOW - 900 - rise * 250, isLight = true)
                cells += SceneCell(sx + 1 - rise % 2, sy - 2 - rise, PlayScene.GLOW - 1300 - rise * 200, isLight = true)
            }
            else -> Unit
        }
        return cells.filter { it.x in 0 until widthCells && it.y >= 0 }
    }

    private fun eagle(phase: Int, widthCells: Int, floorY: Int): List<SceneCell> {
        // Kreist langsam: waagerecht hin und her, dabei leicht auf und ab.
        val t = PlayScene.beat(phase, 2)
        val span = (widthCells - 10).coerceAtLeast(1)
        val swing = t % (span * 2)
        val x = if (swing < span) swing else span * 2 - swing
        val y = floorY - skyRoom(floorY, 34) - 4 + (t / 5) % 3
        if (y < 1) return emptyList()
        val up = t % 4 < 2
        val shape = if (up) art("#.....#", ".#...#.", "..###..") else art(".......", "###.###", "..###..")
        return place(shape, x, y, PlayScene.FURNITURE - 200)
    }

    private fun parrot(phase: Int, widthCells: Int, floorY: Int): List<SceneCell> {
        val pos = lane(phase, widthCells, slot = 20, ticksPerCell = 1, extent = 5) ?: return emptyList()
        val y = floorY - 20 + (pos / 4) % 3
        if (y < 1) return emptyList()
        val flap = PlayScene.beat(phase, 1) % 2 == 0
        val shape = if (flap) art(".#..", "####", "#.#.") else art("....", "####", "##..")
        // Der Schwanz ist lang und leuchtend - ein heller Punkt hinten.
        return place(shape, pos, y, PlayScene.FURNITURE) + SceneCell(pos - 1, y + 2, PlayScene.GLOW - 700)
    }

    private fun gull(phase: Int, widthCells: Int, floorY: Int): List<SceneCell> {
        val pos = lane(phase, widthCells, slot = 21, ticksPerCell = 2, extent = 5) ?: return emptyList()
        val x = widthCells - 1 - pos
        val y = floorY - 22 + (pos / 6) % 2
        if (y < 1) return emptyList()
        val up = PlayScene.beat(phase, 2) % 2 == 0
        val shape = if (up) art("#...#", ".#.#.", "..#..") else art(".....", "##.##", "..#..")
        return place(shape, x, y, PlayScene.FURNITURE - 100)
    }

    private fun crab(phase: Int, widthCells: Int, floorY: Int): List<SceneCell> {
        // Seitwaerts, wie sich das gehoert - hin und her ueber den Sand.
        val span = (widthCells / 3).coerceAtLeast(4)
        val t = PlayScene.beat(phase, 2) % (span * 2)
        val x = (widthCells * 0.62f).toInt() + if (t < span) t else span * 2 - t
        val claws = PlayScene.beat(phase, 2) % 2 == 0
        val shape = if (claws) art("#...#", ".###.", "#.#.#") else art(".#.#.", "#####", ".#.#.")
        return place(shape, x, floorY - 3, PlayScene.FURNITURE - 200)
    }

    private fun fireflies(phase: Int, widthCells: Int, floorY: Int, count: Int): List<SceneCell> =
        (0 until count).mapNotNull { i ->
            val on = (PlayScene.beat(phase, 3) + i * 5) % 7 < 3
            if (!on) return@mapNotNull null
            val baseX = hash(i, 41) % widthCells.coerceAtLeast(1)
            val baseY = floorY - 3 - hash(i, 42) % 12
            val wobble = (PlayScene.beat(phase, 4) + i) % 3 - 1
            SceneCell(baseX + wobble, baseY + (i + PlayScene.beat(phase, 6)) % 2, PlayScene.GLOW - 400, isLight = true)
        }

    /** Tropfen in der Grotte: fallen in eigenem Takt von der Decke bis zum Boden. */
    private fun drips(phase: Int, widthCells: Int, floorY: Int): List<SceneCell> =
        (0 until 3).mapNotNull { i ->
            val x = (widthCells * (0.22f + i * 0.27f)).toInt()
            val fall = (PlayScene.beat(phase, 1) + i * 7) % 20
            val y = floorY - 16 + fall
            if (fall > 15) null else SceneCell(x, y, PlayScene.GLOW - 1300, isLight = true)
        }

    private fun bubbles(phase: Int, widthCells: Int, floorY: Int): List<SceneCell> =
        (0 until 4).mapNotNull { i ->
            val x = (widthCells * (0.15f + i * 0.23f)).toInt()
            val rise = (PlayScene.beat(phase, 2) + i * 3) % 7
            if (rise > 3) null else SceneCell(x + (rise % 2), floorY - 1 - rise, PlayScene.STRUCTURE * 2)
        }

    private fun mist(phase: Int, widthCells: Int, floorY: Int): List<SceneCell> {
        val drift = PlayScene.beat(phase, 6)
        return (0 until widthCells).mapNotNull { x ->
            if ((x + drift) % 5 < 3) SceneCell(x, floorY - 5 - ((x + drift) / 11) % 2, FAR - 120) else null
        }
    }

    private fun frog(phase: Int, widthCells: Int, floorY: Int): List<SceneCell> {
        val t = PlayScene.beat(phase, 4) % 12
        val x = (widthCells * 0.70f).toInt() + t / 2
        val hop = if (t % 4 == 1) -2 else 0
        return place(art("#.#", "###", "#.#"), x, floorY - 3 + hop, PlayScene.FURNITURE - 300)
    }

    /** Die vier Fluegel der Windmuehle, im Wechsel als Plus und als Kreuz. */
    private fun windmillBlades(
        phase: Int,
        widthCells: Int,
        floorY: Int,
        placements: List<Placement>
    ): List<SceneCell> {
        val mill = placements.firstOrNull { it.prop === WINDMILL } ?: return emptyList()
        val hx = PlayScene.originX(mill, widthCells) + WINDMILL_HUB.first
        val hy = PlayScene.originY(mill, floorY) + WINDMILL_HUB.second
        val straight = PlayScene.beat(phase, 4) % 2 == 0
        val cells = mutableListOf(SceneCell(hx, hy, PlayScene.FURNITURE))
        for (d in 1..5) {
            val b = PlayScene.FURNITURE - 200
            if (straight) {
                cells += SceneCell(hx + d, hy, b); cells += SceneCell(hx - d, hy, b)
                cells += SceneCell(hx, hy + d, b); cells += SceneCell(hx, hy - d, b)
            } else {
                cells += SceneCell(hx + d, hy + d, b); cells += SceneCell(hx - d, hy - d, b)
                cells += SceneCell(hx + d, hy - d, b); cells += SceneCell(hx - d, hy + d, b)
            }
        }
        return cells
    }

    /** Ein Luftschiff zieht gemaechlich hoch ueber den Himmel. */
    private fun airship(
        phase: Int,
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase
    ): List<SceneCell> {
        val pos = lane(phase, widthCells, slot = 30, ticksPerCell = 6, extent = 16) ?: return emptyList()
        // Nur wo wirklich Himmel UEBER allem anderen ist - auf flachen Bildern bliebe es sonst
        // vor den Hausfassaden haengen.
        val y = floorY - skyRoom(floorY, 34) - 10
        if (y < 1) return emptyList()
        val shape = art(
            "#..##########...",
            "##############..",
            "################",
            "##############..",
            "#..##########...",
            "......####......"
        )
        val cells = place(shape, pos, y, NEAR + 200).toMutableList()
        if (isDark(dayPhase)) {
            // Positionslichter und erleuchtete Gondel.
            cells += SceneCell(pos + 15, y + 2, PlayScene.GLOW - 300, isLight = true)
            cells += SceneCell(pos + 7, y + 5, PlayScene.GLOW - 900, isLight = true)
            cells += SceneCell(pos + 8, y + 5, PlayScene.GLOW - 900, isLight = true)
        }
        return cells
    }

    /** Eine kleine Drohne, die ueber den Daechern schwebt - nachts mit blinkendem Licht. */
    private fun drone(
        phase: Int,
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase,
        slot: Int
    ): List<SceneCell> {
        val pos = lane(phase, widthCells, slot = 40 + slot, ticksPerCell = 2, extent = 5) ?: return emptyList()
        val x = if (slot % 2 == 0) pos else widthCells - 1 - pos
        val bob = (PlayScene.beat(phase, 3) + slot) % 4
        val y = floorY - 24 - slot * 5 + (if (bob == 3) 1 else 0)
        if (y < 1) return emptyList()
        val spin = PlayScene.beat(phase, 1) % 2 == 0
        val shape = if (spin) art("#.#.#", ".###.", "..#..") else art(".#.#.", ".###.", "..#..")
        val cells = place(shape, x, y, PlayScene.FURNITURE - 300).toMutableList()
        if (isDark(dayPhase) && PlayScene.beat(phase, 3) % 2 == 0) {
            cells += SceneCell(x + 2, y + 3, PlayScene.GLOW, isLight = true)
        }
        return cells
    }

    /** Ein fliegendes Auto rauscht hoch ueber der Stadt vorbei - nachts mit Lichtspur. */
    private fun flyingCar(
        phase: Int,
        widthCells: Int,
        floorY: Int,
        dayPhase: PlayAmbientActivity.DayPhase
    ): List<SceneCell> {
        val pos = lane(phase, widthCells, slot = 50, ticksPerCell = 1, extent = 8) ?: return emptyList()
        val x = widthCells - 1 - pos
        val y = floorY - 30
        if (y < 1) return emptyList()
        val cells = place(art(".####...", "########", ".#....#."), x, y, PlayScene.FURNITURE - 250).toMutableList()
        if (isDark(dayPhase)) {
            for (t in 1..4) cells += SceneCell(x + 7 + t, y + 1, PlayScene.GLOW - 300 * t, isLight = true)
            cells += SceneCell(x, y + 1, PlayScene.GLOW, isLight = true)
        }
        return cells
    }
}
