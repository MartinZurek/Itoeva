package com.notime.glyphsim.matrix

import kotlin.math.abs
import kotlin.math.sin

/**
 * **Ein Spiel, bei dem alle mitmachen, die gerade da sind.**
 *
 * ## Warum es das gibt
 *
 * Gemeldet am 2026-09-26: Morgens lief Sport "mit der Gruppe" - vier Figuren standen im Park,
 * vor dem Hauptavatar baute sich ein Geraet aus Pixeln auf, die anderen drei taten nichts, und
 * nach kurzer Zeit gingen alle wieder. Ohne eigene Musik, und ohne dass man erkennen konnte,
 * was fuer ein Sport das war. Gewuenscht: dass sie tatsaechlich MITEINANDER spielen - sich einen
 * Ball zuwerfen, eine Frisbee, nacheinander auf den Korb werfen, sich einen Fussball zuspielen -
 * mit Musik darunter. Solche Szenen brechen den Alltag auf und haben Vorrang.
 *
 * Bis dahin kannten die Sonderaktivitaeten nur EINE Figur: Ball, Hantel und Korb hingen am
 * Hauptavatar. Ein zweiter Teilnehmer durfte hoechstens dieselbe Koerperregung mitmachen.
 *
 * ## Wie es gebaut ist
 *
 * Rein und ohne Android: [momentAt] bekommt die Mitspieler in Szenenzellen und das Alter des
 * Spiels in Szenentakten und liefert, wo der Ball ist und welche Haltung jeder gerade hat. Die
 * Oberflaeche zeichnet beides nur noch. Dadurch kommt ein Gast, der mitten im Spiel geht oder
 * dazukommt, ohne Sonderfall aus: Die naechste Runde rechnet mit denen, die da sind.
 *
 * Die Reihenfolge der Paesse ist fest aus dem Takt gerechnet, nicht gewuerfelt - Bildschirm,
 * Schnappschuss und Film zeigen denselben Wurf.
 */
object PlayGroupGame {

    /** Was gespielt wird. */
    enum class Kind {
        /** Ein Ball im hohen Bogen von einem zum naechsten. */
        CATCH,

        /** Eine Scheibe, flach und weit, die im Flug kippelt. */
        FRISBEE,

        /** Ein Fussball, am Boden zugespielt. */
        KICKABOUT,

        /** Nacheinander auf den Korb - wer trifft, wird bejubelt. */
        HOOPS
    }

    /** Die Haltung eines Mitspielers in diesem Augenblick (siehe [AvatarAnimations.gamePose]). */
    enum class Pose { READY, THROW, CATCH, CHEER, WATCH }

    /**
     * Ein Mitspieler, in Szenenzellen: [left]/[top] ist die obere linke Ecke seines Sprites,
     * [size] die Breite in Zellen (16 fuer volle Groesse, kleiner fuer Hintergrundfiguren).
     */
    data class Player(val id: String, val left: Int, val top: Int, val size: Int) {
        val centerX: Int get() = left + size / 2
        val groundY: Int get() = top + size * AvatarGeometry.HEIGHT / AvatarGeometry.SIZE - 1

        /** Wo ein gehaltener Ball liegt: ueber dem Kopf, nicht im Bauch. */
        val handY: Int get() = groundY - size * 13 / 16
    }

    /** Was in einem Takt zu sehen ist. */
    data class Moment(
        val ballCells: List<SceneCell>,
        val poses: Map<String, Pose>,
        /** Wer nach links schaut - jeder blickt dem Ball hinterher. */
        val facesLeft: Map<String, Boolean>
    )

    /** So lange dauert ein Spiel - lang genug, dass es als Ereignis zaehlt, nicht als Einlage. */
    const val DURATION_MS = 60_000L

    /** Wie lange der Ball beim Werfer liegt, bevor er fliegt. */
    private const val HOLD_TICKS = 3

    /** Flugzeit je Art, in Szenentakten (200 ms). */
    private fun flightTicks(kind: Kind): Int = when (kind) {
        Kind.CATCH -> 6
        Kind.FRISBEE -> 8
        Kind.KICKABOUT -> 5
        Kind.HOOPS -> 7
    }

    // Korbwurf: prellen, werfen, durch das Netz fallen, zum Naechsten zurueck.
    private const val HOOP_DRIBBLE = 5
    private const val HOOP_DROP = 5
    private const val HOOP_RETURN = 5

