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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map

/**
 * Zeigt [SkillTreeScreen] als Dialog und holt dazu Freischalt-Stand, Level und XP-Rest.
 *
 * Getrennt vom Bildschirm selbst, damit der ohne Datenbank auskommt: [SkillTreeScreen] bekommt
 * fertige Werte und laesst sich dadurch in einer Vorschau oder einem Test zeigen, ohne dass ein
 * Spielstand existieren muss.
 *
 * **Liest den Freischalt-Stand selbst**, statt ihn vom Aufrufer durchgereicht zu bekommen: Fuer
 * das Brett zaehlt der VOLLE Stand (auch ein Knoten ohne Motiv waere ein sichtbarer Punkt auf dem
 * Weg), nicht nur die Teilmenge, die in der Zieh-Leiste ziehbar ist.
 */
@Composable
fun SkillTreeDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val profileId = PresentCompanion.profileId(context)
    // Dieselbe Quelle, aus der auch die Profil-ID stammt (siehe PresentCompanion.profileId) - die
    // Vorfuehrung nach einer Freischaltung laeuft damit garantiert auf der Kreatur, deren Baum
    // gerade offen ist, und nicht auf einer anderen.
    val species = remember(context) { AvatarSpeciesPrefs.get(context) }
    val repository = remember(context) { AvatarUnlockRepository(AppDatabase.getInstance(context)) }
    val scope = rememberCoroutineScope()

    val unlocked by repository.observeUnlockedNodes(profileId)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // `remember`, weil `map` sonst bei jeder Recomposition einen neuen Flow baut und das
    // Sammeln dadurch immer wieder von vorn beginnt (Lint: FlowOperatorInvokedInComposition).
    val xpFlow = remember(context, profileId) {
        AppDatabase.getInstance(context).avatarPlayStateDao()
            .observeForProfile(profileId)
            .map { it?.xp ?: 0 }
    }
    val xp by xpFlow.collectAsStateWithLifecycle(initialValue = 0)
    val level = PlayModeXp.levelFor(xp)

    // Sperrt weitere Taps, waehrend ein Freischalt-Aufruf noch laeuft - ohne sie koennte ein
    // Doppel-Tap auf zwei verschiedene Knoten kurz hintereinander mit nur einem Punkt zwei Knoten
    // freischalten, weil der aktualisierte Bestand erst NACH der Datenbankschreibung ankommt.
    var busy by remember { mutableStateOf(false) }

    // Einmaliger Hinweis, wie ein Skillpunkt eingesetzt wird - verschwindet dauerhaft nach dem
    // ersten "OK" (siehe OnboardingPrefs.hasSeenSkillTreeHint).
    var showHint by remember { mutableStateOf(!OnboardingPrefs.hasSeenSkillTreeHint(context)) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        SkillTreeScreen(
            unlocked = unlocked.toSet(),
            level = level,
            // Wieviel bis zum naechsten Aufstieg fehlt - der Rest bis zur naechsten vollen Stufe.
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
            }
        )
    }
}
