package com.notime.glyphsim.ui

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.notime.glyphsim.matrix.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.yield
import java.lang.ref.SoftReference

internal data class GameAssets(val images: Map<String,ImageBitmap?> = emptyMap(), val loaded: Int = 0,
    val total: Int = 30, val ready: Boolean = false, val missing: List<String> = emptyList()) {
    val progress get() = (loaded.toFloat()/total.coerceAtLeast(1)).coerceIn(0f,1f)
}
private data class CachedGameImage(val bitmap: Bitmap,val image: ImageBitmap,val field: GameGroundLight.Field,
    val corrected: Boolean=false)
/** Wiederbetreten nutzt fertige Bilder; Android darf unbenutzte Eintraege bei Speicherdruck verwerfen. */
private object GameImageCache {
    private val images=java.util.concurrent.ConcurrentHashMap<String,SoftReference<CachedGameImage>>()
    fun get(path: String)=images[path]?.get()
    fun put(path: String,entry: CachedGameImage) { images[path]=SoftReference(entry) }
}
private fun loadGameImage(assets: AssetManager,path: String): CachedGameImage {
    GameImageCache.get(path)?.let { return it }
    val bitmap=assets.open(path).use { BitmapFactory.decodeStream(it) } ?: error("Bild fehlt: $path")
    val small=Bitmap.createScaledBitmap(bitmap,96,32,false)
    val pixels=IntArray(96*32);small.getPixels(pixels,0,96,0,0,96,32)
    val field=GameGroundLight.estimate(path) { u,v -> pixels[(v*32).toInt().coerceAtMost(31)*96+(u*96).toInt().coerceAtMost(95)] }
    if(small!==bitmap) small.recycle()
    bitmap.prepareToDraw()
    return CachedGameImage(bitmap,bitmap.asImageBitmap(),field).also { GameImageCache.put(path,it) }
}
/** Ein kleiner Block arbeitet mit Zeilenpuffern. Geometrie wird nicht fuer jeden einzelnen Pixel aufgebaut. */
private fun correctRows(path: String,source: CachedGameImage,bitmap: Bitmap,from: Int,endRow: Int) {
    val row=IntArray(bitmap.width)
    for(y in from until endRow) {
        val v=(y+.5f)/bitmap.height
        if(v<.71f || v>.99f) continue
        bitmap.getPixels(row,0,bitmap.width,0,y,bitmap.width,1)
        for(x in row.indices step 8) {
            val end=minOf(x+8,row.size)
            val u=(x+end)*.5f/row.size
            val gain=source.field.gain(u,v)
            if(kotlin.math.abs(gain-1f)<.002f || !GameGroundLight.floor(path,u,v) ||
                !GameGroundLight.floor(path,(x+.5f)/row.size,v) || !GameGroundLight.floor(path,(end-.5f)/row.size,v)) continue
            for(i in x until end) row[i]=GameGroundLight.apply(row[i],gain)
        }
        bitmap.setPixels(row,0,bitmap.width,0,y,bitmap.width,1)
    }
}
/** Bilder/Figuren werden vor dem Spiel vorbereitet. Bodenlicht folgt in kurzen ruhigen Arbeitsfenstern.
 * Kein Ortswechsel wartet auf IO, keine Ladepause wird durch die Ausdauermechanik erzwungen. */
@Composable
internal fun rememberGameAssets(enabled: Boolean,place: PlayScene.Place,idle: Boolean,retry: Int): GameAssets {
    val context=LocalContext.current.applicationContext
    val assets=context.assets
    val initialPlace=remember(enabled,retry) { place }
    val idleNow by rememberUpdatedState(idle)
    val state by produceState(GameAssets(),assets,enabled,retry) {
        if(!enabled) { value=GameAssets(); return@produceState }
        val paths=GameAssetPlan.paths(initialPlace)
        val total=paths.size+AvatarSpecies.entries.size
        var images=emptyMap<String,ImageBitmap?>();var loaded=0;val missing=mutableListOf<String>()
        val sources=mutableMapOf<String,CachedGameImage>()
        for(path in paths) {
            val result=withContext(Dispatchers.Default) { runCatching { loadGameImage(assets,path) }.getOrNull() }
            if(result==null) missing+=path else sources[path]=result
            images=images+(path to result?.image)
            value=GameAssets(images,++loaded,total,false,missing.toList())
            yield()
        }
        for(species in AvatarSpecies.entries) {
            val sheet=withContext(Dispatchers.Default) { CreatureSheets.get(context,species) }
            if(sheet==null) missing+=CreatureSprites.assetFor(species)
            value=GameAssets(images,++loaded,total,false,missing.toList())
            yield()
        }
        value=GameAssets(images,loaded,total,missing.isEmpty(),missing.toList())
        if(missing.isNotEmpty()) return@produceState
        for(path in paths) {
            // Die ersetzte Rohfassung nicht bis zum Ende aller Korrekturen festhalten.
            // Sichtbare Bitmaps werden nie recycelt; Compose gibt sie nach dem Austausch frei.
            val source=sources.remove(path) ?: continue
            if(source.corrected) continue
            snapshotFlow { idleNow }.first { it }
            val corrected=withContext(Dispatchers.Default) { runCatching { source.bitmap.copy(Bitmap.Config.ARGB_8888,true) }.getOrNull() } ?: continue
            for(y in 0 until corrected.height step 32) {
                snapshotFlow { idleNow }.first { it }
                withContext(Dispatchers.Default) { correctRows(path,source,corrected,y,minOf(y+32,corrected.height)) }
                yield()
            }
            withContext(Dispatchers.Default) { corrected.prepareToDraw() }
            val entry=CachedGameImage(corrected,corrected.asImageBitmap(),source.field,true)
            GameImageCache.put(path,entry)
            images=images+(path to entry.image)
            value=value.copy(images=images)
        }
    }
    return state
}
