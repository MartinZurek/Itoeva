package com.notime.glyphsim.matrix

import com.notime.glyphsim.living.ActionCatalog
import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.LivingSite
import com.notime.glyphsim.living.LivingSimulation
import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.living.Needs
import com.notime.glyphsim.living.Plan
import com.notime.glyphsim.living.WorldState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Die drei Einwohner leben zwischen den Besuchen weiter** (NT-088).
 *
 * Seit NT-086 haben sie Identitaet, Erinnerung und eigene Ressourcen - aber ihr Zustand bewegte
 * sich nur, wenn der Hauptavatar ihnen zufaellig begegnete. Ein Laden, dessen Verkaufskraft nur
 * existiert, waehrend jemand hinsieht, ist eine Kulisse mit Gedaechtnis.
 *
 * Geprueft werden deshalb Eigenschaften eines LAUFS ueber mehrere simulierte Tage, nicht die
 * Zahlen einzelner Handlungen: dass er wiederholbar ist, dass die drei auseinanderlaufen, dass
 * die Rolle nur neigt und nicht zwingt, und dass die Geldbeutel getrennt bleiben.
 */
class LivingPopulationTest {

    private val start = 6 * WorldState.MINUTES_PER_DAY / 24

    /** Ein Lauf ueber [days] Tage in Halbstundenschritten - wie ihn die Laufzeit faehrt. */
    private fun run(days: Int, step: Int = 30): Map<String, ResidentState> {
        var states = LivingPopulation.initial(start)
        var minute = start
        val ende = start + days * WorldState.MINUTES_PER_DAY
        while (minute < ende) {
            minute += step
            states = LivingPopulation.advance(states, minute)
        }
        return states
    }

    /**
     * **Ohne Wiederholbarkeit waere jede andere Zusicherung wertlos.**
     *
     * Der Kern wuerfelt nicht, und das Interessenthema kommt aus einer festen Rotation statt aus
     * einem Zufallsgenerator. Nur deshalb laesst sich ein Mehrtageslauf ueberhaupt als Beleg
     * heranziehen - ein einmaliges Ergebnis waere eine Anekdote.
     */
    @Test
    fun `zwei Laeufe ueber drei Tage ergeben denselben Zustand`() {
        assertEquals(LivingPopulation.snapshot(run(3)), LivingPopulation.snapshot(run(3)))
    }

    /**
     * **Verschiedene Historien, nicht drei Kopien mit anderen Namen.**
     *
     * Rolle, Ankerort und Interessenneigung unterscheiden die drei nur am Anfang. Liefen sie
     * danach synchron, waere die Bevoelkerung eine Figur in drei Farben.
     */
    @Test
    fun `die drei Einwohner entwickeln verschiedene Historien`() {
        val states = run(5)
        val schnappschuesse = LivingPopulation.snapshot(states)
        assertEquals(LivingResidents.all.size, schnappschuesse.size)

        // Gelernter Geschmack und Erinnerung entstehen aus dem, was jeder wirklich getan hat.
        val geschmack = LivingResidents.all.map { states.getValue(it.profileId).agent.learnedPreferences }
        assertEquals(
            "Zwei Einwohner haben denselben gelernten Geschmack",
            LivingResidents.all.size,
            geschmack.distinct().size
        )

        val episoden = LivingResidents.all.map {
            states.getValue(it.profileId).agent.episodes.map { e -> e.event.kind to e.event.action }
        }
        assertEquals(
            "Zwei Einwohner haben dieselbe Erinnerung",
            LivingResidents.all.size,
            episoden.distinct().size
        )
    }

