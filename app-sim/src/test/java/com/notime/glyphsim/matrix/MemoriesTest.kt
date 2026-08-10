package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * **Eine Erinnerung kann man nicht verfehlen.**
 *
 * Die Eigenschaften hier sind die Zusage des Merkmals, nicht Implementierungsdetails: In dieser
 * Liste darf nichts stehen, was jemandem etwas vorwirft, und nichts, was wieder verschwindet,
 * wenn eine Woche schlecht laeuft.
 */
class MemoriesTest {

    private val tag = 24L * 60 * 60 * 1000
    private val anfang = 1_700_000_000_000L

    private fun thema(typ: AnimationType, tageNachAnfang: Long) =
        TopicFirst(typ, null, anfang + tageNachAnfang * tag)

    /** Ohne gemeinsamen Anfang gibt es keine Geschichte - auch nicht aus Restdaten. */
    @Test
    fun `ohne Anfang bleibt die Liste leer`() {
        assertEquals(
            emptyList<Memory>(),
            buildMemories(null, listOf(thema(AnimationType.DRINK, 0)), anfang + 100 * tag)
        )
    }

    /** Der erste gemeinsame Moment steht immer da, auch wenn sonst nichts passiert ist. */
    @Test
    fun `der Anfang steht immer als Erstes`() {
        val liste = buildMemories(anfang, emptyList(), anfang)

        assertEquals(Memory.Kind.MET, liste.first().kind)
        assertEquals(anfang, liste.first().epochMillis)
    }

    /**
     * **Die Reihenfolge ist die der Geschichte.** Aufsteigend, damit man von vorn liest und unten
     * in der Gegenwart ankommt.
     */
    @Test
    fun `die Liste laeuft chronologisch vorwaerts`() {
        val liste = buildMemories(
            anfang,
            listOf(thema(AnimationType.MOVE, 5), thema(AnimationType.DRINK, 1)),
            anfang + 20 * tag
        )

        val zeiten = liste.map { it.epochMillis }
        assertEquals("Nicht aufsteigend sortiert: $zeiten", zeiten.sorted(), zeiten)
    }

    /**
     * Kapitel entstehen aus dem Anfang und werden nicht gespeichert - aufgenommen wird nur, was
     * schon erreicht ist. Eine Vorschau auf das naechste waere ein Ziel.
     */
    @Test
    fun `nur erreichte Kapitel stehen drin`() {
        val amAnfang = buildMemories(anfang, emptyList(), anfang)
        assertEquals(
            listOf(CompanionChapter.SETTLING),
            amAnfang.mapNotNull { it.chapter }
        )

        val nachZweiWochen = buildMemories(anfang, emptyList(), anfang + 14 * tag)
        assertEquals(
            listOf(CompanionChapter.SETTLING, CompanionChapter.FAMILIAR, CompanionChapter.LONGSTANDING),
            nachZweiWochen.mapNotNull { it.chapter }
        )
    }

    /**
     * **Die eigentliche Zusage.** Ueber ein Jahr hinweg, Tag fuer Tag: die Liste wird nie kuerzer.
     * Es gibt also keinen Weg, eine Erinnerung zu verlieren - auch nicht durch Nichtstun.
     */
    @Test
    fun `die Liste wird nie kuerzer`() {
        val themen = listOf(thema(AnimationType.DRINK, 1), thema(AnimationType.MOVE, 30))
        var vorher = 0

        for (tage in 0..400) {
            val jetzt = buildMemories(anfang, themen.filter { it.firstEpochMillis <= anfang + tage * tag }, anfang + tage * tag)
            assertTrue(
                "Nach $tage Tagen schrumpfte die Liste von $vorher auf ${jetzt.size}",
                jetzt.size >= vorher
            )
            vorher = jetzt.size
        }
    }

    /**
     * Bibliotheks-Animationen tragen einen Namen statt eines Themas - beides muss durchkommen,
     * sonst fehlten ausgerechnet die selbst gewaehlten Erinnerungen.
     */
    @Test
    fun `Bibliotheks-Animationen kommen mit ihrem Namen durch`() {
        val liste = buildMemories(
            anfang,
            listOf(TopicFirst(null, "Rocket", anfang + tag)),
            anfang + 2 * tag
        )

        val eintrag = liste.single { it.kind == Memory.Kind.FIRST_TOPIC }
        assertEquals("Rocket", eintrag.libraryLabel)
        assertEquals(null, eintrag.topic)
    }

    /**
     * Eine Zeile ohne Thema UND ohne Namen laesst sich nicht darstellen - das kann nur eine
     * Altzeile sein, deren Typ es nicht mehr gibt. Sie faellt still weg, statt eine leere Zeile
     * zu erzeugen.
     */
    @Test
    fun `unlesbare Altzeilen fallen weg`() {
        val liste = buildMemories(anfang, listOf(TopicFirst(null, null, anfang + tag)), anfang + 2 * tag)

        assertTrue(liste.none { it.kind == Memory.Kind.FIRST_TOPIC })
    }

    /**
     * Am ersten Tag fallen Kennenlernen und erstes Kapitel auf denselben Zeitpunkt. Ohne feste
     * Reihenfolge stuende dort mal das eine, mal das andere oben.
     */
    @Test
    fun `bei gleichem Zeitpunkt steht das Kennenlernen oben`() {
        val liste = buildMemories(anfang, listOf(thema(AnimationType.DRINK, 0)), anfang)

        assertEquals(
            listOf(Memory.Kind.MET, Memory.Kind.CHAPTER, Memory.Kind.FIRST_TOPIC),
            liste.map { it.kind }
        )
    }
}
