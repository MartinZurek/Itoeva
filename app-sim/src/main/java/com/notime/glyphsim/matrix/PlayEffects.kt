package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Was der Avatar TRAEGT und was aufblitzt, wenn er etwas ANFASST - die beiden Ebenen, die aus
 * einer Bewegung eine erkennbare Handlung machen.
 *
 * **Das Problem, das hier geloest wird.** Bis hierher ging der Avatar zum Kuehlschrank, machte
 * eine Streck-Bewegung und ging zum Tisch. Was dazwischen passiert sein sollte - er nimmt sich
 * etwas heraus und traegt es hinueber - war reine Behauptung: Auf dem Bildschirm sprang eine
 * Figur, und man musste raten, was sie vorhat. Ein sichtbarer Gegenstand in der Hand und ein
 * kurzes Aufblitzen im Moment des Zugriffs beantworten beides ohne ein einziges Wort.
 *
 * Beides wird als Szenen-Zellen ausgegeben ([SceneCell]) und in derselben Ebene gezeichnet wie
 * die Kulisse - nur eben VOR dem Avatar. Dadurch teilen Figur, Welt und Gegenstand ein einziges
 * Raster, und nichts davon braucht einen eigenen Zeichenweg.
 */
object PlayEffects {

    /**
     * Die Breite, mit der beim Seitenwechsel gerechnet wird - die des breitesten Motivs.
     *
     * Bewusst nicht die des jeweiligen Motivs: Ein Motiv, das waehrend seines Ablaufs waechst,
     * wuerde sonst mitten in der Bewegung von rechts nach links springen.
     */
    private const val MOTIF_WIDTH = 13

    /** Was sich tragen laesst. Bewusst wenige, klar unterscheidbare Formen - auf drei mal drei
     *  Zellen ist alles darueber hinaus nicht mehr auseinanderzuhalten. */
    enum class Carried { BOOK, FOOD, CUP, GUITAR, EASEL }

    /** Lesbare Phasen einer Drachen-Szene: auspacken, hochziehen, fliegen, einholen. */
    enum class KitePhase { PREPARE, LAUNCH, FLY, LAND }

    /** Ballkontrolle, Schuss und der spaeter erlernbare Spezialtrick. */
    enum class FootballPhase { DRIBBLE, AIM, KICK, TRICK }

    /** Basketball vom Prellen bis zum Treffer. */
    enum class BasketballPhase { DRIBBLE, AIM, SHOOT, SCORE }

    /** Krafttraining mit einer Hantel. */
    enum class TrainingPhase { WARM_UP, LIFT, REST }

    /** Gitarre stimmen, spielen und mit einem Akkord enden. */
    enum class MusicPhase { TUNE, PLAY, FINALE }

    /** Ein Bild von der Skizze bis zur fertigen Leinwand. */
    enum class PaintingPhase { SKETCH, PAINT, REVEAL }

    /** Werfen, Warten und der Fang - die drei sichtbaren Phasen einer Angel-Szene am Teich. */
    enum class FishingPhase { CAST, WAIT, CATCH }

