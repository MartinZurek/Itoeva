package com.notime.glyphsim.ui

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notime.glyphcore.data.ReminderOpenDuration
import com.notime.glyphsim.R
import com.notime.glyphsim.data.CrashLog
import com.notime.glyphsim.matrix.AvatarAnimations
import com.notime.glyphsim.matrix.AvatarClip
import com.notime.glyphsim.matrix.AvatarMood
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.AvatarSpriteView
import com.notime.glyphsim.matrix.ClipStorage
import com.notime.glyphsim.matrix.ClockFrameSim
import com.notime.glyphsim.matrix.ClockStyle
import com.notime.glyphsim.matrix.MatrixAnimator
import com.notime.glyphsim.matrix.PlayClipRecorder
import com.notime.glyphsim.matrix.PlaySnapshot
import com.notime.glyphsim.matrix.PlayTimeLapse
import com.notime.glyphsim.matrix.PlayWeather
import com.notime.glyphsim.matrix.ReminderAnimationBus
import com.notime.glyphsim.matrix.SimulatedMatrixView
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Merkt sich die gerade laufende Erinnerung, solange sie noch nicht "gefuettert" wurde -
 * ohne diese Angaben liesse sich beim Zusammenschieben von Uhr und Avatar weder die richtige
 * Reaktion auswaehlen noch das Fuetter-Ereignis der passenden Erinnerung zuordnen.
 */
private data class ActiveReminder(
    val reminderId: Long,
    val occurrenceId: Long,
    val animationType: com.notime.glyphcore.data.AnimationType?,
    val libraryAnimationLabel: String?,
    /** Vorschau-Frames der Erinnerung - noetig, falls sie statt gefuettert in einen
     *  Speicherplatz gezogen wird (siehe [SavedAction], [ActionSlotsColumn]). */
    val frames: List<IntArray>
)

private fun ActiveReminder.toSavedAction() =
    SavedAction(reminderId, occurrenceId, animationType, libraryAnimationLabel, frames)

