package com.notime.glyphsim.matrix

/**
 * **Der Tag daemmert, statt umzuschalten.**
 *
 * Bis hierhin kannte die Welt vier Helligkeiten (siehe [PlayAmbientActivity.DayPhase]) - um
 * 18:00 Uhr sprang der ganze Raum von einem Moment auf den anderen eine Stufe dunkler, und im
 * selben Takt gingen in der Strasse alle Fenster gleichzeitig an. Auf dem Nachttisch faellt das
 * kaum auf. In einem Stream, der rund um die Uhr laeuft, ist genau dieser Moment der schoenste des
 * Tages - und er war ein Schnitt.
 *
 * Zwei Kurven, beide reine Rechnung aus der Minute des Tages:
 *
 * - [atmosphere] - wie hell der RAUM steht. Stueckweise linear zwischen festen Stuetzstellen,
 *   die in der Mitte jeder Phase genau die bisherigen Werte treffen: Morgens 0,92, mittags 1,
 *   abends 0,74, nachts 0,58. Nur die Uebergaenge dazwischen sind neu - eine Morgendaemmerung
 *   von gut anderthalb Stunden, eine Abenddaemmerung ebenso.
 * - [windowLit] - ob EIN bestimmtes Fenster gerade brennt. Jedes Fenster hat seine eigene
 *   Uhrzeit, zu der drinnen jemand heimkommt, und seine eigene, zu der er schlafen geht; einige
 *   stehen frueh auf. Abends gehen die Lichter so nach und nach an, nachts nach und nach aus -
 *   jeden Tag dieselben, weil es die Gewohnheiten derselben Nachbarn sind.
 */
object PlayDaylight {

    private const val DAY = 24 * 60

    /**
     * Stuetzstellen (Minute des Tages to Raumhelligkeit). Die Phasenmitten liegen auf den alten
     * Stufenwerten, damit sich an einem Standbild nichts aendert - nur an der Bewegung dazwischen.
     */
    private val CURVE = listOf(
        0 to 0.58f,
        5 * 60 to 0.58f,           // tiefste Nacht bis fuenf
        6 * 60 + 40 to 0.92f,      // Morgendaemmerung
        10 * 60 + 30 to 0.92f,
        11 * 60 + 30 to 1f,        // der Vormittag wird Tag
        17 * 60 to 1f,
        18 * 60 + 40 to 0.74f,     // Abenddaemmerung
        21 * 60 + 30 to 0.74f,
        23 * 60 to 0.58f,          // Nacht
        DAY to 0.58f
    )

    /** Raumhelligkeit zur Minute [minuteOfDay] (wird auf 0 bis 1439 gefaltet). */
    fun atmosphere(minuteOfDay: Int): Float {
        val m = Math.floorMod(minuteOfDay, DAY)
        for (i in 0 until CURVE.size - 1) {
            val (m0, v0) = CURVE[i]
            val (m1, v1) = CURVE[i + 1]
            if (m in m0..m1) {
                if (m1 == m0) return v1
                val t = (m - m0).toFloat() / (m1 - m0)
                return v0 + (v1 - v0) * t
            }
        }
        return CURVE.last().second
    }

    /**
     * Die bisherigen vier Stufen - fuer alle Stellen, die keine Uhrzeit, nur eine Phase kennen
     * (Vorschau-Werkzeuge, Clips aus gespeicherten Phasen).
     */
    fun atmosphere(dayPhase: PlayAmbientActivity.DayPhase): Float = when (dayPhase) {
        PlayAmbientActivity.DayPhase.MIDDAY -> 1f
        PlayAmbientActivity.DayPhase.MORNING -> 0.92f
        PlayAmbientActivity.DayPhase.EVENING -> 0.74f
        // Nicht tiefer: Bei 0,5 sank die Bodenlinie unter die Sichtbarkeitsschwelle, und die Figur
        // schien nachts wieder im Schwarzen zu schweben. Der Raum darf zurueckweichen, aber der
        // Boden muss bleiben.
        PlayAmbientActivity.DayPhase.NIGHT -> 0.58f
    }

