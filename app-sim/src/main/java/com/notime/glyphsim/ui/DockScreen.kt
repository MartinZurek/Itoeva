package com.notime.glyphsim.ui

import android.os.SystemClock
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.ReminderOpenDuration
import com.notime.glyphsim.R
import com.notime.glyphsim.data.AvatarFeedEvent
import com.notime.glyphsim.matrix.AvatarAnimations
import com.notime.glyphsim.matrix.AvatarBodies
import com.notime.glyphsim.matrix.AvatarFooting
import com.notime.glyphsim.matrix.AvatarGeometry
import com.notime.glyphsim.matrix.AvatarMood
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.AvatarSpriteView
import com.notime.glyphsim.matrix.MatrixAnimator
import com.notime.glyphsim.matrix.MoonFrame
import com.notime.glyphsim.matrix.PlayAmbientActivity
import com.notime.glyphsim.matrix.PlayClipRecorder
import com.notime.glyphsim.matrix.PlayClipRenderer
import com.notime.glyphsim.matrix.PlayEffects
import com.notime.glyphsim.matrix.PlayPantry
import com.notime.glyphsim.matrix.PlayRoutine
import com.notime.glyphsim.matrix.PlayRoutines
import com.notime.glyphsim.matrix.PlayChime
import com.notime.glyphsim.matrix.PlayScene
import com.notime.glyphsim.matrix.CompanionChapter
import com.notime.glyphsim.matrix.PlayWeather
import com.notime.glyphsim.matrix.PlayFootballSkill
import com.notime.glyphsim.matrix.PlaySceneView
import com.notime.glyphsim.matrix.PlaySnapshot
import com.notime.glyphsim.matrix.PlayTimeLapse
import com.notime.glyphsim.matrix.PlayWallet
import com.notime.glyphsim.matrix.ReminderAnimationBus
import com.notime.glyphsim.matrix.RoutineStep
import com.notime.glyphsim.matrix.SimulatedMatrixView
import com.notime.glyphsim.matrix.groundRow
import java.time.LocalTime
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reiner Dock-Bildschirm: nichts als schwarze Flaeche und die Uhr - Groesse und
 * Position frei einstellbar (ein Finger auf der Uhr = ziehen, zwei Finger = Groesse
 * aendern) und in [DockLayoutPrefs] gespeichert, damit beides beim naechsten
 * Andocken erhalten bleibt statt jedes Mal neu eingestellt werden zu muessen.
 * Verschieb-/Skalierbarkeit dient zusaetzlich dem Burn-in-Schutz auf OLED-Displays,
 * da die Uhr hier dauerhaft leuchten wuerde. Tippen auf die Uhr beendet den
 * Dock-Modus wieder, symmetrisch zum Einstieg per Tippen auf die Uhr im HomeScreen.
 *
 * Faellige Erinnerungen kommen ueber [ReminderAnimationBus] rein (siehe dort fuer
 * die Begruendung - das Home-Screen-Widget bekommt sie sonst direkt, In-App-Views
 * gar nicht) und unterbrechen kurz den Uhr-Tick, genau wie der Uhr-Tick im
 * :app-Modul waehrend einer Hardware-Animation pausiert (siehe
 * GlyphMatrixConnection.isAnimationPlaying im README).
 *
 * **Tamagotchi-Avatar**: solange die Erinnerungs-Animation auf der Uhr laeuft, taucht
 * zusaetzlich ein kleiner Avatar an einer zufaelligen Stelle auf (idle-animiert, siehe
 * [AvatarAnimations.idleFrames]) - anders als die Uhr KEIN runder Puck, sondern ein
 * eckiges Pixel-Sprite ([AvatarSpriteView]) in derselben Groesse wie die Uhr in ihrer
 * Grundeinstellung ([DockLayoutPrefs.DEFAULT_SIZE_DP]). Schiebt der User die Uhr per
 * Drag-Geste auf den Avatar (Kollision ueber Bounding-Box-Overlap, siehe [isColliding]),
 * "friss" der Avatar die Erinnerung: er spielt seine typ-spezifische Reaktion
 * ([AvatarAnimations.reactionFramesFor]) einmal komplett durch ([MatrixAnimator.playOnce])
 * und verschwindet danach, waehrend die Uhr sofort wieder normal tickt. Der Feed-Vorgang
 * wird als [AvatarFeedEvent] geloggt - Grundlage fuer ein spaeter separat zu definierendes
 * Punktesystem, hier bewusst nur Rohdaten ohne Bepunktung. Laeuft die Animation aus, ohne
 * dass die Uhr den Avatar erreicht hat, verschwindet er einfach ungetrackt.
 */