/**
 * Startbildschirm: zeigt die simulierte, runde Glyph-Matrix mit der laufenden
 * Uhrzeit (tickt sekuendlich) - das ist der eigentliche "Trick" dieser App: keine
 * Nothing-Hardware noetig, die Matrix wird 1:1 im gleichen Pixel-/Helligkeitsformat
 * wie im :app-Modul nachgebaut und einfach auf dem Bildschirm gezeichnet.
 *
 * Emoji statt Icon-Font fuer die beiden Kopfzeilen-Aktionen: das Projekt haengt
 * bewusst nur an material-icons-core (siehe :app), nicht am deutlich groesseren
 * -extended-Artefakt - Nightlight/List-Icons sind dort nicht enthalten.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onEnterDockMode: () -> Unit,
    onOpenReminders: () -> Unit,
    /** Wie [onOpenReminders], oeffnet dort aber direkt den Bearbeiten-Dialog dieser Erinnerung -
     *  das Tapthrough vom Rhythmus-Kommentar im Pflegebuch (siehe [FeedStatsDialog]). */
    onOpenReminderForEdit: (Long) -> Unit,
    /** Von aussen hereingereichte KI-Antwort (Teilen) - oeffnet direkt den Import. */
    sharedImportText: String? = null,
    onSharedImportHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    // Fuer die Abkuerzung "zweimal die Uhr auf den Avatar" weiter unten - der Modus selbst wird
    // von MainActivity gezeigt, sobald PlayModePrefs umspringt.
    val playViewModel: PlayModeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    var showSettings by remember { mutableStateOf(false) }
    var showAssistant by remember { mutableStateOf(false) }
    // Der gerade abgespielte Vorstellungs-Clip (siehe AvatarClipPlayer/AvatarClips) - null heisst
    // "kein Player offen". Ein eigener Zustand statt eines Booleans, weil der Player den Clip
    // selbst braucht, nicht nur wissen muss, DASS er offen ist.
    var playingClip by remember { mutableStateOf<AvatarClip?>(null) }
    var avatarTapped by remember { mutableStateOf(OnboardingPrefs.hasTappedAvatar(context)) }
    // Beim allerersten Start stellt sich das Wesen von selbst vor, statt darauf zu warten, dass
    // jemand auf die Idee kommt, es anzutippen (siehe OnboardingPrefs.hasBeenGreeted). Der
    // Zustand entscheidet zugleich, WOMIT der Assistent aufgeht: Begruessung und Einfuehrung
    // statt Menue.
    var greeting by remember { mutableStateOf(!OnboardingPrefs.hasBeenGreeted(context)) }
    var clockTapped by remember { mutableStateOf(OnboardingPrefs.hasTappedClock(context)) }
    var showStats by remember { mutableStateOf(false) }
    // Uhr-Frame und Animations-Frame getrennt (siehe rememberClockFrame / DockScreen): die Uhr
    // tickt selbstaendig minutengenau weiter, die Erinnerungs-Animation legt sich darueber.
    var isPlayingAnimation by remember { mutableStateOf(false) }
    var animationFrame by remember { mutableStateOf<IntArray?>(null) }
    val clockFrame by rememberClockFrame(paused = isPlayingAnimation)
    val frame = animationFrame ?: clockFrame
    // Der gewaehlte Avatar wird beobachtet statt einmalig gelesen, damit die Anzeige unten
    // sofort umspringt, wenn im Einstellungs-Dialog ein anderer ausgewaehlt wird.
    val species by AvatarSpeciesPrefs.selected(context).collectAsStateWithLifecycle()
    val currentSpecies = species ?: AvatarSpecies.PUFFLING

    // Fuetter-Zustand: die Uhr laesst sich auf den Avatar schieben, genau wie im Dock-Modus.
    var activeReminder by remember { mutableStateOf<ActiveReminder?>(null) }
    var feedingOccurrenceId by remember { mutableStateOf<Long?>(null) }
    var clockDrag by remember { mutableStateOf(Offset.Zero) }
    var avatarDrag by remember { mutableStateOf(Offset.Zero) }
    var clockBounds by remember { mutableStateOf(Rect.Zero) }
    var avatarBounds by remember { mutableStateOf(Rect.Zero) }
    var clockAnimJob by remember { mutableStateOf<Job?>(null) }
    // Easter Egg "einmal um den Avatar herum" (siehe triggerCircleEasterEgg) - Winkel der Uhr
    // relativ zum Avatar-Mittelpunkt, aufsummiert waehrend EINER Ziehgeste.
    var dragAngleAccum by remember { mutableStateOf(0f) }
    var lastDragAngle by remember { mutableStateOf<Float?>(null) }
    // Wie viele volle Umdrehungen innerhalb der LAUFENDEN Geste schon eine Reaktion ausgeloest
    // haben - dragAngleAccum selbst wird dafuer NICHT mehr zurueckgesetzt (waechst ueber eine
    // Umdrehung hinaus weiter), sonst liesse sich eine zweite Umdrehung in derselben Geste nicht
    // von der ersten unterscheiden (siehe triggerCircleEasterEgg).
    var lastTriggeredRevolutions by remember { mutableStateOf(0) }
    // Vorgezogen aus der Deklaration weiter unten (bei der Idle-Schleife des Avatars) - der
    // Bus-Collector direkt darunter muss schon vor dieser Stelle darauf warten koennen.
    var isReacting by remember { mutableStateOf(false) }
    // Spielmodus-Schalter oben in der Leiste. Beim allerersten Einschalten erklaert der Avatar
    // zunaechst, was sich dadurch aendert (siehe PlayModeIntroDialog).
    val playActive by PlayModePrefs.active(context).collectAsStateWithLifecycle(initialValue = PlayModePrefs.isActive(context))
    // Der Modus, wie der Nutzer ihn sieht - aus beiden Schaltern zusammengesetzt (siehe AppMode).
    var appMode by remember { mutableStateOf(AppMode.current(context)) }
    val playState by playViewModel.state.collectAsStateWithLifecycle()
    var showPlayIntro by remember { mutableStateOf(false) }

    // Vier feste Speicherplaetze (siehe ActionSlots.kt): eine Aktion landet hier, wenn sie statt
    // auf den Avatar hierher gezogen wird, und bleibt liegen, bis sie von hier aus gefuettert
    // wird. `slotBounds` haelt ihre aktuellen Bildschirm-Grenzen fuer dieselbe Kollisionspruefung,
    // die auch Uhr und Avatar benutzen (siehe AvatarFeeding.overlaps).
    var slots by remember { mutableStateOf<List<SavedAction?>>(List(ACTION_SLOT_COUNT) { null }) }
    val slotBounds = remember { mutableStateListOf(*Array(ACTION_SLOT_COUNT) { Rect.Zero }) }

    /**
     * Laeuft eine Erinnerung aus, ohne dass darauf reagiert wurde: frueher war sie damit
     * unwiderruflich weg. Jetzt zieht sie stattdessen automatisch in den ersten freien
     * Speicherplatz - nur wenn alle vier belegt sind, bleibt es beim alten Verhalten, weil dafuer
     * schlicht kein Platz ist. Kein Fuettern, keine DB-Schreibung: die Ausloesung steht bereits in
     * `avatar_feed_events` und bleibt unbeantwortet, bis der Platz tatsaechlich gefuettert wird.
     */
    fun archiveActiveReminderIfExpired() {
        val current = activeReminder ?: return
        val freeIndex = slots.indexOfFirst { it == null }
        if (freeIndex >= 0) {
            slots = slots.toMutableList().also { it[freeIndex] = current.toSavedAction() }
        }
        activeReminder = null
        isPlayingAnimation = false
        animationFrame = null
    }

    // Faellige Erinnerungen kommen ueber ReminderAnimationBus rein (siehe dort) und
    // unterbrechen kurz den Uhr-Tick oben, statt dass hier nur immer die Uhrzeit zu
    // sehen ist - das Home-Screen-Widget bekommt dieselbe Animation direkt.
    //
    // Volle Offen-Dauer inklusive "Nonstop" (siehe ReminderOpenDuration): seit hier ebenfalls
    // gefuettert werden kann, laesst sich auch eine endlose Animation jederzeit beenden - der
    // fruehere Deckel waere jetzt eine unnoetige Einschraenkung. Nur das Widget deckelt noch
    // (dort gibt es keinen Avatar, siehe GlyphClockWidgetProvider).
    // Beim Oeffnen pruefen, ob gerade noch eine Erinnerung laeuft - genau der Fall, wenn man das
    // Widget waehrend einer Animation antippt und dadurch hier landet. Ohne das zeigte die App
    // stumpf die Uhr, und der Avatar liess sich nicht mehr fuettern (siehe OpenReminderLookup).
    LaunchedEffect(currentSpecies) {
        if (activeReminder != null) return@LaunchedEffect
        val open = OpenReminderLookup.find(context, AvatarSpeciesPrefs.profileId(currentSpecies))
            ?: return@LaunchedEffect
        val event = open.event
        activeReminder = ActiveReminder(event.reminderId, event.occurrenceId, event.animationType, event.libraryAnimationLabel, event.frames)
        isPlayingAnimation = true
        clockAnimJob = launch {
            // Nur die RESTdauer, nicht von vorn: die Erinnerung laeuft ja schon eine Weile, und
            // sie soll zum selben Zeitpunkt enden, zu dem sie auch im Widget geendet haette.
            MatrixAnimator.play(
                event.frames,
                targetDurationMs = open.remainingMillis,
                frameDelayMs = MatrixAnimator.CLOCK_FRAME_DELAY_MS
            ) {
                animationFrame = it
            }
        }
        clockAnimJob?.join()
        archiveActiveReminderIfExpired()
    }

    LaunchedEffect(Unit) {
        ReminderAnimationBus.events.collect { event ->
            clockAnimJob?.cancel()
            // Waehrend einer noch laufenden Fuetter-Reaktion ist die Uhr unten unsichtbar (siehe
            // isReacting) - deshalb erst hier weitermachen, sobald sie wieder da ist. Ohne das
            // begann die Anzeigedauer bereits "im Dunkeln" zu laufen: traf eine Erinnerung waehrend
            // einer noch laufenden Reaktion ein, konnte sie einen Teil ihrer Zeit oder sogar die
            // komplette Anzeigedauer verlieren, ohne je sichtbar (und damit fuetterbar) gewesen zu
            // sein - genau das liess bei mehreren kurz hintereinander ausgeloesten Erinnerungen eine
            // mittlere manchmal unfuetterbar werden, obwohl sie ganz normal ausgeloest hatte.
            snapshotFlow { isReacting }.first { !it }
            activeReminder = ActiveReminder(event.reminderId, event.occurrenceId, event.animationType, event.libraryAnimationLabel, event.frames)
            isPlayingAnimation = true
            clockAnimJob = launch {
                MatrixAnimator.play(
                    event.frames,
                    targetDurationMs = ReminderOpenDuration.toDurationMillis(event.openDurationSeconds),
                    frameDelayMs = MatrixAnimator.CLOCK_FRAME_DELAY_MS
                ) { animationFrame = it }
            }
            clockAnimJob?.join()
            // Ausgelaufen, ohne dass gefuettert wurde (Fuettern setzt activeReminder selbst
            // auf null und cancelt diesen Job) - zieht dann automatisch in einen freien
            // Speicherplatz statt verloren zu gehen (siehe archiveActiveReminderIfExpired).
            archiveActiveReminderIfExpired()
        }
    }

    // Idle-Schleife des Avatars unten - dieselbe Sequenz wie im Dock-Modus, samt des
    // spezies-eigenen Rhythmus (siehe AvatarAnimations.idleSequence).
    // Stimmung wirkt auf die Ruhe-Schleife (siehe AvatarMood) - refreshKey zaehlt nach jedem
    // Fuettern hoch, damit die Aufmunterung sofort sichtbar wird.
    var moodRefresh by remember { mutableStateOf(0) }
    val mood by rememberAvatarMood(currentSpecies, moodRefresh)
    val idle = remember(currentSpecies, mood) { AvatarAnimations.idleSequence(currentSpecies, mood) }
    var avatarFrame by remember(currentSpecies) { mutableStateOf(idle.frames.first()) }
    LaunchedEffect(currentSpecies, mood, isReacting) {
        if (isReacting) return@LaunchedEffect
        while (true) {
            MatrixAnimator.playTimed(idle.frames, idle.holdsMs) { avatarFrame = it }
        }
    }

    // Gemeinsamer Kern von feedNow (Uhr -> Avatar) und feedFromSlot (Speicherplatz -> Avatar):
    // beide beenden eine Ausloesung auf dieselbe Weise, nur woher sie kommt und was danach
    // aufzuraeumen ist ([onConsumed]) unterscheidet sich. [isStillRelevant] wiederholt die
    // Pruefung von vorher: Quelle kann waehrend der kurzen DB-Operation ausgelaufen oder ersetzt
    // worden sein - persistiert ist die Antwort trotzdem, eine Reaktion auf das falsche sichtbare
    // Ereignis waere aber irrefuehrend.
    fun feedOccurrence(
        occurrenceId: Long,
        animationType: com.notime.glyphcore.data.AnimationType?,
        libraryAnimationLabel: String?,
        isStillRelevant: () -> Boolean,
        onConsumed: () -> Unit
    ) {
        if (feedingOccurrenceId == occurrenceId) return
        feedingOccurrenceId = occurrenceId
        scope.launch {
            var confirmedReactionStarted = false
            try {
                val result = withContext(Dispatchers.IO) {
                    AvatarFeeding.logFeedEvent(context, occurrenceId)
                }
                if (!isStillRelevant()) return@launch
                if (!result.isUiSuccess()) {
                    Log.w(HOME_TAG, "Stale feed occurrenceId=$occurrenceId")
                    onConsumed()
                    return@launch
                }

                // Erst der bestaetigte DB-Erfolg darf die sichtbare Erfolgsreaktion ausloesen.
                onConsumed()
                isReacting = true
                confirmedReactionStarted = true
                AvatarFeeding.playReaction(
                    species = currentSpecies,
                    animationType = animationType,
                    libraryAnimationLabel = libraryAnimationLabel,
                    screenWidthPx = screenWidthPx,
                    screenHeightPx = screenHeightPx,
                    onFrame = { avatarFrame = it },
                    onOffset = { avatarDrag = it }
                )
            } catch (cancellation: kotlinx.coroutines.CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                // Die Transaktion hat Markierung und XP gemeinsam zurueckgerollt. Solange die
                // Erinnerung noch offen ist, bleibt sie sichtbar und kann erneut versucht werden.
                Log.e(HOME_TAG, "Feed transaction failed occurrenceId=$occurrenceId", error)
            } finally {
                // Zwingend im finally: isReacting blendet seit Neuestem die Uhr aus. Bliebe das
                // Flag nach einem Abbruch (Spezies gewechselt, Fehler in der Reaktion) auf true
                // stehen, waere die Uhr bis zum naechsten App-Start verschwunden - aus einem
                // kurzen Aussetzer wuerde ein dauerhaft kaputter Startbildschirm.
                if (confirmedReactionStarted) {
                    moodRefresh++
                    avatarDrag = Offset.Zero
                    clockDrag = Offset.Zero
                    isReacting = false
                }
                if (feedingOccurrenceId == occurrenceId) feedingOccurrenceId = null
            }
        }
    }

    // Herausgeloest, damit dieselbe Logik sowohl von der Drag-Kollision unten als auch von der
    // TalkBack-Zusatzaktion "Fuettern" am Avatar ausgeloest werden kann (siehe AvatarSpriteView
    // weiter unten) - per Drag laesst sich nicht sinnvoll fuer einen Screenreader bedienen.
    fun feedNow() {
        val current = activeReminder ?: return
        feedOccurrence(
            occurrenceId = current.occurrenceId,
            animationType = current.animationType,
            libraryAnimationLabel = current.libraryAnimationLabel,
            isStillRelevant = { activeReminder?.occurrenceId == current.occurrenceId },
            onConsumed = {
                activeReminder = null
                clockAnimJob?.cancel()
                isPlayingAnimation = false
                animationFrame = null
            }
        )
    }

    /**
     * Wendet eine zuvor abgelegte Aktion aus Speicherplatz [index] auf den Avatar an - dieselbe
     * Fuetter-Mechanik wie [feedNow], nur ohne die laufende Uhr-Animation.
     */
    fun feedFromSlot(index: Int) {
        val saved = slots.getOrNull(index) ?: return
        feedOccurrence(
            occurrenceId = saved.occurrenceId,
            animationType = saved.animationType,
            libraryAnimationLabel = saved.libraryAnimationLabel,
            isStillRelevant = { slots.getOrNull(index)?.occurrenceId == saved.occurrenceId },
            onConsumed = {
                slots = slots.toMutableList().also { it[index] = null }
            }
        )
    }

    /**
     * Legt die gerade laufende Erinnerung in Speicherplatz [index] ab, statt sie zu fuettern - sie
     * bleibt dort erhalten, bis sie spaeter per [feedFromSlot] auf den Avatar gezogen wird. Kein
     * Fuettern, keine Reaktion, keine XP: das Ablegen selbst ist ein neutraler Zwischenschritt.
     */
    fun saveToSlot(index: Int) {
        val current = activeReminder ?: return
        if (slots.getOrNull(index) != null) return
        slots = slots.toMutableList().also { it[index] = current.toSavedAction() }
        activeReminder = null
        clockAnimJob?.cancel()
        isPlayingAnimation = false
        animationFrame = null
        clockDrag = Offset.Zero
        if (!OnboardingPrefs.hasUsedActionSlot(context)) {
            OnboardingPrefs.markActionSlotUsed(context)
        }
    }

    // Kollisions-Erkennung waehrend des Ziehens (laeuft bei jeder Bewegung neu an, nicht erst
    // am Gesten-Ende) - die Reaktion selbst laeuft ueber [scope], damit sie ungestoert zu Ende
    // spielt, auch wenn der Finger danach noch bewegt wird.
    //
    // Der Avatar hat Vorrang vor den Speicherplaetzen: ueberschneidet die Uhr zufaellig beide
    // zugleich, gewinnt der sofortige Effekt - genau das erwartet, wer gezielt auf den Avatar
    // zielt. Erst wenn das nicht zutrifft, zaehlt ein freier Speicherplatz.
    LaunchedEffect(clockDrag) {
        if (activeReminder == null) return@LaunchedEffect
        if (AvatarFeeding.overlaps(clockBounds, avatarBounds)) {
            feedNow()
            return@LaunchedEffect
        }
        val freeSlotIndex = slotBounds.indexOfFirst { bounds ->
            AvatarFeeding.overlaps(clockBounds, bounds)
        }
        if (freeSlotIndex >= 0 && slots.getOrNull(freeSlotIndex) == null) {
            saveToSlot(freeSlotIndex)
        }
    }

    /**
     * Easter Egg: die Uhr einmal ganz um den Avatar herumziehen loest eine kleine ueberraschende
     * Reaktion aus - siehe Winkel-Tracking im Drag-Handler der Uhr weiter unten, das diese
     * Funktion fuer jede innerhalb DERSELBEN Geste neu erreichte volle Umdrehung aufruft (1., 2.,
     * ...). Beeinflusst keine Erinnerung, wird nirgends gezaehlt - ein reiner Gruss.
     *
     * Nur ausserhalb einer aktiven Erinnerung (sonst konkurriert es mit dem eigentlichen
     * Fuettern) und nicht waehrend schon eine andere Reaktion laeuft - bei schnellem Weiterdrehen
     * kann eine Umdrehung dadurch auch mal folgenlos bleiben, weil die vorherige Reaktion noch
     * spielt.
     *
     * Reaktion selbst variiert doppelt: [AvatarAnimations.tapReaction]/[AvatarAnimations.reactionFor]
     * unterscheiden ohnehin schon je Spezies (sechs eigene Choreografien), und die Wahl UNTER den
     * Reaktionen haengt von [revolutions] und der Stimmung ab:
     * - 1. Umdrehung: normale Freuden-Reaktion, mit stimmungsabhaengiger Chance (siehe
     *   [rocketChanceFor]) stattdessen die seltene "Rocket"-Flugreaktion.
     * - ab der 2. Umdrehung IN DERSELBEN Geste: garantiert die Flugreaktion - eine verlaessliche
     *   Steigerung als Belohnung fuers Drandranbleiben, statt nur eine hoehere Zufallschance.
     * "Rocket" ist sonst nur der gleichnamigen Bibliotheks-Animation vorbehalten -
     * [AvatarAnimations.reactionFor] entscheidet rein ueber den Namen, die Bibliotheks-Animation
     * muss dafuer gar nicht existieren.
     */
    fun triggerCircleEasterEgg(revolutions: Int) {
        if (isReacting || activeReminder != null) return
        isReacting = true
        scope.launch {
            try {
                val useRocket = revolutions >= 2 || Random.nextFloat() < rocketChanceFor(mood)
                if (useRocket) {
                    AvatarFeeding.playReaction(
                        species = currentSpecies,
                        animationType = null,
                        libraryAnimationLabel = "Rocket",
                        screenWidthPx = screenWidthPx,
                        screenHeightPx = screenHeightPx,
                        onFrame = { avatarFrame = it },
                        onOffset = { avatarDrag = it }
                    )
                } else {
                    val reaction = AvatarAnimations.tapReaction(currentSpecies)
                    val pace = reactionPaceFor(mood)
                    val pacedHolds = reaction.holdsMs.map { (it * pace).toLong() }
                    MatrixAnimator.playTimed(reaction.frames, pacedHolds) { avatarFrame = it }
                }
            } finally {
                avatarDrag = Offset.Zero
                isReacting = false
            }
        }
    }

    /**
     * Kurze Freuden-Reaktion beim Antippen des Avatars, bevor sich der Assistent oeffnet - macht
     * ihn "reaktiver", statt dass ein Antippen kommentarlos direkt in ein Menue springt.
     *
     * [onSettled] (oeffnet den Assistenten) wird erst nach [AVATAR_TAP_PEEK_MS] aufgerufen, die
     * Reaktion selbst laeuft unabhaengig davon zu Ende weiter (wie ueberall sonst bei Reaktionen,
     * z.B. [feedNow]) - der Dialog verdeckt sie danach einfach, ohne sie abzubrechen.
     *
     * Laeuft schon eine andere Reaktion oder wartet eine Erinnerung auf Antwort, wird gar nicht
     * erst reagiert (die Aufmerksamkeit gehoert dann dem, was gerade passiert) - der Assistent
     * oeffnet trotzdem sofort, ohne die kuenstliche Verzoegerung.
     */
    fun playAvatarTapReaction(onSettled: () -> Unit) {
        if (isReacting || activeReminder != null) {
            onSettled()
            return
        }
        isReacting = true
        scope.launch {
            try {
                val reaction = AvatarAnimations.tapReaction(currentSpecies)
                MatrixAnimator.playTimed(reaction.frames, reaction.holdsMs) { avatarFrame = it }
            } finally {
                isReacting = false
            }
        }
        scope.launch {
            delay(AVATAR_TAP_PEEK_MS)
            onSettled()
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            Column {
                TopAppBar(
                    // **Der Tagesstand steht hier NICHT mehr.**
                    //
                    // Er stand hier, weil dies die einzige Stelle war, an der er dauerhaft
                    // sichtbar sein konnte. Genau das war der Fehler: Diese App ist inzwischen so
                    // gebaut, dass man den Begleiter FRAGT und er antwortet - der Streifen war der
                    // letzte Rest der aelteren Bauweise "alles ist immer zu sehen". Beides
                    // nebeneinander heisst, dass dieselbe Auskunft an zwei Stellen steht und dem
                    // Antippen des Avatars seinen Sinn nimmt: Wer den Stand schon oben liest,
                    // fragt nicht mehr nach.
                    //
                    // Dazu kam, dass der Streifen ueber beide Modi dasselbe zeigte, obwohl Spiel
                    // und Normalbetrieb voellig verschiedene Zahlen fuehren (siehe PlayTalk).
                    //
                    // Feste, niedrigere Inhaltshoehe (statt der M3-Standardhoehe von 64dp): In der
                    // Leiste stehen nur noch zwei Textknoepfe, und die volle Hoehe waere ein
                    // leerer Balken ueber der Uhr.
                    expandedHeight = 40.dp,
                    // **Die Modus-Auswahl steht LINKS, wo frueher der Tagesstand stand.**
                    //
                    // Sie gehoert nicht zu den Aktionen rechts: Eine Aktion tut etwas und ist
                    // danach vorbei, diese Auswahl dagegen sagt dauerhaft, in welchem Zustand die
                    // App ist. Links gelesen zu werden, bevor man irgendetwas antippt, ist genau
                    // richtig fuer eine Angabe, die alles Weitere einordnet.
                    title = {
                        ModeSelector(
                            current = appMode,
                            onSelect = { mode ->
                                // Beim ersten Mal ins Spiel erst erklaeren, eingeschaltet wird
                                // dann im Dialog - sonst aendert sich unvermittelt, was die App
                                // tut.
                                if (mode == AppMode.PLAY && !PlayModePrefs.hasSeenIntro(context)) {
                                    showPlayIntro = true
                                } else {
                                    appMode = mode
                                    // switchTo statt set: der Modus entscheidet mit, ob ueberhaupt
                                    // Alarme stehen duerfen (siehe QuietModeState). Ohne das
                                    // Nachziehen hoerte die App beim Verlassen des Uhr-Modus
                                    // still auf zu erinnern.
                                    scope.launch { AppMode.switchTo(context, mode) }
                                }
                            }
                        )
                    },
                    actions = {
                        // "Dock an" ist entfallen: das Antippen der Uhr fuehrt bereits dorthin, ein
                        // zweiter Weg zum selben Ziel kostete nur Platz.
                        //
                        // Aus demselben Grund ist das ZAHNRAD entfallen. Der Avatar ist der
                        // Einstieg in Einfuehrung, gefuehrtes Einrichten, Import UND Einstellungen
                        // - der einmalige Hinweis unter ihm sagt genau das. Ein zweiter Weg
                        // daneben nahm dieser Rolle die Eindeutigkeit: Wer das Zahnrad sah,
                        // brauchte den Avatar nie anzutippen und fand die Haelfte der App nicht.
                        // Uebrig bleiben in der Leiste nur die beiden Dinge, die der Avatar NICHT
                        // kann: den Spielmodus umschalten und die Erinnerungsliste oeffnen.
                        TextButton(onClick = onOpenReminders) {
                            Text(stringResource(R.string.home_reminders))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black,
                        titleContentColor = Color(0xFFE8E4DA),
                        actionIconContentColor = Color(0xFFE8E4DA)
                    )
                )
                // Die Ueberlaufzeile ist mit dem Streifen entfallen - sie war nur dessen
                // Fortsetzung fuer den Fall, dass mehr Erinnerungen als Platz da waren.
            }
        }
    ) { padding ->
        // Aeussere Box statt direkt der Column: die vier Speicherplaetze (siehe ActionSlots.kt)
        // stehen fest am rechten Rand, unabhaengig von der zentrierten Spalte aus Uhr und Avatar -
        // eine zweite, ueberlagernde Ebene statt eines weiteren Eintrags in deren Ablauf.
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Schwarzer Grund wie im Dock-Modus: Uhr und Avatar sind Lichtpunkte auf dunklem
                // Untergrund. Auf der hellen Themefarbe brauchten beide eine eigene dunkle
                // Flaeche, um ueberhaupt lesbar zu sein - und genau diese Flaechen wirkten als
                // Kasten bzw. Kreis um die Figuren und beschnitten deren Animationen. Mit
                // schwarzem Hintergrund entfaellt der Grund dafuer, beide duerfen frei stehen.
                .background(Color.Black)
                .padding(padding)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Einmaliger Hinweis auf die Uhr, bevor sie angetippt wird - sie fuehrt sofort und
            // ohne Rueckfrage in den Dock-Modus (staendig eingeschaltetes Display, spuerbar mehr
            // Akkuverbrauch), siehe OnboardingPrefs.hasTappedClock. Ohne diesen Hinweis waere ein
            // zufaelliges erstes Antippen eine unerklaerte Ueberraschung. Gleiches Muster wie der
            // Hinweis auf den Avatar unten: Sprechblase statt Dialog, verschwindet nach der
            // ersten Beruehrung fuer immer.
            if (!clockTapped) {
                Text(
                    stringResource(R.string.onboarding_tap_clock),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TamaPalette.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp, topEnd = 16.dp,
                                bottomEnd = 16.dp, bottomStart = 4.dp
                            )
                        )
                        .background(TamaPalette.BubbleBackground)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
                Spacer(Modifier.height(10.dp))
            }

            // Bewusst deutlich kleiner als frueher (0.82f): die Uhr fuellte den Startbildschirm
            // fast komplett aus und liess keinen Platz fuer den Avatar darunter.
            //
            // Waehrend der Fuetter-Reaktion verschwindet die Uhr, damit ihr runder Puck die
            // Reaktion des Avatars nicht ueberdeckt (gleiche Begruendung wie im Dock-Modus,
            // siehe DockScreen). Anders als dort steht sie hier aber in einer Column - wuerde
            // sie ersatzlos entfallen, ruckte der Avatar nach oben. Der Box-Platzhalter haelt
            // deshalb exakt ihre Flaeche frei (SimulatedMatrixView ist intern quadratisch).
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.52f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                if (!isReacting) {
                    // TalkBack haette sonst keinerlei Information ueber das, was hier gezeichnet
                    // wird - je nachdem, ob gerade eine Erinnerung wartet, entweder deren Thema
                    // oder schlicht die aktuelle Uhrzeit.
                    val activeTopicLabel = activeReminder?.libraryAnimationLabel
                        ?: activeReminder?.animationType?.let { stringResource(it.labelRes) }
                        ?: stringResource(R.string.a11y_reminder_generic)
                    val clockContentDescription = if (activeReminder != null) {
                        stringResource(R.string.a11y_clock_reminder, activeTopicLabel)
                    } else {
                        val now = LocalTime.now()
                        stringResource(R.string.a11y_clock_time, "%02d:%02d".format(now.hour, now.minute))
                    }
                    // Geste und Bedienungshilfen teilen sich denselben Ablauf - sonst
                    // liefen sie irgendwann auseinander.
                    val oeffneDockLabel = stringResource(R.string.a11y_clock_open_dock)
                    val oeffneDock = {
                        if (!clockTapped) {
                            clockTapped = true
                            OnboardingPrefs.markClockTapped(context)
                        }
                        onEnterDockMode()
                    }
                    // Wie [feedActionLabel] weiter unten am Avatar: Ziehen laesst sich fuer einen
                    // Screenreader nicht sinnvoll bedienen, deshalb bekommt jeder FREIE
                    // Speicherplatz hier eine eigene Zusatzaktion, solange eine Erinnerung wartet.
                    val slotSaveActions = if (activeReminder != null) {
                        slots.mapIndexedNotNull { index, saved ->
                            if (saved == null) {
                                index to stringResource(R.string.a11y_clock_save_to_slot, index + 1)
                            } else {
                                null
                            }
                        }
                    } else {
                        emptyList()
                    }
                    // Reihenfolge der Modifier ist wichtig: .offset VOR .onGloballyPositioned,
                    // damit die gemeldeten Bounds die gezogene Position enthalten - genau die
                    // braucht die Kollisionspruefung gegen den Avatar.
                    SimulatedMatrixView(
                        frame = frame,
                        // MIT Puck - wie im Dock-Modus und in den Clips. Ohne ihn schwebten die
                        // Punkte frei auf dem schwarzen Grund und die Uhr hatte keine erkennbare
                        // Gestalt mehr; sie sah nach einem Rest von etwas aus statt nach einem
                        // Geraet. Dieselbe Uhr soll ueberall dieselbe Uhr sein.
                        contentDescription = clockContentDescription,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(clockDrag.x.roundToInt(), clockDrag.y.roundToInt()) }
                            .onGloballyPositioned { clockBounds = it.boundsInRoot() }
                            .pointerInput(Unit) {
                                detectTapGestures(onTap = { oeffneDock() })
                            }
                            /*
                             * `detectTapGestures` ist reine Zeigerverarbeitung - fuer die
                             * Bedienungshilfen existiert dieses Antippen schlicht nicht. Ein
                             * Screenreader las die Uhr vor und bot nichts an, obwohl ein Tipp der
                             * Weg in den Dock-Modus ist; mit Switch Access oder Tastatur war er
                             * ueberhaupt nicht erreichbar.
                             *
                             * Die Aktion hier meldet genau dasselbe Verhalten an die Semantik.
                             * Beschriftung statt blossem "Doppeltippen zum Aktivieren", damit
                             * vorgelesen wird, WOHIN es fuehrt.
                             */
                            .semantics {
                                role = Role.Button
                                onClick(label = oeffneDockLabel) { oeffneDock(); true }
                                if (slotSaveActions.isNotEmpty()) {
                                    customActions = slotSaveActions.map { (index, label) ->
                                        CustomAccessibilityAction(label) { saveToSlot(index); true }
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = {
                                        lastDragAngle = null
                                        dragAngleAccum = 0f
                                        lastTriggeredRevolutions = 0
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        clockDrag += dragAmount

                                        // Easter Egg: Winkel der Uhr relativ zum Avatar-
                                        // Mittelpunkt verfolgen, um eine volle Umkreisung zu
                                        // erkennen (siehe triggerCircleEasterEgg). Nur wenn
                                        // gerade keine Erinnerung wartet - die Uhr dient dann
                                        // dem Fuettern, nicht dem Easter Egg - und beide
                                        // Positionen schon bekannt sind.
                                        if (activeReminder == null &&
                                            avatarBounds != Rect.Zero &&
                                            clockBounds != Rect.Zero
                                        ) {
                                            val avatarCenter = avatarBounds.center
                                            val clockCenter = clockBounds.center
                                            val angle = atan2(
                                                clockCenter.y - avatarCenter.y,
                                                clockCenter.x - avatarCenter.x
                                            )
                                            val last = lastDragAngle
                                            if (last != null) {
                                                var delta = angle - last
                                                // Auf (-PI, PI] normalisieren - sonst zaehlte
                                                // der Sprung beim Ueberschreiten von +-180°
                                                // faelschlich fast eine halbe Umdrehung mit.
                                                if (delta > PI.toFloat()) delta -= (2 * PI).toFloat()
                                                if (delta < -PI.toFloat()) delta += (2 * PI).toFloat()
                                                dragAngleAccum += delta
                                                // Ganzzahliger Umdrehungs-Stand statt eines
                                                // einzelnen Schwellwerts - erkennt so auch die
                                                // zweite (dritte, ...) Umdrehung in derselben
                                                // Geste als eigenes, neues Ereignis.
                                                val revolutions = (abs(dragAngleAccum) / (2 * PI).toFloat()).toInt()
                                                if (revolutions > lastTriggeredRevolutions) {
                                                    lastTriggeredRevolutions = revolutions
                                                    triggerCircleEasterEgg(revolutions)
                                                }
                                            }
                                            lastDragAngle = angle
                                        }
                                    },
                                    // Ohne aktive Erinnerung ist das Ziehen folgenlos - dann
                                    // gleitet die Uhr am Gesten-Ende an ihren Platz zurueck,
                                    // statt verschoben liegenzubleiben.
                                    onDragEnd = { if (activeReminder == null) clockDrag = Offset.Zero }
                                )
                            }
                    )
                }
            }
            Spacer(Modifier.height(28.dp))

            // Einmaliger Hinweis auf den Avatar. Er ist der Einstieg in Einfuehrung, gefuehrtes
            // Einrichten, KI-Import und Einstellungen - nur sieht man ihm das nicht an. Bewusst
            // eine Sprechblase und kein Dialog: sie erklaert sich von selbst, blockiert nichts
            // und verschwindet nach der ersten Beruehrung fuer immer (siehe OnboardingPrefs).
            if (!avatarTapped) {
                Text(
                    stringResource(R.string.onboarding_tap_me),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TamaPalette.TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp, topEnd = 16.dp,
                                bottomEnd = 16.dp, bottomStart = 4.dp
                            )
                        )
                        .background(TamaPalette.BubbleBackground)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
                Spacer(Modifier.height(10.dp))
            }

            // Level/XP stehen bewusst NICHT hier, sondern nur im Dock-Modus (siehe DockScreen):
            // Der Startbildschirm ist der Ort zum Einrichten und Nachsehen, nicht die Spielflaeche
            // - eine Fortschrittszeile zwischen Uhr und Avatar war schlicht im Weg.

            // Der gewaehlte Avatar - nicht nur Deko: er ist hier, wie im Dock-Modus, das Ziel der
            // Fuetter-Geste, und sein Pflegebuch entscheidet ueber Stimmung und Spielstand (siehe
            // PresentCompanion). Auf die Erinnerungsliste hat er seit Phase 4b keinen Einfluss
            // mehr - die gehoert dem Nutzer (siehe RoutineOwner).
            val feedActionLabel = stringResource(R.string.a11y_feed_action)
            AvatarSpriteView(
                frame = avatarFrame,
                // Ohne eigene Flaeche: der schwarze Kasten schnitt bislang alles ab, was ueber
                // das Sprite-Quadrat hinausragte - besonders sichtbar bei der Rocket-Reaktion,
                // die den Avatar quer ueber den Bildschirm fliegen laesst.
                showBackground = false,
                contentDescription = stringResource(
                    R.string.a11y_avatar_state,
                    stringResource(currentSpecies.labelRes),
                    mood.describeForAccessibility()
                ),
                modifier = Modifier
                    .size(96.dp)
                    .offset { IntOffset(avatarDrag.x.roundToInt(), avatarDrag.y.roundToInt()) }
                    .onGloballyPositioned { avatarBounds = it.boundsInRoot() }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (!avatarTapped) {
                                avatarTapped = true
                                OnboardingPrefs.markAvatarTapped(context)
                            }
                            playAvatarTapReaction(onSettled = { showAssistant = true })
                        }
                    )
                    // Fuettern ist sonst nur per Ziehen der Uhr auf den Avatar moeglich - fuer
                    // TalkBack gibt es dafuer diese Zusatzaktion (erreichbar ueber das
                    // Aktionen-Menue), solange gerade eine Erinnerung auf Reaktion wartet.
                    .then(
                        if (activeReminder != null) {
                            Modifier.semantics {
                                customActions = listOf(
                                    CustomAccessibilityAction(feedActionLabel) { feedNow(); true }
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
            )

        }

            // Vier feste Speicherplaetze rechts, vertikal zentriert (siehe ActionSlotsColumn) -
            // eine eigene, ueberlagernde Spalte statt Teil des zentrierten Ablaufs oben: die Welt
            // (Uhr, Avatar) bleibt dadurch unveraendert mittig, unabhaengig davon, wie viele
            // Plaetze gerade belegt sind.
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Einmaliger Hinweis, solange noch nie etwas abgelegt wurde UND gerade nichts
                // belegt ist - sonst erklaert er etwas, das laengst benutzt wird oder gerade
                // sichtbar vorgemacht wird.
                if (!OnboardingPrefs.hasUsedActionSlot(context) && slots.all { it == null }) {
                    Text(
                        stringResource(R.string.onboarding_action_slots),
                        style = MaterialTheme.typography.bodySmall,
                        color = TamaPalette.TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .widthIn(max = 120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TamaPalette.BubbleBackground)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
                ActionSlotsColumn(
                    slots = slots,
                    avatarBounds = avatarBounds,
                    onBoundsChanged = { index, bounds -> slotBounds[index] = bounds },
                    onDropOnAvatar = { feedFromSlot(it) }
                )
            }
        }
    }

    if (showPlayIntro) {
        PlayModeIntroDialog(
            species = currentSpecies,
            onStart = {
                PlayModePrefs.markIntroSeen(context)
                showPlayIntro = false
                appMode = AppMode.PLAY
                scope.launch { AppMode.switchTo(context, AppMode.PLAY) }
            },
            onDismiss = { showPlayIntro = false }
        )
    }

    // Sofort beim Aufbau und nicht nach einer Verzoegerung: Ein Dialog, der erst nach ein paar
    // Sekunden aufspringt, wirkt wie ein Fehler - der Nutzer hat dann schon angefangen, sich
    // umzusehen. Der Merker wird hier gesetzt und nicht beim Schliessen: Wer wegtippt, hat sich
    // entschieden; ein Gruss, der beim naechsten Start wiederkommt, ist keiner mehr.
    LaunchedEffect(Unit) {
        if (greeting) {
            OnboardingPrefs.markGreeted(context)
            showAssistant = true
        }
    }

    if (showAssistant) {
        AvatarAssistantDialog(
            species = currentSpecies,
            startScreen = if (greeting) AssistantScreen.INTRO else AssistantScreen.MENU,
            greeting = greeting,
            onOpenSettings = {
                showAssistant = false
                greeting = false
                showSettings = true
            },
            onDismiss = {
                showAssistant = false
                // Ab jetzt ist es das gewohnte Menue - die Begruessung gibt es genau einmal.
                greeting = false
            },
            onPlayClip = AvatarClips.forSpecies(currentSpecies)?.let { clip ->
                {
                    showAssistant = false
                    playingClip = clip
                }
            }
        )
    }
    // Zurueck ins Avatar-Menue (nicht auf den Startbildschirm) nach Ende oder manuellem
    // Schliessen - genau das hatte der Nutzer fuer diese Funktion verlangt.
    playingClip?.let { clip ->
        AvatarClipPlayer(
            clip = clip,
            onFinished = {
                playingClip = null
                showAssistant = true
            }
        )
    }
    if (showSettings) {
        SettingsDialog(onDismiss = { showSettings = false })
    }
    // Geteilter Text oeffnet den Import unmittelbar - der Nutzer hat die Absicht ja schon
    // in der anderen App geaeussert, ein weiterer Zwischenschritt waere nur Reibung.
    if (sharedImportText != null) {
        ReminderImportDialog(
            initialText = sharedImportText,
            onDismiss = onSharedImportHandled
        )
    }

    if (showStats) {
        FeedStatsDialog(
            species = currentSpecies,
            onDismiss = { showStats = false },
            onEditReminder = { reminderId ->
                showStats = false
                onOpenReminderForEdit(reminderId)
            }
        )
    }
}




