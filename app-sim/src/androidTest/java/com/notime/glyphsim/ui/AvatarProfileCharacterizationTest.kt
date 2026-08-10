package com.notime.glyphsim.ui

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.DaysOfWeekMask
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphcore.data.GlyphReminderRepository
import com.notime.glyphcore.reminder.ActiveProfilePrefs
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.matrix.AvatarSpecies
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek

/**
 * **Avatarwechsel und Erinnerungsbesitz - vorher und nachher.**
 *
 * Angelegt als Charakterisierungstest in Phase 1b: er hielt fest, dass jeder Avatar seinen eigenen
 * Satz Erinnerungen hat und die Profil-Id im Kern der Avatarname IST. Das war Problem 3 der
 * Produktanalyse - eine rein optische Personalisierung aenderte unerwartet die praktische
 * Konfiguration.
 *
 * Phase 4b hat diese Bindung aufgeloest. Die Tests sind deshalb umgeschrieben, und zwar
 * absichtlich nicht geloescht: Wo vorher die alte Zusage stand, steht jetzt die neue, und in der
 * Doku daneben, was sich geaendert hat. Ein Test, der beim Umbau angepasst werden MUSS, macht die
 * Aenderung zu einer sichtbaren Entscheidung - genau dafuer war er da.
 *
 * ## Was jetzt gilt
 *
 * - Die Routinen gehoeren dem Nutzer ([RoutineOwner]), unter einer festen Profil-Id.
 * - Das Wesen bestimmt Aussehen, Charakter - und sein Pflegebuch ([PresentCompanion]).
 * - Der Avatarwechsel ruehrt die Erinnerungsliste nicht mehr an.
 *
 * ## Zwei Datenbanken
 *
 * Wo es um reine Datenlogik geht, laeuft eine **Datenbank im Arbeitsspeicher** - schnell, isoliert,
 * ohne Spuren. Wo die Verbindung zwischen Avatarauswahl und Kern geprueft wird, muss es die echte
 * Ablage sein, weil genau diese Kopplung geprueft wird.
 */
