package com.notime.glyphsim.stream

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Eine gemeinsame Kanalzeit; explizite Ortsfragen verwenden denselben wirklichen Zeitpunkt. */
internal object StreamTime {
    val channelZone: ZoneId = ZoneId.of("Europe/Berlin")
    data class Reading(val id: String, val label: String, val time: String, val date: String, val offset: String)
    private val zones = linkedMapOf(
        "BERLIN" to ("Berlin" to channelZone), "UTC" to ("UTC" to ZoneId.of("UTC")),
        "NEW_YORK" to ("New York" to ZoneId.of("America/New_York")),
        "LONDON" to ("London" to ZoneId.of("Europe/London")),
        "TOKYO" to ("Tokyo" to ZoneId.of("Asia/Tokyo")),
        "LOS_ANGELES" to ("Los Angeles" to ZoneId.of("America/Los_Angeles")),
        "SYDNEY" to ("Sydney" to ZoneId.of("Australia/Sydney")),
        "KOLKATA" to ("Kolkata" to ZoneId.of("Asia/Kolkata"))
    )
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm")

    fun channelTime(instant: Instant = Instant.now()): LocalTime = instant.atZone(channelZone).toLocalTime()

    fun readings(instant: Instant = Instant.now()): List<Reading> = zones.map { (id, entry) ->
        val time = instant.atZone(entry.second)
        Reading(id, entry.first, time.format(timeFormat), time.toLocalDate().toString(), time.offset.id)
    }

    fun label(instant: Instant = Instant.now()): String {
        val readings = readings(instant)
        val channel = readings.first()
        return "Berlin · UTC${channel.offset.removeSuffix(":00")}\nUTC ${readings[1].time}"
    }
}
