package com.notime.glyphsim.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.notime.glyphsim.R
import com.notime.glyphsim.matrix.MusicCatalog
import com.notime.glyphsim.matrix.MusicRole

/**
 * **Die Musik-Bibliothek** - ein eigener Testbildschirm, damit sich ein einzelnes Stueck beim
 * Titel nennen laesst statt auf einen internen Ressourcennamen zu zeigen.
 *
 * Gemeldet: Rueckmeldung zu einem bestimmten Track war nur moeglich, wenn man wusste, wie er
 * heisst - und das weiss ausserhalb dieser Datei niemand, nicht einmal die App selbst zeigt es
 * irgendwo an. [MusicCatalog] traegt seitdem die Titel, dieser Bildschirm listet sie samt
 * Rolle, Anlass ("wann laeuft es") und bei Charakterthemen der zugehoerigen Spezies auf und
 * spielt auf Antippen genau DIESES eine Stueck ab.
 *
 * **Bewusst getrennt von [PlayMusic].** Ein eigener, schlichter [MediaPlayer] statt eines
 * Aufrufs von `PlayMusic.apply`: Die Weltlogik waehlt Rolle und Variante selbst, kennt die
 * Einstellung "Musik im Spielmodus" und blendet ueber, waehrend hier die Nutzerin GENAU EINEN
 * bestimmten Track antippt, unabhaengig davon, was gerade zur Tageszeit passen wuerde und ob der
 * Schalter ueberhaupt an ist. Beide Systeme sollen sich nicht gegenseitig unterbrechen koennen -
 * deshalb greift dieser Bildschirm nie auf `PlayMusic`s eigenen Player zu.
 *
 * Loop absichtlich AN, genau wie in der Welt (`MediaPlayer.isLooping = true`): Ein spuerbarer
 * Ruckler an der Schleifennaht ist selbst eine Qualitaetsfrage, die sich nur im Loop pruefen
 * laesst.
 */
@Composable
fun MusicLibraryDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        MusicLibraryScreen(onBack = onDismiss)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicLibraryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // Welche (Rolle, Variante) gerade ueber DIESEN Bildschirm laeuft - null heisst "keine".
    var playing by remember { mutableStateOf<Pair<MusicRole, Int>?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    fun stopPreview() {
        player?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        player = null
        playing = null
    }

    fun playPreview(role: MusicRole, variant: Int, resId: Int) {
        stopPreview()
        runCatching {
            val next = MediaPlayer.create(context, resId) ?: return
            next.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            next.isLooping = true
            next.start()
            player = next
            playing = role to variant
        }
    }

    // Verlassen des Bildschirms ist ein ebenso hartes Stopp-Signal wie das Verlassen des
    // Spielmodus bei PlayMusic - niemand soll eine Vorschau ungewollt im Hintergrund weiterlaufen
    // haben.
    DisposableEffect(Unit) {
        onDispose { stopPreview() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.music_library_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(MusicCatalog.DISPLAY_ORDER, key = { it.name }) { role ->
                RoleSection(
                    role = role,
                    playingVariant = playing?.takeIf { it.first == role }?.second,
                    onPlay = { variant, resId -> playPreview(role, variant, resId) },
                    onStop = ::stopPreview
                )
            }
        }
    }
}

@Composable
private fun RoleSection(
    role: MusicRole,
    playingVariant: Int?,
    onPlay: (Int, Int) -> Unit,
    onStop: () -> Unit
) {
    val context = LocalContext.current
    val variants = remember(role) { PlayMusic.availableVariants(context, role) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(MusicCatalog.roleDescriptionRes(role)),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        if (variants.isEmpty()) {
            Text(
                stringResource(R.string.music_library_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            variants.forEach { variant ->
                val resId = PlayMusic.trackResId(context, role, variant)
                if (resId != null) {
                    TrackRow(
                        role = role,
                        variant = variant,
                        resId = resId,
                        isPlaying = playingVariant == variant,
                        onPlay = { onPlay(variant, resId) },
                        onStop = onStop
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackRow(
    role: MusicRole,
    variant: Int,
    resId: Int,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit
) {
    val titleRes = MusicCatalog.titleRes(role, variant)
    val title = if (titleRes != null) stringResource(titleRes) else role.variantResource(variant)
    val species = MusicCatalog.speciesFor(role, variant)
    val playDescription = stringResource(R.string.a11y_music_play_track, title)
    val stopDescription = stringResource(R.string.a11y_music_stop_track, title)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (species != null) {
                    Text(
                        stringResource(species.labelRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isPlaying) {
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.semantics { contentDescription = stopDescription }
                ) {
                    // Kein "Stop"-Symbol in material-icons-core (siehe HomeScreen.kt fuer
                    // dieselbe Grenze) - ein gefuelltes Quadrat als Text ist hier die vorhandene
                    // Ausweichloesung statt des groesseren -extended-Artefakts.
                    Text(
                        "■",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                IconButton(
                    onClick = onPlay,
                    modifier = Modifier.semantics { contentDescription = playDescription }
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                }
            }
        }
    }
}
