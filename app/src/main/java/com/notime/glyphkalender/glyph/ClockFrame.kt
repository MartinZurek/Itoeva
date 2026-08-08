package com.notime.glyphkalender.glyph

import android.content.Context
import com.nothing.ketchum.Common
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Zeichnet die aktuelle Uhrzeit auf die Matrix. Gemeinsam genutzt von
 * ClockGlyphToyService (laufender Sekundentakt) und ReminderGlyphService
 * (letzter Frame nach einer Animation), damit der Uebergang zurueck zur Uhr
 * ohne Schwarzbild-Luecke passiert.
 *
 * Stunden/Minuten zweizeilig statt "HH:mm" einzeilig: Jede Ziffer ist im SDK ein
 * festes 4x6-Pixel-Glyph, der Doppelpunkt 1 breit - "HH:mm" braeuchte also ca.
 * 21 Spalten Breite, das (4a) Pro hat aber nur ein 13x13-Raster
 * (Common.getDeviceMatrixLength()), der Text wuerde rechts abgeschnitten.
 * Zwei Zeilen zu je zwei Ziffern (4+1+4=9 Spalten) passen dagegen locker rein.
 * Rechnerisch wuerden zwei 6 Zeilen hohe Bloecke plus 1 Zeile Abstand exakt die
 * 13 Zeilen fuellen - die Matrix ist aber rund (kreisfoermig ins Quadrat
 * einbeschrieben), die aeusserste Zeile ganz unten hat also kaum/keine LEDs.
 * Deshalb kein Abstand zwischen den Zeilen: unterer Block direkt unter dem
 * oberen, die letzte (kaum sichtbare) Randzeile bleibt frei.
 */
object ClockFrame {
    private val hourFormatter = DateTimeFormatter.ofPattern("HH")
    private val minuteFormatter = DateTimeFormatter.ofPattern("mm")

    private const val DIGIT_PAIR_WIDTH = 9 // 4 (Ziffer) + 1 (Abstand) + 4 (Ziffer)
    private const val ROW_HEIGHT = 6 // Ziffernhoehe

    fun draw(context: Context, glyphMatrixManager: GlyphMatrixManager) {
        val size = Common.getDeviceMatrixLength()
        val xOffset = (size - DIGIT_PAIR_WIDTH) / 2
        val now = LocalTime.now()

        val hourObject = GlyphMatrixObject.Builder()
            .setText(now.format(hourFormatter))
            .setPosition(xOffset, 0)
            .build()
        val minuteObject = GlyphMatrixObject.Builder()
            .setText(now.format(minuteFormatter))
            .setPosition(xOffset, ROW_HEIGHT)
            .build()

        val frame = GlyphMatrixFrame.Builder()
            .addTop(hourObject)
            .addLow(minuteObject)
            .build(context)
        glyphMatrixManager.setMatrixFrame(frame.render())
    }
}
