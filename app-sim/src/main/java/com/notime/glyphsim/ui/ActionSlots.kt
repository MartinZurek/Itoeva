package com.notime.glyphsim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.R
import com.notime.glyphsim.matrix.SimulatedMatrixView
import kotlin.math.roundToInt

/**
 * Feste Anzahl an Speicherplaetzen - vier statt einer wachsenden Liste, weil [ActionSlotsColumn]
 * genau so viele fest gezeichnete Plaetze anbietet. Ohne Kartendeck, ohne Raritaeten: ein Platz
 * ist entweder frei oder belegt, mehr Zustand gibt es bewusst nicht.
 */
internal const val ACTION_SLOT_COUNT = 4

/**
 * Eine Aktion, die der Spieler von der Uhr in einen Speicherplatz gezogen hat, statt sie sofort
 * auf den Avatar anzuwenden (siehe [HomeScreen]s `saveToSlot`). Sie bleibt hier liegen, bis sie
 * von dort aus gefuettert wird - [frames] dient dabei nur der Vorschau im Platz selbst; welche
 * Reaktion das Fuettern spaeter zeigt, entscheidet [AvatarFeeding.playReaction] unabhaengig davon
 * ueber [animationType]/[libraryAnimationLabel].
 */
internal data class SavedAction(
    val reminderId: Long,
    val occurrenceId: Long,
    val animationType: AnimationType?,
    val libraryAnimationLabel: String?,
    val frames: List<IntArray>
)

/**
 * Vier feste Speicherplaetze in einer Spalte - erreichbar per Ziehen der Uhr hierher (ablegen)
 * und von hier aus per Ziehen auf den Avatar (anwenden), siehe [ActionSlot].
 *
 * Eine einzelne Spalte statt zweier Gruppen links und rechts: die Uhr sitzt oben in der Mitte,
 * eine Spalte haelt den Ziehweg zu allen vier Plaetzen dadurch ungefaehr gleich lang, egal welcher
 * gemeint ist. Zwei Gruppen haetten diesen Weg fuer die Haelfte der Plaetze verkuerzt, dafuer aber
 * auf BEIDEN Seiten Platz von der zentrierten Welt (Uhr und Avatar) abgezogen - eine Spalte nimmt
 * ihn nur auf einer.
 */
@Composable
internal fun ActionSlotsColumn(
    slots: List<SavedAction?>,
    avatarBounds: Rect,
    onBoundsChanged: (index: Int, bounds: Rect) -> Unit,
    onDropOnAvatar: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        slots.forEachIndexed { index, saved ->
            ActionSlot(
                index = index,
                saved = saved,
                avatarBounds = avatarBounds,
                onBoundsChanged = { onBoundsChanged(index, it) },
                onDropOnAvatar = { onDropOnAvatar(index) }
            )
        }
    }
}

/**
 * Ein einzelner Speicherplatz: leer eine schlichte umrandete Flaeche, belegt eine kleine Vorschau
 * der abgelegten Aktion, die sich wie die Uhr auf den Avatar ziehen laesst.
 *
 * Dieselbe Kollisions- statt Loslass-Logik wie bei der Uhr (siehe [AvatarFeeding.overlaps] und
 * `HomeScreen`s `LaunchedEffect(clockDrag)`): [onDropOnAvatar] feuert, sobald sich die Flaeche
 * mit dem Avatar ueberschneidet, nicht erst beim Abheben des Fingers.
 */
@Composable
private fun ActionSlot(
    index: Int,
    saved: SavedAction?,
    avatarBounds: Rect,
    onBoundsChanged: (Rect) -> Unit,
    onDropOnAvatar: () -> Unit
) {
    // Ueber den Belegungs-Schluessel neu erzeugt: sobald ein Platz frei wird (nach bestaetigtem
    // Fuettern) oder neu belegt wird, beginnt die Verschiebung wieder bei Null - sonst haenge
    // die naechste Aktion an diesem Platz an der Position der vorigen.
    var dragOffset by remember(saved?.occurrenceId) { mutableStateOf(Offset.Zero) }
    var bounds by remember { mutableStateOf(Rect.Zero) }

    LaunchedEffect(dragOffset) {
        if (saved == null || dragOffset == Offset.Zero) return@LaunchedEffect
        if (AvatarFeeding.overlaps(bounds, avatarBounds)) {
            onDropOnAvatar()
        }
    }

    val topicLabel = saved?.libraryAnimationLabel
        ?: saved?.animationType?.let { stringResource(it.labelRes) }
    val slotNumber = index + 1
    val description = if (saved != null) {
        stringResource(
            R.string.a11y_action_slot_filled,
            slotNumber,
            topicLabel ?: stringResource(R.string.a11y_reminder_generic)
        )
    } else {
        stringResource(R.string.a11y_action_slot_empty, slotNumber)
    }
    val applyActionLabel = stringResource(R.string.a11y_action_slot_apply)

    Box(
        modifier = Modifier
            .size(56.dp)
            // .offset VOR .onGloballyPositioned - wie bei der Uhr in HomeScreen: die gemeldeten
            // Bounds muessen die gezogene Position enthalten, sonst greift die Kollisionspruefung
            // gegen den Avatar ins Leere.
            .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
            .onGloballyPositioned {
                bounds = it.boundsInRoot()
                onBoundsChanged(bounds)
            }
            .clip(RoundedCornerShape(14.dp))
            .background(if (saved != null) TamaPalette.BubbleBackground else TamaPalette.RowBackground)
            .border(
                width = 1.dp,
                color = TamaPalette.TextMuted.copy(alpha = if (saved != null) 0f else 0.35f),
                shape = RoundedCornerShape(14.dp)
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
                                // Ueberschneidet sich der Platz gerade mit dem Avatar, laeuft das
                                // Fuettern bereits (siehe LaunchedEffect oben) - dann NICHT
                                // zurueckschnappen, der Platz verschwindet gleich ohnehin.
                                if (!AvatarFeeding.overlaps(bounds, avatarBounds)) {
                                    dragOffset = Offset.Zero
                                }
                            },
                            onDragCancel = { dragOffset = Offset.Zero }
                        )
                    }
                } else {
                    Modifier
                }
            )
            .semantics {
                contentDescription = description
                if (saved != null) {
                    customActions = listOf(
                        CustomAccessibilityAction(applyActionLabel) { onDropOnAvatar(); true }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (saved != null) {
            SimulatedMatrixView(
                frame = saved.frames.firstOrNull() ?: IntArray(0),
                showPuck = false,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            )
        }
    }
}
