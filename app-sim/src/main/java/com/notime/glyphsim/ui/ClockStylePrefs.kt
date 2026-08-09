package com.notime.glyphsim.ui

import android.content.Context
import android.content.SharedPreferences
import com.notime.glyphsim.matrix.ClockStyle
import com.notime.glyphsim.settings.SettingsCatalog
import com.notime.glyphsim.settings.SettingsStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

/** Persistiert das vom Nutzer gewaehlte Ziffernblatt-Design (siehe [ClockStyle]), Standard CLASSIC. */
object ClockStylePrefs {
    fun get(context: Context): ClockStyle =
        toStyle(SettingsStore.read(context, SettingsCatalog.ClockStyle))

    fun set(context: Context, style: ClockStyle) {
        SettingsStore.write(context, SettingsCatalog.ClockStyle, style.name)
    }

    /**
     * Der Stil steht als Zeichenkette in den Einstellungen. Ein Name, den es nicht mehr gibt -
     * nach einem Rueckbau oder einer alten Installation -, faellt still auf CLASSIC zurueck statt
     * eine Ausnahme zu werfen: ein unbekanntes Ziffernblatt ist kein Grund, die Uhr nicht
     * anzuzeigen.
     */
    private fun toStyle(name: String?): ClockStyle {
        if (name == null) return ClockStyle.CLASSIC
        return runCatching { ClockStyle.valueOf(name) }.getOrDefault(ClockStyle.CLASSIC)
    }

    /**
     * Meldet den aktuellen Stil und danach jede Aenderung. Vorher lasen die Uhr-Schleifen in
     * HomeScreen/DockScreen den Wert bei JEDEM Tick neu, nur damit ein Stilwechsel ueberhaupt
     * ankommt - das koppelte die Reaktionszeit an die Tickrate und las die Preferences dauerhaft
     * im Hintergrund mit. Als Flow kommt die Aenderung sofort an, und der Tick selbst muss die
     * Preferences gar nicht mehr anfassen.
     *
     * [awaitClose] meldet den Listener wieder ab, sobald der Collector endet (Screen verlassen) -
     * sonst haengt er am Application-weiten SharedPreferences-Objekt und leckt.
     */
    fun observe(context: Context): Flow<ClockStyle> =
        SettingsStore.observe(context, SettingsCatalog.ClockStyle).map(::toStyle)
}
