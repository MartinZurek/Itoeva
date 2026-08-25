package com.notime.glyphsim.skilltree

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Haelt fest, was der Begleiter gerade tut - beobachtbar, damit mehrere Ansichten dasselbe sehen.
 *
 * **Warum im Speicher und nicht in der Datenbank.** Eine laufende Beschaeftigung ist ein Zustand
 * der Sitzung, kein Besitz: Sie laeuft ohnehin nach [AvatarActivity.LIFETIME_MS] ab, und ueber
 * einen Neustart hinweg "spielt noch Ball" zu behaupten waere schlicht falsch. Gespeichert wird
 * nur, was jemandem gehoert - der Freischalt-Stand (siehe [AvatarUnlockRepository]).
 *
 * Als Objekt und nicht als Zustand eines einzelnen Bildschirms, weil derselbe Begleiter auf dem
 * Startbildschirm und im Dock-Modus derselbe sein soll. Dasselbe Muster wie
 * `ReminderAnimationBus`.
 */
object AvatarActivityBus {

    private val _current = MutableStateFlow<AvatarActivity?>(null)

    /** Ohne Ablauf-Pruefung - wer entscheiden will, nimmt [currentIfFresh]. */
    val current: StateFlow<AvatarActivity?> = _current.asStateFlow()

    /**
     * Die laufende Beschaeftigung, oder `null`, wenn keine laeuft oder die letzte abgelaufen ist.
     *
     * Raeumt die abgelaufene dabei gleich weg: Sonst zeigte eine beobachtende Anzeige weiterhin
     * etwas an, das fuer jede Entscheidung schon nicht mehr gilt.
     */
    fun currentIfFresh(nowMillis: Long): AvatarActivity? {
        val activity = _current.value ?: return null
        if (activity.isStale(nowMillis)) {
            _current.value = null
            return null
        }
        return activity
    }

    fun set(nodeId: String, nowMillis: Long) {
        _current.value = AvatarActivity(nodeId, nowMillis)
    }

    /** Nur fuer Pruefungen - der Zustand ist prozessweit und muss zwischen Tests zurueck. */
    internal fun reset() {
        _current.value = null
    }
}
