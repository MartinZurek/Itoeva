package com.notime.glyphkalender.glyph

import com.nothing.ketchum.Common
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.FrameCrossfade
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Baut Roh-Pixel-Frames (ein Helligkeitswert 0..4095 pro Zelle) je Erinnerungs-Typ.
 * Bewusst als grobe, direkt fuer das Raster entworfene Pixel-Kunst statt als
 * skaliertes Vektor-Icon: Das (4a) Pro hat laut offizieller Spezifikation nur ein
 * 13x13-Raster (Common.getDeviceMatrixLength()) - feine Linien wuerden beim
 * Herunterskalieren einer Bitmap unlesbar/zufaellig aussehen. Koordinaten unten
 * sind auf ein 13x13-Raster gemuenzt (Zentrum ca. 6,6) - [Common.getDeviceMatrixLength]
 * wird nur fuers Grid-Array selbst benutzt, nicht fuer die Koordinaten-Entwuerfe.
 *
 * Deutlich mehr Frames pro Animation als eine reine 4-6-Posen-Schleife: die meisten
 * Typen laufen ueber [FrameCrossfade.withCrossfades] durch weiche Helligkeits-
 * Ueberblendungen zwischen den Posen statt hartem Sprung. DRINK und REST sind komplett eigens als
 * durchgaengige kleine Erzaehlung gebaut (Glas fuellt sich tropfenweise und wird
 * geleert; Dampf steigt echt auf und verblasst, statt in einem Zickzack zu
 * "springen") - siehe [drinkFrames]/[restFrames].
 *
 * Alle Frames wurden mit einem kleinen Python/Pillow-Skript (nicht Teil des Repos, nur
 * zur Entwurfspruefung) als vergroesserte Bitmaps gerendert und visuell durchgesehen -
 * mehrere erste Entwuerfe waren am Bildschirm nicht erkennbar (Glas sah wie eine
 * Fensterscheibe aus, ein Auge wie eine Fliege oder ein Kreisel, die Teetasse mit Dampf
 * wie ein Hasenkopf, die Meditationsfigur wie ein schwebendes UFO, das Buch wie
 * Domino-Steine bzw. ein Geraet mit Trennwand) und wurden deshalb ueberarbeitet, bevor
 * sie ins Repo kamen. Insbesondere FOCUS wurde bewusst von "blinzelndes Auge" (liess
 * sich bei 13x13 nicht überzeugend von einem Kreis/Kreuz unterscheiden) zu einem
 * Kamera-Fokus-Sucher umgestaltet, der als Bewegungsmotiv deutlich besser funktioniert.
 */
object ReminderAnimations {

    fun framesFor(type: AnimationType): List<IntArray> {
        val size = Common.getDeviceMatrixLength()
        return when (type) {
            AnimationType.MOVE -> FrameCrossfade.withCrossfades(
                size,
                listOf(moveContactA(), moveFlight(), moveContactB(), moveFlight())
            )
            AnimationType.DRINK -> drinkFrames(size)
            AnimationType.FOCUS -> FrameCrossfade.withCrossfades(
                size,
                listOf(
                    focusReticle(ringSmall()), focusReticle(ringMedium()),
                    focusReticle(ringBig()), focusReticle(ringMedium())
                )
            )
            AnimationType.GENERAL -> FrameCrossfade.withCrossfades(size, listOf(bellLeft(), bellCenter(), bellRight(), bellCenter()))
            AnimationType.REST -> restFrames(size)
            AnimationType.WORK -> FrameCrossfade.withCrossfades(size, workScreenDigits().map { laptopFrame(it) })
            AnimationType.MINDFULNESS -> FrameCrossfade.withCrossfades(
                size,
                yogaSequence().map { (aura, kneeShift) -> yogaFrame(aura, kneeShift) }
            )
            AnimationType.LOVE -> FrameCrossfade.withCrossfades(size, listOf(heartBig(), heartSmall(), heartMedium(), heartSmall()))
            AnimationType.SLEEP -> FrameCrossfade.withCrossfades(size, listOf(sleepFrame1(), sleepFrame2(), sleepFrame3(), sleepFrame4()))
            AnimationType.MEDICINE -> FrameCrossfade.withCrossfades(
                size,
                listOf(medicineTiny(), medicineSmall(), medicineBig(), medicineSmall())
            )
            AnimationType.BOOK -> FrameCrossfade.withCrossfades(size, bookFrames(), steps = 1)
            AnimationType.CREATIVITY -> creativityFrames(size)
        }
    }

