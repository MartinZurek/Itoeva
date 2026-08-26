package com.notime.glyphcore.data

/**
 * Setzt ein kleines Bild mit seiner linken oberen Ecke auf ([x], [y]).
 *
 * `#` ist eine leuchtende Zelle, alles andere bleibt dunkel. Punkte ausserhalb des Rasters fallen
 * still weg - das ist gewollt, damit ein Motiv am Rand ein- und ausfahren kann, ohne dass jede
 * Position von Hand beschnitten werden muss.
 *
 * **Warum ASCII statt Koordinatenlisten:** Eine Zeile wie `listOf(5 to 4, 6 to 4, 7 to 4)` ist
 * kompakt, aber man sieht ihr nicht an, was sie zeichnet - und genau daran ist in diesem Katalog
 * schon Pixelarbeit gescheitert. `sprite(5, 4, "###")` liest sich wie das, was es ist.
 *
 * Zum Ansehen des Ergebnisses gibt es `AnimationPreviewTest`, das jedes Motiv als Kontaktbogen
 * nach `core/build/preview/` schreibt.
 */
internal fun sprite(x: Int, y: Int, vararg rows: String): List<Pair<Int, Int>> =
    rows.flatMapIndexed { dy, row ->
        row.mapIndexedNotNull { dx, c ->
            if (c == '#') (x + dx) to (y + dy) else null
        }
    }.filter { (px, py) -> px in 0 until ReminderFrameGrid.SIZE && py in 0 until ReminderFrameGrid.SIZE }
