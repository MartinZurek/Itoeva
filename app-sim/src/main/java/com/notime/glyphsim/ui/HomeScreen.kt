package com.notime.glyphsim.ui

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.notime.glyphsim.R
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.data.CrashLog
import com.notime.glyphcore.data.GlyphReminderRepository
import com.notime.glyphcore.data.ReminderOpenDuration
import com.notime.glyphsim.matrix.AvatarAnimations
import com.notime.glyphsim.matrix.AvatarClip
import com.notime.glyphsim.matrix.AvatarMood
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.AvatarSpriteView
import com.notime.glyphsim.matrix.PlayClipRecorder
import com.notime.glyphsim.matrix.PlaySnapshot
import com.notime.glyphsim.matrix.PlayTimeLapse
import com.notime.glyphsim.matrix.ClockFrameSim
import com.notime.glyphsim.matrix.ClockStyle
import com.notime.glyphsim.matrix.MatrixAnimator
import com.notime.glyphsim.matrix.ReminderAnimationBus
import com.notime.glyphsim.matrix.SimulatedMatrixView
import com.notime.glyphcore.reminder.ReminderScheduler
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
    val libraryAnimationLabel: String?
)

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
    val species by AvatarSpeciesPrefs.selected(context).collectAsState()
    val currentSpecies = species ?: AvatarSpecies.PUFFLING

    // Fuetter-Zustand: die Uhr laesst sich auf den Avatar schieben, genau wie im Dock-Modus.
    var activeReminder by remember { mutableStateOf<ActiveReminder?>(null) }
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
    val playActive by PlayModePrefs.active(context).collectAsState(initial = PlayModePrefs.isActive(context))
    val playState by playViewModel.state.collectAsState()
    var showPlayIntro by remember { mutableStateOf(false) }

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
        activeReminder = ActiveReminder(event.reminderId, event.occurrenceId, event.animationType, event.libraryAnimationLabel)
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
        if (activeReminder != null) {
            activeReminder = null
            isPlayingAnimation = false
            animationFrame = null
        }
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
            activeReminder = ActiveReminder(event.reminderId, event.occurrenceId, event.animationType, event.libraryAnimationLabel)
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
            // auf null und cancelt diesen Job).
            if (activeReminder != null) {
                activeReminder = null
                isPlayingAnimation = false
                animationFrame = null
            }
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

    // Herausgeloest, damit dieselbe Logik sowohl von der Drag-Kollision unten als auch von der
    // TalkBack-Zusatzaktion "Fuettern" am Avatar ausgeloest werden kann (siehe AvatarSpriteView
    // weiter unten) - per Drag laesst sich nicht sinnvoll fuer einen Screenreader bedienen.
    fun feedNow() {
        val current = activeReminder ?: return
        activeReminder = null
        clockAnimJob?.cancel()
        isPlayingAnimation = false
        animationFrame = null
        isReacting = true

        scope.launch(Dispatchers.IO) {
            AvatarFeeding.logFeedEvent(context, current.occurrenceId)
        }
        scope.launch {
            try {
                AvatarFeeding.playReaction(
                    species = currentSpecies,
                    animationType = current.animationType,
                    libraryAnimationLabel = current.libraryAnimationLabel,
                    screenWidthPx = screenWidthPx,
                    screenHeightPx = screenHeightPx,
                    onFrame = { avatarFrame = it },
                    onOffset = { avatarDrag = it }
                )
            } finally {
                // Zwingend im finally: isReacting blendet seit Neuestem die Uhr aus. Bliebe das
                // Flag nach einem Abbruch (Spezies gewechselt, Fehler in der Reaktion) auf true
                // stehen, waere die Uhr bis zum naechsten App-Start verschwunden - aus einem
                // kurzen Aussetzer wuerde ein dauerhaft kaputter Startbildschirm.
                moodRefresh++
                avatarDrag = Offset.Zero
                clockDrag = Offset.Zero
                isReacting = false
            }
        }
    }

    // Kollisions-Erkennung waehrend des Ziehens (laeuft bei jeder Bewegung neu an, nicht erst
    // am Gesten-Ende) - die Reaktion selbst laeuft ueber [scope], damit sie ungestoert zu Ende
    // spielt, auch wenn der Finger danach noch bewegt wird.
    LaunchedEffect(clockDrag) {
        if (activeReminder == null) return@LaunchedEffect
        if (!AvatarFeeding.overlaps(clockBounds, avatarBounds)) return@LaunchedEffect
        feedNow()
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

    val mealEntries = rememberMealEntries(currentSpecies, playMode = playActive == true)

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            Column {
                TopAppBar(
                    // Statt des blossen App-Namens steht hier jetzt der Tagesstand: welche
                    // Erinnerungen heute schon beantwortet wurden, und wo ein Tagesziel gesetzt ist
                    // auch wie weit es erfuellt ist. Der Name sagte nichts, was man nicht ohnehin
                    // wuesste - dieser Platz ist die einzige Stelle, an der der Stand dauerhaft
                    // sichtbar sein kann, ohne dem Avatar unten Raum wegzunehmen.
                    //
                    // Feste, niedrigere Inhaltshoehe (statt der M3-Standardhoehe von 64dp): der
                    // Inhalt (eine einzeilige Chip-Reihe) ist viel kuerzer als das, und mittig
                    // zentriert liess das eine grosse Luecke zur [MealOverflowRow] darunter
                    // entstehen. expandedHeight statt Modifier.height: Letzteres legt die
                    // GESAMThoehe inklusive Status-Bar-Inset fest, wodurch dem Inhalt darunter
                    // nichts mehr blieb und die Zeile ganz verschwand. Bei 40dp bleibt das
                    // Zahnrad-Symbol (24dp) unbeschnitten - nur seine Antippflaeche wird etwas
                    // kleiner als die vollen 48dp.
                    expandedHeight = 40.dp,
                    title = {
                        MealStrip(
                            entries = mealEntries.take(MAX_STRIP_ENTRIES_TOP),
                            onClick = { showStats = true }
                        )
                    },
                    actions = {
                        // Spielmodus an/aus. Bewusst hier neben "Erinnerungen" und nicht im
                        // Avatar-Menue versteckt: Es ist der Schalter, der bestimmt, WELCHE
                        // Erinnerungen ueberhaupt kommen - er gehoert an dieselbe Stelle wie sie
                        // und muss jederzeit sichtbar sein, damit man nie im Unklaren ist, in
                        // welchem der beiden Modi die App gerade laeuft.
                        TextButton(
                            onClick = {
                                if (playActive == true) {
                                    playViewModel.setActive(false)
                                } else if (PlayModePrefs.hasSeenIntro(context)) {
                                    playViewModel.setActive(true)
                                } else {
                                    // Beim ersten Mal erst erklaeren, eingeschaltet wird dann im
                                    // Dialog - sonst aendert sich unvermittelt, was die App tut.
                                    showPlayIntro = true
                                }
                            }
                        ) {
                            Text(
                                stringResource(
                                    if (playActive == true) R.string.home_play_off else R.string.home_play_on
                                ),
                                color = if (playActive == true) Color(0xFF7FD1A6) else Color(0xFFE8E4DA)
                            )
                        }
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
                val overflowMealEntries = mealEntries.drop(MAX_STRIP_ENTRIES_TOP)
                if (overflowMealEntries.isNotEmpty()) {
                    MealOverflowRow(
                        entries = overflowMealEntries,
                        onClick = { showStats = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black)
                            .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 6.dp)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Schwarzer Grund wie im Dock-Modus: Uhr und Avatar sind Lichtpunkte auf dunklem
                // Untergrund. Auf der hellen Themefarbe brauchten beide eine eigene dunkle
                // Flaeche, um ueberhaupt lesbar zu sein - und genau diese Flaechen wirkten als
                // Kasten bzw. Kreis um die Figuren und beschnitten deren Animationen. Mit
                // schwarzem Hintergrund entfaellt der Grund dafuer, beide duerfen frei stehen.
                .background(Color.Black)
                .padding(padding)
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
                    color = Color(0xFFF1EEE6),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp, topEnd = 16.dp,
                                bottomEnd = 16.dp, bottomStart = 4.dp
                            )
                        )
                        .background(Color(0xFF1E1E22))
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
                                detectTapGestures(
                                    onTap = {
                                        if (!clockTapped) {
                                            clockTapped = true
                                            OnboardingPrefs.markClockTapped(context)
                                        }
                                        onEnterDockMode()
                                    }
                                )
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
                    color = Color(0xFFF1EEE6),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp, topEnd = 16.dp,
                                bottomEnd = 16.dp, bottomStart = 4.dp
                            )
                        )
                        .background(Color(0xFF1E1E22))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
                Spacer(Modifier.height(10.dp))
            }

            // Level/XP stehen bewusst NICHT hier, sondern nur im Dock-Modus (siehe DockScreen):
            // Der Startbildschirm ist der Ort zum Einrichten und Nachsehen, nicht die Spielflaeche
            // - eine Fortschrittszeile zwischen Uhr und Avatar war schlicht im Weg.

            // Der gewaehlte Avatar - nicht nur Deko: er bestimmt, WELCHER Erinnerungs-Satz
            // gerade gilt (jeder Avatar hat seinen eigenen), und er ist hier - wie im
            // Dock-Modus - das Ziel der Fuetter-Geste.
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
    }

    if (showPlayIntro) {
        PlayModeIntroDialog(
            species = currentSpecies,
            onStart = {
                PlayModePrefs.markIntroSeen(context)
                showPlayIntro = false
                playViewModel.setActive(true)
            },
            onDismiss = { showPlayIntro = false }
        )
    }

    if (showAssistant) {
        AvatarAssistantDialog(
            species = currentSpecies,
            onOpenSettings = {
                showAssistant = false
                showSettings = true
            },
            onDismiss = { showAssistant = false },
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
 * Ermittelt den Tagesstand: je Erinnerung, auf die heute reagiert wurde, ihr Symbol und die
 * Anzahl - bei gesetztem Tagesziel als "erreicht/Ziel".
 *
 * Von der Anzeige getrennt (siehe [MealStrip]/[MealOverflowRow]), weil bei vielen aktiven
 * Zielen nicht mehr alles in die Kopfzeile passt und der Rest in einer zweiten Zeile
 * darunter weitergeht - beide brauchen dieselbe Liste, nur unterschiedlich aufgeteilt.
 *
 * Zeigt bewusst nur, was heute schon passiert ist - eine Liste aller Erinnerungen mit lauter
 * Nullen waere eine Mahnung, keine Auskunft.
 *
 * **Spielmodus und Normalbetrieb fuehren getrennt Buch** ([playMode]): Frueher stand hier beides
 * vermischt, also die eigenen Tagesziele neben zufaelligen Spiel-Fuetterungen - eine Zeile, die
 * dadurch weder ueber das eine noch ueber das andere Auskunft gab. Jetzt zeigt die Leiste immer
 * genau den Modus, in dem man gerade ist, und wechselt mit ihm.
 */
@Composable
private fun rememberMealEntries(species: AvatarSpecies, playMode: Boolean): List<MealEntry> {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val profileId = remember(species) { AvatarSpeciesPrefs.profileId(species) }
    val todayStart = FeedStatsPeriod.TODAY.startMillis()

    if (playMode) return rememberPlayMealEntries(db, profileId, todayStart)

    val reminders by remember(profileId) {
        db.glyphReminderDao().observeForProfile(profileId)
    }.collectAsState(initial = emptyList())
    val fedToday by remember(profileId, todayStart) {
        db.avatarFeedEventDao().observeFedPerReminderSince(profileId, todayStart, isPlayMode = false)
    }.collectAsState(initial = emptyList())

    return remember(reminders, fedToday) {
        val fedByReminder = fedToday.associate { it.reminderId to it.count }
        reminders
            // Die Spiel-Zeile gehoert nicht in den Normalbetrieb - sie ist keine Erinnerung des
            // Nutzers (siehe GlyphReminderViewModel.reminders).
            .filter { it.enabled && !it.isPlayMode }
            .mapNotNull { reminder ->
                val fed = fedByReminder[reminder.id] ?: 0
                // Ohne Reaktion und ohne Ziel gibt es nichts zu berichten.
                if (fed == 0 && reminder.dailyGoal == 0) return@mapNotNull null
                MealEntry(
                    emoji = animationVisuals[reminder.animationType]?.emoji ?: "✨",
                    fed = fed,
                    goal = reminder.dailyGoal
                )
            }
    }
}

/**
 * Der Tagesstand im Spielmodus: nach THEMA gruppiert statt nach Erinnerung, weil es dort nur eine
 * einzige Erinnerungs-Zeile mit wechselndem Thema gibt (siehe
 * [com.notime.glyphsim.data.AvatarFeedEventDao.observePlayFedPerTypeSince]).
 *
 * Ohne Tagesziele: Das Spiel gibt keine vor - gezeigt wird schlicht, was der Avatar heute schon
 * bekommen hat.
 */
@Composable
private fun rememberPlayMealEntries(
    db: AppDatabase,
    profileId: String,
    todayStart: Long
): List<MealEntry> {
    val fedToday by remember(profileId, todayStart) {
        db.avatarFeedEventDao().observePlayFedPerTypeSince(profileId, todayStart)
    }.collectAsState(initial = emptyList())

    return remember(fedToday) {
        fedToday.map { row ->
            MealEntry(
                emoji = animationVisuals[row.animationType]?.emoji ?: "✨",
                fed = row.count,
                goal = 0
            )
        }
    }
}

@Composable
private fun MealChip(entry: MealEntry) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(entry.emoji, style = MaterialTheme.typography.bodyMedium)
        Text(
            if (entry.goal > 0) "${entry.fed.coerceAtMost(entry.goal)}/${entry.goal}" else "${entry.fed}",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            // Erreichtes Ziel gruen - derselbe Hinweis wie in der Auswertung.
            color = if (entry.goal > 0 && entry.fed >= entry.goal) {
                Color(0xFF63C88A)
            } else {
                Color(0xFFE8E4DA)
            }
        )
    }
}