    // setMatrixFrame(int[]) reicht Werte direkt an die Hardware durch (kein Umweg ueber
    // GlyphMatrixObject/Brightness-API). Deren Skala geht 0..4095, nicht 0..255 - siehe
    // GlyphMatrixUtils-Bytecode: nativeWert = 4095 * brightness(0..255) / 255. Ein hier
    // gesetzter Wert von 255 waere also nur ~6% der tatsaechlichen Maximalhelligkeit.
    private const val MAX_BRIGHTNESS = 4095

    private fun toGrid(size: Int, points: List<Pair<Int, Int>>): IntArray {
        val grid = IntArray(size * size)
        for ((x, y) in points) {
            if (x in 0 until size && y in 0 until size) {
                grid[y * size + x] = MAX_BRIGHTNESS
            }
        }
        return grid
    }

    /** Frame aus voll hellen Punkten plus optionalen gedimmten Akzenten (Schatten/Glanz/Dampf). */
    private fun buildFrame(size: Int, full: List<Pair<Int, Int>>, dim: List<Pair<Pair<Int, Int>, Int>> = emptyList()): IntArray {
        val grid = IntArray(size * size)
        fun set(x: Int, y: Int, brightness: Int) {
            if (x in 0 until size && y in 0 until size) grid[y * size + x] = brightness
        }
        for ((point, brightness) in dim) set(point.first, point.second, brightness)
        for ((x, y) in full) set(x, y, MAX_BRIGHTNESS)
        return grid
    }

    // ---- Ueberblendung: aus wenigen Posen viele weiche Zwischenframes machen, siehe FrameCrossfade ----

    // ---- MOVE: Strichmaennchen-Laufzyklus. Zwei Kontakt-Posen (Beine weit gegraetscht,
    // Fuesse auf dem "Boden") wechseln sich mit einer geduckten Flugphase ab (beide Fuesse
    // eng zusammengezogen, wie in der Luft) - die Kontrast zwischen ausgestreckt/geduckt
    // macht den Laufeindruck viel deutlicher als eine dritte, nur leicht andere
    // Kontakt-Pose es koennte ----

    private fun moveContactA() = listOf(
        6 to 2,
        6 to 3,
        6 to 4, 6 to 5, 6 to 6,
        4 to 4, 5 to 4,
        7 to 5, 8 to 6,
        6 to 7,
        5 to 8, 4 to 9, 3 to 10,
        7 to 8, 8 to 9, 9 to 10
    )

    private fun moveFlight() = listOf(
        6 to 2,
        6 to 3,
        6 to 4, 6 to 5, 6 to 6,
        5 to 4, 7 to 4,
        6 to 7,
        5 to 7, 5 to 8,
        7 to 7, 7 to 8
    )

    private fun moveContactB() = listOf(
        6 to 2,
        6 to 3,
        6 to 4, 6 to 5, 6 to 6,
        8 to 4, 9 to 4,
        5 to 5, 4 to 6,
        6 to 7,
        7 to 8, 8 to 9, 9 to 10,
        5 to 8, 4 to 9, 3 to 10
    )

    // ---- DRINK: Glas fuellt sich tropfenweise, wird kurz gehalten, dann geleert ----

    private const val GLASS_LEFT = 4
    private const val GLASS_RIGHT = 8
    private const val GLASS_TOP = 4
    private const val GLASS_BOTTOM = 9
    private val WATER_ROWS = listOf(8, 7, 6, 5) // von unten nach oben befuellt

