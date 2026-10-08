package com.notime.glyphsim.matrix

import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/** Desktop-Vorschau mit derselben Welt, Bewegungsphysik und Kamera wie im Spiel. */
fun main(args: Array<String>) {
    val root = File(args[0]); val output = File(args[1]).apply { mkdirs() }
    val images = (GameWorld.regions.map { it.asset } + GameWorld.seams.map { it.asset }).associateWith { ImageIO.read(File(root,"app-sim/src/game/assets/$it")) }
    val sheet = ImageIO.read(File(root,"app-sim/src/main/assets/creatures/fennec.png"))
    // Jeweils echte Hin- und Rueckbewegung an allen sechs Bildgrenzen, dazu der Gang ins Wasser.
    val cases = listOf(PlayScene.Place.JUNGLE, PlayScene.Place.COAST_PATH, PlayScene.Place.FOREST,
        PlayScene.Place.VILLAGE_EDGE, PlayScene.Place.MOUNTAINS, PlayScene.Place.MOUNTAIN_PASS, PlayScene.Place.BEACH)
    val selectedCases = args.getOrNull(2)?.split(",")?.map { it.toInt() }?.toSet()
    for ((caseIndex, start) in cases.withIndex()) {
        if (selectedCases != null && caseIndex !in selectedCases) continue
        var frame = caseIndex * 150
        val crossed = mutableSetOf<PlayControl.Dir>()
        var place = start
        var movement = GameMovement.State(PlayControl.Pos(if(start == PlayScene.Place.BEACH) .5f else .72f,.35f))
        var camera = GameCamera.State()
        repeat(150) { index ->
            val stick = if(start == PlayScene.Place.BEACH) PlayControl.Stick(0f,if(index < 75) .48f else -.48f)
                else PlayControl.Stick(if(index < 75) .55f else -.55f,0f)
            repeat(2) {
                val result = GameMovement.tick(movement,stick,33L,GameSurfaces.painted(GameWorld.scene(place)!!),
                    immediateExits = GameWorld.immediateExits(place),horizontalScale = GameWorld.horizontalScale(place))
                movement = result.state
                result.exit?.let { dir -> GameWorld.exitAt(place,dir,movement.pos)?.let { next ->
                    GameWorld.transfer(place,next,dir,movement)?.let { movement = it; place = next; crossed += dir }
                } }
                camera = GameCamera.tick(camera,GameWorld.scene(place)!!,movement.pos,stick,960f,540f,33L)
            }
            val scene = GameWorld.scene(place)!!
            val fit = GameCamera.fit(camera,scene,960f,540f)
            val left = fit.left - GameWorld.origin(place)*fit.scale
            val image = BufferedImage(960,540,BufferedImage.TYPE_INT_RGB)
            val g = image.createGraphics()
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            for(region in GameWorld.regions) g.drawImage(images.getValue(region.asset),
                (left+GameWorld.regionOrigin(region)*fit.scale).roundToInt(),fit.top.roundToInt(),
                (region.width*fit.scale).roundToInt(),(GameWorld.HEIGHT*fit.scale).roundToInt(),null)
            for (seam in GameWorld.seams) {
                if (left+(seam.x+GameWorld.SEAM_HALF)*fit.scale < 0f || left+(seam.x-GameWorld.SEAM_HALF)*fit.scale > 960f) continue
                val patch=images.getValue(seam.asset)
                fun strip(offset: Float, width: Float, alpha: Float) {
                    val sx0=((offset+GameWorld.SEAM_HALF)/(2f*GameWorld.SEAM_HALF)*patch.width).roundToInt()
                    val sx1=((offset+width+GameWorld.SEAM_HALF)/(2f*GameWorld.SEAM_HALF)*patch.width).roundToInt()
                    val x0=(left+(seam.x+offset)*fit.scale).roundToInt()
                    val x1=(left+(seam.x+offset+width)*fit.scale).roundToInt()
                    g.composite=java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER,alpha)
                    g.drawImage(patch,x0,fit.top.roundToInt(),x1,(fit.top+GameWorld.HEIGHT*fit.scale).roundToInt(),sx0,0,sx1,patch.height,null)
                }
                val edge=GameWorld.SEAM_HALF-GameWorld.SEAM_FEATHER
                strip(-edge,2f*edge,1f)
                repeat(16) { i ->
                    val width=GameWorld.SEAM_FEATHER/16f
                    val a=-GameWorld.SEAM_HALF+i*width; val b=edge+i*width
                    strip(a,width,GameWorld.seamAlpha(a+width/2f))
                    strip(b,width,GameWorld.seamAlpha(b+width/2f))
                }
            }
            g.composite=java.awt.AlphaComposite.SrcOver
            val (fx,fy) = GameScenes.feet(scene,movement.pos); val (x,y) = fit.toScreen(fx,fy)
            val drawn = GameScenes.avatarHeight(scene,movement.pos)*fit.scale/.8f*CreatureSprites.Rich.scaleFor(AvatarSpecies.FENNEC)
            val sprite = CreatureSprites.Rich.WALK_FIRST + ((movement.gaitMs / 120).toInt() % 8)
            val source = sheet.getSubimage(sprite*128,0,128,128)
            val wet = GameWorld.wetness(scene,movement.pos)
            val waterY = y-GameScenes.avatarHeight(scene,movement.pos)*wet*fit.scale
            val clip = g.clip; g.clipRect(0,0,960,waterY.roundToInt())
            g.drawImage(source,(x-drawn/2).roundToInt(),(y-drawn*126/128f-movement.height*fit.scale).roundToInt(),drawn.roundToInt(),drawn.roundToInt(),null)
            g.clip = clip
            if(wet > 0f) { g.color = Color(230,248,244,160); g.drawOval((x-21*fit.scale).roundToInt(),(waterY-2*fit.scale).roundToInt(),(42*fit.scale).roundToInt(),(7*fit.scale).roundToInt()) }
            g.color = Color(18,28,22,210); g.fillRoundRect(14,14,480,29,10,10)
            g.color = Color(242,234,214); g.drawString("Desktop-Test | $place | ${if(index<75) "Hinweg" else "Rueckweg"} | Zoom %.2f".format(camera.zoom),24,34)
            g.dispose(); ImageIO.write(image,"png",File(output,"frame-%04d.png".format(frame++)))
        }
        if (start != PlayScene.Place.BEACH) check(crossed.containsAll(listOf(PlayControl.Dir.RIGHT, PlayControl.Dir.LEFT))) {
            "$start: Die Vorschau hat die Bildgrenze nicht in beiden Richtungen durchlaufen: $crossed"
        }
        println("$start: ${if (start == PlayScene.Place.BEACH) "Uferbewegung" else "Hin-/Rueckgrenze $crossed"}")
    }
}
