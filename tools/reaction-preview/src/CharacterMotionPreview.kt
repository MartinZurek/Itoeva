import com.notime.glyphsim.matrix.*
import java.io.File
import javax.imageio.ImageIO

/** Die Vorschau liest die produktive Kotlin-Netzrechnung, keine nachgebaute Python-Physik. */
fun main(args: Array<String>) {
    val root = File(args[0])
    val out = File(args[1]).apply { mkdirs() }
    val follow = AvatarSpecies.entries.associateWith { GameCharacterMotion.Follow() }
    val sheets = AvatarSpecies.entries.associateWith {
        ImageIO.read(File(root, "app-sim/src/main/assets/creatures/${it.name.lowercase()}.png"))
    }
    for (tick in 0 until 80) {
        val clock = tick * 50L
        val speed = if (clock in 800L until 2000L) 1.6f else 0f
        val role = when {
            clock < 800L -> (tick / 2) % 8
            speed > 0 -> 9 + ((clock - 800) / 95 % 8).toInt()
            else -> 95
        }
        for (species in AvatarSpecies.entries) {
            val image = sheets.getValue(species).getSubimage(role*128, 0, 128, 128)
            val top = (0 until 128).first { y -> (0 until 128).any { x -> (image.getRGB(x,y) ushr 24) > 128 } }
            val vertices = FloatArray(GameCharacterMotion.VERTICES * 2)
            val response = follow.getValue(species).update(clock, speed, .65f)
            GameCharacterMotion.fill(vertices, species, role, top / 128f, clock,
                if (speed > 0) clock - 800 else 0, speed, .65f, response)
            File(out, "${species.name.lowercase()}-$tick.csv").writeText(
                "$role,${GameCharacterScale.reference(species).top},${GameCharacterScale.reference(species).relativeHeight}\n" +
                    vertices.joinToString(","))
        }
    }
}
