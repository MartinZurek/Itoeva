package com.notime.glyphsim.ui

import android.content.Context
import com.notime.glyphcore.reminder.ActiveProfilePrefs

/**
 * Wem die Routinen gehoeren - **getrennt von der Frage, welches Wesen gerade da ist.**
 *
 * ## Die Verwechslung, die es aufloest
 *
 * Im Kern gibt es genau eine Profil-Id ([com.notime.glyphcore.reminder.ActiveProfilePrefs]), und
 * sie beantwortet heute zwei voellig verschiedene Fragen:
 *
 * 1. **Wessen Erinnerungen werden eingeplant?** (Planer, Watchdog, Erinnerungsliste)
 * 2. **Wessen Pflegebuch, Stimmung und Spielstand ist das?** (Fuetter-Ereignisse, AvatarMood,
 *    AvatarPlayState)
 *
 * Solange beide denselben Wert haben, faellt der Unterschied nicht auf - und genau deshalb ist er
 * entstanden. Sichtbar wird er erst an den Folgen: ein Avatarwechsel ist optische
 * Personalisierung, tauscht aber die komplette Routinenliste aus. Das ist Problem 3 der
 * Produktanalyse, und `AvatarProfileCharacterizationTest` misst es nach.
 *
 * ## Die Aufloesung
 *
 * Seit Phase 4b-2 gibt es **einen einzigen Routinen-Besitzer**: den Nutzer. [current] liefert
 * deshalb einen festen Wert und fragt niemanden mehr, welches Wesen gerade gewaehlt ist. Wer
 * "welches Wesen" meint, fragt [PresentCompanion] beziehungsweise [AvatarSpeciesPrefs].
 *
 * Ein Avatarwechsel ist damit wieder das, wonach er aussieht: eine Entscheidung ueber das Wesen,
 * das einen begleitet - nicht ueber den Inhalt der Erinnerungsliste.
 *
 * ## Warum ausgerechnet das Standardprofil
 *
 * Der Wert ist kein neu erfundener: [ActiveProfilePrefs.DEFAULT_PROFILE_ID] ist der Ruhewert des
 * gemeinsamen Kerns, und das :app-Modul arbeitet seit jeher genau so - es kennt keine Avatare und
 * bleibt beim Standardprofil. Beide Apps sind damit im selben Zustand, statt dass eine davon eine
 * Sonderregel mitfuehrt.
 *
 * Wichtig ist, dass der Kern dieselbe Antwort gibt: [com.notime.glyphcore.reminder.ReminderScheduler]
 * und der Watchdog lesen nicht diese Datei, sondern [ActiveProfilePrefs] direkt. Deshalb schreibt
 * [AvatarSpeciesPrefs] dort **nicht mehr** den Avatarnamen hinein, und
 * [com.notime.glyphsim.GlyphSimApp] setzt den Wert bei jedem Prozessstart auf das Standardprofil
 * zurueck - auch auf Geraeten, auf denen noch ein Avatarname von frueher darin steht. Liefen die
 * beiden auseinander, plante der Kern fuer ein Profil ohne Erinnerungen, und es kaeme nie wieder
 * eine.
 *
 * ## Der Umbau selbst
 *
 * Die Zusammenlegung der bestehenden Saetze ist eine echte Room-Migration
 * (`AppDatabaseMigrations.MIGRATION_19_20`), keine Aufraeumaktion beim Start: sie laeuft genau
 * einmal, in einer Transaktion, und ist ueber `MigrationTestHelper` pruefbar.
 */
object RoutineOwner {

    /**
     * Die Profil-Id, unter der die Routinen des Nutzers stehen - **fuer die ganze App dieselbe.**
     *
     * [context] bleibt im Aufruf, obwohl er nicht mehr gebraucht wird: die Aufrufer sollen nicht
     * anfangen, den Wert als Konstante durchzureichen. Wenn hier je wieder etwas nachzuschlagen
     * ist (mehrere Saetze, geteilte Haushalte), gehoert es an genau diese Stelle.
     */
    @Suppress("UNUSED_PARAMETER")
    fun current(context: Context): String = ActiveProfilePrefs.DEFAULT_PROFILE_ID
}
