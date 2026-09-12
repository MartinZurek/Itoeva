package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.living.ActionCatalog
import com.notime.glyphsim.living.ActionKind
import com.notime.glyphsim.living.AgentState
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.LivingSite
import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.living.Needs
import com.notime.glyphsim.living.Personality
import com.notime.glyphsim.living.Planner
import com.notime.glyphsim.living.WorldState
import com.notime.glyphsim.ui.PlayModeXp
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Jeder Reminder bewirkt etwas Eigenes** (NT-072).
 *
 * ## Was vorher war
 *
 * Acht der zwoelf Reminder-Typen - Buch, Fokus, Kreativitaet, Achtsamkeit, Liebe, Bewegung,
 * Medizin und Allgemein - liefen im Living Agent durch **eine einzige** Handlung
 * ([ActionKind.PURSUE_INTEREST]). Sichtbar unterschieden sie sich laengst: Der Avatar ging ans
 * Regal, an die Staffelei, nach draussen. Im Kern aber stillte ein Buch genau dasselbe wie eine
 * Tablette - Spass 0,5, Neugier 0,4, Wachstum 0,3.
 *
 * Das war nicht nur ungenau. Es nahm dem Tag seine Struktur: Wenn jede Beschaeftigung dasselbe
 * stillt, kann keine die naechste nach sich ziehen, und die Erinnerung lernt aus allen dasselbe.
 *
 * ## Was diese Tests festhalten
 *
 * Nicht bestimmte Zahlen - die sind einstellbar und sollen es bleiben -, sondern die
 * **Unterscheidungen**, ohne die das System wieder zusammenfiele.
 */
class ReminderActionsTest {

    private fun world(
        site: LivingSite = LivingSite.HOME,
        coins: Int = 4,
        portions: Int = 2,
        minute: Int = 14 * 60,
        nearby: Set<String> = emptySet()
    ) = WorldState(
        day = 2,
        minuteOfDay = minute,
        site = site,
        coins = coins,
        portions = portions,
        openSites = LivingRuntimeAdapter.openSitesAt(minute),
        nearbyProfiles = nearby
    )

    private fun agent(vararg needs: Pair<NeedKind, Double>) = AgentState(
        profileId = "HOOTLET",
        personality = Personality(),
        needs = Needs.of(*needs)
    )

    /**
     * Was beim Fuettern dieses Reminders im Kern wirklich passiert.
     *
     * [ort] ist die sichtbare Kulisse und damit die Wahrheit ueber den Aufenthalt: Der Adapter
     * leitet den Ort der Welt daraus ab ([LivingRuntimeAdapter.synchroniseWorld]), statt einem
     * mitgegebenen Weltzustand zu glauben. Genau richtig - was auf dem Bildschirm steht, gilt.
     */
    private fun handlungenFuer(
        topic: AnimationType,
        start: AgentState = agent(),
        welt: WorldState = world(),
        ort: PlayScene.Place = PlayScene.Place.LIVING
    ): List<ActionKind> = LivingRuntimeAdapter.applyRequestedRoutine(
        agent = start,
        world = welt,
        renderedPlace = ort,
        topic = topic,
        routine = PlayRoutines.forTopic(topic, random = Random(7))
    ).events.mapNotNull { it.action }

    @Test
    fun `jeder der zwoelf Reminder loest eine Handlung aus`() {
        for (topic in AnimationType.entries) {
            assertTrue(
                "$topic loest im Living Agent gar nichts aus",
                handlungenFuer(topic).isNotEmpty()
            )
        }
    }

