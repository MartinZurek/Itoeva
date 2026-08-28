package com.notime.glyphsim.matrix

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

    /** Werfen, Warten und der Fang - die drei sichtbaren Phasen einer Angel-Szene am Teich. */
    enum class FishingPhase { CAST, WAIT, CATCH }

    fun footballCells(
        avatarCellX: Int,
        avatarCellY: Int,
        phase: FootballPhase,
        scenePhase: Int,
        widthCells: Int
    ): List<SceneCell> {
        val groundY = avatarCellY + AvatarGeometry.HEIGHT - 1
        val direction = if ((scenePhase / 5) % 2 == 0) 1 else -1
        val centerX = when (phase) {
            FootballPhase.DRIBBLE -> avatarCellX + 15 + direction * 2
            FootballPhase.AIM -> avatarCellX + 17
            FootballPhase.KICK -> avatarCellX + 23
            FootballPhase.TRICK -> avatarCellX + 12 + direction * 4
        }.coerceIn(2, (widthCells - 3).coerceAtLeast(2))
        val centerY = when (phase) {
            FootballPhase.DRIBBLE, FootballPhase.AIM -> groundY - 1
            FootballPhase.KICK -> groundY - 7
            FootballPhase.TRICK -> groundY - 13
        }
        val ball = listOf(
            0 to -2,
            -1 to -1, 0 to -1, 1 to -1,
            -2 to 0, -1 to 0, 0 to 0, 1 to 0, 2 to 0,
            -1 to 1, 0 to 1, 1 to 1,
            0 to 2
        ).map { (dx, dy) ->
            SceneCell(centerX + dx, centerY + dy, PlayScene.GLOW - 250, isLight = true)
        }
        val goalRight = widthCells - 3
        val goalLeft = (goalRight - 12).coerceAtLeast(0)
        val goalTop = groundY - 9
        val goal = buildList {
            for (x in goalLeft..goalRight) add(SceneCell(x, goalTop, PlayScene.BACKDROP))
            for (y in goalTop..groundY) {
                add(SceneCell(goalLeft, y, PlayScene.BACKDROP))
                add(SceneCell(goalRight, y, PlayScene.BACKDROP))
            }
            for (x in goalLeft + 2 until goalRight step 2) {
                for (y in goalTop + 2 until groundY step 2) {
                    add(SceneCell(x, y, PlayScene.BACKDROP))
                }
            }
        }
        return (goal + ball).filter { it.x in 0 until widthCells }.distinctBy { it.x to it.y }
    }

    fun basketballCells(
        avatarCellX: Int,
        avatarCellY: Int,
        phase: BasketballPhase,
        scenePhase: Int,
        widthCells: Int
    ): List<SceneCell> {
        val groundY = avatarCellY + AvatarGeometry.HEIGHT - 1
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
        val ball = listOf(0 to -1, -1 to 0, 0 to 0, 1 to 0, 0 to 1).map { (dx, dy) ->
            SceneCell(ballX + dx, ballY + dy, PlayScene.GLOW - 250, isLight = true)
        }
        // Der Korb wird nur fuer Basketball eingeblendet. Fussball und Training behalten damit
        // denselben freien Platz statt dauerhaft vor einer falschen Requisite stattzufinden.
        val hoop = buildList {
            for (y in hoopY - 5..groundY) add(SceneCell(hoopX + 4, y, PlayScene.FURNITURE))
            for (x in hoopX..hoopX + 4) add(SceneCell(x, hoopY - 5, PlayScene.FURNITURE))
            for (x in hoopX - 3..hoopX + 1) {
                add(SceneCell(x, hoopY, PlayScene.GLOW - 400, isLight = true))
            }
            add(SceneCell(hoopX - 2, hoopY + 1, PlayScene.FURNITURE))
            add(SceneCell(hoopX, hoopY + 1, PlayScene.FURNITURE))
        }
        return (hoop + ball).filter { it.x in 0 until widthCells }.distinctBy { it.x to it.y }
    }

    fun trainingCells(
        avatarCellX: Int,
        avatarCellY: Int,
        phase: TrainingPhase,
        scenePhase: Int
    ): List<SceneCell> {
        val groundY = avatarCellY + AvatarGeometry.HEIGHT - 1
        val pulse = if ((scenePhase / 5) % 2 == 0) 0 else 1
        val centerX = avatarCellX + 8
        val centerY = when (phase) {
            TrainingPhase.WARM_UP -> groundY - 3 - pulse
            TrainingPhase.LIFT -> groundY - 17 - pulse
            TrainingPhase.REST -> groundY - 2
        }
        return buildList {
            for (x in centerX - 6..centerX + 6) {
                add(SceneCell(x, centerY, PlayScene.FURNITURE))
            }
            for (dy in -2..2) {
                add(SceneCell(centerX - 7, centerY + dy, PlayScene.GLOW - 350, isLight = true))
                add(SceneCell(centerX + 7, centerY + dy, PlayScene.GLOW - 350, isLight = true))
            }
        }.distinctBy { it.x to it.y }
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

        val result = mutableListOf<SceneCell>()
        // Die Rute: ein kurzer schraeger Strich von der Hand nach vorn-oben.
        result += SceneCell(handX, handY, PlayScene.FURNITURE)
        result += SceneCell(handX + 1, handY - 1, PlayScene.FURNITURE)
        result += SceneCell(tipX, tipY, PlayScene.FURNITURE)
        // Die Schnur von der Rutenspitze zum Schwimmer - gleichmaessige Stichproben wie beim
        // Drachen, auf dem Zellraster reicht das fuer eine erkennbare Linie.
        val steps = kotlin.math.max(kotlin.math.abs(bobberX - tipX), kotlin.math.abs(bobberY - tipY))
            .coerceAtLeast(1)
        for (i in 0..steps step 2) {
            val t = i.toFloat() / steps
            result += SceneCell(
                x = (tipX + (bobberX - tipX) * t).roundToInt(),
                y = (tipY + (bobberY - tipY) * t).roundToInt(),
                brightness = PlayScene.FURNITURE
            )
        }
        if (phase == FishingPhase.CATCH) {
            // Der Fisch: ein kleiner Umriss direkt ueber dem Schwimmer, gerade aus dem Wasser
            // gezogen statt darauf zu treiben.
            val fish = listOf(-1 to 0, 0 to 0, 1 to 0, 2 to -1, 0 to 1, 1 to 1)
            for ((dx, dy) in fish) {
                result += SceneCell(bobberX + dx, bobberY + dy - 2, PlayScene.GLOW - 250, isLight = true)
            }
        } else {
            result += SceneCell(bobberX, bobberY, PlayScene.GLOW - 250, isLight = true)
        }
        return result.distinctBy { it.x to it.y }
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
        val centerX = rawCenterX.coerceIn(3, (widthCells - 4).coerceAtLeast(3))
        val centerY = when (phase) {
            KitePhase.PREPARE -> handY + 3
            KitePhase.LAUNCH -> handY - 13
            KitePhase.FLY -> (handY - 30 - kotlin.math.abs(sway)).coerceAtLeast(4)
            KitePhase.LAND -> handY - 7
        }

        val result = mutableListOf<SceneCell>()
        if (phase != KitePhase.PREPARE) {
            // Eine duenne diagonale Schnur; gleichmaessige Stichproben reichen auf dem Zellraster.
            val steps = kotlin.math.max(kotlin.math.abs(centerX - handX), kotlin.math.abs(centerY - handY))
                .coerceAtLeast(1)
            for (i in 0..steps step 2) {
                val t = i.toFloat() / steps
                result += SceneCell(
                    x = (handX + (centerX - handX) * t).roundToInt(),
                    y = (handY + (centerY - handY) * t).roundToInt(),
                    brightness = PlayScene.FURNITURE
                )
            }
        }

        val kite = listOf(
            0 to -2,
            -1 to -1, 0 to -1, 1 to -1,
            -2 to 0, -1 to 0, 0 to 0, 1 to 0, 2 to 0,
            -1 to 1, 0 to 1, 1 to 1,
            0 to 2
        )
        for ((dx, dy) in kite) {
            result += SceneCell(centerX + dx, centerY + dy, PlayScene.GLOW - 250, isLight = true)
        }
        // Der geknickte Schweif macht die Raute auch in Bewegung eindeutig als Drachen lesbar.
        result += SceneCell(centerX + 1, centerY + 3, PlayScene.FURNITURE)
        result += SceneCell(centerX, centerY + 4, PlayScene.FURNITURE)
        result += SceneCell(centerX + 1, centerY + 5, PlayScene.FURNITURE)
        return result.distinctBy { it.x to it.y }
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
        val ox = avatarCellX + CARRY_OFFSET_X
        val oy = avatarCellY + CARRY_OFFSET_Y
        return artFor(item).map { (x, y) ->
            SceneCell(ox + x, oy + y, PlayScene.GLOW - 400, isLight = true)
        }
    }

    private fun artFor(item: Carried): List<Pair<Int, Int>> = when (item) {
        // Buch: geschlossener Block mit angedeutetem Ruecken.
        Carried.BOOK -> listOf(0 to 0, 1 to 0, 2 to 0, 0 to 1, 2 to 1, 0 to 2, 1 to 2, 2 to 2)
        // Schale mit Inhalt.
        Carried.FOOD -> listOf(0 to 0, 2 to 0, 0 to 1, 1 to 1, 2 to 1, 1 to 2)
        // Becher mit Henkel.
        Carried.CUP -> listOf(0 to 0, 1 to 0, 0 to 1, 1 to 1, 2 to 1, 0 to 2, 1 to 2)
        // Gitarre: schmaler Hals oben, runder Korpus darunter.
        Carried.GUITAR -> listOf(1 to -1, 1 to 0, 0 to 1, 1 to 1, 2 to 1, 0 to 2, 1 to 2, 2 to 2)
        // Staffelei: Dreibein mit aufgespannter Leinwand obenauf.
        Carried.EASEL -> listOf(0 to 0, 1 to 0, 2 to 0, 1 to 1, 0 to 2, 2 to 2)
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

    private const val CARRY_OFFSET_X = 11
    // Im hohen Raster: 9 Zeilen unter der Figur plus deren Kopffreiheit (siehe AvatarGeometry).
    private val CARRY_OFFSET_Y = 9 + AvatarGeometry.HEADROOM
}
