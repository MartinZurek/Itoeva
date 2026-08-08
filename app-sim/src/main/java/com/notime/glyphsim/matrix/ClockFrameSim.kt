package com.notime.glyphsim.matrix

import java.time.LocalTime
import kotlin.math.roundToInt

/**
 * Baut den Uhrzeit-Frame fuer die simulierte Matrix - mehrere waehlbare Designs
 * (siehe [ClockStyle]), vom Nutzer im Einstellungen-Dialog in HomeScreen
 * umschaltbar (siehe ClockStylePrefs in ui/).
 *
 * CLASSIC ist unveraendert das urspruengliche, groessere Design (zwei Ziffernbloecke
 * je 4 breit + 1 Spalte Abstand = 9 Spalten, mittig auf dem 13x13-Raster, aehnlich
 * glyph/ClockFrame.kt im :app-Modul). Dabei faellt auf: die Matrix ist rund (Kreis
 * mit Radius 6,5 um die Rastermitte 6,6, siehe [MatrixGeometry]) - in den
 * kritischen Randzeilen y=1 und y=11 sind dadurch nur die mittleren 9 Spalten
 * (x=2..10) ueberhaupt aktiv, exakt so breit wie der CLASSIC-Block selbst. Ziffern
 * mit einer vollen obersten/untersten Reihe (z.B. "0", "2", "3", ...) sitzen an
 * diesen Stellen also randlos auf dem runden Rand statt sichtbaren Abstand zu
 * haben. COMPACT/MINIMAL/RING nutzen deshalb den schmaleren [CompactDigitFont]
 * (3 statt 4 Spalten breit), was auf beiden Seiten mindestens 1 Spalte Luft zum
 * Rand laesst.
 */
object ClockFrameSim {

    fun buildFrame(time: LocalTime = LocalTime.now(), style: ClockStyle = ClockStyle.CLASSIC): IntArray =
        when (style) {
            ClockStyle.CLASSIC -> classicFrame(time)
            ClockStyle.COMPACT -> compactFrame(time)
            ClockStyle.MINIMAL -> minimalFrame(time)
            ClockStyle.RING -> ringFrame(time)
        }

    private const val Y_OFFSET = 1 // Randzeile y=0 frei lassen (runde Matrix)
    private const val ROW_GAP = 1 // Abstand zwischen Stunden- und Minutenblock
    private const val MINIMAL_ROW_GAP = 3
    private val MINIMAL_BRIGHTNESS = (MatrixGeometry.MAX_BRIGHTNESS * 0.7).roundToInt()
    private const val RING_TICK_BRIGHTNESS_FRACTION = 0.18
    private const val RING_HOUR_MARKS = 12 // volle Stundenstriche auf dem Ring

    private fun classicFrame(time: LocalTime): IntArray {
        val font = DigitFont
        val pairWidth = font.digitWidth * 2 + 1
        val xOffset = (MatrixGeometry.SIZE - pairWidth) / 2
        val points = mutableListOf<Pair<Int, Int>>()
        points += twoCharPoints("%02d".format(time.hour), font, xOffset, Y_OFFSET)
        points += twoCharPoints("%02d".format(time.minute), font, xOffset, Y_OFFSET + font.digitHeight + ROW_GAP)
        return toGrid(points)
    }

    private fun compactFrame(time: LocalTime): IntArray {
        val font = CompactDigitFont
        val pairWidth = font.digitWidth * 2 + 1
        val xOffset = (MatrixGeometry.SIZE - pairWidth) / 2
        val points = mutableListOf<Pair<Int, Int>>()
        points += twoCharPoints("%02d".format(time.hour), font, xOffset, Y_OFFSET)
        points += twoCharPoints("%02d".format(time.minute), font, xOffset, Y_OFFSET + font.digitHeight + ROW_GAP)
        return toGrid(points)
    }

    private fun minimalFrame(time: LocalTime): IntArray {
        val font = CompactDigitFont
        val points = mutableListOf<Pair<Int, Int>>()
        // Stunde bewusst ohne fuehrende Null (variable Breite, jede Zeile eigenstaendig
        // zentriert) - zusammen mit dem groesseren Zeilenabstand und der gedimmten
        // Helligkeit der eigentliche "minimalistische" Unterschied zu COMPACT.
        points += textRowPoints(time.hour.toString(), font, Y_OFFSET)
        points += textRowPoints("%02d".format(time.minute), font, Y_OFFSET + font.digitHeight + MINIMAL_ROW_GAP)
        return toGrid(points, brightness = MINIMAL_BRIGHTNESS)
    }

    private fun ringFrame(time: LocalTime): IntArray {
        val frame = compactFrame(time)
        val ring = ClockRing.perimeterCells
        if (ring.isEmpty()) return frame

        val tickBrightness = (MatrixGeometry.MAX_BRIGHTNESS * RING_TICK_BRIGHTNESS_FRACTION).roundToInt()
        val hourMarkStep = (ring.size / RING_HOUR_MARKS).coerceAtLeast(1)
        ring.forEachIndexed { index, (x, y) ->
            if (index % hourMarkStep == 0) {
                setIfActive(frame, x, y, tickBrightness)
            }
        }

        val minuteFraction = time.minute / 60f
        val markerIndex = (minuteFraction * ring.size).roundToInt() % ring.size
        val (markerX, markerY) = ring[markerIndex]
        setIfActive(frame, markerX, markerY, MatrixGeometry.MAX_BRIGHTNESS)
        return frame
    }

    private fun twoCharPoints(twoChars: String, font: PixelFont, xOffset: Int, yOffset: Int): List<Pair<Int, Int>> {
        val first = font.pointsFor(twoChars[0]).map { (x, y) -> (x + xOffset) to (y + yOffset) }
        val secondX = xOffset + font.digitWidth + 1
        val second = font.pointsFor(twoChars[1]).map { (x, y) -> (x + secondX) to (y + yOffset) }
        return first + second
    }

    private fun textRowPoints(text: String, font: PixelFont, yOffset: Int): List<Pair<Int, Int>> {
        val width = text.length * font.digitWidth + (text.length - 1)
        val xOffset = (MatrixGeometry.SIZE - width) / 2
        val points = mutableListOf<Pair<Int, Int>>()
        text.forEachIndexed { index, char ->
            val charX = xOffset + index * (font.digitWidth + 1)
            points += font.pointsFor(char).map { (x, y) -> (x + charX) to (y + yOffset) }
        }
        return points
    }

    private fun setIfActive(frame: IntArray, x: Int, y: Int, brightness: Int) {
        if (x in 0 until MatrixGeometry.SIZE && y in 0 until MatrixGeometry.SIZE) {
            frame[y * MatrixGeometry.SIZE + x] = brightness
        }
    }

    private fun toGrid(points: List<Pair<Int, Int>>, brightness: Int = MatrixGeometry.MAX_BRIGHTNESS): IntArray {
        val grid = IntArray(MatrixGeometry.SIZE * MatrixGeometry.SIZE)
        for ((x, y) in points) {
            if (x in 0 until MatrixGeometry.SIZE && y in 0 until MatrixGeometry.SIZE) {
                grid[y * MatrixGeometry.SIZE + x] = brightness
            }
        }
        return grid
    }
}
