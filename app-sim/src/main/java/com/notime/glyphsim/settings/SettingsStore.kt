package com.notime.glyphsim.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Der eine Zugang zu allen Einstellungen - liest und schreibt nach dem, was in
 * [SettingsCatalog] steht.
 *
 * ## Was er ersetzt
 *
 * Vierzehn `object`s hatten jeweils ihren eigenen `prefs(context)`-Helfer, ihre eigenen
 * `getBoolean`/`putBoolean`-Aufrufe und - wo sie beobachtbar sein mussten - ihre eigene
 * `callbackFlow`-Konstruktion samt Listener-Verwaltung. Dieselbe Mechanik vierzehnmal, jedes Mal
 * mit der Moeglichkeit, den Listener zu vergessen abzumelden.
 *
 * ## Was er bewusst NICHT tut
 *
 * Er aendert das Verhalten nicht. Gelesen wird weiterhin **synchron**, aus denselben Dateien, mit
 * denselben Schluesseln. Das ist der Punkt: die 45 Aufrufstellen im Rest der App bleiben
 * unangetastet, und damit kann bei diesem Schritt keine Einstellung verlorengehen.
 *
 * Der Wechsel auf DataStore - was Phase 3.2 eigentlich vorsieht - ist danach eine Aenderung an
 * genau dieser Datei. Die Huerde dafuer liegt woanders: DataStore liest nur asynchron, und die
 * Aufrufstellen im Widget-Provider, im Planer und im Watchdog sind synchron. Siehe die
 * Begruendung in [SettingsCatalog].
 *
 * ## Zwischenspeicher fuer die Dateien
 *
 * `getSharedPreferences` ist selbst schon gepuffert (Android haelt eine Instanz je Dateiname),
 * ein eigener Zwischenspeicher waere also ueberfluessig - und schaedlich, weil er den
 * Listener-Mechanismus umginge, ueber den [observe] Aenderungen mitbekommt.
 */
object SettingsStore {

    private fun prefs(context: Context, file: String): SharedPreferences =
        context.applicationContext.getSharedPreferences(file, Context.MODE_PRIVATE)

    // --- Lesen ---------------------------------------------------------------------------------

    fun read(context: Context, setting: Setting.Bool): Boolean =
        prefs(context, setting.file).getBoolean(setting.key, setting.default)

    fun read(context: Context, setting: Setting.Float): Float =
        prefs(context, setting.file).getFloat(setting.key, setting.default)

    fun read(context: Context, setting: Setting.Int): Int =
        prefs(context, setting.file).getInt(setting.key, setting.default)

    fun read(context: Context, setting: Setting.OptionalString): String? =
        prefs(context, setting.file).getString(setting.key, setting.default)

    // --- Schreiben ------------------------------------------------------------------------------

    fun write(context: Context, setting: Setting.Bool, value: Boolean) {
        prefs(context, setting.file).edit().putBoolean(setting.key, value).apply()
    }

    fun write(context: Context, setting: Setting.Float, value: Float) {
        prefs(context, setting.file).edit().putFloat(setting.key, value).apply()
    }

    fun write(context: Context, setting: Setting.Int, value: Int) {
        prefs(context, setting.file).edit().putInt(setting.key, value).apply()
    }

    fun write(context: Context, setting: Setting.OptionalString, value: String?) {
        prefs(context, setting.file).edit().putString(setting.key, value).apply()
    }

    /**
     * Schreibt mehrere Werte derselben Datei in EINEM Vorgang.
     *
     * Wichtig fuer zusammengehoerende Werte wie Dock-Groesse und -Position: einzeln geschrieben
     * kann ein Beobachter den Zwischenstand sehen (neue Groesse, alte Position) und die Anzeige
     * springt zweimal.
     */
    fun writeFloats(context: Context, vararg werte: Pair<Setting.Float, Float>) {
        require(werte.map { it.first.file }.distinct().size <= 1) {
            "writeFloats schreibt in eine Datei - hier kamen mehrere: " +
                werte.map { it.first.file }.distinct()
        }
        val datei = werte.firstOrNull()?.first?.file ?: return
        prefs(context, datei).edit().apply {
            werte.forEach { (setting, value) -> putFloat(setting.key, value) }
        }.apply()
    }

    // --- Beobachten -----------------------------------------------------------------------------

    /**
     * Meldet jede Aenderung dieser Einstellung, beginnend mit dem aktuellen Wert.
     *
     * `callbackFlow` mit `awaitClose`: der Listener meldet sich ab, sobald niemand mehr zuhoert.
     * Genau das war vorher an vier Stellen einzeln nachgebaut.
     *
     * `distinctUntilChanged`, weil SharedPreferences auch dann meldet, wenn derselbe Wert erneut
     * geschrieben wurde - ohne das zeichnete die Oberflaeche grundlos neu.
     */
    fun observe(context: Context, setting: Setting.Bool): Flow<Boolean> =
        observeKey(context, setting.file, setting.key) { read(context, setting) }

    fun observe(context: Context, setting: Setting.OptionalString): Flow<String?> =
        observeKey(context, setting.file, setting.key) { read(context, setting) }

    private fun <T> observeKey(
        context: Context,
        file: String,
        key: String,
        lies: () -> T
    ): Flow<T> = callbackFlow {
        val store = prefs(context, file)
        trySend(lies())
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, geaendert ->
            // `geaendert` ist null, wenn alles geloescht wurde (clear()) - dann gilt wieder die
            // Vorgabe, und das ist ebenso eine Aenderung.
            if (geaendert == null || geaendert == key) trySend(lies())
        }
        store.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { store.unregisterOnSharedPreferenceChangeListener(listener) }
    }.distinctUntilChanged()
}
