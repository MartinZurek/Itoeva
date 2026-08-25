package com.notime.glyphsim.data

import android.content.Context
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.notime.glyphcore.data.AnimationMotif
import com.notime.glyphcore.data.AnimationTree
import com.notime.glyphcore.data.LibraryAnimationNodeIds
import com.notime.glyphcore.reminder.ActiveProfilePrefs

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

    /**
     * 19 -> 20: **die Erinnerungen gehoeren ab jetzt dem Nutzer, nicht mehr dem Avatar.**
     *
     * Das Schema bleibt unveraendert - was sich aendert, sind die Daten. Trotzdem eine echte
     * Migration und keine Aufraeumaktion beim App-Start: Room fuehrt sie genau einmal aus, in
     * einer Transaktion, und `MigrationTestHelper` kann sie gegen eine echte Datenbank von
     * Version 19 pruefen. Ein Start-Skript mit einem Merker in den Einstellungen haette beides
     * nicht - und bei einem Abbruch mittendrin bliebe ein halb zusammengelegter Stand zurueck.
     *
     * ## Was passiert
     *
     * Ein Satz ueberlebt, alle anderen werden geloescht, und der ueberlebende wandert auf das
     * Standardprofil ([com.notime.glyphcore.reminder.ActiveProfilePrefs.DEFAULT_PROFILE_ID]) -
     * dieselbe Id, unter der das :app-Modul seit jeher arbeitet.
     *
     * **Welcher Satz ueberlebt** ([chooseSurvivingProfile]): der des zuletzt gewaehlten Avatars.
     * Das ist der Satz, den der Nutzer zuletzt vor sich hatte, also der, den er als "seinen"
     * kennt. Bewusst ohne Rueckfrage: eine Auswahlliste beim ersten Start nach dem Update muesste
     * eine Frage stellen, die sich ohne Vorwissen ueber den Umbau nicht beantworten laesst.
     *
     * Hat ausgerechnet dieser Avatar keine eigenen Erinnerungen - etwa weil er gerade erst
     * ausgewaehlt und noch nichts angelegt wurde -, gewinnt stattdessen der Satz mit den meisten
     * Eintraegen. Ohne diese Ausweichregel koennte ein einziger Fehlgriff kurz vor dem Update
     * jahrelang gepflegte Erinnerungen kosten. Bei Gleichstand entscheidet der Profilname, damit
     * das Ergebnis reproduzierbar ist und nicht von der Zeilenreihenfolge abhaengt.
     *
     * ## Was ausdruecklich stehen bleibt
     *
     * Die Fuetter-Ereignisse (`avatar_feed_events`) werden **nicht** angefasst. Sie gehoeren dem
     * Wesen, nicht der Routine (siehe [com.notime.glyphsim.ui.PresentCompanion]) - das Pflegebuch
     * von Gloop bleibt bei Gloop, auch wenn seine Erinnerungen im gemeinsamen Satz aufgegangen
     * sind. Dass dabei Zeilen zurueckbleiben, deren `reminderId` es nicht mehr gibt, ist gewollt:
     * die Zahl "so oft hast du reagiert" stimmt weiterhin, nur der Titel dahinter ist nicht mehr
     * aufloesbar. Die Alternative waere, Geschichte zu loeschen, um eine Verknuepfung zu retten.
     *
     * Ebenso unberuehrt bleibt `avatar_play_state` - der Spielstand ist das Konto des Wesens.
     *
     * ## Die Spiel-Zeile
     *
     * Sie wird wie jede andere Erinnerung behandelt: die des ueberlebenden Profils wandert mit,
     * die uebrigen fallen weg. Ein Nachlegen ist nicht noetig - beim naechsten Einschalten des
     * Spielmodus entsteht sie ohnehin neu (siehe ui/PlayModeViewModel.ensurePlayReminder), und
     * ihr Inhalt wird bei jeder Neuplanung frisch gewuerfelt.
     *
     * [preferred] ist der zuletzt gewaehlte Avatar. Als Parameter statt als Nachschlagen in den
     * Einstellungen, damit der Test jede Ausgangslage herstellen kann, ohne
     * SharedPreferences-Dateien zu praeparieren.
     */
    internal fun migration19To20(preferred: String?): Migration = object : Migration(19, 20) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val survivor = chooseSurvivingProfile(db, preferred) ?: return
            db.execSQL("DELETE FROM glyph_reminders WHERE profileId <> ?", arrayOf(survivor))
            db.execSQL(
                "UPDATE glyph_reminders SET profileId = ?",
                arrayOf(ActiveProfilePrefs.DEFAULT_PROFILE_ID)
            )
        }
    }

    /**
     * Welcher Erinnerungs-Satz die Zusammenlegung ueberlebt - siehe [migration19To20].
     *
     * Gibt `null` zurueck, wenn es ueberhaupt keine Erinnerungen gibt; dann ist nichts zu tun.
     *
     * `isPlayMode = 0` bei der Zaehlung: die vom Spielmodus verwaltete Zeile ist keine Einstellung
     * des Nutzers (siehe [com.notime.glyphcore.data.GlyphReminder.isPlayMode]). Zaehlte sie mit,
     * koennte ein Avatar, mit dem einmal kurz gespielt wurde, einen tatsaechlich gepflegten Satz
     * ausstechen.
     *
     * `internal`, damit `AppDatabaseMigrationTest` die Entscheidung einzeln pruefen kann - sie ist
     * der einzige Teil dieser Migration, an dem sich ueberhaupt etwas falsch machen laesst.
     */
    internal fun chooseSurvivingProfile(db: SupportSQLiteDatabase, preferred: String?): String? {
        fun userReminderCount(profileId: String): Int =
            db.query(
                "SELECT COUNT(*) FROM glyph_reminders WHERE profileId = ? AND isPlayMode = 0",
                arrayOf(profileId)
            ).use { if (it.moveToFirst()) it.getInt(0) else 0 }

        if (preferred != null && userReminderCount(preferred) > 0) return preferred

        // Der Satz mit den meisten Eintraegen; bei Gleichstand der alphabetisch erste, damit das
        // Ergebnis nicht von der Zeilenreihenfolge abhaengt.
        db.query(
            "SELECT profileId FROM glyph_reminders WHERE isPlayMode = 0 " +
                "GROUP BY profileId ORDER BY COUNT(*) DESC, profileId ASC LIMIT 1"
        ).use { if (it.moveToFirst()) return it.getString(0) }

        // Keine einzige echte Erinnerung - dann gibt es hoechstens noch Spiel-Zeilen. Die des
        // bevorzugten Profils darf bleiben; ist auch das nicht gesetzt, bleibt irgendeine, und
        // beim naechsten Spielstart wird sie ohnehin neu gewuerfelt.
        if (preferred != null) return preferred
        db.query("SELECT profileId FROM glyph_reminders LIMIT 1")
            .use { if (it.moveToFirst()) return it.getString(0) }
        return null
    }

    /**
     * 20 -> 21: [com.notime.glyphcore.data.LibraryAnimation] bekommt `nodeId` - den Knoten im
     * Animations-Baum, zu dem eine Animation gehoert (siehe
     * [com.notime.glyphcore.data.AnimationTree] und SKILLBAUM.md).
     *
     * Reines Hinzufuegen einer Spalte, alle bestehenden Zeilen bleiben erhalten. Nullable ohne
     * Standardwert, weil `null` hier eine echte Aussage ist: Selbstgezeichnete Animationen der
     * Nutzer gehoeren zu keinem Knoten, solange niemand sie zugeordnet hat.
     *
     * Der Nachtrag fuer die Vorgaben liegt im Kern ([LibraryAnimationNodeIds]) - dieselbe Spalte
     * kommt auch in `:app` dazu, und zwei Kopien derselben 56 Zuordnungen wuerden auseinanderlaufen.
     */
    private val MIGRATION_20_21 = object : Migration(20, 21) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE library_animations ADD COLUMN nodeId TEXT")
            LibraryAnimationNodeIds.backfill(db)
        }
    }

    /**
     * Alle bekannten Migrationen, in [AppDatabase] registriert.
     *
     * Braucht seit 19 -> 20 einen [Context]: welcher Avatar zuletzt gewaehlt war, steht in den
     * Einstellungen und nicht in der Datenbank. Bewusst ueber [ActiveProfilePrefs] und nicht ueber
     * `AvatarSpeciesPrefs` - die Datenschicht soll die Avatar-Schicht nicht kennen, und beide
     * Werte waren bis zu diesem Umbau ohnehin identisch (siehe ui/AvatarSpeciesPrefs). Steht dort
     * noch nichts, greift die Ausweichregel in [chooseSurvivingProfile].
     */
    /**
     * 21 -> 22: [AvatarFeedEvent] bekommt `nodeId` - auf welchen Knoten des Animations-Baums eine
     * Ausloesung faellt (siehe [AvatarFeedEvent.nodeId]).
     *
     * **Anders als 20 -> 21 nur hier.** [AvatarFeedEvent] liegt in diesem Modul und nicht im Kern;
     * `:app` kennt die Tabelle gar nicht und bleibt deshalb auf seiner Version.
     *
     * **Der Nachtrag ist hier das Wesentliche**, nicht die neue Spalte: Die Neigungsrechnung des
     * Skillbaums (SKILLBAUM.md, Paket P4) leitet aus dieser Historie ab, welchen Zweig jemand am
     * ehesten bedient. Bliebe sie fuer Altdaten leer, faenge ein Nutzer mit monatelanger Historie
     * bei null an - und bekaeme beim ersten Levelaufstieg eine Auswahl, die nichts mit dem zu tun
     * hat, was er tatsaechlich tut.
     *
     * Nachgetragen wird aus BEIDEN Quellen: `libraryAnimationLabel` hat Vorrang, weil es die
     * genauere Angabe ist; wo es fehlt, entscheidet `animationType`. Zeilen ohne beides - und
     * MEDICINE, das ausserhalb des Baums steht - bleiben null.
     */
    private val MIGRATION_21_22 = object : Migration(21, 22) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE avatar_feed_events ADD COLUMN nodeId TEXT")
            for (node in AnimationTree.nodes) {
                when (val motif = node.motif) {
                    is AnimationMotif.Library -> db.execSQL(
                        "UPDATE avatar_feed_events SET nodeId = ? " +
                            "WHERE libraryAnimationLabel = ? AND nodeId IS NULL",
                        arrayOf(node.id, motif.label)
                    )
                    is AnimationMotif.Builtin -> db.execSQL(
                        "UPDATE avatar_feed_events SET nodeId = ? " +
                            "WHERE libraryAnimationLabel IS NULL AND animationType = ? AND nodeId IS NULL",
                        arrayOf(node.id, motif.type.name)
                    )
                    null -> Unit
                }
            }
        }
    }

    fun all(context: Context): Array<Migration> = arrayOf(
        MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16,
        MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19,
        migration19To20(preferred = ActiveProfilePrefs.get(context)),
        MIGRATION_20_21, MIGRATION_21_22
    )
}
