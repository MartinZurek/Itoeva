package com.notime.glyphsim.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    /**
     * Legt den Spielstand an, falls es fuer dieses Profil noch keinen gibt - und laesst einen
     * bestehenden unangetastet.
     *
     * `IGNORE` statt `REPLACE`: `profileId` ist der Primaerschluessel, `REPLACE` wuerde die
     * vorhandene Zeile loeschen und neu anlegen. Damit waeren XP, Level und Startzeitpunkt weg -
     * genau der Fortschritt, der einen Moduswechsel ueberdauern soll.
     *
     * Ersetzt ein Nachsehen-dann-Anlegen im ViewModel. Die Bedingung steht damit dort, wo sie
     * ohnehin schon galt: im Primaerschluessel der Tabelle.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(state: AvatarPlayState)

    /** Atomarer Zuwachs statt Lesen-Aendern-Schreiben - zwei gleichzeitige Fuetterungen duerfen
     *  sich nicht gegenseitig ueberschreiben. Der Rueckgabewert ist die Zahl aktualisierter
     *  Zeilen; die Fuetter-Transaktion verlangt fuer ein Play-Ereignis genau eine. */
    @Query("UPDATE avatar_play_state SET xp = xp + :amount WHERE profileId = :profileId")
    suspend fun addXp(profileId: String, amount: Int): Int

    @Query("UPDATE avatar_play_state SET lastSeenLevel = :level WHERE profileId = :profileId")
    suspend fun setLastSeenLevel(profileId: String, level: Int)
}