/**
 * **Die drei Modi nebeneinander - man WAEHLT einen aus, statt durchzutippen.**
 *
 * Die erste Fassung war ein einzelner Knopf, der den aktuellen Modus nannte und beim Antippen
 * reihum wechselte. Das war kompakt und in einem Punkt falsch: Man konnte sich nichts aussuchen.
 * Wer von "Nur Uhr" ins Spiel wollte, musste erst durch die Erinnerungen hindurch - und wer die
 * App neu hatte, erfuhr nie, dass es ueberhaupt drei gibt. Ein Zustand, den man nicht sehen kann,
 * laesst sich auch nicht verstehen.
 *
 * **Ohne Rahmen und ohne Knopfform.** Drei umrandete Schaltflaechen waeren ueber einer schwarzen
 * Flaeche mit einer leuchtenden Uhr drei helle Kaesten - genau die Art Bedienelement, die dieser
 * Bildschirm sonst vermeidet. Der gewaehlte Modus ist schlicht hell und die anderen sind
 * gedaempft; das genuegt, um ihn zu erkennen, und bleibt ruhig.
 */
@Composable
private fun ModeSelector(current: AppMode, onSelect: (AppMode) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (mode in AppMode.entries) {
            val selected = mode == current
            Text(
                stringResource(mode.labelRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    // Das Spiel bekommt seine eigene Farbe, sobald es laeuft - es ist der einzige
                    // Modus, der die App spuerbar veraendert, und das darf man sehen.
                    selected && mode == AppMode.PLAY -> Color(0xFF7FD1A6)
                    selected -> Color(0xFFE8E4DA)
                    else -> Color(0xFF6E6A63)
                },
                modifier = Modifier
                    .clickable(enabled = !selected) { onSelect(mode) }
                    .padding(vertical = 6.dp)
            )
        }
    }
}

