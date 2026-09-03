package com.notime.glyphsim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.notime.glyphsim.R
import com.notime.glyphsim.matrix.AvatarAnimations
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.AvatarSpriteView
import com.notime.glyphsim.matrix.MatrixAnimator
import com.notime.glyphsim.matrix.ReactionTrigger

/**
 * Spielt die Reaktion eines Baumknotens **einmal** auf der gewaehlten Kreatur und meldet sich
 * danach ab.
 *
 * Zwei Abnehmer, ein Bauteil: die Vorfuehrung direkt nach einer Freischaltung ("was habe ich
 * gerade bekommen?") und die Testumgebung unter dem Baum ("wie sieht das bei Fennec aus?"). Beide
 * wollen dasselbe - genau einen Durchlauf, danach zurueck zum Baum -, und beide sollen dieselbe
 * Reaktion zeigen wie das Spiel selbst. Deshalb geht der Weg ueber [AvatarAnimations.reactionFor]
 * und nicht ueber eine eigene Vorschau-Choreografie: Was hier laeuft, ist nicht *aehnlich* dem,
 * was spaeter im Spiel kommt, es ist dasselbe.
 *
 * **Ohne Flugbahn** ([AvatarAnimations.flightOffsetsFor]): Die Rakete verlaesst im Spiel ihr
 * eigenes Raster und wandert ueber den Bildschirm. In einem Kaertchen mitten im Baum gaebe es
 * dafuer weder Platz noch einen Bezugsrahmen - sie spielt hier an Ort und Stelle. Das ist der
 * einzige Knoten, bei dem sich Vorschau und Spiel unterscheiden, und es betrifft die Bewegung der
 * Box, nicht die Figur darin.
 *
 * Ein Tipp auf die Flaeche bricht ab. Eine Vorfuehrung, die man nicht abkuerzen kann, ist eine
 * Wartezeit; und wer den Baum weiter erkunden will, soll nicht erst zusehen muessen.
 */
@Composable
fun SkillReactionPreview(
    species: AvatarSpecies,
    nodeId: String,
    title: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sequence = remember(species, nodeId) {
        AvatarAnimations.reactionFor(species, ReactionTrigger.Node(nodeId))
    }
    // `rememberUpdatedState`, weil der Effekt nur auf die Sequenz hoert: Ein Aufrufer, der bei
    // einer Recomposition eine neue Lambda-Instanz uebergibt, wuerde sonst den Durchlauf von vorn
    // beginnen lassen statt nur den Rueckruf auszutauschen.
    val done by rememberUpdatedState(onDone)
    var frame by remember(sequence) { mutableStateOf(sequence.frames.first()) }

    LaunchedEffect(sequence) {
        MatrixAnimator.playTimed(sequence.frames, sequence.holdsMs) { frame = it }
        done()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TamaPalette.Background)
            .clickable { done() }
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AvatarSpriteView(
            frame = frame,
            modifier = Modifier.size(160.dp),
            showBackground = false,
            contentDescription = title
        )
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = TamaPalette.Accent,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
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
