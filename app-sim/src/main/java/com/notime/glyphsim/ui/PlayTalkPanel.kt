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
 * **Das Gespraech mit dem Avatar** - vorgegebene Fragen, echte Auskuenfte (siehe [PlayTalk]).
 *
 * **Warum feste Fragen und kein Eingabefeld.** Ein Eingabefeld verspricht, dass alles gefragt
 * werden darf, und bricht dieses Versprechen bei der zweiten Frage. Vier Fragen, die er
 * VOLLSTAENDIG beantworten kann, wirken dagegen wie ein Wesen mit einem klaren, kleinen
 * Verstand - und das ist genau, was er ist. Nebenbei ist es die einzige Fassung, die ohne
 * Netzverbindung, ohne laufende Kosten und ohne dass Daten das Geraet verlassen funktioniert.
 *
 * **Bewusst kein Material-Dialog.** Der Play-Modus ist eine schwarze Flaeche mit einer Pixelwelt;
 * eine helle Karte mit Schlagschatten darauf sieht aus, als haette sich das Betriebssystem
 * eingemischt. Stattdessen ein dunkles Feld in derselben warmweissen Schrift, in der auch die
 * Uhr leuchtet.
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
                // Die Begruessung steht nur ueber der Fragenliste, nicht ueber jeder Antwort:
                // Ein Wesen, das sich nach jeder Auskunft erneut begruesst, klingt wie ein
                // Automat, der neu gestartet wurde.
                Text(stringResource(voice.greeting), color = INK, size = 15)
                for (question in Question.entries) {
                    // Vorschlaege nur anbieten, wenn es tatsaechlich etwas vorzuschlagen gibt -
                    // eine Frage, auf die "nichts" die Antwort ist, ist eine leere Zeile.
                    if (question == Question.SUGGEST &&
                        PlayTalk.nextSuggestion(knowledge) == null
                    ) continue
                    // Nach dem Spielstand laesst sich nur fragen, wenn es ein Spiel gibt.
                    if (question == Question.GAME && knowledge.game == null) continue
                    QuestionLine(stringResource(question.labelRes)) { asked = question }
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
 * Die Fragen. Vier, und in dieser Reihenfolge: erst was war, dann was geplant ist, dann was ihn
 * heute umtreibt, dann - nur falls es etwas gibt - sein eigener Vorschlag.
 */
private enum class Question(val labelRes: Int) {
    /**
     * **Nur im Spielmodus** - und die erste Frage, weil sie dort die dringendste ist. Wer zusieht,
     * will als erstes wissen, warum er gerade tut, was er tut; Vorrat und Geld beantworten das.
     */
    GAME(R.string.talk_q_game),
    TODAY(R.string.talk_q_today),
    WEEK(R.string.talk_q_week),
    PLAN(R.string.talk_q_plan),
    STEERING(R.string.talk_q_steering),
    SUGGEST(R.string.talk_q_suggest)
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
        Question.GAME -> {
            val game = knowledge.game
            if (game == null) {
                Text(stringResource(R.string.talk_a_game_off), color = INK, size = 15)
            } else {
                Text(
                    stringResource(R.string.talk_a_game_level, game.level, game.xp),
                    color = INK, size = 15
                )
                Text(
                    stringResource(R.string.talk_a_game_purse, game.coins, game.pantry),
                    color = INK, size = 14
                )
                // **Die eigentliche Aussage dieser Frage.** Nicht die Zahlen, sondern was sie
                // fuer den naechsten Gang bedeuten: Ein leerer Vorrat schickt ihn in den Laden,
                // fehlt dazu das Geld, muss er erst arbeiten. Wer das gelesen hat, sieht kein
                // zufaelliges Herumlaufen mehr, sondern jemanden mit einem Problem.
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
                // Der Vorrat ist leer und Geld ist da: Dann kann man ihn losschicken. Fehlt auch
                // das Geld, waere die Bitte eine Zumutung - dann bleibt nur die Arbeit.
                when {
                    game.brokeAndHungry ->
                        QuestionLine(stringResource(R.string.talk_ask_work)) {
                            onAsk(AnimationType.WORK)
                        }
                    game.pantryEmpty ->
                        QuestionLine(stringResource(R.string.talk_ask_shop)) {
                            onAsk(AnimationType.DRINK)
                        }
                    else ->
                        QuestionLine(stringResource(R.string.talk_ask_walk)) {
                            onAsk(AnimationType.MOVE)
                        }
                }
            }
        }

        Question.TODAY -> {
            if (!knowledge.hasPlan) {
                Text(stringResource(R.string.talk_a_today_noplan), color = INK, size = 15)
            } else if (knowledge.fedToday == 0) {
                // In SEINER Stimme: Ein leerer Tag ist der Moment, in dem es am meisten darauf
                // ankommt, WIE es gesagt wird. Wyrmling fragt, ob man anfangen will; Gloop sagt,
                // es sei kein Stress. Die Tatsache ist dieselbe.
                Text(stringResource(voice.emptyDay), color = INK, size = 15)
            } else {
                Text(
                    stringResource(R.string.talk_a_today, knowledge.fedToday),
                    color = INK, size = 15
                )
                if (knowledge.goalsTotal > 0) {
                    Text(
                        stringResource(
                            R.string.talk_a_today_goals,
                            knowledge.goalsReached,
                            knowledge.goalsTotal
                        ),
                        color = INK_DIM, size = 13
                    )
                }
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

        Question.STEERING -> {
            // **Die Frage, wegen der das Ganze existiert.** Was hier steht, ist keine Auskunft
            // ueber die Vergangenheit, sondern eine Ansage ueber das, was gleich zu sehen sein
            // wird: Offene Themen heben ihre Gewichtung an (siehe PlayHabitSignal), er greift
            // also von sich aus oefter danach. Wer das gelesen hat, sieht ihm anschliessend beim
            // Einloesen zu.
            if (knowledge.steering.isEmpty()) {
                Text(
                    stringResource(
                        // "Nichts hat ein Tagesziel" ist eine sachliche Auskunft und bleibt
                        // neutral; "alles erledigt" ist ein Moment, der ihm gehoert.
                        if (knowledge.goalsTotal == 0) R.string.talk_a_steering_nogoals
                        else voice.allDone
                    ),
                    color = INK, size = 15
                )
            } else {
                Text(stringResource(R.string.talk_a_steering), color = INK, size = 15)
                Text(stringResource(R.string.talk_a_steering_hint), color = INK_DIM, size = 13)
                // **Hier wird aus dem Bericht ein Gegenueber.** Bis zu dieser Zeile sagt er nur,
                // wie es steht; ab ihr darf man ihn bitten - und er geht sofort los. Dass die
                // offenen Themen einzeln antippbar sind statt als blosse Aufzaehlung dazustehen,
                // ist der ganze Unterschied.
                for (topic in knowledge.steering) {
                    QuestionLine(
                        stringResource(R.string.talk_ask_now, stringResource(topic.labelRes))
                    ) { onAsk(topic) }
                }
            }
        }

        Question.SUGGEST -> {
            val topic = PlayTalk.nextSuggestion(knowledge)
            if (topic == null) {
                Text(stringResource(R.string.talk_a_suggest_none), color = INK, size = 15)
            } else if (justAdded == topic) {
                Text(
                    stringResource(R.string.talk_a_suggest_added, stringResource(topic.labelRes)),
                    color = INK, size = 15
                )
                Text(stringResource(R.string.talk_a_suggest_added_hint), color = INK_DIM, size = 13)
            } else {
                Text(stringResource(voice.offering), color = INK_DIM, size = 13)
                Text(
                    stringResource(R.string.talk_a_suggest, stringResource(topic.labelRes)),
                    color = INK, size = 15
                )
                val preset = PlayTalk.presetFor(topic)
                Text(
                    stringResource(
                        R.string.talk_a_suggest_detail,
                        formatMinuteOfDay(preset.startMinuteOfDay),
                        formatMinuteOfDay(preset.endMinuteOfDay),
                        preset.dailyGoal
                    ),
                    color = INK_DIM, size = 13
                )
                QuestionLine(stringResource(R.string.talk_a_suggest_accept)) { onAddReminder(topic) }
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
