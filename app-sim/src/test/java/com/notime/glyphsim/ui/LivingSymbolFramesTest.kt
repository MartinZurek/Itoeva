package com.notime.glyphsim.ui

import com.notime.glyphsim.living.GoalKind
import com.notime.glyphsim.living.LivingSite
import com.notime.glyphsim.living.LivingSymbols
import com.notime.glyphsim.living.Requirement
import com.notime.glyphsim.living.SymbolicIntent
import com.notime.glyphsim.matrix.MatrixGeometry
import org.junit.Assert.assertEquals
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
    fun `jedes Motiv liegt auf dem Matrix-Raster`() {
        // **Der Fehler, den diese Pruefung festnagelt, war nicht zu sehen, sondern zu LESEN.**
        // Die Zeichen liegen auf 13x13; gezeichnet wurden sie in den Blasen ueber dem Kopf von
        // der Avatar-Ansicht, und die liest mit der Zeilenbreite des Avatars, also 16. Jede
        // Zeile rutschte um drei Spalten weiter, das Zeichen wurde diagonal zerschert, und ueber
        // dem Kopf stand nur noch Rauschen.
        //
        // Die Groesse hier festzuhalten ist die Haelfte der Absicherung: Wer ein Motiv auf einem
        // anderen Raster anlegt, faellt auf. Die andere Haelfte ist, dass die Blasen dieselbe
        // Ansicht benutzen wie die Speicherplaetze (siehe PlayWishBubble).
        val erwartet = MatrixGeometry.SIZE * MatrixGeometry.SIZE
        for (intent in SymbolicIntent.entries) {
            val frame = LivingSymbolFrames.frameFor(intent) ?: continue
            assertEquals("$intent liegt nicht auf 13x13", erwartet, frame.size)
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
        // YES gehoert zur Verstaendigung zwischen zwei Wesen und kann heute nicht ueber dem Kopf
        // erscheinen. Kunst auf Verdacht waere der falsche Weg; ein ehrliches null ist der
        // richtige.
        //
        // **SURPRISE stand bis NT-074 ebenfalls hier.** Es ist gezeichnet worden, weil ein Ziel
        // darauf zeigt (EXPLORE) - also genau in dem Moment, in dem es gebraucht wurde, und
        // keinen davor. Aufgefallen ist das nicht beim Nachdenken, sondern weil der Test
        // darueber rot wurde.
        assertNull(LivingSymbolFrames.frameFor(SymbolicIntent.YES))
        assertTrue(SymbolicIntent.YES !in erreichbareSymbole())
    }

    @Test
    fun `das Ausrufezeichen ist das Gegenstueck zum Fragezeichen`() {
        val frame = LivingSymbolFrames.frameFor(SymbolicIntent.SURPRISE)!!
        fun an(x: Int, y: Int) = frame[y * MatrixGeometry.SIZE + x] > 0

        assertTrue("Der Kopf muss breit sein", an(5, 2) && an(6, 2) && an(7, 2))
        assertTrue("Der Balken muss durchgehen", an(6, 4) && an(6, 5) && an(6, 6) && an(6, 7))
        assertTrue("Die Luecke traegt die Bedeutung", !an(6, 8) && !an(6, 9))
        assertTrue("Der Punkt muss den Kopf ausbalancieren", an(5, 10) && an(6, 10) && an(7, 10))
        // Auf ungeradem Raster muss es symmetrisch um Spalte 6 liegen, sonst haengt es schief.
        for (y in 0 until MatrixGeometry.SIZE) {
            for (x in 0 until MatrixGeometry.SIZE) {
                assertTrue(
                    "Zeile $y ist nicht spiegelsymmetrisch",
                    an(x, y) == an(MatrixGeometry.SIZE - 1 - x, y)
                )
            }
        }
    }
}
