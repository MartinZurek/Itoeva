package com.notime.glyphsim.living

import com.notime.glyphsim.matrix.AvatarSpecies
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft den Living-Agent-Kern - **das Verhalten, nicht die Datenklassen.**
 *
 * Am Geraet waere davon nichts zu belegen: Man muesste einen Avatar hungrig werden lassen, ihm
 * das Geld wegnehmen, den Laden schliessen und dann Stunden zusehen, ohne hinterher sagen zu
 * koennen, ob er wegen der Lage arbeiten ging oder weil eine Animation an der Reihe war. Genau
 * deshalb ist diese Schicht rein: kein Android, keine Uhr, kein Zufall.
 */
class LivingAgentTest {

    private val alleOffen = setOf(LivingSite.HOME, LivingSite.WORKPLACE, LivingSite.MARKET)

    private fun welt(
        coins: Int = 0,
        portions: Int = 0,
        site: LivingSite = LivingSite.HOME,
        openSites: Set<LivingSite> = alleOffen,
        minuteOfDay: Int = 8 * 60,
        day: Int = 0,
        nearbyProfiles: Set<String> = emptySet()
    ) = WorldState(day, minuteOfDay, site, coins, portions, openSites, nearbyProfiles)

    private fun agent(
        hunger: Double = 0.0,
        species: AvatarSpecies = AvatarSpecies.PUFFLING,
        vararg weitere: Pair<NeedKind, Double>
    ) = AgentState(
        profileId = species.name,
        personality = Personality.of(species),
        needs = Needs.of(NeedKind.HUNGER to hunger, *weitere)
    )

    // ================= Die Zielwahl =================

    @Test
    fun `ein satter und ausgeruhter Agent will nichts`() {
        // Die Grenze, ohne die alles Weitere sinnlos waere: Ein Wesen, das immer irgendein Ziel
        // hat, hat keines.
        assertNull(UtilitySelector.choose(agent(hunger = 0.0), welt(portions = 3)))
    }

    @Test
    fun `draengender Hunger gewinnt die Zielwahl`() {
        assertEquals(GoalKind.GET_FOOD, UtilitySelector.choose(agent(hunger = 0.8), welt(portions = 3)))
    }

    @Test
    fun `die Rangfolge nennt Druck, Bias und Kosten getrennt`() {
        // Ohne diese Aufschluesselung waere "warum will er das?" nur mit einem Debugger zu
        // beantworten - und der Meilenstein verlangt eine programmatische Antwort.
        val rang = UtilitySelector.rank(agent(hunger = 0.8), welt(portions = 3))
        val essen = rang.first { it.goal == GoalKind.GET_FOOD }
        assertEquals(0.8, essen.needPressure, 1e-9)
        assertTrue("Kosten muessen einfliessen", essen.cost > 0.0)
        assertEquals(
            essen.needPressure + essen.bias + essen.learnedPreference +
                essen.memoryInfluence + essen.socialValue - essen.cost,
            essen.total,
            1e-9
        )
    }

    @Test
    fun `dieselbe Lage ergibt zweimal dieselbe Wahl`() {
        val a = agent(hunger = 0.5, weitere = arrayOf(NeedKind.FUN to 0.5))
        val w = welt(portions = 1)
        assertEquals(UtilitySelector.rank(a, w), UtilitySelector.rank(a, w))
        assertEquals(LivingSimulation.step(a, w).events, LivingSimulation.step(a, w).events)
    }

    @Test
    fun `ein unerreichbares Ziel verliert gegen ein erreichbares`() {
        // Hunger ist draengender als Langeweile - aber nachts ist weder Laden noch Arbeit offen,
        // und dann ist Essen schlicht kein Weg. Der Agent tut das, was geht.
        val nachts = welt(openSites = setOf(LivingSite.HOME), minuteOfDay = 2 * 60)
        val gewaehlt = UtilitySelector.choose(
            agent(hunger = 0.9, weitere = arrayOf(NeedKind.FUN to 0.4)),
            nachts
        )
        assertEquals(GoalKind.HAVE_FUN, gewaehlt)
    }

    // ================= Der Ressourcenpfad =================

    @Test
    fun `mit vollem Vorrat wird nur nachgesehen und gegessen`() {
        val plan = Planner.planFor(GoalKind.GET_FOOD, welt(portions = 2))
        assertEquals(listOf(ActionKind.INSPECT_FOOD, ActionKind.EAT), plan?.kinds)
    }

