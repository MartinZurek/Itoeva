package com.notime.glyphsim.stream

import com.notime.glyphsim.matrix.PlayMap
import com.notime.glyphsim.matrix.PlayMapScene
import com.notime.glyphsim.matrix.PlayScene.Place
import org.junit.Assert.*
import org.junit.Test

class FennecWorldTest {
    @Test fun `nur Kartenansichten und echte Ziele werden dargestellt`() {
        assertEquals(FennecWorld.Presentation(null), FennecWorld.presentation("show_world", ""))
        assertEquals(FennecWorld.Presentation(Place.GROTTO), FennecWorld.presentation("show_place", "GROTTO"))
        listOf("travel", "complete_reminder", "execute", "help", "none").forEach {
            assertNull(FennecWorld.presentation(it, "FOREST"))
        }
        assertNull(FennecWorld.presentation("show_place", "PRIVATE_REMINDER"))
        assertNull(FennecWorld.presentation("show_world", "FOREST"))
    }

    @Test fun `Wegvorschau teilt den echten Weg und laesst den Ausgangsort stehen`() {
        val presentation = FennecWorld.Presentation(Place.GROTTO)
        assertEquals(listOf(Place.MOUNTAINS, Place.GROTTO), FennecWorld.route(Place.FOREST, presentation))
        for (from in Place.entries) for (to in Place.entries) {
            val route = FennecWorld.route(from, FennecWorld.Presentation(to))
            (listOf(from) + route).zipWithNext().forEach { (a, b) -> assertTrue(b in PlayMap.neighbors(a)) }
        }
        assertTrue(FennecWorld.route(Place.PARK, FennecWorld.Presentation(null)).isEmpty())
    }

    @Test fun `Kartenbeschriftung kennt alle Orte und fasst Zuhause zusammen`() {
        Place.entries.forEach {
            assertTrue(FennecWorld.name(it, false).isNotBlank())
            assertTrue(FennecWorld.name(it, true).isNotBlank())
        }
        val labels = PlayMapScene.labels(100, 50)
        assertTrue(labels.containsKey(Place.LIVING))
        assertFalse(labels.containsKey(Place.KITCHEN))
        assertEquals(Place.entries.filter { PlayMap.regionOf(it) != PlayMap.Region.HOME || it == Place.LIVING }.toSet(), labels.keys)
        labels.values.forEach { (x, y) -> assertTrue(x in 0 until 100 && y in 0 until 50) }
    }
}
