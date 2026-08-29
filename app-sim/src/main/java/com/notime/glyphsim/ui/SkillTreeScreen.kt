package com.notime.glyphsim.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.notime.glyphcore.data.AnimationTreeLayout
import com.notime.glyphsim.R
import com.notime.glyphsim.skilltree.LevelUnlocks
import com.notime.glyphsim.skilltree.NodeState
import com.notime.glyphsim.skilltree.SkillTreeRow
import com.notime.glyphsim.skilltree.SkillTreeRows

/**
 * **Das Wander-Brett** - der Skillbaum als raeumliches Netz aus Knoten und Verbindungslinien statt
 * als Liste. Vorbild: der Skillbaum aus Diablo 2, das Sphere Grid aus Final Fantasy X.
 *
 * **Freischalten ist ab hier eine Wahl, keine Automatik.** Jeder Levelaufstieg gibt einen
 * Skillpunkt ([LevelUnlocks.due]); der Spieler tippt selbst auf einen erreichbaren Nachbarknoten
 * ([NodeState.AVAILABLE], siehe [SkillTreeRows]) statt ein algorithmisches Angebot vorgesetzt zu
 * bekommen (die fruehere `LevelUnlockDialog`/`UnlockOffers.build`-Automatik, siehe SKILLBAUM.md P11).
 *
 * Die Zuordnung Knoten -> Zustand bleibt vollstaendig in [SkillTreeRows] (siehe `SkillTreeRowsTest`)
 * - hier aendert sich nur, WIE sie gezeichnet wird: als Positionsraster ([AnimationTreeLayout])
 * statt als eingerueckte Liste, pan- und zoombar wie ein Kartenausschnitt.
 *
 * Tiefenunabhaengig: [AnimationTreeLayout] rechnet jede Ebene aus, keine Annahme "genau drei
 * Stufen" steckt hier oder dort im Code.
 */
@Composable
fun SkillTreeScreen(
    unlocked: Set<String>,
    level: Int,
    xpToNextLevel: Int,
    onUnlock: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = remember(unlocked) { SkillTreeRows.build(unlocked) }
    val positions = remember { AnimationTreeLayout.compute() }
    val pointsRemaining = remember(level, unlocked) { LevelUnlocks.due(level, unlocked) }

    var scale by remember { mutableStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
        pan += panChange
    }

    // Einmal in Pixel umgerechnete Mittelpunkte, nur fuer die Kanten - die Knoten selbst bleiben
    // in Dp und wandern ueber Modifier.offset, das braucht keine Umrechnung.
    val density = LocalDensity.current
    val nodeCenters: Map<String, Offset> = remember(positions, density) {
        with(density) {
            positions.mapValues { (_, pos) ->
                Offset(
                    x = (boardX(pos.x) + NODE_SIZE / 2).toPx(),
                    y = (boardY(pos.y) + NODE_SIZE / 2).toPx()
                )
            }
        }
    }
    val stateById = remember(rows) { rows.associate { it.node.id to it.state } }

    // Die tatsaechliche Ausdehnung des Bretts - explizit statt erraten. Ohne feste Groesse haette
    // die innere Box nichts, woran sich ihre eigene Groesse bemessen liesse (alle Kinder haengen
    // per Modifier.offset daneben, nicht ineinander), und das Canvas fuer die Kanten waere null
    // Pixel hoch.
    val boardSize = remember(positions) {
        val maxX = positions.values.maxOfOrNull { it.x } ?: 0f
        val maxY = positions.values.maxOfOrNull { it.y } ?: 0
        DpSize(
            width = boardX(maxX) + NODE_SIZE + BOARD_PADDING,
            height = boardY(maxY) + NODE_SIZE + BOARD_PADDING
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    stringResource(R.string.skill_tree_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = TamaPalette.TextPrimary
                )
                Text(
                    stringResource(R.string.skill_tree_level, level, xpToNextLevel),
                    style = MaterialTheme.typography.labelSmall,
                    color = TamaPalette.TextMuted
                )
            }
            if (pointsRemaining > 0) {
                Text(
                    stringResource(R.string.skill_tree_points_available, pointsRemaining),
                    style = MaterialTheme.typography.labelMedium,
                    color = TamaPalette.TextPrimary
                )
            }
            TextButton(onClick = onClose) { Text(stringResource(R.string.skill_tree_close)) }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .transformable(transformState)
        ) {
            Box(
                modifier = Modifier
                    .size(boardSize)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = pan.x,
                        translationY = pan.y,
                        // Von der Ecke aus skalieren/verschieben, nicht von der Mitte - sonst
                        // springt das Brett beim ersten Zoomen, weil sich der Ursprung mitverschiebt.
                        transformOrigin = TransformOrigin(0f, 0f)
                    )
            ) {
                Canvas(modifier = Modifier.size(boardSize)) {
                    for (row in rows) {
                        val parentId = row.node.parentId ?: continue
                        val from = nodeCenters[parentId] ?: continue
                        val to = nodeCenters[row.node.id] ?: continue
                        // Ein hell gezeichneter Kante ist der Weg, den man schon gegangen ist -
                        // beide Enden muessen dafuer offen sein, nicht nur eines.
                        val begangen = stateById[parentId] == NodeState.UNLOCKED &&
                            row.state == NodeState.UNLOCKED
                        drawLine(
                            color = if (begangen) TamaPalette.TextPrimary else TamaPalette.TextMuted,
                            start = from,
                            end = to,
                            alpha = if (begangen) 0.9f else 0.22f,
                            strokeWidth = 3f
                        )
                    }
                }

                for (row in rows) {
                    val pos = positions[row.node.id] ?: continue
                    NodeChip(
                        row = row,
                        tappable = row.state == NodeState.AVAILABLE && pointsRemaining > 0,
                        onUnlock = onUnlock,
                        modifier = Modifier.offset(x = boardX(pos.x), y = boardY(pos.y))
                    )
                }
            }
        }
    }
}

