package com.notime.glyphsim.ui

import android.content.Context
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.notime.glyphsim.matrix.MusicLoudness
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
 *
 * **A/B-Vergleich (nur Hoertest-Paket, 26.09.):** Liegt neben einem Stueck eine Datei
 * `<name>_alt.ogg`, zeigt die Zeile zwei Knoepfe - A spielt die bisherige Fassung, B die neue.
 * Gewuenscht, weil nach einer Neuerzeugung sonst nicht zu erkennen war, welche Fassung man
 * gerade hoert. Die Welt selbst findet `_alt` nie: [PlayMusic] sucht genau `itoeva_<rolle>_NN`.
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
    // Welche (Rolle, Variante, Fassung) gerade ueber DIESEN Bildschirm laeuft - null heisst "keine".
    var playing by remember { mutableStateOf<Take?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }

    fun stopPreview() {
        player?.let {
            runCatching { it.stop() }
            runCatching { it.release() }
        }
        player = null
        playing = null
    }

    fun playPreview(take: Take, resId: Int) {
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
            // Derselbe Lautheitsausgleich wie im Spiel (siehe MusicLoudness): Beim Vergleichen
            // soll kein Stueck nur deshalb besser klingen, weil es lauter erzeugt wurde.
            val lautstaerke =
                (PREVIEW_VOLUME * MusicLoudness.gainFor(take.resourceName)).coerceIn(0f, 1f)
            next.setVolume(lautstaerke, lautstaerke)
            next.start()
            player = next
            playing = take
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
                    playing = playing?.takeIf { it.role == role },
                    onPlay = ::playPreview,
                    onStop = ::stopPreview
                )
            }
        }
    }
}

@Composable
private fun RoleSection(
    role: MusicRole,
    playing: Take?,
    onPlay: (Take, Int) -> Unit,
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
                        altResId = alternateResId(context, role, variant),
                        playing = playing?.takeIf { it.variant == variant },
                        onPlay = onPlay,
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
    altResId: Int?,
    playing: Take?,
    onPlay: (Take, Int) -> Unit,
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
            if (altResId == null) {
                PlayStopButton(
                    isPlaying = playing != null,
                    playDescription = playDescription,
                    stopDescription = stopDescription,
                    onPlay = { onPlay(Take(role, variant, alt = false), resId) },
                    onStop = onStop
                )
            }
        }
        if (altResId != null) {
            Column(modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                Text(
                    stringResource(R.string.music_library_take_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TakeButton(
                        label = stringResource(R.string.music_library_take_old),
                        title = title,
                        isPlaying = playing?.alt == true,
                        onPlay = { onPlay(Take(role, variant, alt = true), altResId) },
                        onStop = onStop,
                        modifier = Modifier.weight(1f)
                    )
                    TakeButton(
                        label = stringResource(R.string.music_library_take_new),
                        title = title,
                        isPlaying = playing?.alt == false,
                        onPlay = { onPlay(Take(role, variant, alt = false), resId) },
                        onStop = onStop,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayStopButton(
    isPlaying: Boolean,
    playDescription: String,
    stopDescription: String,
    onPlay: () -> Unit,
    onStop: () -> Unit
) {
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

/** Ein Knopf je Fassung: gefuellt, solange genau diese Fassung laeuft. */
@Composable
private fun TakeButton(
    label: String,
    title: String,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val named = "$title - $label"
    if (isPlaying) {
        val description = stringResource(R.string.a11y_music_stop_track, named)
        FilledTonalButton(
            onClick = onStop,
            modifier = modifier.semantics { contentDescription = description }
        ) {
            Text("■  $label")
        }
    } else {
        val description = stringResource(R.string.a11y_music_play_track, named)
        OutlinedButton(
            onClick = onPlay,
            modifier = modifier.semantics { contentDescription = description }
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Text(label, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

/** Was die Vorschau gerade spielt: ein Stueck in einer seiner Fassungen. */
private data class Take(val role: MusicRole, val variant: Int, val alt: Boolean) {
    val resourceName: String
        get() = role.variantResource(variant) + if (alt) ALT_SUFFIX else ""
}

/** Die bisherige Fassung eines Stuecks, falls das Hoertest-Paket sie mitliefert. */
private fun alternateResId(context: Context, role: MusicRole, variant: Int): Int? =
    context.resources
        .getIdentifier(role.variantResource(variant) + ALT_SUFFIX, "raw", context.packageName)
        .takeIf { it != 0 }

private const val ALT_SUFFIX = "_alt"

/**
 * Grundlautstaerke der Vorschau. Unter 1, damit der Lautheitsausgleich leise Stuecke auch anheben
 * kann (hoechstens [MusicLoudness.MAX_BOOST_DB], also etwa Faktor 2).
 */
private const val PREVIEW_VOLUME = 0.5f
