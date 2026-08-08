package com.notime.glyphsim.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AvatarPlayStateDao {
    @Query("SELECT * FROM avatar_play_state WHERE profileId = :profileId")
    suspend fun getForProfile(profileId: String): AvatarPlayState?

    /** Beobachtbar, damit der Assistenten-Dialog Level/XP sofort mitzaehlt, sobald gefuettert wird. */
    @Query("SELECT * FROM avatar_play_state WHERE profileId = :profileId")
    fun observeForProfile(profileId: String): Flow<AvatarPlayState?>

    @Insert
    suspend fun insert(state: AvatarPlayState)

    /** Atomarer Zuwachs statt Lesen-Aendern-Schreiben - zwei gleichzeitige Fuetterungen duerfen
     *  sich nicht gegenseitig ueberschreiben. */
    @Query("UPDATE avatar_play_state SET xp = xp + :amount WHERE profileId = :profileId")
    suspend fun addXp(profileId: String, amount: Int)

    @Query("UPDATE avatar_play_state SET lastSeenLevel = :level WHERE profileId = :profileId")
    suspend fun setLastSeenLevel(profileId: String, level: Int)
}
