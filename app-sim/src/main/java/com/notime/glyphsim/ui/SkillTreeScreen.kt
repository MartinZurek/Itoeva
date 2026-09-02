package com.notime.glyphsim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.notime.glyphsim.R
import com.notime.glyphsim.skilltree.LevelUnlocks
import com.notime.glyphsim.skilltree.NodeState
import com.notime.glyphsim.skilltree.SkillTreeRow
import com.notime.glyphsim.skilltree.SkillTreeRows

/**
 * **Der Baum zum Aufklappen** - jeder Ast oeffnet sich an Ort und Stelle, statt den Blick woanders
 * hinzuschicken. Wer will, klappt alles auf und sieht den ganzen Baum auf einmal; wer nicht, laesst
 * das meiste zu.
 *
 * **Warum nicht die vorige Fassung (ein Kopf, seine Kinder, der Rest verschwindet).** Genau das
 * fuehlte sich beim Bedienen falsch an: Antippen einer Gruppe sprang zu ihr hin und nahm dabei die
 * Uebersicht ueber alles andere weg. Hier bleibt jeder Ast, den man geoeffnet hat, offen stehen -
 * mehrere gleichzeitig, bis hin zum ganzen Baum.
 *
 * - **Jede Zeile mit Kindern** traegt einen Pfeil (▸/▾) und klappt beim Antippen ihre Kinder direkt
 *   darunter auf oder wieder zu - eingerueckt nach Tiefe, keine Ebene versteckt sich woanders.
 * - **"Alles aufklappen"** oben zeigt in einem Schritt den kompletten Baum; derselbe Knopf klappt
 *   danach wieder alles zu.
 * - **Ein Punkt** an einer zugeklappten Zeile heisst: darunter liegt etwas, das gerade freischaltbar
 *   waere - man muss nicht erst hineinklappen, um das zu wissen.
 *
 * **Freischalten ist eine Wahl, keine Automatik.** Jeder Levelaufstieg gibt einen Skillpunkt
 * ([LevelUnlocks.due]); der Spieler tippt selbst auf einen erreichbaren Nachbarknoten
 * ([NodeState.AVAILABLE]) statt ein algorithmisches Angebot vorgesetzt zu bekommen.
 *
 * Die Zuordnung Knoten -> Zustand bleibt vollstaendig in [SkillTreeRows] (siehe `SkillTreeRowsTest`)
 * - hier aendert sich nur, WIE sie gezeichnet wird. Tiefenunabhaengig: Ob ein Knoten Kinder hat,
 * wird aus dem Bestand selbst gelesen ([List.hasChildren]), die Einrueckung folgt
 * [com.notime.glyphcore.data.AnimationNode.depth] direkt - eine vierte Ebene braeuchte hier keine
 * Codeaenderung.
 */
@Composable
fun SkillTreeScreen(
    unlocked: Set<String>,
    level: Int,
    xpToNextLevel: Int,
    onUnlock: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    /** Ob der einmalige Erklaer-Hinweis steht - siehe [OnboardingPrefs.hasSeenSkillTreeHint]. */
    showHint: Boolean = false,
    onDismissHint: () -> Unit = {}
) {
    val rows = remember(unlocked) { SkillTreeRows.build(unlocked) }
    val pointsRemaining = remember(level, unlocked) { LevelUnlocks.due(level, unlocked) }
    val branchIds = remember(rows) { rows.filter { rows.hasChildren(it.node.id) }.map { it.node.id }.toSet() }
    var expanded by remember { mutableStateOf(setOf<String>()) }
    val allOpen = branchIds.isNotEmpty() && expanded.containsAll(branchIds)

    val visible = remember(rows, expanded) { visibleRows(rows, expanded) }

    // Freischalten geht vor Aufklappen: Ein Knoten kann BEIDES sein - eine Untergruppe mit
    // eigenen Kindern UND selbst gerade dran (ihr Elternknoten ist ja schon offen). Stand die
    // Aufklapp-Pruefung zuerst, liess sich ein solcher Knoten NIE freischalten - Antippen klappte
    // ihn immer nur auf, der Stern blieb liegen. Nach dem Freischalten steht der naechste Tipp auf
    // denselben Knoten wieder fuer das Aufklappen zur Verfuegung, weil sein Zustand dann UNLOCKED
    // ist und dieser Zweig hier nicht mehr greift.
    fun tap(row: SkillTreeRow) {
        if (row.state == NodeState.AVAILABLE && pointsRemaining > 0) {
            onUnlock(row.node.id)
        } else if (rows.hasChildren(row.node.id)) {
            expanded = if (row.node.id in expanded) expanded - row.node.id else expanded + row.node.id
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

        // Einmaliger Erklaer-Hinweis: "wie setze ich einen Stern ein" sieht man der Oberflaeche
        // sonst nicht an, anders als bei Avatar oder Uhr gibt es hier keine Geste, auf die man
        // von selbst kommt. Verschwindet dauerhaft nach dem ersten "OK" (siehe OnboardingPrefs).
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
            items(visible.size, key = { visible[it].node.id }) { index ->
                val row = visible[index]
                val hasChildren = rows.hasChildren(row.node.id)
                TreeRow(
                    row = row,
                    hasChildren = hasChildren,
                    isExpanded = row.node.id in expanded,
                    hasAvailableDescendant = hasChildren && row.node.id !in expanded &&
                        hasAvailableDescendant(rows, row.node.id),
                    // Nur fuer bereits FREIGESCHALTETE Zweige: Ein Knoten, der selbst erst noch
                    // freizuschalten waere, hat gar keine freigeschalteten Kinder - "0 von 3
                    // offen" stuende dort immer und verdeckte genau das "als Naechstes", das zum
                    // Tippen einlaedt.
                    progress = if (hasChildren && row.state == NodeState.UNLOCKED) {
                        SkillTreeRows.progressFor(row.node.id, unlocked)
                    } else {
                        null
                    },
                    interactive = hasChildren || (row.state == NodeState.AVAILABLE && pointsRemaining > 0),
                    onClick = { tap(row) }
                )
            }
        }
    }
}