    /**
     * Ein kleines, bewegtes Weltzeichen fuer jede Haupttaetigkeit.
     *
     * **Es ist kein Symbol neben der Figur, sondern ein Gegenstand in ihrer Welt.** Der
     * Unterschied ist nicht Groesse, sondern Zugehoerigkeit: Der Gegenstand steht in derselben
     * Tonwertordnung wie die Kulisse ([PlayInk]), bekommt sein Licht aus derselben Richtung,
     * wirft einen Schatten auf denselben Boden und ist mit einer schwarzen Kante aus dem
     * Hintergrund freigestellt. Erst das macht aus einem hellen Fleck ein Ding.
     *
     * **Warum die Motive kleiner geworden sind.** Die vorige Fassung zeichnete bis zu 18 Zellen
     * breite Requisiten - breiter als die Figur selbst. Auf dem Kontaktbogen war die Folge nicht
     * Deutlichkeit, sondern das Gegenteil: Das Motiv lief durch die halbe Kulisse, ueberdeckte
     * Moebel, kreuzte den Bodenstrich und lief unten aus der Welt heraus. Ein Gegenstand wird
     * nicht dadurch erkennbar, dass er gross ist, sondern dadurch, dass er eine geschlossene
     * Silhouette hat und ringsum Luft.
     */
    fun activityCells(
        topic: AnimationType,
        avatarCellX: Int,
        avatarCellY: Int,
        scenePhase: Int,
        widthCells: Int
    ): List<SceneCell> {
        // Dieselbe Herleitung des Bodens wie bei den grossen Szenen weiter unten - die unterste
        // Koerperzeile der Figur IST die Standlinie.
        val groundY = avatarCellY + AvatarGeometry.HEIGHT - 1
        val floorY = groundY + 1
        val beat = (scenePhase / 3) % 4

        // Zur rechten Seite, solange das ganze Motiv dort Platz hat - sonst gespiegelt nach links.
        // Gemessen wird mit der groessten vorkommenden Motivbreite, nicht mit der jeweiligen:
        // sonst spraenge ein Motiv beim Wachsen mitten im Ablauf auf die andere Seite.
        val useRight = avatarCellX + AvatarGeometry.SIZE + 2 + MOTIF_WIDTH <= widthCells
        val direction = if (useRight) 1 else -1
        val ox = if (useRight) avatarCellX + AvatarGeometry.SIZE + 2 else avatarCellX - 3

        /** Eine Zeichenflaeche, deren unterste Zeile [lift] Zellen ueber dem Boden liegt. */
        fun place(height: Int, lift: Int = 0) =
            PlayInk.Sketch(ox, groundY - height + 1 - lift, direction, widthCells, floorY)

        return when (topic) {
            AnimationType.SLEEP -> {
                // Sichel und aufsteigende Z. Der Mond wird freigestellt, die Z ausdruecklich
                // NICHT: Sie sind kein Gegenstand, sondern ein Laut - eine schwarze Kante um sie
                // herum machte harte Aufkleber daraus.
                val moon = place(height = 6, lift = 9)
                moon.art(
                    6, 0,
                    " ###",
                    "##  ",
                    "##  ",
                    "##  ",
                    "##  ",
                    " ###"
                )
                moon.spark(7, 1)
                // Die Z steigen LINKS neben dem Mond auf, nicht durch ihn hindurch: Zwei
                // luftige Zeichen, die sich ueberlagern, ergeben keins von beiden.
                val breath = place(height = 13, lift = 3)
                for (i in 0..2) {
                    val drift = (beat + i) % 2
                    val y = 9 - i * 4 - drift
                    val level = if (i == beat % 3) PlayInk.SPARK else PlayInk.EDGE
                    breath.stamp(drift, y, level, "###", "  #", " # ", "###")
                }
                moon.render(grounded = false) + breath.render(carve = false)
            }

            AnimationType.REST -> {
                // Tasse mit Henkel, dazu Dampf, der nach oben duenner wird.
                val cup = place(height = 7)
                cup.art(
                    0, 0,
                    "######   ",
                    "#++++#   ",
                    "#++++####",
                    "#++++#  #",
                    "#++++####",
                    "#++++#   ",
                    " ####    "
                )
                cup.spark(1, 1)
                val steam = place(height = 6, lift = 7)
                for (i in 0..2) {
                    val x = 1 + i * 2
                    val sway = ((beat + i) % 3) - 1
                    val level = if (i == 1) PlayInk.EDGE else PlayInk.DETAIL
                    for (y in 0..4) {
                        steam.dot(x + if ((y + beat) % 2 == 0) sway else 0, 5 - y, level)
                    }
                }
                cup.render(grounded = true) + steam.render(carve = false)
            }

            AnimationType.BOOK -> {
                // Aufgeschlagenes Buch: ZWEI HOHLE Seiten (nur der Rahmen auf BODY, keine
                // gefuellte Flaeche) mit einer Textzeile, die pro Takt dazukommt - dieselbe Idee
                // wie bei der Schriftrolle (AvatarSignatureAnimations.scrollFrames): "es passiert
                // gerade etwas mit dem Text" liest sich ueberzeugender als jede Silhouette allein,
                // und eine leere, gefuellte Flaeche wirkt aus der Distanz nur wie ein Klotz.
                val book = place(height = 6)
                book.art(
                    0, 0,
                    "  ###   ###  ",
                    " ##### ##### ",
                    " #   # #   # ",
                    " #   # #   # ",
                    " #   # #   # ",
                    " ##### ##### "
                )
                for (line in 0 until beat) {
                    val y = 2 + line
                    book.line(2, y, 4, y, PlayInk.DETAIL)
                    book.line(8, y, 10, y, PlayInk.DETAIL)
                }
                // Die umblaetternde Seite hebt sich UEBER die Oberkante des Buches, genau an der
                // Luecke - im letzten Takt, wenn auch die letzte Zeile steht.
                if (beat == 3) {
                    book.dot(6, -1, PlayInk.BODY)
                    book.dot(7, -2, PlayInk.BODY)
                }
                book.render(grounded = true)
            }

            AnimationType.MINDFULNESS -> {
                // Atemringe um einen ruhigen Kern. Nichts davon ist Materie, also keine
                // Freistellung und kein Schatten - es soll durch die Welt hindurchscheinen.
                val rings = place(height = 13, lift = 3)
                val cx = 6
                val cy = 6
                val open = PlayInk.swing(scenePhase, 12)
                rings.spark(cx, cy)
                for (step in 0..2) {
                    val r = 2 + step * 2 + (open * 2).toInt()
                    if (r > 6) continue
                    // Als Licht gezeichnet, nicht als Materie: Atem hat keinen Umriss, also
                    // trennt er sich ueber die Helligkeit. Auf DETAIL laege er genau dort, wo
                    // auch Regale und Moebel liegen, und verschwaende vor ihnen.
                    val level = PlayInk.EDGE
                    for (i in 0 until r) {
                        rings.dot(cx + r - i, cy - i, level)
                        rings.dot(cx - i, cy - r + i, level)
                        rings.dot(cx - r + i, cy + i, level)
                        rings.dot(cx + i, cy + r - i, level)
                    }
                }
                rings.render(carve = false)
            }

            AnimationType.LOVE -> {
                // Ein Herz, das wirklich schlaegt: zwei Takte gross, zwei klein, und die
                // Bodenlinie darunter bleibt frei, damit es schwebt.
                val big = beat % 2 == 0
                val heart = place(height = if (big) 8 else 7, lift = if (big) 5 else 6)
                if (big) {
                    heart.art(
                        0, 0,
                        " ##   ## ",
                        "#########",
                        "#########",
                        "#########",
                        " ####### ",
                        "  #####  ",
                        "   ###   ",
                        "    #    "
                    )
                } else {
                    heart.art(
                        1, 0,
                        " #   #  ",
                        "####### ",
                        "####### ",
                        " ##### ",
                        "  ###  ",
                        "   #   "
                    )
                }
                heart.spark(2, 1)
                heart.render()
            }

            AnimationType.DRINK -> {
                // Glas mit steigendem Pegel und einem Tropfen, der sichtbar bis zum Rand faellt
                // und dort spritzt. Der Tropfen liegt in DENSELBEN lokalen Koordinaten wie das
                // Glas (nicht in einem eigenen, weit darueber schwebenden Sketch) - sonst haengt
                // er sichtbar in der Luft, ohne dass er je etwas beruehrt.
                val glass = place(height = 8)
                glass.art(
                    0, 0,
                    "#    #",
                    "#    #",
                    "#    #",
                    "#    #",
                    "#    #",
                    "#    #",
                    " #  # ",
                    " #### "
                )
                // Nur bis Zeile 5: darunter zieht sich das Glas zusammen, und eine volle Zeile
                // dort loeschte genau die Verjuengung, an der man ein Glas erkennt.
                val level = 1 + beat
                for (y in (6 - level).coerceAtLeast(1)..5) glass.line(1, y, 4, y, PlayInk.DETAIL)
                glass.spark(1, 1)
                // Der Tropfen naehert sich ueber die vier Takte dem Rand (y=-4 bis y=-1) und
                // spritzt im letzten Takt sichtbar auf - erst das macht aus "ein Punkt schwebt"
                // ein "etwas faellt und trifft".
                val dropY = -4 + beat
                glass.dot(2, dropY, PlayInk.SPARK)
                glass.dot(2, dropY + 1, PlayInk.EDGE)
                if (beat == 3) {
                    glass.dot(1, 0, PlayInk.EDGE)
                    glass.dot(3, 0, PlayInk.EDGE)
                    glass.spark(2, 0)
                }
                glass.render(grounded = true)
            }

            AnimationType.MEDICINE -> {
                // Kapsel, quer, mit sichtbarer Trennnaht und einem wandernden Glanzpunkt.
                // Die Kapsel hebt und senkt sich. Ein Glanzpunkt allein genuegt nicht: Er liegt
                // INNERHALB der Silhouette, und was sich nur dort aendert, bewegt nichts.
                val bob = (PlayInk.swing(scenePhase, 12) * 2).toInt()
                val pill = place(height = 5, lift = 6 + bob)
                pill.art(
                    0, 0,
                    " ###### ",
                    "#+++####",
                    "#+++####",
                    "#+++####",
                    " ###### "
                )
                // Ueber alle vier Takte, nicht ueber drei: Sonst steht der Glanzpunkt im
                // ersten und im letzten Takt an derselben Stelle und die Kapsel wirkt tot.
                pill.spark(1 + beat, 1)
                pill.render()
            }

            AnimationType.WORK -> {
                // Aufgeklappter Rechner: Mattscheibe mit Zeilen, blinkender Schreibmarke und
                // einer Tastatur, die als eigene Flaeche darunter liegt.
                val laptop = place(height = 8)
                laptop.art(
                    0, 0,
                    "##########",
                    "#++++++++#",
                    "#++++++++#",
                    "#++++++++#",
                    "#++++++++#",
                    "##########",
                    "##########",
                    " ######## "
                )
                for ((row, len) in listOf(2 to 5, 3 to 7)) {
                    laptop.line(2, row, 1 + len, row, PlayInk.DETAIL)
                }
                // Die unterste Zeile waechst, die Schreibmarke steht an ihrem Ende: geschrieben
                // wird gerade jetzt.
                val typed = 2 + beat
                laptop.line(2, 4, 1 + typed, 4, PlayInk.DETAIL)
                laptop.spark(2 + typed, 4)
                // Ein Zeichen, das den Umriss VERLAESST. Alles, was sich nur innerhalb der
                // Mattscheibe aendert, laesst die Silhouette unberuehrt - und eine Silhouette,
                // die sich nie ruehrt, wirkt aus zwei Metern Abstand wie ein Standbild, egal wie
                // fleissig es darin blinkt.
                val signal = place(height = 4, lift = 8)
                for (i in 0..1) {
                    val step = (beat + i * 2) % 4
                    if (step > 2) continue
                    signal.dot(3 + i * 3, 2 - step, if (step == 0) PlayInk.SPARK else PlayInk.EDGE)
                }
                laptop.render(grounded = true) + signal.render(carve = false)
            }

            AnimationType.FOCUS -> {
                // Zielscheibe auf einem Fuss; der Pfeil kommt von aussen und steckt im vierten
                // Takt in der Mitte. Anlauf und Treffer statt gleichmaessigem Wandern.
                val target = place(height = 11)
                val cx = 5
                val cy = 4
                // Aussen Material, innen Binnenzeichnung: Zwei Ringe auf derselben Stufe
                // verschmelzen zu einer gefuellten Raute, sobald ein Pfeil sie kreuzt.
                for ((ring, r) in listOf(4, 2).withIndex()) {
                    val level = if (ring == 0) PlayInk.BODY else PlayInk.DETAIL
                    for (i in 0 until r) {
                        target.dot(cx + r - i, cy - i, level)
                        target.dot(cx - i, cy - r + i, level)
                        target.dot(cx - r + i, cy + i, level)
                        target.dot(cx + i, cy + r - i, level)
                    }
                }
                target.art(
                    cx - 1, cy - 1,
                    "###",
                    "###",
                    "###"
                )
                target.line(cx, cy + 5, cx, 10, PlayInk.BODY)
                target.line(cx - 2, 10, cx + 2, 10, PlayInk.BODY)
                if (beat == 3) {
                    // Der Treffer: Der Pfeil ist weg, in der Mitte steht der Funke.
                    target.spark(cx, cy)
                } else {
                    // Anflug von aussen, immer mit Abstand zum aeusseren Ring - ein Pfeil, der
                    // die Scheibe schon beruehrt, hat nichts mehr vor.
                    val travel = 7 - beat * 2
                    target.line(cx + travel, cy - travel, cx + travel + 2, cy - travel - 2)
                    target.spark(cx + travel, cy - travel)
                }
                target.render(grounded = true)
            }

            AnimationType.MOVE -> {
                // Zwei Abdruecke auf dem Boden: der frische hell, der vorige schon verblassend.
                // Dazu zwei Tempolinien, die NICHT freigestellt werden - sie sind Luftzug.
                // Der Boden zieht vorbei, nicht die Figur: Die Abdruecke wandern Takt fuer Takt
                // nach hinten aus dem Bild. Zuvor wechselten sie nur die Helligkeit an fester
                // Stelle - das liest sich als Flackern, nicht als Gehen.
                val steps = place(height = 5)
                for (i in 0..2) {
                    val x = 1 + i * 4 - beat
                    if (x < 0) continue
                    val y = if (i % 2 == 0) 0 else 2
                    val level = if (i == 2) PlayInk.BODY else PlayInk.DETAIL
                    steps.stamp(x, y, level, "###", "###", " ##")
                }
                val wind = place(height = 6, lift = 5)
                for (i in 0..1) {
                    val y = 1 + i * 3
                    val from = (beat - i).coerceAtLeast(0)
                    wind.line(from, y, from + 3, y, PlayInk.DETAIL)
                }
                steps.render(carve = true) + wind.render(carve = false)
            }

            AnimationType.CREATIVITY -> {
                // Palette mit Daumenloch und Farbklecksen, dazu ein Pinsel, der sie beruehrt.
                val palette = place(height = 9, lift = 2)
                palette.art(
                    0, 0,
                    " ######## ",
                    "##########",
                    "###    ###",
                    "###    ###",
                    "##########",
                    " ######## ",
                    "  ######  "
                )
                for ((i, spot) in listOf(2 to 1, 6 to 1, 8 to 3, 3 to 5).withIndex()) {
                    palette.dot(spot.first, spot.second, PlayInk.DETAIL)
                }
                // Der leuchtende Klecks wandert, statt nur die Stufe zu wechseln: Eine reine
                // Helligkeitsaenderung an fester Stelle liest sich als Flackern, nicht als Tun.
                val wet = listOf(2 to 1, 6 to 1, 8 to 3, 3 to 5)[beat]
                palette.spark(wet.first, wet.second)
                // Der Pinsel liegt AUF der Palette und wandert darueber - vorher zeigte er nach
                // unten aus dem Motiv heraus und wurde am Boden abgeschnitten.
                // Schrittweite 12 - genau eine volle Taktfolge, damit Pinsel und Klecks
                // denselben Atem haben.
                val stroke = PlayInk.swing(scenePhase, 12)
                val brushX = 2 + (stroke * 4).toInt()
                // Der Pinsel ragt ueber die Palette hinaus - innerhalb ihrer Flaeche waere er
                // nur eine andere Farbe auf demselben Umriss.
                palette.line(brushX, 1, brushX + 2, -2)
                palette.spark(brushX + 2, -2)
                palette.render(grounded = false)
            }

            AnimationType.GENERAL -> {
                // Ein Funke, der aufblitzt und wieder zusammenfaellt - der kleine Alltagsmoment.
                // Ohne Freistellung: Licht hat keine Kante.
                // Hoch genug, dass der Funke ueber der Sitzgruppe steht: Ein Lichtzeichen ohne
                // Freistellung braucht freien Grund, sonst liest man beides als ein Muster.
                val burst = place(height = 9, lift = 6)
                val cx = 4
                val cy = 4
                // Hoechstens drei Zellen weit. Eine groessere Reichweite fuellte den halben Raum
                // mit Licht, das heller ist als jede Lampe darin - und nahm damit der Figur die
                // Aufmerksamkeit, statt einen kleinen Moment zu setzen.
                val reach = 1 + (PlayInk.swing(scenePhase, 12) * 3).toInt()
                burst.spark(cx, cy)
                for ((dx, dy) in listOf(
                    1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, -1 to 1, 1 to -1, -1 to -1
                )) {
                    // Die Diagonalen bleiben kuerzer als die Achsen - ein Stern mit vier langen
                    // und vier kurzen Strahlen liest sich als Funkeln, acht gleich lange als Rad.
                    val arme = if (dx != 0 && dy != 0) 1 else reach
                    // Ab dem ersten Ring, nicht ab dem zweiten: Sonst steht im engsten Takt
                    // nur der Kern da, und ein einzelner Punkt ist kein Funke, sondern Staub.
                    for (r in 1..arme + 1) {
                        burst.dot(
                            cx + dx * r,
                            cy + dy * r,
                            if (r <= arme) PlayInk.EDGE else PlayInk.DETAIL
                        )
                    }
                }
                burst.render(carve = false)
            }
        }
    }