    /**
     * Welches Spiel zu einem Ort und einer geplanten Sonderaktivitaet passt.
     *
     * Der Sportplatz hat Korb und Tor, also Basketball oder Fussball; Park und Wiese sind offen,
     * dort fliegen Ball und Frisbee. [roll] waehlt zwischen den Moeglichkeiten.
     */
    fun kindFor(place: PlayScene.Place, special: PlayRoutines.SpecialActivity?, roll: Int): Kind = when {
        special == PlayRoutines.SpecialActivity.BASKETBALL -> Kind.HOOPS
        special == PlayRoutines.SpecialActivity.FOOTBALL -> Kind.KICKABOUT
        place == PlayScene.Place.SPORT -> if (abs(roll) % 2 == 0) Kind.HOOPS else Kind.KICKABOUT
        else -> listOf(Kind.CATCH, Kind.FRISBEE, Kind.KICKABOUT)[abs(roll) % 3]
    }

    /**
     * Der Ablauf eines Gruppenspiels: hingehen, einen Platz in der Mitte suchen, spielen, kurz
     * verschnaufen. Ein Ortswechsel des urspruenglichen Ablaufs ([place]) bleibt vorn erhalten -
     * Basketball und Fussball finden auf dem Sportplatz statt, auch in der Gruppe.
     */
    fun routine(kind: Kind, place: PlayScene.Place?): PlayRoutine = PlayRoutine(
        buildList {
            if (place != null) add(RoutineStep.GoToPlace(place))
            // Beim Korbwurf steht der Korb rechts - die Figur faengt weiter links an.
            add(RoutineStep.Stroll(if (kind == Kind.HOOPS) 0.18f else 0.30f))
            add(RoutineStep.GroupGame(kind))
            add(RoutineStep.Stir(AvatarAnimations.Fidget.SHAKE))
            add(RoutineStep.Linger(3_000L))
        }
    )

    /**
     * Ob aus einem Bewegungs-Ablauf ein Gruppenspiel wird: nur tagsueber, nur auf einem
     * [PLAYGROUNDS]-Ort, und nur, wenn noch jemand da ist. Dann aber immer - gewuenscht war, dass
     * solche Szenen Vorrang haben.
     */
    fun shouldPlay(
        topicIsMove: Boolean,
        place: PlayScene.Place,
        othersPresent: Boolean,
        night: Boolean
    ): Boolean = topicIsMove && !night && othersPresent && place in PLAYGROUNDS

    /** Orte, an denen genug Platz fuer ein Spiel ist. */
    val PLAYGROUNDS: Set<PlayScene.Place> = setOf(
        PlayScene.Place.PARK,
        PlayScene.Place.SPORT,
        PlayScene.Place.MEADOW
    )

    /**
     * Wer in Runde [round] den Ball hat - eine Folge ohne Hin-und-Her: Bei drei oder mehr
     * Mitspielern geht der Ball nie direkt an den zurueck, von dem er kam, und jeder kommt
     * regelmaessig dran.
     */
    internal fun holderAt(round: Int, count: Int): Int {
        if (count <= 1) return 0
        var holder = 0
        var previous = -1
        for (k in 0 until round) {
            val next = nextHolder(holder, previous, k, count)
            previous = holder
            holder = next
        }
        return holder
    }

    private fun nextHolder(holder: Int, previous: Int, round: Int, count: Int): Int {
        if (count == 2) return 1 - holder
        // Deterministisch gestreut: mal der Nachbar, mal weiter weg.
        val candidates = (0 until count).filter { it != holder && it != previous }
        return candidates[(round * 7 + 3) % candidates.size]
    }

    /** Der Takt [age] des Spiels [kind] mit [players] (in Blickrichtung von links nach rechts). */
    fun momentAt(kind: Kind, players: List<Player>, age: Int, widthCells: Int, floorY: Int): Moment {
        if (players.isEmpty()) return Moment(emptyList(), emptyMap(), emptyMap())
        val ordered = players.sortedBy { it.centerX }
        return if (kind == Kind.HOOPS) {
            hoops(ordered, age.coerceAtLeast(0), widthCells, floorY)
        } else {
            passing(kind, ordered, age.coerceAtLeast(0), widthCells, floorY)
        }
    }

