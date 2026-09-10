package com.notime.glyphsim.living

/**
 * **Der Zustand eines lebendigen Wesens** - alles, was es ueber einen Schritt hinaus mit sich
 * traegt.
 *
 * Unveraenderlich, wie alles hier: Ein Schritt nimmt einen Zustand und gibt einen neuen zurueck.
 * Das ist nicht Geschmack, sondern die Bedingung dafuer, dass sich derselbe Tag zweimal
 * identisch abspielen laesst - und dass ein Test einen Zustand von gestern neben einen von
 * heute legen kann.
 *
 * ## Was hier noch fehlt und wo es hingehoert
 *
 * `episodes` (verdichtete Erinnerungen), `relationships` und gelernter Geschmack sind Teil des
 * naechsten Schnitts (NT-063b). Sie kommen als weitere Felder DIESER Klasse dazu; der
 * Entscheidungsweg bleibt derselbe, weil [UtilitySelector] ohnehin den ganzen Agenten sieht
 * und nicht nur seine Beduerfnisse.
 */
data class AgentState(
    /** Dasselbe Profil wie in der bestehenden Welt - der Enum-Name der Spezies. */
    val profileId: String,
    /** Startbias, veraenderbar. Siehe [Personality]. */
    val personality: Personality,
    val needs: Needs,
    /** Was der Agent gerade will. Ueberlebt einen gescheiterten Plan. */
    val goal: GoalKind? = null,
    /** Der Weg dorthin, oder `null`, wenn noch keiner gefunden ist. */
    val plan: Plan? = null,
    /** Das letzte bedeutungsvolle Ereignis - fuer die Erklaerung, nicht als Protokoll. */
    val lastEvent: LivingEvent? = null
)

/**
 * **Was tatsaechlich passiert ist** - typisiert, nie als Text.
 *
 * Kein freier Satz, in keinem Feld. Das ist Absicht und keine Sparsamkeit: Ein Ereignis, das
 * einen deutschen Satz enthaelt, ist in einem Stream fuer die Haelfte der Zuschauer wertlos und
 * in einem Test nur noch per Zeichenkettenvergleich pruefbar. Die Anzeige macht daraus Sprache
 * oder Symbole; hier stehen Fakten.
 *
 * Was NICHT hierher gehoert: jeder Tick, jede Leerlaufbewegung, jedes Bild. Ein Ereignis
 * markiert eine Wendung - Ziel gewaehlt, Plan gefasst, Schritt getan, Schritt blockiert, Plan
 * verworfen.
 */
data class LivingEvent(
    val kind: LivingEventKind,
    /** Wann, in absoluten Minuten seit Simulationsbeginn - vergleichbar ueber Tage hinweg. */
    val atMinute: Int,
    val goal: GoalKind? = null,
    val action: ActionKind? = null,
    /** Bei [LivingEventKind.ACTION_BLOCKED] die Voraussetzung, an der es lag. */
    val blockedBy: Requirement? = null
)

enum class LivingEventKind {
    /** Ein Beduerfnis hat gewonnen; der Agent will etwas. */
    GOAL_CHOSEN,

    /** Zu diesem Ziel gibt es einen Weg. */
    PLAN_MADE,

    /** Ein Schritt ist getan. */
    ACTION_DONE,

    /**
     * Ein Schritt geht gerade nicht. **Das interessanteste Ereignis dieser Liste** - hier
     * entsteht aus einer Absicht ein Problem, und daraus eine Geschichte.
     */
    ACTION_BLOCKED,

    /** Der Plan wurde verworfen. Das Ziel bleibt. */
    PLAN_ABANDONED,

    /** Es gibt keinen Weg zum Ziel; der Agent wartet. */
    NO_PLAN,

    /** Das Ziel ist erreicht. */
    GOAL_REACHED,

    /** Nichts draengt. Ein Wesen darf auch einmal nichts vorhaben. */
    IDLE
}

