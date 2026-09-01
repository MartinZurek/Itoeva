package com.notime.glyphsim.ui

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.DAILY_GOAL_OPTIONS
import com.notime.glyphcore.data.DaysOfWeekMask
import com.notime.glyphcore.data.INTERVAL_OPTIONS
import com.notime.glyphcore.data.LibraryAnimation
import com.notime.glyphcore.data.NO_GOAL
import com.notime.glyphcore.data.ReminderValidation
import com.notime.glyphcore.data.snapToOption
import java.time.DayOfWeek

/**
 * Eine Erinnerung, so wie sie aus einer KI-Antwort kommt: alles optional, alles unbestaetigt.
 *
 * Bewusst getrennt vom fertigen [com.notime.glyphcore.data.GlyphReminder] gehalten. Die Ausgabe
 * eines Sprachmodells ist Text, keine Zusage - sie kann Felder weglassen, unbekannte Themen
 * erfinden oder unsinnige Zahlen enthalten. Erst [ReminderImport.sanitize] macht daraus etwas,
 * das die App anlegen darf.
 */
data class RawImportedReminder(
    val label: String? = null,
    val topic: String? = null,
    /**
     * Name einer bereits vom Nutzer angelegten Bibliotheks-Animation (siehe [LibraryAnimation]),
     * als Alternative zu [topic]. Ermoeglicht der KI, eigene statt nur die 12 fest eingebauten
     * Animationen zu nutzen - vorausgesetzt, ihr wurde die Liste der vorhandenen im Prompt
     * mitgegeben (siehe import_prompt_library_intro).
     */
    val libraryAnimation: String? = null,
    val dailyGoal: Int? = null,
    val intervalMinutes: Int? = null,
    val start: String? = null,
    val end: String? = null,
    val days: List<String>? = null
)

/** Eine gepruefte Erinnerung samt Hinweisen darauf, was dabei zurechtgerueckt wurde. */
data class SanitizedReminder(
    val label: String,
    val animationChoice: AnimationChoice,
    val dailyGoal: Int,
    val intervalMinutes: Int,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val daysOfWeekMask: Int,
    val corrections: List<String>
)

/**
 * Nimmt entgegen, was eine KI ausgegeben hat, und macht daraus anlegbare Erinnerungen.
 *
 * **Warum so misstrauisch:** der Text kommt aus einem Sprachmodell und laeuft ueber die
 * Zwischenablage - er kann veraltet, abgeschnitten, halluziniert oder schlicht von woanders sein.
 * Jede Angabe wird deshalb gegen die tatsaechlichen Moeglichkeiten der App geprueft; unbrauchbare
 * Werte werden nicht abgelehnt, sondern auf einen sinnvollen Stand gezogen. Ein Import, der wegen
 * einer einzelnen krummen Zahl komplett scheitert, waere aergerlicher als einer, der sie
 * korrigiert und das offenlegt - [SanitizedReminder.corrections] traegt genau diese Offenlegung,
 * damit die Vorschau sie zeigen kann.
 */
object ReminderImport {

    /**
     * Dieselbe Obergrenze wie fuer jede andere Erinnerung auch - frueher stand hier eine eigene
     * Kopie der Zahl, die beim Aendern der einen Seite stillschweigend auseinandergelaufen waere.
     */
    private const val MAX_LABEL_LENGTH = ReminderValidation.MAX_LABEL_LENGTH

    /**
     * Obergrenzen fuer das, was von aussen hereinkommt.
     *
     * **Warum ueberhaupt Grenzen.** Die Activity ist `exported` und nimmt Teilen-Absichten von
     * JEDER App auf dem Geraet entgegen (siehe Manifest). Was hier ankommt, ist damit nicht
     * "eine KI-Antwort", sondern beliebiger Text aus beliebiger Quelle - versehentlich (jemand
     * teilt ein ganzes E-Book) wie absichtlich. Ohne Grenzen haengt die Zahl der Klammern und
     * Eintraege allein am Absender.
     *
     * Die Werte sind bewusst grosszuegig: eine echte Antwort mit einem Dutzend Erinnerungen liegt
     * bei wenigen tausend Zeichen. Sie sollen Unfug abfangen, nicht den normalen Fall streifen.
     */
    /** Alles darueber wird abgeschnitten - siehe [begrenzeEingabe]. */
    const val MAX_INPUT_CHARS = 20_000

    /** Mehr Erinnerungen auf einmal anzulegen ergibt keinen Sinn und fuellt nur die Liste. */
    const val MAX_REMINDERS = 25

    /** Maximale Objekt- und Array-Verschachtelung zum Schutz des rekursiven JSON-Lesers. */
    const val MAX_JSON_DEPTH = 32

    /**
     * Was beim Begrenzen der Eingabe herauskam - [gekuerzt] sagt, ob dem Nutzer etwas
     * vorzuenthalten war.
     *
     * Bewusst mit Rueckmeldung statt still: Text abzuschneiden und so zu tun, als sei nichts
     * gewesen, waere die schlechteste Variante - der Nutzer sucht dann nach Erinnerungen, die
     * er geteilt hat und die nirgends auftauchen.
     */
    data class BegrenzteEingabe(val text: String, val gekuerzt: Boolean)

