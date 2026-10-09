package com.notime.glyphsim.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
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
import kotlinx.coroutines.delay

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
    }
}

/** Eigene Pointer-Flaeche: ein zweiter Daumen wird unabhaengig vom Welt-/Bewegungsgriff erfasst. */
@Composable
internal fun GameActionPad(enabled: Boolean, onCommand: (GameMovement.Command, PlayControl.Stick?) -> Unit,
    modifier: Modifier = Modifier) {
    val commandNow by rememberUpdatedState(onCommand)
    val slop = with(LocalDensity.current) { 18.dp.toPx() }
    var origin by remember { mutableStateOf<Offset?>(null) }
    var started by remember { mutableLongStateOf(0L) }
    var fired by remember { mutableStateOf(false) }
    val jump = stringResource(R.string.game_control_jump)
    val roll = stringResource(R.string.game_control_roll)
    val rest = stringResource(R.string.game_control_rest)
    LaunchedEffect(origin, started, enabled) {
        if (enabled && origin != null) {
            delay(500L)
            if (!fired) { fired = true; commandNow(GameMovement.Command.REST, null) }
        }
    }
    val input = if (enabled) Modifier.pointerInput(enabled, slop) {
        var finger: PointerId? = null
        try {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    for (change in event.changes) {
                        val cancelled = !change.pressed && change.previousPressed && change.isConsumed
                        if (finger == null && change.pressed && !change.previousPressed && !change.isConsumed) {
                            finger = change.id; origin = change.position; started = change.uptimeMillis; fired = false
                        }
                        if (change.id != finger) continue
                        val delta = change.position - (origin ?: change.position)
                        if (change.pressed && !fired && delta.getDistance() > slop * 1.7f) {
                            fired = true
                            val command = if (kotlin.math.abs(delta.x) > kotlin.math.abs(delta.y))
                                GameMovement.Command.ROLL else if (delta.y < 0f) GameMovement.Command.JUMP else GameMovement.Command.REST
                            commandNow(command, if (command == GameMovement.Command.ROLL)
                                PlayControl.stick(delta.x, delta.y, slop, 0f) else null)
                        }
                        change.consume()
                        if (!change.pressed && change.previousPressed) {
                            if (!fired && !cancelled) commandNow(GameMovement.Command.JUMP, null)
                            finger = null; origin = null
                        }
                    }
                }
            }
        } finally { origin = null }
    } else Modifier
    Canvas(modifier.testTag("game-action-pad").semantics {
        contentDescription = "$jump · $roll · $rest"
        if (!enabled) disabled()
        customActions = listOf(
            CustomAccessibilityAction(jump) { if (enabled) commandNow(GameMovement.Command.JUMP, null); enabled },
            CustomAccessibilityAction(roll) { if (enabled) commandNow(GameMovement.Command.ROLL, null); enabled },
            CustomAccessibilityAction(rest) { if (enabled) commandNow(GameMovement.Command.REST, null); enabled })
    }.then(input)) {
        val alpha = if (enabled) 1f else .65f
        val action = center
        val radius = size.minDimension / 2f
        drawCircle(Color(0x337EBBA4).copy(alpha = .20f * alpha), radius, action)
        drawCircle(Color(0x88DDF5E8).copy(alpha = .5f * alpha), radius, action,
            style = Stroke(if (origin == null) 2.dp.toPx() else 4.dp.toPx()))
        drawCircle(Color(0x55DDF5E8).copy(alpha = .25f * alpha), radius * .55f, action, style = Stroke(1.dp.toPx()))
        val ink = Color(0xFFDDF5E8).copy(alpha = .8f * alpha)
        val w = 3.dp.toPx()
        drawLine(ink, action + Offset(0f, 12.dp.toPx()), action + Offset(0f, -12.dp.toPx()), w)
        drawLine(ink, action + Offset(-8.dp.toPx(), -4.dp.toPx()), action + Offset(0f, -12.dp.toPx()), w)
        drawLine(ink, action + Offset(8.dp.toPx(), -4.dp.toPx()), action + Offset(0f, -12.dp.toPx()), w)
        drawArc(ink, 70f, 220f, false, action + Offset(-30.dp.toPx(), -10.dp.toPx()),
            androidx.compose.ui.geometry.Size(12.dp.toPx(), 20.dp.toPx()), style = Stroke(w))
        drawArc(ink, -110f, 220f, false, action + Offset(18.dp.toPx(), -10.dp.toPx()),
            androidx.compose.ui.geometry.Size(12.dp.toPx(), 20.dp.toPx()), style = Stroke(w))
        drawLine(ink, action + Offset(-4.dp.toPx(), 24.dp.toPx()), action + Offset(-4.dp.toPx(), 32.dp.toPx()), w)
        drawLine(ink, action + Offset(4.dp.toPx(), 24.dp.toPx()), action + Offset(4.dp.toPx(), 32.dp.toPx()), w)
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
