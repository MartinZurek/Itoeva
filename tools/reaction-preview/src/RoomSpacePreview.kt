import com.notime.glyphsim.matrix.*
import java.io.File

/** Exportiert die produktiven Raumkoerper fuer eine Softwarevorschau ohne Android. */
fun main(args: Array<String>) {
    val output=File(args[0]).apply { mkdirs() }
    for(place in PlayScene.Place.entries.filter { GameWorld.scene(it)?.let(GameRoomSpace::enabled)==true }) {
        val scene=GameWorld.scene(place)!!
        val palette=GameRoomSpace.palette(place)
        val bodies=GameRoomSpace.bodies(scene).joinToString(",") { body ->
            val faces=body.faces.joinToString(",") { face ->
                "{\"shade\":${face.shade},\"points\":[${face.points.joinToString(",") { "[${it.first},${it.second}]" }}]}"
            }
            "{\"id\":\"${body.piece.id}\",\"material\":\"${body.material}\",\"top\":${body.piece.top},\"ground\":${body.piece.ground},\"front\":${body.piece.front},\"faces\":[$faces]}"
        }
        fun points(points: List<Pair<Float,Float>>) = "[${points.joinToString(",") { "[${it.first},${it.second}]" }}]"
        fun mark(mark: GameRoomArt.Mark): String =
            "{\"color\":${mark.color},\"oval\":${mark.oval},\"points\":${points(mark.points)},\"clip\":${mark.clip?.let(::points) ?: "null"}}"
        val art=GameRoomArt.marks(scene).joinToString(",",transform=::mark)
        val doorArt=GameWorld.passages(place).filter { it.door }.map { GameDoors.aperture(it.from,it.to) }
            .distinct().joinToString(",") { box ->
                val width=(box.x1-box.x0).toInt();val height=(box.y1-box.y0).toInt()
                "{\"box\":[${box.x0},${box.y0},${box.x1},${box.y1}],\"marks\":[${GameRoomArt.door(width,height).joinToString(",",transform=::mark)}]}"
            }
        val doors=GameWorld.passages(place).filter { it.door }.map { GameDoors.aperture(it.from,it.to) }
            .distinct().joinToString(",") { "[${it.x0},${it.y0},${it.x1},${it.y1}]" }
        val species=AvatarSpecies.FENNEC
        val positions=listOf(GameScenes.posAt(scene,245f,130f),GameScenes.posAt(scene,245f,245f))
        val avatars=positions.joinToString(",") { p ->
            val (x,y)=GameScenes.feet(scene,p)
            "{\"x\":$x,\"y\":$y,\"height\":${GameCharacterScale.visibleHeight(scene,p,species)}}"
        }
        File(output,"${place.name.lowercase()}.json").writeText(
            "{\"place\":\"$place\",\"palette\":[${listOf(palette.wall,palette.side,palette.floor,palette.trim,palette.accent).joinToString(",")}],\"art\":[$art],\"doorArt\":[$doorArt],\"doors\":[$doors],\"bodies\":[$bodies],\"avatars\":[$avatars]}")
    }
}
