package com.notime.glyphsim.matrix

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

/**
 * Der Zeichenkasten fuer alles, was der Avatar BENUTZT - Requisiten, Geraete, Baelle, Werkzeug.
 *
 * **Warum es ihn gibt.** Bis hierher zeichnete jedes Motiv seine Helligkeiten selbst aus, meist
 * als Rechnung wie `GLOW - 180`. Das Ergebnis war zwangslaeufig: zwoelf Motive, zwoelf Tonwerte,
 * und alle davon zufaellig dort, wo auch die Kulisse liegt. Auf dem Kontaktbogen sah man dann ein
 * Buch, dessen Seiten genau so hell waren wie das Regal dahinter, das sie beruehrte - zwei
 * Gegenstaende, ein Fleck. Auf einem Raster ohne Farbe ist der Tonwert die EINZIGE Auskunft
 * darueber, was wozu gehoert; wer ihn dem Zufall ueberlaesst, verliert die Lesbarkeit, egal wie
 * sorgfaeltig die Form gezeichnet ist.
 *
 * **Was hier festgelegt ist.** Drei Regeln, die kein Motiv mehr einzeln beantworten muss:
 *
 * 1. **Requisiten haben ihre eigene Tonwertstufe.** [BODY] liegt zwischen [PlayScene.FURNITURE]
 *    und [PlayScene.GLOW] - ueber allem, was nur herumsteht, unter allem, was leuchtet. Damit
 *    steht jede Requisite immer vor der Kulisse, ohne dass irgendwo eine Zahl nachgerechnet wird.
 * 2. **Das Licht kommt von oben links.** Immer. [Sketch.render] hebt Oberkanten auf [EDGE] und
 *    linke Kanten auf einen Zwischenwert, ohne dass ein Motiv das selbst zeichnet. Ein
 *    einheitlicher Lichteinfall ueber alle Gegenstaende hinweg ist das, was aus zwoelf einzelnen
 *    Zeichnungen eine Welt macht.
 * 3. **Jede Requisite bekommt Luft.** [VOID] ist eine ausdruecklich schwarze Zelle, die die
 *    Effektebene deckend ueber die Kulisse malt. Ein Ring davon um die Silhouette schneidet den
 *    Gegenstand aus dem Hintergrund heraus.
 *
 * **Und daraus folgt die vierte, die beim Zeichnen am leichtesten uebersehen wird.** Es gibt auf
 * diesem Raster genau ZWEI Arten, etwas vom Hintergrund zu trennen, und jedes Motiv muss sich
 * fuer eine entscheiden:
 *
 * - **Materie trennt sich durch die Kante.** Ein Becher, ein Buch, ein Ball hat einen Umriss, und
 *   um diesen Umriss gehoert der schwarze Ring. Solche Motive zeichnet [Sketch.render] mit
 *   eingeschalteter Aussparung.
 * - **Licht trennt sich durch Helligkeit.** Dampf, Atem, Funken, Noten haben keinen Umriss - ein
 *   schwarzer Ring machte harte Aufkleber daraus. Sie trennen sich stattdessen dadurch, dass sie
 *   HELLER sind als alles, was in der Kulisse vorkommen kann, also ab [EDGE] aufwaerts.
 *
 * Dazwischen liegt die einzige wirklich falsche Wahl: Material auf [BODY]-Gewicht OHNE Aussparung.
 * Das ist schwer genug, um mit Moebeln zu verschmelzen, und nicht hell genug, um sich davon
 * abzuheben - genau der Fleck, mit dem diese Ebene angefangen hat. Sehr schwache Zeichnung bis
 * [DETAIL] darf dagegen ohne Aussparung stehen: Sie ist Atmosphaere und soll sich einfuegen.
 * Festgehalten wird die Wahl von PlayInkTest.
 *
 * Gezeichnet wird in LOKALEN Koordinaten mit (0,0) oben links am Motiv, immer nach rechts gedacht.
 * Steht die Figur zu weit rechts, spiegelt [Sketch] beim Ausgeben - die Beleuchtung wird trotzdem
 * erst danach berechnet und faellt deshalb auch im gespiegelten Bild von oben links ein.
 */
object PlayInk {

    /**
     * Deckend schwarz - die ausgesparte Zelle.
     *
     * Nicht 0: Die Ebene ueberspringt unbeleuchtete Zellen beim Zeichnen (siehe [PlaySceneView]),
     * eine 0 waere also schlicht durchsichtig und liesse die Kulisse stehen. 1 ist die kleinste
     * Helligkeit, die noch gemalt wird - optisch schwarz, aber deckend.
     */
    const val VOID = 1

    /** Der Schatten, den ein Gegenstand auf den Boden wirft: eine Spur DUNKLER als der Boden
     *  selbst ([PlayScene.STRUCTURE]) - sonst waere es kein Schatten, sondern eine Aufhellung. */
    const val SHADOW = 480

    /** Das Material einer Requisite - die Grundstufe, in der alles Feste gezeichnet wird. */
    const val BODY = 1900