/**
 * **Die Antwort auf die fuenf Fragen, die dieser Meilenstein beantworten koennen muss.**
 *
 * - Was will das Wesen gerade? -> [goal]
 * - Warum will es das? -> [ranking], aufgeschluesselt in Druck, Bias und Kosten
 * - Was ist sein Plan? -> [plan]
 * - Warum diese Handlung? -> [currentAction] als erster Schritt eben dieses Plans
 * - Was verhindert den Fortschritt? -> [blockedBy]
 *
 * Der Vertrag ist bewusst read-only und ohne Verweis auf veraenderliche Objekte: Eine Anzeige,
 * die diesen Schnappschuss liest, kann die Simulation nicht steuern. Das gilt spaeter genauso
 * fuer ein Stream-Overlay.
 */
data class AgentExplanation(
    val profileId: String,
    val strongestNeed: NeedKind,
    val goal: GoalKind?,
    val ranking: List<GoalScore>,
    val plan: List<ActionKind>,
    val currentAction: ActionKind?,
    val blockedBy: Requirement?,
    val lastEvent: LivingEvent?
)

/** Was ein einzelner Simulationsschritt hinterlaesst. */
data class StepResult(
    val agent: AgentState,
    val world: WorldState,
    /** Die Ereignisse dieses Schritts, in der Reihenfolge ihres Auftretens. */
    val events: List<LivingEvent>
) {
    fun explain(): AgentExplanation = LivingSimulation.explain(agent, world)
}

/**
 * **Ein Schritt Leben** - die einzige Stelle, an der Beduerfnisse, Ziel, Plan und Welt
 * zusammenkommen.
 *
 * ## Die Reihenfolge ist die Aussage
 *
 * 1. Steht kein Ziel oder ist es gestillt, waehlt [UtilitySelector] ein neues.
 * 2. Fehlt ein Plan, sucht [Planner] einen Weg.
 * 3. **Vor** dem naechsten Schritt werden dessen Voraussetzungen erneut geprueft - nicht beim
 *    Planen, sondern jetzt. Zwischen Planen und Tun kann die Welt sich geaendert haben.
 * 4. Traegt die Lage den Schritt nicht, faellt der Plan und das Ziel bleibt. Beim naechsten
 *    Schritt wird aus derselben Absicht ein anderer Weg abgeleitet.
 *
 * Punkt 3 und 4 sind der Unterschied zwischen einem lebendigen Wesen und einer Animation. Eine
 * fest verdrahtete Folge "arbeiten, einkaufen, essen" waere nach Punkt 2 fertig und wuerde
 * blind weiterlaufen, auch wenn der Laden inzwischen zu hat.
 *
 * ## Kein Zufall, keine Uhr, kein Text
 *
 * Alles hier ist eine reine Funktion von Agent und Welt. Zweimal derselbe Aufruf ergibt
 * zweimal dasselbe Ergebnis - eine Bedingung dafuer, dass sich ein Mehrtageslauf ueberhaupt
 * pruefen laesst.
 */
object LivingSimulation {

    /**
     * Ab welchem Restdruck ein Ziel als erreicht gilt.
     *
     * Nicht null: Ein Agent, der bis zur vollstaendigen Saettigung isst, isst drei Portionen
     * hintereinander und ist danach dieselbe Figur wie vorher, nur aermer. Ein Rest von 0,15
     * heisst "erst mal gut" und laesst Platz fuer alles andere.
     */
    const val SATISFIED_BELOW = 0.15

