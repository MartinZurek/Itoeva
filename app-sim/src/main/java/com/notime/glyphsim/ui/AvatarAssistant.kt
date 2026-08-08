package com.notime.glyphsim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.DaysOfWeekMask
import com.notime.glyphsim.R
import com.notime.glyphsim.matrix.AvatarAnimations
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.AvatarSpriteView
import java.time.DayOfWeek
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Dunkle Flaechen - dieselbe Welt wie Startbildschirm und Pflegebuch. */
private val SHEET_BG = Color(0xFF101012)
private val BUBBLE_BG = Color(0xFF1E1E22)
private val TEXT_PRIMARY = Color(0xFFF1EEE6)
private val TEXT_MUTED = Color(0xFF9A968E)

/** Was der Avatar-Assistent gerade zeigt. */
enum class AssistantScreen { MENU, DAY, INTRO, SETUP, IMPORT, FAQ }

/**
 * Der Avatar als Einstieg statt als blosser Schalter.
 *
 * Ein Antippen oeffnete bisher direkt die Einstellungen - eine Abkuerzung, die nur weiterhilft,
 * wenn man ohnehin weiss, was man sucht. Wer die App zum ersten Mal oeffnet, sieht eine Uhr und
 * eine Kreatur und hat keinen Anhaltspunkt, was er tun soll. Die Kreatur ist die naheliegendste
 * Anlaufstelle, also fuehrt sie jetzt auch: sie bietet an, zu erklaeren, gemeinsam etwas
 * einzurichten - oder eben in die Einstellungen zu gehen.
 *
 * Bewusst als Sprechblasen: es soll sich nach Ansprechen anfuehlen, nicht nach Menue. Deshalb
 * steht der Avatar auf jedem Schritt sichtbar daneben.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AvatarAssistantDialog(
    species: AvatarSpecies,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    /** Nur gesetzt, wenn es fuer [species] tatsaechlich einen Clip gibt (siehe AvatarClips) -
     *  der Menuepunkt "Spiel meinen Clip" erscheint dann erst gar nicht, statt einen leeren
     *  oder kaputten Player zu zeigen. */
    onPlayClip: (() -> Unit)? = null
) {
    var screen by remember { mutableStateOf(AssistantScreen.MENU) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SHEET_BG,
        titleContentColor = TEXT_PRIMARY,
        textContentColor = TEXT_PRIMARY,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AvatarSpriteView(
                    frame = remember(species) { AvatarAnimations.idlePose(species) },
                    showBackground = false,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    stringResource(species.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (screen) {
                    AssistantScreen.MENU -> AssistantMenu(
                        onDay = { screen = AssistantScreen.DAY },
                        onIntro = { screen = AssistantScreen.INTRO },
                        onSetup = { screen = AssistantScreen.SETUP },
                        onImport = { screen = AssistantScreen.IMPORT },
                        onFaq = { screen = AssistantScreen.FAQ },
                        onSettings = onOpenSettings,
                        onPlayClip = onPlayClip
                    )
                    AssistantScreen.DAY -> AssistantDay(
                        species = species,
                        onBack = { screen = AssistantScreen.MENU }
                    )
                    AssistantScreen.INTRO -> AssistantIntro(onBack = { screen = AssistantScreen.MENU })
                    AssistantScreen.SETUP -> AssistantSetup(
                        onDone = onDismiss,
                        onBack = { screen = AssistantScreen.MENU }
                    )
                    AssistantScreen.IMPORT ->
                        ReminderImportDialog(onDismiss = { screen = AssistantScreen.MENU })
                    AssistantScreen.FAQ -> AssistantFaq(onBack = { screen = AssistantScreen.MENU })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
        }
    )
}

/** Eine Sprechblase des Avatars. */
@Composable
private fun Bubble(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = TEXT_PRIMARY,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp))
            .background(BUBBLE_BG)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}

/** Eine antippbare Antwort - optisch als das, was der Nutzer sagen wuerde. */
@Composable
private fun Choice(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = TEXT_PRIMARY,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2A2A30))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    )
}

