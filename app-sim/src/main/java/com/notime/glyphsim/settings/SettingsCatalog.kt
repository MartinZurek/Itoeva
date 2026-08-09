package com.notime.glyphsim.settings

/**
 * Der vollstaendige Katalog aller Einstellungen - Ablageort, Schluessel und Vorgabewert an genau
 * einer Stelle.
 *
 * ## Warum
 *
 * Die Einstellungen lagen in vierzehn `object`s verstreut, jedes mit seiner eigenen
 * SharedPreferences-Datei, seinem eigenen `prefs(context)`-Helfer und seinen eigenen
 * Schluesselkonstanten - dieselben fuenf Zeilen vierzehnmal. Wer wissen wollte, WO eine
 * Einstellung liegt, musste die passende Datei finden; wer alle Ablageorte brauchte (fuer die
 * Sicherungsregeln, fuer eine Migration, fuer eine Exportfunktion), musste sie zusammensuchen.
 *
 * ## Was das mit DataStore zu tun hat
 *
 * Der Plan sieht fuer Phase 3.2 DataStore vor. Das scheitert derzeit an etwas Konkretem: es liest
 * **ausschliesslich asynchron**, und es gibt 45 synchrone Lesestellen - darunter im
 * Widget-Provider (dessen `onUpdate` keine Coroutine kennt), im Planer und im Watchdog. Dort
 * `runBlocking` einzusetzen waere schlechter als der Ist-Zustand: genau so entstehen ANRs beim
 * Aktualisieren eines Widgets.
 *
 * Dieser Katalog ist der Schritt davor. Er aendert nichts am Verhalten und nichts am Speicherort -
 * er sammelt nur das Wissen darueber ein. Sobald die Aufrufstellen asynchron sind, ist der Wechsel
 * auf DataStore eine Aenderung an [SettingsStore] und an dieser Datei, statt an 45 Stellen.
 *
 * ## Die eiserne Regel
 *
 * **Dateiname und Schluessel duerfen sich nie aendern.** Sie sind der einzige Bezug zu dem, was
 * bereits auf den Geraeten der Nutzer liegt; eine Umbenennung setzt die betroffene Einstellung
 * stillschweigend auf ihren Vorgabewert zurueck. `SettingsCatalogTest` haelt jeden Namen einzeln
 * fest und schlaegt an, sobald einer sich bewegt.
 */
sealed interface Setting<T> {
    /** Name der SharedPreferences-Datei. Historisch gewachsen - siehe die eiserne Regel oben. */
    val file: String

    /** Schluessel innerhalb der Datei. */
    val key: String

    /** Was gilt, solange nichts gespeichert ist. */
    val default: T

    data class Bool(
        override val file: String,
        override val key: String,
        override val default: Boolean
    ) : Setting<Boolean>

    data class Float(
        override val file: String,
        override val key: String,
        override val default: kotlin.Float
    ) : Setting<kotlin.Float>

    /**
     * Zeichenketten mit `null` als Vorgabe: "noch nie gesetzt" ist bei Aufzaehlungen eine eigene
     * Aussage und nicht dasselbe wie "steht auf dem ersten Wert" (siehe AvatarSpeciesPrefs, wo
     * daran der Erstkontakt haengt).
     */
    data class OptionalString(
        override val file: String,
        override val key: String
    ) : Setting<String?> {
        override val default: String? = null
    }
}

/**
 * Alle Einstellungen der App.
 *
 * Gruppiert nach dem, was sie betreffen - nicht nach ihrer Datei. Die Dateien sind ein
 * Ablagedetail und stehen deshalb rechts, nicht in der Gliederung.
 */
object SettingsCatalog {

    // --- Modus ---------------------------------------------------------------------------------

    /** Dock-Modus (Vollbild-Nachttisch-Anzeige). */
    val DockModeEnabled = Setting.Bool("dock_mode_prefs", "dock_mode_enabled", default = false)

    /** "Nur Uhr" - hat Vorrang vor allem anderen, siehe WatchModePrefs. */
    val WatchOnly = Setting.Bool("watch_mode_prefs", "watch_only", default = false)

    /**
     * Spielmodus.
     *
     * Liegt als einzige in einer Datei des gemeinsamen Kerns (`play_mode_state`) statt in einer
     * des :app-sim-Moduls: der Planer im Kern muss sie lesen koennen, ohne die App zu kennen.
     */
    val PlayModeActive = Setting.Bool("play_mode_state", "play_mode_active", default = false)

    // --- Darstellung ---------------------------------------------------------------------------

    val ClockStyle = Setting.OptionalString("clock_style_prefs", "style")
    val Language = Setting.OptionalString("language_prefs", "language")
    val AllowRotation = Setting.Bool("orientation_prefs", "allow_rotation", default = false)

    val DockBrightnessOverride = Setting.Bool("dock_brightness_prefs", "override_enabled", default = false)
    val DockBrightness = Setting.Float("dock_brightness_prefs", "brightness", default = 0.3f)

    val DockSizeDp = Setting.Float("dock_layout_prefs", "size_dp", default = 168f)
    val DockOffsetFractionX = Setting.Float("dock_layout_prefs", "offset_fraction_x", default = 0.5f)
    val DockOffsetFractionY = Setting.Float("dock_layout_prefs", "offset_fraction_y", default = 0.5f)

    // --- Avatar ---------------------------------------------------------------------------------

    val AvatarSpecies = Setting.OptionalString("avatar_species_prefs", "species")

    /** Ob die Stimmung des Avatars auf sein Verhalten wirkt. */
    val MoodEnabled = Setting.Bool("mood_prefs", "mood_enabled", default = false)

    /** Profil-Id im gemeinsamen Kern - dort neutral, hier ist es der Avatar. */
    val ActiveProfileId = Setting.OptionalString("reminder_profile_prefs", "active_profile_id")

    // --- Aufnahme -------------------------------------------------------------------------------

    val ClipRecordingEnabled = Setting.Bool("clip_prefs", "recording_enabled", default = false)

    // --- Erstkontakt ----------------------------------------------------------------------------

    val TappedAvatar = Setting.Bool("onboarding_prefs", "tapped_avatar", default = false)
    val TappedClock = Setting.Bool("onboarding_prefs", "tapped_clock", default = false)
    val PlayModeIntroSeen = Setting.Bool("play_mode_prefs", "intro_seen", default = false)

    /** Alle Eintraege - Grundlage fuer [SettingsCatalogTest] und kuenftige Migrationen. */
    val all: List<Setting<*>> = listOf(
        DockModeEnabled, WatchOnly, PlayModeActive,
        ClockStyle, Language, AllowRotation,
        DockBrightnessOverride, DockBrightness,
        DockSizeDp, DockOffsetFractionX, DockOffsetFractionY,
        AvatarSpecies, MoodEnabled, ActiveProfileId,
        ClipRecordingEnabled,
        TappedAvatar, TappedClock, PlayModeIntroSeen
    )
}
