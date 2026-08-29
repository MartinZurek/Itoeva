package com.notime.glyphsim.matrix

import com.notime.glyphsim.ui.PlayPath
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * **Die Fehler, die beim Aufbau dieser Welt immer wieder passiert sind - hier festgenagelt.**
 *
 * Alle bisherigen Gestaltungsfehler waren vom selben Typ und keiner davon war eine Zahl, die
 * irgendwo falsch stand: Die Figur stand IM Teleskop. Sie verdeckte den ganzen Kuehlschrank. Sie
 * stand im Lichtkegel des Leuchtturms und loeschte ihn aus. Sie stand mitten in der Parkbank. Die
 * Kueche war so voll, dass zwischen den Moebeln kein Platz zum Stehen blieb. Gefunden wurde das
 * jedes Mal erst, indem die Szene von Hand als Zeichenraster ausgegeben und angesehen wurde.
 *
 * Diese Pruefungen machen daraus etwas Automatisches. Sie sind bewusst grosszuegig eingestellt:
 * Eine Figur DARF vor einem Moebelstueck stehen - das liest sich als Tiefe. Sie darf es nur nicht
 * zum Verschwinden bringen.
 *
 * Schlaegt hier etwas an, steht in der Fehlermeldung das gerenderte Bild (siehe [ScenePreview]) -
 * man sieht also sofort, was gemeint ist, statt Koordinaten deuten zu muessen.
 */
class SceneCompositionTest {

    private val width = ScenePreview.WIDTH
    private val floorY = ScenePreview.FLOOR_Y

    /**
     * **Immer bei klarem Wetter pruefen - sonst haengt das Ergebnis am Kalender.**
     *
     * Aufgefallen an einem Regentag: Die Pruefung "jede Kreatur hat ihre eigene Landschaft" schlug
     * an, ohne dass sich eine Zeile geaendert hatte. Das Wetter wird aus dem Datum gerechnet (siehe
     * PlayWeather.forDate), an knapp jedem fuenften Tag faellt Regen - und Regen faellt ueber alle
     * sechs Landschaften gleich. Er machte damit aus zwei verschiedenen Gegenden zwei zu zwei
     * Dritteln gleiche Bilder, und die Pruefung mass etwas, das sie gar nicht meint.
     *
     * Ein Test, der an einem Dienstag durchlaeuft und am Mittwoch nicht, ist schlimmer als kein
     * Test - man gewoehnt sich an, ihn zu wiederholen.
     */
    @Before
    fun clearSky() {
        PlayWeather.forceForPreview(PlayWeather.CLEAR)
    }

    @After
    fun restoreSky() {
        PlayWeather.forceForPreview(null)
    }

    /** Die Zellen, die der Koerper der Figur an einem Platz tatsaechlich belegt. */
    private fun avatarCellsAt(
        place: PlayScene.Place,
        station: PlayScene.Station,
        species: AvatarSpecies
    ): Set<Pair<Int, Int>> {
        val spot = PlayScene.stationSpot(place, station, width, floorY, species) ?: return emptySet()
        val frame = AvatarAnimations.idlePose(species)
        val originX = spot.centerX - AvatarGeometry.SIZE / 2
        val originY = spot.groundY - AvatarBodies.forSpecies(species).groundRow()
        return buildSet {
            for (y in 0 until AvatarGeometry.HEIGHT) {
                for (x in 0 until AvatarGeometry.SIZE) {
                    if (frame[y * AvatarGeometry.SIZE + x] > 0) add((originX + x) to (originY + y))
                }
            }
        }
    }

