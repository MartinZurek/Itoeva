package com.notime.glyphsim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.R
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.PlayPantry
import com.notime.glyphsim.matrix.PlayScene
import kotlin.random.Random
import kotlin.math.roundToInt

/**
 * **Das Gespraech mit dem Avatar** - er sagt etwas, man kann darauf eingehen (siehe [PlayTalk]).
 *
 * **Er faengt an, statt zu fragen.** Der erste Entwurf legte sechs Fragen nebeneinander; das sah
 * aus wie ein Menue und war auch eines - der Nutzer musste sich erst selbst ueberlegen, was er
 * wissen will, bevor irgendetwas gesagt wurde. Jetzt steht oben EINE Aussage, die zur Lage passt,
 * und darunter hoechstens zwei Angebote (siehe [PlayTalk.focus]). Wer jemanden anspricht,
 * erwartet, dass der andere anfaengt.
 *
 * **Kein Eingabefeld.** Ein Eingabefeld verspricht, dass alles gefragt werden darf, und bricht
 * dieses Versprechen bei der zweiten Frage. Ein Wesen, das wenig sagt und dabei nichts Falsches,
 * wirkt wie jemand mit einem klaren, kleinen Verstand - und das ist genau, was er ist. Nebenbei
 * ist es die einzige Fassung, die ohne Netzverbindung, ohne laufende Kosten und ohne dass Daten
 * das Geraet verlassen funktioniert.
 *
 * **Bewusst kein Material-Dialog.** Der Play-Modus ist eine schwarze Flaeche mit einer Pixelwelt;
 * eine helle Karte mit Schlagschatten darauf sieht aus, als haette sich das Betriebssystem
 * eingemischt. Stattdessen ein dunkles Feld in derselben warmweissen Schrift, in der auch die
 * Uhr leuchtet.
 *
 * **Der lange Bericht steht woanders.** Wer den vollstaendigen Ueberblick will - heute, Woche,
 * ganzer Plan -, bekommt ihn auf dem Startbildschirm ueber denselben Avatar (siehe
 * AvatarAssistant). Hier draussen in der Welt zaehlt, was gerade dran ist.
 */
private val INK = Color(0xFFF3F1EA)
private val INK_DIM = Color(0xFF8F8B82)
private val PANEL = Color(0xF00A0A0A)

