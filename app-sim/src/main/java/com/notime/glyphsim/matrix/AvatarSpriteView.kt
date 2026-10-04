package com.notime.glyphsim.matrix

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val LED_OFF_COLOR = Color(MatrixColors.LED_OFF)
private val LED_ON_COLOR = Color(MatrixColors.LED_ON)

// Wie stark die Augen Richtung Weiss aufgehellt werden gegenueber dem flachen Akzentton des
// restlichen Gesichts (siehe AvatarAccent.eyesIn) - deutlich genug fuer ein Glanzlicht, aber
// nicht so stark, dass die Speziesfarbe verloren geht.
private const val EYE_GLINT_FRACTION = 0.4f

/**
 * Zeichnet den Tamagotchi-Avatar als eckige Pixelgrafik direkt auf dem schwarzen
 * Dock-Hintergrund - bewusst KEIN runder "Puck" wie [SimulatedMatrixView] (der Avatar
 * ist keine Uhr/Matrix-Hardware-Nachbildung, siehe [AvatarGeometry]). Jede Zelle von
 * [frame] wird als gefuelltes Quadrat gezeichnet statt als isoliertem LED-Punkt,
 * dadurch wirkt die Kreatur wie ein zusammenhaengendes Sprite. Zellen mit Helligkeit 0
 * werden komplett ausgelassen (statt gedimmt gezeichnet wie bei der Matrix) - dadurch
 * bleibt nur die Kreatur-Silhouette sichtbar, kein flaechiges Raster drumherum.
 */
/**
 * [showBackground] fuellt die Sprite-Flaeche schwarz. Gebraucht wird das nur dort, wo der Avatar
 * auf hellem Grund steht und sich sonst nicht abheben wuerde (Vorschaubilder im
 * Einstellungs-Dialog). Auf den Bildschirmen selbst liegt er ohnehin auf Schwarz - dort wirkt
 * die Fuellung als sichtbarer Kasten um die Figur und schneidet zudem alles ab, was ueber ihr
 * Quadrat hinausreicht, weshalb sie dort abgeschaltet wird.
 */
@Composable
fun AvatarSpriteView(
    frame: IntArray,
    modifier: Modifier = Modifier,
    showBackground: Boolean = true,
    /**
     * Beschreibung fuer TalkBack (Avatar-Name + Stimmung) - siehe [SimulatedMatrixView] fuer
     * dieselbe Begruendung. Null bei rein dekorativen Vorschauen.
     */
    contentDescription: String? = null,
    /**
     * Daempfung der gesamten Figur (1 = voll, kleiner = zurueckgenommen).
     *
     * Gebraucht fuer BESUCHER: Stehen zwei Kreaturen voreinander, verschmelzen zwei gleich helle
     * Silhouetten zu einem einzigen unlesbaren Fleck - man erkennt keine von beiden. Eine
     * durchgehend etwas schwaechere Zeichnung des Gastes loest das an der Wurzel: Sie liest sich
     * als groesserer Abstand zum Betrachter, und der eigene Avatar bleibt in JEDER Lage die
     * hellste Figur im Bild. Das wirkt auch dann noch, wenn sich eine Ueberdeckung nicht
     * vermeiden laesst.
     */
    brightnessScale: Float = 1f,
    /**
     * Welche Kreatur hier gezeichnet wird - bestimmt die Farbe ihres GESICHTS (siehe
     * [AvatarAccent] und [AvatarPalette]). Der Koerper bleibt weiss wie die Welt.
     *
     * `null` laesst auch das Gesicht weiss, also das Bild von vor NT-076.
     *
     * Diese Ansicht ist NUR fuer Kreaturen. Die 13x13-Zeichen aus `LivingSymbolFrames` liefen
     * bis NT-079 auch hier durch und wurden dabei zerschert, weil hier mit der Zeilenbreite des
     * Avatars gelesen wird; sie gehen jetzt ueber [SimulatedMatrixView] (siehe PlayWishBubble).
     */
    species: AvatarSpecies? = null,
    /**
     * Beschattete Flanke (siehe [AvatarShading.Side]) - im Stand [AvatarShading.Side.NONE],
     * beim Gehen die Seite, von der die Kreatur KOMMT.
     */
    shadeSide: AvatarShading.Side = AvatarShading.Side.NONE,
    /** Im aktiven Spiel: vorne/hinten erhalten eigene gezeichnete Ansichten. */
    gameDirection: PlayControl.Dir? = null,
    /** Im aktiven Spiel bleibt die Ansicht beim Anhalten, ohne weiterzulaufen. */
    gameMoving: Boolean? = null
) {
    // **Die Wesen in feiner Pixel-Art** (siehe [CreatureSprites]): Gibt es fuer die Kreatur einen
    // Bogen, wird statt der groben Zellen das passende Bild daraus gezeichnet. Die grobe Pose
    // bestimmt weiter, WAS die Figur tut; der Takt hier nur Atmen, Schritte und Blinzeln.
    val context = LocalContext.current
    val sheet = species?.let { CreatureSheets.get(context, it) }
    val tick by produceState(0L, sheet != null) {
        if (sheet == null) return@produceState
        while (true) {
            value = System.currentTimeMillis()
            delay(CREATURE_TICK_MS)
        }
    }
    Canvas(
        modifier = modifier
            // Hoeher als breit: Das Raster hat oberhalb der Figur Kopffreiheit, damit Spruenge
            // nicht abgeschnitten werden (siehe AvatarGeometry.HEADROOM).
            .aspectRatio(AvatarGeometry.SIZE.toFloat() / AvatarGeometry.HEIGHT)
            .then(if (showBackground) Modifier.background(Color.Black) else Modifier)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            )
    ) {
        if (sheet != null && species != null) {
            drawCreature(sheet, frame, brightnessScale, species, shadeSide, tick + species.ordinal * 731L,
                gameDirection, gameMoving)
        } else {
            drawSprite(frame, brightnessScale, species, shadeSide)
        }
    }
}

