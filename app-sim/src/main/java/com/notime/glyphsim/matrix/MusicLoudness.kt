package com.notime.glyphsim.matrix

import kotlin.math.pow

/**
 * **Jedes Stueck gleich laut** - der Ausgleich zwischen den erzeugten Dateien.
 *
 * Gemessen am 2026-09-24 lagen die Stuecke zwischen etwa -19,5 und -10,8 LUFS. Der Player spielte
 * jede Datei mit derselben Lautstaerke; ein Wechsel von einem leisen zu einem lauten Stueck
 * klang wie jemand, der am Regler dreht. Hier wird jedes Stueck auf [TARGET_LUFS] angeglichen.
 *
 * Die Messwerte stehen in [MusicLoudnessTable], erzeugt von `tools/music/loudness_table.py`.
 * Ein Stueck ohne Messwert (etwa ganz frisch erzeugt) spielt unveraendert - nie stumm, nie
 * uebersteuert.
 */
object MusicLoudness {

    /** Das gemeinsame Ziel - die Mitte der bisher ausgelieferten Stuecke. */
    const val TARGET_LUFS = -16.0

    /**
     * Hoechstens so viel lauter. Der Player regelt ohnehin nur nach unten (er spielt mit 0,35),
     * aber ein sehr leises Stueck um mehr als 6 dB anzuheben, hiesse sein Rauschen mit anzuheben.
     */
    const val MAX_BOOST_DB = 6.0

    /** Hoechstens so viel leiser - ein Stueck soll leiser werden, nicht verschwinden. */
    const val MAX_CUT_DB = 9.0

    /** Der lineare Faktor fuer die Datei [resource] (z. B. `itoeva_sport_03`), 1 ohne Messwert. */
    fun gainFor(resource: String): Float =
        gainForLufs(MusicLoudnessTable.MEASURED_LUFS[resource])

    /** Rein, damit sich die Rechnung ohne Tabelle pruefen laesst. */
    fun gainForLufs(measuredLufs: Double?): Float {
        if (measuredLufs == null || measuredLufs.isNaN() || measuredLufs.isInfinite()) return 1f
        val db = (TARGET_LUFS - measuredLufs).coerceIn(-MAX_CUT_DB, MAX_BOOST_DB)
        return 10.0.pow(db / 20.0).toFloat()
    }
}
