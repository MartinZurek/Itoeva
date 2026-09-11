package com.notime.glyphsim.ui

import com.notime.glyphcore.data.AnimationType

/**
 * Vier feste Plaetze statt einer wachsenden Liste halten Auswahl und spaeteres Overlay deckungsgleich.
 */
internal const val ACTION_SLOT_COUNT = 4

/**
 * Eine aufbewahrte Ausloesung, die erst bei einer tatsaechlichen Reaktion verbraucht wird.
 *
 * Die Vorschau bleibt Teil desselben Datums wie die typisierte Bedeutung. Damit muss ein
 * Stream-Client weder eine zweite Reminder-Darstellung noch freie Beschriftungen erfinden.
 */
internal data class SavedAction(
    val reminderId: Long,
    val occurrenceId: Long,
    val animationType: AnimationType?,
    val libraryAnimationLabel: String?,
    val frames: List<IntArray>
)