    /**
     * **Die Geldbeutel bleiben getrennt** - jeder Einwohner hat seine eigene Welt.
     *
     * `WorldState` traegt Muenzen und Vorrat. Eine gemeinsame Welt haette die Verkaufskraft aus
     * dem Geldbeutel des Parkgastes bezahlen lassen; genau dieselbe Falle lauerte in NT-086 beim
     * Gast gegenueber dem Hauptavatar.
     */
    @Test
    fun `Muenzen und Vorrat gehoeren je einem Profil`() {
        val states = run(4)
        for ((profileId, state) in states) {
            assertEquals(
                "Der Zustand liegt unter einer fremden Kennung",
                profileId,
                state.agent.profileId
            )
        }
        // **Nicht ueber verschiedene Kontostaende geprueft, und das ist Absicht.** Gemessen
        // stehen nach vier Tagen alle drei auf null Muenzen und null Portionen - nicht weil sie
        // sich eine Welt teilen, sondern weil Lohn und Einkauf beide 2 kosten: ein
        // Subsistenzkreislauf ohne Puffer, in dem jeder immer wieder durch dieselben Werte
        // laeuft. Gleiche Zahlen waeren hier also gar kein Beleg fuer eine geteilte Welt.
        //
        // Geprueft wird stattdessen die Trennung selbst: Wer einem Einwohner Geld gibt, darf
        // damit keinen anderen reicher machen.
        val reich = states.mapValues { (profileId, state) ->
            if (profileId == LivingResidents.all.first().profileId) {
                state.copy(world = state.world.copy(coins = 99))
            } else {
                state
            }
        }
        val danach = LivingPopulation.advance(reich, reich.values.maxOf { it.world.absoluteMinute } + 120)
        for (resident in LivingResidents.all.drop(1)) {
            assertTrue(
                "${resident.profileId} wurde vom fremden Geldbeutel beruehrt",
                danach.getValue(resident.profileId).world.coins < 99
            )
        }
    }

    /**
     * **Rolle ist Neigung, nicht Zwang.**
     *
     * Der ausdrueckliche Auftrag aus NT-088: Auch die Verkaufskraft darf bei dringendem Hunger
     * den Laden verlassen. Geprueft wird genau das - dieselbe Figur, einmal satt und einmal
     * hungrig, und die Entscheidung muss sich unterscheiden.
     */
    @Test
    fun `eine hungrige Verkaufskraft verlaesst ihren Laden`() {
        val laden = LivingResidents.all.first { it.role == ResidentRole.SHOPKEEPER }
        val amPlatz = ResidentState(
            agent = LivingResidents.initialAgent(laden),
            world = LivingResidents.initialWorld(laden, 10 * 60)
                .copy(site = LivingSite.WORKPLACE, portions = 0, coins = 2)
        )
        val hungrig = amPlatz.copy(
            agent = amPlatz.agent.copy(
                needs = Needs.of(NeedKind.HUNGER to 0.95),
                goal = null,
                plan = null
            )
        )
        // **Die Bahn zaehlt, nicht der Endzustand.** Der erste Entwurf dieses Tests sah vier
        // Stunden spaeter nach und fand REST - weil sie da laengst gegessen hatte und muede war.
        // Die Zusicherung war nicht falsch gerechnet, sondern am falschen Zeitpunkt gemessen.
        var zustand = hungrig
        var minute = 10 * 60
        val ziele = mutableListOf<GoalKind?>()
        val orte = mutableListOf<LivingSite>()
        repeat(6) {
            minute += 30
            zustand = LivingPopulation.advance(mapOf(laden.profileId to zustand), minute)
                .getValue(laden.profileId)
            ziele += zustand.agent.goal
            orte += zustand.world.site
        }

        assertTrue(
            "Dringender Hunger setzt sich nicht gegen die Rolle durch: $ziele",
            GoalKind.GET_FOOD in ziele
        )
        assertTrue(
            "Die Verkaufskraft bleibt trotz Hunger an ihrem Arbeitsplatz stehen: $orte",
            orte.any { it != LivingSite.WORKPLACE }
        )
        // Und sie geht den wirklichen Weg: erst einkaufen, dann nach Hause essen.
        assertTrue("Sie war nie am Markt: $orte", LivingSite.MARKET in orte)
        assertTrue("Sie hat nie zu Hause gegessen: $orte", LivingSite.HOME in orte)
    }

