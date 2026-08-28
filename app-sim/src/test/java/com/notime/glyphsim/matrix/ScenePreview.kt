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
 *
 * **Warum ZWEI Ansichten.** Lange zeichnete dieses Werkzeug jede Effektzelle als `*` und die
 * Figur als `A` - unabhaengig davon, wie hell sie wirklich waren. Damit war ausgerechnet die
 * Information weg, an der Lesbarkeit haengt: ob sich ein Motiv vom Moebel dahinter ueberhaupt
 * abhebt. Ein Bogen, der jede Requisite gleich hell zeigt, bescheinigt jedem Entwurf Trennung,
 * die er gar nicht hat. Darum steht jetzt zuerst das VERBUNDBILD - alles durch dieselbe Rampe,
 * so wie es das Auge trifft - und darunter die EBENENKARTE, die nur noch die Herkunft nennt.
 * Beurteilt wird oben, zugeordnet wird unten.
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
        // Die ausgesparte Zelle (PlayInk.VOID) ist deckend schwarz gemalt, aber eben SCHWARZ -
        // sie muss hier als Dunkelheit erscheinen, sonst sieht der Bogen eine Trennung als Flaeche.
        brightness > 60 -> '.'
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
        showAvatar: Boolean = true,
        /** Zusaetzliche Handlungseffekte, zuletzt wie auf dem Bildschirm gezeichnet. */
        overlay: List<SceneCell> = emptyList(),
        /** Was sich angesammelt hat - siehe PlayScene.Acquisition. */
        acquisitions: Set<PlayScene.Acquisition> = emptySet()
    ): String {
        val grid = Array(floorY + 3) { CharArray(width) { ' ' } }
        val layer = Array(floorY + 3) { CharArray(width) { ' ' } }

        // Eine einzige Schreibstelle fuer alle Ebenen: das Bild bekommt die WIRKLICHE Helligkeit,
        // die Ebenenkarte daneben nur die Herkunft. Solange beides in derselben Funktion passiert,
        // koennen die zwei Ansichten nicht auseinanderlaufen.
        fun put(x: Int, y: Int, brightness: Int, mark: Char) {
            if (y !in grid.indices || x !in 0 until width) return
            grid[y][x] = ramp(brightness)
            layer[y][x] = if (brightness <= 0) ' ' else mark
        }

        for (cell in PlayScene.build(
            place, phase, width, floorY, dayPhase, 1f, lampOn, tvOn, station, species, acquisitions
        )) {
            put(cell.x, cell.y, cell.brightness, '.')
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
                    val lit = frame[y * AvatarGeometry.SIZE + x]
                    if (lit <= 0) continue
                    put(originX + x, originY + y, lit, 'A')
                }
            }
            // Vordere Ebene ZULETZT - sie liegt auch im Bild vor der Figur.
            if (station != null) {
                for (cell in PlayScene.buildFront(place, station, width, floorY, dayPhase, 1f, species)) {
                    put(cell.x, cell.y, cell.brightness, 'X')
                }
            }
        }

        for (cell in overlay) {
            put(cell.x, cell.y, cell.brightness, '*')
        }

        val header = buildString {
            append(place)
            append(" / ").append(species)
            append(" / ").append(dayPhase)
            if (station != null) append(" / an ").append(station)
        }
        val picture = grid.joinToString("\n") { String(it).trimEnd() }
        val map = layer.joinToString("\n") { String(it).trimEnd() }
        return header + "\n" + picture +
            "\n   [Bild]    @#+=-. hell nach dunkel - SO trifft es das Auge\n" + map +
            "\n   [Ebenen]  .=Kulisse  A=Figur  X=davor  *=Effekt\n"
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
