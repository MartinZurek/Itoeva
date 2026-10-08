package com.notime.glyphsim.matrix

/** Baumkronen, Gebaeude und Felsen haben Weltanker und teilen Sonne/Abschattung mit den Figuren. */
object GameWorldShadows {
    data class Caster(val id: String, val x: Float, val ground: Float, val height: Float,
        val width: Float, val foliage: Boolean = false)
    data class Cast(val points: List<Pair<Float,Float>>, val alpha: Float, val softness: Float)
    private fun cast(asset: String, vararg values: Caster) = GameWorld.regions.first { it.asset == asset }.let { region ->
        values.map { it.copy(x = it.x + GameWorld.regionOrigin(region)) }
    }
    val casters: List<Caster> by lazy { buildList {
        addAll(cast("world/street-park-forest.png",
            Caster("street-house",180f,501f,290f,190f), Caster("park-tree",530f,495f,330f,95f,true),
            Caster("park-tree-east",716f,493f,285f,90f,true), Caster("forest-tree",1780f,509f,340f,120f,true)))
        addAll(cast("world/coast.png", Caster("pond-house",100f,463f,260f,150f),
            Caster("pond-tree",281f,468f,280f,110f,true), Caster("jungle-palm",1510f,491f,300f,100f,true),
            Caster("jungle-tree",1750f,496f,310f,115f,true)))
        addAll(cast("world/village-edge.png", Caster("village-west-tree",160f,506f,350f,100f,true),
            Caster("village-orchard",1140f,490f,230f,80f,true), Caster("village-gate",1580f,510f,220f,190f)))
        addAll(cast("world/uplands.png", Caster("city-house",130f,507f,285f,170f),
            Caster("city-tree",375f,508f,270f,100f,true), Caster("mountain-rock",1750f,500f,175f,170f)))
        addAll(cast("world/coast-path.png", Caster("coast-path-tree",1440f,490f,250f,100f,true)))
        addAll(cast("world/mountain-pass.png", Caster("pass-tree",1290f,500f,350f,110f,true),
            Caster("pass-rock",275f,500f,175f,130f)))
        addAll(cast("world/expedition.png", Caster("camp-tree",105f,498f,350f,120f,true),
            Caster("camp-tree-east",590f,499f,335f,110f,true)))
    } }
    fun project(caster: Caster, light: GameSceneLighting.Light, clock: Long, receiverHeight: Float = 0f): Cast {
        val height = (caster.height - receiverHeight).coerceAtLeast(0f)
        val sway = if (caster.foliage) GameAtmosphere.wind(caster.x,clock) * height * .017f else 0f
        val dx = light.directionX * height * .65f + sway
        val dy = height * .22f
        val w = caster.width / 2f
        val points = if (caster.foliage) listOf(
            caster.x - w * .20f to caster.ground, caster.x + w * .20f to caster.ground,
            caster.x + dx + w to caster.ground + dy * .72f,
            caster.x + dx + w * .7f to caster.ground + dy,
            caster.x + dx - w * .8f to caster.ground + dy,
            caster.x + dx - w to caster.ground + dy * .68f)
            else listOf(caster.x-w to caster.ground,caster.x+w to caster.ground,
                caster.x+dx+w to caster.ground+dy,caster.x+dx-w to caster.ground+dy)
        return Cast(points, GameSceneLighting.influence(caster.x, caster.ground, light) *
            if (caster.foliage) .17f else .24f, if (caster.foliage) 7f else 3f)
    }
    fun contains(points: List<Pair<Float,Float>>, x: Float, y: Float): Boolean {
        var inside = false; var j = points.lastIndex
        for (i in points.indices) {
            val a = points[i]; val b = points[j]
            if ((a.second > y) != (b.second > y) && x <
                (b.first-a.first)*(y-a.second)/(b.second-a.second)+a.first) inside = !inside
            j = i
        }
        return inside
    }
    fun transmission(scene: GameScenes.Scene, light: GameSceneLighting.Light, x: Float, floor: Float, z: Float): Float {
        if (!GameWorld.isWorld(scene) || !light.directional) return 1f
        val worldX = GameWorld.origin(scene.place) + x
        val worldLight = light.copy(x = light.x + GameWorld.origin(scene.place),
            fadeStart = light.fadeStart + GameWorld.origin(scene.place))
        var darkest = 1f
        for (c in casters) {
            if (kotlin.math.abs(c.x-worldX) >= c.height+c.width || z >= c.height) continue
            val points = project(c,worldLight,light.shadowClock,z).points
            if (!contains(points,worldX,floor)) continue
            var distance = Float.MAX_VALUE
            for (i in points.indices) {
                val a=points[i];val b=points[(i+1)%points.size]
                val dx=b.first-a.first;val dy=b.second-a.second
                val t=(((worldX-a.first)*dx+(floor-a.second)*dy)/(dx*dx+dy*dy).coerceAtLeast(.001f)).coerceIn(0f,1f)
                distance=minOf(distance,kotlin.math.hypot(worldX-a.first-t*dx,floor-a.second-t*dy))
            }
            darkest=minOf(darkest,1f-.65f*(distance/8f).coerceIn(0f,1f))
        }
        return darkest
    }
}
