import com.notime.glyphcore.data.AnimationTree
import com.notime.glyphsim.matrix.AvatarAnimations
import com.notime.glyphsim.matrix.AvatarGeometry
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.ReactionTrigger
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

const val W = AvatarGeometry.SIZE
const val H = AvatarGeometry.HEIGHT
const val CELL = 5
const val COLS = 8
const val LABEL = 230

val OFF = Color(0x23, 0x23, 0x23)
val ON = Color(0xF3, 0xF1, 0xEA)

fun lerp(f: Float): Color = Color(
    (OFF.red + (ON.red - OFF.red) * f).toInt(),
    (OFF.green + (ON.green - OFF.green) * f).toInt(),
    (OFF.blue + (ON.blue - OFF.blue) * f).toInt()
)

fun drawFrame(img: BufferedImage, frame: IntArray, ox: Int, oy: Int) {
    val g = img.createGraphics()
    g.color = Color.BLACK
    g.fillRect(ox, oy, W * CELL, H * CELL)
    for (y in 0 until H) for (x in 0 until W) {
        val b = frame.getOrElse(y * W + x) { 0 }
        if (b <= 0) continue
        g.color = lerp((b.coerceIn(0, AvatarGeometry.MAX_BRIGHTNESS).toFloat() / AvatarGeometry.MAX_BRIGHTNESS))
        g.fillRect(ox + x * CELL, oy + y * CELL, CELL, CELL)
    }
    g.dispose()
}

/** Gleichmaessig ueber die Sequenz verteilte Stichproben - der erste und der letzte immer dabei. */
fun sample(n: Int, want: Int): List<Int> {
    if (n <= want) return (0 until n).toList()
    return (0 until want).map { it * (n - 1) / (want - 1) }
}

fun main(args: Array<String>) {
    val species = AvatarSpecies.valueOf(args.getOrElse(0) { "PUFFLING" })
    val outDir = File(args.getOrElse(1) { "sheets" }).apply { mkdirs() }

    for (root in AnimationTree.roots()) {
        val nodes = AnimationTree.nodes.filter {
            it.motif != null && AnimationTree.fallbackChain(it.id).last() == root.id
        }
        if (nodes.isEmpty()) continue
        val rowH = H * CELL + 16
        val img = BufferedImage(LABEL + COLS * W * CELL, nodes.size * rowH + 24, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.color = Color(0x14, 0x14, 0x16); g.fillRect(0, 0, img.width, img.height)
        g.color = Color.WHITE
        g.font = Font("SansSerif", Font.BOLD, 14)
        g.drawString("${root.id}   (${species.name})", 8, 17)
        g.dispose()

        nodes.forEachIndexed { i, node ->
            val seq = AvatarAnimations.reactionFor(species, ReactionTrigger.Node(node.id))
            val idx = sample(seq.frames.size, COLS)
            val y = 24 + i * rowH
            val gg = img.createGraphics()
            gg.color = Color(0xC8, 0xC4, 0xBA)
            gg.font = Font("SansSerif", Font.PLAIN, 11)
            gg.drawString(node.id.substringAfter('/'), 6, y + 14)
            gg.color = Color(0x8A, 0x86, 0x7E)
            gg.font = Font("SansSerif", Font.PLAIN, 10)
            gg.drawString("${seq.frames.size} Bilder", 6, y + 28)
            gg.dispose()
            idx.forEachIndexed { c, f -> drawFrame(img, seq.frames[f], LABEL + c * W * CELL, y) }
        }
        ImageIO.write(img, "png", File(outDir, root.id + ".png"))
        println("${root.id}: ${nodes.size} Knoten")
    }
}
