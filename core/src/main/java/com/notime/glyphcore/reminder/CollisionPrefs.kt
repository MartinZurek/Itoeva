package com.notime.glyphcore.reminder

import android.content.Context

/**
 * Merkt sich, wie mehrere Erinnerungen behandelt werden sollen, die auf denselben
 * (oder einen sehr nahen) Zeitpunkt fallen wuerden.
 *
 * **ACHTUNG - wirkungslos.** [ReminderScheduler] wertet diese Einstellung nicht mehr aus: das
 * Verschieben kollidierender Erinnerungen ist ersatzlos entfallen, weil es sie von dem Zeitpunkt
 * wegruecke, fuer den sie gestellt waren (aus "alle fuenf Minuten" wurde faktisch "alle acht").
 * An seine Stelle tritt eine feste Reihenfolge beim Ausloesen - kurze Intervalle zuerst, laengere
 * direkt danach -, ohne einen Slot anzutasten. Siehe `ReminderTrigger.firePending` im
 * :app-sim-Modul.
 *
 * Tama (:app-sim) hat den zugehoerigen Einstellungs-Dialog entfernt. **:app zeigt ihn noch** und
 * schreibt hier weiterhin Werte, die niemand mehr liest - dieser Schalter luegt den Nutzer also
 * an und gehoert dort ebenfalls entfernt. Solange das nicht passiert ist, bleibt diese Klasse
 * bestehen, damit :app kompiliert.
 */
@Deprecated("Wird von ReminderScheduler nicht mehr ausgewertet - Reihenfolge statt Verschieben, siehe ReminderTrigger")
object CollisionPrefs {
    enum class CollisionMode { IMMEDIATE, SPREAD }

    private const val PREFS_NAME = "collision_prefs"
    private const val KEY_MODE = "collision_mode"
    private const val KEY_SPREAD_GAP_MINUTES = "spread_gap_minutes"
    const val DEFAULT_SPREAD_GAP_MINUTES = 3
    val SPREAD_GAP_OPTIONS = listOf(2, 3, 5)

    fun getMode(context: Context): CollisionMode {
        val stored = prefs(context).getString(KEY_MODE, null) ?: return CollisionMode.IMMEDIATE
        return runCatching { CollisionMode.valueOf(stored) }.getOrDefault(CollisionMode.IMMEDIATE)
    }

    fun setMode(context: Context, mode: CollisionMode) {
        prefs(context).edit().putString(KEY_MODE, mode.name).apply()
    }

    fun getSpreadGapMinutes(context: Context): Int =
        prefs(context).getInt(KEY_SPREAD_GAP_MINUTES, DEFAULT_SPREAD_GAP_MINUTES)

    fun setSpreadGapMinutes(context: Context, minutes: Int) {
        prefs(context).edit().putInt(KEY_SPREAD_GAP_MINUTES, minutes).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
