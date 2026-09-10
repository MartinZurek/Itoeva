package com.notime.glyphsim.living

/**
 * **Was ein Wesen tun kann** - ein kleiner Satz tief verbundener Handlungen mit benannten Voraussetzungen und einer
 * benannten Wirkung.
 *
 * ## Die Trennung, die diese Datei traegt
 *
 * > Ein Ziel sagt, WAS erreicht werden soll. Eine Handlung sagt, WIE - und was sie kostet.
 *
 * Beide bleiben getrennt, auch wenn heute je Ziel nur ein Weg existiert. Sobald es einen
 * zweiten gibt (essen gehen statt kochen, arbeiten statt tauschen), waehlt der Planer zwischen
 * Handlungen, ohne dass am Ziel eine Zeile zu aendern ist. Waeren beide eins, hiesse jeder neue
 * Weg ein neues Ziel - und aus sieben Zielen wuerden dreissig.
 *
 * ## Die Wirkung ist die Erweiterungsstelle
 *
 * [ActionOutcome] traegt Muenzen, Vorrat, Ort, Zeit, Beduerfnislinderung, Erinnerung,
 * Geschmack, Beziehung und Symbolbedeutung. Genau diese gemeinsame Form verhindert
 * Sonderwege fuer soziale Handlungen; [Action.applyTo] bleibt die einzige Rechnung.
 */
enum class ActionKind {
    /**
     * In den Vorrat sehen.
     *
     * **Heute kostet sie nur eine Minute und liefert ein Ereignis** - sie beschafft keine
     * Information, denn der Planer sieht den Vorrat ohnehin. Sie steht trotzdem im Plan, und
     * zwar aus zwei Gruenden: Sie macht die Ereignisfolge lesbar ("er sieht nach - nichts da"),
     * und sie ist die Stelle, an der spaeter unvollstaendiges Weltwissen eintritt. Ein Agent,
     * der seinen Vorrat erst nachsehen MUSS, braucht dann keine neue Handlung, sondern eine
     * andere Wirkung an dieser.
     */
    INSPECT_FOOD,

    /** Eine Portion essen. */
    EAT,

    /** Eine Schicht arbeiten. Bringt Lohn, kostet Zeit und Kraft. */
    WORK,

    /** Vorrat kaufen. */
    BUY_FOOD,

    /** Ausruhen. */
    REST,

    /**
     * Sich mit etwas beschaeftigen - spielen, Musik hoeren, lernen.
     *
     * Eine Handlung fuer Freizeit UND Entwicklung, weil der Unterschied heute keiner waere: Was
     * sie stillt, entscheidet das Ziel, aus dem sie kommt. Sobald Faehigkeiten existieren,
     * trennt sich das von selbst.
     */
    PURSUE_INTEREST,

    /** Eine sprachunabhaengige Einladung an ein anwesendes Wesen senden. */
    INVITE_TO_PLAY,

    /** Auf eine Einladung anhand des eigenen Zustands antworten. */
    RESPOND_TO_INVITE,

    /** Die Antwort der anderen Seite wahrnehmen und fuer die Beziehung behalten. */
    RECEIVE_RESPONSE,

    /**
     * Zu einem anderen Ort gehen.
     *
     * Eine eigene Handlung und kein stiller Sprung: Wege kosten Zeit, koennen scheitern (der
     * Laden ist zu) und sind in der Ereignisfolge zu sehen. Ein Plan, der den Agenten
     * unbemerkt versetzt, waere in der Anzeige spaeter nicht erklaerbar.
     */
    TRAVEL
}

/**
 * Was eine Handlung veraendert.
 *
 * Alles ist eine Differenz und kein neuer Zustand: Nur so laesst sich dieselbe Handlung auf
 * jede Weltlage anwenden, ohne dass sie die Welt kennen muss.
 */
enum class SymbolDirection { SEND, RECEIVE }

data class SymbolEffect(
    val counterpartProfileId: String,
    val intents: Set<SymbolicIntent>,
    val direction: SymbolDirection
)

data class RelationshipEffect(
    val counterpartProfileId: String,
    val trustDelta: Double,
    val closenessDelta: Double
)

