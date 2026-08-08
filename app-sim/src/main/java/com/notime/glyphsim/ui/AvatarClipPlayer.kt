package com.notime.glyphsim.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.notime.glyphsim.matrix.AvatarClip
import com.notime.glyphsim.matrix.AvatarGeometry
import com.notime.glyphsim.matrix.AvatarSpriteView
import com.notime.glyphsim.matrix.ClockCue
import com.notime.glyphsim.matrix.ClockFrameSim
import com.notime.glyphsim.matrix.ClockMotion
import com.notime.glyphsim.matrix.SimulatedMatrixView
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Vollbild-Wiedergabe eines [AvatarClip] (siehe [AvatarClips] fuer die Zuordnung je Avatar) -
 * komplett stumm, wie es die Philosophie der App verlangt: ausschliesslich Pixel-Animation,
 * Kamera-Zoom/Pan, die Uhr als zweites Element (siehe [com.notime.glyphsim.matrix.ClockCue]) und
 * Texteinblendungen tragen die kleine Geschichte.
 *
 * **Lebenszyklus-sicher ohne eigenes Zutun:** die gesamte Wiedergabe laeuft in EINEM
 * [LaunchedEffect], dessen Coroutine an die Komposition dieses Composables gebunden ist - wird
 * [AvatarClipPlayer] aus der Komposition entfernt (Schliessen-Knopf, System-Zurueck ODER
 * natuerliches Ende ruft [onFinished], der Aufrufer setzt daraufhin seinen "zeige Player"-
 * Zustand auf false), bricht Compose sie automatisch ab - kein Frame spielt nach dem Schliessen
 * weiter.
 *
 * [onFinished] wird sowohl beim natuerlichen Ende als auch beim manuellen Schliessen gerufen -
 * der Aufrufer muss nicht zwischen beiden unterscheiden (siehe HomeScreen: beide fuehren gleich
 * zurueck zum Avatar-Menue). System-Zurueck und Antippen ausserhalb loesen dasselbe aus, weil
 * [Dialog] das per `onDismissRequest` schon von Haus aus so behandelt.
 */
