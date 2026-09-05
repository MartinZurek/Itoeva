package com.notime.glyphsim.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import com.notime.glyphsim.matrix.MusicContext
import com.notime.glyphsim.matrix.MusicResolver
import com.notime.glyphsim.matrix.MusicRole
import com.notime.glyphsim.settings.SettingsCatalog
import com.notime.glyphsim.settings.SettingsStore

/**
 * **Die Musik der Welt** - im Gegensatz zu [PlaySound] nicht die Stimme des Wesens, sondern der
 * Score darueber.
 *
 * ## Die eine Trennung, die diese Datei traegt
 *
 * > Die Welt entscheidet, WAS passen wuerde. Der Nutzer entscheidet, OB ueberhaupt Musik laufen
 * > darf.
 *
 * Das WAS liegt vollstaendig in [MusicResolver] - ohne Android, ohne Einstellungen, pruefbar.
 * Das OB liegt hier, und es ist eine harte Sperre: Ist [isEnabled] falsch, wird der Resolver
 * gar nicht erst gefragt. **Kein Szenenwechsel und keine Aufloesung darf Musik eigenmaechtig
 * einschalten** - der Resolver kennt die Einstellung nicht einmal und koennte es deshalb nicht.
 *
 * ## Was der Schalter bedeutet
 *
 * "Musik grundsaetzlich verwenden" - **nicht** "diesen einen Track jetzt abspielen". Die
 * Einstellung liegt in [SettingsCatalog.MusicEnabled] und damit in SharedPreferences; sie
 * ueberlebt App-Start, Moduswechsel und Szenenwechsel. [stop] haelt nur die Wiedergabe an und
 * fasst die Einstellung **nie** an - sonst waere ein Verlassen des Spielmodus stillschweigend
 * ein Ausschalten, und der Nutzer haette einen Schalter, der sich selbst umlegt.
 *
 * ## Warum gibt es hier ueberhaupt eine Datei zu hoeren
 *
 * [com.notime.glyphsim.matrix.PlayChime] begruendet, warum der Klang dieser App gerechnet und
 * nicht abgespielt wird. Diese Datei ist die benannte Ausnahme davon, mit einer Grenze:
 * **Alles, was die Welt oder das Wesen selbst von sich gibt, bleibt gerechnet. Nur der Score
 * darf eine Datei sein.** Wer hier spaeter Schritte, Tueren oder Wetter als Sample ergaenzt,
 * verletzt sie.
 *
 * ## Wann sie zu hoeren ist
 *
 * Dieselbe Zurueckhaltung wie bei [PlaySound], mit **einer** bewussten Abweichung:
 *
 * - **Beim allerersten Start aus.** Wer nichts eingeschaltet hat, hoert nichts. Danach gilt
 *   ausschliesslich die zuletzt gespeicherte Entscheidung des Nutzers.
 * - **Nicht ueber fremdem Ton**, und ausdruecklich **ohne Audio-Focus** anzufordern. Wer Focus
 *   greift, pausiert die Wiedergabe des Nutzers; ein Spielmodus, der den Podcast anhaelt, ist
 *   kaputt.
 * - **Nicht bei stumm gestelltem Geraet.**
 * - **Nur solange der Spielmodus zu sehen ist** - der Aufrufer haelt an (siehe `DockScreen`).
 *
 * **Die Abweichung: nachts wird nicht gesperrt.** [PlaySound] schweigt nachts, weil ein Ton dort
 * unaufgefordert aus einem dunklen Zimmer kommt. Musik kann das nicht: Sie laeuft nur, solange
 * jemand den eingeschalteten Bildschirm ansieht und sie eingeschaltet hat.
 *
 * ## Warum die Tracks zur Laufzeit gesucht werden
 *
 * [availableRoles] schlaegt jede Rolle ueber ihren Ressourcennamen nach statt ueber `R.raw`.
 * Der Grund ist nicht Bequemlichkeit: Audiodateien kommen aus **eigenen, erzeugten Pull
 * Requests** und sind kein fester Bestandteil des Quellbaums. Ein direkter Verweis wuerde jeden
 * Build brechen, in dem eine Rolle noch keinen Track hat - und genau das ist heute fuer vier
 * von fuenf Rollen der Fall. So bleibt die Welt einfach still, bis es etwas zu hoeren gibt.
 *
 * Damit der Ressourcen-Schrumpfer die Dateien im Release nicht als unbenutzt entfernt, haelt
 * `app-sim/src/main/res/raw/keep.xml` sie ausdruecklich fest.
 */
object PlayMusic {

    private const val TAG = "PlayMusic"

    /**
     * Hintergrundmusik unter einer 16x16-Figur soll zuruecktreten, nicht fuehren. Bewusst
     * niedriger als die Systemlautstaerke, damit der Nutzer nach oben regeln kann statt nach
     * unten regeln zu muessen.
     */
    private const val VOLUME = 0.35f

    private var player: MediaPlayer? = null

