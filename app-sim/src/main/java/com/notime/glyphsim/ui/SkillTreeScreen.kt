package com.notime.glyphsim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.notime.glyphcore.data.AnimationNode
import com.notime.glyphcore.data.AnimationTree
import com.notime.glyphsim.R
import com.notime.glyphsim.skilltree.NodeState
import com.notime.glyphsim.skilltree.SkillTreeRow
import com.notime.glyphsim.skilltree.SkillTreeRows

/**
 * **Der Baum zum Ansehen** - was ist offen, was kommt als Naechstes, was liegt dahinter.
 *
 * Bewusst nicht bedienbar: Freigeschaltet wird beim Levelaufstieg, nicht hier. Ein Bildschirm, auf
 * dem man tippen kann, verspricht sonst eine Wahl, die es an dieser Stelle gar nicht gibt.
 *
 * Die Zuordnung Knoten → Zustand liegt in [SkillTreeRows] und nicht hier - dadurch laesst sie sich
 * pruefen, ohne einen Bildschirm zu starten (siehe `SkillTreeRowsTest`). Hier bleibt nur das
 * Zeichnen.
 */
@Composable
fun SkillTreeScreen(
    unlocked: Set<String>,
    level: Int,
    xpToNextLevel: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val roots = remember { AnimationTree.roots() }

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
            TextButton(onClick = onClose) { Text(stringResource(R.string.skill_tree_close)) }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (root in roots) {
                val rows = SkillTreeRows.forRoot(root.id, unlocked)
                val (offen, gesamt) = SkillTreeRows.progressFor(root.id, unlocked)

                item(key = "head-${root.id}") {
                    GroupHeader(root = root, open = offen, total = gesamt)
                }
                items2(rows.drop(1)) { row -> NodeRow(row) }
            }
        }
    }
}

/** Kleiner Helfer statt `items(...)`, damit die Schluessel eindeutig bleiben. */
private inline fun androidx.compose.foundation.lazy.LazyListScope.items2(
    rows: List<SkillTreeRow>,
    crossinline content: @Composable (SkillTreeRow) -> Unit
) = items(rows.size, key = { rows[it].node.id }) { content(rows[it]) }

@Composable
private fun GroupHeader(root: AnimationNode, open: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(root.emoji, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        Text(
            root.rememberTitle(),
            style = MaterialTheme.typography.titleSmall,
            color = TamaPalette.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            stringResource(R.string.skill_tree_progress, open, total),
            style = MaterialTheme.typography.labelSmall,
            color = TamaPalette.TextMuted
        )
    }
}

@Composable
private fun NodeRow(row: SkillTreeRow) {
    val title = row.node.rememberTitle()
    val stateLabel = when (row.state) {
        NodeState.UNLOCKED -> null
        NodeState.AVAILABLE -> stringResource(R.string.skill_tree_state_available)
        NodeState.LOCKED -> stringResource(R.string.skill_tree_state_locked)
        NodeState.PENDING_ART -> stringResource(R.string.skill_tree_state_pending)
    }
    // Offene Knoten stehen voll da, alles andere tritt zurueck - der Unterschied soll sich beim
    // Ueberfliegen zeigen, nicht erst beim Lesen der Beschriftung.
    val dimmed = row.state != NodeState.UNLOCKED

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            // Stufe 3 rueckt ein: Die Einrueckung IST die Baumstruktur, ohne sie waere es eine Liste.
            .padding(start = if (row.node.depth == 3) 26.dp else 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (dimmed) TamaPalette.Background else TamaPalette.BubbleBackground)
            .padding(horizontal = 10.dp, vertical = 7.dp)
            // Ein Screenreader soll "Basketball, gesperrt" lesen und nicht zwei Textbausteine
            // nacheinander, zwischen denen der Zusammenhang verlorengeht.
            .clearAndSetSemantics {
                contentDescription = listOfNotNull(title, stateLabel).joinToString(", ")
            }
    ) {
        Box(modifier = Modifier.width(26.dp), contentAlignment = Alignment.Center) {
            Text(
                row.node.emoji,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.alpha(if (dimmed) 0.45f else 1f)
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (dimmed) TamaPalette.TextMuted else TamaPalette.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        if (stateLabel != null) {
            Text(
                stateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = TamaPalette.TextMuted,
                textAlign = TextAlign.End
            )
        }
    }
    Spacer(Modifier.height(2.dp))
}
