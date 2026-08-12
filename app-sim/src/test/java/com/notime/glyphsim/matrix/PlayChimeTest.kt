package com.notime.glyphsim.matrix

import com.notime.glyphsim.ui.PlaySound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * **Der Klang laesst sich nicht anhoeren, ohne ein Geraet in der Hand zu haben** - und die beiden
 * Fehler, die dabei entstehen, hoert man entweder gar nicht (eine Bedingung, die nur nachts
 * greift) oder erst, wenn es zu spaet ist (ein Knacken im Lautsprecher).
 *
 * Geprueft wird deshalb beides rechnerisch: dass die Toene sauber anfangen und aufhoeren, und dass
 * geschwiegen wird, wo geschwiegen werden muss.
 */
class PlayChimeTest {

    @Test
    fun `jede Kreatur hat zu jedem Ereignis ein Motiv`() {
        for (species in AvatarSpecies.entries) {
            for (event in PlayChime.Event.entries) {
                val motif = PlayChime.motifFor(species, event)
                assertTrue("$species/$event hat kein Motiv", motif.isNotEmpty())
                assertTrue(
                    "$species/$event hat einen Ton ohne Dauer",
                    motif.all { it.millis > 0 }
                )
                assertTrue(
                    "$species/$event ist zu leise oder uebersteuert",
                    motif.all { it.level > 0f && it.level <= 1f }
                )
            }
        }
    }

    @Test
    fun `die Motive der Kreaturen unterscheiden sich`() {
        // Der ganze Zweck: Man soll hoeren, WER da ist. Sechs gleiche Piepser waeren Aufwand
        // ohne Wirkung - dieselbe Ueberlegung wie bei den Landschaften und den Wohnungen.
        val motifs = AvatarSpecies.entries.map { species ->
            PlayChime.motifFor(species, PlayChime.Event.FEED).map { it.semitones to it.millis }
        }
        assertEquals("Zwei Kreaturen klingen gleich", motifs.size, motifs.toSet().size)
    }

    @Test
    fun `das Fuettern klingt kuerzer als ein Aufstieg`() {
        // Was man oft hoert, muss kurz sein. Ein Aufstieg kommt selten und darf ausholen.
        for (species in AvatarSpecies.entries) {
            val feed = PlayChime.motifFor(species, PlayChime.Event.FEED).sumOf { it.millis }
            val levelUp = PlayChime.motifFor(species, PlayChime.Event.LEVEL_UP).sumOf { it.millis }
            assertTrue("$species: Fuettern dauert laenger als ein Aufstieg", feed < levelUp)
            assertTrue("$species: Fuettern ist zu lang fuer eine Rueckmeldung", feed <= 700)
        }
    }

    @Test
    fun `die Toene fangen bei null an und hoeren bei null auf`() {
        // **Der Fehler, den man erst im Lautsprecher hoert.** Ein Rechteck, das mitten in der
        // Schwingung anfaengt oder aufhoert, erzeugt einen Sprung im Signal - und der klingt wie
        // ein Knacken, nicht wie ein Ton.
        for (species in AvatarSpecies.entries) {
            val samples = PlayChime.render(PlayChime.motifFor(species, PlayChime.Event.LEVEL_UP))
            assertTrue("$species erzeugt gar keine Abtastwerte", samples.isNotEmpty())
            assertTrue(
                "$species faengt mit einem Sprung an: ${samples.first()}",
                abs(samples.first().toInt()) < QUIET_EDGE
            )
            assertTrue(
                "$species hoert mit einem Sprung auf: ${samples.last()}",
                abs(samples.last().toInt()) < QUIET_EDGE
            )
        }
    }

    /** Wie nah an der Null ein Anfang bzw. Ende liegen muss, damit es nicht knackt. */
    private val QUIET_EDGE = (Short.MAX_VALUE * 0.05).toInt()

    @Test
    fun `die Laenge stimmt mit der Dauer ueberein`() {
        val notes = listOf(PlayChime.Note(0, 100), PlayChime.Note(7, 200))
        val samples = PlayChime.render(notes, sampleRate = 1000)
        assertEquals(300, samples.size)
    }

    // ---- Wann geschwiegen wird ----

    @Test
    fun `nachts bleibt es still`() {
        // Die Bedingung, die man am Geraet praktisch nie ausprobiert - und die einzige, deren
        // Verletzung jemanden aufweckt.
        assertTrue(
            !PlaySound.shouldSound(
                enabled = true,
                phase = PlayAmbientActivity.DayPhase.NIGHT,
                otherAudioActive = false,
                deviceSilent = false
            )
        )
    }

    @Test
    fun `ueber laufender Musik bleibt es still`() {
        // Und zwar OHNE Audio-Focus anzufordern: Wer Focus greift, pausiert die Wiedergabe des
        // Nutzers. Eine Uhr, die den Podcast anhaelt, ist kaputt.
        assertTrue(
            !PlaySound.shouldSound(
                enabled = true,
                phase = PlayAmbientActivity.DayPhase.MIDDAY,
                otherAudioActive = true,
                deviceSilent = false
            )
        )
    }

    @Test
    fun `ohne Einschalten bleibt es still, auch wenn sonst alles passt`() {
        assertTrue(
            !PlaySound.shouldSound(
                enabled = false,
                phase = PlayAmbientActivity.DayPhase.MIDDAY,
                otherAudioActive = false,
                deviceSilent = false
            )
        )
    }

    @Test
    fun `am Tag, eingeschaltet und ohne fremden Ton klingt es`() {
        // Die Gegenprobe - sonst koennte man alle Bedingungen erfuellen, indem man nie etwas
        // abspielt.
        assertTrue(
            PlaySound.shouldSound(
                enabled = true,
                phase = PlayAmbientActivity.DayPhase.MIDDAY,
                otherAudioActive = false,
                deviceSilent = false
            )
        )
    }
}
