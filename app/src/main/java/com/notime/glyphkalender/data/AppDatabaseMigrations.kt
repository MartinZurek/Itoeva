package com.notime.glyphkalender.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.notime.glyphcore.data.LibraryAnimationNodeIds

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
     * 18 -> 19: `dailyGoal` UND `isPlayMode` an der Erinnerung.
     *
     * Beide Spalten kommen aus dem gemeinsamen Kern ([com.notime.glyphcore.data.GlyphReminder])
     * und muessen deshalb in BEIDEN Datenbanken angelegt werden, obwohl nur :app-sim etwas mit
     * ihnen anfaengt - diese App kennt weder Avatare noch einen Spielmodus. Room vergleicht beim
     * Oeffnen aber das gesamte Schema, nicht nur die Spalten, die man tatsaechlich benutzt.
     *
     * **`isPlayMode` fehlte hier.** Die Spalte wurde im Kern fuer den Spielmodus von :app-sim
     * eingefuehrt; dort zog die Migration 16 -> 17 sie mit, hier zog sie niemand nach. Das Schema
     * 19 fuehrte sie trotzdem - Room erzeugt es aus der Entity, und die ist gemeinsam. Ergebnis:
     * eine bestehende Installation auf Version 18 waere beim Update ueber ein abweichendes Schema
     * gestolpert, und ein Fehlschlag beim Oeffnen der Datenbank ist ein Absturz beim Start.
     *
     * Aufgefallen ist das erst, als es fuer dieses Modul zum ersten Mal einen Migrationstest gab
     * (siehe androidTest/.../AppDatabaseMigrationTest). Genau davor sollte er schuetzen: eine
     * Aenderung im geteilten Kern trifft beide Datenbanken, und hier wurde sie uebersehen.
     */
    private val MIGRATION_18_19 = object : Migration(18, 19) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE glyph_reminders ADD COLUMN dailyGoal INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE glyph_reminders ADD COLUMN isPlayMode INTEGER NOT NULL DEFAULT 0")
        }
    }

    /**
     * 19 -> 20: `nodeId` an der Bibliotheks-Animation
     * ([com.notime.glyphcore.data.LibraryAnimation]) - der Knoten im Animations-Baum, zu dem sie
     * gehoert (siehe [com.notime.glyphcore.data.AnimationTree] und SKILLBAUM.md).
     *
     * **Wieder eine Spalte aus dem gemeinsamen Kern, die diese App selbst nicht braucht.** Der
     * Animations-Baum gehoert zum Avatar-Spiel und damit zu :app-sim; hier gibt es weder Avatar
     * noch Skillbaum. Room vergleicht beim Oeffnen aber das ganze Schema - genau der Fall, der bei
     * `isPlayMode` einmal uebersehen wurde (siehe [MIGRATION_18_19]). Diesmal mitgezogen.
     *
     * Der Nachtrag der Werte laeuft ueber [com.notime.glyphcore.data.LibraryAnimationNodeIds] und
     * ist damit dieselbe Logik, die auch :app-sim benutzt. Er ist hier nicht sinnlos: Beide Apps
     * spielen dieselben Bibliotheks-Animationen ab, und eine spaeter zugeordnete Animation soll in
     * beiden Datenbanken denselben Knoten tragen.
     */
    private val MIGRATION_19_20 = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE library_animations ADD COLUMN nodeId TEXT")
            LibraryAnimationNodeIds.backfill(db)
        }
    }

    /** Alle bekannten Migrationen, in [AppDatabase] registriert. */
    val ALL: Array<Migration> = arrayOf(MIGRATION_18_19, MIGRATION_19_20)
}
