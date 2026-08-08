package com.notime.glyphkalender.ui

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphcore.data.LibraryAnimation

/**
 * Welche Animation ein Reminder abspielt: entweder einer der 12 fest eingebauten
 * [AnimationType]s, oder eine per Id referenzierte Animation aus der erweiterbaren
 * Bibliothek (siehe LibraryAnimation.kt). Rein UI-seitiger Typ - in der Datenbank
 * landet das als (animationType, libraryAnimationId) auf [com.notime.glyphcore.data.GlyphReminder].
 * 1:1 uebernommen aus dem :app-sim-Modul.
 */
sealed interface AnimationChoice {
    data class BuiltIn(val type: AnimationType) : AnimationChoice
    data class Library(val id: Long, val label: String, val emoji: String) : AnimationChoice
}

/**
 * Loest die aktuelle [AnimationChoice] eines Reminders auf. Faellt auf [AnimationType]
 * zurueck, falls [GlyphReminder.libraryAnimationId] gesetzt ist, die referenzierte
 * Bibliotheks-Animation aber inzwischen geloescht wurde.
 */
fun GlyphReminder.animationChoice(libraryAnimations: List<LibraryAnimation>): AnimationChoice {
    val libraryId = libraryAnimationId
    if (libraryId != null) {
        val library = libraryAnimations.find { it.id == libraryId }
        if (library != null) return AnimationChoice.Library(library.id, library.label, library.emoji)
    }
    return AnimationChoice.BuiltIn(animationType)
}

/** Wandelt eine [AnimationChoice] zurueck in die beiden auf [GlyphReminder] gespeicherten Felder. */
fun AnimationChoice.toStorage(): Pair<AnimationType, Long?> = when (this) {
    is AnimationChoice.BuiltIn -> type to null
    is AnimationChoice.Library -> AnimationType.GENERAL to id
}
