package com.notime.glyphsim.matrix

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests fuer die Stimmung des Avatars ([AvatarMood]) und ihre Wirkung auf die Ruhe-Schleife.
 *
 * Die Stimmung ist die einzige Stelle, an der das Reagieren auf Erinnerungen eine sichtbare
 * Folge hat - stimmt die Ableitung nicht, verliert die ganze Fuetter-Mechanik ihren Sinn, ohne
 * dass irgendetwas abstuerzt.
 */
class AvatarMoodTest {

    // ---- Ableitung aus dem Antwortverhalten ----

    @Test
    fun `ohne Ausloesungen bleibt es neutral`() {
        assertEquals(AvatarMood.NEUTRAL, AvatarMood.fromGoals(emptyList()))
    }

    @Test
    fun `bei sehr wenigen Ausloesungen bleibt es neutral`() {
        // Eine einzelne verpasste Erinnerung darf den Avatar nicht sofort traurig machen -
        // das waere weder fair noch aussagekraeftig.
        assertEquals(AvatarMood.NEUTRAL, AvatarMood.fromGoals(listOf(GoalProgress(goal = 0, achieved = 0))))
    }

    @Test
    fun `lueckenloses Reagieren macht gluecklich`() {
        assertEquals(AvatarMood.HAPPY, AvatarMood.fromGoals(listOf(GoalProgress(goal = 4, achieved = 4))))
    }

    @Test
    fun `ueberwiegend reagiert ist zufrieden`() {
        assertEquals(AvatarMood.CONTENT, AvatarMood.fromGoals(listOf(GoalProgress(goal = 10, achieved = 6))))
    }

    @Test
    fun `selten reagiert macht hungrig`() {
        assertEquals(AvatarMood.HUNGRY, AvatarMood.fromGoals(listOf(GoalProgress(goal = 10, achieved = 3))))
    }

    @Test
    fun `durchgehend ignoriert macht traurig`() {
        assertEquals(AvatarMood.SAD, AvatarMood.fromGoals(listOf(GoalProgress(goal = 10, achieved = 0))))
    }

    @Test
    fun `die Stufen folgen luecken- und ueberschneidungsfrei aufeinander`() {
        // Ueber die volle Bandbreite: die Stimmung darf sich nur verbessern, nie zwischendurch
        // wieder abfallen - sonst gaebe es Quoten, bei denen mehr Reagieren schlechter dasteht.
        val order = listOf(AvatarMood.SAD, AvatarMood.HUNGRY, AvatarMood.CONTENT, AvatarMood.HAPPY)
        var lastIndex = 0
        for (fed in 0..100) {
            val mood = AvatarMood.fromGoals(listOf(GoalProgress(goal = 100, achieved = fed)))
            val index = order.indexOf(mood)
            assertTrue("Bei $fed/100 kam unerwartet $mood", index >= 0)
            assertTrue("Stimmung faellt bei $fed/100 wieder ab", index >= lastIndex)
            lastIndex = index
        }
        assertEquals(AvatarMood.HAPPY, AvatarMood.fromGoals(listOf(GoalProgress(goal = 100, achieved = 100))))
    }

    // ---- Was bis JETZT anstand ----

    /**
     * **Der Fehler, den diese Rechnung behebt.**
     *
     * Ein voellig normaler Tag: sechsmal trinken zwischen 8 und 20 Uhr, puenktlich erledigt.
     * Gerechnet gegen das volle Tagesziel ab Mitternacht war dieser Tag bis zum Nachmittag
     * "traurig" oder "hungrig" - eine Bestrafung dafuer, dass der Tag noch nicht vorbei ist.
     *
     * Der Test faehrt genau diesen Tag ab. Gegen den frueheren Stand faellt er in den ersten
     * beiden Zeilen durch.
     */
    @Test
    fun `ein puenktlich erledigter Tag ist durchgehend gut`() {
        val ziel = 6
        val start = 8 * 60
        val ende = 20 * 60

        // (Uhrzeit in Minuten, wie oft bis dahin getrunken)
        val tag = listOf(
            7 * 60 to 0,
            9 * 60 to 1,
            12 * 60 to 2,
            15 * 60 to 3,
            18 * 60 to 4,
            20 * 60 to 6
        )

        for ((minute, getan) in tag) {
            val stimmung = AvatarMood.fromGoals(
                listOf(
                    GoalProgress(
                        goal = ziel,
                        achieved = getan,
                        expected = expectedByNow(ziel, start, ende, minute)
                    )
                )
            )
            assertTrue(
                "Um ${minute / 60} Uhr mit $getan von $ziel kam $stimmung heraus",
                stimmung == AvatarMood.HAPPY || stimmung == AvatarMood.NEUTRAL
            )
        }
    }

    /**
     * Vor dem Fenster steht nichts an - also gibt es nichts zu bewerten. Genau der Zustand beim
     * Aufwachen, und der darf niemals truebe sein.
     */
    @Test
    fun `vor dem Fenster steht nichts an`() {
        assertEquals(0, expectedByNow(goal = 6, startMinuteOfDay = 8 * 60, endMinuteOfDay = 20 * 60, minuteOfDay = 7 * 60))
        assertEquals(
            AvatarMood.NEUTRAL,
            AvatarMood.fromGoals(listOf(GoalProgress(goal = 6, achieved = 0, expected = 0)))
        )
    }

