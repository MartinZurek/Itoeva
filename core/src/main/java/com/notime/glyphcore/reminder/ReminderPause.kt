package com.notime.glyphcore.reminder

import android.content.Context
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Erinnerungen ausdruecklich pausieren - **der ehrliche Ersatz fuer das, was der Spielmodus
 * vorher nebenbei tat.**
 *
 * ## Warum es das braucht
 *
 * Bis Phase 3 setzte das Einschalten der Welt die eigenen Routinen aus. Das war nirgends
 * angekuendigt, aber es war der einzige Weg, sie ueberhaupt stillzustellen - ausser jede einzelne
 * Erinnerung von Hand abzuschalten und danach wieder einzuschalten.
 *
 * Diesen Wunsch gibt es: man sitzt in einer Besprechung, man ist krank, man will einen Tag Ruhe.
 * Er ist nur falsch verpackt gewesen. Hier bekommt er einen eigenen Namen, ein sichtbares Ende und
 * keine Nebenwirkung auf die Welt des Wesens.
 *
 * ## Warum mit Ablaufzeitpunkt und nicht als Schalter
 *
 * Ein Schalter "Erinnerungen aus" wird eingeschaltet und vergessen. Danach kommt wochenlang
 * nichts, und die App gilt als kaputt - der haeufigste Weg, wie eine Erinnerungs-App still
 * aufhoert zu erinnern.
 *
 * Deshalb ist jede Pause **befristet**: sie traegt den Zeitpunkt, an dem sie von selbst endet.
 * "Bis auf Weiteres" gibt es bewusst nicht; wer dauerhaft Ruhe will, schaltet die betreffende
 * Erinnerung ab - das ist dieselbe Absicht, aber eine, die man in der Liste wiederfindet.
 *
 * ## Wo es wirkt
 *
 * Im Planer, nicht in der Oberflaeche: eine pausierte Erinnerung bekommt keinen Alarm. Nach
 * Ablauf plant der Watchdog sie von selbst wieder ein (er sucht ohnehin nach Erinnerungen ohne
 * Alarm - siehe [ReminderWatchdog]), es braucht also keinen eigenen Wecker fuer das Ende der Pause.
 *
 * Der Zustand liegt in SharedPreferences und nicht im Speicher, aus demselben Grund wie
 * [PlayModeState]: Planer und Alarm-Empfaenger laufen auch in kalt gestarteten Prozessen.
 */
object ReminderPause {

    private const val PREFS_NAME = "reminder_pause"
    private const val KEY_UNTIL = "paused_until_epoch_millis"

    /** Kein Ablauf gespeichert - es ist nichts pausiert. */
    private const val NO_PAUSE = 0L

    /**
     * Bis wann alle Erinnerungen ruhen, oder `null` wenn nichts pausiert ist.
     *
     * Ein abgelaufener Zeitpunkt zaehlt als "nicht pausiert" - die Pause endet von selbst, ohne
     * dass jemand aufraeumen muss.
     */
    fun pausedUntil(context: Context, nowMillis: Long = System.currentTimeMillis()): Long? {
        val until = prefs(context).getLong(KEY_UNTIL, NO_PAUSE)
        return until.takeIf { it > nowMillis }
    }

    /** Ob gerade pausiert ist. */
    fun isPaused(context: Context, nowMillis: Long = System.currentTimeMillis()): Boolean =
        pausedUntil(context, nowMillis) != null

    /** Pausiert bis zu einem festen Zeitpunkt. */
    fun pauseUntil(context: Context, epochMillis: Long) {
        prefs(context).edit().putLong(KEY_UNTIL, epochMillis).apply()
    }

    /** Hebt eine laufende Pause sofort auf. */
    fun resume(context: Context) {
        prefs(context).edit().remove(KEY_UNTIL).apply()
    }

    /**
     * Die angebotenen Dauern.
     *
     * Bewusst wenige und bewusst kurz. Lange Pausen sind der Weg, auf dem eine Erinnerungs-App
     * still verschwindet; wer laenger Ruhe braucht, ist mit dem Abschalten der Erinnerung besser
     * bedient, weil er sie danach in der Liste wiederfindet.
     */
    enum class Duration {
        /** Eine Stunde - fuer eine Besprechung oder ein Essen. */
        ONE_HOUR,

        /**
         * Bis zum naechsten Morgen.
         *
         * Endet um Mitternacht und nicht "in 24 Stunden": ein Tagesziel endet ebenfalls dort
         * (siehe AvatarMood), und der naechste Tag soll unbelastet anfangen.
         */
        UNTIL_TOMORROW
    }

    /**
     * Rechnet eine Dauer in einen Ablaufzeitpunkt um.
     *
     * Reine Rechnung mit hereingereichter Zeit und Zeitzone - dadurch ohne Geraet pruefbar
     * (siehe `ReminderPauseTest`), und die Umstellungstage verhalten sich wie ueberall sonst in
     * dieser App.
     */
    fun endOf(
        duration: Duration,
        nowMillis: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long = when (duration) {
        Duration.ONE_HOUR -> nowMillis + 60 * 60 * 1000L
        Duration.UNTIL_TOMORROW -> {
            val heute = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
            morgenFrueh(heute, zone)
        }
    }

    private fun morgenFrueh(heute: LocalDate, zone: ZoneId): Long =
        heute.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
