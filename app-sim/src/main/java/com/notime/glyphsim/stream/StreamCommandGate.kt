package com.notime.glyphsim.stream

/** Warum ein Angebot nicht durchgelassen wurde. Jede Ablehnung hat genau einen Grund. */
internal enum class StreamRejection {
    /** Dieser Zuschauer war zu kurz vorher schon dran. */
    VIEWER_COOLDOWN,

    /** Der gemeinsame Mindestabstand aller Zuschauer ist noch nicht abgelaufen. */
    GLOBAL_COOLDOWN,

    /** Ein Anstoss laeuft noch; zwei gleichzeitig wuerden dieselbe Figur zerreissen. */
    IMPULSE_PENDING,

    /** In dem genannten Platz liegt gerade nichts. */
    SLOT_EMPTY
}

/** Ein Eintrag des Eingangsprotokolls. Bewusst ohne Chat-Text und ohne Personendaten. */
internal data class StreamGateLogEntry(
    val atMillis: Long,
    val viewerId: String,
    val origin: ExternalImpulseSource,
    val slotId: Int?,
    val rejection: StreamRejection?
) {
    val accepted: Boolean get() = rejection == null
}

/**
 * Der Gedaechtnisteil der Eingangsschicht: wer wann zuletzt durchkam, und das Protokoll.
 *
 * Ein Wert statt eines Objekts mit Feldern, damit die Torpruefung eine reine Funktion bleiben
 * kann und sich ohne Uhr und ohne Nebenlaeufigkeit pruefen laesst.
 */
internal data class StreamGateState(
    val lastGlobalAcceptMillis: Long? = null,
    val lastAcceptByViewer: Map<String, Long> = emptyMap(),
    val log: List<StreamGateLogEntry> = emptyList()
)

internal sealed interface StreamGateDecision {
    val state: StreamGateState

    data class Accepted(
        override val state: StreamGateState,
        val slotId: Int,
        val command: ViewerCommand
    ) : StreamGateDecision

    data class Rejected(
        override val state: StreamGateState,
        val reason: StreamRejection
    ) : StreamGateDecision
}

/**
 * Die Schutzschicht zwischen Eingang und Spiel.
 *
 * ## Was sie ist
 *
 * Eine Tuer mit zwei Fragen: Darf dieser Zuschauer gerade? Gibt es etwas, worauf er sich
 * bezieht? Mehr nicht - insbesondere **keine Moderationsplattform**,
 * keine Sperrlisten, keine Rollen. Das waere eine zweite Anwendung neben dem Spiel, und dafuer
 * gibt es bislang keinen belegten Bedarf.
 *
 * ## Was sie ausdruecklich nicht tut
 *
 * Sie entscheidet nichts ueber das Spiel. Ein angenommenes Angebot wird zu einem
 * [ExternalImpulse] und damit zu einem [com.notime.glyphsim.living.GoalInfluence] - einem
 * Vorschlag, den dieselbe Utility-Auswahl ueberstimmen darf wie jeden anderen Anreiz. Ein
 * Zuschauer kann das Wesen also anstupsen, nie fernsteuern.
 *
 * ## Reihenfolge der Pruefungen
 *
 * Erst die Abstaende, dann der Weltzustand. Das ist kein Zufall: Wer auf Abstand steht, wird
 * abgelehnt, egal was in den Plaetzen liegt. Wer dagegen einen leeren Platz nennt, soll dafuer
 * **nicht** seinen Abstand verlieren - er hat nur danebengegriffen, und eine Minute Schweigen
 * als Strafe fuer einen Tippfehler waere die falsche Lehre fuer ein neues Publikum.
 */
internal object StreamCommandGate {

    /**
     * Prueft ein bereits normalisiertes Angebot und gibt bei Erfolg den Platz frei.
     *
     * Was hier ankommt, ist keine Chat-Zeile mehr, sondern ein [ViewerCommand] - das Uebersetzen
     * von Text in Bedeutung hat der jeweilige Provider erledigt. Deshalb kennt dieses Tor weder
     * Twitch noch eine Befehlssyntax, und ein spaeterer Bits-Eingang laeuft ohne Aenderung
     * durch dieselbe Pruefung.
     *
     * Die Zeit steckt in [ViewerCommand.receivedAtMillis] statt in einer Uhr. Dadurch ist jeder
     * Ablauf - zwei Befehle in derselben Millisekunde, einer genau auf der Abstandsgrenze - ohne
     * Warten pruefbar.
     */
    fun admit(
        state: StreamGateState,
        command: ViewerCommand,
        world: StreamInteractionState,
        config: StreamCommandConfig = StreamCommandConfig()
    ): StreamGateDecision {
        val viewerId = command.viewerId
        val origin = command.origin
        val nowMillis = command.receivedAtMillis
        val slotId = when (val interaction = command.interaction) {
            is StreamInteraction.DropSafeSlot -> interaction.slotId
        }

        val lastForViewer = state.lastAcceptByViewer[viewerId]
        if (lastForViewer != null && nowMillis - lastForViewer < config.perViewerCooldownMillis) {
            return reject(state, viewerId, origin, slotId, StreamRejection.VIEWER_COOLDOWN, nowMillis, config)
        }
        val lastGlobal = state.lastGlobalAcceptMillis
        if (lastGlobal != null && nowMillis - lastGlobal < config.globalCooldownMillis) {
            return reject(state, viewerId, origin, slotId, StreamRejection.GLOBAL_COOLDOWN, nowMillis, config)
        }
        if (world.pending != null) {
            return reject(state, viewerId, origin, slotId, StreamRejection.IMPULSE_PENDING, nowMillis, config)
        }
        if (world.slots.getOrNull(slotId - 1) == null) {
            return reject(state, viewerId, origin, slotId, StreamRejection.SLOT_EMPTY, nowMillis, config)
        }

        val next = state.copy(
            lastGlobalAcceptMillis = nowMillis,
            lastAcceptByViewer = prune(state.lastAcceptByViewer, nowMillis, config) + (viewerId to nowMillis),
            log = append(state.log, StreamGateLogEntry(nowMillis, viewerId, origin, slotId, null), config)
        )
        return StreamGateDecision.Accepted(next, slotId, command)
    }

    private fun reject(
        state: StreamGateState,
        viewerId: String,
        origin: ExternalImpulseSource,
        slotId: Int?,
        reason: StreamRejection,
        nowMillis: Long,
        config: StreamCommandConfig
    ): StreamGateDecision {
        val entry = StreamGateLogEntry(nowMillis, viewerId, origin, slotId, reason)
        return StreamGateDecision.Rejected(state.copy(log = append(state.log, entry, config)), reason)
    }

    private fun append(
        log: List<StreamGateLogEntry>,
        entry: StreamGateLogEntry,
        config: StreamCommandConfig
    ): List<StreamGateLogEntry> = (log + entry).takeLast(config.logCapacity)

    /**
     * Wirft Zuschauer aus dem Gedaechtnis, deren Abstand ohnehin abgelaufen ist.
     *
     * Ohne das waechst die Abbildung mit jedem Namen, der je einen Befehl geschickt hat, und ein
     * Lauf ueber viele Stunden traegt am Ende ein Publikum mit sich herum, das laengst weg ist.
     */
    private fun prune(
        lastAcceptByViewer: Map<String, Long>,
        nowMillis: Long,
        config: StreamCommandConfig
    ): Map<String, Long> = lastAcceptByViewer.filterValues {
        nowMillis - it < config.perViewerCooldownMillis
    }
}
