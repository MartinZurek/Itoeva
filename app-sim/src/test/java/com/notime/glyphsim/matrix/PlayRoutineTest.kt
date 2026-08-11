package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft die Kopplung zwischen Tagesablaeufen ([PlayRoutine]) und Einrichtung ([PlayScene]).
 *
 * **Warum das ausgerechnet hier wichtig ist:** Die beiden Dateien kennen einander nur ueber die
 * Namen der Plaetze ([PlayScene.Station]). Nennt ein Ablauf einen Platz, den es an seinem Ort
 * nicht gibt, faellt das nirgends auf - der Schritt wird zur Laufzeit stillschweigend
 * uebersprungen, und der Avatar bleibt an Ort und Stelle stehen, statt zum Bett zu gehen. Kein
 * Absturz, keine Warnung, nur eine Figur, die weniger tut als gedacht. Genau solche Fehler
 * entstehen beim Umstellen der Einrichtung.
 */
class PlayRoutineTest {

    private val width = 46
    private val floorY = 22

    /**
     * Laeuft den Ablauf Schritt fuer Schritt ab und fuehrt dabei mit, in welchem Raum die Figur
     * gerade ist - seit es [RoutineStep.GoToPlace] gibt, kann ein einziger Ablauf mehrere Raeume
     * beruehren (einkaufen gehen und zu Hause essen). Ein Test, der alle Plaetze gegen den
     * Startraum prueft, wuerde genau diese Ablaeufe faelschlich anschlagen lassen.
     */
    private fun stationsWithPlace(
        startPlace: PlayScene.Place,
        routine: PlayRoutine
    ): List<Pair<PlayScene.Place, PlayScene.Station>> {
        var place = startPlace
        val result = mutableListOf<Pair<PlayScene.Place, PlayScene.Station>>()
        for (step in routine.steps) {
            when (step) {
                is RoutineStep.GoToPlace -> place = step.place
                is RoutineStep.GoTo -> result += place to step.station
                is RoutineStep.Occupy -> result += place to step.station
                else -> Unit
            }
        }
        return result
    }

