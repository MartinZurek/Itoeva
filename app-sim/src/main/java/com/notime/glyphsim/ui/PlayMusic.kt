package com.notime.glyphsim.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import com.notime.glyphsim.settings.SettingsCatalog
import com.notime.glyphsim.settings.SettingsStore

/**
 * **Die Musik der Welt** - im Gegensatz zu [PlaySound] nicht die Stimme des Wesens, sondern der
 * Score darueber.
 *
 * ## Warum das hier ueberhaupt eine Datei abspielt
 *
 * [com.notime.glyphsim.matrix.PlayChime] begruendet ausfuehrlich, warum der Klang dieser App
 * **gerechnet und nicht abgespielt** wird - unter anderem, weil ein aufgenommener Klang neben
 * einer gerechneten Welt derselbe Bruch waere wie eine fotografierte Blume in einer
 * Pixel-Kulisse. Diese Datei ist die Ausnahme davon, und sie hat eine Grenze:
 *
 * **Alles, was die Welt oder das Wesen selbst von sich gibt, bleibt gerechnet. Nur der Score darf
 * eine Datei sein.** Eine Filmmusik war noch nie aus demselben Material wie das Buehnenbild; die
 * Stimme einer Figur schon. Wer hier spaeter Schritte, Tueren oder Wetter als Sample ergaenzen
 * will, verletzt die Regel - das gehoert zu [com.notime.glyphsim.matrix.PlayChime].
 *
 * Die Herkunft der Datei liegt bei der versionierten Musik-Pipeline (siehe `music/README.md`):
 * Prompt, Modell und Seed sind festgehalten, und erst der Merge eines erzeugten Pull Requests
 * macht einen Track zum Asset.
 *
 * ## Wann sie zu hoeren ist
 *
 * Dieselbe Zurueckhaltung wie bei [PlaySound], mit **einer** bewussten Abweichung:
 *
 * - **Standardmaessig aus.** Wer nichts eingeschaltet hat, hoert nichts.
 * - **Nicht ueber fremdem Ton.** Laeuft ein Podcast, bleibt es still - und wie bei [PlaySound]
 *   wird ausdruecklich **kein Audio-Focus** angefordert. Wer Focus greift, pausiert die
 *   Wiedergabe des Nutzers; ein Spielmodus, der den Podcast anhaelt, ist kaputt.
 * - **Nicht bei stumm gestelltem Geraet.** Streng genommen regelt der Klingelton-Schalter die
 *   Medienlautstaerke nicht mit. Uebernommen wird die Regel trotzdem, weil sie dem entspricht,
 *   was jemand erwartet, der sein Telefon stumm gestellt hat.
 * - **Nur solange der Spielmodus zu sehen ist.** Der Aufrufer haelt sie an, sobald der Bildschirm
 *   verschwindet (siehe `DockScreen`).
 *
 * **Die Abweichung: nachts wird nicht gesperrt.** [PlaySound] schweigt nachts, weil ein Ton dort
 * unaufgefordert aus einem dunklen Zimmer kommt. Musik hier kann das nicht: Sie laeuft nur,
 * solange jemand den eingeschalteten Bildschirm ansieht, und nur, wenn er sie eingeschaltet hat.
 * Ein Abendtrack, den man abends nicht hoeren darf, waere zudem sinnlos.
 *
 * ## Warum der Track zur Laufzeit gesucht wird
 *
 * [trackResId] schlaegt die Ressource ueber ihren Namen nach statt ueber `R.raw`. Der Grund ist
 * nicht Bequemlichkeit: Die Audiodatei kommt aus einem **eigenen, erzeugten Pull Request** und
 * ist damit kein fester Bestandteil des Quellbaums. Ein direkter `R.raw`-Verweis wuerde jeden
 * Build brechen, in dem noch kein Track gemergt wurde. So bleibt die Welt einfach still, bis es
 * etwas zu hoeren gibt - was auch der ehrlichere Zustand ist.
 */
object PlayMusic {

    private const val TAG = "PlayMusic"