/**
 * Wahrscheinlichkeit, dass das Umkreisen-Easter-Egg (eine Umdrehung) die seltene "Rocket"-
 * Flugreaktion statt der gewoehnlichen Freuden-Reaktion zeigt - siehe triggerCircleEasterEgg.
 *
 * Haengt bewusst von der Stimmung ab, nicht nur vom Zufall: dieselbe "weniger Energie"-Sprache
 * wie ueberall sonst (siehe AvatarMood-Klassendoku), nur hier als Verhalten statt als Satz - ein
 * Avatar mit wenig Energie zeigt seltener die aufwendige Flugreaktion, ganz ausschliessen soll
 * ihn das aber nicht (siehe [MoodPrefs]-Grundsatz "nie mit Schuldgefuehlen arbeiten": auch bei
 * SAD bleibt eine kleine Chance).
 */
private fun rocketChanceFor(mood: AvatarMood): Float = when (mood) {
    AvatarMood.HAPPY -> 0.5f
    AvatarMood.CONTENT -> 0.35f
    AvatarMood.NEUTRAL -> 0.3f
    AvatarMood.HUNGRY -> 0.15f
    AvatarMood.SAD -> 0.05f
}

/**
 * Tempo-Faktor fuer die gewoehnliche (nicht-Rocket) Easter-Egg-Reaktion, auf deren `holdsMs`
 * angewandt - >1 heisst langsamer. Dieselbe Choreografie bekommt so je nach Stimmung
 * spuerbar mehr oder weniger Schwung, ohne dass dafuer eigene Animationsdaten je Stimmung noetig
 * waeren (die sich blind, ohne visuelle Kontrolle, kaum verlaesslich von Hand zeichnen liessen).
 */
