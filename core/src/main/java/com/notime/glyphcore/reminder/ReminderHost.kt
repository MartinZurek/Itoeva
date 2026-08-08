package com.notime.glyphcore.reminder

import android.content.BroadcastReceiver
import android.content.Context
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphcore.data.GlyphReminderDao

/**
 * Die beiden Dinge, die der gemeinsame Erinnerungs-Kern vom jeweiligen App-Modul braucht.
 *
 * **Warum ueberhaupt ein Einhaengepunkt?** Zwei Dinge lassen sich bewusst nicht teilen:
 *
 * 1. **Die Datenbank.** Die Module fuehren unterschiedliche Entities (:app-sim zusaetzlich
 *    `AvatarFeedEvent`); eine gemeinsame `@Database`-Klasse wuerde dem jeweils anderen Modul
 *    Tabellen aufzwingen, die es nicht braucht. Jedes Modul behaelt daher seine eigene
 *    Datenbank und reicht hier nur den Zugriff auf die gemeinsame Erinnerungs-Tabelle herein.
 * 2. **Der Alarm-Empfaenger.** Er ist pro Modul im Manifest registriert und stellt die
 *    Erinnerung voellig unterschiedlich dar - :app auf der echten Glyph-Matrix-Hardware,
 *    :app-sim auf Widget und simulierter Matrix. [ReminderScheduler] muss den passenden
 *    `PendingIntent` bauen und braucht dafuer die konkrete Klasse.
 *
 * [install] MUSS aus `Application.onCreate()` aufgerufen werden, nicht aus einer Activity:
 * [ReminderScheduler] laeuft auch in kalt gestarteten Prozessen (AlarmManager, BOOT_COMPLETED),
 * in denen nie eine Activity existiert hat. `Application.onCreate` ist der einzige Punkt, der
 * davor garantiert durchlaufen wird.
 */
object ReminderHost {

    private data class Config(
        val daoProvider: (Context) -> GlyphReminderDao,
        val alarmReceiver: Class<out BroadcastReceiver>,
        val playModeReroll: suspend (Context, GlyphReminder) -> GlyphReminder
    )

    @Volatile
    private var config: Config? = null

    /**
     * [playModeReroll] wird nur vom :app-sim-Modul gebraucht (Play Mode kennt nur dieses -
     * siehe [GlyphReminder.isPlayMode]): fuer jede Play-Mode-Erinnerung wuerfelt es bei jeder
     * Neuplanung ein neues, zum Avatar-Charakter passendes Thema samt Intervall. Default ist
     * die Identitaet, damit das :app-Modul (keine Avatare) [install] unveraendert aufrufen kann.
     *
     * `suspend`, weil die Wahl vom Spielstand des Avatars abhaengt und der aus der Datenbank
     * gelesen werden muss (siehe `PlayModeRoll`/`PlayGamePlans` im :app-sim-Modul).
     */
    fun install(
        alarmReceiver: Class<out BroadcastReceiver>,
        playModeReroll: suspend (Context, GlyphReminder) -> GlyphReminder = { _, reminder -> reminder },
        daoProvider: (Context) -> GlyphReminderDao
    ) {
        config = Config(daoProvider, alarmReceiver, playModeReroll)
    }

    fun dao(context: Context): GlyphReminderDao =
        requireConfig().daoProvider(context.applicationContext)

    fun alarmReceiver(): Class<out BroadcastReceiver> = requireConfig().alarmReceiver

    /** Siehe [Config.playModeReroll]. */
    suspend fun reroll(context: Context, reminder: GlyphReminder): GlyphReminder =
        requireConfig().playModeReroll(context, reminder)

    private fun requireConfig(): Config {
        val current = config
        checkNotNull(current) {
            "ReminderHost.install(...) wurde nicht aufgerufen - erwartet in Application.onCreate()"
        }
        return current
    }
}
