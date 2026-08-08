package com.notime.glyphsim.ui

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * **Nur-Uhr**: Der Dock-Modus zeigt ausschliesslich die Uhr - kein Wesen, keine Wohnung, keine
 * Erinnerungen.
 *
 * **Warum das ein eigener Modus ist und keine Einstellung.** Es gibt Naechte, in denen man eine
 * Uhr will und sonst nichts. Ein Begleiter, der dabei durchs Bild laeuft, ist dann kein Begleiter
 * mehr, sondern eine Stoerung - und eine Erinnerung, die um drei Uhr morgens aufblinkt, erst
 * recht. Als Einstellung tief in einem Dialog waere das unbrauchbar: Wer nachts Ruhe will, will
 * sie JETZT und mit einem Griff, und morgens will er seinen Begleiter zurueck.
 *
 * **Erinnerungen ruhen dabei, und das ist Absicht.** Im Dock wird eine Erinnerung beantwortet,
 * indem man die Uhr auf das Wesen zieht - ohne Wesen gaebe es keinen Weg, sie zu beantworten. Sie
 * wuerden also der Reihe nach auflaufen und unbeantwortet verfallen, und das Pflegebuch zeigte
 * anschliessend ein Scheitern, das gar keines war. Lieber ausdruecklich still als heimlich
 * falsch.
 *
 * Getrennt von [PlayModePrefs] gehalten, statt beide in eine Drei-Wege-Einstellung zu giessen:
 * Der Spielmodus entscheidet, WELCHE Erinnerungen kommen, und diese Frage stellt sich hier gar
 * nicht mehr. Ihre Zusammenfuehrung zu den drei Modi, die der Nutzer sieht, macht [AppMode].
 */
object WatchModePrefs {
    private const val PREFS_NAME = "watch_mode_prefs"
    private const val KEY_ENABLED = "watch_only"

    private val _enabled = MutableStateFlow<Boolean?>(null)

    fun isEnabled(context: Context): Boolean {
        _enabled.value?.let { return it }
        val stored = prefs(context).getBoolean(KEY_ENABLED, false)
        _enabled.value = stored
        return stored
    }

    /** Beobachtbarer Zustand; beim ersten Zugriff aus [isEnabled] befuellt. */
    fun enabled(context: Context): StateFlow<Boolean?> {
        isEnabled(context)
        return _enabled.asStateFlow()
    }

    fun setEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()
        _enabled.value = value
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}

/**
 * Die drei Modi, wie der Nutzer sie sieht - aus zwei getrennt gespeicherten Schaltern
 * zusammengesetzt.
 *
 * **Warum zusammengesetzt und nicht als ein gespeicherter Wert.** Der Spielmodus muss auch aus
 * kalt gestarteten Prozessen lesbar sein (Alarm-Empfaenger, Scheduler - siehe
 * [com.notime.glyphcore.reminder.PlayModeState]); "nur Uhr" dagegen betrifft ausschliesslich die
 * Anzeige. Beides in einen gemeinsamen Wert zu zwingen hiesse, den Kern um einen Zustand zu
 * erweitern, der ihn nichts angeht.
 *
 * Dass daraus nach aussen trotzdem GENAU EIN Modus wird, ist die Aufgabe von [set]: Es gibt keine
 * Kombination, die der Nutzer je zu Gesicht bekommt.
 */
enum class AppMode(val labelRes: Int) {
    /**
     * Nur die Uhr.
     *
     * **Die Reihenfolge dieser drei ist die Reihenfolge in der Auswahl** und laeuft von der
     * ruhigsten zur lebhaftesten Fassung der App: erst nur eine Uhr, dann eine Uhr mit den
     * eigenen Gewohnheiten, dann ein Wesen, das seinen eigenen Tag lebt. So gelesen erklaert die
     * Leiste sich selbst - jeder Schritt nach rechts fuegt etwas hinzu, statt etwas anderes zu
     * sein.
     */
    WATCH(com.notime.glyphsim.R.string.mode_watch),

    /** Der Normalbetrieb: eigene Erinnerungen, Avatar erscheint, wenn eine faellig ist. */
    REMINDER(com.notime.glyphsim.R.string.mode_reminder),

    /** Das Spiel: der Avatar lebt dauerhaft in seiner Welt. */
    PLAY(com.notime.glyphsim.R.string.mode_play);

    companion object {
        fun current(context: Context): AppMode = when {
            WatchModePrefs.isEnabled(context) -> WATCH
            PlayModePrefs.isActive(context) -> PLAY
            else -> REMINDER
        }

        /**
         * Setzt beide Schalter so, dass genau dieser Modus herauskommt.
         *
         * Die Reihenfolge ist nicht beliebig: "Nur Uhr" wird zuerst abgeschaltet, damit beim
         * Wechsel ins Spiel nicht kurzzeitig beides zugleich gilt.
         */
        fun set(context: Context, mode: AppMode) {
            WatchModePrefs.setEnabled(context, mode == WATCH)
            PlayModePrefs.setActive(context, mode == PLAY)
        }
    }
}
