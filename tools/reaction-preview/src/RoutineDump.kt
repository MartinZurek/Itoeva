import com.notime.glyphcore.data.AnimationTree
import com.notime.glyphsim.matrix.PlayScene
import com.notime.glyphsim.matrix.RoutineStep
import com.notime.glyphsim.skilltree.ActivityContext
import com.notime.glyphsim.skilltree.AvatarActivity
import com.notime.glyphsim.skilltree.AvatarActivityPlans

fun show(
    label: String,
    nodeId: String,
    place: PlayScene.Place,
    unlocked: Set<String>,
    /** Laufende Beschaeftigung - fuer den Einlage-Fall, siehe AvatarActivityPlans.isInsert. */
    current: String? = null
) {
    val node = AnimationTree.node(nodeId) ?: error(nodeId)
    val running = current?.let { AvatarActivity(it, System.currentTimeMillis()) }
    val r = AvatarActivityPlans.resolve(running, node, ActivityContext(place, unlocked))
    if (r == null) { println("$label -> kein kontextueller Schnitt (alter Reaktionsweg)"); return }
    val steps = r.routine.steps.joinToString(", ") {
        when (it) {
            is RoutineStep.GoToPlace -> "GoToPlace(${it.place})"
            is RoutineStep.Stroll -> "Stroll"
            is RoutineStep.Football -> "Football.${it.phase}"
            is RoutineStep.Training -> "Training.${it.phase}"
            is RoutineStep.Music -> "Music.${it.phase}"
            is RoutineStep.Linger -> "Linger(${it.millis})"
            else -> it::class.simpleName ?: "?"
        }
    }
    println("$label -> [$steps]")
}

fun main() {
    val B = "sport/ballsport"; val K = "sport/kraft-ausdauer"; val M = "kreativ/musik"
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
    println("== Musizieren (neu) ==")
    show("Anfaenger, Park", M, PlayScene.Place.PARK, emptySet())
    show("Gruppe gelernt", M, PlayScene.Place.PARK, setOf(M))
    show("mit Singen", M, PlayScene.Place.PARK, setOf(M, "$M/singen"))
    show("im Wohnzimmer", M, PlayScene.Place.LIVING, setOf(M, "$M/singen"))
    show("in der Leseecke", M, PlayScene.Place.NOOK, setOf(M, "$M/singen"))
    show("aus der Kueche", M, PlayScene.Place.KITCHEN, setOf(M, "$M/singen"))
    show("aus dem Schlafzimmer", M, PlayScene.Place.BEDROOM, setOf(M, "$M/singen"))
    show("Blatt singen direkt", "$M/singen", PlayScene.Place.PARK, setOf(M, "$M/singen"))
    println()
    println("== Einlage in eine LAUFENDE Beschaeftigung (kein neuer Anlauf) ==")
    show("Dribbling, spielt schon Ball", "$B/dribbling", PlayScene.Place.SPORT,
        setOf(B, "$B/dribbling"), current = B)
    show("Heben, trainiert schon", "$K/heben", PlayScene.Place.SPORT,
        setOf(K, "$K/heben"), current = K)
    show("Heben ungelernt, trainiert schon", "$K/heben", PlayScene.Place.SPORT,
        setOf(K), current = K)
    show("Singen, musiziert schon", "$M/singen", PlayScene.Place.KITCHEN,
        setOf(M, "$M/singen"), current = M)
    println()
    println("== bleibt aussen vor ==")
    show("Summit", "$K/summit", PlayScene.Place.SPORT, setOf(K, "$K/summit"))
    show("Basketball", "$B/basketball", PlayScene.Place.SPORT, setOf(B, "$B/basketball"))
    show("Drum", "$M/drum", PlayScene.Place.PARK, setOf(M, "$M/drum"))
    show("Bauen & Malen", "kreativ/bauen-malen", PlayScene.Place.MEADOW, setOf("kreativ/bauen-malen"))
}