    @Test
    fun `jeder Ablauf spricht nur Plaetze an, die es an seinem Ort gibt`() {
        for (topic in AnimationType.entries) {
            val place = PlayScene.forTopic(topic)
            for (routine in PlayRoutines.allFor(topic)) {
                for ((at, station) in stationsWithPlace(place, routine)) {
                    // Ueber alle Spezies: Am Arbeitsplatz haengt die Einrichtung von der Kreatur
                    // ab, ein Ablauf muss also bei JEDER funktionieren.
                    for (species in AvatarSpecies.entries) {
                        assertTrue(
                            "Ablauf zu $topic will an $at ($species) zu $station - dort gibt es " +
                                "nur ${PlayScene.stationsAt(at, species)}",
                            station in PlayScene.stationsAt(at, species)
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `leerer Vorrat fuehrt in den Laden, voller nicht`() {
        // Der eine Fall, in dem NICHT gewuerfelt wird: Ist nichts mehr da, MUSS der Ablauf ueber
        // den Laden gehen. Und umgekehrt darf er bei vollem Kuehlschrank nicht staendig einkaufen
        // fahren - sonst waere der Vorrat wieder bedeutungslos.
        fun goesShopping(routine: PlayRoutine) =
            routine.steps.any { it is RoutineStep.GoToPlace && it.place == PlayScene.Place.SHOP }

        assertTrue(
            "Bei leerem Vorrat wird nicht eingekauft",
            goesShopping(PlayRoutines.forTopic(AnimationType.DRINK, needsShopping = true))
        )
        repeat(40) {
            assertTrue(
                "Bei vollem Vorrat wird trotzdem eingekauft",
                !goesShopping(PlayRoutines.forTopic(AnimationType.DRINK, needsShopping = false))
            )
        }
    }

    @Test
    fun `Besuch gibt es nur dort, wo man Leute trifft`() {
        // Eine fremde Kreatur, die nachts durchs Schlafzimmer laeuft, zerstoert den Eindruck
        // einer bewohnten Welt eher, als ihn zu staerken.
        assertTrue(PlayScene.allowsVisitors(PlayScene.Place.PARK))
        assertTrue(PlayScene.allowsVisitors(PlayScene.Place.SHOP))
        assertTrue(PlayScene.allowsVisitors(PlayScene.Place.LIVING))
        assertTrue(!PlayScene.allowsVisitors(PlayScene.Place.BEDROOM))
        assertTrue(!PlayScene.allowsVisitors(PlayScene.Place.DESK))
        assertTrue(!PlayScene.allowsVisitors(PlayScene.Place.NOOK))
    }

    @Test
    fun `jedes Thema hat mindestens einen Ablauf`() {
        for (topic in AnimationType.entries) {
            assertTrue("$topic hat keinen Ablauf", PlayRoutines.allFor(topic).isNotEmpty())
            for (routine in PlayRoutines.allFor(topic)) {
                assertTrue("$topic hat einen leeren Ablauf", routine.steps.isNotEmpty())
            }
        }
    }

    @Test
    fun `jeder Platz laesst sich tatsaechlich aufsuchen`() {
        // stationSpot muss fuer jeden ausgewiesenen Platz einen Ort liefern - sonst fehlt der
        // Requisite ihr useSpot und der Ablauf liefe wieder ins Leere.
        //
        // Ueber ALLE Spezies, seit der Arbeitsplatz je nach Kreatur anders eingerichtet ist:
        // Sonst bliebe eine fehlende Aufsetzstelle in fuenf von sechs Berufen unentdeckt.
        for (species in AvatarSpecies.entries) {
            for (place in PlayScene.Place.entries) {
                for (station in PlayScene.stationsAt(place, species)) {
                    assertNotNull(
                        "$place/$species kennt $station, liefert dafuer aber keinen Platz",
                        PlayScene.stationSpot(place, station, width, floorY, species)
                    )
                }
            }
        }
    }

    @Test
    fun `benutzte Plaetze liegen im Bild und ueber dem Boden`() {
        // Ueber mehrere Breiten, weil die Requisiten an Bruchteilen der Breite haengen: Ein Platz
        // ausserhalb des Bildes wuerde die Figur halb abschneiden.
        //
        // Ab PlayScene.MIN_SCENE_CELLS: Schmaler wird die Szene nicht mehr gezeichnet - dort
        // verkleinert sich stattdessen die Zelle. Frueher stand hier 24, was einen Raum forderte,
        // in den Sofa, Fernseher und Tuer rechnerisch gar nicht nebeneinanderpassen.
        for (w in listOf(PlayScene.MIN_SCENE_CELLS, 46, 70, 96)) {
            for (species in AvatarSpecies.entries) {
                for (place in PlayScene.Place.entries) {
                    for (station in PlayScene.stationsAt(place, species)) {
                        val spot = PlayScene.stationSpot(place, station, w, floorY, species) ?: continue
                        assertTrue(
                            "$place/$species/$station liegt bei Breite $w ausserhalb: ${spot.centerX}",
                            spot.centerX in 0 until w
                        )
                        assertTrue(
                            "$place/$species/$station setzt unterhalb des Bodens auf: ${spot.groundY}",
                            spot.groundY < floorY
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `wo man sich hinlegt oder hinsetzt, gibt es auch etwas zum Verdecken`() {
        // Ohne vordere Ebene waere ein Occupy wirkungslos: Die Figur rueckte auf die Matratze und
        // stuende dort aufrecht, ohne dass eine Decke sie als Liegende lesbar macht. Genau davon
        // lebt die Loesung (siehe PlayScene.buildFront) - deshalb hier festgenagelt.
        val occupied = AnimationType.entries
            .flatMap { PlayRoutines.allFor(it) }
            .flatMap { it.steps }
            .filterIsInstance<RoutineStep.Occupy>()
            .map { it.station }
            .toSet()
        assertTrue("Kein einziger Ablauf benutzt eine Requisite", occupied.isNotEmpty())

        for (station in occupied) {
            val place = PlayScene.Place.entries.first { station in PlayScene.stationsAt(it) }
            val front = PlayScene.buildFront(
                place, station, width, floorY, PlayAmbientActivity.DayPhase.MIDDAY
            )
            assertTrue("$station wird benutzt, hat aber keine vordere Ebene", front.isNotEmpty())
        }
    }

    @Test
    fun `wer irgendwo hineinsteigt, schaut noch mit dem Kopf heraus`() {
        // Gilt fuer JEDEN Platz, in den man sich hineinbegibt - Bett, Nest, Haengematte, Wanne,
        // Sandbad, Sessel, Stange, Sofa, Parkbank. Ueberall loest eine vordere Ebene das Problem
        // der fehlenden Lieg- und Sitzhaltungen (siehe PlayScene.buildFront), und ueberall gilt
        // dieselbe Grenze: Sie soll bis zum Hals verdecken, aber eben NUR bis dahin.
        //
        // Vorher pruefte das nur das Schlafzimmer. Inzwischen gibt es ueber zwanzig solcher
        // Plaetze (sechs Spezies mal mehrere Raeume), und die Formen sind unterschiedlich hoch -
        // eine Verdeckung, die fuer die grosse Form passt, begraebt die kleine vollstaendig.
        var checked = 0
        for (species in AvatarSpecies.entries) {
            for (place in PlayScene.Place.entries) {
                for (station in PlayScene.stationsAt(place, species)) {
                    if (!PlayScene.isOccupiable(place, station, species)) continue
                    val spot = PlayScene.stationSpot(place, station, width, floorY, species) ?: continue
                    val front = PlayScene.buildFront(
                        place, station, width, floorY, PlayAmbientActivity.DayPhase.NIGHT, 1f, species
                    )
                    if (front.isEmpty()) continue

                    val frame = AvatarAnimations.idlePose(species)
                    val size = AvatarGeometry.SIZE
                    val topRow = (0 until AvatarGeometry.HEIGHT)
                        .first { y -> (0 until size).any { x -> frame[y * size + x] > 0 } }
                    val originY = spot.groundY - AvatarBodies.forSpecies(species).groundRow()
                    val visibleRows = front.minOf { it.y } - (originY + topRow)
                    assertTrue(
                        "$species ist an $station in $place nur noch $visibleRows Zeilen hoch " +
                            "sichtbar - dort laege dann ein Moebelstueck ohne erkennbaren " +
                            "Bewohner.\n\n" + ScenePreview.render(place, species, station = station),
                        visibleRows >= 3
                    )
                    checked++
                }
            }
        }
        assertTrue("Es wurde gar kein Platz geprueft", checked > 15)
    }

    @Test
    fun `Ablaeufe ohne Verdeckung lassen die Figur nie sitzen zurueck`() {
        // Jedes Occupy braucht ein spaeteres Rise im selben Ablauf. Fehlte es, bliebe die Decke
        // ueber ihr liegen, bis irgendein spaeterer Ablauf zufaellig aufsteht.
        for (topic in AnimationType.entries) {
            for (routine in PlayRoutines.allFor(topic)) {
                val occupyAt = routine.steps.indexOfFirst { it is RoutineStep.Occupy }
                if (occupyAt < 0) continue
                val riseAt = routine.steps.indexOfLast { it is RoutineStep.Rise }
                assertTrue(
                    "Ablauf zu $topic legt sich hin, steht aber nie wieder auf",
                    riseAt > occupyAt
                )
            }
        }
    }

    @Test
    fun `der Avatar kommt regelmaessig nach draussen`() {
        // **Die Pruefung zu einer gemeldeten Beobachtung:** Beim Zusehen im Zeitraffer spielte
        // sich praktisch alles in Zimmern ab. Das war kein Fehler in einer einzelnen Zeile,
        // sondern die Summe vieler Gewichte - und genau so etwas faellt beim Lesen des Codes
        // niemals auf, weil jede einzelne Stelle fuer sich vernuenftig aussieht.
        //
        // Gezaehlt wird deshalb das ERGEBNIS: Wie oft fuehrt eine autonome Regung unter freien
        // Himmel? Die Nacht ist ausgenommen - wer nachts schlaeft, gehoert ins Bett.
        val daytime = listOf(
            PlayAmbientActivity.DayPhase.MORNING,
            PlayAmbientActivity.DayPhase.MIDDAY,
            PlayAmbientActivity.DayPhase.EVENING
        )
        // **Genug Ziehungen, damit die Zahl etwas bedeutet.** Mit sechshundert lag die Streuung bei
        // rund anderthalb Prozentpunkten, waehrend der tatsaechliche Anteil abends nur drei ueber
        // der Grenze liegt - der Test schlug dadurch gelegentlich an, ohne dass sich etwas
        // geaendert hatte. Ein Test, der mal so und mal so ausgeht, ist schlimmer als keiner: Man
        // gewoehnt sich an, ihn noch einmal laufen zu lassen, und uebersieht darueber den Tag, an
        // dem er recht hat. Der feste Startwert nimmt die restliche Zufaelligkeit aus der
        // Themenwahl; die Auswahl unter den Ablaeufen wuerfelt weiterhin und mittelt sich heraus.
        val random = kotlin.random.Random(4)
        for (phase in daytime) {
            var outside = 0
            val draws = 6_000
            repeat(draws) {
                val topic = PlayAmbientActivity.nextTopic(phase, random = random)
                // Der TATSAECHLICH gewuerfelte Ablauf, nicht alle moeglichen: Sonst zaehlte ein
                // Thema schon dann als "draussen", wenn irgendeine seiner Varianten hinausfuehrt -
                // und die Zahl sagte nichts mehr darueber aus, was man beim Zusehen wirklich sieht.
                val routine = PlayRoutines.forTopic(topic)
                val visited = routine.steps.filterIsInstance<RoutineStep.GoToPlace>()
                    .map { it.place } + PlayScene.forTopic(topic)
                if (visited.any { PlayScene.isOutdoors(it) }) outside++
            }
            val share = outside.toFloat() / draws
            assertTrue(
                "In der Phase $phase fuehren nur ${(share * 100).toInt()} % der Regungen nach " +
                    "draussen - diese Welt besteht dann fast nur aus Zimmern.",
                share >= 0.20f
            )
        }
    }

    @Test
    fun `jeder Ort draussen wird tatsaechlich aufgesucht`() {
        // Eine Kulisse, die niemand betritt, ist Aufwand ohne Wirkung. Geprueft wird deshalb,
        // dass jeder Ort unter freiem Himmel in mindestens einem Ablauf vorkommt - entweder als
        // Startort einer Taetigkeit oder als Ziel eines Ortswechsels.
        val reachable = buildSet {
            for (topic in AnimationType.entries) {
                add(PlayScene.forTopic(topic))
                for (routine in PlayRoutines.allFor(topic)) {
                    for (step in routine.steps) {
                        if (step is RoutineStep.GoToPlace) add(step.place)
                    }
                }
            }
        }
        for (place in PlayScene.Place.entries.filter { PlayScene.isOutdoors(it) }) {
            assertTrue(
                "$place kommt in keinem Ablauf vor - die Kulisse waere nie zu sehen",
                place in reachable
            )
        }
    }
}
