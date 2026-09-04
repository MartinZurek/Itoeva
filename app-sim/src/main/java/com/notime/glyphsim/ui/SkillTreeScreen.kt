package com.notime.glyphsim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.notime.glyphcore.data.AnimationNode
import com.notime.glyphcore.data.AnimationTree
import com.notime.glyphsim.R
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.skilltree.LevelUnlocks
import com.notime.glyphsim.skilltree.NodeState
import com.notime.glyphsim.skilltree.SkillTreeRow
import com.notime.glyphsim.skilltree.SkillTreeRows

/**
 * Der aufklappbare Skill-Baum.
 *
 * Freischaltungen werden sofort auf der aktuellen Kreatur vorgefuehrt. Zusaetzlich kann ein
 * Entwickler-Labor eingeblendet werden, wenn [showDeveloperLab] gesetzt ist. Das Labor ist bewusst
 * kein Spieler-Feature: Es dient dazu, jede bereits gezeichnete Skill-Reaktion auf jeder Kreatur
 * mit demselben Wiedergabepfad wie im Spiel zu kontrollieren, ohne Freischaltungen, XP oder andere
 * Spielstandsdaten zu veraendern.
 */
@Composable
fun SkillTreeScreen(
    unlocked: Set<String>,
    level: Int,
    xpToNextLevel: Int,
    /** Die gerade gespielte Kreatur - auf ihr laeuft die Vorfuehrung nach einer Freischaltung. */
    species: AvatarSpecies,
    onUnlock: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    /** Ob der einmalige Erklaer-Hinweis steht - siehe [OnboardingPrefs.hasSeenSkillTreeHint]. */
    showHint: Boolean = false,
    onDismissHint: () -> Unit = {},
    /** Nur fuer debuggable Entwickler-Builds setzen. In Release-Builds bleibt das Labor unsichtbar. */
    showDeveloperLab: Boolean = false
) {
    // Was in dieser Sitzung angetippt, aber von der Datenbank noch nicht zurueckgemeldet wurde.
    // Der lokale Bestand schliesst das kleine Zeitfenster zwischen Tap und Flow-Update.
    var pending by remember { mutableStateOf(setOf<String>()) }
    val owned = remember(unlocked, pending) { unlocked + pending }

    val rows = remember(owned) { SkillTreeRows.build(owned) }
    val pointsRemaining = remember(level, owned) { LevelUnlocks.due(level, owned) }
    val branchIds = remember(rows) { rows.filter { rows.hasChildren(it.node.id) }.map { it.node.id }.toSet() }
    var expanded by remember { mutableStateOf(setOf<String>()) }
    val allOpen = branchIds.isNotEmpty() && expanded.containsAll(branchIds)
    val visible = remember(rows, expanded) { visibleRows(rows, expanded) }

    var justUnlocked by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<PreviewRequest?>(null) }

    var showLab by remember { mutableStateOf(false) }
    // An species gebunden: Wechselt die Kreatur, soll das Labor nicht auf der alten stehenbleiben.
    var labSpecies by remember(species) { mutableStateOf(species) }
    val previewable = remember { SkillTreeRows.previewable() }

    fun tap(row: SkillTreeRow) {
        if (row.state == NodeState.AVAILABLE && pointsRemaining > 0) {
            onUnlock(row.node.id)
            pending = pending + row.node.id
            justUnlocked = row.node.id
            var opened = expanded + SkillTreeRows.ancestorsOf(row.node.id)
            if (rows.hasChildren(row.node.id)) opened = opened + row.node.id
            expanded = opened
            preview = PreviewRequest(row.node.id, species)
        } else if (rows.hasChildren(row.node.id)) {
            expanded = if (row.node.id in expanded) expanded - row.node.id else expanded + row.node.id
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                        color = TamaPalette.Accent
                    )
                }
                TextButton(onClick = onClose) { Text(stringResource(R.string.skill_tree_close)) }
            }

            if (showHint) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(TamaPalette.BubbleBackground)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        stringResource(R.string.skill_tree_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = TamaPalette.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onDismissHint) { Text(stringResource(R.string.action_ok)) }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { expanded = if (allOpen) emptySet() else branchIds }) {
                    Text(
                        stringResource(
                            if (allOpen) R.string.skill_tree_collapse_all else R.string.skill_tree_expand_all
                        )
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visible.size, key = { "tree/" + visible[it].node.id }) { index ->
                    val row = visible[index]
                    val hasChildren = rows.hasChildren(row.node.id)
                    TreeRow(
                        row = row,
                        hasChildren = hasChildren,
                        isExpanded = row.node.id in expanded,
                        hasAvailableDescendant = hasChildren && row.node.id !in expanded &&
                            hasAvailableDescendant(rows, row.node.id),
                        progress = if (hasChildren && row.state == NodeState.UNLOCKED) {
                            SkillTreeRows.progressFor(row.node.id, owned)
                        } else {
                            null
                        },
                        spendable = row.state == NodeState.AVAILABLE && pointsRemaining > 0,
                        isNew = row.node.id == justUnlocked,
                        interactive = hasChildren || (row.state == NodeState.AVAILABLE && pointsRemaining > 0),
                        onClick = { tap(row) }
                    )
                }

                // Das Labor ist absichtlich nur ein Einstiegspunkt. Frueher wurden seine Zeilen
                // unterhalb dieses bereits am Listenende stehenden Eintrags eingefuegt; auf kleinen
                // Displays sah ein Tap deshalb aus, als passiere nichts. Jetzt oeffnet der Tap eine
                // eigene, sofort sichtbare Entwickleransicht ueber dem Baum.
                if (showDeveloperLab) {
                    item(key = "lab/header") {
                        LabHeader(expanded = false, onToggle = { showLab = true })
                    }
                }
            }
        }

        if (showDeveloperLab && showLab) {
            DeveloperSkillLab(
                selectedSpecies = labSpecies,
                nodes = previewable,
                onSelectSpecies = { labSpecies = it },
                onPreview = { node -> preview = PreviewRequest(node.id, labSpecies) },
                onClose = { showLab = false },
                modifier = Modifier.fillMaxSize()
            )
        }

        val request = preview
        val node = request?.let { AnimationTree.node(it.nodeId) }
        if (request != null && node != null) {
            SkillReactionPreview(
                species = request.species,
                nodeId = node.id,
                title = node.rememberTitle(),
                onDone = { preview = null },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Welche Reaktion gerade auf welcher Kreatur vorgefuehrt wird - siehe [SkillReactionPreview]. */
private data class PreviewRequest(val nodeId: String, val species: AvatarSpecies)

private fun visibleRows(rows: List<SkillTreeRow>, expanded: Set<String>): List<SkillTreeRow> {
    val out = mutableListOf<SkillTreeRow>()
    fun walk(parentId: String?) {
        for (row in rows) {
            if (row.node.parentId != parentId) continue
            out += row
            if (row.node.id in expanded) walk(row.node.id)
        }
    }
    walk(null)
    return out
}

private fun List<SkillTreeRow>.hasChildren(id: String): Boolean = any { it.node.parentId == id }

private fun hasAvailableDescendant(rows: List<SkillTreeRow>, id: String): Boolean =
    rows.filter { it.node.parentId == id }.any { child ->
        child.state == NodeState.AVAILABLE || hasAvailableDescendant(rows, child.node.id)
    }

private fun stateColor(state: NodeState, spendable: Boolean): Color = when {
    spendable -> TamaPalette.AccentBackground
    state == NodeState.UNLOCKED -> TamaPalette.BubbleBackground
    state == NodeState.AVAILABLE -> TamaPalette.ChoiceBackground
    else -> TamaPalette.Background
}

@Composable
private fun TreeRow(
    row: SkillTreeRow,
    hasChildren: Boolean,
    isExpanded: Boolean,
    hasAvailableDescendant: Boolean,
    progress: Pair<Int, Int>?,
    spendable: Boolean,
    isNew: Boolean,
    interactive: Boolean,
    onClick: () -> Unit
) {
    val title = row.node.rememberTitle()
    val unlockedLabel = stringResource(R.string.skill_tree_state_unlocked)
    val stateLabel = when (row.state) {
        NodeState.UNLOCKED -> "✓"
        NodeState.AVAILABLE ->
            if (spendable) stringResource(R.string.skill_tree_spend_star)
            else stringResource(R.string.skill_tree_state_available)
        NodeState.LOCKED -> stringResource(R.string.skill_tree_state_locked)
        NodeState.PENDING_ART -> stringResource(R.string.skill_tree_state_pending)
    }
    val progressLabel = progress?.let { stringResource(R.string.skill_tree_progress, it.first, it.second) }
    val dimmed = row.state == NodeState.LOCKED || row.state == NodeState.PENDING_ART
    val trailingLabel = progressLabel ?: stateLabel
    val newLabel = stringResource(R.string.skill_tree_new)
    val spokenState = if (row.state == NodeState.UNLOCKED) unlockedLabel else stateLabel

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = INDENT_STEP * (row.node.depth - 1))
            .clip(RoundedCornerShape(14.dp))
            .background(stateColor(row.state, spendable))
            .let { base ->
                if (isNew) base.border(1.dp, TamaPalette.Accent, RoundedCornerShape(14.dp)) else base
            }
            .let { base -> if (interactive) base.clickable(onClick = onClick) else base }
            .alpha(if (dimmed) 0.5f else 1f)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clearAndSetSemantics {
                contentDescription = listOfNotNull(
                    title,
                    if (isNew) newLabel else null,
                    progressLabel,
                    spokenState
                ).joinToString(", ")
            }
    ) {
        Box(modifier = Modifier.width(18.dp), contentAlignment = Alignment.Center) {
            if (hasChildren) {
                Text(
                    if (isExpanded) "▾" else "▸",
                    style = MaterialTheme.typography.labelMedium,
                    color = TamaPalette.TextMuted
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(TamaPalette.ChoiceBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(row.node.emoji, style = MaterialTheme.typography.bodyMedium)
        }
        if (hasAvailableDescendant) {
            Spacer(Modifier.width(4.dp))
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(TamaPalette.Accent))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (dimmed) TamaPalette.TextMuted else TamaPalette.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (isNew) {
            Text(
                newLabel,
                style = MaterialTheme.typography.labelSmall,
                color = TamaPalette.Accent,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        Text(
            trailingLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (spendable) TamaPalette.Accent else TamaPalette.TextMuted
        )
    }
}

@Composable
private fun LabHeader(expanded: Boolean, onToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(TamaPalette.RowBackground)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (expanded) "▾" else "▸",
                style = MaterialTheme.typography.labelMedium,
                color = TamaPalette.TextMuted,
                modifier = Modifier.width(18.dp)
            )
            Text(
                stringResource(R.string.skill_tree_lab_title),
                style = MaterialTheme.typography.bodyMedium,
                color = TamaPalette.TextPrimary
            )
        }
        Text(
            stringResource(R.string.skill_tree_lab_hint),
            style = MaterialTheme.typography.labelSmall,
            color = TamaPalette.TextMuted,
            modifier = Modifier.padding(start = 18.dp, top = 4.dp)
        )
    }
}

/** Eigene Entwickleransicht, damit das Oeffnen sofort sichtbar ist und nicht unter den Viewport faellt. */
@Composable
private fun DeveloperSkillLab(
    selectedSpecies: AvatarSpecies,
    nodes: List<AnimationNode>,
    onSelectSpecies: (AvatarSpecies) -> Unit,
    onPreview: (AnimationNode) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(TamaPalette.Background)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.skill_tree_lab_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = TamaPalette.TextPrimary
                )
                Text(
                    stringResource(R.string.skill_tree_lab_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = TamaPalette.TextMuted
                )
            }
            TextButton(onClick = onClose) { Text(stringResource(R.string.skill_tree_close)) }
        }

        Spacer(Modifier.size(8.dp))
        LabSpeciesPicker(selected = selectedSpecies, onSelect = onSelectSpecies)
        Spacer(Modifier.size(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(nodes.size, key = { "developer-lab/" + nodes[it].id }) { index ->
                val node = nodes[index]
                LabRow(node = node, onClick = { onPreview(node) })
            }
        }
    }
}

@Composable
private fun LabSpeciesPicker(selected: AvatarSpecies, onSelect: (AvatarSpecies) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (candidate in AvatarSpecies.entries) {
            val isSelected = candidate == selected
            Text(
                stringResource(candidate.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) TamaPalette.Accent else TamaPalette.TextMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) TamaPalette.AccentBackground else TamaPalette.BubbleBackground
                    )
                    .clickable { onSelect(candidate) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

/** Eine Zeile der Testumgebung: nur Name und Motiv, kein Freischalt-Zustand. */
@Composable
private fun LabRow(node: AnimationNode, onClick: () -> Unit) {
    val title = node.rememberTitle()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = INDENT_STEP * (node.depth - 1))
            .clip(RoundedCornerShape(14.dp))
            .background(TamaPalette.BubbleBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clearAndSetSemantics { contentDescription = title }
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(TamaPalette.ChoiceBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(node.emoji, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = TamaPalette.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text("▶", style = MaterialTheme.typography.labelSmall, color = TamaPalette.TextMuted)
    }
}

private val INDENT_STEP: Dp = 18.dp
