package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Verbindet ReminderAlarmReceiver mit den In-App-Matrix-Ansichten (HomeScreen,
 * DockScreen): das Home-Screen-Widget bekommt eine faellige Erinnerungs-Animation
 * bereits direkt ueber RemoteViews (siehe widget/GlyphClockWidgetProvider), die
 * In-App-Ansichten liefen bisher aber komplett unabhaengig davon und zeigten immer
 * nur die tickende Uhr, nie eine Animation. Dieser Bus reicht dieselbe Frame-Liste
 * zusaetzlich an alle gerade sichtbaren In-App-Views weiter, solange der Prozess
 * laeuft - extraBufferCapacity statt replay, damit ein Kollisionsfall (mehrere
 * Erinnerungen kurz hintereinander) der Reihe nach abgespielt statt verworfen wird.
 *
 * [ReminderAnimationEvent] traegt neben den Frames auch [ReminderAnimationEvent.reminderId]
 * und [ReminderAnimationEvent.animationType] mit - DockScreen braucht beides fuer den
 * Tamagotchi-Avatar (welche Reaktion zeigen, welche Erinnerung als "gefuettert" tracken).
 * [animationType] ist null, wenn die Erinnerung eine Custom-Bibliotheksanimation
 * ([com.notime.glyphcore.data.LibraryAnimation]) statt eines festen Typs verwendet - in dem
 * Fall traegt [libraryAnimationLabel] stattdessen deren Label (z.B. "Rocket"), damit
 * AvatarAnimations vereinzelten Bibliotheks-Animationen trotzdem eine individuelle statt der
 * generischen Fallback-Reaktion geben kann.
 */
data class ReminderAnimationEvent(
    val reminderId: Long,
    /**
     * Zeilen-ID dieses einen Auftretens in `avatar_feed_events` (siehe ReminderAlarmReceiver).
     * Reagiert der Nutzer, wird genau diese Zeile als beantwortet markiert - ueber die
     * Erinnerungs-ID allein liesse sich das nicht zuordnen, weil dieselbe Erinnerung immer
     * wieder ausloest.
     */
    val occurrenceId: Long,
    val animationType: AnimationType?,
    val libraryAnimationLabel: String?,
    val frames: List<IntArray>,
    /**
     * Wie lange diese Erinnerung offen bleibt, wenn niemand reagiert (siehe
     * [com.notime.glyphcore.data.ReminderOpenDuration]) - der Dock-Modus laesst die Animation
     * entsprechend lange laufen, das Fuettern des Avatars bricht sie jederzeit vorzeitig ab.
     */
    val openDurationSeconds: Int = com.notime.glyphcore.data.ReminderOpenDuration.DEFAULT_SECONDS
)

object ReminderAnimationBus {
    private val _events = MutableSharedFlow<ReminderAnimationEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<ReminderAnimationEvent> = _events

    fun emit(event: ReminderAnimationEvent) {
        _events.tryEmit(event)
    }
}
