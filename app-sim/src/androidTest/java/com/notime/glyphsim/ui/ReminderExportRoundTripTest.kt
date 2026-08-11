package com.notime.glyphsim.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.DaysOfWeekMask
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphcore.data.LibraryAnimation
import com.notime.glyphcore.data.NO_GOAL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek

/**
 * **Was der Export schreibt, muss der Import wieder hergeben.**
 *
 * Ein Export, dessen Rueckweg niemand nachvollzieht, ist eine Datei und keine Sicherung. Der
 * Fehler faellt zudem zum denkbar spaetesten Zeitpunkt auf: wenn jemand nach einem Geraetewechsel
 * seinen Stand zurueckholen will - also genau dann, wenn er nichts mehr hat, worauf er
 * ausweichen koennte.
 *
 * Geprueft wird deshalb nicht das Format, sondern der **Rundlauf**: exportieren, durch
 * `parseReminders` schicken, vergleichen. Damit laeuft der Test durch dieselbe Schleuse wie ein
 * echter Import, samt Laengenbegrenzung, Deckelung und Einrasten auf waehlbare Werte.
 *
 * Instrumentiert, weil `parseReminders` `org.json` benutzt - auf der JVM ist das nur ein
 * Platzhalter, der bei jedem Aufruf wirft. Die reine Textform prueft `ReminderExportTest`.
 */
@RunWith(AndroidJUnit4::class)
class ReminderExportRoundTripTest {

    private fun reminder(
        label: String,
        typ: AnimationType = AnimationType.DRINK,
        tage: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
        start: Int = 8 * 60,
        ende: Int = 20 * 60,
        intervall: Int = 60,
        ziel: Int = NO_GOAL,
        libraryId: Long? = null
    ) = GlyphReminder(
        label = label,
        animationType = typ,
        libraryAnimationId = libraryId,
        daysOfWeekMask = DaysOfWeekMask.toMask(tage),
        startMinuteOfDay = start,
        endMinuteOfDay = ende,
        intervalMinutes = intervall,
        dailyGoal = ziel
    )

    /**
     * **Der Hauptfall.** Alles, was der Nutzer eingestellt hat, kommt wieder heraus: Bezeichnung,
     * Thema, Zeitfenster, Intervall, Tagesziel und Wochentage.
     */
    @Test
    fun allesKommtWiederHeraus() {
        val original = reminder(
            label = "Trink was",
            typ = AnimationType.DRINK,
            tage = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            start = 7 * 60 + 30,
            ende = 19 * 60,
            intervall = 60,
            ziel = 4
        )

        val zurueck = parseReminders(ReminderExport.toJson(listOf(original)), emptyList())

        assertEquals(1, zurueck.size)
        val e = zurueck.single()
        assertEquals("Trink was", e.label)
        assertEquals(AnimationChoice.BuiltIn(AnimationType.DRINK), e.animationChoice)
        assertEquals(7 * 60 + 30, e.startMinuteOfDay)
        assertEquals(19 * 60, e.endMinuteOfDay)
        assertEquals(60, e.intervalMinutes)
        assertEquals(4, e.dailyGoal)
        assertEquals(original.daysOfWeekMask, e.daysOfWeekMask)
    }

    /**
     * Nichts wird unterwegs zurechtgerueckt. Eine leere Korrekturliste heisst: Der Import musste
     * an keiner Angabe etwas aendern - genau das ist die Zusage eines Rundlaufs. Sobald der
     * Export ein Format schriebe, das der Import nur noch ungefaehr versteht, faellt dieser Test.
     */
    @Test
    fun derImportMussNichtsKorrigieren() {
        val alle = listOf(
            reminder("Bewegen", AnimationType.MOVE, ziel = 2),
            reminder("Lesen", AnimationType.BOOK, start = 0, ende = 23 * 60 + 59, intervall = 120),
            reminder("Ohne Ziel", AnimationType.GENERAL)
        )

        val zurueck = parseReminders(ReminderExport.toJson(alle), emptyList())

        assertEquals(3, zurueck.size)
        zurueck.forEach { e ->
            assertTrue("\"${e.label}\" wurde korrigiert: ${e.corrections}", e.corrections.isEmpty())
        }
    }

