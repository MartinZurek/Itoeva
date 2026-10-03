package com.notime.glyphsim.ui

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.data.AvatarFeedEvent
import org.junit.Assert.*
import org.junit.Test

class StreamOfferDeckTest {
    @Test fun onlyRealUnansweredGameOccurrencesOfThisCompanionCanBeDeferred() {
        val game = AvatarFeedEvent(1, 1, AnimationType.DRINK, 0, "FENNEC", isPlayMode = true)
        assertTrue(StreamOfferDeck.canDefer(game, "FENNEC"))
        assertFalse(StreamOfferDeck.canDefer(game.copy(isPlayMode = false), "FENNEC"))
        assertFalse(StreamOfferDeck.canDefer(game.copy(fedAtMillis = 1), "FENNEC"))
        assertFalse(StreamOfferDeck.canDefer(game, "PUFFLING"))
        assertFalse(StreamOfferDeck.canDefer(null, "FENNEC"))
    }
    private fun saved(type: AnimationType, id: Long = 1) = SavedAction(1, id, type, null, listOf(intArrayOf(1)))

    @Test fun duplicateCupsLeaveRoomForDifferentOffers() {
        assertEquals(AnimationType.REST, StreamOfferDeck.nextTopic(listOf(saved(AnimationType.DRINK), saved(AnimationType.DRINK, 2), null, null)))
    }
    @Test fun occupiedSlotsAreNeverReplaced() {
        assertNull(StreamOfferDeck.nextTopic(List(4) { saved(AnimationType.DRINK, it.toLong()) }))
    }
    @Test fun duplicateOffersKeepThePendingOccurrenceAndNeverArchivePrivateSnapshots() {
        val slots = listOf(saved(AnimationType.DRINK, 1), saved(AnimationType.DRINK, 2),
            saved(AnimationType.DRINK, 3).copy(libraryAnimationLabel = "Private"), saved(AnimationType.MEDICINE, 4))
        assertEquals(listOf(0), StreamOfferDeck.duplicateIndices(slots, 2))
        assertEquals(listOf(1), StreamOfferDeck.duplicateIndices(slots, null))
    }
    @Test fun fillingEmptyPlacesProducesFourDifferentPublicTopics() {
        val slots = MutableList<SavedAction?>(4) { null }
        repeat(4) { index -> slots[index] = saved(StreamOfferDeck.nextTopic(slots)!!, index.toLong()) }
        assertEquals(setOf(AnimationType.DRINK, AnimationType.REST, AnimationType.BOOK, AnimationType.MOVE), slots.map { it!!.animationType }.toSet())
        assertNull(StreamOfferDeck.nextTopic(slots))
    }
}