    /**
     * **Oeffnungszeiten muessen Entscheidungen veraendern koennen.**
     *
     * `WorldState.advanced` bewegt die Zeit, aber nicht `openSites`. Ohne das Nachfuehren bei
     * jedem Schritt truege ein um sieben Uhr angelegter Einwohner bis in die Nacht die
     * Oeffnungszeiten von sieben Uhr mit sich herum - der Laden haette fuer ihn nie geschlossen,
     * und `SiteOpen` waere als benanntes Hindernis wirkungslos.
     */
    @Test
    fun `die Oeffnungszeiten wandern mit der Tageszeit mit`() {
        var states = LivingPopulation.initial(9 * 60)
        val gesehen = mutableSetOf<Set<LivingSite>>()
        var minute = 9 * 60
        repeat(40) {
            minute += 60
            states = LivingPopulation.advance(states, minute)
            gesehen += states.values.first().world.openSites
        }
        assertTrue(
            "Die offenen Orte haben sich ueber einen ganzen Tag nie geaendert: $gesehen",
            gesehen.size > 1
        )
        assertTrue(
            "Es gab keinen Zeitpunkt, an dem der Markt zu war",
            gesehen.any { LivingSite.MARKET !in it }
        )
    }

    /**
     * **Der Ankerort vertritt auch die Arbeit** - und das war zuerst falsch.
     *
     * Die Verkaufskraft arbeitet im Laden, aber die Domaene kennt fuer Arbeit nur `WORKPLACE`,
     * und `siteFor(SHOP)` ist `MARKET`. Ohne [LivingResident.anchorSites] fiel sie beim Arbeiten
     * auf die Kulisse WORK durch und stand im eigenen Laden nur dann, wenn sie dort gerade
     * EINKAUFTE - gemessen neun von 240 Schnappschuessen, nach der Korrektur sechsunddreissig.
     */
    @Test
    fun `die Verkaufskraft steht beim Arbeiten in ihrem eigenen Laden`() {
        val laden = LivingResidents.all.first { it.role == ResidentRole.SHOPKEEPER }
        val amArbeitsplatz = LivingResidents.initialWorld(laden, 10 * 60)
            .copy(site = LivingSite.WORKPLACE)
        assertEquals(
            PlayScene.Place.SHOP,
            LivingPopulation.placeFor(laden, amArbeitsplatz)
        )
        // Zu Hause ist niemand oeffentlich zu sehen - das ist eine Aussage, kein Fehlwert.
        assertNull(
            LivingPopulation.placeFor(laden, amArbeitsplatz.copy(site = LivingSite.HOME))
        )
    }

    /**
     * **Oeffentliche Orte werden wirklich belebt.**
     *
     * Die Zusicherung aus NT-088 im Wortlaut. Geprueft wird die Eigenschaft, nicht die Zahl: Jeder
     * Einwohner muss innerhalb seines Anwesenheitsfensters regelmaessig oeffentlich zu sehen sein,
     * und sein Ankerort muss darunter vorkommen - sonst waere die Rolle nur ein Etikett.
     */
    @Test
    fun `jeder Einwohner belebt seinen Ankerort ueber fuenf Tage`() {
        var states = LivingPopulation.initial(start)
        val gesehen = LivingResidents.all.associate { it.profileId to mutableListOf<PlayScene.Place?>() }
        var minute = start
        while (minute < start + 5 * WorldState.MINUTES_PER_DAY) {
            minute += 30
            states = LivingPopulation.advance(states, minute)
            for (s in LivingPopulation.snapshot(states)) {
                if (s.publiclyPresent) gesehen.getValue(s.profileId) += s.place
            }
        }
        for (resident in LivingResidents.all) {
            val orte = gesehen.getValue(resident.profileId)
            assertTrue(
                "${resident.profileId} war in fuenf Tagen nur ${orte.size} Mal oeffentlich zu sehen",
                orte.size >= 15
            )
            assertTrue(
                "${resident.profileId} war nie an seinem Ankerort ${resident.anchorPlace}",
                orte.any { it == resident.anchorPlace }
            )
        }
    }

