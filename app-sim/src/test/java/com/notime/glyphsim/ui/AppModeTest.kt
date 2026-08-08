package com.notime.glyphsim.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft die drei Modi, zwischen denen sich der Nutzer entscheidet.
 *
 * **Warum das eine eigene Pruefung wert ist.** Nach aussen gibt es genau EINEN Modus, gespeichert
 * wird er aber in ZWEI getrennten Schaltern (Spiel im Kern, "nur Uhr" in der Oberflaeche - siehe
 * [WatchModePrefs]). Aus zwei Schaltern lassen sich vier Zustaende bilden, von denen nur drei
 * gueltig sind; der vierte - Spiel UND nur Uhr zugleich - waere ein Widerspruch, den niemand
 * bemerkt, weil beide Seiten fuer sich plausibel aussehen.
 */
class AppModeTest {

    @Test
    fun `jeder Modus hat eine eigene Beschriftung`() {
        // Zwei Modi mit demselben Text waeren in der Auswahl nicht auseinanderzuhalten - und
        // nebeneinander stehend faellt es doppelt auf.
        val labels = AppMode.entries.map { it.labelRes }
        assertEquals("zwei Modi teilen sich einen Text", AppMode.entries.size, labels.toSet().size)
        assertTrue("ein Modus hat gar keinen Text", labels.none { it == 0 })
    }

    @Test
    fun `die Auswahl laeuft von der ruhigsten zur lebhaftesten Fassung`() {
        // Die Reihenfolge ist keine Laune: Sie ist der Grund, warum die Leiste sich selbst
        // erklaert - jeder Schritt nach rechts fuegt etwas hinzu. Wird sie umgestellt, geht diese
        // Lesart verloren, ohne dass irgendetwas kaputt aussieht.
        assertEquals(
            listOf(AppMode.WATCH, AppMode.REMINDER, AppMode.PLAY),
            AppMode.entries.toList()
        )
    }

    @Test
    fun `es gibt genau drei Modi`() {
        // Ein vierter wuerde die Leiste sprengen (siehe ModeSelector in HomeScreen) - das soll
        // dann eine bewusste Entscheidung sein und kein Nebeneffekt.
        assertEquals(3, AppMode.entries.size)
    }
}