    fun footballCells(
        avatarCellX: Int,
        avatarCellY: Int,
        phase: FootballPhase,
        scenePhase: Int,
        widthCells: Int
    ): List<SceneCell> {
        val groundY = avatarCellY + AvatarGeometry.HEIGHT - 1
        val floorY = groundY + 1
        val direction = if ((scenePhase / 5) % 2 == 0) 1 else -1

        // VOR der Ballposition berechnet, nicht danach: Der Schuss muss wissen, wohin er trifft.
        val goalRight = widthCells - 3
        val goalLeft = (goalRight - 12).coerceAtLeast(0)
        val goalTop = groundY - 9

        val centerX = when (phase) {
            FootballPhase.DRIBBLE -> avatarCellX + 15 + direction * 2
            FootballPhase.AIM -> avatarCellX + 17
            // Vorher avatarCellX + 23, unabhaengig vom Tor - traf es nur zufaellig, wenn die
            // Figur genau richtig stand, und lag sonst davor oder daneben. Jetzt wie beim
            // Basketballkorb: die Zielposition selbst entscheidet, nicht der Abstand zur Figur.
            FootballPhase.KICK -> (goalLeft + goalRight) / 2
            FootballPhase.TRICK -> avatarCellX + 12 + direction * 4
        }.coerceIn(2, (widthCells - 3).coerceAtLeast(2))
        val centerY = when (phase) {
            FootballPhase.DRIBBLE, FootballPhase.AIM -> groundY - 1
            // Sichtbar INNERHALB des Tors, nicht nur auf seiner Anflughoehe - sonst haengt der
            // Ball vor der Torlinie in der Luft, statt drin zu liegen.
            FootballPhase.KICK -> goalTop + 5
            FootballPhase.TRICK -> groundY - 13
        }

        // Der Ball: Fuenfecke und Naehte auf DETAIL verhindern, dass er wie ein heller Kreis
        // aussieht, statt wie ein Ball mit Struktur.
        val ball = PlayInk.Sketch(centerX - 2, centerY - 3, 1, widthCells, floorY)
        ball.art(
            0, 0,
            " ### ",
            "##+##",
            "#+++#",
            "#+++#",
            "##+##",
            " ### "
        )
        ball.spark(2, 1)

        // Beim Schuss und beim Trick zieht der Ball eine Spur hinter sich her - Luftzug, kein
        // Material, also nicht freigestellt.
        val trail = PlayInk.Sketch(centerX, centerY, 1, widthCells, floorY)
        if (phase == FootballPhase.KICK || phase == FootballPhase.TRICK) {
            for (i in 1..4) trail.dot(-i * 2, i / 2, PlayInk.DETAIL)
        }

        val goal = PlayInk.Sketch(goalLeft, goalTop, 1, widthCells, floorY)
        goal.box(0, 0, goalRight - goalLeft, groundY - goalTop, PlayInk.BODY)
        for (x in 2 until (goalRight - goalLeft) step 2) {
            for (y in 2 until (groundY - goalTop) step 2) {
                goal.dot(x, y, PlayInk.DETAIL)
            }
        }

        return (goal.render(grounded = false) + ball.render(grounded = false) + trail.render(carve = false))
            .filter { it.x in 0 until widthCells }
            .distinctBy { it.x to it.y }
    }

