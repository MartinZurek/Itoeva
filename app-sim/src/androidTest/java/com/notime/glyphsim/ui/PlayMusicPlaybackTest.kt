package com.notime.glyphsim.ui

import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.notime.glyphsim.matrix.MusicRole
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Echte ausgelieferte Ressource und nativer Decoder; kein Hoertest des Telefonlautsprechers. */
@RunWith(AndroidJUnit4::class)
class PlayMusicPlaybackTest {
    @Test fun derAusgelieferteTagestrackStartetMitDemProduktivenMedienplayer() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val res = requireNotNull(PlayMusic.trackResId(context, MusicRole.MAIN_DAY))
        val player = requireNotNull(PlayMusic.createScorePlayer(context, res))
        try {
            assertTrue(player.duration > 1_000)
            player.isLooping = true
            player.setVolume(0f, 0f)
            player.start()
            SystemClock.sleep(600)
            assertTrue(player.isPlaying)
            assertTrue(player.currentPosition > 0)
        } finally { player.release() }
    }
}
