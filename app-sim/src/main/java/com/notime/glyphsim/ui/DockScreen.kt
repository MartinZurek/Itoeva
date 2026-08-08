package com.notime.glyphsim.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.R
import com.notime.glyphsim.data.AvatarFeedEvent
import com.notime.glyphcore.data.ReminderOpenDuration
import kotlinx.coroutines.flow.onSubscription
import com.notime.glyphsim.matrix.AvatarAnimations
import com.notime.glyphsim.matrix.AvatarBodies
import com.notime.glyphsim.matrix.AvatarGeometry
import com.notime.glyphsim.matrix.groundRow
import com.notime.glyphsim.matrix.AvatarMood
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.AvatarSpriteView
import com.notime.glyphsim.matrix.MatrixAnimator
import com.notime.glyphsim.matrix.MoonFrame
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import kotlinx.coroutines.withContext
import com.notime.glyphsim.matrix.PlayClipRenderer
import com.notime.glyphsim.matrix.PlayClipRecorder
import com.notime.glyphsim.matrix.PlayAmbientActivity
import com.notime.glyphsim.matrix.PlayEffects
import com.notime.glyphsim.matrix.PlayPantry
import com.notime.glyphsim.matrix.PlayWallet
import com.notime.glyphsim.matrix.PlayRoutine
import com.notime.glyphsim.matrix.PlayRoutines
import com.notime.glyphsim.matrix.PlayScene
import com.notime.glyphsim.matrix.PlaySceneView
import com.notime.glyphsim.matrix.PlayTimeLapse
import com.notime.glyphsim.matrix.RoutineStep
import com.notime.glyphsim.matrix.ReminderAnimationBus
import com.notime.glyphsim.matrix.SimulatedMatrixView
import android.os.SystemClock
import androidx.compose.ui.unit.Density
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    playMode: Boolean = false
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

        // Ort und dargestellter Ort - stehen VOR der Geometrie, weil die Bodenhoehe vom Ort
        // abhaengt (siehe PlayScene.floorFraction).
        var currentPlace by remember { mutableStateOf(PlayScene.Place.NOOK) }
        var renderedPlace by remember { mutableStateOf(PlayScene.Place.NOOK) }

        // ---- Geometrie der Lebenswelt (nur Play-Modus, siehe PlayScene) ----
        //
        // Die Kulisse teilt sich Zellraster UND Zellgroesse mit dem Avatar-Sprite: beides aus
        // derselben Avatar-Groesse abgeleitet, damit Figur und Welt sichtbar zur selben
        // Pixelwelt gehoeren. Eine eigene Kulissen-Zellgroesse waere sofort als zwei
        // uebereinandergelegte Grafiken aufgefallen.
        val worldAvatarSizeDp = (clockSizeDp * AVATAR_TO_CLOCK_RATIO)
            .coerceIn(DockLayoutPrefs.MIN_SIZE_DP, DockLayoutPrefs.DEFAULT_SIZE_DP)
        val worldAvatarPx = with(density) { worldAvatarSizeDp.dp.toPx() }
        val sceneCellPx = worldAvatarPx / AvatarGeometry.SIZE
        val sceneWidthCells = if (sceneCellPx > 0f) (maxWidthPx / sceneCellPx).toInt() else 0
        // Die Bodenhoehe haengt am ORT und an der Tageszeit (siehe PlayScene.floorFraction) und
        // wird weich nachgefuehrt: Beim Wechsel in den naechtlichen Park senkt sich der Horizont
        // sichtbar ab, statt zu springen - genau diese Bewegung erzeugt die Weite.
        val targetFloorFraction = PlayScene.floorFraction(renderedPlace, PlayAmbientActivity.currentDayPhase())
        val floorFraction by animateFloatAsState(
            targetValue = targetFloorFraction,
            animationSpec = tween(FLOOR_SHIFT_MS),
            label = "floor"
        )
        val floorYCells = if (sceneCellPx > 0f) {
            (maxHeightPx * floorFraction / sceneCellPx).toInt()
        } else {
            0
        }
        val floorYPx = floorYCells * sceneCellPx

        var avatar by remember { mutableStateOf<AvatarState?>(null) }
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
        var economyTick by remember { mutableIntStateOf(0) }
        // Laufende Aufnahme: null = keine. Der Fortschritt gilt fuers Zusammenrechnen DANACH.
        var clipSession by remember { mutableStateOf<PlayClipRecorder.Session?>(null) }
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
                    avatar = avatar?.copy(offset = Offset(value, destination.y))
                }
                gait.cancel()
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
                        PlayScene.avatarAnchorX(target),
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

            /** Setzt die Figur so, dass sie mit [centerX]/[groundY] (Szenenzellen) zusammenfaellt. */
            fun spotToOffset(spot: PlayScene.SceneSpot, avatarPx: Float): Offset {
                val cell = avatarPx / AvatarGeometry.SIZE
                val groundRow = AvatarBodies.forSpecies(species).groundRow()
                return Offset(
                    x = (spot.centerX * cell - avatarPx / 2f)
                        .coerceIn(0f, (maxWidthPx - avatarPx).coerceAtLeast(0f)),
                    y = ((spot.groundY + 1) * cell - (groundRow + 1) * cell).coerceAtLeast(0f)
                )
            }

            try {
            for (step in routine.steps) {
                val current = avatar ?: return
                if (current.fed || current.occurrenceId != null) return   // echte Erinnerung hat Vorrang
                val avatarPx = with(density) { current.sizeDp.dp.toPx() }

                when (step) {
                    is RoutineStep.GoTo -> {
                        val spot = PlayScene.stationSpot(currentPlace, step.station, sceneWidthCells, floorYCells)
                            ?: continue
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
                        val destination = avatarSpot(step.anchorX, avatarPx, maxWidthPx, floorYPx, species)
                        if (walkAvatarTo(destination)) startAvatarIdleLoop(species, mood)
                    }

                    is RoutineStep.Occupy -> {
                        val spot = PlayScene.stationSpot(currentPlace, step.station, sceneWidthCells, floorYCells)
                            ?: continue
                        // Kurzes Hinauf statt Sprung: dieselbe weiche Bewegung wie beim Gehen,
                        // nur senkrecht - sich hinzulegen ist keine Ortsveraenderung, sondern
                        // eine Bewegung an Ort und Stelle.
                        val target = spotToOffset(spot, avatarPx)
                        val from = current.offset
                        animate(0f, 1f, animationSpec = tween(SETTLE_INTO_MS, easing = FastOutSlowInEasing)) { t, _ ->
                            avatar = avatar?.copy(
                                offset = Offset(
                                    from.x + (target.x - from.x) * t,
                                    from.y + (target.y - from.y) * t
                                )
                            )
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
                        animate(0f, 1f, animationSpec = tween(SETTLE_INTO_MS, easing = FastOutSlowInEasing)) { t, _ ->
                            avatar = avatar?.copy(
                                offset = Offset(
                                    from.x + (onFloor.x - from.x) * t,
                                    from.y + (onFloor.y - from.y) * t
                                )
                            )
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

                    RoutineStep.Drop -> {
                        if (carried != null) {
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

                    is RoutineStep.GoToPlace -> {
                        activeStation = null
                        moveToPlace(step.place, species)
                    }

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
                occupiedStation = null
                // Dasselbe fuer Getragenes: Bricht der Ablauf zwischen Take und Drop ab, trueg
                // die Figur das Buch sonst durch alle folgenden Szenen mit sich herum.
                carried = null
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
            libraryAnimationLabel: String? = null
        ): AvatarState {
            // Auf dem Boden der Kulisse statt an zufaelliger Stelle im Schwarzen: sobald ein Raum
            // gezeichnet wird, ist eine frei schwebende Figur der eine Fehler, den man sofort
            // sieht. Diese Funktion wird ausschliesslich im Play-Modus aufgerufen - nur dort gibt
            // es ueberhaupt einen Boden.
            val species = AvatarSpeciesPrefs.get(context)
            val spawnOffset = avatarSpot(
                anchorX = PlayScene.avatarAnchorX(currentPlace),
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
                frame = AvatarAnimations.idleSequence(species, mood).frames.first()
            )
        }

        // Play-Modus-Einstieg/-Ausstieg: der Avatar ist hier - anders als im normalen Dock -
        // dauerhaft sichtbar, nicht nur waehrend eine Erinnerungs-Animation laeuft (siehe
        // Klassendoku oben). Verlaesst man den Play-Modus, waehrend der Avatar nur ambient idlet
        // (keine offene Erinnerung, keine laufende Fuetter-Reaktion), verschwindet er wieder -
        // der normale Dock-Modus soll ungestoert bleiben, wie er es immer war.
        LaunchedEffect(playMode) {
            if (playMode) {
                if (avatar == null) {
                    avatar = spawnAmbientAvatar()
                }
            } else if (avatar?.occurrenceId == null && avatar?.fed != true) {
                avatarIdleJob?.cancel()
                occupiedStation = null
                avatar = null
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
            LaunchedEffect(Unit) {
                while (isActive) {
                    delay((VISIT_INTERVAL_MS.random() * PlayTimeLapse.paceFactor()).toLong().coerceAtLeast(3_000L))
                    val current = avatar
                    if (current != null && !current.fed && current.occurrenceId == null &&
                        occupiedStation == null && !avatarHidden &&
                        PlayScene.allowsVisitors(currentPlace)
                    ) {
                        runVisit()
                    }
                }
            }

            // Die Mond-Szene beginnt und endet mit der Park-Nacht. Beim Beginn wird EINMAL
            // gewuerfelt, ob sie ueberhaupt stattfindet, und wie hoch der Mond steht - so sieht
            // nicht jede Nacht gleich aus.
            LaunchedEffect(renderedPlace, scenePhase / 40) {
                val isParkNight = renderedPlace == PlayScene.Place.PARK &&
                    PlayAmbientActivity.currentDayPhase() == PlayAmbientActivity.DayPhase.NIGHT
                if (isParkNight && !moonMode) {
                    if (Random.nextFloat() < MOON_SCENE_CHANCE) {
                        savedClockOffset = clockOffset
                        moonMode = true
                        val clockPx = with(density) { clockSizeDp.dp.toPx() }
                        val target = Offset(
                            x = ((maxWidthPx - clockPx) * (0.14f + Random.nextFloat() * 0.5f))
                                .coerceAtLeast(0f),
                            y = (maxHeightPx * (0.06f + Random.nextFloat() * 0.12f))
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
                        animate(0f, 1f, animationSpec = tween(MOON_RISE_MS, easing = FastOutSlowInEasing)) { t, _ ->
                            clockOffset = Offset(
                                from.x + (back.x - from.x) * t,
                                from.y + (back.y - from.y) * t
                            )
                        }
                        savedClockOffset = null
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
        LaunchedEffect(Unit) {
            ReminderAnimationBus.events.onSubscription {
                val open = OpenReminderLookup.find(
                    context,
                    AvatarSpeciesPrefs.profileId(AvatarSpeciesPrefs.get(context))
                ) ?: return@onSubscription
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
                            libraryAnimationLabel = event.libraryAnimationLabel
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
                            libraryAnimationLabel = event.libraryAnimationLabel
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
                        // Anders als im normalen Dock verschwindet der Avatar hier nicht - nur
                        // die Verknuepfung zur ausgelaufenen Erinnerung faellt weg, seine Idle-
                        // Schleife (siehe oben, unangetastet seit dem Play-Modus-Einstieg bzw.
                        // der letzten Verknuepfung) laeuft einfach weiter.
                        avatar = avatar?.copy(
                            reminderId = null,
                            occurrenceId = null,
                            animationType = null,
                            libraryAnimationLabel = null
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
            // Gefuettert wird immer frei stehend - eine Fuetter-Reaktion unter der Bettdecke waere
            // zur Haelfte unsichtbar.
            occupiedStation = null
            avatar = current.copy(fed = true)
            clockAnimJob?.cancel()
            avatarIdleJob?.cancel()
            isPlayingAnimation = false
            animationFrame = null

            // Fuetter-Logik geteilt mit dem Startbildschirm (siehe AvatarFeeding) - dort
            // laesst sich die Uhr inzwischen genauso auf den Avatar schieben, beide sollen
            // sich identisch verhalten.
            scope.launch(Dispatchers.IO) {
                AvatarFeeding.logFeedEvent(context, current.occurrenceId)
            }
            scope.launch {
                try {
                    AvatarFeeding.playReaction(
                        species = current.species,
                        animationType = current.animationType,
                        libraryAnimationLabel = current.libraryAnimationLabel,
                        screenWidthPx = maxWidthPx,
                        screenHeightPx = maxHeightPx,
                        onFrame = { f -> avatar = avatar?.copy(frame = f) },
                        onOffset = { o -> avatar = avatar?.copy(offset = current.offset + o) }
                    )
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
                    if (avatar?.occurrenceId == current.occurrenceId) {
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
                        } else {
                            avatar = null
                        }
                    }
                }
            }
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
            }
        }

        // Play-Modus: kleine autonome Regungen zwischen zwei echten Ausloesungen (die je nach
        // Spezies/Level mehrere Minuten auseinander liegen koennen, siehe PlayGamePlan) - sonst
        // stuende der Avatar dazwischen nur unveraendert idle da. Rein kosmetisch (keine
        // Erinnerung wird beantwortet, kein XP), aber PERFORM zeigt einen an die Tageszeit
        // gekoppelten Tagesablauf: siehe PlayAmbientActivity fuer die Themen-/Tagesphasen-Logik.
        if (playMode) {
            LaunchedEffect(avatar?.species) {
                val species = avatar?.species ?: return@LaunchedEffect
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
                            val anchor = (PlayScene.avatarAnchorX(currentPlace) + Random.nextFloat() * 0.16f - 0.08f)
                                .coerceIn(0.02f, 0.98f)
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
                            val profileId = AvatarSpeciesPrefs.profileId(species)
                            val boostedTopics = PlayHabitSignal.underfulfilledTopics(context, profileId)
                            // Vorrang vor allem anderen: Ist nichts mehr da UND kein Geld fuer
                            // einen Einkauf, muss gearbeitet werden. Das ist die Stelle, an der
                            // die Welt den Zufall ueberstimmt - und der Grund, warum man ihm beim
                            // Zuschauen abnimmt, dass er zur Arbeit GEHT statt dort zu erscheinen.
                            val mustEarn = PlayPantry.isEmpty(context) && !PlayWallet.canAfford(context)
                            val topic = if (mustEarn) {
                                AnimationType.WORK
                            } else {
                                PlayAmbientActivity.nextTopic(boostedTopics = boostedTopics)
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
                            moveToPlace(place, species)

                            // Nicht mehr EINE Animation, sondern ein mehrschrittiger Ablauf:
                            // hingehen, benutzen, handeln, verweilen, aufstehen (siehe
                            // PlayRoutine). Erst dadurch setzt sich die Figur mit ihrer Umgebung
                            // auseinander, statt neben den Moebeln zu agieren.
                            runRoutine(
                                PlayRoutines.forTopic(
                                    topic = topic,
                                    needsShopping = PlayPantry.isEmpty(context) && PlayWallet.canAfford(context)
                                ),
                                species
                            )
                        }
                    }
                }
            }
        }

        // Der Mitschnitt: Nimmt regelmaessig auf, was gerade zu sehen ist - Kulisse, Figur, Uhr,
        // Getragenes, Gast. Festgehalten werden nur Beschreibungen, keine Bilder (siehe
        // PlayClipRecorder.Session), deshalb kostet eine lange Aufnahme kaum Speicher.
        LaunchedEffect(clipSession) {
            val session = clipSession ?: return@LaunchedEffect
            while (isActive && !session.isFull) {
                val current = avatar
                if (current != null && sceneCellPx > 0f) {
                    val avatarPx = with(density) { current.sizeDp.dp.toPx() }
                    val boundX = (maxWidthPx - avatarPx).coerceAtLeast(1f)
                    val clockPx = with(density) { clockSizeDp.dp.toPx() }
                    session.add(
                        PlayClipRenderer.Frame(
                            place = renderedPlace,
                            species = current.species,
                            dayPhase = PlayAmbientActivity.currentDayPhase(),
                            avatarFrame = current.frame,
                            avatarAnchorX = (current.offset.x / boundX).coerceIn(0f, 1f),
                            scenePhase = scenePhase,
                            station = occupiedStation ?: activeStation,
                            lampOn = lampOn,
                            tvOn = tvOn,
                            clockFrame = if (avatar?.fed == true) null else frame,
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
                    )
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
                lampOn, tvOn, activeStation, avatar?.species
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
                    species = avatar?.species ?: AvatarSpeciesPrefs.get(context)
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
            SimulatedMatrixView(
                frame = frame,
                contentDescription = clockContentDescription,
                modifier = Modifier
                    .size(clockSizeDp.dp)
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
                        // maxWidth/maxHeight/density sind fuer die Lebensdauer dieses
                        // Screens praktisch konstant (keine Rotation waehrend des
                        // Andockens vorgesehen) - deshalb hier direkt verwendet statt
                        // ueber rememberUpdatedState nachgezogen. clockSizeDp/clockOffset
                        // sind State-gestuetzt und liefern beim Lesen/Schreiben immer den
                        // aktuellen Wert, auch aus dieser einmalig gestarteten Coroutine.
                        detectTransformGestures(panZoomLock = false) { _, pan, zoom, _ ->
                            val newSize = (clockSizeDp * zoom)
                                .coerceIn(DockLayoutPrefs.MIN_SIZE_DP, DockLayoutPrefs.MAX_SIZE_DP)
                            clockSizeDp = newSize
                            val clockPxNow = with(density) { newSize.dp.toPx() }
                            val boundX = (with(density) { maxWidth.toPx() } - clockPxNow).coerceAtLeast(0f)
                            val boundY = (with(density) { maxHeight.toPx() } - clockPxNow).coerceAtLeast(0f)
                            clockOffset = Offset(
                                (clockOffset.x + pan.x).coerceIn(0f, boundX),
                                (clockOffset.y + pan.y).coerceIn(0f, boundY)
                            )
                        }
                    }
            )
        }

        // Fortschritt im Spielmodus - bewusst klein und gedaempft, aber die EINZIGE Stelle, an der
        // Level und XP zu sehen sind (im Startbildschirm stuenden sie nur im Weg).
        //
        // **Unten und nicht oben:** Am oberen Rand lag die Zahl bei Geraeten mit Kamera-Ausschnitt
        // teilweise darunter und war schlicht nicht lesbar. Der untere Rand ist davon auf jedem
        // Geraet frei - im Dock-Modus sind die Systemleisten ohnehin ausgeblendet, es gibt dort
        // also auch nichts, womit sie sich stossen koennte.
        if (playMode) {
            val playViewModel = androidx.lifecycle.viewmodel.compose.viewModel<PlayModeViewModel>()
            val playState by playViewModel.state.collectAsState()
            Text(
                if (playState.showLevelUp) {
                    stringResource(R.string.playmode_level_up, playState.level)
                } else {
                    // Muenzen und Vorrat gehoeren hierher, nicht in eine eigene Anzeige: Sie
                    // erklaeren, WARUM der Avatar gerade tut, was er tut. Wer sieht, dass der
                    // Vorrat auf 0 steht, versteht den Gang in den Laden - und wer sieht, dass
                    // auch das Geld fehlt, versteht den Gang zur Arbeit.
                    stringResource(
                        R.string.playmode_status,
                        playState.level,
                        playState.xp,
                        remember(economyTick) { PlayWallet.coins(context) },
                        remember(economyTick) { PlayPantry.level(context) }
                    )
                },
                color = if (playState.showLevelUp) Color(0xFF7FD1A6) else Color(0xFF6E6A63),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            )
            if (playState.showLevelUp) {
                // Erst nach einem Moment bestaetigen, sonst waere der Glueckwunsch im selben
                // Frame wieder verschwunden, in dem er erscheint.
                LaunchedEffect(playState.level) {
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
    val fed: Boolean = false
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

/** Takt, in dem die Sprechpunkte erscheinen. */
private const val SPEECH_DOT_MS = 190L

/** Abstand zwischen zwei Besuchen. */
private val VISIT_INTERVAL_MS = 90_000L..210_000L

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
    val boundX = (maxWidthPx - avatarPx).coerceAtLeast(0f)
    val cellPx = avatarPx / AvatarGeometry.SIZE
    val groundRow = AvatarBodies.forSpecies(species).groundRow()
    val y = (floorYPx - (groundRow + 1) * cellPx).coerceAtLeast(0f)
    return Offset(boundX * anchorX.coerceIn(0f, 1f), y)
}

/** Wie lange das Absenken/Anheben des Horizonts dauert - langsam genug, dass man die Weite
 *  entstehen SIEHT, statt sie nur vorzufinden. */
private const val FLOOR_SHIFT_MS = 1200

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
