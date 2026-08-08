package com.notime.glyphkalender.glyph

import android.content.Context
import com.nothing.ketchum.GlyphMatrixManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Erster eigener Glyph Toy: zeigt fortlaufend die aktuelle Uhrzeit auf der Matrix.
 *
 * Der Sekundentakt hier und ReminderGlyphService schreiben ueber denselben
 * GlyphMatrixManager-Prozess-Singleton auf dieselbe Matrix. Waehrend eine
 * Erinnerungsanimation laeuft (`GlyphMatrixConnection.isAnimationPlaying()`), laesst
 * der Tick hier deshalb einen Frame aus statt die Animation mit der Uhrzeit zu
 * ueberschreiben - sonst wuerde spaetestens nach einer Sekunde jeder Animationsframe
 * wieder verschwinden.
 */
class ClockGlyphToyService : GlyphMatrixService("Clock-Toy") {

    private var tickerScope: CoroutineScope? = null

    override fun performOnServiceConnected(context: Context, glyphMatrixManager: GlyphMatrixManager) {
        val scope = CoroutineScope(Dispatchers.Default)
        tickerScope = scope
        scope.launch {
            while (isActive) {
                if (!GlyphMatrixConnection.isAnimationPlaying()) {
                    ClockFrame.draw(applicationContext, glyphMatrixManager)
                }
                delay(1000)
            }
        }
    }

    override fun performOnServiceDisconnected(context: Context) {
        tickerScope?.cancel()
        tickerScope = null
    }
}
