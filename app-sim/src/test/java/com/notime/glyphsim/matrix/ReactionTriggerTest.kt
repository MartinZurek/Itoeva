package com.notime.glyphsim.matrix

import com.notime.glyphcore.data.AnimationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft die Uebersetzung von "was steht in der Datenbank" nach [ReactionTrigger].
 *
 * Vier Faelle, die auseinandergehalten werden muessen - und die vorher als zwei nullbare Werte
 * genau nicht auseinanderzuhalten waren. Die beiden mittleren sind der Grund, warum es diesen Typ
 * ueberhaupt gibt: Eine selbstgezeichnete Animation ist ein echter Anlass ohne Knoten, "gar kein
 * Anlass" ist etwas anderes, und beide haetten als `nodeId = null` gleich ausgesehen.
 */
class ReactionTriggerTest {

    @Test
    fun `ein eingebauter Typ wird zum Thema`() {
        assertEquals(
            ReactionTrigger.Topic(AnimationType.DRINK),
            ReactionTrigger.of(AnimationType.DRINK, null)
        )
    }

    @Test
    fun `eine Bibliotheks-Animation im Baum wird zum Knoten`() {
        assertEquals(
            ReactionTrigger.Node("sport/ballsport/basketball"),
            ReactionTrigger.of(null, "Basketball")
        )
    }

    @Test
    fun `eine selbstgezeichnete Animation ist ein Anlass ohne Knoten`() {
        assertEquals(ReactionTrigger.Untracked, ReactionTrigger.of(null, "Mein Kringel"))
    }

    @Test
    fun `ohne beides gibt es keinen Anlass`() {
        assertEquals(ReactionTrigger.None, ReactionTrigger.of(null, null))
    }

    /**
     * **MEDICINE steht ausdruecklich nicht im Baum** (siehe
     * [com.notime.glyphcore.data.AnimationTree.EXCLUDED_TYPES]) - trotzdem muss eine
     * Medikamenten-Erinnerung ihre Reaktion bekommen. Sie laeuft ueber [ReactionTrigger.Topic] und
     * damit an jedem Knoten vorbei; genau dafuer gibt es diesen Fall getrennt vom Knoten.
     */
    @Test
    fun `MEDICINE hat keinen Knoten und bekommt trotzdem seine Reaktion`() {
        val trigger = ReactionTrigger.of(AnimationType.MEDICINE, null)
        assertEquals(ReactionTrigger.Topic(AnimationType.MEDICINE), trigger)

        for (species in AvatarSpecies.entries) {
            val frames = AvatarAnimations.reactionFramesFor(species, trigger)
            assertTrue("$species hat keine Reaktion auf MEDICINE", frames.isNotEmpty())
        }
    }

    /**
     * Der Weg der Zieh-Leiste (Paket P5): Dort steht der Knoten schon fest, es gibt kein Label
     * mehr zu uebersetzen.
     */
    @Test
    fun `ein bekannter Knoten laesst sich direkt uebergeben`() {
        assertEquals(
            ReactionTrigger.Node("ruhe/schlafen"),
            ReactionTrigger.ofNode("ruhe/schlafen")
        )
        assertEquals(ReactionTrigger.None, ReactionTrigger.ofNode(null))
    }

    /**
     * Der Vorrang zaehlt nur in einem Fall, den es geben kann: eine Erinnerung aus einer aelteren
     * Fassung, die beides traegt. Dann gewinnt die genauere Angabe.
     */
    @Test
    fun `bei beidem gewinnt die Bibliotheks-Animation`() {
        assertEquals(
            ReactionTrigger.Node("sport/ballsport/basketball"),
            ReactionTrigger.of(AnimationType.DRINK, "Basketball")
        )
    }
}
