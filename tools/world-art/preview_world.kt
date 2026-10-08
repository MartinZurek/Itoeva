package com.notime.glyphsim.matrix

import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/** Echte Welt-, Bewegungs- und Kamerarechnung; Vorschau ist keine APK-Aufnahme. */
fun main(args: Array<String>) {
    val root = File(args[0]); val output = File(args[1]).apply { mkdirs() }
    val worlds = GameWorld.regions.associate { it.asset to ImageIO.read(File(root,"app-sim/src/game/assets/${it.asset}")) }
    val sheet = ImageIO.read(File(root, "app-sim/src/main/assets/creatures/fennec.png"))
    var place = PlayScene.Place.STREET
    var movement = GameMovement.State(PlayControl.Pos(.22f, .40f))
    var camera = GameCamera.State()
    repeat(300) { frame ->
        val stick = if (frame < 35 || frame > 270 || (place == PlayScene.Place.FOREST && movement.pos.x > .78f)) PlayControl.Stick() else PlayControl.Stick(.75f, 0f)
        repeat(2) {
            val result = GameMovement.tick(movement, stick, 33, GameSurfaces.painted(GameWorld.scene(place)!!),
                immediateExits = GameWorld.immediateExits(place), horizontalScale = GameWorld.horizontalScale(place))
            movement = result.state
            result.exit?.let { dir ->
                val next = GameScenes.exit(GameWorld.scene(place)!!, dir)
                if (next != null) GameWorld.transfer(place, next, dir, movement)?.let { carried -> movement = carried; place = next }
            }
            val currentScene = GameWorld.scene(place)!!
            camera = GameCamera.tick(camera, currentScene, movement.pos, stick, 960f, 540f, 33L,
                GameCamera.interest(currentScene, movement.pos))
        }
        val currentScene = GameWorld.scene(place)!!
        val fit = GameCamera.fit(camera, currentScene, 960f, 540f)
        val worldLeft = fit.left - GameWorld.origin(place) * fit.scale
        val image = BufferedImage(960, 540, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        for (region in GameWorld.regions) g.drawImage(worlds.getValue(region.asset),
            (worldLeft + GameWorld.regionOrigin(region) * fit.scale).roundToInt(), fit.top.roundToInt(),
            (region.width * fit.scale).roundToInt(), (GameWorld.HEIGHT * fit.scale).roundToInt(), null)
        val (x, y) = GameScenes.feet(currentScene, movement.pos)
        val (sx, sy) = fit.toScreen(x, y)
        val px = GameCharacterScale.layoutWidth(currentScene, movement.pos, AvatarSpecies.FENNEC) * fit.scale
        val drawn = px * CreatureSprites.Rich.scaleFor(AvatarSpecies.FENNEC)
        val sprite = if (movement.moving) CreatureSprites.Rich.WALK_FIRST + ((movement.gaitMs / 120).toInt() % 8)
            else CreatureSprites.Rich.IDLE_FIRST + (frame / 8 % 8)
        val source = sheet.getSubimage(sprite * 128, 0, 128, 128)
        g.color = Color(10, 20, 15, 75)
        g.fillOval((sx - 13f * fit.scale).roundToInt(), (sy - fit.scale).roundToInt(),
            (26f * fit.scale).roundToInt(), (5f * fit.scale).roundToInt())
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        g.drawImage(source, (sx - drawn / 2).roundToInt(), (sy - drawn * 126 / 128f - movement.height * fit.scale).roundToInt(),
            drawn.roundToInt(), drawn.roundToInt(), null)
        g.color = Color(18, 28, 22, 180); g.fillRoundRect(15, 15, 280, 28, 12, 12)
        g.color = Color(242, 234, 214); g.drawString("${place.name.lowercase()}  |  zoom %.2f  |  %.1f s".format(camera.zoom, frame / 15f), 25, 34)
        g.dispose()
        ImageIO.write(image, "png", File(output, "frame-%03d.png".format(frame)))
    }
}
