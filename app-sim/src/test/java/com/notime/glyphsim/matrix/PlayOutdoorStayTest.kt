package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft die Mindestdauer eines Aufenthalts unter freiem Himmel (NT-057).
 *
 * Am Geraet waere das kaum zu belegen: Man muesste mit der Stoppuhr danebensitzen und koennte
 * danach nicht sagen, ob die Figur wegen der Regel draussen blieb oder weil der Wuerfel ohnehin
 * MOVE ergab.
 */
class PlayOutdoorStayTest {

    private val tagphasen = listOf(
        PlayAmbientActivity.DayPhase.MORNING,
        PlayAmbientActivity.DayPhase.MIDDAY,
        PlayAmbientActivity.DayPhase.EVENING
    )

    // ================= Wann gehalten wird =================

    @Test
    fun `vor Ablauf der Mindestdauer wird draussen gehalten`() {
        for (phase in tagphasen) {
            assertTrue("$phase", PlayOutdoorStay.holdsOutdoors(0L, phase))
            assertTrue("$phase", PlayOutdoorStay.holdsOutdoors(45_000L, phase))
            assertTrue("$phase", PlayOutdoorStay.holdsOutdoors(PlayOutdoorStay.MIN_STAY_MS - 1, phase))
        }
    }

    @Test
    fun `danach darf sie wieder hinein`() {
        for (phase in tagphasen) {
            assertFalse("$phase", PlayOutdoorStay.holdsOutdoors(PlayOutdoorStay.MIN_STAY_MS, phase))
            assertFalse("$phase", PlayOutdoorStay.holdsOutdoors(600_000L, phase))
        }
    }

    /** Drinnen gilt die Regel nie - sie haelt nach draussen, nicht nach drinnen. */
    @Test
    fun `drinnen wird nichts gehalten`() {
        for (phase in PlayAmbientActivity.DayPhase.entries) {
            assertFalse("$phase", PlayOutdoorStay.holdsOutdoors(-1L, phase))
        }
    }

    /**
     * **Die wichtigste Ausnahme.** Ein Wesen, das um drei Uhr nachts auf der Strasse festgehalten
     * wird, weil es dort zufaellig die Tageszeit gewechselt hat, waere ein schlimmerer Fehler als
     * der, den diese Regel behebt.
     */
    @Test
    fun `nachts wird nie draussen gehalten`() {
        for (ms in listOf(0L, 10_000L, PlayOutdoorStay.MIN_STAY_MS - 1)) {
            assertFalse("$ms", PlayOutdoorStay.holdsOutdoors(ms, PlayAmbientActivity.DayPhase.NIGHT))
        }
    }

    // ================= Wirkung auf die Themenwahl =================

    /**
     * Solange gehalten wird, faellt kein Thema, das nach drinnen fuehrt. Das ist der Unterschied
     * zu allen anderen Signalen in [PlayAmbientActivity]: Eine Mindestdauer, die sich
     * fortwuerfeln laesst, ist keine.
     */
    @Test
    fun `waehrend des Haltens fuehrt kein Thema nach drinnen`() {
        val random = Random(19)
        for (phase in tagphasen) {
            repeat(600) {
                val topic = PlayAmbientActivity.nextTopic(
                    phase = phase, holdOutdoors = true, random = random
                )
                assertTrue(
                    "$phase zieht $topic, und das fuehrt nach ${PlayScene.forTopic(topic)}",
                    PlayScene.isOutdoors(PlayScene.forTopic(topic))
                )
            }
        }
    }

    /** Ohne Halten steht der ganze Tagesplan wieder offen - die Regel darf nichts dauerhaft sperren. */
    @Test
    fun `ohne Halten kommt die Figur auch wieder hinein`() {
        val random = Random(21)
        val gezogen = (1..800).map {
            PlayAmbientActivity.nextTopic(
                phase = PlayAmbientActivity.DayPhase.EVENING, holdOutdoors = false, random = random
            )
        }
        assertTrue(
            "Abends kommt kein einziges Innenthema vor",
            gezogen.any { !PlayScene.isOutdoors(PlayScene.forTopic(it)) }
        )
    }

    /**
     * **Der Rueckfall, der die Regel zahm haelt.** Gaebe es zur Tageszeit gar kein Aussenthema,
     * duerfte das Halten den Tagesablauf nicht zum Stillstand bringen. Nachts ist genau das der
     * Fall - dort greift die Regel ohnehin nicht, aber der Rueckfall wird trotzdem geprueft,
     * damit er beim naechsten Umbau der Gewichte nicht verlorengeht.
     */
    @Test
    fun `ohne jedes Aussenthema gewinnt die Tageszeit`() {
        val random = Random(23)
        repeat(200) {
            assertEquals(
                AnimationType.SLEEP,
                PlayAmbientActivity.nextTopic(
                    phase = PlayAmbientActivity.DayPhase.NIGHT, holdOutdoors = true, random = random
                )
            )
        }
    }

    /** Und MEDICINE bleibt auch unter dieser Regel draussen. */
    @Test
    fun `MEDICINE kommt auch beim Halten nicht vor`() {
        val random = Random(29)
        for (phase in tagphasen) {
            repeat(400) {
                assertTrue(
                    AnimationType.MEDICINE != PlayAmbientActivity.nextTopic(
                        phase = phase, holdOutdoors = true, random = random
                    )
                )
            }
        }
    }
}
