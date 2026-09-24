package com.notime.glyphsim.matrix

/**
 * Gemessene Lautheit (LUFS) jeder ausgelieferten Musikdatei - ERZEUGT von
 * `tools/music/loudness_table.py`, nicht von Hand bearbeiten. Siehe [MusicLoudness].
 */
internal object MusicLoudnessTable {
    val MEASURED_LUFS: Map<String, Double> = mapOf(
        "itoeva_city_01" to -17.6,
        "itoeva_city_02" to -12.4,
        "itoeva_city_03" to -15.8,
        "itoeva_dream_01" to -17.4,
        "itoeva_home_evening_01" to -17.1,
        "itoeva_home_evening_02" to -17.5,
        "itoeva_main_day_01" to -15.0,
        "itoeva_main_day_02" to -14.6,
        "itoeva_main_day_03" to -19.5,
        "itoeva_morning_01" to -19.0,
        "itoeva_morning_02" to -22.6,
        "itoeva_morning_03" to -15.1,
        "itoeva_sport_01" to -16.1,
        "itoeva_sport_02" to -16.2,
        "itoeva_sport_03" to -10.8,
        "itoeva_sport_04" to -14.9,
        "itoeva_theme_01" to -17.0,
        "itoeva_theme_02" to -16.3,
        "itoeva_theme_03" to -17.9,
        "itoeva_theme_04" to -17.8,
        "itoeva_theme_05" to -14.6,
        "itoeva_theme_06" to -18.0,
    )
}
