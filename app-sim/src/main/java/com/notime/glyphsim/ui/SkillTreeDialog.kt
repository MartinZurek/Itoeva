package com.notime.glyphsim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.skilltree.AvatarUnlockRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Zeigt [SkillTreeScreen] als Dialog und holt dazu Freischalt-Stand, Level und XP-Rest.
 *
 * Getrennt vom Bildschirm selbst, damit der ohne Datenbank auskommt: [SkillTreeScreen] bekommt
 * fertige Werte und laesst sich dadurch in einer Vorschau oder einem Test zeigen, ohne dass ein
 * Spielstand existieren muss.
 *
 * Das Skill-Animationslabor wird nur in einer debuggable APK eingeblendet. Damit bleibt es in den
 * Entwickler-APKs erreichbar, die per `assembleDebug` ausgeliefert werden, verschwindet aber
 * automatisch aus einem normalen Release-Build fuer Spieler.
 */
@Composable
fun SkillTreeDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val profileId = PresentCompanion.profileId(context)
    val species = remember(context) { AvatarSpeciesPrefs.get(context) }
    val repository = remember(context) { AvatarUnlockRepository(AppDatabase.getInstance(context)) }
    val scope = rememberCoroutineScope()
    val showDeveloperLab = remember(context) {
        context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    val unlocked by repository.observeUnlockedNodes(profileId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val xpFlow = remember(context, profileId) {
        AppDatabase.getInstance(context).avatarPlayStateDao()
            .observeForProfile(profileId)
            .map { it?.xp ?: 0 }
    }
    val xp by xpFlow.collectAsStateWithLifecycle(initialValue = 0)
    val level = PlayModeXp.levelFor(xp)

    var busy by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(!OnboardingPrefs.hasSeenSkillTreeHint(context)) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        SkillTreeScreen(
            unlocked = unlocked.toSet(),
            level = level,
            xpToNextLevel = PlayModeXp.XP_PER_LEVEL - (xp % PlayModeXp.XP_PER_LEVEL),
            species = species,
            onUnlock = { nodeId ->
                if (busy) return@SkillTreeScreen
                busy = true
                scope.launch {
                    repository.unlock(profileId, nodeId, System.currentTimeMillis())
                    busy = false
                }
            },
            onClose = onDismiss,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(20.dp))
                .background(TamaPalette.Background),
            showHint = showHint,
            onDismissHint = {
                showHint = false
                OnboardingPrefs.markSkillTreeHintSeen(context)
            },
            showDeveloperLab = showDeveloperLab
        )
    }
}
