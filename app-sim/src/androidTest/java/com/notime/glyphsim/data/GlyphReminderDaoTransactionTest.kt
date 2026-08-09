package com.notime.glyphsim.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.BuiltInAnimationSelection
import com.notime.glyphcore.data.DaysOfWeekMask
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphcore.data.GlyphReminderDao
import com.notime.glyphcore.data.LibraryAnimation
import com.notime.glyphcore.data.LibraryAnimationRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek

/**
 * Prueft die Zusagen, die seit Phase 2.1 in der Datenbank selbst stehen statt im aufrufenden Code:
 * "hoechstens eine Spiel-Erinnerung je Profil", "entweder alle Standard-Erinnerungen oder keine",
 * "das Picker-Limit laesst sich nicht ueberschreiten".
 *
 * Instrumentiert und nicht als JVM-Test, aus demselben Grund wie bei [AppDatabaseMigrationTest]:
 * geprueft wird hier gerade das Verhalten von echtem SQLite - Transaktionsgrenzen, ein
 * Primaerschluessel-Konflikt, eine Unterabfrage in einem UPDATE. Ein Nachbau davon wuerde das
 * pruefen, was der Nachbau tut, nicht was die Datenbank tut.
 *
 *     ./gradlew :app-sim:connectedDebugAndroidTest
 *
 * Die Datenbank laeuft im Arbeitsspeicher: jeder Test faengt bei null an, und nichts bleibt auf
 * dem Geraet zurueck.
 */
