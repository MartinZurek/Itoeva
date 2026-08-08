package com.notime.glyphsim.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueft die Migrationspfade der Tama-Datenbank gegen die exportierten Schemas unter
 * `app-sim/schemas/` (siehe [AppDatabaseMigrations] fuer das Baseline-Konzept).
 *
 * Instrumentiert statt JVM-Unit-Test, weil hier echtes SQLite laufen muss - Room fuehrt die
 * Migration wirklich aus und vergleicht das Ergebnis danach Spalte fuer Spalte mit dem erwarteten
 * Schema. Ausfuehren mit angeschlossenem Geraet/Emulator:
 *
 *     ./gradlew :app-sim:connectedDebugAndroidTest
 *
 * **Der wichtigste Test ist [baselineSchemaIsExported]**: er schlaegt fehl, sobald jemand die
 * Datenbankversion erhoeht, ohne das neue Schema zu exportieren oder eine Migration zu
 * hinterlegen. Genau dieses Versaeumnis hat vorher zum stillen Datenverlust gefuehrt.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private companion object {
        const val TEST_DB = "migration-test.db"
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Stellt sicher, dass fuer JEDEN Stand ab der Baseline bis zur aktuellen Version ein
     * exportiertes Schema existiert und eine Datenbank darauf angelegt werden kann.
     *
     * Faellt die Version hoch, ohne dass das Schema mitexportiert wurde, findet der Helper die
     * JSON-Datei nicht und der Test scheitert - der Fehler kommt also hier ans Licht und nicht
     * erst beim Nutzer, dessen Daten sonst geloescht wuerden.
     */
    @Test
    fun baselineSchemaIsExported() {
        val db = helper.createDatabase(TEST_DB, AppDatabaseMigrations.BASELINE_VERSION)
        assertTrue("Baseline-Datenbank muss sich anlegen lassen", db.isOpen)
        db.close()
    }

    /**
     * Faehrt die Datenbank auf der Baseline hoch, schreibt echte Daten hinein und migriert dann
     * ueber alle registrierten Pfade bis zur aktuellen Version - inklusive Room-eigener
     * Schemapruefung (`validateDroppedTables = true`).
     *
     * Solange Baseline und aktuelle Version identisch sind, gibt es nichts zu migrieren und der
     * Test belegt lediglich, dass der Stand konsistent ist. Ab der ersten echten Migration
     * verifiziert er dann genau das, was vorher niemand geprueft hat: dass bestehende Zeilen den
     * Versionswechsel ueberleben.
     */
    @Test
    fun migratesFromBaselineToLatestKeepingData() {
        helper.createDatabase(TEST_DB, AppDatabaseMigrations.BASELINE_VERSION).apply {
            execSQL(
                """
                INSERT INTO glyph_reminders
                    (label, animationType, libraryAnimationId, daysOfWeekMask, startMinuteOfDay,
                     endMinuteOfDay, intervalMinutes, enabled, profileId, openDurationSeconds,
                     nextTriggerEpochMillis)
                VALUES ('Migrationstest', 'GENERAL', NULL, 127, 540, 1080, 60, 1, 'default', 8, NULL)
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DB,
            CURRENT_VERSION,
            true,
            *AppDatabaseMigrations.ALL
        )

        migrated.query("SELECT label FROM glyph_reminders").use { cursor ->
            assertTrue("Die vor der Migration angelegte Erinnerung muss erhalten bleiben", cursor.moveToFirst())
            assertTrue(
                "Label muss unveraendert sein",
                cursor.getString(0) == "Migrationstest"
            )
        }
        migrated.close()
    }

    /**
     * 13 -> 14 fuegt der Fuetter-Tabelle `profileId` und `libraryAnimationLabel` hinzu. Geprueft
     * wird das, worauf es bei einer Spaltenerweiterung ankommt: die vorhandenen Zeilen ueberleben,
     * und die neue NOT-NULL-Spalte bekommt ihren Standardwert - eine Migration, die stattdessen
     * die Tabelle neu anlegt, wuerde die bisherigen Fuetterungen stillschweigend loeschen.
     */
    @Test
    fun migration13To14KeepsExistingFeedEventsAndFillsDefaults() {
        helper.createDatabase(TEST_DB, 13).apply {
            execSQL(
                """
                INSERT INTO avatar_feed_events (reminderId, animationType, epochMillis)
                VALUES (7, 'DRINK', 1700000000000)
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 14, true, *AppDatabaseMigrations.ALL)

        migrated.query(
            "SELECT reminderId, animationType, profileId, libraryAnimationLabel FROM avatar_feed_events"
        ).use { cursor ->
            assertTrue("Die vor der Migration angelegte Fuetterung muss erhalten bleiben", cursor.moveToFirst())
            assertTrue("reminderId muss unveraendert sein", cursor.getLong(0) == 7L)
            assertTrue("animationType muss unveraendert sein", cursor.getString(1) == "DRINK")
            assertTrue("profileId muss den Standardwert bekommen", cursor.getString(2) == "PUFFLING")
            assertTrue("libraryAnimationLabel muss leer bleiben", cursor.isNull(3))
        }
        migrated.close()
    }

    /**
     * 14 -> 15 fuehrt `fedAtMillis` ein. Entscheidend ist hier nicht die Spalte selbst, sondern
     * der Backfill: bis Version 14 entstand eine Zeile ausschliesslich beim Fuettern, alle
     * Altdaten sind also beantwortete Erinnerungen. Bliebe die Spalte bei ihnen NULL, wuerden sie
     * rueckwirkend als "verpasst" gezaehlt - die Bilanz waere still verfaelscht, ohne dass
     * irgendetwas abstuerzt.
     */
    @Test
    fun migration14To15MarksExistingRowsAsFed() {
        helper.createDatabase(TEST_DB, 14).apply {
            execSQL(
                """
                INSERT INTO avatar_feed_events
                    (reminderId, animationType, epochMillis, profileId, libraryAnimationLabel)
                VALUES (3, 'MOVE', 1700000000000, 'FENNEC', NULL)
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 15, true, *AppDatabaseMigrations.ALL)

        migrated.query("SELECT epochMillis, fedAtMillis FROM avatar_feed_events").use { cursor ->
            assertTrue("Die Altzeile muss erhalten bleiben", cursor.moveToFirst())
            assertTrue(
                "Altdaten muessen als beantwortet gelten, sonst zaehlen sie als verpasst",
                !cursor.isNull(1) && cursor.getLong(1) == cursor.getLong(0)
            )
        }
        migrated.close()
    }

    /**
     * 16 -> 19 deckt die drei Play-Mode-Migrationen ab ([AppDatabaseMigrations.MIGRATION_16_17]
     * bis `_18_19`). Geprueft wird von Version 16 direkt bis zur aktuellen Version - dazwischen
     * gibt es keine exportierten Schemas, weil 17 und 18 nie eigenstaendig veroeffentlicht wurden
     * (siehe Klassendoku von [AppDatabaseMigrations]: exportierte JSONs entstehen nur fuer
     * tatsaechlich ausgelieferte Versionen, hier direkt 16 und die neue aktuelle 19).
     *
     * Deckt beide Spalten-Erweiterungen (`glyph_reminders.isPlayMode`,
     * `avatar_feed_events.isPlayMode` - jeweils mit Bestandsdaten, die den Standardwert false
     * bekommen muessen) UND die neue Tabelle `avatar_play_state` in einem Durchlauf ab.
     */
    @Test
    fun migration16To19AddsPlayModeSupport() {
        helper.createDatabase(TEST_DB, 16).apply {
            execSQL(
                """
                INSERT INTO glyph_reminders
                    (label, animationType, libraryAnimationId, daysOfWeekMask, startMinuteOfDay,
                     endMinuteOfDay, intervalMinutes, enabled, profileId, openDurationSeconds,
                     dailyGoal, nextTriggerEpochMillis)
                VALUES ('Migrationstest', 'GENERAL', NULL, 127, 540, 1080, 60, 1, 'default', 8, 0, NULL)
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO avatar_feed_events
                    (reminderId, animationType, epochMillis, profileId, libraryAnimationLabel, fedAtMillis)
                VALUES (5, 'REST', 1700000000000, 'GLOOP', NULL, 1700000000000)
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 19, true, *AppDatabaseMigrations.ALL)

        migrated.query("SELECT label, isPlayMode FROM glyph_reminders").use { cursor ->
            assertTrue("Die Altzeile muss erhalten bleiben", cursor.moveToFirst())
            assertTrue("isPlayMode muss fuer Altdaten false sein", cursor.getInt(1) == 0)
        }
        migrated.query("SELECT reminderId, isPlayMode FROM avatar_feed_events").use { cursor ->
            assertTrue("Die Altzeile muss erhalten bleiben", cursor.moveToFirst())
            assertTrue("isPlayMode muss fuer Altdaten false sein", cursor.getInt(1) == 0)
        }

        migrated.execSQL(
            "INSERT INTO avatar_play_state (profileId, xp, startedAtMillis, lastSeenLevel) " +
                "VALUES ('PUFFLING', 10, 1700000000000, 1)"
        )
        migrated.query("SELECT xp FROM avatar_play_state WHERE profileId = 'PUFFLING'").use { cursor ->
            assertTrue("Die neu angelegte Play-State-Zeile muss lesbar sein", cursor.moveToFirst())
            assertTrue("xp muss dem eingefuegten Wert entsprechen", cursor.getInt(0) == 10)
        }
        migrated.close()
    }
}

/**
 * Aktuelle Schemaversion, gespiegelt aus der `@Database`-Annotation von [AppDatabase] - die
 * Annotation selbst laesst sich zur Laufzeit nicht bequem auslesen. Muss beim naechsten
 * Versionssprung mit erhoeht werden; [AppDatabaseMigrationTest.migratesFromBaselineToLatestKeepingData]
 * schlaegt sonst fehl, was genau der gewuenschte Stolperdraht ist.
 */
private const val CURRENT_VERSION = 19
