package com.notime.glyphcore.data

/**
 * Kantenlaenge des Rasters, in dem Erinnerungs-Animationen gespeichert und abgespielt werden.
 *
 * 13x13 ist die Aufloesung der echten Glyph Matrix des Nothing Phone (4a) Pro (laut
 * `Common.getDeviceMatrixLength()`, siehe :app) - dieselbe Zahl gilt deshalb fuer die
 * Hardware-Ausgabe wie fuer die simulierte Darstellung, und die in [LibraryAnimation.framesData]
 * abgelegten Punktlisten sind nur mit genau dieser Kantenlaenge korrekt zu interpretieren.
 *
 * Nur die reine Groesse liegt hier: wie ein Raster gezeichnet wird und welche Zellen innerhalb
 * des runden Ausschnitts liegen, bleibt Sache der App-Module (`matrix/MatrixGeometry.kt`), die
 * sich darin unterscheiden.
 */
object ReminderFrameGrid {
    const val SIZE = 13
}
