package com.notime.glyphsim.stream

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.living.ActionCatalog
import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.living.GoalInfluence
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.matrix.PreparedLivingRoutine
import com.notime.glyphsim.ui.ACTION_SLOT_COUNT
import com.notime.glyphsim.ui.SavedAction

/**
 * Herkunft eines Impulses.
 *
 * Die Spielschicht liest diesen Wert nie. Er steht im Eingangsprotokoll und macht spaeter den
 * Unterschied zwischen kostenlosen und bezahlten Quellen sichtbar, **ohne** dass dafuer an der
 * Auswahl oder am Living Agent etwas geaendert werden muesste: Eine neue Quelle ist ein neuer
 * Eintrag hier und ein neuer [StreamInteractionProvider], mehr nicht.
 *
 * Bezahlrechte und Plattformkonten gehoeren weiterhin nicht hierher - nur die Angabe, welcher
 * Eingang ein Angebot erzeugt hat.
 */
enum class ExternalImpulseSource {
    /** Ein Tippen auf einen Platz im Client selbst - der aelteste Weg, zum Pruefen am Geraet. */
    LOCAL_VIEWER_SIMULATOR,

    /** Der eingebaute Demo-/Testeingang: erzeugte Zeilen ohne jede Netzverbindung. */
    LOCAL_TEST_PROVIDER,

    /** Ein kostenloser Befehl aus dem Twitch-Chat. Keine Bits, kein Sub, kein Konto noetig. */
    TWITCH_CHAT_FREE
    // Spaeter, ohne Aenderung an der Spielschicht: TWITCH_BITS, TWITCH_SUBSCRIPTION.
}

/**
 * Sprachunabhaengiger Eingang an der Grenze der laufenden Itoeva-Instanz.
 *
 * [atMinute] ist Simulationszeit statt Wanduhr. Dadurch bleibt derselbe Ablauf auch im
 * Zeitraffer pruefbar, und ein spaeteres Backend muss keine Agentenzeit erfinden.
 */
data class ExternalImpulse(
    val impulseId: String,
    val savedSlotId: Int,
    val reminderId: Long,
    val occurrenceId: Long,
    val animationType: AnimationType,
    val source: ExternalImpulseSource,
    val atMinute: Int
) {
    init {
        require(savedSlotId in 1..ACTION_SLOT_COUNT) { "savedSlotId out of range" }
        require(atMinute >= 0) { "atMinute must not be negative" }
    }
}

/** Der kleine Zustand zwischen Save-Slots, lokalem Viewer und Living Agent. */
internal data class StreamInteractionState(
    val slots: List<SavedAction?> = List(ACTION_SLOT_COUNT) { null },
    val pending: ExternalImpulse? = null,
    val latest: ExternalImpulse? = null
) {
    init {
        require(slots.size == ACTION_SLOT_COUNT) { "stream mode needs exactly four slots" }
    }
}

internal sealed interface StreamSelection {
    data class Accepted(val state: StreamInteractionState, val impulse: ExternalImpulse) :
        StreamSelection

    data object Missing : StreamSelection
    data object Busy : StreamSelection
}

/**
 * Twitch-neutrale Regeln fuer den lokalen PoC.
 *
 * Der Controller setzt kein Ziel und fuehrt keine Handlung aus. Er liefert nur einen begrenzten
 * [GoalInfluence], den dieselbe Utility-Auswahl wie jeden anderen Anreiz ueberstimmen darf.
 */
internal object StreamInteractions {

    fun autoSave(
        streamMode: Boolean,
        state: StreamInteractionState,
        action: SavedAction
    ): StreamInteractionState {
        if (!streamMode || !isPublic(action)) return state
        if (state.slots.any { it?.occurrenceId == action.occurrenceId }) return state
        val index = state.slots.indexOfFirst { it == null }
        if (index < 0) return state
        return state.copy(slots = state.slots.toMutableList().also { it[index] = action })
    }

