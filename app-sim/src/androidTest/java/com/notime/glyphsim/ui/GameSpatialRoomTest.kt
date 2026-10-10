package com.notime.glyphsim.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.notime.glyphsim.matrix.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameSpatialRoomTest {
    @Test fun alleRaeumeBewahrenDieGeladeneMalerei() {
        for(place in GameInteriorCatalog.scenes.keys) {
            val scene=GameWorld.scene(place)!!
            val output=Bitmap.createBitmap(480,270,Bitmap.Config.ARGB_8888)
            val context=InstrumentationRegistry.getInstrumentation().targetContext
            // Game-Assets sind im Debug-Emulator nicht gepackt. Dort prueft eine eindeutige
            // Testbitmap dieselbe native Bilduebergabe; im Game-Build die Originalmalerei.
            val source=if(context.assets.list("interiors").orEmpty().contains(scene.asset.substringAfterLast('/')))
                context.assets.open(scene.asset).use { BitmapFactory.decodeStream(it) }
            else Bitmap.createBitmap(960,540,Bitmap.Config.ARGB_8888).apply {
                val paint=android.graphics.Paint().apply {
                    shader=android.graphics.LinearGradient(0f,0f,960f,540f,
                        intArrayOf(0xFFB57A45.toInt(),0xFF3B5132.toInt(),0xFFFFD69A.toInt()),null,
                        android.graphics.Shader.TileMode.CLAMP)
                }
                android.graphics.Canvas(this).drawRect(0f,0f,960f,540f,paint)
            }
            assertNotNull(source)
            try {
                CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,
                    Canvas(output.asImageBitmap()),Size(480f,270f)) {
                    paintRoomSpace(scene,GameScenes.Fit(1f,0f,0f),1f,image=source.asImageBitmap())
                }
                // Die geladene Bildbasis muss im echten Android-Canvas erhalten bleiben.
                // Eine einzelne Linie schneidet manchmal nur zwei gleichfarbige Dielen.
                val woodColors=(235..260).flatMap { y -> (120..360).map { x -> output.getPixel(x,y) } }.distinct()
                assertTrue(place.name,woodColors.size>12)
                assertEquals(255,output.getPixel(240,245) ushr 24)
                assertNotEquals(output.getPixel(240,245),output.getPixel(300,40))
                val body=GameRoomSpace.bodies(scene).first()
                val x=((body.piece.left+body.piece.right)/2).toInt()
                assertNotEquals(output.getPixel(x,body.piece.top.toInt()-2),output.getPixel(x,body.piece.ground.toInt()+10))
                // Stichproben vergleichen den produktiven Zeichenweg mit derselben Originalbitmap.
                val expected=Bitmap.createBitmap(480,270,Bitmap.Config.ARGB_8888)
                try {
                    val paint=android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
                    android.graphics.Canvas(expected).drawBitmap(source,null,android.graphics.Rect(0,0,480,270),paint)
                    for((x,y) in listOf(240 to 245,300 to 40,80 to 80,225 to 140))
                        assertEquals(place.name,expected.getPixel(x,y),output.getPixel(x,y))
                } finally { expected.recycle() }
            } finally { output.recycle();source.recycle() }
        }
    }
}