private fun reactionPaceFor(mood: AvatarMood): Float = when (mood) {
    AvatarMood.HAPPY -> 0.85f
    AvatarMood.CONTENT -> 1f
    AvatarMood.NEUTRAL -> 1f
    AvatarMood.HUNGRY -> 1.25f
    AvatarMood.SAD -> 1.5f
}

/** Wie lange der Avatar-Assistent nach einem Antippen des Avatars wartet, bevor er sich oeffnet -
 *  kurz genug, um nicht wie eine Verzoegerung zu wirken, lang genug, um den ersten Moment der
 *  Freuden-Reaktion noch zu sehen, bevor der Dialog sie verdeckt. */
private const val AVATAR_TAP_PEEK_MS = 320L
private const val HOME_TAG = "HomeScreen"


/**
 * Zwei Abschnitte: Uhr-Design (siehe [ClockStyle]/ClockStylePrefs, Auswahl mit
 * Live-Vorschau je Design) und Dock-Helligkeit (laeuft standardmaessig mit der
 * normalen Systemhelligkeit, kein Override - genau wie das Home-Screen-Widget auch;
 * wer trotzdem dimmen will, kann das hier bewusst einschalten, per Default aus).
 */
@Composable
private fun SettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedLanguage by remember { mutableStateOf(LanguagePrefs.get(context)) }
    var moodEnabled by remember { mutableStateOf(MoodPrefs.isEnabled(context)) }
    var soundEnabled by remember { mutableStateOf(PlaySound.isEnabled(context)) }
    var lapseSpeed by remember { mutableStateOf(PlayTimeLapse.speed()) }
    var forcedWeather by remember { mutableStateOf(PlayWeather.forcedOrNull()) }
    // Aufnahme laeuft / fertige Datei / Fehlschlag - drei Zustaende, mehr braucht es nicht.
    var clipEnabled by remember { mutableStateOf(ClipPrefs.isEnabled(context)) }
    var showClipLibrary by remember { mutableStateOf(false) }
    var clipRefresh by remember { mutableStateOf(0) }
    // Filme und Schnappschuesse zusammen, neueste zuerst - siehe ClipLibraryDialog dazu, warum
    // sie nicht getrennt aufgefuehrt werden.
    val clips = remember(clipRefresh) {
        (PlayClipRecorder.library(context) + PlaySnapshot.library(context))
            .sortedByDescending { it.lastModified() }
    }
    val snapshotCountInLibrary = remember(clips) {
        clips.count { it.extension.equals("png", true) }
    }
    var crashReport by remember { mutableStateOf(CrashLog.read(context)) }
    var rotationAllowed by remember { mutableStateOf(OrientationPrefs.isRotationAllowed(context)) }
    var selectedStyle by remember { mutableStateOf(ClockStylePrefs.get(context)) }
    var selectedSpecies by remember { mutableStateOf(AvatarSpeciesPrefs.get(context)) }
    var overrideEnabled by remember { mutableStateOf(DockBrightnessPrefs.isOverrideEnabled(context)) }
    var brightness by remember { mutableStateOf(DockBrightnessPrefs.getBrightness(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sprache zuerst: wer die App in der falschen Sprache vorfindet, sucht genau
                // hiernach - und findet es dann ganz oben statt am Ende der Liste.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    AppLanguage.entries.forEach { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (language != selectedLanguage) {
                                        selectedLanguage = language
                                        LanguagePrefs.set(context, language)
                                        // Ein Sprachwechsel wirkt erst, wenn die Activity ihre
                                        // Ressourcen neu aufloest (siehe
                                        // MainActivity.attachBaseContext) - deshalb neu starten
                                        // statt nur den Zustand zu aendern.
                                        (context as? Activity)?.recreate()
                                    }
                                }
                                .padding(vertical = 6.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = language == selectedLanguage,
                                onClick = null
                            )
                            Text(language.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.settings_clock_design),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    ClockStyle.entries.forEach { style ->
                        ClockStyleRow(
                            style = style,
                            selected = style == selectedStyle,
                            onClick = {
                                selectedStyle = style
                                ClockStylePrefs.set(context, style)
                            }
                        )
                    }
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.settings_avatar),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.settings_avatar_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_mood))
                            Text(
                                stringResource(R.string.settings_mood_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = moodEnabled,
                            onCheckedChange = { enabled ->
                                moodEnabled = enabled
                                MoodPrefs.setEnabled(context, enabled)
                            }
                        )
                    }
                    // **Ton - standardmaessig aus.** Diese App war bis dahin vollstaendig stumm;
                    // wer nichts eingeschaltet hat, hoert weiterhin nichts (siehe PlaySound).
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_sound))
                            Text(
                                stringResource(R.string.settings_sound_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { enabled ->
                                soundEnabled = enabled
                                PlaySound.setEnabled(context, enabled)
                            }
                        )
                    }
                    // Freischalten - AUFGENOMMEN wird im Play-Modus, nicht hier. Die
                    // Einstellungen sind der Ort zum Einrichten, nicht zum Bedienen.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_clip_enable))
                            Text(
                                stringResource(R.string.settings_clip_enable_hint) + " " +
                                    stringResource(R.string.settings_clip_enable_snapshot),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = clipEnabled,
                            onCheckedChange = { enabled ->
                                clipEnabled = enabled
                                ClipPrefs.setEnabled(context, enabled)
                            }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_library))
                            Text(
                                if (clips.isEmpty()) stringResource(R.string.settings_library_empty)
                                else stringResource(
                                    R.string.settings_library_hint,
                                    clips.size - snapshotCountInLibrary,
                                    snapshotCountInLibrary
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            enabled = clips.isNotEmpty(),
                            onClick = { showClipLibrary = true }
                        ) { Text(stringResource(R.string.action_open)) }
                    }

                    // Testschalter: laesst den Tagesablauf im Zeitraffer laufen, damit sich alle
                    // Tageszeiten und Szenen in wenigen Minuten ansehen lassen, statt einen
                    // echten Tag darauf zu warten (siehe PlayTimeLapse).
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_timelapse))
                        }
                        // Drei Stufen statt eines Schalters - siehe PlayTimeLapse.Speed. Wer sich
                        // den Tagesablauf ansehen will, braucht ihn zuegig aber verfolgbar; wer
                        // PRUEFEN will, ob alle Orte vorkommen, braucht viele Tage in Minuten.
                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            listOf(
                                PlayTimeLapse.Speed.OFF to R.string.settings_timelapse_off,
                                PlayTimeLapse.Speed.FAST to R.string.settings_timelapse_fast,
                                PlayTimeLapse.Speed.TURBO to R.string.settings_timelapse_turbo
                            ).forEach { (speed, label) ->
                                TextButton(onClick = {
                                    lapseSpeed = speed
                                    PlayTimeLapse.setSpeed(context, speed)
                                }) {
                                    Text(
                                        stringResource(label),
                                        color = if (lapseSpeed == speed) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        stringResource(R.string.settings_timelapse_speeds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Wetter festhalten - aus demselben Grund wie der Zeitraffer: Regen faellt an
                    // knapp jedem fuenften Tag und Schnee nur im Winter. Ohne diesen Schalter
                    // liesse sich nicht beurteilen, ob es gut aussieht.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_weather))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            listOf(
                                null to R.string.settings_weather_today,
                                PlayWeather.CLEAR to R.string.settings_weather_clear,
                                PlayWeather.RAIN to R.string.settings_weather_rain,
                                PlayWeather.SNOW to R.string.settings_weather_snow
                            ).forEach { (value, label) ->
                                TextButton(onClick = {
                                    forcedWeather = value
                                    PlayWeather.force(context, value)
                                }) {
                                    Text(
                                        stringResource(label),
                                        color = if (forcedWeather == value) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        stringResource(R.string.settings_weather_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AvatarSpecies.entries.forEach { species ->
                        AvatarSpeciesRow(
                            species = species,
                            selected = species == selectedSpecies,
                            onClick = {
                                // **Nur noch das Wesen.** Bis Phase 4b hing hier ein
                                // Neu-Anlegen der Vorgaben und ein komplettes Umplanen aller
                                // Alarme dran - der Wechsel tauschte ja den Erinnerungs-Satz
                                // aus. Jetzt gehoeren die Routinen dem Nutzer (siehe
                                // RoutineOwner), also gibt es an ihnen nichts zu tun: dieselben
                                // Erinnerungen laufen weiter, nur begleitet von jemand anderem.
                                //
                                // Das Pflegebuch des neuen Wesens ist dagegen leer, und das ist
                                // Absicht - es war bei nichts davon dabei (siehe
                                // PresentCompanion).
                                selectedSpecies = species
                                AvatarSpeciesPrefs.set(context, species)
                            }
                        )
                    }
                }

                // Nur sichtbar, wenn wirklich ein Absturz aufgezeichnet wurde - ein dauerhaft
                // sichtbarer Eintrag "Absturzbericht" waere ein Hinweis auf ein Problem, das es
                // in aller Regel gar nicht gibt.
                crashReport?.let { report ->
                    HorizontalDivider()
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            stringResource(R.string.settings_crash_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            stringResource(R.string.settings_crash_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, report)
                                        },
                                        null
                                    )
                                )
                            }) { Text(stringResource(R.string.settings_crash_share)) }
                            TextButton(onClick = {
                                CrashLog.clear(context)
                                crashReport = null
                            }) { Text(stringResource(R.string.action_delete)) }
                        }
                    }
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.settings_display),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.settings_allow_rotation))
                            Text(
                                stringResource(R.string.settings_allow_rotation_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = rotationAllowed,
                            onCheckedChange = { allowed ->
                                rotationAllowed = allowed
                                OrientationPrefs.setRotationAllowed(context, allowed)
                                // Wirkt sofort, ohne Neustart der App.
                                (context as? Activity)?.let { OrientationPrefs.apply(it, allowed) }
                            }
                        )
                    }
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.settings_dock_brightness),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        stringResource(R.string.settings_dock_brightness_explainer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.settings_use_custom_brightness))
                        Switch(
                            checked = overrideEnabled,
                            onCheckedChange = { checked ->
                                overrideEnabled = checked
                                DockBrightnessPrefs.setOverride(context, checked, brightness)
                            }
                        )
                    }
                    if (overrideEnabled) {
                        Slider(
                            value = brightness,
                            onValueChange = { value ->
                                brightness = value
                                DockBrightnessPrefs.setOverride(context, true, value)
                            },
                            valueRange = 0.02f..1f
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
        }
    )

    // Die Sammlung liegt ueber den Einstellungen, nicht daneben - sie gehoert zum selben Ort.
    if (showClipLibrary) {
        ClipLibraryDialog(
            clips = clips,
            onDismiss = { showClipLibrary = false },
            onChanged = { clipRefresh++ }
        )
    }
}

