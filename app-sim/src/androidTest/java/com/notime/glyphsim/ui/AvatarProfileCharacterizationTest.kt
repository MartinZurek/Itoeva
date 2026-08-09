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
 * **Charakterisierungstests fuer Avatarwechsel und Erinnerungsbesitz — Phase 1b.**
 *
 * Halten fest, was heute gilt: **jeder Avatar hat seinen eigenen Satz Erinnerungen.** Die Profil-Id
 * im gemeinsamen Kern *ist* der Avatarname (`AvatarSpeciesPrefs.profileId(species) = species.name`),
 * beides ist damit untrennbar aneinander gebunden.
 *
 * Genau diese Bindung loest Phase 4 auf - deshalb muss vorher festgeschrieben sein, was sie heute
 * bewirkt. Problem 3 der Produktanalyse lautet: eine rein optische Personalisierung aendert
 * unerwartet die praktische Konfiguration. Diese Tests zeigen, in welcher Form.
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

    // --- Die Kopplung Avatar <-> Profil --------------------------------------------------------

    /**
     * Die Profil-Id IST der Avatarname. Diese Gleichsetzung ist der Kern von Problem 3 - und die
     * Stelle, die Phase 4 aufloest.
     */
    @Test
    fun dieProfilIdIstDerAvatarname() {
        AvatarSpecies.entries.forEach { species ->
            assertEquals(species.name, AvatarSpeciesPrefs.profileId(species))
        }
    }

    /**
     * Der Kern kennt keine Avatare, nur Profile. Ein Avatarwechsel muss die aktive Profil-Id im
     * Kern nachziehen - sonst planten Alarm-Empfaenger und Planer weiterhin fuer den alten.
     */
    @Test
    fun einAvatarwechselZiehtDieProfilIdImKernNach() {
        AvatarSpeciesPrefs.set(context, AvatarSpecies.GLOOP)
        assertEquals("GLOOP", ActiveProfilePrefs.get(context))

        AvatarSpeciesPrefs.set(context, AvatarSpecies.FENNEC)
        assertEquals("FENNEC", ActiveProfilePrefs.get(context))
    }

    // --- Erinnerungen gehoeren dem Avatar, nicht dem Nutzer ------------------------------------

    /**
     * **Das Kernverhalten, das Phase 4 aendern wird.**
     *
     * Erinnerungen sind an ihr Profil gebunden. Ein Avatarwechsel zeigt damit einen voellig
     * anderen Satz - was der Nutzer als "meine Erinnerungen sind weg" erlebt, obwohl nichts
     * geloescht wurde.
     */
    @Test
    fun jederAvatarSiehtNurSeineEigenenErinnerungen() = runBlocking {
        repository.add(reminder("Trinken", "PUFFLING"))
        repository.add(reminder("Bewegen", "PUFFLING"))
        repository.add(reminder("Lesen", "GLOOP"))

        val puffling = repository.observeForProfile("PUFFLING").first()
        val gloop = repository.observeForProfile("GLOOP").first()

        assertEquals(setOf("Trinken", "Bewegen"), puffling.map { it.label }.toSet())
        assertEquals(setOf("Lesen"), gloop.map { it.label }.toSet())
    }

    /**
     * Ein frisch gewaehlter Avatar startet nicht leer, sondern bekommt die Vorgaben. Nach Phase 4
     * gaebe es diesen Moment nicht mehr - der Nutzer haette einen Satz, der ihm gehoert.
     */
    @Test
    fun einNeuerAvatarBekommtDieVorgaben() = runBlocking {
        val angelegt = repository.seedIfEmpty("WYRMLING")

        assertTrue("Ein frisches Profil muss Vorgaben bekommen", angelegt.isNotEmpty())
        assertTrue("Die Vorgaben gehoeren dem neuen Profil", angelegt.all { it.profileId == "WYRMLING" })

        // Ein zweiter Aufruf legt nichts nach - sonst verdoppelten sich die Vorgaben bei jedem
        // Wechsel hin und zurueck.
        assertTrue(repository.seedIfEmpty("WYRMLING").isEmpty())
    }

    /**
     * Erinnerungen eines nicht gewaehlten Avatars bleiben bestehen - sie ruhen nur. Das ist der
     * Grund, warum ein Wechsel zurueck alles wiederbringt, und die Grundlage jeder Migration in
     * Phase 4: es gibt nichts zu retten, was nicht noch da waere.
     */
    @Test
    fun dieErinnerungenEinesRuhendenAvatarsBleibenErhalten() = runBlocking {
        repository.add(reminder("Trinken", "PUFFLING"))
        repository.seedIfEmpty("GLOOP")

        val pufflingDanach = repository.observeForProfile("PUFFLING").first()
        assertEquals(
            "Der Wechsel darf am ruhenden Profil nichts aendern",
            listOf("Trinken"),
            pufflingDanach.map { it.label }
        )
    }

    // --- Die Spiel-Zeile ist ebenfalls profilgebunden -------------------------------------------

    /**
     * Jeder Avatar hat hoechstens EINE Spiel-Erinnerung, und sie gehoert ihm allein. Bei einer
     * Zusammenlegung in Phase 4 muss entschieden sein, was mit mehreren davon geschieht - sie
     * duerfen nicht nebeneinander bestehen bleiben, sonst wuerfeln zwei unabhaengig weiter.
     */
    @Test
    fun jederAvatarHatHoechstensEineEigeneSpielZeile() = runBlocking {
        val vorlage = reminder("Spiel", "egal").copy(isPlayMode = true)

        val a = repository.ensurePlayReminder("PUFFLING", vorlage)
        val b = repository.ensurePlayReminder("PUFFLING", vorlage)
        val c = repository.ensurePlayReminder("GLOOP", vorlage)

        assertEquals("Zweimal anfordern darf nur eine Zeile ergeben", a.id, b.id)
        assertTrue("Ein anderer Avatar bekommt eine eigene", a.id != c.id)
        assertNotNull(db.glyphReminderDao().getPlayReminderForProfile("PUFFLING"))
        assertNotNull(db.glyphReminderDao().getPlayReminderForProfile("GLOOP"))
    }

    /**
     * Die Spiel-Zeile taucht in der Liste des Nutzers nicht auf - sie ist keine Einstellung,
     * sondern wird vom Spielmodus selbst verwaltet. Beim Zusammenlegen darf sie deshalb nicht wie
     * eine Nutzer-Erinnerung behandelt werden.
     */
    @Test
    fun dieSpielZeileZaehltNichtAlsEigeneErinnerung() = runBlocking {
        repository.ensurePlayReminder("HOOTLET", reminder("Spiel", "egal").copy(isPlayMode = true))

        val eigene = db.glyphReminderDao().getUserRemindersForProfile("HOOTLET")
        assertTrue("Die Spiel-Zeile gehoert nicht zu den eigenen Erinnerungen", eigene.isEmpty())

        // Und sie taeuscht die Vorgaben-Entscheidung nicht: das Profil gilt weiter als leer.
        assertTrue(repository.seedIfEmpty("HOOTLET").isNotEmpty())
    }

    // --- Kopieren zwischen Avataren -------------------------------------------------------------

    /**
     * Der heutige Weg, einen Satz zu uebernehmen: bewusst und auf Wunsch. Phase 4 ersetzt ihn
     * durch einen gemeinsamen Satz - der Test haelt fest, was dabei verlorenginge.
     */
    @Test
    fun einSatzLaesstSichBewusstUebernehmen() = runBlocking {
        repository.add(reminder("Trinken", "PUFFLING"))
        repository.add(reminder("Bewegen", "PUFFLING"))
        repository.ensurePlayReminder("PUFFLING", reminder("Spiel", "egal").copy(isPlayMode = true))

        val kopien = repository.copyProfile("PUFFLING", "STARLET", replace = false)

        assertEquals(setOf("Trinken", "Bewegen"), kopien.map { it.label }.toSet())
        assertNull(
            "Die Spiel-Zeile darf nie mitkopiert werden",
            db.glyphReminderDao().getPlayReminderForProfile("STARLET")
        )
    }
}