    /** Die beleuchtete Kante. Wird von [Sketch.render] gesetzt, nicht von den Motiven. */
    const val EDGE = 2900

    /** Der eine helle Punkt: Glanz auf dem Glas, der Funke, die Spitze der Flamme. Sparsam -
     *  eine ganze Flaeche auf dieser Stufe nimmt der Figur die Aufmerksamkeit weg. */
    const val SPARK = PlayScene.HIGHLIGHT

    /** Binnenzeichnung: Schrift auf der Seite, Muster auf dem Stoff, Speichen im Rad. Bewusst
     *  UNTER [BODY], damit Einzelheiten die Silhouette nicht zerschneiden. */
    const val DETAIL = PlayScene.FURNITURE

    /** Die halbe Stufe fuer die linke Kante: oben hell, links etwas heller als das Material,
     *  rechts und unten gar nicht. */
    const val LEFT_EDGE = (BODY + EDGE) / 2

    /**
     * Alle Stufen, die auf dieser Ebene ueberhaupt vorkommen duerfen.
     *
     * Oeffentlich, damit ein Test es festhalten kann: Genau die Freiheit, "mal eben" eine Zahl
     * wie `GLOW - 180` hinzuschreiben, hat den Look vorher aufgeloest. Eine Stufe, die hier nicht
     * steht, ist keine gestalterische Entscheidung, sondern ein Ausrutscher.
     */
    val LEVELS: Set<Int> = setOf(VOID, SHADOW, DETAIL, BODY, LEFT_EDGE, EDGE, SPARK)

    /**
     * Weiche Bewegung statt gleichfoermigem Hin und Her.
     *
     * Ein `beat % 4` bewegt einen Gegenstand in vier gleich grossen Spruengen - das liest sich als
     * Ruckeln, nicht als Bewegung. Diese Kurve laeuft ueber [steps] Takte einmal von 0 nach 1 und
     * zurueck, langsam an den Enden und schnell in der Mitte. Genau so bewegt sich alles, was
     * schwingt: ein Pendel, ein Deckel, ein atmender Brustkorb.
     */
    fun swing(phase: Int, steps: Int): Float {
        if (steps <= 0) return 0f
        val t = (((phase % steps) + steps) % steps) / steps.toFloat()
        return (1f - cos(2f * PI.toFloat() * t)) / 2f
    }

    /**
     * Der Anlauf vor der Tat: -1 holt aus, 0 ist Ruhe, 1 fuehrt aus.
     *
     * Eine Bewegung, die ohne Ausholen sofort losgeht, wirkt auf so wenigen Zellen wie ein
     * Bildfehler; ein einziger Takt Gegenbewegung davor macht daraus eine Handlung.
     */
    fun anticipate(phase: Int, steps: Int): Float {
        if (steps <= 0) return 0f
        val t = (((phase % steps) + steps) % steps) / steps.toFloat()
        return when {
            t < 0.25f -> -(t / 0.25f)
            t < 0.5f -> -1f + (t - 0.25f) / 0.25f * 2f
            else -> 1f - (t - 0.5f) / 0.5f
        }
    }

