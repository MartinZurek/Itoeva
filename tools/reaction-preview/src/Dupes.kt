import com.notime.glyphcore.data.AnimationTree
import com.notime.glyphsim.matrix.AvatarAnimations
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.ReactionTrigger

fun fingerprint(species: AvatarSpecies, nodeId: String): String {
    val s = AvatarAnimations.reactionFor(species, ReactionTrigger.Node(nodeId))
    return s.frames.joinToString("|") { f -> f.joinToString(",") } + "##" + s.holdsMs.joinToString(",")
}

fun main() {
    val species = AvatarSpecies.PUFFLING
    val nodes = AnimationTree.nodes.filter { it.motif != null }
    val byPrint = LinkedHashMap<String, MutableList<String>>()
    for (n in nodes) byPrint.getOrPut(fingerprint(species, n.id)) { mutableListOf() }.add(n.id)

    val groups = byPrint.values.filter { it.size > 1 }.sortedByDescending { it.size }
    println("Knoten mit Motiv: ${nodes.size}")
    println("Verschiedene Reaktionen: ${byPrint.size}")
    println("Davon nur einmal vorhanden: ${byPrint.values.count { it.size == 1 }}")
    println()
    println("== Knoten, die BILDGLEICH dieselbe Reaktion spielen ==")
    for (g in groups) {
        println("  ${g.size}x  ${g.joinToString("  ")}")
    }
}