@Composable
private fun AssistantMenu(
    onDay: () -> Unit,
    onIntro: () -> Unit,
    onSetup: () -> Unit,
    onImport: () -> Unit,
    onFaq: () -> Unit,
    onSettings: () -> Unit,
    onPlayClip: (() -> Unit)?
) {
    // Der Spielmodus steht bewusst NICHT hier, sondern als Schalter in der oberen Leiste des
    // Startbildschirms: Er bestimmt, welche Erinnerungen ueberhaupt kommen, und muss deshalb
    // jederzeit sichtbar sein statt zwei Ebenen tief in einem Menue zu liegen.
    Bubble(stringResource(R.string.assistant_greeting))
    // **ZUERST der Tag, dann die Verwaltung.** Bis hierhin bedeutete dasselbe Antippen derselben
    // Figur auf den beiden Bildschirmen etwas voellig Verschiedenes: im Play-Modus ein Gespraech
    // ueber den heutigen Tag, hier ein Menue aus Anleitung, Import und Einstellungen. Wer den
    // Avatar antippt, will aber in beiden Faellen dasselbe - mit ihm reden.
    //
    // Die Fragen sind dieselben wie im Play-Modus und beziehen ihre Antworten aus derselben
    // Quelle (siehe PlayTalk); nur die Umgebung wechselt, weil ein heller Dialog auf dem
    // Startbildschirm richtig ist und auf der schwarzen Spielflaeche falsch waere.
    Choice(stringResource(R.string.assistant_menu_day), onDay)
    Choice(stringResource(R.string.assistant_menu_intro), onIntro)
    Choice(stringResource(R.string.assistant_menu_setup), onSetup)
    Choice(stringResource(R.string.assistant_menu_import), onImport)
    Choice(stringResource(R.string.assistant_menu_faq), onFaq)
    onPlayClip?.let { Choice(stringResource(R.string.clip_play), it) }
    Choice(stringResource(R.string.assistant_menu_settings), onSettings)
}

/**
 * **Derselbe Tagesbericht wie im Play-Modus**, nur in Sprechblasen statt auf schwarzem Grund.
 *
 * Die Zahlen kommen aus [PlayTalk], die Formulierungen aus [PlayVoice] - beides unveraendert
 * geteilt. Was sich unterscheidet, ist ausschliesslich die Umgebung: Der Startbildschirm ist ein
 * heller Dialog mit Sprechblasen, die Spielflaeche ist schwarz. Dieselbe Auskunft in der jeweils
 * passenden Form zu zeigen ist Konsequenz; sie zweimal zu FORMULIEREN waere die Art von Kopie,
 * bei der nach dem dritten Umbau die eine Haelfte etwas anderes sagt als die andere.
 *
 * Bewusst OHNE Vorschlag zum Anlegen: Der Startbildschirm hat mit "Erinnerungen" und dem
 * gefuehrten Einrichten bereits zwei Wege dorthin, und ein dritter waere genau die Redundanz, die
 * hier gerade abgebaut wird. Im Play-Modus gibt es diese Wege nicht - dort ist der Vorschlag der
 * einzige, und deshalb steht er nur dort.
 */