/** Ergebnis genau eines angewandten Schritts, einschliesslich semantischer Ausgabe. */
data class AppliedAction(
    val agent: AgentState,
    val world: WorldState,
    val event: LivingEvent,
    val message: SymbolicMessage? = null
)

data class ActionOutcome(
    val coinsDelta: Int = 0,
    val portionsDelta: Int = 0,
    /** Positiv erleichtert, negativ belastet - Arbeit macht muede. */
    val needRelief: Map<NeedKind, Double> = emptyMap(),
    /** Wohin die Handlung den Agenten bringt, oder `null`, wenn sie ihn stehen laesst. */
    val movesTo: LivingSite? = null,
    val minutes: Int = 0,
    /** Langsame Veraenderung des Geschmacks fuer das Ziel dieser Handlung. */
    val preferenceDelta: Double = 0.0,
    /** `null` bedeutet: zu klein fuer eine Episode; sonst emotionale Wertung -1, 0 oder 1. */
    val rememberValence: Int? = null,
    val relationshipEffect: RelationshipEffect? = null,
    val symbols: SymbolEffect? = null,
    /** Soziale Schritte bleiben im Ereignisstrom von normalen Handlungen unterscheidbar. */
    val eventKind: LivingEventKind = LivingEventKind.ACTION_DONE
)

/**
 * Eine Handlung mit allem, was der Planer ueber sie wissen muss.
 *
 * [requirements] sind geordnet: Die erste nicht erfuellte ist die, die gemeldet wird. Ein
 * Agent, der am falschen Ort steht UND kein Geld hat, meldet den Ort - er kann sich nur um
 * eines zuerst kuemmern, und die Reihenfolge sagt, um welches.
 */
data class Action(
    val kind: ActionKind,
    val requirements: List<Requirement>,
    val outcome: ActionOutcome,
    /**
     * Was die Handlung an Muehe kostet, jenseits von Zeit und Geld - fliesst als Abzug in die
     * Zielbewertung ein. Arbeit ist anstrengend, in den Vorrat sehen nicht.
     */
    val effort: Double = 0.0
) {

    /** Die erste Voraussetzung, die diese Welt nicht erfuellt - oder `null`. */
    fun blockedBy(world: WorldState): Requirement? = requirements.firstOrNull { !world.has(it) }

    fun isPossible(world: WorldState): Boolean = blockedBy(world) == null

    /**
     * Agent und Welt nach dieser Handlung.
     *
     * Ruft man das mit einer Welt, die [blockedBy] meldet, kommt trotzdem ein Ergebnis heraus -
     * die Pruefung gehoert dem Aufrufer ([LivingSimulation]). Diese eine Rechnung wendet ALLE
     * Felder von [ActionOutcome] an: Welt, Beduerfnisse, Geschmack, Beziehung, Episode und
     * Symbole. Eine neue Handlung bekommt deshalb keinen zweiten Wirkungsweg.
     */
    fun applyTo(agent: AgentState, world: WorldState, goal: GoalKind): AppliedAction {
        val nextWorld = world.advanced(outcome.minutes).copy(
            site = outcome.movesTo ?: world.site,
            coins = (world.coins + outcome.coinsDelta).coerceAtLeast(0),
            portions = (world.portions + outcome.portionsDelta).coerceAtLeast(0)
        )
        val symbolEffect = outcome.symbols
        val relationshipEffect = outcome.relationshipEffect
        val event = LivingEvent(
            kind = outcome.eventKind,
            atMinute = nextWorld.absoluteMinute,
            goal = goal,
            action = kind,
            counterpartProfileId = symbolEffect?.counterpartProfileId
                ?: relationshipEffect?.counterpartProfileId,
            intents = symbolEffect?.intents ?: emptySet()
        )
        val nextRelationships = if (relationshipEffect == null) {
            agent.relationships
        } else {
            val previous = agent.relationships[relationshipEffect.counterpartProfileId]
                ?: RelationshipState()
            agent.relationships + (
                relationshipEffect.counterpartProfileId to previous.changedBy(
                    relationshipEffect.trustDelta,
                    relationshipEffect.closenessDelta,
                    event
                )
            )
        }
        val nextPreferences = if (outcome.preferenceDelta == 0.0) {
            agent.learnedPreferences
        } else {
            val learned = (agent.learnedPreferences[goal] ?: 0.0) + outcome.preferenceDelta
            agent.learnedPreferences + (goal to learned.coerceIn(-0.15, 0.15))
        }
        val nextEpisodes = outcome.rememberValence?.let { valence ->
            (agent.episodes + Episode.from(event, valence)).takeLast(AgentState.MAX_EPISODES)
        } ?: agent.episodes
        val nextAgent = agent.copy(
            needs = agent.needs
                .advanced(outcome.minutes, agent.personality)
                .relieved(outcome.needRelief),
            learnedPreferences = nextPreferences,
            relationships = nextRelationships,
            episodes = nextEpisodes,
            lastEvent = event
        )
        val message = symbolEffect
            ?.takeIf { it.direction == SymbolDirection.SEND }
            ?.let {
                SymbolicMessage(
                    senderProfileId = agent.profileId,
                    recipientProfileId = it.counterpartProfileId,
                    intents = it.intents,
                    atMinute = nextWorld.absoluteMinute
                )
            }
        return AppliedAction(nextAgent, nextWorld, event, message)
    }
}