    /**
     * Der Ressourcenname aus `music/manifest.json` (`android_resource`). Aendert sich dort etwas,
     * aendert es sich hier mit - deshalb steht der Name an genau einer Stelle.
     */
    private const val TRACK_RESOURCE = "itoeva_home_evening_01"

    /**
     * Hintergrundmusik unter einer 16x16-Figur soll zuruecktreten, nicht fuehren. Bewusst
     * niedriger als die Systemlautstaerke, damit der Nutzer nach oben regeln kann statt nach
     * unten regeln zu muessen.
     */
    private const val VOLUME = 0.35f

    private var player: MediaPlayer? = null

    fun isEnabled(context: Context): Boolean =
        SettingsStore.read(context, SettingsCatalog.MusicEnabled)

    fun setEnabled(context: Context, enabled: Boolean) {
        SettingsStore.write(context, SettingsCatalog.MusicEnabled, enabled)
    }

    /** Die Ressourcen-Id des gemergten Tracks, oder `null`, wenn noch keiner ausgeliefert wird. */
    fun trackResId(context: Context): Int? {
        val id = context.resources.getIdentifier(
            TRACK_RESOURCE, "raw", context.packageName
        )
        return id.takeIf { it != 0 }
    }

    /**
     * Die Entscheidung selbst - ohne Android, damit sie sich pruefen laesst.
     *
     * Ohne diese Trennung waere die Frage "spielt er gerade zu Recht nichts?" nur am Geraet zu
     * beantworten, und zwar nachts, mit laufendem Podcast, bei stumm gestelltem Telefon.
     */
    fun shouldPlay(
        enabled: Boolean,
        trackPresent: Boolean,
        otherAudioActive: Boolean,
        deviceSilent: Boolean
    ): Boolean =
        enabled && trackPresent && !otherAudioActive && !deviceSilent

    /** Warum gerade nichts laeuft, obwohl Musik eingeschaltet ist - `null`, wenn nichts im Weg steht. */
    enum class Reason { NO_TRACK, OTHER_AUDIO, DEVICE_SILENT }

    fun silentReason(context: Context): Reason? {
        if (!isEnabled(context)) return null
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        return when {
            trackResId(context) == null -> Reason.NO_TRACK
            // Der eigene Player zaehlt hier nicht mit: Gefragt wird nur, bevor etwas laeuft.
            player == null && audio?.isMusicActive == true -> Reason.OTHER_AUDIO
            audio?.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                audio?.ringerMode == AudioManager.RINGER_MODE_VIBRATE -> Reason.DEVICE_SILENT
            else -> null
        }
    }

    /**
     * Startet die Schleife, falls sie erlaubt ist und nicht schon laeuft.
     *
     * Mehrfaches Aufrufen ist harmlos - der Spielmodus darf das bei jedem Wiedereintritt tun,
     * ohne pruefen zu muessen, ob er es schon getan hat.
     */
    fun start(context: Context) {
        if (player != null) return
        val res = trackResId(context) ?: return
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val allowed = shouldPlay(
            enabled = isEnabled(context),
            trackPresent = true,
            otherAudioActive = audio?.isMusicActive == true,
            deviceSilent = audio?.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                audio?.ringerMode == AudioManager.RINGER_MODE_VIBRATE
        )
        if (!allowed) return

        runCatching {
            MediaPlayer.create(context, res)?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        // MEDIA/MUSIC statt SONIFICATION wie bei PlaySound: Das hier ist keine
                        // Rueckmeldung auf eine Handlung, sondern laufende Musik - sie gehoert
                        // an den Medienregler, den der Nutzer dafuer benutzt.
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                setVolume(VOLUME, VOLUME)
                start()
                player = this
            }
        }.onFailure {
            Log.w(TAG, "Musik konnte nicht starten", it)
            release()
        }
    }

    /** Haelt an und gibt den Player frei. Mehrfaches Aufrufen ist harmlos. */
    fun stop() = release()

    private fun release() {
        val current = player ?: return
        player = null
        runCatching {
            if (current.isPlaying) current.stop()
        }
        runCatching { current.release() }
    }
}