@Composable
private fun AvatarSpeciesRow(species: AvatarSpecies, selected: Boolean, onClick: () -> Unit) {
    val previewFrame = remember(species) { AvatarAnimations.idlePose(species) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .let { if (selected) it.background(MaterialTheme.colorScheme.primaryContainer) else it }
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AvatarSpriteView(frame = previewFrame, modifier = Modifier.size(48.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(species.labelRes), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(species.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun ClockStyleRow(style: ClockStyle, selected: Boolean, onClick: () -> Unit) {
    val previewFrame = remember(style) { ClockFrameSim.buildFrame(style = style) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .let { if (selected) it.background(MaterialTheme.colorScheme.primaryContainer) else it }
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SimulatedMatrixView(frame = previewFrame, modifier = Modifier.size(48.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(style.labelRes), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(style.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        RadioButton(selected = selected, onClick = onClick)
    }
}

/**
 * Die Sammlung: Filme und Schnappschuesse - teilen, in die Galerie legen, loeschen.
 *
 * **Beides in EINER Liste, nach Aufnahmezeit gemischt.** Zwei getrennte Listen waeren die
 * naheliegende Aufteilung und die falsche: Wer etwas sucht, erinnert sich daran, WANN er es
 * aufgenommen hat, nicht daran, ob er dabei den oberen oder den unteren Knopf gedrueckt hat. Ein
 * kleines Kennzeichen an der Zeile sagt, worum es sich handelt - das genuegt.
 *
 * Bewusst ohne Vorschaubilder. Bei den Filmen sagt ein Standbild aus wenigen Sekunden kaum etwas;
 * bei den Schnappschuessen waere eine Vorschau zwar aussagekraeftig, aber dann muesste die Liste
 * beim Oeffnen Dutzende Bilder in voller Aufloesung laden - fuer einen Einstellungsdialog eine
 * unangemessene Last.
 */
@Composable
private fun ClipLibraryDialog(
    clips: List<java.io.File>,
    onDismiss: () -> Unit,
    onChanged: () -> Unit
) {
    val context = LocalContext.current
    var savedTo by remember { mutableStateOf<String?>(null) }

    /*
     * Beim Oeffnen der Galerie Bruchstuecke abgebrochener Aufnahmen entfernen (Phase 6.1).
     *
     * Hier und nicht beim App-Start: an dieser Stelle ist es sichtbar folgenlos, und bis dahin
     * belegt ein Bruchstueck nur Platz. Siehe PlayClipRecorder.cleanPartials.
     */
    LaunchedEffect(Unit) {
        if (PlayClipRecorder.cleanPartials(context) > 0) onChanged()
    }

    val belegt = remember(clips) { ClipStorage.formatBytes(ClipStorage.usedBytes(clips)) }
    // 90 Sekunden ist die laengstmoegliche Aufnahme (siehe PlayClipRecorder.MAX_SECONDS).
    val platzKnapp = remember(clips) { !PlayClipRecorder.hasRoomForRecording(context, 90) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
        },
        title = { Text(stringResource(R.string.settings_library)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (clips.isEmpty()) {
                    Text(stringResource(R.string.settings_library_empty))
                } else {
                    // Belegter Platz - die App loescht nichts von allein, also muss der Nutzer
                    // sehen koennen, worueber er entscheidet.
                    Text(
                        stringResource(R.string.library_storage_used, belegt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (platzKnapp) {
                    Text(
                        stringResource(R.string.library_storage_full),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                clips.forEach { file ->
                    val isPicture = file.extension.equals("png", true)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            stringResource(
                                if (isPicture) R.string.library_snapshots else R.string.library_clips
                            ) + " · " +
                                java.text.DateFormat.getDateTimeInstance(
                                    java.text.DateFormat.SHORT, java.text.DateFormat.SHORT
                                ).format(java.util.Date(file.lastModified())),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { shareClip(context, file) }) {
                                Text(
                                    stringResource(
                                        if (isPicture) R.string.snapshot_share
                                        else R.string.settings_clip_share
                                    )
                                )
                            }
                            if (ClipGallery.isSupported) {
                                TextButton(onClick = {
                                    savedTo = if (ClipGallery.save(context, file)) file.name else null
                                }) {
                                    Text(
                                        if (savedTo == file.name) stringResource(R.string.clip_saved_gallery)
                                        else stringResource(R.string.clip_save_gallery)
                                    )
                                }
                            }
                            TextButton(onClick = {
                                file.delete()
                                onChanged()
                            }) { Text(stringResource(R.string.clip_delete)) }
                        }
                    }
                }
            }
        }
    )
}

/**
 * Reicht den fertigen Film an andere Apps weiter.
 *
 * Ueber einen FileProvider und NICHT ueber einen direkten Dateipfad: Seit Android 7 loest ein
 * `file://`-Verweis nach aussen eine Ausnahme aus. Die empfangende App bekommt eine befristete
 * Leseerlaubnis fuer genau diese eine Datei.
 */
private fun shareClip(context: android.content.Context, file: java.io.File) {
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.clips",
        file
    )
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                // Der Typ muss zum Inhalt passen: Mit "video/mp4" an einem Bild bieten viele
                // Apps das Teilen gar nicht erst an oder nehmen die Datei kommentarlos nicht an.
                type = if (file.extension.equals("png", true)) "image/png" else "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            null
        )
    )
}