    @Test
    fun `die Figur verdeckt nicht die Requisite, die sie benutzt`() {
        for (species in AvatarSpecies.entries) {
            for (place in PlayScene.Place.entries) {
                for (station in PlayScene.stationsAt(place, species)) {
                    if (station == PlayScene.Station.DOOR) continue
                    // Bett, Sessel, Wanne: Dort SOLL sie drinstecken, das loest die vordere
                    // Ebene auf (siehe PlayScene.buildFront).
                    if (PlayScene.isOccupiable(place, station, species)) continue

                    val prop = PlayScene.propCellsAt(place, station, width, floorY, species).toSet()
                    if (prop.isEmpty()) continue
                    val covered = prop.count { it in avatarCellsAt(place, station, species) }
                    val share = covered.toFloat() / prop.size

                    assertTrue(
                        "Die Figur verdeckt $station an $place ($species) zu " +
                            "${(share * 100).toInt()} % - das Moebelstueck ist dann nicht mehr zu " +
                            "erkennen, und man sieht nicht, womit sie umgeht.\n\n" +
                            ScenePreview.render(place, species, station = station),
                        share <= MAX_COVERED
                    )
                }
            }
        }
    }

    @Test
    fun `keine zwei Requisiten stehen ineinander`() {
        // Ueber ALLE Requisiten, nicht nur die mit Station und ausdruecklich EINSCHLIESSLICH der
        // Tuer: Die vorherige Fassung liess beides aus und uebersah dadurch, dass in jedem
        // Arbeitsraum die Lampe im Tuerrahmen stand.
        //
        // Auch ueber mehrere Breiten, denn die Verankerung ist ein Bruchteil der Szenenbreite -
        // was auf einem breiten Bild nebeneinander steht, kann auf einem schmalen ineinander
        // rutschen. Genau diese Abhaengigkeit macht die Anordnung mit blossem Auge unpruefbar.
        // Ab der garantierten Mindestbreite aufwaerts - schmaler wird die Szene nicht gezeichnet,
        // dafuer sorgt die Zellgroesse in DockScreen.
        for (sceneWidth in intArrayOf(PlayScene.MIN_SCENE_CELLS, 46, width, 72, 96)) {
            for (species in AvatarSpecies.entries) {
                for (place in PlayScene.Place.entries) {
                    val props = PlayScene.propFootprints(place, sceneWidth, floorY, species)
                        .filter { it.second.isNotEmpty() }
                        .map { it.first to it.second.toSet() }

                    for (i in props.indices) {
                        for (j in (i + 1) until props.size) {
                            val (aName, a) = props[i]
                            val (bName, b) = props[j]
                            val shared = a.count { it in b }
                            assertTrue(
                                "$aName und $bName stehen an $place ($species) ineinander " +
                                    "($shared gemeinsame Zellen, Breite $sceneWidth).\n\n" +
                                    ScenePreview.render(place, species, width = sceneWidth),
                                shared == 0
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `kleines Beiwerk macht Strasse Stadt und Wiese sichtbar voller`() {
        // Hintergrundfassaden und -baeume werden von propFootprints bewusst ausgeblendet. Damit
        // zaehlt diese Pruefung genau den Vordergrund: benutzbare Stationen plus das neue kleine
        // Beiwerk. Auf einem normalen Hochformat darf fitting davon nichts still verwerfen.
        val expectedForegroundProps = mapOf(
            PlayScene.Place.STREET to 3, // Briefkasten, Bank, Laterne
            PlayScene.Place.CITY to 5,   // Briefkasten, Abfallkorb, Bank, zwei Laternen
            PlayScene.Place.MEADOW to 4  // Zaun, Bank, Wildwuchs, Pilze
        )

        for ((place, expected) in expectedForegroundProps) {
            val visible = PlayScene.propFootprints(place, width, floorY)
                .count { it.second.isNotEmpty() }
            assertTrue(
                "$place zeigt nur $visible von $expected Vordergrund-Requisiten.\n\n" +
                    ScenePreview.render(place, AvatarSpecies.PUFFLING),
                visible == expected
            )
        }
    }

    @Test
    fun `der Laden besteht aus zwei klar getrennten Funktionsbereichen`() {
        // Eine Auslage und eine Kasse. Schon das zusaetzliche Wandregal verband beide auf dem
        // schmalen Raster wieder zu einem dichten Moebelblock.
        for (sceneWidth in intArrayOf(PlayScene.MIN_SCENE_CELLS, 46, width, 72)) {
            val visible = PlayScene.propFootprints(
                PlayScene.Place.SHOP, sceneWidth, floorY
            ).filter { it.second.isNotEmpty() }
            // Hinzu kommt die Tuer, die propFootprints absichtlich ebenfalls mitzaehlt.
            assertTrue(
                "Der Laden hat bei Breite $sceneWidth ${visible.size} statt drei sichtbare " +
                    "Bereiche.\n\n" + ScenePreview.render(
                        PlayScene.Place.SHOP, width = sceneWidth, showAvatar = false
                    ),
                visible.size == 3
            )
        }
    }

    @Test
    fun `jedes erworbene Stueck ist auch tatsaechlich zu sehen`() {
        // **Der Fehler, der nicht weh tut und deshalb der schlimmste ist.** Ein Beiwerk, das mit
        // etwas anderem kollidiert, wird lautlos weggelassen (siehe PlayScene.fitting) - kein
        // Absturz, keine Warnung, das Zimmer sieht nur genauso aus wie vorher. Der Nutzer haette
        // wochenlang auf eine Stufe hingearbeitet und bekaeme nichts zu sehen.
        //
        // Genau das ist beim ersten Versuch passiert: Die Erwerbungen rechneten ihren Anker gegen
        // die volle Breite, die Moebel daneben gegen die Breite ohne Tuerzone - dieselbe Zahl
        // bedeutete zwei verschiedene Stellen, und die Apparatur landete im Fernseher.
        for (species in AvatarSpecies.entries) {
            for (acquisition in PlayScene.Acquisition.entries) {
                val without = PlayScene.build(
                    acquisition.place, 0, width, floorY,
                    PlayAmbientActivity.DayPhase.MIDDAY, species = species
                ).map { it.x to it.y }.toSet()
                val with = PlayScene.build(
                    acquisition.place, 0, width, floorY,
                    PlayAmbientActivity.DayPhase.MIDDAY, species = species,
                    acquisitions = setOf(acquisition)
                ).map { it.x to it.y }.toSet()

                assertTrue(
                    "$acquisition ist bei $species an ${acquisition.place} nicht zu sehen - " +
                        "es faellt weg, weil es mit der Einrichtung kollidiert.\n\n" +
                        ScenePreview.render(
                            acquisition.place, species, showAvatar = false,
                            acquisitions = setOf(acquisition)
                        ),
                    (with - without).size >= MIN_VISIBLE_CELLS
                )
            }
        }
    }

    /** So viele Zellen muss ein erworbenes Stueck mindestens beisteuern, um zu zaehlen. */
    private val MIN_VISIBLE_CELLS = 8

    @Test
    fun `auch das Angesammelte steht nicht in den Moebeln`() {
        // **Der Fall, den es vorher nicht gab.** Was sich im Lauf der Entwicklung ansammelt
        // (Rucksack, Werkzeugkiste, Haustier), kommt zu einer bereits fertig eingerichteten
        // Wohnung DAZU - und zwar bei jeder der sechs Kreaturen, deren Zimmer verschieden voll
        // sind. Ein Rucksack, der im Sofa steckt, faellt beim Bauen niemandem auf: Man sieht ihn
        // erst, wenn man Wochen spaeter die Stufe erreicht.
        //
        // Geprueft wird mit dem VOLLEN Satz je Pfad, also dem Zustand nach Stufe drei.
        for (sceneWidth in intArrayOf(PlayScene.MIN_SCENE_CELLS, 46, width, 72)) {
            for (species in AvatarSpecies.entries) {
                for (path in PlayPath.entries) {
                    val owned = PlayPath.acquisitionsUpTo(path, 3)
                    for (place in owned.map { it.place }.distinct()) {
                        val props = PlayScene.propFootprints(place, sceneWidth, floorY, species, owned)
                            .filter { it.second.isNotEmpty() }
                            .map { it.first to it.second.toSet() }

                        for (i in props.indices) {
                            for (j in (i + 1) until props.size) {
                                val (aName, a) = props[i]
                                val (bName, b) = props[j]
                                val shared = a.count { it in b }
                                assertTrue(
                                    "$aName und $bName stehen an $place ($species, $path) " +
                                        "ineinander ($shared Zellen, Breite $sceneWidth).\n\n" +
                                        ScenePreview.render(place, species, width = sceneWidth),
                                    shared == 0
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `das Angesammelte setzt sauber auf dem Boden auf`() {
        // Dieselbe Regel wie fuer alle Requisiten (siehe PlaySceneTest): Was auf dem Boden steht,
        // endet genau eine Zelle darueber. Ein Rucksack, der einen Pixel ueber dem Boden schwebt,
        // ist der Fehler, den man erst am Geraet sieht - und dann nur, wenn man genau hinsieht.
        for (path in PlayPath.entries) {
            val owned = PlayPath.acquisitionsUpTo(path, 3)
            for (place in owned.map { it.place }.distinct()) {
                val cells = PlayScene.build(
                    place, 0, width, floorY, PlayAmbientActivity.DayPhase.MIDDAY,
                    species = AvatarSpecies.PUFFLING, acquisitions = owned
                ).filter { it.y < floorY }
                assertTrue("$place zeichnet nichts", cells.isNotEmpty())
                assertTrue(
                    "$place ($path) zeichnet unterhalb des Bodens",
                    cells.maxOf { it.y } == floorY - 1
                )
            }
        }
    }

    @Test
    fun `an jedem Ort bleibt Platz zum Stehen`() {
        // Die Kueche war einmal so voll gestellt, dass die Figur unweigerlich in einem Moebel
        // stand. Geprueft wird deshalb, dass ihr Ruheplatz frei ist.
        for (species in AvatarSpecies.entries) {
            for (place in PlayScene.Place.entries) {
                val occupied = PlayScene.stationsAt(place, species)
                    .filter { it != PlayScene.Station.DOOR }
                    .flatMap { PlayScene.propCellsAt(place, station = it, widthCells = width, floorY = floorY, species = species) }
                    .toSet()

                val originX = ((width - AvatarGeometry.SIZE) * PlayScene.avatarAnchorX(place)).toInt()
                val originY = (floorY - 1) - AvatarBodies.forSpecies(species).groundRow()
                val frame = AvatarAnimations.idlePose(species)
                var covered = 0
                var total = 0
                for (y in 0 until AvatarGeometry.HEIGHT) {
                    for (x in 0 until AvatarGeometry.SIZE) {
                        if (frame[y * AvatarGeometry.SIZE + x] <= 0) continue
                        total++
                        if ((originX + x) to (originY + y) in occupied) covered++
                    }
                }
                if (total == 0) continue
                val share = covered.toFloat() / total
                assertTrue(
                    "Am Ruheplatz von $place ($species) steckt die Figur zu " +
                        "${(share * 100).toInt()} % in Moebeln.\n\n" +
                        ScenePreview.render(place, species),
                    share <= MAX_AVATAR_IN_FURNITURE
                )
            }
        }
    }

    @Test
    fun `niemand ragt durch die Zimmerdecke`() {
        // Ein Fehler, den sonst NICHTS bemerkt: Die Kulisse ist korrekt, die Aufsetzstelle
        // gueltig, die Figur steht sauber darauf - und trotzdem steckt ihr Kopf im Stockwerk
        // darueber. Aufgefallen ist das bei HOOTLETs Sitzstange, die auf halber Zimmerhoehe
        // haengen sollte: Die Figur reicht STEHEND schon fast bis zur Decke, jede Erhoehung
        // schiebt sie hindurch.
        for (species in AvatarSpecies.entries) {
            for (place in PlayScene.Place.entries) {
                val ceiling = PlayScene.ceilingY(place, floorY) ?: continue
                for (station in PlayScene.stationsAt(place, species)) {
                    if (station == PlayScene.Station.DOOR) continue
                    val cells = avatarCellsAt(place, station, species)
                    if (cells.isEmpty()) continue
                    val highest = cells.minOf { it.second }
                    assertTrue(
                        "An $station in $place ($species) ragt die Figur bis Zeile $highest - " +
                            "die Decke liegt bei $ceiling.\n\n" +
                            ScenePreview.render(place, species, station = station),
                        highest > ceiling
                    )
                }
            }
        }
    }

    /**
     * Bis hierhin darf eine Requisite verdeckt sein: Ein Drittel liest sich als "die Figur steht
     * davor", darueber verschwindet der Gegenstand.
     */
    private val MAX_COVERED = 0.34f

    /** Und so viel von der Figur darf im Ruhezustand in Moebeln stecken. */
    private val MAX_AVATAR_IN_FURNITURE = 0.30f

    @Test
    fun `auf breiten Bildern bleibt die Einrichtung beieinander`() {
        // **Gemeldet als "im Querformat steht alles sehr weit auseinander".** Die Verankerung ist
        // ein Bruchteil der Breite; quer gedreht verdoppelt sich die Strecke, und zwischen Bett
        // und Nachttisch klafft ein halber Bildschirm. Kein einzelner Wert ist dabei falsch - es
        // ist die Vorschrift, die bei ungewohnten Massen auseinanderlaeuft, und genau so etwas
        // faellt beim Lesen des Codes nie auf.
        //
        // Geprueft wird die Folge, nicht die Ursache: Wie weit liegen linkeste und rechteste
        // Requisite auseinander? Ueber die Zimmerbreite hinaus darf das nicht gehen.
        for (sceneWidth in intArrayOf(72, 84, 96, 120)) {
            for (species in AvatarSpecies.entries) {
                for (place in PlayScene.Place.entries) {
                    val cells = PlayScene.propFootprints(place, sceneWidth, floorY, species)
                        .flatMap { it.second }
                    if (cells.isEmpty()) continue
                    val extent = cells.maxOf { it.first } - cells.minOf { it.first } + 1
                    assertTrue(
                        "$place ($species) zieht sich bei Breite $sceneWidth ueber $extent " +
                            "Spalten - ein Zimmer ist hoechstens ${PlayScene.MAX_ROOM_CELLS} " +
                            "breit." + System.lineSeparator() +
                            ScenePreview.render(place, species, width = sceneWidth),
                        extent <= PlayScene.MAX_ROOM_CELLS
                    )
                }
            }
        }
    }

    @Test
    fun `das Zimmer steht mittig, nicht am Rand`() {
        // An der ABBILDUNG geprueft, nicht an den Requisiten: Der erste Versuch mass den Abstand
        // der aeussersten Moebel und schlug bei der Strasse an - dort sind die Hausfassaden
        // Hintergrund und aus der Pruefung ausgenommen (siehe Placement.behind), uebrig blieben
        // Bank und Laterne, und die stehen naturgemaess nicht symmetrisch. Gemessen wurde also
        // etwas anderes als gemeint.
        //
        // Die Aussage lautet: Was im Zimmer ganz links sitzt, ist vom linken Bildrand genauso
        // weit entfernt wie das ganz rechte vom rechten.
        for (sceneWidth in intArrayOf(72, 96, 120)) {
            val left = PlayScene.screenFraction(0f, sceneWidth)
            val right = PlayScene.screenFraction(1f, sceneWidth)
            assertTrue(
                "Bei Breite $sceneWidth sitzt das Zimmer nicht mittig: links $left, rechts $right",
                kotlin.math.abs(left - (1f - right)) < 0.02f
            )
        }
    }

    @Test
    fun `auf schmalen Bildern nutzt das Zimmer die volle Breite`() {
        // Die Gegenprobe: Die Deckelung darf nur GROSSE Bilder betreffen. Wuerde sie auch im
        // Hochformat greifen, verschenkte sie Platz, den diese Zimmer dringend brauchen.
        for (sceneWidth in intArrayOf(PlayScene.MIN_SCENE_CELLS, 46, 54, PlayScene.MAX_ROOM_CELLS)) {
            assertTrue(
                "Bei Breite $sceneWidth wird das Zimmer unnoetig eingeengt",
                PlayScene.roomWidth(sceneWidth) == sceneWidth
            )
            assertTrue(
                "Bei Breite $sceneWidth wird die Verankerung unnoetig verschoben",
                PlayScene.screenFraction(0.25f, sceneWidth) == 0.25f
            )
        }
    }

    @Test
    fun `jede Kreatur hat ihre eigene Landschaft`() {
        // **Der Zweck der ganzen Arbeit, als Zahl.** Sechs Lebensraeume, die sich am Ende doch
        // aehneln, waeren viel Aufwand fuer nichts - und beim Bauen faellt das kaum auf, weil man
        // immer nur einen davon vor sich hat. Verglichen werden deshalb die tatsaechlich
        // gezeichneten Zellen, paarweise.
        // OHNE die Bodenlinie selbst: Sie ist der Horizont und ueber alle Landschaften hinweg
        // absichtlich dieselbe (sie laeuft durchs ganze Bild, damit kein Kasten entsteht). Sie
        // mitzuzaehlen verwaesserte jeden Vergleich - zwei voellig verschiedene Landschaften
        // haetten allein dadurch schon die halbe Flaeche gemeinsam.
        val drawn = AvatarSpecies.entries.associateWith { species ->
            PlayScene.build(
                PlayScene.Place.PARK, 0, width, floorY,
                PlayAmbientActivity.DayPhase.MIDDAY, species = species
            ).filter { it.y < floorY }.map { it.x to it.y }.toSet()
        }

        for (a in AvatarSpecies.entries) {
            for (b in AvatarSpecies.entries) {
                if (a.ordinal >= b.ordinal) continue
                val cellsA = drawn.getValue(a)
                val cellsB = drawn.getValue(b)
                val shared = cellsA.count { it in cellsB }
                val overlap = shared.toFloat() / minOf(cellsA.size, cellsB.size)
                assertTrue(
                    "Die Landschaften von $a und $b stimmen zu ${(overlap * 100).toInt()} % " +
                        "ueberein - dann ist es dieselbe Landschaft mit anderer Figur." +
                        System.lineSeparator() +
                        ScenePreview.render(PlayScene.Place.PARK, a) +
                        ScenePreview.render(PlayScene.Place.PARK, b),
                    overlap < 0.6f
                )
            }
        }
    }

    @Test
    fun `jede Kreatur wohnt anders`() {
        // **Das Gegenstueck zur Landschafts-Pruefung, und aus demselben Anlass.** Gemeldet wurde
        // "sie sehen alle gleich aus" - und das stimmte: Wohnzimmer, Kueche, Leseecke, Bad und
        // Schreibtisch waren fuer alle sechs Kreaturen bis auf die Zelle genau dieselben Raeume.
        // Beim Bauen faellt das nicht auf, weil man immer nur eine Wohnung vor sich hat.
        //
        // Verglichen wird die EINRICHTUNG, nicht das gezeichnete Bild: Boden, Decke und Tuer sind
        // ueberall dieselben und wuerden jeden Vergleich verwaessern - zwei voellig verschieden
        // moeblierte Zimmer haetten allein dadurch schon die halbe Flaeche gemeinsam.
        val rooms = listOf(
            PlayScene.Place.LIVING,
            PlayScene.Place.NOOK,
            PlayScene.Place.KITCHEN,
            PlayScene.Place.BATH,
            PlayScene.Place.DESK,
            PlayScene.Place.BEDROOM,
            PlayScene.Place.CRAFT
        )
        for (place in rooms) {
            val furniture = AvatarSpecies.entries.associateWith { species ->
                PlayScene.propFootprints(place, width, floorY, species)
                    .filterNot { it.first == PlayScene.Station.DOOR.name }
                    .flatMap { it.second }
                    .toSet()
            }
            for (a in AvatarSpecies.entries) {
                for (b in AvatarSpecies.entries) {
                    if (a.ordinal >= b.ordinal) continue
                    val cellsA = furniture.getValue(a)
                    val cellsB = furniture.getValue(b)
                    if (cellsA.isEmpty() || cellsB.isEmpty()) continue
                    val shared = cellsA.count { it in cellsB }
                    // An BEIDEN Zimmern gemessen (gemeinsame Zellen geteilt durch alle Zellen der
                    // beiden zusammen), nicht am kleineren von beiden wie bei den Landschaften.
                    // Der Unterschied zaehlt hier: Wer sparsam eingerichtet ist - GLOOP hat nur
                    // Bodenpolster, Moos und ein paar Schalen -, faellt sonst allein deshalb auf,
                    // weil das eine Stueck, das er mit den anderen teilt, bei ihm einen groesseren
                    // Anteil ausmacht. Gemessen werden soll aber, wie verschieden die beiden
                    // Zimmer sind, nicht wie voll das kleinere ist.
                    val overlap = shared.toFloat() / (cellsA.size + cellsB.size - shared)
                    assertTrue(
                        "$place ist bei $a und $b zu ${(overlap * 100).toInt()} % dieselbe " +
                            "Einrichtung - dann ist es dieselbe Wohnung mit anderem Bewohner." +
                            System.lineSeparator() +
                            ScenePreview.render(place, a) + ScenePreview.render(place, b),
                        overlap < MAX_SAME_FURNITURE
                    )
                }
            }
        }
    }

    /**
     * Wie viel zwei Wohnungen im selben Raum gemeinsam haben duerfen.
     *
     * Bewusst nicht bei null: Kuehlschrank, Waschbecken und Kuechenzeile sind bei allen dieselben,
     * und das soll auch so bleiben - es sind Gegenstaende ohne Charakter, und sechs verschiedene
     * Waschbecken zu zeichnen waere Arbeit, die niemand bemerkt.
     *
     * **Und bewusst nicht schaerfer als das, was die Zahl hergibt.** Gezaehlt werden Zellen, nicht
     * Gegenstaende - zwei ganz verschiedene Dinge an derselben Stelle (ein Pflanzenregal und eine
     * Truhe, beide neun Zellen breit, beide am selben Anker) haben allein durch ihre Umrisse gut
     * die Haelfte ihrer Zellen gemeinsam, ohne dass man sie im Bild verwechseln koennte. Beim
     * Einrichten der sechs Wohnungen lagen solche Paare durchweg zwischen 50 und 67 %.
     *
     * Was diese Grenze dagegen zuverlaessig faengt, ist genau der Fall, um den es geht: dasselbe
     * Moebelstueck in zwei Wohnungen. Ein geteiltes Sofa lag bei 76 %, eine geteilte Kuechenzeile
     * bei 66 bis 91 %, zwei unveraendert gleiche Zimmer bei 100 %.
     */
    private val MAX_SAME_FURNITURE = 0.7f

    @Test
    fun `jeder Lebensraum bietet etwas zum Sitzen`() {
        // Alle Spazier-Ablaeufe steuern Station.BENCH an (siehe PlayRoutines). Fehlt sie in einem
        // Lebensraum, laeuft dort die Haelfte davon ins Leere - ohne Absturz und ohne Warnung,
        // die Figur bliebe einfach stehen.
        for (species in AvatarSpecies.entries) {
            assertTrue(
                "$species hat draussen nichts zum Sitzen",
                PlayScene.Station.BENCH in PlayScene.stationsAt(PlayScene.Place.PARK, species)
            )
        }
    }

    @Test
    fun `der Boden gehoert zur Landschaft`() {
        // Sand, Wasser und Wiese muessen sich unterscheiden - sonst saehe die Savanne aus wie
        // eine Wiese mit Akazien. Geprueft an der untersten Zeile, in der Bewuchs bzw. Untergrund
        // liegt, und zwar OHNE die Requisiten darueber.
        val ground = AvatarSpecies.entries.associateWith { species ->
            PlayScene.build(
                PlayScene.Place.PARK, 0, width, floorY,
                PlayAmbientActivity.DayPhase.MIDDAY, species = species
            ).filter { it.y >= floorY - 1 }.map { it.x to it.y }.toSet()
        }
        val distinct = ground.values.toSet()
        assertTrue(
            "Von sechs Landschaften haben nur ${distinct.size} einen eigenen Boden",
            distinct.size >= 4
        )
    }
}
