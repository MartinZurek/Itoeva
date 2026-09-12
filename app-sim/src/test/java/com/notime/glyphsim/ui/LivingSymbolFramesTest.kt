package com.notime.glyphsim.ui

import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.LivingSite
import com.notime.glyphsim.living.LivingSymbols
import com.notime.glyphsim.living.Requirement
import com.notime.glyphsim.living.SymbolicIntent
import com.notime.glyphsim.matrix.MatrixGeometry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Haelt die beiden Haelften der Symbolanzeige zusammen: die Bedeutung
 * ([com.notime.glyphsim.living.LivingSymbols]) und das Bild ([LivingSymbolFrames]).
 *
 * **Der Fehler, gegen den das hier steht**, faellt sonst erst am Geraet auf, und dort als
 * Nichts: Ein Ziel bekaeme ein Symbol, zu dem es kein Motiv gibt, und ueber dem Kopf bliebe die
 * Blase leer. Kein Absturz, keine rote Zeile - nur ein Wesen, das wieder stumm ist.
 */
class LivingSymbolFramesTest {

    /** Alles, was der Agent tatsaechlich ueber dem Kopf zeigen kann. */
    private fun erreichbareSymbole(): Set<SymbolicIntent> {
        val wuensche = GoalKind.entries.mapNotNull { LivingSymbols.wish(it) }
        val hindernisse = listOf(
            Requirement.Coins(2),
            Requirement.Portions(1),
            Requirement.At(LivingSite.MARKET),
            Requirement.SiteOpen(LivingSite.MARKET),
            Requirement.Near("PUFFLING")
        ).mapNotNull { LivingSymbols.obstacle(it) }
        return (wuensche + hindernisse).toSet()
    }

    @Test
    fun `jedes erreichbare Symbol hat ein Motiv`() {
        for (intent in erreichbareSymbole()) {
            assertNotNull("$intent hat kein Motiv", LivingSymbolFrames.frameFor(intent))
        }
    }

    @Test
    fun `ein spaeter angehaengtes Ziel faellt hier auf`() {
        // Die Zusicherung ist absichtlich ueber GoalKind.entries formuliert und nicht ueber eine
        // Aufzaehlung: Wer ein siebtes Ziel ergaenzt und das Motiv vergisst, bekommt einen roten
        // Test statt einer leeren Blase.
        for (goal in GoalKind.entries) {
            val wunsch = LivingSymbols.wish(goal)
            assertNotNull("$goal ohne Symbol", wunsch)
            assertNotNull("$goal ohne Motiv", LivingSymbolFrames.frameFor(wunsch!!))
        }
    }

    @Test
    fun `die neuen Motive sind nicht leer und liegen im runden Bildschirm`() {
        for (intent in listOf(SymbolicIntent.QUESTION, SymbolicIntent.NO)) {
            val frame = LivingSymbolFrames.frameFor(intent)!!
            val hell = frame.count { it > 0 }
            assertTrue("$intent ist leer", hell > 0)
            // Zellen ausserhalb des runden Bildschirms fallen beim Bauen weg. Waere ein Motiv
            // ueber den Rand gezeichnet, verloere es dort stillschweigend Punkte - und ein
            // Fragezeichen ohne Haken ist keines mehr.
            assertTrue("$intent hat zu wenige Punkte: $hell", hell >= 10)
            for (i in frame.indices) {
                if (frame[i] > 0) {
                    val x = i % MatrixGeometry.SIZE
                    val y = i / MatrixGeometry.SIZE
                    assertTrue("$intent ragt bei ($x,$y) heraus", MatrixGeometry.isActive(x, y))
                }
            }
        }
    }

    @Test
    fun `das Fragezeichen hat seine Luecke vor dem Punkt`() {
        // Die eine Zelle, an der das Zeichen kippt: ohne sie liest es sich als Haken mit
        // Schwanz statt als Frage.
        val frame = LivingSymbolFrames.frameFor(SymbolicIntent.QUESTION)!!
        fun an(x: Int, y: Int) = frame[y * MatrixGeometry.SIZE + x] > 0
        assertTrue("Stiel fehlt", an(5, 8))
        assertTrue("Punkt fehlt", an(5, 10))
        assertTrue("Die Luecke dazwischen ist zugewachsen", !an(5, 9))
    }

    @Test
    fun `noch nicht gezeichnete Symbole sagen das ehrlich`() {
        // YES und SURPRISE gehoeren zur Verstaendigung zwischen zwei Wesen und koennen heute
        // nicht ueber dem Kopf erscheinen. Kunst auf Verdacht waere der falsche Weg; ein
        // ehrliches null ist der richtige.
        assertNull(LivingSymbolFrames.frameFor(SymbolicIntent.YES))
        assertNull(LivingSymbolFrames.frameFor(SymbolicIntent.SURPRISE))
        assertTrue(SymbolicIntent.YES !in erreichbareSymbole())
        assertTrue(SymbolicIntent.SURPRISE !in erreichbareSymbole())
    }
}
