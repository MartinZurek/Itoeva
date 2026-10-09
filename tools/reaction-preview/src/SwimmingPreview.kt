import com.notime.glyphsim.matrix.*
import java.io.File
import javax.imageio.ImageIO

/** Exportiert Wasserkaemme, Maske, Bildrollen und Massstab aus produktivem Kotlin. */
fun main(args: Array<String>) {
    val root=File(args[0]); val out=File(args[1]).apply { mkdirs() }
    val scene=GameWorld.scene(PlayScene.Place.BEACH)!!
    val pos=GameTerrain.clamp(scene,PlayControl.Pos(.5f,.9f))
    val coast=ImageIO.read(File(root,"app-sim/src/game/assets/world/coast.png"))
    val runs=GameWater.measure("world/coast.png") { x,y -> coast.getRGB(
        (x*coast.width).toInt().coerceIn(0,coast.width-1),(y*coast.height).toInt().coerceIn(0,coast.height-1)) }
    File(out,"mask.json").writeText(runs.joinToString(",","[","]") { "[${it.x0},${it.x1},${it.y}]" })
    for(tick in 0 until 60) {
        val clock=tick*45L
        val dir=listOf(PlayControl.Dir.RIGHT,PlayControl.Dir.DOWN,PlayControl.Dir.UP)[tick/20]
        val rows=AvatarSpecies.entries.map { species ->
            val swim=GameWater.swim(scene,pos,0f,dir,true,clock,species)!!
            val drawn=GameCharacterScale.layoutWidth(scene,pos,species)*CreatureSprites.Rich.scaleFor(species)
            "{\"name\":\"${species.name.lowercase()}\",\"frame\":${GameWater.swimFrame(swim.stroke,dir)},\"drawn\":$drawn,\"rise\":${GameCharacterScale.waterRise(scene,pos)},\"lift\":${swim.buoyancy-swim.bob}}"
        }
        val waves=(24..53).flatMap { row -> (0..60).map { column -> GameWater.crest(column,row,clock) } }
        File(out,"$tick.json").writeText("{\"clock\":$clock,\"direction\":\"$dir\",\"characters\":[${rows.joinToString(",")}],\"waves\":"+
            waves.joinToString(",","[","]") { "[${it.x},${it.y},${it.width},${it.light}]" }+"}")
    }
}
