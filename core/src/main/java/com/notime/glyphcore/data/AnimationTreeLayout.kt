package com.notime.glyphcore.data

/**
 * Ordnet jedem Knoten des [AnimationTree] eine Position auf einer zweidimensionalen Flaeche zu -
 * die Grundlage fuer das Wander-Brett, auf dem der Baum statt als Liste als raeumliches Netz aus
 * Knoten und Verbindungslinien gezeigt wird (Vorbild: der Skillbaum aus Diablo 2, das Sphere Grid
 * aus Final Fantasy X).
 *
 * **Bewusst tiefenunabhaengig.** Der Baum selbst kennt keine feste Anzahl Ebenen - [AnimationNode.depth]
 * und [AnimationNode.parentId] werden rein aus dem `/`-getrennten Pfad berechnet, nicht aus einer
 * Drei-Ebenen-Annahme. Dieses Layout rechnet deshalb genauso: `y` ist unmittelbar die Tiefe, `x`
 * entsteht aus einem klassischen Node-Link-Baumlayout, das mit jeder Anzahl Ebenen funktioniert.
 * Ein fuenfter oder sechster Level braucht hier keine Codeaenderung - siehe `AnimationTreeLayoutTest`
 * mit einem eigens dafuer gebauten fuenfstufigen Baum.
 *
 * ## Das Layout
 *
 * - **Blaetter** (Knoten ohne Kinder) bekommen fortlaufende `x`-Werte in der Reihenfolge, in der
 *   sie in einem Tiefendurchlauf (Vorfahre vor Nachfahre, Geschwister in Listenreihenfolge)
 *   erreicht werden.
 * - **Innere Knoten** bekommen als `x` den Durchschnitt der `x`-Werte ihrer Kinder - dadurch steht
 *   ein Elternknoten immer mittig ueber seinem eigenen Teilbaum, egal wie breit der ist.
 * - **Jede Hauptgruppe** (Wurzel) bekommt eine eigene Spalten-Spur mit Abstand zur naechsten, damit
 *   sich Teilbaeume verschiedener Wurzeln auf dem Brett nie beruehren.
 * - `y` ist [AnimationNode.depth] minus eins, also 0 fuer die Wurzeln.
 */
object AnimationTreeLayout {

    /** Position auf dem Brett - Zellen-Einheiten, keine Pixel. Der Aufrufer skaliert auf dp. */
    data class Position(val x: Float, val y: Int)

    /** Abstand zwischen den Blatt-Spalten zweier benachbarter Wurzeln, in Zellen-Einheiten. */
    private const val ROOT_GAP = 1f

    /**
     * Berechnet die Positionen fuer [nodes].
     *
     * Standardmaessig der komplette [AnimationTree], aber bewusst als Parameter: Ein Test kann
     * damit einen eigens gebauten, tieferen Baum durchrechnen lassen, ohne den echten Katalog
     * anzufassen.
     *
     * Ein Knoten, dessen [AnimationNode.parentId] in [nodes] nicht vorkommt, wird wie eine
     * zusaetzliche Wurzel behandelt (eigene Spur) statt die Berechnung abstuerzen zu lassen -
     * robuster als eine Annahme, die nur beim vollstaendigen Baum stimmt.
     */
    fun compute(nodes: List<AnimationNode> = AnimationTree.nodes): Map<String, Position> {
        if (nodes.isEmpty()) return emptyMap()

        val byId = nodes.associateBy { it.id }
        val byParent: Map<String?, List<AnimationNode>> = nodes.groupBy { node ->
            node.parentId?.takeIf { it in byId }
        }
        val roots = byParent[null].orEmpty()

        val xById = mutableMapOf<String, Float>()
        var nextLeafX = 0f

        fun layout(node: AnimationNode) {
            val children = byParent[node.id].orEmpty()
            if (children.isEmpty()) {
                xById[node.id] = nextLeafX
                nextLeafX += 1f
            } else {
                for (child in children) layout(child)
                xById[node.id] = children.map { xById.getValue(it.id) }.average().toFloat()
            }
        }

        for (root in roots) {
            layout(root)
            nextLeafX += ROOT_GAP
        }

        return nodes.associate { node -> node.id to Position(xById.getValue(node.id), node.depth - 1) }
    }
}
