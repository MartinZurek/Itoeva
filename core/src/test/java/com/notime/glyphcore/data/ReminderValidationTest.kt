package com.notime.glyphcore.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek

/**
 * Prueft die Regeln aus [ReminderValidation] - reine Kotlin-Rechnerei ohne Android, laeuft
 * deshalb als schneller JVM-Test und damit bei jedem `gradlew verify` mit.
 */
class ReminderValidationTest {

    private fun reminder(
        label: String = "Trink was",
        daysOfWeekMask: Int = DaysOfWeekMask.toMask(DayOfWeek.entries.toSet()),
        startMinuteOfDay: Int = 9 * 60,
        endMinuteOfDay: Int = 18 * 60,
        intervalMinutes: Int = 30,
        dailyGoal: Int = NO_GOAL,
        openDurationSeconds: Int = ReminderOpenDuration.DEFAULT_SECONDS,
        profileId: String = "puffling",
        isPlayMode: Boolean = false
    ) = GlyphReminder(
        label = label,
        animationType = AnimationType.GENERAL,
        daysOfWeekMask = daysOfWeekMask,
        startMinuteOfDay = startMinuteOfDay,
        endMinuteOfDay = endMinuteOfDay,
        intervalMinutes = intervalMinutes,
        dailyGoal = dailyGoal,
        openDurationSeconds = openDurationSeconds,
        profileId = profileId,
        isPlayMode = isPlayMode
    )

    private inline fun <reified T> assertHasProblem(reminder: GlyphReminder) {
        val problems = ReminderValidation.validate(reminder)
        assertTrue(
            "erwartet: ${T::class.simpleName}, bekommen: $problems",
            problems.any { it is T }
        )
    }

    @Test
    fun eineGewoehnlicheErinnerungIstGueltig() {
        assertEquals(emptyList<ReminderValidation.Problem>(), ReminderValidation.validate(reminder()))
    }

    /**
     * Ende vor Start heisst "ueber Mitternacht" und ist ausdruecklich erlaubt - das darf die
     * Pruefung nicht als Fehler auffassen, sonst liesse sich eine Nachtruhe-Erinnerung nicht mehr
     * speichern.
     */
    @Test
    fun fensterUeberMitternachtIstErlaubt() {
        val nachts = reminder(startMinuteOfDay = 22 * 60, endMinuteOfDay = 6 * 60)
        assertTrue(ReminderValidation.isValid(nachts))
    }

    @Test
    fun leereBezeichnungWirdBeanstandet() {
        assertHasProblem<ReminderValidation.Problem.BlankLabel>(reminder(label = "   "))
    }

    @Test
    fun zuLangeBezeichnungWirdBeanstandet() {
        val zuLang = "x".repeat(ReminderValidation.MAX_LABEL_LENGTH + 1)
        assertHasProblem<ReminderValidation.Problem.LabelTooLong>(reminder(label = zuLang))
    }

    @Test
    fun genauDieMaximalLaengeIstNochInOrdnung() {
        val gerade = "x".repeat(ReminderValidation.MAX_LABEL_LENGTH)
        assertTrue(ReminderValidation.isValid(reminder(label = gerade)))
    }

    @Test
    fun ohneWochentagWirdBeanstandet() {
        assertHasProblem<ReminderValidation.Problem.NoDaysSelected>(reminder(daysOfWeekMask = 0))
    }

    @Test
    fun maskeAusserhalbDerSiebenBitsWirdBeanstandet() {
        assertHasProblem<ReminderValidation.Problem.MaskOutOfRange>(reminder(daysOfWeekMask = 0b1000_0000))
        assertHasProblem<ReminderValidation.Problem.MaskOutOfRange>(reminder(daysOfWeekMask = -1))
    }

    @Test
    fun minutenAusserhalbDesTagesWerdenBeanstandet() {
        assertHasProblem<ReminderValidation.Problem.MinuteOutOfRange>(reminder(startMinuteOfDay = -1))
        assertHasProblem<ReminderValidation.Problem.MinuteOutOfRange>(reminder(endMinuteOfDay = 1440))
    }

    @Test
    fun gleicheStartUndEndzeitIstEinLeeresFenster() {
        assertHasProblem<ReminderValidation.Problem.EmptyWindow>(
            reminder(startMinuteOfDay = 9 * 60, endMinuteOfDay = 9 * 60)
        )
    }

