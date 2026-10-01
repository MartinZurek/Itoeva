package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.matrix.PlayScene.Place

/**
 * **Die Weltkarte: welcher Ort an welchem liegt.**
 *
 * Gemeldet am 29.09.2026: Das Wesen war auf der Expedition im Gebirge - und stand im naechsten
 * Bild auf dem Sportplatz. Nach einer gefuetterten Trink-Erinnerung ging es aus der Wildnis
 * "nach Hause", um sich dort etwas zu holen. Beides lag an derselben Stelle: Jeder Ortswechsel
 * war ein Sprung (Tuer auf, Kulisse getauscht), egal wie weit das Ziel entfernt lag, und jedes
 * Thema hatte genau einen festen Ort, zu dem es fuehrte.
 *
 * Entschieden vom Nutzer: "Es muss eine Art Mappe geben, wo er sich gerade befindet, und dann
 * gibt es nur logische Wege irgendwo anders hin." Dazu: **Lange Wege werden richtig gereist** -
 * an Zwischenorten draussen bleibt das Wesen kurz stehen und sieht sich um, statt nur
 * durchzuhuschen.
 *
 * ## Die Karte
 *
 * Vier Gegenden, von innen nach aussen:
 *
 * - **Daheim** - die Zimmer liegen alle an einem Flur und sind untereinander direkt erreichbar;
 *   hinaus geht es nur durchs Wohnzimmer auf die Strasse.
 * - **Stadt** - an der Strasse liegen Laden, Stadtmitte und Park; in der Stadtmitte Arbeit, Cafe
 *   und Spielhalle.
 * - **Gruen** - hinter dem Park Sportplatz, Teich und Wiese; hinter der Wiese der Wald.
 * - **Wildnis** - Ebene, Strand, Sumpf, Dschungel und Gebirge, am Gebirge Grotte und Lager.
 *
 * [route] liefert den kuerzesten Weg; die Ausfuehrung (DockScreen.moveToPlace) geht jeden
 * Zwischenort sichtbar durch. Was unterwegs anfaellt und daheim laege, wird vor Ort erledigt
 * ([staysLocal], [onTheSpot]).
 */
object PlayMap {

    /** Die vier Gegenden, von innen nach aussen - die Reihenfolge ist die Entfernung von daheim. */
    enum class Region { HOME, TOWN, GREEN, WILD }

    fun regionOf(place: Place): Region = when (place) {
        Place.BEDROOM, Place.BATH, Place.DESK, Place.KITCHEN, Place.NOOK, Place.LIVING, Place.CRAFT ->
            Region.HOME
        Place.STREET, Place.SHOP, Place.CITY, Place.WORK, Place.CAFE, Place.ARCADE -> Region.TOWN
        Place.PARK, Place.SPORT, Place.POND, Place.MEADOW, Place.FOREST -> Region.GREEN
        Place.PLAINS, Place.BEACH, Place.SWAMP, Place.JUNGLE, Place.MOUNTAINS, Place.GROTTO, Place.CAMP ->
            Region.WILD
    }

    private val ROOMS = listOf(
        Place.LIVING, Place.KITCHEN, Place.BEDROOM, Place.BATH, Place.DESK, Place.NOOK, Place.CRAFT
    )