    private fun glassOutline() =
        (GLASS_TOP..GLASS_BOTTOM).flatMap { y -> listOf(GLASS_LEFT to y, GLASS_RIGHT to y) } +
            (GLASS_LEFT + 1 until GLASS_RIGHT).map { it to GLASS_BOTTOM }

    private fun waterRow(y: Int) = (GLASS_LEFT + 1 until GLASS_RIGHT).map { it to y }

    private fun drinkFrame(size: Int, filledRows: List<Int>, dropletY: Int? = null, splash: Boolean = false): IntArray {
        val points = glassOutline().toMutableList()
        filledRows.forEach { points += waterRow(it) }
        dropletY?.let { points += 6 to it }
        if (splash) {
            val splashY = ((filledRows.minOrNull() ?: GLASS_BOTTOM) - 1).coerceIn(GLASS_TOP, GLASS_BOTTOM)
            points += listOf(5 to splashY, 7 to splashY)
        }
        return toGrid(size, points)
    }

    private fun drinkFrames(size: Int): List<IntArray> {
        val frames = mutableListOf<IntArray>()
        frames += drinkFrame(size, emptyList())
        frames += drinkFrame(size, emptyList())

        val filled = mutableListOf<Int>()
        for (row in WATER_ROWS) {
            val top = 1
            val bottom = (row - 1).coerceAtLeast(top)
            val fallSteps = if (bottom > top) listOf(top, (top + bottom) / 2, bottom) else listOf(top, bottom)
            fallSteps.forEach { y -> frames += drinkFrame(size, filled.toList(), dropletY = y) }
            frames += drinkFrame(size, filled.toList(), splash = true)
            filled += row
            frames += drinkFrame(size, filled.toList())
        }

        frames += drinkFrame(size, filled.toList())
        frames += drinkFrame(size, filled.toList())

        val remaining = filled.toMutableList()
        for (row in WATER_ROWS.reversed()) {
            remaining.remove(row)
            frames += drinkFrame(size, remaining.toList())
        }
        frames += drinkFrame(size, emptyList())
        return frames
    }

    // ---- FOCUS: Kamera-Fokus-Sucher - vier feste Eckwinkel (Sucherrahmen) umrahmen einen
    // Zielring, der durchgehend zwischen drei Groessen pulsiert (Autofokus-Eindruck). Ein
    // blinzelndes Auge liess sich bei 13x13 nicht ueberzeugend von einem Kreis/Kreuz
    // unterscheiden - der Sucherrahmen macht die "Fokus"-Bedeutung eindeutig ----

    private fun viewfinderCorners() = listOf(
        2 to 2, 3 to 2, 2 to 3,
        10 to 2, 9 to 2, 10 to 3,
        2 to 10, 3 to 10, 2 to 9,
        10 to 10, 9 to 10, 10 to 9
    )

    private fun ringSmall() = listOf(
        6 to 4, 5 to 5, 7 to 5, 4 to 6, 8 to 6, 5 to 7, 7 to 7, 6 to 8, 6 to 6
    )

    private fun ringMedium() = listOf(
        6 to 3, 4 to 4, 8 to 4, 3 to 6, 9 to 6, 4 to 8, 8 to 8, 6 to 9
    )

    private fun ringBig() = listOf(
        6 to 2, 4 to 3, 8 to 3, 3 to 4, 9 to 4, 2 to 6, 10 to 6, 3 to 8, 9 to 8, 4 to 9, 8 to 9, 6 to 10
    )

    private fun focusReticle(ring: List<Pair<Int, Int>>) = viewfinderCorners() + ring

    // ---- GENERAL: Glocke schwingt wie ein Pendel (links -> Mitte -> rechts -> Mitte) ----

    private fun bellLeft() = listOf(
        5 to 2,
        4 to 3, 6 to 3,
        3 to 4, 7 to 4,
        3 to 5, 7 to 5,
        3 to 6, 7 to 6,
        2 to 7, 3 to 7, 4 to 7, 5 to 7, 6 to 7, 7 to 7, 8 to 7,
        5 to 8
    )

