package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.living.AgentState
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.LivingSimulation
import com.notime.glyphsim.living.LivingSite
import com.notime.glyphsim.living.Requirement
import com.notime.glyphsim.living.WorldState

/**
 * Der Zustand EINES Einwohners - Wesen und eigene Welt, nie geteilt.
 *
 * Zwei getrennte Welten und nicht eine gemeinsame: [WorldState] traegt Muenzen und Vorrat, und
 * die gehoeren dem jeweiligen Wesen. Eine gemeinsame Welt haette die Verkaufskraft aus dem
 * Geldbeutel des Parkgastes bezahlen lassen (siehe NT-086, wo derselbe Fehler beim Gast lauerte).
 */
data class ResidentState(
    val agent: AgentState,
    val world: WorldState
)

/**
 * Was die Anzeige von einem Einwohner sehen darf - **lesend und vollstaendig abgeleitet**.
 *
 * Kein Zugriff auf [AgentState]: Wer den ganzen Kern bekommt, faengt irgendwann an, ihn von der
 * Oberflaeche aus zu veraendern, und dann gibt es zwei Stellen, an denen Leben entsteht. Hier
 * steht genau so viel, wie ein Beobachter beantworten koennen muss - wer, wo, was und warum
 * gerade nicht.
 */
data class ResidentSnapshot(
    val profileId: String,
    val role: ResidentRole,
    val species: AvatarSpecies,
    /**
     * Der sichtbare Ort, oder `null`, wenn der Einwohner gerade zu Hause ist.
     *
     * `null` ist kein Fehlwert, sondern eine Aussage: Die Domaene kennt `HOME` als echten Ort,
     * aber das Zuhause eines Einwohners ist keine der sechzehn sichtbaren Kulissen. Wer nicht
     * draussen und nicht im Laden ist, ist fuer die Welt schlicht nicht da.
     */
    val place: PlayScene.Place?,
    val site: LivingSite,
    val goal: GoalKind?,
    val nextAction: ActionKind?,
    /** Woran es gerade haengt - dieselbe benannte Voraussetzung wie im Kern, nie ein `false`. */
    val blockedBy: Requirement?,
    val coins: Int,
    val portions: Int,
    val minuteOfDay: Int,
    /** Ob der Einwohner in seinem Anwesenheitsfenster UND an einem sichtbaren Ort ist. */
    val publiclyPresent: Boolean
)

/**
 * **Die drei Einwohner leben zwischen den Besuchen weiter** (NT-088).
 *
 * Bis hierher besassen sie seit NT-086 zwar Identitaet, Erinnerung und eigene Ressourcen - aber
 * ihr Zustand bewegte sich ausschliesslich, wenn der Hauptavatar ihnen zufaellig begegnete.
 * Zwischen zwei Begegnungen standen sie still. Ein Laden, dessen Verkaufskraft nur existiert,
 * waehrend jemand hinsieht, ist eine Kulisse mit Gedaechtnis, keine Bevoelkerung.
 *
 * ## Derselbe Kern, kein zweiter
 *
 * Fortgeschrieben wird ueber [LivingSimulation.step] - dieselbe Entscheidungsfolge wie beim
 * Hauptavatar. Es gibt hier keine eigene Zielwahl, keinen eigenen Planer und keine Abkuerzung
 * "die Verkaufskraft steht eben im Laden". Rolle und Ankerort sind Bias und Ortsvorrat, mehr
 * nicht: Wird die Verkaufskraft hungrig genug, verlaesst sie den SHOP, weil `GET_FOOD` gewinnt -
 * und nicht, weil hier eine Ausnahme dafuer stuende.
 *
 * ## Warum die Oeffnungszeiten JEDEN Schritt neu gesetzt werden
 *
 * [WorldState.advanced] bewegt die Zeit, aber nicht [WorldState.openSites]. Ein Einwohner, der um
 * sieben Uhr angelegt wird, traegt ohne diese Zeile bis in die Nacht die Oeffnungszeiten von
 * sieben Uhr mit sich herum - der Laden haette fuer ihn nie geschlossen, und `SiteOpen` waere als
 * Hindernis wirkungslos. Genau das soll aber eine Entscheidung veraendern koennen.
 */