    /**
     * Der Schnappschuss ist vollstaendig ABGELEITET und gibt den Kern nicht preis - wer ihn
     * bekommt, kann beobachten und nichts veraendern.
     */
    @Test
    fun `der Schnappschuss beschreibt dieselbe Lage wie der Zustand`() {
        val states = run(2)
        for (s in LivingPopulation.snapshot(states)) {
            val zustand = states.getValue(s.profileId)
            assertEquals(zustand.world.site, s.site)
            assertEquals(zustand.agent.goal, s.goal)
            assertEquals(zustand.agent.plan?.next?.kind, s.nextAction)
            assertEquals(zustand.world.coins, s.coins)
            assertEquals(zustand.world.portions, s.portions)
            assertEquals(zustand.world.minuteOfDay, s.minuteOfDay)
        }
    }

    /**
     * **Das Signal, das NT-092 zuerst fehlte.** Ohne ein eigenes, vom Hauptavatar unabhaengiges
     * Zeichen dafuer, WELCHE Sonderaktivitaet ein `MOVE_BODY` meint, waere jede gemeinsame
     * Sportszene nur eine zufaellige Koinzidenz zweier generischer Handlungen gewesen - genau das
     * hat ein automatisches Review am ersten Entwurf zu Recht zurueckgewiesen (siehe
     * `EVOLUTION.md`, NT-092). Geprueft wird hier die Eigenschaft, nicht ein Einzelwert: Das
     * Feld ist gesetzt genau dann, wenn `MOVE_BODY` wirklich geplant ist, es ist immer eine der
     * beiden am Sportplatz moeglichen Aktivitaeten, und derselbe Einwohner am selben Tag ergibt
     * wieder denselben Wert.
     */
    @Test
    fun `nextSpecialActivity ist gesetzt genau dann wenn wirklich MOVE_BODY geplant ist`() {
        val states = run(5)
        for (s in LivingPopulation.snapshot(states)) {
            if (s.nextAction == ActionKind.MOVE_BODY) {
                assertTrue(
                    "${s.profileId} plant MOVE_BODY, traegt aber kein Sportsignal",
                    s.nextSpecialActivity in setOf(
                        PlayRoutines.SpecialActivity.TRAINING,
                        PlayRoutines.SpecialActivity.BASKETBALL
                    )
                )
            } else {
                assertNull(
                    "${s.profileId} plant kein MOVE_BODY, traegt aber ein Sportsignal",
                    s.nextSpecialActivity
                )
            }
        }
    }

    @Test
    fun `specialActivityFor ist deterministisch und wiederholt sich nicht bei jedem Tag gleich`() {
        val athlet = LivingResidents.all.first { it.role == ResidentRole.ATHLETE }
        val welt = LivingResidents.initialWorld(athlet, 10 * 60)

        val ersterLauf = (0 until 6).map {
            LivingPopulation.specialActivityFor(athlet, welt.copy(day = it))
        }
        val zweiterLauf = (0 until 6).map {
            LivingPopulation.specialActivityFor(athlet, welt.copy(day = it))
        }
        assertEquals("gleicher Tag muss denselben Wert ergeben", ersterLauf, zweiterLauf)
        assertTrue(
            "ueber sechs Tage sollten beide Aktivitaeten vorkommen, nicht immer dieselbe",
            ersterLauf.toSet().size == 2
        )
    }

