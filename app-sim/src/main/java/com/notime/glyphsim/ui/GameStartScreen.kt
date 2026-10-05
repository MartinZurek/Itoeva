package com.notime.glyphsim.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.CreatureSheets
import com.notime.glyphsim.matrix.CreatureSprites
import kotlinx.coroutines.delay

private val PANEL = Color(0xE6101C26)
private val ACCENT = Color(0xFF9DDAC7)
private val INK = Color(0xFFF0F5F6)
private val DIM = Color(0xFFAEC5D0)
private val GOLD = Color(0xFFFFD88A)

/**
 * **Der Startbildschirm von Itoeva 2** (Wunsch vom 04.10.): Vor dem Spielen waehlt man sein Wesen
 * und stellt die Musik ein. Bisher startete das Spiel immer mit dem zuletzt gespeicherten Wesen,
 * ohne Moeglichkeit zu wechseln. Aus dem Spiel fuehrt der Knopf oben links (und "Einstellungen"
 * im Figurmenue) hierher zurueck.
 *
 * Die Wahl wird ueber [AvatarSpeciesPrefs] gespeichert - dieselbe Stelle, die auch die Begleiter-
 * Wahl in Itoeva 1 benutzt; Pflegebuch und Spielstand haengen weiterhin am gewaehlten Wesen.
 */
@Composable
fun GameStartScreen(onStart: () -> Unit) {
    val context = LocalContext.current
    val german = LocalConfiguration.current.locales[0].language == "de"
    var selected by remember { mutableStateOf(AvatarSpeciesPrefs.get(context)) }
    var musicOn by remember { mutableStateOf(PlayMusic.isEnabled(context)) }
    val backdrop = remember {
        runCatching {
            context.assets.open("scenes/living.png").use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
        }.getOrNull()
    }
    // Eigener Takt fuer das Atmen der Wesen in der Auswahl.
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(120)
            tick += 120
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0B1218)), contentAlignment = Alignment.Center) {
        if (backdrop != null) {
            Image(
                bitmap = backdrop, contentDescription = null,
                contentScale = ContentScale.Crop, filterQuality = FilterQuality.None,
                modifier = Modifier.fillMaxSize()
            )
            Box(Modifier.fillMaxSize().background(Color(0x99000000)))
        }
        Column(
            Modifier
                .widthIn(max = 860.dp)
                .fillMaxWidth()
                .padding(16.dp)
                .background(PANEL, RoundedCornerShape(18.dp))
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Itoeva", color = GOLD, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Text(
                if (german) "Wähle dein Wesen" else "Choose your companion",
                color = ACCENT, fontSize = 15.sp
            )
            AvatarSpecies.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { species ->
                        SpeciesCard(
                            species = species,
                            sheet = CreatureSheets.get(context, species),
                            chosen = species == selected,
                            chosenTick = if (species == selected) tick else 0L,
                            onClick = { selected = species }
                        )
                    }
                }
            }
            Text(
                stringResource(selected.taglineRes),
                color = DIM, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 520.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                StartButton(
                    if (german) (if (musicOn) "Musik: an" else "Musik: aus") else (if (musicOn) "Music: on" else "Music: off"),
                    highlighted = false
                ) {
                    musicOn = !musicOn
                    PlayMusic.setEnabled(context, musicOn)
                }
                StartButton(if (german) "Spielen" else "Play", highlighted = true) {
                    AvatarSpeciesPrefs.set(context, selected)
                    onStart()
                }
            }
        }
    }
}

@Composable
private fun SpeciesCard(
    species: AvatarSpecies,
    sheet: CreatureSheets.Sheet?,
    chosen: Boolean,
    chosenTick: Long,
    onClick: () -> Unit
) {
    // Feiner Bogen (128er-Bilder): eigene Ruheschleife; einfacher Bogen: Atmen im Wechsel.
    val rich = sheet?.frameSize == CreatureSprites.Rich.FRAME
    val frameSize = if (rich) CreatureSprites.Rich.FRAME else CreatureSprites.FRAME
    val frame = when {
        rich -> CreatureSprites.Rich.idleFrame(chosenTick)
        chosen && (chosenTick / 900) % 2 == 1L -> CreatureSprites.IDLE_BREATH
        else -> CreatureSprites.IDLE
    }
    Column(
        Modifier
            .width(150.dp)
            .background(if (chosen) Color(0x332E5A4A) else Color.Transparent, RoundedCornerShape(14.dp))
            .border(if (chosen) 2.dp else 1.dp, if (chosen) GOLD else Color(0x557FA2B0), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(Modifier.size(96.dp)) {
            if (sheet != null) {
                val px = size.minDimension.toInt()
                drawImage(
                    image = sheet.frames[frame],
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(frameSize, frameSize),
                    dstOffset = IntOffset(((size.width - px) / 2).toInt(), 0),
                    dstSize = IntSize(px, px),
                    filterQuality = FilterQuality.None
                )
            }
        }
        Text(
            stringResource(species.labelRes),
            color = if (chosen) GOLD else INK, fontSize = 14.sp,
            fontWeight = if (chosen) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun StartButton(label: String, highlighted: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .background(if (highlighted) Color(0xFF2E5A4A) else Color.Transparent, RoundedCornerShape(12.dp))
            .border(1.dp, if (highlighted) GOLD else ACCENT, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        Text(label, color = if (highlighted) GOLD else INK, fontSize = 16.sp)
    }
}
