package com.notime.glyphsim.settings

import android.content.Context

/**
 * Attrappe fuer `SettingsStore` - siehe tools/reaction-preview/README.md.
 *
 * Die echte Datei haengt an `kotlinx.coroutines`-Flows, die fuer diese Pruefungen bedeutungslos
 * sind. Der Ersatz haelt Werte nur im Speicher; geprueft wird die Entscheidungslogik daneben,
 * nicht die Persistenz. **Dass die Einstellung ueber App-Starts hinweg erhalten bleibt, sichert
 * die echte SharedPreferences-Fassung und die CI - nicht dieser Ersatz.**
 */
object SettingsStore {
    private val werte = mutableMapOf<String, Boolean>()

    fun read(context: Context, setting: Setting.Bool): Boolean =
        werte["${setting.file}:${setting.key}"] ?: setting.default

    fun write(context: Context, setting: Setting.Bool, value: Boolean) {
        werte["${setting.file}:${setting.key}"] = value
    }
}