@Composable
fun PlayTalkPanel(
    knowledge: PlayTalk.Knowledge?,
    species: AvatarSpecies,
    /** Was er gerade tut - siehe [PlayTalk.Doing]. */
    doing: PlayTalk.Doing? = null,
    /** Der Zustand der Welt, ueber den er von sich aus etwas erzaehlt - siehe [PlayTalk.Mood]. */
    mood: PlayTalk.Mood? = null,
    onAddReminder: (AnimationType) -> Unit,
    onOpenReminders: () -> Unit,
    /**
     * **Ihn um etwas BITTEN** - der Unterschied zwischen einer Auskunft und einem Gegenueber.
     *
     * Alles andere in diesem Feld ist Bericht: Er sagt, wie es steht. Hier darf man ihm etwas
     * sagen, und er tut es unmittelbar (siehe requestedTopic in DockScreen). Deshalb gibt es das
     * nur im Spielmodus - dort ist eine Welt da, in der er es ausfuehren KANN. Auf dem
     * Startbildschirm gaebe es nichts zu sehen, und die Bitte verpuffte.
     */
    onAsk: (AnimationType) -> Unit,
    /**
     * Eine bestehende Erinnerung so aendern, wie er es vorschlaegt (siehe [PlayTalk.Advice]).
     *
     * Bewusst dieselbe Bauart wie [onAddReminder]: Der Rat traegt die fertige Erinnerung schon bei
     * sich, das Feld hier fuehrt sie nur nach draussen. Ein Hinweis, der den Nutzer anschliessend
     * in eine Maske schickt, waere keine Hilfe, sondern eine Hausaufgabe.
     */
    onAdjust: (com.notime.glyphcore.data.GlyphReminder) -> Unit = {},
    /** Die Frage, die ER gerade stellt - siehe [PlayTalk.Ask]. */
    ask: PlayTalk.Ask? = null,
    /** Die Antwort darauf: das gewaehlte Thema bzw. die gewaehlte Tageszeit. */
    onAnswerFocus: (AnimationType) -> Unit = {},
    onAnswerTime: (com.notime.glyphsim.matrix.PlayAmbientActivity.DayPhase) -> Unit = {},
    onAnswerPurpose: (PlayTalk.Purpose) -> Unit = {},
    onAnswerWeekend: (Boolean) -> Unit = {},
    /** Die Frage wegwischen, ohne zu antworten - sie kommt dann nicht wieder. */
    onSkipAsk: () -> Unit = {},
    /** Alles vergessen, was der Nutzer ueber sich gesagt hat. */
    onForget: () -> Unit = {},
    /**
     * Das naechste Stueck seiner Geschichte erzaehlen - `null`, wenn er alles gesagt hat.
     *
     * Als Rueckruf und nicht als Text, weil das Erzaehlen etwas VERAENDERT: Was einmal gesagt
     * wurde, ist gesagt (siehe [PlayLore]).
     */
    onTell: (() -> Unit)? = null,
    /** Was er in diesem Gespraech schon erzaehlt hat - bleibt untereinander stehen. */
    told: List<Int> = emptyList(),
    /**
     * Ob er gerade zu hoeren ist, und der Schalter dazu (siehe [PlaySound]).
     *
     * **Warum das hier steht und nicht nur in den Einstellungen.** Ton stoert immer im selben
     * Moment: waehrend man zusieht. Wer dafuer den Dock-Modus verlassen, in die Einstellungen
     * gehen und wieder zurueckkommen muss, schaltet ihn beim ersten Mal ab und nie wieder an.
     * Hier ist er einen Fingertipp von der Welt entfernt - und "sei mal still" ist ohnehin etwas,
     * das man dem Wesen sagt und nicht einem Formular.
     */
    soundOn: Boolean = false,
    onToggleSound: (Boolean) -> Unit = {},
    /** Warum er gerade trotzdem still ist - siehe [PlaySound.silentReason]. */
    soundSilentReason: PlaySound.Reason? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Welche Frage gerade beantwortet wird - null heisst: nur die Fragen stehen da.
    var asked by remember { mutableStateOf<Question?>(null) }
    // Was gerade angelegt wurde, damit der Vorschlag danach nicht unveraendert dasteht.
    var justAdded by remember { mutableStateOf<AnimationType?>(null) }
    val voice = remember(species) { PlayVoice.forSpecies(species) }

    Column(
        modifier = modifier
            .widthIn(max = 340.dp)
            .background(PANEL, RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF2A2A28), RoundedCornerShape(14.dp))
            .padding(18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(stringResource(species.labelRes), color = INK_DIM, size = 12, weight = FontWeight.Medium)

        if (knowledge == null) {
            Text(stringResource(R.string.talk_loading), color = INK_DIM, size = 14)
        } else {
            val current = asked
            if (current == null) {
                // **Er faengt an.** Eine Aussage, die zur Lage passt, und hoechstens zwei
                // Angebote - siehe PlayTalk.focus fuer die Reihenfolge. Vorher standen hier sechs
                // gleich aussehende Fragen; das war ein Menue und verlangte vom Nutzer, sich
                // zuerst selbst zu ueberlegen, was er wissen will.
                // Die Verschiebung wird je Gespraech EINMAL gezogen: Waehrend man liest, soll
                // sich der Vorschlag nicht unter dem Finger aendern - beim naechsten Oeffnen
                // dagegen schon (siehe PlayTalk.focus).
                val focus = remember(knowledge) { PlayTalk.focus(knowledge, Random.nextInt(1_000)) }
                // **Und eine Bemerkung von ihm selbst.** Sie steht UNTER der Lage und ueber den
                // Angeboten: erst worum es geht, dann wie es ihm dabei geht, dann was man tun
                // kann. Wie die Angebote wird sie je Gespraech einmal gezogen - sonst wechselte
                // der Satz beim Lesen unter dem Finger.
                val remark = remember(knowledge, mood) {
                    mood?.let { PlayTalk.remarkFor(it, Random.nextInt(1_000)) }
                }
                Headline(focus.headline, knowledge, voice)
                if (remark != null && mood != null) RemarkLine(remark, mood)
                // **Seine eigene Frage steht UNTER seinen Angeboten**, nicht darueber. Wer das
                // Gespraech oeffnet, will zuerst wissen, wie es steht - erst danach ist Platz
                // fuer eine Frage zurueck. Umgekehrt waere es ein Formular mit Begruessung.
                if (ask != null) {
                    AskLine(ask, onAnswerFocus, onAnswerTime, onAnswerPurpose, onAnswerWeekend, onSkipAsk)
                }
                for (offer in focus.offers) {
                    OfferLine(
                        offer = offer,
                        justAdded = justAdded,
                        onAsk = onAsk,
                        onAdd = { topic -> justAdded = topic; onAddReminder(topic) },
                        onShow = { asked = it }
                    )
                }
                // Ganz unten und gedimmt: Es ist kein Angebot von ihm, sondern ein Handgriff des
                // Nutzers - dieselbe Stellung wie "lieber nicht" unter einer Frage.
                Text(
                    text = stringResource(
                        if (soundOn) R.string.talk_sound_off else R.string.talk_sound_on
                    ),
                    color = INK_DIM,
                    size = 12,
                    modifier = Modifier.clickable { onToggleSound(!soundOn) }
                )
                // Und warum er trotzdem still ist, falls etwas im Weg steht - sonst haelt genau
                // der, der den Ton haben wollte, den Schalter fuer kaputt.
                if (soundOn && soundSilentReason != null) {
                    Text(
                        text = stringResource(
                            when (soundSilentReason) {
                                PlaySound.Reason.NIGHT -> R.string.talk_sound_quiet_night
                                PlaySound.Reason.OTHER_AUDIO -> R.string.talk_sound_quiet_audio
                                PlaySound.Reason.DEVICE_SILENT -> R.string.talk_sound_quiet_device
                            }
                        ),
                        color = INK_DIM,
                        size = 11
                    )
                }
            } else {
                Answer(
                    question = current,
                    knowledge = knowledge,
                    doing = doing,
                    voice = voice,
                    justAdded = justAdded,
                    onAddReminder = { topic ->
                        justAdded = topic
                        onAddReminder(topic)
                    },
                    onOpenReminders = onOpenReminders,
                    onAsk = onAsk,
                    onAdjust = onAdjust,
                    onForget = onForget,
                    onTell = onTell,
                    told = told
                )
                QuestionLine(stringResource(R.string.talk_back)) { asked = null }
            }
        }

        // Mit demselben Winkel wie jede andere antippbare Zeile. Ohne ihn stand hier nur noch
        // "Bis gleich." - ein Satz, der sich wie Text liest und nicht wie ein Knopf. Die
        // arteigene Verabschiedung war ein Gewinn fuer den Charakter und ein Verlust fuer die
        // Bedienbarkeit; beides geht zusammen, sobald ein Zeichen sagt, dass man darauf tippen
        // kann. In diesem Feld gilt: Was mit "›" beginnt, laesst sich antippen - ausnahmslos.
        QuestionLine(stringResource(voice.farewell)) { onDismiss() }
    }
}

