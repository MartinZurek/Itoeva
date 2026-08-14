package com.notime.glyphsim.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.ui.PlayModeXp
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Prueft die echte SQLite-Transaktion hinter einer Fuetterung. */
@RunWith(AndroidJUnit4::class)
class AvatarFeedingTransactionTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: AvatarFeedingRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = AvatarFeedingRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun playState(profileId: String = PROFILE) {
        db.avatarPlayStateDao().insertIfAbsent(
            AvatarPlayState(profileId = profileId, startedAtMillis = 1_000L)
        )
    }

    private suspend fun event(isPlayMode: Boolean = true, profileId: String = PROFILE): Long =
        db.avatarFeedEventDao().insert(
            AvatarFeedEvent(
                reminderId = 11L,
                animationType = AnimationType.DRINK,
                epochMillis = 2_000L,
                profileId = profileId,
                isPlayMode = isPlayMode
            )
        )

    @Test
    fun firstPlayFeedMarksEventAndAwardsXp() = runBlocking {
        playState()
        val id = event()

        val result = repository.feed(id, FIRST_TIME, PlayModeXp.XP_PER_FEED)

        assertEquals(FeedCommitResult.FIRST_FEED, result)
        assertEquals(FIRST_TIME, db.avatarFeedEventDao().getById(id)?.fedAtMillis)
        assertEquals(PlayModeXp.XP_PER_FEED, db.avatarPlayStateDao().getForProfile(PROFILE)?.xp)
    }

    @Test
    fun sequentialRetryKeepsFirstTimestampAndAwardsXpOnce() = runBlocking {
        playState()
        val id = event()

        val first = repository.feed(id, FIRST_TIME, PlayModeXp.XP_PER_FEED)
        val retry = repository.feed(id, SECOND_TIME, PlayModeXp.XP_PER_FEED)

        assertEquals(FeedCommitResult.FIRST_FEED, first)
        assertEquals(FeedCommitResult.ALREADY_FED, retry)
        assertEquals(FIRST_TIME, db.avatarFeedEventDao().getById(id)?.fedAtMillis)
        assertEquals(PlayModeXp.XP_PER_FEED, db.avatarPlayStateDao().getForProfile(PROFILE)?.xp)
    }

    @Test
    fun concurrentFeedsHaveExactlyOneWinner() = runBlocking {
        playState()
        val id = event()
        val startGate = CompletableDeferred<Unit>()

        val calls = listOf(FIRST_TIME, SECOND_TIME).map { time ->
            async(Dispatchers.IO) {
                startGate.await()
                repository.feed(id, time, PlayModeXp.XP_PER_FEED)
            }
        }
        // Beide Coroutines existieren und warten ausserhalb der Room-Transaktion auf dasselbe Gate.
        startGate.complete(Unit)
        val results = withTimeout(5_000L) { calls.awaitAll() }

        assertEquals(1, results.count { it == FeedCommitResult.FIRST_FEED })
        assertEquals(1, results.count { it == FeedCommitResult.ALREADY_FED })
        assertEquals(PlayModeXp.XP_PER_FEED, db.avatarPlayStateDao().getForProfile(PROFILE)?.xp)
        assertNotNull(db.avatarFeedEventDao().getById(id)?.fedAtMillis)
    }

    @Test
    fun nonPlayFeedMarksEventWithoutXp() = runBlocking {
        playState()
        val id = event(isPlayMode = false)

        assertEquals(
            FeedCommitResult.FIRST_FEED,
            repository.feed(id, FIRST_TIME, PlayModeXp.XP_PER_FEED)
        )
        assertEquals(FIRST_TIME, db.avatarFeedEventDao().getById(id)?.fedAtMillis)
        assertEquals(0, db.avatarPlayStateDao().getForProfile(PROFILE)?.xp)
    }

    @Test
    fun unknownOccurrenceIsNotFoundAndDoesNotAwardXp() = runBlocking {
        playState()

        assertEquals(
            FeedCommitResult.NOT_FOUND,
            repository.feed(999L, FIRST_TIME, PlayModeXp.XP_PER_FEED)
        )
        assertEquals(0, db.avatarPlayStateDao().getForProfile(PROFILE)?.xp)
    }

    @Test
    fun missingPlayStateRollsBackAndRetryCanSucceed() = runBlocking {
        val id = event()

        val failed = runCatching {
            repository.feed(id, FIRST_TIME, PlayModeXp.XP_PER_FEED)
        }
        assertTrue("Fehlender Spielstand muss die Transaktion abbrechen", failed.isFailure)
        assertNull(db.avatarFeedEventDao().getById(id)?.fedAtMillis)

        playState()
        assertEquals(
            FeedCommitResult.FIRST_FEED,
            repository.feed(id, SECOND_TIME, PlayModeXp.XP_PER_FEED)
        )
        assertEquals(SECOND_TIME, db.avatarFeedEventDao().getById(id)?.fedAtMillis)
        assertEquals(PlayModeXp.XP_PER_FEED, db.avatarPlayStateDao().getForProfile(PROFILE)?.xp)
    }

    @Test
    fun differentOccurrencesEachAwardXp() = runBlocking {
        playState()
        val first = event()
        val second = event()

        assertEquals(
            FeedCommitResult.FIRST_FEED,
            repository.feed(first, FIRST_TIME, PlayModeXp.XP_PER_FEED)
        )
        assertEquals(
            FeedCommitResult.FIRST_FEED,
            repository.feed(second, SECOND_TIME, PlayModeXp.XP_PER_FEED)
        )
        assertEquals(2 * PlayModeXp.XP_PER_FEED, db.avatarPlayStateDao().getForProfile(PROFILE)?.xp)
    }

    private companion object {
        const val PROFILE = "PUFFLING"
        const val FIRST_TIME = 1_700_000_000_000L
        const val SECOND_TIME = 1_700_000_001_000L
    }
}