    @Test
    fun intervallNullOderNegativWirdBeanstandet() {
        assertHasProblem<ReminderValidation.Problem.IntervalOutOfRange>(reminder(intervalMinutes = 0))
        assertHasProblem<ReminderValidation.Problem.IntervalOutOfRange>(reminder(intervalMinutes = -5))
    }

    @Test
    fun leereProfilIdWirdBeanstandet() {
        assertHasProblem<ReminderValidation.Problem.BlankProfileId>(reminder(profileId = " "))
    }

    // --- Der Unterschied zwischen Nutzer- und Spielmodus-Erinnerungen -----------------------

    /**
     * 7 Minuten steht in keiner Auswahlliste. Fuer eine vom Nutzer eingerichtete Erinnerung ist
     * das ein Fehler - der Dialog koennte sie nicht darstellen und wuerde sie beim naechsten
     * Bearbeiten ueberschreiben.
     */
    @Test
    fun krummesIntervallIstFuerNutzerErinnerungenUnzulaessig() {
        assertHasProblem<ReminderValidation.Problem.IntervalNotSelectable>(reminder(intervalMinutes = 7))
    }

    /**
     * Dieselben 7 Minuten sind fuer den Spielmodus voellig richtig: die Wartezeit wird dort aus
     * einer Spanne gewuerfelt (siehe PlayGamePlan) und erscheint in keinem Dialog.
     */
    @Test
    fun krummesIntervallIstFuerDenSpielmodusInOrdnung() {
        assertTrue(ReminderValidation.isValid(reminder(intervalMinutes = 7, isPlayMode = true)))
    }

    @Test
    fun harteGrenzenGeltenAuchFuerDenSpielmodus() {
        assertHasProblem<ReminderValidation.Problem.IntervalOutOfRange>(
            reminder(intervalMinutes = 0, isPlayMode = true)
        )
        assertHasProblem<ReminderValidation.Problem.NoDaysSelected>(
            reminder(daysOfWeekMask = 0, isPlayMode = true)
        )
    }

    @Test
    fun nichtAuswaehlbaresTagszielWirdBeanstandet() {
        assertHasProblem<ReminderValidation.Problem.DailyGoalNotSelectable>(reminder(dailyGoal = 7))
        assertTrue(ReminderValidation.isValid(reminder(dailyGoal = 7, isPlayMode = true)))
    }

    @Test
    fun offenzeitBisGefuettertIstGueltig() {
        assertTrue(
            ReminderValidation.isValid(
                reminder(openDurationSeconds = ReminderOpenDuration.UNTIL_FED)
            )
        )
    }

    @Test
    fun offenzeitNullWirdBeanstandet() {
        assertHasProblem<ReminderValidation.Problem.OpenDurationInvalid>(reminder(openDurationSeconds = 0))
    }

    // --- Alle Beanstandungen auf einmal ------------------------------------------------------

    /**
     * Wer eine kaputte Erinnerung repariert, soll nicht nach jedem behobenen Punkt erneut
     * anstossen muessen, um den naechsten zu erfahren.
     */
    @Test
    fun mehrereFehlerWerdenGemeinsamGemeldet() {
        val problems = ReminderValidation.validate(
            reminder(label = "", daysOfWeekMask = 0, intervalMinutes = 0, profileId = "")
        )
        assertTrue("erwartet mindestens vier Beanstandungen, bekommen: $problems", problems.size >= 4)
    }

    // --- Die mitgelieferten Vorgaben ---------------------------------------------------------

    /**
     * Die Standard-Erinnerungen landen in jedem neu angelegten Profil. Ein krummer Wert darin
     * laege damit bei jedem Nutzer - und weil sie in einer Transaktion im DAO entstehen, faellt
     * das sonst nirgends auf.
     */
    @Test
    fun alleMitgeliefertenVorgabenSindGueltig() {
        // Ueber die kontextfreie Fassung: geprueft werden Zeiten, Wochentage und Abstaende -
        // die Beschriftung ist hier egal und braeuchte sonst ein Android-Geraet.
        DefaultReminders.all(drinkLabel = "Trinken", moveLabel = "Bewegen").forEach { vorgabe ->
            val problems = ReminderValidation.validate(vorgabe.copy(profileId = "puffling"))
            assertEquals("\"${vorgabe.label}\": $problems", emptyList<ReminderValidation.Problem>(), problems)
        }
    }
}
