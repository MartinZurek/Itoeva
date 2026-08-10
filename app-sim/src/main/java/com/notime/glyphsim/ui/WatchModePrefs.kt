package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphcore.reminder.QuietModeState
import com.notime.glyphcore.reminder.ReminderScheduler
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
 * **Dieser Absatz stand hier von Anfang an - umgesetzt war er nicht.** Unterdrueckt wurde nur die
 * Anzeige im Dock; ausgeloest, protokolliert und im Widget abgespielt wurde weiterhin. Genau der
 * beschriebene Schaden trat also ein, nur unsichtbar. Seit Phase 5b traegt der gemeinsame Kern
 * den Zustand mit ([QuietModeState]) und sperrt beide Ausloese-Wege; [setEnabled] schreibt ihn
 * deshalb immer mit - dieselbe Bruecke wie [AvatarSpeciesPrefs] sie frueher zum Profil hatte.
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
        // Der Kern liest den Zustand auch aus kalt gestarteten Prozessen (Alarm, Boot), in denen
        // setEnabled nie lief - hier nachziehen, damit beide Seiten uebereinstimmen. Auf
        // Geraeten, die vor Phase 5b liefen, ist der Wert im Kern noch gar nicht gesetzt.
        QuietModeState.setActive(context, stored)
        return stored
    }

    /** Beobachtbarer Zustand; beim ersten Zugriff aus [isEnabled] befuellt. */
    fun enabled(context: Context): StateFlow<Boolean?> {
        isEnabled(context)
        return _enabled.asStateFlow()
    }

    fun setEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, value).apply()
        QuietModeState.setActive(context, value)
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
         * Setzt beide Schalter so, dass genau dieser Modus herauskommt - **ohne die Alarme
         * anzufassen.**
         *
         * Die Reihenfolge ist nicht beliebig: "Nur Uhr" wird zuerst abgeschaltet, damit beim
         * Wechsel ins Spiel nicht kurzzeitig beides zugleich gilt.
         *
         * Fuer die Oberflaeche ist das die falsche Funktion - dort gehoert [switchTo] hin. Diese
         * hier bleibt oeffentlich, weil Tests den Modus setzen, ohne einen Planer zu haben.
         */
        fun set(context: Context, mode: AppMode) {
            WatchModePrefs.setEnabled(context, mode == WATCH)
            PlayModePrefs.setActive(context, mode == PLAY)
        }

        /**
         * Der Moduswechsel, wie ihn die Oberflaeche braucht: umschalten **und** die Alarme
         * nachziehen.
         *
         * **Warum das zusammengehoert.** Seit der Uhr-Modus wirklich still ist (siehe
         * [com.notime.glyphcore.reminder.QuietModeState]), entscheidet der Modus darueber, ob
         * ueberhaupt Alarme stehen duerfen. Wer nur umschaltet, hinterlaesst beim Wechsel IN den
         * Uhr-Modus lauter Alarme, die dort nichts mehr zu suchen haben - und beim Wechsel HERAUS
         * gar keine, bis zufaellig der Watchdog vorbeikommt. Letzteres ist der schlimmere Fall:
         * die App hoerte still auf zu erinnern, und der Nutzer haette nur eine Anzeige
         * umgeschaltet.
         *
         * `rescheduleAll` bestellt zuerst alles ab und stellt dann nur wieder ein, was im neuen
         * Modus erlaubt ist - beide Richtungen also mit demselben Aufruf.
         */
        suspend fun switchTo(context: Context, mode: AppMode) {
            set(context, mode)
            ReminderScheduler.rescheduleAll(context)
        }
    }
}
