package com.notime.glyphkalender.glyph

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Verfolgt zwei Dinge ueber die (Prozess-Singleton-)Verbindung von GlyphMatrixManager:
 *
 * 1. Ob die Verbindung gerade besteht ([isConnected]). Noetig, weil ein zweiter
 *    init()-Aufruf auf eine bereits verbundene ServiceConnection kein erneutes
 *    onServiceConnected ausloest (Android bindet intern nur einmal) - ohne diesen
 *    Zustand wuerde z.B. ReminderGlyphService bei jedem weiteren Aufruf nach
 *    ClockGlyphToyService (oder einem vorherigen eigenen Lauf) auf einen Callback
 *    warten, der nie mehr kommt.
 *
 * 2. Ob gerade eine Erinnerungsanimation laeuft ([isAnimationPlaying]). Noetig, weil
 *    ClockGlyphToyService unabhaengig von ReminderGlyphService einen eigenen
 *    Sekundentakt hat, der ueber denselben Manager auf dieselbe Matrix schreibt
 *    (siehe Klassenkommentar dort) - ohne diesen Zustand wuerde der Uhr-Tick
 *    spaetestens nach einer Sekunde jeden Animationsframe wieder ueberschreiben,
 *    wodurch die Animation nur ganz am Anfang sichtbar bewegt wirkt.
 */
object GlyphMatrixConnection {
    private val connected = AtomicBoolean(false)
    private val animationPlaying = AtomicBoolean(false)

    fun markConnected() = connected.set(true)
    fun markDisconnected() = connected.set(false)
    fun isConnected(): Boolean = connected.get()

    fun markAnimationPlaying() = animationPlaying.set(true)
    fun markAnimationStopped() = animationPlaying.set(false)
    fun isAnimationPlaying(): Boolean = animationPlaying.get()
}
