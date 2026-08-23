package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.matrix.PlayScene
import java.time.LocalDateTime

/**
 * Merkt sich, wo der Avatar zuletzt war und womit er beschaeftigt war.
 *
 * Eine kurze Abwesenheit setzt diese Situation fort. Nach einer laengeren Abwesenheit wird nicht
 * jede verpasste Handlung nachsimuliert; der Avatar steigt direkt an der Stelle seines Tagesplans
 * ein, die zur aktuellen Uhrzeit passt.
 */
object PlayPresence {

    data class Entry(
        val place: PlayScene.Place,
        val topic: AnimationType,
        val resumesPreviousSituation: Boolean
    )

    internal data class Snapshot(
        val place: PlayScene.Place,
        val topic: AnimationType,
        val savedAtMillis: Long
    )

    fun entry(
        context: Context,
        profileId: String,
        nowMillis: Long = System.currentTimeMillis(),
        now: LocalDateTime = LocalDateTime.now()
    ): Entry {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val savedAt = prefs.getLong(key(profileId, "saved_at"), 0L)
        val place = prefs.getString(key(profileId, "place"), null)
            ?.let { runCatching { PlayScene.Place.valueOf(it) }.getOrNull() }
        val topic = prefs.getString(key(profileId, "topic"), null)
            ?.let { runCatching { AnimationType.valueOf(it) }.getOrNull() }
        val snapshot = if (place != null && topic != null && savedAt > 0L) {
            Snapshot(place, topic, savedAt)
        } else {
            null
        }
        return resolve(snapshot, nowMillis, now)
    }

    internal fun resolve(
        snapshot: Snapshot?,
        nowMillis: Long,
        now: LocalDateTime
    ): Entry {
        val age = snapshot?.let { nowMillis - it.savedAtMillis }
        if (snapshot != null && age != null && age in 0..SHORT_RETURN_MS) {
            return Entry(snapshot.place, snapshot.topic, true)
        }
        val topic = topicFor(now)
        return Entry(PlayScene.forTopic(topic), topic, false)
    }

    fun save(
        context: Context,
        profileId: String,
        place: PlayScene.Place,
        topic: AnimationType,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(key(profileId, "place"), place.name)
            .putString(key(profileId, "topic"), topic.name)
            .putLong(key(profileId, "saved_at"), nowMillis)
            .apply()
    }

    /** Grober, wiederholbarer Tagesplan statt eines neuen Zufalls bei jedem App-Start. */
    fun topicFor(now: LocalDateTime): AnimationType = when (now.hour) {
        in 0..5 -> AnimationType.SLEEP
        6 -> AnimationType.GENERAL
        7 -> AnimationType.DRINK
        8 -> AnimationType.MOVE
        9 -> AnimationType.FOCUS
        10, 11 -> AnimationType.WORK
        12 -> AnimationType.DRINK
        13 -> AnimationType.MOVE
        14 -> AnimationType.WORK
        15 -> AnimationType.FOCUS
        16 -> AnimationType.CREATIVITY
        17 -> AnimationType.MOVE
        18 -> AnimationType.DRINK
        19 -> AnimationType.LOVE
        20 -> AnimationType.BOOK
        21 -> AnimationType.REST
        22 -> AnimationType.MINDFULNESS
        else -> AnimationType.SLEEP
    }

    private fun key(profileId: String, field: String): String = "${profileId}_$field"

    private const val PREFS = "play_presence"
    const val SHORT_RETURN_MS = 10 * 60 * 1000L
}