    /**
     * Die Wege. Ungerichtet: Wer hinkommt, kommt auch zurueck. Die Reihenfolge entscheidet bei
     * gleich langen Wegen, welcher genommen wird - damit ist jeder Weg reproduzierbar.
     */
    private val EDGES: List<Pair<Place, Place>> = buildList {
        // Daheim: ein Flur, jedes Zimmer an jedem.
        for (i in ROOMS.indices) for (j in i + 1 until ROOMS.size) add(ROOMS[i] to ROOMS[j])
        // Die Haustuer.
        add(Place.LIVING to Place.STREET)
        // Die Stadt.
        add(Place.STREET to Place.SHOP)
        add(Place.STREET to Place.CITY)
        add(Place.STREET to Place.PARK)
        add(Place.CITY to Place.WORK)
        add(Place.CITY to Place.CAFE)
        add(Place.CITY to Place.ARCADE)
        // Das Gruen hinter dem Park.
        add(Place.PARK to Place.SPORT)
        add(Place.PARK to Place.POND)
        add(Place.PARK to Place.MEADOW)
        add(Place.POND to Place.MEADOW)
        add(Place.MEADOW to Place.FOREST)
        // Hinaus in die Wildnis.
        add(Place.MEADOW to Place.PLAINS)
        add(Place.FOREST to Place.MOUNTAINS)
        add(Place.FOREST to Place.SWAMP)
        add(Place.PLAINS to Place.MOUNTAINS)
        add(Place.PLAINS to Place.SWAMP)
        add(Place.PLAINS to Place.BEACH)
        add(Place.SWAMP to Place.JUNGLE)
        add(Place.JUNGLE to Place.BEACH)
        add(Place.MOUNTAINS to Place.GROTTO)
        // Das Lager liegt unter den Bergen, zwischen Wald und Ebene - dort, wo beide mehrtaegigen
        // Reisen uebernachten (siehe PlayQuests).
        add(Place.MOUNTAINS to Place.CAMP)
        add(Place.FOREST to Place.CAMP)
        add(Place.PLAINS to Place.CAMP)
    }

    private val NEIGHBORS: Map<Place, List<Place>> = Place.entries.associateWith { place ->
        EDGES.mapNotNull { (a, b) ->
            when (place) {
                a -> b
                b -> a
                else -> null
            }
        }
    }

    /** Die Orte, die direkt an [place] liegen. */
    fun neighbors(place: Place): List<Place> = NEIGHBORS.getValue(place)

    /**
     * **Der Weg von [from] nach [to]** - ohne den Ausgangsort, mit dem Ziel am Ende. Leer, wenn
     * beide gleich sind. Jeder Ort der Liste liegt direkt am vorigen.
     */
    fun route(from: Place, to: Place): List<Place> {
        if (from == to) return emptyList()
        val cameFrom = mutableMapOf<Place, Place>()
        val queue = ArrayDeque(listOf(from))
        val seen = mutableSetOf(from)
        while (queue.isNotEmpty()) {
            val here = queue.removeFirst()
            if (here == to) break
            for (next in neighbors(here)) {
                if (seen.add(next)) {
                    cameFrom[next] = here
                    queue.addLast(next)
                }
            }
        }
        val path = mutableListOf<Place>()
        var step: Place? = to
        while (step != null && step != from) {
            path += step
            step = cameFrom[step]
        }
        return path.reversed()
    }

    /**
     * Ob vor diesem Weg die Karte erscheint (siehe [PlayMapScene]): bei weiten Wegen, die in eine
     * andere Gegend fuehren. Von Zimmer zu Zimmer oder vom Park zum Teich braucht es keinen Blick
     * auf die Karte - sie soll ein Ereignis bleiben, kein Vorspann vor jedem Schritt.
     */
    fun showsMap(from: Place, to: Place): Boolean =
        regionOf(from) != regionOf(to) && hops(from, to) >= MAP_MIN_HOPS

    /** Ab so vielen Orten Weg erscheint die Karte. */
    const val MAP_MIN_HOPS = 3

    /** Wie viele Wege zwischen [from] und [to] liegen. */
    fun hops(from: Place, to: Place): Int = route(from, to).size

    /**
     * Ob das Wesen an diesem Zwischenort auf der Durchreise **stehen bleibt und sich umsieht**
     * (Entscheidung des Nutzers: "richtig reisen"). Draussen im Gruen und in der Wildnis ja;
     * durch Zimmer und Strassen geht es zuegig hindurch.
     */
    fun lingersAt(place: Place): Boolean = regionOf(place) >= Region.GREEN

    /**
     * **Vor Ort statt heimlaufen.** Wer draussen ist - im Gruen oder in der Wildnis - und etwas
     * tun will, das sonst weiter drinnen stattfindet (Trinken in der Kueche, Hanteln auf dem
     * Sportplatz), macht es an Ort und Stelle, statt dafuer einen langen Weg zurueckzugehen.
     * Kurze Wege (bis zwei Orte weit) werden weiterhin gegangen: Vom Teich zum Park laeuft man.
     */
    fun staysLocal(here: Place, destination: Place): Boolean =
        regionOf(here) >= Region.GREEN &&
            regionOf(destination) < regionOf(here) &&
            hops(here, destination) >= LOCAL_FROM_HOPS

