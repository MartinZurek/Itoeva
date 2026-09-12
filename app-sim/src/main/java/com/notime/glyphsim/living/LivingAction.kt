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
     * Sich mit irgendetwas beschaeftigen - der Rueckfall ohne eigene Bedeutung.
     *
     * **Diese Handlung war einmal alle acht darunter.** Buch, Fokus, Kreativitaet, Achtsamkeit,
     * Bewegung, Fuersorge und Zuwendung liefen samt und sonders hierdurch, mit identischer
     * Wirkung: Ein Buch zu lesen und eine Tablette zu nehmen stillte dasselbe. Das war im Bild
     * nicht zu sehen und im Agenten trotzdem falsch - es gab dem Wesen einen Tag, in dem alles
     * dasselbe bedeutet.
     *
     * Was bleibt, ist der ehrliche Sammelposten fuer [AnimationType.GENERAL]: eine Regung ohne
     * naeher benannte Absicht.
     */
    PURSUE_INTEREST,

    /**
     * Lesen.
     *
     * Neugier zuerst, Wachstum dahinter: Ein Buch beantwortet Fragen, und dabei lernt man auch
     * etwas - nicht umgekehrt.
     */
    READ,

    /** Etwas herstellen. Wachstum und Freude zugleich; nichts kostet dabei so wenig Kraft. */
    CREATE,

    /**
     * Sich sammeln und an einer Sache bleiben.
     *
     * Die einzige freiwillige Handlung, die auch **belastet**: Konzentration zehrt an der Kraft
     * und macht keinen Spass. Genau daran unterscheidet sie sich von [CREATE], mit dem sie sich
     * sonst das Wachstum teilte.
     */
    CONCENTRATE,

    /** Zur Ruhe kommen. Behaglichkeit, ein wenig Kraft - und sehr wenig Zeit. */
    SETTLE,

    /**
     * Den Koerper bewegen.
     *
     * Macht Freude und tut gut, kostet Kraft **und macht hungrig**. Die letzte Wirkung ist die
     * interessanteste: Erst dadurch zieht eine Bewegung am Nachmittag ein Abendessen nach sich,
     * ohne dass irgendwo eine Regel "nach Sport kommt Essen" stuende.
     */
    MOVE_BODY,

    /**
     * Fuer sich selbst sorgen - Medizin, Pflege, das Noetige.
     *
     * **Stillt ausschliesslich Behaglichkeit, ausdruecklich keinen Spass.** Fuersorge ist kein
     * Vergnuegen; sie soll es im Modell auch nicht sein. Vorher lief sie durch
     * [PURSUE_INTEREST] und machte das Wesen damit vergnuegt und neugierig.
     */
    TEND_SELF,

    /**
     * **Losgehen und etwas sehen, das man noch nicht kennt** (NT-074).
     *
     * Die Handlung, die es vorher nicht gab - und deshalb wirkte das Wesen nie neugierig,
     * obwohl der Wert dafuer die ganze Zeit mitlief. Sie verlangt den Aufenthalt draussen: Was
     * man zu Hause finden kann, kennt man schon.
     *
     * Anders als [MOVE_BODY], das dem Koerper guttut, stillt sie vor allem den Kopf. Beide
     * fuehren hinaus, aus verschiedenen Gruenden - genau daran wird der Unterschied beim
     * Zusehen sichtbar.
     */
    EXPLORE,

    /**
     * Zuwendung zeigen, ohne dass jemand da ist.
     *
     * [INVITE_TO_PLAY] braucht ein anwesendes Gegenueber; diese hier nicht. Sie lindert das
     * soziale Beduerfnis nur teilweise - wer allein an jemanden denkt, ist nicht in
     * Gesellschaft gewesen, aber auch nicht ganz allein geblieben.
     */
    SHOW_AFFECTION,

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
        ),

        // ---- Die sieben benannten Beschaeftigungen (NT-072) ----
        //
        // **Warum sie einzeln dastehen.** Jede stillt etwas anderes, und genau das ist ihr
        // ganzer Zweck: Erst wenn Lesen die Neugier stillt und Bewegung hungrig macht, zieht das
        // eine das andere nach sich - ohne dass irgendwo eine Regel "nach Sport kommt Essen"
        // stuende. Ein einziger Sammelposten konnte das nicht, gleich wie gut seine Zahlen waren.
        //
        // Keine hat eine Voraussetzung ausser der Zeit, die sie kostet. Das ist Absicht: Ein
        // Wesen soll lesen koennen, wo es gerade ist. Wo etwas stattfindet, entscheidet die
        // sichtbare Routine ([com.notime.glyphsim.matrix.PlayRoutines]), nicht der Kern.
        Action(
            kind = ActionKind.READ,
            requirements = emptyList(),
            outcome = ActionOutcome(
                needRelief = mapOf(
                    NeedKind.CURIOSITY to 0.55,
                    NeedKind.GROWTH to 0.35,
                    NeedKind.FUN to 0.2,
                    NeedKind.ENERGY to -0.05
                ),
                minutes = 45,
                preferenceDelta = 0.02,
                rememberValence = 1
            ),
            effort = 0.05
        ),
        Action(
            kind = ActionKind.CREATE,
            requirements = emptyList(),
            outcome = ActionOutcome(
                needRelief = mapOf(
                    NeedKind.GROWTH to 0.5,
                    NeedKind.FUN to 0.4,
                    NeedKind.CURIOSITY to 0.25,
                    NeedKind.ENERGY to -0.08
                ),
                minutes = 60,
                preferenceDelta = 0.03,
                rememberValence = 1
            ),
            effort = 0.1
        ),
        Action(
            kind = ActionKind.CONCENTRATE,
            requirements = emptyList(),
            outcome = ActionOutcome(
                // Die einzige freiwillige Handlung, die auch belastet - siehe ActionKind.
                needRelief = mapOf(
                    NeedKind.GROWTH to 0.55,
                    NeedKind.CURIOSITY to 0.2,
                    NeedKind.ENERGY to -0.12,
                    NeedKind.FUN to -0.05
                ),
                minutes = 50,
                preferenceDelta = 0.02,
                rememberValence = 1
            ),
            effort = 0.18
        ),
        Action(
            kind = ActionKind.SETTLE,
            requirements = emptyList(),
            outcome = ActionOutcome(
                needRelief = mapOf(
                    NeedKind.COMFORT to 0.6,
                    NeedKind.ENERGY to 0.15,
                    NeedKind.FUN to 0.1
                ),
                minutes = 20,
                preferenceDelta = 0.02,
                rememberValence = 1
            )
        ),
        Action(
            kind = ActionKind.MOVE_BODY,
            // **Die einzige Handlung, die verlangt, draussen zu sein** (NT-073).
            //
            // Bis hierher war [LivingSite.OUTSIDE] ein Ort, an dem sich das Wesen gelegentlich
            // BEFAND - kein einziges Requirement im ganzen Kern nannte ihn. Damit war Draussensein
            // nie ein Ziel, sondern immer nur eine Nebenwirkung der Themenwahl, und genau deshalb
            // wirkte die Figur, als hocke sie im Zimmer. Der Ort stand in der Welt und in keinem
            // einzigen Plan.
            //
            // Mit dieser Zeile muss der Planer einen Weg nach draussen voranstellen, bevor er sich
            // bewegen kann - der Gang vor die Tuer ist ab jetzt ein sichtbarer Planschritt und
            // keine Kulissenfrage mehr.
            requirements = listOf(Requirement.At(LivingSite.OUTSIDE)),
            outcome = ActionOutcome(
                // Etwas grosszuegiger als drinnen, und das mit Absicht: Der Weg nach draussen
                // kostet jetzt Zeit, und ohne diesen Ausgleich waere Hinausgehen unterm Strich
                // teurer als Herumsitzen - die Aenderung haette dann das Gegenteil bewirkt.
                needRelief = mapOf(
                    NeedKind.FUN to 0.6,
                    NeedKind.COMFORT to 0.35,
                    NeedKind.ENERGY to -0.2,
                    NeedKind.HUNGER to -0.08
                ),
                minutes = 40,
                preferenceDelta = 0.02,
                rememberValence = 1
            ),
            effort = 0.12
        ),
        Action(
            kind = ActionKind.TEND_SELF,
            requirements = emptyList(),
            outcome = ActionOutcome(
                // Ausdruecklich NUR Behaglichkeit. Siehe ActionKind.TEND_SELF.
                needRelief = mapOf(NeedKind.COMFORT to 0.7),
                minutes = 10,
                rememberValence = 0
            ),
            effort = 0.02
        ),
        Action(
            kind = ActionKind.EXPLORE,
            // Draussen, aus demselben Grund wie MOVE_BODY (NT-073): Der Ort ist Teil der Sache.
            requirements = listOf(Requirement.At(LivingSite.OUTSIDE)),
            outcome = ActionOutcome(
                needRelief = mapOf(
                    NeedKind.CURIOSITY to 0.65,
                    NeedKind.FUN to 0.3,
                    NeedKind.GROWTH to 0.15,
                    NeedKind.ENERGY to -0.15,
                    NeedKind.HUNGER to -0.05
                ),
                minutes = 50,
                preferenceDelta = 0.03,
                rememberValence = 1
            ),
            effort = 0.1
        ),
        Action(
            kind = ActionKind.SHOW_AFFECTION,
            requirements = emptyList(),
            outcome = ActionOutcome(
                needRelief = mapOf(
                    NeedKind.SOCIAL to 0.35,
                    NeedKind.COMFORT to 0.25,
                    NeedKind.FUN to 0.15
                ),
                minutes = 25,
                preferenceDelta = 0.02,
                rememberValence = 1
            ),
            // **Der Ersatz darf nicht bequemer sein als die Sache selbst** (NT-074).
            //
            // Seit diese Handlung auch von sich aus gewaehlt werden kann (Planner, wenn niemand
            // da ist), entscheidet ihr Preis mit. Ohne Muehe war sie schneller und billiger als
            // jede Freizeitbeschaeftigung - ein Wesen allein zu Hause haette dann bei gleichem
            // Druck IMMER an jemanden gedacht, statt sich zu beschaeftigen, und die Anwesenheit
            // eines Freundes haette an der Entscheidung nichts mehr geaendert.
            //
            // Das ist auch inhaltlich richtig: An jemanden zu denken, der nicht da ist, ist die
            // schwerere Wahl, nicht die leichtere.
            effort = 0.12
        )
    ).associateBy { it.kind }

    /**
     * Die Handlungen, mit denen sich eine Freizeit- oder Entwicklungsphase fuellen laesst.
     *
     * Eine ausdrueckliche Menge und keine Ableitung aus den Zielen: Sie ist das, was der Planer
     * von aussen vorgeschlagen bekommen DARF. Arbeiten, essen und schlafen stehen bewusst nicht
     * darin - die entscheidet der Kern selbst aus der Lage, und ein Vorschlag von aussen soll
     * sie nicht umgehen koennen.
     */
    val FREE_TIME: Set<ActionKind> = setOf(
        ActionKind.PURSUE_INTEREST,
        ActionKind.EXPLORE,
        ActionKind.READ,
        ActionKind.CREATE,
        ActionKind.CONCENTRATE,
        ActionKind.SETTLE,
        ActionKind.MOVE_BODY,
        ActionKind.SHOW_AFFECTION
    )

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
                trustDelta = if (accepted) 0.06 else -0.03,
                closenessDelta = if (accepted) 0.08 else -0.02
            ),
            symbols = SymbolEffect(senderProfileId, intents, SymbolDirection.RECEIVE),
            eventKind = LivingEventKind.SYMBOLS_RECEIVED
        )
    )
}