    fun basketballCells(
        avatarCellX: Int,
        avatarCellY: Int,
        phase: BasketballPhase,
        scenePhase: Int,
        widthCells: Int
    ): List<SceneCell> {
        val groundY = avatarCellY + AvatarGeometry.HEIGHT - 1
        val floorY = groundY + 1
        val hoopX = (widthCells - 8).coerceAtLeast(10)
        val hoopY = groundY - 15
        val bob = if ((scenePhase / 4) % 2 == 0) 0 else 4
        val ballX = when (phase) {
            BasketballPhase.DRIBBLE -> avatarCellX + 15
            BasketballPhase.AIM -> avatarCellX + 11
            BasketballPhase.SHOOT -> (avatarCellX + hoopX) / 2
            BasketballPhase.SCORE -> hoopX - 2
        }.coerceIn(2, (widthCells - 3).coerceAtLeast(2))
        val ballY = when (phase) {
            BasketballPhase.DRIBBLE -> groundY - 2 - bob
            BasketballPhase.AIM -> groundY - 11
            BasketballPhase.SHOOT -> hoopY - 5
            BasketballPhase.SCORE -> hoopY + 2
        }

        // Kreuznaehte als typische Basketballstruktur, auf DETAIL statt die Silhouette zu
        // zerschneiden.
        val ball = PlayInk.Sketch(ballX - 2, ballY - 3, 1, widthCells, floorY)
        ball.art(
            0, 0,
            " ### ",
            "#+#+#",
            "#####",
            "+###+",
            "#####",
            "#+#+#",
            " ### "
        )
        ball.spark(1, 1)

        val trail = PlayInk.Sketch(ballX, ballY, 1, widthCells, floorY)
        if (phase == BasketballPhase.SHOOT) {
            for (i in 1..5) trail.dot(-i * 2, i, PlayInk.DETAIL)
        }

        // Der Korb wird nur fuer Basketball eingeblendet. Fussball und Training behalten damit
        // denselben freien Platz statt dauerhaft vor einer falschen Requisite stattzufinden.
        val hoop = PlayInk.Sketch(hoopX, hoopY - 5, 1, widthCells, floorY)
        hoop.line(4, 0, 4, groundY - (hoopY - 5), PlayInk.BODY)
        hoop.box(0, 0, 4, 2, PlayInk.BODY)
        hoop.line(-3, 5, 1, 5, PlayInk.BODY)
        hoop.spark(-3, 5)
        for (dy in 1..5) {
            val localY = 5 + dy
            val inset = dy / 3
            hoop.dot(-3 + inset, localY, PlayInk.DETAIL)
            hoop.dot(1 - inset, localY, PlayInk.DETAIL)
            if (dy % 2 == 0) hoop.dot(-1, localY, PlayInk.DETAIL)
        }

        return (hoop.render(grounded = false) + ball.render(grounded = false) + trail.render(carve = false))
            .filter { it.x in 0 until widthCells }
            .distinctBy { it.x to it.y }
    }

