package com.notime.glyphsim.matrix

import kotlin.math.ceil
import kotlin.math.hypot
import com.notime.glyphsim.matrix.GameEnvironment.Material as M

/** Ortsfeste Laufkarte in Bildpixeln. Kein Farbraten im Bildtakt und keine zweite Simulation. */
object GameWalkingMap {
    data class Edge(val x: Float, val back: Float, val front: Float)
    data class Area(val id: String, val points: List<Pair<Float, Float>>, val material: M? = null) {
        fun contains(x: Float, y: Float): Boolean {
            var hit = false
            var previous = points.last()
            for (p in points) {
                if ((p.second > y) != (previous.second > y) &&
                    x < (previous.first - p.first) * (y - p.second) / (previous.second - p.second) + p.first) hit = !hit
                previous = p
            }
            return hit
        }
    }
    private fun edge(vararg values: Int) = values.toList().chunked(3).map { Edge(it[0].toFloat(), it[1].toFloat(), it[2].toFloat()) }
    /** Hintere Stauden und vordere Felskante sind entlang jeder Malerei einzeln vermessen.
     * Die Randwerte bleiben am Anschluss gleich; der Brueckenbogen hat sein eigenes Profil. */
    val profiles = mapOf(
        "world/street-park-forest.png" to edge(0,495,562, 120,497,555, 280,501,551, 440,499,550,
            620,510,552, 780,505,558, 940,504,552, 1100,492,548, 1280,497,550,
            1450,508,555, 1640,523,560, 1810,518,557, 1920,495,562),
        "world/village-edge.png" to edge(0,495,562, 180,501,558, 370,505,552, 540,510,554,
            710,510,555, 880,506,550, 1050,507,551, 1250,502,549, 1430,505,554, 1650,510,557, 1920,495,562),
        "world/uplands.png" to edge(0,495,562, 160,501,554, 340,502,552, 510,504,553,
            710,508,557, 930,503,553, 1120,508,554, 1310,506,554, 1480,501,553,
            1690,507,553, 1840,502,557, 1920,495,562),
        "world/mountain-pass.png" to edge(0,495,562, 190,502,556, 370,507,551, 550,508,552,
            760,512,553, 990,512,554, 1210,510,553, 1440,515,559, 1680,511,558, 1920,495,562),
        "world/expedition.png" to edge(0,495,562, 180,503,557, 360,510,554, 540,517,557,
            710,517,558, 920,511,555, 1090,505,554, 1280,508,557, 1450,513,557,
            1650,514,556, 1820,506,557, 1920,495,562)
    )
    private fun area(id: String, material: M?, vararg xy: Int) = Area(id,
        xy.toList().chunked(2).map { it[0].toFloat() to it[1].toFloat() }, material)
    /** Ausbuchtungen/Hindernisse innerhalb des Laufkorridors; Material ist keine Begehbarkeit. */
    val areas = mapOf(
        "world/street-park-forest.png" to listOf(
            area("park-grass-edge",M.GRASS, 490,495, 610,505, 700,520, 665,529, 520,519),
            area("meadow-grass-edge",M.GRASS, 985,497, 1090,490, 1210,505, 1180,520, 1040,516),
            area("forest-fern-edge",M.GRASS, 1470,509, 1605,519, 1690,529, 1640,538, 1490,527),
            area("park-front-rock",null, 735,550, 753,545, 786,552, 802,574, 729,577)),
        "world/village-edge.png" to listOf(
            area("orchard-grass",M.GRASS, 710,509, 860,505, 942,510, 890,524, 730,526),
            area("village-front-rock",null, 1410,551, 1440,547, 1462,555, 1471,576, 1401,577)),
        "world/uplands.png" to listOf(area("field-grass",M.GRASS, 970,508, 1135,507, 1190,515, 1125,527, 985,524)),
        "world/mountain-pass.png" to listOf(area("pass-grass",M.GRASS, 1060,510, 1230,509, 1280,521, 1085,525)),
        "world/expedition.png" to listOf(area("camp-grass",M.GRASS, 390,510, 510,513, 564,525, 405,529)),
        "world/coast.png" to listOf(
            area("swamp-reeds",M.GRASS, 1040,450, 1160,451, 1200,483, 1070,489),
            area("jungle-reeds",M.GRASS, 1440,449, 1600,456, 1625,498, 1460,486))
    )
    fun regionX(scene: GameScenes.Scene, x: Float) = GameWorld.region(scene.place)?.let {
        GameWorld.origin(scene.place) - GameWorld.regionOrigin(it) + x
    } ?: x
    fun band(scene: GameScenes.Scene, normalizedX: Float): Pair<Float, Float>? {
        val points = profiles[scene.asset] ?: return null
        val x = regionX(scene, normalizedX * GameWorld.region(scene.place)!!.section)
        val index = points.indexOfFirst { it.x >= x }.let { if (it < 0) points.lastIndex else it }
        if (index == 0) return points.first().back to points.first().front
        val a=points[index-1]; val b=points[index]
        val t=((x-a.x)/(b.x-a.x)).coerceIn(0f,1f)
        return a.back+(b.back-a.back)*t to a.front+(b.front-a.front)*t
    }
    fun blocked(scene: GameScenes.Scene, pos: PlayControl.Pos): Boolean {
        val (x,y)=GameScenes.feet(scene,pos);val rx=regionX(scene,x)
        return areas[scene.asset].orEmpty().any { it.material==null && it.contains(rx,y) }
    }
    fun material(scene: GameScenes.Scene, x: Float, y: Float): M? {
        val rx=regionX(scene,x)
        areas[scene.asset].orEmpty().lastOrNull { it.material!=null && it.contains(rx,y) }?.let { return it.material }
        if (scene.asset.startsWith("interiors/")) return if(scene.place==PlayScene.Place.BATH) M.STONE else M.WOOD
        if (!GameWorld.isWorld(scene)) return null
        return when(scene.place) {
            PlayScene.Place.CITY,PlayScene.Place.STREET,PlayScene.Place.GROTTO,PlayScene.Place.COAST_PATH -> M.STONE
            PlayScene.Place.PARK,PlayScene.Place.SPORT,PlayScene.Place.VILLAGE_EDGE -> M.SAND
            PlayScene.Place.POND,PlayScene.Place.BEACH -> M.SAND
            PlayScene.Place.SWAMP -> M.MUD
            else -> M.SAND
        }
    }
    /** Auch ein schneller Schritt oder Sprung darf keine schmale Sperrinsel ueberspringen. */
    fun clear(scene: GameScenes.Scene, from: PlayControl.Pos, to: PlayControl.Pos): Boolean {
        val a=GameScenes.feet(scene,from);val b=GameScenes.feet(scene,to)
        val steps=ceil(hypot(b.first-a.first,b.second-a.second)/2f).toInt().coerceAtLeast(1)
        return (0..steps).all { i ->
            val t=i.toFloat()/steps
            GameTerrain.valid(scene,PlayControl.Pos(from.x+(to.x-from.x)*t,from.depth+(to.depth-from.depth)*t))
        }
    }
    fun corners(scene: GameScenes.Scene): List<PlayControl.Pos> {
        val region=GameWorld.region(scene.place) ?: return emptyList()
        val offset=GameWorld.origin(scene.place)-GameWorld.regionOrigin(region)
        return areas[scene.asset].orEmpty().filter { it.material==null }.flatMap { area ->
            val centerX=area.points.map { it.first }.average().toFloat()
            val centerY=area.points.map { it.second }.average().toFloat()
            area.points.map { (x,y) -> GameScenes.posAt(scene,
                x-offset+if(x<centerX) -4f else 4f,y+if(y<centerY) -4f else 4f) }
        }.filter { GameTerrain.valid(scene,it) }
    }
}