    fun begrenzeEingabe(text: String): BegrenzteEingabe =
        if (text.length <= MAX_INPUT_CHARS) {
            BegrenzteEingabe(text, gekuerzt = false)
        } else {
            BegrenzteEingabe(text.take(MAX_INPUT_CHARS), gekuerzt = true)
        }

    private val ALL_DAYS = DaysOfWeekMask.toMask(DayOfWeek.entries.toSet())

    private val DAY_ALIASES: Map<String, DayOfWeek> = mapOf(
        "MO" to DayOfWeek.MONDAY, "MON" to DayOfWeek.MONDAY,
        "TU" to DayOfWeek.TUESDAY, "TUE" to DayOfWeek.TUESDAY, "DI" to DayOfWeek.TUESDAY,
        "WE" to DayOfWeek.WEDNESDAY, "WED" to DayOfWeek.WEDNESDAY, "MI" to DayOfWeek.WEDNESDAY,
        "TH" to DayOfWeek.THURSDAY, "THU" to DayOfWeek.THURSDAY, "DO" to DayOfWeek.THURSDAY,
        "FR" to DayOfWeek.FRIDAY, "FRI" to DayOfWeek.FRIDAY,
        "SA" to DayOfWeek.SATURDAY, "SAT" to DayOfWeek.SATURDAY,
        "SU" to DayOfWeek.SUNDAY, "SUN" to DayOfWeek.SUNDAY, "SO" to DayOfWeek.SUNDAY
    )

    /**
     * Schneidet den JSON-Block aus einer Antwort heraus.
     *
     * Sprachmodelle liefern fast nie nur JSON - davor steht eine Erklaerung, danach ein
     * Schlusssatz, oft liegt alles in einem Markdown-Codeblock. Vom Nutzer zu verlangen, exakt
     * den richtigen Ausschnitt zu kopieren, wuerde den Import unbrauchbar machen. Genommen wird
     * deshalb der Bereich von der ersten geschweiften Klammer bis zur letzten passenden.
     */
    fun extractJson(text: String): String? {
        val start = text.indexOf('{')
        if (start < 0) return null
        val containers = CharArray(MAX_JSON_DEPTH)
        var depth = 0
        var inString = false
        var escaped = false
        for (i in start until text.length) {
            val character = text[i]
            if (inString) {
                when {
                    escaped -> escaped = false
                    character == '\\' -> escaped = true
                    character == '"' -> inString = false
                }
                continue
            }
            when (character) {
                '"' -> inString = true
                '{', '[' -> {
                    // Tief verschachtelte Objekte und Arrays bringen spaeter den JSON-Leser in
                    // Not: er arbeitet rekursiv, und genuegend Ebenen erzeugen einen
                    // StackOverflowError. Eine echte Antwort kommt mit einer Handvoll aus; alles
                    // darueber ist kein Ausschnitt, den es zu retten lohnt.
                    if (depth == MAX_JSON_DEPTH) return null
                    containers[depth] = character
                    depth++
                }
                '}', ']' -> {
                    if (depth == 0) return null
                    val expectedOpening = if (character == '}') '{' else '['
                    if (containers[depth - 1] != expectedOpening) return null
                    depth--
                    if (depth == 0) return text.substring(start, i + 1)
                }
            }
        }
        return null
    }