    fun step(agent: AgentState, world: WorldState): StepResult {
        val ereignisse = mutableListOf<LivingEvent>()
        var zustand = agent
        var welt = world

        // 1. Ziel pruefen und gegebenenfalls neu waehlen.
        val altesZiel = zustand.goal
        if (altesZiel == null || zustand.needs.pressure(altesZiel.drivenBy) < SATISFIED_BELOW) {
            if (altesZiel != null) {
                ereignisse += event(welt, LivingEventKind.GOAL_REACHED, goal = altesZiel)
            }
            val neu = UtilitySelector.choose(zustand, welt)
            zustand = zustand.copy(goal = neu, plan = null)
            if (neu == null) {
                // Nichts draengt. Der Agent tut nichts und die Zeit laeuft trotzdem weiter -
                // sonst stuende eine zufriedene Welt still und kein Beduerfnis wuechse je nach.
                val leerlauf = event(welt, LivingEventKind.IDLE)
                return StepResult(
                    agent = zustand.copy(
                        needs = zustand.needs.advanced(IDLE_MINUTES, zustand.personality),
                        lastEvent = leerlauf
                    ),
                    world = welt.advanced(IDLE_MINUTES),
                    events = ereignisse + leerlauf
                )
            }
            ereignisse += event(welt, LivingEventKind.GOAL_CHOSEN, goal = neu)
        }
        val ziel = zustand.goal ?: error("Ziel wurde soeben gesetzt")

        // 2. Plan besorgen, falls keiner steht.
        if (zustand.plan == null || zustand.plan?.isDone == true) {
            val plan = Planner.planFor(ziel, welt)
            if (plan == null) {
                val ohneWeg = event(welt, LivingEventKind.NO_PLAN, goal = ziel)
                return StepResult(
                    agent = zustand.copy(
                        plan = null,
                        needs = zustand.needs.advanced(IDLE_MINUTES, zustand.personality),
                        lastEvent = ohneWeg
                    ),
                    world = welt.advanced(IDLE_MINUTES),
                    events = ereignisse + ohneWeg
                )
            }
            zustand = zustand.copy(plan = plan)
            ereignisse += event(welt, LivingEventKind.PLAN_MADE, goal = ziel)
        }

        // 3. Naechsten Schritt pruefen - jetzt, nicht beim Planen.
        val plan = zustand.plan ?: error("Plan wurde soeben gesetzt")
        val schritt = plan.next ?: error("Ein fertiger Plan wurde oben ersetzt")
        val hindernis = schritt.blockedBy(welt)
        if (hindernis != null) {
            val blockiert = event(
                welt, LivingEventKind.ACTION_BLOCKED,
                goal = ziel, action = schritt.kind, blockedBy = hindernis
            )
            val verworfen = event(welt, LivingEventKind.PLAN_ABANDONED, goal = ziel)
            // Der Plan faellt, das Ziel bleibt. Der naechste Schritt leitet aus derselben
            // Absicht einen neuen Weg ab - oder meldet, dass es keinen gibt.
            return StepResult(
                agent = zustand.copy(
                    plan = null,
                    needs = zustand.needs.advanced(IDLE_MINUTES, zustand.personality),
                    lastEvent = verworfen
                ),
                world = welt.advanced(IDLE_MINUTES),
                events = ereignisse + blockiert + verworfen
            )
        }

        // 4. Ausfuehren.
        welt = schritt.applyTo(welt)
        val getan = event(welt, LivingEventKind.ACTION_DONE, goal = ziel, action = schritt.kind)
        zustand = zustand.copy(
            needs = zustand.needs
                .advanced(schritt.outcome.minutes, zustand.personality)
                .relieved(schritt.outcome.needRelief),
            plan = plan.advanced(),
            lastEvent = getan
        )
        return StepResult(zustand, welt, ereignisse + getan)
    }

    /** [count] Schritte am Stueck - fuer Mehrtageslaeufe im Test. */
    fun run(agent: AgentState, world: WorldState, count: Int): StepResult {
        var ergebnis = StepResult(agent, world, emptyList())
        val alle = mutableListOf<LivingEvent>()
        repeat(count) {
            ergebnis = step(ergebnis.agent, ergebnis.world)
            alle += ergebnis.events
        }
        return ergebnis.copy(events = alle)
    }

    /** Der Schnappschuss fuer Anzeige und spaeteres Overlay. Veraendert nichts. */
    fun explain(agent: AgentState, world: WorldState): AgentExplanation = AgentExplanation(
        profileId = agent.profileId,
        strongestNeed = agent.needs.strongest(),
        goal = agent.goal,
        ranking = UtilitySelector.rank(agent, world),
        plan = agent.plan?.kinds ?: emptyList(),
        currentAction = agent.plan?.next?.kind,
        blockedBy = agent.plan?.next?.blockedBy(world),
        lastEvent = agent.lastEvent
    )

    /**
     * Wie lange ein Schritt dauert, in dem nichts getan wurde.
     *
     * Auch Warten und Scheitern kosten Zeit. Ohne das liefe die Simulation bei einem
     * unloesbaren Ziel unendlich oft durch dieselbe Minute.
     */
    private const val IDLE_MINUTES = 30

    private fun event(
        world: WorldState,
        kind: LivingEventKind,
        goal: GoalKind? = null,
        action: ActionKind? = null,
        blockedBy: Requirement? = null
    ) = LivingEvent(kind, world.absoluteMinute, goal, action, blockedBy)
}
