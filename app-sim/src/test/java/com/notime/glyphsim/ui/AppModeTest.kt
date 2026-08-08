package com.notime.glyphsim.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft das Umschalten zwischen den drei Modi.
 *
 * **Warum das eine eigene Pruefung wert ist.** Nach aussen gibt es genau EINEN Modus, gespeichert
 * wird er aber in ZWEI getrennten Schaltern (Spiel im Kern, "nur Uhr" in der Oberflaeche - siehe
 * [WatchModePrefs]). Aus zwei Schaltern lassen sich vier Zustaende bilden, von denen nur drei
 * gueltig sind; der vierte - Spiel UND nur Uhr zugleich - waere ein Widerspruch, den niemand
 * bemerkt, weil beide Seiten fuer sich plausibel aussehen.
 *
 * Der Reihum-Wechsel wird mitgeprueft, weil ein Modus, der sich beim Durchtippen ueberspringen
 * laesst, praktisch nicht erreichbar waere.
 */
class AppModeTest {

    @Test
    fun `reihum werden alle drei Modi erreicht und keiner uebersprungen`() {
        val seen = mutableListOf(AppMode.REMINDER)
        var mode = AppMode.REMINDER
        repeat(AppMode.entries.size - 1) {
            mode = mode.next()
            seen += mode
        }
        assertEquals(
            "Beim Durchtippen muss jeder Modus genau einmal vorkommen",
            AppMode.entries.toSet(), seen.toSet()
        )
    }

    @Test
    fun `nach einer vollen Runde ist man wieder am Anfang`() {
        var mode = AppMode.REMINDER
        repeat(AppMode.entries.size) { mode = mode.next() }
        assertEquals(AppMode.REMINDER, mode)
    }

    @Test
    fun `jeder Modus hat eine eigene Beschriftung`() {
        // Zwei Modi mit demselben Text waeren nicht auseinanderzuhalten - und der Knopf nennt
        // ausschliesslich den aktuellen Modus, es gibt also keinen zweiten Anhaltspunkt.
        val labels = AppMode.entries.map { it.labelRes }
        assertEquals("zwei Modi teilen sich einen Text", AppMode.entries.size, labels.toSet().size)
        assertTrue("ein Modus hat gar keinen Text", labels.none { it == 0 })
    }
}
