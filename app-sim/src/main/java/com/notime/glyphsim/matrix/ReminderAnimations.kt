package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.FrameCrossfade
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Baut Roh-Pixel-Frames (ein Helligkeitswert 0..4095 pro Zelle) je Erinnerungs-Typ.
 * Urspruenglich 1:1 aus glyph/ReminderAnimations.kt im :app-Modul uebernommen: dort
 * inzwischen nur noch die Basis-Posen (Handpositionen etc.), hier deutlich mehr
 * Frames pro Animation - per Ueberblendung zwischen Posen ([FrameCrossfade.withCrossfades]) oder,
 * bei DRINK/REST, komplett eigens als durchgaengige kleine Erzaehlung gebaut (Glas
 * fuellt sich tropfenweise und wird geleert; Dampf steigt echt auf und verblasst,
 * statt in einem Zickzack zu "springen").
 */
object ReminderAnimations {

    fun framesFor(type: AnimationType): List<IntArray> = when (type) {
        AnimationType.MOVE -> FrameCrossfade.withCrossfades(SIZE,
            listOf(moveContactA(), moveFlight(), moveContactB(), moveFlight())
        )
        AnimationType.DRINK -> drinkFrames()
        AnimationType.FOCUS -> FrameCrossfade.withCrossfades(SIZE,
            listOf(
                focusReticle(ringSmall()), focusReticle(ringMedium()),
                focusReticle(ringBig()), focusReticle(ringMedium())
            )
        )
        // steps = 0: keine Helligkeits-Ueberblendung zwischen den drei Glocken-Posen - dieselbe
        // Behebung, die "traege, verwaschene" Bewegung schon bei AvatarAnimations behoben hat
        // (siehe MatrixAnimator.AVATAR_FRAME_DELAY_MS-Klassendoku). Vorher lief die Standard-
        // Ueberblendung (steps=2) mit, wodurch die Glocke nie richtig scharf zu sehen war,
        // sondern staendig zwischen zwei Positionen verwischte.
        AnimationType.GENERAL -> FrameCrossfade.withCrossfades(
            SIZE,
            listOf(bellLeft(), bellCenter(), bellRight(), bellCenter()),
            steps = 0
        )
        AnimationType.REST -> restFrames()
        AnimationType.WORK -> FrameCrossfade.withCrossfades(SIZE,workScreenDigits().map { laptopFrame(it) })
        AnimationType.MINDFULNESS -> FrameCrossfade.withCrossfades(SIZE,
            yogaSequence().map { (aura, kneeShift) -> yogaFrame(aura, kneeShift) }
        )
        AnimationType.LOVE -> FrameCrossfade.withCrossfades(SIZE,listOf(heartBig(), heartSmall(), heartMedium(), heartSmall()))
        AnimationType.SLEEP -> FrameCrossfade.withCrossfades(SIZE,listOf(sleepFrame1(), sleepFrame2(), sleepFrame3(), sleepFrame4()))
        AnimationType.MEDICINE -> FrameCrossfade.withCrossfades(SIZE,listOf(medicineTiny(), medicineSmall(), medicineBig(), medicineSmall()))
        AnimationType.BOOK -> FrameCrossfade.withCrossfades(SIZE,bookFrames(), steps = 1)
        AnimationType.CREATIVITY -> creativityFrames()
    }

    private const val MAX_BRIGHTNESS = MatrixGeometry.MAX_BRIGHTNESS
    private const val SIZE = MatrixGeometry.SIZE

    private fun toGrid(size: Int, points: List<Pair<Int, Int>>): IntArray {
        val grid = IntArray(size * size)
        for ((x, y) in points) {
            if (x in 0 until size && y in 0 until size) grid[y * size + x] = MAX_BRIGHTNESS
        }
        return grid
    }

    /** Frame aus voll hellen Punkten plus optionalen gedimmten Akzenten (Schatten/Glanz/Dampf). */
    private fun buildFrame(full: List<Pair<Int, Int>>, dim: List<Pair<Pair<Int, Int>, Int>> = emptyList()): IntArray {
        val grid = IntArray(SIZE * SIZE)
        fun set(x: Int, y: Int, brightness: Int) {
            if (x in 0 until SIZE && y in 0 until SIZE) grid[y * SIZE + x] = brightness
        }
        for ((point, brightness) in dim) set(point.first, point.second, brightness)
        for ((x, y) in full) set(x, y, MAX_BRIGHTNESS)
        return grid
    }

