package com.notime.glyphsim.ui

import android.graphics.Bitmap
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
    @Test fun alleRaeumeZeichnenKoerperOhneHintergrundbitmap() {
        for(place in GameInteriorCatalog.scenes.keys) {
            val scene=GameWorld.scene(place)!!
            val output=Bitmap.createBitmap(480,270,Bitmap.Config.ARGB_8888)
            try {
                CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,
                    Canvas(output.asImageBitmap()),Size(480f,270f)) {
                    paintRoomSpace(scene,GameScenes.Fit(1f,0f,0f),1f)
                }
                // Neue Materiallagen muessen im echten Android-Canvas statt nur im Export erscheinen.
                val woodColors=(120..240).map { output.getPixel(it,250) }.distinct()
                assertTrue(place.name,woodColors.size>4)
                assertEquals(255,output.getPixel(240,245) ushr 24)
                assertNotEquals(output.getPixel(240,245),output.getPixel(300,40))
                val body=GameRoomSpace.bodies(scene).first()
                val x=((body.piece.left+body.piece.right)/2).toInt()
                assertNotEquals(output.getPixel(x,body.piece.top.toInt()-2),output.getPixel(x,body.piece.ground.toInt()+10))
            } finally { output.recycle() }
        }
    }
}