private fun boardX(x: Float): Dp = NODE_SPACING_X * x + BOARD_PADDING
private fun boardY(y: Int): Dp = NODE_SPACING_Y * y.toFloat() + BOARD_PADDING

@Composable
private fun NodeChip(
    row: SkillTreeRow,
    tappable: Boolean,
    onUnlock: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val title = row.node.rememberTitle()
    val stateLabel = when (row.state) {
        NodeState.UNLOCKED -> null
        NodeState.AVAILABLE -> stringResource(R.string.skill_tree_state_available)
        NodeState.LOCKED -> stringResource(R.string.skill_tree_state_locked)
        NodeState.PENDING_ART -> stringResource(R.string.skill_tree_state_pending)
    }
    val background = when (row.state) {
        NodeState.UNLOCKED -> TamaPalette.BubbleBackground
        NodeState.AVAILABLE -> TamaPalette.ChoiceBackground
        NodeState.LOCKED, NodeState.PENDING_ART -> TamaPalette.Background
    }
    // Nur gesperrte/ungezeichnete Knoten treten zurueck - ein AVAILABLE-Knoten ohne Punkt darf
    // trotzdem voll sichtbar bleiben, sonst sieht er aus wie gesperrt statt wie "fast erreichbar".
    val dimmed = row.state == NodeState.LOCKED || row.state == NodeState.PENDING_ART

    Box(
        modifier = modifier
            .size(NODE_SIZE)
            .clip(CircleShape)
            .background(background)
            .let { base -> if (tappable) base.clickable { onUnlock(row.node.id) } else base }
            .alpha(if (dimmed) 0.4f else 1f)
            // Ein Screenreader soll "Basketball, als Naechstes dran" lesen, keine zwei getrennten
            // Textbausteine - dasselbe Muster wie zuvor in der Listenansicht.
            .clearAndSetSemantics {
                contentDescription = listOfNotNull(title, stateLabel).joinToString(", ")
            },
        contentAlignment = Alignment.Center
    ) {
        Text(row.node.emoji, style = MaterialTheme.typography.titleMedium)
    }
}

private val NODE_SIZE = 44.dp
private val NODE_SPACING_X = 64.dp
private val NODE_SPACING_Y = 96.dp
private val BOARD_PADDING = 40.dp
private const val MIN_SCALE = 0.5f
private const val MAX_SCALE = 2.5f
