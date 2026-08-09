package com.notime.glyphkalender.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.DaysOfWeekMask
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphcore.data.LibraryAnimation
import com.notime.glyphkalender.glyph.ReminderPlayback
import com.notime.glyphkalender.matrix.MatrixGeometry
import com.notime.glyphkalender.matrix.MatrixPreviewView
import com.notime.glyphkalender.matrix.PreviewAnimator
import java.time.DayOfWeek

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

private val intervalOptions = listOf(1, 5, 10, 15, 20, 30, 45, 60, 90, 120)

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
private data class ReminderDraft(
    val label: String,
    val animationChoice: AnimationChoice,
    val selectedDays: Set<DayOfWeek>,
    val startMinute: Int,
    val endMinute: Int,
    val interval: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(viewModel: GlyphReminderViewModel = viewModel()) {
    var showLibrary by remember { mutableStateOf(false) }
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val libraryAnimations by viewModel.libraryAnimations.collectAsStateWithLifecycle()
    val selectedLibraryAnimations = libraryAnimations.filter { it.isSelected }
    val builtInSelections by viewModel.builtInSelections.collectAsStateWithLifecycle()
    val selectedBuiltInTypes = builtInSelections
        .filter { it.isSelected }
        .mapNotNull { runCatching { AnimationType.valueOf(it.animationType) }.getOrNull() }
        .toSet()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<GlyphReminder?>(null) }
    var addDraft by remember { mutableStateOf<ReminderDraft?>(null) }
    var editDraft by remember { mutableStateOf<ReminderDraft?>(null) }
    val context = LocalContext.current
    var exactAlarmsAllowed by remember { mutableStateOf(viewModel.canScheduleExactAlarms()) }
    var notificationsAllowed by remember { mutableStateOf(hasNotificationPermission(context)) }
    val nowPlaying by ReminderPlayback.nowPlaying.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    /*
     * Nach der Rueckkehr aus den Systemeinstellungen kann sich beides geaendert haben, ohne dass
     * die App etwas davon mitbekommt - deshalb bei jedem ON_RESUME neu nachsehen statt einmal
     * beim Aufbau. Sonst behauptet der Hinweis weiter, die Berechtigung fehle, obwohl der Nutzer
     * sie gerade erteilt hat.
     */
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                exactAlarmsAllowed = viewModel.canScheduleExactAlarms()
                notificationsAllowed = hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    /*
     * Der Systemdialog erscheint nur, solange der Nutzer nicht endgueltig abgelehnt hat. Danach
     * kommt der Aufruf sofort mit "abgelehnt" zurueck, ohne dass irgendetwas zu sehen war - ein
     * Knopf, der nichts tut. In genau diesem Fall bleibt nur der Umweg ueber die
     * Systemeinstellungen, und der wird hier gegangen.
     *
     * Unterscheiden lassen sich die beiden Faelle an `shouldShowRequestPermissionRationale`: nach
     * einer endgueltigen Ablehnung ist es false.
     */
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsAllowed = granted
        val activity = context.findActivity()
        if (!granted && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, POST_NOTIFICATIONS)
        ) {
            context.openAppNotificationSettings()
        }
    }
    val requestNotifications = { permissionLauncher.launch(POST_NOTIFICATIONS) }

    if (showLibrary) {
        LibraryScreen(onBack = { showLibrary = false }, viewModel = viewModel)
    } else {
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Reminders") },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("New") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = { showAddDialog = true }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!exactAlarmsAllowed) {
                ExactAlarmBanner(
                    onClick = { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)) }
                )
            }

            if (!notificationsAllowed) {
                NotificationPermissionBanner(onClick = requestNotifications)
            }

            // Steht ueber der Liste, solange wirklich etwas laeuft - der von der
            // Benachrichtigung unabhaengige Weg zum Beenden (siehe ReminderPlayback).
            nowPlaying?.let { title ->
                NowPlayingBanner(
                    title = title,
                    onStop = { ReminderPlayback.dismiss(context) }
                )
            }

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
                            onClick = { editingReminder = reminder },
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
            onSave = { label, choice, days, start, end, interval ->
                viewModel.addReminder(label, choice, days, start, end, interval)
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
            onDismiss = { editingReminder = null; editDraft = null },
            onSave = { label, choice, days, start, end, interval ->
                val (animationType, libraryAnimationId) = choice.toStorage()
                viewModel.updateReminder(
                    reminder.copy(
                        label = label,
                        animationType = animationType,
                        libraryAnimationId = libraryAnimationId,
                        daysOfWeekMask = days,
                        startMinuteOfDay = start,
                        endMinuteOfDay = end,
                        intervalMinutes = interval
                    )
                )
                editingReminder = null
                editDraft = null
            },
            onDelete = {
                viewModel.deleteReminder(reminder)
                editingReminder = null
                editDraft = null
            }
        )
    }
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
            "No reminders yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Create a reminder, e.g. \"Drink\" from 9:00 to 18:00 every 15 minutes.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onAddClick) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Create first reminder")
        }
    }
}

