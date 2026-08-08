package com.notime.glyphsim.matrix

import android.os.SystemClock
import kotlin.math.roundToLong
import kotlinx.coroutines.delay

/**
 * Zeitbasierte Wiedergabe einer vorbereiteten Frame-Liste - gleiche Logik wie
 * playAnimation() in glyph/ReminderGlyphService.kt im :app-Modul (Frame wird aus der
 * tatsaechlich vergangenen Realzeit berechnet, nicht aus der Anzahl bereits
 * gemachter delay()-Aufrufe, damit ein kurzes Thread-Stocken nicht zum "Haengen"
 * fuehrt). Als suspend-Funktion gedacht fuer den Aufruf aus einem LaunchedEffect
 * heraus - Abbruch (Dialog geschlossen, Activity beendet) stoppt die Schleife
 * automatisch ueber die Coroutine-Cancellation im delay().
 *
 * Nimmt [frames] direkt entgegen statt eines [com.notime.glyphcore.data.AnimationType] -
 * so laesst sich dieselbe Wiedergabe-Logik sowohl fuer die 12 fest eingebauten
 * Animationen (ReminderAnimations.framesFor(type)) als auch fuer Animationen aus der
 * erweiterbaren Bibliothek (LibraryAnimationRepository.framesFor(id)) benutzen.
 *
 * Die Zielspieldauer wird VOR dem Start auf ein ganzzahliges Vielfaches der
 * tatsaechlichen Durchlauflaenge (frames.size * FRAME_DELAY_MS) gerundet, statt
 * einfach ANIMATION_DURATION_MS direkt als Abbruchgrenze zu nehmen - sonst konnte die
 * Schleife jederzeit mitten in einem Durchlauf abbrechen (bei Animationen mit vielen
 * Frames wurde so nicht mal ein einziger vollstaendiger Durchlauf gezeigt, z.B. 36
 * Frames * 260ms = 9360ms > 8000ms Zielspieldauer). Gerundet statt immer aufgerundet,
 * damit eine Animation, die nicht sauber in ANIMATION_DURATION_MS aufgeht, im Zweifel
 * lieber etwas kuerzer als spuerbar laenger laeuft. Jeder begonnene Durchlauf wird
 * garantiert zu Ende gespielt, mindestens einmal.
 */
object MatrixAnimator {
    const val FRAME_DELAY_MS = 260L
    const val ANIMATION_DURATION_MS = 8000L
    private const val TICK_MS = 100L

    /**
     * Deutlich schnellerer Takt als [FRAME_DELAY_MS] - ausschliesslich fuer den
     * Tamagotchi-Avatar (Idle-Loop und Reaktionen in ui/DockScreen.kt) gedacht, der als
     * reines In-App-Sprite NICHT an die Kadenz der echten Glyph-Matrix-Hardware gebunden
     * ist (anders als [FRAME_DELAY_MS], das bewusst mit ReminderGlyphService im :app-Modul
     * uebereinstimmt). Traege, verwaschene Avatar-Bewegung kam aus zwei Quellen: diesem zu
     * langsamen Takt plus den Helligkeits-Ueberblendungen aus FrameCrossfade (siehe dortiger
     * steps-Parameter, in AvatarAnimations inzwischen auf 0 gesetzt) - beides zusammen liess
     * den Avatar wirken wie durch Zeitlupe/Weichzeichner statt wie ein knackiges Pixel-Sprite.
     */
    const val AVATAR_FRAME_DELAY_MS = 120L

    /**
     * Wie [AVATAR_FRAME_DELAY_MS], nur fuer die Erinnerungs-Animationen auf der Uhr
     * ([com.notime.glyphsim.matrix.ReminderAnimations]), wenn sie in einer In-App-Ansicht laufen
     * (Startbildschirm-Uhr, Dock-Modus) statt im Home-Screen-Widget. Dieselbe Unterscheidung wie
     * beim Avatar: das Widget bleibt bewusst bei [FRAME_DELAY_MS] (RemoteViews-Update-Rhythmus,
     * ausserhalb der eigenen Kontrolle), die In-App-Ansicht dagegen ist ein ganz gewoehnliches
     * Compose-Canvas ohne diese Einschraenkung - der langsamere Takt dort war nie noetig, nur
     * nie hinterfragt.
     */
    const val CLOCK_FRAME_DELAY_MS = 120L

