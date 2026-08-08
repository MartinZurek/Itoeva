package com.notime.glyphsim.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notime.glyphcore.data.LibraryAnimation
import com.notime.glyphcore.data.NO_GOAL
import com.notime.glyphsim.R
import org.json.JSONObject

private val SHEET_BG = Color(0xFF101012)
private val ROW_BG = Color(0xFF1A1A1D)
private val TEXT_PRIMARY = Color(0xFFF1EEE6)
private val TEXT_MUTED = Color(0xFF9A968E)

/**
 * Import aus einem KI-Gespraech: Vorlage kopieren, Antwort einfuegen, Vorschau bestaetigen.
 *
 * **Warum Einfuegen statt Anbindung:** ein ChatGPT-Plus- oder Claude-Pro-Abo enthaelt keinen
 * API-Zugang, beides sind getrennte Produkte mit getrennter Abrechnung. Eine App kann das Abo
 * eines Nutzers also gar nicht mitbenutzen. Ueber die Zwischenablage fuehrt er das Gespraech
 * dort, wo er ohnehin bezahlt hat - die App verschickt dabei nichts, was ihr im Play Store die
 * Offenlegung einer Weitergabe an Dritte erspart.
 *
 * Die Vorschau ist Pflicht, nicht Zierde: was hier ankommt, ist von einem Sprachmodell erzeugter
 * Text. Angelegt wird erst nach Bestaetigung, und Korrekturen an unbrauchbaren Werten werden
 * offen ausgewiesen (siehe [ReminderImport]).
 */
