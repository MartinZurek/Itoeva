package com.notime.glyphkalender.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.notime.glyphcore.data.BuiltInAnimationSelection
import com.notime.glyphcore.data.BuiltInAnimationSelectionDao
import com.notime.glyphcore.data.Converters
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphcore.data.GlyphReminderDao
import com.notime.glyphcore.data.LibraryAnimation
import com.notime.glyphcore.data.LibraryAnimationDao

/**
 * Entities und DAOs liegen im gemeinsamen Kern (:core), die `@Database`-Klasse bleibt bewusst
 * pro App-Modul: :app-sim fuehrt zusaetzlich eine Tabelle fuer Tamagotchi-Fuetterungen, die
 * diese App nicht braucht. Den Zugriff auf die geteilte Erinnerungs-Tabelle reicht
 * [com.notime.glyphkalender.GlyphKalenderApp] ueber
 * [com.notime.glyphcore.reminder.ReminderHost] an den Kern weiter.
 *
 * Version 18: [GlyphReminder] hat mit dem Umzug in den Kern zwei Felder bekommen - `profileId`
 * (hier immer das Standardprofil, siehe GlyphKalenderApp) und `openDurationSeconds`.
 */
@Database(
    entities = [GlyphReminder::class, LibraryAnimation::class, BuiltInAnimationSelection::class],
    version = 19,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun glyphReminderDao(): GlyphReminderDao
    abstract fun libraryAnimationDao(): LibraryAnimationDao
    abstract fun builtInAnimationSelectionDao(): BuiltInAnimationSelectionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "glyphkalender.db"
                )
                    .addMigrations(*AppDatabaseMigrations.ALL)
                    // Destruktives Zuruecksetzen nur noch fuer die Staende vor der Baseline
                    // (siehe AppDatabaseMigrations) - ab Version 18 braucht jede Schemaaenderung
                    // einen echten Migrationspfad, sonst scheitert das Oeffnen sofort.
                    .fallbackToDestructiveMigrationFrom(
                        true,
                        *AppDatabaseMigrations.PRE_BASELINE_VERSIONS
                    )
                    .build().also { instance = it }
            }
    }
}
