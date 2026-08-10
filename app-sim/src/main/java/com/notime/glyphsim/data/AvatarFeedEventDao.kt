package com.notime.glyphsim.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.notime.glyphcore.data.AnimationType
import kotlinx.coroutines.flow.Flow

/**
 * Eine Zeile der Aufschluesselung "womit wurde gefuettert": entweder ein fest eingebauter
 * [animationType] ODER eine Bibliotheks-Animation ([libraryAnimationLabel]) - nie beides.
 * Die Statistik fasst beide Quellen anschliessend zu einer gemeinsamen Liste zusammen.
 */
data class FeedBreakdownRow(
    val animationType: AnimationType?,
    val libraryAnimationLabel: String?,
    /** Wie oft darauf reagiert wurde. */
    val count: Int,
    /** Wie oft diese Erinnerung ueberhaupt ausgeloest hat - [count] plus die uebergangenen. */
    val triggered: Int
) {
    /**
     * Anteil der beantworteten an allen Ausloesungen, 0..100. Die eigentliche Aussage der
     * Statistik: nicht "wie oft", sondern "wie zuverlaessig".
     */
    val responseRate: Int get() = if (triggered > 0) count * 100 / triggered else 0
}

/** Beantwortete Erinnerungen je Erinnerungs-ID in einem Zeitraum. */
data class ReminderFedCount(val reminderId: Long, val count: Int)

/** Beantwortete Erinnerungen je Thema - fuer den Spielmodus, siehe
 *  [AvatarFeedEventDao.observePlayFedPerTypeSince]. */
data class TypeFedCount(val animationType: AnimationType, val count: Int)

/**
 * Wann ein Thema zum ersten Mal beantwortet wurde - siehe
 * [AvatarFeedEventDao.firstAnsweredPerTopic]. Genau eines von [animationType] und
 * [libraryAnimationLabel] ist gesetzt.
 */
data class TopicFirstRow(
    val animationType: AnimationType?,
    val libraryAnimationLabel: String?,
    val firstEpochMillis: Long
)

/** Erster jemals gespeicherter Zeitpunkt je Erinnerungs-ID, siehe [AvatarFeedEventDao.observeFirstOccurrencePerReminder]. */
data class ReminderFirstOccurrence(val reminderId: Long, val firstEpochMillis: Long)

@Dao
interface AvatarFeedEventDao {
    @Insert
    suspend fun insert(event: AvatarFeedEvent): Long

    @Query("SELECT COUNT(*) FROM avatar_feed_events")
    suspend fun count(): Int

    /** Eine einzelne Zeile per ID - [AvatarFeeding.logFeedEvent] laedt sie nach dem Fuettern
     *  nach, um [isPlayMode] fuer die XP-Vergabe zu pruefen. */
    @Query("SELECT * FROM avatar_feed_events WHERE id = :id")
    suspend fun getById(id: Long): AvatarFeedEvent?

    /**
     * Die juengste noch unbeantwortete Ausloesung dieses Avatars.
     *
     * Grundlage dafuer, eine laufende Erinnerung beim Oeffnen der App fortzusetzen: tippt man das
     * Widget waehrend einer Animation an, startet die App neu und der [ReminderAnimationBus] ist
     * leer - er hat kein Replay, das Ereignis war beim Ausloesen an niemanden zugestellt worden.
     * Die Datenbankzeile ueberlebt dagegen auch das Beenden des Prozesses; ob die Erinnerung noch
     * offen ist, ergibt sich aus ihrem Zeitstempel und der Offen-Dauer der Erinnerung.
     */
    @Query(
        "SELECT * FROM avatar_feed_events WHERE profileId = :profileId AND fedAtMillis IS NULL " +
            "ORDER BY epochMillis DESC LIMIT 1"
    )
    suspend fun latestUnfed(profileId: String): AvatarFeedEvent?

    @Query("SELECT COUNT(*) FROM avatar_feed_events WHERE reminderId = :reminderId")
    suspend fun countForReminder(reminderId: Long): Int

