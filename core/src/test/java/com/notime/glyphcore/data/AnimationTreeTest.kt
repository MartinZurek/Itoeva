package com.notime.glyphcore.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Bewacht den Animations-Baum ([AnimationTree]).
 *
 * **Warum das mehr ist als Buchhaltung.** Der Baum ist eine reine Datenbeschreibung - er laesst
 * sich falsch hinschreiben, ohne dass irgendetwas abstuerzt. Ein Blatt unter einer Untergruppe,
 * die es nicht gibt, waere beim Ziehen einfach wirkungslos; ein Motiv, das an zwei Knoten haengt,
 * wuerde die Zuordnung Motiv → Knoten stillschweigend mehrdeutig machen und je nach Reihenfolge
 * mal so und mal so ausfallen. Beides faellt ohne Pruefung erst auf, wenn jemand die App bedient.
 *
 * Der Abgleich gegen den echten Bestand ([DefaultLibraryAnimations.seed]) ist dabei wichtiger als
 * jede abgetippte Liste: Wer eine Animation umbenennt oder ergaenzt, bekommt hier einen roten
 * Test, statt einen toten Knoten im Baum zu hinterlassen.
 */
class AnimationTreeTest {

    private val nodes = AnimationTree.nodes

    /** Alle Labels, die es wirklich gibt - 26 allgemeine plus 30 charakterspezifische. */
    private val vorhandeneLabels: Set<String> =
        DefaultLibraryAnimations.seed().map { it.label }.toSet()

    // ================= Form des Baums =================

    @Test
    fun `jeder Knoten haengt an einem Knoten, den es gibt`() {
        val verwaist = nodes
            .filter { it.parentId != null && AnimationTree.node(it.parentId!!) == null }
            .map { it.id }
        assertEquals("Diese Knoten haengen an einem Pfad, den es nicht gibt", emptyList<String>(), verwaist)
    }

    @Test
    fun `der Baum hat neun Hauptgruppen und achtzehn Untergruppen`() {
        assertEquals(9, nodes.count { it.depth == 1 })
        assertEquals(18, nodes.count { it.depth == 2 })
        assertEquals("Der Baum hat genau drei Ebenen", 0, nodes.count { it.depth > 3 })
    }

    @Test
    fun `jede Hauptgruppe hat genau zwei Untergruppen`() {
        for (gruppe in AnimationTree.roots()) {
            assertEquals(
                "${gruppe.id} hat nicht zwei Untergruppen",
                2,
                AnimationTree.children(gruppe.id).size
            )
        }
    }

    /**
     * Mit nur einem Blatt waere die Untergruppe beim Freischalten eine Sackgasse: Man bekaeme
     * genau einmal etwas angeboten und danach nie wieder etwas aus diesem Zweig.
     */
    @Test
    fun `jede Untergruppe hat mindestens zwei Blaetter`() {
        val zuDuenn = nodes
            .filter { it.depth == 2 }
            .filter { AnimationTree.children(it.id).size < 2 }
            .map { it.id }
        assertEquals("Diese Untergruppen haben weniger als zwei Blaetter", emptyList<String>(), zuDuenn)
    }

    /**
     * Der Pfad landet als `nodeId` in der Datenbank. Umlaute, Grossbuchstaben oder Leerzeichen
     * waeren spaeter nicht mehr zu aendern, ohne gespeicherte Zeilen anzufassen.
     */
    @Test
    fun `Pfade sind stabil geschrieben`() {
        val muster = Regex("^[a-z0-9]+(-[a-z0-9]+)*(/[a-z0-9]+(-[a-z0-9]+)*)*$")
        val schief = nodes.map { it.id }.filterNot { muster.matches(it) }
        assertEquals("Diese Pfade sind nicht rein ASCII/klein/bindestrich", emptyList<String>(), schief)
    }

    @Test
    fun `jeder Pfad kommt nur einmal vor`() {
        val doppelt = nodes.groupBy { it.id }.filterValues { it.size > 1 }.keys.sorted()
        assertEquals(emptyList<String>(), doppelt)
    }

    // ================= Art der Knoten =================

    /**
     * Die Ebene entscheidet, was beim Ziehen passiert (siehe [AnimationNode.Kind]). Eine
     * Untergruppe als Einlage waere nichts, dem der Avatar nachgehen koennte; ein Blatt als
     * Taetigkeit haette nichts, worin es stattfinden koennte.
     */
    @Test
    fun `Stufe 1 und 2 sind Taetigkeiten, Stufe 3 sind Einlagen`() {
        for (node in nodes) {
            val erwartet =
                if (node.depth <= 2) AnimationNode.Kind.ACTIVITY else AnimationNode.Kind.FLOURISH
            assertEquals("${node.id} hat die falsche Art", erwartet, node.kind)
        }
    }

    @Test
    fun `nur Stufe 1 und 2 tragen einen eigenen Namen`() {
        for (node in nodes) {
            if (node.depth <= 2) {
                assertNotNull("${node.id} braucht einen Namen", node.titleRes)
            } else {
                assertNull("${node.id} soll den Namen seines Motivs tragen", node.titleRes)
            }
        }
    }

    @Test
    fun `jeder Knoten hat ein Kurzzeichen`() {
        val ohne = nodes.filter { it.emoji.isBlank() }.map { it.id }
        assertEquals(emptyList<String>(), ohne)
    }

    // ================= Zuordnung der Motive =================

