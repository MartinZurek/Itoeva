package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayScene.Placement
import com.notime.glyphsim.matrix.PlayScene.Prop
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

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

    /** Die fuenf neuen Landschaften - alle unter freiem Himmel. */
    val NATURE: Set<PlayScene.Place> = setOf(
        PlayScene.Place.JUNGLE,
        PlayScene.Place.MOUNTAINS,
        PlayScene.Place.SWAMP,
        PlayScene.Place.PLAINS,
        PlayScene.Place.BEACH
    )

    /** Wo die Figur in Ruhe steht - immer vorn links, wo nichts ihren Platz belegt. */
    fun avatarAnchorX(place: PlayScene.Place): Float? = when (place) {
        PlayScene.Place.JUNGLE -> 0.06f
        PlayScene.Place.MOUNTAINS -> 0.08f
        PlayScene.Place.SWAMP -> 0.06f
        PlayScene.Place.PLAINS -> 0.10f
        PlayScene.Place.BEACH -> 0.14f
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
        // STRAND: Palme und Sonnenschirm hinten, davor der Liegestuhl, eine Sandburg.
        PlayScene.Place.BEACH -> listOf(
            Placement(PALM, anchorX = 0f, brightness = PlayScene.BACKDROP, behind = true),
            Placement(PARASOL, anchorX = 0.56f, brightness = PlayScene.BACKDROP, behind = true),
            Placement(DECKCHAIR, anchorX = 0.54f, station = PlayScene.Station.BENCH),
            Placement(SANDCASTLE, anchorX = 0.90f)
        )
        else -> null
    }

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
        foreground: List<Pair<IntRange, Int>> = emptyList()
    ): List<SceneCell> = when (place) {
        PlayScene.Place.MOUNTAINS -> mountainRange(widthCells, floorY)
        PlayScene.Place.JUNGLE -> jungleCanopy(phase, widthCells, floorY)
        PlayScene.Place.SWAMP -> swampBackdrop(widthCells, floorY)
        PlayScene.Place.PLAINS -> rollingHills(widthCells, floorY)
        PlayScene.Place.BEACH -> sea(phase, widthCells, floorY, dayPhase)
        PlayScene.Place.CITY -> skyline(phase, widthCells, floorY, dayPhase)
        PlayScene.Place.SHOP -> shopBackWall(phase, widthCells, floorY, foreground)
        else -> emptyList()
    }

    /**
     * Welche Himmelszellen der Hintergrund verdeckt - Sterne und Wolken ziehen HINTER Bergen,
     * Blaetterdach und Hochhaeusern vorbei, nicht davor (dieselbe Regel wie bei den Fassaden,
     * siehe PlayScene.facadeMask).
     */
    fun skyMask(place: PlayScene.Place, widthCells: Int, floorY: Int): Set<Pair<Int, Int>> = when (place) {
        PlayScene.Place.MOUNTAINS, PlayScene.Place.JUNGLE, PlayScene.Place.CITY ->
            background(place, 0, widthCells, floorY, PlayAmbientActivity.DayPhase.MIDDAY)
                .mapTo(HashSet()) { it.x to it.y }
        else -> emptySet()
    }

    /**
     * Zwei Bergketten hintereinander: die ferne hoeher und blasser, die nahe niedriger und
     * kraeftiger. Auf den hoechsten Gipfeln liegt Schnee. Aus der Bildbreite gerechnet, damit
     * es im Hoch- und Querformat dieselben Berge sind.
     */
    private fun mountainRange(widthCells: Int, floorY: Int): List<SceneCell> {
        val maxH = skyRoom(floorY, 34)
        if (maxH < 4 || widthCells <= 0) return emptyList()
        fun ridge(peaks: List<Triple<Float, Float, Float>>, x: Int): Int = peaks.maxOf { (at, height, slope) ->
            (maxH * height - abs(x - widthCells * at) * slope).roundToInt()
        }.coerceAtLeast(0)
        val far = listOf(
            Triple(0.10f, 0.78f, 1.1f), Triple(0.36f, 1.00f, 1.25f),
            Triple(0.62f, 0.84f, 1.0f), Triple(0.90f, 0.95f, 1.2f)
        )
        val near = listOf(
            Triple(0.22f, 0.52f, 0.9f), Triple(0.52f, 0.44f, 0.8f), Triple(0.80f, 0.58f, 1.0f)
        )
        val cells = mutableListOf<SceneCell>()
        for (x in 0 until widthCells) {
            val hNear = ridge(near, x)
            val hFar = ridge(far, x)
            val topNear = floorY - 1 - hNear
            val topFar = floorY - 1 - hFar
            // Ferne Kette nur, wo sie ueber die nahe hinausragt.
            if (hFar > hNear) {
                for (y in topFar until topNear) {
                    val depth = y - topFar
                    val b = when {
                        depth == 0 -> FAR + 120
                        // Schnee auf den hohen Gipfeln - die obersten Zeilen hell.
                        hFar > maxH * 0.72f && depth <= 2 -> PlayScene.FURNITURE - 300
                        (x + y) % 3 == 0 -> FAR - 80
                        else -> 0
                    }
                    if (b > 0) cells += SceneCell(x, y, b)
                }
            }
            for (y in topNear until floorY - 1) {
                val depth = y - topNear
                val b = when {
                    depth == 0 -> NEAR + 120
                    hNear > maxH * 0.5f && depth <= 1 -> PlayScene.FURNITURE - 400
                    (x + 2 * y) % 4 == 0 -> NEAR - 120
                    (x + y) % 5 == 0 -> FAR - 100
                    else -> 0
                }
                if (b > 0) cells += SceneCell(x, y, b)
            }
        }
        return cells
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

    /** Der Sumpf: ferne, kahle Baeume ueber dunklem Wasser. */
    private fun swampBackdrop(widthCells: Int, floorY: Int): List<SceneCell> {
        if (widthCells <= 0 || floorY < 12) return emptyList()
        val cells = mutableListOf<SceneCell>()
        var i = 0
        var x = 4
        while (x < widthCells) {
            val height = 9 + hash(i, 21) % 8
            val baseY = floorY - 1
            for (d in 0 until height) cells += SceneCell(x, baseY - d, FAR)
            // Zwei kahle Aeste je Baum.
            val branchY = baseY - height + 2 + hash(i, 22) % 3
            cells += SceneCell(x - 1, branchY, FAR)
            cells += SceneCell(x - 2, branchY - 1, FAR)
            cells += SceneCell(x + 1, branchY + 2, FAR)
            cells += SceneCell(x + 2, branchY + 1, FAR)
            x += 9 + hash(i, 23) % 6
            i++
        }
        // Wasserlinie hinten - eine unterbrochene, blasse Spiegelung.
        for (wx in 0 until widthCells) {
            if (wx % 3 != 1) cells += SceneCell(wx, floorY - 1, FAR - 60)
        }
        return cells.filter { it.x in 0 until widthCells && it.y >= 0 }
    }

    /** Weite, sanfte Huegel und ein fernes Gehoeft. */
    private fun rollingHills(widthCells: Int, floorY: Int): List<SceneCell> {
        if (widthCells <= 0 || floorY < 10) return emptyList()
        val cells = mutableListOf<SceneCell>()
        val amp = skyRoom(floorY, 8)
        val heights = IntArray(widthCells) { x ->
            (3 + amp * 0.5f * (1 + sin(x * 0.13)) + amp * 0.3f * sin(x * 0.051 + 1.3)).roundToInt()
        }
        for (x in 0 until widthCells) {
            val top = floorY - 1 - heights[x]
            cells += SceneCell(x, top, NEAR)
            for (y in top + 1 until floorY - 1) if ((x + y) % 4 == 0) cells += SceneCell(x, y, FAR - 60)
        }
        // Fernes Gehoeft auf dem Huegel.
        val fx = (widthCells * 0.32f).toInt()
        if (fx + 6 < widthCells) {
            val ground = floorY - 1 - heights[fx + 3]
            cells += place(art(
                "...#...",
                "..###..",
                ".#####.",
                ".#.#.#.",
                ".#####."
            ), fx, ground - 5, FAR + 80)
        }
        return cells
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
        dayPhase: PlayAmbientActivity.DayPhase
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
            PlayScene.Place.CITY -> {
                cells += airship(phase, widthCells, floorY, dayPhase)
                cells += drone(phase, widthCells, floorY, dayPhase, slot = 0)
                cells += drone(phase, widthCells, floorY, dayPhase, slot = 1)
                cells += flyingCar(phase, widthCells, floorY, dayPhase)
            }
            PlayScene.Place.STREET -> cells += drone(phase, widthCells, floorY, dayPhase, slot = 2)
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
