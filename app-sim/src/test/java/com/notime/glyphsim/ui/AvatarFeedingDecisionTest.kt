package com.notime.glyphsim.ui

import com.notime.glyphsim.data.FeedCommitResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarFeedingDecisionTest {

    @Test
    fun `erster Feed startet die Erfolgs UI`() {
        assertTrue(FeedCommitResult.FIRST_FEED.isUiSuccess())
    }

    @Test
    fun `Retry nach einem Commit bleibt ein idempotenter UI Erfolg`() {
        assertTrue(FeedCommitResult.ALREADY_FED.isUiSuccess())
    }

    @Test
    fun `unbekanntes Ereignis darf keine Erfolgs UI starten`() {
        assertFalse(FeedCommitResult.NOT_FOUND.isUiSuccess())
    }
}
