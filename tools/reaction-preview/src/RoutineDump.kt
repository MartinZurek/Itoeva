import com.notime.glyphcore.data.AnimationTree
import com.notime.glyphsim.matrix.PlayScene
import com.notime.glyphsim.matrix.RoutineStep
import com.notime.glyphsim.skilltree.ActivityContext
import com.notime.glyphsim.skilltree.AvatarActivityPlans

fun show(label: String, nodeId: String, place: PlayScene.Place, unlocked: Set<String>) {
    val node = AnimationTree.node(nodeId) ?: error(nodeId)
    val r = AvatarActivityPlans.resolve(null, node, ActivityContext(place, unlocked))
    if (r == null) { println("$label -> kein kontextueller Schnitt (alter Reaktionsweg)"); return }
    val steps = r.routine.steps.joinToString(", ") {
        when (it) {
            is RoutineStep.GoToPlace -> "GoToPlace(${it.place})"
            is RoutineStep.Stroll -> "Stroll"
            is RoutineStep.Football -> "Football.${it.phase}"
            is RoutineStep.Training -> "Training.${it.phase}"
            is RoutineStep.Linger -> "Linger(${it.millis})"
            else -> it::class.simpleName ?: "?"
        }
    }
    println("$label -> [$steps]")
}

fun main() {
    val B = "sport/ballsport"; val K = "sport/kraft-ausdauer"
    println("== Fussball (unveraendert) ==")
    show("Anfaenger, Sportplatz", B, PlayScene.Place.SPORT, setOf(B))
    show("mit Dribbling", B, PlayScene.Place.SPORT, setOf(B, "$B/dribbling"))
    show("aus dem Wohnzimmer", B, PlayScene.Place.LIVING, setOf(B))
    println()
    println("== Kraft & Ausdauer (neu) ==")
    show("Anfaenger, Sportplatz", K, PlayScene.Place.SPORT, emptySet())
    show("Gruppe gelernt", K, PlayScene.Place.SPORT, setOf(K))
    show("mit Heben", K, PlayScene.Place.SPORT, setOf(K, "$K/heben"))
    show("im Park", K, PlayScene.Place.PARK, setOf(K, "$K/heben"))
    show("aus dem Wohnzimmer", K, PlayScene.Place.LIVING, setOf(K, "$K/heben"))
    show("Blatt heben direkt", "$K/heben", PlayScene.Place.SPORT, setOf(K, "$K/heben"))
    println()
    println("== bleibt aussen vor ==")
    show("Summit", "$K/summit", PlayScene.Place.SPORT, setOf(K, "$K/summit"))
    show("Basketball", "$B/basketball", PlayScene.Place.SPORT, setOf(B, "$B/basketball"))
}
