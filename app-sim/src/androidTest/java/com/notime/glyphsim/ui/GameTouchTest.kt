package com.notime.glyphsim.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.notime.glyphsim.matrix.GameWorld
import com.notime.glyphsim.matrix.GameTerrain
import com.notime.glyphsim.matrix.GameSurfaces
import com.notime.glyphsim.matrix.AvatarSpecies
import com.notime.glyphsim.matrix.PlayScene
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
import org.junit.Assert.assertNull
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

    @Test fun antippbarerAktionsknopfLiegtUeberDerDaumensteuerung() {
        var entered = 0
        compose.setContent {
            Box(Modifier.fillMaxSize()) {
                GameTouch({ stick = it }, { command, _ -> commands += command }, {}, {})
                GameActionButton("Shop", { entered++ }, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
            }
        }
        compose.onNodeWithText("Shop").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, entered); assertTrue(commands.isEmpty()) }
    }
    @Test fun sichtbaresRechtesPadStartetWirklichenSprungAuchNachNeukomposition() {
        val scene = GameWorld.scene(PlayScene.Place.PARK)!!
        var movement by mutableStateOf(GameMovement.State(pos = PlayControl.Pos(.5f, .6f)))
        var taps = 0
        compose.setContent {
            GameTouch({ stick = it }, { command, direction ->
                movement = GameTerrain.command(scene, movement, command, direction ?: stick, GameSurfaces.painted(scene))
            }, { taps++ }, {}, enabled = enabled.value)
        }
        compose.onNodeWithTag("game-action-pad", useUnmergedTree = true).performTouchInput { click() }
        compose.runOnIdle {
            assertEquals(GameMovement.Action.JUMP, movement.action)
            repeat(20) { movement = GameTerrain.tick(scene, AvatarSpecies.FENNEC, movement, stick, 16, GameSurfaces.painted(scene)).state }
            assertTrue(movement.height > 15f)
            assertEquals(0, taps)
            repeat(30) { movement = GameTerrain.tick(scene, AvatarSpecies.FENNEC, movement, stick, 16, GameSurfaces.painted(scene)).state }
            assertNull(movement.action)
        }
        compose.onNodeWithTag("game-action-pad", useUnmergedTree = true).performTouchInput { click() }
        compose.runOnIdle { assertEquals(GameMovement.Action.JUMP, movement.action) }
    }

    @Test fun rechterTippBleibtBisZumNaechstenMotorbildErhalten() {
        val scene=GameWorld.scene(PlayScene.Place.PARK)!!
        val surfaces=GameSurfaces.painted(scene)
        var request by mutableStateOf<Pair<GameMovement.Command,PlayControl.Stick>?>(null)
        compose.setContent { GameTouch({stick=it},{command,direction->request=command to (direction ?: stick)},
            {},{},enabled=enabled.value) }
        compose.onNodeWithTag("game-action-pad",useUnmergedTree=true).performTouchInput { click() }
        compose.runOnIdle {
            assertEquals(GameMovement.Command.JUMP,request!!.first)
            // Der Orts-/Positionsabgleich darf einen gerade angetippten Befehl nicht verwerfen.
            var movement=GameMovement.State(pos=PlayControl.Pos(.5f,.6f))
            movement=GameTerrain.tick(scene,AvatarSpecies.FENNEC,movement,stick,16L,surfaces,request).state
            request=null
            repeat(20) { movement=GameTerrain.tick(scene,AvatarSpecies.FENNEC,movement,stick,16L,surfaces).state }
            assertTrue(movement.height>20f)
        }
    }

}
