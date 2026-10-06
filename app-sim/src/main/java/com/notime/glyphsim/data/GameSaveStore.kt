package com.notime.glyphsim.data

import android.content.Context
import com.notime.glyphsim.matrix.GameAdventure
import com.notime.glyphsim.matrix.PlayBackpack

/** Ein atomarer Snapshot fuer Inventar, Weltfolgen und Aufenthalt; Legacy-Rucksack bleibt erhalten. */
class GameSaveStore(private val context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("itoeva2", Context.MODE_PRIVATE)
    var error: String? = null
        private set

    fun load(): GameAdventure.State {
        val encoded = prefs.getString("world_snapshot", null)
        if (encoded == null) return GameAdventure.State(backpack = PlayBackpack.decode(prefs.getString("backpack", null)))
        return try { GameAdventure.decode(encoded) } catch (failure: IllegalArgumentException) {
            error = if (context.resources.configuration.locales[0].language == "de") "Der Spielstand ist nicht lesbar. Er bleibt unverändert gespeichert." else "This save cannot be read. The original is preserved."
            GameAdventure.State()
        }
    }

    /** Auf IO aufrufen. Der Aufrufer zeigt Folgen erst nach erfolgreichem Commit. */
    @Suppress("ApplySharedPref")
    fun save(state: GameAdventure.State): Boolean {
        if (error != null) return false
        return prefs.edit().putString("world_snapshot", GameAdventure.encode(state))
            .putString("backpack", PlayBackpack.encode(state.backpack)).commit()
    }
}