    @Test
    fun `die acht frueher gleichen Reminder sind jetzt unterscheidbar`() {
        val erwartet = mapOf(
            AnimationType.BOOK to ActionKind.READ,
            AnimationType.CREATIVITY to ActionKind.CREATE,
            AnimationType.FOCUS to ActionKind.CONCENTRATE,
            AnimationType.MINDFULNESS to ActionKind.SETTLE,
            AnimationType.MOVE to ActionKind.MOVE_BODY,
            AnimationType.MEDICINE to ActionKind.TEND_SELF,
            AnimationType.LOVE to ActionKind.SHOW_AFFECTION,
            AnimationType.GENERAL to ActionKind.PURSUE_INTEREST
        )
        for ((topic, kind) in erwartet) {
            // Die LETZTE Handlung ist die eigentliche; davor kann seit NT-073 ein Weg stehen
            // (Bewegung verlangt, draussen zu sein).
            assertEquals("$topic", kind, handlungenFuer(topic).last())
        }
        // Und keine zwei davon sind dieselbe - genau das war vorher der Fall.
        assertEquals(erwartet.size, erwartet.values.toSet().size)
    }

    @Test
    fun `Bewegung faengt vor der Tuer an`() {
        // **Der Kern des Zimmerhockens** (NT-073): Bis hierher verlangte keine einzige Handlung
        // im ganzen Kern, draussen zu sein. OUTSIDE war ein Ort, an dem sich das Wesen
        // gelegentlich BEFAND - nie einer, an den es gehen MUSSTE.
        assertEquals(
            listOf(ActionKind.TRAVEL, ActionKind.MOVE_BODY),
            handlungenFuer(AnimationType.MOVE, ort = PlayScene.Place.LIVING)
        )
        // Ist er schon draussen, faellt der Weg weg - und Bleiben wird billiger als Hineingehen.
        assertEquals(
            listOf(ActionKind.MOVE_BODY),
            handlungenFuer(AnimationType.MOVE, ort = PlayScene.Place.PARK)
        )
    }

    @Test
    fun `auch von sich aus geht er zum Bewegen hinaus`() {
        // Nicht nur auf Bitte: Waehlt der Kern selbst Freizeit und die Themenwahl Bewegung,
        // gehoert der Gang nach draussen in den Plan wie der Weg zur Arbeit.
        val plan = Planner.planFor(
            goal = GoalKind.HAVE_FUN,
            world = world(site = LivingSite.HOME),
            agent = agent(NeedKind.FUN to 0.9),
            interest = ActionKind.MOVE_BODY
        )
        assertEquals(listOf(ActionKind.TRAVEL, ActionKind.MOVE_BODY), plan?.kinds)

        // Eine Beschaeftigung ohne eigenen Ort bekommt weiterhin keinen Weg vorangestellt.
        assertEquals(
            listOf(ActionKind.READ),
            Planner.planFor(
                goal = GoalKind.DEVELOP,
                world = world(site = LivingSite.HOME),
                agent = agent(NeedKind.GROWTH to 0.9),
                interest = ActionKind.READ
            )?.kinds
        )
    }

    @Test
    fun `Fuersorge ist kein Vergnuegen`() {
        // Der deutlichste Einzelfall der alten Regelung: Eine Tablette machte das Wesen
        // vergnuegt und neugierig, weil sie durch dieselbe Handlung lief wie ein Spiel.
        val vorher = agent(NeedKind.FUN to 0.8, NeedKind.COMFORT to 0.8, NeedKind.CURIOSITY to 0.8)
        val nachher = LivingRuntimeAdapter.applyRequestedRoutine(
            agent = vorher,
            world = world(),
            renderedPlace = PlayScene.Place.LIVING,
            topic = AnimationType.MEDICINE,
            routine = PlayRoutines.forTopic(AnimationType.MEDICINE, random = Random(1))
        ).agent

        assertTrue(
            "Fuersorge muss die Behaglichkeit stillen",
            nachher.needs.pressure(NeedKind.COMFORT) < vorher.needs.pressure(NeedKind.COMFORT)
        )
        assertTrue(
            "Fuersorge darf den Spass NICHT stillen",
            nachher.needs.pressure(NeedKind.FUN) >= vorher.needs.pressure(NeedKind.FUN)
        )
        assertTrue(
            "Fuersorge darf die Neugier NICHT stillen",
            nachher.needs.pressure(NeedKind.CURIOSITY) >= vorher.needs.pressure(NeedKind.CURIOSITY)
        )
    }

