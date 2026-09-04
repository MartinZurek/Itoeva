package com.notime.glyphsim.skilltree

import com.notime.glyphcore.data.AnimationTree
import com.notime.glyphcore.data.AnimationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Prueft, was das Wesen aus dem Skillbaum im Alltag zeigen darf ([SkillRepertoire]).
 *
 * Zwei Dinge koennen hier schiefgehen, und beide faellt beim Zuschauen nicht auf: dass eine
 * Freischaltung im falschen Thema auftaucht (Basketball beim Trinken), und dass MEDICINE doch
 * einen Weg in den autonomen Ablauf findet - der eine Fall, den `PlayAmbientActivity` und
 * `AnimationTree` beide ausdruecklich ausschliessen.
 */
class SkillRepertoireTest {

    private val alles = AnimationTree.nodes.map { it.id }.toSet()

    @Test
    fun `ohne Freischaltung gibt es keine Faehigkeit`() {
        for (topic in AnimationType.entries) {
            assertEquals(topic.name, emptyList<String>(), SkillRepertoire.skillsFor(topic, emptySet()))
        }
    }

    /**
     * Der Wirtsknoten ist die Handlung selbst, die der Ablauf gerade gespielt hat - ihn als
     * Einlage zu wiederholen waere keine Faehigkeit, sondern ein Echo.
     */
    @Test
    fun `der Wirtsknoten selbst ist keine Faehigkeit`() {
        assertEquals("sport", SkillRepertoire.hostNodeFor(AnimationType.MOVE))
        assertEquals(emptyList<String>(), SkillRepertoire.skillsFor(AnimationType.MOVE, setOf("sport")))
    }

    @Test
    fun `ein freigeschaltetes Blatt gehoert zu seinem Thema und zu keinem anderen`() {
        val offen = setOf("sport/ballsport/basketball")
        assertEquals(
            listOf("sport/ballsport/basketball"),
            SkillRepertoire.skillsFor(AnimationType.MOVE, offen)
        )
        assertEquals(emptyList<String>(), SkillRepertoire.skillsFor(AnimationType.DRINK, offen))
        assertEquals(emptyList<String>(), SkillRepertoire.skillsFor(AnimationType.SLEEP, offen))
    }

    /**
     * REST und FOCUS haengen als einzige nicht an einer Hauptgruppe, sondern an den beiden
     * `subBuiltin`-Knoten `ruhe/pause` und `arbeit/erledigen`. Genau da bricht eine Zuordnung,
     * die "Thema = Hauptgruppe" annimmt.
     */
    @Test
    fun `REST und FOCUS haengen an Untergruppen und funktionieren trotzdem`() {
        assertEquals("ruhe/pause", SkillRepertoire.hostNodeFor(AnimationType.REST))
        assertEquals("arbeit/erledigen", SkillRepertoire.hostNodeFor(AnimationType.FOCUS))

        assertEquals(
            listOf("ruhe/pause/candle"),
            SkillRepertoire.skillsFor(AnimationType.REST, setOf("ruhe/pause/candle"))
        )
        assertEquals(
            listOf("arbeit/erledigen/check"),
            SkillRepertoire.skillsFor(AnimationType.FOCUS, setOf("arbeit/erledigen/check"))
        )
    }

    @Test
    fun `kontextuelle Fussballskills bleiben im autonomen MOVE Repertoire sichtbar`() {
        val offen = setOf("sport/ballsport", "sport/ballsport/dribbling", "sport/ballsport/schuss")
        val skills = SkillRepertoire.skillsFor(AnimationType.MOVE, offen)
        assertTrue("Dribbling fehlt im autonomen Repertoire", "sport/ballsport/dribbling" in skills)
        assertTrue("Schuss fehlt im autonomen Repertoire", "sport/ballsport/schuss" in skills)
    }

    /**
     * Die Garantie liegt im Baum, nicht in einer Pruefung hier: MEDICINE steht in
     * [AnimationTree.EXCLUDED_TYPES] und hat deshalb keinen Knoten. Dieser Test haelt fest, dass
     * das AUCH DANN traegt, wenn buchstaeblich alles freigeschaltet ist.
     */
    @Test
    fun `MEDICINE hat keinen Wirtsknoten und damit nie eine Faehigkeit`() {
        assertNull(SkillRepertoire.hostNodeFor(AnimationType.MEDICINE))
        assertEquals(emptyList<String>(), SkillRepertoire.skillsFor(AnimationType.MEDICINE, alles))
        assertNull(SkillRepertoire.pick(AnimationType.MEDICINE, alles, Random(1)))
    }

    @Test
    fun `pick liefert nichts, solange nichts offen ist`() {
        assertNull(SkillRepertoire.pick(AnimationType.MOVE, emptySet(), Random(1)))
    }

    /** Gewaehlt wird nur aus dem, was tatsaechlich offen ist - nie aus dem uebrigen Baum. */
    @Test
    fun `pick waehlt ausschliesslich aus dem Freigeschalteten`() {
        val offen = setOf("sport/ballsport", "sport/ballsport/basketball", "sport/kraft-ausdauer/heben")
        val random = Random(7)
        repeat(50) {
            val gewaehlt = SkillRepertoire.pick(AnimationType.MOVE, offen, random)
            assertTrue("unerwartet: $gewaehlt", gewaehlt in offen)
        }
    }

    /**
     * Bei genuegend Wuerfen muss jede offene Faehigkeit einmal drankommen - sonst waere eine
     * davon zwar freigeschaltet, aber nie zu sehen.
     */
    @Test
    fun `pick erreicht auf Dauer jede offene Faehigkeit`() {
        val offen = setOf("sport/ballsport/basketball", "sport/kraft-ausdauer/heben")
        val random = Random(11)
        val gesehen = mutableSetOf<String>()
        repeat(200) { SkillRepertoire.pick(AnimationType.MOVE, offen, random)?.let(gesehen::add) }
        assertEquals(offen, gesehen)
    }

    /**
     * Ein ungezeichneter Knoten ([AnimationTree.pendingArtwork]) darf nicht gewaehlt werden - er
     * haette nichts zu zeigen. Aktuell ist die Liste leer; der Test haelt die Regel fuer den Tag
     * fest, an dem wieder ein Knoten ohne Motiv dazukommt.
     */
    @Test
    fun `ungezeichnete Knoten sind keine Faehigkeit`() {
        val ohneMotiv = AnimationTree.pendingArtwork().map { it.id }.toSet()
        val ausAllem = AnimationType.entries.flatMap { SkillRepertoire.skillsFor(it, alles) }.toSet()
        assertEquals(emptySet<String>(), ausAllem intersect ohneMotiv)
    }
}
