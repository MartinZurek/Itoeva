package com.notime.glyphsim.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.notime.glyphsim.R
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose-Tests fuer den Erinnerungs-Bildschirm.
 *
 * ## Warum es diese Suite gibt
 *
 * Bisher pruefte kein einziger Test das, was der Nutzer tatsaechlich sieht und antippt. Geprueft
 * waren Punktrechnerei (Animationen), Planungslogik und die Datenschicht - alles unterhalb der
 * Oberflaeche. Ob sich ein Dialog oeffnet, ob der Speichern-Knopf zum richtigen Zeitpunkt gesperrt
 * ist, ob eingetippter Text einen Neuaufbau ueberlebt: dafuer gab es nur die Annahme.
 *
 * Das ist zugleich die Vorbedingung fuer die spaeteren Umbauten (Zustandshoheit, Zerlegung des
 * Dock-Bildschirms). Wer eine 2400-Zeilen-Datei ohne solche Tests auseinandernimmt, aendert
 * Verhalten, ohne es zu merken - deshalb halten diese Tests bewusst fest, was HEUTE passiert,
 * nicht was idealerweise passieren sollte.
 *
 * ## Ueber Semantik statt ueber Positionen
 *
 * Gefunden wird ueber das, was auch TalkBack vorliest - Beschriftungen und Beschreibungen, nicht
 * Bildschirmkoordinaten. Ein Test, der hier nichts findet, ist damit zugleich ein Hinweis auf ein
 * Element, das ein Screenreader ebenfalls nicht benennen koennte.
 *
 * Die Beschriftungen kommen aus den Ressourcen und nicht als Literale im Test: sonst wuerde jede
 * Umformulierung den Test brechen, ohne dass sich am Verhalten etwas geaendert haette.
 *
 *     ./gradlew :app-sim:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class ReminderScreenTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun string(id: Int, vararg args: Any): String =
        if (args.isEmpty()) compose.activity.getString(id) else compose.activity.getString(id, *args)

    private fun zeigeBildschirm() {
        compose.setContent { ReminderScreen() }
        compose.waitForIdle()
    }

    /**
     * Der Knopf wird ueber seine Beschreibung angesprochen, nicht ueber seine Beschriftung: die
     * erreicht den zusammengefuehrten Semantik-Knoten nicht (siehe [fabIstFuerScreenreaderBenannt]).
     */
    private fun oeffneNeuDialog() {
        compose.onNodeWithContentDescription(string(R.string.action_new)).performClick()
        compose.waitForIdle()
    }



    // --- Grundlegendes Verhalten -------------------------------------------------------------

    @Test
    fun neuKnopfOeffnetDenDialog() {
        zeigeBildschirm()
        oeffneNeuDialog()

        // Das Eingabefeld gibt es nur im Dialog - taucht es auf, steht der Dialog.
        compose.onNodeWithText(string(R.string.reminder_label_hint)).assertIsDisplayed()
    }

    /**
     * Haelt die Regel fest, die der Dialog heute anwendet: ohne Bezeichnung kein Speichern.
     *
     * Dieselbe Bedingung steht seit Phase 2.2 auch in `ReminderValidation` - dort als Zusage der
     * Datenschicht, hier als das, was der Nutzer davon merkt. Beides soll uebereinstimmen; laufen
     * sie auseinander, faellt es an einer der beiden Stellen auf.
     */
    @Test
    fun ohneBezeichnungBleibtSpeichernGesperrt() {
        zeigeBildschirm()
        oeffneNeuDialog()

        compose.onNodeWithText(string(R.string.action_save)).assertIsNotEnabled()
    }

    /**
     * Haelt die zweite Haelfte der Regel fest: eine Bezeichnung allein genuegt nicht, es muss auch
     * mindestens ein Wochentag gewaehlt sein.
     *
     * Beim Schreiben dieses Tests war die Annahme, die Wochentage seien vorbelegt - sie sind es
     * nicht. Ein neuer Eintrag startet ohne jeden Tag, und der Speichern-Knopf bleibt gesperrt, bis
     * einer angetippt wurde. Genau dafuer sind Charakterisierungstests da: sie halten fest, was die
     * App TUT, nicht was man annimmt.
     *
     * Warum hier nicht weitergeklickt und der Gutfall zu Ende gespielt wird, steht bei
     * [wochentagsKuerzelSindMehrdeutig].
     */
    @Test
    fun bezeichnungAlleinGenuegtNichtZumSpeichern() {
        zeigeBildschirm()
        oeffneNeuDialog()

        compose.onNodeWithText(string(R.string.reminder_label_hint)).performTextInput("Trink was")
        compose.waitForIdle()

        compose.onNodeWithText(string(R.string.action_save)).assertIsNotEnabled()
    }

    /**
     * **Zweiter Fund dieser Suite, hier bewusst nur festgehalten statt behoben.**
     *
     * Die Wochentage stehen als einzelne Buchstaben auf den Kacheln: M, T, W, T, F, S, S. Zwei
     * davon kommen doppelt vor - "T" fuer Dienstag und Donnerstag, "S" fuer Samstag und Sonntag -
     * und sie tragen keine Beschreibung. Ein Screenreader liest also zweimal "T" vor, ohne dass
     * sich die beiden auseinanderhalten liessen; und ein Test kann eine bestimmte Kachel aus
     * demselben Grund nicht eindeutig ansprechen. Deshalb endet der Gutfall oben beim gesperrten
     * Knopf.
     *
     * Die Behebung gehoert zu Phase 5.1/5.3: je Wochentag eine ausgeschriebene Beschreibung als
     * uebersetzte Ressource. Dieser Test haelt den Zustand fest, damit er nicht in Vergessenheit
     * geraet - er schlaegt fehl, sobald die Kacheln eindeutig werden, und ist dann durch einen
     * echten Gutfall-Test zu ersetzen.
     */
    @Test
    fun wochentagsKuerzelSindMehrdeutig() {
        zeigeBildschirm()
        oeffneNeuDialog()

        // Mehr als ein Knoten mit "T": solange das so ist, laesst sich Dienstag nicht von
        // Donnerstag unterscheiden - weder fuer TalkBack noch fuer einen Test.
        val mitT = compose.onAllNodesWithText("T").fetchSemanticsNodes()
        assertTrue(
            "Erwartet mehrere mehrdeutige \"T\"-Kacheln, gefunden: ${mitT.size}",
            mitT.size > 1
        )
    }

    // --- Zustand ueber einen Neuaufbau -------------------------------------------------------

    /**
     * **Der Nachweis fuer Phase 3.4.**
     *
     * `StateRestorationTester` macht genau das, was Android beim Abraeumen einer App tut: er
     * sichert den speicherbaren Zustand, wirft die Komposition weg und baut sie daraus neu auf.
     * Vor 3.4 war die eingetippte Bezeichnung danach verschwunden - das laesst sich mit einem
     * JVM-Test ueber die Saver allein nicht zeigen, weil dort nur die Uebersetzung geprueft wird
     * und nicht, ob der Bildschirm sie ueberhaupt benutzt.
     */
    @Test
    fun eingetippteBezeichnungUeberlebtDenNeuaufbau() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { ReminderScreen() }
        compose.waitForIdle()

        oeffneNeuDialog()
        compose.onNodeWithText(string(R.string.reminder_label_hint)).performTextInput("Beweg dich")
        compose.waitForIdle()

        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()

        // Der Dialog muss wieder offen sein UND die Eingabe noch tragen.
        compose.onNodeWithText("Beweg dich").assertIsDisplayed()
    }

    /** Auch die Tatsache, DASS ein Dialog offen war, gehoert zum wiederhergestellten Zustand. */
    @Test
    fun offenerDialogBleibtNachDemNeuaufbauOffen() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { ReminderScreen() }
        compose.waitForIdle()

        oeffneNeuDialog()
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()

        compose.onNodeWithText(string(R.string.reminder_label_hint)).assertIsDisplayed()
    }

    // --- Barrierefreiheit --------------------------------------------------------------------

    /**
     * Bei 200 % Schriftgroesse - der hoechsten Stufe, die Android in den Bedienungshilfen anbietet -
     * muss der Bildschirm weiterhin stehen und bedienbar bleiben.
     *
     * Geprueft wird hier bewusst nur, dass nichts wegbricht: dass der Aufbau die Vergroesserung
     * ueberlebt und die zentralen Bedienelemente auffindbar bleiben. Ob dabei etwas abgeschnitten
     * aussieht, kann ein Test nicht beurteilen - das gehoert in die Sichtpruefung von Phase 5.2.
     */
    @Test
    fun bildschirmUeberstehtDoppelteSchriftgroesse() {
        compose.setContent {
            val dichte = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = dichte.density, fontScale = 2f)
            ) {
                ReminderScreen()
            }
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription(string(R.string.action_new)).assertIsDisplayed()
        oeffneNeuDialog()
        compose.onNodeWithText(string(R.string.reminder_label_hint)).assertIsDisplayed()
    }

    /**
     * **Dieser Test hat eine echte Luecke gefunden.**
     *
     * Der "Neu"-Knopf trug im zusammengefuehrten Semantik-Baum weder Beschriftung noch
     * Beschreibung - nur `Role = Button`. Ein Screenreader haette dort "Schaltflaeche" vorgelesen
     * und sonst nichts, obwohl gross und sichtbar "New" darauf steht: die Beschriftung des
     * ExtendedFloatingActionButton erreicht den zusammengefuehrten Knoten nicht.
     *
     * Aufgefallen ist es, weil dieser Test den Knopf ueber genau die Semantik sucht, die auch
     * TalkBack benutzt - und ihn nicht fand. Genau dafuer ist der Zugang ueber Semantik statt ueber
     * Bildschirmpositionen da.
     */
    @Test
    fun fabIstFuerScreenreaderBenannt() {
        zeigeBildschirm()

        compose.onNodeWithContentDescription(string(R.string.action_new))
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    /**
     * Der Zurueck-Pfeil ist ein reines Symbol. Ohne Beschreibung liest ein Screenreader dort
     * bestenfalls "Schaltflaeche" vor - der Nutzer erfaehrt nicht, wohin sie fuehrt.
     */
    @Test
    fun zurueckPfeilTraegtEineBeschreibung() {
        compose.setContent { ReminderScreen(onBack = {}) }
        compose.waitForIdle()

        compose.onNodeWithContentDescription(string(R.string.action_back)).assertIsDisplayed()
    }
}