/**
 * Die zwei Auskuenfte, die man sich noch ansehen kann, wenn man ueber die Aussage hinaus etwas
 * wissen will. Alles Uebrige sagt er von selbst (siehe [PlayTalk.focus]).
 */
private enum class Question(val labelRes: Int) {
    WEEK(R.string.talk_q_week),
    PLAN(R.string.talk_q_plan),
    /**
     * **Was hier eigentlich passiert.** Der Spielmodus erklaert sich nicht von selbst: Ein Wesen,
     * das umherlaeuft, arbeitet und einkauft, sieht huebsch aus, aber ohne einen Satz dazu bleibt
     * offen, was das mit einem selbst zu tun hat. Erklaert wird deshalb GENAU dieser Modus - im
     * Alltag-Modus gaebe es hier gar kein Wesen zum Antippen, und im Uhr-Modus soll es keines
     * geben.
     */
    HERE(R.string.talk_q_here),
    ADVICE(R.string.talk_q_advice),
    PROFILE(R.string.talk_q_profile),
    STORY(R.string.talk_q_story),
    /** Stufe, Erfahrung, Muenzen, Vorrat. */
    GAME(R.string.talk_q_game)
}

/**
 * Was er von sich aus sagt - eine einzige Zeile, dazu bei Bedarf eine leise Ergaenzung.
 *
 * Die Ergaenzung steht bewusst gedimmt darunter statt als zweiter Satz daneben: Sie ist der
 * Grund, nicht die Nachricht. "Vorrat leer" ist die Nachricht; "dann muss ich erst arbeiten"
 * erklaert sie.
 */
@Composable
private fun Headline(
    headline: PlayTalk.Headline,
    knowledge: PlayTalk.Knowledge,
    voice: PlayVoice
) {
    when (headline) {
        PlayTalk.Headline.BROKE -> {
            Text(stringResource(R.string.talk_a_game_broke), color = INK, size = 15)
            knowledge.game?.let {
                Text(
                    stringResource(R.string.talk_a_game_purse, it.coins, it.pantry),
                    color = INK_DIM, size = 13
                )
            }
        }
        PlayTalk.Headline.SHOPPING -> {
            Text(stringResource(R.string.talk_a_game_shopping), color = INK, size = 15)
            knowledge.game?.let {
                Text(
                    stringResource(R.string.talk_a_game_purse, it.coins, it.pantry),
                    color = INK_DIM, size = 13
                )
            }
        }
        PlayTalk.Headline.OPEN_TOPICS -> {
            val open = knowledge.steering.map { stringResource(it.labelRes) }
            Text(
                stringResource(R.string.talk_a_steering) + " " + open.joinToString(", "),
                color = INK, size = 15
            )
            Text(stringResource(R.string.talk_a_steering_hint), color = INK_DIM, size = 13)
        }
        PlayTalk.Headline.NOTHING_TODAY ->
            Text(stringResource(voice.emptyDay), color = INK, size = 15)
        PlayTalk.Headline.ALL_DONE ->
            Text(stringResource(voice.allDone), color = INK, size = 15)
        PlayTalk.Headline.NO_PLAN ->
            Text(stringResource(R.string.talk_a_plan_empty), color = INK, size = 15)
        PlayTalk.Headline.SMALL_TALK -> {
            Text(stringResource(voice.greeting), color = INK, size = 15)
            if (knowledge.fedToday > 0) {
                Text(
                    stringResource(R.string.talk_a_today, knowledge.fedToday),
                    color = INK_DIM, size = 13
                )
            }
        }
    }
}

