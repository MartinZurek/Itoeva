package com.notime.glyphcore.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryAnimationDao {
    @Query("SELECT * FROM library_animations ORDER BY sortOrder, id")
    fun observeAll(): Flow<List<LibraryAnimation>>

    @Query("SELECT * FROM library_animations WHERE id = :id")
    suspend fun getById(id: Long): LibraryAnimation?

    @Query("SELECT COUNT(*) FROM library_animations")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM library_animations WHERE isSelected = 1")
    suspend fun countSelected(): Int

    @Insert
    suspend fun insertAll(animations: List<LibraryAnimation>)

    @Query("UPDATE library_animations SET isSelected = :selected WHERE id = :id")
    suspend fun setSelected(id: Long, selected: Boolean)

    /** Alle Labels - Grundlage dafuer, mitgelieferte Animationen wiederzuerkennen. */
    @Query("SELECT label FROM library_animations")
    suspend fun allLabels(): List<String>

    /**
     * Ersetzt die Posen einer mitgelieferten Animation, laesst aber [LibraryAnimation.isSelected]
     * unangetastet: Ob eine Animation im Picker steht, ist die Entscheidung des Nutzers und darf
     * bei einer Zeichenkorrektur nicht zurueckfallen. Siehe
     * [LibraryAnimationRepository.seedOrRefresh].
     */
    @Query("UPDATE library_animations SET framesData = :framesData, emoji = :emoji WHERE label = :label")
    suspend fun updateArtwork(label: String, framesData: String, emoji: String)
}
