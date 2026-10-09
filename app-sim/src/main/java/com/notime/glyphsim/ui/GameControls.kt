package com.notime.glyphsim.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.notime.glyphsim.R
import com.notime.glyphsim.matrix.GameMovement
import com.notime.glyphsim.matrix.PlayControl

/** Zwei unabhaengige Daumen; die rechte Hand darf springen, waehrend links weiter gelenkt wird. */
@Composable
internal fun GameTouch(
    onStick: (PlayControl.Stick) -> Unit,
    onCommand: (GameMovement.Command, PlayControl.Stick?) -> Unit,
    onTap: (Offset) -> Unit,
    onDoubleTap: (Offset) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val radius = with(density) { 56.dp.toPx() }
    val slop = with(density) { 18.dp.toPx() }
    val doubleSlop = with(density) { 48.dp.toPx() }
    val bottom = with(density) { 90.dp.toPx() }
    val stickNow by rememberUpdatedState(onStick)
    val commandNow by rememberUpdatedState(onCommand)
    val tapNow by rememberUpdatedState(onTap)
    val doubleNow by rememberUpdatedState(onDoubleTap)
    var stickOrigin by remember { mutableStateOf<Offset?>(null) }
    var stickKnob by remember { mutableStateOf(Offset.Zero) }
    var lastTapAt by remember { mutableLongStateOf(0L) }
    var lastTapPos by remember { mutableStateOf(Offset.Zero) }
    val jumpLabel = stringResource(R.string.game_control_jump)
    val rollLabel = stringResource(R.string.game_control_roll)
    val restLabel = stringResource(R.string.game_control_rest)
    DisposableEffect(Unit) { onDispose { stickNow(PlayControl.Stick()) } }
    val touchInput = if(enabled) Modifier.pointerInput(enabled, radius, slop, bottom) {
        var left: PointerId? = null
        val taps = mutableMapOf<PointerId, Pair<Offset, Long>>()
        val dragged = mutableSetOf<PointerId>()
        try {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    for (change in event.changes) {
                        // ACTION_CANCEL liefert bereits konsumierte Up-Aenderungen.
                        // Vor unserem consume merken: Abbruch ist kein Tipp.
                        val cancelledRelease = !change.pressed && change.previousPressed && change.isConsumed
                        if (change.pressed && !change.previousPressed && !change.isConsumed) {
                            val origin = change.position
                            when {
                                origin.x < size.width * 0.5f && origin.y > size.height * 0.52f && left == null -> {
                                    left = change.id
                                    stickOrigin = origin
                                    stickKnob = Offset.Zero
                                    taps[change.id] = origin to change.uptimeMillis
                                    change.consume()
                                }
                                else -> taps[change.id] = origin to change.uptimeMillis
                            }
                        }
                        if (change.id == left) {
                            val delta = change.position - (stickOrigin ?: change.position)
                            stickKnob = delta
                            if (delta.getDistance() > slop) dragged += change.id
                            stickNow(PlayControl.stick(delta.x, delta.y, radius))
                            change.consume()
                        } else {
                            taps[change.id]?.let { (origin, _) ->
                                if ((change.position - origin).getDistance() > slop) dragged += change.id
                            }
                        }
                        if (!change.pressed && change.previousPressed) {
                            if (change.id == left) {
                                left = null
                                stickOrigin = null
                                stickNow(PlayControl.Stick())
                            }
                            taps.remove(change.id)?.let { (origin, at) ->
                                if (change.id !in dragged && !cancelledRelease) {
                                    val again = at - lastTapAt in 1..320L &&
                                        (origin - lastTapPos).getDistance() <= doubleSlop
                                    if (again) { lastTapAt = 0L; doubleNow(origin) }
                                    else { lastTapAt = at; lastTapPos = origin; tapNow(origin) }
                                }
                            }
                            dragged.remove(change.id)
                        }
                    }
                }
            }
        } finally {
            stickOrigin = null
            stickNow(PlayControl.Stick())
        }
    } else Modifier
    Box(modifier.fillMaxSize().semantics {
        if(!enabled) disabled()
        customActions = listOf(
            CustomAccessibilityAction(jumpLabel) { if(enabled) commandNow(GameMovement.Command.JUMP,null);enabled },
            CustomAccessibilityAction(rollLabel) { if(enabled) commandNow(GameMovement.Command.ROLL,null);enabled },
            CustomAccessibilityAction(restLabel) { if(enabled) commandNow(GameMovement.Command.REST,null);enabled }
        )
    }.then(touchInput)) {
        Row(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.game_control_pace), color = Color(0xAADDF5E8), fontSize = 10.sp)
            Text(stringResource(R.string.game_control_gestures), color = Color(0xAADDF5E8), fontSize = 10.sp)
        }
        Canvas(Modifier.fillMaxSize()) {
            val alpha = if (enabled) 1f else 0.65f
            val home = Offset(radius * 1.3f, size.height - bottom)
            val origin = stickOrigin ?: home
            drawCircle(Color(0x337EBBA4).copy(alpha = 0.20f * alpha), radius, origin)
            drawCircle(Color(0x88DDF5E8).copy(alpha = 0.5f * alpha), radius, origin,
                style = Stroke(2.dp.toPx()))
            drawCircle(Color(0x55DDF5E8).copy(alpha = 0.25f * alpha), radius * 0.55f, origin,
                style = Stroke(1.dp.toPx()))
            val len = stickKnob.getDistance()
            val knob = if (stickOrigin == null) Offset.Zero else if (len > radius) stickKnob * (radius / len) else stickKnob
            drawCircle(Color(0xCCDDF5E8).copy(alpha = 0.7f * alpha), 14.dp.toPx(), origin + knob)
        }
        GameActionPad(enabled, onCommand,
            Modifier.align(Alignment.BottomEnd).padding(end = 16.8.dp, bottom = 34.dp).size(112.dp))
        // Rollen links neben, Hinsetzen ueber dem Sprungknopf.
        GameSideButtons(enabled, onCommand,
            Modifier.align(Alignment.BottomEnd).padding(end = 96.dp, bottom = 22.dp).size(width = 132.dp, height = 186.dp))
    }
}

