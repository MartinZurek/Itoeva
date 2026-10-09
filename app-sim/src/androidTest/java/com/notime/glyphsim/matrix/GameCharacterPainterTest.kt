package com.notime.glyphsim.matrix

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Der neue native Zeichenweg wird mit echten Android-Bitmaps ausgefuehrt. */
@RunWith(AndroidJUnit4::class)
class GameCharacterPainterTest {
    @Test fun alleWesenBehaltenTransparenteRaenderUndDenGemeinsamenBoden() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (species in AvatarSpecies.entries) {
            val sheet = CreatureSheets.get(context, species)!!
            assertEquals(sheet.frames.size, sheet.tops.size)
            val painter = GameCharacterPainter()
            for (frame in listOf(0, 9, 27, 32, 68, 76, 93, 103, 110, 114, 125, 137, 95, 104, 111)) {
                painter.update(1234,456,2f,1.5f,if(frame in listOf(95,104,111)) GameWater.Swim(.25f,0f,9f,true) else null)
                val output = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
                try {
                    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr,
                        Canvas(output.asImageBitmap()), Size(256f, 256f)) {
                        painter.draw(this, sheet.frames[frame], species, frame, sheet.tops[frame],
                            0f, 0f, 256f, 1f, 1f, null, false)
                    }
                    val pixels = IntArray(256*256)
                    output.getPixels(pixels, 0, 256, 0, 0, 256, 256)
                    assertTrue("$species/$frame: sichtbare Figur", pixels.count { (it ushr 24) > 128 } > 400)
                    assertTrue("$species/$frame: oberer Rand", pixels.take(256).all { (it ushr 24) == 0 })
                    assertTrue("$species/$frame: unter dem Boden", pixels.takeLast(256*3).all { (it ushr 24) == 0 })
                    assertEquals(0, output.getPixel(0,128) ushr 24)
                    assertEquals(0, output.getPixel(255,128) ushr 24)
                } finally { output.recycle() }
            }
        }
    }

    @Test fun diePhysischeLichtseiteBleibtBeimSpiegelnAufDerselbenSeite() {
        val source = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888).apply { eraseColor(-1) }
        val light = GameSceneLighting.CharacterLight(GameSceneLighting.Rgb(1f,.2f,.2f),
            GameSceneLighting.Rgb(.2f,.2f,1f))
        try {
            for (mirrored in listOf(false, true)) {
                val painter = GameCharacterPainter().apply { update(0,0,0f,0f) }
                val output = Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888)
                try {
                    CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,
                        Canvas(output.asImageBitmap()),Size(256f,256f)) {
                        scale(if (mirrored) -1f else 1f,1f) {
                            painter.draw(this,source.asImageBitmap(),AvatarSpecies.FENNEC,95,0,
                                0f,0f,256f,1f,1f,light,mirrored)
                        }
                    }
                    val left=output.getPixel(32,32); val right=output.getPixel(224,32)
                    assertTrue(android.graphics.Color.red(left)>android.graphics.Color.blue(left))
                    assertTrue(android.graphics.Color.blue(right)>android.graphics.Color.red(right))
                } finally { output.recycle() }
            }
        } finally { source.recycle() }
    }
}
