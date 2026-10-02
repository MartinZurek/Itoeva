package com.notime.glyphsim.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import com.notime.glyphsim.matrix.SimulatedMatrixView

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
        travel.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        absorb.animateTo(1f, tween(120))
        finished.value()
    }
    val sizePx = with(LocalDensity.current) { 56.dp.toPx() }
    val avatarPx = with(LocalDensity.current) { avatarSizeDp.dp.toPx() }
    // Recomputed while travelling, so a walking avatar remains the visible destination.
    val target = avatarOffset + Offset((avatarPx - sizePx) / 2f, (avatarPx - sizePx) / 2f)
    Box(
        Modifier
            .size(56.dp)
            .graphicsLayer {
                // Animate the drawing layer, without remeasuring or recomposing the symbol
                // on every frame. Avatar movement may still update the destination normally.
                translationX = from.x + (target.x - from.x) * travel.value
                translationY = from.y + (target.y - from.y) * travel.value
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