    @Test
    fun `leerer Vorrat mit Geld ergibt Einkauf statt Arbeit`() {
        val plan = Planner.planFor(GoalKind.GET_FOOD, welt(coins = 2, portions = 0))
        assertEquals(
            listOf(
                ActionKind.TRAVEL, ActionKind.BUY_FOOD, ActionKind.TRAVEL,
                ActionKind.INSPECT_FOOD, ActionKind.EAT
            ),
            plan?.kinds
        )
        assertTrue("Arbeit gehoert hier nicht hinein", ActionKind.WORK !in plan!!.kinds)
    }

    @Test
    fun `ohne Vorrat und ohne Geld entsteht Arbeit vor Einkauf vor Essen`() {
        // Die Kette des Meilensteins - und sie steht nirgends als Kette im Code, sondern faellt
        // aus denselben drei Bedingungen ab wie die beiden Faelle darueber.
        val plan = Planner.planFor(GoalKind.GET_FOOD, welt(coins = 0, portions = 0))!!
        val tragend = plan.kinds.filter { it != ActionKind.TRAVEL }
        assertEquals(
            listOf(ActionKind.WORK, ActionKind.BUY_FOOD, ActionKind.INSPECT_FOOD, ActionKind.EAT),
            tragend
        )
    }

    @Test
    fun `der ganze Weg wird durchlaufen und der Hunger sinkt am Ende`() {
        val start = agent(hunger = 0.9)
        // Genau sieben Schritte: drei Wege, arbeiten, kaufen, nachsehen, essen. Mehr waeren
        // bequemer und wuerden verdecken, wenn ein Schritt unterwegs blockiert und ersetzt wird.
        val ergebnis = LivingSimulation.run(start, welt(coins = 0, portions = 0), count = 7)

        val getan = ergebnis.events
            .filter { it.kind == LivingEventKind.ACTION_DONE }
            .mapNotNull { it.action }
            .filter { it != ActionKind.TRAVEL }
        assertEquals(
            listOf(ActionKind.WORK, ActionKind.BUY_FOOD, ActionKind.INSPECT_FOOD, ActionKind.EAT),
            getan
        )
        assertTrue(
            "Hunger muss nach dem Essen deutlich kleiner sein: ${ergebnis.agent.needs.pressure(NeedKind.HUNGER)}",
            ergebnis.agent.needs.pressure(NeedKind.HUNGER) < start.needs.pressure(NeedKind.HUNGER) - 0.3
        )
        // Der Lohn ist ausgegeben, der Rest des Einkaufs liegt im Vorrat.
        assertEquals(ActionCatalog.WAGE - ActionCatalog.GROCERY_COST, ergebnis.world.coins)
        assertEquals(ActionCatalog.GROCERY_PORTIONS - 1, ergebnis.world.portions)
    }

    @Test
    fun `Ziel und Handlung bleiben getrennt`() {
        // Dasselbe Ziel, zwei Lagen, zwei voellig verschiedene erste Handlungen. Waeren Ziel und
        // Handlung eins, koennte das nicht sein.
        val satt = Planner.planFor(GoalKind.GET_FOOD, welt(portions = 3))!!
        val pleite = Planner.planFor(GoalKind.GET_FOOD, welt(coins = 0, portions = 0))!!
        assertEquals(satt.goal, pleite.goal)
        assertTrue(satt.kinds.first() != pleite.kinds.first())
    }

    // ================= Voraussetzungen und Neuplanung =================

    @Test
    fun `eine Handlung nennt die Voraussetzung, an der sie scheitert`() {
        val kaufen = ActionCatalog[ActionKind.BUY_FOOD]
        // Am falschen Ort: das erste Hindernis ist der Ort, nicht das fehlende Geld.
        assertEquals(
            Requirement.At(LivingSite.MARKET),
            kaufen.blockedBy(welt(coins = 0, site = LivingSite.HOME))
        )
        assertEquals(
            Requirement.Coins(ActionCatalog.GROCERY_COST),
            kaufen.blockedBy(welt(coins = 0, site = LivingSite.MARKET))
        )
        assertNull(kaufen.blockedBy(welt(coins = 2, site = LivingSite.MARKET)))
    }