    @Test
    fun `sichtbarer gemeinsamer Abschluss nutzt die wirkliche Bewegungswirkung`() {
        val resident = LivingResidents.all.first { it.role == ResidentRole.ATHLETE }
        val before = ResidentState(
            agent = LivingResidents.initialAgent(resident).copy(
                goal = GoalKind.HAVE_FUN,
                plan = Plan(
                    GoalKind.HAVE_FUN,
                    listOf(ActionCatalog[ActionKind.MOVE_BODY])
                )
            ),
            world = LivingResidents.initialWorld(resident, 10 * 60)
                .copy(site = LivingSite.OUTSIDE)
        )

        val result = LivingPopulation.completeSharedAction(before, ActionKind.MOVE_BODY)
        requireNotNull(result)

        assertTrue(result.world.absoluteMinute > before.world.absoluteMinute)
        assertTrue(
            result.agent.needs.pressure(NeedKind.FUN) <
                before.agent.needs.pressure(NeedKind.FUN)
        )
        assertTrue(result.agent.plan?.isDone == true)
    }

    @Test
    fun `unpassender Einwohnerplan wird nicht fuer das Bild umgedeutet`() {
        val resident = LivingResidents.all.first { it.role == ResidentRole.ATHLETE }
        val before = ResidentState(
            agent = LivingResidents.initialAgent(resident).copy(
                goal = GoalKind.DEVELOP,
                plan = Plan(GoalKind.DEVELOP, listOf(ActionCatalog[ActionKind.READ]))
            ),
            world = LivingResidents.initialWorld(resident, 10 * 60)
                .copy(site = LivingSite.OUTSIDE)
        )

        assertNull(LivingPopulation.completeSharedAction(before, ActionKind.MOVE_BODY))
    }

    @Test
    fun `vollstaendiges gemeinsames Training wird von beiden gegenseitig erinnert`() {
        val resident = LivingResidents.all.first { it.role == ResidentRole.ATHLETE }
        val hostBefore = LivingResidents.initialAgent(resident).copy(
            profileId = "PUFFLING",
            goal = GoalKind.HAVE_FUN,
            plan = Plan(GoalKind.HAVE_FUN, listOf(ActionCatalog[ActionKind.MOVE_BODY]))
        )
        val hostWorld = LivingResidents.initialWorld(resident, 10 * 60)
            .copy(site = LivingSite.OUTSIDE)
        val hostMoved = LivingSimulation.step(hostBefore, hostWorld)
        val residentBefore = ResidentState(
            agent = LivingResidents.initialAgent(resident).copy(
                goal = GoalKind.HAVE_FUN,
                plan = Plan(GoalKind.HAVE_FUN, listOf(ActionCatalog[ActionKind.MOVE_BODY]))
            ),
            world = LivingResidents.initialWorld(resident, 10 * 60)
                .copy(site = LivingSite.OUTSIDE)
        )

        val completed = LivingPopulation.completeSharedTraining(hostMoved, residentBefore)
        requireNotNull(completed)

        val hostEpisode = completed.host.agent.episodes.last()
        val residentEpisode = completed.resident.agent.episodes.last()
        assertEquals(ActionKind.TRAIN_TOGETHER, hostEpisode.event.action)
        assertEquals(resident.profileId, hostEpisode.event.counterpartProfileId)
        assertEquals(ActionKind.TRAIN_TOGETHER, residentEpisode.event.action)
        assertEquals("PUFFLING", residentEpisode.event.counterpartProfileId)
        assertEquals(
            ActionCatalog.SHARED_TRAINING_CLOSENESS,
            completed.host.agent.relationships.getValue(resident.profileId).closeness,
            1e-9
        )
        assertEquals(
            ActionCatalog.SHARED_TRAINING_CLOSENESS,
            completed.resident.agent.relationships.getValue("PUFFLING").closeness,
            1e-9
        )
    }

