package com.notime.glyphsim.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Die Antwort bleibt kurz sichtbar; ungepruefter Zuschauertext wird nie ins Bild gespiegelt. */
@Composable
internal fun StreamFennecBubble(viewer: String, reply: String?, modifier: Modifier = Modifier) {
    var dots by remember { mutableIntStateOf(1) }
    LaunchedEffect(reply) {
        while (reply == null) {
            delay(350)
            dots = dots % 3 + 1
        }
    }
    Column(modifier.padding(16.dp).widthIn(max = 380.dp)
        .background(Color(0xEE10202C), RoundedCornerShape(14.dp)).padding(14.dp)) {
        Text("Fennec · @$viewer", color = Color(0xFF9DDAC7), fontSize = 12.sp)
        Text(reply ?: "·".repeat(dots), color = Color(0xFFF0F5F6), fontSize = 17.sp,
            lineHeight = 23.sp)
    }
}
