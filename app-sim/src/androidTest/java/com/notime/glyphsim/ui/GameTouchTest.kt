package com.notime.glyphsim.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.notime.glyphsim.matrix.GameMovement
import com.notime.glyphsim.matrix.PlayControl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Echte Pointer-IDs im Emulator: ein rechter Tipp darf den linken Daumen nicht loslassen. */
@RunWith(AndroidJUnit4::class)
class GameTouchTest {
    @get:Rule val compose = createComposeRule()
    private var stick = PlayControl.Stick()
    private val commands = mutableListOf<GameMovement.Command>()
    private val enabled = mutableStateOf(true)
    private fun show() {
        compose.setContent {
            GameTouch(onStick = { stick = it }, onCommand = { command, _ -> commands += command },
                onTap = {}, onDoubleTap = {}, enabled = enabled.value,
                modifier = Modifier.fillMaxSize().testTag("controls"))
        }
    }
    private fun px(dp: Float) = with(compose.density) { dp.dp.toPx() }

    @Test fun rechterSprungLaesstLinkenDaumenWeiterSteuern() {
        show()
        val radius = px(56f)
        val bottom = px(90f)
        compose.onNodeWithTag("controls").performTouchInput {
            val origin = Offset(width * 0.20f, height * 0.75f)
            down(0, origin)
            moveTo(0, origin + Offset(radius, 0f))
            down(1, Offset(width - radius * 1.3f, height - bottom))
            advanceEventTime(60L)
            up(1)
        }
        compose.runOnIdle {
            assertEquals(listOf(GameMovement.Command.JUMP), commands)
            assertTrue(stick.x > 0.9f)
        }
        compose.onNodeWithTag("controls").performTouchInput { up(0) }
        compose.runOnIdle { assertEquals(PlayControl.Stick(), stick) }
    }

    @Test fun rechteWischgesteErzeugtGenauEineRolle() {
        show()
        val radius = px(56f)
        val bottom = px(90f)
        compose.onNodeWithTag("controls").performTouchInput {
            val origin = Offset(width - radius * 1.3f, height - bottom)
            down(origin)
            moveTo(origin - Offset(radius, 0f))
            advanceEventTime(80L)
            up()
        }
        compose.runOnIdle { assertEquals(listOf(GameMovement.Command.ROLL), commands) }
    }

    @Test fun abbrechenOderMenueOeffnenLaesstKeineBewegungHaengen() {
        show()
        val radius = px(56f)
        compose.onNodeWithTag("controls").performTouchInput {
            val origin = Offset(width * 0.20f, height * 0.75f)
            down(origin)
            moveTo(origin + Offset(radius, 0f))
        }
        compose.runOnIdle { assertTrue(stick.x > 0.9f); enabled.value = false }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(PlayControl.Stick(), stick) }
        compose.onNodeWithTag("controls").performTouchInput { cancel() }
    }

    @Test fun langesHaltenSetztSichHinOhneSprungBeimLoslassen() {
        show()
        val radius = px(56f)
        val bottom = px(90f)
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("controls").performTouchInput {
            down(Offset(width - radius * 1.3f, height - bottom))
        }
        compose.mainClock.advanceTimeBy(600L)
        compose.onNodeWithTag("controls").performTouchInput { up() }
        compose.mainClock.autoAdvance = true
        compose.runOnIdle { assertEquals(listOf(GameMovement.Command.REST), commands) }
    }

    @Test fun systemAbbruchIstKeinSprungOderWeltTipp() {
        var taps = 0
        compose.setContent {
            GameTouch(onStick = { stick = it }, onCommand = { command, _ -> commands += command },
                onTap = { taps++ }, onDoubleTap = { taps++ },
                modifier = Modifier.fillMaxSize().testTag("controls"))
        }
        val radius = px(56f)
        val bottom = px(90f)
        compose.onNodeWithTag("controls").performTouchInput {
            down(0, Offset(width * 0.20f, height * 0.75f))
            down(1, Offset(width - radius * 1.3f, height - bottom))
            cancel()
        }
        compose.runOnIdle {
            assertTrue(commands.isEmpty())
            assertEquals(0, taps)
            assertEquals(PlayControl.Stick(), stick)
        }
    }
    @Test fun rechteAktionsflaecheBleibtAuchBeimGesperrtenSpielSichtbar() {
        show()
        compose.onNodeWithTag("game-action-pad",useUnmergedTree=true).assertIsDisplayed()
        compose.runOnIdle { enabled.value=false }
        compose.waitForIdle()
        compose.onNodeWithTag("game-action-pad",useUnmergedTree=true).assertIsDisplayed()
        val radius=px(56f);val bottom=px(90f)
        compose.onNodeWithTag("controls").performTouchInput {
            down(Offset(width-radius*1.3f,height-bottom));up()
        }
        compose.runOnIdle { assertTrue(commands.isEmpty()) }
    }

}
