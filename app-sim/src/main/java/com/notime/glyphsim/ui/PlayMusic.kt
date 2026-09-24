package com.notime.glyphsim.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.MusicContext
import com.notime.glyphsim.matrix.MusicResolver
import com.notime.glyphsim.matrix.MusicRole
import com.notime.glyphsim.matrix.PlayMusicCue
import com.notime.glyphsim.matrix.PlayMusicLoop
import com.notime.glyphsim.matrix.PlayMusicRotation
import com.notime.glyphsim.matrix.PlayMusicTransition
import com.notime.glyphsim.settings.SettingsCatalog
import com.notime.glyphsim.settings.SettingsStore
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
 * ## Zwei Ebenen
 *
 * Die **Szene** traegt die Lage (Tageszeit, Ort, das eigene Stueck am Tagesanfang). Darueber
 * liegt fuer kurze Zeit der **Einspieler** eines Gasts ([startCue], Regeln in [PlayMusicCue]):
 * Die Szene taucht unter ihm ab, laeuft aber weiter und kehrt danach an ihrer laufenden Stelle
 * zurueck. Beide Ebenen haben je einen eigenen Player; der Einspieler findet nur ueber bereits
 * laufender Szenenmusik statt und erbt damit jede Sperre darunter.
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
 * Build brechen, in dem eine Rolle noch keinen Track hat - und genau das ist heute fuer drei
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

    /**
     * Die Ueberblendung in das eigene Stueck und aus ihm heraus - darauf ist
     * [com.notime.glyphsim.matrix.PlayCharacterTheme.GREETING_MS] gerechnet. Alle anderen
     * Wechsel waehlen ihre Dauer in [PlayMusicTransition.fadeMs].
     */
    internal const val CROSSFADE_MS = 4_000L

    private var player: MediaPlayer? = null
    private var outgoingPlayer: MediaPlayer? = null
    private var transition: ValueAnimator? = null
    private var playerVolume = 0f

    /** Welche Rolle gerade klingt - die Grundlage dafuer, sie NICHT neu zu starten. */
    private var playingRole: MusicRole? = null

    /** Welche Variante der laufenden Rolle gerade klingt - siehe [PlayMusicRotation]. */
    private var playingVariant: Int? = null

    /** Seit wann die aktuelle ROLLE laeuft. Ein Variantenwechsel setzt das bewusst NICHT zurueck. */
    private var roleStartedAtMs: Long = 0L

    /** Ein Rollenwechsel, der noch auf Bestaetigung wartet - siehe [PlayMusicTransition.settle]. */
    private var pendingRole: PlayMusicTransition.Pending? = null

    /** Die geplante Naht des laufenden Stuecks - siehe [PlayMusicLoop]. */
    private var seamTask: Runnable? = null
    private val seamHandler by lazy { Handler(Looper.getMainLooper()) }

    // --- Die zweite Ebene: der Einspieler eines Gasts (siehe [PlayMusicCue]) -------------------

    private var cuePlayer: MediaPlayer? = null
    private var cueAnimator: ValueAnimator? = null
    private var cueVolume = 0f

    /** Welcher Einspieler gerade klingt; [endCue] beendet nur genau diesen. */
    private var cueToken: Long? = null
    private var nextCueToken = 1L

    /**
     * Wie weit die Szenenmusik gerade unter einem Einspieler liegt: 1 = voll, 0 = ganz
     * abgetaucht. Sie laeuft dabei weiter - nach dem Einspieler kehrt sie an der Stelle zurueck,
     * an der sie ohnehin waere, statt von vorn zu beginnen.
     */
    private var baseDuck = 1f

    /** Ueberlebt [stop] absichtlich: Wer rein- und rausgeht, soll keinen Einspieler erzwingen. */
    private var cueMemory = PlayMusicCue.Memory()

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

    /** Die Ressourcen-Id einer bestimmten Variante, oder `null`, wenn es sie nicht gibt. */
    fun trackResId(context: Context, role: MusicRole, variant: Int = 1): Int? =
        context.resources
            .getIdentifier(role.variantResource(variant), "raw", context.packageName)
            .takeIf { it != 0 }

    /**
     * Welche Varianten dieser Rolle tatsaechlich im Paket liegen, aufsteigend.
     *
     * Ueber Namenssuche statt ueber eine gepflegte Liste, aus demselben Grund wie schon bei der
     * einzelnen Datei: Ein erzeugter Track kommt in einem eigenen Pull Request, und niemand soll
     * dabei eine zweite Stelle nachziehen muessen. Die Luecke ist beabsichtigt zugelassen -
     * fehlt die 02, wird die 03 trotzdem gefunden.
     */
    fun availableVariants(context: Context, role: MusicRole): List<Int> =
        (1..MusicRole.MAX_VARIANTS).filter { trackResId(context, role, it) != null }

    /**
     * Welche Rollen tatsaechlich ausgeliefert werden - jede, zu der es mindestens ein Stueck
     * gibt. Jeder gemergte Track erweitert die Menge, ohne dass hier oder im [MusicResolver]
     * etwas zu aendern waere.
     */
    fun availableRoles(context: Context): Set<MusicRole> =
        MusicRole.entries.filterTo(mutableSetOf()) { availableVariants(context, it).isNotEmpty() }

    // --- Die Zusammenfuehrung -------------------------------------------------------------------

    /**
     * Streicht [MusicRole.CHARACTER_THEME] wieder aus dem Angebot, wenn das Stueck GENAU DIESES
     * Wesens fehlt - rein, damit sich der Fall pruefen laesst, den es heute noch fuer fuenf von
     * sechs Wesen gibt.
     *
     * Ohne diesen Schritt genuegte eine einzige ausgelieferte Datei, damit die Rolle als
     * vorhanden gilt ([availableRoles] fragt nur, ob es IRGENDEINE Variante gibt) - und dann
     * bekaeme das Wyrmling zur Begruessung das Thema des Pufflings. Ein fremdes Stueck ist
     * schlechter als gar keines: Es behauptet etwas ueber ein Wesen, das nicht stimmt.
     *
     * Faellt die Rolle hier heraus, bleibt es nicht still - [MusicResolver.candidates] fuehrt
     * hinter dem Thema die gewoehnlichen Kandidaten, und der Tag klingt wie sonst.
     */
    internal fun rolesFor(
        available: Set<MusicRole>,
        musicContext: MusicContext,
        characterThemeVariants: List<Int>
    ): Set<MusicRole> {
        if (MusicRole.CHARACTER_THEME !in available) return available
        val eigenes = musicContext.characterTheme?.let(MusicRole.Companion::characterThemeVariant)
        if (eigenes != null && eigenes in characterThemeVariants) return available
        return available - MusicRole.CHARACTER_THEME
    }

    /**
     * Die Variante, die bei dieser Lage FEST steht - oder `null`, wenn die Rolle frei rotieren
     * darf.
     *
     * Der Unterschied ist der ganze Sinn der Rolle [MusicRole.CHARACTER_THEME]: Ueberall sonst
     * sind mehrere Varianten mehrere Stuecke derselben Stimmung, zwischen denen
     * [PlayMusicRotation] gegen Monotonie wechselt. Hier gehoert jede Variante einem Wesen. Ein
     * Wechsel nach fuenf Minuten waere kein frischer Track, sondern ein anderes Wesen.
     */
    internal fun fixedVariant(role: MusicRole, musicContext: MusicContext): Int? =
        if (role == MusicRole.CHARACTER_THEME) {
            musicContext.characterTheme?.let(MusicRole.Companion::characterThemeVariant)
        } else {
            null
        }

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
     *
     * Und selbst dann nicht sofort: Ein Rollenwechsel muss sich erst bestaetigen (siehe
     * [PlayMusicTransition.settle]). Der Rueckgabewert nennt, in wie vielen Millisekunden das
     * faellig ist, damit der Aufrufer genau dann wieder fragt; `null` heisst, es gibt keinen
     * eigenen Termin.
     */
    fun apply(context: Context, musicContext: MusicContext): Long? {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val wanted = decide(
            enabled = isEnabled(context),
            context = musicContext,
            available = rolesFor(
                available = availableRoles(context),
                musicContext = musicContext,
                characterThemeVariants = availableVariants(context, MusicRole.CHARACTER_THEME)
            ),
            // Der eigene Player zaehlt nicht als fremder Ton, sonst hielte sich die Musik
            // beim naechsten Abgleich selbst fuer eine Stoerung und schaltete sich ab.
            otherAudioActive = player == null && audio?.isMusicActive == true,
            deviceSilent = audio?.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                audio?.ringerMode == AudioManager.RINGER_MODE_VIBRATE
        )

        if (wanted == null) {
            stop()
            return null
        }
        val varianten = availableVariants(context, wanted)
        val fest = fixedVariant(wanted, musicContext)
        if (wanted == playingRole) {
            pendingRole = null
            // Eine feste Variante rotiert nicht (siehe [fixedVariant]). Sie wechselt nur, wenn
            // das Wesen wechselt - dann ist der laufende Track das Thema von jemand anderem.
            if (fest != null) {
                if (fest == playingVariant) return null
                switchTo(context, wanted, fest, rollenwechsel = false)
                return null
            }
            // **Dieselbe Lage, dieselbe Rolle - und trotzdem gelegentlich ein anderes Stueck.**
            // Gemeldet als "nach ungefaehr fuenf Minuten wirkt ein einzelner wiederholter Track
            // monoton". Die Entscheidung darueber faellt in [PlayMusicRotation] und ist dort
            // pruefbar; hier steht nur die Uhr und der Player.
            //
            // Der Zeitstempel gehoert der ROLLE, nicht der Variante: Sonst faenge die Uhr bei
            // jedem Wechsel neu an, und aus "spaetestens nach fuenf Minuten" wuerde "alle fuenf
            // Minuten wieder von vorn" - hoerbar als Metronom.
            //
            // Seit [PlayMusicLoop] faellt diese Entscheidung an der Naht des Stuecks, nicht hier
            // mitten im Takt. Dieser Weg bleibt nur fuer den Fall, dass keine Naht geplant werden
            // konnte (unbekannte Laenge).
            if (seamTask != null || varianten.size < 2) return null
            val gelaufen = System.currentTimeMillis() - roleStartedAtMs
            if (!PlayMusicRotation.rotationDue(gelaufen, varianten.size)) return null
            val naechste = PlayMusicRotation.pickVariant(varianten, playingVariant) ?: return null
            if (naechste == playingVariant) return null
            switchTo(context, wanted, naechste, rollenwechsel = false)
            return null
        }
        // **Erst bestaetigen, dann wechseln.** Ein Ortswechsel, der nur ein Durchgang ist,
        // soll die Musik nicht zweimal umwerfen - siehe [PlayMusicTransition]. Der Rueckgabewert
        // sagt dem Aufrufer, wann die Bestaetigung faellig ist, damit er nicht erst beim naechsten
        // gewoehnlichen Abgleich nachsieht.
        val settle = PlayMusicTransition.settle(
            playingRole, wanted, pendingRole, System.currentTimeMillis()
        )
        pendingRole = settle.pending
        if (!settle.switchNow) return settle.recheckInMs
        val start = fest ?: PlayMusicRotation.pickVariant(varianten, current = null) ?: return null
        switchTo(context, wanted, start, rollenwechsel = true)
        return null
    }

    /**
     * Wechselt auf eine andere Rolle und ueberblendet den bisherigen Score.
     *
     * Der zweite Track macht den Wechsel erstmals real: um 18 Uhr oder beim Heimkommen darf der
     * Tag nicht mitten im Takt abbrechen. Beide Player leben deshalb nur fuer die Dauer dieses
     * Uebergangs nebeneinander; ausserhalb davon bleibt es bei genau einem Decoder.
     */
    private fun switchTo(
        context: Context,
        role: MusicRole,
        variant: Int,
        rollenwechsel: Boolean,
        fadeOverrideMs: Long? = null
    ) {
        val res = trackResId(context, role, variant) ?: return
        val fadeMs = fadeOverrideMs
            ?: PlayMusicTransition.fadeMs(playingRole, role, variantOnly = !rollenwechsel)
        runCatching {
            val next = MediaPlayer.create(context, res)?.apply {
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
                setVolume(0f, 0f)
                start()
            } ?: return

            val previous = player
            val previousVolume = playerVolume
            transition?.removeAllListeners()
            transition?.cancel()
            outgoingPlayer?.takeIf { it !== previous }?.let(::releasePlayer)
            outgoingPlayer = previous
            player = next
            playerVolume = 0f
            playingRole = role
            playingVariant = variant
            // Nur beim ROLLENwechsel neu stellen - siehe die Begruendung in [apply].
            if (rollenwechsel) roleStartedAtMs = System.currentTimeMillis()
            scheduleSeam(context, role, variant, next)

            // **Jeder Start blendet ein - auch ohne Vorgaenger.** Gemeldet als "manchmal geht die
            // Musik fuer mehrere Sekunden aus, und dann kommt der neue Track" - das war kein
            // Aussetzer, sondern genau dieser Zweig: Ein Neustart nach vollstaendiger Stille
            // (Bildschirm verlassen und wiedergekommen, Spielmodus neu betreten) sprang bisher
            // OHNE Einblendung auf volle Lautstaerke. [transitionVolumes] liefert die neue
            // Lautstaerke unabhaengig von der alten (siehe deren Dokumentation) - derselbe
            // Einblend-Bogen wie bei einem Rollenwechsel passt deshalb auch hier, `previous`
            // bleibt einfach null und es gibt nichts auszublenden.
            val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = fadeMs
                addUpdateListener { valueAnimator ->
                    val (oldVolume, newVolume) =
                        transitionVolumes(valueAnimator.animatedValue as Float, previousVolume)
                    if (previous != null && outgoingPlayer === previous) {
                        setBaseVolume(previous, oldVolume)
                    }
                    if (player === next) {
                        playerVolume = newVolume
                        setBaseVolume(next, newVolume)
                    }
                }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (previous != null && outgoingPlayer === previous) {
                            outgoingPlayer = null
                            releasePlayer(previous)
                        }
                        if (player === next) {
                            playerVolume = VOLUME
                            setBaseVolume(next, VOLUME)
                        }
                        if (transition === animation) transition = null
                    }
                })
            }
            transition = animator
            animator.start()
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

    /**
     * Plant, wann das gerade gestartete Stueck [forPlayer] in seinen naechsten Durchlauf
     * uebergeht - vor seinem Ausklang, und gegebenenfalls in ein anderes Stueck derselben
     * Stimmung (siehe [PlayMusicLoop]).
     *
     * Die Schleife des Players selbst bleibt eingeschaltet: Kommt die Naht aus irgendeinem Grund
     * nicht, klingt es wie vorher - nie still.
     */
    private fun scheduleSeam(context: Context, role: MusicRole, variant: Int, forPlayer: MediaPlayer) {
        cancelSeam()
        val dauer = runCatching { forPlayer.duration.toLong() }.getOrDefault(-1L)
        val varianten = availableVariants(context, role)
        val gelaufenBeiNaht = System.currentTimeMillis() - roleStartedAtMs + dauer.coerceAtLeast(0L)
        val naht = PlayMusicLoop.plan(
            durationMs = dauer,
            current = variant,
            variants = varianten,
            fixedVariant = if (role == MusicRole.CHARACTER_THEME) variant else null,
            rotate = PlayMusicRotation.rotationDue(gelaufenBeiNaht, varianten.size),
            variantFadeMs = PlayMusicTransition.fadeMs(role, role, variantOnly = true),
            pickOther = { verfuegbar, jetzt -> PlayMusicRotation.pickVariant(verfuegbar, jetzt) }
        ) ?: return
        val appContext = context.applicationContext
        val task = Runnable {
            seamTask = null
            // Hat inzwischen ein anderer Wechsel stattgefunden, gehoert diese Naht niemandem mehr.
            if (player !== forPlayer || playingRole != role) return@Runnable
            switchTo(appContext, role, naht.nextVariant, rollenwechsel = false, fadeOverrideMs = naht.fadeMs)
        }
        seamTask = task
        seamHandler.postDelayed(task, naht.atMs)
    }

    private fun cancelSeam() {
        seamTask?.let { seamHandler.removeCallbacks(it) }
        seamTask = null
    }

    private fun release() {
        cancelSeam()
        val current = player
        val outgoing = outgoingPlayer
        transition?.removeAllListeners()
        transition?.cancel()
        transition = null
        player = null
        outgoingPlayer = null
        playerVolume = 0f
        playingRole = null
        playingVariant = null
        roleStartedAtMs = 0L
        pendingRole = null
        current?.let(::releasePlayer)
        outgoing?.takeIf { it !== current }?.let(::releasePlayer)
        releaseCue()
    }

    // --- Der Einspieler ---------------------------------------------------------------------

    /**
     * Laesst das Thema von [guest] kurz ueber der Szene klingen - der Auftritt eines Gasts.
     *
     * Ob das ueberhaupt passiert, entscheidet [PlayMusicCue.judge]; hier steht nur der Player.
     * Weil ein Einspieler **nur ueber bereits laufender Musik** stattfindet, braucht er keine
     * eigene Pruefung von Schalter, Stummschaltung oder fremdem Ton: Laeuft keine Musik, gibt es
     * keinen Einspieler.
     *
     * @return ein Kennzeichen fuer [endCue], oder `null`, wenn kein Einspieler beginnt.
     */
    fun startCue(context: Context, guest: AvatarSpecies, host: AvatarSpecies?): Long? {
        val now = System.currentTimeMillis()
        val verdict = PlayMusicCue.judge(
            guest = guest,
            host = host,
            playingRole = playingRole.takeIf { player != null },
            cueRunning = cueToken != null,
            themeVariants = availableVariants(context, MusicRole.CHARACTER_THEME),
            memory = cueMemory,
            nowMs = now
        )
        if (verdict != PlayMusicCue.Verdict.PLAY) return null
        val res = trackResId(
            context, MusicRole.CHARACTER_THEME, MusicRole.characterThemeVariant(guest)
        ) ?: return null
        val next = runCatching {
            MediaPlayer.create(context, res)?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setVolume(0f, 0f)
                start()
            }
        }.onFailure {
            Log.w(TAG, "Einspieler konnte nicht starten: $guest", it)
        }.getOrNull() ?: return null

        releaseCue()
        cueMemory = PlayMusicCue.remember(cueMemory, guest, now)
        val token = nextCueToken++
        cueToken = token
        cuePlayer = next
        animateCue(next, fromDuck = baseDuck, toDuck = 0f, toCue = VOLUME, durationMs = PlayMusicCue.FADE_IN_MS)
        return token
    }

    /**
     * Laesst den Einspieler [token] ausklingen und die Szene zurueckkehren. Harmlos, wenn er
     * schon vorbei ist oder die Musik inzwischen angehalten wurde.
     */
    fun endCue(token: Long) {
        if (cueToken != token) return
        val current = cuePlayer ?: return
        cueToken = null
        animateCue(current, fromDuck = baseDuck, toDuck = 1f, toCue = 0f, durationMs = PlayMusicCue.FADE_OUT_MS) {
            if (cuePlayer === current) {
                cuePlayer = null
                releasePlayer(current)
            }
        }
    }

    /**
     * Ein Bogen fuer beide Ebenen zugleich. Szene und Gast teilen sich dieselbe Kurve mit
     * konstanter wahrgenommener Energie (siehe [transitionVolumes]) - zwei Stuecke in fremden
     * Tonarten und Tempi duerfen sich nur in diesem kurzen Bogen ueberlagern, nie laenger.
     */
    private fun animateCue(
        cue: MediaPlayer,
        fromDuck: Float,
        toDuck: Float,
        toCue: Float,
        durationMs: Long,
        onEnd: () -> Unit = {}
    ) {
        cueAnimator?.removeAllListeners()
        cueAnimator?.cancel()
        val fromCue = cueVolume
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            addUpdateListener { valueAnimator ->
                val progress = valueAnimator.animatedValue as Float
                baseDuck = cueArc(fromDuck, toDuck, progress)
                cueVolume = cueArc(fromCue, toCue, progress)
                refreshBaseVolumes()
                if (cuePlayer === cue) runCatching { cue.setVolume(cueVolume, cueVolume) }
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    baseDuck = toDuck
                    cueVolume = toCue
                    refreshBaseVolumes()
                    if (cuePlayer === cue) runCatching { cue.setVolume(toCue, toCue) }
                    if (cueAnimator === animation) cueAnimator = null
                    onEnd()
                }
            })
        }
        cueAnimator = animator
        animator.start()
    }

    /**
     * Ein Wert auf dem Weg von [from] nach [to]: steigend auf dem Sinus-, fallend auf dem
     * Kosinus-Viertel - dieselbe Kurve wie [transitionVolumes], nur mit beliebigem Anfang, weil ein
     * Einspieler auch mitten in seinem Aufklingen wieder gehen kann.
     */
    internal fun cueArc(from: Float, to: Float, progress: Float): Float {
        val angle = progress.coerceIn(0f, 1f) * (PI / 2.0)
        return if (to >= from) {
            from + (to - from) * sin(angle).toFloat()
        } else {
            to + (from - to) * cos(angle).toFloat()
        }
    }

    private fun releaseCue() {
        val cue = cuePlayer
        cueAnimator?.removeAllListeners()
        cueAnimator?.cancel()
        cueAnimator = null
        cuePlayer = null
        cueToken = null
        cueVolume = 0f
        baseDuck = 1f
        cue?.let(::releasePlayer)
        refreshBaseVolumes()
    }

    /** Setzt die Lautstaerke eines Szenen-Players - immer unter Beruecksichtigung des Duckings. */
    private fun setBaseVolume(value: MediaPlayer, volume: Float) {
        val effective = volume * baseDuck
        runCatching { value.setVolume(effective, effective) }
    }

    /**
     * Nach einer Aenderung von [baseDuck] ausserhalb einer Ueberblendung. Laeuft gerade eine, setzt
     * sie beim naechsten Bild ohnehin beide Player neu.
     */
    private fun refreshBaseVolumes() {
        if (transition != null) return
        player?.let { setBaseVolume(it, playerVolume) }
    }

    private fun releasePlayer(value: MediaPlayer) {
        runCatching { if (value.isPlaying) value.stop() }
        runCatching { value.release() }
    }

    /** Konstante wahrgenommene Energie statt eines Lautstaerke-Lochs in der Mitte. */
    internal fun transitionVolumes(
        progress: Float,
        outgoingStart: Float = VOLUME
    ): Pair<Float, Float> {
        val angle = progress.coerceIn(0f, 1f) * (PI / 2.0)
        return (outgoingStart * cos(angle).toFloat()) to (VOLUME * sin(angle).toFloat())
    }
}