/** Wie oft die feinen Figuren ihr Bild pruefen - fein genug fuer Schritte (140 ms). */
private const val CREATURE_TICK_MS = 70L

/** Laedt die Bilderboegen der Kreaturen einmal und haelt sie (je einige Kilobyte). */
internal object CreatureSheets {
    private val cache = HashMap<AvatarSpecies, ImageBitmap?>()

    fun get(context: Context, species: AvatarSpecies): ImageBitmap? = synchronized(cache) {
        cache.getOrPut(species) {
            runCatching {
                context.assets.open(CreatureSprites.assetFor(species)).use { BitmapFactory.decodeStream(it) }
                    ?.asImageBitmap()
            }.getOrNull()
        }
    }
}

private fun DrawScope.drawCreature(
    sheet: ImageBitmap,
    frame: IntArray,
    brightnessScale: Float,
    species: AvatarSpecies,
    shadeSide: AvatarShading.Side,
    timeMs: Long,
    gameDirection: PlayControl.Dir?,
    gameMoving: Boolean?
) {
    val look = CreatureSprites.look(frame, species, shadeSide, timeMs, gameDirection, gameMoving)
    val cell = size.width / AvatarGeometry.SIZE
    val drawn = size.width * CreatureSprites.SCALE
    // Die Fuesse stehen dort, wo die grobe Figur aufsetzt (siehe AvatarFooting) - angehoben um
    // so viel, wie die grobe Pose gerade abhebt.
    val feetY = (AvatarBodies.forSpecies(species).groundRow() + 1 - look.liftCells) * cell
    val top = feetY - drawn * CreatureSprites.FEET / CreatureSprites.FRAME
    val left = (size.width - drawn) / 2f
    val dim = brightnessScale.coerceIn(0f, 1f)
    val filter = if (dim < 1f) ColorFilter.colorMatrix(ColorMatrix().apply { setToScale(dim, dim, dim, 1f) }) else null
    scale(scaleX = if (look.mirrored) -1f else 1f, scaleY = 1f, pivot = Offset(size.width / 2f, size.height / 2f)) {
        drawImage(
            image = sheet,
            srcOffset = IntOffset(look.frame * CreatureSprites.FRAME, 0),
            srcSize = IntSize(CreatureSprites.FRAME, CreatureSprites.FRAME),
            dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
            dstSize = IntSize(drawn.roundToInt(), drawn.roundToInt()),
            colorFilter = filter,
            // Ohne Glaettung: Jeder Pixel des Bogens bleibt ein scharfes Quadrat.
            filterQuality = FilterQuality.None
        )
    }
}

