package com.notime.glyphsim.ui

import androidx.compose.ui.graphics.Color
import com.notime.glyphcore.data.AnimationType

/** Emoji + Akzentfarbe je fest eingebautem [AnimationType] - gemeinsam genutzt von ReminderScreen und LibraryScreen. */
data class AnimationVisual(val emoji: String, val color: Color)

val animationVisuals: Map<AnimationType, AnimationVisual> = mapOf(
    AnimationType.FOCUS to AnimationVisual("🎯", Color(0xFF6750A4)),
    AnimationType.DRINK to AnimationVisual("💧", Color(0xFF1E88E5)),
    AnimationType.MOVE to AnimationVisual("🏃", Color(0xFFF4511E)),
    AnimationType.GENERAL to AnimationVisual("🔔", Color(0xFF546E7A)),
    AnimationType.REST to AnimationVisual("☕", Color(0xFF00897B)),
    AnimationType.WORK to AnimationVisual("💻", Color(0xFF607D8B)),
    AnimationType.MINDFULNESS to AnimationVisual("🧘", Color(0xFF43A047)),
    AnimationType.LOVE to AnimationVisual("❤️", Color(0xFFD81B60)),
    AnimationType.SLEEP to AnimationVisual("🌙", Color(0xFF3949AB)),
    AnimationType.MEDICINE to AnimationVisual("💊", Color(0xFFE53935)),
    AnimationType.BOOK to AnimationVisual("📖", Color(0xFFF9A825)),
    AnimationType.CREATIVITY to AnimationVisual("🎨", Color(0xFF00ACC1))
)
