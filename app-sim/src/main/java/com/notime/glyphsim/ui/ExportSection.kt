package com.notime.glyphsim.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notime.glyphsim.R
import com.notime.glyphsim.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * **Die eigenen Erinnerungen mitnehmen** - der Abschnitt im Gespraech, der [ReminderExport]
 * sichtbar macht.
 *
 * Bewusst direkt neben dem Import: Beides ist derselbe Vorgang, nur in die andere Richtung, und
 * beide sprechen dasselbe Format. Wer den Export kennt, weiss damit auch, wie er wieder
 * hereinkommt.
 *
 * ## Warum Zwischenablage und Teilen statt einer Datei
 *
 * Eine Datei waere der naheliegende Weg - und der schlechtere. Der Rueckweg fuehrt durch den
 * bestehenden Import, und der nimmt Text entgegen: aus der Zwischenablage oder als geteilten
 * Inhalt (siehe [ReminderImportDialog]). Eine Datei muesste man erst wieder in Text verwandeln,
 * und ob eine Dateiverwaltung das anbietet, haengt vom Geraet ab.
 *
 * Text geht ueberall hin: in eine Notiz, in eine Mail an sich selbst, in eine Cloud - und von
 * dort ohne Umweg zurueck. Ausserdem bleibt er lesbar, was bei einer Sicherung kein Nebenaspekt
 * ist: Man kann nachsehen, was mitgeht, statt es glauben zu muessen.
 */
@Composable
fun AssistantExport(onBack: () -> Unit) {
    val context = LocalContext.current
    var ergebnis by remember { mutableStateOf<Ergebnis?>(null) }
    var kopiert by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ergebnis = withContext(Dispatchers.IO) {
            val db = AppDatabase.getInstance(context)
            // getUserRemindersForProfile laesst die Spiel-Zeile bereits aussen vor; ReminderExport
            // filtert sie zusaetzlich, damit die Zusage auch dann gilt, wenn hier je eine andere
            // Abfrage stuende.
            val reminders = db.glyphReminderDao()
                .getUserRemindersForProfile(RoutineOwner.current(context))
            Ergebnis(
                anzahl = reminders.size,
                text = ReminderExport.toJson(reminders, db.libraryAnimationDao().observeAll().first())
            )
        }
    }

    when (val fertig = ergebnis) {
        null -> Bubble(stringResource(R.string.talk_loading))
        // Kein Vorwurf, nur eine Feststellung - wer noch nichts eingerichtet hat, hat auch nichts
        // versaeumt.
        else -> if (fertig.anzahl == 0) {
            Bubble(stringResource(R.string.export_empty))
        } else {
            Bubble(stringResource(R.string.export_intro))
            // Was NICHT mitgeht, gehoert an dieselbe Stelle wie das, was mitgeht - sonst merkt es
            // jemand erst auf dem neuen Geraet (siehe ReminderExport).
            Bubble(stringResource(R.string.export_scope))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = {
                    copyToClipboard(context, fertig.text)
                    kopiert = true
                }) { Text(stringResource(R.string.export_copy)) }
                TextButton(onClick = { shareText(context, fertig.text) }) {
                    Text(stringResource(R.string.export_share))
                }
            }
            if (kopiert) {
                Text(
                    stringResource(R.string.export_copied),
                    style = MaterialTheme.typography.bodySmall,
                    color = TamaPalette.TextMuted
                )
            }
        }
    }
    Choice(stringResource(R.string.action_back), onBack)
}

/**
 * Text UND Anzahl - die Anzahl laesst sich dem fertigen Text nicht mehr zuverlaessig ansehen,
 * und "hast du ueberhaupt etwas" ist die erste Frage, die dieser Bildschirm beantworten muss.
 */
private data class Ergebnis(val anzahl: Int, val text: String)