    @Test
    fun `Bewegung macht hungrig und zieht damit das Abendessen nach sich`() {
        // Die interessanteste der neuen Wirkungen: Sie erzeugt eine Folge, ohne dass irgendwo
        // eine Regel "nach Sport kommt Essen" stuende.
        val vorher = agent(NeedKind.FUN to 0.7, NeedKind.HUNGER to 0.3)
        val nachher = LivingRuntimeAdapter.applyRequestedRoutine(
            agent = vorher,
            world = world(),
            renderedPlace = PlayScene.Place.PARK,
            topic = AnimationType.MOVE,
            routine = PlayRoutines.forTopic(AnimationType.MOVE, random = Random(3))
        ).agent

        assertTrue(
            "Bewegung muss Freude bringen",
            nachher.needs.pressure(NeedKind.FUN) < vorher.needs.pressure(NeedKind.FUN)
        )
        assertTrue(
            "Bewegung muss hungrig machen",
            nachher.needs.pressure(NeedKind.HUNGER) > vorher.needs.pressure(NeedKind.HUNGER)
        )
    }

    @Test
    fun `Lesen stillt zuerst die Neugier, Konzentration zuerst das Wachstum`() {
        val start = agent(NeedKind.CURIOSITY to 0.8, NeedKind.GROWTH to 0.8)
        fun nach(topic: AnimationType) = LivingRuntimeAdapter.applyRequestedRoutine(
            agent = start,
            world = world(),
            renderedPlace = PlayScene.Place.NOOK,
            topic = topic,
            routine = PlayRoutines.forTopic(topic, random = Random(5))
        ).agent.needs

        val gelesen = nach(AnimationType.BOOK)
        val konzentriert = nach(AnimationType.FOCUS)
        assertTrue(
            "Lesen soll die Neugier staerker stillen als Konzentration",
            gelesen.pressure(NeedKind.CURIOSITY) < konzentriert.pressure(NeedKind.CURIOSITY)
        )
        assertTrue(
            "Konzentration soll das Wachstum staerker stillen als Lesen",
            konzentriert.pressure(NeedKind.GROWTH) < gelesen.pressure(NeedKind.GROWTH)
        )
    }

    @Test
    fun `Zuwendung wird zur Begegnung, sobald wirklich jemand da ist`() {
        assertEquals(
            listOf(ActionKind.SHOW_AFFECTION),
            handlungenFuer(AnimationType.LOVE, welt = world())
        )
        assertEquals(
            listOf(ActionKind.INVITE_TO_PLAY),
            handlungenFuer(AnimationType.LOVE, welt = world(nearby = setOf("STARLET")))
        )
    }

    @Test
    fun `eine Beschaeftigung wird unter dem Ziel verbucht, aus dem sie kommt`() {
        // Episoden und gelernter Geschmack haengen am ZIEL. Liefe Medizin weiterhin unter
        // "Vergnuegen", lernte das Wesen mit jeder Tablette, dass Vergnuegen schoen ist.
        fun zielVon(topic: AnimationType): GoalKind? = LivingRuntimeAdapter.applyRequestedRoutine(
            agent = agent(),
            world = world(),
            renderedPlace = PlayScene.Place.LIVING,
            topic = topic,
            routine = PlayRoutines.forTopic(topic, random = Random(2))
        ).events.firstOrNull()?.goal

        assertEquals(GoalKind.REST, zielVon(AnimationType.MEDICINE))
        assertEquals(GoalKind.REST, zielVon(AnimationType.MINDFULNESS))
        assertEquals(GoalKind.DEVELOP, zielVon(AnimationType.BOOK))
        assertEquals(GoalKind.CONNECT_WITH, zielVon(AnimationType.LOVE))
        assertEquals(GoalKind.HAVE_FUN, zielVon(AnimationType.MOVE))
        assertNotEquals(GoalKind.HAVE_FUN, zielVon(AnimationType.MEDICINE))
    }

