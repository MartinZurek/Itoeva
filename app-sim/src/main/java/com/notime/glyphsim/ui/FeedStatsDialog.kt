package com.notime.glyphsim.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notime.glyphsim.R
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.data.FeedBreakdownRow
import com.notime.glyphsim.matrix.AvatarAnimations
import com.notime.glyphsim.matrix.AvatarMood
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.CompanionChapter
import com.notime.glyphsim.matrix.AvatarSpriteView
import com.notime.glyphsim.matrix.MatrixAnimator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Dunkle Flaechen des Dialogs - dieselbe Welt wie der schwarze Startbildschirm. */

/**
 * Pflegebuch eines Avatars: wie oft im gewaehlten Zeitraum auf Erinnerungen reagiert wurde, und
 * worum es dabei ging.
 *
 * **Gestaltung.** Die Auswertung stand vorher als nackte Zahlenliste da. Drei Dinge machen den
 * Unterschied: (1) der Avatar selbst im Kopf, damit sofort klar ist, WESSEN Buch man sieht -
 * jeder hat sein eigenes; (2) dieselben Emojis und Akzentfarben wie im Erinnerungs-Bildschirm
 * (siehe [animationVisuals]), sodass eine Zeile ohne Lesen wiedererkannt wird; (3) ein dunkler
 * Untergrund passend zum Startbildschirm, statt einer hellen Flaeche, die aus der App
 * herausfaellt.
 *
 * **Wortwahl.** Nicht "Fuetterungen", sondern Mahlzeiten in einem Pflegebuch - das beschreibt,
 * worum es geht: sich um etwas gekuemmert zu haben. Die technische Innensicht ("ein Feed-Event
 * wurde geloggt") gehoert nicht auf die Oberflaeche.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedStatsDialog(
    species: AvatarSpecies,
    onDismiss: () -> Unit,
    /** Tapthrough vom Rhythmus-Kommentar (siehe [RhythmSuggestion]) direkt in den
     *  Bearbeiten-Dialog der betroffenen Erinnerung. */
    onEditReminder: (reminderId: Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getInstance(context) }
    val dao = remember { db.avatarFeedEventDao() }
    // Zwei Besitzer, heute derselbe Wert (siehe RoutineOwner / PresentCompanion): die Zahlen
    // stammen aus dem Pflegebuch des anwesenden Wesens, die Erinnerungen dahinter aus den
    // Routinen des Nutzers. Getrennt gehalten, damit dieser Dialog nach dem Entkoppeln nicht
    // stumm die Ziele eines Profils gegen die Fuetterungen eines anderen rechnet.
    val companionId = remember(species) { AvatarSpeciesPrefs.profileId(species) }
    val routineOwner = remember(species) { RoutineOwner.current(context) }

    // Das Kapitel wird beim Oeffnen einmal bestimmt und nicht beobachtet: es aendert sich
    // hoechstens einmal in Tagen, ein Flow dafuer waere Aufwand ohne Wirkung.
    var chapter by remember(companionId) { mutableStateOf(CompanionChapter.ARRIVED) }
    LaunchedEffect(companionId) {
        val ersterMoment = withContext(Dispatchers.IO) { dao.firstAnsweredMillis(companionId) }
        chapter = CompanionChapter.chapterFor(ersterMoment, System.currentTimeMillis())
    }

    var period by remember { mutableStateOf(FeedStatsPeriod.TODAY) }
    var confirmReset by remember { mutableStateOf(false) }

    // Der Startzeitpunkt wird bei jedem Wechsel neu bestimmt statt einmal beim Oeffnen: bleibt
    // der Dialog ueber Mitternacht offen, zeigte er sonst weiter den alten Tag.
    val since = remember(period) { period.startMillis() }
    val total by remember(companionId, since) {
        dao.observeCountSince(companionId, since)
    }.collectAsStateWithLifecycle(initialValue = 0)
    val breakdown by remember(companionId, since) {
        dao.observeBreakdownSince(companionId, since)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    // Bibliotheks-Animationen speichern im Fuetter-Ereignis nur ihren Namen, das Emoji steht in
    // der Bibliothek. Statt dafuer eine Spalte zu duplizieren (die bei einer spaeter geaenderten
    // Bibliothek veralten wuerde), wird hier ueber den Namen nachgeschlagen.
    val libraryAnimations by remember { db.libraryAnimationDao().observeAll() }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val emojiByLabel = remember(libraryAnimations) {
        libraryAnimations.associate { it.label to it.emoji }
    }

    // Tagesziele immer fuer HEUTE, unabhaengig vom oben gewaehlten Zeitraum: ein Tagesziel endet
    // um Mitternacht: "3 von 4" fuer einen ganzen Monat waere sinnlos. Der Zeitraum-Umschalter
    // bezieht sich auf die Mahlzeiten darunter.
    val todayStart = remember { FeedStatsPeriod.TODAY.startMillis() }
    val reminders by remember(routineOwner) {
        db.glyphReminderDao().observeForProfile(routineOwner)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    // isPlayMode = false: Tagesziele gehoeren ausschliesslich den echten Erinnerungen. Was im
    // Spielmodus gefuettert wurde, ist ein Spielstand und darf ein Ziel weder erfuellen noch
    // beschoenigen (siehe AvatarFeedEvent.isPlayMode).
    val fedToday by remember(companionId, todayStart) {
        db.avatarFeedEventDao().observeFedPerReminderSince(companionId, todayStart, isPlayMode = false)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val goalRows = remember(reminders, fedToday) {
        val fedByReminder = fedToday.associate { it.reminderId to it.count }
        reminders
            .filter { it.enabled && it.dailyGoal > 0 }
            .map { GoalRow(it.label, it.dailyGoal, fedByReminder[it.id] ?: 0) }
    }

    // Stimmungs-Zeile des Avatars unten (siehe AvatarCommentarySection) - dieselbe schuldfreie
    // Sprache wie ueberall sonst (siehe AvatarMood-Klassendoku, settings_mood_hint): "weniger
    // Energie", nie "traurig" oder "enttaeuscht". moodEnabled/goalRows-Faelle zuerst, weil
    // AvatarMood.NEUTRAL beides bedeuten kann (Stimmung aus, oder einfach keine Ziele) und ohne
    // die Unterscheidung stumpf dieselbe wenig hilfreiche Zeile zeigen wuerde.
    val mood by rememberAvatarMood(species)
    val moodEnabled = remember { MoodPrefs.isEnabled(context) }
    val moodTextRes = when {
        !moodEnabled -> R.string.stats_mood_disabled
        goalRows.isEmpty() -> R.string.stats_mood_no_goals
        else -> when (mood) {
            AvatarMood.HAPPY -> R.string.stats_mood_happy
            AvatarMood.CONTENT -> R.string.stats_mood_content
            AvatarMood.HUNGRY -> R.string.stats_mood_hungry
            AvatarMood.SAD -> R.string.stats_mood_sad
            AvatarMood.NEUTRAL -> R.string.stats_mood_no_goals
        }
    }

    // Rhythmus-Kommentar des Avatars (siehe RhythmSuggestion.kt) - unabhaengig vom oben
    // gewaehlten Anzeige-Zeitraum, immer ueber ein rollierendes Fenster der letzten
    // WINDOW_DAYS Tage, damit der Umschalter oben ihn nicht beeinflusst.
    val rhythmWindowStart = remember { System.currentTimeMillis() - WINDOW_DAYS * 24L * 60 * 60 * 1000 }
    // Ebenfalls ohne Spielmodus: Der Rhythmus-Vorschlag raet zu Aenderungen an den EIGENEN
    // Erinnerungen - Spielstand-Zahlen wuerden diesen Rat verfaelschen.
    val fedInRhythmWindow by remember(companionId, rhythmWindowStart) {
        db.avatarFeedEventDao().observeFedPerReminderSince(companionId, rhythmWindowStart, isPlayMode = false)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val firstOccurrencePerReminder by remember(companionId) {
        db.avatarFeedEventDao().observeFirstOccurrencePerReminder(companionId)
    }.collectAsStateWithLifecycle(initialValue = emptyList())
    val rhythmSuggestion = remember(reminders, fedInRhythmWindow, firstOccurrencePerReminder) {
        findRhythmSuggestion(
            reminders = reminders,
            fedCountByReminderId = fedInRhythmWindow.associate { it.reminderId to it.count },
            firstOccurrenceByReminderId = firstOccurrencePerReminder.associate { it.reminderId to it.firstEpochMillis }
        )?.takeIf { RhythmSuggestionPrefs.shouldShow(context, it.reminderId, it.kind) }
    }
    // Als "gezeigt" gilt schon das Erscheinen, nicht erst ein Dismiss - der Kommentar ist eine
    // beilaeufige Beobachtung, kein Dialog, der eine Reaktion erwartet.
    LaunchedEffect(rhythmSuggestion) {
        rhythmSuggestion?.let { RhythmSuggestionPrefs.markShown(context, it.reminderId, it.kind) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DialogPalette.SheetBackground,
        titleContentColor = DialogPalette.TextPrimary,
        textContentColor = DialogPalette.TextPrimary,
        // Avatar und Name standen hier zusaetzlich zur AvatarCommentarySection ganz unten -
        // dieselbe Figur zweimal im selben Dialog. Wessen Buch das ist, zeigt jetzt allein die
        // Figur dort unten (die Spezies IST das Profil, siehe AvatarSpeciesPrefs - ein Blick
        // reicht). Titel bleibt deshalb einfacher, reiner Text.
        title = {
            Column {
                Text(
                    stringResource(R.string.stats_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                // Das Kapitel als Untertitel (siehe CompanionChapter): Es beantwortet die Frage,
                // die ueber diesem Buch steht - wessen Buch ist das, und seit wann? Ohne diese
                // Einordnung liest sich eine kurze Liste wie ein magerer Ertrag statt wie der
                // Anfang von etwas.
                //
                // Bewusst nur der Name und keine Tageszahl: "seit 11 Tagen" waere wieder eine
                // Zahl, die man vergleichen kann.
                Text(
                    stringResource(chapter.labelRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = DialogPalette.TextMuted
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Segmentierte Leiste statt einzelner Chips: die vier Zeitraeume schliessen
                // einander aus, und als geschlossene Leiste liest sich das als eine Auswahl
                // statt als vier unabhaengige Schalter.
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    FeedStatsPeriod.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = option == period,
                            onClick = { period = option },
                            shape = SegmentedButtonDefaults.itemShape(index, FeedStatsPeriod.entries.size)
                        ) {
                            Text(stringResource(option.labelRes), maxLines = 1)
                        }
                    }
                }

                HeroCount(total = total)

                // Tagesziele zuerst: das ist die Aussage, an der sich ablesen laesst, ob der Tag
                // gut lief. Die Mahlzeiten-Aufschluesselung darunter beschreibt nur, WORUM es
                // dabei ging.
                if (goalRows.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val reached = goalRows.count { it.achieved >= it.goal }
                        Text(
                            stringResource(R.string.stats_goals_title, reached, goalRows.size),
                            style = MaterialTheme.typography.labelLarge,
                            color = DialogPalette.TextMuted
                        )
                        // Klarstellung, weil das hier IMMER "heute" ist, unabhaengig vom
                        // Zeitraum-Umschalter oben drueber - ohne den Hinweis wirkte das wie ein
                        // Widerspruch, wenn oben z.B. "Diese Woche" ausgewaehlt ist.
                        Text(
                            stringResource(R.string.stats_goals_today_note),
                            style = MaterialTheme.typography.labelSmall,
                            color = DialogPalette.TextMuted
                        )
                        goalRows.forEach { GoalRowView(it) }
                    }
                }

                if (breakdown.isEmpty()) {
                    Text(
                        stringResource(R.string.stats_empty, stringResource(species.labelRes)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = DialogPalette.TextMuted
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.stats_breakdown_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = DialogPalette.TextMuted
                        )
                        val maxCount = breakdown.maxOf { it.count }.coerceAtLeast(1)
                        breakdown.forEach { row ->
                            BreakdownRow(
                                row = row,
                                maxCount = maxCount,
                                emojiByLabel = emojiByLabel
                            )
                        }
                    }
                }

                // Ganz am Ende, nach den Zahlen: erst die Fakten, dann die Einordnung des Avatars
                // dazu - er kommentiert den Tag, er ist nicht die Schlagzeile des Dialogs.
                AvatarCommentarySection(
                    species = species,
                    moodTextRes = moodTextRes,
                    rhythmSuggestion = rhythmSuggestion,
                    onEditReminder = onEditReminder
                )

                if (confirmReset) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(R.string.stats_reset_confirm, stringResource(species.labelRes)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { confirmReset = false }) {
                                Text(stringResource(R.string.action_cancel))
                            }
                            TextButton(
                                onClick = {
                                    scope.launch(Dispatchers.IO) { dao.deleteForProfile(companionId) }
                                    confirmReset = false
                                }
                            ) { Text(stringResource(R.string.action_delete)) }
                        }
                    }
                } else {
                    TextButton(
                        onClick = { confirmReset = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.stats_reset), color = DialogPalette.TextMuted)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
        }
    )
}

/** Ein paar beilaeufige Zusatz-Saetze fuer [AvatarCommentarySection] - erscheint einer davon, wenn
 *  man den Avatar dort antippt. Bewusst eine kleine feste Auswahl statt bezogen auf den Tag: es
 *  sind allgemeine, jederzeit gueltige Hinweise, kein weiterer Datenauswertung noetig. */
private val TAP_TIPS = listOf(
    R.string.stats_avatar_tip_1,
    R.string.stats_avatar_tip_2,
    R.string.stats_avatar_tip_3
)

/**
 * Der Avatar am Ende des Pflegebuchs: seine eigene, schuldfreie Einschaetzung des Tages
 * ([moodTextRes], siehe AvatarMood-Klassendoku) und - falls vorhanden - der Rhythmus-Kommentar
 * ([RhythmSuggestion]) direkt darunter, antippbar in den Bearbeiten-Dialog der betroffenen
 * Erinnerung. Beides in EINER Karte statt getrennter Zeilen: es ist dieselbe Stimme, die erst
 * sagt, wie es ihm geht, und dann - falls es dazu etwas zu sagen gibt - einen Rat dazu gibt.
 *
 * Antippen des Avatar-Sprites selbst spielt eine kurze Freuden-Reaktion ([AvatarAnimations.tapReaction])
 * und blendet einen von [TAP_TIPS] ein - dieselbe "reaktiver" Anmutung wie beim Antippen auf dem
 * Startbildschirm, nur hier ohne Uebergang in einen weiteren Dialog, weil man ja schon in einem ist.
 */
@Composable
private fun AvatarCommentarySection(
    species: AvatarSpecies,
    @StringRes moodTextRes: Int,
    rhythmSuggestion: RhythmSuggestion?,
    onEditReminder: (reminderId: Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    var avatarFrame by remember(species) { mutableStateOf(AvatarAnimations.idlePose(species)) }
    var isReacting by remember { mutableStateOf(false) }
    var tipIndex by remember { mutableStateOf<Int?>(null) }

    fun onAvatarTap() {
        if (isReacting) return
        isReacting = true
        // Beim naechsten Antippen einen ANDEREN Tipp als zuletzt zeigen, statt womoeglich
        // denselben zweimal hintereinander - sonst wirkt "antippen fuer mehr" schnell wie nichts
        // tut sich.
        tipIndex = TAP_TIPS.indices.filter { it != tipIndex }.random()
        scope.launch {
            try {
                val reaction = AvatarAnimations.tapReaction(species)
                MatrixAnimator.playTimed(reaction.frames, reaction.holdsMs) { avatarFrame = it }
            } finally {
                avatarFrame = AvatarAnimations.idlePose(species)
                isReacting = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DialogPalette.RowBackground)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Der Avatar ist antippbar (er gibt dann einen Hinweis). Sichtbar bleibt er 40 dp
            // gross; die Trefferflaeche waechst auf die von Android geforderten 48 dp, ohne dass
            // sich am Bild etwas aendert - hier ist der Platz dafuer da, anders als bei den
            // sieben Wochentagskacheln nebeneinander (siehe ReminderScreen).
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = ::onAvatarTap
                    ),
                contentAlignment = Alignment.Center
            ) {
                AvatarSpriteView(
                    frame = avatarFrame,
                    showBackground = false,
                    modifier = Modifier.size(40.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(moodTextRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DialogPalette.TextPrimary
                )
                tipIndex?.let { index ->
                    Text(
                        stringResource(TAP_TIPS[index]),
                        style = MaterialTheme.typography.bodySmall,
                        color = DialogPalette.TextMuted
                    )
                }
            }
        }
        rhythmSuggestion?.let { suggestion ->
            val textRes = when (suggestion.kind) {
                RhythmSuggestion.Kind.OVER -> R.string.stats_rhythm_over
                RhythmSuggestion.Kind.UNDER -> R.string.stats_rhythm_under
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            // Dismiss bleibt beim Aufrufer (siehe onOpenSettings in AvatarAssistantDialog fuer
            // dasselbe Muster) - der schliesst diesen Dialog UND oeffnet den naechsten Schritt in
            // einem Zug, statt hier vorzugreifen. Der Pfeil macht die Zeile als antippbar
            // erkennbar, sonst saehe sie wie reiner Text ohne Aktion aus.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditReminder(suggestion.reminderId) },
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(textRes, suggestion.label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = DialogPalette.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text("›", style = MaterialTheme.typography.titleMedium, color = DialogPalette.TextMuted)
            }
        }
    }
}

/** Fortschritt eines Tagesziels (siehe [com.notime.glyphcore.data.GlyphReminder.dailyGoal]). */
private data class GoalRow(val label: String, val goal: Int, val achieved: Int)

/**
 * Eine Zielzeile: Bezeichnung, erreicht/vorgenommen und ein Balken.
 *
 * Erreichte Ziele bekommen einen Haken und eine kraeftige Farbe - der Unterschied zwischen
 * "fast geschafft" und "geschafft" soll auf einen Blick erkennbar sein, denn genau darauf
 * reagiert auch die Stimmung des Avatars.
 */
@Composable
private fun GoalRowView(row: GoalRow) {
    val done = row.achieved >= row.goal
    val accent = if (done) Color(0xFF63C88A) else Color(0xFFE0B341)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DialogPalette.RowBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(if (done) "✓" else "•", color = accent, fontSize = 16.sp)
            Text(
                row.label,
                style = MaterialTheme.typography.bodyLarge,
                color = DialogPalette.TextPrimary,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${row.achieved.coerceAtMost(row.goal)}/${row.goal}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.07f))
        ) {
            Box(
                modifier = Modifier
                    // Ueber das Ziel hinaus faellt der Balken nicht weiter aus - uebererfuellt
                    // ist erfuellt, genau wie in der Stimmungsberechnung.
                    .fillMaxWidth((row.achieved.toFloat() / row.goal).coerceAtMost(1f))
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )
        }
    }
}

/**
 * Die Kernzahl gross und allein stehend. Vorher stand sie als Fliesstext ("12 Fuetterungen")
 * zwischen den uebrigen Zeilen und ging darin unter - obwohl sie die eine Angabe ist, wegen der
 * man den Dialog oeffnet.
 */
@Composable
private fun HeroCount(total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            total.toString(),
            fontSize = 52.sp,
            fontWeight = FontWeight.Light,
            color = DialogPalette.TextPrimary
        )
        Text(
            " " + if (total == 1) {
                stringResource(R.string.stats_meals_one)
            } else {
                stringResource(R.string.stats_meals_other)
            },
            style = MaterialTheme.typography.titleMedium,
            color = DialogPalette.TextMuted,
            modifier = Modifier.padding(bottom = 10.dp)
        )
    }
}

