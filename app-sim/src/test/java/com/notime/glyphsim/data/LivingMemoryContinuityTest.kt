package com.notime.glyphsim.data

import com.notime.glyphsim.living.AgentState
import com.notime.glyphsim.living.Episode
import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.LivingEvent
import com.notime.glyphsim.living.LivingEventKind
import com.notime.glyphsim.living.LivingSite
import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.living.Needs
import com.notime.glyphsim.living.Personality
import com.notime.glyphsim.living.UtilitySelector
import com.notime.glyphsim.living.WorldState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Ablage ohne Android und ohne Dateisystem; ein Neustart ist hier ein zweites `restore`. */
private class SpeicherAttrappe : LivingAgentStorage {
    private val values = mutableMapOf<String, String>()
    override fun read(profileId: String): String? = values[profileId]
    override fun write(profileId: String, payload: String) {
        values[profileId] = payload
    }
}

/**
 * **Ueberlebt die Erinnerung den Neustart - und wirkt sie danach noch?**
 *
 * ## Die Luecke, die dieser Test schliesst
 *
 * Dass der gespeicherte Zustand vollstaendig zurueckkommt, prueft bereits
 * [LivingAgentStoreTest]. Das ist eine Aussage ueber Bytes. Die Aussage, auf die es beim
 * Living Agent ankommt, ist eine andere:
 *
 * > Nach dem Neustart trifft das Wesen dieselbe Entscheidung wie vorher - **wegen** dessen,
 * > was es erlebt hat, nicht obwohl.
 *
 * Zwischen beidem liegt eine echte Fehlermoeglichkeit. Episoden koennten fehlerfrei
 * zurueckgelesen werden und trotzdem wirkungslos sein: Es genuegt, dass beim Wiederherstellen
 * ein Feld an der falschen Stelle landet, dass die Zeitrechnung die juengsten Episoden aus dem
 * Fenster der letzten sechs schiebt, oder dass eine spaetere Version die Gewichtung aendert.
 * Der Nutzer saehe davon nichts ausser einem Wesen, das ueber Nacht vergisst, was es gelernt
 * hat - und das faellt niemandem als Fehler auf, sondern nur als "irgendwie leblos".
 *
 * ## Die Konstruktion
 *
 * Zwei Ziele werden absichtlich gleich stark gemacht: [GoalKind.HAVE_FUN] (getragen von
 * [NeedKind.FUN]) und [GoalKind.DEVELOP] (getragen von [NeedKind.GROWTH]), beide mit Druck
 * 0,5, und in dieser Welt kosten beide genau gleich viel. Ohne Vorgeschichte entscheidet
 * damit allein die Reihenfolge im Enum, und [GoalKind.HAVE_FUN] gewinnt.
 *
 * Genau in diese Waage legt die Erinnerung ihr Gewicht. Drei enttaeuschende Erlebnisse und ein
 * gelernter Widerwille reichen aus - und sollen auch nur knapp ausreichen: Eine Erinnerung, die
 * den Beduerfnisdruck ueberstimmt, waere kein Charakter, sondern ein Trauma.
 */
class LivingMemoryContinuityTest {

    private val welt = WorldState(
        day = 3,
        minuteOfDay = 14 * 60,
        site = LivingSite.HOME,
        coins = 4,
        portions = 2,
        openSites = setOf(
            LivingSite.HOME,
            LivingSite.WORKPLACE,
            LivingSite.MARKET,
            LivingSite.OUTSIDE
        )
    )

    /** Gleicher Druck auf beiden Zielen; ohne Vorgeschichte ist die Waage ausgeglichen. */
    private val unbelastet = AgentState(
        profileId = "HOOTLET",
        personality = Personality(),
        needs = Needs.of(NeedKind.FUN to 0.5, NeedKind.GROWTH to 0.5)
    )

    private val enttaeuschung = LivingEvent(
        kind = LivingEventKind.ACTION_BLOCKED,
        atMinute = 2_000,
        goal = GoalKind.HAVE_FUN
    )