    /**
     * Wann diese Erinnerung zuletzt ausgeloest hat - unabhaengig davon, ob reagiert wurde.
     *
     * Grundlage der Mindestabstands-Regel in `ReminderTrigger`: Eine Erinnerung darf nie
     * haeufiger erscheinen als ihr eigenes Intervall erlaubt, egal welcher Weg sie ausloest.
     */
    @Query("SELECT MAX(epochMillis) FROM avatar_feed_events WHERE reminderId = :reminderId")
    suspend fun latestOccurrenceMillis(reminderId: Long): Long?

    /**
     * Fuetterungen EINES Avatars seit [sinceEpochMillis], beobachtbar - der Startbildschirm zeigt
     * damit die heutige Zahl an und zaehlt sofort hoch, wenn dort gefuettert wird.
     *
     * Die Einschraenkung auf [profileId] ist wesentlich: jeder Avatar hat seinen eigenen Satz
     * Erinnerungen, eine gemeinsame Zahl ueber alle Avatare waere daher wenig aussagekraeftig.
     */
    @Query(
        "SELECT COUNT(*) FROM avatar_feed_events " +
            "WHERE profileId = :profileId AND epochMillis >= :sinceEpochMillis " +
            "AND fedAtMillis IS NOT NULL"
    )
    fun observeCountSince(profileId: String, sinceEpochMillis: Long): Flow<Int>

    /** Alle Ausloesungen im Zeitraum - beantwortete wie uebergangene. */
    @Query(
        "SELECT COUNT(*) FROM avatar_feed_events " +
            "WHERE profileId = :profileId AND epochMillis >= :sinceEpochMillis"
    )
    fun observeTriggeredSince(profileId: String, sinceEpochMillis: Long): Flow<Int>

    /**
     * Beantwortete Erinnerungen seit [sinceEpochMillis], nach Erinnerung gruppiert - Grundlage
     * der Zielerreichung (siehe [com.notime.glyphsim.matrix.AvatarMood.fromGoals]). Die Ziele
     * selbst stehen an der Erinnerung, nicht am Ereignis, und werden vom Aufrufer dazugelegt.
     */
    @Query(
        "SELECT reminderId, COUNT(*) AS count FROM avatar_feed_events " +
            "WHERE profileId = :profileId AND epochMillis >= :sinceEpochMillis " +
            "AND fedAtMillis IS NOT NULL GROUP BY reminderId"
    )
    suspend fun countFedPerReminderSince(profileId: String, sinceEpochMillis: Long): List<ReminderFedCount>

    /**
     * Wie [countFedPerReminderSince], beobachtbar - damit die Zielanzeige beim Fuettern mitzaehlt.
     *
     * [isPlayMode] trennt Spiel und Normalbetrieb: Beide fuehren getrennt Buch (siehe
     * [AvatarFeedEvent.isPlayMode]). Eine gemeinsame Zahl waere in beide Richtungen falsch - im
     * Spiel gefuetterte Zufalls-Erinnerungen haben mit den eigenen Tageszielen nichts zu tun, und
     * umgekehrt.
     */
    @Query(
        "SELECT reminderId, COUNT(*) AS count FROM avatar_feed_events " +
            "WHERE profileId = :profileId AND epochMillis >= :sinceEpochMillis " +
            "AND isPlayMode = :isPlayMode AND fedAtMillis IS NOT NULL GROUP BY reminderId"
    )
    fun observeFedPerReminderSince(
        profileId: String,
        sinceEpochMillis: Long,
        isPlayMode: Boolean
    ): Flow<List<ReminderFedCount>>