    /**
     * Bibliotheks-Animationen gehen ueber ihren NAMEN mit, nicht ueber die Id - auf einem neuen
     * Geraet ist jede Id eine andere. Voraussetzung ist, dass es die Animation dort auch gibt;
     * sonst faellt der Verweis auf das Thema zurueck, ohne den Eintrag zu verlieren.
     */
    @Test
    fun eigeneAnimationenGehenUeberIhrenNamenMit() {
        val bibliothek = listOf(
            LibraryAnimation(id = 7, label = "Rocket", emoji = "🚀", framesData = "")
        )
        val original = reminder("Abheben", libraryId = 7)

        val zurueck = parseReminders(
            ReminderExport.toJson(listOf(original), bibliothek),
            bibliothek
        ).single()

        assertTrue(
            "Erwartet wurde die Bibliotheks-Animation, bekommen: ${zurueck.animationChoice}",
            zurueck.animationChoice is AnimationChoice.Library
        )
        assertEquals("Rocket", (zurueck.animationChoice as AnimationChoice.Library).label)
    }

    /**
     * Fehlt die Animation auf dem Zielgeraet, geht der Eintrag NICHT verloren - er faellt auf sein
     * Thema zurueck. Ein Import, der wegen einer fehlenden Animation eine Erinnerung
     * unterschlaegt, waere die schlechteste Antwort auf den haeufigsten Fall.
     */
    @Test
    fun eineFehlendeAnimationKostetKeineErinnerung() {
        val bibliothek = listOf(
            LibraryAnimation(id = 7, label = "Rocket", emoji = "🚀", framesData = "")
        )
        val text = ReminderExport.toJson(listOf(reminder("Abheben", libraryId = 7)), bibliothek)

        // Auf dem Zielgeraet gibt es die Animation nicht.
        val zurueck = parseReminders(text, emptyList()).single()

        assertEquals("Abheben", zurueck.label)
        assertEquals(AnimationChoice.BuiltIn(AnimationType.DRINK), zurueck.animationChoice)
    }

    /**
     * Anfuehrungszeichen und Rueckstriche in einer selbst gewaehlten Bezeichnung duerfen den Text
     * nicht sprengen. Der haeufigste Weg, wie ein handgeschriebener Serialisierer kaputtgeht - und
     * er kostet nicht einen Eintrag, sondern die ganze Datei.
     */
    @Test
    fun sonderzeichenInDerBezeichnungUeberlebenDenRundlauf() {
        val bezeichnung = """Trink "viel" \ Wasser"""

        val zurueck = parseReminders(
            ReminderExport.toJson(listOf(reminder(bezeichnung))),
            emptyList()
        )

        assertEquals(1, zurueck.size)
        assertEquals(bezeichnung, zurueck.single().label)
    }

    /**
     * Die vom Spielmodus verwaltete Zeile geht nicht mit. Sie ist keine Einstellung des Nutzers,
     * sondern wird bei jeder Neuplanung neu gewuerfelt - mitgenommen waere sie auf dem neuen
     * Geraet eine Erinnerung, die sich niemand vorgenommen hat.
     */
    @Test
    fun dieSpielZeileGehtNichtMit() {
        val alle = listOf(
            reminder("Trinken"),
            reminder("Spiel").copy(isPlayMode = true)
        )

        val zurueck = parseReminders(ReminderExport.toJson(alle), emptyList())

        assertEquals(listOf("Trinken"), zurueck.map { it.label })
    }

    /** Ohne Erinnerungen entsteht gueltiger Text, kein kaputter - der Import gibt dann nichts her. */
    @Test
    fun einLeererStandErgibtGueltigenText() {
        assertEquals(emptyList<SanitizedReminder>(), parseReminders(ReminderExport.toJson(emptyList()), emptyList()))
    }
}