/** Ein Angebot als antippbare Zeile - Bitte, Anlegen oder Nachschlagen. */
@Composable
private fun OfferLine(
    offer: PlayTalk.Offer,
    justAdded: AnimationType?,
    onAsk: (AnimationType) -> Unit,
    onAdd: (AnimationType) -> Unit,
    onShow: (Question) -> Unit
) {
    when (offer) {
        is PlayTalk.Offer.Ask -> {
            val label = when (offer.topic) {
                AnimationType.WORK -> stringResource(R.string.talk_ask_work)
                AnimationType.DRINK -> stringResource(R.string.talk_ask_shop)
                else -> stringResource(
                    R.string.talk_ask_now, stringResource(offer.topic.labelRes)
                )
            }
            QuestionLine(label) { onAsk(offer.topic) }
        }
        is PlayTalk.Offer.Add -> {
            // **In ZWEI Schritten, nicht in einem.**
            //
            // Beim Umbau auf die kurze Fassung war das kurzzeitig ein einziger Griff: Antippen
            // legte sofort eine Erinnerung an. Das ist genau die Art stiller Nebenwirkung, die
            // man einer App nie verzeiht - die Zeile las sich wie eine Frage ("Wie waere es mit
            // Bewegung?") und richtete in Wahrheit etwas ein, das anschliessend jeden Tag
            // anstupst, ohne dass man je gesehen haette, wann und wie oft.
            //
            // Jetzt klappt sie erst auf und zeigt, was entstehen wuerde; angelegt wird es erst
            // durch eine zweite, eindeutige Zeile.
            var expanded by remember(offer.topic) { mutableStateOf(false) }
            when {
                justAdded == offer.topic -> {
                    Text(
                        stringResource(
                            R.string.talk_a_suggest_added, stringResource(offer.topic.labelRes)
                        ),
                        color = INK_DIM, size = 13
                    )
                    Text(
                        stringResource(R.string.talk_a_suggest_added_hint),
                        color = INK_DIM, size = 13
                    )
                }
                expanded -> {
                    Text(
                        stringResource(
                            R.string.talk_a_suggest, stringResource(offer.topic.labelRes)
                        ),
                        color = INK, size = 15
                    )
                    val preset = PlayTalk.presetFor(offer.topic)
                    Text(
                        stringResource(
                            R.string.talk_a_suggest_detail,
                            formatMinuteOfDay(preset.startMinuteOfDay),
                            formatMinuteOfDay(preset.endMinuteOfDay),
                            preset.dailyGoal
                        ),
                        color = INK_DIM, size = 13
                    )
                    QuestionLine(stringResource(R.string.talk_a_suggest_accept)) {
                        onAdd(offer.topic)
                    }
                }
                else -> QuestionLine(
                    stringResource(R.string.talk_a_suggest, stringResource(offer.topic.labelRes))
                ) { expanded = true }
            }
        }
        PlayTalk.Offer.ShowPlan ->
            QuestionLine(stringResource(R.string.talk_q_plan)) { onShow(Question.PLAN) }
        PlayTalk.Offer.ShowWeek ->
            QuestionLine(stringResource(R.string.talk_q_week)) { onShow(Question.WEEK) }
        PlayTalk.Offer.Explain ->
            QuestionLine(stringResource(R.string.talk_q_here)) { onShow(Question.HERE) }
        PlayTalk.Offer.ShowGame ->
            QuestionLine(stringResource(R.string.talk_q_game)) { onShow(Question.GAME) }
        PlayTalk.Offer.ShowAdvice ->
            QuestionLine(stringResource(R.string.talk_q_advice)) { onShow(Question.ADVICE) }
        PlayTalk.Offer.ShowProfile ->
            QuestionLine(stringResource(R.string.talk_q_profile)) { onShow(Question.PROFILE) }
        PlayTalk.Offer.Tell ->
            QuestionLine(stringResource(R.string.talk_q_story)) { onShow(Question.STORY) }
    }
}

/**
 * Was er ungefragt von sich erzaehlt (siehe [PlayTalk.Remark]).
 *
 * Gedimmt wie die Ergaenzungen unter der Lage: Es ist kein Hinweis und keine Aufforderung,
 * sondern das, was er nebenbei sagt. Wer es ueberliest, verpasst nichts - und genau deshalb
 * darf es da stehen.
 */
@Composable
private fun RemarkLine(remark: PlayTalk.Remark, mood: PlayTalk.Mood) {
    val text = when (remark) {
        PlayTalk.Remark.RAIN -> stringResource(R.string.remark_rain)
        PlayTalk.Remark.SNOW -> stringResource(R.string.remark_snow)
        PlayTalk.Remark.BRIGHT_MORNING -> stringResource(R.string.remark_bright_morning)
        PlayTalk.Remark.EVENING -> stringResource(R.string.remark_evening)
        PlayTalk.Remark.NIGHT -> stringResource(R.string.remark_night)
        PlayTalk.Remark.DOING -> mood.doing?.let {
            stringResource(R.string.talk_a_doing, stringResource(placeTextFor(it.place)))
        }
        PlayTalk.Remark.VISITOR -> mood.lastVisitor?.let {
            stringResource(R.string.remark_visitor, stringResource(it.labelRes))
        }
        PlayTalk.Remark.EARNED -> mood.game?.let {
            stringResource(R.string.remark_earned, it.coins)
        }
        PlayTalk.Remark.STOCKED -> stringResource(R.string.remark_stocked)
        PlayTalk.Remark.FOCUS -> mood.focusTopic?.let {
            stringResource(R.string.talk_focus_noted, stringResource(it.labelRes))
        }
        // Das Kapitel spricht mit seiner eigenen Stimme - dafuer gibt es die Zeile schon.
        PlayTalk.Remark.TOGETHER -> stringResource(mood.chapter.lineRes)
        PlayTalk.Remark.QUIET -> stringResource(R.string.remark_quiet)
    } ?: return
    Text(text, color = INK_DIM, size = 13)
}

