package com.notime.glyphsim.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.notime.glyphsim.matrix.PlayAmbientActivity
import com.notime.glyphsim.matrix.PlayChime
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.settings.SettingsCatalog
import com.notime.glyphsim.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * **Wann das Wesen ueberhaupt zu hoeren ist** - und das ist die eigentliche Arbeit, nicht der
 * Klang selbst ([PlayChime]).
 *
 * Eine App, die auf einem Nachttisch steht, hat beim Ton mehr zu verlieren als zu gewinnen.
 * Deshalb steht vor jedem einzelnen Ton eine Reihe von Bedingungen, und jede einzelne davon hat
 * einen konkreten Fall dahinter, in dem ein Ton falsch waere:
 *
 * - **Standardmaessig aus.** Wer nichts eingeschaltet hat, hoert nichts. Eine App, die nach einem
 *   Update ungefragt Geraeusche macht, hat ihr Vertrauen verspielt, bevor der Ton zu Ende ist.
 * - **Nachts nie** (siehe [PlayAmbientActivity.DayPhase.NIGHT]). Dieselbe Regel wie beim Satz
 *   ueber dem Kopf, und hier noch dringender.
 * - **Nicht ueber fremden Ton.** Laeuft gerade Musik oder ein Podcast, bleibt es still. Bewusst
 *   OHNE Audio-Focus anzufordern: Wer Focus greift, pausiert die Wiedergabe des Nutzers - eine
 *   Uhr, die den Podcast anhaelt, ist kaputt, und zwar unabhaengig davon, wie huebsch der Ton war.
 * - **Und wenn das Geraet stumm gestellt ist**, ebenfalls nicht: Der Klang laeuft ueber den
 *   System-Kanal und richtet sich damit nach dem Schalter, den der Nutzer ohnehin schon kennt.
 *
 * Das Erzeugen und Abspielen laeuft im Hintergrund: Ein paar tausend Abtastwerte zu rechnen ist
 * billig, aber nichts davon gehoert in den Zeichenpfad einer Uhr.
 */
object PlaySound {

    private const val TAG = "PlaySound"

    fun isEnabled(context: Context): Boolean =
        SettingsStore.read(context, SettingsCatalog.SoundEnabled)

    fun setEnabled(context: Context, enabled: Boolean) {
        SettingsStore.write(context, SettingsCatalog.SoundEnabled, enabled)
    }

    /**
     * Die Entscheidung selbst - ohne Android, damit sie sich pruefen laesst.
     *
     * Genau hier liegt der Unterschied zwischen einer Rueckmeldung und einer Stoerung, und genau
     * so etwas laesst sich am Geraet kaum ausprobieren: Man muesste nachts, mit laufendem
     * Podcast, bei stumm gestelltem Telefon eine Erinnerung beantworten.
     */
    fun shouldSound(
        enabled: Boolean,
        phase: PlayAmbientActivity.DayPhase,
        otherAudioActive: Boolean,
        deviceSilent: Boolean
    ): Boolean =
        enabled &&
            phase != PlayAmbientActivity.DayPhase.NIGHT &&
            !otherAudioActive &&
            !deviceSilent

    /**
     * **Warum er gerade nichts sagt, obwohl Ton eingeschaltet ist** - `null`, wenn nichts im Weg
     * steht.
     *
     * **Der Fall, den die Bedingungen selbst erzeugen.** Wer im Gespraech auf "lass dich hoeren"
     * tippt, waehrend es Nacht ist oder Musik laeuft, hoert nichts - und schliesst daraus, dass
     * der Schalter kaputt ist. Genau dieser Schluss ist schlimmer als die Stille: Er trifft
     * ausgerechnet den, der den Ton haben WOLLTE. Ein Halbsatz beseitigt ihn.
     */
    enum class Reason { NIGHT, OTHER_AUDIO, DEVICE_SILENT }

    fun silentReason(context: Context): Reason? {
        if (!isEnabled(context)) return null
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return when {
            PlayAmbientActivity.currentDayPhase() == PlayAmbientActivity.DayPhase.NIGHT -> Reason.NIGHT
            audio?.isMusicActive == true -> Reason.OTHER_AUDIO
            audio?.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                audio?.ringerMode == AudioManager.RINGER_MODE_VIBRATE -> Reason.DEVICE_SILENT
            else -> null
        }
    }

    /**
     * Spielt das Motiv der Kreatur zu diesem Ereignis - oder eben nicht.
     *
     * [scope] kommt von aussen, damit der Ton mit dem Bildschirm verschwindet, der ihn ausgeloest
     * hat: Wer den Dock-Modus verlaesst, waehrend gerade etwas klingt, soll den Rest nicht noch
     * hoeren.
     */
    fun play(
        context: Context,
        species: AvatarSpecies,
        event: PlayChime.Event,
        scope: CoroutineScope
    ) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val allowed = shouldSound(
            enabled = isEnabled(context),
            phase = PlayAmbientActivity.currentDayPhase(),
            otherAudioActive = audio?.isMusicActive == true,
            deviceSilent = audio?.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                audio?.ringerMode == AudioManager.RINGER_MODE_VIBRATE
        )
        if (!allowed) return

        scope.launch(Dispatchers.Default) {
            runCatching { emit(PlayChime.render(PlayChime.motifFor(species, event))) }
                .onFailure { Log.w(TAG, "Ton fehlgeschlagen", it) }
        }
    }

    /**
     * Gibt fertige Abtastwerte aus.
     *
     * `MODE_STATIC` und nicht `MODE_STREAM`: Das ganze Motiv ist ein paar zehntel Sekunden lang
     * und liegt schon vollstaendig vor - es einmal in den Puffer zu legen und abzuspielen ist
     * einfacher und braucht keinen mitlaufenden Schreiber.
     */
    private fun emit(samples: ShortArray) {
        if (samples.isEmpty()) return
        val bytes = samples.size * 2
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    // SONIFICATION heisst: eine Rueckmeldung auf eine Handlung des Nutzers. Genau
                    // das ist es - und der Kanal, der sich nach dem Stummschalter richtet.
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(PlayChime.SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        try {
            track.write(samples, 0, samples.size)
            track.setNotificationMarkerPosition(samples.size)
            // Freigeben, sobald der letzte Abtastwert durch ist - ein AudioTrack je Ereignis, der
            // sich selbst aufraeumt. Ohne diesen Rueckruf blieben sie liegen, bis der Prozess
            // endet, und ein Geraet hat nur eine begrenzte Zahl davon.
            track.setPlaybackPositionUpdateListener(
                object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(t: AudioTrack?) {
                        runCatching { t?.release() }
                    }

                    override fun onPeriodicNotification(t: AudioTrack?) = Unit
                }
            )
            track.play()
        } catch (e: Exception) {
            runCatching { track.release() }
            Log.w(TAG, "Wiedergabe fehlgeschlagen", e)
        }
    }
}