    @Test
    fun `aendert sich die Welt zwischen Planen und Tun, faellt der Plan und das Ziel bleibt`() {
        // Der Unterschied zwischen einem lebendigen Wesen und einer Animation: Die feste Folge
        // liefe blind weiter, auch wenn der Laden inzwischen zu hat.
        val hungrig = agent(hunger = 0.9)
        val erster = LivingSimulation.step(hungrig, welt(coins = 2, portions = 0))
        assertEquals(GoalKind.GET_FOOD, erster.agent.goal)
        assertNotNull(erster.agent.plan)

        val ladenZu = erster.world.copy(openSites = setOf(LivingSite.HOME, LivingSite.WORKPLACE))
        val zweiter = LivingSimulation.step(erster.agent, ladenZu)

        assertTrue(
            "Der blockierte Schritt muss gemeldet werden",
            zweiter.events.any { it.kind == LivingEventKind.ACTION_BLOCKED }
        )
        assertTrue(
            "Der Plan muss fallen",
            zweiter.events.any { it.kind == LivingEventKind.PLAN_ABANDONED }
        )
        assertNull("Kein Plan mehr", zweiter.agent.plan)
        assertEquals("Das Ziel bleibt", GoalKind.GET_FOOD, zweiter.agent.goal)
    }

    @Test
    fun `ohne jeden Weg wartet der Agent, statt eine Handlung zu erfinden`() {
        val nachts = welt(coins = 0, portions = 0, openSites = setOf(LivingSite.HOME))
        val ergebnis = LivingSimulation.step(agent(hunger = 0.95).copy(goal = GoalKind.GET_FOOD), nachts)
        assertTrue(ergebnis.events.any { it.kind == LivingEventKind.NO_PLAN })
        assertNull(ergebnis.agent.plan)
        assertTrue("Die Zeit muss trotzdem laufen", ergebnis.world.absoluteMinute > nachts.absoluteMinute)
    }

    // ================= Erklaerbarkeit =================

    @Test
    fun `die Erklaerung beantwortet die fuenf Fragen des Meilensteins`() {
        val nach = LivingSimulation.step(agent(hunger = 0.9), welt(coins = 0, portions = 0))
        val erklaerung = nach.explain()

        assertEquals(NeedKind.HUNGER, erklaerung.strongestNeed)          // was draengt
        assertEquals(GoalKind.GET_FOOD, erklaerung.goal)                 // was will er
        assertTrue(erklaerung.ranking.isNotEmpty())                      // warum
        assertTrue(erklaerung.plan.isNotEmpty())                         // welcher Plan
        assertNotNull(erklaerung.currentAction)                          // welcher Schritt
        assertNotNull(erklaerung.lastEvent)
    }

    @Test
    fun `die Erklaerung benennt das Hindernis`() {
        val hungrig = agent(hunger = 0.9)
        val erster = LivingSimulation.step(hungrig, welt(coins = 2, portions = 0))
        // Dem Agenten wird das Geld genommen, waehrend er auf dem Weg zum Laden ist.
        val ohneGeld = erster.world.copy(coins = 0, site = LivingSite.MARKET)
        val erklaerung = LivingSimulation.explain(erster.agent, ohneGeld)
        assertEquals(Requirement.Coins(ActionCatalog.GROCERY_COST), erklaerung.blockedBy)
    }

    @Test
    fun `die Erklaerung veraendert nichts`() {
        val a = LivingSimulation.step(agent(hunger = 0.9), welt()).agent
        val w = welt(coins = 1)
        val vorher = a to w
        LivingSimulation.explain(a, w)
        assertEquals(vorher, a to w)
    }

    // ================= Persoenlichkeit als Startbias =================

    @Test
    fun `jede Spezies bekommt eine Persoenlichkeit und keine bestimmt allein`() {
        for (species in AvatarSpecies.entries) {
            val p = Personality.of(species)
            assertTrue(
                "$species darf den Beduerfnisdruck nicht ueberstimmen",
                GoalKind.entries.all { p.bias(it) < UtilitySelector.MIN_PRESSURE }
            )
        }
    }