/**
 * **Seine Frage an dich** - mit Antworten zum Antippen.
 *
 * Auswahl statt Eingabefeld: Eine Tastatur mitten in einem Gespraech mit einem Wesen, das sechzehn
 * Zellen hoch ist, waere ein Bruch. Und eine Auswahl kann er anschliessend auch VERSTEHEN - freier
 * Text waere eine Antwort, mit der er nichts anfangen kann, und damit wieder eine Umfrage.
 */
@Composable
private fun AskLine(
    ask: PlayTalk.Ask,
    onAnswerFocus: (AnimationType) -> Unit,
    onAnswerTime: (com.notime.glyphsim.matrix.PlayAmbientActivity.DayPhase) -> Unit,
    onAnswerPurpose: (PlayTalk.Purpose) -> Unit,
    onAnswerWeekend: (Boolean) -> Unit,
    onSkip: () -> Unit
) {
    when (ask) {
        PlayTalk.Ask.FOCUS -> {
            Text(stringResource(R.string.talk_ask_focus), color = INK, size = 15)
            // Nur Themen, die er auch sichtbar tut - sonst waere die Antwort folgenlos.
            for (topic in PlayTalk.SUGGESTABLE) {
                QuestionLine(stringResource(topic.labelRes)) { onAnswerFocus(topic) }
            }
        }
        PlayTalk.Ask.TIME -> {
            Text(stringResource(R.string.talk_ask_time), color = INK, size = 15)
            QuestionLine(stringResource(R.string.talk_ask_time_morning)) {
                onAnswerTime(com.notime.glyphsim.matrix.PlayAmbientActivity.DayPhase.MORNING)
            }
            QuestionLine(stringResource(R.string.talk_ask_time_midday)) {
                onAnswerTime(com.notime.glyphsim.matrix.PlayAmbientActivity.DayPhase.MIDDAY)
            }
            QuestionLine(stringResource(R.string.talk_ask_time_evening)) {
                onAnswerTime(com.notime.glyphsim.matrix.PlayAmbientActivity.DayPhase.EVENING)
            }
        }
        PlayTalk.Ask.PURPOSE -> {
            Text(stringResource(R.string.talk_ask_purpose), color = INK, size = 15)
            QuestionLine(stringResource(R.string.talk_ask_purpose_health)) {
                onAnswerPurpose(PlayTalk.Purpose.HEALTH)
            }
            QuestionLine(stringResource(R.string.talk_ask_purpose_calm)) {
                onAnswerPurpose(PlayTalk.Purpose.CALM)
            }
            QuestionLine(stringResource(R.string.talk_ask_purpose_structure)) {
                onAnswerPurpose(PlayTalk.Purpose.STRUCTURE)
            }
        }
        PlayTalk.Ask.WEEKEND -> {
            Text(stringResource(R.string.talk_ask_weekend), color = INK, size = 15)
            QuestionLine(stringResource(R.string.talk_ask_weekend_yes)) { onAnswerWeekend(true) }
            QuestionLine(stringResource(R.string.talk_ask_weekend_no)) { onAnswerWeekend(false) }
        }
    }
    Text(
        stringResource(R.string.talk_ask_skip),
        color = INK_DIM,
        size = 12,
        modifier = Modifier.clickable { onSkip() }
    )
}

/**
 * Ein Befund zu einer bestehenden Erinnerung, darunter die fertige Aenderung zum Annehmen.
 *
 * Der Befund steht in normaler Helligkeit, die Begruendung darunter gedimmt - dieselbe Staffelung
 * wie bei der Lage oben: erst was ist, dann warum er es sagt.
 */