    /**
     * Haengt dasselbe Motiv an zwei Knoten, ist [AnimationTree.nodeIdFor] mehrdeutig und
     * beantwortet dieselbe Frage je nach Reihenfolge anders.
     */
    @Test
    fun `kein Motiv haengt an zwei Knoten`() {
        val doppelt = nodes
            .mapNotNull { it.motif }
            .groupBy { it }
            .filterValues { it.size > 1 }
            .keys
        assertEquals(emptySet<AnimationMotif>(), doppelt)
    }

    @Test
    fun `alle vorhandenen Bibliotheks-Motive sind einsortiert`() {
        val ohneKnoten = vorhandeneLabels.filter { AnimationTree.nodeIdFor(it) == null }.sorted()
        assertEquals("Diese Motive gibt es, aber sie haengen an keinem Knoten", emptyList<String>(), ohneKnoten)
    }

    /** Die Gegenrichtung: Ein Tippfehler im Baum zeigt sonst auf ein Motiv, das es nicht gibt. */
    @Test
    fun `der Baum verweist auf kein erfundenes Motiv`() {
        val erfunden = nodes
            .mapNotNull { it.motif as? AnimationMotif.Library }
            .map { it.label }
            .filterNot { it in vorhandeneLabels }
            .sorted()
        assertEquals("Diese Motive stehen im Baum, gibt es aber nicht", emptyList<String>(), erfunden)
    }

    @Test
    fun `alle eingebauten Typen ausser den ausgeschlossenen sind einsortiert`() {
        val fehlend = AnimationType.entries
            .filterNot { it in AnimationTree.EXCLUDED_TYPES }
            .filter { AnimationTree.nodeIdFor(it) == null }
        assertEquals(emptyList<AnimationType>(), fehlend)
    }

    /**
     * **Die wichtigste Zusicherung dieser Datei.** Alles im Baum kann gezogen, freigeschaltet und
     * gewuerfelt werden. Fuer eine Medikamenten-Erinnerung ist all das falsch - sie darf nie aus
     * einem Spiel-Vorgang entstehen. Dieselbe Regel gilt im Spielplan des Avatars; hier steht die
     * zweite Stelle, an der sie brechen koennte.
     */
    @Test
    fun `MEDICINE ist kein Knoten im Baum`() {
        assertNull(AnimationTree.nodeIdFor(AnimationType.MEDICINE))
        val drin = nodes.filter { it.motif == AnimationMotif.Builtin(AnimationType.MEDICINE) }
        assertEquals(emptyList<AnimationNode>(), drin)
    }

    /**
     * **Der Baum ist vollstaendig gezeichnet.**
     *
     * Bis Paket P8 warteten zwoelf Knoten auf ihr Motiv; dieser Test hielt die Zahl fest, damit sie
     * nicht unbemerkt waechst. Jetzt steht sie auf null - und die Pruefung bleibt trotzdem, denn
     * jetzt bewacht sie die Gegenrichtung: Ein neuer Knoten ohne Motiv faellt sofort auf, statt
     * still in der Zieh-Leiste zu landen und beim Ziehen nichts zu zeigen.
     */
    @Test
    fun `kein Knoten wartet mehr auf eine Zeichnung`() {
        assertEquals(
            "Diese Knoten haben kein Motiv - siehe SKILLBAUM.md, P8",
            emptyList<String>(),
            AnimationTree.pendingArtwork().map { it.id }.sorted()
        )
    }

    // ================= Rueckfall nach oben =================

    /**
     * Der Rueckfall ist der Grund, warum nicht jeder Knoten eine eigene Choreografie braucht
     * (siehe [AnimationTree] und Paket P2). Stimmt die Reihenfolge nicht, antwortet der Avatar
     * auf ein Blatt mit der Reaktion irgendeines anderen Zweigs.
     */
    @Test
    fun `der Rueckfall laeuft vom Knoten bis zur Hauptgruppe`() {
        assertEquals(
            listOf("sport/ballsport/dribbling", "sport/ballsport", "sport"),
            AnimationTree.fallbackChain("sport/ballsport/dribbling")
        )
        assertEquals(listOf("sport"), AnimationTree.fallbackChain("sport"))
    }

    @Test
    fun `jeder Knoten faellt auf eine Hauptgruppe zurueck`() {
        for (node in nodes) {
            val kette = AnimationTree.fallbackChain(node.id)
            assertEquals("${node.id} beginnt nicht bei sich selbst", node.id, kette.first())
            assertEquals("${node.id} endet nicht in einer Hauptgruppe", 1, AnimationTree.node(kette.last())!!.depth)
            assertEquals("${node.id} hat eine Kette falscher Laenge", node.depth, kette.size)
        }
    }

    /** Ein Pfad, den es nicht gibt, ist ein Fehler - kein Anlass, ersatzweise irgendetwas zu spielen. */
    @Test
    fun `ein unbekannter Pfad ergibt keine Kette`() {
        assertEquals(emptyList<String>(), AnimationTree.fallbackChain("sport/ballsport/gibtesnicht"))
        assertEquals(emptyList<String>(), AnimationTree.fallbackChain(""))
    }

    @Test
    fun `jedes Blatt findet seinen Weg zurueck ueber nodeIdFor`() {
        for (label in vorhandeneLabels) {
            val id = AnimationTree.nodeIdFor(label)!!
            assertEquals(AnimationMotif.Library(label), AnimationTree.motifFor(id))
        }
    }
}
