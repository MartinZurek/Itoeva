package com.notime.glyphsim.stream

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Eine Quelle, aus der Angebote von aussen kommen.
 *
 * ## Die eine Regel
 *
 * Ein Provider uebersetzt seine eigene Welt - Chat-Zeilen, spaeter Bits-Ereignisse - in
 * [ViewerCommand] und hoert dort auf. Er kennt weder Plaetze noch Abstaende noch den Living
 * Agent; was aus einem Angebot wird, entscheiden [StreamCommandGate] und die bereits
 * vorhandene Auswahl in [StreamInteractions].
 *
 * ## Warum das die wichtigste Grenze des Stream-Clients ist
 *
 * Die Spielschicht soll nie erfahren, ob ein Angebot aus einem kostenlosen Chat-Befehl, aus dem
 * Testmodus oder spaeter aus 100 Bits entstanden ist. Solange das so bleibt, ist die
 * Bezahlfrage eine Frage **dieser** Schnittstelle: Ein neuer Eingang ist eine neue
 * Implementierung hier und ein neuer Eintrag in [ExternalImpulseSource] - kein Eingriff in
 * Ziele, Plaene oder Handlungen.
 */
internal interface StreamInteractionProvider {

    /** Steht nur im Protokoll, nie in einer Spielentscheidung. */
    val origin: ExternalImpulseSource

    /** Normalisierte Angebote. Was der Provider nicht versteht, taucht hier gar nicht auf. */
    val commands: Flow<ViewerCommand>
}

/**
 * Der Eingang ohne Netz: fuer Tests, fuer die Entwicklung und fuer den Demo-Modus im Client.
 *
 * Damit laesst sich die vollstaendige Strecke - Befehl, Tor, Platz, Impuls, Living Agent,
 * Aufraeumen - lange bevor eine echte Twitch-Verbindung existiert am Geraet vorfuehren. Das ist
 * der Grund, warum die Twitch-Anbindung die Arbeit an der Mechanik nie blockiert hat.
 */
internal class LocalTestInteractionProvider(
    private val config: StreamCommandConfig = StreamCommandConfig(),
    private val clock: () -> Long = System::currentTimeMillis
) : StreamInteractionProvider {

    override val origin: ExternalImpulseSource = ExternalImpulseSource.LOCAL_TEST_PROVIDER

    private val stream = MutableSharedFlow<ViewerCommand>(extraBufferCapacity = 16)

    override val commands: Flow<ViewerCommand> = stream.asSharedFlow()

    /**
     * Genau das, was ein Zuschauer in den Chat tippen wuerde - derselbe Text, derselbe Parser.
     *
     * Gibt `false` zurueck, wenn die Zeile kein Befehl war. Das ist kein Fehler, sondern der
     * Regelfall fuer alles, was nicht an uns gerichtet ist.
     */
    fun type(viewerId: String, message: String): Boolean {
        val command = viewerCommandOf(viewerId, message, origin, clock(), config) ?: return false
        return stream.tryEmit(command)
    }
}