    /** Ab so vielen Wegen gilt ein Ziel als zu weit fuer eine Kleinigkeit. */
    const val LOCAL_FROM_HOPS = 3

    /**
     * **Umwege abschneiden.** Viele Ablaeufe beginnen "vor der Haustuer" (erst auf die Strasse,
     * dann hinaus zum Ziel), weil sie fuer den Start daheim geschrieben sind. Wer schon draussen
     * ist, ginge sonst erst den ganzen Weg zurueck in die Stadt, um von dort wieder hinaus zu
     * laufen. Ein Abschnitt, der nur Anmarsch ist, faellt weg, wenn der naechste Ort von hier aus
     * naeher liegt als ueber ihn.
     */
    fun trimmedFrom(routine: PlayRoutine, here: Place): PlayRoutine {
        var steps = routine.steps
        while (true) {
            val goes = steps.withIndex().filter { it.value is RoutineStep.GoToPlace }
            if (goes.size < 2) break
            val first = (goes[0].value as RoutineStep.GoToPlace).place
            val second = (goes[1].value as RoutineStep.GoToPlace).place
            if (hops(here, second) >= hops(here, first) + hops(first, second)) break
            steps = steps.take(goes[0].index) + steps.drop(goes[1].index)
        }
        return if (steps == routine.steps) routine else PlayRoutine(steps)
    }

    /** Wohin ein Ablauf zuerst fuehrt - der erste Ortswechsel, sonst der Ort seines Themas. */
    fun destinationOf(routine: PlayRoutine, topic: AnimationType?): Place =
        routine.steps.filterIsInstance<RoutineStep.GoToPlace>().firstOrNull()?.place
            ?: PlayScene.forTopic(topic)

    /**
     * Ein Ablauf **von hier aus**: Umwege abgeschnitten, und liegt das Ziel zu weit fuer das,
     * was es ist, die Fassung vor Ort ([onTheSpot]). [local] sagt, ob es bei diesem Ort bleibt.
     */
    data class FromHere(val routine: PlayRoutine, val local: Boolean)

    fun fromHere(routine: PlayRoutine, topic: AnimationType?, here: Place, roll: Int): FromHere {
        val trimmed = trimmedFrom(routine, here)
        val destination = destinationOf(trimmed, topic)
        if (topic != null && staysLocal(here, destination)) {
            onTheSpot(topic, here, roll)?.let { return FromHere(it, local = true) }
        }
        return FromHere(trimmed, local = false)
    }

    /** Was man draussen zum Stemmen findet: im Wald einen Stamm, sonst einen Stein. */
    fun gearAt(place: Place): PlayEffects.TrainingGear = when (place) {
        Place.FOREST, Place.JUNGLE, Place.SWAMP, Place.CAMP -> PlayEffects.TrainingGear.LOG
        Place.MOUNTAINS, Place.GROTTO, Place.PLAINS, Place.BEACH ->
            PlayEffects.TrainingGear.STONE
        else -> PlayEffects.TrainingGear.DUMBBELL
    }

