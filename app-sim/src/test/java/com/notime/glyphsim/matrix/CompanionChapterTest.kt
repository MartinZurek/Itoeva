package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.CompanionChapter.Companion.chapterFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Das Kapitel darf nur wachsen, nie bewerten.**
 *
 * Die Eigenschaften, die hier festgehalten werden, sind keine Implementierungsdetails, sondern
 * die Zusage des Merkmals: Ein Kapitel, das man verlieren oder verfehlen kann, waere eine Serie
 * mit anderem Namen - genau das, was diese App nicht sein will.
 */
class CompanionChapterTest {

    private val tag = 24L * 60 * 60 * 1000

    /** Ohne gemeinsamen Moment gibt es nichts zu erzaehlen - auch nach Monaten nicht. */
    @Test
    fun `ohne gemeinsamen Moment bleibt es beim Anfang`() {
        assertEquals(CompanionChapter.ARRIVED, chapterFor(null, 0))
        assertEquals(CompanionChapter.ARRIVED, chapterFor(null, 365 * tag))
    }

    @Test
    fun `die Kapitel folgen den Tagen seit dem ersten gemeinsamen Moment`() {
        val anfang = 1_700_000_000_000L
        assertEquals(CompanionChapter.SETTLING, chapterFor(anfang, anfang))
        assertEquals(CompanionChapter.SETTLING, chapterFor(anfang, anfang + 2 * tag))
        assertEquals(CompanionChapter.FAMILIAR, chapterFor(anfang, anfang + 3 * tag))
        assertEquals(CompanionChapter.FAMILIAR, chapterFor(anfang, anfang + 13 * tag))
        assertEquals(CompanionChapter.LONGSTANDING, chapterFor(anfang, anfang + 14 * tag))
    }

    /**
     * **Die eigentliche Zusage.** Ueber ein Jahr hinweg, Tag fuer Tag geprueft: das Kapitel geht
     * nie zurueck. Nichts, was der Nutzer tut oder laesst, geht in die Rechnung ein - es gibt also
     * gar keinen Weg, eines zu verlieren.
     */
    @Test
    fun `ein Kapitel faellt nie zurueck`() {
        val anfang = 1_700_000_000_000L
        var vorher = chapterFor(anfang, anfang)

        for (tage in 0..400) {
            val jetzt = chapterFor(anfang, anfang + tage * tag)
            assertTrue(
                "Nach $tage Tagen fiel das Kapitel von $vorher auf $jetzt zurueck",
                jetzt.ordinal >= vorher.ordinal
            )
            vorher = jetzt
        }
    }

    /**
     * Eine verstellte Uhr, ein Zeitzonenwechsel oder die Zeitumstellung duerfen keine gemeinsame
     * Geschichte loeschen. Liegt "jetzt" vor dem ersten Moment, sind es null Tage - nicht negative.
     */
    @Test
    fun `eine rueckwaerts gestellte Uhr loescht nichts`() {
        val anfang = 1_700_000_000_000L

        assertEquals(CompanionChapter.SETTLING, chapterFor(anfang, anfang - tag))
        assertEquals(CompanionChapter.SETTLING, chapterFor(anfang, anfang - 365 * tag))
    }

    /**
     * Jedes Kapitel bringt seine eigenen Texte mit. Faellt einer weg, stuerzt die App beim
     * Aufloesen der Ressource ab - und zwar genau dann, wenn jemand lange genug dabei ist.
     */
    @Test
    fun `jedes Kapitel hat Namen und Satz`() {
        for (kapitel in CompanionChapter.entries) {
            assertTrue("$kapitel ohne Namen", kapitel.labelRes != 0)
            assertTrue("$kapitel ohne Satz", kapitel.lineRes != 0)
        }
        assertEquals(
            "Namen und Saetze duerfen sich nicht doppeln",
            CompanionChapter.entries.size,
            CompanionChapter.entries.map { it.lineRes }.toSet().size
        )
    }
}