    // ---- Ueberblendung: aus wenigen Posen viele weiche Zwischenframes machen, siehe FrameCrossfade ----

    // ---- MOVE: Strichmaennchen-Laufzyklus ----

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

    private fun drinkFrame(filledRows: List<Int>, dropletY: Int? = null, splash: Boolean = false): IntArray {
        val points = glassOutline().toMutableList()
        filledRows.forEach { points += waterRow(it) }
        dropletY?.let { points += 6 to it }
        if (splash) {
            val splashY = ((filledRows.minOrNull() ?: GLASS_BOTTOM) - 1).coerceIn(GLASS_TOP, GLASS_BOTTOM)
            points += listOf(5 to splashY, 7 to splashY)
        }
        return toGrid(SIZE, points)
    }

    private fun drinkFrames(): List<IntArray> {
        val frames = mutableListOf<IntArray>()
        frames += drinkFrame(emptyList())
        frames += drinkFrame(emptyList())

        val filled = mutableListOf<Int>()
        for (row in WATER_ROWS) {
            val top = 1
            val bottom = (row - 1).coerceAtLeast(top)
            val fallSteps = if (bottom > top) listOf(top, (top + bottom) / 2, bottom) else listOf(top, bottom)
            fallSteps.forEach { y -> frames += drinkFrame(filled.toList(), dropletY = y) }
            frames += drinkFrame(filled.toList(), splash = true)
            filled += row
            frames += drinkFrame(filled.toList())
        }

        frames += drinkFrame(filled.toList())
        frames += drinkFrame(filled.toList())

        val remaining = filled.toMutableList()
        for (row in WATER_ROWS.reversed()) {
            remaining.remove(row)
            frames += drinkFrame(remaining.toList())
        }
        frames += drinkFrame(emptyList())
        return frames
    }

    // ---- FOCUS: Kamera-Fokus-Sucher ----

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

    // ---- GENERAL: Glocke schwingt wie ein Pendel ----

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

