package com.notime.glyphsim.ui

import com.notime.glyphsim.matrix.AvatarGeometry
import com.notime.glyphsim.matrix.SceneCell

/** Display-only adjustments for a landscape broadcast. Simulation and saved preferences stay intact. */
internal object StreamPresentation {
    fun avatarSizeDp(heightDp: Float): Float = (heightDp * 0.30f).coerceIn(88f, 132f)

    fun readableNight(cells: List<SceneCell>, night: Boolean, fade: Float): List<SceneCell> {
        if (!night) return cells
        return cells.map { cell ->
            if (cell.isLight || cell.brightness <= 0) cell else cell.copy(
                brightness = (cell.brightness * 1.65f + 160f * fade.coerceIn(0f, 1f))
                    .toInt().coerceAtMost(AvatarGeometry.MAX_BRIGHTNESS)
            )
        }
    }
}
