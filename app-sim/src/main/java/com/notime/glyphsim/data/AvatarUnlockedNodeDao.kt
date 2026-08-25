package com.notime.glyphsim.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AvatarUnlockedNodeDao {

    @Query("SELECT nodeId FROM avatar_unlocked_nodes WHERE profileId = :profileId")
    suspend fun nodeIdsFor(profileId: String): List<String>

    /** Beobachtbar, damit die Zieh-Leiste sofort mitwaechst, sobald etwas freigeschaltet wird. */
    @Query("SELECT nodeId FROM avatar_unlocked_nodes WHERE profileId = :profileId")
    fun observeNodeIdsFor(profileId: String): Flow<List<String>>

    /**
     * `IGNORE` statt `REPLACE`: Ein bereits offener Knoten soll seinen urspruenglichen
     * Freischalt-Zeitpunkt behalten. `REPLACE` wuerde die Zeile loeschen und neu anlegen und damit
     * die Reihenfolge verfaelschen, in der jemand seinen Baum geoeffnet hat.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(nodes: List<AvatarUnlockedNode>)

    @Query("SELECT COUNT(*) FROM avatar_unlocked_nodes WHERE profileId = :profileId")
    suspend fun countFor(profileId: String): Int
}
