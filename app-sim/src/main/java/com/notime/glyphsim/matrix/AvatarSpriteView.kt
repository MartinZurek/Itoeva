package com.notime.glyphsim.matrix

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

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
    shadeSide: AvatarShading.Side = AvatarShading.Side.NONE
) {
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
        drawSprite(frame, brightnessScale, species, shadeSide)
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