@Composable
private fun AdviceLine(
    advice: PlayTalk.Advice,
    onAdjust: (com.notime.glyphcore.data.GlyphReminder) -> Unit
) {
    var taken by remember(advice) { mutableStateOf(false) }
    val label = advice.reminder.label
    when (advice) {
        is PlayTalk.Advice.LowerGoal -> {
            Text(
                stringResource(R.string.talk_advice_lower, label, advice.reminder.dailyGoal, advice.typical),
                color = INK, size = 15
            )
            Text(stringResource(R.string.talk_advice_lower_why), color = INK_DIM, size = 13)
        }
        is PlayTalk.Advice.RaiseGoal -> {
            Text(
                stringResource(R.string.talk_advice_raise, label, advice.typical),
                color = INK, size = 15
            )
            Text(stringResource(R.string.talk_advice_raise_why), color = INK_DIM, size = 13)
        }
        is PlayTalk.Advice.ShiftWindow -> {
            Text(
                stringResource(R.string.talk_advice_window, label, advice.fromHour, advice.toHour + 1),
                color = INK, size = 15
            )
            Text(stringResource(R.string.talk_advice_window_why), color = INK_DIM, size = 13)
        }
        is PlayTalk.Advice.Ignored -> {
            Text(
                stringResource(R.string.talk_advice_ignored, label, advice.triggered),
                color = INK, size = 15
            )
            Text(stringResource(R.string.talk_advice_ignored_why), color = INK_DIM, size = 13)
        }
    }
    val change = advice.changed
    if (change != null) {
        if (taken) {
            Text(stringResource(R.string.talk_advice_taken), color = INK_DIM, size = 13)
        } else {
            QuestionLine(stringResource(R.string.talk_advice_accept)) {
                taken = true
                onAdjust(change)
            }
        }
    }
}

/** Wie er die genannte Tageszeit nennt. */
private fun timeTextFor(phase: com.notime.glyphsim.matrix.PlayAmbientActivity.DayPhase): Int =
    when (phase) {
        com.notime.glyphsim.matrix.PlayAmbientActivity.DayPhase.MORNING -> R.string.talk_ask_time_morning
        com.notime.glyphsim.matrix.PlayAmbientActivity.DayPhase.MIDDAY -> R.string.talk_ask_time_midday
        else -> R.string.talk_ask_time_evening
    }

/** Und wie den genannten Zweck. */
private fun purposeTextFor(purpose: PlayTalk.Purpose): Int = when (purpose) {
    PlayTalk.Purpose.HEALTH -> R.string.talk_ask_purpose_health
    PlayTalk.Purpose.CALM -> R.string.talk_ask_purpose_calm
    PlayTalk.Purpose.STRUCTURE -> R.string.talk_ask_purpose_structure
}

/** Wie er den Ort nennt, an dem er gerade ist. */
private fun placeTextFor(place: PlayScene.Place): Int = when (place) {
    PlayScene.Place.BEDROOM -> R.string.talk_place_bedroom
    PlayScene.Place.BATH -> R.string.talk_place_bath
    PlayScene.Place.DESK -> R.string.talk_place_desk
    PlayScene.Place.WORK -> R.string.talk_place_work
    PlayScene.Place.KITCHEN -> R.string.talk_place_kitchen
    PlayScene.Place.NOOK -> R.string.talk_place_nook
    PlayScene.Place.LIVING -> R.string.talk_place_living
    PlayScene.Place.CRAFT -> R.string.talk_place_craft
    PlayScene.Place.PARK -> R.string.talk_place_park
    PlayScene.Place.SHOP -> R.string.talk_place_shop
    PlayScene.Place.STREET -> R.string.talk_place_street
    PlayScene.Place.FOREST -> R.string.talk_place_forest
}

