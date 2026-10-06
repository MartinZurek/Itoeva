package com.notime.glyphsim.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notime.glyphsim.matrix.PlayBackpack
import com.notime.glyphsim.matrix.PlayEffects
import com.notime.glyphsim.matrix.PlayMap
import com.notime.glyphsim.matrix.PlayMapScene
import com.notime.glyphsim.matrix.PlayScene.Place
import com.notime.glyphsim.stream.FennecWorld

private val PANEL = Color(0xF010202C)
private val ACCENT = Color(0xFF9DDAC7)
private val INK = Color(0xFFF0F5F6)
private val DIM = Color(0xFFAEC5D0)
private val GOLD = Color(0xFFFFD88A)

/**
 * **Das Menue der Figur** (Itoeva 2): erscheint, wenn man sie antippt. Karte, Rucksack und
 * Musik - mehr braucht es im Moment nicht. Ein Tipp daneben schliesst es.
 */
@Composable
internal fun GameAvatarMenu(
    german: Boolean,
    musicOn: Boolean,
    onMap: () -> Unit,
    onBackpack: () -> Unit,
    onChronicle: () -> Unit,
    onToggleMusic: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .background(PANEL, RoundedCornerShape(16.dp))
                // Tipps auf das Feld selbst schliessen es nicht.
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuButton(if (german) "Karte" else "Map", onMap)
            MenuButton(if (german) "Rucksack" else "Backpack", onBackpack)
            MenuButton(if (german) "Chronik" else "Chronicle", onChronicle)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MenuButton(
                if (german) (if (musicOn) "Musik aus" else "Musik an") else (if (musicOn) "Music off" else "Music on"),
                onToggleMusic
            )
            MenuButton(if (german) "Einstellungen" else "Settings", onSettings)
            }
        }
    }
}

/**
 * **Immer sichtbarer Knopf oben links** (Itoeva 2): fuehrt zurueck zum Startbildschirm mit
 * Wesenwahl und Musik (siehe [GameStartScreen]). Wunsch vom 04.10.: Aus dem Spiel heraus muss
 * man sichtbar zurueckkommen.
 */
@Composable
internal fun GameSettingsButton(german: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(12.dp)
            .background(PANEL, RoundedCornerShape(12.dp))
            .border(1.dp, ACCENT, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(if (german) "☰ Menü" else "☰ Menu", color = INK, fontSize = 14.sp)
    }
}

/** Sichtbarer Zurueck-Knopf in Karte und Rucksack - frueher schloss nur ein Tipp daneben. */
@Composable
private fun CloseButton(german: Boolean, onClose: () -> Unit) {
    Box(
        Modifier
            .border(1.dp, ACCENT, RoundedCornerShape(10.dp))
            .clickable(onClick = onClose)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(if (german) "← Zurück" else "← Back", color = INK, fontSize = 14.sp)
    }
}

@Composable
private fun MenuButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .border(1.dp, ACCENT, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Text(label, color = INK, fontSize = 15.sp)
    }
}

/**
 * **Die Weltkarte** (Itoeva 2): dieselbe Pixel-Karte wie vor weiten Wegen ([PlayMapScene]), mit
 * Ortsnamen. Der eigene Standort blinkt. Tippt man einen Ort an, zeigt sie den Weg dorthin
 * ([PlayMap.route]); gegangen wird er weiterhin selbst. Ein Tipp ausserhalb schliesst sie.
 */
@Composable
internal fun GameMapOverlay(current: Place, german: Boolean, phase: Int, initialTarget: Place? = null, onClose: () -> Unit) {
    var target by remember { mutableStateOf(initialTarget) }
    val labels = remember { PlayMapScene.labels(MAP_W, MAP_H) }
    val route = remember(current, target) { target?.let { PlayMap.route(current, it) }.orEmpty() }
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER } }
    val fontPx = with(LocalDensity.current) { 10.sp.toPx() }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC000000))
            .pointerInput(Unit) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .padding(16.dp)
                .background(PANEL, RoundedCornerShape(16.dp))
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(14.dp)
        ) {
            val here = FennecWorld.name(current, german)
            val title = target?.let {
                (if (german) "Weg von $here nach " else "Route from $here to ") + FennecWorld.name(it, german)
            } ?: ((if (german) "Hier bist du: " else "You are here: ") + here)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = ACCENT, fontSize = 14.sp, modifier = Modifier.weight(1f))
                CloseButton(german, onClose)
            }
            Text(
                if (german) "Tippe einen Ort an, um den Weg zu sehen." else "Tap a place to see the way.",
                color = DIM, fontSize = 11.sp
            )
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(top = 8.dp)
                    .pointerInput(labels) {
                        detectTapGestures { tap ->
                            val cellX = size.width / MAP_W.toFloat()
                            val cellY = size.height / MAP_H.toFloat()
                            target = labels.minByOrNull { (_, pos) ->
                                val dx = (pos.first + 0.5f) * cellX - tap.x
                                val dy = (pos.second + 0.5f) * cellY - tap.y
                                dx * dx + dy * dy
                            }?.key?.takeIf { it != current }
                        }
                    }
            ) {
                val cellX = size.width / MAP_W.toFloat()
                val cellY = size.height / MAP_H.toFloat()
                val blink = (phase / 4) % 2 == 0
                for (cell in PlayMapScene.build(current, route, MAP_W, MAP_H, 1f, blink)) {
                    val color = when {
                        cell.isLight -> GOLD
                        cell.brightness > 1_300 -> ACCENT
                        cell.brightness > 600 -> DIM
                        else -> Color(0xFF45616F)
                    }
                    drawRect(color, Offset(cell.x * cellX, cell.y * cellY), Size(cellX * 0.8f, cellY * 0.8f))
                }
                paint.textSize = fontPx
                for ((place, pos) in labels) {
                    val isHere = place == current ||
                        (place == Place.LIVING && PlayMap.regionOf(current) == PlayMap.Region.HOME)
                    paint.color = when {
                        place == target -> 0xFFFFD88A.toInt()
                        isHere -> 0xFF9DDAC7.toInt()
                        else -> 0xFFDAE7EE.toInt()
                    }
                    drawContext.canvas.nativeCanvas.drawText(
                        FennecWorld.name(place, german),
                        (pos.first + 0.5f) * cellX,
                        (pos.second + 3.6f) * cellY + fontPx / 2,
                        paint
                    )
                }
            }
        }
    }
}

