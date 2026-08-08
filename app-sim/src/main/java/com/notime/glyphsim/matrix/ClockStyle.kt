package com.notime.glyphsim.matrix

import androidx.annotation.StringRes
import com.notime.glyphsim.R

/**
 * Waehlbare Ziffernblatt-Designs fuer die simulierte Matrix (siehe [ClockFrameSim]).
 *
 * Bezeichnung und Beschreibung liegen als Ressourcen-IDs vor, damit sie uebersetzbar sind
 * (siehe ui/LanguagePrefs.kt). Der in den Einstellungen gespeicherte Wert ist weiterhin der
 * Enum-Name, ein Sprachwechsel beruehrt die Auswahl also nicht.
 */
enum class ClockStyle(
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int
) {
    CLASSIC(R.string.clock_style_classic, R.string.clock_style_classic_desc),
    COMPACT(R.string.clock_style_compact, R.string.clock_style_compact_desc),
    MINIMAL(R.string.clock_style_minimal, R.string.clock_style_minimal_desc),
    RING(R.string.clock_style_ring, R.string.clock_style_ring_desc)
}