    // ---- Der autonome Tag: dieselbe Vielfalt, jetzt mit eigener Wirkung ----

    @Test
    fun `die gewichtete Themenwahl bestimmt weiterhin, WAS er von sich aus tut`() {
        // Die Gefahr bei dieser Aenderung war, die vorhandene Vielfalt zu verlieren: Tageszeit,
        // Spezies, Entwicklungspfad und Wiederholungsdaempfer waehlen das Thema, nicht der Kern.
        // Dieser Test haelt fest, dass genau das so bleibt - und die Handlung trotzdem passt.
        for ((topic, kind) in mapOf(
            AnimationType.BOOK to ActionKind.READ,
            AnimationType.CREATIVITY to ActionKind.CREATE,
            AnimationType.MINDFULNESS to ActionKind.SETTLE
        )) {
            val prepared = LivingRuntimeAdapter.prepare(
                agent = agent(NeedKind.CURIOSITY to 0.9, NeedKind.GROWTH to 0.9, NeedKind.FUN to 0.9),
                world = world(),
                renderedPlace = PlayScene.Place.LIVING,
                interestTopic = topic,
                random = Random(11)
            )
            assertEquals("$topic", topic, prepared.topic)
            assertTrue("$topic", kind in prepared.completedActions)
        }
    }

    @Test
    fun `ein Vorschlag von aussen kann Essen, Arbeit und Schlaf nicht umgehen`() {
        // Die Ausformung sagt, WIE Freizeit aussieht - sie darf kein Grundbeduerfnis ersetzen.
        for (kind in listOf(ActionKind.EAT, ActionKind.WORK, ActionKind.REST, ActionKind.BUY_FOOD)) {
            assertTrue("$kind gehoert nicht in die Freizeitmenge", kind !in ActionCatalog.FREE_TIME)
        }
    }

    // ---- Wohlbefinden und Erfahrung ----

    @Test
    fun `Wohlbefinden faellt mit dem Druck und steigt mit dem Gestillten`() {
        assertEquals(1.0, Needs.calm().wellbeing(), 1e-9)
        val belastet = Needs.of(*NeedKind.entries.map { it to 1.0 }.toTypedArray())
        assertEquals(0.0, belastet.wellbeing(), 1e-9)
        assertTrue(
            Needs.of(NeedKind.HUNGER to 0.2).wellbeing() >
                Needs.of(NeedKind.HUNGER to 0.9).wellbeing()
        )
    }

    @Test
    fun `eine gut tuende Handlung bringt zusaetzliche Erfahrung`() {
        val vorher = agent(NeedKind.CURIOSITY to 0.9, NeedKind.GROWTH to 0.9, NeedKind.FUN to 0.6)
        val nachher = LivingRuntimeAdapter.applyRequestedRoutine(
            agent = vorher,
            world = world(),
            renderedPlace = PlayScene.Place.NOOK,
            topic = AnimationType.BOOK,
            routine = PlayRoutines.forTopic(AnimationType.BOOK, random = Random(8))
        ).agent

        val bonus = PlayModeXp.wellbeingBonus(
            vorher.needs.wellbeing(),
            nachher.needs.wellbeing()
        )
        assertTrue("Lesen bei grosser Neugier muss Erfahrung bringen, war $bonus", bonus > 0)
        assertTrue(bonus <= PlayModeXp.MAX_WELLBEING_BONUS)
    }

    @Test
    fun `Anstrengung kostet keine Erfahrung`() {
        // Arbeit und Konzentration senken das Wohlbefinden kurz. Dafuer XP abzuziehen hiesse,
        // das Wesen fuer Anstrengung zu bestrafen.
        assertEquals(0, PlayModeXp.wellbeingBonus(before = 0.8, after = 0.5))
        assertEquals(0, PlayModeXp.wellbeingBonus(before = 0.5, after = 0.5))
    }
}
