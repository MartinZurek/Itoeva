package com.notime.glyphsim.stream

import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.net.SocketTimeoutException
import javax.net.ssl.SSLSocketFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/** Was im Bild ueber die Chat-Verbindung steht. Kein Dashboard, eine Zeile. */
internal enum class TwitchChatStatus { OFF, CONNECTING, LISTENING, RETRYING }

/**
 * Liest den oeffentlichen Twitch-Chat eines Kanals - kostenlos, anonym und nur lesend.
 *
 * ## Was hier bewusst fehlt
 *
 * Kein OAuth, kein Token, kein Konto, keine Bibliothek. Die Verbindung ist ein TLS-Socket aus
 * dem JDK und der Anmeldename ein anonymer `justinfan`-Name (siehe [TwitchIrc]). Das hat drei
 * Folgen, die alle erwuenscht sind:
 *
 * 1. Es gibt in diesem Client **kein Geheimnis**, das im Stream, in einem Protokoll oder in
 *    einem Commit landen koennte.
 * 2. Der Client kann nichts schreiben. Er kann im Chat also auch nichts anrichten.
 * 3. Es kommt keine neue Abhaengigkeit ins Projekt, nur fuer das Mitlesen von Textzeilen.
 *
 * ## Was er nicht kann - und warum das hier richtig ist
 *
 * Bits, Subs, Kanalpunkte und Fluestern brauchen alle ein Token und teils EventSub. Nichts
 * davon gehoert in diese Klasse: Sie waeren ein **eigener** [StreamInteractionProvider] neben
 * diesem, und die Spielschicht bliebe davon unberuehrt. Genau dafuer existiert die
 * Schnittstelle.
 *
 * ## Verbindungsabbruch
 *
 * Ein Stream laeuft stundenlang, und eine IRC-Verbindung haelt nicht stundenlang. [listen]
 * baut deshalb nach jedem Abriss neu auf, mit wachsendem Abstand bis [MAX_BACKOFF_MS]. Das
 * Lese-Zeitlimit ist kuerzer als Twitchs PING-Abstand, damit ein abgebrochener Faden nicht
 * still bis zum naechsten PING haengt und die Coroutine jederzeit abbrechbar bleibt.
 */
internal class TwitchChatInteractionProvider(
    private val channel: String,
    private val config: StreamCommandConfig = StreamCommandConfig(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val connect: () -> Socket = {
        SSLSocketFactory.getDefault().createSocket(TwitchIrc.HOST, TwitchIrc.TLS_PORT)
    }
) : StreamInteractionProvider {

    override val origin: ExternalImpulseSource = ExternalImpulseSource.TWITCH_CHAT_FREE

    private val stream = MutableSharedFlow<ViewerCommand>(extraBufferCapacity = 64)

    override val commands: Flow<ViewerCommand> = stream.asSharedFlow()

    private val statusState = MutableStateFlow(TwitchChatStatus.OFF)

    val status: StateFlow<TwitchChatStatus> = statusState.asStateFlow()

    /**
     * Laeuft, bis die aufrufende Coroutine abgebrochen wird.
     *
     * Kehrt bei leerem Kanalnamen sofort zurueck: Ohne Kanal gibt es nichts zu lesen, und ein
     * fehlender Eintrag soll den Stream-Client nicht in eine endlose Fehlerschleife schicken -
     * der Demo-Eingang funktioniert dann weiterhin.
     */
    suspend fun listen() {
        val target = TwitchIrc.normalizeChannel(channel)
        if (target.isEmpty()) {
            statusState.value = TwitchChatStatus.OFF
            return
        }
        var backoff = FIRST_BACKOFF_MS
        while (true) {
            coroutineContext.ensureActive()
            statusState.value = TwitchChatStatus.CONNECTING
            val clean = runCatching { readUntilClosed(target) }
            coroutineContext.ensureActive()
            clean.onFailure { Log.w(TAG, "Chat-Verbindung verloren: ${it.javaClass.simpleName}") }
            statusState.value = TwitchChatStatus.RETRYING
            delay(backoff)
            backoff = (backoff * 2).coerceAtMost(MAX_BACKOFF_MS)
        }
    }

    private suspend fun readUntilClosed(target: String) = withContext(Dispatchers.IO) {
        connect().use { socket ->
            socket.soTimeout = READ_TIMEOUT_MS
            val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            TwitchIrc.handshake(TwitchIrc.anonymousNick(clock().toInt()), target).forEach { send(writer, it) }
            statusState.value = TwitchChatStatus.LISTENING
            while (true) {
                coroutineContext.ensureActive()
                // Ein Zeitlimit statt eines dauerhaft blockierenden Lesens: Nur so bemerkt diese
                // Schleife ueberhaupt, dass die Coroutine abgebrochen wurde.
                val raw = try {
                    reader.readLine() ?: break
                } catch (timeout: SocketTimeoutException) {
                    continue
                }
                when (val line = TwitchIrc.parseLine(raw)) {
                    is TwitchIrc.Line.Ping -> send(writer, TwitchIrc.pong(line.token))
                    is TwitchIrc.Line.Chat -> emit(line)
                    TwitchIrc.Line.Ignored -> Unit
                }
            }
        }
    }

    /**
     * Uebersetzt eine Chat-Zeile und legt sie ab - oder verwirft sie.
     *
     * Der Chat-Text selbst verlaesst diese Stelle nie. Weitergegeben wird nur, was verstanden
     * wurde: ein Platz und der Name, gegen den der Abstand zaehlt.
     */
    private fun emit(line: TwitchIrc.Line.Chat) {
        val command = viewerCommandOf(line.viewerId, line.text, origin, clock(), config) ?: return
        stream.tryEmit(command)
    }

    private fun send(writer: BufferedWriter, line: String) {
        writer.write(line)
        writer.write("\r\n")
        writer.flush()
    }

    private companion object {
        const val TAG = "ItoevaStream"
        const val FIRST_BACKOFF_MS = 2_000L
        const val MAX_BACKOFF_MS = 60_000L

        /** Kuerzer als Twitchs PING-Abstand von rund fuenf Minuten. */
        const val READ_TIMEOUT_MS = 45_000
    }
}
