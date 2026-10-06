package com.notime.glyphsim.matrix

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Der breite Quelldatensatz darf nie als eine einzige GPU-Textur gezeichnet werden. */
@RunWith(AndroidJUnit4::class)
class CreatureSheetsTest {
    @Test fun fennecBilderSindKleinPixelgleichUndWiederverwendet() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sheet = CreatureSheets.get(context, AvatarSpecies.FENNEC)!!
        assertEquals(128, sheet.frameSize)
        assertEquals(138, sheet.frames.size)
        assertTrue(sheet.frames.all { it.width == 128 && it.height == 128 })
        assertSame(sheet, CreatureSheets.get(context, AvatarSpecies.FENNEC))
        val source = context.assets.open(CreatureSprites.assetFor(AvatarSpecies.FENNEC))
            .use { BitmapFactory.decodeStream(it) }!!
        try {
            for (frame in listOf(0, 68, 114, 125, 137)) {
                val expected = IntArray(128 * 128)
                val actual = IntArray(expected.size)
                source.getPixels(expected, 0, 128, frame * 128, 0, 128, 128)
                sheet.frames[frame].asAndroidBitmap().getPixels(actual, 0, 128, 0, 0, 128, 128)
                assertArrayEquals(expected, actual)
            }
        } finally {
            source.recycle()
        }
    }
}
