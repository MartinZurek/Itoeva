package com.notime.glyphsim.living

import com.notime.glyphsim.matrix.AvatarMood
import com.notime.glyphsim.matrix.GoalProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Jedes Beduerfnis muss ein Grund sein koennen, etwas zu tun** (NT-074).
 *
 * ## Der Befund
 *
 * Von sieben Beduerfnissen trieben nur fuenf ein Ziel. [NeedKind.CURIOSITY] und
 * [NeedKind.COMFORT] wuchsen jede Stunde mit, standen in jeder Erklaerung - und waren nie ein
 * Grund, irgendetwas zu unternehmen. Sie wurden ausschliesslich nebenbei gestillt, wenn ohnehin
 * gelesen, gegessen oder geruht wurde.
 *
 * Das ist der Unterschied zwischen einem Wert, den es gibt, und einem Antrieb. Ein Wesen, das
 * nie aus Neugier losgeht, wirkt nicht neugierig, egal wie hoch die Zahl dahinter steht - und
 * genau das war beim Zusehen zu bemerken.
 *
 * Derselbe blinde Fleck im Sozialen: Ohne Gegenueber lieferte der Planer fuer
 * [GoalKind.CONNECT_WITH] gar keinen Weg. Das Ziel galt damit als unerreichbar und fiel aus der
 * Wahl - ein einsames Wesen konnte gegen seine Einsamkeit nichts tun und stand daneben, bis
 * zufaellig Besuch kam.
 */
class LivingDriveTest {

    private fun welt(
        site: LivingSite = LivingSite.HOME,
        nearby: Set<String> = emptySet()
    ) = WorldState(
        day = 2,
        minuteOfDay = 15 * 60,
        site = site,
        coins = 4,
        portions = 2,
        openSites = setOf(
            LivingSite.HOME,
            LivingSite.OUTSIDE,
            LivingSite.WORKPLACE,
            LivingSite.MARKET
        ),
        nearbyProfiles = nearby
    )

    private fun agent(vararg needs: Pair<NeedKind, Double>) =
        AgentState("HOOTLET", Personality(), Needs.of(*needs))

    @Test
    fun `jedes Beduerfnis traegt ein Ziel`() {
        // Die Aussage, um die es hier geht - und der einzige Test, der sie als Ganzes haelt.
        val getragen = GoalKind.entries.map { it.drivenBy }.toSet()
        assertEquals(
            "Ein Beduerfnis ohne Ziel ist ein Wert, kein Antrieb",
            NeedKind.entries.toSet(),
            getragen
        )
    }

    @Test
    fun `Neugier schickt das Wesen hinaus`() {
        val neugierig = agent(NeedKind.CURIOSITY to 0.9)
        assertEquals(GoalKind.EXPLORE, UtilitySelector.choose(neugierig, welt()))

        // Und zwar wirklich nach draussen: Was man zu Hause findet, kennt man schon.
        val plan = Planner.planFor(GoalKind.EXPLORE, welt(), neugierig)
        assertEquals(listOf(ActionKind.TRAVEL, ActionKind.EXPLORE), plan?.kinds)
        assertEquals(
            listOf(ActionKind.EXPLORE),
            Planner.planFor(GoalKind.EXPLORE, welt(site = LivingSite.OUTSIDE), neugierig)?.kinds
        )
    }

    @Test
    fun `Erkunden stillt vor allem den Kopf, Bewegung vor allem den Koerper`() {
        // Beide fuehren hinaus - aber aus verschiedenen Gruenden, und das soll man sehen.
        val erkunden = ActionCatalog[ActionKind.EXPLORE].outcome.needRelief
        val bewegen = ActionCatalog[ActionKind.MOVE_BODY].outcome.needRelief
        assertTrue(
            erkunden.getValue(NeedKind.CURIOSITY) > (bewegen[NeedKind.CURIOSITY] ?: 0.0)
        )
        assertTrue(
            bewegen.getValue(NeedKind.FUN) > erkunden.getValue(NeedKind.FUN)
        )
    }

    @Test
    fun `Unbehagen laesst sich endlich beheben`() {
        val unbehaglich = agent(NeedKind.COMFORT to 0.85)
        assertEquals(GoalKind.SEEK_COMFORT, UtilitySelector.choose(unbehaglich, welt()))
        assertNotNull(Planner.planFor(GoalKind.SEEK_COMFORT, welt(), unbehaglich))
    }

