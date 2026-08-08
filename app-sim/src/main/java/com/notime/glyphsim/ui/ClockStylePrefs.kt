package com.notime.glyphsim.ui

import android.content.Context
import android.content.SharedPreferences
import com.notime.glyphsim.matrix.ClockStyle
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Persistiert das vom Nutzer gewaehlte Ziffernblatt-Design (siehe [ClockStyle]), Standard CLASSIC. */
object ClockStylePrefs {
    private const val PREFS_NAME = "clock_style_prefs"
    private const val KEY_STYLE = "style"

    fun get(context: Context): ClockStyle {
        val name = prefs(context).getString(KEY_STYLE, null) ?: return ClockStyle.CLASSIC
        return runCatching { ClockStyle.valueOf(name) }.getOrDefault(ClockStyle.CLASSIC)
    }

    fun set(context: Context, style: ClockStyle) {
        prefs(context).edit().putString(KEY_STYLE, style.name).apply()
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
    fun observe(context: Context): Flow<ClockStyle> = callbackFlow {
        val preferences = prefs(context)
        trySend(get(context))
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_STYLE || key == null) trySend(get(context))
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
