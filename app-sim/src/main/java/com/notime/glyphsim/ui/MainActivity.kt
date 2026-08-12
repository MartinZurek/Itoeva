package com.notime.glyphsim.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notime.glyphcore.reminder.ReminderScheduler
import com.notime.glyphsim.matrix.PlayTimeLapse
import com.notime.glyphsim.matrix.PlayWeather
import com.notime.glyphsim.reminder.ReminderTrigger
import com.notime.glyphsim.state.Presentation
import com.notime.glyphsim.state.TamaStateMapping
import com.notime.glyphsim.ui.theme.GlyphSimTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Screen { HOME, REMINDERS }

class MainActivity : ComponentActivity() {

    /**
     * Von aussen hereingereichter Text (Teilen aus einer KI-App). Als Zustand und nicht als
     * Konstruktor-Wert, weil er auch waehrend der Laufzeit ankommen kann - siehe [onNewIntent].
     */
    private val sharedText = mutableStateOf<String?>(null)

    /**
     * Setzt die gewaehlte Oberflaechensprache, bevor die erste Ressource gelesen wird (siehe
     * [LanguagePrefs.wrap]). Ein spaeterer Wechsel im Einstellungs-Dialog ruft [recreate] auf,
     * wodurch diese Methode erneut durchlaufen wird.
     */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguagePrefs.wrap(newBase))
    }

    /**
     * Teilen bei bereits laufender App. Wegen `launchMode="singleTask"` (siehe Manifest) wird
     * keine zweite Instanz erzeugt, sondern die bestehende bekommt den Intent hier zugestellt -
     * ohne diese Methode kaeme geteilter Text bei laufender App schlicht nie an.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractSharedText(intent)?.let { sharedText.value = it }
    }

    /** Nur echter Text aus einer Teilen-Absicht - alles andere ignorieren. */
    private fun extractSharedText(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        return intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Frueher stand hier ein einmaliger Anstoss, die Exact-Alarm-Berechtigung zu erteilen.
        // Die App deklariert sie nicht mehr (Begruendung im Manifest): sie zeigt Erinnerungen
        // rein visuell, und bei dunklem Display loest sie bewusst gar nicht erst aus. Damit gab
        // es nichts mehr zu erbitten - und der Nutzer wird beim ersten Start nicht laenger in
        // die Systemeinstellungen geschickt.

        // Sicherheitsnetz: siehe MainActivity.kt im :app-Modul fuer die Begruendung
        // (Alarme werden zwar bei jeder Aenderung + nach Reboot neu gesetzt, das hier
        // faengt nur Randfaelle wie eine Neuinstallation ohne Reboot dazwischen ab).
        lifecycleScope.launch { ReminderScheduler.rescheduleAll(this@MainActivity) }

        // Der eigentliche Antrieb der Erinnerungen, solange die App sichtbar ist: ein Takt im
        // laufenden Prozess statt AlarmManager. Begruendung ausfuehrlich in [ReminderTrigger] -
        // kurz: ohne Exact-Alarm-Berechtigung drosselt das System Alarme auf grob einen alle
        // 9-15 Minuten, und genau dann bliebe eine auf "alle 5 Minuten" gestellte Erinnerung
        // sichtbar aus. Diese Schleife kennt weder Doze noch Drosselung.
        //
        // repeatOnLifecycle(RESUMED) und nicht lifecycleScope allein: im Hintergrund ist ohnehin
        // nichts zu sehen, dort soll der Takt schlafen und keinen Akku kosten. Beim
        // Zurueckkehren laeuft er automatisch wieder an - und holt dabei einen kurz zuvor
        // verpassten Slot nach.
        //
        // Der Takt haengt sich an denselben Minutenwechsel wie die Uhr ([millisUntilNextMinute],
        // siehe ClockTick.kt) statt an ein eigenes Intervall. Erinnerungs-Slots liegen immer auf
        // vollen Minuten - Fensterbeginn plus einem Vielfachen des Intervalls in Minuten -, ein
        // feineres Raster brachte also nichts ausser Abfragen ins Leere. Der sichtbare Gewinn:
        // Uhr und Erinnerung wechseln im selben Moment, eine im Minutentakt gestellte Erinnerung
        // feuert genau dann, wenn die Anzeige umspringt, statt bis zu zehn Sekunden daneben.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    // Erster Durchlauf sofort, damit ein gerade verpasster Slot beim Aufwecken
                    // nicht bis zum naechsten Minutenwechsel warten muss.
                    ReminderTrigger.firePending(this@MainActivity)
                    delay(millisUntilNextMinute())
                }
            }
        }

        // Startwert aus den Einstellungen - das Manifest legt nur den Ausgangszustand fest,
        // umgeschaltet wird zur Laufzeit (siehe OrientationPrefs).
        OrientationPrefs.apply(this)

        applyDockModeFlags(DockModePrefs.isEnabled(this))

        // Testschalter aus den Einstellungen uebernehmen - PlayTimeLapse haelt seinen Zustand im
        // Speicher, damit die beschleunigte Uhr im Zeichenpfad ohne Dateizugriff auskommt.
        PlayTimeLapse.restore(this)
        // Dasselbe fuer das festgehaltene Wetter (siehe PlayWeather).
        PlayWeather.restore(this)

        sharedText.value = extractSharedText(intent)

        setContent {
            GlyphSimTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    // rememberSaveable: der gewaehlte Bildschirm ueberlebt den Neuaufbau der
                    // Activity und den Prozesstod. Vorher landete jeder, der aus dem Hintergrund
                    // zurueckkam, wieder auf dem Startbildschirm - auch mitten aus der
                    // Erinnerungsliste heraus. Screen ist ein Enum und damit ohne eigenen Saver
                    // bundle-faehig.
                    var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
                    // Nur fuer das Tapthrough vom Rhythmus-Kommentar im Pflegebuch (siehe
                    // FeedStatsDialog) gesetzt - der normale Weg ueber onOpenReminders laesst es
                    // auf null, dann oeffnet ReminderScreen keinen Bearbeiten-Dialog von selbst.
                    var editReminderId by rememberSaveable { mutableStateOf<Long?>(null) }
                    var dockEnabled by remember { mutableStateOf(DockModePrefs.isEnabled(this@MainActivity)) }
                    val shared by sharedText
                    val playActive by PlayModePrefs.active(this@MainActivity)
                        .collectAsStateWithLifecycle(initialValue = PlayModePrefs.isActive(this@MainActivity))
                    val watchOnly by WatchModePrefs.enabled(this@MainActivity)
                        .collectAsStateWithLifecycle(initialValue = WatchModePrefs.isEnabled(this@MainActivity))

                    /*
                     * Die drei Schalter zu EINEM Zustand zusammengefasst (Phase 2).
                     *
                     * Der Gewinn ist nicht weniger Code, sondern eine Unterscheidung, die vorher
                     * nur in Kommentaren stand: `presentation` sagt, WIE Tama gezeigt wird,
                     * `world` sagt, WAS laeuft. Die Bedingungen weiter unten lesen sich damit als
                     * das, was sie meinen - "ist das die Dock-Darstellung?" statt einer
                     * Verknuepfung dreier Wahrheitswerte, deren Vorrangregeln man kennen muss.
                     *
                     * Die Zuordnung selbst liegt in TamaStateMapping und ist dort geprueft; hier
                     * aendert sich nichts am Verhalten. Genau darum geht es: wenn Phase 3 den
                     * Spielmodus aufloest, ist eine Stelle zu aendern statt dieser Bedingungen.
                     */
                    val state = TamaStateMapping.fromSwitches(
                        watchOnly = watchOnly == true,
                        playActive = playActive == true,
                        dockEnabled = dockEnabled
                    )

                    fun setDockMode(enabled: Boolean) {
                        dockEnabled = enabled
                        DockModePrefs.setEnabled(this@MainActivity, enabled)
                        applyDockModeFlags(enabled)
                    }

                    // Geteilter Text hat Vorrang vor dem Dock-Modus: wer gerade etwas
                    // herueberreicht, will das Ergebnis sehen und nicht auf einem schwarzen
                    // Uhrenbildschirm landen.
                    // Geteilter Text hat weiterhin Vorrang vor der Dock-Darstellung.
                    if (state.presentation == Presentation.DOCK && shared == null) {
                        // Der ViewModel wird hier NUR fuer das Anlegen aus dem Gespraech heraus
                        // geholt (siehe PlayTalk). Der Dock-Bildschirm selbst kommt weiterhin
                        // ohne aus - er zeigt eine Welt und verwaltet keine Erinnerungen.
                        val reminderViewModel: GlyphReminderViewModel = viewModel()
                        DockScreen(
                            playMode = state.showsPlayProgress,
                            watchOnly = state.clockOnly,
                            onExit = { setDockMode(false) },
                            onAddHabit = { topic ->
                                // Mit der Tageszeit, die der Nutzer im Gespraech genannt hat -
                                // siehe PlayUserProfile. Wer abends Zeit hat, bekommt keine
                                // Erinnerung, die morgens um neun anfaengt.
                                val preset = PlayTalk.presetFor(
                                    topic,
                                    PlayUserProfile.busyPhase(this@MainActivity)
                                )
                                reminderViewModel.addReminder(
                                    label = getString(topic.labelRes),
                                    animationChoice = AnimationChoice.BuiltIn(topic),
                                    daysOfWeekMask = PlayTalk.EVERY_DAY_MASK,
                                    startMinuteOfDay = preset.startMinuteOfDay,
                                    endMinuteOfDay = preset.endMinuteOfDay,
                                    intervalMinutes = preset.intervalMinutes,
                                    dailyGoal = preset.dailyGoal
                                )
                            },
                            onAdjustHabit = { reminder ->
                                reminderViewModel.updateReminder(reminder)
                            },
                            onOpenReminders = {
                                setDockMode(false)
                                screen = Screen.REMINDERS
                            }
                        )
                    } else {
                        when (screen) {
                            Screen.HOME -> HomeScreen(
                                onEnterDockMode = { setDockMode(true) },
                                onOpenReminders = { screen = Screen.REMINDERS },
                                onOpenReminderForEdit = { reminderId ->
                                    editReminderId = reminderId
                                    screen = Screen.REMINDERS
                                },
                                sharedImportText = shared,
                                onSharedImportHandled = { sharedText.value = null }
                            )
                            Screen.REMINDERS -> ReminderScreen(
                                onBack = { screen = Screen.HOME; editReminderId = null },
                                initialEditReminderId = editReminderId
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Dock-Modus: haelt den Bildschirm an (FLAG_KEEP_SCREEN_ON) und laesst die App
     * notfalls auch ueber dem Sperrbildschirm sichtbar bleiben (wie eine Nachttisch-
     * Uhr) - kein echtes "Screen-off"-AOD (siehe README), der Bildschirm bleibt dabei
     * aktiv, verbraucht also spuerbar mehr Akku als im Normalbetrieb. Die Helligkeit
     * wird dabei NICHT automatisch gedimmt - laeuft mit der normalen Systemhelligkeit,
     * genau wie das Home-Screen-Widget auch. Wer trotzdem dimmen will, kann das ueber
     * den optionalen Regler in HomeScreen (DockBrightnessPrefs) einschalten - Status-/
     * Navigationsleiste werden aber immer ausgeblendet, damit wirklich nur die Uhr auf
     * schwarzem Grund zu sehen ist.
     */
    private fun applyDockModeFlags(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(enabled)
            setTurnScreenOn(enabled)
        } else {
            @Suppress("DEPRECATION")
            if (enabled) {
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            } else {
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
        }

        val attributes = window.attributes
        attributes.screenBrightness = if (enabled && DockBrightnessPrefs.isOverrideEnabled(this)) {
            DockBrightnessPrefs.getBrightness(this)
        } else {
            WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        window.attributes = attributes

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (enabled) {
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
