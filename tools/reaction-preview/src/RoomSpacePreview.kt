import com.notime.glyphsim.matrix.*
import java.io.File

/** Exportiert die produktiven Raumkoerper fuer eine Softwarevorschau ohne Android. */
fun main(args: Array<String>) {
    val output=File(args[0]).apply { mkdirs() }
    for(place in listOf(PlayScene.Place.LIVING,PlayScene.Place.BEDROOM,PlayScene.Place.CAFE)) {
        val scene=GameWorld.scene(place)!!
        val palette=GameRoomSpace.palette(place)
        val bodies=GameRoomSpace.bodies(scene).joinToString(",") { body ->
            val faces=body.faces.joinToString(",") { face ->
                "{\"shade\":${face.shade},\"points\":[${face.points.joinToString(",") { "[${it.first},${it.second}]" }}]}"
            }
            "{\"id\":\"${body.piece.id}\",\"material\":\"${body.material}\",\"top\":${body.piece.top},\"ground\":${body.piece.ground},\"faces\":[$faces]}"
        }
        val doors=GameWorld.passages(place).filter { it.door }.map { GameDoors.aperture(it.from,it.to) }
            .distinct().joinToString(",") { "[${it.x0},${it.y0},${it.x1},${it.y1}]" }
        val species=AvatarSpecies.FENNEC
        val positions=listOf(GameScenes.posAt(scene,245f,153f),GameScenes.posAt(scene,245f,245f))
        val avatars=positions.joinToString(",") { p ->
            val (x,y)=GameScenes.feet(scene,p)
            "{\"x\":$x,\"y\":$y,\"height\":${GameCharacterScale.visibleHeight(scene,p,species)}}"
        }
        File(output,"${place.name.lowercase()}.json").writeText(
            "{\"place\":\"$place\",\"palette\":[${listOf(palette.wall,palette.side,palette.floor,palette.trim,palette.accent).joinToString(",")}],\"doors\":[$doors],\"bodies\":[$bodies],\"avatars\":[$avatars]}")
    }
}