@Composable
fun ReminderImportDialog(
    onDismiss: () -> Unit,
    /**
     * Vorbelegung aus der Teilen-Funktion. Ist sie gesetzt, ist die Vorschau sofort da und der
     * Kopieren-Schritt entfaellt - das Gespraech hat ja bereits stattgefunden.
     */
    initialText: String? = null
) {
    val context = LocalContext.current
    val viewModel = viewModel<GlyphReminderViewModel>()
    val libraryAnimations by viewModel.libraryAnimations.collectAsState()
    var input by remember { mutableStateOf(initialText.orEmpty()) }
    var preview by remember { mutableStateOf(initialText?.let { parseReminders(it, libraryAnimations) } ?: emptyList()) }
    // Auch bei Vorbelegung aus der Teilen-Funktion melden, wenn nichts dabei herauskam - sonst
    // wirkt der Dialog beim Teilen eines nicht auswertbaren Links (siehe looksLikeShareLink)
    // einfach nur leer, ohne jeden Hinweis, was schiefging.
    var parseFailed by remember { mutableStateOf(!initialText.isNullOrBlank() && preview.isEmpty()) }
    var saved by remember { mutableStateOf(false) }

    val basePromptTemplate = stringResource(R.string.import_prompt_template)
    val libraryIntro = stringResource(R.string.import_prompt_library_intro)
    // Die eigenen Animationen sind pro Nutzer verschieden und koennen deshalb nicht Teil der
    // uebersetzten Vorlage sein - die Liste wird hier an den statischen Text angehaengt, nur
    // wenn ueberhaupt welche existieren.
    val promptTemplate = remember(basePromptTemplate, libraryIntro, libraryAnimations) {
        if (libraryAnimations.isEmpty()) {
            basePromptTemplate
        } else {
            val list = libraryAnimations.joinToString("\n") { "- ${it.emoji} ${it.label}" }
            "$basePromptTemplate\n\n$libraryIntro\n$list"
        }
    }

    // Beim allerersten Aufruf ueber die Teilen-Funktion kann [libraryAnimations] noch den
    // anfaenglichen leeren Wert tragen (die Room-Abfrage laeuft erst asynchron an) - sobald die
    // echten Daten da sind, wird hier mit dem bereits eingegebenen Text neu geprueft, statt die
    // Vorschau auf dem veralteten Stand (ohne Bibliotheks-Treffer) stehen zu lassen.
    LaunchedEffect(libraryAnimations) {
        if (input.isNotBlank()) {
            preview = parseReminders(input, libraryAnimations)
            parseFailed = preview.isEmpty()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SHEET_BG,
        titleContentColor = TEXT_PRIMARY,
        textContentColor = TEXT_PRIMARY,
        title = { Text(stringResource(R.string.import_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (saved) {
                    Text(stringResource(R.string.import_saved, preview.size))
                    return@Column
                }

                // Kam der Text bereits ueber die Teilen-Funktion herein, ist das Gespraech schon
                // gefuehrt - die Anleitung zum Kopieren waere dann nur noch Ballast.
                if (initialText == null) {
                    Text(
                        stringResource(R.string.import_step_1),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TEXT_MUTED
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { copyToClipboard(context, promptTemplate) }) {
                            Text(stringResource(R.string.import_copy_prompt))
                        }
                        // Direkter Weg an ChatGPT/Claude vorbei am Umweg ueber die Zwischenablage:
                        // beide Apps nehmen geteilten Text als vorausgefuellte neue Nachricht an,
                        // der Nutzer muss also nicht mehr selbst wechseln und einfuegen.
                        TextButton(onClick = { shareText(context, promptTemplate) }) {
                            Text(stringResource(R.string.import_share_prompt))
                        }
                    }
                    Text(
                        stringResource(R.string.import_step_2),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TEXT_MUTED
                    )
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        parseFailed = false
                        preview = parseReminders(it, libraryAnimations)
                        // Nur melden, wenn wirklich etwas eingegeben wurde - sonst stuende der
                        // Fehler schon vor der ersten Eingabe da.
                        if (it.isNotBlank() && preview.isEmpty()) parseFailed = true
                    },
                    label = { Text(stringResource(R.string.import_paste_hint)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (parseFailed) {
                    Text(
                        // Ein geteilter Link (statt der eigentlichen Antwort) ist der mit Abstand
                        // wahrscheinlichste Grund fuer einen leeren Import - dafuer eine gezielte
                        // Erklaerung statt der generischen Meldung, die nur "kein JSON gefunden"
                        // sagt, ohne dass ersichtlich ist, warum.
                        stringResource(
                            if (looksLikeShareLink(input)) {
                                R.string.import_parse_failed_link
                            } else {
                                R.string.import_parse_failed
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                preview.forEach { reminder ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ROW_BG)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            "${reminder.animationChoice.emoji} ${reminder.label}",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val goalText = if (reminder.dailyGoal == NO_GOAL) {
                            stringResource(R.string.reminder_daily_goal_none)
                        } else {
                            stringResource(R.string.import_preview_goal_times, reminder.dailyGoal)
                        }
                        Text(
                            stringResource(
                                R.string.import_preview_line,
                                goalText,
                                formatMinutes(reminder.startMinuteOfDay),
                                formatMinutes(reminder.endMinuteOfDay),
                                reminder.intervalMinutes
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = TEXT_MUTED
                        )
                        // Zurechtgerueckte Werte werden ausgewiesen statt still uebernommen -
                        // sonst legte die App etwas an, das der Nutzer so nie bestaetigt hat.
                        reminder.corrections.forEach { correction ->
                            Text(
                                "· $correction",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE0B341)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (saved || preview.isEmpty()) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) }
            } else {
                TextButton(onClick = {
                    viewModel.addReminders(preview)
                    saved = true
                }) {
                    Text(stringResource(R.string.import_confirm, preview.size))
                }
            }
        },
        dismissButton = {
            if (!saved) TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

private fun formatMinutes(minuteOfDay: Int): String =
    "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)

/**
 * Erkennt einen geteilten Konversations-Link (statt der eigentlichen Antwort) - der "Teilen"-
 * Knopf in Claude/ChatGPT verschickt oft nur eine URL zum ganzen Gespraech, nicht dessen Text.
 * Ohne diese Unterscheidung saehe der Nutzer nur die generische "kein JSON gefunden"-Meldung,
 * ohne zu erkennen, dass er die falsche Sache geteilt hat (siehe import_parse_failed_link).
 */
private fun looksLikeShareLink(text: String): Boolean {
    val trimmed = text.trim()
    return (trimmed.startsWith("http://") || trimmed.startsWith("https://")) && !trimmed.contains("{")
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Tama", text))
}

private fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

/**
 * Liest die Erinnerungen aus einer KI-Antwort.
 *
 * Jede Ausnahme wird geschluckt und als "nichts gefunden" gewertet: die Eingabe ist beliebiger
 * Text aus der Zwischenablage, ein Absturz waere hier die schlechteste aller Reaktionen.
 * Einzelne unbrauchbare Eintraege werden uebersprungen, statt den ganzen Import zu verwerfen.
 */
private fun parseReminders(text: String, libraryAnimations: List<LibraryAnimation>): List<SanitizedReminder> {
    val json = ReminderImport.extractJson(text) ?: return emptyList()
    return try {
        val root = JSONObject(json)
        val array = root.optJSONArray("reminders") ?: return emptyList()
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val days = item.optJSONArray("days")?.let { list ->
                (0 until list.length()).mapNotNull { list.optString(it).takeIf(String::isNotBlank) }
            }
            ReminderImport.sanitize(
                RawImportedReminder(
                    label = item.optString("label").takeIf { it.isNotBlank() },
                    topic = item.optString("topic").takeIf { it.isNotBlank() },
                    libraryAnimation = item.optString("libraryAnimation").takeIf { it.isNotBlank() },
                    dailyGoal = item.optInt("dailyGoal").takeIf { item.has("dailyGoal") },
                    intervalMinutes = item.optInt("intervalMinutes").takeIf { item.has("intervalMinutes") },
                    start = item.optString("start").takeIf { it.isNotBlank() },
                    end = item.optString("end").takeIf { it.isNotBlank() },
                    days = days
                ),
                libraryAnimations = libraryAnimations
            )
        }
    } catch (e: Exception) {
        emptyList()
    }
}