    fun trainingCells(
        avatarCellX: Int,
        avatarCellY: Int,
        phase: TrainingPhase,
        scenePhase: Int
    ): List<SceneCell> {
        val groundY = avatarCellY + AvatarGeometry.HEIGHT - 1
        val floorY = groundY + 1
        val pulse = if ((scenePhase / 5) % 2 == 0) 0 else 1
        val centerX = avatarCellX + 8
        val centerY = when (phase) {
            TrainingPhase.WARM_UP -> groundY - 3 - pulse
            TrainingPhase.LIFT -> groundY - 17 - pulse
            TrainingPhase.REST -> groundY - 2
        }

        // Trainingsmatte: flache Bodenzeichnung, bewusst ohne Freistellung - sie liegt FLACH
        // auf dem Boden, nicht davor.
        val mat = PlayInk.Sketch(centerX, groundY + 1, 1, UNBOUNDED, floorY)
        mat.line(-10, 0, 10, 0, PlayInk.DETAIL)

        val bar = PlayInk.Sketch(centerX, centerY, 1, UNBOUNDED, floorY)
        bar.line(-6, 0, 6, 0, PlayInk.BODY)
        for (side in listOf(-7, 7)) {
            bar.box(side - 1, -2, side + 1, 2, PlayInk.BODY)
        }
        if (phase == TrainingPhase.LIFT) {
            // Eine zweite, groessere Scheibe aussen: die Hantel ist jetzt voll beladen.
            for (side in listOf(-9, 9)) {
                bar.box(side - 1, -4, side + 1, 4, PlayInk.BODY)
            }
            bar.spark(0, 0)
        }

        val extra = if (phase == TrainingPhase.REST) {
            // Flasche neben der Matte: klarer Abschluss statt liegengelassener Hantel.
            val bottle = PlayInk.Sketch(centerX + 10, groundY - 5, 1, UNBOUNDED, floorY)
            bottle.art(
                0, 0,
                " ## ",
                " ## ",
                "####",
                "#++#",
                "#++#",
                "####"
            )
            bottle.spark(1, 2)
            bottle.render(grounded = true)
        } else {
            emptyList()
        }

        return (mat.render(carve = false) + bar.render(grounded = false) + extra)
            .distinctBy { it.x to it.y }
    }