    /**
     * Fuetterungen des Spielmodus nach THEMA gruppiert, nicht nach Erinnerung.
     *
     * Im Spiel gibt es nur eine einzige Erinnerungs-Zeile je Avatar, deren Thema bei jeder
     * Neuplanung wechselt (siehe [com.notime.glyphsim.matrix.PlayModeRoll]) - nach `reminderId`
     * gruppiert ergaebe das einen einzigen Eintrag mit dem gerade zufaellig aktuellen Symbol,
     * und die Aufschluesselung "was habe ich heute eigentlich gefuettert" ginge verloren. Der
     * Thementyp steht dagegen am Ereignis selbst und bleibt, was er zur Ausloesung war.
     */
    @Query(
        "SELECT animationType, COUNT(*) AS count FROM avatar_feed_events " +
            "WHERE profileId = :profileId AND epochMillis >= :sinceEpochMillis " +
            "AND isPlayMode = 1 AND fedAtMillis IS NOT NULL AND animationType IS NOT NULL " +
            "GROUP BY animationType ORDER BY count DESC"
    )
    fun observePlayFedPerTypeSince(profileId: String, sinceEpochMillis: Long): Flow<List<TypeFedCount>>

    /** Wie [observeCountSince], aber ohne Flow - fuer die Stimmungsberechnung des Avatars. */
    @Query(
        "SELECT COUNT(*) FROM avatar_feed_events " +
            "WHERE profileId = :profileId AND epochMillis >= :sinceEpochMillis " +
            "AND fedAtMillis IS NOT NULL"
    )
    suspend fun countFedSince(profileId: String, sinceEpochMillis: Long): Int

    @Query(
        "SELECT COUNT(*) FROM avatar_feed_events " +
            "WHERE profileId = :profileId AND epochMillis >= :sinceEpochMillis"
    )
    suspend fun countTriggeredSince(profileId: String, sinceEpochMillis: Long): Int

    /**
     * Die ZEITPUNKTE der beantworteten Erinnerungen - fuer die Rueckschau ueber mehrere Tage
     * (siehe [com.notime.glyphsim.ui.PlayTalk]).
     *
     * Bewusst die rohen Zeitpunkte statt einer Gruppierung nach Tag in SQL: SQLites Datums-
     * funktionen rechnen in UTC, sofern man ihnen nicht ausdruecklich etwas anderes sagt. Ein
     * Tag ist aber das, was der Nutzer in SEINER Zeitzone als Tag erlebt - was um 23 Uhr
     * geschieht, gehoert zu diesem Tag und nicht zum naechsten. Diese Grenze wird deshalb dort
     * gezogen, wo die Zeitzone ohnehin bekannt ist, und nicht in der Abfrage.
     */
    @Query(
        "SELECT epochMillis FROM avatar_feed_events " +
            "WHERE profileId = :profileId AND epochMillis >= :sinceEpochMillis " +
            "AND fedAtMillis IS NOT NULL ORDER BY epochMillis"
    )
    suspend fun fedTimestampsSince(profileId: String, sinceEpochMillis: Long): List<Long>

    /**
     * Haelt fest, dass auf eine Ausloesung reagiert wurde. Bewusst ueber die Zeilen-ID statt
     * ueber die Erinnerung: dieselbe Erinnerung loest immer wieder aus, gemeint ist aber genau
     * dieses eine Auftreten.
     */
    @Query("UPDATE avatar_feed_events SET fedAtMillis = :fedAtMillis WHERE id = :id")
    suspend fun markFed(id: Long, fedAtMillis: Long)

    /**
     * Aufschluesselung nach Animation fuer einen Zeitraum, haeufigste zuerst.
     *
     * Gruppiert ueber BEIDE Quellen (fester Typ und Bibliotheks-Label), damit
     * Bibliotheks-Animationen nicht alle in einem namenlosen Topf landen - genau das waere
     * passiert, haette man nur nach `animationType` gruppiert, der dort immer NULL ist.
     */
    @Query(
        "SELECT animationType, libraryAnimationLabel, " +
            "SUM(CASE WHEN fedAtMillis IS NOT NULL THEN 1 ELSE 0 END) AS count, " +
            "COUNT(*) AS triggered " +
            "FROM avatar_feed_events " +
            "WHERE profileId = :profileId AND epochMillis >= :sinceEpochMillis " +
            "GROUP BY animationType, libraryAnimationLabel " +
            "ORDER BY triggered DESC"
    )
    fun observeBreakdownSince(profileId: String, sinceEpochMillis: Long): Flow<List<FeedBreakdownRow>>