@RunWith(AndroidJUnit4::class)
class AvatarProfileCharacterizationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var db: AppDatabase
    private lateinit var repository: GlyphReminderRepository
    private lateinit var vorherigerAvatar: AvatarSpecies

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = GlyphReminderRepository(db.glyphReminderDao())
        vorherigerAvatar = AvatarSpeciesPrefs.get(context)
    }

    @After
    fun tearDown() {
        db.close()
        AvatarSpeciesPrefs.set(context, vorherigerAvatar)
    }

    private fun reminder(label: String, profileId: String) = GlyphReminder(
        label = label,
        animationType = AnimationType.GENERAL,
        daysOfWeekMask = DaysOfWeekMask.toMask(DayOfWeek.entries.toSet()),
        startMinuteOfDay = 9 * 60,
        endMinuteOfDay = 18 * 60,
        intervalMinutes = 30,
        profileId = profileId
    )

    // --- Die aufgeloeste Kopplung --------------------------------------------------------------

    /**
     * **Der Kern des Umbaus.** Der Routinen-Besitzer ist fuer jedes Wesen derselbe.
     *
     * Vorher stand hier das Gegenteil: `dieProfilIdIstDerAvatarname` hielt fest, dass
     * `AvatarSpeciesPrefs.profileId(species) == species.name` die Id ist, unter der Erinnerungen
     * stehen. Diese Gleichsetzung gibt es weiterhin - sie beantwortet jetzt aber nur noch die
     * Frage nach dem Pflegebuch (siehe unten), nicht mehr die nach den Erinnerungen.
     */
    @Test
    fun derRoutinenBesitzerIstFuerAlleWesenDerselbe() {
        val ids = AvatarSpecies.entries.map { species ->
            AvatarSpeciesPrefs.set(context, species)
            RoutineOwner.current(context)
        }

        assertEquals(
            "Ein Avatarwechsel darf den Routinen-Besitzer nicht mehr veraendern",
            1,
            ids.toSet().size
        )
        assertEquals(ActiveProfilePrefs.DEFAULT_PROFILE_ID, ids.first())
    }

    /**
     * **Die Falle, die hier lauert.** Der gemeinsame Kern liest nicht [RoutineOwner], sondern
     * [ActiveProfilePrefs] - Planer und Watchdog laufen aus kalt gestarteten Prozessen und kennen
     * die Avatar-Schicht gar nicht.
     *
     * Vorher spiegelte [AvatarSpeciesPrefs] den Avatarnamen dorthin (`einAvatarwechselZiehtDie…`).
     * Genau das darf jetzt nicht mehr passieren: stuende dort "GLOOP", waehrend die Erinnerungen
     * unter dem Standardprofil liegen, plante der Kern fuer ein leeres Profil - es kaeme nie
     * wieder eine Erinnerung, und niemand wuerde die Ursache in einem Einstellungswert suchen.
     */
    @Test
    fun einAvatarwechselRuehrtDieProfilIdImKernNichtMehrAn() {
        ActiveProfilePrefs.set(context, ActiveProfilePrefs.DEFAULT_PROFILE_ID)

        AvatarSpeciesPrefs.set(context, AvatarSpecies.GLOOP)
        assertEquals(ActiveProfilePrefs.DEFAULT_PROFILE_ID, ActiveProfilePrefs.get(context))

        AvatarSpeciesPrefs.set(context, AvatarSpecies.FENNEC)
        AvatarSpeciesPrefs.get(context)
        assertEquals(
            "Auch das Lesen darf nichts mehr in den Kern spiegeln",
            ActiveProfilePrefs.DEFAULT_PROFILE_ID,
            ActiveProfilePrefs.get(context)
        )
    }

    /**
     * Die Zuordnung Wesen -> Profil-Id gibt es weiterhin, aber sie steht jetzt fuer das
     * **Pflegebuch**: Fuetter-Ereignisse, Stimmung und Spielstand (siehe [PresentCompanion]).
     */
    @Test
    fun dieAvatarIdBenenntJetztDasPflegebuch() {
        AvatarSpecies.entries.forEach { species ->
            AvatarSpeciesPrefs.set(context, species)
            assertEquals(species.name, PresentCompanion.profileId(context))
        }
    }

    // --- Erinnerungen gehoeren dem Nutzer ------------------------------------------------------

    /**
     * **Was der Nutzer davon merkt.** Alle Erinnerungen stehen unter einer Id, also sieht er nach
     * einem Avatarwechsel dieselbe Liste.
     *
     * Vorher hiess dieser Test `jederAvatarSiehtNurSeineEigenenErinnerungen` und beschrieb genau
     * das Gegenteil - "meine Erinnerungen sind weg", obwohl nichts geloescht wurde.
     */
    @Test
    fun alleErinnerungenStehenUnterDemselbenBesitzer() = runBlocking {
        val besitzer = RoutineOwner.current(context)
        repository.add(reminder("Trinken", besitzer))
        repository.add(reminder("Bewegen", besitzer))

        AvatarSpeciesPrefs.set(context, AvatarSpecies.GLOOP)
        val nachDemWechsel = repository.observeForProfile(RoutineOwner.current(context)).first()

        assertEquals(
            "Der Wechsel darf die Liste nicht veraendern",
            setOf("Trinken", "Bewegen"),
            nachDemWechsel.map { it.label }.toSet()
        )
    }

    /**
     * Die Vorgaben entstehen genau einmal - beim allerersten Start, nicht mehr bei jedem neuen
     * Avatar. Frueher bekam jeder frisch gewaehlte Avatar einen eigenen Vorgabensatz; genau
     * dadurch sammelten sich die Saetze an, die die Migration jetzt zusammenlegen muss.
     */
    @Test
    fun dieVorgabenEntstehenNurEinmal() = runBlocking {
        val besitzer = RoutineOwner.current(context)
        val angelegt = repository.seedIfEmpty(context, besitzer)

        assertTrue("Ein leerer Bestand muss Vorgaben bekommen", angelegt.isNotEmpty())
        assertTrue("Die Vorgaben gehoeren dem Nutzer", angelegt.all { it.profileId == besitzer })
        assertTrue("Ein zweiter Aufruf legt nichts nach", repository.seedIfEmpty(context, besitzer).isEmpty())
    }

    // --- Die Spiel-Zeile ------------------------------------------------------------------------

    /**
     * Es gibt genau EINE Spiel-Erinnerung, und sie gehoert dem Routinen-Besitzer. Ihr Inhalt
     * richtet sich nach dem anwesenden Wesen (siehe `PlayModeRoll`), ihre Existenz nicht.
     *
     * Der Vorgaenger dieses Tests hielt fest, dass jeder Avatar eine eigene bekommt - genau die
     * Zeilen, die `migration19To20KeepsExactlyOnePlayReminder` jetzt einsammelt.
     */
    @Test
    fun esGibtGenauEineSpielZeile() = runBlocking {
        val besitzer = RoutineOwner.current(context)
        val vorlage = reminder("Spiel", "egal").copy(isPlayMode = true)

        val a = repository.ensurePlayReminder(besitzer, vorlage)
        val b = repository.ensurePlayReminder(besitzer, vorlage)

        assertEquals("Zweimal anfordern darf nur eine Zeile ergeben", a.id, b.id)
        assertNotNull(db.glyphReminderDao().getPlayReminderForProfile(besitzer))
    }

    /**
     * Die Spiel-Zeile taucht in der Liste des Nutzers nicht auf - sie ist keine Einstellung,
     * sondern wird vom Spielmodus selbst verwaltet.
     */
    @Test
    fun dieSpielZeileZaehltNichtAlsEigeneErinnerung() = runBlocking {
        val besitzer = RoutineOwner.current(context)
        repository.ensurePlayReminder(besitzer, reminder("Spiel", "egal").copy(isPlayMode = true))

        assertTrue(
            "Die Spiel-Zeile gehoert nicht zu den eigenen Erinnerungen",
            db.glyphReminderDao().getUserRemindersForProfile(besitzer).isEmpty()
        )
        // Und sie taeuscht die Vorgaben-Entscheidung nicht: der Bestand gilt weiter als leer.
        assertTrue(repository.seedIfEmpty(context, besitzer).isNotEmpty())
    }

    // --- Das Werkzeug der Zusammenlegung --------------------------------------------------------

    /**
     * `copyProfile` war bis Phase 4b die Nutzerfunktion "Satz eines anderen Avatars uebernehmen".
     * Der Dialog dazu ist weg - es gibt nichts mehr zu uebernehmen. Die Funktion selbst bleibt,
     * weil die Zusammenlegung in `AppDatabaseMigrations` genau sie braucht: einen Satz von einem
     * Profil auf ein anderes bringen, transaktional, ohne die Spiel-Zeile mitzunehmen.
     *
     * Der Test bleibt deshalb auch stehen - er prueft jetzt ein Migrationswerkzeug statt einer
     * Bedienfunktion.
     */
    @Test
    fun einSatzLaesstSichVollstaendigUebertragen() = runBlocking {
        repository.add(reminder("Trinken", "PUFFLING"))
        repository.add(reminder("Bewegen", "PUFFLING"))
        repository.ensurePlayReminder("PUFFLING", reminder("Spiel", "egal").copy(isPlayMode = true))

        val kopien = repository.copyProfile("PUFFLING", "default", replace = false)

        assertEquals(setOf("Trinken", "Bewegen"), kopien.map { it.label }.toSet())
        assertNull(
            "Die Spiel-Zeile darf nie mitkopiert werden",
            db.glyphReminderDao().getPlayReminderForProfile("default")
        )
    }
}
