package com.notime.glyphsim.matrix

import java.awt.*
import java.awt.geom.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/** Originalassets und produktive Kamera/Konturen, keine APK-Aufnahme oder zweite Physik. */
fun main(args: Array<String>) {
    val root=File(args[0]); val out=File(args[1]).apply { mkdirs() }
    fun asset(path: String)=ImageIO.read(File(root,"app-sim/src/game/assets/$path"))
    fun save(image: BufferedImage,name: String) {
        val writer=ImageIO.getImageWritersByFormatName("jpeg").next()
        ImageIO.createImageOutputStream(File(out,name)).use { stream ->
            writer.output=stream
            val params=writer.defaultWriteParam
            params.compressionMode=javax.imageio.ImageWriteParam.MODE_EXPLICIT;params.compressionQuality=.90f
            writer.write(null,javax.imageio.IIOImage(image,null,null),params)
        };writer.dispose()
    }
    fun polygon(points: List<Pair<Float,Float>>) = Path2D.Float().apply {
        points.forEachIndexed { i,(x,y) -> if(i==0) moveTo(x.toDouble(),y.toDouble()) else lineTo(x.toDouble(),y.toDouble()) }
        closePath()
    }
    val sheet=ImageIO.read(File(root,"app-sim/src/main/assets/creatures/fennec.png"))
    fun room(place: PlayScene.Place): BufferedImage {
        val scene=GameWorld.scene(place)!!;val pos=GameAdventure.safePosition(place,PlayControl.Pos(.5f,.65f))
        val camera=GameCamera.tick(GameCamera.State(),scene,pos,PlayControl.Stick(),640f,360f,0)
        val fit=GameCamera.fit(camera,scene,640f,360f)
        val image=BufferedImage(640,360,BufferedImage.TYPE_INT_RGB);val g=image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.translate(fit.left.toDouble(),fit.top.toDouble());g.scale(fit.scale.toDouble(),fit.scale.toDouble())
        g.drawImage(asset(scene.asset),0,0,480,270,null)
        val (x,y)=GameScenes.feet(scene,pos)
        val clip=Area(Rectangle2D.Float(-480f,-270f,1440f,810f))
        for(piece in GameFurniture.pieces(scene).filter { it.hides(y,null) })
            for(points in piece.contours) clip.subtract(Area(polygon(points)))
        g.clip=clip
        val width=GameCharacterScale.layoutWidth(scene,pos,AvatarSpecies.FENNEC)*CreatureSprites.Rich.scaleFor(AvatarSpecies.FENNEC)
        val top=y-width*CreatureSprites.Rich.FEET/128f
        val frame=CreatureSprites.lookRich(AvatarAnimations.idlePose(AvatarSpecies.FENNEC),AvatarSpecies.FENNEC,
            AvatarShading.Side.NONE,0,PlayControl.Dir.RIGHT,false,CreatureSprites.Turn(),0).frame
        g.drawImage(sheet,(x-width/2).roundToInt(),top.roundToInt(),(x+width/2).roundToInt(),(top+width).roundToInt(),
            frame*128,0,(frame+1)*128,128,null)
        g.dispose();return image
    }
    val rooms=BufferedImage(1280,360,BufferedImage.TYPE_INT_RGB);val rg=rooms.createGraphics()
    rg.drawImage(room(PlayScene.Place.LIVING),0,0,null);rg.drawImage(room(PlayScene.Place.BEDROOM),640,0,null);rg.dispose()
    save(rooms,"painted-interiors-preview.jpg")
    val map=BufferedImage(960,GameWorld.regions.size*320,BufferedImage.TYPE_INT_RGB);val g=map.createGraphics()
    GameWorld.regions.forEachIndexed { index,region ->
        val old=g.transform;g.translate(0,index*320);g.scale(.5,.5)
        g.drawImage(asset(region.asset),0,0,1920,640,null)
        val floor=Area()
        for(place in region.places) {
            val scene=GameWorld.scene(place)!!;val offset=GameWorld.origin(place)-GameWorld.regionOrigin(region)
            val points=buildList {
                for(i in 0..80) add(offset+i/80f*region.section to GameTerrain.band(scene,i/80f).first)
                for(i in 80 downTo 0) add(offset+i/80f*region.section to GameTerrain.band(scene,i/80f).second)
            };floor.add(Area(polygon(points)))
        }
        for(area in GameWalkingMap.areas[region.asset].orEmpty().filter { it.material==null }) floor.subtract(Area(polygon(area.points)))
        g.color=Color(49,230,140,80);g.fill(floor);g.color=Color(60,255,165,210);g.stroke=BasicStroke(2f);g.draw(floor)
        val oldClip=g.clip;g.clip=floor
        for(place in region.places) {
            val scene=GameWorld.scene(place)!!;val offset=GameWorld.origin(place)-GameWorld.regionOrigin(region)
            for(x in 0..region.section.toInt() step 4) for(y in 440..616 step 4) if(GameWater.contains(scene,x.toFloat(),y.toFloat())) {
                g.color=Color(35,145,255,95);g.fillRect((offset+x).toInt(),y,4,4)
            }
            for(piece in GameFurniture.pieces(scene)) {
                g.color=Color(255,60,80,150);g.fill(Rectangle2D.Float(offset+piece.left,piece.back,piece.right-piece.left,piece.front-piece.back))
            }
        }
        for(area in GameWalkingMap.areas[region.asset].orEmpty()) {
            g.color=if(area.material==null) Color(255,60,80,200) else Color(180,245,75,160)
            g.fill(polygon(area.points))
        }
        g.clip=oldClip;g.transform=old
        g.color=Color(15,23,24,225);g.fillRect(0,index*320,960,26)
        g.color=Color.WHITE;g.font=Font("SansSerif",Font.PLAIN,12)
        g.drawString("${region.asset} | Gruen: laufen · Blau: schwimmen · Rot: Hindernis · Gelbgruen: Gras",10,index*320+18)
    };g.dispose();save(map,"walking-map-preview.jpg")
}