    fun select(
        state: StreamInteractionState,
        slotId: Int,
        atMinute: Int,
        source: ExternalImpulseSource = ExternalImpulseSource.LOCAL_VIEWER_SIMULATOR
    ): StreamSelection {
        if (state.pending != null) return StreamSelection.Busy
        val index = slotId - 1
        val saved = state.slots.getOrNull(index) ?: return StreamSelection.Missing
        val type = saved.animationType ?: return StreamSelection.Missing
        if (!isPublic(saved)) return StreamSelection.Missing
        val impulse = ExternalImpulse(
            impulseId = "${saved.occurrenceId}:$slotId:$atMinute",
            savedSlotId = slotId,
            reminderId = saved.reminderId,
            occurrenceId = saved.occurrenceId,
            animationType = type,
            source = source,
            atMinute = atMinute
        )
        return StreamSelection.Accepted(
            state.copy(pending = impulse, latest = impulse),
            impulse
        )
    }

    /** Ein kleiner Vorschlag fuer die normale Zielwahl, keine Aenderung am AgentState. */
    fun influenceFor(impulse: ExternalImpulse?): GoalInfluence? = impulse?.let {
        GoalInfluence(goalFor(it.animationType))
    }

    /**
     * Erst eine wirklich vorbereitete Kernhandlung nimmt den Impuls an.
     *
     * Ein anderes dringendes Ziel, ein fehlender Nachbar oder ein Hindernis laesst den Slot
     * unveraendert liegen; der Viewer hat dann sichtbar angestupst, aber nichts erzwungen.
     */
    fun wasHandled(
        impulse: ExternalImpulse,
        prepared: PreparedLivingRoutine
    ): Boolean = when (impulse.animationType) {
        AnimationType.DRINK -> ActionKind.EAT in prepared.completedActions
        AnimationType.WORK -> ActionKind.WORK in prepared.completedActions
        AnimationType.REST,
        AnimationType.SLEEP -> ActionKind.REST in prepared.completedActions
        AnimationType.LOVE -> prepared.completedActions.any {
            it == ActionKind.INVITE_TO_PLAY ||
                it == ActionKind.RESPOND_TO_INVITE ||
                it == ActionKind.RECEIVE_RESPONSE
        }
        // **Nicht mehr nur PURSUE_INTEREST** (NT-072). Seit jede Beschaeftigung ihre eigene
        // Handlung hat, liefert ein Buch-Impuls READ und ein Bewegungs-Impuls MOVE_BODY. Bliebe
        // hier die alte Abfrage stehen, gaelte ein Buch-Anstoss nie als angenommen - der Platz
        // wuerde nie geleert, und der Zuschauer saehe seinen Wunsch auf ewig im Bild stehen.
        else -> prepared.topic == impulse.animationType &&
            prepared.completedActions.any { it in ActionCatalog.FREE_TIME }
    }

    fun clearHandled(
        state: StreamInteractionState,
        impulse: ExternalImpulse
    ): StreamInteractionState {
        if (state.pending?.impulseId != impulse.impulseId) return state
        val index = impulse.savedSlotId - 1
        if (state.slots.getOrNull(index)?.occurrenceId != impulse.occurrenceId) return state
        return state.copy(
            slots = state.slots.toMutableList().also { it[index] = null },
            pending = null
        )
    }

    private fun goalFor(type: AnimationType): GoalKind = when (type) {
        AnimationType.DRINK -> GoalKind.GET_FOOD
        AnimationType.WORK -> GoalKind.EARN_MONEY
        AnimationType.REST,
        AnimationType.SLEEP -> GoalKind.REST
        AnimationType.BOOK,
        AnimationType.FOCUS,
        AnimationType.CREATIVITY -> GoalKind.DEVELOP
        AnimationType.LOVE -> GoalKind.CONNECT_WITH
        else -> GoalKind.HAVE_FUN
    }

    /** Keine medizinischen oder frei beschrifteten privaten Inhalte im oeffentlichen Bild. */
    private fun isPublic(action: SavedAction): Boolean =
        action.animationType != null &&
            action.animationType != AnimationType.MEDICINE &&
            action.libraryAnimationLabel == null &&
            action.frames.isNotEmpty()
}
