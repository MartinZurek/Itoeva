package com.notime.glyphsim.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.DaysOfWeekMask
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphcore.reminder.PlayModeState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek

/**
 * **Charakterisierungstests fuer die Moduswechsel — Phase 1a.**
 *
 * Sie halten fest, was HEUTE passiert, nicht was passieren sollte. Der bevorstehende Umbau
 * (Phase 3: echte Erinnerungen laufen auch waehrend die Welt lebt) fasst genau diese Mechanik an,
 * und ohne Netz waere jede Abweichung ein Zufall statt einer Entscheidung.
 *
 * ## Warum instrumentiert
 *
 * Der Modus steht in zwei SharedPreferences-Dateien - eine davon im gemeinsamen Kern, weil
 * Alarm-Empfaenger und Planer sie aus kalt gestarteten Prozessen lesen muessen. Ein JVM-Test
 * muesste die Ablage nachbauen und wuerde damit den Nachbau pruefen statt die Sache.
 *
 * ## Die eigentliche Aussage
 *
 * Der Modus ist keine Anzeigeeinstellung: er entscheidet ueber
 * [PlayModeState.matchesCurrentMode], **welche Erinnerungen ueberhaupt eingeplant werden**. Genau
 * das ist Problem 2 der Produktanalyse - und die Zeile, die Phase 3b entfernen wird.
 *
 *     ./gradlew :app-sim:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class ModeTransitionCharacterizationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Der Modus des Geraets wird von diesen Tests wirklich umgestellt - die App unter Test ist
     * dieselbe Installation. Der Ausgangszustand wird deshalb gesichert und danach
     * wiederhergestellt, damit ein Testlauf nicht die Einstellungen des Nutzers verstellt.
     */
    private lateinit var vorher: AppMode

    @Before
    fun merkeAusgangszustand() {
        vorher = AppMode.current(context)
    }

    @After
    fun stelleAusgangszustandWiederHer() {
        AppMode.set(context, vorher)
    }

    private fun reminder(isPlayMode: Boolean) = GlyphReminder(
        id = if (isPlayMode) 2L else 1L,
        label = if (isPlayMode) "Spiel" else "Trinken",
        animationType = AnimationType.GENERAL,
        daysOfWeekMask = DaysOfWeekMask.toMask(DayOfWeek.entries.toSet()),
        startMinuteOfDay = 9 * 60,
        endMinuteOfDay = 18 * 60,
        intervalMinutes = 30,
        isPlayMode = isPlayMode
    )

    private val echteErinnerung = reminder(isPlayMode = false)
    private val spielErinnerung = reminder(isPlayMode = true)

    // --- Die Ableitung aus zwei Schaltern -----------------------------------------------------

    @Test
    fun jederModusLaesstSichSetzenUndWirdWiederGelesen() {
        AppMode.entries.forEach { modus ->
            AppMode.set(context, modus)
            assertEquals("Modus $modus liess sich nicht zurueckelesen", modus, AppMode.current(context))
        }
    }

    /**
     * Aus zwei Schaltern lassen sich vier Zustaende bilden, gueltig sind nur drei. Der vierte -
     * Spiel UND nur Uhr zugleich - darf durch [AppMode.set] nie entstehen.
     */
    @Test
    fun setHinterlaesstNieDenWiderspruechlichenZustand() {
        AppMode.entries.forEach { modus ->
            AppMode.set(context, modus)
            val nurUhr = WatchModePrefs.isEnabled(context)
            val spiel = PlayModePrefs.isActive(context)
            assertFalse(
                "Nach set($modus) galten \"nur Uhr\" und Spiel gleichzeitig",
                nurUhr && spiel
            )
        }
    }

    /**
     * Haelt die Ablage selbst fest: welcher Schalter zu welchem Modus gehoert. Das ist die
     * Grundlage jeder kuenftigen Migration - wer den Modus umbaut, muss wissen, was heute wo steht.
     */
    @Test
    fun jederModusHinterlaesstEineBestimmteSchalterstellung() {
        AppMode.set(context, AppMode.WATCH)
        assertTrue("WATCH muss den Nur-Uhr-Schalter setzen", WatchModePrefs.isEnabled(context))

        AppMode.set(context, AppMode.PLAY)
        assertFalse("PLAY darf den Nur-Uhr-Schalter nicht setzen", WatchModePrefs.isEnabled(context))
        assertTrue("PLAY muss den Spiel-Schalter setzen", PlayModePrefs.isActive(context))

        AppMode.set(context, AppMode.REMINDER)
        assertFalse(WatchModePrefs.isEnabled(context))
        assertFalse("REMINDER ist die Abwesenheit beider Schalter", PlayModePrefs.isActive(context))
    }

    /**
     * "Nur Uhr" hat Vorrang: steht es an, ist der Modus WATCH, unabhaengig vom Spiel-Schalter.
     * Diese Vorrangregel ist der Grund, warum der widerspruechliche Zustand nach aussen nie
     * sichtbar wird - selbst wenn er in der Ablage entstuende.
     */
    @Test
    fun nurUhrHatVorrangVorDemSpiel() {
        AppMode.set(context, AppMode.PLAY)
        WatchModePrefs.setEnabled(context, true)

        assertEquals(
            "Bei beiden Schaltern muss WATCH gewinnen",
            AppMode.WATCH,
            AppMode.current(context)
        )
    }

    // --- Die eigentliche Wirkung: welche Erinnerungen werden eingeplant? -----------------------

    /**
     * **Seit Phase 3 umgekehrt - und das ist der Kern des ganzen Umbaus.**
     *
     * Frueher stand hier das Gegenteil: im Spielmodus fielen die echten Erinnerungen aus der
     * Planung. Aus einer Anzeigeentscheidung wurde damit das Aussetzen der Erinnerungsfunktion,
     * ohne dass es irgendwo stand (Problem 2 der Produktanalyse).
     *
     * Jetzt laufen die eigenen Routinen IMMER; die gewuerfelte Zeile des Wesens kommt hinzu,
     * wenn seine Welt aktiv ist. Erst dadurch ist die Verbindung moeglich, um die es geht: eine
     * erfuellte Routine veraendert, was das Wesen von sich aus tut, waehrend die Routine
     * weiterlaeuft.
     *
     * Dass dieser Test sich beim Umbau aendern MUSSTE, war beabsichtigt - er hat die alte Zusage
     * festgehalten, damit ihre Aufhebung eine sichtbare Entscheidung ist.
     */
    @Test
    fun dieEigenenRoutinenLaufenAuchWaehrendDieWeltLaeuft() {
        AppMode.set(context, AppMode.PLAY)

        assertTrue(
            "Die echte Erinnerung bleibt eingeplant, auch wenn die Welt laeuft",
            PlayModeState.matchesCurrentMode(context, echteErinnerung)
        )
        assertTrue(
            "Die Zeile des Wesens kommt hinzu, solange seine Welt aktiv ist",
            PlayModeState.matchesCurrentMode(context, spielErinnerung)
        )
    }

    @Test
    fun imNormalbetriebWirdDieSpielZeileNichtEingeplant() {
        AppMode.set(context, AppMode.REMINDER)

        assertTrue(
            "Im Normalbetrieb wird die echte Erinnerung eingeplant",
            PlayModeState.matchesCurrentMode(context, echteErinnerung)
        )
        assertFalse(
            "Im Normalbetrieb ruht die Spiel-Zeile",
            PlayModeState.matchesCurrentMode(context, spielErinnerung)
        )
    }

    /**
     * "Nur Uhr" verhaelt sich fuer die Planung wie der Normalbetrieb - der Modus wirkt nur auf die
     * Anzeige, nicht auf `isPlayMode`.
     *
     * Das ueberrascht: nach aussen ist WATCH der ruhigste Modus, tatsaechlich laufen die echten
     * Erinnerungen dabei weiter und werden nur nirgends gezeigt. Ob das so bleiben soll, ist eine
     * Produktfrage (siehe offene Entscheidung 3) - hier steht nur, dass es heute so ist.
     */
    @Test
    fun nurUhrLaesstDieEchtenErinnerungenWeiterlaufen() {
        AppMode.set(context, AppMode.WATCH)

        assertTrue(
            "Auch bei \"nur Uhr\" bleiben echte Erinnerungen eingeplant",
            PlayModeState.matchesCurrentMode(context, echteErinnerung)
        )
    }

    // --- Uebergaenge in beide Richtungen -------------------------------------------------------

    /**
     * Der Wechsel ist verlustfrei: nach dem Hin und Zurueck gilt wieder genau das, was vorher galt.
     * Ein Modus, der beim Verlassen etwas zuruecklaesst, waere die haeufigste Ursache fuer
     * "seit gestern kommen keine Erinnerungen mehr".
     */
    @Test
    fun hinUndZurueckStelltDieAusgangslageWiederHer() {
        AppMode.entries.forEach { start ->
            AppMode.entries.forEach { ziel ->
                AppMode.set(context, start)
                AppMode.set(context, ziel)
                AppMode.set(context, start)

                assertEquals("$start -> $ziel -> $start", start, AppMode.current(context))
                assertTrue(
                    "$start -> $ziel -> $start: die echte Erinnerung muss in JEDEM Modus geplant bleiben",
                    PlayModeState.matchesCurrentMode(context, echteErinnerung)
                )
            }
        }
    }

    /**
     * **Eine Eigenart, die beim Umbau zur Falle wird.**
     *
     * [PlayModePrefs] haelt den Spiel-Schalter zusaetzlich in einem prozessweiten Zwischenspeicher
     * (`MutableStateFlow`), der beim ersten Lesen befuellt und danach **nie wieder aus der Ablage
     * aufgefrischt** wird. Wer den Zustand am Kern vorbei aendert - also direkt ueber
     * [PlayModeState], wie es der Planer koennte -, aendert die Ablage, aber nicht das, was die
     * Oberflaeche anzeigt.
     *
     * Heute faellt das nicht auf, weil nur [PlayModePrefs.setActive] schreibt. Nach Phase 3, wenn
     * der Pausen-Zustand dazukommt, waere es ein Fehler, den niemand sucht - deshalb steht er hier.
     */
    @Test
    fun derSpielSchalterWirdZwischengespeichertUndNichtAufgefrischt() {
        AppMode.set(context, AppMode.REMINDER)
        assertFalse(PlayModePrefs.isActive(context))

        // Am Zwischenspeicher vorbei direkt in die Ablage schreiben.
        PlayModeState.setActive(context, true)

        assertTrue("Die Ablage kennt den neuen Wert", PlayModeState.isActive(context))
        assertFalse(
            "Der Zwischenspeicher in PlayModePrefs merkt die Aenderung NICHT - das ist der " +
                "heutige Stand und beim Umbau zu beachten",
            PlayModePrefs.isActive(context)
        )

        // Aufraeumen: ueber die oeffentliche Schnittstelle, damit beide Seiten wieder stimmen.
        PlayModePrefs.setActive(context, false)
    }
}