/**
 * Tagesstand in der Kopfzeile - ersetzt die frueher unter dem Avatar stehende Zaehlkarte. Der
 * Platz dort wird gebraucht: der Avatar soll sich bewegen koennen (die Rocket-Reaktion fliegt
 * quer ueber den Bildschirm), und eine reine Gesamtzahl sagte ohnehin weniger als die
 * Aufschluesselung nach Erinnerung.
 *
 * Zeigt nur die ersten [MAX_STRIP_ENTRIES_TOP] Eintraege - alles darueber hinaus laeuft in
 * [MealOverflowRow] weiter, direkt unter der Kopfzeile. Antippen oeffnet die vollstaendige
 * Auswertung.
 */
@Composable
private fun MealStrip(entries: List<MealEntry>, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (entries.isEmpty()) {
            Text(
                stringResource(R.string.stats_strip_empty),
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF9A968E)
            )
        } else {
            entries.forEach { entry -> MealChip(entry) }
        }
    }
}

/**
 * Fortsetzung von [MealStrip]: alles, was in der Kopfzeile neben "Erinnerungen"-Button und
 * Zahnrad nicht mehr nebeneinander passt, laeuft hier in einer zweiten Zeile weiter - als
 * FlowRow, damit auch eine dritte Zeile entstuende, sollten es noch mehr Ziele werden.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MealOverflowRow(entries: List<MealEntry>, onClick: () -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        entries.forEach { entry -> MealChip(entry) }
    }
}

private data class MealEntry(val emoji: String, val fed: Int, val goal: Int)

/** So viele passen zuverlaessig neben "Erinnerungen"-Button und Zahnrad in die Kopfzeile - der
 *  Rest geht in [MealOverflowRow] darunter weiter. */
private const val MAX_STRIP_ENTRIES_TOP = 4

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
    var lapseSpeed by remember { mutableStateOf(PlayTimeLapse.speed()) }
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
                    AvatarSpecies.entries.forEach { species ->
                        AvatarSpeciesRow(
                            species = species,
                            selected = species == selectedSpecies,
                            onClick = {
                                selectedSpecies = species
                                AvatarSpeciesPrefs.set(context, species)
                                // Alarme des vorherigen Avatars abbestellen, die des neuen
                                // planen - sonst wuerden weiter die alten feuern.
                                scope.launch {
                                    AppDatabase.getInstance(context).let { db ->
                                        GlyphReminderRepository(db.glyphReminderDao())
                                            .seedIfEmpty(AvatarSpeciesPrefs.profileId(species))
                                    }
                                    ReminderScheduler.rescheduleAll(context)
                                }
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
