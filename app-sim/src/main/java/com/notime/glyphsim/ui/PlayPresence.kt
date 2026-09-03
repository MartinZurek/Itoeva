package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.matrix.PlayAmbientActivity
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

    /**
     * Grober, wiederholbarer Tagesplan statt eines neuen Zufalls bei jedem App-Start.
     *
     * Die Tabelle selbst liegt seit der Tagesplan-Gewichtung in
     * [PlayAmbientActivity.plannedTopicFor], weil sie dort im laufenden Betrieb gebraucht wird -
     * und eine zweite Kopie hier waere genau die Art Duplikat, das irgendwann auseinanderlaeuft.
     * Verhalten unveraendert.
     */
    fun topicFor(now: LocalDateTime): AnimationType = PlayAmbientActivity.plannedTopicFor(now.hour)

    private fun key(profileId: String, field: String): String = "${profileId}_$field"

    private const val PREFS = "play_presence"
    const val SHORT_RETURN_MS = 10 * 60 * 1000L
}
