package com.notime.glyphsim.matrix

import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/** Sichtpruefung aus den echten Kotlin-Massen und gelieferten Assets, keine Android-Aufnahme. */
fun main(args: Array<String>) {
    val root = File(args[0]); val output = File(args[1]).apply { mkdirs() }
    val assets = (GameWorld.regions.map { it.asset } + GameWorld.seams.map { it.asset } +
        GameInteriorCatalog.scenes.values.map { it.asset }).associateWith {
        ImageIO.read(File(root,"app-sim/src/game/assets/$it")) }
    val sheets = AvatarSpecies.entries.associateWith {
        ImageIO.read(File(root,"app-sim/src/main/assets/creatures/${it.name.lowercase()}.png")) }

    fun render(place: PlayScene.Place, pos: PlayControl.Pos, old: Boolean = false,
        species: AvatarSpecies = AvatarSpecies.FENNEC, movement: GameMovement.State = GameMovement.State(pos),
        frame: Int = 0): BufferedImage {
        val scene = GameWorld.scene(place)!!
        val camera = GameCamera.tick(GameCamera.State(),scene,pos,PlayControl.Stick(),640f,360f,0L,
            GameCamera.interest(scene,pos))
        val fit = GameCamera.fit(camera,scene,640f,360f)
        val image = BufferedImage(640,360,BufferedImage.TYPE_INT_RGB); val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        if (GameWorld.isWorld(scene)) {
            val left = fit.left - GameWorld.origin(place)*fit.scale
            for (region in GameWorld.regions) g.drawImage(assets.getValue(region.asset),
                (left+GameWorld.regionOrigin(region)*fit.scale).roundToInt(),fit.top.roundToInt(),
                (region.width*fit.scale).roundToInt(),(640f*fit.scale).roundToInt(),null)
        } else g.drawImage(assets.getValue(scene.asset),fit.left.roundToInt(),fit.top.roundToInt(),
            (480*fit.scale).roundToInt(),(270*fit.scale).roundToInt(),null)
        val (fx,fy) = GameScenes.feet(scene,pos); val (x,y) = fit.toScreen(fx,fy)
        val oldHeight = if (scene.asset.startsWith("interiors/")) 40f + 36f * pos.depth
            else GameScenes.avatarHeight(scene,pos)
        val width = (if (old) oldHeight/.8f
            else GameCharacterScale.layoutWidth(scene,pos,species))*fit.scale
        val drawn = width*CreatureSprites.Rich.scaleFor(species)
        val source = sheets.getValue(species).getSubimage(frame*128,0,128,128)
        if (!old) {
            val area = Area(Rectangle2D.Float(-1000f,-1000f,2640f,2360f))
            for (piece in GameFurniture.pieces(scene).filter { it.hides(fy,movement.support) })
                for (points in piece.contours) {
                    val path = Path2D.Float()
                    points.forEachIndexed { i,(px,py) ->
                        val (sx,sy)=fit.toScreen(px,py)
                        if(i==0) path.moveTo(sx.toDouble(),sy.toDouble()) else path.lineTo(sx.toDouble(),sy.toDouble())
                    }
                    path.closePath(); area.subtract(Area(path))
                }
            g.clip = area
        }
        val waterY = y-GameCharacterScale.waterRise(scene,pos)*fit.scale
        if(movement.height==0f) g.clipRect(-1000,-1000,2640,(waterY+1000f).roundToInt())
        g.drawImage(source,(x-drawn/2f).roundToInt(),
            (y-movement.height*fit.scale-drawn*126f/128f).roundToInt(),drawn.roundToInt(),drawn.roundToInt(),null)
        g.clip = null
        g.color=Color(20,35,29,225);g.fillRoundRect(10,10,620,32,8,8)
        g.font=Font("SansSerif",Font.PLAIN,16);g.color=Color(244,234,211)
        val visible = if(old) oldHeight/.8f*CreatureSprites.Rich.scaleFor(species)*
            (126f-GameCharacterScale.reference(species).top)/128f else GameCharacterScale.visibleHeight(scene,pos,species)
        g.drawString("${if(old) "Vorher" else "Neu"} | $place | $species | %.1f Weltpixel".format(visible),20,32)
        g.dispose();return image
    }

    val pairs = listOf(PlayScene.Place.PARK,PlayScene.Place.LIVING,PlayScene.Place.KITCHEN,PlayScene.Place.CAFE)
    val comparison=BufferedImage(1280,1440,BufferedImage.TYPE_INT_RGB);val cg=comparison.createGraphics()
    for ((i,place) in pairs.withIndex()) {
        val scene=GameWorld.scene(place)!!;val spot=scene.spots.first()
        val pos=GameScenes.posAt(scene,spot.standX,spot.standY)
        cg.drawImage(render(place,pos,true),0,i*360,null);cg.drawImage(render(place,pos),640,i*360,null)
    }
    cg.dispose();ImageIO.write(comparison,"png",File(output,"character-scale-comparison.png"))

    val family=BufferedImage(1920,720,BufferedImage.TYPE_INT_RGB);val fg=family.createGraphics()
    for ((i,species) in AvatarSpecies.entries.withIndex()) fg.drawImage(
        render(PlayScene.Place.PARK,PlayControl.Pos(.56f,.434f),species=species),i%3*640,i/3*360,null)
    fg.dispose();ImageIO.write(family,"png",File(output,"character-scale-family.png"))

    val scene=GameWorld.scene(PlayScene.Place.PARK)!!
    val piece=GameFurniture.pieces(scene).single();val surface=piece.surface(scene)
    val examples=BufferedImage(1920,360,BufferedImage.TYPE_INT_RGB);val eg=examples.createGraphics()
    for ((i,y) in listOf(475f,526f,501f).withIndex()) {
        val pos=GameScenes.posAt(scene,267f,y)
        val state=if(i==2) GameMovement.State(pos,height=surface.heightAt(pos),support=piece.id) else GameMovement.State(pos)
        eg.drawImage(render(PlayScene.Place.PARK,pos,movement=state),i*640,0,null)
    }
    eg.dispose();ImageIO.write(examples,"png",File(output,"character-scale-contact.png"))

    for ((i,place) in listOf(PlayScene.Place.KITCHEN,PlayScene.Place.CAFE).withIndex()) {
        val room=GameWorld.scene(place)!!;val table=GameSurfaces.painted(room).single()
        val pos=PlayControl.Pos((table.x0+table.x1)/2f,table.d1+.015f)
        var state=GameMovement.command(GameMovement.State(pos),GameMovement.Command.JUMP,PlayControl.Stick(0f,-.5f),listOf(table),GameCharacterScale.MAX_STEP_HEIGHT)
        repeat(14) { n ->
            state=GameMovement.tick(state,PlayControl.Stick(),50L,listOf(table),maxStepHeight=GameCharacterScale.MAX_STEP_HEIGHT).state
            val frame=if(state.action==GameMovement.Action.JUMP) CreatureSprites.motionFrame(
                CreatureSprites.MotionCue(CreatureSprites.Motion.JUMP,state.progress),CreatureSprites.Facing.BACK) else 0
            ImageIO.write(render(place,state.pos,movement=state,frame=frame),"png",File(output,"jump-${i}-%02d.png".format(n)))
        }
    }
    println("Vorher/nachher, alle sechs Wesen, Bankkontakt und beide Tischlandungen gerendert.")
}
