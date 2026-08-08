package com.notime.glyphsim.matrix

import org.junit.Assert.assertTrue
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
    fun `Bodenmoebel stehen nicht ineinander`() {
        // Nur was auf dem BODEN steht - ein Regal ueber dem Tisch ist gewollt und ueberlappt
        // waagerecht selbstverstaendlich.
        for (species in AvatarSpecies.entries) {
            for (place in PlayScene.Place.entries) {
                val onFloor = PlayScene.stationsAt(place, species)
                    .filter { it != PlayScene.Station.DOOR }
                    .mapNotNull { station ->
                        val cells = PlayScene.propCellsAt(place, station, width, floorY, species)
                        if (cells.isEmpty()) null else station to cells.toSet()
                    }
                for (i in onFloor.indices) {
                    for (j in (i + 1) until onFloor.size) {
                        val (aName, a) = onFloor[i]
                        val (bName, b) = onFloor[j]
                        val shared = a.count { it in b }
                        assertTrue(
                            "$aName und $bName stehen an $place ($species) ineinander " +
                                "($shared gemeinsame Zellen).\n\n" +
                                ScenePreview.render(place, species),
                            shared == 0
                        )
                    }
                }
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
}
