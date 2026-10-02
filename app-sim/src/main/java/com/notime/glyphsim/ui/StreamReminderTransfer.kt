package com.notime.glyphsim.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.notime.glyphsim.matrix.SimulatedMatrixView
import kotlin.math.roundToInt

/** A visual receipt for an accepted offer; it never feeds or clears a reminder itself. */
internal data class StreamSlotTransfer(val slotIndex: Int, val saved: SavedAction)

@Composable
internal fun StreamReminderTransfer(
    saved: SavedAction,
    from: Offset,
    avatarOffset: Offset,
    avatarSizeDp: Float,
    onFinished: () -> Unit
) {
    val travel = remember { Animatable(0f) }
    val absorb = remember { Animatable(0f) }
    val finished = rememberUpdatedState(onFinished)
    LaunchedEffect(saved.occurrenceId) {
        travel.animateTo(1f, tween(1_100, easing = FastOutSlowInEasing))
        absorb.animateTo(1f, tween(220))
        finished.value()
    }
    val sizePx = with(LocalDensity.current) { 56.dp.toPx() }
    val avatarPx = with(LocalDensity.current) { avatarSizeDp.dp.toPx() }
    // Recomputed while travelling, so a walking avatar remains the visible destination.
    val target = avatarOffset + Offset((avatarPx - sizePx) / 2f, (avatarPx - sizePx) / 2f)
    val position = from + (target - from) * travel.value
    Box(
        Modifier
            .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
            .size(56.dp)
            .graphicsLayer {
                alpha = 1f - absorb.value
                scaleX = 1f - absorb.value * 0.45f
                scaleY = scaleX
            }
            .clip(CircleShape)
            .background(TamaPalette.BubbleBackground)
            .border(2.dp, Color(0xFF7FD1A6), CircleShape)
    ) {
        SimulatedMatrixView(
            frame = ActionSlotSymbols.frameFor(saved),
            showPuck = false,
            modifier = Modifier.fillMaxSize().padding(8.dp)
        )
    }
}