    private fun bellCenter() = listOf(
        6 to 2,
        5 to 3, 7 to 3,
        4 to 4, 8 to 4,
        4 to 5, 8 to 5,
        4 to 6, 8 to 6,
        3 to 7, 4 to 7, 5 to 7, 6 to 7, 7 to 7, 8 to 7, 9 to 7,
        6 to 8
    )

    private fun bellRight() = listOf(
        7 to 2,
        6 to 3, 8 to 3,
        5 to 4, 9 to 4,
        5 to 5, 9 to 5,
        5 to 6, 9 to 6,
        4 to 7, 5 to 7, 6 to 7, 7 to 7, 8 to 7, 9 to 7, 10 to 7,
        7 to 8
    )

    // ---- REST: Tasse auf Untertasse, mit aufsteigendem, ausblendendem Dampf ----
    //
    // Die vorherige Fassung wurde als "Kaffeemuehle" gelesen, und das zu Recht: ein exakt
    // rechteckiger Koerper mit einem klobigen, ebenso rechteckigen Henkel daneben sieht aus wie
    // ein Kasten mit Kurbel. Drei Aenderungen machen daraus ein Heissgetraenk:
    //
    // - **Untertasse** unter der Tasse. Das einzelne wirksamste Detail - eine Tasse steht auf
    //   etwas, ein Kasten nicht. Sie ist breiter als die Tasse und gedimmt, damit sie die Form
    //   traegt, ohne mit ihr um Aufmerksamkeit zu konkurrieren.
    // - **Runder Henkel** als dreipunktiger Bogen statt als angesetztes Quadrat.
    // - **Fuellstand**: eine gedimmte Zeile direkt unter dem Rand. Ohne sie ist die Tasse ein
    //   leerer Umriss; mit ihr ist etwas drin, das dampfen kann.
    //
    // Der Dampf war zuvor der eigentliche Grund, warum nichts davon als Getraenk las: zwei
    // EINZELNE, stark gedimmte Punkte, die zeitweise ganz weggefiltert wurden ("brightness > 150").
    // Jetzt drei Schwaden aus je zwei Punkten (Kopf und nachziehender Schweif), versetzt in der
    // Phase und seitlich schwingend - sichtbar aufsteigender Dampf statt zweier wandernder Pixel.
    //
    // Bewusst identisch zur Fassung im :app-sim-Modul gehalten (siehe dortige ReminderAnimations):
    // es ist dieselbe Erinnerung, sie darf auf dem echten Glyph-Raster nicht anders aussehen als
    // im Simulator.
    private const val MUG_LEFT = 3
    private const val MUG_RIGHT = 9
    private const val MUG_TOP = 6
    private const val MUG_BOTTOM = 9

    private fun mugOutline() =
        (MUG_LEFT..MUG_RIGHT).map { it to MUG_TOP } +
            (MUG_LEFT..MUG_RIGHT).map { it to MUG_BOTTOM } +
            listOf(MUG_LEFT to 7, MUG_RIGHT to 7, MUG_LEFT to 8, MUG_RIGHT to 8) +
            // Henkel: Bogen nach aussen, der den Rand oben und unten diagonal beruehrt.
            listOf(10 to 7, 11 to 8, 10 to 9)

