package com.notime.glyphcore.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.notime.glyphcore.reminder.ActiveProfilePrefs
import java.time.DayOfWeek

/**
 * Eine wiederkehrende visuelle Erinnerung ("Trink was", "Beweg dich", ...): an den in
 * [daysOfWeekMask] gesetzten Wochentagen blinkt zwischen [startMinuteOfDay] und
 * [endMinuteOfDay] alle [intervalMinutes] Minuten die zu [animationType] gehoerende
 * Animation auf der simulierten Matrix auf; ausserhalb dieser Fenster laeuft die
 * normale Uhr.
 *
 * [endMinuteOfDay] < [startMinuteOfDay] bedeutet, dass das Fenster ueber Mitternacht
 * hinausgeht (z.B. 22:00-06:00) - siehe reminder/ReminderScheduler.kt fuer die
 * Slot-Berechnung.
 *
 * [libraryAnimationId] verweist optional auf eine Animation aus der erweiterbaren
 * Bibliothek ([LibraryAnimation]) statt auf einen der 12 fest eingebauten
 * [AnimationType]s - ist er gesetzt, hat er beim Abspielen Vorrang vor
 * [animationType] (der dann nur als Fallback-Wert im Schema mitgefuehrt wird).
 *
 * [profileId] ordnet die Erinnerung einem Profil zu: jedes Profil hat seinen EIGENEN Satz
 * Erinnerungen mit eigenen Einstellungen, nur die des aktiven Profils werden eingeplant
 * (siehe [ActiveProfilePrefs]). Im :app-sim-Modul entspricht ein Profil einem Tamagotchi-
 * Avatar; das :app-Modul kennt keine Avatare und bleibt beim Standardprofil.
 *
 * [openDurationSeconds] steuert, wie lange die Erinnerung offen bleibt, wenn niemand
 * reagiert (siehe [ReminderOpenDuration]). Eine Nutzerreaktion - im :app-sim-Modul das
 * Fuettern des Avatars - bricht sie unabhaengig davon jederzeit sofort ab.
 */
@Entity(tableName = "glyph_reminders")
data class GlyphReminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val animationType: AnimationType,
    val libraryAnimationId: Long? = null,
    val daysOfWeekMask: Int,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val intervalMinutes: Int,
    val enabled: Boolean = true,
    val profileId: String = ActiveProfilePrefs.DEFAULT_PROFILE_ID,
    val openDurationSeconds: Int = ReminderOpenDuration.DEFAULT_SECONDS,
    /**
     * Wie oft du diese Taetigkeit an einem Tag tatsaechlich ausfuehren willst - bewusst getrennt
     * von [intervalMinutes], das nur bestimmt, wie oft angestupst wird.
     *
     * **Warum getrennt:** ein 5-Minuten-Intervall ist kein hundertfacher Auftrag, sondern ein Netz,
     * das einen guten Moment einfangen soll. Wer viermal am Tag kurz Sport macht, hat sein Ziel
     * voll erreicht - eine Bewertung nach "beantwortet je Ausloesung" haette daraus 4 % gemacht
     * und den Avatar dauerhaft traurig. Wie oft jemand etwas TUN will, steht in keiner Beziehung
     * zur Stupsfrequenz und laesst sich daher auch nicht daraus ableiten.
     *
     * [NO_GOAL] (Standard) heisst: reiner Stupser ohne Zielvorgabe. Solche Erinnerungen gehen
     * ueberhaupt nicht in die Stimmung ein und koennen den Avatar nie truebe machen - dadurch
     * aendert sich fuer bestehende Erinnerungen nichts, und wer die Mechanik nicht will, bekommt
     * sie auch nicht.
     */
    val dailyGoal: Int = NO_GOAL,
    // Zeitpunkt (Epoch-Millis) des aktuell gesetzten Alarms fuer diese Erinnerung, oder
    // null falls gerade keiner aktiv ist. Wird von ReminderScheduler.schedule() gepflegt -
    // dient dort als Grundlage, um im "Abstand"-Kollisionsmodus zu erkennen, ob eine
    // andere Erinnerung bereits einen nahen Zeitpunkt belegt.
    val nextTriggerEpochMillis: Long? = null,
    /**
     * Vom Nutzer nicht eingerichtet, sondern vom Play Mode selbst verwaltet (siehe
     * :app-sim ui/PlayModeViewModel und matrix/PlayModeRoll): Thema und [intervalMinutes]
     * werden bei jedem [com.notime.glyphcore.reminder.ReminderScheduler.schedule]-Aufruf neu
     * gewuerfelt, passend zum Charakter des Avatars, statt fest zu stehen. Bewusst mit
     * [NO_GOAL] verknuepft (siehe Aufrufer) - Play-Mode-Ausloesungen duerfen nie in die
     * normale Stimmungsberechnung eingehen, die nur echte, vom Nutzer gesetzte Ziele zaehlt.
     */
    val isPlayMode: Boolean = false
)

/** Kein Tagesziel gesetzt - die Erinnerung stupst nur an und wird nicht bewertet. */
const val NO_GOAL = 0

/** Auswaehlbare Tagesziele im Bearbeiten-Dialog. */
val DAILY_GOAL_OPTIONS = listOf(NO_GOAL, 1, 2, 3, 4, 5, 6, 8, 10, 12)

/**
 * Auswaehlbare Intervalle in Minuten.
 *
 * Liegt im Kern und nicht im Bearbeiten-Dialog, weil inzwischen mehrere Stellen dieselbe Liste
 * brauchen: die Chips im Dialog, der gefuehrte Avatar-Assistent und die Pruefung beim KI-Import.
 * Waeren es getrennte Listen, koennte der Import einen Wert anlegen, den der Dialog gar nicht
 * darstellen kann - beim naechsten Bearbeiten stuende dann kein Chip auf ausgewaehlt, und ein
 * unbeabsichtigter Griff wuerde die Einstellung ueberschreiben.
 */
val INTERVAL_OPTIONS = listOf(1, 5, 10, 15, 20, 30, 45, 60, 90, 120)

/** Naechstliegender erlaubter Wert aus [options] - fuer Angaben aus unzuverlaessigen Quellen. */
fun snapToOption(value: Int, options: List<Int>): Int =
    options.minByOrNull { kotlin.math.abs(it - value) } ?: value

/** Bitmaske <-> Set<DayOfWeek>: Bit (DayOfWeek.value - 1), also Bit 0 = Montag ... Bit 6 = Sonntag. */
object DaysOfWeekMask {
    fun toMask(days: Set<DayOfWeek>): Int =
        days.fold(0) { mask, day -> mask or (1 shl (day.value - 1)) }

    fun toSet(mask: Int): Set<DayOfWeek> =
        DayOfWeek.entries.filter { mask and (1 shl (it.value - 1)) != 0 }.toSet()

    fun contains(mask: Int, day: DayOfWeek): Boolean =
        mask and (1 shl (day.value - 1)) != 0
}
