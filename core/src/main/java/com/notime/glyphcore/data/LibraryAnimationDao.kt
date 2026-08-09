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

    /** Gibt die vergebenen Ids zurueck, damit der Aufrufer die neuen Zeilen direkt ansprechen kann. */
    @Insert
    suspend fun insertAll(animations: List<LibraryAnimation>): List<Long>

    @Query("SELECT isSelected FROM library_animations WHERE id = :id")
    suspend fun isSelected(id: Long): Boolean?

    @Query("UPDATE library_animations SET isSelected = :selected WHERE id = :id")
    suspend fun setSelected(id: Long, selected: Boolean)

    /**
     * Waehlt eine Animation aus - aber nur, wenn dadurch die Gesamt-Obergrenze des Pickers nicht
     * ueberschritten wird. Rueckgabe: Anzahl geaenderter Zeilen, also 1 = angenommen, 0 = abgelehnt.
     *
     * **Warum als eine einzige Anweisung.** Die Pruefung lief vorher im ViewModel: erst beide
     * Tabellen zaehlen, dann schreiben. Zwischen Zaehlen und Schreiben kann sich der Stand
     * aendern - zwei schnell hintereinander angetippte Kacheln sahen beide "noch Platz" und
     * belegten denselben letzten. Hier entscheidet die Datenbank beim Schreiben selbst; ein
     * Zwischenraum, in dem sich etwas aendern koennte, existiert nicht.
     *
     * `isSelected = 0` in der Bedingung ist kein Detail: ohne das zaehlte ein erneutes Auswaehlen
     * einer bereits ausgewaehlten Animation gegen das Limit und wuerde am Rand abgelehnt.
     *
     * Die Summe geht ueber BEIDE Quellen, weil fest eingebaute Typen und Bibliotheks-Animationen
     * sich denselben Topf teilen (siehe `GlyphReminderViewModel.MAX_ANIMATIONS_IN_PICKER`).
     */
    @Query(
        """
        UPDATE library_animations SET isSelected = 1
        WHERE id = :id AND isSelected = 0 AND (
            (SELECT COUNT(*) FROM library_animations WHERE isSelected = 1) +
            (SELECT COUNT(*) FROM builtin_animation_selection WHERE isSelected = 1)
        ) < :limit
        """
    )
    suspend fun selectIfWithinLimit(id: Long, limit: Int): Int

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