    private fun restFrames(size: Int): List<IntArray> {
        val mug = mugOutline()
        val saucer = (MUG_LEFT - 1..MUG_RIGHT + 1).map { (it to MUG_BOTTOM + 1) to (MAX_BRIGHTNESS / 3) }
        val liquid = (MUG_LEFT + 1 until MUG_RIGHT).map { (it to MUG_TOP + 1) to (MAX_BRIGHTNESS / 3) }
        // (Basis-x, Phasenversatz) - drei Schwaden gleichmaessig ueber die Tassenbreite verteilt
        // und ueber den Zyklus versetzt, damit immer irgendwo Dampf steht.
        val steamWisps = listOf(4 to 0f, 6 to 0.33f, 8 to 0.66f)
        val frameCount = 24

        return (0 until frameCount).map { i ->
            val dim = (saucer + liquid).toMutableList()
            for ((baseX, phaseOffset) in steamWisps) {
                val phase = (i / frameCount.toFloat() + phaseOffset) % 1f
                // Kopf steigt von direkt ueber dem Tassenrand bis aus dem Bild heraus.
                val headY = MUG_TOP - 1 - (phase * MUG_TOP).toInt()
                val sway = (1.4f * sin((phase * 2 * Math.PI + baseX).toFloat())).roundToInt()
                val headBrightness = (MAX_BRIGHTNESS * (1f - phase) * 0.8f).toInt()
                // Kopf plus nachziehender Schweif eine Zeile darunter, schwaecher und weniger weit
                // ausgelenkt - das macht aus dem Punkt eine Straehne.
                val wisp = listOf(
                    Triple(headY, sway, headBrightness),
                    Triple(headY + 1, sway / 2, headBrightness / 2)
                )
                for ((y, dx, brightness) in wisp) {
                    if (y in 0 until MUG_TOP && brightness > 120) {
                        dim += (baseX + dx to y) to brightness
                    }
                }
            }
            buildFrame(size, mug, dim)
        }
    }

    // ---- WORK: Laptop mit einem im Bildschirm hochzaehlenden Ziffernblock (1 -> 2 -> 3 ->
    // 1 ...) statt Aktenkoffer - ein Koffer war neben REST/BOOK/etc. nur ein weiteres
    // Rechteck und hob sich kaum ab. Nutzt fast die volle Rastergroesse (Bildschirm +
    // zweistufige Tastatur-Basis mit breiterer Vorderkante): der Bildschirm ist dadurch
    // 5 Zeilen statt nur 4 hoch, erst dadurch passen "2" und "3" wirklich unterscheidbar
    // hinein (bei nur 4 Zeilen war "3" nicht von "1"/"2" zu unterscheiden) ----

    private fun laptopOutline() = listOf(
        3 to 3, 4 to 3, 5 to 3, 6 to 3, 7 to 3, 8 to 3, 9 to 3,
        3 to 4, 3 to 5, 3 to 6, 3 to 7, 3 to 8, 9 to 4, 9 to 5, 9 to 6, 9 to 7, 9 to 8,
        3 to 9, 4 to 9, 5 to 9, 6 to 9, 7 to 9, 8 to 9, 9 to 9,
        2 to 10, 3 to 10, 4 to 10, 5 to 10, 6 to 10, 7 to 10, 8 to 10, 9 to 10, 10 to 10,
        1 to 11, 2 to 11, 3 to 11, 4 to 11, 5 to 11, 6 to 11, 7 to 11, 8 to 11, 9 to 11, 10 to 11, 11 to 11
    )

    // Ziffern in einer 3 (breit, Spalten 5-7) x 5 (hoch, Zeilen 4-8) Bildschirm-Flaeche
    private fun screenDigitOne() = listOf(
        6 to 4, 5 to 5, 6 to 5, 6 to 6, 6 to 7, 5 to 8, 6 to 8, 7 to 8
    )
    private fun screenDigitTwo() = listOf(
        5 to 4, 6 to 4, 7 to 4, 7 to 5, 5 to 6, 6 to 6, 7 to 6, 5 to 7, 5 to 8, 6 to 8, 7 to 8
    )
    private fun screenDigitThree() = listOf(
        5 to 4, 6 to 4, 7 to 4, 7 to 5, 5 to 6, 6 to 6, 7 to 6, 7 to 7, 5 to 8, 6 to 8, 7 to 8
    )

    private fun workScreenDigits() = listOf(screenDigitOne(), screenDigitTwo(), screenDigitThree(), screenDigitOne())

    private fun laptopFrame(digit: List<Pair<Int, Int>>) = laptopOutline() + digit

