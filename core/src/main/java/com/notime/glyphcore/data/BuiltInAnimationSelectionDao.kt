package com.notime.glyphcore.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
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

    @Query("UPDATE builtin_animation_selection SET isSelected = :selected WHERE animationType = :type")
    suspend fun setSelected(type: String, selected: Boolean)
}