/**
 * Der feste Satz Handlungen dieses Schnitts.
 *
 * Die Zahlen sind absichtlich dieselben wie in der bestehenden Welt
 * ([com.notime.glyphsim.matrix.PlayWallet]: Lohn 2, Einkauf 2;
 * [com.notime.glyphsim.matrix.PlayPantry]: voll 3), damit der Runtime-Adapter spaeter nichts
 * umrechnet und niemand zwei Wirtschaftssysteme gegeneinander pflegen muss.
 */
object ActionCatalog {

    const val WAGE = 2
    const val GROCERY_COST = 2
    const val GROCERY_PORTIONS = 3
    const val WORK_MINUTES = 180
    const val TRAVEL_MINUTES = 20

    private val actions: Map<ActionKind, Action> = listOf(
        Action(
            kind = ActionKind.INSPECT_FOOD,
            requirements = listOf(Requirement.At(LivingSite.HOME)),
            outcome = ActionOutcome(minutes = 1)
        ),
        Action(
            kind = ActionKind.EAT,
            requirements = listOf(Requirement.At(LivingSite.HOME), Requirement.Portions(1)),
            outcome = ActionOutcome(
                portionsDelta = -1,
                needRelief = mapOf(NeedKind.HUNGER to 0.7, NeedKind.COMFORT to 0.2),
                minutes = 20,
                rememberValence = 1
            )
        ),
        Action(
            kind = ActionKind.WORK,
            requirements = listOf(
                Requirement.SiteOpen(LivingSite.WORKPLACE),
                Requirement.At(LivingSite.WORKPLACE)
            ),
            outcome = ActionOutcome(
                coinsDelta = WAGE,
                // Arbeit stillt nichts, sie kostet: Kraft und ein wenig Laune. Das ist der
                // Grund, warum ein satter Agent nicht den ganzen Tag arbeitet, obwohl Muenzen
                // immer nuetzlich waeren.
                needRelief = mapOf(NeedKind.ENERGY to -0.25, NeedKind.FUN to -0.1),
                minutes = WORK_MINUTES,
                rememberValence = 0
            ),
            effort = 0.35
        ),
        Action(
            kind = ActionKind.BUY_FOOD,
            requirements = listOf(
                Requirement.SiteOpen(LivingSite.MARKET),
                Requirement.At(LivingSite.MARKET),
                Requirement.Coins(GROCERY_COST)
            ),
            outcome = ActionOutcome(
                coinsDelta = -GROCERY_COST,
                portionsDelta = GROCERY_PORTIONS,
                minutes = 30,
                rememberValence = 1
            ),
            effort = 0.1
        ),
        Action(
            kind = ActionKind.REST,
            requirements = listOf(Requirement.At(LivingSite.HOME)),
            outcome = ActionOutcome(
                needRelief = mapOf(NeedKind.ENERGY to 0.6, NeedKind.COMFORT to 0.3),
                minutes = 120,
                rememberValence = 1
            )
        ),
        Action(
            kind = ActionKind.PURSUE_INTEREST,
            requirements = emptyList(),
            outcome = ActionOutcome(
                needRelief = mapOf(
                    NeedKind.FUN to 0.5,
                    NeedKind.CURIOSITY to 0.4,
                    NeedKind.GROWTH to 0.3,
                    NeedKind.ENERGY to -0.05
                ),
                minutes = 60,
                preferenceDelta = 0.02,
                rememberValence = 1
            )
        )
    ).associateBy { it.kind }