/**
 * Drei getrennte Knoepfe statt einer Wischflaeche: Springen (gross), Rollen, Hinsetzen.
 *
 * Auf dem Telefon kamen Sprung und Rolle aus der frueheren Wischflaeche nicht zuverlaessig an:
 * Der Befehl fiel erst beim Loslassen bzw. nach einer Wischstrecke, und ein Seitwaerts-Wischen
 * am rechten Bildschirmrand ist dort Androids Zurueck-Geste - das System bricht die Beruehrung
 * dann ab. Jetzt loest jeder Knopf schon beim Aufsetzen aus, und die Flaeche ist von den
 * System-Gesten ausgenommen. Jeder Knopf hat eine eigene Pointer-Flaeche; der linke Daumen bleibt
 * unabhaengig davon am Steuern.
 */
@Composable
internal fun GameActionPad(enabled: Boolean, onCommand: (GameMovement.Command, PlayControl.Stick?) -> Unit,
    modifier: Modifier = Modifier) {
    val jump = stringResource(R.string.game_control_jump)
    val roll = stringResource(R.string.game_control_roll)
    val rest = stringResource(R.string.game_control_rest)
    // 112 dp wie die fruehere Flaeche: der Sprungknopf sitzt an derselben Stelle.
    Box(modifier.systemGestureExclusion().semantics {
        contentDescription = "$jump · $roll · $rest"
        if (!enabled) disabled()
    }) {
        GameActionButtonCircle(enabled, GameMovement.Command.JUMP, jump, onCommand,
            Modifier.align(Alignment.Center).size(84.dp).testTag("game-action-pad")) { c, r, ink, w ->
            drawLine(ink, c + Offset(0f, r * .42f), c + Offset(0f, -r * .42f), w)
            drawLine(ink, c + Offset(-r * .3f, -r * .14f), c + Offset(0f, -r * .42f), w)
            drawLine(ink, c + Offset(r * .3f, -r * .14f), c + Offset(0f, -r * .42f), w)
        }
    }
}

/** Rollen und Hinsetzen: kleinere Knoepfe neben bzw. ueber dem Sprungknopf. */
@Composable
internal fun GameSideButtons(enabled: Boolean, onCommand: (GameMovement.Command, PlayControl.Stick?) -> Unit,
    modifier: Modifier = Modifier) {
    val roll = stringResource(R.string.game_control_roll)
    val rest = stringResource(R.string.game_control_rest)
    Box(modifier.systemGestureExclusion()) {
        GameActionButtonCircle(enabled, GameMovement.Command.ROLL, roll, onCommand,
            Modifier.align(Alignment.BottomStart).size(60.dp).testTag("game-roll-button")) { c, r, ink, w ->
            drawArc(ink, 200f, 280f, false, c - Offset(r * .42f, r * .42f),
                androidx.compose.ui.geometry.Size(r * .84f, r * .84f), style = Stroke(w))
            drawLine(ink, c + Offset(r * .42f, -r * .02f), c + Offset(r * .58f, -r * .22f), w)
            drawLine(ink, c + Offset(r * .42f, -r * .02f), c + Offset(r * .18f, -r * .12f), w)
        }
        GameActionButtonCircle(enabled, GameMovement.Command.REST, rest, onCommand,
            Modifier.align(Alignment.TopEnd).size(56.dp).testTag("game-rest-button")) { c, r, ink, w ->
            drawLine(ink, c + Offset(-r * .35f, -r * .12f), c + Offset(0f, r * .22f), w)
            drawLine(ink, c + Offset(r * .35f, -r * .12f), c + Offset(0f, r * .22f), w)
            drawLine(ink, c + Offset(-r * .4f, r * .4f), c + Offset(r * .4f, r * .4f), w)
        }
    }
}