@Composable
fun AvatarClipPlayer(clip: AvatarClip, onFinished: () -> Unit) {
    Dialog(
        onDismissRequest = onFinished,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val context = LocalContext.current
        // Einmal beim Start des Clips gelesen, nicht laufend beobachtet - der Clip ist kurz genug,
        // dass ein Stilwechsel waehrend der Wiedergabe kein realistischer Fall ist, und der Player
        // soll dafuer keinen eigenen Preference-Flow mitschleppen.
        val clockStyle = remember { ClockStylePrefs.get(context) }

        var beatIndex by remember { mutableStateOf(0) }
        var frame by remember { mutableStateOf(clip.beats.firstOrNull()?.frames?.firstOrNull()) }
        var clockFrame by remember { mutableStateOf<IntArray?>(null) }
        val scale = remember { Animatable(clip.beats.firstOrNull()?.camera?.fromScale ?: 1f) }
        val offsetX = remember { Animatable(0f) }
        val offsetY = remember { Animatable(0f) }
        // Frame-synchrone Zusatzverschiebung fuer Beats mit [ClipBeat.flightOffsets] (z.B. die
        // Rocket-Reaktion) - kein Animatable, weil sie NICHT gleichmaessig ueber die Beat-Dauer
        // faehrt, sondern jeden Frame direkt auf den zugehoerigen Wert springt.
        var flightOffsetX by remember { mutableStateOf(0f) }
        var flightOffsetY by remember { mutableStateOf(0f) }
        // Fortschritt 0..1 ueber die Dauer des AKTUELLEN Beats - treibt ClockMotion.Arc (siehe
        // graphicsLayer der Uhr unten). Bei Still-Motion oder gar keiner Uhr bleibt er ungenutzt.
        val clockArcProgress = remember { Animatable(0f) }
        // Kurzer Pop (siehe unten, clockContentKey), sobald die Uhr auf einen ANDEREN Inhalt
        // wechselt (Uhrzeit -> Erinnerungsanimation) - macht den Moment sichtbar, in dem "etwas
        // passiert", ohne aufdringlich zu wirken (nur ein dezenter Scale-Bounce, kein Blitzen).
        val clockScale = remember { Animatable(1f) }
        // Sichtbarkeit der Uhr. Sie erscheint und verschwindet nie hart, sondern blendet auf und
        // ab - siehe CLOCK_FADE_IN_MS/CLOCK_FADE_OUT_MS fuer die Begruendung.
        val clockAlpha = remember { Animatable(0f) }

        LaunchedEffect(clip) {
            var lastClockContentKey: Any? = null
            for (index in clip.beats.indices) {
                beatIndex = index
                val beat = clip.beats[index]
                if (beat.frames.isEmpty()) continue

                scale.snapTo(beat.camera.fromScale)
                offsetX.snapTo(beat.camera.fromOffsetFraction.first)
                offsetY.snapTo(beat.camera.fromOffsetFraction.second)
                clockArcProgress.snapTo(0f)
                flightOffsetX = 0f
                flightOffsetY = 0f

                val clockCue = beat.clock
                val clockContentKey = clockContentKeyFor(clockCue)
                val clockContentChanged = clockCue != null && clockContentKey != lastClockContentKey
                lastClockContentKey = clockContentKey
                // Ob die Uhr in DIESEM Beat auftaucht bzw. nach ihm weg ist - aus den Nachbar-Beats
                // abgeleitet statt als Flag an jedem Beat: die Clip-Daten sollen beschreiben, WAS
                // zu sehen ist, nicht, wie die Uebergaenge dazwischen zu animieren sind.
                val clockAppears = clockCue != null && clip.beats.getOrNull(index - 1)?.clock == null
                val clockLeaves = clockCue != null && clip.beats.getOrNull(index + 1)?.clock == null

                // Kamera-Fahrt, Uhr-Bewegung und beide Frame-Wiedergaben laufen bewusst
                // NEBENEINANDER statt nacheinander: Kamera und Uhr bewegen sich stetig ueber die
                // volle Beat-Dauer, waehrend die Pixel-Animationen unabhaengig davon in ihrem
                // eigenen Timing weiterlaufen (und noetigenfalls loopen, siehe playFramesFor).
                coroutineScope {
                    // FastOutSlowIn statt Linear: eine Kamerafahrt, die mit voller Geschwindigkeit
                    // einsetzt und ebenso abrupt stoppt, wirkt mechanisch. Angefahren und
                    // ausgerollt liest sie sich als ruhige Bewegung - derselbe sanfte Charakter,
                    // den auch die Erinnerungen selbst haben sollen.
                    val beatDurationMs = beat.effectiveDurationMs
                    val cameraTween = tween<Float>(beatDurationMs.toInt(), easing = FastOutSlowInEasing)
                    launch { scale.animateTo(beat.camera.toScale, cameraTween) }
                    launch { offsetX.animateTo(beat.camera.toOffsetFraction.first, cameraTween) }
                    launch { offsetY.animateTo(beat.camera.toOffsetFraction.second, cameraTween) }
                    launch {
                        val flightOffsets = beat.flightOffsets
                        if (flightOffsets == null) {
                            playFramesFor(beat.frames, beat.frameHoldsMs, beatDurationMs) { frame = it }
                        } else {
                            // Flug-Reaktionen spielen genau einmal durch, exakt im eigenen Tempo -
                            // dieselbe Wiedergabe wie im echten Fuettern (siehe AvatarFeeding),
                            // nicht auf die Beat-Dauer geloopt/abgeschnitten wie sonst: die
                            // Flugbahn ist Frame-fuer-Frame an diese eine Choreografie gebunden.
                            beat.frames.forEachIndexed { i, f ->
                                frame = f
                                val (dxFraction, dyFraction) = flightOffsets.getOrElse(i) { flightOffsets.last() }
                                flightOffsetX = dxFraction
                                flightOffsetY = dyFraction
                                delay(beat.frameHoldsMs.getOrElse(i) { 200L })
                            }
                        }
                    }
                    when (clockCue) {
                        null -> clockFrame = null
                        is ClockCue.Animation -> launch {
                            playFramesFor(clockCue.frames, clockCue.frameHoldsMs, beatDurationMs) {
                                clockFrame = it
                            }
                        }
                        // Ein einzelnes, statisches Bild reicht - in einem 1-2s-Beat tickt die
                        // Minute ohnehin nicht sichtbar weiter (siehe ClockFrameSim).
                        is ClockCue.LiveTime -> clockFrame = ClockFrameSim.buildFrame(style = clockStyle)
                    }
                    when (clockCue?.motion) {
                        is ClockMotion.Arc -> launch {
                            clockArcProgress.animateTo(
                                1f,
                                tween(beatDurationMs.toInt(), easing = LinearEasing)
                            )
                        }
                        // Beschleunigend statt gleichmaessig: Die Uhr loest sich langsam von ihrem
                        // Platz und faellt dann zuegig in den Avatar - eine Zustellung mit
                        // konstantem Tempo wirkt wie ein Menue-Uebergang, nicht wie eine Geste.
                        // Sie kommt bewusst VOR dem Beat-Ende an ([DELIVER_ARRIVAL_SHARE]), damit
                        // das Ankommen selbst noch zu sehen ist, bevor umgeschnitten wird.
                        is ClockMotion.Deliver -> launch {
                            clockArcProgress.animateTo(
                                1f,
                                tween(
                                    (beatDurationMs * DELIVER_ARRIVAL_SHARE).toInt(),
                                    easing = FastOutLinearInEasing
                                )
                            )
                        }
                        else -> Unit
                    }
                    if (clockContentChanged) {
                        launch {
                            clockScale.snapTo(0.85f)
                            clockScale.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
                        }
                    }
                    launch {
                        if (clockCue == null) {
                            clockAlpha.snapTo(0f)
                            return@launch
                        }
                        if (clockAppears) {
                            clockAlpha.snapTo(0f)
                            clockAlpha.animateTo(1f, tween(CLOCK_FADE_IN_MS, easing = FastOutSlowInEasing))
                        } else {
                            clockAlpha.snapTo(1f)
                        }
                        if (clockLeaves) {
                            // Ausblenden noch INNERHALB dieses Beats, damit die Uhr genau in dem
                            // Moment weg ist, in dem der naechste Beat die Reaktion startet - so,
                            // wie sie im echten Dock verschwindet, sobald die Erinnerung
                            // angenommen wurde. Ein harter Schnitt an der Beatgrenze saehe
                            // stattdessen aus, als wuerde sie im Avatar aufgehen.
                            delay((beatDurationMs - CLOCK_FADE_OUT_MS).coerceAtLeast(0L))
                            clockAlpha.animateTo(0f, tween(CLOCK_FADE_OUT_MS, easing = FastOutSlowInEasing))
                        }
                    }
                }
            }
            onFinished()
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // EINE gemeinsame Referenzgroesse fuer die Uhr-Verschiebung in X *und* Y (statt
            // maxWidth/maxHeight getrennt) - auf einem hochformatigen Bildschirm waere die
            // Y-Achse sonst um ein Vielfaches weiter gestreckt als die X-Achse und dieselbe
            // ClockMotion saehe je nach Geraet komplett anders aus. Bildschirmbreite statt der
            // Avatar-Box-Groesse als Bezug, weil die Uhr (anders als der Avatar-Kamera-Pan) klar
            // neben dem Avatar stehen soll, nicht nur in seiner unmittelbaren Naehe wackeln.
            // Groesster Abstand, den die Uhr zur Bildmitte haben darf, ohne seitlich aus dem Bild
            // zu laufen. Greift praktisch nie (siehe CLOCK_DISTANCE), ist aber die Absicherung
            // dafuer, dass ein sehr schmales Geraet die Uhr abschneidet statt sie zu zeigen.
            val maxClockOffsetPx = with(LocalDensity.current) {
                (constraints.maxWidth - CLOCK_SIZE.roundToPx()) / 2f
            }

            frame?.let { currentFrame ->
                AvatarSpriteView(
                    frame = currentFrame,
                    showBackground = false,
                    modifier = Modifier
                        .align(Alignment.Center)
                        // Breite festgelegt, Hoehe folgt dem Raster: Seit der Avatar oben
                        // Kopffreiheit hat, ist sein Sprite hoeher als breit. In einem Quadrat
                        // wuerde es auf 80 % Breite geschrumpft - und die Uhr-Abstandsrechnung
                        // unten geht von der vollen Breite aus.
                        .width(AVATAR_SIZE)
                        .height(AVATAR_SIZE * AvatarGeometry.HEIGHT / AvatarGeometry.SIZE)
                        .graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                            translationX = (offsetX.value + flightOffsetX) * size.width
                            translationY = (offsetY.value + flightOffsetY) * size.height
                        }
                )
            }

            val currentClockCue = clip.beats.getOrNull(beatIndex)?.clock
            if (currentClockCue != null) {
                clockFrame?.let { currentClockFrame ->
                    SimulatedMatrixView(
                        frame = currentClockFrame,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(CLOCK_SIZE)
                            .graphicsLayer {
                                // Winkel UND Abstandsfaktor: Nur die Zustellung veraendert den
                                // Abstand (sie faehrt nach innen), alle anderen Bewegungen halten
                                // ihn und aendern hoechstens den Winkel.
                                val (angleDeg, distanceFactor) = when (val motion = currentClockCue.motion) {
                                    is ClockMotion.Still -> motion.angleDeg to 1f
                                    is ClockMotion.Arc -> {
                                        val progress = clockArcProgress.value
                                        val angle = motion.fromAngleDeg +
                                            (motion.toAngleDeg - motion.fromAngleDeg) * progress
                                        angle to 1f
                                    }
                                    is ClockMotion.Deliver ->
                                        motion.fromAngleDeg to (1f - clockArcProgress.value)
                                }
                                val angleRad = Math.toRadians(angleDeg.toDouble())
                                val distance = CLOCK_DISTANCE.toPx() * distanceFactor
                                // Y negativ, weil der Winkel mathematisch gemeint ist (90 Grad =
                                // oben), die Bildschirm-Y-Achse aber nach unten waechst.
                                translationX = (distance * cos(angleRad)).toFloat()
                                    .coerceIn(-maxClockOffsetPx, maxClockOffsetPx)
                                translationY = -(distance * sin(angleRad)).toFloat()
                                // Beim Ankommen schrumpft die Uhr in den Avatar hinein, statt in
                                // voller Groesse ueber ihm zu verschwinden - so liest es sich als
                                // "aufgenommen" und nicht als "ausgeblendet".
                                val deliverShrink = if (currentClockCue.motion is ClockMotion.Deliver) {
                                    1f - DELIVER_SHRINK * clockArcProgress.value
                                } else {
                                    1f
                                }
                                scaleX = clockScale.value * deliverShrink
                                scaleY = clockScale.value * deliverShrink
                                alpha = clockAlpha.value
                            }
                    )
                }
            }

            // Crossfade statt hartem Austausch: die Saetze loesen einander ab, ohne dass ein
            // Wechsel als Zucken auffaellt - bei den laengeren, ruhigeren Beats faellt genau das
            // sonst am staerksten auf.
            Crossfade(
                targetState = clip.beats.getOrNull(beatIndex)?.textRes,
                animationSpec = tween(TEXT_FADE_MS),
                label = "clip-text",
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp, start = 32.dp, end = 32.dp)
            ) { textRes ->
                if (textRes != null) {
                    Text(
                        text = stringResource(textRes),
                        color = Color(0xFFF1EEE6),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Textglyphe statt Icon, wie schon an anderen Stellen der App (siehe z.B. "›" in
            // RhythmSuggestionCard) - material-icons-core deckt "Close" zwar ab, aber ein
            // weiterer Icon-Import fuer einen einzigen Knopf lohnt sich hier nicht.
            Text(
                "✕",
                color = Color(0xFF9A968E),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onFinished)
                    .padding(12.dp)
            )
        }
    }
}

/**
 * Grundgroesse des Avatars, bevor die Kamera (siehe [CameraMove]) sie skaliert.
 *
 * Bewusst kleiner als frueher (220dp): der Avatar stand so gross im Bild, dass die einzelnen
 * Pixel zu Kacheln wurden - gerade das Filigrane der Figuren, das den Reiz ausmacht, ging dabei
 * verloren. Zusammen mit den flacheren Zoomstufen in [AvatarClips] steht er jetzt sichtbar
 * ruhiger im Bild, statt es auszufuellen.
 */
private val AVATAR_SIZE = 190.dp

/**
 * Groesse der Uhr. Etwas groesser als frueher (130dp), weil die Erinnerungsanimation darin das
 * eigentlich Gezeigte ist - neben einem grossen Avatar ging sie schlicht unter. Nach oben
 * begrenzt durch ihre Auslenkung: je groesser die Uhr, desto weniger weit darf sie zur Seite
 * wandern, ohne aus dem Bild zu laufen (siehe AvatarClips.CLOCK_REST_OFFSET fuer die Rechnung).
 */
private val CLOCK_SIZE = 142.dp

/**
 * Wie weit die aeusserste LEUCHTENDE Zelle einer Ruhe-Animation vom Sprite-Mittelpunkt entfernt
 * ist, in Rasterzellen - gemessen ueber alle Spezies und Stimmungen (schlechtester Fall: Fennec,
 * 8.544; hier auf 8.6 aufgerundet, damit etwas Luft bleibt).
 *
 * Absichtlich die tatsaechliche Silhouette und nicht die halbe Rastergroesse (8.0): Zellen mit
 * Helligkeit 0 zeichnet [AvatarSpriteView] gar nicht erst, ein Sprite ist also fast nie so gross
 * wie sein Kaestchen. Und absichtlich nur die RUHE-Animationen: nur waehrend dieser ist die Uhr
 * ueberhaupt gleichzeitig zu sehen - sobald eine Reaktion laeuft (die weiter ausgreift, bis hin
 * zum Raketenflug), ist die Uhr bereits ausgeblendet.
 *
 * Wird von AvatarClipsGeometryTest gegen die echten Frames geprueft, damit eine neue oder
 * geaenderte Spezies diesen Wert nicht stillschweigend ueberholt.
 */
const val AVATAR_MAX_LIT_RADIUS_CELLS = 8.6f

/**
 * Groesster Kamera-Zoom, der laufen darf, waehrend die Uhr sichtbar ist (siehe AvatarClips:
 * CAMERA_CLOSE waehrend des Bogen-Beats). Der Avatar waechst damit, der Sicherheitsabstand muss
 * ihn also im groessten Zustand einrechnen.
 */
private const val CLOCK_MAX_AVATAR_SCALE = 1.1f

/** Sichtbare Luft zwischen Avatar-Silhouette und Uhr, damit "knapp daneben" nicht "beruehrt" wird. */
private val CLOCK_CLEARANCE = 8.dp

/**
 * Abstand der Uhr zur Bildmitte - so gross, dass sich Avatar und Uhr in KEINER Richtung beruehren
 * koennen: Radius der groessten Avatar-Silhouette plus Radius der Uhr plus etwas Luft.
 *
 * Die Uhr zeichnet ihr Punkteraster bis an den Rand ihres Kaestchens (siehe SimulatedMatrixView:
 * Rasterbreite plus Punktradius ergibt zusammen genau den Durchmesser), ihr sichtbarer Radius ist
 * also die halbe [CLOCK_SIZE].
 *
 * Das ist der Ersatz fuer die frueheren Bruchteile der Bildschirmbreite. Der Abstand haengt jetzt
 * nur noch von den beiden Groessen ab, die tatsaechlich kollidieren koennen - wer [AVATAR_SIZE]
 * oder [CLOCK_SIZE] aendert, bekommt den passenden Abstand automatisch, statt ihn nachziehen zu
 * muessen (und es beim naechsten Mal zu vergessen).
 */
private val CLOCK_DISTANCE =
    AVATAR_SIZE * (AVATAR_MAX_LIT_RADIUS_CELLS / AvatarGeometry.SIZE) * CLOCK_MAX_AVATAR_SCALE +
        CLOCK_SIZE / 2 + CLOCK_CLEARANCE

/**
 * Wie lange die Uhr zum Erscheinen braucht. Kurz genug, dass der Moment "jetzt ist sie da" klar
 * bleibt, lang genug, dass sie nicht aufblitzt.
 */
private const val CLOCK_FADE_IN_MS = 320

/**
 * Wie lange das Verschwinden der Uhr dauert - laenger als das Erscheinen, weil ein Weggehen sonst
 * wie ein Abschalten wirkt. Wird vom Ende des Beats aus gerechnet (siehe Aufrufer), damit die Uhr
 * genau dann verschwunden ist, wenn die Reaktion des Avatars uebernimmt.
 */
private const val CLOCK_FADE_OUT_MS = 520

/**
 * Welcher Anteil eines Zustell-Beats fuer den Anflug draufgeht (siehe [ClockMotion.Deliver]).
 * Der Rest ist der Moment danach: Die Uhr ist angekommen und weg, der Avatar hat sie - erst
 * dann schneidet der Clip auf die Reaktion um. Ohne diesen Rest faende das Ankommen exakt auf
 * der Schnittkante statt, also praktisch unsichtbar.
 */
private const val DELIVER_ARRIVAL_SHARE = 0.7f

/** Wie weit die Uhr beim Ankommen zusammenschrumpft - sie geht im Avatar auf, statt zu platzen. */
private const val DELIVER_SHRINK = 0.55f

/** Uebergang zwischen zwei Cliptexten - siehe Crossfade beim Aufrufer. */
private const val TEXT_FADE_MS = 400

/**
 * Vergleichsschluessel fuer den sichtbaren INHALT einer [ClockCue] - unabhaengig von ihrer
 * [ClockMotion] (siehe Aufrufer: derselbe Inhalt mit anderer Bewegung, z.B. Still gefolgt von
 * Arc mit denselben Frames, soll KEINEN erneuten Pop ausloesen). Bei [ClockCue.Animation]
 * reicht die `frames`-Liste selbst als Schluessel: Listengleichheit ist hier gewollt und
 * funktioniert korrekt, weil Beats, die "denselben Inhalt" zeigen sollen, dieselbe Listen-Instanz
 * wiederverwenden (siehe AvatarClips) statt sie neu aufzubauen.
 */
private fun clockContentKeyFor(cue: ClockCue?): Any? = when (cue) {
    null -> null
    is ClockCue.LiveTime -> "live-time"
    is ClockCue.Animation -> cue.frames
}

/**
 * Spielt [frames] genau [durationMs] lang - kuerzer als die Animation selbst wird sie
 * abgeschnitten, laenger geloopt (derselbe Trick wie die Idle-Schleifen ueberall sonst, nur mit
 * fester statt endloser Laufzeit). Der letzte Frame darf dabei kuerzer stehen bleiben als sein
 * eigentliches Hold, damit der naechste Beat puenktlich zur geplanten Zeit uebernimmt.
 *
 * Generisch fuer Avatar- UND Uhr-Frames innerhalb eines Beats - beide brauchen exakt dieselbe
 * Loop/Abschneide-Logik, nur mit unterschiedlichen Eingaben.
 */
private suspend fun playFramesFor(
    frames: List<IntArray>,
    holdsMs: List<Long>,
    durationMs: Long,
    onFrame: (IntArray) -> Unit
) {
    if (frames.isEmpty()) return
    var elapsedMs = 0L
    var index = 0
    while (elapsedMs < durationMs) {
        val currentFrame = frames[index % frames.size]
        val hold = holdsMs.getOrElse(index % holdsMs.size) { 200L }
        val actualHold = minOf(hold, durationMs - elapsedMs)
        onFrame(currentFrame)
        delay(actualHold)
        elapsedMs += actualHold
        index++
    }
}
