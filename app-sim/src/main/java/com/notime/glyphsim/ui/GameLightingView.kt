package com.notime.glyphsim.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import com.notime.glyphsim.matrix.*
import kotlin.math.hypot

internal fun lightColor(tone: GameSceneLighting.Tone) = when (tone) {
    GameSceneLighting.Tone.SUN -> Color(0xFFFFE7B4)
    GameSceneLighting.Tone.WARM -> Color(0xFFFFB66A)
    GameSceneLighting.Tone.COOL -> Color(0xFF82CFF1)
}

/** Weiche Saeume brauchen keinen Bitmap-Blur und behalten ihren Weltanker beim Kameraschwenk. */
internal fun DrawScope.paintShadow(shadow: GameSceneLighting.Shadow, alpha: Float) {
    val length = hypot(shadow.tipX - shadow.footX, shadow.tipY - shadow.footY)
    val point = Offset(shadow.footX, shadow.footY)
    repeat(4) { pass ->
        val feather = shadow.softness * (3 - pass) / 3f
        val width = shadow.width + feather
        val color = Color(0xFF101B25).copy(alpha = shadow.opacity * alpha * (.10f + pass * .04f))
        if (length < .01f) drawOval(color, point - Offset(width, width * .20f), Size(width * 2f, width * .40f))
        else drawLine(color, point, Offset(shadow.tipX, shadow.tipY), width * 2f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

private fun furniturePath(scene: GameScenes.Scene): Path = Path().apply {
    for (piece in GameFurniture.pieces(scene)) for (contour in piece.contours) {
        contour.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x, y) else lineTo(x, y) }
        close()
    }
}

/** Fensterstrahlen enden am Boden; Lichtstaub wird aus genau denselben Quellen gerechnet. */
internal fun DrawScope.paintSceneLights(scene: GameScenes.Scene, lights: List<GameSceneLighting.Light>,
    fit: GameScenes.Fit, clock: Long, alpha: Float) {
    withTransform({ translate(fit.left, fit.top); scale(fit.scale, fit.scale, Offset.Zero) }) {
        val furniture = furniturePath(scene)
        val window = GameLightingCatalog.rooms[scene.place]?.window.takeIf { !GameWorld.isWorld(scene) }
        val windowLight = lights.firstOrNull { it.kind == GameSceneLighting.Kind.WINDOW }
        if (window != null) {
            val night = 1f - (windowLight?.power?.div(.60f) ?: 0f).coerceIn(0f, 1f)
            // Nur die Scheiben werden kuehl: Fensterrahmen und Pflanzen behalten ihre Malerei.
            val cellW = (window.right - window.left) / 3f
            val cellH = (window.bottom - window.top) / 2f
            repeat(3) { column -> repeat(2) { row ->
                drawRect(Color(0xFF102843).copy(alpha = night * .46f * alpha),
                    Offset(window.left + column * cellW + 3f, window.top + row * cellH + 4f),
                    Size((cellW - 6f).coerceAtLeast(1f), (cellH - 8f).coerceAtLeast(1f)))
            } }
        }
        val beam = if (window != null && windowLight != null) Path().apply {
                moveTo(window.left, window.bottom); lineTo(window.right, window.bottom)
                lineTo(window.floorX + 68f, window.floorY); lineTo(window.floorX - 64f, window.floorY); close()
        } else null
        if (beam != null && window != null && windowLight != null) {
            clipPath(furniture, ClipOp.Difference) {
                drawPath(beam, Brush.verticalGradient(listOf(Color.Transparent,
                    lightColor(windowLight.tone).copy(alpha = windowLight.power * .12f * alpha)),
                    window.bottom, window.floorY))
            }
        }
        lights.filter { !it.directional && it.power > .01f }.forEach { light ->
            val center = Offset(light.x, light.y)
            val radius = light.radius
            drawCircle(Brush.radialGradient(listOf(lightColor(light.tone).copy(alpha = light.power * .24f * alpha),
                Color.Transparent), center, radius), radius, center)
            if (light.kind in setOf(GameSceneLighting.Kind.WINDOW, GameSceneLighting.Kind.FIRE,
                    GameSceneLighting.Kind.CRYSTAL)) {
                fun dust() { repeat(9) { index ->
                    val p = GameAtmosphere.dust(light, index, clock)
                    drawCircle(lightColor(light.tone).copy(alpha = p.alpha * alpha), p.radius, Offset(p.x, p.y))
                } }
                if (light.kind == GameSceneLighting.Kind.WINDOW && beam != null) clipPath(beam) { dust() }
                else dust()
            }
            if (light.kind == GameSceneLighting.Kind.FIRE) {
                val t = clock / 1000f
                repeat(5) { i ->
                    val age = (t * .25f + i * .20f) % 1f
                    drawCircle(Color(0xFFFFCC78).copy(alpha = (1f - age) * .3f * alpha), 1f,
                        Offset(light.x + kotlin.math.sin(t + i) * 7f, light.y - age * 37f))
                }
            }
        }
        // Der neue Wurfschatten liegt auf dem Boden, nicht ueber den gemalten Moebelfronten.
        clipPath(furniture, ClipOp.Difference) {
            for (piece in GameFurniture.pieces(scene)) {
                val effective = lights.filter { GameSceneLighting.influence((piece.left + piece.right) / 2f,
                    piece.top, it) > .04f }.maxByOrNull {
                    GameSceneLighting.influence((piece.left + piece.right) / 2f, piece.top, it) }
                    ?: continue
                val height = piece.ground - piece.top
                val center = (piece.left + piece.right) / 2f
                val dx = if (effective.directional) effective.directionX * height * .8f
                    else ((center - effective.x) * height / effective.height.coerceAtLeast(25f)).coerceIn(-90f, 90f)
                val dy = if (effective.directional) height * .14f
                    else ((piece.ground - effective.groundY) * .28f * height / effective.height.coerceAtLeast(25f)).coerceIn(-25f, 45f)
                val path = Path().apply {
                    moveTo(piece.left, piece.back); lineTo(piece.right, piece.back)
                    lineTo(piece.right + dx, piece.front + dy); lineTo(piece.left + dx, piece.front + dy); close()
                }
                val power = GameSceneLighting.influence(center, piece.top, effective)
                drawPath(path, Color(0xFF182126).copy(alpha = .11f * power * alpha), style = Stroke(5f))
                drawPath(path, Color(0xFF182126).copy(alpha = .18f * power * alpha))
            }
        }
    }
}

internal data class GameShadowCaster(val scene: GameScenes.Scene, val pos: PlayControl.Pos,
    val species: AvatarSpecies, val lift: Float = 0f, val receiver: Float = 0f)

/** Spieler und Bewohner haben Schatten aus derselben Rechnung, jeweils an ihren eigenen Fuessen. */
@Composable
internal fun GameCharacterShadows(casters: List<GameShadowCaster>, camera: GameCamera.State,
    minute: Int, lampOn: Boolean, tvOn: Boolean, phase: Int, fade: Float, modifier: Modifier) {
    Canvas(modifier) {
        for (caster in casters) {
            val scene = caster.scene
            val fit = GameCamera.fit(camera, scene, size.width, size.height)
            val (x, y) = GameScenes.feet(scene, caster.pos)
            val (screenX, screenY) = fit.toScreen(x, y)
            val margin = GameCharacterScale.visibleHeight(scene, caster.pos, caster.species) * fit.scale * 2f
            if (screenX + margin < 0f || screenX - margin > size.width ||
                screenY + margin < 0f || screenY - margin > size.height) continue
            val lights = GameSceneLighting.sources(scene, minute, lampOn, tvOn, phase)
            val shadows = GameSceneLighting.shadows(scene, caster.pos, lights,
                GameCharacterScale.visibleHeight(scene, caster.pos, caster.species), caster.lift, caster.receiver)
            withTransform({ translate(fit.left, fit.top); scale(fit.scale, fit.scale, Offset.Zero) }) {
                fun paint() { shadows.forEach { paintShadow(it, fade) } }
                if (caster.receiver > 0f) paint()
                else clipPath(furniturePath(scene), ClipOp.Difference) { paint() }
            }
        }
    }
}


/** Schatten werden einmal in absoluten Weltkoordinaten gezeichnet, auch ueber Bildanschluesse. */
internal fun DrawScope.paintWorldShadows(scene: GameScenes.Scene, fit: GameScenes.Fit,
    minute: Int, clock: Long, alpha: Float) {
    val origin = GameWorld.origin(scene.place)
    val sun = GameSceneLighting.sources(scene, minute, false, false, (clock / 200L).toInt())
        .firstOrNull { it.directional }?.let { it.copy(x = it.x + origin, fadeStart = it.fadeStart + origin) } ?: return
    withTransform({ translate(fit.left - origin * fit.scale, fit.top); scale(fit.scale, fit.scale, Offset.Zero) }) {
        for (caster in GameWorldShadows.casters) {
            val cast = GameWorldShadows.project(caster, sun, clock)
            if (cast.alpha <= .001f) continue
            val minX = cast.points.minOf { it.first }; val maxX = cast.points.maxOf { it.first }
            if (fit.left + (maxX-origin)*fit.scale < 0f || fit.left + (minX-origin)*fit.scale > size.width) continue
            val path = Path().apply {
                cast.points.forEachIndexed { i, p -> if (i == 0) moveTo(p.first,p.second) else lineTo(p.first,p.second) }
                close()
            }
            repeat(3) { pass -> drawPath(path, Color(0xFF172C35).copy(alpha = cast.alpha * alpha * .14f),
                style = Stroke(cast.softness * (3-pass))) }
            drawPath(path, Color(0xFF172C35).copy(alpha = cast.alpha * alpha * .58f))
        }
    }
}