    /** "08:00" oder "8:00" -> Minuten seit Mitternacht, sonst null. */
    fun parseTime(value: String?): Int? {
        val parts = value?.trim()?.split(":") ?: return null
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    /**
     * Prueft eine rohe Erinnerung und ergaenzt Fehlendes aus dem Rhythmus-Konzept des Themas
     * (siehe [ReminderRhythm]) - dieselbe Wissensbasis, die auch der gefuehrte Dialog nutzt.
     *
     * [libraryAnimations] sind die aktuell in der Bibliothek vorhandenen eigenen Animationen des
     * Nutzers - nur gegen die darf [RawImportedReminder.libraryAnimation] ueberhaupt treffen,
     * alles andere waere ein Verweis auf etwas, das es (mehr) gibt.
     */
    fun sanitize(raw: RawImportedReminder, libraryAnimations: List<LibraryAnimation> = emptyList()): SanitizedReminder {
        val corrections = mutableListOf<String>()

        // Bestimmt weiterhin den Rhythmus (Vorschlaege fuer Ziel/Intervall/Zeitfenster) - auch
        // wenn am Ende eine Bibliotheks-Animation angezeigt wird, braucht deren Erinnerung
        // trotzdem sinnvolle Zahlen, und "topic" ist die einzige Angabe, aus der sich die
        // ableiten lassen.
        val type = AnimationType.entries.firstOrNull { it.name.equals(raw.topic?.trim(), ignoreCase = true) }
            ?: AnimationType.GENERAL.also {
                if (!raw.topic.isNullOrBlank()) corrections += "topic=\"${raw.topic}\" → GENERAL"
            }
        val rhythm = ReminderRhythm.forType(type)

        // Nur gegen tatsaechlich vorhandene Bibliotheks-Animationen matchen (Name, ohne Gross-/
        // Kleinschreibung - die KI kennt die exakte Schreibweise nicht zuverlaessig). Kein Treffer
        // faellt zurueck auf die Themen-Animation, nicht auf einen Fehler - ein erfundener oder
        // veralteter Name ist kein Grund, den ganzen Eintrag zu verwerfen.
        val matchedLibrary = raw.libraryAnimation
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { requested -> libraryAnimations.find { it.label.equals(requested, ignoreCase = true) } }
        val animationChoice: AnimationChoice = if (matchedLibrary != null) {
            AnimationChoice.Library(matchedLibrary.id, matchedLibrary.label, matchedLibrary.emoji)
        } else {
            if (!raw.libraryAnimation.isNullOrBlank()) {
                corrections += "libraryAnimation=\"${raw.libraryAnimation}\" → ${type.name}"
            }
            AnimationChoice.BuiltIn(type)
        }

        // Auf die tatsaechlich waehlbaren Werte einrasten statt jede Zahl zu uebernehmen.
        // Sonst entstuende eine Erinnerung, die der Bearbeiten-Dialog nicht darstellen kann: dort
        // stuende dann kein Chip auf ausgewaehlt, und ein unbeabsichtigter Griff wuerde die
        // Einstellung ueberschreiben. Die KI kann den Vorgaben folgen - verlassen darf man sich
        // darauf nicht.
        // Ein ausdrueckliches 0 ist "kein Ziel gewollt" (siehe NO_GOAL, import_prompt_template)
        // und wird ohne Korrektur uebernommen - anders als ein fehlendes Feld, das mangels
        // Angabe auf den Rhythmus-Vorschlag zurueckfaellt. Ein NEGATIVER Wert dagegen ist keine
        // bewusste Ansage, sondern eine unsinnige Zahl wie jede andere - der faellt ebenfalls auf
        // NO_GOAL zurueck, aber MIT ausgewiesener Korrektur (sonst liesse sich ueber den Import
        // eine offensichtlich fehlerhafte Zahl unbemerkt einschleusen).
        val allowedGoals = DAILY_GOAL_OPTIONS.filter { it > 0 }
        val goal = when {
            raw.dailyGoal == null -> rhythm.suggestedGoal
            raw.dailyGoal == NO_GOAL -> NO_GOAL
            raw.dailyGoal < NO_GOAL -> NO_GOAL.also {
                corrections += "dailyGoal=${raw.dailyGoal} → $it"
            }
            else -> snapToOption(raw.dailyGoal, allowedGoals).also {
                if (it != raw.dailyGoal) corrections += "dailyGoal=${raw.dailyGoal} → $it"
            }
        }

        val interval = raw.intervalMinutes
            ?.takeIf { it > 0 }
            ?.let { requested ->
                snapToOption(requested, INTERVAL_OPTIONS).also {
                    if (it != requested) corrections += "intervalMinutes=$requested → $it"
                }
            }
            ?: rhythm.suggestedInterval.also {
                if (raw.intervalMinutes != null) corrections += "intervalMinutes=${raw.intervalMinutes} → $it"
            }

        val start = parseTime(raw.start) ?: rhythm.suggestedWindow.startMinute
        val endParsed = parseTime(raw.end) ?: rhythm.suggestedWindow.endMinute
        // Gleicher Anfang und gleiches Ende ergaebe ein Fenster ohne Dauer - dann lieber den
        // ganzen Tag, statt eine Erinnerung anzulegen, die nie ausloest.
        val end = if (endParsed == start) {
            corrections += "Zeitfenster ohne Dauer → ganzer Tag"
            23 * 60 + 59
        } else {
            endParsed
        }

        val mask = raw.days
            ?.mapNotNull { DAY_ALIASES[it.trim().uppercase().take(3)] ?: DAY_ALIASES[it.trim().uppercase().take(2)] }
            ?.toSet()
            ?.takeIf { it.isNotEmpty() }
            ?.let { DaysOfWeekMask.toMask(it) }
            ?: ALL_DAYS

        // Begrenzt, weil der Text von einem Sprachmodell stammt und theoretisch beliebig lang
        // sein kann - ohne Deckel wuerde eine solche Erinnerung ungekuerzt in der DB landen und
        // die Karten-/Vorschau-Layouts sprengen (dort wird nur noch optisch abgeschnitten).
        val label = raw.label?.trim()?.takeIf { it.isNotEmpty() }?.let {
            if (it.length > MAX_LABEL_LENGTH) {
                corrections += "label gekuerzt (${it.length} → $MAX_LABEL_LENGTH Zeichen)"
                it.take(MAX_LABEL_LENGTH)
            } else {
                it
            }
        } ?: type.name

        return SanitizedReminder(
            label = label,
            animationChoice = animationChoice,
            dailyGoal = goal,
            intervalMinutes = interval,
            startMinuteOfDay = start,
            endMinuteOfDay = end,
            daysOfWeekMask = mask,
            corrections = corrections
        )
    }
}
