package com.notime.glyphsim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.notime.glyphsim.R
import com.notime.glyphsim.matrix.AvatarAnimations
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.AvatarSpriteView
import com.notime.glyphsim.matrix.ReactionTrigger
import kotlin.math.roundToInt

/**
 * Spielt die Reaktion eines Baumknotens einmal auf der gewaehlten Kreatur und meldet sich danach
 * ab. Die Vorschau benutzt absichtlich denselben Wiedergabepfad wie das echte Auf-den-Avatar-
 * Schieben im Spiel: [AvatarFeeding.playReaction]. Damit gelten dieselben individuellen Frame-
 * Timings und dieselben Bewegungs-Offsets, einschliesslich der Rocket-Flugbahn.
 *
 * Der einzige Unterschied zum Spiel ist die Umgebung: Hier wird weder ein Feed-Event gespeichert
 * noch XP vergeben. Das Labor ist damit ein sicherer Renderer-Test und veraendert keinen Spielstand.
 * Ein Tipp auf die Flaeche bricht die Vorfuehrung ab.
 */
@Composable
fun SkillReactionPreview(
    species: AvatarSpecies,
    nodeId: String,
    title: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trigger = remember(nodeId) { ReactionTrigger.Node(nodeId) }
    val sequence = remember(species, trigger) { AvatarAnimations.reactionFor(species, trigger) }
    val done by rememberUpdatedState(onDone)

    // Auch eine versehentlich leere Reaktion darf das Entwickler-Labor nicht abschiessen. Der
    // Wiedergabepfad protokolliert diesen Fall ebenfalls nur und kehrt sauber zurueck.
    var frame by remember(sequence) { mutableStateOf(sequence.frames.firstOrNull() ?: IntArray(17 * 17)) }
    var reactionOffset by remember(species, trigger) { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(TamaPalette.Background)
            .clickable { done() }
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        LaunchedEffect(species, trigger, widthPx, heightPx) {
            reactionOffset = Offset.Zero
            AvatarFeeding.playReaction(
                species = species,
                trigger = trigger,
                screenWidthPx = widthPx,
                screenHeightPx = heightPx,
                onFrame = { frame = it },
                onOffset = { reactionOffset = it }
            )
            done()
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            AvatarSpriteView(
                frame = frame,
                modifier = Modifier
                    .offset {
                        IntOffset(
                            reactionOffset.x.roundToInt(),
                            reactionOffset.y.roundToInt()
                        )
                    }
                    .size(160.dp),
                showBackground = false,
                contentDescription = title
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = TamaPalette.Accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                stringResource(R.string.skill_tree_preview_skip),
                style = MaterialTheme.typography.labelSmall,
                color = TamaPalette.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}
