package com.notime.glyphsim

import android.app.Application
import com.notime.glyphcore.reminder.ActiveProfilePrefs
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

        // **Es gibt genau einen Erinnerungs-Satz, und er gehoert dem Nutzer** (siehe
        // ui/RoutineOwner). Der Kern liest das nicht dort, sondern in seinen eigenen
        // Einstellungen - Planer und Watchdog laufen aus kalt gestarteten Prozessen und kennen
        // die Avatar-Schicht gar nicht.
        //
        // Bis Phase 4b spiegelte AvatarSpeciesPrefs den Avatarnamen hierher. Auf Geraeten, die
        // schon liefen, steht dort also noch "PUFFLING" oder "GLOOP", waehrend die Erinnerungen
        // nach der Migration unter dem Standardprofil liegen. Ohne dieses Zuruecksetzen plante
        // der Kern fuer ein Profil, in dem nichts mehr steht - es kaeme schlicht nie wieder eine
        // Erinnerung, und niemand wuerde die Ursache in einem vergessenen Einstellungswert
        // suchen.
        //
        // Bei jedem Start statt einmalig: das heilt sich damit selbst, egal auf welchem Weg der
        // Wert je wieder verstellt wuerde. Vor [ReminderWatchdogWorker.enqueue] und vor jeder
        // Activity, aus demselben Grund wie der Modus darueber.
        ActiveProfilePrefs.set(this, ActiveProfilePrefs.DEFAULT_PROFILE_ID)

        // Faengt abgerissene Alarmketten auf - siehe ReminderWatchdog fuer die Ursachen.
        ReminderWatchdogWorker.enqueue(this)
    }
}
