package com.notime.glyphsim.data

import androidx.room.withTransaction

/** Ergebnis eines idempotenten Fuetter-Aufrufs. */
enum class FeedCommitResult {
    /** Dieser Aufruf hat die Ausloesung erstmals beantwortet. */
    FIRST_FEED,

    /** Die Ausloesung war bereits beantwortet; ein Retry gilt weiterhin als UI-Erfolg. */
    ALREADY_FED,

    /** Zu dieser occurrenceId existiert keine Ausloesung mehr. */
    NOT_FOUND
}

/**
 * Persistiert eine Fuetterung und ihre moeglichen XP als EINE Room-Transaktion.
 *
 * Der bedingte Schreibzugriff auf `fedAtMillis` ist absichtlich die erste Datenbankoperation:
 * genau ein konkurrierender Aufruf gewinnt den Wechsel von NULL auf einen Zeitpunkt. Nur dieser
 * Gewinner darf innerhalb derselben Transaktion XP vergeben. Eine Exception oder Cancellation
 * vor dem Commit rollt beides gemeinsam zurueck; ein Retry nach einem Commit erhaelt
 * [FeedCommitResult.ALREADY_FED] und vergibt keine weiteren XP.
 *
 * Die Garantie gilt fuer Aufrufe ueber dieselbe prozessweite [AppDatabase]-Instanz. Widget,
 * Ton und Reaktionsanimation sind UI-Side-Effects und ausdruecklich nicht Teil dieser
 * SQLite-Transaktion.
 */
class AvatarFeedingRepository(private val database: AppDatabase) {

    suspend fun feed(
        occurrenceId: Long,
        fedAtMillis: Long,
        xpPerFeed: Int
    ): FeedCommitResult = database.withTransaction {
        val eventDao = database.avatarFeedEventDao()
        val updated = eventDao.markFedIfUnfed(occurrenceId, fedAtMillis)

        if (updated == 0) {
            return@withTransaction if (eventDao.getById(occurrenceId) == null) {
                FeedCommitResult.NOT_FOUND
            } else {
                FeedCommitResult.ALREADY_FED
            }
        }
        check(updated == 1) { "Feed update affected $updated rows for occurrenceId=$occurrenceId" }

        val event = checkNotNull(eventDao.getById(occurrenceId)) {
            "Feed event disappeared inside transaction: occurrenceId=$occurrenceId"
        }
        if (event.isPlayMode) {
            val xpRows = database.avatarPlayStateDao().addXp(event.profileId, xpPerFeed)
            check(xpRows == 1) {
                "Play feed has no unique play state: profileId=${event.profileId}, rows=$xpRows"
            }
        }
        FeedCommitResult.FIRST_FEED
    }
}
