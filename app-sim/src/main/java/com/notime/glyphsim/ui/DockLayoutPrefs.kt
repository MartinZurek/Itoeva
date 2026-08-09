package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphsim.settings.SettingsCatalog
import com.notime.glyphsim.settings.SettingsStore

/**
 * Persistiert Position (als Bruchteil 0..1 der verfuegbaren Flaeche, damit sie bei
 * unterschiedlichen Bildschirmgroessen/Rotationen sinnvoll bleibt) und Groesse
 * (in dp) der Uhr im Dock-Modus (siehe [DockScreen]), damit Verschieben/Skalieren
 * per Geste einmalig passiert und nicht bei jedem Andocken neu eingestellt werden
 * muss.
 */
object DockLayoutPrefs {
    private const val PREFS_NAME = "dock_layout_prefs"
    private const val KEY_SIZE_DP = "size_dp"
    private const val KEY_OFFSET_FRACTION_X = "offset_fraction_x"
    private const val KEY_OFFSET_FRACTION_Y = "offset_fraction_y"

    // Entspricht ungefaehr der tatsaechlichen Darstellungsgroesse des Home-Screen-
    // Widgets (2x2-Zellen, siehe glyph_widget_info.xml) bzw. der Glyph Matrix auf dem
    // Nothing Phone (4a) Pro - deutlich groesser als der reine Fallback-Renderwert
    // (96dp) in GlyphClockWidgetProvider, der nur fuer den Fall ohne bekannte
    // Widget-Optionen greift.
    /**
     * Etwas kleiner als frueher (192dp): Im Dock steht neben der Uhr bei einer faelligen
     * Erinnerung der Avatar, und beide zusammen brauchen Platz. Bei 192dp blieb auf schmalen
     * Geraeten selbst mit verkleinertem Avatar kaum Luft, und die Platzierung wurde eng.
     * Wer es grosser mag, zieht die Uhr im Dock jederzeit auf - der Wert hier ist nur der
     * Startpunkt.
     */
    const val DEFAULT_SIZE_DP = 168f
    const val MIN_SIZE_DP = 64f
    const val MAX_SIZE_DP = 400f
    private const val DEFAULT_FRACTION = 0.5f

    fun getSizeDp(context: Context): Float =
        SettingsStore.read(context, SettingsCatalog.DockSizeDp)

    fun getOffsetFraction(context: Context): Pair<Float, Float> {
        return SettingsStore.read(context, SettingsCatalog.DockOffsetFractionX) to
            SettingsStore.read(context, SettingsCatalog.DockOffsetFractionY)
    }

    /**
     * Groesse und Position gehoeren zusammen und wandern in EINEM Vorgang in die Ablage - sonst
     * kann ein Beobachter den Zwischenstand sehen und die Uhr springt zweimal.
     */
    fun save(context: Context, sizeDp: Float, offsetFractionX: Float, offsetFractionY: Float) {
        SettingsStore.writeFloats(
            context,
            SettingsCatalog.DockSizeDp to sizeDp,
            SettingsCatalog.DockOffsetFractionX to offsetFractionX,
            SettingsCatalog.DockOffsetFractionY to offsetFractionY
        )
    }
}
