package com.notime.glyphsim.matrix

import java.io.File

/**
 * **Werkzeug, kein Test.** Rendert eine Szene als lesbares Zeichenraster - der Ersatz fuer das
 * Geraet, wenn am Aussehen der Welt gearbeitet wird.
 *
 * **Warum es das gibt.** Praktisch jeder Gestaltungsfehler beim Aufbau dieser Welt war ein
 * Kompositionsfehler und in keiner Zahl zu erkennen: die Figur stand IM Teleskop, der Avatar
 * verdeckte den ganzen Kuehlschrank, das Gras stanzte dem Baumstamm eine Kerbe, der
 * Buecherschrank war so dicht gezeichnet, dass man nur ein Gitter sah. Gefunden wurde das jedes
 * Mal dadurch, dass die Szene als ASCII ausgegeben und angesehen wurde - und jedes Mal wurde
 * dieser Ausgabe-Code neu geschrieben und danach weggeworfen.
 *
 * Hier steht er nun fest. Zwei Verwendungen:
 * 1. [dumpAll] schreibt alle Kombinationen in eine Datei, wenn an der Gestaltung gearbeitet wird.
 * 2. [render] liefert das Bild fuer FEHLERMELDUNGEN in SceneCompositionTest - dort steht dann
 *    nicht nur "ueberdeckt zu 70 %", sondern das Bild dazu.
 *
 * Die Helligkeitsstufen sind bewusst als unterschiedlich "dichte" Zeichen gewaehlt, damit sich
 * die Tiefenstaffelung (Boden schwach, Moebel mittel, Licht hell) beim Ueberfliegen ablesen
 * laesst - genau darum geht es beim Beurteilen einer Szene.
 */
object ScenePreview {

    const val WIDTH = 54
    const val FLOOR_Y = 24

    private fun ramp(brightness: Int): Char = when {
        brightness >= 2200 -> '@'
        brightness >= 1600 -> '#'
        brightness >= 1100 -> '+'
        brightness >= 700 -> '='
        brightness >= 400 -> '-'
        brightness > 0 -> '.'
        else -> ' '
    }

    /**
     * Ein Bild der Szene. Ist [station] gesetzt, steht die Figur an genau diesem Platz (und die
     * Requisite wird als "in Benutzung" gezeichnet); sonst an ihrem Ruheplatz.
     */
    fun render(
        place: PlayScene.Place,
        species: AvatarSpecies = AvatarSpecies.PUFFLING,
        dayPhase: PlayAmbientActivity.DayPhase = PlayAmbientActivity.DayPhase.MIDDAY,
        station: PlayScene.Station? = null,
        phase: Int = 0,
        lampOn: Boolean = true,
        tvOn: Boolean = true,
        width: Int = WIDTH,
        floorY: Int = FLOOR_Y,
        showAvatar: Boolean = true
    ): String {
        val grid = Array(floorY + 3) { CharArray(width) { ' ' } }

        for (cell in PlayScene.build(
            place, phase, width, floorY, dayPhase, 1f, lampOn, tvOn, station, species
        )) {
            if (cell.y in grid.indices && cell.x in 0 until width) {
                grid[cell.y][cell.x] = ramp(cell.brightness)
            }
        }

        if (showAvatar) {
            val spot = station?.let { PlayScene.stationSpot(place, it, width, floorY, species) }
            val originX = spot?.let { it.centerX - AvatarGeometry.SIZE / 2 }
                ?: ((width - AvatarGeometry.SIZE) * PlayScene.avatarAnchorX(place)).toInt()
            val groundY = spot?.groundY ?: (floorY - 1)
            val originY = groundY - AvatarBodies.forSpecies(species).groundRow()
            val frame = AvatarAnimations.idlePose(species)
            for (y in 0 until AvatarGeometry.HEIGHT) {
                for (x in 0 until AvatarGeometry.SIZE) {
                    if (frame[y * AvatarGeometry.SIZE + x] <= 0) continue
                    val gy = originY + y
                    val gx = originX + x
                    if (gy in grid.indices && gx in 0 until width) grid[gy][gx] = 'A'
                }
            }
            // Vordere Ebene ZULETZT - sie liegt auch im Bild vor der Figur.
            if (station != null) {
                for (cell in PlayScene.buildFront(place, station, width, floorY, dayPhase, 1f, species)) {
                    if (cell.y in grid.indices && cell.x in 0 until width) grid[cell.y][cell.x] = 'X'
                }
            }
        }

        val header = buildString {
            append(place)
            append(" / ").append(species)
            append(" / ").append(dayPhase)
            if (station != null) append(" / an ").append(station)
        }
        return header + "\n" + grid.joinToString("\n") { String(it).trimEnd() } +
            "\n   A=Figur  X=davor  @#+=- von hell nach dunkel\n"
    }

    /**
     * Schreibt ALLE Kombinationen in eine Datei - der Blick auf die ganze Welt auf einmal.
     * Aufgerufen von ScenePreviewTool; sonst nicht Teil eines Testlaufs.
     */
    fun dumpAll(target: File) {
        val out = StringBuilder()
        for (species in AvatarSpecies.entries) {
            for (place in PlayScene.Place.entries) {
                out.append(render(place, species)).append('\n')
                for (station in PlayScene.stationsAt(place, species)) {
                    if (station == PlayScene.Station.DOOR) continue
                    out.append(render(place, species, station = station)).append('\n')
                }
            }
        }
        // Die Nacht fuer JEDE Kreatur, nicht nur fuer den Einstiegs-Avatar: Seit jede ihre eigene
        // Leuchte hat (Feuerschale, Leuchtpilz, Kerzen, Laterne, Lampion), ist der Nachtblick der
        // wichtigste von allen - dann weicht der Raum zurueck und nur das Licht bleibt stehen.
        for (species in AvatarSpecies.entries) {
            for (place in PlayScene.Place.entries) {
                out.append(render(place, species, PlayAmbientActivity.DayPhase.NIGHT)).append('\n')
            }
        }
        target.writeText(out.toString())
    }
}
