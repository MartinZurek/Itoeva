package com.notime.glyphcore.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Bewacht [AnimationTreeLayout] - vor allem die Zusage, dass es fuer JEDE Tiefe funktioniert, nicht
 * nur fuer die drei Ebenen, die [AnimationTree] heute hat.
 */
class AnimationTreeLayoutTest {

    private fun node(id: String) =
        AnimationNode(id = id, emoji = "🔹", kind = AnimationNode.Kind.ACTIVITY, motif = null)

    // ================= Der echte Baum =================

    @Test
    fun `jeder Knoten des echten Baums bekommt eine Position`() {
        val positions = AnimationTreeLayout.compute()
        val fehlend = AnimationTree.nodes.map { it.id }.filterNot { it in positions }
        assertEquals("Diesen Knoten fehlt eine Position", emptyList<String>(), fehlend)
    }

    @Test
    fun `keine zwei Knoten des echten Baums teilen sich eine Position`() {
        val positions = AnimationTreeLayout.compute()
        val duplikate = positions.values
            .groupingBy { it }
            .eachCount()
            .filter { it.value > 1 }
        assertEquals("Diese Positionen sind mehrfach vergeben", emptyMap<Any, Int>(), duplikate)
    }

    @Test
    fun `y ist bei jedem Knoten des echten Baums genau die Tiefe minus eins`() {
        val positions = AnimationTreeLayout.compute()
        for (n in AnimationTree.nodes) {
            assertEquals("${n.id}: falsches y", n.depth - 1, positions.getValue(n.id).y)
        }
    }

    @Test
    fun `ein Elternknoten steht mittig ueber seinen Kindern`() {
        val positions = AnimationTreeLayout.compute()
        for (n in AnimationTree.nodes) {
            val children = AnimationTree.children(n.id)
            if (children.isEmpty()) continue
            val erwartet = children.map { positions.getValue(it.id).x }.average().toFloat()
            assertEquals("${n.id}: nicht mittig ueber seinen Kindern", erwartet, positions.getValue(n.id).x, 0.0001f)
        }
    }

    // ================= Ein eigens gebauter, fuenfstufiger Baum =================
    //
    //   root
    //    `- a
    //        |- b
    //        |   |- c1
    //        |   |   `- d1     (Tiefe 5)
    //        |   `- c2
    //        `- b2

    private val tiefBaum = listOf(
        node("root"),
        node("root/a"),
        node("root/a/b"),
        node("root/a/b/c1"),
        node("root/a/b/c1/d1"),
        node("root/a/b/c2"),
        node("root/a/b2")
    )

    @Test
    fun `funktioniert fuer einen fuenfstufigen Baum ohne Codeaenderung`() {
        val positions = AnimationTreeLayout.compute(tiefBaum)

        // Alle sieben Knoten bekommen eine Position.
        assertEquals(tiefBaum.map { it.id }.toSet(), positions.keys)

        // Tiefe 5 kommt wirklich vor - das ist der eigentliche Beleg.
        assertTrue("Kein Knoten erreicht y=4 (Tiefe 5)", positions.values.any { it.y == 4 })

        // y waechst mit jeder Ebene um genau eins.
        assertEquals(0, positions.getValue("root").y)
        assertEquals(1, positions.getValue("root/a").y)
        assertEquals(2, positions.getValue("root/a/b").y)
        assertEquals(2, positions.getValue("root/a/b2").y)
        assertEquals(3, positions.getValue("root/a/b/c1").y)
        assertEquals(3, positions.getValue("root/a/b/c2").y)
        assertEquals(4, positions.getValue("root/a/b/c1/d1").y)

        // Keine zwei Knoten teilen sich eine Position.
        assertEquals(tiefBaum.size, positions.values.toSet().size)

        // Jeder Elternknoten steht mittig ueber seinen Kindern - bis hoch zur Wurzel.
        val d1x = positions.getValue("root/a/b/c1/d1").x
        assertEquals(d1x, positions.getValue("root/a/b/c1").x, 0.0001f)
        val c2x = positions.getValue("root/a/b/c2").x
        val bx = (positions.getValue("root/a/b/c1").x + c2x) / 2f
        assertEquals(bx, positions.getValue("root/a/b").x, 0.0001f)
        val b2x = positions.getValue("root/a/b2").x
        val ax = (bx + b2x) / 2f
        assertEquals(ax, positions.getValue("root/a").x, 0.0001f)
        assertEquals(ax, positions.getValue("root").x, 0.0001f)
    }

    @Test
    fun `ein Knoten mit unbekanntem Elternteil stuerzt nicht ab, sondern bekommt eine eigene x-Spur`() {
        val verwaist = listOf(node("root"), node("root/a"), node("anderswo/verwaist"))
        val positions = AnimationTreeLayout.compute(verwaist)
        assertEquals(verwaist.map { it.id }.toSet(), positions.keys)
        // "anderswo/verwaist" hat keinen Elternknoten IM ANGEGEBENEN Baum - fuer die x-Spur wird es
        // wie eine zusaetzliche Wurzel behandelt (eigene, von "root" getrennte Spalte), auch wenn
        // sein Pfad (eine Ebene) rechnerisch eine andere Tiefe nahelegt als eine echte Wurzel.
        assertEquals(1, positions.getValue("anderswo/verwaist").y)
        assertTrue(
            "anderswo/verwaist steht nicht in einer eigenen Spalte",
            positions.getValue("anderswo/verwaist").x != positions.getValue("root/a").x
        )
    }

    @Test
    fun `ein leerer Baum ergibt eine leere Zuordnung`() {
        assertEquals(emptyMap<String, AnimationTreeLayout.Position>(), AnimationTreeLayout.compute(emptyList()))
    }
}
