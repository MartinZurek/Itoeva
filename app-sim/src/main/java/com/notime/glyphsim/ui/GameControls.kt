package com.notime.glyphsim.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.notime.glyphsim.matrix.PlayControl

/**
 * **Die Fingersteuerung von Itoeva 2** (gewuenscht am 03.10. statt Steuerkreuz und Aktionsknopf).
 *
 * Finger irgendwo aufsetzen und ziehen: Die Figur laeuft in die Richtung, in die gezogen wird,
 * solange der Finger liegt - wie ein unsichtbarer Joystick, der dort entsteht, wo man den Daumen
 * aufsetzt (ein Ring zeigt ihn an, solange man zieht). Loslassen heisst stehen bleiben.
 *
 * Ohne Ziehen ist es ein Tipp: [onTap] bei einem einfachen Tipp (die Figur antippen oeffnet ihr
 * Menue), [onDoubleTap] bei zwei kurz hintereinander an derselben Stelle (dort handeln, durch eine
 * Tuer gehen). Alle Positionen in Pixeln dieses Bildschirms.
 */
@Composable
internal fun GameTouch(
    onDir: (PlayControl.Dir?) -> Unit,
    onTap: (Offset) -> Unit,
    onDoubleTap: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val deadZonePx = with(density) { 18.dp.toPx() }
    val doubleTapSlopPx = with(density) { 48.dp.toPx() }
    val dirNow by rememberUpdatedState(onDir)
    val tapNow by rememberUpdatedState(onTap)
    val doubleNow by rememberUpdatedState(onDoubleTap)
    var stickOrigin by remember { mutableStateOf<Offset?>(null) }
    var stickKnob by remember { mutableStateOf(Offset.Zero) }
    var lastTapAt by remember { mutableLongStateOf(0L) }
    var lastTapPos by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val origin = down.position
                    var dragging = false
                    var current: PlayControl.Dir? = null
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val delta = change.position - origin
                        val dir = PlayControl.swipeDir(delta.x, delta.y, deadZonePx)
                        if (dir != null) dragging = true
                        if (dragging) {
                            stickOrigin = origin
                            stickKnob = delta
                            if (dir != current) {
                                current = dir
                                dirNow(dir)
                            }
                            change.consume()
                        }
                        if (!change.pressed) break
                    }
                    if (dragging) {
                        stickOrigin = null
                        dirNow(null)
                    } else {
                        val now = down.uptimeMillis
                        val again = now - lastTapAt <= DOUBLE_TAP_MS &&
                            (origin - lastTapPos).getDistance() <= doubleTapSlopPx
                        if (again) {
                            lastTapAt = 0L
                            doubleNow(origin)
                        } else {
                            lastTapAt = now
                            lastTapPos = origin
                            tapNow(origin)
                        }
                    }
                }
            }
    ) {
        stickOrigin?.let { origin ->
            Canvas(Modifier.fillMaxSize()) {
                val ring = 34.dp.toPx()
                drawCircle(Color(0x55FFFFFF), radius = ring, center = origin, style = Stroke(width = 2.dp.toPx()))
                val len = stickKnob.getDistance()
                val knob = if (len > ring) stickKnob * (ring / len) else stickKnob
                drawCircle(Color(0x88DDF5E8), radius = 12.dp.toPx(), center = origin + knob)
            }
        }
    }
}

/** Zwei Tipps innerhalb dieser Zeit gelten als Doppeltipp. */
private const val DOUBLE_TAP_MS = 320L

/**
 * Pfeiltasten und WASD fuer Tastatur, Gamepad und Emulator - dieselbe Logik wie das Wischen.
 * Leertaste/Enter/E/Gamepad-A handeln am Platz in Reichweite. Eine unsichtbare, fokussierte
 * Flaeche; Beruehrungen gehen durch sie hindurch.
 */
@Composable
internal fun GameKeys(
    onDir: (PlayControl.Dir?) -> Unit,
    onAction: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focus = remember { FocusRequester() }
    val held = remember { mutableListOf<PlayControl.Dir>() }
    Box(
        modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { event ->
                if (event.key == Key.Spacebar || event.key == Key.Enter || event.key == Key.E ||
                    event.key == Key.ButtonA
                ) {
                    if (event.type == KeyEventType.KeyDown) onAction()
                    return@onKeyEvent true
                }
                val dir = when (event.key) {
                    Key.DirectionLeft, Key.A -> PlayControl.Dir.LEFT
                    Key.DirectionRight, Key.D -> PlayControl.Dir.RIGHT
                    Key.DirectionUp, Key.W -> PlayControl.Dir.UP
                    Key.DirectionDown, Key.S -> PlayControl.Dir.DOWN
                    else -> return@onKeyEvent false
                }
                when (event.type) {
                    KeyEventType.KeyDown -> if (held.lastOrNull() != dir) {
                        held.remove(dir)
                        held += dir
                        onDir(dir)
                    }
                    KeyEventType.KeyUp -> {
                        held.remove(dir)
                        onDir(held.lastOrNull())
                    }
                    else -> return@onKeyEvent false
                }
                true
            }
    )
    LaunchedEffect(Unit) { focus.requestFocus() }
}
