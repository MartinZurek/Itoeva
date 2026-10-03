package com.notime.glyphsim.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.notime.glyphsim.matrix.PlayAmbience
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * **Die Atmo unter der Musik** - Regen, Brandung, Wind, Grillen, Stadt, Cafe (siehe
 * [PlayAmbience]).
 *
 * ## Wann sie zu hoeren ist
 *
 * **Nur zusammen mit der Musik** und damit unter all ihren Sperren (siehe [PlayMusic]): Der
 * Nutzer hat Musik eingeschaltet, kein fremder Ton laeuft, das Geraet ist nicht stumm, der
 * Spielmodus ist zu sehen. Die Atmo ist Teil desselben Klangbilds und bekommt deshalb keinen
 * eigenen Schalter - wer die Musik ausschaltet, will Ruhe, nicht nur keine Melodie.
 *
 * ## Wie
 *
 * Je Klangbild eine gerechnete, nahtlose Schleife in einem `AudioTrack` im statischen Modus mit
 * Schleifenpunkten - einmal geschrieben, laeuft sie ohne mitlaufenden Schreiber. Beim Wechsel
 * blendet die alte aus und die neue ein, wie die Musik zwischen zwei Stuecken. Gerechnete
 * Schleifen werden behalten; ein zweiter Besuch am Strand rechnet nicht neu.
 */
object PlayAmbienceSound {

    private const val TAG = "PlayAmbience"

    /** Wie laut die Atmo unter der Musik liegt - sie traegt, sie fuehrt nie. */
    private val VOLUME = if (com.notime.glyphsim.BuildConfig.BUILD_TYPE == "stream") 0.11f else 0.22f

    /** Die Ueberblendung zwischen zwei Klangbildern. */
    private const val FADE_MS = 2_500L
    private const val FADE_STEPS = 25

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val cache = HashMap<PlayAmbience.Kind, ShortArray>()

    private var current: AudioTrack? = null

    /** Die zuletzt gesetzte Lautstaerke je Spur - ein abgebrochener Uebergang setzt dort fort. */
    private val volumes = HashMap<AudioTrack, Float>()
    private var currentKind: PlayAmbience.Kind? = null
    private var job: Job? = null
    @Volatile private var streamAttention = 1f

    @Synchronized
    internal fun setStreamAttention(value: Float) {
        if (com.notime.glyphsim.BuildConfig.BUILD_TYPE != "stream") return
        streamAttention = value.coerceIn(0f, 1f)
        current?.let { track ->
            val raw = synchronized(volumes) { volumes[track] } ?: VOLUME
            setVolume(track, raw)
        }
    }

    /**
     * Stellt das Klangbild [kind] ein - `null` blendet aus. [allowed] ist die Antwort der Musik
     * auf die Frage, ob gerade ueberhaupt Klang sein darf (siehe [PlayMusic.isPlaying]).
     */
    @Synchronized
    fun apply(kind: PlayAmbience.Kind?, allowed: Boolean) {
        val wanted = kind.takeIf { allowed }
        if (wanted == currentKind) return
        currentKind = wanted
        val outgoing = current
        current = null
        job?.cancel()
        job = scope.launch {
            val incoming = wanted?.let { k ->
                runCatching { start(samplesFor(k)) }
                    .onFailure { Log.w(TAG, "Atmo fehlgeschlagen", it) }
                    .getOrNull()
            }
            synchronized(this@PlayAmbienceSound) {
                if (currentKind == wanted) current = incoming else incoming?.let(::release)
            }
            val outgoingFrom = outgoing?.let { synchronized(volumes) { volumes[it] } } ?: VOLUME
            for (step in 1..FADE_STEPS) {
                val t = step.toFloat() / FADE_STEPS
                incoming?.let { setVolume(it, VOLUME * t) }
                outgoing?.let { setVolume(it, outgoingFrom * (1f - t)) }
                delay(FADE_MS / FADE_STEPS)
            }
            outgoing?.let(::release)
        }.also { fade ->
            // Wird der Uebergang abgebrochen, darf der alte Klang nicht weiterlaufen.
            fade.invokeOnCompletion { cause -> if (cause != null) outgoing?.let(::release) }
        }
    }

    /** Sofort still - beim Verlassen des Spielmodus. */
    @Synchronized
    fun stop() {
        job?.cancel()
        job = null
        current?.let(::release)
        current = null
        currentKind = null
    }

    private fun setVolume(track: AudioTrack, value: Float) {
        synchronized(volumes) { volumes[track] = value }
        runCatching { track.setVolume(value * streamAttention) }
    }

    private fun samplesFor(kind: PlayAmbience.Kind): ShortArray =
        synchronized(cache) { cache[kind] } ?: PlayAmbience.render(kind).also { rendered ->
            synchronized(cache) { cache[kind] = rendered }
        }

    private fun start(samples: ShortArray): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    // Derselbe Kanal wie die Musik: Die Atmo gehoert zum Klangbild der Welt,
                    // nicht zu den Rueckmeldungen des Geraets.
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(PlayAmbience.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        try {
            track.write(samples, 0, samples.size)
            track.setLoopPoints(0, samples.size, -1)
            setVolume(track, 0f)
            track.play()
        } catch (e: Exception) {
            release(track)
            throw e
        }
        return track
    }

    private fun release(track: AudioTrack) {
        synchronized(volumes) { volumes.remove(track) }
        runCatching { track.stop() }
        runCatching { track.release() }
    }
}
