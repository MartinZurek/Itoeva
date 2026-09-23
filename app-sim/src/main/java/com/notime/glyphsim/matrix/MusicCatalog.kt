package com.notime.glyphsim.matrix

import androidx.annotation.StringRes
import com.notime.glyphsim.R

/**
 * **Menschlich lesbare Angaben zu einem einzelnen Musikstueck - fuer die Musik-Bibliothek in den
 * Einstellungen, nicht fuer die Weltlogik.**
 *
 * [MusicResolver] und [com.notime.glyphsim.ui.PlayMusic] kennen nur Rolle und Variante - ein
 * Titel oder eine Speziesbeschriftung waere fuer sie Ballast, den sie nie brauchen. Diese Datei
 * ist die einzige Stelle, die einen Titel dazu kennt, damit man beim Hoertest sagen kann "das
 * meine ich" statt auf einen internen Ressourcennamen zeigen zu muessen.
 *
 * **Muss von Hand mit `music/manifest.json` synchron gehalten werden.** Genau dieselbe Grenze
 * wie bei jedem erzeugten Track: Die Audiodatei entsteht in einem eigenen, generierten Pull
 * Request, eine Uebersicht hier kann nicht automatisch mitwachsen. `MusicCatalogTest` prueft
 * wenigstens, dass ein vorhandener Titel zur richtigen Rolle passt und dass jede
 * Charakterthema-Variante genau die Spezies traegt, die [MusicRole.characterThemeVariant] auch
 * ihr zuordnen wuerde - beide duerfen nie auseinanderlaufen.
 */
object MusicCatalog {

    /** Ein einzelner Eintrag, wie ihn die Musik-Bibliothek zeilenweise anzeigt. */
    data class Track(
        val role: MusicRole,
        val variant: Int,
        @StringRes val titleRes: Int,
        val species: AvatarSpecies?
    )

    private val TITLES: Map<Pair<MusicRole, Int>, Int> = mapOf(
        (MusicRole.MORNING to 1) to R.string.music_title_morning_01,
        (MusicRole.MORNING to 2) to R.string.music_title_morning_02,
        (MusicRole.MAIN_DAY to 1) to R.string.music_title_main_day_01,
        (MusicRole.MAIN_DAY to 2) to R.string.music_title_main_day_02,
        (MusicRole.MAIN_DAY to 3) to R.string.music_title_main_day_03,
        (MusicRole.SPORT to 1) to R.string.music_title_sport_01,
        (MusicRole.SPORT to 2) to R.string.music_title_sport_02,
        (MusicRole.HOME_EVENING to 1) to R.string.music_title_home_evening_01,
        (MusicRole.HOME_EVENING to 2) to R.string.music_title_home_evening_02,
        (MusicRole.CHARACTER_THEME to 1) to R.string.music_title_theme_puffling,
        (MusicRole.CHARACTER_THEME to 2) to R.string.music_title_theme_starlet,
        (MusicRole.CHARACTER_THEME to 3) to R.string.music_title_theme_wyrmling,
        (MusicRole.CHARACTER_THEME to 4) to R.string.music_title_theme_fennec,
        (MusicRole.CHARACTER_THEME to 5) to R.string.music_title_theme_gloop,
        (MusicRole.CHARACTER_THEME to 6) to R.string.music_title_theme_hootlet
    )

    /**
     * Rollen in der Reihenfolge, in der die Bibliothek sie zeigt - vom fruehen Morgen zum
     * persoenlichen Thema, nicht die Deklarationsreihenfolge des Enums (die haelt sich an keine
     * Tageszeit).
     */
    val DISPLAY_ORDER: List<MusicRole> = listOf(
        MusicRole.MORNING,
        MusicRole.MAIN_DAY,
        MusicRole.SPORT,
        MusicRole.HOME_EVENING,
        MusicRole.CHARACTER_THEME,
        MusicRole.DREAM
    )

    /** Der Titel einer Variante, oder `null`, wenn diese Kombination hier noch nicht bekannt ist. */
    @StringRes
    fun titleRes(role: MusicRole, variant: Int): Int? = TITLES[role to variant]

    /**
     * Die Spezies, deren persoenliches Thema [variant] ist - nur fuer [MusicRole.CHARACTER_THEME],
     * sonst `null`. Direkt aus [AvatarSpecies.entries] statt einer eigenen Tabelle, aus demselben
     * Grund wie [MusicRole.characterThemeVariant]: Eine zweite Zuordnung koennte von der ersten
     * abweichen, ohne dass es auffiele.
     */
    fun speciesFor(role: MusicRole, variant: Int): AvatarSpecies? =
        if (role == MusicRole.CHARACTER_THEME) AvatarSpecies.entries.getOrNull(variant - 1) else null

    /** Kurzbeschreibung, wann diese Rolle in der Welt zum Zug kommt. */
    @StringRes
    fun roleDescriptionRes(role: MusicRole): Int = when (role) {
        MusicRole.MORNING -> R.string.music_role_morning
        MusicRole.MAIN_DAY -> R.string.music_role_main_day
        MusicRole.SPORT -> R.string.music_role_sport
        MusicRole.HOME_EVENING -> R.string.music_role_home_evening
        MusicRole.CHARACTER_THEME -> R.string.music_role_character_theme
        MusicRole.DREAM -> R.string.music_role_dream
    }
}
