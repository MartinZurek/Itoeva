package com.notime.glyphsim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.DAILY_GOAL_OPTIONS
import com.notime.glyphcore.data.DaysOfWeekMask
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphcore.data.INTERVAL_OPTIONS
import com.notime.glyphcore.data.LibraryAnimation
import com.notime.glyphcore.data.NO_GOAL
import com.notime.glyphcore.data.ReminderOpenDuration
import com.notime.glyphsim.R
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.MatrixAnimator
import com.notime.glyphsim.matrix.MatrixGeometry
import com.notime.glyphsim.matrix.SimulatedMatrixView
import java.time.DayOfWeek
import java.time.format.TextStyle

private val dayLabels: Map<DayOfWeek, String> = mapOf(
    DayOfWeek.MONDAY to "Mo",
    DayOfWeek.TUESDAY to "Tu",
    DayOfWeek.WEDNESDAY to "We",
    DayOfWeek.THURSDAY to "Th",
    DayOfWeek.FRIDAY to "Fr",
    DayOfWeek.SATURDAY to "Sa",
    DayOfWeek.SUNDAY to "Su"
)

/** Akzentfarbe fuer Animationen aus der erweiterbaren Bibliothek (statt einer festen pro Typ). */
private val libraryAccentColor = Color(0xFF8E24AA)



/**
 * Ab dieser Offen-Dauer weist der Dialog darauf hin, dass Startbildschirm und Widget deckeln
 * (dort gibt es keinen Avatar zum Fuettern) - muss zu MAX_HOME_DURATION_SECONDS in
 * HomeScreen.kt bzw. MAX_WIDGET_DURATION_SECONDS in GlyphClockWidgetProvider.kt passen.
 */
private const val MAX_UNATTENDED_DURATION_SECONDS = 60

private fun formatMinuteOfDay(minuteOfDay: Int): String =
    "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)

private val emptyFrame = IntArray(MatrixGeometry.SIZE * MatrixGeometry.SIZE)

/**
 * Snapshot der noch ungespeicherten Eingaben im Reminder-Dialog, aufgenommen beim
 * Wechsel in den Bibliotheks-Screen (Button "More"/Zahnrad-Kreis im AnimationPicker):
 * ReminderDialog wird dabei geschlossen (ein AlertDialog kann nicht "unsichtbar,
 * aber weiter gemerkt" bleiben - er wuerde ueber dem Bibliotheks-Screen schweben) und
 * beim Zurueckkommen mit exakt diesem Stand neu geoeffnet, statt leer.
 */