    @Test
    fun `der Bias verschiebt die Wahl, ohne sie zu erzwingen`() {
        // Gloop ruht gern, Hootlet lernt gern. Bei GLEICHEN Beduerfnissen und gleicher Lage
        // entscheidet allein der Startbias - und er entscheidet in beide Richtungen.
        val lage = welt(portions = 3)
        val gloop = AgentState(
            "GLOOP", Personality.of(AvatarSpecies.GLOOP),
            Needs.of(NeedKind.ENERGY to 0.5, NeedKind.GROWTH to 0.4)
        )
        val hootlet = gloop.copy(
            profileId = "HOOTLET", personality = Personality.of(AvatarSpecies.HOOTLET)
        )
        assertEquals(GoalKind.REST, UtilitySelector.choose(gloop, lage))
        assertEquals(GoalKind.DEVELOP, UtilitySelector.choose(hootlet, lage))

        // Und der Bias verliert, sobald ein echtes Beduerfnis draengt.
        val hungrigerGloop = gloop.copy(needs = Needs.of(NeedKind.ENERGY to 0.5, NeedKind.HUNGER to 0.9))
        assertEquals(GoalKind.GET_FOOD, UtilitySelector.choose(hungrigerGloop, lage))
    }

    @Test
    fun `die Persoenlichkeit steht im Agenten und nicht im Enum`() {
        // Die Bedingung dafuer, dass sich ein Wesen spaeter ueberhaupt veraendern kann: Der Wert
        // liegt als Datum im Zustand, nicht hinter einem Nachschlagen in AvatarSpecies.
        val geaendert = agent(hunger = 0.3).let {
            it.copy(personality = it.personality.copy(goalBias = mapOf(GoalKind.REST to 0.19)))
        }
        val lage = welt(portions = 3)
        assertEquals(
            GoalKind.REST,
            UtilitySelector.choose(geaendert.copy(needs = Needs.of(NeedKind.ENERGY to 0.25)), lage)
        )
    }

    // ================= Zeit und Beduerfnisse =================

    @Test
    fun `Beduerfnisse wachsen mit der Zeit und die Nacht wird mitgezaehlt`() {
        val start = Needs.calm()
        val spaeter = start.advanced(minutes = 10 * 60, personality = Personality())
        assertTrue(spaeter.pressure(NeedKind.HUNGER) > start.pressure(NeedKind.HUNGER))

        val kurzVorMitternacht = welt(minuteOfDay = 23 * 60 + 50).advanced(20)
        assertEquals(1, kurzVorMitternacht.day)
        assertEquals(10, kurzVorMitternacht.minuteOfDay)
    }

    @Test
    fun `ein Mehrtageslauf laeuft nicht leer und erzeugt eine lesbare Ereignisfolge`() {
        // Kein vorgegebener Plot: geprueft wird nur, dass ueber mehrere Tage aus Beduerfnissen
        // wiederholt Ziele, Plaene und abgeschlossene Handlungen entstehen.
        val ergebnis = LivingSimulation.run(agent(hunger = 0.4), welt(coins = 0, portions = 1), count = 120)
        val gezaehlt = ergebnis.events.groupingBy { it.kind }.eachCount()

        assertTrue("mehrere Tage: ${ergebnis.world.day}", ergebnis.world.day >= 2)
        assertTrue("Ziele: $gezaehlt", (gezaehlt[LivingEventKind.GOAL_CHOSEN] ?: 0) >= 3)
        assertTrue("Plaene: $gezaehlt", (gezaehlt[LivingEventKind.PLAN_MADE] ?: 0) >= 3)
        assertTrue("Handlungen: $gezaehlt", (gezaehlt[LivingEventKind.ACTION_DONE] ?: 0) >= 10)
        assertTrue(
            "Es muss mehrmals gegessen worden sein",
            ergebnis.events.count { it.action == ActionKind.EAT } >= 2
        )
        assertTrue(
            "Kein Ereignis darf ohne Zeitstempel dastehen",
            ergebnis.events.all { it.atMinute >= 0 }
        )
    }

    @Test
    fun `derselbe Mehrtageslauf ergibt zweimal dasselbe`() {
        val a = agent(hunger = 0.4)
        val w = welt(coins = 1, portions = 1)
        assertEquals(
            LivingSimulation.run(a, w, count = 60).events,
            LivingSimulation.run(a, w, count = 60).events
        )
    }

    // ================= Erinnerung und symbolische Begegnung =================

