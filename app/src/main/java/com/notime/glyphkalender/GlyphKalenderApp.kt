package com.notime.glyphkalender

import android.app.Application
import com.notime.glyphcore.reminder.ReminderHost
import com.notime.glyphcore.reminder.ReminderWatchdogWorker
import com.notime.glyphkalender.data.AppDatabase
import com.notime.glyphkalender.reminder.ReminderAlarmReceiver

/**
 * Verbindet den gemeinsamen Erinnerungs-Kern (:core) mit diesem Modul: welche Datenbank die
 * Erinnerungen haelt und welcher Empfaenger die faelligen Alarme entgegennimmt.
 *
 * Muss in `Application.onCreate()` passieren und nicht in einer Activity - der Alarm-Empfaenger
 * und der Boot-Empfaenger laufen auch in Prozessen, in denen nie eine Activity existiert hat
 * (siehe [ReminderHost]).
 *
 * Ein Profil im Sinne des Kerns gibt es hier bewusst nicht: diese App steuert die echte
 * Glyph-Matrix an und kennt keine Avatare, es bleibt daher beim Standardprofil (siehe
 * [com.notime.glyphcore.reminder.ActiveProfilePrefs]).
 */
class GlyphKalenderApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderHost.install(ReminderAlarmReceiver::class.java) { context ->
            AppDatabase.getInstance(context).glyphReminderDao()
        }
        // Faengt abgerissene Alarmketten auf - siehe ReminderWatchdog fuer die Ursachen.
        ReminderWatchdogWorker.enqueue(this)
    }
}
