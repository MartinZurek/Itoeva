package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.data.AvatarFeedEvent
import com.notime.glyphsim.matrix.ReminderAnimations
import com.notime.glyphsim.reminder.ReminderTrigger

/** Public game offers use real occurrence rows and the ordinary completion path. */
internal object StreamOfferDeck {
    private val topics = listOf(AnimationType.DRINK, AnimationType.REST, AnimationType.BOOK, AnimationType.MOVE)

    fun nextTopic(slots: List<SavedAction?>): AnimationType? {
        if (slots.none { it == null }) return null
        val present = slots.mapNotNull { it?.animationType }.toSet()
        return topics.firstOrNull { it !in present }
    }

    fun duplicateIndices(slots: List<SavedAction?>, pendingId: Long?): List<Int> {
        val reserved = slots.firstOrNull { it?.occurrenceId == pendingId }
        val seen = mutableSetOf<AnimationType>()
        reserved?.animationType?.let { seen += it }
        return slots.mapIndexedNotNull { index, saved ->
            if (saved == null || saved.occurrenceId == pendingId || saved.libraryAnimationLabel != null || saved.animationType !in topics) null
            else if (!seen.add(saved.animationType!!)) index else null
        }
    }

    fun canDefer(event: AvatarFeedEvent?, profileId: String): Boolean =
        event != null && event.isPlayMode && event.profileId == profileId && event.fedAtMillis == null

    suspend fun create(context: Context, profileId: String, topic: AnimationType): SavedAction? {
        require(topic in topics)
        val db = AppDatabase.getInstance(context)
        for (saved in ActionSlotStore.deferredForStream(context, profileId).filter { it.animationType == topic }) {
            val event = db.avatarFeedEventDao().getById(saved.occurrenceId)
            if (event != null && event.fedAtMillis == null && event.isPlayMode && event.profileId == profileId) return saved
            ActionSlotStore.removeDeferred(context, profileId, saved.occurrenceId)
        }
        val reminder = db.glyphReminderDao().getPlayReminderForProfile(RoutineOwner.current(context)) ?: return null
        if (!reminder.isPlayMode) return null
        val event = ReminderTrigger.feedEventFor(reminder, profileId, topic, null, System.currentTimeMillis())
        val occurrenceId = db.avatarFeedEventDao().insert(event)
        return SavedAction(reminder.id, occurrenceId, topic, null, ReminderAnimations.framesFor(topic))
    }
}