    @Test
    fun `PLAY und QUESTION werden aus echtem Zustand mit TIRED und NO beantwortet`() {
        val stern = agent(
            species = AvatarSpecies.STARLET,
            weitere = arrayOf(NeedKind.SOCIAL to 0.8)
        )
        val beisammen = welt(nearbyProfiles = setOf("WYRMLING"))
        val einladung = LivingSimulation.step(stern, beisammen)

        assertEquals(GoalKind.CONNECT_WITH, einladung.agent.goal)
        assertEquals(
            setOf(SymbolicIntent.PLAY, SymbolicIntent.QUESTION),
            einladung.messages.single().intents
        )

        val muede = agent(
            species = AvatarSpecies.WYRMLING,
            weitere = arrayOf(NeedKind.SOCIAL to 0.8, NeedKind.ENERGY to 0.9)
        )
        val antwort = LivingSimulation.respondToPlay(
            muede,
            einladung.world.copy(nearbyProfiles = setOf("STARLET")),
            einladung.messages.single()
        )

        assertEquals(
            setOf(SymbolicIntent.TIRED, SymbolicIntent.NO),
            antwort.messages.single().intents
        )
        val beziehung = antwort.agent.relationships.getValue("STARLET")
        assertTrue(beziehung.trust < 0.0)
        assertTrue(beziehung.closeness < 0.0)
        assertEquals(LivingEventKind.SYMBOLS_SENT, beziehung.lastInteraction?.kind)

        val verstanden = LivingSimulation.receiveResponse(
            einladung.agent,
            einladung.world,
            antwort.messages.single()
        )
        assertEquals(
            setOf(SymbolicIntent.TIRED, SymbolicIntent.NO),
            verstanden.agent.lastEvent?.intents
        )
        assertTrue(verstanden.agent.relationships.getValue("WYRMLING").trust < 0.0)
    }

    @Test
    fun `dieselbe Einladung wird bei Kraft und sozialem Bedarf angenommen`() {
        val request = SymbolicMessage(
            senderProfileId = "STARLET",
            recipientProfileId = "WYRMLING",
            intents = setOf(SymbolicIntent.PLAY, SymbolicIntent.QUESTION),
            atMinute = 10
        )
        val bereit = agent(
            species = AvatarSpecies.WYRMLING,
            weitere = arrayOf(NeedKind.SOCIAL to 0.7, NeedKind.ENERGY to 0.1)
        )
        val antwort = LivingSimulation.respondToPlay(
            bereit,
            welt(nearbyProfiles = setOf("STARLET")),
            request
        )

        assertEquals(
            setOf(SymbolicIntent.PLAY, SymbolicIntent.YES),
            antwort.messages.single().intents
        )
        val beziehung = antwort.agent.relationships.getValue("STARLET")
        assertTrue(beziehung.trust > 0.0)
        assertTrue(beziehung.closeness > 0.0)
        assertEquals(GoalKind.CONNECT_WITH, antwort.agent.episodes.last().event.goal)
    }

    @Test
    fun `ein dringendes laufendes Ziel kann eine Spielanfrage verdraengen`() {
        val request = SymbolicMessage(
            senderProfileId = "STARLET",
            recipientProfileId = "WYRMLING",
            intents = setOf(SymbolicIntent.PLAY, SymbolicIntent.QUESTION),
            atMinute = 10
        )
        val hungrig = agent(
            hunger = 0.95,
            species = AvatarSpecies.WYRMLING,
            weitere = arrayOf(NeedKind.SOCIAL to 0.9, NeedKind.ENERGY to 0.1)
        ).copy(goal = GoalKind.GET_FOOD)

        val antwort = LivingSimulation.respondToPlay(
            hungrig,
            welt(nearbyProfiles = setOf("STARLET")),
            request
        )
        assertEquals(
            setOf(SymbolicIntent.FOOD, SymbolicIntent.NO),
            antwort.messages.single().intents
        )
    }

    @Test
    fun `Episoden bleiben verdichtet und an einer festen Grenze`() {
        var zustand = agent(weitere = arrayOf(NeedKind.FUN to 0.8))
        var lage = welt()
        repeat(AgentState.MAX_EPISODES + 7) {
            val angewandt = ActionCatalog[ActionKind.PURSUE_INTEREST]
                .applyTo(zustand, lage, GoalKind.HAVE_FUN)
            zustand = angewandt.agent
            lage = angewandt.world
        }

        assertEquals(AgentState.MAX_EPISODES, zustand.episodes.size)
        assertTrue(zustand.episodes.all { it.event.kind == LivingEventKind.ACTION_DONE })
        assertTrue(zustand.episodes.first().event.atMinute > 60)
    }