/** `POST_NOTIFICATIONS` ist seit Android 13 eine Laufzeitberechtigung; minSdk ist hier 34. */
private const val POST_NOTIFICATIONS = Manifest.permission.POST_NOTIFICATIONS

private fun hasNotificationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/**
 * Die Activity hinter einem Compose-Context.
 *
 * `LocalContext` ist in Compose nicht zwingend die Activity selbst, sondern kann ein
 * [ContextWrapper] darum sein - deshalb die Schleife statt einer Umwandlung, die je nach
 * Aufrufort still fehlschlaegt.
 */
private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/** Direkt zu den Benachrichtigungseinstellungen dieser App, nicht in die allgemeine Liste. */
private fun Context.openAppNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    // Sollte ein Hersteller diese Ansicht nicht anbieten, lieber auf die App-Detailseite
    // ausweichen als den Nutzer mit einer ActivityNotFoundException stehen zu lassen.
    val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.fromParts("package", packageName, null))
    runCatching { startActivity(intent) }.onFailure { runCatching { startActivity(fallback) } }
}

/**
 * Hinweis auf die fehlende Benachrichtigungsberechtigung.
 *
 * **Warum ein Hinweis und nicht sofort der Systemdialog beim Start.** Der Systemdialog erklaert
 * nichts - er fragt. Wer nicht weiss, wofuer die Berechtigung gebraucht wird, lehnt ab, und beim
 * zweiten Mal fragt Android gar nicht mehr. Der Grund steht deshalb hier, sichtbar und dauerhaft,
 * und der Dialog kommt erst auf Antippen.
 *
 * Der Grund ist auch ein konkreter: ohne Benachrichtigung fehlt der "Beenden"-Knopf zur laufenden
 * Erinnerung. Die Erinnerungen selbst erscheinen weiterhin - sie laufen ueber die Glyph-Matrix,
 * nicht ueber Benachrichtigungen. Genau das sagt der Text, damit niemand eine Berechtigung
 * erteilt, weil er sonst Funktionsverlust befuerchtet, den es nicht gibt.
 */
@Composable
private fun NotificationPermissionBanner(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Notifications are off. Reminders still play on the Glyph Matrix – but the " +
                    "\"Stop\" button that ends one early lives in a notification. Tap to allow.",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Sichtbar, solange eine Erinnerung laeuft - mit demselben "Beenden" wie in der Benachrichtigung.
 *
 * Das ist der Weg, der auch ohne Benachrichtigungsberechtigung bleibt. Er ersetzt sie nicht
 * vollstaendig (man muss die App dafuer offen haben), aber er sorgt dafuer, dass es ueberhaupt
 * einen gibt - vorher war die Benachrichtigung der einzige.
 */
@Composable
private fun NowPlayingBanner(title: String, onStop: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Playing: $title",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            TextButton(onClick = onStop) { Text("Stop") }
        }
    }
}

