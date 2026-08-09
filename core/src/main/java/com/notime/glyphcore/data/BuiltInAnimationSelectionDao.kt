package com.notime.glyphcore.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface BuiltInAnimationSelectionDao {
    @Query("SELECT * FROM builtin_animation_selection")
    fun observeAll(): Flow<List<BuiltInAnimationSelection>>

    @Query("SELECT COUNT(*) FROM builtin_animation_selection")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM builtin_animation_selection WHERE isSelected = 1")
    suspend fun countSelected(): Int

    @Insert
    suspend fun insertAll(rows: List<BuiltInAnimationSelection>)

    @Query("SELECT isSelected FROM builtin_animation_selection WHERE animationType = :type")
    suspend fun isSelected(type: String): Boolean?

    @Query("UPDATE builtin_animation_selection SET isSelected = :selected WHERE animationType = :type")
    suspend fun setSelected(type: String, selected: Boolean)

    /**
     * Gegenstueck zu `LibraryAnimationDao.selectIfWithinLimit` fuer die fest eingebauten Typen -
     * die ausfuehrliche Begruendung steht dort. Rueckgabe: 1 = angenommen, 0 = abgelehnt.
     */
    @Query(
        """
        UPDATE builtin_animation_selection SET isSelected = 1
        WHERE animationType = :type AND isSelected = 0 AND (
            (SELECT COUNT(*) FROM library_animations WHERE isSelected = 1) +
            (SELECT COUNT(*) FROM builtin_animation_selection WHERE isSelected = 1)
        ) < :limit
        """
    )
    suspend fun selectIfWithinLimit(type: String, limit: Int): Int

    /**
     * Legt die Startauswahl an, falls die Tabelle noch leer ist - alle Typen ausgewaehlt.
     *
     * `@Transaction` aus demselben Grund wie bei `GlyphReminderDao.seedIfEmpty`: Zaehlen und
     * Anlegen waren zwei Schritte, und ein Abbruch dazwischen hinterliess eine halb befuellte
     * Tabelle, die beim naechsten Start als "schon befuellt" durchging.
     */
    @Transaction
    suspend fun seedIfEmpty(rows: List<BuiltInAnimationSelection>) {
        if (count() > 0) return
        insertAll(rows)
    }
}
