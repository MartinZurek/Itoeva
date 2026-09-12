package com.notime.glyphsim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.notime.glyphsim.living.LivingSymbolPair
import com.notime.glyphsim.matrix.AvatarSpriteView
import kotlin.math.roundToInt

/**
 * **Was das Wesen will - und woran es haengt.** Zwei kleine Symbole ueber dem Kopf, kein Satz.
 *
 * ## Warum das hier steht, wo vorher Text stand
 *
 * Bis hierher erschien ueber dem Kopf ein gewuerfelter Satz (`PlaySpeech`, bei jeder dritten
 * Regung), der allein am Thema hing. Er hatte mit dem ZIEL des Wesens nichts zu tun - der
 * Living Agent entschied laengst, aber nichts davon war zu sehen. Wer zusah, konnte nicht
 * unterscheiden, ob die Figur etwas wollte oder ob der Wuerfel es ergab.
 *
 * Hier steht jetzt, was tatsaechlich im Zustand steht, und nur das.
 *
 * ## Zwei Symbole, klar unterschieden
 *
 * Der Wunsch steht gross und hell. Das Hindernis daneben ist kleiner und blasser - es ist eine
 * Randbemerkung zum Wunsch, keine zweite Meldung. Waeren beide gleich gross, laese man sie als
 * Aufzaehlung, und aus "ich will essen, aber mir fehlt Geld" wuerde "Essen. Arbeit." Genau die
 * Beziehung zwischen den beiden ist die kleine Geschichte.
 *
 * ## Kein Text, auch nicht klein
 *
 * Die Blase faengt keine Gesten ab: Wer die Figur antippt, oeffnet weiterhin das Gespraech -
 * und dort gibt es Sprache. Das ist die Trennung, die diese Aenderung traegt: Wer fragt,
 * bekommt eine Auskunft; wer zusieht, bekommt ein Bild.
 */
@Composable
internal fun PlayWishBubble(
    symbols: LivingSymbolPair,
    avatarOffset: Offset,
    avatarSizeDp: Float,
    maxWidthPx: Float
) {
    val density = LocalDensity.current
    val avatarPx = with(density) { avatarSizeDp.dp.toPx() }
    val wunschDp = avatarSizeDp * WISH_SCALE
    val wunschPx = with(density) { wunschDp.dp.toPx() }

    // Ueber dem Kopf und leicht nach rechts versetzt - wie die Traumblase, damit beide als
    // dieselbe Art von Aeusserung gelesen werden und nicht als zwei Systeme.
    val left = (avatarOffset.x + avatarPx * 0.55f)
        .coerceIn(0f, (maxWidthPx - wunschPx).coerceAtLeast(0f))
    val top = (avatarOffset.y - wunschPx * 0.95f).coerceAtLeast(0f)

    Row(
        modifier = Modifier.offset { IntOffset(left.roundToInt(), top.roundToInt()) }
    ) {
        Box(
            modifier = Modifier
                .size(wunschDp.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
        ) {
            LivingSymbolFrames.frameFor(symbols.wish)?.let { frame ->
                AvatarSpriteView(
                    frame = frame,
                    showBackground = false,
                    brightnessScale = 0.95f,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        symbols.obstacle?.let { hindernis ->
            LivingSymbolFrames.frameFor(hindernis)?.let { frame ->
                Box(
                    modifier = Modifier
                        .size((wunschDp * OBSTACLE_SCALE).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.03f))
                ) {
                    AvatarSpriteView(
                        frame = frame,
                        showBackground = false,
                        // Blasser als der Wunsch: eine Randbemerkung, keine zweite Meldung.
                        brightnessScale = 0.55f,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Wie gross der Wunsch gegenueber der Figur ist.
 *
 * Gross genug, dass die dreizehn Zellen auf einem Telefon noch als Motiv lesbar sind, und klein
 * genug, dass die Figur selbst das Bild behaelt. Sie ist die Hauptsache; das Symbol erklaert
 * sie nur.
 */
private const val WISH_SCALE = 0.62f

/** Das Hindernis ist kleiner als der Wunsch - siehe die Begruendung in der Klassendoku. */
private const val OBSTACLE_SCALE = 0.72f
