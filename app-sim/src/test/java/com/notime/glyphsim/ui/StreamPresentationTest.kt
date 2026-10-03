package com.notime.glyphsim.ui

import com.notime.glyphsim.matrix.AvatarGeometry
import com.notime.glyphsim.matrix.SceneCell
import org.junit.Assert.*
import org.junit.Test

class StreamPresentationTest {
    @Test fun nightKeepsLightsAndEmptyCellsWhileRevealingScenery() {
        val cells = listOf(SceneCell(1, 2, 80), SceneCell(2, 2, 180, true), SceneCell(3, 2, 0))
        val result = StreamPresentation.readableNight(cells, true, 1f)
        assertTrue(result[0].brightness > cells[0].brightness)
        assertEquals(cells[1], result[1])
        assertEquals(cells[2], result[2])
        assertEquals(80, cells[0].brightness)
    }

    @Test fun daylightAndSceneTransitionsRemainIntact() {
        val cells = listOf(SceneCell(1, 2, 0), SceneCell(2, 2, AvatarGeometry.MAX_BRIGHTNESS))
        assertSame(cells, StreamPresentation.readableNight(cells, false, 1f))
        val result = StreamPresentation.readableNight(cells, true, 0f)
        assertEquals(0, result[0].brightness)
        assertEquals(AvatarGeometry.MAX_BRIGHTNESS, result[1].brightness)
    }

    @Test fun avatarRemainsReadableOnShortAndTallBroadcasts() {
        assertEquals(88f, StreamPresentation.avatarSizeDp(200f), 0.01f)
        assertEquals(120f, StreamPresentation.avatarSizeDp(400f), 0.01f)
        assertEquals(132f, StreamPresentation.avatarSizeDp(900f), 0.01f)
    }
}