    // ---- MINDFULNESS: sitzende Meditationsfigur. Kopf und Oberkoerper bleiben fest an
    // derselben Stelle, aber der Schneidersitz ist jetzt ein sichtbares Beinpaar (Huefte ->
    // Knie aussen -> Fuss zur Mitte gekreuzt) mit einer dunklen Luecke im Schoss dazwischen,
    // statt eines massiven Dreiecks ohne erkennbare Beine. Die Knie ruecken zusaetzlich
    // leicht nach aussen/innen (kneeShift), synchron zur Aura ueber dem Kopf -> die ganze
    // Figur "atmet" mit, nicht nur der Kopf ----

    private fun yogaHead() = listOf(
        5 to 3, 6 to 3, 7 to 3, 5 to 4, 6 to 4, 7 to 4
    )

    private fun yogaTorso() = listOf(6 to 5, 5 to 6, 6 to 6, 7 to 6)

    private fun yogaLegs(kneeShift: Int) = listOf(
        4 to 7, 5 to 7, 7 to 7, 8 to 7, // Huefte/Oberschenkel (Luecke bei 6,7 = Schoss)
        3 - kneeShift to 8, 9 + kneeShift to 8, // Knie, ruecken leicht aus-/einwaerts
        4 to 8, 8 to 8, // Oberschenkel zum Knie
        4 to 9, 5 to 9, 6 to 9, 7 to 9, 8 to 9 // gekreuzte Waden/Fuesse, Fussspitzen aussen
    )

    private fun yogaSequence(): List<Pair<List<Pair<Int, Int>>, Int>> {
        val none = emptyList<Pair<Int, Int>>()
        val one = listOf(6 to 1)
        val three = listOf(5 to 1, 6 to 1, 7 to 1)
        return listOf(
            none to 0, one to 0, three to 1, one to 0, none to 0, one to 1
        )
    }

    private fun yogaFrame(aura: List<Pair<Int, Int>>, kneeShift: Int) =
        yogaHead() + yogaTorso() + yogaLegs(kneeShift) + aura

    // ---- LOVE: Herzschlag gross -> klein -> mittel -> klein ("lub-dub") ----

    private fun heartBig() = listOf(
        4 to 3, 5 to 3, 7 to 3, 8 to 3,
        3 to 4, 4 to 4, 5 to 4, 6 to 4, 7 to 4, 8 to 4, 9 to 4,
        3 to 5, 4 to 5, 5 to 5, 6 to 5, 7 to 5, 8 to 5, 9 to 5,
        4 to 6, 5 to 6, 6 to 6, 7 to 6, 8 to 6,
        5 to 7, 6 to 7, 7 to 7,
        6 to 8
    )

    private fun heartMedium() = listOf(
        4 to 3, 5 to 3, 7 to 3, 8 to 3,
        4 to 4, 5 to 4, 6 to 4, 7 to 4, 8 to 4,
        4 to 5, 5 to 5, 6 to 5, 7 to 5, 8 to 5,
        4 to 6, 5 to 6, 6 to 6, 7 to 6, 8 to 6,
        5 to 7, 6 to 7, 7 to 7,
        6 to 8
    )

    private fun heartSmall() = listOf(
        4 to 4, 5 to 4, 7 to 4, 8 to 4,
        4 to 5, 5 to 5, 6 to 5, 7 to 5, 8 to 5,
        5 to 6, 6 to 6, 7 to 6,
        6 to 7
    )

    // ---- SLEEP: Der Mond selbst pulsiert durchgehend zwischen einer duennen und einer
    // vollen, gefuellten Sichel (wie ein atmendes Mondlicht/Mondphasen-Wechsel) statt nur
    // als starres Requisit dazustehen, waehrend rechts zwei Sterne "twinkeln". Die Sichel-
    // Kontur ist per Kreisformel (Hauptkreis minus versetzter "Biss"-Kreis) berechnet statt
    // von Hand gezeichnet - eine handgezeichnete Diagonale wirkte auf der linken/aeusseren
    // Kante eckig statt rund ----