    /**
     * **Dasselbe Thema, an Ort und Stelle.** Trinken aus dem Proviant, Sport mit dem, was da
     * ist (sprinten, Saltos, einen Stein oder Stamm stemmen), Lesen und Rasten auf der Bank oder
     * einfach im Gras. `null` fuer das, was sich draussen nicht tun laesst (Schlafen, Arbeiten,
     * am Schreibtisch sitzen) - dafuer wird der Weg gegangen.
     */
    fun onTheSpot(topic: AnimationType, place: Place, roll: Int): PlayRoutine? {
        val bench = PlayScene.Station.BENCH in PlayScene.stationsAt(place)
        val sitDown: List<RoutineStep> = if (bench) {
            listOf(RoutineStep.GoTo(PlayScene.Station.BENCH), RoutineStep.Occupy(PlayScene.Station.BENCH))
        } else {
            listOf(RoutineStep.Stroll(0.3f))
        }
        val standUp: List<RoutineStep> = if (bench) listOf(RoutineStep.Rise) else emptyList()
        val steps: List<RoutineStep> = when (topic) {
            // Aus dem Rucksack: auspacken, hinstellen, trinken.
            AnimationType.DRINK -> sitDown + listOf(
                RoutineStep.Take(PlayEffects.Carried.FOOD), RoutineStep.Linger(1_500L), RoutineStep.Drop,
                RoutineStep.Act(AnimationType.DRINK), RoutineStep.Linger(4_000L)
            ) + standUp
            AnimationType.MEDICINE -> listOf(
                RoutineStep.Stroll(0.4f), RoutineStep.Act(AnimationType.MEDICINE), RoutineStep.Linger(3_000L)
            )
            AnimationType.MOVE -> moveHere(place, roll)
            AnimationType.REST, AnimationType.LOVE -> sitDown + listOf(
                RoutineStep.Linger(10_000L), RoutineStep.Daydream, RoutineStep.Act(topic), RoutineStep.Linger(3_000L)
            ) + standUp + RoutineStep.Stir(AvatarAnimations.Fidget.STRETCH)
            AnimationType.BOOK -> listOf<RoutineStep>(RoutineStep.Take(PlayEffects.Carried.BOOK)) + sitDown + listOf(
                RoutineStep.Act(AnimationType.BOOK), RoutineStep.Linger(10_000L)
            ) + standUp + RoutineStep.Drop
            AnimationType.MINDFULNESS -> listOf(
                RoutineStep.Stroll(0.6f), RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                RoutineStep.Act(AnimationType.MINDFULNESS), RoutineStep.Linger(10_000L)
            )
            AnimationType.CREATIVITY -> listOf(
                RoutineStep.Stroll(0.4f), RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                RoutineStep.Act(AnimationType.CREATIVITY), RoutineStep.Linger(9_000L),
                RoutineStep.Stir(AvatarAnimations.Fidget.STRETCH)
            )
            AnimationType.GENERAL -> listOf(
                RoutineStep.Stroll(0.5f), RoutineStep.Stir(AvatarAnimations.Fidget.LOOK_AROUND),
                RoutineStep.Act(AnimationType.GENERAL), RoutineStep.Linger(4_000L)
            )
            AnimationType.SLEEP, AnimationType.WORK, AnimationType.FOCUS -> return null
        }
        return PlayRoutine(steps)
    }

    /**
     * Sport ohne Sportplatz: hin und her sprinten und springen, ein paar Saltos, oder etwas
     * stemmen, das hier liegt ([gearAt]). [roll] waehlt.
     */
    private fun moveHere(place: Place, roll: Int): List<RoutineStep> = when (Math.floorMod(roll, 3)) {
        0 -> listOf(
            RoutineStep.Stroll(0.15f), RoutineStep.Stroll(0.85f), RoutineStep.Stroll(0.25f),
            RoutineStep.Act(AnimationType.MOVE), RoutineStep.Stir(AvatarAnimations.Fidget.STRETCH),
            RoutineStep.Linger(3_000L)
        )
        1 -> listOf(
            RoutineStep.Stroll(0.5f), RoutineStep.Stir(AvatarAnimations.Fidget.FLIP), RoutineStep.Linger(1_500L),
            RoutineStep.Stir(AvatarAnimations.Fidget.FLIP), RoutineStep.Linger(1_500L),
            RoutineStep.Stir(AvatarAnimations.Fidget.FLIP), RoutineStep.Stir(AvatarAnimations.Fidget.SHAKE),
            RoutineStep.Linger(3_000L)
        )
        else -> {
            val gear = gearAt(place)
            listOf(
                RoutineStep.Stroll(0.34f),
                RoutineStep.Training(PlayEffects.TrainingPhase.WARM_UP, gear), RoutineStep.Linger(4_000L),
                RoutineStep.Training(PlayEffects.TrainingPhase.LIFT, gear), RoutineStep.Linger(4_000L),
                RoutineStep.Training(PlayEffects.TrainingPhase.REST, gear), RoutineStep.Linger(2_500L)
            )
        }
    }
}