    fun musicCells(
        avatarCellX: Int,
        avatarCellY: Int,
        phase: MusicPhase,
        scenePhase: Int,
        widthCells: Int
    ): List<SceneCell> {
        val useRight = avatarCellX + 34 < widthCells
        val direction = if (useRight) 1 else -1
        val ox = if (useRight) avatarCellX + 11 else avatarCellX + 5
        val oy = avatarCellY + AvatarGeometry.HEADROOM + 2

        // Kopfplatte, Hals und ein gerundeter Korpus mit Schallloch als Binnenzeichnung.
        val guitar = PlayInk.Sketch(ox, oy, direction, widthCells, UNBOUNDED)
        guitar.dot(2, -3, PlayInk.BODY)
        guitar.dot(4, -3, PlayInk.BODY)
        guitar.line(3, -3, 3, -1, PlayInk.BODY)
        guitar.art(
            0, 0,
            "   ##   ",
            "   ##   ",
            "  #++#  ",
            " #++++# ",
            "#++++++#",
            "#++++++#",
            " #++++# ",
            "  #++#  "
        )
        guitar.spark(3, 4)

        val count = when (phase) {
            MusicPhase.TUNE -> 1
            MusicPhase.PLAY -> 3
            MusicPhase.FINALE -> 5
        }
        val drift = (scenePhase / 2) % 6
        // Noten sind Klang, kein Gegenstand: Sie trennen sich ueber Helligkeit, nicht ueber
        // eine schwarze Kante.
        val notes = PlayInk.Sketch(ox, oy, direction, widthCells, UNBOUNDED)
        for (i in 0 until count) {
            val x = 12 + i * 4
            val y = 9 - i * 3 - if (phase != MusicPhase.TUNE) drift else 0
            for ((dx, dy) in listOf(0 to 0, 0 to -1, 0 to -2, 1 to -3, 2 to -3)) {
                notes.dot(x + dx, y + dy, PlayInk.EDGE)
            }
        }

        val stage = PlayInk.Sketch(ox, oy, direction, widthCells, UNBOUNDED)
        stage.line(-2, 17, 24, 17, PlayInk.DETAIL)

        return (guitar.render(grounded = false) + notes.render(carve = false) + stage.render(carve = false))
            .filter { it.x in 0 until widthCells }
            .distinctBy { it.x to it.y }
    }

    fun paintingCells(
        avatarCellX: Int,
        avatarCellY: Int,
        phase: PaintingPhase,
        scenePhase: Int,
        widthCells: Int
    ): List<SceneCell> {
        val useRight = avatarCellX + 35 < widthCells
        val direction = if (useRight) 1 else -1
        val ox = if (useRight) avatarCellX + 17 else avatarCellX - 2
        val oy = avatarCellY + AvatarGeometry.HEADROOM - 1

        val frame = PlayInk.Sketch(ox, oy, direction, widthCells, UNBOUNDED)
        frame.box(0, 0, 13, 13, PlayInk.BODY)
        frame.line(2, 15, 11, 15, PlayInk.BODY)
        frame.line(3, 14, 3, 15, PlayInk.BODY)
        frame.line(10, 14, 10, 15, PlayInk.BODY)
        frame.dot(2, 16, PlayInk.BODY)
        frame.dot(11, 16, PlayInk.BODY)

        val progress = when (phase) {
            PaintingPhase.SKETCH -> 5
            PaintingPhase.PAINT -> 9
            PaintingPhase.REVEAL -> 12
        }
        // Das Bild ist gemaltes Licht auf der Leinwand, kein Gegenstand - es trennt sich ueber
        // Helligkeit von der Kulisse, nicht ueber eine Aussparung.
        val picture = PlayInk.Sketch(ox, oy, direction, widthCells, UNBOUNDED)
        if (progress >= 5) {
            // Sonne und Bergsilhouette.
            picture.dot(10, 3, PlayInk.EDGE)
            picture.dot(9, 3, PlayInk.EDGE)
            picture.dot(10, 2, PlayInk.EDGE)
            for (x in 2..6) picture.dot(x, 10 - kotlin.math.abs(x - 4), PlayInk.EDGE)
        }
        if (progress >= 9) {
            // Die Wiese darunter.
            for (x in 1..12) picture.dot(x, 11 - (x % 3), PlayInk.EDGE)
            for (x in 2..11 step 2) picture.dot(x, 12, PlayInk.EDGE)
        }
        if (progress >= 12) {
            // Fertige Leinwand: durchgehender Bodenstrich, mit flackernder Textur darunter.
            for (x in 1..12) picture.dot(x, 11, PlayInk.EDGE)
            for (x in 1..12) if ((x + scenePhase / 4) % 3 != 0) picture.dot(x, 12, PlayInk.EDGE)
        }

        val brushY = 5 + (scenePhase / 2) % 7
        val brush = PlayInk.Sketch(ox, oy, direction, widthCells, UNBOUNDED)
        for (i in 0..6) brush.dot(16 - i / 2, brushY + i, PlayInk.BODY)
        brush.spark(16, brushY)

        return (
            frame.render(grounded = false) + picture.render(carve = false) +
                brush.render(grounded = false)
            )
            .filter { it.x in 0 until widthCells }
            .distinctBy { it.x to it.y }
    }

