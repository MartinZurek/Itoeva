package com.notime.glyphcore.reminder

import android.content.Context
import com.notime.glyphcore.data.GlyphReminder

/**
 * Ob gerade der **Spielmodus** laeuft statt des Normalbetriebs.
 *
 * Die beiden schliessen sich aus, und zwar an genau einer Stelle: welche Erinnerungen ueberhaupt
 * eingeplant und ausgeloest werden. Im Normalbetrieb sind das die vom Nutzer eingerichteten, im
 * Spielmodus ausschliesslich die vom Spiel gewuerfelte (siehe [GlyphReminder.isPlayMode]).
 * **Ansonsten aendert sich nichts** - Startbildschirm, Dock-Modus und Widget verhalten sich
 * identisch, es kommt nur etwas anderes an.
 *
 * Warum sich das ausschliessen muss: Liefen beide Saetze gleichzeitig, ueberlagerten sich die
 * sorgfaeltig eingestellten echten Erinnerungen mit einem Spiel-Takt von wenigen Minuten. Die
 * eigentliche Aufgabe der App - verlaesslich an das Richtige zu erinnern - ginge im Spiel unter,
 * und die Auswertung im Pflegebuch waere nicht mehr zu lesen.
 *
 * Wie [ActiveProfilePrefs] in SharedPreferences und nicht in einer Laufzeit-Variable: der
 * Scheduler und die Alarm-Empfaenger laufen auch aus kalt gestarteten Prozessen (Boot,
 * AlarmManager), in denen nie ein Initialisierungspunkt durchlaufen wurde.
 *
 * Das :app-Modul kennt keinen Spielmodus und laesst den Wert auf `false` - dort feuern also
 * unveraendert die echten Erinnerungen.
 */
object PlayModeState {
    private const val PREFS_NAME = "play_mode_state"
    private const val KEY_ACTIVE = "play_mode_active"

    fun isActive(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ACTIVE, false)

    fun setActive(context: Context, active: Boolean) {
        prefs(context).edit().putBoolean(KEY_ACTIVE, active).apply()
    }

    /** Ob diese Erinnerung in den gerade laufenden Modus gehoert. */
    fun matchesCurrentMode(context: Context, reminder: GlyphReminder): Boolean =
        reminder.isPlayMode == isActive(context)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