@Composable
private fun ExactAlarmBanner(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Exact alarms aren't allowed – animations won't show up on time. " +
                    "Tap to enable.",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
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
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                Text(reminder.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "$timeLabel · every ${reminder.intervalMinutes} min",
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
                    Icon(Icons.Default.Add, contentDescription = "More animations")
                }
                Spacer(Modifier.height(4.dp))
                Text("More", style = MaterialTheme.typography.labelSmall, maxLines = 1)
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

/**
 * Miniatur der Glyph Matrix direkt im Handy-Display (siehe matrix/MatrixPreviewView.kt) -
 * damit man beim Anlegen/Testen eines Reminders nicht das Handy umdrehen muss, um die
 * echte Matrix auf der Rueckseite zu sehen. Reine Vorschau, loest NICHT die echte
 * Hardware aus (die spielt die Animation weiterhin unabhaengig davon, wenn ein
 * Reminder tatsaechlich faellig wird, siehe glyph/ReminderGlyphService.kt).
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
            if (frames.isNotEmpty()) PreviewAnimator.play(frames) { frame = it }
            frame = emptyFrame
        }
    }
    MatrixPreviewView(frame = frame, modifier = modifier.size(110.dp))
}

@Composable
private fun DayPicker(
    selected: Set<DayOfWeek>,
    onToggle: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DayOfWeek.entries.forEach { day ->
            val isSelected = day in selected
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clickable { onToggle(day) }
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
        Text("Every day", style = MaterialTheme.typography.bodyMedium)
    }
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
        intervalMinutes: Int
    ) -> Unit,
    onDelete: (() -> Unit)?
) {
    var label by remember { mutableStateOf(initialDraft?.label ?: initial?.label ?: "") }
    val defaultChoice = builtInTypes.firstOrNull()?.let { AnimationChoice.BuiltIn(it) }
        ?: libraryAnimations.firstOrNull()?.let { AnimationChoice.Library(it.id, it.label, it.emoji) }
        ?: AnimationChoice.BuiltIn(AnimationType.GENERAL)
    var animationChoice by remember {
        mutableStateOf(initialDraft?.animationChoice ?: initial?.animationChoice(libraryAnimations) ?: defaultChoice)
    }
    var previewChoice by remember { mutableStateOf<AnimationChoice?>(null) }
    var selectedDays by remember {
        mutableStateOf(
            initialDraft?.selectedDays
                ?: initial?.let { DaysOfWeekMask.toSet(it.daysOfWeekMask) }
                ?: emptySet()
        )
    }
    var startMinute by remember { mutableStateOf(initialDraft?.startMinute ?: initial?.startMinuteOfDay ?: 9 * 60) }
    var endMinute by remember { mutableStateOf(initialDraft?.endMinute ?: initial?.endMinuteOfDay ?: 18 * 60) }
    var interval by remember { mutableStateOf(initialDraft?.interval ?: initial?.intervalMinutes ?: 15) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val isValid = label.isNotBlank() && selectedDays.isNotEmpty() && startMinute != endMinute

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New reminder" else "Edit reminder") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label, e.g. \"Drink\"") },
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
                    SectionLabel("Animation")
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
                        Text("Play preview")
                    }
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel("Days of week")
                    SelectAllDaysRow(selected = selectedDays, onSelectedChange = { selectedDays = it })
                    DayPicker(selected = selectedDays, onToggle = { day ->
                        selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                    })
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel("Time window")
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { showStartPicker = true }, modifier = Modifier.weight(1f)) {
                            Text("From ${formatMinuteOfDay(startMinute)}")
                        }
                        OutlinedButton(onClick = { showEndPicker = true }, modifier = Modifier.weight(1f)) {
                            Text("To ${formatMinuteOfDay(endMinute)}")
                        }
                    }
                    if (endMinute <= startMinute) {
                        Text(
                            "Window goes past midnight (ends the next day).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (startMinute == endMinute) {
                        Text(
                            "Start and end time must be different.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel("Interval")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        intervalOptions.forEach { minutes ->
                            FilterChip(
                                selected = interval == minutes,
                                onClick = { interval = minutes },
                                label = { Text("$minutes min") }
                            )
                        }
                    }
                }

                if (onDelete != null) {
                    HorizontalDivider()
                    if (confirmDelete) {
                        Text(
                            "Really delete?",
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
                        Text(if (confirmDelete) "Really delete" else "Delete reminder")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = {
                    onSave(label.trim(), animationChoice, DaysOfWeekMask.toMask(selectedDays), startMinute, endMinute, interval)
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showStartPicker = false }) { Text("Cancel") } },
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
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showEndPicker = false }) { Text("Cancel") } },
            text = { TimePicker(state = state) }
        )
    }
}

// Hier stand bis zuletzt ein "Settings"-Dialog mit genau einer Einstellung: wie mit mehreren
// gleichzeitig faelligen Erinnerungen umzugehen sei ("Back-to-back" vs. "Spread out").
//
// Er ist ersatzlos entfallen, weil er den Nutzer angelogen hat. Das Auseinanderziehen
// kollidierender Erinnerungen wurde im Planer abgeschafft - es rueckte sie von dem Zeitpunkt weg,
// fuer den sie gestellt waren (aus "alle fuenf Minuten" wurde faktisch "alle acht"). An seine
// Stelle trat eine feste Reihenfolge beim Ausloesen, ohne einen Zeitpunkt anzutasten. Der Schalter
// blieb stehen, schrieb weiter Werte in die Einstellungen - und niemand las sie mehr.
//
// Ein Schalter, der sichtbar umspringt und nichts bewirkt, ist schlimmer als kein Schalter: er
// laesst den Nutzer glauben, er haette das Verhalten geaendert. Die zugehoerige `CollisionPrefs`
// im :core-Modul ist mitentfallen.
//
// Die Begruendung im Planer selbst steht in ReminderScheduler.kt, die Reihenfolge beim Ausloesen
// in ReminderTrigger.firePending (:app-sim).
