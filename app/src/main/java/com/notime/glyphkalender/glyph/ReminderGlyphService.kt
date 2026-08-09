package com.notime.glyphkalender.glyph

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixManager
import com.notime.glyphkalender.R
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphkalender.data.AppDatabase
import com.notime.glyphcore.data.LibraryAnimationRepository
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.roundToLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Kurzlebiger Foreground-Service (Typ "shortService", max. 3 Minuten Laufzeit),
 * der unabhaengig vom aktuell aktiven Glyph Toy Erinnerungsanimationen abspielt und
 * sich danach selbst beendet.
 *
 * Mehrere Erinnerungen koennen gleichzeitig oder sehr kurz hintereinander feuern
 * (z.B. mehrere mit demselben 5-Minuten-Intervall). Da die Glyph Matrix nur eine
 * Animation gleichzeitig zeigen kann, landet jede eingehende Erinnerung in [queue]
 * und wird der Reihe nach abgespielt, statt eine noch laufende Animation abzubrechen
 * (das wuerde bei einer Kollision dazu fuehren, dass immer nur die zuletzt
 * eingetroffene Erinnerung tatsaechlich zu sehen ist). Direkt aufeinanderfolgende
 * Erinnerungen mit demselben Animationstyp werden dabei zusammengefasst (siehe
 * [drainQueue]) - dieselbe Animation mehrfach identisch hintereinander abzuspielen
 * braechte keinen zusaetzlichen Informationsgehalt.
 *
 * Die Warteschlange holt NICHT bedingungslos alles nach: kommt ein Eintrag erst dran,
 * wenn sein eigenes Intervall (z.B. "alle 5 Minuten") laengst wieder verstrichen ist,
 * ist er veraltet und wird ohne Anzeige verworfen statt verspaetet nachgezeigt zu werden
 * (siehe [isExpired]). Das eingehaltene Intervall ist der Zweck der Erinnerung, nicht die
 * Animation selbst - eine verpasste Animation soll deshalb ausfallen statt sich
 * aufzustauen und alle nachfolgenden Erinnerungen ebenfalls zu verspaeten.
 *
 * Vorzeitiges Beenden: Zwei direktere Ansaetze (Naeherungssensor-Geste, Antippen der
 * Glyph-Matrix ueber den Toy-Touch-Mechanismus) wurden versucht und wieder verworfen -
 * ersterer war unzuverlaessig (Sensor-Rohwerte je nach Handy-Lage), zweiterer kam beim
 * Nutzer nie an (vermutlich weil die Matrix waehrend dieser Service laeuft nicht mehr als
 * "Toy-UI" gilt und der System-Touch-Handler deshalb nicht ausgeloest wird). Stattdessen
 * jetzt ein "Beenden"-Button in der ohnehin noetigen Foreground-Notification - normale,
 * gut dokumentierte Android-API statt unklarem Geraete-/SDK-Verhalten. "Beenden" bricht
 * die aktuell laufende Animation ab UND verwirft die restliche Warteschlange.
 *
 * Achtung: GlyphMatrixManager.getInstance() ist ein Prozess-Singleton. Laeuft
 * gleichzeitig ClockGlyphToyService als aktiver Toy, ist unklar (nicht in der
 * SDK-Doku spezifiziert), ob sich beide Verbindungen gegenseitig stoeren -
 * das noch auf dem echten Geraet verifizieren.
 */
class ReminderGlyphService : Service() {

    private data class QueueItem(
        val title: String,
        val animationType: AnimationType,
        val libraryAnimationId: Long? = null,
        val intervalMinutes: Int,
        val enqueuedAtElapsedRealtime: Long
    )

    // ConcurrentLinkedQueue statt einer normalen Liste: onStartCommand() haengt auf dem
    // Main-Thread neue Erinnerungen an, waehrend drainQueue() auf Dispatchers.Default
    // parallel dazu Eintraege entfernt - ohne threadsichere Struktur waere das eine Race.
    private val queue = ConcurrentLinkedQueue<QueueItem>()
    private var playerJob: Job? = null
    private var glyphMatrixManager: GlyphMatrixManager? = null