    @Test
    fun `eine gewoehnliche Bewegung erzeugt keine gemeinsame Erinnerung`() {
        val resident = LivingResidents.all.first { it.role == ResidentRole.ATHLETE }
        val before = LivingResidents.initialAgent(resident).copy(
            profileId = "PUFFLING",
            goal = GoalKind.HAVE_FUN,
            plan = Plan(GoalKind.HAVE_FUN, listOf(ActionCatalog[ActionKind.MOVE_BODY]))
        )
        val moved = LivingSimulation.step(
            before,
            LivingResidents.initialWorld(resident, 10 * 60).copy(site = LivingSite.OUTSIDE)
        )

        assertTrue(moved.agent.relationships.isEmpty())
        assertTrue(moved.agent.episodes.none { it.event.action == ActionKind.TRAIN_TOGETHER })
        assertTrue(moved.agent.episodes.none { it.event.counterpartProfileId != null })
    }

    @Test
    fun `fehlender Abschluss auf einer Seite veraendert keinen Eingangszustand`() {
        val resident = LivingResidents.all.first { it.role == ResidentRole.ATHLETE }
        val hostBefore = LivingResidents.initialAgent(resident).copy(
            profileId = "PUFFLING",
            goal = GoalKind.DEVELOP,
            plan = Plan(GoalKind.DEVELOP, listOf(ActionCatalog[ActionKind.READ]))
        )
        val hostResult = LivingSimulation.step(
            hostBefore,
            LivingResidents.initialWorld(resident, 10 * 60).copy(site = LivingSite.OUTSIDE)
        )
        val residentBefore = ResidentState(
            agent = LivingResidents.initialAgent(resident).copy(
                goal = GoalKind.HAVE_FUN,
                plan = Plan(GoalKind.HAVE_FUN, listOf(ActionCatalog[ActionKind.MOVE_BODY]))
            ),
            world = LivingResidents.initialWorld(resident, 10 * 60)
                .copy(site = LivingSite.OUTSIDE)
        )

        assertNull(LivingPopulation.completeSharedTraining(hostResult, residentBefore))
        assertTrue(hostResult.agent.relationships.isEmpty())
        assertTrue(residentBefore.agent.relationships.isEmpty())
        assertTrue(hostResult.agent.episodes.none { it.event.action == ActionKind.TRAIN_TOGETHER })
        assertTrue(
            residentBefore.agent.episodes.none {
                it.event.action == ActionKind.TRAIN_TOGETHER
            }
        )
    }

    @Test
    fun `fehlender Einwohnerabschluss hinterlaesst auch beim Host keine soziale Spur`() {
        val resident = LivingResidents.all.first { it.role == ResidentRole.ATHLETE }
        val hostBefore = LivingResidents.initialAgent(resident).copy(
            profileId = "PUFFLING",
            goal = GoalKind.HAVE_FUN,
            plan = Plan(GoalKind.HAVE_FUN, listOf(ActionCatalog[ActionKind.MOVE_BODY]))
        )
        val hostMoved = LivingSimulation.step(
            hostBefore,
            LivingResidents.initialWorld(resident, 10 * 60).copy(site = LivingSite.OUTSIDE)
        )
        val residentBefore = ResidentState(
            agent = LivingResidents.initialAgent(resident).copy(
                goal = GoalKind.DEVELOP,
                plan = Plan(GoalKind.DEVELOP, listOf(ActionCatalog[ActionKind.READ]))
            ),
            world = LivingResidents.initialWorld(resident, 10 * 60)
                .copy(site = LivingSite.OUTSIDE)
        )

        assertNull(LivingPopulation.completeSharedTraining(hostMoved, residentBefore))
        assertTrue(hostMoved.agent.relationships.isEmpty())
        assertTrue(residentBefore.agent.relationships.isEmpty())
        assertTrue(hostMoved.agent.episodes.none { it.event.action == ActionKind.TRAIN_TOGETHER })
        assertTrue(
            residentBefore.agent.episodes.none {
                it.event.action == ActionKind.TRAIN_TOGETHER
            }
        )
    }

