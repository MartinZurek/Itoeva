package com.notime.glyphkalender.matrix

import android.os.SystemClock
import kotlin.math.roundToLong
import kotlinx.coroutines.delay

/**
 * Zeitbasierte Wiedergabe einer vorbereiteten Frame-Liste fuer [MatrixPreviewView] -
 * unabhaengig vom Abspiel-Loop in glyph/ReminderGlyphService.kt (der schreibt echte
 * Frames an die Hardware; hier geht's nur um die On-Screen-Vorschau beim Testen eines
 * Reminders). Gleiche Zeitbasis-Logik wie dort, inklusive derselben Rundung der
 * Zielspieldauer auf ein ganzzahliges Vielfaches der tatsaechlichen Durchlauflaenge
 * (siehe dortiger Kommentar) - garantiert, dass auch die Vorschau nie mitten in einem
 * Durchlauf abbricht.
 */
object PreviewAnimator {
    const val FRAME_DELAY_MS = 260L
    const val ANIMATION_DURATION_MS = 8000L
    private const val TICK_MS = 100L

    suspend fun play(frames: List<IntArray>, onFrame: (IntArray) -> Unit) {
        if (frames.isEmpty()) return
        val loopDurationMs = frames.size * FRAME_DELAY_MS
        val loopsToPlay = (ANIMATION_DURATION_MS.toDouble() / loopDurationMs).roundToLong().coerceAtLeast(1)
        val totalDurationMs = loopsToPlay * loopDurationMs
        val startElapsed = SystemClock.elapsedRealtime()
        var lastFrameIndex = -1
        while (true) {
            val elapsed = SystemClock.elapsedRealtime() - startElapsed
            if (elapsed >= totalDurationMs) break
            val frameIndex = (elapsed / FRAME_DELAY_MS).toInt() % frames.size
            if (frameIndex != lastFrameIndex) {
                onFrame(frames[frameIndex])
                lastFrameIndex = frameIndex
            }
            delay(TICK_MS)
        }
    }
}
