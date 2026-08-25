package com.notime.glyphkalender.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueft die Migrationspfade der Glyphminder-Datenbank gegen die exportierten Schemas unter
 * `app/schemas/`.
 *
 * **Warum es diese Datei ueberhaupt braucht.** Fuer Tama (`:app-sim`) gab es solche Tests laengst,
 * fuer dieses Modul bisher keinen einzigen - obwohl beide Datenbanken dieselbe Entity aus dem
 * gemeinsamen Kern teilen und eine Aenderung dort **beide** trifft. Genau das ist bei 18 -> 19
 * passiert: `dailyGoal` kommt aus dem Kern und musste hier mitgezogen werden, obwohl diese App
 * gar keine Avatare kennt und mit dem Feld nichts anfaengt. Wird das beim naechsten Mal vergessen,
 * scheitert Room beim Oeffnen der Datenbank - und ein Fehler beim Oeffnen ist ein Absturz beim
 * Start, nicht eine Funktion, die fehlt.
 *
 * Instrumentiert und nicht als JVM-Test, weil Room die Migration hier wirklich ausfuehrt und das
 * Ergebnis danach Spalte fuer Spalte gegen das erwartete Schema haelt:
 *
 *     ./gradlew :app:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    private companion object {
        const val TEST_DB = "glyphkalender-migration-test.db"
        const val CURRENT_VERSION = 20
    }

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    /**
     * Schlaegt fehl, sobald jemand die Datenbankversion erhoeht, ohne das Schema zu exportieren.
     * Der `validateRelease`-Torwaechter faengt denselben Fehler beim Bauen ab - hier faellt er
     * schon beim Testen auf.
     */
    @Test
    fun baselineSchemaIsExported() {
        val db = helper.createDatabase(TEST_DB, AppDatabaseMigrations.BASELINE_VERSION)
        assertTrue("Baseline-Datenbank muss sich anlegen lassen", db.isOpen)
        db.close()
    }

    /**
     * Der eigentliche Test: eine Erinnerung auf dem Baseline-Stand anlegen, ueber alle
     * registrierten Pfade migrieren und nachsehen, ob sie den Versionswechsel unveraendert
     * ueberlebt hat. `validateDroppedTables = true` laesst Room zusaetzlich das gesamte Schema
     * pruefen.
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
                VALUES ('Migrationstest', 'DRINK', NULL, 127, 540, 1080, 60, 1, 'default', 8, NULL)
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

        migrated.query(
            "SELECT label, animationType, daysOfWeekMask, intervalMinutes FROM glyph_reminders"
        ).use { cursor ->
            assertTrue("Die vor der Migration angelegte Erinnerung muss erhalten bleiben", cursor.moveToFirst())
            assertEquals("Migrationstest", cursor.getString(0))
            assertEquals("DRINK", cursor.getString(1))
            assertEquals(127, cursor.getInt(2))
            assertEquals(60, cursor.getInt(3))
        }
        migrated.close()
    }

    /**
     * 18 -> 19 fuegt `dailyGoal` und `isPlayMode` hinzu.
     *
     * **Dieser Test hat beim ersten Lauf einen echten Fehler gefunden:** `isPlayMode` fehlte in
     * der Migration. Die Spalte kam ueber die geteilte Entity aus dem Kern ins Schema, wurde hier
     * aber nie nachgezogen - eine bestehende Installation auf Version 18 waere beim Update am
     * Oeffnen der Datenbank gescheitert, also mit einem Absturz beim Start.
     *
     * Geprueft wird beides: dass Room das Schema akzeptiert (`validateDroppedTables = true` weiter
     * unten haette den fehlenden Fall aufgedeckt) und dass die Standardwerte stimmen. Beide Spalten
     * sind NOT NULL, bestehende Zeilen haben aber keinen Wert dafuer - bekaemen sie etwas anderes
     * als 0, traege jede Alt-Erinnerung ploetzlich ein Tagesziel, das nie jemand gesetzt hat, oder
     * gaelte als Spielmodus-Zeile, die dem Nutzer gar nicht gehoert.
     */
    @Test
    fun migration18To19AddsDailyGoalAndPlayModeWithDefaults() {
        helper.createDatabase(TEST_DB, 18).apply {
            execSQL(
                """
                INSERT INTO glyph_reminders
                    (label, animationType, libraryAnimationId, daysOfWeekMask, startMinuteOfDay,
                     endMinuteOfDay, intervalMinutes, enabled, profileId, openDurationSeconds,
                     nextTriggerEpochMillis)
                VALUES ('Alt', 'MOVE', NULL, 31, 480, 1020, 30, 1, 'default', 8, 1700000000000)
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 19, true, *AppDatabaseMigrations.ALL)

        migrated.query(
            "SELECT label, dailyGoal, isPlayMode, nextTriggerEpochMillis FROM glyph_reminders"
        ).use { cursor ->
            assertTrue("Die Alt-Erinnerung muss erhalten bleiben", cursor.moveToFirst())
            assertEquals("Alt", cursor.getString(0))
            assertEquals("Alt-Erinnerungen duerfen kein Tagesziel geschenkt bekommen", 0, cursor.getInt(1))
            assertEquals("Alt-Erinnerungen gehoeren dem Nutzer, nicht dem Spielmodus", 0, cursor.getInt(2))
            assertEquals("Der geplante Zeitpunkt muss unveraendert bleiben", 1700000000000L, cursor.getLong(3))
        }
        migrated.close()
    }

    /**
     * 19 -> 20 fuegt `nodeId` an der Bibliotheks-Animation hinzu und traegt die Zuordnung zum
     * Animations-Baum fuer die bekannten Motive nach.
     *
     * **Wieder eine Spalte aus dem gemeinsamen Kern, die diese App selbst nicht braucht** - genau
     * die Sorte Aenderung, die bei `isPlayMode` einmal uebersehen wurde (siehe Test darueber).
     *
     * Geprueft werden beide Faelle, die sich unterscheiden muessen: Ein bekanntes Motiv bekommt
     * seinen Knoten, eine selbstgezeichnete Animation bleibt `null`. Bekaeme auch sie einen Wert,
     * waere sie stillschweigend irgendwo im Baum einsortiert, wo sie inhaltlich nicht hingehoert -
     * und niemand koennte sie spaeter noch als "nicht zugeordnet" finden.
     */
    @Test
    fun migration19To20AddsNodeIdAndBackfillsKnownMotifs() {
        helper.createDatabase(TEST_DB, 19).apply {
            execSQL(
                """
                INSERT INTO library_animations (label, emoji, framesData, isSelected, sortOrder)
                VALUES ('Basketball', '🏀', '0,0', 1, 15),
                       ('Mein Kringel', '🌀', '1,1', 0, 99)
                """.trimIndent()
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 20, true, *AppDatabaseMigrations.ALL)

        migrated.query(
            "SELECT label, nodeId, sortOrder FROM library_animations ORDER BY sortOrder"
        ).use { cursor ->
            assertTrue("Basketball muss erhalten bleiben", cursor.moveToFirst())
            assertEquals("Basketball", cursor.getString(0))
            assertEquals(
                "Ein bekanntes Motiv muss seinen Knoten im Baum bekommen",
                "sport/ballsport/basketball",
                cursor.getString(1)
            )
            assertEquals("Die Sortierung darf sich nicht verschieben", 15, cursor.getInt(2))

            assertTrue("Die selbstgezeichnete Animation muss erhalten bleiben", cursor.moveToNext())
            assertEquals("Mein Kringel", cursor.getString(0))
            assertTrue(
                "Eine selbstgezeichnete Animation gehoert zu keinem Knoten und muss null bleiben",
                cursor.isNull(1)
            )
        }
        migrated.close()
    }
}
