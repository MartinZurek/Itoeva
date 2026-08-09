package com.notime.glyphcore.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
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

    /**
     * Die vom Nutzer eingerichteten Erinnerungen eines Profils - ohne die Play-Mode-Zeile.
     *
     * Ersetzt ein frueheres `getAll().filter { ... }` im Repository: die Auswahl gehoert in die
     * Abfrage. Vorher wurde dafuer die gesamte Tabelle (alle Profile, alle Avatare) in den
     * Speicher geladen, um die grosse Mehrheit sofort wieder wegzuwerfen.
     */
    @Query("SELECT * FROM glyph_reminders WHERE profileId = :profileId AND isPlayMode = 0")
    suspend fun getUserRemindersForProfile(profileId: String): List<GlyphReminder>

    /** Die Play-Mode-Zeile eines Profils. Es darf hoechstens eine geben, siehe [insertPlayReminderIfAbsent]. */
    @Query("SELECT * FROM glyph_reminders WHERE profileId = :profileId AND isPlayMode = 1 LIMIT 1")
    suspend fun getPlayReminderForProfile(profileId: String): GlyphReminder?

    @Insert
    suspend fun insert(reminder: GlyphReminder): Long

    @Insert
    suspend fun insertAll(reminders: List<GlyphReminder>): List<Long>

    @Update
    suspend fun update(reminder: GlyphReminder)

    @Delete
    suspend fun delete(reminder: GlyphReminder)

    @Query("UPDATE glyph_reminders SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    /** Loescht in EINER Anweisung statt Zeile fuer Zeile - siehe [copyProfile]. */
    @Query("DELETE FROM glyph_reminders WHERE profileId = :profileId AND isPlayMode = 0")
    suspend fun deleteUserRemindersForProfile(profileId: String)

    @Query("UPDATE glyph_reminders SET nextTriggerEpochMillis = :epochMillis WHERE id = :id")
    suspend fun updateNextTrigger(id: Long, epochMillis: Long?)

    /**
     * Legt [defaults] fuer [profileId] an, aber nur solange dieses Profil noch keine eigenen
     * Erinnerungen hat.
     *
     * **Warum `@Transaction`.** Vorher standen Zaehlen und Anlegen als zwei getrennte Schritte im
     * Repository, und dazwischen lief jedes Anlegen einzeln. Wurde die Coroutine dazwischen
     * abgebrochen - der Aufrufer ist `viewModelScope`, der beim Verlassen des Bildschirms
     * verschwindet -, blieb ein halb befuelltes Profil zurueck: ein paar Standard-Erinnerungen da,
     * der Rest nicht. Beim naechsten Start zaehlte die Pruefung dann "hat schon welche" und der
     * fehlende Rest kam nie nach. Innerhalb einer Transaktion gibt es nur ganz oder gar nicht.
     */
    @Transaction
    suspend fun seedIfEmpty(profileId: String, defaults: List<GlyphReminder>): List<GlyphReminder> {
        if (countForProfile(profileId) > 0) return emptyList()
        val prepared = defaults.map { it.copy(profileId = profileId) }
        val ids = insertAll(prepared)
        return prepared.mapIndexed { index, reminder -> reminder.copy(id = ids[index]) }
    }

    /**
     * Kopiert die Erinnerungen eines Profils in ein anderes - siehe `GlyphReminderRepository`
     * fuer die fachliche Begruendung (was kopiert wird und warum die Play-Mode-Zeile aussen vor
     * bleibt).
     *
     * **Warum `@Transaction`.** Mit `replace` ist das ein Loeschen UND ein Anlegen. Bricht es
     * dazwischen ab, hat das Zielprofil seine alten Erinnerungen verloren und die neuen nicht
     * bekommen - der schlimmste denkbare Ausgang, und der frueher moegliche.
     */
    @Transaction
    suspend fun copyProfile(
        fromProfileId: String,
        toProfileId: String,
        replace: Boolean
    ): List<GlyphReminder> {
        if (fromProfileId == toProfileId) return emptyList()
        val source = getUserRemindersForProfile(fromProfileId)
        if (replace) deleteUserRemindersForProfile(toProfileId)
        val copies = source.map {
            it.copy(id = 0, profileId = toProfileId, nextTriggerEpochMillis = null)
        }
        val ids = insertAll(copies)
        return copies.mapIndexed { index, reminder -> reminder.copy(id = ids[index]) }
    }

    /**
     * Sorgt dafuer, dass das Profil genau EINE aktivierte Play-Mode-Zeile hat, und gibt sie
     * zurueck.
     *
     * **Die Invariante.** Ein Avatar hat einen Spielstand und eine Zufalls-Erinnerung. Gaebe es
     * zwei, wuerfelten beide unabhaengig voneinander weiter - der Spielmodus feuerte doppelt, und
     * welche der beiden Zeilen der Spielstand-Anker ist, waere Zufall.
     *
     * Vorher stand genau dafuer ein Lesen-dann-Schreiben im ViewModel: erst nachsehen, ob es schon
     * eine gibt, dann anlegen. Zweimal schnell hintereinander auf den Schalter getippt, und beide
     * Durchlaeufe sahen "gibt es noch nicht".
     *
     * **Warum keine Datenbank-Bedingung stattdessen.** Naheliegend waere ein eindeutiger Index auf
     * (`profileId`, `isPlayMode`) - der verboete aber zugleich zwei normale Erinnerungen je Profil.
     * Was man braeuchte, ist ein Index nur ueber die Zeilen mit `isPlayMode = 1`; solche
     * Teil-Indizes kennt SQLite zwar, Room laesst sie sich aber nicht deklarieren, und ein an Room
     * vorbei angelegter Index laesst die Schema-Pruefung beim Oeffnen fehlschlagen. Die Transaktion
     * ist hier die tragfaehige Fassung derselben Zusage.
     */
    @Transaction
    suspend fun insertPlayReminderIfAbsent(
        profileId: String,
        reminder: GlyphReminder
    ): GlyphReminder {
        val existing = getPlayReminderForProfile(profileId)
        if (existing != null) {
            // Vorhanden, aber vielleicht abgeschaltet (Modus war schon einmal aus).
            if (!existing.enabled) setEnabled(existing.id, true)
            return existing.copy(enabled = true)
        }
        val id = insert(reminder.copy(profileId = profileId, isPlayMode = true))
        return reminder.copy(id = id, profileId = profileId, isPlayMode = true)
    }
}
