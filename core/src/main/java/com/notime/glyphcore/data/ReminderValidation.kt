package com.notime.glyphcore.data

/**
 * Die Regeln, denen eine [GlyphReminder] genuegen muss, um gespeichert werden zu duerfen.
 *
 * ## Warum das hier steht und nicht im Dialog
 *
 * Dieselben Regeln galten vorher schon - aber verstreut und jeweils nur an einer Stelle. Der
 * Bearbeiten-Dialog von :app-sim schaltet seinen Speichern-Knopf frei ueber
 * `label.isNotBlank() && selectedDays.isNotEmpty() && startMinute != endMinute`; der Dialog von
 * :app hat seine eigene Fassung; der KI-Import prueft noch einmal anders. Auf keinem dieser Wege
 * kommen die Erinnerungen des Spielmodus zustande, und die entstehen ganz ohne Pruefung.
 *
 * Die Regeln gehoeren aber nicht der Oberflaeche - sie gehoeren der Erinnerung. Eine Erinnerung
 * ohne aktive Wochentage feuert nie; eine mit `startMinuteOfDay == endMinuteOfDay` hat ein Fenster
 * von null Minuten; eine mit einem Intervall, das der Dialog nicht darstellen kann, steht beim
 * naechsten Bearbeiten auf keinem Chip - und ein unbeabsichtigter Griff ueberschreibt sie
 * stillschweigend. Solche Zeilen liegen dann dauerhaft in der Datenbank und lassen sich weder
 * ausloesen noch sinnvoll reparieren.
 *
 * ## Wie streng
 *
 * Unterschieden wird, wer die Erinnerung angelegt hat:
 *
 * - **Vom Nutzer eingerichtet** - hier gilt zusaetzlich, dass Intervall, Tagesziel und Offenzeit
 *   aus den Listen stammen muessen, die der Dialog anbietet ([INTERVAL_OPTIONS],
 *   [DAILY_GOAL_OPTIONS], [ReminderOpenDuration.OPTIONS]). Nur dann laesst sich die Erinnerung
 *   spaeter unfallfrei wieder bearbeiten.
 * - **Vom Spielmodus gewuerfelt** ([GlyphReminder.isPlayMode]) - dort sind die Intervalle
 *   bewusst krumm (3, 7, 12 Minuten, siehe `PlayGamePlan`), weil sie nie in einem Dialog
 *   erscheinen. Fuer sie gelten nur die harten Grenzen.
 *
 * Die Meldungen sind **Diagnosetexte fuer Entwickler**, keine Oberflaechentexte: sie tauchen in
 * Ausnahmen und Protokollen auf. Was der Nutzer zu sehen bekommt, entscheiden die Dialoge -
 * uebersetzt und in ihren eigenen Worten.
 */
object ReminderValidation {

    /**
     * Genug fuer jede sinnvolle Bezeichnung, kurz genug, um Karten- und Vorschau-Layouts nicht zu
     * sprengen. Frueher eine private Konstante im KI-Import; sie gilt fuer jede Erinnerung, nicht
     * nur fuer importierte.
     */
    const val MAX_LABEL_LENGTH = 40

    /** Minuten eines Tages: 0 (00:00) bis 1439 (23:59). */
    val MINUTE_OF_DAY_RANGE = 0..1439

    /** Alle sieben Wochentagsbits gesetzt - die groesstmoegliche Maske. */
    const val ALL_DAYS_MASK = 0b111_1111

    /** Ein Intervall unter einer Minute ergibt keinen Slot, eines ueber einem Tag kein Muster. */
    val INTERVAL_RANGE = 1..1440

    /** Was an einer Erinnerung nicht stimmt. Ein Fall je Regel, mit dem beanstandeten Wert. */
    sealed interface Problem {
        val message: String

        data object BlankLabel : Problem {
            override val message = "Die Bezeichnung ist leer."
        }

        data class LabelTooLong(val length: Int) : Problem {
            override val message =
                "Die Bezeichnung ist $length Zeichen lang, erlaubt sind hoechstens $MAX_LABEL_LENGTH."
        }

        data class MaskOutOfRange(val mask: Int) : Problem {
            override val message =
                "Die Wochentagsmaske $mask liegt ausserhalb von 0..$ALL_DAYS_MASK."
        }

        data object NoDaysSelected : Problem {
            override val message = "Kein Wochentag aktiv - die Erinnerung koennte nie ausloesen."
        }

        data class MinuteOutOfRange(val field: String, val value: Int) : Problem {
            override val message =
                "$field = $value liegt ausserhalb von ${MINUTE_OF_DAY_RANGE.first}..${MINUTE_OF_DAY_RANGE.last}."
        }

        data object EmptyWindow : Problem {
            override val message =
                "Start- und Endzeit sind gleich - das Zeitfenster waere null Minuten lang."
        }

        data class IntervalOutOfRange(val minutes: Int) : Problem {
            override val message =
                "Intervall $minutes min liegt ausserhalb von ${INTERVAL_RANGE.first}..${INTERVAL_RANGE.last}."
        }

        data class IntervalNotSelectable(val minutes: Int) : Problem {
            override val message =
                "Intervall $minutes min steht nicht in INTERVAL_OPTIONS - der Dialog koennte es " +
                    "nicht anzeigen und wuerde es beim naechsten Bearbeiten ueberschreiben."
        }