@Composable
private fun AssistantDay(species: AvatarSpecies, onBack: () -> Unit) {
    val context = LocalContext.current
    val voice = remember(species) { PlayVoice.forSpecies(species) }
    // Welcher Modus gerade laeuft, entscheidet, WELCHE Zahlen ueberhaupt etwas bedeuten - im
    // Spiel seine (Stufe, Geld, Vorrat), sonst deine (Ziele, Woche). Siehe PlayTalk.Game.
    val playing = remember { PlayModePrefs.isActive(context) }
    var knowledge by remember { mutableStateOf<PlayTalk.Knowledge?>(null) }

    LaunchedEffect(species, playing) {
        knowledge = withContext(Dispatchers.IO) {
            PlayTalk.gather(
                context, AvatarSpeciesPrefs.profileId(species), includeGame = playing
            )
        }
    }

    val current = knowledge
    if (current == null) {
        Bubble(stringResource(R.string.talk_loading))
    } else {
        // Im Spiel zuerst der Spielstand - dort ist er die Auskunft, auf die es ankommt.
        current.game?.let { game ->
            Bubble(
                stringResource(R.string.talk_a_game_level, game.level, game.xp) + "\n" +
                    stringResource(R.string.talk_a_game_purse, game.coins, game.pantry) + "\n" +
                    stringResource(
                        when {
                            game.brokeAndHungry -> R.string.talk_a_game_broke
                            game.pantryEmpty -> R.string.talk_a_game_shopping
                            else -> R.string.talk_a_game_fine
                        }
                    )
            )
        }

        // Heute
        if (!current.hasPlan) {
            Bubble(stringResource(R.string.talk_a_today_noplan))
        } else if (current.fedToday == 0) {
            Bubble(stringResource(voice.emptyDay))
        } else {
            Bubble(
                stringResource(R.string.talk_a_today, current.fedToday) +
                    if (current.goalsTotal > 0) {
                        "\n" + stringResource(
                            R.string.talk_a_today_goals, current.goalsReached, current.goalsTotal
                        )
                    } else ""
            )
        }

        // Die Woche
        val week = current.week
        if (week.total > 0) {
            Bubble(
                stringResource(R.string.talk_a_week, week.total, week.activeDays, week.daysSoFar) +
                    if (week.activeDays >= week.daysSoFar) {
                        "\n" + stringResource(R.string.talk_a_week_every_day)
                    } else if (week.bestDay > 0) {
                        "\n" + stringResource(R.string.talk_a_week_best, week.bestDay)
                    } else ""
            )
        }

        // Worauf er heute achtet - der Punkt, an dem der Bericht in sein Verhalten uebergeht.
        if (current.steering.isNotEmpty()) {
            // Die Namen VORHER einsammeln: stringResource laesst sich nur in einer
            // Composable-Umgebung aufrufen, und `joinToString` reicht seine Funktion nicht
            // eingebettet weiter. `map` dagegen schon.
            val open = current.steering.map { stringResource(it.labelRes) }
            Bubble(
                stringResource(R.string.talk_a_steering) + "\n" +
                    open.joinToString("\n") { "· $it" } +
                    "\n" + stringResource(R.string.talk_a_steering_hint)
            )
        } else if (current.goalsTotal > 0) {
            Bubble(stringResource(voice.allDone))
        }
    }
    Choice(stringResource(R.string.action_back), onBack)
}

@Composable
private fun AssistantIntro(onBack: () -> Unit) {
    val context = LocalContext.current
    val mode = remember { AppMode.current(context) }

    // **Zuerst der Modus, in dem man GERADE ist.**
    //
    // Die Erklaerung fing frueher mit "ich tauche auf, wenn eine Erinnerung faellig ist" an -
    // ein Satz, der nur in einem der drei Modi stimmt. Wer im Spiel danach fragte, bekam etwas
    // erklaert, das er vor sich gar nicht sah, und wer im Uhr-Modus fragte, wartete anschliessend
    // vergeblich auf eine Erinnerung. Was erklaert wird, muss zu dem passen, was man vor sich
    // hat; alles andere kommt danach.
    Bubble(stringResource(currentModeExplanation(mode)))

    // Mehrere kurze Blasen statt eines Textblocks: so liest es sich als Erklaerung Schritt fuer
    // Schritt und nicht als Bedienungsanleitung.
    Bubble(stringResource(R.string.assistant_intro_1))
    Bubble(stringResource(R.string.assistant_intro_2))
    Bubble(stringResource(R.string.assistant_intro_3))
    Bubble(stringResource(R.string.assistant_intro_4))
    Bubble(stringResource(R.string.assistant_intro_6))
    Bubble(stringResource(R.string.assistant_intro_5))
    Choice(stringResource(R.string.action_back), onBack)
}

/**
 * Der Satz, mit dem er erklaert, wo man gerade ist.
 *
 * Fuer den Uhr-Modus bewusst der kuerzeste von allen: Dort gibt es nichts zu bedienen, und eine
 * ausfuehrliche Erklaerung fuer "hier ist eine Uhr" waere selbst schon eine Stoerung. Er sagt nur
 * das eine, was man tatsaechlich wissen muss - dass die Erinnerungen ruhen.
 */
private fun currentModeExplanation(mode: AppMode): Int = when (mode) {
    AppMode.WATCH -> R.string.assistant_here_watch
    AppMode.REMINDER -> R.string.assistant_here_reminder
    AppMode.PLAY -> R.string.assistant_here_play
}

