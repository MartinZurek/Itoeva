package com.notime.glyphsim.matrix

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/** Vorschau benutzt die echte Kotlin-Bewegungsrechnung; Android-Zeichnung separat per CI pruefen. */
fun main(args: Array<String>) {
    val root = File(args[0])
    val output = File(args[1]).apply { mkdirs() }
    val assets = File(root,"app-sim/src/game/assets/scenes")
    val places = listOf(PlayScene.Place.LIVING,PlayScene.Place.BEDROOM,PlayScene.Place.FOREST,
        PlayScene.Place.BEACH,PlayScene.Place.SWAMP,PlayScene.Place.POND)
    val images = places.associateWith { place ->
        val name = place.name.lowercase()
        ImageIO.read(File(assets,"${name}_live.png")) to ImageIO.read(File(assets,"${name}_parts.png"))
    }
    repeat(80) { frame ->
        val clock = frame * 100L
        val composite = BufferedImage(960,810,BufferedImage.TYPE_INT_RGB)
        val g = composite.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        places.forEachIndexed { index,place ->
            val (base,atlas) = images.getValue(place)
            val room = GameRoomCatalog.rooms.getValue(place)
            val ox = index % 2 * 480
            val oy = index / 2 * 270
            val local = g.create(ox,oy,480,270)
            local.drawImage(base,0,0,null)
            room.parts.forEach { p ->
                val pose = GameEnvironment.pose(p,clock)
                val part = atlas.getSubimage(p.sx,p.sy,p.w,p.h)
                val pg = local.create()
                if (p.kind == GameEnvironment.Kind.WATER) {
                    pg.clipRect(p.x,p.y,p.w,p.h)
                    for (wrap in -1..1) pg.drawImage(part,p.x+pose.dx.roundToInt()+wrap*p.w,p.y,null)
                } else {
                    val pivotY = if (p.kind == GameEnvironment.Kind.CURTAIN) p.y else p.y+p.h
                    (pg as java.awt.Graphics2D).rotate(Math.toRadians(pose.angle.toDouble()),p.x+p.w/2.0,pivotY.toDouble())
                    pg.drawImage(part,p.x+pose.dx.roundToInt(),p.y+pose.dy.roundToInt(),null)
                }
                pg.dispose()
            }
            // Materialkontakt-Demonstration auf einem echten Fusspunkt der Gehflaeche.
            if (place == PlayScene.Place.BEACH || place == PlayScene.Place.SWAMP) {
                val scene = GameScenes.of(place)!!
                val grid = GameEnvironment.Grid(File(assets,"${place.name.lowercase()}_materials.bin").readBytes(),room.material)
                val x = if (place == PlayScene.Place.BEACH) 420f else 418f
                val y = if (place == PlayScene.Place.BEACH) 163f else 202f
                check(grid.at(x,y) == GameEnvironment.Material.WATER)
                val life = (clock % 2400L) / 1000f
                val radius = 2 + life*13
                local.color = Color(208,239,225)
                local.drawOval((x-radius).toInt(),(y-radius*.28f).toInt(),(radius*2).toInt(),(radius*.56f).toInt())
            }
            local.color = Color(15,21,27,185)
            local.fillRect(5,5,180,18)
            local.color = Color(245,239,219)
            local.drawString("${place.name.lowercase()} | ${clock / 1000f}s",10,18)
            local.dispose()
        }
        g.dispose()
        ImageIO.write(composite,"png",File(output,"frame-%03d.png".format(frame)))
    }
}
