package com.notime.glyphsim.data

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
 * Die Datenbank bleibt bewusst pro App-Modul, obwohl Entities und DAOs im gemeinsamen Kern
 * (:core) liegen: nur dieses Modul kennt [AvatarFeedEvent] (Tamagotchi-Fuetterungen). Eine
 * gemeinsame `@Database`-Klasse wuerde dem :app-Modul diese Tabelle aufzwingen, obwohl es gar
 * keine Avatare hat. Den Zugriff auf die geteilte Erinnerungs-Tabelle reicht
 * [com.notime.glyphsim.GlyphSimApp] ueber
 * [com.notime.glyphcore.reminder.ReminderHost] an den Kern weiter.
 */
@Database(
    entities = [
        GlyphReminder::class,
        LibraryAnimation::class,
        BuiltInAnimationSelection::class,
        AvatarFeedEvent::class,
        AvatarPlayState::class,
        AvatarUnlockedNode::class
    ],
    version = 23,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun glyphReminderDao(): GlyphReminderDao
    abstract fun libraryAnimationDao(): LibraryAnimationDao
    abstract fun builtInAnimationSelectionDao(): BuiltInAnimationSelectionDao
    abstract fun avatarFeedEventDao(): AvatarFeedEventDao
    abstract fun avatarPlayStateDao(): AvatarPlayStateDao
    abstract fun avatarUnlockedNodeDao(): AvatarUnlockedNodeDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "glyphsim.db"
                )
                    .addMigrations(*AppDatabaseMigrations.all(context.applicationContext))
                    // NUR noch fuer die Staende vor der Baseline, nicht mehr pauschal fuer alles:
                    // vorher hat jede Schemaaenderung stillschweigend alle Nutzerdaten geloescht.
                    // Jetzt scheitert ein fehlender Migrationspfad ab Version 13 laut und
                    // sofort - im Test statt beim Nutzer.
                    .fallbackToDestructiveMigrationFrom(
                        true,
                        *AppDatabaseMigrations.PRE_BASELINE_VERSIONS
                    )
                    .build().also { instance = it }
            }
    }
}