    @Volatile
    private var dismissRequested: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISMISS) {
            Log.d(TAG, "\"Beenden\" angetippt - verwerfe Warteschlange (${queue.size})")
            dismissRequested = true
            queue.clear()
            // Ein Beenden-Wunsch kann diesen Dienst auch ERST STARTEN: sowohl der PendingIntent
            // der Benachrichtigung als auch ReminderPlayback.dismiss() richten sich an den Dienst,
            // und Android startet ihn dafuer notfalls neu. Das passiert im Rennen zwischen "letzter
            // Frame gespielt" und "Nutzer tippt Stop". Ohne das Folgende bliebe eine solche Instanz
            // ohne laufenden Auftrag stehen, bis das System sie irgendwann einsammelt.
            if (playerJob?.isActive != true) stopSelf()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_TITLE).orEmpty()
        val type = intent?.getStringExtra(EXTRA_ANIMATION_TYPE)
            ?.let { runCatching { AnimationType.valueOf(it) }.getOrNull() }
            ?: AnimationType.GENERAL
        val libraryAnimationId = intent?.getLongExtra(EXTRA_LIBRARY_ANIMATION_ID, -1L)?.takeIf { it != -1L }
        val intervalMinutes = intent?.getIntExtra(EXTRA_INTERVAL_MINUTES, 1)?.coerceAtLeast(1) ?: 1
        queue.add(QueueItem(title, type, libraryAnimationId, intervalMinutes, SystemClock.elapsedRealtime()))

        val alreadyPlaying = playerJob?.isActive == true
        Log.d(
            TAG,
            "onStartCommand: title=\"$title\" type=$type eingereiht " +
                "(Warteschlange=${queue.size}, laeuft bereits=$alreadyPlaying)"
        )

        if (!alreadyPlaying) {
            ServiceCompat.startForeground(
                this,
                NOTIF_ID,
                buildNotification(title, 0),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
            )
            val runningScope = CoroutineScope(Dispatchers.Default)
            playerJob = runningScope.launch { drainQueue() }
        }
        // Laeuft bereits eine Animation, reicht das Einreihen oben - drainQueue() holt den
        // neuen Eintrag automatisch ab, sobald der aktuelle Frame-Loop fertig ist.

        return START_NOT_STICKY
    }

    /**
     * Eine Erinnerung soll nie "nachgeholt" werden - kommt sie erst dran, wenn ihr eigenes
     * Intervall laengst wieder abgelaufen ist (z.B. weil sich vor ihr andere Erinnerungen
     * gestaut haben), ist die Anzeige ohnehin veraltet und wird ersatzlos uebersprungen statt
     * verspaetet gezeigt zu werden - siehe Klassenkommentar zum Warteschlangen-Verhalten.
     */
    private fun isExpired(item: QueueItem): Boolean {
        val ageMs = SystemClock.elapsedRealtime() - item.enqueuedAtElapsedRealtime
        return ageMs >= item.intervalMinutes * 60_000L
    }

    /**
     * Verbindet sich mit der Glyph Matrix (Prozess-Singleton, siehe Klassenkommentar) und
     * gibt den verbundenen Manager zurueck, oder null, falls das Geraet keine Glyph Matrix
     * hat.
     */
    private suspend fun connectGlyphMatrixManager(): GlyphMatrixManager? {
        val gmm = GlyphMatrixManager.getInstance(applicationContext) ?: return null
        glyphMatrixManager = gmm

        if (GlyphMatrixConnection.isConnected()) {
            // Verbindung besteht bereits (z.B. von einem vorherigen Testlauf oder von
            // ClockGlyphToyService). Ein erneuter init()-Aufruf wuerde bindService() auf
            // dieselbe, schon verbundene ServiceConnection ausloesen - Android ruft
            // onServiceConnected dann aber nicht nochmal auf. Direkt weiterspielen statt
            // auf den Callback zu warten.
            gmm.register(Glyph.DEVICE_25111p)
            return gmm
        }

        return suspendCancellableCoroutine { cont ->
            gmm.init(object : GlyphMatrixManager.Callback {
                override fun onServiceConnected(componentName: ComponentName?) {
                    GlyphMatrixConnection.markConnected()
                    gmm.register(Glyph.DEVICE_25111p)
                    if (cont.isActive) cont.resumeWith(Result.success(gmm))
                }

                override fun onServiceDisconnected(componentName: ComponentName?) {
                    GlyphMatrixConnection.markDisconnected()
                }
            })
        }
    }

    /** Spielt die Warteschlange ab, bis sie leer ist, und beendet danach den Service. */
    private suspend fun drainQueue() {
        val gmm = connectGlyphMatrixManager()
        if (gmm == null) {
            Log.w(TAG, "GlyphMatrixManager.getInstance() lieferte null - keine Animation moeglich, breche ab")
            stopSelf()
            return
        }

        // Solange gesetzt, laesst ClockGlyphToyServices Sekundentakt die Matrix in Ruhe
        // (siehe GlyphMatrixConnection) - sonst wuerde der naechste Uhr-Tick jeden
        // Animationsframe wieder ueberschreiben.
        GlyphMatrixConnection.markAnimationPlaying()
        try {
            while (true) {
                val first = queue.poll() ?: break
                if (isExpired(first)) {
                    Log.d(
                        TAG,
                        "\"${first.title}\" verfallen (Intervall=${first.intervalMinutes}min laengst um) - " +
                            "wird nicht mehr gezeigt"
                    )
                    continue
                }
                val mergedTitles = mutableListOf(first.title)
                // Direkt nachfolgende Erinnerungen mit derselben Animation (Typ UND ggf.
                // Bibliotheks-Id) sind visuell identisch - zusammenfassen statt dieselbe
                // Animation mehrfach hintereinander abzuspielen.
                while (queue.peek()?.animationType == first.animationType &&
                    queue.peek()?.libraryAnimationId == first.libraryAnimationId
                ) {
                    mergedTitles += queue.poll()!!.title
                }

                dismissRequested = false
                val shownTitle = mergedTitles.joinToString(" · ")
                updateNotification(shownTitle, queue.size)
                // Zweiter, von Benachrichtigungen unabhaengiger Weg zum Beenden - noetig,
                // sobald POST_NOTIFICATIONS abgelehnt ist und die Benachrichtigung samt ihrem
                // "Beenden"-Button gar nicht erst erscheint. Siehe ReminderPlayback.
                ReminderPlayback.started(shownTitle)
                runCatchingPlayAnimation(gmm, first)
            }
        } finally {
            GlyphMatrixConnection.markAnimationStopped()
            // Ins finally, damit der Hinweis in der App nicht stehen bleibt, wenn das Abspielen
            // mit einem Fehler abbricht - er boete sonst ein Beenden fuer laengst Vorbeigegangenes an.
            ReminderPlayback.finished()
        }

        Log.d(TAG, "Warteschlange leer, beende Service")
        stopSelf()
    }

    /**
     * Faengt unerwartete Fehler in playAnimation() ab (z.B. SDK-Eigenheiten, die sich
     * nicht auf jedem Geraet vorab testen lassen): ohne das wuerde ein einzelner Bug hier
     * die ganze Coroutine unbehandelt abstuerzen lassen und die Matrix mitten in der
     * Animation haengen bleiben, statt wenigstens zur Uhrzeit zurueckzuspringen.
     */
    private suspend fun runCatchingPlayAnimation(gmm: GlyphMatrixManager, item: QueueItem) {
        try {
            playAnimation(gmm, item)
        } catch (e: Exception) {
            Log.e(TAG, "Erinnerungsanimation fehlgeschlagen, springe direkt zur Uhrzeit", e)
            runCatching { ClockFrame.draw(applicationContext, gmm) }
        }
    }

    /** Bibliotheks-Animation hat Vorrang vor [QueueItem.animationType], falls gesetzt. */
    private suspend fun resolveFrames(item: QueueItem): List<IntArray> {
        val libraryId = item.libraryAnimationId
        if (libraryId != null) {
            val repository = LibraryAnimationRepository(AppDatabase.getInstance(applicationContext).libraryAnimationDao())
            val frames = repository.framesFor(libraryId)
            if (frames != null) return frames
            Log.w(TAG, "Bibliotheks-Animation id=$libraryId nicht mehr vorhanden, falle auf ${item.animationType} zurueck")
        }
        return ReminderAnimations.framesFor(item.animationType)
    }

    private suspend fun playAnimation(gmm: GlyphMatrixManager, item: QueueItem) {
        Log.d(TAG, "playAnimation start: type=${item.animationType} libraryAnimationId=${item.libraryAnimationId}")
        val frames = resolveFrames(item)
        if (frames.isEmpty()) return

        // Zeitbasiert statt einer reinen Zaehlschleife: der zu zeigende Frame wird aus
        // der tatsaechlich vergangenen Realzeit berechnet (SystemClock.elapsedRealtime,
        // laeuft unabhaengig von Systemzeit-Aenderungen weiter), nicht aus der Anzahl
        // bereits gemachter delay()-Aufrufe. Geraet der Thread einmal kurz ins Stocken
        // (GC-Pause, CPU-Drosselung im Doze-Modus, o.ae.), "haengt" das Bild dadurch
        // nicht auf einem alten Frame fest, bis die Schleife wieder aufgeholt hat -
        // stattdessen zeigt der naechste Tick sofort den fuer die aktuelle Zeit
        // richtigen Frame. `lastFrameIndex` verhindert nur redundante setMatrixFrame-
        // Aufrufe, wenn sich zwischen zwei Ticks der Frame-Index nicht geaendert hat.
        //
        // `totalDurationMs` wird VOR dem Start auf ein ganzzahliges Vielfaches der
        // tatsaechlichen Durchlauflaenge (frames.size * FRAME_DELAY_MS) gerundet, statt
        // einfach ANIMATION_DURATION_MS direkt als Abbruchgrenze zu nehmen - sonst konnte
        // die Schleife jederzeit mitten in einem Durchlauf abbrechen (bei Animationen mit
        // vielen Frames wurde so nicht mal ein einziger vollstaendiger Durchlauf gezeigt,
        // z.B. 36 Frames * 260ms = 9360ms > 8000ms Zielspieldauer). Gerundet statt immer
        // aufgerundet, damit eine Animation, die nicht sauber in ANIMATION_DURATION_MS
        // aufgeht, im Zweifel lieber etwas kuerzer als spuerbar laenger laeuft. Jeder
        // begonnene Durchlauf wird garantiert zu Ende gespielt, mindestens einmal.
        val loopDurationMs = frames.size * FRAME_DELAY_MS
        val loopsToPlay = (ANIMATION_DURATION_MS.toDouble() / loopDurationMs).roundToLong().coerceAtLeast(1)
        val totalDurationMs = loopsToPlay * loopDurationMs
        val startElapsed = SystemClock.elapsedRealtime()
        var lastFrameIndex = -1
        while (true) {
            val elapsed = SystemClock.elapsedRealtime() - startElapsed
            if (elapsed >= totalDurationMs || dismissRequested) break
            val frameIndex = (elapsed / FRAME_DELAY_MS).toInt() % frames.size
            if (frameIndex != lastFrameIndex) {
                gmm.setMatrixFrame(frames[frameIndex])
                lastFrameIndex = frameIndex
            }
            delay(TICK_MS)
        }
        Log.d(TAG, "playAnimation ende: dismissRequested=$dismissRequested")

        // Letzter Frame ist bereits die aktuelle Uhrzeit statt die Anzeige
        // abzuschalten - dadurch kein Schwarzbild waehrend des Uebergangs,
        // egal ob/wann ClockGlyphToyService als naechstes selbst neu zeichnet.
        ClockFrame.draw(applicationContext, gmm)
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        // Backstop fuer den Fall, dass die Animation aus irgendeinem Grund nicht rechtzeitig
        // endet: das System ruft dies spaetestens nach 3 Minuten shortService-Laufzeit auf
        // und erwartet zeitnah stopSelf(). Ohne diesen Backstop wuerde die Matrix beim
        // harten Kill mitten in der Animation haengen bleiben, statt zur Uhrzeit zu springen.
        super.onTimeout(startId, fgsType)
        playerJob?.cancel()
        queue.clear()
        glyphMatrixManager?.let { gmm -> ClockFrame.draw(applicationContext, gmm) }
        stopSelf(startId)
    }

    override fun onDestroy() {
        playerJob?.cancel()
        // Bewusst kein turnOff()/unInit(): GlyphMatrixManager ist ein Prozess-Singleton
        // mit nur einer Verbindung. Ist gerade ClockGlyphToyService als aktiver Toy
        // gebunden und teilt sich denselben Singleton, wuerde das dessen Verbindung/
        // Anzeige mit beeinflussen. Wir haben den letzten Frame bereits selbst auf die
        // Uhrzeit gesetzt (siehe playAnimation), es gibt also nichts mehr aufzuraeumen.
        glyphMatrixManager = null
        super.onDestroy()
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_reminders),
                    NotificationManager.IMPORTANCE_MIN
                )
            )
        }
    }

    private fun updateNotification(title: String, queuedAfter: Int) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIF_ID, buildNotification(title, queuedAfter))
    }

    private fun buildNotification(title: String, queuedAfter: Int): Notification {
        val dismissIntent = Intent(this, ReminderGlyphService::class.java).setAction(ACTION_DISMISS)
        val dismissPendingIntent = PendingIntent.getService(
            this,
            0,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Formatierte Ressource statt zusammengesetztem Text: die Reihenfolge von Titel und
        // Anzahl ist nicht in jeder Sprache dieselbe, und "in Warteschlange" laesst sich nicht
        // sinnvoll uebersetzen, wenn es als Bruchstueck im Code klebt.
        val text = if (queuedAfter > 0) {
            resources.getQuantityString(R.plurals.notification_queued, queuedAfter, title, queuedAfter)
        } else {
            title
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_reminder_title))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_action_stop),
                dismissPendingIntent
            )
            .build()
    }

    companion object {
        private const val TAG = "ReminderGlyphService"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ANIMATION_TYPE = "extra_animation_type"
        const val EXTRA_LIBRARY_ANIMATION_ID = "extra_library_animation_id"
        const val EXTRA_INTERVAL_MINUTES = "extra_interval_minutes"
        /** Oeffentlich, weil [ReminderPlayback.dismiss] denselben Weg aus der App heraus nutzt. */
        const val ACTION_DISMISS = "com.notime.glyphkalender.action.DISMISS_REMINDER"
        private const val CHANNEL_ID = "glyph_reminders"
        private const val NOTIF_ID = 42
        private const val FRAME_DELAY_MS = 260L
        private const val ANIMATION_DURATION_MS = 8000L
        private const val TICK_MS = 100L

        fun start(
            context: Context,
            title: String,
            type: AnimationType,
            libraryAnimationId: Long? = null,
            intervalMinutes: Int = 1
        ) {
            val intent = Intent(context, ReminderGlyphService::class.java).apply {
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_ANIMATION_TYPE, type.name)
                if (libraryAnimationId != null) putExtra(EXTRA_LIBRARY_ANIMATION_ID, libraryAnimationId)
                putExtra(EXTRA_INTERVAL_MINUTES, intervalMinutes)
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
