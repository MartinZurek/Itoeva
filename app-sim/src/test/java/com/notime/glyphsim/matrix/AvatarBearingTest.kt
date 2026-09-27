package com.notime.glyphsim.matrix

import com.notime.glyphsim.living.NeedKind
import com.notime.glyphsim.living.Needs
import com.notime.glyphsim.matrix.AvatarAnimations.Fidget
import com.notime.glyphsim.matrix.PlayAmbientActivity.DayPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Prueft, dass der Gang und die spontanen Regungen den Zustand des Wesens zeigen. */
class AvatarBearingTest {

    private val calm = Needs.calm()
    private val exhausted = Needs.of(NeedKind.ENERGY to 1.0)
    private val allPressing = Needs(NeedKind.entries.associateWith { 0.9 })

    @Test
    fun tagsueberOhneSimulationGehtEsImGewohntenTempo() {
        assertEquals(1f, AvatarBearing.paceFactor(null, DayPhase.MIDDAY), 0.0001f)
    }

    @Test
    fun muedeGehtLangsamer() {
        val rested = AvatarBearing.paceFactor(Needs.of(NeedKind.HUNGER to 0.3), DayPhase.MIDDAY)
        val tired = AvatarBearing.paceFactor(exhausted, DayPhase.MIDDAY)
        assertTrue("muede $tired, ausgeruht $rested", tired < rested - 0.15f)
    }

    @Test
    fun nachtsGehtAuchEinAusgeruhtesWesenLangsamerAlsMittags() {
        assertTrue(
            AvatarBearing.paceFactor(null, DayPhase.NIGHT) <
                AvatarBearing.paceFactor(null, DayPhase.MIDDAY)
        )
    }

    @Test
    fun zufriedenGehtBeschwingt() {
        assertTrue(AvatarBearing.paceFactor(calm, DayPhase.MIDDAY) > 1.05f)
    }

    @Test
    fun tempoBleibtImmerInDenGrenzen() {
        for (phase in DayPhase.entries) {
            for (needs in listOf(null, calm, exhausted, allPressing)) {
                val pace = AvatarBearing.paceFactor(needs, phase)
                assertTrue(pace in AvatarBearing.MIN_PACE..AvatarBearing.MAX_PACE)
            }
        }
    }

    @Test
    fun keineRegungDirektNachDerLetzten() {
        for (loops in 0 until AvatarBearing.MIN_LOOPS_BETWEEN_FIDGETS) {
            assertNull(AvatarBearing.idleFidget(exhausted, DayPhase.NIGHT, loops, 0f))
        }
    }

    @Test
    fun muedeGaehnt() {
        assertEquals(Fidget.YAWN, AvatarBearing.idleFidget(exhausted, DayPhase.NIGHT, 5, 0.1f))
    }

    @Test
    fun morgensAusgeruhtStrecktEsSich() {
        assertEquals(Fidget.STRETCH, AvatarBearing.idleFidget(calm, DayPhase.MORNING, 5, 0.1f))
    }

    @Test
    fun unruhigSiehtEsSichUm() {
        val bored = Needs.of(NeedKind.FUN to 0.8)
        assertEquals(Fidget.LOOK_AROUND, AvatarBearing.idleFidget(bored, DayPhase.MIDDAY, 5, 0.1f))
    }

    @Test
    fun rundumZufriedenSchuetteltEsSichSelten() {
        assertEquals(Fidget.SHAKE, AvatarBearing.idleFidget(calm, DayPhase.MIDDAY, 5, 0.1f))
        assertNull(AvatarBearing.idleFidget(calm, DayPhase.MIDDAY, 5, 0.5f))
    }

    @Test
    fun ohneAnlassKeineRegung() {
        val middling = Needs(NeedKind.entries.associateWith { 0.4 })
        assertNull(AvatarBearing.idleFidget(middling, DayPhase.MIDDAY, 10, 0.05f))
    }

    @Test
    fun muedigkeitHatVorrangVorUnruhe() {
        val tiredAndBored = Needs.of(NeedKind.ENERGY to 0.9, NeedKind.FUN to 0.9)
        assertEquals(Fidget.YAWN, AvatarBearing.idleFidget(tiredAndBored, DayPhase.MIDDAY, 5, 0.1f))
    }
}