/** Ein runder Knopf, der beim Aufsetzen des Fingers genau einmal [command] ausloest. */
@Composable
private fun GameActionButtonCircle(enabled: Boolean, command: GameMovement.Command, label: String,
    onCommand: (GameMovement.Command, PlayControl.Stick?) -> Unit, modifier: Modifier,
    icon: androidx.compose.ui.graphics.drawscope.DrawScope.(Offset, Float, Color, Float) -> Unit) {
    val commandNow by rememberUpdatedState(onCommand)
    var pressed by remember { mutableStateOf(false) }
    val input = if (enabled) Modifier.pointerInput(command) {
        awaitPointerEventScope {
            var finger: PointerId? = null
            while (true) {
                val event = awaitPointerEvent()
                for (change in event.changes) {
                    if (finger == null && change.pressed && !change.previousPressed) {
                        finger = change.id
                        pressed = true
                        change.consume()
                        commandNow(command, null)
                    } else if (change.id == finger) {
                        change.consume()
                        if (!change.pressed) { finger = null; pressed = false }
                    }
                }
            }
        }
    } else Modifier
    Canvas(modifier.semantics {
        contentDescription = label
        if (!enabled) disabled()
        customActions = listOf(CustomAccessibilityAction(label) { if (enabled) commandNow(command, null); enabled })
    }.then(input)) {
        val alpha = if (enabled) 1f else .65f
        val c = center
        val r = size.minDimension / 2f
        drawCircle(Color(0x337EBBA4).copy(alpha = (if (pressed) .45f else .20f) * alpha), r, c)
        drawCircle(Color(0x88DDF5E8).copy(alpha = .6f * alpha), r - 1.dp.toPx(), c,
            style = Stroke(if (pressed) 4.dp.toPx() else 2.dp.toPx()))
        icon(c, r, Color(0xFFDDF5E8).copy(alpha = .85f * alpha), 3.dp.toPx())
    }
}

/** Tastatur und Gamepad benutzen denselben Analogwert und dieselben freiwilligen Handlungen. */
@Composable
internal fun GameKeys(
    onStick: (PlayControl.Stick) -> Unit,
    onAction: () -> Unit = {},
    onCommand: (GameMovement.Command, PlayControl.Stick?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val focus = remember { FocusRequester() }
    val held = remember { mutableSetOf<Key>() }
    val stickNow by rememberUpdatedState(onStick)
    DisposableEffect(Unit) { onDispose { held.clear(); stickNow(PlayControl.Stick()) } }
    fun update() {
        fun any(a: Key, b: Key) = a in held || b in held
        val x = (if (any(Key.DirectionRight, Key.D)) 1f else 0f) - (if (any(Key.DirectionLeft, Key.A)) 1f else 0f)
        val y = (if (any(Key.DirectionDown, Key.S)) 1f else 0f) - (if (any(Key.DirectionUp, Key.W)) 1f else 0f)
        val raw = PlayControl.stick(x, y, 1f, 0f)
        val strength = if (any(Key.ShiftLeft, Key.ShiftRight)) 1f else 0.55f
        stickNow(PlayControl.Stick(raw.x * strength, raw.y * strength))
    }
    Box(modifier.fillMaxSize().focusRequester(focus).focusable().onKeyEvent { event ->
        val command = when (event.key) {
            Key.Spacebar, Key.ButtonA -> GameMovement.Command.JUMP
            Key.Q, Key.ButtonX -> GameMovement.Command.ROLL
            Key.R, Key.ButtonB -> GameMovement.Command.REST
            else -> null
        }
        val action = event.key in setOf(Key.Enter, Key.E, Key.ButtonY)
        val movement = event.key in setOf(Key.DirectionLeft, Key.A, Key.DirectionRight, Key.D,
            Key.DirectionUp, Key.W, Key.DirectionDown, Key.S, Key.ShiftLeft, Key.ShiftRight)
        if (command == null && !action && !movement) return@onKeyEvent false
        if (event.type == KeyEventType.KeyDown) {
            if (held.add(event.key)) {
                if (command != null) onCommand(command, null)
                if (action) onAction()
            }
        } else if (event.type == KeyEventType.KeyUp) held.remove(event.key)
        if (movement) update()
        true
    })
    LaunchedEffect(Unit) { focus.requestFocus() }
}