/**
 * Eine Zeile der Aufschluesselung: Emoji im farbigen Kreis, Name, Anzahl, darunter ein Anteilsbalken
 * in der Akzentfarbe der jeweiligen Erinnerung.
 *
 * Der Balken zeigt den Anteil am haeufigsten Eintrag ([maxCount]), nicht am Gesamtwert - so
 * bleibt der Vergleich der Zeilen untereinander ablesbar, auch wenn sich die Fuetterungen auf
 * viele Typen verteilen und jeder einzelne Anteil klein waere.
 */
@Composable
private fun BreakdownRow(
    row: FeedBreakdownRow,
    maxCount: Int,
    emojiByLabel: Map<String, String>
) {
    val accent = row.animationType?.let { animationVisuals[it]?.color } ?: LIBRARY_ACCENT
    val emoji = row.animationType?.let { animationVisuals[it]?.emoji }
        ?: row.libraryAnimationLabel?.let { emojiByLabel[it] }
        ?: "✨"
    val label = row.animationType?.let { stringResource(it.labelRes) }
        ?: row.libraryAnimationLabel
        ?: stringResource(R.string.stats_unknown)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DialogPalette.RowBackground)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 17.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = DialogPalette.TextPrimary,
                    maxLines = 1
                )
                // Bewusst NUR die Zahl der Ausloesungen, keine Quote: eine Erinnerung im
                // 5-Minuten-Takt ist ein Netz, kein hundertfacher Auftrag - "4 von 100" haette
                // gute Nutzung wie Versagen aussehen lassen. Wie zuverlaessig jemand ist, steht
                // im Abschnitt der Tagesziele, wo es tatsaechlich etwas bedeutet.
                Text(
                    stringResource(R.string.stats_triggered_count, row.triggered),
                    style = MaterialTheme.typography.labelSmall,
                    color = DialogPalette.TextMuted
                )
            }
            Text(
                row.count.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
        }
        // Eigener Balken statt LinearProgressIndicator: der zeichnet in der Themefarbe und
        // saehe fuer jede Zeile gleich aus - hier traegt gerade die Farbe die Unterscheidung.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.07f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(row.count.toFloat() / maxCount)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )
        }
    }
}

/** Akzent fuer Bibliotheks-Animationen - dieselbe Farbe wie im Erinnerungs-Bildschirm. */
private val LIBRARY_ACCENT = Color(0xFF8E24AA)