    operator fun get(kind: ActionKind): Action = actions.getValue(kind)

    /**
     * Der Weg an einen anderen Ort.
     *
     * Wird gebaut statt nachgeschlagen, weil Voraussetzung und Wirkung vom Ziel abhaengen -
     * die einzige Handlung, bei der das so ist. [LivingSite.HOME] hat bewusst keine
     * Offen-Voraussetzung: Nach Hause kommt man immer.
     */
    fun travelTo(site: LivingSite): Action = Action(
        kind = ActionKind.TRAVEL,
        requirements = if (site == LivingSite.HOME) emptyList() else listOf(Requirement.SiteOpen(site)),
        outcome = ActionOutcome(
            movesTo = site,
            needRelief = mapOf(NeedKind.ENERGY to -0.05),
            minutes = TRAVEL_MINUTES
        ),
        effort = 0.05
    )

    fun inviteToPlay(targetProfileId: String): Action = Action(
        kind = ActionKind.INVITE_TO_PLAY,
        requirements = listOf(Requirement.Near(targetProfileId)),
        outcome = ActionOutcome(
            needRelief = mapOf(NeedKind.SOCIAL to 0.1),
            minutes = 5,
            rememberValence = 0,
            relationshipEffect = RelationshipEffect(targetProfileId, trustDelta = 0.0, closenessDelta = 0.01),
            symbols = SymbolEffect(
                targetProfileId,
                setOf(SymbolicIntent.PLAY, SymbolicIntent.QUESTION),
                SymbolDirection.SEND
            ),
            eventKind = LivingEventKind.SYMBOLS_SENT
        )
    )

    fun respondToInvite(
        senderProfileId: String,
        intents: Set<SymbolicIntent>,
        accepted: Boolean
    ): Action = Action(
        kind = ActionKind.RESPOND_TO_INVITE,
        requirements = listOf(Requirement.Near(senderProfileId)),
        outcome = ActionOutcome(
            needRelief = if (accepted) mapOf(NeedKind.SOCIAL to 0.4) else emptyMap(),
            minutes = 2,
            preferenceDelta = if (accepted) 0.02 else -0.01,
            rememberValence = if (accepted) 1 else -1,
            relationshipEffect = RelationshipEffect(
                senderProfileId,
                trustDelta = if (accepted) 0.06 else -0.03,
                closenessDelta = if (accepted) 0.08 else -0.02
            ),
            symbols = SymbolEffect(senderProfileId, intents, SymbolDirection.SEND),
            eventKind = LivingEventKind.SYMBOLS_SENT
        )
    )

    fun receiveResponse(
        senderProfileId: String,
        intents: Set<SymbolicIntent>,
        accepted: Boolean
    ): Action = Action(
        kind = ActionKind.RECEIVE_RESPONSE,
        requirements = listOf(Requirement.Near(senderProfileId)),
        outcome = ActionOutcome(
            needRelief = if (accepted) mapOf(NeedKind.SOCIAL to 0.5) else emptyMap(),
            minutes = 1,
            preferenceDelta = if (accepted) 0.02 else -0.01,
            rememberValence = if (accepted) 1 else -1,
            relationshipEffect = RelationshipEffect(
                senderProfileId,
                affinityDelta = if (accepted) 0.08 else -0.04
            ),
            symbols = SymbolEffect(senderProfileId, intents, SymbolDirection.RECEIVE),
            eventKind = LivingEventKind.SYMBOLS_RECEIVED
        )
    )
}
