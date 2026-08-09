package com.notime.glyphkalender.ui

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.DaysOfWeekMask

/**
 * Rettet die halb fertigen Eingaben im Erinnerungs-Dialog ueber einen Neuaufbau der Activity.
 *
 * ## Wogegen
 *
 * `remember` haelt nur, solange die Komposition lebt. Sie stirbt bei jedem Neuaufbau der Activity
 * und beim **Prozesstod** - und der ist bei dieser App nicht der Ausnahmefall: Android raeumt Apps
 * im Hintergrund ab, und wer waehrend des Ausfuellens kurz in ChatGPT nachsieht, wie er seine
 * Erinnerung nennen will, kommt womoeglich in eine frisch aufgebaute App zurueck. Bis hierher war
 * das Formular dann leer - ohne Hinweis, ohne Rueckfrage.
 *
 * `rememberSaveable` legt den Zustand stattdessen in dem `Bundle` ab, das Android beim Abraeumen
 * sichert. Dafuer muss der Wert allerdings in ein Bundle passen; eine [ReminderDraft] mit einem
 * versiegelten Typ und einem `Set<DayOfWeek>` darin passt nicht. Diese Datei uebersetzt beides in
 * einfache Werte und wieder zurueck.
 *
 * ## Warum von Hand und nicht `Serializable`
 *
 * [ReminderDraft] liesse sich als `Serializable` markieren und waere damit ohne weiteres
 * speicherbar. Das koppelt aber das gespeicherte Format an die Klassenstruktur: ein umbenanntes
 * Feld, und ein alter Zustand aus dem Bundle laesst sich nicht mehr lesen - im schlimmsten Fall
 * mit einer Ausnahme beim Wiederherstellen, also einem Absturz genau in dem Moment, in dem der
 * Nutzer zurueckkommt. Die Uebersetzung hier ist ein paar Zeilen laenger und dafuer ausdruecklich.
 */

/**
 * [AnimationChoice] als flache Liste. Das erste Element unterscheidet die beiden Faelle, damit
 * beim Zurueckwandeln klar ist, was in den uebrigen steht.
 */
private fun encodeChoice(choice: AnimationChoice): List<Any?> = when (choice) {
    is AnimationChoice.BuiltIn -> listOf(KIND_BUILT_IN, choice.type.name)
    is AnimationChoice.Library -> listOf(KIND_LIBRARY, choice.id, choice.label, choice.emoji)
}

private fun decodeChoice(values: List<Any?>): AnimationChoice? = when (values.firstOrNull()) {
    KIND_BUILT_IN -> {
        // Ein Enum-Name aus einem alten Bundle kann inzwischen weggefallen sein - dann lieber der
        // Standardtyp als eine Ausnahme beim Wiederherstellen.
        val type = runCatching { AnimationType.valueOf(values[1] as String) }
            .getOrDefault(AnimationType.GENERAL)
        AnimationChoice.BuiltIn(type)
    }
    KIND_LIBRARY -> AnimationChoice.Library(
        id = values[1] as Long,
        label = values[2] as String,
        emoji = values[3] as String
    )
    else -> null
}

/** Fuer den Animations-Picker, der seine Auswahl ebenfalls ueber einen Neuaufbau halten soll. */
val AnimationChoiceSaver: Saver<AnimationChoice, Any> = listSaver(
    save = { encodeChoice(it) },
    restore = { decodeChoice(it) }
)

/**
 * [ReminderDraft] als flache Liste: erst die fuenf festen Felder, dann die Animationsauswahl.
 *
 * Die Wochentage wandern als Bitmaske hinein - dieselbe Darstellung, in der sie ohnehin in der
 * Datenbank stehen (siehe [DaysOfWeekMask]), statt einer zweiten Kodierung nur fuer diesen Zweck.
 */
internal val ReminderDraftSaver: Saver<ReminderDraft?, Any> = listSaver(
    save = { draft ->
        // Leere Liste heisst "kein Entwurf". Ohne diesen Fall koennte der Saver nicht
        // unterscheiden, ob nichts gespeichert war oder ob der Wert null lautete.
        if (draft == null) emptyList() else listOf(
            draft.label,
            DaysOfWeekMask.toMask(draft.selectedDays),
            draft.startMinute,
            draft.endMinute,
            draft.interval
        ) + encodeChoice(draft.animationChoice)
    },
    restore = { values ->
        if (values.isEmpty()) null else decodeChoice(values.drop(FIXED_FIELDS))?.let { choice ->
            ReminderDraft(
                label = values[0] as String,
                animationChoice = choice,
                selectedDays = DaysOfWeekMask.toSet(values[1] as Int),
                startMinute = values[2] as Int,
                endMinute = values[3] as Int,
                interval = values[4] as Int
            )
        }
    }
)

/**
 * `Set<DayOfWeek>` als Bitmaske - dieselbe Darstellung wie in der Datenbank.
 *
 * Ein `Set` waere zwar als `Serializable` speicherbar, die Maske ist aber ein einzelnes Int statt
 * eines serialisierten Objektgraphen und ohnehin schon das Format, in dem die Wochentage im Rest
 * der App unterwegs sind.
 */
val DaysOfWeekSaver: Saver<Set<java.time.DayOfWeek>, Any> = Saver(
    save = { DaysOfWeekMask.toMask(it) },
    restore = { DaysOfWeekMask.toSet(it as Int) }
)

/** Anzahl der Felder vor der Animationsauswahl - siehe [ReminderDraftSaver]. */
private const val FIXED_FIELDS = 5
private const val KIND_BUILT_IN = "B"
private const val KIND_LIBRARY = "L"
