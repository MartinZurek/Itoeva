package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphsim.matrix.PlayAmbientActivity
import com.notime.glyphsim.matrix.PlayTimeLapse

/**
 * Merkt sich, wann sich ein Wesen zuletzt bewegt hat - die Ablage zu
 * [PlayAmbientActivity.movementUrge].
 *
 * **In Weltminuten** ([PlayTimeLapse.absoluteMinute]), nicht in Uhrzeit: Der Drang soll im
 * selben Takt wachsen wie der Tag, den man sieht. Im Zeitraffer vergeht ein Tag in Minuten, und
 * ein Drang, der an der echten Uhr haengt, kaeme dort nie an.
 *
 * **Anders als die Verlaufserinnerung in DockScreen dauerhaft.** Die zaehlt nur, was man eben
 * gesehen hat; hier geht es um den Tag des Wesens. Wer morgens den Spielmodus verlaesst und
 * nachmittags wiederkommt, soll ein Wesen antreffen, das seit dem Morgen still war - und das
 * merkt man ihm an.
 *
 * Je Wesen, aus demselben Grund wie bei [PlayCharacterThemeLog]: Wer den Avatar wechselt, hat
 * ein anderes Wesen vor sich, mit eigenem Tag.
 */
object PlayMovementLog {

    private const val PREFS = "play_movement"

    private fun key(profileId: String) = "last_moved_minute_$profileId"

    /** Wie viele Weltminuten seit der letzten Bewegung vergangen sind - `null` heisst "noch nie". */
    fun minutesSinceMove(context: Context, profileId: String, nowMinute: Int = PlayTimeLapse.absoluteMinute()): Long? {
        val stored = prefs(context).getLong(key(profileId), Long.MIN_VALUE)
        if (stored == Long.MIN_VALUE) return null
        return (nowMinute - stored).coerceAtLeast(0L)
    }

    /** Haelt fest, dass sich das Wesen jetzt bewegt. */
    fun moved(context: Context, profileId: String, nowMinute: Int = PlayTimeLapse.absoluteMinute()) {
        prefs(context).edit().putLong(key(profileId), nowMinute.toLong()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