    // ---- Zuspielen: Ball, Frisbee, Fussball -------------------------------------------------

    private fun passing(kind: Kind, players: List<Player>, age: Int, widthCells: Int, floorY: Int): Moment {
        val flight = flightTicks(kind)
        val cycle = HOLD_TICKS + flight
        val round = age / cycle
        val t = age % cycle
        val from = players[holderAt(round, players.size)]
        val to = players[holderAt(round + 1, players.size)]
        val solo = from.id == to.id

        val (x0, y0) = holdPoint(kind, from, toward = to, solo = solo)
        val (x1, y1) = holdPoint(kind, to, toward = from, solo = solo)
        val inFlight = t >= HOLD_TICKS
        val f = if (inFlight) (t - HOLD_TICKS + 1).toFloat() / flight else 0f

        val ballX: Int
        val ballY: Int
        if (!inFlight) {
            ballX = x0
            // Kurz vor dem Wurf holt der Werfer aus: der Ball geht eine Zelle tiefer.
            ballY = if (t == HOLD_TICKS - 1 && kind != Kind.KICKABOUT) y0 + 1 else y0
        } else {
            ballX = x0 + ((x1 - x0) * f).toInt()
            val straight = y0 + (y1 - y0) * f
            ballY = when (kind) {
                Kind.CATCH -> {
                    // Bei einem Wurf fuer sich allein geht der Ball senkrecht hoch und zurueck.
                    val arc = if (solo) 9f else (4f + abs(x1 - x0) / 5f).coerceAtMost(10f)
                    (straight - arc * 4f * f * (1f - f)).toInt()
                }
                Kind.FRISBEE -> {
                    val arc = if (solo) 7f else (2f + abs(x1 - x0) / 10f).coerceAtMost(5f)
                    (straight - arc * 4f * f * (1f - f)).toInt()
                }
                // Am Boden: kleine Hopser statt eines Bogens.
                Kind.KICKABOUT -> y0 - (abs(sin(f * Math.PI * 2)) * 2).toInt()
                Kind.HOOPS -> y0
            }
        }

        val ball = when (kind) {
            Kind.FRISBEE -> frisbee(ballX, ballY, tilt = inFlight && age % 2 == 0, widthCells, floorY)
            Kind.KICKABOUT -> football(ballX, ballY, widthCells, floorY)
            else -> ball(ballX, ballY, widthCells, floorY)
        }

        val poses = players.associate { p ->
            p.id to when {
                p.id == from.id && (t == HOLD_TICKS - 1 || t == HOLD_TICKS) -> Pose.THROW
                p.id == to.id && inFlight && f >= 0.7f -> Pose.CATCH
                p.id == from.id && !inFlight -> Pose.READY
                p.id == to.id -> Pose.READY
                else -> Pose.WATCH
            }
        }
        return Moment(ball, poses, facing(players, ballX))
    }

    /** Wo der Ball bei [player] liegt: ueber dem Kopf, zur Seite des Partners hin. */
    private fun holdPoint(kind: Kind, player: Player, toward: Player, solo: Boolean): Pair<Int, Int> {
        val side = if (solo) 1 else if (toward.centerX >= player.centerX) 1 else -1
        return when (kind) {
            // Ein Fussball liegt vor den Fuessen, nicht ueber dem Kopf.
            Kind.KICKABOUT -> (player.centerX + side * (player.size / 2 + 1)) to (player.groundY - 2)
            else -> (player.centerX + side * (player.size / 4)) to player.handY
        }
    }

    // ---- Korbwurf -----------------------------------------------------------------------------

