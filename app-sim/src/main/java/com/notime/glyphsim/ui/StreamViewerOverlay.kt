package com.notime.glyphsim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notime.glyphsim.R
import com.notime.glyphsim.stream.ExternalImpulse
import com.notime.glyphsim.stream.StreamCommandConfig
import com.notime.glyphsim.stream.StreamCommandParser
import com.notime.glyphsim.stream.StreamGateLogEntry
import com.notime.glyphsim.stream.StreamRejection
import com.notime.glyphsim.stream.TwitchChatStatus

/**
 * Die drei Zeilen, die ein Zuschauer im Bild braucht - und keine vierte.
 *
 * ## Warum so wenig
 *
 * Der Avatar und sein Tag sind der Gegenstand des Streams. Eine Zuschauer-Oberflaeche, die mehr
 * Platz einnimmt als noetig, verschiebt genau das. Deshalb steht hier nur, was jemand wissen
 * muss, der gerade erst eingeschaltet hat:
 *
 * 1. **Geht das ueberhaupt?** - der Zustand der Chat-Verbindung.
 * 2. **Was tippe ich?** - die Befehle, gebaut aus derselben [StreamCommandConfig], die der
 *    Parser benutzt. Eine Anzeige, die eine Syntax verspricht, die es nicht gibt, waere der
 *    teuerste Fehler an dieser Stelle: Niemand meldet ihn, die Leute tippen ihn ab und geben
 *    auf.
 * 3. **Ist etwas passiert?** - die letzte Entscheidung, angenommen oder abgelehnt, mit Grund.
 *
 * Die dritte Zeile ist die wichtigste und war anfangs nicht da. Ohne sie ist eine Ablehnung im
 * Stream nicht von einem Absturz zu unterscheiden: Man tippt, und nichts geschieht. Mit ihr ist
 * der Abstand sichtbar und wird zum Teil des Spiels statt zu einem Verdacht.
 */
@Composable
internal fun StreamViewerOverlay(
    config: StreamCommandConfig,
    chatStatus: TwitchChatStatus,
    channel: String,
    lastEntry: StreamGateLogEntry?,
    latestImpulse: ExternalImpulse?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x66000000))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = when (chatStatus) {
                TwitchChatStatus.OFF -> stringResource(R.string.stream_chat_off)
                TwitchChatStatus.CONNECTING -> stringResource(R.string.stream_chat_connecting)
                TwitchChatStatus.LISTENING -> stringResource(R.string.stream_chat_listening, channel)
                TwitchChatStatus.RETRYING -> stringResource(R.string.stream_chat_retrying)
            },
            color = if (chatStatus == TwitchChatStatus.LISTENING) LIVE else TamaPalette.TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = config.hint(),
            color = TamaPalette.TextMuted,
            fontSize = 11.sp
        )
        Text(
            text = lastEntry?.let { describe(it) }
                ?: latestImpulse?.let {
                    stringResource(
                        R.string.stream_viewer_impulse,
                        it.savedSlotId,
                        stringResource(it.animationType.labelRes)
                    )
                }
                ?: stringResource(R.string.stream_viewer_ready),
            color = if (lastEntry?.accepted == true) LIVE else TamaPalette.TextMuted,
            fontSize = 10.sp
        )
    }
}

/** Dieselbe Farbe wie der Rand eines laufenden Anstosses - siehe DockScreen. */
private val LIVE = Color(0xFF7FD1A6)

@Composable
private fun describe(entry: StreamGateLogEntry): String {
    val letter = entry.slotId?.let { StreamCommandParser.letterFor(it).toString() }.orEmpty()
    return when (entry.rejection) {
        null -> stringResource(R.string.stream_decision_accepted, entry.viewerId, letter)
        StreamRejection.VIEWER_COOLDOWN ->
            stringResource(R.string.stream_decision_viewer_cooldown, entry.viewerId)
        StreamRejection.GLOBAL_COOLDOWN ->
            stringResource(R.string.stream_decision_global_cooldown, entry.viewerId)
        StreamRejection.IMPULSE_PENDING ->
            stringResource(R.string.stream_decision_pending, entry.viewerId)
        StreamRejection.SLOT_EMPTY ->
            stringResource(R.string.stream_decision_slot_empty, entry.viewerId, letter)
    }
}