    /** Welche Rolle gerade klingt - die Grundlage dafuer, sie NICHT neu zu starten. */
    private var playingRole: MusicRole? = null

    // --- Das OB: die Entscheidung des Nutzers ---------------------------------------------------

    fun isEnabled(context: Context): Boolean =
        SettingsStore.read(context, SettingsCatalog.MusicEnabled)

    /**
     * Die einzige Stelle, die die Einstellung schreibt - und sie wird ausschliesslich vom
     * Schalter in den Einstellungen aufgerufen. Weltlogik ruft das nie.
     */
    fun setEnabled(context: Context, enabled: Boolean) {
        SettingsStore.write(context, SettingsCatalog.MusicEnabled, enabled)
    }

    // --- Das WAS: welche Tracks es ueberhaupt gibt -----------------------------------------------

    /** Die Ressourcen-Id eines Tracks, oder `null`, wenn diese Rolle noch keinen hat. */
    fun trackResId(context: Context, role: MusicRole): Int? =
        context.resources
            .getIdentifier(role.androidResource, "raw", context.packageName)
            .takeIf { it != 0 }

    /**
     * Welche Rollen tatsaechlich ausgeliefert werden. Heute genau eine; jeder gemergte Track
     * erweitert die Menge, ohne dass hier oder im [MusicResolver] etwas zu aendern waere.
     */
    fun availableRoles(context: Context): Set<MusicRole> =
        MusicRole.entries.filterTo(mutableSetOf()) { trackResId(context, it) != null }

    // --- Die Zusammenfuehrung -------------------------------------------------------------------

    /**
     * Die vollstaendige Entscheidung, ohne Android - damit sie sich pruefen laesst.
     *
     * Die Reihenfolge ist der Punkt: **[enabled] steht vorn.** Ist es falsch, kommt der Resolver
     * nicht vor - dann gibt es kein "aber die Szene passt doch so gut".
     */
    fun decide(
        enabled: Boolean,
        context: MusicContext,
        available: Set<MusicRole>,
        otherAudioActive: Boolean,
        deviceSilent: Boolean
    ): MusicRole? {
        if (!enabled || otherAudioActive || deviceSilent) return null
        return MusicResolver.resolve(context, available)
    }

    /**
     * Bringt die Wiedergabe mit der Lage in Einklang - **die Stelle, die der Aufrufer wiederholt
     * aufruft.**
     *
     * Mehrfaches Aufrufen mit derselben Lage ist ausdruecklich harmlos und der Normalfall: Loest
     * es zur selben Rolle auf wie zuletzt, passiert **nichts** - der Track laeuft weiter, ohne
     * neu zu beginnen. Genau das verhindert, dass kurzfristige Ortswechsel des Avatars die Musik
     * zerhacken. Ein Wechsel findet nur statt, wenn sich die aufgeloeste ROLLE aendert, nicht
     * wenn sich die Welt aendert.
     */
    fun apply(context: Context, musicContext: MusicContext) {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val wanted = decide(
            enabled = isEnabled(context),
            context = musicContext,
            available = availableRoles(context),
            // Der eigene Player zaehlt nicht als fremder Ton, sonst hielte sich die Musik
            // beim naechsten Abgleich selbst fuer eine Stoerung und schaltete sich ab.
            otherAudioActive = player == null && audio?.isMusicActive == true,
            deviceSilent = audio?.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                audio?.ringerMode == AudioManager.RINGER_MODE_VIBRATE
        )

        if (wanted == playingRole) return
        if (wanted == null) {
            stop()
            return
        }
        switchTo(context, wanted)
    }

    /**
     * Wechselt auf eine andere Rolle.
     *
     * **Hier gehoert spaeter die Ueberblendung hin, und bewusst nur hier.** Heute ist es ein
     * harter Schnitt: anhalten, neu anfangen. Solange es genau einen Track gibt, kann dieser
     * Fall gar nicht eintreten - ein Crossfade waere also Architektur fuer ein Verhalten, das
     * sich noch nie gezeigt hat. Sobald der zweite Track existiert, ist diese eine Methode die
     * einzige Stelle, die dafuer aufgemacht werden muss (siehe NT-055).
     */
    private fun switchTo(context: Context, role: MusicRole) {
        val res = trackResId(context, role) ?: return
        release()
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
                playingRole = role
            }
        }.onFailure {
            Log.w(TAG, "Musik konnte nicht starten: ${role.manifestName}", it)
            release()
        }
    }

    /**
     * Haelt die Wiedergabe an. Mehrfaches Aufrufen ist harmlos.
     *
     * **Fasst die Einstellung des Nutzers nicht an** - siehe Klassendoku. Ein Verlassen des
     * Spielmodus ist kein Ausschalten.
     */
    fun stop() = release()

    private fun release() {
        val current = player ?: run { playingRole = null; return }
        player = null
        playingRole = null
        runCatching { if (current.isPlaying) current.stop() }
        runCatching { current.release() }
    }
}
