package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphcore.data.GlyphReminder
import kotlin.math.abs

/**
 * Der Avatar meldet sich hoechstens EINMAL pro Oeffnen des Pflegebuchs und nur bei einem klaren
 * Muster ueber mehrere Tage - eine einzelne Erinnerung, die heute mal daneben lag, ist kein
 * Anlass, siehe [MIN_RATIO_UNDER]/[MIN_RATIO_OVER]. Bewusst reine Beobachtung ohne Knopf, der
 * direkt etwas aendert: der Nutzer soll seinen Rhythmus selbst finden (siehe
 * ReminderConcept.ReminderRhythm), der Avatar zeigt nur, wo die Realitaet gerade vom eingestellten
 * Ziel abweicht - Aendern bleibt bewusst ein bewusster Schritt im Bearbeiten-Dialog.
 */
data class RhythmSuggestion(val reminderId: Long, val label: String, val kind: Kind) {
    enum class Kind { OVER, UNDER }
}

/**
 * Findet - falls vorhanden - die auffaelligste Erinnerung ueber hoechstens die letzten
 * [WINDOW_DAYS] Tage: entweder deutlich unter- oder ueber ihrem Tagesziel. Bei mehreren
 * zutreffenden wird nur die mit der groessten Abweichung gemeldet, damit aus dem Pflegebuch keine
 * Liste von Ermahnungen wird, sondern hoechstens ein einzelner Kommentar.
 *
 * [firstOccurrenceByReminderId] liefert PRO Erinnerung, seit wann es sie ueberhaupt gibt - bewusst
 * nicht ein einzelner Wert fuer den ganzen Avatar: eine gestern erst angelegte Erinnerung in einem
 * monatealten Profil braucht ihren EIGENEN Startpunkt, sonst wirkte sie schon nach einem Tag
 * faelschlich wie chronisch verpasst, weil der Nenner faelschlich Tage mitzaehlt, an denen es sie
 * noch gar nicht gab. Fehlt ein Eintrag (Erinnerung hat noch nie ausgeloest), wird sie ausgelassen
 * statt mit einem Notbehelf-Zeitpunkt gerechnet.
 */
fun findRhythmSuggestion(
    reminders: List<GlyphReminder>,
    fedCountByReminderId: Map<Long, Int>,
    firstOccurrenceByReminderId: Map<Long, Long>,
    now: Long = System.currentTimeMillis()
): RhythmSuggestion? {
    return reminders
        .asSequence()
        .filter { it.enabled && it.dailyGoal > 0 }
        .mapNotNull { reminder ->
            val first = firstOccurrenceByReminderId[reminder.id] ?: return@mapNotNull null
            val daysActive = (((now - first) / DAY_MILLIS).toInt() + 1).coerceIn(1, WINDOW_DAYS)
            if (daysActive < MIN_DAYS_FOR_SUGGESTION) return@mapNotNull null

            val fed = fedCountByReminderId[reminder.id] ?: 0
            val ratio = fed.toFloat() / (reminder.dailyGoal * daysActive)
            val kind = when {
                ratio >= MIN_RATIO_OVER -> RhythmSuggestion.Kind.OVER
                ratio <= MIN_RATIO_UNDER -> RhythmSuggestion.Kind.UNDER
                else -> return@mapNotNull null
            }
            Triple(reminder, kind, abs(ratio - 1f))
        }
        .maxByOrNull { (_, _, deviation) -> deviation }
        ?.let { (reminder, kind, _) -> RhythmSuggestion(reminder.id, reminder.label, kind) }
}

/** Wie viele Tage zurueck [findRhythmSuggestion] hoechstens schaut. */
const val WINDOW_DAYS = 7

private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/** Erst ab dieser Mindest-Historie wird ueberhaupt kommentiert, siehe Klassendoku. */
private const val MIN_DAYS_FOR_SUGGESTION = 3

/** Ab durchschnittlich 140% des Tagesziels gilt eine Erinnerung als komfortabel uebererfuellt. */
private const val MIN_RATIO_OVER = 1.4f

/** Bei durchschnittlich hoechstens 35% des Tagesziels gilt eine Erinnerung als chronisch verfehlt. */
private const val MIN_RATIO_UNDER = 0.35f

/**
 * Merkt sich je Erinnerung und Richtung, wann [RhythmSuggestion] zuletzt gezeigt wurde - sonst
 * saehe der Nutzer denselben Kommentar bei jedem Oeffnen des Pflegebuchs erneut, obwohl sich am
 * Muster nichts geaendert hat. Genau das sollen die Erinnerungen dieser App NICHT sein (siehe
 * Klassendoku [RhythmSuggestion]): ein wiederholter Kommentar waere selbst das staendige Signal,
 * das die App an anderer Stelle bewusst vermeidet.
 */
object RhythmSuggestionPrefs {
    private const val PREFS_NAME = "rhythm_suggestion_prefs"
    private const val SUPPRESS_DAYS = 7L

    fun shouldShow(context: Context, reminderId: Long, kind: RhythmSuggestion.Kind): Boolean {
        val last = prefs(context).getLong(key(reminderId, kind), Long.MIN_VALUE)
        if (last == Long.MIN_VALUE) return true
        val daysSinceShown = (System.currentTimeMillis() - last) / DAY_MILLIS
        return daysSinceShown >= SUPPRESS_DAYS
    }

    fun markShown(context: Context, reminderId: Long, kind: RhythmSuggestion.Kind) {
        prefs(context).edit().putLong(key(reminderId, kind), System.currentTimeMillis()).apply()
    }

    private fun key(reminderId: Long, kind: RhythmSuggestion.Kind) = "${reminderId}_${kind.name}"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
