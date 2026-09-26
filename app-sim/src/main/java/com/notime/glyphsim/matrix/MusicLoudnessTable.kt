package com.notime.glyphsim.matrix

/**
 * Gemessene Lautheit (LUFS) jeder ausgelieferten Musikdatei - ERZEUGT von
 * `tools/music/loudness_table.py`, nicht von Hand bearbeiten. Siehe [MusicLoudness].
 */
internal object MusicLoudnessTable {
    val MEASURED_LUFS: Map<String, Double> = mapOf(
        "itoeva_arcade_01" to -17.5,
        "itoeva_arcade_02" to -18.2,
        "itoeva_ballgame_01" to -14.1,
        "itoeva_city_01" to -15.2,
        "itoeva_city_02" to -14.3,
        "itoeva_city_03" to -19.1,
        "itoeva_dream_01" to -14.0,
        "itoeva_fishing_01" to -14.8,
        "itoeva_home_evening_01" to -18.1,
        "itoeva_home_evening_02" to -16.2,
        "itoeva_main_day_01" to -15.7,
        "itoeva_main_day_02" to -17.4,
        "itoeva_main_day_03" to -17.8,
        "itoeva_morning_01" to -20.3,
        "itoeva_morning_02" to -22.7,
        "itoeva_morning_03" to -16.9,
        "itoeva_nature_01" to -15.2,
        "itoeva_nature_02" to -12.9,
        "itoeva_shop_01" to -16.3,
        "itoeva_sport_01" to -18.6,
        "itoeva_sport_02" to -15.3,
        "itoeva_sport_03" to -12.0,
        "itoeva_sport_04" to -13.3,
        "itoeva_theme_01" to -15.9,
        "itoeva_theme_02" to -15.4,
        "itoeva_theme_03" to -17.4,
        "itoeva_theme_04" to -15.0,
        "itoeva_theme_05" to -14.6,
        "itoeva_theme_06" to -15.0,
    )
}
