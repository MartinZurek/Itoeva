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

    /**
     * Ob diese Erinnerung derzeit eingeplant werden soll.
     *
     * ## Was sich hier geaendert hat (Phase 3)
     *
     * Frueher stand hier `reminder.isPlayMode == isActive(context)` - eine Gleichheit, und damit
     * ein Entweder-oder: war die Welt aktiv, fielen **die echten Erinnerungen des Nutzers aus der
     * Planung**. Nicht geloescht, nicht abgeschaltet, sie kamen nur nicht mehr. Aus einer
     * Anzeigeentscheidung wurde so das Aussetzen der Erinnerungsfunktion, ohne dass es irgendwo
     * stand.
     *
     * Jetzt gilt: **die eigenen Routinen laufen immer.** Die vom Wesen selbst gewuerfelte Zeile
     * kommt hinzu, wenn seine Welt aktiv ist - sie tritt nicht mehr an deren Stelle.
     *
     * Damit ist die Verbindung moeglich, um die es eigentlich geht: eine erfuellte Routine
     * veraendert, was das Wesen von sich aus tut (siehe `PlayHabitSignal`), waehrend die Routine
     * selbst weiterlaeuft. Vorher schlossen sich die beiden Haelften dieser Idee gegenseitig aus.
     *
     * Wer seine Erinnerungen wirklich stillstellen will, pausiert sie ausdruecklich - siehe
     * [ReminderPause]. Das ist derselbe Wunsch, nur ehrlich benannt und mit einem Ende versehen.
     */
    fun matchesCurrentMode(context: Context, reminder: GlyphReminder): Boolean =
        if (reminder.isPlayMode) isActive(context) else true

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
