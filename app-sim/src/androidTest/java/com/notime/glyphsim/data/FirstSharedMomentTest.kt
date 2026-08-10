package com.notime.glyphsim.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.notime.glyphcore.data.AnimationType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * **Wann ihr euch begegnet seid** - Grundlage des Kapitels (siehe
 * [com.notime.glyphsim.matrix.CompanionChapter]).
 *
 * Die Rechnung darueber liegt als JVM-Test in `CompanionChapterTest`; hier geht es um die
 * Abfrage, aus der die eine Zahl kommt. Ihr Zuschnitt ist die eigentliche Aussage, und er laesst
 * sich nur gegen echtes SQLite pruefen.
 */
@RunWith(AndroidJUnit4::class)
class FirstSharedMomentTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: AvatarFeedEventDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            AppDatabase::class.java
        ).build()
        dao = db.avatarFeedEventDao()
    }

    @After
    fun tearDown() = db.close()

    private suspend fun ereignis(
        profileId: String,
        epochMillis: Long,
        beantwortet: Boolean
    ) = dao.insert(
        AvatarFeedEvent(
            reminderId = 1L,
            animationType = AnimationType.DRINK,
            epochMillis = epochMillis,
            profileId = profileId,
            fedAtMillis = if (beantwortet) epochMillis + 1000 else null
        )
    )

    /** Ohne gemeinsame Geschichte gibt es keinen Anfang - und keine erfundene Null. */
    @Test
    fun ohneEreignisseGibtEsKeinenAnfang() = runBlocking {
        assertNull(dao.firstAnsweredMillis("GLOOP"))
    }

    /**
     * **Der Punkt der ganzen Abfrage.** Eine Erinnerung, die im leeren Raum ablief, ist kein
     * gemeinsamer Anfang. Zaehlte sie mit, begaenne die gemeinsame Geschichte in dem Moment, in
     * dem zum ersten Mal etwas ausloeste - also moeglicherweise, bevor der Nutzer die App
     * ueberhaupt bemerkt hat.
     */
    @Test
    fun unbeantworteteAusloesungenZaehlenNicht() = runBlocking {
        ereignis("GLOOP", 1_000L, beantwortet = false)
        ereignis("GLOOP", 2_000L, beantwortet = false)

        assertNull("Ohne Reaktion habt ihr nichts geteilt", dao.firstAnsweredMillis("GLOOP"))

        ereignis("GLOOP", 3_000L, beantwortet = true)

        assertEquals(3_000L, dao.firstAnsweredMillis("GLOOP"))
    }

    /**
     * Datiert wird nach dem Zeitpunkt der Ausloesung, nicht dem der Reaktion. Der Moment, um den
     * es geht, ist der, in dem etwas anstand - die Reaktion gehoert dazu, datiert ihn aber nicht.
     */
    @Test
    fun derFruehesteBeantworteteMomentGewinnt() = runBlocking {
        ereignis("GLOOP", 5_000L, beantwortet = true)
        ereignis("GLOOP", 1_000L, beantwortet = true)
        ereignis("GLOOP", 9_000L, beantwortet = true)

        assertEquals(1_000L, dao.firstAnsweredMillis("GLOOP"))
    }

    /**
     * **Je Wesen getrennt** - das ist die sichtbarste Auswirkung von Phase 4b. Ein frisch
     * gewaehltes Wesen faengt bei Null an, obwohl dieselben Routinen weiterlaufen: Es war bei
     * nichts davon dabei.
     */
    @Test
    fun jedesWesenHatSeinenEigenenAnfang() = runBlocking {
        ereignis("PUFFLING", 1_000L, beantwortet = true)

        assertEquals(1_000L, dao.firstAnsweredMillis("PUFFLING"))
        assertNull(
            "Das neue Wesen erbt die Geschichte des alten nicht",
            dao.firstAnsweredMillis("GLOOP")
        )
    }
}
