package com.notime.glyphsim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.notime.glyphsim.matrix.GameWorld
import com.notime.glyphsim.matrix.GameDoors
import com.notime.glyphsim.matrix.PlayScene
import com.notime.glyphsim.stream.FennecWorld
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameLoadingScreenTest {
    @get:Rule val compose=createComposeRule()
    @Test fun ladebildschirmIstDeckendUndZeigtEchtenFortschritt() {
        compose.setContent {
            Box(Modifier.fillMaxSize().background(Color.White).testTag("root")) {
                GameLoadingScreen(GameAssets(loaded=15,total=30),true,{},{})
            }
        }
        compose.onNodeWithTag("game-loading").assertIsDisplayed()
        val progress=compose.onNodeWithTag("game-loading-progress").fetchSemanticsNode()
            .config[SemanticsProperties.ProgressBarRangeInfo]
        assertEquals(.5f,progress.current,.001f)
        val pixels=compose.onNodeWithTag("root").captureToImage().toPixelMap()
        assertTrue(pixels[2,2].red<.2f);assertTrue(pixels[2,2].alpha>.99f)
    }
    @Test fun fehlendesBildKannErneutGeladenWerdenStattEndlosZuWarten() {
        var retries=0
        compose.setContent {
            GameLoadingScreen(GameAssets(loaded=30,total=30,missing=listOf("world/coast.png")),true,{ retries++ },{})
        }
        compose.onNodeWithText("Erneut versuchen").performClick()
        compose.runOnIdle { assertEquals(1,retries) }
    }
    @Test fun beideKorridorzieleSindUeberDemDeaktiviertenDaumenpadAntippbar() {
        val choices=GameDoors.choices(GameWorld.passages(PlayScene.Place.KITCHEN).first())
        val selected=mutableListOf<PlayScene.Place>()
        compose.setContent {
            Box(Modifier.fillMaxSize()) {
                GameTouch({},{_,_->},{},{},enabled=false)
                GameDoorChoicePanel(choices,true,{ selected+=it.to },{})
            }
        }
        for(passage in choices) {
            compose.onNodeWithText(FennecWorld.name(passage.to,true)).performTouchInput { click() }
        }
        compose.runOnIdle { assertEquals(choices.map { it.to },selected) }
    }
}
