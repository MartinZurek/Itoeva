package com.notime.glyphsim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.alpha
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
import com.notime.glyphcore.data.AnimationNode
import com.notime.glyphsim.R
import kotlin.math.roundToInt

/**
 * **Die Leiste, aus der man Animationen auf den Begleiter zieht.**
 *
 * Bisher liess sich nur die Uhr auf den Avatar schieben, und auch das nur, solange eine echte
 * Erinnerung wartete - eine Geste, die man haeufig gar nicht benutzen konnte. Hier liegt alles
 * Freigeschaltete bereit und laesst sich jederzeit hinueberziehen.
 *
 * **Die Uhr behaelt ihre Rolle.** Diese Leiste kommt dazu, sie ersetzt nichts: Eine wartende
 * Erinnerung wird weiterhin beantwortet, indem man die Uhr schiebt. Wuerde die Leiste das
 * uebernehmen, waere aus dem Beantworten einer Erinnerung ein Spielzug geworden.
 *
 * **Ausgeloest wird waehrend der Geste, nicht beim Loslassen** - dieselbe Entscheidung wie bei der
 * Uhr (siehe HomeScreen): Der Moment, in dem sich die beiden beruehren, ist der Moment, in dem
 * etwas passieren soll. Wer erst beim Loslassen reagiert, laesst den Nutzer ueber der Figur
 * verharren und nichts geschehen.
 *
 * **Das Kreis-Easter-Egg kann hier nicht ausgeloest werden**, weil die Winkelverfolgung
 * ausschliesslich im Zieh-Handler der Uhr sitzt. Das ist Absicht und keine Luecke: Der Gruss
 * gehoert der Uhr.
 */
@Composable
fun SkillDragBar(
    nodes: List<AnimationNode>,
    avatarBounds: Rect,
    enabled: Boolean,
    onDrop: (AnimationNode) -> Unit,
    onOpenTree: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                stringResource(R.string.skill_bar_title),
                style = MaterialTheme.typography.labelLarge,
                color = TamaPalette.TextPrimary
            )
            // Der Weg zum Baumbildschirm. Als Text und nicht als Symbol: Wer die Leiste zum
            // ersten Mal sieht, soll lesen koennen, wohin es fuehrt.
            TextButton(onClick = onOpenTree, contentPadding = PaddingValues(0.dp)) {
                Text(
                    stringResource(R.string.skill_tree_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = TamaPalette.TextMuted
                )
            }
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            items(nodes, key = { it.id }) { node ->
                SkillChip(
                    node = node,
                    avatarBounds = avatarBounds,
                    enabled = enabled,
                    onDrop = onDrop
                )
            }
        }
    }
}

/**
 * Ein Eintrag der Leiste.
 *
 * Haelt seine eigene Ruhelage ([restingBounds]) fest und rechnet die verschobene Lage daraus - so
 * braucht die Kollisionspruefung keine zweite Messung waehrend der Geste, die ohnehin erst einen
 * Frame spaeter ankaeme.
 */
@Composable
private fun SkillChip(
    node: AnimationNode,
    avatarBounds: Rect,
    enabled: Boolean,
    onDrop: (AnimationNode) -> Unit
) {
    var drag by remember { mutableStateOf(Offset.Zero) }
    var restingBounds by remember { mutableStateOf(Rect.Zero) }
    // Verhindert, dass eine einzige Geste mehrfach ausloest, solange der Finger ueber der Figur
    // stehen bleibt - dieselbe Rolle wie feedingOccurrenceId beim Fuettern der Uhr.
    var alreadyDropped by remember { mutableStateOf(false) }

    val title = node.rememberTitle()
    val dragLabel = stringResource(R.string.a11y_skill_drag, title)
    val playLabel = stringResource(R.string.a11y_skill_play, title)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .offset { IntOffset(drag.x.roundToInt(), drag.y.roundToInt()) }
            .onGloballyPositioned { if (drag == Offset.Zero) restingBounds = it.boundsInRoot() }
            .alpha(if (enabled) 1f else 0.4f)
            .clip(RoundedCornerShape(14.dp))
            .background(TamaPalette.BubbleBackground)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .pointerInput(node.id, enabled, avatarBounds) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { alreadyDropped = false },
                    onDrag = { change, amount ->
                        change.consume()
                        drag += amount
                        if (alreadyDropped || restingBounds == Rect.Zero || avatarBounds == Rect.Zero) {
                            return@detectDragGestures
                        }
                        val moved = restingBounds.translate(drag.x, drag.y)
                        if (AvatarFeeding.overlaps(moved, avatarBounds)) {
                            alreadyDropped = true
                            onDrop(node)
                        }
                    },
                    // Immer zurueckgleiten: Der Eintrag gehoert in die Leiste, nicht dorthin, wo
                    // der Finger ihn zuletzt liegengelassen hat.
                    onDragEnd = { drag = Offset.Zero },
                    onDragCancel = { drag = Offset.Zero }
                )
            }
            /*
             * Ziehen ist reine Zeigerverarbeitung - fuer die Bedienungshilfen existiert diese
             * Geste nicht. Ohne die Zusatzaktion waere die ganze Leiste mit TalkBack oder Switch
             * Access unbenutzbar, also genau der Teil der App, der neu dazukommt. Dieselbe
             * Ueberlegung wie bei der Uhr und beim Fuettern (siehe HomeScreen).
             */
            .semantics {
                contentDescription = dragLabel
                customActions = listOf(
                    CustomAccessibilityAction(playLabel) {
                        if (enabled) onDrop(node)
                        enabled
                    }
                )
            }
    ) {
        Text(node.emoji, style = MaterialTheme.typography.titleMedium)
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            color = TamaPalette.TextPrimary
        )
    }
}
