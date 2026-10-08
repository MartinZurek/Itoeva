package com.notime.glyphsim.matrix

import java.awt.*
import java.awt.geom.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt

/** Desktop-QA aus den produktiven Modellen; keine Aufnahme einer Android-App. */
fun main(args: Array<String>) {
    ImageIO.setUseCache(false)
    val root = File(args[0]); val out = File(args[1]).apply { mkdirs() }
    fun save(image: BufferedImage, name: String) {
        val bytes = java.io.ByteArrayOutputStream(); check(ImageIO.write(image, "png", bytes))
        File(out, name).writeBytes(bytes.toByteArray())
    }
    val paths = (GameWorld.regions.map { it.asset } + GameWorld.seams.map { it.asset } +
        GameInteriorCatalog.scenes.values.map { it.asset }).distinct()
    val originals = paths.associateWith { ImageIO.read(File(root, "app-sim/src/game/assets/$it")) }
    val started = System.nanoTime()
    val corrected = originals.mapValues { (asset, source) ->
        val field = GameGroundLight.estimate(asset) { u,v -> source.getRGB(
            (u*source.width).toInt().coerceAtMost(source.width-1), (v*source.height).toInt().coerceAtMost(source.height-1)) }
        val image = BufferedImage(source.width,source.height,BufferedImage.TYPE_INT_ARGB)
        val g=image.createGraphics();g.drawImage(source,0,0,null);g.dispose()
        for (y in 0 until image.height) {
            val v=(y+.5f)/image.height
            if (v < .71f || v > .99f) continue
            for (x in 0 until image.width) {
                val u=(x+.5f)/image.width; val gain=field.gain(u,v)
                if (kotlin.math.abs(gain-1f)>.002f && GameGroundLight.floor(asset,u,v))
                    image.setRGB(x,y,GameGroundLight.apply(source.getRGB(x,y),gain))
            }
        }
        image
    }
    println("Desktop-Bodenkorrektur fuer ${corrected.size} Assets: ${(System.nanoTime()-started)/1_000_000} ms (keine Android-Messung)")
    val sheets = AvatarSpecies.entries.associateWith { ImageIO.read(File(root,
        "app-sim/src/main/assets/creatures/${it.name.lowercase()}.png")) }
    fun color(rgb: Int, a: Float)=Color(rgb shr 16 and 255,rgb shr 8 and 255,rgb and 255,(a.coerceIn(0f,1f)*255).roundToInt())
    fun shape(points: List<Pair<Float,Float>>)=Path2D.Float().apply {
        points.forEachIndexed { i,p -> if(i==0) moveTo(p.first.toDouble(),p.second.toDouble()) else lineTo(p.first.toDouble(),p.second.toDouble()) }; closePath() }
    fun render(place: PlayScene.Place, species: AvatarSpecies, minute: Int, clock: Long,
        seat: GameSeating.State? = null, basePos: PlayControl.Pos = PlayControl.Pos(.5f,.70f),
        neutral: Boolean = true, contours: Boolean = false, label: String = ""): BufferedImage {
        val scene=GameWorld.scene(place)!!
        val pose=seat?.let { GameSeating.frame(scene,species,it,clock) }
        val pos=pose?.pos ?: basePos;val lift=pose?.lift ?: 0f
        val camera=GameCamera.tick(GameCamera.State(),scene,pos,PlayControl.Stick(),640f,360f,0,
            GameCamera.interest(scene,pos))
        val fit=GameCamera.fit(camera,scene,640f,360f)
        val image=BufferedImage(640,360,BufferedImage.TYPE_INT_RGB);val g=image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.translate(fit.left.toDouble(),fit.top.toDouble());g.scale(fit.scale.toDouble(),fit.scale.toDouble())
        val assets=if(neutral) corrected else originals
        val origin=GameWorld.origin(place)
        if(GameWorld.isWorld(scene)) {
            for(r in GameWorld.regions) g.drawImage(assets.getValue(r.asset),
                (GameWorld.regionOrigin(r)-origin).roundToInt(),0,r.width.roundToInt(),640,null)
            for(s in GameWorld.seams) repeat(96) { i ->
                val offset=-480f+i*10f;val patch=assets.getValue(s.asset)
                g.composite=AlphaComposite.SrcOver.derive(GameWorld.seamAlpha(offset+5f))
                g.drawImage(patch,(s.x-origin+offset).roundToInt(),0,(s.x-origin+offset+10f).roundToInt(),640,
                    i*patch.width/96,0,(i+1)*patch.width/96,patch.height,null)
            };g.composite=AlphaComposite.SrcOver
        } else g.drawImage(assets.getValue(scene.asset),0,0,480,270,null)
        for(patch in GameAtmosphere.patches(scene)) {
            val sample=GameAtmosphere.sample(scene,patch) ?: continue
            val source=assets.getValue(sample.asset);val xs=source.width/sample.width;val ys=source.height/sample.height
            g.clip=Rectangle2D.Float(patch.x,patch.y,patch.w,patch.h)
            repeat(12) { i ->
                val y0=patch.y+patch.h*i/12f;val y1=patch.y+patch.h*(i+1)/12f
                val dx=if(patch.hanging) GameFabric.hangingOffset((i+.5f)/12f,.5f,
                    GameAtmosphere.wind(origin+patch.x,clock)*.65f,clock,patch.seed)
                    else GameAtmosphere.bend(patch,(i+.5f)/12f,clock,origin)
                g.drawImage(source,(patch.x-3f+dx).roundToInt(),y0.roundToInt(),(patch.x+patch.w+3f+dx).roundToInt(),y1.roundToInt(),
                    ((patch.x+sample.offset-3f)*xs).roundToInt().coerceIn(0,source.width-1),(y0*ys).roundToInt(),
                    ((patch.x+sample.offset+patch.w+3f)*xs).roundToInt().coerceIn(1,source.width),(y1*ys).roundToInt(),null)
            };g.clip=null
        }
        for(sectionPlace in if(GameWorld.isWorld(scene)) GameWorld.places else listOf(place)) {
            val section=GameWorld.scene(sectionPlace)!!
            val span=if(GameWorld.isWorld(scene)) GameWorld.region(sectionPlace)!!.section else 480f
            val start=GameWorld.origin(sectionPlace)-origin
            repeat(32) { i ->
                val x=(i+.5f)*span/32f
                g.color=color(0x0A1030,GameSceneLighting.darknessAt(section,x,minute));g.fill(Rectangle2D.Float(start+i*span/32f,0f,span/32f+1,640f))
            }
        }
        val lights=GameSceneLighting.sources(scene,minute,true,false,(clock/200).toInt())
        if(GameWorld.isWorld(scene) && neutral) lights.firstOrNull { it.directional }?.let { sun ->
            val absolute=sun.copy(x=sun.x+origin,fadeStart=sun.fadeStart+origin)
            for(c in GameWorldShadows.casters) {
                val cast=GameWorldShadows.project(c,absolute,clock)
                g.color=color(0x17252A,cast.alpha);g.fill(shape(cast.points.map { it.first-origin to it.second }))
            }
        }
        for(light in lights.filter { !it.directional }) {
            val rgb=when(light.tone) { GameSceneLighting.Tone.WARM->0xFFB66A;GameSceneLighting.Tone.COOL->0x82CFF1;else->0xFFE7B4 }
            g.paint=RadialGradientPaint(Point2D.Float(light.x,light.y),light.radius,floatArrayOf(0f,1f),arrayOf(color(rgb,light.power*.24f),color(rgb,0f)))
            g.fill(Ellipse2D.Float(light.x-light.radius,light.y-light.radius,light.radius*2,light.radius*2))
        }
        val (x,floor)=GameScenes.feet(scene,pos);val body=GameCharacterScale.visibleHeight(scene,pos,species)
        for(shadow in GameSceneLighting.shadows(scene,pos,lights,body,lift,pose?.receiver ?: 0f)) {
            g.color=color(0x101B25,shadow.opacity*.3f);g.stroke=BasicStroke(shadow.width*2,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND)
            g.draw(Line2D.Float(shadow.footX,shadow.footY,shadow.tipX,shadow.tipY))
        }
        val held=seat!=null && pose?.motion==null
        val motion=pose?.motion ?: if(held) CreatureSprites.MotionCue(CreatureSprites.Motion.SIT,1f) else null
        val frame=motion?.let { CreatureSprites.motionFrame(it,CreatureSprites.Facing.FRONT) } ?: 0
        val sheet=sheets.getValue(species);val cols=sheet.width/128
        val raw=sheet.getSubimage(frame%cols*128,frame/cols*128,128,128)
        val sprite=BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB)
        val light=GameSceneLighting.character(scene,pos,species,minute,lights,lift)
        val ref=GameCharacterScale.reference(species)
        val fabric=GameFabric.Pose(species,if(GameWorld.isWorld(scene)) GameAtmosphere.wind(origin+x,clock) else .12f,clock,0f)
        val wind=GameAtmosphere.figureBend(scene,species,0f,clock,origin+x,PlayWeather.CLEAR)
        for(py in 0 until 128) for(px in 0 until 128) {
            val u=(px/11+ .5f)/12f;val v=(py/11+.5f)/12f
            val tips=((.55f-v)/.55f).coerceIn(0f,1f)
            val shift=((GameFabric.offset(fabric,u,v)+wind*tips*tips)*128).roundToInt()
            val pixel=raw.getRGB((px-shift).coerceIn(0,127),py)
            val rgb=light.at((px+.5f)/128,((py-ref.top).toFloat()/(126-ref.top)).coerceIn(0f,1f))
            fun channel(s: Int,f: Float)=((pixel shr s and 255)*f).roundToInt().coerceIn(0,255)
            sprite.setRGB(px,py,(pixel and -0x1000000) or (channel(16,rgb.r) shl 16) or (channel(8,rgb.g) shl 8) or channel(0,rgb.b))
        }
        val area=Area(Rectangle2D.Float(-16000f,-1000f,32000f,2640f))
        for(piece in GameFurniture.pieces(scene).filter { it.hides(floor,seat?.piece?.id) })
            for(contour in piece.contours) area.subtract(Area(shape(contour)))
        g.clip=area
        val drawn=GameCharacterScale.layoutWidth(scene,pos,species)*CreatureSprites.Rich.scaleFor(species)
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        g.drawImage(sprite,(x-drawn/2).roundToInt(),(floor-lift-drawn*126/128).roundToInt(),
            (x+drawn/2).roundToInt(),(floor-lift+drawn*2/128).roundToInt(),0,0,128,128,null);g.clip=null
        if(contours) {
            g.stroke=BasicStroke(1.2f);g.color=Color(80,255,180,200)
            for(piece in GameFurniture.pieces(scene)) for(contour in piece.contours) g.draw(shape(contour))
            g.color=Color.YELLOW
            for(spot in scene.spots) g.fill(Ellipse2D.Float(spot.standX-2,spot.standY-2,4f,4f))
        }
        g.dispose();val text=image.createGraphics();text.color=Color(20,32,36,225);text.fillRect(0,0,640,30)
        text.color=Color.WHITE;text.font=Font("SansSerif",Font.PLAIN,14);text.drawString("$label | $place | $species | ${minute/60}:${minute%60}",10,20);text.dispose()
        return image
    }
    val furniture=BufferedImage(1920,1440,BufferedImage.TYPE_INT_RGB);val fg=furniture.createGraphics()
    for((i,place) in GameInteriorCatalog.scenes.keys.withIndex()) fg.drawImage(render(place,AvatarSpecies.FENNEC,720,1000,contours=true,label="Konturen / Aktionsanker"),i%3*640,i/3*360,null)
    fg.dispose();save(furniture,"polish-furniture.png")
    val family=BufferedImage(1920,720,BufferedImage.TYPE_INT_RGB);val sg=family.createGraphics()
    val living=GameWorld.scene(PlayScene.Place.LIVING)!!
    val from=GameSeating.approach(living,PlayScene.Station.SEAT)!!
    val seat=GameSeating.begin(living,PlayScene.Station.SEAT,from,0)!!
    for((i,species) in AvatarSpecies.entries.withIndex()) sg.drawImage(render(living.place,species,720,1200,seat=seat,label="Sitzflaeche / alle sechs Wesen"),i%3*640,i/3*360,null)
    sg.dispose();save(family,"polish-seating.png")
    val compare=BufferedImage(1280,720,BufferedImage.TYPE_INT_RGB);val cg=compare.createGraphics()
    for((i,minute) in listOf(480,1080).withIndex()) {
        val pos=PlayControl.Pos(.8f,.47f)
        cg.drawImage(render(PlayScene.Place.PARK,AvatarSpecies.FENNEC,minute,3000,basePos=pos,neutral=false,label="Gemalter Boden / ohne neue Kulissenschatten"),0,i*360,null)
        cg.drawImage(render(PlayScene.Place.PARK,AvatarSpecies.FENNEC,minute,3000,basePos=pos,label="Bodenkorrektur + Sonnenschatten"),640,i*360,null)
    };cg.dispose();save(compare,"polish-shadow-comparison.png")
    repeat(64) { n ->
        val clock=n*100L;val state=if(clock>=3900) seat.copy(rise=3900) else seat
        val video=BufferedImage(1920,360,BufferedImage.TYPE_INT_RGB);val vg=video.createGraphics()
        vg.drawImage(render(living.place,AvatarSpecies.FENNEC,720,clock,seat=state.takeIf { clock<4300 },basePos=from,label="Hinsetzen / Stoff / Aufstehen"),0,0,null)
        vg.drawImage(render(PlayScene.Place.PARK,AvatarSpecies.STARLET,420+n*12,clock,basePos=PlayControl.Pos(.8f,.47f),label="Sonne / Baumschatten / Schal"),640,0,null)
        val cafe=GameWorld.scene(PlayScene.Place.CAFE)!!;val approach=GameSeating.approach(cafe,PlayScene.Station.TABLE)!!
        vg.drawImage(render(cafe.place,AvatarSpecies.GLOOP,900,clock,seat=GameSeating.begin(cafe,PlayScene.Station.TABLE,approach,0),label="Sitzhoehe / Vorhang"),1280,0,null)
        vg.dispose();save(video,"polish-motion-%03d.png".format(n))
    }
    println("Moebel, Sitzfamilie, Schattenvergleich und 64 Bewegungsbilder fertig.")
}
