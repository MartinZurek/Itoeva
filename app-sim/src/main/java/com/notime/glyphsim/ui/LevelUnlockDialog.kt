package com.notime.glyphsim.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.notime.glyphcore.data.AnimationNode
import com.notime.glyphsim.R
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.skilltree.AvatarUnlockRepository
import com.notime.glyphsim.skilltree.LevelUnlocks
import com.notime.glyphsim.skilltree.UnlockOffer
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

/**
 * Zwingt keine schnelle Entscheidung, laesst die verdiente Wahl aber auch nicht verschwinden.
 * Der Dialog hat deshalb bewusst keinen Schliessen-Knopf. Wird die App beendet, ergibt
 * [LevelUnlocks] beim naechsten Start dieselbe noch offene Freischaltung.
 */
@Composable
fun LevelUnlockDialog(
    profileId: String,
    level: Int,
    onAllChosen: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { AvatarUnlockRepository(AppDatabase.getInstance(context)) }
    val scope = rememberCoroutineScope()
    var offer by remember(profileId, level) { mutableStateOf<UnlockOffer?>(null) }
    var remaining by remember(profileId, level) { mutableStateOf(0) }
    var choosing by remember(profileId, level) { mutableStateOf(false) }

    suspend fun refresh() {
        repository.ensureSeeded(profileId, System.currentTimeMillis())
        val unlocked = repository.unlockedNodes(profileId)
        remaining = LevelUnlocks.due(level, unlocked)
        if (remaining == 0) {
            offer = null
            onAllChosen()
            return
        }
        val next = repository.offerFor(profileId, System.currentTimeMillis())
        if (next.isEmpty) {
            // Der vollstaendige Baum ist offen. Hoehere Level duerfen dann nicht in einem leeren
            // Dialog haengen bleiben.
            offer = null
            onAllChosen()
        } else {
            offer = next
        }
    }

    LaunchedEffect(profileId, level) { refresh() }

    val current = offer ?: return
    AlertDialog(
        onDismissRequest = {},
        containerColor = TamaPalette.SheetBackground,
        titleContentColor = TamaPalette.TextPrimary,
        textContentColor = TamaPalette.TextPrimary,
        title = { Text(stringResource(R.string.level_unlock_title, level)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.level_unlock_intro),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (current.focused.isNotEmpty()) {
                    Text(
                        stringResource(R.string.level_unlock_focused),
                        style = MaterialTheme.typography.labelMedium,
                        color = TamaPalette.TextMuted
                    )
                    current.focused.forEach { node ->
                        UnlockChoice(node, choosing) {
                            choosing = true
                            scope.launch {
                                repository.unlock(profileId, node.id, System.currentTimeMillis())
                                choosing = false
                                refresh()
                            }
                        }
                    }
                }
                current.wildcard?.let { node ->
                    Text(
                        stringResource(R.string.level_unlock_wildcard),
                        style = MaterialTheme.typography.labelMedium,
                        color = TamaPalette.TextMuted
                    )
                    UnlockChoice(node, choosing) {
                        choosing = true
                        scope.launch {
                            repository.unlock(profileId, node.id, System.currentTimeMillis())
                            choosing = false
                            refresh()
                        }
                    }
                }
                if (remaining > 1) {
                    Text(
                        stringResource(R.string.level_unlock_remaining, remaining),
                        style = MaterialTheme.typography.labelSmall,
                        color = TamaPalette.TextMuted
                    )
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
private fun UnlockChoice(node: AnimationNode, disabled: Boolean, onClick: () -> Unit) {
    Choice("${node.emoji}  ${node.rememberTitle()}") {
        if (!disabled) onClick()
    }
}
