package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphcore.data.LibraryAnimationRepository
import com.notime.glyphcore.data.ReminderOpenDuration
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.matrix.ReminderAnimationEvent
import com.notime.glyphsim.matrix.ReminderAnimations

/**
 * Findet eine Erinnerung, die gerade noch "offen" ist - also ausgeloest hat, auf die aber noch
 * niemand reagiert hat und deren Anzeigedauer noch nicht abgelaufen ist.
 *
 * **Wozu:** tippt man das Home-Screen-Widget waehrend einer laufenden Animation an, oeffnet sich
 * die App - und zeigte bisher einfach die Uhr. Der [com.notime.glyphsim.matrix.ReminderAnimationBus]
 * hat bewusst kein Replay (ein wiederholtes altes Ereignis waere schlimmer als keins), und beim
 * Ausloesen war die App noch gar nicht offen, konnte das Ereignis also nie empfangen. Wer die
 * Erinnerung im Widget sah und daraufhin die App oeffnete, konnte den Avatar folglich nicht
 * fuettern - genau der Weg, den das Antippen nahelegt.
 *
 * Rekonstruiert wird der Zustand deshalb aus der Datenbank, die auch ein Beenden des Prozesses
 * ueberlebt: die juengste unbeantwortete Ausloesung plus die Offen-Dauer ihrer Erinnerung ergeben,
 * ob und wie lange noch etwas laeuft.
 */
object OpenReminderLookup {

    /** Eine noch laufende Erinnerung samt der Restdauer, die von ihrer Anzeige uebrig ist. */
    data class OpenReminder(val event: ReminderAnimationEvent, val remainingMillis: Long)

    /**
     * [companionProfileId] ist das anwesende Wesen: "offen" ist eine Frage an dessen Pflegebuch
     * (siehe [PresentCompanion]), nicht an den Besitzer der Routinen - gefuettert wird schliesslich
     * das Wesen, das gerade da ist.
     */
    suspend fun find(context: Context, companionProfileId: String): OpenReminder? {
        val db = AppDatabase.getInstance(context)
        val occurrence = db.avatarFeedEventDao().latestUnfed(companionProfileId) ?: return null
        val reminder = db.glyphReminderDao().getById(occurrence.reminderId) ?: return null
        if (!reminder.enabled) return null

        val elapsed = System.currentTimeMillis() - occurrence.epochMillis
        if (elapsed < 0) return null
        val remaining = if (reminder.openDurationSeconds == ReminderOpenDuration.UNTIL_FED) {
            // "Nonstop" laeuft, bis gefuettert wird - hier als Restdauer durchgereicht, der
            // Aufrufer uebersetzt das wieder in die endlose Wiedergabe.
            ReminderOpenDuration.UNTIL_FED.toLong()
        } else {
            reminder.openDurationSeconds * 1000L - elapsed
        }
        if (remaining != ReminderOpenDuration.UNTIL_FED.toLong() && remaining <= 0L) return null

        val libraryId = reminder.libraryAnimationId
        val frames = if (libraryId != null) {
            LibraryAnimationRepository(db.libraryAnimationDao()).framesFor(libraryId)
        } else {
            null
        } ?: ReminderAnimations.framesFor(reminder.animationType)
        if (frames.isEmpty()) return null

        val label = libraryId?.let { db.libraryAnimationDao().getById(it)?.label }
        return OpenReminder(
            event = ReminderAnimationEvent(
                reminderId = reminder.id,
                occurrenceId = occurrence.id,
                animationType = if (libraryId == null) reminder.animationType else null,
                libraryAnimationLabel = label,
                frames = frames,
                openDurationSeconds = reminder.openDurationSeconds
            ),
            remainingMillis = remaining
        )
    }
}
