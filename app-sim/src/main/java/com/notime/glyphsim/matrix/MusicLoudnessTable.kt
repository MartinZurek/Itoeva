package com.notime.glyphsim.matrix

/**
 * Gemessene Lautheit (LUFS) jeder ausgelieferten Musikdatei - ERZEUGT von
 * `tools/music/loudness_table.py`, nicht von Hand bearbeiten. Siehe [MusicLoudness].
 */
internal object MusicLoudnessTable {
    val MEASURED_LUFS: Map<String, Double> = mapOf(
        "itoeva_arcade_01" to -14.1,
        "itoeva_arcade_02" to -18.2,
        "itoeva_ballgame_01" to -13.0,
        "itoeva_city_01" to -15.6,
        "itoeva_city_02" to -14.7,
        "itoeva_city_03" to -18.1,
        "itoeva_dream_01" to -16.2,
        "itoeva_fishing_01" to -17.3,
        "itoeva_home_evening_01" to -17.1,
        "itoeva_home_evening_02" to -15.8,
        "itoeva_main_day_01" to -18.4,
        "itoeva_main_day_02" to -17.4,
        "itoeva_main_day_03" to -18.0,
        "itoeva_morning_01" to -16.3,
        "itoeva_morning_02" to -19.1,
        "itoeva_morning_03" to -16.9,
        "itoeva_nature_01" to -13.3,
        "itoeva_nature_02" to -15.4,
        "itoeva_shop_01" to -15.0,
        "itoeva_sport_01" to -16.9,
        "itoeva_sport_02" to -15.3,
        "itoeva_sport_03" to -12.0,
        "itoeva_sport_04" to -13.3,
        "itoeva_theme_01" to -17.7,
        "itoeva_theme_02" to -15.2,
        "itoeva_theme_03" to -17.0,
        "itoeva_theme_04" to -15.0,
        "itoeva_theme_05" to -14.6,
        "itoeva_theme_06" to -17.1,
    )
}
