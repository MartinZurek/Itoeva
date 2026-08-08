package com.notime.glyphkalender.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Fallback-Palette fuer Geraete ohne dynamische Farben (Android < 12) - minSdk ist zwar
// 34 (Glyph Matrix SDK setzt das voraus), Compose-Previews u.ae. laufen aber ohne
// Dynamic-Color-Kontext, daher trotzdem eine feste Palette hinterlegt.
private val LightColors = lightColorScheme(
    primary = Color(0xFF3F5AA9),
    secondary = Color(0xFF5B6472),
    tertiary = Color(0xFF7A5265)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB4C5FF),
    secondary = Color(0xFFC2CAD9),
    tertiary = Color(0xFFEAB8CD)
)

@Composable
fun GlyphKalenderTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
