package com.notime.glyphsim.ui

import androidx.annotation.StringRes
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.R

/**
 * Was fuer ein Erinnerungs-Rhythmus zu einem Thema passt.
 *
 * **Warum nach Rhythmus gruppiert und nicht je Thema einzeln:** die zwoelf Themen unterscheiden
 * sich nicht in zwoelf Richtungen, sondern fallen in wenige Muster. Trinken lebt davon, oft
 * angestupst zu werden; Sport bringt mehr als ein paar Mal am Tag nichts; eine Tablette darf man
 * nicht verpassen; einschlafen tut man einmal. Diese Muster einmal zu beschreiben ist ehrlicher
 * als zwoelf handverlesene Zahlenreihen - und ein neues Thema ordnet sich einfach einem
 * bestehenden Muster zu, statt eigene Werte zu brauchen.
 *
 * [hintRes] ist das, was der Avatar zum Rhythmus sagt - dadurch wirkt die Empfehlung begruendet
 * statt willkuerlich.
 */
enum class ReminderRhythm(
    val goalOptions: List<Int>,
    val suggestedGoal: Int,
    val intervalOptions: List<Int>,
    val suggestedInterval: Int,
    val suggestedWindow: TimeWindowOption,
    @StringRes val hintRes: Int
) {
    /** Trinken: viele kleine Male, haeufiges Anstupsen ist hier ausdruecklich sinnvoll. */
    FREQUENT(
        goalOptions = listOf(4, 6, 8, 10, 12),
        suggestedGoal = 8,
        intervalOptions = listOf(15, 30, 45, 60),
        suggestedInterval = 45,
        suggestedWindow = TimeWindowOption.ALL_DAY,
        hintRes = R.string.rhythm_frequent
    ),

    /** Bewegung, Pause, Fokus: ein paar Mal am Tag, mit spuerbarem Abstand dazwischen. */
    SPACED(
        goalOptions = listOf(2, 3, 4, 5, 6),
        suggestedGoal = 4,
        intervalOptions = listOf(30, 60, 90, 120),
        suggestedInterval = 60,
        suggestedWindow = TimeWindowOption.WORKDAY,
        hintRes = R.string.rhythm_spaced
    ),

    /** Achtsamkeit, Lesen, Kreatives: selten, dafuer in Ruhe - Draengen waere kontraproduktiv. */
    RARE(
        goalOptions = listOf(1, 2, 3),
        suggestedGoal = 2,
        intervalOptions = listOf(60, 120, 180),
        suggestedInterval = 120,
        suggestedWindow = TimeWindowOption.ALL_DAY,
        hintRes = R.string.rhythm_rare
    ),

    /** Medikamente: darf nicht untergehen, deshalb bewusst engmaschig anstupsen. */
    CRITICAL(
        goalOptions = listOf(1, 2, 3, 4),
        suggestedGoal = 2,
        intervalOptions = listOf(5, 10, 15, 30),
        suggestedInterval = 15,
        suggestedWindow = TimeWindowOption.ALL_DAY,
        hintRes = R.string.rhythm_critical
    ),

    /** Schlafen: einmal am Abend, ein Tagesziel groesser eins ergaebe keinen Sinn. */
    ONCE(
        goalOptions = listOf(1),
        suggestedGoal = 1,
        intervalOptions = listOf(10, 15, 30),
        suggestedInterval = 15,
        suggestedWindow = TimeWindowOption.EVENING,
        hintRes = R.string.rhythm_once
    ),

    /** Alles ohne ausgepraegtes Muster - volle Bandbreite, keine Empfehlung mit Anspruch. */
    FLEXIBLE(
        goalOptions = listOf(1, 2, 3, 4, 6, 8),
        suggestedGoal = 3,
        intervalOptions = listOf(15, 30, 60, 120),
        suggestedInterval = 60,
        suggestedWindow = TimeWindowOption.ALL_DAY,
        hintRes = R.string.rhythm_flexible
    );

    companion object {
        /** Ordnet jedem Thema sein Muster zu - siehe Klassendoku fuer die Begruendung. */
        fun forType(type: AnimationType): ReminderRhythm = when (type) {
            AnimationType.DRINK -> FREQUENT
            AnimationType.MOVE, AnimationType.REST, AnimationType.FOCUS -> SPACED
            AnimationType.MINDFULNESS, AnimationType.BOOK,
            AnimationType.CREATIVITY, AnimationType.LOVE -> RARE
            AnimationType.MEDICINE -> CRITICAL
            AnimationType.SLEEP -> ONCE
            AnimationType.WORK, AnimationType.GENERAL -> FLEXIBLE
        }
    }
}

/** Zeitfenster-Vorschlaege - Uhrzeiten von Hand einzugeben waere im Gespraech zu umstaendlich. */
enum class TimeWindowOption(@StringRes val labelRes: Int, val startMinute: Int, val endMinute: Int) {
    ALL_DAY(R.string.assistant_window_all_day, 0, 23 * 60 + 59),
    MORNING(R.string.assistant_window_morning, 8 * 60, 12 * 60),
    WORKDAY(R.string.assistant_window_workday, 9 * 60, 17 * 60),
    EVENING(R.string.assistant_window_evening, 18 * 60, 22 * 60)
}
