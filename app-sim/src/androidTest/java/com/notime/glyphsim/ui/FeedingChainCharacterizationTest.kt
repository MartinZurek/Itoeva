package com.notime.glyphsim.ui

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.notime.glyphcore.data.AnimationType
import com.notime.glyphcore.data.DaysOfWeekMask
import com.notime.glyphcore.data.GlyphReminder
import com.notime.glyphcore.data.NO_GOAL
import com.notime.glyphsim.data.AppDatabase
import com.notime.glyphsim.data.AvatarFeedEvent
import com.notime.glyphsim.data.AvatarPlayState
import java.time.DayOfWeek
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * **Charakterisierungstests fuer die Fuetterkette — Phase 1c.**
 *
 * Die Kette lautet heute:
 *
 * ```
 * Ausloesung  →  AvatarFeedEvent angelegt (unabhaengig von einer Reaktion)
 *             →  Nutzer fuettert  →  atomare Transaktion setzt fedAtMillis genau einmal
 *             →  NUR bei isPlayMode und im selben Commit: XP
 *             →  Stimmung wird aus Tageszielen gerechnet (nicht gespeichert)
 * ```
 *
 * Zwei Eigenheiten sind dabei festzuhalten, weil der Umbau sie beruehrt:
 *
 * 1. **Der Eintrag entsteht beim Ausloesen, nicht beim Tun.** Erst dadurch laesst sich
 *    Uebergangenes ueberhaupt zaehlen. Es gibt keinen Weg, nachtraeglich einzutragen.
 * 2. **Fortschritt gibt es nur fuer Spielmodus-Ausloesungen.** Wer auf seine echten Routinen
 *    reagiert, bekommt kein XP. Das ist genau umgekehrt zu dem, was das Zielprodukt will
 *    ("real-life routine → long-term relationship progression") - und deshalb hier
 *    festgeschrieben, damit die Umkehrung in Phase 3/6 eine sichtbare Aenderung ist und kein
 *    stiller Nebeneffekt.
 *
 * Datenbank im Arbeitsspeicher: geprueft wird Datenlogik, nicht Ablage.
 *
 * Die **Stimmung** kommt hier bewusst nicht vor: sie ist eine reine Rechnung ohne Datenbank und
 * bereits durch `AvatarMoodTest` auf der JVM abgedeckt - einschliesslich der Zusage, dass es
 * keinen Endzustand gibt. Dasselbe zweimal zu pruefen macht es nicht sicherer, nur langsamer.
 *
 * **Hinweis fuer weitere Tests hier:** In instrumentierten Tests sind Methodennamen in
 * Rueckwaerts-Anfuehrungszeichen mit Leerzeichen NICHT erlaubt (D8: "Space characters in
 * SimpleName ... not allowed prior to DEX version 040"). Auf der JVM gehen sie, hier nicht -
 * deshalb durchgehend camelCase.
 */
@RunWith(AndroidJUnit4::class)
class FeedingChainCharacterizationTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() = db.close()

    private fun reminder(label: String, dailyGoal: Int = NO_GOAL, isPlayMode: Boolean = false) =
        GlyphReminder(
            label = label,
            animationType = AnimationType.DRINK,
            daysOfWeekMask = DaysOfWeekMask.toMask(DayOfWeek.entries.toSet()),
            startMinuteOfDay = 0,
            endMinuteOfDay = 23 * 60 + 59,
            intervalMinutes = 30,
            profileId = "PUFFLING",
            dailyGoal = dailyGoal,
            isPlayMode = isPlayMode
        )

    private fun ereignis(reminderId: Long, isPlayMode: Boolean = false) = AvatarFeedEvent(
        reminderId = reminderId,
        animationType = AnimationType.DRINK,
        epochMillis = System.currentTimeMillis(),
        profileId = "PUFFLING",
        libraryAnimationLabel = null,
        isPlayMode = isPlayMode
    )

    // --- Der Eintrag entsteht beim Ausloesen ---------------------------------------------------

    /**
     * Eine Ausloesung wird festgehalten, ob jemand reagiert oder nicht. Genau das macht
     * Uebergangenes zaehlbar - und genau deshalb ist `fedAtMillis` anfangs leer.
     */
    @Test
    fun eineAusloesungWirdAuchOhneReaktionFestgehalten() = runBlocking {
        val dao = db.avatarFeedEventDao()
        val id = dao.insert(ereignis(reminderId = 1L))

        val gespeichert = dao.getById(id)
        assertNotNull(gespeichert)
        assertNull("Ohne Reaktion bleibt fedAtMillis leer", gespeichert!!.fedAtMillis)
    }

    @Test
    fun fuettternMarkiertGenauDieseEineAusloesung() = runBlocking {
        val dao = db.avatarFeedEventDao()
        val erste = dao.insert(ereignis(reminderId = 1L))
        val zweite = dao.insert(ereignis(reminderId = 1L))

        assertEquals(1, dao.markFedIfUnfed(erste, 1_700_000_000_000L))

        assertEquals(1_700_000_000_000L, dao.getById(erste)?.fedAtMillis)
        assertNull(
            "Eine andere Ausloesung derselben Erinnerung darf nicht mitmarkiert werden",
            dao.getById(zweite)?.fedAtMillis
        )
    }

    // --- Fortschritt: nur fuer den Spielmodus ---------------------------------------------------

    /**
     * **Der Befund, der das Zielprodukt am staerksten betrifft.**
     *
     * `AvatarFeeding.logFeedEvent` vergibt XP nur, wenn das Ereignis `isPlayMode` traegt. Auf die
     * eigenen Routinen zu reagieren zahlt heute auf keinerlei Fortschritt ein.
     *
     * Geprueft wird hier die Bedingung an den Daten - die Vergabe selbst haengt am Context und ist
     * nicht der Punkt. Der Punkt ist: die Unterscheidung existiert und laeuft in diese Richtung.
     */
    @Test
    fun nurSpielmodusAusloesungenTragenDieFortschrittsMarkierung() = runBlocking {
        val dao = db.avatarFeedEventDao()
        val echt = dao.insert(ereignis(reminderId = 1L, isPlayMode = false))
        val spiel = dao.insert(ereignis(reminderId = 2L, isPlayMode = true))

        assertEquals(false, dao.getById(echt)?.isPlayMode)
        assertEquals(
            "Nur diese Ausloesung wuerde XP ausloesen - siehe AvatarFeeding.logFeedEvent",
            true,
            dao.getById(spiel)?.isPlayMode
        )
    }

    @Test
    fun derSpielfortschrittHaengtAmProfil() = runBlocking {
        val dao = db.avatarPlayStateDao()
        dao.insertIfAbsent(AvatarPlayState(profileId = "PUFFLING", startedAtMillis = 0L))
        dao.insertIfAbsent(AvatarPlayState(profileId = "GLOOP", startedAtMillis = 0L))

        dao.addXp("PUFFLING", 30)

        assertEquals(30, dao.getForProfile("PUFFLING")?.xp)
        assertEquals("Der Fortschritt eines Avatars gehoert ihm allein", 0, dao.getForProfile("GLOOP")?.xp)
    }

    /**
     * `insertIfAbsent` darf einen bestehenden Fortschritt nie ueberschreiben - sonst waeren XP und
     * Level beim naechsten Einschalten des Spielmodus weg. Das ist die Zusage, auf die sich jede
     * Migration in Phase 6 stuetzt.
     */
    @Test
    fun einBestehenderFortschrittWirdNieUeberschrieben() = runBlocking {
        val dao = db.avatarPlayStateDao()
        dao.insertIfAbsent(AvatarPlayState(profileId = "PUFFLING", startedAtMillis = 111L))
        dao.addXp("PUFFLING", 120)

        dao.insertIfAbsent(AvatarPlayState(profileId = "PUFFLING", startedAtMillis = 999L))

        val stand = dao.getForProfile("PUFFLING")
        assertEquals("XP muessen erhalten bleiben", 120, stand?.xp)
        assertEquals("Der Startzeitpunkt darf nicht neu gesetzt werden", 111L, stand?.startedAtMillis)
    }
}
