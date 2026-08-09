package com.notime.glyphsim.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notime.glyphsim.matrix.ClockFrameSim
import java.time.LocalTime
import kotlinx.coroutines.delay

/**
 * Gemeinsamer Uhr-Frame fuer HomeScreen und DockScreen: liefert den aktuellen Uhrzeit-Frame im
 * gerade eingestellten Stil und aktualisiert ihn selbst weiter, solange [paused] false ist
 * (waehrend einer Erinnerungs-Animation uebernimmt der Aufrufer die Anzeige).
 *
 * **Warum minutengenau statt sekuendlich**: die Uhr zeigt HH:MM. Der vorherige `delay(1000L)`
 * baute den Frame trotzdem jede Sekunde neu und loeste jedes Mal eine Recomposition aus - 59 von
 * 60 Durchlaeufen erzeugten ein identisches Bild. Im Dock-Modus, der die ganze Nacht laufen soll,
 * waren das ueber acht Stunden rund 28.800 Wakeups statt der noetigen 480. Stattdessen wird jetzt
 * bis zur naechsten vollen Minute geschlafen (plus einem kleinen Sicherheitsaufschlag, damit der
 * Aufwachzeitpunkt sicher IN der neuen Minute liegt und nicht knapp davor - sonst zeigte der
 * Frame gelegentlich noch die alte Minute und die Anzeige haenge eine Minute hinterher).
 *
 * Der Stil kommt als Flow (siehe [ClockStylePrefs.observe]) statt bei jedem Tick frisch aus den
 * Preferences gelesen zu werden - dadurch schlaegt ein Stilwechsel sofort durch und nicht erst
 * beim naechsten Tick, der jetzt bis zu eine Minute entfernt sein kann.
 */
@Composable
fun rememberClockFrame(paused: Boolean): State<IntArray> {
    val context = LocalContext.current
    val style by ClockStylePrefs.observe(context).collectAsStateWithLifecycle(initialValue = ClockStylePrefs.get(context))
    val frameState = remember { mutableStateOf(ClockFrameSim.buildFrame(style = style)) }

    LaunchedEffect(style, paused) {
        if (paused) return@LaunchedEffect
        while (true) {
            frameState.value = ClockFrameSim.buildFrame(style = style)
            delay(millisUntilNextMinute())
        }
    }
    return frameState
}

/**
 * Restmillisekunden bis zur naechsten vollen Minute, plus [WAKEUP_MARGIN_MS]. Wird nie 0, damit
 * die Schleife auch bei einem exakt auf der Minutengrenze liegenden Aufruf nicht heisslaeuft.
 *
 * [now] ist parametrisiert (statt intern [LocalTime.now] zu lesen), damit sich die Zeitarithmetik
 * ohne Warten auf echte Uhrzeiten testen laesst - siehe ClockTickTest.
 */
internal fun millisUntilNextMinute(now: LocalTime = LocalTime.now()): Long {
    val elapsedInMinute = now.second * 1000L + now.nano / 1_000_000L
    return (60_000L - elapsedInMinute).coerceAtLeast(1L) + WAKEUP_MARGIN_MS
}

internal const val WAKEUP_MARGIN_MS = 50L
