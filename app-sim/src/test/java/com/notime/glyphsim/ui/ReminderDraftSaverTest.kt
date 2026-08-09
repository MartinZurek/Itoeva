package com.notime.glyphsim.ui

import androidx.compose.runtime.saveable.SaverScope
import com.notime.glyphcore.data.AnimationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

/**
 * Rundlauf-Tests fuer die Saver aus [ReminderDraftSaver.kt].
 *
 * **Warum das einen eigenen Test wert ist.** Ein Saver faellt nicht beim Speichern auf, sondern
 * beim Wiederherstellen - also genau in dem Moment, in dem der Nutzer nach einem Prozesstod
 * zurueckkommt und ohnehin schon ueberrascht ist. Ein Indexfehler in der Liste waere dort ein
 * Absturz, und im normalen Betrieb wuerde er nie auftreten: solange die App im Vordergrund
 * bleibt, laeuft kein einziger dieser Pfade.
 *
 * Reine Kotlin-Logik, laeuft deshalb als JVM-Test bei jedem `gradlew verify` mit.
 *
 * Das :app-Modul haelt eine wortgleiche Kopie dieser Saver (wie bei den uebrigen UI-Dateien beider
 * Apps). Geprueft wird hier die gemeinsame Logik.
 */
class ReminderDraftSaverTest {

    /** Beim echten Speichern entscheidet Compose, was ins Bundle darf - im Test alles. */
    private val scope = SaverScope { true }

    private fun roundTrip(draft: ReminderDraft?): ReminderDraft? {
        val saved = with(ReminderDraftSaver) { scope.save(draft) }
        return saved?.let { ReminderDraftSaver.restore(it) }
    }

    private fun draft(choice: AnimationChoice) = ReminderDraft(
        label = "Trink was",
        animationChoice = choice,
        selectedDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY),
        startMinute = 9 * 60,
        endMinute = 18 * 60,
        interval = 30
    )

    @Test
    fun `Entwurf mit eingebauter Animation ueberlebt den Rundlauf`() {
        val original = draft(AnimationChoice.BuiltIn(AnimationType.DRINK))
        assertEquals(original, roundTrip(original))
    }

    @Test
    fun `Entwurf mit Bibliotheks-Animation ueberlebt den Rundlauf`() {
        val original = draft(AnimationChoice.Library(id = 42L, label = "Katze", emoji = "🐱"))
        assertEquals(original, roundTrip(original))
    }

    /**
     * Ohne diesen Fall koennte der Saver "nichts gespeichert" nicht von "gespeichert wurde null"
     * unterscheiden - siehe die leere Liste in [ReminderDraftSaver].
     */
    @Test
    fun `kein Entwurf bleibt kein Entwurf`() {
        assertNull(roundTrip(null))
    }

    @Test
    fun `Wochentage bleiben vollstaendig und in der richtigen Zuordnung`() {
        val alleTage = draft(AnimationChoice.BuiltIn(AnimationType.MOVE))
            .copy(selectedDays = DayOfWeek.entries.toSet())
        assertEquals(DayOfWeek.entries.toSet(), roundTrip(alleTage)?.selectedDays)

        val nurSonntag = draft(AnimationChoice.BuiltIn(AnimationType.MOVE))
            .copy(selectedDays = setOf(DayOfWeek.SUNDAY))
        assertEquals(setOf(DayOfWeek.SUNDAY), roundTrip(nurSonntag)?.selectedDays)
    }

    @Test
    fun `Fenster ueber Mitternacht bleibt erhalten`() {
        val nachts = draft(AnimationChoice.BuiltIn(AnimationType.SLEEP))
            .copy(startMinute = 22 * 60, endMinute = 6 * 60)
        val wieder = roundTrip(nachts)
        assertEquals(22 * 60, wieder?.startMinute)
        assertEquals(6 * 60, wieder?.endMinute)
    }

    // --- AnimationChoiceSaver ----------------------------------------------------------------

    private fun roundTripChoice(choice: AnimationChoice): AnimationChoice? {
        val saved = with(AnimationChoiceSaver) { scope.save(choice) }
        return saved?.let { AnimationChoiceSaver.restore(it) }
    }

    @Test
    fun `jeder eingebaute Animationstyp ueberlebt den Rundlauf`() {
        AnimationType.entries.forEach { type ->
            assertEquals(AnimationChoice.BuiltIn(type), roundTripChoice(AnimationChoice.BuiltIn(type)))
        }
    }

    @Test
    fun `Bibliotheks-Auswahl behaelt Id, Bezeichnung und Emoji`() {
        val choice = AnimationChoice.Library(id = 7L, label = "Giessen", emoji = "🪴")
        assertEquals(choice, roundTripChoice(choice))
    }

    // --- DaysOfWeekSaver ---------------------------------------------------------------------

    @Test
    fun `Wochentagsmenge ueberlebt den Rundlauf`() {
        val tage = setOf(DayOfWeek.TUESDAY, DayOfWeek.SATURDAY)
        val saved = with(DaysOfWeekSaver) { scope.save(tage) }
        assertEquals(tage, saved?.let { DaysOfWeekSaver.restore(it) })
    }

    @Test
    fun `leere Wochentagsmenge bleibt leer`() {
        val saved = with(DaysOfWeekSaver) { scope.save(emptySet<DayOfWeek>()) }
        assertTrue(saved?.let { DaysOfWeekSaver.restore(it) }?.isEmpty() == true)
    }
}
