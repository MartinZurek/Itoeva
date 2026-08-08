package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphcore.reminder.PlayModeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Der Spielmodus-Schalter aus Sicht der Oberflaeche.
 *
 * **Bruecke zum gemeinsamen Kern**, genau wie [AvatarSpeciesPrefs] zu `ActiveProfilePrefs`: Die
 * eigentliche Wahrheit liegt in [PlayModeState] im Kern, weil Scheduler und Alarm-Empfaenger sie
 * auch aus kalt gestarteten Prozessen brauchen. Hier kommt nur der beobachtbare Zustand dazu -
 * der Schalter sitzt im Startbildschirm, waehrend mehrere Stellen sofort mitbekommen muessen,
 * dass umgeschaltet wurde.
 *
 * [hasSeenIntro] merkt sich, ob die Erklaerung schon einmal lief - sie erscheint nur beim
 * allerersten Einschalten (siehe [PlayModeIntroDialog]).
 */
object PlayModePrefs {
    private const val PREFS_NAME = "play_mode_prefs"
    private const val KEY_INTRO_SEEN = "intro_seen"

    private val _active = MutableStateFlow<Boolean?>(null)

    fun isActive(context: Context): Boolean {
        _active.value?.let { return it }
        val stored = PlayModeState.isActive(context)
        _active.value = stored
        return stored
    }

    /** Beobachtbarer Zustand; beim ersten Zugriff aus [isActive] befuellt. */
    fun active(context: Context): StateFlow<Boolean?> {
        isActive(context)
        return _active.asStateFlow()
    }

    fun setActive(context: Context, active: Boolean) {
        PlayModeState.setActive(context, active)
        _active.value = active
    }

    fun hasSeenIntro(context: Context): Boolean =
        prefs(context).getBoolean(KEY_INTRO_SEEN, false)

    fun markIntroSeen(context: Context) {
        prefs(context).edit().putBoolean(KEY_INTRO_SEEN, true).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