object LivingPopulation {

    /**
     * Wie viele Minuten ein Fortschreibungsschritt hoechstens ueberspringt.
     *
     * Eine Grenze und kein fester Takt: Die Handlungen selbst bestimmen, wie viel Zeit vergeht
     * (Arbeiten 180 Minuten, Ruhen 120, ein Blick in den Vorrat 1). Diese Zahl verhindert nur,
     * dass ein Aufruf mit weit entferntem Ziel beliebig lange rechnet.
     */
    const val MAX_STEPS_PER_ADVANCE = 64

    /** Startzustand aller drei Einwohner zu einer gemeinsamen Simulationsminute. */
    fun initial(absoluteMinute: Int): Map<String, ResidentState> =
        LivingResidents.all.associate { resident ->
            resident.profileId to ResidentState(
                agent = LivingResidents.initialAgent(resident),
                world = synchronised(
                    LivingResidents.initialWorld(resident, absoluteMinute)
                )
            )
        }

    /**
     * Schreibt jeden Einwohner bis [toAbsoluteMinute] fort.
     *
     * Deterministisch: Gleicher Eingangszustand und gleiche Zielminute ergeben denselben
     * Ausgang. Der Kern wuerfelt nicht (siehe [WorldState]), und das Interessenthema kommt aus
     * [interestFor] statt aus einem Zufallsgenerator.
     */
    fun advance(
        states: Map<String, ResidentState>,
        toAbsoluteMinute: Int
    ): Map<String, ResidentState> = states.mapValues { (profileId, state) ->
        val resident = LivingResidents.all.firstOrNull { it.profileId == profileId }
            ?: return@mapValues state
        advanceOne(resident, state, toAbsoluteMinute)
    }

    private fun advanceOne(
        resident: LivingResident,
        state: ResidentState,
        toAbsoluteMinute: Int
    ): ResidentState {
        var agent = state.agent
        var world = state.world
        var steps = 0
        while (world.absoluteMinute < toAbsoluteMinute && steps < MAX_STEPS_PER_ADVANCE) {
            val result = LivingSimulation.step(
                agent = agent,
                world = synchronised(world),
                interest = interestFor(resident, agent.goal, world)
            )
            // Ein Schritt, der die Zeit NICHT bewegt, wuerde die Schleife nur leerlaufen lassen.
            // Der Kern laesst das nicht zu (auch Leerlauf kostet Minuten), aber verlassen wird
            // sich darauf nicht: Eine kuenftige Handlung mit `minutes = 0` haenge hier nicht.
            if (result.world.absoluteMinute <= world.absoluteMinute) {
                return ResidentState(result.agent, synchronised(result.world))
            }
            agent = result.agent
            world = result.world
            steps++
        }
        return ResidentState(agent, synchronised(world))
    }

    /** Der Blick von aussen auf die ganze Bevoelkerung, in fester Reihenfolge. */
    fun snapshot(states: Map<String, ResidentState>): List<ResidentSnapshot> =
        LivingResidents.all.mapNotNull { resident ->
            val state = states[resident.profileId] ?: return@mapNotNull null
            val place = placeFor(resident, state.world)
            ResidentSnapshot(
                profileId = resident.profileId,
                role = resident.role,
                species = resident.species,
                place = place,
                site = state.world.site,
                goal = state.agent.goal,
                nextAction = state.agent.plan?.next?.kind,
                blockedBy = state.agent.plan?.next?.blockedBy(synchronised(state.world)),
                coins = state.world.coins,
                portions = state.world.portions,
                minuteOfDay = state.world.minuteOfDay,
                publiclyPresent = place != null && resident.isActiveAt(state.world.minuteOfDay)
            )
        }