    /**
     * Angel, Schnur und der Fang - eine eigene kleine Szene vor der Figur, am Wasser.
     *
     * Wie bei Drache und Fussball macht erst die Schnur, die bis zum Schwimmer im Wasser reicht,
     * aus "steht am Ufer" ein "angelt": Ohne sie waere die Rute allein nur ein Stock in der Hand.
     */
    fun fishingCells(
        avatarCellX: Int,
        avatarCellY: Int,
        phase: FishingPhase,
        scenePhase: Int,
        widthCells: Int
    ): List<SceneCell> {
        val handX = (avatarCellX + 10).coerceIn(0, (widthCells - 1).coerceAtLeast(0))
        val handY = avatarCellY + AvatarGeometry.HEADROOM + 6
        val tipX = handX + 2
        val tipY = handY - 2
        // In der Wartephase treibt der Schwimmer leicht auf und ab - sonst liesse sich WAIT auf
        // dem Standbild nicht von CAST unterscheiden.
        val bob = if (phase == FishingPhase.WAIT) sin(scenePhase * 0.24).roundToInt() else 0
        val bobberX = (handX + 6).coerceIn(2, (widthCells - 3).coerceAtLeast(2))
        val bobberY = when (phase) {
            FishingPhase.CAST -> handY - 2
            FishingPhase.WAIT -> handY + 4 + bob
            FishingPhase.CATCH -> handY - 1
        }

        // Die Rute: ein kurzer schraeger Strich von der Hand nach vorn-oben.
        val rod = PlayInk.Sketch(handX, handY, 1, widthCells, UNBOUNDED)
        rod.dot(0, 0, PlayInk.BODY)
        rod.dot(1, -1, PlayInk.BODY)
        rod.dot(tipX - handX, tipY - handY, PlayInk.BODY)

        // Die Schnur von der Rutenspitze zum Schwimmer - gleichmaessige Stichproben wie beim
        // Drachen, auf dem Zellraster reicht das fuer eine erkennbare Linie. Kein Gegenstand,
        // also keine Freistellung.
        val line = PlayInk.Sketch(tipX, tipY, 1, widthCells, UNBOUNDED)
        val steps = kotlin.math.max(kotlin.math.abs(bobberX - tipX), kotlin.math.abs(bobberY - tipY))
            .coerceAtLeast(1)
        for (i in 0..steps step 2) {
            val t = i.toFloat() / steps
            line.dot(
                ((bobberX - tipX) * t).roundToInt(),
                ((bobberY - tipY) * t).roundToInt(),
                PlayInk.DETAIL
            )
        }

        val water = PlayInk.Sketch(bobberX, bobberY, 1, widthCells, UNBOUNDED)
        if (phase == FishingPhase.CATCH) {
            // Der Fisch: ein kleiner Umriss direkt ueber dem Schwimmer, gerade aus dem Wasser
            // gezogen statt darauf zu treiben.
            water.art(
                -4, -6,
                "  ###  ",
                " ##+## ",
                "##+++##",
                "#+++++#",
                "##+++##",
                " ##+## ",
                "  ###  "
            )
            water.spark(-1, -4)
        } else {
            // Der Schwimmer ist reiner Lichtpunkt auf dem Wasser, kein Gegenstand.
            water.spark(0, 0)
            // Wellenringe reagieren sichtbar auf den Schwimmer.
            val spread = 2 + kotlin.math.abs(bob)
            for (dx in -spread..spread) {
                if (kotlin.math.abs(dx) >= spread - 1) water.dot(dx, 2, PlayInk.EDGE)
            }
        }

        return (
            rod.render(grounded = false) + line.render(carve = false) +
                water.render(carve = phase == FishingPhase.CATCH)
            ).distinctBy { it.x to it.y }
    }

    /**
     * Drachen, Schnur und Schweif als gemeinsame Szene vor dem Avatar.
     *
     * Nicht als einzelnes Sprite am Koerper: Erst die lange Schnur verbindet die Figur sichtbar
     * mit dem Gegenstand im Himmel. In [KitePhase.FLY] reagiert er langsam auf [scenePhase], ohne
     * dass die Figur dafuer durch den Park laufen muss.
     */
    fun kiteCells(
        avatarCellX: Int,
        avatarCellY: Int,
        phase: KitePhase,
        scenePhase: Int,
        widthCells: Int
    ): List<SceneCell> {
        val handX = (avatarCellX + 12).coerceIn(0, (widthCells - 1).coerceAtLeast(0))
        val handY = avatarCellY + AvatarGeometry.HEADROOM + 9
        val sway = if (phase == KitePhase.FLY) sin(scenePhase * 0.32).roundToInt() * 2 else 0
        val rawCenterX = when (phase) {
            KitePhase.PREPARE -> handX + 4
            KitePhase.LAUNCH -> handX + 10
            KitePhase.FLY -> handX + 15 + sway
            KitePhase.LAND -> handX + 7
        }
        val centerX = rawCenterX.coerceIn(4, (widthCells - 5).coerceAtLeast(4))
        val centerY = when (phase) {
            KitePhase.PREPARE -> handY + 3
            KitePhase.LAUNCH -> handY - 13
            KitePhase.FLY -> (handY - 30 - kotlin.math.abs(sway)).coerceAtLeast(4)
            KitePhase.LAND -> handY - 7
        }

        // Eine duenne diagonale Schnur; gleichmaessige Stichproben reichen auf dem Zellraster.
        // Kein Gegenstand, also keine Freistellung.
        val string = PlayInk.Sketch(handX, handY, 1, widthCells, UNBOUNDED)
        if (phase != KitePhase.PREPARE) {
            val steps = kotlin.math.max(kotlin.math.abs(centerX - handX), kotlin.math.abs(centerY - handY))
                .coerceAtLeast(1)
            for (i in 0..steps step 2) {
                val t = i.toFloat() / steps
                string.dot(
                    ((centerX - handX) * t).roundToInt(),
                    ((centerY - handY) * t).roundToInt(),
                    PlayInk.DETAIL
                )
            }
        }

        // Der Drachen selbst: eine Raute mit Kreuzstreben, dazu der geknickte Schweif, der ihn
        // auch in Bewegung eindeutig als Drachen lesbar macht.
        val kite = PlayInk.Sketch(centerX, centerY, 1, widthCells, UNBOUNDED)
        kite.art(
            -3, -4,
            "  ###  ",
            " ##+## ",
            "#######",
            "#######",
            "#######",
            " ##### ",
            "  ###  "
        )
        kite.spark(0, -3)
        kite.dot(1, 5, PlayInk.BODY)
        kite.dot(-1, 6, PlayInk.BODY)
        kite.dot(1, 7, PlayInk.BODY)
        kite.dot(-1, 8, PlayInk.BODY)
        kite.dot(1, 9, PlayInk.BODY)

        return (string.render(carve = false) + kite.render(grounded = false))
            .filter { it.x in 0 until widthCells }
            .distinctBy { it.x to it.y }
    }

