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
 * ## Was eine Freischaltung sichtbar macht
 *
 * Bis hierher war eine Freischaltung praktisch unsichtbar: Der Stern verschwand aus der Kopfzeile,
 * und die Zeile wechselte von `0xFF2A2A30` auf `0xFF1E1E22` - zwei Graustufen, die nebeneinander
 * unterscheidbar sind und untereinander in einer Liste nicht. Lag der Knoten in einem zugeklappten
 * Ast, aenderte sich gar nichts. Wer einen Punkt einsetzte, konnte danach nicht sagen, WAS er
 * bekommen hatte - und ohne einen Abnehmer, der den Freischalt-Stand liest (die Zieh-Leiste ist
 * entfernt, siehe SKILLBAUM.md), auch nicht, OB ueberhaupt etwas passiert war.
 *
 * Drei Dinge beantworten das jetzt, in dieser Reihenfolge:
 *
 * 1. **Die Reaktion laeuft sofort einmal auf der eigenen Kreatur** ([SkillReactionPreview]) und
 *    kehrt danach in den Baum zurueck. Das ist die einzige Antwort, die wirklich zeigt, was man
 *    bekommen hat - ein Name in einer Zeile zeigt es nicht.
 * 2. **Der Ast klappt sich selbst auf** ([SkillTreeRows.ancestorsOf]), samt dem neuen Knoten, falls
 *    er selbst Kinder hat. Man landet nach der Vorfuehrung dort, wo etwas passiert ist, und sieht
 *    im selben Blick, was jetzt darunter erreichbar wurde.
 * 3. **Der Knoten bleibt markiert** ("Neu", im einzigen warmen Farbton der Palette), bis der Baum
 *    geschlossen wird.
 *
 * ## Die Testumgebung darunter
 *
 * Unter dem Baum, im selben Bildlauf, laesst sich **jede** Reaktion auf **jeder** Kreatur ansehen -
 * auch die noch gesperrten. Sie schaltet nichts frei und aendert nichts; sie zeigt nur. Absichtlich
 * nicht hinter einer Entwickler-Einstellung versteckt: Der Baum verspricht 79 Knoten, und wer
 * wissen will, worauf er zulaeuft, soll nachsehen duerfen. Zugeklappt kostet sie eine Zeile.
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
    /** Die gerade gespielte Kreatur - auf ihr laeuft die Vorfuehrung nach einer Freischaltung. */
    species: AvatarSpecies,
    onUnlock: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    /** Ob der einmalige Erklaer-Hinweis steht - siehe [OnboardingPrefs.hasSeenSkillTreeHint]. */
    showHint: Boolean = false,
    onDismissHint: () -> Unit = {}
) {
    // Was in dieser Sitzung angetippt, aber von der Datenbank noch nicht zurueckgemeldet wurde.
    //
    // Der Weg von [onUnlock] bis zum naechsten Wert von [unlocked] fuehrt ueber eine Schreibung und
    // einen Flow - dazwischen liegen Millisekunden, in denen der Knoten weiter als "als Naechstes"
    // dasteht UND [LevelUnlocks.due] den schon ausgegebenen Punkt noch mitzaehlt. Ein zweiter Tipp
    // in diesem Fenster sah fuer den Bildschirm aus wie ein erster. Die Vereinigung schliesst das
    // Fenster und macht die Rueckmeldung zugleich sofort sichtbar, statt sie auf die Datenbank
    // warten zu lassen; sobald der Flow nachzieht, ist sie folgenlos, weil der Knoten dann in
    // beiden Mengen steht.
    var pending by remember { mutableStateOf(setOf<String>()) }
    val owned = remember(unlocked, pending) { unlocked + pending }

    val rows = remember(owned) { SkillTreeRows.build(owned) }
    val pointsRemaining = remember(level, owned) { LevelUnlocks.due(level, owned) }
    val branchIds = remember(rows) { rows.filter { rows.hasChildren(it.node.id) }.map { it.node.id }.toSet() }
    var expanded by remember { mutableStateOf(setOf<String>()) }
    val allOpen = branchIds.isNotEmpty() && expanded.containsAll(branchIds)

    val visible = remember(rows, expanded) { visibleRows(rows, expanded) }

    // Was in DIESER Sitzung des Dialogs freigeschaltet wurde - die "Neu"-Markierung. Bewusst nicht
    // persistiert: Sie beantwortet "was habe ich gerade getan", nicht "was besitze ich", und die
    // zweite Frage beantwortet der Zustand der Zeile ohnehin.
    var justUnlocked by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<PreviewRequest?>(null) }

    var showLab by remember { mutableStateOf(false) }
    // An [species] gebunden: Wechselt jemand die Kreatur, waehrend der Baum offen ist, soll die
    // Testumgebung nicht auf der alten stehenbleiben.
    var labSpecies by remember(species) { mutableStateOf(species) }

    val previewable = remember { SkillTreeRows.previewable() }

    // Freischalten geht vor Aufklappen: Ein Knoten kann BEIDES sein - eine Untergruppe mit
    // eigenen Kindern UND selbst gerade dran (ihr Elternknoten ist ja schon offen). Stand die
    // Aufklapp-Pruefung zuerst, liess sich ein solcher Knoten NIE freischalten - Antippen klappte
    // ihn immer nur auf, der Stern blieb liegen. Nach dem Freischalten steht der naechste Tipp auf
    // denselben Knoten wieder fuer das Aufklappen zur Verfuegung, weil sein Zustand dann UNLOCKED
    // ist und dieser Zweig hier nicht mehr greift.
    fun tap(row: SkillTreeRow) {
        if (row.state == NodeState.AVAILABLE && pointsRemaining > 0) {
            onUnlock(row.node.id)
            pending = pending + row.node.id
            justUnlocked = row.node.id
            // Den Ast oeffnen UND, falls der neue Knoten selbst eine Gruppe ist, ihn gleich mit:
            // Nach der Vorfuehrung soll sichtbar sein, was gerade darunter erreichbar geworden ist.
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
                        // Derselbe Ton wie die antippbaren Zeilen unten: Der Stern oben und die
                        // Stelle, an der er eingesetzt wird, gehoeren sichtbar zusammen.
                        color = TamaPalette.Accent
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
                items(visible.size, key = { "tree/" + visible[it].node.id }) { index ->
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

                item(key = "lab/header") {
                    LabHeader(expanded = showLab, onToggle = { showLab = !showLab })
                }
                if (showLab) {
                    item(key = "lab/species") {
                        LabSpeciesPicker(selected = labSpecies, onSelect = { labSpecies = it })
                    }
                    items(previewable.size, key = { "lab/" + previewable[it].id }) { index ->
                        val node = previewable[index]
                        LabRow(
                            node = node,
                            onClick = { preview = PreviewRequest(node.id, labSpecies) }
                        )
                    }
                }
            }
        }

        val request = preview
        // Der Knoten wird hier noch einmal nachgeschlagen statt mitgefuehrt: Ein Bezeichner, den
        // der Baum nicht kennt, zeigt dann einfach nichts an, statt die Vorfuehrung auf einen
        // erfundenen Knoten laufen zu lassen. Bewusst OHNE `preview = null` in diesem Fall -
        // waehrend der Komposition Zustand zu schreiben loest die naechste Komposition aus und
        // damit eine Schleife.
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

/**
 * Der Grund einer Zeile.
 *
 * [spendable] bekommt als einzige Lage den warmen Ton: Nicht jeder erreichbare Knoten leuchtet,
 * sondern nur der, den man in diesem Moment tatsaechlich nehmen kann. Ohne einen Stern in der Hand
 * ist "als Naechstes" eine Aussicht und keine Aufforderung - und ein Baum, in dem 20 Zeilen
 * leuchten, von denen keine antippbar ist, waere schlechter als einer ohne Farbe.
 */
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
        // Ein Haken statt des Wortes: Er steht an jeder zweiten Zeile und darf den Namen daneben
        // nicht verdraengen. Die Vorlesehilfe bekommt weiter das Wort (siehe semantics unten).
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
                // Der Rahmen markiert genau einen Knoten - den zuletzt freigeschalteten. Er
                // ueberlebt die Vorfuehrung, damit man nach der Rueckkehr in den Baum wiederfindet,
                // was man gerade bekommen hat.
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

/**
 * Eine Zeile der Testumgebung: nur Name und Motiv, kein Zustand.
 *
 * Absichtlich ohne Zustandsanzeige - hier geht es nicht darum, was jemand besitzt, sondern darum,
 * wie etwas aussieht. Ein "gesperrt" daneben wuerde eine Vorfuehrung, die ohnehin laeuft, als
 * verboten ausweisen.
 */
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
