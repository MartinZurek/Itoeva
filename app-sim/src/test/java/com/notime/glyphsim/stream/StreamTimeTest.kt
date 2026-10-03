package com.notime.glyphsim.stream

import java.time.Instant
import java.time.LocalTime
import org.junit.Assert.*
import org.junit.Test

class StreamTimeTest {
    @Test fun `Kanalzeit ist Berlin und UTC bleibt weltweit eindeutig`() {
        val at = Instant.parse("2026-10-03T09:00:00Z")
        assertEquals(LocalTime.of(11, 0), StreamTime.channelTime(at))
        assertEquals("Berlin · UTC+02\nUTC 09:00", StreamTime.label(at))
        val times = StreamTime.readings(at).associateBy { it.id }
        assertEquals("05:00", times.getValue("NEW_YORK").time)
        assertEquals("18:00", times.getValue("TOKYO").time)
        assertEquals("14:30", times.getValue("KOLKATA").time)
    }

    @Test fun `Sommerzeit springt automatisch und die doppelte Herbststunde bleibt unterscheidbar`() {
        fun berlin(at: String) = StreamTime.readings(Instant.parse(at)).first()
        assertEquals("01:59", berlin("2026-03-29T00:59:00Z").time)
        assertEquals("03:00", berlin("2026-03-29T01:00:00Z").time)
        val first = berlin("2026-10-25T00:30:00Z")
        val second = berlin("2026-10-25T01:30:00Z")
        assertEquals("02:30", first.time)
        assertEquals(first.time, second.time)
        assertEquals("+02:00", first.offset)
        assertEquals("+01:00", second.offset)
    }

    @Test fun `Ortszeiten tragen ihr eigenes Datum und wechseln Sommerzeit getrennt`() {
        val times = StreamTime.readings(Instant.parse("2026-10-03T23:30:00Z")).associateBy { it.id }
        assertEquals("2026-10-04", times.getValue("TOKYO").date)
        assertEquals("2026-10-03", times.getValue("NEW_YORK").date)
        fun sydney(at: String) = StreamTime.readings(Instant.parse(at)).first { it.id == "SYDNEY" }
        assertEquals("01:59", sydney("2026-10-03T15:59:00Z").time)
        assertEquals("03:00", sydney("2026-10-03T16:00:00Z").time)
    }
}
