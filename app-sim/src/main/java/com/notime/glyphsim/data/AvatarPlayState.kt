package com.notime.glyphsim.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fortschritt EINES Avatars im Play Mode (siehe ui/PlayModeViewModel) - eine Zeile je
 * [profileId] (Avatar-Name, siehe ui/AvatarSpeciesPrefs), angelegt beim ersten "Spielmodus
 * starten".
 *
 * [xp] waechst mit jeder erstmals gefuetterten Play-Mode-Ausloesung (siehe
 * [AvatarFeedingRepository]), das Level ergibt sich daraus
 * ([com.notime.glyphsim.ui.PlayModeXp.levelFor]) statt selbst gespeichert zu werden - so kann es
 * nie mit [xp] auseinanderlaufen.
 *
 * [lastSeenLevel] haelt fest, bis zu welchem Level der Avatar dem Nutzer schon gratuliert hat
 * (siehe ui/AvatarAssistant.AssistantPlay) - ohne das wuerde die Glueckwunsch-Blase bei jedem
 * Oeffnen des Dialogs erneut erscheinen, nicht nur beim tatsaechlichen Aufstieg.
 */
@Entity(tableName = "avatar_play_state")
data class AvatarPlayState(
    @PrimaryKey val profileId: String,
    val xp: Int = 0,
    val startedAtMillis: Long,
    val lastSeenLevel: Int = 1
)