    /** Wer etwas tut, bevor es faellig war, hat etwas Gutes getan - nicht "nichts Bewertbares". */
    @Test
    fun `frueher als noetig ist gut, nicht neutral`() {
        assertEquals(
            AvatarMood.HAPPY,
            AvatarMood.fromGoals(listOf(GoalProgress(goal = 6, achieved = 2, expected = 0)))
        )
    }

    /** Nach dem Fenster steht das ganze Ziel an - abends wird ein uebergangener Tag sichtbar. */
    @Test
    fun `nach dem Fenster zaehlt das volle Ziel`() {
        assertEquals(6, expectedByNow(goal = 6, startMinuteOfDay = 8 * 60, endMinuteOfDay = 20 * 60, minuteOfDay = 21 * 60))
        assertEquals(
            AvatarMood.SAD,
            AvatarMood.fromGoals(listOf(GoalProgress(goal = 6, achieved = 0, expected = 6)))
        )
    }

    /** Dazwischen anteilig, und zwar abgerundet - also zugunsten des Nutzers. */
    @Test
    fun `im Fenster waechst die Erwartung anteilig und rundet ab`() {
        val start = 8 * 60
        val ende = 20 * 60
        assertEquals(0, expectedByNow(6, start, ende, 9 * 60))
        assertEquals(2, expectedByNow(6, start, ende, 12 * 60))
        assertEquals(3, expectedByNow(6, start, ende, 15 * 60))
        assertEquals(5, expectedByNow(6, start, ende, 18 * 60))
    }

    /** Die Erwartung waechst ueber den Tag nur, faellt nie - sonst waere sie nicht lesbar. */
    @Test
    fun `die Erwartung waechst monoton`() {
        var vorher = 0
        for (minute in 0..(24 * 60)) {
            val jetzt = expectedByNow(goal = 6, startMinuteOfDay = 8 * 60, endMinuteOfDay = 20 * 60, minuteOfDay = minute)
            assertTrue("Bei Minute $minute fiel die Erwartung von $vorher auf $jetzt", jetzt >= vorher)
            vorher = jetzt
        }
        assertEquals(6, vorher)
    }

    /**
     * Ein Fenster ohne Dauer ist vermutlich ein Eingabefehler. Es gilt dann ab seinem Beginn als
     * ganztaegig - das ganze Ziel schon in derselben Minute zu erwarten waere die haerteste
     * denkbare Auslegung.
     */
    @Test
    fun `ein Fenster ohne Dauer gilt als ganztaegig`() {
        assertEquals(0, expectedByNow(goal = 4, startMinuteOfDay = 600, endMinuteOfDay = 600, minuteOfDay = 600))
        assertTrue(expectedByNow(goal = 4, startMinuteOfDay = 600, endMinuteOfDay = 600, minuteOfDay = 700) < 4)
        assertEquals(4, expectedByNow(goal = 4, startMinuteOfDay = 600, endMinuteOfDay = 600, minuteOfDay = 23 * 60 + 59))
    }

    /** Ohne Ziel gibt es nichts zu erwarten - eine Erinnerung bleibt dann ein reiner Stupser. */
    @Test
    fun `ohne Ziel gibt es keine Erwartung`() {
        assertEquals(0, expectedByNow(goal = 0, startMinuteOfDay = 0, endMinuteOfDay = 1439, minuteOfDay = 720))
    }

    // ---- Wirkung auf die Ruhe-Schleife ----

    @Test
    fun `truebe Stimmung laeuft langsamer als gute`() {
        val happy = AvatarAnimations.idleSequence(AvatarSpecies.PUFFLING, AvatarMood.HAPPY)
        val sad = AvatarAnimations.idleSequence(AvatarSpecies.PUFFLING, AvatarMood.SAD)
        assertTrue(
            "Ein trauriger Avatar muss sich traeger bewegen als ein gluecklicher",
            sad.holdsMs.sum() > happy.holdsMs.sum()
        )
    }

    @Test
    fun `jede Stimmung behaelt den Charakter der Spezies`() {
        // Tempo und Blick duerfen sich aendern, die Anzahl der Bewegungsschritte nicht - sonst
        // ginge die spezies-eigene Leitbewegung (Ohren, Fluegel, Quellen) verloren.
        for (species in AvatarSpecies.entries) {
            val neutral = AvatarAnimations.idleSequence(species, AvatarMood.NEUTRAL)
            for (mood in AvatarMood.entries) {
                val sequence = AvatarAnimations.idleSequence(species, mood)
                assertEquals(
                    "$species veraendert bei $mood die Anzahl der Frames",
                    neutral.frames.size,
                    sequence.frames.size
                )
            }
        }
    }

    @Test
    fun `auch in truebster Stimmung bleibt die Schleife lebendig`() {
        // Kein Einfrieren: selbst ein trauriger Avatar muss sich noch sichtbar bewegen, sonst
        // wirkt er wie ein Standbild oder wie ein Fehler.
        for (species in AvatarSpecies.entries) {
            val sequence = AvatarAnimations.idleSequence(species, AvatarMood.SAD)
            assertTrue("$species hat keine Frames", sequence.frames.size > 1)
            val distinct = sequence.frames.map { it.toList() }.distinct().size
            assertTrue("$species steht bei SAD komplett still", distinct > 1)
        }
    }
}
