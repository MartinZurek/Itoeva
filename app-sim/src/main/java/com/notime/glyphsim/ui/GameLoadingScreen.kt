package com.notime.glyphsim.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

/** Vollstaendig deckende Startflaeche: kein alter Rasterraum unter halb geladenen Weltbildern. */
@Composable
internal fun GameLoadingScreen(state: GameAssets,german: Boolean,onRetry: ()->Unit,onExit: ()->Unit) {
    Box(Modifier.fillMaxSize().zIndex(100f).background(Color(0xFF10212B)).testTag("game-loading")
        .pointerInput(Unit) { detectTapGestures {} }) {
        state.images.entries.firstOrNull { it.key.startsWith("world/") && it.value!=null }?.value?.let { image ->
            Image(image,null,Modifier.fillMaxSize(),contentScale=ContentScale.Crop)
        }
        Box(Modifier.fillMaxSize().background(Color(0x77081721)))
        Column(Modifier.align(Alignment.Center).widthIn(max=460.dp).fillMaxWidth().padding(28.dp),
            horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(14.dp)) {
            Text(if(german) "Deine Welt wird vorbereitet" else "Preparing your world",color=Color(0xFFF5E8CF),fontSize=21.sp)
            Canvas(Modifier.fillMaxWidth().height(8.dp).semantics {
                progressBarRangeInfo=ProgressBarRangeInfo(state.progress,0f..1f)
            }.testTag("game-loading-progress")) {
                drawRect(Color(0xFF253E43));drawRect(Color(0xFFB9DEC4),size=Size(size.width*state.progress,size.height))
            }
            Text("${(state.progress*100).toInt()} %",color=Color.White)
            if(state.missing.isNotEmpty() && state.loaded==state.total) {
                Text(if(german) "Ein Bild konnte nicht geladen werden." else "An image could not be loaded.",color=Color.White)
                GameActionButton(if(german) "Erneut versuchen" else "Try again",onRetry,Modifier.fillMaxWidth())
                GameActionButton(if(german) "Zurueck" else "Back",onExit,Modifier.fillMaxWidth())
            }
        }
    }
}
