package com.notime.glyphkalender.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrationspfade der Glyphminder-Datenbank. Gleiches Vorgehen und dieselbe Begruendung wie in
 * `app-sim`s `AppDatabaseMigrations` - nur liegt die Baseline hier bei Version 18, weil diese
 * Datenbank eine andere Historie hat (unter anderem den Umbau von terminbasiert auf
 * `glyph_reminders`, siehe README).
 *
 * Bis einschliesslich Version 17 gab es keinen Schema-Export, diese Staende sind daher nicht
 * migrierbar und fallen unter [PRE_BASELINE_VERSIONS]. Ab Version 18 liegt jeder Stand als JSON
 * unter `app/schemas/` und jede weitere Aenderung braucht hier einen Eintrag - siehe die
 * Schritt-fuer-Schritt-Anleitung in der Tama-Variante dieser Datei.
 */
object AppDatabaseMigrations {

    /** Erster Stand mit exportiertem Schema. */
    const val BASELINE_VERSION = 18

    /** Staende ohne exportiertes Schema - nur fuer sie ist destruktives Zuruecksetzen erlaubt. */
    val PRE_BASELINE_VERSIONS: IntArray = (1 until BASELINE_VERSION).toList().toIntArray()

    /**
     * 18 -> 19: `dailyGoal` an der Erinnerung.
     *
     * Die Spalte kommt aus dem gemeinsamen Kern ([com.notime.glyphcore.data.GlyphReminder]) und
     * muss deshalb in BEIDEN Datenbanken angelegt werden, obwohl nur :app-sim die Stimmung des
     * Avatars daraus ableitet - diese App kennt gar keine Avatare. Ohne die Spalte wuerde Room
     * hier beim Oeffnen ueber ein abweichendes Schema stolpern.
     */
    private val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE glyph_reminders ADD COLUMN dailyGoal INTEGER NOT NULL DEFAULT 0")
        }
    }

    /** Alle bekannten Migrationen, in [AppDatabase] registriert. */
    val ALL: Array<Migration> = arrayOf(MIGRATION_18_19)
}
