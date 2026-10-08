package com.notime.glyphsim.matrix

import kotlin.math.pow

/** Langsame gemalte Bodenbeleuchtung vom Material trennen; Zeichnung und Farbverhaeltnisse bleiben. */
object GameGroundLight {
    private const val COLS = 96
    private const val ROWS = 32
    data class Field(val values: FloatArray) {
        fun gain(u: Float, v: Float): Float {
            val x = (u.coerceIn(0f,1f) * COLS - .5f).coerceIn(0f,COLS-1f)
            val y = (v.coerceIn(0f,1f) * ROWS - .5f).coerceIn(0f,ROWS-1f)
            val ix = x.toInt(); val iy = y.toInt()
            val jx = (ix+1).coerceAtMost(COLS-1); val jy = (iy+1).coerceAtMost(ROWS-1)
            val a = values[iy*COLS+ix]*(1f-(x-ix))+values[iy*COLS+jx]*(x-ix)
            val b = values[jy*COLS+ix]*(1f-(x-ix))+values[jy*COLS+jx]*(x-ix)
            return a*(1f-(y-iy))+b*(y-iy)
        }
    }
    private data class Geometry(val start: Float, val width: Float, val height: Float, val interior: GameScenes.Scene?)
    private val geometries by lazy {
        (GameInteriorCatalog.scenes.values.map { it.asset } + GameWorld.regions.map { it.asset } +
            GameWorld.seams.map { it.asset }).associateWith { buildGeometry(it) }
    }
    private fun geometry(asset: String) = geometries[asset]
    private fun buildGeometry(asset: String): Geometry? {
        GameInteriorCatalog.scenes.values.firstOrNull { it.asset == asset }?.let { return Geometry(0f,480f,270f,it) }
        GameWorld.regions.firstOrNull { it.asset == asset }?.let { return Geometry(GameWorld.regionOrigin(it),it.width,640f,null) }
        GameWorld.seams.firstOrNull { it.asset == asset }?.let { return Geometry(it.x-480f,960f,640f,null) }
        return null
    }
    fun floor(asset: String, u: Float, v: Float): Boolean {
        val geometry = geometry(asset) ?: return false
        val y = v * geometry.height
        if (geometry.interior != null) {
            val scene = geometry.interior; val x = u * geometry.width
            if (y < maxOf(scene.farY, 193f) || y > 265f) return false
            return GameFurniture.pieces(scene).none { piece -> y <= piece.ground && x in piece.left-5f..piece.right+5f && piece.contours.any { GameWorldShadows.contains(it,x,y) } }
        }
        // Der Vordergrund, Felswaende, Stämme und Wasser werden nicht als Boden aufgezogen.
        if (y !in 520f..604f) return false
        val x = geometry.start + u * geometry.width
        val place = GameWorld.places.lastOrNull { GameWorld.origin(it) <= x } ?: return false
        if (place == PlayScene.Place.GROTTO) return false
        val scene = GameWorld.scene(place)!!
        if (GameWorld.material(scene,x-GameWorld.origin(place),y) == GameEnvironment.Material.WATER) return false
        return GameFurniture.pieces(scene).none { p -> p.contours.any { GameWorldShadows.contains(it,x-GameWorld.origin(place),y) } }
    }
    /** Eingabe ist ein kleines, gleichmaessig gesampeltes Lichtfeld, keine Kopie des Vollbildes. */
    fun estimate(asset: String, sample: (Float,Float) -> Int): Field {
        val luminance = FloatArray(COLS*ROWS)
        val valid = BooleanArray(COLS*ROWS)
        for (y in 0 until ROWS) for (x in 0 until COLS) {
            val u=(x+.5f)/COLS; val v=(y+.5f)/ROWS; val i=y*COLS+x
            valid[i]=floor(asset,u,v)
            if (valid[i]) {
                val rgb=sample(u,v)
                luminance[i]=((rgb shr 16 and 255)*.2126f+(rgb shr 8 and 255)*.7152f+(rgb and 255)*.0722f)/255f
            }
        }
        val gains = FloatArray(COLS*ROWS) { 1f }
        for (y in 0 until ROWS) {
            val row=(0 until COLS).filter { valid[y*COLS+it] }.map { luminance[y*COLS+it] }.sorted()
            if (row.size < 5) continue
            val target=row[row.size/2].coerceAtLeast(.08f)
            for (x in 0 until COLS) {
                val i=y*COLS+x
                if (!valid[i]) continue
                var total=0f; var count=0
                for (dy in -1..1) for (dx in -3..3) {
                    val nx=x+dx; val ny=y+dy
                    if (nx !in 0 until COLS || ny !in 0 until ROWS) continue
                    val j=ny*COLS+nx
                    if (valid[j]) { total+=luminance[j];count++ }
                }
                gains[i]=(target/(total/count.coerceAtLeast(1)).coerceAtLeast(.035f)).pow(.70f).coerceIn(.72f,1.55f)
            }
        }
        return Field(gains)
    }
    fun apply(rgb: Int, gain: Float): Int {
        fun channel(shift: Int)=((rgb shr shift and 255)*gain).toInt().coerceIn(0,255)
        return (rgb and -0x1000000) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}
