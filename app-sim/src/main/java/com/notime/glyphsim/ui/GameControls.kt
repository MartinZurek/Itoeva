package com.notime.glyphsim.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.notime.glyphsim.matrix.PlayControl

/**
 * **Das Steuerkreuz von Itoeva 2** (siehe [PlayControl]).
 *
 * Vier Tasten im Kreuz, unten links ueber der Welt. Gedrueckt halten heisst gehen, loslassen
 * heisst stehen bleiben. Werden zwei gehalten, gilt die zuletzt gedrueckte - und laesst man sie
 * los, wieder die andere. Halb durchsichtig, damit die Welt dahinter sichtbar bleibt.
 */
@Composable
internal fun GameDpad(
    onDir: (PlayControl.Dir?) -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 54.dp
) {
    val held = remember { mutableListOf<PlayControl.Dir>() }
    fun press(dir: PlayControl.Dir) {
        held.remove(dir)
        held += dir
        onDir(dir)
    }
    fun release(dir: PlayControl.Dir) {
        held.remove(dir)
        onDir(held.lastOrNull())
    }
    Box(modifier.size(buttonSize * 3)) {
        for (dir in PlayControl.Dir.entries) {
            val align = when (dir) {
                PlayControl.Dir.LEFT -> Alignment.CenterStart
                PlayControl.Dir.RIGHT -> Alignment.CenterEnd
                PlayControl.Dir.UP -> Alignment.TopCenter
                PlayControl.Dir.DOWN -> Alignment.BottomCenter
            }
            var down by remember { mutableStateOf(false) }
            Box(
                Modifier
                    .align(align)
                    .size(buttonSize)
                    .clip(CircleShape)
                    .background(if (down) Color(0x667FD1A6) else Color(0x33FFFFFF))
                    .semantics { contentDescription = dir.name }
                    .pointerInput(dir) {
                        awaitEachGesture {
                            awaitFirstDown()
                            down = true
                            press(dir)
                            waitForUpOrCancellation()
                            down = false
                            release(dir)
                        }
                    }
            ) {
                Canvas(Modifier.fillMaxSize().padding(16.dp)) {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        when (dir) {
                            PlayControl.Dir.LEFT -> { moveTo(0f, h / 2); lineTo(w, 0f); lineTo(w, h) }
                            PlayControl.Dir.RIGHT -> { moveTo(w, h / 2); lineTo(0f, 0f); lineTo(0f, h) }
                            PlayControl.Dir.UP -> { moveTo(w / 2, 0f); lineTo(w, h); lineTo(0f, h) }
                            PlayControl.Dir.DOWN -> { moveTo(w / 2, h); lineTo(0f, 0f); lineTo(w, 0f) }
                        }
                        close()
                    }
                    drawPath(path, if (down) Color(0xFFDDF5E8) else Color(0xCCFFFFFF))
                }
            }
        }
    }
}

/**
 * Pfeiltasten und WASD fuer Tastatur, Gamepad und Emulator - dieselbe Logik wie das Kreuz.
 * Eine unsichtbare, fokussierte Flaeche; Beruehrungen gehen durch sie hindurch.
 */
@Composable
internal fun GameKeys(onDir: (PlayControl.Dir?) -> Unit, modifier: Modifier = Modifier) {
    val focus = remember { FocusRequester() }
    val held = remember { mutableListOf<PlayControl.Dir>() }
    Box(
        modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .onKeyEvent { event ->
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