    @Test
    fun `Erinnerung Beziehung und Geschmack bleiben in der Zielerklaerung sichtbar`() {
        val vergangenheit = LivingEvent(
            kind = LivingEventKind.SYMBOLS_RECEIVED,
            atMinute = 30,
            goal = GoalKind.CONNECT_WITH,
            action = ActionKind.RECEIVE_RESPONSE,
            counterpartProfileId = "STARLET",
            intents = setOf(SymbolicIntent.PLAY, SymbolicIntent.YES)
        )
        val mitErfahrung = agent(
            species = AvatarSpecies.WYRMLING,
            weitere = arrayOf(NeedKind.SOCIAL to 0.6)
        ).copy(
            learnedPreferences = mapOf(GoalKind.CONNECT_WITH to 0.08),
            episodes = listOf(Episode(vergangenheit, 1)),
            relationships = mapOf(
                "STARLET" to RelationshipState(
                    trust = 0.5,
                    closeness = 0.4,
                    interactions = 2,
                    lastInteraction = vergangenheit
                )
            )
        )
        val erklaerung = LivingSimulation.explain(
            mitErfahrung,
            welt(nearbyProfiles = setOf("STARLET"))
        )
        val verbinden = erklaerung.ranking.first { it.goal == GoalKind.CONNECT_WITH }

        assertEquals(0.08, verbinden.learnedPreference, 1e-9)
        assertTrue(verbinden.memoryInfluence > 0.0)
        assertTrue(verbinden.socialValue > 0.0)
        assertEquals(listOf(Episode(vergangenheit, 1)), erklaerung.influentialEpisodes)
        assertEquals(2, erklaerung.relationships.getValue("STARLET").interactions)
    }

    @Test
    fun `aehnlich gestartete Agenten entwickeln ohne Plot verschiedene Geschichten`() {
        val beduerfnisse = Needs.of(
            NeedKind.FUN to 0.65,
            NeedKind.SOCIAL to 0.65,
            NeedKind.ENERGY to 0.1
        )
        val startA = AgentState("A", Personality(), beduerfnisse)
        val startB = AgentState("B", Personality(), beduerfnisse)

        val ersterA = LivingSimulation.step(startA, welt(portions = 2))
        val ersterB = LivingSimulation.step(
            startB,
            welt(portions = 2, nearbyProfiles = setOf("FREUND"))
        )
        assertEquals(GoalKind.HAVE_FUN, ersterA.agent.goal)
        assertEquals(GoalKind.CONNECT_WITH, ersterB.agent.goal)

        val freund = AgentState(
            "FREUND",
            Personality(goalBias = mapOf(GoalKind.CONNECT_WITH to 0.1)),
            Needs.of(NeedKind.SOCIAL to 0.8, NeedKind.ENERGY to 0.1)
        )
        val antwort = LivingSimulation.respondToPlay(
            freund,
            ersterB.world.copy(nearbyProfiles = setOf("B")),
            ersterB.messages.single()
        )
        val bNachBegegnung = LivingSimulation.receiveResponse(
            ersterB.agent,
            ersterB.world,
            antwort.messages.single()
        )
        val mehrereTageA = LivingSimulation.run(ersterA.agent, ersterA.world, count = 120)
        val mehrereTageB = LivingSimulation.run(
            bNachBegegnung.agent,
            bNachBegegnung.world,
            count = 120
        )

        assertTrue(mehrereTageA.world.day >= 2)
        assertTrue(mehrereTageB.world.day >= 2)
        assertTrue(mehrereTageA.agent.learnedPreferences != mehrereTageB.agent.learnedPreferences)
        assertTrue(mehrereTageA.agent.episodes != mehrereTageB.agent.episodes)
        assertTrue(
            mehrereTageB.agent.episodes.any {
                it.event.kind == LivingEventKind.SYMBOLS_RECEIVED &&
                    SymbolicIntent.YES in it.event.intents
            }
        )
    }

}