@Composable
private fun Answer(
    question: Question,
    knowledge: PlayTalk.Knowledge,
    doing: PlayTalk.Doing?,
    voice: PlayVoice,
    justAdded: AnimationType?,
    onAddReminder: (AnimationType) -> Unit,
    onOpenReminders: () -> Unit,
    onAsk: (AnimationType) -> Unit,
    onAdjust: (com.notime.glyphcore.data.GlyphReminder) -> Unit,
    onForget: () -> Unit,
    onTell: (() -> Unit)?,
    told: List<Int>
) {
    when (question) {
        Question.HERE -> {
            // **Zuerst das Naechstliegende: was er GERADE tut.** Danach erst die Erklaerung des
            // Modus - die ist beim zweiten Lesen bekannt, der erste Satz nie.
            doing?.let { now ->
                Text(
                    stringResource(R.string.talk_a_doing, stringResource(placeTextFor(now.place))),
                    color = INK,
                    size = 15
                )
                // Und WARUM: Wenn das, was er gerade tut, zu einer heute noch offenen Gewohnheit
                // gehoert, ist genau das die Ueberleitung von seinem Tag zu deinem.
                now.topic?.takeIf { it in knowledge.steering }?.let { topic ->
                    Text(
                        stringResource(R.string.talk_a_doing_why, stringResource(topic.labelRes)),
                        color = INK_DIM,
                        size = 13
                    )
                }
            }
            Text(stringResource(R.string.talk_a_here), color = INK, size = 15)
        }

        Question.STORY -> {
            // **Ein Stueck je Tippen, und es bleibt stehen.** Wer weiterhoeren will, tippt noch
            // einmal - dann steht das naechste darunter, und man liest die Geschichte als Folge
            // statt als Einzelsaetze, die einander ueberschreiben.
            for (piece in told) {
                Text(stringResource(piece), color = INK, size = 15)
            }
            when {
                onTell != null -> QuestionLine(
                    stringResource(
                        if (told.isEmpty()) R.string.talk_q_story else R.string.talk_q_story_more
                    )
                ) { onTell() }
                told.isEmpty() -> Text(
                    stringResource(R.string.talk_a_story_end), color = INK, size = 15
                )
                else -> Text(stringResource(R.string.talk_a_story_end), color = INK_DIM, size = 13)
            }
        }

        Question.PROFILE -> {
            val answers = knowledge.answers
            if (answers == null || !answers.anyGiven) {
                Text(stringResource(R.string.talk_a_profile_none), color = INK, size = 15)
            } else {
                Text(stringResource(R.string.talk_a_profile_intro), color = INK, size = 15)
                answers.focusTopic?.let {
                    Text(
                        stringResource(R.string.talk_a_profile_focus, stringResource(it.labelRes)),
                        color = INK_DIM, size = 13
                    )
                }
                answers.busyPhase?.let {
                    Text(
                        stringResource(R.string.talk_a_profile_time, stringResource(timeTextFor(it))),
                        color = INK_DIM, size = 13
                    )
                }
                answers.purpose?.let {
                    Text(
                        stringResource(R.string.talk_a_profile_purpose, stringResource(purposeTextFor(it))),
                        color = INK_DIM, size = 13
                    )
                }
                if (!answers.includesWeekend) {
                    Text(stringResource(R.string.talk_a_profile_weekdays), color = INK_DIM, size = 13)
                }
                // **Und der Weg zurueck.** Wer einmal geantwortet hat, soll sich nicht fuer immer
                // festgelegt haben - sonst waere die Frage eine Falle gewesen.
                QuestionLine(stringResource(R.string.talk_a_profile_forget)) { onForget() }
            }
        }

        Question.ADVICE -> {
            if (knowledge.advice.isEmpty()) {
                Text(stringResource(R.string.talk_a_advice_none), color = INK, size = 15)
            } else {
                Text(stringResource(R.string.talk_a_advice_intro), color = INK, size = 15)
                for (advice in knowledge.advice) {
                    AdviceLine(advice, onAdjust)
                }
            }
            QuestionLine(stringResource(R.string.talk_open_reminders)) { onOpenReminders() }
        }

        Question.GAME -> {
            val game = knowledge.game
            if (game == null) {
                Text(stringResource(R.string.talk_a_game_off), color = INK, size = 15)
            } else {
                Text(
                    stringResource(R.string.talk_a_game_level_only, game.level),
                    color = INK, size = 17, weight = FontWeight.SemiBold
                )
                // **Ein Balken statt einer Zahl.** "180 Erfahrung" sagt niemandem etwas; ein
                // Balken, der sich sichtbar fuellt, beantwortet die eigentliche Frage - wie weit
                // ist es noch? Aus denselben Zeichen gebaut, aus denen die Welt besteht.
                Meter(game.progress)
                Text(
                    stringResource(
                        R.string.talk_a_game_next, game.xpInLevel, game.xpPerLevel, game.level + 1
                    ),
                    color = INK_DIM, size = 13
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column {
                        Text(stringResource(R.string.talk_game_coins), color = INK_DIM, size = 12)
                        Text("${game.coins}", color = INK, size = 15)
                    }
                    Column {
                        Text(stringResource(R.string.talk_game_pantry), color = INK_DIM, size = 12)
                        // Als Punkte statt als Zahl: Der Vorrat reicht von null bis drei, und
                        // drei Punkte liest man schneller als eine Ziffer.
                        Text(
                            (0 until PlayPantry.FULL).joinToString(" ") {
                                if (it < game.pantry) "●" else "○"
                            },
                            color = INK, size = 15
                        )
                    }
                }
                Text(
                    stringResource(
                        when {
                            game.brokeAndHungry -> R.string.talk_a_game_broke
                            game.pantryEmpty -> R.string.talk_a_game_shopping
                            else -> R.string.talk_a_game_fine
                        }
                    ),
                    color = INK_DIM, size = 13
                )
            }
        }

        Question.WEEK -> {
            val week = knowledge.week
            if (week.total == 0) {
                Text(stringResource(R.string.talk_a_week_empty), color = INK, size = 15)
            } else {
                Text(
                    stringResource(
                        R.string.talk_a_week, week.total, week.activeDays, week.daysSoFar
                    ),
                    color = INK, size = 15
                )
                if (week.activeDays >= week.daysSoFar) {
                    // Das Lob nur, wenn es auch stimmt - und dann ohne Zahl davor, damit es sich
                    // wie eine Bemerkung liest und nicht wie eine Auswertung.
                    Text(stringResource(R.string.talk_a_week_every_day), color = INK_DIM, size = 13)
                } else if (week.bestDay > 0) {
                    Text(
                        stringResource(R.string.talk_a_week_best, week.bestDay),
                        color = INK_DIM, size = 13
                    )
                }
            }
            // **Danach erst das Laengere.** Die Woche ist die Antwort auf "wie lief es"; Serie,
            // Vorwoche und Monat sind die Antwort auf "und sonst so". Wer nur das eine wissen
            // will, soll das andere nicht mitlesen muessen - deshalb darunter und gedimmt.
            val history = knowledge.history
            if (history.streakDays > 1) {
                Text(
                    stringResource(R.string.talk_a_streak, history.streakDays),
                    color = INK, size = 15
                )
            }
            history.betterThanLastWeek(week.total)?.let { better ->
                Text(
                    if (better) {
                        stringResource(R.string.talk_a_week_better, history.previousWeekTotal)
                    } else {
                        stringResource(R.string.talk_a_week_prev, history.previousWeekTotal)
                    },
                    color = INK_DIM, size = 13
                )
            }
            if (history.monthActiveDays > 0) {
                Text(
                    stringResource(
                        R.string.talk_a_month, history.monthTotal, history.monthActiveDays,
                        PlayTalk.HISTORY_DAYS
                    ),
                    color = INK_DIM, size = 13
                )
            }
        }

        Question.PLAN -> {
            if (!knowledge.hasPlan) {
                Text(stringResource(R.string.talk_a_plan_empty), color = INK, size = 15)
                QuestionLine(stringResource(R.string.talk_open_reminders)) { onOpenReminders() }
            } else {
                Text(stringResource(R.string.talk_a_plan), color = INK, size = 15)
                for (entry in knowledge.plan) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            entry.reminder.label,
                            color = if (entry.reached) INK_DIM else INK,
                            size = 14,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            when {
                                // Erledigtes wird abgehakt statt weitergezaehlt: Wer sein Ziel
                                // erreicht hat, soll nicht lesen "7 von 3".
                                entry.reached -> stringResource(R.string.talk_plan_done)
                                entry.hasGoal -> "${entry.doneToday}/${entry.goal}"
                                else -> stringResource(R.string.talk_plan_nogoal)
                            },
                            color = INK_DIM, size = 13
                        )
                    }
                }
                QuestionLine(stringResource(R.string.talk_open_reminders)) { onOpenReminders() }
            }
        }

    }
}

