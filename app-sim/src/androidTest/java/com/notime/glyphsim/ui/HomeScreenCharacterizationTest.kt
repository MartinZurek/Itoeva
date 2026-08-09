package com.notime.glyphsim.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.notime.glyphsim.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * **Charakterisierungstests** fuer den Startbildschirm.
 *
 * ## Wozu sie da sind - und wozu ausdruecklich nicht
 *
 * Sie halten fest, was der Bildschirm **heute tut**, nicht was er idealerweise tun sollte. Das ist
 * der Unterschied zu einem gewoehnlichen Test: hier steht kein Urteil ueber richtig oder falsch,
 * sondern eine Momentaufnahme.
 *
 * Der Grund ist der bevorstehende Umbau (Phase 3.1: Zustandshoheit, Phase 4: Zerlegung des
 * Dock-Bildschirms). `HomeScreen` ist heute 1400 Zeilen lang und holt sich Datenbank,
 * Einstellungen und Zufall direkt in der Komposition. Wer das auseinandernimmt, aendert dabei
 * unweigerlich Verhalten - die Frage ist nur, ob er es merkt. Diese Tests sind das Netz: sie
 * muessen VOR dem Umbau gruen sein und danach immer noch, und jede Abweichung ist eine bewusste
 * Entscheidung statt eines Unfalls.
 *
 * Deshalb pruefen sie bewusst grobe, stabile Eigenschaften - "der Avatar ist da und ansprechbar",
 * "der Weg zu den Erinnerungen existiert" - und nicht Feinheiten wie Positionen oder Farben. Ein
 * Netz mit zu engen Maschen reisst beim ersten Umbau und wird dann entfernt statt beachtet.
 *
 *     ./gradlew :app-sim:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class HomeScreenCharacterizationTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private var geoeffneteErinnerungen = 0
    private var dockModusBetreten = 0
    private var zurBearbeitung = mutableListOf<Long>()

    private fun zeigeBildschirm() {
        compose.setContent {
            HomeScreen(
                onEnterDockMode = { dockModusBetreten++ },
                onOpenReminders = { geoeffneteErinnerungen++ },
                onOpenReminderForEdit = { zurBearbeitung += it }
            )
        }
        compose.waitForIdle()
    }

    private fun string(id: Int): String = compose.activity.getString(id)

    // --- Was der Bildschirm zeigt -------------------------------------------------------------

    @Test
    fun zeigtDenWegZuDenErinnerungen() {
        zeigeBildschirm()

        compose.onNodeWithText(string(R.string.home_reminders)).assertIsDisplayed()
    }

    /**
     * Die Uhr ist eine selbstgezeichnete Canvas-Ansicht ohne eigenen Text. Ihre Beschreibung ist
     * das Einzige, woran ein Screenreader - und dieser Test - sie erkennt.
     */
    @Test
    fun dieUhrIstBeschriftetUndAnsprechbar() {
        zeigeBildschirm()

        // Die Beschreibung enthaelt die Uhrzeit und ist damit nicht vorhersagbar; gesucht wird
        // deshalb ueber den unveraenderlichen Anfang.
        val uhrKnoten = compose.onAllNodesWithContentDescription(
            substring = true,
            label = string(R.string.a11y_clock_time).substringBefore(",")
        ).fetchSemanticsNodes()

        assertTrue("Die Uhr muss eine Beschreibung tragen", uhrKnoten.isNotEmpty())
    }

    /**
     * Seit Phase 5.1 traegt die Uhr eine Klick-Aktion mit Beschriftung - vorher war ihr Antippen
     * reine Zeigerverarbeitung und fuer die Bedienungshilfen unsichtbar. Der Test haelt fest, dass
     * der Weg in den Dock-Modus ueber die Semantik erreichbar bleibt.
     */
    @Test
    fun dieUhrFuehrtUeberDieSemantikInDenDockModus() {
        zeigeBildschirm()

        val uhr = compose.onAllNodesWithContentDescription(
            substring = true,
            label = string(R.string.a11y_clock_time).substringBefore(",")
        ).fetchSemanticsNodes()
        assertTrue("Ohne Uhr kein Test", uhr.isNotEmpty())

        compose.onNode(hasClickAction() and androidx.compose.ui.test.hasContentDescription(
            value = string(R.string.a11y_clock_time).substringBefore(","),
            substring = true
        )).performClick()
        compose.waitForIdle()

        assertEquals("Das Antippen der Uhr fuehrt in den Dock-Modus", 1, dockModusBetreten)
    }

    // --- Zustand ueber einen Neuaufbau -------------------------------------------------------

    /**
     * Der Bildschirm haelt heute eine ganze Reihe von Zustaenden direkt in der Komposition
     * (Einstellungen offen, Pflegebuch offen, Avatar-Reaktion laeuft). Beim Umbau nach Phase 3.1
     * wandert das in einen Zustandshalter - dieser Test haelt fest, dass ein Neuaufbau den
     * Bildschirm dabei nicht zerlegt.
     *
     * Bewusst nur "er steht danach noch": WAS genau erhalten bleibt, ist heute uneinheitlich und
     * soll sich beim Umbau verbessern duerfen.
     */
    @Test
    fun ueberstehtEinenNeuaufbau() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            HomeScreen(
                onEnterDockMode = { dockModusBetreten++ },
                onOpenReminders = { geoeffneteErinnerungen++ },
                onOpenReminderForEdit = { zurBearbeitung += it }
            )
        }
        compose.waitForIdle()

        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()

        compose.onNodeWithText(string(R.string.home_reminders)).assertIsDisplayed()
    }

    // --- Geteilter Text ------------------------------------------------------------------------

    /**
     * Wird der Bildschirm mit einer hereingereichten KI-Antwort aufgebaut, oeffnet er direkt den
     * Import. Das ist der Weg aus der Teilen-Funktion (siehe Manifest) und damit der einzige, bei
     * dem der Bildschirm auf etwas reagiert, das von ausserhalb der App kommt.
     */
    @Test
    fun geteilterTextOeffnetDenImport() {
        var behandelt = 0
        compose.setContent {
            HomeScreen(
                onEnterDockMode = {},
                onOpenReminders = {},
                onOpenReminderForEdit = {},
                sharedImportText = """{"reminders":[{"label":"Trinken","topic":"DRINK"}]}""",
                onSharedImportHandled = { behandelt++ }
            )
        }
        compose.waitForIdle()

        // Der Import-Dialog traegt eine eigene Ueberschrift - taucht sie auf, steht er.
        compose.onNodeWithText(string(R.string.import_title)).assertIsDisplayed()
    }
}