internal data class ReminderDraft(
    val label: String,
    val animationChoice: AnimationChoice,
    val selectedDays: Set<DayOfWeek>,
    val startMinute: Int,
    val endMinute: Int,
    val interval: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    onBack: (() -> Unit)? = null,
    /** Oeffnet direkt deren Bearbeiten-Dialog, sobald sie geladen ist - das Tapthrough vom
     *  Rhythmus-Kommentar im Pflegebuch (siehe FeedStatsDialog/MainActivity). */
    initialEditReminderId: Long? = null,
    viewModel: GlyphReminderViewModel = viewModel()
) {
    var showLibrary by rememberSaveable { mutableStateOf(false) }
    /*
     * EIN Zustand statt vier Fluessen (Phase 3.1). Die Ableitungen - welche Animationen
     * ausgewaehlt sind, ob der Leer-Zustand gilt - stehen jetzt in ReminderUiState und nicht mehr
     * hier: sie gehoeren zur Datenschicht, nicht zur Darstellung, und sind dort ohne Geraet
     * pruefbar (siehe ReminderUiStateTest).
     */
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reminders = state.reminders
    val activeSpecies = state.species
    val libraryAnimations = state.libraryAnimations
    val selectedLibraryAnimations = state.selectedLibraryAnimations
    val selectedBuiltInTypes = state.selectedBuiltInTypes
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    /*
     * Die Id statt der Erinnerung selbst - aus zwei Gruenden.
     *
     * Erstens laesst sich eine Id in ein Bundle schreiben und ueberlebt damit den Neuaufbau der
     * Activity und den Prozesstod; ein GlyphReminder-Objekt nicht.
     *
     * Zweitens war das festgehaltene Objekt ein stiller Fehler: es blieb auf dem Stand des
     * Antippens stehen. Aenderte sich die Erinnerung waehrenddessen anderswo - der Watchdog
     * schreibt `nextTriggerEpochMillis`, der Spielmodus wuerfelt Thema und Intervall neu -,
     * bearbeitete der Dialog weiter den alten Stand und schriebe ihn beim Speichern zurueck.
     * Ueber die Id wird bei jedem Neuzeichnen die aktuelle Zeile aufgeloest.
     */
    var editingReminderId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editingReminder = editingReminderId?.let { id -> reminders.firstOrNull { it.id == id } }
    // reminders laedt asynchron aus der DB, ist beim ersten Composen also noch leer - deshalb
    // erst zugreifen, sobald die gesuchte Id tatsaechlich auftaucht, statt einmalig beim Start.
    // "consumed" verhindert, dass ein spaeteres Aktualisieren der Liste (z.B. nach dem Speichern)
    // denselben Dialog ein zweites Mal aufreisst.
    var initialEditConsumed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(reminders, initialEditReminderId) {
        if (!initialEditConsumed && initialEditReminderId != null) {
            reminders.find { it.id == initialEditReminderId }?.let {
                editingReminderId = it.id
                initialEditConsumed = true
            }
        }
    }
    /*
     * Die halb fertigen Eingaben ueberleben jetzt den Neuaufbau der Activity und den Prozesstod -
     * bis hierher waren sie bei der Rueckkehr aus einer anderen App weg. Die Uebersetzung ins
     * Bundle steht in ReminderDraftSaver.kt.
     *
     * `nullable` braucht den Saver ausdruecklich: rememberSaveable kann sonst nicht unterscheiden,
     * ob nichts gespeichert war oder ob der gespeicherte Wert null lautete.
     */
    var addDraft by rememberSaveable(stateSaver = ReminderDraftSaver) {
        mutableStateOf<ReminderDraft?>(null)
    }
    var editDraft by rememberSaveable(stateSaver = ReminderDraftSaver) {
        mutableStateOf<ReminderDraft?>(null)
    }
    var showCopyDialog by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    if (showLibrary) {
        LibraryScreen(onBack = { showLibrary = false }, viewModel = viewModel)
    } else {
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                // Avatar-Name im Titel, weil die Liste NUR die Erinnerungen dieses Avatars
                // zeigt - sonst waere unklar, warum sie sich beim Avatar-Wechsel aendert.
                title = { Text(stringResource(R.string.reminders_title, stringResource(activeSpecies.labelRes))) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
                    }
                },
                actions = {
                    // Erinnerungs-Satz eines anderen Avatars uebernehmen - ohne das muesste ein
                    // eingerichteter Satz fuer jeden weiteren Avatar von Hand nachgebaut werden.
                    TextButton(onClick = { showCopyDialog = true }) { Text(stringResource(R.string.reminders_copy)) }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.action_new)) },
                // Die Beschriftung des Knopfes erreicht den zusammengefuehrten Semantik-Knoten
                // nicht - er traegt nur Role=Button und sonst nichts, ein Screenreader liest dort
                // also blosses "Schaltflaeche" vor. Aufgefallen beim Aufbau der Compose-Tests, die
                // ihn ueber genau diese Semantik suchen und nicht fanden. Die Beschreibung am
                // Symbol schliesst die Luecke.
                icon = {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.action_new))
                },
                onClick = { showAddDialog = true }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (reminders.isEmpty()) {
                EmptyState(onAddClick = { showAddDialog = true }, modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(reminders, key = { it.id }) { reminder ->
                        ReminderCard(
                            reminder = reminder,
                            choice = reminder.animationChoice(libraryAnimations),
                            onClick = { editingReminderId = reminder.id },
                            onToggle = { enabled -> viewModel.setEnabled(reminder, enabled) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ReminderDialog(
            initial = null,
            initialDraft = addDraft,
            libraryAnimations = selectedLibraryAnimations,
            builtInTypes = selectedBuiltInTypes,
            resolveFrames = viewModel::framesFor,
            onManageLibrary = { draft -> addDraft = draft; showLibrary = true },
            onDismiss = { showAddDialog = false; addDraft = null },
            onSave = { label, choice, days, start, end, interval, openDuration, goal ->
                viewModel.addReminder(label, choice, days, start, end, interval, openDuration, goal)
                showAddDialog = false
                addDraft = null
            },
            onDelete = null
        )
    }

    editingReminder?.let { reminder ->
        ReminderDialog(
            initial = reminder,
            initialDraft = editDraft,
            libraryAnimations = selectedLibraryAnimations,
            builtInTypes = selectedBuiltInTypes,
            resolveFrames = viewModel::framesFor,
            onManageLibrary = { draft -> editDraft = draft; showLibrary = true },
            onDismiss = { editingReminderId = null; editDraft = null },
            onSave = { label, choice, days, start, end, interval, openDuration, goal ->
                val (animationType, libraryAnimationId) = choice.toStorage()
                viewModel.updateReminder(
                    reminder.copy(
                        label = label,
                        animationType = animationType,
                        libraryAnimationId = libraryAnimationId,
                        daysOfWeekMask = days,
                        startMinuteOfDay = start,
                        endMinuteOfDay = end,
                        intervalMinutes = interval,
                        openDurationSeconds = openDuration,
                        dailyGoal = goal
                    )
                )
                editingReminderId = null
                editDraft = null
            },
            onDelete = {
                viewModel.deleteReminder(reminder)
                editingReminderId = null
                editDraft = null
            }
        )
    }
    }

    if (showCopyDialog) {
        CopyRemindersDialog(
            activeSpecies = activeSpecies,
            loadCounts = viewModel::reminderCountsBySpecies,
            onCopy = { source, replace ->
                viewModel.copyRemindersFrom(source, replace)
                showCopyDialog = false
            },
            onDismiss = { showCopyDialog = false }
        )
    }

}

@Composable
private fun EmptyState(onAddClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✨", fontSize = 48.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.reminders_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.reminders_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onAddClick) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.reminders_empty_action))
        }
    }
}

