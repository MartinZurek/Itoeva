package com.notime.glyphsim.skilltree

import com.notime.glyphcore.data.AnimationMotif
import com.notime.glyphcore.data.AnimationTree
import com.notime.glyphcore.data.AnimationType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft die Grenze des Freischalt-Stands ([UnlockOffers.frontier]) - was auf dem Wander-Brett als
 * "als Naechstes erreichbar" antippbar ist.
 */
class UnlockOfferTest {

    private val start = UnlockOffers.startingNodes().toSet()

    // ================= Die Grenze =================

    @Test
    fun `am Anfang stehen die Untergruppen der neun Hauptgruppen zur Wahl`() {
        val frontier = UnlockOffers.frontier(start)
        assertTrue("Alle Kandidaten muessen Untergruppen sein", frontier.all { it.depth == 2 })
        assertTrue("Es muessen welche zur Wahl stehen", frontier.isNotEmpty())
    }

    @Test
    fun `ein Blatt wird erst erreichbar, wenn seine Untergruppe offen ist`() {
        assertTrue(
            UnlockOffers.frontier(start).none { it.id == "sport/ballsport/basketball" }
        )
        assertTrue(
            UnlockOffers.frontier(start + "sport/ballsport")
                .any { it.id == "sport/ballsport/basketball" }
        )
    }

    /**
     * **Ein Knoten ohne Zeichnung darf nicht angeboten werden** - er laege sonst in der Zieh-Leiste
     * und zeigte beim Ziehen nichts. Eine leere Belohnung ist schlimmer als gar keine.
     */
    @Test
    fun `ungezeichnete Knoten stehen nie zur Wahl`() {
        val pending = AnimationTree.pendingArtwork().map { it.id }.toSet()
        // Alles ausser den ungezeichneten offen - dann kann die Grenze nur noch aus ihnen bestehen.
        val alleAusserOffen = AnimationTree.nodes.map { it.id }.toSet() - pending
        assertEquals(
            "Ungezeichnete Knoten sind in die Grenze gerutscht",
            emptyList<String>(),
            UnlockOffers.frontier(alleAusserOffen).map { it.id }
        )
    }

    /**
     * **Jeder Knoten des Baums ist erreichbar.**
     *
     * Bis P8 war das nicht so: `koerper/essen` hatte kein Motiv, konnte deshalb nicht
     * freigeschaltet werden - und damit hingen auch seine beiden Blaetter fest, weil die Grenze
     * nur durch freigeschaltete Knoten waechst. Dieser Test hielt die Liste der unerreichbaren
     * Knoten fest; jetzt haelt er fest, dass sie leer ist.
     *
     * Waechst sie wieder, ist ein Knoten ohne Motiv dazugekommen und hat einen ganzen Ast
     * abgeschnitten - ein Fehler, den man der Oberflaeche nicht ansieht.
     */
    @Test
    fun `jeder Knoten ist ueber die Grenze erreichbar`() {
        var offen = start
        while (true) {
            val neu = UnlockOffers.frontier(offen).map { it.id }
            if (neu.isEmpty()) break
            offen = offen + neu
        }

        assertEquals(
            "Diese Knoten lassen sich nie freischalten - siehe SKILLBAUM.md, P8",
            emptyList<String>(),
            (AnimationTree.nodes.map { it.id }.toSet() - offen).sorted()
        )
    }

    /**
     * **MEDICINE darf nie in der Grenze auftauchen.** Es steht nicht im Baum, die Regel gilt also
     * automatisch - aber sie ist wichtig genug, um sie zu bewachen statt sie vorauszusetzen.
     */
    @Test
    fun `MEDICINE kann nicht freigeschaltet werden`() {
        assertNull("MEDICINE darf keinen Knoten haben", AnimationTree.nodeIdFor(AnimationType.MEDICINE))

        // Nicht nur die Grenze pruefen, sondern den ganzen Baum bei vollstaendig geoeffnetem
        // Stand - sonst haenge der Test daran, welcher Zustand gerade vorliegt.
        val medicine = AnimationMotif.Builtin(AnimationType.MEDICINE)
        val jemalsErreichbar = AnimationTree.nodes.map { it.id }
        assertTrue(
            "MEDICINE ist ueber einen Knoten erreichbar geworden",
            jemalsErreichbar.none { AnimationTree.motifFor(it) == medicine }
        )
    }
}
