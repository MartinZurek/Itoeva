package com.notime.glyphsim.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Der Test, der diese Umstellung ueberhaupt verantwortbar macht.**
 *
 * Dateiname und Schluessel jeder Einstellung sind der einzige Bezug zu dem, was bereits auf den
 * Geraeten der Nutzer liegt. Benennt jemand einen um - beim Aufraeumen, beim Vereinheitlichen,
 * beim Uebersetzen eines Namens ins Englische -, findet die App den alten Wert nicht mehr und
 * setzt die Einstellung stillschweigend auf ihre Vorgabe zurueck. Nichts stuerzt ab, nichts
 * meldet sich; der Nutzer stellt irgendwann fest, dass sein Avatar gewechselt hat oder der
 * Dock-Modus wieder an ist.
 *
 * Genau deshalb stehen die Namen hier **noch einmal als Literale**. Das ist keine Verdopplung aus
 * Versehen: der Test soll nicht pruefen, ob der Katalog mit sich selbst uebereinstimmt, sondern ob
 * er noch mit dem uebereinstimmt, was frueher in den vierzehn Prefs-Objekten stand. Eine Aenderung
 * am Katalog muss hier weh tun.
 *
 * Wer einen Namen wirklich aendern will, braucht eine Migration (alten Wert lesen, unter neuem
 * Namen schreiben, alten loeschen) - und darf diesen Test erst danach anpassen.
 */
class SettingsCatalogTest {

    private fun pruefe(setting: Setting<*>, datei: String, schluessel: String) {
        assertEquals("Dateiname von $schluessel", datei, setting.file)
        assertEquals("Schluessel in $datei", schluessel, setting.key)
    }

    // --- Modus ---------------------------------------------------------------------------------

    @Test
    fun `Dock-Modus liegt weiterhin an seinem historischen Ort`() {
        pruefe(SettingsCatalog.DockModeEnabled, "dock_mode_prefs", "dock_mode_enabled")
        assertEquals(false, SettingsCatalog.DockModeEnabled.default)
    }

    @Test
    fun `Nur-Uhr liegt weiterhin an seinem historischen Ort`() {
        pruefe(SettingsCatalog.WatchOnly, "watch_mode_prefs", "watch_only")
        assertEquals(false, SettingsCatalog.WatchOnly.default)
    }

    /**
     * Der Spielmodus liegt als einzige Einstellung in einer Datei des gemeinsamen Kerns - der
     * Planer dort muss sie lesen koennen, ohne die App zu kennen. Wer sie ins :app-sim-Modul
     * zoege, braeche den Planer, und zwar lautlos.
     */
    @Test
    fun `Spielmodus liegt in der Datei des gemeinsamen Kerns`() {
        pruefe(SettingsCatalog.PlayModeActive, "play_mode_state", "play_mode_active")
        assertEquals(false, SettingsCatalog.PlayModeActive.default)
    }

    // --- Darstellung ---------------------------------------------------------------------------

    @Test
    fun `Darstellungseinstellungen liegen weiterhin an ihren historischen Orten`() {
        pruefe(SettingsCatalog.ClockStyle, "clock_style_prefs", "style")
        pruefe(SettingsCatalog.Language, "language_prefs", "language")
        pruefe(SettingsCatalog.AllowRotation, "orientation_prefs", "allow_rotation")
        assertEquals(false, SettingsCatalog.AllowRotation.default)
    }

    @Test
    fun `Dock-Helligkeit behaelt Ort und Vorgabe`() {
        pruefe(SettingsCatalog.DockBrightnessOverride, "dock_brightness_prefs", "override_enabled")
        pruefe(SettingsCatalog.DockBrightness, "dock_brightness_prefs", "brightness")
        assertEquals(0.3f, SettingsCatalog.DockBrightness.default, 0.0001f)
    }

    @Test
    fun `Dock-Anordnung behaelt Ort und Vorgaben`() {
        pruefe(SettingsCatalog.DockSizeDp, "dock_layout_prefs", "size_dp")
        pruefe(SettingsCatalog.DockOffsetFractionX, "dock_layout_prefs", "offset_fraction_x")
        pruefe(SettingsCatalog.DockOffsetFractionY, "dock_layout_prefs", "offset_fraction_y")
        assertEquals(168f, SettingsCatalog.DockSizeDp.default, 0.0001f)
        assertEquals(0.5f, SettingsCatalog.DockOffsetFractionX.default, 0.0001f)
        assertEquals(0.5f, SettingsCatalog.DockOffsetFractionY.default, 0.0001f)
    }

    // --- Avatar ---------------------------------------------------------------------------------

    @Test
    fun `Avatar und Profil liegen weiterhin an ihren historischen Orten`() {
        pruefe(SettingsCatalog.AvatarSpecies, "avatar_species_prefs", "species")
        pruefe(SettingsCatalog.ActiveProfileId, "reminder_profile_prefs", "active_profile_id")
        pruefe(SettingsCatalog.MoodEnabled, "mood_prefs", "mood_enabled")
    }

