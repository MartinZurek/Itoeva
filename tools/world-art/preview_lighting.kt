package com.notime.glyphsim.matrix

import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RadialGradientPaint
import java.awt.RenderingHints
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt
import kotlin.math.sin

/** Dieselbe Kotlin-Licht-/Windrechnung und dieselben Assets; kein Android-Screenshot. */
fun main(args: Array<String>) {
    ImageIO.setUseCache(false)
    fun save(image: BufferedImage, file: File) {
        val bytes = java.io.ByteArrayOutputStream()
        check(ImageIO.write(image, "png", bytes))
        file.writeBytes(bytes.toByteArray())
    }
    val root = File(args[0]); val output = File(args[1]).apply { mkdirs() }
    val assets = (GameWorld.regions.map { it.asset } + GameWorld.seams.map { it.asset } +
        GameInteriorCatalog.scenes.values.map { it.asset }).associateWith {
        ImageIO.read(File(root, "app-sim/src/game/assets/$it")) }
    val sheets = AvatarSpecies.entries.associateWith {
        ImageIO.read(File(root, "app-sim/src/main/assets/creatures/${it.name.lowercase()}.png")) }
    fun color(rgb: Int, alpha: Float) = Color(rgb shr 16 and 255, rgb shr 8 and 255, rgb and 255,
        (alpha.coerceIn(0f, 1f) * 255).roundToInt())
    fun tone(tone: GameSceneLighting.Tone) = when (tone) {
        GameSceneLighting.Tone.SUN -> 0xFFE7B4
        GameSceneLighting.Tone.WARM -> 0xFFB66A
        GameSceneLighting.Tone.COOL -> 0x82CFF1
    }
    fun shape(points: List<Pair<Float, Float>>) = Path2D.Float().apply {
        points.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x.toDouble(), y.toDouble()) else lineTo(x.toDouble(), y.toDouble()) }
        closePath()
    }
    fun render(place: PlayScene.Place, minute: Int, clock: Long, pos: PlayControl.Pos,
        species: AvatarSpecies = AvatarSpecies.FENNEC, old: Boolean = false,
        lamp: Boolean = true, lift: Float = 0f, mirrored: Boolean = false): BufferedImage {
        val scene = GameWorld.scene(place)!!
        val camera = GameCamera.tick(GameCamera.State(), scene, pos, PlayControl.Stick(), 640f, 360f, 0L,
            GameCamera.interest(scene, pos))
        val fit = GameCamera.fit(camera, scene, 640f, 360f)
        val image = BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.translate(fit.left.toDouble(), fit.top.toDouble()); g.scale(fit.scale.toDouble(), fit.scale.toDouble())
        if (GameWorld.isWorld(scene)) {
            val origin = GameWorld.origin(place)
            for (region in GameWorld.regions) g.drawImage(assets.getValue(region.asset),
                (GameWorld.regionOrigin(region) - origin).roundToInt(), 0, region.width.roundToInt(), 640, null)
            for (seam in GameWorld.seams) {
                val patch = assets.getValue(seam.asset)
                for (i in 0 until 96) {
                    val offset = -480f + i * 10f
                    g.composite = AlphaComposite.SrcOver.derive(GameWorld.seamAlpha(offset + 5f))
                    g.drawImage(patch, (seam.x - origin + offset).roundToInt(), 0,
                        (seam.x - origin + offset + 10f).roundToInt(), 640,
                        i * patch.width / 96, 0, (i + 1) * patch.width / 96, patch.height, null)
                }
            }
            g.composite = AlphaComposite.SrcOver
        } else g.drawImage(assets.getValue(scene.asset), 0, 0, 480, 270, null)
        if (!old) {
            for (patch in GameAtmosphere.patches(scene)) {
                val sample = GameAtmosphere.sample(scene, patch) ?: continue
                val source = assets.getValue(sample.asset)
                val origin = sample.offset
                val xs = source.width / sample.width
                val ys = source.height / sample.height
                g.clip = Rectangle2D.Float(patch.x, patch.y, patch.w, patch.h)
                repeat(12) { i ->
                    val y0 = patch.y + patch.h * i / 12f; val y1 = patch.y + patch.h * (i + 1) / 12f
                    val dx = GameAtmosphere.bend(patch, (i + .5f) / 12f, clock, GameWorld.origin(place))
                    g.drawImage(source, (patch.x - 3f + dx).roundToInt(), y0.roundToInt(),
                        (patch.x + patch.w + 3f + dx).roundToInt(), y1.roundToInt(),
                        ((patch.x + origin - 3f) * xs).roundToInt(), (y0 * ys).roundToInt(),
                        ((patch.x + patch.w + origin + 3f) * xs).roundToInt(), (y1 * ys).roundToInt(), null)
                }
            }
            g.clip = null
        }
        if (GameWorld.isWorld(scene)) for (place2 in GameWorld.places) {
            val section = GameWorld.scene(place2)!!
            val start = GameWorld.origin(place2) - GameWorld.origin(place)
            val span = GameWorld.region(place2)!!.section
            repeat(32) { i ->
                val x = (i + .5f) * span / 32f
                val dark = GameSceneLighting.darknessAt(section, x, minute)
                g.color = color(0x0A1030, dark)
                g.fill(Rectangle2D.Float(start + i * span / 32f, 0f, span / 32f + 1f, 640f))
            }
        } else {
            g.color = color(0x0A1030, GameSceneLighting.darkness(scene, minute))
            g.fillRect(0, 0, 480, 270)
        }
        val lights = GameSceneLighting.sources(scene, minute, lamp, false, (clock / 200L).toInt())
        if (!old) {
            GameLightingCatalog.rooms[place]?.window?.let { w ->
                val night = 1f - GameSceneLighting.daylight(minute)
                val cw = (w.right - w.left) / 3f; val ch = (w.bottom - w.top) / 2f
                g.color = color(0x102843, night * .46f)
                repeat(3) { c -> repeat(2) { r -> g.fill(Rectangle2D.Float(
                    w.left + c * cw + 3f, w.top + r * ch + 4f, cw - 6f, ch - 8f)) } }
            }
            for (light in lights.filter { !it.directional }) {
                val center = Point2D.Float(light.x, light.y)
                g.paint = RadialGradientPaint(center, light.radius, floatArrayOf(0f, 1f),
                    arrayOf(color(tone(light.tone), light.power * .24f), color(tone(light.tone), 0f)))
                g.fill(Ellipse2D.Float(light.x - light.radius, light.y - light.radius, light.radius * 2f, light.radius * 2f))
                if (light.kind in setOf(GameSceneLighting.Kind.WINDOW, GameSceneLighting.Kind.FIRE, GameSceneLighting.Kind.CRYSTAL))
                    repeat(9) { i -> val p = GameAtmosphere.dust(light, i, clock)
                        g.color = color(tone(light.tone), p.alpha); g.fill(Ellipse2D.Float(p.x - p.radius, p.y - p.radius, p.radius * 2f, p.radius * 2f)) }
            }
            GameLightingCatalog.rooms[place]?.window?.let { w ->
                val light = lights.firstOrNull { it.kind == GameSceneLighting.Kind.WINDOW }
                if (light != null) {
                    g.color = color(0xFFE7B4, light.power * .06f)
                    g.fill(shape(listOf(w.left to w.bottom, w.right to w.bottom,
                        w.floorX + 68f to w.floorY, w.floorX - 64f to w.floorY)))
                }
            }
        } else {
            for (light in OldGameSceneLighting.sources(scene, minute, lamp, false, (clock / 200L).toInt())) {
                if (GameWorld.isWorld(scene) && light.tone == OldGameSceneLighting.Tone.SUN) continue
                val rgb = when (light.tone) {
                    OldGameSceneLighting.Tone.SUN -> 0xFFEDC6
                    OldGameSceneLighting.Tone.WARM -> 0xFFB96A
                    OldGameSceneLighting.Tone.COOL -> 0x78C8E6
                }
                g.paint = RadialGradientPaint(Point2D.Float(light.x, light.y), light.radius,
                    floatArrayOf(0f, 1f), arrayOf(color(rgb, light.power * if (GameWorld.isWorld(scene)) 1f else .55f), color(rgb, 0f)))
                g.fill(Ellipse2D.Float(light.x - light.radius, light.y - light.radius, light.radius * 2f, light.radius * 2f))
            }
        }
        val (x, floor) = GameScenes.feet(scene, pos)
        val body = GameCharacterScale.visibleHeight(scene, pos, species)
        if (old) {
            if (GameWorld.isWorld(scene)) {
                g.color = color(0x0B1821, .16f); g.fill(Ellipse2D.Float(x - 15f, floor - 2f, 30f, 6f))
            } else {
                val shadow = OldGameSceneLighting.shadow(scene, pos, OldGameSceneLighting.sources(scene, minute, lamp, false, (clock / 200L).toInt()))
                g.color = color(0x0E1720, .60f); g.stroke = BasicStroke(shadow.width)
                g.draw(Line2D.Float(shadow.footX, shadow.footY, shadow.tipX, shadow.tipY))
                g.color = color(0x0B1821, .40f)
                g.fill(Ellipse2D.Float(shadow.footX - shadow.width, shadow.footY - 2f, shadow.width * 2f, 5f))
            }
        }
        else {
            for (shadow in GameSceneLighting.shadows(scene, pos, lights, body, lift)) repeat(4) { pass ->
                val width = shadow.width + shadow.softness * (3 - pass) / 3f
                g.color = color(0x101B25, shadow.opacity * (.10f + pass * .04f))
                if (shadow.tipX == shadow.footX && shadow.tipY == shadow.footY)
                    g.fill(Ellipse2D.Float(shadow.footX - width, shadow.footY - width * .2f, width * 2f, width * .4f))
                else { g.stroke = BasicStroke(width * 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                    g.draw(Line2D.Float(shadow.footX, shadow.footY, shadow.tipX, shadow.tipY)) }
            }
        }
        val drawn = GameCharacterScale.layoutWidth(scene, pos, species) * CreatureSprites.Rich.scaleFor(species)
        val ref = GameCharacterScale.reference(species)
        val raw = sheets.getValue(species).getSubimage(0, 0, 128, 128)
        val sprite = BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB)
        val lighting = GameSceneLighting.character(scene, pos, species, minute, lights, lift)
        val oldLights = OldGameSceneLighting.sources(scene, minute, lamp, false, (clock / 200L).toInt())
        val oldBrightness = (1f - OldGameSceneLighting.darkness(scene, minute) * .45f +
            OldGameSceneLighting.illuminationAt(x, floor, oldLights)).coerceIn(.55f, 1f)
        for (sy in 0 until 128) for (sx in 0 until 128) {
            val pixel = raw.getRGB(if (mirrored) 127 - sx else sx, sy)
            val rgb = if (old) GameSceneLighting.Rgb(oldBrightness, oldBrightness, oldBrightness)
                else lighting.at((sx + .5f) / 128f, ((sy.toFloat() - ref.top) / (126 - ref.top)).coerceIn(0f, 1f))
            val r = ((pixel shr 16 and 255) * rgb.r).roundToInt().coerceIn(0, 255)
            val green = ((pixel shr 8 and 255) * rgb.g).roundToInt().coerceIn(0, 255)
            val b = ((pixel and 255) * rgb.b).roundToInt().coerceIn(0, 255)
            sprite.setRGB(sx, sy, (pixel and -0x1000000) or (r shl 16) or (green shl 8) or b)
        }
        val area = Area(Rectangle2D.Float(-16000f, -1000f, 32000f, 2640f))
        for (piece in GameFurniture.pieces(scene).filter { it.hides(floor, null) })
            for (contour in piece.contours) area.subtract(Area(shape(contour)))
        g.clip = area
        val wet = GameCharacterScale.waterRise(scene, pos)
        if (lift == 0f && wet > 0f) g.clip(Rectangle2D.Float(-16000f, -1000f, 32000f, floor - wet + 1000f))
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        val wind = if (old) 0f else GameAtmosphere.figureBend(scene, species, 0f, clock,
            GameWorld.origin(place) + x, PlayWeather.CLEAR)
        repeat(6) { row ->
            val y0 = row * 128 / 6; val y1 = (row + 1) * 128 / 6
            val v = (row + .5f) / 6f; val tips = ((.55f - v) / .55f).coerceIn(0f, 1f)
            val dx = wind * drawn * tips * tips
            g.drawImage(sprite, (x - drawn / 2f + dx).roundToInt(),
                (floor - lift - drawn * 126f / 128f + drawn * y0 / 128f).roundToInt(),
                (x + drawn / 2f + dx).roundToInt(),
                (floor - lift - drawn * 126f / 128f + drawn * y1 / 128f).roundToInt(),
                0, y0, 128, y1, null)
        }
        g.clip = null
        if (!old && scene.asset == "world/coast.png") repeat(24) { i ->
            val span = GameWorld.region(place)!!.section; val xx = (i + .5f) * span / 24f
            val yy = GameWorld.shoreY(GameWorld.origin(place) + xx) + 12f + i % 4 * 22f
            if (GameWorld.material(scene, xx, yy) == GameEnvironment.Material.WATER) {
                val wave = sin(clock / 1600f + (GameWorld.origin(place) + xx) / 45f)
                g.color = color(0xE0EADB, (.025f + GameSceneLighting.daylight(minute) * .12f) * (.3f + .7f * kotlin.math.abs(wave)))
                g.stroke = BasicStroke(1.1f); g.draw(Line2D.Float(xx - 8f + wave * 2f, yy, xx + 8f + wave * 2f, yy + sin(wave + 1f)))
            }
        }
        g.dispose()
        val label = image.createGraphics(); label.color = Color(20, 32, 36, 225)
        label.fillRoundRect(10, 10, 620, 32, 8, 8); label.color = Color(246, 231, 203)
        label.font = Font("SansSerif", Font.PLAIN, 16)
        label.drawString("${if (old) "Vorher" else "Neu"} | $place | %02d:%02d | $species".format(minute / 60, minute % 60), 20, 32)
        label.dispose(); return image
    }
    val examples = listOf(Triple(PlayScene.Place.LIVING, 1380, PlayControl.Pos(.33f, .70f)),
        Triple(PlayScene.Place.CAMP, 1380, PlayControl.Pos(.28f, .40f)),
        Triple(PlayScene.Place.GROTTO, 1380, PlayControl.Pos(.63f, .36f)),
        Triple(PlayScene.Place.FOREST, 480, PlayControl.Pos(.60f, .60f)))
    val comparison = BufferedImage(1280, 1440, BufferedImage.TYPE_INT_RGB); val cg = comparison.createGraphics()
    for ((i, item) in examples.withIndex()) {
        cg.drawImage(render(item.first, item.second, 4500L, item.third, old = true), 0, i * 360, null)
        cg.drawImage(render(item.first, item.second, 4500L, item.third), 640, i * 360, null)
    }
    cg.dispose(); save(comparison, File(output, "lighting-comparison.png"))
    val family = BufferedImage(1920, 720, BufferedImage.TYPE_INT_RGB); val fg = family.createGraphics()
    for ((i, species) in AvatarSpecies.entries.withIndex()) fg.drawImage(
        render(PlayScene.Place.CAMP, 1380, 4500L, PlayControl.Pos(.25f, .48f), species), i % 3 * 640, i / 3 * 360, null)
    fg.dispose(); save(family, File(output, "lighting-family.png"))
    repeat(60) { n ->
        val video = BufferedImage(1920, 360, BufferedImage.TYPE_INT_RGB); val vg = video.createGraphics()
        vg.drawImage(render(PlayScene.Place.PARK, 450 + n * 12, n * 160L, PlayControl.Pos(.50f, .55f)), 0, 0, null)
        vg.drawImage(render(PlayScene.Place.LIVING, 1380, n * 160L, PlayControl.Pos(.33f, .7f), lamp = n >= 30, mirrored = n >= 30), 640, 0, null)
        vg.drawImage(render(PlayScene.Place.BEACH, 720, n * 160L, PlayControl.Pos(.50f, .62f)), 1280, 0, null)
        vg.dispose(); save(video, File(output, "motion-%03d.png".format(n)))
    }
    println("Lichtvergleich, alle sechs Wesen und 60 Tageszeit-/Lampen-/Wasserbilder gerendert.")
}
