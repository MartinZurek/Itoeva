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
