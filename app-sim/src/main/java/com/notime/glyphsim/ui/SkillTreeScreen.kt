package com.notime.glyphsim.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.notime.glyphsim.R
import com.notime.glyphsim.skilltree.LevelUnlocks
import com.notime.glyphsim.skilltree.NodeState
import com.notime.glyphsim.skilltree.SkillTreeRow
import com.notime.glyphsim.skilltree.SkillTreeRows

/**
 * **Der Baum zum Begehen** - eine Ebene nach der anderen, statt der ganzen Flaeche auf einmal.
 *
 * Die erste Fassung zeigte alle 79 Knoten gleichzeitig auf einem pan- und zoombaren Brett - das
 * Vorbild (Diablo 2, das Sphere Grid aus Final Fantasy X) zeigt so etwas auch, aber dort fuehrt ein
 * ganzer Bildschirm zu nichts anderem hin. Hier musste man sich die Uebersicht erst erarbeiten,
 * bevor man ueberhaupt etwas antippen konnte. Diese Fassung klappt den Baum stattdessen auf:
 *
 * - **Oben** stehen nur die neun Hauptgruppen, als Kacheln mit Fortschritt.
 * - **Antippen einer Gruppe** oeffnet sie - ihre Kinder erscheinen als Knoten um einen Kopf herum,
 *   mit Verbindungslinien dazwischen. Der Rest des Baums ist in diesem Moment nicht zu sehen, weil
 *   er gerade nicht die Frage ist, die man sich stellt.
 * - **Ein Brotkrumen-Pfad** oben fuehrt zurueck, jede Stufe einzeln antippbar.
 *
 * **Freischalten ist eine Wahl, keine Automatik.** Jeder Levelaufstieg gibt einen Skillpunkt
 * ([LevelUnlocks.due]); der Spieler tippt selbst auf einen erreichbaren Nachbarknoten
 * ([NodeState.AVAILABLE]) statt ein algorithmisches Angebot vorgesetzt zu bekommen.
 *
 * Die Zuordnung Knoten -> Zustand bleibt vollstaendig in [SkillTreeRows] (siehe `SkillTreeRowsTest`)
 * - hier aendert sich nur, WIE sie gezeichnet wird.
 *
 * Tiefenunabhaengig: Ob ein Knoten Kinder hat, wird aus dem Bestand selbst gelesen
 * ([List.hasChildren]), keine Annahme "genau drei Stufen" steckt hier irgendwo im Code - eine
 * vierte Ebene liesse sich einfach in denselben Kopf-und-Kinder-Bildschirm weiter hineintippen.
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
    val byId = remember(rows) { rows.associateBy { it.node.id } }
    val pointsRemaining = remember(level, unlocked) { LevelUnlocks.due(level, unlocked) }
    var openId by remember { mutableStateOf<String?>(null) }

    fun tap(row: SkillTreeRow) {
        if (rows.hasChildren(row.node.id)) {
            openId = row.node.id
        } else if (row.state == NodeState.AVAILABLE && pointsRemaining > 0) {
            onUnlock(row.node.id)
        }
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

        SkillTreeBreadcrumb(openId = openId, byId = byId, onJump = { openId = it })

        Crossfade(
            targetState = openId,
            animationSpec = tween(BRANCH_FADE_MS),
            label = "skill-tree-level",
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { id ->
            val visible = rows.filter { it.node.parentId == id }
            val hub = id?.let(byId::get)
            if (hub == null) {
                OverviewGrid(roots = visible, unlocked = unlocked, onTap = ::tap)
            } else {
                BranchFan(
                    hub = hub,
                    children = visible,
                    rows = rows,
                    pointsRemaining = pointsRemaining,
                    onTap = ::tap
                )
            }
        }
    }
}

private fun List<SkillTreeRow>.hasChildren(id: String): Boolean = any { it.node.parentId == id }

private fun stateColor(state: NodeState): Color = when (state) {
    NodeState.UNLOCKED -> TamaPalette.BubbleBackground
    NodeState.AVAILABLE -> TamaPalette.ChoiceBackground
    NodeState.LOCKED, NodeState.PENDING_ART -> TamaPalette.Background
}

/** Der Pfad von der Uebersicht zum gerade geoeffneten Knoten, jede Station einzeln antippbar. */
@Composable
private fun SkillTreeBreadcrumb(
    openId: String?,
    byId: Map<String, SkillTreeRow>,
    onJump: (String?) -> Unit
) {
    val chain = remember(openId, byId) {
        generateSequence(openId) { byId[it]?.node?.parentId }.toList().reversed()
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            stringResource(R.string.skill_tree_overview),
            style = MaterialTheme.typography.labelMedium,
            color = if (openId == null) TamaPalette.TextPrimary else TamaPalette.TextMuted,
            modifier = Modifier.clickable { onJump(null) }.padding(vertical = 6.dp)
        )
        for (id in chain) {
            val node = byId[id]?.node ?: continue
            Text(" › ", style = MaterialTheme.typography.labelMedium, color = TamaPalette.TextMuted)
            Text(
                node.rememberTitle(),
                style = MaterialTheme.typography.labelMedium,
                color = if (id == openId) TamaPalette.TextPrimary else TamaPalette.TextMuted,
                modifier = Modifier.clickable { onJump(id) }.padding(vertical = 6.dp)
            )
        }
    }
}

