package com.notime.glyphsim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notime.glyphsim.data.AppDatabase
import kotlinx.coroutines.flow.map

/**
 * Zeigt [SkillTreeScreen] als Dialog und holt dazu Level und XP-Rest.
 *
 * Getrennt vom Bildschirm selbst, damit der ohne Datenbank auskommt: [SkillTreeScreen] bekommt
 * fertige Werte und laesst sich dadurch in einer Vorschau oder einem Test zeigen, ohne dass ein
 * Spielstand existieren muss.
 */
@Composable
fun SkillTreeDialog(
    unlocked: Set<String>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val profileId = PresentCompanion.profileId(context)
    // `remember`, weil `map` sonst bei jeder Recomposition einen neuen Flow baut und das
    // Sammeln dadurch immer wieder von vorn beginnt (Lint: FlowOperatorInvokedInComposition).
    val xpFlow = remember(context, profileId) {
        AppDatabase.getInstance(context).avatarPlayStateDao()
            .observeForProfile(profileId)
            .map { it?.xp ?: 0 }
    }
    val xp by xpFlow.collectAsStateWithLifecycle(initialValue = 0)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        SkillTreeScreen(
            unlocked = unlocked,
            level = PlayModeXp.levelFor(xp),
            // Wieviel bis zum naechsten Aufstieg fehlt - der Rest bis zur naechsten vollen Stufe.
            xpToNextLevel = PlayModeXp.XP_PER_LEVEL - (xp % PlayModeXp.XP_PER_LEVEL),
            onClose = onDismiss,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(20.dp))
                .background(TamaPalette.Background)
        )
    }
}