/** Themen-Gruppierung der FAQ-Eintraege, siehe [AssistantFaq]. */
private enum class FaqCategory(val labelRes: Int) {
    /**
     * **Zuerst die Modi**, weil sie alles Weitere einordnen. Wer nicht weiss, dass es drei
     * Betriebsarten gibt, liest jede andere Antwort mit der falschen Erwartung - "ich tauche auf,
     * wenn eine Erinnerung faellig ist" stimmt eben nur in einem der drei.
     */
    MODES(R.string.faq_category_modes),
    REMINDERS(R.string.faq_category_reminders),
    ALARMS(R.string.faq_category_alarms),
    ANIMATIONS(R.string.faq_category_animations),
    AVATAR(R.string.faq_category_avatar),
    DISPLAY(R.string.faq_category_display),
    CAPTURE(R.string.faq_category_capture)
}

private data class FaqEntry(val category: FaqCategory, val questionRes: Int, val answerRes: Int)

/**
 * Alle FAQ-Eintraege in einer flachen Liste statt verschachtelt nach Kategorie - [AssistantFaq]
 * filtert selbst. Die "Overlaps & missed reminders"-Kategorie haelt bewusst fest, was in
 * [ReminderTrigger][com.notime.glyphsim.reminder.ReminderTrigger] tatsaechlich passiert (Reihenfolge
 * nach Intervall, Blockade durch offene Erinnerungen, stilles Verfallen nach dem Nachhol-Fenster) -
 * das ist keine triviale Ableitung aus der UI, sondern genau die Stelle, an der Nutzer sonst ratlos
 * vor einer scheinbar grundlos ausgebliebenen Erinnerung stehen.
 */
private val faqEntries = listOf(
    FaqEntry(FaqCategory.MODES, R.string.faq_q_modes, R.string.faq_a_modes),
    FaqEntry(FaqCategory.MODES, R.string.faq_q_mode_watch, R.string.faq_a_mode_watch),
    FaqEntry(FaqCategory.MODES, R.string.faq_q_mode_reminder, R.string.faq_a_mode_reminder),
    FaqEntry(FaqCategory.MODES, R.string.faq_q_mode_play, R.string.faq_a_mode_play),
    FaqEntry(FaqCategory.MODES, R.string.faq_q_mode_influence, R.string.faq_a_mode_influence),
    FaqEntry(FaqCategory.REMINDERS, R.string.faq_q_reminder_structure, R.string.faq_a_reminder_structure),
    FaqEntry(FaqCategory.REMINDERS, R.string.faq_q_interval, R.string.faq_a_interval),
    FaqEntry(FaqCategory.REMINDERS, R.string.faq_q_window, R.string.faq_a_window),
    FaqEntry(FaqCategory.REMINDERS, R.string.faq_q_open_duration_limit, R.string.faq_a_open_duration_limit),
    FaqEntry(FaqCategory.ALARMS, R.string.faq_q_overlap, R.string.faq_a_overlap),
    FaqEntry(FaqCategory.ALARMS, R.string.faq_q_open_duration, R.string.faq_a_open_duration),
    FaqEntry(FaqCategory.ALARMS, R.string.faq_q_missed, R.string.faq_a_missed),
    FaqEntry(FaqCategory.ALARMS, R.string.faq_q_repeat, R.string.faq_a_repeat),
    FaqEntry(FaqCategory.ALARMS, R.string.faq_q_screen_off, R.string.faq_a_screen_off),
    FaqEntry(FaqCategory.ANIMATIONS, R.string.faq_q_animation_length, R.string.faq_a_animation_length),
    FaqEntry(FaqCategory.ANIMATIONS, R.string.faq_q_animation_library, R.string.faq_a_animation_library),
    FaqEntry(FaqCategory.AVATAR, R.string.faq_q_feeding, R.string.faq_a_feeding),
    FaqEntry(FaqCategory.AVATAR, R.string.faq_q_mood, R.string.faq_a_mood),
    FaqEntry(FaqCategory.AVATAR, R.string.faq_q_species, R.string.faq_a_species),
    FaqEntry(FaqCategory.AVATAR, R.string.faq_q_talk, R.string.faq_a_talk),
    FaqEntry(FaqCategory.DISPLAY, R.string.faq_q_dock_mode, R.string.faq_a_dock_mode),
    FaqEntry(FaqCategory.CAPTURE, R.string.faq_q_capture, R.string.faq_a_capture),
    FaqEntry(FaqCategory.CAPTURE, R.string.faq_q_capture_where, R.string.faq_a_capture_where),
    FaqEntry(FaqCategory.CAPTURE, R.string.faq_q_capture_sound, R.string.faq_a_capture_sound)
)