@RunWith(AndroidJUnit4::class)
class GlyphReminderDaoTransactionTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: GlyphReminderDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.glyphReminderDao()
    }

    @After
    fun tearDown() = db.close()

    private fun reminder(
        label: String,
        profileId: String,
        isPlayMode: Boolean = false
    ) = GlyphReminder(
        label = label,
        animationType = AnimationType.GENERAL,
        daysOfWeekMask = DaysOfWeekMask.toMask(DayOfWeek.entries.toSet()),
        startMinuteOfDay = 0,
        endMinuteOfDay = 23 * 60 + 59,
        intervalMinutes = 30,
        enabled = true,
        profileId = profileId,
        isPlayMode = isPlayMode
    )

    // --- seedIfEmpty ------------------------------------------------------------------------

    @Test
    fun seedIfEmpty_legtDieVorgabenAnUndVergibtIds() = runBlocking {
        val defaults = listOf(reminder("A", "egal"), reminder("B", "egal"))

        val created = dao.seedIfEmpty("puffling", defaults)

        assertEquals(2, created.size)
        // Das Profil muss ueberschrieben worden sein, nicht das der Vorlage.
        assertTrue(created.all { it.profileId == "puffling" })
        // Ohne echte Ids koennte der Aufrufer die Erinnerungen nicht einplanen.
        assertTrue(created.all { it.id > 0 })
        assertEquals(setOf("A", "B"), created.map { it.label }.toSet())
    }

    @Test
    fun seedIfEmpty_legtNichtsAnWennDasProfilSchonEigeneErinnerungenHat() = runBlocking {
        dao.insert(reminder("vorhanden", "puffling"))

        val created = dao.seedIfEmpty("puffling", listOf(reminder("A", "egal")))

        assertTrue(created.isEmpty())
        assertEquals(1, dao.getUserRemindersForProfile("puffling").size)
    }

    @Test
    fun seedIfEmpty_laesstSichVonEinerSpielZeileNichtTaeuschen() = runBlocking {
        // Ein frisch gewaehlter Avatar hat unter Umstaenden schon seine Spiel-Zeile, aber noch
        // keine eigenen Erinnerungen - er soll die Vorgaben trotzdem bekommen.
        dao.insert(reminder("Spiel", "puffling", isPlayMode = true))

        val created = dao.seedIfEmpty("puffling", listOf(reminder("A", "egal")))

        assertEquals(1, created.size)
    }

    // --- copyProfile ------------------------------------------------------------------------

    @Test
    fun copyProfile_kopiertNurEigeneErinnerungenUndSetztDenZeigerZurueck() = runBlocking {
        dao.insert(reminder("eigen", "puffling").copy(nextTriggerEpochMillis = 12_345L))
        dao.insert(reminder("Spiel", "puffling", isPlayMode = true))

        val copies = dao.copyProfile("puffling", "gloop", replace = false)

        assertEquals(1, copies.size)
        assertEquals("eigen", copies.single().label)
        // Der geplante Zeitpunkt der Quelle bedeutet fuer die Kopie nichts.
        assertEquals(null, copies.single().nextTriggerEpochMillis)
        // Die Spiel-Zeile bleibt beim Quellprofil - sonst wuerfelten zwei unabhaengig weiter.
        assertNotNull(dao.getPlayReminderForProfile("puffling"))
        assertEquals(null, dao.getPlayReminderForProfile("gloop"))
    }

    @Test
    fun copyProfile_mitReplaceErsetztNurDieEigenenErinnerungenDesZiels() = runBlocking {
        dao.insert(reminder("quelle", "puffling"))
        dao.insert(reminder("altes-ziel", "gloop"))
        dao.insert(reminder("Spiel", "gloop", isPlayMode = true))

        dao.copyProfile("puffling", "gloop", replace = true)

        val ziel = dao.getUserRemindersForProfile("gloop")
        assertEquals(listOf("quelle"), ziel.map { it.label })
        // Der Spielstand-Anker des Ziels darf dabei nicht mit weggeraeumt werden.
        assertNotNull(dao.getPlayReminderForProfile("gloop"))
    }

    @Test
    fun copyProfile_aufSichSelbstTutNichts() = runBlocking {
        dao.insert(reminder("eigen", "puffling"))

        val copies = dao.copyProfile("puffling", "puffling", replace = true)

        assertTrue(copies.isEmpty())
        // Vor allem: das `replace` darf hier nicht zugeschlagen haben.
        assertEquals(1, dao.getUserRemindersForProfile("puffling").size)
    }

    // --- insertPlayReminderIfAbsent ---------------------------------------------------------

    @Test
    fun insertPlayReminderIfAbsent_legtHoechstensEineAn() = runBlocking {
        val vorlage = reminder("Spiel", "egal", isPlayMode = true)

        val erste = dao.insertPlayReminderIfAbsent("puffling", vorlage)
        val zweite = dao.insertPlayReminderIfAbsent("puffling", vorlage)

        assertEquals(erste.id, zweite.id)
        assertEquals(1, dao.getAllForProfile("puffling").count { it.isPlayMode })
    }

    @Test
    fun insertPlayReminderIfAbsent_schaltetEineAbgeschalteteWiederEin() = runBlocking {
        val vorlage = reminder("Spiel", "egal", isPlayMode = true)
        val angelegt = dao.insertPlayReminderIfAbsent("puffling", vorlage)
        dao.setEnabled(angelegt.id, false)

        val wieder = dao.insertPlayReminderIfAbsent("puffling", vorlage)

        assertTrue(wieder.enabled)
        assertTrue(dao.getPlayReminderForProfile("puffling")!!.enabled)
    }

    @Test
    fun insertPlayReminderIfAbsent_trenntProfileVoneinander() = runBlocking {
        val vorlage = reminder("Spiel", "egal", isPlayMode = true)

        val a = dao.insertPlayReminderIfAbsent("puffling", vorlage)
        val b = dao.insertPlayReminderIfAbsent("gloop", vorlage)

        assertTrue(a.id != b.id)
        assertEquals("puffling", dao.getPlayReminderForProfile("puffling")!!.profileId)
        assertEquals("gloop", dao.getPlayReminderForProfile("gloop")!!.profileId)
    }

    // --- Picker-Obergrenze ------------------------------------------------------------------

    @Test
    fun selectIfWithinLimit_lehntAbSobaldDerTopfVollIst() = runBlocking {
        val library = db.libraryAnimationDao()
        val builtIn = db.builtInAnimationSelectionDao()

        // Zwei Plaetze insgesamt, einer davon schon von einem eingebauten Typ belegt.
        builtIn.insertAll(
            AnimationType.entries.map { BuiltInAnimationSelection(it.name, isSelected = false) }
        )
        assertEquals(1, builtIn.selectIfWithinLimit(AnimationType.GENERAL.name, limit = 2))

        val ids = library.insertAll(
            listOf(
                LibraryAnimation(label = "eins", emoji = "1", framesData = "", isSelected = false),
                LibraryAnimation(label = "zwei", emoji = "2", framesData = "", isSelected = false)
            )
        )

        assertEquals("zweiter Platz frei", 1, library.selectIfWithinLimit(ids[0], limit = 2))
        assertEquals("Topf voll", 0, library.selectIfWithinLimit(ids[1], limit = 2))
        assertEquals(2, builtIn.countSelected() + library.countSelected())
    }

    /**
     * Am Limit, aber die Animation ist bereits ausgewaehlt: die Abfrage aendert nichts (richtig,
     * sonst zaehlte die Zeile ein zweites Mal), das Repository muss daraus aber trotzdem
     * "angenommen" machen - dem Nutzer hier "Picker voll" zu melden, waere falsch.
     */
    @Test
    fun bereitsAusgewaehlteAnimationGiltAlsAngenommen() = runBlocking {
        val library = db.libraryAnimationDao()
        val repository = LibraryAnimationRepository(library)
        val id = library.insertAll(
            listOf(LibraryAnimation(label = "eins", emoji = "1", framesData = "", isSelected = true))
        ).single()

        assertEquals(0, library.selectIfWithinLimit(id, limit = 1))
        assertTrue(repository.setSelected(id, selected = true, limit = 1))
        assertEquals(1, library.countSelected())
    }

    @Test
    fun abwaehlenGelingtImmerAuchAmLimit() = runBlocking {
        val library = db.libraryAnimationDao()
        val repository = LibraryAnimationRepository(library)
        val id = library.insertAll(
            listOf(LibraryAnimation(label = "eins", emoji = "1", framesData = "", isSelected = true))
        ).single()

        assertTrue(repository.setSelected(id, selected = false, limit = 1))
        assertEquals(0, library.countSelected())
    }
}