    private fun moonThin() = listOf(
        5 to 2, 6 to 2, 3 to 3, 4 to 3, 3 to 4, 4 to 4, 2 to 5, 3 to 5, 2 to 6, 3 to 6,
        2 to 7, 3 to 7, 3 to 8, 4 to 8, 3 to 9, 4 to 9, 5 to 10, 6 to 10
    )

    private fun moonMedium() = listOf(
        5 to 2, 6 to 2, 7 to 2, 3 to 3, 4 to 3, 5 to 3, 3 to 4, 4 to 4,
        2 to 5, 3 to 5, 4 to 5, 2 to 6, 3 to 6, 4 to 6, 2 to 7, 3 to 7, 4 to 7,
        3 to 8, 4 to 8, 3 to 9, 4 to 9, 5 to 9, 5 to 10, 6 to 10, 7 to 10
    )

    private fun moonThick() = listOf(
        5 to 2, 6 to 2, 7 to 2, 3 to 3, 4 to 3, 5 to 3, 6 to 3, 3 to 4, 4 to 4, 5 to 4,
        2 to 5, 3 to 5, 4 to 5, 5 to 5, 2 to 6, 3 to 6, 4 to 6, 5 to 6, 2 to 7, 3 to 7, 4 to 7, 5 to 7,
        3 to 8, 4 to 8, 5 to 8, 3 to 9, 4 to 9, 5 to 9, 6 to 9, 5 to 10, 6 to 10, 7 to 10
    )

    private fun sleepFrame1() = moonThin() + listOf(9 to 3, 11 to 6)
    private fun sleepFrame2() = moonMedium() + listOf(10 to 4, 9 to 7)
    private fun sleepFrame3() = moonThick() + listOf(9 to 5, 11 to 7)
    private fun sleepFrame4() = moonMedium() + listOf(10 to 6, 9 to 8)

    // ---- MEDICINE: Medizinisches Kreuz pulsiert winzig -> klein -> gross -> klein ----

    private fun medicineTiny() = listOf(6 to 5, 6 to 6, 6 to 7, 5 to 6, 7 to 6)

    private fun medicineSmall() = listOf(
        6 to 4, 6 to 5, 6 to 6, 6 to 7, 6 to 8,
        4 to 6, 5 to 6, 7 to 6, 8 to 6
    )

    private fun medicineBig() = listOf(
        6 to 2, 6 to 3, 6 to 4, 6 to 5, 6 to 6, 6 to 7, 6 to 8, 6 to 9, 6 to 10,
        2 to 6, 3 to 6, 4 to 6, 5 to 6, 7 to 6, 8 to 6, 9 to 6, 10 to 6
    )

    // ---- BOOK: Ein Buch, das sichtbar aufklappt und dessen Text beim Lesen flackert -
    // eine stillstehende Seite mit einem kleinen Rand-Merker (fruehere Version) war fuer
    // "Lesen" nicht erkennbar genug. Nutzt fast die volle Rastergroesse: geschlossen (schmaler
    // Block, Ruecken-Ansicht) -> oeffnet sich -> ganz aufgeklappt (breite Doppelseite mit
    // Falz in der Mitte) -> Textzeilen auf beiden Seiten flackern (erscheinen/verschwinden,
    // simuliert Lesefortschritt) -> klappt wieder zu -> Schleife ----

    private fun closedBook() = listOf(
        5 to 2, 6 to 2, 7 to 2,
        5 to 3, 5 to 4, 5 to 5, 5 to 6, 5 to 7, 5 to 8, 5 to 9,
        7 to 3, 7 to 4, 7 to 5, 7 to 6, 7 to 7, 7 to 8, 7 to 9,
        5 to 10, 6 to 10, 7 to 10,
        6 to 4, 6 to 6, 6 to 8 // Ruecken-Textur
    )