    /**
     * **Brennt Fenster [key] um [minuteOfDay]?**
     *
     * [key] identifiziert das Fenster dauerhaft (Haus und Oeffnung). Daraus werden drei feste
     * Gewohnheiten gerechnet:
     *
     * - **Heimkommen** zwischen 17:30 und 21:00 Uhr.
     * - **Schlafengehen** zwischen 22:30 und 02:00 Uhr; jedes siebte Fenster ist eine Nachteule
     *   und bleibt bis drei.
     * - **Fruehaufsteher** - jedes fuenfte Fenster geht morgens zwischen 5:30 und 6:30 Uhr an und
     *   vor acht wieder aus.
     *
     * Etwa jedes vierte Fenster bleibt den ganzen Abend dunkel: Dort ist niemand zu Hause. Ein
     * voll erleuchtetes Haus saehe aus wie ein Buerogebaeude.
     */
    fun windowLit(minuteOfDay: Int, key: Int): Boolean {
        val m = Math.floorMod(minuteOfDay, DAY)
        val h = mix(key)
        val home = h % 4 != 0
        // Minuten seit Mittag - damit liegt "Heimkommen bis nach Mitternacht" auf einer Achse.
        val sinceNoon = Math.floorMod(m - 12 * 60, DAY)
        val on = 5 * 60 + 30 + (h ushr 3) % 210                    // 17:30 .. 21:00
        val nightOwl = (h ushr 11) % 7 == 0
        val off = if (nightOwl) 15 * 60 else 10 * 60 + 30 + (h ushr 7) % 210   // 22:30 .. 02:00 / 03:00
        if (home && sinceNoon in on until off) return true
        val earlyRiser = (h ushr 15) % 5 == 0
        if (earlyRiser) {
            val rise = 5 * 60 + 30 + (h ushr 5) % 60                // 5:30 .. 6:30
            val leave = 7 * 60 + 15 + (h ushr 9) % 40               // 7:15 .. 7:55
            if (m in rise until leave) return true
        }
        return false
    }

    /**
     * Wo die tief stehende Sonne ist: [xFraction] der Bildbreite, [cellsAboveFloor] ueber der
     * Bodenlinie (negativ heisst: schon darunter, also unsichtbar).
     */
    data class Sun(val xFraction: Float, val cellsAboveFloor: Int)

    /**
     * **Sonnenaufgang und Sonnenuntergang** - `null` den Rest des Tages.
     *
     * Die Sonne steht nur in den beiden Stunden am Rand des Tages im Bild, und zwar tief: Morgens
     * steigt sie links ueber den Boden (5:40 bis 7:20 Uhr), abends sinkt sie rechts hinter ihn
     * (17:10 bis 18:50 Uhr). Mittags gibt es keine - eine Scheibe hoch oben waere nur ein heller
     * Fleck. Tief am Horizont dagegen ist sie das Bild, an dem man einen Tagesanfang und ein
     * Tagesende erkennt, ohne dass es jemand sagt.
     */
    fun sun(minuteOfDay: Int): Sun? {
        val m = Math.floorMod(minuteOfDay, DAY)
        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
        return when (m) {
            in SUNRISE_FROM until SUNRISE_TO -> {
                val t = (m - SUNRISE_FROM).toFloat() / (SUNRISE_TO - SUNRISE_FROM)
                Sun(0.18f, lerp(-3f, SUN_PEAK_CELLS, t).toInt())
            }
            in SUNSET_FROM until SUNSET_TO -> {
                val t = (m - SUNSET_FROM).toFloat() / (SUNSET_TO - SUNSET_FROM)
                Sun(0.80f, lerp(SUN_PEAK_CELLS, -3f, t).toInt())
            }
            else -> null
        }
    }

    private const val SUNRISE_FROM = 5 * 60 + 40
    private const val SUNRISE_TO = 7 * 60 + 20
    private const val SUNSET_FROM = 17 * 60 + 10
    private const val SUNSET_TO = 18 * 60 + 50
    private const val SUN_PEAK_CELLS = 10f

    /** Ein kleiner Ganzzahl-Mischer: benachbarte Fenster bekommen unabhaengige Gewohnheiten. */
    private fun mix(key: Int): Int {
        var x = key * -0x61c88647 + 0x7f4a7c15
        x = (x xor (x ushr 16)) * 0x45d9f3b
        x = x xor (x ushr 16)
        return x and 0x7fffffff
    }
}