    /**
     * Wo ein Einwohner sichtbar waere.
     *
     * Die Domaene fuehrt vier Orte, die Welt sechzehn - und das bleibt so (NT-087). Die Zuordnung
     * geht deshalb ueber den Ortsvorrat des Einwohners: Sein Ankerort gewinnt, sonst der erste
     * seiner Besuchsorte, der zum Domaenenort passt. Damit steht die Verkaufskraft im Laden und
     * nicht irgendwo, ohne dass die Domaene einen fuenften Ort braeuchte.
     */
    fun placeFor(resident: LivingResident, world: WorldState): PlayScene.Place? {
        if (world.site == LivingSite.HOME) return null
        // Der Ankerort gewinnt, sobald er diesen Domaenenort vertritt - fuer die Verkaufskraft
        // also auch beim Arbeiten (siehe [LivingResident.anchorSites]).
        if (world.site in resident.anchorSites) return resident.anchorPlace
        val passend = { place: PlayScene.Place -> LivingRuntimeAdapter.siteFor(place) == world.site }
        return resident.visitPlaces.sortedBy { it.ordinal }.firstOrNull(passend)
    }

    /**
     * Welche Beschaeftigung eine Freizeit- oder Entwicklungsphase annimmt.
     *
     * **Rolle als Neigung, nicht als Drehbuch.** Sie waehlt nur AUS den Themen, die der Kern
     * ohnehin zulaesst, und erst, nachdem er entschieden hat, dass ueberhaupt Freizeit dran ist.
     * Der Wechsel innerhalb der Neigung haengt am Tag und am Einwohner: Ohne ihn taete jeder
     * Einwohner an jedem Tag dasselbe, und "verschiedene Historien" waere eine Behauptung.
     *
     * Kein Zufall, damit ein Mehrtageslauf wiederholbar bleibt.
     */
    fun interestFor(resident: LivingResident, goal: GoalKind?, world: WorldState): ActionKind? {
        val neigung = when (resident.role) {
            ResidentRole.SHOPKEEPER -> listOf(
                AnimationType.GENERAL,
                AnimationType.CREATIVITY,
                AnimationType.LOVE
            )
            ResidentRole.PARK_REGULAR -> listOf(
                AnimationType.MOVE,
                AnimationType.MINDFULNESS,
                AnimationType.BOOK
            )
            ResidentRole.ATHLETE -> listOf(
                AnimationType.MOVE,
                AnimationType.GENERAL,
                AnimationType.FOCUS
            )
        }
        val index = LivingResidents.all.indexOfFirst { it.profileId == resident.profileId }
        val topic = neigung[(world.day + index).mod(neigung.size)]
        // Entwicklung will gelernt werden, nicht gespielt - dieselbe Unterscheidung wie im
        // Adapter, damit ein Einwohner nicht beim Sport klueger wird.
        val gerichtet = if (goal == GoalKind.DEVELOP && topic == AnimationType.MOVE) {
            AnimationType.BOOK
        } else {
            topic
        }
        return when (gerichtet) {
            AnimationType.BOOK -> ActionKind.READ
            AnimationType.CREATIVITY -> ActionKind.CREATE
            AnimationType.FOCUS -> ActionKind.CONCENTRATE
            AnimationType.MINDFULNESS -> ActionKind.SETTLE
            AnimationType.MOVE -> ActionKind.MOVE_BODY
            AnimationType.LOVE -> ActionKind.SHOW_AFFECTION
            else -> ActionKind.PURSUE_INTEREST
        }
    }

    /** Zeit, Ort und Oeffnungszeiten in Uebereinstimmung bringen - siehe KDoc oben. */
    private fun synchronised(world: WorldState): WorldState =
        world.copy(openSites = LivingRuntimeAdapter.openSitesAt(world.minuteOfDay))
}
