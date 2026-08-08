package com.notime.glyphsim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
                val focus = remember(knowledge) { PlayTalk.focus(knowledge) }
                Headline(focus.headline, knowledge, voice)
                for (offer in focus.offers) {
                    OfferLine(
                        offer = offer,
                        justAdded = justAdded,
                        onAsk = onAsk,
                        onAdd = { topic -> justAdded = topic; onAddReminder(topic) },
                        onShow = { asked = it }
                    )
                }
            } else {
                Answer(
                    question = current,
                    knowledge = knowledge,
                    voice = voice,
                    justAdded = justAdded,
                    onAddReminder = { topic ->
                        justAdded = topic
                        onAddReminder(topic)
                    },
                    onOpenReminders = onOpenReminders,
                    onAsk = onAsk
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
    PLAN(R.string.talk_q_plan)
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
            if (justAdded == offer.topic) {
                Text(
                    stringResource(
                        R.string.talk_a_suggest_added, stringResource(offer.topic.labelRes)
                    ),
                    color = INK_DIM, size = 13
                )
            } else {
                QuestionLine(
                    stringResource(R.string.talk_a_suggest, stringResource(offer.topic.labelRes))
                ) { onAdd(offer.topic) }
            }
        }
        PlayTalk.Offer.ShowPlan ->
            QuestionLine(stringResource(R.string.talk_q_plan)) { onShow(Question.PLAN) }
        PlayTalk.Offer.ShowWeek ->
            QuestionLine(stringResource(R.string.talk_q_week)) { onShow(Question.WEEK) }
    }
}

@Composable
private fun Answer(
    question: Question,
    knowledge: PlayTalk.Knowledge,
    voice: PlayVoice,
    justAdded: AnimationType?,
    onAddReminder: (AnimationType) -> Unit,
    onOpenReminders: () -> Unit,
    onAsk: (AnimationType) -> Unit
) {
    when (question) {
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
