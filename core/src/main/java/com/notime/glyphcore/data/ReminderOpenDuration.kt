package com.notime.glyphcore.data

/**
 * Wie lange eine faellige Erinnerung "offen" bleibt, also wie lange ihre Animation laeuft,
 * bevor die Uhr wieder normal weitertickt (siehe [GlyphReminder.openDurationSeconds]).
 *
 * Die Dauer ist eine Obergrenze "falls niemand reagiert", keine feste Laufzeit: eine
 * Nutzerreaktion bricht die Animation jederzeit vorzeitig ab (im :app-sim-Modul das Fuettern
 * des Avatars). [UNTIL_FED] treibt das auf die Spitze - die Animation laeuft endlos weiter,
 * bis reagiert wird.
 *
 * [DEFAULT_SECONDS] entspricht der frueher fest verdrahteten Laufzeit von 8 Sekunden und bleibt
 * Voreinstellung, damit bestehende Erinnerungen sich nicht ploetzlich anders verhalten.
 */
object ReminderOpenDuration {

    /** Sentinel fuer "laeuft endlos, bis der Nutzer reagiert". */
    const val UNTIL_FED = -1

    /** Bisheriges Verhalten (kurzes Aufblinken), bleibt Voreinstellung fuer neue Erinnerungen. */
    const val DEFAULT_SECONDS = 8

    /**
     * Entsprechung von [UNTIL_FED] in Millisekunden - die Abspiel-Logik der Apps erkennt daran
     * die endlose Variante. Hier definiert statt in einem App-Modul, damit der gemeinsame Kern
     * nicht auf die jeweilige Abspiel-Implementierung zeigen muss.
     */
    const val UNTIL_FED_MILLIS = -1L

    /** Auswahl fuer den Erinnerungs-Dialog, in der Reihenfolge kurz -> lang -> endlos. */
    val OPTIONS: List<Int> = listOf(
        DEFAULT_SECONDS,
        60,
        5 * 60,
        10 * 60,
        15 * 60,
        20 * 60,
        UNTIL_FED
    )

    fun label(seconds: Int): String = when {
        seconds == UNTIL_FED -> "Nonstop"
        seconds < 60 -> "$seconds s"
        else -> "${seconds / 60} min"
    }

    /** Laufzeit in Millisekunden, oder [UNTIL_FED_MILLIS] fuer die endlose Variante. */
    fun toDurationMillis(seconds: Int): Long =
        if (seconds == UNTIL_FED) UNTIL_FED_MILLIS else seconds * 1000L

    /**
     * Klemmt eine Offen-Dauer auf hoechstens die Laenge des eigenen Intervalls. Eine Erinnerung,
     * die laenger offen bleibt als bis zu ihrer eigenen naechsten Faelligkeit, blockiert im
     * :app-sim-Modul (ReminderTrigger.firePending) jede andere Erinnerung, bis sie irgendwann -
     * ungedeckelt womoeglich erst nach vielen Minuten - endet; alles, was waehrenddessen faellig
     * war, ist zu dem Zeitpunkt schon laenger als das Nachhol-Fenster her und wird kommentarlos
     * uebersprungen statt verspaetet gezeigt zu werden. [UNTIL_FED] ist per Definition unbegrenzt
     * und faellt daher immer unter diese Klemmung - "Nonstop" ist dadurch keine waehlbare
     * Offen-Dauer mehr.
     */
    fun coerceToInterval(seconds: Int, intervalMinutes: Int): Int {
        val maxSeconds = intervalMinutes.coerceAtLeast(1) * 60
        return if (seconds == UNTIL_FED || seconds > maxSeconds) maxSeconds else seconds
    }
}
