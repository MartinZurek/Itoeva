package com.notime.glyphsim.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.notime.glyphsim.R
import java.time.DayOfWeek
import java.time.format.TextStyle
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



    /**
     * Loest die Klick-Aktion ueber die Semantik aus statt ueber einen eingespritzten Tipp.
     *
     * Genau diesen Weg gehen auch TalkBack und Switch Access - fuer eine Pruefung der
     * Barrierefreiheit ist er also der richtige. Er ist hier zugleich der einzige, der
     * funktioniert: der Dialog liegt in einem eigenen Fenster, und ein an Wurzelkoordinaten
     * eingespritzter Tipp erreicht die Kacheln darin nicht (nachgemessen - der Zustand blieb
     * unveraendert, obwohl der Knoten gefunden wurde).
     */
    private fun SemanticsNodeInteraction.klickeUeberSemantik() =
        performSemanticsAction(SemanticsActions.OnClick)

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

    /**
     * Der Gutfall, der vorher nicht schreibbar war.
     *
     * Die Wochentagskacheln zeigen nur Kuerzel, und "T" wie "S" kommen doppelt vor - eine
     * bestimmte Kachel liess sich dadurch weder von TalkBack noch von einem Test eindeutig
     * ansprechen. Seit Phase 5.1 tragen sie den ausgeschriebenen Namen als Beschreibung; genau
     * darueber greift dieser Test zu.
     *
     * Der Name kommt aus derselben Quelle wie in der Oberflaeche (java.time mit der Sprache des
     * Geraets), nicht als Literal - sonst waere der Test an eine Sprache gebunden.
     */
    @Test
    fun mitBezeichnungUndEinemWochentagLaesstSichSpeichern() {
        zeigeBildschirm()
        oeffneNeuDialog()

        compose.onNodeWithText(string(R.string.reminder_label_hint)).performTextInput("Trink was")
        val montag = DayOfWeek.MONDAY.getDisplayName(
            TextStyle.FULL,
            compose.activity.resources.configuration.locales[0]
        )
        compose.onNodeWithContentDescription(montag).klickeUeberSemantik()
        compose.waitForIdle()

        compose.onNodeWithText(string(R.string.action_save)).assertIsEnabled()
    }

    /**
     * Die Kachel ist ein Schalter, kein Knopf: sie meldet ihren Zustand mit. Ohne das laese ein
     * Screenreader zwar den Wochentag vor, aber nicht, ob er gerade aktiv ist - und genau das ist
     * die Information, die die Kachel optisch traegt.
     */
    @Test
    fun wochentagsKachelMeldetIhrenZustand() {
        zeigeBildschirm()
        oeffneNeuDialog()

        val dienstag = DayOfWeek.TUESDAY.getDisplayName(
            TextStyle.FULL,
            compose.activity.resources.configuration.locales[0]
        )
        compose.onNodeWithContentDescription(dienstag).assertIsOff()
        compose.onNodeWithContentDescription(dienstag).klickeUeberSemantik()
        compose.waitForIdle()
        compose.onNodeWithContentDescription(dienstag).assertIsOn()
    }





    /**
     * Android verlangt fuer Bedienelemente mindestens 48x48 dp.
     *
     * **Die Hoehe ist erreicht, die Breite nicht - und das ist keine Nachlaessigkeit, sondern
     * Arithmetik.** Nachgemessen auf diesem Geraet: der Dialoginhalt ist rund 272 dp breit. Sieben
     * Kacheln nebeneinander zu je 48 dp braeuchten 336 dp. Das passt nicht, und kein Setzen einer
     * Mindestbreite aendert daran etwas - es wuerde die Zeile nur ueber den Rand schieben.
     *
     * Was hier trotzdem besser wurde: die Trefferflaeche ist von dem sichtbaren 38-dp-Kreis
     * getrennt, sie teilt sich die verfuegbare Breite gleichmaessig (auf breiteren Bildschirmen
     * also mehr) und ist mindestens 48 dp hoch. Vorher war das Ziel in beiden Richtungen 38 dp.
     *
     * Die volle Vorgabe braeuchte ein anderes Layout - zwei Zeilen zu vier und drei Kacheln, dann
     * bleiben je rund 68 dp. Das ist eine Gestaltungsentscheidung und keine, die ein Test faellen
     * kann; dieser haelt so lange den erreichten Stand fest.
     */
    @Test
    fun wochentagsKachelnHabenEinAusreichendGrossesZiel() {
        zeigeBildschirm()
        oeffneNeuDialog()
        val locale = compose.activity.resources.configuration.locales[0]

        DayOfWeek.entries.forEach { day ->
            compose.onNodeWithContentDescription(day.getDisplayName(TextStyle.FULL, locale))
                // Volle Vorgabe.
                .assertHeightIsAtLeast(48.dp)
                // Erreichbares Maximum in einer Zeile - siehe die Rechnung oben.
                .assertWidthIsAtLeast(38.dp)
        }
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
