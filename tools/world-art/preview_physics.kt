package com.notime.glyphsim.matrix

import java.awt.*
import java.awt.geom.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import kotlin.math.cos
import kotlin.math.sin

/** Bildpruefung der produktiven Anker/Posen. Kein Android-Screenshot. */
fun main(args: Array<String>) {
    ImageIO.setUseCache(false)
    val root=File(args[0]);val out=File(args[1]).apply { mkdirs() }
    fun save(image: BufferedImage,name: String) {
        val bytes=java.io.ByteArrayOutputStream();ImageIO.write(image,"png",bytes);File(out,name).writeBytes(bytes.toByteArray())
    }
    val assets=(GameWorld.regions.map { it.asset }+GameWorld.seams.map { it.asset }+GameInteriorCatalog.scenes.values.map { it.asset })
        .associateWith { ImageIO.read(File(root,"app-sim/src/game/assets/$it")) }
    for((path,image) in assets) GameWater.measure(path) { u,v -> image.getRGB((u*image.width).toInt(),(v*image.height).toInt()) }
    println("Wasserstreifen: "+listOf("world/coast.png","world/coast-path.png").associateWith { GameWater.runs(it).size })
    val sheets=AvatarSpecies.entries.associateWith { ImageIO.read(File(root,"app-sim/src/main/assets/creatures/${it.name.lowercase()}.png")) }
    fun render(species: AvatarSpecies,clock: Long,doorPhase: Boolean=false): BufferedImage {
        val place=if(doorPhase) PlayScene.Place.LIVING else PlayScene.Place.BEACH
        val scene=GameWorld.scene(place)!!;val pos=if(doorPhase) PlayControl.Pos(.5f,.5f) else PlayControl.Pos(.5f,.86f)
        val camera=GameCamera.tick(GameCamera.State(),scene,pos,PlayControl.Stick(),640f,360f,0)
        val fit=GameCamera.fit(camera,scene,640f,360f)
        val target=BufferedImage(640,360,BufferedImage.TYPE_INT_RGB);val g=target.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.translate(fit.left.toDouble(),fit.top.toDouble());g.scale(fit.scale.toDouble(),fit.scale.toDouble())
        val asset=assets.getValue(scene.asset);val origin=GameWorld.origin(place)
        if(GameWorld.isWorld(scene)) g.drawImage(asset,-origin.roundToInt(),0,1920,640,null)
        else g.drawImage(asset,0,0,480,270,null)
        if(!doorPhase) for(run in GameWater.runs(scene.asset)) {
            val x=run.x0-origin;val end=run.x1-origin
            if(end<0f || x>480f) continue
            for(tile in 0 until kotlin.math.ceil((end-x)/64f).toInt()) {
                val a=x+tile*64f;val b=minOf(end,a+64f);val dx=GameWater.displacement(origin+(a+b)/2,run.y.toFloat(),clock)
                val sx=asset.width/1920f;val sy=asset.height/640f
                g.drawImage(asset,a.roundToInt(),run.y,b.roundToInt(),run.y+4,
                    ((origin+a-dx)*sx).roundToInt().coerceIn(0,asset.width-1),(run.y*sy).roundToInt(),
                    ((origin+b-dx)*sx).roundToInt().coerceIn(1,asset.width),((run.y+4)*sy).roundToInt().coerceAtMost(asset.height),null)
            }
        }
        val (fx,fy)=GameScenes.feet(scene,pos)
        val width=GameCharacterScale.layoutWidth(scene,pos,species)
        val drawn=width*CreatureSprites.Rich.scaleFor(species)
        val swim=GameWater.swim(scene,pos,0f,PlayControl.Dir.RIGHT,true,clock,species)
        val frame=CreatureSprites.lookRich(AvatarAnimations.idlePose(species),species,AvatarShading.Side.NONE,
            clock,PlayControl.Dir.RIGHT,false,CreatureSprites.Turn(),0,swim?.cue).frame
        val sheet=sheets.getValue(species)
        val old=g.transform;val clip=g.clip
        if(swim!=null) {
            g.clip=Rectangle2D.Float(-500f,-500f,1500f,fy-GameCharacterScale.waterRise(scene,pos)+500f)
            g.translate(0.0,(swim.bob-swim.buoyancy)*drawn/128.0);g.rotate(Math.toRadians(swim.angle.toDouble()),fx.toDouble(),fy-drawn*.4)
        }
        val top=fy-drawn*CreatureSprites.Rich.FEET/128f;val left=fx-drawn/2f
        g.drawImage(sheet,left.roundToInt(),top.roundToInt(),(left+drawn).roundToInt(),(top+drawn).roundToInt(),
            frame*128,0,(frame+1)*128,128,null)
        g.transform=old;g.clip=clip
        if(swim!=null) {
            val y=fy-GameCharacterScale.waterRise(scene,pos);val phase=(clock%900L)/900f
            g.color=Color(222,244,235,((1-phase)*160).toInt());g.stroke=BasicStroke(1f)
            val spread=17f+phase*14f;g.draw(Ellipse2D.Float(fx-spread,y-3f,spread*2f,5f+phase*5f))
            repeat(2) { hand ->
                val p=(swim.stroke+hand*.5f)%1f;val side=if(hand==0) -1f else 1f
                g.color=Color(231,244,232,((1-p)*180).toInt())
                val hx=fx+side*(12f+p*18f);val hy=y+sin(p*6.283f)*3f
                g.draw(Line2D.Float(hx,hy,hx-side*9f,hy+2f))
            }
        }
        if(doorPhase) for(p in GameWorld.passages(place).filter { it.door }) {
            val box=GameDoors.aperture(p.from,p.to);val opening=GameDoors.opening(GameDoors.State(p,0),p,clock)
            g.color=Color(23,32,28);g.fill(Rectangle2D.Float(box.x0,box.y0,box.x1-box.x0,box.y1-box.y0))
            val w=(box.x1-box.x0)*cos(opening*1.48f)
            g.drawImage(asset,box.x0.roundToInt(),box.y0.roundToInt(),(box.x0+w).roundToInt(),box.y1.roundToInt(),
                (box.x0*asset.width/480).roundToInt(),(box.y0*asset.height/270).roundToInt(),
                (box.x1*asset.width/480).roundToInt(),(box.y1*asset.height/270).roundToInt(),null)
        }
        g.dispose();val label=target.createGraphics();label.color=Color(16,25,27);label.fillRect(0,0,640,24)
        label.color=Color.WHITE;label.drawString(if(doorPhase) "Tueren ${clock}ms (Modellvorschau)" else "${species.name} · Schwimmen ${clock}ms (Modellvorschau)",10,17);label.dispose()
        return target
    }
    val overview=BufferedImage(1920,720,BufferedImage.TYPE_INT_RGB);val g=overview.createGraphics()
    AvatarSpecies.entries.forEachIndexed { i,s -> g.drawImage(render(s,225),(i%3)*640,(i/3)*360,null) };g.dispose();save(overview,"physics-swimming.png")
    repeat(36) { frame ->
        val img=BufferedImage(1280,360,BufferedImage.TYPE_INT_RGB);val gg=img.createGraphics()
        gg.drawImage(render(AvatarSpecies.FENNEC,frame*100L),0,0,null)
        gg.drawImage(render(AvatarSpecies.FENNEC,((frame%9)*52L),true),640,0,null)
        gg.dispose();save(img,"physics-motion-%03d.png".format(frame))
    }
    val map=BufferedImage(960,320*GameWorld.regions.size,BufferedImage.TYPE_INT_RGB);val mg=map.createGraphics()
    for((i,region) in GameWorld.regions.withIndex()) {
        mg.drawImage(assets.getValue(region.asset),0,i*320,960,320,null)
        for(place in region.places) {
            val scene=GameWorld.scene(place)!!;val origin=GameWorld.origin(place)-GameWorld.regionOrigin(region)
            val path=Path2D.Float()
            for(x in 0..40) {
                val band=GameTerrain.band(scene,x/40f);val px=(origin+x/40f*region.section)/2f
                if(x==0) path.moveTo(px.toDouble(),i*320+band.first/2.0) else path.lineTo(px.toDouble(),i*320+band.first/2.0)
            }
            for(x in 40 downTo 0) { val band=GameTerrain.band(scene,x/40f);path.lineTo((origin+x/40f*region.section)/2.0,i*320+band.second/2.0) };path.closePath()
            mg.color=Color(65,245,156,55);mg.fill(path);mg.color=Color(65,245,156,190);mg.draw(path)
        }
        mg.color=Color.WHITE;mg.drawString(region.asset,8,i*320+18)
    };mg.dispose();save(map,"physics-boundaries.png")
}
