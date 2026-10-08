package com.notime.glyphsim.matrix

import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/** Kontaktbogen mit echter Welt-/Kameraprojektion; keine Android- oder Compose-Aufnahme. */
fun main(args: Array<String>) {
    val root = File(args[0]); val output = File(args[1])
    val sheet = ImageIO.read(File(root, "app-sim/src/main/assets/creatures/fennec.png"))
    val source = sheet.getSubimage(CreatureSprites.Rich.IDLE_FIRST * 128,0,128,128)
    val result = BufferedImage(2400,1350,BufferedImage.TYPE_INT_RGB)
    for ((index, place) in PlayScene.Place.entries.withIndex()) {
        val scene = GameWorld.scene(place)!!
        val pos = PlayControl.Pos(.5f, if (place == PlayScene.Place.BEACH) .8f else .5f)
        val camera = GameCamera.tick(GameCamera.State(),scene,pos,PlayControl.Stick(),480f,270f,0L)
        val fit = GameCamera.fit(camera,scene,480f,270f)
        val image = ImageIO.read(File(root,"app-sim/src/game/assets/${scene.asset}"))
        val g = result.createGraphics()
        g.translate(index % 5 * 480,index / 5 * 270); g.clipRect(0,0,480,270)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        val left = fit.left - if (GameWorld.isWorld(scene)) GameWorld.origin(place) * fit.scale else 0f
        if (GameWorld.isWorld(scene)) for (region in GameWorld.regions) {
            val atlas = ImageIO.read(File(root,"app-sim/src/game/assets/${region.asset}"))
            g.drawImage(atlas,(left+GameWorld.regionOrigin(region)*fit.scale).roundToInt(),fit.top.roundToInt(),
                (region.width*fit.scale).roundToInt(),(GameWorld.HEIGHT*fit.scale).roundToInt(),null)
        } else g.drawImage(image,left.roundToInt(),fit.top.roundToInt(),
            (480*fit.scale).roundToInt(),(270*fit.scale).roundToInt(),null)
        val (fx,fy) = GameScenes.feet(scene,pos); val (x,y) = fit.toScreen(fx,fy)
        val drawn = GameScenes.avatarHeight(scene,pos)*fit.scale/.8f*CreatureSprites.Rich.scaleFor(AvatarSpecies.FENNEC)
        val waterY = y - GameScenes.avatarHeight(scene,pos)*GameWorld.wetness(scene,pos)*fit.scale
        val oldClip = g.clip
        g.clipRect(0,0,480,waterY.roundToInt())
        g.drawImage(source,(x-drawn/2).roundToInt(),(y-drawn*126/128f).roundToInt(),drawn.roundToInt(),drawn.roundToInt(),null)
        g.clip = oldClip
        g.font = Font("SansSerif",Font.PLAIN,10)
        for (p in GameWorld.passages(place).filter { it.door || !GameWorld.connected(place,it.to) }) {
            val (px,py) = GameScenes.feet(scene,p.pos); val (sx,sy) = fit.toScreen(px,py)
            g.color = Color(245,216,155)
            if (p.door) g.drawRect((sx-16*fit.scale).roundToInt(),(sy-65*fit.scale).roundToInt(),(32*fit.scale).roundToInt(),(65*fit.scale).roundToInt())
            else g.drawOval((sx-8).roundToInt(),(sy-8).roundToInt(),16,16)
            g.drawString(p.to.name,(sx-18).roundToInt(),(sy-(if(p.door)70 else 16)*fit.scale).roundToInt())
        }
        g.color = Color(18,28,22,210);g.fillRect(0,0,480,20)
        g.color = Color(242,234,214); g.drawString("$place | ${scene.asset} | depth=${pos.depth}",8,14)
        g.dispose()
    }
    output.parentFile?.mkdirs();ImageIO.write(result,"png",output)
}