    /**
     * Der erste Moment, den ihr wirklich geteilt habt - Grundlage des Kapitels (siehe
     * [com.notime.glyphsim.matrix.CompanionChapter]).
     *
     * `fedAtMillis IS NOT NULL` ist hier das Wesentliche: gezaehlt wird nicht, wann zum ersten Mal
     * etwas ausgeloest hat, sondern wann zum ersten Mal jemand darauf reagiert hat. Eine
     * Erinnerung, die im leeren Raum ablief, ist kein gemeinsamer Anfang.
     *
     * Sortiert wird nach [AvatarFeedEvent.epochMillis] und nicht nach dem Reaktionszeitpunkt: Der
     * Moment, um den es geht, ist der, in dem etwas anstand - die Reaktion gehoert dazu, datiert
     * ihn aber nicht.
     */
    @Query(
        "SELECT MIN(epochMillis) FROM avatar_feed_events " +
            "WHERE profileId = :profileId AND fedAtMillis IS NOT NULL"
    )
    suspend fun firstAnsweredMillis(profileId: String): Long?

    /**
     * Wann jedes Thema zum ersten Mal beantwortet wurde - Grundlage der Erinnerungen (siehe
     * [com.notime.glyphsim.matrix.buildMemories]).
     *
     * `isPlayMode = 0` ist hier wesentlich: Im Spielmodus wird das Thema gewuerfelt (siehe
     * [com.notime.glyphsim.matrix.PlayModeRoll]). "Das erste Mal getrunken" waere dort der Zufall
     * und nicht der Nutzer - und eine Erinnerung an etwas, das man gar nicht getan hat, ist
     * schlimmer als gar keine.
     *
     * Gruppiert wird ueber beide Spalten zugleich, weil genau eine von ihnen gesetzt ist (siehe
     * [AvatarFeedEvent]): eingebaute Themen tragen einen [AnimationType], Bibliotheks-Animationen
     * ihren Namen.
     */
    @Query(
        "SELECT animationType, libraryAnimationLabel, MIN(epochMillis) AS firstEpochMillis " +
            "FROM avatar_feed_events " +
            "WHERE profileId = :profileId AND fedAtMillis IS NOT NULL AND isPlayMode = 0 " +
            "GROUP BY animationType, libraryAnimationLabel ORDER BY firstEpochMillis"
    )
    suspend fun firstAnsweredPerTopic(profileId: String): List<TopicFirstRow>

    /**
     * Erste jemals gespeicherte Ausloesung JE Erinnerung (beantwortet oder nicht) - Grundlage
     * dafuer, wie lange es eine bestimmte Erinnerung ueberhaupt schon gibt. Bewusst getrennt von
     * [observeFirstAnsweredMillis], das nur das Wesen insgesamt betrifft: eine gestern erst
     * angelegte Erinnerung in einem monatealten Profil braucht ihren EIGENEN Startpunkt, sonst
     * wirkte sie schon nach einem Tag faelschlich wie chronisch verpasst.
     */
    @Query(
        "SELECT reminderId, MIN(epochMillis) AS firstEpochMillis FROM avatar_feed_events " +
            "WHERE profileId = :profileId GROUP BY reminderId"
    )
    fun observeFirstOccurrencePerReminder(profileId: String): Flow<List<ReminderFirstOccurrence>>

    /**
     * Setzt die Statistik EINES Avatars zurueck. Bewusst nur dessen Zeilen: die Avatare sind
     * voneinander unabhaengig, ein Zuruecksetzen soll nicht die Historie der anderen mitnehmen.
     */
    @Query("DELETE FROM avatar_feed_events WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: String)
}
