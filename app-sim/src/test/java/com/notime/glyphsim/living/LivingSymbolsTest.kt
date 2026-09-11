package com.notime.glyphsim.living

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft, was von der Entscheidung eines Wesens nach aussen dringt.
 *
 * Am Geraet waere das nur als Eindruck zu pruefen ("da war ein Symbol"). Ob es das RICHTIGE
 * war - also ob es zum tatsaechlichen Ziel und zum tatsaechlichen Hindernis desselben Schritts
 * gehoert -, laesst sich nur hier belegen.
 */
class LivingSymbolsTest {

    @Test
    fun `jedes Ziel hat ein Symbol`() {
        // Die Zusicherung, an der ein spaeter angehaengtes Ziel scheitern soll: Ein Wesen, das
        // etwas will, wofuer es kein Zeichen gibt, waere von aussen wieder stumm.
        for (goal in GoalKind.entries) {
            assertNotNull("$goal ohne Symbol", LivingSymbols.wish(goal))
        }
    }

    @Test
    fun `verschiedene Ziele sehen verschieden aus`() {
        // Zwei Ziele mit demselben Symbol waeren schlimmer als keines: Man saehe etwas und
        // laege daneben.
        val symbole = GoalKind.entries.map { LivingSymbols.wish(it) }
        assertEquals(GoalKind.entries.size, symbole.distinct().size)
    }

    @Test
    fun `ohne Ziel wird nichts gezeigt`() {
        // Ein zufriedenes Wesen soll nichts anzeigen - eine dauerhaft belegte Blase waere
        // Dekoration und keine Aussage.
        assertNull(LivingSymbols.wish(null))
    }

    @Test
    fun `Hunger zeigt Essen, Arbeit zeigt Arbeit`() {
        assertEquals(SymbolicIntent.FOOD, LivingSymbols.wish(GoalKind.GET_FOOD))
        assertEquals(SymbolicIntent.WORK, LivingSymbols.wish(GoalKind.EARN_MONEY))
        assertEquals(SymbolicIntent.TIRED, LivingSymbols.wish(GoalKind.REST))
        assertEquals(SymbolicIntent.AFFECTION, LivingSymbols.wish(GoalKind.CONNECT_WITH))
    }

    // ================= Das Hindernis =================

    @Test
    fun `fehlendes Geld zeigt den Ausweg und nicht den Mangel`() {
        // Arbeit, nicht eine leere Hand: Der Zuschauer soll sehen, was als Naechstes kommt.
        assertEquals(
            SymbolicIntent.WORK,
            LivingSymbols.obstacle(Requirement.Coins(2))
        )
    }

    @Test
    fun `ein geschlossener Ort ist ein Nein`() {
        assertEquals(
            SymbolicIntent.NO,
            LivingSymbols.obstacle(Requirement.SiteOpen(LivingSite.MARKET))
        )
    }

    @Test
    fun `unterwegs sein ist kein Hindernis`() {
        // Der wichtigste der Faelle: Sonst haenge bei jedem zweiten Schritt ein Symbol ueber dem
        // Kopf, und die Faelle, in denen wirklich etwas fehlt, gingen darin unter.
        assertNull(LivingSymbols.obstacle(Requirement.At(LivingSite.MARKET)))
        assertNull(LivingSymbols.obstacle(Requirement.At(LivingSite.HOME)))
    }

    @Test
    fun `ohne Hindernis kein zweites Symbol`() {
        assertNull(LivingSymbols.obstacle(null))
    }

    // ================= Beides zusammen =================

    private fun erklaerung(goal: GoalKind?, blockedBy: Requirement? = null) = AgentExplanation(
        profileId = "PUFFLING",
        strongestNeed = NeedKind.HUNGER,
        goal = goal,
        ranking = emptyList(),
        plan = emptyList(),
        currentAction = null,
        blockedBy = blockedBy,
        influentialEpisodes = emptyList(),
        relationships = emptyMap(),
        lastEvent = null
    )

    @Test
    fun `Wunsch und Hindernis ergeben zusammen die kleinste Geschichte`() {
        // "Ich will essen" plus "mir fehlt Geld" ist bereits ein Konflikt - und genau darauf
        // sieht man zu. Einzeln waere jedes davon nur eine Zustandsanzeige.
        val paar = LivingSymbols.of(erklaerung(GoalKind.GET_FOOD, Requirement.Coins(2)))!!
        assertEquals(SymbolicIntent.FOOD, paar.wish)
        assertEquals(SymbolicIntent.WORK, paar.obstacle)
        assertTrue(paar.isBlocked)
    }

    @Test
    fun `ein laufendes Ziel ohne Hindernis zeigt nur den Wunsch`() {
        val paar = LivingSymbols.of(erklaerung(GoalKind.HAVE_FUN))!!
        assertEquals(SymbolicIntent.PLAY, paar.wish)
        assertNull(paar.obstacle)
        assertTrue(!paar.isBlocked)
    }

    @Test
    fun `ohne Ziel gibt es auch kein Paar`() {
        assertNull(LivingSymbols.of(erklaerung(null)))
        // Auch dann nicht, wenn irgendwo ein Hindernis stuende: Ohne Absicht ist ein Hindernis
        // keines.
        assertNull(LivingSymbols.of(erklaerung(null, Requirement.Coins(2))))
    }

    @Test
    fun `das Symbol stammt aus demselben Schritt wie die Entscheidung`() {
        // Kein Zufall, keine eigene Auswahl: Was gezeigt wird, folgt allein aus der Erklaerung.
        val e = erklaerung(GoalKind.REST, Requirement.SiteOpen(LivingSite.HOME))
        assertEquals(LivingSymbols.of(e), LivingSymbols.of(e))
        assertEquals(LivingSymbols.wish(e.goal), LivingSymbols.of(e)?.wish)
        assertEquals(LivingSymbols.obstacle(e.blockedBy), LivingSymbols.of(e)?.obstacle)
    }
}