private const val MAP_W = 100
private const val MAP_H = 50

/**
 * **Der Rucksack** (Itoeva 2): acht Plaetze und reversible Ablage. Ein Ding antippen waehlt es;
 * Tragen oder Ablegen folgt bewusst. Das gerade gehaltene Ding ist umrandet.
 * Ein Tipp ausserhalb schliesst ihn.
 */
@Composable
internal fun GameBackpackOverlay(
    backpack: PlayBackpack.Backpack,
    held: PlayEffects.Carried?,
    german: Boolean,
    onTake: (PlayEffects.Carried) -> Unit,
    onPutAway: () -> Unit,
    stowed: List<PlayEffects.Carried>,
    onStow: (Int) -> Unit,
    onRetrieve: (Int) -> Unit,
    onClose: () -> Unit
) {
    var selected by remember(backpack) { mutableStateOf<Int?>(null) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xAA000000))
            .pointerInput(Unit) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .background(PANEL, RoundedCornerShape(16.dp))
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(16.dp)
                .heightIn(max = 320.dp).verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    (if (german) "Rucksack" else "Backpack") + "  ${backpack.items.size}/${PlayBackpack.CAPACITY}",
                    color = ACCENT, fontSize = 14.sp
                )
                CloseButton(german, onClose)
            }
            if (backpack.items.isEmpty()) {
                Text(
                    if (german) "Noch leer - am Kuehlschrank, im Regal oder im Laden findet sich etwas."
                    else "Empty - try the fridge, a bookshelf or the shop.",
                    color = DIM, fontSize = 11.sp
                )
            }
            for (row in 0 until 2) {
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (col in 0 until PlayBackpack.CAPACITY / 2) {
                        val item = backpack.items.getOrNull(row * (PlayBackpack.CAPACITY / 2) + col)
                        val inHand = item != null && item == held
                        Box(
                            Modifier
                                .size(64.dp)
                                .background(Color(0xFF1B3040), RoundedCornerShape(10.dp))
                                .border(if (inHand) 2.dp else 1.dp, if (inHand) GOLD else Color(0xFF45616F), RoundedCornerShape(10.dp))
                                .clickable(enabled = item != null) {
                                    if (item == null) return@clickable
                                    selected = row * (PlayBackpack.CAPACITY / 2) + col
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (item != null) ItemIcon(item, Modifier.size(44.dp))
                        }
                    }
                }
            }
            selected?.let { index -> backpack.items.getOrNull(index)?.let { item ->
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MenuButton(if (german) "Tragen" else "Carry") { onTake(item) }
                    MenuButton(if (german) "In die Ablage" else "Store") { onStow(index); selected = null }
                }
            } }
            if (held != null) Text(if (german) "Aus der Hand zurück in den Rucksack" else "Put held item away",
                color = DIM, modifier = Modifier.clickable(onClick = onPutAway).padding(top = 8.dp))
            Text(if (german) "Ablage: Dinge bleiben gespeichert. Antippen holt sie zurück." else "Storage: items are kept. Tap to retrieve.",
                color = DIM, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
            stowed.chunked(4).forEachIndexed { row, items ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                    items.forEachIndexed { col, item ->
                        ItemIcon(item, Modifier.size(44.dp).clickable { onRetrieve(row * 4 + col) })
                    }
                }
            }

        }
    }
}

/** Das Bild eines Dings, so gezeichnet wie in der Hand der Figur. */
@Composable
internal fun ItemIcon(item: PlayEffects.Carried, modifier: Modifier) {
    val cells = remember(item) { PlayBackpack.iconCells(item) }
    Canvas(modifier) {
        if (cells.isEmpty()) return@Canvas
        val w = cells.maxOf { it.x } + 1
        val h = cells.maxOf { it.y } + 1
        val cell = minOf(size.width / w, size.height / h)
        val ox = (size.width - cell * w) / 2f
        val oy = (size.height - cell * h) / 2f
        for (c in cells) {
            val f = (c.brightness.coerceIn(0, 4095) / 4095f).coerceAtLeast(0.25f)
            drawRect(
                Color(0xFFF3F1EA).copy(alpha = f),
                Offset(ox + c.x * cell, oy + c.y * cell),
                Size(cell * 0.9f, cell * 0.9f)
            )
        }
    }
}