    /** Dasselbe Wesen, nur mit drei schlechten Erfahrungen beim Vergnuegen. */
    private val gebrannt = unbelastet.copy(
        episodes = List(3) { Episode(enttaeuschung, valence = -1) },
        learnedPreferences = mapOf(GoalKind.HAVE_FUN to -0.10)
    )

    private fun wahl(agent: AgentState): GoalKind? = UtilitySelector.choose(agent, welt)

    private fun wert(agent: AgentState, goal: GoalKind) =
        UtilitySelector.rank(agent, welt).first { it.goal == goal }

    @Test
    fun `ohne Vorgeschichte steht die Waage auf Kante`() {
        // Wenn diese Annahme faellt, misst der Rest dieses Tests etwas anderes als gedacht -
        // deshalb steht sie hier ausdruecklich und nicht als Kommentar.
        assertEquals(
            "Die Konstruktion verlangt gleiche Kosten fuer beide Ziele",
            wert(unbelastet, GoalKind.HAVE_FUN).cost,
            wert(unbelastet, GoalKind.DEVELOP).cost,
            1e-9
        )
        assertEquals(GoalKind.HAVE_FUN, wahl(unbelastet))
    }

    @Test
    fun `ein gebranntes Kind waehlt anders als ein unbelastetes`() {
        assertNotEquals(wahl(unbelastet), wahl(gebrannt))
        assertEquals(GoalKind.DEVELOP, wahl(gebrannt))

        // Und zwar knapp: Der Bedarf bleibt sichtbar, die Erinnerung hat ihn nicht geloescht.
        val vergnuegen = wert(gebrannt, GoalKind.HAVE_FUN)
        assertEquals(0.5, vergnuegen.needPressure, 1e-9)
        assertTrue(
            "Die Erinnerung soll die Waage kippen, nicht das Beduerfnis ersetzen",
            vergnuegen.total > 0.0
        )
    }

    @Test
    fun `die Erinnerung ueberlebt den Neustart und wirkt danach weiter`() {
        val store = LivingAgentStore(SpeicherAttrappe())
        store.save(gebrannt, welt)

        val nachNeustart = store.restore(
            gebrannt.profileId,
            currentSimulationMinute = welt.absoluteMinute,
            currentOpenSites = welt.openSites,
            currentNearbyProfiles = welt.nearbyProfiles
        )!!.agent

        // 1. Die Erinnerung ist noch da.
        assertEquals(3, nachNeustart.episodes.size)
        assertTrue(nachNeustart.episodes.all { it.valence == -1 })
        assertEquals(-0.10, nachNeustart.learnedPreferences[GoalKind.HAVE_FUN]!!, 1e-9)

        // 2. Sie wiegt noch genauso schwer.
        val vorher = wert(gebrannt, GoalKind.HAVE_FUN)
        val nachher = wert(nachNeustart, GoalKind.HAVE_FUN)
        assertEquals(vorher.memoryInfluence, nachher.memoryInfluence, 1e-9)
        assertEquals(vorher.learnedPreference, nachher.learnedPreference, 1e-9)
        assertTrue("Eine wirkungslose Erinnerung waere keine", nachher.memoryInfluence < 0.0)

        // 3. Und die Entscheidung faellt deshalb wieder so aus wie gestern.
        assertEquals(GoalKind.DEVELOP, wahl(nachNeustart))
    }

    @Test
    fun `ohne Vorgeschichte aendert derselbe Neustart nichts`() {
        // Die Gegenprobe. Ohne sie koennte der Test oben auch dann gruen sein, wenn der
        // Neustart selbst - und nicht die Erinnerung - die Wahl verschoebe.
        val store = LivingAgentStore(SpeicherAttrappe())
        store.save(unbelastet, welt)

        val nachNeustart = store.restore(
            unbelastet.profileId,
            currentSimulationMinute = welt.absoluteMinute,
            currentOpenSites = welt.openSites,
            currentNearbyProfiles = welt.nearbyProfiles
        )!!.agent

        assertTrue(nachNeustart.episodes.isEmpty())
        assertEquals(GoalKind.HAVE_FUN, wahl(nachNeustart))
    }
}
