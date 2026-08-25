package com.notime.glyphsim.data

import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
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
            *MIGRATIONS
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

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 14, true, *MIGRATIONS)

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

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 15, true, *MIGRATIONS)

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

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 19, true, *MIGRATIONS)

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

    /**
     * 15 -> 16 fuegt `dailyGoal` hinzu - die letzte Einzelgrenze, fuer die es hier bisher keinen
     * Test gab (13->14, 14->15 und 16->19 waren abgedeckt).
     *
     * Entscheidend ist der Standardwert. Die Spalte ist NOT NULL, bestehende Zeilen haben aber
     * keinen Wert dafuer. Bekaemen sie irgendetwas anderes als 0
     * ([com.notime.glyphcore.data.NO_GOAL]), traege jede Alt-Erinnerung ploetzlich ein Tagesziel,
     * das nie jemand gesetzt hat - und wuerde damit ungefragt in die Stimmungsberechnung des
     * Avatars eingehen. Nichts wuerde abstuerzen; der Avatar waere nur ohne Grund traurig.
     */
    @Test
    fun migration15To16AddsDailyGoalWithNoGoalDefault() {
        helper.createDatabase(TEST_DB, 15).apply {
            execSQL(
                """
                INSERT INTO glyph_reminders
                    (label, animationType, libraryAnimationId, daysOfWeekMask, startMinuteOfDay,
                     endMinuteOfDay, intervalMinutes, enabled, profileId, openDurationSeconds,
                     nextTriggerEpochMillis)
                VALUES ('Ohne Ziel', 'DRINK', NULL, 31, 480, 1020, 30, 1, 'PUFFLING', 8, NULL)
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 16, true, *MIGRATIONS)

        migrated.query("SELECT label, dailyGoal FROM glyph_reminders").use { cursor ->
            assertTrue("Die Alt-Erinnerung muss erhalten bleiben", cursor.moveToFirst())
            assertTrue("Label muss unveraendert sein", cursor.getString(0) == "Ohne Ziel")
            assertTrue("Alt-Erinnerungen duerfen kein Tagesziel geschenkt bekommen", cursor.getInt(1) == 0)
        }
        migrated.close()
    }

    // --- 19 -> 20: die Zusammenlegung ----------------------------------------------------------

    /**
     * Legt eine 19er-Datenbank an und fuellt sie.
     *
     * **In genau einem Aufruf**, weil `MigrationTestHelper.createDatabase` eine vorhandene Datei
     * loescht - zweimal aufgerufen waere alles aus dem ersten Durchgang wieder weg, und die Tests
     * liefen gruen gegen eine leere Datenbank.
     */
    private fun createVersion19(vararg statements: String) {
        helper.createDatabase(TEST_DB, 19).apply {
            statements.forEach { execSQL(it) }
            close()
        }
    }

    private fun reminderRow(profileId: String, label: String, isPlayMode: Int = 0) =
        "INSERT INTO glyph_reminders (label, animationType, libraryAnimationId, daysOfWeekMask, " +
            "startMinuteOfDay, endMinuteOfDay, intervalMinutes, enabled, profileId, " +
            "openDurationSeconds, dailyGoal, isPlayMode, nextTriggerEpochMillis) VALUES " +
            "('$label', 'GENERAL', NULL, 127, 540, 1080, 60, 1, '$profileId', 8, 0, $isPlayMode, NULL)"

    private fun migrateTo20(preferred: String?): SupportSQLiteDatabase =
        helper.runMigrationsAndValidate(
            TEST_DB, 20, true, AppDatabaseMigrations.migration19To20(preferred)
        )

    /** Alle Zeilen einer Abfrage als Zeichenketten - macht die Erwartung im Test ablesbar. */
    private fun rows(db: SupportSQLiteDatabase, query: String): List<List<String?>> =
        db.query(query).use { c ->
            buildList {
                while (c.moveToNext()) {
                    add((0 until c.columnCount).map { if (c.isNull(it)) null else c.getString(it) })
                }
            }
        }

    /**
     * **Der Hauptfall.** Drei Avatare mit eigenen Saetzen, zuletzt gewaehlt war GLOOP: dessen Satz
     * ueberlebt und steht danach unter dem Standardprofil. Die uebrigen sind weg.
     *
     * Dass hier wirklich geloescht wird, ist der unumkehrbare Teil des ganzen Umbaus - deshalb
     * steht er als eigener Test da und nicht nur als Nebenbedingung eines anderen.
     */
    @Test
    fun migration19To20KeepsOnlyTheLastSelectedProfilesSet() {
        createVersion19(
            reminderRow("PUFFLING", "Trinken"),
            reminderRow("GLOOP", "Bewegen"),
            reminderRow("FENNEC", "Lesen")
        )

        val migrated = migrateTo20(preferred = "GLOOP")

        assertEquals(
            listOf(listOf("default", "Bewegen")),
            rows(migrated, "SELECT profileId, label FROM glyph_reminders ORDER BY label")
        )
        migrated.close()
    }

    /**
     * **Die Ausweichregel.** War zuletzt ein Avatar gewaehlt, der noch gar keine eigenen
     * Erinnerungen hat, gewinnt der groesste Satz statt eines leeren.
     *
     * Ohne diese Regel koennte ein einziger Fehlgriff kurz vor dem Update einen jahrelang
     * gepflegten Satz kosten - und zwar endgueltig, weil die Migration nur einmal laeuft.
     */
    @Test
    fun migration19To20FallsBackToTheLargestSetWhenTheSelectedOneIsEmpty() {
        createVersion19(
            reminderRow("PUFFLING", "Trinken"),
            reminderRow("PUFFLING", "Bewegen"),
            reminderRow("FENNEC", "Lesen")
        )

        val migrated = migrateTo20(preferred = "STARLET")

        assertEquals(
            listOf(listOf("default", "Bewegen"), listOf("default", "Trinken")),
            rows(migrated, "SELECT profileId, label FROM glyph_reminders ORDER BY label")
        )
        migrated.close()
    }

    /**
     * Das Pflegebuch bleibt unangetastet - **auch das der Wesen, deren Erinnerungen verschwinden.**
     *
     * Die Ereignisse gehoeren dem Wesen, nicht der Routine (siehe
     * `com.notime.glyphsim.ui.PresentCompanion`). Dass dabei Zeilen zurueckbleiben, deren
     * `reminderId` es nicht mehr gibt, ist gewollt: "so oft hast du reagiert" stimmt weiterhin,
     * nur der Titel dahinter ist nicht mehr aufloesbar. Die Alternative waere, Geschichte zu
     * loeschen, um eine Verknuepfung zu retten.
     */
    @Test
    fun migration19To20LeavesTheCareLogAlone() {
        createVersion19(
            reminderRow("PUFFLING", "Trinken"),
            reminderRow("GLOOP", "Bewegen"),
            "INSERT INTO avatar_feed_events (reminderId, animationType, epochMillis, profileId, " +
                "libraryAnimationLabel, fedAtMillis, isPlayMode) " +
                "VALUES (2, 'GENERAL', 1700000000000, 'GLOOP', NULL, 1700000000000, 0)",
            "INSERT INTO avatar_play_state (profileId, xp, startedAtMillis, lastSeenLevel) " +
                "VALUES ('GLOOP', 40, 1700000000000, 2)"
        )

        val migrated = migrateTo20(preferred = "PUFFLING")

        assertEquals(
            "Das Fuetter-Ereignis muss bleiben und weiterhin GLOOP gehoeren",
            listOf(listOf("GLOOP")),
            rows(migrated, "SELECT profileId FROM avatar_feed_events")
        )
        assertEquals(
            "Der Spielstand ist das Konto des Wesens und bleibt bei ihm",
            listOf(listOf("40")),
            rows(migrated, "SELECT xp FROM avatar_play_state WHERE profileId = 'GLOOP'")
        )
        migrated.close()
    }

    /**
     * Die Spiel-Zeile wird behandelt wie jede andere Erinnerung: die des ueberlebenden Profils
     * wandert mit, die uebrigen fallen weg. Bliebe je Avatar eine stehen, wuerfelten nach der
     * Zusammenlegung mehrere unabhaengig voneinander weiter - und im Spielmodus kaemen mehrere
     * Stupser gleichzeitig, ohne dass irgendwo stuende, warum.
     */
    @Test
    fun migration19To20KeepsExactlyOnePlayReminder() {
        createVersion19(
            reminderRow("PUFFLING", "Trinken"),
            reminderRow("PUFFLING", "Spiel", isPlayMode = 1),
            reminderRow("GLOOP", "Spiel", isPlayMode = 1)
        )

        val migrated = migrateTo20(preferred = "PUFFLING")

        assertEquals(
            listOf(listOf("1")),
            rows(migrated, "SELECT COUNT(*) FROM glyph_reminders WHERE isPlayMode = 1")
        )
        migrated.close()
    }

    /**
     * Eine Spiel-Zeile darf einen echten Satz nicht ausstechen. Wurde mit GLOOP einmal kurz
     * gespielt, ohne dort je eine Erinnerung anzulegen, gewinnt trotzdem PUFFLINGs gepflegter
     * Satz - die vom Spielmodus verwaltete Zeile ist keine Einstellung des Nutzers.
     */
    @Test
    fun migration19To20IgnoresPlayRemindersWhenChoosingTheSurvivor() {
        createVersion19(
            reminderRow("PUFFLING", "Trinken"),
            reminderRow("GLOOP", "Spiel", isPlayMode = 1)
        )

        val migrated = migrateTo20(preferred = "GLOOP")

        assertEquals(
            listOf(listOf("Trinken")),
            rows(migrated, "SELECT label FROM glyph_reminders WHERE isPlayMode = 0")
        )
        migrated.close()
    }

    /**
     * Eine leere Datenbank ist kein Sonderfall, sondern der Normalfall bei einer frischen
     * Installation, die nur zufaellig ueber diesen Pfad kommt - die Migration darf daran nicht
     * scheitern.
     */
    @Test
    fun migration19To20SurvivesAnEmptyDatabase() {
        createVersion19()

        val migrated = migrateTo20(preferred = "PUFFLING")

        assertEquals(listOf(listOf("0")), rows(migrated, "SELECT COUNT(*) FROM glyph_reminders"))
        migrated.close()
    }

    /**
     * 20 -> 21: `nodeId` an der Bibliotheks-Animation, plus Nachtrag der Zuordnung zum
     * Animations-Baum (siehe [com.notime.glyphcore.data.AnimationTree] und SKILLBAUM.md).
     *
     * Geprueft werden die beiden Faelle, die sich unterscheiden muessen: Ein bekanntes Motiv
     * bekommt seinen Knoten, eine selbstgezeichnete Animation bleibt `null`. Bekaeme auch sie
     * einen Wert, waere sie stillschweigend irgendwo im Baum einsortiert, und niemand koennte sie
     * spaeter noch als "nicht zugeordnet" finden.
     *
     * `Snail` ist bewusst ein charakterspezifisches Motiv und `Basketball` ein allgemeines - der
     * Nachtrag laeuft ueber beide Saetze und wuerde sonst nur fuer einen von beiden geprueft.
     */
    @Test
    fun migration20To21AddsNodeIdAndBackfillsKnownMotifs() {
        helper.createDatabase(TEST_DB, 20).apply {
            execSQL(
                """
                INSERT INTO library_animations (label, emoji, framesData, isSelected, sortOrder)
                VALUES ('Basketball', '🏀', '0,0', 1, 15),
                       ('Snail', '🐌', '1,1', 0, 29),
                       ('Mein Kringel', '🌀', '2,2', 0, 99)
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 21, true, *MIGRATIONS)

        assertEquals(
            listOf(
                listOf("Basketball", "sport/ballsport/basketball"),
                listOf("Snail", "ruhe/schlafen/snail"),
                listOf("Mein Kringel", null)
            ),
            rows(migrated, "SELECT label, nodeId FROM library_animations ORDER BY sortOrder")
        )
        migrated.close()
    }

    /**
     * 21 -> 22: `nodeId` an der Fuetter-Historie, nachgetragen aus BEIDEN Quellen.
     *
     * **Der Nachtrag ist hier wichtiger als die Spalte.** Die Neigungsrechnung des Skillbaums
     * (SKILLBAUM.md, P4) leitet aus dieser Historie ab, welchen Zweig jemand am ehesten bedient.
     * Bliebe sie fuer Altdaten leer, faenge ein Nutzer mit monatelanger Historie bei null an.
     *
     * Geprueft werden alle vier Faelle, die sich unterscheiden muessen:
     * ein eingebauter Typ, eine Bibliotheks-Animation, eine selbstgezeichnete ohne Knoten, und
     * MEDICINE - das bewusst ausserhalb des Baums steht und deshalb null bleiben MUSS. Bekaeme es
     * einen Knoten, koennte eine Medikamenten-Erinnerung spaeter ins Spiel geraten.
     */
    @Test
    fun migration21To22BackfillsNodeIdFromBothSources() {
        helper.createDatabase(TEST_DB, 21).apply {
            execSQL(
                """
                INSERT INTO avatar_feed_events
                    (reminderId, animationType, epochMillis, profileId, libraryAnimationLabel,
                     fedAtMillis, isPlayMode)
                VALUES (1, 'MOVE', 1700000000001, 'PUFFLING', NULL, NULL, 0),
                       (2, NULL, 1700000000002, 'PUFFLING', 'Basketball', NULL, 0),
                       (3, NULL, 1700000000003, 'PUFFLING', 'Mein Kringel', NULL, 0),
                       (4, 'MEDICINE', 1700000000004, 'PUFFLING', NULL, NULL, 0)
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 22, true, *MIGRATIONS)

        assertEquals(
            listOf(
                listOf("1", "sport"),
                listOf("2", "sport/ballsport/basketball"),
                listOf("3", null),
                listOf("4", null)
            ),
            rows(migrated, "SELECT reminderId, nodeId FROM avatar_feed_events ORDER BY reminderId")
        )
        migrated.close()
    }
}

/**
 * Aktuelle Schemaversion, gespiegelt aus der `@Database`-Annotation von [AppDatabase] - die
 * Annotation selbst laesst sich zur Laufzeit nicht bequem auslesen. Muss beim naechsten
 * Versionssprung mit erhoeht werden; [AppDatabaseMigrationTest.migratesFromBaselineToLatestKeepingData]
 * schlaegt sonst fehl, was genau der gewuenschte Stolperdraht ist.
 */
private const val CURRENT_VERSION = 22

/**
 * Alle registrierten Migrationen, so wie die App sie verwendet.
 *
 * Seit 19 -> 20 brauchen sie einen Context: welcher Avatar zuletzt gewaehlt war, steht in den
 * Einstellungen und nicht in der Datenbank (siehe [AppDatabaseMigrations.all]). Die Tests zur
 * Zusammenlegung selbst umgehen das und setzen die Vorauswahl direkt - sie sollen nicht davon
 * abhaengen, was auf dem Testgeraet gerade gespeichert ist.
 */
private val MIGRATIONS: Array<Migration>
    get() = AppDatabaseMigrations.all(InstrumentationRegistry.getInstrumentation().targetContext)
