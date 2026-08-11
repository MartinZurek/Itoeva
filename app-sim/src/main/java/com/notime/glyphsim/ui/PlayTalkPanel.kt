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
                // Die Verschiebung wird je Gespraech EINMAL gezogen: Waehrend man liest, soll
                // sich der Vorschlag nicht unter dem Finger aendern - beim naechsten Oeffnen
                // dagegen schon (siehe PlayTalk.focus).
                val focus = remember(knowledge) { PlayTalk.focus(knowledge, Random.nextInt(1_000)) }
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
                    doing = doing,
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
    PLAN(R.string.talk_q_plan),
    /**
     * **Was hier eigentlich passiert.** Der Spielmodus erklaert sich nicht von selbst: Ein Wesen,
     * das umherlaeuft, arbeitet und einkauft, sieht huebsch aus, aber ohne einen Satz dazu bleibt
     * offen, was das mit einem selbst zu tun hat. Erklaert wird deshalb GENAU dieser Modus - im
     * Alltag-Modus gaebe es hier gar kein Wesen zum Antippen, und im Uhr-Modus soll es keines
     * geben.
     */
    HERE(R.string.talk_q_here),
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
    }
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
    onAsk: (AnimationType) -> Unit
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