@Composable
private fun AnimationAvatar(choice: AnimationChoice, size: Dp = 48.dp) {
    val emoji: String
    val color: Color
    when (choice) {
        is AnimationChoice.BuiltIn -> animationVisuals.getValue(choice.type).let { emoji = it.emoji; color = it.color }
        is AnimationChoice.Library -> { emoji = choice.emoji; color = libraryAccentColor }
    }
    Box(
        modifier = Modifier
            .size(size)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = (size.value * 0.5f).sp)
    }
}

/** Small, non-interactive row of dots: shows at a glance which weekdays a reminder is active on. */
@Composable
private fun DayDots(activeDays: Set<DayOfWeek>, accentColor: Color, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    /*
     * Die sieben Kreise sind eine Uebersicht, kein Bedienelement. Vorgelesen ergaben sie
     * "M T W T F S S" - sieben Buchstaben ohne Aussage darueber, WELCHE davon aktiv sind, und mit
     * zwei doppelten obendrein. Stattdessen eine einzige Aussage mit genau der Information, die
     * die Kreise optisch vermitteln: an welchen Tagen die Erinnerung laeuft.
     */
    val aktiveTage = DayOfWeek.entries
        .filter { it in activeDays }
        .joinToString { it.getDisplayName(TextStyle.FULL, locale) }
    Row(
        modifier = modifier
            .clearAndSetSemantics { contentDescription = aktiveTage },
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DayOfWeek.entries.forEach { day ->
            val isActive = day in activeDays
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(
                        if (isActive) accentColor.copy(alpha = 0.85f) else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    dayLabels.getValue(day).take(1),
                    fontSize = 9.sp,
                    color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: GlyphReminder,
    choice: AnimationChoice,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val activeDays = DaysOfWeekMask.toSet(reminder.daysOfWeekMask)
    val wrapsMidnight = reminder.endMinuteOfDay <= reminder.startMinuteOfDay
    val timeLabel = "${formatMinuteOfDay(reminder.startMinuteOfDay)}–${formatMinuteOfDay(reminder.endMinuteOfDay)}" +
        if (wrapsMidnight) " (+1)" else ""
    val accentColor = when (choice) {
        is AnimationChoice.BuiltIn -> animationVisuals.getValue(choice.type).color
        is AnimationChoice.Library -> libraryAccentColor
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.alpha(if (reminder.enabled) 1f else 0.4f)) {
                AnimationAvatar(choice)
            }
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f).alpha(if (reminder.enabled) 1f else 0.5f)
            ) {
                Text(
                    reminder.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "$timeLabel · every ${reminder.intervalMinutes} min · " +
                        "open ${ReminderOpenDuration.label(reminder.openDurationSeconds)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                DayDots(activeDays, accentColor)
            }
            Spacer(Modifier.width(4.dp))
            Switch(checked = reminder.enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnimationPicker(
    selected: AnimationChoice,
    builtInTypes: Set<AnimationType>,
    libraryAnimations: List<LibraryAnimation>,
    onSelect: (AnimationChoice) -> Unit,
    onManageLibrary: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimationType.entries.filter { it in builtInTypes }.forEach { type ->
            val visual = animationVisuals.getValue(type)
            val isSelected = selected is AnimationChoice.BuiltIn && selected.type == type
            AnimationPickerItem(
                emoji = visual.emoji,
                label = stringResource(type.labelRes),
                color = visual.color,
                isSelected = isSelected,
                onClick = { onSelect(AnimationChoice.BuiltIn(type)) }
            )
        }
        libraryAnimations.forEach { library ->
            val isSelected = selected is AnimationChoice.Library && selected.id == library.id
            AnimationPickerItem(
                emoji = library.emoji,
                label = library.label,
                color = libraryAccentColor,
                isSelected = isSelected,
                onClick = { onSelect(AnimationChoice.Library(library.id, library.label, library.emoji)) }
            )
        }
        if (onManageLibrary != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(60.dp).clickable(onClick = onManageLibrary).padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.reminder_animation_more))
                }
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.action_more), style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun AnimationPickerItem(
    emoji: String,
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp).clickable(onClick = onClick).padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(if (isSelected) color else color.copy(alpha = 0.18f), CircleShape)
                .then(
                    if (isSelected) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 22.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun DayPicker(
    selected: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        val locale = LocalConfiguration.current.locales[0]
        DayOfWeek.entries.forEach { day ->
            val isSelected = day in selected
            /*
             * Trefferflaeche und sichtbarer Kreis sind getrennt.
             *
             * Der Kreis misst 38 dp - unter den 48 dp, die Android als Mindestgroesse fuer ein
             * Bedienelement vorgibt. Ihn einfach zu vergroessern geht nicht: sieben Kacheln zu
             * 48 dp brauchen 336 dp nebeneinander, und so breit ist der Dialoginhalt auf einem
             * gewoehnlichen Telefon nicht (nachgemessen: rund 330 dp).
             *
             * Deshalb bekommt die aeussere Flaeche `weight(1f)` und teilt sich die verfuegbare
             * Breite zu gleichen Teilen - auf diesem Geraet rund 47 dp je Kachel, auf breiteren
             * Bildschirmen mehr - bei einer Mindesthoehe von 48 dp. Der Kreis bleibt bei 38 dp.
             * Damit waechst das Ziel mit dem Platz, statt an einer festen Zahl zu scheitern.
             */
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    /*
                     * `toggleable` statt `clickable`: die Kachel ist ein Schalter, kein Knopf.
                     * Damit meldet sie einem Screenreader auch ihren Zustand ("aktiviert" /
                     * "nicht aktiviert") und laesst sich mit Switch Access und Tastatur bedienen -
                     * vorher war beides nur ein namenloser Klickbereich.
                     */
                    .toggleable(
                        value = isSelected,
                        role = Role.Checkbox,
                        onValueChange = { onToggle(day) }
                    )
                    /*
                     * Sichtbar steht auf der Kachel nur ein Kuerzel, und die sind mehrdeutig: "T"
                     * meint Dienstag UND Donnerstag, "S" Samstag UND Sonntag. Vorgelesen war das
                     * wertlos. Der ausgeschriebene Name kommt aus java.time und ist damit ohne
                     * eigene Ressourcen in der Sprache des Geraets.
                     */
                    .semantics { contentDescription = day.getDisplayName(TextStyle.FULL, locale) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        dayLabels.getValue(day),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private val allDays: Set<DayOfWeek> = DayOfWeek.entries.toSet()

/** Lets the user tick every weekday in one go instead of tapping all seven circles below. */
@Composable
private fun SelectAllDaysRow(
    selected: Set<DayOfWeek>,
    onSelectedChange: (Set<DayOfWeek>) -> Unit,
    modifier: Modifier = Modifier
) {
    val allSelected = selected.size == allDays.size
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelectedChange(if (allSelected) emptySet() else allDays) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = allSelected,
            onCheckedChange = { checked -> onSelectedChange(if (checked) allDays else emptySet()) }
        )
        Text(stringResource(R.string.reminder_days_every), style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Miniatur der simulierten Matrix im Bearbeiten-Dialog: zeigt schwarz/aus, solange
 * keine Vorschau laeuft, und spielt sonst die gewaehlte Animation lokal ab (rein in
 * Compose - anders als der echte Trigger in ReminderAlarmReceiver, der die Animation
 * aufs Home-Screen-Widget spielt). [resolveFrames] loest sowohl fest eingebaute als
 * auch Bibliotheks-Animationen auf (siehe GlyphReminderViewModel.framesFor).
 */
@Composable
private fun AnimationPreview(
    choice: AnimationChoice?,
    resolveFrames: suspend (AnimationChoice) -> List<IntArray>,
    modifier: Modifier = Modifier
) {
    var frame by remember { mutableStateOf(emptyFrame) }
    LaunchedEffect(choice) {
        if (choice == null) {
            frame = emptyFrame
        } else {
            val frames = resolveFrames(choice)
            if (frames.isNotEmpty()) {
                MatrixAnimator.play(frames, frameDelayMs = MatrixAnimator.CLOCK_FRAME_DELAY_MS) { frame = it }
            }
            frame = emptyFrame
        }
    }
    SimulatedMatrixView(frame = frame, modifier = modifier.size(110.dp))
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ReminderDialog(
    initial: GlyphReminder?,
    initialDraft: ReminderDraft?,
    libraryAnimations: List<LibraryAnimation>,
    builtInTypes: Set<AnimationType>,
    resolveFrames: suspend (AnimationChoice) -> List<IntArray>,
    onManageLibrary: ((ReminderDraft) -> Unit)?,
    onDismiss: () -> Unit,
    onSave: (
        label: String,
        animationChoice: AnimationChoice,
        daysOfWeekMask: Int,
        startMinuteOfDay: Int,
        endMinuteOfDay: Int,
        intervalMinutes: Int,
        openDurationSeconds: Int,
        dailyGoal: Int
    ) -> Unit,
    onDelete: (() -> Unit)?
) {
    var label by rememberSaveable { mutableStateOf(initialDraft?.label ?: initial?.label ?: "") }
    val defaultChoice = builtInTypes.firstOrNull()?.let { AnimationChoice.BuiltIn(it) }
        ?: libraryAnimations.firstOrNull()?.let { AnimationChoice.Library(it.id, it.label, it.emoji) }
        ?: AnimationChoice.BuiltIn(AnimationType.GENERAL)
    var animationChoice by rememberSaveable(stateSaver = AnimationChoiceSaver) {
        mutableStateOf(initialDraft?.animationChoice ?: initial?.animationChoice(libraryAnimations) ?: defaultChoice)
    }
    var selectedDays by rememberSaveable(stateSaver = DaysOfWeekSaver) {
        mutableStateOf(
            initialDraft?.selectedDays
                ?: initial?.let { DaysOfWeekMask.toSet(it.daysOfWeekMask) }
                ?: emptySet()
        )
    }
    var startMinute by rememberSaveable { mutableStateOf(initialDraft?.startMinute ?: initial?.startMinuteOfDay ?: 9 * 60) }
    var endMinute by rememberSaveable { mutableStateOf(initialDraft?.endMinute ?: initial?.endMinuteOfDay ?: 18 * 60) }
    var interval by rememberSaveable { mutableStateOf(initialDraft?.interval ?: initial?.intervalMinutes ?: 15) }
    var dailyGoal by rememberSaveable { mutableStateOf(initial?.dailyGoal ?: NO_GOAL) }
    var openDuration by rememberSaveable {
        mutableStateOf(initial?.openDurationSeconds ?: ReminderOpenDuration.DEFAULT_SECONDS)
    }
    // Eine Offen-Dauer laenger als das eigene Intervall wuerde die Erinnerung noch zeigen,
    // waehrend ihre naechste Faelligkeit schon ansteht - siehe ReminderOpenDuration.coerceToInterval.
    // Greift bei jeder Intervall-Aenderung erneut, auch beim ersten Komponieren mit dem
    // Anfangswert, damit eine vorhandene (vor dieser Regel gespeicherte) Kombination beim
    // naechsten Bearbeiten automatisch korrigiert wird.
    LaunchedEffect(interval) {
        openDuration = ReminderOpenDuration.coerceToInterval(openDuration, interval)
    }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var previewChoice by remember { mutableStateOf<AnimationChoice?>(null) }

    val isValid = label.isNotBlank() && selectedDays.isNotEmpty() && startMinute != endMinute
    // Dieselbe Themen-Empfehlung wie im gefuehrten Avatar-Setup (siehe AssistantSetup) - dort
    // gibt es sie schon, hier im manuellen Dialog bisher nicht, obwohl er der Weg ist, den man
    // nach der Ersteinrichtung eher nutzt. Zeigt nur einen Vorschlag (Stern + Begruendung) an,
    // schraenkt die Auswahl aber bewusst nicht ein - der manuelle Dialog bleibt der Weg fuer die
    // volle Bandbreite.
    val rhythm = ReminderRhythm.forType(animationChoice.toStorage().first)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.reminder_new else R.string.reminder_edit)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    // Gleiche Grenze wie beim KI-Import (ReminderImport.MAX_LABEL_LENGTH) - sonst
                    // liesse sich hier von Hand genau der Karten-Ueberlauf erzeugen, den der
                    // Import bereits verhindert.
                    onValueChange = { if (it.length <= 40) label = it },
                    label = { Text(stringResource(R.string.reminder_label_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimationPreview(previewChoice, resolveFrames = resolveFrames)
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel(stringResource(R.string.reminder_animation))
                    AnimationPicker(
                        selected = animationChoice,
                        builtInTypes = builtInTypes,
                        libraryAnimations = libraryAnimations,
                        onSelect = { animationChoice = it },
                        onManageLibrary = onManageLibrary?.let {
                            {
                                it(ReminderDraft(label, animationChoice, selectedDays, startMinute, endMinute, interval))
                            }
                        }
                    )
                    OutlinedButton(
                        onClick = { previewChoice = animationChoice },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.reminder_play_preview))
                    }
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel(stringResource(R.string.reminder_days))
                    SelectAllDaysRow(selected = selectedDays, onSelectedChange = { selectedDays = it })
                    DayPicker(selected = selectedDays, onToggle = { day ->
                        selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                    })
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel(stringResource(R.string.reminder_time_window))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.reminder_from, formatMinuteOfDay(startMinute)))
                        }
                        OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.reminder_to, formatMinuteOfDay(endMinute)))
                        }
                    }
                    if (endMinute <= startMinute) {
                        Text(
                            stringResource(R.string.reminder_window_past_midnight),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (startMinute == endMinute) {
                        Text(
                            stringResource(R.string.reminder_window_same_time),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel(stringResource(R.string.reminder_interval))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        INTERVAL_OPTIONS.forEach { minutes ->
                            val text = stringResource(R.string.reminder_interval_minutes, minutes)
                            FilterChip(
                                selected = interval == minutes,
                                onClick = { interval = minutes },
                                label = {
                                    Text(
                                        if (minutes == rhythm.suggestedInterval) {
                                            stringResource(R.string.assistant_recommended, text)
                                        } else {
                                            text
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel(stringResource(R.string.reminder_daily_goal))
                    Text(
                        stringResource(R.string.reminder_daily_goal_explainer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Derselbe Begruendungssatz wie im gefuehrten Setup (siehe rhythm oben) -
                    // erklaert in einem Satz, warum genau dieser Rhythmus zum Thema passt, statt
                    // Ziel und Intervall unbegruendet nebeneinanderzustellen.
                    Text(
                        stringResource(rhythm.hintRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DAILY_GOAL_OPTIONS.forEach { goal ->
                            val text = if (goal == NO_GOAL) {
                                stringResource(R.string.reminder_daily_goal_none)
                            } else {
                                stringResource(R.string.reminder_daily_goal_times, goal)
                            }
                            FilterChip(
                                selected = dailyGoal == goal,
                                onClick = { dailyGoal = goal },
                                label = {
                                    Text(
                                        if (goal == rhythm.suggestedGoal) {
                                            stringResource(R.string.assistant_recommended, text)
                                        } else {
                                            text
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel(stringResource(R.string.reminder_open_duration))
                    Text(
                        stringResource(R.string.reminder_open_duration_explainer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Nie laenger waehlbar als das eigene Intervall (siehe LaunchedEffect oben) -
                    // "Nonstop" (UNTIL_FED) faellt damit immer heraus, es gibt keinen endlichen
                    // Vergleichswert, der es je einschliesst.
                    val availableOpenDurations = remember(interval) {
                        ReminderOpenDuration.OPTIONS.filter {
                            it != ReminderOpenDuration.UNTIL_FED && it <= interval * 60
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        availableOpenDurations.forEach { seconds ->
                            FilterChip(
                                selected = openDuration == seconds,
                                onClick = { openDuration = seconds },
                                label = { Text(ReminderOpenDuration.label(seconds)) }
                            )
                        }
                    }
                    if (openDuration > MAX_UNATTENDED_DURATION_SECONDS) {
                        Text(
                            stringResource(R.string.reminder_open_duration_cap),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (onDelete != null) {
                    HorizontalDivider()
                    if (confirmDelete) {
                        Text(
                            stringResource(R.string.reminder_really_delete),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    TextButton(
                        onClick = { if (confirmDelete) onDelete() else confirmDelete = true },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(if (confirmDelete) R.string.reminder_really_delete_action else R.string.reminder_delete))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    onSave(
                        label.trim(),
                        animationChoice,
                        DaysOfWeekMask.toMask(selectedDays),
                        startMinute,
                        endMinute,
                        interval,
                        openDuration,
                        dailyGoal
                    )
                }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )

    if (showStartPicker) {
        val state = rememberTimePickerState(
            initialHour = startMinute / 60,
            initialMinute = startMinute % 60,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startMinute = state.hour * 60 + state.minute
                    showStartPicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text(stringResource(R.string.action_cancel)) } },
            text = { TimePicker(state = state) }
        )
    }

    if (showEndPicker) {
        val state = rememberTimePickerState(
            initialHour = endMinute / 60,
            initialMinute = endMinute % 60,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endMinute = state.hour * 60 + state.minute
                    showEndPicker = false
                }) { Text(stringResource(R.string.action_ok)) }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text(stringResource(R.string.action_cancel)) } },
            text = { TimePicker(state = state) }
        )
    }
}

/**
 * Uebernimmt den Erinnerungs-Satz eines anderen Avatars.
 *
 * Zeigt bewusst die Anzahl je Avatar an: ohne sie waere nicht erkennbar, welcher Avatar
 * ueberhaupt etwas zu kopieren hat. Avatare ohne Erinnerungen und der aktive selbst sind
 * deaktiviert.
 */
@Composable
private fun CopyRemindersDialog(
    activeSpecies: AvatarSpecies,
    loadCounts: suspend () -> Map<AvatarSpecies, Int>,
    onCopy: (source: AvatarSpecies, replace: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var counts by remember { mutableStateOf<Map<AvatarSpecies, Int>>(emptyMap()) }
    var selected by remember { mutableStateOf<AvatarSpecies?>(null) }
    var replace by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { counts = loadCounts() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminders_copy_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    stringResource(R.string.reminders_copy_explainer, stringResource(activeSpecies.labelRes)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AvatarSpecies.entries.forEach { species ->
                    val count = counts[species] ?: 0
                    val selectable = species != activeSpecies && count > 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = selectable) { selected = species }
                            .padding(8.dp)
                            .alpha(if (selectable) 1f else 0.4f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RadioButton(
                            selected = selected == species,
                            onClick = { selected = species },
                            enabled = selectable
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(species.labelRes), style = MaterialTheme.typography.titleSmall)
                            Text(
                                when {
                                    species == activeSpecies -> stringResource(R.string.reminders_copy_current)
                                    count == 0 -> stringResource(R.string.reminder_days_none)
                                    count == 1 -> "1 " + stringResource(R.string.reminders_copy_count_one)
                                    else -> "$count " + stringResource(R.string.reminders_copy_count_other)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.reminders_copy_replace))
                        Text(
                            if (replace) {
                                stringResource(
                                    R.string.reminders_copy_replace_hint,
                                    stringResource(activeSpecies.labelRes)
                                )
                            } else {
                                stringResource(R.string.reminders_copy_append_hint)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (replace) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                    Switch(checked = replace, onCheckedChange = { replace = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selected != null,
                onClick = { selected?.let { onCopy(it, replace) } }
            ) { Text(stringResource(R.string.reminders_copy)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

