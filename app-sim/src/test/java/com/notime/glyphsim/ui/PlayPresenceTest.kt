package com.notime.glyphsim.ui

import com.notime.glyphcore.data.AnimationType
import com.notime.glyphsim.matrix.PlayScene
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class PlayPresenceTest {

    @Test
    fun `der Tagesplan deckt den ganzen Tag mit plausiblen Eckpunkten ab`() {
        val day = LocalDateTime.of(2026, 8, 23, 0, 0)
        val topics = (0..23).map { hour -> PlayPresence.topicFor(day.withHour(hour)) }

        assertEquals(24, topics.size)
        assertEquals(AnimationType.SLEEP, topics[3])
        assertEquals(AnimationType.DRINK, topics[7])
        assertEquals(AnimationType.WORK, topics[11])
        assertEquals(AnimationType.MOVE, topics[17])
        assertEquals(AnimationType.BOOK, topics[20])
        assertEquals(AnimationType.SLEEP, topics[23])
    }

    @Test
    fun `derselbe Zeitpunkt erzeugt immer dieselbe Situation`() {
        val now = LocalDateTime.of(2026, 8, 23, 16, 42)
        assertEquals(PlayPresence.topicFor(now), PlayPresence.topicFor(now))
        assertEquals(AnimationType.CREATIVITY, PlayPresence.topicFor(now))
    }

    @Test
    fun `kurze Abwesenheit setzt die vorherige Situation fort`() {
        val savedAt = 1_000_000L
        val result = PlayPresence.resolve(
            snapshot = PlayPresence.Snapshot(PlayScene.Place.PARK, AnimationType.MOVE, savedAt),
            nowMillis = savedAt + PlayPresence.SHORT_RETURN_MS,
            now = LocalDateTime.of(2026, 8, 23, 23, 0)
        )

        assertEquals(PlayScene.Place.PARK, result.place)
        assertEquals(AnimationType.MOVE, result.topic)
        assertEquals(true, result.resumesPreviousSituation)
    }

    @Test
    fun `lange Abwesenheit springt direkt in den aktuellen Tagesplan`() {
        val savedAt = 1_000_000L
        val result = PlayPresence.resolve(
            snapshot = PlayPresence.Snapshot(PlayScene.Place.PARK, AnimationType.MOVE, savedAt),
            nowMillis = savedAt + PlayPresence.SHORT_RETURN_MS + 1,
            now = LocalDateTime.of(2026, 8, 23, 23, 0)
        )

        assertEquals(PlayScene.Place.BEDROOM, result.place)
        assertEquals(AnimationType.SLEEP, result.topic)
        assertEquals(false, result.resumesPreviousSituation)
    }
}