/**
 * Kategorien zuerst, dann Fragen als Akkordeon: Antippen einer Frage klappt ihre [Bubble] direkt
 * darunter auf statt auf eine eigene Detail-Ebene zu wechseln - bei Frage-für-Frage-Navigation
 * verliert man beim Zurueckgehen den Ueberblick, welche man schon gelesen hat.
 */
@Composable
private fun AssistantFaq(onBack: () -> Unit) {
    var category by remember { mutableStateOf<FaqCategory?>(null) }
    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    val currentCategory = category
    if (currentCategory == null) {
        Bubble(stringResource(R.string.faq_intro))
        FaqCategory.entries.forEach { cat ->
            Choice(stringResource(cat.labelRes)) { category = cat }
        }
        Choice(stringResource(R.string.action_back), onBack)
    } else {
        val entries = faqEntries.filter { it.category == currentCategory }
        entries.forEachIndexed { index, entry ->
            Choice(stringResource(entry.questionRes)) {
                expandedIndex = if (expandedIndex == index) null else index
            }
            if (expandedIndex == index) {
                Bubble(stringResource(entry.answerRes))
            }
        }
        Choice(stringResource(R.string.action_back)) {
            category = null
            expandedIndex = null
        }
    }
}

/**
 * Gefuehrtes Einrichten: der Avatar fragt der Reihe nach ab, was gebraucht wird, und legt daraus
 * eine fertige Erinnerung samt Tagesziel an.
 *
 * Die Reihenfolge folgt bewusst der Denkweise des Nutzers - erst WAS, dann WIE OFT gewollt, dann
 * WANN, und zuletzt wie oft angestupst werden soll. Der Erinnerungs-Dialog fragt dieselben Dinge
 * ab, aber als Formular: dort muss man wissen, dass Intervall und Tagesziel zwei verschiedene
 * Dinge sind. Hier ergibt sich das aus der Frage selbst.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AssistantSetup(onDone: () -> Unit, onBack: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    var activity by remember { mutableStateOf<AnimationType?>(null) }
    var goal by remember { mutableStateOf(0) }
    var window by remember { mutableStateOf<TimeWindowOption?>(null) }
    var interval by remember { mutableStateOf(0) }
    var saved by remember { mutableStateOf(false) }
    // Zaehlt die in diesem Durchgang angelegten Erinnerungen - der Avatar spricht sie an, statt
    // nach jeder einzelnen so zu tun, als faenge man neu an.
    var createdCount by remember { mutableStateOf(0) }

    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<GlyphReminderViewModel>()
    // Sobald das Thema feststeht, richten sich alle folgenden Fragen danach (siehe
    // ReminderRhythm): sinnvolle Zahlen, ein markierter Vorschlag und ein Satz dazu, warum.
    val rhythm = activity?.let { ReminderRhythm.forType(it) }

    fun reset() {
        activity = null
        goal = 0
        window = null
        interval = 0
        saved = false
        step = 0
    }

    when {
        saved -> {
            Bubble(stringResource(R.string.assistant_setup_saved))
            Choice(stringResource(R.string.assistant_setup_another)) { reset() }
            Choice(stringResource(R.string.action_done), onDone)
        }

        step == 0 -> {
            Bubble(
                if (createdCount == 0) {
                    stringResource(R.string.assistant_setup_what)
                } else {
                    stringResource(R.string.assistant_setup_what_more)
                }
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnimationType.entries.forEach { type ->
                    FilterChip(
                        selected = activity == type,
                        onClick = {
                            activity = type
                            // Vorschlaege des Themas gleich uebernehmen - wer sie akzeptiert,
                            // tippt sich nur noch durch; wer will, aendert sie im naechsten Schritt.
                            val suggested = ReminderRhythm.forType(type)
                            goal = suggested.suggestedGoal
                            interval = suggested.suggestedInterval
                            window = suggested.suggestedWindow
                            step = 1
                        },
                        label = {
                            Text("${animationVisuals[type]?.emoji ?: ""} ${stringResource(type.labelRes)}")
                        }
                    )
                }
            }
            Choice(stringResource(R.string.action_back), onBack)
        }

        step == 1 && rhythm != null -> {
            Bubble(stringResource(R.string.assistant_setup_how_often_goal))
            // Der Satz zum Rhythmus macht aus einer Zahlenreihe eine Empfehlung: warum bei Sport
            // wenige Male genuegen und bei Trinken viele sinnvoll sind.
            Bubble(stringResource(rhythm.hintRes))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rhythm.goalOptions.forEach { times ->
                    SuggestionChip(
                        text = "$times×",
                        selected = goal == times,
                        recommended = times == rhythm.suggestedGoal,
                        onClick = { goal = times; step = 2 }
                    )
                }
            }
            Choice(stringResource(R.string.action_back), onClick = { step = 0 })
        }

        step == 2 -> {
            Bubble(stringResource(R.string.assistant_setup_when))
            TimeWindowOption.entries.forEach { option ->
                Choice(
                    if (option == rhythm?.suggestedWindow) {
                        stringResource(R.string.assistant_recommended, stringResource(option.labelRes))
                    } else {
                        stringResource(option.labelRes)
                    }
                ) { window = option; step = 3 }
            }
            Choice(stringResource(R.string.action_back), onClick = { step = 1 })
        }

        step == 3 && rhythm != null -> {
            // Die Rueckfrage, die den Unterschied zum Formular ausmacht: hier wird ausdruecklich
            // gesagt, dass haeufiges Anstupsen dem Ziel NICHT schadet - genau die Sorge, die
            // Nutzer sonst zu seltenen Intervallen greifen laesst.
            Bubble(stringResource(R.string.assistant_setup_nudge))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rhythm.intervalOptions.forEach { minutes ->
                    SuggestionChip(
                        text = "$minutes min",
                        selected = interval == minutes,
                        recommended = minutes == rhythm.suggestedInterval,
                        onClick = { interval = minutes; step = 4 }
                    )
                }
            }
            Choice(stringResource(R.string.action_back), onClick = { step = 2 })
        }

        else -> {
            val chosenActivity = activity
            val chosenWindow = window
            if (chosenActivity == null || chosenWindow == null) {
                Choice(stringResource(R.string.action_back), onClick = { step = 0 })
                return
            }
            // Der Anzeigename muss VOR dem Klick aufgeloest werden - stringResource braucht einen
            // Composable-Kontext, im Klick-Handler gibt es den nicht mehr.
            val activityLabel = stringResource(chosenActivity.labelRes)
            Bubble(
                stringResource(
                    R.string.assistant_setup_summary,
                    activityLabel,
                    goal,
                    stringResource(chosenWindow.labelRes),
                    interval
                )
            )
            Choice(stringResource(R.string.assistant_setup_confirm)) {
                viewModel.addReminder(
                    label = activityLabel,
                    animationChoice = AnimationChoice.BuiltIn(chosenActivity),
                    daysOfWeekMask = DaysOfWeekMask.toMask(DayOfWeek.entries.toSet()),
                    startMinuteOfDay = chosenWindow.startMinute,
                    endMinuteOfDay = chosenWindow.endMinute,
                    intervalMinutes = interval,
                    dailyGoal = goal
                )
                createdCount++
                saved = true
            }
            Choice(stringResource(R.string.action_back), onClick = { step = 3 })
        }
    }
}

/**
 * Auswahl-Chip, der den Vorschlag des Avatars sichtbar hervorhebt.
 *
 * Ohne Markierung waeren alle Zahlen gleich plausibel und die Frage bliebe genau die
 * Entscheidung, bei der Vorwissen fehlt - der Sinn des Gespraechs ist ja, sie abzunehmen.
 */
@Composable
private fun SuggestionChip(text: String, selected: Boolean, recommended: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(if (recommended) "$text ★" else text) }
    )
}
