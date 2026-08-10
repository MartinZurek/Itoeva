package com.notime.glyphsim.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notime.glyphsim.R
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.matrix.Memory
import com.notime.glyphsim.matrix.TopicFirst
import com.notime.glyphsim.matrix.buildMemories
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * **Woran ich mich erinnere** - die gemeinsame Geschichte als Liste von ersten Malen.
 *
 * Bewusst als Abschnitt im Gespraech mit dem Wesen und nicht als eigener Reiter: Es ist SEINE
 * Erinnerung, keine Auswertung ueber den Nutzer. Wer sie sehen will, fragt danach, so wie er auch
 * nach dem heutigen Tag fragt.
 *
 * Die Begruendung fuer den Inhalt steht bei [buildMemories]; hier geht es nur noch darum, ihn
 * ruhig darzustellen. Insbesondere: **keine Zahl auf diesem Bildschirm** ausser dem Datum.
 */
@Composable
fun AssistantMemories(species: com.notime.glyphsim.matrix.AvatarSpecies, onBack: () -> Unit) {
    val context = LocalContext.current
    val companionId = remember(species) { AvatarSpeciesPrefs.profileId(species) }
    var memories by remember(companionId) { mutableStateOf<List<Memory>?>(null) }

    LaunchedEffect(companionId) {
        memories = withContext(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(context).avatarFeedEventDao()
            buildMemories(
                firstSharedMomentMillis = dao.firstAnsweredMillis(companionId),
                firstPerTopic = dao.firstAnsweredPerTopic(companionId).map {
                    TopicFirst(it.animationType, it.libraryAnimationLabel, it.firstEpochMillis)
                },
                nowMillis = System.currentTimeMillis()
            )
        }
    }

    when (val list = memories) {
        null -> Bubble(stringResource(R.string.talk_loading))
        // Kein "du hast noch nichts geschafft": Es gibt schlicht noch nichts zu erzaehlen, und
        // das kommt von selbst.
        emptyList<Memory>() -> Bubble(stringResource(R.string.memories_empty))
        else -> {
            Bubble(stringResource(R.string.memories_intro))
            list.forEach { MemoryRow(it) }
        }
    }
    Choice(stringResource(R.string.action_back), onBack)
}

/**
 * Eine Zeile: Datum links, was passiert ist rechts.
 *
 * Das Datum in der Landessprache ueber [DateFormat.MEDIUM] statt selbst zusammengesetzt - eine
 * Erinnerung ohne Jahr waere nach einem Jahreswechsel irrefuehrend, und die Reihenfolge von Tag
 * und Monat ist je Sprache verschieden.
 */
@Composable
private fun MemoryRow(memory: Memory) {
    val datum = remember(memory.epochMillis) {
        DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(memory.epochMillis))
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val visuals = memory.topic?.let { animationVisuals[it] }
        Text(
            text = visuals?.emoji ?: memory.kind.emoji(),
            fontSize = 18.sp,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = memory.describe(),
                style = MaterialTheme.typography.bodyMedium,
                color = DialogPalette.TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = datum,
                style = MaterialTheme.typography.bodySmall,
                color = DialogPalette.TextMuted
            )
        }
    }
}

/** Ein Sinnbild fuer die Eintraege ohne eigenes Thema. */
private fun Memory.Kind.emoji(): String = when (this) {
    Memory.Kind.MET -> "✨"
    Memory.Kind.CHAPTER -> "📖"
    Memory.Kind.FIRST_TOPIC -> "⭐"
}

@Composable
private fun Memory.describe(): String = when (kind) {
    Memory.Kind.MET -> stringResource(R.string.memories_met)
    Memory.Kind.CHAPTER -> stringResource(
        R.string.memories_chapter,
        stringResource(chapter?.labelRes ?: R.string.chapter_arrived)
    )
    Memory.Kind.FIRST_TOPIC -> stringResource(
        R.string.memories_first_topic,
        libraryLabel ?: topic?.let { stringResource(it.labelRes) }.orEmpty()
    )
}