    /**
     * **Draussen trifft man sich** - gemeldet als "noch nie drei oder vier zusammen gesehen".
     *
     * Vorher vertrat fuer jeden Bewohner mit Ankerort unter freiem Himmel immer der Ankerort das
     * ganze Draussen: Hootlet stand nie im Park, obwohl der Park zu ihren Besuchsorten gehoert.
     * Nachmittags ist der Park jetzt der Treffpunkt.
     */
    @Test
    fun `wer nachmittags draussen ist, trifft sich im Park`() {
        val hootlet = LivingResidents.all.first { it.species == AvatarSpecies.HOOTLET }
        val draussen = LivingResidents.initialWorld(hootlet, 15 * 60).copy(site = LivingSite.OUTSIDE)
        assertEquals(PlayScene.Place.PARK, LivingPopulation.placeFor(hootlet, draussen))
        // Wer als Naechstes Sport treibt, bleibt dafuer auf dem Sportplatz - sonst fehlte der
        // gemeinsamen Sportplatz-Szene der Partner.
        assertEquals(
            PlayScene.Place.SPORT,
            LivingPopulation.placeFor(hootlet, draussen, keepAnchor = true)
        )
        // Wer den Treffpunkt nicht kennt, bleibt, wo er hingehoert: Gloop geht nicht in den Park.
        val gloop = LivingResidents.all.first { it.species == AvatarSpecies.GLOOP }
        val gloopDraussen = LivingResidents.initialWorld(gloop, 15 * 60).copy(site = LivingSite.OUTSIDE)
        assertEquals(PlayScene.Place.CITY, LivingPopulation.placeFor(gloop, gloopDraussen))
        // Abends trifft man sich in der Spielhalle - aber nur, wer sie kennt.
        assertEquals(
            PlayScene.Place.ARCADE,
            LivingPopulation.placeFor(
                gloop,
                LivingResidents.initialWorld(gloop, 20 * 60 + 30).copy(site = LivingSite.OUTSIDE)
            )
        )
        val abends = LivingResidents.initialWorld(hootlet, 20 * 60 + 30).copy(site = LivingSite.OUTSIDE)
        assertEquals(PlayScene.Place.SPORT, LivingPopulation.placeFor(hootlet, abends))
    }

    /** Es gibt genau einen Laden - wer einkauft, steht dort und ist nicht unsichtbar. */
    @Test
    fun `wer einkauft, steht im Laden`() {
        val puffling = LivingResidents.all.first { it.species == AvatarSpecies.PUFFLING }
        assertTrue(PlayScene.Place.SHOP !in puffling.visitPlaces)
        val amMarkt = LivingResidents.initialWorld(puffling, 11 * 60).copy(site = LivingSite.MARKET)
        assertEquals(PlayScene.Place.SHOP, LivingPopulation.placeFor(puffling, amMarkt))
    }

    /**
     * **Der Beleg aus der Messung.** Vor der Aenderung standen ueber eine ganze Woche nie mehr als
     * zwei Bewohner gleichzeitig an einem Ort unter freiem Himmel. Jetzt kommt es vor, dass sich
     * drei treffen - selten, weil jeder nur etwa ein Fuenftel des Tages draussen ist, aber es
     * kommt vor.
     */
    @Test
    fun `im Lauf einer Woche treffen sich draussen einmal drei`() {
        var states = LivingPopulation.initial(start)
        var minute = start
        val ende = start + 7 * WorldState.MINUTES_PER_DAY
        var groesstesGrueppchen = 0
        while (minute < ende) {
            minute += 5
            states = LivingPopulation.advance(states, minute)
            val anwesend = LivingPopulation.snapshot(states)
                .filter { it.publiclyPresent && it.place != null && PlayScene.isOutdoors(it.place!!) }
                .groupingBy { it.place }
                .eachCount()
            groesstesGrueppchen = maxOf(groesstesGrueppchen, anwesend.values.maxOrNull() ?: 0)
        }
        assertTrue("groesstes Grueppchen: $groesstesGrueppchen", groesstesGrueppchen >= 3)
    }