    /**
     * Spielt [frames] genau einmal komplett durch, ohne Dehnung/Stauchung auf
     * [ANIMATION_DURATION_MS] und ohne Loop - fuer kurze, einmalige Reaktionen (z.B. der
     * Tamagotchi-Avatar in ui/DockScreen.kt), bei denen ein Wiederholen bis zur vollen
     * Zieldauer nicht gewuenscht ist.
     */
    suspend fun playOnce(frames: List<IntArray>, frameDelayMs: Long = FRAME_DELAY_MS, onFrame: (IntArray) -> Unit) {
        frames.forEach { frame ->
            onFrame(frame)
            delay(frameDelayMs)
        }
    }

    /**
     * Wie [playOnce], aber mit einer eigenen Standzeit je Frame ([holdsMs], gleiche Laenge und
     * Reihenfolge wie [frames]).
     *
     * Ein fester Takt liess ruhige Reaktionen (Ausruhen, Einschlafen) genauso hetzen wie einen
     * Hopser und nahm Betonungen ihre Wirkung - Tempowechsel ist der eigentliche Traeger von
     * Ausdruck in einer Animation (siehe [com.notime.glyphsim.matrix.AvatarAnimations]). Fehlt
     * fuer einen Frame eine Standzeit, greift [AVATAR_FRAME_DELAY_MS] als Rueckfallwert, damit
     * eine unvollstaendige Liste die Wiedergabe nicht abschneidet.
     */
    suspend fun playTimed(frames: List<IntArray>, holdsMs: List<Long>, onFrame: (IntArray) -> Unit) {
        frames.forEachIndexed { index, frame ->
            onFrame(frame)
            delay(holdsMs.getOrElse(index) { AVATAR_FRAME_DELAY_MS })
        }
    }

    /**
     * Sentinel fuer [targetDurationMs]: laeuft endlos weiter, bis die Coroutine abgebrochen
     * wird (im Dock-Modus durch das Fuettern des Avatars, siehe
     * [com.notime.glyphcore.data.ReminderOpenDuration.UNTIL_FED]).
     */
    const val DURATION_UNTIL_FED = -1L

    /**
     * [targetDurationMs] steuert, wie lange die Erinnerung "offen" bleibt - vorher war das
     * fest [ANIMATION_DURATION_MS], jetzt pro Erinnerung einstellbar (siehe
     * [com.notime.glyphcore.data.ReminderOpenDuration]). Wie bisher wird auf ein ganzzahliges
     * Vielfaches der Durchlauflaenge gerundet, damit kein Durchlauf mittendrin abbricht;
     * [DURATION_UNTIL_FED] laeuft stattdessen unbegrenzt (Abbruch nur ueber
     * Coroutine-Cancellation).
     */
    suspend fun play(
        frames: List<IntArray>,
        targetDurationMs: Long = ANIMATION_DURATION_MS,
        // Default bleibt der langsamere, hardware-vertraegliche Takt - das Widget ruft play()
        // ohne diesen Parameter auf und bleibt damit unveraendert. In-App-Aufrufer (siehe
        // CLOCK_FRAME_DELAY_MS) reichen bewusst einen kleineren Wert durch.
        frameDelayMs: Long = FRAME_DELAY_MS,
        onFrame: (IntArray) -> Unit
    ) {
        if (frames.isEmpty()) return
        val loopDurationMs = frames.size * frameDelayMs
        val totalDurationMs = if (targetDurationMs == DURATION_UNTIL_FED) {
            Long.MAX_VALUE
        } else {
            val loopsToPlay = (targetDurationMs.toDouble() / loopDurationMs).roundToLong().coerceAtLeast(1)
            loopsToPlay * loopDurationMs
        }
        val startElapsed = SystemClock.elapsedRealtime()
        var lastFrameIndex = -1
        while (true) {
            val elapsed = SystemClock.elapsedRealtime() - startElapsed
            if (elapsed >= totalDurationMs) break
            val frameIndex = (elapsed / frameDelayMs).toInt() % frames.size
            if (frameIndex != lastFrameIndex) {
                onFrame(frames[frameIndex])
                lastFrameIndex = frameIndex
            }
            delay(TICK_MS)
        }
    }
}
