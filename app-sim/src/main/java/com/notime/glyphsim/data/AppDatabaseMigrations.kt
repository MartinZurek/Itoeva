package com.notime.glyphsim.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrationspfade der Tama-Datenbank.
 *
 * **Warum es hier bei Version 13 losgeht:** bis einschliesslich Version 12 lief die Datenbank mit
 * `exportSchema = false` und einem pauschalen `fallbackToDestructiveMigration`. Damit existiert von
 * keinem dieser Staende eine Schemabeschreibung, und ohne den alten Stand laesst sich keine
 * Migration formulieren (Room muss wissen, WOVON es migriert) - die Staende sind nicht
 * rekonstruierbar, auch nicht nachtraeglich. Version 13 ist deshalb die **Baseline**: der erste
 * Stand, dessen Schema als JSON unter `app-sim/schemas/` liegt und mit eingecheckt wird.
 *
 * **Was das praktisch aendert:** vorher hat JEDE Schemaaenderung beim naechsten App-Start
 * kommentarlos die komplette Datenbank geloescht - alle Erinnerungen, Profile und die
 * Fuetterungs-Historie. Ab jetzt gilt das nur noch fuer Installationen unterhalb der Baseline
 * ([PRE_BASELINE_VERSIONS], praktisch nur alte Entwicklungsstaende auf dem eigenen Geraet). Ab
 * Version 13 fuehrt eine Schemaaenderung ohne passenden Eintrag hier zu einem sofortigen,
 * lautstarken `IllegalStateException` beim Oeffnen der Datenbank - der Fehler faellt also in der
 * Entwicklung auf, statt beim Nutzer stillschweigend Daten zu vernichten.
 *
 * **Vorgehen bei der naechsten Schemaaenderung:**
 * 1. Entity aendern und `version` in [AppDatabase] um 1 erhoehen.
 * 2. Bauen - Room legt automatisch `app-sim/schemas/…/14.json` an (mit einchecken).
 * 3. Hier eine `Migration(13, 14)` ergaenzen und in [ALL] eintragen.
 * 4. In `AppDatabaseMigrationTest` einen Testfall dafuer ergaenzen.
 *
 * Aktuell ist [ALL] leer, weil Version 13 zugleich Baseline und aktueller Stand ist - der erste
 * Eintrag entsteht mit der naechsten Schemaaenderung.
 */
object AppDatabaseMigrations {

    /** Erster Stand mit exportiertem Schema - siehe Klassendoku. */
    const val BASELINE_VERSION = 13

    /**
     * Staende ohne exportiertes Schema. Nur fuer sie ist das destruktive Zuruecksetzen noch
     * erlaubt; alles ab [BASELINE_VERSION] braucht einen echten Migrationspfad.
     */
    val PRE_BASELINE_VERSIONS: IntArray = (1 until BASELINE_VERSION).toList().toIntArray()

    /**
     * 13 -> 14: [AvatarFeedEvent] bekommt `profileId` (welcher Avatar wurde gefuettert) und
     * `libraryAnimationLabel` (womit, falls es eine Bibliotheks-Animation war). Grundlage der
     * Fuetter-Statistik - vorher liess sich weder nach Avatar noch nach Animation auswerten.
     *
     * Reines Hinzufuegen von Spalten, die bestehenden Zeilen bleiben erhalten. `profileId` ist
     * NOT NULL und braucht deshalb einen Standardwert: Altdaten stammen aus der Zeit vor der
     * Avatar-Zuordnung und werden PUFFLING zugeschlagen, dem Standard-Avatar - unter dem sie
     * mit grosser Wahrscheinlichkeit auch entstanden sind.
     */
    private val MIGRATION_13_14 = object : Migration(13, 14) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE avatar_feed_events ADD COLUMN profileId TEXT NOT NULL DEFAULT 'PUFFLING'"
            )
            db.execSQL(
                "ALTER TABLE avatar_feed_events ADD COLUMN libraryAnimationLabel TEXT"
            )
        }
    }

    /**
     * 14 -> 15: `fedAtMillis` kommt dazu. Ab hier legt JEDE ausgeloeste Erinnerung eine Zeile an,
     * nicht mehr nur die beantworteten - dadurch laesst sich auch zaehlen, was uebergangen wurde
     * (siehe [AvatarFeedEvent.fedAtMillis]).
     *
     * **Der Backfill ist hier das Wesentliche**, nicht die neue Spalte: bis Version 14 entstand
     * eine Zeile ausschliesslich beim Fuettern. Alle Altdaten sind also beantwortete
     * Erinnerungen. Bliebe `fedAtMillis` bei ihnen NULL, wuerde die neue Auswertung sie
     * rueckwirkend als "verpasst" ausweisen - aus einer lueckenlosen Bilanz wuerde eine falsche.
     * Deshalb wird der Ausloesezeitpunkt als Reaktionszeitpunkt uebernommen.
     */
    private val MIGRATION_14_15 = object : Migration(14, 15) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE avatar_feed_events ADD COLUMN fedAtMillis INTEGER")
            db.execSQL("UPDATE avatar_feed_events SET fedAtMillis = epochMillis")
        }
    }

    /**
     * 15 -> 16: `dailyGoal` an der Erinnerung (siehe [com.notime.glyphcore.data.GlyphReminder]).
     *
     * Standardwert 0 bedeutet "kein Ziel" - bestehende Erinnerungen bleiben damit reine Stupser
     * und gehen weiterhin nicht in die Stimmung ein. Ein Update aendert also nichts an dem, was
     * der Nutzer bisher eingestellt hatte.
     */
    private val MIGRATION_15_16 = object : Migration(15, 16) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE glyph_reminders ADD COLUMN dailyGoal INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * 16 -> 17: [com.notime.glyphcore.data.GlyphReminder] bekommt `isPlayMode` - markiert die
     * eine, vom Play Mode selbst verwaltete Erinnerungs-Zeile je Avatar (siehe
     * ui/PlayModeViewModel). Standardwert 0: bestehende Erinnerungen sind alle vom Nutzer
     * eingerichtet und bleiben es.
     */
    private val MIGRATION_16_17 = object : Migration(16, 17) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE glyph_reminders ADD COLUMN isPlayMode INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * 17 -> 18: [AvatarFeedEvent] bekommt `isPlayMode`, uebernommen vom Reminder zum Zeitpunkt
     * der Ausloesung (siehe reminder/ReminderTrigger.show). Standardwert 0: Altdaten stammen
     * alle aus der Zeit vor dem Play Mode.
     */
    private val MIGRATION_17_18 = object : Migration(17, 18) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE avatar_feed_events ADD COLUMN isPlayMode INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * 18 -> 19: neue Tabelle [AvatarPlayState] - XP/Level je Avatar im Play Mode. Keine
     * bestehenden Daten betroffen, reines Anlegen einer neuen, anfangs leeren Tabelle.
     */
    private val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS avatar_play_state (" +
                    "profileId TEXT NOT NULL PRIMARY KEY, " +
                    "xp INTEGER NOT NULL DEFAULT 0, " +
                    "startedAtMillis INTEGER NOT NULL, " +
                    "lastSeenLevel INTEGER NOT NULL DEFAULT 1)"
            )
        }
    }

    /** Alle bekannten Migrationen, in [AppDatabase] registriert. */
    val ALL: Array<Migration> = arrayOf(
        MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16,
        MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19
    )
}