    /**
     * **Freizeit draussen: aus der Ausnahme wird ein Nachmittag.** Seit die Bewohner tagsueber
     * ihre Freizeit draussen verbringen, stehen zwischen 8 und 20 Uhr in einem spuerbaren Teil
     * der Zeit drei oder mehr an einem Ort unter freiem Himmel (gemessen: Park 9 %, Strasse
     * 10 %), und auch vier zusammen kommen vor. Vorher waren es drei hoechstens ein- oder zweimal
     * in der Woche.
     */
    @Test
    fun `tagsueber treffen sich draussen oft drei und manchmal vier`() {
        var states = LivingPopulation.initial(start)
        var minute = start
        val ende = start + 7 * WorldState.MINUTES_PER_DAY
        var stichproben = 0
        var mitDreien = 0
        var groesstesGrueppchen = 0
        while (minute < ende) {
            minute += 5
            states = LivingPopulation.advance(states, minute)
            val stunde = (minute % WorldState.MINUTES_PER_DAY) / 60
            if (stunde !in LivingPopulation.OUTDOOR_LEISURE_HOURS) continue
            stichproben++
            val groesste = LivingPopulation.snapshot(states)
                .filter { it.publiclyPresent && it.place != null && PlayScene.isOutdoors(it.place!!) }
                .groupingBy { it.place }
                .eachCount()
                .values.maxOrNull() ?: 0
            if (groesste >= 3) mitDreien++
            groesstesGrueppchen = maxOf(groesstesGrueppchen, groesste)
        }
        assertTrue("drei zusammen in $mitDreien von $stichproben", mitDreien * 10 >= stichproben)
        assertTrue("groesstes Grueppchen: $groesstesGrueppchen", groesstesGrueppchen >= 4)
    }

    @Test
    fun `Freizeit draussen nur tagsueber und nur im eigenen Anwesenheitsfenster`() {
        val puffling = LivingResidents.all.first { it.species == AvatarSpecies.PUFFLING }
        assertEquals(
            LivingSite.OUTSIDE,
            LivingPopulation.leisureSiteFor(puffling, LivingResidents.initialWorld(puffling, 14 * 60))
        )
        assertNull(LivingPopulation.leisureSiteFor(puffling, LivingResidents.initialWorld(puffling, 21 * 60)))
        assertNull(LivingPopulation.leisureSiteFor(puffling, LivingResidents.initialWorld(puffling, 7 * 60)))
    }

    /**
     * **Die sichtbaren Einwohner tun meistens etwas Erkennbares.** Gemessen ueber eine Woche
     * (8-20 Uhr) haben gut zwei Drittel der oeffentlich anwesenden Einwohner eine Handlung, die
     * sich zeigen laesst - vorher standen alle mit derselben Ruhe-Animation da. Der Test haelt
     * die Haelfte als Untergrenze fest und prueft, dass die Handlung aus dem eigenen Ereignis
     * des Einwohners stammt.
     */
    @Test
    fun `sichtbare Einwohner zeigen meistens eine Handlung`() {
        var states = LivingPopulation.initial(start)
        var minute = start
        val ende = start + 7 * WorldState.MINUTES_PER_DAY
        var sichtbar = 0
        var tunEtwas = 0
        while (minute < ende) {
            minute += 5
            states = LivingPopulation.advance(states, minute)
            val stunde = (minute % WorldState.MINUTES_PER_DAY) / 60
            if (stunde !in LivingPopulation.OUTDOOR_LEISURE_HOURS) continue
            for (s in LivingPopulation.snapshot(states)) {
                if (!s.publiclyPresent) continue
                sichtbar++
                val zustand = states.getValue(s.profileId)
                if (s.currentAction != null) {
                    assertEquals(zustand.agent.lastEvent?.action, s.currentAction)
                }
                if (LivingPopulationLayout.poseFor(s) is LivingPopulationLayout.ResidentPose.Doing) tunEtwas++
            }
        }
        assertTrue("$tunEtwas von $sichtbar", tunEtwas * 2 >= sichtbar)
    }
}