/** Die oberste Ebene: die neun Hauptgruppen als Kacheln, drei je Zeile. */
@Composable
private fun OverviewGrid(
    roots: List<SkillTreeRow>,
    unlocked: Set<String>,
    onTap: (SkillTreeRow) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        for (chunk in roots.chunked(3)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                for (root in chunk) {
                    RootTile(
                        row = root,
                        progress = SkillTreeRows.progressFor(root.node.id, unlocked),
                        onClick = { onTap(root) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - chunk.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun RootTile(
    row: SkillTreeRow,
    progress: Pair<Int, Int>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = row.node.rememberTitle()
    val progressLabel = stringResource(R.string.skill_tree_progress, progress.first, progress.second)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(TamaPalette.BubbleBackground)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 4.dp)
            .clearAndSetSemantics { contentDescription = "$title, $progressLabel" }
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(TamaPalette.ChoiceBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(row.node.emoji, style = MaterialTheme.typography.headlineSmall)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = TamaPalette.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Spacer(Modifier.height(2.dp))
        Text(progressLabel, style = MaterialTheme.typography.labelSmall, color = TamaPalette.TextMuted)
    }
}

/**
 * Eine geoeffnete Gruppe: ihr Kopf oben, ihre Kinder darunter, verbunden durch Linien zu den
 * tatsaechlich gemessenen Mittelpunkten (siehe [onGloballyPositioned]/[boundsInRoot]) - so muss
 * hier keine Baumgeometrie nachgerechnet werden, das macht Compose beim Platzieren ohnehin schon.
 */
@Composable
private fun BranchFan(
    hub: SkillTreeRow,
    children: List<SkillTreeRow>,
    rows: List<SkillTreeRow>,
    pointsRemaining: Int,
    onTap: (SkillTreeRow) -> Unit
) {
    var canvasOrigin by remember(hub.node.id) { mutableStateOf(Offset.Zero) }
    var hubCenter by remember(hub.node.id) { mutableStateOf<Offset?>(null) }
    val childCenters = remember(hub.node.id) { mutableStateMapOf<String, Offset>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { canvasOrigin = it.boundsInRoot().topLeft }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val hc = hubCenter ?: return@Canvas
            for (child in children) {
                val cc = childCenters[child.node.id] ?: continue
                val begangen = hub.state == NodeState.UNLOCKED &&
                    child.state != NodeState.LOCKED && child.state != NodeState.PENDING_ART
                drawLine(
                    color = if (begangen) TamaPalette.TextPrimary else TamaPalette.TextMuted,
                    start = hc,
                    end = cc,
                    alpha = if (begangen) 0.55f else 0.2f,
                    strokeWidth = 3f
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp)
        ) {
            HubChip(
                row = hub,
                modifier = Modifier.onGloballyPositioned {
                    hubCenter = it.boundsInRoot().center - canvasOrigin
                }
            )
            Spacer(Modifier.height(30.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            ) {
                for (chunk in children.chunked(4)) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (child in chunk) {
                                val childHasChildren = rows.hasChildren(child.node.id)
                                val interactive = childHasChildren ||
                                    (child.state == NodeState.AVAILABLE && pointsRemaining > 0)
                                ChildChip(
                                    row = child,
                                    hasChildren = childHasChildren,
                                    interactive = interactive,
                                    onClick = { onTap(child) },
                                    modifier = Modifier.onGloballyPositioned {
                                        childCenters[child.node.id] = it.boundsInRoot().center - canvasOrigin
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Der Kopf der geoeffneten Gruppe - Emoji und Name nebeneinander statt als kleiner Kreis, er
 *  steht schliesslich allein da und darf sich das leisten. */
@Composable
private fun HubChip(row: SkillTreeRow, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(stateColor(row.state))
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(row.node.emoji, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.width(10.dp))
        Text(row.node.rememberTitle(), style = MaterialTheme.typography.titleMedium, color = TamaPalette.TextPrimary)
    }
}

@Composable
private fun ChildChip(
    row: SkillTreeRow,
    hasChildren: Boolean,
    interactive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = row.node.rememberTitle()
    val stateLabel = when (row.state) {
        NodeState.UNLOCKED -> null
        NodeState.AVAILABLE -> stringResource(R.string.skill_tree_state_available)
        NodeState.LOCKED -> stringResource(R.string.skill_tree_state_locked)
        NodeState.PENDING_ART -> stringResource(R.string.skill_tree_state_pending)
    }
    val dimmed = row.state == NodeState.LOCKED || row.state == NodeState.PENDING_ART

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(CHILD_COLUMN_WIDTH)) {
        Box(
            modifier = modifier
                .size(CHILD_SIZE)
                .clip(CircleShape)
                .background(stateColor(row.state))
                .let { base -> if (interactive) base.clickable(onClick = onClick) else base }
                .alpha(if (dimmed) 0.45f else 1f)
                // Ein Screenreader soll "Basketball, als Naechstes dran" lesen, keine zwei
                // getrennten Textbausteine.
                .clearAndSetSemantics {
                    contentDescription = listOfNotNull(title, stateLabel).joinToString(", ")
                },
            contentAlignment = Alignment.Center
        ) {
            Text(row.node.emoji, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            color = if (dimmed) TamaPalette.TextMuted else TamaPalette.TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        // Kein eigenes Wort dafuer - der Pfeil allein sagt "hier geht es weiter", genau wie eine
        // Verzeichnis-Kachel im Dateimanager.
        if (hasChildren) {
            Text("›", style = MaterialTheme.typography.labelSmall, color = TamaPalette.TextMuted)
        }
    }
}

private const val BRANCH_FADE_MS = 220
private val CHILD_SIZE = 48.dp
private val CHILD_COLUMN_WIDTH = 76.dp