    /**
     * `null` ist bei diesen dreien eine eigene Aussage - "noch nie gesetzt" - und nicht dasselbe
     * wie der erste Aufzaehlungswert. Am Avatar haengt daran der Erstkontakt.
     */
    @Test
    fun `Aufzaehlungen unterscheiden nie gesetzt von ihrem ersten Wert`() {
        assertEquals(null, SettingsCatalog.AvatarSpecies.default)
        assertEquals(null, SettingsCatalog.ClockStyle.default)
        assertEquals(null, SettingsCatalog.Language.default)
    }

    // --- Aufnahme und Erstkontakt ----------------------------------------------------------------

    @Test
    fun `Aufnahme und Erstkontakt liegen weiterhin an ihren historischen Orten`() {
        pruefe(SettingsCatalog.ClipRecordingEnabled, "clip_prefs", "recording_enabled")
        pruefe(SettingsCatalog.TappedAvatar, "onboarding_prefs", "tapped_avatar")
        pruefe(SettingsCatalog.TappedClock, "onboarding_prefs", "tapped_clock")
        pruefe(SettingsCatalog.PlayModeIntroSeen, "play_mode_prefs", "intro_seen")
    }

    // --- Die Welt des Wesens ----------------------------------------------------------------------

    /**
     * Diese vier werden noch direkt von ihren Objekten gelesen; der Katalog dokumentiert ihren
     * Ablageort (Phase 1d). Gerade weil sie NICHT ueber den Store laufen, ist dieser Test der
     * einzige Ort, an dem eine Umbenennung auffiele.
     */
    @Test
    fun `die Spielwelt-Eintraege liegen an ihren historischen Orten`() {
        pruefe(SettingsCatalog.PlayCoins, "play_wallet", "coins")
        pruefe(SettingsCatalog.PlayPantryLevel, "play_pantry", "level")
        pruefe(SettingsCatalog.PlayForcedWeather, "play_weather", "forced")
        pruefe(SettingsCatalog.PlayTimeLapseSpeed, "play_time_lapse", "speed")

        // Die Vorgaben stammen aus PlayWallet.INITIAL bzw. PlayPantry.FULL.
        assertEquals(2, SettingsCatalog.PlayCoins.default)
        assertEquals(3, SettingsCatalog.PlayPantryLevel.default)
    }

    // --- Der Katalog als Ganzes -------------------------------------------------------------------

    /**
     * Zwei Einstellungen mit derselben Datei UND demselben Schluessel wuerden sich gegenseitig
     * ueberschreiben - ein Fehler, den man beim Hinzufuegen leicht macht (Kopieren einer Zeile,
     * Schluessel zu aendern vergessen) und im Betrieb kaum bemerkt.
     */
    @Test
    fun `keine zwei Einstellungen teilen sich Datei und Schluessel`() {
        val paare = SettingsCatalog.all.map { it.file to it.key }
        val doppelte = paare.groupBy { it }.filterValues { it.size > 1 }.keys

        assertEquals("Doppelt belegt: $doppelte", emptySet<Pair<String, String>>(), doppelte)
    }

    /**
     * Der Katalog beansprucht vollstaendig zu sein - `all` ist die Grundlage fuer kuenftige
     * Migrationen und fuer die Pruefung der Sicherungsregeln. Wer eine Einstellung ergaenzt und
     * `all` vergisst, macht sie fuer beides unsichtbar.
     */
    @Test
    fun `all enthaelt jede einzeln gepruefte Einstellung`() {
        val einzeln = listOf(
            SettingsCatalog.DockModeEnabled, SettingsCatalog.WatchOnly, SettingsCatalog.PlayModeActive,
            SettingsCatalog.ClockStyle, SettingsCatalog.Language, SettingsCatalog.AllowRotation,
            SettingsCatalog.DockBrightnessOverride, SettingsCatalog.DockBrightness,
            SettingsCatalog.DockSizeDp, SettingsCatalog.DockOffsetFractionX, SettingsCatalog.DockOffsetFractionY,
            SettingsCatalog.AvatarSpecies, SettingsCatalog.MoodEnabled, SettingsCatalog.ActiveProfileId,
            SettingsCatalog.ClipRecordingEnabled,
            SettingsCatalog.TappedAvatar, SettingsCatalog.TappedClock, SettingsCatalog.PlayModeIntroSeen,
            SettingsCatalog.PlayCoins, SettingsCatalog.PlayPantryLevel,
            SettingsCatalog.PlayForcedWeather, SettingsCatalog.PlayTimeLapseSpeed
        )
        einzeln.forEach {
            assertTrue("Fehlt in SettingsCatalog.all: ${it.file}/${it.key}", it in SettingsCatalog.all)
        }
        assertEquals("all enthaelt mehr als hier geprueft wird", einzeln.size, SettingsCatalog.all.size)
    }
}