private fun DrawScope.drawSprite(
    unorientedFrame: IntArray,
    brightnessScale: Float,
    species: AvatarSpecies?,
    shadeSide: AvatarShading.Side
) {
    // **Hier und nicht in den Animationsdaten** (siehe [AvatarShading]): Die Posen bleiben reine
    // Punktmengen, Ueberblendungen rechnen unveraendert weiter, und die abgelegten
    // Vergleichsbilder der Reaktionspruefung bleiben gueltig. Schattierung ist Darstellung.
    // Dasselbe gilt fuer die Farbe - eine Pose weiss nicht, wer sie gerade einnimmt.
    // **Der Koerper ist weiss, die Farbe sitzt im Gesicht** (siehe [AvatarAccent]). Eine
    // durchgehend eingefaerbte Figur war eine einfarbige Flaeche in Kreaturform - sie sagte,
    // welches Wesen es ist, aber nichts darueber, was daran ein Gesicht ist.
    val accentColor = species?.let { Color(AvatarPalette.tintFor(it)) } ?: LED_ON_COLOR
    // Die Augen bekommen denselben Ton, nur heller - ein Glanzlicht statt einer zweiten Farbe
    // (siehe AvatarAccent.eyesIn).
    val eyeColor = lerpColor(accentColor, Color.White, EYE_GLINT_FRACTION)
    // Erst in Laufrichtung drehen (siehe [AvatarFacing]), dann alles Weitere an der gedrehten
    // Form - sonst saessen Gesicht und Schatten auf der falschen Seite.
    val rawFrame = AvatarFacing.orient(unorientedFrame, shadeSide)
    val frame = AvatarShading.shade(rawFrame, side = shadeSide)
    // Das Gesicht wird an der ROHEN Form gesucht: Die Schattierung aendert Helligkeiten, nicht
    // die Silhouette, und ein Loch bleibt ein Loch - aber so haengt der Fund nicht daran.
    val face = AvatarAccent.facesIn(rawFrame)
    val eyes = AvatarAccent.eyesIn(rawFrame)
    val cell = size.width / AvatarGeometry.SIZE
    // Winziger Ueberlapp zwischen benachbarten Zellen, damit Antialiasing keine
    // sichtbaren Ein-Pixel-Spalten zwischen zwei eigentlich zusammenhaengenden
    // Sprite-Zellen erzeugt.
    val cellSize = Size(cell + 0.75f, cell + 0.75f)
    for (y in 0 until AvatarGeometry.HEIGHT) {
        for (x in 0 until AvatarGeometry.SIZE) {
            val index = y * AvatarGeometry.SIZE + x
            val imGesicht = face.getOrElse(index) { false }
            val imAuge = eyes.getOrElse(index) { false }
            val brightness = frame.getOrElse(index) { 0 }
            // Gesichtszellen sind LOECHER in der Figur - dort steht keine Helligkeit, sie waeren
            // sonst gar nicht gezeichnet worden. Genau deshalb bekommen sie ihre eigene.
            if (brightness <= 0 && !imGesicht) continue
            val roh = if (imGesicht) AvatarGeometry.MAX_BRIGHTNESS else brightness
            val fraction = (roh.coerceIn(0, AvatarGeometry.MAX_BRIGHTNESS).toFloat() /
                AvatarGeometry.MAX_BRIGHTNESS) * brightnessScale.coerceIn(0f, 1f)
            val ziel = if (imAuge) eyeColor else if (imGesicht) accentColor else LED_ON_COLOR
            drawRect(
                color = lerpColor(LED_OFF_COLOR, ziel, fraction),
                topLeft = Offset(x * cell, y * cell),
                size = cellSize
            )
        }
    }
}

private fun lerpColor(from: Color, to: Color, fraction: Float): Color = Color(
    red = from.red + (to.red - from.red) * fraction,
    green = from.green + (to.green - from.green) * fraction,
    blue = from.blue + (to.blue - from.blue) * fraction,
    alpha = 1f
)
