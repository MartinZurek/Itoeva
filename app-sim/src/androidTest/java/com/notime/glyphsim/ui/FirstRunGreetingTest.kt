package com.notime.glyphsim.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.notime.glyphsim.R
import com.notime.glyphsim.settings.SettingsCatalog
import com.notime.glyphsim.settings.SettingsStore
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * **Der erste Start - das Wesen stellt sich von selbst vor.**
 *
 * Vorher zeigte der erste Start eine Uhr, eine Kreatur und eine Sprechblase "Tipp mich an". Wer
 * nicht auf die Idee kam, die Kreatur anzufassen, erfuhr nie, worum es hier geht - und schon gar
 * nicht das Wichtigste: dass nichts gezaehlt wird und es nichts aufrechtzuerhalten gibt. Wer das
 * nicht weiss, liest jede Erinnerung als Forderung.
 *
 * ## Warum das einen eigenen Test braucht
 *
 * Es ist ein Verhalten, das **genau einmal je Installation** stattfindet. Solche Dinge sind
 * unpruefbar, wenn niemand den Merker kontrolliert - und sie faerben nebenbei auf andere Tests
 * ab: Der erste Test, der den Startbildschirm aufbaut, verbraucht die Begruessung fuer alle
 * folgenden. Genau deshalb setzt dieser Test den Merker selbst und stellt ihn danach wieder her,
 * und [HomeScreenCharacterizationTest] schaltet ihn vorsorglich ab.
 */
@RunWith(AndroidJUnit4::class)
class FirstRunGreetingTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val context get() = compose.activity

    private var vorher = false

    @Before
    fun merkeMerker() {
        vorher = SettingsStore.read(context, SettingsCatalog.Greeted)
    }

    @After
    fun stelleMerkerWiederHer() {
        SettingsStore.write(context, SettingsCatalog.Greeted, vorher)
    }

    private fun zeigeStartbildschirm() {
        compose.setContent {
            HomeScreen(
                onEnterDockMode = {},
                onOpenReminders = {},
                onOpenReminderForEdit = {}
            )
        }
        compose.waitForIdle()
    }

    private fun string(id: Int): String = context.getString(id)

    /**
     * **Der eigentliche Punkt.** Ohne Zutun steht der Satz da, der die ganze App einordnet: keine
     * Serien, keine Punkte, nichts aufrechtzuerhalten.
     *
     * Geprueft wird dieser zweite Satz und nicht die Begruessung davor - ein "Hi" koennte auch
     * aus einer anderen Ecke kommen, dieser Satz steht nur hier.
     */
    @Test
    fun beimErstenStartStelltSichDasWesenVonSelbstVor() {
        SettingsStore.write(context, SettingsCatalog.Greeted, false)

        zeigeStartbildschirm()

        compose.onNodeWithText(string(R.string.assistant_greeting_2)).assertIsDisplayed()
    }

    /**
     * Und danach nie wieder. Ein Gruss, der bei jedem Start wiederkommt, ist keiner mehr, sondern
     * ein Dialog, den man wegtippen muss.
     */
    @Test
    fun beimZweitenStartKommtNichtsMehrVonSelbst() {
        SettingsStore.write(context, SettingsCatalog.Greeted, true)

        zeigeStartbildschirm()

        compose.onNodeWithText(string(R.string.assistant_greeting_2)).assertDoesNotExist()
    }

    /**
     * Der Merker wird beim Aufgehen gesetzt, nicht beim Schliessen. Wer wegtippt, hat sich
     * entschieden - kaeme der Gruss beim naechsten Start wieder, waere das kein Gruss, sondern
     * eine Aufforderung.
     */
    @Test
    fun derGrussGiltAlsErledigtSobaldErStand() {
        SettingsStore.write(context, SettingsCatalog.Greeted, false)

        zeigeStartbildschirm()

        assertTrue(
            "Nach dem Aufgehen muss der Merker stehen, auch ohne dass jemand geantwortet hat",
            SettingsStore.read(context, SettingsCatalog.Greeted)
        )
    }

    /**
     * Die Begruessung ersetzt nicht den Hinweis, dass man das Wesen auch selbst antippen kann -
     * das eine laeuft von allein, das andere ist eine Bedienung, die man kennen muss. Deshalb
     * bleiben es zwei getrennte Merker.
     */
    @Test
    fun derGrussVerbrauchtDenAntipp_HinweisNicht() {
        SettingsStore.write(context, SettingsCatalog.Greeted, false)
        SettingsStore.write(context, SettingsCatalog.TappedAvatar, false)

        zeigeStartbildschirm()

        assertFalse(
            "Der automatische Gruss darf nicht als \"schon angetippt\" gelten",
            OnboardingPrefs.hasTappedAvatar(context)
        )
    }
}
