package com.notime.glyphsim

import android.app.Application
import com.notime.glyphcore.reminder.PlayModeState
import com.notime.glyphcore.reminder.ReminderHost
import com.notime.glyphcore.reminder.ReminderWatchdogWorker
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.data.CrashLog
import com.notime.glyphsim.matrix.PlayModeRoll
import com.notime.glyphsim.reminder.ReminderAlarmReceiver

/**
 * Verbindet den gemeinsamen Erinnerungs-Kern (:core) mit diesem Modul: welche Datenbank die
 * Erinnerungen haelt und welcher Empfaenger die faelligen Alarme entgegennimmt.
 *
 * Muss in `Application.onCreate()` passieren und nicht in einer Activity - der Alarm-Empfaenger
 * und der Boot-Empfaenger laufen auch in Prozessen, in denen nie eine Activity existiert hat
 * (siehe [ReminderHost]).
 */
class GlyphSimApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Als Erstes, damit auch Fehler beim Einrichten selbst noch aufgezeichnet werden.
        CrashLog.install(this)

        ReminderHost.install(
            ReminderAlarmReceiver::class.java,
            playModeReroll = PlayModeRoll::reroll
        ) { context ->
            AppDatabase.getInstance(context).glyphReminderDao()
        }

        // Die App startet immer im Normalbetrieb: Der Spielmodus ist eine Sitzung, die man
        // bewusst beginnt, kein Zustand, in dem man das Geraet aus der Hand legt. Bliebe er ueber
        // den Prozessstart hinweg an, fuehlten sich die eigenen Erinnerungen am naechsten Tag
        // grundlos verschwunden an - und stattdessen liefe im Alltag ein Spiel-Takt von wenigen
        // Minuten mit. Der Fortschritt (XP/Level) haengt nicht daran und bleibt erhalten.
        //
        // Vor [ReminderWatchdogWorker.enqueue] und vor jeder Activity: der Watchdog und der
        // Scheduler fragen den Modus ab und muessen den zurueckgesetzten Stand sehen.
        PlayModeState.setActive(this, false)
        // Faengt abgerissene Alarmketten auf - siehe ReminderWatchdog fuer die Ursachen.
        ReminderWatchdogWorker.enqueue(this)
    }
}
