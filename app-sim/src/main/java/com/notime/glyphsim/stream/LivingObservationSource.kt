package com.notime.glyphsim.stream

import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.living.Episode
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.GoalScore
import com.notime.glyphsim.living.LivingEvent
import com.notime.glyphsim.living.LivingEventKind
import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.living.RelationshipState
import com.notime.glyphsim.living.Requirement
import com.notime.glyphsim.living.StepResult

/**
 * Der sprachunabhaengige Moment, den eine spaetere Anzeige lesen darf.
 *
 * Die Felder beantworten direkt die Fragen eines Zuschauers, ohne Text zu erfinden und ohne
 * Zugriff auf den veraenderbaren Agentenzustand zu geben. [reason] ist nur die Bewertung des
 * tatsaechlich gewaehlten Ziels; eine Anzeige muss sie nicht aus der ganzen Rangliste erraten.
 */
data class LivingObservation(
    val profileId: String,
    val atMinute: Int,
    val strongestNeed: NeedKind,
    val goal: GoalKind?,
    val reason: GoalScore?,
    val plan: List<ActionKind>,
    val currentAction: ActionKind?,
    val blockedBy: Requirement?,
    val influentialEpisodes: List<Episode>,
    val relationships: Map<String, RelationshipState>,
    val importantEvent: LivingEvent?,
    val recentEvents: List<LivingEvent>
)

/** Eine Anzeige kann beobachten, aber weder Agent noch Welt steuern. */
fun interface LivingObservationSource {
    fun current(profileId: String): LivingObservation?
}

/**
 * Begrenzt den Ereignisstrom und baut daraus unveraenderliche Momentaufnahmen.
 *
 * Das Journal kennt keine Uhr und keine Plattform. Es nimmt nur bereits abgeschlossene
 * [StepResult]s entgegen. Rohframes und Leerlauf-Ticks werden nie Teil des Stream-Vertrags.
 */
internal class LivingObservationJournal(
    private val eventLimit: Int = DEFAULT_EVENT_LIMIT
) : LivingObservationSource {

    init {
        require(eventLimit > 0) { "eventLimit must be positive" }
    }

    private val lock = Any()
    private val observations = mutableMapOf<String, LivingObservation>()

    override fun current(profileId: String): LivingObservation? = synchronized(lock) {
        observations[profileId]
    }

    fun record(result: StepResult) {
        synchronized(lock) {
            val previous = observations[result.agent.profileId]
            val persisted = if (previous == null) {
                result.agent.episodes.map(Episode::event) + listOfNotNull(result.agent.lastEvent)
            } else {
                emptyList()
            }
            val recent = appendDistinct(
                previous?.recentEvents.orEmpty() + persisted,
                result.events.filterNot { it.kind == LivingEventKind.IDLE }
            ).takeLast(eventLimit)
            val explanation = result.explain()
            val interrupted = explanation.lastEvent?.kind in INTERRUPTED_KINDS
            val rememberedBlocker = if (interrupted) {
                recent.asReversed().firstOrNull {
                    it.kind == LivingEventKind.ACTION_BLOCKED && it.goal == explanation.goal
                }?.blockedBy
            } else {
                null
            }
            observations[result.agent.profileId] = LivingObservation(
                profileId = explanation.profileId,
                atMinute = result.world.absoluteMinute,
                strongestNeed = explanation.strongestNeed,
                goal = explanation.goal,
                reason = explanation.ranking.firstOrNull { it.goal == explanation.goal },
                plan = explanation.plan.toList(),
                currentAction = explanation.currentAction,
                blockedBy = explanation.blockedBy ?: rememberedBlocker,
                influentialEpisodes = explanation.influentialEpisodes.toList(),
                relationships = explanation.relationships.toMap(),
                importantEvent = recent.asReversed().firstOrNull(::isImportant),
                recentEvents = recent.toList()
            )
        }
    }

    /** Nur direkt aufeinanderfolgende Wiederholungen beim ersten Restore entfernen. */
    private fun appendDistinct(
        existing: List<LivingEvent>,
        incoming: List<LivingEvent>
    ): List<LivingEvent> = (existing + incoming).fold(emptyList()) { events, event ->
        if (events.lastOrNull() == event) events else events + event
    }

    private fun isImportant(event: LivingEvent): Boolean = when (event.kind) {
        LivingEventKind.ACTION_BLOCKED,
        LivingEventKind.NO_PLAN,
        LivingEventKind.GOAL_CHOSEN,
        LivingEventKind.GOAL_REACHED,
        LivingEventKind.SYMBOLS_SENT,
        LivingEventKind.SYMBOLS_RECEIVED -> true
        LivingEventKind.ACTION_DONE -> event.action != ActionKind.TRAVEL &&
            event.action != ActionKind.INSPECT_FOOD
        LivingEventKind.PLAN_MADE,
        LivingEventKind.PLAN_ABANDONED,
        LivingEventKind.IDLE -> false
    }

    private companion object {
        const val DEFAULT_EVENT_LIMIT = 32
        val INTERRUPTED_KINDS = setOf(
            LivingEventKind.ACTION_BLOCKED,
            LivingEventKind.PLAN_ABANDONED,
            LivingEventKind.NO_PLAN
        )
    }
}

/** Prozessweite Quelle fuer die laufende lokale Welt; oeffentlich ist nur der Lesevertrag. */
object LivingObservationFeed : LivingObservationSource {
    private val journal = LivingObservationJournal()

    override fun current(profileId: String): LivingObservation? = journal.current(profileId)

    internal fun record(result: StepResult) = journal.record(result)
}