@Composable
fun DockScreen(
    onExit: () -> Unit,
    /**
     * Play-on-Modus statt normalem Dock: derselbe Bildschirm, aber die auftauchenden Erinnerungen
     * stammen aus dem Spiel (siehe [PlayModeViewModel]) und jede Fuetterung zahlt auf XP ein.
     *
     * **Warum derselbe Bildschirm und kein eigener:** Was der Spielmodus braucht - Avatar taucht
     * an zufaelliger Stelle auf, Uhr draufziehen fuettert ihn, Reaktion spielt ab - ist exakt das,
     * was der Dock-Modus laengst tut, bis hin zum Nachholen einer beim Eintritt schon laufenden
     * Erinnerung. Ein zweiter Bildschirm waere eine Kopie dieser gesamten Mechanik gewesen, die
     * bei jeder kuenftigen Aenderung mitgepflegt werden muesste. Unterschiedlich ist allein die
     * Fortschrittsanzeige.
     */
    playMode: Boolean = false,
    /**
     * **Nur die Uhr** - kein Wesen, keine Wohnung, keine Erinnerung (siehe [WatchModePrefs]).
     *
     * Als eigener Wert und nicht als "playMode = false": Der Normalbetrieb zeigt das Wesen sehr
     * wohl, sobald eine Erinnerung faellig ist. Hier bleibt der Bildschirm unter allen Umstaenden
     * bei der Uhr - das ist der ganze Zweck.
     */
    watchOnly: Boolean = false,
    /**
     * Legt aus dem Gespraech heraus eine Gewohnheit an (siehe [PlayTalk.presetFor]).
     *
     * Als Rueckruf und nicht hier erledigt: Eine Erinnerung anzulegen heisst, sie auch zu PLANEN
     * ([com.notime.glyphcore.reminder.ReminderScheduler]), und das gehoert zum
     * [GlyphReminderViewModel], der die Erinnerungen ohnehin verwaltet. Der Dock-Bildschirm zeigt
     * eine Welt; er sollte nicht nebenbei anfangen, Wecker zu stellen.
     */
    onAddHabit: (AnimationType) -> Unit = {},
    /** Eine bestehende Erinnerung so aendern, wie der Begleiter es vorschlaegt. */
    onAdjustHabit: (com.notime.glyphcore.data.GlyphReminder) -> Unit = {},
    /** Wechselt zur Erinnerungsliste - aus dem Gespraech heraus verlinkt. */
    onOpenReminders: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Zwei getrennte Quellen statt einer gemeinsam beschriebenen `frame`-Variable: die Uhr tickt
    // selbstaendig weiter (pausiert waehrend einer Animation, siehe rememberClockFrame), die
    // Erinnerungs-Animation legt sich als animationFrame darueber. Beim Abbruch/Ende reicht
    // animationFrame = null, um sofort wieder die aktuelle Uhrzeit zu zeigen - vorher musste
    // dafuer an drei Stellen der Uhr-Frame von Hand neu gebaut werden.
    var isPlayingAnimation by remember { mutableStateOf(false) }
    var animationFrame by remember { mutableStateOf<IntArray?>(null) }
    val clockFrame by rememberClockFrame(paused = isPlayingAnimation)
    // Mond-Szene: In der Park-Nacht wird die Uhr zur Sichel und steigt in den Himmel. Bewusst
    // NICHT jede Nacht - eine Ausnahme, die jedes Mal kaeme, waere keine mehr.
    var moonMode by remember { mutableStateOf(false) }
    var moonPhase by remember { mutableIntStateOf(0) }
    val frame = animationFrame ?: if (moonMode) MoonFrame.build(moonPhase) else clockFrame

    var clockSizeDp by remember { mutableFloatStateOf(DockLayoutPrefs.getSizeDp(context)) }
    val initialFraction = remember { DockLayoutPrefs.getOffsetFraction(context) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val density = LocalDensity.current
        val initialClockPx = with(density) { clockSizeDp.dp.toPx() }
        val initialMaxXPx = (with(density) { maxWidth.toPx() } - initialClockPx).coerceAtLeast(0f)
        val initialMaxYPx = (with(density) { maxHeight.toPx() } - initialClockPx).coerceAtLeast(0f)
        var clockOffset by remember {
            mutableStateOf(Offset(initialFraction.first * initialMaxXPx, initialFraction.second * initialMaxYPx))
        }

        // Debounce: nur 300ms nach der letzten Aenderung schreiben, statt bei jedem
        // Geste-Frame - LaunchedEffect bricht die vorherige Wartezeit automatisch ab,
        // sobald sich einer der Keys (Groesse/Position) erneut aendert.
        LaunchedEffect(clockSizeDp, clockOffset) {
            delay(300L)
            // Waehrend der Mond-Szene steht die Uhr am Himmel und nicht dort, wo der Nutzer sie
            // haben will - diese Position darf auf keinen Fall die gemerkte ueberschreiben.
            if (moonMode) return@LaunchedEffect
            val clockPxNow = with(density) { clockSizeDp.dp.toPx() }
            val boundX = (with(density) { maxWidth.toPx() } - clockPxNow).coerceAtLeast(0f)
            val boundY = (with(density) { maxHeight.toPx() } - clockPxNow).coerceAtLeast(0f)
            val fractionX = if (boundX > 0f) (clockOffset.x / boundX).coerceIn(0f, 1f) else 0.5f
            val fractionY = if (boundY > 0f) (clockOffset.y / boundY).coerceIn(0f, 1f) else 0.5f
            DockLayoutPrefs.save(context, clockSizeDp, fractionX, fractionY)
        }

        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }

        // Fuer die Zieh-Geste der Uhr, die nur einmal startet und die Werte sonst einfrieren
        // wuerde - siehe die ausfuehrliche Begruendung dort.
        val currentWidthPx = rememberUpdatedState(maxWidthPx)
        val currentHeightPx = rememberUpdatedState(maxHeightPx)

        // **Auch die Uhr muss beim Drehen mitkommen.**
        //
        // Ihre Position steht in Pixeln und wird beim ersten Aufbau einmal aus dem gemerkten
        // Bruchteil gerechnet. Dreht man das Geraet, tauschen Breite und Hoehe die Rollen - der
        // Pixelwert bleibt aber stehen. Im Querformat lag die Uhr dadurch unter dem unteren Rand,
        // und schlimmer: Das verzoegerte Sichern rechnete den alten Pixelwert gegen die NEUEN
        // Masse zurueck und schrieb damit eine falsche Position fest. Der Fehler ueberlebte also
        // das Zurueckdrehen.
        //
        // Hier wird derselbe Bruchteil auf die neuen Masse angewandt: Die Uhr bleibt anteilig
        // dort, wo der Nutzer sie hingelegt hat. Ohne Verzoegerung, damit dieser Effekt sicher
        // vor dem Sichern laeuft.
        //
        // **Ausschliesslich auf die BILDSCHIRMMASSE hoerend - und das ist entscheidend.**
        //
        // Die erste Fassung hoerte auch auf die Uhrgroesse, und damit war das Ziehen der Uhr
        // kaputt: Die Zieh-Geste erkennt Verschieben und Zoomen zusammen und schreibt bei JEDER
        // Bewegung eine neue Uhrgroesse - beim reinen Verschieben zwar praktisch dieselbe, aber
        // eben eine neue. Dieser Effekt lief dadurch waehrend des Ziehens staendig mit und setzte
        // die Uhr auf ihren alten Bruchteil zurueck. Sie liess sich noch ein Stueck bewegen und
        // wurde sofort zurueckgezogen - von aussen genau das, was gemeldet wurde: eine Grenze,
        // die man nicht ueberschreiten kann, und ein Avatar, den man nicht mehr fuettern konnte.
        //
        // Die Uhrgroesse braucht diesen Effekt auch gar nicht: Die Geste klemmt beim Zoomen
        // bereits selbst gegen die neuen Grenzen.
        var lastScreenWidthPx by remember { mutableStateOf(0f) }
        var lastScreenHeightPx by remember { mutableStateOf(0f) }
        LaunchedEffect(maxWidthPx, maxHeightPx) {
            val previousWidth = lastScreenWidthPx
            val previousHeight = lastScreenHeightPx
            lastScreenWidthPx = maxWidthPx
            lastScreenHeightPx = maxHeightPx

            // Waehrend der Mond-Szene steht die Uhr am Himmel und nicht dort, wo der Nutzer sie
            // haben will - dieselbe Ruecksicht wie beim Sichern.
            if (moonMode || previousWidth <= 0f || previousHeight <= 0f) return@LaunchedEffect

            // Mit der AKTUELLEN Uhrgroesse gegen die ALTEN Bildschirmmasse zurueckgerechnet: Es
            // hat sich ja nur das Bild gedreht, nicht die Uhr.
            val clockPx = with(density) { clockSizeDp.dp.toPx() }
            val previousX = (previousWidth - clockPx).coerceAtLeast(0f)
            val previousY = (previousHeight - clockPx).coerceAtLeast(0f)
            val boundX = (maxWidthPx - clockPx).coerceAtLeast(0f)
            val boundY = (maxHeightPx - clockPx).coerceAtLeast(0f)

            val fractionX = if (previousX > 0f) (clockOffset.x / previousX).coerceIn(0f, 1f) else 0.5f
            val fractionY = if (previousY > 0f) (clockOffset.y / previousY).coerceIn(0f, 1f) else 0.5f
            clockOffset = Offset(boundX * fractionX, boundY * fractionY)
        }

        // Ort und dargestellter Ort stehen vor der Geometrie, die beide fuer den Szenenaufbau
        // benoetigt.
        val presenceProfileId = PresentCompanion.profileId(context)
        val initialPresence = remember(presenceProfileId) {
            PlayPresence.entry(context, presenceProfileId)
        }
        var currentPlace by remember(presenceProfileId) { mutableStateOf(initialPresence.place) }
        var renderedPlace by remember(presenceProfileId) { mutableStateOf(initialPresence.place) }

        // ---- Geometrie der Lebenswelt (nur Play-Modus, siehe PlayScene) ----
        //
        // Die Kulisse teilt sich Zellraster UND Zellgroesse mit dem Avatar-Sprite: beides aus
        // derselben Avatar-Groesse abgeleitet, damit Figur und Welt sichtbar zur selben
        // Pixelwelt gehoeren. Eine eigene Kulissen-Zellgroesse waere sofort als zwei
        // uebereinandergelegte Grafiken aufgefallen.
        val worldAvatarSizeDp = (clockSizeDp * AVATAR_TO_CLOCK_RATIO)
            .coerceIn(DockLayoutPrefs.MIN_SIZE_DP, DockLayoutPrefs.DEFAULT_SIZE_DP)
        val worldAvatarPx = with(density) { worldAvatarSizeDp.dp.toPx() }
        // Die Zelle wird notfalls kleiner, damit die Szene nie unter PlayScene.MIN_SCENE_CELLS
        // Spalten faellt - sonst stehen auf einem kleinen Geraet mit gross gezogener Uhr Sofa,
        // Fernseher und Tuer zwangslaeufig ineinander. Die Figur schrumpft dann ein wenig mit;
        // das ist der guenstigere Tausch, siehe die Begruendung an der Konstanten.
        val sceneCellPx = minOf(
            worldAvatarPx / AvatarGeometry.SIZE,
            if (maxWidthPx > 0f) maxWidthPx / PlayScene.MIN_SCENE_CELLS else Float.MAX_VALUE
        )
        val sceneWidthCells = if (sceneCellPx > 0f) (maxWidthPx / sceneCellPx).toInt() else 0
        // Alle Orte teilen dieselbe Ebene. Ein Ortswechsel darf die Welt nicht unter der Figur
        // anheben oder absenken; geplante Hoehenwege brauchen spaeter eine eigene Mechanik.
        val floorFraction = PlayScene.floorFraction(renderedPlace, PlayAmbientActivity.currentDayPhase())
        val floorYCells = if (sceneCellPx > 0f) {
            (maxHeightPx * floorFraction / sceneCellPx).toInt()
        } else {
            0
        }
        val floorYPx = floorYCells * sceneCellPx
        /**
         * Derselbe Wert, aber fuer laufende Bewegungen lesbar.
         *
         * **Warum das noetig ist.** Ein Gang laeuft ueber eine knappe Sekunde in einer Coroutine.
         * Die hat den Boden beim Start eingefangen - und wenn er sich waehrenddessen bewegt (raus
         * in den Park, wo er tiefer liegt), laeuft die Figur auf einer Hoehe weiter, die es nicht
         * mehr gibt. Genau das war als "er schwebt in der Luft" gemeldet. [rememberUpdatedState]
         * gibt der laufenden Bewegung den jeweils aktuellen Stand, ohne sie neu zu starten.
         */
        val floorYPxNow by rememberUpdatedState(floorYPx)

        var avatar by remember { mutableStateOf<AvatarState?>(null) }
        var feedingOccurrenceId by remember { mutableStateOf<Long?>(null) }

        // Vier feste Speicherplaetze, nur im Spielmodus (siehe ActionSlots.kt/ActionSlotStore.kt) -
        // dieselbe Mechanik wie in HomeScreen, hier auf DockScreens eigenes Koordinatensystem
        // (Pixel-Offsets statt onGloballyPositioned/Rect) und Avatar-Modell (AvatarState statt
        // ActiveReminder) uebertragen. profileId folgt demselben Ad-hoc-Muster wie andernorts in
        // dieser Datei (siehe z. B. weiter unten bei PlayWallet/PlayPantry): avatar?.species, falls
        // schon vorhanden, sonst der gespeicherte Auswahlwert.
        val actionSlotProfileId = AvatarSpeciesPrefs.profileId(avatar?.species ?: AvatarSpeciesPrefs.get(context))
        var slots by remember(actionSlotProfileId) {
            mutableStateOf(ActionSlotStore.read(context, actionSlotProfileId))
        }

        // Feste Position rechts, vertikal zentriert - reines Pixel-Offset/Groessen-Paar wie
        // clockOffset/avatar.offset, damit sich [isColliding] unveraendert wiederverwenden laesst.
        val slotSizePx = with(density) { 56.dp.toPx() }
        val slotGapPx = with(density) { 14.dp.toPx() }
        val slotsRightMarginPx = with(density) { 8.dp.toPx() }
        val slotsTotalHeightPx = ACTION_SLOT_COUNT * slotSizePx + (ACTION_SLOT_COUNT - 1) * slotGapPx
        val slotsTopPx = ((maxHeightPx - slotsTotalHeightPx) / 2f).coerceAtLeast(0f)
        val slotsXPx = (maxWidthPx - slotSizePx - slotsRightMarginPx).coerceAtLeast(0f)
        fun slotOffsetPx(index: Int) = Offset(slotsXPx, slotsTopPx + index * (slotSizePx + slotGapPx))
        /** Ob gerade ein Gang laeuft - siehe das Nachfuehren des Bodens weiter unten. */
        var avatarWalking by remember { mutableStateOf(false) }
        /** Wie oft die Figur schon hintereinander im selben Raum geblieben ist - siehe nextTopic. */
        var stayedRounds by remember { mutableStateOf(0) }
        /** Womit die Figur zuletzt beschaeftigt war - damit sie im Gespraech sagen kann, was sie tut. */
        var currentTopic by remember(presenceProfileId) {
            mutableStateOf<AnimationType?>(initialPresence.topic)
        }
        /** Wer zuletzt zu Besuch da war - damit er im Gespraech davon erzaehlen kann. */
        var lastVisitor by remember { mutableStateOf<AvatarSpecies?>(null) }
        /** Was er gerade ueber dem Kopf sagt (Text-Id), oder null - siehe PlaySpeech. */
        var spokenLine by remember { mutableStateOf<Int?>(null) }
        /** Ob dazu der Halbsatz zu einer heute offenen Gewohnheit gehoert. */
        var spokenIsOpenHabit by remember { mutableStateOf(false) }
        /**
         * Ob gerade ein Tagesablauf laeuft - siehe den Besuchstakt.
         *
         * Ein Besuch uebernimmt die Bilder der Figur (er laesst sie zuhoeren und antworten) und
         * wuerde sich mit einem laufenden Ablauf um dieselbe Figur streiten: Sie ginge zum Bett,
         * waehrend sie sich unterhaelt. Besuch kommt deshalb in den Pausen DAZWISCHEN - was
         * ohnehin die ehrlichere Lesart ist.
         */
        var routineRunning by remember { mutableStateOf(false) }
        /**
         * Ob die Figur sich gerade HINEINSETZT oder aufsteht (siehe [RoutineStep.Occupy]).
         *
         * Dasselbe wie [avatarWalking], nur senkrecht - und aus demselben Grund noetig: Waehrend
         * diese Bewegung laeuft, schreibt sie die Position Bild fuer Bild. Faehrt der Boden in
         * genau diesem Moment nach (Ortswechsel nach draussen), zieht das Nachfuehren die Figur
         * zurueck auf den Boden, waehrend die Bewegung sie hinaufschiebt - sichtbar als Zucken
         * oder als Schweben auf halber Hoehe. Der Zustand [occupiedStation] taugt dafuer nicht:
         * Er wird erst gesetzt, wenn die Bewegung fertig ist, und genau dazwischen liegt die
         * Luecke.
         */
        var avatarSettling by remember { mutableStateOf(false) }
        var clockAnimJob by remember { mutableStateOf<Job?>(null) }
        var avatarIdleJob by remember { mutableStateOf<Job?>(null) }
        // Rein optische Verschiebung waehrend langer Erinnerungen (Burn-in-Schutz, siehe unten) -
        // bewusst getrennt von clockOffset, damit die gespeicherte Nutzerposition unberuehrt bleibt.
        var driftOffset by remember { mutableStateOf(Offset.Zero) }
        var burnInDriftJob by remember { mutableStateOf<Job?>(null) }

        // Wo der Avatar sich gerade aufhaelt. Bewusst KEIN eigenstaendiger Zustand, der gepflegt
        // werden muesste: [currentPlace] wird ausschliesslich aus der Taetigkeit abgeleitet
        // (siehe PlayScene.forTopic in der Regungs-Schleife unten). Dadurch kann die Kulisse
        // nie etwas anderes zeigen als das, was der Avatar gerade tut.
        //
        // [renderedPlace] hinkt [currentPlace] absichtlich hinterher: erst blendet die alte
        // Kulisse ab, dann wird gewechselt, dann blendet die neue auf ([sceneFade]). Ein harter
        // Schnitt mitten im Bild saehe nach Zeichenfehler aus, ein Uebergang nach Ortswechsel.
        // Welchen Platz die Figur gerade BENUTZT (im Bett liegt, im Sessel sitzt) - steuert
        // ausschliesslich, ob die vorderen Teile dieser Requisite ueber ihr gezeichnet werden
        // (siehe PlayScene.buildFront). Null heisst: steht frei auf dem Boden.
        var occupiedStation by remember { mutableStateOf<PlayScene.Station?>(null) }
        // Was die Figur gerade in der Hand haelt, und wo zuletzt etwas aufgeblitzt ist - siehe
        // PlayEffects. Beides zusammen beantwortet beim Zuschauen die Frage "was hat er vor?",
        // die eine blosse Bewegung offenlaesst.
        var carried by remember { mutableStateOf<PlayEffects.Carried?>(null) }
        /** Sichtbare Phase der langen Drachen-Szene; null ausserhalb dieses Ablaufs. */
        var kitePhase by remember { mutableStateOf<PlayEffects.KitePhase?>(null) }
        /** Sichtbare Fussballphase; zugleich der Kontext, in dem ein Trick gelernt werden kann. */
        var footballPhase by remember { mutableStateOf<PlayEffects.FootballPhase?>(null) }
        /** Sichtbare Phase der Angel-Szene am Teich; null ausserhalb dieses Ablaufs. */
        var fishingPhase by remember { mutableStateOf<PlayEffects.FishingPhase?>(null) }
        // Waehrend eines Raumwechsels ist die Figur im Tuerrahmen und damit nicht zu sehen.
        var avatarHidden by remember { mutableStateOf(false) }
        // Daempfung der Figur beim Durchschreiten einer Tuer - siehe moveToPlace.
        val avatarDim = remember { Animatable(1f) }
        // Gast, der gerade durchs Bild laeuft - siehe runVisit.
        var visitor by remember { mutableStateOf<VisitorState?>(null) }
        // Sprechzeichen: -1 = niemand spricht, sonst 0..2 fuer die drei Punkte.
        var speechStep by remember { mutableIntStateOf(-1) }
        var speakerIsGuest by remember { mutableStateOf(false) }
        var sparkAt by remember { mutableStateOf<PlayScene.SceneSpot?>(null) }
        val sparkProgress = remember { Animatable(1f) }
        // Zustand der Stehlampe - der erste Gegenstand, den die Figur selbst schaltet (siehe
        // RoutineStep.Switch). Anfangs nach der Tageszeit: Wer abends dazukommt, findet Licht vor,
        // statt in einem dunklen Zimmer zu stehen, bis der Avatar zufaellig den Schalter erreicht.
        var savedClockOffset by remember { mutableStateOf<Offset?>(null) }
        // Muenzen und Vorrat liegen in den Einstellungen, nicht im Compose-Zustand - eine Anzeige,
        // die sie nur einmal liest, bliebe fuer immer auf dem Startwert stehen. Dieser Zaehler
        // wird bei jeder Aenderung hochgesetzt und dient als Schluessel fuers Neulesen.
        //
        // Gelesen wird er jetzt vom Gespraech: Bezahlt der Avatar an der Kasse, waehrend das Feld
        // offen steht, sollen Muenzen und Vorrat darin nicht auf dem alten Stand stehen bleiben.
        var economyTick by remember { mutableIntStateOf(0) }
        // Laufende Aufnahme: null = keine. Der Fortschritt gilt fuers Zusammenrechnen DANACH.
        var clipSession by remember { mutableStateOf<PlayClipRecorder.Session?>(null) }
        // Das kurze Aufhellen nach einem Schnappschuss. Der Zaehler daneben ist noetig, damit auch
        // zwei rasch aufeinanderfolgende Bilder je ein eigenes Blinken bekommen - ohne ihn
        // uebernaehme das zweite nur die noch laufende Wartezeit des ersten.
        var snapshotFlash by remember { mutableStateOf(false) }
        var snapshotCount by remember { mutableStateOf(0) }
        // Das Gespraech: offen/zu, und was er dabei weiss (siehe PlayTalk). Beim Oeffnen EINMAL
        // geholt, damit die Antworten untereinander nicht auseinanderlaufen; der Zaehler erzwingt
        // ein Nachladen, nachdem gerade eine Erinnerung angelegt wurde.
        var talkOpen by remember { mutableStateOf(false) }
        var talkRefresh by remember { mutableStateOf(0) }
        /** Zaehlt beantwortete/weggewischte Fragen - damit die naechste Frage sofort nachrueckt. */
        var askTick by remember { mutableStateOf(0) }
        /**
         * Die Themen seines Entwicklungspfades (siehe PlayPath) - sie faerben, was er von sich aus
         * tut.
         *
         * **Getrennt vom Gespraech geladen, und das ist keine Doppelung.** Der Pfad wirkt auf das
         * Verhalten, also schon lange bevor jemand das Gespraech oeffnet - haenge er an
         * [talkKnowledge], entwickelte sich das Wesen erst, nachdem man es danach gefragt hat.
         * Nachgeladen wird, wenn sich am Spielstand etwas geruehrt hat.
         */
        var leaningTopics by remember { mutableStateOf(emptySet<AnimationType>()) }
        /** Was sich im Lauf der Entwicklung in seiner Wohnung angesammelt hat - siehe PlayPath. */
        var acquisitions by remember { mutableStateOf(emptySet<PlayScene.Acquisition>()) }
        /**
         * Zaehlt jede Fuetterung mit - der Ausloeser dafuer, die Entwicklung neu nachzusehen.
         *
         * Eine Fuetterung ist der einzige Weg zu neuer Erfahrung und damit zu einer neuen Stufe;
         * haenge das Nachsehen nur am Wirtschafts-Zaehler (Geld, Vorrat), erschiene ein neu
         * erworbenes Stueck erst beim naechsten Einkauf.
         */
        var fedCount by remember { mutableStateOf(0) }
        // Auch auf die STUFE hoeren, nicht nur auf Geld und Vorrat: Ein Aufstieg ist genau der
        // Moment, in dem ein neues Stueck dazukommt - haenge das nur am Wirtschafts-Zaehler,
        // erschiene es erst beim naechsten Einkauf.
        LaunchedEffect(playMode, avatar?.species, economyTick, fedCount) {
            if (!playMode) return@LaunchedEffect
            val development = withContext(Dispatchers.IO) {
                PlayTalk.developmentNow(context, PresentCompanion.profileId(context))
            }
            leaningTopics = development.path?.topics.orEmpty()
            acquisitions = PlayPath.acquisitionsUpTo(development.path, development.stage)
        }

        /** Ob er zu hoeren ist - im Gespraech umschaltbar, siehe PlaySound. */
        var soundOn by remember { mutableStateOf(PlaySound.isEnabled(context)) }
        /**
         * Was er in DIESEM Gespraech schon von sich erzaehlt hat (siehe PlayLore).
         *
         * Im Bildschirm gehalten und nicht in den Einstellungen: Wieviel er insgesamt erzaehlt
         * hat, gehoert dauerhaft gespeichert - was gerade untereinander steht, gehoert zu diesem
         * einen Gespraech. Beim Schliessen wird geleert; beim Wiedereroeffnen (siehe Tap-Handler
         * auf dem Avatar) steht das zuletzt erzaehlte Stueck erneut da, damit "nichts Neues mehr
         * da" nicht wie ein leeres Fenster aussieht - reiner Lesezugriff auf [PlayLore.heard].
         */
        var toldNow by remember { mutableStateOf(listOf<Int>()) }
        var talkKnowledge by remember { mutableStateOf<PlayTalk.Knowledge?>(null) }
        /** Die Frage, die er im Gespraech stellt - siehe PlayTalk.pendingAsk. */
        val pendingAsk = remember(talkKnowledge, askTick) {
            talkKnowledge?.let { known ->
                PlayTalk.pendingAsk(
                    knowledge = known,
                    alreadyAsked = PlayTalk.Ask.entries
                        .filter { PlayUserProfile.wasAsked(context, it) }
                        .toSet()
                )
            }
        }

        // Worum er gerade gebeten wurde - siehe die Regungs-Schleife weiter unten.
        var requestedTopic by remember(presenceProfileId) {
            mutableStateOf<AnimationType?>(initialPresence.topic)
        }
        val lifecycleOwner = LocalLifecycleOwner.current
        var leftPlayAtMillis by remember { mutableStateOf<Long?>(null) }
        DisposableEffect(lifecycleOwner, playMode, presenceProfileId) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_STOP -> if (playMode) {
                        val now = System.currentTimeMillis()
                        currentTopic?.let { topic ->
                            PlayPresence.save(context, presenceProfileId, currentPlace, topic, now)
                        }
                        leftPlayAtMillis = now
                    }
                    Lifecycle.Event.ON_START -> {
                        val leftAt = leftPlayAtMillis
                        if (playMode && leftAt != null &&
                            System.currentTimeMillis() - leftAt > PlayPresence.SHORT_RETURN_MS
                        ) {
                            val topic = PlayPresence.topicFor(java.time.LocalDateTime.now())
                            val place = PlayScene.forTopic(topic)
                            currentPlace = place
                            renderedPlace = place
                            currentTopic = topic
                            requestedTopic = topic
                        }
                        leftPlayAtMillis = null
                    }
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        // Der letzte glaubhafte Zustand wird bei jeder Aenderung gespeichert; ON_STOP oben setzt
        // den Zeitstempel exakt auf den Moment des Weggehens. So laesst sich eine kurze Rueckkehr
        // von einem langen Fortsein unterscheiden, ohne Ablaufschritte nachzusimulieren.
        LaunchedEffect(playMode, presenceProfileId, currentPlace, currentTopic) {
            if (!playMode) return@LaunchedEffect
            currentTopic?.let { topic ->
                PlayPresence.save(context, presenceProfileId, currentPlace, topic)
            }
        }
        var clipSeconds by remember { mutableIntStateOf(0) }
        var clipEncoding by remember { mutableFloatStateOf(-1f) }
        var clipResult by remember { mutableStateOf<java.io.File?>(null) }

        var lampOn by remember { mutableStateOf(false) }
        var tvOn by remember { mutableStateOf(false) }
        // Requisite, an der die Figur gerade hantiert - sie wird dann geoeffnet gezeichnet.
        var activeStation by remember { mutableStateOf<PlayScene.Station?>(null) }
        LaunchedEffect(Unit) {
            val phase = PlayAmbientActivity.currentDayPhase()
            lampOn = phase == PlayAmbientActivity.DayPhase.EVENING ||
                phase == PlayAmbientActivity.DayPhase.NIGHT
        }
        var scenePhase by remember { mutableIntStateOf(0) }
        val sceneFade = remember { Animatable(1f) }

        // Startet (und ersetzt) die endlose Ruhe-Schleife des Avatars - eigene Funktion, weil sie
        // im Play-Modus von mehreren Stellen aus gebraucht wird (Einstieg in den Play-Modus,
        // Rueckkehr aus einer Fuetter-Reaktion, Rueckkehr aus einer autonomen Regung, siehe
        // PlayAmbientActivity), statt wie im normalen Dock nur einmal beim Spawn. Ueber [scope]
        // statt der jeweils aufrufenden LaunchedEffect gestartet, damit die Schleife laenger lebt
        // als der einzelne Aufruf, der sie angestossen hat.
        fun startAvatarIdleLoop(species: AvatarSpecies, mood: AvatarMood) {
            avatarIdleJob?.cancel()
            avatarIdleJob = scope.launch {
                val idle = AvatarAnimations.idleSequence(species, mood)
                while (isActive) {
                    MatrixAnimator.playTimed(idle.frames, idle.holdsMs) { f ->
                        avatar = avatar?.copy(frame = f)
                    }
                }
            }
        }

        /**
         * Laesst den Avatar zu [destination] GEHEN und meldet zurueck, ob er dafuer ueberhaupt
         * losgelaufen ist.
         *
         * Drei Dinge, die zusammen erst den Unterschied zwischen Gehen und Gleiten machen:
         *
         * 1. **Bildsynchron statt Eigentakt.** Vorher lief die Bewegung ueber eine Schleife mit
         *    `delay(40ms)` - also 25 Schritte pro Sekunde, unabhaengig davon, wann das Display
         *    tatsaechlich zeichnet. Auf einem 120-Hz-Bildschirm ist das sichtbares Stocken.
         *    [animate] haengt sich stattdessen an den Bildtakt des Systems.
         * 2. **Ein- und Ausschwingen.** Losfahren und Anhalten mit voller Geschwindigkeit wirkt
         *    mechanisch; nichts Lebendiges bewegt sich so.
         * 3. **Dauer nach Entfernung.** Vorher brauchte jeder Weg dieselben 900ms - ein halber
         *    Schritt genauso lange wie die volle Bildschirmbreite. Das ist der Punkt, an dem
         *    Bewegung ihre Glaubwuerdigkeit verliert, weil die Geschwindigkeit springt.
         *
         * Dazu laeuft der Geh-Zyklus ([AvatarAnimations.walkSequence]) an Stelle der
         * Ruhe-Schleife. Der Aufrufer entscheidet, was danach kommt - deshalb wird die
         * Ruhe-Schleife hier NICHT wieder gestartet: nach einem Spaziergang ist das die Ruhe,
         * nach dem Hinweg zu einer Handlung aber die Handlung selbst.
         */
        suspend fun walkAvatarTo(destination: Offset): Boolean {
            val current = avatar ?: return false
            val avatarPx = with(density) { current.sizeDp.dp.toPx() }
            val distance = abs(destination.x - current.offset.x)
            // Sehr kurze Wege gar nicht erst als Gang inszenieren - ein Geh-Zyklus ueber zwei
            // Pixel sieht aus wie ein Zucken.
            if (distance < avatarPx * MIN_WALK_FRACTION) {
                avatar = avatar?.copy(offset = destination)
                return false
            }
            avatarIdleJob?.cancel()
            // Waehrend eines Gangs darf niemand sonst die Figur versetzen - siehe das
            // Nachfuehren des Bodens weiter unten.
            avatarWalking = true
            try {
            coroutineScope {
                val gait = launch {
                    val walk = AvatarAnimations.walkSequence(current.species)
                    while (isActive) {
                        MatrixAnimator.playTimed(walk.frames, walk.holdsMs) { f ->
                            avatar = avatar?.copy(frame = f)
                        }
                    }
                }
                animate(
                    initialValue = current.offset.x,
                    targetValue = destination.x,
                    animationSpec = tween(walkDurationMs(distance, avatarPx), easing = FastOutSlowInEasing)
                ) { value, _ ->
                    // Die Hoehe wird NICHT mitanimiert, sondern Bild fuer Bild aus dem jetzigen
                    // Boden geholt: Gegangen wird immer auf dem Boden, und der kann sich waehrend
                    // des Gangs bewegen (siehe floorYPxNow). Vorher stand hier die beim Start
                    // eingefangene Zielhoehe - die Figur lief dann auf altem Niveau weiter und
                    // sprang am Ende nach unten.
                    val walking = avatar
                    if (walking != null) {
                        avatar = walking.copy(
                            offset = Offset(
                                value,
                                AvatarFooting.topFor(
                                    floorYPxNow,
                                    avatarPx,
                                    AvatarBodies.forSpecies(walking.species).groundRow()
                                )
                            )
                        )
                    }
                }
                gait.cancel()
            }
            } finally {
                avatarWalking = false
            }
            return true
        }

        /** Laesst an der Hand der Figur kurz etwas aufblitzen (siehe [PlayEffects.sparkCells]). */
        suspend fun flashAt(state: AvatarState?) {
            val current = state ?: return
            if (sceneCellPx <= 0f) return
            val cellX = (current.offset.x / sceneCellPx).roundToInt() + FLASH_HAND_X
            val cellY = (current.offset.y / sceneCellPx).roundToInt() + FLASH_HAND_Y
            sparkAt = PlayScene.SceneSpot(cellX, cellY)
            sparkProgress.snapTo(0f)
            sparkProgress.animateTo(1f, tween(PlayEffects.SPARK_DURATION_MS))
            sparkAt = null
        }

        /**
         * Bringt die Figur DURCH DIE TUER in einen anderen Raum.
         *
         * **Warum ueberhaupt eine Tuer.** Bisher wechselte die Kulisse einfach per Ueberblendung,
         * waehrend die Figur weiterlief - zwei Bilder, zwischen denen umgeschaltet wurde. Dass es
         * sich um zwei ZIMMER derselben Wohnung handelt, war eine Behauptung. Wer den Raum durch
         * eine Tuer verlaesst und im naechsten an derselben Tuer wieder hereinkommt, macht daraus
         * einen Weg - und erst damit wird die Welt zusammenhaengend statt bloss abwechslungsreich.
         *
         * Der Park hat keine Tuer (man geht nach draussen, nicht in ein Zimmer); dorthin und von
         * dort wird weiterhin ueberblendet.
         */
        suspend fun moveToPlace(target: PlayScene.Place, species: AvatarSpecies) {
            if (target == currentPlace) return
            val hasDoorHere = PlayScene.Station.DOOR in PlayScene.stationsAt(currentPlace, species)
            val hasDoorThere = PlayScene.Station.DOOR in PlayScene.stationsAt(target, species)
            if (hasDoorHere) {
                PlayScene.stationSpot(currentPlace, PlayScene.Station.DOOR, sceneWidthCells, floorYCells, species)
                    ?.let { spot ->
                        val px = with(density) { (avatar?.sizeDp ?: worldAvatarSizeDp).dp.toPx() }
                        val boundX = (maxWidthPx - px).coerceAtLeast(1f)
                        val anchor = ((spot.centerX * sceneCellPx - px / 2f) / boundX).coerceIn(0f, 1f)
                        if (walkAvatarTo(avatarSpot(anchor, px, maxWidthPx, floorYPx, species))) {
                            startAvatarIdleLoop(species, AvatarMoodSnapshot.forSpecies(context, species))
                        }
                    }
            }

            // Waehrend die Kulisse ausgeblendet ist, ist auch die Figur weg - sie ist ja gerade
            // im Tuerrahmen. Ohne das schwebte sie sichtbar vor einer leeren Flaeche, waehrend
            // sich der Raum unter ihr austauscht.
            // **Der Durchgang selbst.** Vorher verschwand die Figur schlagartig, die Kulisse
            // wechselte, und sie war ploetzlich woanders - dass sie durch eine TUER gegangen war,
            // sah man nicht. Jetzt geht die Tuer auf, die Figur verblasst IN den Rahmen hinein
            // (sie tritt hindurch), und drueben kommt sie aus dem Rahmen wieder hervor. Erst
            // dieses Verblassen an genau der Stelle macht aus dem Wechsel einen Weg.
            activeStation = PlayScene.Station.DOOR   // Tuer geht auf
            delay(DOOR_OPEN_MS)
            avatarDim.animateTo(0f, tween(DOOR_STEP_MS))
            avatarHidden = true
            currentPlace = target
            delay((SCENE_FADE_OUT_MS + SCENE_FADE_IN_MS).toLong())
            // Im neuen Raum an der Tuer wieder auftauchen und hereinkommen.
            avatar?.let { arriving ->
                val px = with(density) { arriving.sizeDp.dp.toPx() }
                val entry = if (hasDoorThere) {
                    PlayScene.stationSpot(target, PlayScene.Station.DOOR, sceneWidthCells, floorYCells, species)
                        ?.let { spot ->
                            val boundX = (maxWidthPx - px).coerceAtLeast(1f)
                            ((spot.centerX * sceneCellPx - px / 2f) / boundX).coerceIn(0f, 1f)
                        } ?: 0.9f
                } else {
                    0.95f   // aus dem Bild heraus in den Park hinein
                }
                avatar = arriving.copy(offset = avatarSpot(entry, px, maxWidthPx, floorYPx, species))
            }
            avatarHidden = false
            // Aus dem Rahmen hervortreten, dann faellt die Tuer zu.
            avatarDim.animateTo(1f, tween(DOOR_STEP_MS))
            activeStation = null
            if (walkAvatarTo(
                    avatarSpot(
                        PlayScene.screenFraction(PlayScene.avatarAnchorX(target), sceneWidthCells),
                        with(density) { (avatar?.sizeDp ?: worldAvatarSizeDp).dp.toPx() },
                        maxWidthPx, floorYPx, species
                    )
                )
            ) {
                startAvatarIdleLoop(species, AvatarMoodSnapshot.forSpecies(context, species))
            }
        }

        /**
         * Fuehrt einen Tagesablauf aus (siehe [PlayRoutine]) - **die Stelle, an der aus Posen
         * neben Moebeln ein Umgang mit ihnen wird.**
         *
         * Bewusst als schlichte Schleife ueber die Schritte und nicht als Zustandsautomat: Ein
         * Ablauf laeuft von vorn bis hinten durch und wird abgebrochen, wenn etwas dazwischen
         * kommt (die umgebende Coroutine wird gecancelt, sobald eine echte Erinnerung feuert oder
         * der Play-Modus endet). Alles, was darueber hinausginge - Unterbrechen und
         * Wiederaufnehmen, mehrere gleichzeitige Ablaeufe - waere Aufwand fuer einen Fall, den es
         * hier nicht gibt.
         */
        suspend fun runRoutine(routine: PlayRoutine, species: AvatarSpecies) {
            val mood = AvatarMoodSnapshot.forSpecies(context, species)
            routineRunning = true

            /** Setzt die Figur so, dass sie mit [centerX]/[groundY] (Szenenzellen) zusammenfaellt. */
            fun spotToOffset(spot: PlayScene.SceneSpot, avatarPx: Float): Offset =
                stationOffset(spot, avatarPx, maxWidthPx, species)

            try {
            for (step in routine.steps) {
                val current = avatar ?: return
                if (current.fed || current.occurrenceId != null) return   // echte Erinnerung hat Vorrang
                val avatarPx = with(density) { current.sizeDp.dp.toPx() }

                when (step) {
                    is RoutineStep.GoTo -> {
                        // MIT der Spezies: Wo ein Platz liegt, haengt an der Einrichtung, und die
                        // gehoert der Kreatur (siehe PlayScene.Home). Ohne diesen Wert rechnete
                        // der Ablauf mit der Wohnung des Einstiegs-Avatars - die Figur lief zur
                        // Stelle, an der bei PUFFLING das Bett steht, und legte sich dort neben
                        // ihr eigenes. Unauffaellig, solange sich nur das Schlafzimmer
                        // unterschied; mit sechs vollstaendigen Wohnungen waere es in jedem Raum
                        // sichtbar geworden.
                        val spot = PlayScene.stationSpot(
                            currentPlace, step.station, sceneWidthCells, floorYCells, species
                        ) ?: continue
                        // Zum Platz gehen, aber auf dem BODEN bleiben: das Hinaufsteigen ist ein
                        // eigener Schritt (Occupy) - sonst schwebte die Figur beim Hingehen
                        // bereits auf Matratzenhoehe heran.
                        val target = spotToOffset(spot, avatarPx)
                        val onFloor = avatarSpot(
                            anchorX = ((target.x / (maxWidthPx - avatarPx).coerceAtLeast(1f))).coerceIn(0f, 1f),
                            avatarPx = avatarPx,
                            maxWidthPx = maxWidthPx,
                            floorYPx = floorYPx,
                            species = species
                        )
                        if (walkAvatarTo(onFloor)) startAvatarIdleLoop(species, mood)
                        activeStation = step.station
                        // An der Kasse wird bezahlt - mit sichtbarem Aufblitzen, damit man den
                        // Vorgang bemerkt und nicht nur die Zahl unten kleiner wird.
                        if (step.station == PlayScene.Station.CHECKOUT && PlayWallet.pay(context)) {
                            economyTick++
                            flashAt(avatar)
                        }
                    }

                    is RoutineStep.Stroll -> {
                        activeStation = null
                        // Ueber das ZIMMER umgerechnet, nicht ueber das ganze Bild: Im
                        // Querformat liefe sie sonst quer ueber den Bildschirm, waehrend ihre
                        // Moebel in der Mitte stehen (siehe PlayScene.screenFraction).
                        val destination = avatarSpot(
                            PlayScene.screenFraction(step.anchorX, sceneWidthCells),
                            avatarPx, maxWidthPx, floorYPx, species
                        )
                        if (walkAvatarTo(destination)) startAvatarIdleLoop(species, mood)
                    }

                    is RoutineStep.Occupy -> {
                        // Ebenfalls mit der Spezies - siehe GoTo weiter oben.
                        val spot = PlayScene.stationSpot(
                            currentPlace, step.station, sceneWidthCells, floorYCells, species
                        ) ?: continue
                        // Kurzes Hinauf statt Sprung: dieselbe weiche Bewegung wie beim Gehen,
                        // nur senkrecht - sich hinzulegen ist keine Ortsveraenderung, sondern
                        // eine Bewegung an Ort und Stelle.
                        val target = spotToOffset(spot, avatarPx)
                        val from = current.offset
                        avatarSettling = true
                        try {
                            animate(0f, 1f, animationSpec = tween(SETTLE_INTO_MS, easing = FastOutSlowInEasing)) { t, _ ->
                                avatar = avatar?.copy(
                                    offset = Offset(
                                        from.x + (target.x - from.x) * t,
                                        from.y + (target.y - from.y) * t
                                    )
                                )
                            }
                        } finally {
                            avatarSettling = false
                        }
                        occupiedStation = step.station
                    }

                    RoutineStep.Rise -> {
                        occupiedStation = null
                        val standing = avatar ?: return
                        val onFloor = avatarSpot(
                            anchorX = (standing.offset.x / (maxWidthPx - avatarPx).coerceAtLeast(1f)).coerceIn(0f, 1f),
                            avatarPx = avatarPx,
                            maxWidthPx = maxWidthPx,
                            floorYPx = floorYPx,
                            species = species
                        )
                        val from = standing.offset
                        avatarSettling = true
                        try {
                            animate(0f, 1f, animationSpec = tween(SETTLE_INTO_MS, easing = FastOutSlowInEasing)) { t, _ ->
                                avatar = avatar?.copy(
                                    offset = Offset(
                                        from.x + (onFloor.x - from.x) * t,
                                        from.y + (onFloor.y - from.y) * t
                                    )
                                )
                            }
                        } finally {
                            avatarSettling = false
                        }
                    }

                    is RoutineStep.Act -> {
                        // Essen zehrt am Vorrat - das ist die Rueckkopplung, aus der spaeter der
                        // Einkauf entsteht (siehe PlayPantry und PlayRoutines.forTopic).
                        if (step.topic == AnimationType.DRINK) {
                            PlayPantry.consume(context)
                            economyTick++
                        }
                        // Der Lohn - erst dadurch ist Arbeit mehr als eine Bewegung.
                        if (step.topic == AnimationType.WORK) {
                            PlayWallet.earn(context)
                            economyTick++
                        }
                        avatarIdleJob?.cancel()
                        delay(ARRIVAL_SETTLE_MS)
                        val performance = AvatarAnimations.reactionFor(species, step.topic)
                        MatrixAnimator.playTimed(performance.frames, performance.holdsMs) { f ->
                            avatar = avatar?.copy(frame = f)
                        }
                        startAvatarIdleLoop(species, mood)
                    }

                    is RoutineStep.Stir -> {
                        avatarIdleJob?.cancel()
                        val fidget = AvatarAnimations.fidgetSequence(species, step.fidget)
                        MatrixAnimator.playTimed(fidget.frames, fidget.holdsMs) { f ->
                            avatar = avatar?.copy(frame = f)
                        }
                        startAvatarIdleLoop(species, mood)
                    }

                    is RoutineStep.Kite -> {
                        kitePhase = step.phase
                        // Auspacken und Einholen bekommen eine erkennbare Koerperbewegung. Der
                        // lange Flug selbst bleibt ruhig; dort bewegt der Wind den Drachen.
                        if (step.phase == PlayEffects.KitePhase.PREPARE ||
                            step.phase == PlayEffects.KitePhase.LAND
                        ) {
                            avatarIdleJob?.cancel()
                            val handling = AvatarAnimations.fidgetSequence(
                                species,
                                if (step.phase == PlayEffects.KitePhase.PREPARE) {
                                    AvatarAnimations.Fidget.STRETCH
                                } else {
                                    AvatarAnimations.Fidget.LOOK_AROUND
                                }
                            )
                            MatrixAnimator.playTimed(handling.frames, handling.holdsMs) { f ->
                                avatar = avatar?.copy(frame = f)
                            }
                            startAvatarIdleLoop(species, mood)
                        }
                    }

                    is RoutineStep.Football -> {
                        footballPhase = step.phase
                        avatarIdleJob?.cancel()
                        val motion = AvatarAnimations.reactionFor(species, AnimationType.MOVE)
                        MatrixAnimator.playTimed(motion.frames, motion.holdsMs) { f ->
                            avatar = avatar?.copy(frame = f)
                        }
                        startAvatarIdleLoop(species, mood)
                    }

                    is RoutineStep.Fishing -> {
                        fishingPhase = step.phase
                        // Nur beim Auswerfen und beim Anschlagen eine eigene Koerperbewegung -
                        // beim Warten bleibt die Figur ruhig stehen, damit WAIT tatsaechlich nach
                        // Warten aussieht und nicht wie eine weitere Aktion.
                        if (step.phase == PlayEffects.FishingPhase.CAST ||
                            step.phase == PlayEffects.FishingPhase.CATCH
                        ) {
                            avatarIdleJob?.cancel()
                            val motion = AvatarAnimations.fidgetSequence(
                                species,
                                if (step.phase == PlayEffects.FishingPhase.CAST) {
                                    AvatarAnimations.Fidget.STRETCH
                                } else {
                                    AvatarAnimations.Fidget.LOOK_AROUND
                                }
                            )
                            MatrixAnimator.playTimed(motion.frames, motion.holdsMs) { f ->
                                avatar = avatar?.copy(frame = f)
                            }
                            startAvatarIdleLoop(species, mood)
                        }
                    }

                    // Verweilen ist vergehende ZEIT und wird im Zeitraffer entsprechend gekuerzt -
                    // sonst haetten die Pausen zwischen den Regungen ein anderes Tempo als die
                    // Pausen innerhalb eines Ablaufs, und der Turbo bliebe an jedem Sessel haengen.
                    // Die Animationen selbst bleiben unangetastet: mitbeschleunigt saehe man nur
                    // noch Zucken statt Bewegung.
                    is RoutineStep.Linger ->
                        delay((step.millis * PlayTimeLapse.paceFactor()).toLong().coerceAtLeast(120L))

                    is RoutineStep.Take -> {
                        // Hinlangen, Blitz genau dort, wo zugegriffen wird, dann liegt es in der
                        // Hand. Die Reihenfolge ist der ganze Punkt: Erst die Bewegung, dann das
                        // Ereignis, dann das Ergebnis - so liest sich eine Ursache.
                        avatarIdleJob?.cancel()
                        val reach = AvatarAnimations.fidgetSequence(species, AvatarAnimations.Fidget.STRETCH)
                        MatrixAnimator.playTimed(reach.frames, reach.holdsMs) { f ->
                            avatar = avatar?.copy(frame = f)
                        }
                        flashAt(avatar)
                        carried = step.item
                        startAvatarIdleLoop(species, mood)
                    }

                    is RoutineStep.GoToPlace -> {
                        activeStation = null
                        moveToPlace(step.place, species)
                    }

                    // Es gab diesen Zweig zweimal - einmal ohne das Auffuellen des Vorrats, und
                    // zwar VOR diesem hier. Kotlin nimmt in einem `when` den ersten passenden
                    // Zweig, der zweite war also toter Code: Der Avatar ging einkaufen, trug das
                    // Essen nach Hause, legte es in der Kueche ab - und der Vorrat blieb leer.
                    // Damit war ab dem ersten Aufbrauchen dauerhaft "kein Essen da", die Figur
                    // musste endlos arbeiten und einkaufen, ohne dass es je etwas aenderte.
                    RoutineStep.Drop -> {
                        if (carried != null) {
                            // Wird der Einkauf zu Hause eingeraeumt, ist der Vorrat wieder voll.
                            if (currentPlace == PlayScene.Place.KITCHEN &&
                                carried == PlayEffects.Carried.FOOD
                            ) {
                                PlayPantry.refill(context)
                                economyTick++
                            }
                            avatarIdleJob?.cancel()
                            val put = AvatarAnimations.fidgetSequence(species, AvatarAnimations.Fidget.STRETCH)
                            MatrixAnimator.playTimed(put.frames, put.holdsMs) { f ->
                                avatar = avatar?.copy(frame = f)
                            }
                            flashAt(avatar)
                            carried = null
                            startAvatarIdleLoop(species, mood)
                        }
                    }

                    is RoutineStep.Switch -> {
                        // Kurz hinlangen, dann schaltet es - die Regung macht aus dem
                        // Zustandswechsel eine sichtbare Handlung statt eines Sprungs im Bild.
                        avatarIdleJob?.cancel()
                        val reach = AvatarAnimations.fidgetSequence(species, AvatarAnimations.Fidget.STRETCH)
                        MatrixAnimator.playTimed(reach.frames, reach.holdsMs) { f ->
                            avatar = avatar?.copy(frame = f)
                        }
                        when (step.device) {
                            PlayScene.Station.TV -> tvOn = step.on
                            else -> lampOn = step.on
                        }
                        startAvatarIdleLoop(species, mood)
                    }
                }
            }
            } finally {
                // ZWINGEND im finally, nicht am Ende der Schleife: Ein Ablauf wird regelmaessig
                // vorzeitig verlassen - wenn eine echte Erinnerung feuert (return oben) oder wenn
                // die umgebende Coroutine abgebrochen wird (Play-Modus aus, Spezies gewechselt).
                // Bliebe die Figur dann als "benutzt gemeldet" stehen, laege die Bettdecke
                // weiterhin ueber ihr, waehrend sie laengst woanders steht - und zwar so lange,
                // bis zufaellig irgendein spaeterer Ablauf mit einem Rise endet.
                routineRunning = false
                occupiedStation = null
                avatarSettling = false
                // Dasselbe fuer Getragenes: Bricht der Ablauf zwischen Take und Drop ab, trueg
                // die Figur das Buch sonst durch alle folgenden Szenen mit sich herum.
                carried = null
                kitePhase = null
                footballPhase = null
                fishingPhase = null
                // Und zurueck auf den Boden: Wird der Ablauf abgebrochen, waehrend die Figur im
                // Bett liegt, verschwindet zwar die Decke - stehen bliebe sie aber weiterhin auf
                // Matratzenhoehe und damit sichtbar in der Luft. Bewusst ohne Animation gesetzt,
                // weil dieser Block auch im Abbruchfall laeuft und dort nicht mehr gewartet
                // werden darf.
                avatar?.let { standing ->
                    val px = with(density) { standing.sizeDp.dp.toPx() }
                    val fraction = (standing.offset.x / (maxWidthPx - px).coerceAtLeast(1f)).coerceIn(0f, 1f)
                    avatar = standing.copy(
                        offset = avatarSpot(fraction, px, maxWidthPx, floorYPx, standing.species)
                    )
                }
            }
        }

        /**
         * Ein BESUCH: eine zweite Kreatur laeuft durchs Bild und gruesst im Vorbeigehen.
         *
         * **Warum das mehr ist als eine weitere Animation.** Alles bisherige spielte sich zwischen
         * einer Figur und ihren Gegenstaenden ab. Ein Gegenueber ist die einzige Art von Ereignis,
         * die eine Welt bewohnt statt bloss eingerichtet wirken laesst - und es genuegt dafuer,
         * dass jemand vorbeikommt, kurz stehen bleibt und weitergeht. Er muss nichts erledigen.
         *
         * **Bewusst eine ANDERE Spezies als die eigene.** Sonst saehe es aus, als liefe der Avatar
         * sich selbst ueber den Weg - bei sechs Grundformen waere das ein unnoetiger Zufall.
         *
         * Der Besuch bleibt aus, solange eine echte Erinnerung offen ist oder gefuettert wird: In
         * diesen Momenten gehoert die Aufmerksamkeit dem Nutzer, nicht der Kulisse.
         */
        suspend fun runVisit() {
            val host = avatar ?: return
            if (host.fed || host.occurrenceId != null || avatarHidden) return
            val guestSpecies = AvatarSpecies.entries.filter { it != host.species }.random()
            val px = with(density) { host.sizeDp.dp.toPx() }
            val fromLeft = Random.nextBoolean()
            val startX = if (fromLeft) -px else maxWidthPx
            val exitX = if (fromLeft) maxWidthPx else -px
            val groundY = avatarSpot(0f, px, maxWidthPx, floorYPx, guestSpecies).y
            val mood = AvatarMoodSnapshot.forSpecies(context, guestSpecies)
            val walk = AvatarAnimations.walkSequence(guestSpecies)
            val idle = AvatarAnimations.idleSequence(guestSpecies, mood)

            visitor = VisitorState(guestSpecies, Offset(startX, groundY), host.sizeDp, walk.frames.first())
            lastVisitor = guestSpecies
            // Der Gast gruesst mit SEINEM Motiv, nicht mit dem des Bewohners - daran hoert man,
            // dass jemand anderes da ist.
            PlaySound.play(context, guestSpecies, PlayChime.Event.VISIT, scope)

            /** Laesst den Gast von seiner jetzigen Stelle nach [targetX] gehen. */
            suspend fun walkGuestTo(targetX: Float) = coroutineScope {
                val from = visitor?.offset?.x ?: return@coroutineScope
                val gait = launch {
                    while (isActive) {
                        MatrixAnimator.playTimed(walk.frames, walk.holdsMs) { f ->
                            visitor = visitor?.copy(frame = f)
                        }
                    }
                }
                animate(
                    initialValue = from,
                    targetValue = targetX,
                    animationSpec = tween(walkDurationMs(abs(targetX - from), px), easing = FastOutSlowInEasing)
                ) { value, _ -> visitor = visitor?.copy(offset = Offset(value, groundY)) }
                gait.cancel()
            }

            try {
                // Treffpunkt so waehlen, dass sich die beiden NICHT ueberdecken.
                //
                // Der erste Entwurf stellte den Gast einfach auf die Seite, von der er kam. Stand
                // der Bewohner nahe am Bildrand, wurde dieser Platz auf den Rand zurechtgestutzt -
                // und der Gast landete genau auf ihm. Deshalb wird jetzt die Seite mit mehr Raum
                // genommen und geprueft, ob dort ueberhaupt genug Platz ist; sonst bleibt der Gast
                // weiter weg stehen, statt in den Bewohner hineinzulaufen.
                val bound = (maxWidthPx - px).coerceAtLeast(0f)
                val gap = px * VISITOR_GAP
                val roomLeft = host.offset.x
                val roomRight = bound - host.offset.x
                val preferLeft = roomLeft > roomRight
                val meetX = if (preferLeft) {
                    (host.offset.x - gap).coerceIn(0f, bound)
                } else {
                    (host.offset.x + gap).coerceIn(0f, bound)
                }
                walkGuestTo(meetX)

                // UNTERHALTUNG statt eines einzelnen Grusses: mehrere Wortwechsel hin und her,
                // jeweils mit Sprechzeichen ueber dem Kopf dessen, der gerade dran ist. Ein
                // einmaliges gemeinsames Huepfen liess sich als alles moegliche deuten; ein
                // Wechsel mit erkennbarem Sprecher kann nur ein Gespraech sein.
                //
                // Die Ruhe-Schleife des jeweils ZUHOERENDEN laeuft weiter - jemand, der beim
                // Zuhoeren einfriert, sieht aus wie ein Standbild, nicht wie ein Gespraechspartner.
                avatarIdleJob?.cancel()
                coroutineScope {
                    val hostIdleWhileListening = launch {
                        val hostIdle = AvatarAnimations.idleSequence(
                            host.species, AvatarMoodSnapshot.forSpecies(context, host.species)
                        )
                        while (isActive) {
                            MatrixAnimator.playTimed(hostIdle.frames, hostIdle.holdsMs) { f ->
                                avatar = avatar?.copy(frame = f)
                            }
                        }
                    }
                    val guestIdle = launch {
                        while (isActive) {
                            MatrixAnimator.playTimed(idle.frames, idle.holdsMs) { f ->
                                visitor = visitor?.copy(frame = f)
                            }
                        }
                    }

                    repeat(CONVERSATION_TURNS) { turn ->
                        val guestSpeaks = turn % 2 == 0
                        // Sprechzeichen laufen mit: erst ein Punkt, dann zwei, dann drei.
                        for (dot in 0..2) {
                            speakerIsGuest = guestSpeaks
                            speechStep = dot
                            delay(SPEECH_DOT_MS)
                        }
                        speechStep = -1
                        // Wer spricht, unterstreicht es einmal kurz mit seiner arteigenen Regung.
                        if (guestSpeaks) {
                            guestIdle.cancel()
                            val say = AvatarAnimations.fidgetSequence(guestSpecies, AvatarAnimations.Fidget.LOOK_AROUND)
                            MatrixAnimator.playTimed(say.frames, say.holdsMs) { f ->
                                visitor = visitor?.copy(frame = f)
                            }
                        } else {
                            hostIdleWhileListening.cancel()
                            val say = AvatarAnimations.tapReaction(host.species)
                            MatrixAnimator.playTimed(say.frames, say.holdsMs) { f ->
                                avatar = avatar?.copy(frame = f)
                            }
                        }
                    }
                    speechStep = -1
                    hostIdleWhileListening.cancel()
                    guestIdle.cancel()
                }
                startAvatarIdleLoop(host.species, AvatarMoodSnapshot.forSpecies(context, host.species))

                walkGuestTo(exitX)
            } finally {
                // Zwingend: Wird der Besuch abgebrochen (Erinnerung feuert, Play-Modus endet),
                // bliebe der Gast sonst mitten im Bild stehen und ginge nie wieder.
                visitor = null
                speechStep = -1
            }
        }

        // Erzeugt einen neuen, noch nicht an eine Erinnerung gebundenen Avatar (reminderId/
        // occurrenceId optional) und startet gleich seine Ruhe-Schleife - fuer den Einstieg in den
        // Play-Modus (siehe LaunchedEffect(playMode) unten) und fuer den seltenen Randfall, dass
        // eine Erinnerung feuert, bevor dieser Einstieg durchgelaufen ist.
        suspend fun spawnAmbientAvatar(
            reminderId: Long? = null,
            occurrenceId: Long? = null,
            animationType: AnimationType? = null,
            libraryAnimationLabel: String? = null,
            frames: List<IntArray> = emptyList()
        ): AvatarState {
            // Auf dem Boden der Kulisse statt an zufaelliger Stelle im Schwarzen: sobald ein Raum
            // gezeichnet wird, ist eine frei schwebende Figur der eine Fehler, den man sofort
            // sieht. Diese Funktion wird ausschliesslich im Play-Modus aufgerufen - nur dort gibt
            // es ueberhaupt einen Boden.
            val species = AvatarSpeciesPrefs.get(context)
            val spawnOffset = avatarSpot(
                anchorX = PlayScene.screenFraction(
                    PlayScene.avatarAnchorX(currentPlace), sceneWidthCells
                ),
                avatarPx = worldAvatarPx,
                maxWidthPx = maxWidthPx,
                floorYPx = floorYPx,
                species = species
            )
            val mood = AvatarMoodSnapshot.forSpecies(context, species)
            startAvatarIdleLoop(species, mood)
            return AvatarState(
                reminderId = reminderId,
                occurrenceId = occurrenceId,
                animationType = animationType,
                libraryAnimationLabel = libraryAnimationLabel,
                species = species,
                offset = spawnOffset,
                sizeDp = worldAvatarSizeDp,
                frame = AvatarAnimations.idleSequence(species, mood).frames.first(),
                frames = frames
            )
        }

        // ---- Die Welt hat ihre Groesse geaendert ----
        //
        // **Ein Fehler, der sich als "der Avatar haengt in der Luft" zeigte.** Seine Position
        // liegt in PIXELN fest, einmal beim Hinstellen aus der damaligen Bodenhoehe gerechnet.
        // Boden, Zellgroesse und Szenenbreite haengen dagegen an der Uhrgroesse und an den
        // Bildschirmmassen - zieht man die Uhr groesser oder dreht das Geraet, wandert der Boden,
        // und die Figur bleibt, wo sie war. Dazu behielt sie ihre alte Groesse, waehrend die
        // Kulisse mitwuchs: Genau das sah aus, als zoome die Landschaft unter ihr weg.
        //
        // [floorYPx] bleibt bei Ortswechseln stabil. Umgestellt wird hier nur, was der NUTZER
        // veraendert - Uhrgroesse und Bildschirmmasse.
        // Auf GERUNDETE Avatargroesse hoerend, nicht auf den genauen Wert. Sie leitet sich aus der
        // Uhrgroesse ab, und die schreibt die Zieh-Geste bei jeder Bewegung neu - beim reinen
        // Verschieben um Bruchteile eines Punktes. Auf den genauen Wert gehoert, liefe dieser
        // Effekt waehrend des Ziehens staendig mit und setzte die Figur bei jedem Bild neu auf den
        // Boden; mitten in einem Gang haette sie dabei angehalten. Gerundet bleibt er still,
        // solange sich nichts Sichtbares aendert.
        val avatarSizeStep = worldAvatarSizeDp.roundToInt()
        var lastAvatarPx by remember { mutableStateOf(0f) }
        var lastWidthPx by remember { mutableStateOf(0f) }
        LaunchedEffect(avatarSizeStep, maxWidthPx, maxHeightPx) {
            val current = avatar
            val previousAvatarPx = lastAvatarPx
            val previousWidthPx = lastWidthPx
            lastAvatarPx = worldAvatarPx
            lastWidthPx = maxWidthPx

            // Beim allerersten Durchlauf gibt es noch keine vorherige Geometrie - dann ist auch
            // nichts umzustellen, die Figur wird ohnehin gleich frisch hingestellt.
            if (!playMode || current == null || previousAvatarPx <= 0f || previousWidthPx <= 0f) {
                return@LaunchedEffect
            }

            val station = occupiedStation
            val next = if (station != null) {
                // Wer im Bett liegt, muss danach im NEUEN Bett liegen - die Requisiten sind
                // mitgewandert, ihre Aufsetzstelle ebenso.
                PlayScene.stationSpot(
                    renderedPlace, station, sceneWidthCells, floorYCells, current.species
                )?.let { spot ->
                    stationOffset(spot, worldAvatarPx, maxWidthPx, current.species)
                }
            } else {
                // Sonst bleibt die WAAGERECHTE Stelle erhalten, an der sie stand - als Bruchteil
                // gerechnet, und zwar mit den ALTEN Massen. Mit den neuen gerechnet spraenge sie
                // beim Drehen quer durchs Bild.
                val fraction = AvatarFooting.fractionOf(
                    current.offset.x, previousAvatarPx, previousWidthPx
                )
                avatarSpot(fraction, worldAvatarPx, maxWidthPx, floorYPx, current.species)
            }

            avatar = current.copy(
                // Die Groesse MUSS mitgehen: Sie stammt aus der Uhrgroesse, und eine Figur, die
                // als einzige nicht mitwaechst, gehoert sichtbar nicht mehr in ihre Welt.
                sizeDp = worldAvatarSizeDp,
                offset = next ?: current.offset
            )
        }

        // Play-Modus-Einstieg/-Ausstieg: der Avatar ist hier - anders als im normalen Dock -
        // dauerhaft sichtbar, nicht nur waehrend eine Erinnerungs-Animation laeuft (siehe
        // Klassendoku oben). Verlaesst man den Play-Modus, waehrend der Avatar nur ambient idlet
        // (keine offene Erinnerung, keine laufende Fuetter-Reaktion), verschwindet er wieder -
        // der normale Dock-Modus soll ungestoert bleiben, wie er es immer war.
        LaunchedEffect(playMode, watchOnly) {
            when {
                // "Nur Uhr" raeumt IMMER ab, auch mitten in einer offenen Erinnerung. Sonst
                // bliebe beim Umschalten genau die Figur stehen, die man gerade loswerden
                // wollte - und sie bliebe stehen, bis ihre Erinnerung ablaeuft.
                watchOnly -> {
                    avatarIdleJob?.cancel()
                    occupiedStation = null
                    avatar = null
                }
                playMode -> if (avatar == null) {
                    avatar = spawnAmbientAvatar()
                    // Beim Umschalten aus dem Spiel wurde ein laufender Ablauf sauber beendet.
                    // Beim Zurueckkehren wird seine Absicht wieder aufgenommen, statt einen neuen
                    // Zufall zu wuerfeln.
                    if (requestedTopic == null) {
                        requestedTopic = currentTopic ?: PlayPresence.topicFor(java.time.LocalDateTime.now())
                    }
                }
                avatar?.occurrenceId == null && avatar?.fed != true -> {
                    avatarIdleJob?.cancel()
                    occupiedStation = null
                    avatar = null
                }
            }
        }

        if (playMode) {
            // Langsamer Takt fuer die Umgebungsanimation (Dampf, Monitorflackern, Lampenpuls,
            // ziehende Wolke - siehe PlayScene.ambient). Bewusst traege: die Kulisse soll leben,
            // aber der Figur nicht die Aufmerksamkeit streitig machen.
            LaunchedEffect(Unit) {
                while (isActive) {
                    delay(SCENE_PHASE_TICK_MS)
                    scenePhase++
                }
            }

            // Besuchstakt: deutlich seltener als die eigenen Regungen. Kaeme staendig jemand
            // vorbei, waere es keine Begegnung mehr, sondern Verkehr.
            //
            // **Gemeldet als "die Besuche sind ploetzlich weg" - und der Fehler steckte im
            // Zeitpunkt der Pruefung.** Vorher wurde nach Ablauf der Wartezeit EINMAL nachgesehen,
            // ob es gerade passt: Ist die Figur in einem Zimmer, in dem man niemanden trifft
            // (Schlafzimmer, Bad, Kueche, Leseecke, Arbeitszimmer, eigene Ecke), oder sitzt sie
            // gerade irgendwo drin, dann fiel der Besuch ersatzlos aus - und die naechste Chance
            // kam erst anderthalb bis dreieinhalb Minuten spaeter. Ueber den Tag gerechnet passt
            // nur etwa jeder zweite Augenblick, und wer sitzt, faellt zusaetzlich heraus; aus
            // "alle zwei bis drei Minuten" wurden dadurch leicht zehn und mehr.
            //
            // Jetzt wird auf den passenden Augenblick GEWARTET statt ihn zu verpassen. Die
            // Wartezeit zaehlt damit die Zeit, in der ein Besuch moeglich WAERE - und das ist
            // auch die ehrlichere Lesart: Jemand kommt vorbei, wenn man draussen ist, nicht
            // wenn die Uhr es sagt.
            LaunchedEffect(Unit) {
                fun visitPossible(): Boolean {
                    val current = avatar ?: return false
                    return !current.fed && current.occurrenceId == null &&
                        occupiedStation == null && !avatarHidden &&
                        !routineRunning && !avatarWalking && !avatarSettling &&
                        PlayScene.allowsVisitors(currentPlace)
                }
                while (isActive) {
                    delay(
                        (visitIntervalFor(currentPlace).random() * PlayTimeLapse.paceFactor())
                            .toLong().coerceAtLeast(3_000L)
                    )
                    while (isActive && !visitPossible()) {
                        delay((VISIT_RETRY_MS * PlayTimeLapse.paceFactor()).toLong().coerceAtLeast(250L))
                    }
                    if (isActive) runVisit()
                }
            }

            // Die Mond-Szene beginnt und endet mit der Park-Nacht. Beim Beginn wird EINMAL
            // gewuerfelt, ob sie ueberhaupt stattfindet, und wie hoch der Mond steht - so sieht
            // nicht jede Nacht gleich aus.
            LaunchedEffect(renderedPlace, scenePhase / 40) {
                // Ueberall unter freiem Himmel, nicht nur im Park: Seit es Strasse und Wald gibt,
                // waere ein Mond, den es nur ueber einem einzigen der drei Orte gibt, willkuerlich
                // - der Mond steht am Himmel, und der ist ueber allen dreien derselbe.
                val isParkNight = PlayScene.isOutdoors(renderedPlace) &&
                    PlayAmbientActivity.currentDayPhase() == PlayAmbientActivity.DayPhase.NIGHT
                if (isParkNight && !moonMode) {
                    if (Random.nextFloat() < MOON_SCENE_CHANCE) {
                        // **Nur speichern, wenn nichts gespeichert IST.**
                        //
                        // Gemeldet als "die Mondszene passiert auch, wenn man im Raum ist - bei
                        // mir im Geschaeft". Der Weg dorthin: Diese Wirkung wird alle paar
                        // Sekunden neu ausgeloest (siehe die Schluessel), und eine laufende
                        // Rueckkehr-Bewegung wird dabei abgebrochen. Danach stand die Uhr
                        // irgendwo auf halbem Weg, und [savedClockOffset] war nie geleert. Beim
                        // naechsten Mondaufgang ueberschrieb diese Zeile dann die vom Nutzer
                        // gewaehlte Stelle mit einer Mondposition - die Uhr fand nicht mehr
                        // zurueck und stand von da an auch in Zimmern oben in der Ecke.
                        if (savedClockOffset == null) savedClockOffset = clockOffset
                        moonMode = true
                        // Mit der GROESSE des Mondes gerechnet, nicht mit der der Uhr: Er ist
                        // waehrend der Szene groesser (siehe MOON_SCALE), und mit dem kleineren
                        // Wert gerechnet ragte er rechts aus dem Bild.
                        val clockPx = with(density) { clockSizeDp.dp.toPx() } * MOON_SCALE
                        // **Ueber die ganze Breite.** Vorher stieg er nur zwischen 0,14 und 0,64
                        // auf - die rechte Bildhaelfte bekam nie einen Mond, und im Querformat
                        // stand er hartnaeckig links. Ein Himmel, der jede Nacht dieselbe Haelfte
                        // benutzt, ist kein Himmel.
                        val target = Offset(
                            x = ((maxWidthPx - clockPx) * (0.04f + Random.nextFloat() * 0.92f))
                                .coerceAtLeast(0f),
                            y = (maxHeightPx * (0.05f + Random.nextFloat() * 0.14f))
                                .coerceAtLeast(0f)
                        )
                        val from = clockOffset
                        animate(0f, 1f, animationSpec = tween(MOON_RISE_MS, easing = FastOutSlowInEasing)) { t, _ ->
                            clockOffset = Offset(
                                from.x + (target.x - from.x) * t,
                                from.y + (target.y - from.y) * t
                            )
                        }
                    }
                } else if (!isParkNight && moonMode) {
                    // Zurueck an die vom Nutzer gewaehlte Stelle - die blieb waehrend der Szene
                    // unangetastet (siehe die Sperre beim Speichern unten).
                    val back = savedClockOffset
                    moonMode = false
                    if (back != null) {
                        val from = clockOffset
                        // Im finally, weil diese Wirkung regelmaessig mitten in der Bewegung
                        // abgebrochen wird (die Schluessel aendern sich alle paar Sekunden). Ohne
                        // das blieb die Uhr auf halbem Weg stehen und galt weiterhin als
                        // "unterwegs" - der Anfang der ganzen Verschiebung. Ein Sprung an die
                        // richtige Stelle ist allemal besser als ein dauerhaft falscher Platz.
                        try {
                            animate(0f, 1f, animationSpec = tween(MOON_RISE_MS, easing = FastOutSlowInEasing)) { t, _ ->
                                clockOffset = Offset(
                                    from.x + (back.x - from.x) * t,
                                    from.y + (back.y - from.y) * t
                                )
                            }
                        } finally {
                            clockOffset = back
                            savedClockOffset = null
                        }
                    }
                }
            }

            // Die Sichel wandert langsam - so langsam, dass es beim Hinsehen nicht auffaellt und
            // beim laengeren Zuschauen doch.
            LaunchedEffect(moonMode) {
                while (moonMode && isActive) {
                    delay(MOON_PHASE_TICK_MS)
                    moonPhase++
                }
            }

            // Ortswechsel als Ueberblendung, siehe oben bei renderedPlace.
            LaunchedEffect(currentPlace) {
                if (currentPlace != renderedPlace) {
                    sceneFade.animateTo(0f, tween(SCENE_FADE_OUT_MS))
                    renderedPlace = currentPlace
                    sceneFade.animateTo(1f, tween(SCENE_FADE_IN_MS))
                }
            }

            // Die Uhr laesst sich mit zwei Fingern skalieren, und daran haengt die Zellgroesse
            // der ganzen Welt. Ohne dieses Nachziehen behielte der bereits stehende Avatar seine
            // alte Groesse, waehrend die Kulisse mitwaechst - Figur und Welt liefen im Massstab
            // auseinander, und er staende ploetzlich neben statt auf dem Boden.
            //
            // Die waagerechte Position wird als Bruchteil uebernommen statt neu gewuerfelt,
            // damit er beim Skalieren nicht quer durchs Bild springt.
            LaunchedEffect(worldAvatarSizeDp) {
                val current = avatar ?: return@LaunchedEffect
                if (current.fed || current.sizeDp == worldAvatarSizeDp) return@LaunchedEffect
                val oldBoundX = (maxWidthPx - with(density) { current.sizeDp.dp.toPx() }).coerceAtLeast(0f)
                val fraction = if (oldBoundX > 0f) current.offset.x / oldBoundX else 0.5f
                avatar = current.copy(
                    sizeDp = worldAvatarSizeDp,
                    offset = avatarSpot(fraction, worldAvatarPx, maxWidthPx, floorYPx, current.species)
                )
            }
        }

        // Faellige Erinnerung: Avatar spawnen, Uhr zeigt die Animation, Avatar blinzelt
        // idle daneben, bis entweder die Animation ausgelaufen ist (siehe unten, Avatar
        // verschwindet ungetrackt) oder die Kollisions-Erkennung unten die Fuetterung
        // ausloest (bricht beide Jobs hier vorzeitig ab).
        //
        // [onSubscription] holt eine beim Eintritt bereits laufende Erinnerung nach - denselben
        // Dienst tut der Startbildschirm (siehe OpenReminderLookup). Ohne das war der Dock-Modus
        // die einzige Ansicht, in der eine offene Erinnerung unsichtbar blieb: der Bus hat kein
        // Replay, wer also waehrend einer laufenden Erinnerung hierher wechselte, sah nur die Uhr
        // und konnte den Avatar nicht fuettern.
        //
        // Warum ausgerechnet onSubscription und kein eigener LaunchedEffect davor: der liefe, bevor
        // dieser Collector den Bus ueberhaupt abonniert hat - ein Bus ohne Replay wirft ein
        // Ereignis ohne Empfaenger ersatzlos weg, das Nachholen ginge also ins Leere. onSubscription
        // laeuft garantiert NACH dem Abonnieren und stellt nur diesem einen Collector zu, stoert
        // also auch den Startbildschirm nicht.
        //
        // Uebersetzt wird die Restdauer, nicht die volle: die Erinnerung soll hier zum selben
        // Zeitpunkt enden wie ueberall sonst. "Nonstop" bleibt "Nonstop".
        // In "Nur Uhr" wird hier gar nicht erst zugehoert. Eine Erinnerung wird im Dock
        // beantwortet, indem man die Uhr auf das Wesen zieht - ohne Wesen gaebe es keinen Weg
        // dazu, sie liefe ab und zaehlte anschliessend als verpasst. Ein Modus, der still
        // Fehlschlaege ins Pflegebuch schreibt, waere schlimmer als einer, der nichts tut.
        LaunchedEffect(watchOnly) {
            if (watchOnly) return@LaunchedEffect
            ReminderAnimationBus.events.onSubscription {
                val open = OpenReminderLookup.find(context, PresentCompanion.profileId(context))
                    ?: return@onSubscription
                val remainingSeconds = if (open.remainingMillis == ReminderOpenDuration.UNTIL_FED.toLong()) {
                    ReminderOpenDuration.UNTIL_FED
                } else {
                    (open.remainingMillis / 1000L).toInt().coerceAtLeast(1)
                }
                emit(open.event.copy(openDurationSeconds = remainingSeconds))
            }.collect { event ->
                // Solange noch eine Fuetter-Reaktion laeuft (avatar.fed == true, Uhr ausgeblendet
                // - siehe unten), muss die naechste Erinnerung warten, bevor ihre eigene
                // Anzeigedauer zu laufen beginnt. Sonst begaenne diese Anzeigedauer "im Dunkeln":
                // eine kurz nach einer anderen ausgeloeste Erinnerung koennte einen Teil ihrer
                // Zeit oder sogar die komplette Anzeigedauer verlieren, ohne je sichtbar (und
                // damit fuetterbar) gewesen zu sein - siehe dieselbe Begruendung in HomeScreen.
                snapshotFlow { avatar }.first { it == null || it.fed != true }

                clockAnimJob?.cancel()

                // Play-Modus: der Avatar existiert schon (siehe LaunchedEffect(playMode) oben)
                // und idlet bereits ununterbrochen weiter - er wird hier nur mit der feuernden
                // Erinnerung "verknuepft" (dadurch fuetterbar), seine Idle-Schleife und Position
                // bleiben unangetastet. Normaler Dock-Modus: unveraendert wie zuvor - eine neue
                // Instanz an zufaelliger Stelle, die nach Ablauf wieder verschwindet.
                if (playMode) {
                    val current = avatar
                    avatar = if (current != null) {
                        current.copy(
                            reminderId = event.reminderId,
                            occurrenceId = event.occurrenceId,
                            animationType = event.animationType,
                            libraryAnimationLabel = event.libraryAnimationLabel,
                            frames = event.frames
                        )
                    } else {
                        // Randfall: die Spawn-Effekt-Coroutine oben ist noch nicht durchgelaufen,
                        // wenn die allererste Erinnerung schon feuert (z.B. eine beim Eintritt
                        // bereits offene, per onSubscription nachgeholte Erinnerung) - dann hier
                        // nachholen statt darauf zu warten.
                        spawnAmbientAvatar(
                            reminderId = event.reminderId,
                            occurrenceId = event.occurrenceId,
                            animationType = event.animationType,
                            libraryAnimationLabel = event.libraryAnimationLabel,
                            frames = event.frames
                        )
                    }
                } else {
                    avatarIdleJob?.cancel()

                    val clockPx = with(density) { clockSizeDp.dp.toPx() }
                    // Deutlich kleiner als die Uhr statt gleich gross.
                    //
                    // Vorher bekam der Avatar exakt die Grundgroesse der Uhr (192dp). Zwei Quadrate
                    // dieser Groesse nebeneinander brauchen schon 384dp - mehr, als ein uebliches
                    // Telefon in der Breite hat, sobald noch ein Abstand dazukommt. Eine
                    // ueberschneidungsfreie Platzierung war damit auf vielen Geraeten schlicht
                    // unmoeglich, egal wie gut die Suche danach ist.
                    //
                    // Von der TATSAECHLICHEN Uhrgroesse abgeleitet, nicht von der Voreinstellung:
                    // wer die Uhr im Dock kleiner zieht, bekommt sonst einen Avatar, der ploetzlich
                    // groesser ist als sie.
                    val avatarSizeDp = (clockSizeDp * AVATAR_TO_CLOCK_RATIO)
                        .coerceIn(DockLayoutPrefs.MIN_SIZE_DP, DockLayoutPrefs.DEFAULT_SIZE_DP)
                    val avatarPx = with(density) { avatarSizeDp.dp.toPx() }
                    val boundX = (maxWidthPx - avatarPx).coerceAtLeast(0f)
                    val boundY = (maxHeightPx - avatarPx).coerceAtLeast(0f)
                    val spawnOffset = randomAvatarOffset(boundX, boundY, avatarPx, clockOffset, clockPx)
                    val species = AvatarSpeciesPrefs.get(context)
                    val mood = AvatarMoodSnapshot.forSpecies(context, species)
                    val idle = AvatarAnimations.idleSequence(species, mood)

                    avatar = AvatarState(
                        reminderId = event.reminderId,
                        occurrenceId = event.occurrenceId,
                        animationType = event.animationType,
                        libraryAnimationLabel = event.libraryAnimationLabel,
                        species = species,
                        offset = spawnOffset,
                        sizeDp = avatarSizeDp,
                        frame = idle.frames.first()
                    )

                    avatarIdleJob = launch {
                        // playTimed statt festem Takt: die Ruhe-Schleife jeder Spezies lebt von
                        // ihrem eigenen, ungleichmaessigen Rhythmus (lange Ruhelagen, kurze
                        // Regungen) - mit einheitlichem Takt abgespielt wirkte sie hektisch.
                        while (isActive) {
                            MatrixAnimator.playTimed(idle.frames, idle.holdsMs) { f ->
                                avatar = avatar?.copy(frame = f)
                            }
                        }
                    }
                }
                isPlayingAnimation = true
                // Volle, pro Erinnerung eingestellte Offen-Dauer inklusive "Nonstop" (siehe
                // ReminderOpenDuration) - hier ist der Dock-Modus, in dem das Fuettern des
                // Avatars die Animation jederzeit abbricht (Kollisions-Erkennung unten cancelt
                // diesen Job), eine endlose Animation kann also immer beendet werden.
                // Burn-in-Schutz bei langer Offen-Dauer: der Dock-Modus haelt den Bildschirm an
                // (FLAG_KEEP_SCREEN_ON, siehe MainActivity) und genau dafuer laesst sich die Uhr
                // ueberhaupt verschieben/skalieren - eine minutenlange oder endlose Animation an
                // fester Stelle waere aber exakt das Szenario, das dieser Schutz verhindern soll.
                // Deshalb wandert die Darstellung waehrend langer Erinnerungen langsam ueber den
                // Bildschirm. Rein optisch: driftOffset wird NICHT in DockLayoutPrefs
                // gespeichert, die vom User gewaehlte Position bleibt unangetastet.
                burnInDriftJob = if (needsBurnInDrift(event.openDurationSeconds)) {
                    launch { animateBurnInDrift(maxWidthPx, maxHeightPx, clockSizeDp, density) { driftOffset = it } }
                } else {
                    null
                }
                clockAnimJob = launch {
                    MatrixAnimator.play(
                        event.frames,
                        targetDurationMs = ReminderOpenDuration.toDurationMillis(event.openDurationSeconds),
                        frameDelayMs = MatrixAnimator.CLOCK_FRAME_DELAY_MS
                    ) { animationFrame = it }
                }
                clockAnimJob?.join()
                burnInDriftJob?.cancel()
                driftOffset = Offset.Zero

                // Ausgelaufen, ohne dass die Kollisions-Erkennung unten schon gefuettert
                // hat (die setzt fed=true und cancelt beide Jobs selbst).
                if (avatar?.fed != true) {
                    if (playMode) {
                        // Kein automatisches Ablegen in einen Speicherplatz - die ausgelaufene,
                        // unbeantwortete Erinnerung ist damit verloren, wie schon vor den
                        // Speicherplaetzen. Die bleiben ausschliesslich der bewussten Zieh-Geste
                        // vorbehalten (siehe saveToSlot). Anders als im normalen Dock verschwindet
                        // der Avatar hier nicht - nur die Verknuepfung zur ausgelaufenen
                        // Erinnerung faellt weg, seine Idle-Schleife (siehe oben, unangetastet
                        // seit dem Play-Modus-Einstieg bzw. der letzten Verknuepfung) laeuft
                        // einfach weiter.
                        avatar = avatar?.copy(
                            reminderId = null,
                            occurrenceId = null,
                            animationType = null,
                            libraryAnimationLabel = null,
                            frames = emptyList()
                        )
                    } else {
                        // Avatar verschwindet ungetrackt.
                        avatarIdleJob?.cancel()
                        avatar = null
                    }
                }
                isPlayingAnimation = false
                animationFrame = null
            }
        }

        // Herausgeloest, damit dieselbe Logik sowohl von der Drag-Kollision unten als auch von
        // der TalkBack-Zusatzaktion "Fuettern" am Avatar ausgeloest werden kann - per Drag laesst
        // sich nicht sinnvoll fuer einen Screenreader bedienen.
        fun feedAvatarNow() {
            val current = avatar ?: return
            // occurrenceId == null heisst: keine offene Erinnerung, nichts zu fuettern - im Play-
            // Modus der Normalzustand zwischen zwei Ausloesungen (der Avatar existiert dort
            // dauerhaft, siehe LaunchedEffect(playMode) oben).
            if (current.fed || current.occurrenceId == null) return
            if (feedingOccurrenceId == current.occurrenceId) return
            feedingOccurrenceId = current.occurrenceId
            scope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        AvatarFeeding.logFeedEvent(context, current.occurrenceId)
                    }
                    if (avatar?.occurrenceId != current.occurrenceId) return@launch
                    if (!result.isUiSuccess()) {
                        Log.w(DOCK_TAG, "Stale feed occurrenceId=${current.occurrenceId}")
                        clockAnimJob?.cancel()
                        isPlayingAnimation = false
                        animationFrame = null
                        avatar = if (playMode) {
                            avatar?.copy(
                                reminderId = null,
                                occurrenceId = null,
                                animationType = null,
                                libraryAnimationLabel = null
                            )
                        } else {
                            null
                        }
                        return@launch
                    }

                    // Erst der bestaetigte DB-Erfolg darf Ton, Zaehler und Reaktion ausloesen.
                    // Gefuettert wird immer frei stehend - unter der Bettdecke waere die Reaktion
                    // zur Haelfte unsichtbar.
                    occupiedStation = null
                    if (currentPlace == PlayScene.Place.SPORT && footballPhase != null &&
                        PlayFootballSkill.isFootballAnimation(
                            current.animationType, current.libraryAnimationLabel
                        )
                    ) {
                        PlayFootballSkill.learn(context, presenceProfileId)
                    }
                    avatar = current.copy(fed = true)
                    clockAnimJob?.cancel()
                    avatarIdleJob?.cancel()
                    isPlayingAnimation = false
                    animationFrame = null
                    PlaySound.play(context, current.species, PlayChime.Event.FEED, scope)
                    fedCount++
                    AvatarFeeding.playReaction(
                        species = current.species,
                        animationType = current.animationType,
                        libraryAnimationLabel = current.libraryAnimationLabel,
                        screenWidthPx = maxWidthPx,
                        screenHeightPx = maxHeightPx,
                        onFrame = { f -> avatar = avatar?.copy(frame = f) },
                        onOffset = { o -> avatar = avatar?.copy(offset = current.offset + o) }
                    )
                } catch (cancellation: kotlinx.coroutines.CancellationException) {
                    throw cancellation
                } catch (error: Exception) {
                    // Keine Erfolgsreaktion: die Room-Transaktion hat Markierung und XP gemeinsam
                    // zurueckgerollt. Der noch offene Avatar bleibt fuer einen Retry stehen.
                    Log.e(DOCK_TAG, "Feed transaction failed occurrenceId=${current.occurrenceId}", error)
                } finally {
                    // Zwingend im finally: solange avatar.fed gesetzt ist, wird die Uhr
                    // ausgeblendet. Bliebe der Avatar nach einem Abbruch stehen, waere die
                    // Uhr dauerhaft weg - der Dock-Modus zeigte dann nur noch eine
                    // eingefrorene Kreatur auf schwarzem Grund.
                    //
                    // Nur die EIGENE Erinnerung loeschen (Abgleich ueber occurrenceId): der
                    // Collector oben wartet zwar, bevor er eine neue Erinnerung uebernimmt, aber
                    // erst NACHDEM diese Reaktion hier zu Ende ist - ohne den Abgleich koennte
                    // dieses finally sonst genau in dem kurzen Fenster laufen, in dem der
                    // Collector den Avatar bereits auf die naechste Erinnerung gesetzt hat, und
                    // wuerde sie faelschlich wieder loeschen.
                    if (avatar?.fed == true && avatar?.occurrenceId == current.occurrenceId) {
                        if (playMode) {
                            // Der Avatar bleibt im Play-Modus stehen (an der Stelle, an die ihn
                            // die Reaktion zuletzt bewegt hat) statt wie im normalen Dock zu
                            // verschwinden - er geht nur zurueck in die Idle-Schleife.
                            avatar = avatar?.copy(
                                reminderId = null,
                                occurrenceId = null,
                                animationType = null,
                                libraryAnimationLabel = null,
                                fed = false
                            )
                            val mood = AvatarMoodSnapshot.forSpecies(context, current.species)
                            startAvatarIdleLoop(current.species, mood)
                            // Eine ECHTE Erinnerung darf dasselbe bewirken wie eine Bitte im
                            // Gespraech (siehe onAsk/requestedTopic weiter unten): das Wesen geht
                            // danach an den zum Thema passenden Ort ([PlayScene.forTopic]) und
                            // fuehrt dort dessen Tagesablauf-Routine aus ([PlayRoutines.forTopic]),
                            // statt einfach an der Stelle der Reaktion idle stehen zu bleiben.
                            // Reaktion selbst bleibt unberuehrt (laeuft VOR diesem Zweig, frei
                            // stehend und unverdeckt) - erst DANACH beginnt der Ortswechsel. Die
                            // Tageszeit wirkt dabei bereits mit: [moveToPlace] und die Routine
                            // selbst richten sich nach der aktuellen Tagesphase (siehe
                            // PlayAmbientActivity.currentDayPhase), dieselbe Logik wie beim
                            // autonomen Tagesablauf. Nur bei einem festen [AnimationType] moeglich
                            // - eine Bibliotheks-Animation ohne Thema kennt keinen Ort.
                            current.animationType?.let { requestedTopic = it }
                        } else {
                            avatar = null
                        }
                    }
                    if (feedingOccurrenceId == current.occurrenceId) feedingOccurrenceId = null
                }
            }
        }

        /**
         * Legt die gerade mit dem Avatar verknuepfte, noch unbeantwortete Erinnerung in
         * Speicherplatz [index] ab, statt sie zu fuettern - Gegenstueck zu [feedAvatarNow]. Der
         * Avatar behaelt seine Idle-Schleife und Position, verliert nur die Verknuepfung zur
         * Erinnerung (dieselbe Kopie wie im "ausgelaufen, ohne gefuettert zu werden"-Zweig unten).
         */
        fun saveToSlot(index: Int) {
            val current = avatar ?: return
            if (current.occurrenceId == null || current.fed) return
            if (slots.getOrNull(index) != null) return
            val reminderId = current.reminderId ?: return
            val saved = SavedAction(
                reminderId = reminderId,
                occurrenceId = current.occurrenceId,
                animationType = current.animationType,
                libraryAnimationLabel = current.libraryAnimationLabel,
                frames = current.frames.ifEmpty { listOf(current.frame) }
            )
            ActionSlotStore.write(context, actionSlotProfileId, index, saved)
            slots = slots.toMutableList().also { it[index] = saved }
            clockAnimJob?.cancel()
            isPlayingAnimation = false
            animationFrame = null
            avatar = current.copy(
                reminderId = null,
                occurrenceId = null,
                animationType = null,
                libraryAnimationLabel = null,
                frames = emptyList()
            )
            if (!OnboardingPrefs.hasUsedActionSlot(context)) {
                OnboardingPrefs.markActionSlotUsed(context)
            }
        }

        /**
         * Wendet eine zuvor abgelegte Aktion aus Speicherplatz [index] auf den Avatar an, indem
         * sie ihm kurz "verknuepft" wird und danach exakt derselbe Weg wie [feedAvatarNow] laeuft -
         * dieselbe DB-Schreibung, dieselbe Reaktion, dasselbe Aufraeumen danach. Nur erreichbar,
         * wenn der Avatar gerade KEINE eigene offene Erinnerung traegt: sonst wuerde deren
         * Verknuepfung hier verloren gehen, ohne dass sie je beantwortet oder archiviert wurde.
         */
        fun feedFromSlot(index: Int) {
            val saved = slots.getOrNull(index) ?: return
            val current = avatar ?: return
            if (feedingOccurrenceId != null) return
            if (current.occurrenceId != null && !current.fed) return
            avatar = current.copy(
                reminderId = saved.reminderId,
                occurrenceId = saved.occurrenceId,
                animationType = saved.animationType,
                libraryAnimationLabel = saved.libraryAnimationLabel,
                fed = false,
                frames = saved.frames
            )
            ActionSlotStore.write(context, actionSlotProfileId, index, null)
            slots = slots.toMutableList().also { it[index] = null }
            feedAvatarNow()
        }

        // Laeuft bei jeder Aenderung von clockOffset neu an, also auch mitten in einer
        // laufenden Drag-Geste (nicht erst am Gesten-Ende) - dadurch wird die Kollision
        // erkannt, sobald sich Uhr und Avatar beim Ziehen beruehren.
        LaunchedEffect(clockOffset) {
            val current = avatar
            if (current == null || current.fed || current.occurrenceId == null) return@LaunchedEffect
            val clockPx = with(density) { clockSizeDp.dp.toPx() }
            val avatarPx = with(density) { current.sizeDp.dp.toPx() }
            if (isColliding(clockOffset, clockPx, current.offset, avatarPx)) {
                feedAvatarNow()
                return@LaunchedEffect
            }
            if (!playMode) return@LaunchedEffect
            val freeSlotIndex = (0 until ACTION_SLOT_COUNT).firstOrNull { index ->
                slots.getOrNull(index) == null &&
                    isColliding(clockOffset, clockPx, slotOffsetPx(index), slotSizePx)
            }
            if (freeSlotIndex != null) {
                saveToSlot(freeSlotIndex)
            }
        }

        // Play-Modus: kleine autonome Regungen zwischen zwei echten Ausloesungen (die je nach
        // Spezies/Level mehrere Minuten auseinander liegen koennen, siehe PlayGamePlan) - sonst
        // stuende der Avatar dazwischen nur unveraendert idle da. Rein kosmetisch (keine
        // Erinnerung wird beantwortet, kein XP), aber PERFORM zeigt einen an die Tageszeit
        // gekoppelten Tagesablauf: siehe PlayAmbientActivity fuer die Themen-/Tagesphasen-Logik.
        if (playMode) {
            // **[requestedTopic] gehoert bewusst in den SCHLUESSEL dieser Schleife.**
            //
            // Eine Bitte aus dem Gespraech ("dann mach das doch jetzt") soll sofort wirken und
            // nicht erst, wenn die naechste Pause abgelaufen ist - die kann Minuten dauern. Weil
            // eine Aenderung am Schluessel die laufende Coroutine abbricht und neu startet, hoert
            // er im selben Moment auf, was er gerade tat, und beginnt das Erbetene. Genau so
            // verhaelt sich jemand, den man anspricht.
            //
            // Ein zweiter, nebenher laufender Anstoss waere die Alternative gewesen und die
            // schlechtere: Zwei Ablaeufe gleichzeitig schieben dieselbe Figur an zwei Orte.
            LaunchedEffect(avatar?.species, requestedTopic) {
                val species = avatar?.species ?: return@LaunchedEffect

                requestedTopic?.let { topic ->
                    moveToPlace(PlayScene.forTopic(topic), species)
                    runRoutine(
                        PlayRoutines.forTopic(
                            topic = topic,
                            needsShopping = PlayPantry.isEmpty(context) && PlayWallet.canAfford(context),
                            footballTrickLearned = PlayFootballSkill.isLearned(context, presenceProfileId)
                        ),
                        species
                    )
                    // Zuruecksetzen startet die Schleife ein letztes Mal - dann ohne Bitte, und
                    // von da an laeuft wieder der gewoehnliche Tagesablauf.
                    requestedTopic = null
                    return@LaunchedEffect
                }

                while (isActive) {
                    delay(PlayAmbientActivity.nextPauseMillis())
                    val current = avatar
                    // Nur ausserhalb einer offenen Erinnerung und ausserhalb einer Fuetter-
                    // Reaktion - sonst wuerde eine autonome Regung mit der Kollisions-/
                    // Fuetterlogik oben konkurrieren.
                    if (current == null || current.fed || current.occurrenceId != null) continue
                    when (PlayAmbientActivity.nextAction()) {
                        PlayAmbientActivity.Action.FLOURISH -> {
                            avatarIdleJob?.cancel()
                            val flourish = AvatarAnimations.tapReaction(species)
                            MatrixAnimator.playTimed(flourish.frames, flourish.holdsMs) { f ->
                                avatar = avatar?.copy(frame = f)
                            }
                            val mood = AvatarMoodSnapshot.forSpecies(context, species)
                            startAvatarIdleLoop(species, mood)
                        }
                        PlayAmbientActivity.Action.FIDGET -> {
                            // Die haeufigste Regung und absichtlich die unscheinbarste: Sich
                            // umsehen, strecken, gaehnen. Nichts davon bedeutet etwas - genau
                            // deshalb wirkt es wie jemand, der einfach da ist.
                            avatarIdleJob?.cancel()
                            val fidget = AvatarAnimations.fidgetSequence(species, PlayAmbientActivity.nextFidget())
                            MatrixAnimator.playTimed(fidget.frames, fidget.holdsMs) { f ->
                                avatar = avatar?.copy(frame = f)
                            }
                            val mood = AvatarMoodSnapshot.forSpecies(context, species)
                            startAvatarIdleLoop(species, mood)
                        }
                        PlayAmbientActivity.Action.WANDER -> {
                            // Entlang des Bodens und in der Naehe seiner aktuellen Ecke, nicht
                            // mehr quer ueber den ganzen Bildschirm: Seit es eine Kulisse gibt,
                            // ist "wo er sich aufhaelt" eine Aussage - ein Avatar, der vom Bett
                            // zufaellig in die freie Bildmitte springt, macht sie wieder kaputt.
                            // Nur ein kleiner Ausschlag um seinen Platz herum: groesser gewaehlt,
                            // und er wandert bei schmalem Bild in Fenster oder Regal hinein
                            // (siehe die Geometrie-Begruendung in PlayScene.placementsFor).
                            val anchor = PlayScene.screenFraction(
                                (PlayScene.avatarAnchorX(currentPlace) +
                                    Random.nextFloat() * 0.16f - 0.08f).coerceIn(0.02f, 0.98f),
                                sceneWidthCells
                            )
                            val destination = avatarSpot(
                                anchorX = anchor,
                                avatarPx = with(density) { current.sizeDp.dp.toPx() },
                                maxWidthPx = maxWidthPx,
                                floorYPx = floorYPx,
                                species = current.species
                            )
                            if (walkAvatarTo(destination)) {
                                startAvatarIdleLoop(species, AvatarMoodSnapshot.forSpecies(context, species))
                            }
                        }
                        PlayAmbientActivity.Action.PERFORM -> {
                            // Die eigentliche Tagesablauf-Szene: dieselbe Reaktions-Bibliothek wie
                            // bei einer echten gefuetterten Erinnerung (siehe AvatarFeeding/
                            // feedAvatarNow), nur autonom ausgeloest und ohne dass irgendetwas
                            // "gefuettert" wird - der Avatar kuemmert sich gerade selbst darum.
                            //
                            // Die Themenwahl beruecksichtigt zusaetzlich zur Tageszeit, was der
                            // Nutzer heute per echter Erinnerung mit Tagesziel noch nicht erreicht
                            // hat (siehe PlayHabitSignal) - Fuettern wirkt sich dadurch sichtbar
                            // auf das aus, was der Avatar von sich aus tut, nicht nur auf seine
                            // Stimmung (AvatarMood).
                            // Die offenen Routinen des NUTZERS gewichten, was das Wesen von sich
                            // aus tut. "Offen" heisst dabei: von DIESEM Wesen heute noch nicht
                            // erlebt - deshalb geht hier das anwesende Wesen hinein, waehrend
                            // PlayHabitSignal die Ziele selbst beim Routinen-Besitzer holt.
                            // **Und was der Nutzer selbst gesagt hat, zaehlt mit.** Wer auf die
                            // Frage "worauf soll ich achten" geantwortet hat, sieht ihn danach
                            // oefter genau das tun - das ist der ganze Unterschied zwischen einer
                            // Antwort und einem Eintrag in einer Datei (siehe PlayUserProfile).
                            val boostedTopics =
                                PlayHabitSignal.underfulfilledTopics(context, PresentCompanion.profileId(context)) +
                                    setOfNotNull(PlayUserProfile.focusTopic(context))
                            // Vorrang vor allem anderen: Ist nichts mehr da UND kein Geld fuer
                            // einen Einkauf, muss gearbeitet werden. Das ist die Stelle, an der
                            // die Welt den Zufall ueberstimmt - und der Grund, warum man ihm beim
                            // Zuschauen abnimmt, dass er zur Arbeit GEHT statt dort zu erscheinen.
                            val mustEarn = PlayPantry.isEmpty(context) && !PlayWallet.canAfford(context)
                            val topic = if (mustEarn) {
                                AnimationType.WORK
                            } else {
                                // MIT dem jetzigen Ort: Die Figur bleibt dann eher, wo sie ist,
                                // statt bei jeder Regung das Zimmer zu wechseln (siehe
                                // PlayAmbientActivity.nextTopic).
                                //
                                // Aber nur BEGRENZT oft hintereinander. Ein Zuschlag ohne Ende
                                // haette den umgekehrten Fehler erzeugt: Wer im Wohnzimmer sitzt,
                                // bekommt dort dauernd Rueckenwind fuers Bleiben, und die Figur
                                // kaeme kaum noch vor die Tuer - genau der Zustand, gegen den die
                                // Strasse und der Wald angelegt wurden. Zwei-, dreimal verweilen
                                // ist ein Aufenthalt, fuenfmal ist ein Hausarrest.
                                PlayAmbientActivity.nextTopic(
                                    boostedTopics = boostedTopics,
                                    stayAt = currentPlace.takeIf { stayedRounds < PlayAmbientActivity.MAX_STAY_ROUNDS },
                                    // **Und wohin er sich entwickelt hat** (siehe PlayPath): Der
                                    // Pfad faerbt, was er von sich aus tut. Erst dadurch ist die
                                    // Entwicklung etwas, das man SIEHT, statt etwas, das im
                                    // Gespraech behauptet wird.
                                    leaning = leaningTopics
                                )
                            }

                            // Erst den Ort wechseln, dann HINGEHEN, dann handeln - in dieser
                            // Reihenfolge liest sich die Szene als Absicht ("er geht zum Bett und
                            // legt sich hin") statt als Zufall ("er schlaeft, wo er gerade steht").
                            // Das ist der eigentliche Gewinn der Kulisse: die Handlung bekommt
                            // einen Ort, an den sie gehoert.
                            // Ortswechsel und Hinweg laufen ABSICHTLICH gleichzeitig. Vorher
                            // wartete der Avatar erst die volle Ueberblendung ab und ging dann
                            // los - ein toter Takt von gut einer halben Sekunde, in dem sich
                            // nichts ruehrte. Jetzt loest der Wechsel nur die Ueberblendung aus
                            // (siehe LaunchedEffect(currentPlace) oben, laeuft in eigener
                            // Coroutine), und waehrend er geht, blendet der alte Raum weg und der
                            // neue um ihn herum auf. Genau darin liegt der Ortswechsel.
                            val place = PlayScene.forTopic(topic)
                            currentTopic = topic
                            // **Er sagt, was er vorhat** - selten, kurz und nie nachts (siehe
                            // PlaySpeech). Das beantwortet die Frage, die man sich beim Zuschauen
                            // ohnehin stellt, im selben Augenblick, in dem sie aufkommt.
                            PlaySpeech.lineFor(topic, PlayAmbientActivity.currentDayPhase())?.let { line ->
                                spokenIsOpenHabit = topic in boostedTopics
                                spokenLine = line
                            }
                            stayedRounds = if (place == currentPlace) stayedRounds + 1 else 0
                            moveToPlace(place, species)

                            // Nicht mehr EINE Animation, sondern ein mehrschrittiger Ablauf:
                            // hingehen, benutzen, handeln, verweilen, aufstehen (siehe
                            // PlayRoutine). Erst dadurch setzt sich die Figur mit ihrer Umgebung
                            // auseinander, statt neben den Moebeln zu agieren.
                            runRoutine(
                                PlayRoutines.forTopic(
                                    topic = topic,
                                    needsShopping = PlayPantry.isEmpty(context) && PlayWallet.canAfford(context),
                                    footballTrickLearned = PlayFootballSkill.isLearned(context, presenceProfileId)
                                ),
                                species
                            )
                        }
                    }
                }
            }
        }

        // Was gerade zu sehen ist, als Beschreibung - Kulisse, Figur, Uhr, Getragenes, Gast.
        //
        // An EINER Stelle, weil sie zweimal gebraucht wird: Der Film sammelt sie fuenfzehnmal je
        // Sekunde, der Schnappschuss genau einmal. Beide muessen dasselbe festhalten, sonst zeigte
        // ein Bild aus der Sammlung etwas anderes als ein Film derselben Szene.
        fun describeScreen(): PlayClipRenderer.Frame? {
            val current = avatar ?: return null
            if (sceneCellPx <= 0f) return null
            val avatarPx = with(density) { current.sizeDp.dp.toPx() }
            val boundX = (maxWidthPx - avatarPx).coerceAtLeast(1f)
            val clockPx = with(density) { clockSizeDp.dp.toPx() }
            return PlayClipRenderer.Frame(
                place = renderedPlace,
                species = current.species,
                dayPhase = PlayAmbientActivity.currentDayPhase(),
                avatarFrame = current.frame,
                avatarAnchorX = (current.offset.x / boundX).coerceIn(0f, 1f),
                scenePhase = scenePhase,
                station = occupiedStation ?: activeStation,
                lampOn = lampOn,
                tvOn = tvOn,
                clockFrame = if (current.fed) null else frame,
                clockLeftFraction = (clockOffset.x / maxWidthPx).coerceIn(0f, 1f),
                clockTopFraction = (clockOffset.y / maxHeightPx).coerceIn(0f, 1f),
                clockSizeFraction = (clockPx / maxWidthPx).coerceIn(0.05f, 1f),
                carried = carried,
                visitorFrame = visitor?.frame,
                visitorSpecies = visitor?.species,
                visitorAnchorX = visitor?.let {
                    (it.offset.x / boundX).coerceIn(0f, 1f)
                } ?: 0f
            )
        }

        // Der Mitschnitt. Festgehalten werden nur Beschreibungen, keine Bilder (siehe
        // PlayClipRecorder.Session), deshalb kostet eine lange Aufnahme kaum Speicher.
        LaunchedEffect(clipSession) {
            val session = clipSession ?: return@LaunchedEffect
            while (isActive && !session.isFull) {
                describeScreen()?.let {
                    session.add(it)
                    clipSeconds = session.seconds
                }
                delay(PlayClipRecorder.SAMPLE_INTERVAL_MS)
            }
            // Obergrenze erreicht - von selbst beenden, statt weiter mitzulaufen.
            if (session.isFull) {
                clipSession = null
                clipEncoding = 0f
                val done = withContext(Dispatchers.Default) {
                    PlayClipRecorder.encode(context, session) { clipEncoding = it }
                }
                clipEncoding = -1f
                clipResult = done
            }
        }

        // Direkt nach der Aufnahme das Teilen anbieten - wer aufnimmt, will meistens gleich
        // weitergeben. Die Datei liegt ohnehin in der Sammlung, es geht also nichts verloren,
        // wenn man ablehnt.
        LaunchedEffect(clipResult) {
            clipResult?.let { file ->
                shareClipFile(context, file)
                clipResult = null
            }
        }

        // Die Kulisse - ZUERST gezeichnet und damit hinter Uhr und Avatar. Sie faengt bewusst
        // keine Gesten ab (siehe PlaySceneView), das Ziehen der Uhr auf den Avatar bleibt also
        // unveraendert die einzige Interaktion auf diesem Bildschirm.
        if (playMode) {
            val sceneCells = remember(
                renderedPlace, scenePhase, sceneWidthCells, floorYCells, sceneFade.value,
                lampOn, tvOn, activeStation, avatar?.species,
                // Sonst bliebe die Kulisse stehen, wie sie war, bis sich zufaellig etwas anderes
                // aendert - und das neu erworbene Stueck taucht erst beim naechsten Ortswechsel
                // auf statt in dem Moment, in dem es dazukommt.
                acquisitions
            ) {
                PlayScene.build(
                    place = renderedPlace,
                    phase = scenePhase,
                    widthCells = sceneWidthCells,
                    floorY = floorYCells,
                    dayPhase = PlayAmbientActivity.currentDayPhase(),
                    fade = sceneFade.value,
                    lampOn = lampOn,
                    tvOn = tvOn,
                    activeStation = activeStation,
                    species = avatar?.species ?: AvatarSpeciesPrefs.get(context),
                    // Was er sich im Lauf seiner Entwicklung zugelegt hat (siehe PlayPath) - der
                    // Teil des Fortschritts, den man nicht liest, sondern sieht.
                    acquisitions = acquisitions
                )
            }
            PlaySceneView(
                cells = sceneCells,
                cellPx = sceneCellPx,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Sobald gefuettert wurde, verschwindet die Uhr fuer die Dauer der Reaktion.
        //
        // Vorher blieb sie stehen, wo der Finger sie hingezogen hatte - also genau ueber dem
        // Avatar - und ihr runder Puck ueberdeckte einen Teil der Reaktion. Das sah nicht nach
        // Absicht aus, sondern nach zwei Ebenen, die nichts voneinander wissen. Inhaltlich passt
        // das Verschwinden ohnehin besser: der Avatar hat die Erinnerung ja gerade
        // aufgenommen, sie ist damit weg. Danach kommt die Uhr an ihrer Position zurueck.
        //
        // Bewusst komplett aus der Komposition genommen statt nur durchsichtig geschaltet - eine
        // unsichtbare Uhr wuerde weiter Tipp- und Ziehgesten schlucken.
        if (avatar?.fed != true) {
            // Siehe HomeScreen fuer dieselbe Begruendung: ohne Beschreibung haette TalkBack keine
            // Information darueber, was hier gezeichnet wird.
            val activeAvatar = avatar
            // occurrenceId != null statt bloss != null: im Play-Modus existiert der Avatar jetzt
            // dauerhaft (siehe LaunchedEffect(playMode) oben), auch ohne offene Erinnerung - ohne
            // diese Praezisierung haette die Uhr hier staendig eine Erinnerung angesagt, obwohl
            // sie nur die aktuelle Uhrzeit zeigt.
            val clockContentDescription = if (activeAvatar?.occurrenceId != null) {
                val topicLabel = activeAvatar.libraryAnimationLabel
                    ?: activeAvatar.animationType?.let { stringResource(it.labelRes) }
                    ?: stringResource(R.string.a11y_reminder_generic)
                stringResource(R.string.a11y_clock_reminder, topicLabel)
            } else if (moonMode) {
                stringResource(R.string.a11y_clock_moon)
            } else {
                val now = LocalTime.now()
                stringResource(R.string.a11y_clock_time, "%02d:%02d".format(now.hour, now.minute))
            }
            // Der Mond waechst weich auf seine Groesse - im selben Zug, in dem er aufsteigt.
            val moonScale by animateFloatAsState(
                targetValue = if (moonMode) MOON_SCALE else 1f,
                animationSpec = tween(MOON_RISE_MS, easing = FastOutSlowInEasing),
                label = "moon"
            )
            SimulatedMatrixView(
                frame = frame,
                contentDescription = clockContentDescription,
                modifier = Modifier
                    .size((clockSizeDp * moonScale).dp)
                    // driftOffset ist die rein optische Burn-in-Verschiebung und wird nur hier
                    // draufgerechnet - Kollisionspruefung und Speicherung nutzen weiter clockOffset,
                    // damit sich weder das Fuettern noch die gemerkte Position dadurch aendert.
                    .offset {
                        IntOffset(
                            (clockOffset.x + driftOffset.x).roundToInt(),
                            (clockOffset.y + driftOffset.y).roundToInt()
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onExit() })
                    }
                    .pointerInput(Unit) {
                        // **Die Bildschirmmasse muessen nachgezogen werden.**
                        //
                        // Hier stand: "fuer die Lebensdauer dieses Screens praktisch konstant
                        // (keine Rotation waehrend des Andockens vorgesehen)". Die Annahme war
                        // falsch - das Drehen ist eine Einstellung (siehe OrientationPrefs), und
                        // im Dock-Modus liegt das Geraet gerade dann auf dem Tisch, wenn man es
                        // dreht.
                        //
                        // `pointerInput(Unit)` startet genau einmal und schliesst die damaligen
                        // Werte ein. Nach dem Drehen ins Querformat klemmte die Geste deshalb
                        // weiter gegen die HOCHFORMAT-Breite: Die Uhr liess sich nur bis dorthin
                        // schieben und blieb dann stehen, als waere der Bildschirm dort zu Ende.
                        //
                        // clockSizeDp/clockOffset waren nie betroffen - sie sind State-gestuetzt
                        // und liefern beim Lesen immer den aktuellen Wert. Genau deshalb ist der
                        // Fehler nur den Massen passiert, die als einfache Zahlen danebenstanden.
                        detectTransformGestures(panZoomLock = false) { _, pan, zoom, _ ->
                            val newSize = (clockSizeDp * zoom)
                                .coerceIn(DockLayoutPrefs.MIN_SIZE_DP, DockLayoutPrefs.MAX_SIZE_DP)
                            // Nur schreiben, wenn sich wirklich etwas aendert. Beim reinen
                            // Verschieben liefert die Geste ein Zoom von fast genau 1, also eine
                            // Groesse, die sich in der zehnten Nachkommastelle unterscheidet -
                            // sichtbar ist das nichts, aber jede Zuweisung stoesst alles an, was
                            // an der Uhrgroesse haengt. Genau daraus entstand die "Grenze", ueber
                            // die sich die Uhr nicht schieben liess.
                            if (kotlin.math.abs(newSize - clockSizeDp) > 0.05f) {
                                clockSizeDp = newSize
                            }
                            val clockPxNow = with(density) { newSize.dp.toPx() }
                            val boundX = (currentWidthPx.value - clockPxNow).coerceAtLeast(0f)
                            val boundY = (currentHeightPx.value - clockPxNow).coerceAtLeast(0f)
                            clockOffset = Offset(
                                (clockOffset.x + pan.x).coerceIn(0f, boundX),
                                (clockOffset.y + pan.y).coerceIn(0f, boundY)
                            )
                        }
                    }
            )
        }

        // **Ein leiser Hinweis, dass Erinnerungen gerade ruhen.**
        //
        // Ohne ihn waere "Nur Uhr" ein Modus, in dem die App still aufhoert zu tun, wofuer man sie
        // installiert hat - und wer ihn abends einschaltet und morgens vergisst, wundert sich
        // tagelang, warum nichts mehr kommt. So gedaempft wie moeglich, damit er eine
        // Nachttisch-Uhr nicht stoert, aber vorhanden: Eine Funktion abzuschalten darf nie
        // unsichtbar sein.
        if (watchOnly) {
            Text(
                stringResource(R.string.watch_only_note),
                color = Color(0xFF3A3833),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
        }

        // **Der Aufstieg - und sonst nichts mehr.**
        //
        // Hier stand dauerhaft eine Zeile mit Stufe, Erfahrung, Muenzen und Vorrat. Sie war der
        // letzte Ueberrest der Bauweise "alles ist immer zu sehen", genau wie der Tagesstand am
        // oberen Rand des Startbildschirms, und sie hatte denselben Nachteil: Wer den Stand
        // ohnehin dastehen sieht, fragt seinen Begleiter nicht mehr danach. Nachzulesen ist er
        // jetzt im Gespraech (siehe PlayTalk.Offer.ShowGame), dort mit einem Balken statt mit
        // Ziffern.
        //
        // Der AUFSTIEG bleibt: Er ist kein Zustand, sondern ein Ereignis. Etwas, das man erreicht
        // hat, muss in dem Moment zu sehen sein, in dem es geschieht - es hinterher nachschlagen
        // zu koennen ist kein Ersatz dafuer. Er zeigt sich kurz und verschwindet von selbst.
        if (playMode) {
            val playViewModel = androidx.lifecycle.viewmodel.compose.viewModel<PlayModeViewModel>()
            val playState by playViewModel.state.collectAsStateWithLifecycle()
            if (playState.showLevelUp) {
                Text(
                    stringResource(R.string.playmode_level_up, playState.level),
                    color = Color(0xFF7FD1A6),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                )
                // Erst nach einem Moment bestaetigen, sonst waere der Glueckwunsch im selben
                // Frame wieder verschwunden, in dem er erscheint.
                LaunchedEffect(playState.level) {
                    avatar?.species?.let { species ->
                        PlaySound.play(context, species, PlayChime.Event.LEVEL_UP, scope)
                    }
                    delay(LEVEL_UP_MESSAGE_MS)
                    playViewModel.acknowledgeLevelUp()
                }
            }
        }

        visitor?.let { guest ->
            AvatarSpriteView(
                frame = guest.frame,
                showBackground = false,
                // Durchgehend zurueckgenommen: So bleibt der eigene Avatar auch dann die hellste
                // Figur im Bild, wenn die beiden sich ueberdecken - und das laesst sich beim
                // Aneinander-Vorbeigehen nicht immer vermeiden.
                brightnessScale = VISITOR_DIM,
                contentDescription = stringResource(R.string.a11y_visitor, stringResource(guest.species.labelRes)),
                modifier = Modifier
                    .width(guest.sizeDp.dp)
                    .height(guest.sizeDp.dp * AvatarGeometry.HEIGHT / AvatarGeometry.SIZE)
                    .offset { IntOffset(guest.offset.x.roundToInt(), guest.offset.y.roundToInt()) }
            )
        }

        // Aufnahmeknopf - nur im Play-Modus und nur, wenn in den Einstellungen freigeschaltet.
        // Oben RECHTS: Die Uhr laesst sich frei plazieren und die Figur laeuft am Boden entlang;
        // die obere rechte Ecke ist die einzige Stelle, an der ein festes Bedienelement keiner
        // von beiden ins Gehege kommt.
        if (playMode && ClipPrefs.isEnabled(context)) {
            val recordLabel = stringResource(R.string.a11y_record_clip)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .size(40.dp)
                    .semantics { contentDescription = recordLabel }
                    .pointerInput(clipSession, clipEncoding) {
                        detectTapGestures(onTap = {
                            if (clipEncoding >= 0f) return@detectTapGestures
                            val running = clipSession
                            if (running == null) {
                                clipSeconds = 0
                                clipSession = PlayClipRecorder.Session()
                            } else {
                                // Beenden: Aufnahme stoppen und zusammenrechnen.
                                clipSession = null
                                scope.launch {
                                    clipEncoding = 0f
                                    val done = withContext(Dispatchers.Default) {
                                        PlayClipRecorder.encode(context, running) { clipEncoding = it }
                                    }
                                    clipEncoding = -1f
                                    clipResult = done
                                }
                            }
                        })
                    }
            ) {
                // Kreis zum Starten, Quadrat zum Beenden - dieselbe Zeichensprache wie auf
                // jeder Kamera. Waehrend des Zusammenrechnens zeigt der Ring den Fortschritt.
                val recording = clipSession != null
                val encoding = clipEncoding >= 0f
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val r = size.minDimension / 2f
                    drawCircle(
                        color = Color(0xFF6E6A63),
                        radius = r,
                        style = Stroke(width = r * 0.16f)
                    )
                    when {
                        encoding -> drawArc(
                            color = Color(0xFF7FD1A6),
                            startAngle = -90f,
                            sweepAngle = 360f * clipEncoding,
                            useCenter = false,
                            style = Stroke(width = r * 0.16f)
                        )
                        recording -> {
                            // Quadrat = "beenden".
                            val half = r * 0.42f
                            drawRect(
                                color = Color(0xFFE06C6C),
                                topLeft = Offset(center.x - half, center.y - half),
                                size = androidx.compose.ui.geometry.Size(half * 2, half * 2)
                            )
                        }
                        else -> drawCircle(color = Color(0xFF8A8A8A), radius = r * 0.5f)
                    }
                }
            }
            // Laufzeit neben dem Knopf - sonst weiss man nicht, wie lang die Aufnahme schon ist.
            if (clipSession != null) {
                Text(
                    "$clipSeconds s",
                    color = Color(0xFFE06C6C),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 64.dp, end = 20.dp)
                )
            }

            // Der Schnappschuss - UNTER dem Aufnahmeknopf, nicht daneben.
            //
            // Beide gehoeren zusammen und sollen als Paar lesbar sein; nebeneinander waeren sie
            // aber zwei gleichrangige Knoepfe in der Ecke, und man muesste jedes Mal kurz
            // nachdenken, welcher welcher ist. Untereinander bleibt die Ecke ruhig, und der
            // Daumen findet den zweiten blind. Waehrend eine Aufnahme zusammengerechnet wird,
            // verschwindet er: Der Zeichner ist dann beschaeftigt.
            if (clipEncoding < 0f) {
                val snapshotLabel = stringResource(R.string.a11y_snapshot)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 68.dp, end = 20.dp)
                        .size(40.dp)
                        .semantics { contentDescription = snapshotLabel }
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = {
                                val described = describeScreen() ?: return@detectTapGestures
                                scope.launch {
                                    // Zeichnen und Schreiben abseits des Bildschirm-Fadens: Ein
                                    // Bild in voller Aufloesung zu erzeugen dauert lange genug,
                                    // dass die Figur sonst sichtbar stockte - ausgerechnet in dem
                                    // Moment, den man festhalten wollte.
                                    val file = withContext(Dispatchers.Default) {
                                        PlaySnapshot.capture(context, described)
                                    }
                                    if (file != null) {
                                        snapshotFlash = true
                                        snapshotCount++
                                    }
                                }
                            })
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val r = size.minDimension / 2f
                        // Ein Kamerasucher: Rahmen mit Linse. Bewusst NICHT derselbe Kreis wie
                        // beim Film - zwei gleich aussehende Knoepfe uebereinander waeren die
                        // schlechteste aller Anordnungen.
                        drawRect(
                            color = Color(0xFF6E6A63),
                            topLeft = Offset(center.x - r * 0.85f, center.y - r * 0.62f),
                            size = androidx.compose.ui.geometry.Size(r * 1.7f, r * 1.24f),
                            style = Stroke(width = r * 0.16f)
                        )
                        drawCircle(color = Color(0xFF8A8A8A), radius = r * 0.34f)
                    }
                }
            }

        }

        avatar?.takeIf { !avatarHidden }?.let { current ->
            val feedActionLabel = stringResource(R.string.a11y_feed_action)
            // "wartet aufs Fuettern" stimmt nur, solange tatsaechlich eine Erinnerung offen ist -
            // im Play-Modus idlet der Avatar die meiste Zeit ohne eine (siehe occurrenceId).
            val avatarContentDescription = if (current.occurrenceId != null) {
                stringResource(R.string.a11y_avatar_waiting, stringResource(current.species.labelRes))
            } else {
                stringResource(current.species.labelRes)
            }
            AvatarSpriteView(
                frame = current.frame,
                brightnessScale = avatarDim.value,
                // OHNE eigene Flaeche - und das ist im Play-Modus zwingend, nicht kosmetisch:
                // [AvatarSpriteView] fuellt sein Sprite-Quadrat sonst schwarz aus. Solange der
                // Dock-Modus nur aus schwarzer Flaeche und Uhr bestand, war das unsichtbar. Seit
                // hinter der Figur ein Zimmer steht, stanzt dieses Quadrat ein schwarzes Loch in
                // die Moebel, das ihr ueberallhin folgt - die Figur zog einen Kasten hinter sich
                // her. Dasselbe gilt laengst im Startbildschirm (siehe HomeScreen).
                showBackground = false,
                contentDescription = avatarContentDescription,
                modifier = Modifier
                    // Hoeher als breit wegen der Kopffreiheit - sonst staucht die feste
                    // Quadratgroesse das Raster und die Figur waere zu klein.
                    .width(current.sizeDp.dp)
                    .height(current.sizeDp.dp * AvatarGeometry.HEIGHT / AvatarGeometry.SIZE)
                    .offset { IntOffset(current.offset.x.roundToInt(), current.offset.y.roundToInt()) }
                    // **Antippen im Play-Modus oeffnet das Gespraech** (siehe PlayTalkPanel).
                    //
                    // Nur dort und nur, wenn keine Erinnerung offen ist: Steht eine an, ist das
                    // Antippen bereits mit dem Fuettern belegt, und ein Wesen, das im selben
                    // Moment auf zwei Arten auf denselben Griff reagiert, ist unberechenbar.
                    .then(
                        if (playMode && current.occurrenceId == null) {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures(onTap = {
                                    // Beim Wiedereroeffnen zeigen, was zuletzt erzaehlt wurde -
                                    // nur solange in DIESEM Gespraech noch nichts stand, sonst
                                    // wuerde ein frisch erzaehltes Stueck verdoppelt erscheinen.
                                    if (toldNow.isEmpty()) {
                                        PlayLore.lastToldPiece(context, current.species)?.let {
                                            toldNow = listOf(it)
                                        }
                                    }
                                    talkOpen = true
                                })
                            }
                        } else {
                            Modifier
                        }
                    )
                    // Fuettern ist sonst nur per Ziehen der Uhr auf den Avatar moeglich - siehe
                    // HomeScreen fuer dieselbe TalkBack-Zusatzaktion. Nur anbieten, solange auch
                    // wirklich etwas zu fuettern da ist (occurrenceId != null).
                    .then(
                        if (!current.fed && current.occurrenceId != null) {
                            Modifier.semantics {
                                customActions = listOf(
                                    CustomAccessibilityAction(feedActionLabel) { feedAvatarNow(); true }
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
            )
        }

        // Die VORDERE Kulissen-Ebene - als einziges NACH dem Avatar gezeichnet und nur fuer die
        // Requisite, die er gerade benutzt: Bettdecke ueber dem Liegenden, Sitzkante vor dem
        // Sitzenden. Genau diese Ueberdeckung ersetzt die sonst noetigen Lieg- und Sitzhaltungen
        // (siehe PlayScene.buildFront) - was verdeckt ist, muss nicht gezeichnet werden.
        if (playMode && occupiedStation != null) {
            val frontCells = remember(renderedPlace, occupiedStation, sceneWidthCells, floorYCells, sceneFade.value, avatar?.species) {
                PlayScene.buildFront(
                    place = renderedPlace,
                    station = occupiedStation,
                    widthCells = sceneWidthCells,
                    floorY = floorYCells,
                    dayPhase = PlayAmbientActivity.currentDayPhase(),
                    fade = sceneFade.value,
                    species = avatar?.species ?: AvatarSpeciesPrefs.get(context)
                )
            }
            PlaySceneView(
                cells = frontCells,
                cellPx = sceneCellPx,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Getragener Gegenstand und Zugriffs-Blitz - beide VOR dem Avatar, weil er sie in der Hand
        // haelt bzw. sie an ihm geschehen. Gemeinsam in einer Ebene, weil sie dieselbe Aufgabe
        // haben: sichtbar machen, was er gerade tut (siehe PlayEffects).
        if (playMode) {
            val effectCells = buildList {
                val current = avatar
                val item = carried
                if (current != null && item != null && !avatarHidden && sceneCellPx > 0f) {
                    addAll(
                        PlayEffects.carriedCells(
                            item = item,
                            avatarCellX = (current.offset.x / sceneCellPx).roundToInt(),
                            avatarCellY = (current.offset.y / sceneCellPx).roundToInt()
                        )
                    )
                }
                val kite = kitePhase
                if (current != null && kite != null && !avatarHidden && sceneCellPx > 0f) {
                    addAll(
                        PlayEffects.kiteCells(
                            avatarCellX = (current.offset.x / sceneCellPx).roundToInt(),
                            avatarCellY = (current.offset.y / sceneCellPx).roundToInt(),
                            phase = kite,
                            scenePhase = scenePhase,
                            widthCells = sceneWidthCells
                        )
                    )
                }
                val football = footballPhase
                if (current != null && football != null && !avatarHidden && sceneCellPx > 0f) {
                    addAll(
                        PlayEffects.footballCells(
                            avatarCellX = (current.offset.x / sceneCellPx).roundToInt(),
                            avatarCellY = (current.offset.y / sceneCellPx).roundToInt(),
                            phase = football,
                            scenePhase = scenePhase,
                            widthCells = sceneWidthCells
                        )
                    )
                }
                val fishing = fishingPhase
                if (current != null && fishing != null && !avatarHidden && sceneCellPx > 0f) {
                    addAll(
                        PlayEffects.fishingCells(
                            avatarCellX = (current.offset.x / sceneCellPx).roundToInt(),
                            avatarCellY = (current.offset.y / sceneCellPx).roundToInt(),
                            phase = fishing,
                            scenePhase = scenePhase,
                            widthCells = sceneWidthCells
                        )
                    )
                }
                sparkAt?.let { spot ->
                    addAll(PlayEffects.sparkCells(spot.centerX, spot.groundY, sparkProgress.value))
                }
                if (speechStep >= 0 && sceneCellPx > 0f) {
                    val speaker = if (speakerIsGuest) visitor?.offset else avatar?.offset
                    speaker?.let { at ->
                        addAll(
                            PlayEffects.speechCells(
                                avatarCellX = (at.x / sceneCellPx).roundToInt(),
                                avatarCellY = (at.y / sceneCellPx).roundToInt(),
                                step = speechStep
                            )
                        )
                    }
                }
            }
            if (effectCells.isNotEmpty()) {
                PlaySceneView(
                    cells = effectCells,
                    cellPx = sceneCellPx,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // **Sein Satz ueber dem Kopf** (siehe PlaySpeech).
        //
        // Ueber der Figur und unter dem Gespraech: Er gehoert zur Welt, nicht zur Bedienung -
        // deshalb faengt er auch keine Gesten ab. Wer die Figur antippt, oeffnet weiterhin das
        // Gespraech, auch wenn der Satz gerade darueber steht.
        spokenLine?.let { line ->
            avatar?.takeIf { playMode && !avatarHidden }?.let { current ->
                val speechY = current.offset.y - with(density) { SPEECH_LIFT_DP.dp.toPx() }
                Column(
                    modifier = Modifier
                        .widthIn(max = SPEECH_MAX_WIDTH_DP.dp)
                        .offset {
                            IntOffset(
                                current.offset.x.roundToInt(),
                                speechY.roundToInt().coerceAtLeast(0)
                            )
                        }
                ) {
                    Text(
                        text = stringResource(line),
                        color = Color(0xFFF3F1EA),
                        fontSize = 13.sp,
                        lineHeight = 16.sp
                    )
                    if (spokenIsOpenHabit) {
                        Text(
                            text = stringResource(PlaySpeech.habitHint()),
                            color = Color(0xFF8F8B82),
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
            LaunchedEffect(line, spokenIsOpenHabit) {
                delay((SPEECH_HOLD_MS * PlayTimeLapse.paceFactor()).toLong().coerceAtLeast(600L))
                spokenLine = null
            }
        }

        // ---- Vier feste Speicherplaetze, nur im Spielmodus (siehe ActionSlots.kt) ----
        //
        // Die Plaetze selbst stehen fest wie die Kulisse - nur ihr INHALT laesst sich ziehen, auf
        // den Avatar. Die Kollisionspruefung folgt dabei nicht nur der Zieh-Geste (dragOffset),
        // sondern auch der AKTUELLEN Avatar-Position (avatar?.offset als zweiter Schluessel des
        // LaunchedEffect unten) - der Avatar bewegt sich im Spielmodus animiert durch die Kulisse
        // (siehe moveToPlace) statt an fester Bildschirmstelle zu stehen, waere er nicht mit
        // einbezogen, traefe ein Ziehen auf ein Ziel, das sich waehrenddessen schon weiterbewegt
        // hat. Fuer TalkBack bleibt zusaetzlich die Zusatzaktion "Anwenden" - Ziehen laesst sich
        // fuer einen Screenreader nicht sinnvoll bedienen, dasselbe Muster wie beim Fuettern per
        // Uhr-Ziehen weiter oben.
        if (playMode) {
            Column(
                modifier = Modifier
                    .offset { IntOffset(slotsXPx.roundToInt(), slotsTopPx.roundToInt()) },
                verticalArrangement = Arrangement.spacedBy(with(density) { slotGapPx.toDp() })
            ) {
                repeat(ACTION_SLOT_COUNT) { index ->
                    val saved = slots.getOrNull(index)
                    val slotLabel = if (saved != null) {
                        val topicLabel = saved.libraryAnimationLabel
                            ?: saved.animationType?.let { stringResource(it.labelRes) }
                        stringResource(
                            R.string.a11y_action_slot_filled,
                            index + 1,
                            topicLabel ?: stringResource(R.string.a11y_reminder_generic)
                        )
                    } else {
                        stringResource(R.string.a11y_action_slot_empty, index + 1)
                    }
                    val applyLabel = stringResource(R.string.a11y_action_slot_apply)

                    // Ueber den Belegungs-Schluessel neu erzeugt: sobald dieser Platz frei wird
                    // (gefuettert) oder neu belegt wird, beginnt die Verschiebung wieder bei Null -
                    // sonst haenge die naechste Aktion an diesem Platz an der Position der vorigen
                    // (dasselbe Muster wie ActionSlot in ActionSlots.kt).
                    var dragOffset by remember(saved?.occurrenceId) { mutableStateOf(Offset.Zero) }

                    LaunchedEffect(dragOffset, avatar?.offset, avatar?.sizeDp) {
                        val current = avatar
                        if (saved == null || current == null || dragOffset == Offset.Zero) {
                            return@LaunchedEffect
                        }
                        val avatarPx = with(density) { current.sizeDp.dp.toPx() }
                        val slotAbsolute = slotOffsetPx(index) + dragOffset
                        if (isColliding(slotAbsolute, slotSizePx, current.offset, avatarPx)) {
                            feedFromSlot(index)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(with(density) { slotSizePx.toDp() })
                            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                            .clip(CircleShape)
                            .background(if (saved != null) TamaPalette.BubbleBackground else TamaPalette.RowBackground)
                            .border(
                                width = 1.dp,
                                color = TamaPalette.TextMuted.copy(alpha = if (saved != null) 0f else 0.35f),
                                shape = CircleShape
                            )
                            .then(
                                if (saved != null) {
                                    Modifier.pointerInput(saved.occurrenceId) {
                                        detectDragGestures(
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragOffset += amount
                                            },
                                            onDragEnd = {
                                                // Ueberschneidet sich der Platz gerade mit dem
                                                // Avatar, laeuft das Fuettern bereits (siehe
                                                // LaunchedEffect oben) - dann NICHT zurueckschnappen,
                                                // der Platz verschwindet gleich ohnehin.
                                                val current = avatar
                                                val stillOverlapping = current != null &&
                                                    isColliding(
                                                        slotOffsetPx(index) + dragOffset,
                                                        slotSizePx,
                                                        current.offset,
                                                        with(density) { current.sizeDp.dp.toPx() }
                                                    )
                                                if (!stillOverlapping) dragOffset = Offset.Zero
                                            },
                                            onDragCancel = { dragOffset = Offset.Zero }
                                        )
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .semantics {
                                contentDescription = slotLabel
                                if (saved != null) {
                                    customActions = listOf(
                                        CustomAccessibilityAction(applyLabel) { feedFromSlot(index); true }
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (saved != null) {
                            SimulatedMatrixView(
                                frame = ActionSlotSymbols.frameFor(saved),
                                showPuck = false,
                                modifier = Modifier.fillMaxSize().padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // ---- Das Gespraech (siehe PlayTalk / PlayTalkPanel) ----
        //
        // Es liegt ueber allem und faengt die Gesten ab: Solange es offen ist, laeuft die Welt zwar
        // weiter (der Avatar geht seinen Tag weiter), aber ein Griff daneben soll nicht versehentlich
        // die Uhr verschieben.
        if (talkOpen) {
            LaunchedEffect(talkOpen, talkRefresh, economyTick) {
                talkKnowledge = null
                val species = avatar?.species ?: AvatarSpeciesPrefs.get(context)
                talkKnowledge = withContext(Dispatchers.IO) {
                    // Mit Spielstand: Dieses Feld gibt es nur im Spielmodus, also ist die Frage
                    // nach Stufe, Geld und Vorrat hier immer sinnvoll.
                    PlayTalk.gather(
                        context, AvatarSpeciesPrefs.profileId(species), includeGame = true
                    )
                }
            }
            val species = avatar?.species ?: AvatarSpeciesPrefs.get(context)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0xB3000000))
                    .pointerInput(Unit) { detectTapGestures(onTap = { talkOpen = false }) },
                contentAlignment = Alignment.Center
            ) {
                // Eigenes pointerInput auf dem Feld selbst: Ohne das schloesse jeder Tipp auf eine
                // Frage zugleich das Gespraech, weil der Griff bis zur Flaeche darunter durchfiele.
                Box(modifier = Modifier.pointerInput(Unit) { detectTapGestures {} }) {
                    PlayTalkPanel(
                        knowledge = talkKnowledge,
                        species = species,
                        // Was er GERADE tut - der einzige Teil des Gespraechs, der sich bei jedem
                        // Oeffnen aendert, ohne dass dafuer ein Text mehr noetig waere.
                        doing = if (playMode) PlayTalk.Doing(currentPlace, currentTopic) else null,
                        mood = if (playMode) {
                            PlayTalk.Mood(
                                doing = PlayTalk.Doing(currentPlace, currentTopic),
                                phase = PlayAmbientActivity.currentDayPhase(),
                                weather = PlayWeather.current(),
                                lastVisitor = lastVisitor,
                                focusTopic = PlayUserProfile.focusTopic(context),
                                chapter = talkKnowledge?.chapter ?: CompanionChapter.ARRIVED,
                                game = talkKnowledge?.game
                            )
                        } else {
                            null
                        },
                        onAddReminder = { topic -> onAddHabit(topic); talkRefresh++ },
                        onOpenReminders = { talkOpen = false; onOpenReminders() },
                        // Bitten schliesst das Gespraech - man will ja sehen, was er tut.
                        onAsk = { topic -> talkOpen = false; requestedTopic = topic },
                        // Ein Rat wird sofort wirksam und das Gespraech bleibt offen: Man will
                        // sehen, dass es angekommen ist, und danach weiterlesen.
                        onAdjust = { reminder -> onAdjustHabit(reminder); talkRefresh++ },
                        // **Seine Frage an den Nutzer** - hoechstens eine je Gespraech, und nur,
                        // wenn er ueberhaupt schon etwas ueber ihn weiss (siehe PlayTalk.pendingAsk).
                        ask = pendingAsk,
                        onAnswerFocus = { topic ->
                            PlayUserProfile.setFocusTopic(context, topic)
                            askTick++
                            talkRefresh++
                        },
                        onAnswerTime = { phase ->
                            PlayUserProfile.setBusyPhase(context, phase)
                            askTick++
                            talkRefresh++
                        },
                        onAnswerPurpose = { purpose ->
                            PlayUserProfile.setPurpose(context, purpose)
                            askTick++
                            talkRefresh++
                        },
                        onAnswerWeekend = { includes ->
                            PlayUserProfile.setIncludesWeekend(context, includes)
                            askTick++
                            talkRefresh++
                        },
                        told = toldNow,
                        // Ob spaeter noch etwas kommt - siehe PlayLore: "heute nichts mehr" und
                        // "das war alles" sind zwei verschiedene Auskuenfte.
                        moreToTellLater = avatar?.species?.let {
                            PlayLore.hasMoreEver(context, it)
                        } == true,
                        onTell = avatar?.species?.takeIf { PlayLore.hasMore(context, it) }?.let { species ->
                            {
                                PlayLore.nextPiece(context, species)?.let { piece ->
                                    toldNow = toldNow + piece
                                    PlayLore.remember(context, species)
                                }
                            }
                        },
                        soundOn = soundOn,
                        // Bei jedem Oeffnen frisch nachgesehen: Ob Musik laeuft oder das Geraet
                        // stumm ist, aendert sich, waehrend die App laeuft.
                        soundSilentReason = remember(talkOpen, soundOn, askTick) {
                            PlaySound.silentReason(context)
                        },
                        onToggleSound = { on ->
                            PlaySound.setEnabled(context, on)
                            soundOn = on
                            // **Beim Einschalten sofort ein Ton.** Sonst tippt man auf "lass dich
                            // hoeren" und es passiert nichts - und man weiss weder, ob der
                            // Schalter gegriffen hat, noch wie er ueberhaupt klingt. Beim
                            // Ausschalten waere derselbe Ton ein Widerspruch in sich.
                            if (on) {
                                avatar?.species?.let { species ->
                                    PlaySound.play(context, species, PlayChime.Event.VISIT, scope)
                                }
                            }
                        },
                        onForget = {
                            PlayUserProfile.forget(context)
                            askTick++
                            talkRefresh++
                        },
                        onSkipAsk = {
                            pendingAsk?.let { PlayUserProfile.markAsked(context, it) }
                            askTick++
                        },
                        onDismiss = { talkOpen = false; toldNow = emptyList() }
                    )
                }
            }
        }

        // Kurzes Aufhellen als Bestaetigung fuer einen Schnappschuss - dieselbe Rueckmeldung wie
        // bei jeder Kamera. Ohne sie bliebe voellig offen, ob der Griff etwas bewirkt hat: Das
        // Bild wandert in die Sammlung, und auf dem Bildschirm aendert sich sonst nichts.
        //
        // ALLERLETZT, nach Figur, vorderer Kulisse und Effekten: Ein Aufblitzen, das die Figur
        // ausspart, saehe aus wie ein Zeichenfehler statt wie ein Ausloeser.
        if (snapshotFlash) {
            LaunchedEffect(snapshotCount) {
                delay(SNAPSHOT_FLASH_MS)
                snapshotFlash = false
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0x33FFFFFF))
            )
        }
    }
}

/** Eine zweite Kreatur, die gerade zu Besuch durchs Bild geht - siehe runVisit in DockScreen. */
private data class VisitorState(
    val species: AvatarSpecies,
    val offset: Offset,
    val sizeDp: Float,
    val frame: IntArray
)

private data class AvatarState(
    // Nullable seit dem Play-Modus: dort existiert der Avatar dauerhaft und ist nicht mehr an
    // eine konkrete Erinnerung gebunden, solange keine offen ist (siehe DockScreen oben und
    // PlayAmbientActivity). Im normalen Dock-Modus ist eine Instanz weiterhin nur zu einer
    // konkreten Erinnerung erzeugt worden, dort sind beide Felder immer gesetzt.
    val reminderId: Long?,
    val occurrenceId: Long?,
    val animationType: AnimationType?,
    val libraryAnimationLabel: String?,
    val species: AvatarSpecies,
    val offset: Offset,
    val sizeDp: Float,
    val frame: IntArray,
    val fed: Boolean = false,
    /**
     * Die volle Frame-Liste der gerade laufenden Erinnerungs-Animation - nur befuellt, waehrend
     * [occurrenceId] gesetzt ist. Wird ausschliesslich fuer die Vorschau gebraucht, falls diese
     * Ausloesung statt gefuettert in einen Speicherplatz gezogen wird (siehe [SavedAction],
     * ActionSlots.kt). [frame] allein reicht dafuer nicht - das ist nur das gerade sichtbare
     * Einzelbild und wechselt staendig.
     */
    val frames: List<IntArray> = emptyList()
)

/**
 * Der Avatar ist ein eckiges Sprite (kein runder Puck mehr wie die Uhr) - Kollision
 * daher ueber Bounding-Box-Overlap statt Mittelpunkt-Abstand. Beide Boxen werden vorher
 * etwas eingezogen ([OVERLAP_INSET]), damit ein blosses Beruehren der aeussersten Ecken
 * noch nicht als Treffer zaehlt.
 */
private fun isColliding(clockOffset: Offset, clockSizePx: Float, avatarOffset: Offset, avatarSizePx: Float): Boolean {
    val clockInset = clockSizePx * OVERLAP_INSET
    val avatarInset = avatarSizePx * OVERLAP_INSET
    val clockLeft = clockOffset.x + clockInset
    val clockRight = clockOffset.x + clockSizePx - clockInset
    val clockTop = clockOffset.y + clockInset
    val clockBottom = clockOffset.y + clockSizePx - clockInset
    val avatarLeft = avatarOffset.x + avatarInset
    val avatarRight = avatarOffset.x + avatarSizePx - avatarInset
    val avatarTop = avatarOffset.y + avatarInset
    val avatarBottom = avatarOffset.y + avatarSizePx - avatarInset
    return clockLeft < avatarRight && clockRight > avatarLeft && clockTop < avatarBottom && clockBottom > avatarTop
}

/**
 * Wie gross der Avatar im Verhaeltnis zur Uhr erscheint.
 *
 * 0,62 ist die Obergrenze, bei der Uhr und Avatar bei der Grundeinstellung (192dp) noch
 * nebeneinander auf ein uebliches Telefon passen: 192 + 119 + Abstand bleibt unter der Breite
 * gaengiger Geraete. Groesser gewaehlt, und eine ueberschneidungsfreie Platzierung ist auf
 * schmalen Displays rechnerisch unmoeglich - unabhaengig davon, wie gut man danach sucht.
 */
/** Wie lange das Bild nach einem Schnappschuss aufhellt - lang genug, um es zu bemerken, kurz
 *  genug, um nicht als Fehler zu wirken. */
private const val SNAPSHOT_FLASH_MS = 140L

private const val AVATAR_TO_CLOCK_RATIO = 0.62f

private const val OVERLAP_INSET = 0.15f

/**
 * Ab dieser Offen-Dauer wandert die Uhr waehrend der Erinnerung langsam ueber den Bildschirm
 * (Burn-in-Schutz). Kurze Erinnerungen bleiben bewusst stehen - dort waere die Bewegung nur
 * unruhig und schuetzt ohnehin nichts.
 */
private const val BURN_IN_DRIFT_FROM_SECONDS = 60

private fun needsBurnInDrift(openDurationSeconds: Int): Boolean =
    openDurationSeconds == ReminderOpenDuration.UNTIL_FED ||
        openDurationSeconds >= BURN_IN_DRIFT_FROM_SECONDS

/**
 * Laesst [onOffset] eine langsame, geschlossene Schleife beschreiben (Lissajous-Figur mit
 * ungleichen Perioden, damit sie nicht immer denselben Pfad nachfaehrt). Bewusst gemaechlich -
 * es geht darum, dass keine LED minutenlang dieselbe Helligkeit haelt, nicht um einen sichtbaren
 * Effekt. Die Amplitude bleibt so bemessen, dass die Uhr vollstaendig im Bild bleibt.
 */
private suspend fun animateBurnInDrift(
    maxWidthPx: Float,
    maxHeightPx: Float,
    clockSizeDp: Float,
    density: Density,
    onOffset: (Offset) -> Unit
) {
    val clockPx = with(density) { clockSizeDp.dp.toPx() }
    // Nur so weit auslenken, wie ueberhaupt Platz ist - sonst schoebe die Drift die Uhr aus dem
    // sichtbaren Bereich, gerade bei grosser Uhr auf kleinem Display.
    val amplitudeX = ((maxWidthPx - clockPx) / 2f).coerceAtLeast(0f).coerceAtMost(DRIFT_MAX_PX)
    val amplitudeY = ((maxHeightPx - clockPx) / 2f).coerceAtLeast(0f).coerceAtMost(DRIFT_MAX_PX)
    if (amplitudeX <= 0f && amplitudeY <= 0f) return

    val startMs = SystemClock.elapsedRealtime()
    while (true) {
        val t = (SystemClock.elapsedRealtime() - startMs) / 1000.0
        onOffset(
            Offset(
                (amplitudeX * sin(2 * Math.PI * t / DRIFT_PERIOD_X_SECONDS)).toFloat(),
                (amplitudeY * sin(2 * Math.PI * t / DRIFT_PERIOD_Y_SECONDS)).toFloat()
            )
        )
        delay(DRIFT_TICK_MS)
    }
}

/**
 * Wie lange ein Weg dauert - aus der Entfernung gerechnet, nicht pauschal.
 *
 * Als Mass dient die Koerperbreite des Avatars und nicht eine Pixelzahl: Damit haelt das Tempo
 * automatisch, wenn die Uhr (und mit ihr die ganze Welt) groesser oder kleiner gezogen wird -
 * eine feste Pixelgeschwindigkeit liesse ihn auf grossen Geraeten kriechen.
 *
 * Die Grenzen fangen beide Enden ab: ein sehr kurzer Weg soll nicht unnatuerlich hetzen, ein Weg
 * ueber die volle Bildschirmbreite nicht zur Wanderung werden.
 */
private fun walkDurationMs(distancePx: Float, avatarPx: Float): Int {
    if (avatarPx <= 0f) return WALK_MIN_MS
    // Leichte Streuung des Tempos von Weg zu Weg: exakt gleich schnell zu gehen ist eine
    // Eigenschaft von Maschinen. Der Ausschlag ist klein genug, dass man ihn nicht als Zufall
    // bemerkt - nur das Fehlen des Metronoms.
    val pace = WALK_BODIES_PER_SECOND * (1f + (Random.nextFloat() - 0.5f) * WALK_PACE_SPREAD)
    val seconds = distancePx / (avatarPx * pace)
    return (seconds * 1000f).roundToInt().coerceIn(WALK_MIN_MS, WALK_MAX_MS)
}

/** Koerperbreiten pro Sekunde - gemuetliches, aber nicht schleppendes Gehtempo. */
private const val WALK_BODIES_PER_SECOND = 1.15f

/** Wie stark das Gehtempo von Weg zu Weg streut (0,25 = plus/minus gut ein Achtel). */
private const val WALK_PACE_SPREAD = 0.25f
private const val WALK_MIN_MS = 380
private const val WALK_MAX_MS = 2200

/**
 * Kuerzester Weg, der ueberhaupt als Gang gezeigt wird (Bruchteil der Avatarbreite) - darunter
 * wird nur umgesetzt. Ein Geh-Zyklus ueber wenige Pixel liest sich als Zucken, nicht als Schritt.
 */
private const val MIN_WALK_FRACTION = 0.22f

/**
 * Reicht einen aufgenommenen Film an andere Apps weiter - ueber einen FileProvider, weil seit
 * Android 7 ein direkter Dateipfad nach aussen eine Ausnahme ausloest.
 */
private fun shareClipFile(context: android.content.Context, file: java.io.File) {
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context, "${context.packageName}.clips", file
    )
    context.startActivity(
        android.content.Intent.createChooser(
            android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
            null
        )
    )
}

/** Wie lange die Tuer offen steht, bevor er hindurchgeht. */
private const val DOOR_OPEN_MS = 380L

/** Wie lange das Hinein- und Heraustreten aus dem Tuerrahmen dauert. */
private const val DOOR_STEP_MS = 420

/** Wie oft die Mond-Szene ueberhaupt stattfindet - eine Ausnahme, die jedes Mal kaeme, waere
 *  keine mehr. */
/**
 * Wie viel groesser der Mond waehrend seiner Szene ist als die Uhr sonst.
 *
 * Er IST dieselbe Matrix - nur eben als Mond gezeichnet (siehe MoonFrame). In gleicher Groesse wie
 * eine Uhr wirkte er wie eine verschobene Uhr; erst spuerbar groesser wird daraus ein Himmelskoerper.
 * Nicht mehr als das Anderthalbfache: Darueber wird aus der Sichel eine Scheibe, die den halben
 * Bildschirm fuellt, und die Weite, um die es in dieser Szene geht, ist wieder dahin.
 */
private const val MOON_SCALE = 1.5f

private const val MOON_SCENE_CHANCE = 0.6f

/** Aufstieg und Rueckkehr der Uhr. */
private const val MOON_RISE_MS = 2400

/** Takt, in dem die Sichel ihre Form aendert. */
private const val MOON_PHASE_TICK_MS = 900L

/**
 * Wie stark ein Besucher gegenueber dem Bewohner zurueckgenommen wird.
 *
 * Von 0,62 auf 0,78 angehoben: Der urspruengliche Wert loeste zwar die Ueberdeckung, liess den
 * Gast aber truebe wirken - als waere er nicht ganz da. 0,78 haelt den Abstand zum Bewohner
 * (der bleibt die hellste Figur), gibt dem Besuch aber genug Substanz, um als eigene Kreatur zu
 * gelten und nicht als Schatten.
 */
private const val VISITOR_DIM = 0.78f

/** Abstand beim Stehenbleiben, in Vielfachen der Figurenbreite - eine volle Breite plus etwas
 *  Luft, damit sich die Silhouetten sicher nicht beruehren. */
private const val VISITOR_GAP = 1.15f

/** Wortwechsel je Begegnung - drei reichen, damit es ein Gespraech ist; mehr wuerde den
 *  Besuch zur Szene aufblasen, die er nicht sein soll. */
private const val CONVERSATION_TURNS = 3

/** Wie lange sein Satz ueber dem Kopf stehen bleibt. */
private const val SPEECH_HOLD_MS = 3_200L

/** Wie weit ueber der Figur - hoch genug, dass er ihren Kopf nicht verdeckt. */
private const val SPEECH_LIFT_DP = 22

/** Und wie breit hoechstens: ein Satz, keine Spalte. */
private const val SPEECH_MAX_WIDTH_DP = 210

/** Takt, in dem die Sprechpunkte erscheinen. */
private const val SPEECH_DOT_MS = 190L

/** Abstand zwischen zwei Besuchen. */
private val VISIT_INTERVAL_MS = 90_000L..210_000L

/**
 * Deutlich kuerzerer Abstand fuer die belebten Orte (Strasse, Stadt): Auf einem Weg begegnet man
 * einander haeufiger als beim Ausruhen im Park oder beim Einkaufen - grob ein Drittel des
 * sonstigen Takts statt alle anderthalb bis dreieinhalb Minuten.
 */
private val VISIT_INTERVAL_MS_BUSY = 30_000L..70_000L

/** Wie oft nachgesehen wird, ob ein Besuch inzwischen passt - siehe den Besuchstakt in DockScreen. */
private const val VISIT_RETRY_MS = 4_000L

/** Welcher Besuchstakt an diesem Ort gilt - siehe [VISIT_INTERVAL_MS_BUSY]. */
private fun visitIntervalFor(place: PlayScene.Place): LongRange = when (place) {
    PlayScene.Place.STREET, PlayScene.Place.CITY -> VISIT_INTERVAL_MS_BUSY
    else -> VISIT_INTERVAL_MS
}


/** Wo an der Figur ein Zugriff aufblitzt - auf Handhoehe, seitlich vorn (16x16-Raster). */
private const val FLASH_HAND_X = 12
private val FLASH_HAND_Y = 10 + AvatarGeometry.HEADROOM

/** Atemzug zwischen Ankommen und Handlung. */
private const val ARRIVAL_SETTLE_MS = 260L

/** Wie lange das Hinlegen/Hinsetzen bzw. Aufstehen dauert - eine Bewegung an Ort und Stelle,
 *  deshalb deutlich kuerzer als ein Weg. */
private const val SETTLE_INTO_MS = 420

/**
 * Wo der Avatar steht, wenn er an einem Ort der Kulisse ([PlayScene]) auf dem Boden aufsetzt.
 *
 * Die senkrechte Lage haengt an der SPEZIES, nicht am Sprite-Quadrat: Die sechs Grundformen
 * reichen unterschiedlich tief in ihr 16x16-Raster hinein (siehe [groundRow]). Gerechnet wird
 * deshalb so, dass die unterste belegte Zeile der Figur genau auf der Bodenlinie aufsitzt - ein
 * pauschaler Versatz liess je nach gewaehltem Avatar die Fuesse im Boden verschwinden oder die
 * Figur darueber schweben.
 */
private fun avatarSpot(
    anchorX: Float,
    avatarPx: Float,
    maxWidthPx: Float,
    floorYPx: Float,
    species: AvatarSpecies
): Offset {
    val groundRow = AvatarBodies.forSpecies(species).groundRow()
    return Offset(
        AvatarFooting.leftFor(anchorX, avatarPx, maxWidthPx),
        AvatarFooting.topFor(floorYPx, avatarPx, groundRow)
    )
}

/**
 * Setzt die Figur auf die Aufsetzstelle einer Requisite - dieselbe Rechnung wie `spotToOffset`
 * innerhalb eines Ablaufs, hier aber ohne dessen Umgebung.
 *
 * Doppelt vorhanden zu sein waere schlecht; die Fassung dort sitzt allerdings in einer Funktion,
 * die Spezies und Bildbreite bereits kennt, waehrend sie hier hereingereicht werden muessen. Die
 * gemeinsame Regel steht im Kommentar an beiden Stellen: Die Figur setzt mit ihrer Fussreihe auf
 * der Zeile auf, die die Requisite als Boden angibt.
 */
private fun stationOffset(
    spot: PlayScene.SceneSpot,
    avatarPx: Float,
    maxWidthPx: Float,
    species: AvatarSpecies
): Offset {
    val groundRow = AvatarBodies.forSpecies(species).groundRow()
    val cell = avatarPx / AvatarGeometry.SIZE
    return Offset(
        x = AvatarFooting.leftForCenter(spot.centerX, avatarPx, maxWidthPx),
        y = AvatarFooting.topFor((spot.groundY + 1) * cell, avatarPx, groundRow)
    )
}

/**
 * GRUNDtakt der Umgebungsanimation - jedes Detail nimmt sich daraus sein eigenes Vielfaches
 * (siehe PlayScene.beat), damit Dampf, Lampe, Sterne und Mattscheibe nicht im Gleichschritt
 * laufen. Ein grober gemeinsamer Takt (der erste Entwurf lag bei 600ms) macht daraus ein Uhrwerk:
 * alles regt sich gleichzeitig, und genau das nimmt der Umgebung das Lebendige.
 */
private const val SCENE_PHASE_TICK_MS = 200L

private const val SCENE_FADE_OUT_MS = 220
private const val SCENE_FADE_IN_MS = 380

/** Wie lange der Glueckwunsch zum neuen Level stehen bleibt, bevor wieder der nuechterne Stand
 *  erscheint - lang genug zum Lesen, kurz genug, um nicht im Weg zu stehen. */
private const val LEVEL_UP_MESSAGE_MS = 4_000L
private const val DOCK_TAG = "DockScreen"

/** Maximale Auslenkung der Burn-in-Drift. */
private const val DRIFT_MAX_PX = 60f
private const val DRIFT_PERIOD_X_SECONDS = 47.0
private const val DRIFT_PERIOD_Y_SECONDS = 31.0
private const val DRIFT_TICK_MS = 250L

/**
 * Zufaellige Position innerhalb der Bounds, mit Mindestabstand zur aktuellen
 * Uhr-Position (sonst wuerde die Kollisions-Erkennung sofort beim Spawn ausloesen) -
 * ein paar Zufalls-Versuche, sonst Fallback auf die gegenueberliegende Ecke.
 */
internal fun randomAvatarOffset(
    boundX: Float,
    boundY: Float,
    avatarSizePx: Float,
    clockOffset: Offset,
    clockSizePx: Float
): Offset {
    if (boundX <= 0f || boundY <= 0f) return Offset(boundX / 2f, boundY / 2f)

    // Sichtabstand, damit Uhr und Avatar nicht auf Kante stehen.
    val gap = avatarSizePx * 0.10f

    /**
     * Zwei achsenparallele Quadrate ueberlappen NICHT, sobald sie sich in EINER Achse trennen.
     *
     * Vorher wurde stattdessen der Abstand der Mittelpunkte gegen `(uhr + avatar) / 2 * 1,2`
     * geprueft. Das laesst diagonale Lagen faelschlich durch: liegen beide Achsen bei
     * 0,85 × halber Summe, ist der euklidische Abstand rund 1,2 × halbe Summe - die Pruefung
     * bestand also, waehrend sich die Quadrate sichtbar ueberschnitten. Genau die schraege
     * Ueberlappung, die im Dock zu sehen war.
     */
    fun overlaps(candidate: Offset): Boolean {
        val separatedX = candidate.x + avatarSizePx + gap <= clockOffset.x ||
            clockOffset.x + clockSizePx + gap <= candidate.x
        val separatedY = candidate.y + avatarSizePx + gap <= clockOffset.y ||
            clockOffset.y + clockSizePx + gap <= candidate.y
        return !separatedX && !separatedY
    }

    repeat(40) {
        val candidate = Offset(Random.nextFloat() * boundX, Random.nextFloat() * boundY)
        if (!overlaps(candidate)) return candidate
    }

    // Rueckfall mit Garantie statt Zufall: die vier Ecken durchgehen und die erste freie nehmen.
    // Frueher stand hier eine gespiegelte Position OHNE jede Pruefung - schlugen die
    // Zufallsversuche fehl (was bei grosser Uhr der Normalfall ist), landete der Avatar
    // regelmaessig direkt auf ihr.
    val corners = listOf(
        Offset(0f, 0f),
        Offset(boundX, 0f),
        Offset(0f, boundY),
        Offset(boundX, boundY)
    )
    corners.firstOrNull { !overlaps(it) }?.let { return it }

    // Selbst dann keine freie Ecke - dann wenigstens die am weitesten entfernte.
    val clockCenter = Offset(clockOffset.x + clockSizePx / 2f, clockOffset.y + clockSizePx / 2f)
    return corners.maxByOrNull { corner ->
        val dx = corner.x + avatarSizePx / 2f - clockCenter.x
        val dy = corner.y + avatarSizePx / 2f - clockCenter.y
        sqrt(dx * dx + dy * dy)
    } ?: Offset(0f, 0f)
}
