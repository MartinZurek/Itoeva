package com.notime.glyphsim.reminder

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.DaysOfWeekMask
import com.notime.glyphcore.data.GlyphReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

/**
 * **Wer im Pflegebuch steht.**
 *
 * Ein Fuetter-Ereignis beantwortet zwei Fragen auf einmal: WELCHE Routine hat ausgeloest
 * ([com.notime.glyphsim.data.AvatarFeedEvent.reminderId]) und WER war dabei
 * ([com.notime.glyphsim.data.AvatarFeedEvent.profileId]). Bis Phase 4b kam die zweite Antwort aus
 * der ersten - das Ereignis uebernahm schlicht `reminder.profileId`.
 *
 * Das war korrekt, solange jedes Wesen seinen eigenen Satz Routinen hat, und faellt genau in dem
 * Moment auseinander, in dem die Routinen dem Nutzer gehoeren: dann traegt jede Ausloesung die
 * gemeinsame Profil-Id, und "welches Wesen hat das miterlebt" ist nirgends mehr festgehalten.
 * Stimmung, Statistik und Spielstand je Wesen haetten damit keine Grundlage mehr.
 *
 * Diese Tests halten die Regel fest, bevor der Umbau sie braucht: **die Routine bestimmt, WAS
 * ausgeloest hat - das anwesende Wesen bestimmt, WESSEN Pflegebuch es ist.**
 *
 * Ohne Geraet pruefbar, weil `feedEventFor` eine reine Zuordnung ist. Ueber den echten
 * Ausloese-Weg waere dasselbe nur mit Alarmen, Bildschirmzustand und Datenbank erreichbar - und
 * heute ohnehin nicht sichtbar, weil beide Ids denselben Wert haben.
 */
class FeedEventOwnershipTest {

    private fun reminder(ownedBy: String) = GlyphReminder(
        id = 42L,
        label = "Trinken",
        animationType = AnimationType.DRINK,
        daysOfWeekMask = DaysOfWeekMask.toMask(DayOfWeek.entries.toSet()),
        startMinuteOfDay = 9 * 60,
        endMinuteOfDay = 18 * 60,
        intervalMinutes = 30,
        profileId = ownedBy
    )

    /**
     * **Der eigentliche Punkt.** Die Routine gehoert einem gemeinsamen Profil, anwesend ist GLOOP -
     * ins Pflegebuch gehoert GLOOP.
     *
     * Dieser Test scheitert gegen den frueheren Stand: dort stand `SHARED` im Ereignis.
     */
    @Test
    fun dasEreignisGehoertDemAnwesendenWesenUndNichtDerRoutine() {
        val event = ReminderTrigger.feedEventFor(
            reminder = reminder(ownedBy = "SHARED"),
            companionProfileId = "GLOOP",
            animationType = AnimationType.DRINK,
            libraryAnimationLabel = null,
            nowMillis = 1_000L
        )

        assertEquals("GLOOP", event.profileId)
    }

    /**
     * Die andere Haelfte muss trotzdem bei der Routine bleiben: welche Zeile ausgeloest hat, ist
     * keine Eigenschaft des Wesens. Ohne diese Verknuepfung liesse sich eine spaetere Reaktion
     * nicht mehr dem richtigen Auftreten zuordnen (siehe `AvatarFeedEvent.fedAtMillis`).
     */
    @Test
    fun dieAusloesendeZeileBleibtDieDerRoutine() {
        val event = ReminderTrigger.feedEventFor(
            reminder = reminder(ownedBy = "SHARED"),
            companionProfileId = "GLOOP",
            animationType = AnimationType.DRINK,
            libraryAnimationLabel = null,
            nowMillis = 1_000L
        )

        assertEquals(42L, event.reminderId)
        // Der Knoten wird gleich beim Anlegen mitgeschrieben, damit die Neigungsrechnung des
        // Skillbaums spaeter nach Zweig gruppieren kann statt jede Zeile einzeln zu uebersetzen.
        assertEquals("koerper", event.nodeId)
        assertEquals(1_000L, event.epochMillis)
    }

    /**
     * Zwei Wesen, dieselbe gemeinsame Routine: zwei Pflegebuecher. Das ist die Zusage, auf der die
     * Beziehung aufbaut - ein frisch gewaehltes Wesen kennt die Vergangenheit des vorherigen
     * nicht, obwohl es dieselben Routinen begleitet.
     */
    @Test
    fun dieselbeRoutineFuehrtZuGetrenntenBuechern() {
        val gemeinsam = reminder(ownedBy = "SHARED")

        val beiGloop = ReminderTrigger.feedEventFor(gemeinsam, "GLOOP", AnimationType.DRINK, null, 1L)
        val beiPuffling = ReminderTrigger.feedEventFor(gemeinsam, "PUFFLING", AnimationType.DRINK, null, 2L)

        assertTrue(
            "Zwei Wesen duerfen dieselbe Routine nicht in dasselbe Buch schreiben",
            beiGloop.profileId != beiPuffling.profileId
        )
        assertEquals(beiGloop.reminderId, beiPuffling.reminderId)
    }

    /**
     * [com.notime.glyphsim.data.AvatarFeedEvent.isPlayMode] bleibt eine Eigenschaft der Routine,
     * nicht des Wesens - daran haengt, ob die Reaktion auf den Spielstand einzahlt (siehe
     * `AvatarFeeding.logFeedEvent`). Wuerde es vom anwesenden Wesen abgeleitet, koennte eine echte
     * Erinnerung im Spielmodus faelschlich Erfahrung geben.
     */
    @Test
    fun derSpielCharakterDerZeileWirdVonDerRoutineUebernommen() {
        val spiel = reminder(ownedBy = "SHARED").copy(isPlayMode = true)
        val echt = reminder(ownedBy = "SHARED")

        assertTrue(ReminderTrigger.feedEventFor(spiel, "GLOOP", null, null, 1L).isPlayMode)
        assertTrue(!ReminderTrigger.feedEventFor(echt, "GLOOP", AnimationType.DRINK, null, 1L).isPlayMode)
    }

    /**
     * Bibliotheks-Animationen tragen ihren Namen statt eines Typs (siehe `AvatarFeedEvent`) -
     * beides wird unveraendert durchgereicht, damit die Statistik sie spaeter aufschluesseln kann.
     */
    @Test
    fun typUndBibliotheksNameWerdenUnveraendertUebernommen() {
        val event = ReminderTrigger.feedEventFor(
            reminder = reminder(ownedBy = "SHARED"),
            companionProfileId = "GLOOP",
            animationType = null,
            libraryAnimationLabel = "Rocket",
            nowMillis = 1L
        )

        assertEquals(null, event.animationType)
        assertEquals("Rocket", event.libraryAnimationLabel)
    }
}