    private fun restFrames(): List<IntArray> {
        val mug = mugOutline()
        val saucer = (MUG_LEFT - 1..MUG_RIGHT + 1).map { (it to MUG_BOTTOM + 1) to (MAX_BRIGHTNESS / 3) }
        val liquid = (MUG_LEFT + 1 until MUG_RIGHT).map { (it to MUG_TOP + 1) to (MAX_BRIGHTNESS / 3) }
        // (Basis-x, Phasenversatz) - drei Schwaden gleichmaessig ueber die Tassenbreite verteilt
        // und ueber den Zyklus versetzt, damit immer irgendwo Dampf steht.
        val steamWisps = listOf(4 to 0f, 6 to 0.33f, 8 to 0.66f)
        // 24 Frames * CLOCK_FRAME_DELAY_MS ergibt knapp drei Sekunden je Zyklus - langsam genug,
        // dass der Dampf wirklich steigt, statt zu flackern.
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
            buildFrame(mug, dim)
        }
    }

    // ---- WORK: Laptop mit hochzaehlender Ziffer im Bildschirm ----

    private fun laptopOutline() = listOf(
        3 to 3, 4 to 3, 5 to 3, 6 to 3, 7 to 3, 8 to 3, 9 to 3,
        3 to 4, 3 to 5, 3 to 6, 3 to 7, 3 to 8, 9 to 4, 9 to 5, 9 to 6, 9 to 7, 9 to 8,
        3 to 9, 4 to 9, 5 to 9, 6 to 9, 7 to 9, 8 to 9, 9 to 9,
        2 to 10, 3 to 10, 4 to 10, 5 to 10, 6 to 10, 7 to 10, 8 to 10, 9 to 10, 10 to 10,
        1 to 11, 2 to 11, 3 to 11, 4 to 11, 5 to 11, 6 to 11, 7 to 11, 8 to 11, 9 to 11, 10 to 11, 11 to 11
    )

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

    // ---- MINDFULNESS: sitzende Meditationsfigur ----

    private fun yogaHead() = listOf(
        5 to 3, 6 to 3, 7 to 3, 5 to 4, 6 to 4, 7 to 4
    )

    private fun yogaTorso() = listOf(6 to 5, 5 to 6, 6 to 6, 7 to 6)

    private fun yogaLegs(kneeShift: Int) = listOf(
        4 to 7, 5 to 7, 7 to 7, 8 to 7,
        3 - kneeShift to 8, 9 + kneeShift to 8,
        4 to 8, 8 to 8,
        4 to 9, 5 to 9, 6 to 9, 7 to 9, 8 to 9
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

    // ---- LOVE: Herzschlag gross -> klein -> mittel -> klein ----

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

    // ---- SLEEP: Mond pulsiert zwischen duenner und voller Sichel, Sterne twinkeln ----

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

    // ---- MEDICINE: Kreuz pulsiert winzig -> klein -> gross -> klein ----

    private fun medicineTiny() = listOf(6 to 5, 6 to 6, 6 to 7, 5 to 6, 7 to 6)

    private fun medicineSmall() = listOf(
        6 to 4, 6 to 5, 6 to 6, 6 to 7, 6 to 8,
        4 to 6, 5 to 6, 7 to 6, 8 to 6
    )

    private fun medicineBig() = listOf(
        6 to 2, 6 to 3, 6 to 4, 6 to 5, 6 to 6, 6 to 7, 6 to 8, 6 to 9, 6 to 10,
        2 to 6, 3 to 6, 4 to 6, 5 to 6, 7 to 6, 8 to 6, 9 to 6, 10 to 6
    )

    // ---- BOOK: Buch klappt auf, Text flackert ----

    private fun closedBook() = listOf(
        5 to 2, 6 to 2, 7 to 2,
        5 to 3, 5 to 4, 5 to 5, 5 to 6, 5 to 7, 5 to 8, 5 to 9,
        7 to 3, 7 to 4, 7 to 5, 7 to 6, 7 to 7, 7 to 8, 7 to 9,
        5 to 10, 6 to 10, 7 to 10,
        6 to 4, 6 to 6, 6 to 8
    )

    private fun openingBook() = listOf(
        4 to 2, 5 to 2, 6 to 2, 7 to 2, 8 to 2,
        4 to 3, 4 to 4, 4 to 5, 4 to 6, 4 to 7, 4 to 8, 4 to 9,
        8 to 3, 8 to 4, 8 to 5, 8 to 6, 8 to 7, 8 to 8, 8 to 9,
        4 to 10, 5 to 10, 6 to 10, 7 to 10, 8 to 10,
        6 to 3, 6 to 4, 6 to 5, 6 to 6, 6 to 7, 6 to 8, 6 to 9
    )

    /**
     * Aufgeschlagenes Buch: Ober- und Unterkante plus Falz - **ohne senkrechte Seitenraender**.
     *
     * Die Raender bei x=1 und x=11 sind bewusst entfallen. Mit ihnen blieben je Seite nur vier
     * Pixel Innenraum (x=2..5 bzw. x=7..10), und jede Textzeile beruehrte zwangslaeufig sowohl
     * den Rand als auch den Falz - Zeile und Rahmen verschmolzen zu einer gefuellten Flaeche,
     * die nichts mehr mit Text zu tun hatte. Ohne sie stehen je Seite fuenf Pixel zur Verfuegung,
     * und die Zeilen haben eine Rinne zum Falz hin. Bei dieser Aufloesung liest sich ein Buch
     * ohnehin ueber Kante und Falz, nicht ueber einen geschlossenen Rahmen.
     */
    private fun openBookOutline() = listOf(
        1 to 3, 2 to 3, 3 to 3, 4 to 3, 5 to 3, 6 to 3, 7 to 3, 8 to 3, 9 to 3, 10 to 3, 11 to 3,
        1 to 9, 2 to 9, 3 to 9, 4 to 9, 5 to 9, 6 to 9, 7 to 9, 8 to 9, 9 to 9, 10 to 9, 11 to 9,
        6 to 4, 6 to 5, 6 to 6, 6 to 7, 6 to 8
    )

    /**
     * Textzeilen **unterschiedlicher Laenge**, Zeile fuer Zeile aufgebaut.
     *
     * Vorher waren es zwei gleich lange Vollbalken auf den Zeilen 5 und 7, die abwechselnd an
     * und aus gingen. Gleich lange Balken lesen sich nicht als Text, sondern als Balken - und
     * an/aus nicht als Lesen, sondern als Blinken. Jetzt stehen vier Zeilen mit ungleichen
     * Laengen zur Verfuegung, die nacheinander erscheinen: das ist die Bewegung, die man beim
     * Lesen tatsaechlich sieht.
     *
     * Zwischen den Umrisszeilen (y=3 und y=9) liegen die Zeilen 4 bis 7; x=2..5 links,
     * x=7..10 rechts.
     */
    private fun leftText(lines: Int) = listOf(
        (1..4).map { it to 4 },
        (1..3).map { it to 5 },
        (1..4).map { it to 6 },
        (1..2).map { it to 7 }
    ).take(lines.coerceIn(0, 4)).flatten()

    private fun rightText(lines: Int) = listOf(
        (8..11).map { it to 4 },
        (8..10).map { it to 5 },
        (8..11).map { it to 6 },
        (8..9).map { it to 7 }
    ).take(lines.coerceIn(0, 4)).flatten()

    private fun openBook(left: Int, right: Int) = openBookOutline() + leftText(left) + rightText(right)

    /**
     * Das Blatt auf dem Weg zur Mitte. [step] 0 = steht noch rechts, 1 = kurz vor dem Falz.
     *
     * Ohne dieses Zwischenbild sprang der Text von der einen auf die andere Seite - das sah nach
     * Bildwechsel aus, nicht nach Umblaettern.
     */
    private fun turningPage(step: Int) = when (step) {
        0 -> (4..8).map { 10 to it }
        else -> (4..8).map { 8 to it }
    }

    private fun bookFrames() = listOf(
        closedBook(),
        openingBook(),
        openBook(0, 0),
        // Lesen: Zeilen bauen sich links auf, dann rechts.
        openBook(1, 0),
        openBook(2, 0),
        openBook(3, 1),
        openBook(4, 2),
        openBook(4, 4),
        // Umblaettern - das rechte Blatt wandert zum Falz.
        openBookOutline() + leftText(4) + turningPage(0),
        openBookOutline() + leftText(4) + turningPage(1),
        // Neue Doppelseite, Text beginnt von vorn.
        openBook(1, 0),
        openBook(3, 2),
        // Zuklappen.
        openingBook(),
        closedBook()
    )

    // ---- CREATIVITY: Pinsel malt einen vollen Kreis, dann kurzer Funken-Abschluss ----

    private fun circleBrushPoints() = listOf(
        6 to 1, 8 to 2, 10 to 4, 11 to 6, 10 to 8, 8 to 10,
        6 to 11, 4 to 10, 2 to 8, 1 to 6, 2 to 4, 4 to 2
    )

    private fun brushHead(point: Pair<Int, Int>): List<Pair<Int, Int>> {
        val (x, y) = point
        return listOf(x to y, x - 1 to y, x + 1 to y, x to y - 1, x to y + 1)
    }

    private fun creativityFrames(): List<IntArray> {
        val points = circleBrushPoints()
        val drawingKeyframes = points.indices.map { index -> points.subList(0, index) + brushHead(points[index]) }
        val frames = FrameCrossfade.withCrossfades(SIZE,drawingKeyframes, steps = 1, loop = false).toMutableList()

        // Kurzer Funken-Abschluss in der Mitte, sobald der Kreis fertig ist.
        val center = 6 to 6
        frames += buildFrame(points, dim = listOf(center to MAX_BRIGHTNESS))
        frames += toGrid(SIZE, points + listOf(5 to 6, 7 to 6, 6 to 5, 6 to 7, center))
        frames += buildFrame(points, dim = listOf(center to (MAX_BRIGHTNESS / 2)))

        // Vor dem naechsten Durchlauf sanft ausblenden statt hart zu verschwinden.
        val fadeSteps = 4
        for (step in 1..fadeSteps) {
            val brightness = (MAX_BRIGHTNESS * (1f - step.toFloat() / (fadeSteps + 1))).toInt()
            frames += buildFrame(emptyList(), dim = points.map { it to brightness })
        }
        return frames
    }
}
