package com.notime.glyphsim.skilltree

import com.notime.glyphcore.data.AnimationMotif
import com.notime.glyphcore.data.AnimationTree
import com.notime.glyphcore.data.AnimationType
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Prueft das Freischalt-Angebot ([UnlockOffers]).
 *
 * Der Kern ist die Regel **2 + 1**: zwei Kandidaten aus dem staerksten Zweig, einer von woanders.
 * Der Querschlaeger ist kein Beiwerk - ohne ihn verstaerkt sich die erste Wahl selbst, und wer
 * zufaellig zweimal Sport bedient hat, sieht nach zehn Leveln nie etwas anderes. Genau das ist
 * hier festgehalten.
 */
class UnlockOfferTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000
    private val start = UnlockOffers.startingNodes().toSet()

    private fun answer(nodeId: String, ageDays: Long = 0) =
        BranchAffinity.Answer(nodeId, now - ageDays * day)

    private fun rootOf(nodeId: String) = AnimationTree.fallbackChain(nodeId).last()

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
     * Die Nebenwirkung der Regel darueber, bewusst festgehalten: `koerper/essen` hat noch kein
     * Motiv, und solange das so ist, sind auch seine Blaetter unerreichbar - die Grenze waechst ja
     * nur durch freigeschaltete Knoten.
     *
     * Waechst diese Liste unbemerkt, faellt der Baum still auseinander. Schrumpft sie, ist etwas
     * gezeichnet worden (SKILLBAUM.md, P8) und der Test gehoert angepasst.
     */
    @Test
    fun `unerreichbar sind genau die ungezeichneten Knoten und die Blaetter unter essen`() {
        // Die Grenze so lange ausweiten, bis nichts Neues mehr dazukommt.
        var offen = start
        while (true) {
            val neu = UnlockOffers.frontier(offen).map { it.id }
            if (neu.isEmpty()) break
            offen = offen + neu
        }

        val unerreichbar = (AnimationTree.nodes.map { it.id }.toSet() - offen).sorted()
        val ungezeichnet = AnimationTree.pendingArtwork().map { it.id }
        // `koerper/essen` hat kein Motiv und kann deshalb nicht freigeschaltet werden - damit
        // bleiben auch seine beiden gezeichneten Blaetter haengen. Loest sich mit P8 von selbst.
        val erwartet = (ungezeichnet + "koerper/essen/plant" + "koerper/essen/battery").sorted()

        assertEquals(
            "Die Liste unerreichbarer Knoten hat sich geaendert - siehe SKILLBAUM.md, P8",
            erwartet,
            unerreichbar
        )
    }

    // ================= Die Regel 2 + 1 =================

    @Test
    fun `das Angebot besteht aus zwei aus dem staerksten Zweig plus einem anderen`() {
        val answers = List(10) { answer("sport/ballsport") }
        val offer = UnlockOffers.build(start, answers, now, Random(1))

        assertEquals(2, offer.focused.size)
        assertTrue("Beide muessen aus Sport kommen", offer.focused.all { rootOf(it.id) == "sport" })
        assertNotNull("Der Querschlaeger fehlt", offer.wildcard)
        assertTrue(
            "Der Querschlaeger darf nicht aus dem staerksten Zweig kommen",
            rootOf(offer.wildcard!!.id) != "sport"
        )
        assertEquals(3, offer.all.size)
    }

    /**
     * **Die Sackgasse, gegen die der Querschlaeger gebaut ist.** Ueber viele Aufstiege hinweg darf
     * nicht nur ein einziger Zweig auftauchen.
     */
    @Test
    fun `auch bei einseitiger Historie tauchen andere Zweige auf`() {
        val answers = List(50) { answer("sport") }
        val gesehen = (1..40).flatMap { seed ->
            UnlockOffers.build(start, answers, now, Random(seed.toLong())).all.map { rootOf(it.id) }
        }.toSet()
        assertTrue(
            "Bei einseitiger Historie kam nur Sport heraus - der Baum waere eine Sackgasse",
            gesehen.size > 1
        )
        assertTrue("Der Schwerpunkt muss trotzdem Sport bleiben", "sport" in gesehen)
    }

    @Test
    fun `der Schwerpunkt folgt der Historie und nicht der Reihenfolge im Baum`() {
        val answers = List(10) { answer("hootlet-gibt-es-nicht") } + List(10) { answer("lernen") }
        val offer = UnlockOffers.build(start, answers, now, Random(7))
        assertTrue(
            "Erwartet wurde Lernen als Schwerpunkt",
            offer.focused.all { rootOf(it.id) == "lernen" }
        )
    }

    // ================= Randfaelle =================

    @Test
    fun `ohne jede Historie kommt trotzdem ein gueltiges Angebot`() {
        val offer = UnlockOffers.build(start, emptyList(), now, Random(3))
        assertEquals(2, offer.focused.size)
        assertNotNull(offer.wildcard)
        assertTrue(offer.all.all { it.motif != null })
    }

    /** Ohne Signal gibt es keinen staerksten Zweig - immer denselben zu nehmen waere gelogen. */
    @Test
    fun `ohne Historie ist der Schwerpunkt nicht immer derselbe Zweig`() {
        val zweige = (1..30)
            .map { seed -> UnlockOffers.build(start, emptyList(), now, Random(seed.toLong())) }
            .mapNotNull { it.focused.firstOrNull() }
            .map { rootOf(it.id) }
            .toSet()
        assertTrue("Ohne Historie kam immer derselbe Zweig - das behauptet etwas", zweige.size > 1)
    }

    @Test
    fun `ist alles offen, gibt es nichts mehr anzubieten`() {
        val alles = AnimationTree.nodes.map { it.id }.toSet()
        val offer = UnlockOffers.build(alles, emptyList(), now, Random(1))
        assertTrue(offer.isEmpty)
        assertNull(offer.wildcard)
    }

    @Test
    fun `bleibt nur ein einziger Zweig uebrig, entfaellt der Querschlaeger`() {
        // Alles offen ausser Sport und dem, was ungezeichnet ist.
        val alleAusserSport = AnimationTree.nodes
            .map { it.id }
            .filterNot { rootOf(it) == "sport" }
            .toSet() + "sport"
        val offer = UnlockOffers.build(alleAusserSport, emptyList(), now, Random(1))
        assertTrue("Es muesste noch Sport-Kandidaten geben", offer.focused.isNotEmpty())
        assertTrue("Alles aus Sport", offer.focused.all { rootOf(it.id) == "sport" })
        assertNull("Es gibt keinen anderen Zweig mehr", offer.wildcard)
    }

    /**
     * **MEDICINE darf nie in einem Angebot auftauchen.** Es steht nicht im Baum, die Regel gilt
     * also automatisch - aber sie ist wichtig genug, um sie zu bewachen statt sie vorauszusetzen.
     */
    @Test
    fun `MEDICINE kann nicht freigeschaltet werden`() {
        assertNull("MEDICINE darf keinen Knoten haben", AnimationTree.nodeIdFor(AnimationType.MEDICINE))

        // Nicht nur das Angebot pruefen, sondern die ganze Grenze bei vollstaendig geoeffnetem
        // Baum - sonst haenge der Test daran, welche Knoten der Zufall gerade gewaehlt hat.
        val medicine = AnimationMotif.Builtin(AnimationType.MEDICINE)
        val jemalsErreichbar = AnimationTree.nodes.map { it.id }
        assertTrue(
            "MEDICINE ist ueber einen Knoten erreichbar geworden",
            jemalsErreichbar.none { AnimationTree.motifFor(it) == medicine }
        )
    }

    @Test
    fun `dasselbe Saatkorn ergibt dasselbe Angebot`() {
        val a = UnlockOffers.build(start, emptyList(), now, Random(42))
        val b = UnlockOffers.build(start, emptyList(), now, Random(42))
        assertEquals(a.all.map { it.id }, b.all.map { it.id })
    }
}