    /**
     * Der getragene Gegenstand, gezeichnet auf Hoehe der Haende und leicht seitlich versetzt.
     *
     * [avatarCellX]/[avatarCellY] sind die linke obere Ecke des 16x16-Sprites in Szenen-Zellen.
     * Der Versatz zielt auf die untere rechte Koerperhaelfte: Die Kreaturen blicken den Betrachter
     * an (siehe [AvatarBodies]), "in der Hand" heisst hier also vorn-seitlich und nicht vor dem
     * Bauch, wo der Gegenstand von der Silhouette verschluckt wuerde.
     */
    fun carriedCells(item: Carried, avatarCellX: Int, avatarCellY: Int): List<SceneCell> {
        val sketch = PlayInk.Sketch(
            originX = avatarCellX + CARRY_OFFSET_X,
            originY = avatarCellY + CARRY_OFFSET_Y,
            direction = 1,
            widthCells = UNBOUNDED,
            floorY = UNBOUNDED
        )
        artFor(sketch, item)
        // Freistellung ist hier KEIN Feinschliff, sondern die ganze Sache: Der getragene
        // Gegenstand liegt vor dem Koerper, und der Koerper leuchtet mit voller Helligkeit.
        // Ohne den schwarzen Ring ringsum liegt ein Ding mittlerer Helligkeit auf einer helleren
        // Flaeche - und ist damit nicht dunkler oder heller als sie, sondern schlicht nicht da.
        return sketch.render(carve = true)
    }

    private fun artFor(s: PlayInk.Sketch, item: Carried) = when (item) {
        // Buch: geschlossener Deckel mit abgesetztem Ruecken links.
        Carried.BOOK -> s.art(0, 0, "###", "##+", "##+", "###")
        // Schale mit Inhalt und einem Glanzpunkt darauf.
        Carried.FOOD -> {
            s.art(0, 0, "#  #", "####", " ## ")
            s.spark(2, 0)
        }
        // Becher: Koerper links, Henkel rechts angesetzt.
        Carried.CUP -> s.art(0, 0, "###", "#+##", "#+ #", "###")
        // Gitarre: schmaler Hals oben, Korpus mit Schallloch darunter.
        Carried.GUITAR -> s.art(0, -1, " # ", " # ", "###", "#+#", "###")
        // Staffelei: aufgespannte Leinwand ueber einem Dreibein.
        Carried.EASEL -> s.art(0, 0, "####", "#++#", "####", "# # ")
    }

    /**
     * Kurzes Aufblitzen an der Stelle, an der die Figur gerade zugreift.
     *
     * **Warum ein Stern und kein Leuchten.** Ein weicher heller Fleck waere auf diesem Raster von
     * einer Lampe nicht zu unterscheiden - und Lampen gibt es hier reichlich. Ein Muster, das nach
     * aussen laeuft und dabei verschwindet, kann dagegen nichts anderes sein als ein Ereignis:
     * Es hat keine Entsprechung im ruhenden Bild.
     *
     * [progress] laeuft von 0 bis 1; der Stern wandert dabei nach aussen und verliert an
     * Helligkeit, sodass der Blitz von selbst ausklingt.
     */
    fun sparkCells(centerX: Int, centerY: Int, progress: Float): List<SceneCell> {
        if (progress <= 0f || progress >= 1f) return emptyList()
        val radius = 1 + (progress * 3f).roundToInt()
        val fade = 1f - progress
        val bright = (PlayScene.GLOW * fade).roundToInt()
        val dim = (bright * 0.55f).roundToInt()
        if (bright <= 0) return emptyList()
        return listOf(
            SceneCell(centerX - radius, centerY, bright, isLight = true),
            SceneCell(centerX + radius, centerY, bright, isLight = true),
            SceneCell(centerX, centerY - radius, bright, isLight = true),
            SceneCell(centerX, centerY + radius, bright, isLight = true),
            SceneCell(centerX - radius + 1, centerY - radius + 1, dim, isLight = true),
            SceneCell(centerX + radius - 1, centerY - radius + 1, dim, isLight = true),
            SceneCell(centerX - radius + 1, centerY + radius - 1, dim, isLight = true),
            SceneCell(centerX + radius - 1, centerY + radius - 1, dim, isLight = true)
        )
    }

    /**
     * Sprechzeichen ueber dem Kopf - drei Punkte, die nacheinander erscheinen.
     *
     * **Warum ueberhaupt ein Zeichen.** Zwei Kreaturen, die abwechselnd huepfen, koennten alles
     * moegliche tun; erst ein Zeichen ueber genau einem der beiden Koepfe sagt, WER gerade spricht.
     * Punkte statt einer Sprechblase, weil eine Blase auf diesem Raster nur ein Klecks waere -
     * drei Punkte in einer Reihe sind das kleinste Muster, das eindeutig fuer Rede steht.
     *
     * [step] laeuft von 0 bis 2 und laesst die Punkte nacheinander auftauchen.
     */
    fun speechCells(avatarCellX: Int, avatarCellY: Int, step: Int): List<SceneCell> {
        val ox = avatarCellX + SPEECH_OFFSET_X
        val oy = avatarCellY + SPEECH_OFFSET_Y
        return (0..(step.coerceIn(0, 2))).map { i ->
            SceneCell(ox + i * 2, oy, PlayScene.GLOW - 200, isLight = true)
        }
    }

    private const val SPEECH_OFFSET_X = 5
    // Ganz oben in der Kopffreiheit - dort ist jetzt Platz, ueber dem Kopf zu sprechen.
    private const val SPEECH_OFFSET_Y = 1

    /** Wie lange ein Blitz dauert - lang genug, um ihn zu bemerken, kurz genug, um nicht zu stoeren. */
    const val SPARK_DURATION_MS = 520

    /** Kein Rand: Der getragene Gegenstand haengt an der Figur und wird mit ihr beschnitten,
     *  nicht an der Spielfeldkante. */
    private const val UNBOUNDED = Int.MAX_VALUE

    private const val CARRY_OFFSET_X = 11
    // Im hohen Raster: 9 Zeilen unter der Figur plus deren Kopffreiheit (siehe AvatarGeometry).
    private val CARRY_OFFSET_Y = 9 + AvatarGeometry.HEADROOM
}