    @Test
    fun `auch allein laesst sich etwas gegen Einsamkeit tun`() {
        val einsam = agent(NeedKind.SOCIAL to 0.9)
        val allein = Planner.planFor(GoalKind.CONNECT_WITH, welt(), einsam)
        assertEquals(listOf(ActionKind.SHOW_AFFECTION), allein?.kinds)

        val zuZweit = Planner.planFor(GoalKind.CONNECT_WITH, welt(nearby = setOf("STARLET")), einsam)
        assertEquals(listOf(ActionKind.INVITE_TO_PLAY), zuZweit?.kinds)
    }

    @Test
    fun `ein Gegenueber macht weiterhin den Unterschied`() {
        // **Die Falle bei der vorigen Aenderung.** Zuwendung ins Leere war ohne Muehe schneller
        // und billiger als jede Freizeitbeschaeftigung - ein Wesen allein haette bei gleichem
        // Druck IMMER an jemanden gedacht, und die Anwesenheit eines Freundes haette an der
        // Entscheidung nichts mehr geaendert. Genau die Aussage, die das Soziale traegt, waere
        // dabei verloren gegangen.
        val gleicherDruck = agent(NeedKind.SOCIAL to 0.65, NeedKind.FUN to 0.65)
        assertNotEquals(
            UtilitySelector.choose(gleicherDruck, welt()),
            UtilitySelector.choose(gleicherDruck, welt(nearby = setOf("FREUND")))
        )
        assertEquals(
            GoalKind.CONNECT_WITH,
            UtilitySelector.choose(gleicherDruck, welt(nearby = setOf("FREUND")))
        )

        // Wer wirklich einsam ist, greift trotzdem danach - nur eben nicht bei Gleichstand.
        assertEquals(
            GoalKind.CONNECT_WITH,
            UtilitySelector.choose(agent(NeedKind.SOCIAL to 0.9, NeedKind.FUN to 0.3), welt())
        )
    }

    // ---- Die Stimmung ----

    @Test
    fun `ohne Tagesziele folgt die Stimmung dem Wesen statt neutral zu bleiben`() {
        // Der groessere Teil der Aenderung: Wer keine Tagesziele gesetzt hat, sah bisher IMMER
        // ein regungsloses Gesicht - egal was das Wesen gerade erlebte.
        assertEquals(AvatarMood.NEUTRAL, AvatarMood.fromGoals(emptyList()))
        assertEquals(AvatarMood.HAPPY, AvatarMood.of(emptyList(), wellbeing = 0.95))
        assertEquals(AvatarMood.SAD, AvatarMood.of(emptyList(), wellbeing = 0.05))
    }

    @Test
    fun `beide Quellen zaehlen, keine allein`() {
        val allesErfuellt = listOf(GoalProgress(goal = 4, achieved = 4, expected = 4))
        assertEquals(AvatarMood.HAPPY, AvatarMood.fromGoals(allesErfuellt))

        // Haekchen gemacht, aber dem Wesen geht es schlecht: nicht mehr strahlend.
        assertNotEquals(
            AvatarMood.HAPPY,
            AvatarMood.of(allesErfuellt, wellbeing = 0.1)
        )
        // Und umgekehrt zieht ein gut gelauntes Wesen einen mageren Tag nach oben.
        val nichtsErfuellt = listOf(GoalProgress(goal = 4, achieved = 0, expected = 4))
        assertEquals(AvatarMood.SAD, AvatarMood.fromGoals(nichtsErfuellt))
        assertNotEquals(AvatarMood.SAD, AvatarMood.of(nichtsErfuellt, wellbeing = 0.95))
    }

    @Test
    fun `es gibt weiterhin nichts Schlimmeres als truebe`() {
        // Der Grundsatz aus der Klassendoku von AvatarMood bleibt unangetastet: kein Verhungern,
        // keine Strafe. Auch bei vollstaendigem Elend ist SAD das Ende der Skala.
        assertEquals(AvatarMood.SAD, AvatarMood.of(emptyList(), wellbeing = 0.0))
        assertEquals(
            AvatarMood.SAD,
            AvatarMood.of(listOf(GoalProgress(goal = 9, achieved = 0, expected = 9)), 0.0)
        )
    }

    @Test
    fun `das Wohlbefinden bleibt in seinen Grenzen`() {
        // Ein Wert ausserhalb 0..1 darf die Skala nicht sprengen.
        assertEquals(AvatarMood.HAPPY, AvatarMood.of(emptyList(), wellbeing = 42.0))
        assertEquals(AvatarMood.SAD, AvatarMood.of(emptyList(), wellbeing = -3.0))
    }
}