/**
 * Ein Fortschrittsbalken aus Bloecken - dieselbe Sprache wie die Welt daneben.
 *
 * Bewusst gestufte Bloecke und keine glatte Linie: Diese Welt besteht aus Zellen, und ein weich
 * verlaufender Balken waere das einzige Element darin, das das nicht taete. Zehn Stufen genuegen -
 * feiner abgelesen wird ohnehin nicht.
 */
@Composable
private fun Meter(progress: Float) {
    val filled = (progress.coerceIn(0f, 1f) * METER_STEPS).roundToInt()
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(METER_STEPS) { index ->
            Box(
                modifier = Modifier
                    .size(width = 14.dp, height = 6.dp)
                    .background(
                        if (index < filled) INK else Color(0xFF2A2A28),
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

private const val METER_STEPS = 10

/** Eigene Fassung statt der aus ReminderScreen - die ist dort dateiprivat, und eine Zeitangabe
 *  als zwei zweistellige Zahlen ist kein Wissen, das geteilt werden muesste. */
private fun formatMinuteOfDay(minuteOfDay: Int): String =
    "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)

/** Eine antippbare Zeile - Fragen und Aktionen sehen bewusst gleich aus, beides ist ein Angebot. */
@Composable
private fun QuestionLine(label: String, onClick: () -> Unit) {
    Text(
        "› $label",
        color = INK,
        size = 15,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp)
    )
}

/**
 * Schrift in EINER Form fuer das ganze Feld - ohne MaterialTheme, das im Play-Modus gar nicht
 * gesetzt ist und dessen Farben hier auch nicht passen wuerden.
 *
 * **Warum dicktengleich und nicht wirklich Pixel.** Naheliegend waere der vorhandene Zeichensatz
 * dieser Welt gewesen ([com.notime.glyphsim.matrix.PixelFont]) - der kennt aber nur Ziffern und
 * ist fuer ein 13x13-Feld gebaut. Ein vollstaendiger Pixel-Zeichensatz mit Umlauten und
 * Satzzeichen waere eine Schriftdatei, also ein Gestaltungsprojekt fuer sich; und in Fliesstext
 * gesetzt liest sich echte Pixelschrift bei diesen Groessen deutlich muehsamer, gerade fuer Augen,
 * die es ohnehin schwer haben.
 *
 * Dicktengleiche Schrift mit etwas Laufweite trifft denselben Ton - technisch, ruhig, zur
 * Pixelwelt gehoerig - und bleibt vollstaendig lesbar. Das ist hier kein Notbehelf, sondern der
 * bessere Tausch: Der Charakter dieses Feldes soll aus dem kommen, was darin steht.
 */
@Composable
private fun Text(
    text: String,
    color: Color,
    size: Int,
    weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Text(
        text = text,
        color = color,
        fontSize = size.sp,
        lineHeight = (size * 1.5f).sp,
        fontWeight = weight,
        fontFamily = FontFamily.Monospace,
        // Etwas Luft zwischen den Zeichen: Genau das laesst dicktengleiche Schrift nach Anzeige
        // aussehen statt nach Quelltext.
        letterSpacing = 0.06.em,
        modifier = modifier
    )
}