    private fun hoops(players: List<Player>, age: Int, widthCells: Int, floorY: Int): Moment {
        val shot = flightTicks(Kind.HOOPS)
        val cycle = HOOP_DRIBBLE + shot + HOOP_DROP + HOOP_RETURN
        val round = age / cycle
        val t = age % cycle
        val shooter = players[round % players.size]
        val next = players[(round + 1) % players.size]
        val groundY = players.maxOf { it.groundY }
        val hoopX = (widthCells - 8).coerceAtLeast(10)
        val hoopY = groundY - 15
        val ringX = hoopX - 1
        // Jeder dritte Wurf geht daneben - wer immer trifft, bekommt keinen Jubel, der zaehlt.
        val scores = round % 3 != 2

        val handX = shooter.centerX + shooter.size / 4
        val (ballX, ballY) = when {
            t < HOOP_DRIBBLE -> {
                val bob = listOf(0, 3, 5, 3, 0)[t % 5]
                (shooter.centerX + shooter.size / 2 + 1) to (shooter.groundY - 2 - bob)
            }
            t < HOOP_DRIBBLE + shot -> {
                val f = (t - HOOP_DRIBBLE + 1).toFloat() / shot
                val x = handX + ((ringX - handX) * f).toInt()
                val straight = shooter.handY + (hoopY - shooter.handY) * f
                x to (straight - 6f * 4f * f * (1f - f)).toInt()
            }
            t < HOOP_DRIBBLE + shot + HOOP_DROP -> {
                val f = (t - HOOP_DRIBBLE - shot + 1).toFloat() / HOOP_DROP
                if (scores) {
                    // Durch das Netz nach unten.
                    ringX to (hoopY + 2 + ((groundY - 2 - hoopY - 2) * f).toInt())
                } else {
                    // Vom Ring abgeprallt, seitlich heraus.
                    (ringX - (6 * f).toInt()) to (hoopY - 3 + ((groundY - 2 - hoopY + 3) * f * f).toInt())
                }
            }
            else -> {
                val f = (t - HOOP_DRIBBLE - shot - HOOP_DROP + 1).toFloat() / HOOP_RETURN
                val startX = if (scores) ringX else ringX - 6
                val x = startX + ((next.centerX + next.size / 2 + 1 - startX) * f).toInt()
                x to (groundY - 2 - (abs(sin(f * Math.PI * 2)) * 2).toInt())
            }
        }

        val cheering = scores && t >= HOOP_DRIBBLE + shot && t < HOOP_DRIBBLE + shot + HOOP_DROP
        val poses = players.associate { p ->
            p.id to when {
                cheering -> Pose.CHEER
                p.id == shooter.id && t >= HOOP_DRIBBLE - 1 && t <= HOOP_DRIBBLE -> Pose.THROW
                p.id == shooter.id && t < HOOP_DRIBBLE -> Pose.READY
                p.id == next.id && t >= cycle - 2 -> Pose.CATCH
                else -> Pose.WATCH
            }
        }
        val cells = ball(ballX, ballY, widthCells, floorY, big = true) +
            PlayEffects.hoopCells(hoopX, hoopY, groundY, widthCells, floorY)
        return Moment(cells.distinctBy { it.x to it.y }, poses, facing(players, ballX))
    }

    // ---- Zeichnen -----------------------------------------------------------------------------

    private fun facing(players: List<Player>, ballX: Int): Map<String, Boolean> =
        players.associate { it.id to (ballX < it.centerX - 1) }

    private fun ball(x: Int, y: Int, widthCells: Int, floorY: Int, big: Boolean = false): List<SceneCell> {
        val sketch = PlayInk.Sketch(x - if (big) 2 else 1, y - if (big) 2 else 1, 1, widthCells, floorY)
        if (big) {
            sketch.art(0, 0, " ### ", "#+#+#", "#####", "#+#+#", " ### ")
            sketch.spark(1, 1)
        } else {
            sketch.art(0, 0, " # ", "###", " # ")
            sketch.spark(1, 1)
        }
        return sketch.render(grounded = false)
    }

    private fun frisbee(x: Int, y: Int, tilt: Boolean, widthCells: Int, floorY: Int): List<SceneCell> {
        val sketch = PlayInk.Sketch(x - 2, y - 1, 1, widthCells, floorY)
        // Die Scheibe kippelt im Flug - flach, dann schraeg. Genau daran erkennt man sie.
        if (tilt) {
            sketch.art(0, 0, "   ##", " ### ", "##   ")
        } else {
            sketch.art(0, 0, "     ", "#####", " +++ ")
        }
        sketch.spark(2, 1)
        return sketch.render(grounded = false)
    }

    private fun football(x: Int, y: Int, widthCells: Int, floorY: Int): List<SceneCell> {
        val sketch = PlayInk.Sketch(x - 2, y - 2, 1, widthCells, floorY)
        sketch.art(0, 0, " ### ", "##+##", "#+++#", "##+##", " ### ")
        sketch.spark(1, 1)
        return sketch.render(grounded = false)
    }
}