/** Alle sichtbaren Zeilen in Baumreihenfolge: eine Wurzel, direkt gefolgt von ihren Kindern, falls
 *  sie in [expanded] steht - Tiefensuche, aber flach ausgegeben, damit eine simple `LazyColumn`
 *  reicht statt einer eigenen verschachtelten Compose-Struktur. */
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

/** Ob irgendwo unter [id] ein Knoten steckt, der jetzt freischaltbar waere - fuer den Punkt an
 *  einer zugeklappten Zeile, damit man nicht erst hineinklappen muss, um das zu sehen. */
private fun hasAvailableDescendant(rows: List<SkillTreeRow>, id: String): Boolean =
    rows.filter { it.node.parentId == id }.any { child ->
        child.state == NodeState.AVAILABLE || hasAvailableDescendant(rows, child.node.id)
    }

private fun stateColor(state: NodeState): Color = when (state) {
    NodeState.UNLOCKED -> TamaPalette.BubbleBackground
    NodeState.AVAILABLE -> TamaPalette.ChoiceBackground
    NodeState.LOCKED, NodeState.PENDING_ART -> TamaPalette.Background
}

@Composable
private fun TreeRow(
    row: SkillTreeRow,
    hasChildren: Boolean,
    isExpanded: Boolean,
    hasAvailableDescendant: Boolean,
    progress: Pair<Int, Int>?,
    interactive: Boolean,
    onClick: () -> Unit
) {
    val title = row.node.rememberTitle()
    val stateLabel = when (row.state) {
        NodeState.UNLOCKED -> null
        NodeState.AVAILABLE -> stringResource(R.string.skill_tree_state_available)
        NodeState.LOCKED -> stringResource(R.string.skill_tree_state_locked)
        NodeState.PENDING_ART -> stringResource(R.string.skill_tree_state_pending)
    }
    val progressLabel = progress?.let { stringResource(R.string.skill_tree_progress, it.first, it.second) }
    val dimmed = row.state == NodeState.LOCKED || row.state == NodeState.PENDING_ART
    val trailingLabel = progressLabel ?: stateLabel

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = INDENT_STEP * (row.node.depth - 1))
            .clip(RoundedCornerShape(14.dp))
            .background(stateColor(row.state))
            .let { base -> if (interactive) base.clickable(onClick = onClick) else base }
            .alpha(if (dimmed) 0.5f else 1f)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clearAndSetSemantics {
                contentDescription = listOfNotNull(title, trailingLabel).joinToString(", ")
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
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(TamaPalette.TextPrimary))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (dimmed) TamaPalette.TextMuted else TamaPalette.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (trailingLabel != null) {
            Text(trailingLabel, style = MaterialTheme.typography.labelSmall, color = TamaPalette.TextMuted)
        }
    }
}

private val INDENT_STEP: Dp = 18.dp