        data class DailyGoalNotSelectable(val goal: Int) : Problem {
            override val message = "Tagesziel $goal steht nicht in DAILY_GOAL_OPTIONS."
        }

        data class OpenDurationInvalid(val seconds: Int) : Problem {
            override val message = "Offenzeit $seconds s ist weder UNTIL_FED noch ein positiver Wert."
        }

        data class OpenDurationNotSelectable(val seconds: Int) : Problem {
            override val message = "Offenzeit $seconds s steht nicht in ReminderOpenDuration.OPTIONS."
        }

        data object BlankProfileId : Problem {
            override val message =
                "Die Profil-Id ist leer - die Erinnerung gehoerte zu keinem Avatar und wuerde nie eingeplant."
        }
    }

    /**
     * Prueft [reminder] und gibt **alle** Beanstandungen zurueck; eine leere Liste heisst
     * "darf gespeichert werden".
     *
     * Bewusst alle statt der ersten: wer eine Erinnerung repariert, will nicht nach jedem
     * behobenen Punkt erneut anstossen, um den naechsten zu erfahren.
     */
    fun validate(reminder: GlyphReminder): List<Problem> = buildList {
        val label = reminder.label.trim()
        if (label.isEmpty()) add(Problem.BlankLabel)
        if (label.length > MAX_LABEL_LENGTH) add(Problem.LabelTooLong(label.length))

        if (reminder.daysOfWeekMask !in 0..ALL_DAYS_MASK) {
            add(Problem.MaskOutOfRange(reminder.daysOfWeekMask))
        } else if (reminder.daysOfWeekMask == 0) {
            add(Problem.NoDaysSelected)
        }

        if (reminder.startMinuteOfDay !in MINUTE_OF_DAY_RANGE) {
            add(Problem.MinuteOutOfRange("startMinuteOfDay", reminder.startMinuteOfDay))
        }
        if (reminder.endMinuteOfDay !in MINUTE_OF_DAY_RANGE) {
            add(Problem.MinuteOutOfRange("endMinuteOfDay", reminder.endMinuteOfDay))
        }
        // Gleich heisst leer, nicht "ganzer Tag". Ein Fenster ueber Mitternacht (Ende < Start,
        // z.B. 22:00-06:00) ist dagegen ausdruecklich erlaubt - siehe GlyphReminder.
        if (reminder.startMinuteOfDay == reminder.endMinuteOfDay) add(Problem.EmptyWindow)

        if (reminder.intervalMinutes !in INTERVAL_RANGE) {
            add(Problem.IntervalOutOfRange(reminder.intervalMinutes))
        }

        if (reminder.openDurationSeconds != ReminderOpenDuration.UNTIL_FED &&
            reminder.openDurationSeconds <= 0
        ) {
            add(Problem.OpenDurationInvalid(reminder.openDurationSeconds))
        }

        if (reminder.profileId.isBlank()) add(Problem.BlankProfileId)

        // Ab hier nur noch das, was den Bearbeiten-Dialog betrifft - die gewuerfelten
        // Spielmodus-Zeilen erscheinen dort nie und duerfen krumme Werte tragen.
        if (!reminder.isPlayMode) {
            if (reminder.intervalMinutes in INTERVAL_RANGE &&
                reminder.intervalMinutes !in INTERVAL_OPTIONS
            ) {
                add(Problem.IntervalNotSelectable(reminder.intervalMinutes))
            }
            if (reminder.dailyGoal !in DAILY_GOAL_OPTIONS) {
                add(Problem.DailyGoalNotSelectable(reminder.dailyGoal))
            }
            if (reminder.openDurationSeconds !in ReminderOpenDuration.OPTIONS) {
                add(Problem.OpenDurationNotSelectable(reminder.openDurationSeconds))
            }
        }
    }

    /** Kurzform fuer Aufrufer, die nur wissen wollen, ob etwas gespeichert werden darf. */
    fun isValid(reminder: GlyphReminder): Boolean = validate(reminder).isEmpty()
}

/**
 * Fliegt, wenn eine Erinnerung gespeichert werden soll, die den Regeln nicht genuegt.
 *
 * **Warum eine Ausnahme und keine stille Korrektur.** An dieser Stelle ist ein ungueltiger Wert
 * kein Nutzerfehler mehr - die Oberflaechen lassen ihn gar nicht erst zu und der KI-Import zieht
 * ihn vorher zurecht (siehe `ReminderImport.sanitize`). Was hier ankommt, ist ein Fehler im
 * Programm. Ihn stillschweigend zurechtzubiegen hiesse, die Ursache zu verstecken und dem Nutzer
 * eine Erinnerung unterzuschieben, die er so nicht eingerichtet hat.
 *
 * Der Zeitpunkt ist bewusst frueh: lieber beim Schreiben scheitern, wo der Ausloeser noch im
 * Stapelabbild steht, als Wochen spaeter beim Einplanen einer Zeile, die niemand mehr zuordnen kann.
 */
class InvalidReminderException(
    val problems: List<ReminderValidation.Problem>
) : IllegalArgumentException(
    "Erinnerung nicht speicherbar:\n" + problems.joinToString("\n") { "  - ${it.message}" }
)
