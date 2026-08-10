package com.notime.glyphcore.reminder

import android.content.Context

/**
 * Ob die App gerade **nur eine Anzeige** sein soll - keine Erinnerungen, kein Begleiter.
 *
 * ## Die Zusage, die vorher nur im Kommentar stand
 *
 * Im :app-sim-Modul ist das der Uhr-Modus (`ui/WatchModePrefs`): das Dock zeigt ausschliesslich
 * die Uhr. Dessen Dokumentation sagte von Anfang an, warum die Erinnerungen dabei ruhen sollen -
 * im Dock wird eine Erinnerung beantwortet, indem man die Uhr auf das Wesen zieht, und **ohne
 * Wesen gibt es keinen Weg, sie zu beantworten.** Sie liefen also der Reihe nach auf und
 * verfielen unbeantwortet.
 *
 * Umgesetzt war das nie. Unterdrueckt wurde nur die *Anzeige* im Dock; ausgeloest wurde
 * weiterhin. Jede Erinnerung schrieb also ihre Zeile ins Pflegebuch, ohne dass irgendjemand sie
 * haette beantworten koennen.
 *
 * ## Warum das mehr ist als ein paar verpasste Stupser
 *
 * `RhythmSuggestion` rechnet ueber sieben Tage, wie oft eine Erinnerung mit Tagesziel
 * tatsaechlich beantwortet wurde. Wer sein Telefon abends zwei Stunden als Nachttischuhr
 * benutzt, erzeugt in dieser Zeit lauter unbeantwortete Ausloesungen - und das Wesen schliesst
 * daraus, dass diese Routine chronisch verfehlt wird, und schlaegt vor, sie seltener zu machen.
 *
 * **Eine Anzeigeeinstellung erzieht die App damit zu einer falschen Schlussfolgerung ueber den
 * Nutzer.** Das ist genau die Sorte stiller Bestrafung, die diese App nicht haben soll.
 *
 * ## Warum das keine Pause ist
 *
 * [ReminderPause] ist bewusst zeitlich begrenzt: eine Pause, die man vergisst, ist der haeufigste
 * Weg, auf dem eine Erinnerungs-App still aufhoert zu erinnern. Diese Ruhe braucht kein Ende,
 * weil sie nicht vergessen werden kann - sie ist die ganze Zeit sichtbar. Wer sie eingeschaltet
 * hat, sieht eine Uhr und sonst nichts, und die Modus-Auswahl steht oben links im Bild.
 *
 * ## Wie [PlayModeState] und nicht wie eine Einstellung
 *
 * In SharedPreferences statt in einer Laufzeit-Variable: Planer und Alarm-Empfaenger laufen auch
 * aus kalt gestarteten Prozessen (Boot, AlarmManager), in denen nie ein Initialisierungspunkt
 * durchlaufen wurde.
 *
 * Das :app-Modul kennt keinen solchen Modus und laesst den Wert auf `false` - dort feuern also
 * unveraendert die echten Erinnerungen.
 */
object QuietModeState {
    private const val PREFS_NAME = "quiet_mode_state"
    private const val KEY_ACTIVE = "quiet_mode_active"

    fun isActive(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ACTIVE, false)

    fun setActive(context: Context, active: Boolean) {
        prefs(context).edit().putBoolean(KEY_ACTIVE, active).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
