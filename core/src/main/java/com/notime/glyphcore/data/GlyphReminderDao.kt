package com.notime.glyphcore.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GlyphReminderDao {
    @Query("SELECT * FROM glyph_reminders ORDER BY startMinuteOfDay")
    fun observeAll(): Flow<List<GlyphReminder>>

    /** Erinnerungen EINES Profils - jedes Profil hat seinen eigenen Satz (siehe [GlyphReminder.profileId]). */
    @Query("SELECT * FROM glyph_reminders WHERE profileId = :profileId ORDER BY startMinuteOfDay")
    fun observeForProfile(profileId: String): Flow<List<GlyphReminder>>

    @Query("SELECT * FROM glyph_reminders WHERE id = :id")
    suspend fun getById(id: Long): GlyphReminder?

    @Query("SELECT * FROM glyph_reminders WHERE enabled = 1")
    suspend fun getEnabled(): List<GlyphReminder>

    /**
     * Nur die aktiven Erinnerungen des gerade gewaehlten Profils - Grundlage fuer Alarmplanung
     * UND Kollisionspruefung, damit Erinnerungen eines gerade nicht gewaehlten Profils weder
     * feuern noch die Zeitfenster des aktiven Profils verschieben.
     */
    @Query("SELECT * FROM glyph_reminders WHERE enabled = 1 AND profileId = :profileId")
    suspend fun getEnabledForProfile(profileId: String): List<GlyphReminder>

    /**
     * Alle Erinnerungen EINES Profils, auch deaktivierte - anders als [getEnabledForProfile]
     * gebraucht, wo eine bestimmte Zeile (z.B. die Play-Mode-Zeile, siehe
     * [GlyphReminder.isPlayMode]) wiedergefunden werden muss, unabhaengig von ihrem
     * [GlyphReminder.enabled]-Status.
     */
    @Query("SELECT * FROM glyph_reminders WHERE profileId = :profileId")
    suspend fun getAllForProfile(profileId: String): List<GlyphReminder>

    @Query("SELECT * FROM glyph_reminders")
    suspend fun getAll(): List<GlyphReminder>

    /**
     * Nur die "echten", vom Nutzer eingerichteten Erinnerungen - ohne die Play-Mode-Zeile
     * (siehe [GlyphReminder.isPlayMode]). Grundlage fuer die Seed-Entscheidung in
     * `GlyphReminderRepository.seedIfEmpty`: ohne den Ausschluss wuerde ein frisch gewaehlter
     * Avatar, der nur seine Play-Mode-Zeile hat, faelschlich als "hat schon Erinnerungen"
     * gelten und nie die Standard-Erinnerungen bekommen.
     */
    @Query("SELECT COUNT(*) FROM glyph_reminders WHERE profileId = :profileId AND isPlayMode = 0")
    suspend fun countForProfile(profileId: String): Int

    @Insert
    suspend fun insert(reminder: GlyphReminder): Long

    @Update
    suspend fun update(reminder: GlyphReminder)

    @Delete
    suspend fun delete(reminder: GlyphReminder)

    @Query("UPDATE glyph_reminders SET nextTriggerEpochMillis = :epochMillis WHERE id = :id")
    suspend fun updateNextTrigger(id: Long, epochMillis: Long?)
}