    private fun openingBook() = listOf(
        4 to 2, 5 to 2, 6 to 2, 7 to 2, 8 to 2,
        4 to 3, 4 to 4, 4 to 5, 4 to 6, 4 to 7, 4 to 8, 4 to 9,
        8 to 3, 8 to 4, 8 to 5, 8 to 6, 8 to 7, 8 to 8, 8 to 9,
        4 to 10, 5 to 10, 6 to 10, 7 to 10, 8 to 10,
        6 to 3, 6 to 4, 6 to 5, 6 to 6, 6 to 7, 6 to 8, 6 to 9
    )

    private fun openBookOutline() = listOf(
        1 to 3, 2 to 3, 3 to 3, 4 to 3, 5 to 3, 6 to 3, 7 to 3, 8 to 3, 9 to 3, 10 to 3, 11 to 3,
        1 to 9, 2 to 9, 3 to 9, 4 to 9, 5 to 9, 6 to 9, 7 to 9, 8 to 9, 9 to 9, 10 to 9, 11 to 9,
        1 to 4, 1 to 5, 1 to 6, 1 to 7, 1 to 8,
        11 to 4, 11 to 5, 11 to 6, 11 to 7, 11 to 8,
        6 to 3, 6 to 4, 6 to 5, 6 to 6, 6 to 7, 6 to 8, 6 to 9 // Falz/Buchruecken in der Mitte
    )

    private fun textLeft(rows: List<Int>) = rows.flatMap { y -> listOf(2 to y, 3 to y, 4 to y, 5 to y) }
    private fun textRight(rows: List<Int>) = rows.flatMap { y -> listOf(7 to y, 8 to y, 9 to y, 10 to y) }

    private fun openBook(textRows: List<Int>) = openBookOutline() + textLeft(textRows) + textRight(textRows)

    private fun bookFrames() = listOf(
        closedBook(),
        openingBook(),
        openBook(emptyList()),
        openBook(listOf(5)),
        openBook(listOf(5, 7)),
        openBook(listOf(7)),
        openBook(listOf(5, 7)),
        openingBook()
    )

    // ---- CREATIVITY: ein Pinsel malt einen vollen Kreis, dann kurzer Funken-Abschluss ----

    private fun circleBrushPoints() = listOf(
        6 to 1, 8 to 2, 10 to 4, 11 to 6, 10 to 8, 8 to 10,
        6 to 11, 4 to 10, 2 to 8, 1 to 6, 2 to 4, 4 to 2
    )

    // Pluspoermiger Pinselkopf an der aktuellen Position, deutlich groesser als die
    // duennen Punkte der bereits gemalten Kontur dahinter
    private fun brushHead(point: Pair<Int, Int>): List<Pair<Int, Int>> {
        val (x, y) = point
        return listOf(x to y, x - 1 to y, x + 1 to y, x to y - 1, x to y + 1)
    }

    private fun creativityFrames(size: Int): List<IntArray> {
        val points = circleBrushPoints()
        val drawingKeyframes = points.indices.map { index -> points.subList(0, index) + brushHead(points[index]) }
        val frames = FrameCrossfade.withCrossfades(size, drawingKeyframes, steps = 1, loop = false).toMutableList()

        // Kurzer Funken-Abschluss in der Mitte, sobald der Kreis fertig ist.
        val center = 6 to 6
        frames += buildFrame(size, points, dim = listOf(center to MAX_BRIGHTNESS))
        frames += toGrid(size, points + listOf(5 to 6, 7 to 6, 6 to 5, 6 to 7, center))
        frames += buildFrame(size, points, dim = listOf(center to (MAX_BRIGHTNESS / 2)))

        // Vor dem naechsten Durchlauf sanft ausblenden statt hart zu verschwinden.
        val fadeSteps = 4
        for (step in 1..fadeSteps) {
            val brightness = (MAX_BRIGHTNESS * (1f - step.toFloat() / (fadeSteps + 1))).toInt()
            frames += buildFrame(size, emptyList(), dim = points.map { it to brightness })
        }
        return frames
    }
}
