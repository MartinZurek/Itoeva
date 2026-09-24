package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.living.AgentState
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.LivingEventKind
import com.notime.glyphsim.living.LivingSimulation
import com.notime.glyphsim.living.LivingSite
import com.notime.glyphsim.living.Requirement
import com.notime.glyphsim.living.StepResult
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

/** Beide bereits gerechneten Seiten eines vollstaendig abgeschlossenen gemeinsamen Trainings. */
data class SharedTrainingCompletion(
    val host: StepResult,
    val resident: StepResult
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
    val publiclyPresent: Boolean,
    /**
     * Welche Sonderaktivitaet das geplante `MOVE_BODY` wirklich meint - `null` bei jeder anderen
     * Handlung (siehe [LivingPopulation.specialActivityFor]).
     *
     * **Ohne dieses Feld waere jedes `MOVE_BODY` austauschbar.** Der Kern kennt keine einzelne
     * Sportart (siehe `ActionKind.MOVE_BODY`-KDoc); ohne einen echten, vom Einwohner selbst
     * herleitbaren Wert koennte eine gemeinsame Szene nur behaupten, beide Seiten meinten
     * dieselbe Sonderaktivitaet, ohne es zu belegen - genau der Fehler, den NT-092 gefunden und
     * zurueckgenommen hat (siehe `EVOLUTION.md`). Der Wert ist deterministisch aus Einwohner und
     * Simulationstag hergeleitet, nie aus Rolle allein und nie gewuerfelt.
     */
    val nextSpecialActivity: PlayRoutines.SpecialActivity?
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
            val nextKind = state.agent.plan?.next?.kind
            // Wer als Naechstes Sport treibt, bleibt dafuer am Ankerort (siehe [placeFor]).
            val place = placeFor(resident, state.world, keepAnchor = nextKind == ActionKind.MOVE_BODY)
            ResidentSnapshot(
                profileId = resident.profileId,
                role = resident.role,
                species = resident.species,
                place = place,
                site = state.world.site,
                goal = state.agent.goal,
                nextAction = nextKind,
                blockedBy = state.agent.plan?.next?.blockedBy(synchronised(state.world)),
                coins = state.world.coins,
                portions = state.world.portions,
                minuteOfDay = state.world.minuteOfDay,
                publiclyPresent = place != null && resident.isActiveAt(state.world.minuteOfDay),
                nextSpecialActivity = if (nextKind == ActionKind.MOVE_BODY) {
                    specialActivityFor(resident, state.world)
                } else {
                    null
                }
            )
        }

    /**
     * Schliesst genau die bereits geplante Einwohnerhandlung an der sichtbaren Wirkungsgrenze ab.
     *
     * NT-091 darf Bewegung nicht direkt auf Beduerfnisse oder Erinnerungen schreiben. Deshalb
     * laeuft auch der zweite Teilnehmer nach der Choreografie durch [LivingSimulation.step] und
     * damit durch denselben `ActionOutcome` wie jede unsichtbare Fortschreibung. Ist der Plan
     * inzwischen unpassend oder blockiert, gibt es kein Ergebnis und der Aufrufer uebernimmt
     * keinen der beiden vorbereiteten Zustaende.
     */
    fun completeSharedAction(
        state: ResidentState,
        expected: ActionKind
    ): StepResult? {
        val world = synchronised(state.world)
        val next = state.agent.plan?.next ?: return null
        if (next.kind != expected || next.blockedBy(world) != null) return null
        val result = LivingSimulation.step(state.agent, world)
        return result.takeIf { step ->
            step.events.any {
                it.kind == LivingEventKind.ACTION_DONE && it.action == expected
            }
        }
    }

    /**
     * Verbindet zwei abgeschlossene `MOVE_BODY`-Schritte erst hinter der sichtbaren Grenze.
     *
     * Fehlt der wirkliche Abschluss auf nur einer Seite, entsteht gar kein Ergebnis. Der
     * Aufrufer kann dadurch weder die Hauptseite noch den Einwohner teilweise speichern. Erst
     * danach erzeugt [LivingSimulation.rememberSharedTraining] die gegenseitige Episode und
     * Naehe; die vorhandenen Bewegungswirkungen werden nicht ein zweites Mal gerechnet.
     */
    fun completeSharedTraining(
        host: StepResult,
        residentBefore: ResidentState
    ): SharedTrainingCompletion? {
        val hostMoved = host.events.any {
            it.kind == LivingEventKind.ACTION_DONE && it.action == ActionKind.MOVE_BODY
        }
        if (!hostMoved) return null
        val residentMoved = completeSharedAction(residentBefore, ActionKind.MOVE_BODY)
            ?: return null
        if (
            host.world.site != LivingSite.OUTSIDE ||
            residentMoved.world.site != LivingSite.OUTSIDE
        ) return null

        val memory = LivingSimulation.rememberSharedTraining(
            first = host.agent,
            firstWorld = host.world,
            second = residentMoved.agent,
            secondWorld = residentMoved.world
        )
        return SharedTrainingCompletion(
            host = host.copy(
                agent = memory.first,
                events = host.events + memory.firstEvent
            ),
            resident = residentMoved.copy(
                agent = memory.second,
                events = residentMoved.events + memory.secondEvent
            )
        )
    }

    /**
     * Wo ein Einwohner sichtbar waere.
     *
     * Die Domaene fuehrt vier Orte, die Welt sechzehn - und das bleibt so (NT-087). Die Zuordnung
     * geht deshalb ueber den Ortsvorrat des Einwohners: Sein Ankerort gewinnt, sonst der erste
     * seiner Besuchsorte, der zum Domaenenort passt. Damit steht die Verkaufskraft im Laden und
     * nicht irgendwo, ohne dass die Domaene einen fuenften Ort braeuchte.
     *
     * ## Draussen trifft man sich
     *
     * **Gemeldet als "ich habe noch nie drei oder vier zusammen gesehen".** Gemessen ueber eine
     * Woche (8 bis 20 Uhr, alle fuenf Minuten) stand im Park nie mehr als zwei, zu 75 Prozent
     * niemand - obwohl vier Bewohner den Park in ihren Besuchsorten fuehren. Der Grund stand
     * hier: Draussen ist fuer die Domaene ein einziger Ort, [LivingSite.OUTSIDE], und den
     * vertritt fuer jeden Bewohner mit Ankerort unter freiem Himmel immer der Ankerort. Hootlet
     * und Wyrmling standen deshalb ausschliesslich auf dem Sportplatz, Gloop nur in der Stadt;
     * Strasse und Park-Besuche aus [LivingResident.visitPlaces] kamen nie vor.
     *
     * Jetzt geht, wer draussen ist, zum Treffpunkt der Tageszeit ([gatheringPlace]), sofern der
     * zu seinen Besuchsorten gehoert - sonst bleibt er an seinem Ankerort. Kein Zufall: Derselbe
     * Stand ergibt denselben Ort.
     *
     * [keepAnchor] haelt den Bewohner an seinem Ankerort fest, solange er dort gerade etwas
     * Bestimmtes vorhat (Sport auf dem Sportplatz - siehe [snapshot]); sonst verschwaende die
     * gemeinsame Sportplatz-Szene (siehe [LivingPopulationLayout.sharedSportPartner]) oft ihren Partner.
     *
     * ## Einkaufen im einzigen Laden
     *
     * Wer auf den Markt geht und den Laden nicht in seinen Besuchsorten fuehrt, war bisher
     * unsichtbar ([LivingSite.MARKET] ohne passenden Ort). Es gibt aber genau einen Laden in der
     * Stadt - wer einkauft, steht dort.
     */
    fun placeFor(
        resident: LivingResident,
        world: WorldState,
        keepAnchor: Boolean = false
    ): PlayScene.Place? {
        if (world.site == LivingSite.HOME) return null
        val passend = resident.visitPlaces
            .filter { LivingRuntimeAdapter.siteFor(it) == world.site }
            .sortedBy { it.ordinal }
        // Der Ankerort gewinnt, sobald er diesen Domaenenort vertritt - fuer die Verkaufskraft
        // also auch beim Arbeiten (siehe [LivingResident.anchorSites]). Nur draussen darf ein
        // Bewohner zwischendurch woanders sein.
        if (world.site in resident.anchorSites) {
            val andere = passend - resident.anchorPlace
            if (keepAnchor || world.site != LivingSite.OUTSIDE || andere.isEmpty()) {
                return resident.anchorPlace
            }
            return wanderPlace(resident, world, andere)
        }
        if (passend.isEmpty() && world.site == LivingSite.MARKET) return PlayScene.Place.SHOP
        if (world.site == LivingSite.OUTSIDE) {
            val treffpunkt = gatheringPlace(world.minuteOfDay)
            if (treffpunkt in passend) return treffpunkt
        }
        return passend.firstOrNull()
    }

    /**
     * Der Treffpunkt der Stadt zu dieser Tageszeit - fuer alle derselbe.
     *
     * **Absichtlich gemeinsam und nicht je Bewohner versetzt.** Ein erster Entwurf liess jeden
     * Bewohner in eigenem Takt durch seine Besuchsorte wandern. Gemessen verteilte das nur: Der
     * Park hatte danach noch seltener Besuch als vorher (16 statt 25 Prozent), weil ein Bewohner
     * ohnehin nur etwa ein Fuenftel des Tages draussen ist. Menschen verteilen sich nicht
     * gleichmaessig - sie treffen sich dort, wo zu dieser Stunde alle hingehen. Genau daraus
     * entstehen Grueppchen.
     */
    fun gatheringPlace(minuteOfDay: Int): PlayScene.Place = when (minuteOfDay / 60) {
        in 0 until 11 -> PlayScene.Place.PARK
        in 11 until 14 -> PlayScene.Place.CITY
        in 14 until 18 -> PlayScene.Place.PARK
        in 18 until 20 -> PlayScene.Place.STREET
        // Abends in die Spielhalle - wer sie nicht kennt, bleibt an seinem Ankerort. Ohne diese
        // Zeile stand dort nie ein Bewohner, obwohl Gloop und Wyrmling sie in ihren
        // Besuchsorten fuehren.
        in 20 until 24 -> PlayScene.Place.ARCADE
        else -> PlayScene.Place.CITY
    }

    /** Zum Treffpunkt, wenn er zu den eigenen Besuchsorten gehoert - sonst der Ankerort. */
    private fun wanderPlace(
        resident: LivingResident,
        world: WorldState,
        andere: List<PlayScene.Place>
    ): PlayScene.Place {
        val treffpunkt = gatheringPlace(world.minuteOfDay)
        return if (treffpunkt in andere) treffpunkt else resident.anchorPlace
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
            ResidentRole.NEIGHBOR -> listOf(
                AnimationType.LOVE,
                AnimationType.GENERAL,
                AnimationType.MINDFULNESS
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

    /**
     * Welche Sonderaktivitaet ein geplantes `MOVE_BODY` wirklich meint (Folgeschnitt zu NT-092).
     *
     * **Der Fund, der diese Funktion noetig gemacht hat.** Ein erster Versuch liess eine
     * gemeinsame Basketball-Szene allein daran haengen, dass der Hauptavatar wirklich Basketball
     * spielte UND ein Einwohner irgendein unblockiertes `MOVE_BODY` plante - ohne dass der
     * Einwohner selbst je Basketball "meinte". Dieselbe Bedingung haette ebenso Fussball, Drachen
     * oder Angeln "belegt" (siehe `EVOLUTION.md`, Eintrag zu NT-092); der Entwurf wurde deshalb
     * zurueckgenommen.
     *
     * **Nur zwei Werte, aus demselben Grund wie beim Hauptavatar.** TRAINING und BASKETBALL
     * teilen sich den Ort SPORT; eine dritte Sonderaktivitaet braeuchte entweder einen eigenen
     * Domaenenort (Drachen: PARK, Angeln: POND) oder zusaetzlichen, rein hauptavatarbezogenen
     * Zustand (Fussball: der gelernte Trick) - beides ausserhalb dieses kleinen Schnitts.
     *
     * **Deterministisch aus Einwohner und Simulationstag**, nach demselben Muster wie
     * [interestFor]s Themenrotation - nicht aus der Rolle allein (beide Werte kommen fuer
     * ATHLETE UND PARK_REGULAR gleichermassen an die Reihe) und nicht gewuerfelt, damit ein
     * Mehrtageslauf wiederholbar bleibt und der Wert unabhaengig vom Hauptavatar entsteht: Nur
     * wenn beide Seiten am selben Tag zufaellig dieselbe konkrete Aktivitaet "meinen", entsteht
     * eine gemeinsame Szene.
     */
    fun specialActivityFor(
        resident: LivingResident,
        world: WorldState
    ): PlayRoutines.SpecialActivity {
        val index = LivingResidents.all.indexOfFirst { it.profileId == resident.profileId }
        return SHARED_SPORT_ROTATION[(world.day + index).mod(SHARED_SPORT_ROTATION.size)]
    }

    private val SHARED_SPORT_ROTATION = listOf(
        PlayRoutines.SpecialActivity.TRAINING,
        PlayRoutines.SpecialActivity.BASKETBALL
    )

    /** Zeit, Ort und Oeffnungszeiten in Uebereinstimmung bringen - siehe KDoc oben. */
    private fun synchronised(world: WorldState): WorldState =
        world.copy(openSites = LivingRuntimeAdapter.openSitesAt(world.minuteOfDay))
}