    /**
     * Eine Zeichenflaeche fuer ein Motiv.
     *
     * [floorY] ist die Bodenzeile: Nichts wird darunter gezeichnet. Das ist keine Feinheit,
     * sondern der haeufigste Fehler dieser Ebene gewesen - Motive, die unten aus der Welt
     * herausliefen und im Nichts weitergingen.
     */
    class Sketch(
        private val originX: Int,
        private val originY: Int,
        private val direction: Int,
        private val widthCells: Int,
        private val floorY: Int
    ) {
        private val painted = LinkedHashMap<Long, Int>()

        private fun key(x: Int, y: Int): Long = x.toLong() * STRIDE + y.toLong()

        private fun put(lx: Int, ly: Int, level: Int) {
            val x = originX + direction * lx
            val y = originY + ly
            if (x < 0 || x >= widthCells) return
            if (y < 0 || y >= floorY) return
            painted[key(x, y)] = level
        }

        /** Eine einzelne Zelle. */
        fun dot(x: Int, y: Int, level: Int = BODY) = put(x, y, level)

        /** Der eine helle Punkt - siehe [SPARK]. */
        fun spark(x: Int, y: Int) = put(x, y, SPARK)

        /** Gerade Linie zwischen zwei Punkten. */
        fun line(x1: Int, y1: Int, x2: Int, y2: Int, level: Int = BODY) {
            val steps = maxOf(abs(x2 - x1), abs(y2 - y1)).coerceAtLeast(1)
            for (i in 0..steps) {
                put(x1 + (x2 - x1) * i / steps, y1 + (y2 - y1) * i / steps, level)
            }
        }

        /** Umriss eines Rechtecks. */
        fun box(left: Int, top: Int, right: Int, bottom: Int, level: Int = BODY) {
            line(left, top, right, top, level)
            line(left, bottom, right, bottom, level)
            line(left, top, left, bottom, level)
            line(right, top, right, bottom, level)
        }

        /** Gefuellte Flaeche. */
        fun fill(left: Int, top: Int, right: Int, bottom: Int, level: Int = BODY) {
            for (y in top..bottom) for (x in left..right) put(x, y, level)
        }

        /**
         * Ein Motiv als lesbares Bild - dieselbe Begruendung wie bei `sprite` im Katalog: eine
         * Zeichnung, der man ansieht, was sie zeichnet, laesst sich ueberhaupt verbessern.
         *
         * `#` Material, `+` Binnenzeichnung ([DETAIL]), `*` Glanzpunkt ([SPARK]), alles andere
         * bleibt frei.
         */
        fun art(x: Int, y: Int, vararg rows: String) {
            rows.forEachIndexed { dy, row ->
                row.forEachIndexed { dx, c ->
                    when (c) {
                        '#' -> put(x + dx, y + dy, BODY)
                        '+' -> put(x + dx, y + dy, DETAIL)
                        '*' -> put(x + dx, y + dy, SPARK)
                    }
                }
            }
        }

        /**
         * Dieselbe Zeichnung wie [art], aber ganz in einer waehlbaren Stufe.
         *
         * Fuer dasselbe Ding in zwei Zustaenden - der frische Fussabdruck hell, der vorige schon
         * verblassend. Ohne diesen Weg muesste man die Form nach dem Zeichnen flaechig
         * uebermalen, und dabei geht genau das verloren, was sie erkennbar macht: die Luecken.
         */
        fun stamp(x: Int, y: Int, level: Int, vararg rows: String) {
            rows.forEachIndexed { dy, row ->
                row.forEachIndexed { dx, c ->
                    if (c == '#') put(x + dx, y + dy, level)
                }
            }
        }

        /** Ob ueberhaupt etwas gezeichnet wurde. */
        fun isEmpty(): Boolean = painted.isEmpty()

        /**
         * Das fertige Motiv: Material, Beleuchtung, Aussparung und Bodenschatten.
         *
         * [carve] schneidet das Motiv mit einem Ring schwarzer Zellen aus der Kulisse. Richtig
         * fuer alles, was VOR der Welt steht - falsch fuer Dampf, Duft oder Notenzeichen, die
         * luftig bleiben sollen.
         *
         * [grounded] setzt unter die tiefste Zeile einen Schatten. Nur fuer Dinge, die wirklich
         * auf dem Boden stehen; ein schwebender Mond bekommt keinen.
         */
        fun render(carve: Boolean = true, grounded: Boolean = false): List<SceneCell> {
            if (painted.isEmpty()) return emptyList()
            val out = mutableListOf<SceneCell>()

            // 1. Aussparung zuerst in die Liste - sie liegt unter dem Motiv, und die Ebene wird
            // in Reihenfolge gezeichnet.
            if (carve) {
                val halo = LinkedHashSet<Long>()
                for (k in painted.keys) {
                    val x = (k / STRIDE).toInt()
                    val y = (k % STRIDE).toInt()
                    for ((dx, dy) in NEIGHBOURS) {
                        val nx = x + dx
                        val ny = y + dy
                        if (nx < 0 || nx >= widthCells || ny < 0 || ny >= floorY) continue
                        val nk = key(nx, ny)
                        if (!painted.containsKey(nk)) halo += nk
                    }
                }
                for (k in halo) out += SceneCell((k / STRIDE).toInt(), (k % STRIDE).toInt(), VOID)
            }

            // 2. Der Bodenschatten liegt vor der Aussparung, aber hinter dem Gegenstand.
            if (grounded) {
                val lowest = painted.keys.maxOf { (it % STRIDE).toInt() }
                val shadowY = (lowest + 1).coerceAtMost(floorY)
                val columns = painted.keys.map { (it / STRIDE).toInt() }
                for (x in columns.min()..columns.max()) out += SceneCell(x, shadowY, SHADOW)
            }

            // 3. Beleuchtung. Erst hier, und ausdruecklich im BILD-Raster statt im lokalen: sonst
            // faellt das Licht im gespiegelten Motiv von oben RECHTS ein, und zwei nebeneinander
            // stehende Gegenstaende haetten zwei verschiedene Sonnen.
            for ((k, level) in painted) {
                val x = (k / STRIDE).toInt()
                val y = (k % STRIDE).toInt()
                val shade = when {
                    level != BODY -> level
                    (painted[key(x, y - 1)] ?: 0) == 0 && (painted[key(x, y + 1)] ?: 0) >= BODY -> EDGE
                    (painted[key(x - 1, y)] ?: 0) == 0 && (painted[key(x + 1, y)] ?: 0) >= BODY -> LEFT_EDGE
                    else -> level
                }
                out += SceneCell(x, y, shade, shade >= SPARK)
            }
            return out
        }

        private companion object {
            /** Packt (x,y) in eine Zahl. 4096 liegt weit ueber jeder vorkommenden Zellenzahl. */
            const val STRIDE = 4096L
            val NEIGHBOURS = listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0)
        }
    }
}
