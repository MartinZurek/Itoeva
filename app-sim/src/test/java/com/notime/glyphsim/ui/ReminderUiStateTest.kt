package com.notime.glyphsim.ui

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.BuiltInAnimationSelection
import com.notime.glyphcore.data.DaysOfWeekMask
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphcore.data.LibraryAnimation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

/**
 * Tests fuer [ReminderUiState] - die Ableitungen, die bis Phase 3.1 mitten in der Komposition
 * standen.
 *
 * **Das ist der eigentliche Gewinn des Umbaus.** Vorher brauchte jede dieser Fragen ein Geraet und
 * einen aufgebauten Bildschirm, weil sie in einer `@Composable` gerechnet wurden. Jetzt sind es
 * JVM-Tests, die in Millisekunden bei jedem `gradlew verify` mitlaufen.
 */
class ReminderUiStateTest {

    private fun reminder(label: String, isPlayMode: Boolean = false) = GlyphReminder(
        label = label,
        animationType = AnimationType.GENERAL,
        daysOfWeekMask = DaysOfWeekMask.toMask(DayOfWeek.entries.toSet()),
        startMinuteOfDay = 9 * 60,
        endMinuteOfDay = 18 * 60,
        intervalMinutes = 30,
        isPlayMode = isPlayMode
    )

    private fun library(label: String, selected: Boolean) =
        LibraryAnimation(label = label, emoji = "x", framesData = "", isSelected = selected)

    private fun builtIn(type: String, selected: Boolean) =
        BuiltInAnimationSelection(animationType = type, isSelected = selected)

    // --- Leer-Zustand -------------------------------------------------------------------------

    /**
     * Der Grund fuer das Feld `loaded`: ohne es zeigte der Bildschirm beim Aufbau kurz "Noch keine
     * Erinnerungen", bevor die Liste aus der Datenbank eintraf - ein Aufblitzen, das wie ein
     * Datenverlust aussieht.
     */
    @Test
    fun `vor dem Laden gilt der Leer-Zustand nicht`() {
        assertFalse(ReminderUiState().showEmptyState)
    }

    @Test
    fun `nach dem Laden ohne Erinnerungen gilt der Leer-Zustand`() {
        assertTrue(ReminderUiState(loaded = true).showEmptyState)
    }

    @Test
    fun `mit Erinnerungen gilt der Leer-Zustand nie`() {
        val state = ReminderUiState(reminders = listOf(reminder("Trinken")), loaded = true)
        assertFalse(state.showEmptyState)
    }

    // --- Ausgewaehlte Animationen -------------------------------------------------------------

    @Test
    fun `ausgewaehlte Bibliotheks-Animationen werden herausgefiltert`() {
        val state = ReminderUiState(
            libraryAnimations = listOf(
                library("Katze", selected = true),
                library("Hund", selected = false),
                library("Blume", selected = true)
            )
        )

        assertEquals(listOf("Katze", "Blume"), state.selectedLibraryAnimations.map { it.label })
    }

    @Test
    fun `ausgewaehlte eingebaute Typen werden herausgefiltert`() {
        val state = ReminderUiState(
            builtInSelections = listOf(
                builtIn(AnimationType.DRINK.name, selected = true),
                builtIn(AnimationType.MOVE.name, selected = false),
                builtIn(AnimationType.REST.name, selected = true)
            )
        )

        assertEquals(setOf(AnimationType.DRINK, AnimationType.REST), state.selectedBuiltInTypes)
    }

    /**
     * Der Typ steht als Zeichenkette in der Datenbank (siehe Converters). Ein Eintrag, dessen Typ
     * es nicht mehr gibt - etwa nach einem Rueckbau -, darf die Liste nicht sprengen.
     *
     * Genau diese Fehlerbehandlung stand bis Phase 3.1 als `runCatching` mitten in einer
     * `@Composable` und war nur mit Geraet pruefbar.
     */
    @Test
    fun `ein unbekannter Typ wird still verworfen statt zu scheitern`() {
        val state = ReminderUiState(
            builtInSelections = listOf(
                builtIn(AnimationType.DRINK.name, selected = true),
                builtIn("EIN_TYP_DEN_ES_NICHT_MEHR_GIBT", selected = true)
            )
        )

        assertEquals(setOf(AnimationType.DRINK), state.selectedBuiltInTypes)
    }

    @Test
    fun `nicht ausgewaehlte Eintraege zaehlen auch dann nicht wenn ihr Typ unbekannt ist`() {
        val state = ReminderUiState(
            builtInSelections = listOf(builtIn("UNBEKANNT", selected = false))
        )

        assertTrue(state.selectedBuiltInTypes.isEmpty())
    }

    // --- Gesamtzahl fuer das Picker-Limit -----------------------------------------------------

    /**
     * Beide Quellen teilen sich denselben Topf (siehe `MAX_ANIMATIONS_IN_PICKER`). Die Summe muss
     * deshalb beide zaehlen - eine Verwechslung hier waere im Betrieb nur daran zu merken, dass
     * das Limit zu frueh oder zu spaet greift.
     */
    @Test
    fun `die Gesamtzahl zaehlt beide Quellen`() {
        val state = ReminderUiState(
            libraryAnimations = listOf(
                library("Katze", selected = true),
                library("Hund", selected = false)
            ),
            builtInSelections = listOf(
                builtIn(AnimationType.DRINK.name, selected = true),
                builtIn(AnimationType.MOVE.name, selected = true)
            )
        )

        assertEquals(3, state.selectedAnimationCount)
    }

    @Test
    fun `ohne Auswahl ist die Gesamtzahl null`() {
        assertEquals(0, ReminderUiState().selectedAnimationCount)
    }

    // --- Unveraenderlichkeit -------------------------------------------------------------------

    /**
     * Der Zustand ist ein Wert: zwei gleich befuellte Exemplare sind gleich. Compose stuetzt sich
     * darauf, um Neuzeichnen zu vermeiden - waere es eine Klasse mit Identitaetsvergleich, zeichnete
     * der Bildschirm bei jedem Fluss-Ereignis neu, auch wenn sich nichts geaendert hat.
     */
    @Test
    fun `gleicher Inhalt bedeutet Gleichheit`() {
        val a = ReminderUiState(reminders = listOf(reminder("Trinken")), loaded = true)
        val b = ReminderUiState(reminders = listOf(reminder("Trinken")), loaded = true)

        assertEquals(a, b)
    }
}
