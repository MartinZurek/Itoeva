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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.R

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
    speciesLabel: String,
    onAddReminder: (AnimationType) -> Unit,
    onOpenReminders: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Welche Frage gerade beantwortet wird - null heisst: nur die Fragen stehen da.
    var asked by remember { mutableStateOf<Question?>(null) }
    // Was gerade angelegt wurde, damit der Vorschlag danach nicht unveraendert dasteht.
    var justAdded by remember { mutableStateOf<AnimationType?>(null) }

    Column(
        modifier = modifier
            .widthIn(max = 340.dp)
            .background(PANEL, RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFF2A2A28), RoundedCornerShape(14.dp))
            .padding(18.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(speciesLabel, color = INK_DIM, size = 12, weight = FontWeight.Medium)

        if (knowledge == null) {
            Text(stringResource(R.string.talk_loading), color = INK_DIM, size = 14)
        } else {
            val current = asked
            if (current == null) {
                for (question in Question.entries) {
                    // Vorschlaege nur anbieten, wenn es tatsaechlich etwas vorzuschlagen gibt -
                    // eine Frage, auf die "nichts" die Antwort ist, ist eine leere Zeile.
                    if (question == Question.SUGGEST &&
                        PlayTalk.nextSuggestion(knowledge) == null
                    ) continue
                    QuestionLine(stringResource(question.labelRes)) { asked = question }
                }
            } else {
                Answer(
                    question = current,
                    knowledge = knowledge,
                    justAdded = justAdded,
                    onAddReminder = { topic ->
                        justAdded = topic
                        onAddReminder(topic)
                    },
                    onOpenReminders = onOpenReminders
                )
                QuestionLine(stringResource(R.string.talk_back)) { asked = null }
            }
        }

        Text(
            stringResource(R.string.talk_close),
            color = INK_DIM,
            size = 13,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDismiss() }
                .padding(top = 4.dp)
        )
    }
}

/**
 * Die Fragen. Vier, und in dieser Reihenfolge: erst was war, dann was geplant ist, dann was ihn
 * heute umtreibt, dann - nur falls es etwas gibt - sein eigener Vorschlag.
 */
private enum class Question(val labelRes: Int) {
    TODAY(R.string.talk_q_today),
    PLAN(R.string.talk_q_plan),
    STEERING(R.string.talk_q_steering),
    SUGGEST(R.string.talk_q_suggest)
}

@Composable
private fun Answer(
    question: Question,
    knowledge: PlayTalk.Knowledge,
    justAdded: AnimationType?,
    onAddReminder: (AnimationType) -> Unit,
    onOpenReminders: () -> Unit
) {
    when (question) {
        Question.TODAY -> {
            if (!knowledge.hasPlan) {
                Text(stringResource(R.string.talk_a_today_noplan), color = INK, size = 15)
            } else if (knowledge.fedToday == 0) {
                Text(stringResource(R.string.talk_a_today_nothing), color = INK, size = 15)
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
                        if (knowledge.goalsTotal == 0) R.string.talk_a_steering_nogoals
                        else R.string.talk_a_steering_done
                    ),
                    color = INK, size = 15
                )
            } else {
                Text(stringResource(R.string.talk_a_steering), color = INK, size = 15)
                for (topic in knowledge.steering) {
                    Text("· " + stringResource(topic.labelRes), color = INK, size = 14)
                }
                Text(stringResource(R.string.talk_a_steering_hint), color = INK_DIM, size = 13)
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
        lineHeight = (size * 1.4f).sp,
        fontWeight = weight,
        modifier = modifier
    )
}
